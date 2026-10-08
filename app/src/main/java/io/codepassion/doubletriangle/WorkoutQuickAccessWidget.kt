package io.codepassion.doubletriangle

import io.codepassion.wildforce.android.R

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import io.codepassion.doubletriangle.feature.workout.WorkoutSessionStore
import org.json.JSONObject
import java.time.DayOfWeek

/** The Android counterpart of iOS's Workout Status home-screen widget. */
class WorkoutQuickAccessWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { widgetId -> manager.updateAppWidget(widgetId, views(context)) }
    }

    companion object {
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, WorkoutQuickAccessWidget::class.java)
            manager.getAppWidgetIds(component).forEach { widgetId -> manager.updateAppWidget(widgetId, views(context)) }
        }

        private fun views(context: Context): RemoteViews {
            val preferences = context.getSharedPreferences("wildforce_profile", 0)
            val workout = workoutForWidget(context, preferences.getString("workout_plan_json", null))
            val streak = preferences.getInt("current_streak", 0).coerceAtLeast(0)
            val openApp = PendingIntent.getActivity(context, 901, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            return RemoteViews(context.packageName, R.layout.widget_workout_status).apply {
                setOnClickPendingIntent(R.id.widget_root, openApp)
                setTextViewText(R.id.widget_streak, "🔥 $streak")
                setTextViewText(R.id.widget_title, workout.title.uppercase())
                setTextViewText(R.id.widget_detail, workout.detail)
                setTextViewText(R.id.widget_status, workout.status)
                setViewVisibility(R.id.widget_progress_group, if (workout.progress == null) View.GONE else View.VISIBLE)
                workout.progress?.let {
                    setTextViewText(R.id.widget_progress_label, "$it% EN CURSO")
                    setProgressBar(R.id.widget_progress, 100, it, false)
                }
            }
        }

        private fun workoutForWidget(context: Context, rawPlan: String?): WidgetWorkout = runCatching {
            val workouts = JSONObject(rawPlan ?: return@runCatching WidgetWorkout.noPlan).optJSONArray("workouts")
                ?: return@runCatching WidgetWorkout.noPlan
            val today = DayOfWeek.from(java.time.LocalDate.now()).name
            val completed = context.getSharedPreferences("wildforce_profile", Context.MODE_PRIVATE)
                .getStringSet("completed_workouts", emptySet()).orEmpty()
            val days = (0 until workouts.length()).mapNotNull { workouts.optJSONObject(it) }
            val active = days.firstOrNull { day ->
                day.optString("id").takeIf(String::isNotBlank)?.let { WorkoutSessionStore.activeExerciseIndex(context, it) } != null
            }
            val selected = active ?: days.firstOrNull { it.optString("weekday").equals(today, true) } ?: days.firstOrNull()
                ?: return@runCatching WidgetWorkout.noPlan
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
            val currentExercise = activeIndex?.let { exercises?.optJSONObject(it)?.optString("name") }?.takeIf(String::isNotBlank)
            val status = when {
                activeIndex != null -> "ENTRENAMIENTO EN CURSO"
                id in completed || selected.optString("status").equals("COMPLETED", true) -> "COMPLETADO"
                selected.optString("status").equals("SKIPPED", true) -> "OMITIDO"
                else -> "PLANIFICADO"
            }
            WidgetWorkout(
                title = selected.optString("title", "Entrenamiento"),
                detail = currentExercise ?: listOfNotNull(
                    selected.optString("focus").takeIf(String::isNotBlank),
                    selected.optInt("estimatedDurationMinutes", 0).takeIf { it > 0 }?.let { "$it min" },
                ).joinToString(" · ").ifBlank { "Tu próxima sesión" },
                status = status,
                progress = progress,
            )
        }.getOrDefault(WidgetWorkout.noPlan)

        private data class WidgetWorkout(val title: String, val detail: String, val status: String, val progress: Int?) {
            companion object { val noPlan = WidgetWorkout("Sin plan", "Crea tu primer entrenamiento", "ENTRENAMIENTO", null) }
        }
    }
}
