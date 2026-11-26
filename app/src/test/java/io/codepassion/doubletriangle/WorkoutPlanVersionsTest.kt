package io.codepassion.doubletriangle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkoutPlanVersionsTest {
    @Test fun changedRowUsesEditTimeRatherThanSyncTime() {
        assertEquals("edit-time", versionForChange("old", "new", "previous-time", "edit-time"))
    }

    @Test fun unchangedRowKeepsItsOwnVersion() {
        assertEquals("previous-time", versionForChange("same", "same", "previous-time", "another-row-edit-time"))
    }

    @Test fun unknownLegacyVersionIsNotInvented() {
        assertNull(versionForChange("same", "same", null, "current-time"))
    }
}
