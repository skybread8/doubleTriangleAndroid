package io.codepassion.doubletriangle.feature.onboarding

import android.Manifest
import android.content.res.Configuration
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.widget.NumberPicker

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Icon
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextFieldDefaults
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Chair
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.viewinterop.AndroidView
import java.time.Year
import java.time.Month
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.log10
import kotlinx.coroutines.delay
import io.codepassion.doubletriangle.core.designsystem.AntonFontFamily
import io.codepassion.doubletriangle.core.designsystem.Exo2FontFamily
import io.codepassion.doubletriangle.core.designsystem.WildforceThemeTokens
import io.codepassion.doubletriangle.core.designsystem.liquidGlass
import io.codepassion.doubletriangle.core.designsystem.liquidGlassBackground
import io.codepassion.doubletriangle.core.model.RemoteSyncChangeTracker
import io.codepassion.doubletriangle.feature.workout.WildforceWatchLocalBridge
import io.codepassion.doubletriangle.feature.workout.RemoteTrainingImage
import io.codepassion.doubletriangle.feature.workout.publicAvatarUrl

// Keep the visible Android surface aligned with iOS FeatureFlag.watchMotionDetection,
// which is currently disabled in the iOS baseline. The Wear implementation remains
// available for the platform-parity work once that flag is enabled on iOS.
private const val WATCH_MOTION_DETECTION_ENABLED = false

