package com.example.meaningosapp.ui.main.face.emotion

/**
 * SemanticEmojiEngine
 *
 * Version 9 — STG geometry + superposition.
 *
 * Baby Node now:
 *  - infers a 6D STG relational vector (presence, identity, relationship, attention, dependence, curiosity)
 *  - projects that into emotional axes (happiness, sadness, anger, fear, love, surprise)
 *  - holds multiple emotional states in superposition
 *  - uses an adaptive (Riemannian-style) curvature factor based on intensity
 *  - collapses along a geodesic-like path into a final emotional vector
 *  - maps that vector into an emoji, with lensing, memory, decay, smoothing, and contextual weighting
 */
object SemanticEmojiEngine {

    // ---------------------------------------------------------
    // INTERNAL DATA MODELS
    // ---------------------------------------------------------

    private data class StgVector(
        val presence: Float,
        val identity: Float,
        val relationship: Float,
        val attention: Float,
        val dependence: Float,
        val curiosity: Float
    )

    private data class EmotionVector(
        val happiness: Float,
        val sadness: Float,
        val anger: Float,
        val fear: Float,
        val love: Float,
        val surprise: Float
    ) {
        fun magnitude(): Float {
            return kotlin.math.sqrt(
                happiness * happiness +
                        sadness * sadness +
                        anger * anger +
                        fear * fear +
                        love * love +
                        surprise * surprise
            )
        }

        fun normalize(): EmotionVector {
            val mag = magnitude()
            if (mag <= 1e-6f) return this
            return EmotionVector(
                happiness / mag,
                sadness / mag,
                anger / mag,
                fear / mag,
                love / mag,
                surprise / mag
            )
        }

        operator fun plus(other: EmotionVector): EmotionVector {
            return EmotionVector(
                happiness + other.happiness,
                sadness + other.sadness,
                anger + other.anger,
                fear + other.fear,
                love + other.love,
                surprise + other.surprise
            )
        }

        operator fun times(scale: Float): EmotionVector {
            return EmotionVector(
                happiness * scale,
                sadness * scale,
                anger * scale,
                fear * scale,
                love * scale,
                surprise * scale
            )
        }
    }

    // ---------------------------------------------------------
    // EMOTIONAL MEMORY + DECAY + SMOOTHING
    // ---------------------------------------------------------

    private var lastEmoji: String = "🙂"
    private var lastSentiment: Float = 0f
    private var lastIntensity: Float = 0f
    private var lastEmotionVector: EmotionVector = EmotionVector(0f, 0f, 0f, 0f, 0f, 0f)

    private const val SENTIMENT_DECAY = 0.15f
    private const val INTENSITY_DECAY = 0.20f

    private val smoothingBuffer = ArrayDeque<String>()
    private const val SMOOTHING_WINDOW = 5

    private const val USER_EMOTION_WEIGHT = 1.0f
    private const val NODE_EMOTION_WEIGHT = 0.6f

    private const val LENS_STRENGTH = 0.55f

    // ---------------------------------------------------------
    // PUBLIC ENTRY POINT
    // ---------------------------------------------------------

    fun pickEmoji(signals: EmotionSignals): String {

        applyDecay()

        // Hard overrides
        if (signals.isThinking) {
            lastEmoji = "🤔"
            pushToBuffer(lastEmoji)
            return smoothEmoji()
        }

        if (signals.isListening) {
            lastEmoji = "👂"
            pushToBuffer(lastEmoji)
            return smoothEmoji()
        }

        val user = signals.userText?.lowercase().orEmpty()
        val reply = signals.nodeReply?.lowercase().orEmpty()

        val combined = buildString {
            append(user.repeatWeight(USER_EMOTION_WEIGHT))
            append(" ")
            append(reply.repeatWeight(NODE_EMOTION_WEIGHT))
        }

        val sentiment = signals.sentimentScore ?: 0f
        val intensity = (signals.intensity ?: 0f).coerceIn(0f, 1f)

        // ---------------------------------------------------------
        // STG GEOMETRY + SUPERPOSITION
        // ---------------------------------------------------------

        val stg = inferStgVector(combined, sentiment, intensity)

        val baseEmotion = projectStgToEmotion(stg, sentiment, intensity)
        val keywordEmotion = classifyKeywordEmotion(combined, sentiment, intensity)
        val sentimentEmotion = sentimentToEmotion(sentiment, intensity)

        val superposed = listOf(baseEmotion, keywordEmotion, sentimentEmotion)

        val superposedVector = superpose(superposed, intensity)

        val collapsedVector = riemannianCollapse(lastEmotionVector, superposedVector, intensity)

        val rawEmoji = mapEmotionVectorToEmoji(collapsedVector)
        val lensedEmoji = lensEmotion(lastEmoji, rawEmoji, intensity)

        lastEmotionVector = collapsedVector
        lastEmoji = lensedEmoji
        lastSentiment = sentiment
        lastIntensity = intensity

        pushToBuffer(lensedEmoji)
        return smoothEmoji()
    }

