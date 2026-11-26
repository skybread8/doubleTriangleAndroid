package io.codepassion.doubletriangle

import org.junit.Assert.assertEquals
import org.junit.Test

class SyncBatchRequestTest {
    @Test fun firstPullOmitsOptionalWatermarkLikeIos() {
        assertEquals(mapOf("resource" to "workout-plans"), syncBatchResource("workout-plans", null))
    }

    @Test fun subsequentPullIncludesTheSavedWatermark() {
        assertEquals(mapOf("resource" to "workout-plans", "updated_after" to "2026-11-25T10:00:00Z"), syncBatchResource("workout-plans", "2026-11-25T10:00:00Z"))
    }

    @Test fun gymProgramsAreFullyRefreshedToDetectLostMembership() {
        assertEquals(mapOf("resource" to "gym-programs"), syncBatchResource("gym-programs", "2026-11-25T10:00:00Z"))
        assertEquals(true, replaceSyncProjection("gym-programs", true))
        assertEquals(false, replaceSyncProjection("gym-programs", false))
        assertEquals(false, replaceSyncProjection("workout-days", true))
    }
}
