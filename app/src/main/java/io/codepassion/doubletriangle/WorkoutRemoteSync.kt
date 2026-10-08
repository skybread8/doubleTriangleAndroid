package io.codepassion.doubletriangle

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import io.codepassion.doubletriangle.feature.onboarding.OnboardingProfile
import io.codepassion.doubletriangle.feature.onboarding.Equipment
import io.codepassion.doubletriangle.feature.onboarding.GymType
import io.codepassion.doubletriangle.feature.onboarding.LifestyleLevel
import io.codepassion.doubletriangle.feature.onboarding.MovementRestriction
import io.codepassion.doubletriangle.feature.onboarding.TrainingLevel
import io.codepassion.doubletriangle.feature.onboarding.TrainingLocationProfile
import io.codepassion.doubletriangle.feature.onboarding.TrainingSplitPreference
import io.codepassion.doubletriangle.feature.onboarding.WorkoutFocus
import io.codepassion.doubletriangle.feature.onboarding.WorkoutWeekday
import io.codepassion.doubletriangle.feature.onboarding.effectiveTrainingLocations
import io.codepassion.doubletriangle.core.model.WildforceApiEnvironment
import io.codepassion.doubletriangle.feature.workout.WorkoutHistoryStore
import io.codepassion.doubletriangle.feature.workout.CoachExerciseMediaStore
import io.codepassion.doubletriangle.nutrition.NutritionStore
import io.codepassion.doubletriangle.nutrition.budgetSensitivitySyncValue
import io.codepassion.doubletriangle.nutrition.cookingEffortSyncValue
import io.codepassion.doubletriangle.nutrition.dietaryStyleSyncValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/** Bridges Android's JSON plan store to the same sync graph used by iOS. */
internal object WorkoutRemoteSync {
    private const val accountPrefs = "wildforce_account"
    private const val planKey = "remote_workout_plan_id"
    private const val syncPrefs = "wildforce_remote_sync"
    private const val nutritionEntryIDsKey = "nutrition_entry_ids"
    private const val nutritionItemIDsKey = "nutrition_item_ids"
    private const val accountProgressPrefs = "wildforce_account_progress"
    private const val exerciseProfilesKey = "exercise_profiles"
    private const val skippedExercisesPrefs = "wildforce_skipped_exercises"
    private const val skippedExercisesKey = "entries"
    /** Same ordered resource set used by iOS's WildforceSync. */
    private val batchResources = listOf(
        "users", "user-app-settings", "subscriptions", "training-preferences", "training-locations",
        "body-metric-entries", "body-progress-photo-sessions", "exercise-profiles", "nutrition-profiles",
        "workout-plans", "workout-days", "workout-blocks", "planned-exercises", "exercise-results",
        "coach-exercise-media", "nutrition-plans", "nutrition-days", "nutrition-meals",
        "nutrition-log-entries", "nutrition-log-items",
    )
    private val synchronizationMutex = Mutex()

    sealed interface Outcome {
        /** A first-device import can replace the in-memory profile before the UI is shown. */
        data class Synchronized(val importedProfile: OnboardingProfile? = null) : Outcome
        data object NoAccount : Outcome
        data object SubscriptionRequired : Outcome
        data class Failed(val message: String?) : Outcome
    }

    /** Records completed-session omissions until the corresponding plan graph has uploaded them. */
    fun recordSkippedExercises(context: Context, workoutId: String, exerciseIndices: Set<Int>) {
        if (exerciseIndices.isEmpty()) return
        val preferences = context.getSharedPreferences(skippedExercisesPrefs, Context.MODE_PRIVATE)
        val entries = preferences.getStringSet(skippedExercisesKey, emptySet()).orEmpty().toMutableSet()
        val timestamp = System.currentTimeMillis()
        exerciseIndices.forEach { index ->
            entries.removeAll { it.substringBefore('|') == workoutId && it.substringAfter('|').substringBefore('|').toIntOrNull() == index }
            entries += "$workoutId|$index|$timestamp"
        }
        preferences.edit().putStringSet(skippedExercisesKey, entries).apply()
    }

    suspend fun synchronize(context: Context, profile: OnboardingProfile, preferences: SharedPreferences): Outcome = synchronizationMutex.withLock {
        withContext(Dispatchers.IO) {
        val account = context.getSharedPreferences(accountPrefs, 0)
        val token = account.getString("token", null) ?: return@withContext Outcome.NoAccount
        val userId = account.getString("id", null) ?: return@withContext Outcome.NoAccount
        try {
            val hasPendingLocalChanges = preferences.getBoolean("remote_sync_pending", false)
            val shouldImportRemoteSnapshot = preferences.getBoolean("remote_sync_import_required", false) || preferences.getString("workout_plan_json", null).isNullOrBlank()
            // A first install must never manufacture an Android snapshot over an
            // existing account. Later, only an explicitly queued local mutation
            // is pushed. In particular, do not give every app launch a new
            // server timestamp: that was overwriting newer iOS records.
            val shouldPullRemote = shouldImportRemoteSnapshot || !hasPendingLocalChanges
            val shouldPushLocal = hasPendingLocalChanges && !shouldImportRemoteSnapshot
            val importedProfile = if (shouldPullRemote) {
                val remote = pullRemoteSnapshot(token)
                val remoteProfile = pullRemoteProfile(context, remote, profile)
                pullRemoteAccountProgress(context, remote)
                pullRemoteExerciseProfiles(context, remote)
                pullWorkoutPlans(context, remote, preferences)
                pullRemoteNutrition(context, token, remote)
                pullRemoteBodyMetrics(context, remote.records("body-metric-entries"), (remoteProfile ?: profile).heightCm)
                pullRemoteAppSettings(context, remote)
                CoachExerciseMediaStore.replace(context, remote.records("coach-exercise-media"))
                // iOS pulls media on every synchronization. The local store
                // de-duplicates sessions, so this also restores progress photos
                // added from another device after the first Android sign-in.
                pullRemoteMedia(context, token, remote.records("body-progress-photo-sessions"))
                if (shouldImportRemoteSnapshot) {
                    preferences.edit().remove("remote_sync_import_required").apply()
                }
                remoteProfile
            } else null
            val profileToSync = importedProfile ?: profile
            if (shouldPushLocal) {
            val now = java.time.Instant.now().toString()
            val progress = accountProgress(context)
            // User is a first-class iOS sync resource. Keeping it here ensures
            // profile edits made on Android are visible to the other platform.
            push(token, "users", JSONArray().put(record(userId, now)
                .put("name", profileToSync.name).put("height", profileToSync.heightCm).put("weight", profileToSync.weightKg)
                .put("birth_date", "%04d-%02d-01".format(profileToSync.birthYear, profileToSync.birthMonth))
                .put("gender", profileToSync.gender.storedValue).put("language", AppLocale.languageTag(profileToSync.appLanguage))
                .put("metric_system", profileToSync.metricSystem.storedValue)
                .put("current_streak", progress.currentStreak).put("longest_streak", progress.longestStreak)
                .put("last_completed_workout_at", progress.lastCompletedWorkoutAt ?: JSONObject.NULL)
                .put("xp", progress.xp).put("xp_level", progress.xpLevel)))
            push(token, "training-preferences", JSONArray().put(record(stableId("$userId:training-preferences"), now)
                .put("goal", profileToSync.goal.storedValue).put("lifestyle", profileToSync.lifestyle.storedValue)
                .put("gym_type", profileToSync.gymType.storedValue).put("general_training_level", profileToSync.trainingLevel.storedValue)
                .put("training_split_preference", profileToSync.trainingSplitPreference.storedValue)
                .put("preferred_workout_duration_minutes", profileToSync.preferredWorkoutDurationMinutes)
                .put("workout_planner_notes", profileToSync.workoutPlannerNotes)
                .put("workout_days", JSONArray(profileToSync.workoutDays.map { it.storedValue }))
                .put("custom_workout_focuses", JSONObject(profileToSync.customWorkoutFocuses.mapKeys { it.key.storedValue }.mapValues { it.value.storedValue }))
                .put("movement_restrictions", JSONArray(profileToSync.movementRestrictions.map { it.storedValue }))
                .put("body_composition_phase", profileToSync.bodyCompositionPhase?.storedValue ?: JSONObject.NULL)
                .put("skips_warmups", profileToSync.skipsWarmups).put("skips_cooldowns", profileToSync.skipsCooldowns).put("skips_rest_periods", profileToSync.skipsRestPeriods)))
            val remoteLocationIds = context.getSharedPreferences("wildforce_remote_sync", Context.MODE_PRIVATE)
                .getString("training_location_ids", "[]")?.let(::JSONArray) ?: JSONArray()
            push(token, "training-locations", JSONArray().apply {
                // An empty explicit list still represents the selected gym and
                // equipment. Persist that effective default as a first-class
                // location so iOS and the backend receive the same context.
                profileToSync.effectiveTrainingLocations().forEachIndexed { index, location -> put(record(
                    remoteLocationIds.optString(index).takeIf(::isUuid) ?: stableId("$userId:location:$index"), now)
                    .put("name", location.name).put("is_default", location.isDefault).put("sort_order", index)
                    .put("equipment", JSONArray(location.equipment.map { it.storedValue }))) }
            })
            val detailPrefs = context.getSharedPreferences("wildforce_profile_details", Context.MODE_PRIVATE)
            push(token, "user-app-settings", JSONArray().put(record(stableId("$userId:settings"), now)
                .put("is_health_kit_enabled", profileToSync.isHealthConnectEnabled)
                .put("is_watch_auto_tracking_enabled", detailPrefs.getBoolean("amazfit_watch_enabled", false))
                .put("is_screen_on_during_workout_enabled", detailPrefs.getBoolean("keep_screen_on", true))
                .put("is_notifications_enabled", context.getSharedPreferences("wildforce_notification_settings", Context.MODE_PRIVATE).getBoolean("enabled", true))
                .put("is_full_focus_mode_enabled", detailPrefs.getBoolean("full_focus", false))
                .put("has_seen_notification_request", detailPrefs.getBoolean("has_seen_notification_request", false))
                .put("has_seen_body_progress_tutorial", detailPrefs.getBoolean("has_seen_body_progress_tutorial", false))
                .put("has_rated_app", detailPrefs.getBoolean("has_rated_app", false))))
            val bodyMetrics = context.getSharedPreferences("wildforce_body_metrics", Context.MODE_PRIVATE).getString("history", "[]") ?: "[]"
            val metricRecords = JSONArray(); val metrics = JSONArray(bodyMetrics)
            for (index in 0 until metrics.length()) { val metric = metrics.optJSONObject(index) ?: continue; val recorded = metric.optString("date") + "T12:00:00Z"; metricRecords.put(record(stableId("$userId:weight:${metric.optString("date")}"), now).put("type", "weight").put("value", metric.optDouble("weightKg")).put("recorded_at", recorded).put("source", "manual")) }
            push(token, "body-metric-entries", metricRecords)
            synchronizeAvatar(context, token)
            synchronizeBodyProgressPhotos(context, token, userId, now)

            synchronizeNutrition(token, userId, profileToSync, now, context)
            preferences.getString("workout_plan_json", null)?.let { rawPlan ->
                synchronizeWorkoutPlan(token, userId, profileToSync, now, context, preferences, JSONObject(rawPlan))
            }
            }
            Outcome.Synchronized(importedProfile)
        } catch (error: Exception) {
            Log.w("WorkoutRemoteSync", "Synchronization failed", error)
            if (error is SubscriptionRequiredException) Outcome.SubscriptionRequired
            else Outcome.Failed(error.message)
        }
        }
    }

