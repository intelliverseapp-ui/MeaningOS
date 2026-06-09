package com.example.meaningosapp.ui.main.face.ui

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

import com.example.meaningosapp.ui.main.face.audio.BabyNodeMode
import com.example.meaningosapp.ui.main.face.audio.BabyEmotion

@Composable
fun BabyNodeFace(
    modifier: Modifier = Modifier,
    mode: BabyNodeMode,
    emotion: BabyEmotion = BabyEmotion.NEUTRAL
) {
    // BREATHING (Idle)
    val breath = rememberInfiniteTransition(label = "breath")
    val breathScale by breath.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath-scale"
    )

    // BLINKING (Natural)
    val blink = rememberInfiniteTransition(label = "blink")
    val blinkScale by blink.animateFloat(
        initialValue = 1f,
        targetValue = 0.1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 4200
                1f at 0
                1f at 3500
                0.1f at 3600
                1f at 3700
            }
        ),
        label = "blink-value"
    )

    // MODE TRANSITIONS (Eyes + Mouth)
    val listeningEyeScale = if (mode == BabyNodeMode.LISTENING) 1.15f else 1f
    val thinkingEyeScale = if (mode == BabyNodeMode.THINKING) 0.7f else 1f
    val eyeScale = listeningEyeScale * thinkingEyeScale

    val speaking = rememberInfiniteTransition(label = "speak")
    val speakingMouth by speaking.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(300, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "speak-mouth"
    )

    Canvas(modifier = modifier.size(180.dp)) {

        // Face background
        drawCircle(
            color = Color(0xFFFFF3C4),
            radius = size.minDimension / 2 * breathScale,
            style = Fill
        )

        // EYES
        val eyeY = size.height * 0.40f
        val leftX = size.width * 0.35f
        val rightX = size.width * 0.65f
        val baseEyeRadius = size.minDimension * 0.06f

        fun drawEye(center: Offset) {
            drawOval(
                color = Color.Black,
                topLeft = Offset(
                    x = center.x - baseEyeRadius,
                    y = center.y - (baseEyeRadius * blinkScale)
                ),
                size = Size(
                    width = baseEyeRadius * 2 * eyeScale,
                    height = baseEyeRadius * 2 * blinkScale
                )
            )
        }

        drawEye(Offset(leftX, eyeY))
        drawEye(Offset(rightX, eyeY))

        // MOUTH
        val mouthWidth = size.width * 0.30f
        val mouthHeight = size.height * 0.06f
        val mouthX = size.width * 0.35f
        val mouthY = size.height * 0.62f

        when (mode) {

            BabyNodeMode.SPEAKING -> {
                drawOval(
                    color = Color.Black,
                    topLeft = Offset(
                        x = size.width * 0.50f - (mouthWidth * speakingMouth / 2),
                        y = mouthY
                    ),
                    size = Size(
                        width = mouthWidth * speakingMouth,
                        height = mouthHeight * speakingMouth
                    )
                )
            }

            else -> when (emotion) {

                BabyEmotion.NEUTRAL -> {
                    drawRect(
                        color = Color.Black,
                        topLeft = Offset(mouthX, mouthY),
                        size = Size(mouthWidth, mouthHeight * 0.2f)
                    )
                }

                BabyEmotion.HAPPY -> {
                    drawArc(
                        color = Color.Black,
                        startAngle = 0f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = Offset(mouthX, mouthY - mouthHeight),
                        size = Size(mouthWidth, mouthHeight * 2),
                        style = Stroke(width = 6f)
                    )
                }

                BabyEmotion.SAD -> {
                    drawArc(
                        color = Color.Black,
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = Offset(mouthX, mouthY),
                        size = Size(mouthWidth, mouthHeight * 2),
                        style = Stroke(width = 6f)
                    )
                }

                BabyEmotion.SURPRISED -> {
                    drawCircle(
                        color = Color.Black,
                        radius = mouthHeight * 0.9f,
                        center = Offset(
                            x = size.width * 0.50f,
                            y = size.height * 0.62f
                        )
                    )
                }

                BabyEmotion.ANGRY -> {
                    drawRect(
                        color = Color.Black,
                        topLeft = Offset(mouthX, mouthY),
                        size = Size(mouthWidth, mouthHeight * 0.15f)
                    )
                }

                else -> {
                    drawRect(
                        color = Color.Black,
                        topLeft = Offset(mouthX, mouthY),
                        size = Size(mouthWidth, mouthHeight * 0.2f)
                    )
                }
            }
        }
    }
}
