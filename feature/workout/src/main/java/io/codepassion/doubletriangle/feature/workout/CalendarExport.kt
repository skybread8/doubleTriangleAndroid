package io.codepassion.doubletriangle.feature.workout

import android.app.TimePickerDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.content.ActivityNotFoundException
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import io.codepassion.doubletriangle.core.designsystem.WildforceThemeTokens
import io.codepassion.doubletriangle.core.model.WorkoutDaySummary
import io.codepassion.doubletriangle.core.model.WorkoutHubState
import io.codepassion.doubletriangle.core.model.WorkoutStatus
import io.codepassion.doubletriangle.core.model.isPlanFinalized
import java.io.File
import java.security.MessageDigest
import java.time.*
import java.time.format.DateTimeFormatter
import kotlin.math.*

internal fun calendarDate(raw: String?): LocalDate? = raw?.let {
    runCatching { LocalDate.parse(it.take(10)) }.getOrNull()
}

/** Missing dates follow each session's preferred weekday in the plan's week. */
internal fun suggestedCalendarDates(plan: WorkoutHubState, today: LocalDate = LocalDate.now()): Map<String, LocalDate> {
    val anchor = calendarDate(plan.planStartsOn)
        ?: plan.workouts.firstNotNullOfOrNull { calendarDate(it.scheduledFor) }
        ?: today
    val monday = anchor.minusDays((anchor.dayOfWeek.value - 1).toLong())
    val occurrences = mutableMapOf<DayOfWeek, Int>()
    return plan.workouts.sortedBy { it.order }.associate { workout ->
        val occurrence = occurrences.getOrDefault(workout.scheduledDay, 0)
        occurrences[workout.scheduledDay] = occurrence + 1
        workout.id to (calendarDate(workout.scheduledFor)
            ?: monday.plusDays((workout.scheduledDay.value - 1 + 7L * occurrence)))
    }
}

internal fun suggestedWorkoutTime(workouts: List<WorkoutDaySummary>, zone: ZoneId = ZoneId.systemDefault()): LocalTime? {
    val times = workouts.asSequence().filter { it.status == WorkoutStatus.Completed }
        .distinctBy { it.id }.mapNotNull { it.startedAt?.let { raw -> runCatching { Instant.parse(raw) }.getOrNull() } }
        .sortedDescending().take(10).map { it.atZone(zone).toLocalTime() }.toList()
    if (times.isEmpty()) return null
    val angles = times.map { (it.hour * 60 + it.minute) * 2.0 * Math.PI / 1440 }
    val x = angles.sumOf { cos(it) }; val y = angles.sumOf { sin(it) }
    if (hypot(x, y) < 1e-6) return times.first().withSecond(0).withNano(0)
    val minutes = ((atan2(y, x) * 1440 / (2 * Math.PI)).roundToInt() + 1440) % 1440
    return LocalTime.of(minutes / 60, minutes % 60)
}

private fun icsEscape(value: String) = value.replace("\\", "\\\\").replace("\r\n", "\n").replace("\r", "\n").replace("\n", "\\n").replace(";", "\\;").replace(",", "\\,")
private fun foldIcs(line: String): String {
    val result = StringBuilder(); var bytes = 0
    line.codePoints().forEach { cp ->
        val part = String(Character.toChars(cp)); val size = part.toByteArray(Charsets.UTF_8).size
        if (bytes + size > 75) { result.append("\r\n "); bytes = 1 }
        result.append(part); bytes += size
    }
    return result.toString()
}
internal fun exportableWorkouts(plan: WorkoutHubState): List<WorkoutDaySummary> =
    plan.workouts.filterNot { it.status.isPlanFinalized }

internal fun calendarFile(plan: WorkoutHubState, time: LocalTime, zone: ZoneId, now: Instant = Instant.now(), dates: Map<String, LocalDate> = emptyMap()): String {
    val fmt = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC)
    val lines = mutableListOf("BEGIN:VCALENDAR", "VERSION:2.0", "PRODID:-//Wildforce//Workout Plan//EN", "CALSCALE:GREGORIAN", "METHOD:PUBLISH", "X-WR-CALNAME:${icsEscape(plan.planName)}")
    exportableWorkouts(plan).forEach { workout ->
        val date = requireNotNull(dates[workout.id] ?: calendarDate(workout.scheduledFor))
        val start = date.atTime(time).atZone(zone).toInstant()
        val uid = MessageDigest.getInstance("SHA-256").digest("${plan.planId.orEmpty()}:${workout.id}".toByteArray()).joinToString("") { "%02x".format(it.toInt() and 255) }
        lines += listOf("BEGIN:VEVENT", "UID:$uid@wildforce", "DTSTAMP:${fmt.format(now)}", "DTSTART:${fmt.format(start)}", "DTEND:${fmt.format(start.plusSeconds(workout.estimatedMinutes.coerceAtLeast(1) * 60L))}", "SUMMARY:${icsEscape(workout.title)}", "DESCRIPTION:${icsEscape(workout.focus)}", "END:VEVENT")
    }
    lines += "END:VCALENDAR"
    return lines.joinToString("\r\n", postfix = "\r\n", transform = ::foldIcs)
}

