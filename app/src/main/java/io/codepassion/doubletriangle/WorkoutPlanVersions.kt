package io.codepassion.doubletriangle

import org.json.JSONArray
import org.json.JSONObject

/** Versions of individual plan rows, recorded when the local plan is saved. */
internal object WorkoutPlanVersions {
    const val KEY = "workout_plan_record_versions_v1"

    fun planKey(root: JSONObject): String = "plan:" + root.optString("planId").takeUnless { it.isBlank() || it == "null" }.orEmpty().ifBlank { root.optString("planName") }
    fun dayKey(root: JSONObject, day: JSONObject, index: Int) = planKey(root) + "/day:" + day.optString("id").ifBlank { index.toString() }
    fun blockKey(root: JSONObject, day: JSONObject, dayIndex: Int, block: JSONObject, index: Int) = dayKey(root, day, dayIndex) + "/block:" + block.optString("remoteId").takeUnless { it.isBlank() || it == "null" }.orEmpty().ifBlank { index.toString() }
    fun exerciseKey(root: JSONObject, day: JSONObject, dayIndex: Int, block: JSONObject, blockIndex: Int, exercise: JSONObject, index: Int) = blockKey(root, day, dayIndex, block, blockIndex) + "/exercise:" + exercise.optString("remoteId").takeUnless { it.isBlank() || it == "null" }.orEmpty().ifBlank { index.toString() }

    private fun rows(root: JSONObject): Map<String, String> {
        val rows = linkedMapOf<String, String>()
        fun add(key: String, row: JSONObject, position: Int?, vararg children: String) {
            val copy = JSONObject(row.toString())
            children.forEach(copy::remove)
            if (position != null) copy.put("_sync_order", position)
            rows[key] = copy.toString()
        }
        add(planKey(root), root, null, "workouts")
        val days = root.optJSONArray("workouts") ?: JSONArray()
        for (i in 0 until days.length()) {
            val day = days.optJSONObject(i) ?: continue
            add(dayKey(root, day, i), day, i, "blocks", "exercises")
            val blocks = day.optJSONArray("blocks") ?: JSONArray()
            for (j in 0 until blocks.length()) {
                val block = blocks.optJSONObject(j) ?: continue
                add(blockKey(root, day, i, block, j), block, j, "exercises")
                val exercises = block.optJSONArray("exercises") ?: JSONArray()
                for (k in 0 until exercises.length()) {
                    val exercise = exercises.optJSONObject(k) ?: continue
                    add(exerciseKey(root, day, i, block, j, exercise, k), exercise, k)
                }
            }
        }
        return rows
    }

    fun imported(root: JSONObject, byResource: Map<String, Map<String, String>>): String {
        val result = JSONObject()
        fun add(key: String, resource: String, id: String) {
            byResource[resource]?.get(id.lowercase(java.util.Locale.ROOT))?.let { result.put(key, it) }
        }
        add(planKey(root), "workout-plans", root.optString("planId"))
        val days = root.optJSONArray("workouts") ?: JSONArray()
        for (i in 0 until days.length()) {
            val day = days.optJSONObject(i) ?: continue
            add(dayKey(root, day, i), "workout-days", day.optString("id"))
            val blocks = day.optJSONArray("blocks") ?: JSONArray()
            for (j in 0 until blocks.length()) {
                val block = blocks.optJSONObject(j) ?: continue
                add(blockKey(root, day, i, block, j), "workout-blocks", block.optString("remoteId"))
                val exercises = block.optJSONArray("exercises") ?: JSONArray()
                for (k in 0 until exercises.length()) {
                    val exercise = exercises.optJSONObject(k) ?: continue
                    add(exerciseKey(root, day, i, block, j, exercise, k), "planned-exercises", exercise.optString("remoteId"))
                }
            }
        }
        return result.toString()
    }

    fun update(previous: String?, current: String, versions: String?, timestamp: String): String {
        val before = previous?.let { rows(JSONObject(it)) }.orEmpty()
        val after = rows(JSONObject(current))
        val stored = versions?.let(::JSONObject) ?: JSONObject()
        val result = JSONObject()
        after.forEach { (key, value) ->
            versionForChange(before[key], value, stored.optString(key).takeIf { it.isNotBlank() }, timestamp)
                ?.let { result.put(key, it) }
        }
        return result.toString()
    }
}

internal fun versionForChange(previous: String?, current: String, savedVersion: String?, changedAt: String): String? =
    if (previous != current) changedAt else savedVersion
