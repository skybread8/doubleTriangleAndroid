package io.codepassion.doubletriangle.nutrition

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NutritionPlanRecordVersionsTest {
    @Test fun keepsUnchangedRowVersionAndUsesActualEditTimeForChangedRow() {
        assertEquals("old", nutritionPlanRecordVersion("same", "same", "old", "new"))
        assertEquals("new", nutritionPlanRecordVersion("before", "after", "old", "new"))
        assertEquals("new", nutritionPlanRecordVersion(null, "created", null, "new"))
        assertNull(nutritionPlanRecordVersion("same", "same", null, "new"))
    }

    @Test fun restoresRemotePreAndPostWorkoutMealTypes() {
        assertEquals(MealType.PreWorkout, mealTypeFromRemote("preWorkout"))
        assertEquals(MealType.PostWorkout, mealTypeFromRemote("postWorkout"))
    }
}
