package io.codepassion.doubletriangle.feature.workout

import android.content.Context
import io.codepassion.doubletriangle.core.model.ExerciseSummary
import io.codepassion.doubletriangle.core.model.ExerciseSetStyle
import io.codepassion.doubletriangle.core.model.SetStyleParameters
import io.codepassion.doubletriangle.core.model.WorkoutBlockSummary
import io.codepassion.doubletriangle.core.model.WorkoutBlockType
import io.codepassion.doubletriangle.core.model.WorkoutDaySummary
import io.codepassion.doubletriangle.core.model.WorkoutStatus
import io.codepassion.doubletriangle.core.model.executionExercises
import java.time.DayOfWeek
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

/** Persistent collection used by the custom-workout list and detail editor. */
object CustomWorkoutStore {
    private const val PREFERENCES = "wildforce_custom_workouts"
    private const val KEY = "workouts"

    fun load(context: Context): List<WorkoutDaySummary> = runCatching {
        val raw = context.getSharedPreferences(PREFERENCES, 0).getString(KEY, null) ?: return emptyList()
        val array = JSONArray(raw)
        buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                val blocks = item.optJSONArray("blocks")?.let(::parseBlocks).orEmpty()
                val legacyExercises = item.optJSONArray("exercises")?.let(::parseExercises).orEmpty()
                val exercises = if (blocks.isEmpty()) legacyExercises else blocks.executionExercises()
                add(
                    WorkoutDaySummary(
                        id = item.getString("id"), order = item.optInt("order", index + 1), title = item.getString("title"),
                        focus = item.optString("focus", "Full body"), dayType = "Personalizado",
                        scheduledDay = runCatching { DayOfWeek.valueOf(item.getString("scheduledDay")) }.getOrDefault(DayOfWeek.MONDAY),
                        estimatedMinutes = item.optInt("estimatedMinutes", estimateMinutes(exercises)), status = WorkoutStatus.Planned, exercises = exercises, blocks = blocks,
                    ),
                )
            }
        }
    }.getOrDefault(emptyList())

    fun save(context: Context, workout: WorkoutDaySummary) {
        val workouts = (load(context).filterNot { it.id == workout.id } + workout)
            .mapIndexed { index, item -> item.copy(order = index + 1) }
        write(context, workouts)
    }

    fun delete(context: Context, workoutId: String) = write(context, load(context).filterNot { it.id == workoutId })

    fun duplicate(context: Context, source: WorkoutDaySummary): WorkoutDaySummary {
        val copy = source.copy(id = "custom-${UUID.randomUUID()}", title = "${source.title} (copia)", order = load(context).size + 1, status = WorkoutStatus.Planned)
        save(context, copy)
        return copy
    }

    fun empty(day: DayOfWeek): WorkoutDaySummary = WorkoutDaySummary(
        id = "custom-${UUID.randomUUID()}", order = 1, title = "Entrenamiento personalizado", focus = "Full body", dayType = "Personalizado",
        scheduledDay = day, estimatedMinutes = 0, status = WorkoutStatus.Planned,
    )

    fun automatic(day: DayOfWeek): WorkoutDaySummary = empty(day).copy(
        title = "Full body personalizado", estimatedMinutes = 45,
        exercises = listOf(
            ExerciseSummary("Sentadilla goblet", "gobletSquat", 3, "10-12", 90),
            ExerciseSummary("Press de banca", "benchPress", 3, "8-10", 90),
            ExerciseSummary("Remo con barra", "bentOverRow", 3, "10", 75),
            ExerciseSummary("Peso muerto rumano", "romanianDeadlift", 3, "8-10", 90),
            ExerciseSummary("Plancha", "plank", 3, "45 s", 60),
        ),
    )

    fun estimateMinutes(exercises: List<ExerciseSummary>): Int = exercises.sumOf { exercise ->
        exercise.sets.coerceAtLeast(1) * (45 + exercise.restSeconds.coerceAtLeast(0))
    }.let { seconds -> if (seconds == 0) 0 else (seconds + 59) / 60 }

    fun estimateBlockMinutes(blocks: List<WorkoutBlockSummary>): Int = blocks.sumOf { block ->
        if (block.type == WorkoutBlockType.Superset) {
            block.rounds.coerceAtLeast(1) * block.exercises.size * 45 +
                (block.rounds.coerceAtLeast(1) - 1) * (block.restAfterBlockSeconds ?: 0)
        } else {
            block.exercises.sumOf { it.sets.coerceAtLeast(1) * (45 + it.restSeconds.coerceAtLeast(0)) }
        }
    }.let { seconds -> if (seconds == 0) 0 else (seconds + 59) / 60 }

    private fun write(context: Context, workouts: List<WorkoutDaySummary>) {
        val array = JSONArray().apply {
            workouts.sortedBy { it.order }.forEach { workout ->
                val exercises = JSONArray().apply {
                    workout.exercises.forEach { put(encodeExercise(it)) }
                }
                val blocks = JSONArray().apply {
                    workout.blocks.forEach { block ->
                        put(
                            JSONObject()
                                .put("type", block.type.name)
                                .put("rounds", block.rounds)
                                .put("restAfterBlockSeconds", block.restAfterBlockSeconds ?: 0)
                                .put("notes", block.notes ?: "")
                                .put("exercises", JSONArray().apply { block.exercises.forEach { put(encodeExercise(it)) } }),
                        )
                    }
                }
                put(
                    JSONObject().put("id", workout.id).put("order", workout.order).put("title", workout.title).put("focus", workout.focus)
                        .put("scheduledDay", workout.scheduledDay.name).put("estimatedMinutes", workout.estimatedMinutes).put("exercises", exercises).put("blocks", blocks),
                )
            }
        }
        context.getSharedPreferences(PREFERENCES, 0).edit().putString(KEY, array.toString()).apply()
    }

    private fun parseBlocks(json: JSONArray): List<WorkoutBlockSummary> = buildList {
        for (index in 0 until json.length()) {
            val block = json.getJSONObject(index)
            add(
                WorkoutBlockSummary(
                    type = runCatching { WorkoutBlockType.valueOf(block.optString("type")) }.getOrDefault(WorkoutBlockType.Standard),
                    rounds = block.optInt("rounds", 1).coerceAtLeast(1),
                    restAfterBlockSeconds = block.optInt("restAfterBlockSeconds").takeIf { it > 0 },
                    notes = block.optString("notes").takeIf(String::isNotBlank),
                    exercises = parseExercises(block.optJSONArray("exercises") ?: JSONArray()),
                ),
            )
        }
    }

    private fun parseExercises(json: JSONArray): List<ExerciseSummary> = buildList {
        for (index in 0 until json.length()) {
            val exercise = json.getJSONObject(index)
            val parameters = exercise.optJSONObject("setStyleParameters") ?: JSONObject()
            add(
                ExerciseSummary(
                    exercise.getString("name"), exercise.optString("imageKey").takeIf(String::isNotBlank),
                    exercise.getInt("sets"), exercise.getString("reps"), exercise.getInt("restSeconds"),
                    runCatching { ExerciseSetStyle.valueOf(exercise.optString("setStyle")) }.getOrDefault(ExerciseSetStyle.Straight),
                    SetStyleParameters(
                        dropCount = parameters.optInt("dropCount", 2), dropWeightPercent = parameters.optInt("dropWeightPercent", 20),
                        backoffSetCount = parameters.optInt("backoffSetCount", 3), backoffWeightPercent = parameters.optInt("backoffWeightPercent", 15),
                        intraSetRestSeconds = parameters.optInt("intraSetRestSeconds", 15), tempo = parameters.optString("tempo", "3-1-1-0"),
                        targetRir = parameters.optInt("targetRir", 2),
                    ),
                ),
            )
        }
    }

    private fun encodeExercise(exercise: ExerciseSummary): JSONObject {
        val parameters = JSONObject()
            .put("dropCount", exercise.setStyleParameters.dropCount).put("dropWeightPercent", exercise.setStyleParameters.dropWeightPercent)
            .put("backoffSetCount", exercise.setStyleParameters.backoffSetCount).put("backoffWeightPercent", exercise.setStyleParameters.backoffWeightPercent)
            .put("intraSetRestSeconds", exercise.setStyleParameters.intraSetRestSeconds).put("tempo", exercise.setStyleParameters.tempo)
            .put("targetRir", exercise.setStyleParameters.targetRir)
        return JSONObject().put("name", exercise.name).put("imageKey", exercise.imageKey ?: "").put("sets", exercise.sets)
            .put("reps", exercise.reps).put("restSeconds", exercise.restSeconds).put("setStyle", exercise.setStyle.name)
            .put("setStyleParameters", parameters)
    }
}

