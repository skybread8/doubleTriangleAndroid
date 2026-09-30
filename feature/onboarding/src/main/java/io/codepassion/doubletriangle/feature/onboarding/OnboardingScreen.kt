package io.codepassion.doubletriangle.feature.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.AlertDialog
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Icon
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Remove
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import io.codepassion.doubletriangle.core.designsystem.AntonFontFamily
import io.codepassion.doubletriangle.core.designsystem.WildforceTheme
import io.codepassion.doubletriangle.core.designsystem.WildforceThemeTokens
import io.codepassion.doubletriangle.core.designsystem.liquidGlass
import io.codepassion.doubletriangle.core.designsystem.liquidGlassBackground
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

data class OnboardingProfile(
    val name: String,
    val goal: FitnessGoal,
    val lifestyle: LifestyleLevel = LifestyleLevel.ModeratelyActive,
    val workoutDays: Set<WorkoutWeekday> = setOf(WorkoutWeekday.Monday, WorkoutWeekday.Wednesday, WorkoutWeekday.Friday),
    val preferredWorkoutDurationMinutes: Int = 50,
    val trainingLevel: TrainingLevel = TrainingLevel.Novice,
    val trainingSplitPreference: TrainingSplitPreference = TrainingSplitPreference.Automatic,
    val bodyCompositionPhase: BodyCompositionPhase? = null,
    val customWorkoutFocuses: Map<WorkoutWeekday, WorkoutFocus> = emptyMap(),
    val gymType: GymType = GymType.SmallGym,
    val availableEquipment: Set<Equipment> = GymType.SmallGym.defaultEquipment,
    val movementRestrictions: Set<MovementRestriction> = emptySet(),
    val isHealthConnectEnabled: Boolean = false,
    val birthMonth: Int = 1,
    val birthYear: Int = 1995,
    val gender: Gender = Gender.Male,
    val metricSystem: MetricSystem = MetricSystem.Metric,
    val heightCm: Int = 175,
    val weightKg: Double = 70.0,
    val skipsWarmups: Boolean = false,
    val skipsCooldowns: Boolean = false,
    val skipsRestPeriods: Boolean = false,
    val workoutPlannerNotes: String = "",
    val trainingLocations: List<TrainingLocationProfile> = emptyList(),
    val appLanguage: String = "Español",
)

data class OnboardingPlanPreview(
    val name: String,
    val phase: String,
    val workouts: List<String>,
)

enum class LifestyleLevel(val storedValue: String, val title: String, val description: String, val glyph: String) {
    Sedentary("sedentary", "Sedentario", "Pasas la mayor parte del día sentado.", "□"),
    LightlyActive("lightlyActive", "Ligeramente activo", "Pasas buena parte del día de pie.", "│"),
    ModeratelyActive("moderatelyActive", "Moderadamente activo", "Pasas gran parte del día moviéndote.", "→"),
    VeryActive("veryActive", "Muy activo", "Realizas actividad física durante la mayor parte del día.", "↗"),
    ;

    companion object {
        fun fromStoredValue(value: String) = entries.firstOrNull { it.storedValue == value } ?: ModeratelyActive
    }
}

enum class WorkoutWeekday(val storedValue: String, val title: String, val glyph: String) {
    Monday("monday", "Lunes", "L"), Tuesday("tuesday", "Martes", "M"),
    Wednesday("wednesday", "Miércoles", "X"), Thursday("thursday", "Jueves", "J"),
    Friday("friday", "Viernes", "V"), Saturday("saturday", "Sábado", "S"),
    Sunday("sunday", "Domingo", "D");

    companion object {
        fun fromStoredValues(values: Set<String>?) =
            entries.filterTo(mutableSetOf()) { it.storedValue in values.orEmpty() }
                .ifEmpty { setOf(Monday, Wednesday, Friday) }
    }
}

enum class TrainingLevel(val storedValue: String, val title: String, val description: String, val glyph: String) {
    CompleteBeginner("completeBeginner", "Principiante total", "Estás empezando con el entrenamiento estructurado y los movimientos básicos.", "○"),
    Beginner("beginner", "Principiante", "Tienes algo de experiencia y estás desarrollando constancia y técnica.", "◔"),
    Novice("novice", "Novato", "Entrenas con cierta regularidad y sigues progresiones sencillas.", "◑"),
    Intermediate("intermediate", "Intermedio", "Tienes hábitos sólidos y toleras un volumen de entrenamiento moderado.", "◕"),
    Advanced("advanced", "Avanzado", "Entrenas de forma constante y dominas bien la progresión.", "●"),
    Elite("elite", "Élite", "Tienes una base muy alta y puedes seguir planes especializados.", "◆");

    companion object {
        fun fromStoredValue(value: String) = entries.firstOrNull { it.storedValue == value } ?: Novice
    }
}

