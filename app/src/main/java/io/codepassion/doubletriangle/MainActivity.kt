package io.codepassion.doubletriangle

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.TextButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.core.view.WindowCompat
import android.graphics.Color as AndroidColor
import io.codepassion.doubletriangle.core.model.PreviewWorkoutRepository
import io.codepassion.doubletriangle.core.model.WorkoutDaySummary
import io.codepassion.doubletriangle.core.model.WorkoutHubState
import io.codepassion.doubletriangle.core.model.WorkoutStatus
import io.codepassion.doubletriangle.core.model.isPlanFinalized
import io.codepassion.doubletriangle.core.designsystem.WildforceTheme
import io.codepassion.doubletriangle.core.designsystem.WildforceThemeTokens
import io.codepassion.doubletriangle.core.designsystem.liquidGlass
import io.codepassion.doubletriangle.core.designsystem.liquidGlassBackground
import io.codepassion.doubletriangle.feature.onboarding.BodyCompositionPhase
import io.codepassion.doubletriangle.feature.onboarding.Equipment
import io.codepassion.doubletriangle.feature.onboarding.FitnessGoal
import io.codepassion.doubletriangle.feature.onboarding.Gender
import io.codepassion.doubletriangle.feature.onboarding.GymType
import io.codepassion.doubletriangle.feature.onboarding.LifestyleLevel
import io.codepassion.doubletriangle.feature.onboarding.MetricSystem
import io.codepassion.doubletriangle.feature.onboarding.MovementRestriction
import io.codepassion.doubletriangle.feature.onboarding.TrainingLevel
import io.codepassion.doubletriangle.feature.onboarding.TrainingSplitPreference
import io.codepassion.doubletriangle.feature.onboarding.TrainingLocationProfile
import io.codepassion.doubletriangle.feature.onboarding.effectiveTrainingLocations
import io.codepassion.doubletriangle.feature.onboarding.WorkoutFocus
import io.codepassion.doubletriangle.feature.onboarding.WorkoutWeekday
import io.codepassion.doubletriangle.feature.onboarding.OnboardingProfile
import io.codepassion.doubletriangle.feature.onboarding.OnboardingPlanPreview
import io.codepassion.doubletriangle.feature.onboarding.OnboardingScreen
import io.codepassion.doubletriangle.feature.onboarding.ProfileScreen
import io.codepassion.doubletriangle.feature.workout.ActiveWorkoutScreen
import io.codepassion.doubletriangle.feature.workout.CompletionProgressStore
import io.codepassion.doubletriangle.feature.workout.WorkoutDetailScreen
import io.codepassion.doubletriangle.feature.workout.CustomWorkoutStore
import io.codepassion.doubletriangle.feature.workout.WorkoutHubScreen
import io.codepassion.doubletriangle.feature.workout.WorkoutAnalyticsStore
import io.codepassion.doubletriangle.feature.workout.WorkoutSessionStore
import io.codepassion.doubletriangle.feature.workout.RemoteTrainingImage
import io.codepassion.doubletriangle.feature.workout.exerciseImageUrl
import io.codepassion.doubletriangle.nutrition.NutritionScreen
import java.time.Instant
import java.io.File
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val profileLanguage = getSharedPreferences("wildforce_profile", 0).getString("app_language", null)
        val onboardingLanguage = getSharedPreferences("wildforce_profile_details", 0).getString("language", "Español")
        AppLocale.apply(profileLanguage ?: onboardingLanguage)
        // Opt into one consistent edge-to-edge policy. Without this, Android
        // applies system-bar insets differently depending on the OEM and
        // navigation mode (notably on Galaxy S23 vs Pixel). Composables below
        // own the top and bottom insets explicitly.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 7203)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && checkSelfPermission(Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                arrayOf(Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_CONNECT),
                7205,
            )
        }
        setContent {
            val systemDarkTheme = isSystemInDarkTheme()
            val savedTheme = remember { getSharedPreferences("wildforce_profile_details", 0).getString("theme", "Sistema") ?: "Sistema" }
            WildforceTheme(darkTheme = when (savedTheme) { "Claro" -> false; "Oscuro" -> true; else -> systemDarkTheme }) {
                // Apply only the top inset here. The bottom navigation owns the
                // navigation-bar inset; applying safeDrawing at the root as
                // well created an extra bottom gap on devices such as S23 Ultra.
                Box(Modifier.fillMaxSize().statusBarsPadding()) {
                    WildforceRoot(onThemeChanged = { recreate() })
                }
            }
        }
    }
}

private enum class RootDestination(@param:androidx.annotation.StringRes val labelRes: Int, val icon: ImageVector) {
    Workout(R.string.destination_workout, Icons.Filled.FitnessCenter), Nutrition(R.string.destination_nutrition, Icons.Filled.Restaurant),
    Analytics(R.string.destination_analytics, Icons.Filled.BarChart), Profile(R.string.destination_profile, Icons.Filled.AccountCircle),
}

@Composable
private fun RootBottomNavigation(selected: RootDestination, onSelected: (RootDestination) -> Unit, modifier: Modifier = Modifier) {
    BottomNavigation(
        modifier = modifier.navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 6.dp)
            .height(62.dp)
            .shadow(10.dp, RoundedCornerShape(31.dp), clip = false)
            .clip(RoundedCornerShape(31.dp))
            .background(
                // Use the same translucent system material as the "Resume"
                // accessory so the root navigation stays visually consistent.
                if (androidx.compose.material.MaterialTheme.colors.isLight) Color.White.copy(alpha = 0.88f)
                else Color.Black.copy(alpha = 0.68f),
            )
            .border(
                1.dp,
                if (androidx.compose.material.MaterialTheme.colors.isLight) Color.White.copy(alpha = 0.64f)
                else Color.White.copy(alpha = 0.24f),
                RoundedCornerShape(31.dp),
            ),
        backgroundColor = Color.Transparent,
        elevation = 0.dp,
    ) {
        RootDestination.values().forEach { destination ->
            val label = androidx.compose.ui.res.stringResource(destination.labelRes)
            BottomNavigationItem(
                selected = selected == destination, onClick = { onSelected(destination) },
                icon = {
                    Box(
                        Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(24.dp))
                            .background(if (selected == destination) WildforceThemeTokens.textPrimary.copy(alpha = 0.10f) else Color.Transparent)
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(destination.icon, contentDescription = label, modifier = Modifier.size(28.dp), tint = if (androidx.compose.material.MaterialTheme.colors.isLight) Color(0xFF252525) else Color.White)
                    }
                },
                label = null,
                alwaysShowLabel = false,
                selectedContentColor = WildforceThemeTokens.textPrimary,
                unselectedContentColor = WildforceThemeTokens.textPrimary,
            )
        }
    }
}

