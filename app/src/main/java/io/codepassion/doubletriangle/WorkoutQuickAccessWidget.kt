package io.codepassion.doubletriangle

import io.codepassion.wildforce.android.R

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import io.codepassion.doubletriangle.feature.workout.cachedTrainingBitmap
import io.codepassion.doubletriangle.feature.workout.exerciseDisplayName
import io.codepassion.doubletriangle.feature.workout.exerciseImageUrl
import io.codepassion.doubletriangle.feature.workout.loadTrainingBitmap
import io.codepassion.doubletriangle.feature.workout.workoutCoverUrl
import io.codepassion.doubletriangle.feature.workout.WorkoutSessionStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.time.DayOfWeek

internal data class WidgetWorkout(
    val title: String,
    val detail: String,
    val status: String,
    val progress: Int?,
    val coverUrl: String? = null,
    val dayNumber: Int? = null,
    val meta: String? = null,
    val streakWarning: Boolean = false,
)
internal data class WidgetDayCandidate(val id: String, val weekday: String, val status: String, val completedAt: String? = null)
private val noPlanWidgetWorkout = WidgetWorkout("Sin plan", "Crea tu primer entrenamiento", "ENTRENAMIENTO", null)

internal fun selectWidgetDay(days: List<WidgetDayCandidate>, today: String, todayDate: java.time.LocalDate, activeId: String? = null): String? {
    val active = activeId?.let { id -> days.firstOrNull { it.id == id } }
    val completedToday = days.firstOrNull { day ->
        day.status.equals("COMPLETED", true) && day.completedAt?.let { raw ->
            runCatching { java.time.Instant.parse(raw).atZone(java.time.ZoneId.systemDefault()).toLocalDate() == todayDate }.getOrDefault(false)
        } == true
    }
    val plannedToday = days.firstOrNull { it.weekday.equals(today, true) && it.status.equals("PLANNED", true) }
    val anyToday = days.firstOrNull { it.weekday.equals(today, true) }
    return (active ?: completedToday ?: plannedToday ?: anyToday)?.id
}

internal fun fallbackWidgetWorkout(scheduledDays: Set<String>, today: String): WidgetWorkout =
    if (scheduledDays.any { it.equals(today, true) }) {
        WidgetWorkout("Entrenamiento planeado", "", "PLANIFICADO", null)
    } else {
        WidgetWorkout("Día de descanso", "Recupera y vuelve mañana", "DÍA DE DESCANSO", null)
    }

internal fun widgetShowsStatusBadge(status: String): Boolean =
    status.equals("COMPLETADO", true) || status.equals("OMITIDO", true)

internal fun widgetShowsStreakWarning(scheduledDays: Set<String>, today: String, completedToday: Boolean): Boolean =
    scheduledDays.any { it.equals(today, true) } && !completedToday

/** iOS uses the current exercise image while a workout is in progress and the
 * workout cover for planned/completed/rest-day states. */
internal fun workoutWidgetImageUrl(
    isInProgress: Boolean,
    currentExerciseImageKey: String?,
    focus: String,
    gender: String,
    workoutId: String,
): String = if (isInProgress) {
    exerciseImageUrl(currentExerciseImageKey, gender)
        ?: workoutCoverUrl(focus = focus, gender = gender, workoutId = workoutId)
} else {
    workoutCoverUrl(focus = focus, gender = gender, workoutId = workoutId)
}

