package io.codepassion.doubletriangle.feature.workout

import android.content.Context
import io.codepassion.doubletriangle.core.model.ExerciseSummary
import io.codepassion.doubletriangle.core.model.ExerciseSetStyle
import io.codepassion.doubletriangle.core.model.ExerciseTrackingMode
import io.codepassion.doubletriangle.core.model.RepRange
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

    fun automatic(day: DayOfWeek, request: CustomWorkoutRequest = CustomWorkoutRequest("Full body", 45, "Peso corporal")): WorkoutDaySummary {
        val main = listOf(
            ExerciseSummary("Sentadilla goblet", "gobletSquat", 3, "10-12", 90),
            ExerciseSummary("Press de banca", "benchPress", 3, "8-10", 90),
            ExerciseSummary("Remo con barra", "bentOverRow", 3, "10", 75),
            ExerciseSummary("Peso muerto rumano", "romanianDeadlift", 3, "8-10", 90),
            ExerciseSummary("Plancha", "plank", 3, "45 s", 60, targetDurationSeconds = 45),
        )
        val blocks = buildList {
            if (request.includeWarmup) add(WorkoutBlockSummary(WorkoutBlockType.Warmup, exercises = listOf(ExerciseSummary("Movilidad articular", "catCow", 1, "5 min", 0, ExerciseSetStyle.Warmup, targetDurationMinutes = 5))))
            addAll(main.map { WorkoutBlockSummary(WorkoutBlockType.Standard, exercises = listOf(it)) })
            if (request.includeCooldown) add(WorkoutBlockSummary(WorkoutBlockType.Cooldown, exercises = listOf(ExerciseSummary("Vuelta a la calma", "thoracicRotation", 1, "5 min", 0, ExerciseSetStyle.Warmup, targetDurationMinutes = 5))))
        }
        return empty(day).copy(
            title = "${request.focus} personalizado", focus = request.focus, estimatedMinutes = request.durationMinutes,
            blocks = blocks, exercises = blocks.executionExercises(),
        )
    }

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
                        appliesToFinalSetOnly = parameters.optBoolean("appliesToFinalSetOnly", false),
                    ),
                    targetWeightKg = exercise.optDoubleOrNull("targetWeightKg"),
                    targetRepsPerSet = exercise.optIntList("targetRepsPerSet"),
                    targetWeightsKg = exercise.optDoubleList("targetWeightsKg"),
                    targetDurationSeconds = exercise.optIntOrNull("targetDurationSeconds"),
                    targetDurationMinutes = exercise.optIntOrNull("targetDurationMinutes"),
                    targetDistanceKm = exercise.optDoubleOrNull("targetDistanceKm"),
                    isPerSideLoad = exercise.optBoolean("isPerSideLoad", false),
                    trackingMode = runCatching { ExerciseTrackingMode.valueOf(exercise.optString("trackingMode")) }.getOrElse {
                        when {
                            exercise.optDoubleOrNull("targetDistanceKm") != null -> ExerciseTrackingMode.DurationAndDistance
                            exercise.optIntOrNull("targetDurationSeconds") != null || exercise.optIntOrNull("targetDurationMinutes") != null -> ExerciseTrackingMode.Duration
                            else -> ExerciseTrackingMode.Repetitions
                        }
                    },
                    repRange = exercise.optIntOrNull("repsMin")?.let { minimum -> RepRange(minimum, (exercise.optIntOrNull("repsMax") ?: minimum).coerceAtLeast(minimum)) },
                ),
            )
        }
    }

    private fun encodeExercise(exercise: ExerciseSummary): JSONObject {
        val parameters = JSONObject()
            .put("dropCount", exercise.setStyleParameters.dropCount).put("dropWeightPercent", exercise.setStyleParameters.dropWeightPercent)
            .put("backoffSetCount", exercise.setStyleParameters.backoffSetCount).put("backoffWeightPercent", exercise.setStyleParameters.backoffWeightPercent)
            .put("intraSetRestSeconds", exercise.setStyleParameters.intraSetRestSeconds).put("tempo", exercise.setStyleParameters.tempo)
            .put("targetRir", exercise.setStyleParameters.targetRir).put("appliesToFinalSetOnly", exercise.setStyleParameters.appliesToFinalSetOnly)
        return JSONObject().put("name", exercise.name).put("imageKey", exercise.imageKey ?: "").put("sets", exercise.sets)
            .put("reps", exercise.reps).put("restSeconds", exercise.restSeconds).put("setStyle", exercise.setStyle.name)
            .put("setStyleParameters", parameters)
            .put("targetWeightKg", exercise.targetWeightKg)
            .put("targetRepsPerSet", JSONArray(exercise.targetRepsPerSet.orEmpty()))
            .put("targetWeightsKg", JSONArray(exercise.targetWeightsKg.orEmpty()))
            .put("targetDurationSeconds", exercise.targetDurationSeconds)
            .put("targetDurationMinutes", exercise.targetDurationMinutes)
            .put("targetDistanceKm", exercise.targetDistanceKm)
            .put("isPerSideLoad", exercise.isPerSideLoad)
            .put("trackingMode", exercise.trackingMode.name)
            .put("repsMin", exercise.repRange?.minimum)
            .put("repsMax", exercise.repRange?.maximum)
    }
}

