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
import io.codepassion.doubletriangle.feature.onboarding.OnboardingProfile
import java.net.HttpURLConnection
import java.net.URL
import java.time.DayOfWeek
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object WorkoutPlanGenerator {
    suspend fun generate(profile: OnboardingProfile): Pair<String, WorkoutHubState> = withContext(Dispatchers.IO) {
        generateWithInstruction(profile, "Genera un plan semanal progresivo respetando exactamente los días seleccionados, el objetivo, nivel, restricciones, equipamiento y duración del perfil.")
    }

    suspend fun generateCustom(profile: OnboardingProfile, request: CustomWorkoutRequest): WorkoutDaySummary = withContext(Dispatchers.IO) {
        val (_, state) = generateWithInstruction(
            profile,
            "Genera UNA ÚNICA sesión personalizada. Enfoque: ${request.focus}. Duración objetivo: ${request.durationMinutes} minutos. Equipamiento disponible para esta sesión: ${request.equipment}. Devuelve un objeto de día dentro de `workouts` y aplica las mismas reglas de bloques, prescripciones y seguridad que el plan semanal.",
        )
        state.workouts.first().copy(id = "custom-ai-${UUID.randomUUID()}", order = 1, estimatedMinutes = request.durationMinutes, status = WorkoutStatus.Planned)
    }

    private suspend fun generateWithInstruction(profile: OnboardingProfile, instruction: String): Pair<String, WorkoutHubState> = withContext(Dispatchers.IO) {
        check(BuildConfig.OPENAI_API_KEY.isNotBlank()) { "Falta OPENAI_API_KEY en local.properties" }
        val body = JSONObject()
            .put("model", BuildConfig.OPENAI_MODEL)
            .put("messages", JSONArray()
                .put(JSONObject().put("role", "system").put("content", systemPrompt(profile)))
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
            planJson to parse(planJson, profile.name, profile.goal.title)
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
                val weekday = day.optString("weekday", day.optString("intendedWeekday", "MONDAY"))
                    .uppercase().let { value -> runCatching { DayOfWeek.valueOf(value) }.getOrDefault(DayOfWeek.MONDAY) }
                val estimatedMinutes = day.optInt("estimatedMinutes", day.optInt("estimatedDurationMinutes", 50))
                add(WorkoutDaySummary("ai-${index + 1}", index + 1, day.optString("title", "Sesión ${index + 1}"), day.optString("focus", "Fitness general"), day.optString("dayType", "strength"), weekday, estimatedMinutes.coerceIn(15, 180), WorkoutStatus.Planned, exercises, blocks))
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

    private fun systemPrompt(profile: OnboardingProfile): String = SYSTEM_PROMPT + """

Perfil obligatorio del usuario:
- Nombre: ${profile.name}; objetivo: ${profile.goal.title}; nivel: ${profile.trainingLevel.title}; estilo de vida: ${profile.lifestyle.title}
- Días disponibles: ${profile.workoutDays.joinToString { it.storedValue }}; duración preferida: ${profile.preferredWorkoutDurationMinutes} minutos
- Estructura: ${profile.trainingSplitPreference.title}; focos personalizados: ${profile.customWorkoutFocuses.entries.joinToString { "${it.key.storedValue}=${it.value.storedValue}" }.ifBlank { "ninguno" }}
- Equipamiento: ${profile.availableEquipment.joinToString { it.storedValue }}; restricciones: ${profile.movementRestrictions.joinToString { it.storedValue }.ifBlank { "ninguna" }}
- Composición corporal: ${profile.bodyCompositionPhase?.storedValue ?: "no especificada"}; edad aproximada: ${(java.time.Year.now().value - profile.birthYear).coerceAtLeast(13)}; altura: ${profile.heightCm} cm; peso: ${profile.weightKg} kg
Reglas: crea exactamente un workout por cada día disponible, no inventes días, no uses ejercicios incompatibles con equipamiento/restricciones y mantén el volumen dentro de la duración indicada. Usa siempre bloques y prescripciones concretas.
"""

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
        val name = exercise.optString("name", exercise.optString("exercise", "Ejercicio"))
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
            reps = exercise.optString("reps").ifBlank {
                val min = exercise.optInt("repsMin", 0)
                val max = exercise.optInt("repsMax", min)
                when {
                    min > 0 && max > 0 && min != max -> "$min-$max"
                    min > 0 -> min.toString()
                    exercise.has("targetDurationSeconds") -> "${exercise.optInt("targetDurationSeconds")} s"
                    else -> "8-12"
                }
            },
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
    private const val SYSTEM_PROMPT = """Eres un entrenador profesional y el planificador de Wildforce. Responde exclusivamente con JSON válido, sin markdown. Usa esta forma: {"planName":"...","phase":"...","workouts":[{"title":"...","focus":"...","dayType":"strength|hypertrophy|technique|volume|deload|recovery|conditioning","weekday":"MONDAY","estimatedDurationMinutes":50,"blocks":[{"type":"warmup|standard|superset|cooldown","orderIndex":0,"rounds":1,"restAfterBlockSeconds":90,"notes":"...","exercises":[{"name":"...","imageKey":"benchPress","sets":3,"reps":"8-12","repsMin":8,"repsMax":12,"targetWeightKg":20,"restSeconds":90,"setStyle":"straight","setStyleParameters":{"targetRir":2}}]}]}]}. Usa los días indicados en el perfil, no una lista fija. Cada día debe tener calentamiento, trabajo principal y vuelta a la calma salvo que el perfil indique lo contrario. Usa bloques standard para ejercicios individuales y superset solo para parejas seguras; en superserie cada ejercicio tiene sets=1 y rounds contiene repeticiones del bloque. Incluye entre 4 y 7 ejercicios seguros por sesión, mantén la duración solicitada, usa metric kg y segundos en reps para ejercicios temporizados. Respeta equipamiento, nivel, objetivo y restricciones. imageKey debe ser uno de: airSquat,gobletSquat,barbellBackSquat,walkingLunge,legPress,deadlift,romanianDeadlift,pushUp,benchPress,inclineBenchPress,overheadPress,lateralRaise,chestDip,tricepsPushdown,pullUp,latPulldown,seatedCableRow,bentOverRow,facePull,bicepsCurl,hammerCurl,plank,sidePlank,deadBug,mountainClimber."""
}
