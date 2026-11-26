package io.codepassion.doubletriangle

import android.content.Context
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.codepassion.doubletriangle.core.designsystem.WildforceThemeTokens
import io.codepassion.wildforce.android.R

// Update this only when the curated content below changes for a new release.
private const val WHATS_NEW_REVISION = 1
private const val PREFS = "wildforce_whats_new"
private const val SEEN_REVISION = "seen_revision"

@Composable
internal fun WhatsNewOnLaunch() {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    var visible by remember { mutableStateOf(preferences.getInt(SEEN_REVISION, 0) < WHATS_NEW_REVISION) }
    if (visible) {
        val dismiss = {
            preferences.edit().putInt(SEEN_REVISION, WHATS_NEW_REVISION).apply()
            visible = false
        }
        WhatsNewDialog(onDismiss = dismiss)
    }
}

@Composable
private fun WhatsNewDialog(onDismiss: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "calendar")
    val scale by transition.animateFloat(
        initialValue = .94f, targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "calendar scale",
    )
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = WildforceThemeTokens.backgroundSecondary) {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 620.dp).verticalScroll(rememberScrollState()).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier.size(92.dp).background(WildforceThemeTokens.accentGold.copy(alpha = .13f), RoundedCornerShape(28.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = WildforceThemeTokens.accentGold, modifier = Modifier.size(50.dp).scale(scale))
                }
                Spacer(Modifier.height(18.dp))
                Text(stringResource(R.string.whats_new_title), style = MaterialTheme.typography.h5, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.whats_new_subtitle), style = MaterialTheme.typography.body2, color = WildforceThemeTokens.textSecondary, textAlign = TextAlign.Center)
                Spacer(Modifier.height(24.dp))
                NewsItem(Icons.Default.CalendarMonth, stringResource(R.string.whats_new_calendar_title), stringResource(R.string.whats_new_calendar_description))
                Spacer(Modifier.height(18.dp))
                NewsItem(Icons.Default.FitnessCenter, stringResource(R.string.whats_new_exercises_title), stringResource(R.string.whats_new_exercises_description))
                Spacer(Modifier.height(26.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(16.dp)) {
                    Text(stringResource(R.string.whats_new_continue))
                }
            }
        }
    }
}

@Composable
private fun NewsItem(icon: ImageVector, title: String, description: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, contentDescription = null, tint = WildforceThemeTokens.accentGold, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.Bold)
            Text(description, style = MaterialTheme.typography.body2, color = WildforceThemeTokens.textSecondary)
        }
    }
}
