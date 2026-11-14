package io.codepassion.doubletriangle.feature.onboarding

import android.widget.NumberPicker
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.AlertDialog
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Icon
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChangeHistory
import androidx.compose.material.icons.filled.ChangeCircle
import androidx.compose.material.icons.filled.Chair
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Male
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.filled.ViewWeek
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import io.codepassion.doubletriangle.core.designsystem.Exo2FontFamily
import io.codepassion.doubletriangle.core.designsystem.PlanGenerationAnimation
import io.codepassion.doubletriangle.core.designsystem.WildforceTheme
import io.codepassion.doubletriangle.core.designsystem.WildforceThemeTokens
import io.codepassion.doubletriangle.core.designsystem.liquidGlass
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    val gymJoinCode: String? = null,
    val gymJoinName: String? = null,
    /** Day is optional for backwards-compatible profiles that only stored month/year. */
    val birthDay: Int = 1,
)

data class GymJoinCodeInfo(val code: String, val gymName: String)

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

    /** iOS derives its weekday-square icon from the selected app language. */
    fun localizedInitial(language: String): String {
        val locale = Locale.forLanguageTag(when (language) {
            "English" -> "en"
            "Català" -> "ca"
            "Français" -> "fr"
            "Italiano" -> "it"
            "Português" -> "pt"
            "Deutsch" -> "de"
            "中文（简体）" -> "zh-Hans"
            "Nederlands" -> "nl"
            "日本語" -> "ja"
            else -> "es"
        })
        return java.time.DayOfWeek.of(ordinal + 1)
            .getDisplayName(TextStyle.FULL, locale)
            .take(1)
            .uppercase(locale)
    }

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
Bodyweight("bodyweight","Peso corporal"),ResistanceBands("resistanceBands","Bandas"),Dumbbells("dumbbells","Mancuernas"),Kettlebells("kettlebells","Kettlebells"),MedicineBall("medicineBall","Balón medicinal"),BattleRopes("battleRopes","Cuerdas"),JumpRope("jumpRope","Comba"),SuspensionTrainer("suspensionTrainer","TRX"),GymnasticRings("gymnasticRings","Anillas"),CableMachine("cableMachine","Poleas"),SmithMachine("smithMachine","Máquina Smith"),LegPressMachine("legPressMachine","Prensa"),ChestPressMachine("chestPressMachine","Press de pecho"),LegCurlMachine("legCurlMachine","Curl femoral"),RearDeltMachine("rearDeltMachine","Deltoide posterior"),SeatedRowMachine("seatedRowMachine","Remo sentado"),GluteKickbackMachine("gluteKickbackMachine","Patada glúteo"),PecDeckMachine("pecDeckMachine","Pec deck"),HipAbductionMachine("hipAbductionMachine","Abductores"),HipAdductionMachine("hipAdductionMachine","Aductores"),HackSquatMachine("hackSquatMachine","Máquina de sentadilla hack"),HipThrustMachine("hipThrustMachine","Máquina de hip thrust"),RowingMachine("rowingMachine","Remo cardio"),Treadmill("treadmill","Cinta"),StationaryBike("stationaryBike","Bicicleta"),Elliptical("elliptical","Elíptica"),StairClimber("stairClimber","Escaladora"),SkiErg("skiErg","Ski erg"),FlatBench("flatBench","Banco plano"),AdjustableBench("adjustableBench","Banco ajustable"),SquatRack("squatRack","Rack"),OlympicBarbell("olympicBarbell","Barra olímpica"),EzBar("ezBar","Barra EZ"),TrapBar("trapBar","Trap bar"),DeadliftPlatform("deadliftPlatform","Plataforma"),PullUpBar("pullUpBar","Dominadas"),DipStation("dipStation","Paralelas"),PlyoBox("plyoBox","Cajón"),BoxingBag("boxingBag","Saco"),LandmineAttachment("landmineAttachment","Landmine");
companion object{fun fromStoredValues(values:Set<String>?)=entries.filterTo(mutableSetOf()){it.storedValue in values.orEmpty()}}}

private val Equipment.category: String
    get() = when (this) {
        Equipment.Bodyweight -> "Peso corporal"
        Equipment.ResistanceBands, Equipment.Dumbbells, Equipment.Kettlebells, Equipment.OlympicBarbell, Equipment.EzBar, Equipment.TrapBar -> "Pesos libres"
        Equipment.CableMachine, Equipment.SmithMachine, Equipment.LegPressMachine, Equipment.ChestPressMachine, Equipment.LegCurlMachine, Equipment.RearDeltMachine, Equipment.SeatedRowMachine, Equipment.GluteKickbackMachine, Equipment.PecDeckMachine, Equipment.HipAbductionMachine, Equipment.HipAdductionMachine, Equipment.HackSquatMachine, Equipment.HipThrustMachine, Equipment.FlatBench, Equipment.AdjustableBench, Equipment.SquatRack, Equipment.DeadliftPlatform, Equipment.PullUpBar, Equipment.DipStation -> "Máquinas y estaciones"
        Equipment.RowingMachine, Equipment.Treadmill, Equipment.StationaryBike, Equipment.Elliptical, Equipment.StairClimber, Equipment.SkiErg, Equipment.JumpRope -> "Cardio"
        else -> "Funcional"
    }

data class TrainingLocationProfile(
    val name: String,
    val equipment: Set<Equipment>,
    val isDefault: Boolean = false,
    val displayAddress: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val photoPath: String? = null,
    val id: String? = null,
    val gymId: String? = null,
)