@Composable
fun WildforceRoot(onThemeChanged: () -> Unit = {}) {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("wildforce_profile", 0) }
    val workoutPlanStore = remember(preferences) { WorkoutPlanStateStore(preferences) }
    val healthPermissions = remember {
        setOf(
            HealthPermission.getReadPermission(HeightRecord::class),
            HealthPermission.getReadPermission(WeightRecord::class),
            HealthPermission.getReadPermission(androidx.health.connect.client.records.ActiveCaloriesBurnedRecord::class),
            HealthPermission.getReadPermission(androidx.health.connect.client.records.TotalCaloriesBurnedRecord::class),
            HealthPermission.getWritePermission(WeightRecord::class),
            HealthPermission.getWritePermission(androidx.health.connect.client.records.ExerciseSessionRecord::class),
            HealthPermission.getWritePermission(androidx.health.connect.client.records.NutritionRecord::class),
        )
    }
    val coroutineScope = rememberCoroutineScope()
    var pendingHealthResult by remember { mutableStateOf<((Boolean, Int?, Double?) -> Unit)?>(null) }
    val healthPermissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract(),
    ) { granted ->
        if (granted.containsAll(healthPermissions)) {
            coroutineScope.launch {
                val client = HealthConnectClient.getOrCreate(context)
                val imported = runCatching {
                    val timeRange = TimeRangeFilter.before(Instant.now())
                    val height = client.readRecords(ReadRecordsRequest(HeightRecord::class, timeRangeFilter = timeRange, ascendingOrder = false, pageSize = 1))
                        .records.firstOrNull()?.height?.inMeters?.times(100)?.toInt()
                    val weight = client.readRecords(ReadRecordsRequest(WeightRecord::class, timeRangeFilter = timeRange, ascendingOrder = false, pageSize = 1))
                        .records.firstOrNull()?.weight?.inKilograms
                    height to weight
                }
                imported.onSuccess { (height, weight) ->
                    pendingHealthResult?.invoke(true, height, weight)
                    pendingHealthResult = null
                }.onFailure {
                    pendingHealthResult?.invoke(false, null, null)
                    pendingHealthResult = null
                }
            }
        } else {
            pendingHealthResult?.invoke(false, null, null)
            pendingHealthResult = null
        }
    }
    val requestHealthConnect: (((Boolean, Int?, Double?) -> Unit) -> Unit) = { result ->
        if (HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE) {
            pendingHealthResult = result
            healthPermissionLauncher.launch(healthPermissions)
        } else result(false, null, null)
    }
    var profile by remember {
        mutableStateOf(
            preferences.getString("name", null)?.let { name ->
                OnboardingProfile(
                    name = name,
                    goal = FitnessGoal.fromStoredValue(preferences.getString("goal", "").orEmpty()),
                    lifestyle = LifestyleLevel.fromStoredValue(preferences.getString("lifestyle", "").orEmpty()),
                    workoutDays = WorkoutWeekday.fromStoredValues(preferences.getStringSet("workout_days", null)),
                    preferredWorkoutDurationMinutes = preferences.getInt("workout_duration", 50).coerceIn(15, 180),
                    trainingLevel = TrainingLevel.fromStoredValue(preferences.getString("training_level", "").orEmpty()),
                    trainingSplitPreference = TrainingSplitPreference.fromStoredValue(preferences.getString("training_split", "").orEmpty()),
                    bodyCompositionPhase = preferences.getString("body_phase", null)?.let(BodyCompositionPhase::fromStoredValue),
                    customWorkoutFocuses = preferences.getStringSet("custom_focuses", emptySet()).orEmpty().mapNotNull { encoded ->
                        val parts = encoded.split(":", limit = 2)
                        val day = WorkoutWeekday.entries.firstOrNull { it.storedValue == parts.firstOrNull() }
                        val focus = WorkoutFocus.entries.firstOrNull { it.storedValue == parts.getOrNull(1) }
                        if (day != null && focus != null && focus in WorkoutFocus.customSelectionCases) day to focus else null
                    }.toMap(),
                    gymType = GymType.fromStoredValue(preferences.getString("gym_type", "").orEmpty()),
                    availableEquipment = Equipment.fromStoredValues(preferences.getStringSet("equipment", null))
                        .ifEmpty { GymType.SmallGym.defaultEquipment },
                    movementRestrictions = MovementRestriction.fromStoredValues(preferences.getStringSet("restrictions", null)),
                    isHealthConnectEnabled = preferences.getBoolean("health_connect", false),
                    birthMonth = preferences.getInt("birth_month", 1).coerceIn(1, 12),
                    birthYear = preferences.getInt("birth_year", 1995).coerceIn(1920, java.time.Year.now().value),
                    gender = Gender.fromStoredValue(preferences.getString("gender", "").orEmpty()),
                    metricSystem = MetricSystem.fromStoredValue(preferences.getString("metric_system", "").orEmpty()),
                    heightCm = preferences.getInt("height_cm", 175).coerceIn(120, 230),
                    weightKg = java.lang.Double.longBitsToDouble(preferences.getLong("weight_kg", java.lang.Double.doubleToRawLongBits(70.0)))
                        .takeIf { it.isFinite() && it in 35.0..250.0 } ?: 70.0,
                    skipsWarmups = preferences.getBoolean("skips_warmups", false),
                    skipsCooldowns = preferences.getBoolean("skips_cooldowns", false),
                    skipsRestPeriods = preferences.getBoolean("skips_rest_periods", false),
                    workoutPlannerNotes = preferences.getString("planner_notes", "").orEmpty(),
                    trainingLocations = decodeTrainingLocations(preferences.getStringSet("training_locations", emptySet()).orEmpty()),
                    appLanguage = preferences.getString("app_language", "Español") ?: "Español",
                )
            },
        )
    }

    var generatedWorkoutState by remember { mutableStateOf<WorkoutHubState?>(null) }
    var initialPlanPreview by remember { mutableStateOf<Pair<String, WorkoutHubState>?>(null) }
    var initialPlanProfile by remember { mutableStateOf<OnboardingProfile?>(null) }
    var pendingGeneratedPlan by remember { mutableStateOf<Pair<String, WorkoutHubState>?>(null) }
    var isGenerating by remember { mutableStateOf(false) }
    var generationError by remember { mutableStateOf<String?>(null) }

    fun persistOnboardingProfile(value: OnboardingProfile) {
        AppLocale.apply(value.appLanguage)
        preferences.edit()
            .putInt("profile_schema_version", PROFILE_SCHEMA_VERSION)
            .putString("name", value.name)
            .putString("goal", value.goal.storedValue)
            .putString("lifestyle", value.lifestyle.storedValue)
            .putStringSet("workout_days", value.workoutDays.mapTo(mutableSetOf()) { it.storedValue })
            .putInt("workout_duration", value.preferredWorkoutDurationMinutes)
            .putString("training_level", value.trainingLevel.storedValue)
            .putString("training_split", value.trainingSplitPreference.storedValue)
            .putString("body_phase", value.bodyCompositionPhase?.storedValue)
            .putStringSet("custom_focuses", value.customWorkoutFocuses.mapTo(mutableSetOf()) { (day, focus) -> "${day.storedValue}:${focus.storedValue}" })
            .putString("gym_type", value.gymType.storedValue)
            .putStringSet("equipment", value.availableEquipment.mapTo(mutableSetOf()) { it.storedValue })
            .putStringSet("restrictions", value.movementRestrictions.mapTo(mutableSetOf()) { it.storedValue })
            .putBoolean("health_connect", value.isHealthConnectEnabled)
            .putInt("birth_month", value.birthMonth)
            .putInt("birth_year", value.birthYear)
            .putString("gender", value.gender.storedValue)
            .putString("metric_system", value.metricSystem.storedValue)
            .putInt("height_cm", value.heightCm)
            .putLong("weight_kg", java.lang.Double.doubleToRawLongBits(value.weightKg))
            .putBoolean("skips_warmups", value.skipsWarmups)
            .putBoolean("skips_cooldowns", value.skipsCooldowns)
            .putBoolean("skips_rest_periods", value.skipsRestPeriods)
            .putString("planner_notes", value.workoutPlannerNotes)
            .putStringSet("training_locations", encodeTrainingLocations(value.trainingLocations))
            .putString("app_language", value.appLanguage)
            .apply()
    }

    val currentProfile = profile
    if (currentProfile == null) {
        OnboardingScreen(
            onRequestHealthConnect = requestHealthConnect,
            isGenerating = isGenerating,
            generationError = generationError,
            generatedPlanPreview = initialPlanPreview?.second?.let { state ->
                OnboardingPlanPreview(
                    name = state.planName,
                    phase = state.phase,
                    workouts = state.workouts.take(6).map { "${it.scheduledDay}: ${it.title} · ${it.exercises.size} ejercicios" },
                )
            },
            onStartTraining = {
                val generated = initialPlanPreview
                val generatedProfile = initialPlanProfile
                if (generated != null && generatedProfile != null) {
                    persistOnboardingProfile(generatedProfile)
                    workoutPlanStore.saveCurrentPlan(generated.first)
                    workoutPlanStore.saveProfileSignature(generatedProfile.workoutPlanProfileSignature())
                    WorkoutQuickAccessWidget.refresh(context)
                    generatedWorkoutState = generated.second
                    profile = generatedProfile
                    initialPlanPreview = null
                    initialPlanProfile = null
                }
            },
            onLanguageSelected = { language ->
                context.getSharedPreferences("wildforce_profile_details", 0).edit().putString("language", language).apply()
                AppLocale.apply(language)
            },
        ) { completedProfile, useAi ->
            generationError = null
            if (useAi) {
                isGenerating = true
                coroutineScope.launch {
                    runCatching { WorkoutPlanGenerator.generate(completedProfile, context) }
                        .onSuccess { (json, state) ->
                            initialPlanPreview = json to state
                            initialPlanProfile = completedProfile
                        }
                        .onFailure { generationError = it.message?.takeIf(String::isNotBlank) ?: "No se pudo generar el plan" }
                    isGenerating = false
                }
            } else {
                workoutPlanStore.clearCurrentPlan()
                generatedWorkoutState = PreviewWorkoutRepository.load(completedProfile.name, completedProfile.goal.title)
                profile = completedProfile
            }
        }
    } else {
        val baseState = generatedWorkoutState ?: workoutPlanStore.currentPlanJson()?.let { json ->
            runCatching { WorkoutPlanGenerator.parse(json, currentProfile.name, currentProfile.goal.title) }.getOrNull()
        } ?: PreviewWorkoutRepository.load(currentProfile.name, currentProfile.goal.title)
        val completedIds = preferences.getStringSet("completed_workouts", emptySet()).orEmpty()
        val skippedIds = preferences.getStringSet("skipped_workouts", emptySet()).orEmpty()
        val restoredState = baseState.copy(
            completedDays = baseState.workouts.filter { it.id in completedIds }.mapTo(mutableSetOf()) { it.scheduledDay },
            workouts = baseState.workouts.map {
                when {
                    it.id in completedIds -> it.copy(status = WorkoutStatus.Completed)
                    it.id in skippedIds -> it.copy(status = WorkoutStatus.Skipped)
                    else -> it
                }
            },
        )
        LaunchedEffect(currentProfile.name, workoutPlanStore.currentPlanJson()) {
            val hasRemoteSession = context.getSharedPreferences("wildforce_account", 0).getString("token", null) != null
            if (preferences.getBoolean("remote_sync_pending", false) || hasRemoteSession) {
                if (WorkoutRemoteSync.synchronize(context, currentProfile, preferences)) {
                    preferences.edit().putBoolean("remote_sync_pending", false).apply()
                    if (preferences.getBoolean("remote_plan_imported", false)) {
                        preferences.edit().remove("remote_plan_imported").apply()
                        workoutPlanStore.currentPlanJson()?.let { remotePlan ->
                            runCatching { WorkoutPlanGenerator.parse(remotePlan, currentProfile.name, currentProfile.goal.title) }
                                .onSuccess { generatedWorkoutState = it }
                        }
                    }
                }
            }
        }
        WildforceApp(
            profile = currentProfile,
            workoutState = restoredState,
            onWorkoutCompleted = { id, duration, sets, volume ->
                val previouslyCompleted = preferences.getStringSet("completed_workouts", emptySet()).orEmpty()
                val completed = previouslyCompleted + id
                val totalCompleted = preferences.getInt("total_completed_workouts", 0) + if (id in previouslyCompleted) 0 else 1
                preferences.edit().putStringSet("completed_workouts", completed)
                    .putInt("total_completed_workouts", totalCompleted)
                    .putInt("last_workout_duration", duration).putInt("last_workout_sets", sets)
                    .putLong("last_workout_volume", java.lang.Double.doubleToRawLongBits(volume)).apply()
                WildforceNotificationScheduler.recordWorkoutCompleted(context)
                // Finishing a workout must not crash if an optional side
                // effect (history migration or widget provider) fails on any
                // Android version, manufacturer or launcher.
                val completedWorkout = restoredState.workouts.firstOrNull { it.id == id }
                runCatching { TrainingSessionHistoryStore.record(preferences, duration, sets, volume, completedWorkout?.focus) }
                runCatching { WorkoutQuickAccessWidget.refresh(context) }
                preferences.edit().putBoolean("remote_sync_pending", true).apply()
                coroutineScope.launch {
                    if (WorkoutRemoteSync.synchronize(context, currentProfile, preferences)) {
                        preferences.edit().putBoolean("remote_sync_pending", false).apply()
                    }
                }
                if (currentProfile.isHealthConnectEnabled) {
                    val workoutTitle = completedWorkout?.title ?: "Entrenamiento"
                    coroutineScope.launch {
                        runCatching { HealthConnectWeightSync.publishCompletedWorkout(context, workoutTitle, duration) }
                    }
                }
            },
            onResetOnboarding = {
                preferences.edit().clear().apply()
                profile = null
            },
            onProfileUpdated = { updated ->
                AppLocale.apply(updated.appLanguage)
                preferences.edit()
                    .putInt("profile_schema_version", PROFILE_SCHEMA_VERSION)
                    .putString("name", updated.name)
                    .putString("goal", updated.goal.storedValue)
                    .putString("lifestyle", updated.lifestyle.storedValue)
                    .putStringSet("workout_days", updated.workoutDays.mapTo(mutableSetOf()) { it.storedValue })
                    .putInt("workout_duration", updated.preferredWorkoutDurationMinutes)
                    .putString("training_level", updated.trainingLevel.storedValue)
                    .putString("training_split", updated.trainingSplitPreference.storedValue)
                    .putString("body_phase", updated.bodyCompositionPhase?.storedValue)
                    .putStringSet("custom_focuses", updated.customWorkoutFocuses.mapTo(mutableSetOf()) { (day, focus) -> "${day.storedValue}:${focus.storedValue}" })
                    .putString("gym_type", updated.gymType.storedValue)
                    .putStringSet("equipment", updated.availableEquipment.mapTo(mutableSetOf()) { it.storedValue })
                    .putStringSet("restrictions", updated.movementRestrictions.mapTo(mutableSetOf()) { it.storedValue })
                    .putBoolean("health_connect", updated.isHealthConnectEnabled)
                    .putInt("birth_month", updated.birthMonth).putInt("birth_year", updated.birthYear)
                    .putString("gender", updated.gender.storedValue).putString("metric_system", updated.metricSystem.storedValue)
                    .putInt("height_cm", updated.heightCm).putLong("weight_kg", java.lang.Double.doubleToRawLongBits(updated.weightKg))
                    .putBoolean("skips_warmups", updated.skipsWarmups)
                    .putBoolean("skips_cooldowns", updated.skipsCooldowns)
                    .putBoolean("skips_rest_periods", updated.skipsRestPeriods)
                    .putString("planner_notes", updated.workoutPlannerNotes)
                    .putStringSet("training_locations", encodeTrainingLocations(updated.trainingLocations))
                    .putString("app_language", updated.appLanguage)
                    .putBoolean("remote_sync_pending", true)
                .apply()
                profile = updated
                if (updated.isHealthConnectEnabled) {
                    coroutineScope.launch {
                        runCatching { HealthConnectWeightSync.publishManualWeight(context, updated.weightKg) }
                    }
                }
            },
            onRegenerateProfile = { updated ->
                generationError = null
                pendingGeneratedPlan = null
                val nextPlanRequested = preferences.getBoolean("generate_next_plan", false)
                preferences.edit().remove("generate_next_plan").apply()
                isGenerating = true
                coroutineScope.launch {
                    runCatching {
                        if (nextPlanRequested) {
                            WorkoutPlanGenerator.generateNext(updated, restoredState, context)
                        } else WorkoutPlanGenerator.generate(updated, context)
                    }
                        .onSuccess { (json, state) ->
                            pendingGeneratedPlan = json to state
                        }
                        .onFailure { generationError = it.message?.takeIf(String::isNotBlank) ?: "No se pudo regenerar el plan" }
                    isGenerating = false
                }
            },
            onPlanWorkoutUpdated = { updatedState ->
                generatedWorkoutState = updatedState
                workoutPlanStore.saveCurrentPlan(WorkoutPlanGenerator.serialize(updatedState))
                preferences.edit().putBoolean("remote_sync_pending", true).apply()
                WorkoutQuickAccessWidget.refresh(context)
            },
            isGeneratingProfilePlan = isGenerating,
            profileGenerationError = generationError,
            onRequestHealthConnect = requestHealthConnect,
            onThemeChanged = onThemeChanged,
        )
        pendingGeneratedPlan?.let { (json, state) ->
            WorkoutPlanPreviewSheet(
                state = state,
                gender = currentProfile.gender.storedValue,
                onDismiss = { pendingGeneratedPlan = null },
                onCommit = {
                    // New plans have a distinct workout identity. Discard any
                    // paused state from the plan being replaced before its IDs
                    // can be mistaken for the new week's sessions.
                    WorkoutSessionStore.discardPlanSessions(context, baseState.workouts.map(WorkoutDaySummary::id))
                    workoutPlanStore.replaceCurrentPlan(json)
                    workoutPlanStore.saveProfileSignature(currentProfile.workoutPlanProfileSignature())
                    WorkoutQuickAccessWidget.refresh(context)
                    preferences.edit().remove("completed_workouts").remove("skipped_workouts").putBoolean("remote_sync_pending", true).apply()
                    generatedWorkoutState = state
                    pendingGeneratedPlan = null
                },
            )
        }
    }
}