internal data class ExerciseChoice(val name: String, val imageKey: String)

internal object CustomExerciseCatalog {
    private val spanishNames = mapOf(
        "airSquat" to "Sentadilla libre", "gobletSquat" to "Sentadilla goblet", "barbellBackSquat" to "Sentadilla con barra",
        "benchPress" to "Press de banca", "inclineBenchPress" to "Press inclinado", "pushUp" to "Flexiones",
        "pullUp" to "Dominadas", "dumbbellRow" to "Remo con mancuerna", "bentOverRow" to "Remo con barra",
        "deadlift" to "Peso muerto", "romanianDeadlift" to "Peso muerto rumano", "walkingLunge" to "Zancadas",
        "overheadPress" to "Press militar", "bicepsCurl" to "Curl de bíceps", "chestDip" to "Fondos", "plank" to "Plancha",
    )

    fun load(context: Context): List<ExerciseChoice> = runCatching {
        val root = JSONObject(context.resources.openRawResource(R.raw.exercise_guide_es).bufferedReader().use { it.readText() })
        root.keys().asSequence().map { key -> ExerciseChoice(spanishNames[key] ?: humanize(key), key) }.sortedBy { it.name }.toList()
    }.getOrDefault(spanishNames.map { ExerciseChoice(it.value, it.key) }.sortedBy { it.name })

    private fun humanize(value: String): String = value.replace(Regex("([a-z])([A-Z])"), "$1 $2").replaceFirstChar { it.uppercase() }
}
