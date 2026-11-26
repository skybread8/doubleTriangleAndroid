package io.codepassion.doubletriangle.nutrition

import android.content.Context
import io.codepassion.doubletriangle.core.model.RemoteSyncChangeTracker
import io.codepassion.doubletriangle.NutritionStatusWidget
import io.codepassion.doubletriangle.WildforceNotificationScheduler
import io.codepassion.doubletriangle.feature.onboarding.BodyCompositionPhase
import io.codepassion.doubletriangle.feature.onboarding.FitnessGoal
import io.codepassion.doubletriangle.feature.onboarding.OnboardingProfile
import io.codepassion.doubletriangle.feature.onboarding.NutritionPlanProfileSnapshot
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
            in 7..11 -> Breakfast; in 13..16 -> Lunch; in 20..23 -> Dinner; else -> Snack
        }
    }
}

internal fun MealType.syncValue(): String = name.replaceFirstChar { it.lowercaseChar() }

internal data class NutritionTargets(val calories: Int = 2350, val protein: Int = 165, val carbs: Int = 250, val fat: Int = 75)

/** Mirrors iOS NutritionLogItem so reviewed food metadata survives local storage and sync. */
internal data class MealItem(
    val name: String,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val brand: String = "",
    val quantity: Double = 1.0,
    val unit: String = "",
    val grams: Double? = null,
    val notes: String = "",
    val remoteId: String? = null,
    val updatedAt: Instant? = null,
) {
    constructor(
        name: String,
        calories: Int,
        protein: Int,
        carbs: Int,
        fat: Int,
        brand: String = "",
        quantity: Double = 1.0,
        unit: String = "",
        grams: Double? = null,
        notes: String = "",
    ) : this(name, calories.toDouble(), protein.toDouble(), carbs.toDouble(), fat.toDouble(), brand, quantity, unit, grams, notes)
}

internal fun MealLog.loggedCalories(): Double = items.takeIf { it.isNotEmpty() }?.sumOf { it.calories } ?: calories.toDouble()
internal fun MealLog.loggedProtein(): Double = items.takeIf { it.isNotEmpty() }?.sumOf { it.protein } ?: protein.toDouble()
internal fun MealLog.loggedCarbs(): Double = items.takeIf { it.isNotEmpty() }?.sumOf { it.carbs } ?: carbs.toDouble()
internal fun MealLog.loggedFat(): Double = items.takeIf { it.isNotEmpty() }?.sumOf { it.fat } ?: fat.toDouble()

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
    val loggedAt: Instant? = null,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null,
)

/** Keep the modification time of unchanged rows when saving a whole day. */
internal fun stampEditedMeals(previous: List<MealLog>, current: List<MealLog>, now: Instant): List<MealLog> {
    val byId = previous.associateBy { it.id }
    return current.map { meal ->
        val old = byId[meal.id]
        val used = mutableSetOf<Int>()
        val foods = meal.items.mapIndexed { index, item ->
            val priorItems = old?.items.orEmpty()
            val byRemoteId = item.remoteId?.let { id -> priorItems.indices.firstOrNull { it !in used && priorItems[it].remoteId.equals(id, true) } }
            val byValue = priorItems.indices.filter { it !in used &&
                priorItems[it].copy(remoteId = null, updatedAt = null) == item.copy(remoteId = null, updatedAt = null) }.singleOrNull()
            val priorIndex = byRemoteId ?: byValue ?: index.takeIf { it in priorItems.indices && it !in used }
            val prior = priorIndex?.let { used += it; priorItems[it] }
            val sameValue = prior != null && prior.copy(remoteId = null, updatedAt = null) == item.copy(remoteId = null, updatedAt = null)
            item.copy(remoteId = item.remoteId ?: prior?.remoteId,
                updatedAt = if (sameValue && priorIndex == index) prior?.updatedAt else now)
        }
        fun MealLog.parentValue() = copy(items = emptyList(), calories = 0, protein = 0, carbs = 0, fat = 0, updatedAt = null)
        val parentTime = if (old != null && old.parentValue() == meal.parentValue()) old.updatedAt else now
        meal.copy(items = foods, updatedAt = parentTime)
    }
}

/** A newer local entry wins; equal live parents still allow independent item updates. */
internal fun shouldApplyNutritionEntry(localUpdatedAt: Instant?, remoteUpdatedAt: Instant?, deleted: Boolean): Boolean {
    if (remoteUpdatedAt == null) return false
    if (localUpdatedAt == null) return true
    return if (deleted) remoteUpdatedAt.isAfter(localUpdatedAt) else !remoteUpdatedAt.isBefore(localUpdatedAt)
}

