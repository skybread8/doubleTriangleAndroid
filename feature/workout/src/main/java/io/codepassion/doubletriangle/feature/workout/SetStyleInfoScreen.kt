package io.codepassion.doubletriangle.feature.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PauseCircleOutline
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.codepassion.doubletriangle.core.designsystem.AntonFontFamily
import io.codepassion.doubletriangle.core.designsystem.WildforceThemeTokens
import io.codepassion.doubletriangle.core.designsystem.liquidGlass
import io.codepassion.doubletriangle.core.designsystem.liquidGlassBackground
import io.codepassion.doubletriangle.core.model.ExerciseSetStyle
import io.codepassion.doubletriangle.core.model.SetStyleParameters

private data class SetStyleCopy(
    val meaning: String,
    val operation: String,
    val usefulness: String,
    val benefits: List<String>,
)

/** The same plan-specific legend shown by iOS, not just abbreviated chips. */
internal data class SetStyleDetail(val label: String, val explanation: String)

internal fun setStyleIcon(style: ExerciseSetStyle): ImageVector = when (style) {
    ExerciseSetStyle.Warmup -> Icons.Filled.LocalFireDepartment
    ExerciseSetStyle.Straight -> Icons.Filled.Equalizer
    ExerciseSetStyle.TopSetBackoff -> Icons.Filled.Terrain
    ExerciseSetStyle.AscendingPyramid -> Icons.Filled.TrendingUp
    ExerciseSetStyle.DropSet -> Icons.Filled.TrendingDown
    ExerciseSetStyle.RestPause -> Icons.Filled.PauseCircleOutline
    ExerciseSetStyle.Intervals -> Icons.Filled.Timer
    ExerciseSetStyle.Tempo -> Icons.Filled.MusicNote
}

/**
 * Keeps the straight-set glyph aligned with iOS, whose SF Symbol is `equal`
 * (two horizontal strokes), rather than Material's three-bar Equalizer icon.
 */
