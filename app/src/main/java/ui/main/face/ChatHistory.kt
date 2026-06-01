package com.example.meaningosapp.ui.main.chat

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ⭐ Data class for each chat message
data class ChatMessage(
    val text: String,
    val fromUser: Boolean
)

// ⭐ Chat bubble composable
@Composable
fun ChatBubble(message: ChatMessage) {

    val isUser = message.fromUser

    // ⭐ Baby Node bubble bounce animation
    val scale by animateFloatAsState(
        targetValue = if (!isUser) 1.05f else 1.0f,
        animationSpec = tween(240, easing = FastOutSlowInEasing),
        label = "bubbleScale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {

        Surface(
            modifier = Modifier
                .scale(scale)
                .widthIn(max = 280.dp),
            shape = if (isUser)
                RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp) // ⭐ Hybrid: medium-round for user
            else
                RoundedCornerShape(22.dp), // ⭐ Fully rounded pill for Baby Node
            color = if (isUser)
                Color(0xFF4A90E2) // Blue bubble
            else
                Color(0xFF9B6BFF) // Baby Node purple
        ) {

            // ⭐ Baby Node bubble glow
            val bubbleModifier = if (!isUser) {
                Modifier.background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x885F3CFF),
                            Color.Transparent
                        )
                    )
                )
            } else Modifier

            Box(
                modifier = bubbleModifier.padding(14.dp)
            ) {
                Text(
                    text = message.text,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isUser) Color.White else Color.White
                )
            }
        }
    }
}
