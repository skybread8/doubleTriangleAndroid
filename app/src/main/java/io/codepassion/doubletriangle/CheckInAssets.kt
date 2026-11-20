package io.codepassion.doubletriangle

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import org.json.JSONObject
import org.json.JSONArray
import java.io.File
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

/** Pending body metrics belonging to a coach check-in, scoped by account. */
internal object CheckInAssets {
    private const val prefsName = "wildforce_check_in_assets"
    private val lock = Any()

    fun putMetric(context: Context, userId: String, checkInId: String, type: String, value: Double) = synchronized(lock) {
        require(value.isFinite() && value > 0)
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val entries = JSONObject(prefs.getString(userId, "{}") ?: "{}")
        val key = "$checkInId:$type"
        val previous = metrics(context, userId, checkInId).firstOrNull { it.optString("type") == type } ?: entries.optJSONObject(key)
        val now = Instant.now().truncatedTo(ChronoUnit.SECONDS).toString()
        val row = JSONObject()
            .put("id", previous?.optString("id")?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString())
            .put("created_at", previous?.optString("created_at")?.takeIf { it.isNotBlank() } ?: now)
            .put("updated_at", now).put("recorded_at", now).put("deleted_at", JSONObject.NULL)
            // The server stores body metrics as decimal(…, 3); send the same value
            // that it can return, including after converting pounds or inches.
            .put("type", type).put("value", kotlin.math.round(value * 1000.0) / 1000.0).put("source", "checkIn").put("check_in_id", checkInId)
            .put("version", UUID.randomUUID().toString()).put("pending", true)
        entries.put(key, row)
        check(prefs.edit().putString(userId, entries.toString()).commit())
    }

