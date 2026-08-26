package io.codepassion.doubletriangle.feature.workout

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.codepassion.doubletriangle.core.designsystem.AntonFontFamily
import io.codepassion.doubletriangle.core.designsystem.WildforceThemeTokens
import io.codepassion.doubletriangle.core.model.ExerciseSummary
import org.json.JSONObject

internal data class ExerciseGuide(val description: String, val instructions: List<String>, val tips: List<String>)

private object ExerciseGuideRepository {
    private var guides: Map<String, ExerciseGuide>? = null

    fun get(context: Context, imageKey: String?): ExerciseGuide? {
        if (imageKey == null) return null
        val loaded = guides ?: load(context).also { guides = it }
        return loaded[imageKey]
    }

    private fun load(context: Context): Map<String, ExerciseGuide> = runCatching {
        val root = JSONObject(context.resources.openRawResource(R.raw.exercise_guide_es).bufferedReader().use { it.readText() })
        buildMap {
            root.keys().forEach { key ->
                val item = root.getJSONObject(key)
                put(key, ExerciseGuide(item.optString("description"), item.stringList("instructions"), item.stringList("tips")))
            }
        }
    }.getOrDefault(emptyMap())

    private fun JSONObject.stringList(name: String): List<String> {
        val array = optJSONArray(name) ?: return emptyList()
        return List(array.length()) { array.getString(it) }
    }
}

@Composable
internal fun ExerciseGuideScreen(exercise: ExerciseSummary, gender: String, onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val guide = remember(exercise.imageKey) { ExerciseGuideRepository.get(context, exercise.imageKey) }
    Box(Modifier.fillMaxSize().background(WildforceThemeTokens.backgroundSecondary)) {
        RemoteTrainingImage(exerciseImageUrl(exercise.imageKey, gender), exercise.name, Modifier.fillMaxWidth().height(500.dp))
        Box(Modifier.fillMaxWidth().height(500.dp).background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent, WildforceThemeTokens.backgroundSecondary))))
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Box(Modifier.padding(14.dp).size(48.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.84f)).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", tint = Color.Black, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.height(255.dp))
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 38.dp, topEnd = 38.dp)).background(WildforceThemeTokens.backgroundSecondary).padding(20.dp)) {
                Text(exercise.name.uppercase(), fontFamily = AntonFontFamily, fontSize = 34.sp, color = WildforceThemeTokens.textPrimary, maxLines = 2)
                Text("${exercise.sets} series · ${exercise.reps} reps", color = WildforceThemeTokens.textSecondary)
                Spacer(Modifier.height(22.dp))
                Text("MÚSCULOS", fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
                MuscleStrip(exercise.imageKey, onDarkBackground = false, modifier = Modifier.padding(vertical = 8.dp))
                Text(guide?.description?.takeIf(String::isNotBlank) ?: "Ejecuta el movimiento con control y mantén una postura estable.", color = WildforceThemeTokens.textSecondary, modifier = Modifier.padding(vertical = 12.dp))
                if (!exercise.imageKey.isNullOrBlank()) RemoteTrainingImage(
                    url = exerciseTutorialImageUrl(exercise.imageKey), contentDescription = "Tutorial de ${exercise.name}",
                    modifier = Modifier.fillMaxWidth().height(230.dp).clip(RoundedCornerShape(24.dp)),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                )
                val instructions = guide?.instructions?.takeIf { it.isNotEmpty() } ?: listOf("Coloca el cuerpo en la posición inicial.", "Realiza el recorrido sin rebotes.", "Vuelve lentamente a la posición inicial.")
                if (instructions.isNotEmpty()) {
                    SectionTitle("PASOS")
                    instructions.forEachIndexed { index, instruction ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
                            Box(Modifier.size(24.dp).clip(CircleShape).background(WildforceThemeTokens.accentGold), contentAlignment = Alignment.Center) {
                                Text("${index + 1}", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.caption)
                            }
                            Text(instruction, Modifier.padding(start = 12.dp).weight(1f), color = WildforceThemeTokens.textPrimary)
                        }
                    }
                }
                if (!guide?.tips.isNullOrEmpty()) {
                    SectionTitle("CONSEJOS")
                    guide.tips.forEach { tip ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("●", color = WildforceThemeTokens.accentGold)
                            Text(tip, Modifier.weight(1f), color = WildforceThemeTokens.textSecondary)
                        }
                    }
                }
                Spacer(Modifier.height(30.dp))
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, Modifier.padding(top = 26.dp, bottom = 8.dp), fontFamily = AntonFontFamily, style = MaterialTheme.typography.h5, color = WildforceThemeTokens.textPrimary)
}
