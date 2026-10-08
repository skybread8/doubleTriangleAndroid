package io.codepassion.doubletriangle.feature.workout

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.codepassion.doubletriangle.core.designsystem.Exo2FontFamily
import io.codepassion.doubletriangle.core.designsystem.WildforceThemeTokens
import io.codepassion.doubletriangle.core.model.ExerciseSetStyle
import io.codepassion.doubletriangle.core.model.ExerciseSummary
import io.codepassion.doubletriangle.core.model.WorkoutBlockType

/** Android counterpart of iOS ActiveWorkoutFeatureOnboarding. */
internal sealed interface WorkoutFeatureOnboarding {
    val id: String
    data class Exercise(val exercise: ExerciseSummary) : WorkoutFeatureOnboarding { override val id = "exercise:${exercise.imageKey ?: exercise.name.lowercase()}" }
    data class SetStyle(val exercise: ExerciseSummary) : WorkoutFeatureOnboarding { override val id = "setStyle:${exercise.setStyle.name}" }
    data class Superset(val exercises: List<ExerciseSummary>, val rounds: Int, val restSeconds: Int?) : WorkoutFeatureOnboarding { override val id = "superset" }
}

internal class WorkoutFeatureOnboardingStore(context: Context) {
    private val preferences = context.getSharedPreferences("activeWorkout.featureOnboarding", Context.MODE_PRIVATE)
    fun hasSeen(item: WorkoutFeatureOnboarding): Boolean = preferences.getBoolean(item.id, false)
    /** iOS marks the item as seen when it is presented, so an interrupted sheet is never repeated. */
    fun markSeen(item: WorkoutFeatureOnboarding) { preferences.edit().putBoolean(item.id, true).apply() }
}

internal fun workoutFeatureOnboardings(
    exercise: ExerciseSummary,
    pathBlock: WorkoutPathBlock?,
    store: WorkoutFeatureOnboardingStore,
): List<WorkoutFeatureOnboarding> = buildList {
    WorkoutFeatureOnboarding.Exercise(exercise).takeUnless(store::hasSeen)?.let(::add)
    if (exercise.setStyle != ExerciseSetStyle.Straight) {
        WorkoutFeatureOnboarding.SetStyle(exercise).takeUnless(store::hasSeen)?.let(::add)
    }
    if (pathBlock?.type == WorkoutBlockType.Superset) {
        WorkoutFeatureOnboarding.Superset(pathBlock.exercises.map { it.exercise }, pathBlock.rounds, pathBlock.restAfterBlockSeconds)
            .takeUnless(store::hasSeen)?.let(::add)
    }
}

@Composable
internal fun WorkoutFeatureOnboardingDialog(item: WorkoutFeatureOnboarding, gender: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(WildforceThemeTokens.backgroundSecondary)) {
            when (item) {
                is WorkoutFeatureOnboarding.Exercise -> ExerciseOnboarding(item.exercise, gender, onDismiss)
                is WorkoutFeatureOnboarding.SetStyle -> SetStyleOnboarding(item.exercise, onDismiss)
                is WorkoutFeatureOnboarding.Superset -> SupersetOnboarding(item, gender, onDismiss)
            }
        }
    }
}

@Composable
private fun OnboardingScaffold(total: Int, step: Int, eyebrow: String, title: String, description: String, icon: @Composable () -> Unit, body: @Composable () -> Unit, onBack: (() -> Unit)?, onNext: () -> Unit, onSkip: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(top = 20.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Paso ${step + 1} de $total", color = WildforceThemeTokens.textSecondary, style = MaterialTheme.typography.body2, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onSkip) { Text("SALTAR", color = WildforceThemeTokens.textSecondary) }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(total) { index -> Box(Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(99.dp)).background(if (index == step) WildforceThemeTokens.textPrimary else WildforceThemeTokens.textPrimary.copy(alpha = .14f))) }
        }
        Column(Modifier.weight(1f).padding(horizontal = 24.dp, vertical = 28.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                icon(); Spacer(Modifier.width(12.dp))
                Column { Text(eyebrow.uppercase(), color = WildforceThemeTokens.accentGold, fontSize = 12.sp, fontWeight = FontWeight.Bold); Text(title, color = WildforceThemeTokens.textPrimary, fontFamily = Exo2FontFamily, fontWeight = FontWeight.SemiBold, fontSize = 24.sp) }
            }
            Text(description, Modifier.padding(top = 16.dp), color = WildforceThemeTokens.textSecondary, style = MaterialTheme.typography.body1)
            // iOS moves each onboarding page in from below when the step changes.
            // Key the Android content to the same step so informative cards do not
            // snap between unrelated states.
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    (fadeIn(tween(260)) + slideInVertically(tween(300)) { it / 8 }) togetherWith
                        (fadeOut(tween(160)) + slideOutVertically(tween(180)) { -it / 12 })
                },
                label = "workout-feature-onboarding-step",
            ) { body() }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (onBack != null) TextButton(onClick = onBack, modifier = Modifier.height(48.dp)) { Text("VOLVER", color = WildforceThemeTokens.textSecondary) }
            Button(onClick = onNext, modifier = Modifier.weight(1f).height(48.dp), colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary, contentColor = WildforceThemeTokens.primaryButtonText), shape = RoundedCornerShape(8.dp)) { Text(if (step == total - 1) "ENTENDIDO" else "SIGUIENTE") }
        }
    }
}

