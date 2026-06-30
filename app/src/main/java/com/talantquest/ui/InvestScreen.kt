package com.talantquest.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.talantquest.data.GameData
import com.talantquest.data.GameViewModel
import kotlin.random.Random

@Composable
fun InvestScreen(vm: GameViewModel, tagId: String, onBack: () -> Unit) {
    val currentTalant = vm.talant.intValue

    var selectedOption by remember { mutableIntStateOf(-1) }
    var showResult by remember { mutableStateOf(false) }
    var investResult by remember { mutableIntStateOf(0) }
    var isSuccess by remember { mutableStateOf(false) }

    if (showResult) {
        val opt = GameData.investOptions[selectedOption]
        ResultScreen(
            emoji = if (isSuccess) "📈" else "📉",
            title = if (isSuccess) "투자 성공!" else "투자 실패...",
            body = if (isSuccess) "${opt.betAmount}달란트를 걸어 ${opt.betAmount}달란트 추가 획득!" else "${opt.betAmount}달란트 손실",
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
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(Modifier.height(16.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("현재 달란트", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "🟡 $currentTalant",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("성공 확률", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "🎯 50%",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Text(
            "투자 금액 선택",
            fontWeight = FontWeight.Bold,
            fontSize = 21.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))

        // 카드·요약만 스크롤 → 하단 버튼은 항상 화면에 고정
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
        GameData.investOptions.forEachIndexed { i, opt ->
            val selected = selectedOption == i
            val canAfford = currentTalant >= opt.betAmount
            Card(
                onClick = { if (canAfford) selectedOption = i },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        selected -> MaterialTheme.colorScheme.primaryContainer
                        !canAfford -> MaterialTheme.colorScheme.surfaceVariant
                        else -> MaterialTheme.colorScheme.surface
                    }
                ),
                border = if (selected) CardDefaults.outlinedCardBorder() else null
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Text(
                        "${opt.emoji} ${opt.name}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = if (!canAfford) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        opt.description,
                        fontSize = 17.sp,
                        lineHeight = 26.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "${opt.betAmount} 달란트",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (!canAfford) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "성공 시 × 2",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            color = if (!canAfford) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.tertiary
                        )
                        if (!canAfford) {
                            Text("달란트 부족", fontSize = 14.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        } // 스크롤 영역 끝

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = {
                val opt = GameData.investOptions[selectedOption]
                val success = Random.nextFloat() < GameData.INVEST_SUCCESS_RATE
                isSuccess = success
                investResult = if (success) opt.betAmount else -opt.betAmount
                vm.addTalant(investResult)
                vm.markTagUsed("INVEST_$tagId")
                showResult = true
            },
            enabled = selectedOption >= 0,
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
        ) {
            Text("투자 실행!", fontWeight = FontWeight.Bold, fontSize = 24.sp)
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("돌아가기")
        }
    }
}
