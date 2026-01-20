package com.example.meaningosapp

// Meaning layer: pure, side‑effect free
sealed class OSAction {
    object None : OSAction()
    data class LaunchApp(val packageName: String) : OSAction()
    data class OpenUrl(val url: String) : OSAction()
    data class SetVolume(val percent: Int) : OSAction()
    object StopSpeech : OSAction()
}

data class MeaningResult(
    val text: String,
    val speak: Boolean,
    val speechText: String? = null,
    val action: OSAction = OSAction.None
)

object IntentEngine {

    fun handleMeaning(input: String): MeaningResult {
        val normalized = input.trim().lowercase()

        return when {
            normalized.startsWith("hello") -> {
                MeaningResult(
                    text = "Hi Mike, I'm here and listening.",
                    speak = true,
                    speechText = "Hi Mike, I'm here and listening."
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

            normalized.contains("open chrome") || normalized.contains("open browser") -> {
                MeaningResult(
                    text = "Opening Chrome.",
                    speak = true,
                    speechText = "Opening Chrome now.",
                    action = OSAction.LaunchApp(packageName = "com.android.chrome")
                )
            }

            normalized.startsWith("search for ") -> {
                val query = normalized.removePrefix("search for ").trim()
                val url = "https://www.google.com/search?q=" + java.net.URLEncoder.encode(query, "UTF-8")
                MeaningResult(
                    text = "Searching for $query.",
                    speak = true,
                    speechText = "Searching for $query now.",
                    action = OSAction.OpenUrl(url = url)
                )
            }

            normalized.contains("who are you") || normalized.contains("what's your name") || normalized.contains("whats your name") -> {
                MeaningResult(
                    text = "I am Computer.",
                    speak = true,
                    speechText = "I am Computer."
                )
            }

            normalized == "stop" || normalized == "be quiet" -> {
                MeaningResult(
                    text = "Okay, I will be quiet.",
                    speak = false,
                    speechText = null,
                    action = OSAction.StopSpeech
                )
            }

            normalized.startsWith("set volume to ") -> {
                val percentText = normalized.removePrefix("set volume to ").trim().split(" ").firstOrNull()
                val percent = percentText?.filter { it.isDigit() }?.toIntOrNull() ?: 50
                MeaningResult(
                    text = "Volume set to $percent percent.",
                    speak = true,
                    speechText = "Volume set to $percent percent.",
                    action = OSAction.SetVolume(percent = percent.coerceIn(0, 100))
                )
            }

            else -> {
                MeaningResult(
                    text = "I don't know that yet.",
                    speak = true,
                    speechText = "I don't know that yet.",
                    action = OSAction.None
                )
            }
        }
    }
}
