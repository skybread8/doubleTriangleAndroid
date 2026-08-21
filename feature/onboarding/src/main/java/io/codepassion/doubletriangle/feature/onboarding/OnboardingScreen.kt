package io.codepassion.doubletriangle.feature.onboarding

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.codepassion.doubletriangle.core.designsystem.AntonFontFamily
import io.codepassion.doubletriangle.core.designsystem.WildforceTheme
import io.codepassion.doubletriangle.core.designsystem.WildforceThemeTokens
import io.codepassion.doubletriangle.core.designsystem.liquidGlass
import io.codepassion.doubletriangle.core.designsystem.liquidGlassBackground

data class OnboardingProfile(
    val name: String,
    val goal: FitnessGoal,
    val lifestyle: LifestyleLevel = LifestyleLevel.ModeratelyActive,
    val workoutDays: Set<WorkoutWeekday> = setOf(WorkoutWeekday.Monday, WorkoutWeekday.Wednesday, WorkoutWeekday.Friday),
    val preferredWorkoutDurationMinutes: Int = 50,
    val trainingLevel: TrainingLevel = TrainingLevel.Novice,
    val trainingSplitPreference: TrainingSplitPreference = TrainingSplitPreference.Automatic,
    val bodyCompositionPhase: BodyCompositionPhase? = null,
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
fun OnboardingScreen(onCompleted: (OnboardingProfile) -> Unit) {
    var step by remember { mutableStateOf(0) }
    var name by remember { mutableStateOf("") }
    var goal by remember { mutableStateOf<FitnessGoal?>(null) }
    var lifestyle by remember { mutableStateOf<LifestyleLevel?>(null) }
    var workoutDays by remember { mutableStateOf(setOf(WorkoutWeekday.Monday, WorkoutWeekday.Wednesday, WorkoutWeekday.Friday)) }
    var duration by remember { mutableStateOf(50) }
    var trainingLevel by remember { mutableStateOf(TrainingLevel.Novice) }
    var trainingSplit by remember { mutableStateOf(TrainingSplitPreference.Automatic) }
    var bodyPhase by remember { mutableStateOf(BodyCompositionPhase.Automatic) }
    val completeProfile = {
        val selectedGoal = goal
        val selectedLifestyle = lifestyle
        if (selectedGoal != null && selectedLifestyle != null) {
            val selectedBodyPhase = bodyPhase.takeIf { selectedGoal.supportsBodyComposition && trainingLevel.supportsBodyComposition }
            onCompleted(OnboardingProfile(name.trim(), selectedGoal, selectedLifestyle, workoutDays, duration, trainingLevel, trainingSplit, selectedBodyPhase))
        }
    }

    AnimatedContent(targetState = step, label = "onboarding-step") { currentStep ->
        when (currentStep) {
            0 -> CoverStep { step = 1 }
            1 -> FormStep(
                progress = 1f / 16f,
                title = "Bienvenido",
                subtitle = "¿Cómo quieres que te llamemos?",
                canContinue = name.isNotBlank(),
                onBack = { step = 0 },
                onContinue = { step = 2 },
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(18.dp)),
                    textStyle = MaterialTheme.typography.h5.copy(textAlign = TextAlign.Center),
                    placeholder = { Text("Tu nombre", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                )
            }
            2 -> FormStep(
                progress = 2f / 16f,
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
                progress = 3f / 16f,
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
                progress = 4f / 16f,
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
                progress = 5f / 16f,
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
                    Text("−", Modifier.size(48.dp).clickable { duration = (duration - 5).coerceAtLeast(15) }, textAlign = TextAlign.Center, style = MaterialTheme.typography.h4)
                    Text("$duration min", style = MaterialTheme.typography.h5, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
                    Text("+", Modifier.size(48.dp).clickable { duration = (duration + 5).coerceAtMost(180) }, textAlign = TextAlign.Center, style = MaterialTheme.typography.h4)
                }
            }
            6 -> FormStep(
                progress = 6f / 16f,
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
                progress = 7f / 16f,
                title = "Programa",
                subtitle = "Elige la estructura de entrenamiento que prefieres.",
                canContinue = true,
                onBack = { step = 6 },
                onContinue = {
                    if (goal?.supportsBodyComposition == true && trainingLevel.supportsBodyComposition) step = 8
                    else completeProfile()
                },
                buttonTitle = if (goal?.supportsBodyComposition == true && trainingLevel.supportsBodyComposition) "CONTINUAR" else "CREAR PLAN",
            ) {
                trainingSplit.warning(workoutDays.size)?.let { warning ->
                    Text(
                        warning,
                        Modifier.fillMaxWidth().background(Color(0xFFFFB020).copy(alpha = 0.14f), RoundedCornerShape(12.dp)).padding(12.dp),
                        color = WildforceThemeTokens.textPrimary,
                    )
                    Spacer(Modifier.height(12.dp))
                }
                TrainingSplitPreference.entries.forEach { split ->
                    ChoiceOption(split.title, split.description, split.glyph, trainingSplit == split) { trainingSplit = split }
                    Spacer(Modifier.height(10.dp))
                }
            }
            else -> FormStep(
                progress = 8f / 16f,
                title = "Fase corporal",
                subtitle = "Elige cómo debe influir tu objetivo corporal en el plan.",
                canContinue = true,
                onBack = { step = 7 },
                onContinue = completeProfile,
                buttonTitle = "CREAR PLAN",
            ) {
                BodyCompositionPhase.entries.forEach { phase ->
                    ChoiceOption(phase.title, phase.description, phase.glyph, bodyPhase == phase) { bodyPhase = phase }
                    Spacer(Modifier.height(10.dp))
                }
            }
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
            modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 32.dp, vertical = 36.dp),
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
            Spacer(Modifier.height(36.dp))
            PrimaryAction("EMPEZAR", true, onStart)
        }
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
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize().liquidGlassBackground()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
            .liquidGlass(RoundedCornerShape(14.dp)).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("‹", modifier = Modifier.size(44.dp).clickable(onClick = onBack), style = MaterialTheme.typography.h4, color = WildforceThemeTokens.textPrimary)
            LinearProgressIndicator(progress = progress, modifier = Modifier.weight(1f), color = WildforceThemeTokens.accentGold)
            Spacer(Modifier.size(44.dp))
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Text(title, style = MaterialTheme.typography.h4, fontFamily = AntonFontFamily, color = WildforceThemeTokens.textPrimary)
            Text(subtitle, color = WildforceThemeTokens.textSecondary, style = MaterialTheme.typography.body1)
            Spacer(Modifier.height(34.dp))
            content()
        }
        Box(Modifier.fillMaxWidth().padding(12.dp).liquidGlass(RoundedCornerShape(18.dp), emphasized = true).padding(16.dp)) {
            PrimaryAction(buttonTitle, canContinue, onContinue)
        }
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
        Modifier.fillMaxWidth().then(
            if (selected) Modifier.clip(RoundedCornerShape(16.dp)).background(WildforceThemeTokens.accentGold.copy(alpha = 0.20f))
            else Modifier.liquidGlass(RoundedCornerShape(16.dp)),
        ).clickable(onClick = onClick).padding(15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(glyph, style = MaterialTheme.typography.h5, color = WildforceThemeTokens.accentGold)
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(title, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
            description?.let {
                Text(it, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
            }
        }
        Text(if (selected) "●" else "○", color = WildforceThemeTokens.accentGold)
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
private fun OnboardingPreview() = WildforceTheme { OnboardingScreen {} }