fun OnboardingProfile.effectiveTrainingLocations(): List<TrainingLocationProfile> = trainingLocations.ifEmpty {
    listOf(
        TrainingLocationProfile(
            gymJoinName?.takeIf(String::isNotBlank) ?: "Mi gimnasio",
            availableEquipment.ifEmpty { setOf(Equipment.Bodyweight) },
            true,
        ),
    )
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

val TrainingLevel.skipsWarmupsByDefault: Boolean
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
    onRequestHealthConnect: ((((Boolean, Int?, Double?, Double?, Double?) -> Unit) -> Unit)) = { result -> result(false, null, null, null, null) },
    onHealthMetricsImported: (Boolean, Double?, Double?) -> Unit = { _, _, _ -> },
    isGenerating: Boolean = false,
    generationError: String? = null,
    generatedPlanPreview: OnboardingPlanPreview? = null,
    onStartTraining: () -> Unit = {},
    onContinueWithoutPlan: () -> Unit = {},
    onLanguageSelected: (String) -> Unit = {},
    onLoggedIn: (AuthResponse) -> Unit = {},
    onRegistered: (AuthResponse, OnboardingProfile) -> Unit = { _, _ -> },
    onCheckGymJoinCode: suspend (String) -> GymJoinCodeInfo? = { null },
    onCompleted: (OnboardingProfile, Boolean) -> Unit,
) {
    val context = LocalContext.current
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
    var appLanguage by remember { mutableStateOf(context.getSharedPreferences("wildforce_profile_details", 0).getString("language", null)?.let(::profileLanguageName) ?: deviceLanguageName()) }
    var isLanguagePickerVisible by remember { mutableStateOf(false) }
    var isGymCodePickerVisible by remember { mutableStateOf(false) }
    var gymCodeInput by remember { mutableStateOf("") }
    val invalidQrMessage = androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.this_qr_is_not_a_gym_join_code_)
    val validationErrorMessage = androidx.compose.ui.res.stringResource(R.string.onboarding_code_validation_error)
    val scannerErrorMessage = androidx.compose.ui.res.stringResource(R.string.onboarding_scanner_error)
    var gymCodeError by remember { mutableStateOf<String?>(null) }
    var gymJoinCode by remember { mutableStateOf<String?>(null) }
    var gymJoinName by remember { mutableStateOf<String?>(null) }
    val gymCodeScope = rememberCoroutineScope()
    var isLoginVisible by remember { mutableStateOf(false) }
    var authenticationRegisters by remember { mutableStateOf(false) }
    var registrationCompletesOnboarding by remember { mutableStateOf(false) }
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
    var birthDay by remember{mutableStateOf(1)};var birthMonth by remember{mutableStateOf(1)};var birthYear by remember{mutableStateOf(1995)};var gender by remember{mutableStateOf<Gender?>(null)};var metricSystem by remember{mutableStateOf(MetricSystem.Metric)};var heightCm by remember{mutableStateOf(175)};var weightKg by remember{mutableStateOf(70.0)}
    fun showsBodyComposition() = goal?.supportsBodyComposition == true && trainingLevel.supportsBodyComposition
    fun visibleSteps() = (1..16).filter { current ->
        when (current) {
            8 -> showsBodyComposition()
            9, 10 -> gymJoinCode == null
            15 -> !importedHeight
            16 -> !importedWeight
            else -> true
        }
    }
    fun nextVisibleStep(after: Int) = visibleSteps().firstOrNull { it > after } ?: 18
    fun previousVisibleStep(before: Int) = visibleSteps().lastOrNull { it < before } ?: 0
    fun advanceToNextStep(after: Int) {
        val next = nextVisibleStep(after)
        if (next == 18) {
            authenticationRegisters = true
            registrationCompletesOnboarding = true
            isLoginVisible = true
        } else {
            step = next
        }
    }
    fun progressFor(current: Int): Float = ((visibleSteps().indexOf(current) + 1).coerceAtLeast(0).toFloat() / visibleSteps().size.coerceAtLeast(1))
    val completeProfile: (Boolean) -> Unit = { useAi ->
        val selectedGoal = goal
        val selectedLifestyle = lifestyle
        if (selectedGoal != null && selectedLifestyle != null) {
            val selectedBodyPhase = bodyPhase.takeIf { selectedGoal.supportsBodyComposition && trainingLevel.supportsBodyComposition }
            onCompleted(OnboardingProfile(name.trim(), selectedGoal, selectedLifestyle, workoutDays, duration, trainingLevel, trainingSplit, selectedBodyPhase, customFocuses.filterKeys { it in workoutDays }, gymType ?: GymType.SmallGym, equipment, restrictions, healthConnectEnabled == true, birthMonth, birthYear, gender ?: Gender.Male, metricSystem, heightCm, weightKg, skipsWarmups = trainingLevel.skipsWarmupsByDefault, skipsCooldowns = true, appLanguage = appLanguage, gymJoinCode = gymJoinCode, gymJoinName = gymJoinName, birthDay = birthDay), useAi)
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
            0 -> CoverStep(onStart = {
                gymJoinCode = null
                gymJoinName = null
                step = 1
            }, onLogin = {
                authenticationRegisters = false
                registrationCompletesOnboarding = false
                isLoginVisible = true
            }, onJoinGym = { isGymCodePickerVisible = true })
            1 -> FormStep(
                progress = progressFor(1),
                title = androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.welcome),
                subtitle = androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.what_s_the_name_that_we_should_call_you_),
                canContinue = name.isNotBlank(),
                onBack = { step = 0 },
                onContinue = { step = 2 },
            ) {
                gymJoinName?.let { name ->
                    Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_3936d9755e257277, name), color = WildforceThemeTokens.accent, fontWeight = FontWeight.SemiBold)
                }
                NameStepInput(name = name, onNameChange = { name = it })
            }
            2 -> FormStep(
                progress = progressFor(2),
                title = androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.your_goals),
                subtitle = androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.choose_the_training_objective_that_best_fits_you_),
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
                title = androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.lifestyle),
                subtitle = androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.tell_us_about_your_daily_activity_level_),
                canContinue = lifestyle != null,
                onBack = { step = 2 },
                onContinue = { step = 4 },
            ) {
                LifestyleLevel.entries.forEach { item ->
                    ChoiceOption(onboardingLabel("LifestyleLevel", item.storedValue, item.title), onboardingLabel("LifestyleLevel", item.storedValue, item.description, true), item.glyph, lifestyle == item, { lifestyle = item }, onboardingLifestyleIcon(item))
                    Spacer(Modifier.height(10.dp))
                }
            }
            4 -> FormStep(
                progress = progressFor(4),
                title = androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.availability),
                subtitle = androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.select_when_you_want_to_train_each_week_),
                canContinue = workoutDays.isNotEmpty(),
                onBack = { step = 3 },
                onContinue = { step = 5 },
            ) {
                WorkoutWeekday.entries.forEach { day ->
                    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
                    val dayName = java.time.DayOfWeek.of(day.ordinal + 1).getDisplayName(TextStyle.FULL, locale).replaceFirstChar { it.titlecase(locale) }
                    ChoiceOption(dayName, null, day.localizedInitial(appLanguage), day in workoutDays, onClick = {
                        workoutDays = if (day in workoutDays && workoutDays.size > 1) workoutDays - day else workoutDays + day
                    }, isWeekday = true)
                    Spacer(Modifier.height(10.dp))
                }
            }
            5 -> FormStep(
                progress = progressFor(5),
                title = androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.workout_duration),
                subtitle = androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.define_your_preferred_workout_structure_and_duration_),
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
                    Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string._d_min, duration), style = MaterialTheme.typography.h5, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
                    CenteredOnboardingControl("+", 48.dp) { duration = (duration + 5).coerceAtMost(180) }
                }
            }
            6 -> FormStep(
                progress = progressFor(6),
                title = androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.experience),
                subtitle = androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.tell_us_about_your_current_fitness_experience_),
                canContinue = true,
                onBack = { step = 5 },
                onContinue = { step = 7 },
            ) {
                TrainingLevel.entries.forEach { level ->
                    ChoiceOption(onboardingLabel("TrainingLevel", level.storedValue, level.title), onboardingLabel("TrainingLevel", level.storedValue, level.description, true), level.glyph, trainingLevel == level, { trainingLevel = level }, onboardingTrainingLevelIcon(level))
                    Spacer(Modifier.height(10.dp))
                }
            }
            7 -> FormStep(
                progress=progressFor(7),title=androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.program),subtitle=androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.choose_the_workout_structure_that_feels_right_for_you_),
                canContinue=trainingSplit!=TrainingSplitPreference.Custom||workoutDays.all{it in customFocuses},
                onBack={step=6},onContinue={step=nextVisibleStep(7)},
            ){
                trainingSplit.warning(workoutDays.size)?.let{Text(androidx.compose.ui.res.stringResource(R.string.onboarding_split_warning),Modifier.fillMaxWidth().background(Color(0xFFFFB020).copy(alpha=.14f),RoundedCornerShape(12.dp)).padding(12.dp));Spacer(Modifier.height(12.dp))}
                TrainingSplitPreference.entries.forEach{split->ChoiceOption(onboardingLabel("TrainingSplitPreference",split.storedValue,split.title),onboardingSplitDescription(split),split.glyph,trainingSplit==split, {
                    val previousSplit = trainingSplit
                    trainingSplit=split
                    if(split==TrainingSplitPreference.Custom)customFocuses=normalizedCustomFocuses(customFocuses, workoutDays, previousSplit)
                }, onboardingTrainingSplitIcon(split));Spacer(Modifier.height(10.dp))}
                if(trainingSplit==TrainingSplitPreference.Custom){
                    Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_82da898217b2b8b7),fontWeight=FontWeight.Bold,color=WildforceThemeTokens.textPrimary)
                    workoutDays.sortedBy{it.ordinal}.forEach{day->
                        val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
                        val dayName = java.time.DayOfWeek.of(day.ordinal + 1).getDisplayName(TextStyle.FULL, locale).replaceFirstChar { it.titlecase(locale) }
                        Text(dayName,Modifier.padding(top=12.dp),color=WildforceThemeTokens.textSecondary)
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                            WorkoutFocus.customSelectionCases.forEach{focus->CompactOption(onboardingLabel("WorkoutFocus",focus.storedValue,focus.title),customFocuses[day]==focus){customFocuses=customFocuses+(day to focus)}}
                        }
                    }
                }
            }
            8 -> FormStep(progress=progressFor(8),title=androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.body_composition),subtitle=androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.choose_how_your_current_body_composition_focus_should_shape_your_training_plan_),canContinue=true,onBack={step=previousVisibleStep(8)},onContinue={step=nextVisibleStep(8)}){
                BodyCompositionPhase.entries.forEach{phase->ChoiceOption(onboardingLabel("BodyCompositionPhase",phase.storedValue,phase.title),onboardingLabel("BodyCompositionPhase",phase.storedValue,phase.description,true),phase.glyph,bodyPhase==phase,{bodyPhase=phase}, onboardingBodyPhaseIcon(phase));Spacer(Modifier.height(10.dp))}
            }
            9 -> FormStep(progress=progressFor(9),title=androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.gym),subtitle=androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.what_kind_of_environment_will_you_be_training_in_),canContinue=gymType!=null,onBack={step=previousVisibleStep(9)},onContinue={step=nextVisibleStep(9)}){
                GymType.entries.forEach{gym->ChoiceOption(onboardingLabel("GymType",gym.storedValue,gym.title),null,gym.glyph,gymType==gym,{gymType=gym;equipment=gym.defaultEquipment}, onboardingGymTypeIcon(gym));Spacer(Modifier.height(10.dp))}
            }
            10 -> {
                val equipmentNames = Equipment.entries.associateWith { onboardingLabel("Equipment", it.storedValue, it.title) }
                FormStep(progress=progressFor(10),title=androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.equipment),subtitle=androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.fine_tune_the_equipment_you_actually_have_access_to_),canContinue=true,onBack={step=previousVisibleStep(10)},onContinue={step=nextVisibleStep(10)}, lazyContent = {
                item {
                OutlinedTextField(value=equipmentSearch,onValueChange={equipmentSearch=it},modifier=Modifier.fillMaxWidth(),placeholder={Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.search_equipment))},singleLine=true,shape=RoundedCornerShape(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string._d_selected, equipment.size),Modifier.padding(vertical=12.dp),color=WildforceThemeTokens.textSecondary)
                    Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.unselect_all), Modifier.heightIn(min = 48.dp).clickable { equipment = emptySet() }.padding(horizontal = 8.dp, vertical = 8.dp), color = WildforceThemeTokens.textSecondary)
                }
                }
                Equipment.entries.filter{equipmentNames.getValue(it).contains(equipmentSearch.trim(),ignoreCase=true)}.groupBy { it.category }.forEach { (category, categoryItems) ->
                    item(key = "category-$category") {
                    val categoryKey = when (category) {
                        "Peso corporal" -> "bodyweight"
                        "Pesos libres" -> "freeWeights"
                        "Máquinas y estaciones" -> "machines"
                        "Cardio" -> "cardio"
                        else -> "functional"
                    }
                    Text(onboardingLabel("EquipmentCategory", categoryKey, category), fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary, modifier = Modifier.padding(top = 12.dp, bottom = 8.dp))
                    }
                    categoryItems.chunked(2).forEach{items->
                    item(key = "equipment-${items.first().storedValue}") {
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){
                        items.forEach { item ->
                            Box(Modifier.weight(1f)) {
                                EquipmentOption(item, item in equipment) {
                                    equipment = if (item in equipment) equipment - item else equipment + item
                                }
                            }
                        }
                        if(items.size==1)Spacer(Modifier.weight(1f))
                    };Spacer(Modifier.height(10.dp))
                    }
                    }
                }
                })
            }
            11 -> FormStep(progress=progressFor(11),title=androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.restrictions),subtitle=androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.do_you_have_any_injury_or_mobility_restriction_),canContinue=true,onBack={step=previousVisibleStep(11)},onContinue={step=12}){
                ChoiceOption(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.no),androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.you_can_always_update_this_in_the_future_),"✓",!hasRestrictions, onClick = {hasRestrictions=false;restrictions=emptySet()})
                Spacer(Modifier.height(10.dp))
                ChoiceOption(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.yes),null,"!",hasRestrictions, onClick = {hasRestrictions=true})
                if(hasRestrictions){
                    Spacer(Modifier.height(18.dp))
                    MovementRestriction.entries.forEach{item->ChoiceOption(onboardingLabel("MovementRestriction",item.storedValue,item.title),null,"•",item in restrictions, onClick = {restrictions=if(item in restrictions)restrictions-item else restrictions+item});Spacer(Modifier.height(8.dp))}
                }
            }
            12 -> FormStep(progress=progressFor(12),title="",subtitle="",canContinue=true,onBack={step=11},onContinue={step=nextVisibleStep(12)},showBottomAction=false){
                HealthConnectStep(
                    onConnect = {
                    onRequestHealthConnect { granted, height, weight, leanBodyMass, bodyFat ->
                        onHealthMetricsImported(granted, leanBodyMass, bodyFat)
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
            13 -> FormStep(progress=progressFor(13),title=androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.birth_date),subtitle=androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.this_helps_the_ai_tailor_the_volume_and_intensity_of_your_plans_),canContinue=true,onBack={step=previousVisibleStep(13)},onContinue={advanceToNextStep(13)}){
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    WheelPicker(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.day), (1..31).toList(), birthDay, { it.toString() }, Modifier.weight(.7f)) { birthDay = it }
                    WheelPicker(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.month), (1..12).toList(), birthMonth, { monthNames[it - 1] }, Modifier.weight(1.4f)) { birthMonth = it }
                    WheelPicker(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.year), (1920..currentYear).toList().reversed(), birthYear, { it.toString() }, Modifier.weight(1f)) { birthYear = it }
                }
            }
            14 -> FormStep(progress=progressFor(14),title=androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.what_s_your_sex_),subtitle=androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.the_formula_to_get_better_results_differs_based_on_sex__we_need_this_information_to_serve_you_better_),canContinue=gender!=null,onBack={step=previousVisibleStep(14)},onContinue={advanceToNextStep(14)}){Gender.entries.forEach{item->ChoiceOption(onboardingLabel("Gender",item.storedValue,item.title),null,item.glyph,gender==item, onClick = {gender=item});Spacer(Modifier.height(10.dp))}}
            15 -> FormStep(progress=progressFor(15),title=androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.height),subtitle=androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.this_helps_us_calculate_your_needs_accurately_),canContinue=true,onBack={step=previousVisibleStep(15)},onContinue={advanceToNextStep(15)}){
                MetricSystemSegmentedControl(metricSystem) { metricSystem = it }
                Spacer(Modifier.height(20.dp))
                val displayedHeight = if (metricSystem == MetricSystem.Metric) heightCm else (heightCm / 2.54).toInt()
                WheelPicker(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.height), if (metricSystem == MetricSystem.Metric) (120..230).toList() else (48..91).toList(), displayedHeight, {
                    if (metricSystem == MetricSystem.Metric) "$it cm" else "${it / 12} ft ${it % 12} in"
                }, Modifier.fillMaxWidth()) { selected -> heightCm = if (metricSystem == MetricSystem.Metric) selected else (selected * 2.54).toInt() }
            }
            16 -> FormStep(progress=progressFor(16),title=androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.weight),subtitle=androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.this_helps_us_calculate_your_needs_accurately_),canContinue=true,onBack={step=previousVisibleStep(16)},onContinue={
                authenticationRegisters = true
                registrationCompletesOnboarding = true
                isLoginVisible = true
            }){
                val displayedWeight = if (metricSystem == MetricSystem.Metric) weightKg else weightKg * 2.20462
                WeightWheelPicker(displayedWeight, metricSystem) { selected -> weightKg = if (metricSystem == MetricSystem.Metric) selected else selected / 2.20462 }
            }
            else -> PlanGenerationStep(
                preview = generatedPlanPreview,
                generationError = generationError,
                isGenerating = isGenerating,
                generationAttempt = generationAttempt,
                onGenerate = { generationAttempt++ },
                onStartTraining = onStartTraining,
                onContinueWithoutPlan = onContinueWithoutPlan,
                onInitialGeneration = { completeProfile(true) },
            )
        }
    }
    if (step < 18) {
        Text(
            "◎  ${localizedLanguageName(appLanguage)}",
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).heightIn(min = 48.dp).clickable { isLanguagePickerVisible = true }.padding(horizontal = 12.dp, vertical = 9.dp),
            color = if (step == 0) Color.White else WildforceThemeTokens.textPrimary,
            style = MaterialTheme.typography.caption,
        )
    }
    if (isLanguagePickerVisible) {
        val languages = listOf("Español", "English", "Català", "Français", "Italiano", "Português", "Deutsch", "中文（简体）", "Nederlands", "日本語")
        AlertDialog(
            onDismissRequest = { isLanguagePickerVisible = false },
            title = { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.language)) },
            text = { Column { languages.forEach { language -> Text(localizedLanguageName(language), Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { appLanguage = language; onLanguageSelected(language); isLanguagePickerVisible = false }.padding(vertical = 12.dp), color = if (language == appLanguage) WildforceThemeTokens.accent else WildforceThemeTokens.textPrimary) } } },
            confirmButton = {},
        )
    }
    if (isGymCodePickerVisible) {
        var checking by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { if (!checking) isGymCodePickerVisible = false },
            title = { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_526484234b492b02)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.enter_the_code_your_gym_gave_you__or_scan_its_qr_))
                    OutlinedTextField(
                        value = gymCodeInput,
                        onValueChange = { gymCodeInput = it; gymCodeError = null },
                        label = { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.gym_code)) },
                        singleLine = true,
                        enabled = !checking,
                        trailingIcon = {
                            Box(
                                Modifier.size(48.dp).clickable(enabled = !checking) {
                                    runCatching {
                                        val options = GmsBarcodeScannerOptions.Builder()
                                            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                                            .enableAutoZoom()
                                            .build()
                                        GmsBarcodeScanning.getClient(context, options).startScan()
                                        .addOnSuccessListener { barcode ->
                                            barcode.rawValue?.let { value ->
                                                val parsed = parseGymJoinCode(value)
                                                if (parsed == null) gymCodeError = invalidQrMessage
                                                else {
                                                    gymCodeInput = parsed
                                                    gymCodeError = null
                                                    checking = true
                                                    gymCodeScope.launch {
                                                        val result = runCatching { onCheckGymJoinCode(parsed) }.getOrNull()
                                                        if (result != null) {
                                                            gymJoinCode = result.code
                                                            gymJoinName = result.gymName
                                                            gymType = GymType.BigGym
                                                            equipment = GymType.BigGym.defaultEquipment
                                                            isGymCodePickerVisible = false
                                                            step = 1
                                                        } else gymCodeError = validationErrorMessage
                                                        checking = false
                                                    }
                                                }
                                            }
                                        }
                                        .addOnFailureListener { gymCodeError = scannerErrorMessage }
                                    }.onFailure { gymCodeError = scannerErrorMessage }
                                },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Filled.QrCodeScanner, contentDescription = androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.scan_the_gym_s_qr), modifier = Modifier.size(24.dp))
                            }
                        },
                    )
                    gymCodeError?.let { Text(it, color = Color(0xFFE26D5A)) }
                }
            },
            confirmButton = {
                TextButton(enabled = parseGymJoinCode(gymCodeInput) != null && !checking, onClick = {
                    checking = true
                    gymCodeScope.launch {
                        val result = runCatching { onCheckGymJoinCode(requireNotNull(parseGymJoinCode(gymCodeInput))) }.getOrNull()
                        if (result != null) {
                            gymJoinCode = result.code
                            gymJoinName = result.gymName
                            // A gym membership supplies the training location and
                            // equipment in iOS, so the gym questions are skipped
                            // without falling back to the small-gym defaults.
                            gymType = GymType.BigGym
                            equipment = GymType.BigGym.defaultEquipment
                            gymCodeError = null
                            isGymCodePickerVisible = false
                            step = 1
                        } else gymCodeError = validationErrorMessage
                        checking = false
                    }
                }) { Text(if (checking) androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_f12a8ada4dfd091e) else androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.key_continue).uppercase()) }
            },
            dismissButton = { TextButton(enabled = !checking, onClick = { isGymCodePickerVisible = false }) { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.cancel).uppercase()) } },
        )
    }
    if (isLoginVisible) {
        val authenticationProfile = OnboardingProfile(
            name = name.trim(),
            goal = goal ?: FitnessGoal.GeneralFitness,
            lifestyle = lifestyle ?: LifestyleLevel.ModeratelyActive,
            workoutDays = workoutDays,
            preferredWorkoutDurationMinutes = duration,
            trainingLevel = trainingLevel,
            trainingSplitPreference = trainingSplit,
            bodyCompositionPhase = bodyPhase.takeIf { showsBodyComposition() },
            customWorkoutFocuses = customFocuses.filterKeys { it in workoutDays },
            gymType = gymType ?: GymType.SmallGym,
            availableEquipment = equipment,
            movementRestrictions = restrictions,
            isHealthConnectEnabled = healthConnectEnabled == true,
            birthDay = birthDay,
            birthMonth = birthMonth,
            birthYear = birthYear,
            gender = gender ?: Gender.Male,
            metricSystem = metricSystem,
            heightCm = heightCm,
            weightKg = weightKg,
            appLanguage = appLanguage,
            gymJoinCode = gymJoinCode,
            gymJoinName = gymJoinName,
        )
        AuthenticationSheet(
            register = authenticationRegisters,
            profile = authenticationProfile,
            onDismiss = { isLoginVisible = false },
            onSwitch = { authenticationRegisters = !authenticationRegisters },
            onAuthenticated = { response ->
                isLoginVisible = false
                if (authenticationRegisters && registrationCompletesOnboarding) {
                    onRegistered(response, authenticationProfile)
                    if (authenticationProfile.gymJoinCode != null) {
                        // iOS members enter the app after registration and
                        // membership join; they do not enter the AI-plan step.
                        onCompleted(authenticationProfile, false)
                    } else {
                        step = 18
                    }
                } else {
                    onLoggedIn(response)
                }
            },
        )
    }
    }
}

