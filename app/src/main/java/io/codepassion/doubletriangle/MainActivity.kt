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
import io.codepassion.doubletriangle.feature.workout.ActiveWorkoutScreen
import io.codepassion.doubletriangle.feature.workout.WorkoutDetailScreen
import io.codepassion.doubletriangle.feature.workout.WorkoutHubScreen
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
                .apply()
            generationError = null
            if (useAi) {
                isGenerating = true
                coroutineScope.launch {
                    runCatching { WorkoutPlanGenerator.generate(completedProfile.name, completedProfile.goal.title) }
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
        val restoredState = generatedWorkoutState ?: preferences.getString("workout_plan_json", null)?.let { json ->
            runCatching { WorkoutPlanGenerator.parse(json, currentProfile.name, currentProfile.goal.title) }.getOrNull()
        } ?: PreviewWorkoutRepository.load(currentProfile.name, currentProfile.goal.title)
        WildforceApp(
            profile = currentProfile,
            workoutState = restoredState,
            onResetOnboarding = {
                preferences.edit().clear().apply()
                profile = null
            },
        )
    }
}

@Composable
private fun WildforceApp(profile: OnboardingProfile, workoutState: WorkoutHubState, onResetOnboarding: () -> Unit) {
    var selected by remember { mutableStateOf(RootDestination.Workout) }
    var workoutDetail by remember { mutableStateOf<WorkoutDaySummary?>(null) }
    var activeWorkout by remember { mutableStateOf<WorkoutDaySummary?>(null) }
    activeWorkout?.let { workout ->
        ActiveWorkoutScreen(workout) { activeWorkout = null; workoutDetail = null }
        return
    }
    workoutDetail?.let { workout ->
        WorkoutDetailScreen(workout, onBack = { workoutDetail = null }, onStart = { activeWorkout = workout })
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
            WorkoutHubScreen(contentPadding = padding, state = workoutState, onWorkoutSelected = { workoutDetail = it })
        } else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = if (selected == RootDestination.Profile) "${selected.label}\nReiniciar onboarding" else "${selected.label}\nPróxima vertical",
                color = WildforceThemeTokens.textSecondary,
                modifier = if (selected == RootDestination.Profile) Modifier.clickable(onClick = onResetOnboarding) else Modifier,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AppPreview() = WildforceTheme {
    WildforceApp(OnboardingProfile("Jordi", FitnessGoal.BuildMuscle), PreviewWorkoutRepository.load()) {}
}
