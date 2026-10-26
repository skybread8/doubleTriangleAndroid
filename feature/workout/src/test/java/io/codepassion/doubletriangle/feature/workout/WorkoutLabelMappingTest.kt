package io.codepassion.doubletriangle.feature.workout

import org.junit.Assert.assertEquals
import org.junit.Test

class WorkoutLabelMappingTest {
    @Test
    fun mapsPlanStatusesToExistingIosCatalogKeys() {
        assertEquals("active", canonicalWorkoutLabelKey("active"))
        assertEquals("completed", canonicalWorkoutLabelKey("Completed"))
        assertEquals("archived", canonicalWorkoutLabelKey("archivado"))
        assertEquals("draft", canonicalWorkoutLabelKey("borrador"))
    }

    @Test
    fun onlyCompletedPlansUseTheHistoryCheckmark() {
        assertEquals(true, isCompletedPlanStatus("completed"))
        assertEquals(true, isCompletedPlanStatus("COMPLETED"))
        assertEquals(false, isCompletedPlanStatus("archived"))
        assertEquals(false, isCompletedPlanStatus(null))
    }
}
