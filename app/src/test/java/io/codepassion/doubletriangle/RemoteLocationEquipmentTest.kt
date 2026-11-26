package io.codepassion.doubletriangle

import io.codepassion.doubletriangle.feature.onboarding.Equipment
import org.junit.Assert.assertEquals
import org.junit.Test

class RemoteLocationEquipmentTest {
    @Test fun explicitEmptyDoesNotInheritAnotherLocationsEquipment() {
        assertEquals(setOf(Equipment.Bodyweight), resolveRemoteLocationEquipment(emptySet(), setOf(Equipment.Dumbbells)))
    }

    @Test fun preservesRemoteSelectionAndUsesFallbackOnlyWhenMissing() {
        val remote = setOf(Equipment.Dumbbells, Equipment.CableMachine)
        assertEquals(remote, resolveRemoteLocationEquipment(remote, setOf(Equipment.Bodyweight)))
        assertEquals(remote, resolveRemoteLocationEquipment(null, remote))
    }
}
