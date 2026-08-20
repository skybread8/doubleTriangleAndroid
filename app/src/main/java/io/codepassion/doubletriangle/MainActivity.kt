package io.codepassion.doubletriangle

import android.os.Bundle
import androidx.activity.ComponentActivity
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
import androidx.compose.ui.platform.LocalContext
import io.codepassion.doubletriangle.core.model.PreviewWorkoutRepository
import io.codepassion.doubletriangle.core.designsystem.WildforceTheme
import io.codepassion.doubletriangle.core.designsystem.WildforceThemeTokens
import io.codepassion.doubletriangle.core.designsystem.liquidGlass
import io.codepassion.doubletriangle.core.designsystem.liquidGlassBackground
import io.codepassion.doubletriangle.feature.onboarding.FitnessGoal
import io.codepassion.doubletriangle.feature.onboarding.LifestyleLevel
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
    var profile by remember {
        mutableStateOf(
            preferences.getString("name", null)?.let { name ->
                OnboardingProfile(
                    name = name,
                    goal = FitnessGoal.fromStoredValue(preferences.getString("goal", "").orEmpty()),
                    lifestyle = LifestyleLevel.fromStoredValue(preferences.getString("lifestyle", "").orEmpty()),
                    workoutDays = WorkoutWeekday.fromStoredValues(preferences.getStringSet("workout_days", null)),
                    preferredWorkoutDurationMinutes = preferences.getInt("workout_duration", 50),
                )
            },
        )
    }

    val currentProfile = profile
    if (currentProfile == null) {
        OnboardingScreen { completedProfile ->
            preferences.edit()
                .putString("name", completedProfile.name)
                .putString("goal", completedProfile.goal.storedValue)
                .putString("lifestyle", completedProfile.lifestyle.storedValue)
                .putStringSet("workout_days", completedProfile.workoutDays.mapTo(mutableSetOf()) { it.storedValue })
                .putInt("workout_duration", completedProfile.preferredWorkoutDurationMinutes)
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
