package io.codepassion.doubletriangle.feature.workout

import android.content.Context
import io.codepassion.doubletriangle.core.model.WildforceApiEnvironment
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** Android counterpart of iOS WildforceApiFeedback.submit. */
internal object FeedbackClient {
    suspend fun submit(context: Context, category: String, message: String) = withContext(Dispatchers.IO) {
        val token = context.getSharedPreferences("wildforce_account", Context.MODE_PRIVATE)
            .getString("token", null)
            ?: error("Inicia sesión para enviar feedback")
        val connection = (URL(WildforceApiEnvironment.apiUrl("feedback")).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 30_000
            readTimeout = 30_000
            doOutput = true
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Content-Type", "application/json")
        }
        try {
            connection.outputStream.bufferedWriter().use { writer ->
                writer.write(JSONObject().put("category", category).put("message", message).toString())
            }
            val response = (if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            check(connection.responseCode in 200..299) {
                runCatching { JSONObject(response).optString("message") }
                    .getOrNull()?.takeIf(String::isNotBlank)
                    ?: "No se pudo enviar el feedback"
            }
        } finally {
            connection.disconnect()
        }
    }
}
