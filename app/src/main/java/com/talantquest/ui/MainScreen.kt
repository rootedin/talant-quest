package com.talantquest.ui

import android.content.Intent
import android.nfc.NfcAdapter
import android.provider.Settings
import androidx.compose.animation.core.*
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.talantquest.data.GameViewModel

private enum class NfcStatus { ENABLED, DISABLED, UNSUPPORTED }

@Composable
private fun rememberNfcStatus(): NfcStatus {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current

    fun check(): NfcStatus {
        val adapter = NfcAdapter.getDefaultAdapter(context) ?: return NfcStatus.UNSUPPORTED
        return if (adapter.isEnabled) NfcStatus.ENABLED else NfcStatus.DISABLED
    }

    var status by remember { mutableStateOf(check()) }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) status = check()
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    return status
}

@Composable
fun MainScreen(vm: GameViewModel, onReset: () -> Unit) {
    val context = LocalContext.current
    val teamName by vm.teamName
    val rawTalant by vm.talant
    val animatedTalant by animateIntAsState(
        targetValue = rawTalant,
        animationSpec = tween(700),
        label = "talant"
    )

    // tagsVersion 구독 → 진행도 갱신
    vm.tagsVersion.intValue
    val quizFound = vm.quizFound()
    val codeFound = vm.codeFound()
    val allFound = quizFound == vm.totalQuiz && codeFound == vm.totalCode

    val nfcStatus = rememberNfcStatus()
    var showResetDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))

        // 팀 이름 (길게 누르면 운영자 초기화)
        Text(
            teamName,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.pointerInput(Unit) {
                detectTapGestures(onLongPress = { showResetDialog = true })
            }
        )

        Spacer(Modifier.height(24.dp))

        // 달란트 카운터
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("보유 달란트", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Text(
                    "💰 $animatedTalant",
                    fontSize = 46.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text("달란트", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(16.dp))

        // 수집 진행도
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    if (allFound) "🏆 모든 보물을 찾았어요!" else "🗺️ 수집 진행도",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (allFound) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(14.dp))
                ProgressRow("❓ 퀴즈", quizFound, vm.totalQuiz)
                Spacer(Modifier.height(10.dp))
                ProgressRow("🔑 암호", codeFound, vm.totalCode)
            }
        }

        Spacer(Modifier.height(32.dp))

        // NFC 상태 영역
        when (nfcStatus) {
            NfcStatus.ENABLED -> NfcReadyIndicator()
            NfcStatus.DISABLED -> NfcWarningCard(
                title = "NFC가 꺼져 있습니다",
                message = "게임을 하려면 NFC를 켜주세요.",
                actionLabel = "NFC 설정 열기",
                onAction = {
                    runCatching { context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS)) }
                }
            )
            NfcStatus.UNSUPPORTED -> NfcWarningCard(
                title = "NFC 미지원 기기",
                message = "이 기기는 NFC를 지원하지 않아 태그를 읽을 수 없습니다.",
                actionLabel = null,
                onAction = {}
            )
        }

        Spacer(Modifier.height(24.dp))
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("게임 초기화") },
            text = { Text("팀 이름과 모은 달란트, 수집 기록이 모두 삭제됩니다.\n정말 초기화할까요?") },
            confirmButton = {
                TextButton(onClick = {
                    showResetDialog = false
                    onReset()
                }) { Text("초기화", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("취소") }
            }
        )
    }
}

@Composable
private fun ProgressRow(label: String, found: Int, total: Int) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(
                "$found / $total",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { if (total == 0) 0f else found.toFloat() / total },
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
private fun NfcReadyIndicator() {
    val pulse = rememberInfiniteTransition(label = "pulse")
    val scale by pulse.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    Box(
        modifier = Modifier
            .size(160.dp)
            .scale(scale)
            .border(
                width = 3.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("📡", fontSize = 48.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                "NFC 대기중",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
    }
    Spacer(Modifier.height(20.dp))
    Text(
        "태그에 기기를 가까이 대세요",
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun NfcWarningCard(
    title: String,
    message: String,
    actionLabel: String?,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("📵", fontSize = 40.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(Modifier.height(4.dp))
            Text(
                message,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null) {
                Spacer(Modifier.height(16.dp))
                Button(onClick = onAction) { Text(actionLabel) }
            }
        }
    }
}
