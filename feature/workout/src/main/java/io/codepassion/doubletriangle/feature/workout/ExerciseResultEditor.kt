package io.codepassion.doubletriangle.feature.workout

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import io.codepassion.doubletriangle.core.model.ExerciseSummary
import io.codepassion.doubletriangle.core.model.ExerciseTrackingMode
import io.codepassion.doubletriangle.core.model.ExerciseSetStyle
import io.codepassion.doubletriangle.core.designsystem.WildforceThemeTokens
import kotlin.math.roundToInt

private data class ResultSetDraft(val reps: String, val weight: String)
private data class ResultNumericInput(val field: String, val index: Int = 0, val title: String, val value: Double, val decimal: Boolean, val step: Double)

@Composable
private fun editorString(@androidx.annotation.StringRes id: Int): String {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val language = context.getSharedPreferences("wildforce_profile", 0).getString("app_language", "Español") ?: "Español"
    return remember(id, language, configuration) {
        val localized = android.content.res.Configuration(configuration).apply { setLocale(java.util.Locale.forLanguageTag(workoutLanguageTag(language))) }
        context.createConfigurationContext(localized).resources.getString(id)
    }
}

@Composable
fun ExerciseResultEditorForId(exercise: ExerciseSummary, point: ExerciseAnalyticsPoint, useImperial: Boolean, onDismiss: () -> Unit, onSaved: () -> Unit) {
    val context = LocalContext.current
    val entry = remember(exercise, point) {
        WorkoutHistoryStore.history(context, exercise).filter {
            if (point.resultId != null) it.remoteResultId == point.resultId
            else it.remoteResultId == null && it.timestampMillis == point.timestampMillis && it.workoutId == point.workoutId && it.plannedExerciseId == point.plannedExerciseId
        }.singleOrNull()
    }
    if (entry != null) ExerciseResultEditor(exercise, entry, useImperial, onDismiss) { updated, kind ->
        if (WorkoutHistoryStore.saveEdit(context, exercise, entry, updated, kind)) onSaved()
        onDismiss()
    }
}

