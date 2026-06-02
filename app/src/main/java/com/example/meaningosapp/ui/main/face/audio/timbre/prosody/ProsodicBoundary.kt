package com.example.meaningosapp.ui.main.face.audio.timbre.prosody

import com.example.meaningosapp.ui.main.face.audio.timbre.core.TimbreParams

enum class ProsodicBoundary {
    PHRASE_START,
    COMMA,
    PAUSE,
    PHRASE_END,
    NONE
}

object ProsodicBoundaryTimbre {
    private fun clamp(v: Double) = v.coerceIn(0.0, 1.0)

    fun applyProsodicBoundaryTimbre(params: TimbreParams, boundary: ProsodicBoundary): TimbreParams {
        return when (boundary) {
            ProsodicBoundary.PHRASE_START -> TimbreParams(
                brightness = clamp(params.brightness + 0.08),
                warmth = clamp(params.warmth),
                sharpness = clamp(params.sharpness + 0.05),
                resonance = clamp(params.resonance)
            )
            ProsodicBoundary.COMMA -> TimbreParams(
                brightness = clamp(params.brightness - 0.06),
                warmth = clamp(params.warmth + 0.06),
                sharpness = clamp(params.sharpness - 0.03),
                resonance = clamp(params.resonance + 0.02)
            )
            ProsodicBoundary.PAUSE -> TimbreParams(
                brightness = clamp(params.brightness - 0.04),
                warmth = clamp(params.warmth + 0.10),
                sharpness = clamp(params.sharpness - 0.06),
                resonance = clamp(params.resonance + 0.10)
            )
            ProsodicBoundary.PHRASE_END -> TimbreParams(
                brightness = clamp(params.brightness - 0.08),
                warmth = clamp(params.warmth + 0.12),
                sharpness = clamp(params.sharpness - 0.04),
                resonance = clamp(params.resonance + 0.06)
            )
            ProsodicBoundary.NONE -> params
        }
    }
}
