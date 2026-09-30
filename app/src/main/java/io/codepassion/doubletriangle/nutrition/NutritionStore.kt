package io.codepassion.doubletriangle.nutrition

import android.content.Context
import io.codepassion.doubletriangle.feature.onboarding.BodyCompositionPhase
import io.codepassion.doubletriangle.feature.onboarding.OnboardingProfile
import io.codepassion.doubletriangle.feature.onboarding.WorkoutWeekday
import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

internal enum class MealType(val title: String) {
    Breakfast("Desayuno"), Lunch("Comida"), Dinner("Cena"), Snack("Snack");
    companion object {
        fun detect(): MealType = when (java.time.LocalTime.now().hour) {
            in 5..10 -> Breakfast; in 11..15 -> Lunch; in 18..22 -> Dinner; else -> Snack
        }
    }
}

internal data class NutritionTargets(val calories: Int = 2350, val protein: Int = 165, val carbs: Int = 250, val fat: Int = 75)

internal data class MealItem(val name: String, val calories: Int, val protein: Int, val carbs: Int, val fat: Int)

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
                val meals = day.optJSONArray("meals")?.let { array -> buildList { for (mealIndex in 0 until array.length()) { val meal = array.getJSONObject(mealIndex); add(PlannedMeal(meal.optString("title"), runCatching { MealType.valueOf(meal.optString("type")) }.getOrDefault(MealType.Snack), (meal.optJSONObject("targets") ?: JSONObject()).toTargets(NutritionTargets()), meal.optString("guidance"), meal.optJSONArray("foods")?.stringPairs().orEmpty())) } } }.orEmpty()
                add(NutritionDayPlan(LocalDate.parse(day.getString("date")), runCatching { NutritionDayType.valueOf(day.getString("type")) }.getOrDefault(NutritionDayType.Rest), runCatching { EnergyDemand.valueOf(day.getString("demand")) }.getOrDefault(EnergyDemand.Medium), (day.optJSONObject("targets") ?: JSONObject()).toTargets(NutritionTargets()), day.optString("workoutTitle"), day.optString("notes"), day.optString("pre").takeIf(String::isNotBlank), day.optString("post").takeIf(String::isNotBlank), meals))
            }
        }.takeIf { it.isNotEmpty() }
    }.getOrNull()

    fun saveGeneratedPlan(context: Context, plan: List<NutritionDayPlan>) {
        val days = JSONArray().apply { plan.forEach { day -> put(JSONObject().put("date", day.date.toString()).put("type", day.type.name).put("demand", day.energyDemand.name).put("targets", day.targets.toJson()).put("workoutTitle", day.workoutTitle).put("notes", day.notes).put("pre", day.preWorkoutGuidance).put("post", day.postWorkoutGuidance).put("meals", JSONArray().apply { day.meals.forEach { meal -> put(JSONObject().put("title", meal.title).put("type", meal.type.name).put("targets", meal.targets.toJson()).put("guidance", meal.guidance).put("foods", JSONArray().apply { meal.foods.forEach { (name, grams) -> put(JSONObject().put("name", name).put("grams", grams)) } })) } })) } }
        context.getSharedPreferences(PREFS, 0).edit().putString(GENERATED_PLAN_KEY, days.toString()).apply()
    }

    /** Converts the graph returned by iOS's `/sync/nutrition-plans` endpoint to Android's local plan. */
    fun importRemotePlan(context: Context, remote: JSONObject) {
        val days = remote.optJSONArray("days") ?: return
        val plan = buildList {
            for (index in 0 until days.length()) {
                val day = days.optJSONObject(index) ?: continue
                if (!day.isNull("deleted_at")) continue
                val date = runCatching { LocalDate.parse(day.optString("date").take(10)) }.getOrNull() ?: continue
                val meals = day.optJSONArray("meals")?.let { entries -> buildList {
                    for (mealIndex in 0 until entries.length()) {
                        val meal = entries.optJSONObject(mealIndex) ?: continue
                        if (!meal.isNull("deleted_at")) continue
                        val foods = meal.optJSONArray("example_foods")?.let { foodsJson -> buildList {
                            for (foodIndex in 0 until foodsJson.length()) {
                                val food = foodsJson.optJSONObject(foodIndex) ?: continue
                                food.optString("name").trim().takeIf(String::isNotBlank)?.let { add(it to food.optInt("grams").coerceAtLeast(0)) }
                            }
                        } }.orEmpty()
                        add(PlannedMeal(
                            title = meal.optString("title", "Comida"),
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
                    workoutTitle = day.optString("planned_workout_title"), notes = day.optString("notes"),
                    preWorkoutGuidance = day.optString("pre_workout_guidance").takeIf(String::isNotBlank),
                    postWorkoutGuidance = day.optString("post_workout_guidance").takeIf(String::isNotBlank), meals = meals,
                ))
            }
        }
        if (plan.isNotEmpty()) saveGeneratedPlan(context, plan)
    }

    /** Restores dietary preferences before Android's next push, matching iOS's initial snapshot. */
    fun importRemotePreferences(context: Context, remote: JSONObject) {
        savePreferences(context, NutritionPreferences(
            dietaryStyle = remote.optString("dietary_style", "Omnívora"),
            mealsPerDay = remote.optInt("meals_per_day_preference", 4).coerceIn(1, 8),
            wantsSuggestions = remote.optBoolean("wants_meal_suggestions", true),
            eatingWindowStartHour = remote.optionalHour("preferred_eating_window_start_hour"),
            eatingWindowEndHour = remote.optionalHour("preferred_eating_window_end_hour"),
            cookingEffort = remote.optString("cooking_effort", "Medio"),
            budgetSensitivity = remote.optString("budget_sensitivity", "Media"),
            preferredProteinSources = remote.stringList("preferred_protein_sources"), dislikes = remote.stringList("dislikes"),
            excludedFoods = remote.stringList("excluded_foods"), allergiesAndIntolerances = remote.stringList("allergies_and_intolerances"),
            notes = remote.optString("notes"),
        ))
    }

    fun clearGeneratedPlan(context: Context) {
        context.getSharedPreferences(PREFS, 0).edit().remove(GENERATED_PLAN_KEY).apply()
    }

    fun loadTargets(context: Context, profile: OnboardingProfile? = null): NutritionTargets = runCatching {
        val stored = context.getSharedPreferences(PREFS, 0).getString(TARGETS_KEY, null)
        if (stored == null && profile != null) return@runCatching targetsFor(profile)
        JSONObject(stored ?: "{}").toTargets(NutritionTargets())
    }.getOrDefault(NutritionTargets())

    fun saveTargets(context: Context, targets: NutritionTargets) {
        context.getSharedPreferences(PREFS, 0).edit().putString(TARGETS_KEY, targets.toJson().toString()).apply()
    }

    fun loadPreferences(context: Context): NutritionPreferences = runCatching {
        val own = JSONObject(context.getSharedPreferences(PREFS, 0).getString(PROFILE_KEY, "{}") ?: "{}")
        val profile = JSONObject(context.getSharedPreferences("wildforce_profile_details", 0).getString("nutrition_profile", "{}") ?: "{}")
        // A locally edited profile is authoritative. The onboarding profile is only a
        // migration/default source; otherwise later edits would appear not to save.
        val json = if (own.has("dietaryStyle") || own.has("mealsPerDay")) own
        else if (profile.optBoolean("isConfigured", false)) profile
        else own
        NutritionPreferences(
            dietaryStyle = json.optString("dietaryStyle", "Omnívora"),
            mealsPerDay = json.optInt("mealsPerDay", 4).coerceIn(1, 8),
            wantsSuggestions = json.optBoolean("wantsMealSuggestions", json.optBoolean("wantsSuggestions", true)),
            eatingWindowStartHour = json.optionalHour("eatingWindowStartHour"),
            eatingWindowEndHour = json.optionalHour("eatingWindowEndHour"),
            cookingEffort = json.optString("cookingEffort", "Medio"),
            budgetSensitivity = json.optString("budgetSensitivity", "Media"),
            preferredProteinSources = json.stringList("preferredProteinSources"),
            dislikes = json.stringList("dislikes"),
            excludedFoods = json.stringList("excludedFoods"),
            allergiesAndIntolerances = json.stringList("allergiesAndIntolerances"),
            notes = json.optString("notes"),
        )
    }.getOrDefault(NutritionPreferences())

    fun savePreferences(context: Context, value: NutritionPreferences) {
        val json = JSONObject()
            .put("dietaryStyle", value.dietaryStyle)
            .put("mealsPerDay", value.mealsPerDay)
            .put("wantsSuggestions", value.wantsSuggestions)
            .put("eatingWindowStartHour", value.eatingWindowStartHour)
            .put("eatingWindowEndHour", value.eatingWindowEndHour)
            .put("cookingEffort", value.cookingEffort)
            .put("budgetSensitivity", value.budgetSensitivity)
            .put("preferredProteinSources", JSONArray(value.preferredProteinSources))
            .put("dislikes", JSONArray(value.dislikes))
            .put("excludedFoods", JSONArray(value.excludedFoods))
            .put("allergiesAndIntolerances", JSONArray(value.allergiesAndIntolerances))
            .put("notes", value.notes)
        context.getSharedPreferences(PREFS, 0).edit().putString(PROFILE_KEY, json.toString()).apply()
    }

    fun load(context: Context, date: LocalDate = LocalDate.now()): List<MealLog> = all(context).filter { it.loggedDate == date }.sortedBy { it.id }

    fun recent(context: Context, limit: Int = 12): List<MealLog> = all(context).sortedByDescending { it.id }
        .distinctBy { listOf(it.name.lowercase(), it.calories, it.protein, it.carbs, it.fat) }.take(limit)

    /** Returns every local entry so the account synchronizer can upload its complete history. */
    fun allForSync(context: Context): List<MealLog> = all(context)

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
                    val items = item.optJSONArray("items")?.let { array -> buildList { for (itemIndex in 0 until array.length()) { val food = array.getJSONObject(itemIndex); add(MealItem(food.optString("name"), food.optInt("calories"), food.optInt("protein"), food.optInt("carbs"), food.optInt("fat"))) } } }.orEmpty()
                    add(MealLog(id, item.optString("name").trim().ifBlank { "Comida" }, item.optInt("calories").coerceIn(0, 8000), item.optInt("protein").coerceIn(0, 500), item.optInt("carbs").coerceIn(0, 1000), item.optInt("fat").coerceIn(0, 500), runCatching { MealType.valueOf(item.optString("type")) }.getOrDefault(MealType.Snack), date, item.optString("notes"), item.optString("source", "manual"), item.optBoolean("bookmarked", false), items))
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun write(context: Context, entries: List<MealLog>) {
        val array = JSONArray().apply {
            entries.sortedBy { it.id }.forEach { meal ->
                val itemArray = JSONArray().apply { meal.items.forEach { food -> put(JSONObject().put("name", food.name).put("calories", food.calories).put("protein", food.protein).put("carbs", food.carbs).put("fat", food.fat)) } }
                put(JSONObject().put("id", meal.id).put("date", meal.loggedDate.toString()).put("name", meal.name).put("calories", meal.calories).put("protein", meal.protein).put("carbs", meal.carbs).put("fat", meal.fat).put("type", meal.type.name).put("notes", meal.notes).put("source", meal.source).put("bookmarked", meal.isBookmarked).put("items", itemArray))
            }
        }
        context.getSharedPreferences(PREFS, 0).edit().putString(ENTRIES_KEY, array.toString()).remove(LEGACY_KEY).apply()
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
        }
        val filteredFoods = foods.filterNot { (name, _) ->
            (preferences.excludedFoods + preferences.allergiesAndIntolerances + preferences.dislikes).any { blocked -> name.contains(blocked, true) || blocked.contains(name, true) }
        }
        PlannedMeal(title, type, macros, if (workoutDay && type == MealType.Lunch) "Aumenta aquí los carbohidratos para rendir en la sesión." else "Ajusta las cantidades manteniendo los objetivos.", filteredFoods)
    }
}

private fun mealTypeFromRemote(value: String) = when (value.lowercase()) {
    "breakfast" -> MealType.Breakfast; "lunch" -> MealType.Lunch; "dinner" -> MealType.Dinner; else -> MealType.Snack
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
