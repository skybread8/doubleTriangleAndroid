package io.codepassion.doubletriangle.nutrition

import android.content.Context
import io.codepassion.doubletriangle.NutritionStatusWidget
import io.codepassion.doubletriangle.WildforceNotificationScheduler
import io.codepassion.doubletriangle.feature.onboarding.BodyCompositionPhase
import io.codepassion.doubletriangle.feature.onboarding.FitnessGoal
import io.codepassion.doubletriangle.feature.onboarding.OnboardingProfile
import io.codepassion.doubletriangle.feature.onboarding.WorkoutWeekday
import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

internal enum class MealType(val title: String) {
    Breakfast("Desayuno"),
    Lunch("Comida"),
    Dinner("Cena"),
    Snack("Snack"),
    PreWorkout("Pre-entreno"),
    PostWorkout("Post-entreno");
    companion object {
        fun detect(): MealType = when (java.time.LocalTime.now().hour) {
            in 5..10 -> Breakfast; in 11..15 -> Lunch; in 18..22 -> Dinner; else -> Snack
        }
    }
}

internal data class NutritionTargets(val calories: Int = 2350, val protein: Int = 165, val carbs: Int = 250, val fat: Int = 75)

/** Mirrors iOS NutritionLogItem so reviewed food metadata survives local storage and sync. */
internal data class MealItem(
    val name: String,
    val calories: Int,
    val protein: Int,
    val carbs: Int,
    val fat: Int,
    val brand: String = "",
    val quantity: Double = 1.0,
    val unit: String = "",
    val grams: Double? = null,
    val notes: String = "",
)

internal data class MealLog(
    val id: Long,
    val name: String,
    val calories: Int,
    val protein: Int,
    val carbs: Int,
    val fat: Int,
    val type: MealType = MealType.Snack,
    val loggedDate: LocalDate = Instant.ofEpochMilli(id).atZone(ZoneId.systemDefault()).toLocalDate(),
    val notes: String = "",
    val source: String = "manual",
    val isBookmarked: Boolean = false,
    val items: List<MealItem> = emptyList(),
    val photoPath: String? = null,
    /** Local counterpart of iOS's nutritionMealId: hides the fulfilled suggestion. */
    val plannedMealKey: String? = null,
    /** Optional user-facing name assigned when bookmarking a meal. */
    val bookmarkName: String = "",
    /** Remote product/meal image counterpart of iOS NutritionLogMedia.remoteURL. */
    val mediaUrl: String? = null,
)

internal enum class NutritionDayType(val title: String) { Training("Entrenamiento"), Rest("Descanso"), Recovery("Recuperación") }
internal enum class EnergyDemand(val title: String) { High("Alta"), Medium("Media"), Low("Baja") }

internal data class PlannedMeal(
    val title: String,
    val type: MealType,
    val targets: NutritionTargets,
    val guidance: String,
    val foods: List<Pair<String, Int>>,
)

internal data class NutritionDayPlan(
    val date: LocalDate,
    val type: NutritionDayType,
    val energyDemand: EnergyDemand,
    val targets: NutritionTargets,
    val workoutTitle: String,
    val notes: String,
    val preWorkoutGuidance: String?,
    val postWorkoutGuidance: String?,
    val meals: List<PlannedMeal>,
)

/** Plan-level fields kept with the generated week, matching iOS NutritionPlan. */
internal data class NutritionPlanMetadata(
    val id: String? = null,
    val startsOn: LocalDate? = null,
    val goal: FitnessGoal? = null,
    val bodyCompositionPhase: BodyCompositionPhase? = null,
    val dailyCalorieAverage: Double? = null,
    val notes: String? = null,
)

/** Treat JSON null sent by older/remote plan payloads as an absent title, not user-visible text. */
internal fun NutritionDayPlan.safeWorkoutTitle(): String =
    workoutTitle.trim().takeUnless { it.equals("null", ignoreCase = true) }.orEmpty()

internal fun JSONObject.cleanString(key: String, fallback: String = ""): String =
    optString(key, fallback).takeUnless { it.equals("null", ignoreCase = true) } ?: fallback

internal data class NutritionPreferences(
    val dietaryStyle: String = "Omnívora",
    val mealsPerDay: Int = 4,
    val wantsSuggestions: Boolean = true,
    val eatingWindowStartHour: Int? = null,
    val eatingWindowEndHour: Int? = null,
    val cookingEffort: String = "Medio",
    val budgetSensitivity: String = "Media",
    val preferredProteinSources: List<String> = emptyList(),
    val dislikes: List<String> = emptyList(),
    val excludedFoods: List<String> = emptyList(),
    val allergiesAndIntolerances: List<String> = emptyList(),
    val notes: String = "",
)

internal object NutritionStore {
    private const val PREFS = "wildforce_nutrition"
    private const val ENTRIES_KEY = "nutrition_entries_v2"
    private const val LEGACY_KEY = "today_meals"
    private const val TARGETS_KEY = "nutrition_targets"
    private const val PROFILE_KEY = "nutrition_profile"
    private const val PLAN_VERSION_KEY = "nutrition_plan_version"
    private const val GENERATED_PLAN_KEY = "nutrition_generated_plan"
    private const val PLAN_METADATA_KEY = "nutrition_plan_metadata"
    private const val REMOTE_PLAN_ID_KEY = "nutrition_remote_plan_id"
    private const val DISMISSED_PLANNED_MEALS_KEY = "nutrition_dismissed_planned_meals"

