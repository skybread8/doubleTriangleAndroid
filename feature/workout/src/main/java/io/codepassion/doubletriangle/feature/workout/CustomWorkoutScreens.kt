package io.codepassion.doubletriangle.feature.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme

import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.codepassion.doubletriangle.core.designsystem.AntonFontFamily
import io.codepassion.doubletriangle.core.designsystem.WildforceThemeTokens
import io.codepassion.doubletriangle.core.designsystem.liquidGlass
import io.codepassion.doubletriangle.core.designsystem.liquidGlassBackground
import io.codepassion.doubletriangle.core.model.ExerciseSummary
import io.codepassion.doubletriangle.core.model.ExerciseSetStyle
import io.codepassion.doubletriangle.core.model.WorkoutBlockSummary
import io.codepassion.doubletriangle.core.model.WorkoutBlockType
import io.codepassion.doubletriangle.core.model.WorkoutDaySummary

/** Mirrors the one-off workout request used by iOS.  Keep [focus] for the
 * existing adaptation entry point, while preserving the complete selection
 * rather than reducing it to the text displayed by the picker. */
data class CustomWorkoutRequest(
    val focus: String,
    val durationMinutes: Int,
    val equipment: String,
    val goal: String? = null,
    val focuses: Set<String> = emptySet(),
    val muscleGroups: Set<String> = emptySet(),
    val includeWarmup: Boolean = true,
    val includeCooldown: Boolean = true,
)

