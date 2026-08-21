package io.codepassion.doubletriangle.feature.workout

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

internal enum class MuscleVisual(val label: String, @param:DrawableRes val drawable: Int) {
    Abs("Abdominales", R.drawable.muscle_group_abs),
    Back("Espalda", R.drawable.muscle_group_back),
    Biceps("Bíceps", R.drawable.muscle_group_biceps),
    Calves("Gemelos", R.drawable.muscle_group_calves),
    Cardio("Cardio", R.drawable.muscle_group_cardio),
    Chest("Pecho", R.drawable.muscle_group_chest),
    Glutes("Glúteos", R.drawable.muscle_group_glutes),
    Hamstrings("Isquiotibiales", R.drawable.muscle_group_hamstrings),
    LowerBack("Lumbar", R.drawable.muscle_group_lower_back),
    Obliques("Oblicuos", R.drawable.muscle_group_obliques),
    Quads("Cuádriceps", R.drawable.muscle_group_quads),
    Shoulders("Hombros", R.drawable.muscle_group_shoulders),
    Triceps("Tríceps", R.drawable.muscle_group_triceps),
}

internal data class ExerciseVisualMetadata(
    val primary: List<MuscleVisual>,
    val secondary: List<MuscleVisual> = emptyList(),
)

internal object ExerciseVisualCatalog {
    private val definitions = mapOf(
        "airSquat" to ExerciseVisualMetadata(listOf(MuscleVisual.Quads), listOf(MuscleVisual.Hamstrings, MuscleVisual.Glutes)),
        "gobletSquat" to ExerciseVisualMetadata(listOf(MuscleVisual.Quads), listOf(MuscleVisual.Glutes, MuscleVisual.Abs)),
        "barbellBackSquat" to ExerciseVisualMetadata(listOf(MuscleVisual.Quads, MuscleVisual.Glutes), listOf(MuscleVisual.Hamstrings, MuscleVisual.LowerBack)),
        "walkingLunge" to ExerciseVisualMetadata(listOf(MuscleVisual.Quads), listOf(MuscleVisual.Glutes, MuscleVisual.Hamstrings, MuscleVisual.Calves)),
        "legPress" to ExerciseVisualMetadata(listOf(MuscleVisual.Quads), listOf(MuscleVisual.Glutes, MuscleVisual.Hamstrings)),
        "deadlift" to ExerciseVisualMetadata(listOf(MuscleVisual.Hamstrings, MuscleVisual.Glutes, MuscleVisual.Back), listOf(MuscleVisual.LowerBack, MuscleVisual.Quads)),
        "romanianDeadlift" to ExerciseVisualMetadata(listOf(MuscleVisual.Hamstrings, MuscleVisual.Glutes), listOf(MuscleVisual.LowerBack)),
        "pushUp" to ExerciseVisualMetadata(listOf(MuscleVisual.Chest), listOf(MuscleVisual.Shoulders, MuscleVisual.Triceps)),
        "benchPress" to ExerciseVisualMetadata(listOf(MuscleVisual.Chest), listOf(MuscleVisual.Triceps, MuscleVisual.Shoulders)),
        "inclineBenchPress" to ExerciseVisualMetadata(listOf(MuscleVisual.Chest), listOf(MuscleVisual.Shoulders, MuscleVisual.Triceps)),
        "overheadPress" to ExerciseVisualMetadata(listOf(MuscleVisual.Shoulders), listOf(MuscleVisual.Triceps, MuscleVisual.Abs)),
        "lateralRaise" to ExerciseVisualMetadata(listOf(MuscleVisual.Shoulders)),
        "chestDip" to ExerciseVisualMetadata(listOf(MuscleVisual.Chest), listOf(MuscleVisual.Triceps, MuscleVisual.Shoulders)),
        "tricepsPushdown" to ExerciseVisualMetadata(listOf(MuscleVisual.Triceps)),
        "pullUp" to ExerciseVisualMetadata(listOf(MuscleVisual.Back), listOf(MuscleVisual.Biceps, MuscleVisual.Shoulders)),
        "latPulldown" to ExerciseVisualMetadata(listOf(MuscleVisual.Back), listOf(MuscleVisual.Biceps)),
        "seatedCableRow" to ExerciseVisualMetadata(listOf(MuscleVisual.Back), listOf(MuscleVisual.Biceps)),
        "bentOverRow" to ExerciseVisualMetadata(listOf(MuscleVisual.Back), listOf(MuscleVisual.Biceps, MuscleVisual.LowerBack)),
        "facePull" to ExerciseVisualMetadata(listOf(MuscleVisual.Shoulders, MuscleVisual.Back)),
        "bicepsCurl" to ExerciseVisualMetadata(listOf(MuscleVisual.Biceps)),
        "hammerCurl" to ExerciseVisualMetadata(listOf(MuscleVisual.Biceps)),
        "plank" to ExerciseVisualMetadata(listOf(MuscleVisual.Abs), listOf(MuscleVisual.Shoulders)),
        "sidePlank" to ExerciseVisualMetadata(listOf(MuscleVisual.Obliques), listOf(MuscleVisual.Shoulders)),
        "deadBug" to ExerciseVisualMetadata(listOf(MuscleVisual.Abs)),
        "mountainClimber" to ExerciseVisualMetadata(listOf(MuscleVisual.Cardio), listOf(MuscleVisual.Abs, MuscleVisual.Shoulders)),
    )

    fun metadata(imageKey: String?): ExerciseVisualMetadata? = definitions[imageKey]
}

@Composable
internal fun MuscleStrip(imageKey: String?, onDarkBackground: Boolean, modifier: Modifier = Modifier) {
    val metadata = ExerciseVisualCatalog.metadata(imageKey) ?: return
    val tint = if (onDarkBackground) Color.White else Color.Unspecified
    Row(modifier.horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
        metadata.primary.forEach { muscle -> MuscleIcon(muscle, true) }
        metadata.secondary.forEach { muscle -> MuscleIcon(muscle, false) }
    }
}

@Composable
private fun MuscleIcon(muscle: MuscleVisual, primary: Boolean) {
    Box(Modifier.padding(end = 6.dp).size(if (primary) 54.dp else 48.dp).clip(RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(muscle.drawable),
            contentDescription = muscle.label,
            modifier = Modifier.size(if (primary) 52.dp else 46.dp),
        )
    }
}