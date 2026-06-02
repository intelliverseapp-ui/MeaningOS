package com.example.meaningosapp.ui.main.face.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

// Audio
import com.example.meaningosapp.ui.main.face.audio.BabyAudioRecorder
import com.example.meaningosapp.ui.main.face.audio.BabyCloudSttEngine
import com.example.meaningosapp.ui.main.face.audio.BabyCloudTtsEngine

// UI state
import com.example.meaningosapp.ui.main.face.ui.BabyNodeMode
import com.example.meaningosapp.ui.main.face.ui.BabyEmotion

// Emotion + STG pipeline
import com.example.meaningosapp.ui.main.face.emotion.EmotionSignals
import com.example.meaningosapp.ui.main.face.emotion.SemanticEmojiEngine
import com.example.meaningosapp.ui.main.face.emotion.SentimentAnalyzer

// API
import com.example.meaningosapp.ui.main.face.api.BabyNodeApi

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs

class BabyNodeViewModel(
    private val context: Context
) : ViewModel() {

    // ⭐ Streaming audio recorder
    private val recorder = BabyAudioRecorder()

    // ⭐ REAL Google Cloud streaming STT engine
    private val cloudStt = BabyCloudSttEngine()

    // ⭐ REAL Google Cloud TTS engine (with SSML + emotion + intensity)
    private val cloudTts = BabyCloudTtsEngine(
        apiKey = com.example.meaningosapp.BuildConfig.GOOGLE_CLOUD_TTS_API_KEY
    )

    // ---------------------------------------------------------
    // MODE + EMOTION STATE
    // ---------------------------------------------------------

    private val _mode = MutableStateFlow(BabyNodeMode.IDLE)
    val mode: StateFlow<BabyNodeMode> = _mode

    private val _emotion = MutableStateFlow(BabyEmotion.NEUTRAL)
    val emotion: StateFlow<BabyEmotion> = _emotion

    // ---------------------------------------------------------
    // Legacy booleans (UI rings)
    // ---------------------------------------------------------

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening

    private val _isThinking = MutableStateFlow(false)
    val isThinking: StateFlow<Boolean> = _isThinking

    // ---------------------------------------------------------
    // Emoji overlay
    // ---------------------------------------------------------

    private val _currentEmoji = MutableStateFlow<String?>(null)
    val currentEmoji: StateFlow<String?> = _currentEmoji

    // ---------------------------------------------------------
    // Debug + Graph
    // ---------------------------------------------------------

    private val _debug = MutableStateFlow("")
    val debug: StateFlow<String> = _debug

    private var graph: Any? = null

    // ---------------------------------------------------------
    // Text + Reply
    // ---------------------------------------------------------

    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText

    private val _finalText = MutableStateFlow("")
    val finalText: StateFlow<String> = _finalText

    private val _nodeReply = MutableStateFlow("")
    val nodeReply: StateFlow<String> = _nodeReply

    // ---------------------------------------------------------
    // Waveform samples
    // ---------------------------------------------------------

    private val _waveform = MutableStateFlow<List<Int>>(emptyList())
    val waveform: StateFlow<List<Int>> = _waveform

    fun addWaveformSample(pcm: ByteArray) {
        if (pcm.size >= 2) {
            val sample = pcm[0].toInt() or (pcm[1].toInt() shl 8)
            _waveform.value = _waveform.value + listOf(abs(sample))
        }
    }

    // ---------------------------------------------------------
    // Recording timer
    // ---------------------------------------------------------

    private val _recordingTime = MutableStateFlow(0)
    val recordingTime: StateFlow<Int> = _recordingTime

    private fun startTimer() {
        viewModelScope.launch {
            _recordingTime.value = 0
            while (_isListening.value) {
                delay(1000)
                _recordingTime.value += 1
            }
        }
    }

    // ---------------------------------------------------------
    // MODE HELPERS
    // ---------------------------------------------------------

    private fun setMode(newMode: BabyNodeMode) {
        _mode.value = newMode
    }

    private fun setEmotion(newEmotion: BabyEmotion) {
        _emotion.value = newEmotion
    }

    // ---------------------------------------------------------
    // STREAMING LISTENING CONTROL
    // ---------------------------------------------------------

    fun onStartListening() {
        _isListening.value = true
        _isThinking.value = false
        _currentEmoji.value = null
        _waveform.value = emptyList()

        setMode(BabyNodeMode.LISTENING)
        setEmotion(BabyEmotion.NEUTRAL)

        // ⭐ Start REAL Google Cloud streaming
        cloudStt.startStreaming(
            scope = viewModelScope,
            onPartial = { partial ->
                _partialText.value = partial
            },
            onFinal = { final ->
                _finalText.value = final
                onTranscriptFinal(final)
            }
        )

        // ⭐ Start streaming audio
        recorder.start(
            scope = viewModelScope,
            onAudioChunk = { pcm ->
                addWaveformSample(pcm)
                cloudStt.sendAudio(pcm)
            }
        )

        startTimer()
    }

    fun onStopListening() {
        _isListening.value = false
        _isThinking.value = true
        setMode(BabyNodeMode.THINKING)

        recorder.stop()
        cloudStt.finishStreaming()
    }

    // ---------------------------------------------------------
    // FINAL TRANSCRIPT → BABY NODE API → EMOTION + INTENSITY → TTS
    // ---------------------------------------------------------

    private fun onTranscriptFinal(text: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val reply = BabyNodeApi.sendTextAndGetResponse(text)

                _isThinking.value = false
                _nodeReply.value = reply
                setMode(BabyNodeMode.SPEAKING)

                // ⭐ Emotion inference
                val (sentimentScore, intensityRaw) =
                    SentimentAnalyzer.analyze("$text $reply")

                // clamp intensity to [0.0, 1.0]
                val intensity = intensityRaw.coerceIn(0.0, 1.0)

                val signals = EmotionSignals(
                    userText = text,
                    nodeReply = reply,
                    isThinking = false,
                    isListening = false,
                    sentimentScore = sentimentScore,
                    intensity = intensity
                )

                val emoji = SemanticEmojiEngine.pickEmoji(signals)
                _currentEmoji.value = emoji

                // ⭐ Updated emotion mapping
                val detectedEmotion =
                    when (emoji) {
                        "😊" -> BabyEmotion.HAPPY
                        "😢" -> BabyEmotion.SAD
                        "😮", "🤯", "🤩" -> BabyEmotion.SURPRISED
                        "😡", "🤬" -> BabyEmotion.ANGRY
                        "😱", "😨" -> BabyEmotion.SCARED
                        "❤️", "🥰" -> BabyEmotion.LOVE
                        "🤔" -> BabyEmotion.THINKING
                        "🧐" -> BabyEmotion.CURIOUS
                        else -> BabyEmotion.NEUTRAL
                    }

                setEmotion(detectedEmotion)

                // ⭐ NEW: Speak reply with emotion + intensity
                cloudTts.speak(
                    scope = viewModelScope,
                    text = reply,
                    emotion = detectedEmotion,
                    intensity = intensity
                )

                delay(1200)
                setMode(BabyNodeMode.IDLE)

            } catch (e: Exception) {
                _isThinking.value = false
                _nodeReply.value = "API error: ${e.message}"
                _currentEmoji.value = "⚠️"
                setMode(BabyNodeMode.ERROR)
                setEmotion(BabyEmotion.SAD)
            }
        }
    }

    // ---------------------------------------------------------
    // DEBUG + GRAPH
    // ---------------------------------------------------------

    fun setDebug(msg: String) {
        _debug.value = msg
    }

    fun setGraph(g: Any) {
        graph = g
    }
}
