package io.codepassion.doubletriangle.feature.workout

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

internal object WorkoutNotificationActionStore {
    private const val PREFS = "wildforce_notification_actions"
    fun post(context: Context, action: String) {
        context.getSharedPreferences(PREFS, 0).edit()
            .putString("action", action)
            .putLong("timestamp", System.currentTimeMillis())
            .apply()
    }
    fun read(context: Context): Pair<String, Long>? {
        val preferences = context.getSharedPreferences(PREFS, 0)
        return preferences.getString("action", null)?.let { it to preferences.getLong("timestamp", 0L) }
    }
    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, 0).edit()
            .remove("action")
            .remove("timestamp")
            .remove("handledTimestamp")
            .apply()
    }

    fun markHandled(context: Context, timestamp: Long) {
        context.getSharedPreferences(PREFS, 0).edit().putLong("handledTimestamp", timestamp).apply()
    }

    fun wasHandled(context: Context, timestamp: Long): Boolean =
        context.getSharedPreferences(PREFS, 0).getLong("handledTimestamp", -1L) == timestamp
}

internal class WorkoutNotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        intent.action?.let { WorkoutNotificationActionStore.post(context, it) }
    }
}