@Composable private fun HeroIcon(content: @Composable () -> Unit) = Box(Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(WildforceThemeTokens.textPrimary), contentAlignment = Alignment.Center) { content() }

@Composable
private fun ExerciseOnboarding(exercise: ExerciseSummary, gender: String, onDismiss: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val guide = remember(exercise.imageKey) { ExerciseGuideRepository.get(context, exercise.imageKey) }
    val visual = remember(exercise.imageKey) { ExerciseVisualCatalog.metadata(exercise.imageKey) }
    val tracking = when (exercise.trackingMode.name) { "DurationAndDistance" -> "tiempo, distancia y ritmo"; "Duration" -> "esfuerzos cronometrados"; else -> "series y repeticiones" }
    val (eyebrow, title, description) = when (step) {
        0 -> Triple("Ejercicio nuevo", exercise.name, guide?.description?.takeIf { it.isNotBlank() } ?: "Conoce el objetivo antes de empezar.")
        1 -> Triple("Cómo lo registrarás", "Regístralo correctamente", "Este ejercicio se sigue mediante $tracking.")
        else -> Triple("Técnica", "Céntrate en las claves", "Muévete con control y prioriza el rango de movimiento limpio.")
    }
    OnboardingScaffold(3, step, eyebrow, title, description, { HeroIcon { Icon(if (step == 1) Icons.Filled.Tune else Icons.Filled.FitnessCenter, null, tint = WildforceThemeTokens.primaryButtonText) } }, {
        when (step) {
            0 -> {
                Row(Modifier.padding(top = 22.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf(exercise.trackingMode.name, "${exercise.sets} series", if (exercise.blockType == WorkoutBlockType.Superset) "Superserie" else "Bilateral").forEach { Pill(it) } }
                val muscles = visual?.primary.orEmpty() + visual?.secondary.orEmpty()
                if (muscles.isNotEmpty()) { Text("MÚSCULOS", Modifier.padding(top = 24.dp), color = WildforceThemeTokens.textPrimary, fontWeight = FontWeight.Bold); Row(Modifier.padding(top = 10.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) { muscles.take(4).forEach { muscle -> Column(horizontalAlignment = Alignment.CenterHorizontally) { androidx.compose.foundation.Image(androidx.compose.ui.res.painterResource(muscle.drawable), muscle.label, Modifier.size(58.dp)); Text(muscle.label, color = WildforceThemeTokens.textSecondary, style = MaterialTheme.typography.caption) } } } }
                RemoteTrainingImage(exerciseTutorialImageUrl(exercise.imageKey), "Tutorial de ${exercise.name}", Modifier.fillMaxWidth().height(190.dp).padding(top = 22.dp).clip(RoundedCornerShape(22.dp)), contentScale = androidx.compose.ui.layout.ContentScale.Fit)
            }
            1 -> Row(Modifier.padding(top = 42.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) { InfoCard("SEGUIMIENTO", tracking); InfoCard("OBJETIVO", exercise.reps); InfoCard("CARGA", if (exercise.targetWeightKg != null) "Peso" else "Peso corporal") }
            else -> Column(Modifier.padding(top = 28.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) { ExerciseVisualCatalog.equipmentFor(exercise.imageKey).forEach { Pill(it) }; (guide?.instructions?.take(3).orEmpty() + guide?.tips?.take(1).orEmpty()).ifEmpty { listOf("Mantén una postura estable.", "Completa el recorrido sin rebotes.") }.forEachIndexed { index, cue -> Row { Text(if (index < 3) "${index + 1}" else "💡", color = WildforceThemeTokens.accentGold, fontWeight = FontWeight.Bold); Text(cue, Modifier.padding(start = 12.dp), color = WildforceThemeTokens.textPrimary) } } }
        }
    }, if (step > 0) {{ step-- }} else null, { if (step == 2) onDismiss() else step++ }, onDismiss)
}

@Composable private fun InfoCard(title: String, value: String) = Column(Modifier.width(104.dp).clip(RoundedCornerShape(18.dp)).background(WildforceThemeTokens.background).padding(10.dp)) { Text(title, color = WildforceThemeTokens.textSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold); Text(value, Modifier.padding(top = 5.dp), color = WildforceThemeTokens.textPrimary, style = MaterialTheme.typography.body2, fontWeight = FontWeight.SemiBold) }
@Composable private fun Pill(value: String) = Text(value, Modifier.clip(RoundedCornerShape(99.dp)).background(WildforceThemeTokens.background).padding(horizontal = 12.dp, vertical = 8.dp), color = WildforceThemeTokens.textPrimary, style = MaterialTheme.typography.caption, fontWeight = FontWeight.SemiBold)

@Composable
private fun SetStyleOnboarding(exercise: ExerciseSummary, onDismiss: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    val style = exercise.setStyle
    val intro = mapOf(ExerciseSetStyle.Warmup to "Esta serie te prepara para el trabajo duro.", ExerciseSetStyle.TopSetBackoff to "Harás una serie más intensa y después trabajo de descarga.", ExerciseSetStyle.AscendingPyramid to "Las series aumentan la carga progresivamente.", ExerciseSetStyle.DropSet to "Tras bajar rápido el peso, prolongarás el esfuerzo.", ExerciseSetStyle.RestPause to "Una serie dura se divide en mini-esfuerzos con descansos breves.", ExerciseSetStyle.Intervals to "Alternarás trabajo y recuperación planificados.", ExerciseSetStyle.Tempo to "La velocidad y el control importan tanto como las repeticiones.").getOrDefault(style, "Seguirás un patrón de trabajo constante y repetible.")
    val body = if (step == 1) "Sigue el patrón indicado y registra la serie como siempre." else "Ayuda a repetir, comparar y progresar tu rendimiento."
    OnboardingScaffold(3, step, "Nueva indicación", style.label, if (step == 0) intro else body, { HeroIcon { Text(style.glyph, color = WildforceThemeTokens.primaryButtonText, fontSize = 24.sp, fontWeight = FontWeight.Bold) } }, {
        if (step > 0) Column(Modifier.padding(top = 34.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) { Text(if (step == 1) "Cómo hacerlo" else "Por qué se usa", color = WildforceThemeTokens.textPrimary, fontWeight = FontWeight.Bold); Text("${exercise.sets} series · ${exercise.reps}", Modifier.padding(top = 16.dp), color = WildforceThemeTokens.textSecondary); Row(Modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { repeat(exercise.sets.coerceAtMost(6)) { index -> Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(if (index == 0) WildforceThemeTokens.accentGold else WildforceThemeTokens.textPrimary.copy(alpha = .12f))) } } }
    }, if (step > 0) {{ step-- }} else null, { if (step == 2) onDismiss() else step++ }, onDismiss)
}

@Composable
private fun SupersetOnboarding(item: WorkoutFeatureOnboarding.Superset, gender: String, onDismiss: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    val names = item.exercises.take(2).joinToString(" y ") { it.name }
    OnboardingScaffold(2, step, if (step == 0) "Nuevo flujo" else "Cómo funciona", if (step == 0) "Superserie" else "Pasa de un ejercicio a otro", if (step == 0) "Este bloque une $names en un flujo de ${item.rounds} rondas." else "Completa los ejercicios en orden antes de reiniciar la ronda.", { HeroIcon { Icon(Icons.Filled.Repeat, null, tint = WildforceThemeTokens.primaryButtonText) } }, {
        if (step == 0) Column(Modifier.padding(top = 28.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { item.exercises.take(2).forEachIndexed { index, exercise -> Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(WildforceThemeTokens.textPrimary.copy(alpha = .08f)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) { RemoteTrainingImage(exerciseImageUrl(exercise.imageKey, gender), exercise.name, Modifier.size(width = 56.dp, height = 68.dp).clip(RoundedCornerShape(10.dp))); Column(Modifier.padding(start = 12.dp)) { Text("A${index + 1}", color = WildforceThemeTokens.textSecondary, style = MaterialTheme.typography.caption); Text(exercise.name, color = WildforceThemeTokens.textPrimary, fontWeight = FontWeight.Bold) } } }; Pill("Repite durante ${item.rounds} rondas"); Pill(if ((item.restSeconds ?: 0) > 0) "Descansa ${item.restSeconds} s tras cada ronda" else "Descansa tras cada ronda") } else Column(Modifier.padding(top = 44.dp), verticalArrangement = Arrangement.spacedBy(30.dp)) { Tip("Mantén el ritmo", "Pasa al siguiente ejercicio sin perder la concentración."); Tip("Piensa en rondas", "Cada recorrido completo del bloque cuenta como una ronda."); Tip("Descansa al final", if ((item.restSeconds ?: 0) > 0) "Después de ambos ejercicios descansa ${item.restSeconds} segundos." else "Descansa después de completar la ronda.") }
    }, if (step > 0) {{ step-- }} else null, { if (step == 1) onDismiss() else step++ }, onDismiss)
}
@Composable private fun Tip(title: String, detail: String) = Column { Text(title, color = WildforceThemeTokens.textPrimary, fontWeight = FontWeight.Bold); Text(detail, Modifier.padding(top = 4.dp), color = WildforceThemeTokens.textSecondary) }