    fun plannedMealKey(date: LocalDate, meal: PlannedMeal): String =
        "${date}|${meal.type.name}|${meal.title.trim().lowercase()}"

    fun dismissPlannedMeal(context: Context, date: LocalDate, meal: PlannedMeal) {
        val prefs = context.getSharedPreferences(PREFS, 0)
        val dismissed = prefs.getStringSet(DISMISSED_PLANNED_MEALS_KEY, emptySet()).orEmpty() + plannedMealKey(date, meal)
        prefs.edit().putStringSet(DISMISSED_PLANNED_MEALS_KEY, dismissed).apply()
    }

    fun isPlannedMealDismissed(context: Context, date: LocalDate, meal: PlannedMeal): Boolean =
        plannedMealKey(date, meal) in context.getSharedPreferences(PREFS, 0)
            .getStringSet(DISMISSED_PLANNED_MEALS_KEY, emptySet()).orEmpty()

    fun loadPlanVersion(context: Context): Int = context.getSharedPreferences(PREFS, 0).getInt(PLAN_VERSION_KEY, 1).coerceAtLeast(1)

    fun regeneratePlan(context: Context): Int {
        val next = loadPlanVersion(context) + 1
        context.getSharedPreferences(PREFS, 0).edit().putInt(PLAN_VERSION_KEY, next).apply()
        return next
    }

    fun loadGeneratedPlan(context: Context): List<NutritionDayPlan>? = runCatching {
        val days = JSONArray(context.getSharedPreferences(PREFS, 0).getString(GENERATED_PLAN_KEY, null) ?: return null)
        buildList {
            for (index in 0 until days.length()) {
                val day = days.getJSONObject(index)
                val meals = day.optJSONArray("meals")?.let { array -> buildList { for (mealIndex in 0 until array.length()) { val meal = array.getJSONObject(mealIndex); add(PlannedMeal(meal.cleanString("title", "Comida"), runCatching { MealType.valueOf(meal.optString("type")) }.getOrDefault(MealType.Snack), (meal.optJSONObject("targets") ?: JSONObject()).toTargets(NutritionTargets()), meal.cleanString("guidance"), meal.optJSONArray("foods")?.stringPairs().orEmpty())) } } }.orEmpty()
                add(NutritionDayPlan(LocalDate.parse(day.getString("date")), runCatching { NutritionDayType.valueOf(day.getString("type")) }.getOrDefault(NutritionDayType.Rest), runCatching { EnergyDemand.valueOf(day.getString("demand")) }.getOrDefault(EnergyDemand.Medium), (day.optJSONObject("targets") ?: JSONObject()).toTargets(NutritionTargets()), day.cleanString("workoutTitle"), day.cleanString("notes"), day.cleanString("pre").takeIf(String::isNotBlank), day.cleanString("post").takeIf(String::isNotBlank), meals))
            }
        }.takeIf { it.isNotEmpty() }
    }.getOrNull()

    fun loadPlanMetadata(context: Context): NutritionPlanMetadata? = runCatching {
        val json = context.getSharedPreferences(PREFS, 0).getString(PLAN_METADATA_KEY, null)
            ?.let(::JSONObject) ?: return null
        NutritionPlanMetadata(
            id = json.cleanString("id").takeIf(String::isNotBlank),
            startsOn = parseRemoteNutritionDate(json.cleanString("startsOn")),
            goal = fitnessGoalFromStoredValue(json.cleanString("goal")),
            bodyCompositionPhase = bodyCompositionPhaseFromStoredValue(json.cleanString("bodyCompositionPhase")),
            dailyCalorieAverage = json.optDouble("dailyCalorieAverage", Double.NaN).takeUnless(Double::isNaN),
            notes = json.cleanString("notes").takeIf(String::isNotBlank),
        )
    }.getOrNull()

    fun metadataForPlan(profile: OnboardingProfile?, plan: List<NutritionDayPlan>): NutritionPlanMetadata =
        NutritionPlanMetadata(
            startsOn = plan.minOfOrNull { it.date },
            goal = profile?.goal,
            bodyCompositionPhase = profile?.bodyCompositionPhase,
            dailyCalorieAverage = plan.take(7).map { it.targets.calories }.average().takeIf { it.isFinite() },
        )

