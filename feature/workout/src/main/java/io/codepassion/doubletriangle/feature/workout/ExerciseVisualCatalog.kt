package io.codepassion.doubletriangle.feature.workout

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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

internal enum class MuscleVisual(val iosIdentifier: String, val label: String, @param:DrawableRes val drawable: Int) {
    Abs("abs", "Abdominales", R.drawable.muscle_group_abs),
    Back("back", "Espalda", R.drawable.muscle_group_back),
    Biceps("biceps", "Bíceps", R.drawable.muscle_group_biceps),
    Calves("calves", "Gemelos", R.drawable.muscle_group_calves),
    Cardio("cardio", "Cardio", R.drawable.muscle_group_cardio),
    Chest("chest", "Pecho", R.drawable.muscle_group_chest),
    Glutes("glutes", "Glúteos", R.drawable.muscle_group_glutes),
    Hamstrings("hamstrings", "Isquiotibiales", R.drawable.muscle_group_hamstrings),
    LowerBack("lowerBack", "Lumbar", R.drawable.muscle_group_lower_back),
    Obliques("obliques", "Oblicuos", R.drawable.muscle_group_obliques),
    Quads("quads", "Cuádriceps", R.drawable.muscle_group_quads),
    Shoulders("shoulders", "Hombros", R.drawable.muscle_group_shoulders),
    Triceps("triceps", "Tríceps", R.drawable.muscle_group_triceps),
}

internal data class ExerciseVisualMetadata(
    val primary: List<MuscleVisual>,
    val secondary: List<MuscleVisual> = emptyList(),
)

internal enum class EquipmentVisual(val label: String, @param:DrawableRes val drawable: Int) {
    Bodyweight("Peso corporal", R.drawable.equipment_bodyweight),
    Dumbbells("Mancuernas", R.drawable.equipment_dumbbells),
    AdjustableBench("Banco ajustable", R.drawable.equipment_adjustable_bench),
    CableMachine("Máquina de poleas", R.drawable.equipment_cable_machine),
    OlympicBarbell("Barra y discos", R.drawable.equipment_olympic_barbell),
    PullUpBar("Barra de dominadas", R.drawable.equipment_pull_up_bar),
    LegPressMachine("Prensa de piernas", R.drawable.equipment_leg_press_machine),
}

object ExerciseVisualCatalog {
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

    internal fun metadata(imageKey: String?): ExerciseVisualMetadata? = imageKey?.let { key ->
        definitions[key] ?: definitions.entries.firstOrNull { it.key.equals(key, ignoreCase = true) }?.value
    }

    fun primaryMuscleLabels(imageKey: String?): List<String> = metadata(imageKey)?.primary?.map(MuscleVisual::label).orEmpty()

    /** Matches iOS: choose the alphabetically first primary `MuscleGroup.rawValue`. */
    fun primaryMuscleDrawable(imageKey: String?): Int? = metadata(imageKey)
        ?.primary
        ?.minByOrNull(MuscleVisual::iosIdentifier)
        ?.drawable

    fun equipmentFor(imageKey: String?): List<String> = when (imageKey?.trim()) {
        "benchPress", "inclineBenchPress", "chestDip" -> listOf("Banco ajustable")
        "latPulldown", "seatedCableRow", "facePull", "tricepsPushdown" -> listOf("Máquina de poleas")
        "barbellBackSquat", "deadlift", "romanianDeadlift", "bentOverRow" -> listOf("Barra y discos")
        "gobletSquat", "overheadPress", "lateralRaise", "bicepsCurl", "hammerCurl" -> listOf("Mancuernas")
        "pullUp" -> listOf("Barra de dominadas")
        "legPress" -> listOf("Prensa de piernas")
        else -> listOf("Peso corporal")
    }

    /** Uses the same equipment illustrations as iOS's WorkoutEquipmentScroll. */
    internal fun equipmentVisualsFor(imageKey: String?): List<EquipmentVisual> = when (imageKey?.trim()) {
        "benchPress", "inclineBenchPress", "chestDip" -> listOf(EquipmentVisual.AdjustableBench)
        "latPulldown", "seatedCableRow", "facePull", "tricepsPushdown" -> listOf(EquipmentVisual.CableMachine)
        "barbellBackSquat", "deadlift", "romanianDeadlift", "bentOverRow" -> listOf(EquipmentVisual.OlympicBarbell)
        "gobletSquat", "overheadPress", "lateralRaise", "bicepsCurl", "hammerCurl" -> listOf(EquipmentVisual.Dumbbells)
        "pullUp" -> listOf(EquipmentVisual.PullUpBar)
        "legPress" -> listOf(EquipmentVisual.LegPressMachine)
        else -> listOf(EquipmentVisual.Bodyweight)
    }
}

@Composable
internal fun MuscleStrip(imageKey: String?, onDarkBackground: Boolean, modifier: Modifier = Modifier, wrapContent: Boolean = false) {
    val metadata = ExerciseVisualCatalog.metadata(imageKey) ?: run {
        androidx.compose.material.Text("Músculos no catalogados", modifier.padding(vertical = 8.dp), color = if (onDarkBackground) Color.White.copy(alpha = 0.7f) else Color.Gray, style = androidx.compose.material.MaterialTheme.typography.caption)
        return
    }
    val tint = if (onDarkBackground) Color.White else Color.Unspecified
    val muscles = metadata.primary.map { it to true } + metadata.secondary.map { it to false }
    if (wrapContent) {
        Column(modifier) {
            muscles.chunked(3).forEach { row ->
                Row(verticalAlignment = Alignment.CenterVertically) { row.forEach { (muscle, primary) -> MuscleIcon(muscle, primary) } }
            }
        }
    } else {
        Row(modifier.horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
            muscles.forEach { (muscle, primary) -> MuscleIcon(muscle, primary) }
        }
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
