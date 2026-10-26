package io.codepassion.doubletriangle.feature.workout

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.codepassion.doubletriangle.core.designsystem.AntonFontFamily
import io.codepassion.doubletriangle.core.designsystem.WildforceThemeTokens
import io.codepassion.doubletriangle.core.model.ExerciseSummary
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

internal data class ExerciseGuide(val description: String, val instructions: List<String>, val tips: List<String>)

private enum class ExerciseDetailsSection(val title: String) {
    Instructions("Instrucciones"),
    History("Historial"),
    Profile("Perfil"),
    Related("Relacionados"),
}

private data class LocalExerciseProfile(
    val level: String = "Principiante",
    val workingWeightKg: Double? = null,
    val estimatedOneRepMaxKg: Double? = null,
    val maxReps: Int? = null,
    val preferredRepRangeMin: Int? = null,
    val preferredRepRangeMax: Int? = null,
    val typicalDurationMinutes: Int? = null,
    val typicalDistanceKm: Double? = null,
    val typicalPaceSecondsPerKm: Int? = null,
    val notes: String = "",
)

private object LocalExerciseProfileStore {
    private const val preferencesName = "wildforce_exercise_profiles"

    fun load(context: Context, exercise: ExerciseSummary): LocalExerciseProfile? {
        val key = exercise.historyKey()
        val preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
        val local = if (preferences.contains("$key.level")) {
            LocalExerciseProfile(
                level = preferences.getString("$key.level", "Principiante") ?: "Principiante",
                workingWeightKg = preferences.getString("$key.workingWeightKg", null)?.toDoubleOrNull(),
                estimatedOneRepMaxKg = preferences.getString("$key.estimatedOneRepMaxKg", null)?.toDoubleOrNull(),
                maxReps = preferences.getInt("$key.maxReps", 0).takeIf { it > 0 },
                preferredRepRangeMin = preferences.getInt("$key.preferredRepRangeMin", 0).takeIf { it > 0 },
                preferredRepRangeMax = preferences.getInt("$key.preferredRepRangeMax", 0).takeIf { it > 0 },
                typicalDurationMinutes = preferences.getInt("$key.typicalDurationMinutes", 0).takeIf { it > 0 },
                typicalDistanceKm = preferences.getString("$key.typicalDistanceKm", null)?.toDoubleOrNull(),
                typicalPaceSecondsPerKm = preferences.getInt("$key.typicalPaceSecondsPerKm", 0).takeIf { it > 0 },
                notes = preferences.getString("$key.notes", "") ?: "",
            )
        } else {
            null
        }
        val remote = remoteProfile(context, exercise)
        return local?.mergeMissing(remote) ?: remote
    }

    fun create(context: Context, exercise: ExerciseSummary): LocalExerciseProfile {
        val profile = LocalExerciseProfile()
        save(context, exercise, profile)
        return profile
    }