    fun saveGeneratedPlan(context: Context, plan: List<NutritionDayPlan>, metadata: NutritionPlanMetadata? = null) {
        val days = JSONArray().apply { plan.forEach { day -> put(JSONObject().put("date", day.date.toString()).put("type", day.type.name).put("demand", day.energyDemand.name).put("targets", day.targets.toJson()).put("workoutTitle", day.safeWorkoutTitle()).put("notes", day.notes).put("pre", day.preWorkoutGuidance).put("post", day.postWorkoutGuidance).put("meals", JSONArray().apply { day.meals.forEach { meal -> put(JSONObject().put("title", meal.title).put("type", meal.type.name).put("targets", meal.targets.toJson()).put("guidance", meal.guidance).put("foods", JSONArray().apply { meal.foods.forEach { (name, grams) -> put(JSONObject().put("name", name).put("grams", grams)) } })) } })) } }
        val effectiveMetadata = metadata ?: loadPlanMetadata(context) ?: metadataForPlan(null, plan)
        val metadataJson = JSONObject().apply {
            put("id", effectiveMetadata.id)
            put("startsOn", effectiveMetadata.startsOn?.toString())
            put("goal", effectiveMetadata.goal?.storedValue)
            put("bodyCompositionPhase", effectiveMetadata.bodyCompositionPhase?.storedValue)
            put("dailyCalorieAverage", effectiveMetadata.dailyCalorieAverage)
            put("notes", effectiveMetadata.notes)
        }
        context.getSharedPreferences(PREFS, 0).edit()
            .putString(GENERATED_PLAN_KEY, days.toString())
            .putString(PLAN_METADATA_KEY, metadataJson.toString())
            .apply()
        NutritionStatusWidget.refresh(context)
    }

    fun remotePlanId(context: Context): String? = context.getSharedPreferences(PREFS, 0)
        .getString(REMOTE_PLAN_ID_KEY, null)?.takeIf { it.matches(Regex("[0-9a-fA-F-]{36}")) }

    fun rememberRemotePlanId(context: Context, id: String) {
        if (id.matches(Regex("[0-9a-fA-F-]{36}"))) {
            context.getSharedPreferences(PREFS, 0).edit().putString(REMOTE_PLAN_ID_KEY, id).apply()
        }
    }

    /** Converts the graph returned by iOS's `/sync/nutrition-plans` endpoint to Android's local plan. */
    fun importRemotePlan(context: Context, remote: JSONObject) {
        val days = remote.optJSONArray("days") ?: return
        rememberRemotePlanId(context, remote.optString("id"))
        val plan = buildList {
            for (index in 0 until days.length()) {
                val day = days.optJSONObject(index) ?: continue
                if (!day.isNull("deleted_at")) continue
                val date = parseRemoteNutritionDate(day.optString("date")) ?: continue
                val meals = day.optJSONArray("meals")?.let { entries -> buildList {
                    for (mealIndex in 0 until entries.length()) {
                        val meal = entries.optJSONObject(mealIndex) ?: continue
                        if (!meal.isNull("deleted_at")) continue
                        val foods = meal.optJSONArray("example_foods")?.let { foodsJson -> buildList {
                            for (foodIndex in 0 until foodsJson.length()) {
                                val food = foodsJson.optJSONObject(foodIndex) ?: continue
                                food.optString("name").trim().takeIf(String::isNotBlank)?.let {
                                    add(it to food.optInt("amountGrams", food.optInt("grams")).coerceAtLeast(0))
                                }
                            }
                        } }.orEmpty()
                        add(PlannedMeal(
                            title = meal.cleanString("title", "Comida"),
                            type = mealTypeFromRemote(meal.optString("meal_type")),
                            targets = NutritionTargets(meal.optDouble("target_calories").toInt(), meal.optDouble("target_protein_grams").toInt(), meal.optDouble("target_carbs_grams").toInt(), meal.optDouble("target_fat_grams").toInt()),
                            guidance = meal.optString("guidance"), foods = foods,
                        ))
                    }
                } }.orEmpty()
                add(NutritionDayPlan(
                    date = date,
                    type = nutritionDayTypeFromRemote(day.optString("day_type")),
                    energyDemand = energyDemandFromRemote(day.optString("energy_demand")),
                    targets = NutritionTargets(day.optDouble("target_calories").toInt(), day.optDouble("target_protein_grams").toInt(), day.optDouble("target_carbs_grams").toInt(), day.optDouble("target_fat_grams").toInt()),
                    workoutTitle = day.cleanString("planned_workout_title"), notes = day.cleanString("notes"),
                    preWorkoutGuidance = day.cleanString("pre_workout_guidance").takeIf(String::isNotBlank),
                    postWorkoutGuidance = day.cleanString("post_workout_guidance").takeIf(String::isNotBlank), meals = meals,
                ))
            }
        }
        if (plan.isNotEmpty()) {
            saveGeneratedPlan(
                context,
                plan,
                NutritionPlanMetadata(
                    id = remote.cleanString("id").takeIf(String::isNotBlank),
                    startsOn = parseRemoteNutritionDate(remote.cleanString("starts_on")) ?: plan.minOfOrNull { it.date },
                    goal = fitnessGoalFromStoredValue(remote.cleanString("goal")),
                    bodyCompositionPhase = bodyCompositionPhaseFromStoredValue(remote.cleanString("body_composition_phase")),
                    dailyCalorieAverage = remote.optDouble("daily_calorie_average", Double.NaN)
                        .takeUnless(Double::isNaN)
                        ?: plan.take(7).map { it.targets.calories }.average(),
                    notes = remote.cleanString("notes").takeIf(String::isNotBlank),
                ),
            )
        }
    }

