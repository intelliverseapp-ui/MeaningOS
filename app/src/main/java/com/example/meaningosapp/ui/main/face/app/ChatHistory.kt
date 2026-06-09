package com.example.meaningosapp.ui.main.face.app

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// ⭐ FIXED: BabyNodeMode now comes from audio package
import com.example.meaningosapp.ui.main.face.audio.BabyNodeMode

@Composable
fun ChatHistory(
    messages: List<Pair<String, BabyNodeMode>>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        messages.forEach { (text, mode) ->
            ChatBubble(
                text = text,
                mode = mode
            )
        }
    }
}
