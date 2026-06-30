package com.talantquest.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.talantquest.data.GameData
import com.talantquest.data.GameViewModel

@Composable
fun EventScreen(vm: GameViewModel, tagId: String, onBack: () -> Unit) {
    val usedKey = "EVENT_$tagId"
    val wasAlreadyUsed = remember { vm.isTagUsed(usedKey) }

    if (wasAlreadyUsed) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("🚫", fontSize = 56.sp)
            Spacer(Modifier.height(16.dp))
            Text("이미 사용한 태그", fontSize = 33.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary)
            Text("이 이벤트 태그는 이미 사용했습니다.",
                fontSize = 21.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
            Spacer(Modifier.height(32.dp))
            Button(onClick = onBack, modifier = Modifier.fillMaxWidth().height(72.dp)) {
                Text("돌아가기", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
        return
    }

    val event = remember { GameData.getRandomEvent() }
    val isGain = event.amount > 0
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        vm.addTalant(event.amount)
        vm.markTagUsed(usedKey)
        vibrate(context, isGain)
    }

    val bounce = rememberInfiniteTransition(label = "bounce")
    val scale by bounce.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(if (isGain) "🎉" else "😱", fontSize = 72.sp, modifier = Modifier.scale(scale))

            Spacer(Modifier.height(24.dp))

            Text(
                event.description,
                fontSize = 33.sp,
                lineHeight = 48.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(16.dp))

            Text(
                event.flavor,
                fontSize = 21.sp,
                lineHeight = 32.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(40.dp))

            val amountText = if (isGain) "+${event.amount}" else "${event.amount}"
            Text(
                "$amountText 달란트",
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold,
                color = if (isGain) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "현재 잔액: ${vm.talant.intValue} 달란트",
                fontSize = 21.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(56.dp))

            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth().height(72.dp)
            ) {
                Text("메인으로 돌아가기", fontWeight = FontWeight.Bold, fontSize = 24.sp)
            }
        }
        if (isGain) ConfettiOverlay()
    }
}
