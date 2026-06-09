package com.example.meaningosapp.ui.main.face.audio.timbre.modifiers

import com.example.meaningosapp.ui.main.face.audio.BabyEmotion

/**
 * EmotionTimbre
 *
 * Maps BabyEmotion → timbre shaping parameters.
 * These values influence:
 *  - EQ curves
 *  - Brightness
 *  - Breathiness
 *  - Resonance
 *
 * Baby Node 1.0 keeps this subtle.
 */
object EmotionTimbre {

    data class TimbreParams(
        val brightness: Float,
        val breathiness: Float,
        val resonance: Float
    )

    /**
     * Main mapping function.
     * Must be exhaustive for BabyEmotion enum.
     */
    fun forEmotion(emotion: BabyEmotion): TimbreParams {
        return when (emotion) {

            BabyEmotion.NEUTRAL ->
                TimbreParams(
                    brightness = 0.0f,
                    breathiness = 0.0f,
                    resonance = 0.0f
                )

            BabyEmotion.HAPPY ->
                TimbreParams(
                    brightness = 0.25f,
                    breathiness = 0.10f,
                    resonance = 0.15f
                )

            BabyEmotion.LOVE ->
                TimbreParams(
                    brightness = 0.10f,
                    breathiness = 0.20f,
                    resonance = 0.25f
                )

            BabyEmotion.CONNECTED ->
                TimbreParams(
                    brightness = 0.05f,
                    breathiness = 0.05f,
                    resonance = 0.10f
                )

            BabyEmotion.SAD ->
                TimbreParams(
                    brightness = -0.20f,
                    breathiness = 0.15f,
                    resonance = -0.10f
                )

            BabyEmotion.ANGRY ->
                TimbreParams(
                    brightness = 0.30f,
                    breathiness = -0.10f,
                    resonance = 0.20f
                )

            BabyEmotion.SCARED ->
                TimbreParams(
                    brightness = 0.15f,
                    breathiness = 0.25f,
                    resonance = -0.05f
                )

            BabyEmotion.THINKING ->
                TimbreParams(
                    brightness = -0.05f,
                    breathiness = 0.0f,
                    resonance = 0.05f
                )

            BabyEmotion.CURIOUS ->
                TimbreParams(
                    brightness = 0.10f,
                    breathiness = 0.05f,
                    resonance = 0.10f
                )

            BabyEmotion.SURPRISED ->
                TimbreParams(
                    brightness = 0.20f,
                    breathiness = 0.10f,
                    resonance = 0.15f
                )

            // ⭐ Newly added states for TTS prosody
            BabyEmotion.CALM ->
                TimbreParams(
                    brightness = -0.10f,
                    breathiness = 0.05f,
                    resonance = 0.10f
                )

            BabyEmotion.EXCITED ->
                TimbreParams(
                    brightness = 0.35f,
                    breathiness = 0.15f,
                    resonance = 0.25f
                )
        }
    }
}
