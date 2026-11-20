package io.codepassion.doubletriangle

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import io.codepassion.doubletriangle.feature.onboarding.CheckInGuidedPhotoCapture
import io.codepassion.doubletriangle.core.designsystem.WildforceThemeTokens
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.io.File

private fun JSONArray.objects(): List<JSONObject> = (0 until length()).mapNotNull(::optJSONObject)
private fun JSONArray.strings(): List<String> = (0 until length()).mapNotNull { optString(it).takeIf(String::isNotBlank) }
private fun checkInEstimatedMinutes(questions: List<JSONObject>): Int {
    val seconds = questions.sumOf { item ->
        val q = item.question()
        when (q.type) { "yes_no", "scale" -> 5; "single_choice", "multi_choice", "number" -> 10; "bodyweight", "pain_map", "text" -> 20; "long_text" -> 45; "measurements" -> 30 * q.sites.size.coerceAtLeast(1); "progress_photos" -> 60; else -> 0 }
    }
    return ((seconds + 59) / 60).coerceAtLeast(1)
}
private fun JSONObject.question(): CheckInQuestion {
    val config = optJSONObject("config") ?: JSONObject()
    return CheckInQuestion(optString("id"), optString("type"), optBoolean("required", true), config.optInt("min", 0), config.optInt("max", 10), config.optJSONArray("options")?.strings().orEmpty(), config.optJSONArray("sites")?.strings().orEmpty())
}
private fun JSONObject.toAnswer(q: CheckInQuestion): CheckInAnswer? {
    val v = optJSONObject(q.id)?.opt("value") ?: return null
    return when (q.type) {
        "text", "long_text" -> (v as? String)?.let(CheckInAnswer::Text)
        "number" -> (v as? Number)?.toDouble()?.let(CheckInAnswer::Number)
        "scale" -> (v as? Number)?.toInt()?.let(CheckInAnswer::Scale)
        "single_choice" -> (v as? String)?.let(CheckInAnswer::Choice)
        "multi_choice" -> (v as? JSONArray)?.strings()?.let(CheckInAnswer::Choices)
        "yes_no" -> (v as? Boolean)?.let(CheckInAnswer::YesNo)
        "pain_map" -> (v as? JSONArray)?.objects()?.map { PainEntry(it.optString("zone"), it.optInt("intensity")) }?.let(CheckInAnswer::Pain)
        else -> null
    }
}

@Composable
private fun CheckInPhotoThumbnail(context: Context, session: JSONObject, angle: String, description: String, modifier: Modifier = Modifier) {
    val local = session.optString(angle).takeIf { File(it).isFile }
        ?: File(context.filesDir, "body_progress_photos/${session.optString("id")}_${angle}.jpg").takeIf(File::isFile)?.absolutePath
    val lastModified = local?.let { File(it).lastModified() } ?: 0L
    val bitmap = remember(local, lastModified) {
        local?.let { path -> BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = 4 }) }
    }
    DisposableEffect(bitmap) { onDispose { bitmap?.recycle() } }
    Box(modifier.aspectRatio(0.75f).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colors.surface)) {
        bitmap?.let { Image(it.asImageBitmap(), contentDescription = description, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
    }
}

@Composable
private fun CheckInDialogSystemBars(lightBackground: Boolean) {
    val view = LocalView.current
    SideEffect {
        (view.parent as? DialogWindowProvider)?.window?.let { window ->
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = lightBackground
                isAppearanceLightNavigationBars = lightBackground
            }
        }
    }
}