@Composable
internal fun SetStyleGlyph(style: ExerciseSetStyle, modifier: Modifier = Modifier, tint: Color) {
    if (style == ExerciseSetStyle.Straight) {
        Text(
            "=",
            modifier = modifier,
            color = tint,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    } else {
        Icon(setStyleIcon(style), contentDescription = null, modifier = modifier, tint = tint)
    }
}

@Composable
internal fun SetStyleInfoScreen(style: ExerciseSetStyle, totalSets: Int, parameters: SetStyleParameters, onBack: () -> Unit) {
    val copy = style.copyText(parameters)
    Column(Modifier.fillMaxSize().liquidGlassBackground().padding(horizontal = 18.dp, vertical = 12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f).heightIn(min = 48.dp).semantics { contentDescription = "Volver" }.clickable(onClick = onBack).padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp), tint = WildforceThemeTokens.accent)
                Text("VOLVER", color = WildforceThemeTokens.accent, fontWeight = FontWeight.Bold)
            }
            Text("TIPO DE SERIE", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary, fontWeight = FontWeight.Bold)
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            SetStyleGlyph(style, Modifier.size(52.dp).background(WildforceThemeTokens.accent.copy(alpha = 0.14f), RoundedCornerShape(14.dp)).padding(14.dp), WildforceThemeTokens.textPrimary)
            Column(Modifier.padding(start = 14.dp)) {
                Text(style.label.uppercase(), fontFamily = AntonFontFamily, style = MaterialTheme.typography.h5, color = WildforceThemeTokens.textPrimary)
                style.scopeLabel(totalSets, parameters.appliesToFinalSetOnly)?.let {
                    Text(it, color = WildforceThemeTokens.accent, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.caption)
                }
            }
        }
        val planDetails = styleParameterDetails(style, parameters)
        if (planDetails.isNotEmpty()) {
            Text("DETALLES DE TU PLAN", fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary, modifier = Modifier.padding(top = 8.dp, bottom = 6.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                planDetails.forEach { detail ->
                    Column(
                        Modifier.fillMaxWidth()
                            .background(WildforceThemeTokens.textPrimary.copy(alpha = 0.04f), RoundedCornerShape(14.dp))
                            .padding(12.dp),
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                Icons.Filled.Info,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp).background(WildforceThemeTokens.accent.copy(alpha = 0.12f), RoundedCornerShape(8.dp)).padding(6.dp),
                                tint = WildforceThemeTokens.accent,
                            )
                            Column(Modifier.padding(start = 12.dp)) {
                                Text(detail.label, fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textPrimary)
                                Text(detail.explanation, Modifier.padding(top = 3.dp), style = MaterialTheme.typography.body2, color = WildforceThemeTokens.textSecondary)
                            }
                        }
                    }
                }
            }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            InfoSection("QUÉ SIGNIFICA", copy.meaning)
            InfoSection("CÓMO FUNCIONA", copy.operation)
            SetStyleMiniDiagram(style, totalSets, parameters)
            Text("BENEFICIOS", fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary, modifier = Modifier.padding(top = 12.dp, bottom = 8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                copy.benefits.chunked(2).forEach { rowBenefits ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        rowBenefits.forEach { benefit ->
                            Row(
                                Modifier.weight(1f).background(WildforceThemeTokens.textPrimary.copy(alpha = 0.10f), RoundedCornerShape(20.dp)).padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = WildforceThemeTokens.accent, modifier = Modifier.size(15.dp))
                                Text(benefit, color = WildforceThemeTokens.textPrimary, style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                        }
                        if (rowBenefits.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
            InfoSection("POR QUÉ ES ÚTIL", copy.usefulness)
            Spacer(Modifier.padding(bottom = 12.dp))
        }
    }
}

@Composable
private fun SetStyleMiniDiagram(style: ExerciseSetStyle, totalSets: Int, parameters: SetStyleParameters) {
    val items = when (style) {
        ExerciseSetStyle.Warmup -> listOf("LIGERA", "SUBE", "LISTA")
        ExerciseSetStyle.Straight -> List(totalSets.coerceAtLeast(1).coerceAtMost(6)) { "S${it + 1}" }
        ExerciseSetStyle.TopSetBackoff -> listOf("TOP SET") + List(parameters.backoffSetCount.coerceAtLeast(1).coerceAtMost(4)) { "BACKOFF" }
        ExerciseSetStyle.AscendingPyramid -> List(totalSets.coerceAtLeast(1).coerceAtMost(6)) { "${it + 1}" }
        ExerciseSetStyle.DropSet -> listOf("SERIE", "-${parameters.dropWeightPercent}%", "-${parameters.dropWeightPercent}%")
        ExerciseSetStyle.RestPause -> listOf("DURO", "${parameters.intraSetRestSeconds}s", "MINI", "${parameters.intraSetRestSeconds}s", "MINI")
        ExerciseSetStyle.Intervals -> listOf("TRABAJO", "${parameters.intraSetRestSeconds}s", "TRABAJO", "${parameters.intraSetRestSeconds}s")
        ExerciseSetStyle.Tempo -> parameters.tempo.split('-').filter { it.isNotBlank() }.ifEmpty { listOf("BAJA", "PAUSA", "SUBE") }.map { "${it}s" }
    }
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEachIndexed { index, label ->
            val isPause = label.endsWith("s") && style in setOf(ExerciseSetStyle.RestPause, ExerciseSetStyle.Intervals, ExerciseSetStyle.Tempo)
            Text(
                label,
                Modifier
                    .background(
                        if (isPause) WildforceThemeTokens.backgroundSecondary else WildforceThemeTokens.textPrimary,
                        RoundedCornerShape(20.dp),
                    )
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                color = if (isPause) WildforceThemeTokens.textSecondary else WildforceThemeTokens.backgroundSecondary,
                style = MaterialTheme.typography.caption,
                fontWeight = FontWeight.Bold,
            )
            if (index < items.lastIndex && style in setOf(ExerciseSetStyle.DropSet, ExerciseSetStyle.RestPause, ExerciseSetStyle.Intervals)) {
                Icon(Icons.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp), tint = WildforceThemeTokens.textSecondary)
            }
        }
    }
}