@Composable
internal fun CustomWorkoutsScreen(
    workouts: List<WorkoutDaySummary>,
    gender: String,
    defaultEquipment: String,
    equipmentPresets: List<Pair<String, String>> = emptyList(),
    onCreateManual: () -> Unit,
    onCreateAutomatic: (CustomWorkoutRequest) -> Unit,
    onOpen: (WorkoutDaySummary) -> Unit,
    onEdit: (WorkoutDaySummary) -> Unit,
    onDuplicate: (WorkoutDaySummary) -> Unit,
    onDelete: (WorkoutDaySummary) -> Unit,
    onAdapt: (WorkoutDaySummary) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showsCreationMode by remember { mutableStateOf(false) }
    var showsAutomaticRequest by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<WorkoutDaySummary?>(null) }
    if (showsCreationMode) {
        AlertDialog(
            onDismissRequest = { showsCreationMode = false }, title = { Text(stringResource(R.string.workout_custom_new), fontFamily = AntonFontFamily) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    CreationOption("✦", "AUTOMÁTICO", "Crea una sesión adaptada a tus objetivos con IA.") { showsCreationMode = false; showsAutomaticRequest = true }
                    CreationOption("✎", "MANUAL", "Añade y configura cada ejercicio a mano.") { showsCreationMode = false; onCreateManual() }
                }
            },
            confirmButton = { TextButton(onClick = { showsCreationMode = false }) { Text(stringResource(R.string.workout_cancel)) } },
        )
    }
    if (showsAutomaticRequest) {
        AutomaticWorkoutRequestDialog(
            defaultEquipment = defaultEquipment,
            equipmentPresets = equipmentPresets,
            onDismiss = { showsAutomaticRequest = false },
            onGenerate = { request -> showsAutomaticRequest = false; onCreateAutomatic(request) },
        )
    }
    deleting?.let { workout ->
        AlertDialog(
            onDismissRequest = { deleting = null }, title = { Text(stringResource(R.string.workout_delete_confirmation)) },
            text = { Text(workout.title) },
            confirmButton = { TextButton(onClick = { deleting = null; onDelete(workout) }) { Text(stringResource(R.string.workout_delete), color = Color(0xFFC62828)) } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.workout_cancel)) } },
        )
    }
    if (workouts.isEmpty()) {
        Column(
            modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("🏋", style = MaterialTheme.typography.h3, modifier = Modifier.padding(bottom = 12.dp))
            Text(
                "NO HAY ENTRENAMIENTOS PERSONALIZADOS",
                fontFamily = AntonFontFamily,
                style = MaterialTheme.typography.h5,
                color = WildforceThemeTokens.textPrimary,
                textAlign = TextAlign.Center,
            )
            Text(
                stringResource(R.string.workout_custom_empty_description),
                modifier = Modifier.padding(top = 8.dp),
                color = WildforceThemeTokens.textSecondary,
                textAlign = TextAlign.Center,
            )
            PrimaryAction(
                stringResource(R.string.workout_custom_create),
                Modifier.padding(top = 24.dp),
            ) { showsCreationMode = true }
        }
    } else {
        LazyColumn(
            modifier,
            contentPadding = PaddingValues(start = 16.dp, top = 10.dp, end = 16.dp, bottom = 160.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                TextButton(
                    onClick = { showsCreationMode = true },
                    modifier = Modifier.fillMaxWidth().height(52.dp).liquidGlass(RoundedCornerShape(18.dp)),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, tint = WildforceThemeTokens.accentGold, modifier = Modifier.size(20.dp))
                    Text(
                        stringResource(R.string.workout_custom_new),
                        modifier = Modifier.padding(start = 8.dp),
                        color = WildforceThemeTokens.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            items(workouts, key = { it.id }) { workout ->
                var menuExpanded by remember(workout.id) { mutableStateOf(false) }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onOpen(workout) },
                ) {
                    RemoteTrainingImage(
                        workoutCoverUrl(workout.focus, gender, workout.order),
                        workout.title,
                        Modifier.fillMaxSize(),
                    )
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Black.copy(alpha = 0.10f), Color.Black.copy(alpha = 0.84f)),
                                ),
                            ),
                    )
                    Column(
                        Modifier.align(Alignment.BottomStart).padding(start = 14.dp, end = 58.dp, bottom = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            workout.title.uppercase(),
                            fontFamily = AntonFontFamily,
                            style = MaterialTheme.typography.h5,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(workout.focus, style = MaterialTheme.typography.caption, color = Color.White.copy(alpha = 0.84f))
                            Text("${workout.estimatedMinutes} min", style = MaterialTheme.typography.caption, color = Color.White.copy(alpha = 0.84f))
                        }
                    }
                    Box(Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 8.dp)) {
                        Box(Modifier.size(48.dp).clickable { menuExpanded = true }, contentAlignment = Alignment.Center) {
                            Box(Modifier.size(30.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.20f)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.MoreHoriz, contentDescription = stringResource(R.string.workout_actions), tint = Color.White, modifier = Modifier.size(19.dp))
                            }
                        }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            DropdownMenuItem(onClick = { menuExpanded = false; onEdit(workout) }) { Text(stringResource(R.string.workout_edit)) }
                            DropdownMenuItem(onClick = { menuExpanded = false; onDuplicate(workout) }) { Text(stringResource(R.string.workout_duplicate)) }
                            DropdownMenuItem(onClick = { menuExpanded = false; onAdapt(workout) }) { Text(stringResource(R.string.workout_adapt_location)) }
                        }
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
    var focusMenuExpanded by remember { mutableStateOf(false) }
    if (selectingExercise) {
        ExercisePickerScreen(gender, onBack = { selectingExercise = false }) { choice ->
            blocks = (blocks + WorkoutBlockSummary(WorkoutBlockType.Standard, exercises = listOf(ExerciseSummary(choice.name, choice.imageKey, 3, "10", 90)))).workoutOrder()
            selectingExercise = false
        }
        return
    }
    // The app-level navigation rail is drawn above destinations. Keep the editor's
    // persistent action outside that rail on tall physical devices as well as on
    // smaller phones.
    Column(
        Modifier
            .fillMaxSize()
            .liquidGlassBackground()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .padding(bottom = 92.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(CircleShape).background(WildforceThemeTokens.textPrimary).clickable(onClick = onCancel), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.workout_close), tint = WildforceThemeTokens.backgroundSecondary, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text(
                    stringResource(R.string.workout_custom_editor_title),
                    fontFamily = AntonFontFamily,
                    style = MaterialTheme.typography.h6.copy(fontSize = 20.sp),
                    color = WildforceThemeTokens.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(stringResource(R.string.workout_custom_editor_subtitle), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
            }
            TextButton(onClick = { selectingExercise = true }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                Text(stringResource(R.string.workout_add), color = WildforceThemeTokens.accentGold, style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold)
            }
            TextButton(
                onClick = { onSave(workout.withEditableBlocks(blocks).copy(title = workout.title.trim(), estimatedMinutes = CustomWorkoutStore.estimateBlockMinutes(blocks))) },
                enabled = workout.title.trim().isNotBlank() && blocks.any { it.exercises.isNotEmpty() },
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
            ) {
                Text(stringResource(R.string.workout_ready), color = WildforceThemeTokens.accentGold, style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold)
            }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            TextField(
                value = workout.title, onValueChange = { workout = workout.copy(title = it) }, modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.workout_title_label)) }, singleLine = true,
            )
            Text(stringResource(R.string.workout_focus), Modifier.padding(top = 18.dp, bottom = 8.dp), fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
            Box {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(WildforceThemeTokens.textSecondary.copy(alpha = 0.08f)).clickable { focusMenuExpanded = true }.padding(horizontal = 14.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Enfoque", Modifier.weight(1f), color = WildforceThemeTokens.textSecondary)
                    Text(workout.focus, color = WildforceThemeTokens.textPrimary, fontWeight = FontWeight.Bold)
                    Text("⌄", Modifier.padding(start = 10.dp), color = WildforceThemeTokens.textSecondary)
                }
                DropdownMenu(expanded = focusMenuExpanded, onDismissRequest = { focusMenuExpanded = false }) {
                    listOf("Full body", "Empuje", "Tirón", "Piernas").forEach { focus ->
                        DropdownMenuItem(onClick = { workout = workout.copy(focus = focus); focusMenuExpanded = false }) { Text(focus) }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.workout_exercises), fontFamily = AntonFontFamily, style = MaterialTheme.typography.h5, color = WildforceThemeTokens.textPrimary)
                    Text(stringResource(R.string.workout_estimated_duration, CustomWorkoutStore.estimateBlockMinutes(blocks)), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
                }
                Text(stringResource(R.string.workout_add_exercise), Modifier.clickable { selectingExercise = true }.padding(10.dp), color = WildforceThemeTokens.accentGold, fontWeight = FontWeight.Bold)
            }
            if (blocks.isEmpty()) {
            Text(stringResource(R.string.workout_empty_editor), Modifier.fillMaxWidth().padding(28.dp), color = WildforceThemeTokens.textSecondary, textAlign = TextAlign.Center, maxLines = 2)
            }
            blocks.forEachIndexed { blockIndex, block ->
                if (block.type == WorkoutBlockType.Superset) {
                    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.workout_superset), Modifier.weight(1f), fontWeight = FontWeight.Bold, color = WildforceThemeTokens.accentGold)
                            Text(stringResource(R.string.workout_split_superset), Modifier.clickable { blocks = blocks.splitSupersetAt(blockIndex) }.padding(9.dp), style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            EditorStepper(stringResource(R.string.workout_rounds), block.rounds.toString(), {
                                blocks = blocks.toMutableList().also { it[blockIndex] = block.copy(rounds = (block.rounds - 1).coerceAtLeast(1)) }
                            }, {
                                blocks = blocks.toMutableList().also { it[blockIndex] = block.copy(rounds = block.rounds + 1) }
                            }, Modifier.weight(1f))
                            EditorStepper(stringResource(R.string.workout_final_rest), "${block.restAfterBlockSeconds ?: 0}s", {
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
                        WorkoutBlockType.Warmup -> WildforceThemeTokens.warmupAccent
                        WorkoutBlockType.Cooldown -> WildforceThemeTokens.cooldownAccent
                        else -> WildforceThemeTokens.textSecondary
                    }
                    Text(stringResource(R.string.workout_section, block.type.label.uppercase()), Modifier.fillMaxWidth().clickable { blocks = blocks.cycleSectionAt(blockIndex) }.padding(top = 8.dp, bottom = 2.dp), style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold, color = sectionColor)
                    EditableExerciseCard(
                        exercise, gender, blockIndex, blocks.size,
                        onChange = { changed -> blocks = blocks.updateBlockExercise(blockIndex, 0) { changed } },
                        onMoveUp = { blocks = blocks.move(blockIndex, blockIndex - 1).workoutOrder() },
                        onMoveDown = { blocks = blocks.move(blockIndex, blockIndex + 1).workoutOrder() },
                        onRemove = { blocks = blocks.removeBlockExercise(blockIndex, 0) },
                    )
                    val next = blocks.getOrNull(blockIndex + 1)
                    if (next?.type == WorkoutBlockType.Standard && next.exercises.size == 1) {
                        Text(stringResource(R.string.workout_create_superset_next), Modifier.fillMaxWidth().clickable { blocks = blocks.createSupersetAt(blockIndex) }.padding(vertical = 9.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.accentGold)
                    }
                }
            }
        }
    }
}

