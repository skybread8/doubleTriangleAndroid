package io.codepassion.doubletriangle.core.model

import java.time.DayOfWeek

data class UserSummary(val name: String, val goal: String, val currentStreak: Int)

enum class WorkoutMode(val label: String) {
    Plan("Tu plan"),
    Custom("Entrenamientos personalizados"),
}

enum class WorkoutStatus { Planned, Completed }

data class ExerciseSummary(
    val name: String,
    val imageKey: String? = null,
    val sets: Int,
    val reps: String,
    val restSeconds: Int,
)

data class WorkoutDaySummary(
    val id: String,
    val order: Int,
    val title: String,
    val focus: String,
    val dayType: String,
    val scheduledDay: DayOfWeek,
    val estimatedMinutes: Int,
    val status: WorkoutStatus,
    val exercises: List<ExerciseSummary> = emptyList(),
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
            WorkoutDaySummary("push", 1, "PUSH · TREN SUPERIOR", "Empuje", "Fuerza", DayOfWeek.MONDAY, 55, WorkoutStatus.Completed, listOf(ExerciseSummary("Press de banca", "benchPress", 4, "8-10", 120), ExerciseSummary("Press militar", "overheadPress", 3, "10", 90), ExerciseSummary("Fondos", "chestDip", 3, "8-12", 90))),
            WorkoutDaySummary("legs", 2, "PIERNAS Y CORE", "Piernas", "Fuerza", DayOfWeek.WEDNESDAY, 60, WorkoutStatus.Planned, listOf(ExerciseSummary("Sentadilla", "barbellBackSquat", 4, "6-8", 150), ExerciseSummary("Peso muerto rumano", "romanianDeadlift", 3, "8-10", 120), ExerciseSummary("Plancha", "plank", 3, "45 s", 60))),
            WorkoutDaySummary("pull", 3, "PULL · ESPALDA", "Tirón", "Hipertrofia", DayOfWeek.FRIDAY, 50, WorkoutStatus.Planned, listOf(ExerciseSummary("Dominadas", "pullUp", 4, "6-10", 120), ExerciseSummary("Remo con barra", "bentOverRow", 4, "8-10", 120), ExerciseSummary("Curl de bíceps", "bicepsCurl", 3, "10-12", 75))),
            WorkoutDaySummary("full", 4, "CUERPO COMPLETO", "Full body", "Fuerza", DayOfWeek.SATURDAY, 65, WorkoutStatus.Planned, listOf(ExerciseSummary("Peso muerto", "deadlift", 3, "5", 180), ExerciseSummary("Press inclinado", "inclineBenchPress", 3, "8", 120), ExerciseSummary("Zancadas", "walkingLunge", 3, "10/lado", 90))),
        ),
    )
}