    fun save(context: Context, exercise: ExerciseSummary, profile: LocalExerciseProfile) {
        context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE).edit()
            .putString("${exercise.historyKey()}.level", profile.level)
            .putString("${exercise.historyKey()}.workingWeightKg", profile.workingWeightKg?.toString())
            .putString("${exercise.historyKey()}.estimatedOneRepMaxKg", profile.estimatedOneRepMaxKg?.toString())
            .putInt("${exercise.historyKey()}.maxReps", profile.maxReps ?: 0)
            .putInt("${exercise.historyKey()}.preferredRepRangeMin", profile.preferredRepRangeMin ?: 0)
            .putInt("${exercise.historyKey()}.preferredRepRangeMax", profile.preferredRepRangeMax ?: 0)
            .putInt("${exercise.historyKey()}.typicalDurationMinutes", profile.typicalDurationMinutes ?: 0)
            .putString("${exercise.historyKey()}.typicalDistanceKm", profile.typicalDistanceKm?.toString())
            .putInt("${exercise.historyKey()}.typicalPaceSecondsPerKm", profile.typicalPaceSecondsPerKm ?: 0)
            .putString("${exercise.historyKey()}.notes", profile.notes.take(500))
            .apply()
    }

    private fun remoteProfile(context: Context, exercise: ExerciseSummary): LocalExerciseProfile? = runCatching {
        val raw = context.getSharedPreferences("wildforce_remote_sync", Context.MODE_PRIVATE)
            .getString("exercise_profiles", null) ?: return null
        val keys = listOfNotNull(exercise.imageKey, exercise.name).map(::normalizedKey).toSet()
        val profile = JSONArray(raw).let { records ->
            (0 until records.length()).asSequence()
                .map { records.getJSONObject(it) }
                .firstOrNull { record ->
                    val exerciseKey = normalizedKey(record.optString("exercise"))
                    exerciseKey in keys && record.optString("deleted_at").let { it.isBlank() || it.equals("null", ignoreCase = true) }
                }
        } ?: return null
        LocalExerciseProfile(
            level = trainingLevelLabel(profile.optString("level")),
            workingWeightKg = profile.optionalDouble("working_weight", "workingWeight"),
            estimatedOneRepMaxKg = profile.optionalDouble("estimated_one_rep_max", "estimatedOneRepMax"),
            maxReps = profile.optionalInt("max_reps", "maxReps"),
            preferredRepRangeMin = profile.optionalInt("preferred_rep_range_min", "preferredRepRangeMin"),
            preferredRepRangeMax = profile.optionalInt("preferred_rep_range_max", "preferredRepRangeMax"),
            typicalDurationMinutes = profile.optionalInt("typical_duration_minutes", "typicalDurationMinutes"),
            typicalDistanceKm = profile.optionalDouble("typical_distance_km", "typicalDistanceKm"),
            typicalPaceSecondsPerKm = profile.optionalInt("typical_pace_seconds_per_km", "typicalPaceSecondsPerKm"),
            notes = profile.optString("notes").takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }.orEmpty(),
        )
    }.getOrNull()

    private fun LocalExerciseProfile.mergeMissing(remote: LocalExerciseProfile?): LocalExerciseProfile = remote?.let {
        copy(
            workingWeightKg = workingWeightKg ?: it.workingWeightKg,
            estimatedOneRepMaxKg = estimatedOneRepMaxKg ?: it.estimatedOneRepMaxKg,
            maxReps = maxReps ?: it.maxReps,
            preferredRepRangeMin = preferredRepRangeMin ?: it.preferredRepRangeMin,
            preferredRepRangeMax = preferredRepRangeMax ?: it.preferredRepRangeMax,
            typicalDurationMinutes = typicalDurationMinutes ?: it.typicalDurationMinutes,
            typicalDistanceKm = typicalDistanceKm ?: it.typicalDistanceKm,
            typicalPaceSecondsPerKm = typicalPaceSecondsPerKm ?: it.typicalPaceSecondsPerKm,
            notes = notes.takeIf(String::isNotBlank) ?: it.notes,
        )
    } ?: this

    private fun normalizedKey(value: String): String = value.filter(Char::isLetterOrDigit).lowercase(Locale.ROOT)

    private fun trainingLevelLabel(value: String): String = when (value.lowercase(Locale.ROOT)) {
        "beginner", "principiante" -> "Principiante"
        "intermediate", "intermedio" -> "Intermedio"
        "advanced", "avanzado" -> "Avanzado"
        else -> value.takeIf(String::isNotBlank) ?: "Principiante"
    }

    private fun JSONObject.optionalDouble(vararg names: String): Double? = names.asSequence()
        .mapNotNull { name -> optDouble(name, Double.NaN).takeIf(Double::isFinite) }
        .firstOrNull()
        ?.takeIf { it > 0 }

    private fun JSONObject.optionalInt(vararg names: String): Int? = names.asSequence()
        .mapNotNull { name -> optInt(name, 0).takeIf { it > 0 } }
        .firstOrNull()
}

internal object ExerciseGuideRepository {
    private var guides: Map<String, ExerciseGuide>? = null

    fun get(context: Context, imageKey: String?): ExerciseGuide? {
        if (imageKey == null) return null
        val loaded = guides ?: load(context).also { guides = it }
        return loaded[imageKey]
    }

    private fun load(context: Context): Map<String, ExerciseGuide> = runCatching {
        val root = JSONObject(context.resources.openRawResource(R.raw.exercise_guide_es).bufferedReader().use { it.readText() })
        buildMap {
            root.keys().forEach { key ->
                val item = root.getJSONObject(key)
                put(key, ExerciseGuide(item.optString("description"), item.stringList("instructions"), item.stringList("tips")))
            }
        }
    }.getOrDefault(emptyMap())

    private fun JSONObject.stringList(name: String): List<String> {
        val array = optJSONArray(name) ?: return emptyList()
        return List(array.length()) { array.getString(it) }
    }
}

