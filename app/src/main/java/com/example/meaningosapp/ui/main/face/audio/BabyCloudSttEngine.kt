// FILE: BabyCloudSttEngine.kt
package com.example.meaningosapp.ui.main.face.audio

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class BabyCloudSttEngine(
    private val appContext: Context,
    private val scope: CoroutineScope,
    private val gcpClient: GoogleCloudSttClient,
    private val sampleRateHz: Int = 16_000,
    private val languageCode: String = "en-US",
    private val modelId: String = ""
) {

    private val _partialText = MutableSharedFlow<String>(replay = 1)
    val partialText: SharedFlow<String> = _partialText

    private val _finalText = MutableSharedFlow<String>(replay = 1)
    val finalText: SharedFlow<String> = _finalText

    private var streamingJob: Job? = null
    private var isStreaming = false

    /**
     * Starts a new streaming recognition session.
     */
    fun startStreaming() {
        if (isStreaming) return
        isStreaming = true

        streamingJob?.cancel()
        streamingJob = scope.launch(Dispatchers.IO + SupervisorJob()) {

            val configRequest = GcpSttRequest.Config(
                encoding = "LINEAR16",
                sampleRateHz = sampleRateHz,
                languageCode = languageCode,
                model = modelId,
                enablePartialResults = true
            )

            try {
                gcpClient.startStreaming(
                    context = appContext,
                    config = configRequest,
                    onResponse = { response ->
                        scope.launch {
                            handleSttResponse(response)
                        }
                    },
                    onError = { throwable ->
                        throwable.printStackTrace()
                        stopStreaming()
                    }
                )
            } catch (e: Exception) {
                e.printStackTrace()
                isStreaming = false
            }
        }
    }

    /**
     * Sends raw PCM16 audio bytes to the active streaming session.
     */
    fun sendAudioChunk(pcmBytes: ByteArray) {
        if (!isStreaming || pcmBytes.isEmpty()) return

        scope.launch(Dispatchers.IO + SupervisorJob()) {
            try {
                gcpClient.sendAudioChunk(pcmBytes)
            } catch (e: Exception) {
                e.printStackTrace()
                stopStreaming()
            }
        }
    }

    /**
     * Stops the current streaming session.
     */
    fun stopStreaming() {
        if (!isStreaming) return
        isStreaming = false

        scope.launch(Dispatchers.IO + SupervisorJob()) {
            try {
                gcpClient.finishStreaming()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                streamingJob?.cancel()
                streamingJob = null
            }
        }
    }

    /**
     * Handles responses from Google Cloud STT.
     */
    private suspend fun handleSttResponse(response: GcpSttResponse) {
        withContext(Dispatchers.Default) {
            when (response) {
                is GcpSttResponse.PartialResult -> {
                    val text = response.transcript
                    if (text.isNotBlank()) _partialText.emit(text)
                }

                is GcpSttResponse.FinalResult -> {
                    val text = response.transcript
                    if (text.isNotBlank()) _finalText.emit(text)
                }

                is GcpSttResponse.Error -> {
                    println("STT error: ${response.message}")
                    stopStreaming()
                }
            }
        }
    }
}
