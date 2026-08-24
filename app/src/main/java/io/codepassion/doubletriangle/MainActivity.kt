package io.codepassion.doubletriangle

import android.os.Bundle
import androidx.activity.ComponentActivity
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.compose.ui.platform.LocalContext
import io.codepassion.doubletriangle.core.model.PreviewWorkoutRepository
import io.codepassion.doubletriangle.core.model.WorkoutDaySummary
import io.codepassion.doubletriangle.core.model.WorkoutHubState
import io.codepassion.doubletriangle.core.model.WorkoutStatus
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
import io.codepassion.doubletriangle.feature.onboarding.OnboardingScreen
import io.codepassion.doubletriangle.feature.onboarding.ProfileScreen
import io.codepassion.doubletriangle.feature.workout.ActiveWorkoutScreen
import io.codepassion.doubletriangle.feature.workout.CompletionProgressStore
import io.codepassion.doubletriangle.feature.workout.WorkoutDetailScreen
import io.codepassion.doubletriangle.feature.workout.WorkoutHubScreen
import io.codepassion.doubletriangle.feature.workout.WorkoutAnalyticsStore
import io.codepassion.doubletriangle.nutrition.NutritionScreen
import java.time.Instant
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 7203)
        }
        setContent {
            WildforceTheme {
                Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                    WildforceRoot()
                }
            }
        }
    }
}

private enum class RootDestination(val label: String, val glyph: String) {
    Workout("Entrenamiento", "W"), Nutrition("Nutrición", "N"),
    Analytics("Analíticas", "A"), Profile("Perfil", "P"),
}

