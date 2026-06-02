package com.example.meaningosapp.ui.main.face.audio

import com.google.cloud.texttospeech.v1.AudioConfig
import com.google.cloud.texttospeech.v1.AudioEncoding
import com.google.cloud.texttospeech.v1.SsmlVoiceGender
import com.google.cloud.texttospeech.v1.SynthesisInput
import com.google.cloud.texttospeech.v1.TextToSpeechGrpc
import com.google.cloud.texttospeech.v1.VoiceSelectionParams
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder

/**
 * GoogleCloudTtsClient
 *
 * Baby Step 17:
 *  - Add SSML support
 *  - Allow expressive prosody (pitch, speed, pauses)
 *  - Accept either plain text or SSML
 */
class GoogleCloudTtsClient(
    private val apiKey: String
) {

    private var channel: ManagedChannel? = null
    private var stub: TextToSpeechGrpc.TextToSpeechBlockingStub? = null

    init {
        channel = ManagedChannelBuilder
            .forTarget("texttospeech.googleapis.com")
            .build()

        stub = TextToSpeechGrpc.newBlockingStub(channel)
    }

    /**
     * Synthesize text or SSML → PCM16 audio bytes.
     *
     * If the input contains <speak> tags, we treat it as SSML.
     */
    fun synthesize(
        text: String,
        pitch: Double = 0.0,
        speed: Double = 1.0
    ): ByteArray {

        val isSsml = text.trim().startsWith("<speak>", ignoreCase = true)

        val input = if (isSsml) {
            SynthesisInput.newBuilder().setSsml(text).build()
        } else {
            SynthesisInput.newBuilder().setText(text).build()
        }

        val voice = VoiceSelectionParams.newBuilder()
            .setLanguageCode("en-US")
            .setSsmlVoiceGender(SsmlVoiceGender.NEUTRAL)
            .build()

        val audioConfig = AudioConfig.newBuilder()
            .setAudioEncoding(AudioEncoding.LINEAR16)
            .setSpeakingRate(speed)
            .setPitch(pitch)
            .build()

        val request = com.google.cloud.texttospeech.v1.SynthesizeSpeechRequest.newBuilder()
            .setInput(input)
            .setVoice(voice)
            .setAudioConfig(audioConfig)
            .build()

        val response = stub?.synthesizeSpeech(request)

        return response?.audioContent?.toByteArray() ?: ByteArray(0)
    }

    fun shutdown() {
        channel?.shutdown()
        channel = null
        stub = null
    }
}
