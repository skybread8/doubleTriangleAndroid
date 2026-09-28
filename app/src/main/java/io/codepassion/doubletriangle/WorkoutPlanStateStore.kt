package io.codepassion.doubletriangle

import android.content.SharedPreferences

/**
 * Local persistence boundary for the currently selected training plan.
 *
 * Callers depend on this contract rather than preference keys. Its implementation can therefore
 * move to Room and later to synced records without changing app flows.
 */
internal class WorkoutPlanStateStore(
    private val preferences: SharedPreferences,
) {
    fun currentPlanJson(): String? = preferences.getString(CURRENT_PLAN_KEY, null)

    fun saveCurrentPlan(rawJson: String) {
        require(rawJson.isNotBlank()) { "No se puede guardar un plan vacío." }
        preferences.edit().putString(CURRENT_PLAN_KEY, rawJson).apply()
    }

    /**
     * Stores the training-profile snapshot used to create the current plan.  It is
     * deliberately kept outside the AI response so plans saved before this field
     * existed remain valid rather than suddenly asking users to recreate them.
     */
    fun saveProfileSignature(signature: String) {
        preferences.edit().putString(PROFILE_SIGNATURE_KEY, signature).apply()
    }

    fun requiresRegeneration(currentProfileSignature: String): Boolean {
        val savedSignature = preferences.getString(PROFILE_SIGNATURE_KEY, null)
        return currentPlanJson() != null && savedSignature != null && savedSignature != currentProfileSignature
    }

    fun clearCurrentPlan() {
        preferences.edit().remove(CURRENT_PLAN_KEY).remove(PROFILE_SIGNATURE_KEY).apply()
    }

    fun replaceCurrentPlan(rawJson: String) {
        currentPlanJson()?.let { previous -> WorkoutPlanArchiveStore.archive(preferences, previous) }
        saveCurrentPlan(rawJson)
    }

    fun archivedPlans(): List<ArchivedWorkoutPlan> = WorkoutPlanArchiveStore.load(preferences)

    private companion object {
        const val CURRENT_PLAN_KEY = "workout_plan_json"
        const val PROFILE_SIGNATURE_KEY = "workout_plan_profile_signature"
    }
}
