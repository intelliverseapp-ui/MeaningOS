package com.example.meaningosapp.ui.main.face.audio.timbre

import com.example.meaningosapp.ui.main.face.ui.BabyEmotion
import kotlin.math.sin
import kotlin.math.PI
import kotlin.random.Random

class BabyTimbre {

    // ------------------------------------------------------------
    //  BASE TIMBRE SHAPERS
    // ------------------------------------------------------------

    fun applyBrightness(text: String, brightness: Double): String {
        if (text.isBlank()) return text

        val clamped = brightness.coerceIn(0.0, 1.0)
        val semitones = (-2 + clamped * 5).coerceIn(-2.0, 3.0)
        val pitch = if (semitones >= 0) "+${semitones}st" else "${semitones}st"

        return """
            <prosody pitch="$pitch">
                $text
            </prosody>
        """.trimIndent()
    }

    fun applyWarmth(text: String, warmth: Double): String {
        if (text.isBlank()) return text

        val clamped = warmth.coerceIn(0.0, 1.0)
        val volumeBoost = (clamped * 12).toInt()
        val pitchShift = (-2 * clamped).coerceIn(-2.0, 0.0)
        val pitch = "${pitchShift}st"

        return """
            <prosody volume="+${volumeBoost}%" pitch="$pitch">
                $text
            </prosody>
        """.trimIndent()
    }

    fun applySharpness(text: String, sharpness: Double): String {
        if (text.isBlank()) return text

        val clamped = sharpness.coerceIn(0.0, 1.0)
        val pitchShift = (clamped * 3).coerceIn(0.0, 3.0)
        val rateBoost = (100 + clamped * 10).toInt()
        val volumeBoost = (clamped * 8).toInt()

        val pitch = "+${pitchShift}st"

        return """
            <prosody pitch="$pitch" rate="${rateBoost}%" volume="+${volumeBoost}%">
                $text
            </prosody>
        """.trimIndent()
    }

    fun applyResonance(text: String, resonance: Double): String {
        if (text.isBlank()) return text

        val clamped = resonance.coerceIn(0.0, 1.0)
        val volumeBoost = (clamped * 10).toInt()
        val rateBoost = (100 + clamped * 8).toInt()

        return """
            <prosody volume="+${volumeBoost}%" rate="${rateBoost}%">
                $text
            </prosody>
        """.trimIndent()
    }

    // ------------------------------------------------------------
    //  EMOTION → BASE TIMBRE
    // ------------------------------------------------------------

    fun timbreForEmotion(
        emotion: BabyEmotion,
        intensity: Double
    ): TimbreParams {

        val scaled = { v: Double -> (v * (0.5 + intensity * 0.5)).coerceIn(0.0, 1.0) }

        return when (emotion) {

            BabyEmotion.HAPPY ->
                TimbreParams(scaled(0.9), scaled(0.5), scaled(0.3), scaled(0.7))

            BabyEmotion.SAD ->
                TimbreParams(scaled(0.2), scaled(0.9), scaled(0.1), scaled(0.4))

            BabyEmotion.ANGRY ->
                TimbreParams(scaled(0.7), scaled(0.3), scaled(1.0), scaled(0.5))

            BabyEmotion.SCARED ->
                TimbreParams(scaled(0.6), scaled(0.2), scaled(0.8), scaled(0.3))

            BabyEmotion.LOVE ->
                TimbreParams(scaled(0.5), scaled(1.0), scaled(0.2), scaled(0.9))

            BabyEmotion.SURPRISED ->
                TimbreParams(scaled(1.0), scaled(0.4), scaled(0.7), scaled(0.8))

            BabyEmotion.THINKING ->
                TimbreParams(scaled(0.4), scaled(0.7), scaled(0.2), scaled(0.5))

            BabyEmotion.CURIOUS ->
                TimbreParams(scaled(0.8), scaled(0.5), scaled(0.4), scaled(0.6))

            else ->
                TimbreParams(scaled(0.5), scaled(0.5), scaled(0.5), scaled(0.5))
        }
    }

