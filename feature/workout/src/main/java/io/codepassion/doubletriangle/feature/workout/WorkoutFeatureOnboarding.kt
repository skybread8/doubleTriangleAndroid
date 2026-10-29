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
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.automirrored.filled.CompareArrows
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
private fun OnboardingScaffold(total: Int, step: Int, eyebrow: String, title: String, description: String, icon: @Composable () -> Unit, body: @Composable (Int) -> Unit, onBack: (() -> Unit)?, onNext: () -> Unit, onSkip: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(top = 20.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(tutorialText("Step %d of %d").format(step + 1, total), color = WildforceThemeTokens.textSecondary, style = MaterialTheme.typography.body2, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onSkip) { Text(tutorialText("Skip").uppercase(), color = WildforceThemeTokens.textSecondary) }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(total) { index -> Box(Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(99.dp)).background(if (index == step) WildforceThemeTokens.textPrimary else WildforceThemeTokens.textPrimary.copy(alpha = .14f))) }
        }
        Column(Modifier.weight(1f).padding(horizontal = 24.dp, vertical = 28.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                icon(); Spacer(Modifier.width(12.dp))
                Column { Text(eyebrow.uppercase(), color = WildforceThemeTokens.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold); Text(title, color = WildforceThemeTokens.textPrimary, fontFamily = Exo2FontFamily, fontWeight = FontWeight.SemiBold, fontSize = 24.sp) }
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
            ) { targetStep ->
                // AnimatedContent lays out its content as one animated child.
                // The exercise step emits several siblings (pills, muscle
                // strip, labels and tutorial image); keep them in a normal
                // vertical container so they are measured and placed in
                // sequence instead of being stacked on top of each other.
                Column(Modifier.fillMaxWidth()) {
                    body(targetStep)
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (onBack != null) TextButton(onClick = onBack, modifier = Modifier.height(48.dp)) { Text(tutorialText("action.back").uppercase(), color = WildforceThemeTokens.textSecondary) }
            Button(onClick = onNext, modifier = Modifier.weight(1f).height(48.dp), colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary, contentColor = WildforceThemeTokens.primaryButtonText), shape = RoundedCornerShape(8.dp)) { Text(tutorialText(if (step == total - 1) "Got it" else "Next").uppercase()) }
        }
    }
}

@Composable private fun HeroIcon(content: @Composable () -> Unit) = Box(Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(WildforceThemeTokens.textPrimary), contentAlignment = Alignment.Center) { content() }