    // ---------------------------------------------------------
    // STG INFERENCE + PROJECTION
    // ---------------------------------------------------------

    private fun inferStgVector(text: String, sentiment: Float, intensity: Float): StgVector {
        val lower = text.lowercase()

        val presence = when {
            listOf("here", "with you", "together", "stay", "still there").any { it in lower } -> 0.8f
            else -> 0.4f + 0.3f * intensity
        }

        val identity = when {
            listOf("i am", "i'm", "about me", "who i am").any { it in lower } -> 0.8f
            else -> 0.4f + 0.2f * sentiment.coerceAtLeast(0f)
        }

        val relationship = when {
            listOf("friend", "care", "love", "trust", "us", "we").any { it in lower } -> 0.9f
            else -> 0.3f + 0.4f * sentiment.coerceAtLeast(0f)
        }

        val attention = when {
            listOf("listen", "hear", "look", "notice", "focus").any { it in lower } -> 0.9f
            else -> 0.3f + 0.5f * intensity
        }

        val dependence = when {
            listOf("need you", "need help", "can't", "depend", "rely").any { it in lower } -> 0.9f
            sentiment < -0.4f -> 0.6f
            else -> 0.3f
        }

        val curiosity = when {
            listOf("why", "how", "what if", "wonder", "curious").any { it in lower } -> 0.9f
            else -> 0.3f + 0.4f * intensity
        }

        return StgVector(
            presence.coerceIn(0f, 1f),
            identity.coerceIn(0f, 1f),
            relationship.coerceIn(0f, 1f),
            attention.coerceIn(0f, 1f),
            dependence.coerceIn(0f, 1f),
            curiosity.coerceIn(0f, 1f)
        )
    }

    private fun projectStgToEmotion(stg: StgVector, sentiment: Float, intensity: Float): EmotionVector {

        val happiness =
            0.4f * stg.presence +
                    0.3f * stg.identity +
                    0.2f * stg.relationship +
                    0.1f * stg.curiosity +
                    0.5f * sentiment.coerceAtLeast(0f)

        val sadness =
            0.4f * stg.dependence +
                    0.2f * (1f - stg.presence) +
                    0.2f * (1f - stg.relationship) +
                    0.4f * (-sentiment).coerceAtLeast(0f)

        val anger =
            0.4f * (1f - stg.relationship) +
                    0.3f * (1f - stg.identity) +
                    0.3f * (-sentiment).coerceAtLeast(0f) * intensity

        val fear =
            0.4f * stg.dependence +
                    0.3f * (1f - stg.presence) +
                    0.3f * (-sentiment).coerceAtLeast(0f)

        val love =
            0.5f * stg.relationship +
                    0.2f * stg.presence +
                    0.2f * stg.identity +
                    0.1f * sentiment.coerceAtLeast(0f)

        val surprise =
            0.5f * stg.curiosity +
                    0.3f * intensity +
                    0.2f * kotlin.math.abs(sentiment)

        return EmotionVector(
            happiness.coerceAtLeast(0f),
            sadness.coerceAtLeast(0f),
            anger.coerceAtLeast(0f),
            fear.coerceAtLeast(0f),
            love.coerceAtLeast(0f),
            surprise.coerceAtLeast(0f)
        )
    }

    // ---------------------------------------------------------
    // KEYWORD + SENTIMENT EMOTIONS
    // ---------------------------------------------------------

