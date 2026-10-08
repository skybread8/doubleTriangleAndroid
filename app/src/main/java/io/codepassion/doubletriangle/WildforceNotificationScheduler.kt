package io.codepassion.doubletriangle

import io.codepassion.wildforce.android.R

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import io.codepassion.doubletriangle.core.model.WorkoutHubState
import io.codepassion.doubletriangle.core.model.WorkoutStatus
import io.codepassion.doubletriangle.feature.workout.WorkoutNotificationPreferences
import io.codepassion.doubletriangle.nutrition.MealType
import io.codepassion.doubletriangle.nutrition.NutritionStore
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

internal object WildforceNotificationScheduler {
    const val ACTION_REMINDER = "io.codepassion.doubletriangle.ACTION_REMINDER"
    const val CHANNEL_WORKOUT = "workout_reminders"
    const val CHANNEL_NUTRITION = "nutrition_reminders"
    private const val WORKOUT_ALARM = 7201
    private const val NUTRITION_ALARM = 7202
    private const val SCHEDULED_WORKOUT_ALARMS = "scheduled_workout_alarm_codes"
    private const val PROFILE_PREFERENCES = "wildforce_profile"
    private const val LAST_WORKOUT_STARTED_AT = "last_workout_started_at"
    private const val LAST_WORKOUT_COMPLETED_AT = "last_workout_completed_at"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(NotificationChannel(CHANNEL_WORKOUT, "Entrenamientos", NotificationManager.IMPORTANCE_DEFAULT))
            manager.createNotificationChannel(NotificationChannel(CHANNEL_NUTRITION, "Nutrición", NotificationManager.IMPORTANCE_DEFAULT))
        }
    }

    fun scheduleNextWorkout(context: Context, state: WorkoutHubState) {
        cancelWorkoutReminders(context)
        if (!WorkoutNotificationPreferences.remindersEnabled(context)) return

        val reminderTime = preferredWorkoutTime(context)
        val scheduledAlarmCodes = mutableSetOf<Int>()
        state.workouts
            .asSequence()
            .filter { it.status == WorkoutStatus.Planned }
            .sortedBy { it.order }
            .take(7)
            .forEach { workout ->
                val date = LocalDate.now().plusDays(daysUntil(workout.scheduledDay).toLong())
                // Keep the same lead time as iOS: the reminder arrives 30 minutes
                // before the user's usual workout start time.
                val reminderAt = date.atTime(reminderTime).minusMinutes(30)
                if (reminderAt.isAfter(LocalDateTime.now())) {
                    val requestCode = workoutAlarmCode(workout.id)
                    schedule(
                        context = context,
                        requestCode = requestCode,
                        dateTime = reminderAt,
                        channel = CHANNEL_WORKOUT,
                        title = workoutReminderTitle(context),
                        body = context.getString(R.string.workout_reminder_body, workout.title),
                    )
                    scheduledAlarmCodes.add(requestCode)
                }
            }
        persistScheduledWorkoutAlarmCodes(context, scheduledAlarmCodes)
    }

    fun scheduleNutritionReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent(context, NUTRITION_ALARM))
        if (!WorkoutNotificationPreferences.remindersEnabled(context)) return

        // Mirrors iOS: reminders follow the meals actually planned for today,
        // rather than a generic fixed-hour nudge. A day with no food log yet
        // is deliberately left alone.
        val now = LocalDateTime.now()
        val next = pendingNutritionCheckpoints(context, now)
            .filter { it.at.isAfter(now) }
            .minByOrNull { it.at }
            ?: return
        val mealsAtCheckpoint = pendingNutritionCheckpoints(context, now)
            .filter { it.at == next.at }
            .map { it.title }
        schedule(
            context,
            NUTRITION_ALARM,
            next.at,
            CHANNEL_NUTRITION,
            "Registra tus comidas",
            nutritionReminderBody(mealsAtCheckpoint),
        )
    }

    fun cancelAll(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        cancelWorkoutReminders(context)
        alarmManager.cancel(pendingIntent(context, NUTRITION_ALARM))
    }

    /** Store the start time so future reminders follow the user's actual routine. */
    fun recordWorkoutStarted(context: Context, startedAtMillis: Long = System.currentTimeMillis()) {
        context.getSharedPreferences(PROFILE_PREFERENCES, Context.MODE_PRIVATE)
            .edit()
            .putLong(LAST_WORKOUT_STARTED_AT, startedAtMillis)
            .apply()
    }

    fun recordWorkoutCompleted(context: Context, completedAtMillis: Long = System.currentTimeMillis()) {
        context.getSharedPreferences(PROFILE_PREFERENCES, Context.MODE_PRIVATE)
            .edit()
            .putLong(LAST_WORKOUT_COMPLETED_AT, completedAtMillis)
            .apply()
    }

    private fun schedule(context: Context, requestCode: Int, dateTime: LocalDateTime, channel: String, title: String, body: String) {
        ensureChannels(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAt = dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val alarmIntent = pendingIntent(context, requestCode, channel, title, body)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, alarmIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, alarmIntent)
        }
    }

    private fun pendingIntent(context: Context, requestCode: Int, channel: String? = null, title: String? = null, body: String? = null): PendingIntent {
        val intent = Intent(context, WildforceNotificationReceiver::class.java).setAction(ACTION_REMINDER)
            .putExtra("channel", channel).putExtra("title", title).putExtra("body", body)
            .putExtra("notification_id", requestCode)
        return PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun daysUntil(day: DayOfWeek): Int = ((day.value - LocalDate.now().dayOfWeek.value) + 7) % 7

    private fun preferredWorkoutTime(context: Context): LocalTime {
        val preferences = context.getSharedPreferences(PROFILE_PREFERENCES, Context.MODE_PRIVATE)
        val lastStartedAt = preferences.getLong(LAST_WORKOUT_STARTED_AT, 0L)
        val lastCompletedAt = preferences.getLong(LAST_WORKOUT_COMPLETED_AT, 0L)
        val reference = when {
            lastStartedAt > 0L -> lastStartedAt
            lastCompletedAt > 0L -> lastCompletedAt - 45 * 60 * 1_000L
            else -> return LocalTime.of(9, 0)
        }
        return java.time.Instant.ofEpochMilli(reference).atZone(ZoneId.systemDefault()).toLocalTime()
    }

    private fun workoutAlarmCode(workoutId: String): Int = WORKOUT_ALARM + (workoutId.hashCode() and 0x3fffffff)

    private fun scheduledWorkoutAlarmCodes(context: Context): MutableSet<Int> = context
        .getSharedPreferences(PROFILE_PREFERENCES, Context.MODE_PRIVATE)
        .getStringSet(SCHEDULED_WORKOUT_ALARMS, emptySet())
        .orEmpty()
        .mapNotNull { it.toIntOrNull() }
        .toMutableSet()

    private fun persistScheduledWorkoutAlarmCodes(context: Context, codes: Set<Int> = scheduledWorkoutAlarmCodes(context)) {
        context.getSharedPreferences(PROFILE_PREFERENCES, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(SCHEDULED_WORKOUT_ALARMS, codes.mapTo(mutableSetOf()) { it.toString() })
            .apply()
    }

    private fun cancelWorkoutReminders(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        scheduledWorkoutAlarmCodes(context).forEach { alarmManager.cancel(pendingIntent(context, it)) }
        // Cancel the legacy single-workout alarm too, so updating the app never
        // leaves an old 09:00 reminder behind.
        alarmManager.cancel(pendingIntent(context, WORKOUT_ALARM))
        persistScheduledWorkoutAlarmCodes(context, emptySet())
    }

    private fun workoutReminderTitle(context: Context): String =
        context.resources.getStringArray(R.array.workout_reminder_titles).random()

    /** Planned breakfast/lunch/dinner checkpoints, using the same iOS ranges: 12:00, 17:00 and 21:00. */
    private fun pendingNutritionCheckpoints(context: Context, now: LocalDateTime): List<NutritionCheckpoint> {
        val today = now.toLocalDate()
        val logged = NutritionStore.load(context, today)
        if (logged.isEmpty()) return emptyList()
        val planned = NutritionStore.loadGeneratedPlan(context)
            ?.firstOrNull { it.date == today }
            ?.meals
            .orEmpty()
        val loggedTypes = logged.map { it.type }.toSet()
        return planned
            .filter { it.type in reminderMealTypes && it.type !in loggedTypes }
            .map { NutritionCheckpoint(it.title.ifBlank { it.type.title }, today.atTime(reminderTime(it.type))) }
    }

    /** A notification remains valid only while at least one of its meals is still overdue. */
    fun nutritionReminderDue(context: Context, now: LocalDateTime = LocalDateTime.now()): Boolean =
        pendingNutritionCheckpoints(context, now).any { !it.at.isAfter(now) }

    private fun reminderTime(type: MealType): LocalTime = when (type) {
        MealType.Breakfast -> LocalTime.of(12, 0)
        MealType.Lunch -> LocalTime.of(17, 0)
        MealType.Dinner -> LocalTime.of(21, 0)
        MealType.Snack -> error("Snack has no logging checkpoint")
    }

    private fun nutritionReminderBody(names: List<String>): String = when (names.size) {
        0 -> "Aún te quedan comidas por registrar hoy."
        1 -> "Aún tienes que registrar ${names.first()} hoy."
        2 -> "Aún tienes que registrar ${names[0]} y ${names[1]} hoy."
        else -> "Aún tienes que registrar ${names.dropLast(1).joinToString(", ")} y ${names.last()} hoy."
    }

    private data class NutritionCheckpoint(val title: String, val at: LocalDateTime)

    private val reminderMealTypes = setOf(MealType.Breakfast, MealType.Lunch, MealType.Dinner)
}

internal class WildforceNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!WorkoutNotificationPreferences.remindersEnabled(context)) return
        val channel = intent.getStringExtra("channel") ?: WildforceNotificationScheduler.CHANNEL_WORKOUT
        if (channel == WildforceNotificationScheduler.CHANNEL_NUTRITION) {
            // A meal might have been logged after this alarm was queued.
            // Re-evaluate it before showing anything, then queue only the
            // next still-pending planned checkpoint.
            val isStillDue = WildforceNotificationScheduler.nutritionReminderDue(context)
            WildforceNotificationScheduler.scheduleNutritionReminder(context)
            if (!isStillDue) return
        }
        val title = intent.getStringExtra("title") ?: "Wildforce"
        val body = intent.getStringExtra("body") ?: "Tienes una actividad pendiente."
        WildforceNotificationScheduler.ensureChannels(context)
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) android.app.Notification.Builder(context, channel) else @Suppress("DEPRECATION") android.app.Notification.Builder(context)
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = launchIntent?.let {
            PendingIntent.getActivity(
                context,
                channel.hashCode(),
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
        val built = notification.setSmallIcon(R.drawable.ic_wildforce_notification).setContentTitle(title).setContentText(body)
            .setAutoCancel(true).apply { contentIntent?.let(::setContentIntent) }.build()
        val notificationId = intent.getIntExtra("notification_id", channel.hashCode())
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(notificationId, built)
    }
}
