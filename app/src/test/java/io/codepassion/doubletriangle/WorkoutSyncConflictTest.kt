package io.codepassion.doubletriangle

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutSyncConflictTest {
    @Test fun serverTimestampDecidesWhetherAWriteWasRejected() {
        assertTrue(serverHasNewerWorkoutRecord("2026-11-14T10:00:00Z", "2026-11-14T10:00:00.001Z"))
        assertFalse(serverHasNewerWorkoutRecord("2026-11-14T10:00:00Z", "2026-11-14T10:00:00Z"))
        assertFalse(serverHasNewerWorkoutRecord("2026-11-14T10:00:01Z", "2026-11-14T10:00:00Z"))
    }
}
