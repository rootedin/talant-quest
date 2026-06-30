package com.talantquest.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ErrorScreen(message: String, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("❌", fontSize = 56.sp)
        Spacer(Modifier.height(16.dp))
        Text("오류", fontSize = 33.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
        Text(message, fontSize = 21.sp, lineHeight = 32.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
        Spacer(Modifier.height(40.dp))
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth().height(72.dp)) {
            Text("돌아가기", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun UsedTagScreen(message: String, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🔒", fontSize = 56.sp)
        Spacer(Modifier.height(16.dp))
        Text("이미 사용됨", fontSize = 33.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(message, fontSize = 21.sp, lineHeight = 32.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
        Spacer(Modifier.height(40.dp))
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth().height(72.dp)) {
            Text("돌아가기", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ResultScreen(
    emoji: String,
    title: String,
    body: String,
    amount: Int,
    onClose: () -> Unit
) {
    val isGain = amount >= 0
    val context = LocalContext.current
    LaunchedEffect(Unit) { vibrate(context, isGain) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(emoji, fontSize = 72.sp)
            Spacer(Modifier.height(28.dp))
            Text(title, fontSize = 36.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            Text(body, fontSize = 22.sp, lineHeight = 34.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(Modifier.height(40.dp))
            val sign = if (amount > 0) "+" else ""
            Text(
                "$sign$amount 달란트",
                fontSize = 60.sp,
                fontWeight = FontWeight.Bold,
                color = if (isGain) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.height(48.dp))
            Button(onClick = onClose, modifier = Modifier.fillMaxWidth().height(72.dp)) {
                Text("메인으로 돌아가기", fontWeight = FontWeight.Bold, fontSize = 24.sp)
            }
        }
        if (isGain) ConfettiOverlay()
    }
}
