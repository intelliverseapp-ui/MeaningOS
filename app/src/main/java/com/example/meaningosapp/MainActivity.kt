package com.example.meaningosapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.meaningosapp.ui.theme.MeaningOSAppTheme
import java.util.Locale

class MainActivity : ComponentActivity() {

    // Permission launcher
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            // optional: handle UI feedback if needed
        }

    // TextToSpeech fields
    private var tts: TextToSpeech? = null
    private var selectedVoice: Voice? = null

    // UI state
    private val lastRecognizedText = mutableStateOf("")
    private val lastResponse = mutableStateOf("")

    // Speech recognizer launcher (consumes MeaningResult)
    private val speechLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data = result.data
            val spokenText = data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()

            if (spokenText != null) {
                // Update UI with what was recognized
                lastRecognizedText.value = spokenText

                // IntentEngine now returns MeaningResult
                val meaningResult = IntentEngine.handleMeaning(spokenText)

                // Display the text portion on screen
                lastResponse.value = meaningResult.text

                // Speak only when the meaning requests it
                if (meaningResult.speak) {
                    val toSpeak = meaningResult.speechText ?: meaningResult.text
                    speak(toSpeak)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        ensureMicPermission()

        // Initialize TextToSpeech before UI so it's ready when user triggers speak
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // Baseline locale
                tts?.language = Locale.US

                // Heuristic: prefer voices whose name suggests male/expressive/deep/rich
                try {
                    val voices = tts?.voices
                    selectedVoice = voices?.firstOrNull { v ->
                        val name = v.name.lowercase()
                        (name.contains("male") || name.contains("expressive") || name.contains("deep") || name.contains("rich"))
                    } ?: tts?.defaultVoice

                    selectedVoice?.let { tts?.voice = it }

                    // Debug log (optional)
                    Log.d("MeaningOS", "Selected TTS voice: ${selectedVoice?.name ?: "default"}")
                } catch (e: Exception) {
                    selectedVoice = tts?.defaultVoice
                }
            } else {
                Log.w("MeaningOS", "TTS initialization failed with status $status")
            }
        }

        setContent {
            MeaningOSAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->

                    Column(
                        modifier = Modifier
                            .padding(innerPadding)
                            .padding(24.dp)
                    ) {

                        Button(onClick = { startListening() }) {
                            Text("Start Listening")
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text("You said: ${lastRecognizedText.value}")

                        Spacer(modifier = Modifier.height(16.dp))

                        Text("MeaningOS: ${lastResponse.value}")
                    }
                }
            }
        }
    }
    // Ensure microphone permission
    private fun ensureMicPermission() {
        val permission = Manifest.permission.RECORD_AUDIO
        when {
            ContextCompat.checkSelfPermission(this, permission) ==
                    PackageManager.PERMISSION_GRANTED -> {
                // Already granted
            }
            else -> {
                requestPermissionLauncher.launch(permission)
            }
        }
    }

    // Start system speech recognizer
    private fun startListening() {
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
            // Configure for a warm, expressive default
            engine.setSpeechRate(1.0f)   // normal speed; tweak to taste
            engine.setPitch(1.0f)        // normal pitch; lower for deeper warmth
            engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "MeaningOSUtterance")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
