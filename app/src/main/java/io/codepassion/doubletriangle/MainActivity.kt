package io.codepassion.doubletriangle

import io.codepassion.wildforce.android.R
import io.codepassion.wildforce.android.BuildConfig

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
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Scaffold
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.tween
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.TextButton
import androidx.compose.material.Text
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.ArrowCircleDown
import androidx.compose.material.icons.filled.ArrowCircleUp
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
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
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.LeanBodyMassRecord
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.core.view.WindowCompat
import android.graphics.Color as AndroidColor
import java.util.Locale
import io.codepassion.doubletriangle.core.model.confirmGeneratedPlan
import io.codepassion.doubletriangle.core.model.PreviewWorkoutRepository
import io.codepassion.doubletriangle.core.model.ExerciseSummary
import io.codepassion.doubletriangle.core.model.UserSummary
import io.codepassion.doubletriangle.core.model.WorkoutDaySummary
import io.codepassion.doubletriangle.core.model.WorkoutHubState
import io.codepassion.doubletriangle.core.model.WorkoutStatus
import io.codepassion.doubletriangle.core.model.isPlanFinalized
import io.codepassion.doubletriangle.core.model.withPlanCompletionStatus
import io.codepassion.doubletriangle.core.designsystem.WildforceTheme
import io.codepassion.doubletriangle.core.designsystem.WildforceThemeTokens
import io.codepassion.doubletriangle.core.designsystem.liquidGlass
import io.codepassion.doubletriangle.core.designsystem.liquidGlassBackground
import io.codepassion.doubletriangle.core.designsystem.PlanGenerationAnimation
import io.codepassion.doubletriangle.feature.onboarding.BodyCompositionPhase
import io.codepassion.doubletriangle.feature.onboarding.BodyMetricsStore
import io.codepassion.doubletriangle.feature.onboarding.ImportedBodyMetric
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
import io.codepassion.doubletriangle.feature.onboarding.saveAuthenticatedAccount
import io.codepassion.doubletriangle.feature.onboarding.SubscriptionAccessStore
import io.codepassion.doubletriangle.feature.onboarding.subscriptionAccess
import io.codepassion.doubletriangle.feature.onboarding.clearLocalAccountData
import io.codepassion.doubletriangle.feature.onboarding.ProfileScreen
import io.codepassion.doubletriangle.feature.onboarding.SubscriptionPaywall
import io.codepassion.doubletriangle.feature.onboarding.SubscriptionTier
import io.codepassion.doubletriangle.feature.workout.ActiveWorkoutScreen
import io.codepassion.doubletriangle.feature.workout.CompletionProgressStore
import io.codepassion.doubletriangle.feature.workout.ExerciseGuideScreen
import io.codepassion.doubletriangle.feature.workout.ExerciseVisualCatalog
import io.codepassion.doubletriangle.feature.workout.exerciseDisplayName
import io.codepassion.doubletriangle.feature.workout.WorkoutDetailScreen
import io.codepassion.doubletriangle.feature.workout.CustomWorkoutStore
import io.codepassion.doubletriangle.feature.workout.WorkoutHubScreen
import io.codepassion.doubletriangle.feature.workout.WorkoutAnalyticsStore
import io.codepassion.doubletriangle.feature.workout.WorkoutCompletionMetricsStore
import io.codepassion.doubletriangle.feature.workout.WorkoutSessionStore
import io.codepassion.doubletriangle.feature.workout.RemoteTrainingImage
import io.codepassion.doubletriangle.feature.workout.exerciseImageUrl
import io.codepassion.doubletriangle.nutrition.NutritionScreen
import java.time.Instant
import java.io.File
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

/** Reads the complete dated body-metric history for the manual Profile sync. */
private suspend fun readHealthConnectBodyMetricHistory(
    context: android.content.Context,
    granted: Set<String>,
): List<ImportedBodyMetric> {
    val client = HealthConnectClient.getOrCreate(context)
    val timeRange = TimeRangeFilter.before(Instant.now())
    val zone = java.time.ZoneId.systemDefault()
    val byDate = sortedMapOf<java.time.LocalDate, ImportedBodyMetric>()

    fun update(date: java.time.LocalDate, change: (ImportedBodyMetric) -> ImportedBodyMetric) {
        val key = date.toString()
        byDate[date] = change(byDate[date] ?: ImportedBodyMetric(key))
    }

    val heights = if (HealthPermission.getReadPermission(HeightRecord::class) in granted) {
        runCatching {
            client.readRecords(
                ReadRecordsRequest(HeightRecord::class, timeRangeFilter = timeRange, ascendingOrder = true),
            ).records
        }.getOrDefault(emptyList())
    } else emptyList()
    heights.forEach { record ->
        update(record.time.atZone(zone).toLocalDate()) { it.copy(heightCm = record.height.inMeters * 100.0) }
    }

    val weights = if (HealthPermission.getReadPermission(WeightRecord::class) in granted) {
        runCatching {
            client.readRecords(
                ReadRecordsRequest(WeightRecord::class, timeRangeFilter = timeRange, ascendingOrder = true),
            ).records
        }.getOrDefault(emptyList())
    } else emptyList()
    weights.forEach { record ->
        update(record.time.atZone(zone).toLocalDate()) { it.copy(weightKg = record.weight.inKilograms) }
    }

    val bodyFat = if (HealthPermission.getReadPermission(BodyFatRecord::class) in granted) {
        runCatching {
            client.readRecords(
                ReadRecordsRequest(BodyFatRecord::class, timeRangeFilter = timeRange, ascendingOrder = true),
            ).records
        }.getOrDefault(emptyList())
    } else emptyList()
    bodyFat.forEach { record ->
        update(record.time.atZone(zone).toLocalDate()) { it.copy(bodyFatPercentage = record.percentage.value) }
    }

    val leanMass = if (HealthPermission.getReadPermission(LeanBodyMassRecord::class) in granted) {
        runCatching {
            client.readRecords(
                ReadRecordsRequest(LeanBodyMassRecord::class, timeRangeFilter = timeRange, ascendingOrder = true),
            ).records
        }.getOrDefault(emptyList())
    } else emptyList()
    leanMass.forEach { record ->
        update(record.time.atZone(zone).toLocalDate()) { it.copy(leanBodyMassKg = record.mass.inKilograms) }
    }

    return byDate.values.toList()
}

/** Android counterpart of iOS FullFocusModeManager's workout shield lifecycle. */
private object AndroidFullFocusModeManager {
    private const val DETAIL_PREFERENCES = "wildforce_profile_details"
    private const val RUNTIME_PREFERENCES = "wildforce_full_focus_runtime"
    private const val PREVIOUS_FILTER = "previous_interruption_filter"
    private const val APPLIED = "interruption_filter_applied"

    fun reconcile(context: android.content.Context, hasActiveWorkout: Boolean) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return

        val detailPreferences = context.getSharedPreferences(DETAIL_PREFERENCES, android.content.Context.MODE_PRIVATE)
        val runtimePreferences = context.getSharedPreferences(RUNTIME_PREFERENCES, android.content.Context.MODE_PRIVATE)
        val notificationManager = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val wasApplied = runtimePreferences.getBoolean(APPLIED, false)

        if (!hasActiveWorkout || !detailPreferences.getBoolean("full_focus", false)) {
            if (wasApplied) {
                val previousFilter = runtimePreferences.getInt(PREVIOUS_FILTER, android.app.NotificationManager.INTERRUPTION_FILTER_ALL)
                runCatching { notificationManager.setInterruptionFilter(previousFilter) }
                runtimePreferences.edit().remove(PREVIOUS_FILTER).putBoolean(APPLIED, false).apply()
            }
            return
        }

        // Android requires explicit Notification Policy access before an app can
        // apply Zen Mode. If it is missing, the profile setting remains enabled
        // and the user can grant it from the settings action beside the toggle.
        if (!notificationManager.isNotificationPolicyAccessGranted || wasApplied) return

        runtimePreferences.edit()
            .putInt(PREVIOUS_FILTER, notificationManager.currentInterruptionFilter)
            .putBoolean(APPLIED, true)
            .apply()
        runCatching {
            notificationManager.setInterruptionFilter(android.app.NotificationManager.INTERRUPTION_FILTER_NONE)
        }.onFailure {
            runtimePreferences.edit().remove(PREVIOUS_FILTER).putBoolean(APPLIED, false).apply()
        }
    }
}