/** Coach check-in entry point. Remote records remain the source of form definitions. */
@Composable
internal fun CheckInPanel(userId: String, imperial: Boolean, synchronize: suspend () -> Unit, content: @Composable ((@Composable () -> Unit)?) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var revision by remember { mutableIntStateOf(0) }
    var opened by remember { mutableStateOf<String?>(null) }
    var readingReply by remember { mutableStateOf(false) }
    val state = remember(context) { context.getSharedPreferences("wildforce_sync_state", Context.MODE_PRIVATE) }
    val draftPrefs = remember(context) { context.getSharedPreferences("wildforce_check_in_drafts", Context.MODE_PRIVATE) }
    val pushPrefs = remember(context) { context.getSharedPreferences("wildforce_check_in_push", Context.MODE_PRIVATE) }
    val coachPrefs = remember(context) { context.getSharedPreferences("wildforce_remote_sync", Context.MODE_PRIVATE) }
    DisposableEffect(state, draftPrefs, pushPrefs, coachPrefs) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> revision++ }
        state.registerOnSharedPreferenceChangeListener(listener); draftPrefs.registerOnSharedPreferenceChangeListener(listener)
        pushPrefs.registerOnSharedPreferenceChangeListener(listener); coachPrefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { state.unregisterOnSharedPreferenceChangeListener(listener); draftPrefs.unregisterOnSharedPreferenceChangeListener(listener); pushPrefs.unregisterOnSharedPreferenceChangeListener(listener); coachPrefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    val records = remember(userId, revision) { CheckInDraftStore.projected(context, userId).objects() }
    val coaches = remember(revision) { runCatching { JSONArray(coachPrefs.getString("coach_summaries", "[]") ?: "[]").objects() }.getOrDefault(emptyList()) }
    fun coachName(record: JSONObject): String? = coaches.firstOrNull { it.optString("id").equals(record.optString("coach_user_id"), true) }
        ?.optString("name")?.takeIf { it.isNotBlank() && it != "null" }
    val readPrefs = remember(context) { context.getSharedPreferences("wildforce_check_in_read", Context.MODE_PRIVATE) }
    val read = readPrefs.getStringSet(userId, emptySet()).orEmpty()
    val pushedId = pushPrefs.getString("id", null)
    val pushedType = pushPrefs.getString("type", null)
    var pushAttempt by remember { mutableIntStateOf(0) }
    var pushLoading by remember { mutableStateOf(false) }
    var pushIssue by remember { mutableStateOf<Boolean?>(null) } // true: removed; false: sync failed
    LaunchedEffect(pushedId, pushedType, userId, pushAttempt) {
        if (pushedId != null && pushedType != null) {
            fun findRecord(): JSONObject? = CheckInDraftStore.projected(context, userId).objects().firstOrNull {
                    it.optString("id").equals(pushedId, true) && it.optString("user_id").equals(userId, true) && it.isNull("deleted_at")
            }
            fun openRecord(record: JSONObject) {
                readingReply = pushedType == "check_in_reviewed" && record.optString("status") == "submitted"
                opened = record.optString("id")
                pushPrefs.edit().remove("id").remove("type").apply()
                revision++
            }
            val cached = findRecord()
            if (cached != null && (pushedType != "check_in_reviewed" || !cached.isNull("reviewed_at"))) {
                openRecord(cached)
            } else {
                pushLoading = true
                pushIssue = null
                try {
                    synchronize()
                    val record = findRecord()
                    if (record == null) pushIssue = true else openRecord(record)
                } catch (error: kotlinx.coroutines.CancellationException) {
                    throw error
                } catch (_: Exception) {
                    pushIssue = false
                } finally {
                    pushLoading = false
                }
            }
        }
    }
    if (pushLoading) Dialog(onDismissRequest = {}) { Surface { Box(Modifier.padding(28.dp)) { CircularProgressIndicator() } } }
    pushIssue?.let { missing ->
        fun closeIssue() { pushIssue = null; pushPrefs.edit().remove("id").remove("type").apply() }
        AlertDialog(onDismissRequest = ::closeIssue,
            title = { Text(androidx.compose.ui.res.stringResource(if (missing) io.codepassion.doubletriangle.core.designsystem.R.string.ui_sheet_not_available else io.codepassion.doubletriangle.core.designsystem.R.string.ui_sheet_something_went_wrong)) },
            confirmButton = { if (!missing) TextButton(onClick = { pushIssue = null; pushAttempt++ }) { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.ui_sheet_retry)) } },
            dismissButton = { TextButton(onClick = ::closeIssue) { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.ui_sheet_close)) } })
    }
    val mapped = records.mapNotNull { row ->
        val due = runCatching { LocalDate.parse(row.optString("due_on")) }.getOrNull() ?: return@mapNotNull null
        fun timestamp(name: String) = row.optString(name).takeUnless { it.isBlank() || it == "null" }?.let { runCatching { Instant.parse(it) }.getOrNull() }
        val questions = row.optJSONArray("questions")?.objects().orEmpty().map { it.question() }.filter { it.type in checkInQuestionTypes }
        val answers = row.optJSONObject("answers") ?: JSONObject()
        val metrics = CheckInAssets.metrics(context, userId, row.optString("id")).map { it.optString("type") }.toSet()
        val photos = CheckInAssets.photos(context, userId).firstOrNull { it.optString("check_in_id") == row.optString("id") }
        val hasPhotos = photos != null && listOf("front", "profile", "back").all { photos.optString(it).isNotBlank() }
        val answered = questions.count { q -> when (q.type) { "bodyweight" -> "weight" in metrics; "measurements" -> q.sites.isNotEmpty() && metrics.containsAll(q.sites); "progress_photos" -> hasPhotos; else -> answers.toAnswer(q)?.let { validCheckInAnswer(q, it) } == true } }
        CheckInCardRecord(row.optString("id"), row.optString("user_id"), due, row.optString("status"), !row.isNull("deleted_at"), !row.isNull("missed_at"), timestamp("submitted_at"), timestamp("reviewed_at"), row.optString("coach_reply").takeUnless { it == "null" }, answered, questions.size)
    }
    val card = resolveCheckInCard(mapped, userId, read)
    val cardContent: @Composable () -> Unit = {
        card?.let {
            val record = records.firstOrNull { row -> row.optString("id") == it.record.id }
            val coach = record?.let(::coachName)
            val minutes = checkInEstimatedMinutes(record?.optJSONArray("questions")?.objects().orEmpty())
            CheckInHomeCard(it, record?.optString("title") ?: "Check-in", coach, minutes) {
                readingReply = it.kind == CheckInCardKind.Replied; opened = it.record.id
            }
        }
    }
    content(if (card == null) null else cardContent)
    opened?.let { id -> records.firstOrNull { it.optString("id") == id }?.let { record ->
        if (readingReply) {
            Dialog(onDismissRequest = { opened = null }, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
                CheckInDialogSystemBars(lightBackground = MaterialTheme.colors.isLight)
                Surface(Modifier.fillMaxSize(), color = WildforceThemeTokens.background) {
                    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Spacer(Modifier.width(64.dp))
                            Text(coachName(record) ?: androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.your_coach), Modifier.weight(1f), style = MaterialTheme.typography.subtitle1, color = WildforceThemeTokens.textPrimary, textAlign = TextAlign.Center)
                            TextButton(onClick = { opened = null }, modifier = Modifier.widthIn(min = 64.dp)) { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.done), color = WildforceThemeTokens.accent) }
                        }
                        Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            record.optString("reviewed_at").takeUnless { it.isBlank() || it == "null" }?.let { raw ->
                                val date = remember(raw) { runCatching { java.time.format.DateTimeFormatter.ofLocalizedDateTime(java.time.format.FormatStyle.MEDIUM, java.time.format.FormatStyle.SHORT).withLocale(java.util.Locale.getDefault()).withZone(java.time.ZoneId.systemDefault()).format(Instant.parse(raw)) }.getOrNull() }
                                if (date != null) Text(date, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
                            }
                            Text(record.optString("coach_reply").takeUnless { it == "null" }.orEmpty(), style = MaterialTheme.typography.body1, color = WildforceThemeTokens.textPrimary)
                        }
                    }
                }
            }
            LaunchedEffect(id) { readPrefs.edit().putStringSet(userId, read + id).apply(); revision++ }
        } else CheckInForm(record, userId, imperial, coachName(record), remoteRevision = revision, onClose = { opened = null; revision++; scope.launch { runCatching { synchronize() } } }, synchronize = synchronize)
    } }
}