/** The generated-plan confirmation mirrors iOS's expandable preview sheet. */
@Composable
private fun WorkoutPlanPreviewSheet(
    state: WorkoutHubState,
    gender: String,
    onDismiss: () -> Unit,
    onCommit: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        androidx.compose.material.Surface(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(.92f),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = WildforceThemeTokens.background,
        ) {
            Column {
                Row(
                    Modifier.fillMaxWidth().padding(start = 24.dp, top = 18.dp, end = 12.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("TU NUEVO PLAN", Modifier.weight(1f), style = androidx.compose.material.MaterialTheme.typography.h6, color = WildforceThemeTokens.textPrimary)
                    TextButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = androidx.compose.ui.res.stringResource(R.string.cancel), tint = WildforceThemeTokens.textSecondary)
                    }
                }
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    PlanPreviewSummary(state)
                    state.workouts.sortedBy(WorkoutDaySummary::order).forEach { workout ->
                        PlanPreviewWorkoutCard(workout, gender)
                    }
                    Spacer(Modifier.height(4.dp))
                }
                Button(
                    onClick = onCommit,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = WildforceThemeTokens.textPrimary,
                        contentColor = WildforceThemeTokens.background,
                    ),
                ) {
                    Text("¡ME COMPROMETO!", modifier = Modifier.padding(vertical = 4.dp), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun PlanPreviewSummary(state: WorkoutHubState) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(WildforceThemeTokens.backgroundSecondary).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("ESTA SEMANA", style = androidx.compose.material.MaterialTheme.typography.caption, fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textSecondary)
        Text(state.planName, style = androidx.compose.material.MaterialTheme.typography.h4, color = WildforceThemeTokens.textPrimary, maxLines = 2)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PlanPreviewPill(state.phase)
            PlanPreviewPill("${state.workouts.size} sesiones")
        }
        Text(
            androidx.compose.ui.res.stringResource(R.string.plan_progress, state.mesocycleNumber, state.mesocycleIndex, state.weekIndex, state.cycleLength),
            style = androidx.compose.material.MaterialTheme.typography.caption,
            color = WildforceThemeTokens.textSecondary,
        )
    }
}

