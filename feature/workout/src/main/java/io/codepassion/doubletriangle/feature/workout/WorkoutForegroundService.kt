package io.codepassion.doubletriangle.feature.workout

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.Handler
import android.os.Looper

internal class WorkoutForegroundService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private var workoutId: String? = null
    private var currentTitle: String = "Entrenamiento activo"
    private var totalExercises: Int = 0
    private var restWasActive = false
    private var lastNotifiedRemaining: Int? = null
    private var restEndAtMillis: Long? = null
    private var lastObservedRest: Int? = null
    private var lastActionTimestamp: Long = 0L
    private val restMonitor = object : Runnable {
        override fun run() {
            workoutId?.let { id ->
                var snapshot = WorkoutSessionStore.load(this@WorkoutForegroundService, id)
                val appForeground = getSharedPreferences("wildforce_notification_settings", 0).getBoolean("app_foreground", false)
                if (!appForeground) {
                    WorkoutNotificationActionStore.read(this@WorkoutForegroundService)?.let { (action, timestamp) ->
                        if (timestamp > lastActionTimestamp) {
                            lastActionTimestamp = timestamp
                            snapshot?.let { current ->
                                snapshot = when (action) {
                                    WorkoutActiveNotification.ACTION_ADD_REST -> current.restRemaining?.let { current.copy(restRemaining = it + 30, restInitialSeconds = current.restInitialSeconds + 30) } ?: current
                                    WorkoutActiveNotification.ACTION_SKIP_CURRENT -> current.copy(restRemaining = null, restBetweenExercises = false)
                                    else -> current
                                }
                                snapshot?.let { WorkoutSessionStore.save(this@WorkoutForegroundService, id, it) }
                                WorkoutNotificationActionStore.clear(this@WorkoutForegroundService)
                                if (action == WorkoutActiveNotification.ACTION_SKIP_CURRENT) {
                                    // El contador anterior no puede seguir siendo
                                    // válido después de omitir el descanso.
                                    restEndAtMillis = null
                                    lastObservedRest = null
                                    lastNotifiedRemaining = null
                                }
                            }
                        }
                    }
                }
                val storedRemaining = snapshot?.restRemaining
                val now = System.currentTimeMillis()
                if (storedRemaining != null && storedRemaining > 0) {
                    val previous = lastObservedRest
                    if (restEndAtMillis == null || previous == null) {
                        restEndAtMillis = now + storedRemaining * 1_000L
                    } else if (storedRemaining > previous + 1) {
                        restEndAtMillis = (restEndAtMillis ?: now) + (storedRemaining - previous) * 1_000L
                    }
                    lastObservedRest = storedRemaining
                }
                val remaining = restEndAtMillis?.let { ((it - now).coerceAtLeast(0L) / 1_000L).toInt() }
                if (!appForeground && remaining != null && remaining > 0) {
                    restWasActive = true
                    if (remaining != lastNotifiedRemaining) {
                        lastNotifiedRemaining = remaining
                        WorkoutActiveNotification.show(
                            this@WorkoutForegroundService,
                            currentTitle,
                            "${remaining}s restantes",
                            progress = remaining,
                            progressMax = snapshot?.restInitialSeconds ?: remaining,
                            isResting = true,
                        )
                    }
                }
                if (!appForeground && restWasActive && (remaining == null || remaining <= 0)) {
                    restWasActive = false
                    lastNotifiedRemaining = null
                    restEndAtMillis = null
                    lastObservedRest = null
                    val currentExercise = snapshot?.exerciseIndex ?: 0
                    WorkoutActiveNotification.show(
                        this@WorkoutForegroundService,
                        currentTitle,
                        "Ejercicio ${currentExercise + 1}/${totalExercises.coerceAtLeast(1)}",
                        progress = currentExercise.coerceIn(0, totalExercises.coerceAtLeast(1)),
                        progressMax = totalExercises.coerceAtLeast(1),
                        segmentedProgress = true,
                    )
                    if (!appForeground) WorkoutActiveNotification.showRestCompletedInPlace(this@WorkoutForegroundService, currentTitle)
                }
            }
            handler.postDelayed(this, 750L)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val title = intent?.getStringExtra("title") ?: "Entrenamiento activo"
        val detail = intent?.getStringExtra("detail") ?: "Sesión activa"
        workoutId = intent?.getStringExtra("workoutId")
        totalExercises = intent?.getIntExtra("totalExercises", 0) ?: 0
        currentTitle = title
        lastActionTimestamp = WorkoutNotificationActionStore.read(this)?.second ?: 0L
        val notification = runCatching { WorkoutActiveNotification.build(this, title, detail) }.getOrElse {
            @Suppress("DEPRECATION")
            (if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) android.app.Notification.Builder(this, "active_workout_live_v2") else android.app.Notification.Builder(this))
                .setSmallIcon(android.R.drawable.ic_media_play).setContentTitle(title).setContentText(detail).setOngoing(true).build()
        }
        startForeground(4101, notification)
        handler.removeCallbacks(restMonitor)
        handler.post(restMonitor)
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(restMonitor)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
