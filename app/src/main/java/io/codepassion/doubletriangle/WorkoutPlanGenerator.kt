package io.codepassion.doubletriangle

import io.codepassion.doubletriangle.core.model.ExerciseSummary
import io.codepassion.doubletriangle.core.model.ExerciseSetStyle
import io.codepassion.doubletriangle.core.model.SetStyleParameters
import io.codepassion.doubletriangle.core.model.UserSummary
import io.codepassion.doubletriangle.core.model.WorkoutBlockSummary
import io.codepassion.doubletriangle.core.model.WorkoutBlockType
import io.codepassion.doubletriangle.core.model.WorkoutDaySummary
import io.codepassion.doubletriangle.core.model.WorkoutHubState
import io.codepassion.doubletriangle.core.model.WorkoutStatus
import io.codepassion.doubletriangle.core.model.executionExercises
import io.codepassion.doubletriangle.feature.workout.CustomWorkoutRequest
import java.net.HttpURLConnection
import java.net.URL
import java.time.DayOfWeek
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object WorkoutPlanGenerator {
    suspend fun generate(userName: String, goal: String): Pair<String, WorkoutHubState> = withContext(Dispatchers.IO) {
        generateWithInstruction(userName, goal, "Genera un plan general de iniciación de 3 días por semana, 45-55 minutos por sesión.")
    }

    suspend fun generateCustom(userName: String, goal: String, request: CustomWorkoutRequest): WorkoutDaySummary = withContext(Dispatchers.IO) {
        val (_, state) = generateWithInstruction(
            userName,
            goal,
            "Genera UNA ÚNICA sesión personalizada. Enfoque: ${request.focus}. Duración objetivo: ${request.durationMinutes} minutos. Equipamiento disponible: ${request.equipment}. Debe ser una sesión segura, concreta y editable.",
        )
        state.workouts.first().copy(id = "custom-ai-${UUID.randomUUID()}", order = 1, estimatedMinutes = request.durationMinutes, status = WorkoutStatus.Planned)
    }

    private suspend fun generateWithInstruction(userName: String, goal: String, instruction: String): Pair<String, WorkoutHubState> = withContext(Dispatchers.IO) {
        check(BuildConfig.OPENAI_API_KEY.isNotBlank()) { "Falta OPENAI_API_KEY en local.properties" }
        val body = JSONObject()
            .put("model", BuildConfig.OPENAI_MODEL)
            .put("messages", JSONArray()
                .put(JSONObject().put("role", "system").put("content", SYSTEM_PROMPT))
                .put(JSONObject().put("role", "user").put("content", instruction)))
            .put("response_format", JSONObject().put("type", "json_object"))
            .put("stream", false)
        val connection = URL("https://api.openai.com/v1/chat/completions").openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 30_000
            connection.readTimeout = 180_000
            connection.doOutput = true
            connection.setRequestProperty("Authorization", "Bearer ${BuildConfig.OPENAI_API_KEY}")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.outputStream.use { it.write(body.toString().toByteArray()) }
            val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            check(connection.responseCode in 200..299) {
                JSONObject(response).optJSONObject("error")?.optString("message")?.takeIf(String::isNotBlank)
                    ?: "OpenAI devolvió HTTP ${connection.responseCode}"
            }
            val planJson = JSONObject(response).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
            planJson to parse(planJson, userName, goal)
        } finally {
            connection.disconnect()
        }
    }

    fun parse(json: String, userName: String, goal: String): WorkoutHubState {
        val root = JSONObject(json)
        val daysJson = root.getJSONArray("workouts")
        val workouts = buildList {
            for (index in 0 until daysJson.length()) {
                val day = daysJson.getJSONObject(index)
                val blocks = day.optJSONArray("blocks")?.let(::parseBlocks).orEmpty()
                val exercises = if (blocks.isNotEmpty()) {
                    blocks.executionExercises()
                } else {
                    parseExercises(day.getJSONArray("exercises"))
                }
                add(WorkoutDaySummary("ai-${index + 1}", index + 1, day.getString("title"), day.getString("focus"), day.getString("dayType"), DayOfWeek.valueOf(day.getString("weekday").uppercase()), day.getInt("estimatedMinutes").coerceIn(15, 180), WorkoutStatus.Planned, exercises, blocks))
            }
        }
        check(workouts.isNotEmpty()) { "La IA devolvió un plan vacío" }
        return WorkoutHubState(UserSummary(userName, goal, 0), workouts.mapTo(mutableSetOf()) { it.scheduledDay }, emptySet(), root.optString("planName", "Plan IA · Semana 1"), root.optString("phase", "Adaptación · Mesociclo 1"), workouts)
    }
    internal fun parseSetStyle(value: String): ExerciseSetStyle = when (value.trim().lowercase()) {
        "warmup" -> ExerciseSetStyle.Warmup
        "topsetbackoff", "top_set_backoff" -> ExerciseSetStyle.TopSetBackoff
        "ascendingpyramid", "ascending_pyramid" -> ExerciseSetStyle.AscendingPyramid
        "dropset", "drop_set" -> ExerciseSetStyle.DropSet
        "restpause", "rest_pause" -> ExerciseSetStyle.RestPause
        "intervals" -> ExerciseSetStyle.Intervals
        "tempo" -> ExerciseSetStyle.Tempo
        else -> ExerciseSetStyle.Straight
    }

    internal fun parseBlockType(value: String): WorkoutBlockType = when (value.trim().lowercase()) {
        "warmup" -> WorkoutBlockType.Warmup
        "superset" -> WorkoutBlockType.Superset
        "cooldown" -> WorkoutBlockType.Cooldown
        else -> WorkoutBlockType.Standard
    }

    private fun parseBlocks(json: JSONArray): List<WorkoutBlockSummary> = buildList {
        for (index in 0 until json.length()) {
            val block = json.getJSONObject(index)
            val type = parseBlockType(block.optString("type"))
            add(
                WorkoutBlockSummary(
                    type = type,
                    rounds = block.optInt("rounds", 1).coerceIn(1, 10),
                    restAfterBlockSeconds = block.optInt("restAfterBlockSeconds").takeIf { it > 0 }?.coerceIn(15, 600),
                    notes = block.optString("notes").takeIf(String::isNotBlank),
                    exercises = parseExercises(block.getJSONArray("exercises")).map {
                        if (type == WorkoutBlockType.Superset) it.copy(sets = 1) else it
                    },
                ),
            )
        }
    }

    private fun parseExercises(json: JSONArray): List<ExerciseSummary> = buildList {
        for (index in 0 until json.length()) add(parseExercise(json.getJSONObject(index)))
    }

    private fun parseExercise(exercise: JSONObject): ExerciseSummary {
        val name = exercise.getString("name")
        val imageKey = exercise.optString("imageKey").takeIf(String::isNotBlank) ?: legacyImageKey(name)
        val details = exercise.optJSONObject("setStyleParameters") ?: JSONObject()
        val parameters = SetStyleParameters(
            dropCount = details.optInt("dropCount", 2).coerceIn(1, 5),
            dropWeightPercent = details.optInt("dropWeightPercent", 20).coerceIn(5, 50),
            backoffSetCount = details.optInt("backoffSetCount", 3).coerceIn(1, 6),
            backoffWeightPercent = details.optInt("backoffWeightPercent", 15).coerceIn(5, 50),
            intraSetRestSeconds = details.optInt("intraSetRestSeconds", 15).coerceIn(5, 120),
            tempo = details.optString("tempo", "3-1-1-0").take(9),
            targetRir = details.optInt("targetRir", 2).coerceIn(0, 5),
        )
        return ExerciseSummary(
            name = name,
            imageKey = imageKey,
            sets = exercise.optInt("sets", 1).coerceIn(1, 10),
            reps = exercise.getString("reps"),
            restSeconds = exercise.optInt("restSeconds", 60).coerceIn(0, 600),
            setStyle = parseSetStyle(exercise.optString("setStyle")),
            setStyleParameters = parameters,
        )
    }


    private fun legacyImageKey(name: String): String? {
        val value = name.lowercase()
        return when {
            "banca" in value || "bench press" in value -> "benchPress"
            "militar" in value || "overhead press" in value -> "overheadPress"
            "inclinado" in value || "incline" in value -> "inclineBenchPress"
            "fondos" in value || "dip" in value -> "chestDip"
            "sentadilla" in value || "squat" in value -> "barbellBackSquat"
            "rumano" in value || "romanian" in value -> "romanianDeadlift"
            "peso muerto" in value || "deadlift" in value -> "deadlift"
            "zancada" in value || "lunge" in value -> "walkingLunge"
            "dominada" in value || "pull-up" in value || "pull up" in value -> "pullUp"
            "remo" in value || "row" in value -> "bentOverRow"
            "bíceps" in value || "biceps" in value || "curl" in value -> "bicepsCurl"
            "plancha" in value || "plank" in value -> "plank"
            "flexion" in value || "flexión" in value || "push-up" in value -> "pushUp"
            else -> null
        }
    }
    private const val SYSTEM_PROMPT = """Eres un entrenador profesional. Responde exclusivamente con JSON válido, sin markdown. Usa: {"planName":"...","phase":"...","workouts":[{"title":"...","focus":"...","dayType":"...","weekday":"MONDAY","estimatedMinutes":50,"blocks":[{"type":"warmup","rounds":1,"notes":"...","exercises":[{"name":"...","imageKey":"airSquat","sets":1,"reps":"10","restSeconds":0,"setStyle":"warmup"}]},{"type":"superset","rounds":3,"restAfterBlockSeconds":90,"exercises":[{"name":"...","imageKey":"benchPress","sets":1,"reps":"8-10","restSeconds":0,"setStyle":"straight","setStyleParameters":{"targetRir":2}},{"name":"...","imageKey":"seatedCableRow","sets":1,"reps":"10-12","restSeconds":0,"setStyle":"straight"}]},{"type":"cooldown","rounds":1,"exercises":[{"name":"...","imageKey":"sidePlank","sets":1,"reps":"45 s","restSeconds":0,"setStyle":"warmup"}]}]}]}. type debe ser warmup, standard, superset o cooldown. Usa bloques standard para ejercicios individuales y superset para 2 ejercicios alternados; rounds contiene las rondas y cada ejercicio de una superserie tiene sets=1. Incluye calentamiento breve, trabajo principal y vuelta a la calma. setStyle debe ser warmup, straight, topSetBackoff, ascendingPyramid, dropSet, restPause, intervals o tempo. Incluye setStyleParameters solo con valores relevantes. weekday debe ser MONDAY, WEDNESDAY o FRIDAY. Incluye entre 4 y 7 ejercicios seguros por sesión y usa segundos en reps (por ejemplo 45 s) para ejercicios temporizados. imageKey debe ser uno de: airSquat,gobletSquat,barbellBackSquat,walkingLunge,legPress,deadlift,romanianDeadlift,pushUp,benchPress,inclineBenchPress,overheadPress,lateralRaise,chestDip,tricepsPushdown,pullUp,latPulldown,seatedCableRow,bentOverRow,facePull,bicepsCurl,hammerCurl,plank,sidePlank,deadBug,mountainClimber."""
}