enum class WorkoutFocus(val storedValue:String,val title:String){FullBody("fullBody","Cuerpo completo"),UpperBody("upperBody","Tren superior"),LowerBody("lowerBody","Tren inferior"),Push("push","Empuje"),Pull("pull","Tirón"),Legs("legs","Piernas"),Core("core","Core"),Cardio("cardio","Cardio"),Mobility("mobility","Movilidad"),Recovery("recovery","Recuperación"); companion object { val customSelectionCases get() = entries.filter { it != Mobility && it != Recovery } }}
enum class Equipment(val storedValue:String,val title:String){
Bodyweight("bodyweight","Peso corporal"),ResistanceBands("resistanceBands","Bandas"),Dumbbells("dumbbells","Mancuernas"),Kettlebells("kettlebells","Kettlebells"),MedicineBall("medicineBall","Balón medicinal"),BattleRopes("battleRopes","Cuerdas"),JumpRope("jumpRope","Comba"),SuspensionTrainer("suspensionTrainer","TRX"),GymnasticRings("gymnasticRings","Anillas"),CableMachine("cableMachine","Poleas"),SmithMachine("smithMachine","Máquina Smith"),LegPressMachine("legPressMachine","Prensa"),ChestPressMachine("chestPressMachine","Press de pecho"),LegCurlMachine("legCurlMachine","Curl femoral"),RearDeltMachine("rearDeltMachine","Deltoide posterior"),SeatedRowMachine("seatedRowMachine","Remo sentado"),GluteKickbackMachine("gluteKickbackMachine","Patada glúteo"),PecDeckMachine("pecDeckMachine","Pec deck"),HipAbductionMachine("hipAbductionMachine","Abductores"),HipAdductionMachine("hipAdductionMachine","Aductores"),RowingMachine("rowingMachine","Remo cardio"),Treadmill("treadmill","Cinta"),StationaryBike("stationaryBike","Bicicleta"),Elliptical("elliptical","Elíptica"),StairClimber("stairClimber","Escaladora"),SkiErg("skiErg","Ski erg"),FlatBench("flatBench","Banco plano"),AdjustableBench("adjustableBench","Banco ajustable"),SquatRack("squatRack","Rack"),OlympicBarbell("olympicBarbell","Barra olímpica"),EzBar("ezBar","Barra EZ"),TrapBar("trapBar","Trap bar"),DeadliftPlatform("deadliftPlatform","Plataforma"),PullUpBar("pullUpBar","Dominadas"),DipStation("dipStation","Paralelas"),PlyoBox("plyoBox","Cajón"),BoxingBag("boxingBag","Saco"),LandmineAttachment("landmineAttachment","Landmine");
companion object{fun fromStoredValues(values:Set<String>?)=entries.filterTo(mutableSetOf()){it.storedValue in values.orEmpty()}}}

private val Equipment.category: String
    get() = when (this) {
        Equipment.Bodyweight -> "Peso corporal"
        Equipment.ResistanceBands, Equipment.Dumbbells, Equipment.Kettlebells, Equipment.OlympicBarbell, Equipment.EzBar, Equipment.TrapBar -> "Pesos libres"
        Equipment.CableMachine, Equipment.SmithMachine, Equipment.LegPressMachine, Equipment.ChestPressMachine, Equipment.LegCurlMachine, Equipment.RearDeltMachine, Equipment.SeatedRowMachine, Equipment.GluteKickbackMachine, Equipment.PecDeckMachine, Equipment.HipAbductionMachine, Equipment.HipAdductionMachine, Equipment.FlatBench, Equipment.AdjustableBench, Equipment.SquatRack, Equipment.DeadliftPlatform, Equipment.PullUpBar, Equipment.DipStation -> "Máquinas y estaciones"
        Equipment.RowingMachine, Equipment.Treadmill, Equipment.StationaryBike, Equipment.Elliptical, Equipment.StairClimber, Equipment.SkiErg, Equipment.JumpRope -> "Cardio"
        else -> "Funcional"
    }

data class TrainingLocationProfile(
    val name: String,
    val equipment: Set<Equipment>,
    val isDefault: Boolean = false,
)

fun OnboardingProfile.effectiveTrainingLocations(): List<TrainingLocationProfile> = trainingLocations.ifEmpty {
    listOf(TrainingLocationProfile("Mi gimnasio", availableEquipment.ifEmpty { setOf(Equipment.Bodyweight) }, true))
}
enum class GymType(val storedValue:String,val title:String,val glyph:String){
BigGym("bigGym","Gimnasio grande","▦"),SmallGym("smallGym","Gimnasio pequeño","▤"),HomeGym("homeGym","Gimnasio en casa","⌂"),SomeAccessories("someAccessories","Algunos accesorios","◆"),BodyweightOnly("bodyweightOnly","Solo peso corporal","◎");
val defaultEquipment:Set<Equipment> get()=when(this){BigGym->Equipment.entries.toSet();SmallGym->setOf(Equipment.Bodyweight,Equipment.ResistanceBands,Equipment.Dumbbells,Equipment.Kettlebells,Equipment.MedicineBall,Equipment.CableMachine,Equipment.SmithMachine,Equipment.LegPressMachine,Equipment.ChestPressMachine,Equipment.RowingMachine,Equipment.Treadmill,Equipment.StationaryBike,Equipment.Elliptical,Equipment.StairClimber,Equipment.FlatBench,Equipment.AdjustableBench,Equipment.SquatRack,Equipment.OlympicBarbell,Equipment.EzBar,Equipment.PullUpBar,Equipment.DipStation);HomeGym->setOf(Equipment.Bodyweight,Equipment.Dumbbells,Equipment.Kettlebells,Equipment.FlatBench,Equipment.AdjustableBench,Equipment.SquatRack,Equipment.OlympicBarbell,Equipment.EzBar,Equipment.PullUpBar);SomeAccessories->setOf(Equipment.Bodyweight,Equipment.ResistanceBands,Equipment.Dumbbells,Equipment.Kettlebells,Equipment.MedicineBall);BodyweightOnly->setOf(Equipment.Bodyweight)}
companion object{fun fromStoredValue(value:String)=entries.firstOrNull{it.storedValue==value}?:SmallGym}}

enum class Gender(val storedValue:String,val title:String,val glyph:String){Male("male","Hombre","♂"),Female("female","Mujer","♀");companion object{fun fromStoredValue(value:String)=entries.firstOrNull{it.storedValue==value}?:Male}}
enum class MetricSystem(val storedValue:String,val title:String){Metric("metric","Métrico"),Imperial("imperial","Imperial");companion object{fun fromStoredValue(value:String)=entries.firstOrNull{it.storedValue==value}?:Metric}}

