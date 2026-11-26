package io.codepassion.doubletriangle.core.model

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

/** Local, bounded operational journal. Never store credentials or record bodies. */
object SyncDiagnostics {
    const val PREFS = "wildforce_sync_diagnostics"
    private const val KEY = "events"
    private const val LIMIT = 1500

    fun errorDetail(error: Throwable): String {
        val message = error.message.orEmpty()
        // A transport error may contain an entire response body. Do not put
        // meal/profile payloads into a journal that can be copied or shared.
        val detail = (if (message.trimStart().startsWith("{") || message.trimStart().startsWith("[") || message.trimStart().startsWith("<")) "[response body omitted]" else message)
            .replace(Regex("(?i)bearer\\s+[^\\s,;]+"), "Bearer [redacted]")
            .replace(Regex("(?i)(token|password|authorization|secret|api[_-]?key)([\\s\"':=]+)[^\\s,}]+"), "$1$2[redacted]")
            .replace(Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}"), "[email]")
            .replace(Regex("eyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+"), "[token]")
            .replace(Regex("https?://[^\\s]+"), "[url]")
            .take(300)
        return error.javaClass.simpleName + if (detail.isBlank()) "" else ": $detail"
    }

    @Synchronized
    fun record(context: Context, category: String, message: String) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val previous = runCatching { JSONArray(prefs.getString(KEY, "[]")) }.getOrDefault(JSONArray())
        val events = JSONArray()
        for (index in (previous.length() - LIMIT + 1).coerceAtLeast(0) until previous.length()) {
            previous.optJSONObject(index)?.let(events::put)
        }
        events.put(JSONObject().put("at", Instant.now().toString()).put("category", category.take(48)).put("message", message.take(600)))
        prefs.edit().putString(KEY, events.toString()).apply()
    }

    @Synchronized
    fun entries(context: Context): List<String> {
        val raw = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "[]")
        val events = runCatching { JSONArray(raw) }.getOrDefault(JSONArray())
        return (events.length() - 1 downTo 0).mapNotNull { index ->
            events.optJSONObject(index)?.let { "${it.optString("at")}  ${it.optString("category")}  ${it.optString("message")}" }
        }
    }
}