@Composable
private fun CoverStep(onStart: () -> Unit, onLogin: () -> Unit, onJoinGym: () -> Unit) {
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    // The iOS cover reveals each element separately instead of fading the whole
    // column at once. Keeping these timelines independent makes the Android
    // entrance feel equally paced and keeps the button from competing with the logo.
    val logoAlpha by animateFloatAsState(if (appeared) 1f else 0f, tween(800, delayMillis = 100), label = "cover-logo-alpha")
    val logoOffset by animateFloatAsState(if (appeared) 0f else 30f, tween(800, delayMillis = 100), label = "cover-logo-offset")
    val logoBlur by animateFloatAsState(if (appeared) 0f else 20f, tween(800, delayMillis = 100), label = "cover-logo-blur")
    val copyAlpha by animateFloatAsState(if (appeared) 1f else 0f, tween(900, delayMillis = 300), label = "cover-copy-alpha")
    val copyOffset by animateFloatAsState(if (appeared) 0f else 30f, tween(900, delayMillis = 300), label = "cover-copy-offset")
    val actionAlpha by animateFloatAsState(if (appeared) 1f else 0f, tween(800, delayMillis = 600), label = "cover-action-alpha")
    val actionOffset by animateFloatAsState(if (appeared) 0f else 10f, tween(800, delayMillis = 600), label = "cover-action-offset")
    val actionScale by animateFloatAsState(if (appeared) 1f else .8f, tween(800, delayMillis = 600), label = "cover-action-scale")
    val dust = rememberInfiniteTransition(label = "cover-dust")
    val dustTime by dust.animateFloat(
        initialValue = 0f,
        // DustParticlesEffectView on iOS uses the wall-clock time and moves at
        // 6–20 px/s.  Use a long cycle here so the particles don't visibly jump
        // back to their initial positions every few seconds.
        targetValue = 1_800f,
        animationSpec = infiniteRepeatable(tween(1_800_000, easing = LinearEasing)),
        label = "cover-dust-time",
    )
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Image(
            painter = painterResource(R.drawable.onboarding_cover),
            contentDescription = null,
            // This is the same -160pt framing used by OnboardingCoverStepView
            // on iOS. It keeps the warm bokeh in the left half of the image
            // around the logo instead of hiding it below the fold.
            modifier = Modifier.fillMaxSize().offset(y = (-160).dp),
            contentScale = ContentScale.Crop,
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color.Transparent, Color.Black, Color.Black)),
            ),
        )
        // Keep the subtle dust behind the whole cover, including the actions,
        // so the lower black area feels connected to the photo above it.
        // The cover photo still supplies the larger soft bokeh.
        Box(
            modifier = Modifier
                .fillMaxSize(),
        ) {
            FloatingDustOverlay(timeSeconds = dustTime)
        }
        Column(
            modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 50.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(R.drawable.logo_white),
                contentDescription = "Wildforce",
                modifier = Modifier.fillMaxWidth().height(82.dp).alpha(logoAlpha).offset(y = logoOffset.dp).blur(logoBlur.dp),
                contentScale = ContentScale.Fit,
            )
            Text(
                androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.your_journey_to_a_stronger__healthier_you_starts_here_),
                // The cover is always dark in iOS, independent of the app theme.
                color = Color(0xFFA7A6AE),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.body1.copy(fontSize = 18.sp, letterSpacing = 4.sp),
                modifier = Modifier.alpha(copyAlpha).offset(y = copyOffset.dp),
            )
            Spacer(Modifier.height(40.dp))
            Box(Modifier.alpha(actionAlpha).offset(y = actionOffset.dp).graphicsLayer { scaleX = actionScale; scaleY = actionScale }) {
                PrimaryAction(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.get_started), true, onStart)
            }
            Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_526484234b492b02), Modifier.padding(top = 18.dp).heightIn(min = 48.dp).clickable(onClick = onJoinGym).padding(8.dp), color = Color.White, style = MaterialTheme.typography.caption, fontWeight = FontWeight.SemiBold)
            Text(
                androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.i_already_have_an_account),
                modifier = Modifier.padding(top = 10.dp).heightIn(min = 48.dp).clickable(onClick = onLogin).padding(8.dp),
                color = Color.White.copy(alpha = 0.80f),
                style = MaterialTheme.typography.caption,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun FloatingDustOverlay(timeSeconds: Float) {
    Canvas(Modifier.fillMaxSize()) {
        repeat(48) { seed ->
            val seedValue = seed.toFloat()
            val initialX = ((kotlin.math.sin(seedValue * 12.9898f) + 1f) / 2f) * size.width
            val initialY = ((kotlin.math.cos(seedValue * 78.233f) + 1f) / 2f) * size.height
            // Keep the same soft dust language as iOS, but express the radius
            // in dp. The old raw-pixel values were sub-pixel on this emulator,
            // so the animation existed but was effectively invisible.
            val radius = (1.25f + kotlin.math.abs(kotlin.math.sin(seedValue * 3.7f)) * 2.05f).dp.toPx()
            val baseAlpha = 0.32f + kotlin.math.abs(kotlin.math.cos(seedValue * 5.1f)) * 0.36f
            val riseSpeed = 8f + kotlin.math.abs(kotlin.math.sin(seedValue * 2.9f)) * 18f
            val wrapHeight = size.height + 24.dp.toPx()
            val yRaw = initialY - (timeSeconds * riseSpeed % wrapHeight) - 12.dp.toPx()
            val y = if (yRaw < -12.dp.toPx()) yRaw + wrapHeight else yRaw
            // In iOS the wobble and twinkle coefficients are deliberately zero,
            // leaving a calm, slow dust-in-light treatment instead of a glitter.
            val center = androidx.compose.ui.geometry.Offset(initialX, y)
            drawCircle(Color.White.copy(alpha = baseAlpha * .08f), radius = radius * 1.9f, center = center)
            drawCircle(Color.White.copy(alpha = baseAlpha * .78f), radius = radius, center = center)
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
    onContinueWithoutPlan: () -> Unit,
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
                Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_8535879c75ac3c86), modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.h6, color = WildforceThemeTokens.textPrimary, textAlign = TextAlign.Center)
                Text(generationError, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(WildforceThemeTokens.textSecondary.copy(alpha = .10f)).padding(16.dp), color = WildforceThemeTokens.textSecondary, style = MaterialTheme.typography.caption)
                Spacer(Modifier.height(20.dp))
                PrimaryAction(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.retry), !isGenerating, onGenerate)
                TextButton(
                    onClick = onContinueWithoutPlan,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.continue_without_a_plan)) }
            }
            preview != null -> {
                PlanWeekSummary(preview)
                preview.workouts.forEach { workout -> PlanWorkoutPreviewCard(workout) }
                Spacer(Modifier.height(12.dp))
                PrimaryAction(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.start_training), true, onStartTraining)
            }
            else -> {
                Spacer(Modifier.height(110.dp))
                PlanCreationAnimation()
                Text(if (isGenerating) androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_535139f051f45a12) else androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_d2b925965ea8d2b9), modifier = Modifier.fillMaxWidth().padding(top = 20.dp), color = WildforceThemeTokens.textSecondary, style = MaterialTheme.typography.body1, textAlign = TextAlign.Center)
            }
        }
    }
}