enum class MovementRestriction(val storedValue:String,val title:String){
LowerBackPain("lowerBackPain","Dolor lumbar"),ShoulderPain("shoulderPain","Dolor de hombro"),KneePain("kneePain","Dolor de rodilla"),HipPain("hipPain","Dolor de cadera"),AnklePain("anklePain","Dolor de tobillo"),WristPain("wristPain","Dolor de muñeca"),ElbowPain("elbowPain","Dolor de codo"),NeckPain("neckPain","Dolor de cuello"),LimitedShoulderMobility("limitedShoulderMobility","Movilidad limitada de hombro"),LimitedHipMobility("limitedHipMobility","Movilidad limitada de cadera"),LimitedAnkleMobility("limitedAnkleMobility","Movilidad limitada de tobillo"),LimitedKneeFlexion("limitedKneeFlexion","Flexión limitada de rodilla"),OverheadMovementLimitation("overheadMovementLimitation","Limitación sobre la cabeza"),ImpactSensitivity("impactSensitivity","Sensibilidad al impacto");
companion object{fun fromStoredValues(values:Set<String>?)=entries.filterTo(mutableSetOf()){it.storedValue in values.orEmpty()}}}

enum class TrainingSplitPreference(val storedValue: String, val title: String, val description: String, val glyph: String) {
    Automatic("automatic", "Automático", "Elegiremos la estructura que mejor encaje con tu objetivo y recuperación.", "✦"),
    FullBody("fullBody", "Cuerpo completo", "Sesiones de cuerpo completo distribuidas durante la semana.", "◎"),
    UpperLower("upperLower", "Tren superior / inferior", "Alterna sesiones de parte superior e inferior.", "↕"),
    PushPullLegs("pushPullLegs", "Empuje / tirón / piernas", "Divide el trabajo en empuje, tirón y piernas.", "△"),
    BodyPartSplit("bodyPartSplit", "Por grupos musculares", "Da más volumen específico a cada grupo muscular.", "◇"),
    Custom("custom", "Personalizado", "Define una estructura específica para cada día seleccionado.", "☷");

    fun warning(days: Int): String? = when {
        this == UpperLower && days % 2 != 0 -> "Esta estructura suele funcionar mejor con 2, 4 o 6 días."
        this == PushPullLegs && days % 3 != 0 -> "Esta estructura está pensada principalmente para 3 o 6 días."
        this == BodyPartSplit && days < 5 -> "La división por grupos suele necesitar al menos 5 días."
        else -> null
    }

    companion object {
        fun fromStoredValue(value: String) = entries.firstOrNull { it.storedValue == value } ?: Automatic
    }
}

internal fun normalizedCustomFocuses(
    current: Map<WorkoutWeekday, WorkoutFocus>,
    workoutDays: Set<WorkoutWeekday>,
    previousSplit: TrainingSplitPreference,
): Map<WorkoutWeekday, WorkoutFocus> {
    val orderedDays = workoutDays.sortedBy { it.ordinal }
    val valid = current.filter { (day, focus) -> day in workoutDays && focus in WorkoutFocus.customSelectionCases }.toMutableMap()
    val pattern = when (previousSplit) {
        TrainingSplitPreference.FullBody -> listOf(WorkoutFocus.FullBody)
        TrainingSplitPreference.UpperLower -> listOf(WorkoutFocus.UpperBody, WorkoutFocus.LowerBody)
        TrainingSplitPreference.PushPullLegs -> listOf(WorkoutFocus.Push, WorkoutFocus.Pull, WorkoutFocus.Legs)
        TrainingSplitPreference.BodyPartSplit -> listOf(WorkoutFocus.Push, WorkoutFocus.Pull, WorkoutFocus.Legs, WorkoutFocus.UpperBody, WorkoutFocus.LowerBody)
        else -> when (orderedDays.size) {
            1 -> listOf(WorkoutFocus.FullBody)
            2, 4 -> listOf(WorkoutFocus.UpperBody, WorkoutFocus.LowerBody)
            5 -> listOf(WorkoutFocus.Push, WorkoutFocus.Pull, WorkoutFocus.Legs, WorkoutFocus.UpperBody, WorkoutFocus.LowerBody)
            else -> listOf(WorkoutFocus.Push, WorkoutFocus.Pull, WorkoutFocus.Legs)
        }
    }
    orderedDays.forEachIndexed { index, day -> valid.putIfAbsent(day, pattern[index % pattern.size]) }
    return valid
}

enum class BodyCompositionPhase(val storedValue: String, val title: String, val description: String, val glyph: String) {
    Automatic("automatic", "Automático", "Elegiremos el énfasis adecuado según tu objetivo, métricas y evolución.", "✦"),
    Bulk("bulk", "Volumen", "Prioriza el crecimiento muscular y la sobrecarga progresiva.", "↗"),
    Cut("cut", "Definición", "Prioriza perder grasa manteniendo fuerza y masa muscular.", "↘"),
    Maintain("maintain", "Mantenimiento", "Mantiene un equilibrio estable de volumen e intensidad.", "=");

    companion object {
        fun fromStoredValue(value: String) = entries.firstOrNull { it.storedValue == value } ?: Automatic
    }
}

val TrainingLevel.supportsBodyComposition: Boolean
    get() = this == TrainingLevel.Intermediate || this == TrainingLevel.Advanced || this == TrainingLevel.Elite

val FitnessGoal.supportsBodyComposition: Boolean
    get() = this == FitnessGoal.LoseWeight || this == FitnessGoal.BuildMuscle ||
        this == FitnessGoal.GainStrength || this == FitnessGoal.BodyRecomposition

enum class FitnessGoal(val storedValue: String, val title: String, val description: String, val glyph: String) {
    LoseWeight("loseWeight", "Perder peso", "Quema grasa y mejora tu salud metabólica.", "↘"),
    BuildMuscle("buildMuscle", "Ganar músculo", "Aumenta masa muscular con entrenamiento de hipertrofia.", "◆"),
    GainStrength("gainStrength", "Ganar fuerza", "Levanta más peso y mejora tus movimientos principales.", "ϟ"),
    ImproveEndurance("improveEndurance", "Mejorar resistencia", "Aumenta tu capacidad cardiovascular y energía.", "∞"),
    ImproveMobility("improveMobility", "Mejorar movilidad", "Mejora rango de movimiento, flexibilidad y articulaciones.", "◎"),
    BodyRecomposition("bodyRecomposition", "Recomposición corporal", "Pierde grasa mientras desarrollas músculo.", "◐"),
    GeneralFitness("generalFitness", "Fitness general", "Mantén un estilo de vida sano y equilibrado.", "♥"),
    ;

