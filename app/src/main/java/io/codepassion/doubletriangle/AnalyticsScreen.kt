package io.codepassion.doubletriangle

import android.content.SharedPreferences
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.codepassion.doubletriangle.core.designsystem.AntonFontFamily
import io.codepassion.doubletriangle.core.designsystem.WildforceThemeTokens
import io.codepassion.doubletriangle.core.designsystem.liquidGlass
import io.codepassion.doubletriangle.core.designsystem.liquidGlassBackground
import io.codepassion.doubletriangle.core.model.WorkoutHubState

@Composable
fun AnalyticsScreen(state: WorkoutHubState, preferences: SharedPreferences) {
    val completed = state.workouts.count { it.status.name == "Completed" }
    val total = state.workouts.size.coerceAtLeast(1)
    val lastDuration = preferences.getInt("last_workout_duration", 0)
    val lastSets = preferences.getInt("last_workout_sets", 0)
    val lastVolume = java.lang.Double.longBitsToDouble(preferences.getLong("last_workout_volume", 0L))
    Column(Modifier.fillMaxSize().liquidGlassBackground().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("ANALÍTICAS", fontFamily = AntonFontFamily, style = MaterialTheme.typography.h4, color = WildforceThemeTokens.textPrimary)
        Text("Tu evolución y consistencia en un vistazo.", color = WildforceThemeTokens.textSecondary)
        Column(Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(22.dp), emphasized = true).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Column { Text("PROGRESO SEMANAL", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary); Text("$completed / ${state.workouts.size} sesiones", fontFamily = AntonFontFamily, style = MaterialTheme.typography.h5, color = WildforceThemeTokens.textPrimary) }
                Text("${(completed * 100 / total)}%", fontFamily = AntonFontFamily, style = MaterialTheme.typography.h4, color = WildforceThemeTokens.accentGold)
            }
            LinearProgressIndicator(completed.toFloat() / total, Modifier.fillMaxWidth().height(10.dp), WildforceThemeTokens.accentGold, WildforceThemeTokens.textSecondary.copy(alpha = .15f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard("RACHA", state.user.currentStreak.toString(), "días", Modifier.weight(1f))
            MetricCard("SESIONES", completed.toString(), "completadas", Modifier.weight(1f))
        }
        Column(Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(18.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("ÚLTIMA SESIÓN", fontFamily = AntonFontFamily, color = WildforceThemeTokens.accentGold)
            if (lastDuration == 0 && lastSets == 0) {
                Text("Completa tu primer entrenamiento para desbloquear métricas detalladas.", color = WildforceThemeTokens.textSecondary)
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    MetricLine("Tiempo", formatDuration(lastDuration)); MetricLine("Series", lastSets.toString()); MetricLine("Volumen", String.format("%.0f kg", lastVolume))
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text("Las analíticas detalladas por ejercicio y evolución histórica se añadirán sobre este resumen.", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
    }
}

@Composable private fun MetricCard(title: String, value: String, suffix: String, modifier: Modifier) { Column(modifier.liquidGlass(RoundedCornerShape(16.dp)).padding(14.dp)) { Text(title, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary); Text(value, fontFamily = AntonFontFamily, style = MaterialTheme.typography.h4, color = WildforceThemeTokens.textPrimary); Text(suffix, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary) } }
@Composable private fun MetricLine(title: String, value: String) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(value, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary); Text(title, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary) } }
private fun formatDuration(seconds: Int): String = if (seconds >= 3600) "%d h %02d".format(seconds / 3600, seconds / 60 % 60) else "%d min".format(seconds / 60)
