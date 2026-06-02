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
fun ListeningRing(isListening: Boolean) {

    val infinite = rememberInfiniteTransition(label = "listen")

    val scale by infinite.animateFloat(
        initialValue = if (isListening) 0.9f else 1f,
        targetValue = if (isListening) 1.1f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "listen-scale"
    )

    if (!isListening) return

    Canvas(modifier = Modifier.size(200.dp)) {
        drawCircle(
            color = Color(0xFF4DA3FF),
            radius = size.minDimension / 2 * scale,
            style = Stroke(width = 8.dp.toPx())
        )
    }
}