@Composable
fun ExerciseGuideScreen(exercise: ExerciseSummary, gender: String, onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val guide = remember(exercise.imageKey) { ExerciseGuideRepository.get(context, exercise.imageKey) }
    val coachMedia = remember(exercise.imageKey) { CoachExerciseMediaStore.latest(context, exercise.imageKey) }
    val visualMetadata = remember(exercise.imageKey) { ExerciseVisualCatalog.metadata(exercise.imageKey) }
    val equipmentVisuals = remember(exercise.imageKey) { ExerciseVisualCatalog.equipmentVisualsFor(exercise.imageKey) }
    val history = remember(exercise.imageKey) { WorkoutHistoryStore.history(context, exercise) }
    var profile by remember(exercise.imageKey) { mutableStateOf(LocalExerciseProfileStore.load(context, exercise)) }
    var section by remember(exercise.imageKey) { mutableStateOf(ExerciseDetailsSection.Instructions) }
    var relatedExercise by remember(exercise.imageKey) { mutableStateOf<ExerciseSummary?>(null) }
    var appeared by remember(exercise.imageKey) { mutableStateOf(false) }
    LaunchedEffect(exercise.imageKey) { appeared = true }

    relatedExercise?.let { related ->
        ExerciseGuideScreen(related, gender) { relatedExercise = null }
        return
    }

    Box(Modifier.fillMaxSize().background(WildforceThemeTokens.background)) {
        RemoteTrainingImage(coachMedia?.imageUrl ?: exerciseImageUrl(exercise.imageKey, gender), exerciseDisplayName(exercise.imageKey, exercise.name), Modifier.fillMaxWidth().height(500.dp))
        Box(Modifier.fillMaxWidth().height(500.dp).background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent, WildforceThemeTokens.background))))
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 112.dp)) {
            Box(Modifier.padding(14.dp).size(48.dp).clip(CircleShape).background(WildforceThemeTokens.imageControlBackground).semantics { contentDescription = "Volver" }.clickable(onClick = onBack), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", tint = WildforceThemeTokens.imageControlContent, modifier = Modifier.size(22.dp))
            }
            Box(Modifier.fillMaxWidth().height(255.dp)) {
                Row(
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 20.dp, end = 20.dp, bottom = 12.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    equipmentVisuals.forEach { equipment ->
                        Box(
                            Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.82f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            androidx.compose.foundation.Image(
                                painter = androidx.compose.ui.res.painterResource(equipment.drawable),
                                contentDescription = equipment.label,
                                modifier = Modifier.size(52.dp),
                            )
                        }
                    }
                }
            }
            AnimatedVisibility(
                visible = appeared,
                enter = fadeIn(tween(280, delayMillis = 80)) + slideInVertically(tween(320, delayMillis = 80)) { it / 12 },
            ) {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 38.dp, topEnd = 38.dp)).background(WildforceThemeTokens.backgroundSecondary).padding(20.dp)) {
                Text((coachMedia?.name ?: exerciseDisplayName(exercise.imageKey, exercise.name)).uppercase(), fontFamily = AntonFontFamily, fontSize = 34.sp, color = WildforceThemeTokens.textPrimary, maxLines = 2)
                Text("${exercise.sets} series · ${exercise.reps} reps", color = WildforceThemeTokens.textSecondary)
                Text(
                    if (coachMedia?.isUnilateral == true || ExerciseVisualCatalog.isUnilateral(exercise.imageKey)) "UNILATERAL" else "BILATERAL",
                    Modifier
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(WildforceThemeTokens.textPrimary)
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                    color = WildforceThemeTokens.primaryButtonText,
                    style = MaterialTheme.typography.caption,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(22.dp))
                Text("MÚSCULOS PRINCIPALES", fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
                MuscleStrip(exercise.imageKey, onDarkBackground = false, modifier = Modifier.padding(vertical = 8.dp))
                if (!visualMetadata?.secondary.isNullOrEmpty()) {
                    Text("MÚSCULOS SECUNDARIOS", Modifier.padding(top = 10.dp), fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        visualMetadata.secondary.forEach { muscle ->
                            Column(
                                Modifier.width(84.dp).clip(RoundedCornerShape(14.dp)).background(WildforceThemeTokens.textSecondary.copy(alpha = 0.10f)).padding(vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                androidx.compose.foundation.Image(
                                    painter = androidx.compose.ui.res.painterResource(muscle.drawable),
                                    contentDescription = muscle.label,
                                    modifier = Modifier.size(48.dp),
                                )
                                Text(muscle.label, color = WildforceThemeTokens.textPrimary, style = MaterialTheme.typography.caption, maxLines = 1)
                            }
                        }
                    }
                }
                ExerciseDetailsTabs(section) { section = it }
                when (section) {
                    ExerciseDetailsSection.Instructions -> InstructionsContent(context, exercise, guide, coachMedia)
                    ExerciseDetailsSection.History -> HistoryContent(history, exercise)
                    ExerciseDetailsSection.Profile -> ProfileContent(
                        context = context,
                        exercise = exercise,
                        profile = profile,
                        history = history,
                        onCreate = { profile = LocalExerciseProfileStore.create(context, exercise) },
                        onNotesChanged = { value ->
                            profile?.let { current ->
                                val updated = current.copy(notes = value)
                                profile = updated
                                LocalExerciseProfileStore.save(context, exercise, updated)
                            }
                        },
                    )
                    ExerciseDetailsSection.Related -> RelatedContent(context, exercise, gender, onSelect = { relatedExercise = it })
                }
                Spacer(Modifier.height(30.dp))
                }
            }
        }
    }
}

