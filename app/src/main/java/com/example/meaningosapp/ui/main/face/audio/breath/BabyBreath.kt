package com.example.meaningosapp.ui.main.face.audio.breath

class BabyBreath {

    /**
     * Adds a soft inhale before the text.
     * This is a minimal, safe SSML transformation.
     */
    fun addInhale(text: String): String {
        if (text.isBlank()) return text

        val inhale = "<breath duration=\"200ms\"/>"
        return "$inhale $text"
    }

    /**
     * Adds a gentle exhale after the text.
     * Useful for natural sentence endings or emotional release.
     */
    fun addExhale(text: String): String {
        if (text.isBlank()) return text

        val exhale = "<breath duration=\"250ms\" volume=\"-10dB\"/>"
        return "$text $exhale"
    }

    /**
     * Inserts natural breaths at clause boundaries.
     * Commas, conjunctions, and long phrases trigger a breath.
     */
    fun addNaturalBreaths(text: String): String {
        if (text.isBlank()) return text

        val breathTag = "<breath duration=\"180ms\"/>"

        val withCommas = text.replace(",", ", $breathTag")

        val conjunctions = listOf("and", "but", "or", "so", "because")
        var result = withCommas

        conjunctions.forEach { conj ->
            val pattern = " $conj "
            val replacement = " $conj $breathTag "
            result = result.replace(pattern, replacement)
        }

        return result
    }

    /**
     * Emotional breath patterns:
     * - "sad": slower, softer breaths
     * - "excited": faster, higher‑energy breaths
     * - "tired": long exhales, slow inhales
     * - "calm": balanced, gentle breathing
     */
    fun applyEmotionalBreath(text: String, emotion: String): String {
        if (text.isBlank()) return text

        return when (emotion.lowercase()) {

            "sad" -> """
                <breath duration="300ms" volume="-15dB"/>
                <prosody rate="92%" pitch="-10%">
                    $text
                </prosody>
                <breath duration="350ms" volume="-18dB"/>
            """.trimIndent()

            "excited" -> """
                <breath duration="120ms" volume="-5dB"/>
                <prosody rate="108%" pitch="+12%">
                    $text
                </prosody>
                <breath duration="150ms" volume="-6dB"/>
            """.trimIndent()

            "tired" -> """
                <breath duration="400ms" volume="-20dB"/>
                <prosody rate="88%" pitch="-12%">
                    $text
                </prosody>
                <breath duration="450ms" volume="-22dB"/>
            """.trimIndent()

            "calm" -> """
                <breath duration="220ms" volume="-10dB"/>
                <prosody rate="98%" pitch="-2%">
                    $text
                </prosody>
                <breath duration="240ms" volume="-12dB"/>
            """.trimIndent()

            else -> text
        }
    }

    /**
     * Full breath pipeline:
     * 1. Inhale
     * 2. Natural breaths
     * 3. Emotional breath shaping (optional)
     * 4. Exhale
     */
    fun applyBreathPipeline(
        text: String,
        emotion: String? = null
    ): String {
        if (text.isBlank()) return text

        // Step 1: inhale
        var shaped = addInhale(text)

        // Step 2: natural breaths
        shaped = addNaturalBreaths(shaped)

        // Step 3: emotional breath shaping (optional)
        if (emotion != null) {
            shaped = applyEmotionalBreath(shaped, emotion)
        }

        // Step 4: exhale
        shaped = addExhale(shaped)

        return shaped
    }
}
