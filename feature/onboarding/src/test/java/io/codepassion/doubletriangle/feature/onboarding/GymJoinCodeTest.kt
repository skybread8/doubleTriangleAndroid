package io.codepassion.doubletriangle.feature.onboarding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GymJoinCodeTest {
    @Test fun acceptsTypedCodesAndJoinLinks() {
        listOf("wild2345", " WILD 2345\n", "https://wildforce.app/join/wild2345", "https://wildforce.app/join/WILD2345?utm=poster")
            .forEach { assertEquals("WILD2345", parseGymJoinCode(it)) }
    }

    @Test fun rejectsInvalidCodesAndUnrelatedLinks() {
        listOf("", "   ", "https://wildforce.app/dashboard", "ABCDEFGHJKLMNPQRSTUV", "https://wildforce.app/join/")
            .forEach { assertNull(parseGymJoinCode(it)) }
    }
}