    /**
     * iOS pulls these resources before it writes local changes.  Android has no
     * per-record history store yet, so the first sign-in is its conflict boundary:
     * the existing account is authoritative and is applied before any push.
     */
    private fun pullRemoteProfile(context: Context, remote: RemoteSnapshot, local: OnboardingProfile): OnboardingProfile? {
        val remoteUser = remote.records("users").firstLiveRecord()
        val userProfile = remoteUser?.let { user ->
            local.copy(
                name = user.optString("name", local.name).ifBlank { local.name },
                heightCm = user.optInt("height", user.optInt("height_cm", local.heightCm)).takeIf { it in 120..230 } ?: local.heightCm,
                weightKg = user.optDouble("weight", user.optDouble("weight_kg", local.weightKg)).takeIf { it.isFinite() && it in 35.0..250.0 } ?: local.weightKg,
                birthYear = user.optString("birth_date").take(4).toIntOrNull()?.coerceIn(1920, java.time.Year.now().value) ?: local.birthYear,
                birthMonth = user.optString("birth_date").drop(5).take(2).toIntOrNull()?.coerceIn(1, 12) ?: local.birthMonth,
                appLanguage = user.optString("language", local.appLanguage).ifBlank { local.appLanguage },
            )
        } ?: local
        val preferences = remote.records("training-preferences").firstLiveRecord() ?: return remoteUser?.let { userProfile }
        val remoteLocations = remote.records("training-locations").liveRecords()
        val locations = remoteLocations.mapIndexed { index, location ->
                TrainingLocationProfile(
                    name = location.optString("name").trim().ifBlank { "Ubicación ${index + 1}" },
                    equipment = location.optJSONArray("equipment").storedValues(Equipment.entries) { it.storedValue }
                        .ifEmpty { userProfile.availableEquipment },
                    isDefault = location.optBoolean("is_default", index == 0),
                )
            }
        val locationIds = remoteLocations.map { it.optString("id") }
        if (locationIds.isNotEmpty()) {
            context.getSharedPreferences("wildforce_remote_sync", Context.MODE_PRIVATE).edit()
                .putString("training_location_ids", JSONArray(locationIds).toString()).apply()
        }
        val focuses = preferences.optJSONObject("custom_workout_focuses")?.let { objectValue ->
            WorkoutWeekday.entries.mapNotNull { day ->
                objectValue.optString(day.storedValue).takeIf(String::isNotBlank)
                    ?.let { value -> WorkoutFocus.entries.firstOrNull { it.storedValue == value } }
                    ?.takeIf { it in WorkoutFocus.customSelectionCases }
                    ?.let { day to it }
            }.toMap()
        } ?: userProfile.customWorkoutFocuses
        return userProfile.copy(
            goal = io.codepassion.doubletriangle.feature.onboarding.FitnessGoal.fromStoredValue(preferences.optString("goal", userProfile.goal.storedValue)),
            lifestyle = LifestyleLevel.fromStoredValue(preferences.optString("lifestyle", userProfile.lifestyle.storedValue)),
            workoutDays = preferences.optJSONArray("workout_days").storedValues(WorkoutWeekday.entries) { it.storedValue }.ifEmpty { userProfile.workoutDays },
            preferredWorkoutDurationMinutes = preferences.optInt("preferred_workout_duration_minutes", userProfile.preferredWorkoutDurationMinutes).coerceIn(15, 180),
            trainingLevel = TrainingLevel.fromStoredValue(preferences.optString("general_training_level", userProfile.trainingLevel.storedValue)),
            trainingSplitPreference = TrainingSplitPreference.fromStoredValue(preferences.optString("training_split_preference", userProfile.trainingSplitPreference.storedValue)),
            customWorkoutFocuses = focuses,
            movementRestrictions = preferences.optJSONArray("movement_restrictions").storedValues(MovementRestriction.entries) { it.storedValue },
            skipsWarmups = preferences.optBoolean("skips_warmups", userProfile.skipsWarmups),
            skipsCooldowns = preferences.optBoolean("skips_cooldowns", userProfile.skipsCooldowns),
            skipsRestPeriods = preferences.optBoolean("skips_rest_periods", userProfile.skipsRestPeriods),
            workoutPlannerNotes = preferences.optString("workout_planner_notes", userProfile.workoutPlannerNotes),
            trainingLocations = locations.ifEmpty { userProfile.trainingLocations },
            availableEquipment = locations.firstOrNull { it.isDefault }?.equipment ?: userProfile.availableEquipment,
            gymType = if (locations.isEmpty()) userProfile.gymType else GymType.SmallGym,
        )
    }

    /** Stores the account-wide progression fields iOS keeps on User. */
    private fun pullRemoteAccountProgress(context: Context, remote: RemoteSnapshot) {
        val user = remote.records("users").firstLiveRecord() ?: return
        context.getSharedPreferences(accountProgressPrefs, Context.MODE_PRIVATE).edit()
            .putInt("current_streak", user.optInt("current_streak", 0).coerceAtLeast(0))
            .putInt("longest_streak", user.optInt("longest_streak", 0).coerceAtLeast(0))
            .putString("last_completed_workout_at", user.optString("last_completed_workout_at").takeIf(String::isNotBlank))
            .putInt("xp", user.optInt("xp", 0).coerceAtLeast(0))
            .putInt("xp_level", user.optInt("xp_level", 1).coerceAtLeast(1))
            .apply()
    }

