package io.codepassion.doubletriangle

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.codepassion.doubletriangle.core.designsystem.WildforceThemeTokens
import io.codepassion.doubletriangle.core.designsystem.R as DesignR

@Composable
internal fun CheckInCoachAvatar(name: String?, size: Int = 40) {
    val initials = name?.trim()?.split(Regex("\\s+"))?.filter(String::isNotBlank)?.take(2)?.map { it.first().uppercaseChar() }?.joinToString("").orEmpty()
    Box(Modifier.size(size.dp).clip(CircleShape).background(WildforceThemeTokens.accent), contentAlignment = Alignment.Center) {
        if (initials.isNotEmpty()) Text(initials, color = WildforceThemeTokens.primaryButtonText, fontSize = (size * .38f).sp, fontWeight = FontWeight.Bold)
        else Icon(Icons.Default.Person, null, Modifier.size((size * .46f).dp), tint = WildforceThemeTokens.primaryButtonText)
    }
}

@Composable
internal fun CheckInOption(title: String, selected: Boolean, onClick: () -> Unit, icon: ImageVector? = null, multiple: Boolean = false) {
    val color = if (selected) WildforceThemeTokens.primaryButtonText else WildforceThemeTokens.textPrimary
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
        .background(if (selected) WildforceThemeTokens.accent else WildforceThemeTokens.backgroundSecondary)
        .selectable(selected = selected, role = if (multiple) Role.Checkbox else Role.RadioButton, onClick = onClick).defaultMinSize(minHeight = 56.dp).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        icon?.let { Icon(it, null, Modifier.size(24.dp), tint = color) }
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.body1, fontWeight = FontWeight.Bold, color = color)
        Icon(if (multiple) { if (selected) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank } else { if (selected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked }, null, Modifier.size(22.dp), tint = if (selected) color else WildforceThemeTokens.textSecondary.copy(alpha = .55f))
    }
}

@Composable
internal fun CheckInSegments(count: Int, current: Int, answered: Set<Int>) {
    val label = stringResource(DesignR.string.question__d_of__d, current, count)
    Row(Modifier.fillMaxWidth().semantics { contentDescription = label }, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(count) { index ->
            val color = when { index + 1 == current -> WildforceThemeTokens.accent; index in answered -> WildforceThemeTokens.accent.copy(alpha = .45f); else -> WildforceThemeTokens.textPrimary.copy(alpha = .12f) }
            Box(Modifier.weight(1f).height(6.dp).clip(CircleShape).background(color))
        }
    }
}

@Composable
internal fun CheckInSentCelebration(message: String) {
    val transition = rememberInfiniteTransition(label = "check-in-confetti")
    val phase by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(3600, easing = LinearEasing), RepeatMode.Restart), label = "check-in-confetti-phase")
    val accent = WildforceThemeTokens.accent
    val minHeight = (LocalConfiguration.current.screenHeightDp * .52f).dp
    Box(Modifier.fillMaxWidth().heightIn(min = minHeight), contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val colors = listOf(accent, Color(0xFF4CAF50), Color(0xFFFFC83D), Color(0xFFF28A29))
            repeat(28) { index ->
                val progress = (phase + index * .071f) % 1f
                val x = ((index * .618034f) % 1f) * size.width
                val y = progress * size.height
                val alpha = (1f - kotlin.math.abs(progress * 2f - 1f)).coerceIn(0f, 1f) * .75f
                drawCircle(colors[index % colors.size].copy(alpha = alpha), radius = (if (index % 3 == 0) 3.5f else 2.5f).dp.toPx(), center = androidx.compose.ui.geometry.Offset(x, y))
            }
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Icon(Icons.Default.Verified, null, Modifier.size(88.dp), tint = WildforceThemeTokens.positiveText)
            Text(stringResource(DesignR.string.sent_).uppercase(), style = MaterialTheme.typography.h1, color = WildforceThemeTokens.textPrimary)
            Text(message, style = MaterialTheme.typography.body1, color = WildforceThemeTokens.textSecondary, textAlign = TextAlign.Center)
        }
    }
}