@Composable
private fun PlanPreviewPill(text: String) {
    Text(
        text,
        Modifier.background(WildforceThemeTokens.surfaceSubtle, RoundedCornerShape(100.dp)).padding(horizontal = 10.dp, vertical = 6.dp),
        style = androidx.compose.material.MaterialTheme.typography.caption,
        color = WildforceThemeTokens.textSecondary,
        maxLines = 1,
    )
}

@Composable
private fun PlanPreviewWorkoutCard(workout: WorkoutDaySummary, gender: String) {
    Box(Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(10.dp))) {
        RemoteTrainingImage(
            url = exerciseImageUrl(workout.exercises.firstOrNull()?.imageKey, gender),
            contentDescription = workout.title,
            modifier = Modifier.fillMaxSize(),
        )
        Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .84f)))))
        Column(
            Modifier.align(Alignment.BottomStart).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(workout.scheduledDay.name.lowercase().replaceFirstChar(Char::titlecase), style = androidx.compose.material.MaterialTheme.typography.caption, color = Color.White.copy(alpha = .78f))
            Text(workout.title, style = androidx.compose.material.MaterialTheme.typography.h6, color = Color.White, maxLines = 1)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Timer, null, modifier = Modifier.size(14.dp), tint = Color.White.copy(alpha = .84f))
                Text("${workout.estimatedMinutes} min", style = androidx.compose.material.MaterialTheme.typography.caption, color = Color.White.copy(alpha = .84f))
                Icon(Icons.Filled.FitnessCenter, null, modifier = Modifier.size(14.dp), tint = Color.White.copy(alpha = .84f))
                Text(workout.focus, style = androidx.compose.material.MaterialTheme.typography.caption, color = Color.White.copy(alpha = .84f), maxLines = 1)
            }
        }
    }
}

