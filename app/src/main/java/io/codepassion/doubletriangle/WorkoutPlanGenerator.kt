package io.codepassion.doubletriangle

import io.codepassion.doubletriangle.core.model.ExerciseSummary
import io.codepassion.doubletriangle.core.model.UserSummary
import io.codepassion.doubletriangle.core.model.WorkoutDaySummary
import io.codepassion.doubletriangle.core.model.WorkoutHubState
import io.codepassion.doubletriangle.core.model.WorkoutStatus
import java.net.HttpURLConnection
import java.net.URL
import java.time.DayOfWeek
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

object WorkoutPlanGenerator {
    suspend fun generate(userName: String, goal: String): Pair<String, WorkoutHubState> = withContext(Dispatchers.IO) {
        check(BuildConfig.OPENAI_API_KEY.isNotBlank()) { "Falta OPENAI_API_KEY en local.properties" }
        val body = JSONObject()
            .put("model", BuildConfig.OPENAI_MODEL)
            .put("messages", JSONArray()
                .put(JSONObject().put("role", "system").put("content", SYSTEM_PROMPT))
                .put(JSONObject().put("role", "user").put("content", "Genera un plan general de iniciación de 3 días por semana, 45-55 minutos por sesión.")))
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
                val exercisesJson = day.getJSONArray("exercises")
                val exercises = buildList {
                    for (exerciseIndex in 0 until exercisesJson.length()) {
                        val exercise = exercisesJson.getJSONObject(exerciseIndex)
                        val name = exercise.getString("name")
                        val imageKey = exercise.optString("imageKey").takeIf(String::isNotBlank) ?: legacyImageKey(name)
                        add(ExerciseSummary(name, imageKey, exercise.getInt("sets").coerceIn(1, 10), exercise.getString("reps"), exercise.getInt("restSeconds").coerceIn(15, 600)))
                    }
                }
                add(WorkoutDaySummary("ai-${index + 1}", index + 1, day.getString("title"), day.getString("focus"), day.getString("dayType"), DayOfWeek.valueOf(day.getString("weekday").uppercase()), day.getInt("estimatedMinutes").coerceIn(15, 180), WorkoutStatus.Planned, exercises))
            }
        }
        check(workouts.isNotEmpty()) { "La IA devolvió un plan vacío" }
        return WorkoutHubState(UserSummary(userName, goal, 0), workouts.mapTo(mutableSetOf()) { it.scheduledDay }, emptySet(), root.optString("planName", "Plan IA · Semana 1"), root.optString("phase", "Adaptación · Mesociclo 1"), workouts)
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
    private const val SYSTEM_PROMPT = """Eres un entrenador profesional. Responde exclusivamente con JSON válido, sin markdown. Usa exactamente: {"planName":"...","phase":"...","workouts":[{"title":"...","focus":"...","dayType":"...","weekday":"MONDAY","estimatedMinutes":50,"exercises":[{"name":"...","imageKey":"benchPress","sets":3,"reps":"8-10","restSeconds":90}]}]}. weekday debe ser MONDAY, WEDNESDAY o FRIDAY. Incluye entre 4 y 6 ejercicios seguros por sesión. imageKey debe ser uno de: airSquat,gobletSquat,barbellBackSquat,walkingLunge,legPress,deadlift,romanianDeadlift,pushUp,benchPress,inclineBenchPress,overheadPress,lateralRaise,chestDip,tricepsPushdown,pullUp,latPulldown,seatedCableRow,bentOverRow,facePull,bicepsCurl,hammerCurl,plank,sidePlank,deadBug,mountainClimber."""
}