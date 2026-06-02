package com.example.meaningosapp.ui.main.face.ui


import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.meaningosapp.R
import com.example.meaningosapp.ui.theme.*

@Composable
fun StartListeningButton(
    isListening: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val seed = remember { (0..1000).random() / 1000f }

    val pulse: Float = if (isListening) {
        val infinite = rememberInfiniteTransition()
        val duration = (1100 + (seed * 200)).toInt()
        infinite.animateFloat(
            initialValue = 0.92f,
            targetValue = 1.08f,
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

    val glow = if (isListening) GlowMeaning else Color.Transparent

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .size(84.dp)
                .scale(pulse)
                .background(glow, CircleShape),
            shape = CircleShape,
            color = BabyBlue
        ) {
            IconButton(onClick = onClick) {
                Icon(
                    painter = painterResource(
                        if (isListening) R.drawable.ic_mic_on else R.drawable.ic_mic_off
                    ),
                    contentDescription = null,
                    tint = TextOnDark
                )
            }
        }
    }
}