    /**
     * Exercise profiles are not exposed by Android's current editor yet, but
     * retaining the iOS payload locally makes them available to the Android
     * planner when that editor is added and, crucially, never discards them.
     */
    private fun pullRemoteExerciseProfiles(context: Context, remote: RemoteSnapshot) {
        val profiles = remote.records("exercise-profiles")
        context.getSharedPreferences(syncPrefs, Context.MODE_PRIVATE).edit()
            .putString(exerciseProfilesKey, profiles.toString()).apply()
    }

    private fun accountProgress(context: Context): AccountProgress {
        val preferences = context.getSharedPreferences(accountProgressPrefs, Context.MODE_PRIVATE)
        return AccountProgress(
            currentStreak = preferences.getInt("current_streak", 0).coerceAtLeast(0),
            longestStreak = preferences.getInt("longest_streak", 0).coerceAtLeast(0),
            lastCompletedWorkoutAt = preferences.getString("last_completed_workout_at", null),
            xp = preferences.getInt("xp", 0).coerceAtLeast(0),
            xpLevel = preferences.getInt("xp_level", 1).coerceAtLeast(1),
        )
    }

    /** Mirrors iOS's per-session streak and base workout XP award. */
    fun recordWorkoutCompletion(context: Context, completedAt: java.time.Instant = java.time.Instant.now()) {
        val preferences = context.getSharedPreferences(accountProgressPrefs, Context.MODE_PRIVATE)
        val previous = preferences.getString("last_completed_workout_at", null)
            ?.let { runCatching { java.time.Instant.parse(it) }.getOrNull() }
        val zone = java.time.ZoneId.systemDefault()
        if (previous?.atZone(zone)?.toLocalDate() == completedAt.atZone(zone).toLocalDate()) return

        val currentStreak = preferences.getInt("current_streak", 0).coerceAtLeast(0) + 1
        val longestStreak = maxOf(preferences.getInt("longest_streak", 0).coerceAtLeast(0), currentStreak)
        val xp = preferences.getInt("xp", 0).coerceAtLeast(0) + 25
        preferences.edit()
            .putInt("current_streak", currentStreak)
            .putInt("longest_streak", longestStreak)
            .putString("last_completed_workout_at", completedAt.toString())
            .putInt("xp", xp)
            .putInt("xp_level", experienceLevelFor(xp))
            .apply()
    }

    private fun experienceLevelFor(totalXP: Int): Int {
        var level = 1
        var required = 0
        while (totalXP >= required + 60 + (level - 1) * 35) {
            required += 60 + (level - 1) * 35
            level += 1
        }
        return level
    }

    /** Imports the iOS nutrition graph so a new Android device does not upload an empty plan over it. */
    private fun pullRemoteNutrition(context: Context, token: String, remote: RemoteSnapshot) {
        // `/sync/pull` is a flat-resource API. Hydrate the plan graph before
        // passing it to the store, which otherwise expects the legacy nested
        // `days -> meals` response and silently imports nothing.
        val plans = remote.records("nutrition-plans").liveRecords()
        val days = remote.records("nutrition-days").liveRecords()
        val meals = remote.records("nutrition-meals").liveRecords()
        val mealsByDay = meals.groupBy { it.optString("nutrition_day_id") }
        plans.firstOrNull()?.let { plan ->
            val planId = plan.optString("id")
            val planDays = days.filter { it.optString("nutrition_plan_id") == planId }
            planDays.forEach { day ->
                day.put("meals", JSONArray(mealsByDay[day.optString("id")].orEmpty()))
            }
            plan.put("days", JSONArray(planDays))
            NutritionStore.importRemotePlan(context, plan)
        }
        remote.records("nutrition-profiles").firstLiveRecord()?.let { NutritionStore.importRemotePreferences(context, it) }
        val entries = remote.records("nutrition-log-entries")
        val items = remote.records("nutrition-log-items")
        NutritionStore.importRemoteLogs(context, entries, items)
        rememberRemoteNutritionIDs(context, entries, items)
        pullRemoteNutritionMedia(context, token, entries)
    }

    /** iOS keeps nutrition images private and fetches them separately from the graph snapshot. */
    private fun pullRemoteNutritionMedia(context: Context, token: String, entries: JSONArray) {
        val directory = File(context.filesDir, "NutritionLogMedia").apply { mkdirs() }
        entries.liveRecords().forEach { entry ->
            val entryId = entry.optString("id").takeIf(::isUuid) ?: return@forEach
            val image = File(directory, "$entryId.jpg")
            if (image.isFile && image.length() in 1..(5 * 1024 * 1024)) {
                NutritionStore.attachRemotePhoto(context, entryId, image.absolutePath)
                return@forEach
            }
            val temporary = File(directory, "$entryId.download")
            val connection = (URL(WildforceApiEnvironment.apiUrl("nutrition-log-entries/$entryId/image")).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"; connectTimeout = 20_000; readTimeout = 60_000
                setRequestProperty("Authorization", "Bearer $token")
            }
            try {
                if (connection.responseCode !in 200..299) return@forEach
                connection.inputStream.use { input -> temporary.outputStream().use { output -> input.copyTo(output) } }
                if (temporary.length() in 1..(5 * 1024 * 1024)) {
                    if (image.exists()) image.delete()
                    if (temporary.renameTo(image)) NutritionStore.attachRemotePhoto(context, entryId, image.absolutePath)
                }
            } catch (_: Exception) {
                // Images are optional media. Keep the graph sync usable and retry next pull.
            } finally {
                temporary.delete()
                connection.disconnect()
            }
        }
    }

    /** Keeps iOS UUIDs when Android re-uploads an imported food log. */
    private fun rememberRemoteNutritionIDs(context: Context, entries: JSONArray, items: JSONArray) {
        val entryIDs = JSONObject()
        val itemIDs = JSONObject()
        val localEntryIDs = entries.liveRecords().associate { entry ->
            stableRemoteNutritionLogId(entry.optString("id")) to entry.optString("id")
        }
        localEntryIDs.forEach { (localID, remoteID) -> entryIDs.put(localID.toString(), remoteID) }
        items.liveRecords().forEach { item ->
            val localEntryID = stableRemoteNutritionLogId(item.optString("nutrition_log_entry_id"))
            itemIDs.put("$localEntryID:${item.optInt("order_index", 0)}", item.optString("id"))
        }
        context.getSharedPreferences(syncPrefs, Context.MODE_PRIVATE).edit()
            .putString(nutritionEntryIDsKey, entryIDs.toString())
            .putString(nutritionItemIDsKey, itemIDs.toString())
            .apply()
    }

    /** Restores the weight-history portion Android can represent from iOS's body metric log. */
    private fun pullRemoteBodyMetrics(context: Context, remote: JSONArray, fallbackHeightCm: Int) {
        val history = JSONArray()
        for (index in 0 until remote.length()) {
            val entry = remote.optJSONObject(index) ?: continue
            if (!entry.isNull("deleted_at") || entry.optString("type") != "weight") continue
            val date = entry.optString("recorded_at").take(10)
            val weight = entry.optDouble("value", Double.NaN)
            if (date.isBlank() || !weight.isFinite() || weight !in 20.0..400.0) continue
            history.put(JSONObject().put("date", date).put("heightCm", fallbackHeightCm.toDouble()).put("weightKg", weight))
        }
        if (history.length() > 0) context.getSharedPreferences("wildforce_body_metrics", Context.MODE_PRIVATE)
            .edit().putString("history", history.toString()).apply()
    }

