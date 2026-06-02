package com.example.meaningosapp.ui.main.face.audio

import kotlinx.serialization.Serializable

@Serializable
data class GcpSttRequest(
    val config: GcpSttConfig,
    val audio: GcpSttAudio
)

@Serializable
data class GcpSttConfig(
    val encoding: String = "LINEAR16",
    val sampleRateHertz: Int,
    val languageCode: String,
    val enableAutomaticPunctuation: Boolean = true
)

@Serializable
data class GcpSttAudio(
    val content: String
)


