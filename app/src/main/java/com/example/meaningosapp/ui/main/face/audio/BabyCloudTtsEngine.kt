package com.example.meaningosapp.ui.main.face.audio

import com.example.meaningosapp.ui.main.face.ui.BabyEmotion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BabyCloudTtsEngine
 *
 * Baby Step 32:
 *  - Intent Modeling
 *  - Detects communicative intent (reassure, warn, persuade, reflect, reveal, urge, neutral)
 *  - Shapes prosody based on intent on top of emotion, cadence, timbre, breath
 */
class BabyCloudTtsEngine(
    private val apiKey: String
) {

    private val ttsClient = GoogleCloudTtsClient(apiKey)
    private val audioPlayer = BabyAudioPlayer()

    // ---------------------------------------------------------
    // VOICE PERSONAS
    // ---------------------------------------------------------

    private data class VoicePersona(
        val voiceName: String,
        val languageCode: String,
        val gender: String
    )

    private val personaMap = mapOf(
        BabyEmotion.HAPPY to VoicePersona("en-US-Wavenet-F", "en-US", "FEMALE"),
        BabyEmotion.SAD to VoicePersona("en-US-Wavenet-C", "en-US", "MALE"),
        BabyEmotion.ANGRY to VoicePersona("en-US-Wavenet-D", "en-US", "MALE"),
        BabyEmotion.SURPRISED to VoicePersona("en-US-Wavenet-E", "en-US", "FEMALE"),
        BabyEmotion.SCARED to VoicePersona("en-US-Wavenet-H", "en-US", "FEMALE"),
        BabyEmotion.LOVE to VoicePersona("en-US-Wavenet-J", "en-US", "MALE"),
        BabyEmotion.CURIOUS to VoicePersona("en-US-Wavenet-B", "en-US", "MALE"),
        BabyEmotion.THINKING to VoicePersona("en-US-Wavenet-A", "en-US", "MALE"),
        BabyEmotion.NEUTRAL to VoicePersona("en-US-Wavenet-A", "en-US", "MALE")
    )

    private fun personaForEmotion(emotion: BabyEmotion): VoicePersona {
        return personaMap[emotion] ?: personaMap[BabyEmotion.NEUTRAL]!!
    }

    // ---------------------------------------------------------
    // BASE + INTENSITY‑SCALED PARAMETERS
    // ---------------------------------------------------------

    private fun baseParamsForEmotion(emotion: BabyEmotion): VoiceParams {
        return when (emotion) {
            BabyEmotion.HAPPY -> VoiceParams(4.0, 1.15)
            BabyEmotion.SAD -> VoiceParams(-4.0, 0.85)
            BabyEmotion.ANGRY -> VoiceParams(2.0, 1.25)
            BabyEmotion.SURPRISED -> VoiceParams(6.0, 1.30)
            BabyEmotion.SCARED -> VoiceParams(-2.0, 0.95)
            BabyEmotion.LOVE -> VoiceParams(3.0, 1.10)
            BabyEmotion.THINKING -> VoiceParams(0.0, 0.95)
            BabyEmotion.CURIOUS -> VoiceParams(2.0, 1.05)
            else -> VoiceParams(0.0, 1.0)
        }
    }

    private fun scaledParams(base: VoiceParams, intensity: Double): VoiceParams {
        val scale = 0.5 + (intensity * 0.5)
        return VoiceParams(
            pitch = base.pitch * scale,
            speed = base.speed * scale
        )
    }

    // ---------------------------------------------------------
    // EMOTIONAL OVERDRIVE MODE
    // ---------------------------------------------------------

    private fun isOverdrive(intensity: Double): Boolean = intensity > 0.85

    private fun applyOverdrive(sentence: String, emotion: BabyEmotion): String {
        val sweep = when (emotion) {
            BabyEmotion.HAPPY -> "+8st"
            BabyEmotion.ANGRY -> "+10st"
            BabyEmotion.SURPRISED -> "+12st"
            BabyEmotion.LOVE -> "+6st"
            BabyEmotion.SAD -> "-8st"
            BabyEmotion.SCARED -> "-10st"
            else -> "+6st"
        }

        return """
            <prosody pitch="$sweep">
                <emphasis level="strong">$sentence</emphasis>
            </prosody>
        """.trimIndent()
    }

    // ---------------------------------------------------------
    // WHISPER MODE
    // ---------------------------------------------------------

    private fun shouldWhisper(emotion: BabyEmotion, intensity: Double): Boolean {
        return when (emotion) {
            BabyEmotion.SCARED -> true
            BabyEmotion.LOVE -> intensity > 0.6
            BabyEmotion.THINKING -> intensity > 0.4
            else -> false
        }
    }

    private fun whisperWrap(text: String, intensity: Double): String {
        val softness = (60 + intensity * 25).toInt()
        val pitch = "-${2 + intensity * 3}st"
        val rate = "${85 - intensity * 10}%"

        return """
            <prosody volume="${softness}%" pitch="$pitch" rate="$rate">
                $text
            </prosody>
        """.trimIndent()
    }

    // ---------------------------------------------------------
    // KEYWORD EMPHASIS ENGINE
    // ---------------------------------------------------------

    private val strongEmotionWords = setOf(
        "love", "hate", "amazing", "terrible", "beautiful", "awful",
        "incredible", "scary", "dangerous", "excited", "sad", "happy",
        "angry", "furious", "shocked", "surprised"
    )

    private val intensifiers = setOf(
        "very", "really", "extremely", "absolutely", "totally", "completely"
    )

    private val userFocusWords = setOf(
        "you", "your", "yours", "now", "right now", "listen", "look"
    )

    private fun emphasizeKeywords(sentence: String, emotion: BabyEmotion, intensity: Double): String {
        val words = sentence.split(" ")

        val emphasisLevel =
            if (intensity > 0.7) "strong"
            else if (intensity > 0.3) "moderate"
            else "reduced"

        return words.joinToString(" ") { word ->
            val clean = word.lowercase().trim('!', '.', ',', '?')

            val shouldEmphasize =
                clean in strongEmotionWords ||
                        clean in intensifiers ||
                        clean in userFocusWords

            if (shouldEmphasize)
                "<emphasis level=\"$emphasisLevel\">$word</emphasis>"
            else
                word
        }
    }

    // ---------------------------------------------------------
    // MICRO‑PAUSE INJECTION
    // ---------------------------------------------------------

    private val microPauseRegex = Regex(
        "(,| and | but | or | so | because | although | though | however )",
        RegexOption.IGNORE_CASE
    )

    private fun injectMicroPauses(sentence: String, intensity: Double): String {
        val pause = (40 + intensity * 80).toInt()
        return sentence.replace(microPauseRegex) { match ->
            val token = match.value
            "$token<break time=\"${pause}ms\"/>"
        }
    }

    // ---------------------------------------------------------
    // ADAPTIVE CONVERSATIONAL CADENCE
    // ---------------------------------------------------------

    private enum class SentenceType { QUESTION, EXCLAMATION, REFLECTION, STATEMENT }

    private fun detectSentenceType(sentence: String): SentenceType {
        return when {
            sentence.endsWith("?") -> SentenceType.QUESTION
            sentence.endsWith("!") -> SentenceType.EXCLAMATION
            sentence.contains("I think") ||
                    sentence.contains("I wonder") ||
                    sentence.contains("maybe") -> SentenceType.REFLECTION
            else -> SentenceType.STATEMENT
        }
    }

    private fun cadenceWrap(sentence: String, type: SentenceType, emotion: BabyEmotion, intensity: Double): String {
        return when (type) {

            SentenceType.QUESTION -> """
                <prosody pitch="+3st" rate="${105 + intensity * 10}%">
                    $sentence
                </prosody>
            """.trimIndent()

            SentenceType.EXCLAMATION -> """
                <prosody pitch="+6st" rate="${120 + intensity * 20}%">
                    <emphasis level="strong">$sentence</emphasis>
                </prosody>
            """.trimIndent()

            SentenceType.REFLECTION -> """
                <prosody pitch="-2st" rate="${85 - intensity * 5}%">
                    <break time="150ms"/> $sentence
                </prosody>
            """.trimIndent()

            SentenceType.STATEMENT -> sentence
        }
    }

    // ---------------------------------------------------------
    // INTENT MODELING (Baby Step 32)
    // ---------------------------------------------------------

    private enum class BabyIntent {
        REASSURE, WARN, PERSUADE, REFLECT, REVEAL, URGE, NEUTRAL
    }

    private val reassureWords = setOf("okay", "alright", "it’s okay", "you’re safe", "don’t worry", "it will be fine")
    private val warnWords = setOf("careful", "warning", "danger", "risky", "be careful", "watch out")
    private val persuadeWords = setOf("should", "need to", "have to", "must", "trust me", "believe me")
    private val reflectWords = setOf("I think", "I feel", "I wonder", "maybe", "sometimes", "it seems")
    private val revealWords = setOf("actually", "in fact", "the truth is", "what’s really", "let me tell you")
    private val urgeWords = setOf("now", "right now", "hurry", "quick", "immediately", "do it")

    private fun detectIntent(sentence: String, emotion: BabyEmotion): BabyIntent {
        val lower = sentence.lowercase()

        return when {
            reassureWords.any { lower.contains(it) } ||
                    (emotion == BabyEmotion.LOVE || emotion == BabyEmotion.SAD) && lower.contains("you") ->
                BabyIntent.REASSURE

            warnWords.any { lower.contains(it) } ||
                    (emotion == BabyEmotion.SCARED || emotion == BabyEmotion.ANGRY) && lower.contains("careful") ->
                BabyIntent.WARN

            persuadeWords.any { lower.contains(it) } ->
                BabyIntent.PERSUADE

            reflectWords.any { lower.contains(it) } ||
                    emotion == BabyEmotion.THINKING ->
                BabyIntent.REFLECT

            revealWords.any { lower.contains(it) } ->
                BabyIntent.REVEAL

            urgeWords.any { lower.contains(it) } ||
                    (emotion == BabyEmotion.SURPRISED || emotion == BabyEmotion.HAPPY) && lower.contains("now") ->
                BabyIntent.URGE

            else -> BabyIntent.NEUTRAL
        }
    }

    private fun intentWrap(
        sentence: String,
        intent: BabyIntent,
        emotion: BabyEmotion,
        intensity: Double
    ): String {
        return when (intent) {

            BabyIntent.REASSURE -> """
                <prosody rate="${90 - intensity * 5}%" pitch="-1st">
                    $sentence
                </prosody>
            """.trimIndent()

            BabyIntent.WARN -> """
                <prosody rate="${105 + intensity * 10}%" pitch="+2st">
                    <emphasis level="strong">$sentence</emphasis>
                </prosody>
            """.trimIndent()

            BabyIntent.PERSUADE -> """
                <prosody rate="${100 + intensity * 5}%" pitch="+1st">
                    $sentence
                </prosody>
            """.trimIndent()

            BabyIntent.REFLECT -> """
                <prosody rate="${85 - intensity * 5}%" pitch="-1st">
                    <break time="120ms"/> $sentence
                </prosody>
            """.trimIndent()

            BabyIntent.REVEAL -> """
                <prosody rate="${95 + intensity * 5}%" pitch="+2st">
                    <break time="80ms"/> $sentence
                </prosody>
            """.trimIndent()

            BabyIntent.URGE -> """
                <prosody rate="${115 + intensity * 10}%" pitch="+3st">
                    <emphasis level="strong">$sentence</emphasis>
                </prosody>
            """.trimIndent()

            BabyIntent.NEUTRAL -> sentence
        }
    }

    // ---------------------------------------------------------
    // EMOTIONAL TIMBRE SHAPING
    // ---------------------------------------------------------

    private data class TimbreParams(
        val brightness: Double,
        val warmth: Double,
        val sharpness: Double
    )

    private fun timbreForEmotion(emotion: BabyEmotion, intensity: Double): TimbreParams {
        val scaled = { v: Double -> (v * (0.5 + intensity * 0.5)).coerceIn(0.0, 1.0) }

        return when (emotion) {
            BabyEmotion.HAPPY -> TimbreParams(scaled(0.9), scaled(0.6), scaled(0.3))
            BabyEmotion.SAD -> TimbreParams(scaled(0.2), scaled(0.9), scaled(0.2))
            BabyEmotion.ANGRY -> TimbreParams(scaled(0.7), scaled(0.3), scaled(0.9))
            BabyEmotion.SURPRISED -> TimbreParams(scaled(1.0), scaled(0.4), scaled(0.7))
            BabyEmotion.SCARED -> TimbreParams(scaled(0.3), scaled(0.5), scaled(0.8))
            BabyEmotion.LOVE -> TimbreParams(scaled(0.6), scaled(1.0), scaled(0.2))
            BabyEmotion.THINKING -> TimbreParams(scaled(0.4), scaled(0.7), scaled(0.3))
            BabyEmotion.CURIOUS -> TimbreParams(scaled(0.8), scaled(0.5), scaled(0.4))
            else -> TimbreParams(scaled(0.5), scaled(0.5), scaled(0.5))
        }
    }

    private fun timbreWrap(innerProsody: String, emotion: BabyEmotion, intensity: Double): String {
        val t = timbreForEmotion(emotion, intensity)

        val baseVolume = 100.0
        val volumeDelta = (t.warmth * 8.0) - (t.sharpness * 4.0)
        val volume = (baseVolume + volumeDelta).coerceIn(75.0, 115.0).toInt()

        val diff = t.brightness - t.warmth
        val pitchOffset = when {
            diff > 0.35 -> "+2st"
            diff > 0.15 -> "+1st"
            diff < -0.35 -> "-2st"
            diff < -0.15 -> "-1st"
            else -> "0st"
        }

        return """
            <prosody volume="${volume}%" pitch="$pitchOffset">
                $innerProsody
            </prosody>
        """.trimIndent()
    }

    // ---------------------------------------------------------
    // BREATH‑CURVE MODELING
    // ---------------------------------------------------------

    private fun breathCurveWrap(text: String, emotion: BabyEmotion, intensity: Double): String {
        val startBoost = (40 + intensity * 40).toInt()
        val midDip = (10 + intensity * 20).toInt()
        val endTaper = (30 + intensity * 50).toInt()

        return """
            <prosody volume="+${startBoost}%">
                $text
            </prosody>
            <prosody volume="-${midDip}%"></prosody>
            <prosody volume="-${endTaper}%"></prosody>
        """.trimIndent()
    }

    // ---------------------------------------------------------
    // MULTI‑SENTENCE PIPELINE
    // ---------------------------------------------------------

    private fun splitIntoSentences(text: String): List<String> =
        text.split(Regex("(?<=[.!?])\\s+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    private fun breathStrength(emotion: BabyEmotion, intensity: Double): String {
        return when (emotion) {
            BabyEmotion.SAD -> if (intensity > 0.6) "strong" else "medium"
            BabyEmotion.SCARED -> if (intensity > 0.5) "strong" else "medium"
            BabyEmotion.ANGRY -> if (intensity > 0.7) "medium" else "weak"
            BabyEmotion.LOVE -> if (intensity > 0.5) "medium" else "x-weak"
            BabyEmotion.THINKING -> "x-weak"
            BabyEmotion.CURIOUS -> "x-weak"
            BabyEmotion.HAPPY -> "weak"
            BabyEmotion.SURPRISED -> "weak"
            else -> "x-weak"
        }
    }

    private fun sentencePause(emotion: BabyEmotion, intensity: Double): Int {
        val base = when (emotion) {
            BabyEmotion.HAPPY -> 120
            BabyEmotion.SAD -> 300
            BabyEmotion.ANGRY -> 80
            BabyEmotion.SURPRISED -> 150
            BabyEmotion.SCARED -> 250
            BabyEmotion.LOVE -> 180
            BabyEmotion.THINKING -> 250
            BabyEmotion.CURIOUS -> 150
            else -> 150
        }
        return (base + intensity * base).toInt()
    }

    private fun ssmlForSentence(sentence: String, emotion: BabyEmotion, intensity: Double): String {
        val type = detectSentenceType(sentence)

        val microPaused = injectMicroPauses(sentence, intensity)
        val emphasized = emphasizeKeywords(microPaused, emotion, intensity)

        if (isOverdrive(intensity)) {
            return applyOverdrive(emphasized, emotion)
        }

        val cadenced = cadenceWrap(emphasized, type, emotion, intensity)

        val intent = detectIntent(sentence, emotion)
        val intentShaped = intentWrap(cadenced, intent, emotion, intensity)

        val pitch = when (emotion) {
            BabyEmotion.HAPPY -> "+${4 + intensity * 4}st"
            BabyEmotion.SAD -> "-${4 + intensity * 4}st"
            BabyEmotion.ANGRY -> "+${2 + intensity * 6}st"
            BabyEmotion.SURPRISED -> "+${6 + intensity * 6}st"
            BabyEmotion.SCARED -> "-${2 + intensity * 4}st"
            BabyEmotion.LOVE -> "+${3 + intensity * 3}st"
            BabyEmotion.CURIOUS -> "+${2 + intensity * 4}st"
            else -> "0st"
        }

        val rate = when (emotion) {
            BabyEmotion.HAPPY -> "${115 + intensity * 15}%"
            BabyEmotion.SAD -> "${85 - intensity * 10}%"
            BabyEmotion.ANGRY -> "${125 + intensity * 25}%"
            BabyEmotion.SURPRISED -> "${130 + intensity * 20}%"
            BabyEmotion.SCARED -> "${95 - intensity * 10}%"
            BabyEmotion.LOVE -> "${110 + intensity * 10}%"
            BabyEmotion.THINKING -> "${95 - intensity * 5}%"
            BabyEmotion.CURIOUS -> "${105 + intensity * 10}%"
            else -> "100%"
        }

        val baseProsody = """
            <prosody pitch="$pitch" rate="$rate">
                $intentShaped
            </prosody>
        """.trimIndent()

        val timbred = timbreWrap(baseProsody, emotion, intensity)
        val breathed = breathCurveWrap(timbred, emotion, intensity)

        return if (shouldWhisper(emotion, intensity))
            whisperWrap(breathed, intensity)
        else
            breathed
    }

    private fun buildMultiSentenceSsml(text: String, emotion: BabyEmotion, intensity: Double): String {
        val sentences = splitIntoSentences(text)
        val pause = sentencePause(emotion, intensity)
        val breath = breathStrength(emotion, intensity)

        val shaped = sentences.joinToString("\n") { sentence ->
            ssmlForSentence(sentence, emotion, intensity) +
                    "\n<break strength=\"$breath\" time=\"${pause}ms\"/>"
        }

        val preInhale =
            if (emotion == BabyEmotion.SAD || emotion == BabyEmotion.SCARED)
                "<break strength=\"weak\" time=\"180ms\"/>"
            else
                ""

        return """
            <speak>
                $preInhale
                $shaped
            </speak>
        """.trimIndent()
    }

    // ---------------------------------------------------------
    // SPEAK
    // ---------------------------------------------------------

    fun speak(scope: CoroutineScope, text: String, emotion: BabyEmotion, intensity: Double) {
        scope.launch(Dispatchers.IO) {
            try {
                val persona = personaForEmotion(emotion)
                val base = baseParamsForEmotion(emotion)
                val scaled = scaledParams(base, intensity)

                val ssml = buildMultiSentenceSsml(text, emotion, intensity)

                val pcm = ttsClient.synthesize(
                    text = ssml,
                    pitch = scaled.pitch,
                    speed = scaled.speed,
                    voiceName = persona.voiceName,
                    languageCode = persona.languageCode,
                    gender = persona.gender
                )

                if (pcm.isNotEmpty()) {
                    audioPlayer.play(pcm)
                }

            } catch (e: Exception) {
                println("TTS error: ${e.message}")
            }
        }
    }

    fun stop() {
        audioPlayer.stop()
    }

    fun shutdown() {
        ttsClient.shutdown()
    }
}

/**
 * Voice parameters container.
 */
data class VoiceParams(
    val pitch: Double,
    val speed: Double
)
