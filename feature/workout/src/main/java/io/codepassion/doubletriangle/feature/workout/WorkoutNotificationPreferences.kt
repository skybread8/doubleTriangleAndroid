package io.codepassion.doubletriangle.feature.workout

import android.content.Context

/** Shared notification switches used by the workout UI and reminder scheduler. */
object WorkoutNotificationPreferences {
    private const val PREFS = "wildforce_notification_settings"
    private const val ENABLED = "enabled"
    private const val REST_ALERTS = "rest_alerts"
    private const val REMINDERS = "reminders"
    private const val COMPLETION_EDUCATION_SEEN = "completion_education_seen"

    fun enabled(context: Context): Boolean = context.getSharedPreferences(PREFS, 0).getBoolean(ENABLED, true)
    fun restAlertsEnabled(context: Context): Boolean = context.getSharedPreferences(PREFS, 0).getBoolean(REST_ALERTS, true)
    fun remindersEnabled(context: Context): Boolean = context.getSharedPreferences(PREFS, 0).getBoolean(REMINDERS, true)
    fun completionEducationSeen(context: Context): Boolean = context.getSharedPreferences(PREFS, 0).getBoolean(COMPLETION_EDUCATION_SEEN, false)
    fun markCompletionEducationSeen(context: Context) {
        context.getSharedPreferences(PREFS, 0).edit().putBoolean(COMPLETION_EDUCATION_SEEN, true).apply()
    }
}
