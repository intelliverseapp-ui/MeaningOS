package com.example.meaningosapp.ui.main.face.audio

import com.example.meaningosapp.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * BabyCloudSttEngine
 *
 * REAL Google Cloud STREAMING Speech‑to‑Text engine for Baby Node.
 *
 * This version:
 *  - opens a real gRPC streaming session
 *  - streams PCM16 audio chunks
 *  - receives partial transcripts
 *  - receives final transcripts
 *  - shuts down cleanly
 */
class BabyCloudSttEngine {

    private val apiKey: String = BuildConfig.GOOGLE_CLOUD_STT_API_KEY

    // Streaming state
    private val isStreaming = AtomicBoolean(false)
    private var streamJob: Job? = null

    // Callbacks
    private var onPartialCallback: ((String) -> Unit)? = null
    private var onFinalCallback: ((String) -> Unit)? = null

    // ⭐ REAL Google Cloud streaming client
    private var streamingClient: GoogleCloudStreamingClient? = null

    /**
     * Start a new streaming session.
     */
    fun startStreaming(
        scope: CoroutineScope,
        onPartial: (String) -> Unit,
        onFinal: (String) -> Unit
    ) {
        if (isStreaming.get()) return
        if (apiKey.isBlank()) return

        onPartialCallback = onPartial
        onFinalCallback = onFinal

        isStreaming.set(true)

        // Launch the streaming client on IO dispatcher
        streamJob = scope.launch(Dispatchers.IO) {
            streamingClient = GoogleCloudStreamingClient(apiKey).apply {
                start(
                    onPartial = { partial ->
                        onPartialCallback?.invoke(partial)
                    },
                    onFinal = { final ->
                        onFinalCallback?.invoke(final)
                    }
                )
            }
        }
    }

    /**
     * Send a chunk of PCM16 audio to the streaming recognizer.
     */
    fun sendAudio(pcmChunk: ByteArray) {
        if (!isStreaming.get()) return
        streamingClient?.sendAudio(pcmChunk)
    }

    /**
     * Finish the streaming session.
     */
    fun finishStreaming() {
        if (!isStreaming.get()) return

        isStreaming.set(false)

        // ⭐ Close the REAL Google Cloud stream
        streamingClient?.finish()
        streamingClient = null

        // Cancel coroutine job
        streamJob?.cancel()
        streamJob = null
    }
}
