package io.codepassion.doubletriangle

import android.content.Context
import android.content.SharedPreferences
import io.codepassion.doubletriangle.feature.onboarding.OnboardingProfile
import io.codepassion.doubletriangle.feature.workout.WorkoutHistoryStore
import io.codepassion.doubletriangle.nutrition.NutritionStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/** Bridges Android's JSON plan store to the same sync graph used by iOS. */
internal object WorkoutRemoteSync {
    private const val api = "https://www.wildforce.app/api"
    private const val accountPrefs = "wildforce_account"
    private const val planKey = "remote_workout_plan_id"

    suspend fun synchronize(context: Context, profile: OnboardingProfile, preferences: SharedPreferences): Boolean = withContext(Dispatchers.IO) {
        val account = context.getSharedPreferences(accountPrefs, 0)
        val token = account.getString("token", null) ?: return@withContext false
        val userId = account.getString("id", null) ?: return@withContext false
        runCatching {
            if (preferences.getBoolean("remote_sync_import_required", false) || preferences.getString("workout_plan_json", null).isNullOrBlank()) {
                pullWorkoutPlanWhenMissing(token, preferences)
                preferences.edit().remove("remote_sync_import_required").apply()
            }
            val now = java.time.Instant.now().toString()
            push(token, "training-preferences", JSONArray().put(record(stableId("$userId:training-preferences"), now)
                .put("goal", profile.goal.storedValue).put("lifestyle", profile.lifestyle.storedValue)
                .put("gym_type", profile.gymType.storedValue).put("general_training_level", profile.trainingLevel.storedValue)
                .put("training_split_preference", profile.trainingSplitPreference.storedValue)
                .put("preferred_workout_duration_minutes", profile.preferredWorkoutDurationMinutes)
                .put("workout_planner_notes", profile.workoutPlannerNotes)
                .put("workout_days", JSONArray(profile.workoutDays.map { it.storedValue }))
                .put("custom_workout_focuses", JSONObject(profile.customWorkoutFocuses.mapKeys { it.key.storedValue }.mapValues { it.value.storedValue }))
                .put("movement_restrictions", JSONArray(profile.movementRestrictions.map { it.storedValue }))
                .put("body_composition_phase", profile.bodyCompositionPhase?.storedValue ?: JSONObject.NULL)
                .put("skips_warmups", profile.skipsWarmups).put("skips_cooldowns", profile.skipsCooldowns).put("skips_rest_periods", profile.skipsRestPeriods)))
            push(token, "training-locations", JSONArray().apply {
                profile.trainingLocations.forEachIndexed { index, location -> put(record(stableId("$userId:location:$index"), now)
                    .put("name", location.name).put("is_default", location.isDefault).put("sort_order", index)
                    .put("equipment", JSONArray(location.equipment.map { it.storedValue }))) }
            })
            val detailPrefs = context.getSharedPreferences("wildforce_profile_details", Context.MODE_PRIVATE)
            push(token, "user-app-settings", JSONArray().put(record(stableId("$userId:settings"), now)
                .put("is_health_kit_enabled", profile.isHealthConnectEnabled)
                .put("is_screen_on_during_workout_enabled", detailPrefs.getBoolean("keep_screen_on", true))
                .put("is_notifications_enabled", context.getSharedPreferences("wildforce_notification_settings", Context.MODE_PRIVATE).getBoolean("enabled", true))
                .put("is_full_focus_mode_enabled", detailPrefs.getBoolean("full_focus", false))))
            val bodyMetrics = context.getSharedPreferences("wildforce_body_metrics", Context.MODE_PRIVATE).getString("history", "[]") ?: "[]"
            val metricRecords = JSONArray(); val metrics = JSONArray(bodyMetrics)
            for (index in 0 until metrics.length()) { val metric = metrics.optJSONObject(index) ?: continue; val recorded = metric.optString("date") + "T12:00:00Z"; metricRecords.put(record(stableId("$userId:weight:${metric.optString("date")}"), now).put("type", "weight").put("value", metric.optDouble("weightKg")).put("recorded_at", recorded).put("source", "manual")) }
            push(token, "body-metric-entries", metricRecords)

            synchronizeNutrition(token, userId, profile, now, context)
            preferences.getString("workout_plan_json", null)?.let { rawPlan ->
                synchronizeWorkoutPlan(token, userId, profile, now, context, preferences, JSONObject(rawPlan))
            }
        }.isSuccess
    }

