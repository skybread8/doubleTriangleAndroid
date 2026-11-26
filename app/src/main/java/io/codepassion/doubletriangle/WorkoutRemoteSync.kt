package io.codepassion.doubletriangle

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.annotation.SuppressLint
import android.util.Log
import io.codepassion.doubletriangle.nutrition.nutritionLogTimestampForMeal
import io.codepassion.doubletriangle.feature.onboarding.activeCustomWorkoutFocuses
import io.codepassion.doubletriangle.feature.onboarding.OnboardingProfile
import io.codepassion.doubletriangle.feature.onboarding.BodyMetricsStore
import io.codepassion.doubletriangle.feature.onboarding.BodyCompositionPhase
import io.codepassion.doubletriangle.feature.onboarding.Equipment
import io.codepassion.doubletriangle.feature.onboarding.GymType
import io.codepassion.doubletriangle.feature.onboarding.LifestyleLevel
import io.codepassion.doubletriangle.feature.onboarding.MovementRestriction
import io.codepassion.doubletriangle.feature.onboarding.TrainingLevel
import io.codepassion.doubletriangle.feature.onboarding.TrainingLocationProfile
import io.codepassion.doubletriangle.feature.onboarding.TrainingLocationPhotoStore
import io.codepassion.doubletriangle.feature.onboarding.TrainingSplitPreference
import io.codepassion.doubletriangle.feature.onboarding.WorkoutFocus
import io.codepassion.doubletriangle.feature.onboarding.WorkoutWeekday
import io.codepassion.doubletriangle.feature.onboarding.effectiveTrainingLocations
import io.codepassion.doubletriangle.core.model.WorkoutHubState
import io.codepassion.doubletriangle.core.model.WildforceApiEnvironment
import io.codepassion.doubletriangle.core.model.RemoteSyncChangeTracker
import io.codepassion.doubletriangle.core.model.SyncDiagnostics
import io.codepassion.doubletriangle.feature.workout.WorkoutHistoryStore
import io.codepassion.doubletriangle.feature.workout.ExerciseProfileSync
import io.codepassion.doubletriangle.feature.workout.CoachExerciseMediaStore
import io.codepassion.doubletriangle.nutrition.NutritionStore
import io.codepassion.doubletriangle.nutrition.nutritionEntryId
import io.codepassion.doubletriangle.nutrition.resolveRemoteNutritionIdentities
import io.codepassion.doubletriangle.nutrition.shouldSendNutritionTombstone
import io.codepassion.doubletriangle.nutrition.shouldReuseRemoteNutritionMedia
import io.codepassion.doubletriangle.nutrition.canAcknowledgeNutritionMedia
import io.codepassion.doubletriangle.nutrition.nutritionItemBelongsToParent
import io.codepassion.doubletriangle.nutrition.itemIdsForDeletedNutritionEntries
import io.codepassion.doubletriangle.nutrition.nutritionPushDependencies
import io.codepassion.doubletriangle.nutrition.nutritionLogTimestamp
import io.codepassion.doubletriangle.nutrition.syncValue
import io.codepassion.doubletriangle.nutrition.mealTypeFromRemote
import io.codepassion.doubletriangle.nutrition.localNutritionLogId
import io.codepassion.doubletriangle.nutrition.budgetSensitivitySyncValue
import io.codepassion.doubletriangle.nutrition.cookingEffortSyncValue
import io.codepassion.doubletriangle.nutrition.dietaryStyleSyncValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
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
    private const val plansToArchiveKey = "remote_workout_plans_to_archive"
    private const val planArchiveVersionsKey = "remote_workout_archive_versions"

    internal fun queuePreviousPlanArchive(preferences: SharedPreferences, previous: WorkoutHubState, next: WorkoutHubState) {
        val previousId = previous.planId ?: preferences.getString(planKey, null)
        if (previous.planStatus != "active" || previousId.isNullOrBlank() || previousId == next.planId) return
        val pending = preferences.getStringSet(plansToArchiveKey, emptySet()).orEmpty() + previousId
        val versions = preferences.getString(planArchiveVersionsKey, "{}")?.let(::JSONObject) ?: JSONObject()
        versions.put(previousId, java.time.Instant.now().toString())
        preferences.edit().putStringSet(plansToArchiveKey, pending).putString(planArchiveVersionsKey, versions.toString()).apply()
    }
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
    private const val pendingLocationDeletionsKey = "training_location_deletions_v1"
    private const val skippedExercisesPrefs = "wildforce_skipped_exercises"
    private const val skippedExercisesKey = "entries"
    private const val coachIDsKey = "coach_ids"
    /** Same ordered resource set used by iOS's WildforceSync. */
    private val batchResources = listOf(
        "users", "user-app-settings", "subscriptions", "training-preferences", "training-locations",
        "body-metric-entries", "body-progress-photo-sessions", "check-ins", "exercise-profiles", "nutrition-profiles",
        "gym-programs", "workout-plans", "workout-days", "workout-blocks", "planned-exercises", "exercise-results",
        "coach-exercise-content", "coach-exercises", "nutrition-plans", "nutrition-days", "nutrition-meals",
        // iOS intentionally keeps nutrition-log-media out of pull batches:
        // paths are device-local and the private image endpoint is the source
        // of the binary bytes.
        "nutrition-log-entries", "nutrition-log-items",
    )
    private val historyResources = setOf("body-metric-entries", "exercise-results", "nutrition-log-entries", "nutrition-log-items")
    private const val historyDownloadedKey = "history_downloaded"

    fun isHistoryDownloaded(context: Context): Boolean {
        val state = context.getSharedPreferences(syncStatePrefs, Context.MODE_PRIVATE)
        val userId = context.getSharedPreferences(accountPrefs, Context.MODE_PRIVATE).getString("id", null) ?: return false
        return state.getBoolean("history_full_import_v2:$userId", false)
    }

    /** iOS downloads the growing history after the essential initial sync. */
    suspend fun downloadHistory(context: Context, profile: OnboardingProfile, preferences: SharedPreferences, onProgress: suspend (Float) -> Unit): Outcome = synchronizationMutex.withLock {
        withContext(Dispatchers.IO) {
            val account = context.getSharedPreferences(accountPrefs, Context.MODE_PRIVATE)
            val token = account.getString("token", null) ?: return@withContext Outcome.NoAccount
            val userId = account.getString("id", null) ?: return@withContext Outcome.NoAccount
            try {
                SyncDiagnostics.record(context, "history", "Started; pending=${RemoteSyncChangeTracker.dirty(context).sorted().joinToString()}")
                val state = context.getSharedPreferences(syncStatePrefs, Context.MODE_PRIVATE)
                if (!state.getBoolean("history_full_import_v2:$userId", false)) {
                    state.edit().apply { historyResources.forEach { remove("$pullWatermarkPrefix$it") } }.apply()
                }
                val remote = pullRemoteSnapshot(context, token, onProgress, historyResources).materialize(context)
                val dirty = RemoteSyncChangeTracker.dirty(context)
                val knownNutritionIds = knownNutritionLogIds(context, userId, remote.records("nutrition-log-entries"))
                val nutritionCanMerge = dirty.none { it in nutritionLogResources } || synchronized(NutritionStore) {
                    protectedNutritionLogIds(context, knownNutritionIds) != null
                }
                check(nutritionCanMerge) {
                    "Hay cambios pendientes de sincronizar antes de importar el historial."
                }
                if (dirty.none { it in workoutResources }) pullWorkoutPlans(context, remote, preferences, false)
                else importRemoteWorkoutHistorySnapshot(context, remote)
                pullRemoteNutrition(context, token, remote, dirty)
                synchronized(BodyMetricsStore) {
                    migrateMatchingBodyMetricVersions(context)
                    pullRemoteBodyMetrics(context, remote.records("body-metric-entries"), profile.heightCm)
                }
                remote.savePullWatermarks(context)
                context.getSharedPreferences(syncStatePrefs, Context.MODE_PRIVATE).edit()
                    .putBoolean(historyDownloadedKey, true)
                    .putBoolean("history_full_import_v2:$userId", true)
                    .putBoolean("nutrition_history_full_pull_v1:$userId", true).apply()
                SyncDiagnostics.record(context, "history", "Completed; resources=${historyResources.sorted().joinToString()}")
                Outcome.Synchronized()
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                Log.w("WorkoutRemoteSync", "History download failed", error)
                SyncDiagnostics.record(context, "history", "Failed: ${SyncDiagnostics.errorDetail(error)}")
                if (error is SubscriptionRequiredException) Outcome.SubscriptionRequired else Outcome.Failed(error.message)
            }
        }
    }

    private val pushableResources = setOf(
        "users", "user-app-settings", "training-preferences", "training-locations",
        "body-metric-entries", "body-progress-photo-sessions", "exercise-profiles", "nutrition-profiles",
        "workout-plans", "workout-days", "workout-blocks", "planned-exercises", "exercise-results",
        "nutrition-plans", "nutrition-days", "nutrition-meals", "nutrition-log-media", "nutrition-log-entries", "nutrition-log-items",
    )
    private val nutritionPlanResources = setOf("nutrition-plans", "nutrition-days", "nutrition-meals")
    private val nutritionLogResources = setOf("nutrition-log-media", "nutrition-log-entries", "nutrition-log-items")
    private val profileResources = setOf("users", "training-preferences", "training-locations", "user-app-settings")
    private val workoutResources = setOf("workout-plans", "workout-days", "workout-blocks", "planned-exercises", "exercise-results")
    private val synchronizationMutex = Mutex()
    // `synchronize` is serialized by synchronizationMutex, so this scoped
    // selection safely gives the generic push helpers iOS-style per-resource
    // change filtering without a second global pending flag.
    private var activePushResources: Set<String> = emptySet()
    // This reference is always the application context and is cleared when
    // the serialized push scope finishes; it cannot retain an Activity.
    @SuppressLint("StaticFieldLeak")
    private var activePushContext: Context? = null
    private var activeDiscardedLegacyWorkoutRows = false
    private var activeReconcileNutritionRows = false
    private val activeProjectionCache = mutableMapOf<String, Map<String, JSONObject>>()

    /** Marks only the resources changed by an Android interaction. */
    fun markResourcesDirty(context: Context, vararg resources: String) {
        RemoteSyncChangeTracker.mark(context, *resources)
    }

    /** Record location edits independently before scheduling the outbound sync. */
    fun prepareLocationProfile(context: Context, previous: OnboardingProfile, edited: OnboardingProfile): OnboardingProfile {
        val accountId = context.getSharedPreferences(accountPrefs, Context.MODE_PRIVATE).getString("id", null)
            ?: return edited
        val sync = context.getSharedPreferences(syncPrefs, Context.MODE_PRIVATE)
        val rememberedIds = sync.getString("training_location_ids", "[]")?.let(::JSONArray) ?: JSONArray()
        val cached = context.getSharedPreferences(syncStatePrefs, Context.MODE_PRIVATE)
            .getString("remote_projection:training-locations", "[]")?.let(::JSONArray) ?: JSONArray()
        val cachedById = (0 until cached.length()).mapNotNull(cached::optJSONObject).associateBy { it.optString("id").lowercase() }
        val old = previous.effectiveTrainingLocations().mapIndexed { index, location ->
            val id = location.id?.takeIf(::isUuid) ?: rememberedIds.optString(index).takeIf(::isUuid)
                ?: stableId("$accountId:location:$index")
            val row = cachedById[id.lowercase()]
            val remoteEquipment = row?.optJSONArray("equipment").storedValues(Equipment.entries) { it.storedValue }
            val remotePhoto = row?.optString("photo_path")?.takeUnless { it.isBlank() || it.equals("null", true) }
            val same = row != null && row.isNull("deleted_at") && location.name == row.optString("name") &&
                location.equipment == remoteEquipment && location.isDefault == row.optBoolean("is_default") &&
                location.displayAddress.orEmpty() == row.optString("display_address").let { if (it == "null") "" else it } &&
                location.latitude == row.optDouble("latitude", Double.NaN).takeUnless(Double::isNaN) &&
                location.longitude == row.optDouble("longitude", Double.NaN).takeUnless(Double::isNaN) &&
                location.photoPath?.substringAfterLast('/') == remotePhoto?.substringAfterLast('/')
            location.copy(id = id, updatedAt = location.updatedAt ?: row?.optString("updated_at")?.takeIf { same })
        }
        val now = java.time.Instant.now().toString()
        val stamped = stampLocationChanges(old, edited.effectiveTrainingLocations(), now)
        val deletions = sync.getString(pendingLocationDeletionsKey, "{}")?.let(::JSONObject) ?: JSONObject()
        stamped.deletedIds.forEach { deletions.put(it, now) }
        stamped.locations.forEach { location ->
            val id = location.id?.lowercase() ?: return@forEach
            val removedAt = deletions.optString(id).takeIf(String::isNotBlank) ?: return@forEach
            if (shouldApplyRemoteRecord(removedAt, location.updatedAt)) deletions.remove(id)
        }
        sync.edit().putString(pendingLocationDeletionsKey, deletions.toString()).apply()
        return edited.copy(trainingLocations = stamped.locations)
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
        // Media uploads require the entry to point at the media row first;
        // item uploads likewise require their parent entry to exist.
        return nutritionPushDependencies(selected)
    }

    private var activePushGenerations: Map<String, Long> = emptyMap()

    private fun clearDirtyResource(resource: String) {
        val context = activePushContext ?: return
        if (!RemoteSyncChangeTracker.clearIfUnchanged(context, resource, activePushGenerations[resource] ?: return)) return
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
        data class Synchronized(val importedProfile: OnboardingProfile? = null, val userDeleted: Boolean = false) : Outcome
        data object NoAccount : Outcome
        data object SubscriptionRequired : Outcome
        /** A pull can succeed before a later push fails; iOS still renders the imported data. */
        data class Failed(
            val message: String?,
            val importedRemotePlan: Boolean = false,
            val importedProfile: OnboardingProfile? = null,
        ) : Outcome
    }

    /** Mirrors iOS: duplicate the day, then pull its graph without pushing local changes. */
    suspend fun duplicateWorkoutDay(context: Context, preferences: SharedPreferences, workoutId: String, idempotencyKey: String): io.codepassion.doubletriangle.core.model.WorkoutDaySummary = synchronizationMutex.withLock {
        withContext(Dispatchers.IO) {
            val token = context.getSharedPreferences(accountPrefs, Context.MODE_PRIVATE).getString("token", null)
                ?: error("No account")
            val persistedId = if (isUuid(workoutId)) workoutId else {
                val plan = preferences.getString("workout_plan_json", null)?.let { runCatching { JSONObject(it) }.getOrNull() }
                val planId = plan?.opt("planId")?.let(WorkoutPlanGenerator::normalizeNullableString) ?: preferences.getString(planKey, null)
                val days = plan?.optJSONArray("workouts")
                val isPlanDay = days != null && (0 until days.length()).any { days.optJSONObject(it)?.optString("id") == workoutId }
                require(isPlanDay && !planId.isNullOrBlank()) { "Workout has not been synchronized" }
                stableId("$planId:$workoutId")
            }
            duplicateThenPull(idempotencyKey, create = { key ->
            val connection = (URL(WildforceApiEnvironment.apiUrl("workout/days/$persistedId/duplicate")).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 20_000
                readTimeout = 30_000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Idempotency-Key", key)
            }
            try {
                val code = connection.responseCode
                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (code !in 200..299) throw apiException(body, "Couldn't duplicate workout")
                JSONObject(body).getJSONObject("data").getString("id").also { require(isUuid(it)) }
            } finally { connection.disconnect() }
            }, pull = { duplicateId ->
            var graph: RemoteSnapshot? = null
            listOf("workout-days", "workout-blocks", "planned-exercises").forEach { resource ->
                graph = pullRemoteSnapshot(context, token, {}, setOf(resource)).materialize(context)
            }
            val days = pullRemoteWorkoutDays(requireNotNull(graph))
            val duplicate = (0 until days.length()).mapNotNull(days::optJSONObject)
                .firstOrNull { it.optString("id").equals(duplicateId, true) }
                ?: error("Duplicated workout was not returned by sync")
            // Reuse the existing graph parser without attaching this day to a real plan.
            val temporaryPlan = UUID.randomUUID().toString()
            val isolatedDay = JSONObject(duplicate.toString()).put("workout_plan_id", temporaryPlan)
            val plan = remotePlanJson(context, JSONObject(), temporaryPlan, JSONArray().put(isolatedDay))
                ?: error("Couldn't load duplicated workout")
            val parsed = WorkoutPlanGenerator.parse(plan.rawJson, "", "").workouts.single()
            val result = parsed.copy(
                dayType = WorkoutPlanGenerator.normalizeNullableString(duplicate.opt("day_type")).orEmpty(),
                createdAt = WorkoutPlanGenerator.normalizeNullableString(duplicate.opt("created_at")),
                estimatedMinutes = if (duplicate.isNull("estimated_duration_minutes")) parsed.estimatedMinutes else duplicate.optInt("estimated_duration_minutes"),
            )
            io.codepassion.doubletriangle.feature.workout.CustomWorkoutStore.save(context, result)
            // Do not advance shared cursors: unrelated rows still need their normal import.
            result
            })
        }
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

    suspend fun synchronize(
        context: Context,
        profile: OnboardingProfile,
        preferences: SharedPreferences,
        resourcesOnly: Set<String>? = null,
        hydrateMedia: Boolean = true,
        onPullProgress: suspend (Float) -> Unit = {},
    ): Outcome = synchronizationMutex.withLock {
        withContext(Dispatchers.IO) {
        val account = context.getSharedPreferences(accountPrefs, 0)
        val token = account.getString("token", null) ?: return@withContext Outcome.NoAccount
        val userId = account.getString("id", null) ?: return@withContext Outcome.NoAccount
        var importedRemotePlan = false
        var importedProfile: OnboardingProfile? = null
        val startingGenerations = pushableResources.associateWith { RemoteSyncChangeTracker.generation(context, it) }
        val startingUpdatedAt = pushableResources.associateWith { RemoteSyncChangeTracker.updatedAtMillis(context, it) }
        val latestProfile = loadPersistedOnboardingProfile(preferences) ?: profile
        val profileGenerations = profileResources.associateWith { startingGenerations.getValue(it) }
        var pendingProfileAtStart = emptySet<String>()
        var locationsReconciled = false
        fun safeImportedProfile(): OnboardingProfile? {
            val remoteProfile = importedProfile ?: return null
            if (profileResources.any { RemoteSyncChangeTracker.generation(context, it) != profileGenerations[it] }) return null
            return mergeProfileResources(latestProfile, remoteProfile, pendingProfileAtStart)
        }
        try {
            Log.d("WorkoutRemoteSync", "Starting synchronization")
            SyncDiagnostics.record(context, "sync", "Started; pending=${RemoteSyncChangeTracker.dirty(context).sorted().joinToString()}; requested=${resourcesOnly?.sorted()?.joinToString() ?: "all"}")
            // An existing account signing in on a new Android device must import
            // the server graph before it can create any local sync records. iOS's
            // store has no generated fallback graph to upload in this case.
            val importingExistingAccount = preferences.getBoolean("remote_sync_import_required", false)
            val profileMigrationKey = "exercise_profiles_uploaded_v2:$userId"
            val syncState = context.getSharedPreferences(syncStatePrefs, Context.MODE_PRIVATE)
            // A cursor without its materialized user graph cannot represent a
            // completed import (for example after an interrupted account reset).
            // Re-fetch all resources while leaving local edits and their dirty
            // markers intact; the normal conflict handling still applies.
            val usersProjection = syncState.getString("remote_projection:users", null)
                ?.let { runCatching { JSONArray(it).length() }.getOrNull() } ?: 0
            if (needsFullProjectionRepair(syncState.contains("${pullWatermarkPrefix}users"), usersProjection)) {
                SyncDiagnostics.record(context, "repair", "User projection missing; requesting complete snapshot")
                syncState.edit().apply { batchResources.forEach { remove("$pullWatermarkPrefix$it") } }.commit()
            }
            val nutritionHistoryRepairKey = "nutrition_history_full_pull_v1:$userId"
            val historyReady = isHistoryDownloaded(context)
            val needsNutritionHistoryRepair = historyReady && !syncState.getBoolean(nutritionHistoryRepairKey, false)
            if (needsNutritionHistoryRepair) {
                // Earlier versions could advance the cursor with an incomplete
                // projection. Re-fetch history once and merge it with the local
                // graph; keep retrying from the start until the pull succeeds.
                syncState.edit().remove("${pullWatermarkPrefix}nutrition-log-entries")
                    .remove("${pullWatermarkPrefix}nutrition-log-items").apply()
            }
            if (!importingExistingAccount && !syncState.getBoolean(profileMigrationKey, false) && (ExerciseProfileSync.hasLocalProfiles(context) || WorkoutHistoryStore.remoteEntries(context).isNotEmpty())
                && "exercise-profiles" !in RemoteSyncChangeTracker.dirty(context)) {
                RemoteSyncChangeTracker.mark(context, "exercise-profiles")
            }
            // Match iOS: every synchronization pulls first and then pushes local
            // changes. Keep the local projection in memory while pulling so a
            // pending Android edit is not replaced before it is uploaded.
            val shouldPullRemote = true
            val resourcesToPush = dirtyResources(context, preferences, importingExistingAccount)
                .let { dirty -> resourcesOnly?.let { allowed -> dirty.filterTo(mutableSetOf()) { it in allowed } } ?: dirty }
                .let(::nutritionPushDependencies)
                .toMutableSet()
            val pendingWorkoutChanges = !importingExistingAccount && dirtyResources(context, preferences, false).any { it in workoutResources }
            pendingProfileAtStart = if (importingExistingAccount) emptySet() else dirtyResources(context, preferences, false).intersect(profileResources)
            importedProfile = if (shouldPullRemote) {
                resetCoachContentIfCoachesChanged(context, token)
                Log.d("WorkoutRemoteSync", "Starting batch pull")
                // iOS applies each changed row into SwiftData before using the
                // graph. Materialize Android's incremental pages the same way;
                // treating a delta as a full snapshot was the root cause of
                // incomplete nutrition/profile imports after the first sync.
                val remote = pullRemoteSnapshot(context, token, onPullProgress, if (historyReady) batchResources.toSet() else batchResources.toSet() - historyResources).materialize(context)
                // ContentView on iOS uses only non-deleted User rows. A newer
                // user tombstone therefore removes the current user from the UI.
                val users = remote.records("users")
                val currentUser = (0 until users.length()).mapNotNull(users::optJSONObject)
                    .firstOrNull { it.optString("id").equals(userId, ignoreCase = true) }
                val userDirty = "users" in dirtyResources(context, preferences, importingExistingAccount)
                if (currentUser != null && !currentUser.isNull("deleted_at") &&
                    remoteDeletionWins(userDirty, startingUpdatedAt["users"], currentUser.optString("updated_at"))) {
                    val applied = withContext(Dispatchers.Main) {
                        if (RemoteSyncChangeTracker.generation(context, "users") != startingGenerations["users"]) false
                        else {
                            preferences.edit().putBoolean("remote_user_deleted", true).apply()
                            if (userDirty) RemoteSyncChangeTracker.clearIfUnchanged(context, "users", startingGenerations.getValue("users"))
                            true
                        }
                    }
                    if (applied) return@withContext Outcome.Synchronized(userDeleted = true)
                }
                // iOS compares each singleton model's updatedAt, rather than
                // making a pending edit automatically win over a newer server row.
                val remoteWinners = setOf("users", "training-preferences", "user-app-settings", "nutrition-profiles")
                    .filterTo(mutableSetOf()) { resource ->
                        resource in resourcesToPush && startingUpdatedAt[resource]?.let { millis ->
                            val records = remote.records(resource)
                            val row = records.firstLiveRecord() ?: if (resource != "users") records.latestRecord() else null
                            row != null && shouldApplyRemoteRecord(java.time.Instant.ofEpochMilli(millis).toString(), row.optString("updated_at"))
                        } == true
                    }
                resourcesToPush.removeAll(remoteWinners)
                pendingProfileAtStart = pendingProfileAtStart - remoteWinners
                val failedPullResources = mutableSetOf<String>()
                fun apply(vararg resources: String, block: () -> Unit) {
                    runCatching(block).onFailure {
                        failedPullResources += resources
                        Log.w("WorkoutRemoteSync", "Unable to import ${resources.joinToString()}", it)
                        SyncDiagnostics.record(context, "import", "Failed ${resources.joinToString()}: ${SyncDiagnostics.errorDetail(it)}")
                    }
                }
                var remoteProfile: OnboardingProfile? = null
                var resolvedLocationDeletions = emptySet<String>()
                // Custom coach exercises resolve the nullable exercise reference
                // in the workout graph, so load them before materializing plans.
                apply("coach-exercise-content") { CoachExerciseMediaStore.replace(context, remote.records("coach-exercise-content")) }
                apply("coach-exercises") { CoachExerciseMediaStore.replaceCoachExercises(context, remote.records("coach-exercises")) }
                apply("gym-programs", "workout-days", "workout-blocks", "planned-exercises") { importGymPrograms(context, remote) }
                apply("users", "training-preferences", "training-locations") {
                    remoteProfile = pullRemoteProfile(context, remote, latestProfile)
                }
                if ("training-locations" in pendingProfileAtStart && remoteProfile != null) {
                    val localLocations = latestProfile.effectiveTrainingLocations()
                    if (localLocations.all { it.id?.let(::isUuid) == true }) {
                        val sync = context.getSharedPreferences(syncPrefs, Context.MODE_PRIVATE)
                        val deletions = sync.getString(pendingLocationDeletionsKey, "{}")?.let(::JSONObject) ?: JSONObject()
                        val pending = deletions.keys().asSequence().associate { it.lowercase() to deletions.optString(it) }
                        val localIds = localLocations.mapNotNull { it.id?.lowercase() }.toSet()
                        val baseline = syncState.getString("$pushedRecordsPrefix${"training-locations"}", "{}")?.let(::JSONObject) ?: JSONObject()
                        val unknownDeletions = baseline.keys().asSequence().map(String::lowercase)
                            .filter { it !in localIds && it !in pending }.toSet()
                        val liveById = requireNotNull(remoteProfile).trainingLocations.mapNotNull { location ->
                            location.id?.lowercase()?.let { it to location }
                        }.toMap()
                        val records = remote.records("training-locations")
                        val versions = (0 until records.length()).mapNotNull { index ->
                            val row = records.optJSONObject(index) ?: return@mapNotNull null
                            val id = row.optString("id").lowercase()
                            if (id in unknownDeletions) return@mapNotNull null
                            val value = if (row.isNull("deleted_at")) liveById[id] else null
                            RemoteLocationVersion(id, row.optString("updated_at"), value, row.optInt("sort_order", index))
                        }
                        val merged = mergeLocationVersions(localLocations, versions, pending)
                        resolvedLocationDeletions = merged.resolvedDeletions
                        val equipment = merged.locations.firstOrNull { it.isDefault }?.equipment ?: setOf(Equipment.Bodyweight)
                        remoteProfile = requireNotNull(remoteProfile).copy(trainingLocations = merged.locations, availableEquipment = equipment)
                        pendingProfileAtStart = pendingProfileAtStart - "training-locations"
                        locationsReconciled = true
                    }
                }
                withContext(Dispatchers.Main) {
                    if ("users" !in resourcesToPush &&
                        RemoteSyncChangeTracker.generation(context, "users") == startingGenerations["users"]) {
                        apply("users") { pullRemoteAccountProgress(context, remote) }
                    }
                }
                apply("exercise-profiles") { pullRemoteExerciseProfiles(context, remote) }
                withContext(Dispatchers.Main) {
                    apply("workout-plans", "workout-days", "workout-blocks", "planned-exercises", "exercise-results") {
                        val graphResources = workoutResources - "exercise-results"
                        val dirtyGraph = dirtyResources(context, preferences, importingExistingAccount).intersect(graphResources)
                        val storedVersions = preferences.getString(WorkoutPlanVersions.KEY, null)?.let(::JSONObject) ?: JSONObject()
                        val hasVersionedRows = storedVersions.keys().hasNext()
                        // Older versions did not store individual row timestamps. If the entire
                        // pending graph is legacy, restore the downloaded graph rather than
                        // assigning a misleading new timestamp to the old local snapshot.
                        val canReplaceLegacy = GymProgramStore.pending(context) == null && canReplaceLegacyWorkoutGraph(
                            dirtyGraph, hasVersionedRows, startingGenerations,
                            dirtyGraph.associateWith { RemoteSyncChangeTracker.generation(context, it) },
                        )
                        val replacedLegacy = canReplaceLegacy && pullWorkoutPlans(context, remote, preferences, false)
                        if (replacedLegacy) {
                            dirtyGraph.forEach { resource ->
                                if (RemoteSyncChangeTracker.clearIfUnchanged(context, resource, startingGenerations.getValue(resource))) {
                                    resourcesToPush.remove(resource)
                                }
                            }
                            preferences.edit().remove(plansToArchiveKey).remove(planArchiveVersionsKey).apply()
                            importedRemotePlan = true
                            SyncDiagnostics.record(context, "workout", "Replaced unversioned legacy plan with downloaded plan")
                        } else {
                            val workoutIsDirty = pendingWorkoutChanges || RemoteSyncChangeTracker.dirty(context).any { it in workoutResources }
                            if (workoutIsDirty) importRemoteWorkoutHistorySnapshot(context, remote)
                            else importedRemotePlan = pullWorkoutPlans(context, remote, preferences, true)
                        }
                    }
                }
                withContext(Dispatchers.Main) {
                    if ("nutrition-profiles" !in resourcesToPush &&
                        RemoteSyncChangeTracker.generation(context, "nutrition-profiles") == startingGenerations["nutrition-profiles"]) {
                        apply("nutrition-profiles") {
                            val records = remote.records("nutrition-profiles")
                            val profileRecord = records.firstLiveRecord()
                            if (profileRecord != null) NutritionStore.importRemotePreferences(context, profileRecord)
                            else if (records.length() > 0) NutritionStore.clearRemotePreferences(context)
                        }
                    }
                }
                apply("nutrition-plans", "nutrition-days", "nutrition-meals", "nutrition-log-entries", "nutrition-log-items") {
                    pullRemoteNutrition(context, token, remote, if (historyReady) resourcesToPush else resourcesToPush + nutritionLogResources, hydrateMedia)
                }
                if (historyReady) {
                    apply("body-metric-entries") {
                        synchronized(BodyMetricsStore) {
                            migrateMatchingBodyMetricVersions(context)
                            pullRemoteBodyMetrics(context, remote.records("body-metric-entries"), (remoteProfile ?: profile).heightCm)
                        }
                    }
                }
                var importedHealthConnectEnabled: Boolean? = null
                withContext(Dispatchers.Main) {
                    if ((importingExistingAccount || "user-app-settings" !in pendingProfileAtStart) &&
                        RemoteSyncChangeTracker.generation(context, "user-app-settings") == startingGenerations["user-app-settings"]) {
                        apply("user-app-settings") {
                            importedHealthConnectEnabled = pullRemoteAppSettings(context, remote)
                        }
                    }
                }
                importedHealthConnectEnabled?.let { enabled ->
                    remoteProfile = (remoteProfile ?: latestProfile).copy(isHealthConnectEnabled = enabled)
                }
                // iOS pulls media on every synchronization. The local store
                // de-duplicates sessions, so this also restores progress photos
                // added from another device after the first Android sign-in.
                // A local capture/deletion has the same precedence as an iOS
                // SwiftData dirty object. Do not rehydrate the previous remote
                // session over it before the outbound tombstone/upload.
                if (hydrateMedia && "body-progress-photo-sessions" !in resourcesToPush) {
                    pullRemoteMedia(context, token, userId, remote.records("body-progress-photo-sessions"))
                }
                if (hydrateMedia && "training-locations" !in resourcesToPush) {
                    pullRemoteTrainingLocationPhotos(context, token, remote.records("training-locations"))
                }
                // Avatars are stored outside the sync graph on both platforms.
                // iOS resolves the public avatar path when rendering Profile;
                // hydrate it before Android renders the imported profile too.
                if (hydrateMedia) pullRemoteAvatar(context, token, userId)
                // iOS advances each pull watermark only after every record in
                // this page set has been applied locally. Do that before the
                // outbound pass: a later push failure must not re-fetch and
                // re-append an already-imported remote graph.
                remote.savePullWatermarks(context, failedPullResources)
                if (needsNutritionHistoryRepair && failedPullResources.none { it in nutritionLogResources }) {
                    syncState.edit().putBoolean(nutritionHistoryRepairKey, true).apply()
                }
                if (failedPullResources.isNotEmpty()) {
                    error("No se pudieron importar todos los datos. Vuelve a intentarlo.")
                }
                if (remoteWinners.isNotEmpty() || locationsReconciled) {
                    // Serialize persistence with UI edits. A newer generation keeps
                    // its dirty marker and wins on the next pass.
                    withContext(Dispatchers.Main) {
                        val unchanged = remoteWinners.filter { RemoteSyncChangeTracker.generation(context, it) == startingGenerations[it] }
                        if ((unchanged.any { it in profileResources } || locationsReconciled) && remoteProfile != null &&
                            profileResources.all { RemoteSyncChangeTracker.generation(context, it) == startingGenerations[it] }) {
                            persistProfilePreferences(preferences, mergeProfileResources(latestProfile, requireNotNull(remoteProfile), pendingProfileAtStart))
                        }
                        if (locationsReconciled && profileResources.all { RemoteSyncChangeTracker.generation(context, it) == startingGenerations[it] }) {
                            val sync = context.getSharedPreferences(syncPrefs, Context.MODE_PRIVATE)
                            val deletions = sync.getString(pendingLocationDeletionsKey, "{}")?.let(::JSONObject) ?: JSONObject()
                            resolvedLocationDeletions.forEach(deletions::remove)
                            sync.edit().putString(pendingLocationDeletionsKey, deletions.toString()).apply()
                        }
                        unchanged.forEach { resource ->
                            if (resource !in profileResources || profileResources.all { RemoteSyncChangeTracker.generation(context, it) == startingGenerations[it] }) {
                                RemoteSyncChangeTracker.clearIfUnchanged(context, resource, startingGenerations.getValue(resource))
                            }
                        }
                    }
                }
                remoteProfile
            } else null
            // A change made during the pull belongs to the next sync, whose
            // profile snapshot and generations will be read after taking the lock.
            val deferred = resourcesToPush.filter { RemoteSyncChangeTracker.generation(context, it) != startingGenerations[it] }
            if (deferred.isNotEmpty()) SyncDiagnostics.record(context, "push", "Deferred because changes occurred during download: ${deferred.sorted().joinToString()}")
            resourcesToPush.removeAll(deferred.toSet())
            // A gym graph must be sent as one unit. Do not acknowledge a
            // subset when another workout resource changed during the pull.
            if (GymProgramStore.pending(context) != null && !resourcesToPush.containsAll(setOf("workout-days", "workout-blocks", "planned-exercises", "exercise-results"))) {
                resourcesToPush.removeAll(workoutResources)
            }
            val shouldPushLocal = resourcesToPush.isNotEmpty()
            val pushGenerations = resourcesToPush.associateWith { startingGenerations.getValue(it) }
            val profileToSync = if (shouldPushLocal && importedProfile != null)
                mergeProfileResources(latestProfile, requireNotNull(importedProfile), pendingProfileAtStart)
            else if (shouldPushLocal) latestProfile else importedProfile ?: latestProfile
            if (shouldPushLocal) {
            activePushResources = resourcesToPush
            activePushGenerations = pushGenerations
            activePushContext = context.applicationContext
            activeProjectionCache.clear()
            activeDiscardedLegacyWorkoutRows = false
            activeReconcileNutritionRows = false
            val now = java.time.Instant.now().toString()
            fun modifiedAt(resource: String): String = startingUpdatedAt[resource]?.let { java.time.Instant.ofEpochMilli(it).toString() } ?: now
            val progress = accountProgress(context)
            // User is a first-class iOS sync resource. Keeping it here ensures
            // profile edits made on Android are visible to the other platform.
            push(token, "users", JSONArray().put(recordFromRemoteProjection(context, "users", userId, modifiedAt("users"))
                .put("name", profileToSync.name).put("height", profileToSync.heightCm).put("weight", profileToSync.weightKg)
                .put("birth_date", "%04d-%02d-%02d".format(profileToSync.birthYear, profileToSync.birthMonth, profileToSync.birthDay.coerceIn(1, java.time.YearMonth.of(profileToSync.birthYear.coerceIn(1920, 9999), profileToSync.birthMonth.coerceIn(1, 12)).lengthOfMonth())))
                .put("gender", profileToSync.gender.storedValue).put("language", AppLocale.languageTag(profileToSync.appLanguage))
                .put("metric_system", profileToSync.metricSystem.storedValue)
                .put("current_streak", progress.currentStreak).put("longest_streak", progress.longestStreak)
                .put("last_completed_workout_at", progress.lastCompletedWorkoutAt ?: JSONObject.NULL)
                .put("xp", progress.xp).put("xp_level", progress.xpLevel)))
            val detailPrefs = context.getSharedPreferences("wildforce_profile_details", Context.MODE_PRIVATE)
            val appSettingsId = remoteProjectionID(context, "user-app-settings", stableId("$userId:settings"))
            push(token, "user-app-settings", JSONArray().put(recordFromRemoteProjection(context, "user-app-settings", appSettingsId, modifiedAt("user-app-settings"))
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
            val trainingPreferencesId = remoteProjectionID(context, "training-preferences", stableId("$userId:training-preferences"))
            push(token, "training-preferences", JSONArray().put(recordFromRemoteProjection(context, "training-preferences", trainingPreferencesId, modifiedAt("training-preferences"))
                .put("goal", profileToSync.goal.storedValue).put("lifestyle", profileToSync.lifestyle.storedValue)
                .put("gym_type", profileToSync.gymType.storedValue).put("general_training_level", profileToSync.trainingLevel.storedValue)
                .put("training_split_preference", profileToSync.trainingSplitPreference.storedValue)
                .put("preferred_workout_duration_minutes", profileToSync.preferredWorkoutDurationMinutes)
                .put("workout_planner_notes", profileToSync.workoutPlannerNotes)
                .put("workout_days", JSONArray(profileToSync.workoutDays.map { it.storedValue }))
                .put("custom_workout_focuses", JSONObject(profileToSync.activeCustomWorkoutFocuses.mapKeys { it.key.storedValue }.mapValues { it.value.storedValue }))
                .put("movement_restrictions", JSONArray(profileToSync.movementRestrictions.map { it.storedValue }))
                .put("body_composition_phase", profileToSync.bodyCompositionPhase?.storedValue ?: JSONObject.NULL)
                .put("skips_warmups", profileToSync.skipsWarmups).put("skips_cooldowns", profileToSync.skipsCooldowns).put("skips_rest_periods", profileToSync.skipsRestPeriods)))
            val remoteLocationIds = context.getSharedPreferences("wildforce_remote_sync", Context.MODE_PRIVATE)
                .getString("training_location_ids", "[]")?.let(::JSONArray) ?: JSONArray()
            val locationPhotoUploads = mutableListOf<Pair<String, File>>()
            val locationState = context.getSharedPreferences(syncPrefs, Context.MODE_PRIVATE)
            val locationDeletions = locationState.getString(pendingLocationDeletionsKey, "{}")?.let(::JSONObject) ?: JSONObject()
            val locationProjection = syncState.getString("remote_projection:training-locations", "[]")?.let(::JSONArray) ?: JSONArray()
            val cachedLocationIds = (0 until locationProjection.length()).mapNotNull { locationProjection.optJSONObject(it)?.optString("id")?.lowercase() }.toSet()
            val legacyLocationIds = mutableSetOf<String>()
            val locationRecords = JSONArray().apply {
                // An empty explicit list still represents the selected gym and
                // equipment. Persist that effective default as a first-class
                // location so iOS and the backend receive the same context.
                val locationsToPush = if (locationsReconciled) profileToSync.trainingLocations else profileToSync.effectiveTrainingLocations()
                locationsToPush.forEachIndexed { index, location ->
                    val id = location.id?.takeIf(::isUuid) ?: remoteLocationIds.optString(index).takeIf(::isUuid) ?: stableId("$userId:location:$index")
                    val updatedAt = location.updatedAt?.takeIf { runCatching { java.time.Instant.parse(it) }.isSuccess }
                        ?: startingUpdatedAt["training-locations"]?.takeIf { id.lowercase() !in cachedLocationIds }
                            ?.let { java.time.Instant.ofEpochMilli(it).toString() }
                    if (updatedAt == null) { legacyLocationIds += id.lowercase(); return@forEachIndexed }
                    val record = recordFromRemoteProjection(context, "training-locations", id, updatedAt)
                        .put("is_default", location.isDefault).put("sort_order", index)
                    if (location.gymId == null) {
                        record.put("name", location.name).put("equipment", JSONArray(location.equipment.map { it.storedValue }))
                            .put("display_address", location.displayAddress ?: JSONObject.NULL)
                            .put("latitude", location.latitude ?: JSONObject.NULL).put("longitude", location.longitude ?: JSONObject.NULL)
                            .put("photo_path", location.photoPath?.substringAfterLast('/') ?: JSONObject.NULL)
                        TrainingLocationPhotoStore.file(context, location.photoPath)?.takeIf(File::isFile)
                            ?.let { locationPhotoUploads += id to it }
                    }
                    put(record)
                }
            }
            val previousLocations = syncState.getString("$pushedRecordsPrefix${"training-locations"}", "{}")?.let(::JSONObject) ?: JSONObject()
            val currentLocationIds = (0 until locationRecords.length()).mapNotNull { locationRecords.optJSONObject(it)?.optString("id")?.lowercase() }.toSet()
            val preserveLocations = previousLocations.keys().asSequence().filter { id ->
                id.lowercase() in legacyLocationIds || (id.lowercase() !in currentLocationIds && !locationDeletions.has(id.lowercase()))
            }.toSet()
            push(token, "training-locations", locationRecords, preserveMissingIds = preserveLocations,
                clearOnSuccess = legacyLocationIds.isEmpty() && preserveLocations.isEmpty())
            synchronizeTrainingLocationPhotos(context, token, locationPhotoUploads)
            val bodyMetrics = context.getSharedPreferences("wildforce_body_metrics", Context.MODE_PRIVATE).getString("history", "[]") ?: "[]"
            val metricRecords = JSONArray(); val metrics = JSONArray(bodyMetrics)
            val legacyMetricIds = mutableSetOf<String>()
            fun addMetric(metric: JSONObject, type: String, value: Double, range: ClosedFloatingPointRange<Double>) {
                val date = metric.optString("date")
                if (date.isBlank() || !value.isFinite() || value !in range) return
                val storedId = metric.optString("id").takeIf(::isUuid)
                val recordedAt = metric.optString("recordedAt").takeIf { runCatching { java.time.Instant.parse(it) }.isSuccess }
                    ?: "${date}T12:00:00Z"
                val createdAt = metric.optString("createdAt").takeIf { runCatching { java.time.Instant.parse(it) }.isSuccess } ?: recordedAt
                val recordId = storedId ?: stableId("$userId:$type:$date")
                val updatedAt = metric.optString("updatedAt").takeIf { runCatching { java.time.Instant.parse(it) }.isSuccess }
                if (updatedAt == null) { legacyMetricIds += recordId.lowercase(); return }
                metricRecords.put(record(recordId, updatedAt)
                    .put("created_at", createdAt)
                    .put("type", type).put("value", value).put("recorded_at", recordedAt)
                    .put("source", metric.optString("source", "manual").let { if (it == "healthConnect") "manual" else it })
                    .apply { metric.optString("checkInId").takeIf(::isUuid)?.let { put("check_in_id", it) } })
            }
            for (index in 0 until metrics.length()) {
                val metric = metrics.optJSONObject(index) ?: continue
                addMetric(metric, "weight", metric.optDouble("weightKg", Double.NaN), Double.MIN_VALUE..Double.MAX_VALUE)
                addMetric(metric, "leanBodyMass", metric.optionalDouble("leanBodyMassKg"), Double.MIN_VALUE..Double.MAX_VALUE)
                addMetric(metric, "bodyFatPercentage", metric.optionalDouble("bodyFatPercentage"), Double.MIN_VALUE..Double.MAX_VALUE)
                addMetric(metric, "waistCircumference", metric.optionalDouble("waistCm"), Double.MIN_VALUE..Double.MAX_VALUE)
                addMetric(metric, "neckCircumference", metric.optionalDouble("neckCm"), Double.MIN_VALUE..Double.MAX_VALUE)
                addMetric(metric, "hipCircumference", metric.optionalDouble("hipCm"), Double.MIN_VALUE..Double.MAX_VALUE)
                addMetric(metric, "chestCircumference", metric.optionalDouble("chestCm"), Double.MIN_VALUE..Double.MAX_VALUE)
                addMetric(metric, "thighCircumference", metric.optionalDouble("thighCm"), Double.MIN_VALUE..Double.MAX_VALUE)
                addMetric(metric, "armCircumference", metric.optionalDouble("armCm"), Double.MIN_VALUE..Double.MAX_VALUE)
            }
            val metricBaseline = syncState.getString("$pushedRecordsPrefix${"body-metric-entries"}", "{}")?.let(::JSONObject) ?: JSONObject()
            val metricDeletes = context.getSharedPreferences("wildforce_body_metrics", Context.MODE_PRIVATE)
                .getString(BodyMetricsStore.PENDING_DELETIONS_KEY, "{}")?.let(::JSONObject) ?: JSONObject()
            val currentMetricIds = (0 until metricRecords.length()).mapNotNull { metricRecords.optJSONObject(it)?.optString("id")?.lowercase() }.toSet()
            val protectedMetricIds = metricBaseline.keys().asSequence().filter { id ->
                id.lowercase() in legacyMetricIds || (id.lowercase() !in currentMetricIds && !metricDeletes.has(id.lowercase()))
            }.toSet()
            push(token, "body-metric-entries", metricRecords, preserveMissingIds = protectedMetricIds,
                clearOnSuccess = legacyMetricIds.isEmpty() && protectedMetricIds.isEmpty())
            synchronizeAvatar(context, token)
            synchronizeBodyProgressPhotos(context, token, userId)

            if ("exercise-profiles" in activePushResources) {
                val remoteProfiles = context.getSharedPreferences(syncPrefs, Context.MODE_PRIVATE)
                    .getString(exerciseProfilesKey, "[]")?.let(::JSONArray) ?: JSONArray()
                val profileRecords = ExerciseProfileSync.records(context, userId, remoteProfiles, profileToSync.trainingLevel.storedValue)
                val records = profileRecords.values
                for (start in 0 until records.length() step 100) {
                    val batch = JSONArray()
                    for (index in start until minOf(start + 100, records.length())) batch.put(records.get(index))
                    pushBatch(token, "exercise-profiles", batch)
                }
                val accepted = JSONObject()
                for (index in 0 until records.length()) {
                    val record = records.getJSONObject(index)
                    accepted.put(record.getString("id"), record)
                }
                updateRemoteProjectionAfterPush("exercise-profiles", accepted, JSONObject(), now)
                check(profileRecords.unresolved == 0) { "Hay perfiles locales con ejercicios que no se han podido identificar." }
                syncState.edit().putBoolean(profileMigrationKey, true).apply()
                clearDirtyResource("exercise-profiles")
            }
            synchronizeNutrition(token, userId, profileToSync, now, context, modifiedAt("nutrition-profiles"), profileOnly = true)
            var gymUploaded = false
            GymProgramStore.pending(context)?.takeIf { activePushResources.containsAll(setOf("workout-days", "workout-blocks", "planned-exercises", "exercise-results")) }?.let { (root, versions) ->
                synchronizeWorkoutPlan(token, userId, profileToSync, now, context, preferences, JSONObject(root), gymVersions = versions)
                GymProgramStore.acknowledge(context, root, versions)
                gymUploaded = true
            }
            val personalPlan = preferences.getString("workout_plan_json", null)
            personalPlan?.let { rawPlan ->
                synchronizeWorkoutPlan(token, userId, profileToSync, now, context, preferences, JSONObject(rawPlan))
            }
            if (gymUploaded && personalPlan == null) {
                listOf("workout-days", "workout-blocks", "planned-exercises", "exercise-results").forEach(::clearDirtyResource)
            }
            if (gymUploaded) {
                val refreshed = pullRemoteSnapshot(context, token, {}, setOf("gym-programs", "workout-days", "workout-blocks", "planned-exercises", "exercise-results")).materialize(context)
                importGymPrograms(context, refreshed)
                refreshed.savePullWatermarks(context)
            }
            if ("exercise-results" in resourcesToPush) {
                try {
                    synchronizeEditedExerciseResults(context, token, now)
                    if (WorkoutHistoryStore.remoteEntries(context).any { it.pendingEditAt > 0 }) {
                        RemoteSyncChangeTracker.mark(context, "exercise-results")
                    }
                } catch (error: Exception) {
                    RemoteSyncChangeTracker.mark(context, "exercise-results")
                    throw error
                }
            }
            synchronizeNutrition(token, userId, profileToSync, now, context, modifiedAt("nutrition-profiles"), profileOnly = false)
            // Import rows that arrived while a local plan change was pending,
            // once the accepted local rows have joined the cached projection.
            if (resourcesToPush.any { it in nutritionPlanResources }) activeReconcileNutritionRows = true
            if (activeDiscardedLegacyWorkoutRows) {
                withContext(Dispatchers.Main) {
                    val graphResources = workoutResources - "exercise-results"
                    if (RemoteSyncChangeTracker.dirty(context).none { it in graphResources } &&
                        graphResources.all { RemoteSyncChangeTracker.generation(context, it) == startingGenerations[it] }) {
                        val cached = RemoteSnapshot(emptyMap(), null, emptySet()).materialize(context)
                        if (!pullWorkoutPlans(context, cached, preferences, false)) {
                            RemoteSyncChangeTracker.mark(context, *graphResources.toTypedArray())
                            error("No se pudo restaurar el plan remoto después de migrar cambios antiguos.")
                        }
                        importedRemotePlan = true
                    }
                }
            }
            if (activeReconcileNutritionRows) {
                withContext(Dispatchers.Main) {
                    if (RemoteSyncChangeTracker.dirty(context).none { it in nutritionPlanResources } &&
                        nutritionPlanResources.all { RemoteSyncChangeTracker.generation(context, it) == startingGenerations[it] }) {
                        val cached = RemoteSnapshot(emptyMap(), null, emptySet()).materialize(context)
                        pullRemoteNutrition(context, token, cached, RemoteSyncChangeTracker.dirty(context), hydrateMedia = false)
                    }
                }
            }
            activeDiscardedLegacyWorkoutRows = false
            activeReconcileNutritionRows = false
            activePushResources = emptySet()
            activePushGenerations = emptyMap()
            activePushContext = null
            activeProjectionCache.clear()
            }
            if (resourcesOnly == null) {
                synchronizeCheckInMetrics(context, token, userId)
                synchronizeCheckInPhotos(context, token, userId)
                synchronizeCheckInDrafts(context, token, userId)
            }
            if (importingExistingAccount) preferences.edit().remove("remote_sync_import_required").apply()
            SyncDiagnostics.record(context, "sync", "Sync pass finished; still pending=${RemoteSyncChangeTracker.dirty(context).sorted().joinToString()}")
            Outcome.Synchronized(safeImportedProfile())
        } catch (error: Exception) {
            activeDiscardedLegacyWorkoutRows = false
            activeReconcileNutritionRows = false
            activePushResources = emptySet()
            activePushGenerations = emptyMap()
            activePushContext = null
            activeProjectionCache.clear()
            if (error is CancellationException) throw error
            Log.w("WorkoutRemoteSync", "Synchronization failed", error)
            SyncDiagnostics.record(context, "sync", "Failed: ${SyncDiagnostics.errorDetail(error)}; pending=${RemoteSyncChangeTracker.dirty(context).sorted().joinToString()}")
            if (error is SubscriptionRequiredException) Outcome.SubscriptionRequired
            else Outcome.Failed(error.message, importedRemotePlan, safeImportedProfile())
        }
        }
    }

    private fun synchronizeCheckInMetrics(context: Context, token: String, userId: String) {
        for (row in CheckInAssets.metrics(context, userId).filter { it.optBoolean("pending") }) {
            checkCheckInAssetMaySync(context, row.getString("check_in_id"), row.getString("updated_at"))
            val payload = JSONObject(row.toString()).apply { remove("version"); remove("pending") }
            val returned = pushBatchWithResponse(token, "body-metric-entries", JSONArray().put(payload))
                .optJSONArray("data")?.optJSONObject(0) ?: error("No se confirmó la medida del check-in.")
            check(returned.optString("id").equals(payload.optString("id"), true) && returned.optString("check_in_id").equals(payload.optString("check_in_id"), true) &&
                returned.optString("type") == payload.optString("type") && returned.optDouble("value") == payload.optDouble("value") &&
                returned.isNull("deleted_at") == payload.isNull("deleted_at") &&
                java.time.Instant.parse(returned.getString("updated_at")) == java.time.Instant.parse(payload.getString("updated_at"))) {
                "La medida cambió en otro dispositivo."
            }
            CheckInAssets.acknowledge(context, userId, row)
        }
    }

    private fun synchronizeCheckInPhotos(context: Context, token: String, userId: String) {
        for (row in CheckInAssets.photos(context, userId).filter { it.optBoolean("pending") }) {
            checkCheckInAssetMaySync(context, row.getString("check_in_id"), row.getString("updated_at"))
            val payload = JSONObject().apply {
                listOf("id", "created_at", "updated_at", "deleted_at", "check_in_id").forEach { put(it, row.opt(it)) }
            }
            val returned = pushBatchWithResponse(token, "body-progress-photo-sessions", JSONArray().put(payload))
                .optJSONArray("data")?.optJSONObject(0) ?: error("No se confirmó la sesión de fotos.")
            check(returned.optString("id").equals(payload.optString("id"), true) && returned.isNull("deleted_at") &&
                returned.optString("check_in_id").equals(payload.optString("check_in_id"), true)) {
                "Las fotos cambiaron en otro dispositivo."
            }
            for (angle in listOf("front", "profile", "back")) {
                val path = row.optString(angle).takeIf { it.isNotBlank() } ?: continue
                val file = File(path)
                check(file.isFile) { "Falta una foto del check-in." }
                uploadImage(token, WildforceApiEnvironment.apiUrl("body-progress-photo-sessions/${row.getString("id")}/photos/$angle"), file, "image/jpeg")
            }
            CheckInAssets.acknowledgePhoto(context, userId, row)
        }
    }

    private fun checkCheckInAssetMaySync(context: Context, id: String, updatedAt: String) {
        val state = context.getSharedPreferences(syncStatePrefs, Context.MODE_PRIVATE)
        val records = JSONArray(state.getString("remote_projection:check-ins", "[]") ?: "[]")
        val record = (0 until records.length()).mapNotNull { records.optJSONObject(it) }
            .firstOrNull { it.optString("id").equals(id, true) }
        val submittedAt = record?.optString("submitted_at")?.takeUnless { it.isBlank() || it == "null" }
            ?.let { java.time.Instant.parse(it) }
        check(record != null && (record.optString("status") != "submitted" || submittedAt != null) &&
            maySyncCheckInAsset(!record.isNull("deleted_at"), submittedAt, java.time.Instant.parse(updatedAt))) {
            "El check-in ya no se puede editar; se conservarán los cambios locales."
        }
    }

    /** Upload check-ins after body metrics and photo sessions, never discarding a newer local edit. */
    private fun synchronizeCheckInDrafts(context: Context, token: String, userId: String) {
        val state = context.getSharedPreferences(syncStatePrefs, Context.MODE_PRIVATE)
        for (item in CheckInDraftStore.pending(context, userId)) {
            val payload = item.getJSONObject("payload")
            val id = payload.getString("id")
            val cached = JSONArray(state.getString("remote_projection:check-ins", "[]") ?: "[]")
            val server = (0 until cached.length()).mapNotNull { cached.optJSONObject(it) }
                .firstOrNull { it.optString("id").equals(id, ignoreCase = true) }
                ?: error("Check-in no disponible; se conservará el borrador.")
            if (server.optString("status") == "submitted" && payload.optString("status") == "submitted" &&
                checkInJsonEquivalent(server.opt("answers"), payload.opt("answers"))) {
                CheckInDraftStore.acknowledge(context, userId, id, item.getString("version"))
                continue
            }
            check(server.isNull("deleted_at") && server.optString("status") != "submitted") {
                "El check-in ya no se puede editar; se conservará el borrador."
            }
            val response = pushBatchWithResponse(token, "check-ins", JSONArray().put(payload))
            val returned = response.optJSONArray("data")?.optJSONObject(0)
                ?: error("No se recibió la confirmación del check-in.")
            check(checkInWriteAcknowledged(id, returned.optString("id"),
                java.time.Instant.parse(payload.getString("updated_at")), java.time.Instant.parse(returned.getString("updated_at")),
                returned.optString("status") == payload.optString("status") && checkInJsonEquivalent(returned.opt("answers"), payload.opt("answers")))) {
                "El check-in cambió en otro dispositivo; se conservará el borrador."
            }
            val updated = JSONArray().apply {
                for (index in 0 until cached.length()) {
                    val row = cached.optJSONObject(index) ?: continue
                    put(if (row.optString("id").equals(id, ignoreCase = true)) returned else row)
                }
            }
            check(state.edit().putString("remote_projection:check-ins", updated.toString()).commit())
            CheckInDraftStore.acknowledge(context, userId, id, item.getString("version"))
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
                weightKg = user.optDouble("weight", user.optDouble("weight_kg", local.weightKg)).takeIf { it.isFinite() && it > 0.0 } ?: local.weightKg,
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
        val preferenceRecords = remote.records("training-preferences")
        val preferences = preferenceRecords.firstLiveRecord()
        val locationRecords = remote.records("training-locations")
        val remoteLocations = locationRecords.liveRecords()
            .sortedBy { it.optInt("sort_order", Int.MAX_VALUE) }
        val locations = remoteLocations.mapIndexed { index, location ->
                TrainingLocationProfile(
                    name = location.optString("name").trim().ifBlank { "Ubicación ${index + 1}" },
                    equipment = resolveRemoteLocationEquipment(
                        location.optJSONArray("equipment")?.storedValues(Equipment.entries) { it.storedValue },
                        userProfile.availableEquipment,
                    ),
                    isDefault = location.optBoolean("is_default", index == 0),
                    displayAddress = location.optString("display_address").takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) },
                    latitude = location.optDouble("latitude").takeUnless { it.isNaN() }?.takeIf { it in -90.0..90.0 },
                    longitude = location.optDouble("longitude").takeUnless { it.isNaN() }?.takeIf { it in -180.0..180.0 },
                    photoPath = location.optString("photo_path").takeIf(String::isNotBlank),
                    id = location.optString("id").takeIf(::isUuid),
                    gymId = location.optString("gym_id").takeIf(::isUuid),
                    updatedAt = location.optString("updated_at").takeIf(String::isNotBlank),
                )
            }
        val resolvedLocations = if (locationRecords.length() > 0) locations else userProfile.trainingLocations
        val resolvedEquipment = locations.firstOrNull { it.isDefault }?.equipment
            ?: if (locationRecords.length() > 0 && locations.isEmpty()) setOf(Equipment.Bodyweight) else userProfile.availableEquipment
        val locationIds = remoteLocations.map { it.optString("id") }
        if (locationIds.isNotEmpty()) {
            context.getSharedPreferences("wildforce_remote_sync", Context.MODE_PRIVATE).edit()
                .putString("training_location_ids", JSONArray(locationIds).toString()).apply()
        }
        if (preferences == null) {
            if (remoteUser == null && locations.isEmpty()) return null
            val withoutDeletedPreferences = if (preferenceRecords.length() > 0) userProfile.copy(
                goal = io.codepassion.doubletriangle.feature.onboarding.FitnessGoal.GeneralFitness,
                lifestyle = LifestyleLevel.Sedentary,
                gymType = GymType.SmallGym,
                trainingLevel = TrainingLevel.Beginner,
                trainingSplitPreference = TrainingSplitPreference.Automatic,
                preferredWorkoutDurationMinutes = 50,
                workoutPlannerNotes = "",
                workoutDays = setOf(WorkoutWeekday.Monday, WorkoutWeekday.Wednesday, WorkoutWeekday.Friday),
                customWorkoutFocuses = emptyMap(), movementRestrictions = emptySet(), bodyCompositionPhase = null,
                skipsWarmups = false, skipsCooldowns = false, skipsRestPeriods = false,
            ) else userProfile
            return withoutDeletedPreferences.copy(
                trainingLocations = resolvedLocations,
                availableEquipment = resolvedEquipment,
            )
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
            trainingLocations = resolvedLocations,
            availableEquipment = resolvedEquipment,
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
        io.codepassion.doubletriangle.feature.workout.CompletionProgressStore.applyAccountProgress(
            context, user.optInt("xp", 0), user.optInt("xp_level", 1),
            user.optInt("current_streak", 0), user.optInt("longest_streak", 0),
            user.optString("last_completed_workout_at").takeUnless { it.isBlank() || it.equals("null", true) },
        )
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
        response.optJSONArray("data")?.toString()?.let { coaches ->
            if (preferences.getString("coach_summaries", null) != coaches) preferences.edit().putString("coach_summaries", coaches).apply()
        }
        val previous = preferences.getStringSet(coachIDsKey, null)?.sorted()
        val projectionRepairKey = "coach_content_projection_repaired_v1"
        if (previous == ids && preferences.getBoolean(projectionRepairKey, false)) return

        CoachExerciseMediaStore.replace(context, JSONArray())
        context.getSharedPreferences(syncStatePrefs, Context.MODE_PRIVATE).edit()
            .remove("${pullWatermarkPrefix}coach-exercise-content")
            .remove("remote_projection:coach-exercise-content")
            // Keep custom exercises for past workouts, but force a full pull so
            // a newly assigned coach's older library is available immediately.
            .remove("${pullWatermarkPrefix}coach-exercises").apply()
        preferences.edit().putStringSet(coachIDsKey, ids.toSet()).putBoolean(projectionRepairKey, true).apply()
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

    /**
     * Mirrors iOS's per-session XP award, including the plan and mesocycle
     * milestones when the caller supplies them. Streaks still advance at most
     * once per local day, but every distinct completed session earns its XP.
     */
    fun recordWorkoutCompletion(context: Context, completedAt: java.time.Instant = java.time.Instant.now()) {
        // The completion screen already committed these exact values. Upload the
        // same snapshot the user sees, rather than maintaining a second counter.
        val progress = io.codepassion.doubletriangle.feature.workout.CompletionProgressStore.progress(context)
        context.getSharedPreferences(accountProgressPrefs, Context.MODE_PRIVATE).edit()
            .putInt("current_streak", progress.currentStreak)
            .putInt("longest_streak", progress.longestStreak)
            .putString("last_completed_workout_at", completedAt.toString())
            .putInt("xp", progress.xp).putInt("xp_level", progress.level).apply()
    }

    /** Imports the iOS nutrition graph so a new Android device does not upload an empty plan over it. */
    private fun pullRemoteNutrition(
        context: Context,
        token: String,
        remote: RemoteSnapshot,
        pendingLocalResources: Set<String>,
        hydrateMedia: Boolean = true,
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
        val entries = remote.records("nutrition-log-entries")
        val items = remote.records("nutrition-log-items")
        val knownLocalIds = knownNutritionLogIds(context, context.getSharedPreferences(accountPrefs, Context.MODE_PRIVATE).getString("id", "").orEmpty(), entries)
        synchronized(NutritionStore) {
            val hasPendingLogs = pendingLocalResources.any { it in nutritionLogResources }
            val protectedIds = (if (hasPendingLogs) protectedNutritionLogIds(context, knownLocalIds) else emptySet())?.toMutableSet()
            if (protectedIds != null) {
                val localState = context.getSharedPreferences("wildforce_nutrition", Context.MODE_PRIVATE)
                val deletions = localState.getString(NutritionStore.PENDING_DELETIONS_KEY, "{}")?.let(::JSONObject) ?: JSONObject()
                for (index in 0 until entries.length()) {
                    val entry = entries.optJSONObject(index) ?: continue
                    val id = entry.optString("id").lowercase()
                    val deletedAt = deletions.optString(id).takeIf(String::isNotBlank) ?: continue
                    if (shouldApplyRemoteRecord(deletedAt, entry.optString("updated_at"))) {
                        // iOS deletes the parent, retaining its item records. A newer
                        // remote parent therefore restores the entry and its items.
                        deletions.remove(id)
                        protectedIds.remove(id)
                    }
                }
                localState.edit().putString(NutritionStore.PENDING_DELETIONS_KEY, deletions.toString()).apply()
                NutritionStore.importRemoteLogs(context, entries, items, remoteNutritionMealKeys(days, mealsByDay), knownLocalIds, protectedIds)
                // Include remote parents and tombstones in the accepted baseline,
                // including locally protected rows that may need an outbound edit.
                rememberImportedPushBaseline(context, "nutrition-log-entries", entries)
                rememberImportedPushBaseline(context, "nutrition-log-items", items)
            }
        }
        rememberRemoteNutritionIDs(context, entries, items, knownLocalIds)
        if (hydrateMedia && pendingLocalResources.none { it in nutritionLogResources }) pullRemoteNutritionMedia(context, token, entries, knownLocalIds)
    }

    /** Identify locally changed logs; unrelated remote meals can still be imported. */
    private fun protectedNutritionLogIds(context: Context, knownLocalIds: Map<String, Long>): Set<String>? {
        val userId = context.getSharedPreferences(accountPrefs, Context.MODE_PRIVATE).getString("id", null) ?: return null
        NutritionStore.migrateMatchingRemoteVersions(context, knownLocalIds)
        val knownByLocal = knownLocalIds.entries.associate { it.value to it.key.lowercase() }
        val meals = NutritionStore.allForSync(context)
        val local = meals.associate { meal ->
            (knownByLocal[meal.id] ?: nutritionEntryId(userId, meal.id)).lowercase() to meal.updatedAt?.toString()
        }
        val state = context.getSharedPreferences(syncStatePrefs, Context.MODE_PRIVATE)
        val baseline = state.getString("$pushedRecordsPrefix${"nutrition-log-entries"}", "{}")?.let(::JSONObject) ?: JSONObject()
        val baselineRows = baseline.keys().asSequence().associate { it.lowercase() to baseline.optJSONObject(it) }
        val pendingJson = context.getSharedPreferences("wildforce_nutrition", Context.MODE_PRIVATE)
            .getString(NutritionStore.PENDING_DELETIONS_KEY, "{}")?.let(::JSONObject) ?: JSONObject()
        val pending = pendingJson.keys().asSequence().associate { it.lowercase() to pendingJson.optString(it).takeIf(String::isNotBlank) }
        val itemIds = context.getSharedPreferences(syncPrefs, Context.MODE_PRIVATE)
            .getString(nutritionItemIDsKey, "{}")?.let(::JSONObject) ?: JSONObject()
        return buildSet {
            addAll(pending.keys)
            // Old releases did not date deletions. Preserve only those unknown
            // rows; other entries can still reconcile independently.
            addAll(baselineRows.keys.filter { it !in local && it !in pending })
            meals.forEach { meal ->
                val id = (knownByLocal[meal.id] ?: nutritionEntryId(userId, meal.id)).lowercase()
                if (meal.updatedAt == null || meal.items.isEmpty() || meal.items.withIndex().any { (index, food) ->
                    val itemId = food.remoteId ?: itemIds.optString("${meal.id}:$index")
                    !isUuid(itemId) || food.updatedAt == null
                }) add(id)
            }
        }
    }

    private fun rememberImportedPushBaseline(context: Context, resource: String, records: JSONArray) {
        val baseline = JSONObject()
        records.liveRecords().forEach { record ->
            record.optString("id").takeIf(::isUuid)?.let { baseline.put(it, record) }
        }
        context.getSharedPreferences(syncStatePrefs, Context.MODE_PRIVATE).edit()
            .putString("$pushedRecordsPrefix$resource", baseline.toString())
            .apply()
    }

    /** iOS keeps nutrition images private and fetches them separately from the graph snapshot. */
    private fun pullRemoteNutritionMedia(context: Context, token: String, entries: JSONArray, knownLocalIds: Map<String, Long>) {
        val directory = File(context.filesDir, "NutritionLogMedia").apply { mkdirs() }
        entries.liveRecords().forEach { entry ->
            val entryId = entry.optString("id").takeIf(::isUuid) ?: return@forEach
            // The image endpoint can only serve an entry with a media row.
            // Avoid one network request per text-only meal during history import.
            if (!isUuid(entry.optString("nutrition_log_media_id"))) return@forEach
            val image = File(directory, "$entryId.jpg")
            if (image.isFile && image.length() in 1..(5 * 1024 * 1024)) {
                NutritionStore.attachRemotePhoto(context, entryId, image.absolutePath, knownLocalIds)
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
                        NutritionStore.attachRemotePhoto(context, entryId, image.absolutePath, knownLocalIds)
                        SyncDiagnostics.record(context, "media", "Nutrition image download completed")
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
    private fun knownNutritionLogIds(context: Context, userId: String, entries: JSONArray): Map<String, Long> {
        val stored = context.getSharedPreferences(syncPrefs, Context.MODE_PRIVATE)
            .getString(nutritionEntryIDsKey, "{}")?.let(::JSONObject) ?: JSONObject()
        val storedByLocal = stored.keys().asSequence().mapNotNull { local ->
            val id = local.toLongOrNull() ?: return@mapNotNull null
            stored.optString(local).takeIf(::isUuid)?.let { id to it }
        }.toMap()
        val remoteIds = (0 until entries.length()).mapNotNull { entries.optJSONObject(it)?.optString("id")?.takeIf(::isUuid) }
        return resolveRemoteNutritionIdentities(userId, NutritionStore.allForSync(context).associate { it.id to it.source }, storedByLocal, remoteIds)
    }

    private fun rememberRemoteNutritionIDs(context: Context, entries: JSONArray, items: JSONArray, knownLocalIds: Map<String, Long>) {
        val preferences = context.getSharedPreferences(syncPrefs, Context.MODE_PRIVATE)
        val entryIDs = preferences.getString(nutritionEntryIDsKey, "{}")?.let(::JSONObject) ?: JSONObject()
        val itemIDs = preferences.getString(nutritionItemIDsKey, "{}")?.let(::JSONObject) ?: JSONObject()
        val localEntryIDs = entries.liveRecords().associate { entry ->
            localNutritionLogId(entry.optString("id"), knownLocalIds) to entry.optString("id")
        }
        localEntryIDs.forEach { (localID, remoteID) -> entryIDs.put(localID.toString(), remoteID) }
        items.liveRecords().forEach { item ->
            val localEntryID = localNutritionLogId(item.optString("nutrition_log_entry_id"), knownLocalIds)
            itemIDs.put("$localEntryID:${item.optInt("order_index", 0)}", item.optString("id"))
        }
        preferences.edit()
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
        "$date|${mealTypeFromRemote(mealType).name}|${title.trim().lowercase()}"

    /** Fill a legacy timestamp only when the cached server row matches the local metric. */
    private fun migrateMatchingBodyMetricVersions(context: Context) = synchronized(BodyMetricsStore) {
        val prefs = context.getSharedPreferences("wildforce_body_metrics", Context.MODE_PRIVATE)
        val rows = prefs.getString("history", "[]")?.let(::JSONArray) ?: JSONArray()
        val cache = context.getSharedPreferences(syncStatePrefs, Context.MODE_PRIVATE)
            .getString("remote_projection:body-metric-entries", "[]")?.let(::JSONArray) ?: JSONArray()
        val byId = (0 until cache.length()).mapNotNull(cache::optJSONObject)
            .filter { it.isNull("deleted_at") }.associateBy { it.optString("id").lowercase() }
        val fields = mapOf("weight" to "weightKg", "leanBodyMass" to "leanBodyMassKg", "bodyFatPercentage" to "bodyFatPercentage",
            "waistCircumference" to "waistCm", "neckCircumference" to "neckCm", "hipCircumference" to "hipCm",
            "chestCircumference" to "chestCm", "thighCircumference" to "thighCm", "armCircumference" to "armCm")
        var changed = false
        for (index in 0 until rows.length()) {
            val row = rows.optJSONObject(index) ?: continue
            if (runCatching { java.time.Instant.parse(row.optString("updatedAt")) }.isSuccess) continue
            val remote = byId[row.optString("id").lowercase()] ?: continue
            val key = fields[remote.optString("type")] ?: continue
            if (fields.values.count { !row.isNull(it) } != 1 || row.isNull(key)) continue
            val remoteTime = runCatching { java.time.Instant.parse(remote.optString("updated_at")) }.getOrNull() ?: continue
            val sameTimestamp = row.optString("recordedAt").takeIf(String::isNotBlank) == remote.optString("recorded_at")
            val localSource = row.optString("source", "manual").let { if (it == "healthConnect") "manual" else it }
            if (sameTimestamp && row.optDouble(key, Double.NaN) == remote.optDouble("value", Double.NaN) &&
                localSource == remote.optString("source", "manual") &&
                row.optString("checkInId").let { if (it == "null") "" else it } ==
                    remote.optString("check_in_id").let { if (it == "null") "" else it }) {
                row.put("updatedAt", remoteTime.toString())
                changed = true
            }
        }
        if (changed) prefs.edit().putString("history", rows.toString()).apply()
    }

    /** Restores every body-metric type exposed by iOS, in canonical metric units. */
    private fun pullRemoteBodyMetrics(context: Context, remote: JSONArray, fallbackHeightCm: Int) {
        val prefs = context.getSharedPreferences("wildforce_body_metrics", Context.MODE_PRIVATE)
        val stored = prefs.getString("history", "[]")?.let(::JSONArray) ?: JSONArray()
        val pendingDeletions = prefs.getString(BodyMetricsStore.PENDING_DELETIONS_KEY, "{}")?.let(::JSONObject) ?: JSONObject()
        val userId = context.getSharedPreferences(accountPrefs, Context.MODE_PRIVATE).getString("id", null) ?: return
        val fields = linkedMapOf(
            "weight" to "weightKg", "leanBodyMass" to "leanBodyMassKg", "bodyFatPercentage" to "bodyFatPercentage",
            "waistCircumference" to "waistCm", "neckCircumference" to "neckCm", "hipCircumference" to "hipCm",
            "chestCircumference" to "chestCm", "thighCircumference" to "thighCm", "armCircumference" to "armCm",
        )
        val rows = linkedMapOf<String, JSONObject>()
        val legacyIds = mutableSetOf<String>()
        for (index in 0 until stored.length()) {
            val entry = stored.optJSONObject(index) ?: continue
            val date = entry.optString("date").takeIf(String::isNotBlank) ?: continue
            val existingId = entry.optString("id").takeIf(::isUuid)
            if (existingId != null) {
                rows[existingId.lowercase()] = JSONObject(entry.toString())
            } else {
                // Old Android entries combined all values recorded on one day. Split
                // them without changing the deterministic IDs used by older uploads.
                fields.forEach { (type, key) ->
                    if (!entry.isNull(key)) {
                        val id = stableId("$userId:$type:$date")
                        rows[id.lowercase()] = JSONObject().put("id", id).put("date", date)
                            .put("heightCm", entry.optDouble("heightCm", fallbackHeightCm.toDouble()))
                            .put("source", entry.optString("source", "manual")).put(key, entry.opt(key))
                        legacyIds += id.lowercase()
                    }
                }
            }
        }
        val syncState = context.getSharedPreferences(syncStatePrefs, Context.MODE_PRIVATE)
        val baseline = syncState.getString("$pushedRecordsPrefix${"body-metric-entries"}", "{}")?.let(::JSONObject) ?: JSONObject()
        val metricDirty = "body-metric-entries" in RemoteSyncChangeTracker.dirty(context)
        val unknownDeletedIds = if (metricDirty) baseline.keys().asSequence().map(String::lowercase)
            .filter { it !in rows && !pendingDeletions.has(it) }.toSet() else emptySet()
        val remoteIds = (0 until remote.length()).mapNotNull { remote.optJSONObject(it)?.optString("id")?.takeIf(::isUuid)?.lowercase() }.toSet()
        for (index in 0 until remote.length()) {
            val remoteEntry = remote.optJSONObject(index) ?: continue
            val id = remoteEntry.optString("id").takeIf(::isUuid) ?: continue
            val normalizedId = id.lowercase()
            if (normalizedId in unknownDeletedIds) continue
            val pendingDeletion = pendingDeletions.optString(normalizedId).takeIf(String::isNotBlank)
            if (pendingDeletion != null) {
                if (!shouldApplyRemoteRecord(pendingDeletion, remoteEntry.optString("updated_at"))) {
                    // Keep the original server payload as the basis for the outbound tombstone.
                    if (!baseline.has(id) && !baseline.has(normalizedId)) baseline.put(id, JSONObject(remoteEntry.toString()))
                    continue
                }
                pendingDeletions.remove(normalizedId)
            }
            val existing = rows[normalizedId]
            if (metricDirty && existing != null && runCatching { java.time.Instant.parse(existing.optString("updatedAt")) }.isFailure) continue
            if (existing != null && !shouldApplyRemoteRecord(existing.optString("updatedAt"), remoteEntry.optString("updated_at"))) continue
            val isDeleted = !remoteEntry.isNull("deleted_at") && remoteEntry.optString("deleted_at").isNotBlank()
            val removed = if (isDeleted) rows.remove(normalizedId) else null
            if (isDeleted) {
                baseline.remove(id)
                baseline.remove(normalizedId)
            }
            val type = remoteEntry.optString("type")
            val key = fields[type] ?: continue
            val value = remoteEntry.optDouble("value", Double.NaN)
            val recordedAt = remoteEntry.optString("recorded_at")
            val instant = runCatching { java.time.Instant.parse(recordedAt) }.getOrNull() ?: continue
            if (!value.isFinite()) continue
            val date = instant.atZone(java.time.ZoneId.systemDefault()).toLocalDate().toString()
            if (isDeleted) {
                if (removed == null) {
                    val legacy = legacyIds.firstOrNull { oldId ->
                        val old = rows[oldId] ?: return@firstOrNull false
                        oldId !in remoteIds && old.optString("date") == date && !old.isNull(key) &&
                            kotlin.math.abs(old.optDouble(key, Double.NaN) - value) < .001
                    }
                    if (legacy != null && !metricDirty) { rows.remove(legacy); legacyIds.remove(legacy) }
                }
                continue
            }
            // Earlier Android versions collapsed an iOS row into a day bucket.
            // Adopt its real ID when the stored value matches, instead of duplicating it.
            if (!rows.containsKey(normalizedId)) {
                val matchingLegacy = legacyIds.firstOrNull { oldId ->
                    val old = rows[oldId] ?: return@firstOrNull false
                    oldId !in remoteIds && old.optString("date") == date && !old.isNull(key) &&
                        kotlin.math.abs(old.optDouble(key, Double.NaN) - value) < .001
                }
                if (matchingLegacy != null && !metricDirty) {
                    rows.remove(matchingLegacy)
                    legacyIds.remove(matchingLegacy)
                }
            }
            val previous = rows[normalizedId]
            val target = JSONObject().put("id", id).put("date", date)
                .put("heightCm", previous?.optDouble("heightCm", fallbackHeightCm.toDouble()) ?: fallbackHeightCm.toDouble())
                .put("recordedAt", recordedAt)
                .put("createdAt", remoteEntry.optString("created_at", recordedAt))
                .put("updatedAt", remoteEntry.optString("updated_at", recordedAt))
                .put("source", if (previous?.optString("source") == "healthConnect" && remoteEntry.optString("source") == "manual") "healthConnect" else remoteEntry.optString("source", "manual")).put(key, value)
            remoteEntry.optString("check_in_id").takeIf(::isUuid)?.let { target.put("checkInId", it) }
            rows[normalizedId] = target
            // Keep the actual remote ID in the upload baseline so deleting an
            // imported row emits a tombstone for that same entry.
            baseline.put(id, record(id, remoteEntry.optString("updated_at", recordedAt))
                .put("created_at", remoteEntry.optString("created_at", recordedAt))
                .put("type", type).put("value", value).put("recorded_at", recordedAt)
                .put("source", remoteEntry.optString("source", "manual"))
                .apply { remoteEntry.optString("check_in_id").takeIf(::isUuid)?.let { put("check_in_id", it) } })
        }
        val orderedRows = rows.values.sortedWith(compareBy<JSONObject> { it.optString("date") }.thenBy { it.optString("recordedAt") })
        val history = JSONArray().apply { orderedRows.forEach(::put) }
        prefs.edit().apply {
            if (!prefs.contains("history_legacy_backup_v1") && stored.length() > 0) {
                putString("history_legacy_backup_v1", stored.toString())
            }
            putString("history", history.toString())
            putString(BodyMetricsStore.PENDING_DELETIONS_KEY, pendingDeletions.toString())
        }.apply()
        syncState.edit().putString("$pushedRecordsPrefix${"body-metric-entries"}", baseline.toString()).apply()
        val details = context.getSharedPreferences("wildforce_profile_details", Context.MODE_PRIVATE)
        val composition = details.getString("body_composition", "{}")?.let(::JSONObject) ?: JSONObject()
        listOf("waistCm", "neckCm", "hipCm").forEach { key ->
            orderedRows.lastOrNull { !it.isNull(key) }?.let { composition.put(key, it.optDouble(key)) }
        }
        details.edit().putString("body_composition", composition.toString()).apply()
    }

    private fun pullRemoteAppSettings(context: Context, remote: RemoteSnapshot): Boolean? {
        val records = remote.records("user-app-settings")
        val settingsDeleted = records.length() > 0 && records.firstLiveRecord() == null
        val settings = records.firstLiveRecord() ?: if (settingsDeleted) JSONObject()
            .put("is_health_kit_enabled", false).put("is_watch_auto_tracking_enabled", false)
            .put("is_screen_on_during_workout_enabled", false).put("is_notifications_enabled", false)
            .put("is_full_focus_mode_enabled", false).put("has_seen_notification_request", false)
            .put("has_seen_body_progress_tutorial", false).put("has_rated_app", false) else return null
        val profile = context.getSharedPreferences("wildforce_profile_details", Context.MODE_PRIVATE)
        profile.edit()
            .putBoolean("keep_screen_on", settings.optBoolean("is_screen_on_during_workout_enabled", profile.getBoolean("keep_screen_on", true)))
            .putBoolean("full_focus", settings.optBoolean("is_full_focus_mode_enabled", profile.getBoolean("full_focus", false)))
            .putBoolean("has_seen_notification_request", settings.optBoolean("has_seen_notification_request", profile.getBoolean("has_seen_notification_request", false)))
            .putBoolean("has_seen_body_progress_tutorial", settings.optBoolean("has_seen_body_progress_tutorial", false) || (!settingsDeleted && profile.getBoolean("has_seen_body_progress_tutorial", false)))
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

    private fun synchronizeNutrition(token: String, userId: String, profile: OnboardingProfile, now: String, context: Context, profileUpdatedAt: String, profileOnly: Boolean) {
        val nutritionState = context.getSharedPreferences("wildforce_nutrition", Context.MODE_PRIVATE)
        val pendingDeletionsAtStart = nutritionState.getString(NutritionStore.PENDING_DELETIONS_KEY, "{}")?.let(::JSONObject) ?: JSONObject()
        if (profileOnly) {
            val prefs = NutritionStore.loadPreferences(context)
            val nutritionProfileId = remoteProjectionID(context, "nutrition-profiles", stableId("$userId:nutrition-profile"))
            push(token, "nutrition-profiles", JSONArray().put(recordFromRemoteProjection(context, "nutrition-profiles", nutritionProfileId, profileUpdatedAt)
                .put("dietary_style", dietaryStyleSyncValue(prefs.dietaryStyle)).put("meals_per_day_preference", prefs.mealsPerDay)
                .put("preferred_eating_window_start_hour", prefs.eatingWindowStartHour ?: JSONObject.NULL)
                .put("preferred_eating_window_end_hour", prefs.eatingWindowEndHour ?: JSONObject.NULL)
                .put("excluded_foods", JSONArray(prefs.excludedFoods)).put("allergies_and_intolerances", JSONArray(prefs.allergiesAndIntolerances))
                .put("cooking_effort", cookingEffortSyncValue(prefs.cookingEffort)).put("budget_sensitivity", budgetSensitivitySyncValue(prefs.budgetSensitivity))
                .put("preferred_protein_sources", JSONArray(prefs.preferredProteinSources)).put("dislikes", JSONArray(prefs.dislikes))
                .put("wants_meal_suggestions", prefs.wantsSuggestions).put("notes", prefs.notes)))

            return
        }

        val planVersions = nutritionState.getString(NutritionStore.PLAN_RECORD_VERSIONS_KEY, "{}")?.let(::JSONObject) ?: JSONObject()
        val dismissalDates = nutritionState.getString(NutritionStore.DISMISSED_PLANNED_MEAL_DATES_KEY, "{}")?.let(::JSONObject) ?: JSONObject()
        fun nutritionRecord(resource: String, id: String, key: String): JSONObject {
            val timestamp = planVersions.optString(key).takeIf { runCatching { java.time.Instant.parse(it) }.isSuccess }
            return recordFromRemoteProjection(context, resource, id, timestamp ?: now).apply {
                if (timestamp == null) put("_android_missing_nutrition_version", true)
            }
        }
        val ids = context.getSharedPreferences(syncPrefs, Context.MODE_PRIVATE)
        val entryIDs = ids.getString(nutritionEntryIDsKey, "{}")?.let(::JSONObject) ?: JSONObject()
        val itemIDs = ids.getString(nutritionItemIDsKey, "{}")?.let(::JSONObject) ?: JSONObject()
        val dayIDs = ids.getString(nutritionDayIDsKey, "{}")?.let(::JSONObject) ?: JSONObject()
        val plannedMealIDs = ids.getString(nutritionMealIDsKey, "{}")?.let(::JSONObject) ?: JSONObject()
        // Create planned meals before entries that reference their server IDs.
        NutritionStore.loadGeneratedPlan(context)?.let { plan ->
            val planMetadata = NutritionStore.loadPlanMetadata(context)
            // A server-generated plan already has an ID. Reuse it instead of
            // creating Android's deterministic duplicate on the next sync.
            val nutritionPlanId = NutritionStore.remotePlanId(context) ?: stableId("$userId:nutrition-plan")
            NutritionStore.rememberRemotePlanId(context, nutritionPlanId)
            push(token, "nutrition-plans", JSONArray().put(nutritionRecord("nutrition-plans", nutritionPlanId, "plan")
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
                days.put(nutritionRecord("nutrition-days", dayId, "day:$date").put("nutrition_plan_id", nutritionPlanId).put("date", day.date.toString())
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
                    val mealRecord = nutritionRecord("nutrition-meals", mealId, "meal:$mealKey")
                    val discardedAt = if (mealKey in dismissedMeals) {
                        dismissalDates.optString(mealKey).takeIf { runCatching { java.time.Instant.parse(it) }.isSuccess }
                            ?: mealRecord.optString("discarded_at").takeIf { runCatching { java.time.Instant.parse(it) }.isSuccess }
                    } else null
                    if (mealKey in dismissedMeals && discardedAt == null) mealRecord.put("_android_missing_nutrition_version", true)
                    meals.put(mealRecord.put("nutrition_day_id", dayId).put("title", meal.title).put("order_index", index).put("meal_type", meal.type.syncValue())
                    .put("target_calories", meal.targets.calories).put("target_protein_grams", meal.targets.protein)
                    .put("target_carbs_grams", meal.targets.carbs).put("target_fat_grams", meal.targets.fat)
                    .put("guidance", meal.guidance).put("example_foods", JSONArray().apply { meal.foods.forEach { (name, grams) -> put(JSONObject().put("name", name).put("grams", grams)) } })
                    .put("discarded_at", discardedAt ?: JSONObject.NULL))
                }
            }
            ids.edit().putString(nutritionDayIDsKey, dayIDs.toString()).putString(nutritionMealIDsKey, plannedMealIDs.toString()).apply()
            push(token, "nutrition-days", days)
            push(token, "nutrition-meals", meals)
        }

        val entries = JSONArray(); val items = JSONArray(); val media = JSONArray()
        val mediaSyncPrefs = idsForNutritionMedia(context)
        val downloadedMedia = mediaSyncPrefs.getStringSet(remoteNutritionMediaKey, emptySet()).orEmpty().toSet()
        val previouslyUploadedMedia = mediaSyncPrefs.getStringSet(uploadedNutritionMediaKey, emptySet()).orEmpty().toSet()
        val mediaBaseline = context.getSharedPreferences(syncStatePrefs, Context.MODE_PRIVATE)
            .getString("$pushedRecordsPrefix${"nutrition-log-media"}", "{}")?.let(::JSONObject) ?: JSONObject()
        val legacyEntryIds = mutableSetOf<String>()
        NutritionStore.allForSync(context).forEach { meal ->
            val entryKey = meal.id.toString()
            val entryId = entryIDs.optString(entryKey).takeIf(::isUuid) ?: nutritionEntryId(userId, meal.id)
            entryIDs.put(entryKey, entryId)
            if (meal.updatedAt == null || meal.items.any { it.updatedAt == null }) {
                legacyEntryIds += entryId.lowercase()
                return@forEach
            }
            val photo = meal.photoPath?.takeIf { File(it).isFile }
            val entryRecord = recordFromRemoteProjection(context, "nutrition-log-entries", entryId, meal.updatedAt.toString())
            val remoteId = entryRecord.optString("nutrition_log_media_id").takeIf(::isUuid)
            val remoteMediaUrl = meal.mediaUrl?.trim()?.takeIf { it.isNotBlank() && photo == null }
            val reuseRemote = shouldReuseRemoteNutritionMedia(
                hasRemoteId = remoteId != null, hasLocalPhoto = photo != null,
                isDownloadedPhoto = photo != null && "$entryId:$photo" in downloadedMedia,
                hasRemoteUrl = remoteMediaUrl != null, importedEntry = meal.source == "sync",
                isUploadedPhoto = photo != null && "$entryId:$photo" in previouslyUploadedMedia,
            )
            val mediaId = if (reuseRemote) remoteId else if (photo != null || remoteMediaUrl != null)
                stableId("$entryId:nutrition-log-media:${photo ?: remoteMediaUrl}") else null
            if (!reuseRemote && mediaId != null) {
                val source = if (remoteMediaUrl != null) "remoteURL" else "localPhoto"
                val previous = mediaBaseline.optJSONObject(mediaId)
                val unchanged = previous != null && previous.optString("source") == source &&
                    previous.optString("remote_url").takeUnless { it.isBlank() || it == "null" } == remoteMediaUrl
                val modified = if (unchanged) previous?.optString("updated_at")?.takeIf { runCatching { java.time.Instant.parse(it) }.isSuccess }
                    ?: meal.updatedAt.toString() else meal.updatedAt.toString()
                val created = previous?.optString("created_at")?.takeIf { runCatching { java.time.Instant.parse(it) }.isSuccess }
                    ?: (meal.createdAt ?: meal.updatedAt).toString()
                media.put(record(mediaId, modified).put("created_at", created)
                    .put("source", source)
                    .put("local_relative_path", photo?.let { File(it).name } ?: JSONObject.NULL)
                    .put("remote_url", remoteMediaUrl ?: JSONObject.NULL))
            }
            val previousLoggedAt = entryRecord.optString("logged_at").takeIf(String::isNotBlank)
            entries.put(entryRecord.put("title", meal.name)
                .put("logged_at", nutritionLogTimestampForMeal(meal, previousLoggedAt)).put("meal_type", meal.type.syncValue())
                .put("notes", meal.notes).put("is_favorite", meal.isBookmarked)
                .put("nutrition_log_media_id", mediaId ?: JSONObject.NULL)
                .put("nutrition_meal_id", meal.plannedMealKey?.let { key -> plannedMealIDs.optString(key).takeIf(::isUuid) } ?: JSONObject.NULL))
            val foods = meal.items.ifEmpty { listOf(io.codepassion.doubletriangle.nutrition.MealItem(meal.name, meal.calories.toDouble(), meal.protein.toDouble(), meal.carbs.toDouble(), meal.fat.toDouble(), updatedAt = meal.updatedAt)) }
            foods.forEachIndexed { index, food ->
                val itemKey = "${meal.id}:$index"
                val itemId = food.remoteId?.takeIf(::isUuid) ?: itemIDs.optString(itemKey).takeIf(::isUuid) ?: stableId("$entryId:$index")
                itemIDs.put(itemKey, itemId)
                val itemRecord = recordFromRemoteProjection(context, "nutrition-log-items", itemId, food.updatedAt?.toString() ?: now)
                val original = activeProjectionCache["nutrition-log-items"]?.get(itemId.lowercase(java.util.Locale.ROOT))
                val remoteTime = original?.optString("updated_at")
                val preserveOrder = original != null && original.has("order_index") &&
                    !shouldApplyRemoteRecord(remoteTime, food.updatedAt?.toString())
                val order = if (preserveOrder) original!!.optInt("order_index", index) else index
                items.put(itemRecord.put("nutrition_log_entry_id", entryId).put("name", food.name).put("brand", food.brand.ifBlank { JSONObject.NULL })
                .put("quantity", food.quantity).put("unit", food.unit.ifBlank { JSONObject.NULL }).put("amount_grams", food.grams ?: JSONObject.NULL)
                .put("calories", food.calories).put("protein_grams", food.protein).put("carbs_grams", food.carbs)
                .put("fat_grams", food.fat).put("notes", food.notes.ifBlank { JSONObject.NULL }).put("order_index", order))
            }
        }
        ids.edit().putString(nutritionEntryIDsKey, entryIDs.toString()).putString(nutritionItemIDsKey, itemIDs.toString()).apply()
        // Keep media dirty until the binary queue has also been drained. If
        // the process stops after metadata upload, the files must retry.
        push(token, "nutrition-log-media", media,
            preserveAllMissing = true, clearOnSuccess = false)
        val priorEntries = context.getSharedPreferences(syncStatePrefs, Context.MODE_PRIVATE)
            .getString("$pushedRecordsPrefix${"nutrition-log-entries"}", "{}")?.let(::JSONObject) ?: JSONObject()
        val entryIds = (0 until entries.length()).mapNotNull { entries.optJSONObject(it)?.optString("id")?.lowercase() }.toSet()
        val unknownDeletedIds = priorEntries.keys().asSequence().filter { id ->
            id.lowercase() !in entryIds && id.lowercase() !in legacyEntryIds && !pendingDeletionsAtStart.has(id.lowercase())
        }.toSet()
        SyncDiagnostics.record(context, "nutrition", "Prepared entries=${entries.length()}, legacy=${legacyEntryIds.size}, unconfirmed removals=${unknownDeletedIds.size}")
        val preserveEntries = priorEntries.keys().asSequence().filter { it.lowercase() in legacyEntryIds }.toSet() + unknownDeletedIds
        push(token, "nutrition-log-entries", entries, preserveMissingIds = preserveEntries,
            clearOnSuccess = preserveEntries.isEmpty())
        // iOS soft-deletes a meal entry without deleting its food items.
        // Keep those children in the push baseline while the parent is removed.
        val priorItems = context.getSharedPreferences(syncStatePrefs, Context.MODE_PRIVATE)
            .getString("$pushedRecordsPrefix${"nutrition-log-items"}", "{}")?.let(::JSONObject) ?: JSONObject()
        val entryProjection = context.getSharedPreferences(syncStatePrefs, Context.MODE_PRIVATE)
            .getString("remote_projection:nutrition-log-entries", "[]")?.let(::JSONArray) ?: JSONArray()
        val deletedParents = (0 until entryProjection.length()).mapNotNull { index ->
            entryProjection.optJSONObject(index)?.takeUnless { it.isNull("deleted_at") }
                ?.optString("id")?.lowercase()
        }.toSet() + pendingDeletionsAtStart.keys().asSequence().map(String::lowercase).toSet()
        val itemParents = priorItems.keys().asSequence().mapNotNull { id ->
            priorItems.optJSONObject(id)?.optString("nutrition_log_entry_id")?.takeIf(String::isNotBlank)?.let { id to it }
        }.toMap()
        val preservedDeletedParentItems = itemIdsForDeletedNutritionEntries(itemParents, deletedParents + legacyEntryIds + unknownDeletedIds.map(String::lowercase))
        push(token, "nutrition-log-items", items, preserveMissingIds = preservedDeletedParentItems,
            clearOnSuccess = legacyEntryIds.isEmpty() && unknownDeletedIds.isEmpty())
        if (legacyEntryIds.isEmpty() && unknownDeletedIds.isEmpty() && setOf("nutrition-log-entries", "nutrition-log-items").all { it in activePushResources &&
                RemoteSyncChangeTracker.generation(context, it) == activePushGenerations[it] }) {
            synchronized(NutritionStore) {
                val pending = nutritionState.getString(NutritionStore.PENDING_DELETIONS_KEY, "{}")?.let(::JSONObject) ?: JSONObject()
                val projection = context.getSharedPreferences(syncStatePrefs, Context.MODE_PRIVATE)
                    .getString("remote_projection:nutrition-log-entries", "[]")?.let(::JSONArray) ?: JSONArray()
                val projected = (0 until projection.length()).mapNotNull(projection::optJSONObject)
                    .associateBy { it.optString("id").lowercase() }
                pendingDeletionsAtStart.keys().forEach { id ->
                    val deletion = pendingDeletionsAtStart.optString(id)
                    val row = projected[id.lowercase()]
                    val confirmed = row == null && isHistoryDownloaded(context) ||
                        (row != null && !row.isNull("deleted_at") &&
                            !shouldApplyRemoteRecord(row.optString("updated_at"), deletion))
                    if (confirmed && pending.optString(id) == deletion) pending.remove(id)
                }
                nutritionState.edit().putString(NutritionStore.PENDING_DELETIONS_KEY, pending.toString()).apply()
            }
        }
        if ("nutrition-log-media" !in activePushResources) return
        // iOS stores meal photos separately from the sync graph and uploads
        // them after their entry exists. Do the same for Android photo logs.
        val mediaState = idsForNutritionMedia(context)
        val uploadedMedia = mediaState.getStringSet(uploadedNutritionMediaKey, emptySet()).orEmpty().toMutableSet()
        val remoteMedia = mediaState.getStringSet(remoteNutritionMediaKey, emptySet()).orEmpty()
        var mediaUploadFailed = false
        NutritionStore.allForSync(context).forEach { meal ->
            val entryId = entryIDs.optString(meal.id.toString()).takeIf(::isUuid)
            meal.photoPath?.takeIf { File(it).isFile }?.let { path ->
                if (entryId != null) {
                    val mediaKey = "$entryId:$path"
                    if (mediaKey in uploadedMedia || mediaKey in remoteMedia) return@let
                    try {
                        uploadNutritionLogImage(token, entryId, File(path))
                        SyncDiagnostics.record(context, "media", "Nutrition image upload completed")
                        uploadedMedia += mediaKey
                        mediaState.edit().putStringSet(uploadedNutritionMediaKey, uploadedMedia).apply()
                    } catch (error: Exception) {
                        if (error is CancellationException) throw error
                        // iOS leaves this item queued, including missing parents,
                        // and continues with the remaining outbound images.
                        mediaUploadFailed = true
                        RemoteSyncChangeTracker.mark(context, "nutrition-log-media")
                        SyncDiagnostics.record(context, "media", "Nutrition image upload failed: ${SyncDiagnostics.errorDetail(error)}")
                    }
                }
            }
        }
        if (canAcknowledgeNutritionMedia(mediaUploadFailed, legacyEntryIds.isNotEmpty())) clearDirtyResource("nutrition-log-media")
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

    /** Uploads the available check-in images after their server-side session exists. */
    private fun synchronizeBodyProgressPhotos(context: Context, token: String, userId: String) {
        val raw = context.getSharedPreferences("wildforce_body_progress", Context.MODE_PRIVATE)
            .getString("sessions", "[]") ?: "[]"
        val sessions = JSONArray(raw)
        val records = JSONArray()
        val pending = context.getSharedPreferences("wildforce_body_progress", Context.MODE_PRIVATE)
            .getString("pending_session_deletions_v1", "{}")?.let(::JSONObject) ?: JSONObject()
        val deletionTimes = pending.keys().asSequence().associate { id ->
            bodyProgressRemoteID(context, userId, id).lowercase(java.util.Locale.ROOT) to pending.optString(id)
        }
        val syncState = context.getSharedPreferences(syncStatePrefs, Context.MODE_PRIVATE)
        val baseline = syncState.getString("$pushedRecordsPrefix${"body-progress-photo-sessions"}", "{}")?.let(::JSONObject) ?: JSONObject()
        val protectedIds = baseline.keys().asSequence().filter { it.lowercase(java.util.Locale.ROOT) !in deletionTimes }.toSet()
        val uploadQueue = mutableListOf<Triple<String, String, File>>()
        var unresolvedSessions = 0
        for (index in 0 until sessions.length()) {
            val session = sessions.optJSONObject(index) ?: continue
            val localId = session.optString("id").takeIf(String::isNotBlank) ?: continue
            val remoteId = bodyProgressRemoteID(context, userId, localId)
            val prior = baseline.optJSONObject(remoteId)
            val updated = session.optString("updatedAt").takeIf { runCatching { java.time.Instant.parse(it) }.isSuccess }
                ?: prior?.optString("updated_at")?.takeIf { runCatching { java.time.Instant.parse(it) }.isSuccess }
            if (updated == null) { unresolvedSessions++; continue }
            val created = session.optString("createdAt").takeIf { runCatching { java.time.Instant.parse(it) }.isSuccess }
                ?: prior?.optString("created_at")?.takeIf { runCatching { java.time.Instant.parse(it) }.isSuccess }
                ?: updated
            records.put(recordFromRemoteProjection(context, "body-progress-photo-sessions", remoteId, updated).put("created_at", created))
            listOf("front" to session.optString("frontPath"), "profile" to session.optString("profilePath"), "back" to session.optString("backPath")).forEach { (angle, path) ->
                File(path).takeIf(File::isFile)?.let { uploadQueue += Triple(remoteId, angle, it) }
            }
        }
        push(token, "body-progress-photo-sessions", records, preserveMissingIds = protectedIds, clearOnSuccess = false)
        val uploadState = context.getSharedPreferences("wildforce_remote_media", Context.MODE_PRIVATE)
        uploadQueue.forEach { (remoteId, angle, file) ->
            val key = "body:$remoteId:$angle"
            val fingerprint = "${file.absolutePath}:${file.length()}:${file.lastModified()}"
            if (uploadState.getString(key, null) != fingerprint) {
                uploadImage(token, WildforceApiEnvironment.apiUrl("body-progress-photo-sessions/$remoteId/photos/$angle"), file, "image/jpeg")
                uploadState.edit().putString(key, fingerprint).apply()
            }
        }
        val remainingDeletions = context.getSharedPreferences("wildforce_body_progress", Context.MODE_PRIVATE)
            .getString("pending_session_deletions_v1", "{}")?.let(::JSONObject) ?: JSONObject()
        if (unresolvedSessions == 0 && !remainingDeletions.keys().hasNext()) clearDirtyResource("body-progress-photo-sessions")
    }

    /** The sync row carries only the safe file name; bytes use the private endpoint. */
    private fun synchronizeTrainingLocationPhotos(
        context: Context,
        token: String,
        uploads: List<Pair<String, File>>,
    ) {
        val state = context.getSharedPreferences("wildforce_remote_media", Context.MODE_PRIVATE)
        uploads.forEach { (locationId, file) ->
            val key = "training-location:$locationId"
            val fingerprint = "${file.name}:${file.length()}:${file.lastModified()}"
            if (state.getString(key, null) != fingerprint) {
                uploadImage(token, WildforceApiEnvironment.apiUrl("training-locations/$locationId/photo"), file, "image/jpeg")
                state.edit().putString(key, fingerprint).apply()
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
    private fun pullRemoteMedia(context: Context, token: String, userId: String, sessions: JSONArray) {
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
        val syncState = context.getSharedPreferences(syncStatePrefs, Context.MODE_PRIVATE)
        val pushed = syncState.getString("$pushedRecordsPrefix${"body-progress-photo-sessions"}", "{}")?.let(::JSONObject) ?: JSONObject()
        val mediaState = context.getSharedPreferences("wildforce_remote_media", Context.MODE_PRIVATE)
        fun rememberDownloaded(id: String, angle: String, file: File) {
            mediaState.edit().putString("body:$id:$angle", "${file.absolutePath}:${file.length()}:${file.lastModified()}").apply()
        }
        for (index in 0 until sessions.length()) {
            val session = sessions.optJSONObject(index) ?: continue
            val id = session.optString("id").takeIf(String::isNotBlank) ?: continue
            val localId = localById.keys.firstOrNull { candidate ->
                bodyProgressRemoteID(context, userId, candidate).equals(id, ignoreCase = true)
            } ?: id
            if (!session.isNull("deleted_at")) {
                localById.remove(localId)?.let { removed ->
                    listOf("frontPath", "profilePath", "backPath", "futurePath").forEach { key ->
                        removed.optString(key).takeIf { it.isNotBlank() }?.let { File(it).delete() }
                    }
                }
                remoteIds.remove(localId)
                pushed.remove(id)
                continue
            }
            // Remember imported rows so deleting one later emits a server tombstone.
            val existing = localById[localId]
            if (existing != null) {
                // A previous download may have restored only some angles. Retry
                // missing files on later syncs without replacing local captures.
                listOf("front", "profile", "back").forEach { angle ->
                    val key = "${angle}Path"
                    val savedPath = existing.optString(key)
                    if (savedPath.isBlank() || !File(savedPath).isFile) {
                        val destination = File(folder, "${id}_${angle}.jpg")
                        if (download(token, WildforceApiEnvironment.apiUrl("body-progress-photo-sessions/$id/photos/$angle"), destination)) {
                            existing.put(key, destination.absolutePath)
                            rememberDownloaded(id, angle, destination)
                        }
                    }
                }
                pushed.put(id, JSONObject(session.toString()))
                continue
            }
            val front = File(folder, "${id}_front.jpg")
            val profile = File(folder, "${id}_profile.jpg")
            val back = File(folder, "${id}_back.jpg")
            val frontDownloaded = download(token, WildforceApiEnvironment.apiUrl("body-progress-photo-sessions/$id/photos/front"), front)
            val profileDownloaded = download(token, WildforceApiEnvironment.apiUrl("body-progress-photo-sessions/$id/photos/profile"), profile)
            val backDownloaded = download(token, WildforceApiEnvironment.apiUrl("body-progress-photo-sessions/$id/photos/back"), back)
            if (frontDownloaded) rememberDownloaded(id, "front", front)
            if (profileDownloaded) rememberDownloaded(id, "profile", profile)
            if (backDownloaded) rememberDownloaded(id, "back", back)
            if (frontDownloaded && profileDownloaded) {
                pushed.put(id, JSONObject(session.toString()))
                localById[id] = JSONObject().put("id", id).put("date", session.optString("created_at").take(10))
                    .put("createdAt", session.optString("created_at")).put("updatedAt", session.optString("updated_at"))
                    .put("frontPath", front.absolutePath).put("profilePath", profile.absolutePath)
                    .put("backPath", if (backDownloaded) back.absolutePath else localById[id]?.optString("backPath"))
                    .put("futurePath", localById[id]?.optString("futurePath"))
                remoteIds.put(id, id)
            }
        }
        preferences.edit().putString("sessions", JSONArray().apply { localById.values.forEach(::put) }.toString()).apply()
        context.getSharedPreferences(syncPrefs, Context.MODE_PRIVATE).edit()
            .putString("body_progress_remote_ids", remoteIds.toString()).apply()
        syncState.edit().putString("$pushedRecordsPrefix${"body-progress-photo-sessions"}", pushed.toString()).apply()
    }

    /** Hydrates iOS-created location photos without exposing their storage path. */
    private fun pullRemoteTrainingLocationPhotos(context: Context, token: String, locations: JSONArray) {
        for (index in 0 until locations.length()) {
            val location = locations.optJSONObject(index) ?: continue
            if (!location.isNull("deleted_at")) continue
            val id = location.optString("id").takeIf(::isUuid) ?: continue
            val photoPath = location.optString("photo_path")
                .takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) } ?: continue
            val destination = TrainingLocationPhotoStore.file(context, photoPath) ?: continue
            if (!destination.isFile) {
                download(token, WildforceApiEnvironment.apiUrl("training-locations/$id/photo"), destination)
            }
        }
    }

    /** Imported sessions retain their server UUID; locally-created sessions keep Android's stable ID. */
    private fun bodyProgressRemoteID(context: Context, userId: String, localSessionId: String): String {
        val ids = context.getSharedPreferences(syncPrefs, Context.MODE_PRIVATE)
            .getString("body_progress_remote_ids", "{}")?.let(::JSONObject) ?: JSONObject()
        return ids.optString(localSessionId).takeIf(::isUuid) ?: stableId("$userId:body-progress:$localSessionId")
    }

    /** Keep fields Android does not model (address, coordinates and location photo) when editing an iOS record. */
    private fun recordFromRemoteProjection(context: Context, resource: String, id: String, now: String): JSONObject {
        fun loadRows(): Map<String, JSONObject> {
            val cached = context.getSharedPreferences(syncStatePrefs, Context.MODE_PRIVATE)
                .getString("remote_projection:$resource", "[]")?.let(::JSONArray) ?: JSONArray()
            return (0 until cached.length()).mapNotNull(cached::optJSONObject)
                .associateBy { it.optString("id").lowercase(java.util.Locale.ROOT) }
        }
        val rows = if (activePushContext != null) activeProjectionCache.getOrPut(resource, ::loadRows) else loadRows()
        val source = rows[id.lowercase(java.util.Locale.ROOT)] ?: return record(id, now)
        return JSONObject(source.toString()).apply {
            put("id", id)
            put("updated_at", now)
            remove("deleted_at")
        }
    }

    /** Singleton child models are assigned UUIDs by iOS, not derived from the user ID. */
    private fun remoteProjectionID(context: Context, resource: String, fallback: String): String {
        val cached = context.getSharedPreferences(syncStatePrefs, Context.MODE_PRIVATE)
            .getString("remote_projection:$resource", "[]")?.let(::JSONArray) ?: JSONArray()
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
    private fun importGymPrograms(context: Context, remote: RemoteSnapshot) {
        val syntheticId = "gym-session-projection"
        val links = JSONObject()
        val gymDays = JSONArray()
        val days = pullRemoteWorkoutDays(remote)
        for (index in 0 until days.length()) {
            val day = days.optJSONObject(index) ?: continue
            val programId = WorkoutPlanGenerator.normalizeNullableString(day.opt("gym_program_id")) ?: continue
            val id = day.optString("id")
            links.put(id, JSONObject().put("program", programId).put("updated", day.optString("updated_at")).put("deleted", !day.isNull("deleted_at")))
            gymDays.put(JSONObject(day.toString()).put("workout_plan_id", syntheticId))
        }
        val plan = JSONObject().put("id", syntheticId).put("name", "Gym").put("status", "active")
        val imported = remotePlanJson(context, plan, syntheticId, gymDays)?.rawJson ?: return
        // Store the same representation produced by subsequent local edits;
        // otherwise saving one session would mark the entire graph as changed.
        val root = WorkoutPlanGenerator.serialize(WorkoutPlanGenerator.parse(imported, "", "", allowEmpty = true))
        val versions = listOf("workout-days", "workout-blocks", "planned-exercises").associateWith { resource ->
            remote.records(resource).liveRecords().associate { it.optString("id").lowercase(java.util.Locale.ROOT) to it.optString("updated_at") }
        }
        GymProgramStore.import(context, remote.records("gym-programs"), root, links, WorkoutPlanVersions.imported(JSONObject(root), versions))
    }

    private fun pullWorkoutPlans(context: Context, remote: RemoteSnapshot, preferences: SharedPreferences, mayClearPlan: Boolean): Boolean {
        val remotePlans = remote.records("workout-plans").liveRecords()
        Log.d("WorkoutRemoteSync", "Pulled workout-plans=${remotePlans.size}, workout-days=${remote.records("workout-days").liveRecords().size}")
        val previousId = preferences.getString(planKey, null)
        fun clearPreviousIfKnown(): Boolean {
            if (!mayClearPlan || previousId.isNullOrBlank()) return false
            val records = remote.records("workout-plans")
            if ((0 until records.length()).none { records.optJSONObject(it)?.optString("id") == previousId }) return false
            preferences.edit().remove("workout_plan_json").remove(planKey).putBoolean("remote_plan_imported", true).apply()
            return true
        }
        val remoteDays = importRemoteWorkoutHistorySnapshot(context, remote)
        // The profile summary must come from the cross-device workout graph,
        // not from Android's local counter alone.
        val completedCount = remoteDays.liveRecords().count { day ->
            day.optString("status").equals("completed", ignoreCase = true) ||
                day.optString("status").equals("finished", ignoreCase = true)
        }
        context.getSharedPreferences("wildforce_profile", Context.MODE_PRIVATE).edit()
            .putInt("total_completed_workouts", completedCount).apply()
        if (remotePlans.isEmpty()) {
            SyncDiagnostics.record(context, "workout", "No live workout plans in the downloaded records")
            return clearPreviousIfKnown()
        }

        val plans = remotePlans.mapNotNull { plan ->
            val planId = plan.optString("id").takeIf(String::isNotBlank) ?: return@mapNotNull null
            remotePlanJson(context, plan, planId, remoteDays)
        }
        if (plans.isEmpty()) return clearPreviousIfKnown()
        // Match iOS User.currentWorkoutPlan exactly: active plans first, then
        // newest createdAt, updatedAt, and finally UUID as a deterministic tie
        // breaker. Do not infer the current plan from mesocycle numbers.
        val active = selectCurrentRemoteWorkoutPlan(plans, { it.status }, { it.createdAt }, { it.updatedAt }, { it.id })
            ?: run {
                val counts = plans.groupingBy { it.status.lowercase() }.eachCount().entries.sortedBy { it.key }
                    .joinToString { "${it.key}=${it.value}" }
                SyncDiagnostics.record(context, "workout", "No active or completed plan in the downloaded records: $counts")
                return clearPreviousIfKnown()
            }
        Log.d("WorkoutRemoteSync", "Selected plan id=${active.id}, status=${active.status}, mesocycle=${active.mesocycleNumber}, createdAt=${active.createdAt}, updatedAt=${active.updatedAt}")
        // Keep every completed mesocycle locally. The hub renders the selected
        // active plan while its history remains available to overview screens.
        plans.filterNot { it.id == active.id || it.status.equals("scheduled", true) }.forEach { archived ->
            WorkoutPlanArchiveStore.archive(preferences, archived.rawJson)
        }
        val versionsByResource = listOf("workout-plans", "workout-days", "workout-blocks", "planned-exercises").associateWith { resource ->
            val records = remote.records(resource)
            (0 until records.length()).mapNotNull { index ->
                val row = records.optJSONObject(index) ?: return@mapNotNull null
                val id = row.optString("id").lowercase(java.util.Locale.ROOT)
                val timestamp = row.optString("updated_at")
                if (id.isBlank() || runCatching { java.time.Instant.parse(timestamp) }.isFailure) null else id to timestamp
            }.toMap()
        }
        val importedVersions = WorkoutPlanVersions.imported(JSONObject(active.rawJson), versionsByResource)
        preferences.edit().putString(WorkoutPlanVersions.KEY, importedVersions).putString("workout_plan_json", active.rawJson)
            .putString(planKey, active.id)
            .putBoolean("remote_plan_imported", true).apply()
        return true
    }

    /** Merge history by result identity even while the plan has pending edits. */
    private fun importRemoteWorkoutHistorySnapshot(context: Context, remote: RemoteSnapshot): JSONArray {
        val resultRecords = remote.records("exercise-results")
        val deletedResults = buildMap {
            for (index in 0 until resultRecords.length()) {
                val result = resultRecords.optJSONObject(index) ?: continue
                if (result.isNull("deleted_at")) continue
                val id = result.optString("id").takeIf(String::isNotBlank) ?: continue
                val updatedAt = runCatching { java.time.Instant.parse(result.optString("updated_at")).toEpochMilli() }.getOrNull() ?: continue
                put(id, updatedAt)
            }
        }
        WorkoutHistoryStore.removeRemoteDeletedEntries(context, deletedResults)
        val remoteDays = pullRemoteWorkoutDays(remote)
        importRemoteWorkoutHistory(context, remoteDays)
        return remoteDays
    }

    /**
     * The generic sync endpoint exposes normalized workout records. Rebuild the
     * graph expected by the Android plan importer instead of relying on the
     * removed legacy `sync/workout-days` endpoint.
     */
    private fun pullRemoteWorkoutDays(remote: RemoteSnapshot): JSONArray {
        // Sync pages are ordered by modification time, not by the coach's
        // prescribed sequence. Preserve order_index when rebuilding the graph.
        fun records(resource: String) = remote.records(resource).liveRecords()
            .sortedWith(compareBy<JSONObject> { it.optInt("order_index", 0) }.thenBy { it.optString("id") })

        val days = records("workout-days")
        val blocks = records("workout-blocks").sortedWith(
            compareBy<JSONObject> {
                when (it.optString("type").lowercase()) {
                    "warmup" -> 0
                    "cooldown" -> 2
                    else -> 1
                }
            }.thenBy { it.optInt("order_index", 0) }.thenBy { it.optString("id") }
        )
        val exercises = records("planned-exercises")
        val results = records("exercise-results")
        val resultsByExercise = results.groupBy { it.optString("planned_exercise_id") }
        val exercisesByBlock = exercises.groupBy { it.optString("workout_block_id") }
        val directExercisesByDay = exercises.filter { it.isNull("workout_block_id") || it.optString("workout_block_id").isBlank() }
            .groupBy { it.optString("workout_day_id") }
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
                day.put("exercises", JSONArray(directExercisesByDay[day.optString("id")].orEmpty()))
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
            val legacyExercises = remoteExercises(context, day.optJSONArray("exercises"))
            if (legacyExercises.length() > 0) {
                blocks.put(JSONObject().put("remoteId", "legacy-direct-exercises").put("type", "standard")
                    .put("rounds", 1).put("exercises", legacyExercises))
            }
            workouts.put(JSONObject().put("id", day.optString("id")).put("remoteId", day.optString("id")).put("title", day.optString("title", "Sesión ${index + 1}"))
                .put("focus", day.optString("focus", "Fitness general")).put("dayType", day.optString("day_type", "strength"))
                .put("status", day.optString("status", "planned").uppercase()).put("kind", day.optString("kind", "workout"))
                .put("creationSource", day.optString("creation_source", "generated"))
                .put("gymProgramId", WorkoutPlanGenerator.normalizeNullableString(day.opt("gym_program_id")))
                .put("scheduledFor", WorkoutPlanGenerator.normalizeNullableString(day.opt("scheduled_for")))
                .put("startedAt", WorkoutPlanGenerator.normalizeNullableString(day.opt("started_at")))
                .put("completedAt", WorkoutPlanGenerator.normalizeNullableString(day.opt("completed_at")))
                .put("didCountTowardStreak", day.optBoolean("did_count_toward_streak", false))
                .put("notes", WorkoutPlanGenerator.normalizeNullableString(day.opt("notes")))
                .put("activeDurationSeconds", day.optInt("active_duration_seconds").takeIf { day.has("active_duration_seconds") && !day.isNull("active_duration_seconds") })
                .put("activeCaloriesBurned", day.optDouble("active_calories_burned").takeIf { day.has("active_calories_burned") && !day.isNull("active_calories_burned") })
                .put("averageHeartRate", day.optDouble("average_heart_rate").takeIf { day.has("average_heart_rate") && !day.isNull("average_heart_rate") })
                .put("maximumHeartRate", day.optDouble("maximum_heart_rate").takeIf { day.has("maximum_heart_rate") && !day.isNull("maximum_heart_rate") })
                .put("totalVolumeKg", day.optDouble("total_volume_kg").takeIf { day.has("total_volume_kg") && !day.isNull("total_volume_kg") })
                .put("weekday", day.optString("intended_weekday", "MONDAY").uppercase())
                .put("estimatedDurationMinutes", day.optInt("estimated_duration_minutes", 50)).put("blocks", blocks))
        }
        // The API normally returns snake_case, but Android also receives
        // locally-created/legacy payloads using the JSON names emitted by the
        // plan generator. Read both forms so a downloaded plan keeps its real
        // progression metadata instead of silently falling back to 1.
        val cycleLength = plan.optInt("cycle_length", plan.optInt("cycleLength", 1)).coerceAtLeast(1)
        val phaseWeek = plan.optInt("phase_week", plan.optInt("phaseWeek", 1)).coerceIn(1, cycleLength)
        val mesocycleNumber = plan.optInt("mesocycle_number", plan.optInt("mesocycleNumber", 1)).coerceAtLeast(1)
        val mesocycleIndex = plan.optInt("mesocycle_index", plan.optInt("mesocycleIndex", ((mesocycleNumber - 1) / cycleLength) + 1)).coerceAtLeast(1)
        val root = JSONObject().put("planId", planId).put("planName", plan.optString("name", "Plan sincronizado"))
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
        val legacyResultTimestamps = mutableSetOf<Long>()
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
                            note = result.optString("notes").takeIf(String::isNotBlank), durationSeconds = result.optInt("completed_duration_seconds").takeIf { it > 0 }
                                ?: (result.optInt("completed_duration_minutes").coerceAtLeast(0) * 60),
                            distanceKm = result.optDouble("completed_distance_km", 0.0).takeIf(Double::isFinite) ?: 0.0,
                            workoutId = day.optString("id").nullableRemoteIdentifier(),
                            plannedExerciseId = exercise.optString("id").nullableRemoteIdentifier(),
                            remoteResultId = result.optString("id").nullableRemoteIdentifier(),
                            updatedAtMillis = runCatching { java.time.Instant.parse(result.optString("updated_at")).toEpochMilli() }.getOrDefault(0),
                            paceSecondsPerKm = result.optInt("completed_pace_seconds_per_km").takeIf { it > 0 },
                        )
                        exerciseEntries += entry; results += entry
                    }
                }
            }
            collect(day.optJSONArray("exercises"))
            day.optJSONArray("blocks")?.let { blocks -> for (blockIndex in 0 until blocks.length()) collect(blocks.optJSONObject(blockIndex)?.optJSONArray("exercises")) }
            val completedAt = runCatching { java.time.Instant.parse(day.optString("completed_at")).toEpochMilli() }.getOrNull()
            val workoutId = day.optString("id").nullableRemoteIdentifier()
            if (completedAt != null && workoutId != null) {
                legacyResultTimestamps += results.map { it.timestampMillis }
                val volume = results.sumOf { entry -> entry.setReps.mapIndexed { set, reps -> reps * entry.setWeightsKg.getOrElse(set) { entry.maxWeightKg } }.sum() }
                sessions += TrainingSessionHistoryEntry(
                    completedAt,
                    day.optInt("active_duration_seconds").takeIf { it > 0 } ?: results.sumOf { it.durationSeconds },
                    results.sumOf { it.sets },
                    day.optDouble("total_volume_kg", volume).takeIf { it.isFinite() } ?: volume,
                    day.optString("focus").takeIf { it.isNotBlank() && it != "null" },
                    workoutId,
                )
            }
        }
        WorkoutHistoryStore.importRemoteEntries(context, exerciseEntries)
        TrainingSessionHistoryStore.importRemote(context.getSharedPreferences("wildforce_profile", Context.MODE_PRIVATE), sessions, legacyResultTimestamps)
    }

    private fun remoteExercises(context: Context, entries: JSONArray?): JSONArray = JSONArray().apply {
        entries ?: return@apply
        for (index in 0 until entries.length()) {
            val exercise = entries.optJSONObject(index) ?: continue
            if (!exercise.isNull("deleted_at")) continue
            val min = exercise.opt("reps_min"); val max = exercise.opt("reps_max")
            val coachId = exercise.optString("coach_exercise_id").nullableRemoteIdentifier()
            val exerciseCatalogId = exercise.optString("exercise").trim().takeIf(String::isNotBlank)
            val coach = coachId?.let { CoachExerciseMediaStore.latest(context, "coach:$it") }
            val imageKey = coachId?.let { "coach:$it" } ?: exerciseCatalogId.orEmpty()
            val name = coach?.name ?: exerciseCatalogId ?: "Ejercicio"
            put(JSONObject().put("remoteId", exercise.optString("id")).put("name", name).put("imageKey", imageKey)
                .put("role", exercise.opt("role"))
                .put("coachExerciseId", coachId)
                .put("sets", exercise.optInt("sets", 1)).put("repsMin", min).put("repsMax", max)
                .put("reps", if (min is Number && max is Number) "${min.toInt()}-${max.toInt()}" else "")
                .put("restSeconds", exercise.optInt("rest_seconds", 0)).put("targetWeightKg", exercise.opt("target_weight_kg"))
                .put("targetReps", exercise.opt("target_reps")).put("targetWeightsKg", exercise.opt("target_weights_kg"))
                .put("targetDurationMinutes", exercise.opt("target_duration_minutes")).put("targetDurationSeconds", exercise.opt("target_duration_seconds"))
                .put("targetDistanceKm", exercise.opt("target_distance_km")).put("targetPaceSecondsPerKm", exercise.opt("target_pace_seconds_per_km")).put("setStyle", androidSetStyle(exercise.optJSONObject("set_style_configuration")?.optString("style")))
                .put("skippedAt", exercise.opt("skipped_at"))
                .put("setStyleParameters", androidSetStyleParameters(exercise.optJSONObject("set_style_configuration")))
                .put("trackingMode", coach?.trackingMode?.androidTrackingMode() ?: JSONObject.NULL)
                .put("isPerSideLoad", coach?.isPerSideLoad ?: false))
        }
    }

    private fun exerciseReferenceKey(exercise: JSONObject): String = exercise.optString("coach_exercise_id")
        .nullableRemoteIdentifier()
        ?.let { "coach:$it" }
        ?: exercise.optString("exercise").trim().lowercase()

    /** `optString` turns a JSON null into the literal text "null". */
    private fun String.nullableRemoteIdentifier(): String? = trim().lowercase()
        .takeIf { it.isNotBlank() && it != "null" && it != "undefined" }

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

    private fun synchronizeWorkoutPlan(token: String, userId: String, profile: OnboardingProfile, now: String, context: Context, preferences: SharedPreferences, root: JSONObject, gymVersions: String? = null) {
        val gym = gymVersions != null
        val planId = WorkoutPlanGenerator.normalizeNullableString(root.opt("planId")) ?: preferences.getString(planKey, null) ?: stableId("$userId:${root.optString("planName")}").also { preferences.edit().putString(planKey, it).apply() }
        val syncState = context.getSharedPreferences(syncStatePrefs, Context.MODE_PRIVATE)
        val versions = (gymVersions ?: preferences.getString(WorkoutPlanVersions.KEY, null))?.let(::JSONObject) ?: JSONObject()
        fun versionedRecord(resource: String, id: String, key: String): JSONObject {
            val updatedAt = versions.optString(key).takeIf { runCatching { java.time.Instant.parse(it) }.isSuccess }
            val row = recordFromRemoteProjection(context, resource, id, updatedAt ?: now)
            if (updatedAt == null) row.put("_android_missing_local_version", true)
            return row
        }
        fun previouslyPushed(resource: String) = syncState.getString("$pushedRecordsPrefix$resource", "{}")
            ?.let(::JSONObject) ?: JSONObject()
        val previousDays = previouslyPushed("workout-days")
        val previousBlocks = previouslyPushed("workout-blocks")
        val previousExercises = previouslyPushed("planned-exercises")
        fun parentIds(records: JSONObject, key: String): Map<String, String?> = records.keys().asSequence().associateWith { records.optJSONObject(it)?.optString(key) }
        val historical = historicalWorkoutGraph(planId, parentIds(previousDays, "workout_plan_id"), parentIds(previousBlocks, "workout_day_id"), parentIds(previousExercises, "workout_block_id"))
        val pendingArchives = if (gym) emptySet() else preferences.getStringSet(plansToArchiveKey, emptySet()).orEmpty().toSet() - planId
        for (previousId in pendingArchives.filter { "workout-plans" in activePushResources }) {
            val archives = preferences.getString(planArchiveVersionsKey, "{}")?.let(::JSONObject) ?: JSONObject()
            val archivedAt = archives.optString(previousId).takeIf { runCatching { java.time.Instant.parse(it) }.isSuccess }
            if (archivedAt != null) {
                push(token, "workout-plans", JSONArray().put(recordFromRemoteProjection(context, "workout-plans", previousId, archivedAt).put("status", "archived")), preserveAllMissing = true, clearOnSuccess = false)
            } else {
                SyncDiagnostics.record(context, "workout", "Discarded legacy archive with unknown modification time")
            }
            val latestArchives = preferences.getString(planArchiveVersionsKey, "{}")?.let(::JSONObject) ?: JSONObject()
            if (latestArchives.optString(previousId).takeIf(String::isNotBlank) == archivedAt) {
                latestArchives.remove(previousId)
                preferences.edit().putStringSet(plansToArchiveKey, preferences.getStringSet(plansToArchiveKey, emptySet()).orEmpty() - previousId)
                    .putString(planArchiveVersionsKey, latestArchives.toString()).apply()
            }
        }
        if (!gym) preferences.edit().putString(planKey, planId).apply()
        if (!gym) push(token, "workout-plans", JSONArray().put(versionedRecord("workout-plans", planId, WorkoutPlanVersions.planKey(root))
            .put("name", root.optString("planName", "Plan Android")).put("goal", root.optString("planGoal", profile.goal.storedValue)).put("status", root.optString("planStatus", "active"))
            .put("phase", WorkoutPlanGenerator.normalizeNullableString(root.opt("phase")) ?: JSONObject.NULL).put("mesocycle_number", root.optInt("mesocycleNumber", 1))
            .put("phase_week", root.optInt("phaseWeek", 1)).put("cycle_length", root.optInt("cycleLength", 1))
            .put("body_composition_phase", profile.bodyCompositionPhase?.storedValue ?: JSONObject.NULL)
            .put("notes", root.opt("planNotes") ?: JSONObject.NULL)
            .put("starts_on", root.opt("planStartsOn") ?: JSONObject.NULL)
            .put("profile_settings_hash", root.opt("profileSettingsHash") ?: JSONObject.NULL)), preserveAllMissing = true)
        val days = root.optJSONArray("workouts") ?: JSONArray(); val dayRecords = JSONArray(); val blocks = JSONArray(); val exercises = JSONArray(); val plannedExerciseReferences = mutableListOf<PlannedExerciseSyncReference>()
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
            dayRecords.put(versionedRecord("workout-days", dayId, WorkoutPlanVersions.dayKey(root, day, dayIndex)).apply { if (!gym) put("workout_plan_id", planId) }.put("kind", day.optString("kind", "workout")).put("title", day.optString("title", "Sesión ${dayIndex + 1}")).put("focus", backendWorkoutFocus(day.optString("focus"))).put("status", backendWorkoutStatus(day.optString("status", "planned"))).put("did_count_toward_streak", day.optBoolean("didCountTowardStreak", false)).apply { if (!gym) put("order_index", dayIndex).put("intended_weekday", day.optString("weekday").lowercase()) }.put("day_type", WorkoutPlanGenerator.normalizeNullableString(day.opt("dayType")) ?: JSONObject.NULL).put("creation_source", day.optString("creationSource", "generated")).put("estimated_duration_minutes", day.optInt("estimatedDurationMinutes", 50)).apply {
                // Mirror iOS's optional Date encoding: an absent date is not
                // sent. `optString` turns JSON null into the text "null", so
                // normalize it before constructing the sync payload.
                listOf("scheduledFor" to "scheduled_for", "startedAt" to "started_at", "completedAt" to "completed_at", "notes" to "notes").forEach { (local, remote) ->
                    val value = WorkoutPlanGenerator.normalizeNullableString(day.opt(local))
                    if (value != null) put(remote, value) else if (gym) put(remote, JSONObject.NULL)
                }
                listOf(
                    "activeDurationSeconds" to "active_duration_seconds",
                    "activeCaloriesBurned" to "active_calories_burned",
                    "averageHeartRate" to "average_heart_rate",
                    "maximumHeartRate" to "maximum_heart_rate",
                    "totalVolumeKg" to "total_volume_kg",
                ).forEach { (local, remote) ->
                    if (day.has(local) && !day.isNull(local)) put(remote, day.opt(local)) else if (gym) put(remote, JSONObject.NULL)
                }
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
                blocks.put(versionedRecord("workout-blocks", blockId, WorkoutPlanVersions.blockKey(root, day, dayIndex, block, blockIndex)).put("workout_day_id", dayId).put("type", block.optString("type", "standard")).put("order_index", blockIndex).put("rounds", block.optInt("rounds", 1)).put("rest_after_block_seconds", block.opt("restAfterBlockSeconds")).put("notes", block.opt("notes")))
                val entries = block.optJSONArray("exercises") ?: JSONArray()
                for (exerciseIndex in 0 until entries.length()) {
                    val item = entries.getJSONObject(exerciseIndex); val exerciseId = item.optString("remoteId").takeIf(::isUuid) ?: stableId("$blockId:$exerciseIndex")
                    val coachExerciseId = item.optString("coachExerciseId").nullableRemoteIdentifier()
                    val historyKey = coachExerciseId?.let { "coach:$it" }
                        ?: item.optString("imageKey").trim().ifBlank { item.optString("name").trim() }.lowercase()
                    if (historyKey.isNotBlank()) plannedExerciseReferences += PlannedExerciseSyncReference(localWorkoutId, historyKey, exerciseId)
                    // Catalog and coach references are mutually exclusive, as
                    // on iOS. The API deliberately receives the null catalog
                    // field when a coach exercise is selected.
                    val exerciseCatalogId = item.optString("imageKey").trim()
                    require(coachExerciseId != null || exerciseCatalogId.isNotBlank()) { "El ejercicio ${item.optString("name", exerciseIndex.toString())} no tiene identificador de catálogo." }
                    val skippedKeys = workoutBlockExecutionIndices(
                        executionIndex, entries.length(), block.optInt("rounds", 1), block.optString("type"), exerciseIndex,
                    ).map { "$localWorkoutId|$it" }
                    val record = versionedRecord("planned-exercises", exerciseId, WorkoutPlanVersions.exerciseKey(root, day, dayIndex, block, blockIndex, item, exerciseIndex)).put("workout_day_id", dayId).put("workout_block_id", blockId)
                        .put("exercise", if (coachExerciseId == null) exerciseCatalogId else JSONObject.NULL)
                        .put("coach_exercise_id", coachExerciseId ?: JSONObject.NULL)
                        .put("role", item.opt("role") ?: JSONObject.NULL)
                        .put("skipped_at", item.opt("skippedAt") ?: JSONObject.NULL)
                        .put("order_index", exerciseIndex).put("sets", item.optInt("sets", 1)).put("reps_min", item.opt("repsMin")).put("reps_max", item.opt("repsMax")).put("target_reps", item.opt("targetReps")).put("target_weight_kg", item.opt("targetWeightKg")).put("target_weights_kg", item.opt("targetWeightsKg")).put("target_duration_minutes", item.opt("targetDurationMinutes")).put("target_duration_seconds", item.opt("targetDurationSeconds")).put("target_distance_km", item.opt("targetDistanceKm")).put("target_pace_seconds_per_km", item.opt("targetPaceSecondsPerKm")).put("rest_seconds", item.optInt("restSeconds", 0)).put("set_style_configuration", backendSetStyleConfiguration(item))
                    item.optString("role").takeIf { it == "main" || it == "accessory" }?.let { record.put("role", it) }
                    skippedKeys.mapNotNull(skippedExercises::get).minOrNull()?.let { timestamp ->
                        val skippedAt = java.time.Instant.ofEpochMilli(timestamp)
                        record.put("skipped_at", skippedAt.toString())
                        val version = runCatching { java.time.Instant.parse(record.optString("updated_at")) }.getOrNull()
                        if (!record.optBoolean("_android_missing_local_version") && (version == null || skippedAt > version)) {
                            record.put("updated_at", skippedAt.toString())
                        }
                        uploadedSkippedExerciseKeys += skippedKeys.filter(skippedExercises::containsKey)
                    }
                    exercises.put(record)
                }
                executionIndex += workoutBlockExecutionCount(entries.length(), block.optInt("rounds", 1), block.optString("type"))
            }
        }
        fun changedGymRows(resource: String, records: JSONArray): JSONArray {
            val cached = context.getSharedPreferences(syncStatePrefs, Context.MODE_PRIVATE)
                .getString("remote_projection:$resource", "[]")?.let(::JSONArray) ?: JSONArray()
            val remote = (0 until cached.length()).mapNotNull(cached::optJSONObject).associateBy { it.optString("id").lowercase() }
            return JSONArray().apply {
                for (index in 0 until records.length()) {
                    val row = records.optJSONObject(index) ?: continue
                    val old = remote[row.optString("id").lowercase()]
                    if (shouldPushGymRow(row.optString("updated_at"), old?.optString("updated_at"))) put(row)
                }
            }
        }
        if (!gym) push(token, "workout-days", dayRecords, preserveMissingIds = historical.days)
        push(token, "workout-blocks", if (gym) changedGymRows("workout-blocks", blocks) else blocks, preserveMissingIds = historical.blocks, preserveAllMissing = gym, clearOnSuccess = !gym, compareRemote = gym)
        push(token, "planned-exercises", if (gym) changedGymRows("planned-exercises", exercises) else exercises, preserveMissingIds = historical.exercises, preserveAllMissing = gym, clearOnSuccess = !gym, compareRemote = gym)
        if (gym) push(token, "workout-days", changedGymRows("workout-days", dayRecords), preserveAllMissing = true, clearOnSuccess = false, compareRemote = true)
        if (uploadedSkippedExerciseKeys.isNotEmpty()) clearSkippedExercises(context, uploadedSkippedExerciseKeys)
        val results = JSONArray()
        val uploadedLocalEdits = mutableListOf<Triple<io.codepassion.doubletriangle.feature.workout.RemoteExerciseHistoryEntry, String, String>>()
        WorkoutHistoryStore.remoteEntries(context = context).forEach { entry ->
            if (entry.remoteResultId != null) return@forEach
            val plannedExerciseId = resolvePlannedExerciseId(entry.workoutId, entry.exerciseKey, entry.plannedExerciseId, plannedExerciseReferences) ?: return@forEach
            val resultId = stableId("$plannedExerciseId:${entry.timestampMillis}")
            if (entry.pendingEditAt > 0) uploadedLocalEdits += Triple(entry, resultId, plannedExerciseId)
            val result = record(resultId, java.time.Instant.ofEpochMilli(entry.timestampMillis).toString())
                .put("planned_exercise_id", plannedExerciseId).put("feedback", backendExerciseFeedback(entry.feedback))
                .put("completed_at", java.time.Instant.ofEpochMilli(entry.timestampMillis).toString())
                .put("completed_sets", entry.sets).put("completed_reps", entry.totalReps).put("completed_weight", entry.maxWeightKg.finiteOrZero())
                .put("per_set_reps", JSONArray(entry.setReps)).put("per_set_weights_kg", JSONArray(entry.setWeightsKg.map { it.finiteOrZero() }))
                .put("completed_duration_seconds", entry.durationSeconds).put("completed_duration_minutes", entry.durationSeconds / 60)
                .put("completed_distance_km", entry.distanceKm.finiteOrZero()).put("notes", entry.note ?: JSONObject.NULL)
            if (entry.pendingEditAt > 0) {
                result.put("created_at", java.time.Instant.ofEpochMilli(entry.timestampMillis).toString())
                    .put("updated_at", java.time.Instant.ofEpochMilli(entry.pendingEditAt).toString())
                applyExerciseResultEdit(result, entry)
            }
            results.put(result)
        }
        // This call only sees one workout graph at a time. Results from the
        // other graph are history, not deletions.
        push(token, "exercise-results", results, preserveAllMissing = true, clearOnSuccess = !gym)
        if ("exercise-results" in activePushResources) {
            uploadedLocalEdits.forEach { (entry, id, plannedId) -> WorkoutHistoryStore.claimUploadedResult(context, entry, id, plannedId) }
        }
    }

    private fun record(id: String, now: String) = JSONObject().put("id", id).put("created_at", now).put("updated_at", now)

    private fun synchronizeEditedExerciseResults(context: Context, token: String, now: String) {
        val edits = WorkoutHistoryStore.remoteEntries(context).filter { it.pendingEditAt > 0 && it.remoteResultId != null && it.plannedExerciseId != null }
        edits.forEach { entry ->
            val id = requireNotNull(entry.remoteResultId)
            val payload = recordFromRemoteProjection(context, "exercise-results", id, now)
                .put("planned_exercise_id", entry.plannedExerciseId)
                .put("updated_at", java.time.Instant.ofEpochMilli(entry.pendingEditAt).toString())
            if (!applyExerciseResultEdit(payload, entry)) return@forEach
            val response = pushBatchWithResponse(token, "exercise-results", JSONArray().put(payload))
            val serverRecord = response.optJSONArray("data")?.optJSONObject(0)
                ?: error("No se recibió el resultado sincronizado.")
            if (!serverRecord.optString("id").equals(id, ignoreCase = true)) error("La respuesta no corresponde al resultado editado.")
            val serverUpdatedAt = runCatching { java.time.Instant.parse(serverRecord.optString("updated_at")).toEpochMilli() }.getOrNull()
                ?: error("El resultado sincronizado no tiene fecha válida.")
            if (serverUpdatedAt > entry.pendingEditAt) error("El resultado se ha actualizado en otro dispositivo.")
            updateRemoteProjectionAfterPush("exercise-results", JSONObject().put(id, payload), JSONObject(), now)
            WorkoutHistoryStore.acknowledgeEdit(context, entry)
        }
    }

    private fun applyExerciseResultEdit(payload: JSONObject, entry: io.codepassion.doubletriangle.feature.workout.RemoteExerciseHistoryEntry): Boolean {
            when (entry.pendingEditKind) {
                "reps", "weightedReps" -> payload.put("completed_sets", entry.sets)
                    .put("per_set_reps", JSONArray(entry.setReps))
                    .put("completed_reps", entry.setReps.lastOrNull() ?: JSONObject.NULL)
                    .put("per_set_weights_kg", if (entry.pendingEditKind == "weightedReps") JSONArray(entry.setWeightsKg) else JSONObject.NULL)
                    .put("completed_weight", if (entry.pendingEditKind == "weightedReps") entry.setWeightsKg.lastOrNull() ?: JSONObject.NULL else JSONObject.NULL)
                "seconds" -> payload.put("completed_duration_seconds", entry.durationSeconds.takeIf { it > 0 } ?: JSONObject.NULL)
                "minutes", "distance" -> {
                    val minutes = entry.durationSeconds / 60
                    payload.put("completed_duration_minutes", minutes.takeIf { it > 0 } ?: JSONObject.NULL)
                    if (entry.pendingEditKind == "distance") {
                        payload.put("completed_distance_km", entry.distanceKm.takeIf { it > 0 } ?: JSONObject.NULL)
                        payload.put("completed_pace_seconds_per_km", if (minutes > 0 && entry.distanceKm > 0) kotlin.math.round(minutes * 60 / entry.distanceKm).toInt() else JSONObject.NULL)
                    }
                }
                else -> return false
            }
            return true
    }

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
    private fun push(token: String, resource: String, records: JSONArray, preserveMissingIds: Set<String> = emptySet(), preserveAllMissing: Boolean = false, clearOnSuccess: Boolean = true, compareRemote: Boolean = false) {
        if (resource !in activePushResources) return
        val state = activePushContext?.getSharedPreferences(syncStatePrefs, Context.MODE_PRIVATE)
            ?: return
        val previous = state.getString("$pushedRecordsPrefix$resource", "{}")
            ?.let(::JSONObject) ?: JSONObject()
        val current = JSONObject()
        val changes = JSONArray()
        val remoteRows = state.getString("remote_projection:$resource", "[]")?.let(::JSONArray) ?: JSONArray()
        val remoteById = (0 until remoteRows.length()).mapNotNull(remoteRows::optJSONObject)
            .associateBy { it.optString("id").lowercase(java.util.Locale.ROOT) }
        for (index in 0 until records.length()) {
            val source = records.optJSONObject(index) ?: continue
            val record = JSONObject(source.toString())
            val missingVersion = record.optBoolean("_android_missing_local_version", false)
            val missingNutritionVersion = record.optBoolean("_android_missing_nutrition_version", false)
            record.remove("_android_missing_local_version")
            record.remove("_android_missing_nutrition_version")
            val id = record.optString("id").takeIf(String::isNotBlank) ?: continue
            val previousRecord = previous.optJSONObject(id)
            val remote = remoteById[id.lowercase(java.util.Locale.ROOT)]
            if (resource == "nutrition-log-items" && remote != null &&
                !nutritionItemBelongsToParent(remote.optString("nutrition_log_entry_id"), record.optString("nutrition_log_entry_id"))) {
                // A stale Android index can point to another entry's child.
                // Never move that server row to a different parent.
                current.put(id, JSONObject(remote.toString()))
                activeReconcileNutritionRows = true
                continue
            }
            if (resource == "body-progress-photo-sessions" && remote != null &&
                shouldApplyRemoteRecord(record.optString("updated_at"), remote.optString("updated_at"))) {
                current.put(id, JSONObject(remote.toString()))
                continue
            }
            // iOS applies a newer downloaded row before collecting local changes.
            // Reconcile each workout row by its own timestamp, including tombstones.
            if (!missingVersion && resource in workoutResources && remote != null &&
                shouldApplyRemoteRecord(record.optString("updated_at"), remote.optString("updated_at"))) {
                current.put(id, JSONObject(remote.toString()))
                activeDiscardedLegacyWorkoutRows = true
                continue
            }
            val remoteMatches = remote != null && remote.isNull("deleted_at") && syncFingerprint(remote) == syncFingerprint(record)
            if (resource in (nutritionPlanResources + nutritionLogResources - "nutrition-log-media") && remote != null &&
                (missingNutritionVersion || shouldApplyRemoteRecord(record.optString("updated_at"), remote.optString("updated_at")))) {
                current.put(id, JSONObject(remote.toString()))
                if (!remoteMatches) activeReconcileNutritionRows = true
                continue
            }
            if (missingNutritionVersion) {
                error("El plan de nutrición contiene cambios antiguos sin fecha y sin copia remota; se conservan pendientes.")
            }
            when (legacyWorkoutRecordAction(!missingVersion, remote != null, remoteMatches)) {
                LegacyWorkoutRecordAction.KeepPending -> error("No se puede recuperar un registro antiguo de entrenamiento desde la cuenta; se conserva pendiente.")
                LegacyWorkoutRecordAction.UseRemote, LegacyWorkoutRecordAction.UseMatchingRemote -> {
                    current.put(id, JSONObject(requireNotNull(remote).toString()))
                    if (!remoteMatches) activeDiscardedLegacyWorkoutRows = true
                    continue
                }
                LegacyWorkoutRecordAction.UploadVersioned -> Unit
            }
            current.put(id, record)
            if (previousRecord == null || syncFingerprint(previousRecord) != syncFingerprint(record) || (compareRemote && !remoteMatches)) {
                changes.put(record)
            }
        }
        // A plan projection contains only the current plan. Missing records
        // from other plans remain valid history and must not become tombstones.
        previous.keys().forEach { id ->
            if (!current.has(id) && (preserveAllMissing || id in preserveMissingIds)) {
                previous.optJSONObject(id)?.let { current.put(id, it) }
            }
        }
        // A result imported after its first upload is no longer emitted as a
        // local creation. Its disappearance from that list is not a deletion.
        if (resource == "exercise-results") {
            val importedIds = WorkoutHistoryStore.remoteEntries(requireNotNull(activePushContext))
                .mapNotNull { it.remoteResultId }.toSet()
            previous.keys().forEach { id ->
                if (!current.has(id) && id in importedIds) previous.optJSONObject(id)?.let { current.put(id, it) }
            }
        }
        // Android projections do not retain deleted rows. Preserve their last
        // accepted payload and turn a disappearance into the same tombstone
        // iOS sends through SwiftData.
        val deletedAt = java.time.Instant.now().toString()
        val metricDeletionTimes = if (resource == "body-metric-entries") {
            activePushContext?.getSharedPreferences("wildforce_body_metrics", Context.MODE_PRIVATE)
                ?.getString(BodyMetricsStore.PENDING_DELETIONS_KEY, "{}")?.let(::JSONObject) ?: JSONObject()
        } else JSONObject()
        val locationDeletionTimes = if (resource == "training-locations") {
            activePushContext?.getSharedPreferences(syncPrefs, Context.MODE_PRIVATE)
                ?.getString(pendingLocationDeletionsKey, "{}")?.let(::JSONObject) ?: JSONObject()
        } else JSONObject()
        val nutritionDeletionTimes = if (resource == "nutrition-log-entries" || resource == "nutrition-log-items") {
            activePushContext?.getSharedPreferences("wildforce_nutrition", Context.MODE_PRIVATE)
                ?.getString(NutritionStore.PENDING_DELETIONS_KEY, "{}")?.let(::JSONObject) ?: JSONObject()
        } else JSONObject()
        val bodyDeletionTimes = if (resource == "body-progress-photo-sessions") {
            val context = requireNotNull(activePushContext)
            val userId = context.getSharedPreferences(accountPrefs, Context.MODE_PRIVATE).getString("id", "").orEmpty()
            val local = context.getSharedPreferences("wildforce_body_progress", Context.MODE_PRIVATE)
                .getString("pending_session_deletions_v1", "{}")?.let(::JSONObject) ?: JSONObject()
            JSONObject().apply { local.keys().forEach { id -> put(bodyProgressRemoteID(context, userId, id).lowercase(java.util.Locale.ROOT), local.optString(id)) } }
        } else JSONObject()
        val itemDeletionTimes = if (resource == "nutrition-log-items") {
            activePushContext?.getSharedPreferences("wildforce_nutrition", Context.MODE_PRIVATE)
                ?.getString(NutritionStore.PENDING_ITEM_DELETIONS_KEY, "{}")?.let(::JSONObject) ?: JSONObject()
        } else JSONObject()
        previous.keys().forEach { id ->
            if (!current.has(id)) {
                previous.optJSONObject(id)?.let { prior ->
                    val entryId = if (resource == "nutrition-log-entries") id else prior.optString("nutrition_log_entry_id")
                    val localTime = metricDeletionTimes.optString(id.lowercase()).ifBlank { bodyDeletionTimes.optString(id.lowercase()) }.ifBlank { locationDeletionTimes.optString(id.lowercase()) }
                        .ifBlank { itemDeletionTimes.optString(id.lowercase()) }
                        .ifBlank { nutritionDeletionTimes.optString(entryId.lowercase()) }
                    if ((resource == "body-progress-photo-sessions" && !shouldSendBodyPhotoTombstone(localTime)) ||
                        !shouldSendNutritionTombstone(resource, localTime)) {
                        // Absence from Android's projection is not a deletion.
                        // iOS only pushes rows carrying an explicit tombstone.
                        current.put(id, prior)
                        return@let
                    }
                    val timestamp = localTime.takeIf { runCatching { java.time.Instant.parse(it) }.isSuccess } ?: deletedAt
                    changes.put(JSONObject(prior.toString())
                        .put("updated_at", timestamp)
                        .put("deleted_at", timestamp))
                }
            }
        }
        SyncDiagnostics.record(requireNotNull(activePushContext), "push", "$resource: local=${records.length()}, sending=${changes.length()}, protected=${preserveMissingIds.size}, acknowledge=$clearOnSuccess")
        if (changes.length() == 0) {
            state.edit().putString("$pushedRecordsPrefix$resource", current.toString()).apply()
            if (clearOnSuccess) clearDirtyResource(resource)
            return
        }
        for (start in 0 until changes.length() step 100) {
            val batch = JSONArray()
            for (index in start until minOf(start + 100, changes.length())) batch.put(changes.get(index))
            if (resource in workoutResources || resource == "body-metric-entries" ||
                resource in setOf("users", "training-preferences", "training-locations", "user-app-settings", "nutrition-profiles", "nutrition-log-entries", "nutrition-log-items")) {
                val response = pushBatchWithResponse(token, resource, batch)
                // iOS accepts any successful HTTP response. When rows are returned,
                // retain Android's extra conflict check; a body is not required.
                response.optJSONArray("data")?.let { returned ->
                    val byId = buildMap {
                        for (index in 0 until returned.length()) {
                            returned.optJSONObject(index)?.let { row -> put(row.optString("id").lowercase(), row) }
                        }
                    }
                    for (index in 0 until batch.length()) {
                        val sent = batch.getJSONObject(index)
                        val received = byId[sent.optString("id").lowercase()]
                            ?: error("Falta un registro en la respuesta de sincronización.")
                        if (serverHasNewerWorkoutRecord(sent.getString("updated_at"), received.getString("updated_at"))) {
                            // Keep the authoritative row returned by the server.
                            // iOS treats a successful batch as accepted and the
                            // next pull applies newer versions individually.
                            val id = sent.getString("id")
                            current.put(id, JSONObject(received.toString()))
                            if (resource in workoutResources) activeDiscardedLegacyWorkoutRows = true
                            if (resource in nutritionPlanResources || resource in nutritionLogResources) activeReconcileNutritionRows = true
                        }
                    }
                }
            } else pushBatch(token, resource, batch)
        }
        // iOS applies its own accepted writes to SwiftData immediately. Without
        // this equivalent, the next Android pull can materialize an older
        // cached User row and overwrite a just-saved weight before the server
        // happens to echo that row back through the pull watermark.
        val sentCurrent = JSONObject()
        val sentDeletions = JSONObject()
        for (index in 0 until changes.length()) {
            val id = changes.optJSONObject(index)?.optString("id") ?: continue
            if (current.has(id)) current.optJSONObject(id)?.let { sentCurrent.put(id, it) }
            else changes.optJSONObject(index)?.let { sentDeletions.put(id, it) }
        }
        updateRemoteProjectionAfterPush(resource, sentCurrent, sentDeletions, deletedAt)
        if (resource == "body-progress-photo-sessions" && sentDeletions.length() > 0) {
            val context = requireNotNull(activePushContext)
            val userId = context.getSharedPreferences(accountPrefs, Context.MODE_PRIVATE).getString("id", "").orEmpty()
            val prefs = context.getSharedPreferences("wildforce_body_progress", Context.MODE_PRIVATE)
            val pending = prefs.getString("pending_session_deletions_v1", "{}")?.let(::JSONObject) ?: JSONObject()
            pending.keys().asSequence().toList().forEach { localId ->
                val remoteId = bodyProgressRemoteID(context, userId, localId)
                val sent = sentDeletions.optJSONObject(remoteId)?.optString("deleted_at")
                if (sent != null && pending.optString(localId) == sent) pending.remove(localId)
            }
            prefs.edit().putString("pending_session_deletions_v1", pending.toString()).apply()
        }
        if (resource == "body-metric-entries" && sentDeletions.length() > 0) {
            synchronized(BodyMetricsStore) {
                val prefs = requireNotNull(activePushContext).getSharedPreferences("wildforce_body_metrics", Context.MODE_PRIVATE)
                val pending = prefs.getString(BodyMetricsStore.PENDING_DELETIONS_KEY, "{}")?.let(::JSONObject) ?: JSONObject()
                sentDeletions.keys().forEach { id ->
                    val sent = sentDeletions.optJSONObject(id)?.optString("deleted_at")
                    if (sent != null && pending.optString(id.lowercase()) == sent) pending.remove(id.lowercase())
                }
                prefs.edit().putString(BodyMetricsStore.PENDING_DELETIONS_KEY, pending.toString()).apply()
            }
        }
        if (resource == "training-locations" && sentDeletions.length() > 0) {
            val sync = requireNotNull(activePushContext).getSharedPreferences(syncPrefs, Context.MODE_PRIVATE)
            val pending = sync.getString(pendingLocationDeletionsKey, "{}")?.let(::JSONObject) ?: JSONObject()
            sentDeletions.keys().forEach { id ->
                val sent = sentDeletions.optJSONObject(id)?.optString("deleted_at")
                if (sent != null && pending.optString(id.lowercase()) == sent) pending.remove(id.lowercase())
            }
            sync.edit().putString(pendingLocationDeletionsKey, pending.toString()).apply()
        }
        if (resource == "nutrition-log-items" && sentDeletions.length() > 0) {
            synchronized(NutritionStore) {
                val prefs = requireNotNull(activePushContext).getSharedPreferences("wildforce_nutrition", Context.MODE_PRIVATE)
                val pending = prefs.getString(NutritionStore.PENDING_ITEM_DELETIONS_KEY, "{}")?.let(::JSONObject) ?: JSONObject()
                sentDeletions.keys().forEach { id ->
                    val sent = sentDeletions.optJSONObject(id)?.optString("deleted_at")
                    if (sent != null && pending.optString(id.lowercase()) == sent) pending.remove(id.lowercase())
                }
                prefs.edit().putString(NutritionStore.PENDING_ITEM_DELETIONS_KEY, pending.toString()).apply()
            }
        }
        state.edit().putString("$pushedRecordsPrefix$resource", current.toString()).apply()
        if (clearOnSuccess) clearDirtyResource(resource)
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
            ?.let(::JSONArray) ?: JSONArray()
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
                byId[id] = JSONObject(prior.toString()).apply {
                    if (isNull("deleted_at")) {
                        put("deleted_at", deletedAt)
                        put("updated_at", deletedAt)
                    }
                }
            }
        }
        state.edit().putString("remote_projection:$resource", JSONArray().apply { byId.values.forEach(::put) }.toString()).apply()
        activeProjectionCache.remove(resource)
    }

    /** Ignore JSON key order and numeric formatting as well as transport timestamps. */
    private fun syncFingerprint(record: JSONObject): String = canonicalSyncJson(JSONObject(record.toString()).apply {
        remove("created_at")
        remove("updated_at")
        remove("deleted_at")
    })

    private fun canonicalSyncJson(value: Any?): String = when (value) {
        null, JSONObject.NULL -> "null"
        is JSONObject -> value.keys().asSequence().toList().sorted().joinToString(",", "{", "}") { key ->
            JSONObject.quote(key) + ":" + canonicalSyncJson(value.opt(key))
        }
        is JSONArray -> (0 until value.length()).joinToString(",", "[", "]") { canonicalSyncJson(value.opt(it)) }
        is Number -> canonicalSyncNumber(value)
        is Boolean -> value.toString()
        else -> JSONObject.quote(value.toString())
    }

    private fun pushBatch(token: String, resource: String, records: JSONArray) {
        pushBatchWithResponse(token, resource, records)
    }

    private fun diagnosticRequestId(connection: HttpURLConnection): String {
        val id = (connection.getHeaderField("X-Request-ID") ?: connection.getHeaderField("Traceparent"))?.take(100).orEmpty()
        return if (id.matches(Regex("[A-Za-z0-9_.-]+"))) ", request=$id" else ""
    }

    private fun pushBatchWithResponse(token: String, resource: String, records: JSONArray): JSONObject {
        val started = android.os.SystemClock.elapsedRealtime()
        val connection = (URL(WildforceApiEnvironment.apiUrl("sync/push")).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"; doOutput = true; connectTimeout = 15_000; readTimeout = 15_000
            setRequestProperty("Accept", "application/json"); setRequestProperty("Content-Type", "application/json"); setRequestProperty("Authorization", "Bearer $token")
        }
        OutputStreamWriter(connection.outputStream).use { it.write(JSONObject().put("resource", resource).put("records", records).toString()) }
        val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
        val response = BufferedReader(stream.reader()).use { it.readText() }
        activePushContext?.let { context ->
            val serverCode = if (connection.responseCode !in 200..299) runCatching { JSONObject(response).optString("code").take(80) }.getOrNull().orEmpty() else ""
            SyncDiagnostics.record(context, "http", "POST sync/push $resource: HTTP ${connection.responseCode}, records=${records.length()}, ${android.os.SystemClock.elapsedRealtime() - started}ms${diagnosticRequestId(connection)}${if (serverCode.matches(Regex("[A-Za-z0-9_.-]+"))) ", code=$serverCode" else ""}")
        }
        if (connection.responseCode !in 200..299) {
            val error = apiException(response, "No se pudo sincronizar $resource")
            Log.w("WorkoutRemoteSync", "Sync resource $resource rejected with HTTP ${connection.responseCode}: ${error.message}")
            throw error
        }
        return runCatching { JSONObject(response) }.getOrElse { JSONObject() }
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
    private suspend fun pullRemoteSnapshot(context: Context, token: String, onProgress: suspend (Float) -> Unit, resources: Set<String>): RemoteSnapshot {
        val result = linkedMapOf<String, JSONArray>()
        val pullState = context.getSharedPreferences(syncStatePrefs, Context.MODE_PRIVATE)
        var request = JSONObject().put("resources", JSONArray().apply {
            batchResources.filter { it in resources }.forEach { resource ->
                val watermark = pullState.getString("$pullWatermarkPrefix$resource", null)
                put(JSONObject(syncBatchResource(resource, watermark)))
            }
        })
        var asOf: String? = null
        var downloadedCount = 0
        var pageCount = 0
        do {
            Log.d("WorkoutRemoteSync", "Requesting pull page ${pageCount + 1}")
            val response = postJson(context, token, WildforceApiEnvironment.apiUrl("sync/pull/batch"), request)
            val data = response.optJSONArray("data") ?: error("La respuesta de sincronización no contiene datos válidos.")
            val serverAsOf = response.optJSONObject("meta")?.optString("as_of")
                ?.let { runCatching { java.time.Instant.parse(it) }.getOrNull() }
                ?: error("La respuesta de sincronización no contiene una fecha válida.")
            if (asOf == null) asOf = serverAsOf.toString()
            for (index in 0 until data.length()) {
                val item = data.optJSONObject(index) ?: error("Elemento de sincronización inválido")
                val resource = item.optString("resource").takeIf(String::isNotBlank)
                    ?: error("Elemento de sincronización sin recurso")
                if (resource !in resources) continue
                val record = item.optJSONObject("record") ?: error("Registro de sincronización inválido: $resource")
                check(isUuid(record.optString("id")) &&
                    runCatching { java.time.Instant.parse(record.optString("updated_at")) }.isSuccess) {
                    "Registro de sincronización sin identidad o fecha válida: $resource"
                }
                result.getOrPut(resource, ::JSONArray).put(record)
            }
            downloadedCount += data.length()
            pageCount++
            Log.d("WorkoutRemoteSync", "Pull page=$pageCount records=${data.length()} total=$downloadedCount")
            SyncDiagnostics.record(context, "pull", "Page $pageCount: ${data.length()} records, total=$downloadedCount")
            val total = response.optJSONObject("meta")?.optInt("total", 0) ?: 0
            if (total > 0) onProgress((downloadedCount.toFloat() / total).coerceIn(0f, 1f))
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
        SyncDiagnostics.record(context, "pull", "Completed: ${resources.sorted().joinToString { "$it=${result[it]?.length() ?: 0}" }}")
        return RemoteSnapshot(result, asOf, resources)
    }

    private suspend fun postJson(context: Context, token: String, url: String, body: JSONObject): JSONObject {
        while (true) {
            val started = android.os.SystemClock.elapsedRealtime()
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"; doOutput = true; connectTimeout = 20_000; readTimeout = 30_000
                setRequestProperty("Accept", "application/json"); setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer $token")
            }
            OutputStreamWriter(connection.outputStream).use { it.write(body.toString()) }
            val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
            val response = BufferedReader(stream.reader()).use { it.readText() }
            val retrySeconds = connection.getHeaderField("Retry-After")?.toDoubleOrNull()
            val serverCode = if (connection.responseCode !in 200..299) runCatching { JSONObject(response).optString("code").take(80) }.getOrNull().orEmpty() else ""
            SyncDiagnostics.record(context, "http", "POST sync/pull/batch: HTTP ${connection.responseCode}, ${android.os.SystemClock.elapsedRealtime() - started}ms${diagnosticRequestId(connection)}${retrySeconds?.let { ", retry-after=${it}s" }.orEmpty()}${if (serverCode.matches(Regex("[A-Za-z0-9_.-]+"))) ", code=$serverCode" else ""}")
            if (connection.responseCode == 429 && retrySeconds != null && retrySeconds.isFinite() && retrySeconds >= 0) {
                Log.w("WorkoutRemoteSync", "Pull rate limited; retry in $retrySeconds seconds")
                connection.disconnect()
                delay((retrySeconds * 1000).toLong())
                continue
            }
            if (connection.responseCode !in 200..299) throw apiException(response, "No se pudo descargar la cuenta")
            return JSONObject(response)
        }
    }

    private data class RemoteSnapshot(private val data: Map<String, JSONArray>, private val asOf: String?, private val pulledResources: Set<String>) {
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
                val cached = if (replaceSyncProjection(resource, resource in pulledResources)) JSONArray()
                    else preferences.getString("remote_projection:$resource", "[]")?.let(::JSONArray) ?: JSONArray()
                for (index in 0 until cached.length()) {
                    cached.optJSONObject(index)?.let { record ->
                        record.optString("id").takeIf(String::isNotBlank)?.let { byId[it] = record }
                    }
                }
                val changes = data[resource] ?: JSONArray()
                for (index in 0 until changes.length()) {
                    changes.optJSONObject(index)?.let { record ->
                        record.optString("id").takeIf(String::isNotBlank)?.let { id ->
                            val existing = byId[id]
                            if (existing == null || shouldApplyRemoteRecord(existing.optString("updated_at"), record.optString("updated_at"))) {
                                byId[id] = JSONObject(record.toString())
                            }
                        }
                    }
                }
                val records = JSONArray().apply { byId.values.forEach(::put) }
                full[resource] = records
                editor.putString("remote_projection:$resource", records.toString())
            }
            editor.apply()
            return RemoteSnapshot(full, asOf, pulledResources)
        }

        /** iOS overlaps one second because server timestamps have second precision. */
        fun savePullWatermarks(context: Context, failedResources: Set<String> = emptySet()) {
            val serverTime = asOf?.let { runCatching { java.time.Instant.parse(it) }.getOrNull() } ?: return
            val watermark = serverTime.minusSeconds(1).toString()
            context.getSharedPreferences(syncStatePrefs, Context.MODE_PRIVATE).edit().apply {
                pulledResources.filterNot(failedResources::contains).forEach { resource ->
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

    private fun JSONArray?.latestRecord(): JSONObject? {
        val records = this ?: return null
        return (0 until records.length()).mapNotNull(records::optJSONObject).maxByOrNull { record ->
            runCatching { java.time.Instant.parse(record.optString("updated_at")) }.getOrDefault(java.time.Instant.EPOCH)
        }
    }

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

internal suspend fun <T> duplicateThenPull(key: String, create: suspend (String) -> String, pull: suspend (String) -> T): T = pull(create(key))
