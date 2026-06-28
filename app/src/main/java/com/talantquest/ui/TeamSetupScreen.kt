package com.talantquest.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.talantquest.data.GameViewModel

@Composable
fun TeamSetupScreen(vm: GameViewModel, onDone: () -> Unit) {
    var name by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🏆", fontSize = 64.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            "달란트 퀘스트",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            "NFC 태그를 찾아 달란트를 모으세요!",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
        Spacer(Modifier.height(48.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("팀 이름") },
            placeholder = { Text("예: 믿음팀") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                if (name.isNotBlank()) {
                    vm.setTeamName(name.trim())
                    onDone()
                }
            },
            enabled = name.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text("게임 시작!", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}