    companion object {
        fun fromStoredValue(value: String): FitnessGoal = entries.firstOrNull { it.storedValue == value } ?: BuildMuscle
    }
}

@Composable
fun OnboardingScreen(
    onRequestHealthConnect: ((((Boolean, Int?, Double?) -> Unit) -> Unit)) = { result -> result(false, null, null) },
    isGenerating: Boolean = false,
    generationError: String? = null,
    generatedPlanPreview: OnboardingPlanPreview? = null,
    onStartTraining: () -> Unit = {},
    onLanguageSelected: (String) -> Unit = {},
    onCompleted: (OnboardingProfile, Boolean) -> Unit,
) {
    var step by remember { mutableStateOf(0) }
    var name by remember { mutableStateOf("") }
    var goal by remember { mutableStateOf<FitnessGoal?>(null) }
    var lifestyle by remember { mutableStateOf<LifestyleLevel?>(null) }
    var workoutDays by remember { mutableStateOf(setOf(WorkoutWeekday.Monday, WorkoutWeekday.Wednesday, WorkoutWeekday.Friday)) }
    var duration by remember { mutableStateOf(50) }
    var trainingLevel by remember { mutableStateOf(TrainingLevel.Novice) }
    var trainingSplit by remember { mutableStateOf(TrainingSplitPreference.Automatic) }
    var bodyPhase by remember { mutableStateOf(BodyCompositionPhase.Automatic) }
    var customFocuses by remember { mutableStateOf<Map<WorkoutWeekday, WorkoutFocus>>(emptyMap()) }
    var gymType by remember { mutableStateOf<GymType?>(null) }
    var equipment by remember { mutableStateOf(setOf(Equipment.Bodyweight)) }
    var equipmentSearch by remember { mutableStateOf("") }
    var hasRestrictions by remember { mutableStateOf(false) }
    var restrictions by remember { mutableStateOf(emptySet<MovementRestriction>()) }
    var healthConnectEnabled by remember { mutableStateOf<Boolean?>(null) }
    var importedHeight by remember { mutableStateOf(false) }
    var importedWeight by remember { mutableStateOf(false) }
    var generationAttempt by remember { mutableStateOf(0) }
    var appLanguage by remember { mutableStateOf("Español") }
    var isLanguagePickerVisible by remember { mutableStateOf(false) }
    val currentYear=java.time.Year.now().value
    val onboardingLocale = when (appLanguage) {
        "English" -> Locale.ENGLISH
        "Català" -> Locale.forLanguageTag("ca")
        "Français" -> Locale.FRENCH
        "Italiano" -> Locale.ITALIAN
        "Português" -> Locale.forLanguageTag("pt")
        "Deutsch" -> Locale.GERMAN
        "中文（简体）" -> Locale.forLanguageTag("zh-Hans")
        "Nederlands" -> Locale.forLanguageTag("nl")
        "日本語" -> Locale.JAPANESE
        else -> Locale.forLanguageTag("es")
    }
    val monthNames=(1..12).map { month -> Month.of(month).getDisplayName(TextStyle.FULL, onboardingLocale).replaceFirstChar { it.titlecase(onboardingLocale) } }
    var birthMonth by remember{mutableStateOf(1)};var birthYear by remember{mutableStateOf(1995)};var gender by remember{mutableStateOf<Gender?>(null)};var metricSystem by remember{mutableStateOf(MetricSystem.Metric)};var heightCm by remember{mutableStateOf(175)};var weightKg by remember{mutableStateOf(70.0)}
    fun showsBodyComposition() = goal?.supportsBodyComposition == true && trainingLevel.supportsBodyComposition
    fun visibleSteps() = (1..16).filter { current ->
        when (current) {
            8 -> showsBodyComposition()
            15 -> !importedHeight
            16 -> !importedWeight
            else -> true
        }
    }
    fun nextVisibleStep(after: Int) = visibleSteps().firstOrNull { it > after } ?: 18
    fun previousVisibleStep(before: Int) = visibleSteps().lastOrNull { it < before } ?: 0
    fun progressFor(current: Int): Float = ((visibleSteps().indexOf(current) + 1).coerceAtLeast(0).toFloat() / visibleSteps().size.coerceAtLeast(1))
    val completeProfile: (Boolean) -> Unit = { useAi ->
        val selectedGoal = goal
        val selectedLifestyle = lifestyle
        if (selectedGoal != null && selectedLifestyle != null) {
            val selectedBodyPhase = bodyPhase.takeIf { selectedGoal.supportsBodyComposition && trainingLevel.supportsBodyComposition }
            onCompleted(OnboardingProfile(name.trim(), selectedGoal, selectedLifestyle, workoutDays, duration, trainingLevel, trainingSplit, selectedBodyPhase, customFocuses.filterKeys { it in workoutDays }, gymType ?: GymType.SmallGym, equipment, restrictions, healthConnectEnabled == true, birthMonth, birthYear, gender ?: Gender.Male, metricSystem, heightCm, weightKg, appLanguage = appLanguage), useAi)
        }
    }

    Box(Modifier.fillMaxSize()) {
    // Mirrors the iOS progressive transition: forward steps enter from the
    // trailing edge, while going back reverses that direction.
    AnimatedContent(
        targetState = step,
        transitionSpec = {
            if (targetState > initialState) {
                (slideInHorizontally(tween(350)) { it / 3 } + fadeIn(tween(250))) togetherWith
                    (slideOutHorizontally(tween(280)) { -it / 5 } + fadeOut(tween(180)))
            } else {
                (slideInHorizontally(tween(350)) { -it / 3 } + fadeIn(tween(250))) togetherWith
                    (slideOutHorizontally(tween(280)) { it / 5 } + fadeOut(tween(180)))
            }
        },
        label = "onboarding-step",
    ) { currentStep ->
        when (currentStep) {
            0 -> CoverStep { step = 1 }
            1 -> FormStep(
                progress = progressFor(1),
                title = "Bienvenido",
                subtitle = "¿Cómo quieres que te llamemos?",
                canContinue = name.isNotBlank(),
                onBack = { step = 0 },
                onContinue = { step = 2 },
            ) {
                NameStepInput(name = name, onNameChange = { name = it })
            }
            2 -> FormStep(
                progress = progressFor(2),
                title = "Tus objetivos",
                subtitle = "Elige el objetivo de entrenamiento que mejor encaja contigo.",
                canContinue = goal != null,
                onBack = { step = 1 },
                onContinue = { step = 3 },
            ) {
                FitnessGoal.entries.forEach { item ->
                    GoalOption(item = item, selected = goal == item, onClick = { goal = item })
                    Spacer(Modifier.height(10.dp))
                }
            }
            3 -> FormStep(
                progress = progressFor(3),
                title = "Estilo de vida",
                subtitle = "Cuéntanos cuál es tu actividad durante un día normal.",
                canContinue = lifestyle != null,
                onBack = { step = 2 },
                onContinue = { step = 4 },
            ) {
                LifestyleLevel.entries.forEach { item ->
                    ChoiceOption(item.title, item.description, item.glyph, lifestyle == item) { lifestyle = item }
                    Spacer(Modifier.height(10.dp))
                }
            }
            4 -> FormStep(
                progress = progressFor(4),
                title = "Disponibilidad",
                subtitle = "Selecciona cuándo quieres entrenar cada semana.",
                canContinue = workoutDays.isNotEmpty(),
                onBack = { step = 3 },
                onContinue = { step = 5 },
            ) {
                WorkoutWeekday.entries.forEach { day ->
                    ChoiceOption(day.title, null, day.glyph, day in workoutDays) {
                        workoutDays = if (day in workoutDays && workoutDays.size > 1) workoutDays - day else workoutDays + day
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }
            5 -> FormStep(
                progress = progressFor(5),
                title = "Duración del entrenamiento",
                subtitle = "Indica la duración de sesión que quieres que optimicemos.",
                canContinue = true,
                onBack = { step = 4 },
                onContinue = { step = 6 },
            ) {
                Row(
                    Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(16.dp)).padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CenteredOnboardingControl("−", 48.dp) { duration = (duration - 5).coerceAtLeast(15) }
                    Text("$duration min", style = MaterialTheme.typography.h5, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
                    CenteredOnboardingControl("+", 48.dp) { duration = (duration + 5).coerceAtMost(180) }
                }
            }
            6 -> FormStep(
                progress = progressFor(6),
                title = "Experiencia",
                subtitle = "Selecciona el nivel que mejor describe tu experiencia entrenando.",
                canContinue = true,
                onBack = { step = 5 },
                onContinue = { step = 7 },
            ) {
                TrainingLevel.entries.forEach { level ->
                    ChoiceOption(level.title, level.description, level.glyph, trainingLevel == level) { trainingLevel = level }
                    Spacer(Modifier.height(10.dp))
                }
            }
            7 -> FormStep(
                progress=progressFor(7),title="Programa",subtitle="Elige la estructura de entrenamiento que prefieres.",
                canContinue=trainingSplit!=TrainingSplitPreference.Custom||workoutDays.all{it in customFocuses},
                onBack={step=6},onContinue={step=nextVisibleStep(7)},
            ){
                trainingSplit.warning(workoutDays.size)?.let{Text(it,Modifier.fillMaxWidth().background(Color(0xFFFFB020).copy(alpha=.14f),RoundedCornerShape(12.dp)).padding(12.dp));Spacer(Modifier.height(12.dp))}
                TrainingSplitPreference.entries.forEach{split->ChoiceOption(split.title,split.description,split.glyph,trainingSplit==split){
                    val previousSplit = trainingSplit
                    trainingSplit=split
                    if(split==TrainingSplitPreference.Custom)customFocuses=normalizedCustomFocuses(customFocuses, workoutDays, previousSplit)
                };Spacer(Modifier.height(10.dp))}
                if(trainingSplit==TrainingSplitPreference.Custom){
                    Text("Foco semanal",fontWeight=FontWeight.Bold,color=WildforceThemeTokens.textPrimary)
                    workoutDays.sortedBy{it.ordinal}.forEach{day->
                        Text(day.title,Modifier.padding(top=12.dp),color=WildforceThemeTokens.textSecondary)
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                            WorkoutFocus.customSelectionCases.forEach{focus->CompactOption(focus.title,customFocuses[day]==focus){customFocuses=customFocuses+(day to focus)}}
                        }
                    }
                }
            }
            8 -> FormStep(progress=progressFor(8),title="Fase corporal",subtitle="Elige cómo debe influir tu objetivo corporal en el plan.",canContinue=true,onBack={step=7},onContinue={step=9}){
                BodyCompositionPhase.entries.forEach{phase->ChoiceOption(phase.title,phase.description,phase.glyph,bodyPhase==phase){bodyPhase=phase};Spacer(Modifier.height(10.dp))}
            }
            9 -> FormStep(progress=progressFor(9),title="Gimnasio",subtitle="¿En qué entorno vas a entrenar?",canContinue=gymType!=null,onBack={step=previousVisibleStep(9)},onContinue={step=10}){
                GymType.entries.forEach{gym->ChoiceOption(gym.title,null,gym.glyph,gymType==gym){gymType=gym;equipment=gym.defaultEquipment};Spacer(Modifier.height(10.dp))}
            }
            10 -> FormStep(progress=progressFor(10),title="Equipamiento",subtitle="Ajusta el material al que realmente tienes acceso.",canContinue=true,onBack={step=9},onContinue={step=11}){
                OutlinedTextField(value=equipmentSearch,onValueChange={equipmentSearch=it},modifier=Modifier.fillMaxWidth(),placeholder={Text("Buscar equipamiento")},singleLine=true,shape=RoundedCornerShape(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("${equipment.size} seleccionados",Modifier.padding(vertical=12.dp),color=WildforceThemeTokens.textSecondary)
                    Text("Deseleccionar todo", Modifier.clickable { equipment = emptySet() }.padding(8.dp), color = WildforceThemeTokens.textSecondary)
                }
                Equipment.entries.filter{it.title.contains(equipmentSearch.trim(),ignoreCase=true)}.groupBy { it.category }.forEach { (category, categoryItems) ->
                    Text(category, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary, modifier = Modifier.padding(top = 12.dp, bottom = 8.dp))
                    categoryItems.chunked(2).forEach{items->
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){
                        items.forEach { item ->
                            Box(Modifier.weight(1f)) {
                                EquipmentOption(item.title, item in equipment) {
                                    equipment = if (item in equipment) equipment - item else equipment + item
                                }
                            }
                        }
                        if(items.size==1)Spacer(Modifier.weight(1f))
                    };Spacer(Modifier.height(10.dp))
                    }
                }
            }
            11 -> FormStep(progress=progressFor(11),title="Restricciones",subtitle="Indica cualquier lesión o limitación que debamos tener en cuenta.",canContinue=true,onBack={step=10},onContinue={step=12}){
                ChoiceOption("No tengo restricciones","Podrás cambiarlo más adelante.","✓",!hasRestrictions){hasRestrictions=false;restrictions=emptySet()}
                Spacer(Modifier.height(10.dp))
                ChoiceOption("Sí, tengo restricciones",null,"!",hasRestrictions){hasRestrictions=true}
                if(hasRestrictions){
                    Spacer(Modifier.height(18.dp))
                    MovementRestriction.entries.forEach{item->ChoiceOption(item.title,null,"•",item in restrictions){restrictions=if(item in restrictions)restrictions-item else restrictions+item};Spacer(Modifier.height(8.dp))}
                }
            }
            12 -> FormStep(progress=progressFor(12),title="",subtitle="",canContinue=true,onBack={step=11},onContinue={step=nextVisibleStep(12)},showBottomAction=false){
                HealthConnectStep(
                    onConnect = {
                        onRequestHealthConnect { granted, height, weight ->
                            healthConnectEnabled = granted
                            height?.let { heightCm = it; importedHeight = true }
                            weight?.let { weightKg = it; importedWeight = true }
                            if (granted) step = nextVisibleStep(12)
                        }
                    },
                    onNotNow = {
                        healthConnectEnabled = false
                        step = nextVisibleStep(12)
                    },
                )
            }
            13 -> FormStep(progress=progressFor(13),title="Fecha de nacimiento",subtitle="Esto nos ayuda a adaptar volumen, intensidad y recuperación.",canContinue=true,onBack={step=12},onContinue={step=14}){
                ValueStepper("Mes",monthNames[birthMonth-1],{birthMonth=if(birthMonth==1)12 else birthMonth-1},{birthMonth=if(birthMonth==12)1 else birthMonth+1});Spacer(Modifier.height(12.dp))
                ValueStepper("Año",birthYear.toString(),{birthYear=(birthYear-1).coerceAtLeast(1920)},{birthYear=(birthYear+1).coerceAtMost(currentYear)})
            }
            14 -> FormStep(progress=progressFor(14),title="¿Cuál es tu sexo?",subtitle="Se utiliza para ajustar los cálculos físicos del plan.",canContinue=gender!=null,onBack={step=13},onContinue={step=nextVisibleStep(14)}){Gender.entries.forEach{item->ChoiceOption(item.title,null,item.glyph,gender==item){gender=item};Spacer(Modifier.height(10.dp))}}
            15 -> FormStep(progress=progressFor(15),title="Altura",subtitle="Esto nos ayuda a calcular tus necesidades con precisión.",canContinue=true,onBack={step=previousVisibleStep(15)},onContinue={step=nextVisibleStep(15)}){
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){MetricSystem.entries.forEach{system->Box(Modifier.weight(1f)){CompactOption(system.title,metricSystem==system){metricSystem=system}}}}
                Spacer(Modifier.height(20.dp))
                val heightLabel=if(metricSystem==MetricSystem.Metric)"$heightCm cm" else {val inches=(heightCm/2.54).toInt();"${inches/12} ft ${inches%12} in"}
                ValueStepper("Altura",heightLabel,{heightCm=(heightCm-1).coerceAtLeast(120)},{heightCm=(heightCm+1).coerceAtMost(230)})
            }
            16 -> FormStep(progress=progressFor(16),title="Peso",subtitle="Esto nos ayuda a calcular tus necesidades con precisión.",canContinue=true,onBack={step=previousVisibleStep(16)},onContinue={step=18}){
                val weightLabel=if(metricSystem==MetricSystem.Metric)String.format("%.1f kg",weightKg) else String.format("%.1f lb",weightKg*2.20462)
                ValueStepper("Peso",weightLabel,{weightKg=(weightKg-.5).coerceAtLeast(35.0)},{weightKg=(weightKg+.5).coerceAtMost(250.0)})
            }
            else -> PlanGenerationStep(
                preview = generatedPlanPreview,
                generationError = generationError,
                isGenerating = isGenerating,
                generationAttempt = generationAttempt,
                onGenerate = { generationAttempt++ },
                onStartTraining = onStartTraining,
                onInitialGeneration = { completeProfile(true) },
            )
        }
    }
    if (step < 18) {
        Text(
            "◎  $appLanguage",
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).clickable { isLanguagePickerVisible = true }.padding(horizontal = 12.dp, vertical = 9.dp),
            color = if (step == 0) Color.White else WildforceThemeTokens.textPrimary,
            style = MaterialTheme.typography.caption,
        )
    }
    if (isLanguagePickerVisible) {
        val languages = listOf("Español", "English", "Català", "Français", "Italiano", "Português", "Deutsch", "中文（简体）", "Nederlands", "日本語")
        AlertDialog(
            onDismissRequest = { isLanguagePickerVisible = false },
            title = { Text("Idioma") },
            text = { Column { languages.forEach { language -> Text(language, Modifier.fillMaxWidth().clickable { appLanguage = language; onLanguageSelected(language); isLanguagePickerVisible = false }.padding(vertical = 12.dp), color = if (language == appLanguage) WildforceThemeTokens.accentGold else WildforceThemeTokens.textPrimary) } } },
            confirmButton = {},
        )
    }
    }
}