/** Android equivalent of iOS CreatePlanAnimationView and its notification stack. */
@Composable
private fun PlanCreationAnimation() {
    PlanGenerationAnimation(icon = "⌁")
}

@Composable
private fun PlanWeekSummary(preview: OnboardingPlanPreview) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(WildforceThemeTokens.backgroundSecondary).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.this_week).uppercase(), style = MaterialTheme.typography.caption, fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textSecondary)
        Text(preview.name, style = MaterialTheme.typography.h4, fontFamily = Exo2FontFamily, fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textPrimary)
        Text(preview.phase, style = MaterialTheme.typography.body2, color = WildforceThemeTokens.textSecondary)
        Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string._d_sessions, preview.workouts.size), style = MaterialTheme.typography.body2, color = WildforceThemeTokens.textSecondary)
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
    buttonTitle: String? = null,
    showBottomAction: Boolean = true,
    showTopBar: Boolean = true,
    lazyContent: (LazyListScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    // iOS uses the plain semantic background here; frosted treatment is
    // reserved for modal surfaces, not the onboarding canvas.
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = spring(dampingRatio = .7f, stiffness = 520f),
        label = "onboarding-step-progress",
    )
    Column(Modifier.fillMaxSize().background(WildforceThemeTokens.background).navigationBarsPadding()) {
        if (showTopBar) {
            LinearProgressIndicator(
                progress = animatedProgress,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                // SwiftUI's linear ProgressView follows the app accent, not the
                // warm workout-only emphasis color.
                color = WildforceThemeTokens.accent,
            )
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).clickable(onClick = onBack), contentAlignment = Alignment.CenterStart) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.action_back), tint = WildforceThemeTokens.textPrimary, modifier = Modifier.size(22.dp))
                }
            }
        }
        if (lazyContent != null) {
            LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp)) {
                item {
                    if (title.isNotBlank()) {
                        Text(title, style = MaterialTheme.typography.h4.copy(fontSize = 40.sp), fontFamily = Exo2FontFamily, fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textPrimary)
                        if (subtitle.isNotBlank()) Text(subtitle, color = WildforceThemeTokens.textSecondary, style = MaterialTheme.typography.body1)
                        Spacer(Modifier.height(34.dp))
                    }
                }
                lazyContent()
            }
        } else Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            if (title.isNotBlank()) {
                Text(title, style = MaterialTheme.typography.h4.copy(fontSize = 40.sp), fontFamily = Exo2FontFamily, fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textPrimary)
                if (subtitle.isNotBlank()) Text(subtitle, color = WildforceThemeTokens.textSecondary, style = MaterialTheme.typography.body1)
                Spacer(Modifier.height(34.dp))
            }
            content()
        }
        if (showBottomAction) {
            Divider(color = WildforceThemeTokens.textSecondary.copy(alpha = 0.16f))
            Box(Modifier.fillMaxWidth().padding(24.dp)) {
                PrimaryAction(buttonTitle ?: androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.key_continue), canContinue, onContinue)
            }
        }
    }
}

