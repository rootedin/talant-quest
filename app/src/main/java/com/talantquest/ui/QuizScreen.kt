package com.talantquest.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.talantquest.data.GameData
import com.talantquest.data.GameViewModel

private const val REWARD_PER_CORRECT = 30

@Composable
fun QuizScreen(vm: GameViewModel, tagId: String, onBack: () -> Unit) {
    val quizTag = remember { GameData.getQuizTag(tagId) }

    if (quizTag == null) {
        ErrorScreen("알 수 없는 퀴즈 태그입니다", onBack); return
    }

    var currentIndex by remember { mutableIntStateOf(0) }
    var correctCount by remember { mutableIntStateOf(0) }
    var selectedIndex by remember { mutableIntStateOf(-1) }
    var showResult by remember { mutableStateOf(false) }
    var showFinalResult by remember { mutableStateOf(false) }

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // 헤더
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("❓", fontSize = 24.sp)
            Spacer(Modifier.width(8.dp))
            Text(
                "퀴즈 태그 #$tagId",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(Modifier.height(8.dp))

        // 진행 상황
        LinearProgressIndicator(
            progress = { (currentIndex + 1).toFloat() / quizTag.questions.size },
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            "${currentIndex + 1} / ${quizTag.questions.size}",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(Modifier.height(24.dp))

        // 문제
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Text(
                question.question,
                modifier = Modifier.padding(20.dp),
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 26.sp
            )
        }

        Spacer(Modifier.height(20.dp))

        // 선택지
        question.options.forEachIndexed { i, option ->
            val isSelected = selectedIndex == i
            val bgColor = when {
                !showResult -> if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                i == question.correctIndex -> Color(0xFF1B5E20)
                isSelected && i != question.correctIndex -> Color(0xFF7F0000)
                else -> MaterialTheme.colorScheme.surface
            }
            OutlinedButton(
                onClick = { if (!showResult) selectedIndex = i },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .height(52.dp),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = bgColor)
            ) {
                Text(
                    "${'①'.plus(i)} $option",
                    fontSize = 14.sp,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(Modifier.weight(1f))

        // 정답 확인 / 다음 버튼
        Button(
            onClick = {
                if (!showResult) {
                    if (selectedIndex == -1) return@Button
                    if (selectedIndex == question.correctIndex) correctCount++
                    showResult = true
                } else {
                    showResult = false
                    selectedIndex = -1
                    if (currentIndex + 1 < quizTag.questions.size) {
                        currentIndex++
                    } else {
                        vm.addTalant(correctCount * REWARD_PER_CORRECT)
                        vm.markTagUsed("QUIZ_$tagId")
                        showFinalResult = true
                    }
                }
            },
            enabled = !showResult || true,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(
                when {
                    !showResult && selectedIndex == -1 -> "답을 선택하세요"
                    !showResult -> "정답 확인"
                    currentIndex + 1 < quizTag.questions.size -> "다음 문제 →"
                    else -> "결과 보기"
                },
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun Char.plus(n: Int) = Char(this.code + n)
