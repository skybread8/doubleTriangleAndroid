package io.codepassion.doubletriangle.core.model

import java.time.DayOfWeek

data class UserSummary(val name: String, val goal: String, val currentStreak: Int)

enum class WorkoutMode(val label: String) {
    Plan("Tu plan"),
    Custom("Entrenamientos personalizados"),
}

enum class WorkoutStatus { Planned, Completed }

data class WorkoutDaySummary(
    val id: String,
    val order: Int,
    val title: String,
    val focus: String,
    val dayType: String,
    val scheduledDay: DayOfWeek,
    val estimatedMinutes: Int,
    val status: WorkoutStatus,
)

data class WorkoutHubState(
    val user: UserSummary,
    val trainingDays: Set<DayOfWeek>,
    val completedDays: Set<DayOfWeek>,
    val planName: String,
    val phase: String,
    val workouts: List<WorkoutDaySummary>,
)

fun WorkoutHubState.workoutsFor(dayOfWeek: DayOfWeek?): List<WorkoutDaySummary> =
    dayOfWeek?.let { selected -> workouts.filter { it.scheduledDay == selected } } ?: workouts

object PreviewWorkoutRepository {
    fun load(name: String = "Jordi Puigdellivol", goal: String = "Ganar músculo") = WorkoutHubState(
        user = UserSummary(name, goal, 4),
        trainingDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY),
        completedDays = setOf(DayOfWeek.MONDAY),
        planName = "Hipertrofia · Semana 1",
        phase = "Acumulación · Mesociclo 1",
        workouts = listOf(
            WorkoutDaySummary("push", 1, "PUSH · TREN SUPERIOR", "Empuje", "Fuerza", DayOfWeek.MONDAY, 55, WorkoutStatus.Completed),
            WorkoutDaySummary("legs", 2, "PIERNAS Y CORE", "Piernas", "Fuerza", DayOfWeek.WEDNESDAY, 60, WorkoutStatus.Planned),
            WorkoutDaySummary("pull", 3, "PULL · ESPALDA", "Tirón", "Hipertrofia", DayOfWeek.FRIDAY, 50, WorkoutStatus.Planned),
            WorkoutDaySummary("full", 4, "CUERPO COMPLETO", "Full body", "Fuerza", DayOfWeek.SATURDAY, 65, WorkoutStatus.Planned),
        ),
    )
}
