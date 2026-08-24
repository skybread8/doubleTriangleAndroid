package io.codepassion.doubletriangle.feature.workout

import android.app.Service
import android.content.Intent
import android.os.IBinder

internal class WorkoutForegroundService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val title = intent?.getStringExtra("title") ?: "Entrenamiento activo"
        val detail = intent?.getStringExtra("detail") ?: "Sesión activa"
        startForeground(4101, WorkoutActiveNotification.build(this, title, detail))
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
