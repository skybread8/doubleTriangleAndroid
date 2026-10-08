package io.codepassion.doubletriangle.feature.onboarding

import android.graphics.BitmapFactory

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Icon
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Help
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Year
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.log10
import io.codepassion.doubletriangle.core.designsystem.AntonFontFamily
import io.codepassion.doubletriangle.core.designsystem.Exo2FontFamily
import io.codepassion.doubletriangle.core.designsystem.WildforceThemeTokens
import io.codepassion.doubletriangle.core.designsystem.liquidGlass
import io.codepassion.doubletriangle.core.designsystem.liquidGlassBackground
import io.codepassion.doubletriangle.feature.workout.WildforceWatchLocalBridge

/** Editable fitness profile. These are the same inputs consumed by the iOS planner. */
@Composable
fun ProfileScreen(
    initial: OnboardingProfile,
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
    onRequestHealthConnect: (((Boolean, Int?, Double?) -> Unit) -> Unit) = { _ -> },
    onSave: (OnboardingProfile) -> Unit,
    onRegenerate: (OnboardingProfile) -> Unit = {},
    onChangeAvatar: () -> Unit = {},
    onThemeChanged: () -> Unit = {},
    onKeepScreenOnChanged: (Boolean) -> Unit = {},
    onGenerateFuturePreview: suspend (android.graphics.Bitmap, OnboardingProfile) -> android.graphics.Bitmap = { _, _ -> error("La vista a futuro no está configurada") },
) {
    var draft by remember(initial) { mutableStateOf(initial) }
    var heightInput by remember(initial) { mutableStateOf(displayHeight(initial.heightCm, initial.metricSystem)) }
    var weightInput by remember(initial) { mutableStateOf(displayWeight(initial.weightKg, initial.metricSystem)) }
    var durationInput by remember(initial) { mutableStateOf(initial.preferredWorkoutDurationMinutes.toString()) }
    var birthYearInput by remember(initial) { mutableStateOf(initial.birthYear.toString()) }
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
    val detailPreferences = remember(profileContext) { profileContext.getSharedPreferences("wildforce_profile_details", 0) }
    var appLanguage by remember { mutableStateOf(detailPreferences.getString("language", "Español") ?: "Español") }
    var appTheme by remember { mutableStateOf(detailPreferences.getString("theme", "Sistema") ?: "Sistema") }
    var keepScreenOn by remember { mutableStateOf(detailPreferences.getBoolean("keep_screen_on", true)) }
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
    var accountRevision by remember { mutableStateOf(0) }
    var showsBodyProgress by remember { mutableStateOf(false) }
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
            onSave = { updatedProfile, updatedComposition, leanBodyMass, bodyFat ->
                draft = updatedProfile
                heightInput = displayHeight(updatedProfile.heightCm, updatedProfile.metricSystem)
                weightInput = displayWeight(updatedProfile.weightKg, updatedProfile.metricSystem)
                bodyComposition = updatedComposition
                ProfileDetailPreferencesStore.saveBodyComposition(profileContext, updatedComposition)
                BodyMetricsStore.record(profileContext, updatedProfile, updatedComposition, leanBodyMass, bodyFat)
                onSave(updatedProfile)
            },
        )
        return
    }
    if (selectedSection == ProfileSection.Body && showsBodyProgress) {
        ProgressPhotoAccessGate(locked = progressPhotoLockEnabled, onClose = { showsBodyProgress = false }) {
            BodyProgressScreen(
                profile = validatedProfile(draft, heightInput, weightInput, durationInput, birthYearInput),
                generateFuturePreview = onGenerateFuturePreview,
                onClose = { showsBodyProgress = false },
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
        profileContext.getSharedPreferences("wildforce_profile", 0)
            .edit().putBoolean("remote_sync_pending", true).apply()
    }
    LaunchedEffect(Unit) { entered = true }
    LaunchedEffect(draft, heightInput, weightInput, durationInput, birthYearInput) {
        saveProfile(validatedProfile(draft, heightInput, weightInput, durationInput, birthYearInput))
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().liquidGlassBackground().padding(horizontal = 16.dp),
        // The menu stays over the canvas; reserve room inside the scroll so the
        // final profile setting can be brought above it (and the resume bar).
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 16.dp, bottom = contentPadding.calculateBottomPadding() + 160.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (selectedSection != null) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(44.dp).clip(CircleShape).background(WildforceThemeTokens.surfaceElevated).clickable { selectedSection = null }, contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = WildforceThemeTokens.textPrimary, modifier = Modifier.size(22.dp))
                    }
                    Text(selectedSection?.title.orEmpty(), fontFamily = Exo2FontFamily, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textPrimary)
                }
            }
        }
        if (selectedSection == null) { item { ProfileEntrance(entered, 0) { ProfileHero(draft, currentStreak, completedWorkouts, longestStreak, avatarPath, avatarRevision, onChangeAvatar) } } }
        if (selectedSection == null) { item { ProfileEntrance(entered, 35) { ProfileExperience(experienceXp, experienceLevel, experienceProgress) } } }
        if (selectedSection == null) { item { ProfileCategoryTree(draft, nutritionProfile, appLanguage, accountRevision) { selectedSection = it } } }
        // `achievementsSection` is intentionally disabled in the canonical iOS
        // profile view.  Do not render a placeholder rail here: it changes the
        // hierarchy and pushes the four profile destinations below the fold.
        if (selectedSection == ProfileSection.Body) { item { Section("Cuerpo") {
                ChoiceButton("Sexo", draft.gender.title, icon = Icons.Filled.Person) { picker = Picker.Gender }
                ChoiceButton("Fecha de nacimiento", "${monthName(draft.birthMonth)} $birthYearInput", icon = Icons.Filled.Person) { picker = Picker.BirthMonth }
                NumberField("Año de nacimiento", birthYearInput, { birthYearInput = it.filter(Char::isDigit).take(4) }, Modifier.fillMaxWidth())
                NumberField(if (draft.metricSystem == MetricSystem.Imperial) "Altura (in)" else "Altura (cm)", heightInput, { heightInput = it.filter(Char::isDigit).take(3) }, Modifier.fillMaxWidth())
                ChoiceButton("Métricas corporales", bodyMetrics.lastOrNull()?.let { displayWeight(it.weightKg, draft.metricSystem) + if (draft.metricSystem == MetricSystem.Imperial) " lb" else " kg" } ?: "Sin registros", icon = Icons.Filled.FitnessCenter) { showsBodyMetrics = true }
                ChoiceButton("Fotos de progreso corporal", BodyProgressStore.load(profileContext).size.let { if (it == 0) "Sin fotos todavía" else "$it check-ins" }, icon = Icons.Filled.PhotoCamera) { showsBodyProgress = !showsBodyProgress }
            }
        } }
        if (selectedSection == ProfileSection.Body) { item {
            // Detail sections are rendered directly once selected. Keeping them out of
            // AnimatedVisibility avoids a LazyColumn re-measure race when switching
            // from the category tree to an editor with focused text fields.
            ProfileBodyMetrics(
                profile = validatedProfile(draft, heightInput, weightInput, durationInput, birthYearInput),
                measurements = bodyComposition,
            )
        } }
        if (selectedSection == ProfileSection.Fitness) { item { ProfileEntrance(entered, 105) { Section("Entrenamiento") {
                ChoiceButton("Objetivo", draft.goal.title, icon = Icons.Filled.FitnessCenter) { picker = Picker.Goal }
                ChoiceButton("Estilo de vida", draft.lifestyle.title, draft.lifestyle.description, Icons.Filled.Person) { picker = Picker.Lifestyle }
                ChoiceButton("Nivel de entrenamiento", draft.trainingLevel.title, draft.trainingLevel.description, Icons.Filled.FitnessCenter) { picker = Picker.Level }
                if (draft.goal.supportsBodyComposition && draft.trainingLevel.supportsBodyComposition) ChoiceButton("Intención de composición corporal", draft.bodyCompositionPhase?.title ?: "Automático", icon = Icons.Filled.Person) { picker = Picker.Body }
            } } } }
        if (selectedSection == ProfileSection.Fitness) { item { ProfileEntrance(entered, 155) { Section("Configuración de entrenamiento") {
                ChoiceButton("Configuración", draft.trainingSplitPreference.title) { picker = Picker.Split }
                Text("Días de entrenamiento", fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
                draft.workoutDays.toList().let { days ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        WorkoutWeekday.entries.forEach { day ->
                            val selected = day in draft.workoutDays
                            Text(day.localizedInitial(appLanguage), modifier = Modifier.size(38.dp).background(if (selected) WildforceThemeTokens.textPrimary else WildforceThemeTokens.textSecondary.copy(alpha = .12f), RoundedCornerShape(12.dp)).clickable { draft = draft.copy(workoutDays = if (selected && draft.workoutDays.size > 1) draft.workoutDays - day else draft.workoutDays + day).let { it } }.padding(10.dp), color = if (selected) WildforceThemeTokens.backgroundSecondary else WildforceThemeTokens.textPrimary)
                        }
                    }
                }
                NumberField("Duración preferida (min)", durationInput, { durationInput = it.filter(Char::isDigit).take(3) }, Modifier.fillMaxWidth())
                draft.workoutDays.sortedBy { it.ordinal }.forEach { day ->
                    if (draft.trainingSplitPreference == TrainingSplitPreference.Custom) ChoiceButton(day.title, draft.customWorkoutFocuses[day]?.title ?: "Seleccionar foco") { picker = Picker.Focus(day) }
                }
                TogglePreference("Omitir calentamientos", "Oculta los bloques de activación y ajusta la duración objetivo.", draft.skipsWarmups, Icons.Filled.FitnessCenter) { draft = draft.copy(skipsWarmups = !draft.skipsWarmups) }
                TogglePreference("Omitir enfriamientos", "Oculta los bloques finales y mantiene el plan dentro de la duración objetivo.", draft.skipsCooldowns, Icons.Filled.FitnessCenter) { draft = draft.copy(skipsCooldowns = !draft.skipsCooldowns) }
                TogglePreference("Omitir periodos de descanso", "Salta directamente a la siguiente serie o ejercicio durante el entrenamiento.", draft.skipsRestPeriods, Icons.Filled.FitnessCenter) { draft = draft.copy(skipsRestPeriods = !draft.skipsRestPeriods) }
                OutlinedTextField(draft.workoutPlannerNotes, { draft = draft.copy(workoutPlannerNotes = it.take(500)) }, Modifier.fillMaxWidth(), label = { Text("Notas para el planificador") }, minLines = 2, maxLines = 4)
            } } } }
        if (selectedSection == ProfileSection.Fitness) { item { ProfileEntrance(entered, 205) { Section("Entorno") {
                ChoiceButton("Tipo de gimnasio", draft.gymType.title) { picker = Picker.Gym }
                ChoiceButton("Ubicaciones de entrenamiento", draft.effectiveTrainingLocations().joinToString(" · ") { if (it.isDefault) "${it.name} (principal)" else it.name }) { showsTrainingLocations = true }
                ChoiceButton("Equipamiento disponible", "${draft.availableEquipment.size} seleccionado(s)") { picker = Picker.Equipment }
                ChoiceButton("Restricciones o molestias", draft.movementRestrictions.takeIf { it.isNotEmpty() }?.size?.let { "$it seleccionado(s)" } ?: "Ninguna") { picker = Picker.Restrictions }
            } } } }
        if (selectedSection == ProfileSection.Nutrition) { item { ProfileEntrance(entered, 235) {
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
        if (selectedSection == ProfileSection.Nutrition && nutritionProfile.isConfigured) { item { ProfileEntrance(entered, 255) { Section("Preferencias") {
                ChoiceButton("Esfuerzo al cocinar", nutritionProfile.cookingEffort, icon = Icons.Filled.LocalFireDepartment) { nutritionPicker = NutritionPicker.CookingEffort }
                ChoiceButton("Sensibilidad al presupuesto", nutritionProfile.budgetSensitivity, icon = Icons.Filled.Settings) { nutritionPicker = NutritionPicker.BudgetSensitivity }
                ChoiceButton("Fuentes de proteína", nutritionProfile.preferredProteinSources.listSummary(), icon = Icons.Filled.Restaurant) { nutritionTextEditor = NutritionTextField.ProteinSources }
                ChoiceButton("Alimentos que no te gustan", nutritionProfile.dislikes.listSummary(), icon = Icons.Filled.Restaurant) { nutritionTextEditor = NutritionTextField.Dislikes }
            } } } }
        if (selectedSection == ProfileSection.Nutrition && nutritionProfile.isConfigured) { item { ProfileEntrance(entered, 275) { Section("Restricciones") {
                ChoiceButton("Alimentos excluidos", nutritionProfile.excludedFoods.listSummary(), icon = Icons.Filled.Restaurant) { nutritionTextEditor = NutritionTextField.ExcludedFoods }
                ChoiceButton("Alergias o intolerancias", nutritionProfile.allergiesAndIntolerances.listSummary(), icon = Icons.Filled.Restaurant) { nutritionTextEditor = NutritionTextField.Allergies }
            } } } }
        if (selectedSection == ProfileSection.Nutrition && nutritionProfile.isConfigured) { item { ProfileEntrance(entered, 295) { Section("Notas") {
                ChoiceButton("Notas", nutritionProfile.notes.ifBlank { "Ninguna" }, icon = Icons.Filled.Settings) { nutritionTextEditor = NutritionTextField.Notes }
            } } } }
        if (selectedSection == ProfileSection.Settings) { item { ProfileEntrance(entered, 245) { Section("Preferencias") {
                ChoiceButton("Idioma", appLanguage, icon = Icons.Filled.Settings) { settingsPicker = SettingsPicker.Language }
                ChoiceButton("Sistema métrico", draft.metricSystem.title, icon = Icons.Filled.Settings) { picker = Picker.Metric }
                ChoiceButton("Tema", appTheme, icon = Icons.Filled.Settings) { settingsPicker = SettingsPicker.Theme }
            } } } }
        if (selectedSection == ProfileSection.Settings) { item { ProfileEntrance(entered, 265) { Section("Entorno") {
                ChoiceButton("Preguntas frecuentes", "Respuestas sobre entrenamiento, nutrición, Android y tu cuenta.", icon = Icons.Filled.Help) { selectedSection = ProfileSection.Faq }
                TogglePreference("Health Connect", "Guarda entrenamientos y sincroniza medidas corporales con Health Connect.", draft.isHealthConnectEnabled, Icons.Filled.FitnessCenter) {
                    if (draft.isHealthConnectEnabled) {
                        draft = draft.copy(isHealthConnectEnabled = false)
                        saveProfile(draft)
                        healthConnectMessage = "Health Connect se ha desactivado para Wildforce."
                    } else onRequestHealthConnect { granted, importedHeight, importedWeight ->
                        if (granted) {
                            importedHeight?.let { heightInput = displayHeight(it, draft.metricSystem) }
                            importedWeight?.let { weightInput = displayWeight(it, draft.metricSystem) }
                            draft = draft.copy(isHealthConnectEnabled = true, heightCm = importedHeight ?: draft.heightCm, weightKg = importedWeight ?: draft.weightKg)
                            saveProfile(draft)
                            healthConnectMessage = if (importedHeight != null || importedWeight != null) "Medidas importadas desde Health Connect." else "Health Connect está conectado."
                        } else healthConnectMessage = "No se concedió el acceso a Health Connect."
                    }
                }
                healthConnectMessage?.let { Text(it, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary) }
                TogglePreference("Recordatorios", "Recibe recordatorios de próximos entrenamientos y comidas pendientes de registrar.", reminderNotificationsEnabled, Icons.Filled.Settings) {
                    reminderNotificationsEnabled = !reminderNotificationsEnabled
                    notificationPreferences.edit().putBoolean("reminders", reminderNotificationsEnabled).apply()
                }
                TogglePreference("Mantener pantalla encendida durante el entrenamiento", "Evita que la pantalla se bloquee mientras hay un entrenamiento activo abierto.", keepScreenOn, Icons.Filled.Settings) {
                    keepScreenOn = !keepScreenOn
                    detailPreferences.edit().putBoolean("keep_screen_on", keepScreenOn).apply()
                    onKeepScreenOnChanged(keepScreenOn)
                }
                TogglePreference("Modo concentración total", "Reduce las distracciones durante los entrenamientos mediante los ajustes de concentración de Android.", fullFocusEnabled, Icons.Filled.Settings) {
                    fullFocusEnabled = !fullFocusEnabled
                    detailPreferences.edit().putBoolean("full_focus", fullFocusEnabled).apply()
                    if (fullFocusEnabled) runCatching { profileContext.startActivity(android.content.Intent("android.settings.ZEN_MODE_SETTINGS")) }
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
                    notificationsEnabled = !notificationsEnabled
                    notificationPreferences.edit().putBoolean("enabled", notificationsEnabled).apply()
                }
                TogglePreference("Reloj Amazfit", "Sincroniza el entrenamiento activo con Wildforce en tu Amazfit. Solo se activa durante el workout.", amazfitWatchEnabled, Icons.Filled.Settings) {
                    amazfitWatchEnabled = !amazfitWatchEnabled
                    WildforceWatchLocalBridge.setEnabled(profileContext, amazfitWatchEnabled)
                }
                TogglePreference("Avisos al comenzar el descanso", "Emite un aviso cuando empieza el temporizador de descanso.", restAlertsEnabled, Icons.Filled.Settings) {
                    restAlertsEnabled = !restAlertsEnabled
                    notificationPreferences.edit().putBoolean("rest_alerts", restAlertsEnabled).apply()
                }
            } } } }
        if (selectedSection == ProfileSection.Settings) { item { ProfileEntrance(entered, 285) { Section("Privacidad") {
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
                onAccountChanged = { accountRevision++ },
            )
        } }
        if (selectedSection == ProfileSection.Account) { item {
            AccountSection(
                profile = draft,
                destination = AccountProfileDestination.Account,
                onProfileSynchronized = { remoteProfile -> draft = remoteProfile; saveProfile(remoteProfile) },
                onAccountChanged = { accountRevision++ },
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
                CircularProgressIndicator(color = WildforceThemeTokens.accentGold)
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
}

@Composable
private fun ProfileEntrance(visible: Boolean, delayMillis: Int, content: @Composable () -> Unit) {
    AnimatedVisibility(visible = visible, enter = fadeIn(tween(330, delayMillis = delayMillis)) + slideInVertically(tween(330, delayMillis = delayMillis)) { it / 16 }) { content() }
}

private enum class ProfileSection(val title: String, val icon: ImageVector) {
    Body("Perfil corporal", Icons.Filled.Person),
    Fitness("Perfil de fitness", Icons.Filled.FitnessCenter),
    Nutrition("Perfil nutricional", Icons.Filled.Restaurant),
    Settings("Ajustes", Icons.Filled.Settings),
    Subscription("Suscripción", Icons.Filled.Star),
    Account("Cuenta", Icons.Filled.Person),
    Faq("Preguntas frecuentes", Icons.Filled.Help),
}

@Composable
private fun ProfileCategoryTree(profile: OnboardingProfile, nutritionProfile: NutritionProfilePreferences, appLanguage: String, accountRevision: Int, onSelect: (ProfileSection) -> Unit) {
    val context = LocalContext.current
    val account = remember(accountRevision) { currentWildforceAccount(context) }
    val summaries = mapOf(
        ProfileSection.Body to displayWeight(profile.weightKg, profile.metricSystem).let { "$it ${if (profile.metricSystem == MetricSystem.Imperial) "lb" else "kg"}" },
        ProfileSection.Fitness to "${profile.goal.title} • ${profile.effectiveTrainingLocations().firstOrNull { it.isDefault }?.name ?: profile.gymType.title}",
        ProfileSection.Nutrition to if (nutritionProfile.isConfigured) "${nutritionProfile.dietaryStyle} • ${nutritionProfile.mealsPerDay} comidas" else "Configuración necesaria",
        ProfileSection.Settings to "$appLanguage • ${profile.metricSystem.title}",
        ProfileSection.Subscription to "Gestionar plan",
        ProfileSection.Account to (account?.email ?: "Configuración necesaria"),
    )
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(WildforceThemeTokens.backgroundSecondary)) {
        val visibleSections = ProfileSection.entries.filter { it != ProfileSection.Faq }
        visibleSections.forEachIndexed { index, section ->
            Row(
                Modifier.fillMaxWidth().clickable { onSelect(section) }.padding(horizontal = 16.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(section.icon, contentDescription = null, modifier = Modifier.size(21.dp), tint = WildforceThemeTokens.textPrimary)
                Text(section.title, Modifier.weight(1f), fontFamily = Exo2FontFamily, fontSize = 16.sp, color = WildforceThemeTokens.textPrimary)
                Text(summaries.getValue(section), Modifier.weight(1f), style = MaterialTheme.typography.body2, color = WildforceThemeTokens.textSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.End, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("›", color = WildforceThemeTokens.textSecondary, fontSize = 22.sp)
            }
            if (index < visibleSections.lastIndex) {
                Box(Modifier.fillMaxWidth().padding(start = 49.dp).height(1.dp).background(WildforceThemeTokens.textSecondary.copy(alpha = 0.16f)))
            }
        }
    }
}

private enum class BodyMetricKind(val title: String, val unit: String) {
    Weight("Peso", "kg"),
    LeanBodyMass("Masa corporal magra", "kg"),
    Waist("Cintura", "cm"),
    Neck("Cuello", "cm"),
    Hip("Cadera", "cm"),
    BodyFat("Grasa corporal", "%"),
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
    onSave: (OnboardingProfile, BodyCompositionMeasurements, Double?, Double?) -> Unit,
) {
    var selected by remember { mutableStateOf(BodyMetricKind.Weight) }
    var timeframeDays by remember { mutableStateOf(30) }
    var metricHistory by remember(history) { mutableStateOf(history) }
    var input by remember(selected, profile, composition) {
        mutableStateOf(bodyMetricDisplayValue(selected, profile, composition, history.lastOrNull()))
    }
    val entries = remember(metricHistory, selected) { metricHistory.filter { it.valueFor(selected) != null } }
    val visibleEntries = remember(entries, timeframeDays) {
        if (timeframeDays == Int.MAX_VALUE) entries else {
            val cutoff = java.time.LocalDate.now().minusDays(timeframeDays.toLong()).toString()
            entries.filter { it.date >= cutoff }
        }
    }
    val latest = entries.lastOrNull()?.valueFor(selected)
    LazyColumn(
        modifier = Modifier.fillMaxSize().liquidGlassBackground().padding(horizontal = 16.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 16.dp, bottom = 160.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(44.dp).clip(CircleShape).background(WildforceThemeTokens.backgroundSecondary).clickable(onClick = onClose), contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = WildforceThemeTokens.textPrimary, modifier = Modifier.size(22.dp))
                }
                Text("MÉTRICAS CORPORALES", fontFamily = Exo2FontFamily, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BodyMetricKind.entries.chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { kind ->
                            val isSelected = kind == selected
                            Box(
                                Modifier.weight(1f).clip(RoundedCornerShape(16.dp))
                                    .background(if (isSelected) WildforceThemeTokens.accent else WildforceThemeTokens.backgroundSecondary)
                                    .clickable { selected = kind }.padding(12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(kind.title, style = MaterialTheme.typography.caption, fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) WildforceThemeTokens.primaryButtonText else WildforceThemeTokens.textPrimary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center, maxLines = 2)
                        }
                    }
                    }
                    if (row.size == 1) Spacer(Modifier.height(0.dp))
                }
            }
        }
        item {
            Section("Progreso de ${selected.title.lowercase()}") {
                Text(latest?.let { formatBodyMetric(selected, it, profile.metricSystem) } ?: "—", fontFamily = Exo2FontFamily, fontSize = 30.sp, color = WildforceThemeTokens.textPrimary)
                Text("Último valor registrado", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(7 to "7 días", 30 to "30 días", Int.MAX_VALUE to "Todo").forEach { (days, label) ->
                        val active = timeframeDays == days
                        Text(label, Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                            .background(if (active) WildforceThemeTokens.textPrimary else WildforceThemeTokens.textSecondary.copy(alpha = .12f))
                            .clickable { timeframeDays = days }.padding(vertical = 8.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            style = MaterialTheme.typography.caption, color = if (active) WildforceThemeTokens.backgroundSecondary else WildforceThemeTokens.textPrimary)
                    }
                }
                if (visibleEntries.isEmpty()) {
                    Text("Aún no hay registros para este periodo.", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
                } else {
                    val values = visibleEntries.mapNotNull { it.valueFor(selected) }
                    val min = values.minOrNull() ?: 0.0
                    val max = (values.maxOrNull() ?: min).let { if (it == min) min + 1 else it }
                    visibleEntries.takeLast(8).forEach { entry ->
                        val value = entry.valueFor(selected) ?: return@forEach
                        val fraction = ((value - min) / (max - min)).toFloat().coerceIn(.04f, 1f)
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(entry.date.takeLast(5), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary, modifier = Modifier.size(width = 44.dp, height = 20.dp))
                            Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(8.dp)).background(WildforceThemeTokens.textSecondary.copy(alpha = .12f))) {
                                Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().background(WildforceThemeTokens.accent))
                            }
                            Text(formatBodyMetric(selected, value, profile.metricSystem), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textPrimary)
                        }
                    }
                }
            }
        }
        item {
            Section("Registrar ${selected.title.lowercase()}") {
                NumberField("${selected.title} (${displayBodyMetricUnit(selected, profile.metricSystem)})", input, { input = it.profileDecimal() }, Modifier.fillMaxWidth(), decimal = true)
                Button(
                    onClick = {
                        val canonical = canonicalBodyMetric(selected, input.profileDouble(), profile.metricSystem) ?: return@Button
                        var updatedProfile = profile
                        var updatedComposition = composition
                        var leanBodyMass: Double? = null
                        var bodyFat: Double? = null
                        when (selected) {
                            BodyMetricKind.Weight -> updatedProfile = profile.copy(weightKg = canonical.coerceIn(35.0, 250.0))
                            BodyMetricKind.LeanBodyMass -> leanBodyMass = canonical
                            BodyMetricKind.Waist -> updatedComposition = composition.copy(waistCm = canonical)
                            BodyMetricKind.Neck -> updatedComposition = composition.copy(neckCm = canonical)
                            BodyMetricKind.Hip -> updatedComposition = composition.copy(hipCm = canonical)
                            BodyMetricKind.BodyFat -> bodyFat = canonical
                        }
                        onSave(updatedProfile, updatedComposition, leanBodyMass, bodyFat)
                        val today = java.time.LocalDate.now().toString()
                        metricHistory = metricHistory.filterNot { it.date == today } + BodyMetricEntry(
                            date = today,
                            heightCm = updatedProfile.heightCm.toDouble(),
                            weightKg = updatedProfile.weightKg,
                            leanBodyMassKg = leanBodyMass ?: metricHistory.lastOrNull()?.leanBodyMassKg,
                            waistCm = updatedComposition.waistCm,
                            neckCm = updatedComposition.neckCm,
                            hipCm = updatedComposition.hipCm,
                            bodyFatPercentage = bodyFat ?: metricHistory.lastOrNull()?.bodyFatPercentage,
                        )
                        input = formatBodyMetric(selected, canonical, profile.metricSystem).substringBefore(' ')
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.accent, contentColor = WildforceThemeTokens.primaryButtonText),
                ) { Text("GUARDAR REGISTRO") }
            }
        }
        item {
            Section(selected.title) {
                Text(bodyMetricHint(selected), style = MaterialTheme.typography.body2, color = WildforceThemeTokens.textSecondary)
            }
        }
    }
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

private fun canonicalBodyMetric(kind: BodyMetricKind, value: Double?, metricSystem: MetricSystem): Double? {
    val input = value ?: return null
    return when (kind) {
        BodyMetricKind.Weight, BodyMetricKind.LeanBodyMass -> if (metricSystem == MetricSystem.Imperial) input / PoundsPerKilogram else input
        BodyMetricKind.Waist, BodyMetricKind.Neck, BodyMetricKind.Hip -> if (metricSystem == MetricSystem.Imperial) input * 2.54 else input
        BodyMetricKind.BodyFat -> input
    }.takeIf { it.isFinite() && it > 0 }
}

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
    val age = (currentYear - safeBirthYear).coerceIn(0, 120)
    val safeHeightCm = profile.heightCm.coerceIn(80, 260)
    val safeWeightKg = profile.weightKg.takeIf { it.isFinite() && it > 0.0 } ?: 70.0
    val basalMetabolicRate = when (profile.gender.storedValue) {
        "female" -> 10 * safeWeightKg + 6.25 * safeHeightCm - 5 * age - 161
        else -> 10 * safeWeightKg + 6.25 * safeHeightCm - 5 * age + 5
    }.roundToInt()
    val activityFactor = when (profile.lifestyle) {
        LifestyleLevel.Sedentary -> 1.2
        LifestyleLevel.LightlyActive -> 1.375
        LifestyleLevel.ModeratelyActive -> 1.55
        LifestyleLevel.VeryActive -> 1.725
    }
    val activeMetabolicRate = (basalMetabolicRate * activityFactor).roundToInt()
    Section("TMB") {
        ReadOnlyProfileRow("Metabolismo basal", "$basalMetabolicRate kcal", "Calorías que quema tu cuerpo en reposo absoluto según altura, peso, edad y sexo.")
        ReadOnlyProfileRow("Metabolismo activo", "$activeMetabolicRate kcal", "TMB ajustada a tu estilo de vida ${profile.lifestyle.title.lowercase()}.")
    }
    Section("Composición corporal") {
        val waistToHeight = measurements.waistCm?.div(safeHeightCm)
        if (waistToHeight != null) {
            val zone = when {
                waistToHeight < .42 -> "Delgado"
                waistToHeight < .50 -> "Saludable"
                waistToHeight < .60 -> "Elevado"
                else -> "Alto"
            }
            ReadOnlyProfileRow("Relación cintura-altura", "%.2f".format(waistToHeight), zone)
        } else ReadOnlyProfileRow("Relación cintura-altura", "—", "Registra la cintura para ver este dato.")
        val bodyFat = navyBodyFat(profile.gender, safeHeightCm.toDouble(), measurements)
        if (bodyFat != null) {
            val zone = when {
                bodyFat < 14 -> "Atlético"
                bodyFat < 21 -> "En forma"
                bodyFat < 30 -> "Promedio"
                else -> "Elevado"
            }
            ReadOnlyProfileRow("Grasa corporal estimada", "%.1f%%".format(bodyFat), "$zone · Fórmula de la Marina de EE. UU.")
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

private fun navyBodyFat(gender: Gender, heightCm: Double, measurements: BodyCompositionMeasurements): Double? {
    val waist = measurements.waistCm ?: return null
    val neck = measurements.neckCm ?: return null
    val denominator = if (gender.storedValue == "female") {
        val hip = measurements.hipCm ?: return null
        val circumference = waist + hip - neck
        if (circumference <= 0) return null
        1.29579 - 0.35004 * log10(circumference) + 0.22100 * log10(heightCm)
    } else {
        val circumference = waist - neck
        if (circumference <= 0) return null
        1.0324 - 0.19077 * log10(circumference) + 0.15456 * log10(heightCm)
    }
    return (495 / denominator - 450).takeIf { it.isFinite() && it in 2.0..70.0 }
}

@Composable
private fun ProfileHero(profile: OnboardingProfile, currentStreak: Int, completedWorkouts: Int, longestStreak: Int, avatarPath: String?, avatarRevision: Int, onChangeAvatar: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val avatarSize = if (maxWidth < 380.dp) 88.dp else 110.dp
        val gap = if (maxWidth < 380.dp) 12.dp else 20.dp
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(gap)) {
            Box(Modifier.size(avatarSize).clickable(onClick = onChangeAvatar)) {
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
            Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(profile.name.ifBlank { "Tu perfil" }, fontFamily = AntonFontFamily, fontSize = 28.sp, color = WildforceThemeTokens.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Filled.FitnessCenter, contentDescription = null, modifier = Modifier.size(15.dp), tint = WildforceThemeTokens.textSecondary)
                    Text("${profile.goal.title} • ${profile.trainingLevel.title}", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    ProfileStat("Racha", currentStreak.toString(), Modifier.weight(1f))
                    ProfileStat("Más larga", longestStreak.toString(), Modifier.weight(1f))
                    ProfileStat("Entrenos", completedWorkouts.toString(), Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ProfileExperience(experienceXp: Int, experienceLevel: Int, experienceProgress: Float) {
    val animatedExperienceProgress by animateFloatAsState(experienceProgress.coerceIn(0f, 1f), animationSpec = tween(650), label = "profile-experience")
    val nextLevelXp = minimumTotalXp(experienceLevel + 1)
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
    Column(modifier.padding(4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
        Notes -> profile.notes
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
private fun ProfileOptionPickerDialog(title: String, options: List<String>, onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    androidx.compose.material.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontFamily = Exo2FontFamily, fontSize = 22.sp, fontWeight = FontWeight.SemiBold) },
        text = {
            LazyColumn(Modifier.height(if (options.size > 6) 380.dp else (options.size * 52).dp)) {
                items(options) { option ->
                    Text(option, Modifier.fillMaxWidth().clickable { onSelect(option) }.padding(vertical = 14.dp), color = WildforceThemeTokens.textPrimary)
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
        dismissButton = { Text("CANCELAR", Modifier.clickable(onClick = onDismiss).padding(12.dp), color = WildforceThemeTokens.textSecondary) },
    )
}

private fun NutritionProfilePreferences.eatingWindowLabel(): String = eatingWindowStartHour?.let { start ->
    eatingWindowEndHour?.let { end -> "%02d:00 - %02d:00".format(start, end) }
} ?: "Flexible"

private fun List<String>.listSummary(): String = if (isEmpty()) "Ninguno" else joinToString(", ")

private fun String.profileDecimal(): String = filter { it.isDigit() || it == ',' || it == '.' }.take(6)
private fun String.profileDouble(): Double? = replace(',', '.').toDoubleOrNull()?.takeIf { it in 20.0..300.0 }

private sealed class Picker { data object Goal : Picker(); data object Level : Picker(); data object Lifestyle : Picker(); data object Split : Picker(); data object Body : Picker(); data object Gym : Picker(); data object Gender : Picker(); data object Metric : Picker(); data object Equipment : Picker(); data object Restrictions : Picker(); data object BirthMonth : Picker(); data class Focus(val day: WorkoutWeekday) : Picker() }

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
        Picker.BirthMonth -> { title = "Mes de nacimiento"; values = (1..12).map { month -> monthName(month) to { p: OnboardingProfile -> p.copy(birthMonth = month) } } }
        Picker.Equipment, Picker.Restrictions -> { title = ""; values = emptyList() }
        is Picker.Focus -> { title = "Foco de ${picker.day.title}"; values = WorkoutFocus.customSelectionCases.map { it.title to { p: OnboardingProfile -> p.copy(customWorkoutFocuses = p.customWorkoutFocuses + (picker.day to it)) } } }
    }
            androidx.compose.material.AlertDialog(onDismissRequest = { onSelect(profile) }, title = { Text(title, fontFamily = AntonFontFamily) }, text = { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { values.forEach { (label, transform) -> Text(label, Modifier.fillMaxWidth().clickable { onSelect(transform(profile)) }.padding(12.dp), color = WildforceThemeTokens.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis) } } }, confirmButton = {})
}

@Composable
private fun EquipmentPickerDialog(initial: Set<Equipment>, onSave: (Set<Equipment>) -> Unit, onDismiss: () -> Unit) {
    var selected by remember { mutableStateOf(initial) }
    androidx.compose.material.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("EQUIPAMIENTO", fontFamily = AntonFontFamily) },
        text = { LazyColumn(Modifier.height(360.dp)) { items(Equipment.entries) { equipment -> TogglePickerRow(equipment.title, equipment in selected) { selected = if (equipment in selected) selected - equipment else selected + equipment } } } },
        confirmButton = { Button(onClick = { onSave(selected); onDismiss() }) { Text("GUARDAR") } },
        dismissButton = { Text("CANCELAR", Modifier.clickable(onClick = onDismiss).padding(12.dp), color = WildforceThemeTokens.textSecondary) },
    )
}

@Composable
private fun TrainingLocationsDialog(
    initial: List<TrainingLocationProfile>,
    onSave: (List<TrainingLocationProfile>) -> Unit,
    onDismiss: () -> Unit,
) {
    var locations by remember(initial) { mutableStateOf(initial) }
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    var editorName by remember { mutableStateOf("") }
    var editorEquipment by remember { mutableStateOf(setOf<Equipment>()) }
    val editing = editingIndex?.let(locations::getOrNull)
    androidx.compose.material.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editing == null) "UBICACIONES" else "EDITAR UBICACIÓN", fontFamily = AntonFontFamily) },
        text = {
            if (editing == null) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LazyColumn(Modifier.height(250.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        itemsIndexed(locations) { index, location ->
                            Row(Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(12.dp)).clickable {
                                locations = locations.mapIndexed { itemIndex, item -> item.copy(isDefault = itemIndex == index) }
                            }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(location.name, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${location.equipment.size} elementos", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary, maxLines = 1)
                                }
                                Text(if (location.isDefault) "PRINCIPAL" else "EDITAR", Modifier.clickable {
                                    editingIndex = locations.indexOf(location)
                                    editorName = location.name
                                    editorEquipment = location.equipment
                                }.padding(6.dp), style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.accentGold)
                            }
                        }
                    }
                    Text("＋ AÑADIR UBICACIÓN", Modifier.fillMaxWidth().clickable {
                        editingIndex = -1
                        editorName = ""
                        editorEquipment = setOf(Equipment.Bodyweight)
                    }.padding(10.dp), color = WildforceThemeTokens.accentGold, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Text("Toca una ubicación para marcarla como principal. Su equipamiento será el usado al generar planes.", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(editorName, { editorName = it.take(50) }, Modifier.fillMaxWidth(), label = { Text("Nombre") }, singleLine = true)
                    LazyColumn(Modifier.height(280.dp)) {
                        items(Equipment.entries) { equipment ->
                            TogglePickerRow(equipment.title, equipment in editorEquipment) {
                                editorEquipment = if (equipment in editorEquipment) editorEquipment - equipment else editorEquipment + equipment
                            }
                        }
                    }
                    if (locations.size > 1) {
                        Text("ELIMINAR UBICACIÓN", Modifier.fillMaxWidth().clickable {
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
                if (editing == null) onSave(locations)
                else {
                    val updated = TrainingLocationProfile(
                        name = editorName.trim().ifBlank { "Ubicación ${locations.size + 1}" },
                        equipment = editorEquipment.ifEmpty { setOf(Equipment.Bodyweight) },
                        isDefault = editing.isDefault || locations.isEmpty(),
                    )
                    locations = if (editingIndex == -1) {
                        if (locations.any { it.isDefault }) locations + updated else locations + updated.copy(isDefault = true)
                    } else locations.mapIndexed { index, location -> if (index == editingIndex) updated else location }
                    editingIndex = null
                }
            }) { Text(if (editing == null) "GUARDAR" else "LISTO") }
        },
        dismissButton = {
            Text(if (editing == null) "CANCELAR" else "VOLVER", Modifier.clickable {
                if (editing == null) onDismiss() else editingIndex = null
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
        dismissButton = { Text("CANCELAR", Modifier.clickable(onClick = onDismiss).padding(12.dp), color = WildforceThemeTokens.textSecondary) },
    )
}

@Composable
private fun TogglePickerRow(label: String, selected: Boolean, onToggle: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = WildforceThemeTokens.textPrimary)
        Text(if (selected) "✓" else "", color = WildforceThemeTokens.accentGold, fontWeight = FontWeight.Bold)
    }
}

@Composable private fun Section(title: String, content: @Composable () -> Unit) { Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(title, fontFamily = Exo2FontFamily, fontSize = 18.sp, color = WildforceThemeTokens.textPrimary); Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(WildforceThemeTokens.backgroundSecondary).padding(horizontal = 14.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { content() } } }
@Composable private fun ChoiceButton(label: String, value: String, description: String? = null, icon: ImageVector? = null, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) .97f else 1f, spring(dampingRatio = .7f, stiffness = 700f), label = "profile-choice-press")
    Row(Modifier.fillMaxWidth().graphicsLayer { scaleX = scale; scaleY = scale }.clickable(interactionSource = interaction, indication = null, onClick = onClick).padding(vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { icon?.let { Icon(it, contentDescription = null, modifier = Modifier.padding(end = 12.dp).size(20.dp), tint = WildforceThemeTokens.textSecondary) }; Column(Modifier.weight(1f)) { Text(label, color = WildforceThemeTokens.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis); description?.let { Text(it, Modifier.padding(top = 3.dp), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary, maxLines = 3, overflow = TextOverflow.Ellipsis) } }; Text(value, Modifier.padding(start = 10.dp).weight(.85f), color = WildforceThemeTokens.textSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.End, maxLines = 2, overflow = TextOverflow.Ellipsis); Text("›", Modifier.padding(start = 8.dp), color = WildforceThemeTokens.textSecondary, fontWeight = FontWeight.Bold) }
}
@Composable
private fun NumberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier,
    decimal: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = { Text(label) },
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
@Composable private fun TogglePreference(label: String, description: String, selected: Boolean, icon: ImageVector? = null, onClick: () -> Unit) { Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { icon?.let { Icon(it, contentDescription = null, modifier = Modifier.padding(end = 12.dp).size(20.dp), tint = WildforceThemeTokens.textSecondary) }; Column(Modifier.weight(1f)) { Text(label, color = WildforceThemeTokens.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis); Text(description, Modifier.padding(top = 3.dp), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary, maxLines = 3, overflow = TextOverflow.Ellipsis) }; androidx.compose.material.Switch(checked = selected, onCheckedChange = { onClick() }, modifier = Modifier.padding(start = 10.dp), colors = androidx.compose.material.SwitchDefaults.colors(checkedThumbColor = WildforceThemeTokens.accent, checkedTrackColor = WildforceThemeTokens.accent.copy(alpha = .32f))) } }

private const val PoundsPerKilogram = 2.2046226218

private fun displayHeight(heightCm: Int, system: MetricSystem): String = when (system) {
    MetricSystem.Metric -> heightCm.toString()
    MetricSystem.Imperial -> (heightCm / 2.54).roundToInt().toString()
}

private fun displayWeight(weightKg: Double, system: MetricSystem): String = when (system) {
    MetricSystem.Metric -> "%.1f".format(Locale.US, weightKg)
    MetricSystem.Imperial -> "%.1f".format(Locale.US, weightKg * PoundsPerKilogram)
}

private fun validatedProfile(draft: OnboardingProfile, height: String, weight: String, duration: String, birthYear: String = draft.birthYear.toString()): OnboardingProfile = draft.copy(
    heightCm = height.toIntOrNull()?.let { entered -> if (draft.metricSystem == MetricSystem.Imperial) (entered * 2.54).roundToInt() else entered }?.coerceIn(120, 230) ?: draft.heightCm,
    weightKg = weight.replace(',', '.').toDoubleOrNull()?.let { entered -> if (draft.metricSystem == MetricSystem.Imperial) entered / PoundsPerKilogram else entered }?.coerceIn(35.0, 250.0) ?: draft.weightKg,
    preferredWorkoutDurationMinutes = duration.toIntOrNull()?.coerceIn(15, 180) ?: draft.preferredWorkoutDurationMinutes,
    birthYear = birthYear.toIntOrNull()?.coerceIn(1920, Year.now().value) ?: draft.birthYear,
)

private fun monthName(month: Int): String = Month.of(month.coerceIn(1, 12)).getDisplayName(TextStyle.FULL, Locale.forLanguageTag("es-ES")).replaceFirstChar { it.titlecase(Locale.forLanguageTag("es-ES")) }
