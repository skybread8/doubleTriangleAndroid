package io.codepassion.doubletriangle.feature.workout

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.Text
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val IMAGE_BASE = "https://khbsrnhvonggicfmhcpg.supabase.co/storage/v1/object/public/wildfit"
private val memoryCache = object : LruCache<String, Bitmap>(32 * 1024 * 1024) {
    override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
}

@Composable
fun RemoteTrainingImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val context = LocalContext.current.applicationContext
    val bitmap by produceState<Bitmap?>(initialValue = url?.let(memoryCache::get), key1 = url) {
        value = url?.let { loadTrainingBitmap(context, it) }
    }
    if (bitmap != null) {
        Image(bitmap = bitmap!!.asImageBitmap(), contentDescription = contentDescription, modifier = modifier, contentScale = contentScale)
    } else {
        Box(modifier.background(Brush.linearGradient(listOf(Color(0xFF171717), Color(0xFF5A5141), Color(0xFF8B7A5C))))) {
            contentDescription?.takeIf(String::isNotBlank)?.let {
                Text("$it\nImagen no disponible", modifier = Modifier.fillMaxSize().padding(12.dp), color = Color.White.copy(alpha = 0.72f), textAlign = TextAlign.Center)
            }
        }
    }
}

fun exerciseImageUrl(imageKey: String?, gender: String): String? =
    imageKey?.takeIf(String::isNotBlank)?.let { "$IMAGE_BASE/vertical/${it}_${gender}.jpeg" }

internal fun exerciseTutorialImageUrl(imageKey: String?): String? =
    imageKey?.takeIf(String::isNotBlank)?.let { "$IMAGE_BASE/tutorial/${it}_female.png" }

/**
 * Mirrors `WorkoutDay.coverImage()` on iOS: a workout keeps the same cover
 * variant even when its display order changes.
 */
internal fun workoutCoverUrl(focus: String, gender: String, workoutId: String): String {
    val normalized = focus.lowercase()
    val focusKey = when {
        "empuje" in normalized || "push" in normalized -> "push"
        "tirón" in normalized || "tiron" in normalized || "pull" in normalized -> "pull"
        "pierna" in normalized || "legs" in normalized -> "legs"
        "superior" in normalized || "upper" in normalized -> "upperBody"
        "inferior" in normalized || "lower" in normalized -> "lowerBody"
        "core" in normalized || "abd" in normalized -> "core"
        "cardio" in normalized -> "cardio"
        "movilidad" in normalized || "mobility" in normalized -> "mobility"
        "recuper" in normalized || "recovery" in normalized -> "recovery"
        else -> "fullBody"
    }
    val compactUuid = workoutId.filter(Char::isLetterOrDigit)
    val uuidByteSum = compactUuid
        .takeIf { it.length == 32 && it.all { character -> character.digitToIntOrNull(16) != null } }
        ?.chunked(2)
        ?.sumOf { byte -> byte.toInt(16) }
        // Local/demo workouts do not have a UUID. Keep their fallback stable too.
        ?: workoutId.encodeToByteArray().sumOf { byte -> byte.toInt() and 0xFF }
    val variant = (uuidByteSum % 5) + 1
    return "$IMAGE_BASE/covers/cover_${focusKey}_${gender}_$variant.png"
}

internal suspend fun loadTrainingBitmap(context: Context, url: String): Bitmap? = withContext(Dispatchers.IO) {
    memoryCache.get(url)?.let { return@withContext it }
    runCatching {
        val directory = File(context.cacheDir, "training-images").apply { mkdirs() }
        val key = MessageDigest.getInstance("SHA-256").digest(url.toByteArray()).joinToString("") { "%02x".format(it) }
        val cached = File(directory, key)
        val bitmap = if (cached.exists()) {
            BitmapFactory.decodeFile(cached.absolutePath)
        } else {
            val connection = URL(url).openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = 15_000
                connection.readTimeout = 30_000
                connection.instanceFollowRedirects = true
                check(connection.responseCode in 200..299)
                val bytes = connection.inputStream.use { it.readBytes() }
                cached.writeBytes(bytes)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } finally {
                connection.disconnect()
            }
        }
        bitmap?.also { memoryCache.put(url, it) }
    }.getOrNull()
}
