package io.codepassion.doubletriangle.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** The time choices used by the booking and calendar export flows. */
@Composable
fun <T> TimeChoiceGrid(options: List<T>, selected: T?, label: (T) -> String, detail: (T) -> String? = { null }, detailColor: @Composable (T) -> Color = { WildforceThemeTokens.textSecondary }, onSelect: (T) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columns = ((maxWidth.value + 8f) / 108f).toInt().coerceAtLeast(1)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.chunked(columns).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { option ->
                        val isSelected = option == selected
                        val foreground = if (isSelected) WildforceThemeTokens.primaryButtonText else WildforceThemeTokens.textPrimary
                        Column(Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) WildforceThemeTokens.textPrimary else WildforceThemeTokens.backgroundSecondary)
                            .semantics { this.selected = isSelected }.clickable { onSelect(option) }
                            .padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(label(option), fontWeight = FontWeight.SemiBold, color = foreground)
                            detail(option)?.let { Text(it, fontSize = 11.sp, color = if (isSelected) foreground.copy(alpha = .7f) else detailColor(option)) }
                        }
                    }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}