    /** Restores dietary preferences before Android's next push, matching iOS's initial snapshot. */
    fun importRemotePreferences(context: Context, remote: JSONObject) {
        savePreferences(context, NutritionPreferences(
            dietaryStyle = dietaryStyleDisplayName(remote.optString("dietary_style", "standard")),
            mealsPerDay = remote.optInt("meals_per_day_preference", 4).coerceIn(1, 8),
            wantsSuggestions = remote.optBoolean("wants_meal_suggestions", true),
            eatingWindowStartHour = remote.optionalHour("preferred_eating_window_start_hour"),
            eatingWindowEndHour = remote.optionalHour("preferred_eating_window_end_hour"),
            cookingEffort = cookingEffortDisplayName(remote.optString("cooking_effort", "medium")),
            budgetSensitivity = budgetSensitivityDisplayName(remote.optString("budget_sensitivity", "medium")),
            preferredProteinSources = remote.stringList("preferred_protein_sources"), dislikes = remote.stringList("dislikes"),
            excludedFoods = remote.stringList("excluded_foods"), allergiesAndIntolerances = remote.stringList("allergies_and_intolerances"),
            notes = remote.optString("notes"),
        ))
    }

    /**
     * Restores the food log graph returned by iOS's batch sync.  Android stores
     * a compact local representation, so entries and their items are folded
     * into one MealLog while retaining a deterministic local id for later syncs.
     */
    fun importRemoteLogs(
        context: Context,
        remoteEntries: JSONArray,
        remoteItems: JSONArray,
        plannedMealKeys: Map<String, String> = emptyMap(),
    ) {
        // Pull pages are incremental: keep item changes by their iOS
        // orderIndex so a one-item edit never drops the other foods locally.
        val itemChangesByEntry = mutableMapOf<String, MutableMap<Int, MealItem?>>()
        for (index in 0 until remoteItems.length()) {
            val item = remoteItems.optJSONObject(index) ?: continue
            val entryId = item.optString("nutrition_log_entry_id")
            if (entryId.isBlank()) continue
            val orderIndex = item.optInt("order_index", index).coerceAtLeast(0)
            val changes = itemChangesByEntry.getOrPut(entryId) { mutableMapOf() }
            if (!item.isNull("deleted_at")) {
                changes[orderIndex] = null
                continue
            }
            changes[orderIndex] = MealItem(
                name = item.optString("name").trim().ifBlank { "Alimento" },
                calories = item.optDouble("calories").toInt().coerceIn(0, 8000),
                protein = item.optDouble("protein_grams").toInt().coerceIn(0, 500),
                carbs = item.optDouble("carbs_grams").toInt().coerceIn(0, 1000),
                fat = item.optDouble("fat_grams").toInt().coerceIn(0, 500),
                brand = item.optString("brand"),
                quantity = item.optDouble("quantity", 1.0).coerceAtLeast(0.0),
                unit = item.optString("unit"),
                grams = item.optDouble("amount_grams", Double.NaN).takeUnless(Double::isNaN),
                notes = item.optString("notes"),
            )
        }

        val merged = all(context).associateBy { it.id }.toMutableMap()
        var changed = false
        for (index in 0 until remoteEntries.length()) {
            val entry = remoteEntries.optJSONObject(index) ?: continue
            val remoteId = entry.optString("id")
            if (remoteId.isBlank()) continue
            val localId = stableRemoteLogId(remoteId, index)
            if (!entry.isNull("deleted_at")) {
                changed = merged.remove(localId) != null || changed
                continue
            }
            val loggedDate = runCatching { LocalDate.parse(entry.optString("logged_at").take(10)) }.getOrNull() ?: continue
            // Pull responses are incremental. When an entry changes without its
            // items in the same page, retain the already imported item graph.
            val existing = merged[localId]
            val items = existing?.items.orEmpty().toMutableList()
            itemChangesByEntry[remoteId]?.toSortedMap()?.forEach { (itemIndex, changedItem) ->
                if (changedItem == null) {
                    if (itemIndex in items.indices) items.removeAt(itemIndex)
                } else if (itemIndex in items.indices) {
                    items[itemIndex] = changedItem
                } else {
                    // New logs carry their complete ordered item list. A sparse
                    // later page can only append after the known local items.
                    items.add(changedItem)
                }
            }
            merged[localId] = MealLog(
                id = localId,
                name = entry.optString("title").trim().ifBlank { "Comida" },
                calories = if (items.isEmpty()) existing?.calories ?: 0 else items.sumOf { it.calories }.coerceIn(0, 8000),
                protein = if (items.isEmpty()) existing?.protein ?: 0 else items.sumOf { it.protein }.coerceIn(0, 500),
                carbs = if (items.isEmpty()) existing?.carbs ?: 0 else items.sumOf { it.carbs }.coerceIn(0, 1000),
                fat = if (items.isEmpty()) existing?.fat ?: 0 else items.sumOf { it.fat }.coerceIn(0, 500),
                type = mealTypeFromRemote(entry.optString("meal_type")),
                loggedDate = loggedDate,
                notes = entry.optString("notes"),
                source = "sync",
                isBookmarked = entry.optBoolean("is_favorite", false),
                items = items,
                photoPath = existing?.photoPath,
                plannedMealKey = plannedMealKeys[entry.optString("nutrition_meal_id")],
                bookmarkName = existing?.bookmarkName.orEmpty(),
                mediaUrl = existing?.mediaUrl,
            )
            changed = true
        }
        if (changed) write(context, merged.values.toList())
    }