    private fun classifyKeywordEmotion(text: String, sentiment: Float, intensity: Float): EmotionVector {
        val lower = text.lowercase()

        return when {
            listOf("love", "care", "appreciate", "thank you").any { it in lower } ->
                EmotionVector(0.3f, 0f, 0f, 0f, 1.0f, 0.2f) * (0.5f + 0.5f * intensity)

            listOf("funny", "lol", "haha", "joke", "lmao").any { it in lower } ->
                EmotionVector(1.0f, 0f, 0f, 0f, 0.2f, 0.4f) * (0.5f + 0.5f * intensity)

            listOf("sorry", "sad", "hurt", "pain", "upset", "lonely").any { it in lower } ->
                EmotionVector(0f, 1.0f, 0.1f, 0.3f, 0.4f, 0f) * (0.5f + 0.5f * intensity)

            listOf("angry", "mad", "frustrated", "annoyed", "irritated").any { it in lower } ->
                EmotionVector(0f, 0.2f, 1.0f, 0.3f, 0f, 0f) * (0.5f + 0.5f * intensity)

            listOf("confused", "unsure", "don't understand", "what?", "huh").any { it in lower } ->
                EmotionVector(0.1f, 0.2f, 0.1f, 0.4f, 0.1f, 0.6f) * (0.5f + 0.5f * intensity)

            listOf("wow", "really?", "no way", "surprised", "omg").any { it in lower } ->
                EmotionVector(0.5f, 0.1f, 0.1f, 0.2f, 0.2f, 1.0f) * (0.5f + 0.5f * intensity)

            else -> EmotionVector(0f, 0f, 0f, 0f, 0f, 0f)
        }
    }

    private fun sentimentToEmotion(sentiment: Float, intensity: Float): EmotionVector {
        return when {
            sentiment > 0.2f ->
                EmotionVector(0.8f, 0f, 0f, 0f, 0.4f, 0.2f) * (0.3f + 0.7f * intensity)

            sentiment < -0.2f ->
                EmotionVector(0f, 0.8f, 0.3f, 0.3f, 0.1f, 0f) * (0.3f + 0.7f * intensity)

            else ->
                EmotionVector(0.2f, 0.1f, 0f, 0.1f, 0.1f, 0.1f) * 0.3f
        }
    }

    // ---------------------------------------------------------
    // SUPERPOSITION + COLLAPSE
    // ---------------------------------------------------------

    private fun superpose(vectors: List<EmotionVector>, intensity: Float): EmotionVector {
        if (vectors.isEmpty()) return EmotionVector(0f, 0f, 0f, 0f, 0f, 0f)

        val baseWeight = 0.4f + 0.6f * intensity
        var acc = EmotionVector(0f, 0f, 0f, 0f, 0f, 0f)

        vectors.forEachIndexed { index, v ->
            val w = when (index) {
                0 -> baseWeight
                1 -> 0.6f * baseWeight
                2 -> 0.5f * baseWeight
                else -> 0.3f * baseWeight
            }
            acc += v * w
        }

        return acc.normalize()
    }

    private fun riemannianCollapse(
        previous: EmotionVector,
        current: EmotionVector,
        intensity: Float
    ): EmotionVector {
        val curvature = 0.2f + 0.8f * intensity
        val alpha = curvature.coerceIn(0f, 1f)

        val blended = EmotionVector(
            happiness = (1 - alpha) * previous.happiness + alpha * current.happiness,
            sadness   = (1 - alpha) * previous.sadness   + alpha * current.sadness,
            anger     = (1 - alpha) * previous.anger     + alpha * current.anger,
            fear      = (1 - alpha) * previous.fear      + alpha * current.fear,
            love      = (1 - alpha) * previous.love      + alpha * current.love,
            surprise  = (1 - alpha) * previous.surprise  + alpha * current.surprise
        )

        return blended.normalize()
    }

    // ---------------------------------------------------------
    // EMOJI MAPPING + LENSING
    // ---------------------------------------------------------