internal fun mealLoggedAt(meal: MealLog, zone: ZoneId = ZoneId.systemDefault()): Instant =
    meal.loggedAt ?: Instant.ofEpochMilli(meal.id).takeIf { it.atZone(zone).toLocalDate() == meal.loggedDate }
    ?: meal.loggedDate.atTime(12, 0).atZone(zone).toInstant()

internal fun mealCreatedAt(meal: MealLog, zone: ZoneId = ZoneId.systemDefault()): Instant =
    meal.createdAt ?: mealLoggedAt(meal, zone)

internal fun orderNutritionMealsForDay(meals: List<MealLog>): List<MealLog> = meals.sortedBy { mealLoggedAt(it) }
internal fun orderRecentNutritionMeals(meals: List<MealLog>): List<MealLog> = meals.sortedByDescending { mealCreatedAt(it) }

/** Match iOS localizedStandardContains for titles and individual food names. */
internal fun matchesRecentMealSearch(entry: MealLog, query: String): Boolean {
    if (query.isEmpty()) return true
    fun normalized(value: String): String = java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
        .filterNot { Character.getType(it) == Character.NON_SPACING_MARK.toInt() }
        .lowercase(java.util.Locale.getDefault())
    val needle = normalized(query)
    return normalized(entry.name).contains(needle) || entry.items.any { normalized(it.name).contains(needle) }
}

internal fun applyNutritionBookmark(entry: MealLog, name: String?): MealLog =
    if (name == null) entry.copy(isBookmarked = false, bookmarkName = "")
    else entry.copy(name = name.trim().ifBlank { entry.name }, isBookmarked = true, bookmarkName = name.trim())

internal data class RemoteNutritionItem(val order: Int, val item: MealItem, val deleted: Boolean)

internal data class NutritionItemMergeResult(val items: List<MealItem>, val resolvedDeletions: Set<String>)

/** iOS reconciles every food row independently by UUID and updatedAt. */
internal fun mergeNutritionItems(
    local: List<MealItem>, remote: List<RemoteNutritionItem>, pendingDeletions: Map<String, Instant>,
): NutritionItemMergeResult {
    val merged = local.toMutableList()
    val positions = local.mapIndexedNotNull { index, item -> item.remoteId?.lowercase()?.let { it to index } }.toMap().toMutableMap()
    val order = local.mapIndexedNotNull { index, item -> item.remoteId?.lowercase()?.let { it to index } }.toMap().toMutableMap()
    val resolved = mutableSetOf<String>()
    val removed = mutableSetOf<String>()
    remote.forEach { row ->
        val id = row.item.remoteId?.lowercase() ?: return@forEach
        val remoteTime = row.item.updatedAt ?: return@forEach
        val pendingTime = pendingDeletions[id]
        if (pendingTime != null) {
            if (!remoteTime.isAfter(pendingTime)) return@forEach
            resolved += id
        }
        val index = positions[id]
        if (index == null) {
            if (!row.deleted) {
                positions[id] = merged.size
                order[id] = row.order
                merged += row.item
            }
        } else {
            val localTime = merged[index].updatedAt
            if (localTime == null || remoteTime.isAfter(localTime)) {
                if (row.deleted) removed += id
                else { merged[index] = row.item; order[id] = row.order }
            }
        }
    }
    return NutritionItemMergeResult(merged.filterNot { it.remoteId?.lowercase() in removed }
        .sortedBy { it.remoteId?.lowercase()?.let(order::get) ?: Int.MAX_VALUE }, resolved)
}

internal fun localNutritionLogId(remoteId: String, knownIds: Map<String, Long>): Long =
    knownIds[remoteId] ?: knownIds.entries.firstOrNull { it.key.equals(remoteId, ignoreCase = true) }?.value
    ?: stableRemoteLogId(remoteId.lowercase(java.util.Locale.ROOT))

/** The caller supplies the materialized server snapshot, including tombstones. */
internal fun activeRemoteNutritionItems(items: List<RemoteNutritionItem>): List<MealItem> =
    items.filterNot { it.deleted }.sortedBy { it.order }.map { it.item }

