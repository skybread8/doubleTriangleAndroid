package io.codepassion.doubletriangle.nutrition

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.time.LocalDate
import java.time.ZoneId

internal enum class MealType(val title: String) { Breakfast("Desayuno"), Lunch("Comida"), Dinner("Cena"), Snack("Snack") }

internal data class MealLog(val id: Long, val name: String, val calories: Int, val protein: Int, val carbs: Int, val fat: Int, val type: MealType = MealType.Snack)
internal data class NutritionTargets(val calories: Int = 2350, val protein: Int = 165, val carbs: Int = 250, val fat: Int = 75)

internal object NutritionStore {
    private const val PREFS = "wildforce_nutrition"
    private const val KEY = "today_meals"
    private const val TARGETS_KEY = "nutrition_targets"

    fun loadTargets(context: Context): NutritionTargets = runCatching {
        val item = JSONObject(context.getSharedPreferences(PREFS, 0).getString(TARGETS_KEY, "{}") ?: "{}")
        NutritionTargets(item.optInt("calories", 2350), item.optInt("protein", 165), item.optInt("carbs", 250), item.optInt("fat", 75))
    }.getOrDefault(NutritionTargets())

    fun saveTargets(context: Context, targets: NutritionTargets) {
        context.getSharedPreferences(PREFS, 0).edit().putString(TARGETS_KEY, JSONObject().put("calories", targets.calories).put("protein", targets.protein).put("carbs", targets.carbs).put("fat", targets.fat).toString()).apply()
    }

    fun load(context: Context, date: LocalDate = LocalDate.now()): List<MealLog> = runCatching {
        val raw = context.getSharedPreferences(PREFS, 0).getString(KEY, null) ?: return emptyList()
        val array = JSONArray(raw)
        buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                val type = runCatching { MealType.valueOf(item.optString("type")) }.getOrDefault(MealType.Snack)
                val meal = MealLog(item.getLong("id"), item.getString("name"), item.getInt("calories"), item.getInt("protein"), item.getInt("carbs"), item.getInt("fat"), type)
                if (isOnDate(meal.id, date)) add(meal)
            }
        }
    }.getOrDefault(emptyList())

    fun save(context: Context, meals: List<MealLog>) {
        val preferences = context.getSharedPreferences(PREFS, 0)
        val json = JSONArray().apply {
            val previous = runCatching { JSONArray(preferences.getString(KEY, "[]")) }.getOrDefault(JSONArray())
            for (index in 0 until previous.length()) {
                val item = previous.getJSONObject(index)
                if (!isToday(item.getLong("id"))) put(item)
            }
            meals.forEach { meal -> put(JSONObject().put("id", meal.id).put("name", meal.name).put("calories", meal.calories).put("protein", meal.protein).put("carbs", meal.carbs).put("fat", meal.fat).put("type", meal.type.name)) }
        }
        preferences.edit().putString(KEY, json.toString()).apply()
    }
}

private fun isToday(timestamp: Long): Boolean {
    return isOnDate(timestamp, LocalDate.now())
}

private fun isOnDate(timestamp: Long, date: LocalDate): Boolean {
    val today = Calendar.getInstance().apply { timeInMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() }
    val mealDate = Calendar.getInstance().apply { timeInMillis = timestamp }
    return today.get(Calendar.YEAR) == mealDate.get(Calendar.YEAR) && today.get(Calendar.DAY_OF_YEAR) == mealDate.get(Calendar.DAY_OF_YEAR)
}
