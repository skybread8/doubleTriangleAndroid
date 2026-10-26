package io.codepassion.doubletriangle

import android.content.Context
import io.codepassion.doubletriangle.core.model.WildforceApiEnvironment
import io.codepassion.doubletriangle.feature.onboarding.BodyCompositionAnalysis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/** Requests the same non-persisted body-composition estimate as the iOS media API. */
internal object BodyCompositionAnalysisGenerator {
    suspend fun analyze(context: Context, sessionId: String): BodyCompositionAnalysis = withContext(Dispatchers.IO) {
        val token = context.getSharedPreferences("wildforce_account", Context.MODE_PRIVATE)
            .getString("token", null) ?: error("Inicia sesión para analizar el check-in")
        val connection = (URL(WildforceApiEnvironment.apiUrl("body-progress-photo-sessions/$sessionId/composition-analysis")).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 30_000
            readTimeout = 120_000
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Idempotency-Key", UUID.randomUUID().toString())
        }
        try {
            val body = (if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            check(connection.responseCode in 200..299) { errorMessage(body) }
            val data = JSONObject(body).optJSONObject("data") ?: JSONObject(body)
            BodyCompositionAnalysis(
                estimatedBodyFatRange = data.optString("estimated_body_fat_range"),
                confidence = when (data.optString("confidence").lowercase()) {
                    "high" -> BodyCompositionAnalysis.Confidence.High
                    "medium" -> BodyCompositionAnalysis.Confidence.Medium
                    else -> BodyCompositionAnalysis.Confidence.Low
                },
                summary = data.optString("summary"),
                insights = data.optJSONArray("insights").asStringList(),
                recommendations = data.optJSONArray("recommendations").asStringList(),
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun errorMessage(body: String): String = runCatching {
        val json = JSONObject(body)
        json.optString("message").ifBlank { json.optJSONObject("error")?.optString("message").orEmpty() }
    }.getOrNull().orEmpty().ifBlank { "No se pudo analizar el check-in" }

    private fun JSONArray?.asStringList(): List<String> = this?.let { array ->
        (0 until array.length()).mapNotNull { array.optString(it).takeIf(String::isNotBlank) }
    }.orEmpty()
}
