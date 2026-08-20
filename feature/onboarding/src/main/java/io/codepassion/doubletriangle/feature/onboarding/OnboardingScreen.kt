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

data class OnboardingProfile(val name: String, val goal: FitnessGoal)

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

    AnimatedContent(targetState = step, label = "onboarding-step") { currentStep ->
        when (currentStep) {
            0 -> CoverStep { step = 1 }
            1 -> FormStep(
                progress = 0.5f,
                title = "Bienvenido",
                subtitle = "¿Cómo quieres que te llamemos?",
                canContinue = name.isNotBlank(),
                onBack = { step = 0 },
                onContinue = { step = 2 },
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.h5.copy(textAlign = TextAlign.Center),
                    placeholder = { Text("Tu nombre", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                )
            }
            else -> FormStep(
                progress = 1f,
                title = "Tus objetivos",
                subtitle = "Elige el objetivo de entrenamiento que mejor encaja contigo.",
                canContinue = goal != null,
                onBack = { step = 1 },
                onContinue = { goal?.let { onCompleted(OnboardingProfile(name.trim(), it)) } },
                buttonTitle = "CREAR PLAN",
            ) {
                FitnessGoal.entries.forEach { item ->
                    GoalOption(item = item, selected = goal == item, onClick = { goal = item })
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
            modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 42.dp, vertical = 36.dp),
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
    Column(Modifier.fillMaxSize().background(WildforceThemeTokens.background)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
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
        Box(Modifier.fillMaxWidth().background(WildforceThemeTokens.backgroundSecondary).padding(24.dp)) {
            PrimaryAction(buttonTitle, canContinue, onContinue)
        }
    }
}

@Composable
private fun GoalOption(item: FitnessGoal, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(if (selected) WildforceThemeTokens.accentGold.copy(alpha = 0.18f) else WildforceThemeTokens.backgroundSecondary)
            .clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(item.glyph, style = MaterialTheme.typography.h5, color = WildforceThemeTokens.accentGold)
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(item.title, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
            Text(item.description, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
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
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = WildforceThemeTokens.textPrimary,
            contentColor = WildforceThemeTokens.backgroundSecondary,
        ),
    ) { Text(title, fontWeight = FontWeight.Bold) }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingPreview() = WildforceTheme { OnboardingScreen {} }
