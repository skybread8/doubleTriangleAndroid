package io.codepassion.doubletriangle.feature.workout

import io.codepassion.doubletriangle.core.model.ExerciseSummary
import io.codepassion.doubletriangle.core.model.ExerciseTrackingMode
import io.codepassion.doubletriangle.core.model.RepRange
import io.codepassion.doubletriangle.core.model.WorkoutBlockType
import io.codepassion.doubletriangle.core.model.WorkoutDaySummary
import io.codepassion.doubletriangle.core.model.WorkoutStatus
import java.time.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomWorkoutBlocksTest {
    @Test fun `imported omission is merged into matching saved session without changing local targets`() {
        val localExercise = ExerciseSummary("Press", imageKey = "benchPress", sets = 4, reps = "8", restSeconds = 90)
        val block = io.codepassion.doubletriangle.core.model.WorkoutBlockSummary(WorkoutBlockType.Standard, exercises = listOf(localExercise))
        val imported = block.copy(exercises = listOf(localExercise.copy(sets = 3, skippedAt = "2026-10-28T08:00:00Z")))
        val merged = reconcileImportedSkips(listOf(block), listOf(imported)).single().exercises.single()
        assertEquals(4, merged.sets)
        assertEquals("2026-10-28T08:00:00Z", merged.skippedAt)
        assertNull(localExercise.skippedAt)
    }

    @Test fun `ambiguous or structurally changed imported exercises do not overwrite a saved session`() {
        val one = ExerciseSummary("Press", imageKey = "benchPress", sets = 1, reps = "8", restSeconds = 0)
        val block = io.codepassion.doubletriangle.core.model.WorkoutBlockSummary(WorkoutBlockType.Standard, exercises = listOf(one, one.copy()))
        val imported = block.copy(exercises = listOf(one.copy(skippedAt = "2026-10-28T08:00:00Z"), one.copy()))
        assertEquals(listOf(null, null), reconcileImportedSkips(listOf(block), listOf(imported)).single().exercises.map { it.skippedAt })
        assertEquals(listOf(block), reconcileImportedSkips(listOf(block), listOf(imported.copy(rounds = 2))))
    }

    @Test fun `planned exercise ids reconcile repeated exercises after remote reordering`() {
        val first = ExerciseSummary("Press", imageKey = "benchPress", sets = 3, reps = "8", restSeconds = 0, plannedExerciseId = "id-1")
        val second = first.copy(plannedExerciseId = "id-2")
        val local = listOf(io.codepassion.doubletriangle.core.model.WorkoutBlockSummary(WorkoutBlockType.Standard, exercises = listOf(first, second)))
        val imported = listOf(io.codepassion.doubletriangle.core.model.WorkoutBlockSummary(WorkoutBlockType.Standard, exercises = listOf(second.copy(skippedAt = "2026-10-28T08:00:00Z"), first)))
        val merged = reconcileImportedSkips(local, imported).single().exercises
        assertNull(merged[0].skippedAt)
        assertEquals("2026-10-28T08:00:00Z", merged[1].skippedAt)
    }

    @Test fun `duplicated workout clears planned exercise ids and omissions`() {
        val source = workout.copy(exercises = listOf(workout.exercises.first().copy(plannedExerciseId = "id-1", skippedAt = "2026-10-28T08:00:00Z")))
        val copied = source.asIndependentWorkoutCopy().exercises.single()
        assertTrue(copied.plannedExerciseId != null && copied.plannedExerciseId != "id-1")
        assertNull(copied.skippedAt)
        assertEquals("id-1", source.exercises.single().plannedExerciseId)
    }
    @Test fun `cancelling or duplicating clears omissions in every expanded round`() {
        val skipped = ExerciseSummary("Press", sets = 1, reps = "8", restSeconds = 0, skippedAt = "2026-10-28T08:00:00Z")
        val blocks = listOf(io.codepassion.doubletriangle.core.model.WorkoutBlockSummary(WorkoutBlockType.Superset, rounds = 2, exercises = listOf(skipped)))
        val active = workout.withEditableBlocks(blocks).copy(status = WorkoutStatus.InProgress, startedAt = "2026-10-28T08:00:00Z")
        val reset = active.resetCancelledWorkout()
        assertEquals(WorkoutStatus.Planned, reset.status)
        assertNull(reset.startedAt)
        assertEquals(listOf(null, null), reset.exercises.map { it.skippedAt })
        assertEquals(listOf(null, null), active.clearExerciseOmissions().exercises.map { it.skippedAt })
        assertEquals("2026-10-28T08:00:00Z", active.blocks.first().exercises.first().skippedAt)
    }
    @Test fun `editing a uniform rep target clears old per-set values`() {
        val edited = ExerciseSummary("Press", sets = 3, reps = "8", restSeconds = 90,
            targetRepsPerSet = listOf(8, 6, 4)).withUniformRepTarget(12)
        assertEquals("12", edited.reps)
        assertEquals(RepRange(12, 12), edited.repRange)
        assertNull(edited.targetRepsPerSet)
    }

    @Test fun `switching timed work to repetitions discards duration and distance`() {
        val edited = ExerciseSummary("Run", sets = 1, reps = "150 s", restSeconds = 0,
            targetDurationSeconds = 150, targetDistanceKm = 1.0).withRepetitionTracking()
        assertEquals(ExerciseTrackingMode.Repetitions, edited.trackingMode)
        assertEquals("10", edited.reps)
        assertNull(edited.targetDurationSeconds)
        assertNull(edited.targetDistanceKm)
    }
    private val workout = WorkoutDaySummary(
        "custom", 1, "Push", "Empuje", "Personalizado", DayOfWeek.MONDAY, 30, WorkoutStatus.Planned,
        exercises = listOf(
            ExerciseSummary("Press", sets = 4, reps = "8", restSeconds = 90),
            ExerciseSummary("Remo", sets = 3, reps = "10", restSeconds = 75),
            ExerciseSummary("Plancha", sets = 2, reps = "30 s", restSeconds = 60),
        ),
    )

    @Test fun `creates and executes an adjacent superset`() {
        val blocks = workout.editableBlocks().createSupersetAt(0)
        val edited = workout.withEditableBlocks(blocks)

        assertEquals(listOf(WorkoutBlockType.Superset, WorkoutBlockType.Standard), blocks.map { it.type })
        assertEquals(4, blocks.first().rounds)
        assertEquals(90, blocks.first().restAfterBlockSeconds)
        assertEquals(listOf("Press", "Remo", "Press", "Remo", "Press", "Remo", "Press", "Remo", "Plancha"), edited.exercises.map { it.name })
    }

    @Test fun `adding after a superset does not insert the exercise into its rounds`() {
        val original = workout.editableBlocks().createSupersetAt(0)
        val added = ExerciseSummary("Curl", sets = 3, reps = "10", restSeconds = 60)
        val edited = workout.withEditableBlocks(original.insertExerciseAfter(0, 0, added))

        assertEquals(listOf(WorkoutBlockType.Superset, WorkoutBlockType.Standard, WorkoutBlockType.Standard), edited.blocks.map { it.type })
        assertEquals(listOf("Press", "Remo", "Press", "Remo", "Press", "Remo", "Press", "Remo", "Curl", "Plancha"), edited.exercises.map { it.name })
    }

    @Test fun `execution index resolves the current block across all superset rounds`() {
        val blocks = workout.editableBlocks().createSupersetAt(0)
        assertEquals(0, blocks.blockIndexForExecution(0))
        assertEquals(0, blocks.blockIndexForExecution(7))
        assertEquals(1, blocks.blockIndexForExecution(8))
        assertNull(blocks.blockIndexForExecution(9))
    }

    @Test fun `reordering preserves results by exercise rather than position`() {
        val original = workout.editableBlocks()
        val reordered = listOf(original[1], original[0], original[2])
        val mapping = executionIndexRemap(original, reordered)
        assertEquals(mapOf(0 to 1, 1 to 0, 2 to 2), mapping)
        assertEquals(mapOf(1 to "press result", 0 to "row result"), mapOf(0 to "press result", 1 to "row result").remapExecutionIndices(mapping))
    }

    @Test fun `removing a superset exercise maps every remaining round and later block`() {
        val original = workout.editableBlocks().createSupersetAt(0)
        val changed = listOf(original.first().copy(exercises = listOf(original.first().exercises[1])), original[1])
        assertEquals(mapOf(1 to 0, 3 to 1, 5 to 2, 7 to 3, 8 to 4), executionIndexRemap(original, changed))
    }

    @Test fun `converting a remaining superset exercise combines all its recorded rounds`() {
        val original = workout.editableBlocks().createSupersetAt(0)
        val remaining = original.first().exercises[1]
        val changed = listOf(
            io.codepassion.doubletriangle.core.model.WorkoutBlockSummary(WorkoutBlockType.Standard, exercises = listOf(remaining)),
            original[1],
        )
        val mapping = executionIndexRemap(original, changed)
        assertEquals(mapOf(1 to 0, 3 to 0, 5 to 0, 7 to 0, 8 to 1), mapping)
        val stats = mapOf(1 to ExerciseSessionStats(1, 8, 20.0, 160.0), 3 to ExerciseSessionStats(1, 10, 30.0, 300.0))
            .remapAndCombineStats(mapping).getValue(0)
        assertEquals(2, stats.sets)
        assertEquals(18, stats.totalReps)
        assertEquals(460.0, stats.volumeKg, 0.0)
        val records = listOf(CompletedSetRecord(1, 1, 8, 20.0), CompletedSetRecord(3, 1, 10, 30.0)).remapSetRecords(mapping)
        assertEquals(listOf(1, 2), records.map { it.setNumber })
        assertEquals(listOf(0, 0), records.map { it.exerciseIndex })
        assertEquals(mapOf(0 to 2), mapOf(1 to 1, 3 to 1).sumRemappedExecutionIndices(mapping))
    }

    @Test fun `splitting an active superset preserves its planned sets and maps previous rounds`() {
        val original = workout.editableBlocks().createSupersetAt(0)
        val split = original.splitActiveSuperset(0)
        val mapping = executionIndexRemap(original, split, mapOf(0 to 0, 1 to 1))
        assertEquals(listOf(4, 4, 2), split.map { it.exercises.single().sets })
        assertEquals(mapOf(0 to 0, 2 to 0, 4 to 0, 6 to 0, 1 to 1, 3 to 1, 5 to 1, 7 to 1, 8 to 2), mapping)
        val stats = mapOf(0 to ExerciseSessionStats(1, 8), 2 to ExerciseSessionStats(1, 10)).remapAndCombineStats(mapping)
        assertEquals(18, stats.getValue(0).totalReps)
        assertEquals(2, stats.getValue(0).sets)
    }

    @Test fun `changing superset rounds remaps following exercises and retains removed round results`() {
        val original = workout.editableBlocks().createSupersetAt(0)
        val increased = listOf(original.first().copy(rounds = 5), original[1])
        assertEquals(10, executionIndexRemap(original, increased)[8])
        val decreased = listOf(original.first().copy(rounds = 2), original[1])
        val mapping = executionIndexRemap(original, decreased)
        assertEquals(2, mapping[6])
        assertEquals(3, mapping[7])
        assertEquals(4, mapping[8])
        val stats = mapOf(2 to ExerciseSessionStats(1, 8), 6 to ExerciseSessionStats(1, 9)).remapAndCombineStats(mapping)
        assertEquals(17, stats.getValue(2).totalReps)
    }

    @Test fun `creating active superset groups adjacent exercises in the same block with ios defaults`() {
        val exercises = workout.exercises
        val original = listOf(io.codepassion.doubletriangle.core.model.WorkoutBlockSummary(WorkoutBlockType.Standard, exercises = exercises))
        val edited = original.createActiveSuperset(1)
        assertEquals(listOf(WorkoutBlockType.Standard, WorkoutBlockType.Superset), edited.map { it.type })
        assertEquals(listOf("Remo", "Plancha"), edited[1].exercises.map { it.name })
        assertEquals(3, edited[1].rounds)
        assertEquals(75, edited[1].restAfterBlockSeconds)
        assertEquals(mapOf(0 to 0, 1 to 1, 2 to 2), executionIndexRemap(original, edited))
    }

    @Test fun `active superset creation does not combine separate blocks`() {
        val blocks = workout.editableBlocks()
        assertFalse(blocks.canCreateActiveSuperset(0))
        assertEquals(blocks, blocks.createActiveSuperset(0))
    }

    @Test fun `equal exercises remain distinct when one is removed`() {
        val first = ExerciseSummary("Press", sets = 1, reps = "8", restSeconds = 0)
        val second = first.copy()
        val original = listOf(first, second).map { io.codepassion.doubletriangle.core.model.WorkoutBlockSummary(WorkoutBlockType.Standard, exercises = listOf(it)) }
        assertEquals(mapOf(1 to 0), executionIndexRemap(original, listOf(original[1])))
    }

    @Test fun `splitting a superset restores its rounds as sets`() {
        val blocks = workout.editableBlocks().createSupersetAt(0).splitSupersetAt(0)

        assertEquals(List(3) { WorkoutBlockType.Standard }, blocks.map { it.type })
        assertEquals(listOf(4, 4, 2), blocks.map { it.exercises.single().sets })
        assertEquals(listOf(90, 90, 60), blocks.map { it.exercises.single().restSeconds })
    }

    @Test fun `splitting or removing an exercise preserves zero superset rest`() {
        val superset = workout.editableBlocks().createSupersetAt(0).let {
            listOf(it.first().copy(restAfterBlockSeconds = 0)) + it.drop(1)
        }

        assertEquals(listOf(0, 0), superset.splitSupersetAt(0).take(2).map { it.exercises.single().restSeconds })
        assertEquals(0, superset.removeBlockExercise(0, 0).first().exercises.single().restSeconds)
    }

    @Test fun `individual blocks cycle through workout sections`() {
        val original = listOf(workout.editableBlocks().first())
        val warmup = original.cycleSectionAt(0)
        val cooldown = warmup.cycleSectionAt(0)
        val standard = cooldown.cycleSectionAt(0)

        assertEquals(WorkoutBlockType.Warmup, warmup.first().type)
        assertEquals(WorkoutBlockType.Cooldown, cooldown.first().type)
        assertEquals(WorkoutBlockType.Standard, standard.first().type)
    }
}
