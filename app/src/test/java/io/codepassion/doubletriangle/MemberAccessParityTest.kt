package io.codepassion.doubletriangle

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MemberAccessParityTest {
    @Test fun gymMemberKeepsWorkoutsAndAnalyticsWhenSyncRequiresSubscription() {
        assertFalse(showsMandatoryAccessPaywall(subscriptionRequired = true, member = true))
        assertFalse(showsMemberNutritionOffer(true, RootDestination.Workout))
        assertFalse(showsMemberNutritionOffer(true, RootDestination.Analytics))
        assertTrue(showsMemberNutritionOffer(true, RootDestination.Nutrition))
    }

    @Test fun nonMemberStillReceivesMandatoryPaywall() {
        assertTrue(showsMandatoryAccessPaywall(subscriptionRequired = true, member = false))
        assertFalse(showsMemberNutritionOffer(false, RootDestination.Nutrition))
    }
}
