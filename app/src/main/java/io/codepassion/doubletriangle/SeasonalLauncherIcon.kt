package io.codepassion.doubletriangle

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import io.codepassion.doubletriangle.core.designsystem.SeasonalDecorations
import io.codepassion.doubletriangle.feature.workout.CustomWorkoutStore
import io.codepassion.doubletriangle.core.model.WorkoutStatus
import io.codepassion.wildforce.android.BuildConfig
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Switches launcher aliases; MainActivity stays enabled for deep links and widgets. */
internal object SeasonalLauncherIcon {
    private const val DEFAULT_ALIAS = "io.codepassion.doubletriangle.LauncherDefault"
    private const val CHRISTMAS_ALIAS = "io.codepassion.doubletriangle.LauncherChristmas"
    private const val BROKEN_ALIAS = "io.codepassion.doubletriangle.LauncherBroken"
    private const val CHRISTMAS_BROKEN_ALIAS = "io.codepassion.doubletriangle.LauncherChristmasBroken"
    private const val ALARM_ACTION = "io.codepassion.wildforce.android.SEASONAL_ICON_BOUNDARY"
    private val aliases = listOf(DEFAULT_ALIAS, CHRISTMAS_ALIAS, BROKEN_ALIAS, CHRISTMAS_BROKEN_ALIAS)

    internal fun nextBoundary(today: LocalDate): LocalDate = when {
        today.monthValue == 1 && today.dayOfMonth <= 6 -> LocalDate.of(today.year, 1, 7)
        today.monthValue == 12 && today.dayOfMonth >= 7 -> LocalDate.of(today.year + 1, 1, 7)
        else -> LocalDate.of(today.year, 12, 7)
    }

    internal fun isWorkoutPending(scheduledDays: Set<String>, today: LocalDate, completedToday: Boolean): Boolean =
        scheduledDays.any { it.equals(today.dayOfWeek.name, ignoreCase = true) } && !completedToday

    internal fun aliasFor(christmas: Boolean, pending: Boolean): String = when {
        christmas && pending -> CHRISTMAS_BROKEN_ALIAS
        christmas -> CHRISTMAS_ALIAS
        pending -> BROKEN_ALIAS
        else -> DEFAULT_ALIAS
    }

    private fun completedOn(raw: String?, today: LocalDate): Boolean = raw?.takeIf(String::isNotBlank)?.let {
        runCatching { Instant.parse(it).atZone(ZoneId.systemDefault()).toLocalDate() == today }.getOrDefault(false)
    } == true

    private fun pendingWorkout(context: Context, today: LocalDate): Boolean {
        val prefs = context.getSharedPreferences("wildforce_profile", Context.MODE_PRIVATE)
        val scheduled = prefs.getStringSet("workout_days", emptySet()).orEmpty()
        if (!scheduled.any { it.equals(today.dayOfWeek.name, ignoreCase = true) }) return false
        val completedLocally = prefs.getStringSet("completed_workout_local_dates", emptySet()).orEmpty()
            .any { it.substringAfterLast('|', "") == today.toString() }
        val completedInPlan = runCatching {
            val days = JSONObject(prefs.getString("workout_plan_json", null) ?: "{}").optJSONArray("workouts")
            days != null && (0 until days.length()).any { index ->
                days.optJSONObject(index)?.let { day ->
                    day.optString("status").equals("COMPLETED", ignoreCase = true) && completedOn(day.optString("completedAt"), today)
                } == true
            }
        }.getOrDefault(false)
        val completedCustom = runCatching {
            CustomWorkoutStore.load(context).any { it.status == WorkoutStatus.Completed && completedOn(it.completedAt, today) }
        }.getOrDefault(false)
        return isWorkoutPending(scheduled, today, completedLocally || completedInPlan || completedCustom)
    }

    @Synchronized
    fun refresh(context: Context) {
        val appContext = context.applicationContext
        val manager = appContext.packageManager
        val today = LocalDate.now()
        val christmas = SeasonalDecorations.enabled(appContext) && SeasonalDecorations.isChristmas(today)
        val pending = pendingWorkout(appContext, today)
        // Debug deliberately keeps Android Studio's launch alias enabled. Its manifest previews
        // the combined broken/Christmas artwork; release builds switch by actual state.
        val selectedName = if (BuildConfig.DEBUG) DEFAULT_ALIAS else aliasFor(christmas, pending)
        val changes = aliases.mapNotNull { name ->
            val component = ComponentName(appContext, name)
            val expectedEnabled = name == selectedName
            val state = manager.getComponentEnabledSetting(component)
            val isEnabled = state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED ||
                (state == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT && name == DEFAULT_ALIAS)
            if (isEnabled == expectedEnabled) null else component to expectedEnabled
        }
        if (changes.isNotEmpty()) {
            if (Build.VERSION.SDK_INT >= 33) {
                manager.setComponentEnabledSettings(changes.map { (component, enabled) ->
                    PackageManager.ComponentEnabledSetting(component, if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
                })
            } else {
                changes.sortedByDescending { it.second }.forEach { (component, enabled) ->
                    manager.setComponentEnabledSetting(component, if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
                }
            }
        }
        // A daily inexact alarm catches new training days as well as seasonal boundaries.
        val next = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val intent = Intent(appContext, SeasonalIconReceiver::class.java).setAction(ALARM_ACTION)
        val pendingIntent = PendingIntent.getBroadcast(appContext, 4928, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        (appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager)
            .setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, pendingIntent)
    }
}

class SeasonalIconReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        SeasonalLauncherIcon.refresh(context)
    }
}
