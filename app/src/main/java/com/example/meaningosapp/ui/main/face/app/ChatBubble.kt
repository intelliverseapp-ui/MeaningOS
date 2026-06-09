package com.example.meaningosapp.ui.main.face.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color

// BabyNodeMode
import com.example.meaningosapp.ui.main.face.audio.BabyNodeMode

// Baby colors
import com.example.meaningosapp.ui.theme.BabyPurple
import com.example.meaningosapp.ui.theme.BabyBlue
import com.example.meaningosapp.ui.theme.BabyGreen

@Composable
fun ChatBubble(
    text: String,
    mode: BabyNodeMode
) {
    val bubbleColor = when (mode) {
        BabyNodeMode.IDLE -> BabyPurple
        BabyNodeMode.LISTENING -> BabyBlue
        BabyNodeMode.PROCESSING -> BabyGreen
        BabyNodeMode.THINKING -> BabyGreen
        BabyNodeMode.SPEAKING -> BabyGreen
        BabyNodeMode.TYPING -> BabyGreen
        BabyNodeMode.ERROR -> BabyPurple
    }

    Box(
        modifier = Modifier
            .padding(8.dp)
            .background(color = bubbleColor)
            .padding(12.dp)
    ) {
        Text(text)
    }
}