    fun clearGeneratedPlan(context: Context) {
        context.getSharedPreferences(PREFS, 0).edit()
            .remove(GENERATED_PLAN_KEY)
            .remove(PLAN_METADATA_KEY)
            .apply()
        NutritionStatusWidget.refresh(context)
    }

    fun loadTargets(context: Context, profile: OnboardingProfile? = null): NutritionTargets = runCatching {
        val stored = context.getSharedPreferences(PREFS, 0).getString(TARGETS_KEY, null)
        if (stored == null && profile != null) return@runCatching targetsFor(profile)
        JSONObject(stored ?: "{}").toTargets(NutritionTargets())
    }.getOrDefault(NutritionTargets())

    fun saveTargets(context: Context, targets: NutritionTargets) {
        context.getSharedPreferences(PREFS, 0).edit().putString(TARGETS_KEY, targets.toJson().toString()).apply()
        NutritionStatusWidget.refresh(context)
    }

    fun loadPreferences(context: Context): NutritionPreferences = runCatching {
        val own = JSONObject(context.getSharedPreferences(PREFS, 0).getString(PROFILE_KEY, "{}") ?: "{}")
        val profile = JSONObject(context.getSharedPreferences("wildforce_profile_details", 0).getString("nutrition_profile", "{}") ?: "{}")
        // The dedicated Android Profile screen and the Nutrition hub edit the
        // same profile. Its configured snapshot is authoritative so an edit in
        // either surface is immediately reflected by the other, like iOS.
        val json = if (profile.optBoolean("isConfigured", false)) profile else own
        NutritionPreferences(
            dietaryStyle = dietaryStyleDisplayName(json.optString("dietaryStyle", "standard")),
            mealsPerDay = json.optInt("mealsPerDay", 4).coerceIn(1, 8),
            wantsSuggestions = json.optBoolean("wantsMealSuggestions", json.optBoolean("wantsSuggestions", true)),
            eatingWindowStartHour = json.optionalHour("eatingWindowStartHour"),
            eatingWindowEndHour = json.optionalHour("eatingWindowEndHour"),
            cookingEffort = cookingEffortDisplayName(json.optString("cookingEffort", "medium")),
            budgetSensitivity = budgetSensitivityDisplayName(json.optString("budgetSensitivity", "medium")),
            preferredProteinSources = json.stringList("preferredProteinSources"),
            dislikes = json.stringList("dislikes"),
            excludedFoods = json.stringList("excludedFoods"),
            allergiesAndIntolerances = json.stringList("allergiesAndIntolerances"),
            notes = json.optString("notes"),
        )
    }.getOrDefault(NutritionPreferences())

    /**
     * iOS creates a NutritionProfile during its nutrition onboarding and never
     * generates a plan until that record exists.  Defaults are useful for
     * rendering an empty state, but must not be mistaken for a user choice.
     */
    fun hasNutritionProfile(context: Context): Boolean = runCatching {
        val own = JSONObject(context.getSharedPreferences(PREFS, 0).getString(PROFILE_KEY, "{}") ?: "{}")
        if (own.has("dietaryStyle") || own.has("mealsPerDay")) return@runCatching true

        val onboarding = JSONObject(
            context.getSharedPreferences("wildforce_profile_details", 0)
                .getString("nutrition_profile", "{}") ?: "{}",
        )
        onboarding.optBoolean("isConfigured", false)
    }.getOrDefault(false)

    fun savePreferences(context: Context, value: NutritionPreferences) {
        val json = JSONObject()
            .put("dietaryStyle", dietaryStyleDisplayName(value.dietaryStyle))
            .put("mealsPerDay", value.mealsPerDay)
            .put("wantsSuggestions", value.wantsSuggestions)
            .put("eatingWindowStartHour", value.eatingWindowStartHour)
            .put("eatingWindowEndHour", value.eatingWindowEndHour)
            .put("cookingEffort", cookingEffortDisplayName(value.cookingEffort))
            .put("budgetSensitivity", budgetSensitivityDisplayName(value.budgetSensitivity))
            .put("preferredProteinSources", JSONArray(value.preferredProteinSources))
            .put("dislikes", JSONArray(value.dislikes))
            .put("excludedFoods", JSONArray(value.excludedFoods))
            .put("allergiesAndIntolerances", JSONArray(value.allergiesAndIntolerances))
            .put("notes", value.notes)
        context.getSharedPreferences(PREFS, 0).edit().putString(PROFILE_KEY, json.toString()).apply()
        // ProfileScreen lives in the onboarding feature module and owns this
        // view of the same preferences. Keep both stores in lockstep until
        // the app has a shared repository for these settings.
        context.getSharedPreferences("wildforce_profile_details", 0).edit()
            .putString("nutrition_profile", JSONObject(json.toString())
                .put("isConfigured", true)
                .put("wantsMealSuggestions", value.wantsSuggestions)
                .toString())
            .apply()
        NutritionStatusWidget.refresh(context)
    }

