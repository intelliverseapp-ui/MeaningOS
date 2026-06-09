// FILE: GcpSttResponse.kt
package com.example.meaningosapp.ui.main.face.audio

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * GcpSttResponse
 *
 * Represents the three types of responses emitted by the
 * streaming STT pipeline:
 *
 *  - PartialResult: interim transcript (not final)
 *  - FinalResult: final transcript for the utterance
 *  - Error: transport or decoding error
 *
 * This matches exactly what BabyCloudSttEngine expects.
 */
sealed class GcpSttResponse {

    /**
     * Partial transcript (interim result).
     */
    @Serializable
    @SerialName("partial")
    data class PartialResult(
        val transcript: String
    ) : GcpSttResponse() {
        init {
            require(transcript.isNotEmpty()) { "Partial transcript cannot be empty" }
        }
    }

    /**
     * Final transcript (end of utterance).
     */
    @Serializable
    @SerialName("final")
    data class FinalResult(
        val transcript: String
    ) : GcpSttResponse() {
        init {
            require(transcript.isNotEmpty()) { "Final transcript cannot be empty" }
        }
    }

    /**
     * Error message.
     */
    @Serializable
    @SerialName("error")
    data class Error(
        val message: String
    ) : GcpSttResponse() {
        init {
            require(message.isNotBlank()) { "Error message cannot be blank" }
        }
    }
}
