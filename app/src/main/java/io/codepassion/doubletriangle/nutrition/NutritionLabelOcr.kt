package io.codepassion.doubletriangle.nutrition

import android.graphics.Bitmap
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlin.math.abs
import kotlin.math.max

/** One recognized line in image coordinates, used by both local parsing and API table serialization. */
internal data class NutritionLabelTextLine(
    val text: String,
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
) {
    val midY: Float get() = top + height / 2f
}

/**
 * Android counterpart of NutritionLabelRecognizedToTableText and
 * RegexpNutritionLabelParser on iOS. Image coordinates start at the top, so
 * reading order is the inverse of Vision's normalized bottom-left Y axis.
 */
internal object NutritionLabelOcr {
    fun recognize(bitmap: Bitmap): List<NutritionLabelTextLine> {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        return try {
            val text = Tasks.await(recognizer.process(InputImage.fromBitmap(bitmap, 0)))
            text.textBlocks.flatMap { block ->
                block.lines.mapNotNull { line ->
                    line.boundingBox?.let { bounds ->
                        NutritionLabelTextLine(
                            text = line.text,
                            left = bounds.left.toFloat(),
                            top = bounds.top.toFloat(),
                            width = bounds.width().toFloat(),
                            height = bounds.height().toFloat(),
                        )
                    }
                }
            }
        } finally {
            recognizer.close()
        }
    }

    fun tableText(lines: List<NutritionLabelTextLine>, imageHeight: Int): String {
        if (lines.isEmpty()) return ""
        val rows = mutableListOf<MutableList<NutritionLabelTextLine>>()
        lines.sortedBy(NutritionLabelTextLine::midY).forEach { line ->
            val tolerance = max(line.height * .5f, imageHeight * .005f)
            val row = rows.lastOrNull()?.takeIf { abs(it.first().midY - line.midY) <= tolerance }
            if (row == null) rows += mutableListOf(line) else row += line
        }
        return rows.joinToString("\n") { row ->
            row.sortedBy(NutritionLabelTextLine::left).joinToString("\t") { it.text }
        }
    }

    fun localMacros(lines: List<NutritionLabelTextLine>, imageHeight: Int): NutritionTargets {
        val sorted = lines.sortedBy(NutritionLabelTextLine::midY)
        fun extract(text: String): Int? = number.find(text)?.value?.replace(',', '.')?.toDoubleOrNull()?.toInt()
        fun valueFor(keywords: List<String>): Int? {
            sorted.forEachIndexed { index, line ->
                if (keywords.any { keyword -> line.text.contains(keyword, ignoreCase = true) }) {
                    extract(line.text)?.let { return it }
                    for (offset in 1..3) {
                        val next = sorted.getOrNull(index + offset) ?: break
                        if (abs(next.midY - line.midY) < imageHeight * .02f) {
                            extract(next.text)?.let { return it }
                        }
                    }
                }
            }
            return null
        }
        return NutritionTargets(
            calories = (valueFor(listOf("calories", "kcal", "energy", "energi", "valor energético", "valor energètic")) ?: 0).coerceIn(0, 8_000),
            protein = (valueFor(listOf("protein", "proteína", "proteínas", "proteinas", "proteines", "proteina")) ?: 0).coerceIn(0, 500),
            carbs = (valueFor(listOf("carbohydrate", "carbs", "glúcidos", "hidratos de carbono", "glucides", "hidrats de carboni")) ?: 0).coerceIn(0, 1_000),
            fat = (valueFor(listOf("fat", "grasas", "matières grasses", "lipides", "greixos")) ?: 0).coerceIn(0, 500),
        )
    }

    private val number = Regex("[0-9]+([.,][0-9]+)?")
}
