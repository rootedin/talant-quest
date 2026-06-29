package com.talantquest.ui

import android.content.Intent
import android.net.Uri
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.talantquest.data.GameViewModel
import com.talantquest.data.GameViewModelFactory
import com.talantquest.nfc.NfcHandler
import com.talantquest.nfc.NfcWriteController
import java.util.Locale

@Composable
fun TalantQuestApp(
    nfcTagEvent: State<NfcHandler.TagData?>,
    onNfcConsumed: () -> Unit,
    writeController: NfcWriteController
) {
    val context = LocalContext.current
    val vm: GameViewModel = viewModel(factory = GameViewModelFactory(context))
    val navController = rememberNavController()

    // TTS 엔진 체크
    var ttsWarning by remember { mutableStateOf<String?>(null) }
    DisposableEffect(Unit) {
        val ref = arrayOfNulls<TextToSpeech>(1)
        ref[0] = TextToSpeech(context) { status ->
            if (status != TextToSpeech.SUCCESS) {
                ttsWarning = "TTS 엔진을 초기화할 수 없습니다.\n퀴즈 음성 읽기 기능이 작동하지 않습니다."
            } else {
                val lang = ref[0]?.setLanguage(Locale.KOREAN) ?: TextToSpeech.LANG_NOT_SUPPORTED
                if (lang == TextToSpeech.LANG_MISSING_DATA || lang == TextToSpeech.LANG_NOT_SUPPORTED) {
                    ttsWarning = "한국어 TTS 데이터가 없습니다.\n구글 TTS 앱에서 한국어를 다운로드해야\n퀴즈 음성 읽기 기능을 사용할 수 있습니다."
                }
            }
        }
        onDispose { ref[0]?.shutdown() }
    }

    ttsWarning?.let { msg ->
        AlertDialog(
            onDismissRequest = { ttsWarning = null },
            title = { Text("음성 기능 안내") },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = {
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW,
                                Uri.parse("market://details?id=com.google.android.tts"))
                        )
                    }
                    ttsWarning = null
                }) { Text("TTS 설치") }
            },
            dismissButton = {
                TextButton(onClick = { ttsWarning = null }) { Text("닫기") }
            }
        )
    }

    val tag = nfcTagEvent.value
    LaunchedEffect(tag) {
        if (tag == null) return@LaunchedEffect
        // 팀 설정 전에는 NFC 스캔 무시 (설정 건너뛰기 방지)
        if (!vm.hasTeam()) {
            onNfcConsumed()
            return@LaunchedEffect
        }
        val route = when (tag.type) {
            NfcHandler.TagType.QUIZ -> "quiz/${tag.id}"
            NfcHandler.TagType.CODE -> "code/${tag.id}"
            NfcHandler.TagType.INVEST -> "invest/${tag.id}"
            NfcHandler.TagType.EVENT -> "event/${tag.id}"
            NfcHandler.TagType.UNKNOWN -> null
        }
        if (route == null) {
            Toast.makeText(context, "알 수 없는 태그입니다", Toast.LENGTH_SHORT).show()
        } else {
            // 이전 미션 화면 위에 쌓이지 않도록 메인 기준으로 정리
            navController.navigate(route) {
                popUpTo("main")
                launchSingleTop = true
            }
        }
        onNfcConsumed()
    }

    var showAdminPin by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    if (showAdminPin) {
        AlertDialog(
            onDismissRequest = {
                showAdminPin = false
                pinInput = ""
                pinError = false
            },
            title = { Text("관리자 인증") },
            text = {
                Column {
                    Text("PIN 번호를 입력하세요.")
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { pinInput = it; pinError = false },
                        label = { Text("PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        isError = pinError,
                        supportingText = if (pinError) ({ Text("PIN이 올바르지 않습니다.") }) else null,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (pinInput == "1004") {
                        showAdminPin = false
                        pinInput = ""
                        pinError = false
                        navController.navigate("admin")
                    } else {
                        pinError = true
                        pinInput = ""
                    }
                }) { Text("확인") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAdminPin = false
                    pinInput = ""
                    pinError = false
                }) { Text("취소") }
            }
        )
    }

    val start = if (vm.hasTeam()) "main" else "setup"

    NavHost(navController = navController, startDestination = start) {
        composable("setup") {
            TeamSetupScreen(
                vm,
                onAdmin = { showAdminPin = true },
                onDone = {
                    navController.navigate("main") {
                        popUpTo("setup") { inclusive = true }
                    }
                }
            )
        }
        composable("main") {
            MainScreen(vm, onAdmin = { showAdminPin = true })
        }
        composable("admin") {
            AdminScreen(
                controller = writeController,
                onReset = {
                    vm.resetGame()
                    navController.navigate("setup") { popUpTo(0) }
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable("quiz/{tagId}") { back ->
            val tagId = back.arguments?.getString("tagId") ?: return@composable
            QuizScreen(vm, tagId) { navController.popBackStack() }
        }
        composable("code/{tagId}") { back ->
            val tagId = back.arguments?.getString("tagId") ?: return@composable
            CodeHuntScreen(vm, tagId) { navController.popBackStack() }
        }
        composable("invest/{tagId}") { back ->
            val tagId = back.arguments?.getString("tagId") ?: return@composable
            InvestScreen(vm, tagId) { navController.popBackStack() }
        }
        composable("event/{tagId}") { back ->
            val tagId = back.arguments?.getString("tagId") ?: return@composable
            EventScreen(vm, tagId) { navController.popBackStack() }
        }
    }
}
