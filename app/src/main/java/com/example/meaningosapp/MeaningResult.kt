package com.example.meaningosapp

data class MeaningResult(
    val text: String,
    val speak: Boolean = false,
    val speechText: String? = null,
    val action: OSAction = OSAction.None
)
