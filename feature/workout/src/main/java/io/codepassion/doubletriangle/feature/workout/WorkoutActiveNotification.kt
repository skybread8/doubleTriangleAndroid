package io.codepassion.doubletriangle.feature.workout

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.media.RingtoneManager
import android.graphics.drawable.Icon

internal enum class WorkoutNotificationType {
    PROGRESS, REST
}

internal object WorkoutActiveNotification {
    const val ACTION_TOGGLE_TIMER = "io.codepassion.doubletriangle.ACTION_TOGGLE_TIMER"
    const val ACTION_SKIP_CURRENT = "io.codepassion.doubletriangle.ACTION_SKIP_CURRENT"
    const val ACTION_ADD_REST = "io.codepassion.doubletriangle.ACTION_ADD_REST"
    private const val channelId = "active_workout_live_v4"
    private const val notificationId = 4101
    private const val restFinishedNotificationId = 4105
    // These match the active workout's progress segments: completed, current,
    // and pending respectively.
    private const val workoutProgressCompleted = 0xE0FFFFFF.toInt()
    private const val workoutProgressCurrent = 0x9EFFFFFF.toInt()
    private const val workoutProgressPending = 0x47FFFFFF
    private var artworkTitle: String? = null
    private var artwork: Bitmap? = null
    private var liveArtworkSource: Bitmap? = null
    private var liveArtwork: Bitmap? = null

    fun show(
        context: Context,
        title: String,
        detail: String = "Sesión activa",
        headsUp: Boolean = false,
        progress: Int = 0,
        progressMax: Int = 0,
        type: WorkoutNotificationType = WorkoutNotificationType.PROGRESS,
        artwork: Bitmap? = null,
        exerciseIndex: Int? = null,
        totalExercises: Int? = null,
        exerciseTimerSeconds: Int? = null,
        canToggleExerciseTimer: Boolean = false,
        restEndAtMillis: Long? = null,
    ) {
        runCatching {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            ensureChannel(manager)
            manager.cancel(restFinishedNotificationId)
            val notification = build(
                context, title, detail, headsUp, progress, progressMax, type, artwork,
                exerciseIndex = exerciseIndex,
                totalExercises = totalExercises,
                exerciseTimerSeconds = exerciseTimerSeconds,
                canToggleExerciseTimer = canToggleExerciseTimer,
                restEndAtMillis = restEndAtMillis,
            )
            manager.notify(notificationId, notification)
        }
    }

