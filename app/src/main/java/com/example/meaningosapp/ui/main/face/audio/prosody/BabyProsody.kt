package com.example.meaningosapp.ui.main.face.audio.prosody

class BabyProsody {

    /**
     * Injects micro‑pauses into text to improve natural rhythm.
     * Example: “I think so” → “I… think… so”
     */
    fun injectMicroPauses(text: String): String {
        if (text.isBlank()) return text

        val words = text.split(" ")
        return words.joinToString(" ") { word ->
            if (word.length > 3) "$word…" else word
        }
    }

    /**
     * Adds light emphasis to emotionally important keywords.
     * This is a safe, minimal SSML transformation.
     */
    fun emphasizeKeywords(text: String): String {
        if (text.isBlank()) return text

        val keywords = setOf(
            "really", "very", "so", "important",
            "love", "hate", "think", "feel",
            "yes", "no", "please", "now"
        )

        return text.split(" ").joinToString(" ") { word ->
            val clean = word.lowercase().trim('.', ',', '?', '!')
            if (clean in keywords) "<emphasis level=\"moderate\">$word</emphasis>" else word
        }
    }

    /**
     * Overdrive mode: increases intensity by boosting emphasis
     * and adding slight pitch/energy cues.
     */
    fun applyOverdrive(text: String): String {
        if (text.isBlank()) return text

        val strongWords = text.split(" ").map { word ->
            if (word.length > 3)
                "<emphasis level=\"strong\">$word</emphasis>"
            else
                word
        }.joinToString(" ")

        return "<prosody pitch=\"+10%\" rate=\"105%\">$strongWords</prosody>"
    }

    /**
     * Whisper mode: soft, intimate delivery with reduced volume,
     * lower pitch, and slower cadence.
     */
    fun applyWhisper(text: String): String {
        if (text.isBlank()) return text

        return """
            <prosody volume="-20dB" pitch="-15%" rate="90%">
                $text
            </prosody>
        """.trimIndent()
    }

    /**
     * Detects the sentence type based on punctuation and tone.
     * Returns: "question", "exclamation", "statement", or "neutral".
     */
    fun detectSentenceType(text: String): String {
        val trimmed = text.trim()

        return when {
            trimmed.endsWith("?") -> "question"
            trimmed.endsWith("!") -> "exclamation"
            trimmed.endsWith(".") -> "statement"
            else -> "neutral"
        }
    }

    /**
     * Cadence shaping adjusts pitch, rate, and contour based on
     * the detected sentence type.
     */
    fun applyCadenceShaping(text: String): String {
        if (text.isBlank()) return text

        return when (detectSentenceType(text)) {

            "question" -> """
                <prosody pitch="+12%" rate="95%">
                    $text
                </prosody>
            """.trimIndent()

            "exclamation" -> """
                <prosody pitch="+15%" rate="108%">
                    $text
                </prosody>
            """.trimIndent()

            "statement" -> """
                <prosody pitch="0%" rate="100%">
                    $text
                </prosody>
            """.trimIndent()

            else -> """
                <prosody pitch="-2%" rate="98%">
                    $text
                </prosody>
            """.trimIndent()
        }
    }

    /**
     * The full prosody pipeline:
     * 1. Cadence shaping
     * 2. Keyword emphasis
     * 3. Micro‑pauses
     * 4. Whisper/Overdrive modes
     */
    fun applyProsodyPipeline(text: String, mode: String = "normal"): String {
        if (text.isBlank()) return text

        // Step 1: cadence shaping
        var shaped = applyCadenceShaping(text)

        // Step 2: keyword emphasis
        shaped = emphasizeKeywords(shaped)

        // Step 3: micro‑pauses
        shaped = injectMicroPauses(shaped)

        // Step 4: optional modes
        shaped = when (mode.lowercase()) {
            "whisper" -> applyWhisper(shaped)
            "overdrive" -> applyOverdrive(shaped)
            else -> shaped
        }

        return shaped
    }
}
