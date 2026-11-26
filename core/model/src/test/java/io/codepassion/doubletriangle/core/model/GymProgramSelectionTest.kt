package io.codepassion.doubletriangle.core.model

import java.time.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Test

class GymProgramSelectionTest {
    @Test fun modesFollowIosFeatureAndProgramAvailability() {
        assertEquals(listOf(WorkoutMode.Plan, WorkoutMode.Gym, WorkoutMode.Custom), availableWorkoutModes(true, true))
        assertEquals(listOf(WorkoutMode.Plan, WorkoutMode.Custom), availableWorkoutModes(false, true))
        assertEquals(listOf(WorkoutMode.Gym, WorkoutMode.Custom), availableWorkoutModes(true, false))
        assertEquals(listOf(WorkoutMode.Plan, WorkoutMode.Custom), availableWorkoutModes(false, false))
    }

    @Test fun gymTitlesSortNumbersNaturally() {
        val titles = listOf("Session 10", "Session 2", "Session 1")
        assertEquals(listOf("Session 1", "Session 2", "Session 10"), titles.sortedWith(::compareGymTitles))
    }
    @Test fun selectsNewestLiveActiveProgram() {
        val programs = listOf(
            GymProgramSummary("old", "Old", "active", "2026-11-24T00:00:00Z"),
            GymProgramSummary("paused", "Paused", "paused", "2026-11-27T00:00:00Z"),
            GymProgramSummary("deleted", "Deleted", "active", "2026-11-28T00:00:00Z", deleted = true),
            GymProgramSummary("current", "Current", "active", "2026-11-25T00:00:00Z"),
        )
        assertEquals("current", activeGymProgram(programs)?.id)
    }

    @Test fun selectsOnePendingSessionPerTitleAndPrefersInProgress() {
        fun workout(id: String, title: String, status: WorkoutStatus) = WorkoutDaySummary(
            id, 0, title, "", "", DayOfWeek.MONDAY, 30, status,
        )
        val rows = listOf(
            GymProgramWorkout("gym", workout("planned", "Strength", WorkoutStatus.Planned), "2026-11-25T00:00:00Z"),
            GymProgramWorkout("gym", workout("started", "Strength", WorkoutStatus.InProgress), "2026-11-24T00:00:00Z"),
            GymProgramWorkout("gym", workout("complete", "Strength", WorkoutStatus.Completed), "2026-11-26T00:00:00Z"),
            GymProgramWorkout("other", workout("other", "Cardio", WorkoutStatus.Planned), "2026-11-26T00:00:00Z"),
        )
        assertEquals(listOf("started"), gymProgramWorkouts("gym", rows).map { it.id })
    }

    @Test fun pausedAndArchivedProgramsAreNotAvailable() {
        val programs = listOf(
            GymProgramSummary("paused", "Paused", "paused", "2026-11-25T00:00:00Z"),
            GymProgramSummary("old", "Old", "archived", "2026-11-26T00:00:00Z"),
        )
        assertEquals(null, activeGymProgram(programs))
    }

    @Test fun ignoresSkippedAndDeletedSessionsAndChoosesNewestPending() {
        fun row(id: String, status: WorkoutStatus, updated: String, deleted: Boolean = false) = GymProgramWorkout(
            "gym", WorkoutDaySummary(id, 0, "Session", "", "", DayOfWeek.MONDAY, 30, status), updated, deleted,
        )
        val rows = listOf(
            row("old", WorkoutStatus.Planned, "2026-11-24T00:00:00Z"),
            row("new", WorkoutStatus.Planned, "2026-11-25T00:00:00Z"),
            row("skipped", WorkoutStatus.Skipped, "2026-11-26T00:00:00Z"),
            row("deleted", WorkoutStatus.Planned, "2026-11-27T00:00:00Z", deleted = true),
        )
        assertEquals(listOf("new"), gymProgramWorkouts("gym", rows).map { it.id })
    }
}
