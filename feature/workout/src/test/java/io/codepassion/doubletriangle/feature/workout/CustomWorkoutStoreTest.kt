package io.codepassion.doubletriangle.feature.workout

import io.codepassion.doubletriangle.core.model.ExerciseSummary
import io.codepassion.doubletriangle.core.model.ExerciseTrackingMode
import io.codepassion.doubletriangle.core.model.WorkoutStatus
import java.time.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomWorkoutStoreTest {
    @Test fun durationIncludesWorkAndRestForEverySet() {
        val exercises = listOf(
            ExerciseSummary("Press", "benchPress", 3, "10", 90),
            ExerciseSummary("Plancha", "plank", 2, "45 s", 60),
        )
        assertEquals(11, CustomWorkoutStore.estimateMinutes(exercises))
    }

    @Test fun emptyWorkoutHasZeroDuration() {
        assertEquals(0, CustomWorkoutStore.estimateMinutes(emptyList()))
    }

    @Test fun manualCreationStartsAsDraftAndEmptyDraftIsDiscardable() {
        val draft = CustomWorkoutStore.empty(DayOfWeek.MONDAY)

        assertEquals(WorkoutStatus.Draft, draft.status)
        assertEquals("manual", draft.creationSource)
        assertFalse(CustomWorkoutStore.shouldRetainManualDraft(draft))
    }

    @Test fun manualDraftWithExercisesIsRetainedUntilConfirmed() {
        val draft = CustomWorkoutStore.empty(DayOfWeek.MONDAY).copy(
            blocks = listOf(
                io.codepassion.doubletriangle.core.model.WorkoutBlockSummary(
                    io.codepassion.doubletriangle.core.model.WorkoutBlockType.Standard,
                    exercises = listOf(ExerciseSummary("Press", "benchPress", 3, "10", 90)),
                ),
            ),
        )

        assertTrue(CustomWorkoutStore.shouldRetainManualDraft(draft))
    }

    @Test fun pickerKeepsCoachExerciseReferenceWhenBuildingWorkout() {
        val selected = ExerciseChoice(
            name = "Remo del entrenador",
            imageKey = "coach:4ad4b47d-6d5d-4c33-a428-f06a8a77e8dd",
            coachExerciseId = "4ad4b47d-6d5d-4c33-a428-f06a8a77e8dd",
            trackingMode = ExerciseTrackingMode.Duration,
            isPerSideLoad = true,
        ).toExerciseSummary()

        assertEquals("4ad4b47d-6d5d-4c33-a428-f06a8a77e8dd", selected.coachExerciseId)
        assertEquals(ExerciseTrackingMode.Duration, selected.trackingMode)
        assertTrue(selected.isPerSideLoad)
    }
}
