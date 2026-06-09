// FILE: GoogleCloudSttClient.kt
package com.example.meaningosapp.ui.main.face.audio

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.util.concurrent.atomic.AtomicBoolean

/**
 * GoogleCloudSttClient
 *
 * High‑level wrapper around GoogleCloudSttTransport.
 * Manages:
 *  - Audio channel lifecycle
 *  - Running state
 *  - Forwarding responses
 *  - Clean shutdown
 *  - Transport death detection
 */
class GoogleCloudSttClient {

    private var audioChannel: Channel<ByteArray>? = null
    private val running = AtomicBoolean(false)
    private var transportJob: Job? = null

    /**
     * Start a new Google Cloud STT streaming session.
     */
    suspend fun startStreaming(
        context: Context,
        config: GcpSttRequest.Config,
        onResponse: suspend (GcpSttResponse) -> Unit,
        onError: (Throwable) -> Unit
    ) = withContext(Dispatchers.IO) {

        // Prevent double-start
        if (!running.compareAndSet(false, true)) return@withContext

        // Bounded channel prevents memory blow-up
        val channel = Channel<ByteArray>(capacity = 32)
        audioChannel = channel

        // Cancel any previous transport job
        transportJob?.cancel()

        transportJob = CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                GoogleCloudSttTransport.startStreaming(
                    context = context,
                    config = config,
                    audioChannel = channel,
                    onResponse = { response ->
                        onResponse(response)
                    },
                    onError = { throwable ->
                        onError(throwable)
                        stopStreaming()
                    }
                )
            } catch (e: Exception) {
                onError(e)
                stopStreaming()
            }
        }
    }

    /**
     * Send a PCM16 audio chunk to the streaming recognizer.
     */
    fun sendAudioChunk(pcm: ByteArray) {
        if (!running.get()) return
        if (pcm.isEmpty()) return

        try {
            audioChannel?.trySend(pcm)
        } catch (_: Exception) {
            // Channel closed or transport dead
            stopStreaming()
        }
    }

    /**
     * Finish the streaming session.
     */
    fun finishStreaming() {
        stopStreaming()
    }

    /**
     * Stop streaming and clean up resources.
     */
    fun stopStreaming() {
        if (!running.compareAndSet(true, false)) return

        try {
            audioChannel?.close()
        } catch (_: Exception) {}

        audioChannel = null

        try {
            transportJob?.cancel()
        } catch (_: Exception) {}

        transportJob = null

        try {
            GoogleCloudSttTransport.stopStreaming()
        } catch (_: Exception) {}
    }
}