/** Editable fitness profile. These are the same inputs consumed by the iOS planner. */
@Composable
fun ProfileScreen(
    initial: OnboardingProfile,
    openBodyMetrics: Boolean = false,
    onBodyMetricsOpened: () -> Unit = {},
    contentPadding: androidx.compose.foundation.layout.PaddingValues = androidx.compose.foundation.layout.PaddingValues(),
    isRegenerating: Boolean = false,
    generationError: String? = null,
    currentStreak: Int = 0,
    completedWorkouts: Int = 0,
    longestStreak: Int = 0,
    experienceXp: Int = 0,
    experienceLevel: Int = 1,
    experienceProgress: Float = 0f,
    avatarPath: String? = null,
    avatarRevision: Int = 0,
    gymMembershipRevision: Int = 0,
    onRequestHealthConnect: (((Boolean, Int?, Double?, Double?, Double?) -> Unit) -> Unit) = { _ -> },
    onSyncHealthConnect: (((Boolean, List<ImportedBodyMetric>) -> Unit) -> Unit) = { result -> result(false, emptyList()) },
    onBodyMetricSaved: (String, Double) -> Unit = { _, _ -> },
    onSave: (OnboardingProfile) -> Unit,
    onRegenerate: (OnboardingProfile) -> Unit = {},
    onChangeAvatar: () -> Unit = {},
    onRemoteSyncRequested: () -> Unit = {},
    onBodyProgressVisibilityChanged: (Boolean) -> Unit = {},
    onThemeChanged: () -> Unit = {},
    onJoinGymRequested: () -> Unit = {},
    onKeepScreenOnChanged: (Boolean) -> Unit = {},
    onNotificationPreferencesChanged: () -> Unit = {},
    onAccountChanged: () -> Unit = {},
    onGenerateFuturePreview: suspend (String, String) -> android.graphics.Bitmap = { _, _ -> error("La vista a futuro no está configurada") },
    onLoadFuturePreview: suspend (String, String) -> android.graphics.Bitmap? = { _, _ -> null },
    onAnalyzeBodyComposition: suspend (String) -> BodyCompositionAnalysis = { error("El análisis no está configurado") },
) {
    var draft by remember(initial) { mutableStateOf(initial) }
    // Opening Profile is a read operation. Do not turn the freshly imported
    // iOS profile into an Android edit before the user changes a field.
    var profilePersistencePrimed by remember(initial) { mutableStateOf(false) }
    var heightInput by remember(initial) { mutableStateOf(displayHeight(initial.heightCm, initial.metricSystem)) }
    var weightInput by remember(initial) { mutableStateOf(displayWeight(initial.weightKg, initial.metricSystem)) }
    var durationInput by remember(initial) { mutableStateOf(initial.preferredWorkoutDurationMinutes.toString()) }
    var birthDayInput by remember(initial) { mutableStateOf(initial.birthDay.toString()) }
    var birthYearInput by remember(initial) { mutableStateOf(initial.birthYear.toString()) }
    var nameEditorVisible by remember { mutableStateOf(false) }
    var nameInput by remember(initial) { mutableStateOf(initial.name) }
    var picker by remember { mutableStateOf<Picker?>(null) }
    var showsTrainingLocations by remember { mutableStateOf(false) }
    var healthConnectMessage by remember { mutableStateOf<String?>(null) }
    var entered by remember { mutableStateOf(false) }
    val profileContext = LocalContext.current
    var amazfitWatchEnabled by remember { mutableStateOf(WildforceWatchLocalBridge.enabled(profileContext)) }
    val notificationPreferences = remember(profileContext) { profileContext.getSharedPreferences("wildforce_notification_settings", 0) }
    var notificationsEnabled by remember { mutableStateOf(notificationPreferences.getBoolean("enabled", true)) }
    var restAlertsEnabled by remember { mutableStateOf(notificationPreferences.getBoolean("rest_alerts", true)) }
    var reminderNotificationsEnabled by remember { mutableStateOf(notificationPreferences.getBoolean("reminders", true)) }
    var pendingNotificationPreference by remember { mutableStateOf<NotificationPreference?>(null) }
    fun enableNotificationPreference(preference: NotificationPreference) {
        when (preference) {
            NotificationPreference.Reminders -> reminderNotificationsEnabled = true
            NotificationPreference.Training -> notificationsEnabled = true
        }
        notificationPreferences.edit()
            .putBoolean(if (preference == NotificationPreference.Reminders) "reminders" else "enabled", true)
            .apply()
        RemoteSyncChangeTracker.mark(profileContext, "user-app-settings")
        onRemoteSyncRequested()
        onNotificationPreferencesChanged()
    }
    val requestNotificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val preference = pendingNotificationPreference
        pendingNotificationPreference = null
        if (granted && preference != null) enableNotificationPreference(preference)
    }
    fun requestOrEnableNotification(preference: NotificationPreference) {
        if (
            android.os.Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(profileContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            pendingNotificationPreference = preference
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            enableNotificationPreference(preference)
        }
    }
    val detailPreferences = remember(profileContext) { profileContext.getSharedPreferences("wildforce_profile_details", 0) }
    var appLanguage by remember { mutableStateOf(detailPreferences.getString("language", "Español") ?: "Español") }
    var appTheme by remember { mutableStateOf(detailPreferences.getString("theme", "Sistema") ?: "Sistema") }
    var keepScreenOn by remember { mutableStateOf(detailPreferences.getBoolean("keep_screen_on", true)) }
    var wearAutoTrackingEnabled by remember { mutableStateOf(detailPreferences.getBoolean("wear_auto_tracking_enabled", false)) }
    var fullFocusEnabled by remember { mutableStateOf(detailPreferences.getBoolean("full_focus", false)) }
    var progressPhotoLockEnabled by remember { mutableStateOf(detailPreferences.getBoolean("progress_photo_lock", false)) }
    var pendingProgressPhotoLockValue by remember { mutableStateOf<Boolean?>(null) }
    var progressPhotoLockError by remember { mutableStateOf<String?>(null) }
    var nutritionProfile by remember { mutableStateOf(ProfileDetailPreferencesStore.loadNutrition(profileContext)) }
    var bodyComposition by remember { mutableStateOf(ProfileDetailPreferencesStore.loadBodyComposition(profileContext)) }
    var nutritionPicker by remember { mutableStateOf<NutritionPicker?>(null) }
    var nutritionTextEditor by remember { mutableStateOf<NutritionTextField?>(null) }
    var settingsPicker by remember { mutableStateOf<SettingsPicker?>(null) }
    var selectedSection by remember { mutableStateOf<ProfileSection?>(null) }
    var showsPlannerNotesEditor by remember { mutableStateOf(false) }
    var plannerNotesInput by remember(initial) { mutableStateOf(initial.workoutPlannerNotes.orEmpty()) }
    var showHeightPicker by remember { mutableStateOf(false) }
    var selectedHeight by remember { mutableStateOf(0) }
    var sectionEntered by remember { mutableStateOf(false) }
    var accountRevision by remember { mutableStateOf(0) }
    val currentAccount = remember(accountRevision) { currentWildforceAccount(profileContext) }
    var coaches by remember(accountRevision) { mutableStateOf(emptyList<WildforceCoach>()) }
    var gymMemberships by remember(accountRevision) { mutableStateOf(emptyList<WildforceGymMembership>()) }
    var showsBodyProgress by remember { mutableStateOf(false) }
    var showsWearMotionOnboarding by remember { mutableStateOf(false) }
    LaunchedEffect(showsBodyProgress) { onBodyProgressVisibilityChanged(showsBodyProgress) }
    var showsBodyMetrics by remember { mutableStateOf(false) }
    val authenticateProgressPhotoSetting = rememberDeviceAuthenticator(
        onSuccess = {
            pendingProgressPhotoLockValue?.let { enabled ->
                progressPhotoLockEnabled = enabled
                detailPreferences.edit().putBoolean("progress_photo_lock", enabled).apply()
            }
            pendingProgressPhotoLockValue = null
        },
        onFailure = {
            pendingProgressPhotoLockValue = null
            progressPhotoLockError = it
        },
    )
    val bodyMetrics = remember(profileContext, initial) { BodyMetricsStore.load(profileContext) }
    if (selectedSection == ProfileSection.Body && showsBodyMetrics) {
        BodyMetricsScreen(
            profile = validatedProfile(draft, heightInput, weightInput, durationInput, birthYearInput),
            composition = bodyComposition,
            history = bodyMetrics,
            onClose = { showsBodyMetrics = false },
            onRequestHealthConnect = onRequestHealthConnect,
            onSyncHealthConnect = onSyncHealthConnect,
            onSave = { updatedProfile, updatedComposition, leanBodyMass, bodyFat ->
                draft = updatedProfile
                heightInput = displayHeight(updatedProfile.heightCm, updatedProfile.metricSystem)
                weightInput = displayWeight(updatedProfile.weightKg, updatedProfile.metricSystem)
                bodyComposition = updatedComposition
                ProfileDetailPreferencesStore.saveBodyComposition(profileContext, updatedComposition)
                BodyMetricsStore.record(profileContext, updatedProfile, updatedComposition, leanBodyMass, bodyFat)
                onSave(updatedProfile)
            },
            onMetricSaved = onBodyMetricSaved,
        )
        return
    }
    if (selectedSection == ProfileSection.Body && showsBodyProgress) {
        ProgressPhotoAccessGate(locked = progressPhotoLockEnabled, onClose = { showsBodyProgress = false }) {
            BodyProgressScreen(
                profile = validatedProfile(draft, heightInput, weightInput, durationInput, birthYearInput),
                generateFuturePreview = onGenerateFuturePreview,
                loadFuturePreview = onLoadFuturePreview,
                analyzeComposition = onAnalyzeBodyComposition,
                onClose = { showsBodyProgress = false; onRemoteSyncRequested() },
            )
        }
        return
    }
    fun saveProfile(value: OnboardingProfile) {
        BodyMetricsStore.record(profileContext, value)
        onSave(value)
    }
    // Nutrition can be edited from this profile tab or from its own hub. Both
    // must enter the same remote-sync path so neither device silently wins.
    fun saveNutritionProfile(value: NutritionProfilePreferences) {
        ProfileDetailPreferencesStore.saveNutrition(profileContext, value)
        RemoteSyncChangeTracker.mark(
            profileContext,
            "nutrition-profiles", "nutrition-plans", "nutrition-days", "nutrition-meals",
            "nutrition-log-media", "nutrition-log-entries", "nutrition-log-items",
        )
        onRemoteSyncRequested()
    }
    LaunchedEffect(Unit) { entered = true }
    LaunchedEffect(openBodyMetrics) {
        if (openBodyMetrics) {
            selectedSection = ProfileSection.Body
            showsBodyMetrics = true
            onBodyMetricsOpened()
        }
    }
    LaunchedEffect(currentAccount, accountRevision, gymMembershipRevision) {
        if (currentAccount == null) {
            coaches = emptyList()
            gymMemberships = emptyList()
        } else {
            coaches = currentWildforceCoaches(profileContext)
            currentWildforceGymMemberships(profileContext)?.let { gymMemberships = it }
        }
    }
    LaunchedEffect(selectedSection) {
        sectionEntered = false
        if (selectedSection != null) sectionEntered = true
    }
    fun closePlannerNotesEditor() {
        draft = draft.copy(workoutPlannerNotes = plannerNotesInput.trim())
        showsPlannerNotesEditor = false
    }
    BackHandler(enabled = showsPlannerNotesEditor) {
        closePlannerNotesEditor()
    }
    LaunchedEffect(draft, heightInput, weightInput, durationInput, birthDayInput, birthYearInput) {
        if (profilePersistencePrimed) {
            saveProfile(validatedProfile(draft, heightInput, weightInput, durationInput, birthYearInput, birthDayInput))
        } else {
            profilePersistencePrimed = true
        }
    }
    LazyColumn(
        // Resume and root navigation are bottom overlays. Keep this background
        // full-height; the list's trailing padding keeps the final row clear
        // when it is scrolled beneath the controls.
        modifier = Modifier
            .fillMaxSize()
            .liquidGlassBackground()
            .padding(horizontal = 16.dp),
        // The menu stays over the canvas; reserve room inside the scroll so the
        // final profile setting can be brought above it (and the resume bar).
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            top = 16.dp,
            bottom = contentPadding.calculateBottomPadding().coerceAtLeast(160.dp),
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (selectedSection != null) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(48.dp).clip(CircleShape).background(WildforceThemeTokens.surfaceElevated).clickable {
                        if (showsPlannerNotesEditor) closePlannerNotesEditor() else selectedSection = null
                    }, contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = WildforceThemeTokens.textPrimary, modifier = Modifier.size(22.dp))
                    }
                    Text(if (showsPlannerNotesEditor) "Notas para el planificador" else selectedSection?.title.orEmpty(), fontFamily = Exo2FontFamily, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textPrimary)
                }
            }
        }
        if (selectedSection == null) { item { ProfileEntrance(entered, 0) { ProfileHero(draft, currentStreak, completedWorkouts, longestStreak, avatarPath, avatarRevision, onChangeAvatar, entered, onEditName = { nameInput = draft.name; nameEditorVisible = true }) } } }
        if (selectedSection == null) { item { ProfileEntrance(entered, 35) { ProfileExperience(experienceXp, experienceLevel, experienceProgress) } } }
        // iOS exposes the account creation CTA on the root profile when the
        // user is signed out, while keeping the full Account destination in
        // the profile list. This makes the first-run path discoverable without
        // changing the existing account flow.
        if (selectedSection == null && currentAccount == null) { item {
            ProfileEntrance(entered, 300) {
                AccountSection(
                    profile = draft,
                    destination = AccountProfileDestination.Account,
                    onProfileSynchronized = { remoteProfile -> draft = remoteProfile; saveProfile(remoteProfile) },
                    onAccountChanged = { accountRevision++; onAccountChanged() },
                )
            }
        } }
        if (selectedSection == null) { item { ProfileEntrance(entered, 75) { ProfileCategoryTree(draft, nutritionProfile, appLanguage, accountRevision) { selectedSection = it } } } }
        if (selectedSection == null && currentAccount != null) { item {
            ProfileEntrance(entered, 350) {
                AccountLogoutAction(onAccountChanged = { accountRevision++; onAccountChanged() })
            }
        } }
        // iOS renders the authenticated gym section even when the user has no
        // memberships yet, because the section itself contains the Join a gym
        // action. Keeping it unconditional for authenticated users preserves
        // that entry point instead of hiding it until after the first join.
        if (selectedSection == null && currentAccount != null) { item {
            ProfileEntrance(entered, 400) {
                Section(if (gymMemberships.size > 1) "Tus gimnasios" else "Tu gimnasio") {
                    gymMemberships.forEach { membership ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(Icons.Filled.Apartment, contentDescription = null, modifier = Modifier.size(22.dp), tint = WildforceThemeTokens.textPrimary)
                            Column(Modifier.weight(1f)) {
                                Text(membership.teamName, color = WildforceThemeTokens.textPrimary)
                                membership.gymName?.let { Text(it, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary) }
                            }
                            if (membership.status.equals("suspended", ignoreCase = true)) {
                                Text("Suspendida", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
                            }
                        }
                    }
                    ChoiceButton(
                        "Unirse a un gimnasio",
                        "Introducir código o escanear QR",
                        icon = Icons.Filled.Apartment,
                        onClick = onJoinGymRequested,
                    )
                }
            }
        } }
        if (selectedSection == null && coaches.isNotEmpty()) { item {
            ProfileEntrance(entered, 450) {
                Section("Tus entrenadores") {
                    coaches.forEach { coach ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            RemoteTrainingImage(
                                url = publicAvatarUrl(coach.id),
                                contentDescription = coach.name,
                                modifier = Modifier.size(44.dp).clip(CircleShape),
                                showUnavailableLabel = false,
                                unavailableContent = {
                                    Box(
                                        Modifier.fillMaxSize().background(WildforceThemeTokens.textSecondary.copy(alpha = .12f)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(Icons.Filled.Person, contentDescription = null, tint = WildforceThemeTokens.textSecondary)
                                    }
                                },
                            )
                            Text(coach.name, Modifier.padding(start = 12.dp), color = WildforceThemeTokens.textPrimary)
                        }
                    }
                }
            }
        } }
        // `achievementsSection` is intentionally disabled in the canonical iOS
        // profile view.  Do not render a placeholder rail here: it changes the
        // hierarchy and pushes the four profile destinations below the fold.
        if (selectedSection == ProfileSection.Body) { item { ProfileEntrance(sectionEntered, 105) { Section("Cuerpo") {
                ChoiceButton("Sexo", draft.gender.title, icon = Icons.Filled.Person) { picker = Picker.Gender }
                ChoiceButton("Fecha de nacimiento", "$birthDayInput de ${monthName(draft.birthMonth)} $birthYearInput", icon = Icons.Filled.CalendarMonth) {
                    val validProfile = validatedProfile(draft, heightInput, weightInput, durationInput, birthYearInput, birthDayInput)
                    android.app.DatePickerDialog(
                        profileContext,
                        { _, year, month, day ->
                            draft = draft.copy(birthMonth = month + 1)
                            birthDayInput = day.toString()
                            birthYearInput = year.toString()
                        },
                        validProfile.birthYear,
                        validProfile.birthMonth - 1,
                        validProfile.birthDay,
                    ).show()
                }
                ChoiceButton(
                    "Altura",
                    formatHeightChoice(heightInput, draft.metricSystem),
                    icon = Icons.Filled.Straighten,
                ) {
                    selectedHeight = heightInput.toIntOrNull() ?: displayHeight(draft.heightCm, draft.metricSystem).toInt()
                    showHeightPicker = true
                }
                // The profile weight is the canonical current value, just like iOS.
                // The body-metrics collection is history and can legitimately lag
                // behind when the profile is edited from another entry point.
                ChoiceButton("Métricas corporales", displayWeight(draft.weightKg, draft.metricSystem).let { "$it ${if (draft.metricSystem == MetricSystem.Imperial) "lb" else "kg"}" }, icon = Icons.Filled.Scale) { showsBodyMetrics = true }
                ChoiceButton("Fotos de progreso corporal", BodyProgressStore.load(profileContext).size.let { if (it == 0) "Sin fotos todavía" else "$it check-ins" }, icon = Icons.Filled.PhotoCamera) { showsBodyProgress = !showsBodyProgress }
            }
        } } }
        if (selectedSection == ProfileSection.Body) { item {
            // Detail sections are rendered directly once selected. Keeping them out of
            // AnimatedVisibility avoids a LazyColumn re-measure race when switching
            // from the category tree to an editor with focused text fields.
            ProfileBodyMetrics(
                profile = validatedProfile(draft, heightInput, weightInput, durationInput, birthYearInput),
                measurements = bodyComposition,
            )
        } }
        if (selectedSection == ProfileSection.Fitness && showsPlannerNotesEditor) { item { ProfileEntrance(sectionEntered, 105) {
            Section("Notas para el planificador") {
                OutlinedTextField(
                    value = plannerNotesInput,
                    onValueChange = { plannerNotesInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Añade contexto útil para tus planes de entrenamiento") },
                    minLines = 5,
                    maxLines = 10,
                )
                Text(
                    "Las notas deben ser específicas para tus entrenamientos. Se usarán para adaptar el plan y no pueden sustituir ajustes como el equipamiento o las restricciones.",
                    style = MaterialTheme.typography.caption,
                    color = WildforceThemeTokens.textSecondary,
                )
            }
        } } }
        if (selectedSection == ProfileSection.Fitness && !showsPlannerNotesEditor) { item { ProfileEntrance(sectionEntered, 105) { Section("Entrenamiento") {
                ChoiceButton("Objetivo", draft.goal.title, icon = profileGoalIcon(draft.goal)) { picker = Picker.Goal }
                ChoiceButton("Estilo de vida", draft.lifestyle.title, draft.lifestyle.description, profileLifestyleIcon(draft.lifestyle)) { picker = Picker.Lifestyle }
                ChoiceButton("Nivel de entrenamiento", draft.trainingLevel.title, draft.trainingLevel.description, Icons.Filled.FitnessCenter) { picker = Picker.Level }
                if (draft.goal.supportsBodyComposition && draft.trainingLevel.supportsBodyComposition) ChoiceButton("Intención de composición corporal", draft.bodyCompositionPhase?.title ?: "Automático", icon = Icons.Filled.Scale) { picker = Picker.Body }
            } } } }
        if (selectedSection == ProfileSection.Fitness && !showsPlannerNotesEditor) { item { ProfileEntrance(sectionEntered, 155) { Section("Configuración de entrenamiento") {
                ChoiceButton("Configuración", draft.trainingSplitPreference.title, icon = Icons.Filled.GridView) { picker = Picker.Split }
                Text("Días de entrenamiento", fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
                draft.workoutDays.toList().let { days ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        WorkoutWeekday.entries.forEach { day ->
                            val selected = day in draft.workoutDays
                            Text(
                                day.localizedInitial(appLanguage),
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        if (selected) WildforceThemeTokens.textPrimary
                                        else WildforceThemeTokens.textSecondary.copy(alpha = .12f),
                                        RoundedCornerShape(12.dp),
                                    )
                                    .clickable {
                                        draft = draft.copy(
                                            workoutDays = if (selected && draft.workoutDays.size > 1) {
                                                draft.workoutDays - day
                                            } else {
                                                draft.workoutDays + day
                                            },
                                        )
                                    }
                                    .semantics {
                                        contentDescription = day.title
                                        this.selected = selected
                                    }
                                    .padding(10.dp),
                                color = if (selected) WildforceThemeTokens.backgroundSecondary else WildforceThemeTokens.textPrimary,
                            )
                        }
                    }
                }
                val durationValue = durationInput.toIntOrNull()?.coerceIn(15, 180) ?: draft.preferredWorkoutDurationMinutes.coerceIn(15, 180)
                Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Duración preferida", Modifier.weight(1f), color = WildforceThemeTokens.textPrimary)
                    IconButton(
                        onClick = { durationInput = (durationValue - 5).coerceAtLeast(15).toString() },
                        enabled = durationValue > 15,
                        modifier = Modifier.semantics { contentDescription = "Reducir duración preferida" },
                    ) { Icon(Icons.Filled.Remove, contentDescription = null, tint = WildforceThemeTokens.textPrimary) }
                    Text("$durationValue min", Modifier.width(64.dp), color = WildforceThemeTokens.textSecondary, textAlign = TextAlign.Center)
                    IconButton(
                        onClick = { durationInput = (durationValue + 5).coerceAtMost(180).toString() },
                        enabled = durationValue < 180,
                        modifier = Modifier.semantics { contentDescription = "Aumentar duración preferida" },
                    ) { Icon(Icons.Filled.Add, contentDescription = null, tint = WildforceThemeTokens.textPrimary) }
                }
                draft.workoutDays.sortedBy { it.ordinal }.forEach { day ->
                    if (draft.trainingSplitPreference == TrainingSplitPreference.Custom) ChoiceButton(day.title, draft.customWorkoutFocuses[day]?.title ?: "Seleccionar foco") { picker = Picker.Focus(day) }
                }
                TogglePreference("Omitir calentamientos", "Oculta los bloques de activación y ajusta la duración objetivo.", draft.skipsWarmups, Icons.Filled.FastForward) { draft = draft.copy(skipsWarmups = !draft.skipsWarmups) }
                TogglePreference("Omitir enfriamientos", "Oculta los bloques finales y mantiene el plan dentro de la duración objetivo.", draft.skipsCooldowns, Icons.Filled.DirectionsWalk) { draft = draft.copy(skipsCooldowns = !draft.skipsCooldowns) }
                TogglePreference("Omitir periodos de descanso", "Salta directamente a la siguiente serie o ejercicio durante el entrenamiento.", draft.skipsRestPeriods, Icons.Filled.SkipNext) { draft = draft.copy(skipsRestPeriods = !draft.skipsRestPeriods) }
                val plannerNotes = draft.workoutPlannerNotes.takeUnless { it.equals("null", ignoreCase = true) }
                ChoiceButton(
                    "Notas para el planificador",
                    plannerNotes ?: "Ninguna",
                    description = plannerNotes?.takeIf { it.isNotBlank() },
                    icon = Icons.Filled.Notes,
                ) {
                    plannerNotesInput = plannerNotes.orEmpty()
                    showsPlannerNotesEditor = true
                }
            } } } }
        if (selectedSection == ProfileSection.Fitness && !showsPlannerNotesEditor) { item { ProfileEntrance(sectionEntered, 205) { Section("Entorno") {
                ChoiceButton("Tipo de gimnasio", draft.gymType.title, icon = profileGymTypeIcon(draft.gymType)) { picker = Picker.Gym }
                ChoiceButton("Ubicaciones de entrenamiento", draft.effectiveTrainingLocations().joinToString(" · ") { if (it.isDefault) "${it.name} (principal)" else it.name }, icon = Icons.Filled.Home) { showsTrainingLocations = true }
                ChoiceButton("Equipamiento disponible", "${draft.availableEquipment.size} seleccionado(s)", icon = Icons.Filled.FitnessCenter) { picker = Picker.Equipment }
                ChoiceButton("Restricciones o molestias", draft.movementRestrictions.takeIf { it.isNotEmpty() }?.size?.let { "$it seleccionado(s)" } ?: "Ninguna", icon = Icons.Filled.SelfImprovement) { picker = Picker.Restrictions }
            } } } }
        if (selectedSection == ProfileSection.Nutrition) { item { ProfileEntrance(sectionEntered, 235) {
                if (!nutritionProfile.isConfigured) Section("Perfil nutricional") {
                    Text("Configuración nutricional necesaria", style = MaterialTheme.typography.h6, color = WildforceThemeTokens.textPrimary)
                    Text("Crea tu perfil nutricional para guardar preferencias alimentarias, restricciones y valores predeterminados de planificación en un solo lugar.", style = MaterialTheme.typography.body2, color = WildforceThemeTokens.textSecondary)
                    Button(
                        onClick = { nutritionProfile = nutritionProfile.copy(isConfigured = true); saveNutritionProfile(nutritionProfile) },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary, contentColor = WildforceThemeTokens.backgroundSecondary),
                        shape = RoundedCornerShape(14.dp),
                    ) { Text("INICIAR CONFIGURACIÓN NUTRICIONAL") }
                } else Section("Planificación") {
                    ChoiceButton("Estilo alimentario", nutritionProfile.dietaryStyle, icon = Icons.Filled.Restaurant) { nutritionPicker = NutritionPicker.DietaryStyle }
                    ChoiceButton("Comidas al día", "${nutritionProfile.mealsPerDay} comidas", icon = Icons.Filled.Restaurant) { nutritionPicker = NutritionPicker.MealsPerDay }
                    ChoiceButton("Ventana de alimentación", nutritionProfile.eatingWindowLabel(), icon = Icons.Filled.Restaurant) { nutritionPicker = NutritionPicker.EatingWindow }
                    TogglePreference("Sugerencias de comidas", "Muestra ideas sencillas junto a calorías, macros y recomendaciones para el entrenamiento.", nutritionProfile.wantsMealSuggestions, Icons.Filled.Restaurant) {
                        nutritionProfile = nutritionProfile.copy(wantsMealSuggestions = !nutritionProfile.wantsMealSuggestions)
                        saveNutritionProfile(nutritionProfile)
                    }
                }
            }
        } }
        if (selectedSection == ProfileSection.Nutrition && nutritionProfile.isConfigured) { item { ProfileEntrance(sectionEntered, 255) { Section("Preferencias") {
                ChoiceButton("Esfuerzo al cocinar", nutritionProfile.cookingEffort, icon = Icons.Filled.LocalFireDepartment) { nutritionPicker = NutritionPicker.CookingEffort }
                ChoiceButton("Sensibilidad al presupuesto", nutritionProfile.budgetSensitivity, icon = Icons.Filled.Settings) { nutritionPicker = NutritionPicker.BudgetSensitivity }
                ChoiceButton("Fuentes de proteína", nutritionProfile.preferredProteinSources.listSummary(), icon = Icons.Filled.Restaurant) { nutritionTextEditor = NutritionTextField.ProteinSources }
                ChoiceButton("Alimentos que no te gustan", nutritionProfile.dislikes.listSummary(), icon = Icons.Filled.Restaurant) { nutritionTextEditor = NutritionTextField.Dislikes }
            } } } }
        if (selectedSection == ProfileSection.Nutrition && nutritionProfile.isConfigured) { item { ProfileEntrance(sectionEntered, 275) { Section("Restricciones") {
                ChoiceButton("Alimentos excluidos", nutritionProfile.excludedFoods.listSummary(), icon = Icons.Filled.Restaurant) { nutritionTextEditor = NutritionTextField.ExcludedFoods }
                ChoiceButton("Alergias o intolerancias", nutritionProfile.allergiesAndIntolerances.listSummary(), icon = Icons.Filled.Restaurant) { nutritionTextEditor = NutritionTextField.Allergies }
            } } } }
        if (selectedSection == ProfileSection.Nutrition && nutritionProfile.isConfigured) { item { ProfileEntrance(sectionEntered, 295) { Section("Notas") {
                ChoiceButton("Notas", nutritionProfile.notes.takeUnless { it.equals("null", ignoreCase = true) }?.ifBlank { "Ninguna" } ?: "Ninguna", icon = Icons.Filled.Settings) { nutritionTextEditor = NutritionTextField.Notes }
            } } } }
        if (selectedSection == ProfileSection.Settings) { item { ProfileEntrance(sectionEntered, 245) { Section("Preferencias") {
                ChoiceButton("Idioma", appLanguage, icon = Icons.Filled.Settings) { settingsPicker = SettingsPicker.Language }
                ChoiceButton("Sistema métrico", draft.metricSystem.title, icon = Icons.Filled.Settings) { picker = Picker.Metric }
                ChoiceButton("Tema", appTheme, icon = Icons.Filled.Settings) { settingsPicker = SettingsPicker.Theme }
            } } } }
        if (selectedSection == ProfileSection.Settings) { item { ProfileEntrance(sectionEntered, 265) { Section("Entorno") {
                ChoiceButton("Gimnasio", "Unirse o gestionar tus gimnasios", icon = Icons.Filled.Apartment, onClick = onJoinGymRequested)
                ChoiceButton("Preguntas frecuentes", "Respuestas sobre entrenamiento, nutrición, Android y tu cuenta.", icon = Icons.Filled.Help) { selectedSection = ProfileSection.Faq }
                TogglePreference("Health Connect", "Guarda entrenamientos y sincroniza medidas corporales con Health Connect.", draft.isHealthConnectEnabled, Icons.Filled.Favorite) {
                    if (draft.isHealthConnectEnabled) {
                        draft = draft.copy(isHealthConnectEnabled = false)
                        saveProfile(draft)
                        healthConnectMessage = "Health Connect se ha desactivado para Wildforce."
                    } else onRequestHealthConnect { granted, importedHeight, importedWeight, importedLeanMass, importedBodyFat ->
                        if (granted) {
                            importedHeight?.let { heightInput = displayHeight(it, draft.metricSystem) }
                            importedWeight?.let { weightInput = displayWeight(it, draft.metricSystem) }
                            draft = draft.copy(isHealthConnectEnabled = true, heightCm = importedHeight ?: draft.heightCm, weightKg = importedWeight ?: draft.weightKg)
                            BodyMetricsStore.record(
                                profileContext,
                                draft,
                                leanBodyMassKg = importedLeanMass,
                                bodyFatPercentage = importedBodyFat,
                            )
                            saveProfile(draft)
                            healthConnectMessage = if (importedHeight != null || importedWeight != null) "Medidas importadas desde Health Connect." else "Health Connect está conectado."
                        } else healthConnectMessage = "No se concedió el acceso a Health Connect."
                    }
                }
                healthConnectMessage?.let { Text(it, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary) }
                TogglePreference("Recordatorios", "Recibe recordatorios de próximos entrenamientos y comidas pendientes de registrar.", reminderNotificationsEnabled, Icons.Filled.Settings) {
                    if (reminderNotificationsEnabled) {
                        reminderNotificationsEnabled = false
                        notificationPreferences.edit().putBoolean("reminders", false).apply()
                        onNotificationPreferencesChanged()
                    } else {
                        requestOrEnableNotification(NotificationPreference.Reminders)
                    }
                }
                TogglePreference("Mantener pantalla encendida durante el entrenamiento", "Evita que la pantalla se bloquee mientras hay un entrenamiento activo abierto.", keepScreenOn, Icons.Filled.Settings) {
                    keepScreenOn = !keepScreenOn
                    detailPreferences.edit().putBoolean("keep_screen_on", keepScreenOn).apply()
                    RemoteSyncChangeTracker.mark(profileContext, "user-app-settings")
                    onRemoteSyncRequested()
                    onKeepScreenOnChanged(keepScreenOn)
                }
                TogglePreference("Modo concentración total", "Reduce las distracciones durante los entrenamientos mediante los ajustes de concentración de Android.", fullFocusEnabled, Icons.Filled.Settings) {
                    fullFocusEnabled = !fullFocusEnabled
                    detailPreferences.edit().putBoolean("full_focus", fullFocusEnabled).apply()
                    RemoteSyncChangeTracker.mark(profileContext, "user-app-settings")
                    onRemoteSyncRequested()
                    if (fullFocusEnabled) {
                        val notificationManager = profileContext.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                        val settingsAction = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M && !notificationManager.isNotificationPolicyAccessGranted) {
                            android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS
                        } else {
                            "android.settings.ZEN_MODE_SETTINGS"
                        }
                        runCatching { profileContext.startActivity(android.content.Intent(settingsAction)) }
                    }
                }
                if (fullFocusEnabled) {
                    ChoiceButton(
                        "Configurar categorías bloqueadas",
                        "Gestiona las aplicaciones y categorías que Android silencia durante el entrenamiento",
                        icon = Icons.Filled.Settings,
                    ) {
                        runCatching { profileContext.startActivity(android.content.Intent("android.settings.ZEN_MODE_SETTINGS")) }
                    }
                }
                if (android.os.Build.VERSION.SDK_INT >= 36) {
                    val notificationManager = profileContext.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                    val promotedEnabled = runCatching { notificationManager.canPostPromotedNotifications() }.getOrDefault(false)
                    val liveUpdateDescription = "Muestra la sesión como tarjeta/chip cuando Android 16 y el fabricante lo permitan."
                    ChoiceButton("Live Update de entrenamiento", if (promotedEnabled) "Activada" else "Activar en Ajustes", liveUpdateDescription) {
                        val promotedIntent = android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_PROMOTION_SETTINGS).apply {
                            putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, profileContext.packageName)
                        }
                        val fallbackIntent = android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, profileContext.packageName)
                        }
                        runCatching { profileContext.startActivity(promotedIntent) }.onFailure { runCatching { profileContext.startActivity(fallbackIntent) } }
                    }
                }
                TogglePreference("Notificaciones de entrenamiento", "Muestra la sesión activa en el panel y la pantalla bloqueada.", notificationsEnabled, Icons.Filled.Settings) {
                    if (notificationsEnabled) {
                        notificationsEnabled = false
                        notificationPreferences.edit().putBoolean("enabled", false).apply()
                        RemoteSyncChangeTracker.mark(profileContext, "user-app-settings")
                        onRemoteSyncRequested()
                        onNotificationPreferencesChanged()
                    } else {
                        requestOrEnableNotification(NotificationPreference.Training)
                    }
                }
                TogglePreference("Reloj Amazfit", "Sincroniza el entrenamiento activo con Wildforce en tu Amazfit. Solo se activa durante el workout.", amazfitWatchEnabled, Icons.Filled.Settings) {
                    amazfitWatchEnabled = !amazfitWatchEnabled
                    WildforceWatchLocalBridge.setEnabled(profileContext, amazfitWatchEnabled)
                    detailPreferences.edit().putBoolean("amazfit_watch_enabled", amazfitWatchEnabled).apply()
                    RemoteSyncChangeTracker.mark(profileContext, "user-app-settings")
                    onRemoteSyncRequested()
                }
                if (WATCH_MOTION_DETECTION_ENABLED) {
                    TogglePreference("Seguimiento automático Wear OS", "Cuenta repeticiones y muestra insights de movimiento en ejercicios compatibles.", wearAutoTrackingEnabled, Icons.Filled.Settings) {
                        wearAutoTrackingEnabled = !wearAutoTrackingEnabled
                        detailPreferences.edit().putBoolean("wear_auto_tracking_enabled", wearAutoTrackingEnabled).apply()
                        if (wearAutoTrackingEnabled) showsWearMotionOnboarding = true
                    }
                }
                TogglePreference("Avisos al comenzar el descanso", "Emite un aviso cuando empieza el temporizador de descanso.", restAlertsEnabled, Icons.Filled.Settings) {
                    restAlertsEnabled = !restAlertsEnabled
                    notificationPreferences.edit().putBoolean("rest_alerts", restAlertsEnabled).apply()
                }
            } } } }
        if (selectedSection == ProfileSection.Settings) { item { ProfileEntrance(sectionEntered, 285) { Section("Privacidad") {
                TogglePreference("Bloquear fotos de progreso", "Requiere la autenticación del dispositivo para abrir las fotos de progreso corporal.", progressPhotoLockEnabled, Icons.Filled.Settings) {
                    val enable = !progressPhotoLockEnabled
                    pendingProgressPhotoLockValue = enable
                    progressPhotoLockError = null
                    authenticateProgressPhotoSetting(
                        if (enable) "Confirma tu identidad para proteger tus fotos de progreso."
                        else "Confirma tu identidad para desactivar la protección de tus fotos de progreso.",
                    )
                }
        } } } }
        if (selectedSection == ProfileSection.Faq) { item { FrequentlyAskedQuestions() } }
        if (selectedSection == ProfileSection.Subscription) { item {
            AccountSection(
                profile = draft,
                destination = AccountProfileDestination.Subscription,
                onProfileSynchronized = { remoteProfile -> draft = remoteProfile; saveProfile(remoteProfile) },
                onAccountChanged = { accountRevision++; onAccountChanged() },
            )
        } }
        if (selectedSection == ProfileSection.Account) { item {
            AccountSection(
                profile = draft,
                destination = AccountProfileDestination.Account,
                onProfileSynchronized = { remoteProfile -> draft = remoteProfile; saveProfile(remoteProfile) },
                onAccountChanged = { accountRevision++; onAccountChanged() },
            )
        } }
    }
    picker?.let { current ->
        when (current) {
            Picker.Equipment -> EquipmentPickerDialog(draft.availableEquipment, { selected ->
                val locations = draft.effectiveTrainingLocations().map { location ->
                    if (location.isDefault) location.copy(equipment = selected) else location
                }
                draft = draft.copy(availableEquipment = selected, trainingLocations = locations)
                saveProfile(draft)
            }) { picker = null }
            Picker.Restrictions -> RestrictionsPickerDialog(draft.movementRestrictions, { selected -> draft = draft.copy(movementRestrictions = selected); saveProfile(draft) }) { picker = null }
            else -> PickerDialog(current, draft) { updated ->
                if (current == Picker.Metric && updated.metricSystem != draft.metricSystem) {
                    val normalized = validatedProfile(draft, heightInput, weightInput, durationInput, birthYearInput)
                    draft = normalized.copy(metricSystem = updated.metricSystem)
                    heightInput = displayHeight(normalized.heightCm, updated.metricSystem)
                    weightInput = displayWeight(normalized.weightKg, updated.metricSystem)
                } else draft = updated
                saveProfile(validatedProfile(draft, heightInput, weightInput, durationInput, birthYearInput))
                picker = null
            }
        }
    }
    if (showHeightPicker) {
        val minimum = if (draft.metricSystem == MetricSystem.Imperial) 47 else 120
        val maximum = if (draft.metricSystem == MetricSystem.Imperial) 91 else 230
        androidx.compose.material.AlertDialog(
            onDismissRequest = { showHeightPicker = false },
            title = { Text("Altura", fontFamily = AntonFontFamily) },
            text = {
                AndroidView(
                    factory = { viewContext ->
                        NumberPicker(viewContext).apply {
                            minValue = minimum
                            maxValue = maximum
                            displayedValues = (minimum..maximum).map { formatHeightChoice(it.toString(), draft.metricSystem) }.toTypedArray()
                            wrapSelectorWheel = false
                            value = selectedHeight.coerceIn(minimum, maximum)
                        }
                    },
                    update = { pickerView ->
                        pickerView.minValue = minimum
                        pickerView.maxValue = maximum
                        pickerView.displayedValues = (minimum..maximum).map { formatHeightChoice(it.toString(), draft.metricSystem) }.toTypedArray()
                        if (pickerView.value != selectedHeight) pickerView.value = selectedHeight.coerceIn(minimum, maximum)
                        pickerView.setOnValueChangedListener { _, _, newValue -> selectedHeight = newValue }
                    },
                    modifier = Modifier.fillMaxWidth().height(180.dp),
                )
            },
            confirmButton = {
                TextButton(onClick = { heightInput = selectedHeight.toString(); showHeightPicker = false }) { Text("GUARDAR") }
            },
            dismissButton = { TextButton(onClick = { showHeightPicker = false }) { Text("CANCELAR") } },
        )
    }
    nutritionPicker?.let { current ->
        ProfileOptionPickerDialog(current.title, current.options, onDismiss = { nutritionPicker = null }) { selected ->
            nutritionProfile = current.apply(nutritionProfile, selected)
            saveNutritionProfile(nutritionProfile)
            nutritionPicker = null
        }
    }
    nutritionTextEditor?.let { field ->
        NutritionTextEditorDialog(field, nutritionProfile, onDismiss = { nutritionTextEditor = null }) { value ->
            nutritionProfile = field.apply(nutritionProfile, value)
            saveNutritionProfile(nutritionProfile)
            nutritionTextEditor = null
        }
    }
    settingsPicker?.let { current ->
        ProfileOptionPickerDialog(current.title, current.options, onDismiss = { settingsPicker = null }) { selected ->
            when (current) {
                SettingsPicker.Language -> {
                    appLanguage = selected
                    detailPreferences.edit().putString("language", selected).apply()
                    draft = draft.copy(appLanguage = selected)
                    saveProfile(draft)
                }
                SettingsPicker.Theme -> {
                    appTheme = selected
                    detailPreferences.edit().putString("theme", selected).apply()
                    onThemeChanged()
                }
            }
            settingsPicker = null
        }
    }
    if (showsWearMotionOnboarding) {
        WearMotionOnboardingDialog(onDismiss = { showsWearMotionOnboarding = false })
    }
    if (showsTrainingLocations) {
        TrainingLocationsDialog(
            initial = draft.effectiveTrainingLocations(),
            onSave = { locations ->
                val defaultLocation = locations.firstOrNull { it.isDefault } ?: locations.first()
                draft = draft.copy(trainingLocations = locations, availableEquipment = defaultLocation.equipment)
                saveProfile(draft)
                showsTrainingLocations = false
            },
            onDismiss = { showsTrainingLocations = false },
        )
    }
    if (isRegenerating) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.58f)).clickable { }, contentAlignment = Alignment.Center) {
            Column(Modifier.padding(28.dp).liquidGlass(RoundedCornerShape(24.dp), emphasized = true).padding(26.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                CircularProgressIndicator(color = WildforceThemeTokens.accent)
                Text("CREANDO TU PLAN", fontFamily = AntonFontFamily, style = MaterialTheme.typography.h5, color = WildforceThemeTokens.textPrimary)
                Text("La IA está organizando tus sesiones y ajustándolas a tu perfil.", style = MaterialTheme.typography.body2, color = WildforceThemeTokens.textSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
    progressPhotoLockError?.let { message ->
        androidx.compose.material.AlertDialog(
            onDismissRequest = { progressPhotoLockError = null },
            title = { Text("No se pudo cambiar la protección") },
            text = { Text(message) },
            confirmButton = { Button(onClick = { progressPhotoLockError = null }) { Text("Entendido") } },
        )
    }
    if (nameEditorVisible) {
        androidx.compose.material.AlertDialog(
            onDismissRequest = { nameEditorVisible = false },
            title = { Text("Editar nombre", fontFamily = Exo2FontFamily) },
            text = {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it.take(80) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Nombre") },
                )
            },
            confirmButton = {
                Button(onClick = {
                    val value = nameInput.trim()
                    if (value.isNotEmpty()) {
                        draft = draft.copy(name = value)
                        saveProfile(draft)
                    }
                    nameEditorVisible = false
                }) { Text("GUARDAR") }
            },
            dismissButton = { androidx.compose.material.TextButton(onClick = { nameEditorVisible = false }) { Text("CANCELAR") } },
        )
    }
}

@Composable
private fun ProfileEntrance(visible: Boolean, delayMillis: Int, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    // Keep the item measured while the staggered entrance runs. AnimatedVisibility
    // removes an invisible LazyColumn item from measurement, which makes later
    // profile sections jump into the top position before the first cards appear.
    var animationStarted by remember { mutableStateOf(false) }
    LaunchedEffect(visible, delayMillis) {
        animationStarted = false
        if (visible) {
            delay(delayMillis.toLong())
            animationStarted = true
        }
    }
    val alpha by animateFloatAsState(
        targetValue = if (animationStarted) 1f else 0f,
        animationSpec = tween(330),
        label = "profile-entrance-alpha",
    )
    val offsetY by animateDpAsState(
        targetValue = if (animationStarted) 0.dp else (-16).dp,
        animationSpec = tween(330),
        label = "profile-entrance-offset",
    )
    Box(modifier.offset(y = offsetY).graphicsLayer { this.alpha = alpha }) { content() }
}

private enum class NotificationPreference { Reminders, Training }

/** Visual counterpart of iOS's `carrot.fill` used by the nutrition profile row. */
@Composable
private fun ProfileNutritionCarrotIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val leaves = Path().apply {
            moveTo(w * .50f, h * .34f)
            cubicTo(w * .39f, h * .28f, w * .31f, h * .16f, w * .33f, h * .08f)
            cubicTo(w * .42f, h * .10f, w * .48f, h * .17f, w * .50f, h * .24f)
            cubicTo(w * .52f, h * .16f, w * .59f, h * .09f, w * .68f, h * .08f)
            cubicTo(w * .69f, h * .18f, w * .61f, h * .29f, w * .50f, h * .34f)
            close()
        }
        drawPath(leaves, tint)
        val carrot = Path().apply {
            moveTo(w * .32f, h * .28f)
            cubicTo(w * .41f, h * .34f, w * .59f, h * .34f, w * .68f, h * .28f)
            cubicTo(w * .69f, h * .48f, w * .61f, h * .73f, w * .50f, h * .92f)
            cubicTo(w * .39f, h * .73f, w * .31f, h * .48f, w * .32f, h * .28f)
            close()
        }
        drawPath(carrot, tint)
    }
}

