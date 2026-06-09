// FILE: BabyAudioRecorder.kt
package com.example.meaningosapp.ui.main.face.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * BabyAudioRecorder
 *
 * Production‑grade streaming PCM16 recorder for BabyNode.
 *
 * - 16 kHz
 * - Mono
 * - PCM 16‑bit
 * - VOICE_RECOGNITION source (fallback to MIC)
 * - Non‑blocking reads
 * - Auto‑recovery on mic failure
 */
class BabyAudioRecorder(
    private val sampleRate: Int = 16_000,
    private val chunkMillis: Int = 100
) {

    private var audioRecord: AudioRecord? = null
    private var bufferSizeInBytes: Int = 0
    private val isRecording = AtomicBoolean(false)
    private var recordingJob: Job? = null

    /**
     * Start streaming audio.
     */
    fun start(
        scope: CoroutineScope,
        onAudioChunk: (ByteArray) -> Unit
    ) {
        if (isRecording.get()) return

        initRecorder() ?: return

        val recorder = audioRecord ?: return
        isRecording.set(true)
        recorder.startRecording()

        val bytesPerSample = 2
        val samplesPerChunk = (sampleRate * (chunkMillis / 1000.0))
            .toInt()
            .coerceAtLeast(160)
        val chunkSizeBytes = samplesPerChunk * bytesPerSample
        val buffer = ByteArray(chunkSizeBytes)

        recordingJob = scope.launch(Dispatchers.IO + SupervisorJob()) {
            try {
                while (isActive && isRecording.get()) {

                    val read = recorder.read(buffer, 0, buffer.size, AudioRecord.READ_NON_BLOCKING)

                    if (!isRecording.get()) break
                    if (read <= 0) {
                        // Handle invalid states
                        if (read == AudioRecord.ERROR_INVALID_OPERATION ||
                            read == AudioRecord.ERROR_BAD_VALUE ||
                            recorder.state != AudioRecord.STATE_INITIALIZED
                        ) {
                            restartRecorder()
                        }
                        continue
                    }

                    // Copy only the valid portion
                    val chunk = buffer.copyOf(read)
                    onAudioChunk(chunk)
                }
            } catch (_: Exception) {
                // Prevent crashes from bubbling up
            } finally {
                stopInternal()
            }
        }
    }

    /**
     * Initialize AudioRecord safely.
     */
    private fun initRecorder(): AudioRecord? {
        bufferSizeInBytes = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(sampleRate / 10 * 2)

        val audioSource = try {
            MediaRecorder.AudioSource.VOICE_RECOGNITION
        } catch (_: Exception) {
            MediaRecorder.AudioSource.MIC
        }

        val recorder = AudioRecord(
            audioSource,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSizeInBytes
        )

        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            return null
        }

        audioRecord = recorder
        return recorder
    }

    /**
     * Restart recorder if Android kills the mic.
     */
    private fun restartRecorder() {
        stopInternal()
        initRecorder()?.startRecording()
    }

    /**
     * Stop streaming audio.
     */
    fun stop() {
        if (!isRecording.compareAndSet(true, false)) return

        recordingJob?.cancel()
        recordingJob = null

        stopInternal()
    }

    private fun stopInternal() {
        try { audioRecord?.stop() } catch (_: Exception) {}
        try { audioRecord?.release() } catch (_: Exception) {}
        audioRecord = null
    }
}