@Composable
private fun InfoSection(title: String, body: String) {
    Column(Modifier.padding(top = 16.dp)) {
        Text(title, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
        Text(body, Modifier.padding(top = 6.dp), color = WildforceThemeTokens.textSecondary, style = MaterialTheme.typography.body2)
    }
}

internal fun setStyleScopeLabel(style: ExerciseSetStyle, totalSets: Int, appliesToFinalSetOnly: Boolean): String? = when {
    totalSets <= 1 -> null
    style == ExerciseSetStyle.TopSetBackoff -> "Top set primero"
    appliesToFinalSetOnly -> "Solo en la última serie"
    else -> "Se aplica en todas las series"
}

private fun ExerciseSetStyle.scopeLabel(totalSets: Int, appliesToFinalSetOnly: Boolean): String? =
    setStyleScopeLabel(this, totalSets, appliesToFinalSetOnly)

internal fun styleParameterDetails(style: ExerciseSetStyle, parameters: SetStyleParameters): List<SetStyleDetail> = buildList {
    when (style) {
        ExerciseSetStyle.TopSetBackoff -> {
            add(SetStyleDetail("${parameters.backoffSetCount} backoff", "Después del top set, realiza ${parameters.backoffSetCount} serie${if (parameters.backoffSetCount == 1) "" else "s"} más ligera${if (parameters.backoffSetCount == 1) "" else "s"} para sumar volumen de calidad."))
            add(SetStyleDetail("-${parameters.backoffWeightPercent}% de carga", "Para las series backoff, reduce aproximadamente un ${parameters.backoffWeightPercent}% el peso respecto al top set."))
        }
        ExerciseSetStyle.DropSet -> {
            add(SetStyleDetail("${parameters.dropCount} drop${if (parameters.dropCount == 1) "" else "s"}", "Tras el esfuerzo principal, baja el peso y continúa ${parameters.dropCount} ${if (parameters.dropCount == 1) "vez" else "veces"} casi sin descanso."))
            add(SetStyleDetail("-${parameters.dropWeightPercent}% por drop", "En cada descenso utiliza aproximadamente un ${parameters.dropWeightPercent}% menos de carga que en el esfuerzo anterior."))
        }
        ExerciseSetStyle.RestPause, ExerciseSetStyle.Intervals -> add(SetStyleDetail("${parameters.intraSetRestSeconds}s de pausa", "Descansa unos ${parameters.intraSetRestSeconds} segundos antes del siguiente mini-esfuerzo de esta misma secuencia."))
        ExerciseSetStyle.Tempo -> add(SetStyleDetail("Tempo ${parameters.tempo}", "El tempo ${parameters.tempo} indica cómo controlar la bajada, las pausas y la subida de cada repetición."))
        else -> Unit
    }
    if (style != ExerciseSetStyle.Warmup) {
        val explanation = if (parameters.targetRir == 0) {
            "RIR 0 significa terminar al fallo, sin repeticiones en reserva."
        } else {
            "RIR ${parameters.targetRir} significa acabar sintiendo que aún podrías hacer aproximadamente ${parameters.targetRir} repetición${if (parameters.targetRir == 1) "" else "es"} con buena técnica."
        }
        add(SetStyleDetail("RIR ${parameters.targetRir}", explanation))
    }
}

/** Kept as a compact representation for the inline active-workout card. */
internal fun styleParameterLabels(style: ExerciseSetStyle, parameters: SetStyleParameters): List<String> =
    styleParameterDetails(style, parameters).map(SetStyleDetail::label)

private fun ExerciseSetStyle.copyText(parameters: SetStyleParameters): SetStyleCopy = when (this) {
    ExerciseSetStyle.Warmup -> SetStyleCopy("Series más ligeras que preparan músculos, articulaciones y técnica.", "Empieza con series fáciles antes de las exigentes; sube la temperatura, practica el movimiento y acércate a tu carga de trabajo sin acumular demasiada fatiga.", "Te prepara para el trabajo pesado, mejora la calidad del movimiento y evita que la primera serie dura llegue de golpe.", listOf("Preparación", "Técnica", "Movilidad"))
    ExerciseSetStyle.Straight -> SetStyleCopy("Mismo peso y esfuerzo parecido en todas las series.", "Usa la misma carga de trabajo y un esfuerzo similar. Cada serie debe sentirse consistente para seguir el rendimiento y detectar el progreso.", "Son fáciles de repetir y progresar: puedes comparar serie a serie y semana a semana sin demasiadas variables.", listOf("Consistencia", "Progreso", "Simplicidad"))
    ExerciseSetStyle.TopSetBackoff -> SetStyleCopy("Una primera serie pesada seguida de trabajo más ligero.", "Llega a una serie principal muy exigente y luego reduce la carga aproximadamente un ${parameters.backoffWeightPercent}% durante ${parameters.backoffSetCount} backoff para sumar volumen de calidad.", "Combina intensidad y volumen: el top set practica el esfuerzo alto y los backoff añaden repeticiones productivas sin mantener tu peso máximo.", listOf("Intensidad", "Volumen", "Control"))
    ExerciseSetStyle.AscendingPyramid -> SetStyleCopy("El peso aumenta serie a serie y normalmente bajan las repeticiones.", "Aumenta el peso en cada serie mientras normalmente bajan las repeticiones. Empiezas manejable y terminas con el trabajo más exigente.", "Te permite entrar progresivamente en cargas altas, mejora la confianza y el ritmo técnico para el trabajo de fuerza.", listOf("Progresión", "Carga", "Fuerza"))
    ExerciseSetStyle.DropSet -> SetStyleCopy("Una serie extendida reduciendo peso con poco o ningún descanso.", "Completa el esfuerzo principal, reduce inmediatamente la carga aproximadamente un ${parameters.dropWeightPercent}% y continúa. Repite ese descenso ${parameters.dropCount} ${if (parameters.dropCount == 1) "vez" else "veces"} con descanso mínimo.", "Aporta estímulo extra de hipertrofia en poco tiempo; concentra mucha fatiga y funciona especialmente bien como finalizador.", listOf("Hipertrofia", "Intensidad", "Finalizador"))
    ExerciseSetStyle.RestPause -> SetStyleCopy("Una serie dura dividida en mini-series con pausas muy breves.", "Acércate al fallo, descansa unos ${parameters.intraSetRestSeconds} segundos y continúa con el mismo peso en otro mini-set. Puedes repetir el patrón para conseguir más repeticiones de calidad.", "Permite hacer más repeticiones exigentes con una carga desafiante sin añadir muchas series completas.", listOf("Más repeticiones", "Pausa breve", "Esfuerzo"))
    ExerciseSetStyle.Intervals -> SetStyleCopy("Esfuerzos repetidos alternados con recuperaciones planificadas.", "Alterna bloques de trabajo con unos ${parameters.intraSetRestSeconds} segundos de recuperación. Cada intervalo debe ser exigente pero repetible para mantener la intensidad objetivo.", "Mejora el acondicionamiento, el ritmo y la capacidad de repetir esfuerzos intensos con una estructura clara de trabajo y recuperación.", listOf("Ritmo", "Resistencia", "Repetición"))
    ExerciseSetStyle.Tempo -> SetStyleCopy("Cada fase de la repetición se realiza a una velocidad controlada.", "Usa el tempo ${parameters.tempo} en cada repetición. Define cómo controlar bajada, pausa y subida para que el estímulo dependa de la calidad del movimiento, no solo de la carga.", "Mejora el control y la tensión sobre el músculo objetivo; hace eficaces cargas ligeras cuando importan la técnica y la colocación.", listOf("Control", "Tensión", "Técnica"))
}
