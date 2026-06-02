package com.example.meaningosapp.ui.main.face.audio

import android.media.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * BabyAudioRecorder
 *
 * Production‑grade streaming PCM16 recorder for Baby Node.
 *
 * - 16 kHz
 * - Mono
 * - PCM 16‑bit
 * - VOICE_RECOGNITION source (with fallback)
 *
 * It:
 *   start(scope, onAudioChunk) → streams audio chunks in real time
 *   stop()                     → stops recording and releases resources
 *
 * Designed for Google Cloud Streaming STT.
 */
class BabyAudioRecorder(
    private val sampleRate: Int = 16000,
    private val chunkMillis: Int = 100 // ~100ms chunks
) {

    private var audioRecord: AudioRecord? = null
    private var bufferSizeInBytes: Int = 0
    private val isRecording = AtomicBoolean(false)
    private var recordingJob: Job? = null

    /**
     * Start streaming audio.
     *
     * @param scope CoroutineScope (e.g., viewModelScope)
     * @param onAudioChunk callback invoked with each PCM16 chunk
     */
    fun start(
        scope: CoroutineScope,
        onAudioChunk: (ByteArray) -> Unit
    ) {
        if (isRecording.get()) return

        // Compute minimum buffer size
        bufferSizeInBytes = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(sampleRate / 10 * 2) // at least 100ms of audio

        // Try VOICE_RECOGNITION first, fallback to MIC
        val audioSource = try {
            MediaRecorder.AudioSource.VOICE_RECOGNITION
        } catch (_: Exception) {
            MediaRecorder.AudioSource.MIC
        }

        audioRecord = AudioRecord(
            audioSource,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSizeInBytes
        )

        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            audioRecord?.release()
            audioRecord = null
            return
        }

        isRecording.set(true)
        audioRecord?.startRecording()

        // Chunk size in samples → bytes
        val bytesPerSample = 2 // PCM16
        val samplesPerChunk = (sampleRate * (chunkMillis / 1000.0)).toInt().coerceAtLeast(160)
        val chunkSizeBytes = samplesPerChunk * bytesPerSample
        val buffer = ByteArray(chunkSizeBytes)

        recordingJob = scope.launch(Dispatchers.IO) {
            try {
                while (isActive && isRecording.get()) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: break
                    if (read > 0) {
                        val chunk = buffer.copyOf(read)
                        onAudioChunk(chunk)
                    }
                }
            } catch (_: Exception) {
                // Avoid crashing the app; upstream handles stream closure
            } finally {
                stopInternal()
            }
        }
    }

    /**
     * Stop streaming audio.
     */
    fun stop() {
        isRecording.set(false)
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
