package io.codepassion.doubletriangle.nutrition

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

internal data class MealLog(val id: Long, val name: String, val calories: Int, val protein: Int, val carbs: Int, val fat: Int)

internal object NutritionStore {
    private const val PREFS = "wildforce_nutrition"
    private const val KEY = "today_meals"

    fun load(context: Context): List<MealLog> = runCatching {
        val raw = context.getSharedPreferences(PREFS, 0).getString(KEY, null) ?: return emptyList()
        val array = JSONArray(raw)
        buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(MealLog(item.getLong("id"), item.getString("name"), item.getInt("calories"), item.getInt("protein"), item.getInt("carbs"), item.getInt("fat")))
            }
        }
    }.getOrDefault(emptyList())

    fun save(context: Context, meals: List<MealLog>) {
        val json = JSONArray().apply { meals.forEach { meal -> put(JSONObject().put("id", meal.id).put("name", meal.name).put("calories", meal.calories).put("protein", meal.protein).put("carbs", meal.carbs).put("fat", meal.fat)) } }
        context.getSharedPreferences(PREFS, 0).edit().putString(KEY, json.toString()).apply()
    }
}
