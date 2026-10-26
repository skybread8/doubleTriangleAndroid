package io.codepassion.doubletriangle.feature.onboarding

import android.graphics.Bitmap
import android.graphics.Color
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.SegmentationMask
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import java.nio.ByteOrder
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

/** The same background choices exposed by iOS BodyProgressCompareView. */
internal enum class BodyProgressBackgroundStyle(val label: String) {
    White("Blanco"),
    Gradient("Degradado"),
    Blurred("Desenfocado"),
}

/**
 * Segments the person before compositing the selected background. This is
 * intentionally a real foreground mask, rather than applying a full-image
 * color/blur filter: iOS keeps the subject sharp in all three styled modes.
 */
internal object BodyProgressBackgroundRemoval {
    private const val MAX_PROCESSING_DIMENSION = 1024

    private val options = SelfieSegmenterOptions.Builder()
        .setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE)
        .build()

    suspend fun process(
        original: Bitmap,
        style: BodyProgressBackgroundStyle,
    ): Bitmap? = withContext(Dispatchers.Default) {
        val source = scaledForProcessing(original)
        val segmenter = Segmentation.getClient(options)
        try {
            val mask = segmenter
                .process(InputImage.fromBitmap(source, 0))
                .awaitResult()
            composite(source, mask, style)
        } catch (_: Exception) {
            null
        } finally {
            segmenter.close()
        }
    }

    private fun scaledForProcessing(original: Bitmap): Bitmap {
        val largest = maxOf(original.width, original.height)
        if (largest <= MAX_PROCESSING_DIMENSION) return original
        val scale = MAX_PROCESSING_DIMENSION.toFloat() / largest.toFloat()
        return Bitmap.createScaledBitmap(
            original,
            (original.width * scale).toInt().coerceAtLeast(1),
            (original.height * scale).toInt().coerceAtLeast(1),
            true,
        )
    }

    private fun composite(
        source: Bitmap,
        mask: SegmentationMask,
        style: BodyProgressBackgroundStyle,
    ): Bitmap {
        val width = source.width
        val height = source.height
        val foreground = IntArray(width * height)
        source.getPixels(foreground, 0, width, 0, 0, width, height)
        val background = backgroundPixels(source, style)
        val output = IntArray(foreground.size)
        val maskWidth = mask.width.coerceAtLeast(1)
        val maskHeight = mask.height.coerceAtLeast(1)
        val buffer = mask.buffer.duplicate().order(ByteOrder.nativeOrder())

        for (y in 0 until height) {
            val maskY = ((y.toFloat() / height) * maskHeight).toInt().coerceIn(0, maskHeight - 1)
            for (x in 0 until width) {
                val maskX = ((x.toFloat() / width) * maskWidth).toInt().coerceIn(0, maskWidth - 1)
                val confidence = buffer
                    .getFloat((maskY * maskWidth + maskX) * Float.SIZE_BYTES)
                    .coerceIn(0f, 1f)
                val foregroundColor = foreground[y * width + x]
                val backgroundColor = background[y * width + x]
                output[y * width + x] = blend(foregroundColor, backgroundColor, confidence)
            }
        }

        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also {
            it.setPixels(output, 0, width, 0, 0, width, height)
        }
    }

    private fun backgroundPixels(source: Bitmap, style: BodyProgressBackgroundStyle): IntArray {
        val width = source.width
        val height = source.height
        return when (style) {
            BodyProgressBackgroundStyle.White -> IntArray(width * height) { Color.WHITE }
            BodyProgressBackgroundStyle.Gradient -> IntArray(width * height) { index ->
                val y = index / width
                val progress = if (height <= 1) 0f else y.toFloat() / (height - 1).toFloat()
                val channel = (255f - 46f * progress).toInt().coerceIn(0, 255)
                Color.rgb(channel, channel, channel)
            }
            BodyProgressBackgroundStyle.Blurred -> blurredPixels(source)
        }
    }

    /** A downsample/upscale blur works on API 28+ and stays close to iOS's Gaussian treatment. */
    private fun blurredPixels(source: Bitmap): IntArray {
        val width = source.width
        val height = source.height
        val reducedWidth = (width / 18).coerceAtLeast(1)
        val reducedHeight = (height / 18).coerceAtLeast(1)
        val reduced = Bitmap.createScaledBitmap(source, reducedWidth, reducedHeight, true)
        val blurred = Bitmap.createScaledBitmap(reduced, width, height, true)
        val pixels = IntArray(width * height)
        blurred.getPixels(pixels, 0, width, 0, 0, width, height)
        if (reduced !== source) reduced.recycle()
        if (blurred !== source && blurred !== reduced) blurred.recycle()
        return pixels
    }

    private fun blend(foreground: Int, background: Int, confidence: Float): Int {
        val inverse = 1f - confidence
        return Color.argb(
            255,
            (Color.red(foreground) * confidence + Color.red(background) * inverse).toInt(),
            (Color.green(foreground) * confidence + Color.green(background) * inverse).toInt(),
            (Color.blue(foreground) * confidence + Color.blue(background) * inverse).toInt(),
        )
    }
}

private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { result ->
        if (continuation.isActive) continuation.resume(result)
    }
    addOnFailureListener { error ->
        if (continuation.isActive) continuation.resumeWithException(error)
    }
    addOnCanceledListener {
        if (continuation.isActive) continuation.cancel()
    }
}
