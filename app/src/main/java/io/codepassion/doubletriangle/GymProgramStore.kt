package io.codepassion.doubletriangle

import android.content.Context
import io.codepassion.doubletriangle.core.model.GymProgramSummary
import io.codepassion.doubletriangle.core.model.GymProgramWorkout
import io.codepassion.doubletriangle.core.model.WorkoutDaySummary
import io.codepassion.doubletriangle.core.model.activeGymProgram
import io.codepassion.doubletriangle.core.model.gymProgramWorkouts
import io.codepassion.doubletriangle.core.model.RemoteSyncChangeTracker
import org.json.JSONArray
import org.json.JSONObject

/** Local copy of server-owned gym workouts. Pending edits are kept until acknowledged. */
internal object GymProgramStore {
    const val NAME = "wildforce_gym_programs"
    const val ROOT = "root"
    const val VERSIONS = "versions"
    private const val PROGRAMS = "programs"
    private const val LINKS = "links"
    private const val OWNER = "owner"
    private const val DIRTY = "dirty"
    private fun prefs(context: Context) = context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
    private fun owner(context: Context) = context.getSharedPreferences("wildforce_account", 0).getString("id", null)

    data class View(val name: String, val workouts: List<WorkoutDaySummary>)

    @Synchronized fun view(context: Context): View? {
        val p = prefs(context)
        if (owner(context) == null || owner(context) != p.getString(OWNER, null)) return null
        val programs = JSONArray(p.getString(PROGRAMS, "[]") ?: "[]")
        val selected = activeGymProgram((0 until programs.length()).mapNotNull { index ->
            val row = programs.optJSONObject(index) ?: return@mapNotNull null
            GymProgramSummary(row.optString("id"), row.optString("name"), row.optString("status"), row.optString("updated_at"), !row.isNull("deleted_at"))
        }) ?: return null
        val root = p.getString(ROOT, null) ?: return View(selected.name, emptyList())
        val state = runCatching { WorkoutPlanGenerator.parse(root, "", "", allowEmpty = true) }.getOrNull() ?: return View(selected.name, emptyList())
        val links = JSONObject(p.getString(LINKS, "{}") ?: "{}")
        val rows = state.workouts.mapNotNull { workout ->
            val metadata = links.optJSONObject(workout.id) ?: return@mapNotNull null
            GymProgramWorkout(metadata.optString("program"), workout, metadata.optString("updated"), metadata.optBoolean("deleted"))
        }
        return View(selected.name, gymProgramWorkouts(selected.id, rows))
    }

    @Synchronized fun find(context: Context, id: String): WorkoutDaySummary? {
        val p = prefs(context)
        if (owner(context) == null || owner(context) != p.getString(OWNER, null)) return null
        return p.getString(ROOT, null)?.let { runCatching { WorkoutPlanGenerator.parse(it, "", "", allowEmpty = true).workouts.firstOrNull { day -> day.id == id } }.getOrNull() }
    }

    @Synchronized fun history(context: Context): List<WorkoutDaySummary> {
        val p = prefs(context)
        if (owner(context) == null || owner(context) != p.getString(OWNER, null)) return emptyList()
        val root = p.getString(ROOT, null) ?: return emptyList()
        return runCatching { WorkoutPlanGenerator.parse(root, "", "", allowEmpty = true).workouts.filter { it.status == io.codepassion.doubletriangle.core.model.WorkoutStatus.Completed } }.getOrDefault(emptyList())
    }

    @Synchronized fun import(context: Context, programs: JSONArray, root: String, links: JSONObject, versions: String) {
        val p = prefs(context)
        val account = owner(context) ?: return
        val sameOwner = account == p.getString(OWNER, null)
        val editor = p.edit().putString(OWNER, account).putString(PROGRAMS, programs.toString())
        if (!sameOwner || !p.getBoolean(DIRTY, false)) {
            editor.putString(ROOT, root).putString(LINKS, links.toString()).putString(VERSIONS, versions).putBoolean(DIRTY, false)
        }
        editor.apply()
    }

    @Synchronized fun save(context: Context, workout: WorkoutDaySummary) {
        val p = prefs(context)
        if (owner(context) == null || owner(context) != p.getString(OWNER, null)) return
        val old = p.getString(ROOT, null) ?: return
        val state = WorkoutPlanGenerator.parse(old, "", "", allowEmpty = true)
        if (state.workouts.none { it.id == workout.id }) return
        val current = WorkoutPlanGenerator.serialize(state.copy(workouts = state.workouts.map { if (it.id == workout.id) workout else it }))
        if (current == old) return
        val versions = WorkoutPlanVersions.update(old, current, p.getString(VERSIONS, null), java.time.Instant.now().toString())
        p.edit().putString(ROOT, current).putString(VERSIONS, versions).putBoolean(DIRTY, true).apply()
        RemoteSyncChangeTracker.mark(context, "workout-days", "workout-blocks", "planned-exercises", "exercise-results")
    }

    @Synchronized fun pending(context: Context): Pair<String, String>? {
        val p = prefs(context)
        if (owner(context) == null || owner(context) != p.getString(OWNER, null) || !p.getBoolean(DIRTY, false)) return null
        return (p.getString(ROOT, null) ?: return null) to (p.getString(VERSIONS, null) ?: return null)
    }

    @Synchronized fun acknowledge(context: Context, root: String, versions: String) {
        val p = prefs(context)
        if (p.getString(ROOT, null) == root && p.getString(VERSIONS, null) == versions) p.edit().putBoolean(DIRTY, false).apply()
    }
}
