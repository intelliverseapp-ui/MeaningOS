package com.example.meaningosapp.ui.main.face.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import com.example.meaningosapp.ui.main.face.audio.BabyNodeMode
import com.example.meaningosapp.ui.main.face.audio.BabyEmotion

// Correct imports
import com.example.meaningosapp.ui.main.face.ui.BabyNodeFace
import com.example.meaningosapp.ui.main.face.ui.PreviewWaveformVisualizer
import com.example.meaningosapp.ui.main.face.ui.StartListeningButton

@Composable
fun BabyNodeScreen(
    mode: BabyNodeMode,
    partialText: String,
    finalText: String,
    reply: String,
    waveform: List<Int>,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit
) {
    val isListening = mode == BabyNodeMode.LISTENING

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        BabyNodeFace(
            mode = mode,
            emotion = BabyEmotion.NEUTRAL
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text("You said: $finalText")
        Text("Partial: $partialText")
        Text("Reply: $reply")

        Spacer(modifier = Modifier.height(24.dp))

        PreviewWaveformVisualizer()

        Spacer(modifier = Modifier.height(24.dp))

        StartListeningButton(
            isListening = isListening,
            onClick = if (isListening) onStopListening else onStartListening
        )
    }
}
