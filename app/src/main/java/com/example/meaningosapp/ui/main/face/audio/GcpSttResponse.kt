package com.example.meaningosapp.ui.main.face.audio

import kotlinx.serialization.Serializable

@Serializable
data class GcpSttResponse(
    val results: List<GcpSttResult> = emptyList()
)

@Serializable
data class GcpSttResult(
    val alternatives: List<GcpSttAlternative> = emptyList()
)

@Serializable
data class GcpSttAlternative(
    val transcript: String? = null,
    val confidence: Float? = null
)


