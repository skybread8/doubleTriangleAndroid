package io.codepassion.doubletriangle.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.LinearProgressIndicator
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.Year
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt
import io.codepassion.doubletriangle.core.designsystem.AntonFontFamily
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
    onRequestHealthConnect: (((Boolean, Int?, Double?) -> Unit) -> Unit) = { _ -> },
    onSave: (OnboardingProfile) -> Unit,
    onRegenerate: (OnboardingProfile) -> Unit = {},
) {
    var draft by remember(initial) { mutableStateOf(initial) }
    var heightInput by remember(initial) { mutableStateOf(displayHeight(initial.heightCm, initial.metricSystem)) }
    var weightInput by remember(initial) { mutableStateOf(displayWeight(initial.weightKg, initial.metricSystem)) }
    var durationInput by remember(initial) { mutableStateOf(initial.preferredWorkoutDurationMinutes.toString()) }
    var birthYearInput by remember(initial) { mutableStateOf(initial.birthYear.toString()) }
    var picker by remember { mutableStateOf<Picker?>(null) }
    var showsTrainingLocations by remember { mutableStateOf(false) }
    var healthConnectMessage by remember { mutableStateOf<String?>(null) }
    var saveMessage by remember(initial) { mutableStateOf<String?>(null) }
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    LazyColumn(
        modifier = Modifier.fillMaxSize().liquidGlassBackground().padding(horizontal = 16.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 16.dp, bottom = contentPadding.calculateBottomPadding() + 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ProfileEntrance(entered, 0) { ProfileHero(draft, currentStreak, completedWorkouts, longestStreak, experienceXp, experienceLevel, experienceProgress) }
        }
        item {
            ProfileEntrance(entered, 55) { Section("DATOS PERSONALES") {
                OutlinedTextField(draft.name, { draft = draft.copy(name = it.take(60)) }, Modifier.fillMaxWidth(), label = { Text("Nombre") }, singleLine = true)
                ChoiceButton("Mes de nacimiento", monthName(draft.birthMonth)) { picker = Picker.BirthMonth }
                NumberField("Año de nacimiento", birthYearInput, { birthYearInput = it.filter(Char::isDigit).take(4) }, Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    NumberField(if (draft.metricSystem == MetricSystem.Imperial) "Altura (in)" else "Altura (cm)", heightInput, { heightInput = it.filter(Char::isDigit).take(3) }, Modifier.weight(1f))
                    NumberField(if (draft.metricSystem == MetricSystem.Imperial) "Peso (lb)" else "Peso (kg)", weightInput, { weightInput = it.filter { char -> char.isDigit() || char == ',' || char == '.' }.take(6) }, Modifier.weight(1f), decimal = true)
                }
                ChoiceButton("Sexo", draft.gender.title) { picker = Picker.Gender }
                ChoiceButton("Unidades", draft.metricSystem.title) { picker = Picker.Metric }
                ChoiceButton("Health Connect", if (draft.isHealthConnectEnabled) "Conectado" else "Conectar") {
                    onRequestHealthConnect { granted, importedHeight, importedWeight ->
                        if (granted) {
                            importedHeight?.let { heightInput = displayHeight(it, draft.metricSystem) }
                            importedWeight?.let { weightInput = displayWeight(it, draft.metricSystem) }
                            draft = draft.copy(
                                isHealthConnectEnabled = true,
                                heightCm = importedHeight ?: draft.heightCm,
                                weightKg = importedWeight ?: draft.weightKg,
                            )
                            healthConnectMessage = if (importedHeight != null || importedWeight != null) "Medidas importadas. Guarda el perfil para aplicarlas al plan." else "Health Connect está conectado."
                        } else {
                            healthConnectMessage = "No se concedió el acceso a Health Connect."
                        }
                    }
                }
                healthConnectMessage?.let { Text(it, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary) }
            } }
        }
        item {
            ProfileEntrance(entered, 105) { Section("OBJETIVO Y NIVEL") {
                ChoiceButton("Objetivo", draft.goal.title) { picker = Picker.Goal }
                ChoiceButton("Nivel", draft.trainingLevel.title, draft.trainingLevel.description) { picker = Picker.Level }
                ChoiceButton("Actividad diaria", draft.lifestyle.title, draft.lifestyle.description) { picker = Picker.Lifestyle }
                if (draft.goal.supportsBodyComposition && draft.trainingLevel.supportsBodyComposition) ChoiceButton("Fase corporal", draft.bodyCompositionPhase?.title ?: "Automático") { picker = Picker.Body }
            } }
        }
        item {
            ProfileEntrance(entered, 155) { Section("ESTRUCTURA DEL PLAN") {
                ChoiceButton("División semanal", draft.trainingSplitPreference.title) { picker = Picker.Split }
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
                TogglePreference("Omitir vuelta a la calma", "Mantiene el plan dentro del tiempo indicado sin bloques finales.", draft.skipsCooldowns) { draft = draft.copy(skipsCooldowns = !draft.skipsCooldowns) }
                TogglePreference("Omitir descansos", "Avanza directamente entre series y ejercicios durante la sesión.", draft.skipsRestPeriods) { draft = draft.copy(skipsRestPeriods = !draft.skipsRestPeriods) }
                OutlinedTextField(draft.workoutPlannerNotes, { draft = draft.copy(workoutPlannerNotes = it.take(500)) }, Modifier.fillMaxWidth(), label = { Text("Notas para el planificador") }, minLines = 2, maxLines = 4)
            } }
        }
        item {
            ProfileEntrance(entered, 205) { Section("ENTORNO Y SEGURIDAD") {
                ChoiceButton("Tipo de gimnasio", draft.gymType.title) { picker = Picker.Gym }
                ChoiceButton("Ubicaciones de entrenamiento", draft.effectiveTrainingLocations().joinToString(" · ") { if (it.isDefault) "${it.name} (principal)" else it.name }) { showsTrainingLocations = true }
                ChoiceButton("Equipamiento disponible", "${draft.availableEquipment.size} seleccionado(s)") { picker = Picker.Equipment }
                ChoiceButton("Restricciones o molestias", draft.movementRestrictions.takeIf { it.isNotEmpty() }?.size?.let { "$it seleccionado(s)" } ?: "Ninguna") { picker = Picker.Restrictions }
            } }
        }
        item {
            ProfileEntrance(entered, 255) {
            generationError?.let { Text(it, Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(12.dp)).padding(12.dp), color = Color(0xFFC62828)) }
            Column(Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(18.dp), emphasized = true).padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("PLAN PERSONALIZADO", fontFamily = AntonFontFamily, color = WildforceThemeTokens.textPrimary)
                Text("Genera un plan nuevo usando todas tus preferencias, medidas, equipamiento y restricciones.", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
                Button(onClick = { val saved = validatedProfile(draft, heightInput, weightInput, durationInput, birthYearInput); onSave(saved); onRegenerate(saved) }, modifier = Modifier.fillMaxWidth().height(56.dp), enabled = !isRegenerating, shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.accentGold, contentColor = Color.White)) { Text(if (isRegenerating) "GENERANDO PLAN…" else "✦  REGENERAR PLAN CON IA", fontWeight = FontWeight.Bold) }
                Button(onClick = { onSave(validatedProfile(draft, heightInput, weightInput, durationInput, birthYearInput)); saveMessage = "Cambios guardados en tu perfil." }, modifier = Modifier.fillMaxWidth().height(46.dp), shape = RoundedCornerShape(14.dp), enabled = !isRegenerating, colors = ButtonDefaults.outlinedButtonColors(backgroundColor = Color.Transparent, contentColor = WildforceThemeTokens.textPrimary)) { Text("GUARDAR CAMBIOS SIN REGENERAR", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.caption) }
                saveMessage?.let { Text(it, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.accentGold) }
            }
            Spacer(Modifier.height(28.dp))
            }
        }
    }
    picker?.let { current ->
        when (current) {
            Picker.Equipment -> EquipmentPickerDialog(draft.availableEquipment, { selected ->
                val locations = draft.effectiveTrainingLocations().map { location ->
                    if (location.isDefault) location.copy(equipment = selected) else location
                }
                draft = draft.copy(availableEquipment = selected, trainingLocations = locations)
            }) { picker = null }
            Picker.Restrictions -> RestrictionsPickerDialog(draft.movementRestrictions, { selected -> draft = draft.copy(movementRestrictions = selected) }) { picker = null }
            else -> PickerDialog(current, draft) { updated ->
                if (current == Picker.Metric && updated.metricSystem != draft.metricSystem) {
                    val normalized = validatedProfile(draft, heightInput, weightInput, durationInput, birthYearInput)
                    draft = normalized.copy(metricSystem = updated.metricSystem)
                    heightInput = displayHeight(normalized.heightCm, updated.metricSystem)
                    weightInput = displayWeight(normalized.weightKg, updated.metricSystem)
                } else draft = updated
                picker = null
            }
        }
    }
    if (showsTrainingLocations) {
        TrainingLocationsDialog(
            initial = draft.effectiveTrainingLocations(),
            onSave = { locations ->
                val defaultLocation = locations.firstOrNull { it.isDefault } ?: locations.first()
                draft = draft.copy(trainingLocations = locations, availableEquipment = defaultLocation.equipment)
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
                Text("La IA está organizando tus sesiones y ajustándolas a tu perfil.", style = MaterialTheme.typography.body2, color = WildforceThemeTokens.textSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
    }
}

@Composable
private fun ProfileEntrance(visible: Boolean, delayMillis: Int, content: @Composable () -> Unit) {
    AnimatedVisibility(visible = visible, enter = fadeIn(tween(330, delayMillis = delayMillis)) + slideInVertically(tween(330, delayMillis = delayMillis)) { it / 16 }) { content() }
}

@Composable
private fun ProfileHero(profile: OnboardingProfile, currentStreak: Int, completedWorkouts: Int, longestStreak: Int, experienceXp: Int, experienceLevel: Int, experienceProgress: Float) {
    val animatedExperienceProgress by animateFloatAsState(experienceProgress.coerceIn(0f, 1f), animationSpec = tween(650), label = "profile-experience")
    Column(Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(24.dp), emphasized = true).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(profile.name.take(1).uppercase().ifBlank { "W" }, Modifier.size(68.dp).background(WildforceThemeTokens.accentGold, CircleShape).padding(19.dp), fontFamily = AntonFontFamily, style = MaterialTheme.typography.h4, color = Color.White)
            Column {
                Text(profile.name.ifBlank { "Tu perfil" }, fontFamily = AntonFontFamily, style = MaterialTheme.typography.h4, color = WildforceThemeTokens.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${profile.goal.title} · ${profile.trainingLevel.title}", color = WildforceThemeTokens.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            ProfileStat("RACHA", currentStreak.toString(), Modifier.weight(1f))
            ProfileStat("SESIONES", completedWorkouts.toString(), Modifier.weight(1f))
            ProfileStat("MEJOR RACHA", longestStreak.toString(), Modifier.weight(1f))
        }
        Column(Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(13.dp)).padding(11.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("NIVEL $experienceLevel", fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
                Text("$experienceXp XP", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
            }
            LinearProgressIndicator(animatedExperienceProgress, Modifier.fillMaxWidth().height(8.dp), color = WildforceThemeTokens.accentGold, backgroundColor = WildforceThemeTokens.textSecondary.copy(alpha = .16f))
        }
    }
}

@Composable
private fun ProfileStat(label: String, value: String, modifier: Modifier) {
    Column(modifier.liquidGlass(RoundedCornerShape(13.dp)).padding(9.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary, maxLines = 1)
        Text(label, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

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
    androidx.compose.material.AlertDialog(onDismissRequest = { onSelect(profile) }, title = { Text(title, fontFamily = AntonFontFamily) }, text = { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { values.forEach { (label, transform) -> Text(label, Modifier.fillMaxWidth().clickable { onSelect(transform(profile)) }.padding(12.dp), color = WildforceThemeTokens.textPrimary) } } }, confirmButton = {})
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
                                    Text(location.name, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
                                    Text("${location.equipment.size} elementos", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
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
                    Text("Toca una ubicación para marcarla como principal. Su equipamiento será el usado al generar planes.", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
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

@Composable private fun Section(title: String, content: @Composable () -> Unit) { Column(Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(18.dp)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { Text(title, fontFamily = AntonFontFamily, color = WildforceThemeTokens.accentGold, maxLines = 1, overflow = TextOverflow.Ellipsis); content() } }
@Composable private fun ChoiceButton(label: String, value: String, description: String? = null, onClick: () -> Unit) { Row(Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(13.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(label, color = WildforceThemeTokens.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis); description?.let { Text(it, Modifier.padding(top = 3.dp), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis) } }; Text(value, Modifier.padding(start = 10.dp), color = WildforceThemeTokens.textPrimary, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis) } }
@Composable private fun NumberField(label: String, value: String, onValueChange: (String) -> Unit, modifier: Modifier, decimal: Boolean = false) { OutlinedTextField(value, onValueChange, modifier, label = { Text(label) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number)) }
@Composable private fun TogglePreference(label: String, description: String, selected: Boolean, onClick: () -> Unit) { Row(Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(13.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(label, color = WildforceThemeTokens.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(description, Modifier.padding(top = 3.dp), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis) }; Text(if (selected) "SÍ" else "NO", Modifier.padding(start = 10.dp), color = if (selected) WildforceThemeTokens.accentGold else WildforceThemeTokens.textSecondary, fontWeight = FontWeight.Bold) } }

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
    weightKg = weight.replace(',', '.').toDoubleOrNull()?.let { entered -> if (draft.metricSystem == MetricSystem.Imperial) entered / PoundsPerKilogram else entered }?.coerceIn(30.0, 300.0) ?: draft.weightKg,
    preferredWorkoutDurationMinutes = duration.toIntOrNull()?.coerceIn(15, 180) ?: draft.preferredWorkoutDurationMinutes,
    birthYear = birthYear.toIntOrNull()?.coerceIn(1920, Year.now().value - 13) ?: draft.birthYear,
)

private fun monthName(month: Int): String = Month.of(month.coerceIn(1, 12)).getDisplayName(TextStyle.FULL, Locale.forLanguageTag("es-ES")).replaceFirstChar { it.titlecase(Locale.forLanguageTag("es-ES")) }
