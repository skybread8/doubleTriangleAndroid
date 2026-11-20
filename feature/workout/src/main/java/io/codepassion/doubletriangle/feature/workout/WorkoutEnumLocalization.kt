package io.codepassion.doubletriangle.feature.workout

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.codepassion.doubletriangle.core.model.ExerciseSetStyle
import io.codepassion.doubletriangle.core.model.MesocyclePhase
import io.codepassion.doubletriangle.core.model.WorkoutBlockType
import io.codepassion.doubletriangle.core.designsystem.R as SharedR

@Composable
internal fun workoutBlockLabel(type: WorkoutBlockType): String = stringResource(when (type) {
    WorkoutBlockType.Warmup -> SharedR.string.ui_sheet_warmup
    WorkoutBlockType.Standard -> SharedR.string.ui_sheet_standard
    WorkoutBlockType.Superset -> SharedR.string.superset
    WorkoutBlockType.Cooldown -> SharedR.string.ui_sheet_cooldown
})

internal fun workoutSetStyleResource(style: ExerciseSetStyle): Int = when (style) {
    ExerciseSetStyle.Warmup -> SharedR.string.ui_sheet_warmup
    ExerciseSetStyle.Straight -> R.string.workout_local_straight
    ExerciseSetStyle.TopSetBackoff -> R.string.workout_local_top_backoff
    ExerciseSetStyle.AscendingPyramid -> R.string.workout_local_pyramid
    ExerciseSetStyle.DropSet -> R.string.workout_local_drop
    ExerciseSetStyle.RestPause -> R.string.workout_local_rest_pause
    ExerciseSetStyle.Intervals -> R.string.workout_local_intervals
    ExerciseSetStyle.Tempo -> SharedR.string.tempo
}

@Composable
internal fun workoutSetStyleLabel(style: ExerciseSetStyle): String = stringResource(workoutSetStyleResource(style))

internal fun workoutPhaseResource(phase: MesocyclePhase): Int = when (phase) {
    MesocyclePhase.Accumulation -> R.string.workout_local_accumulation
    MesocyclePhase.Intensification -> R.string.workout_local_intensification
    MesocyclePhase.Deload -> R.string.workout_local_deload
}

@Composable
internal fun workoutPhaseLabel(phase: MesocyclePhase): String = stringResource(workoutPhaseResource(phase))
