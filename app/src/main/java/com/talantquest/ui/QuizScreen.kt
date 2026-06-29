package com.talantquest.ui

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import com.talantquest.data.GameData
import com.talantquest.data.GameViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale

private const val REWARD_PER_CORRECT = 50
private const val QUESTION_TIME = 30
private const val WRONG_LOCK_SECS = 12

@Composable
fun QuizScreen(vm: GameViewModel, tagId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val quizTag = remember { GameData.getQuizTag(tagId) }

    if (quizTag == null) {
        ErrorScreen("알 수 없는 퀴즈 태그입니다", onBack); return
    }

    var currentIndex by remember { mutableIntStateOf(0) }
    var correctCount by remember { mutableIntStateOf(0) }
    var selectedIndex by remember { mutableIntStateOf(-1) }
    var showResult by remember { mutableStateOf(false) }
    var isWrong by remember { mutableStateOf(false) }
    var showFinalResult by remember { mutableStateOf(false) }
    var timeLeft by remember { mutableIntStateOf(QUESTION_TIME) }
    var revealedCount by remember { mutableIntStateOf(0) }
    var isRevealing by remember { mutableStateOf(true) }
    var wrongLock by remember { mutableIntStateOf(0) }

    // TTS — 준비 완료 시점을 추적
    var showVolumeDialog by remember { mutableStateOf(false) }
    var volumeSlider by remember { mutableFloatStateOf(vm.ttsVolume.floatValue) }

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
        val earned = correctCount * REWARD_PER_CORRECT
        ResultScreen(
            emoji = if (correctCount == quizTag.questions.size) "🎉" else "📝",
            title = "퀴즈 완료!",
            body = "${quizTag.questions.size}문제 중 $correctCount 문제 정답",
            amount = earned,
            onClose = onBack
        )
        return
    }

    if (vm.isTagUsed("QUIZ_$tagId")) {
        UsedTagScreen("이미 사용한 퀴즈 태그입니다", onBack); return
    }

    val question = quizTag.questions[currentIndex]

    // 문제 공개 + 타이머
    LaunchedEffect(currentIndex) {
        revealedCount = 0
        isRevealing = true
        showResult = false
        isWrong = false
        selectedIndex = -1
        wrongLock = 0

        tts.stop()

        // TTS 엔진 준비 대기 (최대 3초)
        val deadline = System.currentTimeMillis() + 3_000L
        while (!ttsReady && System.currentTimeMillis() < deadline) delay(50)

        delay(200)
        tts.speakWait(question.question, vm.ttsVolume.floatValue)
        delay(300)

        for (i in question.options.indices) {
            revealedCount = i + 1
            tts.speakWait("${i + 1}번. ${question.options[i]}", vm.ttsVolume.floatValue)
            delay(200)
        }
        isRevealing = false

        // 타이머
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
            val correct = selectedIndex == question.correctIndex
            if (correct) correctCount++
            isWrong = !correct
            showResult = true
            if (correct) toneGen?.startTone(ToneGenerator.TONE_PROP_ACK, 300)
            else toneGen?.startTone(ToneGenerator.TONE_PROP_NACK, 500)
        }
    }

    // 오답 시 TTS + 잠금 카운트다운
    LaunchedEffect(currentIndex, showResult, isWrong) {
        if (!showResult || !isWrong) return@LaunchedEffect
        delay(600)
        tts.speakWait("정답은, ${question.options[question.correctIndex]} 입니다.", vm.ttsVolume.floatValue)
        wrongLock = WRONG_LOCK_SECS
        while (wrongLock > 0) {
            delay(1000)
            wrongLock--
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
            .padding(24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("❓", fontSize = 36.sp)
            Spacer(Modifier.width(8.dp))
            Text(
                "퀴즈 태그 #$tagId",
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            // 볼륨 조절 버튼
            val volIcon = when {
                vm.ttsVolume.floatValue >= 0.7f -> "🔊"
                vm.ttsVolume.floatValue >= 0.3f -> "🔉"
                else                            -> "🔈"
            }
            TextButton(
                onClick = { volumeSlider = vm.ttsVolume.floatValue; showVolumeDialog = true },
                contentPadding = PaddingValues(4.dp)
            ) { Text(volIcon, fontSize = 28.sp) }

            if (!showResult && !isRevealing) {
                Spacer(Modifier.width(4.dp))
                Text("%02d".format(timeLeft), fontSize = 33.sp, fontWeight = FontWeight.Bold, color = timerColor)
                Text(" 초", fontSize = 21.sp, color = timerColor)
            }
        }

        // 볼륨 슬라이더 다이얼로그
        if (showVolumeDialog) {
            AlertDialog(
                onDismissRequest = { showVolumeDialog = false },
                title = { Text("TTS 음성 크기") },
                text = {
                    Column {
                        Text(
                            "${(volumeSlider * 100).roundToInt()}%",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        Spacer(Modifier.height(8.dp))
                        Slider(
                            value = volumeSlider,
                            onValueChange = { volumeSlider = it },
                            valueRange = 0.1f..1.0f,
                            steps = 8
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        vm.setTtsVolume(volumeSlider)
                        showVolumeDialog = false
                    }) { Text("확인") }
                },
                dismissButton = {
                    TextButton(onClick = { showVolumeDialog = false }) { Text("취소") }
                }
            )
        }

        Spacer(Modifier.height(8.dp))

        LinearProgressIndicator(
            progress = { (currentIndex + 1).toFloat() / quizTag.questions.size },
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            "${currentIndex + 1} / ${quizTag.questions.size}",
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )

        if (!showResult && !isRevealing) {
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { timeLeft.toFloat() / QUESTION_TIME },
                modifier = Modifier.fillMaxWidth(),
                color = timerColor,
                trackColor = timerColor.copy(alpha = 0.2f)
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

            Spacer(Modifier.height(16.dp))

            question.options.forEachIndexed { i, option ->
                if (i >= revealedCount) return@forEachIndexed
                val isSelected = selectedIndex == i
                val isCorrect = i == question.correctIndex
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
            !showResult -> selectedIndex >= 0
            isWrong && wrongLock > 0 -> false
            else -> true
        }
        Button(
            onClick = {
                if (!showResult) {
                    if (selectedIndex == -1) return@Button
                    val correct = selectedIndex == question.correctIndex
                    if (correct) {
                        correctCount++
                        toneGen?.startTone(ToneGenerator.TONE_PROP_ACK, 300)
                    } else {
                        isWrong = true
                        toneGen?.startTone(ToneGenerator.TONE_PROP_NACK, 500)
                    }
                    showResult = true
                } else if (buttonEnabled) {
                    if (currentIndex + 1 < quizTag.questions.size) {
                        showResult = false
                        isWrong = false
                        selectedIndex = -1
                        currentIndex++
                    } else {
                        vm.addTalant(correctCount * REWARD_PER_CORRECT)
                        vm.markTagUsed("QUIZ_$tagId")
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
                    isRevealing                               -> "문제 읽는 중..."
                    !showResult && selectedIndex == -1        -> "답을 선택하세요"
                    !showResult                               -> "정답 확인"
                    isWrong && wrongLock > 0                  -> "잠시 후 다음으로... (${wrongLock}초)"
                    currentIndex + 1 < quizTag.questions.size -> "다음 문제 →"
                    else                                      -> "결과 보기"
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
