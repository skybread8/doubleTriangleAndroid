package io.codepassion.doubletriangle.feature.onboarding

import android.graphics.BitmapFactory

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
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
    var nutritionProfile by remember { mutableStateOf(ProfileDetailPreferencesStore.loadNutrition(profileContext)) }
    var bodyComposition by remember { mutableStateOf(ProfileDetailPreferencesStore.loadBodyComposition(profileContext)) }
    var waistInput by remember { mutableStateOf(bodyComposition.waistCm?.toString().orEmpty()) }
    var neckInput by remember { mutableStateOf(bodyComposition.neckCm?.toString().orEmpty()) }
    var hipInput by remember { mutableStateOf(bodyComposition.hipCm?.toString().orEmpty()) }
    var nutritionPicker by remember { mutableStateOf<NutritionPicker?>(null) }
    var nutritionTextEditor by remember { mutableStateOf<NutritionTextField?>(null) }
    var settingsPicker by remember { mutableStateOf<SettingsPicker?>(null) }
    var selectedSection by remember { mutableStateOf<ProfileSection?>(null) }
    val bodyMetrics = remember(profileContext, initial) { BodyMetricsStore.load(profileContext) }
    fun saveProfile(value: OnboardingProfile) {
        BodyMetricsStore.record(profileContext, value)
        onSave(value)
    }
    LaunchedEffect(Unit) { entered = true }
    LaunchedEffect(draft, heightInput, weightInput, durationInput, birthYearInput) {
        saveProfile(validatedProfile(draft, heightInput, weightInput, durationInput, birthYearInput))
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().liquidGlassBackground().padding(horizontal = 16.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 16.dp, bottom = contentPadding.calculateBottomPadding() + 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (selectedSection != null) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(44.dp).clip(CircleShape).background(Color.White).clickable { selectedSection = null }, contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = WildforceThemeTokens.textPrimary, modifier = Modifier.size(22.dp))
                    }
                    Text(selectedSection?.title?.uppercase().orEmpty(), fontFamily = Exo2FontFamily, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
                }
            }
        }
        if (selectedSection == null) { item { ProfileEntrance(entered, 0) { ProfileHero(draft, currentStreak, completedWorkouts, longestStreak, avatarPath, avatarRevision, onChangeAvatar) } } }
        if (selectedSection == null) { item { ProfileEntrance(entered, 35) { ProfileExperience(experienceXp, experienceLevel, experienceProgress) } } }
        if (selectedSection == null) { item { ProfileCategoryTree(draft, nutritionProfile, appLanguage) { selectedSection = it } } }
        if (selectedSection == null) { item { ProfileEntrance(entered, 65) { ProfileAchievements() } } }
        if (selectedSection == ProfileSection.Body) { item { Section("Cuerpo") {
                ChoiceButton("Sexo", draft.gender.title) { picker = Picker.Gender }
                ChoiceButton("Fecha de nacimiento", "${monthName(draft.birthMonth)} $birthYearInput") { picker = Picker.BirthMonth }
                NumberField("Año de nacimiento", birthYearInput, { birthYearInput = it.filter(Char::isDigit).take(4) }, Modifier.fillMaxWidth())
                NumberField(if (draft.metricSystem == MetricSystem.Imperial) "Altura (in)" else "Altura (cm)", heightInput, { heightInput = it.filter(Char::isDigit).take(3) }, Modifier.fillMaxWidth())
                NumberField(if (draft.metricSystem == MetricSystem.Imperial) "Peso (lb)" else "Peso (kg)", weightInput, { weightInput = it.filter { char -> char.isDigit() || char == ',' || char == '.' }.take(6) }, Modifier.fillMaxWidth(), decimal = true)
                ChoiceButton("Métricas corporales", bodyMetrics.lastOrNull()?.let { "${"%.1f".format(it.weightKg)} kg" } ?: "Sin registros") { }
                ChoiceButton("Fotos de progreso corporal", "Sin fotos todavía") { }
            }
        } }
        if (selectedSection == ProfileSection.Body) { item {
            // Detail sections are rendered directly once selected. Keeping them out of
            // AnimatedVisibility avoids a LazyColumn re-measure race when switching
            // from the category tree to an editor with focused text fields.
            ProfileBodyMetrics(
                profile = validatedProfile(draft, heightInput, weightInput, durationInput, birthYearInput),
                history = bodyMetrics,
                measurements = bodyComposition,
                waistInput = waistInput,
                neckInput = neckInput,
                hipInput = hipInput,
                onWaistChanged = { waistInput = it.profileDecimal(); bodyComposition = bodyComposition.copy(waistCm = waistInput.profileDouble()); ProfileDetailPreferencesStore.saveBodyComposition(profileContext, bodyComposition) },
                onNeckChanged = { neckInput = it.profileDecimal(); bodyComposition = bodyComposition.copy(neckCm = neckInput.profileDouble()); ProfileDetailPreferencesStore.saveBodyComposition(profileContext, bodyComposition) },
                onHipChanged = { hipInput = it.profileDecimal(); bodyComposition = bodyComposition.copy(hipCm = hipInput.profileDouble()); ProfileDetailPreferencesStore.saveBodyComposition(profileContext, bodyComposition) },
            )
        } }
        if (selectedSection == ProfileSection.Fitness) { item { ProfileEntrance(entered, 105) { Section("Entrenamiento") {
                ChoiceButton("Objetivo", draft.goal.title) { picker = Picker.Goal }
                ChoiceButton("Estilo de vida", draft.lifestyle.title, draft.lifestyle.description) { picker = Picker.Lifestyle }
                ChoiceButton("Nivel de entrenamiento", draft.trainingLevel.title, draft.trainingLevel.description) { picker = Picker.Level }
                if (draft.goal.supportsBodyComposition && draft.trainingLevel.supportsBodyComposition) ChoiceButton("Intención de composición corporal", draft.bodyCompositionPhase?.title ?: "Automático") { picker = Picker.Body }
            } } } }
        if (selectedSection == ProfileSection.Fitness) { item { ProfileEntrance(entered, 155) { Section("Configuración de entrenamiento") {
                ChoiceButton("Configuración", draft.trainingSplitPreference.title) { picker = Picker.Split }
                Text("Días de entrenamiento", fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
                draft.workoutDays.toList().let { days ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        WorkoutWeekday.entries.forEach { day ->
                            val selected = day in draft.workoutDays
                            Text(day.glyph, modifier = Modifier.size(38.dp).background(if (selected) WildforceThemeTokens.textPrimary else WildforceThemeTokens.textSecondary.copy(alpha = .12f), RoundedCornerShape(12.dp)).clickable { draft = draft.copy(workoutDays = if (selected && draft.workoutDays.size > 1) draft.workoutDays - day else draft.workoutDays + day).let { it } }.padding(10.dp), color = if (selected) WildforceThemeTokens.backgroundSecondary else WildforceThemeTokens.textPrimary)
                        }
                    }
                }
                NumberField("Duración preferida (min)", durationInput, { durationInput = it.filter(Char::isDigit).take(3) }, Modifier.fillMaxWidth())
                draft.workoutDays.sortedBy { it.ordinal }.forEach { day ->
                    if (draft.trainingSplitPreference == TrainingSplitPreference.Custom) ChoiceButton(day.title, draft.customWorkoutFocuses[day]?.title ?: "Seleccionar foco") { picker = Picker.Focus(day) }
                }
                TogglePreference("Omitir calentamientos", "Oculta los bloques de activación y ajusta la duración objetivo.", draft.skipsWarmups) { draft = draft.copy(skipsWarmups = !draft.skipsWarmups) }
                TogglePreference("Omitir enfriamientos", "Oculta los bloques finales y mantiene el plan dentro de la duración objetivo.", draft.skipsCooldowns) { draft = draft.copy(skipsCooldowns = !draft.skipsCooldowns) }
                TogglePreference("Omitir periodos de descanso", "Salta directamente a la siguiente serie o ejercicio durante el entrenamiento.", draft.skipsRestPeriods) { draft = draft.copy(skipsRestPeriods = !draft.skipsRestPeriods) }
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
                        onClick = { nutritionProfile = nutritionProfile.copy(isConfigured = true); ProfileDetailPreferencesStore.saveNutrition(profileContext, nutritionProfile) },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary, contentColor = WildforceThemeTokens.backgroundSecondary),
                        shape = RoundedCornerShape(14.dp),
                    ) { Text("INICIAR CONFIGURACIÓN NUTRICIONAL") }
                } else Section("Planificación") {
                    ChoiceButton("Estilo alimentario", nutritionProfile.dietaryStyle) { nutritionPicker = NutritionPicker.DietaryStyle }
                    ChoiceButton("Comidas al día", "${nutritionProfile.mealsPerDay} comidas") { nutritionPicker = NutritionPicker.MealsPerDay }
                    ChoiceButton("Ventana de alimentación", nutritionProfile.eatingWindowLabel()) { nutritionPicker = NutritionPicker.EatingWindow }
                    TogglePreference("Sugerencias de comidas", "Muestra ideas sencillas junto a calorías, macros y recomendaciones para el entrenamiento.", nutritionProfile.wantsMealSuggestions) {
                        nutritionProfile = nutritionProfile.copy(wantsMealSuggestions = !nutritionProfile.wantsMealSuggestions)
                        ProfileDetailPreferencesStore.saveNutrition(profileContext, nutritionProfile)
                    }
                }
            }
        } }
        if (selectedSection == ProfileSection.Nutrition && nutritionProfile.isConfigured) { item { ProfileEntrance(entered, 255) { Section("Preferencias") {
                ChoiceButton("Esfuerzo al cocinar", nutritionProfile.cookingEffort) { nutritionPicker = NutritionPicker.CookingEffort }
                ChoiceButton("Sensibilidad al presupuesto", nutritionProfile.budgetSensitivity) { nutritionPicker = NutritionPicker.BudgetSensitivity }
                ChoiceButton("Fuentes de proteína", nutritionProfile.preferredProteinSources.listSummary()) { nutritionTextEditor = NutritionTextField.ProteinSources }
                ChoiceButton("Alimentos que no te gustan", nutritionProfile.dislikes.listSummary()) { nutritionTextEditor = NutritionTextField.Dislikes }
            } } } }
        if (selectedSection == ProfileSection.Nutrition && nutritionProfile.isConfigured) { item { ProfileEntrance(entered, 275) { Section("Restricciones") {
                ChoiceButton("Alimentos excluidos", nutritionProfile.excludedFoods.listSummary()) { nutritionTextEditor = NutritionTextField.ExcludedFoods }
                ChoiceButton("Alergias o intolerancias", nutritionProfile.allergiesAndIntolerances.listSummary()) { nutritionTextEditor = NutritionTextField.Allergies }
            } } } }
        if (selectedSection == ProfileSection.Nutrition && nutritionProfile.isConfigured) { item { ProfileEntrance(entered, 295) { Section("Notas") {
                ChoiceButton("Notas", nutritionProfile.notes.ifBlank { "Ninguna" }) { nutritionTextEditor = NutritionTextField.Notes }
            } } } }
        if (selectedSection == ProfileSection.Settings) { item { ProfileEntrance(entered, 245) { Section("Preferencias") {
                ChoiceButton("Idioma", appLanguage) { settingsPicker = SettingsPicker.Language }
                ChoiceButton("Sistema métrico", draft.metricSystem.title) { picker = Picker.Metric }
                ChoiceButton("Tema", appTheme) { settingsPicker = SettingsPicker.Theme }
            } } } }
        if (selectedSection == ProfileSection.Settings) { item { ProfileEntrance(entered, 265) { Section("Entorno") {
                TogglePreference("Health Connect", "Guarda entrenamientos y sincroniza medidas corporales con Health Connect.", draft.isHealthConnectEnabled) {
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
                TogglePreference("Recordatorios", "Recibe recordatorios de próximos entrenamientos y comidas pendientes de registrar.", reminderNotificationsEnabled) {
                    reminderNotificationsEnabled = !reminderNotificationsEnabled
                    notificationPreferences.edit().putBoolean("reminders", reminderNotificationsEnabled).apply()
                }
                TogglePreference("Mantener pantalla encendida durante el entrenamiento", "Evita que la pantalla se bloquee mientras hay un entrenamiento activo abierto.", keepScreenOn) {
                    keepScreenOn = !keepScreenOn
                    detailPreferences.edit().putBoolean("keep_screen_on", keepScreenOn).apply()
                    onKeepScreenOnChanged(keepScreenOn)
                }
                TogglePreference("Modo concentración total", "Reduce las distracciones durante los entrenamientos mediante los ajustes de concentración de Android.", fullFocusEnabled) {
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
                TogglePreference("Notificaciones de entrenamiento", "Muestra la sesión activa en el panel y la pantalla bloqueada.", notificationsEnabled) {
                    notificationsEnabled = !notificationsEnabled
                    notificationPreferences.edit().putBoolean("enabled", notificationsEnabled).apply()
                }
                TogglePreference("Avisos al comenzar el descanso", "Emite un aviso cuando empieza el temporizador de descanso.", restAlertsEnabled) {
                    restAlertsEnabled = !restAlertsEnabled
                    notificationPreferences.edit().putBoolean("rest_alerts", restAlertsEnabled).apply()
                }
            } } } }
        if (selectedSection == ProfileSection.Settings) { item { ProfileEntrance(entered, 285) { Section("Privacidad") {
                TogglePreference("Bloquear fotos de progreso", "Requiere la autenticación del dispositivo para abrir las fotos de progreso corporal.", progressPhotoLockEnabled) {
                    progressPhotoLockEnabled = !progressPhotoLockEnabled
                    detailPreferences.edit().putBoolean("progress_photo_lock", progressPhotoLockEnabled).apply()
                }
            } } } }
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
            ProfileDetailPreferencesStore.saveNutrition(profileContext, nutritionProfile)
            nutritionPicker = null
        }
    }
    nutritionTextEditor?.let { field ->
        NutritionTextEditorDialog(field, nutritionProfile, onDismiss = { nutritionTextEditor = null }) { value ->
            nutritionProfile = field.apply(nutritionProfile, value)
            ProfileDetailPreferencesStore.saveNutrition(profileContext, nutritionProfile)
            nutritionTextEditor = null
        }
    }
    settingsPicker?.let { current ->
        ProfileOptionPickerDialog(current.title, current.options, onDismiss = { settingsPicker = null }) { selected ->
            when (current) {
                SettingsPicker.Language -> {
                    appLanguage = selected
                    detailPreferences.edit().putString("language", selected).apply()
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
}

@Composable
private fun ProfileEntrance(visible: Boolean, delayMillis: Int, content: @Composable () -> Unit) {
    AnimatedVisibility(visible = visible, enter = fadeIn(tween(330, delayMillis = delayMillis)) + slideInVertically(tween(330, delayMillis = delayMillis)) { it / 16 }) { content() }
}

private enum class ProfileSection(val title: String, val icon: ImageVector) {
    Body("Perfil corporal", Icons.Filled.AccessibilityNew),
    Fitness("Perfil de fitness", Icons.Filled.FitnessCenter),
    Nutrition("Perfil nutricional", Icons.Filled.Restaurant),
    Settings("Ajustes", Icons.Filled.Settings),
}

@Composable
private fun ProfileCategoryTree(profile: OnboardingProfile, nutritionProfile: NutritionProfilePreferences, appLanguage: String, onSelect: (ProfileSection) -> Unit) {
    val summaries = mapOf(
        ProfileSection.Body to displayWeight(profile.weightKg, profile.metricSystem).let { "$it ${if (profile.metricSystem == MetricSystem.Imperial) "lb" else "kg"}" },
        ProfileSection.Fitness to "${profile.goal.title} • ${profile.effectiveTrainingLocations().firstOrNull { it.isDefault }?.name ?: profile.gymType.title}",
        ProfileSection.Nutrition to if (nutritionProfile.isConfigured) "${nutritionProfile.dietaryStyle} • ${nutritionProfile.mealsPerDay} comidas" else "Configuración necesaria",
        ProfileSection.Settings to "$appLanguage • ${profile.metricSystem.title}",
    )
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(WildforceThemeTokens.backgroundSecondary)) {
        ProfileSection.entries.forEachIndexed { index, section ->
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
            if (index < ProfileSection.entries.lastIndex) {
                Box(Modifier.fillMaxWidth().padding(start = 49.dp).height(1.dp).background(WildforceThemeTokens.textSecondary.copy(alpha = 0.16f)))
            }
        }
    }
}

@Composable
private fun ProfileBodyMetrics(
    profile: OnboardingProfile,
    history: List<BodyMetricEntry>,
    measurements: BodyCompositionMeasurements,
    waistInput: String,
    neckInput: String,
    hipInput: String,
    onWaistChanged: (String) -> Unit,
    onNeckChanged: (String) -> Unit,
    onHipChanged: (String) -> Unit,
) {
    val currentYear = Year.now().value
    val safeBirthYear = profile.birthYear.coerceIn(1900, currentYear - 13)
    val age = (currentYear - safeBirthYear).coerceIn(13, 120)
    val safeHeightCm = profile.heightCm.coerceIn(80, 260)
    val safeWeightKg = profile.weightKg.takeIf { it.isFinite() && it > 0.0 } ?: 70.0
    val safeHistory = history.filter { entry ->
        entry.date.isNotBlank() && entry.heightCm.isFinite() && entry.heightCm in 80.0..260.0 &&
            entry.weightKg.isFinite() && entry.weightKg in 20.0..400.0
    }
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
    Section("Tasa metabólica basal") {
        ReadOnlyProfileRow("Metabolismo basal", "$basalMetabolicRate kcal", "Calorías que quema tu cuerpo en reposo absoluto según altura, peso, edad y sexo.")
        ReadOnlyProfileRow("Metabolismo activo", "$activeMetabolicRate kcal", "TMB ajustada a tu estilo de vida ${profile.lifestyle.title.lowercase()}.")
    }
    Section("Composición corporal") {
        NumberField("Cintura (cm)", waistInput, onWaistChanged, Modifier.fillMaxWidth(), decimal = true)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField("Cuello (cm)", neckInput, onNeckChanged, Modifier.weight(1f), decimal = true)
            if (profile.gender.storedValue == "female") NumberField("Cadera (cm)", hipInput, onHipChanged, Modifier.weight(1f), decimal = true)
        }
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
    Section("Historial corporal") {
        if (safeHistory.isEmpty()) {
            Text("Guarda el perfil para comenzar el historial corporal.", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
        } else {
            val visible = safeHistory.takeLast(8)
            val min = visible.minOf { it.weightKg }.coerceAtLeast(1.0)
            val max = visible.maxOf { it.weightKg }.coerceAtLeast(min + 1.0)
            visible.forEach { entry ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(entry.date.takeLast(5), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary, modifier = Modifier.size(width = 44.dp, height = 24.dp))
                    val fraction = ((entry.weightKg - min) / (max - min)).toFloat().takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0f
                    Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(8.dp)).background(WildforceThemeTokens.textSecondary.copy(alpha = 0.14f))) {
                        Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().clip(RoundedCornerShape(8.dp)).background(WildforceThemeTokens.accentGold))
                    }
                    Text("${"%.1f".format(entry.weightKg)} kg", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textPrimary)
                }
            }
        }
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
                else Box(Modifier.fillMaxSize().background(WildforceThemeTokens.accentGold, CircleShape), contentAlignment = Alignment.Center) {
                    Text(profile.name.take(1).uppercase().ifBlank { "W" }, fontFamily = AntonFontFamily, fontSize = if (avatarSize < 100.dp) 32.sp else 40.sp, color = Color.White)
                }
                Box(
                    Modifier.align(Alignment.BottomEnd).offset((-4).dp, (-4).dp).size(34.dp)
                        .background(WildforceThemeTokens.accentGold, CircleShape)
                        .border(4.dp, WildforceThemeTokens.background, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.PhotoCamera, contentDescription = "Cambiar foto de perfil", modifier = Modifier.size(16.dp), tint = WildforceThemeTokens.backgroundSecondary)
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
                Box(Modifier.fillMaxWidth(animatedExperienceProgress).fillMaxHeight().clip(RoundedCornerShape(8.dp)).background(WildforceThemeTokens.accentGold.copy(alpha = .40f)).blur(6.dp))
                Box(Modifier.fillMaxWidth(animatedExperienceProgress).fillMaxHeight().clip(RoundedCornerShape(8.dp)).background(WildforceThemeTokens.accentGold))
            }
        }
    }
}