    // ------------------------------------------------------------
    //  BREATH-SYNCED TIMBRE
    // ------------------------------------------------------------

    fun applyBreathSyncedTimbre(
        params: TimbreParams,
        breathStrength: Double
    ): TimbreParams {

        val b = breathStrength.coerceIn(0.0, 1.0)

        return TimbreParams(
            brightness = (params.brightness + b * 0.2).coerceIn(0.0, 1.0),
            warmth = (params.warmth - b * 0.2).coerceIn(0.0, 1.0),
            sharpness = (params.sharpness + b * 0.3).coerceIn(0.0, 1.0),
            resonance = (params.resonance + b * 0.25).coerceIn(0.0, 1.0)
        )
    }

    // ------------------------------------------------------------
    //  CADENCE-SYNCED TIMBRE
    // ------------------------------------------------------------

    fun applyCadenceSyncedTimbre(
        params: TimbreParams,
        cadence: SentenceCadence
    ): TimbreParams {

        fun clamp(v: Double) = v.coerceIn(0.0, 1.0)

        return when (cadence) {

            SentenceCadence.QUESTION ->
                params.copy(
                    brightness = clamp(params.brightness * 1.15),
                    resonance = clamp(params.resonance * 1.20)
                )

            SentenceCadence.EXCLAMATION ->
                params.copy(
                    sharpness = clamp(params.sharpness * 1.25),
                    brightness = clamp(params.brightness * 1.10)
                )

            SentenceCadence.REFLECTION ->
                params.copy(
                    warmth = clamp(params.warmth * 1.20),
                    brightness = clamp(params.brightness * 0.85)
                )

            SentenceCadence.STATEMENT ->
                params
        }
    }

    // ------------------------------------------------------------
    //  KEYWORD EMPHASIS TIMBRE
    // ------------------------------------------------------------

    fun applyKeywordSyncedTimbre(
        params: TimbreParams,
        emphasisStrength: Double
    ): TimbreParams {

        val e = emphasisStrength.coerceIn(0.0, 1.0)

        return TimbreParams(
            brightness = (params.brightness + e * 0.25).coerceIn(0.0, 1.0),
            sharpness = (params.sharpness + e * 0.35).coerceIn(0.0, 1.0),
            resonance = (params.resonance + e * 0.20).coerceIn(0.0, 1.0),
            warmth = (params.warmth - e * 0.15).coerceIn(0.0, 1.0)
        )
    }

    // ------------------------------------------------------------
    //  SEMANTIC WEIGHT TIMBRE
    // ------------------------------------------------------------

    fun applySemanticWeightTimbre(
        params: TimbreParams,
        semanticWeight: Double
    ): TimbreParams {

        val w = semanticWeight.coerceIn(0.0, 1.0)

        return TimbreParams(
            brightness = (params.brightness + w * 0.10).coerceIn(0.0, 1.0),
            warmth = (params.warmth + w * 0.20).coerceIn(0.0, 1.0),
            sharpness = (params.sharpness - w * 0.10).coerceIn(0.0, 1.0),
            resonance = (params.resonance + w * 0.25).coerceIn(0.0, 1.0)
        )
    }

    // ------------------------------------------------------------
    //  CONTEXTUAL MEMORY TIMBRE
    // ------------------------------------------------------------

    fun applyContextualMemoryTimbre(
        params: TimbreParams,
        contextTone: Double
    ): TimbreParams {

        val c = contextTone.coerceIn(0.0, 1.0)

        return TimbreParams(
            brightness = (params.brightness + c * 0.05).coerceIn(0.0, 1.0),
            warmth = (params.warmth + c * 0.15).coerceIn(0.0, 1.0),
            sharpness = (params.sharpness - c * 0.10).coerceIn(0.0, 1.0),
            resonance = (params.resonance + c * 0.20).coerceIn(0.0, 1.0)
        )
    }

    // ------------------------------------------------------------
    //  PERSONALITY TIMBRE
    // ------------------------------------------------------------