    fun load(context: Context, date: LocalDate = LocalDate.now()): List<MealLog> {
        val everyEntry = all(context)
        val forDate = everyEntry.filter { it.loggedDate == date }.sortedBy { it.id }
        // A locally-created meal can come back from the remote pull with a
        // deterministic sync id. Prefer the local entry so one banana (or any
        // identical food) is not counted twice after synchronization.
        val deduped = forDate.filterNot { candidate ->
            candidate.source == "sync" && forDate.any { local ->
                local.source != "sync" && local.name.trim().equals(candidate.name.trim(), ignoreCase = true) &&
                    local.calories == candidate.calories && local.protein == candidate.protein &&
                    local.carbs == candidate.carbs && local.fat == candidate.fat && local.notes.trim() == candidate.notes.trim()
            }
        }
        if (deduped.size != forDate.size) {
            write(context, everyEntry.filterNot { it.loggedDate == date } + deduped)
        }
        return deduped
    }

    fun recent(context: Context, limit: Int = 12): List<MealLog> = all(context).sortedByDescending { it.id }
        .distinctBy { listOf(it.name.lowercase(), it.calories, it.protein, it.carbs, it.fat) }.take(limit)

    /** Returns every local entry so the account synchronizer can upload its complete history. */
    fun allForSync(context: Context): List<MealLog> = all(context)

    /** Attaches a private image downloaded for an existing server log without creating a new log. */
    fun attachRemotePhoto(context: Context, remoteEntryId: String, path: String) {
        val localId = stableRemoteLogId(remoteEntryId, 0)
        val entries = all(context)
        if (entries.none { it.id == localId }) return
        write(context, entries.map { entry -> if (entry.id == localId) entry.copy(photoPath = path) else entry })
    }

    fun save(context: Context, date: LocalDate, meals: List<MealLog>) {
        write(context, all(context).filterNot { it.loggedDate == date } + meals.map { it.copy(loggedDate = date) })
    }

    fun save(context: Context, meals: List<MealLog>) = save(context, LocalDate.now(), meals)

    fun weekPlan(profile: OnboardingProfile?, baseTargets: NutritionTargets, today: LocalDate = LocalDate.now(), preferences: NutritionPreferences = NutritionPreferences(), version: Int = 1): List<NutritionDayPlan> {
        val monday = today.minusDays((today.dayOfWeek.value - DayOfWeek.MONDAY.value).toLong())
        return (0L..6L).map { offset ->
            val date = monday.plusDays(offset)
            val workoutDay = profile?.workoutDays?.any { it.toDayOfWeek() == date.dayOfWeek } == true
            val recovery = !workoutDay && date.dayOfWeek == DayOfWeek.SUNDAY
            val demand = when { workoutDay -> EnergyDemand.High; recovery -> EnergyDemand.Low; else -> EnergyDemand.Medium }
            val delta = when (demand) { EnergyDemand.High -> 180; EnergyDemand.Medium -> 0; EnergyDemand.Low -> -160 }
            val targets = baseTargets.copy(calories = (baseTargets.calories + delta).coerceAtLeast(1200), carbs = (baseTargets.carbs + delta / 4).coerceAtLeast(80))
            NutritionDayPlan(
                date,
                when { workoutDay -> NutritionDayType.Training; recovery -> NutritionDayType.Recovery; else -> NutritionDayType.Rest },
                demand,
                targets,
                if (workoutDay) workoutTitle(profile, date.dayOfWeek) else if (recovery) "Recuperación activa" else "Día de descanso",
                if (workoutDay) "Concentra una parte mayor de los carbohidratos alrededor del entrenamiento." else "Prioriza alimentos saciantes, hidratación y recuperación.",
                if (workoutDay) "Toma carbohidratos fáciles de digerir y proteína magra 60–90 minutos antes." else null,
                if (workoutDay) "Prioriza proteína y carbohidratos en la primera comida después de entrenar." else null,
                if (preferences.wantsSuggestions) plannedMeals(targets, workoutDay, preferences, version) else emptyList(),
            )
        }
    }

