package io.codepassion.doubletriangle.feature.workout

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

internal object WorkoutActiveNotification {
    private const val channelId = "active_workout_live"
    private const val notificationId = 4101

    fun show(context: Context, title: String, detail: String = "Sesión activa", headsUp: Boolean = false) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureChannel(manager)
        val notification = build(context, title, detail, headsUp)
        manager.notify(notificationId, notification)
    }

    fun build(context: Context, title: String, detail: String, headsUp: Boolean = false): android.app.Notification {
        return (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) android.app.Notification.Builder(context, channelId) else @Suppress("DEPRECATION") android.app.Notification.Builder(context))
            .setSmallIcon(android.R.drawable.ic_media_play).setContentTitle(title).setContentText(detail).setOngoing(true)
            .setVisibility(android.app.Notification.VISIBILITY_PUBLIC).setCategory(android.app.Notification.CATEGORY_PROGRESS)
            .setPriority(android.app.Notification.PRIORITY_HIGH).setOnlyAlertOnce(!headsUp).setShowWhen(false).build()
    }

    private fun ensureChannel(manager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) manager.createNotificationChannel(NotificationChannel(channelId, "Entrenamiento activo", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Progreso, descansos y ejercicio actual"
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        })
    }

    fun cancel(context: Context) {
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(notificationId)
    }
}
