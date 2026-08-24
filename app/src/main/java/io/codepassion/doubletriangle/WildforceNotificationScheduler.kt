package io.codepassion.doubletriangle

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

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(NotificationChannel(CHANNEL_WORKOUT, "Entrenamientos", NotificationManager.IMPORTANCE_DEFAULT))
            manager.createNotificationChannel(NotificationChannel(CHANNEL_NUTRITION, "Nutrición", NotificationManager.IMPORTANCE_DEFAULT))
        }
    }

    fun scheduleNextWorkout(context: Context, state: WorkoutHubState) {
        val next = state.workouts.filter { it.status == WorkoutStatus.Planned }.minByOrNull { daysUntil(it.scheduledDay) } ?: return
        val date = LocalDate.now().plusDays(daysUntil(next.scheduledDay).toLong())
        schedule(context, WORKOUT_ALARM, date.atTime(LocalTime.of(9, 0)), CHANNEL_WORKOUT, "Entrenamiento pendiente", "Hoy: ${next.title}")
    }

    fun scheduleNutritionReminder(context: Context) {
        schedule(context, NUTRITION_ALARM, LocalDate.now().atTime(LocalTime.of(20, 0)), CHANNEL_NUTRITION, "Registro nutricional", "¿Has registrado tus comidas de hoy?")
    }

    fun cancelAll(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        listOf(WORKOUT_ALARM, NUTRITION_ALARM).forEach { alarmManager.cancel(pendingIntent(context, it)) }
    }

    private fun schedule(context: Context, requestCode: Int, dateTime: LocalDateTime, channel: String, title: String, body: String) {
        ensureChannels(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAt = dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli().coerceAtLeast(System.currentTimeMillis() + 5_000)
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent(context, requestCode, channel, title, body))
    }

    private fun pendingIntent(context: Context, requestCode: Int, channel: String? = null, title: String? = null, body: String? = null): PendingIntent {
        val intent = Intent(context, WildforceNotificationReceiver::class.java).setAction(ACTION_REMINDER)
            .putExtra("channel", channel).putExtra("title", title).putExtra("body", body)
        return PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun daysUntil(day: DayOfWeek): Int = ((day.value - LocalDate.now().dayOfWeek.value) + 7) % 7
}

internal class WildforceNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val channel = intent.getStringExtra("channel") ?: WildforceNotificationScheduler.CHANNEL_WORKOUT
        val title = intent.getStringExtra("title") ?: "Wildforce"
        val body = intent.getStringExtra("body") ?: "Tienes una actividad pendiente."
        WildforceNotificationScheduler.ensureChannels(context)
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) android.app.Notification.Builder(context, channel) else @Suppress("DEPRECATION") android.app.Notification.Builder(context)
        val built = notification.setSmallIcon(android.R.drawable.ic_popup_reminder).setContentTitle(title).setContentText(body).setAutoCancel(true).build()
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(channel.hashCode(), built)
    }
}
