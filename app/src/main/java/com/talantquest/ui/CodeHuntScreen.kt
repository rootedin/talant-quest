package com.talantquest.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.talantquest.data.GameData
import com.talantquest.data.GameViewModel

@Composable
fun CodeHuntScreen(vm: GameViewModel, tagId: String, onBack: () -> Unit) {
    val codeTag = remember { GameData.getCodeTag(tagId) }

    if (codeTag == null) {
        ErrorScreen("알 수 없는 암호 태그입니다", onBack); return
    }

    var input by remember { mutableStateOf("") }
    var attemptsLeft by remember { mutableIntStateOf(2) }
    var showResult by remember { mutableStateOf(false) }
    var showFailed by remember { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current

    val currentReward = if (attemptsLeft == 2) codeTag.reward else codeTag.reward / 2

    if (showResult) {
        ResultScreen(
            emoji = "🔑",
            title = "암호 해독 성공!",
            body = "정답: ${codeTag.answer}",
            amount = currentReward,
            onClose = onBack
        )
        return
    }

    if (showFailed) {
        ResultScreen(
            emoji = "❌",
            title = "암호 해독 실패",
            body = "기회를 모두 사용했습니다.\n정답: ${codeTag.answer}",
            amount = 0,
            onClose = onBack
        )
        return
    }

    if (vm.isTagUsed("CODE_$tagId")) {
        UsedTagScreen("이미 사용한 암호 태그입니다", onBack); return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🔍", fontSize = 24.sp)
            Spacer(Modifier.width(8.dp))
            Text(
                "암호 찾기 #$tagId",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(Modifier.height(24.dp))

        // 힌트 카드
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    "💡 힌트",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    codeTag.hint,
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    lineHeight = 40.sp
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "보상: $currentReward 달란트" + if (attemptsLeft < 2) "  (첫 번째 오답으로 절반 감소)" else "",
                    fontSize = 21.sp,
                    color = if (attemptsLeft < 2) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "남은 기회: $attemptsLeft / 2",
                    fontSize = 20.sp,
                    color = if (attemptsLeft == 1) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        // 암호 입력
        OutlinedTextField(
            value = input,
            onValueChange = { input = it.uppercase() },
            label = { Text("암호 입력") },
            placeholder = { Text("찾은 암호를 입력하세요") },
            isError = attemptsLeft < 2,
            supportingText = if (attemptsLeft == 1) {
                { Text("틀렸습니다! 마지막 기회 — 보상이 절반으로 줄었습니다", color = MaterialTheme.colorScheme.error) }
            } else null,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { keyboard?.hide() }),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                keyboard?.hide()
                if (input.trim() == codeTag.answer) {
                    vm.addTalant(currentReward)
                    vm.markTagUsed("CODE_$tagId")
                    showResult = true
                } else {
                    attemptsLeft--
                    input = ""
                    if (attemptsLeft <= 0) {
                        vm.markTagUsed("CODE_$tagId")
                        showFailed = true
                    }
                }
            },
            enabled = input.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
        ) {
            Text(
                if (attemptsLeft == 1) "마지막 기회로 제출" else "암호 제출",
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            )
        }

        Spacer(Modifier.height(12.dp))

        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("돌아가기")
        }
    }
}