@Composable
private fun ExerciseDetailsTabs(selected: ExerciseDetailsSection, onSelected: (ExerciseDetailsSection) -> Unit) {
    TabRow(
        selectedTabIndex = selected.ordinal,
        backgroundColor = WildforceThemeTokens.backgroundSecondary,
        contentColor = WildforceThemeTokens.textPrimary,
    ) {
        ExerciseDetailsSection.entries.forEach { item ->
            Tab(
                selected = selected == item,
                onClick = { onSelected(item) },
                text = { Text(item.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.caption) },
            )
        }
    }
}

@Composable
private fun InstructionsContent(context: Context, exercise: ExerciseSummary, guide: ExerciseGuide?, coachMedia: CoachExerciseMedia?) {
    coachMedia?.notes?.trim()?.takeIf(String::isNotBlank)?.let { notes ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(20.dp), tint = WildforceThemeTokens.textPrimary)
            Column(Modifier.weight(1f)) {
                Text("NOTAS DEL ENTRENADOR", fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
                Text(notes, Modifier.padding(top = 6.dp), color = WildforceThemeTokens.textSecondary)
            }
        }
        Divider(Modifier.padding(top = 14.dp))
    }
    Text(guide?.description?.takeIf(String::isNotBlank) ?: coachMedia?.notes?.takeIf(String::isNotBlank) ?: "Ejecuta el movimiento con control y mantén una postura estable.", color = WildforceThemeTokens.textSecondary, modifier = Modifier.padding(vertical = 12.dp))
    if (!exercise.imageKey.isNullOrBlank()) RemoteTrainingImage(
        url = coachMedia?.imageUrl ?: exerciseTutorialImageUrl(exercise.imageKey), contentDescription = "Tutorial de ${exerciseDisplayName(exercise.imageKey, exercise.name)}",
        modifier = Modifier.fillMaxWidth().height(230.dp).clip(RoundedCornerShape(24.dp)),
        contentScale = androidx.compose.ui.layout.ContentScale.Fit,
    )
    coachMedia?.youtubeUrl?.let { url ->
        androidx.compose.material.OutlinedButton(onClick = {
            context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)))
        }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) { Text("VER VÍDEO DEL ENTRENADOR") }
    }
    val instructions = guide?.instructions?.takeIf { it.isNotEmpty() }
        ?: coachMedia?.instructions?.takeIf { it.isNotEmpty() }
        ?: listOf("Coloca el cuerpo en la posición inicial.", "Realiza el recorrido sin rebotes.", "Vuelve lentamente a la posición inicial.")
    SectionTitle("PASOS")
    instructions.forEachIndexed { index, instruction ->
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
            // iOS uses the general accent for numbered instruction steps;
            // warm gold is reserved for workout-specific emphasis.
            Box(Modifier.size(24.dp).clip(CircleShape).background(WildforceThemeTokens.accent), contentAlignment = Alignment.Center) {
                Text("${index + 1}", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.caption)
            }
            Text(instruction, Modifier.padding(start = 12.dp).weight(1f), color = WildforceThemeTokens.textPrimary)
        }
    }
    val tips = guide?.tips?.takeIf { it.isNotEmpty() } ?: coachMedia?.tips.orEmpty()
    if (tips.isNotEmpty()) {
        SectionTitle("CONSEJOS")
        tips.forEach { tip ->
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // Match SwiftUI's `.yellow` tip icon instead of the Android
                // workout gold used by progress/emphasis surfaces.
                Icon(Icons.Filled.Lightbulb, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.Yellow)
                Text(tip, Modifier.weight(1f), color = WildforceThemeTokens.textSecondary)
            }
        }
    }
}