private fun JSONObject.optIntOrNull(name: String): Int? = takeIf { has(name) && !isNull(name) }?.optInt(name)
private fun JSONObject.optDoubleOrNull(name: String): Double? = takeIf { has(name) && !isNull(name) }?.optDouble(name)
private fun JSONObject.optIntList(name: String): List<Int>? = optJSONArray(name)?.let { array -> List(array.length()) { array.optInt(it) } }
private fun JSONObject.optDoubleList(name: String): List<Double>? = optJSONArray(name)?.let { array -> List(array.length()) { array.optDouble(it) } }
private fun JSONObject.stringList(name: String): List<String> = optJSONArray(name)?.let { array ->
    List(array.length()) { index -> array.optString(index) }.filter(String::isNotBlank)
}.orEmpty()

/**
 * A picker-ready projection of the canonical exercise catalog.
 *
 * Keep the fields that define replacement grouping here instead of maintaining a
 * second, partial Android-only map. This is the same source data used by iOS.
 */
internal data class ExerciseChoice(
    val name: String,
    val imageKey: String,
    val substitutionCandidates: List<String> = emptyList(),
    val primaryMuscles: Set<String> = emptySet(),
)

/** Mirrors `SwapExerciseView` on iOS: candidates first, then exercises with a
 * shared primary muscle, followed by every remaining exercise. */
internal data class ExerciseReplacementSections(
    val recommendedSubstitutes: List<ExerciseChoice>,
    val sameMuscleGroup: List<ExerciseChoice>,
    val allOtherExercises: List<ExerciseChoice>,
) {
    val all: List<ExerciseChoice>
        get() = recommendedSubstitutes + sameMuscleGroup + allOtherExercises
}

internal fun exerciseReplacementSections(
    catalog: List<ExerciseChoice>,
    replacing: ExerciseSummary,
): ExerciseReplacementSections {
    val currentKey = replacing.imageKey?.trim()
    val current = catalog.firstOrNull { it.imageKey.equals(currentKey, ignoreCase = true) }
    val candidates = current?.substitutionCandidates
        .orEmpty()
        .mapNotNull { key -> catalog.firstOrNull { it.imageKey.equals(key, ignoreCase = true) } }
    val candidateKeys = candidates.map { it.imageKey.lowercase() }.toSet()
    val currentPrimaryMuscles = current?.primaryMuscles.orEmpty()
    val remaining = catalog.filterNot { choice ->
        choice.imageKey.equals(currentKey, ignoreCase = true) || choice.imageKey.lowercase() in candidateKeys
    }
    val sameMuscleGroup = remaining.filter { choice ->
        choice.primaryMuscles.intersect(currentPrimaryMuscles).isNotEmpty()
    }.sortedBy { it.name }
    val sameKeys = sameMuscleGroup.map { it.imageKey.lowercase() }.toSet()
    val allOtherExercises = remaining.filterNot { it.imageKey.lowercase() in sameKeys }.sortedBy { it.name }
    return ExerciseReplacementSections(candidates, sameMuscleGroup, allOtherExercises)
}

/** Retained for callers outside the picker; its order is iOS section order. */
internal fun rankExerciseReplacements(
    catalog: List<ExerciseChoice>,
    replacing: ExerciseSummary,
): List<ExerciseChoice> = exerciseReplacementSections(catalog, replacing).all

internal object CustomExerciseCatalog {
    private val spanishNames = mapOf(
        "airSquat" to "Sentadilla libre", "gobletSquat" to "Sentadilla goblet", "barbellBackSquat" to "Sentadilla con barra",
        "benchPress" to "Press de banca", "inclineBenchPress" to "Press inclinado", "dumbbellInclineBenchPress" to "Press inclinado con mancuernas", "pushUp" to "Flexiones",
        "pullUp" to "Dominadas", "dumbbellRow" to "Remo con mancuerna", "bentOverRow" to "Remo con barra",
        "deadlift" to "Peso muerto", "romanianDeadlift" to "Peso muerto rumano", "walkingLunge" to "Zancadas",
        "overheadPress" to "Press militar", "bicepsCurl" to "Curl de bíceps", "chestDip" to "Fondos", "plank" to "Plancha",
    )

    fun load(context: Context): List<ExerciseChoice> = runCatching {
        val catalog = JSONArray(context.resources.openRawResource(R.raw.exercise_catalog).bufferedReader().use { it.readText() })
        List(catalog.length()) { index ->
            val item = catalog.getJSONObject(index)
            val key = item.getString("id")
            ExerciseChoice(
                name = spanishNames[key] ?: item.optString("name").ifBlank { humanize(key) },
                imageKey = key,
                substitutionCandidates = item.stringList("substitutionCandidates"),
                primaryMuscles = item.stringList("primaryMuscles").toSet(),
            )
        }.sortedBy { it.name }
    }.getOrDefault(spanishNames.map { ExerciseChoice(it.value, it.key) }.sortedBy { it.name })

    private fun humanize(value: String): String = value.replace(Regex("([a-z])([A-Z])"), "$1 $2").replaceFirstChar { it.uppercase() }
}