@Composable
private fun ExerciseOnboarding(exercise: ExerciseSummary, gender: String, onDismiss: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val locale = androidx.compose.ui.platform.LocalLocale.current
    val guide = remember(exercise.imageKey, locale) { ExerciseGuideRepository.get(context, exercise.imageKey) }
    val visual = remember(exercise.imageKey) { ExerciseVisualCatalog.metadata(exercise.imageKey) }
    val exerciseName = tutorialExerciseName(exercise.imageKey, exercise.name)
    val tracking = tutorialText(when (exercise.trackingMode.name) { "DurationAndDistance" -> "Time + Distance"; "Duration" -> "Seconds"; else -> "Reps" })
    val (eyebrow, title, description) = when (step) {
        0 -> Triple(tutorialText("New exercise"), exerciseName, guide?.description.orEmpty())
        1 -> Triple(tutorialText("How you'll log it"), tutorialText("Track it the right way"), tutorialText(when (exercise.trackingMode.name) { "DurationAndDistance" -> "This one is tracked more like cardio: time, distance, and pace matter most."; "Duration" -> "This one is tracked by time, so focus on holding or moving well for the full duration."; else -> "Log each effort as a set and keep the reps consistent with the target." }))
        else -> Triple(tutorialText("Form focus"), tutorialText("Focus on the right cues"), tutorialText("Move with control and stay inside a range of motion that feels clean."))
    }
    val heroIcon = when (step) {
        1 -> Icons.Filled.Tune // iOS `slider.horizontal.3`
        2 -> Icons.Filled.CenterFocusStrong // iOS `scope`
        else -> Icons.Filled.FitnessCenter // iOS exercise-category symbol fallback
    }
    OnboardingScaffold(3, step, eyebrow, title, description, { HeroIcon { Icon(heroIcon, null, tint = WildforceThemeTokens.primaryButtonText) } }, { _ ->
        when (step) {
            0 -> {
                Row(Modifier.padding(top = 22.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf(tracking, "${exercise.sets} ${tutorialText("Sets")}", tutorialText(if (ExerciseVisualCatalog.isUnilateral(exercise.imageKey)) "Unilateral" else "Bilateral")).forEach { Pill(it) } }
                val muscles = visual?.primary.orEmpty() + visual?.secondary.orEmpty()
                if (muscles.isNotEmpty()) { Text(tutorialText("Muscle Groups").uppercase(), Modifier.padding(top = 24.dp), color = WildforceThemeTokens.textPrimary, fontWeight = FontWeight.Bold); Row(Modifier.padding(top = 10.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) { muscles.take(4).forEach { muscle -> Column(horizontalAlignment = Alignment.CenterHorizontally) { androidx.compose.foundation.Image(androidx.compose.ui.res.painterResource(muscle.drawable), tutorialMuscleName(muscle), Modifier.size(58.dp)); Text(tutorialMuscleName(muscle), color = WildforceThemeTokens.textSecondary, style = MaterialTheme.typography.caption) } } } }
                RemoteTrainingImage(
                    url = CoachExerciseMediaStore.latest(context, exercise.imageKey)?.imageUrl ?: exerciseTutorialImageUrl(exercise.imageKey, gender),
                    contentDescription = tutorialText("Tutorial animation for %@").replace("%@", exerciseName),
                    modifier = Modifier.fillMaxWidth().height(190.dp).padding(top = 22.dp).clip(RoundedCornerShape(22.dp)),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                    unavailableContent = {},
                )
            }
            1 -> {
                val isUnilateral = ExerciseVisualCatalog.isUnilateral(exercise.imageKey)
                Column(Modifier.padding(top = 42.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        InfoCard(Icons.Filled.Tune, tutorialText("Tracking").uppercase(), tracking, Modifier.weight(1f))
                        InfoCard(Icons.Filled.CenterFocusStrong, tutorialText("Targets").uppercase(), exercise.reps, Modifier.weight(1f))
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        InfoCard(if (isUnilateral) Icons.Filled.SwapHoriz else Icons.AutoMirrored.Filled.CompareArrows, tutorialText("Sides").uppercase(), tutorialText(if (isUnilateral) "Match both sides" else "One shared effort"), Modifier.weight(1f))
                        InfoCard(if (exercise.targetWeightKg != null) Icons.Filled.MonitorWeight else Icons.Filled.Straighten, tutorialText("Load").uppercase(), tutorialText(if (exercise.targetWeightKg != null) "Weight supported" else "Bodyweight / time"), Modifier.weight(1f))
                    }
                }
            }
            else -> Column(Modifier.padding(top = 28.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) { ExerciseVisualCatalog.equipmentFor(exercise.imageKey).forEach { Pill(tutorialEquipmentName(it)) }; (guide?.instructions?.take(3).orEmpty() + guide?.tips?.take(1).orEmpty()).ifEmpty { listOf(tutorialText("Move with control and stay inside a range of motion that feels clean.")) }.forEachIndexed { index, cue -> Row(verticalAlignment = Alignment.CenterVertically) { if (index < 3) Icon(Icons.Filled.FormatListNumbered, contentDescription = null, tint = WildforceThemeTokens.accent) else Icon(Icons.Filled.Lightbulb, contentDescription = null, tint = Color(0xFFFFCC00)); Text(cue, Modifier.padding(start = 12.dp), color = WildforceThemeTokens.textPrimary) } } }
        }
    }, if (step > 0) {{ step-- }} else null, { if (step == 2) onDismiss() else step++ }, onDismiss)
}

@Composable private fun InfoCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, value: String, modifier: Modifier = Modifier) = Column(modifier.clip(RoundedCornerShape(18.dp)).background(WildforceThemeTokens.background).padding(10.dp)) { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = WildforceThemeTokens.accent); Text(title, Modifier.padding(top = 12.dp), color = WildforceThemeTokens.textSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold); Text(value, Modifier.padding(top = 5.dp), color = WildforceThemeTokens.textPrimary, style = MaterialTheme.typography.body2, fontWeight = FontWeight.SemiBold) }
@Composable private fun Pill(value: String) = Text(value, Modifier.clip(RoundedCornerShape(99.dp)).background(WildforceThemeTokens.background).padding(horizontal = 12.dp, vertical = 8.dp), color = WildforceThemeTokens.textPrimary, style = MaterialTheme.typography.caption, fontWeight = FontWeight.SemiBold)

@Composable
private fun SetStyleOnboarding(exercise: ExerciseSummary, onDismiss: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    val style = exercise.setStyle
    val intro = tutorialText(when (style) {
        ExerciseSetStyle.Warmup -> "This cue means the set is here to prepare you for the harder work ahead."
        ExerciseSetStyle.TopSetBackoff -> "This cue means you will push one hardest set, then continue with lighter backoff work."
        ExerciseSetStyle.AscendingPyramid -> "This cue means the sets build up as you go, usually with more load and fewer reps."
        ExerciseSetStyle.DropSet -> "This cue means the set continues after a quick weight drop to extend the effort."
        ExerciseSetStyle.RestPause -> "This cue means one hard set is broken into short mini-efforts with tiny rests."
        ExerciseSetStyle.Intervals -> "This cue means the exercise follows a work and recovery rhythm instead of one continuous push."
        ExerciseSetStyle.Tempo -> "This cue means the speed and control of each rep matter as much as the reps themselves."
        else -> "This cue means the exercise uses a steady, repeatable working pattern."
    })
    val title = tutorialText(when (style) {
        ExerciseSetStyle.Warmup -> "Warmup"
        ExerciseSetStyle.TopSetBackoff -> "Top Set + Backoff"
        ExerciseSetStyle.AscendingPyramid -> "Ascending Pyramid"
        ExerciseSetStyle.DropSet -> "Drop Set"
        ExerciseSetStyle.RestPause -> "Rest-Pause"
        ExerciseSetStyle.Intervals -> "Intervals"
        ExerciseSetStyle.Tempo -> "Tempo"
        else -> "Straight Sets"
    })
    val description = if (step == 0) intro else tutorialText(if (step == 1) "When this cue appears, follow the pattern and then log the set like usual." else "This style changes the goal of the set, not the whole workout flow.")
    OnboardingScaffold(3, step, tutorialText("New workout cue"), title, description, { HeroIcon { Icon(setStyleIcon(style), contentDescription = null, modifier = Modifier.size(28.dp), tint = WildforceThemeTokens.primaryButtonText) } }, { _ ->
        if (step > 0) Column(Modifier.padding(top = 34.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(tutorialText(if (step == 1) "How to do it" else "Why it's used"), color = WildforceThemeTokens.textPrimary, fontWeight = FontWeight.Bold)
            Text("${exercise.sets} ${tutorialText("Sets")} · ${exercise.reps}", Modifier.padding(top = 16.dp), color = WildforceThemeTokens.textSecondary)
            Row(Modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { repeat(exercise.sets.coerceAtMost(6)) { index -> Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(if (index == 0) WildforceThemeTokens.accent else WildforceThemeTokens.textPrimary.copy(alpha = .12f))) } }
        }
    }, if (step > 0) {{ step-- }} else null, { if (step == 2) onDismiss() else step++ }, onDismiss)
}

@Composable
private fun SupersetOnboarding(item: WorkoutFeatureOnboarding.Superset, gender: String, onDismiss: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    val names = item.exercises.take(2).map { tutorialExerciseName(it.imageKey, it.name) }
    val intro = if (names.isEmpty()) tutorialText("This block pairs exercises together so you can move through them as one connected round.") else tutorialText("This block pairs %@ into one flow for %d rounds.").replace("%@", names.joinToString(" & ")).replace("%d", item.rounds.toString())
    val rest = item.restSeconds ?: 0
    val restLabel = if (rest > 0) tutorialText("Rest %d sec after each round").replace("%d", rest.toString()) else tutorialText("Rest after each round")
    val restDetail = if (rest > 0) tutorialText("After both exercises, take %d seconds before repeating the block.").replace("%d", rest.toString()) else tutorialText("After both exercises, take the planned block rest before repeating.")
    OnboardingScaffold(2, step, tutorialText(if (step == 0) "New workout flow" else "How it runs"), tutorialText(if (step == 0) "Superset" else "Move exercise to exercise"), if (step == 0) intro else tutorialText("You will complete each exercise in order before the round resets."), { HeroIcon { Icon(Icons.Filled.Repeat, null, tint = WildforceThemeTokens.primaryButtonText) } }, { _ ->
        if (step == 0) Column(Modifier.padding(top = 28.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item.exercises.take(2).forEachIndexed { index, exercise ->
                val displayName = names[index]
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(WildforceThemeTokens.textPrimary.copy(alpha = .08f)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    RemoteTrainingImage(exerciseImageUrl(exercise.imageKey, gender), displayName, Modifier.size(width = 56.dp, height = 68.dp).clip(RoundedCornerShape(10.dp)))
                    Column(Modifier.padding(start = 12.dp)) { Text("A${index + 1}", color = WildforceThemeTokens.textSecondary, style = MaterialTheme.typography.caption); Text(displayName, color = WildforceThemeTokens.textPrimary, fontWeight = FontWeight.Bold) }
                }
            }
            Pill(tutorialText("Repeat for %d rounds").replace("%d", item.rounds.toString()))
            Pill(restLabel)
        } else Column(Modifier.padding(top = 44.dp), verticalArrangement = Arrangement.spacedBy(30.dp)) {
            Tip(tutorialText("Stay moving"), tutorialText("Transition to the next exercise without losing setup focus."))
            Tip(tutorialText("Think in rounds"), tutorialText("Each pass through the block counts as one full round."))
            Tip(tutorialText("Take the main rest after the round"), restDetail)
        }
    }, if (step > 0) {{ step-- }} else null, { if (step == 1) onDismiss() else step++ }, onDismiss)
}
@Composable private fun Tip(title: String, detail: String) = Column { Text(title, color = WildforceThemeTokens.textPrimary, fontWeight = FontWeight.Bold); Text(detail, Modifier.padding(top = 4.dp), color = WildforceThemeTokens.textSecondary) }