@Composable
private fun WildforceApp(
    profile: OnboardingProfile,
    workoutState: WorkoutHubState,
    onWorkoutCompleted: (String, Int, Int, Double) -> Unit,
    onResetOnboarding: () -> Unit = {},
    onProfileUpdated: (OnboardingProfile) -> Unit = {},
    onRegenerateProfile: (OnboardingProfile) -> Unit = {},
    onPlanWorkoutUpdated: (WorkoutHubState) -> Unit = {},
    isGeneratingProfilePlan: Boolean = false,
    profileGenerationError: String? = null,
    onRequestHealthConnect: (((Boolean, Int?, Double?) -> Unit) -> Unit) = { _ -> },
    onThemeChanged: () -> Unit = {},
) {
    // This must follow the applied app theme, not just the device setting:
    // users can explicitly choose Claro or Oscuro in their profile.
    val darkTheme = !androidx.compose.material.MaterialTheme.colors.isLight
    val activity = LocalActivity.current
    val view = LocalView.current
    SideEffect {
        activity?.let { host ->
            host.window.statusBarColor = AndroidColor.TRANSPARENT
            host.window.navigationBarColor = AndroidColor.TRANSPARENT
            val controller = WindowCompat.getInsetsController(host.window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }
    val appContext = LocalContext.current.applicationContext
    val appPreferences = remember { appContext.getSharedPreferences("wildforce_profile", 0) }
    val profileDetailPreferences = remember { appContext.getSharedPreferences("wildforce_profile_details", 0) }
    var keepScreenOnDuringWorkout by remember { mutableStateOf(profileDetailPreferences.getBoolean("keep_screen_on", true)) }
    val workoutPlanStore = remember(appPreferences) { WorkoutPlanStateStore(appPreferences) }
    var avatarPath by remember { mutableStateOf(appPreferences.getString("avatar_path", null)) }
    var avatarRevision by remember { mutableStateOf(0) }
    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching {
            val destination = File(appContext.filesDir, "profile-avatar.jpg")
            appContext.contentResolver.openInputStream(uri)?.use { input -> destination.writeBytes(input.readBytes()) }
            avatarPath = destination.absolutePath
            avatarRevision++
            appPreferences.edit().putString("avatar_path", avatarPath).apply()
        }
    }
    LaunchedEffect(workoutState.planName, workoutState.workouts) {
        WildforceNotificationScheduler.scheduleNextWorkout(appContext, workoutState)
        WildforceNotificationScheduler.scheduleNutritionReminder(appContext)
    }
    val trainingProgress = CompletionProgressStore.progress(appContext)
    var selected by remember { mutableStateOf(RootDestination.Workout) }
    var displayedWorkoutState by remember(workoutState) { mutableStateOf(workoutState) }
    var workoutDetail by remember { mutableStateOf<WorkoutDaySummary?>(null) }
    var activeWorkout by remember { mutableStateOf<WorkoutDaySummary?>(null) }
    var showExitAppDialog by remember { mutableStateOf(false) }
    SideEffect {
        activity?.window?.let { window ->
            if (keepScreenOnDuringWorkout && activeWorkout != null) window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            else window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
    BackHandler(enabled = activeWorkout == null) {
        when {
            workoutDetail != null -> workoutDetail = null
            selected != RootDestination.Workout -> selected = RootDestination.Workout
            else -> showExitAppDialog = true
        }
    }
    activeWorkout?.let { workout ->
        ActiveWorkoutScreen(
            initialWorkout = workout,
            gender = profile.gender.storedValue,
            userBmr = workoutBmr(profile),
            useImperial = profile.metricSystem == MetricSystem.Imperial,
            skipRestPeriods = profile.skipsRestPeriods,
            currentStreak = displayedWorkoutState.user.currentStreak,
            isPlanCompletedAfterWorkout = displayedWorkoutState.workouts.none { candidate ->
                candidate.id != workout.id && !candidate.status.isPlanFinalized
            },
            isMesocycleCompletedAfterWorkout = displayedWorkoutState.cycleLength > 1 &&
                displayedWorkoutState.positionInCycle >= displayedWorkoutState.cycleLength &&
                displayedWorkoutState.workouts.none { candidate ->
                    candidate.id != workout.id && !candidate.status.isPlanFinalized
                },
            planName = displayedWorkoutState.planName,
            completedPlanWorkouts = displayedWorkoutState.workouts.count { it.status == WorkoutStatus.Completed } + 1,
            totalPlanWorkouts = displayedWorkoutState.workouts.size,
            totalPlanExercises = displayedWorkoutState.workouts.sumOf { it.exercises.size },
            onGenerateNextPlan = {
                appPreferences.edit().putBoolean("generate_next_plan", true).apply()
                onRegenerateProfile(profile)
            },
            onWorkoutChanged = { updated ->
                if (updated.id.startsWith("custom-")) CustomWorkoutStore.save(appContext, updated)
                val updatedState = displayedWorkoutState.copy(
                    workouts = displayedWorkoutState.workouts.map { candidate -> if (candidate.id == updated.id) updated else candidate },
                )
                displayedWorkoutState = updatedState
                if (!updated.id.startsWith("custom-")) workoutPlanStore.saveCurrentPlan(WorkoutPlanGenerator.serialize(updatedState))
                activeWorkout = updated
            },
            onExit = { activeWorkout = null },
            onFinish = { duration, sets, volume, streak ->
                val completedState = displayedWorkoutState.copy(
                    user = displayedWorkoutState.user.copy(currentStreak = streak),
                    completedDays = if (displayedWorkoutState.workouts.any { it.id == workout.id }) displayedWorkoutState.completedDays + workout.scheduledDay else displayedWorkoutState.completedDays,
                    workouts = displayedWorkoutState.workouts.map { if (it.id == workout.id) it.copy(status = WorkoutStatus.Completed) else it },
                )
                displayedWorkoutState = completedState
                if (!workout.id.startsWith("custom-")) workoutPlanStore.saveCurrentPlan(WorkoutPlanGenerator.serialize(completedState))
                onWorkoutCompleted(workout.id, duration, sets, volume)
                WildforceNotificationScheduler.scheduleNextWorkout(appContext, completedState)
                activeWorkout = null
                workoutDetail = null
            },
        )
        return
    }
    workoutDetail?.let { workout ->
        Scaffold(
            modifier = Modifier.liquidGlassBackground(),
            backgroundColor = androidx.compose.ui.graphics.Color.Transparent,
            bottomBar = {
                RootBottomNavigation(
                    selected = selected,
                    onSelected = {
                        workoutDetail = null
                        selected = it
                    },
                )
            },
        ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
        WorkoutDetailScreen(workout, profile.gender.storedValue, useImperial = profile.metricSystem == MetricSystem.Imperial, onBack = { workoutDetail = null }, onStart = {
            WildforceNotificationScheduler.recordWorkoutStarted(appContext)
            activeWorkout = workout
        }, onSkip = {
            val skipped = appPreferences.getStringSet("skipped_workouts", emptySet()).orEmpty() + workout.id
            appPreferences.edit().putStringSet("skipped_workouts", skipped).apply()
            val skippedState = displayedWorkoutState.copy(
                workouts = displayedWorkoutState.workouts.map { if (it.id == workout.id) it.copy(status = WorkoutStatus.Skipped) else it },
            )
            displayedWorkoutState = skippedState
            if (!workout.id.startsWith("custom-")) workoutPlanStore.saveCurrentPlan(WorkoutPlanGenerator.serialize(skippedState))
            WildforceNotificationScheduler.scheduleNextWorkout(appContext, skippedState)
            workoutDetail = null
            // iOS treats skipped days as finished plan slots.  If this was the
            // final pending session, continue straight to the next-plan flow.
            if (skippedState.workouts.isNotEmpty() && skippedState.workouts.all { it.status.isPlanFinalized }) {
                appPreferences.edit().putBoolean("generate_next_plan", true).apply()
                onRegenerateProfile(profile)
            }
        }, onUnskip = {
            val skipped = appPreferences.getStringSet("skipped_workouts", emptySet()).orEmpty() - workout.id
            appPreferences.edit().putStringSet("skipped_workouts", skipped).apply()
            val unskippedState = displayedWorkoutState.copy(
                workouts = displayedWorkoutState.workouts.map { if (it.id == workout.id) it.copy(status = WorkoutStatus.Planned) else it },
            )
            displayedWorkoutState = unskippedState
            if (!workout.id.startsWith("custom-")) workoutPlanStore.saveCurrentPlan(WorkoutPlanGenerator.serialize(unskippedState))
            WildforceNotificationScheduler.scheduleNextWorkout(appContext, unskippedState)
        }, onWorkoutUpdated = { updated ->
            if (updated.id.startsWith("custom-")) {
                CustomWorkoutStore.save(appContext, updated)
            }
            val updatedState = displayedWorkoutState.copy(
                workouts = displayedWorkoutState.workouts.map { existing -> if (existing.id == updated.id) updated else existing },
            )
            displayedWorkoutState = updatedState
            if (!updated.id.startsWith("custom-")) workoutPlanStore.saveCurrentPlan(WorkoutPlanGenerator.serialize(updatedState))
            workoutDetail = updated
            onPlanWorkoutUpdated(updatedState)
        }, defaultAdaptEquipment = profile.effectiveTrainingLocations().firstOrNull { it.isDefault }?.equipment?.joinToString(", ") { it.title }.orEmpty().ifBlank { profile.availableEquipment.joinToString(", ") { it.title } }, adaptEquipmentPresets = profile.effectiveTrainingLocations().map { location -> location.name to location.equipment.joinToString(", ") { it.title } }, adaptAiGenerator = { request ->
            WorkoutPlanGenerator.adaptWorkout(profile, workout, request, appContext)
        })
        }
        }
        return
    }
    val archivedWorkoutStates = remember(displayedWorkoutState.mesocycleNumber, displayedWorkoutState.planName) {
        workoutPlanStore.archivedPlans().mapNotNull { archived ->
            runCatching { WorkoutPlanGenerator.parse(archived.rawJson, profile.name, profile.goal.title) }.getOrNull()
        }
    }
    // A saved session is Android's equivalent of iOS's `.inProgress` workout
    // state. It remains available after leaving the active-session screen.
    val resumableWorkout = displayedWorkoutState.workouts.firstOrNull { workout ->
        workout.status == WorkoutStatus.Planned &&
            WorkoutSessionStore.hasActiveSession(appContext, workout.id)
    }
    Scaffold(
        modifier = Modifier.liquidGlassBackground(),
        backgroundColor = androidx.compose.ui.graphics.Color.Transparent,
        bottomBar = {
            Column {
                resumableWorkout?.let { workout ->
                    ActiveWorkoutAccessory(
                        workout = workout,
                        exerciseIndex = WorkoutSessionStore.activeExerciseIndex(appContext, workout.id),
                        gender = profile.gender.storedValue,
                        onResume = { activeWorkout = workout },
                    )
                }
                RootBottomNavigation(selected = selected, onSelected = { selected = it })
            }
        },
    ) { padding ->
        // Reserve the complete bottom-bar footprint.  The resume accessory can be
        // taller than the navigation bar, so drawing content behind it would make
        // the final item of any scrollable screen unreachable.
        val overlayPadding = PaddingValues(
            start = padding.calculateStartPadding(LayoutDirection.Ltr),
            top = padding.calculateTopPadding(),
            end = padding.calculateEndPadding(LayoutDirection.Ltr),
            bottom = padding.calculateBottomPadding(),
        )
        if (selected == RootDestination.Workout) {
            WorkoutHubScreen(
                contentPadding = overlayPadding,
                state = displayedWorkoutState,
                planHistory = archivedWorkoutStates,
                onWorkoutSelected = { workoutDetail = it },
                gender = profile.gender.storedValue,
                defaultCustomEquipment = profile.availableEquipment.joinToString(", ") { it.title },
                customEquipmentPresets = profile.effectiveTrainingLocations().map { location -> location.name to location.equipment.joinToString(", ") { it.title } },
                generationError = profileGenerationError,
                onRetryGeneration = { onRegenerateProfile(profile) },
                requiresPlanRegeneration = workoutPlanStore.requiresRegeneration(profile.workoutPlanProfileSignature()),
                onRecreatePlan = { onRegenerateProfile(profile) },
                avatarPath = avatarPath,
                avatarRevision = avatarRevision,
                customAiGenerator = { request -> WorkoutPlanGenerator.generateCustom(profile, request, appContext) },
            )
        } else if (selected == RootDestination.Nutrition) {
            NutritionScreen(overlayPadding, profile)
        } else if (selected == RootDestination.Analytics) {
            val analyticsExercises = displayedWorkoutState.workouts.flatMap { it.exercises }
            AnalyticsScreen(
                context = appContext,
                state = displayedWorkoutState,
                preferences = appPreferences,
                exerciseSummaries = WorkoutAnalyticsStore.summaries(appContext, analyticsExercises),
                contentPadding = overlayPadding,
                gender = profile.gender.storedValue,
                useImperial = profile.metricSystem == MetricSystem.Imperial,
            )
        } else if (selected == RootDestination.Profile) {
            ProfileScreen(
                initial = profile,
                contentPadding = overlayPadding,
                isRegenerating = isGeneratingProfilePlan,
                generationError = profileGenerationError,
                currentStreak = trainingProgress.currentStreak.coerceAtLeast(displayedWorkoutState.user.currentStreak),
                completedWorkouts = appPreferences.getInt("total_completed_workouts", 0),
                longestStreak = trainingProgress.longestStreak,
                experienceXp = trainingProgress.xp,
                experienceLevel = trainingProgress.level,
                experienceProgress = trainingProgress.levelProgress,
                avatarPath = avatarPath,
                avatarRevision = avatarRevision,
                onRequestHealthConnect = onRequestHealthConnect,
                onSave = onProfileUpdated,
                onRegenerate = onRegenerateProfile,
                onChangeAvatar = { avatarPicker.launch("image/*") },
                onThemeChanged = onThemeChanged,
                onKeepScreenOnChanged = { keepScreenOnDuringWorkout = it },
                onGenerateFuturePreview = { image, bodyProfile -> FutureBodyPreviewGenerator.generate(image, bodyProfile) },
            )
        } else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(androidx.compose.ui.res.stringResource(R.string.coming_soon, androidx.compose.ui.res.stringResource(selected.labelRes)), color = WildforceThemeTokens.textSecondary)
        }
    }
    if (isGeneratingProfilePlan) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.58f)).clickable { }, contentAlignment = Alignment.Center) {
            Column(Modifier.liquidGlass(RoundedCornerShape(24.dp), emphasized = true).padding(26.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = WildforceThemeTokens.accentGold)
                Text(androidx.compose.ui.res.stringResource(R.string.creating_next_plan), Modifier.padding(top = 14.dp), color = WildforceThemeTokens.textPrimary, fontWeight = FontWeight.Bold)
            }
        }
    }
    if (showExitAppDialog) {
        val hostActivity = LocalActivity.current
        AlertDialog(
            onDismissRequest = { showExitAppDialog = false },
            title = { Text(androidx.compose.ui.res.stringResource(R.string.exit_app_title), fontWeight = FontWeight.Bold) },
            text = { Text(androidx.compose.ui.res.stringResource(R.string.exit_app_message)) },
            confirmButton = {
                androidx.compose.material.TextButton(onClick = {
                    showExitAppDialog = false
                    hostActivity?.finish()
                }) { Text(androidx.compose.ui.res.stringResource(R.string.exit), color = Color(0xFFC62828)) }
            },
            dismissButton = {
                androidx.compose.material.TextButton(onClick = { showExitAppDialog = false }) { Text(androidx.compose.ui.res.stringResource(R.string.cancel), color = WildforceThemeTokens.textSecondary) }
            },
        )
    }
}

