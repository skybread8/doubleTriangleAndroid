package io.codepassion.doubletriangle.feature.workout

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.codepassion.doubletriangle.core.designsystem.WildforceThemeTokens
import io.codepassion.doubletriangle.core.designsystem.liquidGlass
import io.codepassion.doubletriangle.core.model.WorkoutBlockType
import io.codepassion.doubletriangle.core.model.WorkoutDaySummary
import io.codepassion.doubletriangle.core.model.displayBlocks
import kotlin.math.roundToInt

/** Same effective-set calculation and 0...18 scale used by iOS WorkoutDayStimulus. */
internal const val MaxWorkoutMuscleStimulus = 18

internal data class MuscleStimulus(val muscle: MuscleVisual, val effectiveSets: Int)

internal fun workoutMuscleStimulus(workout: WorkoutDaySummary): List<MuscleStimulus> {
    val scores = mutableMapOf<MuscleVisual, Double>()
    workout.displayBlocks().forEach { block ->
        block.exercises.forEach { exercise ->
            val metadata = ExerciseVisualCatalog.metadata(exercise.imageKey) ?: return@forEach
            // Superset rounds are the executable set count, as on iOS. For every
            // other block, the planned set count is the effective set count.
            val sets = (if (block.type == WorkoutBlockType.Superset) block.rounds else exercise.sets)
                .coerceAtLeast(0)
                .toDouble()
            if (sets == 0.0) return@forEach
            metadata.primary.forEach { muscle -> scores[muscle] = scores.getOrDefault(muscle, 0.0) + sets }
            (metadata.secondary - metadata.primary.toSet()).forEach { muscle ->
                scores[muscle] = scores.getOrDefault(muscle, 0.0) + sets * 0.5
            }
        }
    }
    return scores.mapNotNull { (muscle, score) ->
        score.roundToInt().takeIf { it > 0 }?.let { MuscleStimulus(muscle, it) }
    }.sortedWith(compareByDescending<MuscleStimulus> { it.effectiveSets }.thenBy { it.muscle.label })
}

@Composable
internal fun WorkoutMuscleEffort(workout: WorkoutDaySummary, modifier: Modifier = Modifier) {
    val muscles = workoutMuscleStimulus(workout)
    if (muscles.isEmpty()) return

    Column(modifier) {
        Text(
            "MÚSCULOS OBJETIVO",
            color = WildforceThemeTokens.textSecondary,
            style = MaterialTheme.typography.caption,
            fontWeight = FontWeight.Bold,
        )
        Row(
            Modifier.fillMaxWidth().padding(top = 10.dp).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            muscles.forEach { MuscleStimulusCard(it) }
        }
    }
}

@Composable
private fun MuscleStimulusCard(stimulus: MuscleStimulus) {
    val displayedValue = stimulus.effectiveSets.coerceIn(0, MaxWorkoutMuscleStimulus)
    Column(
        Modifier.width(112.dp)
            .liquidGlass(RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .semantics {
                contentDescription = "${stimulus.muscle.label}: $displayedValue de $MaxWorkoutMuscleStimulus series efectivas"
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(stimulus.muscle.drawable),
            contentDescription = null,
            modifier = Modifier.size(68.dp),
        )
        Text(
            stimulus.muscle.label,
            Modifier.padding(top = 8.dp).height(34.dp),
            color = WildforceThemeTokens.textPrimary,
            style = MaterialTheme.typography.caption,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        DottedMuscleProgress(displayedValue, Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun DottedMuscleProgress(value: Int, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            repeat(MaxWorkoutMuscleStimulus + 1) { index ->
                Box(
                    Modifier.size(4.dp)
                        .background(
                            if (index <= value) WildforceThemeTokens.accent.copy(alpha = 0.25f + 0.75f * index / value.coerceAtLeast(1))
                            else WildforceThemeTokens.textSecondary.copy(alpha = 0.3f),
                            RoundedCornerShape(2.dp),
                        ),
                )
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 3.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("0", color = WildforceThemeTokens.textSecondary, style = MaterialTheme.typography.caption)
            Spacer(Modifier.weight(1f))
            Text("$value / $MaxWorkoutMuscleStimulus", color = WildforceThemeTokens.textSecondary, style = MaterialTheme.typography.caption)
        }
    }
}