@Composable
private fun CheckInForm(record: JSONObject, userId: String, imperial: Boolean, coachName: String?, remoteRevision: Int, onClose: () -> Unit, synchronize: suspend () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val painLabels = mapOf(
        "neckPain" to androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.neck_pain),
        "shoulderPain" to androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.shoulder_pain),
        "elbowPain" to androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.elbow_pain),
        "wristPain" to androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.wrist_pain),
        "lowerBackPain" to androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.lower_back_pain),
        "hipPain" to androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.hip_pain),
        "kneePain" to androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.knee_pain),
        "anklePain" to androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.ankle_pain),
    )
    val metricLabels = mapOf(
        "weight" to androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.ui_sheet_weight),
        "waistCircumference" to androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.feature.onboarding.R.string.body_metric_waist),
        "hipCircumference" to androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.feature.onboarding.R.string.body_metric_hip),
        "chestCircumference" to androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.chest),
        "thighCircumference" to androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.thigh),
        "armCircumference" to androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.arm),
    )
    val id = record.getString("id")
    val editableRecord = remember(id) { JSONObject(record.toString()) }
    val questions = remember(id) { record.optJSONArray("questions")?.objects().orEmpty().filter { it.question().type in checkInQuestionTypes } }
    var step by remember(id) {
        val saved = record.optJSONObject("answers") ?: JSONObject()
        val metricTypes = CheckInAssets.metrics(context, userId, id).map { it.optString("type") }.toSet()
        val photos = CheckInAssets.photos(context, userId).firstOrNull { it.optString("check_in_id") == id }
        val hasPhotos = photos != null && listOf("front", "profile", "back").all { photos.optString(it).isNotBlank() }
        val typed = questions.map { it.question() }.mapNotNull { q -> saved.toAnswer(q)?.let { q.id to it } }.toMap()
        val missing = questions.map { it.question() }.filterNot { isCheckInQuestionAnswered(it, typed, metricTypes, hasPhotos) }
        val started = record.optString("status") == "draft" || typed.isNotEmpty() || metricTypes.isNotEmpty() || hasPhotos
        val next = if (!started) 0 else missing.firstOrNull()?.let { question -> questions.indexOfFirst { it.optString("id") == question.id } + 1 } ?: (questions.size + 1)
        mutableIntStateOf(next)
    }
    var answersRaw by remember(id) { mutableStateOf((record.optJSONObject("answers") ?: JSONObject()).toString()) }
    var revision by remember(id) { mutableIntStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var sent by remember(id) { mutableStateOf(false) }
    var capturingPhotos by remember(id) { mutableStateOf(false) }
    fun setAnswer(q: CheckInQuestion, value: Any?) {
        val answers = JSONObject(answersRaw)
        if (value == null) answers.remove(q.id) else answers.put(q.id, JSONObject().put("value", value))
        answersRaw = answers.toString()
        CheckInDraftStore.save(context, userId, editableRecord, answers)
    }
    val answers = JSONObject(answersRaw)
    val metrics = remember(revision, remoteRevision) { CheckInAssets.metrics(context, userId, id) }
    val metricTypes = metrics.map { it.optString("type") }.toSet()
    val photo = remember(revision, remoteRevision) { CheckInAssets.photos(context, userId).firstOrNull { it.optString("check_in_id") == id } }
    val hasPhotos = photo != null && listOf("front", "profile", "back").all { photo.optString(it).isNotBlank() }
    val typed = questions.map { it.question() }.mapNotNull { q -> answers.toAnswer(q)?.let { q.id to it } }.toMap()
    val missing = missingRequiredCheckInQuestions(questions.map { it.question() }, typed, metricTypes, hasPhotos)
    val answeredIndices = questions.mapIndexedNotNull { index, row ->
        index.takeIf { isCheckInQuestionAnswered(row.question(), typed, metricTypes, hasPhotos) }
    }.toSet()
    Dialog(onDismissRequest = { if (!busy && !capturingPhotos) onClose() }, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        CheckInDialogSystemBars(lightBackground = MaterialTheme.colors.isLight)
        Surface(Modifier.fillMaxSize(), color = WildforceThemeTokens.background) {
            Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).imePadding()) {
                Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (!sent) {
                        IconButton(enabled = !busy, onClick = onClose, modifier = Modifier.size(44.dp)) { Icon(Icons.Default.Close, androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.ui_sheet_close), tint = WildforceThemeTokens.textPrimary) }
                        if (step in 1..questions.size) CheckInSegments(questions.size, step, answeredIndices)
                    }
                }
                Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    when {
                        sent -> CheckInSentCelebration(androidx.compose.ui.res.stringResource(if (io.codepassion.wildforce.android.BuildConfig.FCM_CONFIGURED) io.codepassion.doubletriangle.core.designsystem.R.string.your_coach_will_review_it_soon__you_ll_get_a_notification_when_they_reply_ else io.codepassion.doubletriangle.core.designsystem.R.string.ui_sheet_your_coach_will_review_it_soon_))
                        step == 0 -> {
                            Spacer(Modifier.height(40.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                CheckInCoachAvatar(coachName)
                                Text(if (coachName == null) androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.from_your_coach) else androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.from___, coachName), style = MaterialTheme.typography.body2, color = WildforceThemeTokens.textSecondary, fontWeight = FontWeight.Bold)
                            }
                            Text(record.optString("title").uppercase(), style = MaterialTheme.typography.h1, color = WildforceThemeTokens.textPrimary)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) { Icon(Icons.Default.FormatListBulleted, null, tint = WildforceThemeTokens.accent); Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string._d_questions, questions.size), color = WildforceThemeTokens.textPrimary) }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) { Icon(Icons.Default.Schedule, null, tint = WildforceThemeTokens.accent); Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.about__d_min, checkInEstimatedMinutes(questions)), color = WildforceThemeTokens.textPrimary) }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) { Icon(Icons.Default.History, null, tint = WildforceThemeTokens.accent); Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.your_answers_are_saved_as_you_go__so_you_can_finish_it_later_), color = WildforceThemeTokens.textPrimary) }
                        }
                        step > questions.size -> {
                            Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.ready_to_send_).uppercase(), style = MaterialTheme.typography.h2, color = WildforceThemeTokens.textPrimary)
                            if (missing.isNotEmpty()) Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string._d_required_questions_are_still_unanswered_, missing.size), color = WildforceThemeTokens.textSecondary) else Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.check_your_answers__tap_any_of_them_to_change_it_), color = WildforceThemeTokens.textSecondary)
                            questions.forEachIndexed { index, row ->
                                val question = row.question()
                                val summary = when (question.type) {
                                    "bodyweight" -> metrics.firstOrNull { it.optString("type") == "weight" }?.let { "%.1f %s".format(java.util.Locale.getDefault(), it.optDouble("value") * checkInMetricDisplayFactor("weight", imperial), checkInMetricUnit("weight", imperial)) }
                                    "measurements" -> question.sites.mapNotNull { site -> metrics.firstOrNull { it.optString("type") == site }?.let { "${metricLabels[site] ?: site}: " + "%.1f %s".format(java.util.Locale.getDefault(), it.optDouble("value") * checkInMetricDisplayFactor(site, imperial), checkInMetricUnit(site, imperial)) } }.joinToString().takeIf { it.isNotBlank() }
                                    "progress_photos" -> if (hasPhotos) androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.photos_taken) else null
                                    "pain_map" -> (answers.toAnswer(question) as? CheckInAnswer.Pain)?.value?.let { if (it.isEmpty()) androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.ui_sheet_nothing_hurts) else it.joinToString { pain -> "${painLabels[pain.zone] ?: pain.zone}: ${pain.intensity}/10" } }
                                    "yes_no" -> (answers.toAnswer(question) as? CheckInAnswer.YesNo)?.let { if (it.value) androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.ui_sheet_yes) else androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.ui_sheet_no) }
                                    "multi_choice" -> (answers.toAnswer(question) as? CheckInAnswer.Choices)?.value?.let { if (it.isEmpty()) androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.ui_sheet_none_of_these) else it.joinToString() }
                                    else -> answers.optJSONObject(question.id)?.opt("value")?.toString()
                                }
                                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(WildforceThemeTokens.backgroundSecondary).clickable { step = index + 1 }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) { Text(row.optString("label"), fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary); Text(summary ?: if (question.required) androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.required) else androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.not_answered), style = MaterialTheme.typography.body2, color = WildforceThemeTokens.textSecondary) }
                                    Icon(Icons.Default.ChevronRight, null, tint = WildforceThemeTokens.textSecondary)
                                }
                            }
                        }
                        else -> {
                            val raw = questions[step - 1]; val q = raw.question(); val current = answers.optJSONObject(q.id)?.opt("value")
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.question__d_of__d, step, questions.size).uppercase(), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                                if (!q.required) Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.optional), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
                            }
                            Text(raw.optString("label"), style = MaterialTheme.typography.h6.copy(fontSize = 22.sp), color = WildforceThemeTokens.textPrimary)
                            raw.optString("help").takeUnless { it.isBlank() || it == "null" }?.let { Text(it, color = WildforceThemeTokens.textSecondary, style = MaterialTheme.typography.body2) }
                            Spacer(Modifier.height(8.dp))
                            when (q.type) {
                                "text", "long_text", "number" -> {
                                    var input by remember(id, q.id) { mutableStateOf(current?.toString().orEmpty()) }
                                    val focus = remember(q.id) { FocusRequester() }
                                    LaunchedEffect(q.id) { focus.requestFocus() }
                                    TextField(value = input, onValueChange = { value ->
                                        input = value
                                        if (q.type == "number") setAnswer(q, value.replace(',', '.').toDoubleOrNull())
                                        else setAnswer(q, value.takeIf { it.isNotBlank() })
                                    }, modifier = Modifier.fillMaxWidth().focusRequester(focus), shape = RoundedCornerShape(14.dp),
                                        placeholder = { Text(if (q.type == "number") "0" else androidx.compose.ui.res.stringResource(if (q.type == "long_text") io.codepassion.doubletriangle.core.designsystem.R.string.write_as_much_as_you_like_ else io.codepassion.doubletriangle.core.designsystem.R.string.your_answer)) },
                                        keyboardOptions = KeyboardOptions(keyboardType = if (q.type == "number") KeyboardType.Decimal else KeyboardType.Text),
                                        minLines = if (q.type == "long_text") 5 else 1,
                                        textStyle = if (q.type == "number") MaterialTheme.typography.h3.copy(fontSize = 44.sp, fontWeight = FontWeight.Bold) else MaterialTheme.typography.body1,
                                        trailingIcon = { if (q.type == "number") raw.optJSONObject("config")?.optString("unit")?.takeUnless { it.isBlank() || it == "null" }?.let { Text(it, color = WildforceThemeTokens.textSecondary, style = MaterialTheme.typography.h6, modifier = Modifier.padding(end = 12.dp)) } },
                                        colors = TextFieldDefaults.textFieldColors(backgroundColor = WildforceThemeTokens.backgroundSecondary, textColor = WildforceThemeTokens.textPrimary, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent))
                                }
                                "scale" -> {
                                    val values = (q.min..q.max).toList()
                                    val columns = values.size.coerceIn(1, 6)
                                    values.chunked(columns).forEach { row ->
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            row.forEach { n -> Box(Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(12.dp)).background(if (current == n) WildforceThemeTokens.accent else WildforceThemeTokens.backgroundSecondary).selectable(selected = current == n, role = Role.RadioButton, onClick = { setAnswer(q, n) }), contentAlignment = Alignment.Center) { Text("$n", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.h6, color = if (current == n) WildforceThemeTokens.primaryButtonText else WildforceThemeTokens.textPrimary) } }
                                            repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                                        }
                                    }
                                    val config = raw.optJSONObject("config")
                                    val minLabel = config?.optString("min_label")?.takeUnless { it.isBlank() || it == "null" }
                                    val maxLabel = config?.optString("max_label")?.takeUnless { it.isBlank() || it == "null" }
                                    if (minLabel != null || maxLabel != null) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(minLabel?.let { "${q.min} · $it" }.orEmpty(), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary); Text(maxLabel?.let { "$it · ${q.max}" }.orEmpty(), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary) }
                                }
                                "yes_no" -> listOf(true, false).forEach { value -> CheckInOption(if (value) androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.ui_sheet_yes) else androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.ui_sheet_no), current == value, onClick = { setAnswer(q, value) }, icon = if (value) Icons.Default.ThumbUp else Icons.Default.ThumbDown) }
                                "single_choice", "multi_choice" -> {
                                    q.options.forEach { option -> val selected = if (q.type == "single_choice") current == option else (current as? JSONArray)?.strings()?.contains(option) == true; CheckInOption(option, selected, multiple = q.type == "multi_choice", onClick = { if (q.type == "single_choice") setAnswer(q, option) else { val values = (current as? JSONArray)?.strings().orEmpty().toMutableList(); if (selected) values.remove(option) else values.add(option); setAnswer(q, if (values.isEmpty()) null else JSONArray(values)) } }) }
                                    if (q.type == "multi_choice" && !q.required) CheckInOption(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.ui_sheet_none_of_these), current is JSONArray && current.length() == 0, onClick = { setAnswer(q, if (current is JSONArray && current.length() == 0) null else JSONArray()) }, multiple = true)
                                }
                                "bodyweight", "measurements" -> { val types = if (q.type == "bodyweight") listOf("weight") else q.sites.filter { it in checkInMeasurementSites }; types.forEach { type ->
                                    val factor = checkInMetricDisplayFactor(type, imperial)
                                    var value by remember(id, type, imperial) { mutableStateOf(metrics.firstOrNull { it.optString("type") == type }?.optDouble("value")?.let { "%.1f".format(java.util.Locale.ROOT, it * factor) }.orEmpty()) }
                                    val label = metricLabels[type] ?: type
                                    val unit = checkInMetricUnit(type, imperial)
                                    TextField(value = value, onValueChange = { value = it; val number = it.replace(',', '.').toDoubleOrNull()?.takeIf { n -> n > 0 }; if (number == null) CheckInAssets.clearMetric(context, userId, id, type) else CheckInAssets.putMetric(context, userId, id, type, number / factor); revision++ }, label = { Text("$label ($unit)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), shape = RoundedCornerShape(14.dp), colors = TextFieldDefaults.textFieldColors(backgroundColor = WildforceThemeTokens.backgroundSecondary, textColor = WildforceThemeTokens.textPrimary, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent))
                                } }
                                "progress_photos" -> {
                                    val angles = listOf("front" to androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.front), "profile" to androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.ui_sheet_profile), "back" to androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.back))
                                    if (photo != null && hasPhotos) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        angles.forEach { (angle, label) -> CheckInPhotoThumbnail(context, photo, angle, label, Modifier.weight(1f)) }
                                    } else angles.forEach { (angle, label) ->
                                        Text("$label ${if (photo?.optString(angle)?.isNotBlank() == true) "✓" else ""}")
                                    }
                                    Button(onClick = { capturingPhotos = true }) {
                                        Text(androidx.compose.ui.res.stringResource(if (hasPhotos) io.codepassion.doubletriangle.core.designsystem.R.string.ui_sheet_retake_photo else io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_841b8ccb44fa8d7f))
                                    }
                                }
                                "pain_map" -> {
                                    val entries = (current as? JSONArray)?.objects().orEmpty()
                                    CheckInOption(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.ui_sheet_nothing_hurts), current is JSONArray && current.length() == 0, onClick = { setAnswer(q, if (current is JSONArray && current.length() == 0) null else JSONArray()) }, icon = Icons.Default.Mood)
                                    checkInPainZones.toList().chunked(2).forEach { zones ->
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            zones.forEach { zone ->
                                                val selected = entries.any { it.optString("zone") == zone }
                                                Box(Modifier.weight(1f).heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp)).background(if (selected) WildforceThemeTokens.accentRed.copy(alpha = .85f) else WildforceThemeTokens.backgroundSecondary).clickable {
                                                    val updated = entries.filterNot { it.optString("zone") == zone }.toMutableList()
                                                    if (!selected) updated.add(JSONObject().put("zone", zone).put("intensity", 5))
                                                    setAnswer(q, JSONArray(updated))
                                                }.padding(12.dp), contentAlignment = Alignment.Center) { Text(painLabels[zone] ?: zone, color = if (selected) Color.White else WildforceThemeTokens.textPrimary, style = MaterialTheme.typography.body2, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) }
                                            }
                                            if (zones.size == 1) Spacer(Modifier.weight(1f))
                                        }
                                    }
                                    entries.forEach { entry ->
                                        val zone = entry.optString("zone")
                                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(WildforceThemeTokens.backgroundSecondary).padding(16.dp)) {
                                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(painLabels[zone] ?: zone, fontWeight = FontWeight.Bold); Text("${entry.optInt("intensity", 5)}/10", fontWeight = FontWeight.Bold) }
                                            Slider(value = entry.optInt("intensity", 5).toFloat(), onValueChange = { v -> val updated = entries.map { if (it.optString("zone") == zone) JSONObject().put("zone", zone).put("intensity", v.toInt()) else it }; setAnswer(q, JSONArray(updated)) }, valueRange = 1f..10f, steps = 8, colors = SliderDefaults.colors(thumbColor = WildforceThemeTokens.accentRed, activeTrackColor = WildforceThemeTokens.accentRed))
                                        }
                                    }
                                }
                            }
                        }
                    }
                    error?.let { Text(it, color = MaterialTheme.colors.error) }
                }
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (!sent && step > 0) IconButton(enabled = !busy, onClick = { step-- }, modifier = Modifier.size(width = 52.dp, height = 48.dp).clip(RoundedCornerShape(8.dp)).background(WildforceThemeTokens.backgroundSecondary)) { Icon(Icons.Default.ChevronLeft, androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.action_back), tint = WildforceThemeTokens.textPrimary) }
                    Button(modifier = Modifier.weight(1f).heightIn(min = 48.dp), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary, contentColor = WildforceThemeTokens.primaryButtonText, disabledBackgroundColor = WildforceThemeTokens.textPrimary.copy(alpha = .2f), disabledContentColor = WildforceThemeTokens.primaryButtonText), enabled = !busy && (sent || step <= questions.size || missing.isEmpty()), onClick = {
                        if (sent) onClose()
                        else if (step <= questions.size) { step++; scope.launch { runCatching { synchronize() }.onFailure { error = context.getString(io.codepassion.doubletriangle.core.designsystem.R.string.the_operation_could_not_be_completed__please_try_again_) } } }
                        else { busy = true; scope.launch { runCatching { CheckInDraftStore.save(context, userId, editableRecord, JSONObject(answersRaw), submit = true); synchronize() }.onSuccess { sent = true }.onFailure { error = context.getString(io.codepassion.doubletriangle.core.designsystem.R.string.the_operation_could_not_be_completed__please_try_again_) }; busy = false } }
                    }) { Text(androidx.compose.ui.res.stringResource(when { sent -> io.codepassion.doubletriangle.core.designsystem.R.string.done; step == 0 -> io.codepassion.doubletriangle.core.designsystem.R.string.start; step > questions.size -> io.codepassion.doubletriangle.core.designsystem.R.string.send; step - 1 !in answeredIndices -> io.codepassion.doubletriangle.core.designsystem.R.string.skip_for_now; step == questions.size -> io.codepassion.doubletriangle.core.designsystem.R.string.review; else -> io.codepassion.doubletriangle.core.designsystem.R.string.next }), fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp) }
                }
            }
        }
    }
    if (capturingPhotos) {
        Dialog(onDismissRequest = { capturingPhotos = false }, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
            CheckInDialogSystemBars(lightBackground = false)
            Box(Modifier.fillMaxSize().background(Color.Black).windowInsetsPadding(WindowInsets.safeDrawing)) {
                CheckInGuidedPhotoCapture(onClose = { capturingPhotos = false }, onSave = { front, profile, back ->
                    CheckInAssets.putGuidedPhotos(context, userId, id, front, profile, back)
                    revision++
                })
            }
        }
    }
}
