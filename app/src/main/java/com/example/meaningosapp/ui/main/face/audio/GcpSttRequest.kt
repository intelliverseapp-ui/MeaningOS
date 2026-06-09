// FILE: GcpSttRequest.kt
package com.example.meaningosapp.ui.main.face.audio

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * GcpSttRequest
 *
 * Represents the two types of messages sent to Google Cloud STT:
 *  - Config (sent once at the start)
 *  - AudioChunk (sent repeatedly with PCM16 bytes)
 *
 * NOTE:
 *  AudioChunk is NOT @Serializable because raw PCM bytes are never serialized.
 *  They are sent directly over the gRPC stream as ByteString.
 */
sealed class GcpSttRequest {

    /**
     * Initial configuration message for streaming STT.
     *
     * The Speech v1 API accepts:
     *  - encoding = "LINEAR16"
     *  - sampleRateHz = 16000 (or caller‑provided)
     *  - languageCode = "en-US"
     *  - model = "" (server chooses default)
     *
     * recognizer is optional and only used for Speech v2 resource‑style configs.
     */
    @Serializable
    @SerialName("config")
    data class Config(
        val encoding: String = "LINEAR16",
        val sampleRateHz: Int,
        val languageCode: String,
        val model: String = "",
        val enablePartialResults: Boolean = true,
        val recognizer: String? = null
    ) : GcpSttRequest() {
        init {
            require(sampleRateHz > 0) { "sampleRateHz must be > 0" }
        }
    }

    /**
     * Raw PCM16 audio chunk.
     *
     * Not serializable — sent directly as ByteString in the transport layer.
     */
    @SerialName("audio_chunk")
    data class AudioChunk(
        val pcm: ByteArray
    ) : GcpSttRequest()
}