    private fun synchronizeNutrition(token: String, userId: String, profile: OnboardingProfile, now: String, context: Context) {
        val prefs = NutritionStore.loadPreferences(context)
        push(token, "nutrition-profiles", JSONArray().put(record(stableId("$userId:nutrition-profile"), now)
            .put("dietary_style", prefs.dietaryStyle).put("meals_per_day_preference", prefs.mealsPerDay)
            .put("preferred_eating_window_start_hour", prefs.eatingWindowStartHour ?: JSONObject.NULL)
            .put("preferred_eating_window_end_hour", prefs.eatingWindowEndHour ?: JSONObject.NULL)
            .put("excluded_foods", JSONArray(prefs.excludedFoods)).put("allergies_and_intolerances", JSONArray(prefs.allergiesAndIntolerances))
            .put("cooking_effort", prefs.cookingEffort).put("budget_sensitivity", prefs.budgetSensitivity)
            .put("preferred_protein_sources", JSONArray(prefs.preferredProteinSources)).put("dislikes", JSONArray(prefs.dislikes))
            .put("wants_meal_suggestions", prefs.wantsSuggestions).put("notes", prefs.notes)))

        val entries = JSONArray(); val items = JSONArray()
        NutritionStore.allForSync(context).forEach { meal ->
            val entryId = stableId("$userId:nutrition-entry:${meal.id}")
            entries.put(record(entryId, now).put("title", meal.name)
                .put("logged_at", meal.loggedDate.toString() + "T12:00:00Z").put("meal_type", meal.type.name.lowercase())
                .put("notes", meal.notes).put("is_favorite", meal.isBookmarked))
            val foods = meal.items.ifEmpty { listOf(io.codepassion.doubletriangle.nutrition.MealItem(meal.name, meal.calories, meal.protein, meal.carbs, meal.fat)) }
            foods.forEachIndexed { index, food -> items.put(record(stableId("$entryId:$index"), now)
                .put("nutrition_log_entry_id", entryId).put("name", food.name).put("quantity", 1)
                .put("calories", food.calories).put("protein_grams", food.protein).put("carbs_grams", food.carbs)
                .put("fat_grams", food.fat).put("order_index", index)) }
        }
        push(token, "nutrition-log-entries", entries)
        push(token, "nutrition-log-items", items)

        val plan = NutritionStore.loadGeneratedPlan(context) ?: return
        val nutritionPlanId = stableId("$userId:nutrition-plan")
        push(token, "nutrition-plans", JSONArray().put(record(nutritionPlanId, now)
            .put("starts_on", plan.minOf { it.date }.toString() + "T00:00:00Z").put("goal", profile.goal.storedValue)
            .put("body_composition_phase", profile.bodyCompositionPhase?.storedValue ?: JSONObject.NULL)
            .put("daily_calorie_average", plan.map { it.targets.calories }.average()).put("notes", "Plan generado en Android")))
        val days = JSONArray(); val meals = JSONArray()
        plan.forEach { day ->
            val dayId = stableId("$nutritionPlanId:${day.date}")
            days.put(record(dayId, now).put("nutrition_plan_id", nutritionPlanId).put("date", day.date.toString())
                .put("weekday", day.date.dayOfWeek.name.lowercase()).put("day_type", day.type.name.lowercase())
                .put("target_calories", day.targets.calories).put("target_protein_grams", day.targets.protein)
                .put("target_carbs_grams", day.targets.carbs).put("target_fat_grams", day.targets.fat)
                .put("planned_workout_title", day.workoutTitle).put("energy_demand", day.energyDemand.name.lowercase())
                .put("notes", day.notes).put("pre_workout_guidance", day.preWorkoutGuidance ?: JSONObject.NULL)
                .put("post_workout_guidance", day.postWorkoutGuidance ?: JSONObject.NULL))
            day.meals.forEachIndexed { index, meal -> meals.put(record(stableId("$dayId:$index"), now)
                .put("nutrition_day_id", dayId).put("title", meal.title).put("order_index", index).put("meal_type", meal.type.name.lowercase())
                .put("target_calories", meal.targets.calories).put("target_protein_grams", meal.targets.protein)
                .put("target_carbs_grams", meal.targets.carbs).put("target_fat_grams", meal.targets.fat)
                .put("guidance", meal.guidance).put("example_foods", JSONArray().apply { meal.foods.forEach { (name, grams) -> put(JSONObject().put("name", name).put("grams", grams)) } })) }
        }
        push(token, "nutrition-days", days)
        push(token, "nutrition-meals", meals)
    }

