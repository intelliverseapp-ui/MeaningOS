package com.example.meaningosapp.ui.main

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StartListeningButton(
    modifier: Modifier = Modifier,
    isListening: Boolean,
    onClick: () -> Unit
) {
    // small random seed so repeated activations feel slightly different
    val seed = remember { (0..1000).random() / 1000f }

    val pulse: Float = if (isListening) {
        val infinite = rememberInfiniteTransition()
        val duration = (1100 + (seed * 200)).toInt()
        // use the animateFloat extension on InfiniteTransition (import added above)
        infinite.animateFloat(
            initialValue = 0.985f,
            targetValue = 1.045f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = duration, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            )
        ).value
    } else {
        animateFloatAsState(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
        ).value
    }

    val containerColor = Color(0xFF0B3D91)
    val glow = if (isListening) Color(0x33FF6B35) else Color.Transparent

    Button(
        onClick = { onClick() },
        colors = ButtonDefaults.buttonColors(containerColor = containerColor),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(84.dp)
            .scale(pulse)
            .background(glow, shape = RoundedCornerShape(12.dp))
    ) {
        Text(
            text = if (isListening) "Listening…" else "START LISTENING",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}