@Composable
private fun EditableExerciseCard(exercise: ExerciseSummary, gender: String, index: Int, count: Int, label: String? = null, inSuperset: Boolean = false, onChange: (ExerciseSummary) -> Unit, onMoveUp: () -> Unit, onMoveDown: () -> Unit, onRemove: () -> Unit) {
    val moveUpDescription = stringResource(R.string.workout_move_exercise_up)
    val moveDownDescription = stringResource(R.string.workout_move_exercise_down)
    val removeDescription = stringResource(R.string.workout_remove_exercise)
    Column(Modifier.fillMaxWidth().padding(vertical = 5.dp).liquidGlass(RoundedCornerShape(16.dp)).padding(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RemoteTrainingImage(exerciseImageUrl(exercise.imageKey, gender), exercise.name, Modifier.size(54.dp).clip(RoundedCornerShape(12.dp)))
            label?.let { Text(it, Modifier.padding(start = 9.dp), color = WildforceThemeTokens.accentGold, fontWeight = FontWeight.Bold) }
            Text(exercise.name, Modifier.weight(1f).padding(horizontal = 10.dp), fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("↑", Modifier.semantics { contentDescription = moveUpDescription }.clickable(enabled = index > 0, onClick = onMoveUp).padding(8.dp), color = if (index > 0) WildforceThemeTokens.textPrimary else Color.Transparent)
            Text("↓", Modifier.semantics { contentDescription = moveDownDescription }.clickable(enabled = index < count - 1, onClick = onMoveDown).padding(8.dp), color = if (index < count - 1) WildforceThemeTokens.textPrimary else Color.Transparent)
            Text("×", Modifier.semantics { contentDescription = removeDescription }.clickable(onClick = onRemove).padding(8.dp), color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
        }
        Text(stringResource(R.string.workout_tracking), Modifier.padding(top = 9.dp, bottom = 5.dp), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary, fontWeight = FontWeight.Bold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val modes = listOf(
                io.codepassion.doubletriangle.core.model.ExerciseTrackingMode.Repetitions to "REPETICIONES",
                io.codepassion.doubletriangle.core.model.ExerciseTrackingMode.Duration to "TIEMPO",
                io.codepassion.doubletriangle.core.model.ExerciseTrackingMode.DurationAndDistance to "TIEMPO + DISTANCIA",
            )
            items(modes) { (mode, title) ->
                val selected = exercise.trackingMode == mode
                Text(title, Modifier.clip(RoundedCornerShape(11.dp)).background(if (selected) WildforceThemeTokens.accentGold.copy(alpha = 0.18f) else WildforceThemeTokens.textSecondary.copy(alpha = 0.07f)).clickable {
                    onChange(when (mode) {
                        io.codepassion.doubletriangle.core.model.ExerciseTrackingMode.Repetitions -> exercise.copy(trackingMode = mode, targetDurationSeconds = null, targetDurationMinutes = null, targetDistanceKm = null)
                        io.codepassion.doubletriangle.core.model.ExerciseTrackingMode.Duration -> exercise.copy(trackingMode = mode, reps = "${exercise.targetDurationSeconds ?: 30} s", targetDurationSeconds = exercise.targetDurationSeconds ?: 30, targetDurationMinutes = null, targetDistanceKm = null)
                        io.codepassion.doubletriangle.core.model.ExerciseTrackingMode.DurationAndDistance -> exercise.copy(trackingMode = mode, reps = "", targetDurationSeconds = null, targetDurationMinutes = exercise.targetDurationMinutes ?: 10, targetDistanceKm = exercise.targetDistanceKm ?: 1.0)
                    })
                }.padding(horizontal = 10.dp, vertical = 8.dp), color = WildforceThemeTokens.textPrimary, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            if (!inSuperset) {
                EditorStepper(stringResource(R.string.workout_sets), exercise.sets.toString(), { onChange(exercise.copy(sets = (exercise.sets - 1).coerceAtLeast(1))) }, { onChange(exercise.copy(sets = exercise.sets + 1)) }, Modifier.weight(1f))
            }
            if (exercise.trackingMode == io.codepassion.doubletriangle.core.model.ExerciseTrackingMode.Repetitions) EditorStepper(stringResource(R.string.workout_reps), exercise.reps, {
                val reps = ((exercise.reps.toIntOrNull() ?: 10) - 1).coerceAtLeast(1)
                onChange(exercise.copy(reps = reps.toString(), repRange = io.codepassion.doubletriangle.core.model.RepRange(reps, reps)))
            }, {
                val reps = ((exercise.reps.toIntOrNull() ?: 10) + 1).coerceAtLeast(1)
                onChange(exercise.copy(reps = reps.toString(), repRange = io.codepassion.doubletriangle.core.model.RepRange(reps, reps)))
            }, Modifier.weight(1f))
            if (!inSuperset) EditorStepper(stringResource(R.string.workout_rest), "${exercise.restSeconds}s", { onChange(exercise.copy(restSeconds = (exercise.restSeconds - 15).coerceAtLeast(0))) }, { onChange(exercise.copy(restSeconds = exercise.restSeconds + 15)) }, Modifier.weight(1f))
        }
        if (exercise.trackingMode != io.codepassion.doubletriangle.core.model.ExerciseTrackingMode.Repetitions) {
            TextField(
                value = (exercise.targetDurationSeconds ?: exercise.targetDurationMinutes?.times(60) ?: 0).toString(),
                onValueChange = { value -> value.filter(Char::isDigit).toIntOrNull()?.coerceIn(1, 86_400)?.let { seconds -> onChange(exercise.copy(reps = "$seconds s", targetDurationSeconds = seconds, targetDurationMinutes = null)) } },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp), label = { Text(stringResource(R.string.workout_target_duration_seconds)) }, singleLine = true,
            )
        }
        if (exercise.trackingMode == io.codepassion.doubletriangle.core.model.ExerciseTrackingMode.DurationAndDistance) {
            TextField(
                value = exercise.targetDistanceKm?.toString().orEmpty(),
                onValueChange = { value -> value.filter { it.isDigit() || it == ',' || it == '.' }.replace(',', '.').toDoubleOrNull()?.coerceIn(0.0, 1_000.0)?.let { distance -> onChange(exercise.copy(targetDistanceKm = distance)) } },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp), label = { Text(stringResource(R.string.workout_target_distance_km)) }, singleLine = true,
            )
        }
        TextField(
            value = exercise.targetWeightKg?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() }.orEmpty(),
            onValueChange = { value ->
                val clean = value.filter { it.isDigit() || it == ',' || it == '.' }.take(7)
                onChange(exercise.copy(targetWeightKg = clean.replace(',', '.').toDoubleOrNull()?.coerceIn(0.0, 750.0)))
            },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            label = { Text(stringResource(R.string.workout_target_weight_optional)) },
            singleLine = true,
        )
        Text(stringResource(R.string.workout_set_type), Modifier.padding(top = 10.dp, bottom = 5.dp), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary, fontWeight = FontWeight.Bold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(ExerciseSetStyle.values()) { style ->
                val selected = exercise.setStyle == style
                Row(
                    Modifier
                        .clip(RoundedCornerShape(11.dp))
                        .background(if (selected) WildforceThemeTokens.accentGold.copy(alpha = 0.18f) else WildforceThemeTokens.textSecondary.copy(alpha = 0.07f))
                        .clickable {
                            val parameters = if (style == exercise.setStyle) exercise.setStyleParameters else io.codepassion.doubletriangle.core.model.SetStyleParameters(
                                appliesToFinalSetOnly = style in setOf(ExerciseSetStyle.DropSet, ExerciseSetStyle.RestPause),
                            )
                            onChange(exercise.copy(setStyle = style, setStyleParameters = parameters))
                        }
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
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp), label = { Text(stringResource(R.string.workout_tempo)) }, singleLine = true,
        )
        else -> Unit
    }
    val hint = when (exercise.setStyle) {
        ExerciseSetStyle.TopSetBackoff -> "Una serie pesada y series de descarga."
        ExerciseSetStyle.DropSet -> "Reduce la carga al terminar cada caída."
        ExerciseSetStyle.RestPause -> "Pausa breve dentro de la misma serie."
        ExerciseSetStyle.Intervals -> "Alterna trabajo y pausa interna."
        ExerciseSetStyle.Tempo -> "Controla el ritmo de cada repetición."
        ExerciseSetStyle.AscendingPyramid -> "Aumenta la carga progresivamente."
        else -> null
    }
    hint?.let { Text(it, Modifier.padding(top = 4.dp), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary) }
    if (exercise.setStyle !in setOf(ExerciseSetStyle.Warmup, ExerciseSetStyle.TopSetBackoff)) {
        Row(Modifier.fillMaxWidth().padding(top = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.workout_final_set_only), Modifier.weight(1f), style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textSecondary)
            Text(
                stringResource(if (parameters.appliesToFinalSetOnly) R.string.workout_yes else R.string.workout_no),
                Modifier.clickable { update(parameters.copy(appliesToFinalSetOnly = !parameters.appliesToFinalSetOnly)) }.padding(10.dp),
                color = WildforceThemeTokens.accentGold,
                fontWeight = FontWeight.Bold,
            )
        }
    }
    if (exercise.setStyle != ExerciseSetStyle.Warmup) {
        Row(Modifier.fillMaxWidth().padding(top = 7.dp)) {
            EditorStepper("REPETICIONES EN RESERVA (RIR)", parameters.targetRir.toString(), { update(parameters.copy(targetRir = (parameters.targetRir - 1).coerceAtLeast(0))) }, { update(parameters.copy(targetRir = (parameters.targetRir + 1).coerceAtMost(5))) }, Modifier.weight(1f))
        }
    }
}

@Composable
internal fun AutomaticWorkoutRequestDialog(defaultEquipment: String, equipmentPresets: List<Pair<String, String>> = emptyList(), onDismiss: () -> Unit, onGenerate: (CustomWorkoutRequest) -> Unit, initialFocus: String = "Full body", initialDuration: Int = 45, title: String = "CREAR CON IA") {
    var selectedGoal by remember { mutableStateOf<String?>(null) }
    var selectedFocuses by remember { mutableStateOf(emptySet<String>()) }
    var selectedMuscles by remember { mutableStateOf(emptySet<String>()) }
    var duration by remember(initialDuration) { mutableStateOf(initialDuration.coerceIn(15, 180)) }
    var equipment by remember(defaultEquipment) { mutableStateOf(defaultEquipment.ifBlank { "Peso corporal" }) }
    var goalMenuExpanded by remember { mutableStateOf(false) }
    val goals = listOf("Perder peso", "Ganar músculo", "Ganar fuerza", "Mejorar resistencia", "Mejorar movilidad", "Recomposición corporal", "Fitness general")
    val focuses = listOf("Cuerpo completo", "Tren superior", "Tren inferior", "Empuje", "Tirón", "Piernas", "Core", "Cardio")
    val muscles = listOf("Pecho", "Espalda", "Hombros", "Bíceps", "Tríceps", "Cuádriceps", "Isquiotibiales", "Glúteos", "Gemelos", "Abdominales", "Cardio")
    var includeWarmup by remember { mutableStateOf(true) }
    var includeCooldown by remember { mutableStateOf(true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontFamily = AntonFontFamily) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text(stringResource(R.string.workout_ai_adapts), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
                Text(stringResource(R.string.workout_goal), style = MaterialTheme.typography.overline, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textSecondary)
                Box {
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(WildforceThemeTokens.textSecondary.copy(alpha = 0.08f)).clickable { goalMenuExpanded = true }.padding(horizontal = 14.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(selectedGoal ?: stringResource(R.string.workout_default_goal), Modifier.weight(1f), color = WildforceThemeTokens.textPrimary, fontWeight = FontWeight.Bold)
                        Text("⌄", color = WildforceThemeTokens.textSecondary)
                    }
                    DropdownMenu(expanded = goalMenuExpanded, onDismissRequest = { goalMenuExpanded = false }) {
                        DropdownMenuItem(onClick = { selectedGoal = null; goalMenuExpanded = false }) { Text(stringResource(R.string.workout_default_goal)) }
                        goals.forEach { goal -> DropdownMenuItem(onClick = { selectedGoal = goal; goalMenuExpanded = false }) { Text(goal) } }
                    }
                }
                Text("ENFOQUE", style = MaterialTheme.typography.overline, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textSecondary)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(focuses) { option ->
                        val selected = option in selectedFocuses
                        Text(option, Modifier.clip(RoundedCornerShape(11.dp)).background(if (selected) WildforceThemeTokens.accentGold.copy(alpha = 0.18f) else WildforceThemeTokens.textSecondary.copy(alpha = 0.07f)).clickable { selectedFocuses = selectedFocuses.toMutableSet().also { if (!it.add(option)) it.remove(option) } }.padding(horizontal = 10.dp, vertical = 8.dp), color = WildforceThemeTokens.textPrimary, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                    }
                }
                Text(stringResource(R.string.workout_muscle_groups), style = MaterialTheme.typography.overline, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textSecondary)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { items(muscles) { muscle ->
                    val selected = muscle in selectedMuscles
                    Text(muscle, Modifier.clip(RoundedCornerShape(11.dp)).background(if (selected) WildforceThemeTokens.accentGold.copy(alpha = 0.18f) else WildforceThemeTokens.textSecondary.copy(alpha = 0.07f)).clickable { selectedMuscles = selectedMuscles.toMutableSet().also { if (!it.add(muscle)) it.remove(muscle) } }.padding(horizontal = 10.dp, vertical = 8.dp), color = WildforceThemeTokens.textPrimary, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                } }
                Text(stringResource(R.string.workout_estimated_duration_label), style = MaterialTheme.typography.overline, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textSecondary)
                EditorStepper("MINUTOS", duration.toString(), { duration = (duration - 5).coerceAtLeast(15) }, { duration = (duration + 5).coerceAtMost(180) }, Modifier.fillMaxWidth())
                if (equipmentPresets.isNotEmpty()) {
                    Text(stringResource(R.string.workout_training_location), style = MaterialTheme.typography.overline, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textSecondary)
                    equipmentPresets.forEach { (name, presetEquipment) ->
                        Text(name, Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(if (equipment == presetEquipment) WildforceThemeTokens.accentGold.copy(alpha = 0.16f) else WildforceThemeTokens.textSecondary.copy(alpha = 0.07f)).clickable { equipment = presetEquipment }.padding(horizontal = 10.dp, vertical = 8.dp), color = WildforceThemeTokens.textPrimary, fontWeight = FontWeight.SemiBold)
                    }
                }
                TextField(equipment, { equipment = it.take(120) }, label = { Text(stringResource(R.string.workout_equipment_available)) }, maxLines = 2)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.workout_include_warmup), Modifier.weight(1f), style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold)
                    Text(stringResource(if (includeWarmup) R.string.workout_yes else R.string.workout_no), Modifier.clickable { includeWarmup = !includeWarmup }.padding(10.dp), color = WildforceThemeTokens.accentGold, fontWeight = FontWeight.Bold)
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.workout_include_cooldown), Modifier.weight(1f), style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold)
                    Text(stringResource(if (includeCooldown) R.string.workout_yes else R.string.workout_no), Modifier.clickable { includeCooldown = !includeCooldown }.padding(10.dp), color = WildforceThemeTokens.accentGold, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = { TextButton(onClick = {
            val fallbackFocus = focuses.firstOrNull { it.equals(initialFocus, ignoreCase = true) } ?: "Cuerpo completo"
            val focuses = selectedFocuses.ifEmpty { setOf(fallbackFocus) }
            onGenerate(CustomWorkoutRequest(focuses.first(), duration, equipment.trim().ifBlank { defaultEquipment.ifBlank { "Peso corporal" } }, selectedGoal, focuses, selectedMuscles, includeWarmup, includeCooldown))
        }) { Text(stringResource(R.string.workout_generate), color = WildforceThemeTokens.accentGold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.workout_cancel), color = WildforceThemeTokens.textSecondary) } },
    )
}