/** Same Mifflin–St Jeor basal calculation used by the iOS completion fallback. */
private fun workoutBmr(profile: OnboardingProfile): Double {
    val now = java.time.LocalDate.now()
    val age = (now.year - profile.birthYear - if (now.monthValue < profile.birthMonth) 1 else 0).coerceAtLeast(0)
    val genderComponent = if (profile.gender == Gender.Male) 5.0 else -161.0
    return 10.0 * profile.weightKg + 6.25 * profile.heightCm - 5.0 * age + genderComponent
}

/** Matches iOS's tab-view bottom accessory for an interrupted workout. */
@Composable
private fun ActiveWorkoutAccessory(
    workout: WorkoutDaySummary,
    exerciseIndex: Int?,
    gender: String,
    onResume: () -> Unit,
) {
    val currentExercise = workout.exercises.getOrNull(exerciseIndex ?: 0)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .height(49.dp),
        shape = RoundedCornerShape(20.dp),
        // iOS renders the bottom accessory on its own system material. Keep
        // the Android counterpart readable over scrolling content as well.
        color = if (androidx.compose.material.MaterialTheme.colors.isLight) {
            Color.White.copy(alpha = 0.88f)
        } else {
            Color.Black.copy(alpha = 0.68f)
        },
        elevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onResume)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (currentExercise?.imageKey != null) {
                // The resume accessory identifies a specific exercise, so use its
                // own thumbnail instead of an abstract muscle-group symbol.
                RemoteTrainingImage(
                    url = exerciseImageUrl(currentExercise.imageKey, gender),
                    contentDescription = currentExercise.name,
                    modifier = Modifier.size(28.dp).clip(RoundedCornerShape(7.dp)),
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.FitnessCenter,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = WildforceThemeTokens.accentGold,
                )
            }
            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                Text(
                    text = workout.title,
                    style = androidx.compose.material.MaterialTheme.typography.subtitle2,
                    color = WildforceThemeTokens.textPrimary,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
                Text(
                    text = currentExercise?.name ?: workout.focus,
                    style = androidx.compose.material.MaterialTheme.typography.caption,
                    color = WildforceThemeTokens.textSecondary,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }
            Text(
                text = androidx.compose.ui.res.stringResource(R.string.ios_catalog_d640c7421da06661),
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(WildforceThemeTokens.accentGold.copy(alpha = 0.14f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                style = androidx.compose.material.MaterialTheme.typography.subtitle2.copy(fontSize = 15.sp),
                fontWeight = FontWeight.SemiBold,
                color = WildforceThemeTokens.textPrimary,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AppPreview() = WildforceTheme {
    WildforceApp(
        profile = OnboardingProfile("Jordi", FitnessGoal.BuildMuscle),
        workoutState = PreviewWorkoutRepository.load(),
        onWorkoutCompleted = { _, _, _, _ -> },
    )
}

private const val PROFILE_SCHEMA_VERSION = 2
private const val trainingLocationSeparator = "\u0001"

private fun encodeTrainingLocations(locations: List<TrainingLocationProfile>): MutableSet<String> = locations.mapTo(mutableSetOf()) { location ->
    listOf(
        location.name.replace(trainingLocationSeparator, " "),
        if (location.isDefault) "1" else "0",
        location.equipment.joinToString(",") { it.storedValue },
    ).joinToString(trainingLocationSeparator)
}

private fun decodeTrainingLocations(encoded: Set<String>): List<TrainingLocationProfile> = encoded.mapNotNull { value ->
    val fields = value.split(trainingLocationSeparator, limit = 3)
    val name = fields.getOrNull(0)?.trim().orEmpty()
    if (name.isBlank()) null else TrainingLocationProfile(
        name = name,
        isDefault = fields.getOrNull(1) == "1",
        equipment = Equipment.fromStoredValues(fields.getOrNull(2)?.split(',')?.filter(String::isNotBlank)?.toSet()).ifEmpty { setOf(Equipment.Bodyweight) },
    )
}.let { locations ->
    if (locations.isEmpty() || locations.any { it.isDefault }) locations else locations.mapIndexed { index, location -> location.copy(isDefault = index == 0) }
}
