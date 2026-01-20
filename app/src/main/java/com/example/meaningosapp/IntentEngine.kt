package com.example.meaningosapp

object IntentEngine {

    fun handleMeaning(input: String): String {
        val normalized = input.trim().lowercase()

        return when {
            normalized.startsWith("hello") -> {
                "Hi Mike, I'm online and listening."
            }

            normalized.contains("status") -> {
                "MeaningOS is running normally."
            }

            normalized.contains("test") -> {
                "Test acknowledged."
            }

            else -> {
                "I heard you, but I don’t have meaning for that yet."
            }
        }
    }
}
