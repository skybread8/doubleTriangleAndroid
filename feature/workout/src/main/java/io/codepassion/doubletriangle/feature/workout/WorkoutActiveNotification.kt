package io.codepassion.doubletriangle.feature.workout

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.drawable.Icon
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build

internal object WorkoutActiveNotification {
    const val ACTION_TOGGLE_TIMER = "io.codepassion.doubletriangle.ACTION_TOGGLE_TIMER"
    const val ACTION_SKIP_CURRENT = "io.codepassion.doubletriangle.ACTION_SKIP_CURRENT"
    const val ACTION_ADD_REST = "io.codepassion.doubletriangle.ACTION_ADD_REST"
    private const val channelId = "active_workout_live"
    private const val notificationId = 4101
    private const val restFinishedNotificationId = 4105
    private var mediaSession: MediaSession? = null
    private var artworkTitle: String? = null
    private var artwork: Bitmap? = null

    fun show(context: Context, title: String, detail: String = "Sesión activa", headsUp: Boolean = false, progress: Int = 0, progressMax: Int = 0, isResting: Boolean = false, artwork: Bitmap? = null, chronometerBaseMillis: Long? = null) {
        runCatching {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            ensureChannel(manager)
            manager.cancel(restFinishedNotificationId)
            val notification = build(context, title, detail, headsUp, progress, progressMax, isResting, artwork, chronometerBaseMillis)
            manager.notify(notificationId, notification)
        }
    }

