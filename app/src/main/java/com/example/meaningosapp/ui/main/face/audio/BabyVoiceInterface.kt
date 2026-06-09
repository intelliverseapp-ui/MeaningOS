// FILE: BabyVoiceInterface.kt
package com.example.meaningosapp.ui.main.face.audio

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.abs

/**
 * BabyVoiceInterface
 *
 * The full Baby Node presence loop:
 *  - STT streaming (Google Cloud)
 *  - Recorder audio capture
 *  - Waveform sampling
 *  - Meaning engine call
 *  - Emotion + intensity
 *  - TTS reply (Google Cloud)
 *  - Audio playback (BabyAudioPlayer)
 *  - Mode transitions
 *
 * This class is the heart of Baby Node.
 */
class BabyVoiceInterface(
    private val scope: CoroutineScope,
    private val meaningEngine: suspend (String) -> Pair<String, BabyEmotion>,
    private val sttEngine: BabyCloudSttEngine,
    private val ttsClient: GoogleCloudTtsClient
) {

    // ---------------------------------------------------------
    // Engines
    // ---------------------------------------------------------

    private val tts = BabyCloudTtsEngine(
        scope = scope,
        gcpTtsClient = ttsClient,
        sampleRateHz = 16_000,
        languageCode = "en-US",
        voiceName = "en-US-Neural2-C"
    )

    private val recorder = BabyAudioRecorder()
    private val player = BabyAudioPlayer()

    // ---------------------------------------------------------
    // StateFlows
    // ---------------------------------------------------------

    private val _mode = MutableStateFlow(BabyNodeMode.IDLE)
    val mode: StateFlow<BabyNodeMode> = _mode

    private val _emotion = MutableStateFlow(BabyEmotion.NEUTRAL)
    val emotion: StateFlow<BabyEmotion> = _emotion

    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText

    private val _finalText = MutableStateFlow("")
    val finalText: StateFlow<String> = _finalText

    private val _reply = MutableStateFlow("")
    val reply: StateFlow<String> = _reply

    private val _waveform = MutableStateFlow<List<Int>>(emptyList())
    val waveform: StateFlow<List<Int>> = _waveform

    // ---------------------------------------------------------
    // Internal state
    // ---------------------------------------------------------

    private var intensity: Double = 0.7
    private var listening = false
    private var sttJob: Job? = null
    private var sttFinalJob: Job? = null

    fun setIntensity(value: Double) {
        intensity = value.coerceIn(0.0, 1.0)
    }

    // ---------------------------------------------------------
    // Init: subscribe to TTS audio output ONCE
    // ---------------------------------------------------------

    init {
        scope.launch(SupervisorJob()) {
            tts.audioOut.collect { pcm ->
                player.play(pcm)
            }
        }
    }

    // ---------------------------------------------------------
    // Presence Loop
    // ---------------------------------------------------------

    fun startListening() {
        if (listening) return
        listening = true

        _mode.value = BabyNodeMode.LISTENING
        _emotion.value = BabyEmotion.NEUTRAL
        _waveform.value = emptyList()
        _partialText.value = ""
        _finalText.value = ""
        _reply.value = ""

        // Cancel old collectors if they exist
        sttJob?.cancel()
        sttFinalJob?.cancel()

        // Subscribe to STT partial text
        sttJob = scope.launch(SupervisorJob()) {
            sttEngine.partialText.collect { partial ->
                _partialText.value = partial
            }
        }

        // Subscribe to STT final text
        sttFinalJob = scope.launch(SupervisorJob()) {
            sttEngine.finalText.collect { final ->
                _finalText.value = final
                processFinalTranscript(final)
            }
        }

        // Start STT streaming
        sttEngine.startStreaming()

        // Start recorder
        recorder.start(
            scope = scope,
            onAudioChunk = { pcm ->
                addWaveformSample(pcm)
                sttEngine.sendAudioChunk(pcm)
            }
        )
    }

    fun stopListening() {
        if (!listening) return
        listening = false

        _mode.value = BabyNodeMode.THINKING

        recorder.stop()
        sttEngine.stopStreaming()

        sttJob?.cancel()
        sttFinalJob?.cancel()
    }

    // ---------------------------------------------------------
    // Waveform sampling
    // ---------------------------------------------------------

    private fun addWaveformSample(pcm: ByteArray) {
        if (pcm.size >= 2) {
            val sample = pcm[0].toInt() or (pcm[1].toInt() shl 8)
            val absSample = abs(sample)

            // Keep waveform bounded (avoid unbounded list growth)
            val current = _waveform.value
            val updated =
                if (current.size > 200) current.drop(current.size - 200) + absSample
                else current + absSample

            _waveform.value = updated
        }
    }

    // ---------------------------------------------------------
    // FULL LOOP:
    // STT Final → Meaning Engine → Emotion → TTS → Playback → Restart Listening
    // ---------------------------------------------------------

    private fun processFinalTranscript(text: String) {
        scope.launch(Dispatchers.IO + SupervisorJob()) {
            try {
                // Stop listening while thinking
                stopListening()

                // Meaning Engine
                val (replyText, replyEmotion) = meaningEngine(text)

                _reply.value = replyText
                _emotion.value = replyEmotion
                _mode.value = BabyNodeMode.SPEAKING

                // TTS
                tts.synthesize(
                    text = replyText,
                    emotion = replyEmotion
                )

                // After speaking, restart listening
                _mode.value = BabyNodeMode.IDLE
                startListening()

            } catch (e: Exception) {
                _reply.value = "Error: ${e.message}"
                _emotion.value = BabyEmotion.SAD
                _mode.value = BabyNodeMode.ERROR
            }
        }
    }

    // ---------------------------------------------------------
    // Shutdown
    // ---------------------------------------------------------

    fun shutdown() {
        listening = false

        recorder.stop()
        sttEngine.stopStreaming()
        tts.stop()
        player.stop()

        sttJob?.cancel()
        sttFinalJob?.cancel()

        _mode.value = BabyNodeMode.IDLE
    }
}
