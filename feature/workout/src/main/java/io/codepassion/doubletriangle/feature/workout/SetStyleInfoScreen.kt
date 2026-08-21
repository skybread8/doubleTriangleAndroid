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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.codepassion.doubletriangle.core.designsystem.AntonFontFamily
import io.codepassion.doubletriangle.core.designsystem.WildforceThemeTokens
import io.codepassion.doubletriangle.core.designsystem.liquidGlass
import io.codepassion.doubletriangle.core.designsystem.liquidGlassBackground
import io.codepassion.doubletriangle.core.model.ExerciseSetStyle

private data class SetStyleCopy(
    val meaning: String,
    val operation: String,
    val usefulness: String,
    val benefits: List<String>,
)

@Composable
internal fun SetStyleInfoScreen(style: ExerciseSetStyle, totalSets: Int, onBack: () -> Unit) {
    val copy = style.copyText()
    Column(Modifier.fillMaxSize().liquidGlassBackground().padding(horizontal = 18.dp, vertical = 12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("‹ VOLVER", Modifier.weight(1f).clickable(onClick = onBack).padding(vertical = 10.dp), color = WildforceThemeTokens.accentGold, fontWeight = FontWeight.Bold)
            Text("TIPO DE SERIE", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary, fontWeight = FontWeight.Bold)
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(style.glyph, Modifier.size(52.dp).background(WildforceThemeTokens.accentGold.copy(alpha = 0.14f), RoundedCornerShape(14.dp)).padding(top = 11.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.h5, color = WildforceThemeTokens.textPrimary)
            Column(Modifier.padding(start = 14.dp)) {
                Text(style.label.uppercase(), fontFamily = AntonFontFamily, style = MaterialTheme.typography.h5, color = WildforceThemeTokens.textPrimary)
                Text(style.scopeLabel(totalSets), color = WildforceThemeTokens.accentGold, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.caption)
            }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            InfoSection("QUÉ SIGNIFICA", copy.meaning)
            InfoSection("CÓMO FUNCIONA", copy.operation)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(totalSets.coerceAtLeast(1)) { index ->
                    val setNumber = index + 1
                    Column(Modifier.liquidGlass(RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(setNumber.toString(), fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
                        Text(setStyleInstruction(style, setNumber, totalSets), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.accentGold, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Text("BENEFICIOS", fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary, modifier = Modifier.padding(top = 12.dp, bottom = 8.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                copy.benefits.forEach { benefit ->
                    Text(benefit, Modifier.background(WildforceThemeTokens.accentGold.copy(alpha = 0.12f), RoundedCornerShape(20.dp)).padding(horizontal = 12.dp, vertical = 8.dp), color = WildforceThemeTokens.textPrimary, style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold)
                }
            }
            InfoSection("POR QUÉ ES ÚTIL", copy.usefulness)
            Spacer(Modifier.padding(bottom = 12.dp))
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

private fun ExerciseSetStyle.scopeLabel(totalSets: Int): String = when {
    totalSets <= 1 -> "Se aplica en esta serie"
    this == ExerciseSetStyle.TopSetBackoff -> "Top set primero"
    this == ExerciseSetStyle.DropSet || this == ExerciseSetStyle.RestPause -> "Solo en la última serie"
    else -> "Se aplica en todas las series"
}

private fun ExerciseSetStyle.copyText(): SetStyleCopy = when (this) {
    ExerciseSetStyle.Warmup -> SetStyleCopy("Series más ligeras que preparan músculos, articulaciones y técnica.", "Aumenta la carga gradualmente sin generar fatiga antes del trabajo principal.", "Hace que la primera serie exigente no llegue de forma brusca y mejora la calidad del movimiento.", listOf("Preparación", "Técnica", "Movilidad"))
    ExerciseSetStyle.Straight -> SetStyleCopy("Mismo peso y esfuerzo parecido en todas las series.", "Mantén una carga estable e intenta repetir un rendimiento consistente.", "Es simple de medir y facilita comparar el progreso entre semanas.", listOf("Consistencia", "Progreso", "Simplicidad"))
    ExerciseSetStyle.TopSetBackoff -> SetStyleCopy("Una primera serie pesada seguida de trabajo más ligero.", "Empieza con tu top set y reduce la carga en las series backoff manteniendo una ejecución limpia.", "Combina intensidad alta y volumen de calidad controlando la fatiga.", listOf("Intensidad", "Volumen", "Control"))
    ExerciseSetStyle.AscendingPyramid -> SetStyleCopy("El peso aumenta serie a serie y normalmente bajan las repeticiones.", "Sube la carga progresivamente hasta alcanzar la serie más exigente al final.", "Permite acercarse con confianza a cargas altas y consolidar el ritmo técnico.", listOf("Progresión", "Carga", "Fuerza"))
    ExerciseSetStyle.DropSet -> SetStyleCopy("Una serie extendida reduciendo peso con poco o ningún descanso.", "Completa la última serie, baja rápidamente la carga y continúa haciendo repeticiones.", "Añade estímulo de hipertrofia y fatiga local en muy poco tiempo.", listOf("Hipertrofia", "Intensidad", "Finalizador"))
    ExerciseSetStyle.RestPause -> SetStyleCopy("Una serie dura dividida en mini-series con pausas muy breves.", "Acércate al fallo, descansa unos segundos y continúa con el mismo peso.", "Consigue más repeticiones exigentes sin añadir varias series completas.", listOf("Más repeticiones", "Pausa breve", "Esfuerzo"))
    ExerciseSetStyle.Intervals -> SetStyleCopy("Esfuerzos repetidos alternados con recuperaciones planificadas.", "Mantén un ritmo exigente pero repetible durante todos los intervalos.", "Mejora el acondicionamiento, el ritmo y la capacidad de repetir esfuerzo.", listOf("Ritmo", "Resistencia", "Repetición"))
    ExerciseSetStyle.Tempo -> SetStyleCopy("Cada fase de la repetición se realiza a una velocidad controlada.", "Controla bajada, pausa y subida en lugar de acelerar el movimiento.", "Mejora técnica y tensión muscular incluso usando cargas más ligeras.", listOf("Control", "Tensión", "Técnica"))
}
