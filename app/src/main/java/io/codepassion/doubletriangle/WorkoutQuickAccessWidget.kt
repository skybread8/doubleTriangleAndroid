package io.codepassion.doubletriangle

import io.codepassion.wildforce.android.R

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

/** Home-screen shortcut that mirrors the current local workout progress. */
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
            val completed = preferences.getInt("total_completed_workouts", 0)
            val title = if (preferences.getString("workout_plan_json", null) == null) "ENTRENAMIENTO" else "PLAN ACTIVO"
            val openApp = PendingIntent.getActivity(context, 901, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            return RemoteViews(context.packageName, R.layout.widget_workout_quick_access).apply {
                setTextViewText(R.id.widget_title, title)
                setTextViewText(R.id.widget_progress, "$completed sesiones completadas")
                setOnClickPendingIntent(R.id.widget_root, openApp)
            }
        }
    }
}
