package com.example.meaningosapp

/**
 * Minimal placeholder IntentEngine so MainActivity can compile.
 * Replace with your real intent parsing logic.
 */
object IntentEngine {
    fun handleMeaning(spoken: String): MeaningResult {
        val lower = spoken.trim().lowercase()
        // Simple demo rules
        return when {
            lower.contains("open") && lower.contains("browser") ->
                MeaningResult(text = "Opening browser", speak = true, action = OSAction.OpenUrl("https://www.google.com"))
            lower.contains("stop") ->
                MeaningResult(text = "Stopping speech", speak = false, action = OSAction.StopSpeech)
            lower.contains("volume") && lower.contains("up") ->
                MeaningResult(text = "Increasing volume", speak = true, action = OSAction.SetVolume(80))
            else ->
                MeaningResult(text = "Meaning: $spoken", speak = true)
        }
    }
}

data class MeaningResult(
    val text: String,
    val speak: Boolean = false,
    val speechText: String? = null,
    val action: OSAction = OSAction.None
)
