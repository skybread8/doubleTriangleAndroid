package io.codepassion.doubletriangle.nutrition

import org.junit.Assert.assertEquals
import org.junit.Test

class NutritionLoggingMethodsTest {
    @Test
    fun exposesAllIosLoggingMethods() {
        assertEquals(
            listOf("Barcode", "Describe", "Photo", "Recent", "Label", "Search", "Manual"),
            LogMethod.entries.map(LogMethod::name),
        )
    }

    @Test
    fun matchesIosAiNutritionAccessBoundary() {
        assertEquals(true, LogMethod.Describe.requiresAiNutritionAccess())
        assertEquals(true, LogMethod.Photo.requiresAiNutritionAccess())
        assertEquals(false, LogMethod.Barcode.requiresAiNutritionAccess())
        assertEquals(false, LogMethod.Label.requiresAiNutritionAccess())
        assertEquals(false, LogMethod.Manual.requiresAiNutritionAccess())
    }

}
