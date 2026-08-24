package io.codepassion.doubletriangle

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import io.codepassion.doubletriangle.feature.onboarding.WorkoutFocus
import io.codepassion.doubletriangle.feature.onboarding.WorkoutWeekday
import io.codepassion.doubletriangle.feature.onboarding.OnboardingProfile
import io.codepassion.doubletriangle.feature.onboarding.OnboardingScreen
import io.codepassion.doubletriangle.feature.onboarding.ProfileScreen
import io.codepassion.doubletriangle.feature.workout.ActiveWorkoutScreen
import io.codepassion.doubletriangle.feature.workout.WorkoutDetailScreen
import io.codepassion.doubletriangle.feature.workout.WorkoutHubScreen
import io.codepassion.doubletriangle.feature.workout.WorkoutAnalyticsStore
import io.codepassion.doubletriangle.nutrition.NutritionScreen
import java.time.Instant
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                }.getOrDefault(null to null)
                pendingHealthResult?.invoke(true, imported.first, imported.second)
                pendingHealthResult = null
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
                    preferredWorkoutDurationMinutes = preferences.getInt("workout_duration", 50),
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
                    birthMonth = preferences.getInt("birth_month", 1),
                    birthYear = preferences.getInt("birth_year", 1995),
                    gender = Gender.fromStoredValue(preferences.getString("gender", "").orEmpty()),
                    metricSystem = MetricSystem.fromStoredValue(preferences.getString("metric_system", "").orEmpty()),
                    heightCm = preferences.getInt("height_cm", 175),
                    weightKg = java.lang.Double.longBitsToDouble(preferences.getLong("weight_kg", java.lang.Double.doubleToRawLongBits(70.0))),
                    skipsWarmups = preferences.getBoolean("skips_warmups", false),
                    skipsCooldowns = preferences.getBoolean("skips_cooldowns", false),
                    skipsRestPeriods = preferences.getBoolean("skips_rest_periods", false),
                    workoutPlannerNotes = preferences.getString("planner_notes", "").orEmpty(),
                )
            },
        )
    }

    var generatedWorkoutState by remember { mutableStateOf<WorkoutHubState?>(null) }
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
                .apply()
            generationError = null
            if (useAi) {
                isGenerating = true
                coroutineScope.launch {
                    runCatching { WorkoutPlanGenerator.generate(completedProfile) }
                        .onSuccess { (json, state) ->
                            preferences.edit().putString("workout_plan_json", json).apply()
                            generatedWorkoutState = state
                            profile = completedProfile
                        }
                        .onFailure { generationError = it.message ?: "No se pudo generar el plan" }
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
                val completed = preferences.getStringSet("completed_workouts", emptySet()).orEmpty() + id
                preferences.edit().putStringSet("completed_workouts", completed)
                    .putInt("last_workout_duration", duration).putInt("last_workout_sets", sets)
                    .putLong("last_workout_volume", java.lang.Double.doubleToRawLongBits(volume)).apply()
            },
            onResetOnboarding = {
                preferences.edit().clear().apply()
                profile = null
            },
            onProfileUpdated = { updated ->
                preferences.edit()
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
                    .putInt("birth_month", updated.birthMonth).putInt("birth_year", updated.birthYear)
                    .putString("gender", updated.gender.storedValue).putString("metric_system", updated.metricSystem.storedValue)
                    .putInt("height_cm", updated.heightCm).putLong("weight_kg", java.lang.Double.doubleToRawLongBits(updated.weightKg))
                    .putBoolean("skips_warmups", updated.skipsWarmups)
                    .putBoolean("skips_cooldowns", updated.skipsCooldowns)
                    .putBoolean("skips_rest_periods", updated.skipsRestPeriods)
                    .putString("planner_notes", updated.workoutPlannerNotes)
                    .remove("workout_plan_json")
                    .apply()
                profile = updated
                generatedWorkoutState = null
            },
            onRegenerateProfile = { updated ->
                generationError = null
                isGenerating = true
                coroutineScope.launch {
                    runCatching { WorkoutPlanGenerator.generate(updated) }
                        .onSuccess { (json, state) ->
                            preferences.edit().putString("workout_plan_json", json).apply()
                            generatedWorkoutState = state
                        }
                        .onFailure { generationError = it.message ?: "No se pudo regenerar el plan" }
                    isGenerating = false
                }
            },
            isGeneratingProfilePlan = isGenerating,
            profileGenerationError = generationError,
        )
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
    isGeneratingProfilePlan: Boolean = false,
    profileGenerationError: String? = null,
) {
    val appContext = LocalContext.current.applicationContext
    val appPreferences = remember { appContext.getSharedPreferences("wildforce_profile", 0) }
    var selected by remember { mutableStateOf(RootDestination.Workout) }
    var displayedWorkoutState by remember(workoutState) { mutableStateOf(workoutState) }
    var workoutDetail by remember { mutableStateOf<WorkoutDaySummary?>(null) }
    var activeWorkout by remember { mutableStateOf<WorkoutDaySummary?>(null) }
    activeWorkout?.let { workout ->
        ActiveWorkoutScreen(
            workout = workout,
                gender = "male",
            skipRestPeriods = profile.skipsRestPeriods,
            currentStreak = displayedWorkoutState.user.currentStreak,
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
        WorkoutDetailScreen(workout, "male", onBack = { workoutDetail = null }, onStart = { activeWorkout = workout }, onSkip = {
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
        })
        return
    }
    Scaffold(
        modifier = Modifier.liquidGlassBackground(),
        backgroundColor = androidx.compose.ui.graphics.Color.Transparent,
        bottomBar = {
            BottomNavigation(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp).liquidGlass(RoundedCornerShape(18.dp), emphasized = true),
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
                onWorkoutSelected = { workoutDetail = it },
                gender = "male",
                customAiGenerator = { request -> WorkoutPlanGenerator.generateCustom(profile, request) },
            )
        } else if (selected == RootDestination.Nutrition) {
            NutritionScreen()
        } else if (selected == RootDestination.Analytics) {
            val analyticsExercises = displayedWorkoutState.workouts.flatMap { it.exercises }
            AnalyticsScreen(appContext, displayedWorkoutState, appPreferences, WorkoutAnalyticsStore.summaries(appContext, analyticsExercises))
        } else if (selected == RootDestination.Profile) {
            ProfileScreen(profile, padding, isGeneratingProfilePlan, profileGenerationError, onProfileUpdated, onRegenerateProfile)
        } else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("${selected.label}\nPróxima vertical", color = WildforceThemeTokens.textSecondary)
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
