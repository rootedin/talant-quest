package com.talantquest.ui

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/** 결과 화면에서 가벼운 진동 피드백. success=true면 짧은 두 번, false면 길게 한 번. */
fun vibrate(context: Context, success: Boolean) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        manager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    } ?: return

    if (!vibrator.hasVibrator()) return

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val effect = if (success) {
            VibrationEffect.createWaveform(longArrayOf(0, 50, 70, 50), -1)
        } else {
            VibrationEffect.createOneShot(250, VibrationEffect.DEFAULT_AMPLITUDE)
        }
        vibrator.vibrate(effect)
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(if (success) 120L else 250L)
    }
}
