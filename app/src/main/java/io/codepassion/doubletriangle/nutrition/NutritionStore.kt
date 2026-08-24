package io.codepassion.doubletriangle.nutrition

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

internal enum class MealType(val title: String) { Breakfast("Desayuno"), Lunch("Comida"), Dinner("Cena"), Snack("Snack") }

internal data class MealLog(val id: Long, val name: String, val calories: Int, val protein: Int, val carbs: Int, val fat: Int, val type: MealType = MealType.Snack)

internal object NutritionStore {
    private const val PREFS = "wildforce_nutrition"
    private const val KEY = "today_meals"

    fun load(context: Context): List<MealLog> = runCatching {
        val raw = context.getSharedPreferences(PREFS, 0).getString(KEY, null) ?: return emptyList()
        val array = JSONArray(raw)
        buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                val type = runCatching { MealType.valueOf(item.optString("type")) }.getOrDefault(MealType.Snack)
                val meal = MealLog(item.getLong("id"), item.getString("name"), item.getInt("calories"), item.getInt("protein"), item.getInt("carbs"), item.getInt("fat"), type)
                if (isToday(meal.id)) add(meal)
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
    val today = Calendar.getInstance()
    val date = Calendar.getInstance().apply { timeInMillis = timestamp }
    return today.get(Calendar.YEAR) == date.get(Calendar.YEAR) && today.get(Calendar.DAY_OF_YEAR) == date.get(Calendar.DAY_OF_YEAR)
}
