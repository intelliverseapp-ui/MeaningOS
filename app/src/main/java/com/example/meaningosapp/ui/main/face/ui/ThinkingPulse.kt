package com.example.meaningosapp.ui.main.face.ui


import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun ThinkingPulse(isThinking: Boolean) {

    if (!isThinking) return

    val infinite = rememberInfiniteTransition(label = "think")

    val alpha by infinite.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "think-alpha"
    )

    val radiusScale by infinite.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "think-scale"
    )

    Canvas(modifier = Modifier.size(220.dp)) {
        drawCircle(
            color = Color(0xFFB388FF).copy(alpha = alpha),
            radius = size.minDimension / 2 * radiusScale,
            style = Stroke(width = 6.dp.toPx())
        )
    }
}
