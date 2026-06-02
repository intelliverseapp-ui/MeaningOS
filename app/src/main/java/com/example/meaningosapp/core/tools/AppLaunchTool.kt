package com.example.meaningosapp.core.tools

import android.content.Context
import android.content.Intent

class AppLaunchTool(private val context: Context) : Tool {

    override val type = ToolType.APP_LAUNCHER

    override suspend fun execute(input: String): String {
        return try {
            val packageName = resolvePackage(input)
            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)

            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                "Opening $input."
            } else {
                "I couldn't find the app $input on this device."
            }
        } catch (e: Exception) {
            "Failed to open $input."
        }
    }

    private fun resolvePackage(appName: String): String {
        return when (appName.lowercase()) {
            "chrome" -> "com.android.chrome"
            "youtube" -> "com.google.android.youtube"
            "instagram" -> "com.instagram.android"
            "spotify" -> "com.spotify.music"
            else -> appName
        }
    }
}
