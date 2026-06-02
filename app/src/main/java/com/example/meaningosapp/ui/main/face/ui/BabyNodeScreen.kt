package com.example.meaningosapp.ui.main.face.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// ⭐ Correct imports
import com.example.meaningosapp.ui.main.face.viewmodel.BabyNodeViewModel
import com.example.meaningosapp.ui.main.face.ui.BabyNodeFace
import com.example.meaningosapp.ui.main.face.ui.BabyNodeMode
import com.example.meaningosapp.ui.main.face.ui.ThinkingPulse
import com.example.meaningosapp.ui.main.face.ui.StartListeningButton

@Composable
fun BabyNodeScreen(
    viewModel: BabyNodeViewModel
) {
    val mode by viewModel.mode.collectAsState()
    val emotion by viewModel.emotion.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    val isThinking by viewModel.isThinking.collectAsState()
    val finalText by viewModel.finalText.collectAsState()
    val nodeReply by viewModel.nodeReply.collectAsState()
    val waveform by viewModel.waveform.collectAsState()
    val recordingTime by viewModel.recordingTime.collectAsState()
    val debug by viewModel.debug.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = "Baby Node",
                style = MaterialTheme.typography.headlineMedium
            )

            // ⭐ Face + animations
            Box(contentAlignment = Alignment.Center) {

                ThinkingPulse(isThinking = isThinking)

                BreathingIdleAnimation(isIdle = mode == BabyNodeMode.IDLE) {
                    BabyNodeFace(
                        modifier = Modifier,
                        mode = mode,
                        emotion = emotion
                    )
                }
            }

            // ⭐ Waveform
            WaveformVisualizer(samples = waveform)

            if (recordingTime > 0) {
                Text(
                    text = "Recording: ${recordingTime}s",
                    color = Color.Red,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // ⭐ Text output
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
            ) {
                Text(text = "You said:", color = Color.Gray)
                Text(
                    text = finalText,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Text(text = "Baby Node replied:", color = Color.Gray)
                Text(text = nodeReply, color = Color.White)
            }

            // ⭐ Debug line
            if (debug.isNotBlank()) {
                Text(
                    text = debug,
                    color = Color(0xFF9C27B0),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // ⭐ Start/Stop button — FIXED
            StartListeningButton(
                isListening = isListening,
                onClick = {
                    if (isListening) viewModel.onStopListening()
                    else viewModel.onStartListening()
                }
            )
        }
    }
}

//////////////////////////////////////////////////////////////
// BREATHING WRAPPER
//////////////////////////////////////////////////////////////

@Composable
fun BreathingIdleAnimation(isIdle: Boolean, content: @Composable () -> Unit) {
    val scale by animateFloatAsState(
        targetValue = if (isIdle) 1.05f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing"
    )

    Box(modifier = Modifier.scale(scale)) {
        content()
    }
}

//////////////////////////////////////////////////////////////
// WAVEFORM VISUALIZER
//////////////////////////////////////////////////////////////

@Composable
fun WaveformVisualizer(samples: List<Int>) {
    val maxAmp = (samples.maxOrNull() ?: 1).toFloat()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        samples.takeLast(60).forEach { amp ->
            val barHeight = (amp / maxAmp) * 40f

            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(barHeight.dp.coerceAtLeast(2.dp))
                    .padding(horizontal = 1.dp)
                    .background(Color(0xFF9C27B0), RoundedCornerShape(2.dp))
            )
        }
    }
}
