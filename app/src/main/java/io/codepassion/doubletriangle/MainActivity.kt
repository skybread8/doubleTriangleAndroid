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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.compose.ui.platform.LocalContext
import io.codepassion.doubletriangle.core.model.PreviewWorkoutRepository
import io.codepassion.doubletriangle.core.designsystem.WildforceTheme
import io.codepassion.doubletriangle.core.designsystem.WildforceThemeTokens
import io.codepassion.doubletriangle.core.designsystem.liquidGlass
import io.codepassion.doubletriangle.core.designsystem.liquidGlassBackground
import io.codepassion.doubletriangle.feature.onboarding.BodyCompositionPhase
import io.codepassion.doubletriangle.feature.onboarding.Equipment
import io.codepassion.doubletriangle.feature.onboarding.FitnessGoal
import io.codepassion.doubletriangle.feature.onboarding.GymType
import io.codepassion.doubletriangle.feature.onboarding.LifestyleLevel
import io.codepassion.doubletriangle.feature.onboarding.MovementRestriction
import io.codepassion.doubletriangle.feature.onboarding.TrainingLevel
import io.codepassion.doubletriangle.feature.onboarding.TrainingSplitPreference
import io.codepassion.doubletriangle.feature.onboarding.WorkoutFocus
import io.codepassion.doubletriangle.feature.onboarding.WorkoutWeekday
import io.codepassion.doubletriangle.feature.onboarding.OnboardingProfile
import io.codepassion.doubletriangle.feature.onboarding.OnboardingScreen
import io.codepassion.doubletriangle.feature.workout.WorkoutHubScreen

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
    var pendingHealthResult by remember { mutableStateOf<((Boolean) -> Unit)?>(null) }
    val healthPermissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract(),
    ) { granted ->
        pendingHealthResult?.invoke(granted.containsAll(healthPermissions))
        pendingHealthResult = null
    }
    val requestHealthConnect: (((Boolean) -> Unit) -> Unit) = { result ->
        if (HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE) {
            pendingHealthResult = result
            healthPermissionLauncher.launch(healthPermissions)
        } else result(false)
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
                )
            },
        )
    }

    val currentProfile = profile
    if (currentProfile == null) {
        OnboardingScreen(onRequestHealthConnect = requestHealthConnect) { completedProfile ->
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
                .apply()
            profile = completedProfile
        }
    } else {
        WildforceApp(
            profile = currentProfile,
            onResetOnboarding = {
                preferences.edit().clear().apply()
                profile = null
            },
        )
    }
}

@Composable
private fun WildforceApp(profile: OnboardingProfile, onResetOnboarding: () -> Unit) {
    var selected by remember { mutableStateOf(RootDestination.Workout) }
    Scaffold(
        modifier = Modifier.liquidGlassBackground(),
        backgroundColor = androidx.compose.ui.graphics.Color.Transparent,
        bottomBar = {
            BottomNavigation(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    .liquidGlass(RoundedCornerShape(18.dp), emphasized = true),
                backgroundColor = androidx.compose.ui.graphics.Color.Transparent,
                elevation = 0.dp,
            ) {
                RootDestination.values().forEach { destination ->
                    BottomNavigationItem(
                        selected = selected == destination,
                        onClick = { selected = destination },
                        icon = { Text(destination.glyph, fontWeight = FontWeight.Bold) },
                        label = { Text(destination.label) },
                        selectedContentColor = WildforceThemeTokens.accentGold,
                        unselectedContentColor = WildforceThemeTokens.textSecondary,
                    )
                }
            }
        },
    ) { padding ->
        if (selected == RootDestination.Workout) {
            WorkoutHubScreen(
                contentPadding = padding,
                state = PreviewWorkoutRepository.load(profile.name, profile.goal.title),
            )
        }
        else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = if (selected == RootDestination.Profile) "${selected.label}\nReiniciar onboarding"
                else "${selected.label}\nPróxima vertical",
                color = WildforceThemeTokens.textSecondary,
                modifier = if (selected == RootDestination.Profile) Modifier.clickable(onClick = onResetOnboarding) else Modifier,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AppPreview() = WildforceTheme {
    WildforceApp(OnboardingProfile("Jordi", FitnessGoal.BuildMuscle)) {}
}
