package io.codepassion.doubletriangle.feature.workout

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

internal object WorkoutNotificationActionStore {
    private const val PREFS = "wildforce_notification_actions"
    private const val ACTION = "action"
    private const val TIMESTAMP = "timestamp"

    fun post(context: Context, action: String) {
        context.getSharedPreferences(PREFS, 0).edit()
            .putString(ACTION, action)
            .putLong(TIMESTAMP, System.currentTimeMillis())
            .apply()
    }

    fun read(context: Context): Pair<String, Long>? {
        val preferences = context.getSharedPreferences(PREFS, 0)
        val action = preferences.getString(ACTION, null) ?: return null
        return action to preferences.getLong(TIMESTAMP, 0L)
    }
}

internal class WorkoutNotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        intent.action?.let { WorkoutNotificationActionStore.post(context, it) }
    }
}
