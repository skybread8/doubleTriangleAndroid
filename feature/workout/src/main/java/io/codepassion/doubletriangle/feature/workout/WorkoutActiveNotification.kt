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
import android.graphics.RectF
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.media.RingtoneManager
import android.graphics.drawable.Icon
import android.widget.RemoteViews

internal object WorkoutActiveNotification {
    const val ACTION_TOGGLE_TIMER = "io.codepassion.doubletriangle.ACTION_TOGGLE_TIMER"
    const val ACTION_SKIP_CURRENT = "io.codepassion.doubletriangle.ACTION_SKIP_CURRENT"
    const val ACTION_ADD_REST = "io.codepassion.doubletriangle.ACTION_ADD_REST"
    // Canal nuevo para que Android 16 no herede la configuración silenciosa
    // del canal anterior, que algunos dispositivos ocultaban en lockscreen.
    private const val channelId = "active_workout_live_v3"
    private const val notificationId = 4101
    private const val restFinishedNotificationId = 4105
    private var artworkTitle: String? = null
    private var artwork: Bitmap? = null
    private var liveArtworkSource: Bitmap? = null
    private var liveArtwork: Bitmap? = null

    fun show(context: Context, title: String, detail: String = "Sesión activa", headsUp: Boolean = false, progress: Int = 0, progressMax: Int = 0, isResting: Boolean = false, artwork: Bitmap? = null, segmentedProgress: Boolean = false) {
        runCatching {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            ensureChannel(manager)
            manager.cancel(restFinishedNotificationId)
            val notification = build(context, title, detail, headsUp, progress, progressMax, isResting, artwork, segmentedProgress)
            manager.notify(notificationId, notification)
        }
    }