    fun metrics(context: Context, userId: String, checkInId: String? = null): List<JSONObject> = synchronized(lock) {
        val raw = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE).getString(userId, "{}") ?: "{}"
        val entries = JSONObject(raw)
        val local = entries.keys().asSequence().mapNotNull { entries.optJSONObject(it) }
            .filter { checkInId == null || it.optString("check_in_id") == checkInId }
            .map { JSONObject(it.toString()) }.toList()
        if (checkInId == null) return@synchronized local
        val projection = context.getSharedPreferences("wildforce_sync_state", Context.MODE_PRIVATE)
            .getString("remote_projection:body-metric-entries", "[]") ?: "[]"
        val remote = JSONArray(projection)
        val linked = (0 until remote.length()).mapNotNull { remote.optJSONObject(it) }
            .filter { it.optString("check_in_id").equals(checkInId, true) && it.optString("user_id").equals(userId, true) }
        val types = (local.map { it.optString("type") } + linked.map { it.optString("type") }).toSet()
        types.mapNotNull { type ->
            val byKey = linkedMapOf<String, JSONObject>()
            fun revisions(rows: List<JSONObject>, prefix: String) = rows.filter { it.optString("type") == type }.mapIndexed { index, row ->
                val revisionKey = "$prefix$index"
                byKey[revisionKey] = row
                CheckInMetricRevision(revisionKey, row.optString("id"), checkInTimestamp(row.optString("updated_at")),
                    checkInTimestamp(row.optString("recorded_at")), !row.isNull("deleted_at"), prefix == "local" && row.optBoolean("pending"))
            }
            resolveCheckInMetric(revisions(local, "local"), revisions(linked, "remote"))?.let { byKey[it.key] }
        }
    }

    fun clearMetric(context: Context, userId: String, checkInId: String, type: String) = synchronized(lock) {
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val entries = JSONObject(prefs.getString(userId, "{}") ?: "{}")
        val key = "$checkInId:$type"
        val row = metrics(context, userId, checkInId).firstOrNull { it.optString("type") == type }?.let { remote ->
            JSONObject().apply {
                listOf("id", "created_at", "recorded_at", "type", "value", "source", "check_in_id").forEach { put(it, remote.opt(it)) }
            }
        } ?: return@synchronized
        val now = Instant.now().truncatedTo(ChronoUnit.SECONDS).toString()
        row.put("deleted_at", now).put("updated_at", now).put("version", UUID.randomUUID().toString()).put("pending", true)
        entries.put(key, row)
        check(prefs.edit().putString(userId, entries.toString()).commit())
    }

    fun acknowledge(context: Context, userId: String, row: JSONObject) = synchronized(lock) {
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val entries = JSONObject(prefs.getString(userId, "{}") ?: "{}")
        val key = "${row.getString("check_in_id")}:${row.getString("type")}"
        val current = entries.optJSONObject(key)
        if (current?.optString("version") == row.optString("version")) {
            current.put("pending", false)
            check(prefs.edit().putString(userId, entries.toString()).commit())
        }
    }

    fun putPhoto(context: Context, userId: String, checkInId: String, angle: String, uri: Uri) = synchronized(lock) {
        require(angle in setOf("front", "profile", "back"))
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val key = "$userId:photos"
        val entries = JSONObject(prefs.getString(key, "{}") ?: "{}")
        val previous = photos(context, userId).firstOrNull { it.optString("check_in_id").equals(checkInId, true) } ?: entries.optJSONObject(checkInId)
        val id = previous?.optString("id")?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sampleSize = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sampleSize > 3200) sampleSize *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val image = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: error("No se pudo abrir la foto")
        val folder = File(context.filesDir, "check_in_photos").apply { mkdirs() }
        val file = File(folder, "${id}_${angle}_${UUID.randomUUID()}.jpg")
        val orientation = runCatching { context.contentResolver.openInputStream(uri)?.use { ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) } }.getOrNull()
        val matrix = Matrix().apply {
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> postScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> { postRotate(90f); postScale(-1f, 1f) }
                ExifInterface.ORIENTATION_TRANSVERSE -> { postRotate(270f); postScale(-1f, 1f) }
            }
        }
        val oriented = if (matrix.isIdentity) image else Bitmap.createBitmap(image, 0, 0, image.width, image.height, matrix, true)
        val longest = maxOf(oriented.width, oriented.height).coerceAtLeast(1)
        val scaled = if (longest <= 1600) oriented else Bitmap.createScaledBitmap(oriented, (oriented.width * 1600 / longest).coerceAtLeast(1), (oriented.height * 1600 / longest).coerceAtLeast(1), true)
        try {
            file.outputStream().use { check(scaled.compress(Bitmap.CompressFormat.JPEG, 85, it)) }
        } finally { if (scaled !== oriented) scaled.recycle(); if (oriented !== image) oriented.recycle(); image.recycle() }
        val now = Instant.now().truncatedTo(ChronoUnit.SECONDS).toString()
        val row = previous ?: JSONObject().put("id", id).put("created_at", now).put("check_in_id", checkInId)
        row.put("updated_at", now).put("deleted_at", JSONObject.NULL).put(angle, file.absolutePath)
            .put("version", UUID.randomUUID().toString()).put("pending", true)
        entries.put(checkInId, row)
        check(prefs.edit().putString(key, entries.toString()).commit())
    }

    /** Persists the complete guided capture as one durable update. */
    fun putGuidedPhotos(context: Context, userId: String, checkInId: String, front: Bitmap, profile: Bitmap, back: Bitmap) = synchronized(lock) {
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val key = "$userId:photos"
        val entries = JSONObject(prefs.getString(key, "{}") ?: "{}")
        val previous = photos(context, userId).firstOrNull { it.optString("check_in_id").equals(checkInId, true) } ?: entries.optJSONObject(checkInId)
        val id = previous?.optString("id")?.takeIf(String::isNotBlank) ?: UUID.randomUUID().toString()
        val now = Instant.now().truncatedTo(ChronoUnit.SECONDS).toString()
        val row = previous?.let { JSONObject(it.toString()) }
            ?: JSONObject().put("id", id).put("created_at", now).put("check_in_id", checkInId)
        val folder = File(context.filesDir, "check_in_photos").apply { mkdirs() }
        val written = mutableListOf<File>()
        try {
            listOf("front" to front, "profile" to profile, "back" to back).forEach { (angle, bitmap) ->
                val file = File(folder, "${id}_${angle}_${UUID.randomUUID()}.jpg")
                written += file
                val longest = maxOf(bitmap.width, bitmap.height).coerceAtLeast(1)
                val scaled = if (longest <= 1600) bitmap else Bitmap.createScaledBitmap(bitmap, (bitmap.width * 1600 / longest).coerceAtLeast(1), (bitmap.height * 1600 / longest).coerceAtLeast(1), true)
                try { file.outputStream().use { check(scaled.compress(Bitmap.CompressFormat.JPEG, 85, it)) } }
                finally { if (scaled !== bitmap) scaled.recycle() }
                row.put(angle, file.absolutePath)
            }
            row.put("updated_at", now).put("deleted_at", JSONObject.NULL).put("version", UUID.randomUUID().toString()).put("pending", true)
            entries.put(checkInId, row)
            check(prefs.edit().putString(key, entries.toString()).commit())
        } catch (error: Throwable) {
            written.forEach { it.delete() }
            throw error
        }
    }

    fun photos(context: Context, userId: String): List<JSONObject> = synchronized(lock) {
        val raw = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE).getString("$userId:photos", "{}") ?: "{}"
        val entries = JSONObject(raw)
        val local = entries.keys().asSequence().mapNotNull { entries.optJSONObject(it) }.map { JSONObject(it.toString()) }.toList()
        val projection = context.getSharedPreferences("wildforce_sync_state", Context.MODE_PRIVATE)
            .getString("remote_projection:body-progress-photo-sessions", "[]") ?: "[]"
        val remote = JSONArray(projection)
        val server = (0 until remote.length()).mapNotNull { remote.optJSONObject(it) }
            .filter { it.optString("user_id").equals(userId, true) }
        val checkInIds = (local.map { it.optString("check_in_id") } + server.filter { !it.isNull("check_in_id") }.map { it.optString("check_in_id") }).filter(String::isNotBlank).toSet()
        checkInIds.mapNotNull { checkInId ->
            val cached = local.firstOrNull { it.optString("check_in_id").equals(checkInId, true) }
            val byKey = mutableMapOf<String, JSONObject>()
            fun revision(key: String, row: JSONObject, localRow: Boolean): CheckInPhotoRevision {
                byKey[key] = row
                return CheckInPhotoRevision(key, row.optString("id"), row.optString("check_in_id").takeUnless { it.isBlank() || it == "null" },
                    checkInTimestamp(row.optString("created_at")), checkInTimestamp(row.optString("updated_at")), !row.isNull("deleted_at"), localRow && row.optBoolean("pending"))
            }
            val localRevision = cached?.let { revision("local", it, true) }
            val remoteRevisions = server.mapIndexed { index, row -> revision("remote$index", row, false) }
            resolveCheckInPhotos(checkInId, localRevision, remoteRevisions)?.let { byKey[it.key] }
                ?.let { row -> JSONObject(row.toString()).apply { listOf("front", "profile", "back").forEach { angle -> val path = row.optString("${angle}_photo_path"); if (path.isNotBlank() && path != "null") put(angle, path) } } }
        }
    }

    fun acknowledgePhoto(context: Context, userId: String, row: JSONObject) = synchronized(lock) {
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val key = "$userId:photos"
        val entries = JSONObject(prefs.getString(key, "{}") ?: "{}")
        val current = entries.optJSONObject(row.getString("check_in_id"))
        if (current?.optString("version") == row.optString("version")) {
            current.put("pending", false)
            check(prefs.edit().putString(key, entries.toString()).commit())
        }
    }
}