@Composable
private fun ProfileAchievements() {
    Column(Modifier.fillMaxWidth()) {
        Text("Logros", fontFamily = Exo2FontFamily, fontSize = 18.sp, color = WildforceThemeTokens.textPrimary, modifier = Modifier.padding(bottom = 8.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(7) {
                Image(painterResource(id = R.drawable.achievement), contentDescription = "Logro", modifier = Modifier.size(100.dp))
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
    data object DietaryStyle : NutritionPicker("Estilo alimentario", listOf("Estándar", "Vegetariana", "Vegana", "Pescetariana"))
    data object MealsPerDay : NutritionPicker("Comidas al día", (1..8).map { "$it" })
    data object EatingWindow : NutritionPicker("Ventana de alimentación", listOf("Flexible", "06:00 - 18:00", "08:00 - 20:00", "10:00 - 22:00", "12:00 - 20:00"))
    data object CookingEffort : NutritionPicker("Esfuerzo al cocinar", listOf("Rápido y fácil", "Moderado", "Gourmet"))
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
    data object Language : SettingsPicker("Idioma", listOf("Español", "English", "Català", "Français", "Deutsch", "Italiano", "Português", "日本語", "한국어", "中文"))
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
        is Picker.Focus -> { title = "Foco de ${picker.day.title}"; values = WorkoutFocus.entries.map { it.title to { p: OnboardingProfile -> p.copy(customWorkoutFocuses = p.customWorkoutFocuses + (picker.day to it)) } } }
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
@Composable private fun ChoiceButton(label: String, value: String, description: String? = null, onClick: () -> Unit) { Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(label, color = WildforceThemeTokens.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis); description?.let { Text(it, Modifier.padding(top = 3.dp), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary, maxLines = 3, overflow = TextOverflow.Ellipsis) } }; Text(value, Modifier.padding(start = 10.dp).weight(.85f), color = WildforceThemeTokens.textSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.End, maxLines = 2, overflow = TextOverflow.Ellipsis); Text("›", Modifier.padding(start = 8.dp), color = WildforceThemeTokens.textSecondary, fontWeight = FontWeight.Bold) } }
@Composable private fun NumberField(label: String, value: String, onValueChange: (String) -> Unit, modifier: Modifier, decimal: Boolean = false) { OutlinedTextField(value, onValueChange, modifier, label = { Text(label) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number)) }
@Composable private fun TogglePreference(label: String, description: String, selected: Boolean, onClick: () -> Unit) { Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(label, color = WildforceThemeTokens.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis); Text(description, Modifier.padding(top = 3.dp), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary, maxLines = 3, overflow = TextOverflow.Ellipsis) }; androidx.compose.material.Switch(checked = selected, onCheckedChange = { onClick() }, modifier = Modifier.padding(start = 10.dp), colors = androidx.compose.material.SwitchDefaults.colors(checkedThumbColor = WildforceThemeTokens.accentGold, checkedTrackColor = WildforceThemeTokens.accentGold.copy(alpha = .45f))) } }

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
    birthYear = birthYear.toIntOrNull()?.coerceIn(1920, Year.now().value - 13) ?: draft.birthYear,
)

private fun monthName(month: Int): String = Month.of(month.coerceIn(1, 12)).getDisplayName(TextStyle.FULL, Locale.forLanguageTag("es-ES")).replaceFirstChar { it.titlecase(Locale.forLanguageTag("es-ES")) }
