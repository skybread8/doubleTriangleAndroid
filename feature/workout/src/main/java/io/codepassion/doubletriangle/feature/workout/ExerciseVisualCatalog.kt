package io.codepassion.doubletriangle.feature.workout

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp

internal enum class MuscleVisual(val iosIdentifier: String, val label: String, @param:DrawableRes val drawable: Int) {
    Chest("chest", "Pecho", R.drawable.muscle_group_chest),
    Shoulders("shoulders", "Hombros", R.drawable.muscle_group_shoulders),
    Abs("abs", "Abdominales", R.drawable.muscle_group_abs),
    Back("back", "Espalda", R.drawable.muscle_group_back),
    Biceps("biceps", "Bíceps", R.drawable.muscle_group_biceps),
    Calves("calves", "Gemelos", R.drawable.muscle_group_calves),
    Cardio("cardio", "Cardio", R.drawable.muscle_group_cardio),
    Glutes("glutes", "Glúteos", R.drawable.muscle_group_glutes),
    Hamstrings("hamstrings", "Isquiotibiales", R.drawable.muscle_group_hamstrings),
    LowerBack("lowerBack", "Lumbar", R.drawable.muscle_group_lower_back),
    UpperBack("upperBack", "Espalda alta", R.drawable.muscle_group_upper_back),
    Forearms("forearms", "Antebrazos", R.drawable.muscle_group_forearms),
    InnerThigh("innerThigh", "Parte interna del muslo", R.drawable.muscle_group_inner_thigh),
    OuterThigh("outerThigh", "Parte externa del muslo", R.drawable.muscle_group_outer_thigh),
    LowerAbs("lowerAbs", "Abdominales inferiores", R.drawable.muscle_group_lower_abs),
    UpperAbs("upperAbs", "Abdominales superiores", R.drawable.muscle_group_upper_abs),
    Hips("hips", "Caderas", R.drawable.muscle_group_hips_back),
    Legs("legs", "Piernas", R.drawable.muscle_group_quads),
    FullBody("fullBody", "Cuerpo completo", R.drawable.muscle_group_full_body),
    Neck("neck", "Cuello", R.drawable.muscle_group_neck),
    Obliques("obliques", "Oblicuos", R.drawable.muscle_group_obliques),
    Quads("quads", "Cuádriceps", R.drawable.muscle_group_quads),
    Triceps("triceps", "Tríceps", R.drawable.muscle_group_triceps),
}