@Composable
internal fun ExercisePickerScreen(
    gender: String,
    onBack: () -> Unit,
    replacing: ExerciseSummary? = null,
    onSelect: (ExerciseChoice) -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val catalog = remember { CustomExerciseCatalog.load(context) }
    var search by remember { mutableStateOf("") }
    val replacementSections = remember(catalog, replacing) {
        replacing?.let { exerciseReplacementSections(catalog, it) }
    }
    val visibleSections = remember(search, catalog, replacementSections) {
        val source = replacementSections ?: ExerciseReplacementSections(emptyList(), emptyList(), catalog)
        fun filtered(choices: List<ExerciseChoice>) = choices.filter {
            it.name.contains(search, ignoreCase = true) || it.imageKey.contains(search, ignoreCase = true)
        }
        ExerciseReplacementSections(filtered(source.recommendedSubstitutes), filtered(source.sameMuscleGroup), filtered(source.allOtherExercises))
    }
    Column(Modifier.fillMaxSize().liquidGlassBackground().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.workout_back), Modifier.clickable(onClick = onBack).padding(10.dp), color = WildforceThemeTokens.accentGold, fontWeight = FontWeight.Bold)
            Text(stringResource(if (replacing == null) R.string.workout_add_exercise_title else R.string.workout_swap_exercise_title), Modifier.weight(1f), textAlign = TextAlign.Center, fontFamily = AntonFontFamily, style = MaterialTheme.typography.h5)
        }
        TextField(search, { search = it }, Modifier.fillMaxWidth().padding(vertical = 10.dp), label = { Text(stringResource(R.string.workout_search_exercise)) }, singleLine = true)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            fun androidx.compose.foundation.lazy.LazyListScope.exerciseRows(choices: List<ExerciseChoice>) {
                items(choices, key = { it.imageKey }) { choice ->
                    Row(Modifier.fillMaxWidth().clickable { onSelect(choice) }.padding(7.dp), verticalAlignment = Alignment.CenterVertically) {
                        RemoteTrainingImage(exerciseImageUrl(choice.imageKey, gender), choice.name, Modifier.size(54.dp).clip(RoundedCornerShape(12.dp)))
                        Text(choice.name, Modifier.weight(1f).padding(horizontal = 12.dp), color = WildforceThemeTokens.textPrimary, fontWeight = FontWeight.SemiBold)
                        Text("＋", color = WildforceThemeTokens.accentGold, style = MaterialTheme.typography.h5)
                    }
                }
            }
            if (replacing != null && search.isBlank()) {
                if (visibleSections.recommendedSubstitutes.isNotEmpty()) {
                    item { ExercisePickerSectionTitle(stringResource(R.string.workout_recommended_substitutes)) }
                    exerciseRows(visibleSections.recommendedSubstitutes)
                }
                if (visibleSections.sameMuscleGroup.isNotEmpty()) {
                    item { ExercisePickerSectionTitle(stringResource(R.string.workout_same_muscle_group)) }
                    exerciseRows(visibleSections.sameMuscleGroup)
                }
                item { ExercisePickerSectionTitle(stringResource(R.string.workout_all_exercises)) }
                exerciseRows(visibleSections.allOtherExercises)
            } else {
                exerciseRows(visibleSections.all)
            }
        }
    }
}