/** Mirrors the dedicated Apple Health screen instead of presenting Health Connect as a form row. */
@Composable
private fun HealthConnectStep(onConnect: () -> Unit, onNotNow: () -> Unit) {
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(180); appeared = true }
    val appOffset by animateFloatAsState(if (appeared) 50f else 0f, animationSpec = tween(durationMillis = 700, delayMillis = 180), label = "health-app-offset")
    val healthOffset by animateFloatAsState(if (appeared) -50f else 0f, animationSpec = tween(durationMillis = 700, delayMillis = 180), label = "health-connect-offset")
    val appRotation by animateFloatAsState(if (appeared) 12f else 0f, animationSpec = tween(durationMillis = 700, delayMillis = 180), label = "health-app-rotation")
    val healthRotation by animateFloatAsState(if (appeared) -12f else 0f, animationSpec = tween(durationMillis = 700, delayMillis = 180), label = "health-connect-rotation")
    val contentAlpha by animateFloatAsState(if (appeared) 1f else 0f, animationSpec = tween(durationMillis = 450, delayMillis = 300), label = "health-content-alpha")
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
                Modifier.size(96.dp).graphicsLayer { translationX = appOffset; rotationZ = appRotation }.clip(RoundedCornerShape(22.dp)).background(WildforceThemeTokens.textPrimary),
                contentAlignment = Alignment.Center,
            ) { Image(painterResource(R.drawable.wildforce_app_icon), contentDescription = "Wildforce", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
            Box(
                Modifier.size(96.dp).graphicsLayer { translationX = healthOffset; rotationZ = healthRotation }.clip(RoundedCornerShape(22.dp)).background(Color(0xFFFF375F)),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.Favorite, contentDescription = null, tint = Color.White, modifier = Modifier.size(42.dp)) }
        }
        Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_f6f8978612a50da6), modifier = Modifier.alpha(contentAlpha), style = MaterialTheme.typography.h5, fontFamily = Exo2FontFamily, fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textPrimary, textAlign = TextAlign.Center)
        Text(
            androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.import_your_details_from_apple_health_now_and_keep_future_metrics_in_sync_).replace("Apple Health", "Health Connect"),
            modifier = Modifier.padding(top = 12.dp).alpha(contentAlpha),
            color = WildforceThemeTokens.textSecondary,
            style = MaterialTheme.typography.body1,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(48.dp))
        Box(Modifier.alpha(contentAlpha)) { PrimaryAction(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_79de068350f9e051), true, onConnect) }
        Text(
            androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.not_now),
            modifier = Modifier.padding(top = 18.dp).alpha(contentAlpha).clickable(onClick = onNotNow),
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
                fontFamily = Exo2FontFamily,
                fontSize = 34.sp,
                color = WildforceThemeTokens.textPrimary,
                textAlign = TextAlign.Center,
            ),
            singleLine = true,
            decorationBox = { input ->
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    if (name.isBlank()) {
                        Text(
                            androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.your_name),
                            color = WildforceThemeTokens.textSecondary,
                            style = MaterialTheme.typography.h4.copy(fontFamily = Exo2FontFamily, fontSize = 34.sp, fontWeight = FontWeight.SemiBold),
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
    ChoiceOption(onboardingLabel("FitnessGoal", item.storedValue, item.title), onboardingLabel("FitnessGoal", item.storedValue, item.description, true), item.glyph, selected, onClick, onboardingGoalIcon(item))

@Composable
private fun ChoiceOption(
    title: String,
    description: String?,
    glyph: String,
    selected: Boolean,
    onClick: () -> Unit,
    iconOverride: ImageVector? = null,
    isWeekday: Boolean = false,
) {
    val icon = iconOverride ?: onboardingIcon(glyph)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) .97f else 1f,
        animationSpec = spring(dampingRatio = .7f, stiffness = 700f),
        label = "onboarding-choice-press",
    )
    Row(
        Modifier.fillMaxWidth().graphicsLayer { scaleX = pressScale; scaleY = pressScale }.clip(RoundedCornerShape(16.dp))
            .background(if (selected) WildforceThemeTokens.textPrimary else WildforceThemeTokens.textSecondary.copy(alpha = 0.10f))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp), tint = if (selected) WildforceThemeTokens.backgroundSecondary else WildforceThemeTokens.textPrimary)
        else Text(
            glyph,
            modifier = if (isWeekday) Modifier.clip(RoundedCornerShape(6.dp)).background(if (selected) WildforceThemeTokens.backgroundSecondary.copy(alpha = .18f) else WildforceThemeTokens.textPrimary.copy(alpha = .10f)).padding(horizontal = 6.dp, vertical = 3.dp) else Modifier,
            style = MaterialTheme.typography.h6,
            fontWeight = FontWeight.Bold,
            color = if (selected) WildforceThemeTokens.backgroundSecondary else WildforceThemeTokens.textPrimary,
        )
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(title, fontWeight = FontWeight.Bold, color = if (selected) WildforceThemeTokens.backgroundSecondary else WildforceThemeTokens.textPrimary)
            description?.let {
                Text(it, style = MaterialTheme.typography.caption, color = if (selected) WildforceThemeTokens.backgroundSecondary.copy(alpha = .8f) else WildforceThemeTokens.textSecondary, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
        }
        Box(Modifier.width(24.dp), contentAlignment = Alignment.Center) {
            androidx.compose.animation.AnimatedVisibility(
                visible = selected,
                enter = fadeIn(tween(140)) + scaleIn(spring(dampingRatio = .58f, stiffness = 520f), initialScale = .65f),
            ) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(21.dp), tint = WildforceThemeTokens.backgroundSecondary)
            }
        }
    }
}

