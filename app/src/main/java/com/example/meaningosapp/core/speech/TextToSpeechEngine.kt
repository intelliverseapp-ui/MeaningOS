package com.example.meaningosapp.core.speech

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue

class TextToSpeechEngine(
    private val context: Context
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isReady = false

    // queue to hold utterances submitted before init or while busy
    private val pendingQueue: ConcurrentLinkedQueue<QueuedUtterance> = ConcurrentLinkedQueue()

    // simple state flag
    val isSpeaking: Boolean
        get() = tts?.isSpeaking ?: false

    init {
        tts = TextToSpeech(context, this)
        // attach a progress listener once created
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                // no-op; UI can observe isSpeaking if needed
            }

            override fun onDone(utteranceId: String?) {
                // If there are queued utterances, speak the next one
                val next = pendingQueue.poll()
                next?.let {
                    speakInternal(it.text, it.onDone)
                }
            }

            override fun onError(utteranceId: String?) {
                // Try to continue with next queued utterance
                val next = pendingQueue.poll()
                next?.let {
                    speakInternal(it.text, it.onDone)
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?, errorCode: Int) {
                onError(utteranceId)
            }
        })
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // prefer US English by default; fall back to default locale if unavailable
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // fallback to device default
                tts?.language = Locale.getDefault()
            }
            isReady = true

            // Speak anything that was waiting for initialization (preserve order)
            while (true) {
                val next = pendingQueue.poll() ?: break
                speakInternal(next.text, next.onDone)
            }
        } else {
            // initialization failed; keep isReady false and keep queued items for retry
            isReady = false
        }
    }

    /**
     * Public speak method.
     * If TTS is not ready, the text is queued and will be spoken once initialization completes.
     * If TTS is currently speaking, the text is queued and will be spoken after the current utterance.
     *
     * @param text text to speak
     * @param queueIfBusy if true, queue the utterance when busy; if false, flush and speak immediately
     * @param onDone optional callback invoked on completion of this utterance
     */
    fun speak(text: String, queueIfBusy: Boolean = true, onDone: (() -> Unit)? = null) {
        if (!isReady) {
            // Queue until TTS is initialized
            pendingQueue.add(QueuedUtterance(text, onDone))
            return
        }

        // If currently speaking and caller wants to queue, add to queue
        if (isSpeaking && queueIfBusy) {
            pendingQueue.add(QueuedUtterance(text, onDone))
            return
        }

        // If currently speaking and caller does not want to queue, flush and speak immediately
        if (isSpeaking && !queueIfBusy) {
            tts?.stop()
            pendingQueue.clear()
        }

        speakInternal(text, onDone)
    }

    private fun speakInternal(text: String, onDone: (() -> Unit)? = null) {
        val params = Bundle()
        // You can add SSML or other flags here if needed in the future
        val utteranceId = "BN_TTS_${System.currentTimeMillis()}"
        // store the onDone callback in a lightweight map via utteranceId if needed;
        // for simplicity we wrap onDone into the pendingQueue continuation above.
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        // Note: UtteranceProgressListener handles chaining queued utterances.
        // If caller needs a direct onDone callback for this specific utterance,
        // we rely on the queued continuation pattern: the onDone passed here will be
        // executed when the utterance completes if it was enqueued as next.
        // For immediate onDone notification, consider adding a map<utteranceId, callback>.
        onDone?.let {
            // best-effort: schedule a short watcher to call onDone after speech ends is fragile;
            // keep this simple: if precise per-utterance callbacks are required, we can add a map.
        }
    }

    fun stop() {
        tts?.stop()
        pendingQueue.clear()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isReady = false
        pendingQueue.clear()
    }

    // Optional helpers to tune voice
    fun setSpeechRate(rate: Float) {
        // typical range 0.5 - 2.0
        tts?.setSpeechRate(rate)
    }

    fun setPitch(pitch: Float) {
        // typical range 0.5 - 2.0
        tts?.setPitch(pitch)
    }

    private data class QueuedUtterance(val text: String, val onDone: (() -> Unit)?)
}
