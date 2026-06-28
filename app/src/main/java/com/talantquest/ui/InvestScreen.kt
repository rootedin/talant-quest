package com.talantquest.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.talantquest.data.GameData
import com.talantquest.data.GameViewModel
import kotlin.math.roundToInt
import kotlin.random.Random

@Composable
fun InvestScreen(vm: GameViewModel, tagId: String, onBack: () -> Unit) {
    val currentTalant = vm.talant.intValue

    var betInput by remember { mutableStateOf("") }
    var selectedOption by remember { mutableIntStateOf(-1) }
    var showResult by remember { mutableStateOf(false) }
    var investResult by remember { mutableIntStateOf(0) }
    var isSuccess by remember { mutableStateOf(false) }

    if (showResult) {
        ResultScreen(
            emoji = if (isSuccess) "📈" else "📉",
            title = if (isSuccess) "투자 성공!" else "투자 실패...",
            body = if (isSuccess) "원금 대비 ${GameData.investOptions[selectedOption].multiplier}배 수익!" else "원금 손실",
            amount = investResult,
            onClose = onBack
        )
        return
    }

    if (vm.isTagUsed("INVEST_$tagId")) {
        UsedTagScreen("이미 사용한 투자 태그입니다", onBack); return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("📈", fontSize = 24.sp)
            Spacer(Modifier.width(8.dp))
            Text(
                "투자 태그",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(Modifier.height(16.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("현재 달란트", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "💰 $currentTalant",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // 투자 등급 선택
        Text("투자 등급 선택", fontWeight = FontWeight.Bold, fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))

        GameData.investOptions.forEachIndexed { i, opt ->
            val selected = selectedOption == i
            Card(
                onClick = { selectedOption = i },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surface
                ),
                border = if (selected) CardDefaults.outlinedCardBorder() else null
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "${opt.emoji} ${opt.name}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(opt.description, fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        "× ${opt.multiplier}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // 베팅 금액
        OutlinedTextField(
            value = betInput,
            onValueChange = { betInput = it.filter { c -> c.isDigit() } },
            label = { Text("베팅 금액 (최소 50)") },
            suffix = { Text("달란트") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.weight(1f))

        val bet = betInput.toIntOrNull() ?: 0
        val canInvest = selectedOption >= 0 && bet >= 50 && bet <= currentTalant

        Button(
            onClick = {
                val opt = GameData.investOptions[selectedOption]
                val success = Random.nextFloat() < opt.successRate
                isSuccess = success
                investResult = if (success) {
                    (bet * opt.multiplier).roundToInt() - bet
                } else {
                    val refund = if (opt.name == "안전 투자") bet / 2 else 0
                    refund - bet
                }
                vm.addTalant(investResult)
                vm.markTagUsed("INVEST_$tagId")
                showResult = true
            },
            enabled = canInvest,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text("투자 실행!", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("돌아가기")
        }
    }
}