    fun applyPersonalityTimbre(
        params: TimbreParams,
        personality: PersonalityMode
    ): TimbreParams {

        fun clamp(v: Double) = v.coerceIn(0.0, 1.0)

        return when (personality) {

            PersonalityMode.WARM ->
                params.copy(
                    warmth = clamp(params.warmth * 1.25),
                    resonance = clamp(params.resonance * 1.20),
                    sharpness = clamp(params.sharpness * 0.85)
                )

            PersonalityMode.DIRECT ->
                params.copy(
                    brightness = clamp(params.brightness * 1.20),
                    sharpness = clamp(params.sharpness * 1.25),
                    warmth = clamp(params.warmth * 0.85)
                )

            PersonalityMode.SOFT ->
                params.copy(
                    warmth = clamp(params.warmth * 1.30),
                    brightness = clamp(params.brightness * 0.85),
                    sharpness = clamp(params.sharpness * 0.80)
                )

            PersonalityMode.PLAYFUL ->
                params.copy(
                    brightness = clamp(params.brightness * 1.25),
                    resonance = clamp(params.resonance * 1.20),
                    sharpness = clamp(params.sharpness * 1.10)
                )

            PersonalityMode.CALM ->
                params.copy(
                    warmth = clamp(params.warmth * 1.20),
                    brightness = clamp(params.brightness * 0.90),
                    sharpness = clamp(params.sharpness * 0.85),
                    resonance = clamp(params.resonance * 1.15)
                )
        }
    }

    // ------------------------------------------------------------
    //  BREATH-CYCLE OSCILLATION
    // ------------------------------------------------------------

    fun applyBreathCycleOscillation(
        params: TimbreParams,
        phase: Double
    ): TimbreParams {

        val osc = ((sin(phase) + 1.0) / 2.0).coerceIn(0.0, 1.0)

        return TimbreParams(
            brightness = (params.brightness + osc * 0.05).coerceIn(0.0, 1.0),
            warmth = (params.warmth + (1 - osc) * 0.05).coerceIn(0.0, 1.0),
            sharpness = (params.sharpness - osc * 0.04).coerceIn(0.0, 1.0),
            resonance = (params.resonance + osc * 0.06).coerceIn(0.0, 1.0)
        )
    }

    // ------------------------------------------------------------
    //  MICRO-GESTURES
    // ------------------------------------------------------------

    fun applyMicroGestures(params: TimbreParams): TimbreParams {

        fun flick(base: Double, amount: Double): Double {
            val delta = Random.nextDouble(-amount, amount)
            return (base + delta).coerceIn(0.0, 1.0)
        }

        return TimbreParams(
            brightness = flick(params.brightness, 0.05),
            warmth = flick(params.warmth, 0.05),
            sharpness = flick(params.sharpness, 0.05),
            resonance = flick(params.resonance, 0.05)
        )
    }

    // ------------------------------------------------------------
    //  ARTICULATORY GESTURE TIMBRE
    // ------------------------------------------------------------

    fun applyArticulatoryTimbre(
        params: TimbreParams,
        segment: Char
    ): TimbreParams {

        val c = segment.lowercaseChar()

        fun clamp(v: Double) = v.coerceIn(0.0, 1.0)

        return when (c) {

            // Open vowels
            'a', 'e', 'o' -> params.copy(
                warmth = clamp(params.warmth * 1.10),
                resonance = clamp(params.resonance * 1.12)
            )

            // Closed vowels
            'i', 'u' -> params.copy(
                brightness = clamp(params.brightness * 1.12),
                resonance = clamp(params.resonance * 0.92)
            )

            // Plosives
            'p', 'b', 't', 'd', 'k', 'g' -> params.copy(
                sharpness = clamp(params.sharpness * 1.20)
            )

            // Fricatives
            's', 'f', 'v', 'z' -> params.copy(
                brightness = clamp(params.brightness * 1.15),
                sharpness = clamp(params.sharpness * 1.10)
            )

            // Nasals
            'm', 'n' -> params.copy(
                warmth = clamp(params.warmth * 1.15),
                resonance = clamp(params.resonance * 1.10)
            )

            // Liquids
            'l', 'r' -> params.copy(
                resonance = clamp(params.resonance * 1.12),
                warmth = clamp(params.warmth * 1.05)
            )

            // Glides
            'w', 'y' -> params.copy(
                brightness = clamp(params.brightness * 1.10),
                resonance = clamp(params.resonance * 1.08)
            )

            else -> params
        }
    }

