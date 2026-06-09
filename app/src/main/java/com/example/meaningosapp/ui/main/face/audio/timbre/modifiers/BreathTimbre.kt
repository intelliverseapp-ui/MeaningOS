package com.example.meaningosapp.ui.main.face.audio.timbre.modifiers

import com.example.meaningosapp.ui.main.face.audio.timbre.core.TimbreParams
import kotlin.math.sin

object BreathTimbre {

    fun applyBreathSyncedTimbre(params: TimbreParams, breathStrength: Double): TimbreParams {
        val b = breathStrength.coerceIn(0.0, 1.0)

        return TimbreParams(
            brightness = (params.brightness + b * 0.2).coerceIn(0.0, 1.0),
            warmth     = (params.warmth     - b * 0.2).coerceIn(0.0, 1.0),
            sharpness  = (params.sharpness  + b * 0.3).coerceIn(0.0, 1.0),
            resonance  = (params.resonance  + b * 0.25).coerceIn(0.0, 1.0)
        )
    }

    fun applyBreathCycleOscillation(params: TimbreParams, phase: Double): TimbreParams {
        val osc = ((sin(phase) + 1.0) / 2.0).coerceIn(0.0, 1.0)

        return TimbreParams(
            brightness = (params.brightness + osc * 0.05).coerceIn(0.0, 1.0),
            warmth     = (params.warmth     + (1 - osc) * 0.05).coerceIn(0.0, 1.0),
            sharpness  = (params.sharpness  - osc * 0.04).coerceIn(0.0, 1.0),
            resonance  = (params.resonance  + osc * 0.06).coerceIn(0.0, 1.0)
        )
    }
}
