// FILE: GoogleCloudSttTransport.kt
package com.example.meaningosapp.ui.main.face.audio

import android.content.Context
import com.google.api.gax.rpc.ClientStream
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.speech.v1.*
import com.google.protobuf.ByteString
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.io.InputStream
import java.util.concurrent.atomic.AtomicBoolean

object GoogleCloudSttTransport {

    private val running = AtomicBoolean(false)
    private var speechClient: SpeechClient? = null
    private var bidiStream: ClientStream<StreamingRecognizeRequest>? = null

    /**
     * Load and scope credentials from res/raw/gcp_stt_key.json
     */
    private fun loadCredentials(context: Context): GoogleCredentials {
        val inputStream: InputStream =
            context.resources.openRawResource(
                context.resources.getIdentifier(
                    "gcp_stt_key",
                    "raw",
                    context.packageName
                )
            )

        return GoogleCredentials.fromStream(inputStream)
            .createScoped(listOf("https://www.googleapis.com/auth/cloud-platform"))
    }

    /**
     * Ensure SpeechClient is created with scoped credentials.
     */
    private fun ensureClient(context: Context) {
        if (speechClient == null) {
            val creds = loadCredentials(context)

            val settings = SpeechSettings.newBuilder()
                .setCredentialsProvider { creds }
                .build()

            speechClient = SpeechClient.create(settings)
        }
    }

    /**
     * Start streaming STT session.
     */
    suspend fun startStreaming(
        context: Context,
        config: GcpSttRequest.Config,
        audioChannel: Channel<ByteArray>,
        onResponse: suspend (GcpSttResponse) -> Unit,
        onError: (Throwable) -> Unit
    ) = withContext(Dispatchers.IO) {

        if (!running.compareAndSet(false, true)) return@withContext

        ensureClient(context)
        val client = speechClient ?: return@withContext

        try {
            val callable = client.streamingRecognizeCallable()
            val stream = callable.call()
            bidiStream = stream

            val sampleRate = if (config.sampleRateHz > 0) config.sampleRateHz else 16_000

            val recognitionConfig = RecognitionConfig.newBuilder()
                .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                .setSampleRateHertz(sampleRate)
                .setLanguageCode(config.languageCode)
                .setEnableAutomaticPunctuation(true)
                .apply {
                    if (config.model.isNotBlank()) {
                        setModel(config.model)
                    }
                }
                .build()

            val streamingConfig = StreamingRecognitionConfig.newBuilder()
                .setConfig(recognitionConfig)
                .setInterimResults(true)
                .setSingleUtterance(false)
                .build()

            val initialRequest = StreamingRecognizeRequest.newBuilder()
                .setStreamingConfig(streamingConfig)
                .build()

            try {
                stream.send(initialRequest)
            } catch (e: Exception) {
                onError(e)
                running.set(false)
                return@withContext
            }

            // RECEIVE RESPONSES
            CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                try {
                    for (response in stream) {
                        val results = response.resultsList
                        if (results.isEmpty()) continue

                        val result = results.last()
                        val alt = result.alternativesList.firstOrNull() ?: continue
                        val transcript = alt.transcript

                        if (transcript.isBlank()) continue

                        if (!result.isFinal) {
                            onResponse(GcpSttResponse.PartialResult(transcript))
                        } else {
                            onResponse(GcpSttResponse.FinalResult(transcript))
                        }
                    }
                } catch (t: Throwable) {
                    onError(t)
                }
            }

            // SEND AUDIO CHUNKS
            for (pcm in audioChannel) {
                if (!running.get()) break

                try {
                    val audioRequest = StreamingRecognizeRequest.newBuilder()
                        .setAudioContent(ByteString.copyFrom(pcm))
                        .build()

                    stream.send(audioRequest)
                } catch (e: Exception) {
                    onError(e)
                    break
                }
            }

        } catch (e: Exception) {
            onError(e)
        }
    }

    /**
     * Stop streaming session.
     */
    fun stopStreaming() {
        if (!running.compareAndSet(true, false)) return

        try {
            bidiStream?.closeSend()
        } catch (_: Exception) {}

        bidiStream = null
    }
}