    // ------------------------------------------------------------
    //  PROSODIC BOUNDARY TIMBRE (35S)
    // ------------------------------------------------------------

    enum class ProsodicBoundary {
        PHRASE_START,
        COMMA,
        PAUSE,
        PHRASE_END,
        NONE
    }

    fun applyProsodicBoundaryTimbre(
        params: TimbreParams,
        boundary: ProsodicBoundary
    ): TimbreParams {

        fun clamp(v: Double) = v.coerceIn(0.0, 1.0)

        return when (boundary) {

            ProsodicBoundary.PHRASE_START ->
                TimbreParams(
                    brightness = clamp(params.brightness + 0.08),
                    warmth = clamp(params.warmth),
                    sharpness = clamp(params.sharpness + 0.05),
                    resonance = clamp(params.resonance)
                )

            ProsodicBoundary.COMMA ->
                TimbreParams(
                    brightness = clamp(params.brightness - 0.06),
                    warmth = clamp(params.warmth + 0.06),
                    sharpness = clamp(params.sharpness - 0.03),
                    resonance = clamp(params.resonance + 0.02)
                )

            ProsodicBoundary.PAUSE ->
                TimbreParams(
                    brightness = clamp(params.brightness - 0.04),
                    warmth = clamp(params.warmth + 0.10),
                    sharpness = clamp(params.sharpness - 0.06),
                    resonance = clamp(params.resonance + 0.10)
                )

            ProsodicBoundary.PHRASE_END ->
                TimbreParams(
                    brightness = clamp(params.brightness - 0.08),
                    warmth = clamp(params.warmth + 0.12),
                    sharpness = clamp(params.sharpness - 0.04),
                    resonance = clamp(params.resonance + 0.06)
                )

            ProsodicBoundary.NONE ->
                params
        }
    }

    // ------------------------------------------------------------
    //  EMOTION CURVE
    // ------------------------------------------------------------

    fun applyEmotionCurve(params: TimbreParams, emotion: BabyEmotion): List<TimbreParams> {

        fun clamp(v: Double) = v.coerceIn(0.0, 1.0)

        return when (emotion) {

            BabyEmotion.HAPPY -> listOf(
                params.copy(brightness = clamp(params.brightness * 0.9)),
                params.copy(brightness = clamp(params.brightness * 1.1)),
                params.copy(brightness = clamp(params.brightness * 1.2))
            )

            BabyEmotion.SAD -> listOf(
                params.copy(warmth = clamp(params.warmth * 1.1)),
                params.copy(warmth = clamp(params.warmth * 1.2)),
                params.copy(warmth = clamp(params.warmth * 1.3))
            )

            BabyEmotion.ANGRY -> listOf(
                params.copy(sharpness = clamp(params.sharpness * 0.9)),
                params.copy(sharpness = clamp(params.sharpness * 1.3)),
                params.copy(sharpness = clamp(params.sharpness * 1.1))
            )

            else -> listOf(params, params, params)
        }
    }

    // ------------------------------------------------------------
    //  FULL PIPELINE (WITH PROSODIC BOUNDARIES)
    // ------------------------------------------------------------

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

        // Base params from emotion + global modifiers
        var baseParams = timbreForEmotion(emotion, intensity)
        baseParams = applyBreathSyncedTimbre(baseParams, breathStrength)
        baseParams = applyCadenceSyncedTimbre(baseParams, cadence)
        baseParams = applyKeywordSyncedTimbre(baseParams, emphasisStrength)
        baseParams = applySemanticWeightTimbre(baseParams, semanticWeight)
        baseParams = applyContextualMemoryTimbre(baseParams, contextTone)
        baseParams = applyPersonalityTimbre(baseParams, personality)
        baseParams = applyBreathCycleOscillation(baseParams, breathPhase)
        baseParams = applyMicroGestures(baseParams)

