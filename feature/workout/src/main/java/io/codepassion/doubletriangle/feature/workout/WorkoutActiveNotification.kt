package io.codepassion.doubletriangle.feature.workout

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

internal object WorkoutActiveNotification {
    private const val channelId = "active_workout"
    private const val notificationId = 4101

    fun show(context: Context, title: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(NotificationChannel(channelId, "Entrenamiento activo", NotificationManager.IMPORTANCE_LOW))
        }
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.app.Notification.Builder(context, channelId)
        } else {
            @Suppress("DEPRECATION") android.app.Notification.Builder(context)
        }.setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Entrenamiento activo")
            .setContentText(title)
            .setOngoing(true)
            .setCategory(android.app.Notification.CATEGORY_PROGRESS)
            .build()
        manager.notify(notificationId, notification)
    }

    fun cancel(context: Context) {
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(notificationId)
    }
}