class MainActivity : ComponentActivity() {
    private val requestedRootDestination = androidx.compose.runtime.mutableStateOf<RootDestination?>(null)
    private val openNutritionLogging = androidx.compose.runtime.mutableStateOf(false)
    private val openActiveWorkout = androidx.compose.runtime.mutableStateOf(false)
    private val openWorkoutDetail = androidx.compose.runtime.mutableStateOf(false)
    private val openBodyMetrics = androidx.compose.runtime.mutableStateOf(false)
    private val openFeedback = androidx.compose.runtime.mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedRootDestination.value = intent.requestedRootDestination()
        openNutritionLogging.value = intent.opensNutritionLogging()
        openActiveWorkout.value = intent.opensActiveWorkout()
        openWorkoutDetail.value = intent.opensWorkoutDetail()
        openBodyMetrics.value = intent.opensBodyMetrics()
        openFeedback.value = intent.opensFeedback()
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
        AppQuickActions.refresh(this)
        setContent {
            val systemDarkTheme = isSystemInDarkTheme()
            val savedTheme = remember { getSharedPreferences("wildforce_profile_details", 0).getString("theme", "Sistema") ?: "Sistema" }
            WildforceTheme(darkTheme = when (savedTheme) { "Claro" -> false; "Oscuro" -> true; else -> systemDarkTheme }) {
                // Apply only the top inset here. The bottom navigation owns the
                // navigation-bar inset; applying safeDrawing at the root as
                // well created an extra bottom gap on devices such as S23 Ultra.
                Box(Modifier.fillMaxSize().statusBarsPadding()) {
                    WildforceRoot(
                        requestedDestination = requestedRootDestination.value,
                        openNutritionLogging = openNutritionLogging.value,
                        openActiveWorkout = openActiveWorkout.value,
                        openWorkoutDetail = openWorkoutDetail.value,
                        openBodyMetrics = openBodyMetrics.value,
                        openFeedback = openFeedback.value,
                        onNutritionLoggingOpened = { openNutritionLogging.value = false },
                        onActiveWorkoutOpened = { openActiveWorkout.value = false },
                        onWorkoutDetailOpened = { openWorkoutDetail.value = false },
                        onBodyMetricsOpened = { openBodyMetrics.value = false },
                        onFeedbackOpened = { openFeedback.value = false },
                        onThemeChanged = { recreate() },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        requestedRootDestination.value = intent.requestedRootDestination()
        openNutritionLogging.value = intent.opensNutritionLogging()
        openActiveWorkout.value = intent.opensActiveWorkout()
        openWorkoutDetail.value = intent.opensWorkoutDetail()
        openBodyMetrics.value = intent.opensBodyMetrics()
        openFeedback.value = intent.opensFeedback()
    }

    override fun onResume() {
        super.onResume()
        AppQuickActions.refresh(this)
    }
}

private const val EXTRA_OPEN_NUTRITION = "io.codepassion.doubletriangle.OPEN_NUTRITION"
private const val EXTRA_OPEN_NUTRITION_LOGGING = "io.codepassion.doubletriangle.OPEN_NUTRITION_LOGGING"
private const val EXTRA_OPEN_ACTIVE_WORKOUT = "io.codepassion.doubletriangle.OPEN_ACTIVE_WORKOUT"

internal fun android.content.Intent.requestedRootDestination(): RootDestination? =
    when {
        getBooleanExtra(EXTRA_OPEN_NUTRITION, false) || opensNutritionLogging() -> RootDestination.Nutrition
        opensWorkoutDetail() || opensActiveWorkout() || opensFeedback() -> RootDestination.Workout
        opensBodyMetrics() -> RootDestination.Profile
        else -> null
    }

internal fun android.content.Intent.opensNutritionLogging(): Boolean =
    getBooleanExtra(EXTRA_OPEN_NUTRITION_LOGGING, false) || data?.isWildforceRoute("log-meal") == true

internal fun android.content.Intent.opensActiveWorkout(): Boolean =
    getBooleanExtra(EXTRA_OPEN_ACTIVE_WORKOUT, false) || data?.isWildforceRoute("workout") == true

internal fun android.content.Intent.opensWorkoutDetail(): Boolean =
    data?.isWildforceRoute("start-workout") == true

internal fun android.content.Intent.opensBodyMetrics(): Boolean =
    data?.isWildforceRoute("body-metrics") == true

internal fun android.content.Intent.opensFeedback(): Boolean =
    data?.isWildforceRoute("leaving-feedback") == true

internal fun android.net.Uri.isWildforceRoute(host: String): Boolean =
    appEntryRoute(scheme, this.host) == appEntryRoute("wildforce", host)

internal enum class AppEntryRoute { StartWorkout, LogMeal, BodyMetrics, LeavingFeedback }

internal fun appEntryRoute(scheme: String?, host: String?): AppEntryRoute? =
    if (scheme != "wildforce") null else when (host) {
        "start-workout" -> AppEntryRoute.StartWorkout
        "log-meal" -> AppEntryRoute.LogMeal
        "body-metrics" -> AppEntryRoute.BodyMetrics
        "leaving-feedback" -> AppEntryRoute.LeavingFeedback
        else -> null
    }

internal fun appEntryRootDestination(scheme: String?, host: String?): RootDestination? = when (appEntryRoute(scheme, host)) {
    AppEntryRoute.StartWorkout, AppEntryRoute.LeavingFeedback -> RootDestination.Workout
    AppEntryRoute.LogMeal -> RootDestination.Nutrition
    AppEntryRoute.BodyMetrics -> RootDestination.Profile
    null -> null
}

internal enum class RootDestination(
    @param:androidx.annotation.StringRes val labelRes: Int,
    val icon: ImageVector,
    val usesCarrotIcon: Boolean = false,
) {
    Workout(R.string.ios_catalog_7b3abf24fddda1de, Icons.Filled.FitnessCenter),
    // iOS uses `carrot.fill`; the Material set has no carrot glyph, so the
    // root dock renders the same silhouette with CarrotFillIcon below.
    Nutrition(R.string.ios_catalog_7ab78b7058dd1c09, Icons.Filled.Restaurant, usesCarrotIcon = true),
    Analytics(R.string.ios_catalog_94c116ee118a7299, Icons.Filled.BarChart), Gym(R.string.ios_catalog_fbeb6d88367ec305, Icons.Filled.Apartment), Profile(R.string.ios_catalog_d696a35bdd1883da, Icons.Filled.AccountCircle),
}

/**
 * Mirrors iOS's InitialSyncView: a newly authenticated account stays outside
 * the app until its first pull-then-push synchronization completes.
 */
private sealed interface InitialSyncState {
    data object Synchronizing : InitialSyncState
    data class Failed(val message: String) : InitialSyncState
}

@Composable
private fun InitialSyncScreen(
    isSynchronizing: Boolean,
    message: String?,
    onRetry: () -> Unit,
) {
    Box(
        // iOS InitialSyncView uses the plain shared background, rather than
        // the liquid-glass treatment used by Android's main destinations.
        modifier = Modifier.fillMaxSize().background(WildforceThemeTokens.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            // LoadingState on iOS has 16 pt horizontal padding and a 60 pt
            // top inset, which puts its centered content slightly lower.
            modifier = if (isSynchronizing) {
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .padding(top = 60.dp)
            } else {
                Modifier.fillMaxWidth().padding(horizontal = 32.dp)
            },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = if (isSynchronizing) {
                Arrangement.spacedBy(20.dp, Alignment.CenterVertically)
            } else {
                Arrangement.spacedBy(14.dp)
            },
        ) {
            if (isSynchronizing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(30.dp),
                    color = WildforceThemeTokens.accent,
                )
                Text(
                    text = androidx.compose.ui.res.stringResource(R.string.ios_catalog_6a38b69e9fc567c1),
                    style = androidx.compose.material.MaterialTheme.typography.subtitle1,
                    color = WildforceThemeTokens.textPrimary,
                )
                Text(
                    text = androidx.compose.ui.res.stringResource(R.string.ios_catalog_5e05fbfeb6e4b7d6),
                    fontSize = 15.sp,
                    color = WildforceThemeTokens.textSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            } else {
                Icon(Icons.Filled.CloudOff, contentDescription = null, modifier = Modifier.size(48.dp), tint = WildforceThemeTokens.accent)
                Text(
                    "No hemos podido sincronizar tus datos",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                Text(
                    message ?: "Comprueba tu conexión e inténtalo de nuevo.",
                    color = WildforceThemeTokens.textSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = WildforceThemeTokens.accent,
                        contentColor = WildforceThemeTokens.primaryButtonText,
                    ),
                ) {
                    Text("REINTENTAR")
                }
            }
        }
    }
}

@Composable
private fun RootBottomNavigation(destinations: List<RootDestination>, selected: RootDestination, onSelected: (RootDestination) -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(31.dp)
    BottomNavigation(
        modifier = modifier.navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 6.dp)
            .height(62.dp)
            // This is the historical Android/iOS-matching dock: a floating
            // material bubble, not a full-width white rectangle.
            .shadow(10.dp, shape, clip = false)
            .clip(shape)
            .background(
                // Keep the exact translucent surface used by commit 7126e99.
                if (androidx.compose.material.MaterialTheme.colors.isLight) Color.White.copy(alpha = 0.88f)
                else Color.Black.copy(alpha = 0.68f),
            )
            .border(
                1.dp,
                if (androidx.compose.material.MaterialTheme.colors.isLight) Color.White.copy(alpha = 0.64f)
                else Color.White.copy(alpha = 0.24f),
                shape,
            ),
        backgroundColor = Color.Transparent,
        elevation = 0.dp,
    ) {
        destinations.forEach { destination ->
            val label = androidx.compose.ui.res.stringResource(destination.labelRes)
            BottomNavigationItem(
                selected = selected == destination,
                onClick = { onSelected(destination) },
                icon = {
                    Box(
                        Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                if (selected == destination) {
                                    WildforceThemeTokens.textPrimary.copy(alpha = 0.10f)
                                } else {
                                    Color.Transparent
                                },
                            )
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        val tint = if (androidx.compose.material.MaterialTheme.colors.isLight) Color(0xFF252525) else Color.White
                        if (destination.usesCarrotIcon) {
                            CarrotFillIcon(
                                tint = tint,
                                modifier = Modifier
                                    .size(28.dp)
                                    .semantics {
                                        contentDescription = if (selected == destination) "$label, seleccionada" else label
                                    },
                            )
                        } else {
                            Icon(
                                destination.icon,
                                contentDescription = if (selected == destination) "$label, seleccionada" else label,
                                modifier = Modifier.size(28.dp),
                                tint = tint,
                            )
                        }
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

/** Visual counterpart of iOS's `carrot.fill` symbol for the root dock. */
@Composable
private fun CarrotFillIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val leaves = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * .50f, h * .34f)
            cubicTo(w * .39f, h * .28f, w * .31f, h * .16f, w * .33f, h * .08f)
            cubicTo(w * .42f, h * .10f, w * .48f, h * .17f, w * .50f, h * .24f)
            cubicTo(w * .52f, h * .16f, w * .59f, h * .09f, w * .68f, h * .08f)
            cubicTo(w * .69f, h * .18f, w * .61f, h * .29f, w * .50f, h * .34f)
            close()
        }
        drawPath(leaves, tint)

        val carrot = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * .32f, h * .28f)
            cubicTo(w * .41f, h * .34f, w * .59f, h * .34f, w * .68f, h * .28f)
            cubicTo(w * .69f, h * .48f, w * .61f, h * .73f, w * .50f, h * .92f)
            cubicTo(w * .39f, h * .73f, w * .31f, h * .48f, w * .32f, h * .28f)
            close()
        }
        drawPath(carrot, tint)
    }
}

@Composable
private fun WildforceRoot(
    requestedDestination: RootDestination? = null,
    openNutritionLogging: Boolean = false,
    onNutritionLoggingOpened: () -> Unit = {},
    onActiveWorkoutOpened: () -> Unit = {},
    onWorkoutDetailOpened: () -> Unit = {},
    onBodyMetricsOpened: () -> Unit = {},
    onFeedbackOpened: () -> Unit = {},
    openActiveWorkout: Boolean = false,
    openWorkoutDetail: Boolean = false,
    openBodyMetrics: Boolean = false,
    openFeedback: Boolean = false,
    onThemeChanged: () -> Unit = {},
) {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("wildforce_profile", 0) }
    val workoutPlanStore = remember(preferences) { WorkoutPlanStateStore(preferences) }
    val healthPermissions = remember {
        setOf(
            HealthPermission.getReadPermission(HeightRecord::class),
            HealthPermission.getReadPermission(WeightRecord::class),
            HealthPermission.getReadPermission(androidx.health.connect.client.records.BodyFatRecord::class),
            HealthPermission.getReadPermission(androidx.health.connect.client.records.LeanBodyMassRecord::class),
            HealthPermission.getReadPermission(androidx.health.connect.client.records.ActiveCaloriesBurnedRecord::class),
            HealthPermission.getReadPermission(androidx.health.connect.client.records.TotalCaloriesBurnedRecord::class),
            HealthPermission.getWritePermission(WeightRecord::class),
            HealthPermission.getWritePermission(androidx.health.connect.client.records.BodyFatRecord::class),
            HealthPermission.getWritePermission(androidx.health.connect.client.records.LeanBodyMassRecord::class),
            HealthPermission.getWritePermission(androidx.health.connect.client.records.ExerciseSessionRecord::class),
            HealthPermission.getWritePermission(androidx.health.connect.client.records.ActiveCaloriesBurnedRecord::class),
            HealthPermission.getWritePermission(androidx.health.connect.client.records.NutritionRecord::class),
            HealthPermission.getReadPermission(androidx.health.connect.client.records.NutritionRecord::class),
        )
    }
    val coroutineScope = rememberCoroutineScope()
    var scheduledRemoteSync by remember { mutableStateOf<Job?>(null) }
    var pendingHealthResult by remember { mutableStateOf<((Boolean, Int?, Double?, Double?, Double?) -> Unit)?>(null) }
    var pendingHealthHistoryResult by remember { mutableStateOf<((Boolean, List<ImportedBodyMetric>) -> Unit)?>(null) }
    val healthPermissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract(),
    ) { granted ->
        // HealthKit can authorize each type independently. Health Connect has
        // the same user-facing behavior, so a denied optional read/write type
        // must not discard the body metrics that were granted.
        if (granted.any { it in healthPermissions }) {
            coroutineScope.launch {
                val client = HealthConnectClient.getOrCreate(context)
                val timeRange = TimeRangeFilter.before(Instant.now())
                val height = if (HealthPermission.getReadPermission(HeightRecord::class) in granted) {
                    runCatching {
                        client.readRecords(ReadRecordsRequest(HeightRecord::class, timeRangeFilter = timeRange, ascendingOrder = false, pageSize = 1))
                            .records.firstOrNull()?.height?.inMeters?.times(100)?.toInt()
                    }.getOrNull()
                } else null
                val weight = if (HealthPermission.getReadPermission(WeightRecord::class) in granted) {
                    runCatching {
                        client.readRecords(ReadRecordsRequest(WeightRecord::class, timeRangeFilter = timeRange, ascendingOrder = false, pageSize = 1))
                            .records.firstOrNull()?.weight?.inKilograms
                    }.getOrNull()
                } else null
                val bodyFat = if (HealthPermission.getReadPermission(androidx.health.connect.client.records.BodyFatRecord::class) in granted) {
                    runCatching {
                        client.readRecords(ReadRecordsRequest(androidx.health.connect.client.records.BodyFatRecord::class, timeRangeFilter = timeRange, ascendingOrder = false, pageSize = 1))
                            .records.firstOrNull()?.percentage?.value
                    }.getOrNull()
                } else null
                val leanMass = if (HealthPermission.getReadPermission(androidx.health.connect.client.records.LeanBodyMassRecord::class) in granted) {
                    runCatching {
                        client.readRecords(ReadRecordsRequest(androidx.health.connect.client.records.LeanBodyMassRecord::class, timeRangeFilter = timeRange, ascendingOrder = false, pageSize = 1))
                            .records.firstOrNull()?.mass?.inKilograms
                    }.getOrNull()
                } else null
                val imported = Result.success(arrayOf(height?.toDouble(), weight, bodyFat, leanMass))
                imported.onSuccess { values ->
                    pendingHealthResult?.invoke(true, values[0]?.toInt(), values[1], values[3], values[2])
                    pendingHealthResult = null
                }.onFailure {
                    pendingHealthResult?.invoke(false, null, null, null, null)
                    pendingHealthResult = null
                }
            }
        } else {
            pendingHealthResult?.invoke(false, null, null, null, null)
            pendingHealthResult = null
        }
    }
    val healthHistoryPermissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract(),
    ) { granted ->
        val pending = pendingHealthHistoryResult
        pendingHealthHistoryResult = null
        if (pending != null) {
            if (granted.any { it in healthPermissions }) {
                coroutineScope.launch {
                    val imports = runCatching {
                        val actualGranted = HealthConnectClient.getOrCreate(context).permissionController.getGrantedPermissions()
                        readHealthConnectBodyMetricHistory(context, actualGranted)
                    }.getOrNull()
                    pending.invoke(imports != null, imports.orEmpty())
                }
            } else {
                pending(false, emptyList())
            }
        }
    }
    val requestHealthConnect: (((Boolean, Int?, Double?, Double?, Double?) -> Unit) -> Unit) = { result ->
        if (HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE) {
            pendingHealthResult = result
            healthPermissionLauncher.launch(healthPermissions)
        } else result(false, null, null, null, null)
    }
    val requestHealthConnectHistory: (((Boolean, List<ImportedBodyMetric>) -> Unit) -> Unit) = { result ->
        if (HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE) {
            pendingHealthHistoryResult = result
            healthHistoryPermissionLauncher.launch(healthPermissions)
        } else result(false, emptyList())
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
                    birthDay = preferences.getInt("birth_day", 1).coerceIn(1, 31),
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
                    gymJoinCode = preferences.getString("gym_join_code", null),
                    gymJoinName = preferences.getString("gym_join_name", null),
                )
            },
        )
    }

    var generatedWorkoutState by remember { mutableStateOf<WorkoutHubState?>(null) }
    var healthMetricsImported by remember { mutableStateOf(false) }
    var importedLeanBodyMassKg by remember { mutableStateOf<Double?>(null) }
    var importedBodyFatPercentage by remember { mutableStateOf<Double?>(null) }
    var initialPlanPreview by remember { mutableStateOf<Pair<String, WorkoutHubState>?>(null) }
    var initialPlanProfile by remember { mutableStateOf<OnboardingProfile?>(null) }
    var pendingGeneratedPlan by remember { mutableStateOf<Pair<String, WorkoutHubState>?>(null) }
    var isGenerating by remember { mutableStateOf(false) }
    var generationError by remember { mutableStateOf<String?>(null) }
    var initialSyncState by remember { mutableStateOf<InitialSyncState?>(null) }
    var initialSyncFallback by remember { mutableStateOf<OnboardingProfile?>(null) }
    var isInitialSyncPaywallPresented by remember { mutableStateOf(false) }
    var isInitialSyncGymJoinPresented by remember { mutableStateOf(false) }

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
            .putInt("birth_day", value.birthDay)
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
            .putString("gym_join_code", value.gymJoinCode)
            .putString("gym_join_name", value.gymJoinName)
            .apply()
        if (healthMetricsImported) {
            BodyMetricsStore.recordImported(
                context,
                value,
                leanBodyMassKg = importedLeanBodyMassKg,
                bodyFatPercentage = importedBodyFatPercentage,
            )
        }
    }

    fun synchronizeInitialAccount() {
        val fallback = initialSyncFallback ?: return
        if (initialSyncState == InitialSyncState.Synchronizing) return
        initialSyncState = InitialSyncState.Synchronizing
        coroutineScope.launch {
            // Just like iOS InitialSyncView, a partial pull is not enough to
            // enter the app: the initial pull-and-push must finish as one sync.
            when (val outcome = WorkoutRemoteSync.synchronize(context, fallback, preferences)) {
                is WorkoutRemoteSync.Outcome.Synchronized -> {
                    val resolvedProfile = outcome.importedProfile ?: fallback
                    persistOnboardingProfile(resolvedProfile)
                    workoutPlanStore.currentPlanJson()?.let { rawPlan ->
                        runCatching {
                            WorkoutPlanGenerator.parse(rawPlan, resolvedProfile.name, resolvedProfile.goal.title, resolvedProfile.bodyCompositionPhase?.storedValue)
                        }.onSuccess { importedPlan -> generatedWorkoutState = importedPlan }
                    }
                    profile = resolvedProfile
                    initialSyncState = null
                }

                WorkoutRemoteSync.Outcome.SubscriptionRequired -> {
                    initialSyncState = InitialSyncState.Failed("Necesitas una suscripción activa para sincronizar tus datos.")
                    isInitialSyncPaywallPresented = true
                }

                WorkoutRemoteSync.Outcome.NoAccount -> {
                    initialSyncState = InitialSyncState.Failed("Tu sesión ya no es válida. Vuelve a iniciar sesión.")
                }

                is WorkoutRemoteSync.Outcome.Failed -> {
                    initialSyncState = InitialSyncState.Failed(
                        outcome.message?.takeIf(String::isNotBlank)
                            ?: "Comprueba tu conexión e inténtalo de nuevo.",
                    )
                }
            }
        }
    }

    val currentProfile = profile
    if (currentProfile == null) {
        val activeInitialSync = initialSyncState
        if (activeInitialSync != null) {
            InitialSyncScreen(
                isSynchronizing = activeInitialSync == InitialSyncState.Synchronizing,
                message = (activeInitialSync as? InitialSyncState.Failed)?.message,
                onRetry = ::synchronizeInitialAccount,
            )
            if (isInitialSyncPaywallPresented) {
                SubscriptionPaywall(
                    accountId = context.getSharedPreferences("wildforce_account", 0).getString("id", null),
                    mandatory = true,
                    onJoinGymRequested = { isInitialSyncGymJoinPresented = true },
                    onPurchaseCompleted = {
                        isInitialSyncPaywallPresented = false
                        synchronizeInitialAccount()
                    },
                )
            }
            if (isInitialSyncGymJoinPresented) {
                AuthenticatedJoinGymDialog(
                    onDismiss = { isInitialSyncGymJoinPresented = false },
                    onJoined = {
                        isInitialSyncGymJoinPresented = false
                        isInitialSyncPaywallPresented = false
                        coroutineScope.launch {
                            context.getSharedPreferences("wildforce_account", 0).getString("token", null)?.let { token ->
                                subscriptionAccess(token).onSuccess { SubscriptionAccessStore.update(context, it) }
                            }
                            synchronizeInitialAccount()
                        }
                    },
                )
            }
        } else OnboardingScreen(
            onRequestHealthConnect = requestHealthConnect,
            onHealthMetricsImported = { granted, leanBodyMass, bodyFat ->
                healthMetricsImported = granted
                importedLeanBodyMassKg = leanBodyMass
                importedBodyFatPercentage = bodyFat
            },
            onCheckGymJoinCode = { code -> BookingApi(context).checkJoinCode(code) },
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
                    val confirmedState = generated.second.confirmGeneratedPlan()
                    workoutPlanStore.saveCurrentPlan(WorkoutPlanGenerator.serialize(confirmedState))
                    WorkoutRemoteSync.markResourcesDirty(context, "workout-plans", "workout-days", "workout-blocks", "planned-exercises", "exercise-results")
                    workoutPlanStore.saveProfileSignature(generatedProfile.workoutPlanProfileSignature())
                    WorkoutQuickAccessWidget.refresh(context)
                    AppQuickActions.refresh(context)
                    generatedWorkoutState = confirmedState
                    profile = generatedProfile
                    initialPlanPreview = null
                    initialPlanProfile = null
                }
            },
            onContinueWithoutPlan = {
                // iOS lets onboarding finish when AI generation fails. Keep the
                // registered profile and explicitly leave the workout hub empty;
                // never substitute a sample plan for an authenticated account.
                val completedProfile = initialPlanProfile ?: return@OnboardingScreen
                workoutPlanStore.clearCurrentPlan()
                persistOnboardingProfile(completedProfile)
                generatedWorkoutState = WorkoutHubState(
                    user = UserSummary(completedProfile.name, completedProfile.goal.title, 0),
                    trainingDays = emptySet(),
                    completedDays = emptySet(),
                    planName = "",
                    phase = "",
                    workouts = emptyList(),
                )
                profile = completedProfile
                initialPlanPreview = null
                initialPlanProfile = null
            },
            onLanguageSelected = { language ->
                context.getSharedPreferences("wildforce_profile_details", 0).edit().putString("language", language).apply()
                AppLocale.apply(language)
            },
            onLoggedIn = { response ->
                saveAuthenticatedAccount(context, response)
                preferences.edit()
                    .putBoolean("remote_sync_import_required", !response.isNewAccount)
                    .apply()
                initialSyncFallback = OnboardingProfile(
                    name = response.name.ifBlank { "Wildforce" },
                    goal = FitnessGoal.BuildMuscle,
                )
                synchronizeInitialAccount()
            },
            onRegistered = { response, _ ->
                // Mark the first sync as pending so it is uploaded once the local
                // profile and (when applicable) plan exist, rather than creating
                // an incomplete remote user.
                saveAuthenticatedAccount(context, response)
                WorkoutRemoteSync.markNewAccountResourcesDirty(context)
            },
        ) { completedProfile, useAi ->
            generationError = null
            if (useAi) {
                // Retain the completed profile before the async request so the
                // user can continue when generation is unavailable.
                initialPlanProfile = completedProfile
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
                val gymJoinCode = completedProfile.gymJoinCode
                if (gymJoinCode != null) {
                    // iOS completes the post-registration gym join before it
                    // creates the local user and starts the initial sync. Keep
                    // the same ordering; a failed join remains non-fatal so
                    // the user can retry from Profile later.
                    coroutineScope.launch {
                        runCatching { BookingApi(context).joinGym(gymJoinCode) }
                        profile = completedProfile
                    }
                } else {
                    profile = completedProfile
                }
            }
        }
    } else {
        val hasRemoteSession = context.getSharedPreferences("wildforce_account", 0).getString("token", null) != null
        val storedPlanJson = workoutPlanStore.currentPlanJson()
        // iOS reads the persisted SwiftData plan before rendering WorkoutHub.
        // Parse the compact local JSON in the same composition so a cold start
        // never flashes the generate-plan hero while an existing plan is still
        // being reconstructed in a background effect.
        val persistedWorkoutState = remember(storedPlanJson, currentProfile.name, currentProfile.goal.title, currentProfile.bodyCompositionPhase) {
            storedPlanJson?.let { rawPlan ->
                runCatching {
                    WorkoutPlanGenerator.parse(rawPlan, currentProfile.name, currentProfile.goal.title, currentProfile.bodyCompositionPhase?.storedValue)
                }.getOrNull()
            }
        }
        val baseState = generatedWorkoutState ?: persistedWorkoutState ?: if (hasRemoteSession) {
            // iOS never substitutes a sample plan for an authenticated account
            // whose initial sync is still pending, unavailable, or blocked by
            // subscription access. Keep the hub empty behind its loading/error
            // or paywall state until a real plan has been pulled.
            WorkoutHubState(
                user = UserSummary(currentProfile.name, currentProfile.goal.title, 0),
                trainingDays = emptySet(),
                completedDays = emptySet(),
                planName = "",
                phase = "",
                workouts = emptyList(),
            )
        } else PreviewWorkoutRepository.load(currentProfile.name, currentProfile.goal.title)
        val completedIds = preferences.getStringSet("completed_workouts", emptySet()).orEmpty()
        val completedDatesByWorkout = preferences.getStringSet("completed_workout_local_dates", emptySet())
            .orEmpty()
            .mapNotNull { entry ->
                val separator = entry.lastIndexOf('|')
                if (separator <= 0) return@mapNotNull null
                val date = runCatching { java.time.LocalDate.parse(entry.substring(separator + 1)) }.getOrNull()
                    ?: return@mapNotNull null
                entry.substring(0, separator) to date
            }
            .toMap()
        val skippedIds = preferences.getStringSet("skipped_workouts", emptySet()).orEmpty()
        val restoredState = baseState.copy(
            completedDays = baseState.workouts.filter { it.id in completedIds }.mapTo(mutableSetOf()) {
                completedDatesByWorkout[it.id]?.dayOfWeek ?: it.scheduledDay
            },
            workouts = baseState.workouts.map {
                when {
                    it.id in completedIds -> it.copy(status = WorkoutStatus.Completed)
                    it.id in skippedIds -> it.copy(status = WorkoutStatus.Skipped)
                    else -> it
                }
            },
        )
        var subscriptionRequired by remember { mutableStateOf(false) }
        fun applyPulledPlanIfAvailable() {
            if (!preferences.getBoolean("remote_plan_imported", false)) return
            preferences.edit().remove("remote_plan_imported").apply()
            workoutPlanStore.currentPlanJson()?.let { remotePlan ->
                runCatching { WorkoutPlanGenerator.parse(remotePlan, currentProfile.name, currentProfile.goal.title, currentProfile.bodyCompositionPhase?.storedValue) }
                    .onSuccess { generatedWorkoutState = it }
            }
        }
        // iOS debounces every SwiftData save before synchronizing. Android's
        // JSON stores do not emit database-save notifications, so centralize
        // the equivalent scheduling at each root-level persistence boundary.
        fun scheduleRemoteSync(profileToSync: OnboardingProfile) {
            scheduledRemoteSync?.cancel()
            scheduledRemoteSync = coroutineScope.launch {
                delay(2_000)
                when (val result = WorkoutRemoteSync.synchronize(context, profileToSync, preferences)) {
                    is WorkoutRemoteSync.Outcome.Synchronized -> {
                        preferences.edit().putBoolean("remote_sync_pending", false).apply()
                        applyPulledPlanIfAvailable()
                    }
                    WorkoutRemoteSync.Outcome.SubscriptionRequired -> subscriptionRequired = true
                    is WorkoutRemoteSync.Outcome.Failed -> {
                        if (result.importedRemotePlan) applyPulledPlanIfAvailable()
                        result.importedProfile?.let { imported ->
                            persistOnboardingProfile(imported)
                            profile = imported
                        }
                    }
                    WorkoutRemoteSync.Outcome.NoAccount -> Unit
                }
            }
        }
        LaunchedEffect(currentProfile.name, workoutPlanStore.currentPlanJson()) {
            if (preferences.getBoolean("remote_sync_pending", false) || hasRemoteSession) {
                when (val result = WorkoutRemoteSync.synchronize(context, currentProfile, preferences)) {
                    is WorkoutRemoteSync.Outcome.Synchronized -> {
                        preferences.edit().putBoolean("remote_sync_pending", false).apply()
                        applyPulledPlanIfAvailable()
                        result.importedProfile?.let { imported ->
                            persistOnboardingProfile(imported)
                            profile = imported
                        }
                    }
                    // iOS presents the mandatory paywall for every
                    // `subscription_required` sync response. Do not perform a
                    // second status check: an account without access is not
                    // necessarily labelled `expired` by the backend.
                    WorkoutRemoteSync.Outcome.SubscriptionRequired -> subscriptionRequired = true
                    is WorkoutRemoteSync.Outcome.Failed -> {
                        if (result.importedRemotePlan) applyPulledPlanIfAvailable()
                        result.importedProfile?.let { imported ->
                            persistOnboardingProfile(imported)
                            profile = imported
                        }
                    }
                    else -> Unit
                }
            }
        }
        // Same policy as iOS's scenePhase(.active): check server access again
        // whenever the app returns to foreground.  The paywall itself is only
        // forced for the explicit `expired` status, never for a network error.
        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner, currentProfile.name) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME && context.getSharedPreferences("wildforce_account", 0).getString("token", null) != null) {
                    coroutineScope.launch {
                        when (val result = WorkoutRemoteSync.synchronize(context, currentProfile, preferences)) {
                            WorkoutRemoteSync.Outcome.SubscriptionRequired -> subscriptionRequired = true
                            is WorkoutRemoteSync.Outcome.Synchronized -> {
                                preferences.edit().putBoolean("remote_sync_pending", false).apply()
                                applyPulledPlanIfAvailable()
                            }
                            is WorkoutRemoteSync.Outcome.Failed -> {
                                if (result.importedRemotePlan) applyPulledPlanIfAvailable()
                                result.importedProfile?.let { imported ->
                                    persistOnboardingProfile(imported)
                                    profile = imported
                                }
                            }
                            else -> Unit
                        }
                    }
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }
        WildforceApp(
            profile = currentProfile,
            workoutState = restoredState,
            requestedDestination = requestedDestination,
            openNutritionLogging = openNutritionLogging,
            onNutritionLoggingOpened = onNutritionLoggingOpened,
            onActiveWorkoutOpened = onActiveWorkoutOpened,
            onWorkoutDetailOpened = onWorkoutDetailOpened,
            onBodyMetricsOpened = onBodyMetricsOpened,
            onFeedbackOpened = onFeedbackOpened,
            openActiveWorkout = openActiveWorkout,
            openWorkoutDetail = openWorkoutDetail,
            openBodyMetrics = openBodyMetrics,
            openFeedback = openFeedback,
            onSyncHealthConnect = requestHealthConnectHistory,
            subscriptionRequired = subscriptionRequired,
            onSubscriptionPurchaseCompleted = {
                // StoreKit dismisses iOS's mandatory paywall as soon as the
                // purchase has been verified by the backend, then refreshes the
                // app state. Do the same for Google Play: a slow or unrelated
                // sync failure must not leave a verified purchaser blocked.
                subscriptionRequired = false
                coroutineScope.launch {
                    context.getSharedPreferences("wildforce_account", 0).getString("token", null)?.let { token ->
                        subscriptionAccess(token).onSuccess { SubscriptionAccessStore.update(context, it) }
                    }
                    when (val result = WorkoutRemoteSync.synchronize(context, currentProfile, preferences)) {
                        is WorkoutRemoteSync.Outcome.Synchronized -> {
                            preferences.edit().putBoolean("remote_sync_pending", false).apply()
                            applyPulledPlanIfAvailable()
                        }
                        WorkoutRemoteSync.Outcome.SubscriptionRequired -> subscriptionRequired = true
                        is WorkoutRemoteSync.Outcome.Failed -> {
                            if (result.importedRemotePlan) applyPulledPlanIfAvailable()
                            result.importedProfile?.let { imported ->
                                persistOnboardingProfile(imported)
                                profile = imported
                            }
                        }
                        else -> Unit
                    }
                }
            },
            onAccountChanged = {
                WildforceNotificationScheduler.cancelAll(context)
                clearLocalAccountData(context)
                profile = null
            },
            onWorkoutCompleted = { id, duration, sets, volume ->
                val previouslyCompleted = preferences.getStringSet("completed_workouts", emptySet()).orEmpty()
                val isNewCompletion = id !in previouslyCompleted
                val belongsToPlan = restoredState.workouts.any { it.id == id }
                val completesPlan = belongsToPlan && restoredState.workouts.none { candidate ->
                    candidate.id != id && !candidate.status.isPlanFinalized
                }
                val completesMesocycle = completesPlan &&
                    restoredState.cycleLength > 1 &&
                    restoredState.positionInCycle >= restoredState.cycleLength
                val completed = previouslyCompleted + id
                val totalCompleted = preferences.getInt("total_completed_workouts", 0) + if (isNewCompletion) 1 else 0
                preferences.edit().putStringSet("completed_workouts", completed)
                    .putInt("total_completed_workouts", totalCompleted)
                    .putInt("last_workout_duration", duration).putInt("last_workout_sets", sets)
                    .putLong("last_workout_volume", java.lang.Double.doubleToRawLongBits(volume)).apply()
                WildforceNotificationScheduler.recordWorkoutCompleted(context)
                // Finishing a workout must not crash if an optional side
                // effect (history migration or widget provider) fails on any
                // Android version, manufacturer or launcher.
                val completedWorkout = restoredState.workouts.firstOrNull { it.id == id }
                    ?: CustomWorkoutStore.load(context).firstOrNull { it.id == id }
                runCatching { TrainingSessionHistoryStore.record(preferences, duration, sets, volume, completedWorkout?.focus) }
                runCatching { WorkoutQuickAccessWidget.refresh(context) }
                runCatching { AppQuickActions.refresh(context) }
                if (isNewCompletion) {
                    WorkoutRemoteSync.recordWorkoutCompletion(
                        context,
                        xpAward = workoutCompletionXpAward(completesPlan, completesMesocycle),
                    )
                }
                WorkoutRemoteSync.markResourcesDirty(context, "users", "workout-days", "planned-exercises", "exercise-results")
                coroutineScope.launch {
                    when (val result = WorkoutRemoteSync.synchronize(context, currentProfile, preferences)) {
                        is WorkoutRemoteSync.Outcome.Synchronized -> {
                            preferences.edit().putBoolean("remote_sync_pending", false).apply()
                            applyPulledPlanIfAvailable()
                        }
                        WorkoutRemoteSync.Outcome.SubscriptionRequired -> subscriptionRequired = true
                        is WorkoutRemoteSync.Outcome.Failed -> {
                            if (result.importedRemotePlan) applyPulledPlanIfAvailable()
                            result.importedProfile?.let { imported ->
                                persistOnboardingProfile(imported)
                                profile = imported
                            }
                        }
                        else -> Unit
                    }
                }
                if (currentProfile.isHealthConnectEnabled) {
                    val workoutTitle = completedWorkout?.title ?: "Entrenamiento"
                    coroutineScope.launch {
                        runCatching {
                            val metrics = WorkoutCompletionMetricsStore.load(context, id)
                            HealthConnectWeightSync.publishCompletedWorkout(
                                context = context,
                                workoutId = id,
                                title = workoutTitle,
                                focus = completedWorkout?.focus,
                                durationSeconds = duration,
                                activeCalories = metrics?.activeCaloriesBurned ?: 0.0,
                                completedAtMillis = metrics?.completedAtMillis,
                            )
                        }
                    }
                }
            },
            onResetOnboarding = {
                preferences.edit().clear().apply()
                profile = null
            },
            onProfileUpdated = { updated ->
                // Android initially stored the profile snapshot separately from
                // the plan. Plans created by older builds (and plans imported
                // from iOS) therefore have no snapshot to compare against,
                // which made profile edits silently keep a stale plan. Capture
                // the profile *before* this edit once, then the usual signature
                // comparison presents the same recreate-plan banner as iOS.
                if (workoutPlanStore.currentPlanJson() != null && !workoutPlanStore.hasProfileSignature()) {
                    workoutPlanStore.saveProfileSignature(currentProfile.workoutPlanProfileSignature())
                }
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
                    .putInt("birth_month", updated.birthMonth).putInt("birth_year", updated.birthYear).putInt("birth_day", updated.birthDay)
                    .putString("gender", updated.gender.storedValue).putString("metric_system", updated.metricSystem.storedValue)
                    .putInt("height_cm", updated.heightCm).putLong("weight_kg", java.lang.Double.doubleToRawLongBits(updated.weightKg))
                    .putBoolean("skips_warmups", updated.skipsWarmups)
                    .putBoolean("skips_cooldowns", updated.skipsCooldowns)
                    .putBoolean("skips_rest_periods", updated.skipsRestPeriods)
                    .putString("planner_notes", updated.workoutPlannerNotes)
                    .putStringSet("training_locations", encodeTrainingLocations(updated.trainingLocations))
                    .putString("app_language", updated.appLanguage)
                    .putString("gym_join_code", updated.gymJoinCode)
                    .putString("gym_join_name", updated.gymJoinName)
                    .apply { WorkoutRemoteSync.markResourcesDirty(context, "users", "training-preferences", "training-locations", "user-app-settings") }
                .apply()
                profile = updated
                scheduleRemoteSync(updated)
                if (updated.isHealthConnectEnabled) {
                    coroutineScope.launch {
                        runCatching { HealthConnectWeightSync.publishManualWeight(context, updated.weightKg) }
                    }
                }
            },
            onRegenerateProfile = { updated ->
                // The explicit profile sync below supersedes the debounced one.
                // Otherwise its older snapshot could be pushed after generation.
                scheduledRemoteSync?.cancel()
                scheduledRemoteSync = null
                generationError = null
                pendingGeneratedPlan = null
                val nextPlanRequested = preferences.getBoolean("generate_next_plan", false)
                preferences.edit().remove("generate_next_plan").apply()
                isGenerating = true
                coroutineScope.launch {
                    runCatching {
                        // Keep the profile context on the server ahead of generation,
                        // but do not let unrelated nutrition sync failures block the
                        // workout plan request.
                        val profileResources = setOf("users", "training-preferences", "training-locations", "user-app-settings")
                        WorkoutRemoteSync.markResourcesDirty(context, *profileResources.toTypedArray())
                        when (val sync = WorkoutRemoteSync.synchronize(context, updated, preferences, resourcesOnly = profileResources)) {
                            is WorkoutRemoteSync.Outcome.Synchronized -> Unit
                            WorkoutRemoteSync.Outcome.NoAccount -> error("Inicia sesión para sincronizar el perfil antes de generar el plan.")
                            WorkoutRemoteSync.Outcome.SubscriptionRequired -> error("Necesitas acceso AI para sincronizar el perfil.")
                            is WorkoutRemoteSync.Outcome.Failed -> error(sync.message ?: "No se pudo sincronizar el perfil antes de generar el plan.")
                        }
                        if (nextPlanRequested) {
                            WorkoutPlanGenerator.generateNext(updated, restoredState, context)
                        } else WorkoutPlanGenerator.generate(updated, context, recreate = true)
                    }
                        .onSuccess { (json, state) ->
                            pendingGeneratedPlan = json to state
                        }
                        .onFailure { generationError = it.message?.takeIf(String::isNotBlank) ?: "No se pudo regenerar el plan" }
                    isGenerating = false
                }
            },
            onWorkoutStateChanged = { generatedWorkoutState = it },
            onPlanWorkoutUpdated = { updatedState ->
                generatedWorkoutState = updatedState
                workoutPlanStore.saveCurrentPlan(WorkoutPlanGenerator.serialize(updatedState))
                WorkoutRemoteSync.markResourcesDirty(context, "workout-plans", "workout-days", "workout-blocks", "planned-exercises", "exercise-results")
                WorkoutQuickAccessWidget.refresh(context)
                AppQuickActions.refresh(context)
                scheduleRemoteSync(currentProfile)
            },
            isGeneratingProfilePlan = isGenerating,
            profileGenerationError = generationError,
            onRequestHealthConnect = requestHealthConnect,
            onRemoteSyncRequested = { scheduleRemoteSync(currentProfile) },
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
                    val confirmedState = state.confirmGeneratedPlan()
                    WorkoutRemoteSync.queuePreviousPlanArchive(preferences, baseState, confirmedState)
                    workoutPlanStore.replaceCurrentPlan(WorkoutPlanGenerator.serialize(confirmedState))
                    workoutPlanStore.saveProfileSignature(currentProfile.workoutPlanProfileSignature())
                    WorkoutQuickAccessWidget.refresh(context)
                    AppQuickActions.refresh(context)
                    preferences.edit().remove("completed_workouts").remove("skipped_workouts").apply()
                    WorkoutRemoteSync.markResourcesDirty(context, "workout-plans", "workout-days", "workout-blocks", "planned-exercises", "exercise-results")
                    generatedWorkoutState = confirmedState
                    pendingGeneratedPlan = null
                    scheduleRemoteSync(currentProfile)
                },
            )
        }
    }
}