@Composable
fun WildforceRoot() {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("wildforce_profile", 0) }
    val healthPermissions = remember {
        setOf(
            HealthPermission.getReadPermission(HeightRecord::class),
            HealthPermission.getReadPermission(WeightRecord::class),
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
                        if (day != null && focus != null) day to focus else null
                    }.toMap(),
                    gymType = GymType.fromStoredValue(preferences.getString("gym_type", "").orEmpty()),
                    availableEquipment = Equipment.fromStoredValues(preferences.getStringSet("equipment", null))
                        .ifEmpty { GymType.SmallGym.defaultEquipment },
                    movementRestrictions = MovementRestriction.fromStoredValues(preferences.getStringSet("restrictions", null)),
                    isHealthConnectEnabled = preferences.getBoolean("health_connect", false),
                    birthMonth = preferences.getInt("birth_month", 1).coerceIn(1, 12),
                    birthYear = preferences.getInt("birth_year", 1995).coerceIn(1920, java.time.Year.now().value - 13),
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
                )
            },
        )
    }

    var generatedWorkoutState by remember { mutableStateOf<WorkoutHubState?>(null) }
    var pendingGeneratedPlan by remember { mutableStateOf<Pair<String, WorkoutHubState>?>(null) }
    var isGenerating by remember { mutableStateOf(false) }
    var generationError by remember { mutableStateOf<String?>(null) }

    val currentProfile = profile
    if (currentProfile == null) {
        OnboardingScreen(
            onRequestHealthConnect = requestHealthConnect,
            isGenerating = isGenerating,
            generationError = generationError,
        ) { completedProfile, useAi ->
            preferences.edit()
                .putInt("profile_schema_version", PROFILE_SCHEMA_VERSION)
                .putString("name", completedProfile.name)
                .putString("goal", completedProfile.goal.storedValue)
                .putString("lifestyle", completedProfile.lifestyle.storedValue)
                .putStringSet("workout_days", completedProfile.workoutDays.mapTo(mutableSetOf()) { it.storedValue })
                .putInt("workout_duration", completedProfile.preferredWorkoutDurationMinutes)
                .putString("training_level", completedProfile.trainingLevel.storedValue)
                .putString("training_split", completedProfile.trainingSplitPreference.storedValue)
                .putString("body_phase", completedProfile.bodyCompositionPhase?.storedValue)
                .putStringSet("custom_focuses", completedProfile.customWorkoutFocuses.mapTo(mutableSetOf()) { (day, focus) -> "${day.storedValue}:${focus.storedValue}" })
                .putString("gym_type", completedProfile.gymType.storedValue)
                .putStringSet("equipment", completedProfile.availableEquipment.mapTo(mutableSetOf()) { it.storedValue })
                .putStringSet("restrictions", completedProfile.movementRestrictions.mapTo(mutableSetOf()) { it.storedValue })
                .putBoolean("health_connect", completedProfile.isHealthConnectEnabled)
                .putInt("birth_month", completedProfile.birthMonth)
                .putInt("birth_year", completedProfile.birthYear)
                .putString("gender", completedProfile.gender.storedValue)
                .putString("metric_system", completedProfile.metricSystem.storedValue)
                .putInt("height_cm", completedProfile.heightCm)
                .putLong("weight_kg", java.lang.Double.doubleToRawLongBits(completedProfile.weightKg))
                .putBoolean("skips_warmups", completedProfile.skipsWarmups)
                .putBoolean("skips_cooldowns", completedProfile.skipsCooldowns)
                .putBoolean("skips_rest_periods", completedProfile.skipsRestPeriods)
                .putString("planner_notes", completedProfile.workoutPlannerNotes)
                .putStringSet("training_locations", encodeTrainingLocations(completedProfile.trainingLocations))
                .apply()
            generationError = null
            if (useAi) {
                isGenerating = true
                coroutineScope.launch {
                    runCatching { WorkoutPlanGenerator.generate(completedProfile, context) }
                        .onSuccess { (json, state) ->
                            preferences.edit().putString("workout_plan_json", json).apply()
                            generatedWorkoutState = state
                            profile = completedProfile
                        }
                        .onFailure { generationError = it.message?.takeIf(String::isNotBlank) ?: "No se pudo generar el plan" }
                    isGenerating = false
                }
            } else {
                preferences.edit().remove("workout_plan_json").apply()
                generatedWorkoutState = PreviewWorkoutRepository.load(completedProfile.name, completedProfile.goal.title)
                profile = completedProfile
            }
        }
    } else {
        val baseState = generatedWorkoutState ?: preferences.getString("workout_plan_json", null)?.let { json ->
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
                TrainingSessionHistoryStore.record(preferences, duration, sets, volume)
            },
            onResetOnboarding = {
                preferences.edit().clear().apply()
                profile = null
            },
            onProfileUpdated = { updated ->
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
                    .apply()
                profile = updated
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
                preferences.edit().putString("workout_plan_json", WorkoutPlanGenerator.serialize(updatedState)).apply()
            },
            isGeneratingProfilePlan = isGenerating,
            profileGenerationError = generationError,
            onRequestHealthConnect = requestHealthConnect,
        )
        pendingGeneratedPlan?.let { (json, state) ->
            AlertDialog(
                onDismissRequest = { pendingGeneratedPlan = null },
                title = { Text("PREVISUALIZAR NUEVO PLAN", fontWeight = FontWeight.Bold, maxLines = 2) },
                text = {
                    Column {
                        Text(state.planName, fontWeight = FontWeight.Bold, maxLines = 2)
                        Text(state.phase, color = WildforceThemeTokens.textSecondary, maxLines = 2)
                        Text("Plan ${state.mesocycleNumber} · Mesociclo ${state.mesocycleIndex} · Semana ${state.weekIndex}/${state.cycleLength}", color = WildforceThemeTokens.accentGold, maxLines = 1)
                        state.mesocyclePhase?.let { phase -> Text("${phase.label} · Semana de fase ${state.phaseWeek}", color = WildforceThemeTokens.textSecondary) }
                        state.workouts.take(6).forEach { workout ->
                            Text("• ${workout.scheduledDay}: ${workout.title} · ${workout.exercises.size} ejercicios · ${workout.estimatedMinutes} min", modifier = Modifier.padding(top = 6.dp), maxLines = 1)
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        preferences.getString("workout_plan_json", null)?.let { currentJson ->
                            WorkoutPlanArchiveStore.archive(preferences, currentJson)
                        }
                        preferences.edit().putString("workout_plan_json", json).apply()
                        preferences.edit().remove("completed_workouts").remove("skipped_workouts").apply()
                        generatedWorkoutState = state
                        pendingGeneratedPlan = null
                    }) { Text("USAR ESTE PLAN") }
                },
                dismissButton = { Button(onClick = { pendingGeneratedPlan = null }) { Text("CANCELAR") } },
            )
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
) {
    val appContext = LocalContext.current.applicationContext
    val appPreferences = remember { appContext.getSharedPreferences("wildforce_profile", 0) }
    LaunchedEffect(workoutState.planName, workoutState.workouts) {
        WildforceNotificationScheduler.scheduleNextWorkout(appContext, workoutState)
        WildforceNotificationScheduler.scheduleNutritionReminder(appContext)
    }
    val trainingProgress = CompletionProgressStore.progress(appContext)
    var selected by remember { mutableStateOf(RootDestination.Workout) }
    var displayedWorkoutState by remember(workoutState) { mutableStateOf(workoutState) }
    var workoutDetail by remember { mutableStateOf<WorkoutDaySummary?>(null) }
    var activeWorkout by remember { mutableStateOf<WorkoutDaySummary?>(null) }
    activeWorkout?.let { workout ->
        ActiveWorkoutScreen(
            workout = workout,
            gender = profile.gender.storedValue,
            useImperial = profile.metricSystem == MetricSystem.Imperial,
            skipRestPeriods = profile.skipsRestPeriods,
            currentStreak = displayedWorkoutState.user.currentStreak,
            isPlanCompletedAfterWorkout = displayedWorkoutState.workouts.none { candidate ->
                candidate.id != workout.id && candidate.status == WorkoutStatus.Planned
            },
            planName = displayedWorkoutState.planName,
            completedPlanWorkouts = displayedWorkoutState.workouts.count { it.status == WorkoutStatus.Completed } + 1,
            totalPlanWorkouts = displayedWorkoutState.workouts.size,
            totalPlanExercises = displayedWorkoutState.workouts.sumOf { it.exercises.size },
            onGenerateNextPlan = {
                appPreferences.edit().putBoolean("generate_next_plan", true).apply()
                onRegenerateProfile(profile)
            },
            onExit = { activeWorkout = null },
            onFinish = { duration, sets, volume, streak ->
                displayedWorkoutState = displayedWorkoutState.copy(
                    user = displayedWorkoutState.user.copy(currentStreak = streak),
                    completedDays = if (displayedWorkoutState.workouts.any { it.id == workout.id }) displayedWorkoutState.completedDays + workout.scheduledDay else displayedWorkoutState.completedDays,
                    workouts = displayedWorkoutState.workouts.map { if (it.id == workout.id) it.copy(status = WorkoutStatus.Completed) else it },
                )
                onWorkoutCompleted(workout.id, duration, sets, volume)
                activeWorkout = null
                workoutDetail = null
            },
        )
        return
    }
    workoutDetail?.let { workout ->
        WorkoutDetailScreen(workout, profile.gender.storedValue, useImperial = profile.metricSystem == MetricSystem.Imperial, onBack = { workoutDetail = null }, onStart = { activeWorkout = workout }, onSkip = {
            val skipped = appPreferences.getStringSet("skipped_workouts", emptySet()).orEmpty() + workout.id
            appPreferences.edit().putStringSet("skipped_workouts", skipped).apply()
            displayedWorkoutState = displayedWorkoutState.copy(
                workouts = displayedWorkoutState.workouts.map { if (it.id == workout.id) it.copy(status = WorkoutStatus.Skipped) else it },
            )
            workoutDetail = null
        }, onUnskip = {
            val skipped = appPreferences.getStringSet("skipped_workouts", emptySet()).orEmpty() - workout.id
            appPreferences.edit().putStringSet("skipped_workouts", skipped).apply()
            displayedWorkoutState = displayedWorkoutState.copy(
                workouts = displayedWorkoutState.workouts.map { if (it.id == workout.id) it.copy(status = WorkoutStatus.Planned) else it },
            )
        }, onWorkoutUpdated = { updated ->
            val updatedState = displayedWorkoutState.copy(
                workouts = displayedWorkoutState.workouts.map { existing -> if (existing.id == updated.id) updated else existing },
            )
            displayedWorkoutState = updatedState
            workoutDetail = updated
            onPlanWorkoutUpdated(updatedState)
        }, defaultAdaptEquipment = profile.effectiveTrainingLocations().firstOrNull { it.isDefault }?.equipment?.joinToString(", ") { it.title }.orEmpty().ifBlank { profile.availableEquipment.joinToString(", ") { it.title } }, adaptEquipmentPresets = profile.effectiveTrainingLocations().map { location -> location.name to location.equipment.joinToString(", ") { it.title } }, adaptAiGenerator = { request ->
            WorkoutPlanGenerator.adaptWorkout(profile, workout, request, appContext)
        })
        return
    }
    val archivedWorkoutStates = remember(displayedWorkoutState.mesocycleNumber, displayedWorkoutState.planName) {
        WorkoutPlanArchiveStore.load(appPreferences).mapNotNull { archived ->
            runCatching { WorkoutPlanGenerator.parse(archived.rawJson, profile.name, profile.goal.title) }.getOrNull()
        }
    }
    Scaffold(
        modifier = Modifier.liquidGlassBackground(),
        backgroundColor = androidx.compose.ui.graphics.Color.Transparent,
        bottomBar = {
            BottomNavigation(
                modifier = Modifier.navigationBarsPadding().padding(horizontal = 10.dp, vertical = 6.dp).liquidGlass(RoundedCornerShape(18.dp), emphasized = true),
                backgroundColor = androidx.compose.ui.graphics.Color.Transparent,
                elevation = 0.dp,
            ) {
                RootDestination.values().forEach { destination ->
                    BottomNavigationItem(
                        selected = selected == destination, onClick = { selected = destination },
                        icon = { Text(destination.glyph, fontWeight = FontWeight.Bold) }, label = { Text(destination.label) },
                        selectedContentColor = WildforceThemeTokens.accentGold, unselectedContentColor = WildforceThemeTokens.textSecondary,
                    )
                }
            }
        },
    ) { padding ->
        if (selected == RootDestination.Workout) {
            WorkoutHubScreen(
                contentPadding = padding,
                state = displayedWorkoutState,
                planHistory = archivedWorkoutStates,
                onWorkoutSelected = { workoutDetail = it },
                gender = profile.gender.storedValue,
                defaultCustomEquipment = profile.availableEquipment.joinToString(", ") { it.title },
                customEquipmentPresets = profile.effectiveTrainingLocations().map { location -> location.name to location.equipment.joinToString(", ") { it.title } },
                generationError = profileGenerationError,
                onRetryGeneration = { onRegenerateProfile(profile) },
                customAiGenerator = { request -> WorkoutPlanGenerator.generateCustom(profile, request, appContext) },
            )
        } else if (selected == RootDestination.Nutrition) {
            NutritionScreen(padding, profile)
        } else if (selected == RootDestination.Analytics) {
            val analyticsExercises = displayedWorkoutState.workouts.flatMap { it.exercises }
            AnalyticsScreen(appContext, displayedWorkoutState, appPreferences, WorkoutAnalyticsStore.summaries(appContext, analyticsExercises), padding)
        } else if (selected == RootDestination.Profile) {
            ProfileScreen(
                initial = profile,
                contentPadding = padding,
                isRegenerating = isGeneratingProfilePlan,
                generationError = profileGenerationError,
                currentStreak = trainingProgress.currentStreak.coerceAtLeast(displayedWorkoutState.user.currentStreak),
                completedWorkouts = appPreferences.getInt("total_completed_workouts", 0),
                longestStreak = trainingProgress.longestStreak,
                experienceXp = trainingProgress.xp,
                experienceLevel = trainingProgress.level,
                experienceProgress = trainingProgress.levelProgress,
                onRequestHealthConnect = onRequestHealthConnect,
                onSave = onProfileUpdated,
                onRegenerate = onRegenerateProfile,
            )
        } else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("${selected.label}\nPróxima vertical", color = WildforceThemeTokens.textSecondary)
        }
    }
    if (isGeneratingProfilePlan) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.58f)).clickable { }, contentAlignment = Alignment.Center) {
            Column(Modifier.liquidGlass(RoundedCornerShape(24.dp), emphasized = true).padding(26.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = WildforceThemeTokens.accentGold)
                Text("CREANDO TU SIGUIENTE PLAN", Modifier.padding(top = 14.dp), color = WildforceThemeTokens.textPrimary, fontWeight = FontWeight.Bold)
            }
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
