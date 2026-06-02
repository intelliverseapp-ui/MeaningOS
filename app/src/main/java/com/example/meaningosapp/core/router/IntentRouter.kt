package com.example.meaningosapp.core.router

import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import com.example.meaningosapp.core.MeaningResult
import com.example.meaningosapp.core.OSAction
import com.example.meaningosapp.core.tools.ToolRegistry
import com.example.meaningosapp.core.tools.ToolType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class IntentRouter(
    private val tools: ToolRegistry,
    private val speak: (String) -> Unit,
    private val audioManager: AudioManager,
    private val launchIntent: (Intent) -> Unit
) {

    private val scope = CoroutineScope(Dispatchers.IO)

    fun route(meaning: MeaningResult) {

        if (meaning.speak) {
            speak(meaning.text)
        }

        when (val action = meaning.action) {

            // ===== NO ACTION =====
            is OSAction.None -> return

            // ===== CONTROL =====
            is OSAction.StopSpeech -> return

            // ===== TOOLS =====
            is OSAction.UseTool -> {
                scope.launch {
                    val result = when (action.type) {
                        ToolType.WEB_LOOKUP -> tools.lookup(action.input)
                        ToolType.APP_LAUNCHER -> tools.launchApp(action.input)
                        else -> "I don’t know how to do that yet."
                    }
                    speak(result)
                }
                return
            }

            // ===== APP / URL =====
            is OSAction.LaunchApp -> {
                launchIntentForPackage(action.packageName)?.let { launchIntent(it) }
                return
            }

            is OSAction.OpenUrl -> {
                launchIntent(Intent(Intent.ACTION_VIEW, Uri.parse(action.url)))
                return
            }

            // ===== GENERIC VOLUME =====
            is OSAction.SetVolume -> {
                audioManager.setStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    action.percent,
                    AudioManager.FLAG_SHOW_UI
                )
                return
            }

            // ===== PHONE VOLUME =====
            OSAction.PhoneVolumeUp -> {
                audioManager.adjustVolume(AudioManager.ADJUST_RAISE, 0)
                return
            }

            OSAction.PhoneVolumeDown -> {
                audioManager.adjustVolume(AudioManager.ADJUST_LOWER, 0)
                return
            }

            OSAction.PhoneMute -> {
                audioManager.adjustVolume(AudioManager.ADJUST_MUTE, 0)
                return
            }

            OSAction.PhoneUnmute -> {
                audioManager.adjustVolume(AudioManager.ADJUST_UNMUTE, 0)
                return
            }

            // ===== CAMERA / MEDIA =====
            OSAction.OpenCamera -> {
                launchIntent(Intent("android.media.action.IMAGE_CAPTURE"))
                return
            }

            OSAction.OpenPhotos -> {
                launchIntent(Intent(Intent.ACTION_VIEW).apply { type = "image/*" })
                return
            }

            // ===== TV / REMOTE (STUBS FOR HARDWARE LAYER) =====
            OSAction.TvPowerOn,
            OSAction.TvPowerOff,
            OSAction.TvMute,
            OSAction.TvUnmute,
            OSAction.TvVolumeUp,
            OSAction.TvVolumeDown,
            is OSAction.TvSetVolume,
            OSAction.SwitchHdmi1,
            OSAction.SwitchHdmi2,
            OSAction.SwitchCable,
            OSAction.SwitchStreaming,
            OSAction.SwitchConsole,
            OSAction.ChannelChange,
            is OSAction.ChannelSet,
            OSAction.OpenGuide,
            OSAction.OpenMenu,
            OSAction.GoBack,
            OSAction.Confirm,
            OSAction.Play,
            OSAction.Pause,
            OSAction.FastForward,
            OSAction.Rewind,
            OSAction.SkipAhead,
            OSAction.SkipBack -> {
                // Future: integrate with TV / IR / network remote layer
                return
            }
        }
    }

    private fun launchIntentForPackage(packageName: String): Intent? =
        try {
            Intent(Intent.ACTION_MAIN).apply {
                setPackage(packageName)
                addCategory(Intent.CATEGORY_LAUNCHER)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } catch (_: Exception) {
            null
        }
}
