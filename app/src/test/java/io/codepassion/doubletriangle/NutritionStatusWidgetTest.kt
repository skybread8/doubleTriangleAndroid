package io.codepassion.doubletriangle

import io.codepassion.doubletriangle.nutrition.NutritionTargets
import org.junit.Assert.assertEquals
import org.junit.Test

class NutritionStatusWidgetTest {
    @Test
    fun `widget has no target when there is no current nutrition day`() {
        val target = nutritionWidgetTargets(null)

        assertEquals(NutritionTargets(0, 0, 0, 0), target)
    }

    @Test
    fun `widget preserves the current planned day target`() {
        val planned = NutritionTargets(2_400, 170, 280, 70)

        assertEquals(planned, nutritionWidgetTargets(planned))
    }
}