/** Matches iOS's fixed 450 pt, non-dismissible workout-plan generation sheet. */
@Composable
private fun WorkoutPlanGenerationSheet() {
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            AnimatedVisibility(
                visible = entered,
                enter = fadeIn(tween(220)) + slideInVertically(tween(360)) { height -> height },
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth().height(450.dp),
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    color = WildforceThemeTokens.background,
                ) {
                    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier.padding(top = 10.dp, bottom = 12.dp)
                                .size(width = 34.dp, height = 4.dp)
                                .background(WildforceThemeTokens.textSecondary.copy(alpha = .28f), CircleShape),
                        )
                        Text(
                            androidx.compose.ui.res.stringResource(R.string.creating_next_plan),
                            color = WildforceThemeTokens.textPrimary,
                            fontWeight = FontWeight.Bold,
                        )
                        PlanGenerationAnimation(icon = "⌁", modifier = Modifier.width(320.dp).height(380.dp))
                    }
                }
            }
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
                Icon(Icons.Filled.CenterFocusStrong, null, modifier = Modifier.size(14.dp), tint = Color.White.copy(alpha = .84f))
                Text(workout.focus, style = androidx.compose.material.MaterialTheme.typography.caption, color = Color.White.copy(alpha = .84f), maxLines = 1)
            }
        }
    }
}

