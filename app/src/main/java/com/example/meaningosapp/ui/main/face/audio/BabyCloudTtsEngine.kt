// FILE: BabyCloudTtsEngine.kt
package com.example.meaningosapp.ui.main.face.audio

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import java.util.concurrent.atomic.AtomicBoolean

/**
 * BabyCloudTtsEngine
 *
 * High‑level Baby Node TTS engine.
 * - Maps BabyEmotion → prosody (speakingRate, pitch)
 * - Calls GoogleCloudTtsClient (REST)
 * - Emits PCM16 audio chunks through audioOut Flow
 * - Fully cancellation‑safe
 */
class BabyCloudTtsEngine(
    private val scope: CoroutineScope,
    private val gcpTtsClient: GoogleCloudTtsClient,
    private val sampleRateHz: Int,
    private val languageCode: String,
    private val voiceName: String
) {

    private val running = AtomicBoolean(false)
    private var synthJob: Job? = null

    // Bounded channel prevents memory blow‑up if playback lags
    private val audioChannel = Channel<ByteArray>(capacity = 32)
    val audioOut: Flow<ByteArray> = audioChannel.receiveAsFlow()

    /**
     * Main entry point:
     * BabyVoiceInterface calls this with text + emotion.
     */
    suspend fun synthesize(
        text: String,
        emotion: BabyEmotion
    ) = withContext(Dispatchers.IO) {

        // Prevent overlapping synth calls
        if (!running.compareAndSet(false, true)) return@withContext

        // Cancel any previous job (shouldn't happen, but safe)
        synthJob?.cancel()

        val (speakingRate, pitch) = mapEmotionToProsody(emotion)

        synthJob = scope.launch(Dispatchers.IO + SupervisorJob()) {
            try {
                gcpTtsClient.synthesize(
                    text = text,
                    emotion = emotion,
                    speakingRate = speakingRate,
                    pitch = pitch,
                    languageCode = languageCode,
                    voiceName = voiceName,
                    sampleRateHz = sampleRateHz,
                    onAudioChunk = { pcm ->
                        audioChannel.trySend(pcm)
                    },
                    onError = { e ->
                        audioChannel.trySend(byteArrayOf()) // structured error signal
                        throw e
                    }
                )
            } catch (_: Exception) {
                // Error already signaled
            } finally {
                running.set(false)
            }
        }

        synthJob?.join()
    }

    /**
     * Stop any ongoing synthesis.
     */
    fun stop() {
        running.set(false)
        synthJob?.cancel()
        gcpTtsClient.stop()
    }

    /**
     * Emotion → speakingRate + pitch
     *
     * These values are intentionally subtle.
     * Baby Node should sound expressive, not cartoonish.
     */
    private fun mapEmotionToProsody(emotion: BabyEmotion): Pair<Double, Double> {
        return when (emotion) {

            BabyEmotion.HAPPY ->
                1.10 to 2.0   // slightly faster, brighter

            BabyEmotion.SAD ->
                0.90 to -3.0  // slower, lower pitch

            BabyEmotion.ANGRY ->
                1.15 to 4.0   // sharper, more forceful

            BabyEmotion.CALM ->
                0.95 to -1.0  // gentle, slightly lower

            BabyEmotion.EXCITED ->
                1.20 to 3.5   // energetic but not cartoonish

            BabyEmotion.NEUTRAL ->
                1.0 to 0.0    // baseline

            else ->
                1.0 to 0.0
        }
    }
}