@Composable
private fun ExercisePickerSectionTitle(title: String) {
    Text(title, Modifier.padding(top = 12.dp, bottom = 4.dp), color = WildforceThemeTokens.textPrimary, fontWeight = FontWeight.Bold)
}

@Composable private fun CreationOption(glyph: String, title: String, description: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(WildforceThemeTokens.textSecondary.copy(alpha = 0.08f)).clickable(onClick = onClick).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(glyph, style = MaterialTheme.typography.h5, color = WildforceThemeTokens.accentGold)
        Column(Modifier.padding(start = 12.dp)) { Text(title, fontWeight = FontWeight.Bold); Text(description, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary) }
    }
}

@Composable private fun PrimaryAction(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) = Button(onClick, modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(15.dp), colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary, contentColor = WildforceThemeTokens.backgroundSecondary)) { Text(label, fontWeight = FontWeight.Bold) }
@Composable private fun SmallAction(label: String, onClick: () -> Unit) = Text(label, Modifier.clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 6.dp), style = MaterialTheme.typography.caption, color = if (label == "BORRAR") Color(0xFFC62828) else WildforceThemeTokens.textSecondary, fontWeight = FontWeight.Bold)

@Composable private fun EditorStepper(title: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit, modifier: Modifier) {
    // Keep the 48 dp touch target, but do not turn it into the visual affordance.
    // iOS uses compact stepper glyphs in the manual editor; the previous full-size
    // white discs made three controls dominate a single exercise card.
    Column(modifier.background(WildforceThemeTokens.textSecondary.copy(alpha = 0.07f), RoundedCornerShape(12.dp)).padding(horizontal = 4.dp, vertical = 3.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = MaterialTheme.typography.overline, color = WildforceThemeTokens.textSecondary, maxLines = 1)
        Row(verticalAlignment = Alignment.CenterVertically) {
            val decreaseDescription = stringResource(R.string.workout_decrease, title)
            Box(Modifier.size(48.dp).semantics { contentDescription = decreaseDescription }.clickable(onClick = onMinus), contentAlignment = Alignment.Center) {
                Box(Modifier.size(30.dp).clip(CircleShape).background(WildforceThemeTokens.textPrimary), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Remove, contentDescription = null, tint = WildforceThemeTokens.backgroundSecondary, modifier = Modifier.size(16.dp))
                }
            }
            Text(value, Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold, maxLines = 1)
            val increaseDescription = stringResource(R.string.workout_increase, title)
            Box(Modifier.size(48.dp).semantics { contentDescription = increaseDescription }.clickable(onClick = onPlus), contentAlignment = Alignment.Center) {
                Box(Modifier.size(30.dp).clip(CircleShape).background(WildforceThemeTokens.textPrimary), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Add, contentDescription = null, tint = WildforceThemeTokens.backgroundSecondary, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

private fun <T> List<T>.swap(first: Int, second: Int): List<T> {
    if (first !in indices || second !in indices) return this
    return toMutableList().also { list -> val value = list[first]; list[first] = list[second]; list[second] = value }
}
