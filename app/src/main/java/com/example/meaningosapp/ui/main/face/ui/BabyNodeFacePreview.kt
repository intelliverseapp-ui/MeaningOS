package com.example.meaningosapp.ui.main.face.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.meaningosapp.ui.main.face.audio.BabyNodeMode
import com.example.meaningosapp.ui.main.face.audio.BabyEmotion
import kotlin.math.sin

// ⭐ REQUIRED COMPOSE ANIMATION IMPORTS
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat   // ← THIS was the missing one

/**
 * Fake waveform generator for previews only.
 * Produces a smooth animated waveform so you can see the UI in Studio.
 */
@Composable
fun PreviewWaveformVisualizer() {
    val infinite = rememberInfiniteTransition(label = "waveform")

    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f, // 2π
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave-phase"
    )

    // Generate 60 fake samples
    val samples = List(60) { i ->
        val x = i / 60f
        val wave = sin((x * 6.28f) + phase)
        ((wave + 1f) * 40f).toInt() // convert to 0–80 range
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .padding(horizontal = 8.dp)
    ) {
        val barWidth = size.width / samples.size

        samples.forEachIndexed { index, amp ->
            val barHeight = amp.toFloat()

            drawRect(
                color = Color(0xFF9C27B0),
                topLeft = androidx.compose.ui.geometry.Offset(
                    x = index * barWidth,
                    y = size.height - barHeight
                ),
                size = androidx.compose.ui.geometry.Size(
                    width = barWidth * 0.6f,
                    height = barHeight
                ),
                style = Fill
            )
        }
    }
}

@Preview(name = "Baby Node – Idle + Waveform", showBackground = true)
@Composable
fun BabyNodeFaceIdleWithWaveformPreview() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 40.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BabyNodeFace(
            mode = BabyNodeMode.IDLE,
            emotion = BabyEmotion.NEUTRAL
        )

        Spacer(modifier = Modifier.height(24.dp))

        PreviewWaveformVisualizer()
    }
}

@Preview(name = "Baby Node – Listening + Waveform", showBackground = true)
@Composable
fun BabyNodeFaceListeningWithWaveformPreview() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 40.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BabyNodeFace(
            mode = BabyNodeMode.LISTENING,
            emotion = BabyEmotion.NEUTRAL
        )

        Spacer(modifier = Modifier.height(24.dp))

        PreviewWaveformVisualizer()
    }
}

@Preview(name = "Baby Node – Speaking + Waveform", showBackground = true)
@Composable
fun BabyNodeFaceSpeakingWithWaveformPreview() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 40.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BabyNodeFace(
            mode = BabyNodeMode.SPEAKING,
            emotion = BabyEmotion.HAPPY
        )

        Spacer(modifier = Modifier.height(24.dp))

        PreviewWaveformVisualizer()
    }
}
