package io.codepassion.doubletriangle

import org.junit.Assert.assertEquals
import org.junit.Test
import io.codepassion.doubletriangle.feature.workout.exerciseDisplayName

class WorkoutQuickAccessWidgetTest {
    @Test
    fun unscheduledDayShowsRestDay() {
        val widget = fallbackWidgetWorkout(setOf("monday", "wednesday"), "tuesday")

        assertEquals("Día de descanso", widget.title)
        assertEquals("DÍA DE DESCANSO", widget.status)
        assertEquals(null, widget.progress)
    }

    @Test
    fun scheduledDayWithoutMatchingPlanShowsPlannedWorkout() {
        val widget = fallbackWidgetWorkout(setOf("monday", "wednesday"), "wednesday")

        assertEquals("Entrenamiento planeado", widget.title)
        assertEquals("PLANIFICADO", widget.status)
        assertEquals("", widget.detail)
        assertEquals(null, widget.progress)
    }

    @Test
    fun selectionMatchesIosPriorityForCompletedTodayBeforePlannedAndOldDays() {
        val selected = selectWidgetDay(
            days = listOf(
                WidgetDayCandidate("old", "TUESDAY", "COMPLETED", "2026-10-13T10:00:00Z"),
                WidgetDayCandidate("planned", "TUESDAY", "PLANNED"),
                WidgetDayCandidate("completed", "TUESDAY", "COMPLETED", "2026-10-20T10:00:00Z"),
            ),
            today = "TUESDAY",
            todayDate = java.time.LocalDate.of(2026, 10, 20),
        )

        assertEquals("completed", selected)
    }

    @Test
    fun statusBadgeOnlyAppearsForCompletedOrSkippedWorkouts() {
        assertEquals(false, widgetShowsStatusBadge("PLANIFICADO"))
        assertEquals(false, widgetShowsStatusBadge("ENTRENAMIENTO EN CURSO"))
        assertEquals(true, widgetShowsStatusBadge("COMPLETADO"))
        assertEquals(true, widgetShowsStatusBadge("OMITIDO"))
    }

    @Test
    fun widgetExerciseLabelsUseTheReadableWorkoutName() {
        assertEquals("Sentadilla libre", exerciseDisplayName("airSquat", "airSquat"))
    }

    @Test
    fun widgetWorkoutKeepsIosDayAndMetadataSeparateFromCurrentExercise() {
        val widget = WidgetWorkout(
            title = "Tren superior",
            detail = "Press banca",
            status = "ENTRENAMIENTO EN CURSO",
            progress = 50,
            dayNumber = 2,
            meta = "Empuje · 45 min",
        )

        assertEquals(2, widget.dayNumber)
        assertEquals("Empuje · 45 min", widget.meta)
        assertEquals("Press banca", widget.detail)
    }

    @Test
    fun inProgressWidgetUsesCurrentExerciseImageLikeIos() {
        val url = workoutWidgetImageUrl(
            isInProgress = true,
            currentExerciseImageKey = "airSquat",
            focus = "Piernas",
            gender = "male",
            workoutId = "day-1",
        )

        assertEquals(true, url.contains("/vertical/airSquat_male.jpeg"))
    }

    @Test
    fun plannedWidgetKeepsWorkoutCoverAndMissingExerciseImageFallsBackToCover() {
        val planned = workoutWidgetImageUrl(false, "airSquat", "Piernas", "male", "day-1")
        val activeWithoutImage = workoutWidgetImageUrl(true, null, "Piernas", "male", "day-1")

        assertEquals(true, planned.contains("/covers/"))
        assertEquals(true, activeWithoutImage.contains("/covers/"))
    }

    @Test
    fun streakWarningOnlyAppearsOnScheduledUncompletedDays() {
        assertEquals(true, widgetShowsStreakWarning(setOf("TUESDAY"), "TUESDAY", false))
        assertEquals(false, widgetShowsStreakWarning(setOf("TUESDAY"), "TUESDAY", true))
        assertEquals(false, widgetShowsStreakWarning(setOf("MONDAY"), "TUESDAY", false))
    }
}
