package com.example.meaningosapp.ui.main.face.audio.timbre.articulation

import com.example.meaningosapp.ui.main.face.audio.timbre.core.TimbreParams

object ArticulatoryTimbre {
    private fun clamp(v: Double) = v.coerceIn(0.0, 1.0)

    fun applyArticulatoryTimbre(params: TimbreParams, segment: Char): TimbreParams {
        val c = segment.lowercaseChar()
        return when (c) {
            'a', 'e', 'o' -> params.copy(
                warmth = clamp(params.warmth * 1.10),
                resonance = clamp(params.resonance * 1.12)
            )
            'i', 'u' -> params.copy(
                brightness = clamp(params.brightness * 1.12),
                resonance = clamp(params.resonance * 0.92)
            )
            'p', 'b', 't', 'd', 'k', 'g' -> params.copy(
                sharpness = clamp(params.sharpness * 1.20)
            )
            's', 'f', 'v', 'z' -> params.copy(
                brightness = clamp(params.brightness * 1.15),
                sharpness = clamp(params.sharpness * 1.10)
            )
            'm', 'n' -> params.copy(
                warmth = clamp(params.warmth * 1.15),
                resonance = clamp(params.resonance * 1.10)
            )
            'l', 'r' -> params.copy(
                resonance = clamp(params.resonance * 1.12),
                warmth = clamp(params.warmth * 1.05)
            )
            'w', 'y' -> params.copy(
                brightness = clamp(params.brightness * 1.10),
                resonance = clamp(params.resonance * 1.08)
            )
            else -> params
        }
    }
}
