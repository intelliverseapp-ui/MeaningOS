package com.example.meaningosapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = BabyPurple,
    onPrimary = TextOnDark,
    secondary = BabyBlue,
    onSecondary = TextOnDark,
    background = BabyBackgroundTop,
    onBackground = TextPrimary,
    surface = BabyBackgroundBottom,
    onSurface = TextPrimary
)

@Composable
fun BabyNodeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = BabyTypography,
        shapes = BabyShapes,
        content = content
    )
}
