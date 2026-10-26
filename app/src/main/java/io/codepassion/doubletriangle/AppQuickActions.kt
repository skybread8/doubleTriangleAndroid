package io.codepassion.doubletriangle

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.net.Uri
import io.codepassion.wildforce.android.R
import io.codepassion.doubletriangle.feature.workout.WorkoutSessionStore
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDate

/** Android launcher equivalent of iOS's dynamic UIApplication shortcut items. */
internal object AppQuickActions {
    private const val START_WORKOUT = "start-workout"
    private const val LOG_MEAL = "log-meal"
    private const val BODY_METRICS = "body-metrics"
    private const val LEAVING_FEEDBACK = "leaving-feedback"

    fun refresh(context: Context) {
        val manager = context.getSystemService(ShortcutManager::class.java) ?: return
        val shortcuts = buildList {
            if (hasWorkoutTarget(context)) {
                add(
                    shortcut(
                        context,
                        START_WORKOUT,
                        if (hasActiveWorkout(context)) R.string.ios_catalog_0f356e2ca3d2dcf7 else R.string.ios_catalog_cd370e18d2008193,
                        "wildforce://start-workout",
                        android.R.drawable.ic_media_play,
                    ),
                )
            }
            if (hasNutritionProfile(context)) {
                add(
                    shortcut(
                        context,
                        LOG_MEAL,
                        R.string.ios_catalog_a792ac2ed5679ded,
                        "wildforce://log-meal",
                        android.R.drawable.ic_menu_add,
                    ),
                )
            }
            add(
                shortcut(
                    context,
                    BODY_METRICS,
                    R.string.ios_catalog_3cedeefcebcf1fd8,
                    "wildforce://body-metrics",
                    android.R.drawable.ic_menu_edit,
                ),
            )
            add(
                shortcut(
                    context,
                    LEAVING_FEEDBACK,
                    R.string.ios_catalog_aac77df347205252,
                    "wildforce://leaving-feedback",
                    android.R.drawable.ic_menu_info_details,
                ),
            )
        }.take(4)
        manager.dynamicShortcuts = shortcuts
    }

    private fun shortcut(
        context: Context,
        id: String,
        labelRes: Int,
        route: String,
        iconRes: Int,
    ): ShortcutInfo = ShortcutInfo.Builder(context, id)
        .setShortLabel(context.getString(labelRes))
        .setIcon(Icon.createWithResource(context, iconRes))
        .setIntent(Intent(Intent.ACTION_VIEW, Uri.parse(route), context, MainActivity::class.java))
        .setRank(when (id) {
            START_WORKOUT -> 0
            LOG_MEAL -> 1
            BODY_METRICS -> 2
            else -> 3
        })
        .build()

    private fun hasNutritionProfile(context: Context): Boolean =
        context.getSharedPreferences("wildforce_profile_details", Context.MODE_PRIVATE)
            .getString("nutrition_profile", null)
            ?.let { runCatching { JSONObject(it).length() > 0 }.getOrDefault(false) }
            ?: false

    private fun hasWorkoutTarget(context: Context): Boolean {
        val preferences = context.getSharedPreferences("wildforce_profile", Context.MODE_PRIVATE)
        val workouts = runCatching {
            JSONObject(preferences.getString("workout_plan_json", null) ?: return false)
                .optJSONArray("workouts")
        }.getOrNull() ?: return false
        val todayDate = LocalDate.now()
        val today = DayOfWeek.from(todayDate).name
        return (0 until workouts.length()).any { index ->
            val workout = workouts.optJSONObject(index) ?: return@any false
            val id = workout.optString("id").takeIf(String::isNotBlank)
            id?.let { hasActiveWorkout(context, it) } == true ||
                isPlannedWorkoutForToday(workout, todayDate, today)
        }
    }

    /** Mirrors iOS WorkoutQuickActionResolver: active sessions win, otherwise
     * only a genuinely planned workout scheduled for today is offered. */
    internal fun isPlannedWorkoutForToday(workout: JSONObject, todayDate: LocalDate, todayWeekday: String = DayOfWeek.from(todayDate).name): Boolean {
        return isPlannedWorkoutForToday(
            statusValue = workout.optString("status"),
            scheduledForValue = workout.optString("scheduledFor"),
            weekdayValue = workout.optString("weekday"),
            todayDate = todayDate,
            todayWeekday = todayWeekday,
        )
    }

    internal fun isPlannedWorkoutForToday(statusValue: String, scheduledForValue: String, weekdayValue: String, todayDate: LocalDate, todayWeekday: String = DayOfWeek.from(todayDate).name): Boolean {
        val status = statusValue.trim()
        if (status.isNotBlank() && !status.equals("PLANNED", ignoreCase = true)) return false
        val scheduledFor = scheduledForValue.trim()
        if (scheduledFor.isNotBlank()) return scheduledFor.substringBefore('T') == todayDate.toString()
        return weekdayValue.trim().equals(todayWeekday, ignoreCase = true)
    }

    private fun hasActiveWorkout(context: Context): Boolean {
        val preferences = context.getSharedPreferences("wildforce_profile", Context.MODE_PRIVATE)
        val workouts = runCatching {
            JSONObject(preferences.getString("workout_plan_json", null) ?: return false)
                .optJSONArray("workouts")
        }.getOrNull() ?: return false
        return (0 until workouts.length()).any { index ->
            workouts.optJSONObject(index)?.optString("id")?.takeIf(String::isNotBlank)?.let {
                hasActiveWorkout(context, it)
            } == true
        }
    }

    private fun hasActiveWorkout(context: Context, workoutId: String): Boolean =
        WorkoutSessionStore.activeExerciseIndex(context, workoutId) != null
}
