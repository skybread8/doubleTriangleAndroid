package io.codepassion.doubletriangle.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.codepassion.doubletriangle.core.designsystem.AntonFontFamily
import io.codepassion.doubletriangle.core.designsystem.WildforceThemeTokens
import io.codepassion.doubletriangle.core.designsystem.liquidGlass
import io.codepassion.doubletriangle.core.designsystem.liquidGlassBackground

/** Editable fitness profile. These are the same inputs consumed by the iOS planner. */
@Composable
fun ProfileScreen(initial: OnboardingProfile, onSave: (OnboardingProfile) -> Unit) {
    var draft by remember(initial) { mutableStateOf(initial) }
    var picker by remember { mutableStateOf<Picker?>(null) }
    LazyColumn(
        modifier = Modifier.fillMaxSize().liquidGlassBackground().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ProfileHero(draft)
        }
        item {
            Section("DATOS PERSONALES") {
                OutlinedTextField(draft.name, { draft = draft.copy(name = it.take(60)) }, Modifier.fillMaxWidth(), label = { Text("Nombre") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    NumberField("Altura (cm)", draft.heightCm.toString(), { draft = draft.copy(heightCm = it.toIntOrNull()?.coerceIn(120, 230) ?: draft.heightCm) }, Modifier.weight(1f))
                    NumberField("Peso (kg)", draft.weightKg.toString(), { draft = draft.copy(weightKg = it.replace(',', '.').toDoubleOrNull()?.coerceIn(30.0, 300.0) ?: draft.weightKg) }, Modifier.weight(1f))
                }
                ChoiceButton("Sexo", draft.gender.title) { picker = Picker.Gender }
                ChoiceButton("Unidades", draft.metricSystem.title) { picker = Picker.Metric }
            }
        }
        item {
            Section("OBJETIVO Y NIVEL") {
                ChoiceButton("Objetivo", draft.goal.title) { picker = Picker.Goal }
                ChoiceButton("Nivel", draft.trainingLevel.title) { picker = Picker.Level }
                ChoiceButton("Actividad diaria", draft.lifestyle.title) { picker = Picker.Lifestyle }
                if (draft.goal.supportsBodyComposition && draft.trainingLevel.supportsBodyComposition) ChoiceButton("Fase corporal", draft.bodyCompositionPhase?.title ?: "Automático") { picker = Picker.Body }
            }
        }
        item {
            Section("ESTRUCTURA DEL PLAN") {
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
                NumberField("Duración preferida (min)", draft.preferredWorkoutDurationMinutes.toString(), { draft = draft.copy(preferredWorkoutDurationMinutes = it.toIntOrNull()?.coerceIn(15, 180) ?: draft.preferredWorkoutDurationMinutes) }, Modifier.fillMaxWidth())
                draft.workoutDays.sortedBy { it.ordinal }.forEach { day ->
                    if (draft.trainingSplitPreference == TrainingSplitPreference.Custom) ChoiceButton(day.title, draft.customWorkoutFocuses[day]?.title ?: "Seleccionar foco") { picker = Picker.Focus(day) }
                }
                TogglePreference("Omitir calentamientos", draft.skipsWarmups) { draft = draft.copy(skipsWarmups = !draft.skipsWarmups) }
                TogglePreference("Omitir vuelta a la calma", draft.skipsCooldowns) { draft = draft.copy(skipsCooldowns = !draft.skipsCooldowns) }
                TogglePreference("Omitir descansos", draft.skipsRestPeriods) { draft = draft.copy(skipsRestPeriods = !draft.skipsRestPeriods) }
                OutlinedTextField(draft.workoutPlannerNotes, { draft = draft.copy(workoutPlannerNotes = it.take(500)) }, Modifier.fillMaxWidth(), label = { Text("Notas para el planificador") }, minLines = 2, maxLines = 4)
            }
        }
        item {
            Section("ENTORNO Y SEGURIDAD") {
                ChoiceButton("Tipo de gimnasio", draft.gymType.title) { picker = Picker.Gym }
                Text("Equipamiento disponible", fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
                Equipment.entries.chunked(2).forEach { pair -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) { pair.forEach { equipment -> ToggleChip(equipment.title, equipment in draft.availableEquipment) { draft = draft.copy(availableEquipment = if (equipment in draft.availableEquipment) draft.availableEquipment - equipment else draft.availableEquipment + equipment) } } } }
                Spacer(Modifier.height(4.dp))
                Text("Restricciones o molestias", fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
                MovementRestriction.entries.chunked(2).forEach { pair -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) { pair.forEach { restriction -> ToggleChip(restriction.title, restriction in draft.movementRestrictions) { draft = draft.copy(movementRestrictions = if (restriction in draft.movementRestrictions) draft.movementRestrictions - restriction else draft.movementRestrictions + restriction) } } } }
            }
        }
        item {
            Button(onClick = { onSave(draft) }, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary, contentColor = WildforceThemeTokens.backgroundSecondary)) { Text("GUARDAR PERFIL", fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(28.dp))
        }
    }
    picker?.let { current -> PickerDialog(current, draft) { updated -> draft = updated; picker = null } }
}

