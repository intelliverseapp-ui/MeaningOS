// FILE: BabyNodeViewModel.kt
package com.example.meaningosapp.ui.main.face.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.example.meaningosapp.ui.main.face.audio.BabyVoiceInterface
import com.example.meaningosapp.ui.main.face.audio.BabyNodeMode
import com.example.meaningosapp.ui.main.face.audio.GoogleCloudSttClient
import com.example.meaningosapp.ui.main.face.audio.GoogleCloudTtsClient
import com.example.meaningosapp.ui.main.face.audio.BabyCloudSttEngine

import com.example.meaningosapp.ui.main.face.audio.BabyEmotion

import com.example.meaningosapp.ui.main.face.api.BabyNodeApi

import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class BabyNodeViewModel(
    private val context: Context
) : ViewModel() {

    private val sttClient = GoogleCloudSttClient()

    private val sttEngine = BabyCloudSttEngine(
        appContext = context,
        scope = viewModelScope,
        gcpClient = sttClient,
        sampleRateHz = 16_000,
        languageCode = "en-US",
        modelId = ""
    )

    private val ttsClient = GoogleCloudTtsClient()

    private val voice = BabyVoiceInterface(
        scope = viewModelScope,
        meaningEngine = { userText ->
            val reply = BabyNodeApi.sendTextAndGetResponse(userText)
            reply to BabyEmotion.NEUTRAL
        },
        sttEngine = sttEngine,
        ttsClient = ttsClient
    )

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

    private val _currentEmoji = MutableStateFlow<String?>(null)
    val currentEmoji: StateFlow<String?> = _currentEmoji

    private val _debug = MutableStateFlow("")
    val debug: StateFlow<String> = _debug

    private var graph: Any? = null

    init {
        viewModelScope.launch(SupervisorJob()) { voice.mode.collect { _mode.value = it } }
        viewModelScope.launch(SupervisorJob()) { voice.emotion.collect { _emotion.value = it } }
        viewModelScope.launch(SupervisorJob()) { voice.partialText.collect { _partialText.value = it } }
        viewModelScope.launch(SupervisorJob()) { voice.finalText.collect { _finalText.value = it } }

        viewModelScope.launch(SupervisorJob()) {
            voice.reply.collect { replyText ->
                _reply.value = replyText
                updateEmoji(replyText)
            }
        }

        viewModelScope.launch(SupervisorJob()) { voice.waveform.collect { _waveform.value = it } }
    }

    private fun updateEmoji(reply: String) {
        _currentEmoji.value =
            when {
                reply.contains("love", true) -> "❤️"
                reply.contains("sorry", true) -> "😢"
                reply.contains("wow", true) -> "🤯"
                reply.contains("angry", true) -> "😡"
                reply.contains("scared", true) -> "😱"
                reply.contains("happy", true) -> "😊"
                else -> null
            }
    }

    fun startListening() {
        voice.startListening()
    }

    fun stopListening() {
        voice.stopListening()
    }

    fun setDebug(msg: String) {
        _debug.value = msg
    }

    fun setGraph(g: Any) {
        graph = g
    }

    override fun onCleared() {
        super.onCleared()
        sttEngine.stopStreaming()
        ttsClient.shutdown()
        voice.shutdown()
    }
}