private enum class ProfileSection(val title: String, val icon: ImageVector, val usesNutritionCarrot: Boolean = false) {
    Body("Perfil corporal", Icons.Filled.Person),
    // iOS `Icon.fitness` is `dumbbell.fill`; keep the figure icon reserved for
    // the Workout tab and training-level choices.
    Fitness("Perfil de fitness", Icons.Filled.FitnessCenter),
    // iOS uses `carrot.fill` for the nutrition profile, not a restaurant glyph.
    Nutrition("Perfil nutricional", Icons.Filled.Restaurant, usesNutritionCarrot = true),
    // iOS uses `slider.horizontal.3`, whose filled Material counterpart is Tune.
    Settings("Ajustes", Icons.Filled.Tune),
    // iOS uses `crown.fill`; WorkspacePremium is the closest filled Material
    // equivalent and keeps the premium meaning without a generic star.
    Subscription("Suscripción", Icons.Filled.WorkspacePremium),
    Account("Cuenta", Icons.Filled.AccountCircle),
    Faq("Preguntas frecuentes", Icons.Filled.Help),
}

@Composable
private fun ProfileCategoryTree(profile: OnboardingProfile, nutritionProfile: NutritionProfilePreferences, appLanguage: String, accountRevision: Int, onSelect: (ProfileSection) -> Unit) {
    val context = LocalContext.current
    val account = remember(accountRevision) { currentWildforceAccount(context) }
    val subscription = SubscriptionAccessStore.load(context)
    val defaultTrainingLocation = profile.effectiveTrainingLocations().firstOrNull { it.isDefault }
        ?: profile.effectiveTrainingLocations().firstOrNull()
    val fitnessSummary = buildString {
        append(profile.goal.title)
        append(" • ")
        append(defaultTrainingLocation?.name ?: profile.gymType.title)
        defaultTrainingLocation?.displayAddress
            ?.trim()
            ?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
            ?.let { address ->
                append(" • ")
                append(address)
            }
    }
    val summaries = mapOf(
        ProfileSection.Body to displayWeight(profile.weightKg, profile.metricSystem).let { "$it ${if (profile.metricSystem == MetricSystem.Imperial) "lb" else "kg"}" },
        ProfileSection.Fitness to fitnessSummary,
        ProfileSection.Nutrition to if (nutritionProfile.isConfigured) "${nutritionProfile.dietaryStyle} • ${nutritionProfile.mealsPerDay} comidas" else "Configuración necesaria",
        ProfileSection.Settings to "$appLanguage • ${profile.metricSystem.title}",
        // iOS shows the current plan name here, not an action label.
        ProfileSection.Subscription to (subscription?.plan?.let(::subscriptionPlanName) ?: "No disponible"),
        // iOS uses a plain Account label without a secondary value.
        ProfileSection.Account to "",
    )
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(WildforceThemeTokens.backgroundSecondary)) {
        val visibleSections = ProfileSection.entries.filter { it != ProfileSection.Faq }
        visibleSections.forEachIndexed { index, section ->
            val summary = summaries.getValue(section)
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(section) }
                    .semantics {
                        contentDescription = if (summary.isBlank()) section.title else "${section.title}, $summary"
                        role = Role.Button
                    }
                    .padding(horizontal = 16.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (section.usesNutritionCarrot) {
                    ProfileNutritionCarrotIcon(WildforceThemeTokens.textPrimary, Modifier.size(21.dp))
                } else {
                    Icon(section.icon, contentDescription = null, modifier = Modifier.size(21.dp), tint = WildforceThemeTokens.textPrimary)
                }
                Text(section.title, Modifier.weight(1f), fontFamily = Exo2FontFamily, fontSize = 16.sp, color = WildforceThemeTokens.textPrimary)
                if (summary.isNotBlank()) {
                    Text(summary, Modifier.weight(1f), style = MaterialTheme.typography.body2, color = WildforceThemeTokens.textSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.End, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Icon(Icons.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp), tint = WildforceThemeTokens.textSecondary)
            }
            if (index < visibleSections.lastIndex) {
                Box(Modifier.fillMaxWidth().padding(start = 49.dp).height(1.dp).background(WildforceThemeTokens.textSecondary.copy(alpha = 0.16f)))
            }
        }
    }
}

