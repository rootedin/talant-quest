package com.talantquest.ui

import android.content.Intent
import android.nfc.NfcAdapter
import android.provider.Settings
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.talantquest.data.GameData
import com.talantquest.nfc.NfcWriteController
import com.talantquest.nfc.WriteResult

private data class TagTypeInfo(val code: String, val label: String, val max: Int?)

@Composable
fun AdminScreen(
    controller: NfcWriteController,
    onReset: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current

    val types = listOf(
        TagTypeInfo("QUIZ", "퀴즈", 10),
        TagTypeInfo("CODE", "암호", 10),
        TagTypeInfo("INVEST", "투자", null),
        TagTypeInfo("EVENT", "이벤트", null),
    )
    var typeIndex by remember { mutableIntStateOf(0) }
    var number by remember { mutableIntStateOf(1) }
    var autoIncrement by remember { mutableStateOf(true) }
    var armed by remember { mutableStateOf(false) }
    var lastResult by remember { mutableStateOf<WriteResult?>(null) }
    var writtenCount by remember { mutableIntStateOf(0) }
    var showResetDialog by remember { mutableStateOf(false) }

    // NFC 활성 여부 (resume 시 재확인)
    var nfcEnabled by remember { mutableStateOf(false) }
    DisposableEffect(owner) {
        fun check() {
            val a = NfcAdapter.getDefaultAdapter(context)
            nfcEnabled = a != null && a.isEnabled
        }
        check()
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) check()
        }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }

    val type = types[typeIndex]
    val payload = "TALANT:${type.code}:" + "%02d".format(number)
    val payloadState = rememberUpdatedState(payload)

    // 화면 이탈 시 쓰기 세션 정리
    DisposableEffect(Unit) {
        onDispose { controller.stopWriteSession() }
    }

    fun startSession() {
        armed = true
        lastResult = null
        controller.startWriteSession({ payloadState.value }) { result ->
            lastResult = result
            if (result is WriteResult.Success) {
                writtenCount++
                if (autoIncrement) {
                    val max = types[typeIndex].max
                    if (max == null || number < max) number++
                }
            }
        }
    }

    fun stopSession() {
        armed = false
        controller.stopWriteSession()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🔧", fontSize = 24.sp)
            Spacer(Modifier.width(8.dp))
            Text("관리자 모드", fontSize = 20.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(4.dp))
        Text("빈 NFC 태그에 게임 데이터를 직접 기록합니다.",
            fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(Modifier.height(20.dp))

        if (!nfcEnabled) {
            Card(colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )) {
                Column(Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("NFC가 꺼져 있습니다", fontSize = 15.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer)
                    Spacer(Modifier.height(4.dp))
                    Text("태그를 기록하려면 NFC를 켜주세요.", fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = {
                        runCatching { context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS)) }
                    }) { Text("NFC 설정 열기") }
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        // 태그 종류
        Text("태그 종류", fontSize = 13.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            types.forEachIndexed { i, t ->
                FilterChip(
                    selected = typeIndex == i,
                    onClick = {
                        if (!armed) {
                            typeIndex = i
                            number = if (t.max != null) minOf(number, t.max) else number
                            lastResult = null
                        }
                    },
                    label = { Text(t.label, fontSize = 13.sp) },
                    enabled = !armed,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // 번호
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("번호", fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    if (type.max != null) "준비된 콘텐츠: 01 ~ %02d".format(type.max)
                    else "자유 번호 (위치 수만큼)",
                    fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilledTonalButton(
                    onClick = { if (number > 1) number-- },
                    enabled = !armed && number > 1,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.size(44.dp)
                ) { Text("−", fontSize = 22.sp) }
                Text("%02d".format(number), fontSize = 24.sp, fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center, modifier = Modifier.widthIn(min = 56.dp))
                FilledTonalButton(
                    onClick = { number++ },
                    enabled = !armed && (type.max == null || number < type.max),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.size(44.dp)
                ) { Text("+", fontSize = 22.sp) }
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = autoIncrement, onCheckedChange = { autoIncrement = it })
            Spacer(Modifier.width(8.dp))
            Text("성공할 때마다 번호 자동 +1", fontSize = 13.sp)
        }

        Spacer(Modifier.height(16.dp))

        // 기록 미리보기
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text("기록될 내용", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text(payload, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(Modifier.height(12.dp))

        // 태그 내용 미리보기
        TagContentPreview(typeCode = type.code, number = number)

        Spacer(Modifier.height(16.dp))

        if (!armed) {
            Button(
                onClick = { startSession() },
                enabled = nfcEnabled,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("📡 태그 쓰기 시작", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            WaitingIndicator()
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = { stopSession() },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("쓰기 중지", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        lastResult?.let { result ->
            Spacer(Modifier.height(16.dp))
            val success = result is WriteResult.Success
            val msg = when (result) {
                is WriteResult.Success -> "기록 완료: ${result.payload}"
                is WriteResult.Failure -> "실패: ${result.reason}"
            }
            Card(colors = CardDefaults.cardColors(
                containerColor = if (success) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.errorContainer
            )) {
                Row(Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(if (success) "✅" else "❌", fontSize = 20.sp)
                    Spacer(Modifier.width(10.dp))
                    Text(msg, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                        color = if (success) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        }

        if (writtenCount > 0) {
            Spacer(Modifier.height(8.dp))
            Text("이번 세션에서 기록한 태그: ${writtenCount}개",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(Modifier.height(28.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))

        OutlinedButton(
            onClick = { showResetDialog = true },
            enabled = !armed,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) { Text("게임 초기화") }

        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("← 게임으로 돌아가기")
        }
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
private fun TagContentPreview(typeCode: String, number: Int) {
    val id = "%02d".format(number)
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Text(
                "태그 내용 미리보기",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            when (typeCode) {
                "QUIZ" -> {
                    val tag = GameData.getQuizTag(id)
                    if (tag != null) {
                        tag.questions.forEachIndexed { i, q ->
                            Text("Q${i + 1}. ${q.question}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            q.options.forEachIndexed { oi, opt ->
                                val marker = if (oi == q.correctIndex) "✓" else "·"
                                Text(
                                    "   $marker $opt",
                                    fontSize = 11.sp,
                                    color = if (oi == q.correctIndex)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (i < tag.questions.lastIndex) Spacer(Modifier.height(6.dp))
                        }
                    } else {
                        Text("해당 번호($id)의 퀴즈 데이터가 없습니다.", fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error)
                    }
                }
                "CODE" -> {
                    val tag = GameData.getCodeTag(id)
                    if (tag != null) {
                        Row {
                            Text("📍 위치: ", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Text(tag.hint, fontSize = 12.sp)
                        }
                        Spacer(Modifier.height(4.dp))
                        Row {
                            Text("🔑 암호: ", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Text(tag.answer, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(4.dp))
                        Row {
                            Text("💰 보상: ", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Text("${tag.reward} 달란트", fontSize = 12.sp)
                        }
                    } else {
                        Text("해당 번호($id)의 암호 데이터가 없습니다.", fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error)
                    }
                }
                "INVEST" -> {
                    Text("태그를 찍으면 투자 화면으로 이동합니다.", fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    GameData.investOptions.forEach { opt ->
                        Text("${opt.emoji} ${opt.name} — ${opt.description}", fontSize = 11.sp)
                    }
                }
                "EVENT" -> {
                    Text("태그를 찍으면 이벤트 ${GameData.eventPool.size}개 중 랜덤으로 한 가지가 발생합니다.",
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    GameData.eventPool.forEach { e ->
                        val sign = if (e.amount >= 0) "+" else ""
                        Text("• ${e.description} ($sign${e.amount})", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun WaitingIndicator() {
    val pulse = rememberInfiniteTransition(label = "pulse")
    val scale by pulse.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text("📡", fontSize = 40.sp, modifier = Modifier.scale(scale))
            Spacer(Modifier.height(8.dp))
            Text("빈 NFC 태그를 기기 뒷면에 대세요", fontSize = 14.sp,
                fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
            Text("기록될 때까지 태그를 떼지 마세요", fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
