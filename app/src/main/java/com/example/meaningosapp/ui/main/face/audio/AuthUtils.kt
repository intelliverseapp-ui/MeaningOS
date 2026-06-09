package com.example.meaningosapp.ui.main.face.audio

import com.google.auth.oauth2.AccessToken
import com.google.auth.oauth2.GoogleCredentials
import io.grpc.CallCredentials
import io.grpc.auth.MoreCallCredentials
import java.io.FileInputStream
import java.io.InputStream
import java.util.Date

/**
 * AuthUtils
 *
 * Helpers to produce gRPC CallCredentials for Google Cloud RPCs.
 *
 * WARNING: Do NOT embed service account JSON files in production APKs.
 * For production mobile apps prefer a backend that mints short-lived tokens
 * or implement an OAuth flow for end users.
 */
object AuthUtils {

    /**
     * Load CallCredentials from a service account JSON file path.
     *
     * Development / testing helper only.
     *
     * @param saJsonPath absolute path to service account JSON
     * @return CallCredentials to attach to gRPC stubs
     */
    fun loadCallCredentialsFromServiceAccount(saJsonPath: String): CallCredentials {
        FileInputStream(saJsonPath).use { fis ->
            return loadCallCredentialsFromStream(fis)
        }
    }

    /**
     * Load CallCredentials from an InputStream containing service account JSON.
     *
     * Useful if you obtain the JSON from secure storage or a file provider.
     *
     * @param stream InputStream of service account JSON
     * @return CallCredentials
     */
    fun loadCallCredentialsFromStream(stream: InputStream): CallCredentials {
        val creds = GoogleCredentials.fromStream(stream)
            .createScoped(listOf("https://www.googleapis.com/auth/cloud-platform"))
        creds.refreshIfExpired()
        return MoreCallCredentials.from(creds)
    }

    /**
     * Create CallCredentials from an OAuth2 access token string.
     *
     * Use this when you have an access token obtained via OAuth on-device
     * or from a backend. The token should be valid and refreshed by the caller.
     *
     * @param tokenString OAuth2 access token value
     * @param expiryMillis optional expiry epoch millis for the token; if unknown, pass null
     * @return CallCredentials
     */
    fun callCredentialsFromAccessToken(tokenString: String, expiryMillis: Long? = null): CallCredentials {
        val expiryDate = expiryMillis?.let { Date(it) } ?: Date(System.currentTimeMillis() + 60_000L)
        val accessToken = AccessToken(tokenString, expiryDate)
        val creds = GoogleCredentials.create(accessToken)
        return MoreCallCredentials.from(creds)
    }

    /**
     * Load CallCredentials using Application Default Credentials (ADC).
     *
     * Useful in development environments where ADC is configured.
     *
     * @return CallCredentials
     */
    fun loadCallCredentialsFromApplicationDefault(): CallCredentials {
        val creds = GoogleCredentials.getApplicationDefault()
            .createScoped(listOf("https://www.googleapis.com/auth/cloud-platform"))
        creds.refreshIfExpired()
        return MoreCallCredentials.from(creds)
    }
}
