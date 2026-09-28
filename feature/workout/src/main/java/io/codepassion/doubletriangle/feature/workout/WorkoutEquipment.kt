package io.codepassion.doubletriangle.feature.workout

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.codepassion.doubletriangle.core.designsystem.Exo2FontFamily
import io.codepassion.doubletriangle.core.designsystem.WildforceThemeTokens
import io.codepassion.doubletriangle.core.model.WorkoutDaySummary
import io.codepassion.doubletriangle.core.model.displayBlocks

@Composable
internal fun WorkoutEquipment(workout: WorkoutDaySummary, modifier: Modifier = Modifier) {
    val equipment = workout.displayBlocks()
        .flatMap { it.exercises }
        .flatMap { ExerciseVisualCatalog.equipmentVisualsFor(it.imageKey) }
        .distinct()
    if (equipment.isEmpty()) return

    Column(modifier) {
        Text(
            "EQUIPAMIENTO NECESARIO",
            color = WildforceThemeTokens.textSecondary,
            style = MaterialTheme.typography.caption,
            fontWeight = FontWeight.Bold,
        )
        Row(
            Modifier.fillMaxWidth().padding(top = 10.dp).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            equipment.forEach { item ->
                Column(
                    Modifier.width(100.dp).semantics { contentDescription = item.label },
                ) {
                    Image(
                        painter = painterResource(item.drawable),
                        contentDescription = null,
                        modifier = Modifier.size(80.dp)
                            .background(Color.White, RoundedCornerShape(16.dp))
                            .padding(8.dp),
                    )
                    Text(
                        item.label,
                        Modifier.padding(start = 4.dp, top = 4.dp).height(40.dp),
                        color = WildforceThemeTokens.textPrimary,
                        fontFamily = Exo2FontFamily,
                        style = MaterialTheme.typography.caption,
                        textAlign = TextAlign.Start,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