/** Monochrome Android counterparts for the SF Symbols used by the iOS flow. */
private fun onboardingIcon(glyph: String) = when (glyph) {
    "↘" -> Icons.AutoMirrored.Filled.TrendingDown
    "◆", "ϟ" -> Icons.Filled.FitnessCenter
    "∞", "↗" -> Icons.AutoMirrored.Filled.DirectionsRun
    "◎" -> Icons.Filled.AccessibilityNew
    "◐" -> Icons.Filled.ChangeCircle
    "♥" -> Icons.Filled.Favorite
    "□" -> Icons.Filled.Chair
    "│" -> Icons.Filled.Person
    "→" -> Icons.AutoMirrored.Filled.DirectionsWalk
    "▦", "▤" -> Icons.Filled.FitnessCenter
    "⌂" -> Icons.Filled.Home
    "○", "◔", "◑", "◕", "●" -> Icons.Filled.RadioButtonUnchecked
    "✦" -> Icons.Filled.AutoAwesome
    "↕" -> Icons.Filled.UnfoldMore
    "△" -> Icons.Filled.ChangeHistory
    "◇" -> Icons.Filled.Apps
    "☷" -> Icons.Filled.ViewWeek
    "=" -> Icons.Filled.Tune
    "♂" -> Icons.Filled.Male
    "♀" -> Icons.Filled.Female
    "✓" -> Icons.Filled.CheckCircle
    "!", "•" -> Icons.Filled.Warning
    "—" -> Icons.Filled.HorizontalRule
    "↑" -> Icons.AutoMirrored.Filled.TrendingUp
    else -> null
}

