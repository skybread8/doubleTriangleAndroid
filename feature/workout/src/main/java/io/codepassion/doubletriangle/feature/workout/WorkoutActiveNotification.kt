package io.codepassion.doubletriangle.feature.workout

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build

internal object WorkoutActiveNotification {
    private const val channelId = "active_workout_live"
    private const val notificationId = 4101
    private var mediaSession: MediaSession? = null

    fun show(context: Context, title: String, detail: String = "Sesión activa", headsUp: Boolean = false) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureChannel(manager)
        val notification = build(context, title, detail, headsUp)
        manager.notify(notificationId, notification)
    }

    fun build(context: Context, title: String, detail: String, headsUp: Boolean = false): android.app.Notification {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = launchIntent?.let {
            android.app.PendingIntent.getActivity(context, notificationId, it, android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE)
        }
        val session = mediaSession(context)
        session.setMetadata(MediaMetadata.Builder().putString(MediaMetadata.METADATA_KEY_TITLE, title).putString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE, detail).build())
        session.setPlaybackState(PlaybackState.Builder().setActions(PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE).setState(PlaybackState.STATE_PLAYING, 0L, 1f).build())
        @Suppress("DEPRECATION")
        val builder: android.app.Notification.Builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.app.Notification.Builder(context, channelId)
        } else {
            android.app.Notification.Builder(context)
        }
        builder.setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(title)
            .setContentText(detail)
            .setOngoing(true)
            .setVisibility(android.app.Notification.VISIBILITY_PUBLIC)
            .setCategory(android.app.Notification.CATEGORY_TRANSPORT)
            .setPriority(android.app.Notification.PRIORITY_HIGH)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setAutoCancel(false)
        if (contentIntent != null) builder.setContentIntent(contentIntent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setStyle(android.app.Notification.MediaStyle().setMediaSession(session.sessionToken).setShowActionsInCompactView(0))
        }
        if (headsUp) builder.setDefaults(android.app.Notification.DEFAULT_ALL)
        return builder.build()
    }

    private fun mediaSession(context: Context): MediaSession = mediaSession ?: MediaSession(context, "WildforceWorkout").also { session ->
        session.setCallback(object : MediaSession.Callback() {
            override fun onPlay() = openApp(context)
            override fun onPause() = openApp(context)
        })
        session.isActive = true
        mediaSession = session
    }

    private fun openApp(context: Context) {
        context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }?.let(context::startActivity)
    }

    private fun ensureChannel(manager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) manager.createNotificationChannel(NotificationChannel(channelId, "Entrenamiento activo", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Progreso, descansos y ejercicio actual"
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        })
    }

    fun cancel(context: Context) {
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(notificationId)
        mediaSession?.run { isActive = false; release() }
        mediaSession = null
    }
}
