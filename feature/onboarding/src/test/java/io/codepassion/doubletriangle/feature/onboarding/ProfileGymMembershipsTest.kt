package io.codepassion.doubletriangle.feature.onboarding

import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileGymMembershipsTest {
    @Test fun usesBookableGymsWhenMembershipListIsUnavailable() {
        val gym = WildforceGymMembership("active", "Team", "Gym")
        assertEquals(listOf(gym), displayedGymMemberships(emptyList(), listOf(gym)))
    }

    @Test fun keepsActualMembershipStatusAndHomeGymWhenPresent() {
        val membership = WildforceGymMembership("suspended", "Team", "Home")
        val bookable = WildforceGymMembership("active", "Other", "Other gym")
        assertEquals(listOf(membership), displayedGymMemberships(listOf(membership), listOf(bookable)))
    }
}