internal fun nutritionLogDate(value: String, zone: ZoneId = ZoneId.systemDefault()): LocalDate? {
    val text = value.trim()
    return runCatching { Instant.parse(text).atZone(zone).toLocalDate() }.getOrNull()
        ?: runCatching { java.time.OffsetDateTime.parse(text).atZoneSameInstant(zone).toLocalDate() }.getOrNull()
        ?: runCatching { LocalDate.parse(text) }.getOrNull()
}

internal fun nutritionLogTimestamp(date: LocalDate, previous: String?, localId: Long, zone: ZoneId = ZoneId.systemDefault()): String {
    if (previous != null && nutritionLogDate(previous, zone) == date && runCatching { Instant.parse(previous) }.isSuccess) return previous
    val created = runCatching { Instant.ofEpochMilli(localId) }.getOrNull()
    if (created != null && created.atZone(zone).toLocalDate() == date) return created.toString()
    return date.atTime(12, 0).atZone(zone).toInstant().toString()
}

internal fun nutritionLogTimestampForMeal(meal: MealLog, previous: String?, zone: ZoneId = ZoneId.systemDefault()): String =
    meal.loggedAt?.toString() ?: nutritionLogTimestamp(meal.loggedDate, previous, meal.id, zone)

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
    const val PENDING_DELETIONS_KEY = "nutrition_pending_entry_deletions_v1"
    const val PENDING_ITEM_DELETIONS_KEY = "nutrition_pending_item_deletions_v1"
    private const val LEGACY_KEY = "today_meals"
    private const val TARGETS_KEY = "nutrition_targets"
    private const val PROFILE_KEY = "nutrition_profile"
    private const val PLAN_VERSION_KEY = "nutrition_plan_version"
    private const val GENERATED_PLAN_KEY = "nutrition_generated_plan"
    private const val PLAN_METADATA_KEY = "nutrition_plan_metadata"
    private const val REMOTE_PLAN_ID_KEY = "nutrition_remote_plan_id"
    private const val DISMISSED_PLANNED_MEALS_KEY = "nutrition_dismissed_planned_meals"
    const val PLAN_RECORD_VERSIONS_KEY = "nutrition_plan_record_versions_v1"
    const val DISMISSED_PLANNED_MEAL_DATES_KEY = "nutrition_dismissed_planned_meal_dates_v1"

    fun plannedMealKey(date: LocalDate, meal: PlannedMeal): String =
        "${date}|${meal.type.name}|${meal.title.trim().lowercase()}"

    fun dismissPlannedMeal(context: Context, date: LocalDate, meal: PlannedMeal) {
        val prefs = context.getSharedPreferences(PREFS, 0)
        val key = plannedMealKey(date, meal)
        val dismissed = prefs.getStringSet(DISMISSED_PLANNED_MEALS_KEY, emptySet()).orEmpty() + key
        val timestamp = Instant.now().toString()
        val versions = prefs.getString(PLAN_RECORD_VERSIONS_KEY, "{}")?.let(::JSONObject) ?: JSONObject()
        val dates = prefs.getString(DISMISSED_PLANNED_MEAL_DATES_KEY, "{}")?.let(::JSONObject) ?: JSONObject()
        versions.put("meal:$key", timestamp)
        dates.put(key, timestamp)
        prefs.edit().putStringSet(DISMISSED_PLANNED_MEALS_KEY, dismissed)
            .putString(PLAN_RECORD_VERSIONS_KEY, versions.toString())
            .putString(DISMISSED_PLANNED_MEAL_DATES_KEY, dates.toString()).apply()
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

    fun saveGeneratedPlan(context: Context, plan: List<NutritionDayPlan>, metadata: NutritionPlanMetadata? = null, remoteVersions: JSONObject? = null) {
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
        val preferences = context.getSharedPreferences(PREFS, 0)
        val versions = remoteVersions ?: updateNutritionPlanVersions(
            preferences.getString(GENERATED_PLAN_KEY, null), preferences.getString(PLAN_METADATA_KEY, null),
            days, metadataJson, preferences.getString(PLAN_RECORD_VERSIONS_KEY, null), Instant.now().toString(),
        )
        preferences.edit()
            .putString(PLAN_RECORD_VERSIONS_KEY, versions.toString())
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
            val versions = JSONObject()
            fun rememberVersion(key: String, row: JSONObject) {
                row.cleanString("updated_at").takeIf { runCatching { Instant.parse(it) }.isSuccess }
                    ?.let { versions.put(key, it) }
            }
            rememberVersion("plan", remote)
            val dismissed = mutableSetOf<String>()
            val dismissalDates = JSONObject()
            for (index in 0 until days.length()) {
                val day = days.optJSONObject(index) ?: continue
                val date = parseRemoteNutritionDate(day.optString("date")) ?: continue
                rememberVersion("day:$date", day)
                val meals = day.optJSONArray("meals") ?: continue
                for (mealIndex in 0 until meals.length()) {
                    val meal = meals.optJSONObject(mealIndex) ?: continue
                    if (!meal.isNull("deleted_at")) continue
                    val key = "$date|${mealTypeFromRemote(meal.optString("meal_type")).name}|${meal.cleanString("title").trim().lowercase()}"
                    rememberVersion("meal:$key", meal)
                    meal.cleanString("discarded_at").takeIf { runCatching { Instant.parse(it) }.isSuccess }?.let {
                        dismissed += key
                        dismissalDates.put(key, it)
                    }
                }
            }
            context.getSharedPreferences(PREFS, 0).edit().putStringSet(DISMISSED_PLANNED_MEALS_KEY, dismissed)
                .putString(DISMISSED_PLANNED_MEAL_DATES_KEY, dismissalDates.toString()).apply()
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
                remoteVersions = versions,
            )
        }
    }

    /** Restores dietary preferences before Android's next push, matching iOS's initial snapshot. */
    fun importRemotePreferences(context: Context, remote: JSONObject) {
        NutritionPlanProfileSnapshot.ensureBaseline(context)
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
    @Synchronized
    fun importRemoteLogs(
        context: Context,
        remoteEntries: JSONArray,
        remoteItems: JSONArray,
        plannedMealKeys: Map<String, String> = emptyMap(),
        knownLocalIds: Map<String, Long> = emptyMap(),
        protectedEntryIds: Set<String> = emptySet(),
    ) {
        // The sync layer materializes all pages and cached records before this
        // import. Rebuild from live rows so tombstones cannot delete a new row
        // that happens to reuse the same order index.
        val itemsByEntry = mutableMapOf<String, MutableList<RemoteNutritionItem>>()
        val remoteItemParents = mutableMapOf<String, String>()
        for (index in 0 until remoteItems.length()) {
            val item = remoteItems.optJSONObject(index) ?: continue
            val entryId = item.optString("nutrition_log_entry_id")
            if (entryId.isBlank()) continue
            val itemId = item.optString("id").lowercase(java.util.Locale.ROOT)
            if (itemId.isNotBlank()) remoteItemParents[itemId] = entryId
            val orderIndex = item.optInt("order_index", index).coerceAtLeast(0)
            val mealItem = MealItem(
                name = item.optString("name").trim().ifBlank { "Alimento" },
                calories = item.optDouble("calories").coerceIn(0.0, 8000.0),
                protein = item.optDouble("protein_grams").coerceIn(0.0, 500.0),
                carbs = item.optDouble("carbs_grams").coerceIn(0.0, 1000.0),
                fat = item.optDouble("fat_grams").coerceIn(0.0, 500.0),
                brand = item.optString("brand"),
                quantity = item.optDouble("quantity", 1.0).coerceAtLeast(0.0),
                unit = item.optString("unit"),
                grams = item.optDouble("amount_grams", Double.NaN).takeUnless(Double::isNaN),
                notes = item.optString("notes"),
                remoteId = item.optString("id").takeIf(String::isNotBlank),
                updatedAt = runCatching { Instant.parse(item.optString("updated_at")) }.getOrNull(),
            )
            itemsByEntry.getOrPut(entryId.lowercase(java.util.Locale.ROOT)) { mutableListOf() }
                .add(RemoteNutritionItem(orderIndex, mealItem, !item.isNull("deleted_at")))
        }

        val merged = all(context).associateBy { it.id }.toMutableMap()
        val itemIds = context.getSharedPreferences("wildforce_remote_sync", Context.MODE_PRIVATE)
            .getString("nutrition_item_ids", "{}")?.let(::JSONObject) ?: JSONObject()
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val pendingItems = prefs.getString(PENDING_ITEM_DELETIONS_KEY, "{}")?.let(::JSONObject) ?: JSONObject()
        val pendingItemTimes = pendingItems.keys().asSequence().mapNotNull { id ->
            runCatching { Instant.parse(pendingItems.optString(id)) }.getOrNull()?.let { id.lowercase() to it }
        }.toMap()
        var changed = false
        for (index in 0 until remoteEntries.length()) {
            val entry = remoteEntries.optJSONObject(index) ?: continue
            val remoteId = entry.optString("id")
            if (remoteId.isBlank() || remoteId.lowercase() in protectedEntryIds) continue
            val localId = localNutritionLogId(remoteId, knownLocalIds)
            val remoteUpdatedAt = runCatching { Instant.parse(entry.optString("updated_at")) }.getOrNull()
            val deleted = !entry.isNull("deleted_at")
            val existing = merged[localId]
            if (remoteUpdatedAt == null) continue
            if (deleted && shouldApplyNutritionEntry(existing?.updatedAt, remoteUpdatedAt, true)) {
                changed = merged.remove(localId) != null || changed
                continue
            }
            if (deleted && existing == null) continue
            val loggedDate = nutritionLogDate(entry.optString("logged_at")) ?: continue
            val localItems = existing?.items.orEmpty().mapIndexed { itemIndex, food ->
                food.copy(remoteId = food.remoteId ?: itemIds.optString("$localId:$itemIndex").takeIf(String::isNotBlank))
            }.filter { food ->
                val parent = food.remoteId?.lowercase(java.util.Locale.ROOT)?.let(remoteItemParents::get)
                nutritionItemBelongsToParent(parent, remoteId)
            }
            val itemMerge = mergeNutritionItems(localItems, itemsByEntry[remoteId.lowercase(java.util.Locale.ROOT)].orEmpty(), pendingItemTimes)
            itemMerge.resolvedDeletions.forEach(pendingItems::remove)
            val items = itemMerge.items
            val remoteParent = MealLog(
                id = localId,
                name = entry.optString("title").trim().ifBlank { "Comida" },
                calories = items.sumOf { it.calories }.coerceIn(0.0, 8000.0).toInt(),
                protein = items.sumOf { it.protein }.coerceIn(0.0, 500.0).toInt(),
                carbs = items.sumOf { it.carbs }.coerceIn(0.0, 1000.0).toInt(),
                fat = items.sumOf { it.fat }.coerceIn(0.0, 500.0).toInt(),
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
                loggedAt = runCatching { Instant.parse(entry.optString("logged_at")) }.getOrNull(),
                createdAt = runCatching { Instant.parse(entry.optString("created_at")) }.getOrNull(),
                updatedAt = remoteUpdatedAt,
            )
            val parentWins = existing == null || existing.updatedAt == null || remoteUpdatedAt.isAfter(existing.updatedAt)
            merged[localId] = if (parentWins && !deleted) remoteParent else requireNotNull(existing).copy(
                items = items, calories = remoteParent.calories, protein = remoteParent.protein,
                carbs = remoteParent.carbs, fat = remoteParent.fat,
            )
            changed = true
        }
        prefs.edit().putString(PENDING_ITEM_DELETIONS_KEY, pendingItems.toString()).apply()
        if (changed) write(context, merged.values.toList())
    }

    fun clearGeneratedPlan(context: Context) {
        NutritionPlanProfileSnapshot.clear(context)
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

    /** A deleted iOS NutritionProfile is absent, so onboarding is offered again. */
    fun clearRemotePreferences(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(PROFILE_KEY).apply()
        context.getSharedPreferences("wildforce_profile_details", Context.MODE_PRIVATE).edit().remove("nutrition_profile").apply()
    }

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
        return orderNutritionMealsForDay(entriesByDate(context)[date].orEmpty())
    }

    fun recent(context: Context, limit: Int = Int.MAX_VALUE): List<MealLog> = orderRecentNutritionMeals(all(context)).take(limit)

    /** Returns every local entry so the account synchronizer can upload its complete history. */
    fun allForSync(context: Context): List<MealLog> = all(context)

    /** Adopt a cached server version only for legacy rows whose complete contents still match. */
    @Synchronized
    fun migrateMatchingRemoteVersions(context: Context, knownLocalIds: Map<String, Long>) {
        val state = context.getSharedPreferences("wildforce_sync_state", Context.MODE_PRIVATE)
        fun projection(resource: String): Map<String, JSONObject> {
            val array = state.getString("remote_projection:$resource", "[]")?.let(::JSONArray) ?: JSONArray()
            return (0 until array.length()).mapNotNull(array::optJSONObject)
                .filter { it.isNull("deleted_at") }.associateBy { it.optString("id").lowercase() }
        }
        val entries = projection("nutrition-log-entries")
        val remoteItems = projection("nutrition-log-items")
        val localToRemote = knownLocalIds.entries.associate { it.value to it.key.lowercase() }
        val ids = context.getSharedPreferences("wildforce_remote_sync", Context.MODE_PRIVATE)
            .getString("nutrition_item_ids", "{}")?.let(::JSONObject) ?: JSONObject()
        val original = all(context)
        val migrated = original.map { meal ->
            val parentId = localToRemote[meal.id]
            val parent = parentId?.let(entries::get)
            val loggedDate = parent?.optString("logged_at")?.let(::nutritionLogDate)
            val parentMatches = parent != null && meal.name == parent.optString("title").trim().ifBlank { "Comida" } &&
                meal.type == mealTypeFromRemote(parent.optString("meal_type")) && meal.loggedDate == loggedDate &&
                meal.notes == parent.cleanString("notes") && meal.isBookmarked == parent.optBoolean("is_favorite", false) &&
                meal.plannedMealKey == null && parent.isNull("nutrition_meal_id") &&
                meal.photoPath == null && meal.mediaUrl == null && parent.isNull("nutrition_log_media_id")
            val parentTime = if (meal.updatedAt == null && parentMatches) runCatching {
                Instant.parse(requireNotNull(parent).optString("updated_at"))
            }.getOrNull() else meal.updatedAt
            val foods = meal.items.mapIndexed { index, food ->
                val id = food.remoteId ?: ids.optString("${meal.id}:$index").takeIf(String::isNotBlank)
                val row = id?.lowercase()?.let(remoteItems::get)
                val sameParent = row?.optString("nutrition_log_entry_id")?.equals(parentId, true) == true
                val same = row != null && sameParent && row.optInt("order_index", index) == index &&
                    food.name == row.optString("name").trim().ifBlank { "Alimento" } && food.brand == row.cleanString("brand") &&
                    food.quantity == row.optDouble("quantity", 1.0) && food.unit == row.cleanString("unit") &&
                    food.grams == row.optDouble("amount_grams", Double.NaN).takeUnless(Double::isNaN) &&
                    food.calories == row.optDouble("calories") && food.protein == row.optDouble("protein_grams") &&
                    food.carbs == row.optDouble("carbs_grams") && food.fat == row.optDouble("fat_grams") && food.notes == row.cleanString("notes")
                val time = if (food.updatedAt == null && same) runCatching { Instant.parse(requireNotNull(row).optString("updated_at")) }.getOrNull() else food.updatedAt
                food.copy(remoteId = food.remoteId ?: id?.takeIf { same }, updatedAt = time)
            }
            meal.copy(items = foods, updatedAt = parentTime)
        }
        if (migrated != original) write(context, migrated)
    }

    /** Attaches a private image downloaded for an existing server log without creating a new log. */
    fun attachRemotePhoto(context: Context, remoteEntryId: String, path: String, knownLocalIds: Map<String, Long> = emptyMap()) {
        val localId = localNutritionLogId(remoteEntryId, knownLocalIds)
        val entries = all(context)
        if (entries.none { it.id == localId }) return
        write(context, entries.map { entry -> if (entry.id == localId) entry.copy(photoPath = path) else entry })
    }

    private fun stampLocalMeals(context: Context, existing: List<MealLog>, meals: List<MealLog>, now: Instant): List<MealLog> {
        val ids = context.getSharedPreferences("wildforce_remote_sync", Context.MODE_PRIVATE)
            .getString("nutrition_item_ids", "{}")?.let(::JSONObject) ?: JSONObject()
        val hydrated = existing.map { meal -> meal.copy(items = meal.items.mapIndexed { index, food ->
            food.copy(remoteId = food.remoteId ?: ids.optString("${meal.id}:$index").takeIf {
                runCatching { java.util.UUID.fromString(it) }.isSuccess
            })
        }) }
        val stamped = stampEditedMeals(hydrated, meals, now)
        val updatedById = stamped.associateBy { it.id }
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val deleted = prefs.getString(PENDING_ITEM_DELETIONS_KEY, "{}")?.let(::JSONObject) ?: JSONObject()
        hydrated.forEach { old ->
            val updated = updatedById[old.id] ?: return@forEach // deleting a parent keeps its children on iOS
            val surviving = updated.items.mapNotNull { it.remoteId?.lowercase() }.toSet()
            old.items.mapNotNull { it.remoteId?.lowercase() }.filterNot { it in surviving }.forEach { deleted.put(it, now.toString()) }
        }
        prefs.edit().putString(PENDING_ITEM_DELETIONS_KEY, deleted.toString()).apply()
        return stamped
    }

    @Synchronized
    fun save(context: Context, date: LocalDate, meals: List<MealLog>) {
        markNutritionChangesPending(context)
        val existing = all(context)
        val now = Instant.now()
        val remainingIds = meals.mapTo(mutableSetOf()) { it.id }
        val removed = existing.filter { it.loggedDate == date && it.id !in remainingIds }
        if (removed.isNotEmpty()) {
            val accountId = context.getSharedPreferences("wildforce_account", Context.MODE_PRIVATE).getString("id", null)
            if (accountId != null) {
                val idMap = context.getSharedPreferences("wildforce_remote_sync", Context.MODE_PRIVATE)
                    .getString("nutrition_entry_ids", "{}")?.let(::JSONObject) ?: JSONObject()
                val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                val pending = prefs.getString(PENDING_DELETIONS_KEY, "{}")?.let(::JSONObject) ?: JSONObject()
                removed.forEach { meal ->
                    val remoteId = idMap.optString(meal.id.toString()).takeIf {
                        runCatching { java.util.UUID.fromString(it) }.isSuccess
                    } ?: nutritionEntryId(accountId, meal.id)
                    pending.put(remoteId.lowercase(), now.toString())
                }
                prefs.edit().putString(PENDING_DELETIONS_KEY, pending.toString()).apply()
            }
        }
        val changed = stampLocalMeals(context, existing, meals.map { it.copy(loggedDate = date) }, now)
        write(context, existing.filterNot { it.loggedDate == date } + changed)
    }

    @Synchronized
    fun replace(context: Context, meal: MealLog) {
        markNutritionChangesPending(context)
        val existing = all(context)
        write(context, existing.filterNot { it.id == meal.id } + stampLocalMeals(context, existing, listOf(meal), Instant.now()))
    }

    fun save(context: Context, meals: List<MealLog>) = save(context, LocalDate.now(), meals)

    private fun markNutritionChangesPending(context: Context) {
        if (context.getSharedPreferences("wildforce_account", Context.MODE_PRIVATE).getString("id", null) != null) {
            RemoteSyncChangeTracker.mark(context, "nutrition-log-entries", "nutrition-log-items", "nutrition-log-media")
        }
    }

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

    // The hub asks for many dates while rendering. Parsing the entire JSON
    // history for each one makes its first frame increasingly expensive.
    private var cachedEntriesRaw: String? = null
    private var cachedEntriesZone: ZoneId? = null
    private var cachedEntries: List<MealLog> = emptyList()
    private var cachedEntriesByDate: Map<LocalDate, List<MealLog>> = emptyMap()

    @Synchronized
    private fun entriesByDate(context: Context): Map<LocalDate, List<MealLog>> {
        all(context)
        return cachedEntriesByDate
    }

    @Synchronized
    private fun all(context: Context): List<MealLog> {
        val prefs = context.getSharedPreferences(PREFS, 0)
        val raw = prefs.getString(ENTRIES_KEY, null) ?: prefs.getString(LEGACY_KEY, "[]") ?: "[]"
        val zone = ZoneId.systemDefault()
        if (raw == cachedEntriesRaw && zone == cachedEntriesZone) return cachedEntries
        val entries = runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    val id = item.optLong("id", System.currentTimeMillis() + index)
                    val fallbackDate = Instant.ofEpochMilli(id).atZone(zone).toLocalDate()
                    val date = runCatching { LocalDate.parse(item.optString("date")) }.getOrDefault(fallbackDate)
                    val items = item.optJSONArray("items")?.let { array -> buildList { for (itemIndex in 0 until array.length()) { val food = array.getJSONObject(itemIndex); add(MealItem(food.cleanString("name"), food.optDouble("calories"), food.optDouble("protein"), food.optDouble("carbs"), food.optDouble("fat"), food.cleanString("brand"), food.optDouble("quantity", 1.0), food.cleanString("unit"), food.optDouble("grams", Double.NaN).takeUnless(Double::isNaN), food.cleanString("notes"), food.cleanString("remoteId").takeIf(String::isNotBlank), food.cleanString("updatedAt").let { runCatching { Instant.parse(it) }.getOrNull() })) } } }.orEmpty()
                    add(MealLog(id, item.cleanString("name").trim().ifBlank { "Comida" }, item.optInt("calories").coerceIn(0, 8000), item.optInt("protein").coerceIn(0, 500), item.optInt("carbs").coerceIn(0, 1000), item.optInt("fat").coerceIn(0, 500), runCatching { MealType.valueOf(item.cleanString("type")) }.getOrDefault(MealType.Snack), date, item.cleanString("notes"), item.cleanString("source", "manual"), item.optBoolean("bookmarked", false), items, item.cleanString("photoPath").takeIf(String::isNotBlank), item.cleanString("plannedMealKey").takeIf(String::isNotBlank), item.cleanString("bookmarkName"), item.cleanString("mediaUrl").takeIf(String::isNotBlank), item.cleanString("loggedAt").let { runCatching { Instant.parse(it) }.getOrNull() }, item.cleanString("createdAt").let { runCatching { Instant.parse(it) }.getOrNull() }, item.cleanString("updatedAt").let { runCatching { Instant.parse(it) }.getOrNull() }))
                }
            }
        }.getOrDefault(emptyList())
        cachedEntriesRaw = raw
        cachedEntriesZone = zone
        cachedEntries = entries
        cachedEntriesByDate = entries.groupBy { it.loggedDate }
        return entries
    }

    private fun write(context: Context, entries: List<MealLog>) {
        val array = JSONArray().apply {
            entries.sortedBy { it.id }.forEach { meal ->
                val itemArray = JSONArray().apply { meal.items.forEach { food -> put(JSONObject().put("name", food.name).put("calories", food.calories).put("protein", food.protein).put("carbs", food.carbs).put("fat", food.fat).put("brand", food.brand).put("quantity", food.quantity).put("unit", food.unit).put("grams", food.grams).put("notes", food.notes).put("remoteId", food.remoteId ?: JSONObject.NULL).put("updatedAt", food.updatedAt?.toString() ?: JSONObject.NULL)) } }
                put(JSONObject().put("id", meal.id).put("date", meal.loggedDate.toString()).put("name", meal.name).put("calories", meal.calories).put("protein", meal.protein).put("carbs", meal.carbs).put("fat", meal.fat).put("type", meal.type.name).put("notes", meal.notes).put("source", meal.source).put("bookmarked", meal.isBookmarked).put("bookmarkName", meal.bookmarkName).put("items", itemArray).put("photoPath", meal.photoPath ?: JSONObject.NULL).put("plannedMealKey", meal.plannedMealKey ?: JSONObject.NULL).put("mediaUrl", meal.mediaUrl ?: JSONObject.NULL).put("loggedAt", meal.loggedAt?.toString() ?: JSONObject.NULL).put("createdAt", meal.createdAt?.toString() ?: JSONObject.NULL).put("updatedAt", meal.updatedAt?.toString() ?: JSONObject.NULL))
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

internal fun stableRemoteLogId(value: String): Long {
    val digest = java.security.MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
    val id = java.nio.ByteBuffer.wrap(digest).long and Long.MAX_VALUE
    return id.takeIf { it != 0L } ?: 1L
}

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

internal fun mealTypeFromRemote(value: String) = when (value.lowercase()) {
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

/** Store each plan row's edit time at save, not at the next network attempt. */
internal fun updateNutritionPlanVersions(previousDays: String?, previousMetadata: String?, days: JSONArray, metadata: JSONObject, oldVersions: String?, changedAt: String): JSONObject {
    fun rows(array: JSONArray, info: JSONObject): Map<String, String> {
        val result = linkedMapOf("plan" to info.toString())
        for (i in 0 until array.length()) {
            val day = array.optJSONObject(i) ?: continue
            val date = day.optString("date")
            val dayFields = JSONObject(day.toString()).apply { remove("meals") }
            result["day:$date"] = dayFields.toString()
            val meals = day.optJSONArray("meals") ?: continue
            for (j in 0 until meals.length()) {
                val meal = meals.optJSONObject(j) ?: continue
                val key = "$date|${meal.optString("type")}|${meal.optString("title").trim().lowercase()}"
                result["meal:$key"] = JSONObject(meal.toString()).put("_order", j).toString()
            }
        }
        return result
    }
    val old = if (previousDays == null) emptyMap() else rows(JSONArray(previousDays), previousMetadata?.let(::JSONObject) ?: JSONObject())
    val current = rows(days, metadata)
    val versions = oldVersions?.let(::JSONObject) ?: JSONObject()
    return JSONObject().apply {
        current.forEach { (key, value) ->
            val time = nutritionPlanRecordVersion(old[key], value, versions.optString(key).takeIf(String::isNotBlank), changedAt)
            if (time != null) put(key, time)
        }
    }
}

internal fun nutritionPlanRecordVersion(previous: String?, current: String, saved: String?, editedAt: String): String? =
    if (previous != current) editedAt else saved
