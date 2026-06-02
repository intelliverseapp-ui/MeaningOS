package com.example.meaningosapp.core

import com.example.meaningosapp.core.tools.ToolType

sealed class OSAction {

    // ===== BASE ACTIONS =====
    object None : OSAction()
    object StopSpeech : OSAction()

    data class LaunchApp(val packageName: String) : OSAction()
    data class OpenUrl(val url: String) : OSAction()
    data class SetVolume(val percent: Int) : OSAction()

    object OpenCamera : OSAction()
    object OpenPhotos : OSAction()

    // ===== TV / REMOTE CONTROL ACTIONS =====
    object TvPowerOn : OSAction()
    object TvPowerOff : OSAction()

    // TV mute/unmute (separated from phone mute)
    object TvMute : OSAction()
    object TvUnmute : OSAction()

    // Phone mute/unmute
    object PhoneMute : OSAction()
    object PhoneUnmute : OSAction()

    // ===== INPUTS =====
    object SwitchHdmi1 : OSAction()
    object SwitchHdmi2 : OSAction()
    object SwitchCable : OSAction()
    object SwitchStreaming : OSAction()
    object SwitchConsole : OSAction()

    // ===== CHANNELS =====
    data class ChannelSet(val channel: Int) : OSAction()
    object ChannelChange : OSAction()

    // ===== NAVIGATION =====
    object OpenGuide : OSAction()
    object OpenMenu : OSAction()
    object GoBack : OSAction()
    object Confirm : OSAction()

    // ===== PLAYBACK =====
    object Play : OSAction()
    object Pause : OSAction()
    object FastForward : OSAction()
    object Rewind : OSAction()
    object SkipAhead : OSAction()
    object SkipBack : OSAction()

    // ===== TV VOLUME ACTIONS =====
    object TvVolumeUp : OSAction()
    object TvVolumeDown : OSAction()
    data class TvSetVolume(val percent: Int) : OSAction()

    // ===== PHONE VOLUME ACTIONS =====
    object PhoneVolumeUp : OSAction()
    object PhoneVolumeDown : OSAction()

    // ===== TOOL INVOCATION =====
    data class UseTool(
        val type: ToolType,
        val input: String
    ) : OSAction()
}
