// FILE: BabyAudioPlayer.kt
package com.example.meaningosapp.ui.main.face.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack

/**
 * BabyAudioPlayer
 *
 * Plays PCM16 audio (from Google Cloud TTS)
 * - 16 kHz
 * - Mono
 * - PCM 16‑bit
 * - Low‑latency streaming mode
 * - Glitch‑proof, non‑blocking writes
 */
class BabyAudioPlayer {

    private var audioTrack: AudioTrack? = null

    /**
     * Ensure AudioTrack is created and ready.
     */
    private fun ensureInitialized() {
        val existing = audioTrack
        if (existing != null && existing.state == AudioTrack.STATE_INITIALIZED) return

        val sampleRate = 16_000
        val channelConfig = AudioFormat.CHANNEL_OUT_MONO
        val encoding = AudioFormat.ENCODING_PCM_16BIT

        val minBuffer = AudioTrack.getMinBufferSize(sampleRate, channelConfig, encoding)

        // Larger buffer = fewer underruns, smoother streaming
        val bufferSize = minBuffer * 4

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
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack?.play()
    }

    /**
     * Play PCM16 audio bytes.
     */
    fun play(pcm: ByteArray) {
        if (pcm.isEmpty()) return  // error signal from TTS engine

        ensureInitialized()

        val track = audioTrack ?: return

        // Non‑blocking write: prevents freezes
        val result = track.write(pcm, 0, pcm.size, AudioTrack.WRITE_NON_BLOCKING)

        if (result < 0) {
            // AudioTrack died or is in invalid state → recreate
            reset()
        }
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

    /**
     * Optional: recreate the AudioTrack cleanly.
     */
    fun reset() {
        stop()
        ensureInitialized()
    }
}
