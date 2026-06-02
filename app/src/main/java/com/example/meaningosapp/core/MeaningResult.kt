package com.example.meaningosapp.core

import com.example.meaningosapp.core.OSAction

data class MeaningResult(
    val text: String,
    val speak: Boolean = false,
    val speechText: String? = null,
    val action: OSAction = OSAction.None,
    val originalText: String
)
