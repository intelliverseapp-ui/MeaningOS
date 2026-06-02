package com.example.meaningosapp.ui.main.face.audio.timbre.pipeline

import com.example.meaningosapp.ui.main.face.ui.BabyEmotion
import com.example.meaningosapp.ui.main.face.audio.timbre.core.TimbreParams
import com.example.meaningosapp.ui.main.face.audio.timbre.modifiers.BreathTimbre
import com.example.meaningosapp.ui.main.face.audio.timbre.modifiers.EmotionTimbre
import com.example.meaningosapp.ui.main.face.audio.timbre.modifiers.PersonalityTimbre
import com.example.meaningosapp.ui.main.face.audio.timbre.articulation.ArticulatoryTimbre
import com.example.meaningosapp.ui.main.face.audio.timbre.prosody.BoundaryDetector
import com.example.meaningosapp.ui.main.face.audio.timbre.prosody.ProsodicBoundaryTimbre
import com.example.meaningosapp.ui.main.face.audio.timbre.base.applyBrightness
import com.example.meaningosapp.ui.main.face.audio.timbre.base.applyWarmth
import com.example.meaningosapp.ui.main.face.audio.timbre.base.applySharpness
import com.example.meaningosapp.ui.main.face.audio.timbre.base.applyResonance

class TimbrePipeline {

    enum class SentenceCadence { QUESTION, EXCLAMATION, REFLECTION, STATEMENT }
    enum class PersonalityMode { WARM, DIRECT, SOFT, PLAYFUL, CALM }

    private fun applyAll(text: String, p: TimbreParams): String {
        var shaped = text
        shaped = applyBrightness(shaped, p.brightness)
        shaped = applyWarmth(shaped, p.warmth)
        shaped = applySharpness(shaped, p.sharpness)
        shaped = applyResonance(shaped, p.resonance)
        return shaped
    }

    fun applyTimbrePipeline(
        text: String,
        emotion: BabyEmotion,
        intensity: Double,
        breathStrength: Double,
        cadence: SentenceCadence,
        emphasisStrength: Double,
        semanticWeight: Double,
        contextTone: Double,
        personality: PersonalityMode,
        breathPhase: Double
    ): String {
        if (text.isBlank()) return text

        var baseParams = EmotionTimbre.timbreForEmotion(emotion, intensity)
        baseParams = BreathTimbre.applyBreathSyncedTimbre(baseParams, breathStrength)
        baseParams = PersonalityTimbre.applyPersonalityTimbre(baseParams, personality)
        baseParams = BreathTimbre.applyBreathCycleOscillation(baseParams, breathPhase)

        val tokens = BoundaryDetector.tokenizeWithPunctuation(text)
        val shapedBuilder = StringBuilder()
        var nextIsPhraseStart = true

        for (token in tokens) {
            val boundary = BoundaryDetector.detectBoundaryForToken(token, nextIsPhraseStart)
            var params = ProsodicBoundaryTimbre.applyProsodicBoundaryTimbre(baseParams, boundary)

            if (token.trim().isEmpty()) continue

            val tokenBuilder = StringBuilder()
            for (ch in token) {
                val p = ArticulatoryTimbre.applyArticulatoryTimbre(params, ch)
                tokenBuilder.append(applyAll(ch.toString(), p))
            }
            shapedBuilder.append(tokenBuilder.toString())
            nextIsPhraseStart = (boundary.name == "PHRASE_END" || boundary.name == "PAUSE")
        }

        val curve = listOf(baseParams, baseParams, baseParams)
        val start = applyAll(shapedBuilder.toString(), curve[0])
        val mid = applyAll(shapedBuilder.toString(), curve[1])
        val end = applyAll(shapedBuilder.toString(), curve[2])

        return """
            $start
            $mid
            $end
        """.trimIndent()
    }
}