    fun build(context: Context, title: String, detail: String, headsUp: Boolean = false, progress: Int = 0, progressMax: Int = 0, isResting: Boolean = false, artwork: Bitmap? = null, chronometerBaseMillis: Long? = null): android.app.Notification {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = launchIntent?.let {
            android.app.PendingIntent.getActivity(context, notificationId, it, android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE)
        }
        val session = mediaSession(context)
        if (artwork != null) this.artwork = artwork
        val resolvedArtwork = artwork ?: this.artwork
        val displayTitle = if (isResting) detail.substringBefore(" ·") else title
        val metadata = MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, displayTitle)
            .putString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE, detail)
            .putString(MediaMetadata.METADATA_KEY_DISPLAY_DESCRIPTION, detail)
        resolvedArtwork?.let { metadata.putBitmap(MediaMetadata.METADATA_KEY_ART, it) }
        session.setMetadata(metadata.build())
        session.setPlaybackState(PlaybackState.Builder().setActions(PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or PlaybackState.ACTION_SKIP_TO_NEXT).setState(if (isResting) PlaybackState.STATE_PAUSED else PlaybackState.STATE_PLAYING, progress.toLong(), 1f).build())
        @Suppress("DEPRECATION")
        val builder: android.app.Notification.Builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.app.Notification.Builder(context, channelId)
        } else {
            android.app.Notification.Builder(context)
        }
        val toggleIntent = actionIntent(context, ACTION_TOGGLE_TIMER, 4102)
        val skipIntent = actionIntent(context, ACTION_SKIP_CURRENT, 4103)
        val addRestIntent = actionIntent(context, ACTION_ADD_REST, 4104)
        builder.setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(displayTitle)
            .setContentText(detail)
            .setSubText(if (isResting) "Temporizador de descanso" else "Sesión activa")
            .setOngoing(true)
            .setVisibility(android.app.Notification.VISIBILITY_PUBLIC)
            .setCategory(android.app.Notification.CATEGORY_TRANSPORT)
            .setOnlyAlertOnce(true)
            .setShowWhen(isResting)
            .setAutoCancel(false)
            .addAction(android.app.Notification.Action.Builder(Icon.createWithResource(context, android.R.drawable.ic_media_pause), "Pausar", toggleIntent).build())
            .addAction(android.app.Notification.Action.Builder(Icon.createWithResource(context, android.R.drawable.ic_media_next), "Saltar", skipIntent).build())
            .addAction(android.app.Notification.Action.Builder(Icon.createWithResource(context, android.R.drawable.ic_input_add), "+30 s", addRestIntent).build())
        if (contentIntent != null) builder.setContentIntent(contentIntent)
        val promotedAllowed = Build.VERSION.SDK_INT >= 36 && runCatching {
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).canPostPromotedNotifications()
        }.getOrDefault(false)
        if (!promotedAllowed) builder.setLargeIcon(resolvedArtwork ?: exerciseArtwork(title))
        if (progressMax > 0) builder.setProgress(progressMax, progress.coerceIn(0, progressMax), false)
        if (isResting && chronometerBaseMillis != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder.setWhen(chronometerBaseMillis).setUsesChronometer(true).setChronometerCountDown(true)
        }
        builder.setStyle(android.app.Notification.BigTextStyle().bigText(if (progressMax > 0) "$detail\nProgreso: $progress/$progressMax" else detail))
        if (promotedAllowed && progressMax > 0) {
            val liveProgress = if (isResting) (progressMax - progress).coerceIn(0, progressMax) else progress.coerceIn(0, progressMax)
            val liveStyle = android.app.Notification.ProgressStyle()
                .setProgress(liveProgress)
                .setProgressTrackerIcon(Icon.createWithResource(context, R.drawable.ic_workout_live))
                .setProgressSegments(listOf(android.app.Notification.ProgressStyle.Segment(progressMax).setColor(0xFFD5A928.toInt())))
            builder.setStyle(liveStyle)
            builder.extras.putBoolean(android.app.Notification.EXTRA_REQUEST_PROMOTED_ONGOING, true)
            if (isResting) builder.setShortCriticalText(detail.substringAfter("Descanso:").substringBefore(" ").trim().take(7))
        }
        val publicVersion = android.app.Notification.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_workout_live)
            .setContentTitle(displayTitle)
            .setContentText(detail)
            .setVisibility(android.app.Notification.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setStyle(android.app.Notification.BigTextStyle().bigText(if (progressMax > 0) "$detail\nProgreso: $progress/$progressMax" else detail))
            .build()
        builder.setPublicVersion(publicVersion)
        return builder.build()
    }

    fun showRestFinished(context: Context) {
        runCatching {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            ensureChannel(manager)
            val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val contentIntent = launchIntent?.let {
                android.app.PendingIntent.getActivity(context, restFinishedNotificationId, it, android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE)
            }
            val notification = android.app.Notification.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_workout_live)
                .setContentTitle("Descanso terminado")
                .setContentText("Puedes continuar con el siguiente ejercicio")
                .setStyle(android.app.Notification.BigTextStyle().bigText("Descanso terminado\nPuedes continuar con el siguiente ejercicio"))
                .setVisibility(android.app.Notification.VISIBILITY_PUBLIC)
                .setCategory(android.app.Notification.CATEGORY_EVENT)
                .setAutoCancel(true)
                .setOnlyAlertOnce(false)
                .apply { contentIntent?.let(::setContentIntent) }
                .build()
            manager.notify(restFinishedNotificationId, notification)
        }
    }

    fun cancelRestFinished(context: Context) {
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(restFinishedNotificationId)
    }

    private fun exerciseArtwork(title: String): Bitmap {
        if (artworkTitle == title && artwork != null) return artwork!!
        val bitmap = Bitmap.createBitmap(720, 360, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val background = Paint().apply { shader = LinearGradient(0f, 0f, 720f, 360f, intArrayOf(0xFF171717.toInt(), 0xFF6A5A3E.toInt()), null, Shader.TileMode.CLAMP) }
        canvas.drawRect(0f, 0f, 720f, 360f, background)
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt(); textSize = 54f; typeface = android.graphics.Typeface.DEFAULT_BOLD }
        canvas.drawText(title.take(26), 42f, 205f, text)
        artworkTitle = title
        artwork = bitmap
        return bitmap
    }

    private fun mediaSession(context: Context): MediaSession = mediaSession ?: MediaSession(context, "WildforceWorkout").also { session ->
        session.setCallback(object : MediaSession.Callback() {
            override fun onPlay() = sendAction(context, ACTION_TOGGLE_TIMER)
            override fun onPause() = sendAction(context, ACTION_TOGGLE_TIMER)
            override fun onSkipToNext() = sendAction(context, ACTION_SKIP_CURRENT)
        })
        session.isActive = true
        mediaSession = session
    }

    private fun openApp(context: Context) {
        context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }?.let(context::startActivity)
    }

    private fun sendAction(context: Context, action: String) {
        context.sendBroadcast(Intent(action).setPackage(context.packageName))
    }

    private fun actionIntent(context: Context, action: String, requestCode: Int): android.app.PendingIntent =
        android.app.PendingIntent.getBroadcast(context, requestCode, Intent(context, WorkoutNotificationActionReceiver::class.java).setAction(action), android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE)

    private fun ensureChannel(manager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) manager.createNotificationChannel(NotificationChannel(channelId, "Entrenamiento activo", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Progreso, descansos y ejercicio actual"
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        })
    }

    fun cancel(context: Context) {
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(notificationId)
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(restFinishedNotificationId)
        mediaSession?.run { isActive = false; release() }
        mediaSession = null
        artwork = null
        artworkTitle = null
    }
}
