package com.example.meaningosapp

data class MeaningResult(
    val text: String,
    val speak: Boolean,
    val speechText: String? = null
)

object IntentEngine {

    fun handleMeaning(input: String): MeaningResult {
        val normalized = input.trim().lowercase()

        return when {
            normalized.startsWith("hello") -> {
                MeaningResult(
                    text = "Hi Mike, I'm online and listening.",
                    speak = true,
                    speechText = "Hi Mike, I'm online and listening."
                )
            }

            normalized.contains("status") -> {
                MeaningResult(
                    text = "MeaningOS is running normally.",
                    speak = true,
                    speechText = "Everything looks good. MeaningOS is running normally."
                )
            }

            normalized.contains("test") -> {
                MeaningResult(
                    text = "Test acknowledged.",
                    speak = false,
                    speechText = null
                )
            }

            else -> {
                MeaningResult(
                    text = "I heard you, but I don't have meaning for that yet.",
                    speak = true,
                    speechText = "I heard you, but I don't have meaning for that yet."
                )
            }
        }
    }
}