    private fun mapEmotionVectorToEmoji(ev: EmotionVector): String {
        val components = listOf(
            "happiness" to ev.happiness,
            "sadness" to ev.sadness,
            "anger" to ev.anger,
            "fear" to ev.fear,
            "love" to ev.love,
            "surprise" to ev.surprise
        )

        val (dominant, value) = components.maxByOrNull { it.second } ?: ("happiness" to 0f)

        val secondary = components
            .filter { it.first != dominant }
            .maxByOrNull { it.second }
        val secondaryName = secondary?.first
        val secondaryValue = secondary?.second ?: 0f

        // Mixed / emergent states
        if (value > 0.4f && secondaryValue > 0.25f) {

            // sadness + love → 🥺 / 🥲
            if ((dominant == "sadness" && secondaryName == "love") ||
                (dominant == "love" && secondaryName == "sadness")
            ) {
                return if (ev.happiness > 0.2f) "🥲" else "🥺"
            }

            // fear + surprise → 😨
            if ((dominant == "fear" && secondaryName == "surprise") ||
                (dominant == "surprise" && secondaryName == "fear")
            ) {
                return "😨"
            }

            // happiness + surprise → 🤩
            if ((dominant == "happiness" && secondaryName == "surprise") ||
                (dominant == "surprise" && secondaryName == "happiness")
            ) {
                return "🤩"
            }

            // anger + fear → 😤 / 😠
            if ((dominant == "anger" && secondaryName == "fear") ||
                (dominant == "fear" && secondaryName == "anger")
            ) {
                return if (value > 0.6f) "😤" else "😠"
            }
        }

        // Single-axis mapping fallback
        return when (dominant) {
            "happiness" -> if (value > 0.6f) "😄" else "🙂"
            "sadness"   -> if (value > 0.6f) "😭" else "😢"
            "anger"     -> if (value > 0.6f) "🤬" else "😡"
            "fear"      -> if (value > 0.6f) "😱" else "😨"
            "love"      -> if (value > 0.6f) "🥰" else "❤️"
            "surprise"  -> if (value > 0.6f) "🤯" else "😮"
            else        -> "🙂"
        }
    }

    private fun lensEmotion(previous: String, current: String, intensity: Float): String {
        val curvature = intensity * LENS_STRENGTH
        if (previous == current) return current
        if (curvature < 0.2f) return current

        return when (previous to current) {

            // Sadness → happiness (bittersweet)
            "😢" to "😄",
            "😭" to "🤩" ->
                if (curvature > 0.5f) "🥺" else "🙂"

            // Happiness → sadness (compassion)
            "😄" to "😢",
            "🤩" to "😭" ->
                if (curvature > 0.5f) "🥲" else "🙁"

            // Anger → confusion (frustration)
            "😡" to "😕",
            "🤬" to "🤨" ->
                if (curvature > 0.5f) "😤" else "😠"

            // Confusion → love (shy affection)
            "😕" to "❤️",
            "🤨" to "🥰" ->
                if (curvature > 0.5f) "🥹" else "🙂"

            // Surprise → fear (shock)
            "😮" to "😢",
            "🤯" to "😭" ->
                if (curvature > 0.5f) "😱" else "😮"

            else -> current
        }
    }

    // ---------------------------------------------------------
    // DECAY + SMOOTHING + HELPERS
    // ---------------------------------------------------------

    private fun applyDecay() {
        lastSentiment = when {
            lastSentiment > 0 -> (lastSentiment - SENTIMENT_DECAY).coerceAtLeast(0f)
            lastSentiment < 0 -> (lastSentiment + SENTIMENT_DECAY).coerceAtMost(0f)
            else -> 0f
        }

        lastIntensity = (lastIntensity - INTENSITY_DECAY).coerceAtLeast(0f)

        // Reset to neutral if emotional energy is low
        if (lastIntensity < 0.2f) {
            lastEmoji = "🙂"
            lastEmotionVector = EmotionVector(0f, 0f, 0f, 0f, 0f, 0f)
        }
    }

    private fun pushToBuffer(emoji: String) {
        smoothingBuffer.addLast(emoji)
        if (smoothingBuffer.size > SMOOTHING_WINDOW) {
            smoothingBuffer.removeFirst()
        }
    }

    private fun smoothEmoji(): String {
        if (smoothingBuffer.isEmpty()) return lastEmoji

        return smoothingBuffer
            .groupingBy { it }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key ?: lastEmoji
    }

    private fun String.repeatWeight(weight: Float): String {
        val times = (weight * 3).toInt().coerceAtLeast(1)
        return this.repeat(times)
    }
}
