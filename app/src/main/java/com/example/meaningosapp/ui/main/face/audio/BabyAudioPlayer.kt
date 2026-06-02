package com.example.meaningosapp.ui.main.face.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack

/**
 * BabyAudioPlayer
 *
 * Baby Step 12:
 *  - Play PCM16 audio (from Google Cloud TTS)
 *  - Uses AudioTrack in STREAM mode
 *  - Supports 16 kHz mono PCM
 *
 * Next steps:
 *  - Integrate with GoogleCloudTtsClient
 *  - Integrate with BabyNodeViewModel
 */
class BabyAudioPlayer {

    private var audioTrack: AudioTrack? = null

    /**
     * Initialize AudioTrack for 16 kHz PCM16 mono.
     */
    private fun ensureInitialized() {
        if (audioTrack != null) return

        val sampleRate = 16000
        val channelConfig = AudioFormat.CHANNEL_OUT_MONO
        val encoding = AudioFormat.ENCODING_PCM_16BIT

        val minBuffer = AudioTrack.getMinBufferSize(sampleRate, channelConfig, encoding)

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(encoding)
                    .setSampleRate(sampleRate)
                    .setChannelMask(channelConfig)
                    .build()
            )
            .setBufferSizeInBytes(minBuffer)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack?.play()
    }

    /**
     * Play PCM16 audio bytes.
     */
    fun play(pcm: ByteArray) {
        ensureInitialized()
        audioTrack?.write(pcm, 0, pcm.size)
    }

    /**
     * Stop + release audio resources.
     */
    fun stop() {
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {
        }
        audioTrack = null
    }
}