internal enum class BodyMetricKind(val title: String, val unit: String, val icon: ImageVector) {
    // Keep the same order and symbol hierarchy as iOS BodyMetricType.allCases.
    Weight("Peso", "kg", Icons.Filled.Scale),
    BodyFat("Grasa corporal", "%", Icons.Filled.Percent),
    LeanBodyMass("Masa corporal magra", "kg", Icons.Filled.FitnessCenter),
    Waist("Cintura", "cm", Icons.Filled.Straighten),
    Neck("Cuello", "cm", Icons.Filled.Person),
    Hip("Cadera", "cm", Icons.Filled.Straighten),
}

/**
 * Mirrors the iOS Body Metrics destination: metric selector, current value,
 * compact history and a manual log action.  Android stores canonical values so
 * the display remains correct when users change between metric and imperial.
 */
@Composable
private fun BodyMetricsScreen(
    profile: OnboardingProfile,
    composition: BodyCompositionMeasurements,
    history: List<BodyMetricEntry>,
    onClose: () -> Unit,
    onRequestHealthConnect: (((Boolean, Int?, Double?, Double?, Double?) -> Unit) -> Unit),
    onSyncHealthConnect: (((Boolean, List<ImportedBodyMetric>) -> Unit) -> Unit),
    onSave: (OnboardingProfile, BodyCompositionMeasurements, Double?, Double?) -> Unit,
    onMetricSaved: (String, Double) -> Unit,
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val localizedResources = remember(context, configuration, profile.appLanguage) {
        val selectedLocale = Locale.forLanguageTag(
            when (profile.appLanguage) {
                "English" -> "en"
                "Català" -> "ca"
                "Français" -> "fr"
                "Italiano" -> "it"
                "Português" -> "pt"
                "Deutsch" -> "de"
                "中文（简体）" -> "zh-Hans"
                "Nederlands" -> "nl"
                "日本語" -> "ja"
                else -> "es"
            },
        )
        context.createConfigurationContext(Configuration(configuration).apply { setLocale(selectedLocale) }).resources
    }
    fun iosCatalogText(hash: String, fallback: String): String {
        val resourceId = localizedResources.getIdentifier(
            "ios_catalog_$hash",
            "string",
            context.packageName,
        )
        return if (resourceId != 0) localizedResources.getString(resourceId) else fallback
    }
    val timeframeLabels = listOf(
        7 to iosCatalogText("d50aade27375357f", "7D"),
        14 to iosCatalogText("4676a110a97fbc15", "2W"),
        30 to iosCatalogText("0ef5849949bcf7b6", "30D"),
        84 to iosCatalogText("a4a1c512469926e7", "12W"),
        Int.MAX_VALUE to iosCatalogText("a52ace420f2175d0", "All"),
    )
    val fontScale = LocalDensity.current.fontScale
    val timeframeSelectorHeight = maxOf(32f, 38f * fontScale).dp
    val timeframeLabelMaxLines = if (fontScale > 1.15f) 2 else 1
    var selected by remember { mutableStateOf(BodyMetricKind.Weight) }
    var timeframeDays by remember { mutableStateOf(30) }
    var metricHistory by remember(history) { mutableStateOf(history) }
    var pendingDeleteDate by remember { mutableStateOf<String?>(null) }
    var selectedPointIndex by remember(selected, timeframeDays) { mutableStateOf<Int?>(null) }
    var quickLogVisible by remember { mutableStateOf(false) }
    var quickLogInput by remember { mutableStateOf("") }
    var syncingHealthConnect by remember { mutableStateOf(false) }
    var healthConnectSyncMessage by remember { mutableStateOf<String?>(null) }
    var input by remember(selected, profile, composition) {
        mutableStateOf(bodyMetricDisplayValue(selected, profile, composition, history.lastOrNull()))
    }
    val entries = remember(metricHistory, selected) { metricHistory.filter { it.valueFor(selected) != null } }
    val visibleEntries = remember(entries, timeframeDays) {
        if (timeframeDays == Int.MAX_VALUE) entries else {
            // iOS's AnalyticsTimeframe intervals include today and therefore
            // use lookback - 1 as the earliest visible date.
            val cutoff = java.time.LocalDate.now().minusDays((timeframeDays - 1).toLong()).toString()
            entries.filter { it.date >= cutoff }
        }
    }
    val chartPoints = remember(visibleEntries, timeframeDays, selected) {
        bodyMetricChartPoints(visibleEntries, selected, timeframeDays)
    }
    val latest = entries.lastOrNull()?.valueFor(selected)
    fun saveMetricValue(rawValue: String): Boolean {
        val canonical = canonicalBodyMetric(selected, rawValue.bodyMetricDouble(), profile.metricSystem) ?: return false
        var updatedProfile = profile
        var updatedComposition = composition
        var leanBodyMass: Double? = null
        var bodyFat: Double? = null
        when (selected) {
            BodyMetricKind.Weight -> updatedProfile = profile.copy(weightKg = canonical)
            BodyMetricKind.LeanBodyMass -> leanBodyMass = canonical
            BodyMetricKind.Waist -> updatedComposition = composition.copy(waistCm = canonical)
            BodyMetricKind.Neck -> updatedComposition = composition.copy(neckCm = canonical)
            BodyMetricKind.Hip -> updatedComposition = composition.copy(hipCm = canonical)
            BodyMetricKind.BodyFat -> bodyFat = canonical
        }
        onSave(updatedProfile, updatedComposition, leanBodyMass, bodyFat)
        onMetricSaved(selected.name, canonical)
        metricHistory = BodyMetricsStore.load(context)
        input = formatBodyMetric(selected, canonical, profile.metricSystem).substringBefore(' ')
        return true
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().liquidGlassBackground().padding(horizontal = 16.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 16.dp, bottom = 160.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(48.dp).clip(CircleShape).background(WildforceThemeTokens.backgroundSecondary).clickable(onClick = onClose), contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = WildforceThemeTokens.textPrimary, modifier = Modifier.size(22.dp))
                    }
                    // Keep the title flexible: before this had its intrinsic width,
                    // which pushed the import and add actions past the right edge on
                    // narrower devices. iOS reserves this trailing toolbar space.
                    Text(
                        "MÉTRICAS CORPORALES",
                        modifier = Modifier.weight(1f),
                        fontFamily = Exo2FontFamily,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = WildforceThemeTokens.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .clickable {
                                healthConnectSyncMessage = null
                                syncingHealthConnect = true
                                onSyncHealthConnect { granted, imports ->
                                    syncingHealthConnect = false
                                    if (!granted) {
                                        healthConnectSyncMessage = "No se pudieron importar las métricas desde Salud."
                                    } else {
                                        BodyMetricsStore.recordImportedHistory(context, profile, imports)
                                        val latest = imports.maxByOrNull { it.date }
                                        val importedProfile = profile.copy(
                                            heightCm = latest?.heightCm?.coerceIn(120.0, 230.0)?.toInt() ?: profile.heightCm,
                                            weightKg = latest?.weightKg?.takeIf { it.isFinite() && it in 35.0..250.0 } ?: profile.weightKg,
                                        )
                                        onSave(importedProfile, composition, latest?.leanBodyMassKg, latest?.bodyFatPercentage)
                                        metricHistory = BodyMetricsStore.load(context)
                                        input = bodyMetricDisplayValue(selected, importedProfile, composition, metricHistory.lastOrNull())
                                        healthConnectSyncMessage = if (imports.isEmpty()) {
                                            "No se encontraron métricas en Salud."
                                        } else {
                                            "Se han importado ${imports.size} días de métricas desde Salud."
                                        }
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.ArrowDownward, contentDescription = "Importar desde Salud", tint = WildforceThemeTokens.textPrimary, modifier = Modifier.size(22.dp))
                    }
                    Box(
                        Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .clickable {
                                // Match iOS's NumericInputSheet: an add action starts
                                // with a fresh value rather than silently suggesting the
                                // prior measurement.
                                quickLogInput = ""
                                quickLogVisible = true
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Registrar ${selected.title.lowercase()}", tint = WildforceThemeTokens.textPrimary, modifier = Modifier.size(24.dp))
                    }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BodyMetricKind.entries.chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { kind ->
                            val isSelected = kind == selected
                            val cardValue = metricHistory.lastOrNull()?.valueFor(kind)
                                ?.let { formatBodyMetric(kind, it, profile.metricSystem) }
                                ?: bodyMetricDisplayValue(kind, profile, composition, metricHistory.lastOrNull()).ifBlank { "—" }
                            Box(
                                Modifier.weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(WildforceThemeTokens.textSecondary.copy(alpha = .08f))
                                    .border(
                                        width = if (isSelected) 2.dp else 0.dp,
                                        color = if (isSelected) WildforceThemeTokens.accent else Color.Transparent,
                                        shape = RoundedCornerShape(16.dp),
                                    )
                                    .semantics {
                                        contentDescription = "${kind.title}: $cardValue"
                                        role = Role.Button
                                        this.selected = isSelected
                                    }
                                    .clickable { selected = kind }.padding(12.dp),
                            contentAlignment = Alignment.TopStart,
                        ) {
                            BodyMetricStatCard(kind, cardValue, metricHistory, isSelected)
                        }
                    }
                    }
                    if (row.size == 1) Spacer(Modifier.height(0.dp))
                }
            }
        }
        item {
            Section("Progreso de ${selected.title.lowercase()}") {
                val latestValue = latest?.let { formatBodyMetric(selected, it, profile.metricSystem) } ?: "—"
                AnimatedContent(
                    targetState = latestValue,
                    transitionSpec = {
                        (fadeIn(tween(150)) + scaleIn(tween(180), initialScale = .88f)) togetherWith
                            (fadeOut(tween(110)) + scaleOut(tween(120), targetScale = 1.08f))
                    },
                    label = "body-metric-value-$selected",
                ) { value ->
                    Text(value, fontFamily = Exo2FontFamily, fontSize = 30.sp, color = WildforceThemeTokens.textPrimary)
                }
                Text("Último valor registrado", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .requiredHeight(timeframeSelectorHeight)
                        .background(
                            // Match the system fill used by iOS's
                            // UISegmentedControl instead of blending into the
                            // surrounding page/card background.
                            if (MaterialTheme.colors.isLight) Color(0xFFE5E5EA) else Color(0xFF3A3A3C),
                            RoundedCornerShape(10.dp),
                        )
                        .border(
                            1.dp,
                            WildforceThemeTokens.textPrimary.copy(alpha = .05f),
                            RoundedCornerShape(10.dp),
                        )
                        .padding(2.dp),
                ) {
                    // Match the iOS Picker(.segmented) used by BodyMetricsView:
                    // equal segments, compact height, and the same selected
                    // text color as the shared analytics control.
                    timeframeLabels.forEach { (days, label) ->
                        val active = timeframeDays == days
                        Box(
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (active) WildforceThemeTokens.textPrimary else Color.Transparent)
                                .semantics {
                                    contentDescription = label
                                    stateDescription = if (active) "Seleccionado" else "No seleccionado"
                                    this.selected = active
                                }
                                .clickable { timeframeDays = days },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                label,
                                fontSize = 13.sp,
                                lineHeight = 16.sp,
                                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (active) WildforceThemeTokens.primaryButtonText else WildforceThemeTokens.textPrimary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                maxLines = timeframeLabelMaxLines,
                                softWrap = timeframeLabelMaxLines > 1,
                                overflow = TextOverflow.Clip,
                            )
                        }
                    }
                }
                if (chartPoints.size >= 2) {
                    val chartValues = chartPoints.map { it.value }
                    val chartMin = chartValues.minOrNull() ?: 0.0
                    val chartMax = chartValues.maxOrNull() ?: chartMin
                    val chartPadding = maxOf((chartMax - chartMin) * .1, bodyMetricChartMinimumPadding(selected))
                    val chartLower = chartMin - chartPadding
                    val chartUpper = chartMax + chartPadding
                    val selectedPoint = selectedPointIndex?.let { chartPoints.getOrNull(it) }
                    val change = chartValues.last() - chartValues.first()
                    val chartGridColor = WildforceThemeTokens.textSecondary.copy(alpha = .12f)
                    val chartAccent = WildforceThemeTokens.accent
                    val chartBackground = WildforceThemeTokens.backgroundSecondary
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(formatBodyMetric(selected, latest ?: chartValues.last(), profile.metricSystem), fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textPrimary)
                            Text("Último ${selected.title.lowercase()}", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
                        }
                        if (chartValues.size > 1) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (change != 0.0) Icon(if (change < 0) Icons.Filled.ArrowDownward else Icons.Filled.ArrowUpward, contentDescription = null, modifier = Modifier.size(16.dp), tint = if (change < 0) Color(0xFF3F9B62) else WildforceThemeTokens.accent)
                                Text(formatBodyMetric(selected, kotlin.math.abs(change), profile.metricSystem), style = MaterialTheme.typography.caption, color = if (change == 0.0) WildforceThemeTokens.textSecondary else if (change < 0) Color(0xFF3F9B62) else WildforceThemeTokens.accent)
                            }
                        }
                    }
                    Canvas(
                        Modifier.fillMaxWidth().height(120.dp).pointerInput(chartPoints, selected) {
                            detectTapGestures { offset ->
                                val index = ((offset.x / size.width.toFloat().coerceAtLeast(1f)) * chartPoints.size).toInt().coerceIn(0, chartPoints.lastIndex)
                                selectedPointIndex = index
                            }
                        },
                    ) {
                        val chartHeight = size.height - 12.dp.toPx()
                        val step = size.width / (chartPoints.size - 1).coerceAtLeast(1)
                        fun point(index: Int, value: Double): androidx.compose.ui.geometry.Offset {
                            val normalized = ((value - chartLower) / (chartUpper - chartLower)).toFloat().coerceIn(0f, 1f)
                            return androidx.compose.ui.geometry.Offset(step * index, chartHeight * (1f - normalized))
                        }
                        repeat(4) { gridIndex ->
                            val y = chartHeight * gridIndex / 3f
                            drawLine(chartGridColor, androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Offset(size.width, y), 1.dp.toPx())
                        }
                        val line = Path()
                        chartPoints.forEachIndexed { index, chartPoint ->
                            val plotted = point(index, chartPoint.value)
                            if (index == 0) line.moveTo(plotted.x, plotted.y) else line.lineTo(plotted.x, plotted.y)
                        }
                        val area = Path().apply {
                            addPath(line)
                            lineTo(size.width, chartHeight)
                            lineTo(0f, chartHeight)
                            close()
                        }
                        drawPath(area, chartAccent.copy(alpha = .16f))
                        drawPath(line, chartAccent, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                        chartPoints.forEachIndexed { index, chartPoint ->
                            val plotted = point(index, chartPoint.value)
                            drawCircle(chartAccent, 4.dp.toPx(), plotted)
                            if (selectedPointIndex == index) {
                                drawCircle(chartBackground, 8.dp.toPx(), plotted)
                                drawCircle(chartAccent, 5.dp.toPx(), plotted)
                            }
                        }
                    }
                    val axisIndices = bodyMetricAxisIndices(chartPoints, timeframeDays)
                    if (axisIndices.isNotEmpty()) {
                        val axisLocale = localizedResources.configuration.locales[0]
                        Row(
                            Modifier.fillMaxWidth().height(20.dp).padding(horizontal = 2.dp),
                        ) {
                            axisIndices.forEach { index ->
                                val point = chartPoints[index]
                                Box(Modifier.weight(1f), contentAlignment = when {
                                    index == axisIndices.first() -> Alignment.CenterStart
                                    index == axisIndices.last() -> Alignment.CenterEnd
                                    else -> Alignment.Center
                                }) {
                                    Text(
                                        bodyMetricAxisLabel(point.date, timeframeDays, axisLocale),
                                        style = MaterialTheme.typography.caption,
                                        color = WildforceThemeTokens.textSecondary,
                                        maxLines = 1,
                                        modifier = Modifier.semantics {
                                            contentDescription = "Eje: ${bodyMetricAxisLabel(point.date, timeframeDays, axisLocale)}"
                                        },
                                    )
                                }
                            }
                        }
                    }
                    selectedPoint?.let { point ->
                        Text("${point.dateLabel} · ${formatBodyMetric(selected, point.value, profile.metricSystem)}", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
                    }
                }
                if (visibleEntries.isEmpty()) {
                    Text("Aún no hay registros para este periodo.", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
                } else {
                    val values = visibleEntries.mapNotNull { it.valueFor(selected) }
                    val min = values.minOrNull() ?: 0.0
                    val max = (values.maxOrNull() ?: min).let { if (it == min) min + 1 else it }
                    visibleEntries.asReversed().forEach { entry ->
                        val value = entry.valueFor(selected) ?: return@forEach
                        val fraction = ((value - min) / (max - min)).toFloat().coerceIn(.04f, 1f)
                        val animatedFraction by animateFloatAsState(
                            targetValue = fraction,
                            animationSpec = tween(420),
                            label = "body-metric-history-${entry.date}",
                        )
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Column(Modifier.width(92.dp), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                Text(bodyMetricHistoryDateLabel(entry.date, localizedResources.configuration.locales[0]), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary, maxLines = 1)
                                Text(entry.source.displayName, style = MaterialTheme.typography.overline, color = WildforceThemeTokens.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(8.dp)).background(WildforceThemeTokens.textSecondary.copy(alpha = .12f))) {
                                Box(Modifier.fillMaxWidth(animatedFraction).fillMaxHeight().background(WildforceThemeTokens.accent))
                            }
                            Text(formatBodyMetric(selected, value, profile.metricSystem), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textPrimary)
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "Eliminar registro",
                                tint = WildforceThemeTokens.textSecondary,
                                modifier = Modifier.size(48.dp).clickable { pendingDeleteDate = entry.date },
                            )
                        }
                    }
                }
            }
        }
        item {
            Section(selected.title) {
                Text(bodyMetricHint(selected), style = MaterialTheme.typography.body2, color = WildforceThemeTokens.textSecondary)
            }
        }
    }
    pendingDeleteDate?.let { date ->
        androidx.compose.material.AlertDialog(
            onDismissRequest = { pendingDeleteDate = null },
            title = { Text("¿Eliminar registro?") },
            text = { Text("Se eliminará el registro de ${date.takeLast(5)} para ${selected.title.lowercase()}.") },
            confirmButton = {
                Button(onClick = {
                    BodyMetricsStore.deleteMetric(context, date, selected)
                    metricHistory = BodyMetricsStore.load(context)
                    pendingDeleteDate = null
                }) { Text("Eliminar") }
            },
            dismissButton = { Button(onClick = { pendingDeleteDate = null }) { Text("Cancelar") } },
        )
    }
    if (quickLogVisible) {
        androidx.compose.material.AlertDialog(
            onDismissRequest = { quickLogVisible = false },
            title = { Text("Registrar ${selected.title.lowercase()}") },
            text = {
                NumberField(
                    label = "${selected.title} (${displayBodyMetricUnit(selected, profile.metricSystem)})",
                    value = quickLogInput,
                    onValueChange = { quickLogInput = it.profileDecimal() },
                    modifier = Modifier.fillMaxWidth(),
                    decimal = true,
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (saveMetricValue(quickLogInput)) quickLogVisible = false
                }) { Text("GUARDAR") }
            },
            dismissButton = { Button(onClick = { quickLogVisible = false }) { Text("CANCELAR") } },
        )
    }
    if (syncingHealthConnect || healthConnectSyncMessage != null) {
        androidx.compose.material.AlertDialog(
            onDismissRequest = { if (!syncingHealthConnect) healthConnectSyncMessage = null },
            title = { Text("Sincronizar métricas") },
            text = { Text(if (syncingHealthConnect) "Sincronizando métricas desde Salud…" else healthConnectSyncMessage.orEmpty()) },
            confirmButton = {
                if (!syncingHealthConnect) {
                    TextButton(onClick = { healthConnectSyncMessage = null }) { Text("CERRAR") }
                }
            },
        )
    }
}