@Composable
private fun CoverStep(onStart: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Image(
            painter = painterResource(R.drawable.onboarding_cover),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f), Color.Black)),
            ),
        )
        Column(
            modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 50.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(R.drawable.logo_white),
                contentDescription = "Wildforce",
                modifier = Modifier.fillMaxWidth().height(82.dp),
                contentScale = ContentScale.Fit,
            )
            Text(
                "Tu camino hacia una versión más fuerte y saludable empieza aquí.",
                color = Color.White.copy(alpha = 0.72f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.body1,
            )
            Spacer(Modifier.height(40.dp))
            PrimaryAction("EMPEZAR", true, onStart)
        }
    }
}

/** Mirrors iOS's standalone plan-generation and preview screen. */
@Composable
private fun PlanGenerationStep(
    preview: OnboardingPlanPreview?,
    generationError: String?,
    isGenerating: Boolean,
    generationAttempt: Int,
    onGenerate: () -> Unit,
    onStartTraining: () -> Unit,
    onInitialGeneration: () -> Unit,
) {
    LaunchedEffect(generationAttempt) { onInitialGeneration() }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when {
            generationError != null -> {
                Spacer(Modifier.height(96.dp))
                Text("!", modifier = Modifier.fillMaxWidth(), color = WildforceThemeTokens.accentGold, style = MaterialTheme.typography.h2, textAlign = TextAlign.Center)
                Text("No hemos podido crear tu plan", modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.h6, color = WildforceThemeTokens.textPrimary, textAlign = TextAlign.Center)
                Text(generationError, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = .10f)).padding(16.dp), color = WildforceThemeTokens.textSecondary, style = MaterialTheme.typography.caption)
                Spacer(Modifier.height(20.dp))
                PrimaryAction("REINTENTAR", !isGenerating, onGenerate)
            }
            preview != null -> {
                PlanWeekSummary(preview)
                preview.workouts.forEach { workout -> PlanWorkoutPreviewCard(workout) }
                Spacer(Modifier.height(12.dp))
                PrimaryAction("EMPEZAR A ENTRENAR", true, onStartTraining)
            }
            else -> {
                Spacer(Modifier.height(156.dp))
                CircularProgressIndicator(color = WildforceThemeTokens.textPrimary, modifier = Modifier.size(56.dp).align(Alignment.CenterHorizontally))
                Text("Creando tu plan", modifier = Modifier.fillMaxWidth().padding(top = 20.dp), style = MaterialTheme.typography.h5, color = WildforceThemeTokens.textPrimary, textAlign = TextAlign.Center)
                Text(if (isGenerating) "Estamos preparando tus entrenamientos." else "Preparando…", modifier = Modifier.fillMaxWidth(), color = WildforceThemeTokens.textSecondary, style = MaterialTheme.typography.body1, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun PlanWeekSummary(preview: OnboardingPlanPreview) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(WildforceThemeTokens.backgroundSecondary).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("ESTA SEMANA", style = MaterialTheme.typography.caption, fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textSecondary)
        Text(preview.name, style = MaterialTheme.typography.h4, fontFamily = AntonFontFamily, color = WildforceThemeTokens.textPrimary)
        Text(preview.phase, style = MaterialTheme.typography.body2, color = WildforceThemeTokens.textSecondary)
        Text("${preview.workouts.size} sesiones", style = MaterialTheme.typography.body2, color = WildforceThemeTokens.textSecondary)
    }
}

