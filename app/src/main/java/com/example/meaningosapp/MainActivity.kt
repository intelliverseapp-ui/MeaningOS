package com.example.meaningosapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import com.example.meaningosapp.ui.main.MainScreen
import com.example.meaningosapp.ui.theme.MeaningOSAppTheme
import java.util.Locale

class MainActivity : ComponentActivity() {

    // UI state (Compose-friendly)
    private val lastRecognizedText = mutableStateOf("")
    private val lastResponse = mutableStateOf("")
    private val isListeningState = mutableStateOf(false)

    // TTS
    private var tts: TextToSpeech? = null
    private var selectedVoice: Voice? = null

    // Permission launcher for microphone
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted) {
                Log.w("MeaningOS", "Microphone permission not granted")
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

            if (spokenText != null) {
                lastRecognizedText.value = spokenText

                // Pure meaning mapping (no side effects)
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
        enableEdgeToEdge()

        ensureMicPermission()

        // Initialize TTS
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                try {
                    tts?.language = Locale.US
                    val voices = tts?.voices
                    selectedVoice = voices?.firstOrNull { v ->
                        val name = v.name.lowercase()
                        (name.contains("male") || name.contains("man") || name.contains("deep") || name.contains("rich") || name.contains("expressive"))
                    } ?: tts?.defaultVoice

                    selectedVoice?.let { tts?.voice = it }
                    Log.d("MeaningOS", "Selected TTS voice: ${selectedVoice?.name ?: "default"}")
                } catch (e: Exception) {
                    selectedVoice = tts?.defaultVoice
                    Log.w("MeaningOS", "TTS voice selection failed: ${e.message}")
                }
            } else {
                Log.w("MeaningOS", "TTS initialization failed with status $status")
            }
        }

        setContent {
            MeaningOSAppTheme {
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

    // Start system speech recognizer
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

    // Speak helper using TTS
    private fun speak(text: String?) {
        if (text.isNullOrBlank()) return

        tts?.let { engine ->
            engine.setSpeechRate(1.0f)
            engine.setPitch(1.0f)
            engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "MeaningOSUtterance")
        }
    }

    // Centralized OS action dispatcher (all side effects here)
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
                        val playIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${action.packageName}"))
                        startActivity(playIntent)
                    }
                } catch (e: Exception) {
                    lastResponse.value = "Can't open that app on this device."
                    Log.w("MeaningOS", "LaunchApp failed: ${e.message}")
                }
            }

            is OSAction.OpenUrl -> {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(action.url))
                    startActivity(intent)
                } catch (e: Exception) {
                    lastResponse.value = "Failed to open the link."
                    Log.w("MeaningOS", "OpenUrl failed: ${e.message}")
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
                    Log.w("MeaningOS", "SetVolume failed: ${e.message}")
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
}