private fun MuscleVisual.majorAnalyticsGroup(): String = when (this) {
    MuscleVisual.Chest -> "Pecho"
    MuscleVisual.Back, MuscleVisual.UpperBack, MuscleVisual.LowerBack -> "Espalda"
    MuscleVisual.Shoulders -> "Hombros"
    MuscleVisual.Biceps, MuscleVisual.Triceps, MuscleVisual.Forearms -> "Brazos"
    MuscleVisual.Calves, MuscleVisual.Glutes, MuscleVisual.Hamstrings, MuscleVisual.Hips,
    MuscleVisual.InnerThigh, MuscleVisual.OuterThigh, MuscleVisual.Quads, MuscleVisual.Legs -> "Piernas"
    MuscleVisual.Abs, MuscleVisual.LowerAbs, MuscleVisual.UpperAbs, MuscleVisual.Obliques -> "Core"
    MuscleVisual.Cardio, MuscleVisual.FullBody, MuscleVisual.Neck -> "Otros"
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
    // Mirrors ExerciseMetadata.isUnilateral in the iOS exercise catalog.
    private val unilateralKeys = setOf(
        "walkingLunge",
        "reverseLunge",
        "bulgarianSplitSquat",
        "stepUp",
        "calfRaise",
        "machineGluteKickback",
        "cableGluteKickback",
        "cableHipAbduction",
        "cableHipAdduction",
        "cableLateralRaise",
        "cableExternalRotation",
        "cableInternalRotation",
        "singleArmDumbbellRow",
        "bayesianCableCurl",
        "sidePlank",
    )

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
        // Keep the canonical iOS exercise identifiers as first-class entries.
        // Synced plans can contain these keys even when locally generated
        // Android plans use the shorter compatibility aliases below.
        "barbellBenchPress" to ExerciseVisualMetadata(listOf(MuscleVisual.Chest), listOf(MuscleVisual.Triceps, MuscleVisual.Shoulders)),
        "inclineBenchPress" to ExerciseVisualMetadata(listOf(MuscleVisual.Chest), listOf(MuscleVisual.Shoulders, MuscleVisual.Triceps)),
        "dumbbellInclineBenchPress" to ExerciseVisualMetadata(listOf(MuscleVisual.Chest), listOf(MuscleVisual.Shoulders, MuscleVisual.Triceps)),
        "overheadPress" to ExerciseVisualMetadata(listOf(MuscleVisual.Shoulders), listOf(MuscleVisual.Triceps, MuscleVisual.Abs)),
        "lateralRaise" to ExerciseVisualMetadata(listOf(MuscleVisual.Shoulders)),
        "chestDip" to ExerciseVisualMetadata(listOf(MuscleVisual.Chest), listOf(MuscleVisual.Triceps, MuscleVisual.Shoulders)),
        "tricepsPushdown" to ExerciseVisualMetadata(listOf(MuscleVisual.Triceps)),
        "pullUp" to ExerciseVisualMetadata(listOf(MuscleVisual.Back), listOf(MuscleVisual.Biceps, MuscleVisual.Shoulders)),
        "latPulldown" to ExerciseVisualMetadata(listOf(MuscleVisual.Back), listOf(MuscleVisual.Biceps)),
        "cableLatPushdown" to ExerciseVisualMetadata(listOf(MuscleVisual.Back), listOf(MuscleVisual.Shoulders)),
        "seatedCableRow" to ExerciseVisualMetadata(listOf(MuscleVisual.Back), listOf(MuscleVisual.Biceps)),
        "bentOverRow" to ExerciseVisualMetadata(listOf(MuscleVisual.Back), listOf(MuscleVisual.Biceps, MuscleVisual.LowerBack)),
        "barbellBentOverRow" to ExerciseVisualMetadata(listOf(MuscleVisual.Back), listOf(MuscleVisual.Biceps, MuscleVisual.LowerBack)),
        "facePull" to ExerciseVisualMetadata(listOf(MuscleVisual.Shoulders, MuscleVisual.Back)),
        "bicepsCurl" to ExerciseVisualMetadata(listOf(MuscleVisual.Biceps)),
        "hammerCurl" to ExerciseVisualMetadata(listOf(MuscleVisual.Biceps)),
        "plank" to ExerciseVisualMetadata(listOf(MuscleVisual.Abs), listOf(MuscleVisual.Shoulders)),
        "sidePlank" to ExerciseVisualMetadata(listOf(MuscleVisual.Obliques), listOf(MuscleVisual.Shoulders)),
        "deadBug" to ExerciseVisualMetadata(listOf(MuscleVisual.Abs)),
        "bicycleCrunch" to ExerciseVisualMetadata(listOf(MuscleVisual.Abs), listOf(MuscleVisual.Obliques)),
        "walking" to ExerciseVisualMetadata(listOf(MuscleVisual.Cardio), listOf(MuscleVisual.Calves)),
        "outdoorRunning" to ExerciseVisualMetadata(listOf(MuscleVisual.Cardio), listOf(MuscleVisual.Quads, MuscleVisual.Hamstrings, MuscleVisual.Glutes, MuscleVisual.Calves)),
        "treadmillWalking" to ExerciseVisualMetadata(listOf(MuscleVisual.Cardio), listOf(MuscleVisual.Calves)),
        "treadmillRunning" to ExerciseVisualMetadata(listOf(MuscleVisual.Cardio), listOf(MuscleVisual.Quads, MuscleVisual.Hamstrings, MuscleVisual.Glutes, MuscleVisual.Calves)),
        "stationaryBike" to ExerciseVisualMetadata(listOf(MuscleVisual.Cardio), listOf(MuscleVisual.Quads, MuscleVisual.Hamstrings, MuscleVisual.Glutes)),
        "elliptical" to ExerciseVisualMetadata(listOf(MuscleVisual.Cardio), listOf(MuscleVisual.Quads, MuscleVisual.Hamstrings, MuscleVisual.Glutes, MuscleVisual.Chest, MuscleVisual.Back)),
        "rowing" to ExerciseVisualMetadata(listOf(MuscleVisual.Cardio), listOf(MuscleVisual.Back, MuscleVisual.Legs, MuscleVisual.Glutes, MuscleVisual.Biceps)),
        "jumpRope" to ExerciseVisualMetadata(listOf(MuscleVisual.Cardio), listOf(MuscleVisual.Calves, MuscleVisual.Shoulders)),
        "stairClimber" to ExerciseVisualMetadata(listOf(MuscleVisual.Cardio), listOf(MuscleVisual.Glutes, MuscleVisual.Quads, MuscleVisual.Hamstrings, MuscleVisual.Calves)),
        "skiErgWork" to ExerciseVisualMetadata(listOf(MuscleVisual.Cardio), listOf(MuscleVisual.Back, MuscleVisual.Shoulders, MuscleVisual.Triceps, MuscleVisual.Abs, MuscleVisual.Legs)),
        "barbellFrontSquat" to ExerciseVisualMetadata(listOf(MuscleVisual.Quads), listOf(MuscleVisual.UpperBack, MuscleVisual.Abs)),
        "smithMachineSquat" to ExerciseVisualMetadata(listOf(MuscleVisual.Quads, MuscleVisual.Glutes), listOf(MuscleVisual.Hamstrings)),
        "dumbbellSquat" to ExerciseVisualMetadata(listOf(MuscleVisual.Quads, MuscleVisual.Glutes), listOf(MuscleVisual.Hamstrings, MuscleVisual.Abs)),
        "reverseLunge" to ExerciseVisualMetadata(listOf(MuscleVisual.Glutes, MuscleVisual.Hamstrings), listOf(MuscleVisual.Quads)),
        "bulgarianSplitSquat" to ExerciseVisualMetadata(listOf(MuscleVisual.Quads), listOf(MuscleVisual.Glutes)),
        "stepUp" to ExerciseVisualMetadata(listOf(MuscleVisual.Glutes), listOf(MuscleVisual.Quads, MuscleVisual.Hamstrings)),
        "legExtension" to ExerciseVisualMetadata(listOf(MuscleVisual.Quads)),
        "hamstringCurl" to ExerciseVisualMetadata(listOf(MuscleVisual.Hamstrings)),
        "lyingLegCurl" to ExerciseVisualMetadata(listOf(MuscleVisual.Hamstrings)),
        "seatedLegCurl" to ExerciseVisualMetadata(listOf(MuscleVisual.Hamstrings)),
        "calfRaise" to ExerciseVisualMetadata(listOf(MuscleVisual.Calves)),
        "hipThrust" to ExerciseVisualMetadata(listOf(MuscleVisual.Glutes), listOf(MuscleVisual.Hamstrings)),
        "gluteBridge" to ExerciseVisualMetadata(listOf(MuscleVisual.Glutes), listOf(MuscleVisual.Hamstrings, MuscleVisual.Abs)),
        "dumbbellRomanianDeadlift" to ExerciseVisualMetadata(listOf(MuscleVisual.Hamstrings, MuscleVisual.Glutes), listOf(MuscleVisual.LowerBack)),
        "machineGluteKickback" to ExerciseVisualMetadata(listOf(MuscleVisual.Glutes)),
        "cableGluteKickback" to ExerciseVisualMetadata(listOf(MuscleVisual.Glutes), listOf(MuscleVisual.Hamstrings, MuscleVisual.LowerBack)),
        "machineHipAbduction" to ExerciseVisualMetadata(listOf(MuscleVisual.Hips), listOf(MuscleVisual.Glutes)),
        "machineHipAdduction" to ExerciseVisualMetadata(listOf(MuscleVisual.Hips)),
        "cableHipAbduction" to ExerciseVisualMetadata(listOf(MuscleVisual.Hips), listOf(MuscleVisual.Glutes)),
        "cableHipAdduction" to ExerciseVisualMetadata(listOf(MuscleVisual.Hips)),
        "boxJump" to ExerciseVisualMetadata(listOf(MuscleVisual.Quads, MuscleVisual.Glutes), listOf(MuscleVisual.Calves, MuscleVisual.Cardio)),
        "declinePushUp" to ExerciseVisualMetadata(listOf(MuscleVisual.Chest), listOf(MuscleVisual.Shoulders, MuscleVisual.Triceps)),
        "dumbbellBenchPress" to ExerciseVisualMetadata(listOf(MuscleVisual.Chest), listOf(MuscleVisual.Triceps, MuscleVisual.Shoulders)),
        "dumbbellChestFly" to ExerciseVisualMetadata(listOf(MuscleVisual.Chest)),
        "smithMachineBenchPress" to ExerciseVisualMetadata(listOf(MuscleVisual.Chest), listOf(MuscleVisual.Triceps, MuscleVisual.Shoulders)),
        "barbellInclineBenchPress" to ExerciseVisualMetadata(listOf(MuscleVisual.Chest), listOf(MuscleVisual.Shoulders, MuscleVisual.Triceps)),
        "smithMachineInclineBenchPress" to ExerciseVisualMetadata(listOf(MuscleVisual.Chest), listOf(MuscleVisual.Shoulders, MuscleVisual.Triceps)),
        "barbellDeclineBenchPress" to ExerciseVisualMetadata(listOf(MuscleVisual.Chest), listOf(MuscleVisual.Triceps, MuscleVisual.Shoulders)),
        "dumbbellDeclineBenchPress" to ExerciseVisualMetadata(listOf(MuscleVisual.Chest), listOf(MuscleVisual.Triceps, MuscleVisual.Shoulders)),
        "smithMachineDeclineBenchPress" to ExerciseVisualMetadata(listOf(MuscleVisual.Chest), listOf(MuscleVisual.Triceps, MuscleVisual.Shoulders)),
        "chestPressMachine" to ExerciseVisualMetadata(listOf(MuscleVisual.Chest), listOf(MuscleVisual.Shoulders, MuscleVisual.Triceps)),
        "chestFly" to ExerciseVisualMetadata(listOf(MuscleVisual.Chest)),
        "cableFly" to ExerciseVisualMetadata(listOf(MuscleVisual.Chest)),
        "cableCrunchPress" to ExerciseVisualMetadata(listOf(MuscleVisual.Abs), listOf(MuscleVisual.Obliques)),
        "highToLowCableFly" to ExerciseVisualMetadata(listOf(MuscleVisual.Chest)),
        "lowToHighCableFly" to ExerciseVisualMetadata(listOf(MuscleVisual.Chest), listOf(MuscleVisual.Shoulders)),
        "pecDeckFly" to ExerciseVisualMetadata(listOf(MuscleVisual.Chest)),
        "dumbbellOverheadPress" to ExerciseVisualMetadata(listOf(MuscleVisual.Shoulders), listOf(MuscleVisual.Triceps)),
        "barbellOverheadPress" to ExerciseVisualMetadata(listOf(MuscleVisual.Shoulders), listOf(MuscleVisual.Triceps, MuscleVisual.Abs)),
        "smithMachineOverheadPress" to ExerciseVisualMetadata(listOf(MuscleVisual.Shoulders), listOf(MuscleVisual.Triceps)),
        "arnoldPress" to ExerciseVisualMetadata(listOf(MuscleVisual.Shoulders), listOf(MuscleVisual.Triceps)),
        "cableLateralRaise" to ExerciseVisualMetadata(listOf(MuscleVisual.Shoulders)),
        "frontRaise" to ExerciseVisualMetadata(listOf(MuscleVisual.Shoulders)),
        "cableFrontRaise" to ExerciseVisualMetadata(listOf(MuscleVisual.Shoulders)),
        "cableRearDeltFly" to ExerciseVisualMetadata(listOf(MuscleVisual.Shoulders)),
        "cableUprightRow" to ExerciseVisualMetadata(listOf(MuscleVisual.Shoulders), listOf(MuscleVisual.UpperBack)),
        "cableExternalRotation" to ExerciseVisualMetadata(listOf(MuscleVisual.Shoulders)),
        "cableInternalRotation" to ExerciseVisualMetadata(listOf(MuscleVisual.Shoulders)),
        "tricepsDip" to ExerciseVisualMetadata(listOf(MuscleVisual.Triceps), listOf(MuscleVisual.Chest, MuscleVisual.Shoulders)),
        "overheadTricepsExtension" to ExerciseVisualMetadata(listOf(MuscleVisual.Triceps)),
        "cableOverheadTricepsExtension" to ExerciseVisualMetadata(listOf(MuscleVisual.Triceps)),
        "ringDip" to ExerciseVisualMetadata(listOf(MuscleVisual.Triceps, MuscleVisual.Chest), listOf(MuscleVisual.Shoulders, MuscleVisual.Abs)),
        "chinUp" to ExerciseVisualMetadata(listOf(MuscleVisual.Back), listOf(MuscleVisual.Biceps)),
        "seatedMachineRow" to ExerciseVisualMetadata(listOf(MuscleVisual.Back), listOf(MuscleVisual.Biceps)),
        "dumbbellBentOverRow" to ExerciseVisualMetadata(listOf(MuscleVisual.Back), listOf(MuscleVisual.Biceps, MuscleVisual.LowerBack)),
        "smithMachineBentOverRow" to ExerciseVisualMetadata(listOf(MuscleVisual.Back), listOf(MuscleVisual.Biceps, MuscleVisual.LowerBack)),
        "singleArmDumbbellRow" to ExerciseVisualMetadata(listOf(MuscleVisual.Back), listOf(MuscleVisual.Biceps)),
        "rearDeltFly" to ExerciseVisualMetadata(listOf(MuscleVisual.Shoulders)),
        "machineRearDeltFly" to ExerciseVisualMetadata(listOf(MuscleVisual.Shoulders)),
        "cableCurl" to ExerciseVisualMetadata(listOf(MuscleVisual.Biceps), listOf(MuscleVisual.Forearms)),
        "bayesianCableCurl" to ExerciseVisualMetadata(listOf(MuscleVisual.Biceps), listOf(MuscleVisual.Forearms)),
        "suspensionRow" to ExerciseVisualMetadata(listOf(MuscleVisual.Back), listOf(MuscleVisual.Biceps, MuscleVisual.Abs)),
        "landmineRow" to ExerciseVisualMetadata(listOf(MuscleVisual.Back), listOf(MuscleVisual.Biceps, MuscleVisual.LowerBack, MuscleVisual.Abs)),
        "birdDog" to ExerciseVisualMetadata(listOf(MuscleVisual.Abs), listOf(MuscleVisual.Glutes, MuscleVisual.LowerBack)),
        "russianTwist" to ExerciseVisualMetadata(listOf(MuscleVisual.Obliques), listOf(MuscleVisual.Abs)),
        "hangingLegRaise" to ExerciseVisualMetadata(listOf(MuscleVisual.Abs)),
        "cableWoodchop" to ExerciseVisualMetadata(listOf(MuscleVisual.Obliques), listOf(MuscleVisual.Abs, MuscleVisual.Shoulders)),
        "burpee" to ExerciseVisualMetadata(listOf(MuscleVisual.FullBody, MuscleVisual.Cardio)),
        "jumpingJacks" to ExerciseVisualMetadata(listOf(MuscleVisual.Cardio), listOf(MuscleVisual.Calves, MuscleVisual.Shoulders)),
        "highKnees" to ExerciseVisualMetadata(listOf(MuscleVisual.Cardio, MuscleVisual.Abs), listOf(MuscleVisual.Quads, MuscleVisual.Hips)),
        "squatJump" to ExerciseVisualMetadata(listOf(MuscleVisual.Quads, MuscleVisual.Glutes), listOf(MuscleVisual.Calves, MuscleVisual.Cardio)),
        "butterflySitUp" to ExerciseVisualMetadata(listOf(MuscleVisual.Abs), listOf(MuscleVisual.Obliques)),
        "sprawl" to ExerciseVisualMetadata(listOf(MuscleVisual.Cardio, MuscleVisual.FullBody), listOf(MuscleVisual.Abs, MuscleVisual.Quads)),
        "pikePushUp" to ExerciseVisualMetadata(listOf(MuscleVisual.Shoulders), listOf(MuscleVisual.Triceps, MuscleVisual.UpperBack)),
        "superman" to ExerciseVisualMetadata(listOf(MuscleVisual.LowerBack), listOf(MuscleVisual.Glutes, MuscleVisual.Hamstrings, MuscleVisual.Shoulders)),
        "handReleasePushUp" to ExerciseVisualMetadata(listOf(MuscleVisual.Chest), listOf(MuscleVisual.Shoulders, MuscleVisual.Triceps, MuscleVisual.Abs)),
        "climber" to ExerciseVisualMetadata(listOf(MuscleVisual.Cardio, MuscleVisual.Abs), listOf(MuscleVisual.Hips, MuscleVisual.Shoulders, MuscleVisual.Quads)),
        "boxingBagWork" to ExerciseVisualMetadata(listOf(MuscleVisual.Cardio, MuscleVisual.FullBody), listOf(MuscleVisual.Shoulders, MuscleVisual.Abs, MuscleVisual.Legs)),
        "catCow" to ExerciseVisualMetadata(listOf(MuscleVisual.LowerBack, MuscleVisual.Back)),
        "hipFlexorStretch" to ExerciseVisualMetadata(listOf(MuscleVisual.Hips)),
        "hamstringStretch" to ExerciseVisualMetadata(listOf(MuscleVisual.Hamstrings)),
        "thoracicRotation" to ExerciseVisualMetadata(listOf(MuscleVisual.Back), listOf(MuscleVisual.Obliques)),
        "shoulderMobilityFlow" to ExerciseVisualMetadata(listOf(MuscleVisual.Shoulders)),
        "worldGreatestStretch" to ExerciseVisualMetadata(listOf(MuscleVisual.Hips, MuscleVisual.Back, MuscleVisual.Hamstrings), listOf(MuscleVisual.Shoulders, MuscleVisual.Quads)),
        "pigeonPose" to ExerciseVisualMetadata(listOf(MuscleVisual.Glutes, MuscleVisual.Hips), listOf(MuscleVisual.LowerBack)),
        "cobraStretch" to ExerciseVisualMetadata(listOf(MuscleVisual.Abs, MuscleVisual.LowerBack), listOf(MuscleVisual.Shoulders)),
        "childsPose" to ExerciseVisualMetadata(listOf(MuscleVisual.LowerBack, MuscleVisual.Back), listOf(MuscleVisual.Hips, MuscleVisual.Shoulders)),
        "inchworm" to ExerciseVisualMetadata(listOf(MuscleVisual.Hamstrings, MuscleVisual.Abs, MuscleVisual.Shoulders)),
        "armCircles" to ExerciseVisualMetadata(listOf(MuscleVisual.Shoulders)),
        "legSwings" to ExerciseVisualMetadata(listOf(MuscleVisual.Hips), listOf(MuscleVisual.Hamstrings, MuscleVisual.Quads, MuscleVisual.Glutes)),
        "wristCircles" to ExerciseVisualMetadata(listOf(MuscleVisual.Forearms)),
        "ankleCircles" to ExerciseVisualMetadata(listOf(MuscleVisual.Calves)),
        "deepSquatHold" to ExerciseVisualMetadata(listOf(MuscleVisual.Hips, MuscleVisual.Calves, MuscleVisual.Quads), listOf(MuscleVisual.LowerBack, MuscleVisual.Glutes)),
        "scapularCircles" to ExerciseVisualMetadata(listOf(MuscleVisual.UpperBack, MuscleVisual.Shoulders)),
        "quadStretch" to ExerciseVisualMetadata(listOf(MuscleVisual.Quads), listOf(MuscleVisual.Hips)),
        "doorwayChestStretch" to ExerciseVisualMetadata(listOf(MuscleVisual.Chest), listOf(MuscleVisual.Shoulders)),
        "scorpions" to ExerciseVisualMetadata(listOf(MuscleVisual.LowerBack, MuscleVisual.Hips), listOf(MuscleVisual.Chest, MuscleVisual.Abs)),
        "threadTheNeedle" to ExerciseVisualMetadata(listOf(MuscleVisual.UpperBack, MuscleVisual.Shoulders), listOf(MuscleVisual.Back, MuscleVisual.Neck)),
        "neckCircles" to ExerciseVisualMetadata(listOf(MuscleVisual.Neck)),
        "mountainClimber" to ExerciseVisualMetadata(listOf(MuscleVisual.Cardio), listOf(MuscleVisual.Abs, MuscleVisual.Shoulders)),
        "lyingLegRaise" to ExerciseVisualMetadata(listOf(MuscleVisual.Abs), listOf(MuscleVisual.Hips)),
        "barbellRomanianDeadlift" to ExerciseVisualMetadata(listOf(MuscleVisual.Hamstrings, MuscleVisual.Glutes), listOf(MuscleVisual.LowerBack)),
    )

    internal fun metadata(imageKey: String?): ExerciseVisualMetadata? = imageKey?.let { key ->
        definitions[key] ?: definitions.entries.firstOrNull { it.key.equals(key, ignoreCase = true) }?.value
    }

    /** Stable exercise ids used by the related-exercises section of iOS details. */
    internal fun knownKeys(): Set<String> = definitions.keys

    internal fun isUnilateral(imageKey: String?): Boolean = imageKey?.trim() in unilateralKeys

    fun primaryMuscleLabels(imageKey: String?): List<String> = metadata(imageKey)?.primary?.map(MuscleVisual::label).orEmpty()

    /**
     * Groups the detailed catalog muscles into the major groups used by iOS
     * analytics. The first list is primary, the second is secondary.
     */
    fun majorMuscleGroups(imageKey: String?): Pair<List<String>, List<String>> {
        val exercise = metadata(imageKey) ?: return emptyList<String>() to emptyList()
        val primary = exercise.primary.map(MuscleVisual::majorAnalyticsGroup).distinct()
        val secondary = exercise.secondary.map(MuscleVisual::majorAnalyticsGroup).filterNot(primary::contains).distinct()
        return primary to secondary
    }

    /**
     * Fatigue follows iOS's detailed-muscle accumulation before it aggregates
     * to major groups, so same-major detailed muscles intentionally remain
     * repeated in the returned lists.
     */
    fun fatigueMajorMuscleGroups(imageKey: String?): Pair<List<String>, List<String>> {
        val exercise = metadata(imageKey) ?: return emptyList<String>() to emptyList()
        val primaryDetails = exercise.primary.toSet()
        return exercise.primary.map(MuscleVisual::majorAnalyticsGroup) to
            exercise.secondary.filterNot(primaryDetails::contains).map(MuscleVisual::majorAnalyticsGroup)
    }

    /** Matches iOS: choose the alphabetically first primary `MuscleGroup.rawValue`. */
    fun primaryMuscleDrawable(imageKey: String?): Int? = metadata(imageKey)
        ?.primary
        ?.minByOrNull(MuscleVisual::iosIdentifier)
        ?.drawable

    fun equipmentFor(imageKey: String?): List<String> = when (imageKey?.trim()) {
        "benchPress", "inclineBenchPress", "chestDip" -> listOf("Banco ajustable")
        "dumbbellInclineBenchPress" -> listOf("Banco ajustable", "Mancuernas")
        "latPulldown", "cableLatPushdown", "seatedCableRow", "facePull", "tricepsPushdown" -> listOf("Máquina de poleas")
        "barbellBackSquat", "deadlift", "romanianDeadlift", "bentOverRow" -> listOf("Barra y discos")
        "gobletSquat", "overheadPress", "lateralRaise", "bicepsCurl", "hammerCurl" -> listOf("Mancuernas")
        "pullUp" -> listOf("Barra de dominadas")
        "legPress" -> listOf("Prensa de piernas")
        else -> listOf("Peso corporal")
    }

    /** Uses the same equipment illustrations as iOS's WorkoutEquipmentScroll. */
    internal fun equipmentVisualsFor(imageKey: String?): List<EquipmentVisual> = when (imageKey?.trim()) {
        "benchPress", "inclineBenchPress", "chestDip" -> listOf(EquipmentVisual.AdjustableBench)
        "dumbbellInclineBenchPress" -> listOf(EquipmentVisual.AdjustableBench, EquipmentVisual.Dumbbells)
        "latPulldown", "cableLatPushdown", "seatedCableRow", "facePull", "tricepsPushdown" -> listOf(EquipmentVisual.CableMachine)
        "barbellBackSquat", "deadlift", "romanianDeadlift", "bentOverRow" -> listOf(EquipmentVisual.OlympicBarbell)
        "gobletSquat", "overheadPress", "lateralRaise", "bicepsCurl", "hammerCurl" -> listOf(EquipmentVisual.Dumbbells)
        "pullUp" -> listOf(EquipmentVisual.PullUpBar)
        "legPress" -> listOf(EquipmentVisual.LegPressMachine)
        else -> emptyList()
    }
}

