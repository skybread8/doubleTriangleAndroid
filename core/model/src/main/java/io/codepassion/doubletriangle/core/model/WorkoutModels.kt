package io.codepassion.doubletriangle.core.model

import java.time.DayOfWeek

data class UserSummary(val name: String, val goal: String, val currentStreak: Int)

enum class WorkoutMode(val label: String) {
    Plan("Tu plan"),
    Custom("Entrenamientos personalizados"),
}

enum class WorkoutStatus { Planned, Completed, Skipped }

enum class ExerciseSetStyle(val label: String, val glyph: String) {
    Warmup("Calentamiento", "●"),
    Straight("Series normales", "="),
    TopSetBackoff("Top set + backoff", "▲"),
    AscendingPyramid("Pirámide ascendente", "△"),
    DropSet("Drop set", "↘"),
    RestPause("Rest-pause", "Ⅱ"),
    Intervals("Intervalos", "◷"),
    Tempo("Tempo", "♩"),
}

data class SetStyleParameters(
    val dropCount: Int = 2,
    val dropWeightPercent: Int = 20,
    val backoffSetCount: Int = 3,
    val backoffWeightPercent: Int = 15,
    val intraSetRestSeconds: Int = 15,
    val tempo: String = "3-1-1-0",
    val targetRir: Int = 2,
)

enum class WorkoutBlockType(val label: String) {
    Warmup("Calentamiento"),
    Standard("Bloque principal"),
    Superset("Superserie"),
    Cooldown("Vuelta a la calma"),
}

data class ExerciseSummary(
    val name: String,
    val imageKey: String? = null,
    val sets: Int,
    val reps: String,
    val restSeconds: Int,
    val setStyle: ExerciseSetStyle = ExerciseSetStyle.Straight,
    val setStyleParameters: SetStyleParameters = SetStyleParameters(),
    val blockType: WorkoutBlockType = WorkoutBlockType.Standard,
    val blockLabel: String? = null,
    val blockRound: Int = 1,
    val blockRounds: Int = 1,
    val isLastInBlock: Boolean = true,
    val restAfterBlockSeconds: Int? = null,
    val targetWeightKg: Double? = null,
)

fun ExerciseSetStyle.appliesToSet(setNumber: Int, totalSets: Int): Boolean =
    this !in setOf(ExerciseSetStyle.DropSet, ExerciseSetStyle.RestPause) || setNumber == totalSets

data class WorkoutBlockSummary(
    val type: WorkoutBlockType,
    val rounds: Int = 1,
    val restAfterBlockSeconds: Int? = null,
    val notes: String? = null,
    val exercises: List<ExerciseSummary> = emptyList(),
)

fun WorkoutBlockSummary.executionExercises(): List<ExerciseSummary> {
    val safeRounds = rounds.coerceAtLeast(1)
    return if (type == WorkoutBlockType.Superset) {
        List(safeRounds) { roundIndex ->
            exercises.mapIndexed { exerciseIndex, exercise ->
                exercise.copy(
                    sets = 1,
                    blockType = type,
                    blockLabel = "A${exerciseIndex + 1}",
                    blockRound = roundIndex + 1,
                    blockRounds = safeRounds,
                    isLastInBlock = exerciseIndex == exercises.lastIndex,
                    restAfterBlockSeconds = restAfterBlockSeconds,
                )
            }
        }.flatten()
    } else {
        exercises.mapIndexed { index, exercise ->
            exercise.copy(
                blockType = type,
                blockRound = 1,
                blockRounds = safeRounds,
                isLastInBlock = index == exercises.lastIndex,
                restAfterBlockSeconds = restAfterBlockSeconds,
            )
        }
    }
}

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
    val blocks: List<WorkoutBlockSummary> = emptyList(),
)

fun WorkoutDaySummary.displayBlocks(): List<WorkoutBlockSummary> =
    blocks.ifEmpty { listOf(WorkoutBlockSummary(WorkoutBlockType.Standard, exercises = exercises)) }

fun List<WorkoutBlockSummary>.executionExercises(): List<ExerciseSummary> =
    flatMap(WorkoutBlockSummary::executionExercises)