    /**
     * A first Android session must restore the server plan before creating any local
     * records. Otherwise a freshly generated empty/default plan could win the
     * last-write-wins conflict and hide the user's iOS workouts.
     */
    private fun pullWorkoutPlanWhenMissing(token: String, preferences: SharedPreferences) {
        val response = get(token, "$api/sync/workout-days")
        val remoteDays = response.optJSONArray("data") ?: return
        if (remoteDays.length() == 0) return
        val workouts = JSONArray()
        var planId: String? = null
        for (index in 0 until remoteDays.length()) {
            val day = remoteDays.optJSONObject(index) ?: continue
            if (!day.isNull("deleted_at")) continue
            planId = planId ?: day.optString("workout_plan_id").takeIf(String::isNotBlank)
            val blocks = JSONArray()
            val rawBlocks = day.optJSONArray("blocks") ?: JSONArray()
            for (blockIndex in 0 until rawBlocks.length()) {
                val block = rawBlocks.optJSONObject(blockIndex) ?: continue
                if (!block.isNull("deleted_at")) continue
                blocks.put(JSONObject().put("type", block.optString("type", "standard"))
                    .put("rounds", block.optInt("rounds", 1)).put("restAfterBlockSeconds", block.opt("rest_after_block_seconds"))
                    .put("notes", block.opt("notes")).put("exercises", remoteExercises(block.optJSONArray("exercises"))))
            }
            workouts.put(JSONObject().put("id", day.optString("id")).put("title", day.optString("title", "Sesión ${index + 1}"))
                .put("focus", day.optString("focus", "Fitness general")).put("dayType", day.optString("day_type", "strength"))
                .put("status", day.optString("status", "planned").uppercase()).put("weekday", day.optString("intended_weekday", "MONDAY").uppercase())
                .put("estimatedDurationMinutes", day.optInt("estimated_duration_minutes", 50)).put("blocks", blocks))
        }
        if (workouts.length() == 0) return
        val root = JSONObject().put("planName", "Plan sincronizado").put("phase", "Plan de tu cuenta")
            .put("cycleLength", 1).put("mesocycleNumber", 1).put("phaseWeek", 1).put("workouts", workouts)
        preferences.edit().putString("workout_plan_json", root.toString())
            .putString(planKey, planId ?: stableId("remote-plan"))
            .putBoolean("remote_plan_imported", true).apply()
    }

    private fun remoteExercises(entries: JSONArray?): JSONArray = JSONArray().apply {
        entries ?: return@apply
        for (index in 0 until entries.length()) {
            val exercise = entries.optJSONObject(index) ?: continue
            if (!exercise.isNull("deleted_at")) continue
            val min = exercise.opt("reps_min"); val max = exercise.opt("reps_max")
            put(JSONObject().put("name", exercise.optString("exercise", "Ejercicio"))
                .put("sets", exercise.optInt("sets", 1)).put("repsMin", min).put("repsMax", max)
                .put("reps", if (min is Number && max is Number) "${min.toInt()}-${max.toInt()}" else "")
                .put("restSeconds", exercise.optInt("rest_seconds", 0)).put("targetWeightKg", exercise.opt("target_weight_kg"))
                .put("targetReps", exercise.opt("target_reps")).put("targetWeightsKg", exercise.opt("target_weights_kg"))
                .put("targetDurationMinutes", exercise.opt("target_duration_minutes")).put("targetDurationSeconds", exercise.opt("target_duration_seconds"))
                .put("targetDistanceKm", exercise.opt("target_distance_km")).put("setStyle", "Straight")
                .put("setStyleParameters", exercise.opt("set_style_configuration")))
        }
    }