        // Tokenize into phrase tokens while preserving punctuation
        val tokens = tokenizeWithPunctuation(text)

        val shapedBuilder = StringBuilder()

        // Track whether next token is phrase start
        var nextIsPhraseStart = true

        for (token in tokens) {
            val boundary = detectBoundaryForToken(token, nextIsPhraseStart)
            var params = applyProsodicBoundaryTimbre(baseParams, boundary)

            // If token is punctuation-only, render with boundary shaping applied to a short pause marker
            if (token.trim().isEmpty()) {
                // skip empty tokens
                continue
            }

            // Apply articulatory shaping per character inside token
            val tokenBuilder = StringBuilder()
            for (ch in token) {
                val p = applyArticulatoryTimbre(params, ch)
                tokenBuilder.append(applyAll(ch.toString(), p))
            }

            shapedBuilder.append(tokenBuilder.toString())

            // Update phrase start flag: after a phrase end boundary, next token is phrase start
            nextIsPhraseStart = (boundary == ProsodicBoundary.PHRASE_END || boundary == ProsodicBoundary.PAUSE)
        }

        // Apply emotion curve to final shaped text for micro-variation across phrase
        val curve = applyEmotionCurve(baseParams, emotion)
        val start = applyAll(shapedBuilder.toString(), curve[0])
        val mid = applyAll(shapedBuilder.toString(), curve[1])
        val end = applyAll(shapedBuilder.toString(), curve[2])

        return """
            $start
            $mid
            $end
        """.trimIndent()
    }

    // ------------------------------------------------------------
    //  HELPERS: TOKENIZATION AND BOUNDARY DETECTION
    // ------------------------------------------------------------

    private fun tokenizeWithPunctuation(text: String): List<String> {
        // Split into tokens but keep punctuation as separate tokens
        // This is intentionally simple and deterministic.
        val tokens = mutableListOf<String>()
        val sb = StringBuilder()
        for (ch in text) {
            when (ch) {
                ',', ';', ':', '—', '-', '.', '!', '?' -> {
                    if (sb.isNotEmpty()) {
                        tokens.add(sb.toString())
                        sb.clear()
                    }
                    tokens.add(ch.toString())
                }
                else -> {
                    sb.append(ch)
                }
            }
        }
        if (sb.isNotEmpty()) tokens.add(sb.toString())
        return tokens
    }

    private fun detectBoundaryForToken(token: String, nextIsPhraseStart: Boolean): ProsodicBoundary {
        val t = token.trim()
        if (t.isEmpty()) return ProsodicBoundary.NONE

        return when (t) {
            "," -> ProsodicBoundary.COMMA
            ";" -> ProsodicBoundary.PAUSE
            ":" -> ProsodicBoundary.PAUSE
            "—", "-" -> ProsodicBoundary.PAUSE
            "." -> ProsodicBoundary.PHRASE_END
            "!" -> ProsodicBoundary.PHRASE_END
            "?" -> ProsodicBoundary.PHRASE_END
            else -> if (nextIsPhraseStart) ProsodicBoundary.PHRASE_START else ProsodicBoundary.NONE
        }
    }

    // ------------------------------------------------------------
    //  APPLY ALL TIMBRE SHAPERS
    // ------------------------------------------------------------

    private fun applyAll(text: String, p: TimbreParams): String {
        var shaped = text
        shaped = applyBrightness(shaped, p.brightness)
        shaped = applyWarmth(shaped, p.warmth)
        shaped = applySharpness(shaped, p.sharpness)
        shaped = applyResonance(shaped, p.resonance)
        return shaped
    }
}

enum class SentenceCadence {
    QUESTION, EXCLAMATION, REFLECTION, STATEMENT
}

enum class PersonalityMode {
    WARM, DIRECT, SOFT, PLAYFUL, CALM
}

data class TimbreParams(
    val brightness: Double,
    val warmth: Double,
    val sharpness: Double,
    val resonance: Double
)
