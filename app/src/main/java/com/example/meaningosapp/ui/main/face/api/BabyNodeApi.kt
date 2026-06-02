package com.example.meaningosapp.ui.main.face.api

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * BabyNodeApi — Minimal HTTP client for Baby Node 1.0
 *
 * Designed for REAL Android devices using your Mac's LAN IP.
 * Baby Node backend must be running on your Mac:
 *   npm start
 *   → listening on http://<your-mac-ip>:3000
 */
object BabyNodeApi {

    // ⭐ Your actual Mac LAN IP (confirmed via ifconfig)
    private const val BASE_URL = "http://192.168.208.98:3000"

    /**
     * Send text to Baby Node backend and return the reply.
     */
    suspend fun sendTextAndGetResponse(text: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/message")
                val connection = url.openConnection() as HttpURLConnection

                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                // ⭐ JSON body
                val body = JSONObject().apply {
                    put("text", text)
                }.toString()

                // ⭐ Send request
                connection.outputStream.use { os ->
                    os.write(body.toByteArray())
                }

                // ⭐ Read response
                val response = connection.inputStream
                    .bufferedReader()
                    .use { it.readText() }

                Log.d("BabyNodeApi", "Response: $response")

                // ⭐ Parse JSON
                val json = JSONObject(response)
                json.getString("response")

            } catch (e: Exception) {
                Log.e("BabyNodeApi", "Error: ${e.message}")
                throw e
            }
        }
    }
}