    fun showRestCompletedInPlace(context: Context, title: String) {
        runCatching {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            ensureChannel(manager)
            val notification = build(
                context,
                title,
                "DESCANSO TERMINADO · Continúa con el siguiente ejercicio",
                progress = 0,
                progressMax = 0,
                isResting = false,
                segmentedProgress = false,
                alert = true,
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
    fun build(context: Context, title: String, detail: String, headsUp: Boolean = false, progress: Int = 0, progressMax: Int = 0, isResting: Boolean = false, artwork: Bitmap? = null, segmentedProgress: Boolean = false, alert: Boolean = false): android.app.Notification {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = launchIntent?.let {
            android.app.PendingIntent.getActivity(context, notificationId, it, android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE)
        }
        if (artwork != null) this.artwork = artwork
        val resolvedArtwork = artwork ?: this.artwork
        val displayTitle = if (isResting) "Descanso · $detail" else title
        val displayDetail = if (isResting) "" else detail
        @Suppress("DEPRECATION")
        val builder: android.app.Notification.Builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.app.Notification.Builder(context, channelId)
        } else {
            android.app.Notification.Builder(context)
        }
        val skipIntent = actionIntent(context, ACTION_SKIP_CURRENT, 4103)
        val addRestIntent = actionIntent(context, ACTION_ADD_REST, 4104)
        val notificationArtwork = resolvedArtwork ?: exerciseArtwork(title)
        val systemArtwork = if (Build.VERSION.SDK_INT >= 36) centeredSquareArtwork(notificationArtwork) else notificationArtwork
        val isSamsung = Build.MANUFACTURER.equals("samsung", ignoreCase = true) || Build.BRAND.equals("samsung", ignoreCase = true)
        val progressText = displayDetail
        val compactView = notificationView(context, R.layout.notification_workout_compact, notificationArtwork, displayTitle, progressText, progress, progressMax, segmentedProgress, isResting)
        val expandedView = notificationView(context, R.layout.notification_workout_expanded, notificationArtwork, displayTitle, progressText, progress, progressMax, segmentedProgress, isResting)
        val useLiveUpdate = Build.VERSION.SDK_INT >= 36 && !alert
        builder.setSmallIcon(R.drawable.ic_workout_live)
            .setContentTitle(displayTitle)
            .setContentText(displayDetail)
            .setSubText(if (isResting) "" else "Sesión activa")
            .setOngoing(true)
            .setVisibility(android.app.Notification.VISIBILITY_PUBLIC)
            .setCategory(android.app.Notification.CATEGORY_WORKOUT)
            .setOnlyAlertOnce(true)
            .setShowWhen(isResting)
            .setAutoCancel(false)
            .setColor(0xFFD9A441.toInt())
        if (useLiveUpdate) {
            // EXTRA_REQUEST_PROMOTED_ONGOING funciona también en las primeras
            // revisiones de Android 16, anteriores al método 36.1 del Builder.
            builder.extras.putBoolean("android.requestPromotedOngoing", true)
            runCatching {
                builder.setRequestPromotedOngoing(true)
                val criticalText = if (isResting) "${progress}s" else if (progressMax > 0) "$progress/$progressMax" else "ACTIVO"
                builder.setShortCriticalText(criticalText.take(7))
            }
            val liveStyle = android.app.Notification.ProgressStyle()
                .setStyledByProgress(true)
                .setProgressTrackerIcon(Icon.createWithResource(context, R.drawable.ic_workout_live))
            if (!isSamsung) {
                // Pixel y otros sistemas muestran este icono como imagen del
                // ejercicio. One UI puede degradar la promoción si se incluye.
                liveStyle.setProgressStartIcon(Icon.createWithBitmap(systemArtwork))
            }
            if (progressMax > 0) {
                if (segmentedProgress) {
                    repeat(progressMax.coerceAtLeast(1)) {
                        liveStyle.addProgressSegment(
                            android.app.Notification.ProgressStyle.Segment(1).setColor(0xFFD9A441.toInt())
                        )
                    }
                    liveStyle.setProgress(progress.coerceIn(0, progressMax))
                } else {
                    liveStyle.addProgressSegment(
                        android.app.Notification.ProgressStyle.Segment(progressMax.coerceAtLeast(1)).setColor(0xFFD9A441.toInt())
                    )
                    liveStyle.setProgress((progressMax - progress).coerceIn(0, progressMax))
                }
            } else {
                liveStyle.setProgressIndeterminate(true)
            }
            builder.setStyle(liveStyle)
            if (isResting && progress > 0) {
                builder.setWhen(System.currentTimeMillis() + progress * 1_000L)
                    .setUsesChronometer(true)
                    .setChronometerCountDown(true)
                    .setShowWhen(true)
            } else {
                builder.setShowWhen(false)
            }
        } else {
            builder.setLargeIcon(notificationArtwork)
                .setCustomContentView(compactView)
                .setCustomBigContentView(expandedView)
        }
        if (contentIntent != null) builder.setContentIntent(contentIntent)
        if (alert) {
            @Suppress("DEPRECATION")
            builder.setOnlyAlertOnce(false)
                .setPriority(android.app.Notification.PRIORITY_HIGH)
                .setTicker("Descanso terminado")
                .setWhen(System.currentTimeMillis())
                .setShowWhen(true)
                .setLights(0xFFD9A441.toInt(), 500, 500)
        } else {
            builder.setSound(null).setVibrate(null)
        }
        if (isResting) {
            builder.addAction(android.app.Notification.Action.Builder(Icon.createWithResource(context, android.R.drawable.ic_media_next), "Omitir", skipIntent).build())
            builder.addAction(android.app.Notification.Action.Builder(Icon.createWithResource(context, android.R.drawable.ic_input_add), "+30 s", addRestIntent).build())
        }
        if (!useLiveUpdate) {
            if (progressMax > 0) builder.setProgress(progressMax, progress.coerceIn(0, progressMax), false)
            builder.setStyle(android.app.Notification.BigTextStyle().bigText(displayDetail))
        }
        if (!useLiveUpdate) {
            val publicVersion = android.app.Notification.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_workout_live)
                .setContentTitle(displayTitle)
                .setContentText(if (displayDetail.isBlank()) detail else displayDetail)
                .setVisibility(android.app.Notification.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setShowWhen(false)
                .setOnlyAlertOnce(true)
                .build()
            builder.setPublicVersion(publicVersion)
        }
        return builder.build()
    }

    private fun notificationView(
        context: Context,
        layout: Int,
        artwork: Bitmap,
        title: String,
        detail: String,
        progress: Int,
        progressMax: Int,
        segmentedProgress: Boolean,
        isResting: Boolean,
    ): RemoteViews = RemoteViews(context.packageName, layout).apply {
        setImageViewBitmap(R.id.notification_artwork, artwork)
        setImageViewBitmap(R.id.notification_progress_visual, progressArtwork(context, progress, progressMax, segmentedProgress))
        setTextViewText(R.id.notification_title, title)
        setTextViewText(R.id.notification_detail, detail)
        if (layout == R.layout.notification_workout_expanded) {
            setOnClickPendingIntent(R.id.notification_skip, actionIntent(context, ACTION_SKIP_CURRENT, 4203))
            setOnClickPendingIntent(R.id.notification_add, actionIntent(context, ACTION_ADD_REST, 4204))
            setViewVisibility(R.id.notification_skip, if (isResting) android.view.View.VISIBLE else android.view.View.GONE)
            setViewVisibility(R.id.notification_add, if (isResting) android.view.View.VISIBLE else android.view.View.GONE)
        }
    }

    private fun progressArtwork(context: Context, progress: Int, progressMax: Int, segmented: Boolean): Bitmap {
        val width: Int = 480
        val height: Int = 64
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val track = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xAA888888.toInt() }
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFD9A441.toInt() }
        val radius: Float = 12f
        canvas.drawRoundRect(RectF(0f, 20f, width.toFloat(), 44f), radius, radius, track)
        if (progressMax <= 0) return bitmap
        val ratio: Float = (progress.toFloat() / progressMax.toFloat()).coerceIn(0f, 1f)
        val end: Float = (width.toFloat() * ratio).coerceIn(10f, width.toFloat() - 10f)
        if (segmented) {
            val gap = 20f
            val segmentWidth = (width - gap * (progressMax - 1).coerceAtLeast(0)) / progressMax.toFloat()
            repeat(progressMax) { index ->
                val left = index * (segmentWidth + gap)
                val right = left + segmentWidth
                canvas.drawRoundRect(RectF(left, 20f, right, 44f), radius, radius, if (index < progress) fill else track)
            }
        } else {
            canvas.drawRoundRect(RectF(0f, 20f, end, 44f), radius, radius, fill)
        }
        context.getDrawable(R.drawable.ic_workout_live)?.let { icon ->
            val iconSize: Int = 64
            icon.setTint(0xFFFFD77A.toInt())
            icon.setBounds((end - iconSize / 2).toInt(), 0, (end + iconSize / 2).toInt(), iconSize)
            icon.draw(canvas)
        }
        return bitmap
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

    private fun centeredSquareArtwork(source: Bitmap): Bitmap {
        if (liveArtworkSource === source && liveArtwork != null) return liveArtwork!!
        val size = 384
        val result = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        // Fondo integrado con la propia imagen, cubriendo el cuadrado sin
        // deformarla. Se oscurece para que el ejercicio principal destaque.
        val backgroundScale = maxOf(size.toFloat() / source.width, size.toFloat() / source.height)
        val backgroundWidth = source.width * backgroundScale
        val backgroundHeight = source.height * backgroundScale
        canvas.drawBitmap(
            source,
            null,
            RectF(
                (size - backgroundWidth) / 2f,
                (size - backgroundHeight) / 2f,
                (size + backgroundWidth) / 2f,
                (size + backgroundHeight) / 2f,
            ),
            Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply { alpha = 115 },
        )
        canvas.drawColor(0x66000000)

        // La fotografía completa queda centrada, sin recorte ni estiramiento.
        val foregroundScale = minOf(size.toFloat() / source.width, size.toFloat() / source.height)
        val foregroundWidth = source.width * foregroundScale
        val foregroundHeight = source.height * foregroundScale
        canvas.drawBitmap(
            source,
            null,
            RectF(
                (size - foregroundWidth) / 2f,
                (size - foregroundHeight) / 2f,
                (size + foregroundWidth) / 2f,
                (size + foregroundHeight) / 2f,
            ),
            Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG),
        )
        liveArtworkSource = source
        liveArtwork = result
        return result
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

    fun cancel(context: Context) {
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(notificationId)
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(restFinishedNotificationId)
        artwork = null
        artworkTitle = null
    }
}