@Composable
private fun ProfileHero(profile: OnboardingProfile) {
    Column(Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(24.dp), emphasized = true).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(profile.name.take(1).uppercase().ifBlank { "W" }, Modifier.size(68.dp).background(WildforceThemeTokens.accentGold, CircleShape).padding(19.dp), fontFamily = AntonFontFamily, style = MaterialTheme.typography.h4, color = Color.White)
            Column {
                Text(profile.name.ifBlank { "Tu perfil" }, fontFamily = AntonFontFamily, style = MaterialTheme.typography.h4, color = WildforceThemeTokens.textPrimary)
                Text("${profile.goal.title} · ${profile.trainingLevel.title}", color = WildforceThemeTokens.textSecondary)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            ProfileStat("OBJETIVO", profile.goal.title, Modifier.weight(1f))
            ProfileStat("NIVEL", profile.trainingLevel.title, Modifier.weight(1f))
            ProfileStat("DÍAS", profile.workoutDays.size.toString(), Modifier.weight(1f))
        }
    }
}

@Composable
private fun ProfileStat(label: String, value: String, modifier: Modifier) {
    Column(modifier.liquidGlass(RoundedCornerShape(13.dp)).padding(9.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary, maxLines = 1)
        Text(label, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary, maxLines = 1)
    }
}

private sealed class Picker { data object Goal : Picker(); data object Level : Picker(); data object Lifestyle : Picker(); data object Split : Picker(); data object Body : Picker(); data object Gym : Picker(); data object Gender : Picker(); data object Metric : Picker(); data class Focus(val day: WorkoutWeekday) : Picker() }

@Composable private fun PickerDialog(picker: Picker, profile: OnboardingProfile, onSelect: (OnboardingProfile) -> Unit) {
    val title: String
    val values: List<Pair<String, (OnboardingProfile) -> OnboardingProfile>>
    when (picker) {
        Picker.Goal -> { title = "Objetivo"; values = FitnessGoal.entries.map { it.title to { p: OnboardingProfile -> p.copy(goal = it) } } }
        Picker.Level -> { title = "Nivel"; values = TrainingLevel.entries.map { it.title to { p: OnboardingProfile -> p.copy(trainingLevel = it) } } }
        Picker.Lifestyle -> { title = "Actividad diaria"; values = LifestyleLevel.entries.map { it.title to { p: OnboardingProfile -> p.copy(lifestyle = it) } } }
        Picker.Split -> { title = "División semanal"; values = TrainingSplitPreference.entries.map { it.title to { p: OnboardingProfile -> p.copy(trainingSplitPreference = it) } } }
        Picker.Body -> { title = "Fase corporal"; values = BodyCompositionPhase.entries.map { it.title to { p: OnboardingProfile -> p.copy(bodyCompositionPhase = it) } } }
        Picker.Gym -> { title = "Tipo de gimnasio"; values = GymType.entries.map { it.title to { p: OnboardingProfile -> p.copy(gymType = it, availableEquipment = it.defaultEquipment) } } }
        Picker.Gender -> { title = "Sexo"; values = Gender.entries.map { it.title to { p: OnboardingProfile -> p.copy(gender = it) } } }
        Picker.Metric -> { title = "Unidades"; values = MetricSystem.entries.map { it.title to { p: OnboardingProfile -> p.copy(metricSystem = it) } } }
        is Picker.Focus -> { title = "Foco de ${picker.day.title}"; values = WorkoutFocus.entries.map { it.title to { p: OnboardingProfile -> p.copy(customWorkoutFocuses = p.customWorkoutFocuses + (picker.day to it)) } } }
    }
    androidx.compose.material.AlertDialog(onDismissRequest = { onSelect(profile) }, title = { Text(title, fontFamily = AntonFontFamily) }, text = { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { values.forEach { (label, transform) -> Text(label, Modifier.fillMaxWidth().clickable { onSelect(transform(profile)) }.padding(12.dp), color = WildforceThemeTokens.textPrimary) } } }, confirmButton = {})
}

@Composable private fun Section(title: String, content: @Composable () -> Unit) { Column(Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(18.dp)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { Text(title, fontFamily = AntonFontFamily, color = WildforceThemeTokens.accentGold); content() } }
@Composable private fun ChoiceButton(label: String, value: String, onClick: () -> Unit) { Row(Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(13.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(label, color = WildforceThemeTokens.textSecondary); Text(value, color = WildforceThemeTokens.textPrimary, fontWeight = FontWeight.Bold) } }
@Composable private fun ToggleChip(label: String, selected: Boolean, onClick: () -> Unit) { Text(label, Modifier.fillMaxWidth(.5f).liquidGlass(RoundedCornerShape(10.dp), emphasized = selected).clickable(onClick = onClick).padding(10.dp), color = if (selected) WildforceThemeTokens.textPrimary else WildforceThemeTokens.textSecondary) }
@Composable private fun NumberField(label: String, value: String, onValueChange: (String) -> Unit, modifier: Modifier) { OutlinedTextField(value, onValueChange, modifier, label = { Text(label) }, singleLine = true) }
@Composable private fun TogglePreference(label: String, selected: Boolean, onClick: () -> Unit) { Row(Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(13.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(label, color = WildforceThemeTokens.textPrimary); Text(if (selected) "SÍ" else "NO", color = if (selected) WildforceThemeTokens.accentGold else WildforceThemeTokens.textSecondary, fontWeight = FontWeight.Bold) } }
