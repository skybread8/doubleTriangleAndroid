package io.codepassion.doubletriangle.feature.workout

import io.codepassion.doubletriangle.core.model.ExerciseSummary
import io.codepassion.doubletriangle.core.model.ExerciseTrackingMode
import io.codepassion.doubletriangle.core.model.RepRange
import io.codepassion.doubletriangle.core.model.WorkoutBlockSummary
import io.codepassion.doubletriangle.core.model.WorkoutBlockType
import io.codepassion.doubletriangle.core.model.WorkoutStatus
import java.time.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomWorkoutStoreTest {
    @Test fun durationUsesTypedTargetsForRepetitionsAndTimedWork() {
        val exercises = listOf(
            ExerciseSummary("Press", sets = 3, reps = "20", restSeconds = 90, repRange = RepRange(15, 20)),
            ExerciseSummary("Plancha", sets = 2, reps = "45 s", restSeconds = 60, targetDurationSeconds = 45),
            ExerciseSummary("Carrera", sets = 3, reps = "10 min", restSeconds = 0, targetDurationMinutes = 10, targetDistanceKm = 2.0),
        )
        assertEquals(16, CustomWorkoutStore.estimateMinutes(exercises))
    }

    @Test fun supersetCountsInterExerciseRestEachRoundAndBlockRestBetweenRounds() {
        val block = WorkoutBlockSummary(
            WorkoutBlockType.Superset, rounds = 3, restAfterBlockSeconds = 60,
            exercises = listOf(
                ExerciseSummary("A", sets = 1, reps = "12", restSeconds = 15, targetRepsPerSet = listOf(12)),
                ExerciseSummary("B", sets = 1, reps = "30 s", restSeconds = 90, targetDurationSeconds = 30),
            ),
        )
        assertEquals(7, CustomWorkoutStore.estimateBlockMinutes(listOf(block)))
        assertEquals(5, CustomWorkoutStore.estimateBlockMinutes(listOf(block.copy(restAfterBlockSeconds = 0))))
    }

    @Test fun distanceExerciseUsesSecondsWhenEditedOnAndroid() {
        val exercise = ExerciseSummary("Carrera", sets = 1, reps = "", restSeconds = 0,
            targetDurationSeconds = 150, targetDistanceKm = 1.0)
        assertEquals(3, CustomWorkoutStore.estimateMinutes(listOf(exercise)))
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
        assertEquals(30, selected.resolvedTargetDurationSeconds())
        assertTrue(selected.isPerSideLoad)
    }

    @Test fun selectingTheSameExerciseTwiceCreatesDistinctPlannedIdentities() {
        val choice = catalogExerciseChoice("Press", "barbellBenchPress")
        val first = choice.toExerciseSummary()
        val second = choice.toExerciseSummary()
        assertTrue(first.plannedExerciseId != null)
        assertTrue(second.plannedExerciseId != null)
        assertTrue(first.plannedExerciseId != second.plannedExerciseId)
    }

    @Test fun timedCoachChoicesHaveUsableIosDefaults() {
        val minutes = ExerciseChoice("Movilidad", "coach:minutes", trackingMode = ExerciseTrackingMode.Duration, durationUsesMinutes = true).toExerciseSummary()
        val distance = ExerciseChoice("Carrera", "coach:distance", trackingMode = ExerciseTrackingMode.DurationAndDistance).toExerciseSummary()
        assertEquals(1, minutes.sets)
        assertEquals(180, minutes.resolvedTargetDurationSeconds())
        assertEquals(1, distance.sets)
        assertEquals(1200, distance.resolvedTargetDurationSeconds())
        assertEquals(1.0, distance.targetDistanceKm!!, 0.0)
    }

    @Test fun catalogChoicesUseIosTrackingModesAndDefaults() {
        val plank = catalogExerciseChoice("Plancha", "plank").toExerciseSummary()
        val mobility = catalogExerciseChoice("Movilidad", "catCow").toExerciseSummary()
        val running = catalogExerciseChoice("Carrera", "outdoorRunning").toExerciseSummary()
        val squat = catalogExerciseChoice("Sentadilla", "airSquat").toExerciseSummary()
        assertEquals(45, plank.resolvedTargetDurationSeconds())
        assertEquals(180, mobility.resolvedTargetDurationSeconds())
        assertEquals(1200, running.resolvedTargetDurationSeconds())
        assertEquals(ExerciseTrackingMode.DurationAndDistance, running.trackingMode)
        assertEquals(ExerciseTrackingMode.Repetitions, squat.trackingMode)
    }

    @Test fun swappingSameTrackingModeKeepsPrescriptionAndClearsLoad() {
        val original = ExerciseSummary("Press", sets = 4, reps = "8-10", restSeconds = 75,
            repRange = RepRange(8, 10), targetRepsPerSet = listOf(8, 9, 10, 8),
            targetWeightKg = 30.0, targetWeightsKg = listOf(30.0, 32.0))
        val swapped = original.swappedTo(catalogExerciseChoice("Remo", "barbellBentOverRow"))
        assertEquals(4, swapped.sets)
        assertEquals(listOf(8, 9, 10, 8), swapped.targetRepsPerSet)
        assertEquals(75, swapped.restSeconds)
        assertEquals(null, swapped.targetWeightKg)
        assertEquals(null, swapped.targetWeightsKg)
    }

    @Test fun swappingTrackingModeCreatesCompatibleTargets() {
        val original = ExerciseSummary("Press", sets = 3, reps = "10", restSeconds = 90,
            repRange = RepRange(10, 10), targetRepsPerSet = listOf(10, 10, 10))
        val timed = original.swappedTo(catalogExerciseChoice("Plancha", "plank"))
        assertEquals(ExerciseTrackingMode.Duration, timed.trackingMode)
        assertEquals(30, timed.resolvedTargetDurationSeconds())
        assertEquals(null, timed.repRange)
        assertEquals(null, timed.targetRepsPerSet)
        val repetitions = timed.swappedTo(catalogExerciseChoice("Sentadilla", "airSquat"))
        assertEquals(RepRange(10, 12), repetitions.repRange)
        assertEquals(null, repetitions.targetDurationSeconds)
    }
}
