package com.example.meaningosapp.core.tools

import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import org.json.JSONObject

class WebLookupTool(
    private val client: HttpClient,
    private val baseUrl: String
) : Tool {

    override val type = ToolType.WEB_LOOKUP

    override suspend fun execute(input: String): String {
        return try {
            val response = client.post("$baseUrl/v1/tools/web-lookup") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)

                setBody(
                    mapOf(
                        "query" to input,
                        "locale" to "en-US"
                    )
                )
            }

            if (!response.status.isSuccess()) {
                return "I couldn’t find a reliable answer."
            }

            val json = response.bodyAsText()
            val obj = JSONObject(json)
            obj.optString("answer", "I couldn’t find a reliable answer.")

        } catch (e: Exception) {
            "I couldn’t look that up right now."
        }
    }
}
