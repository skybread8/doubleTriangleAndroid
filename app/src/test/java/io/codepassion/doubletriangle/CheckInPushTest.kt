package io.codepassion.doubletriangle

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckInPushTest {
    @Test fun onlyClientCheckInTypesWithValidIdsOpenTheFlow() {
        val id = "a42b0a0a-4f22-4ce1-a84f-1f9e0ab98bd8"
        assertTrue(isCheckInPush("check_in_available", id))
        assertTrue(isCheckInPush("check_in_reminder", id))
        assertTrue(isCheckInPush("check_in_reviewed", id))
        assertFalse(isCheckInPush("check_in_submitted", id))
        assertFalse(isCheckInPush("check_in_available", "not-a-uuid"))
        assertFalse(isCheckInPush("check_in_available", "1-1-1-1-1"))
        assertFalse(isCheckInPush(null, id))
    }
}