@Composable
private fun PlanWorkoutPreviewCard(workout: String) {
    Box(Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(8.dp))) {
        Image(painter = painterResource(R.drawable.onboarding_cover), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .82f)))))
        Text(workout, modifier = Modifier.align(Alignment.BottomStart).padding(20.dp), color = Color.White, style = MaterialTheme.typography.h6)
    }
}

@Composable
private fun FormStep(
    progress: Float,
    title: String,
    subtitle: String,
    canContinue: Boolean,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    buttonTitle: String = "CONTINUAR",
    showBottomAction: Boolean = true,
    showTopBar: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize().liquidGlassBackground()) {
        if (showTopBar) {
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                color = WildforceThemeTokens.accentGold,
            )
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).clickable(onClick = onBack), contentAlignment = Alignment.CenterStart) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", tint = WildforceThemeTokens.textPrimary, modifier = Modifier.size(22.dp))
                }
            }
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            if (title.isNotBlank()) {
                Text(title, style = MaterialTheme.typography.h4, fontFamily = AntonFontFamily, color = WildforceThemeTokens.textPrimary)
                if (subtitle.isNotBlank()) Text(subtitle, color = WildforceThemeTokens.textSecondary, style = MaterialTheme.typography.body1)
                Spacer(Modifier.height(34.dp))
            }
            content()
        }
        if (showBottomAction) Box(Modifier.fillMaxWidth().padding(12.dp).liquidGlass(RoundedCornerShape(18.dp), emphasized = true).padding(16.dp)) {
            PrimaryAction(buttonTitle, canContinue, onContinue)
        }
    }
}