private fun onboardingGoalIcon(goal: FitnessGoal): ImageVector = when (goal) {
    FitnessGoal.LoseWeight -> Icons.Filled.Sync
    FitnessGoal.BuildMuscle -> Icons.Filled.FitnessCenter
    FitnessGoal.GainStrength -> Icons.Filled.Bolt
    FitnessGoal.ImproveEndurance -> Icons.AutoMirrored.Filled.DirectionsRun
    FitnessGoal.ImproveMobility -> Icons.Filled.AccessibilityNew
    FitnessGoal.BodyRecomposition -> Icons.Filled.AccessibilityNew
    FitnessGoal.GeneralFitness -> Icons.Filled.Favorite
}

private fun onboardingLifestyleIcon(level: LifestyleLevel): ImageVector = when (level) {
    LifestyleLevel.Sedentary -> Icons.Filled.Chair
    LifestyleLevel.LightlyActive -> Icons.Filled.AccessibilityNew
    LifestyleLevel.ModeratelyActive -> Icons.AutoMirrored.Filled.DirectionsWalk
    LifestyleLevel.VeryActive -> Icons.AutoMirrored.Filled.DirectionsRun
}

private fun onboardingTrainingLevelIcon(level: TrainingLevel): ImageVector = when (level) {
    TrainingLevel.CompleteBeginner -> Icons.Filled.Eco
    TrainingLevel.Beginner -> Icons.AutoMirrored.Filled.DirectionsWalk
    TrainingLevel.Novice -> Icons.AutoMirrored.Filled.DirectionsWalk
    TrainingLevel.Intermediate -> Icons.AutoMirrored.Filled.DirectionsRun
    TrainingLevel.Advanced -> Icons.Filled.LocalFireDepartment
    TrainingLevel.Elite -> Icons.Filled.Bolt
}

private fun onboardingTrainingSplitIcon(split: TrainingSplitPreference): ImageVector = when (split) {
    TrainingSplitPreference.Automatic -> Icons.Filled.AutoAwesome
    TrainingSplitPreference.FullBody -> Icons.Filled.FitnessCenter
    TrainingSplitPreference.UpperLower -> Icons.Filled.UnfoldMore
    TrainingSplitPreference.PushPullLegs -> Icons.Filled.FitnessCenter
    TrainingSplitPreference.BodyPartSplit -> Icons.Filled.AccessibilityNew
    TrainingSplitPreference.Custom -> Icons.Filled.Tune
}

private fun onboardingBodyPhaseIcon(phase: BodyCompositionPhase): ImageVector = when (phase) {
    BodyCompositionPhase.Automatic -> Icons.Filled.AutoAwesome
    BodyCompositionPhase.Bulk -> Icons.AutoMirrored.Filled.TrendingUp
    BodyCompositionPhase.Cut -> Icons.AutoMirrored.Filled.TrendingDown
    BodyCompositionPhase.Maintain -> Icons.Filled.HorizontalRule
}

private fun onboardingGymTypeIcon(type: GymType): ImageVector = when (type) {
    GymType.BigGym -> Icons.Filled.Apartment
    GymType.SmallGym -> Icons.Filled.Business
    GymType.HomeGym -> Icons.Filled.Home
    GymType.SomeAccessories -> Icons.Filled.FitnessCenter
    GymType.BodyweightOnly -> Icons.Filled.AccessibilityNew
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
Text(title,Modifier.clip(RoundedCornerShape(12.dp)).background(if(selected)WildforceThemeTokens.textPrimary else WildforceThemeTokens.textSecondary.copy(alpha=.10f)).clickable(onClick=onClick).padding(horizontal=13.dp,vertical=11.dp),color=if(selected) WildforceThemeTokens.backgroundSecondary else WildforceThemeTokens.textPrimary,style=MaterialTheme.typography.caption,fontWeight=if(selected)FontWeight.Bold else FontWeight.Normal,maxLines=1)
}

