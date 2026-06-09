// FILE: GoogleCloudTtsClient.kt
package com.example.meaningosapp.ui.main.face.audio

import android.util.Base64
import com.example.meaningosapp.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean

/**
 * GoogleCloudTtsClient (REST)
 *
 * - Uses Google Cloud Text-to-Speech REST API
 * - Returns LINEAR16 PCM audio
 * - Chunks audio manually to simulate streaming
 * - Safe cancellation via AtomicBoolean + coroutine isActive
 *
 * Baby Node 1.0: stable, simple, no gRPC required.
 */
class GoogleCloudTtsClient {

    private val http = OkHttpClient()
    private val running = AtomicBoolean(false)

    /**
     * Synthesize text → PCM16 audio chunks.
     */
    suspend fun synthesize(
        text: String,
        emotion: BabyEmotion,
        speakingRate: Double,
        pitch: Double,
        languageCode: String,
        voiceName: String,
        sampleRateHz: Int,
        onAudioChunk: suspend (ByteArray) -> Unit,
        onError: (Throwable) -> Unit
    ) = withContext(Dispatchers.IO) {

        if (!running.compareAndSet(false, true)) return@withContext

        try {
            val json = JSONObject().apply {
                put("input", JSONObject().put("text", text))

                put(
                    "voice",
                    JSONObject()
                        .put("languageCode", languageCode)
                        .put("name", voiceName)
                )

                put(
                    "audioConfig",
                    JSONObject()
                        .put("audioEncoding", "LINEAR16")
                        .put("speakingRate", speakingRate)
                        .put("pitch", pitch)
                        .put("sampleRateHertz", sampleRateHz)
                )
            }

            val body = json.toString()
                .toRequestBody("application/json; charset=utf-8".toMediaType())

            val request = Request.Builder()
                .url("https://texttospeech.googleapis.com/v1/text:synthesize?key=${BuildConfig.GOOGLE_CLOUD_TTS_API_KEY}")
                .post(body)
                .build()

            val response = http.newCall(request).execute()

            response.use { resp ->
                if (!resp.isSuccessful) {
                    val errorBody = resp.body?.string()
                    val msg = try {
                        JSONObject(errorBody ?: "")
                            .getJSONObject("error")
                            .getString("message")
                    } catch (_: Exception) {
                        "TTS HTTP error: ${resp.code}"
                    }
                    throw RuntimeException(msg)
                }

                val responseJson = JSONObject(resp.body?.string() ?: "")
                val audioBase64 = responseJson.optString("audioContent", "")

                if (audioBase64.isEmpty()) {
                    throw RuntimeException("TTS returned empty audioContent")
                }

                val pcmBytes = Base64.decode(audioBase64, Base64.DEFAULT)

                val bytesPerMs = (sampleRateHz * 2) / 1000
                val chunkSize = bytesPerMs * 100

                var offset = 0
                while (offset < pcmBytes.size && isActive && running.get()) {
                    val end = minOf(offset + chunkSize, pcmBytes.size)
                    val chunk = pcmBytes.copyOfRange(offset, end)
                    onAudioChunk(chunk)
                    offset = end
                }
            }

        } catch (e: Exception) {
            onError(e)
        } finally {
            running.set(false)
        }
    }

    /**
     * Stop any ongoing synthesis.
     */
    fun stop() {
        running.set(false)
    }

    /**
     * Alias for compatibility with shutdown() calls.
     */
    fun shutdown() {
        stop()
    }
}
