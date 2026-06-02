package com.example.meaningosapp.ui.main.face.audio.timbre.modifiers

import com.example.meaningosapp.ui.main.face.audio.timbre.core.TimbreParams
import com.example.meaningosapp.ui.main.face.audio.timbre.pipeline.PersonalityMode

object PersonalityTimbre {
    private fun clamp(v: Double) = v.coerceIn(0.0, 1.0)

    fun applyPersonalityTimbre(params: TimbreParams, personality: PersonalityMode): TimbreParams {
        return when (personality) {
            PersonalityMode.WARM -> params.copy(
                warmth = clamp(params.warmth * 1.25),
                resonance = clamp(params.resonance * 1.20),
                sharpness = clamp(params.sharpness * 0.85)
            )
            PersonalityMode.DIRECT -> params.copy(
                brightness = clamp(params.brightness * 1.20),
                sharpness = clamp(params.sharpness * 1.25),
                warmth = clamp(params.warmth * 0.85)
            )
            PersonalityMode.SOFT -> params.copy(
                warmth = clamp(params.warmth * 1.30),
                brightness = clamp(params.brightness * 0.85),
                sharpness = clamp(params.sharpness * 0.80)
            )
            PersonalityMode.PLAYFUL -> params.copy(
                brightness = clamp(params.brightness * 1.25),
                resonance = clamp(params.resonance * 1.20),
                sharpness = clamp(params.sharpness * 1.10)
            )
            PersonalityMode.CALM -> params.copy(
                warmth = clamp(params.warmth * 1.20),
                brightness = clamp(params.brightness * 0.90),
                sharpness = clamp(params.sharpness * 0.85),
                resonance = clamp(params.resonance * 1.15)
            )
        }
    }
}
