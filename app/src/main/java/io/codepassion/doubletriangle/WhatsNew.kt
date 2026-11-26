package io.codepassion.doubletriangle

import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.codepassion.doubletriangle.core.designsystem.WildforceThemeTokens
import io.codepassion.wildforce.android.R

@Composable
internal fun WhatsNewDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = WildforceThemeTokens.backgroundSecondary) {
            Column(Modifier.fillMaxWidth().heightIn(max = 640.dp)) {
                Column(
                    Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(start = 24.dp, end = 24.dp, top = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(stringResource(R.string.whats_new_title), style = MaterialTheme.typography.h5, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(6.dp))
                    Text(stringResource(R.string.whats_new_subtitle), style = MaterialTheme.typography.body2, color = WildforceThemeTokens.textSecondary, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(24.dp))
                    NewsItem(Icons.Default.CalendarMonth, stringResource(R.string.whats_new_calendar_title), stringResource(R.string.whats_new_calendar_description), demoRes = R.drawable.whats_new_calendar_demo)
                    Spacer(Modifier.height(18.dp))
                    NewsItem(Icons.Default.FitnessCenter, stringResource(R.string.whats_new_exercises_title), stringResource(R.string.whats_new_exercises_description))
                    Spacer(Modifier.height(18.dp))
                    NewsItem(Icons.Default.Event, stringResource(R.string.whats_new_booking_title), stringResource(R.string.whats_new_booking_description))
                    Spacer(Modifier.height(18.dp))
                    NewsItem(Icons.Default.Apartment, stringResource(R.string.whats_new_gym_title), stringResource(R.string.whats_new_gym_description))
                    Spacer(Modifier.height(18.dp))
                    NewsItem(Icons.AutoMirrored.Filled.Notes, stringResource(R.string.whats_new_notes_title), stringResource(R.string.whats_new_notes_description))
                }
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 22.dp, bottom = 24.dp).heightIn(min = 52.dp),
                    shape = RoundedCornerShape(16.dp),
                ) { Text(stringResource(R.string.whats_new_continue)) }
            }
        }
    }
}

/** Recordings of real app flows, cropped to exclude account information. */
@Composable
private fun NewsDemo(@DrawableRes resource: Int) {
    val context = LocalContext.current
    val animation = remember(context, resource) {
        ImageDecoder.decodeDrawable(ImageDecoder.createSource(context.resources, resource)) as AnimatedImageDrawable
    }
    DisposableEffect(animation) { onDispose { animation.stop() } }
    AndroidView(
        factory = { ImageView(it).apply { scaleType = ImageView.ScaleType.FIT_CENTER; setImageDrawable(animation); animation.start() } },
        modifier = Modifier.width(150.dp).height(180.dp).clip(RoundedCornerShape(16.dp))
            .background(WildforceThemeTokens.accentGold.copy(alpha = .08f)),
    )
}

@Composable
private fun NewsItem(icon: ImageVector, title: String, description: String, @DrawableRes demoRes: Int? = null) {
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(icon, contentDescription = null, tint = WildforceThemeTokens.accentGold, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(14.dp))
            Column {
                Text(title, style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.Bold)
                Text(description, style = MaterialTheme.typography.body2, color = WildforceThemeTokens.textSecondary)
            }
        }
        if (demoRes != null && LocalContext.current.resources.configuration.locales[0].language == "es") {
            Spacer(Modifier.height(12.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { NewsDemo(demoRes) }
        }
    }
}