@Composable
private fun BodyMetricStatCard(
    kind: BodyMetricKind,
    value: String,
    history: List<BodyMetricEntry>,
    selected: Boolean,
) {
    val values = history.mapNotNull { it.valueFor(kind) }
    // iOS keeps the card surface and text neutral. Selection is communicated by
    // the accent icon plus the two-point outline around the card, not by
    // inverting the entire card's colors.
    val foreground = WildforceThemeTokens.textPrimary
    val iconColor = if (selected) WildforceThemeTokens.accent else WildforceThemeTokens.textSecondary
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Icon(kind.icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
            if (values.size >= 2) BodyMetricSparkline(values, kind, iconColor)
        }
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = foreground, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(kind.title, style = MaterialTheme.typography.caption, fontWeight = FontWeight.Medium, color = foreground, maxLines = 2)
    }
}

@Composable
private fun BodyMetricSparkline(values: List<Double>, kind: BodyMetricKind, color: Color) {
    Canvas(Modifier.size(width = 52.dp, height = 26.dp)) {
        val minimum = values.minOrNull() ?: return@Canvas
        val maximum = values.maxOrNull() ?: minimum
        val padding = maxOf((maximum - minimum) * .08, bodyMetricChartMinimumPadding(kind))
        val lower = minimum - padding
        val upper = maximum + padding
        val step = size.width / values.lastIndex.coerceAtLeast(1)
        val path = Path()
        values.forEachIndexed { index, value ->
            val x = step * index
            val y = size.height * (1f - ((value - lower) / (upper - lower)).toFloat().coerceIn(0f, 1f))
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

internal data class BodyMetricChartPoint(
    val date: LocalDate,
    val dateLabel: String,
    val value: Double,
)

internal fun bodyMetricChartPoints(
    entries: List<BodyMetricEntry>,
    kind: BodyMetricKind,
    timeframeDays: Int,
): List<BodyMetricChartPoint> {
    val points = entries.mapNotNull { entry ->
        val value = entry.valueFor(kind) ?: return@mapNotNull null
        val date = runCatching { LocalDate.parse(entry.date) }.getOrNull() ?: return@mapNotNull null
        date to value
    }
    val orderedPoints = points.sortedBy { it.first }
    if (timeframeDays <= 30) {
        return orderedPoints.map { (date, value) -> BodyMetricChartPoint(date, date.toString().takeLast(5), value) }
    }
    val grouped = orderedPoints.groupBy { (date, _) ->
        if (timeframeDays == Int.MAX_VALUE) date.withDayOfMonth(1)
        else date.minusDays((date.dayOfWeek.value - 1).toLong())
    }
    return grouped.entries
        .sortedBy { it.key }
        .map { (date, values) ->
            BodyMetricChartPoint(
                date = date,
                dateLabel = date.toString().takeLast(5),
                value = values.map { it.second }.average(),
            )
        }
}

/** Mirrors Swift Charts' period-specific X-axis marks without changing the plotted values. */
internal fun bodyMetricAxisIndices(
    points: List<BodyMetricChartPoint>,
    timeframeDays: Int,
): List<Int> {
    if (points.isEmpty()) return emptyList()
    if (timeframeDays <= 14) return points.indices.toList()

    val indices = points.indices.filter { index ->
        val previous = points.getOrNull(index - 1)?.date
        when {
            timeframeDays <= 30 -> index == 0 || points[index].date.dayOfWeek.value == 1 || index == points.lastIndex
            else -> index == 0 || previous?.month != points[index].date.month || index == points.lastIndex
        }
    }
    return indices.distinct()
}

internal fun bodyMetricAxisLabel(date: LocalDate, timeframeDays: Int, locale: Locale): String = when {
    timeframeDays <= 14 -> date.format(DateTimeFormatter.ofPattern("EEE", locale))
    timeframeDays <= 30 -> date.format(DateTimeFormatter.ofPattern("d MMM", locale))
    else -> date.format(DateTimeFormatter.ofPattern("MMM", locale))
}

internal fun bodyMetricHistoryDateLabel(rawDate: String, locale: Locale): String {
    val date = runCatching { LocalDate.parse(rawDate) }.getOrNull() ?: return rawDate
    return date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))
}

private fun BodyMetricEntry.valueFor(kind: BodyMetricKind): Double? = when (kind) {
    BodyMetricKind.Weight -> weightKg
    BodyMetricKind.LeanBodyMass -> leanBodyMassKg
    BodyMetricKind.Waist -> waistCm
    BodyMetricKind.Neck -> neckCm
    BodyMetricKind.Hip -> hipCm
    BodyMetricKind.BodyFat -> bodyFatPercentage
}

private fun bodyMetricDisplayValue(kind: BodyMetricKind, profile: OnboardingProfile, composition: BodyCompositionMeasurements, latest: BodyMetricEntry?): String = when (kind) {
    BodyMetricKind.Weight -> displayWeight(profile.weightKg, profile.metricSystem)
    BodyMetricKind.Waist -> composition.waistCm?.let { formatBodyMetric(kind, it, profile.metricSystem).substringBefore(' ') }.orEmpty()
    BodyMetricKind.Neck -> composition.neckCm?.let { formatBodyMetric(kind, it, profile.metricSystem).substringBefore(' ') }.orEmpty()
    BodyMetricKind.Hip -> composition.hipCm?.let { formatBodyMetric(kind, it, profile.metricSystem).substringBefore(' ') }.orEmpty()
    BodyMetricKind.LeanBodyMass, BodyMetricKind.BodyFat -> latest?.valueFor(kind)?.let { formatBodyMetric(kind, it, profile.metricSystem).substringBefore(' ') }.orEmpty()
}

private fun displayBodyMetricUnit(kind: BodyMetricKind, metricSystem: MetricSystem): String = when (kind) {
    BodyMetricKind.Weight, BodyMetricKind.LeanBodyMass -> if (metricSystem == MetricSystem.Imperial) "lb" else "kg"
    BodyMetricKind.Waist, BodyMetricKind.Neck, BodyMetricKind.Hip -> if (metricSystem == MetricSystem.Imperial) "in" else "cm"
    BodyMetricKind.BodyFat -> "%"
}

private fun formatBodyMetric(kind: BodyMetricKind, canonical: Double, metricSystem: MetricSystem): String {
    val displayed = when (kind) {
        BodyMetricKind.Weight, BodyMetricKind.LeanBodyMass -> if (metricSystem == MetricSystem.Imperial) canonical * PoundsPerKilogram else canonical
        BodyMetricKind.Waist, BodyMetricKind.Neck, BodyMetricKind.Hip -> if (metricSystem == MetricSystem.Imperial) canonical / 2.54 else canonical
        BodyMetricKind.BodyFat -> canonical
    }
    return "%.1f %s".format(Locale.US, displayed, displayBodyMetricUnit(kind, metricSystem))
}

private fun bodyMetricChartMinimumPadding(kind: BodyMetricKind): Double = when (kind) {
    BodyMetricKind.Weight, BodyMetricKind.LeanBodyMass, BodyMetricKind.BodyFat -> 0.25
    BodyMetricKind.Waist, BodyMetricKind.Neck, BodyMetricKind.Hip -> 0.5
}

internal fun canonicalBodyMetric(kind: BodyMetricKind, value: Double?, metricSystem: MetricSystem): Double? {
    val input = value ?: return null
    val canonical = when (kind) {
        BodyMetricKind.Weight, BodyMetricKind.LeanBodyMass -> if (metricSystem == MetricSystem.Imperial) input / PoundsPerKilogram else input
        BodyMetricKind.Waist, BodyMetricKind.Neck, BodyMetricKind.Hip -> if (metricSystem == MetricSystem.Imperial) input * 2.54 else input
        BodyMetricKind.BodyFat -> input
    }
    return canonical.takeIf { it.isFinite() && when (kind) {
        BodyMetricKind.Weight -> it in 20.0..400.0
        BodyMetricKind.LeanBodyMass -> it in 1.0..300.0
        BodyMetricKind.Waist, BodyMetricKind.Neck, BodyMetricKind.Hip -> it in 20.0..300.0
        BodyMetricKind.BodyFat -> it in 1.0..90.0
    } }
}

private fun String.bodyMetricDouble(): Double? = replace(',', '.').toDoubleOrNull()

private fun bodyMetricHint(kind: BodyMetricKind): String = when (kind) {
    BodyMetricKind.Weight -> "Registra el peso en condiciones similares para que la tendencia sea comparable."
    BodyMetricKind.LeanBodyMass -> "Usa una medición fiable y registra siempre con el mismo método cuando sea posible."
    BodyMetricKind.Waist -> "Mide alrededor de la cintura, relajado y tras exhalar normalmente."
    BodyMetricKind.Neck -> "Mide justo por debajo de la laringe, con la cinta horizontal."
    BodyMetricKind.Hip -> "Mide en la parte más ancha de las caderas y los glúteos."
    BodyMetricKind.BodyFat -> "Registra el porcentaje de grasa corporal de tu método de medición habitual."
}

@Composable
private fun ProfileBodyMetrics(
    profile: OnboardingProfile,
    measurements: BodyCompositionMeasurements,
) {
    val currentYear = Year.now().value
    val safeBirthYear = profile.birthYear.coerceIn(1900, currentYear)
    val age = ageInYears(safeBirthYear, profile.birthMonth, profile.birthDay, LocalDate.now()).coerceIn(0, 120)
    val safeHeightCm = profile.heightCm.coerceIn(80, 260)
    val safeWeightKg = profile.weightKg.takeIf { it.isFinite() && it > 0.0 } ?: 70.0
    val basalMetabolicRate = when (profile.gender.storedValue) {
        "female" -> 10 * safeWeightKg + 6.25 * safeHeightCm - 5 * age - 161
        else -> 10 * safeWeightKg + 6.25 * safeHeightCm - 5 * age + 5
    }
    val activityFactor = when (profile.lifestyle) {
        LifestyleLevel.Sedentary -> 1.2
        LifestyleLevel.LightlyActive -> 1.375
        LifestyleLevel.ModeratelyActive -> 1.55
        LifestyleLevel.VeryActive -> 1.725
    }
    val displayedBasalMetabolicRate = basalMetabolicRate.roundToInt()
    val activeMetabolicRate = (basalMetabolicRate * activityFactor).roundToInt()
    Section("TMB") {
        ReadOnlyProfileRow("Metabolismo basal", "$displayedBasalMetabolicRate kcal", "Calorías que quema tu cuerpo en reposo absoluto según altura, peso, edad y sexo.")
        ReadOnlyProfileRow("Metabolismo activo", "$activeMetabolicRate kcal", "TMB ajustada a tu estilo de vida ${profile.lifestyle.title.lowercase()}.")
    }
    Section("Composición corporal") {
        val waistToHeight = measurements.waistCm?.div(safeHeightCm)
        if (waistToHeight != null) {
            val zone = waistToHeightZone(waistToHeight)
            val zoneColor = when {
                waistToHeight < .40 -> WildforceThemeTokens.textSecondary
                waistToHeight < .50 -> Color(0xFF4CAF50)
                waistToHeight < .60 -> Color(0xFFFF9800)
                else -> Color(0xFFF44336)
            }
            ZoneMetricRow(
                title = "Relación cintura-altura",
                value = "%.2f".format(waistToHeight),
                zone = zone,
                zoneColor = zoneColor,
                progress = ((waistToHeight - .30) / (.70 - .30)).toFloat(),
                segmentFractions = listOf(.25f, .25f, .25f, .25f),
                boundaryLabels = listOf("0.40", "0.50", "0.60"),
                segmentColors = listOf(
                    WildforceThemeTokens.textSecondary.copy(alpha = .4f),
                    Color(0xFF4CAF50).copy(alpha = .75f),
                    Color(0xFFFF9800).copy(alpha = .75f),
                    Color(0xFFF44336).copy(alpha = .75f),
                ),
            )
        } else ReadOnlyProfileRow("Relación cintura-altura", "—", "Registra la cintura para ver este dato.")
        val bodyFat = navyBodyFat(profile.gender, safeHeightCm.toDouble(), measurements)
        if (bodyFat != null) {
            val isFemale = profile.gender.storedValue == "female"
            val zone = if (isFemale) {
                when {
                    bodyFat < 21 -> "Atlético"
                    bodyFat < 25 -> "En forma"
                    bodyFat < 32 -> "Promedio"
                    else -> "Elevado"
                }
            } else {
                when {
                    bodyFat < 14 -> "Atlético"
                    bodyFat < 18 -> "En forma"
                    bodyFat < 25 -> "Promedio"
                    else -> "Elevado"
                }
            }
            val lowerBoundaries = if (isFemale) listOf(21.0, 25.0, 32.0) else listOf(14.0, 18.0, 25.0)
            val barMin = if (isFemale) 10.0 else 3.0
            val barMax = if (isFemale) 50.0 else 40.0
            val edges = listOf(barMin) + lowerBoundaries + barMax
            val fractions = edges.zipWithNext { start, end -> ((end - start) / (barMax - barMin)).toFloat() }
            val zoneColor = when {
                bodyFat < lowerBoundaries[0] -> Color(0xFF2196F3)
                bodyFat < lowerBoundaries[1] -> Color(0xFF4CAF50)
                bodyFat < lowerBoundaries[2] -> Color(0xFFFF9800)
                else -> Color(0xFFF44336)
            }
            ZoneMetricRow(
                title = "Grasa corporal estimada",
                value = "%.1f%%".format(bodyFat),
                zone = zone,
                zoneColor = zoneColor,
                progress = ((bodyFat - barMin) / (barMax - barMin)).toFloat(),
                segmentFractions = fractions,
                boundaryLabels = lowerBoundaries.map { it.toInt().toString() + "%" },
                segmentColors = listOf(
                    Color(0xFF2196F3).copy(alpha = .6f),
                    Color(0xFF4CAF50).copy(alpha = .75f),
                    Color(0xFFFF9800).copy(alpha = .75f),
                    Color(0xFFF44336).copy(alpha = .75f),
                ),
                description = "Estimación mediante la fórmula de la Marina de EE. UU. usando cintura${if (isFemale) ", cadera" else ""} y cuello.",
            )
        } else ReadOnlyProfileRow("Grasa corporal estimada", "—", if (profile.gender.storedValue == "female") "Registra cintura, cuello y cadera para verla." else "Registra cintura y cuello para verla.")
    }
}

@Composable
private fun ReadOnlyProfileRow(label: String, value: String, description: String? = null) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, Modifier.weight(1f), color = WildforceThemeTokens.textPrimary)
            Text(value, color = WildforceThemeTokens.textSecondary)
        }
        description?.let { Text(it, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary) }
    }
}

