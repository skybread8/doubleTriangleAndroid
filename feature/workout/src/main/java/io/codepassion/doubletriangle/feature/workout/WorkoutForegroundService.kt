package io.codepassion.doubletriangle.feature.workout

import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.Handler
import android.os.Looper

internal class WorkoutForegroundService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private var workoutId: String? = null
    private var currentTitle: String = "Entrenamiento activo"
    private var totalExercises: Int = 1
    private var currentExerciseHasTimer = false
    private var restWasActive = false
    private var lastNotifiedRemaining: Int? = null
    private var restEndAtMillis: Long? = null
    private var lastObservedRest: Int? = null
    private var restPausedByAction = false
    private var restCompletionShownAtMillis: Long? = null
    private var lastActionTimestamp: Long = 0L

    private val monitor = object : Runnable {
        override fun run() {
            val id = workoutId
            if (id == null) {
                handler.postDelayed(this, 1000L)
                return
            }
            val snapshot = WorkoutSessionStore.load(this@WorkoutForegroundService, id)
            if (snapshot == null) {
                // A cancelled/discarded workout has no session to monitor.
                // Stop rather than leaving a stale foreground notification.
                WorkoutNotificationActionStore.clear(this@WorkoutForegroundService)
                WorkoutActiveNotification.cancel(this@WorkoutForegroundService)
                stopSelf()
                return
            }
            val appForeground = getSharedPreferences("wildforce_notification_settings", 0).getBoolean("app_foreground", false)

            // The foreground service is the authoritative owner of notification
            // actions. Lifecycle flags can be stale while the app backgrounds.
            WorkoutNotificationActionStore.read(this@WorkoutForegroundService)?.let { (action, timestamp) ->
                if (timestamp > lastActionTimestamp) {
                    lastActionTimestamp = timestamp
                    handleAction(id, snapshot, action)
                    WorkoutNotificationActionStore.markHandled(this@WorkoutForegroundService, timestamp)
                    handler.post(this)
                    return
                }
            }

            val now = System.currentTimeMillis()
            val storedRest = snapshot.restRemaining
            val currentExerciseIndex = snapshot.exerciseIndex

            if (storedRest != null && storedRest > 0 && !restPausedByAction) {
                restWasActive = true
                if (restEndAtMillis == null || lastObservedRest == null || storedRest > (lastObservedRest ?: 0) + 1) {
                    restEndAtMillis = now + storedRest * 1_000L
                }
                lastObservedRest = storedRest

                val remaining = ((restEndAtMillis!! - now).coerceAtLeast(0L) / 1_000L).toInt()
                if (remaining <= 0) {
                    restWasActive = false
                    restEndAtMillis = null
                    lastObservedRest = null
                    lastNotifiedRemaining = 0
                    WorkoutSessionStore.save(
                        this@WorkoutForegroundService,
                        id,
                        snapshot.copy(
                            restRemaining = null,
                            restBetweenExercises = false,
                            restContext = null,
                        ),
                    )
                    if (!appForeground) {
                        WorkoutActiveNotification.showRestCompletedInPlace(
                            this@WorkoutForegroundService,
                            currentTitle,
                            currentExerciseIndex,
                            totalExercises,
                        )
                        restCompletionShownAtMillis = now
                    }
                } else if (!appForeground && remaining != lastNotifiedRemaining) {
                    lastNotifiedRemaining = remaining
                    WorkoutActiveNotification.show(
                        context = this@WorkoutForegroundService,
                        title = currentTitle,
                        progress = remaining,
                        progressMax = snapshot.restInitialSeconds,
                        type = WorkoutNotificationType.REST,
                        exerciseIndex = currentExerciseIndex,
                        totalExercises = totalExercises,
                        restEndAtMillis = restEndAtMillis,
                    )
                }
            } else {
                if (!appForeground) {
                    val completionShownAt = restCompletionShownAtMillis
                    if (completionShownAt != null) {
                        // Keep the promoted completion state (and its LISTO
                        // capsule) visible until the user returns to the app.
                        handler.postDelayed(this, 750L)
                        return
                    }
                    if (restWasActive && (storedRest == null || storedRest <= 0)) {
                        restWasActive = false
                        restEndAtMillis = null
                        lastObservedRest = null
                        lastNotifiedRemaining = 0
                        WorkoutSessionStore.save(this@WorkoutForegroundService, id, snapshot.copy(restRemaining = null, restBetweenExercises = false))
                        WorkoutActiveNotification.showRestCompletedInPlace(this@WorkoutForegroundService, currentTitle, currentExerciseIndex, totalExercises)
                    } else if (lastNotifiedRemaining != -1) {
                        lastNotifiedRemaining = -1
                        WorkoutActiveNotification.show(
                            context = this@WorkoutForegroundService,
                            title = currentTitle,
                            detail = "A por la siguiente serie",
                            progress = currentExerciseIndex,
                            progressMax = totalExercises,
                            type = WorkoutNotificationType.PROGRESS,
                            exerciseIndex = currentExerciseIndex,
                            totalExercises = totalExercises,
                            exerciseTimerSeconds = snapshot.exerciseTimeRemaining,
                            canToggleExerciseTimer = currentExerciseHasTimer,
                        )
                    }
                }

                if (storedRest == null || storedRest <= 0) {
                    restEndAtMillis = null
                    lastObservedRest = null
                    restPausedByAction = false
                    if (appForeground) restCompletionShownAtMillis = null
                }
            }

            handler.postDelayed(this, if (storedRest != null && storedRest > 0) 250L else 750L)
        }
    }

    private fun handleAction(id: String, current: WorkoutSessionSnapshot, action: String) {
        val updated = when (action) {
            WorkoutActiveNotification.ACTION_TOGGLE_TIMER -> {
                if (current.restRemaining != null) {
                    restPausedByAction = !restPausedByAction
                    restEndAtMillis = if (restPausedByAction) null else System.currentTimeMillis() + current.restRemaining * 1_000L
                    current
                } else {
                    current.copy(exerciseTimerRunning = !current.exerciseTimerRunning)
                }
            }
            WorkoutActiveNotification.ACTION_ADD_REST -> current.restRemaining?.let {
                val now = System.currentTimeMillis()
                val remaining = restEndAtMillis
                    ?.let { endAt -> ((endAt - now).coerceAtLeast(0L) / 1_000L).toInt() }
                    ?: it
                val updatedRemaining = remaining + 30
                restEndAtMillis = now + updatedRemaining * 1_000L
                lastObservedRest = updatedRemaining
                current.copy(
                    restRemaining = updatedRemaining,
                    restInitialSeconds = current.restInitialSeconds + 30,
                )
            } ?: current
            WorkoutActiveNotification.ACTION_SKIP_CURRENT -> {
                restEndAtMillis = null
                lastObservedRest = null
                restPausedByAction = false
                // Omitir no es una finalización: evita que el siguiente ciclo
                // del monitor publique brevemente el aviso de descanso terminado.
                restWasActive = false
                // Fuerza el siguiente ciclo a sustituir la tarjeta de descanso
                // por el progreso normal del entrenamiento de inmediato.
                lastNotifiedRemaining = null
                restCompletionShownAtMillis = null
                current.copy(restRemaining = null, restBetweenExercises = false, restContext = null)
            }
            else -> current
        }
        WorkoutSessionStore.save(this, id, updated)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        android.util.Log.d("WorkoutService", "onStartCommand")
        val title = intent?.getStringExtra("title") ?: currentTitle
        workoutId = intent?.getStringExtra("workoutId") ?: workoutId
        totalExercises = intent?.getIntExtra("totalExercises", totalExercises).takeIf { it != 0 } ?: totalExercises
        currentExerciseHasTimer = intent?.getBooleanExtra("currentExerciseHasTimer", currentExerciseHasTimer)
            ?: currentExerciseHasTimer
        currentTitle = title

        // Always ensure foreground notification is posted
        val snapshot = workoutId?.let { WorkoutSessionStore.load(this, it) }
        val notification = WorkoutActiveNotification.build(
            context = this,
            title = title,
            detail = "Sesión activa",
            progress = snapshot?.restRemaining ?: snapshot?.exerciseIndex ?: 0,
            progressMax = snapshot?.restInitialSeconds ?: totalExercises,
            type = if (snapshot?.restRemaining != null) WorkoutNotificationType.REST else WorkoutNotificationType.PROGRESS,
            exerciseIndex = snapshot?.exerciseIndex,
            totalExercises = totalExercises,
            exerciseTimerSeconds = snapshot?.exerciseTimeRemaining,
            canToggleExerciseTimer = currentExerciseHasTimer,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(4101, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(4101, notification)
        }

        handler.removeCallbacks(monitor)
        handler.post(monitor)
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(monitor)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

}