/** Mirrors the dedicated Apple Health screen instead of presenting Health Connect as a form row. */
@Composable
private fun HealthConnectStep(onConnect: () -> Unit, onNotNow: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.height(132.dp),
            horizontalArrangement = Arrangement.spacedBy((-22).dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(96.dp).clip(RoundedCornerShape(22.dp)).background(WildforceThemeTokens.textPrimary),
                contentAlignment = Alignment.Center,
            ) { Text("W", color = WildforceThemeTokens.backgroundSecondary, style = MaterialTheme.typography.h3, fontWeight = FontWeight.Bold) }
            Box(
                Modifier.size(96.dp).clip(RoundedCornerShape(22.dp)).background(Color(0xFFFF375F)),
                contentAlignment = Alignment.Center,
            ) { Text("♥", color = Color.White, style = MaterialTheme.typography.h3) }
        }
        Text("Sincroniza con Health Connect", style = MaterialTheme.typography.h5, fontFamily = AntonFontFamily, color = WildforceThemeTokens.textPrimary, textAlign = TextAlign.Center)
        Text(
            "Importa ahora tus datos corporales y mantén sincronizadas tus futuras métricas.",
            modifier = Modifier.padding(top = 12.dp),
            color = WildforceThemeTokens.textSecondary,
            style = MaterialTheme.typography.body1,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(48.dp))
        PrimaryAction("CONECTAR HEALTH CONNECT", true, onConnect)
        Text(
            "Ahora no",
            modifier = Modifier.padding(top = 18.dp).clickable(onClick = onNotNow),
            color = WildforceThemeTokens.textSecondary,
            style = MaterialTheme.typography.body1,
        )
    }
}

