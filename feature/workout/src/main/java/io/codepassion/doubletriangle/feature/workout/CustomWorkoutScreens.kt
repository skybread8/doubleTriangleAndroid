package io.codepassion.doubletriangle.feature.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme

import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.codepassion.doubletriangle.core.designsystem.AntonFontFamily
import io.codepassion.doubletriangle.core.designsystem.WildforceThemeTokens
import io.codepassion.doubletriangle.core.designsystem.liquidGlass
import io.codepassion.doubletriangle.core.designsystem.liquidGlassBackground
import io.codepassion.doubletriangle.core.model.ExerciseSummary
import io.codepassion.doubletriangle.core.model.ExerciseSetStyle
import io.codepassion.doubletriangle.core.model.WorkoutBlockSummary
import io.codepassion.doubletriangle.core.model.WorkoutBlockType
import io.codepassion.doubletriangle.core.model.WorkoutDaySummary

data class CustomWorkoutRequest(val focus: String, val durationMinutes: Int, val equipment: String)

@Composable
internal fun CustomWorkoutsScreen(
    workouts: List<WorkoutDaySummary>,
    gender: String,
    defaultEquipment: String,
    onCreateManual: () -> Unit,
    onCreateAutomatic: (CustomWorkoutRequest) -> Unit,
    onOpen: (WorkoutDaySummary) -> Unit,
    onEdit: (WorkoutDaySummary) -> Unit,
    onDuplicate: (WorkoutDaySummary) -> Unit,
    onDelete: (WorkoutDaySummary) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showsCreationMode by remember { mutableStateOf(false) }
    var showsAutomaticRequest by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<WorkoutDaySummary?>(null) }
    if (showsCreationMode) {
        AlertDialog(
            onDismissRequest = { showsCreationMode = false }, title = { Text("NUEVO ENTRENAMIENTO", fontFamily = AntonFontFamily) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    CreationOption("✦", "AUTOMÁTICO", "Crea una sesión adaptada a tus objetivos con IA.") { showsCreationMode = false; showsAutomaticRequest = true }
                    CreationOption("✎", "MANUAL", "Añade y configura cada ejercicio a mano.") { showsCreationMode = false; onCreateManual() }
                }
            },
            confirmButton = { TextButton(onClick = { showsCreationMode = false }) { Text("CANCELAR") } },
        )
    }
    if (showsAutomaticRequest) {
        AutomaticWorkoutRequestDialog(
            defaultEquipment = defaultEquipment,
            onDismiss = { showsAutomaticRequest = false },
            onGenerate = { request -> showsAutomaticRequest = false; onCreateAutomatic(request) },
        )
    }
    deleting?.let { workout ->
        AlertDialog(
            onDismissRequest = { deleting = null }, title = { Text("¿ELIMINAR ENTRENAMIENTO?") },
            text = { Text(workout.title) },
            confirmButton = { TextButton(onClick = { deleting = null; onDelete(workout) }) { Text("ELIMINAR", color = Color(0xFFC62828)) } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("CANCELAR") } },
        )
    }
    if (workouts.isEmpty()) {
        Column(modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("◆", style = MaterialTheme.typography.h2, color = WildforceThemeTokens.accentGold)
            Text("SIN ENTRENAMIENTOS PERSONALIZADOS", fontFamily = AntonFontFamily, style = MaterialTheme.typography.h5, color = WildforceThemeTokens.textPrimary, textAlign = TextAlign.Center)
            Text("Crea una sesión para entrenar fuera de tu gimnasio habitual o probar algo nuevo.", color = WildforceThemeTokens.textSecondary, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 12.dp))
            PrimaryAction("CREAR PRIMER ENTRENAMIENTO") { showsCreationMode = true }
        }
    } else {
        LazyColumn(modifier, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { PrimaryAction("＋ NUEVO ENTRENAMIENTO") { showsCreationMode = true } }
            items(workouts, key = { it.id }) { workout ->
                Column(Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(18.dp)).clickable { onOpen(workout) }.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RemoteTrainingImage(workoutCoverUrl(workout.focus, gender, workout.order), workout.title, Modifier.size(70.dp).clip(RoundedCornerShape(14.dp)))
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text(workout.title, fontFamily = AntonFontFamily, style = MaterialTheme.typography.h6, color = WildforceThemeTokens.textPrimary)
                            Text("${workout.pathBlocks().flatMap { it.exercises }.size} ejercicios · ${workout.estimatedMinutes} min", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
                            Text(workout.focus.uppercase(), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.accentGold, fontWeight = FontWeight.Bold)
                        }
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
                        SmallAction("EDITAR") { onEdit(workout) }
                        SmallAction("DUPLICAR") { onDuplicate(workout) }
                        SmallAction("BORRAR") { deleting = workout }
                    }
                }
            }
        }
    }
}

