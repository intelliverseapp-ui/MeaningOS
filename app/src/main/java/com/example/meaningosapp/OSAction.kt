package com.example.meaningosapp

sealed class OSAction {
    object None : OSAction()
    data class LaunchApp(val packageName: String) : OSAction()
    data class OpenUrl(val url: String) : OSAction()
    data class SetVolume(val percent: Int) : OSAction()
    object StopSpeech : OSAction()
}
