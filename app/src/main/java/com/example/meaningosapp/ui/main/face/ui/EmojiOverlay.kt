package com.example.meaningosapp.ui.main.face.ui


import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp

@Composable
fun EmojiOverlay(
    emoji: String?,
    modifier: Modifier = Modifier
) {
    if (emoji == null) return

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = emoji,
            fontSize = 64.sp
        )
    }
}
