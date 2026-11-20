package io.codepassion.doubletriangle.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import io.codepassion.doubletriangle.core.designsystem.Exo2FontFamily
import io.codepassion.doubletriangle.core.designsystem.WildforceThemeTokens

@Composable
internal fun FrequentlyAskedQuestions() {
    val questions = stringArrayResource(R.array.faq_questions)
    val answers = stringArrayResource(R.array.faq_answers)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            stringResource(R.string.faq_intro),
            style = MaterialTheme.typography.body2,
            color = WildforceThemeTokens.textSecondary,
        )
        FaqCategory.entries.forEach { category ->
            Text(
                stringResource(category.title).uppercase(),
                fontFamily = Exo2FontFamily,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = WildforceThemeTokens.accent,
                modifier = Modifier.padding(top = 8.dp),
            )
            category.indices.forEach { index -> FaqRow(FaqEntry(questions[index], answers[index])) }
        }
        Text(
            stringResource(R.string.faq_help),
            style = MaterialTheme.typography.body2,
            color = WildforceThemeTokens.textSecondary,
            modifier = Modifier.padding(top = 12.dp, bottom = 20.dp),
        )
    }
}

@Composable
private fun FaqRow(entry: FaqEntry) {
    var expanded by remember(entry.question) { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(WildforceThemeTokens.backgroundSecondary)
            .clickable { expanded = !expanded }
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                entry.question,
                modifier = Modifier.weight(1f),
                fontFamily = Exo2FontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = WildforceThemeTokens.textPrimary,
            )
            Text(if (expanded) "−" else "+", color = WildforceThemeTokens.accent, fontSize = 24.sp)
        }
        if (expanded) {
            Text(entry.answer, style = MaterialTheme.typography.body2, color = WildforceThemeTokens.textSecondary)
        }
    }
}

private data class FaqEntry(val question: String, val answer: String)

private enum class FaqCategory(val title: Int, val indices: IntRange) {
    GettingStarted(R.string.faq_getting_started, 0..3),
    Training(R.string.faq_training, 4..8),
    Nutrition(R.string.faq_nutrition, 9..12),
    Devices(R.string.faq_devices, 13..17),
    Account(R.string.faq_account, 18..22),
}
