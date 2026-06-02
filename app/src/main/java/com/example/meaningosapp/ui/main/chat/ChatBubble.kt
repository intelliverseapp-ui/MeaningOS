package com.example.meaningosapp.ui.main.chat

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ⭐ Correct BabyNodeMode import
import com.example.meaningosapp.ui.main.face.ui.BabyNodeMode

// Your theme + models
import com.example.meaningosapp.ui.theme.*
import com.example.meaningosapp.ui.model.ChatMessage
import com.example.meaningosapp.ui.model.Sender

@Composable
fun ChatBubble(
    message: ChatMessage,
    mode: BabyNodeMode
) {
    val isUser = message.sender == Sender.USER

    val scale by animateFloatAsState(
        targetValue = if (!isUser) 1.05f else 1.0f,
        animationSpec = tween(240, easing = FastOutSlowInEasing),
        label = "bubbleScale"
    )

    // ---------------------------------------------------------
    // ⭐ BUBBLE COLOR (exhaustive)
    // ---------------------------------------------------------
    val bubbleColor = if (isUser) {
        UserBubbleBlue
    } else {
        when (mode) {
            BabyNodeMode.IDLE -> BabyPurple
            BabyNodeMode.LISTENING -> BabyBlue
            BabyNodeMode.PROCESSING -> BabyGreen
            BabyNodeMode.THINKING -> BabyGreen
            BabyNodeMode.SPEAKING -> BabyGreen
            BabyNodeMode.TYPING -> BabyGreen
            BabyNodeMode.ERROR -> BabyPurple
        }
    }

    // ---------------------------------------------------------
    // ⭐ GLOW COLOR (exhaustive)
    // ---------------------------------------------------------
    val glowColor = if (isUser) {
        UserBubbleBlue.copy(alpha = 0.25f)
    } else {
        when (mode) {
            BabyNodeMode.IDLE -> BabyPurple.copy(alpha = 0.25f)
            BabyNodeMode.LISTENING -> BabyBlue.copy(alpha = 0.25f)
            BabyNodeMode.PROCESSING -> BabyGreen.copy(alpha = 0.25f)
            BabyNodeMode.THINKING -> BabyGreen.copy(alpha = 0.25f)
            BabyNodeMode.SPEAKING -> BabyGreen.copy(alpha = 0.25f)
            BabyNodeMode.TYPING -> BabyGreen.copy(alpha = 0.25f)
            BabyNodeMode.ERROR -> BabyPurple.copy(alpha = 0.25f)
        }
    }

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
                RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp)
            else
                RoundedCornerShape(22.dp),
            color = bubbleColor
        ) {

            val bubbleModifier = if (!isUser) {
                Modifier.background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            glowColor,
                            androidx.compose.ui.graphics.Color.Transparent
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
                    color = TextOnDark
                )
            }
        }
    }
}
