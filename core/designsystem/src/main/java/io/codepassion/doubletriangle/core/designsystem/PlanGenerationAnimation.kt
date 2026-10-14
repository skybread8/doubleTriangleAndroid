package io.codepassion.doubletriangle.core.designsystem

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Shared counterpart of iOS's CreatePlanAnimationView.
 *
 * Keeping this in the design system prevents the onboarding, workout and nutrition
 * entry points from slowly drifting into separate generation experiences.
 */
@Composable
fun PlanGenerationAnimation(
    icon: String = "✦",
    steps: List<String> = defaultPlanGenerationSteps(),
    modifier: Modifier = Modifier,
) {
    var currentStep by remember { mutableIntStateOf(0) }
    var isStepComplete by remember { mutableStateOf(false) }
    var notificationTick by remember { mutableIntStateOf(0) }
    val safeSteps = steps.ifEmpty { defaultPlanGenerationSteps() }

    LaunchedEffect(safeSteps) {
        while (true) {
            isStepComplete = false
            // iOS holds a working step for 3.4 s and marks it complete at 72%.
            delay(2_448)
            isStepComplete = true
            delay(1_852)
            currentStep = (currentStep + 1) % safeSteps.size
        }
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(2_000)
            notificationTick += 1
        }
    }

    val transition = rememberInfiniteTransition(label = "plan-generation")
    val copyOpacity by transition.animateFloat(
        initialValue = 1f,
        targetValue = .38f,
        animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse),
        label = "plan-generation-copy-pulse",
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(50.dp),
    ) {
        NotificationPlaceholderStack(notificationTick, icon)
        Row(
            modifier = Modifier.fillMaxWidth().height(80.dp).padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (isStepComplete) "✓" else "○",
                color = if (isStepComplete) androidx.compose.ui.graphics.Color(0xFF34A853) else WildforceThemeTokens.textSecondary.copy(alpha = .5f),
                style = MaterialTheme.typography.h5,
                modifier = Modifier.alpha(if (isStepComplete) 1f else copyOpacity),
            )
            AnimatedContent(targetState = currentStep, label = "plan-generation-step") { index ->
                Text(
                    safeSteps[index],
                    modifier = Modifier.padding(start = 10.dp).alpha(if (isStepComplete) 1f else copyOpacity),
                    color = WildforceThemeTokens.textPrimary,
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun NotificationPlaceholderStack(tick: Int, icon: String) {
    Box(Modifier.fillMaxWidth().height(250.dp), contentAlignment = Alignment.Center) {
        AnimatedContent(
            targetState = tick,
            transitionSpec = {
                (slideInVertically(animationSpec = tween(550)) { fullHeight -> fullHeight } + fadeIn(tween(550)))
                    .togetherWith(slideOutVertically(animationSpec = tween(550)) { fullHeight -> -fullHeight } + fadeOut(tween(550)))
            },
            label = "plan-generation-notification-stack",
        ) {
            // Identical to NotificationStackAnimationView on iOS: three cards,
            // 80 pt apart; the centre one is full-size and its neighbours dimmed.
            listOf(0, 1, 2).forEach { position ->
                PlanNotificationPlaceholder(
                    icon = icon,
                    modifier = Modifier
                        .fillMaxWidth(.8f)
                        .height(90.dp)
                        .graphicsLayer {
                            val scale = if (position == 1) 1f else .9f
                            scaleX = scale
                            scaleY = scale
                            alpha = if (position == 1) 1f else .7f
                        }
                        .offset(y = ((position - 1) * 80).dp),
                )
            }
        }
    }
}

@Composable
private fun PlanNotificationPlaceholder(icon: String, modifier: Modifier) {
    Box(
        modifier
            .shadow(8.dp, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .background(WildforceThemeTokens.backgroundSecondary)
            .padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(6.dp))
                    .background(WildforceThemeTokens.textSecondary.copy(alpha = .2f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(icon, color = WildforceThemeTokens.textSecondary, style = MaterialTheme.typography.subtitle1)
            }
            Column(Modifier.padding(start = 12.dp)) {
                PlaceholderBar(140.dp, 14.dp, WildforceThemeTokens.textPrimary.copy(alpha = .3f))
                Spacer(Modifier.height(8.dp))
                PlaceholderBar(200.dp, 12.dp, WildforceThemeTokens.textSecondary.copy(alpha = .3f))
            }
            Spacer(Modifier.weight(1f))
            PlaceholderBar(40.dp, 12.dp, WildforceThemeTokens.textSecondary.copy(alpha = .3f))
        }
    }
}

@Composable
private fun PlaceholderBar(width: androidx.compose.ui.unit.Dp, height: androidx.compose.ui.unit.Dp, color: androidx.compose.ui.graphics.Color) {
    Spacer(Modifier.width(width).height(height).clip(RoundedCornerShape(8.dp)).background(color))
}

private fun defaultPlanGenerationSteps() = listOf(
    "Analizando tus objetivos de fitness…",
    "Evaluando tu equipamiento disponible…",
    "Optimizando la división de entrenamientos…",
    "Calculando las cargas objetivo…",
    "Revisando las restricciones de movimiento…",
    "Consultando al generador de IA…",
    "Personalizando los ejercicios…",
    "Equilibrando volumen e intensidad…",
    "Terminando tu plan personalizado…",
    "Ya casi está…",
)
