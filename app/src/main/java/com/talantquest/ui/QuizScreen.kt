package com.talantquest.ui

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.talantquest.data.GameData
import com.talantquest.data.GameViewModel
import com.talantquest.data.QuizQuestion
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale

private const val REWARD_PER_CORRECT = 50
private const val QUESTION_TIME = 30
private const val WRONG_LOCK_SECS = 12
private const val SHORT_ANSWER_MAX_POINTS = 100 // 주관식 기본 점수 (문제별 maxPoints로 override 가능)
private const val WRONG_PENALTY = 10           // 주관식 오답 1회당 감점

@Composable
fun QuizScreen(vm: GameViewModel, tagId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val quizTag = remember { GameData.getQuizTag(tagId) }

    if (quizTag == null) {
        ErrorScreen("알 수 없는 퀴즈 태그입니다", onBack); return
    }

    var currentIndex by remember { mutableIntStateOf(0) }
    var correctCount by remember { mutableIntStateOf(0) }
    var selectedIndex by remember { mutableIntStateOf(-1) }
    var typedAnswer by remember { mutableStateOf("") }
    var showResult by remember { mutableStateOf(false) }
    var isWrong by remember { mutableStateOf(false) }
    var showFinalResult by remember { mutableStateOf(false) }
    var timeLeft by remember { mutableIntStateOf(QUESTION_TIME) }
    var revealedCount by remember { mutableIntStateOf(0) }
    var isRevealing by remember { mutableStateOf(true) }
    var wrongLock by remember { mutableIntStateOf(0) }
    var earnedTalant by remember { mutableIntStateOf(0) }       // 누적 획득 달란트
    var saPoints by remember {
        val initial = (quizTag.questions.firstOrNull() as? QuizQuestion.ShortAnswer)?.maxPoints
            ?: SHORT_ANSWER_MAX_POINTS
        mutableIntStateOf(initial)
    } // 현재 주관식 문제 점수 (문제별 maxPoints 우선)
    var revealedHints by remember { mutableIntStateOf(0) }      // 공개된 힌트 단계 수
    var showHintConfirm by remember { mutableStateOf(false) }   // 힌트 공개 확인 팝업
    var saJustWrong by remember { mutableStateOf(false) }       // 직전 제출이 오답이었는지(재시도용)

    // 한 번 답을 제출(또는 타임아웃)하면 그 즉시 태그를 사용 처리한다.
    // 이렇게 해야 중간에 뒤로가기 → 재태깅으로 처음부터 다시 푸는 악용을 막을 수 있다.
    var committed by rememberSaveable { mutableStateOf(false) }
    fun commitTag() {
        if (!committed) {
            vm.markTagUsed("QUIZ_$tagId")
            committed = true
        }
    }
    var showExitConfirm by remember { mutableStateOf(false) } // 뒤로가기 경고 팝업

    // TTS — 준비 완료 시점을 추적
    var ttsReady by remember { mutableStateOf(false) }
    val tts = remember {
        val ref = arrayOfNulls<TextToSpeech>(1)
        ref[0] = TextToSpeech(context) { status ->
            ref[0]?.language = Locale.KOREAN
            ttsReady = true // 성공/실패 무관하게 준비 완료 처리
        }
        ref[0]!!
    }
    DisposableEffect(Unit) { onDispose { tts.stop(); tts.shutdown() } }

    val toneGen = remember { try { ToneGenerator(AudioManager.STREAM_MUSIC, 85) } catch (_: Exception) { null } }
    DisposableEffect(Unit) { onDispose { toneGen?.release() } }

    if (showFinalResult) {
        val earned = earnedTalant
        ResultScreen(
            emoji = if (correctCount == quizTag.questions.size) "🎉" else "📝",
            title = "퀴즈 완료!",
            body = "${quizTag.questions.size}문제 중 $correctCount 문제 정답",
            amount = earned,
            onClose = onBack
        )
        return
    }

    if (!committed && vm.isTagUsed("QUIZ_$tagId")) {
        UsedTagScreen("이미 사용한 퀴즈 태그입니다", onBack); return
    }

    val question = quizTag.questions[currentIndex]
    val showTimer = question is QuizQuestion.MultipleChoice // 주관식은 점수제라 타이머 없음

    // 이미 답을 제출해 진행 중일 때 뒤로가기를 누르면 경고 → 실수 이탈 방지
    // (아직 한 문제도 풀지 않았다면 태그가 소비되지 않으므로 그냥 나가도 됨)
    BackHandler(enabled = committed) {
        showExitConfirm = true
    }

    // 현재 입력/선택이 정답인지 — 유형별로 판정
    fun isAnswerCorrect(): Boolean = when (val q = question) {
        is QuizQuestion.MultipleChoice -> selectedIndex == q.correctIndex
        is QuizQuestion.ShortAnswer -> q.matches(typedAnswer)
    }

    // 정답 제출 가능 상태(보기 선택 or 입력 존재)
    fun hasAnswerInput(): Boolean = when (question) {
        is QuizQuestion.MultipleChoice -> selectedIndex >= 0
        is QuizQuestion.ShortAnswer -> typedAnswer.isNotBlank()
    }

    // 정답 제출(채점) — 버튼/키보드 완료에서 공용 호출
    fun submitAnswer() {
        if (showResult || isRevealing || !hasAnswerInput()) return
        commitTag() // 첫 제출 순간 태그를 소비 → 뒤로가기 후 재도전 차단
        val correct = isAnswerCorrect()
        when (question) {
            is QuizQuestion.MultipleChoice -> {
                if (correct) {
                    correctCount++
                    earnedTalant += REWARD_PER_CORRECT
                    vm.addTalant(REWARD_PER_CORRECT) // 정답 즉시 지급(중도 이탈해도 유지)
                    toneGen?.startTone(ToneGenerator.TONE_PROP_ACK, 300)
                } else {
                    isWrong = true
                    toneGen?.startTone(ToneGenerator.TONE_PROP_NACK, 500)
                }
                showResult = true
                focusManager.clearFocus()
            }
            is QuizQuestion.ShortAnswer -> {
                if (correct) {
                    correctCount++
                    earnedTalant += saPoints // 남은 점수만큼 획득
                    vm.addTalant(saPoints)   // 정답 즉시 지급(중도 이탈해도 유지)
                    toneGen?.startTone(ToneGenerator.TONE_PROP_ACK, 300)
                    showResult = true
                    focusManager.clearFocus()
                } else {
                    saPoints -= WRONG_PENALTY
                    saJustWrong = true
                    toneGen?.startTone(ToneGenerator.TONE_PROP_NACK, 500)
                    vibrate(context, false) // 촉각 피드백
                    if (saPoints <= 0) {
                        saPoints = 0
                        isWrong = true // 0점 → 실패: 정답 공개 + 잠금 후 다음
                        showResult = true
                        focusManager.clearFocus()
                        Toast.makeText(context, "❌ 실패! 정답을 확인하세요.", Toast.LENGTH_SHORT).show()
                    } else {
                        // 점수가 남아 있으면 showResult를 두지 않아 재시도 가능
                        Toast.makeText(
                            context,
                            "❌ 틀렸어요!  -${WRONG_PENALTY}점 (남은 점수 ${saPoints}점)",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    // 문제 공개 + 타이머
    LaunchedEffect(currentIndex) {
        revealedCount = 0
        isRevealing = true
        showResult = false
        isWrong = false
        selectedIndex = -1
        typedAnswer = ""
        wrongLock = 0
        saPoints = (question as? QuizQuestion.ShortAnswer)?.maxPoints ?: SHORT_ANSWER_MAX_POINTS
        revealedHints = 0
        showHintConfirm = false
        saJustWrong = false

        tts.stop()

        // TTS 엔진 준비 대기 (최대 3초)
        val deadline = System.currentTimeMillis() + 3_000L
        while (!ttsReady && System.currentTimeMillis() < deadline) delay(50)

        delay(200)
        tts.speakWait(question.question)
        delay(300)

        // 객관식만 보기를 하나씩 공개하며 읽어준다 (주관식은 힌트를 버튼으로 직접 펼침)
        (question as? QuizQuestion.MultipleChoice)?.let { q ->
            for (i in q.options.indices) {
                revealedCount = i + 1
                tts.speakWait("${i + 1}번. ${q.options[i]}")
                delay(200)
            }
        }
        isRevealing = false

        // 타이머 — 객관식만 (주관식은 점수제라 시간 제한 없음)
        if (question is QuizQuestion.MultipleChoice) {
            timeLeft = QUESTION_TIME
            while (timeLeft > 0 && !showResult) {
                delay(1000)
                if (showResult) break
                timeLeft--
                if (timeLeft in 1..10 && !showResult) {
                    toneGen?.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
                }
            }

            // 시간 초과 처리
            if (!showResult) {
                commitTag() // 시간 초과도 한 번의 시도로 간주 → 재도전 차단
                val correct = isAnswerCorrect()
                if (correct) {
                    correctCount++
                    earnedTalant += REWARD_PER_CORRECT
                    vm.addTalant(REWARD_PER_CORRECT)
                }
                isWrong = !correct
                showResult = true
                if (correct) toneGen?.startTone(ToneGenerator.TONE_PROP_ACK, 300)
                else toneGen?.startTone(ToneGenerator.TONE_PROP_NACK, 500)
            }
        }
    }

    // 결과 발표(TTS) + (오답/실패 시) 잠금 카운트다운
    LaunchedEffect(currentIndex, showResult) {
        if (!showResult) return@LaunchedEffect
        val q = question
        delay(600)
        if (isWrong) {
            // 오답/실패 — 정답을 읽어주고, 주관식이면 해설까지 읽은 뒤 잠금
            tts.speakWait("정답은, ${q.correctAnswerText} 입니다.")
            if (q is QuizQuestion.ShortAnswer) {
                q.explanation?.let { tts.speakWait("해설. $it") }
            }
            wrongLock = WRONG_LOCK_SECS
            while (wrongLock > 0) {
                delay(1000)
                wrongLock--
            }
        } else if (q is QuizQuestion.ShortAnswer) {
            // 정답 — 주관식은 정답과 해설을 읽어준다
            tts.speakWait("정답입니다. 정답은 ${q.correctAnswerText}.")
            q.explanation?.let { tts.speakWait("해설. $it") }
        }
    }

    // 힌트를 새로 펼치면 그 힌트를 TTS로 읽어준다
    LaunchedEffect(revealedHints) {
        val q = question
        if (q is QuizQuestion.ShortAnswer && revealedHints in 1..q.hints.size) {
            tts.speakWait("힌트 ${revealedHints}단계. ${q.hints[revealedHints - 1].text}")
        }
    }

    val timerColor by animateColorAsState(
        targetValue = when {
            timeLeft > 10 -> MaterialTheme.colorScheme.primary
            timeLeft > 5  -> Color(0xFFE65100)
            else          -> Color(0xFFB71C1C)
        },
        animationSpec = tween(300),
        label = "timerColor"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("❓", fontSize = 24.sp)
            Spacer(Modifier.width(8.dp))
            Text(
                "퀴즈 태그 #$tagId",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )

            if (showTimer && !showResult && !isRevealing) {
                Spacer(Modifier.width(4.dp))
                Text("%02d".format(timeLeft), fontSize = 26.sp, fontWeight = FontWeight.Bold, color = timerColor)
                Text(" 초", fontSize = 18.sp, color = timerColor)
            }
        }

        // 뒤로가기 확인 다이얼로그 — 나가면 남은 문제를 다시 풀 수 없음을 경고
        if (showExitConfirm) {
            AlertDialog(
                onDismissRequest = { showExitConfirm = false },
                title = { Text("퀴즈를 나가시겠어요?") },
                text = {
                    Text(
                        "지금 나가면 남은 문제는 다시 풀 수 없어요.\n" +
                            "이미 푼 문제로 받은 달란트는 그대로 유지됩니다.",
                        fontSize = 20.sp,
                        lineHeight = 32.sp
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showExitConfirm = false; onBack() }) { Text("나가기") }
                },
                dismissButton = {
                    TextButton(onClick = { showExitConfirm = false }) { Text("계속 풀기") }
                }
            )
        }

        // 힌트 공개 확인 다이얼로그 — 감점을 알리고 정말 볼지 확인
        if (showHintConfirm && question is QuizQuestion.ShortAnswer && revealedHints < question.hints.size) {
            val nextHint = question.hints[revealedHints]
            val after = maxOf(0, saPoints - nextHint.penalty)
            AlertDialog(
                onDismissRequest = { showHintConfirm = false },
                title = { Text("힌트 ${revealedHints + 1}단계 공개") },
                text = {
                    Text(
                        "이 힌트를 보면 ${nextHint.penalty}점이 감점됩니다.\n" +
                            "현재 ${saPoints}점 → ${after}점\n\n정말 보시겠어요?",
                        fontSize = 20.sp,
                        lineHeight = 32.sp
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        saPoints = after
                        revealedHints++
                        showHintConfirm = false
                    }) { Text("보기 (-${nextHint.penalty}점)") }
                },
                dismissButton = {
                    TextButton(onClick = { showHintConfirm = false }) { Text("취소") }
                }
            )
        }

        Spacer(Modifier.height(8.dp))

        LinearProgressIndicator(
            progress = { (currentIndex + 1).toFloat() / quizTag.questions.size },
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
            drawStopIndicator = {}
        )
        Text(
            "${currentIndex + 1} / ${quizTag.questions.size}",
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )

        if (showTimer && !showResult && !isRevealing) {
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { timeLeft.toFloat() / QUESTION_TIME },
                modifier = Modifier.fillMaxWidth(),
                color = timerColor,
                trackColor = timerColor.copy(alpha = 0.2f),
                drawStopIndicator = {}
            )
        }

        Spacer(Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Text(
                    question.question,
                    modifier = Modifier.padding(20.dp),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 38.sp
                )
            }

            // 문제에 이미지가 있으면 표시
            question.imageRes?.let { res ->
                Spacer(Modifier.height(16.dp))
                Image(
                    painter = painterResource(id = res),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            }

            Spacer(Modifier.height(16.dp))

            when (val q = question) {
                is QuizQuestion.MultipleChoice ->
                    q.options.forEachIndexed { i, option ->
                        if (i >= revealedCount) return@forEachIndexed
                        val isSelected = selectedIndex == i
                        val isCorrect = i == q.correctIndex
                        val bgColor = when {
                            !showResult -> if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            isCorrect   -> Color(0xFF1B5E20)
                            isSelected  -> Color(0xFF7F0000)
                            else        -> MaterialTheme.colorScheme.surface
                        }
                        OutlinedButton(
                            onClick = { if (!showResult && !isRevealing) selectedIndex = i },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .heightIn(min = 72.dp),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = bgColor)
                        ) {
                            Text(
                                "${'①'.plus(i)} $option",
                                fontSize = 21.sp,
                                textAlign = TextAlign.Start,
                                modifier = Modifier.fillMaxWidth(),
                                lineHeight = 30.sp
                            )
                        }
                    }

                is QuizQuestion.ShortAnswer -> {
                    OutlinedTextField(
                        value = typedAnswer,
                        onValueChange = { if (!showResult && !isRevealing) { typedAnswer = it; saJustWrong = false } },
                        enabled = !showResult && !isRevealing,
                        singleLine = true,
                        isError = saJustWrong && !showResult,
                        label = { Text("정답을 입력하세요", fontSize = 16.sp) },
                        textStyle = LocalTextStyle.current.copy(fontSize = 24.sp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submitAnswer() }),
                        supportingText = if (saJustWrong && !showResult) {
                            {
                                Text(
                                    "❌ 틀렸어요!  -${WRONG_PENALTY}점 (남은 점수 ${saPoints}점)",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else null,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // 풀이 중 — 점수 / 오답 피드백 / 단계별 힌트
                    if (!showResult) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "이 문제 점수: ${saPoints}점  (오답 시 -${WRONG_PENALTY}점, 0점이면 실패)",
                            fontSize = 20.sp,
                            lineHeight = 30.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (q.hints.isNotEmpty()) {
                            Spacer(Modifier.height(16.dp))
                            for (idx in 0 until revealedHints) {
                                Text(
                                    "💡 힌트 ${idx + 1}. ${q.hints[idx].text}",
                                    fontSize = 20.sp,
                                    lineHeight = 30.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                            if (revealedHints < q.hints.size) {
                                val nextPenalty = q.hints[revealedHints].penalty
                                TextButton(
                                    onClick = { showHintConfirm = true },
                                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        "💡 힌트 ${revealedHints + 1}단계 보기 (-${nextPenalty}점)",
                                        fontSize = 20.sp
                                    )
                                }
                            }
                        }
                    }

                    // 결과 — 정답/실패 + 해설
                    if (showResult) {
                        Spacer(Modifier.height(20.dp))
                        val ok = !isWrong
                        Text(
                            if (ok) "✅ 정답입니다!  (+${saPoints}점)" else "❌ 실패!  정답: ${q.answers.first()}",
                            fontSize = 26.sp,
                            lineHeight = 38.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (ok) Color(0xFF1B5E20) else MaterialTheme.colorScheme.error
                        )
                        q.explanation?.let { exp ->
                            Spacer(Modifier.height(16.dp))
                            Card(colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                            )) {
                                Column(Modifier.fillMaxWidth().padding(20.dp)) {
                                    Text("📖 해설", fontSize = 22.sp, fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.height(10.dp))
                                    Text(exp, fontSize = 20.sp, lineHeight = 32.sp)
                                }
                            }
                        }
                    }
                }
            }

            if (showResult && isWrong && wrongLock > 0) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "정답 확인 중... ${wrongLock}초 후 넘어갈 수 있습니다",
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(12.dp))

        val buttonEnabled = when {
            isRevealing -> false
            !showResult -> hasAnswerInput()
            isWrong && wrongLock > 0 -> false
            else -> true
        }
        Button(
            onClick = {
                if (!showResult) {
                    submitAnswer()
                } else if (buttonEnabled) {
                    if (currentIndex + 1 < quizTag.questions.size) {
                        showResult = false
                        isWrong = false
                        selectedIndex = -1
                        typedAnswer = ""
                        currentIndex++
                    } else {
                        // 달란트는 정답마다 즉시 지급했고, 태그는 첫 제출 시 이미 사용 처리됨
                        commitTag() // 안전망: 혹시 커밋되지 않았다면 여기서 보장
                        showFinalResult = true
                    }
                }
            },
            enabled = buttonEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
        ) {
            Text(
                when {
                    isRevealing                  -> "문제 읽는 중..."
                    !showResult && !hasAnswerInput() ->
                        if (question is QuizQuestion.ShortAnswer) "정답을 입력하세요" else "답을 선택하세요"
                    !showResult                  -> "정답 확인"
                    isWrong && wrongLock > 0     -> "잠시 후 다음으로... (${wrongLock}초)"
                    currentIndex + 1 < quizTag.questions.size -> "다음 문제 →"
                    else                         -> "결과 보기"
                },
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        }
    }
}

private fun Char.plus(n: Int) = Char(this.code + n)

private suspend fun TextToSpeech.speakWait(text: String, volume: Float = 1.0f) {
    val id = System.nanoTime().toString()
    val ch = Channel<Unit>(Channel.CONFLATED)
    setOnUtteranceProgressListener(object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String) {}
        override fun onDone(utteranceId: String) { ch.trySend(Unit) }
        @Suppress("OVERRIDE_DEPRECATION")
        override fun onError(utteranceId: String) { ch.trySend(Unit) }
    })
    val params = Bundle().apply {
        putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume.coerceIn(0.1f, 1.0f))
    }
    val result = speak(text, TextToSpeech.QUEUE_FLUSH, params, id)
    if (result == TextToSpeech.ERROR) {
        delay((text.length * 150L + 500L).coerceAtMost(4_000L))
        return
    }
    val timeout = (text.length * 400L + 2_000L).coerceAtMost(12_000L)
    withTimeoutOrNull(timeout) { ch.receive() }
}