    fun showRestCompletedInPlace(context: Context, title: String, completedExercises: Int, totalExercises: Int) {
        runCatching {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            ensureChannel(manager)
            val notification = build(
                context,
                "Descanso terminado",
                "¡DESCANSO TERMINADO! · Pulsa para siguiente serie",
                headsUp = true,
                progress = 0,
                progressMax = totalExercises.coerceAtLeast(1),
                type = WorkoutNotificationType.PROGRESS,
                alert = true,
                exerciseIndex = completedExercises,
                totalExercises = totalExercises
            )
            manager.notify(notificationId, notification)
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(VibratorManager::class.java)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (vibrator?.hasVibrator() == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0L, 180L, 90L, 180L), -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(longArrayOf(0L, 180L, 90L, 180L), -1)
                }
            }
            RingtoneManager.getRingtone(context, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))?.play()
        }
    }

    @Suppress("DEPRECATION")
    fun build(
        context: Context,
        title: String,
        detail: String,
        headsUp: Boolean = false,
        progress: Int = 0,
        progressMax: Int = 0,
        type: WorkoutNotificationType = WorkoutNotificationType.PROGRESS,
        artwork: Bitmap? = null,
        alert: Boolean = false,
        exerciseIndex: Int? = null,
        totalExercises: Int? = null,
        exerciseTimerSeconds: Int? = null,
        canToggleExerciseTimer: Boolean = false,
        restEndAtMillis: Long? = null,
    ): android.app.Notification {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = launchIntent?.let {
            android.app.PendingIntent.getActivity(context, notificationId, it, android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE)
        }
        if (artwork != null) this.artwork = artwork
        val resolvedArtwork = artwork ?: this.artwork

        val isResting = type == WorkoutNotificationType.REST
        val index = exerciseIndex ?: 0
        val count = (totalExercises ?: 1).coerceAtLeast(1)

        val displayTitle = if (isResting) {
            "Descanso · ${progress.coerceAtLeast(0)}s"
        } else {
            val timer = exerciseTimerSeconds?.coerceAtLeast(0)?.let { " · ${it}s" }.orEmpty()
            "$title$timer (${index + 1}/$count)"
        }
        val displayDetail = if (isResting) "${progress}s restantes" else detail

        val builder: android.app.Notification.Builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            ensureChannel(manager)
            android.app.Notification.Builder(context, channelId)
        } else {
            android.app.Notification.Builder(context)
        }
        val skipIntent = actionIntent(context, ACTION_SKIP_CURRENT, 4103)
        val addRestIntent = actionIntent(context, ACTION_ADD_REST, 4104)
        val notificationArtwork = resolvedArtwork ?: exerciseArtwork(title)
        val appIconResourceId = context.applicationInfo.icon
        // Keep the completion alert promoted too, so Android 16 can show its
        // short critical text in the Live Update capsule.
        val useLiveUpdate = Build.VERSION.SDK_INT >= 36

        // Status-bar icons must be transparent monochrome vectors. This W is
        // the Wildforce mark, so Android does not replace it with a bell.
        builder.setSmallIcon(R.drawable.ic_wildforce_notification)
            .setContentTitle(displayTitle)
            .setContentText(displayDetail)
            .setSubText("Sesión activa")
            .setOngoing(true)
            .setVisibility(android.app.Notification.VISIBILITY_PUBLIC)
            .setCategory(android.app.Notification.CATEGORY_WORKOUT)
            .setOnlyAlertOnce(true)
            .setAutoCancel(false)
            .setColor(workoutProgressCurrent)

        if (useLiveUpdate) {
            // Android 16 only promotes an ongoing notification to the compact
            // Live Update surface when it requests promotion and uses
            // ProgressStyle. A regular progress bar silently falls back to a
            // conventional notification and loses its critical status text.
            builder.extras.putBoolean("android.requestPromotedOngoing", true)
            builder.setRequestPromotedOngoing(true)
            builder.setShortCriticalText(
                if (alert) "LISTO"
                else if (isResting) "${progress.coerceAtLeast(0)}s"
                else "${index + 1}/$count"
            )

            val liveStyle = android.app.Notification.ProgressStyle()
                .setStyledByProgress(true)
                .setProgressStartIcon(Icon.createWithResource(context, appIconResourceId))
                .setProgressTrackerIcon(Icon.createWithBitmap(workoutProgressTrackerDot()))

            // Without a custom tracker icon, ProgressStyle uses its native
            // circular marker instead of the previous dumbbell graphic.

            if (isResting) {
                val duration = progressMax.coerceAtLeast(1)
                liveStyle.addProgressSegment(
                    android.app.Notification.ProgressStyle.Segment(duration)
                        .setColor(workoutProgressCurrent)
                )
                // The remaining seconds are the progress value so the tracker
                // travels back towards zero as the rest counts down.
                liveStyle.setProgress(progress.coerceIn(0, duration))
            } else {
                repeat(count) { segmentIndex ->
                    val segmentColor = when {
                        segmentIndex < progress -> workoutProgressCompleted
                        segmentIndex == progress -> workoutProgressCurrent
                        else -> workoutProgressPending
                    }
                    liveStyle.addProgressSegment(
                        android.app.Notification.ProgressStyle.Segment(1)
                            .setColor(segmentColor)
                    )
                }
                liveStyle.setProgress(progress.coerceIn(0, count))
            }
            builder.setStyle(liveStyle)
        } else {
            builder.setLargeIcon(appIconBitmap(context) ?: notificationArtwork)
            if (isResting) {
                builder.setProgress(
                    progressMax.coerceAtLeast(1),
                    progress.coerceIn(0, progressMax.coerceAtLeast(1)),
                    false,
                )
            } else {
                builder.setProgress(count, progress.coerceIn(0, count), false)
            }
        }

        if (isResting && progress > 0) {
            // Notification.when is wall-clock based. elapsedRealtime() makes
            // the system chronometer resolve to zero/invalid on the lockscreen.
            builder.setWhen(restEndAtMillis ?: System.currentTimeMillis() + progress * 1_000L)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setShowWhen(true)
        } else {
            builder.setShowWhen(false)
        }

        if (contentIntent != null) builder.setContentIntent(contentIntent)
        if (alert) {
            builder.setOnlyAlertOnce(false)
                .setPriority(android.app.Notification.PRIORITY_HIGH)
                .setTicker("¡Listo!")
                .setWhen(System.currentTimeMillis())
                .setShowWhen(true)
                .setLights(workoutProgressCurrent, 500, 500)
        } else {
            builder.setSound(null).setVibrate(null)
        }

        if (isResting) {
            if (contentIntent != null) {
                builder.addAction(android.app.Notification.Action.Builder(Icon.createWithResource(context, android.R.drawable.ic_menu_view), "Abrir", contentIntent).build())
            }
            builder.addAction(android.app.Notification.Action.Builder(Icon.createWithResource(context, android.R.drawable.ic_media_next), "Omitir", skipIntent).build())
            builder.addAction(android.app.Notification.Action.Builder(Icon.createWithResource(context, android.R.drawable.ic_input_add), "+30 s", addRestIntent).build())
        } else if (canToggleExerciseTimer) {
            builder.addAction(android.app.Notification.Action.Builder(Icon.createWithResource(context, android.R.drawable.ic_media_pause), "Pausa/Reanuda", actionIntent(context, ACTION_TOGGLE_TIMER, 4202)).build())
        }

        return builder.build()
    }

    private fun actionIntent(context: Context, action: String, requestCode: Int): android.app.PendingIntent =
        android.app.PendingIntent.getBroadcast(context, requestCode, Intent(context, WorkoutNotificationActionReceiver::class.java).setAction(action), android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE)

    private fun ensureChannel(manager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) manager.createNotificationChannel(NotificationChannel(channelId, "Entrenamiento activo", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Progreso, descansos y ejercicio actual"
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            setSound(null, null)
            enableVibration(false)
        })
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

    private fun appIconBitmap(context: Context): Bitmap? =
        BitmapFactory.decodeResource(context.resources, context.applicationInfo.icon)

    private fun workoutProgressTrackerDot(): Bitmap {
        val size = 36
        return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).also { bitmap ->
            Canvas(bitmap).drawCircle(
                size / 2f,
                size / 2f,
                size * 0.28f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt() },
            )
        }
    }

    private fun centeredSquareArtwork(source: Bitmap): Bitmap {
        if (liveArtworkSource === source && liveArtwork != null) return liveArtwork!!

        val size = 384
        val result = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        // Fill the square with a dimmed cover version, then fit the original
        // image on top. This keeps portrait and landscape exercise photos
        // recognisable without stretching or clipping their subject.
        val coverScale = maxOf(size.toFloat() / source.width, size.toFloat() / source.height)
        val coverWidth = source.width * coverScale
        val coverHeight = source.height * coverScale
        canvas.drawBitmap(
            source,
            null,
            RectF(
                (size - coverWidth) / 2f,
                (size - coverHeight) / 2f,
                (size + coverWidth) / 2f,
                (size + coverHeight) / 2f,
            ),
            Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply { alpha = 96 },
        )
        canvas.drawColor(0x66000000)

        val fitScale = minOf(size.toFloat() / source.width, size.toFloat() / source.height)
        val fitWidth = source.width * fitScale
        val fitHeight = source.height * fitScale
        canvas.drawBitmap(
            source,
            null,
            RectF(
                (size - fitWidth) / 2f,
                (size - fitHeight) / 2f,
                (size + fitWidth) / 2f,
                (size + fitHeight) / 2f,
            ),
            Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG),
        )

        liveArtworkSource = source
        liveArtwork = result
        return result
    }

    fun cancelRestFinished(context: Context) {
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(restFinishedNotificationId)
    }

    fun cancel(context: Context) {
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(notificationId)
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(restFinishedNotificationId)
        artwork = null
        artworkTitle = null
        liveArtwork = null
        liveArtworkSource = null
    }
}