@Composable
internal fun ExerciseResultEditor(
    exercise: ExerciseSummary,
    entry: ExerciseHistoryEntry,
    useImperial: Boolean,
    onDismiss: () -> Unit,
    onSave: (ExerciseHistoryEntry, String) -> Unit,
) {
    val context = LocalContext.current
    val metadata = remember(exercise.imageKey) {
        runCatching {
            val json = org.json.JSONObject(context.resources.openRawResource(R.raw.ios_exercise_editor_metadata).bufferedReader().use { it.readText() })
            json.optJSONObject(exercise.imageKey?.lowercase(java.util.Locale.ROOT).orEmpty())
        }.getOrNull()
    }
    val coach = remember(exercise.imageKey) { CoachExerciseMediaStore.latest(context, exercise.imageKey) }
    val iosMode = metadata?.optString("mode") ?: coach?.trackingMode ?: when (coach?.category) {
        "strength", "bodyweight" -> "reps"
        "cardio" -> "durationMinutesAndDistance"
        "mobility" -> "durationMinutes"
        "hiit" -> "durationSeconds"
        else -> null
    }
    val mode = when (iosMode) {
        "durationSeconds", "durationMinutes" -> ExerciseTrackingMode.Duration
        "durationMinutesAndDistance" -> ExerciseTrackingMode.DurationAndDistance
        "reps" -> ExerciseTrackingMode.Repetitions
        else -> exercise.trackingMode
    }
    val weightFactor = if (useImperial) 2.2046226218 else 1.0
    val distanceFactor = if (useImperial) .621371192 else 1.0
    var sets by remember(entry) {
        val count = maxOf(entry.sets, entry.setDetails.size, 1)
        mutableStateOf((0 until count).map { index ->
            val detail = entry.setDetails.getOrNull(index)
            ResultSetDraft((detail?.reps ?: entry.totalReps).toString(), "%.1f".format(java.util.Locale.US, (detail?.weightKg ?: entry.maxWeightKg) * weightFactor))
        })
    }
    val usesSeconds = mode == ExerciseTrackingMode.Duration && (iosMode == "durationSeconds" || (iosMode == null && exercise.targetDurationSeconds != null))
    var duration by remember(entry) { mutableStateOf((if (usesSeconds) entry.durationSeconds else entry.durationSeconds / 60).toString()) }
    var distance by remember(entry) { mutableStateOf("%.1f".format(java.util.Locale.US, entry.distanceKm * distanceFactor)) }
    var numeric by remember { mutableStateOf<ResultNumericInput?>(null) }
    val repsTitle = editorString(R.string.wf_editor_reps)
    val weightTitle = editorString(R.string.wf_editor_weight)
    val durationTitle = editorString(R.string.wf_editor_duration)
    val distanceTitle = editorString(R.string.wf_editor_distance)
    val coachWeight = coach?.let { if (it.targetMetrics.isNotEmpty()) "weight" in it.targetMetrics else it.supportsExternalLoad || it.category == "strength" } == true
    val showWeight = metadata?.optBoolean("weight") == true || coachWeight || entry.setDetails.any { it.weightKg > 0 } || entry.maxWeightKg > 0
    fun number(value: String): Double? = value.replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0 }
    val valid = if (mode == ExerciseTrackingMode.Repetitions) sets.all { number(it.reps) != null && (!showWeight || number(it.weight) != null) }
        else number(duration) != null && (mode != ExerciseTrackingMode.DurationAndDistance || number(distance) != null)
    val save: () -> Unit = {
        if (mode == ExerciseTrackingMode.Repetitions) {
            val details = sets.mapIndexed { index, draft -> SetPerformance(index + 1, number(draft.reps)!!.roundToInt(), if (showWeight) number(draft.weight)!! / weightFactor else 0.0, entry.setDetails.getOrNull(index)?.setStyle ?: ExerciseSetStyle.Straight) }
            onSave(entry.copy(sets = details.size, totalReps = details.sumOf { it.reps }, maxWeightKg = details.maxOfOrNull { it.weightKg } ?: 0.0, volumeKg = details.sumOf { it.reps * it.weightKg }, setDetails = details), if (showWeight) "weightedReps" else "reps")
        } else {
            val seconds = number(duration)!!.roundToInt() * if (usesSeconds) 1 else 60
            val km = if (mode == ExerciseTrackingMode.DurationAndDistance) number(distance)!! / distanceFactor else entry.distanceKm
            val pace = if (mode == ExerciseTrackingMode.DurationAndDistance && seconds > 0 && km > 0) (seconds / km).roundToInt() else if (mode == ExerciseTrackingMode.DurationAndDistance) null else entry.paceSecondsPerKm
            onSave(entry.copy(durationSeconds = seconds, distanceKm = km, paceSecondsPerKm = pace), if (mode == ExerciseTrackingMode.DurationAndDistance) "distance" else if (usesSeconds) "seconds" else "minutes")
        }
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Surface(modifier = Modifier.fillMaxWidth().heightIn(max = maxHeight * .9f), shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp), color = WildforceThemeTokens.background) {
            Column(Modifier.fillMaxWidth()) {
                Box(Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp).size(width = 36.dp, height = 5.dp).clip(RoundedCornerShape(50)).background(WildforceThemeTokens.textSecondary.copy(alpha = .35f)))
                Box(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp)) {
                    Text(editorString(R.string.wf_editor_edit), Modifier.align(Alignment.Center), style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, color = WildforceThemeTokens.textPrimary)
                    TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterStart)) { Text(editorString(R.string.wf_editor_cancel), color = WildforceThemeTokens.accent) }
                    TextButton(enabled = valid, onClick = save, modifier = Modifier.align(Alignment.CenterEnd)) { Text(editorString(R.string.wf_editor_save), color = WildforceThemeTokens.accent, fontWeight = FontWeight.SemiBold) }
                }
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(WildforceThemeTokens.backgroundSecondary).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    if (mode == ExerciseTrackingMode.Repetitions) {
                        Text(editorString(R.string.wf_editor_sets), style = MaterialTheme.typography.h6, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
                        Text(tutorialText("Tap a value to edit it"), style = MaterialTheme.typography.body2, color = WildforceThemeTokens.textSecondary)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(editorString(R.string.wf_editor_set), Modifier.width(38.dp), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
                            Text(repsTitle, Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
                            if (showWeight) Text(weightTitle, Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
                            Spacer(Modifier.width(32.dp))
                        }
                        sets.forEachIndexed { index, draft ->
                            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(WildforceThemeTokens.textSecondary.copy(alpha = .09f)).padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("${index + 1}", Modifier.width(38.dp), fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textPrimary)
                                TextButton(onClick = { numeric = ResultNumericInput("reps", index, repsTitle, number(draft.reps) ?: 0.0, false, 1.0) }, modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(WildforceThemeTokens.background)) { Text(draft.reps, color = WildforceThemeTokens.textPrimary, fontWeight = FontWeight.SemiBold) }
                                if (showWeight) TextButton(onClick = { numeric = ResultNumericInput("weight", index, "$weightTitle (${if (useImperial) "lb" else "kg"})", number(draft.weight) ?: 0.0, true, if (useImperial) 5.0 else 2.5) }, modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(WildforceThemeTokens.background)) { Text(draft.weight, color = WildforceThemeTokens.textPrimary, fontWeight = FontWeight.SemiBold) }
                                TextButton(onClick = { sets = sets.filterIndexed { i, _ -> i != index } }, modifier = Modifier.width(40.dp), enabled = sets.size > 1, contentPadding = PaddingValues(0.dp)) { Text("⊖", color = MaterialTheme.colors.error) }
                            }
                        }
                        TextButton(onClick = { sets = sets + sets.last() }, modifier = Modifier.fillMaxWidth().border(1.dp, WildforceThemeTokens.accent.copy(alpha = .4f), RoundedCornerShape(10.dp))) { Text("⊕  ${tutorialText("Add set")}", color = WildforceThemeTokens.accent) }
                    } else {
                        Text(editorString(R.string.wf_editor_results), style = MaterialTheme.typography.h6, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(WildforceThemeTokens.textSecondary.copy(alpha = .09f)).padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            val title = "$durationTitle (${if (usesSeconds) "s" else "min"})"
                            Text(title, color = WildforceThemeTokens.textPrimary)
                            TextButton(onClick = { numeric = ResultNumericInput("duration", title = title, value = number(duration) ?: 0.0, decimal = false, step = if (usesSeconds) 5.0 else 1.0) }, modifier = Modifier.width(124.dp).clip(RoundedCornerShape(10.dp)).background(WildforceThemeTokens.background)) { Text(duration, color = WildforceThemeTokens.textPrimary) }
                        }
                        if (mode == ExerciseTrackingMode.DurationAndDistance) Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(WildforceThemeTokens.textSecondary.copy(alpha = .09f)).padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            val title = "$distanceTitle (${if (useImperial) "mi" else "km"})"
                            Text(title, color = WildforceThemeTokens.textPrimary)
                            TextButton(onClick = { numeric = ResultNumericInput("distance", title = title, value = number(distance) ?: 0.0, decimal = true, step = .1) }, modifier = Modifier.width(124.dp).clip(RoundedCornerShape(10.dp)).background(WildforceThemeTokens.background)) { Text(distance, color = WildforceThemeTokens.textPrimary) }
                        }
                        if (mode == ExerciseTrackingMode.DurationAndDistance && (number(duration) ?: 0.0) > 0 && (number(distance) ?: 0.0) > 0) {
                            val paceSeconds = ((number(duration) ?: 0.0) * 60 / (number(distance) ?: 1.0)).roundToInt()
                            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(WildforceThemeTokens.accent.copy(alpha = .10f)).padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(editorString(R.string.wf_editor_pace), color = WildforceThemeTokens.textPrimary)
                                Text("${paceSeconds / 60}:${(paceSeconds % 60).toString().padStart(2, '0')} ${if (useImperial) "min/mi" else "min/km"}", fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textPrimary)
                            }
                        }
                    }
                    }
                }
            }
        }
        }
    }
    numeric?.let { input ->
        WorkoutNumericInputDialog(input.title, input.value, input.decimal, input.step, onDismissRequest = { numeric = null }) { value ->
            val formatted = if (input.decimal) "%.1f".format(java.util.Locale.US, value) else value.roundToInt().toString()
            when (input.field) {
                "reps" -> sets = sets.toMutableList().also { it[input.index] = it[input.index].copy(reps = formatted) }
                "weight" -> sets = sets.toMutableList().also { it[input.index] = it[input.index].copy(weight = formatted) }
                "duration" -> duration = formatted
                "distance" -> distance = formatted
            }
            numeric = null
        }
    }
}
