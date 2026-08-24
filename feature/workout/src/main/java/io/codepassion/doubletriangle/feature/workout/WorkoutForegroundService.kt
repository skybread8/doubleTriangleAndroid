package io.codepassion.doubletriangle.feature.workout

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.Handler
import android.os.Looper

internal class WorkoutForegroundService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private var workoutId: String? = null
    private var restWasActive = false
    private val restMonitor = object : Runnable {
        override fun run() {
            workoutId?.let { id ->
                val remaining = WorkoutSessionStore.load(this@WorkoutForegroundService, id)?.restRemaining
                if (remaining != null && remaining > 0) restWasActive = true
                if (restWasActive && (remaining == null || remaining <= 0)) {
                    restWasActive = false
                    WorkoutActiveNotification.show(this@WorkoutForegroundService, "Descanso terminado", "Puedes continuar con el siguiente ejercicio", headsUp = true)
                }
            }
            handler.postDelayed(this, 750L)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val title = intent?.getStringExtra("title") ?: "Entrenamiento activo"
        val detail = intent?.getStringExtra("detail") ?: "Sesión activa"
        workoutId = intent?.getStringExtra("workoutId")
        val notification = runCatching { WorkoutActiveNotification.build(this, title, detail) }.getOrElse {
            @Suppress("DEPRECATION")
            (if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) android.app.Notification.Builder(this, "active_workout_live") else android.app.Notification.Builder(this))
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