@Composable
internal fun CustomWorkoutEditorScreen(initial: WorkoutDaySummary, gender: String, onCancel: () -> Unit, onSave: (WorkoutDaySummary) -> Unit) {
    var workout by remember(initial.id) { mutableStateOf(initial) }
    var blocks by remember(initial.id) { mutableStateOf(initial.editableBlocks()) }
    var selectingExercise by remember { mutableStateOf(false) }
    if (selectingExercise) {
        ExercisePickerScreen(gender, onBack = { selectingExercise = false }) { choice ->
            blocks = (blocks + WorkoutBlockSummary(WorkoutBlockType.Standard, exercises = listOf(ExerciseSummary(choice.name, choice.imageKey, 3, "10", 90)))).workoutOrder()
            selectingExercise = false
        }
        return
    }
    Column(Modifier.fillMaxSize().liquidGlassBackground().padding(horizontal = 16.dp, vertical = 10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("‹ CERRAR", Modifier.clickable(onClick = onCancel).padding(10.dp), color = WildforceThemeTokens.accentGold, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text("EDITOR MANUAL", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            TextField(
                value = workout.title, onValueChange = { workout = workout.copy(title = it) }, modifier = Modifier.fillMaxWidth(),
                label = { Text("Título del entrenamiento") }, singleLine = true,
            )
            Text("ENFOQUE", Modifier.padding(top = 18.dp, bottom = 8.dp), fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Full body", "Empuje", "Tirón", "Piernas").forEach { focus ->
                    Text(
                        focus, Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                            .background(if (workout.focus == focus) WildforceThemeTokens.accentGold else WildforceThemeTokens.textSecondary.copy(alpha = 0.08f))
                            .clickable { workout = workout.copy(focus = focus) }.padding(vertical = 9.dp),
                        color = if (workout.focus == focus) Color.White else WildforceThemeTokens.textPrimary, textAlign = TextAlign.Center, style = MaterialTheme.typography.caption,
                    )
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("EJERCICIOS", fontFamily = AntonFontFamily, style = MaterialTheme.typography.h5, color = WildforceThemeTokens.textPrimary)
                    Text("Duración estimada: ${CustomWorkoutStore.estimateBlockMinutes(blocks)} min", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
                }
                Text("＋ AÑADIR", Modifier.clickable { selectingExercise = true }.padding(10.dp), color = WildforceThemeTokens.accentGold, fontWeight = FontWeight.Bold)
            }
            if (blocks.isEmpty()) {
                Text("Añade al menos un ejercicio para guardar.", Modifier.fillMaxWidth().padding(28.dp), color = WildforceThemeTokens.textSecondary, textAlign = TextAlign.Center)
            }
            blocks.forEachIndexed { blockIndex, block ->
                if (block.type == WorkoutBlockType.Superset) {
                    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⛓  SUPERSERIE", Modifier.weight(1f), fontWeight = FontWeight.Bold, color = WildforceThemeTokens.accentGold)
                            Text("SEPARAR", Modifier.clickable { blocks = blocks.splitSupersetAt(blockIndex) }.padding(9.dp), style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            EditorStepper("RONDAS", block.rounds.toString(), {
                                blocks = blocks.toMutableList().also { it[blockIndex] = block.copy(rounds = (block.rounds - 1).coerceAtLeast(1)) }
                            }, {
                                blocks = blocks.toMutableList().also { it[blockIndex] = block.copy(rounds = block.rounds + 1) }
                            }, Modifier.weight(1f))
                            EditorStepper("DESCANSO FINAL", "${block.restAfterBlockSeconds ?: 0}s", {
                                blocks = blocks.toMutableList().also { it[blockIndex] = block.copy(restAfterBlockSeconds = ((block.restAfterBlockSeconds ?: 0) - 15).coerceAtLeast(0)) }
                            }, {
                                blocks = blocks.toMutableList().also { it[blockIndex] = block.copy(restAfterBlockSeconds = (block.restAfterBlockSeconds ?: 0) + 15) }
                            }, Modifier.weight(1f))
                        }
                        block.exercises.forEachIndexed { exerciseIndex, exercise ->
                            EditableExerciseCard(
                                exercise, gender, exerciseIndex, block.exercises.size,
                                label = "A${exerciseIndex + 1}",
                                inSuperset = true,
                                onChange = { changed -> blocks = blocks.updateBlockExercise(blockIndex, exerciseIndex) { changed.copy(sets = 1, restSeconds = 0) } },
                                onMoveUp = { blocks = blocks.mapIndexed { index, item -> if (index == blockIndex) item.copy(exercises = item.exercises.move(exerciseIndex, exerciseIndex - 1)) else item } },
                                onMoveDown = { blocks = blocks.mapIndexed { index, item -> if (index == blockIndex) item.copy(exercises = item.exercises.move(exerciseIndex, exerciseIndex + 1)) else item } },
                                onRemove = { blocks = blocks.removeBlockExercise(blockIndex, exerciseIndex) },
                            )
                        }
                    }
                } else {
                    val exercise = block.exercises.singleOrNull() ?: return@forEachIndexed
                    val sectionColor = when (block.type) {
                        WorkoutBlockType.Warmup -> Color(0xFFF08A24)
                        WorkoutBlockType.Cooldown -> Color(0xFF4A8FE7)
                        else -> WildforceThemeTokens.textSecondary
                    }
                    Text("SECCIÓN · ${block.type.label.uppercase()}  ›", Modifier.fillMaxWidth().clickable { blocks = blocks.cycleSectionAt(blockIndex) }.padding(top = 8.dp, bottom = 2.dp), style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold, color = sectionColor)
                    EditableExerciseCard(
                        exercise, gender, blockIndex, blocks.size,
                        onChange = { changed -> blocks = blocks.updateBlockExercise(blockIndex, 0) { changed } },
                        onMoveUp = { blocks = blocks.move(blockIndex, blockIndex - 1).workoutOrder() },
                        onMoveDown = { blocks = blocks.move(blockIndex, blockIndex + 1).workoutOrder() },
                        onRemove = { blocks = blocks.removeBlockExercise(blockIndex, 0) },
                    )
                    val next = blocks.getOrNull(blockIndex + 1)
                    if (next?.type == WorkoutBlockType.Standard && next.exercises.size == 1) {
                        Text("⛓  CREAR SUPERSERIE CON EL SIGUIENTE", Modifier.fillMaxWidth().clickable { blocks = blocks.createSupersetAt(blockIndex) }.padding(vertical = 9.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.accentGold)
                    }
                }
            }
        }
        Button(
            onClick = { onSave(workout.withEditableBlocks(blocks).copy(estimatedMinutes = CustomWorkoutStore.estimateBlockMinutes(blocks))) },
            enabled = workout.title.isNotBlank() && blocks.isNotEmpty(), modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary, contentColor = WildforceThemeTokens.backgroundSecondary),
        ) { Text("GUARDAR ENTRENAMIENTO", fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun EditableExerciseCard(exercise: ExerciseSummary, gender: String, index: Int, count: Int, label: String? = null, inSuperset: Boolean = false, onChange: (ExerciseSummary) -> Unit, onMoveUp: () -> Unit, onMoveDown: () -> Unit, onRemove: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 5.dp).liquidGlass(RoundedCornerShape(16.dp)).padding(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RemoteTrainingImage(exerciseImageUrl(exercise.imageKey, gender), exercise.name, Modifier.size(54.dp).clip(RoundedCornerShape(12.dp)))
            label?.let { Text(it, Modifier.padding(start = 9.dp), color = WildforceThemeTokens.accentGold, fontWeight = FontWeight.Bold) }
            Text(exercise.name, Modifier.weight(1f).padding(horizontal = 10.dp), fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
            Text("↑", Modifier.clickable(enabled = index > 0, onClick = onMoveUp).padding(8.dp), color = if (index > 0) WildforceThemeTokens.textPrimary else Color.Transparent)
            Text("↓", Modifier.clickable(enabled = index < count - 1, onClick = onMoveDown).padding(8.dp), color = if (index < count - 1) WildforceThemeTokens.textPrimary else Color.Transparent)
            Text("×", Modifier.clickable(onClick = onRemove).padding(8.dp), color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            if (!inSuperset) {
                EditorStepper("SERIES", exercise.sets.toString(), { onChange(exercise.copy(sets = (exercise.sets - 1).coerceAtLeast(1))) }, { onChange(exercise.copy(sets = exercise.sets + 1)) }, Modifier.weight(1f))
            }
            EditorStepper("REPS", exercise.reps, { onChange(exercise.copy(reps = ((exercise.reps.toIntOrNull() ?: 10) - 1).coerceAtLeast(1).toString())) }, { onChange(exercise.copy(reps = ((exercise.reps.toIntOrNull() ?: 10) + 1).toString())) }, Modifier.weight(1f))
            if (!inSuperset) EditorStepper("DESCANSO", "${exercise.restSeconds}s", { onChange(exercise.copy(restSeconds = (exercise.restSeconds - 15).coerceAtLeast(0))) }, { onChange(exercise.copy(restSeconds = exercise.restSeconds + 15)) }, Modifier.weight(1f))
        }
        Text("TIPO DE SERIE", Modifier.padding(top = 10.dp, bottom = 5.dp), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary, fontWeight = FontWeight.Bold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(ExerciseSetStyle.values()) { style ->
                val selected = exercise.setStyle == style
                Row(
                    Modifier
                        .clip(RoundedCornerShape(11.dp))
                        .background(if (selected) WildforceThemeTokens.accentGold.copy(alpha = 0.18f) else WildforceThemeTokens.textSecondary.copy(alpha = 0.07f))
                        .clickable { onChange(exercise.copy(setStyle = style)) }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("${style.glyph}  ${style.label}", color = if (selected) WildforceThemeTokens.textPrimary else WildforceThemeTokens.textSecondary, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
        StyleParameterControls(exercise, onChange)
}

}

@Composable
private fun StyleParameterControls(exercise: ExerciseSummary, onChange: (ExerciseSummary) -> Unit) {
    val parameters = exercise.setStyleParameters
    val update: (io.codepassion.doubletriangle.core.model.SetStyleParameters) -> Unit = { onChange(exercise.copy(setStyleParameters = it)) }
    val minusPercent = { value: Int -> (value - 5).coerceAtLeast(5) }
    val plusPercent = { value: Int -> (value + 5).coerceAtMost(50) }

    when (exercise.setStyle) {
        ExerciseSetStyle.TopSetBackoff -> Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            EditorStepper("BACKOFF", parameters.backoffSetCount.toString(), { update(parameters.copy(backoffSetCount = (parameters.backoffSetCount - 1).coerceAtLeast(1))) }, { update(parameters.copy(backoffSetCount = parameters.backoffSetCount + 1)) }, Modifier.weight(1f))
            EditorStepper("REDUCCIÓN", "${parameters.backoffWeightPercent}%", { update(parameters.copy(backoffWeightPercent = minusPercent(parameters.backoffWeightPercent))) }, { update(parameters.copy(backoffWeightPercent = plusPercent(parameters.backoffWeightPercent))) }, Modifier.weight(1f))
        }
        ExerciseSetStyle.DropSet -> Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            EditorStepper("DROPS", parameters.dropCount.toString(), { update(parameters.copy(dropCount = (parameters.dropCount - 1).coerceAtLeast(1))) }, { update(parameters.copy(dropCount = parameters.dropCount + 1)) }, Modifier.weight(1f))
            EditorStepper("REDUCCIÓN", "${parameters.dropWeightPercent}%", { update(parameters.copy(dropWeightPercent = minusPercent(parameters.dropWeightPercent))) }, { update(parameters.copy(dropWeightPercent = plusPercent(parameters.dropWeightPercent))) }, Modifier.weight(1f))
        }
        ExerciseSetStyle.RestPause, ExerciseSetStyle.Intervals -> Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            EditorStepper("PAUSA INTERNA", "${parameters.intraSetRestSeconds}s", { update(parameters.copy(intraSetRestSeconds = (parameters.intraSetRestSeconds - 5).coerceAtLeast(5))) }, { update(parameters.copy(intraSetRestSeconds = parameters.intraSetRestSeconds + 5)) }, Modifier.weight(1f))
        }
        ExerciseSetStyle.Tempo -> TextField(
            value = parameters.tempo,
            onValueChange = { value -> update(parameters.copy(tempo = value.filter { it.isDigit() || it == '-' }.take(9))) },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp), label = { Text("TEMPO · BAJADA-PAUSA-SUBIDA-PAUSA") }, singleLine = true,
        )
        else -> Unit
    }
    if (exercise.setStyle != ExerciseSetStyle.Warmup) {
        Row(Modifier.fillMaxWidth().padding(top = 7.dp)) {
            EditorStepper("REPETICIONES EN RESERVA (RIR)", parameters.targetRir.toString(), { update(parameters.copy(targetRir = (parameters.targetRir - 1).coerceAtLeast(0))) }, { update(parameters.copy(targetRir = (parameters.targetRir + 1).coerceAtMost(5))) }, Modifier.weight(1f))
        }
    }
}

@Composable
internal fun AutomaticWorkoutRequestDialog(defaultEquipment: String, onDismiss: () -> Unit, onGenerate: (CustomWorkoutRequest) -> Unit, initialFocus: String = "Full body", initialDuration: Int = 45, title: String = "CREAR CON IA") {
    var focus by remember(initialFocus) { mutableStateOf(initialFocus) }
    var duration by remember(initialDuration) { mutableStateOf(initialDuration.toString()) }
    var equipment by remember(defaultEquipment) { mutableStateOf(defaultEquipment.ifBlank { "Peso corporal" }) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontFamily = AntonFontFamily) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("La IA adaptará la sesión a estos datos.", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
                Text("ENFOQUE", style = MaterialTheme.typography.overline, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textSecondary)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    listOf("Full body", "Empuje", "Tirón", "Piernas").forEach { option ->
                        Text(option, Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(if (focus == option) WildforceThemeTokens.accentGold else WildforceThemeTokens.textSecondary.copy(alpha = 0.08f)).clickable { focus = option }.padding(vertical = 8.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.overline, color = if (focus == option) Color.White else WildforceThemeTokens.textPrimary)
                    }
                }
                TextField(duration, { duration = it.filter(Char::isDigit).take(3) }, label = { Text("Duración (minutos)") }, singleLine = true)
                TextField(equipment, { equipment = it.take(120) }, label = { Text("Equipamiento disponible") }, maxLines = 2)
            }
        },
        confirmButton = { TextButton(onClick = { onGenerate(CustomWorkoutRequest(focus, duration.toIntOrNull()?.coerceIn(15, 180) ?: 45, equipment.trim())) }) { Text("GENERAR", color = WildforceThemeTokens.accentGold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCELAR", color = WildforceThemeTokens.textSecondary) } },
    )
}

@Composable
private fun ExercisePickerScreen(gender: String, onBack: () -> Unit, onSelect: (ExerciseChoice) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val catalog = remember { CustomExerciseCatalog.load(context) }
    var search by remember { mutableStateOf("") }
    val filtered = remember(search, catalog) { catalog.filter { it.name.contains(search, ignoreCase = true) || it.imageKey.contains(search, ignoreCase = true) } }
    Column(Modifier.fillMaxSize().liquidGlassBackground().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("‹ VOLVER", Modifier.clickable(onClick = onBack).padding(10.dp), color = WildforceThemeTokens.accentGold, fontWeight = FontWeight.Bold)
            Text("AÑADIR EJERCICIO", Modifier.weight(1f), textAlign = TextAlign.Center, fontFamily = AntonFontFamily, style = MaterialTheme.typography.h5)
        }
        TextField(search, { search = it }, Modifier.fillMaxWidth().padding(vertical = 10.dp), label = { Text("Buscar ejercicio") }, singleLine = true)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(filtered, key = { it.imageKey }) { choice ->
                Row(Modifier.fillMaxWidth().clickable { onSelect(choice) }.padding(7.dp), verticalAlignment = Alignment.CenterVertically) {
                    RemoteTrainingImage(exerciseImageUrl(choice.imageKey, gender), choice.name, Modifier.size(54.dp).clip(RoundedCornerShape(12.dp)))
                    Text(choice.name, Modifier.weight(1f).padding(horizontal = 12.dp), color = WildforceThemeTokens.textPrimary, fontWeight = FontWeight.SemiBold)
                    Text("＋", color = WildforceThemeTokens.accentGold, style = MaterialTheme.typography.h5)
                }
            }
        }
    }
}

@Composable private fun CreationOption(glyph: String, title: String, description: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(WildforceThemeTokens.textSecondary.copy(alpha = 0.08f)).clickable(onClick = onClick).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(glyph, style = MaterialTheme.typography.h5, color = WildforceThemeTokens.accentGold)
        Column(Modifier.padding(start = 12.dp)) { Text(title, fontWeight = FontWeight.Bold); Text(description, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary) }
    }
}

@Composable private fun PrimaryAction(label: String, onClick: () -> Unit) = Button(onClick, Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(15.dp), colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary, contentColor = WildforceThemeTokens.backgroundSecondary)) { Text(label, fontWeight = FontWeight.Bold) }
@Composable private fun SmallAction(label: String, onClick: () -> Unit) = Text(label, Modifier.clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 6.dp), style = MaterialTheme.typography.caption, color = if (label == "BORRAR") Color(0xFFC62828) else WildforceThemeTokens.textSecondary, fontWeight = FontWeight.Bold)

@Composable private fun EditorStepper(title: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit, modifier: Modifier) {
    Column(modifier.background(WildforceThemeTokens.textSecondary.copy(alpha = 0.07f), RoundedCornerShape(12.dp)).padding(7.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary, maxLines = 1)
        Row(verticalAlignment = Alignment.CenterVertically) { Text("−", Modifier.clickable(onClick = onMinus).padding(5.dp)); Text(value, Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, maxLines = 1); Text("+", Modifier.clickable(onClick = onPlus).padding(5.dp)) }
    }
}

private fun <T> List<T>.swap(first: Int, second: Int): List<T> {
    if (first !in indices || second !in indices) return this
    return toMutableList().also { list -> val value = list[first]; list[first] = list[second]; list[second] = value }
}