/** The Android counterpart of iOS's Workout Status home-screen widget. */
class WorkoutQuickAccessWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        updateWidgets(context, manager, appWidgetIds)
    }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, appWidgetId: Int, newOptions: android.os.Bundle) {
        updateWidgets(context, manager, intArrayOf(appWidgetId))
    }

    companion object {
        private val imagePrefetchScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, WorkoutQuickAccessWidget::class.java)
            updateWidgets(context, manager, manager.getAppWidgetIds(component))
        }

        private fun updateWidgets(context: Context, manager: AppWidgetManager, widgetIds: IntArray) {
            val preferences = context.getSharedPreferences("wildforce_profile", Context.MODE_PRIVATE)
            val workout = workoutForWidget(context, preferences.getString("workout_plan_json", null))
            widgetIds.forEach { widgetId ->
                val options = manager.getAppWidgetOptions(widgetId)
                manager.updateAppWidget(widgetId, views(context, options, workout))

                // RemoteViews cannot fetch media itself. Render the cached
                // state immediately, then update the same widget after the
                // public cover/exercise image has been cached off-thread.
                workout.coverUrl?.let { url ->
                    imagePrefetchScope.launch {
                        if (loadTrainingBitmap(context, url) != null) {
                            manager.updateAppWidget(widgetId, views(context, options, workout))
                        }
                    }
                }
            }
        }

        private fun views(context: Context, options: android.os.Bundle, workout: WidgetWorkout): RemoteViews {
            val streak = context.getSharedPreferences("wildforce_profile", Context.MODE_PRIVATE)
                .getInt("current_streak", 0)
                .coerceAtLeast(0)
            val openApp = PendingIntent.getActivity(
                context,
                901,
                Intent(context, MainActivity::class.java).setData(Uri.parse("wildforce://workout")),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
            val layout = if (minWidth in 1 until 250) R.layout.widget_workout_status_small else R.layout.widget_workout_status
            return RemoteViews(context.packageName, layout).apply {
                setOnClickPendingIntent(R.id.widget_root, openApp)
                setImageViewResource(R.id.widget_streak_icon, R.drawable.ic_macro_calories)
                setViewVisibility(R.id.widget_streak_warning, if (workout.streakWarning) View.VISIBLE else View.GONE)
                setTextViewText(R.id.widget_streak, streak.toString())
                setViewVisibility(R.id.widget_day_label, if (workout.dayNumber != null) View.VISIBLE else View.GONE)
                workout.dayNumber?.let { setTextViewText(R.id.widget_day_label, "DÍA $it") }
                setTextViewText(R.id.widget_title, workout.title.uppercase())
                setViewVisibility(
                    R.id.widget_detail,
                    if (workout.detail.isBlank() || workout.progress != null) View.GONE else View.VISIBLE,
                )
                setTextViewText(R.id.widget_detail, workout.detail)
                setViewVisibility(
                    R.id.widget_progress_exercise,
                    if (workout.progress != null && workout.detail.isNotBlank()) View.VISIBLE else View.GONE,
                )
                setTextViewText(R.id.widget_progress_exercise, workout.detail)
                setViewVisibility(R.id.widget_meta, if (workout.meta.isNullOrBlank()) View.GONE else View.VISIBLE)
                workout.meta?.let { setTextViewText(R.id.widget_meta, it) }
                setTextViewText(R.id.widget_status, workout.status)
                workout.coverUrl?.let { coverUrl -> cachedTrainingBitmap(context, coverUrl) }.let { bitmap ->
                    if (bitmap == null) {
                        setViewVisibility(R.id.widget_background, View.GONE)
                    } else {
                        setImageViewBitmap(R.id.widget_background, bitmap)
                        setViewVisibility(R.id.widget_background, View.VISIBLE)
                    }
                }
                setViewVisibility(R.id.widget_status, if (widgetShowsStatusBadge(workout.status)) View.VISIBLE else View.GONE)
                setTextColor(
                    R.id.widget_status,
                    if (workout.status.equals("COMPLETADO", true)) Color.rgb(116, 210, 126) else Color.rgb(190, 190, 184),
                )
                setViewVisibility(R.id.widget_progress_group, if (workout.progress == null) View.GONE else View.VISIBLE)
                workout.progress?.let {
                    setTextViewText(R.id.widget_progress_label, "$it%")
                    setProgressBar(R.id.widget_progress, 100, it, false)
                }
            }
        }

        private fun workoutForWidget(context: Context, rawPlan: String?): WidgetWorkout = runCatching {
            val workouts = JSONObject(rawPlan ?: return@runCatching noPlanWidgetWorkout).optJSONArray("workouts")
                ?: return@runCatching noPlanWidgetWorkout
            val today = DayOfWeek.from(java.time.LocalDate.now()).name
            val completed = context.getSharedPreferences("wildforce_profile", Context.MODE_PRIVATE)
                .getStringSet("completed_workouts", emptySet()).orEmpty()
            val days = (0 until workouts.length()).mapNotNull { workouts.optJSONObject(it) }
            val active = days.firstOrNull { day ->
                day.optString("id").takeIf(String::isNotBlank)?.let { WorkoutSessionStore.activeExerciseIndex(context, it) } != null
            }
            val todayDate = java.time.LocalDate.now()
            val scheduledToday = context.getSharedPreferences("wildforce_profile", Context.MODE_PRIVATE)
                .getStringSet("workout_days", emptySet())
                .orEmpty()
            val completedTodayFromLocalDates = context.getSharedPreferences("wildforce_profile", Context.MODE_PRIVATE)
                .getStringSet("completed_workout_local_dates", emptySet())
                .orEmpty()
                .any { it.substringAfterLast('|', "") == todayDate.toString() }
            val completedTodayFromPlan = days.any { day ->
                day.optString("status").equals("COMPLETED", true) && (
                    day.optString("completedAt")
                        .takeIf(String::isNotBlank)
                        ?.let { raw -> runCatching { java.time.Instant.parse(raw).atZone(java.time.ZoneId.systemDefault()).toLocalDate() == todayDate }.getOrDefault(false) }
                        == true
                    )
            }
            val selectedId = selectWidgetDay(
                days = days.map { day ->
                    WidgetDayCandidate(day.optString("id"), day.optString("weekday"), day.optString("status"), day.optString("completedAt").takeIf(String::isNotBlank))
                },
                today = today,
                todayDate = todayDate,
                activeId = active?.optString("id")?.takeIf(String::isNotBlank),
            )
            val selected = days.firstOrNull { it.optString("id") == selectedId }
            if (selected == null) {
                val scheduledToday = context.getSharedPreferences("wildforce_profile", Context.MODE_PRIVATE)
                    .getStringSet("workout_days", emptySet())
                    .orEmpty()
                return@runCatching fallbackWidgetWorkout(scheduledToday, today)
            }
            val id = selected.optString("id")
            val activeIndex = id.takeIf(String::isNotBlank)?.let { WorkoutSessionStore.activeExerciseIndex(context, it) }
            val exercises = selected.optJSONArray("exercises") ?: selected.optJSONArray("blocks")?.let { blocks ->
                org.json.JSONArray().apply {
                    for (index in 0 until blocks.length()) {
                        blocks.optJSONObject(index)?.optJSONArray("exercises")?.let { list ->
                            for (exerciseIndex in 0 until list.length()) put(list.opt(exerciseIndex))
                        }
                    }
                }
            }
            val total = exercises?.length() ?: 0
            val progress = activeIndex?.let { ((it.coerceAtLeast(0) * 100) / total.coerceAtLeast(1)).coerceIn(0, 100) }
            val currentExercise = activeIndex?.let { index ->
                exercises?.optJSONObject(index)?.let { exercise ->
                    exerciseDisplayName(exercise.optString("imageKey").takeIf(String::isNotBlank), exercise.optString("name"))
                }
            }?.takeIf(String::isNotBlank)
            val currentExerciseImageKey = activeIndex?.let { index ->
                exercises?.optJSONObject(index)?.optString("imageKey")?.takeIf(String::isNotBlank)
            }
            val gender = context.getSharedPreferences("wildforce_profile", 0).getString("gender", "male") ?: "male"
            val meta = listOfNotNull(
                selected.optString("focus").takeIf(String::isNotBlank),
                selected.optInt("estimatedDurationMinutes", 0).takeIf { it > 0 }?.let { "$it min" },
            ).joinToString(" · ").ifBlank { null }
            val status = when {
                activeIndex != null -> "ENTRENAMIENTO EN CURSO"
                id in completed || selected.optString("status").equals("COMPLETED", true) -> "COMPLETADO"
                selected.optString("status").equals("SKIPPED", true) -> "OMITIDO"
                else -> "PLANIFICADO"
            }
            WidgetWorkout(
                title = selected.optString("title", "Entrenamiento"),
                detail = currentExercise ?: "",
                status = status,
                progress = progress,
                coverUrl = workoutWidgetImageUrl(
                    isInProgress = activeIndex != null,
                    currentExerciseImageKey = currentExerciseImageKey,
                    focus = selected.optString("focus"),
                    gender = gender,
                    workoutId = id,
                ),
                dayNumber = selected.optInt("orderIndex", -1).takeIf { it >= 0 }?.plus(1),
                meta = meta,
                streakWarning = widgetShowsStreakWarning(scheduledToday, today, completedTodayFromLocalDates || completedTodayFromPlan),
            )
        }.getOrDefault(noPlanWidgetWorkout)

    }
}
