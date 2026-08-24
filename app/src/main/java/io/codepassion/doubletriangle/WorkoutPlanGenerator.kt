package io.codepassion.doubletriangle

import android.content.Context
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
import io.codepassion.doubletriangle.core.model.displayBlocks
import io.codepassion.doubletriangle.feature.workout.CustomWorkoutRequest
import io.codepassion.doubletriangle.feature.workout.WorkoutHistoryStore
import io.codepassion.doubletriangle.feature.onboarding.OnboardingProfile
import io.codepassion.doubletriangle.feature.onboarding.WorkoutWeekday
import java.net.HttpURLConnection
import java.net.URL
import java.time.DayOfWeek
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object WorkoutPlanGenerator {
    suspend fun generate(profile: OnboardingProfile, context: Context? = null): Pair<String, WorkoutHubState> = withContext(Dispatchers.IO) {
        generateWithInstruction(profile, "Genera un plan semanal progresivo respetando exactamente los días seleccionados, el objetivo, nivel, restricciones, equipamiento y duración del perfil.", context)
    }

    suspend fun generateNext(profile: OnboardingProfile, previous: WorkoutHubState, context: Context? = null): Pair<String, WorkoutHubState> = withContext(Dispatchers.IO) {
        val previousStructure = previous.workouts.joinToString("; ") { workout ->
            "${workout.scheduledDay}: ${workout.focus}, ${workout.exercises.joinToString(", ") { it.name }}"
        }
        val (rawJson, generated) = generateWithInstruction(
            profile,
            "Genera la SIGUIENTE semana progresiva del plan anterior. Mantén exactamente los mismos días, objetivo, división y restricciones. Ajusta cargas, repeticiones, volumen o sustituciones usando el historial y feedback disponible; no repitas ciegamente la semana anterior. Semana anterior (${previous.weekIndex}/${previous.cycleLength}, mesociclo ${previous.mesocycleIndex}): $previousStructure",
            context,
        )
        val nextWeek = previous.weekIndex + 1
        val effectiveCycleLength = if (previous.cycleLength <= 1) 4 else previous.cycleLength
        val startsNewMesocycle = nextWeek > effectiveCycleLength
        val nextState = generated.copy(
            mesocycleIndex = if (startsNewMesocycle) previous.mesocycleIndex + 1 else previous.mesocycleIndex,
            cycleLength = effectiveCycleLength,
            weekIndex = if (startsNewMesocycle) 1 else nextWeek,
        )
        serialize(nextState) to nextState
    }

    suspend fun generateCustom(profile: OnboardingProfile, request: CustomWorkoutRequest, context: Context? = null): WorkoutDaySummary = withContext(Dispatchers.IO) {
        val (_, state) = generateWithInstruction(
            profile.copy(workoutDays = setOf(WorkoutWeekday.Monday)),
            "Genera UNA ÚNICA sesión personalizada para MONDAY. Enfoque: ${request.focus}. Duración objetivo: ${request.durationMinutes} minutos. El equipamiento temporal de esta sesión es exactamente: ${request.equipment.ifBlank { "peso corporal" }}. Sustituye con él el equipamiento habitual del perfil: no añadas ni presupongas máquinas, barras o accesorios que no figuren en esta lista. Devuelve un único objeto dentro de `workouts` y aplica las mismas reglas de bloques, prescripciones y seguridad que el plan semanal.",
            context,
        )
        state.workouts.first().copy(id = "custom-ai-${UUID.randomUUID()}", order = 1, estimatedMinutes = request.durationMinutes, status = WorkoutStatus.Planned)
    }

    suspend fun adaptWorkout(profile: OnboardingProfile, workout: WorkoutDaySummary, request: CustomWorkoutRequest, context: Context? = null): WorkoutDaySummary = withContext(Dispatchers.IO) {
        val (_, state) = generateWithInstruction(
            profile.copy(workoutDays = setOf(WorkoutWeekday.Monday)),
            "Adapta UNA ÚNICA sesión existente para MONDAY. Equipamiento disponible en esta ubicación: ${request.equipment.ifBlank { "peso corporal" }}. Conserva el foco (${workout.focus}), tipo (${workout.dayType}), duración aproximada (${workout.estimatedMinutes} min), intención, patrones de movimiento y dificultad de la sesión original; sustituye solo lo que el nuevo equipamiento o la seguridad requieran. No inventes equipamiento. Sesión original:\n${workoutPromptSummary(workout)}\nDevuelve un único objeto dentro de `workouts`.",
            context,
        )
        state.workouts.first().copy(
            id = workout.id,
            order = workout.order,
            focus = workout.focus,
            dayType = workout.dayType,
            scheduledDay = workout.scheduledDay,
            status = workout.status,
        )
    }

    private suspend fun generateWithInstruction(profile: OnboardingProfile, instruction: String, context: Context?): Pair<String, WorkoutHubState> = withContext(Dispatchers.IO) {
        check(BuildConfig.OPENAI_API_KEY.isNotBlank()) { "Falta OPENAI_API_KEY en local.properties" }
        val body = JSONObject()
            .put("model", BuildConfig.OPENAI_MODEL)
            .put("messages", JSONArray()
                .put(JSONObject().put("role", "system").put("content", systemPrompt(profile, context?.let { WorkoutHistoryStore.aiPlanningContext(it.applicationContext) })))
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
            val parsed = parse(planJson, profile.name, profile.goal.title)
            validateGeneratedPlan(parsed, profile, enforceProfileSections = instruction.startsWith("Genera un plan semanal"))
            planJson to parsed
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
                add(WorkoutDaySummary("ai-${index + 1}", index + 1, day.optString("title").trim().ifBlank { "Sesión ${index + 1}" }, day.optString("focus").trim().ifBlank { "Fitness general" }, day.optString("dayType").trim().lowercase().ifBlank { "strength" }, weekday, estimatedMinutes.coerceIn(15, 180), WorkoutStatus.Planned, exercises, blocks))
            }
        }
        check(workouts.isNotEmpty()) { "La IA devolvió un plan vacío" }
        val cycleLength = root.optInt("cycleLength", 1).coerceAtLeast(1)
        val weekIndex = root.optInt("weekIndex", 1).coerceIn(1, cycleLength)
        return WorkoutHubState(
            user = UserSummary(userName, goal, 0),
            trainingDays = workouts.mapTo(mutableSetOf()) { it.scheduledDay },
            completedDays = emptySet(),
            planName = root.optString("planName").trim().ifBlank { "Plan IA · Semana 1" },
            phase = root.optString("phase").trim().ifBlank { "Adaptación · Mesociclo 1" },
            workouts = workouts,
            mesocycleIndex = root.optInt("mesocycleIndex", 1).coerceAtLeast(1),
            cycleLength = cycleLength,
            weekIndex = weekIndex,
        )
    }

    private fun validateGeneratedPlan(state: WorkoutHubState, profile: OnboardingProfile, enforceProfileSections: Boolean) {
        val expectedDays = profile.workoutDays.map { it.storedValue.uppercase() }.toSet()
        val actualDays = state.workouts.map { it.scheduledDay.name }
        check(actualDays.size == expectedDays.size && actualDays.toSet() == expectedDays) {
            "La IA no respetó exactamente los días seleccionados. Vuelve a generar el plan."
        }
        state.workouts.forEach { workout ->
            check(workout.exercises.isNotEmpty()) {
                "La IA devolvió una sesión vacía para ${workout.scheduledDay}. Vuelve a generar el plan."
            }
            check(workout.exercises.none { it.name.isBlank() }) {
                "La IA devolvió un ejercicio sin nombre en ${workout.title}. Vuelve a generar el plan."
            }
            check(workout.exercises.map { it.name.trim().lowercase() }.distinct().size == workout.exercises.size) {
                "La IA repitió ejercicios dentro de ${workout.title}. Vuelve a generar el plan."
            }
            val blocks = workout.displayBlocks()
            val hasWarmup = blocks.any { it.type == WorkoutBlockType.Warmup && it.exercises.isNotEmpty() }
            val hasCooldown = blocks.any { it.type == WorkoutBlockType.Cooldown && it.exercises.isNotEmpty() }
            if (enforceProfileSections) {
                check(profile.skipsWarmups || hasWarmup) { "Falta calentamiento en ${workout.title}. Vuelve a generar el plan." }
                check(profile.skipsCooldowns || hasCooldown) { "Falta vuelta a la calma en ${workout.title}. Vuelve a generar el plan." }
                check(!profile.skipsWarmups || !blocks.any { it.type == WorkoutBlockType.Warmup }) { "El plan incluye calentamiento aunque está desactivado." }
                check(!profile.skipsCooldowns || !blocks.any { it.type == WorkoutBlockType.Cooldown }) { "El plan incluye vuelta a la calma aunque está desactivada." }
            }
        }
    }

    fun serialize(state: WorkoutHubState): String {
        val days = JSONArray()
        state.workouts.forEach { workout ->
            val blocks = JSONArray()
            workout.displayBlocks().forEach { block ->
                val exercises = JSONArray()
                block.exercises.forEach { exercise ->
                    val parameters = JSONObject()
                        .put("dropCount", exercise.setStyleParameters.dropCount)
                        .put("dropWeightPercent", exercise.setStyleParameters.dropWeightPercent)
                        .put("backoffSetCount", exercise.setStyleParameters.backoffSetCount)
                        .put("backoffWeightPercent", exercise.setStyleParameters.backoffWeightPercent)
                        .put("intraSetRestSeconds", exercise.setStyleParameters.intraSetRestSeconds)
                        .put("tempo", exercise.setStyleParameters.tempo)
                        .put("targetRir", exercise.setStyleParameters.targetRir)
                    exercises.put(JSONObject()
                        .put("name", exercise.name)
                        .put("imageKey", exercise.imageKey)
                        .put("sets", exercise.sets)
                        .put("reps", exercise.reps)
                        .put("restSeconds", exercise.restSeconds)
                        .put("setStyle", exercise.setStyle.name)
                        .put("targetWeightKg", exercise.targetWeightKg)
                        .put("setStyleParameters", parameters))
                }
                blocks.put(JSONObject()
                    .put("type", block.type.name.lowercase())
                    .put("rounds", block.rounds)
                    .put("restAfterBlockSeconds", block.restAfterBlockSeconds)
                    .put("notes", block.notes)
                    .put("exercises", exercises))
            }
            days.put(JSONObject()
                .put("title", workout.title)
                .put("focus", workout.focus)
                .put("dayType", workout.dayType)
                .put("weekday", workout.scheduledDay.name)
                .put("estimatedDurationMinutes", workout.estimatedMinutes)
                .put("blocks", blocks))
        }
        return JSONObject()
            .put("planName", state.planName)
            .put("phase", state.phase)
            .put("mesocycleIndex", state.mesocycleIndex)
            .put("cycleLength", state.cycleLength)
            .put("weekIndex", state.weekIndex)
            .put("workouts", days)
            .toString()
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

    private fun systemPrompt(profile: OnboardingProfile, historyContext: String?): String = SYSTEM_PROMPT + """

Perfil obligatorio del usuario:
- Nombre: ${profile.name}; objetivo: ${profile.goal.title}; nivel: ${profile.trainingLevel.title}; estilo de vida: ${profile.lifestyle.title}
- Sexo: ${profile.gender.title}; idioma de salida: español; todas las cadenas visibles, títulos, notas y explicaciones deben estar en español.
- Días disponibles: ${profile.workoutDays.joinToString { it.storedValue }}; duración preferida: ${profile.preferredWorkoutDurationMinutes} minutos
- Estructura: ${profile.trainingSplitPreference.title}; focos personalizados: ${profile.customWorkoutFocuses.entries.joinToString { "${it.key.storedValue}=${it.value.storedValue}" }.ifBlank { "ninguno" }}
- Equipamiento: ${profile.availableEquipment.joinToString { it.storedValue }}; restricciones: ${profile.movementRestrictions.joinToString { it.storedValue }.ifBlank { "ninguna" }}
- Composición corporal: ${profile.bodyCompositionPhase?.storedValue ?: "no especificada"}; edad aproximada: ${(java.time.Year.now().value - profile.birthYear).coerceAtLeast(13)}; altura: ${profile.heightCm} cm; peso: ${profile.weightKg} kg
- Omitir calentamiento: ${if (profile.skipsWarmups) "sí" else "no"}; omitir vuelta a la calma: ${if (profile.skipsCooldowns) "sí" else "no"}; omitir descansos: ${if (profile.skipsRestPeriods) "sí" else "no"}; notas del planificador: ${profile.workoutPlannerNotes.ifBlank { "ninguna" }}
${historyContext ?: "Historial de entrenamientos recientes: todavía no hay sesiones completadas."}
Reglas estrictas: crea exactamente un workout por cada día disponible y no inventes días. Cada sesión debe respetar el presupuesto total de duración incluyendo calentamiento, trabajo principal y vuelta a la calma. Si se omite calentamiento o vuelta a la calma, devuelve cero ejercicios y cero bloques de ese tipo: nunca uses bloques vacíos, ocultos o de relleno. No uses ejercicios incompatibles con el equipamiento o las restricciones; prioriza sustituciones seguras y cercanas. Usa siempre bloques y prescripciones concretas, conserva el foco personalizado de cada día y distribuye el volumen de forma recuperable.
Metadatos de progresión: incluye también mesocycleIndex, cycleLength y weekIndex como enteros positivos en la raíz del JSON. Mantén cycleLength normalmente entre 4 y 6 semanas y usa weekIndex para indicar la semana actual del mesociclo; si no hay contexto previo, empieza en mesocycleIndex=1 y weekIndex=1.
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
        val name = exercise.optString("name", exercise.optString("exercise", "Ejercicio")).trim().ifBlank { "Ejercicio" }
        val imageKey = exercise.optString("imageKey").trim().takeIf(String::isNotBlank) ?: legacyImageKey(name)
        val details = exercise.optJSONObject("setStyleParameters") ?: JSONObject()
        val parameters = SetStyleParameters(
            dropCount = details.optInt("dropCount", 2).coerceIn(1, 5),
            dropWeightPercent = details.optInt("dropWeightPercent", 20).coerceIn(5, 50),
            backoffSetCount = details.optInt("backoffSetCount", 3).coerceIn(1, 6),
            backoffWeightPercent = details.optInt("backoffWeightPercent", 15).coerceIn(5, 50),
            intraSetRestSeconds = details.optInt("intraSetRestSeconds", 15).coerceIn(5, 120),
            tempo = details.optString("tempo", "3-1-1-0").trim().take(9),
            targetRir = details.optInt("targetRir", 2).coerceIn(0, 5),
        )
        return ExerciseSummary(
            name = name,
            imageKey = imageKey,
            sets = exercise.optInt("sets", 1).coerceIn(1, 10),
            reps = exercise.optString("reps").trim().ifBlank {
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
            targetWeightKg = exercise.optDouble("targetWeightKg", Double.NaN).takeUnless { it.isNaN() }
                ?: exercise.optJSONArray("targetWeightsKg")?.optDouble(0, Double.NaN)?.takeUnless { it.isNaN() },
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

    private fun workoutPromptSummary(workout: WorkoutDaySummary): String = buildString {
        appendLine("- Título: ${workout.title}")
        workout.displayBlocks().forEach { block ->
            appendLine("- ${block.type.label}: ${block.exercises.joinToString { exercise -> "${exercise.name} (${exercise.sets}×${exercise.reps}, descanso ${exercise.restSeconds}s)" }}")
        }
    }
    private const val SYSTEM_PROMPT = """Eres un entrenador profesional y el planificador de Wildforce. Responde exclusivamente con JSON válido, sin markdown. Usa esta forma: {"planName":"...","phase":"...","workouts":[{"title":"...","focus":"...","dayType":"strength|hypertrophy|technique|volume|deload|recovery|conditioning","weekday":"MONDAY","estimatedDurationMinutes":50,"blocks":[{"type":"warmup|standard|superset|cooldown","orderIndex":0,"rounds":1,"restAfterBlockSeconds":90,"notes":"...","exercises":[{"name":"...","imageKey":"benchPress","sets":3,"reps":"8-12","repsMin":8,"repsMax":12,"targetWeightKg":20,"restSeconds":90,"setStyle":"straight","setStyleParameters":{"targetRir":2}}]}]}]}. Usa los días indicados en el perfil, no una lista fija. Cada día debe tener calentamiento, trabajo principal y vuelta a la calma salvo que el perfil indique lo contrario. Usa bloques standard para ejercicios individuales y superset solo para parejas seguras; en superserie cada ejercicio tiene sets=1 y rounds contiene repeticiones del bloque. Incluye entre 4 y 7 ejercicios seguros por sesión, mantén la duración solicitada, usa metric kg y segundos en reps para ejercicios temporizados. Respeta equipamiento, nivel, objetivo y restricciones. imageKey debe ser uno de: airSquat,gobletSquat,barbellBackSquat,walkingLunge,legPress,deadlift,romanianDeadlift,pushUp,benchPress,inclineBenchPress,overheadPress,lateralRaise,chestDip,tricepsPushdown,pullUp,latPulldown,seatedCableRow,bentOverRow,facePull,bicepsCurl,hammerCurl,plank,sidePlank,deadBug,mountainClimber."""
}