@Composable
private fun ZoneMetricRow(
    title: String,
    value: String,
    zone: String,
    zoneColor: Color,
    progress: Float,
    segmentFractions: List<Float>,
    boundaryLabels: List<String>,
    segmentColors: List<Color>,
    description: String? = null,
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, Modifier.weight(1f), color = WildforceThemeTokens.textPrimary)
            Text(value, color = WildforceThemeTokens.textSecondary)
        }
        Text(zone, style = MaterialTheme.typography.caption, color = zoneColor)
        ZoneRangeBar(progress, segmentFractions, boundaryLabels, segmentColors)
        description?.let { Text(it, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary) }
    }
}

@Composable
private fun ZoneRangeBar(progress: Float, segmentFractions: List<Float>, boundaryLabels: List<String>, segmentColors: List<Color>) {
    BoxWithConstraints(Modifier.fillMaxWidth().height(36.dp)) {
        val clampedProgress = progress.coerceIn(0f, 1f)
        Row(
            Modifier.fillMaxWidth().align(Alignment.TopCenter).padding(top = 14.dp).height(8.dp).clip(CircleShape),
            horizontalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            segmentFractions.forEachIndexed { index, fraction ->
                Box(Modifier.weight(fraction).fillMaxHeight().background(segmentColors[index]))
            }
        }
        Box(
            Modifier.offset(x = maxWidth * clampedProgress - 7.dp, y = 11.dp)
                .size(14.dp)
                .background(Color.White, CircleShape)
                .border(1.5.dp, WildforceThemeTokens.textPrimary.copy(alpha = .5f), CircleShape)
                .clip(CircleShape),
        )
        var cumulative = 0f
        boundaryLabels.forEachIndexed { index, label ->
            cumulative += segmentFractions[index]
            Text(
                label,
                Modifier.offset(x = maxWidth * cumulative - 20.dp, y = 20.dp).width(40.dp),
                style = MaterialTheme.typography.overline,
                color = WildforceThemeTokens.textSecondary,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

private fun navyBodyFat(gender: Gender, heightCm: Double, measurements: BodyCompositionMeasurements): Double? {
    return navyBodyFatEstimate(gender, heightCm, measurements.waistCm, measurements.neckCm, measurements.hipCm)
}

internal fun waistToHeightZone(ratio: Double): String = when {
    ratio < .40 -> "Delgado"
    ratio < .50 -> "Saludable"
    ratio < .60 -> "Elevado"
    else -> "Alto"
}

internal fun ageInYears(birthYear: Int, birthMonth: Int, today: LocalDate): Int {
    val safeMonth = birthMonth.coerceIn(1, 12)
    return ageInYears(birthYear, safeMonth, 1, today)
}

internal fun ageInYears(birthYear: Int, birthMonth: Int, birthDay: Int, today: LocalDate): Int {
    val safeMonth = birthMonth.coerceIn(1, 12)
    val safeDay = birthDay.coerceIn(1, daysInBirthMonth(birthYear, safeMonth))
    return java.time.Period.between(LocalDate.of(birthYear, safeMonth, safeDay), today).years.coerceAtLeast(0)
}

internal fun navyBodyFatEstimate(
    gender: Gender,
    heightCm: Double,
    waistCm: Double?,
    neckCm: Double?,
    hipCm: Double?,
): Double? {
    val waist = waistCm ?: return null
    val neck = neckCm ?: return null
    if (!heightCm.isFinite() || heightCm <= 0 || !waist.isFinite() || waist <= 0 || !neck.isFinite() || neck <= 0) return null
    val centimetersPerInch = 2.54
    val heightInches = heightCm / centimetersPerInch
    val waistInches = waist / centimetersPerInch
    val neckInches = neck / centimetersPerInch
    val bodyFat = if (gender.storedValue == "female") {
        val hipInches = (hipCm ?: return null) / centimetersPerInch
        val circumference = waistInches + hipInches - neckInches
        if (!hipInches.isFinite() || hipInches <= 0 || circumference <= 0) return null
        163.205 * log10(circumference) - 97.684 * log10(heightInches) - 78.387
    } else {
        val circumference = waistInches - neckInches
        if (circumference <= 0) return null
        86.010 * log10(circumference) - 70.041 * log10(heightInches) + 36.76
    }
    return bodyFat.takeIf { it.isFinite() && it > 0 }
}

@Composable
private fun ProfileHero(profile: OnboardingProfile, currentStreak: Int, completedWorkouts: Int, longestStreak: Int, avatarPath: String?, avatarRevision: Int, onChangeAvatar: () -> Unit, entered: Boolean, onEditName: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val avatarSize = if (maxWidth < 380.dp) 88.dp else 110.dp
        val gap = if (maxWidth < 380.dp) 12.dp else 20.dp
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(gap)) {
            ProfileEntrance(entered, 100) {
                Box(
                    Modifier.size(avatarSize)
                        .semantics { contentDescription = "Cambiar foto de perfil" }
                        .clickable(onClick = onChangeAvatar),
                ) {
                    val bitmap = remember(avatarPath, avatarRevision) { avatarPath?.let { BitmapFactory.decodeFile(it) } }
                    if (bitmap != null) Image(bitmap.asImageBitmap(), profile.name, Modifier.fillMaxSize().clip(CircleShape), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                    else Box(
                        Modifier.fillMaxSize().background(WildforceThemeTokens.textSecondary.copy(alpha = .1f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.Person,
                            contentDescription = null,
                            modifier = Modifier.size(if (avatarSize < 100.dp) 44.dp else 55.dp),
                            tint = WildforceThemeTokens.textSecondary,
                        )
                    }
                    Box(
                        Modifier.align(Alignment.BottomEnd).offset((-4).dp, (-4).dp).size(34.dp)
                            .background(WildforceThemeTokens.accent, CircleShape)
                            .border(4.dp, WildforceThemeTokens.background, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.PhotoCamera, contentDescription = "Cambiar foto de perfil", modifier = Modifier.size(16.dp), tint = WildforceThemeTokens.primaryButtonText)
                    }
                }
            }
            Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ProfileEntrance(entered, 120) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(profile.name.ifBlank { "Tu perfil" }, fontFamily = AntonFontFamily, fontSize = 28.sp, color = WildforceThemeTokens.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.heightIn(min = 48.dp).clickable(onClick = onEditName))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Filled.FitnessCenter, contentDescription = null, modifier = Modifier.size(15.dp), tint = WildforceThemeTokens.textSecondary)
                            Text("${profile.goal.title} • ${profile.trainingLevel.title}", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    ProfileEntrance(entered, 180, Modifier.weight(1f)) { ProfileStat("Racha", currentStreak.toString(), Modifier.fillMaxWidth()) }
                    ProfileEntrance(entered, 220, Modifier.weight(1f)) { ProfileStat("Más larga", longestStreak.toString(), Modifier.fillMaxWidth()) }
                    ProfileEntrance(entered, 260, Modifier.weight(1f)) { ProfileStat("Entrenos", completedWorkouts.toString(), Modifier.fillMaxWidth()) }
                }
            }
        }
    }
}

@Composable
private fun ProfileExperience(experienceXp: Int, experienceLevel: Int, experienceProgress: Float) {
    val animatedExperienceProgress by animateFloatAsState(experienceProgress.coerceIn(0f, 1f), animationSpec = tween(650), label = "profile-experience")
    val nextLevelXp = minimumTotalXp(experienceLevel + 1)
    Column(
        Modifier.fillMaxWidth()
            .padding(vertical = 8.dp)
            .semantics {
                contentDescription = "Nivel $experienceLevel"
                stateDescription = "$experienceXp de $nextLevelXp XP"
            },
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Text("Nivel $experienceLevel", fontFamily = Exo2FontFamily, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textPrimary)
            Text("$experienceXp / $nextLevelXp XP", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
        }
        Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(8.dp)).background(WildforceThemeTokens.textSecondary.copy(alpha = .12f))) {
            if (animatedExperienceProgress > 0f) {
                Box(Modifier.fillMaxWidth(animatedExperienceProgress).fillMaxHeight().clip(RoundedCornerShape(8.dp)).background(WildforceThemeTokens.accentRed.copy(alpha = .40f)).blur(6.dp))
                Box(Modifier.fillMaxWidth(animatedExperienceProgress).fillMaxHeight().clip(RoundedCornerShape(8.dp)).background(WildforceThemeTokens.accentRed))
            }
        }
    }
}

