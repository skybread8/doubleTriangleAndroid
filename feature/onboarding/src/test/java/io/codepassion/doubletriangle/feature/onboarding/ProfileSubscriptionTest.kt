package io.codepassion.doubletriangle.feature.onboarding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProfileSubscriptionTest {
    private fun record(user: String, updated: String, plan: String, deleted: Boolean = false) =
        ProfileSubscriptionRecord(user, updated, deleted, ProfileSubscription(plan, "active", "internal", false, null))

    @Test fun selectsLatestLiveSubscriptionForCurrentAccount() {
        val records = listOf(
            record("other", "2026-05-01", "other"),
            record("USER", "2026-01-01", "trial"),
            record("user", "2026-03-01", "premium"),
            record("user", "2026-04-01", "deleted", deleted = true),
        )
        assertEquals("premium", selectProfileSubscription(records, "user")?.plan)
    }

    @Test fun doesNotShowAnotherAccountOrDeletedSubscription() {
        assertNull(selectProfileSubscription(listOf(record("other", "2026-01-01", "premium")), "user"))
        assertNull(selectProfileSubscription(listOf(record("user", "2026-01-01", "trial", deleted = true)), "user"))
    }
}
