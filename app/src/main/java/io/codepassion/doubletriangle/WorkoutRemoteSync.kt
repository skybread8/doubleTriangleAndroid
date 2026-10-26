package io.codepassion.doubletriangle

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.annotation.SuppressLint
import android.util.Log
import io.codepassion.doubletriangle.feature.onboarding.OnboardingProfile
import io.codepassion.doubletriangle.feature.onboarding.BodyCompositionPhase
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
import io.codepassion.doubletriangle.core.model.RemoteSyncChangeTracker
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
    private const val nutritionDayIDsKey = "nutrition_day_ids"
    private const val nutritionMealIDsKey = "nutrition_meal_ids"
    private const val uploadedNutritionMediaKey = "nutrition_uploaded_media"
    private const val remoteNutritionMediaKey = "nutrition_remote_media"
    private const val accountProgressPrefs = "wildforce_account_progress"
    private const val syncStatePrefs = "wildforce_sync_state"
    private const val pullWatermarkPrefix = "pull_watermark:"
    private const val pushWatermarkPrefix = "push_watermark:"
    private const val pushedRecordsPrefix = "pushed_records:"
    private const val exerciseProfilesKey = "exercise_profiles"
    private const val skippedExercisesPrefs = "wildforce_skipped_exercises"
    private const val skippedExercisesKey = "entries"
    private const val coachIDsKey = "coach_ids"
    /** Same ordered resource set used by iOS's WildforceSync. */
    private val batchResources = listOf(
        "users", "user-app-settings", "subscriptions", "training-preferences", "training-locations",
        "body-metric-entries", "body-progress-photo-sessions", "exercise-profiles", "nutrition-profiles",
        "workout-plans", "workout-days", "workout-blocks", "planned-exercises", "exercise-results",
        "coach-exercise-content", "coach-exercises", "nutrition-plans", "nutrition-days", "nutrition-meals",
        // iOS intentionally keeps nutrition-log-media out of pull batches:
        // paths are device-local and the private image endpoint is the source
        // of the binary bytes.
        "nutrition-log-entries", "nutrition-log-items",
    )
    private val pushableResources = setOf(
        "users", "user-app-settings", "training-preferences", "training-locations",
        "body-metric-entries", "body-progress-photo-sessions", "nutrition-profiles",
        "workout-plans", "workout-days", "workout-blocks", "planned-exercises", "exercise-results",
        "nutrition-plans", "nutrition-days", "nutrition-meals", "nutrition-log-media", "nutrition-log-entries", "nutrition-log-items",
    )
    private val nutritionPlanResources = setOf("nutrition-plans", "nutrition-days", "nutrition-meals")
    private val nutritionLogResources = setOf("nutrition-log-media", "nutrition-log-entries", "nutrition-log-items")
    private val synchronizationMutex = Mutex()
    // `synchronize` is serialized by synchronizationMutex, so this scoped
    // selection safely gives the generic push helpers iOS-style per-resource
    // change filtering without a second global pending flag.
    private var activePushResources: Set<String> = emptySet()
    // This reference is always the application context and is cleared when
    // the serialized push scope finishes; it cannot retain an Activity.
    @SuppressLint("StaticFieldLeak")
    private var activePushContext: Context? = null

    /** Marks only the resources changed by an Android interaction. */
    fun markResourcesDirty(context: Context, vararg resources: String) {
        RemoteSyncChangeTracker.mark(context, *resources)
    }

    fun markNewAccountResourcesDirty(context: Context) {
        RemoteSyncChangeTracker.mark(context, *pushableResources.toTypedArray())
    }

    private fun dirtyResources(context: Context, preferences: SharedPreferences, importingExistingAccount: Boolean): Set<String> {
        if (importingExistingAccount) return emptySet()
        val explicit = RemoteSyncChangeTracker.dirty(context)
        // Existing builds only have the legacy boolean. Sync it once using the
        // complete ordered graph, then subsequent edits use precise resources.
        val selected = if (explicit.isNotEmpty()) explicit
        else if (preferences.getBoolean("remote_sync_pending", false)) pushableResources
        else emptySet()
        // A nutrition entry can refer to its media row. Preserve the iOS
        // parent-before-child ordering even for data created by Android builds
        // that predate the explicit media dirty marker.
        return if ("nutrition-log-entries" in selected || "nutrition-log-items" in selected) {
            selected + "nutrition-log-media"
        } else selected
    }

    private fun clearDirtyResource(resource: String) {
        val context = activePushContext ?: return
        RemoteSyncChangeTracker.clear(context, resource)
        context.getSharedPreferences(syncStatePrefs, Context.MODE_PRIVATE).edit()
            .putString("$pushWatermarkPrefix$resource", java.time.Instant.now().toString()).apply()
    }

    /** The server session ID is deterministic, so the forecast API can be used after a sync. */
    fun remoteBodyProgressSessionId(context: Context, localSessionId: String): String {
        val userId = context.getSharedPreferences(accountPrefs, Context.MODE_PRIVATE).getString("id", null)
            ?: error("Inicia sesión para crear una previsión")
        return bodyProgressRemoteID(context, userId, localSessionId)
    }

    sealed interface Outcome {
        /** A first-device import can replace the in-memory profile before the UI is shown. */
        data class Synchronized(val importedProfile: OnboardingProfile? = null) : Outcome
        data object NoAccount : Outcome
        data object SubscriptionRequired : Outcome
        /** A pull can succeed before a later push fails; iOS still renders the imported data. */
        data class Failed(
            val message: String?,
            val importedRemotePlan: Boolean = false,
            val importedProfile: OnboardingProfile? = null,
        ) : Outcome
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
        var importedRemotePlan = false
        var importedProfile: OnboardingProfile? = null
        try {
            val hasPendingLocalChanges = preferences.getBoolean("remote_sync_pending", false)
            // An existing account signing in on a new Android device must import
            // the server graph before it can create any local sync records. iOS's
            // store has no generated fallback graph to upload in this case.
            val importingExistingAccount = preferences.getBoolean("remote_sync_import_required", false)
            val shouldImportRemoteSnapshot = importingExistingAccount || preferences.getString("workout_plan_json", null).isNullOrBlank()
            // Match iOS: every synchronization pulls first and then pushes local
            // changes. Keep the local projection in memory while pulling so a
            // pending Android edit is not replaced before it is uploaded.
            val pendingPlanJson = preferences.getString("workout_plan_json", null)
            val pendingPlanId = preferences.getString(planKey, null)
            val shouldPullRemote = true
            val resourcesToPush = dirtyResources(context, preferences, importingExistingAccount)
            val shouldPushLocal = resourcesToPush.isNotEmpty()
            importedProfile = if (shouldPullRemote) {
                resetCoachContentIfCoachesChanged(context, token)
                // iOS applies each changed row into SwiftData before using the
                // graph. Materialize Android's incremental pages the same way;
                // treating a delta as a full snapshot was the root cause of
                // incomplete nutrition/profile imports after the first sync.
                val remote = pullRemoteSnapshot(context, token).materialize(context)
                val failedPullResources = mutableSetOf<String>()
                fun apply(vararg resources: String, block: () -> Unit) {
                    runCatching(block).onFailure {
                        failedPullResources += resources
                        Log.w("WorkoutRemoteSync", "Unable to import ${resources.joinToString()}", it)
                    }
                }
                var remoteProfile: OnboardingProfile? = null
                // Custom coach exercises resolve the nullable exercise reference
                // in the workout graph, so load them before materializing plans.
                apply("coach-exercise-content") { CoachExerciseMediaStore.replace(context, remote.records("coach-exercise-content")) }
                apply("coach-exercises") { CoachExerciseMediaStore.replaceCoachExercises(context, remote.records("coach-exercises")) }
                apply("users", "training-preferences", "training-locations") {
                    remoteProfile = pullRemoteProfile(context, remote, profile)
                }
                apply("users") { pullRemoteAccountProgress(context, remote) }
                apply("exercise-profiles") { pullRemoteExerciseProfiles(context, remote) }
                apply("workout-plans", "workout-days", "workout-blocks", "planned-exercises", "exercise-results") {
                    importedRemotePlan = pullWorkoutPlans(context, remote, preferences)
                }
                apply("nutrition-profiles", "nutrition-plans", "nutrition-days", "nutrition-meals", "nutrition-log-entries", "nutrition-log-items") {
                    pullRemoteNutrition(context, token, remote, resourcesToPush)
                }
                apply("body-metric-entries") {
                    pullRemoteBodyMetrics(context, remote.records("body-metric-entries"), (remoteProfile ?: profile).heightCm)
                }
                var importedHealthConnectEnabled: Boolean? = null
                apply("user-app-settings") {
                    importedHealthConnectEnabled = pullRemoteAppSettings(context, remote)
                }
                remoteProfile = remoteProfile?.let { imported ->
                    importedHealthConnectEnabled?.let { imported.copy(isHealthConnectEnabled = it) } ?: imported
                }
                // iOS pulls media on every synchronization. The local store
                // de-duplicates sessions, so this also restores progress photos
                // added from another device after the first Android sign-in.
                // A local capture/deletion has the same precedence as an iOS
                // SwiftData dirty object. Do not rehydrate the previous remote
                // session over it before the outbound tombstone/upload.
                if ("body-progress-photo-sessions" !in resourcesToPush) {
                    pullRemoteMedia(context, token, remote.records("body-progress-photo-sessions"))
                }
                // Avatars are stored outside the sync graph on both platforms.
                // iOS resolves the public avatar path when rendering Profile;
                // hydrate it before Android renders the imported profile too.
                pullRemoteAvatar(context, token, userId)
                // iOS advances each pull watermark only after every record in
                // this page set has been applied locally. Do that before the
                // outbound pass: a later push failure must not re-fetch and
                // re-append an already-imported remote graph.
                remote.savePullWatermarks(context, failedPullResources)
                if (shouldImportRemoteSnapshot) {
                    preferences.edit().remove("remote_sync_import_required").apply()
                }
                if (hasPendingLocalChanges && !importingExistingAccount && pendingPlanJson != null) {
                    preferences.edit().putString("workout_plan_json", pendingPlanJson)
                        .apply { pendingPlanId?.let { putString(planKey, it) } }
                        .apply()
                }
                remoteProfile
            } else null
            val profileToSync = if (shouldPushLocal) profile else importedProfile ?: profile
            if (shouldPushLocal) {
            activePushResources = resourcesToPush
            activePushContext = context.applicationContext
            val now = java.time.Instant.now().toString()
            val progress = accountProgress(context)
            // User is a first-class iOS sync resource. Keeping it here ensures
            // profile edits made on Android are visible to the other platform.
            push(token, "users", JSONArray().put(recordFromRemoteProjection(context, "users", userId, now)
                .put("name", profileToSync.name).put("height", profileToSync.heightCm).put("weight", profileToSync.weightKg)
                .put("birth_date", "%04d-%02d-%02d".format(profileToSync.birthYear, profileToSync.birthMonth, profileToSync.birthDay.coerceIn(1, java.time.YearMonth.of(profileToSync.birthYear.coerceIn(1920, 9999), profileToSync.birthMonth.coerceIn(1, 12)).lengthOfMonth())))
                .put("gender", profileToSync.gender.storedValue).put("language", AppLocale.languageTag(profileToSync.appLanguage))
                .put("metric_system", profileToSync.metricSystem.storedValue)
                .put("current_streak", progress.currentStreak).put("longest_streak", progress.longestStreak)
                .put("last_completed_workout_at", progress.lastCompletedWorkoutAt ?: JSONObject.NULL)
                .put("xp", progress.xp).put("xp_level", progress.xpLevel)))
            val trainingPreferencesId = remoteProjectionID(context, "training-preferences", stableId("$userId:training-preferences"))
            push(token, "training-preferences", JSONArray().put(recordFromRemoteProjection(context, "training-preferences", trainingPreferencesId, now)
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
                profileToSync.effectiveTrainingLocations().forEachIndexed { index, location -> {
                    val id = remoteLocationIds.optString(index).takeIf(::isUuid) ?: stableId("$userId:location:$index")
                    put(recordFromRemoteProjection(context, "training-locations", id, now)
                    .put("name", location.name).put("is_default", location.isDefault).put("sort_order", index)
                    .put("equipment", JSONArray(location.equipment.map { it.storedValue }))
                    .put("display_address", location.displayAddress ?: JSONObject.NULL)
                    .put("latitude", location.latitude ?: JSONObject.NULL)
                    .put("longitude", location.longitude ?: JSONObject.NULL)
                    .put("photo_path", location.photoPath ?: JSONObject.NULL))
                } }
            })
            val detailPrefs = context.getSharedPreferences("wildforce_profile_details", Context.MODE_PRIVATE)
            val appSettingsId = remoteProjectionID(context, "user-app-settings", stableId("$userId:settings"))
            push(token, "user-app-settings", JSONArray().put(recordFromRemoteProjection(context, "user-app-settings", appSettingsId, now)
                .put("is_health_kit_enabled", profileToSync.isHealthConnectEnabled)
                // iOS exposes one shared watch-auto-tracking setting. Android has
                // separate Amazfit and Wear OS switches, so publish the shared
                // value when either companion integration is enabled.
                .put("is_watch_auto_tracking_enabled", detailPrefs.getBoolean("amazfit_watch_enabled", false) || detailPrefs.getBoolean("wear_auto_tracking_enabled", false))
                .put("is_screen_on_during_workout_enabled", detailPrefs.getBoolean("keep_screen_on", true))
                .put("is_notifications_enabled", context.getSharedPreferences("wildforce_notification_settings", Context.MODE_PRIVATE).getBoolean("enabled", true))
                .put("is_full_focus_mode_enabled", detailPrefs.getBoolean("full_focus", false))
                .put("has_seen_notification_request", detailPrefs.getBoolean("has_seen_notification_request", false))
                .put("has_seen_body_progress_tutorial", detailPrefs.getBoolean("has_seen_body_progress_tutorial", false))
                .put("has_rated_app", detailPrefs.getBoolean("has_rated_app", false))))
            val bodyMetrics = context.getSharedPreferences("wildforce_body_metrics", Context.MODE_PRIVATE).getString("history", "[]") ?: "[]"
            val metricRecords = JSONArray(); val metrics = JSONArray(bodyMetrics)
            fun addMetric(date: String, type: String, value: Double, range: ClosedFloatingPointRange<Double>, source: String) {
                if (date.isBlank() || !value.isFinite() || value !in range) return
                metricRecords.put(record(stableId("$userId:$type:$date"), now)
                    .put("type", type).put("value", value).put("recorded_at", "${date}T12:00:00Z").put("source", source))
            }
            for (index in 0 until metrics.length()) {
                val metric = metrics.optJSONObject(index) ?: continue
                val date = metric.optString("date")
                val source = metric.optString("source", "manual")
                addMetric(date, "weight", metric.optDouble("weightKg", Double.NaN), 20.0..400.0, source)
                addMetric(date, "leanBodyMass", metric.optionalDouble("leanBodyMassKg"), 1.0..300.0, source)
                addMetric(date, "bodyFatPercentage", metric.optionalDouble("bodyFatPercentage"), 1.0..90.0, source)
                addMetric(date, "waistCircumference", metric.optionalDouble("waistCm"), 20.0..300.0, source)
                addMetric(date, "neckCircumference", metric.optionalDouble("neckCm"), 20.0..300.0, source)
                addMetric(date, "hipCircumference", metric.optionalDouble("hipCm"), 20.0..300.0, source)
            }
            push(token, "body-metric-entries", metricRecords)
            synchronizeAvatar(context, token)
            synchronizeBodyProgressPhotos(context, token, userId, now)

            synchronizeNutrition(token, userId, profileToSync, now, context)
            preferences.getString("workout_plan_json", null)?.let { rawPlan ->
                synchronizeWorkoutPlan(token, userId, profileToSync, now, context, preferences, JSONObject(rawPlan))
            }
            activePushResources = emptySet()
            activePushContext = null
            }
            Outcome.Synchronized(importedProfile)
        } catch (error: Exception) {
            activePushResources = emptySet()
            activePushContext = null
            Log.w("WorkoutRemoteSync", "Synchronization failed", error)
            if (error is SubscriptionRequiredException) Outcome.SubscriptionRequired
            else Outcome.Failed(error.message, importedRemotePlan, importedProfile)
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
                birthDay = user.optString("birth_date").drop(8).take(2).toIntOrNull()?.coerceIn(1, 31) ?: local.birthDay,
                gender = io.codepassion.doubletriangle.feature.onboarding.Gender.fromStoredValue(
                    user.optString("gender", local.gender.storedValue),
                ),
                metricSystem = io.codepassion.doubletriangle.feature.onboarding.MetricSystem.fromStoredValue(
                    user.optString("metric_system", local.metricSystem.storedValue),
                ),
                appLanguage = user.optString("language", local.appLanguage).ifBlank { local.appLanguage },
            )
        } ?: local
        val preferences = remote.records("training-preferences").firstLiveRecord() ?: return remoteUser?.let { userProfile }
        val remoteLocations = remote.records("training-locations").liveRecords()
            .sortedBy { it.optInt("sort_order", Int.MAX_VALUE) }
        val locations = remoteLocations.mapIndexed { index, location ->
                TrainingLocationProfile(
                    name = location.optString("name").trim().ifBlank { "Ubicación ${index + 1}" },
                    equipment = location.optJSONArray("equipment").storedValues(Equipment.entries) { it.storedValue }
                        .ifEmpty { userProfile.availableEquipment },
                    isDefault = location.optBoolean("is_default", index == 0),
                    displayAddress = location.optString("display_address").takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) },
                    latitude = location.optDouble("latitude").takeUnless { it.isNaN() }?.takeIf { it in -90.0..90.0 },
                    longitude = location.optDouble("longitude").takeUnless { it.isNaN() }?.takeIf { it in -180.0..180.0 },
                    photoPath = location.optString("photo_path").takeIf(String::isNotBlank),
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
            bodyCompositionPhase = if (preferences.isNull("body_composition_phase")) null else
                BodyCompositionPhase.fromStoredValue(preferences.optString("body_composition_phase")),
            trainingLocations = locations.ifEmpty { userProfile.trainingLocations },
            availableEquipment = locations.firstOrNull { it.isDefault }?.equipment ?: userProfile.availableEquipment,
            gymType = GymType.fromStoredValue(preferences.optString("gym_type", userProfile.gymType.storedValue)),
        )
    }

    /** Stores the account-wide progression fields iOS keeps on User. */
    private fun pullRemoteAccountProgress(context: Context, remote: RemoteSnapshot) {
        val user = remote.records("users").firstLiveRecord() ?: return
        context.getSharedPreferences(accountProgressPrefs, Context.MODE_PRIVATE).edit()
            .putInt("current_streak", user.optInt("current_streak", 0).coerceAtLeast(0))
            .putInt("longest_streak", user.optInt("longest_streak", 0).coerceAtLeast(0))
            // JSONObject#optString turns a JSON null into the literal text "null".
            // iOS keeps this optional field as nil and sends JSON null back to the
            // API; retain the same representation instead of later submitting an
            // invalid date string that makes the server return 500.
            .putString("last_completed_workout_at", user.optString("last_completed_workout_at")
                .takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) })
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

    /**
     * The batch endpoint only returns content for currently active coaches. A
     * newly assigned coach can have content older than the existing pull
     * watermark, so mirror iOS by clearing that one projection and restarting
     * just its resource cursor whenever the coach set changes.
     */
    private fun resetCoachContentIfCoachesChanged(context: Context, token: String) {
        val response = runCatching {
            get(token, WildforceApiEnvironment.apiUrl("account/coaches"))
        }.getOrElse { return }
        val ids = response.optJSONArray("data")?.let { coaches ->
            buildList {
                for (index in 0 until coaches.length()) {
                    coaches.optJSONObject(index)?.optString("id")
                        ?.takeIf(String::isNotBlank)?.let(::add)
                }
            }.sorted()
        }.orEmpty()
        val preferences = context.getSharedPreferences(syncPrefs, Context.MODE_PRIVATE)
        val previous = preferences.getStringSet(coachIDsKey, null)?.sorted()
        if (previous == ids) return

        CoachExerciseMediaStore.replace(context, JSONArray())
        context.getSharedPreferences(syncStatePrefs, Context.MODE_PRIVATE).edit()
            .remove("${pullWatermarkPrefix}coach-exercise-content")
            // Keep custom exercises for past workouts, but force a full pull so
            // a newly assigned coach's older library is available immediately.
            .remove("${pullWatermarkPrefix}coach-exercises").apply()
        preferences.edit().putStringSet(coachIDsKey, ids.toSet()).apply()
    }

    private fun accountProgress(context: Context): AccountProgress {
        val preferences = context.getSharedPreferences(accountProgressPrefs, Context.MODE_PRIVATE)
        return AccountProgress(
            currentStreak = preferences.getInt("current_streak", 0).coerceAtLeast(0),
            longestStreak = preferences.getInt("longest_streak", 0).coerceAtLeast(0),
            lastCompletedWorkoutAt = preferences.getString("last_completed_workout_at", null)
                ?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) },
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
    private fun pullRemoteNutrition(
        context: Context,
        token: String,
        remote: RemoteSnapshot,
        pendingLocalResources: Set<String>,
    ) {
        // `/sync/pull` is a flat-resource API. Hydrate the plan graph before
        // passing it to the store, which otherwise expects the legacy nested
        // `days -> meals` response and silently imports nothing.
        val plans = remote.records("nutrition-plans").liveRecords()
        val days = remote.records("nutrition-days").liveRecords()
        val meals = remote.records("nutrition-meals").liveRecords()
        val mealsByDay = meals.groupBy { it.optString("nutrition_day_id") }
        plans.maxWithOrNull(compareBy<JSONObject> { it.optString("starts_on") }.thenBy { it.optString("updated_at") })?.let { plan ->
            val planId = plan.optString("id")
            val planDays = days.filter { it.optString("nutrition_plan_id") == planId }
            planDays.forEach { day ->
                day.put("meals", JSONArray(mealsByDay[day.optString("id")].orEmpty()))
            }
            rememberRemoteNutritionPlanIDs(context, planDays, mealsByDay)
            plan.put("days", JSONArray(planDays))
            if (pendingLocalResources.none { it in nutritionPlanResources }) {
                NutritionStore.importRemotePlan(context, plan)
            }
        }
        remote.records("nutrition-profiles").firstLiveRecord()?.let {
            if ("nutrition-profiles" !in pendingLocalResources) NutritionStore.importRemotePreferences(context, it)
        }
        val entries = remote.records("nutrition-log-entries")
        val items = remote.records("nutrition-log-items")
        if (pendingLocalResources.none { it in nutritionLogResources }) {
            NutritionStore.importRemoteLogs(context, entries, items, remoteNutritionMealKeys(days, mealsByDay))
        }
        rememberRemoteNutritionIDs(context, entries, items)
        if (pendingLocalResources.none { it in nutritionLogResources }) pullRemoteNutritionMedia(context, token, entries)
    }

    /** iOS keeps nutrition images private and fetches them separately from the graph snapshot. */
    private fun pullRemoteNutritionMedia(context: Context, token: String, entries: JSONArray) {
        val directory = File(context.filesDir, "NutritionLogMedia").apply { mkdirs() }
        entries.liveRecords().forEach { entry ->
            val entryId = entry.optString("id").takeIf(::isUuid) ?: return@forEach
            val image = File(directory, "$entryId.jpg")
            if (image.isFile && image.length() in 1..(5 * 1024 * 1024)) {
                NutritionStore.attachRemotePhoto(context, entryId, image.absolutePath)
                val state = idsForNutritionMedia(context)
                state.edit().putStringSet(
                    remoteNutritionMediaKey,
                    state.getStringSet(remoteNutritionMediaKey, emptySet()).orEmpty() + "$entryId:${image.absolutePath}"
                ).apply()
                return@forEach
            }
            val temporary = File(directory, "$entryId.download")
            val connection = (URL(WildforceApiEnvironment.apiUrl("nutrition/log-entries/$entryId/image")).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"; connectTimeout = 20_000; readTimeout = 60_000
                setRequestProperty("Authorization", "Bearer $token")
            }
            try {
                if (connection.responseCode !in 200..299) return@forEach
                connection.inputStream.use { input -> temporary.outputStream().use { output -> input.copyTo(output) } }
                if (temporary.length() in 1..(5 * 1024 * 1024)) {
                    if (image.exists()) image.delete()
                    if (temporary.renameTo(image)) {
                        NutritionStore.attachRemotePhoto(context, entryId, image.absolutePath)
                        val state = idsForNutritionMedia(context)
                        state.edit().putStringSet(
                            remoteNutritionMediaKey,
                            state.getStringSet(remoteNutritionMediaKey, emptySet()).orEmpty() + "$entryId:${image.absolutePath}"
                        ).apply()
                    }
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

    /** Preserve iOS UUIDs for imported nutrition children, avoiding duplicate graph rows on Android's next push. */
    private fun rememberRemoteNutritionPlanIDs(
        context: Context,
        days: List<JSONObject>,
        mealsByDay: Map<String, List<JSONObject>>,
    ) {
        val dayIDs = JSONObject()
        val mealIDs = JSONObject()
        days.forEach { day ->
            val date = day.optString("date").take(10)
            val dayId = day.optString("id")
            if (date.isBlank() || !isUuid(dayId)) return@forEach
            dayIDs.put(date, dayId)
            mealsByDay[dayId].orEmpty()
                .sortedBy { it.optInt("order_index", 0) }
                .forEach { meal ->
                    val mealId = meal.optString("id")
                    if (isUuid(mealId)) mealIDs.put(
                        nutritionMealKey(date, meal.optString("meal_type"), meal.optString("title")),
                        mealId,
                    )
                }
        }
        context.getSharedPreferences(syncPrefs, Context.MODE_PRIVATE).edit()
            .putString(nutritionDayIDsKey, dayIDs.toString())
            .putString(nutritionMealIDsKey, mealIDs.toString())
            .apply()
    }

    private fun remoteNutritionMealKeys(
        days: List<JSONObject>,
        mealsByDay: Map<String, List<JSONObject>>,
    ): Map<String, String> = buildMap {
        days.forEach { day ->
            val date = day.optString("date").take(10)
            if (date.isBlank()) return@forEach
            mealsByDay[day.optString("id")].orEmpty().forEach { meal ->
                val mealId = meal.optString("id")
                if (isUuid(mealId)) put(mealId, nutritionMealKey(date, meal.optString("meal_type"), meal.optString("title")))
            }
        }
    }

    private fun nutritionMealKey(date: String, mealType: String, title: String): String =
        "$date|${mealType.lowercase().replaceFirstChar(Char::uppercase)}|${title.trim().lowercase()}"

    /** Restores every body-metric type exposed by iOS, in canonical metric units. */
    private fun pullRemoteBodyMetrics(context: Context, remote: JSONArray, fallbackHeightCm: Int) {
        val metricPreferences = context.getSharedPreferences("wildforce_body_metrics", Context.MODE_PRIVATE)
        val stored = metricPreferences
            .getString("history", "[]")?.let(::JSONArray) ?: JSONArray()
        val byDate = linkedMapOf<String, JSONObject>()
        for (index in 0 until stored.length()) {
            val entry = stored.optJSONObject(index) ?: continue
            entry.optString("date").takeIf(String::isNotBlank)?.let { byDate[it] = JSONObject(entry.toString()) }
        }
        for (index in 0 until remote.length()) {
            val entry = remote.optJSONObject(index) ?: continue
            if (!entry.isNull("deleted_at")) continue
            val date = entry.optString("recorded_at").take(10)
            val value = entry.optDouble("value", Double.NaN)
            if (date.isBlank() || !value.isFinite()) continue
            val target = byDate.getOrPut(date) {
                JSONObject().put("date", date).put("heightCm", fallbackHeightCm.toDouble())
            }
            target.put("source", entry.optString("source", target.optString("source", "manual")))
            when (entry.optString("type")) {
                "weight" -> if (value in 20.0..400.0) target.put("weightKg", value)
                "leanBodyMass" -> if (value in 1.0..300.0) target.put("leanBodyMassKg", value)
                "bodyFatPercentage" -> if (value in 1.0..90.0) target.put("bodyFatPercentage", value)
                "waistCircumference" -> if (value in 20.0..300.0) target.put("waistCm", value)
                "neckCircumference" -> if (value in 20.0..300.0) target.put("neckCm", value)
                "hipCircumference" -> if (value in 20.0..300.0) target.put("hipCm", value)
            }
        }
        val history = JSONArray()
        byDate.toSortedMap().values.forEach { history.put(it) }
        if (history.length() > 0) {
            metricPreferences.edit().putString("history", history.toString()).apply()
            // The body section reads its circumference card from this compact
            // profile store. Mirror the newest remote values into it so the
            // imported iOS measurements are visible immediately, not just
            // retained in background history.
            val latest = history.optJSONObject(history.length() - 1)
            if (latest != null) {
                val details = context.getSharedPreferences("wildforce_profile_details", Context.MODE_PRIVATE)
                val current = details.getString("body_composition", "{}")?.let(::JSONObject) ?: JSONObject()
                fun copyMeasurement(localKey: String) {
                    if (!latest.isNull(localKey)) current.put(localKey, latest.optDouble(localKey))
                }
                copyMeasurement("waistCm")
                copyMeasurement("neckCm")
                copyMeasurement("hipCm")
                details.edit().putString("body_composition", current.toString()).apply()
            }
        }
    }

    /** Applies user settings before the next push so a fresh Android install never resets iOS choices. */
    private fun pullRemoteAppSettings(context: Context, remote: RemoteSnapshot): Boolean? {
        val settings = remote.records("user-app-settings").firstLiveRecord() ?: return null
        val profile = context.getSharedPreferences("wildforce_profile_details", Context.MODE_PRIVATE)
        profile.edit()
            .putBoolean("keep_screen_on", settings.optBoolean("is_screen_on_during_workout_enabled", profile.getBoolean("keep_screen_on", true)))
            .putBoolean("full_focus", settings.optBoolean("is_full_focus_mode_enabled", profile.getBoolean("full_focus", false)))
            .putBoolean("has_seen_notification_request", settings.optBoolean("has_seen_notification_request", profile.getBoolean("has_seen_notification_request", false)))
            .putBoolean("has_seen_body_progress_tutorial", settings.optBoolean("has_seen_body_progress_tutorial", profile.getBoolean("has_seen_body_progress_tutorial", false)))
            .putBoolean("has_rated_app", settings.optBoolean("has_rated_app", profile.getBoolean("has_rated_app", false)))
            .putBoolean("amazfit_watch_enabled", settings.optBoolean("is_watch_auto_tracking_enabled", profile.getBoolean("amazfit_watch_enabled", false)))
            .putBoolean("wear_auto_tracking_enabled", settings.optBoolean("is_watch_auto_tracking_enabled", profile.getBoolean("wear_auto_tracking_enabled", false)))
            .apply()
        // HealthKit and Health Connect have the same account-level meaning.
        // Permission is still requested locally by Android before any data access.
        context.getSharedPreferences("wildforce_profile", Context.MODE_PRIVATE).edit()
            .putBoolean("health_connect", settings.optBoolean("is_health_kit_enabled", false))
            .apply()
        context.getSharedPreferences("wildforce_notification_settings", Context.MODE_PRIVATE).edit()
            .putBoolean("enabled", settings.optBoolean("is_notifications_enabled", true)).apply()
        context.getSharedPreferences("wildforce_watch", Context.MODE_PRIVATE).edit()
            .putBoolean("enabled", settings.optBoolean("is_watch_auto_tracking_enabled", false)).apply()
        return settings.takeUnless { it.isNull("is_health_kit_enabled") }
            ?.optBoolean("is_health_kit_enabled")
    }

    private fun synchronizeNutrition(token: String, userId: String, profile: OnboardingProfile, now: String, context: Context) {
        val prefs = NutritionStore.loadPreferences(context)
        val nutritionProfileId = remoteProjectionID(context, "nutrition-profiles", stableId("$userId:nutrition-profile"))
        push(token, "nutrition-profiles", JSONArray().put(recordFromRemoteProjection(context, "nutrition-profiles", nutritionProfileId, now)
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
        val dayIDs = ids.getString(nutritionDayIDsKey, "{}")?.let(::JSONObject) ?: JSONObject()
        val plannedMealIDs = ids.getString(nutritionMealIDsKey, "{}")?.let(::JSONObject) ?: JSONObject()
        val entries = JSONArray(); val items = JSONArray(); val media = JSONArray()
        NutritionStore.allForSync(context).forEach { meal ->
            val entryKey = meal.id.toString()
            val entryId = entryIDs.optString(entryKey).takeIf(::isUuid) ?: stableId("$userId:nutrition-entry:${meal.id}")
            entryIDs.put(entryKey, entryId)
            val photo = meal.photoPath?.takeIf { File(it).isFile }
            // Match iOS's graph: a log entry references a separately synced
            // media record; the binary uploads only after both rows exist.
            val remoteMediaUrl = meal.mediaUrl?.trim()?.takeIf { it.isNotBlank() && photo == null }
            val mediaId = if (photo != null || remoteMediaUrl != null) stableId("$entryId:nutrition-log-media") else null
            mediaId?.let { id ->
                media.put(record(id, now)
                    .put("source", if (remoteMediaUrl != null) "remoteURL" else "localPhoto")
                    .put("local_relative_path", JSONObject.NULL)
                    .put("remote_url", remoteMediaUrl ?: JSONObject.NULL))
            }
            entries.put(record(entryId, now).put("title", meal.name)
                .put("logged_at", meal.loggedDate.toString() + "T12:00:00Z").put("meal_type", meal.type.name.lowercase())
                .put("notes", meal.notes).put("is_favorite", meal.isBookmarked)
                .put("nutrition_log_media_id", mediaId ?: JSONObject.NULL)
                .put("nutrition_meal_id", meal.plannedMealKey?.let { key -> plannedMealIDs.optString(key).takeIf(::isUuid) } ?: JSONObject.NULL))
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
        push(token, "nutrition-log-media", media)
        push(token, "nutrition-log-entries", entries)
        push(token, "nutrition-log-items", items)
        // iOS stores meal photos separately from the sync graph and uploads
        // them after their entry exists. Do the same for Android photo logs.
        val mediaState = idsForNutritionMedia(context)
        val uploadedMedia = mediaState.getStringSet(uploadedNutritionMediaKey, emptySet()).orEmpty().toMutableSet()
        val remoteMedia = mediaState.getStringSet(remoteNutritionMediaKey, emptySet()).orEmpty()
        NutritionStore.allForSync(context).forEach { meal ->
            val entryId = entryIDs.optString(meal.id.toString()).takeIf(::isUuid)
            meal.photoPath?.takeIf { File(it).isFile }?.let { path ->
                if (entryId != null) {
                    val mediaKey = "$entryId:$path"
                    if (mediaKey in uploadedMedia || mediaKey in remoteMedia) return@let
                    try {
                        uploadNutritionLogImage(token, entryId, File(path))
                        uploadedMedia += mediaKey
                        mediaState.edit().putStringSet(uploadedNutritionMediaKey, uploadedMedia).apply()
                    } catch (_: MissingNutritionLogEntryException) {
                        // The local projection can outlive a server-side deletion.
                        // iOS keeps the media task queued until its parent exists;
                        // recreate the parent once before retrying the binary.
                        val entry = (0 until entries.length())
                            .asSequence()
                            .mapNotNull(entries::optJSONObject)
                            .firstOrNull { it.optString("id") == entryId }
                        try {
                            if (entry == null) throw IllegalStateException("No se encontró la entrada de comida para reintentar la foto")
                            pushBatch(token, "nutrition-log-entries", JSONArray().put(entry))
                            uploadNutritionLogImage(token, entryId, File(path))
                            uploadedMedia += mediaKey
                            mediaState.edit().putStringSet(uploadedNutritionMediaKey, uploadedMedia).apply()
                        } catch (error: Exception) {
                            RemoteSyncChangeTracker.mark(context, "nutrition-log-media")
                            throw error
                        }
                    } catch (error: Exception) {
                        // Keep the binary queued after its graph records have
                        // been accepted, just as iOS retains MediaOutboundTask.
                        RemoteSyncChangeTracker.mark(context, "nutrition-log-media")
                        throw error
                    }
                }
            }
        }

        val plan = NutritionStore.loadGeneratedPlan(context) ?: return
        val planMetadata = NutritionStore.loadPlanMetadata(context)
        // A server-generated plan already has an ID. Reuse it instead of
        // creating Android's deterministic duplicate on the next sync.
        val nutritionPlanId = NutritionStore.remotePlanId(context) ?: stableId("$userId:nutrition-plan")
        NutritionStore.rememberRemotePlanId(context, nutritionPlanId)
        push(token, "nutrition-plans", JSONArray().put(recordFromRemoteProjection(context, "nutrition-plans", nutritionPlanId, now)
            .put("starts_on", (planMetadata?.startsOn ?: plan.minOf { it.date }).toString() + "T00:00:00Z")
            .put("goal", planMetadata?.goal?.storedValue ?: profile.goal.storedValue)
            .put("body_composition_phase", planMetadata?.bodyCompositionPhase?.storedValue ?: JSONObject.NULL)
            .put("daily_calorie_average", planMetadata?.dailyCalorieAverage ?: plan.map { it.targets.calories }.average())
            .put("notes", planMetadata?.notes ?: JSONObject.NULL)))
        val days = JSONArray(); val meals = JSONArray()
        val dismissedMeals = context.getSharedPreferences("wildforce_nutrition", Context.MODE_PRIVATE)
            .getStringSet("nutrition_dismissed_planned_meals", emptySet()).orEmpty()
        plan.forEach { day ->
            val date = day.date.toString()
            val dayId = dayIDs.optString(date).takeIf(::isUuid) ?: stableId("$nutritionPlanId:$date")
            dayIDs.put(date, dayId)
            days.put(recordFromRemoteProjection(context, "nutrition-days", dayId, now).put("nutrition_plan_id", nutritionPlanId).put("date", day.date.toString())
                .put("weekday", day.date.dayOfWeek.name.lowercase()).put("day_type", day.type.name.lowercase())
                .put("target_calories", day.targets.calories).put("target_protein_grams", day.targets.protein)
                .put("target_carbs_grams", day.targets.carbs).put("target_fat_grams", day.targets.fat)
                .put("planned_workout_title", day.workoutTitle).put("energy_demand", day.energyDemand.name.lowercase())
                .put("notes", day.notes).put("pre_workout_guidance", day.preWorkoutGuidance ?: JSONObject.NULL)
                .put("post_workout_guidance", day.postWorkoutGuidance ?: JSONObject.NULL))
            day.meals.forEachIndexed { index, meal ->
                val mealKey = nutritionMealKey(date, meal.type.name, meal.title)
                val mealId = plannedMealIDs.optString(mealKey).takeIf(::isUuid) ?: stableId("$dayId:$index")
                plannedMealIDs.put(mealKey, mealId)
                meals.put(recordFromRemoteProjection(context, "nutrition-meals", mealId, now)
                .put("nutrition_day_id", dayId).put("title", meal.title).put("order_index", index).put("meal_type", meal.type.name.lowercase())
                .put("target_calories", meal.targets.calories).put("target_protein_grams", meal.targets.protein)
                .put("target_carbs_grams", meal.targets.carbs).put("target_fat_grams", meal.targets.fat)
                .put("guidance", meal.guidance).put("example_foods", JSONArray().apply { meal.foods.forEach { (name, grams) -> put(JSONObject().put("name", name).put("grams", grams)) } })
                .put("discarded_at", if (mealKey in dismissedMeals) now else JSONObject.NULL))
            }
        }
        ids.edit().putString(nutritionDayIDsKey, dayIDs.toString()).putString(nutritionMealIDsKey, plannedMealIDs.toString()).apply()
        push(token, "nutrition-days", days)
        push(token, "nutrition-meals", meals)
    }

    private fun idsForNutritionMedia(context: Context): SharedPreferences =
        context.getSharedPreferences(syncPrefs, Context.MODE_PRIVATE)

    private fun uploadNutritionLogImage(token: String, entryId: String, image: File) {
        require(image.length() in 1..(5 * 1024 * 1024)) { "La imagen de la comida supera el límite de 5 MB" }
        val boundary = "Wildforce-${UUID.randomUUID()}"
        val connection = URL(WildforceApiEnvironment.apiUrl("nutrition/log-entries/$entryId/image")).openConnection() as HttpURLConnection
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
            val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
            val response = BufferedReader(stream.reader()).use { it.readText() }
            if (connection.responseCode !in 200..299) {
                val error = apiException(response, "No se pudo subir la foto de comida")
                if (connection.responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                    throw MissingNutritionLogEntryException(error.message)
                }
                throw error
            }
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
            val remoteId = bodyProgressRemoteID(context, userId, localId)
            records.put(recordFromRemoteProjection(context, "body-progress-photo-sessions", remoteId, now))
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

    private fun pullRemoteAvatar(context: Context, token: String, userId: String) {
        val profile = context.getSharedPreferences("wildforce_profile", Context.MODE_PRIVATE)
        profile.getString("avatar_path", null)?.let(::File)?.takeIf(File::isFile)?.let { return }
        val avatar = File(context.filesDir, "profile-avatar.png")
        val temporary = File(context.filesDir, "profile-avatar.download")
        if (download(token, WildforceApiEnvironment.publicMediaUrl("avatars/${userId.lowercase()}.png").toString(), temporary)) {
            if (avatar.exists()) avatar.delete()
            if (temporary.renameTo(avatar)) profile.edit().putString("avatar_path", avatar.absolutePath).apply()
        }
    }

    /** Restores media only during the authoritative first-account import. */
    private fun pullRemoteMedia(context: Context, token: String, sessions: JSONArray) {
        val preferences = context.getSharedPreferences("wildforce_body_progress", Context.MODE_PRIVATE)
        val localById = linkedMapOf<String, JSONObject>()
        val stored = preferences.getString("sessions", "[]")?.let(::JSONArray) ?: JSONArray()
        for (index in 0 until stored.length()) {
            stored.optJSONObject(index)?.let { session ->
                session.optString("id").takeIf(String::isNotBlank)?.let { localById[it] = session }
            }
        }
        val remoteIds = context.getSharedPreferences(syncPrefs, Context.MODE_PRIVATE)
            .getString("body_progress_remote_ids", "{}")?.let(::JSONObject) ?: JSONObject()
        val folder = File(context.filesDir, "body_progress_photos").apply { mkdirs() }
        for (index in 0 until sessions.length()) {
            val session = sessions.optJSONObject(index) ?: continue
            val id = session.optString("id").takeIf(String::isNotBlank) ?: continue
            if (!session.isNull("deleted_at")) {
                localById.remove(id)
                remoteIds.remove(id)
                continue
            }
            val front = File(folder, "${id}_front.jpg")
            val profile = File(folder, "${id}_profile.jpg")
            val frontDownloaded = download(token, WildforceApiEnvironment.apiUrl("body-progress-photo-sessions/$id/photos/front"), front)
            val profileDownloaded = download(token, WildforceApiEnvironment.apiUrl("body-progress-photo-sessions/$id/photos/profile"), profile)
            if (frontDownloaded && profileDownloaded) {
                localById[id] = JSONObject().put("id", id).put("date", session.optString("created_at").take(10))
                    .put("frontPath", front.absolutePath).put("profilePath", profile.absolutePath)
                remoteIds.put(id, id)
            }
        }
        preferences.edit().putString("sessions", JSONArray().apply { localById.values.forEach(::put) }.toString()).apply()
        context.getSharedPreferences(syncPrefs, Context.MODE_PRIVATE).edit()
            .putString("body_progress_remote_ids", remoteIds.toString()).apply()
    }

    /** Imported sessions retain their server UUID; locally-created sessions keep Android's stable ID. */
    private fun bodyProgressRemoteID(context: Context, userId: String, localSessionId: String): String {
        val ids = context.getSharedPreferences(syncPrefs, Context.MODE_PRIVATE)
            .getString("body_progress_remote_ids", "{}")?.let(::JSONObject) ?: JSONObject()
        return ids.optString(localSessionId).takeIf(::isUuid) ?: stableId("$userId:body-progress:$localSessionId")
    }

    /** Keep fields Android does not model (address, coordinates and location photo) when editing an iOS record. */
    private fun recordFromRemoteProjection(context: Context, resource: String, id: String, now: String): JSONObject {
        val cached = context.getSharedPreferences(syncStatePrefs, Context.MODE_PRIVATE)
            .getString("remote_projection:$resource", "[]")?.let { runCatching(::JSONArray).getOrNull() } ?: JSONArray()
        for (index in 0 until cached.length()) {
            val source = cached.optJSONObject(index) ?: continue
            if (source.optString("id") == id) {
                return JSONObject(source.toString()).apply {
                    put("id", id)
                    put("updated_at", now)
                    remove("deleted_at")
                }
            }
        }
        return record(id, now)
    }

    /** Singleton child models are assigned UUIDs by iOS, not derived from the user ID. */
    private fun remoteProjectionID(context: Context, resource: String, fallback: String): String {
        val cached = context.getSharedPreferences(syncStatePrefs, Context.MODE_PRIVATE)
            .getString("remote_projection:$resource", "[]")?.let { runCatching(::JSONArray).getOrNull() } ?: JSONArray()
        for (index in 0 until cached.length()) {
            val candidate = cached.optJSONObject(index) ?: continue
            if (candidate.isNull("deleted_at")) {
                candidate.optString("id").takeIf(::isUuid)?.let { return it }
            }
        }
        return fallback
    }

    /**
     * A first Android session must restore the server plan before creating any local
     * records. Otherwise a freshly generated empty/default plan could win the
     * last-write-wins conflict and hide the user's iOS workouts.
     */
    private fun pullWorkoutPlans(context: Context, remote: RemoteSnapshot, preferences: SharedPreferences): Boolean {
        val remotePlans = remote.records("workout-plans").liveRecords()
        Log.d("WorkoutRemoteSync", "Pulled workout-plans=${remotePlans.size}, workout-days=${remote.records("workout-days").liveRecords().size}")
        if (remotePlans.isEmpty()) return false
        val remoteDays = pullRemoteWorkoutDays(remote)
        importRemoteWorkoutHistory(context, remoteDays)
        // The profile summary must come from the cross-device workout graph,
        // not from Android's local counter alone.
        val completedCount = remoteDays.liveRecords().count { day ->
            day.optString("status").equals("completed", ignoreCase = true) ||
                day.optString("status").equals("finished", ignoreCase = true)
        }
        context.getSharedPreferences("wildforce_profile", Context.MODE_PRIVATE).edit()
            .putInt("total_completed_workouts", completedCount).apply()

        val plans = remotePlans.mapNotNull { plan ->
            val planId = plan.optString("id").takeIf(String::isNotBlank) ?: return@mapNotNull null
            remotePlanJson(context, plan, planId, remoteDays)
        }
        if (plans.isEmpty()) return false
        // Match iOS User.currentWorkoutPlan exactly: active plans first, then
        // newest createdAt, updatedAt, and finally UUID as a deterministic tie
        // breaker. Do not infer the current plan from mesocycle numbers.
        val activeCandidates = plans.filter { it.status.equals("active", ignoreCase = true) }
        val active = (activeCandidates.ifEmpty { plans }).maxWithOrNull(
            compareBy<RemotePlan> { it.createdAt }
                .thenBy { it.updatedAt }
                .thenBy { it.id }
        ) ?: plans.first()
        Log.d("WorkoutRemoteSync", "Selected plan id=${active.id}, status=${active.status}, mesocycle=${active.mesocycleNumber}, createdAt=${active.createdAt}, updatedAt=${active.updatedAt}")
        // Keep every completed mesocycle locally. The hub renders the selected
        // active plan while its history remains available to overview screens.
        plans.filterNot { it.id == active.id }.forEach { archived ->
            WorkoutPlanArchiveStore.archive(preferences, archived.rawJson)
        }
        preferences.edit().putString("workout_plan_json", active.rawJson)
            .putString(planKey, active.id)
            .putBoolean("remote_plan_imported", true).apply()
        return true
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

    private fun remotePlanJson(context: Context, plan: JSONObject, planId: String, remoteDays: JSONArray): RemotePlan? {
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
                    .put("notes", block.opt("notes")).put("exercises", remoteExercises(context, block.optJSONArray("exercises"))))
            }
            // iOS no longer models day-level exercises. Read the legacy shape
            // only by migrating it into a standard block, never re-emitting it.
            if (blocks.length() == 0) {
                val legacyExercises = remoteExercises(context, day.optJSONArray("exercises"))
                if (legacyExercises.length() > 0) {
                    blocks.put(JSONObject().put("remoteId", "legacy-direct-exercises").put("type", "standard")
                        .put("rounds", 1).put("exercises", legacyExercises))
                }
            }
            workouts.put(JSONObject().put("id", day.optString("id")).put("remoteId", day.optString("id")).put("title", day.optString("title", "Sesión ${index + 1}"))
                .put("focus", day.optString("focus", "Fitness general")).put("dayType", day.optString("day_type", "strength"))
                .put("status", day.optString("status", "planned").uppercase()).put("kind", day.optString("kind", "workout"))
                .put("creationSource", day.optString("creation_source", "generated"))
                .put("scheduledFor", day.optString("scheduled_for").takeIf(String::isNotBlank))
                .put("startedAt", day.optString("started_at").takeIf(String::isNotBlank))
                .put("completedAt", day.optString("completed_at").takeIf(String::isNotBlank))
                .put("didCountTowardStreak", day.optBoolean("did_count_toward_streak", false))
                .put("notes", day.optString("notes").takeIf(String::isNotBlank))
                .put("activeDurationSeconds", day.optInt("active_duration_seconds").takeIf { day.has("active_duration_seconds") && !day.isNull("active_duration_seconds") })
                .put("activeCaloriesBurned", day.optDouble("active_calories_burned").takeIf { day.has("active_calories_burned") && !day.isNull("active_calories_burned") })
                .put("averageHeartRate", day.optDouble("average_heart_rate").takeIf { day.has("average_heart_rate") && !day.isNull("average_heart_rate") })
                .put("maximumHeartRate", day.optDouble("maximum_heart_rate").takeIf { day.has("maximum_heart_rate") && !day.isNull("maximum_heart_rate") })
                .put("totalVolumeKg", day.optDouble("total_volume_kg").takeIf { day.has("total_volume_kg") && !day.isNull("total_volume_kg") })
                .put("weekday", day.optString("intended_weekday", "MONDAY").uppercase())
                .put("estimatedDurationMinutes", day.optInt("estimated_duration_minutes", 50)).put("blocks", blocks))
        }
        if (workouts.length() == 0) return null
        // The API normally returns snake_case, but Android also receives
        // locally-created/legacy payloads using the JSON names emitted by the
        // plan generator. Read both forms so a downloaded plan keeps its real
        // progression metadata instead of silently falling back to 1.
        val cycleLength = plan.optInt("cycle_length", plan.optInt("cycleLength", 1)).coerceAtLeast(1)
        val phaseWeek = plan.optInt("phase_week", plan.optInt("phaseWeek", 1)).coerceIn(1, cycleLength)
        val mesocycleNumber = plan.optInt("mesocycle_number", plan.optInt("mesocycleNumber", 1)).coerceAtLeast(1)
        val mesocycleIndex = plan.optInt("mesocycle_index", plan.optInt("mesocycleIndex", ((mesocycleNumber - 1) / cycleLength) + 1)).coerceAtLeast(1)
        val root = JSONObject().put("planName", plan.optString("name", "Plan sincronizado"))
            .put("planStatus", plan.optString("status", "active").ifBlank { "active" })
            .put("planGoal", WorkoutPlanGenerator.normalizeNullableString(plan.opt("goal")))
            .put("bodyCompositionPhase", WorkoutPlanGenerator.normalizeNullableString(plan.opt("body_composition_phase")))
            .put("planNotes", WorkoutPlanGenerator.normalizeNullableString(plan.opt("notes")))
            .put("planStartsOn", WorkoutPlanGenerator.normalizeNullableString(plan.opt("starts_on")))
            .put("profileSettingsHash", WorkoutPlanGenerator.normalizeNullableString(plan.opt("profile_settings_hash")))
            .put("phase", plan.optString("phase", "Plan de tu cuenta"))
            .put("cycleLength", cycleLength).put("mesocycleNumber", mesocycleNumber)
            .put("mesocycleIndex", mesocycleIndex).put("weekIndex", phaseWeek)
            .put("phaseWeek", phaseWeek).put("workouts", workouts)
        return RemotePlan(
            id = planId,
            status = plan.optString("status"),
            mesocycleNumber = mesocycleNumber,
            updatedAt = plan.optString("updated_at", plan.optString("updatedAt")),
            createdAt = plan.optString("created_at", plan.optString("createdAt")),
            rawJson = root.toString(),
        )
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
                    val key = exerciseReferenceKey(exercise)
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

    private fun remoteExercises(context: Context, entries: JSONArray?): JSONArray = JSONArray().apply {
        entries ?: return@apply
        for (index in 0 until entries.length()) {
            val exercise = entries.optJSONObject(index) ?: continue
            if (!exercise.isNull("deleted_at")) continue
            val min = exercise.opt("reps_min"); val max = exercise.opt("reps_max")
            val coachId = exercise.optString("coach_exercise_id").trim().lowercase().takeIf(String::isNotBlank)
            val exerciseCatalogId = exercise.optString("exercise").trim().takeIf(String::isNotBlank)
            val coach = coachId?.let { CoachExerciseMediaStore.latest(context, "coach:$it") }
            val imageKey = coachId?.let { "coach:$it" } ?: exerciseCatalogId.orEmpty()
            val name = coach?.name ?: exerciseCatalogId ?: "Ejercicio"
            put(JSONObject().put("remoteId", exercise.optString("id")).put("name", name).put("imageKey", imageKey)
                .put("coachExerciseId", coachId)
                .put("sets", exercise.optInt("sets", 1)).put("repsMin", min).put("repsMax", max)
                .put("reps", if (min is Number && max is Number) "${min.toInt()}-${max.toInt()}" else "")
                .put("restSeconds", exercise.optInt("rest_seconds", 0)).put("targetWeightKg", exercise.opt("target_weight_kg"))
                .put("targetReps", exercise.opt("target_reps")).put("targetWeightsKg", exercise.opt("target_weights_kg"))
                .put("targetDurationMinutes", exercise.opt("target_duration_minutes")).put("targetDurationSeconds", exercise.opt("target_duration_seconds"))
                .put("targetDistanceKm", exercise.opt("target_distance_km")).put("targetPaceSecondsPerKm", exercise.opt("target_pace_seconds_per_km")).put("setStyle", androidSetStyle(exercise.optJSONObject("set_style_configuration")?.optString("style")))
                .put("setStyleParameters", androidSetStyleParameters(exercise.optJSONObject("set_style_configuration")))
                .put("trackingMode", coach?.trackingMode?.androidTrackingMode() ?: JSONObject.NULL)
                .put("isPerSideLoad", coach?.isPerSideLoad ?: false))
        }
    }

    private fun exerciseReferenceKey(exercise: JSONObject): String = exercise.optString("coach_exercise_id")
        .trim()
        .lowercase()
        .takeIf(String::isNotBlank)
        ?.let { "coach:$it" }
        ?: exercise.optString("exercise").trim().lowercase()

    private fun String.androidTrackingMode(): String? = when (trim().lowercase()) {
        "reps", "repetitions" -> "Repetitions"
        "durationseconds", "duration_seconds", "durationminutes", "duration_minutes" -> "Duration"
        "durationminutesanddistance", "duration_minutes_and_distance" -> "DurationAndDistance"
        else -> null
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

    /** Keep the iOS WorkoutDayStatus raw value expected by the sync API. */
    private fun backendWorkoutStatus(value: String): String = when (
        value.trim().replace("_", "").replace("-", "").lowercase()
    ) {
        "draft" -> "draft"
        "planned" -> "planned"
        "inprogress" -> "inProgress"
        "skipped" -> "skipped"
        "completed", "finished" -> "completed"
        else -> "planned"
    }

    private fun synchronizeWorkoutPlan(token: String, userId: String, profile: OnboardingProfile, now: String, context: Context, preferences: SharedPreferences, root: JSONObject) {
        val planId = preferences.getString(planKey, null) ?: stableId("$userId:${root.optString("planName")}").also { preferences.edit().putString(planKey, it).apply() }
        push(token, "workout-plans", JSONArray().put(record(planId, now)
            .put("name", root.optString("planName", "Plan Android")).put("goal", root.optString("planGoal", profile.goal.storedValue)).put("status", root.optString("planStatus", "active"))
            .put("phase", root.optString("phase")).put("mesocycle_number", root.optInt("mesocycleNumber", 1))
            .put("phase_week", root.optInt("phaseWeek", 1)).put("cycle_length", root.optInt("cycleLength", 1))
            .put("body_composition_phase", profile.bodyCompositionPhase?.storedValue ?: JSONObject.NULL)
            .put("notes", root.opt("planNotes") ?: JSONObject.NULL)
            .put("starts_on", root.opt("planStartsOn") ?: JSONObject.NULL)
            .put("profile_settings_hash", root.opt("profileSettingsHash") ?: JSONObject.NULL)))
        val days = root.optJSONArray("workouts") ?: JSONArray(); val dayRecords = JSONArray(); val blocks = JSONArray(); val exercises = JSONArray(); val plannedExerciseIds = mutableMapOf<String, String>()
        val skippedExercises = skippedExerciseTimestamps(context)
        val uploadedSkippedExerciseKeys = mutableSetOf<String>()
        for (dayIndex in 0 until days.length()) {
            // Server-generated days use their UUID directly.  Retaining it is
            // essential for iOS/Android parity: it is also the public workout
            // card identifier used by the share route.
            val day = days.getJSONObject(dayIndex); val dayId = day.optString("remoteId").takeIf(::isUuid)
                ?: day.optString("id").takeIf(::isUuid)
                ?: stableId("$planId:${day.optString("id", dayIndex.toString())}")
            val localWorkoutId = day.optString("id", dayIndex.toString())
            var executionIndex = 0
            dayRecords.put(record(dayId, now).put("workout_plan_id", planId).put("kind", day.optString("kind", "workout")).put("title", day.optString("title", "Sesión ${dayIndex + 1}")).put("focus", backendWorkoutFocus(day.optString("focus"))).put("status", backendWorkoutStatus(day.optString("status", "planned"))).put("did_count_toward_streak", day.optBoolean("didCountTowardStreak", false)).put("order_index", dayIndex).put("intended_weekday", day.optString("weekday").lowercase()).put("day_type", day.optString("dayType")).put("creation_source", day.optString("creationSource", "generated")).put("estimated_duration_minutes", day.optInt("estimatedDurationMinutes", 50)).apply {
                day.optString("scheduledFor").takeIf(String::isNotBlank)?.let { put("scheduled_for", it) }
                day.optString("startedAt").takeIf(String::isNotBlank)?.let { put("started_at", it) }
                day.optString("completedAt").takeIf(String::isNotBlank)?.let { put("completed_at", it) }
                day.optString("notes").takeIf(String::isNotBlank)?.let { put("notes", it) }
                listOf(
                    "activeDurationSeconds" to "active_duration_seconds",
                    "activeCaloriesBurned" to "active_calories_burned",
                    "averageHeartRate" to "average_heart_rate",
                    "maximumHeartRate" to "maximum_heart_rate",
                    "totalVolumeKg" to "total_volume_kg",
                ).forEach { (local, remote) -> if (day.has(local) && !day.isNull(local)) put(remote, day.opt(local)) }
            })
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
                    val coachExerciseId = item.optString("coachExerciseId").trim().lowercase().takeIf(String::isNotBlank)
                    val historyKey = coachExerciseId?.let { "coach:$it" }
                        ?: item.optString("imageKey").trim().ifBlank { item.optString("name").trim() }.lowercase()
                    if (historyKey.isNotBlank()) plannedExerciseIds.putIfAbsent(historyKey, exerciseId)
                    // Catalog and coach references are mutually exclusive, as
                    // on iOS. The API deliberately receives the null catalog
                    // field when a coach exercise is selected.
                    val exerciseCatalogId = item.optString("imageKey").trim()
                    require(coachExerciseId != null || exerciseCatalogId.isNotBlank()) { "El ejercicio ${item.optString("name", exerciseIndex.toString())} no tiene identificador de catálogo." }
                    val skippedKey = "$localWorkoutId|$executionIndex"
                    val record = record(exerciseId, now).put("workout_day_id", dayId).put("workout_block_id", blockId)
                        .put("exercise", if (coachExerciseId == null) exerciseCatalogId else JSONObject.NULL)
                        .put("coach_exercise_id", coachExerciseId ?: JSONObject.NULL)
                        .put("order_index", exerciseIndex).put("sets", item.optInt("sets", 1)).put("reps_min", item.opt("repsMin")).put("reps_max", item.opt("repsMax")).put("target_reps", item.opt("targetReps")).put("target_weight_kg", item.opt("targetWeightKg")).put("target_weights_kg", item.opt("targetWeightsKg")).put("target_duration_minutes", item.opt("targetDurationMinutes")).put("target_duration_seconds", item.opt("targetDurationSeconds")).put("target_distance_km", item.opt("targetDistanceKm")).put("target_pace_seconds_per_km", item.opt("targetPaceSecondsPerKm")).put("rest_seconds", item.optInt("restSeconds", 0)).put("set_style_configuration", backendSetStyleConfiguration(item))
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
                .put("planned_exercise_id", plannedExerciseId).put("feedback", backendExerciseFeedback(entry.feedback))
                .put("completed_at", java.time.Instant.ofEpochMilli(entry.timestampMillis).toString())
                .put("completed_sets", entry.sets).put("completed_reps", entry.totalReps).put("completed_weight", entry.maxWeightKg.finiteOrZero())
                .put("per_set_reps", JSONArray(entry.setReps)).put("per_set_weights_kg", JSONArray(entry.setWeightsKg.map { it.finiteOrZero() }))
                .put("completed_duration_seconds", entry.durationSeconds).put("completed_duration_minutes", entry.durationSeconds / 60)
                .put("completed_distance_km", entry.distanceKm.finiteOrZero()).put("notes", entry.note ?: JSONObject.NULL))
        }
        push(token, "exercise-results", results)
    }

    private fun record(id: String, now: String) = JSONObject().put("id", id).put("created_at", now).put("updated_at", now)

    /** Uses the same Codable values as iOS and the backend ExerciseFeedback enum. */
    private fun backendExerciseFeedback(value: String?): String = when (value.orEmpty().trim().lowercase()
        .replace("_", "").replace("-", "").replace(" ", "")) {
        "veryeasy", "mu fácil", "muyfacil" -> "veryEasy"
        "easy", "facil", "fácil" -> "easy"
        "justright", "normal", "justo" -> "justRight"
        "hard", "dificil", "difícil" -> "hard"
        "veryhard", "muydificil", "muydifícil" -> "veryHard"
        else -> "justRight"
    }

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
        if (resource !in activePushResources) return
        val state = activePushContext?.getSharedPreferences(syncStatePrefs, Context.MODE_PRIVATE)
            ?: return
        val previous = state.getString("$pushedRecordsPrefix$resource", "{}")
            ?.let { runCatching(::JSONObject).getOrNull() } ?: JSONObject()
        val current = JSONObject()
        val changes = JSONArray()
        for (index in 0 until records.length()) {
            val record = records.optJSONObject(index) ?: continue
            val id = record.optString("id").takeIf(String::isNotBlank) ?: continue
            current.put(id, record)
            val previousRecord = previous.optJSONObject(id)
            if (previousRecord == null || syncFingerprint(previousRecord) != syncFingerprint(record)) {
                changes.put(record)
            }
        }
        // Android projections do not retain deleted rows. Preserve their last
        // accepted payload and turn a disappearance into the same tombstone
        // iOS sends through SwiftData.
        val deletedAt = java.time.Instant.now().toString()
        previous.keys().forEach { id ->
            if (!current.has(id)) {
                previous.optJSONObject(id)?.let { prior ->
                    changes.put(JSONObject(prior.toString())
                        .put("updated_at", deletedAt)
                        .put("deleted_at", deletedAt))
                }
            }
        }
        if (changes.length() == 0) {
            clearDirtyResource(resource)
            return
        }
        for (start in 0 until changes.length() step 100) {
            val batch = JSONArray()
            for (index in start until minOf(start + 100, changes.length())) batch.put(changes.get(index))
            pushBatch(token, resource, batch)
        }
        // iOS applies its own accepted writes to SwiftData immediately. Without
        // this equivalent, the next Android pull can materialize an older
        // cached User row and overwrite a just-saved weight before the server
        // happens to echo that row back through the pull watermark.
        updateRemoteProjectionAfterPush(resource, current, previous, deletedAt)
        state.edit().putString("$pushedRecordsPrefix$resource", current.toString()).apply()
        clearDirtyResource(resource)
    }

    /** Make a successfully accepted local write part of the same record projection used by pulls. */
    private fun updateRemoteProjectionAfterPush(
        resource: String,
        current: JSONObject,
        previous: JSONObject,
        deletedAt: String,
    ) {
        val context = activePushContext ?: return
        val state = context.getSharedPreferences(syncStatePrefs, Context.MODE_PRIVATE)
        val cached = state.getString("remote_projection:$resource", "[]")
            ?.let { runCatching(::JSONArray).getOrNull() } ?: JSONArray()
        val byId = linkedMapOf<String, JSONObject>()
        for (index in 0 until cached.length()) {
            cached.optJSONObject(index)?.let { record ->
                record.optString("id").takeIf(String::isNotBlank)?.let { byId[it] = JSONObject(record.toString()) }
            }
        }
        current.keys().forEach { id ->
            current.optJSONObject(id)?.let { byId[id] = JSONObject(it.toString()) }
        }
        previous.keys().forEach { id ->
            if (!current.has(id)) previous.optJSONObject(id)?.let { prior ->
                byId[id] = JSONObject(prior.toString()).put("updated_at", deletedAt).put("deleted_at", deletedAt)
            }
        }
        state.edit().putString("remote_projection:$resource", JSONArray().apply { byId.values.forEach(::put) }.toString()).apply()
    }

    /** Timestamps are transport metadata; only material payload changes push. */
    private fun syncFingerprint(record: JSONObject): String = JSONObject(record.toString()).apply {
        remove("created_at")
        remove("updated_at")
        remove("deleted_at")
    }.toString()

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

    /** Mirrors iOS's paginated `/sync/pull/batch` pull state, per resource. */
    private fun pullRemoteSnapshot(context: Context, token: String): RemoteSnapshot {
        val result = linkedMapOf<String, JSONArray>()
        val pullState = context.getSharedPreferences(syncStatePrefs, Context.MODE_PRIVATE)
        var request = JSONObject().put("resources", JSONArray().apply {
            batchResources.forEach { resource ->
                val watermark = pullState.getString("$pullWatermarkPrefix$resource", null)
                put(JSONObject().put("resource", resource).put("updated_after", watermark ?: JSONObject.NULL))
            }
        })
        var asOf: String? = null
        do {
            val response = postJson(token, WildforceApiEnvironment.apiUrl("sync/pull/batch"), request)
            if (asOf == null) asOf = response.optJSONObject("meta")?.optString("as_of")?.takeIf(String::isNotBlank)
            val data = response.optJSONArray("data") ?: JSONArray()
            for (index in 0 until data.length()) {
                val item = data.optJSONObject(index) ?: continue
                val resource = item.optString("resource")
                val record = item.optJSONObject("record") ?: continue
                if (resource in batchResources) result.getOrPut(resource, ::JSONArray).put(record)
            }
            // JSONObject.optString turns a JSON null into the literal string
            // "null". Treat both representations as the end of pagination;
            // otherwise Android posts cursor="null" after the final page and
            // the backend rejects the whole sync as an invalid cursor.
            val cursor = response.optJSONObject("meta")?.opt("next_cursor")
                ?.takeUnless { it == JSONObject.NULL }
                ?.toString()
                ?.takeUnless { it.equals("null", ignoreCase = true) }
                .orEmpty()
            request = if (cursor.isBlank()) JSONObject() else JSONObject().put("cursor", cursor)
        } while (request.has("cursor"))
        return RemoteSnapshot(result, asOf)
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

    private data class RemoteSnapshot(private val data: Map<String, JSONArray>, private val asOf: String?) {
        fun records(resource: String): JSONArray = data[resource] ?: JSONArray()

        /**
         * `/sync/pull/batch` returns only rows changed since each resource's
         * watermark. iOS merges those rows into SwiftData; retain the matching
         * Android projection so graph importers always see parents, children,
         * and tombstones together.
         */
        fun materialize(context: Context): RemoteSnapshot {
            val preferences = context.getSharedPreferences(syncStatePrefs, Context.MODE_PRIVATE)
            val full = linkedMapOf<String, JSONArray>()
            val editor = preferences.edit()
            batchResources.forEach { resource ->
                val byId = linkedMapOf<String, JSONObject>()
                val cached = preferences.getString("remote_projection:$resource", "[]")
                    ?.let { runCatching(::JSONArray).getOrNull() } ?: JSONArray()
                for (index in 0 until cached.length()) {
                    cached.optJSONObject(index)?.let { record ->
                        record.optString("id").takeIf(String::isNotBlank)?.let { byId[it] = record }
                    }
                }
                val changes = data[resource] ?: JSONArray()
                for (index in 0 until changes.length()) {
                    changes.optJSONObject(index)?.let { record ->
                        record.optString("id").takeIf(String::isNotBlank)?.let { byId[it] = JSONObject(record.toString()) }
                    }
                }
                val records = JSONArray().apply { byId.values.forEach(::put) }
                full[resource] = records
                editor.putString("remote_projection:$resource", records.toString())
            }
            editor.apply()
            return RemoteSnapshot(full, asOf)
        }

        /** iOS overlaps one second because server timestamps have second precision. */
        fun savePullWatermarks(context: Context, failedResources: Set<String> = emptySet()) {
            val serverTime = asOf?.let { runCatching { java.time.Instant.parse(it) }.getOrNull() } ?: return
            val watermark = serverTime.minusSeconds(1).toString()
            context.getSharedPreferences(syncStatePrefs, Context.MODE_PRIVATE).edit().apply {
                batchResources.filterNot(failedResources::contains).forEach { resource ->
                    putString("$pullWatermarkPrefix$resource", watermark)
                }
            }.apply()
        }
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
        if (error?.optString("code") == "subscription_required") return SubscriptionRequiredException
        val detail = error?.optString("message")?.takeIf(String::isNotBlank)
            ?: response.trim().takeIf(String::isNotBlank)
        return IllegalStateException(detail ?: fallback)
    }

    private data object SubscriptionRequiredException : Exception()

    private class MissingNutritionLogEntryException(message: String?) : IllegalStateException(message)

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

    private data class RemotePlan(
        val id: String,
        val status: String,
        val mesocycleNumber: Int,
        val updatedAt: String,
        val createdAt: String,
        val rawJson: String,
    )

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

    private fun JSONObject.optionalDouble(key: String): Double =
        if (isNull(key)) Double.NaN else optDouble(key, Double.NaN)

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
