package com.example.meaningosapp

object IntentEngine {

    fun handleMeaning(spoken: String): MeaningResult {
        val raw = spoken.trim()
        val lower = raw.lowercase()

        when {
            lower == "stop" || lower == "stop speaking" || lower == "be quiet" ->
                return MeaningResult(
                    text = "Okay, I will be quiet.",
                    speak = false,
                    action = OSAction.StopSpeech
                )

            lower == "open browser" || lower == "open the browser" ->
                return MeaningResult(
                    text = "Opening browser",
                    speak = true,
                    action = OSAction.OpenUrl("https://www.google.com")
                )
        }

        extractPercent(lower)?.let { percent ->
            return MeaningResult(
                text = "Volume set to ${percent}%.",
                speak = true,
                action = OSAction.SetVolume(percent)
            )
        }

        if (lower.contains("volume up") || lower.contains("increase volume") || lower.contains("turn up the volume")) {
            return MeaningResult(
                text = "Increasing volume to 80%.",
                speak = true,
                action = OSAction.SetVolume(80)
            )
        }

        if (lower.contains("volume down") || lower.contains("decrease volume") || lower.contains("turn down the volume")) {
            return MeaningResult(
                text = "Decreasing volume to 20%.",
                speak = true,
                action = OSAction.SetVolume(20)
            )
        }

        if (lower.contains("youtube")) {
            return MeaningResult(
                text = "Opening YouTube",
                speak = true,
                action = OSAction.LaunchApp("com.google.android.youtube")
            )
        }

        parseOpenUrl(lower)?.let { url ->
            return MeaningResult(
                text = "Opening link",
                speak = true,
                action = OSAction.OpenUrl(url)
            )
        }

        parseLaunchApp(lower)?.let { packageName ->
            return MeaningResult(
                text = "Opening app",
                speak = true,
                action = OSAction.LaunchApp(packageName)
            )
        }

        return MeaningResult(
            text = "Meaning: $raw",
            speak = true,
            speechText = "I heard: $raw",
            action = OSAction.None
        )
    }

    private fun extractPercent(text: String): Int? {
        val percentRegex = Regex("""\b(\d{1,3})\s*%?\s*(percent)?\b""")
        val match = percentRegex.find(text) ?: return null
        val value = match.groupValues[1].toIntOrNull() ?: return null
        return value.coerceIn(0, 100)
    }

    private fun parseOpenUrl(text: String): String? {
        val httpRegex = Regex("""(https?://[^\s]+)""")
        httpRegex.find(text)?.let { return it.groupValues[1] }

        val domainRegex = Regex("""\b((?:www\.)?[a-z0-9\-]+\.[a-z]{2,}(?:/[^\s]*)?)\b""")
        domainRegex.find(text)?.let {
            var domain = it.groupValues[1]
            if (!domain.startsWith("http")) domain = "https://$domain"
            return domain
        }

        return null
    }

    private fun parseLaunchApp(text: String): String? {
        val openRegex = Regex("""\b(open|launch|start)\s+([a-z0-9\s]+)""")
        val match = openRegex.find(text) ?: return null
        val appName = match.groupValues[2].trim()

        return when {
            appName.contains("spotify") -> "com.spotify.music"
            appName.contains("chrome") || appName.contains("browser") -> "com.android.chrome"
            appName.contains("gmail") || appName.contains("email") -> "com.google.android.gm"
            appName.contains("calculator") -> "com.android.calculator2"
            appName.contains("camera") -> "com.google.android.camera"
            appName.contains("youtube") -> "com.google.android.youtube"
            appName.contains("maps") -> "com.google.android.apps.maps"
            else -> null
        }
    }
}
