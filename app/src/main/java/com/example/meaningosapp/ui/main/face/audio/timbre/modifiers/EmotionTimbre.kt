package com.example.meaningosapp.ui.main.face.audio.timbre.modifiers

import com.example.meaningosapp.ui.main.face.ui.BabyEmotion
import com.example.meaningosapp.ui.main.face.audio.timbre.core.TimbreParams

object EmotionTimbre {
    fun timbreForEmotion(emotion: BabyEmotion, intensity: Double): TimbreParams {
        val scaled = { v: Double -> (v * (0.5 + intensity * 0.5)).coerceIn(0.0, 1.0) }
        return when (emotion) {
            BabyEmotion.HAPPY -> TimbreParams(scaled(0.9), scaled(0.5), scaled(0.3), scaled(0.7))
            BabyEmotion.SAD -> TimbreParams(scaled(0.2), scaled(0.9), scaled(0.1), scaled(0.4))
            BabyEmotion.ANGRY -> TimbreParams(scaled(0.7), scaled(0.3), scaled(1.0), scaled(0.5))
            BabyEmotion.SCARED -> TimbreParams(scaled(0.6), scaled(0.2), scaled(0.8), scaled(0.3))
            BabyEmotion.LOVE -> TimbreParams(scaled(0.5), scaled(1.0), scaled(0.2), scaled(0.9))
            BabyEmotion.SURPRISED -> TimbreParams(scaled(1.0), scaled(0.4), scaled(0.7), scaled(0.8))
            BabyEmotion.THINKING -> TimbreParams(scaled(0.4), scaled(0.7), scaled(0.2), scaled(0.5))
            BabyEmotion.CURIOUS -> TimbreParams(scaled(0.8), scaled(0.5), scaled(0.4), scaled(0.6))
            else -> TimbreParams(scaled(0.5), scaled(0.5), scaled(0.5), scaled(0.5))
        }
    }
}