@Composable
private fun WildforceApp(
    profile: OnboardingProfile,
    workoutState: WorkoutHubState,
    requestedDestination: RootDestination? = null,
    openNutritionLogging: Boolean = false,
    onNutritionLoggingOpened: () -> Unit = {},
    onActiveWorkoutOpened: () -> Unit = {},
    onWorkoutDetailOpened: () -> Unit = {},
    onBodyMetricsOpened: () -> Unit = {},
    onFeedbackOpened: () -> Unit = {},
    openActiveWorkout: Boolean = false,
    openWorkoutDetail: Boolean = false,
    openBodyMetrics: Boolean = false,
    openFeedback: Boolean = false,
    onWorkoutCompleted: (String, Int, Int, Double) -> Unit,
    subscriptionRequired: Boolean = false,
    onSubscriptionPurchaseCompleted: () -> Unit = {},
    onAccountChanged: () -> Unit = {},
    onResetOnboarding: () -> Unit = {},
    onProfileUpdated: (OnboardingProfile) -> Unit = {},
    onRegenerateProfile: (OnboardingProfile) -> Unit = {},
    onWorkoutStateChanged: (WorkoutHubState) -> Unit = {},
    onPlanWorkoutUpdated: (WorkoutHubState) -> Unit = {},
    isGeneratingProfilePlan: Boolean = false,
    profileGenerationError: String? = null,
    onRequestHealthConnect: (((Boolean, Int?, Double?, Double?, Double?) -> Unit) -> Unit) = { _ -> },
    onSyncHealthConnect: (((Boolean, List<ImportedBodyMetric>) -> Unit) -> Unit) = { result -> result(false, emptyList()) },
    onRemoteSyncRequested: () -> Unit = {},
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
    var wearAvailable by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        WearRemoteLauncher.checkAvailability(appContext) { available -> wearAvailable = available }
    }
    val appPreferences = remember { appContext.getSharedPreferences("wildforce_profile", 0) }
    val profileDetailPreferences = remember { appContext.getSharedPreferences("wildforce_profile_details", 0) }
    var keepScreenOnDuringWorkout by remember { mutableStateOf(profileDetailPreferences.getBoolean("keep_screen_on", true)) }
    val workoutPlanStore = remember(appPreferences) { WorkoutPlanStateStore(appPreferences) }
    // A migrated preference can point to the private files directory of the
    // previous application id. Do not hand that non-existent path to Compose.
    var avatarPath by remember {
        mutableStateOf(appPreferences.getString("avatar_path", null)?.takeIf { File(it).isFile })
    }
    var avatarRevision by remember { mutableStateOf(0) }
    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching {
            val destination = File(appContext.filesDir, "profile-avatar.jpg")
            appContext.contentResolver.openInputStream(uri)?.use { input -> destination.writeBytes(input.readBytes()) }
            avatarPath = destination.absolutePath
            avatarRevision++
            appPreferences.edit().putString("avatar_path", avatarPath).apply()
            WorkoutRemoteSync.markResourcesDirty(appContext, "users")
            onRemoteSyncRequested()
        }
    }
    LaunchedEffect(workoutState.planName, workoutState.workouts) {
        WildforceNotificationScheduler.scheduleNextWorkout(appContext, workoutState)
        WildforceNotificationScheduler.scheduleNutritionReminder(appContext)
    }
    val trainingProgress = CompletionProgressStore.progress(appContext)
    val coroutineScope = rememberCoroutineScope()
    var selected by remember { mutableStateOf(requestedDestination ?: RootDestination.Workout) }
    var bodyProgressOpen by remember { mutableStateOf(false) }
    var customWorkoutEditorOpen by remember { mutableStateOf(false) }
    var bookingFlowOpen by remember { mutableStateOf(false) }
    var bookingGyms by remember { mutableStateOf<List<BookingGym>?>(null) }
    var showJoinGymDialog by remember { mutableStateOf(false) }
    var gymMembershipRevision by remember { mutableStateOf(0) }
    var isAiCoachOfferPresented by remember { mutableStateOf(false) }
    fun refreshSubscriptionAccess() {
        val token = appContext.getSharedPreferences("wildforce_account", 0).getString("token", null) ?: return
        coroutineScope.launch { subscriptionAccess(token).onSuccess { SubscriptionAccessStore.update(appContext, it) } }
    }
    suspend fun reloadBookingGyms() {
        runCatching { BookingApi(appContext).gyms() }.onSuccess { gyms ->
            bookingGyms = gyms
            appPreferences.edit().putBoolean("booking_gyms_available", gyms.isNotEmpty()).apply()
        }
    }
    LaunchedEffect(Unit) { reloadBookingGyms() }
    val hasAuthenticatedAccount = appContext.getSharedPreferences("wildforce_account", 0)
        .getString("token", null)
        ?.isNotBlank() == true
    val hasBookableGyms = hasAuthenticatedAccount &&
        (bookingGyms?.isNotEmpty() ?: appPreferences.getBoolean("booking_gyms_available", false))
    val cachedAccess = SubscriptionAccessStore.load(appContext)
    val isMemberMode = cachedAccess?.isMemberMode == true
    // The hero is visible through showsAiCoach (including the locked Premium
    // state); the actual generation/redo action requires the AI-coach feature.
    // Unknown access keeps the pre-server full-app behavior.
    val canGenerateNextPlan = cachedAccess?.features?.aiCoach ?: true
    val rootDestinations = remember(hasBookableGyms) {
        RootDestination.values().filter { it != RootDestination.Gym || hasBookableGyms }
    }
    LaunchedEffect(hasBookableGyms, requestedDestination) {
        if (!hasBookableGyms && selected == RootDestination.Gym) {
            selected = RootDestination.Workout
        } else if (hasBookableGyms && isMemberMode && requestedDestination == null && selected == RootDestination.Workout) {
            // iOS members land on their Gym tab as soon as the cached booking
            // gyms are available. Explicit app-entry routes still win.
            selected = RootDestination.Gym
        }
    }
    LaunchedEffect(requestedDestination) {
        requestedDestination?.let { selected = it }
    }
    LaunchedEffect(selected) {
        if (selected != RootDestination.Profile) bodyProgressOpen = false
    }
    var displayedWorkoutState by remember(workoutState) { mutableStateOf(workoutState) }
    var workoutDetail by remember { mutableStateOf<WorkoutDaySummary?>(null) }
    var activeWorkout by remember { mutableStateOf<WorkoutDaySummary?>(null) }
    var activeExerciseGuide by remember { mutableStateOf<ExerciseSummary?>(null) }
    fun markWorkoutStarted(source: WorkoutDaySummary): WorkoutDaySummary {
        val started = source.copy(
            status = WorkoutStatus.InProgress,
            startedAt = source.startedAt ?: Instant.now().toString(),
        )
        val updatedState = displayedWorkoutState.copy(
            workouts = displayedWorkoutState.workouts.map { existing ->
                if (existing.id == started.id) started else existing
            },
        )
        displayedWorkoutState = updatedState
        if (!started.id.startsWith("custom-")) {
            workoutPlanStore.saveCurrentPlan(WorkoutPlanGenerator.serialize(updatedState))
            onPlanWorkoutUpdated(updatedState)
        }
        // iOS reloads the home-screen workout widget as soon as a session
        // starts, so the active exercise and progress are visible immediately.
        runCatching { WorkoutQuickAccessWidget.refresh(appContext) }
        runCatching { AppQuickActions.refresh(appContext) }
        return started
    }
    LaunchedEffect(activeWorkout?.id) {
        AndroidFullFocusModeManager.reconcile(appContext, activeWorkout != null)
    }
    var showExitAppDialog by remember { mutableStateOf(false) }
    SideEffect {
        activity?.let { host ->
            // The active workout draws a darkened exercise photo behind the
            // status bar, matching iOS's full-screen NavigationStack scene.
            // Keep light status-bar icons there; regular app surfaces follow
            // the selected light/dark theme.
            WindowCompat.getInsetsController(host.window, view).isAppearanceLightStatusBars =
                !darkTheme && activeWorkout == null
        }
    }
    // iOS's member mode keeps the Gym surface available but gates the rest of
    // the root tabs behind the mandatory subscription screen. A successful
    // sync is not an escape hatch here: booking-only access is a valid server
    // mode, not a synchronization failure.
    if (subscriptionRequired || (isMemberMode && selected != RootDestination.Gym)) {
        val accountPreferences = remember(appContext) { appContext.getSharedPreferences("wildforce_account", 0) }
        SubscriptionPaywall(
            accountId = accountPreferences.getString("id", null),
            mandatory = true,
            onJoinGymRequested = { showJoinGymDialog = true },
            onPurchaseCompleted = onSubscriptionPurchaseCompleted,
        )
    }
    SideEffect {
        activity?.window?.let { window ->
            if (keepScreenOnDuringWorkout && activeWorkout != null) window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            else window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
    BackHandler(enabled = activeWorkout == null && activeExerciseGuide == null) {
        when {
            workoutDetail != null -> workoutDetail = null
            selected != RootDestination.Workout -> selected = RootDestination.Workout
            else -> showExitAppDialog = true
        }
    }
    BackHandler(enabled = activeExerciseGuide != null) { activeExerciseGuide = null }
    activeWorkout?.let { workout ->
        Box(Modifier.fillMaxSize()) {
            ActiveWorkoutScreen(
            initialWorkout = workout,
            gender = profile.gender.storedValue,
            userBmr = workoutBmr(profile),
            userAgeYears = profileAgeYears(profile),
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
            totalPlanExercises = displayedWorkoutState.workouts.sumOf { it.exercises.size },
            mesocycleIndex = displayedWorkoutState.mesocycleIndex,
            cycleLength = displayedWorkoutState.cycleLength,
            canGenerateNextPlan = canGenerateNextPlan,
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
                if (!updated.id.startsWith("custom-")) {
                    workoutPlanStore.saveCurrentPlan(WorkoutPlanGenerator.serialize(updatedState))
                    WorkoutRemoteSync.markResourcesDirty(appContext, "workout-days", "workout-blocks", "planned-exercises", "exercise-results")
                }
                activeWorkout = updated
                // ActiveWorkoutStore reloads iOS's widget after each exercise
                // advance; keep Android's home-screen widget in step with the
                // persisted active exercise as well.
                runCatching { WorkoutQuickAccessWidget.refresh(appContext) }
                runCatching { AppQuickActions.refresh(appContext) }
            },
            onShowExerciseGuide = { activeExerciseGuide = it },
            onExit = { activeWorkout = null },
            onFinish = { duration, sets, volume, streak, completedOn, skippedExerciseIndices ->
                WorkoutRemoteSync.recordSkippedExercises(appContext, workout.id, skippedExerciseIndices)
                val completionMetrics = WorkoutCompletionMetricsStore.load(appContext, workout.id)
                val completionTimestamp = completionMetrics?.completedAtMillis?.let { Instant.ofEpochMilli(it).toString() }
                    ?: Instant.now().toString()
                val completedDates = appPreferences
                    .getStringSet("completed_workout_local_dates", emptySet())
                    .orEmpty()
                    .filterNot { it.substringBeforeLast('|', missingDelimiterValue = "") == workout.id }
                    .toMutableSet()
                    .apply { add("${workout.id}|$completedOn") }
                appPreferences.edit().putStringSet("completed_workout_local_dates", completedDates).apply()
                val completedWorkouts = displayedWorkoutState.workouts.map {
                    if (it.id == workout.id) it.copy(
                        status = WorkoutStatus.Completed,
                        completedAt = completionTimestamp,
                        didCountTowardStreak = true,
                        activeDurationSeconds = completionMetrics?.activeDurationSeconds ?: duration,
                        activeCaloriesBurned = completionMetrics?.activeCaloriesBurned,
                        totalVolumeKg = completionMetrics?.totalVolumeKg ?: volume,
                    ) else it
                }
                val completedState = displayedWorkoutState.copy(
                    user = displayedWorkoutState.user.copy(currentStreak = streak),
                    completedDays = if (displayedWorkoutState.workouts.any { it.id == workout.id }) displayedWorkoutState.completedDays + completedOn.dayOfWeek else displayedWorkoutState.completedDays,
                    workouts = completedWorkouts,
                ).withPlanCompletionStatus()
                displayedWorkoutState = completedState
                if (workout.id.startsWith("custom-")) {
                    CustomWorkoutStore.save(
                        appContext,
                        workout.copy(
                            status = WorkoutStatus.Completed,
                            completedAt = completionTimestamp,
                            didCountTowardStreak = true,
                            activeDurationSeconds = completionMetrics?.activeDurationSeconds ?: duration,
                            activeCaloriesBurned = completionMetrics?.activeCaloriesBurned,
                            totalVolumeKg = completionMetrics?.totalVolumeKg ?: volume,
                        ),
                    )
                } else {
                    workoutPlanStore.saveCurrentPlan(WorkoutPlanGenerator.serialize(completedState))
                }
                onWorkoutCompleted(workout.id, duration, sets, volume)
                WildforceNotificationScheduler.scheduleNextWorkout(appContext, completedState)
                activeWorkout = null
                workoutDetail = null
            },
            )
            activeExerciseGuide?.let { exercise ->
                Box(Modifier.fillMaxSize().zIndex(1f)) {
                    ExerciseGuideScreen(exercise, profile.gender.storedValue) {
                        activeExerciseGuide = null
                    }
                }
            }
        }
        return
    }
    workoutDetail?.let { workout ->
        val cachedAccess = SubscriptionAccessStore.load(appContext)
        val showsAiCoach = cachedAccess?.let { it.features.aiCoach || !it.hasCoach } ?: true
        Box(Modifier.fillMaxSize()) {
        WorkoutDetailScreen(workout, profile.gender.storedValue, useImperial = profile.metricSystem == MetricSystem.Imperial, showWearStartOption = wearAvailable, appLanguage = profile.appLanguage, mesocyclePhase = displayedWorkoutState.mesocyclePhase, onBack = { workoutDetail = null }, onStart = { startOnWear ->
            WildforceNotificationScheduler.recordWorkoutStarted(appContext)
            if (startOnWear) WearRemoteLauncher.startWorkout(appContext)
            activeWorkout = markWorkoutStarted(workout)
        }, onSkip = {
            val skipped = appPreferences.getStringSet("skipped_workouts", emptySet()).orEmpty() + workout.id
            appPreferences.edit().putStringSet("skipped_workouts", skipped).apply()
            WorkoutRemoteSync.markResourcesDirty(appContext, "workout-days", "planned-exercises", "exercise-results")
            val skippedWorkouts = displayedWorkoutState.workouts.map { if (it.id == workout.id) it.copy(status = WorkoutStatus.Skipped) else it }
            val skippedState = displayedWorkoutState.copy(
                workouts = skippedWorkouts,
            ).withPlanCompletionStatus()
            displayedWorkoutState = skippedState
            // Keep the in-memory source used by regeneration aligned with the
            // state currently shown on screen. Without this, recreating a plan
            // in the same app session could use the pre-skip week snapshot.
            onWorkoutStateChanged(skippedState)
            if (!workout.id.startsWith("custom-")) workoutPlanStore.saveCurrentPlan(WorkoutPlanGenerator.serialize(skippedState))
            runCatching { WorkoutQuickAccessWidget.refresh(appContext) }
            runCatching { AppQuickActions.refresh(appContext) }
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
            WorkoutRemoteSync.markResourcesDirty(appContext, "workout-days", "planned-exercises", "exercise-results")
            val unskippedState = displayedWorkoutState.copy(
                workouts = displayedWorkoutState.workouts.map { if (it.id == workout.id) it.copy(status = WorkoutStatus.Planned) else it },
            )
            displayedWorkoutState = unskippedState
            onWorkoutStateChanged(unskippedState)
            if (!workout.id.startsWith("custom-")) workoutPlanStore.saveCurrentPlan(WorkoutPlanGenerator.serialize(unskippedState))
            runCatching { WorkoutQuickAccessWidget.refresh(appContext) }
            runCatching { AppQuickActions.refresh(appContext) }
            WildforceNotificationScheduler.scheduleNextWorkout(appContext, unskippedState)
        }, onWorkoutUpdated = { updated ->
            if (updated.id.startsWith("custom-")) {
                CustomWorkoutStore.save(appContext, updated)
            }
            val updatedState = displayedWorkoutState.copy(
                workouts = displayedWorkoutState.workouts.map { existing -> if (existing.id == updated.id) updated else existing },
            )
            displayedWorkoutState = updatedState
            onWorkoutStateChanged(updatedState)
            if (!updated.id.startsWith("custom-")) workoutPlanStore.saveCurrentPlan(WorkoutPlanGenerator.serialize(updatedState))
            workoutDetail = updated
            runCatching { WorkoutQuickAccessWidget.refresh(appContext) }
            runCatching { AppQuickActions.refresh(appContext) }
            onPlanWorkoutUpdated(updatedState)
        }, adaptEquipmentPresets = profile.effectiveTrainingLocations().map { location -> location.name to location.equipment.joinToString(", ") { it.title } }, adaptEquipmentPresetIds = appContext.getSharedPreferences("wildforce_remote_sync", 0).getString("training_location_ids", "[]")?.let { raw -> org.json.JSONArray(raw) }?.let { ids -> profile.effectiveTrainingLocations().indices.map { ids.optString(it) } }.orEmpty(), adaptAiGenerator = if (showsAiCoach) ({ request ->
            WorkoutPlanGenerator.adaptWorkout(profile, workout, request, appContext)
        }) else null, canUseAiCoach = canGenerateNextPlan, onShowAiCoachOffer = { isAiCoachOfferPresented = true })
            RootBottomNavigation(
                destinations = rootDestinations,
                selected = selected,
                onSelected = {
                    workoutDetail = null
                    selected = it
                },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
        return
    }
    val archivedWorkoutStates = remember(displayedWorkoutState.mesocycleNumber, displayedWorkoutState.planName) {
        workoutPlanStore.archivedPlans().mapNotNull { archived ->
            runCatching { WorkoutPlanGenerator.parse(archived.rawJson, profile.name, profile.goal.title, profile.bodyCompositionPhase?.storedValue) }.getOrNull()
        }
    }
    // Keep the persisted iOS `.inProgress` state and Android's local session
    // fallback both discoverable by the compact Resume accessory. iOS also
    // searches independent/custom workouts, which Android stores separately
    // from the current generated plan.
    val resumableWorkout = (displayedWorkoutState.workouts + CustomWorkoutStore.load(appContext))
        .distinctBy { it.id }
        .firstOrNull { workout ->
            workout.status == WorkoutStatus.InProgress ||
                (workout.status == WorkoutStatus.Planned && WorkoutSessionStore.hasActiveSession(appContext, workout.id))
        }
    LaunchedEffect(openActiveWorkout, openWorkoutDetail, resumableWorkout?.id, displayedWorkoutState.workouts) {
        if (openActiveWorkout || openWorkoutDetail && resumableWorkout != null) {
            resumableWorkout?.let { activeWorkout = markWorkoutStarted(it) }
            if (openActiveWorkout) onActiveWorkoutOpened() else onWorkoutDetailOpened()
        } else if (openWorkoutDetail) {
            val today = java.time.DayOfWeek.from(java.time.LocalDate.now())
            displayedWorkoutState.workouts.firstOrNull { workout ->
                (workout.status == WorkoutStatus.Planned || workout.status == WorkoutStatus.InProgress) && workout.scheduledDay == today
            }?.let { workoutDetail = it }
            onWorkoutDetailOpened()
        }
    }
    // Preserve the 7126e99 layout contract: Scaffold reserves the exact space
    // used by Resume and the dock, while each screen continues beneath them.
    Scaffold(
        modifier = Modifier.liquidGlassBackground(),
        backgroundColor = Color.Transparent,
        bottomBar = {
            if (!bodyProgressOpen && !customWorkoutEditorOpen && !bookingFlowOpen) {
                Column {
                    resumableWorkout?.let { workout ->
                        ActiveWorkoutAccessory(
                            workout = workout,
                            exerciseIndex = WorkoutSessionStore.activeExerciseIndex(appContext, workout.id),
                            onResume = { activeWorkout = markWorkoutStarted(workout) },
                        )
                    }
                    RootBottomNavigation(
                        destinations = rootDestinations,
                        selected = selected,
                        onSelected = { selected = it },
                    )
                }
            }
        },
    ) { padding ->
        val overlayPadding = PaddingValues(
            start = padding.calculateStartPadding(LayoutDirection.Ltr),
            top = padding.calculateTopPadding(),
            end = padding.calculateEndPadding(LayoutDirection.Ltr),
            bottom = 0.dp,
        )
        if (selected == RootDestination.Workout) {
            val cachedAccess = SubscriptionAccessStore.load(appContext)
            val showsAiCoach = cachedAccess?.let { it.features.aiCoach || !it.hasCoach } ?: true
            WorkoutHubScreen(
                contentPadding = overlayPadding,
                state = displayedWorkoutState,
                planHistory = archivedWorkoutStates,
                onWorkoutSelected = { workoutDetail = it },
                gender = profile.gender.storedValue,
                defaultCustomEquipment = profile.availableEquipment.joinToString(", ") { it.title },
                customEquipmentPresets = profile.effectiveTrainingLocations().map { location -> location.name to location.equipment.joinToString(", ") { it.title } },
                allowAutomaticCreation = showsAiCoach,
                canRegeneratePlan = canGenerateNextPlan,
                canGenerateNextPlan = canGenerateNextPlan,
                onGenerateNextPlan = {
                    appPreferences.edit().putBoolean("generate_next_plan", true).apply()
                    onRegenerateProfile(profile)
                },
                onShowAiCoachOffer = { isAiCoachOfferPresented = true },
                generationError = profileGenerationError,
                onRetryGeneration = { onRegenerateProfile(profile) },
                requiresPlanRegeneration = workoutPlanStore.requiresRegeneration(profile.workoutPlanProfileSignature()),
                onRecreatePlan = { onRegenerateProfile(profile) },
                avatarPath = avatarPath,
                avatarRevision = avatarRevision,
                appLanguage = profile.appLanguage,
                openFeedback = openFeedback,
                onFeedbackOpened = onFeedbackOpened,
                customAiGenerator = { request -> WorkoutPlanGenerator.generateCustom(profile, request, appContext) },
                onCustomEditorVisibilityChanged = { customWorkoutEditorOpen = it },
            )
            if (isAiCoachOfferPresented) {
                SubscriptionPaywall(
                    accountId = appContext.getSharedPreferences("wildforce_account", 0).getString("id", null),
                    mandatory = false,
                    tiers = listOf(SubscriptionTier.Premium),
                    onDismiss = { isAiCoachOfferPresented = false },
                    onPurchaseCompleted = {
                        isAiCoachOfferPresented = false
                        onSubscriptionPurchaseCompleted()
                    },
                )
            }
        } else if (selected == RootDestination.Nutrition) {
            NutritionScreen(
                contentPadding = overlayPadding,
                profile = profile,
                openLogging = openNutritionLogging,
                onLoggingOpened = onNutritionLoggingOpened,
                onRequestHealthConnect = { result ->
                    onRequestHealthConnect { granted, heightCm, weightKg, leanBodyMassKg, bodyFatPercentage ->
                        if (granted) {
                            BodyMetricsStore.recordImported(
                                context = appContext,
                                profile = profile,
                                heightCm = heightCm?.toDouble(),
                                weightKg = weightKg,
                                leanBodyMassKg = leanBodyMassKg,
                                bodyFatPercentage = bodyFatPercentage,
                            )
                            onProfileUpdated(profile.copy(isHealthConnectEnabled = true))
                        }
                        result(granted, heightCm, weightKg, leanBodyMassKg, bodyFatPercentage)
                    }
                },
            )
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
        } else if (selected == RootDestination.Gym) {
            BookingScreen(overlayPadding, onBookingVisibilityChanged = { bookingFlowOpen = it })
        } else if (selected == RootDestination.Profile) {
            ProfileScreen(
                initial = profile,
                openBodyMetrics = openBodyMetrics,
                onBodyMetricsOpened = onBodyMetricsOpened,
                onBodyMetricSaved = { kind, value ->
                    if (profile.isHealthConnectEnabled) {
                        coroutineScope.launch {
                            runCatching {
                                if (kind == "Weight") {
                                    HealthConnectWeightSync.publishManualWeight(appContext, value)
                                } else {
                                    HealthConnectWeightSync.publishBodyMetric(appContext, kind, value)
                                }
                            }
                        }
                    }
                },
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
                gymMembershipRevision = gymMembershipRevision,
                onSyncHealthConnect = onSyncHealthConnect,
                onSave = onProfileUpdated,
                onRegenerate = onRegenerateProfile,
                onChangeAvatar = { avatarPicker.launch("image/*") },
                onRemoteSyncRequested = onRemoteSyncRequested,
                onBodyProgressVisibilityChanged = { bodyProgressOpen = it },
                onThemeChanged = onThemeChanged,
                onJoinGymRequested = { showJoinGymDialog = true },
                onKeepScreenOnChanged = { keepScreenOnDuringWorkout = it },
                onNotificationPreferencesChanged = {
                    WildforceNotificationScheduler.scheduleNextWorkout(appContext, workoutState)
                    WildforceNotificationScheduler.scheduleNutritionReminder(appContext)
                },
                onAccountChanged = {
                    onAccountChanged()
                    workoutDetail = null
                    activeWorkout = null
                    selected = RootDestination.Workout
                },
                onGenerateFuturePreview = { localSessionId, angle ->
                    WorkoutRemoteSync.synchronize(appContext, profile, appPreferences)
                    FutureBodyPreviewGenerator.generate(appContext, WorkoutRemoteSync.remoteBodyProgressSessionId(appContext, localSessionId), angle)
                },
                onLoadFuturePreview = { localSessionId, angle ->
                    FutureBodyPreviewGenerator.load(appContext, WorkoutRemoteSync.remoteBodyProgressSessionId(appContext, localSessionId), angle)
                },
                onAnalyzeBodyComposition = { localSessionId ->
                    WorkoutRemoteSync.synchronize(appContext, profile, appPreferences)
                    BodyCompositionAnalysisGenerator.analyze(appContext, WorkoutRemoteSync.remoteBodyProgressSessionId(appContext, localSessionId))
                },
            )
        } else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(androidx.compose.ui.res.stringResource(R.string.coming_soon, androidx.compose.ui.res.stringResource(selected.labelRes)), color = WildforceThemeTokens.textSecondary)
        }
    }
    if (isGeneratingProfilePlan) {
        WorkoutPlanGenerationSheet()
    }
    if (showJoinGymDialog) {
        AuthenticatedJoinGymDialog(
            onDismiss = { showJoinGymDialog = false },
            onJoined = {
                showJoinGymDialog = false
                gymMembershipRevision++
                refreshSubscriptionAccess()
                onRemoteSyncRequested()
                coroutineScope.launch { reloadBookingGyms() }
            },
        )
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
private fun profileAgeYears(profile: OnboardingProfile): Int {
    val today = java.time.LocalDate.now()
    val year = profile.birthYear.coerceIn(1900, today.year)
    val month = profile.birthMonth.coerceIn(1, 12)
    val day = profile.birthDay.coerceIn(1, java.time.YearMonth.of(year, month).lengthOfMonth())
    return java.time.Period.between(java.time.LocalDate.of(year, month, day), today).years.coerceIn(0, 120)
}

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
    onResume: () -> Unit,
) {
    val currentExercise = workout.exercises.getOrNull(exerciseIndex ?: 0)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .height(49.dp),
        shape = RoundedCornerShape(20.dp),
        // Keep the Resume accessory on the exact translucent material used by
        // the historical 7126e99 root navigation surface.
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
                .semantics {
                    role = Role.Button
                    contentDescription = buildString {
                        append("Reanudar ")
                        append(workout.title)
                        currentExercise?.let {
                            append(", ")
                            append(exerciseDisplayName(it.imageKey, it.name))
                        }
                    }
                }
                .clickable(onClick = onResume)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val primaryMuscleDrawable = ExerciseVisualCatalog.primaryMuscleDrawable(currentExercise?.imageKey)
            if (primaryMuscleDrawable != null) {
                // Match iOS: the accessory uses the alphabetically first primary
                // target muscle, rather than the exercise's training photo.
                Image(
                    painter = painterResource(primaryMuscleDrawable),
                    contentDescription = currentExercise?.let { exerciseDisplayName(it.imageKey, it.name) },
                    modifier = Modifier.size(28.dp),
                )
            } else {
                Icon(
                    imageVector = workoutAccessoryFocusIcon(workout.focus),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = WildforceThemeTokens.accent,
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
                    text = currentExercise?.let { exerciseDisplayName(it.imageKey, it.name) } ?: workout.focus,
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
                    // iOS uses AccentColor at 14% opacity here (not the
                    // workout's orange emphasis), so this adapts from dark
                    // gray in light mode to white in dark mode as it does there.
                    .background(WildforceThemeTokens.accent.copy(alpha = 0.14f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                style = androidx.compose.material.MaterialTheme.typography.subtitle2.copy(fontSize = 15.sp),
                fontWeight = FontWeight.SemiBold,
                color = WildforceThemeTokens.textPrimary,
            )
        }
    }
}

/** Matches iOS's focus icon fallback when the current exercise has no muscle icon. */
private fun workoutAccessoryFocusIcon(focus: String): ImageVector = when (focus.lowercase(Locale.ROOT)) {
    "fullbody", "full body", "cuerpo completo", "upperbody", "upper body", "tren superior", "lowerbody", "lower body", "tren inferior" -> Icons.Filled.AccessibilityNew
    "empuje", "push" -> Icons.Filled.ArrowCircleUp
    "tirón", "tiron", "pull" -> Icons.Filled.ArrowCircleDown
    "piernas", "legs" -> Icons.Filled.DirectionsWalk
    "core" -> Icons.Filled.CenterFocusStrong
    "movilidad", "mobility" -> Icons.Filled.SelfImprovement
    "recuperación", "recuperacion", "recovery" -> Icons.Filled.Hotel
    "cardio" -> Icons.Filled.Favorite
    else -> Icons.Filled.AccessibilityNew
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
        location.displayAddress.orEmpty().replace(trainingLocationSeparator, " "),
        location.latitude?.toString().orEmpty(),
        location.longitude?.toString().orEmpty(),
        location.photoPath.orEmpty().replace(trainingLocationSeparator, " "),
    ).joinToString(trainingLocationSeparator)
}

private fun decodeTrainingLocations(encoded: Set<String>): List<TrainingLocationProfile> = encoded.mapNotNull { value ->
    val fields = value.split(trainingLocationSeparator, limit = 7)
    val name = fields.getOrNull(0)?.trim().orEmpty()
    if (name.isBlank()) null else TrainingLocationProfile(
        name = name,
        isDefault = fields.getOrNull(1) == "1",
        equipment = Equipment.fromStoredValues(fields.getOrNull(2)?.split(',')?.filter(String::isNotBlank)?.toSet()).ifEmpty { setOf(Equipment.Bodyweight) },
        displayAddress = fields.getOrNull(3)?.trim()?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) },
        latitude = fields.getOrNull(4)?.replace(',', '.')?.toDoubleOrNull()?.takeIf { it in -90.0..90.0 },
        longitude = fields.getOrNull(5)?.replace(',', '.')?.toDoubleOrNull()?.takeIf { it in -180.0..180.0 },
        photoPath = fields.getOrNull(6)?.trim()?.takeIf(String::isNotBlank),
    )
}.let { locations ->
    if (locations.isEmpty() || locations.any { it.isDefault }) locations else locations.mapIndexed { index, location -> location.copy(isDefault = index == 0) }
}