    /** Applies user settings before the next push so a fresh Android install never resets iOS choices. */
    private fun pullRemoteAppSettings(context: Context, remote: RemoteSnapshot) {
        val settings = remote.records("user-app-settings").firstLiveRecord() ?: return
        val profile = context.getSharedPreferences("wildforce_profile_details", Context.MODE_PRIVATE)
        profile.edit()
            .putBoolean("keep_screen_on", settings.optBoolean("is_screen_on_during_workout_enabled", profile.getBoolean("keep_screen_on", true)))
            .putBoolean("full_focus", settings.optBoolean("is_full_focus_mode_enabled", profile.getBoolean("full_focus", false)))
            .putBoolean("has_seen_notification_request", settings.optBoolean("has_seen_notification_request", profile.getBoolean("has_seen_notification_request", false)))
            .putBoolean("has_seen_body_progress_tutorial", settings.optBoolean("has_seen_body_progress_tutorial", profile.getBoolean("has_seen_body_progress_tutorial", false)))
            .putBoolean("has_rated_app", settings.optBoolean("has_rated_app", profile.getBoolean("has_rated_app", false)))
            .apply()
        // HealthKit and Health Connect have the same account-level meaning.
        // Permission is still requested locally by Android before any data access.
        context.getSharedPreferences("wildforce_profile", Context.MODE_PRIVATE).edit()
            .putBoolean("health_connect", settings.optBoolean("is_health_kit_enabled", false))
            .apply()
        context.getSharedPreferences("wildforce_notification_settings", Context.MODE_PRIVATE).edit()
            .putBoolean("enabled", settings.optBoolean("is_notifications_enabled", true)).apply()
    }

    private fun synchronizeNutrition(token: String, userId: String, profile: OnboardingProfile, now: String, context: Context) {
        val prefs = NutritionStore.loadPreferences(context)
        push(token, "nutrition-profiles", JSONArray().put(record(stableId("$userId:nutrition-profile"), now)
            .put("dietary_style", dietaryStyleSyncValue(prefs.dietaryStyle)).put("meals_per_day_preference", prefs.mealsPerDay)
            .put("preferred_eating_window_start_hour", prefs.eatingWindowStartHour ?: JSONObject.NULL)
            .put("preferred_eating_window_end_hour", prefs.eatingWindowEndHour ?: JSONObject.NULL)
            .put("excluded_foods", JSONArray(prefs.excludedFoods)).put("allergies_and_intolerances", JSONArray(prefs.allergiesAndIntolerances))
            .put("cooking_effort", cookingEffortSyncValue(prefs.cookingEffort)).put("budget_sensitivity", budgetSensitivitySyncValue(prefs.budgetSensitivity))
            .put("preferred_protein_sources", JSONArray(prefs.preferredProteinSources)).put("dislikes", JSONArray(prefs.dislikes))
            .put("wants_meal_suggestions", prefs.wantsSuggestions).put("notes", prefs.notes)))

        val ids = context.getSharedPreferences(syncPrefs, Context.MODE_PRIVATE)
        val entryIDs = ids.getString(nutritionEntryIDsKey, "{}")?.let(::JSONObject) ?: JSONObject()
        val itemIDs = ids.getString(nutritionItemIDsKey, "{}")?.let(::JSONObject) ?: JSONObject()
        val entries = JSONArray(); val items = JSONArray()
        NutritionStore.allForSync(context).forEach { meal ->
            val entryKey = meal.id.toString()
            val entryId = entryIDs.optString(entryKey).takeIf(::isUuid) ?: stableId("$userId:nutrition-entry:${meal.id}")
            entryIDs.put(entryKey, entryId)
            entries.put(record(entryId, now).put("title", meal.name)
                .put("logged_at", meal.loggedDate.toString() + "T12:00:00Z").put("meal_type", meal.type.name.lowercase())
                .put("notes", meal.notes).put("is_favorite", meal.isBookmarked))
            val foods = meal.items.ifEmpty { listOf(io.codepassion.doubletriangle.nutrition.MealItem(meal.name, meal.calories, meal.protein, meal.carbs, meal.fat)) }
            foods.forEachIndexed { index, food ->
                val itemKey = "${meal.id}:$index"
                val itemId = itemIDs.optString(itemKey).takeIf(::isUuid) ?: stableId("$entryId:$index")
                itemIDs.put(itemKey, itemId)
                items.put(record(itemId, now)
                .put("nutrition_log_entry_id", entryId).put("name", food.name).put("brand", food.brand.ifBlank { JSONObject.NULL })
                .put("quantity", food.quantity).put("unit", food.unit.ifBlank { JSONObject.NULL }).put("amount_grams", food.grams ?: JSONObject.NULL)
                .put("calories", food.calories).put("protein_grams", food.protein).put("carbs_grams", food.carbs)
                .put("fat_grams", food.fat).put("notes", food.notes.ifBlank { JSONObject.NULL }).put("order_index", index))
            }
        }
        ids.edit().putString(nutritionEntryIDsKey, entryIDs.toString()).putString(nutritionItemIDsKey, itemIDs.toString()).apply()
        push(token, "nutrition-log-entries", entries)
        push(token, "nutrition-log-items", items)
        // iOS stores meal photos separately from the sync graph and uploads
        // them after their entry exists. Do the same for Android photo logs.
        NutritionStore.allForSync(context).forEach { meal ->
            val entryId = entryIDs.optString(meal.id.toString()).takeIf(::isUuid)
            meal.photoPath?.takeIf { File(it).isFile }?.let { path ->
                if (entryId != null) uploadNutritionLogImage(token, entryId, File(path))
            }
        }

        val plan = NutritionStore.loadGeneratedPlan(context) ?: return
        // A server-generated plan already has an ID. Reuse it instead of
        // creating Android's deterministic duplicate on the next sync.
        val nutritionPlanId = NutritionStore.remotePlanId(context) ?: stableId("$userId:nutrition-plan")
        NutritionStore.rememberRemotePlanId(context, nutritionPlanId)
        push(token, "nutrition-plans", JSONArray().put(record(nutritionPlanId, now)
            .put("starts_on", plan.minOf { it.date }.toString() + "T00:00:00Z").put("goal", profile.goal.storedValue)
            .put("body_composition_phase", profile.bodyCompositionPhase?.storedValue ?: JSONObject.NULL)
            .put("daily_calorie_average", plan.map { it.targets.calories }.average()).put("notes", "Plan generado en Android")))
        val days = JSONArray(); val meals = JSONArray()
        plan.forEach { day ->
            val dayId = stableId("$nutritionPlanId:${day.date}")
            days.put(record(dayId, now).put("nutrition_plan_id", nutritionPlanId).put("date", day.date.toString())
                .put("weekday", day.date.dayOfWeek.name.lowercase()).put("day_type", day.type.name.lowercase())
                .put("target_calories", day.targets.calories).put("target_protein_grams", day.targets.protein)
                .put("target_carbs_grams", day.targets.carbs).put("target_fat_grams", day.targets.fat)
                .put("planned_workout_title", day.workoutTitle).put("energy_demand", day.energyDemand.name.lowercase())
                .put("notes", day.notes).put("pre_workout_guidance", day.preWorkoutGuidance ?: JSONObject.NULL)
                .put("post_workout_guidance", day.postWorkoutGuidance ?: JSONObject.NULL))
            day.meals.forEachIndexed { index, meal -> meals.put(record(stableId("$dayId:$index"), now)
                .put("nutrition_day_id", dayId).put("title", meal.title).put("order_index", index).put("meal_type", meal.type.name.lowercase())
                .put("target_calories", meal.targets.calories).put("target_protein_grams", meal.targets.protein)
                .put("target_carbs_grams", meal.targets.carbs).put("target_fat_grams", meal.targets.fat)
                .put("guidance", meal.guidance).put("example_foods", JSONArray().apply { meal.foods.forEach { (name, grams) -> put(JSONObject().put("name", name).put("grams", grams)) } })) }
        }
        push(token, "nutrition-days", days)
        push(token, "nutrition-meals", meals)
    }

    private fun uploadNutritionLogImage(token: String, entryId: String, image: File) {
        if (image.length() !in 1..(5 * 1024 * 1024)) return
        val boundary = "Wildforce-${UUID.randomUUID()}"
        val connection = URL(WildforceApiEnvironment.apiUrl("nutrition-log-entries/$entryId/image")).openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.connectTimeout = 20_000
        connection.readTimeout = 60_000
        connection.setRequestProperty("Authorization", "Bearer $token")
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        try {
            DataOutputStream(connection.outputStream).use { output ->
                output.writeBytes("--$boundary\r\nContent-Disposition: form-data; name=\"image\"; filename=\"${entryId}.jpg\"\r\nContent-Type: image/jpeg\r\n\r\n")
                image.inputStream().use { it.copyTo(output) }
                output.writeBytes("\r\n--$boundary--\r\n")
            }
            connection.inputStream.close()
        } catch (_: Exception) {
            // The local image remains attached and will be retried on the next sync.
        } finally {
            connection.disconnect()
        }
    }

