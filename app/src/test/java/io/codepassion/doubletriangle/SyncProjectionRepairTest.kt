package io.codepassion.doubletriangle

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncProjectionRepairTest {
    @Test fun missingProjectionWithCursorRequiresFullPull() {
        assertTrue(needsFullProjectionRepair(hasUserWatermark = true, userRecordCount = 0))
    }

    @Test fun firstPullAndIntactProjectionKeepTheirCursors() {
        assertFalse(needsFullProjectionRepair(hasUserWatermark = false, userRecordCount = 0))
        assertFalse(needsFullProjectionRepair(hasUserWatermark = true, userRecordCount = 1))
    }
}