@Composable
private fun HistoryContent(history: List<ExerciseHistoryEntry>, exercise: ExerciseSummary) {
    if (history.isEmpty()) {
        EmptyDetailsState(Icons.Filled.BarChart, "SIN HISTORIAL", "Completa este ejercicio para ver aquí tus sesiones anteriores.")
        return
    }
    exerciseHistoryChartSeries(exercise, history).forEach { series ->
        ExerciseHistoryChartCard(series)
    }
    SectionTitle("HISTORIAL")
    history.forEach { entry ->
        Column(
            Modifier.fillMaxWidth().padding(vertical = 4.dp).background(WildforceThemeTokens.textSecondary.copy(alpha = 0.07f), RoundedCornerShape(14.dp)).padding(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(entry.timestampMillis)), fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
                    Text(historyEntrySummary(entry, false), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(formatTrainingWeight(entry.maxWeightKg, false), fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
                    Text(formatVolumeSummary(entry.volumeKg), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
                }
            }
            if (entry.note != null) Text("“${entry.note}”", Modifier.padding(top = 4.dp), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ExerciseHistoryChartCard(series: ExerciseHistoryChartSeries) {
    var selectedIndex by remember(series) { mutableStateOf<Int?>(null) }
    val accent = WildforceThemeTokens.accent
    val selected = selectedIndex?.let(series.points::getOrNull) ?: series.points.last()
    val values = series.points.map(ExerciseHistoryChartPoint::value)
    val low = values.minOrNull() ?: 0.0
    val high = values.maxOrNull() ?: 0.0
    val padding = maxOf((high - low) * .15, 1.0)
    val lower = low - padding
    val upper = high + padding
    Column(
        Modifier.fillMaxWidth().padding(bottom = 12.dp).background(WildforceThemeTokens.textSecondary.copy(alpha = .07f), RoundedCornerShape(24.dp)).padding(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(series.metric.title, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
            Spacer(Modifier.weight(1f))
            Text("${series.metric.displayValue(low)} – ${series.metric.displayValue(high)}", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
        }
        Text(series.metric.displayValue(selected.value), Modifier.padding(top = 6.dp), style = MaterialTheme.typography.h6, color = WildforceThemeTokens.textPrimary)
        Canvas(
            Modifier.fillMaxWidth().height(90.dp).padding(top = 10.dp)
                .pointerInput(series) {
                    detectTapGestures { offset ->
                        val step = size.width / (series.points.size - 1).coerceAtLeast(1)
                        selectedIndex = (offset.x / step).toInt().coerceIn(0, series.points.lastIndex)
                    }
                },
        ) {
            val step = size.width / (series.points.size - 1).coerceAtLeast(1)
            fun point(index: Int, value: Double): Offset = Offset(
                x = step * index,
                y = size.height - ((value - lower) / (upper - lower)).toFloat() * size.height,
            )
            val path = Path()
            series.points.forEachIndexed { index, entry ->
                val offset = point(index, entry.value)
                if (index == 0) path.moveTo(offset.x, offset.y) else path.lineTo(offset.x, offset.y)
            }
            drawPath(path, accent, style = Stroke(3f * density, cap = StrokeCap.Round, join = StrokeJoin.Round))
            series.points.forEachIndexed { index, entry ->
                val offset = point(index, entry.value)
                drawCircle(accent, if (index == selectedIndex) 5f * density else 3f * density, offset)
            }
        }
    }
}

private fun formatVolumeSummary(volumeKg: Double): String =
    String.format(java.util.Locale.getDefault(), "%.0f kg vol.", volumeKg)

@Composable
private fun ProfileContent(
    context: Context,
    exercise: ExerciseSummary,
    profile: LocalExerciseProfile?,
    history: List<ExerciseHistoryEntry>,
    onCreate: () -> Unit,
    onNotesChanged: (String) -> Unit,
) {
    val locale = LocalLocale.current.platformLocale
    if (profile == null) {
        EmptyDetailsState(Icons.Filled.Person, "SIN PERFIL", "Crea un perfil para guardar notas e información específica de este ejercicio.")
        Button(onClick = onCreate, colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary, contentColor = WildforceThemeTokens.backgroundSecondary), modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) { Text("CREAR PERFIL") }
        return
    }
    SectionTitle("PERFIL")
    ProfileValue("Nivel", profile.level)
    profile.workingWeightKg?.let { ProfileValue("Peso de trabajo", formatTrainingWeight(it, false)) }
    profile.estimatedOneRepMaxKg?.let { ProfileValue("1RM estimado", formatTrainingWeight(it, false)) }
    (profile.maxReps ?: history.maxOfOrNull { it.totalReps }.takeIf { reps -> reps?.let { it > 0 } == true })?.let { ProfileValue("Máximas repeticiones", "$it reps") }
    if (profile.preferredRepRangeMin != null && profile.preferredRepRangeMax != null) {
        val range = if (profile.preferredRepRangeMin == profile.preferredRepRangeMax) {
            "${profile.preferredRepRangeMin} reps"
        } else {
            "${profile.preferredRepRangeMin}-${profile.preferredRepRangeMax} reps"
        }
        ProfileValue("Rango de repeticiones", range)
    }
    (profile.typicalDurationMinutes ?: history.firstOrNull()?.durationSeconds?.takeIf { it > 0 }?.div(60))?.let { ProfileValue("Duración habitual", "$it min") }
    profile.typicalDistanceKm?.let { ProfileValue("Distancia habitual", String.format(locale, "%.1f km", it)) }
    profile.typicalPaceSecondsPerKm?.let { pace -> ProfileValue("Ritmo habitual", "${pace / 60}:${(pace % 60).toString().padStart(2, '0')} min/km") }
    Divider(Modifier.padding(vertical = 14.dp))
    Text("NOTAS", fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
    OutlinedTextField(
        value = profile.notes,
        onValueChange = onNotesChanged,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        placeholder = { Text("Añade notas personales para este ejercicio") },
        minLines = 4,
        maxLines = 8,
    )
}

@Composable
private fun ProfileValue(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = WildforceThemeTokens.textSecondary)
        Text(value, color = WildforceThemeTokens.textPrimary, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun RelatedContent(context: Context, exercise: ExerciseSummary, gender: String, onSelect: (ExerciseSummary) -> Unit) {
    var related by remember(exercise.imageKey) { mutableStateOf<List<ExerciseSummary>?>(null) }
    LaunchedEffect(exercise.imageKey) {
        related = withContext(Dispatchers.Default) {
            val catalog = CustomExerciseCatalog.load(context)
            val sections = exerciseReplacementSections(catalog, exercise)
            (sections.recommendedSubstitutes + sections.sameMuscleGroup)
                .take(8)
                .map { choice -> ExerciseSummary(choice.name, choice.imageKey, 1, "", 0) }
        }
    }

    val resolvedRelated = related ?: run {
        EmptyDetailsState(Icons.Filled.Link, "CARGANDO EJERCICIOS", "Buscando sustitutos y ejercicios del mismo grupo muscular.")
        return
    }
    if (resolvedRelated.isEmpty()) {
        EmptyDetailsState(Icons.Filled.Link, "SIN EJERCICIOS RELACIONADOS", "No hay otros ejercicios del mismo grupo muscular.")
        return
    }
    resolvedRelated.forEachIndexed { index, item ->
        Row(Modifier.fillMaxWidth().clickable { onSelect(item) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            RemoteTrainingImage(exerciseImageUrl(item.imageKey, gender), item.name, Modifier.size(58.dp).clip(RoundedCornerShape(12.dp)))
            Text(item.name, Modifier.padding(start = 12.dp).weight(1f), color = WildforceThemeTokens.textPrimary, fontWeight = FontWeight.SemiBold)
        }
        if (index < resolvedRelated.lastIndex) Divider()
    }
}

@Composable
private fun EmptyDetailsState(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, body: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(46.dp), tint = WildforceThemeTokens.textSecondary)
        Text(title, Modifier.padding(top = 10.dp), fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary, textAlign = TextAlign.Center)
        Text(body, Modifier.padding(top = 6.dp), color = WildforceThemeTokens.textSecondary, textAlign = TextAlign.Center)
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, Modifier.padding(top = 26.dp, bottom = 8.dp), fontFamily = AntonFontFamily, style = MaterialTheme.typography.h5, color = WildforceThemeTokens.textPrimary)
}
