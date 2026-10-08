package io.codepassion.doubletriangle

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import io.codepassion.doubletriangle.nutrition.NutritionStore
import io.codepassion.wildforce.android.R
import java.time.LocalDate

/** The Android counterpart of iOS's Nutrition Tracker widget. */
class NutritionStatusWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { manager.updateAppWidget(it, views(context)) }
    }

    companion object {
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, NutritionStatusWidget::class.java)
            manager.getAppWidgetIds(component).forEach { manager.updateAppWidget(it, views(context)) }
        }

        private fun views(context: Context): RemoteViews {
            val today = LocalDate.now()
            val logged = NutritionStore.load(context, today)
            val plannedTargets = NutritionStore.loadGeneratedPlan(context)?.firstOrNull { it.date == today }?.targets
            val target = plannedTargets ?: NutritionStore.loadTargets(context)
            val calories = logged.sumOf { it.calories }
            val protein = logged.sumOf { it.protein }
            val carbs = logged.sumOf { it.carbs }
            val fat = logged.sumOf { it.fat }
            val openApp = PendingIntent.getActivity(context, 902, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            return RemoteViews(context.packageName, R.layout.widget_nutrition_status).apply {
                setOnClickPendingIntent(R.id.nutrition_widget_root, openApp)
                setTextViewText(R.id.nutrition_calories, "$calories / ${target.calories} kcal")
                setTextViewText(R.id.nutrition_remaining, "${(target.calories - calories).coerceAtLeast(0)} KCAL RESTANTES")
                bindMacro(R.id.nutrition_protein, R.id.nutrition_protein_progress, "P", protein, target.protein)
                bindMacro(R.id.nutrition_carbs, R.id.nutrition_carbs_progress, "C", carbs, target.carbs)
                bindMacro(R.id.nutrition_fat, R.id.nutrition_fat_progress, "G", fat, target.fat)
                setProgressBar(R.id.nutrition_calorie_progress, 100, percent(calories, target.calories), false)
            }
        }

        private fun RemoteViews.bindMacro(textId: Int, progressId: Int, label: String, current: Int, target: Int) {
            setTextViewText(textId, "$label  $current/$target g")
            setProgressBar(progressId, 100, percent(current, target), false)
        }

        private fun percent(current: Int, target: Int): Int = if (target <= 0) 0 else ((current * 100) / target).coerceIn(0, 100)
    }
}
