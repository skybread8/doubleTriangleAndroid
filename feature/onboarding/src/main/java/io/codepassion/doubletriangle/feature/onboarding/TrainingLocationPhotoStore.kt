package io.codepassion.doubletriangle.feature.onboarding

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/** Private, app-owned copies of training-location photos.
 *
 * `photoPath` is deliberately only the server-safe file name.  The physical
 * file always stays below this directory, as it does on iOS.
 */
object TrainingLocationPhotoStore {
    private const val directory = "training_location_photos"
    private val safeName = Regex("[a-z0-9-]+\\.jpe?g")

    fun import(context: Context, uri: Uri): String? = runCatching {
        val bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
        val filename = "${UUID.randomUUID()}.jpg"
        writeScaled(bitmap, requireNotNull(file(context, filename)))
        if (!bitmap.isRecycled) bitmap.recycle()
        filename
    }.getOrNull()

    fun file(context: Context, photoPath: String?): File? {
        val filename = photoPath?.substringAfterLast('/')?.lowercase() ?: return null
        if (!safeName.matches(filename)) return null
        return File(context.filesDir, "$directory/$filename")
    }

    private fun writeScaled(source: Bitmap, destination: File) {
        destination.parentFile?.mkdirs()
        val longest = maxOf(source.width, source.height).coerceAtLeast(1)
        val bitmap = if (longest <= 1600) source else {
            val scale = 1600f / longest
            Bitmap.createScaledBitmap(source, (source.width * scale).toInt(), (source.height * scale).toInt(), true)
        }
        FileOutputStream(destination).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 92, it) }
        if (bitmap !== source) bitmap.recycle()
    }
}
