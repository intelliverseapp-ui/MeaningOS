package com.example.meaningosapp.ui.main.face.emotion

/**
 * SentimentAnalyzer
 *
 * A lightweight, local sentiment engine for Baby Node.
 *
 * It returns:
 *  - sentimentScore:  -1.0 (very negative) → +1.0 (very positive)
 *  - intensity:       0.0 (calm) → 1.0 (strong emotion)
 *
 * Version 1 — keyword-based scoring.
 *
 * Future expansions:
 *  - contextual sentiment
 *  - multi-turn emotional deltas
 *  - tone-of-voice weighting
 *  - emotional memory
 *  - STG-aware sentiment shaping
 */
object SentimentAnalyzer {

    // ⭐ Positive emotional keywords
    private val positiveWords = listOf(
        "good", "great", "awesome", "nice", "happy", "love", "glad",
        "excellent", "wonderful", "amazing", "cool", "thanks", "thank you"
    )

    // ⭐ Negative emotional keywords
    private val negativeWords = listOf(
        "bad", "sad", "angry", "upset", "hurt", "pain", "terrible",
        "awful", "hate", "mad", "frustrated", "annoyed"
    )

    /**
     * Analyze sentiment of a text string.
     *
     * @return Pair(sentimentScore, intensity)
     */
    fun analyze(text: String): Pair<Float, Float> {
        val lower = text.lowercase()

        var score = 0f
        var hits = 0

        // Count positive words
        for (word in positiveWords) {
            if (word in lower) {
                score += 1f
                hits++
            }
        }

        // Count negative words
        for (word in negativeWords) {
            if (word in lower) {
                score -= 1f
                hits++
            }
        }

        // ⭐ Normalize sentiment score to -1.0 → +1.0
        val sentimentScore = when {
            score > 0 -> (score / (positiveWords.size / 2)).coerceIn(0f, 1f)
            score < 0 -> (score / (negativeWords.size / 2)).coerceIn(-1f, 0f)
            else -> 0f
        }

        // ⭐ Emotional intensity: how many emotional words appeared
        val intensity = (hits / 5f).coerceIn(0f, 1f)

        return Pair(sentimentScore, intensity)
    }
}
