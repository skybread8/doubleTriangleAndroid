package io.codepassion.doubletriangle

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.widget.RemoteViews
import android.net.Uri
import io.codepassion.doubletriangle.nutrition.NutritionStore
import io.codepassion.doubletriangle.nutrition.NutritionTargets
import io.codepassion.wildforce.android.R
import java.time.LocalDate
import kotlin.math.roundToInt

/** The Android counterpart of iOS's Nutrition Tracker widget. */
class NutritionStatusWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { id ->
            manager.updateAppWidget(id, views(context, manager.getAppWidgetOptions(id)))
        }
    }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, appWidgetId: Int, newOptions: android.os.Bundle) {
        manager.updateAppWidget(appWidgetId, views(context, newOptions))
    }

    companion object {
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, NutritionStatusWidget::class.java)
            manager.getAppWidgetIds(component).forEach { widgetId ->
                manager.updateAppWidget(widgetId, views(context, manager.getAppWidgetOptions(widgetId)))
            }
        }

        private fun views(context: Context, options: android.os.Bundle): RemoteViews {
            // WidgetKit renders a dedicated compact composition for
            // systemSmall. Android exposes the same choice through the
            // current minimum width in dp.
            val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
            val isSmall = minWidth in 1 until 250
            val today = LocalDate.now()
            val logged = NutritionStore.load(context, today)
            val plannedTargets = NutritionStore.loadGeneratedPlan(context)?.firstOrNull { it.date == today }?.targets
            val target = nutritionWidgetTargets(plannedTargets)
            val calories = logged.sumOf { it.calories }
            val protein = logged.sumOf { it.protein }
            val carbs = logged.sumOf { it.carbs }
            val fat = logged.sumOf { it.fat }
            val openAppIntent = Intent(context, MainActivity::class.java)
                .setData(Uri.parse("wildforce://log-meal"))
            val openApp = PendingIntent.getActivity(context, 902, openAppIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            return RemoteViews(
                context.packageName,
                if (isSmall) R.layout.widget_nutrition_status_small else R.layout.widget_nutrition_status,
            ).apply {
                setOnClickPendingIntent(R.id.nutrition_widget_root, openApp)
                setImageViewBitmap(R.id.nutrition_calorie_ring, calorieRing(context, calories, target.calories))
                val hasCalorieTarget = target.calories > 0
                setTextViewText(R.id.nutrition_calorie_percent, if (hasCalorieTarget) caloriePercent(calories, target.calories) else "")
                setViewVisibility(R.id.nutrition_calorie_percent, if (hasCalorieTarget) android.view.View.VISIBLE else android.view.View.GONE)
                setViewVisibility(R.id.nutrition_calorie_empty, if (hasCalorieTarget) android.view.View.GONE else android.view.View.VISIBLE)
                if (isSmall) {
                    setProgressBar(R.id.nutrition_protein_progress, 100, percent(protein, target.protein), false)
                    setProgressBar(R.id.nutrition_carbs_progress, 100, percent(carbs, target.carbs), false)
                    setProgressBar(R.id.nutrition_fat_progress, 100, percent(fat, target.fat), false)
                } else {
                    // iOS keeps the current value prominent and renders the
                    // target/unit as a quieter trailing suffix.
                    setTextViewText(R.id.nutrition_calories, calories.toString())
                    setTextViewText(R.id.nutrition_calories_target, target.calories.toString())
                    // Keep the same localized copy and signed difference as
                    // NutritionWidget's `%d Kcal Left`, including over-target
                    // and no-target states.
                    setTextViewText(
                        R.id.nutrition_remaining,
                        context.getString(R.string.ios_catalog_06c4ce57478b878d)
                            .replace("%d", (target.calories - calories).toString()),
                    )
                    bindMacro(context, R.id.nutrition_protein_title, R.id.nutrition_protein, R.id.nutrition_protein_progress, R.string.ios_catalog_92a31f1de3dff9a0, protein, target.protein)
                    bindMacro(context, R.id.nutrition_carbs_title, R.id.nutrition_carbs, R.id.nutrition_carbs_progress, R.string.ios_catalog_2c06fb68cd6e1a28, carbs, target.carbs)
                    bindMacro(context, R.id.nutrition_fat_title, R.id.nutrition_fat, R.id.nutrition_fat_progress, R.string.ios_catalog_ddcdda5ac2be57d4, fat, target.fat)
                }
            }
        }

        private fun RemoteViews.bindMacro(context: Context, titleId: Int, textId: Int, progressId: Int, titleRes: Int, current: Int, target: Int) {
            setTextViewText(titleId, context.getString(titleRes))
            setTextViewText(textId, "$current / $target g")
            setProgressBar(progressId, 100, percent(current, target), false)
        }

        private fun percent(current: Int, target: Int): Int = if (target <= 0) 0 else ((current * 100) / target).coerceIn(0, 100)

        private fun caloriePercent(current: Int, target: Int): String =
            if (target <= 0) "—" else "${(current.toDouble() / target * 100).roundToInt()}%"

        /** Mirrors iOS NutritionWidget's base ring plus the overflow arc. */
        private fun calorieRing(context: Context, current: Int, target: Int): Bitmap {
            val size = 216
            val stroke = 18f
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val bounds = RectF(stroke / 2, stroke / 2, size - stroke / 2, size - stroke / 2)
            val isDark = (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
            val track = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isDark) 0x24FEFFFF else 0x24373732
                style = Paint.Style.STROKE
                strokeWidth = stroke
            }
            val accent = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFFD4AF37.toInt()
                style = Paint.Style.STROKE
                strokeWidth = stroke
                strokeCap = Paint.Cap.ROUND
            }
            canvas.drawArc(bounds, -90f, 360f, false, track)
            if (target > 0) {
                val progress = current.toFloat() / target
                canvas.drawArc(bounds, -90f, (progress.coerceIn(0f, 1f) * 360f), false, accent)
                if (progress > 1f) {
                    canvas.drawArc(bounds, -90f, ((progress - 1f).coerceIn(0f, 1f) * 360f), false, accent)
                }
            }
            return bitmap
        }
    }
}

/** iOS uses zero targets when no current nutrition day exists; do not invent a goal in the widget. */
internal fun nutritionWidgetTargets(planned: NutritionTargets?): NutritionTargets =
    planned ?: NutritionTargets(calories = 0, protein = 0, carbs = 0, fat = 0)
