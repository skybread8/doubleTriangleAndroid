package io.codepassion.doubletriangle.feature.workout

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.Handler
import android.os.Looper

internal class WorkoutForegroundService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private var workoutId: String? = null
    private var restEndsAtMillis: Long? = null
    private val updater = object : Runnable {
        override fun run() {
            val id = workoutId
            val snapshot = id?.let { WorkoutSessionStore.load(this@WorkoutForegroundService, it) }
            val remaining = snapshot?.restRemaining
            if (remaining != null) {
                val end = restEndsAtMillis ?: (System.currentTimeMillis() + remaining * 1_000L).also { restEndsAtMillis = it }
                val seconds = ((end - System.currentTimeMillis()) / 1_000L).coerceAtLeast(0L).toInt()
                WorkoutActiveNotification.show(this@WorkoutForegroundService, currentTitle, "Descanso: ${seconds}s", progress = (snapshot.restInitialSeconds - seconds).coerceAtLeast(0), progressMax = snapshot.restInitialSeconds, isResting = true)
                if (seconds == 0) restEndsAtMillis = null
            } else {
                restEndsAtMillis = null
            }
            handler.postDelayed(this, 1_000L)
        }
    }
    private var currentTitle: String = "Entrenamiento activo"

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        currentTitle = intent?.getStringExtra("title") ?: "Entrenamiento activo"
        workoutId = intent?.getStringExtra("workoutId")
        val detail = intent?.getStringExtra("detail") ?: "Sesión activa"
        startForeground(4101, WorkoutActiveNotification.build(this, currentTitle, detail))
        handler.removeCallbacks(updater)
        handler.post(updater)
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(updater)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
