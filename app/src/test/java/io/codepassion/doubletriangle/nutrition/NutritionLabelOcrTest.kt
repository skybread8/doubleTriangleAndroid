package io.codepassion.doubletriangle.nutrition

import org.junit.Assert.assertEquals
import org.junit.Test

class NutritionLabelOcrTest {
    private val lines = listOf(
        NutritionLabelTextLine("Energy", 8f, 10f, 90f, 20f),
        NutritionLabelTextLine("389 kcal", 180f, 10f, 100f, 20f),
        NutritionLabelTextLine("Protein", 8f, 40f, 90f, 20f),
        NutritionLabelTextLine("13.5 g", 180f, 40f, 100f, 20f),
        NutritionLabelTextLine("Carbohydrate", 8f, 70f, 120f, 20f),
        NutritionLabelTextLine("58,7 g", 180f, 70f, 100f, 20f),
        NutritionLabelTextLine("Fat", 8f, 100f, 90f, 20f),
        NutritionLabelTextLine("7 g", 180f, 100f, 100f, 20f),
    )

    @Test fun `serializes OCR into the iOS table layout`() {
        assertEquals(
            "Energy\t389 kcal\nProtein\t13.5 g\nCarbohydrate\t58,7 g\nFat\t7 g",
            NutritionLabelOcr.tableText(lines, imageHeight = 400),
        )
    }

    @Test fun `parses macros locally when AI is unavailable`() {
        assertEquals(NutritionTargets(389, 13, 58, 7), NutritionLabelOcr.localMacros(lines, imageHeight = 400))
    }
}
