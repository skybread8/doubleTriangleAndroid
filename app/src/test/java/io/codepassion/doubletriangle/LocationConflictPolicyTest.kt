package io.codepassion.doubletriangle

import io.codepassion.doubletriangle.feature.onboarding.Equipment
import io.codepassion.doubletriangle.feature.onboarding.TrainingLocationProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationConflictPolicyTest {
    private val older = "2026-01-01T00:00:00Z"
    private val newer = "2026-01-02T00:00:00Z"
    private fun location(id: String, name: String, time: String = older) =
        TrainingLocationProfile(name, setOf(Equipment.Bodyweight), id = id, updatedAt = time)

    @Test fun eachLocationResolvesIndependently() {
        val a = location("a", "Local A", newer)
        val b = location("b", "Local B")
        val result = mergeLocationVersions(listOf(a, b), listOf(
            RemoteLocationVersion("a", older, location("a", "Old A"), 0),
            RemoteLocationVersion("b", newer, location("b", "Remote B", newer), 1),
        ), emptyMap())
        assertEquals(listOf("Local A", "Remote B"), result.locations.map { it.name })
    }

    @Test fun deletionAndRestorationUseTheSameTimestampRule() {
        val remote = location("a", "Gym", newer)
        assertTrue(mergeLocationVersions(emptyList(), listOf(RemoteLocationVersion("a", older, remote, 0)), mapOf("a" to newer)).locations.isEmpty())
        val restored = mergeLocationVersions(emptyList(), listOf(RemoteLocationVersion("a", newer, remote, 0)), mapOf("a" to older))
        assertEquals(listOf(remote), restored.locations)
        assertEquals(setOf("a"), restored.resolvedDeletions)
        assertTrue(mergeLocationVersions(listOf(location("a", "Gym")), listOf(RemoteLocationVersion("a", newer, null, 0)), emptyMap()).locations.isEmpty())
    }

    @Test fun localChangeRetainsIdAndDatesOnlyChangedRecord() {
        val a = location("a", "A")
        val b = location("b", "B")
        val result = stampLocationChanges(listOf(a, b), listOf(a, b.copy(name = "B changed")), newer)
        assertEquals(older, result.locations[0].updatedAt)
        assertEquals(newer, result.locations[1].updatedAt)
        assertEquals("b", result.locations[1].id)
    }

    @Test fun equalRemoteTimestampDoesNotOverrideLocalVersion() {
        val local = location("a", "Local", newer)
        val remote = location("a", "Remote", newer)
        assertEquals(listOf(local), mergeLocationVersions(listOf(local), listOf(RemoteLocationVersion("a", newer, remote, 0)), emptyMap()).locations)
    }

    @Test fun removingOneLocationDoesNotReuseTheSurvivorId() {
        val a = location("a", "A")
        val b = location("b", "B")
        val result = stampLocationChanges(listOf(a, b), listOf(b), newer)
        assertEquals(listOf("b"), result.locations.map { it.id })
        assertEquals(setOf("a"), result.deletedIds)
    }

    @Test fun unknownLegacyVersionIsNotOverwritten() {
        val local = location("a", "Legacy").copy(updatedAt = null)
        val remote = location("a", "Remote", newer)
        assertEquals(listOf(local), mergeLocationVersions(listOf(local), listOf(RemoteLocationVersion("a", newer, remote, 0)), emptyMap()).locations)
    }
}
