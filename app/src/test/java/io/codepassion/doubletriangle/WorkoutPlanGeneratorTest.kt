package io.codepassion.doubletriangle

import io.codepassion.doubletriangle.core.model.ExerciseSetStyle
import io.codepassion.doubletriangle.core.model.WorkoutBlockType
import io.codepassion.doubletriangle.core.model.WorkoutStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkoutPlanGeneratorTest {

    @Test fun generatedWorkoutIdsAreUniqueAndPersistedIdsAreKept() {
        assertEquals("saved-workout-id", WorkoutPlanGenerator.resolveWorkoutId("saved-workout-id"))
        assertNotEquals("ai-1", WorkoutPlanGenerator.resolveWorkoutId(""))
    }
    @Test
    fun `maps every AI set style and keeps legacy plans compatible`() {
        val styles = mapOf(
            "warmup" to ExerciseSetStyle.Warmup,
            "straight" to ExerciseSetStyle.Straight,
            "topSetBackoff" to ExerciseSetStyle.TopSetBackoff,
            "top_set_backoff" to ExerciseSetStyle.TopSetBackoff,
            "ascendingPyramid" to ExerciseSetStyle.AscendingPyramid,
            "dropSet" to ExerciseSetStyle.DropSet,
            "restPause" to ExerciseSetStyle.RestPause,
            "intervals" to ExerciseSetStyle.Intervals,
            "tempo" to ExerciseSetStyle.Tempo,
            "" to ExerciseSetStyle.Straight,
            "unknown" to ExerciseSetStyle.Straight,
        )

        styles.forEach { (wireValue, expected) ->
            assertEquals(expected, WorkoutPlanGenerator.parseSetStyle(wireValue))
        }
    }

    @Test
    fun `maps iOS workout block wire values`() {
        assertEquals(WorkoutBlockType.Warmup, WorkoutPlanGenerator.parseBlockType("warmup"))
        assertEquals(WorkoutBlockType.Standard, WorkoutPlanGenerator.parseBlockType("standard"))
        assertEquals(WorkoutBlockType.Superset, WorkoutPlanGenerator.parseBlockType("SUPERSET"))
        assertEquals(WorkoutBlockType.Cooldown, WorkoutPlanGenerator.parseBlockType("cooldown"))
        assertEquals(WorkoutBlockType.Standard, WorkoutPlanGenerator.parseBlockType("unknown"))
    }

    @Test
    fun `parses persisted workout statuses and defaults legacy plans to planned`() {
        assertEquals(WorkoutStatus.Draft, WorkoutPlanGenerator.parseWorkoutStatus("draft"))
        assertEquals(WorkoutStatus.Completed, WorkoutPlanGenerator.parseWorkoutStatus("Completed"))
        assertEquals(WorkoutStatus.InProgress, WorkoutPlanGenerator.parseWorkoutStatus("inProgress"))
        assertEquals(WorkoutStatus.InProgress, WorkoutPlanGenerator.parseWorkoutStatus("in_progress"))
        assertEquals(WorkoutStatus.Skipped, WorkoutPlanGenerator.parseWorkoutStatus("Skipped"))
        assertEquals(WorkoutStatus.Planned, WorkoutPlanGenerator.parseWorkoutStatus("unknown"))
        assertEquals(WorkoutStatus.Planned, WorkoutPlanGenerator.parseWorkoutStatus(""))
    }

    @Test
    fun `keeps iOS workout metadata in the Android model`() {
        val workout = io.codepassion.doubletriangle.core.model.PreviewWorkoutRepository.load().workouts.first()
            .copy(kind = "template", creationSource = "manual")

        assertEquals("template", workout.kind)
        assertEquals("manual", workout.creationSource)
    }

    @Test
    fun `normalizes literal null plan metadata`() {
        assertNull(WorkoutPlanGenerator.normalizeNullableString("null"))
        assertNull(WorkoutPlanGenerator.normalizeNullableString(" NULL "))
        assertNull(WorkoutPlanGenerator.normalizeNullableString("  "))
        assertEquals("plan notes", WorkoutPlanGenerator.normalizeNullableString(" plan notes "))
    }
}
