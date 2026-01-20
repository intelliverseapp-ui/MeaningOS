package com.example.meaningosapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import com.example.meaningosapp.ui.main.MainScreen
import java.util.Locale

class MainActivity : ComponentActivity() {

    // Compose-friendly UI state
    private val lastRecognizedText = mutableStateOf("")
    private val lastResponse = mutableStateOf("")
    private val isListeningState = mutableStateOf(false)

    // TTS engine
    private var tts: TextToSpeech? = null

    // Permission launcher for RECORD_AUDIO
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted) {
                Log.w(TAG, "Microphone permission not granted")
            }
        }

    // Speech recognizer launcher
    private val speechLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            // Always clear listening state when we get a result (success or cancel)
            isListeningState.value = false

            val data = result.data
            val spokenText = data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()

            if (!spokenText.isNullOrBlank()) {
                lastRecognizedText.value = spokenText

                // Map spoken text to meaning/result using the placeholder IntentEngine
                val meaningResult = IntentEngine.handleMeaning(spokenText)

                // Update UI text
                lastResponse.value = meaningResult.text

                // If action requests immediate stop, stop TTS before speaking
                if (meaningResult.action is OSAction.StopSpeech) {
                    tts?.stop()
                }

                // Speak if requested
                if (meaningResult.speak) {
                    val toSpeak = meaningResult.speechText ?: meaningResult.text
                    speak(toSpeak)
                }

                // Dispatch OS action (side effects)
                handleOSAction(meaningResult.action)
            } else {
                // No recognized text (user cancelled or nothing recognized)
                lastResponse.value = "I didn't catch that."
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ensureMicPermission()

        // Initialize TTS
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                try {
                    tts?.language = Locale.US
                    tts?.voice = tts?.defaultVoice
                    Log.d(TAG, "TTS initialized")
                } catch (e: Exception) {
                    Log.w(TAG, "TTS initialization error: ${e.message}")
                }
            } else {
                Log.w(TAG, "TTS initialization failed with status $status")
            }
        }

        setContent {
            MaterialTheme {
                MainScreen(
                    youSaid = lastRecognizedText.value,
                    meaningText = lastResponse.value,
                    isListening = isListeningState.value,
                    onStartListening = { startListening() }
                )
            }
        }
    }

    private fun ensureMicPermission() {
        val permission = Manifest.permission.RECORD_AUDIO
        when {
            ContextCompat.checkSelfPermission(this, permission) ==
                    PackageManager.PERMISSION_GRANTED -> {
                // already granted
            }
            else -> {
                requestPermissionLauncher.launch(permission)
            }
        }
    }

    /**
     * Start the system speech recognizer.
     * Sets the listening state immediately so UI can react.
     */
    fun startListening() {
        // Set UI state immediately so the button pulses
        isListeningState.value = true

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Listening…")
        }
        speechLauncher.launch(intent)
    }

    private fun speak(text: String?) {
        if (text.isNullOrBlank()) return
        tts?.let { engine ->
            engine.setSpeechRate(1.0f)
            engine.setPitch(1.0f)
            engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "MeaningOSUtterance")
        }
    }

    private fun handleOSAction(action: OSAction) {
        when (action) {
            is OSAction.None -> { /* no-op */ }

            is OSAction.LaunchApp -> {
                try {
                    val launchIntent = packageManager.getLaunchIntentForPackage(action.packageName)
                    if (launchIntent != null) {
                        startActivity(launchIntent)
                    } else {
                        // Fallback: open Play Store listing
                        val playIntent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("market://details?id=${action.packageName}")
                        )
                        startActivity(playIntent)
                    }
                } catch (e: Exception) {
                    lastResponse.value = "Can't open that app on this device."
                    Log.w(TAG, "LaunchApp failed: ${e.message}")
                }
            }

            is OSAction.OpenUrl -> {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(action.url))
                    startActivity(intent)
                } catch (e: Exception) {
                    lastResponse.value = "Failed to open the link."
                    Log.w(TAG, "OpenUrl failed: ${e.message}")
                }
            }

            is OSAction.SetVolume -> {
                try {
                    val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
                    val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                    val newVol = (action.percent.coerceIn(0, 100) * max) / 100
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
                    lastResponse.value = "Volume set to ${action.percent}%."
                } catch (e: Exception) {
                    lastResponse.value = "Unable to set volume."
                    Log.w(TAG, "SetVolume failed: ${e.message}")
                }
            }

            is OSAction.StopSpeech -> {
                tts?.stop()
                lastResponse.value = "Okay, I will be quiet."
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        tts?.stop()
        tts?.shutdown()
        tts = null
    }

    companion object {
        private const val TAG = "MeaningOS"
    }
}
