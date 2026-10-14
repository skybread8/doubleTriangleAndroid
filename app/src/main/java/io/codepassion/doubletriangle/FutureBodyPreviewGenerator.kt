package io.codepassion.doubletriangle

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import io.codepassion.doubletriangle.core.model.WildforceApiEnvironment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/** Uses the authenticated service for body forecasts; provider credentials never leave the backend. */
internal object FutureBodyPreviewGenerator {
    suspend fun generate(context: Context, sessionId: String, angle: String): Bitmap = withContext(Dispatchers.IO) {
        val token = token(context)
        val generation = (URL(endpoint(sessionId, angle)).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 30_000
            readTimeout = 180_000
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Idempotency-Key", UUID.randomUUID().toString())
        }
        try {
            val response = readResponse(generation)
            check(generation.responseCode in 200..299) { errorMessage(response, "No se pudo crear la previsión") }
        } finally {
            generation.disconnect()
        }
        load(context, sessionId, angle) ?: error("La previsión no está disponible todavía")
    }

    suspend fun load(context: Context, sessionId: String, angle: String): Bitmap? = withContext(Dispatchers.IO) {
        val connection = (URL(endpoint(sessionId, angle)).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 20_000
            readTimeout = 60_000
            setRequestProperty("Authorization", "Bearer ${token(context)}")
            setRequestProperty("Accept", "image/*")
        }
        try {
            if (connection.responseCode == HttpURLConnection.HTTP_NOT_FOUND) return@withContext null
            val bytes = (if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream)
                ?.use { it.readBytes() } ?: ByteArray(0)
            check(connection.responseCode in 200..299) { errorMessage(bytes.toString(Charsets.UTF_8), "No se pudo cargar la previsión") }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: error("El servidor no devolvió una imagen válida")
        } finally {
            connection.disconnect()
        }
    }

    private fun endpoint(sessionId: String, angle: String): String {
        require(angle in setOf("front", "profile")) { "Ángulo de foto no válido" }
        return WildforceApiEnvironment.apiUrl("body-progress-photo-sessions/$sessionId/future-photos/$angle")
    }

    private fun token(context: Context): String = context.getSharedPreferences("wildforce_account", Context.MODE_PRIVATE)
        .getString("token", null) ?: error("Inicia sesión para crear una previsión")

    private fun readResponse(connection: HttpURLConnection): String =
        (if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream)
            ?.bufferedReader()?.use { it.readText() }.orEmpty()

    private fun errorMessage(response: String, fallback: String): String = runCatching {
        JSONObject(response).optString("message").ifBlank {
            JSONObject(response).optJSONObject("error")?.optString("message").orEmpty()
        }
    }.getOrDefault("").ifBlank { fallback }
}