@Composable
internal fun CheckInHomeCard(card: CheckInCard, title: String, coach: String?, minutes: Int, onOpen: () -> Unit) {
    val largeText = LocalConfiguration.current.fontScale >= 1.3f
    Surface(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), shape = RoundedCornerShape(28.dp), color = WildforceThemeTokens.backgroundSecondary, elevation = 3.dp) {
        when (card.kind) {
            CheckInCardKind.AwaitingReview -> Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Verified, null, Modifier.size(30.dp), tint = WildforceThemeTokens.positiveText)
                Column { Text(stringResource(DesignR.string.check_in_sent), style = MaterialTheme.typography.subtitle1, color = WildforceThemeTokens.textPrimary); Text(stringResource(DesignR.string.ui_sheet_your_coach_will_review_it_soon_), style = MaterialTheme.typography.body2, color = WildforceThemeTokens.textSecondary) }
            }
            CheckInCardKind.Replied -> Column(Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    CheckInCoachAvatar(coach)
                    Column(Modifier.weight(1f)) {
                        Text(if (coach == null) stringResource(DesignR.string.new_message_from_your_coach) else stringResource(DesignR.string.new_message_from___, coach), style = MaterialTheme.typography.subtitle1, color = WildforceThemeTokens.textPrimary)
                        Text(stringResource(DesignR.string.about_your___, title), style = MaterialTheme.typography.body2, color = WildforceThemeTokens.textSecondary)
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = WildforceThemeTokens.textSecondary)
                }
                Text(card.record.coachReply.orEmpty(), style = MaterialTheme.typography.body1, color = WildforceThemeTokens.textPrimary, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            else -> Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Check-in", style = MaterialTheme.typography.body2, color = WildforceThemeTokens.textSecondary)
                    if (card.otherOpenCount > 0) Text(stringResource(DesignR.string.__d_more, card.otherOpenCount), Modifier.padding(start = 8.dp).clip(CircleShape).background(WildforceThemeTokens.surfaceSubtle).padding(horizontal = 8.dp, vertical = 3.dp), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
                    Spacer(Modifier.weight(1f))
                    Icon(if (card.kind == CheckInCardKind.Late) Icons.Default.Error else Icons.Default.Schedule, null, Modifier.size(15.dp), tint = if (card.kind == CheckInCardKind.Late) WildforceThemeTokens.accentGold else WildforceThemeTokens.textSecondary)
                    Spacer(Modifier.width(4.dp))
                    Text(if (largeText) "~" + stringResource(DesignR.string._d_min, minutes) else stringResource(DesignR.string._d_questions___about__d_min, card.record.total, minutes), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(title, style = MaterialTheme.typography.h6.copy(fontSize = 24.sp), color = WildforceThemeTokens.textPrimary)
                val coachLine: @Composable () -> Unit = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CheckInCoachAvatar(coach, 28)
                        Text(if (coach == null) stringResource(DesignR.string.from_your_coach) else stringResource(DesignR.string.from___, coach), style = MaterialTheme.typography.body2, color = WildforceThemeTokens.textSecondary, maxLines = 2)
                    }
                }
                val action: @Composable () -> Unit = {
                    Surface(shape = RoundedCornerShape(10.dp), color = WildforceThemeTokens.accent, modifier = Modifier.clickable(onClick = onOpen)) {
                        Text((if (card.kind == CheckInCardKind.Available) stringResource(DesignR.string.start) else stringResource(DesignR.string.ui_sheet_key_continue)) + "  →", Modifier.padding(horizontal = 16.dp, vertical = 10.dp), color = WildforceThemeTokens.primaryButtonText, style = MaterialTheme.typography.button)
                    }
                }
                if (largeText) Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { coachLine(); action() }
                else Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { Box(Modifier.weight(1f)) { coachLine() }; action() }
                if (card.kind == CheckInCardKind.InProgress || card.kind == CheckInCardKind.Late) {
                    LinearProgressIndicator(progress = card.record.answered.toFloat() / card.record.total.coerceAtLeast(1), modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape), color = WildforceThemeTokens.textPrimary, backgroundColor = WildforceThemeTokens.textPrimary.copy(alpha = .12f))
                    Text(if (card.kind == CheckInCardKind.Late) stringResource(DesignR.string.it_s_a_little_late__but_you_can_still_send_it_) else stringResource(DesignR.string._d_of__d_answered__pick_up_where_you_left_off_, card.record.answered, card.record.total), Modifier.align(Alignment.End), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
                }
            }
        }
    }
}
