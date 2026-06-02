package com.example.meaningosapp.ui.main.face.audio

// ⭐ Google Cloud Speech gRPC imports
import com.google.cloud.speech.v1.RecognitionConfig
import com.google.cloud.speech.v1.RecognitionConfig.AudioEncoding
import com.google.cloud.speech.v1.StreamingRecognitionConfig
import com.google.cloud.speech.v1.StreamingRecognizeRequest
import com.google.cloud.speech.v1.StreamingRecognizeResponse
import com.google.cloud.speech.v1.SpeechGrpc

// ⭐ gRPC channel imports
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.stub.StreamObserver

// ⭐ ByteString for audio encoding
import com.google.protobuf.ByteString

/**
 * GoogleCloudStreamingClient
 *
 * Baby Step #8 adds:
 *  - REAL stream closing
 *  - REAL channel shutdown
 *  - REAL cleanup
 */
class GoogleCloudStreamingClient(
    private val apiKey: String
) {

    // ⭐ Real gRPC objects
    private var channel: ManagedChannel? = null
    private var stub: SpeechGrpc.SpeechStub? = null

    // ⭐ Configs from Baby Step #5
    private lateinit var recognitionConfig: RecognitionConfig
    private lateinit var streamingConfig: StreamingRecognitionConfig

    // ⭐ The request stream (we send audio into this)
    private var requestObserver: StreamObserver<StreamingRecognizeRequest>? = null

    fun start(
        onPartial: (String) -> Unit,
        onFinal: (String) -> Unit
    ) {
        // ⭐ BABY STEP #4: Create the secure gRPC channel
        channel = ManagedChannelBuilder
            .forTarget("speech.googleapis.com")
            .build()

        // ⭐ Create the async stub (non-blocking)
        stub = SpeechGrpc.newStub(channel)

        // ⭐ BABY STEP #5: Build the REAL recognition config
        recognitionConfig = RecognitionConfig.newBuilder()
            .setEncoding(AudioEncoding.LINEAR16)   // PCM16
            .setSampleRateHertz(16000)             // 16 kHz
            .setLanguageCode("en-US")              // English
            .setEnableAutomaticPunctuation(true)   // Add punctuation
            .build()

        // ⭐ Build the REAL streaming config
        streamingConfig = StreamingRecognitionConfig.newBuilder()
            .setConfig(recognitionConfig)
            .setInterimResults(true)               // Enable partial results
            .setSingleUtterance(false)             // Keep listening until we stop
            .build()

        // ⭐ BABY STEP #6: Open the REAL streaming call
        requestObserver = stub?.streamingRecognize(object : StreamObserver<StreamingRecognizeResponse> {

            override fun onNext(response: StreamingRecognizeResponse) {
                if (response.resultsCount > 0) {
                    val result = response.getResults(0)

                    if (result.alternativesCount > 0) {
                        val transcript = result.getAlternatives(0).transcript

                        if (result.isFinal) {
                            onFinal(transcript)
                        } else {
                            onPartial(transcript)
                        }
                    }
                }
            }

            override fun onError(t: Throwable) {
                onFinal("Error: ${t.message}")
            }

            override fun onCompleted() {
                // Stream closed normally
            }
        })

        // ⭐ Send the initial config message (REQUIRED)
        val configRequest = StreamingRecognizeRequest.newBuilder()
            .setStreamingConfig(streamingConfig)
            .build()

        requestObserver?.onNext(configRequest)
    }

    /**
     * ⭐ BABY STEP #7:
     * Send PCM16 audio frames into the stream.
     */
    fun sendAudio(pcm: ByteArray) {
        val observer = requestObserver ?: return

        val audioBytes = ByteString.copyFrom(pcm)

        val audioRequest = StreamingRecognizeRequest.newBuilder()
            .setAudioContent(audioBytes)
            .build()

        observer.onNext(audioRequest)
    }

    /**
     * ⭐ BABY STEP #8:
     * Finish the stream cleanly.
     */
    fun finish() {
        try {
            // Tell Google we are done sending audio
            requestObserver?.onCompleted()
        } catch (_: Exception) {
        }

        requestObserver = null

        try {
            // Shut down the gRPC channel
            channel?.shutdown()
        } catch (_: Exception) {
        }

        channel = null
        stub = null
    }
}
