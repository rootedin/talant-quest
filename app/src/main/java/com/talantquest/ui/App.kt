package com.talantquest.ui

import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.talantquest.data.GameViewModel
import com.talantquest.data.GameViewModelFactory
import com.talantquest.nfc.NfcHandler

@Composable
fun TalantQuestApp(
    nfcTagEvent: State<NfcHandler.TagData?>,
    onNfcConsumed: () -> Unit
) {
    val context = LocalContext.current
    val vm: GameViewModel = viewModel(factory = GameViewModelFactory(context))
    val navController = rememberNavController()

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

    val start = if (vm.hasTeam()) "main" else "setup"

    NavHost(navController = navController, startDestination = start) {
        composable("setup") {
            TeamSetupScreen(vm) {
                navController.navigate("main") {
                    popUpTo("setup") { inclusive = true }
                }
            }
        }
        composable("main") {
            MainScreen(vm, onReset = {
                vm.resetGame()
                navController.navigate("setup") { popUpTo(0) }
            })
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