    private fun synchronizeWorkoutPlan(token: String, userId: String, profile: OnboardingProfile, now: String, context: Context, preferences: SharedPreferences, root: JSONObject) {
        val planId = preferences.getString(planKey, null) ?: stableId("$userId:${root.optString("planName")}").also { preferences.edit().putString(planKey, it).apply() }
        push(token, "workout-plans", JSONArray().put(record(planId, now)
            .put("name", root.optString("planName", "Plan Android")).put("goal", profile.goal.storedValue).put("status", "active")
            .put("phase", root.optString("phase")).put("mesocycle_number", root.optInt("mesocycleNumber", 1))
            .put("phase_week", root.optInt("phaseWeek", 1)).put("cycle_length", root.optInt("cycleLength", 1))
            .put("body_composition_phase", profile.bodyCompositionPhase?.storedValue ?: JSONObject.NULL)))
        val days = root.optJSONArray("workouts") ?: JSONArray(); val dayRecords = JSONArray(); val blocks = JSONArray(); val exercises = JSONArray(); val plannedExerciseIds = mutableMapOf<String, String>()
        for (dayIndex in 0 until days.length()) {
            val day = days.getJSONObject(dayIndex); val dayId = stableId("$planId:${day.optString("id", dayIndex.toString())}")
            dayRecords.put(record(dayId, now).put("workout_plan_id", planId).put("title", day.optString("title", "Sesión ${dayIndex + 1}")).put("focus", day.optString("focus", "fullBody")).put("status", day.optString("status", "PLANNED").lowercase()).put("order_index", dayIndex).put("intended_weekday", day.optString("weekday").lowercase()).put("day_type", day.optString("dayType")).put("estimated_duration_minutes", day.optInt("estimatedDurationMinutes", 50)))
            val dayBlocks = day.optJSONArray("blocks") ?: JSONArray()
            for (blockIndex in 0 until dayBlocks.length()) {
                val block = dayBlocks.getJSONObject(blockIndex); val blockId = stableId("$dayId:$blockIndex")
                blocks.put(record(blockId, now).put("workout_day_id", dayId).put("type", block.optString("type", "standard")).put("order_index", blockIndex).put("rounds", block.optInt("rounds", 1)).put("rest_after_block_seconds", block.opt("restAfterBlockSeconds")).put("notes", block.opt("notes")))
                val entries = block.optJSONArray("exercises") ?: JSONArray()
                for (exerciseIndex in 0 until entries.length()) {
                    val item = entries.getJSONObject(exerciseIndex); val exerciseId = stableId("$blockId:$exerciseIndex")
                    val historyKey = item.optString("imageKey").trim().ifBlank { item.optString("name").trim() }.lowercase()
                    if (historyKey.isNotBlank()) plannedExerciseIds.putIfAbsent(historyKey, exerciseId)
                    exercises.put(record(exerciseId, now).put("workout_day_id", dayId).put("workout_block_id", blockId).put("exercise", item.optString("name")).put("order_index", exerciseIndex).put("sets", item.optInt("sets", 1)).put("reps_min", item.opt("repsMin")).put("reps_max", item.opt("repsMax")).put("target_reps", item.opt("targetReps")).put("target_weight_kg", item.opt("targetWeightKg")).put("target_weights_kg", item.opt("targetWeightsKg")).put("target_duration_minutes", item.opt("targetDurationMinutes")).put("target_duration_seconds", item.opt("targetDurationSeconds")).put("target_distance_km", item.opt("targetDistanceKm")).put("rest_seconds", item.optInt("restSeconds", 0)).put("set_style_configuration", item.opt("setStyleParameters")))
                }
            }
        }
        push(token, "workout-days", dayRecords); push(token, "workout-blocks", blocks); push(token, "planned-exercises", exercises)
        val results = JSONArray()
        WorkoutHistoryStore.remoteEntries(context = context).forEach { entry ->
            val plannedExerciseId = plannedExerciseIds[entry.exerciseKey] ?: return@forEach
            results.put(record(stableId("$plannedExerciseId:${entry.timestampMillis}"), now)
                .put("planned_exercise_id", plannedExerciseId).put("feedback", entry.feedback ?: "justRight")
                .put("completed_at", java.time.Instant.ofEpochMilli(entry.timestampMillis).toString())
                .put("completed_sets", entry.sets).put("completed_reps", entry.totalReps).put("completed_weight", entry.maxWeightKg)
                .put("per_set_reps", JSONArray(entry.setReps)).put("per_set_weights_kg", JSONArray(entry.setWeightsKg))
                .put("completed_duration_seconds", entry.durationSeconds).put("completed_duration_minutes", entry.durationSeconds / 60)
                .put("completed_distance_km", entry.distanceKm).put("notes", entry.note ?: JSONObject.NULL))
        }
        push(token, "exercise-results", results)
    }

    private fun record(id: String, now: String) = JSONObject().put("id", id).put("created_at", now).put("updated_at", now)
    private fun stableId(value: String) = UUID.nameUUIDFromBytes(value.toByteArray()).toString()
    private fun push(token: String, resource: String, records: JSONArray) {
        if (records.length() == 0) return
        val connection = (URL("$api/sync/push").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"; doOutput = true; connectTimeout = 15_000; readTimeout = 15_000
            setRequestProperty("Accept", "application/json"); setRequestProperty("Content-Type", "application/json"); setRequestProperty("Authorization", "Bearer $token")
        }
        OutputStreamWriter(connection.outputStream).use { it.write(JSONObject().put("resource", resource).put("records", records).toString()) }
        val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
        val response = BufferedReader(stream.reader()).use { it.readText() }
        if (connection.responseCode !in 200..299) throw IllegalStateException(JSONObject(response).optString("message", "No se pudo sincronizar $resource"))
    }

    private fun get(token: String, url: String): JSONObject {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"; connectTimeout = 15_000; readTimeout = 15_000
            setRequestProperty("Accept", "application/json"); setRequestProperty("Authorization", "Bearer $token")
        }
        val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
        val response = BufferedReader(stream.reader()).use { it.readText() }
        if (connection.responseCode !in 200..299) throw IllegalStateException(JSONObject(response).optString("message", "No se pudo descargar la cuenta"))
        return JSONObject(response)
    }
}
