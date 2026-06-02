package com.example.meaningosapp.core.tools

import android.content.Context
import io.ktor.client.HttpClient

class ToolRegistry(
    private val context: Context,
    private val client: HttpClient,
    private val baseUrl: String
) {

    // ⭐ Simple Response Tool
    val simpleResponseTool = SimpleResponseTool()

    // ⭐ Web Lookup Tool
    val webLookupTool = WebLookupTool(client, baseUrl)

    // ⭐ USB Serial Tool (ESP32 communication)
    val usbSerialTool = UsbSerialTool(context)

    // ⭐ App Launch Tool (NEW)
    val appLaunchTool = AppLaunchTool(context)

    // Public API for simple responses
    fun getSimpleResponse(): String {
        return simpleResponseTool.handleSimpleRequest()
    }

    // Public API for web lookup
    suspend fun lookup(question: String): String {
        return webLookupTool.execute(question)
    }

    // ⭐ Public API for launching apps (NEW)
    suspend fun launchApp(appName: String): String {
        return appLaunchTool.execute(appName)
    }

    // ⭐ Public API for sending hardware commands
    fun sendHardwareCommand(cmd: String): Boolean {
        if (!usbSerialTool.connect()) return false
        val ok = usbSerialTool.sendCommand(cmd)
        usbSerialTool.disconnect()
        return ok
    }
}