/** Android equivalent of iOS's plain, centered onboarding name field. */
@Composable
private fun NameStepInput(name: String, onNameChange: (String) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(60.dp))
        BasicTextField(
            value = name,
            onValueChange = onNameChange,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(WildforceThemeTokens.textPrimary.copy(alpha = .10f))
                .padding(16.dp),
            textStyle = MaterialTheme.typography.h4.copy(
                fontFamily = AntonFontFamily,
                fontSize = 34.sp,
                color = WildforceThemeTokens.textPrimary,
                textAlign = TextAlign.Center,
            ),
            singleLine = true,
            decorationBox = { input ->
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    if (name.isBlank()) {
                        Text(
                            "Tu nombre",
                            color = WildforceThemeTokens.textSecondary,
                            style = MaterialTheme.typography.h4.copy(fontFamily = AntonFontFamily, fontSize = 34.sp),
                        )
                    }
                    input()
                }
            },
        )
    }
}

@Composable
private fun GoalOption(item: FitnessGoal, selected: Boolean, onClick: () -> Unit) =
    ChoiceOption(item.title, item.description, item.glyph, selected, onClick)

@Composable
private fun ChoiceOption(
    title: String,
    description: String?,
    glyph: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(if (selected) WildforceThemeTokens.textPrimary else Color.White.copy(alpha = 0.10f))
            .clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(glyph, style = MaterialTheme.typography.h5, color = if (selected) WildforceThemeTokens.backgroundSecondary else WildforceThemeTokens.textPrimary)
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(title, fontWeight = FontWeight.Bold, color = if (selected) WildforceThemeTokens.backgroundSecondary else WildforceThemeTokens.textPrimary)
            description?.let {
                Text(it, style = MaterialTheme.typography.caption, color = if (selected) WildforceThemeTokens.backgroundSecondary.copy(alpha = .8f) else WildforceThemeTokens.textSecondary, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
        }
        Text(if (selected) "✓" else "", color = WildforceThemeTokens.backgroundSecondary, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ValueStepper(label:String,value:String,onMinus:()->Unit,onPlus:()->Unit){Column(Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(16.dp)).padding(16.dp)){Text(label,color=WildforceThemeTokens.textSecondary);Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){CenteredOnboardingControl("−",48.dp,onMinus);Text(value,style=MaterialTheme.typography.h5,fontWeight=FontWeight.Bold,color=WildforceThemeTokens.textPrimary);CenteredOnboardingControl("+",48.dp,onPlus)}}}

@Composable
private fun CenteredOnboardingControl(glyph: String, size: Dp, onClick: () -> Unit) {
    Box(
        Modifier.size(size).clip(CircleShape).background(WildforceThemeTokens.textPrimary).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (glyph == "+") Icons.Filled.Add else Icons.Filled.Remove,
            contentDescription = null,
            tint = WildforceThemeTokens.backgroundSecondary,
            modifier = Modifier.size(size * 0.48f),
        )
    }
}

@Composable
private fun CompactOption(title:String,selected:Boolean,onClick:()->Unit){
Text(title,Modifier.clip(RoundedCornerShape(12.dp)).background(if(selected)WildforceThemeTokens.accentGold.copy(alpha=.22f)else Color.White.copy(alpha=.07f)).clickable(onClick=onClick).padding(horizontal=13.dp,vertical=11.dp),color=WildforceThemeTokens.textPrimary,style=MaterialTheme.typography.caption,fontWeight=if(selected)FontWeight.Bold else FontWeight.Normal,maxLines=1)
}

/** The iOS equipment grid uses charcoal selected cards; orange is not part of this state. */
@Composable
private fun EquipmentOption(title: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) WildforceThemeTokens.textPrimary else Color.White.copy(alpha = .10f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Text(
                if (selected) "☑" else "☐",
                color = if (selected) WildforceThemeTokens.backgroundSecondary else WildforceThemeTokens.textSecondary,
                style = MaterialTheme.typography.h6,
            )
        }
        Text(
            title,
            color = if (selected) WildforceThemeTokens.backgroundSecondary else WildforceThemeTokens.textPrimary,
            style = MaterialTheme.typography.caption,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PrimaryAction(title: String, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        elevation = ButtonDefaults.elevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = WildforceThemeTokens.textPrimary,
            contentColor = WildforceThemeTokens.backgroundSecondary,
        ),
    ) { Text(title, fontWeight = FontWeight.Bold) }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingPreview() = WildforceTheme { OnboardingScreen { _, _ -> } }