    /** Uploads the two physical check-in images after their server-side session exists. */
    private fun synchronizeBodyProgressPhotos(context: Context, token: String, userId: String, now: String) {
        val raw = context.getSharedPreferences("wildforce_body_progress", Context.MODE_PRIVATE)
            .getString("sessions", "[]") ?: "[]"
        val sessions = JSONArray(raw)
        val records = JSONArray()
        val uploadQueue = mutableListOf<Triple<String, String, File>>()
        for (index in 0 until sessions.length()) {
            val session = sessions.optJSONObject(index) ?: continue
            val localId = session.optString("id").takeIf(String::isNotBlank) ?: continue
            val remoteId = stableId("$userId:body-progress:$localId")
            records.put(record(remoteId, now))
            listOf("front" to session.optString("frontPath"), "profile" to session.optString("profilePath")).forEach { (angle, path) ->
                File(path).takeIf(File::isFile)?.let { uploadQueue += Triple(remoteId, angle, it) }
            }
        }
        push(token, "body-progress-photo-sessions", records)
        val uploadState = context.getSharedPreferences("wildforce_remote_media", Context.MODE_PRIVATE)
        uploadQueue.forEach { (remoteId, angle, file) ->
            val key = "body:$remoteId:$angle"
            val fingerprint = "${file.absolutePath}:${file.length()}:${file.lastModified()}"
            if (uploadState.getString(key, null) != fingerprint) {
                uploadImage(token, WildforceApiEnvironment.apiUrl("body-progress-photo-sessions/$remoteId/photos/$angle"), file, "image/jpeg")
                uploadState.edit().putString(key, fingerprint).apply()
            }
        }
    }