@Composable
private fun ProfileStat(label: String, value: String, modifier: Modifier) {
    val icon = when (label) {
        "Entrenos" -> Icons.Filled.EmojiEvents
        else -> Icons.Filled.LocalFireDepartment
    }
    Column(
        modifier.padding(4.dp).semantics {
            contentDescription = label
            stateDescription = value
        },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = if (label == "Entrenos") Color(0xFFF2C21A) else WildforceThemeTokens.accentGold)
            Text(value, style = MaterialTheme.typography.body1, color = WildforceThemeTokens.textPrimary, maxLines = 1)
        }
        Text(label, fontFamily = Exo2FontFamily, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = WildforceThemeTokens.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private fun minimumTotalXp(level: Int): Int {
    if (level <= 1) return 0
    return (2..level).sumOf { current -> 60 + (current - 2) * 35 }
}

private sealed class NutritionPicker(val title: String, val options: List<String>) {
    data object DietaryStyle : NutritionPicker("Estilo alimentario", listOf("Omnívora", "Vegetariana", "Vegana", "Pescetariana"))
    data object MealsPerDay : NutritionPicker("Comidas al día", (1..8).map { "$it" })
    data object EatingWindow : NutritionPicker("Ventana de alimentación", listOf("Flexible", "06:00 - 18:00", "08:00 - 20:00", "10:00 - 22:00", "12:00 - 20:00"))
    data object CookingEffort : NutritionPicker("Esfuerzo al cocinar", listOf("Bajo", "Medio", "Alto"))
    data object BudgetSensitivity : NutritionPicker("Sensibilidad al presupuesto", listOf("Baja", "Media", "Alta"))

    fun apply(profile: NutritionProfilePreferences, selected: String): NutritionProfilePreferences = when (this) {
        DietaryStyle -> profile.copy(dietaryStyle = selected)
        MealsPerDay -> profile.copy(mealsPerDay = selected.toIntOrNull()?.coerceIn(1, 8) ?: profile.mealsPerDay)
        EatingWindow -> if (selected == "Flexible") profile.copy(eatingWindowStartHour = null, eatingWindowEndHour = null) else {
            val hours = Regex("\\d{2}").findAll(selected).mapNotNull { it.value.toIntOrNull() }.toList()
            profile.copy(eatingWindowStartHour = hours.getOrNull(0), eatingWindowEndHour = hours.getOrNull(1))
        }
        CookingEffort -> profile.copy(cookingEffort = selected)
        BudgetSensitivity -> profile.copy(budgetSensitivity = selected)
    }
}

private enum class NutritionTextField(val title: String, val prompt: String, val isList: Boolean = true) {
    ProteinSources("Fuentes de proteína", "p. ej., huevos, yogur griego, salmón, tofu"),
    Dislikes("Alimentos que no te gustan", "p. ej., aceitunas, hígado, requesón"),
    ExcludedFoods("Alimentos excluidos", "p. ej., setas, aceitunas, hígado"),
    Allergies("Alergias o intolerancias", "p. ej., cacahuetes, lactosa"),
    Notes("Notas", "Preferencias o contexto adicional para la planificación", false),
    ;

    fun value(profile: NutritionProfilePreferences): String = when (this) {
        ProteinSources -> profile.preferredProteinSources.joinToString(", ")
        Dislikes -> profile.dislikes.joinToString(", ")
        ExcludedFoods -> profile.excludedFoods.joinToString(", ")
        Allergies -> profile.allergiesAndIntolerances.joinToString(", ")
        Notes -> profile.notes.takeUnless { it.equals("null", ignoreCase = true) } ?: ""
    }

    fun apply(profile: NutritionProfilePreferences, value: String): NutritionProfilePreferences = when (this) {
        ProteinSources -> profile.copy(preferredProteinSources = parseProfileList(value))
        Dislikes -> profile.copy(dislikes = parseProfileList(value))
        ExcludedFoods -> profile.copy(excludedFoods = parseProfileList(value))
        Allergies -> profile.copy(allergiesAndIntolerances = parseProfileList(value))
        Notes -> profile.copy(notes = value.trim().take(500))
    }
}

private sealed class SettingsPicker(val title: String, val options: List<String>) {
    data object Language : SettingsPicker("Idioma", listOf("Español", "English", "Català", "Français", "Italiano", "Português", "Deutsch", "中文（简体）", "Nederlands", "日本語"))
    data object Theme : SettingsPicker("Tema", listOf("Sistema", "Claro", "Oscuro"))
}

@Composable
private fun WearMotionOnboardingDialog(onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(WildforceThemeTokens.background)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(190.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    Modifier
                        .width(164.dp)
                        .height(164.dp)
                        .clip(RoundedCornerShape(38.dp))
                        .background(WildforceThemeTokens.backgroundSecondary)
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    listOf("1   0.9s", "2   1.4s", "3   2.2s", "4   0.9s").forEach { rep ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(WildforceThemeTokens.surfaceElevated)
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(rep, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textPrimary)
                            Spacer(Modifier.weight(1f))
                            Box(Modifier.size(7.dp).clip(CircleShape).background(WildforceThemeTokens.accent))
                        }
                    }
                }
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-2).dp, y = 12.dp)
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(WildforceThemeTokens.accent),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.FitnessCenter, contentDescription = null, tint = Color.White, modifier = Modifier.size(25.dp))
                }
            }
            Text("Seguimiento automático Wear OS", fontFamily = Exo2FontFamily, fontSize = 27.sp, fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textPrimary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Text("Cuenta repeticiones y muestra insights de movimiento en ejercicios compatibles.", style = MaterialTheme.typography.body1, color = WildforceThemeTokens.textSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)

            Column(Modifier.fillMaxWidth().padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                WearMotionFeature(Icons.Filled.FitnessCenter, "Repeticiones automáticas", "El reloj cuenta tus repeticiones mientras te concentras en el movimiento.")
                WearMotionFeature(Icons.Filled.Star, "Insights de movimiento", "Consulta cadencia, rango de movimiento y consistencia de las series.")
                WearMotionFeature(Icons.Filled.Check, "Sincronización en directo", "El progreso se mantiene actualizado entre el teléfono y el reloj.")
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(top = 8.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.accent),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text("Entendido", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun WearMotionFeature(icon: ImageVector, title: String, description: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Top) {
        Icon(icon, contentDescription = null, tint = WildforceThemeTokens.accent, modifier = Modifier.size(28.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.h6, color = WildforceThemeTokens.textPrimary)
            Text(description, style = MaterialTheme.typography.body2, color = WildforceThemeTokens.textSecondary)
        }
    }
}

@Composable
private fun ProfileOptionPickerDialog(title: String, options: List<String>, onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    androidx.compose.material.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontFamily = Exo2FontFamily, fontSize = 22.sp, fontWeight = FontWeight.SemiBold) },
        text = {
            LazyColumn(Modifier.height(if (options.size > 6) 380.dp else (options.size * 52).dp)) {
                items(options) { option ->
                    Text(option, Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { onSelect(option) }.padding(vertical = 14.dp), color = WildforceThemeTokens.textPrimary)
                }
            }
        },
        confirmButton = {},
    )
}

@Composable
private fun NutritionTextEditorDialog(field: NutritionTextField, profile: NutritionProfilePreferences, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var value by remember(field, profile) { mutableStateOf(field.value(profile)) }
    androidx.compose.material.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(field.title, fontFamily = Exo2FontFamily, fontSize = 22.sp, fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value, { value = it.take(500) }, Modifier.fillMaxWidth(), placeholder = { Text(field.prompt) }, minLines = if (field.isList) 2 else 4, maxLines = 6)
                if (field.isList) Text("Separa los elementos con comas.", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
            }
        },
        confirmButton = { Button(onClick = { onSave(value) }) { Text("GUARDAR") } },
        dismissButton = { Text("CANCELAR", Modifier.heightIn(min = 48.dp).clickable(onClick = onDismiss).padding(12.dp), color = WildforceThemeTokens.textSecondary) },
    )
}

private fun NutritionProfilePreferences.eatingWindowLabel(): String = eatingWindowStartHour?.let { start ->
    eatingWindowEndHour?.let { end -> "%02d:00 - %02d:00".format(start, end) }
} ?: "Flexible"

private fun List<String>.listSummary(): String = if (isEmpty()) "Ninguno" else joinToString(", ")

private fun String.profileDecimal(): String = filter { it.isDigit() || it == ',' || it == '.' }.take(6)
private fun String.profileDouble(): Double? = replace(',', '.').toDoubleOrNull()?.takeIf { it in 20.0..300.0 }

/** Filled Material counterparts for the SF Symbols used by iOS profile rows. */
private fun profileGoalIcon(goal: FitnessGoal): ImageVector = when (goal) {
    FitnessGoal.LoseWeight -> Icons.Filled.Sync
    FitnessGoal.BuildMuscle -> Icons.Filled.FitnessCenter
    FitnessGoal.GainStrength -> Icons.Filled.Bolt
    FitnessGoal.ImproveEndurance -> Icons.Filled.DirectionsRun
    FitnessGoal.ImproveMobility -> Icons.Filled.SelfImprovement
    FitnessGoal.BodyRecomposition -> Icons.Filled.AccessibilityNew
    FitnessGoal.GeneralFitness -> Icons.Filled.Favorite
}

private fun profileGymTypeIcon(type: GymType): ImageVector = when (type) {
    GymType.BigGym -> Icons.Filled.Apartment
    GymType.SmallGym -> Icons.Filled.Business
    GymType.HomeGym -> Icons.Filled.Home
    GymType.SomeAccessories -> Icons.Filled.FitnessCenter
    GymType.BodyweightOnly -> Icons.Filled.AccessibilityNew
}

private fun profileLifestyleIcon(lifestyle: LifestyleLevel): ImageVector = when (lifestyle) {
    LifestyleLevel.Sedentary -> Icons.Filled.Chair
    LifestyleLevel.LightlyActive -> Icons.Filled.AccessibilityNew
    LifestyleLevel.ModeratelyActive -> Icons.Filled.DirectionsWalk
    LifestyleLevel.VeryActive -> Icons.Filled.DirectionsRun
}

private sealed class Picker { data object Goal : Picker(); data object Level : Picker(); data object Lifestyle : Picker(); data object Split : Picker(); data object Body : Picker(); data object Gym : Picker(); data object Gender : Picker(); data object Metric : Picker(); data object Equipment : Picker(); data object Restrictions : Picker(); data class Focus(val day: WorkoutWeekday) : Picker() }

