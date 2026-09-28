package io.codepassion.doubletriangle.feature.onboarding

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.core.content.FileProvider
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.util.UUID

/** A locally-owned, paired front/profile body check-in. Photos never leave the device here. */
internal data class BodyProgressSession(
    val id: String,
    val date: String,
    val frontPath: String,
    val profilePath: String,
    val futurePath: String? = null,
)

internal object BodyProgressStore {
    private const val prefs = "wildforce_body_progress"
    private const val key = "sessions"
    private const val directory = "body_progress_photos"

    fun load(context: Context): List<BodyProgressSession> = runCatching {
        val raw = context.getSharedPreferences(prefs, 0).getString(key, null) ?: return emptyList()
        val values = JSONArray(raw)
        buildList {
            for (index in 0 until values.length()) {
                val item = values.optJSONObject(index) ?: continue
                val id = item.optString("id")
                val date = item.optString("date")
                val front = item.optString("frontPath")
                val profile = item.optString("profilePath")
                if (id.isNotBlank() && date.isNotBlank() && File(front).isFile && File(profile).isFile) {
                    add(BodyProgressSession(id, date, front, profile, item.optString("futurePath").takeIf { File(it).isFile }))
                }
            }
        }.sortedBy { it.date }
    }.getOrDefault(emptyList())

    fun save(context: Context, front: Bitmap, profile: Bitmap): BodyProgressSession {
        val id = UUID.randomUUID().toString()
        val folder = File(context.filesDir, directory).apply { mkdirs() }
        val frontFile = File(folder, "${id}_front.jpg")
        val profileFile = File(folder, "${id}_profile.jpg")
        writeScaled(front, frontFile)
        writeScaled(profile, profileFile)
        val session = BodyProgressSession(id, LocalDate.now().toString(), frontFile.absolutePath, profileFile.absolutePath)
        persist(context, (load(context) + session).takeLast(60))
        return session
    }

    fun delete(context: Context, session: BodyProgressSession) {
        File(session.frontPath).delete()
        File(session.profilePath).delete()
        session.futurePath?.let(::File)?.delete()
        persist(context, load(context).filterNot { it.id == session.id })
    }

    fun bitmapFrom(context: Context, uri: Uri): Bitmap? = runCatching {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
    }.getOrNull()

    fun newCameraUri(context: Context): Uri {
        val folder = File(context.cacheDir, "body_progress_camera").apply { mkdirs() }
        val photo = File.createTempFile("capture_", ".jpg", folder)
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photo)
    }

    fun saveFuture(context: Context, session: BodyProgressSession, future: Bitmap): BodyProgressSession {
        val file = File(context.filesDir, directory).apply { mkdirs() }.resolve("${session.id}_future.jpg")
        writeScaled(future, file)
        val updated = session.copy(futurePath = file.absolutePath)
        persist(context, load(context).map { if (it.id == session.id) updated else it })
        return updated
    }

    private fun persist(context: Context, sessions: List<BodyProgressSession>) {
        val value = JSONArray().apply {
            sessions.forEach { session ->
                put(JSONObject().put("id", session.id).put("date", session.date).put("frontPath", session.frontPath).put("profilePath", session.profilePath).put("futurePath", session.futurePath))
            }
        }
        context.getSharedPreferences(prefs, 0).edit().putString(key, value.toString()).apply()
    }

    private fun writeScaled(source: Bitmap, destination: File) {
        val longest = maxOf(source.width, source.height).coerceAtLeast(1)
        val bitmap = if (longest <= 1600) source else {
            val scale = 1600f / longest
            Bitmap.createScaledBitmap(source, (source.width * scale).toInt(), (source.height * scale).toInt(), true)
        }
        FileOutputStream(destination).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 92, it) }
        if (bitmap !== source) bitmap.recycle()
    }
}