data class WorkoutHubState(
    val user: UserSummary,
    val trainingDays: Set<DayOfWeek>,
    val completedDays: Set<DayOfWeek>,
    val planName: String,
    val phase: String,
    val workouts: List<WorkoutDaySummary>,
    val mesocycleIndex: Int = 1,
    val cycleLength: Int = 1,
    val weekIndex: Int = 1,
)

fun WorkoutHubState.workoutsFor(dayOfWeek: DayOfWeek?): List<WorkoutDaySummary> =
    dayOfWeek?.let { selected -> workouts.filter { it.scheduledDay == selected } } ?: workouts

object PreviewWorkoutRepository {
    fun load(name: String = "Jordi Puigdellivol", goal: String = "Ganar músculo"): WorkoutHubState {
        val pushBlocks = listOf(
            WorkoutBlockSummary(
                type = WorkoutBlockType.Warmup,
                notes = "Activa hombros, escápulas y patrón de empuje.",
                exercises = listOf(ExerciseSummary("Flexiones", "pushUp", 1, "12", 0, ExerciseSetStyle.Warmup)),
            ),
            WorkoutBlockSummary(
                type = WorkoutBlockType.Standard,
                exercises = listOf(ExerciseSummary("Press de banca", "benchPress", 4, "8-10", 120)),
            ),
            WorkoutBlockSummary(
                type = WorkoutBlockType.Superset,
                rounds = 3,
                restAfterBlockSeconds = 90,
                notes = "Alterna A1 y A2 sin descanso entre ejercicios.",
                exercises = listOf(
                    ExerciseSummary("Press militar", "overheadPress", 1, "10", 0),
                    ExerciseSummary("Face pull", "facePull", 1, "12-15", 0),
                ),
            ),
            WorkoutBlockSummary(
                type = WorkoutBlockType.Cooldown,
                notes = "Respira lento y recupera movilidad.",
                exercises = listOf(ExerciseSummary("Plancha lateral", "sidePlank", 1, "30 s", 0, ExerciseSetStyle.Warmup)),
            ),
        )
        return WorkoutHubState(
        user = UserSummary(name, goal, 4),
        trainingDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY),
        completedDays = setOf(DayOfWeek.MONDAY),
        planName = "Hipertrofia · Semana 1",
        phase = "Acumulación · Mesociclo 1",
        workouts = listOf(
            WorkoutDaySummary("push", 1, "PUSH · TREN SUPERIOR", "Empuje", "Fuerza", DayOfWeek.MONDAY, 55, WorkoutStatus.Completed, pushBlocks.executionExercises(), pushBlocks),
            WorkoutDaySummary("legs", 2, "PIERNAS Y CORE", "Piernas", "Fuerza", DayOfWeek.WEDNESDAY, 60, WorkoutStatus.Planned, listOf(ExerciseSummary("Sentadilla", "barbellBackSquat", 4, "6-8", 150), ExerciseSummary("Peso muerto rumano", "romanianDeadlift", 3, "8-10", 120), ExerciseSummary("Plancha", "plank", 3, "45 s", 60))),
            WorkoutDaySummary("pull", 3, "PULL · ESPALDA", "Tirón", "Hipertrofia", DayOfWeek.FRIDAY, 50, WorkoutStatus.Planned, listOf(ExerciseSummary("Dominadas", "pullUp", 4, "6-10", 120), ExerciseSummary("Remo con barra", "bentOverRow", 4, "8-10", 120), ExerciseSummary("Curl de bíceps", "bicepsCurl", 3, "10-12", 75))),
            WorkoutDaySummary("full", 4, "CUERPO COMPLETO", "Full body", "Fuerza", DayOfWeek.SATURDAY, 65, WorkoutStatus.Planned, listOf(ExerciseSummary("Peso muerto", "deadlift", 3, "5", 180), ExerciseSummary("Press inclinado", "inclineBenchPress", 3, "8", 120), ExerciseSummary("Zancadas", "walkingLunge", 3, "10/lado", 90))),
        ),
        )
    }
}
