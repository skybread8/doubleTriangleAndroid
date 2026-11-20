package io.codepassion.doubletriangle

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.util.UUID

internal fun checkInJsonEquivalent(a: Any?, b: Any?): Boolean {
    if (a == null || a == JSONObject.NULL) return b == null || b == JSONObject.NULL || (b is JSONArray && b.length() == 0)
    if (b == null || b == JSONObject.NULL) return a is JSONArray && a.length() == 0
    if (a is JSONObject && b is JSONObject) {
        val keys = a.keys().asSequence().toSet()
        return keys == b.keys().asSequence().toSet() && keys.all { checkInJsonEquivalent(a.opt(it), b.opt(it)) }
    }
    if (a is JSONArray && b is JSONArray) return a.length() == b.length() && (0 until a.length()).all { checkInJsonEquivalent(a.opt(it), b.opt(it)) }
    if (a is Number && b is Number) return a.toDouble() == b.toDouble()
    return a == b
}

/** Durable outbound edits, scoped to the account that owns the check-in. */
internal object CheckInDraftStore {
    private const val preferences = "wildforce_check_in_drafts"
    private val lock = Any()

    fun save(context: Context, userId: String, record: JSONObject, answers: JSONObject, submit: Boolean = false) = synchronized(lock) {
        check(record.optString("status") != "submitted") { "This check-in has already been submitted" }
        val id = record.getString("id")
        val prefs = context.getSharedPreferences(preferences, Context.MODE_PRIVATE)
        val queue = JSONObject(prefs.getString(userId, "{}") ?: "{}")
        // The server stores sync timestamps at second precision.
        val updated = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS).toString()
        val payload = JSONObject()
            .put("id", id)
            .put("created_at", record.getString("created_at"))
            .put("updated_at", updated)
            .put("deleted_at", JSONObject.NULL)
            .put("status", if (submit) "submitted" else if (answers.length() > 0 || record.optString("status") == "draft") "draft" else "pending")
            .put("answers", if (answers.length() == 0) JSONObject.NULL else JSONObject(answers.toString()))
            .put("submitted_at", if (submit) updated else JSONObject.NULL)
        queue.put(id, JSONObject().put("version", UUID.randomUUID().toString()).put("payload", payload))
        check(prefs.edit().putString(userId, queue.toString()).commit()) { "Could not save check-in draft" }
    }

    fun pending(context: Context, userId: String): List<JSONObject> = synchronized(lock) {
        val raw = context.getSharedPreferences(preferences, Context.MODE_PRIVATE).getString(userId, "{}") ?: "{}"
        val queue = JSONObject(raw)
        queue.keys().asSequence().mapNotNull { queue.optJSONObject(it) }.map { JSONObject(it.toString()) }.toList()
    }

    /** Do not discard a newer edit made while the request was in flight. */
    fun acknowledge(context: Context, userId: String, id: String, version: String) = synchronized(lock) {
        val prefs = context.getSharedPreferences(preferences, Context.MODE_PRIVATE)
        val queue = JSONObject(prefs.getString(userId, "{}") ?: "{}")
        if (checkInVersionAcknowledged(version, queue.optJSONObject(id)?.optString("version"))) {
            queue.remove(id)
            check(prefs.edit().putString(userId, queue.toString()).commit()) { "Could not acknowledge check-in draft" }
        }
    }

    fun projected(context: Context, userId: String): JSONArray {
        val state = context.getSharedPreferences("wildforce_sync_state", Context.MODE_PRIVATE)
        val records = JSONArray(state.getString("remote_projection:check-ins", "[]") ?: "[]")
        val drafts = pending(context, userId).associateBy { it.getJSONObject("payload").getString("id") }
        return JSONArray().apply {
            for (index in 0 until records.length()) {
                val record = records.optJSONObject(index) ?: continue
                val payload = drafts[record.optString("id")]?.optJSONObject("payload")
                if (shouldOverlayCheckInDraft(record.optString("status"), !record.isNull("deleted_at")) && payload != null) {
                    listOf("status", "answers", "submitted_at", "updated_at").forEach { record.put(it, payload.opt(it)) }
                    // A queued submission is not confirmed until the server accepts it.
                    // Keep it openable if the connection fails or the app is restarted.
                    if (payload.optString("status") == "submitted") {
                        record.put("status", "draft").put("submitted_at", JSONObject.NULL)
                    }
                }
                put(record)
            }
        }
    }
}
