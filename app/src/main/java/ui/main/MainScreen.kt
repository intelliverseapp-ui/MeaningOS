package com.example.meaningosapp.ui.main

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MainScreen(
    youSaid: String,
    meaningText: String,
    isListening: Boolean,
    onStartListening: () -> Unit
) {
    val background = Brush.verticalGradient(
        colors = listOf(Color(0xFFF7FBFF), Color(0xFFEAF2FF))
    )

    Surface(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .background(background)
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.TopCenter),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // StartListeningButton is expected to be in the same package (StartListeningButton.kt)
                StartListeningButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(84.dp),
                    isListening = isListening,
                    onClick = onStartListening
                )

                Spacer(modifier = Modifier.height(28.dp))

                ResponseCard(
                    youSaid = youSaid,
                    meaning = meaningText
                )
            }
        }
    }
}

@Composable
private fun ResponseCard(
    youSaid: String,
    meaning: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(8.dp)
            .background(Color.White, shape = RoundedCornerShape(12.dp))
            .padding(18.dp)
    ) {
        Text(
            text = "You said:",
            color = Color(0xFF6B6B6B),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = youSaid.ifBlank { "—" },
            color = Color(0xFF333333),
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(14.dp))

        val parts = splitFirstWord(meaning)
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (parts.first.isNotBlank()) {
                Text(
                    text = parts.first,
                    color = Color(0xFF0B3D91),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = parts.second,
                color = Color(0xFF333333),
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun splitFirstWord(text: String): Pair<String, String> {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return "" to ""
    val idx = trimmed.indexOf(' ')
    return if (idx <= 0) {
        trimmed to ""
    } else {
        trimmed.substring(0, idx).replaceFirstChar { it.uppercase() } to trimmed.substring(idx + 1)
    }
}