/** Same compact segmented treatment used by iOS for the measurement system. */
@Composable
private fun MetricSystemSegmentedControl(selected: MetricSystem, onSelect: (MetricSystem) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(9.dp)).background(WildforceThemeTokens.textSecondary.copy(alpha = .12f)).padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        MetricSystem.entries.forEach { system ->
            val isSelected = selected == system
            Text(
                androidx.compose.ui.res.stringResource(if (system == MetricSystem.Metric) io.codepassion.doubletriangle.core.designsystem.R.string.metric else io.codepassion.doubletriangle.core.designsystem.R.string.imperial),
                Modifier.weight(1f).clip(RoundedCornerShape(7.dp)).background(if (isSelected) WildforceThemeTokens.background else Color.Transparent).clickable { onSelect(system) }.padding(vertical = 10.dp),
                color = WildforceThemeTokens.textPrimary,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Native spinner wheels: Android's direct equivalent of SwiftUI's wheel picker. */
@Composable
private fun WheelPicker(label: String, values: List<Int>, selected: Int, labelFor: (Int) -> String, modifier: Modifier = Modifier, onSelected: (Int) -> Unit) {
    val index = values.indexOf(selected).coerceAtLeast(0)
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(WildforceThemeTokens.textSecondary.copy(alpha = .07f)), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = WildforceThemeTokens.textSecondary, style = MaterialTheme.typography.caption)
        AndroidView(
            factory = { context -> NumberPicker(context).apply {
                minValue = 0; maxValue = values.lastIndex; displayedValues = values.map(labelFor).toTypedArray()
                wrapSelectorWheel = false
                setOnValueChangedListener { _, _, newIndex -> onSelected(values[newIndex]) }
            } },
            update = { picker ->
                if (picker.maxValue != values.lastIndex) {
                    picker.displayedValues = null; picker.minValue = 0; picker.maxValue = values.lastIndex; picker.displayedValues = values.map(labelFor).toTypedArray()
                }
                if (picker.value != index) picker.value = index
            },
            modifier = Modifier.fillMaxWidth().height(164.dp),
        )
    }
}

@Composable
private fun WeightWheelPicker(weight: Double, metricSystem: MetricSystem, onSelected: (Double) -> Unit) {
    val integer = weight.toInt().coerceIn(if (metricSystem == MetricSystem.Metric) 35 else 77, if (metricSystem == MetricSystem.Metric) 250 else 551)
    val decimal = ((weight - integer) * 10).toInt().coerceIn(0, 9)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        WheelPicker(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.weight), ((if (metricSystem == MetricSystem.Metric) 35 else 77)..(if (metricSystem == MetricSystem.Metric) 250 else 551)).toList(), integer, { it.toString() }, Modifier.weight(1f)) { onSelected(it + decimal / 10.0) }
        WheelPicker("", (0..9).toList(), decimal, { ".$it ${if (metricSystem == MetricSystem.Metric) "kg" else "lb"}" }, Modifier.weight(1f)) { onSelected(integer + it / 10.0) }
    }
}

private val Equipment.imageResource: Int
    get() = when (this) {
        Equipment.Bodyweight -> R.drawable.equipment_bodyweight
        Equipment.ResistanceBands -> R.drawable.equipment_resistance_bands
        Equipment.Dumbbells -> R.drawable.equipment_dumbbells
        Equipment.Kettlebells -> R.drawable.equipment_kettlebells
        Equipment.MedicineBall -> R.drawable.equipment_medicine_ball
        Equipment.BattleRopes -> R.drawable.equipment_battle_ropes
        Equipment.JumpRope -> R.drawable.equipment_jump_rope
        Equipment.SuspensionTrainer -> R.drawable.equipment_suspension_trainer
        Equipment.GymnasticRings -> R.drawable.equipment_gymnastic_rings
        Equipment.CableMachine -> R.drawable.equipment_cable_machine
        Equipment.SmithMachine -> R.drawable.equipment_smith_machine
        Equipment.LegPressMachine -> R.drawable.equipment_leg_press_machine
        Equipment.ChestPressMachine -> R.drawable.equipment_chest_press_machine
        Equipment.LegCurlMachine -> R.drawable.equipment_leg_curl_machine
        Equipment.RearDeltMachine -> R.drawable.equipment_rear_delt_machine
        Equipment.SeatedRowMachine -> R.drawable.equipment_seated_row_machine
        Equipment.GluteKickbackMachine -> R.drawable.equipment_glute_kickback_machine
        Equipment.PecDeckMachine -> R.drawable.equipment_pec_deck_machine
        Equipment.HipAbductionMachine -> R.drawable.equipment_hip_abduction_machine
        Equipment.HipAdductionMachine -> R.drawable.equipment_hip_adduction_machine
        Equipment.HackSquatMachine -> R.drawable.equipment_hack_squat_machine
        Equipment.HipThrustMachine -> R.drawable.equipment_hip_thrust_machine
        Equipment.RowingMachine -> R.drawable.equipment_rowing_machine
        Equipment.Treadmill -> R.drawable.equipment_treadmill
        Equipment.StationaryBike -> R.drawable.equipment_stationary_bike
        Equipment.Elliptical -> R.drawable.equipment_elliptical
        Equipment.StairClimber -> R.drawable.equipment_stair_climber
        Equipment.SkiErg -> R.drawable.equipment_ski_erg
        Equipment.FlatBench -> R.drawable.equipment_flat_bench
        Equipment.AdjustableBench -> R.drawable.equipment_adjustable_bench
        Equipment.SquatRack -> R.drawable.equipment_squat_rack
        Equipment.OlympicBarbell -> R.drawable.equipment_olympic_barbell
        Equipment.EzBar -> R.drawable.equipment_ez_bar
        Equipment.TrapBar -> R.drawable.equipment_trap_bar
        Equipment.DeadliftPlatform -> R.drawable.equipment_deadlift_platform
        Equipment.PullUpBar -> R.drawable.equipment_pull_up_bar
        Equipment.DipStation -> R.drawable.equipment_dip_station
        Equipment.PlyoBox -> R.drawable.equipment_plyo_box
        Equipment.BoxingBag -> R.drawable.equipment_boxing_bag
        Equipment.LandmineAttachment -> R.drawable.equipment_landmine_attachment
    }

/** The iOS equipment grid uses charcoal selected cards; orange is not part of this state. */
@Composable
private fun EquipmentOption(equipment: Equipment, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) WildforceThemeTokens.textPrimary else WildforceThemeTokens.textSecondary.copy(alpha = .10f))
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Text(
                if (selected) "☑" else "☐",
                color = if (selected) WildforceThemeTokens.backgroundSecondary else WildforceThemeTokens.textSecondary,
                style = MaterialTheme.typography.h6,
            )
        }
        Image(painterResource(equipment.imageResource), contentDescription = null, modifier = Modifier.fillMaxWidth().height(72.dp).padding(vertical = 6.dp), contentScale = ContentScale.Fit)
        Text(
            onboardingLabel("Equipment", equipment.storedValue, equipment.title),
            color = if (selected) WildforceThemeTokens.backgroundSecondary else WildforceThemeTokens.textPrimary,
            style = MaterialTheme.typography.caption,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun PrimaryAction(title: String, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        enabled = enabled,
        shape = RoundedCornerShape(8.dp),
        elevation = ButtonDefaults.elevation(defaultElevation = 2.dp, pressedElevation = 0.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = WildforceThemeTokens.textPrimary,
            contentColor = WildforceThemeTokens.backgroundSecondary,
            disabledBackgroundColor = WildforceThemeTokens.textPrimary.copy(alpha = 0.20f),
            disabledContentColor = WildforceThemeTokens.textPrimary.copy(alpha = 0.75f),
        ),
    ) { Text(title, fontFamily = Exo2FontFamily, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp) }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingPreview() = WildforceTheme { OnboardingScreen { _, _ -> } }
