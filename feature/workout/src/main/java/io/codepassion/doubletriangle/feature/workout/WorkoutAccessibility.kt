package io.codepassion.doubletriangle.feature.workout

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import io.codepassion.doubletriangle.core.designsystem.R as SharedR

@Composable
internal fun Modifier.localizedWorkoutProgress(@StringRes subject: Int, completed: Int, total: Int): Modifier {
    val label = stringResource(SharedR.string.___progress, stringResource(subject))
    val state = stringResource(SharedR.string.ui_sheet__d_of__d, completed, total)
    return clearAndSetSemantics {
        contentDescription = label
        stateDescription = state
        progressBarRangeInfo = ProgressBarRangeInfo(completed.toFloat().coerceIn(0f, total.coerceAtLeast(1).toFloat()), 0f..total.coerceAtLeast(1).toFloat())
    }
}

@Composable
internal fun Modifier.localizedWorkoutCardDescription(title: String, detail: String, minutes: Int): Modifier {
    val duration = stringResource(SharedR.string.ui_sheet__d_min, minutes)
    return semantics { contentDescription = "$title, $detail, $duration" }
}

@Composable
internal fun Modifier.localizedFocusDescription(focus: String): Modifier {
    val description = stringResource(SharedR.string.focus____, focus)
    return semantics { contentDescription = description }
}

@Composable
internal fun Modifier.localizedMesocycleDescription(index: Int, week: Int, totalWeeks: Int): Modifier {
    val cycle = stringResource(SharedR.string.mesocycle__d, index)
    val weekLabel = stringResource(SharedR.string.week__d_of__d, week, totalWeeks)
    return semantics { contentDescription = "$cycle, $weekLabel" }
}

@Composable
internal fun Modifier.localizedExerciseGuideDescription(exerciseName: String): Modifier {
    val description = stringResource(SharedR.string.shows_exercise_details_and_instructions)
    return semantics { contentDescription = "$exerciseName. $description" }
}