@Composable
internal fun CalendarExportScreen(state: WorkoutHubState, history: List<WorkoutHubState>, customWorkouts: List<WorkoutDaySummary>, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val suggested = remember(state.workouts, history, customWorkouts) { suggestedWorkoutTime(state.workouts + history.flatMap { it.workouts } + customWorkouts) }
    var time by remember { mutableStateOf(suggested ?: LocalTime.of(9, 0)) }
    var error by remember { mutableStateOf(false) }
    var dates by remember(state.planId, state.workouts, state.planStartsOn) { mutableStateOf(suggestedCalendarDates(state)) }
    val pending = remember(state.workouts) { exportableWorkouts(state) }
    var showDates by remember { mutableStateOf(false) }
    val noAppMessage = stringResource(R.string.calendar_no_app)
    val shareTitle = stringResource(R.string.calendar_share_title)
    var errorMessage by remember { mutableStateOf<String?>(null) }
    fun export(open: Boolean) {
        try {
            val dir = File(context.cacheDir, "calendar_exports").apply { mkdirs() }
            val file = File.createTempFile("wildforce-plan-", ".ics", dir)
            file.writeText(calendarFile(state, time, ZoneId.systemDefault(), dates = dates), Charsets.UTF_8)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = if (open) {
                Intent(Intent.ACTION_VIEW).setDataAndType(uri, "text/calendar")
            } else {
                Intent(Intent.ACTION_SEND).setType("text/calendar").putExtra(Intent.EXTRA_STREAM, uri)
            }.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            context.startActivity(Intent.createChooser(intent, if (open) context.getString(R.string.calendar_choose_app) else shareTitle))
            onDismiss()
        } catch (_: ActivityNotFoundException) {
            errorMessage = noAppMessage
        } catch (_: Exception) {
            error = true
        }
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Box(Modifier.fillMaxSize().clickable(onClick = onDismiss))
            Surface(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = WildforceThemeTokens.background,
            ) {
                Column(
                    Modifier.fillMaxWidth().navigationBarsPadding().heightIn(max = 600.dp)
                        .verticalScroll(rememberScrollState()).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = onDismiss) { Text(stringResource(R.string.calendar_cancel)) }
                        Text(stringResource(R.string.calendar_export), fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textPrimary, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                        Spacer(Modifier.width(64.dp))
                    }
                    if (pending.isEmpty()) {
                        Text(stringResource(R.string.calendar_no_pending), color = WildforceThemeTokens.textSecondary)
                    } else {
                        Card(Modifier.fillMaxWidth(), backgroundColor = WildforceThemeTokens.backgroundSecondary, shape = RoundedCornerShape(14.dp)) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(stringResource(R.string.calendar_pending_count, pending.size), color = WildforceThemeTokens.textPrimary, fontWeight = FontWeight.SemiBold)
                                Text(stringResource(if (suggested == null) R.string.calendar_default_time else R.string.calendar_suggested_time), color = WildforceThemeTokens.textSecondary, fontSize = 13.sp)
                                TextButton(onClick = { TimePickerDialog(context, { _, h, m -> time = LocalTime.of(h, m) }, time.hour, time.minute, true).show() }) {
                                    Text(stringResource(R.string.calendar_other_time, time.format(DateTimeFormatter.ofPattern("HH:mm"))))
                                }
                                TextButton(onClick = { showDates = !showDates }) { Text(stringResource(R.string.calendar_review_dates)) }
                                if (showDates) {
                                    Text(stringResource(if (calendarDate(state.planStartsOn) == null && state.workouts.none { calendarDate(it.scheduledFor) != null }) R.string.calendar_dates_current_week else R.string.calendar_dates_suggested), color = WildforceThemeTokens.textSecondary, fontSize = 13.sp)
                                    pending.sortedBy { it.order }.forEach { workout ->
                                        val date = dates.getValue(workout.id)
                                        TextButton(onClick = {
                                            DatePickerDialog(context, { _, y, m, d -> dates = dates + (workout.id to LocalDate.of(y, m + 1, d)) }, date.year, date.monthValue - 1, date.dayOfMonth).show()
                                        }) { Text("${workout.title}: ${date.format(DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM))}") }
                                    }
                                }
                            }
                        }
                        errorMessage?.let { Text(it, color = MaterialTheme.colors.error, fontSize = 13.sp) }
                        if (error) Text(stringResource(R.string.calendar_export_error), color = MaterialTheme.colors.error, fontSize = 13.sp)
                        Button(onClick = { export(true) }, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary, contentColor = WildforceThemeTokens.background)) {
                            Text(stringResource(R.string.calendar_open).uppercase(), fontWeight = FontWeight.Bold)
                        }
                        TextButton(onClick = { export(false) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.calendar_share)) }
                    }
                }
            }
        }
    }
}