    private fun all(context: Context): List<MealLog> {
        val prefs = context.getSharedPreferences(PREFS, 0)
        val raw = prefs.getString(ENTRIES_KEY, null) ?: prefs.getString(LEGACY_KEY, "[]") ?: "[]"
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    val id = item.optLong("id", System.currentTimeMillis() + index)
                    val fallbackDate = Instant.ofEpochMilli(id).atZone(ZoneId.systemDefault()).toLocalDate()
                    val date = runCatching { LocalDate.parse(item.optString("date")) }.getOrDefault(fallbackDate)
                    val items = item.optJSONArray("items")?.let { array -> buildList { for (itemIndex in 0 until array.length()) { val food = array.getJSONObject(itemIndex); add(MealItem(food.cleanString("name"), food.optInt("calories"), food.optInt("protein"), food.optInt("carbs"), food.optInt("fat"), food.cleanString("brand"), food.optDouble("quantity", 1.0), food.cleanString("unit"), food.optDouble("grams", Double.NaN).takeUnless(Double::isNaN), food.cleanString("notes"))) } } }.orEmpty()
                    add(MealLog(id, item.cleanString("name").trim().ifBlank { "Comida" }, item.optInt("calories").coerceIn(0, 8000), item.optInt("protein").coerceIn(0, 500), item.optInt("carbs").coerceIn(0, 1000), item.optInt("fat").coerceIn(0, 500), runCatching { MealType.valueOf(item.cleanString("type")) }.getOrDefault(MealType.Snack), date, item.cleanString("notes"), item.cleanString("source", "manual"), item.optBoolean("bookmarked", false), items, item.cleanString("photoPath").takeIf(String::isNotBlank), item.cleanString("plannedMealKey").takeIf(String::isNotBlank), item.cleanString("bookmarkName"), item.cleanString("mediaUrl").takeIf(String::isNotBlank)))
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun write(context: Context, entries: List<MealLog>) {
        val array = JSONArray().apply {
            entries.sortedBy { it.id }.forEach { meal ->
                val itemArray = JSONArray().apply { meal.items.forEach { food -> put(JSONObject().put("name", food.name).put("calories", food.calories).put("protein", food.protein).put("carbs", food.carbs).put("fat", food.fat).put("brand", food.brand).put("quantity", food.quantity).put("unit", food.unit).put("grams", food.grams).put("notes", food.notes)) } }
                put(JSONObject().put("id", meal.id).put("date", meal.loggedDate.toString()).put("name", meal.name).put("calories", meal.calories).put("protein", meal.protein).put("carbs", meal.carbs).put("fat", meal.fat).put("type", meal.type.name).put("notes", meal.notes).put("source", meal.source).put("bookmarked", meal.isBookmarked).put("bookmarkName", meal.bookmarkName).put("items", itemArray).put("photoPath", meal.photoPath ?: JSONObject.NULL).put("plannedMealKey", meal.plannedMealKey ?: JSONObject.NULL).put("mediaUrl", meal.mediaUrl ?: JSONObject.NULL))
            }
        }
        context.getSharedPreferences(PREFS, 0).edit().putString(ENTRIES_KEY, array.toString()).remove(LEGACY_KEY).apply()
        NutritionStatusWidget.refresh(context)
        // Keep pending meal reminders in sync immediately after additions,
        // edits, deletions and incoming server changes.
        WildforceNotificationScheduler.scheduleNutritionReminder(context)
    }

    private fun targetsFor(profile: OnboardingProfile): NutritionTargets {
        val activity = when (profile.lifestyle.storedValue) { "sedentary" -> 28.0; "lightlyActive" -> 30.0; "veryActive" -> 35.0; else -> 32.0 }
        val adjustment = when (profile.bodyCompositionPhase) { BodyCompositionPhase.Bulk -> 250; BodyCompositionPhase.Cut -> -350; else -> 0 }
        val calories = (profile.weightKg * activity + adjustment).toInt().coerceIn(1200, 5000)
        val protein = (profile.weightKg * 1.8).toInt().coerceIn(60, 350)
        val fat = (calories * .25 / 9).toInt().coerceIn(30, 180)
        return NutritionTargets(calories, protein, ((calories - protein * 4 - fat * 9) / 4).coerceIn(0, 700), fat)
    }

    private fun workoutTitle(profile: OnboardingProfile?, day: DayOfWeek): String {
        val weekday = WorkoutWeekday.entries.firstOrNull { it.toDayOfWeek() == day }
        return weekday?.let { profile?.customWorkoutFocuses?.get(it)?.title } ?: "Entrenamiento"
    }

    private fun plannedMeals(targets: NutritionTargets, workoutDay: Boolean, preferences: NutritionPreferences, version: Int): List<PlannedMeal> = listOf(
        Triple("Desayuno equilibrado", MealType.Breakfast, .25),
        Triple(if (workoutDay) "Comida pre-entreno" else "Comida principal", MealType.Lunch, .35),
        Triple("Cena de recuperación", MealType.Dinner, .30),
        Triple("Snack proteico", MealType.Snack, .10),
    ).take(preferences.mealsPerDay.coerceIn(1, 4)).map { (title, type, ratio) ->
        val macros = NutritionTargets((targets.calories * ratio).toInt(), (targets.protein * ratio).toInt(), (targets.carbs * ratio).toInt(), (targets.fat * ratio).toInt())
        val foods = when (type) {
            MealType.Breakfast -> if (version % 2 == 0) listOf("Tortilla" to 150, "Pan integral" to 80, "Fruta" to 100) else listOf("Avena" to 80, "Yogur griego" to 150, "Frutos rojos" to 80)
            MealType.Lunch -> if (version % 2 == 0) listOf(if (preferences.dietaryStyle.contains("Veget", true) || preferences.dietaryStyle.contains("Veg", true)) "Tempeh" to 160 else "Pavo" to 160, "Quinoa" to 180, "Verduras" to 200) else listOf(if (preferences.dietaryStyle.contains("Veget", true) || preferences.dietaryStyle.contains("Veg", true)) "Tofu" to 160 else "Pollo" to 160, "Arroz" to 180, "Verduras" to 200)
            MealType.Dinner -> if (version % 2 == 0) listOf(if (preferences.dietaryStyle.contains("Vegana", true)) "Seitán" to 150 else "Merluza" to 150, "Boniato" to 250, "Ensalada" to 180) else listOf(if (preferences.dietaryStyle.contains("Vegana", true)) "Tofu" to 150 else "Salmón" to 150, "Patata" to 250, "Ensalada" to 180)
            MealType.Snack -> listOf("Yogur" to 180, "Fruta" to 120)
            MealType.PreWorkout -> listOf("Plátano" to 120, "Yogur griego" to 150)
            MealType.PostWorkout -> listOf("Batido de proteína" to 300, "Fruta" to 120)
        }
        val filteredFoods = foods.filterNot { (name, _) ->
            (preferences.excludedFoods + preferences.allergiesAndIntolerances + preferences.dislikes).any { blocked -> name.contains(blocked, true) || blocked.contains(name, true) }
        }
        PlannedMeal(title, type, macros, if (workoutDay && type == MealType.Lunch) "Aumenta aquí los carbohidratos para rendir en la sesión." else "Ajusta las cantidades manteniendo los objetivos.", filteredFoods)
    }
}

private fun stableRemoteLogId(value: String, fallback: Int): Long =
    value.hashCode().toLong().and(Long.MAX_VALUE).takeIf { it != 0L } ?: fallback.toLong() + 1

/** Android labels mirror the Spanish UI while the sync contract keeps iOS's raw enum values. */
internal fun dietaryStyleDisplayName(value: String): String = when (value.trim().lowercase()) {
    "vegetarian", "vegetariana" -> "Vegetariana"
    "vegan", "vegana" -> "Vegana"
    "pescatarian", "pescetarian", "pescetariana" -> "Pescetariana"
    else -> "Omnívora"
}

internal fun dietaryStyleSyncValue(value: String): String = when (dietaryStyleDisplayName(value)) {
    "Vegetariana" -> "vegetarian"
    "Vegana" -> "vegan"
    "Pescetariana" -> "pescatarian"
    else -> "standard"
}

internal fun cookingEffortDisplayName(value: String): String = when (value.trim().lowercase()) {
    "low", "bajo" -> "Bajo"
    "high", "alto" -> "Alto"
    else -> "Medio"
}

internal fun cookingEffortSyncValue(value: String): String = when (cookingEffortDisplayName(value)) {
    "Bajo" -> "low"
    "Alto" -> "high"
    else -> "medium"
}

internal fun budgetSensitivityDisplayName(value: String): String = when (value.trim().lowercase()) {
    "low", "baja" -> "Baja"
    "high", "alta" -> "Alta"
    else -> "Media"
}

internal fun budgetSensitivitySyncValue(value: String): String = when (budgetSensitivityDisplayName(value)) {
    "Baja" -> "low"
    "Alta" -> "high"
    else -> "medium"
}

private fun mealTypeFromRemote(value: String) = when (value.lowercase()) {
    "breakfast" -> MealType.Breakfast
    "lunch" -> MealType.Lunch
    "dinner" -> MealType.Dinner
    "preworkout", "pre_workout", "pre-workout", "pre workout" -> MealType.PreWorkout
    "postworkout", "post_workout", "post-workout", "post workout" -> MealType.PostWorkout
    else -> MealType.Snack
}

internal fun fitnessGoalFromStoredValue(value: String): FitnessGoal? {
    val normalized = value.trim()
    if (normalized.isBlank()) return null
    return FitnessGoal.entries.firstOrNull {
        it.storedValue.equals(normalized, ignoreCase = true) ||
            it.name.equals(normalized, ignoreCase = true) ||
            it.title.equals(normalized, ignoreCase = true)
    }
}

internal fun bodyCompositionPhaseFromStoredValue(value: String): BodyCompositionPhase? {
    val normalized = value.trim()
    if (normalized.isBlank()) return null
    return BodyCompositionPhase.entries.firstOrNull {
        it.storedValue.equals(normalized, ignoreCase = true) ||
            it.name.equals(normalized, ignoreCase = true) ||
            it.title.equals(normalized, ignoreCase = true)
    }
}

private fun nutritionDayTypeFromRemote(value: String) = when (value.lowercase()) {
    "training" -> NutritionDayType.Training; "recovery" -> NutritionDayType.Recovery; else -> NutritionDayType.Rest
}
private fun energyDemandFromRemote(value: String) = when (value.lowercase()) {
    "high" -> EnergyDemand.High; "low" -> EnergyDemand.Low; else -> EnergyDemand.Medium
}

private fun JSONObject.toTargets(fallback: NutritionTargets) = NutritionTargets(optInt("calories", fallback.calories).coerceIn(500, 8000), optInt("protein", fallback.protein).coerceIn(0, 500), optInt("carbs", fallback.carbs).coerceIn(0, 1000), optInt("fat", fallback.fat).coerceIn(0, 500))
private fun NutritionTargets.toJson() = JSONObject().put("calories", calories).put("protein", protein).put("carbs", carbs).put("fat", fat)
private fun WorkoutWeekday.toDayOfWeek(): DayOfWeek = DayOfWeek.of(ordinal + 1)
private fun JSONObject.optionalHour(key: String): Int? = takeUnless { isNull(key) }?.optInt(key)?.takeIf { it in 0..23 }
private fun JSONObject.stringList(key: String): List<String> = optJSONArray(key)?.let { values -> buildList { for (index in 0 until values.length()) values.optString(index).trim().takeIf(String::isNotBlank)?.let(::add) } } ?: emptyList()
private fun JSONArray?.stringPairs(): List<Pair<String, Int>> = this?.let { values -> buildList { for (index in 0 until values.length()) { val item = values.optJSONObject(index) ?: continue; item.optString("name").trim().takeIf(String::isNotBlank)?.let { add(it to item.optInt("grams").coerceAtLeast(0)) } } } } ?: emptyList()