@Composable
internal fun MuscleStrip(imageKey: String?, onDarkBackground: Boolean, modifier: Modifier = Modifier, wrapContent: Boolean = false) {
    val metadata = ExerciseVisualCatalog.metadata(imageKey) ?: run {
        return
    }
    val tint = if (onDarkBackground) Color.White else Color.Unspecified
    val muscles = metadata.primary.map { it to true } + metadata.secondary.map { it to false }
    val primaryNames = metadata.primary.map { tutorialMuscleName(it) }.sorted()
    val secondaryNames = metadata.secondary.map { tutorialMuscleName(it) }.sorted()
    val primaryLabel = tutorialText("Primary muscles: %@").replace(": %@", "")
    val secondaryLabel = tutorialText("Secondary muscles: %@").replace(": %@", "")
    val muscleAccessibilityLabel = buildString {
        primaryNames.takeIf { it.isNotEmpty() }?.let {
            append("$primaryLabel: ")
            append(it.joinToString(", "))
        }
        secondaryNames.takeIf { it.isNotEmpty() }?.let {
            if (isNotEmpty()) append(". ")
            append("$secondaryLabel: ")
            append(it.joinToString(", "))
        }
    }
    if (wrapContent) {
        Column(modifier.clearAndSetSemantics { contentDescription = muscleAccessibilityLabel }) {
            muscles.chunked(3).forEach { row ->
                Row(verticalAlignment = Alignment.CenterVertically) { row.forEach { (muscle, _) -> MuscleIcon(muscle) } }
            }
        }
    } else {
        Row(
            modifier
                .horizontalScroll(rememberScrollState())
                .clearAndSetSemantics { contentDescription = muscleAccessibilityLabel },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            muscles.forEachIndexed { index, (muscle, primary) ->
                if (index == metadata.primary.size && metadata.secondary.isNotEmpty()) {
                    Divider(
                        modifier = Modifier
                            .padding(horizontal = 2.dp, vertical = 6.dp)
                            .height(40.dp)
                            .width(2.dp),
                        color = if (onDarkBackground) Color.White.copy(alpha = 0.20f) else Color.Black.copy(alpha = 0.12f),
                    )
                }
                MuscleIcon(muscle)
            }
        }
    }
}

@Composable
private fun MuscleIcon(muscle: MuscleVisual) {
    // ActiveWorkoutSceneView on iOS gives every muscle icon the same 58pt
    // frame.  Keeping primary and secondary icons equal prevents the divider
    // row from appearing vertically misaligned on Android.
    Box(Modifier.padding(end = 6.dp).size(58.dp).clip(RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(muscle.drawable),
            contentDescription = tutorialMuscleName(muscle),
            modifier = Modifier.size(58.dp),
        )
    }
}