    /** The API intentionally accepts a PNG avatar; local gallery files can be any image type. */
    private fun synchronizeAvatar(context: Context, token: String) {
        val file = context.getSharedPreferences("wildforce_profile", Context.MODE_PRIVATE)
            .getString("avatar_path", null)?.let(::File)?.takeIf(File::isFile) ?: return
        val fingerprint = "${file.absolutePath}:${file.length()}:${file.lastModified()}"
        val state = context.getSharedPreferences("wildforce_remote_media", Context.MODE_PRIVATE)
        if (state.getString("avatar", null) == fingerprint) return
        val decoded = BitmapFactory.decodeFile(file.absolutePath) ?: return
        val longest = maxOf(decoded.width, decoded.height).coerceAtLeast(1)
        val bitmap = if (longest <= 512) decoded else Bitmap.createScaledBitmap(decoded, decoded.width * 512 / longest, decoded.height * 512 / longest, true)
        val bytes = ByteArrayOutputStream().use { output -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, output); output.toByteArray() }
        if (bitmap !== decoded) bitmap.recycle()
        decoded.recycle()
        uploadImage(token, WildforceApiEnvironment.apiUrl("users/avatar"), "avatar.png", bytes, "image/png")
        state.edit().putString("avatar", fingerprint).apply()
    }

    /** Restores media only during the authoritative first-account import. */
    private fun pullRemoteMedia(context: Context, token: String, sessions: JSONArray) {
        val localSessions = JSONArray()
        val folder = File(context.filesDir, "body_progress_photos").apply { mkdirs() }
        for (index in 0 until sessions.length()) {
            val session = sessions.optJSONObject(index) ?: continue
            if (!session.isNull("deleted_at")) continue
            val id = session.optString("id").takeIf(String::isNotBlank) ?: continue
            if (session.optString("front_photo_path").isBlank() || session.optString("profile_photo_path").isBlank()) continue
            val front = File(folder, "${id}_front.jpg")
            val profile = File(folder, "${id}_profile.jpg")
            val frontDownloaded = download(token, WildforceApiEnvironment.apiUrl("body-progress-photo-sessions/$id/photos/front"), front)
            val profileDownloaded = download(token, WildforceApiEnvironment.apiUrl("body-progress-photo-sessions/$id/photos/profile"), profile)
            if (frontDownloaded && profileDownloaded) {
                localSessions.put(JSONObject().put("id", id).put("date", session.optString("created_at").take(10))
                    .put("frontPath", front.absolutePath).put("profilePath", profile.absolutePath))
            }
        }
        context.getSharedPreferences("wildforce_body_progress", Context.MODE_PRIVATE).edit()
            .putString("sessions", localSessions.toString()).apply()
    }

    /**
     * A first Android session must restore the server plan before creating any local
     * records. Otherwise a freshly generated empty/default plan could win the
     * last-write-wins conflict and hide the user's iOS workouts.
     */
    private fun pullWorkoutPlans(context: Context, remote: RemoteSnapshot, preferences: SharedPreferences) {
        val remotePlans = remote.records("workout-plans").liveRecords()
        if (remotePlans.isEmpty()) return
        val remoteDays = pullRemoteWorkoutDays(remote)
        importRemoteWorkoutHistory(context, remoteDays)

        val plans = remotePlans.mapNotNull { plan ->
            val planId = plan.optString("id").takeIf(String::isNotBlank) ?: return@mapNotNull null
            remotePlanJson(plan, planId, remoteDays)
        }
        if (plans.isEmpty()) return
        val active = plans.firstOrNull { it.status.equals("active", ignoreCase = true) } ?: plans.first()
        // Keep every completed mesocycle locally. The hub renders the selected
        // active plan while its history remains available to overview screens.
        plans.filterNot { it.id == active.id }.forEach { archived ->
            WorkoutPlanArchiveStore.archive(preferences, archived.rawJson)
        }
        preferences.edit().putString("workout_plan_json", active.rawJson)
            .putString(planKey, active.id)
            .putBoolean("remote_plan_imported", true).apply()
    }

    /**
     * The generic sync endpoint exposes normalized workout records. Rebuild the
     * graph expected by the Android plan importer instead of relying on the
     * removed legacy `sync/workout-days` endpoint.
     */
    private fun pullRemoteWorkoutDays(remote: RemoteSnapshot): JSONArray {
        fun records(resource: String) = remote.records(resource).liveRecords()

        val days = records("workout-days")
        val blocks = records("workout-blocks")
        val exercises = records("planned-exercises")
        val results = records("exercise-results")
        val resultsByExercise = results.groupBy { it.optString("planned_exercise_id") }
        val exercisesByBlock = exercises.groupBy { it.optString("workout_block_id") }
        val blocksByDay = blocks.groupBy { it.optString("workout_day_id") }

        exercises.forEach { exercise ->
            exercise.put("exercise_results", JSONArray(resultsByExercise[exercise.optString("id")].orEmpty()))
        }
        blocks.forEach { block ->
            block.put("exercises", JSONArray(exercisesByBlock[block.optString("id")].orEmpty()))
        }
        return JSONArray().apply {
            days.forEach { day ->
                day.put("blocks", JSONArray(blocksByDay[day.optString("id")].orEmpty()))
                put(day)
            }
        }
    }

    private fun remotePlanJson(plan: JSONObject, planId: String, remoteDays: JSONArray): RemotePlan? {
        val workouts = JSONArray()
        for (index in 0 until remoteDays.length()) {
            val day = remoteDays.optJSONObject(index) ?: continue
            if (!day.isNull("deleted_at")) continue
            if (day.optString("workout_plan_id") != planId) continue
            val blocks = JSONArray()
            val rawBlocks = day.optJSONArray("blocks") ?: JSONArray()
            for (blockIndex in 0 until rawBlocks.length()) {
                val block = rawBlocks.optJSONObject(blockIndex) ?: continue
                if (!block.isNull("deleted_at")) continue
                blocks.put(JSONObject().put("remoteId", block.optString("id")).put("type", block.optString("type", "standard"))
                    .put("rounds", block.optInt("rounds", 1)).put("restAfterBlockSeconds", block.opt("rest_after_block_seconds"))
                    .put("notes", block.opt("notes")).put("exercises", remoteExercises(block.optJSONArray("exercises"))))
            }
            // iOS no longer models day-level exercises. Read the legacy shape
            // only by migrating it into a standard block, never re-emitting it.
            if (blocks.length() == 0) {
                val legacyExercises = remoteExercises(day.optJSONArray("exercises"))
                if (legacyExercises.length() > 0) {
                    blocks.put(JSONObject().put("remoteId", "legacy-direct-exercises").put("type", "standard")
                        .put("rounds", 1).put("exercises", legacyExercises))
                }
            }
            workouts.put(JSONObject().put("id", day.optString("id")).put("remoteId", day.optString("id")).put("title", day.optString("title", "Sesión ${index + 1}"))
                .put("focus", day.optString("focus", "Fitness general")).put("dayType", day.optString("day_type", "strength"))
                .put("status", day.optString("status", "planned").uppercase()).put("weekday", day.optString("intended_weekday", "MONDAY").uppercase())
                .put("estimatedDurationMinutes", day.optInt("estimated_duration_minutes", 50)).put("blocks", blocks))
        }
        if (workouts.length() == 0) return null
        val cycleLength = plan.optInt("cycle_length", 1).coerceAtLeast(1)
        val phaseWeek = plan.optInt("phase_week", 1).coerceIn(1, cycleLength)
        val mesocycleNumber = plan.optInt("mesocycle_number", 1).coerceAtLeast(1)
        val root = JSONObject().put("planName", plan.optString("name", "Plan sincronizado"))
            .put("phase", plan.optString("phase", "Plan de tu cuenta"))
            .put("cycleLength", cycleLength).put("mesocycleNumber", mesocycleNumber)
            .put("mesocycleIndex", mesocycleNumber).put("weekIndex", phaseWeek)
            .put("phaseWeek", phaseWeek).put("workouts", workouts)
        return RemotePlan(planId, plan.optString("status"), root.toString())
    }

    private fun importRemoteWorkoutHistory(context: Context, remoteDays: JSONArray) {
        val exerciseEntries = mutableListOf<io.codepassion.doubletriangle.feature.workout.RemoteExerciseHistoryEntry>()
        val sessions = mutableListOf<TrainingSessionHistoryEntry>()
        for (index in 0 until remoteDays.length()) {
            val day = remoteDays.optJSONObject(index) ?: continue
            if (!day.isNull("deleted_at")) continue
            val results = mutableListOf<io.codepassion.doubletriangle.feature.workout.RemoteExerciseHistoryEntry>()
            fun collect(exercises: JSONArray?) {
                exercises ?: return
                for (exerciseIndex in 0 until exercises.length()) {
                    val exercise = exercises.optJSONObject(exerciseIndex) ?: continue
                    val key = exercise.optString("exercise").trim().lowercase()
                    if (key.isBlank()) continue
                    val exerciseResults = exercise.optJSONArray("exercise_results") ?: continue
                    for (resultIndex in 0 until exerciseResults.length()) {
                        val result = exerciseResults.optJSONObject(resultIndex) ?: continue
                        if (!result.isNull("deleted_at")) continue
                        val timestamp = runCatching { java.time.Instant.parse(result.optString("completed_at")).toEpochMilli() }.getOrNull() ?: continue
                        val reps = result.optJSONArray("per_set_reps").intValues()
                        val weights = result.optJSONArray("per_set_weights_kg").doubleValues()
                        val entry = io.codepassion.doubletriangle.feature.workout.RemoteExerciseHistoryEntry(
                            exerciseKey = key, timestampMillis = timestamp,
                            sets = result.optInt("completed_sets", reps.size), totalReps = result.optInt("completed_reps", reps.sum()),
                            maxWeightKg = result.optDouble("completed_weight", weights.maxOrNull() ?: 0.0),
                            setReps = reps, setWeightsKg = weights, feedback = result.optString("feedback").takeIf(String::isNotBlank),
                            note = result.optString("notes").takeIf(String::isNotBlank), durationSeconds = result.optInt("completed_duration_seconds"),
                            distanceKm = result.optDouble("completed_distance_km", 0.0).takeIf(Double::isFinite) ?: 0.0,
                        )
                        exerciseEntries += entry; results += entry
                    }
                }
            }
            collect(day.optJSONArray("exercises"))
            day.optJSONArray("blocks")?.let { blocks -> for (blockIndex in 0 until blocks.length()) collect(blocks.optJSONObject(blockIndex)?.optJSONArray("exercises")) }
            results.groupBy { it.timestampMillis }.forEach { (timestamp, completed) ->
                sessions += TrainingSessionHistoryEntry(timestamp, completed.sumOf { it.durationSeconds }, completed.sumOf { it.sets }, completed.sumOf { entry -> entry.setReps.mapIndexed { set, reps -> reps * entry.setWeightsKg.getOrElse(set) { entry.maxWeightKg } }.sum() }, day.optString("focus").takeIf(String::isNotBlank))
            }
        }
        WorkoutHistoryStore.importRemoteEntries(context, exerciseEntries)
        TrainingSessionHistoryStore.importRemote(context.getSharedPreferences("wildforce_profile", Context.MODE_PRIVATE), sessions)
    }

    private fun remoteExercises(entries: JSONArray?): JSONArray = JSONArray().apply {
        entries ?: return@apply
        for (index in 0 until entries.length()) {
            val exercise = entries.optJSONObject(index) ?: continue
            if (!exercise.isNull("deleted_at")) continue
            val min = exercise.opt("reps_min"); val max = exercise.opt("reps_max")
            val exerciseCatalogId = exercise.optString("exercise", "Ejercicio")
            put(JSONObject().put("remoteId", exercise.optString("id")).put("name", exerciseCatalogId).put("imageKey", exerciseCatalogId)
                .put("sets", exercise.optInt("sets", 1)).put("repsMin", min).put("repsMax", max)
                .put("reps", if (min is Number && max is Number) "${min.toInt()}-${max.toInt()}" else "")
                .put("restSeconds", exercise.optInt("rest_seconds", 0)).put("targetWeightKg", exercise.opt("target_weight_kg"))
                .put("targetReps", exercise.opt("target_reps")).put("targetWeightsKg", exercise.opt("target_weights_kg"))
                .put("targetDurationMinutes", exercise.opt("target_duration_minutes")).put("targetDurationSeconds", exercise.opt("target_duration_seconds"))
                .put("targetDistanceKm", exercise.opt("target_distance_km")).put("setStyle", androidSetStyle(exercise.optJSONObject("set_style_configuration")?.optString("style")))
                .put("setStyleParameters", androidSetStyleParameters(exercise.optJSONObject("set_style_configuration"))))
        }
    }

    /** The backend requires a `style` field even when no style-specific options are set. */
    private fun backendSetStyleConfiguration(item: JSONObject): JSONObject {
        val parameters = item.optJSONObject("setStyleParameters") ?: JSONObject()
        return JSONObject().put("style", backendSetStyle(item.optString("setStyle")))
            .put("applies_to_final_set_only", parameters.opt("appliesToFinalSetOnly"))
            .put("drop_count", parameters.opt("dropCount"))
            .put("drop_weight_percent", parameters.opt("dropWeightPercent"))
            .put("backoff_set_count", parameters.opt("backoffSetCount"))
            .put("backoff_weight_percent", parameters.opt("backoffWeightPercent"))
            .put("intra_set_rest_seconds", parameters.opt("intraSetRestSeconds"))
            .put("tempo", parameters.opt("tempo"))
            .put("target_rir", parameters.opt("targetRir"))
    }

    private fun androidSetStyleParameters(configuration: JSONObject?): JSONObject = JSONObject().apply {
        configuration ?: return@apply
        put("appliesToFinalSetOnly", configuration.opt("applies_to_final_set_only"))
        put("dropCount", configuration.opt("drop_count"))
        put("dropWeightPercent", configuration.opt("drop_weight_percent"))
        put("backoffSetCount", configuration.opt("backoff_set_count"))
        put("backoffWeightPercent", configuration.opt("backoff_weight_percent"))
        put("intraSetRestSeconds", configuration.opt("intra_set_rest_seconds"))
        put("tempo", configuration.opt("tempo"))
        put("targetRir", configuration.opt("target_rir"))
    }

    private fun backendSetStyle(value: String): String = when (value.trim().lowercase()) {
        "warmup" -> "warmup"
        "straight" -> "straight"
        "topsetbackoff", "top_set_backoff" -> "topSetBackoff"
        "ascendingpyramid", "ascending_pyramid" -> "ascendingPyramid"
        "dropset", "drop_set" -> "dropSet"
        "restpause", "rest_pause" -> "restPause"
        "intervals" -> "intervals"
        "tempo" -> "tempo"
        else -> "straight"
    }

    private fun androidSetStyle(value: String?): String = when (backendSetStyle(value.orEmpty())) {
        "warmup" -> "Warmup"
        "topSetBackoff" -> "TopSetBackoff"
        "ascendingPyramid" -> "AscendingPyramid"
        "dropSet" -> "DropSet"
        "restPause" -> "RestPause"
        "intervals" -> "Intervals"
        "tempo" -> "Tempo"
        else -> "Straight"
    }

    private fun backendWorkoutFocus(value: String): String {
        if (value in setOf("fullBody", "upperBody", "lowerBody", "push", "pull", "legs", "core", "cardio", "mobility", "recovery")) return value
        val normalized = value.lowercase()
        return when {
            normalized.contains("empuj") || normalized.contains("pecho") || normalized.contains("hombro") || normalized.contains("trícep") || normalized.contains("tricep") -> "push"
            normalized.contains("tirón") || normalized.contains("tiron") || normalized.contains("espalda") || normalized.contains("bícep") || normalized.contains("bicep") -> "pull"
            normalized.contains("pierna") || normalized.contains("glúte") || normalized.contains("glute") -> "legs"
            normalized.contains("core") || normalized.contains("abdom") -> "core"
            normalized.contains("cardio") || normalized.contains("resistencia") -> "cardio"
            normalized.contains("movilidad") || normalized.contains("mobility") -> "mobility"
            normalized.contains("recuper") -> "recovery"
            normalized.contains("superior") || normalized.contains("upper") -> "upperBody"
            normalized.contains("inferior") || normalized.contains("lower") -> "lowerBody"
            else -> "fullBody"
        }
    }

    private fun synchronizeWorkoutPlan(token: String, userId: String, profile: OnboardingProfile, now: String, context: Context, preferences: SharedPreferences, root: JSONObject) {
        val planId = preferences.getString(planKey, null) ?: stableId("$userId:${root.optString("planName")}").also { preferences.edit().putString(planKey, it).apply() }
        push(token, "workout-plans", JSONArray().put(record(planId, now)
            .put("name", root.optString("planName", "Plan Android")).put("goal", profile.goal.storedValue).put("status", "active")
            .put("phase", root.optString("phase")).put("mesocycle_number", root.optInt("mesocycleNumber", 1))
            .put("phase_week", root.optInt("phaseWeek", 1)).put("cycle_length", root.optInt("cycleLength", 1))
            .put("body_composition_phase", profile.bodyCompositionPhase?.storedValue ?: JSONObject.NULL)))
        val days = root.optJSONArray("workouts") ?: JSONArray(); val dayRecords = JSONArray(); val blocks = JSONArray(); val exercises = JSONArray(); val plannedExerciseIds = mutableMapOf<String, String>()
        val skippedExercises = skippedExerciseTimestamps(context)
        val uploadedSkippedExerciseKeys = mutableSetOf<String>()
        for (dayIndex in 0 until days.length()) {
            val day = days.getJSONObject(dayIndex); val dayId = day.optString("remoteId").takeIf(::isUuid) ?: stableId("$planId:${day.optString("id", dayIndex.toString())}")
            val localWorkoutId = day.optString("id", dayIndex.toString())
            var executionIndex = 0
            dayRecords.put(record(dayId, now).put("workout_plan_id", planId).put("kind", "workout").put("title", day.optString("title", "Sesión ${dayIndex + 1}")).put("focus", backendWorkoutFocus(day.optString("focus"))).put("status", day.optString("status", "PLANNED").lowercase()).put("did_count_toward_streak", false).put("order_index", dayIndex).put("intended_weekday", day.optString("weekday").lowercase()).put("day_type", day.optString("dayType")).put("creation_source", "generated").put("estimated_duration_minutes", day.optInt("estimatedDurationMinutes", 50)))
            val dayBlocks = day.optJSONArray("blocks") ?: JSONArray()
            // The backend now requires every planned exercise to belong to a
            // block. Older Android plans can still contain direct day-level
            // exercises, so give those a deterministic standard block instead
            // of silently dropping them (or submitting a null foreign key).
            val blocksToSync = JSONArray().apply {
                for (blockIndex in 0 until dayBlocks.length()) put(dayBlocks.getJSONObject(blockIndex))
                day.optJSONArray("exercises")?.takeIf { it.length() > 0 }?.let { directExercises ->
                    put(JSONObject().put("id", "direct-exercises").put("type", "standard").put("rounds", 1).put("exercises", directExercises))
                }
            }
            for (blockIndex in 0 until blocksToSync.length()) {
                val block = blocksToSync.getJSONObject(blockIndex); val blockId = block.optString("remoteId").takeIf(::isUuid) ?: stableId("$dayId:${block.optString("id", blockIndex.toString())}")
                blocks.put(record(blockId, now).put("workout_day_id", dayId).put("type", block.optString("type", "standard")).put("order_index", blockIndex).put("rounds", block.optInt("rounds", 1)).put("rest_after_block_seconds", block.opt("restAfterBlockSeconds")).put("notes", block.opt("notes")))
                val entries = block.optJSONArray("exercises") ?: JSONArray()
                for (exerciseIndex in 0 until entries.length()) {
                    val item = entries.getJSONObject(exerciseIndex); val exerciseId = item.optString("remoteId").takeIf(::isUuid) ?: stableId("$blockId:$exerciseIndex")
                    val historyKey = item.optString("imageKey").trim().ifBlank { item.optString("name").trim() }.lowercase()
                    if (historyKey.isNotBlank()) plannedExerciseIds.putIfAbsent(historyKey, exerciseId)
                    // The API validates the Exercise enum. `name` is localized
                    // display text; `imageKey` is Android's canonical catalog
                    // identifier shared with iOS and the backend.
                    val exerciseCatalogId = item.optString("imageKey").trim()
                    require(exerciseCatalogId.isNotBlank()) { "El ejercicio ${item.optString("name", exerciseIndex.toString())} no tiene identificador de catálogo." }
                    val skippedKey = "$localWorkoutId|$executionIndex"
                    val record = record(exerciseId, now).put("workout_day_id", dayId).put("workout_block_id", blockId).put("exercise", exerciseCatalogId).put("order_index", exerciseIndex).put("sets", item.optInt("sets", 1)).put("reps_min", item.opt("repsMin")).put("reps_max", item.opt("repsMax")).put("target_reps", item.opt("targetReps")).put("target_weight_kg", item.opt("targetWeightKg")).put("target_weights_kg", item.opt("targetWeightsKg")).put("target_duration_minutes", item.opt("targetDurationMinutes")).put("target_duration_seconds", item.opt("targetDurationSeconds")).put("target_distance_km", item.opt("targetDistanceKm")).put("rest_seconds", item.optInt("restSeconds", 0)).put("set_style_configuration", backendSetStyleConfiguration(item))
                    skippedExercises[skippedKey]?.let { timestamp ->
                        record.put("skipped_at", java.time.Instant.ofEpochMilli(timestamp).toString())
                        uploadedSkippedExerciseKeys += skippedKey
                    }
                    exercises.put(record)
                    executionIndex += 1
                }
            }
        }
        push(token, "workout-days", dayRecords); push(token, "workout-blocks", blocks); push(token, "planned-exercises", exercises)
        if (uploadedSkippedExerciseKeys.isNotEmpty()) clearSkippedExercises(context, uploadedSkippedExerciseKeys)
        val results = JSONArray()
        WorkoutHistoryStore.remoteEntries(context = context).forEach { entry ->
            val plannedExerciseId = plannedExerciseIds[entry.exerciseKey] ?: return@forEach
            results.put(record(stableId("$plannedExerciseId:${entry.timestampMillis}"), now)
                .put("planned_exercise_id", plannedExerciseId).put("feedback", entry.feedback ?: "justRight")
                .put("completed_at", java.time.Instant.ofEpochMilli(entry.timestampMillis).toString())
                .put("completed_sets", entry.sets).put("completed_reps", entry.totalReps).put("completed_weight", entry.maxWeightKg.finiteOrZero())
                .put("per_set_reps", JSONArray(entry.setReps)).put("per_set_weights_kg", JSONArray(entry.setWeightsKg.map { it.finiteOrZero() }))
                .put("completed_duration_seconds", entry.durationSeconds).put("completed_duration_minutes", entry.durationSeconds / 60)
                .put("completed_distance_km", entry.distanceKm.finiteOrZero()).put("notes", entry.note ?: JSONObject.NULL))
        }
        push(token, "exercise-results", results)
    }

    private fun record(id: String, now: String) = JSONObject().put("id", id).put("created_at", now).put("updated_at", now)

    private fun Double.finiteOrZero(): Double = if (isFinite()) this else 0.0

    private fun skippedExerciseTimestamps(context: Context): Map<String, Long> =
        context.getSharedPreferences(skippedExercisesPrefs, Context.MODE_PRIVATE)
            .getStringSet(skippedExercisesKey, emptySet()).orEmpty().mapNotNull { entry ->
                val separator = entry.lastIndexOf('|')
                val timestamp = entry.substring(separator + 1).toLongOrNull() ?: return@mapNotNull null
                entry.substring(0, separator).takeIf { it.count { character -> character == '|' } == 1 }?.let { it to timestamp }
            }.toMap()

    private fun clearSkippedExercises(context: Context, keys: Set<String>) {
        val preferences = context.getSharedPreferences(skippedExercisesPrefs, Context.MODE_PRIVATE)
        val remaining = preferences.getStringSet(skippedExercisesKey, emptySet()).orEmpty().filterNot { entry ->
            entry.substringBeforeLast('|') in keys
        }.toSet()
        preferences.edit().putStringSet(skippedExercisesKey, remaining).apply()
    }
    private fun stableId(value: String) = UUID.nameUUIDFromBytes(value.toByteArray()).toString()
    /** The shared API accepts at most 100 generic records per request, like iOS. */
    private fun push(token: String, resource: String, records: JSONArray) {
        if (records.length() == 0) return
        for (start in 0 until records.length() step 100) {
            val batch = JSONArray()
            for (index in start until minOf(start + 100, records.length())) batch.put(records.get(index))
            pushBatch(token, resource, batch)
        }
    }

    private fun pushBatch(token: String, resource: String, records: JSONArray) {
        val connection = (URL(WildforceApiEnvironment.apiUrl("sync/push")).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"; doOutput = true; connectTimeout = 15_000; readTimeout = 15_000
            setRequestProperty("Accept", "application/json"); setRequestProperty("Content-Type", "application/json"); setRequestProperty("Authorization", "Bearer $token")
        }
        OutputStreamWriter(connection.outputStream).use { it.write(JSONObject().put("resource", resource).put("records", records).toString()) }
        val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
        val response = BufferedReader(stream.reader()).use { it.readText() }
        if (connection.responseCode !in 200..299) {
            val error = apiException(response, "No se pudo sincronizar $resource")
            Log.w("WorkoutRemoteSync", "Sync resource $resource rejected with HTTP ${connection.responseCode}: ${error.message}")
            throw error
        }
    }

    private fun get(token: String, url: String): JSONObject {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"; connectTimeout = 15_000; readTimeout = 15_000
            setRequestProperty("Accept", "application/json"); setRequestProperty("Authorization", "Bearer $token")
        }
        val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
        val response = BufferedReader(stream.reader()).use { it.readText() }
        if (connection.responseCode !in 200..299) throw apiException(response, "No se pudo descargar la cuenta")
        return JSONObject(response)
    }

    /**
     * Mirrors iOS's paginated `/sync/pull/batch` transport.  Android keeps a
     * compact local projection rather than a row store, so it deliberately
     * requests a complete snapshot; following the opaque cursor is essential
     * because a single page is capped by the backend.
     */
    private fun pullRemoteSnapshot(token: String): RemoteSnapshot {
        val result = linkedMapOf<String, JSONArray>()
        var request = JSONObject().put("resources", JSONArray().apply {
            batchResources.forEach { resource -> put(JSONObject().put("resource", resource).put("updated_after", JSONObject.NULL)) }
        })
        do {
            val response = postJson(token, WildforceApiEnvironment.apiUrl("sync/pull/batch"), request)
            val data = response.optJSONArray("data") ?: JSONArray()
            for (index in 0 until data.length()) {
                val item = data.optJSONObject(index) ?: continue
                val resource = item.optString("resource")
                val record = item.optJSONObject("record") ?: continue
                if (resource in batchResources) result.getOrPut(resource, ::JSONArray).put(record)
            }
            val cursor = response.optJSONObject("meta")?.optString("next_cursor").orEmpty()
            request = if (cursor.isBlank()) JSONObject() else JSONObject().put("cursor", cursor)
        } while (request.has("cursor"))
        return RemoteSnapshot(result)
    }

    private fun postJson(token: String, url: String, body: JSONObject): JSONObject {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"; doOutput = true; connectTimeout = 20_000; readTimeout = 30_000
            setRequestProperty("Accept", "application/json"); setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer $token")
        }
        OutputStreamWriter(connection.outputStream).use { it.write(body.toString()) }
        val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
        val response = BufferedReader(stream.reader()).use { it.readText() }
        if (connection.responseCode !in 200..299) throw apiException(response, "No se pudo descargar la cuenta")
        return JSONObject(response)
    }

    private data class RemoteSnapshot(private val data: Map<String, JSONArray>) {
        fun records(resource: String): JSONArray = data[resource] ?: JSONArray()
    }

    private fun uploadImage(token: String, url: String, file: File, mimeType: String) {
        uploadImage(token, url, file.name, file.inputStream().use { it.readBytes() }, mimeType)
    }

    private fun uploadImage(token: String, url: String, filename: String, bytes: ByteArray, mimeType: String) {
        val boundary = "----Wildforce${UUID.randomUUID()}"
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"; doOutput = true; connectTimeout = 20_000; readTimeout = 30_000
            setRequestProperty("Accept", "application/json"); setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        }
        DataOutputStream(connection.outputStream).use { output ->
            output.writeBytes("--$boundary\r\n")
            output.writeBytes("Content-Disposition: form-data; name=\"image\"; filename=\"$filename\"\r\n")
            output.writeBytes("Content-Type: $mimeType\r\n\r\n")
            output.write(bytes)
            output.writeBytes("\r\n--$boundary--\r\n")
        }
        val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
        val response = BufferedReader(stream.reader()).use { it.readText() }
        if (connection.responseCode !in 200..299) throw apiException(response, "No se pudo subir la foto de progreso")
    }

    private fun download(token: String, url: String, destination: File): Boolean = runCatching {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"; connectTimeout = 20_000; readTimeout = 30_000
            setRequestProperty("Authorization", "Bearer $token")
        }
        if (connection.responseCode !in 200..299) return@runCatching false
        destination.parentFile?.mkdirs()
        connection.inputStream.use { input -> destination.outputStream().use(input::copyTo) }
        true
    }.getOrDefault(false)

    private fun apiException(response: String, fallback: String): Exception {
        val error = runCatching { JSONObject(response) }.getOrNull()
        return if (error?.optString("code") == "subscription_required") SubscriptionRequiredException
        else IllegalStateException(error?.optString("message", fallback) ?: fallback)
    }

    private data object SubscriptionRequiredException : Exception()

    private fun isUuid(value: String): Boolean = runCatching { UUID.fromString(value) }.isSuccess

    private fun stableRemoteNutritionLogId(value: String): Long =
        value.hashCode().toLong().and(Long.MAX_VALUE).takeIf { it != 0L } ?: 1L

    private data class AccountProgress(
        val currentStreak: Int,
        val longestStreak: Int,
        val lastCompletedWorkoutAt: String?,
        val xp: Int,
        val xpLevel: Int,
    )

    private data class RemotePlan(val id: String, val status: String, val rawJson: String)

    private fun JSONArray?.liveRecords(): List<JSONObject> = buildList {
        this@liveRecords ?: return@buildList
        for (index in 0 until this@liveRecords.length()) {
            // The flat sync API omits `deleted_at` for live rows and only
            // supplies it for tombstones. `JSONObject.isNull` is true both
            // for an absent value and for JSON null, which is exactly the
            // representation that must remain visible locally.
            this@liveRecords.optJSONObject(index)
                ?.takeIf { it.isNull("deleted_at") || it.optString("deleted_at").isBlank() }
                ?.let(::add)
        }
    }

    private fun JSONArray?.firstLiveRecord(): JSONObject? = liveRecords().firstOrNull()

    private fun JSONArray?.intValues(): List<Int> = buildList {
        this@intValues ?: return@buildList
        for (index in 0 until this@intValues.length()) add(this@intValues.optInt(index))
    }

    private fun JSONArray?.doubleValues(): List<Double> = buildList {
        this@doubleValues ?: return@buildList
        for (index in 0 until this@doubleValues.length()) add(this@doubleValues.optDouble(index))
    }

    private fun <T> JSONArray?.storedValues(values: Iterable<T>, storedValue: (T) -> String): Set<T> = buildSet {
        this@storedValues ?: return@buildSet
        for (index in 0 until this@storedValues.length()) {
            val raw = this@storedValues.optString(index)
            values.firstOrNull { storedValue(it) == raw }?.let(::add)
        }
    }
}
