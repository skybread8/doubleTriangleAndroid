package io.codepassion.doubletriangle.feature.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.codepassion.doubletriangle.core.model.RemoteSyncChangeTracker
import io.codepassion.doubletriangle.core.model.SyncDiagnostics
import java.time.Instant

@Composable
internal fun SyncDiagnosticsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val journal = remember(context) { context.getSharedPreferences(SyncDiagnostics.PREFS, 0) }
    val state = remember(context) { context.getSharedPreferences("wildforce_sync_state", 0) }
    var revision by remember { mutableIntStateOf(0) }
    DisposableEffect(journal, state) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> revision++ }
        journal.registerOnSharedPreferenceChangeListener(listener)
        state.registerOnSharedPreferenceChangeListener(listener)
        onDispose { journal.unregisterOnSharedPreferenceChangeListener(listener); state.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    val pending = remember(revision) { RemoteSyncChangeTracker.dirty(context).sorted() }
    val events = remember(revision) { SyncDiagnostics.entries(context) }
    val header = stringResource(R.string.sync_diagnostics_pending)
    val none = stringResource(R.string.sync_diagnostics_none)
    val status = buildString {
        append(header).append(": ").append(if (pending.isEmpty()) none else pending.joinToString())
        pending.forEach { resource ->
            val at = RemoteSyncChangeTracker.updatedAtMillis(context, resource)?.let { Instant.ofEpochMilli(it).toString() } ?: "?"
            append("\n  ").append(resource).append(" · ").append(at)
        }
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f).padding(12.dp), shape = MaterialTheme.shapes.medium, color = MaterialTheme.colors.surface) {
            Column(Modifier.fillMaxSize().padding(16.dp)) {
                Text(stringResource(R.string.sync_diagnostics_title), style = MaterialTheme.typography.h6)
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.sync_diagnostics_info), style = MaterialTheme.typography.caption)
                Spacer(Modifier.height(8.dp))
                SelectionContainer { Text(status, style = MaterialTheme.typography.body2) }
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                    items(events) { event -> SelectionContainer { Text(event, style = MaterialTheme.typography.caption, modifier = Modifier.padding(vertical = 4.dp)) }; Divider() }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { clipboard.setText(AnnotatedString(status + "\n\n" + events.joinToString("\n"))) }) { Text(stringResource(R.string.sync_diagnostics_copy)) }
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.sync_diagnostics_close)) }
                }
            }
        }
    }
}
