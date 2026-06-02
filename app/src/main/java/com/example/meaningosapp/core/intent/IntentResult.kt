package com.example.meaningosapp.core.intent

data class IntentResult(
    val type: IntentType,
    val query: String = "",
    val appName: String = "",
    val volumePercent: Int? = null
)