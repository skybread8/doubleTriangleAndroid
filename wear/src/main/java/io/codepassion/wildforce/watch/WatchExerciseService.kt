package io.codepassion.wildforce.watch

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

/** Keeps the Health Services exercise alive when the watch screen sleeps. */
internal class WatchExerciseService : Service() {
    private lateinit var healthServices: WatchHealthServicesManager

    override fun onCreate() {
        super.onCreate()
        healthServices = WatchHealthServicesManager(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            healthServices.stop()
            stopSelf()
            return START_NOT_STICKY
        }
        startForeground(NOTIFICATION_ID, notification())
        healthServices.start()
        return START_STICKY
    }

    override fun onDestroy() {
        healthServices.stop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Entrenamiento activo", NotificationManager.IMPORTANCE_LOW),
            )
        }
    }

    private fun notification(): Notification = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_media_play)
        .setContentTitle("Wildforce")
        .setContentText("Entrenamiento activo")
        .setOngoing(true)
        .build()

    companion object {
        const val ACTION_START = "io.codepassion.wildforce.watch.START_EXERCISE"
        const val ACTION_STOP = "io.codepassion.wildforce.watch.STOP_EXERCISE"
        const val CHANNEL_ID = "wildforce_watch_exercise"
        const val NOTIFICATION_ID = 4101

        fun start(context: android.content.Context) {
            context.startService(Intent(context, WatchExerciseService::class.java).setAction(ACTION_START))
        }

        fun stop(context: android.content.Context) {
            context.startService(Intent(context, WatchExerciseService::class.java).setAction(ACTION_STOP))
        }
    }
}
