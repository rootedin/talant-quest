package com.talantquest.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.random.Random

private data class Confetto(
    val xFrac: Float,
    val color: Color,
    val size: Float,
    val drift: Float,
    val rotSpeed: Float,
    val delay: Float,
    val startYFrac: Float
)

/**
 * 화면 위에서 색종이가 쏟아져 내리는 1회성 축하 효과.
 * 부모 Box의 맨 위에 올려서 사용한다. (포인터 이벤트를 가로채지 않음)
 */
@Composable
fun ConfettiOverlay(particleCount: Int = 90) {
    val colors = listOf(
        Color(0xFFF5C842), Color(0xFFAB8EFF), Color(0xFF66BB6A),
        Color(0xFF42A5F5), Color(0xFFFF7043), Color(0xFFFFFFFF)
    )
    val particles = remember {
        List(particleCount) {
            Confetto(
                xFrac = Random.nextFloat(),
                color = colors[Random.nextInt(colors.size)],
                size = 8f + Random.nextFloat() * 12f,
                drift = (Random.nextFloat() - 0.5f) * 0.4f,
                rotSpeed = (Random.nextFloat() - 0.5f) * 2f,
                delay = Random.nextFloat() * 0.25f,
                startYFrac = -0.15f - Random.nextFloat() * 0.25f
            )
        }
    }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, animationSpec = tween(2600, easing = LinearEasing))
    }
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val p = progress.value
        particles.forEach { c ->
            val t = ((p - c.delay) / (1f - c.delay)).coerceIn(0f, 1f)
            if (t <= 0f) return@forEach
            val y = (c.startYFrac + t * 1.35f) * h
            val x = (c.xFrac + c.drift * t) * w
            val alpha = (1f - t * t).coerceIn(0f, 1f)
            rotate(degrees = c.rotSpeed * t * 360f, pivot = Offset(x, y)) {
                drawRect(
                    color = c.color.copy(alpha = alpha),
                    topLeft = Offset(x - c.size / 2f, y - c.size / 2f),
                    size = Size(c.size, c.size * 0.6f)
                )
            }
        }
    }
}