@Composable private fun PickerDialog(picker: Picker, profile: OnboardingProfile, onSelect: (OnboardingProfile) -> Unit) {
    val title: String
    val values: List<Pair<String, (OnboardingProfile) -> OnboardingProfile>>
    when (picker) {
        Picker.Goal -> { title = "Objetivo"; values = FitnessGoal.entries.map { it.title to { p: OnboardingProfile -> p.copy(goal = it) } } }
        Picker.Level -> { title = "Nivel"; values = TrainingLevel.entries.map { it.title to { p: OnboardingProfile -> p.copy(trainingLevel = it) } } }
        Picker.Lifestyle -> { title = "Actividad diaria"; values = LifestyleLevel.entries.map { it.title to { p: OnboardingProfile -> p.copy(lifestyle = it) } } }
        Picker.Split -> { title = "División semanal"; values = TrainingSplitPreference.entries.map { it.title to { p: OnboardingProfile -> p.copy(trainingSplitPreference = it) } } }
        Picker.Body -> { title = "Fase corporal"; values = BodyCompositionPhase.entries.map { it.title to { p: OnboardingProfile -> p.copy(bodyCompositionPhase = it) } } }
        Picker.Gym -> { title = "Tipo de gimnasio"; values = GymType.entries.map { it.title to { p: OnboardingProfile ->
            val locations = p.effectiveTrainingLocations().map { location -> if (location.isDefault) location.copy(equipment = it.defaultEquipment) else location }
            p.copy(gymType = it, availableEquipment = it.defaultEquipment, trainingLocations = locations)
        } } }
        Picker.Gender -> { title = "Sexo"; values = Gender.entries.map { it.title to { p: OnboardingProfile -> p.copy(gender = it) } } }
        Picker.Metric -> { title = "Unidades"; values = MetricSystem.entries.map { it.title to { p: OnboardingProfile -> p.copy(metricSystem = it) } } }
        Picker.Equipment, Picker.Restrictions -> { title = ""; values = emptyList() }
        is Picker.Focus -> { title = "Foco de ${picker.day.title}"; values = WorkoutFocus.customSelectionCases.map { it.title to { p: OnboardingProfile -> p.copy(customWorkoutFocuses = p.customWorkoutFocuses + (picker.day to it)) } } }
    }
            androidx.compose.material.AlertDialog(onDismissRequest = { onSelect(profile) }, title = { Text(title, fontFamily = AntonFontFamily) }, text = { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { values.forEach { (label, transform) -> Text(label, Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { onSelect(transform(profile)) }.padding(12.dp), color = WildforceThemeTokens.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis) } } }, confirmButton = {})
}

@Composable
private fun EquipmentPickerDialog(initial: Set<Equipment>, onSave: (Set<Equipment>) -> Unit, onDismiss: () -> Unit) {
    var selected by remember { mutableStateOf(initial) }
    androidx.compose.material.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("EQUIPAMIENTO", fontFamily = AntonFontFamily) },
        text = { LazyColumn(Modifier.height(360.dp)) { items(Equipment.entries) { equipment -> TogglePickerRow(equipment.title, equipment in selected) { selected = if (equipment in selected) selected - equipment else selected + equipment } } } },
        confirmButton = { Button(onClick = { onSave(selected); onDismiss() }) { Text("GUARDAR") } },
        dismissButton = { Text("CANCELAR", Modifier.heightIn(min = 48.dp).clickable(onClick = onDismiss).padding(12.dp), color = WildforceThemeTokens.textSecondary) },
    )
}

@Composable
private fun TrainingLocationsDialog(
    initial: List<TrainingLocationProfile>,
    onSave: (List<TrainingLocationProfile>) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var locations by remember(initial) { mutableStateOf(initial) }
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    var editorName by remember { mutableStateOf("") }
    var editorEquipment by remember { mutableStateOf(setOf<Equipment>()) }
    var editorAddress by remember { mutableStateOf("") }
    var editorLatitude by remember { mutableStateOf("") }
    var editorLongitude by remember { mutableStateOf("") }
    var editorPhotoPath by remember { mutableStateOf<String?>(null) }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { TrainingLocationPhotoStore.import(context, it) }?.let { editorPhotoPath = it }
    }
    val editing = editingIndex?.let(locations::getOrNull)
    val isEditing = editingIndex != null
    androidx.compose.material.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (!isEditing) "UBICACIONES" else if (editing == null) "AÑADIR UBICACIÓN" else "EDITAR UBICACIÓN", fontFamily = AntonFontFamily) },
        text = {
            if (!isEditing) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LazyColumn(Modifier.height(250.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        itemsIndexed(locations) { index, location ->
                            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).liquidGlass(RoundedCornerShape(12.dp)).clickable {
                                locations = locations.mapIndexed { itemIndex, item -> item.copy(isDefault = itemIndex == index) }
                            }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(location.name, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${location.equipment.size} elementos", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary, maxLines = 1)
                                }
                                Text(if (location.isDefault) "PRINCIPAL" else "EDITAR", Modifier.heightIn(min = 48.dp).clickable {
                                    editingIndex = locations.indexOf(location)
                                    editorName = location.name
                                    editorEquipment = location.equipment
                                    editorAddress = location.displayAddress.orEmpty()
                                    editorLatitude = location.latitude?.toString().orEmpty()
                                    editorLongitude = location.longitude?.toString().orEmpty()
                                    editorPhotoPath = location.photoPath
                                }.padding(6.dp), style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.accent)
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable {
                        editingIndex = -1
                        editorName = ""
                        editorEquipment = setOf(Equipment.Bodyweight)
                        editorAddress = ""
                        editorLatitude = ""
                        editorLongitude = ""
                        editorPhotoPath = null
                    }.padding(10.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp), tint = WildforceThemeTokens.accent)
                        Text("AÑADIR UBICACIÓN", Modifier.padding(start = 6.dp), color = WildforceThemeTokens.accent, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                    Text("Toca una ubicación para marcarla como principal. Su equipamiento será el usado al generar planes.", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(editorName, { editorName = it.take(50) }, Modifier.fillMaxWidth(), label = { Text("Nombre") }, singleLine = true)
                    OutlinedTextField(editorAddress, { editorAddress = it.take(200) }, Modifier.fillMaxWidth(), label = { Text("Dirección") }, singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(editorLatitude, { editorLatitude = it.take(20) }, Modifier.weight(1f), label = { Text("Latitud") }, singleLine = true)
                        OutlinedTextField(editorLongitude, { editorLongitude = it.take(20) }, Modifier.weight(1f), label = { Text("Longitud") }, singleLine = true)
                    }
                    TextButton(onClick = { photoPicker.launch("image/*") }) {
                        Icon(Icons.Filled.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(if (editorPhotoPath == null) "AÑADIR FOTO DEL LUGAR" else "CAMBIAR FOTO DEL LUGAR", Modifier.padding(start = 8.dp))
                    }
                    LazyColumn(Modifier.height(280.dp)) {
                        items(Equipment.entries) { equipment ->
                            TogglePickerRow(equipment.title, equipment in editorEquipment) {
                                editorEquipment = if (equipment in editorEquipment) editorEquipment - equipment else editorEquipment + equipment
                            }
                        }
                    }
                    if (editing != null && locations.size > 1) {
                        Text("ELIMINAR UBICACIÓN", Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable {
                            val removedIndex = editingIndex ?: return@clickable
                            val remaining = locations.filterIndexed { index, _ -> index != removedIndex }
                            locations = if (remaining.any { it.isDefault }) remaining else remaining.mapIndexed { index, location -> location.copy(isDefault = index == 0) }
                            editingIndex = null
                        }.padding(10.dp), color = Color(0xFFC62828), fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (!isEditing) onSave(locations)
                else {
                    val updated = TrainingLocationProfile(
                        name = editorName.trim().ifBlank { "Ubicación ${locations.size + 1}" },
                        equipment = editorEquipment.ifEmpty { setOf(Equipment.Bodyweight) },
                        isDefault = editing?.isDefault == true || locations.isEmpty(),
                        displayAddress = editorAddress.trim().ifBlank { null },
                        latitude = editorLatitude.replace(',', '.').toDoubleOrNull()?.takeIf { it in -90.0..90.0 },
                        longitude = editorLongitude.replace(',', '.').toDoubleOrNull()?.takeIf { it in -180.0..180.0 },
                        photoPath = editorPhotoPath,
                    )
                    locations = if (editingIndex == -1) {
                        if (locations.any { it.isDefault }) locations + updated else locations + updated.copy(isDefault = true)
                    } else locations.mapIndexed { index, location -> if (index == editingIndex) updated else location }
                    editingIndex = null
                }
            }) { Text(if (!isEditing) "GUARDAR" else "LISTO") }
        },
        dismissButton = {
            Text(if (!isEditing) "CANCELAR" else "VOLVER", Modifier.heightIn(min = 48.dp).clickable {
                if (!isEditing) onDismiss() else editingIndex = null
            }.padding(12.dp), color = WildforceThemeTokens.textSecondary)
        },
    )
}

@Composable
private fun RestrictionsPickerDialog(initial: Set<MovementRestriction>, onSave: (Set<MovementRestriction>) -> Unit, onDismiss: () -> Unit) {
    var selected by remember { mutableStateOf(initial) }
    androidx.compose.material.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("RESTRICCIONES", fontFamily = AntonFontFamily) },
        text = { LazyColumn(Modifier.height(320.dp)) { items(MovementRestriction.entries) { restriction -> TogglePickerRow(restriction.title, restriction in selected) { selected = if (restriction in selected) selected - restriction else selected + restriction } } } },
        confirmButton = { Button(onClick = { onSave(selected); onDismiss() }) { Text("GUARDAR") } },
        dismissButton = { Text("CANCELAR", Modifier.heightIn(min = 48.dp).clickable(onClick = onDismiss).padding(12.dp), color = WildforceThemeTokens.textSecondary) },
    )
}

@Composable
private fun TogglePickerRow(label: String, selected: Boolean, onToggle: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(onClick = onToggle).padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = WildforceThemeTokens.textPrimary)
        if (selected) Icon(Icons.Filled.Check, contentDescription = "Seleccionado", tint = WildforceThemeTokens.accent)
    }
}

@Composable private fun Section(title: String, content: @Composable () -> Unit) { Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(title, fontFamily = Exo2FontFamily, fontSize = 18.sp, color = WildforceThemeTokens.textPrimary); Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(WildforceThemeTokens.backgroundSecondary).padding(horizontal = 14.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { content() } } }
@Composable private fun ChoiceButton(label: String, value: String, description: String? = null, icon: ImageVector? = null, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) .97f else 1f, spring(dampingRatio = .7f, stiffness = 700f), label = "profile-choice-press")
    val haptics = LocalHapticFeedback.current
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).graphicsLayer { scaleX = scale; scaleY = scale }.clickable(interactionSource = interaction, indication = null, onClick = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); onClick() }).semantics { contentDescription = "$label, $value"; role = Role.Button }.padding(vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { icon?.let { Icon(it, contentDescription = null, modifier = Modifier.padding(end = 12.dp).size(20.dp), tint = WildforceThemeTokens.textSecondary) }; Column(Modifier.weight(1f)) { Text(label, color = WildforceThemeTokens.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis); description?.let { Text(it, Modifier.padding(top = 3.dp), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary, maxLines = 3, overflow = TextOverflow.Ellipsis) } }; Text(value, Modifier.padding(start = 10.dp).weight(.85f), color = WildforceThemeTokens.textSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.End, maxLines = 2, overflow = TextOverflow.Ellipsis); Icon(Icons.Filled.ArrowForward, contentDescription = null, modifier = Modifier.padding(start = 8.dp).size(18.dp), tint = WildforceThemeTokens.textSecondary) }
}
@Composable
private fun NumberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier,
    decimal: Boolean = false,
    leadingIcon: ImageVector? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = { Text(label) },
        leadingIcon = leadingIcon?.let { icon -> { Icon(icon, contentDescription = null, tint = WildforceThemeTokens.textSecondary) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
        colors = TextFieldDefaults.outlinedTextFieldColors(
            textColor = WildforceThemeTokens.textPrimary,
            cursorColor = WildforceThemeTokens.accent,
            focusedBorderColor = WildforceThemeTokens.accent,
            unfocusedBorderColor = WildforceThemeTokens.textSecondary.copy(alpha = 0.55f),
            focusedLabelColor = WildforceThemeTokens.accent,
            unfocusedLabelColor = WildforceThemeTokens.textSecondary,
            backgroundColor = Color.Transparent,
        ),
    )
}
@Composable private fun TogglePreference(label: String, description: String, selected: Boolean, icon: ImageVector? = null, onClick: () -> Unit) { val haptics = LocalHapticFeedback.current; Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(onClick = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); onClick() }).semantics { contentDescription = "$label, $description"; role = Role.Switch }.padding(vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { icon?.let { Icon(it, contentDescription = null, modifier = Modifier.padding(end = 12.dp).size(20.dp), tint = WildforceThemeTokens.textSecondary) }; Column(Modifier.weight(1f)) { Text(label, color = WildforceThemeTokens.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis); Text(description, Modifier.padding(top = 3.dp), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary, maxLines = 3, overflow = TextOverflow.Ellipsis) }; androidx.compose.material.Switch(checked = selected, onCheckedChange = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); onClick() }, modifier = Modifier.padding(start = 10.dp), colors = androidx.compose.material.SwitchDefaults.colors(checkedThumbColor = WildforceThemeTokens.accent, checkedTrackColor = WildforceThemeTokens.accent.copy(alpha = .32f))) } }

private const val PoundsPerKilogram = 2.2046226218

private fun displayHeight(heightCm: Int, system: MetricSystem): String = when (system) {
    MetricSystem.Metric -> heightCm.toString()
    MetricSystem.Imperial -> (heightCm / 2.54).roundToInt().toString()
}

private fun formatHeightChoice(value: String, system: MetricSystem): String {
    val displayed = value.toIntOrNull() ?: return value
    return when (system) {
        MetricSystem.Metric -> "$displayed cm"
        MetricSystem.Imperial -> "${displayed / 12}′ ${displayed % 12}″"
    }
}

private fun displayWeight(weightKg: Double, system: MetricSystem): String = when (system) {
    MetricSystem.Metric -> "%.1f".format(Locale.US, weightKg)
    MetricSystem.Imperial -> "%.1f".format(Locale.US, weightKg * PoundsPerKilogram)
}

private fun validatedProfile(draft: OnboardingProfile, height: String, weight: String, duration: String, birthYear: String = draft.birthYear.toString(), birthDay: String = draft.birthDay.toString()): OnboardingProfile {
    val validBirthYear = birthYear.toIntOrNull()
        ?.takeIf { it in 1920..Year.now().value }
        ?: draft.birthYear
    val validBirthDay = birthDay.toIntOrNull()
        ?.takeIf { it in 1..daysInBirthMonth(validBirthYear, draft.birthMonth) }
        ?: draft.birthDay
    return draft.copy(
    // These fields are persisted while the user types. Do not coerce a partial
    // value to a boundary: deleting a digit from 1995 previously saved 1920,
    // and deleting from weight/height could likewise save their minimums.
    // Until the input is a complete value in range, retain the stored value.
    heightCm = height.toIntOrNull()
        ?.let { entered -> if (draft.metricSystem == MetricSystem.Imperial) (entered * 2.54).roundToInt() else entered }
        ?.takeIf { it in 120..230 }
        ?: draft.heightCm,
    weightKg = weight.replace(',', '.').toDoubleOrNull()
        ?.let { entered -> if (draft.metricSystem == MetricSystem.Imperial) entered / PoundsPerKilogram else entered }
        ?.takeIf { it in 35.0..250.0 }
        ?: draft.weightKg,
    preferredWorkoutDurationMinutes = duration.toIntOrNull()
        ?.takeIf { it in 15..180 }
        ?: draft.preferredWorkoutDurationMinutes,
    birthYear = validBirthYear,
    birthDay = validBirthDay,
    )
}

private fun monthName(month: Int): String = Month.of(month.coerceIn(1, 12)).getDisplayName(TextStyle.FULL, Locale.forLanguageTag("es-ES")).replaceFirstChar { it.titlecase(Locale.forLanguageTag("es-ES")) }
