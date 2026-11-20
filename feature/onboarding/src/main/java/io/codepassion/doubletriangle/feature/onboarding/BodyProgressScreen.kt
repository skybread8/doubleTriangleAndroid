package io.codepassion.doubletriangle.feature.onboarding

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.codepassion.doubletriangle.core.designsystem.R as DesignR
import io.codepassion.doubletriangle.core.designsystem.WildforceThemeTokens
import io.codepassion.doubletriangle.core.model.RemoteSyncChangeTracker
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** Full-screen check-in flow: paired capture, gallery, comparison and future preview. */
@Composable
internal fun BodyProgressScreen(
    profile: OnboardingProfile,
    generateFuturePreview: suspend (String, String) -> Bitmap,
    loadFuturePreview: suspend (String, String) -> Bitmap?,
    analyzeComposition: suspend (String) -> BodyCompositionAnalysis,
    showsAiCoach: Boolean,
    canUseAiCoach: Boolean,
    onShowAiCoachOffer: () -> Unit,
    onChanged: () -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    var sessions by remember { mutableStateOf(BodyProgressStore.load(context)) }
    var screen by remember { mutableStateOf<Screen>(Screen.Gallery) }
    val preferences = remember(context) { context.getSharedPreferences("wildforce_body_progress", 0) }
    val detailPreferences = remember(context) { context.getSharedPreferences("wildforce_profile_details", 0) }
    val legacyTutorialSeen = remember { preferences.getBoolean("gallery_tutorial_v1", false) }
    var showGalleryTutorial by remember { mutableStateOf(!legacyTutorialSeen && !detailPreferences.getBoolean("has_seen_body_progress_tutorial", false)) }
    LaunchedEffect(legacyTutorialSeen) {
        if (legacyTutorialSeen && !detailPreferences.getBoolean("has_seen_body_progress_tutorial", false)) {
            detailPreferences.edit().putBoolean("has_seen_body_progress_tutorial", true).apply()
            RemoteSyncChangeTracker.mark(context, "user-app-settings")
            onChanged()
        }
    }
    // The iOS capture flow uses a directional slide with a short fade between
    // gallery, capture and comparison. Keep the same continuity on Android.
    AnimatedContent(
        targetState = screen,
        transitionSpec = {
            (slideInHorizontally(tween(320)) { it / 4 } + fadeIn(tween(240))) togetherWith
                (slideOutHorizontally(tween(260)) { -it / 6 } + fadeOut(tween(180)))
        },
        label = "body-progress-screen",
    ) { destination ->
        when (destination) {
            Screen.Gallery -> Gallery(sessions, onClose, { screen = Screen.Capture }, { screen = Screen.Compare(it.id) }) { session ->
                BodyProgressStore.delete(context, session); sessions = BodyProgressStore.load(context); onChanged()
            }
            Screen.Capture -> Capture(sessions, { screen = Screen.Gallery }) { sessions = BodyProgressStore.load(context); screen = Screen.Gallery; onChanged() }
            is Screen.Compare -> Compare(sessions, destination.id, profile, { screen = Screen.Gallery }, generateFuturePreview, loadFuturePreview, analyzeComposition, showsAiCoach, canUseAiCoach, onShowAiCoachOffer)
        }
    }
    if (showGalleryTutorial) {
        BodyProgressTutorial(
            onFinish = {
                preferences.edit().putBoolean("gallery_tutorial_v1", true).apply()
                detailPreferences.edit().putBoolean("has_seen_body_progress_tutorial", true).apply()
                RemoteSyncChangeTracker.mark(context, "user-app-settings")
                onChanged()
                showGalleryTutorial = false
            },
            onDismiss = { showGalleryTutorial = false },
        )
    }
}

private sealed class Screen { data object Gallery : Screen(); data object Capture : Screen(); data class Compare(val id: String) : Screen() }

@Composable private fun BodyProgressTutorial(onFinish: () -> Unit, onDismiss: () -> Unit) {
    var step by remember { mutableStateOf(0) }
    val titles = listOf(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.track_visual_progress), androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.keep_it_consistent), androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.compare_clearly))
    val eyebrows = listOf(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.track_visually), androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.same_hour__same_place), androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.compare_over_time)).map { it.uppercase() }
    val descriptions = listOf(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.capture_front__profile__and_back_photos_with_guides_so_every_check_in_is_easy_to_compare_over_time_), androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.keep_your_progress_photos_as_consistent_as_possible_so_changes_are_easier_to_judge_over_time_), androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.review_the_gallery__then_compare_your_first_and_newer_check_ins_))
    val cards = listOf(
        androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.front) + "  +  " + androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.ui_sheet_profile),
        listOf(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.use_similar_lighting), androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.stay_inside_the_guide), androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.stand_relaxed_and_avoid_flexing)).joinToString("\n") { "• $it" },
        androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.ui_sheet_gallery) + "  →  " + androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.ui_sheet_compare),
    )
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
    Column(Modifier.fillMaxWidth().heightIn(min = maxHeight * .5f, max = maxHeight * .85f).clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)).background(WildforceThemeTokens.backgroundSecondary).padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_45881d23fdac1d2c, step + 1), Modifier.weight(1f), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
            Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.skip), Modifier.heightIn(min = 48.dp).clickable(onClick = onFinish).padding(horizontal = 8.dp, vertical = 8.dp), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { repeat(3) { index -> Box(Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(6.dp)).background(if (index == step) WildforceThemeTokens.textPrimary else WildforceThemeTokens.textPrimary.copy(alpha = .14f))) } }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(Modifier.size(54.dp).clip(RoundedCornerShape(16.dp)).background(WildforceThemeTokens.textPrimary), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.PhotoCamera, null, Modifier.size(25.dp), WildforceThemeTokens.background)
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(eyebrows[step], style = MaterialTheme.typography.caption, color = WildforceThemeTokens.accent)
                    Text(titles[step], style = MaterialTheme.typography.h5, color = WildforceThemeTokens.textPrimary)
                }
            }
            Text(descriptions[step], style = MaterialTheme.typography.body1, color = WildforceThemeTokens.textSecondary)
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(WildforceThemeTokens.textPrimary.copy(alpha = .08f)).padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(if (step == 1) androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.for_the_best_comparison) else if (step == 0) androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.what_you_ll_capture) else androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.how_compare_works), style = MaterialTheme.typography.subtitle2, color = WildforceThemeTokens.textPrimary)
                Text(cards[step], style = MaterialTheme.typography.body2, color = WildforceThemeTokens.textSecondary)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (step > 0) Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.action_back), Modifier.heightIn(min = 48.dp).clickable { step-- }.padding(horizontal = 12.dp, vertical = 12.dp), color = WildforceThemeTokens.textSecondary)
            Button(onClick = { if (step == 2) onFinish() else step++ }, colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary, contentColor = WildforceThemeTokens.primaryButtonText), modifier = Modifier.weight(1f)) { Text(if (step == 2) androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.got_it) else androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.next), color = WildforceThemeTokens.primaryButtonText) }
        }
    }
    }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable private fun Gallery(sessions: List<BodyProgressSession>, close: () -> Unit, capture: () -> Unit, open: (BodyProgressSession) -> Unit, delete: (BodyProgressSession) -> Unit) {
    var pendingDelete by remember { mutableStateOf<BodyProgressSession?>(null) }
    var tipsOpen by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(WildforceThemeTokens.background)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clickable(onClick = close), contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Filled.ArrowBack, androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.ui_sheet_go_back), Modifier.size(22.dp), WildforceThemeTokens.textPrimary) }
            Spacer(Modifier.weight(1f))
            Box(Modifier.size(48.dp).clickable { tipsOpen = true }, contentAlignment = Alignment.Center) { Icon(Icons.Filled.Info, androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.view_progress_photo_tips), Modifier.size(25.dp), WildforceThemeTokens.textPrimary) }
            Spacer(Modifier.width(18.dp))
            Box(Modifier.size(48.dp).clickable(onClick = capture), contentAlignment = Alignment.Center) { Icon(Icons.Filled.Add, androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.add_progress_photos), Modifier.size(29.dp), WildforceThemeTokens.accent) }
        }
        if (sessions.isEmpty()) EmptyGallery(capture) else {
            LazyVerticalGrid(GridCells.Fixed(3), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f).padding(horizontal = 16.dp, vertical = 4.dp)) {
                items(sessions.sortedByDescending { it.date }, key = { it.id }) { session ->
                    var menuOpen by remember { mutableStateOf(false) }
                    Box(Modifier.height(138.dp).clip(RoundedCornerShape(18.dp)).background(WildforceThemeTokens.textSecondary.copy(alpha = .10f)).combinedClickable(onClick = { open(session) }, onLongClick = { menuOpen = true })) {
                        Photo(session.frontPath, modifier = Modifier.fillMaxSize())
                        Box(Modifier.fillMaxWidth().height(70.dp).align(Alignment.BottomCenter).background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .62f)))) )
                        Text(progressPhotoDate(session.date, includeYear = false), Modifier.align(Alignment.BottomStart).padding(10.dp), style = MaterialTheme.typography.caption, color = Color.White)
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(onClick = { menuOpen = false; pendingDelete = session }) {
                                Icon(Icons.Filled.Delete, null, Modifier.size(18.dp), WildforceThemeTokens.destructiveText)
                                Spacer(Modifier.width(12.dp))
                                Text(androidx.compose.ui.res.stringResource(DesignR.string.delete_session), color = WildforceThemeTokens.destructiveText)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
    pendingDelete?.let { session -> AlertDialog(onDismissRequest = { pendingDelete = null }, title = { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_e65789c3621d4999)) }, text = { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.this_removes_all_saved_photos_from_this_check_in_and_can_t_be_undone_)) }, confirmButton = { Button(onClick = { delete(session); pendingDelete = null }) { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.delete)) } }, dismissButton = { Button(onClick = { pendingDelete = null }) { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.ui_sheet_cancel)) } }) }
    if (tipsOpen) BestPracticesDialog { tipsOpen = false }
}

@Composable private fun EmptyGallery(capture: () -> Unit) = Column(Modifier.fillMaxSize().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
    Spacer(Modifier.weight(1f))
    Box(Modifier.size(110.dp).clip(RoundedCornerShape(30.dp)).background(WildforceThemeTokens.accent.copy(alpha = .16f)), contentAlignment = Alignment.Center) {
        Icon(Icons.Filled.PhotoCamera, null, Modifier.size(44.dp), WildforceThemeTokens.accent)
    }
    Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_3ab7a4383ee95fc7), style = MaterialTheme.typography.h6, color = WildforceThemeTokens.textPrimary, textAlign = TextAlign.Center)
    Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.capture_front__profile__and_back_photos_with_guides_so_every_check_in_is_easy_to_compare_over_time_), style = MaterialTheme.typography.body2, color = WildforceThemeTokens.textSecondary, textAlign = TextAlign.Center)
    Spacer(Modifier.weight(1f))
    Spacer(Modifier.weight(1f))
    Button(onClick = capture, colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary, contentColor = WildforceThemeTokens.primaryButtonText), modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.start).uppercase(), color = WildforceThemeTokens.primaryButtonText) }
}

@Composable private fun BestPracticesDialog(close: () -> Unit) = AlertDialog(
    onDismissRequest = close,
    title = { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_ce9a1f0628bfdf47)) },
    text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.keep_your_progress_photos_as_consistent_as_possible_so_changes_are_easier_to_judge_over_time_))
        Text(listOf(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.wear_similar_clothing_or_fitted_shorts), androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.use_similar_lighting), androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.keep_the_same_distance_and_framing), androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.stand_relaxed_and_avoid_flexing), androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.use_the_ghost_and_overlay_to_match_your_body_)).joinToString("\n") { "• $it" }, color = WildforceThemeTokens.textSecondary)
    } },
    confirmButton = { Button(onClick = close) { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.got_it)) } }
)

/** Reuses the progress-photo framing flow for coach check-ins. */
@Composable
fun CheckInGuidedPhotoCapture(onClose: () -> Unit, onSave: (Bitmap, Bitmap, Bitmap) -> Unit) {
    val context = LocalContext.current
    val sessions = remember { BodyProgressStore.load(context) }
    Capture(sessions, onClose, savePhotos = onSave, saved = onClose)
}

@Composable private fun Capture(
    sessions: List<BodyProgressSession>,
    close: () -> Unit,
    savePhotos: ((Bitmap, Bitmap, Bitmap) -> Unit)? = null,
    saved: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var step by remember { mutableStateOf(Step.Front) }; var front by remember { mutableStateOf<Bitmap?>(null) }; var profile by remember { mutableStateOf<Bitmap?>(null) }; var back by remember { mutableStateOf<Bitmap?>(null) }; var error by remember { mutableStateOf<String?>(null) }
    var overlay by remember { mutableStateOf(Overlay.Guide) }; var timerSeconds by remember { mutableStateOf(0) }; var countdown by remember { mutableStateOf<Int?>(null) }
    var tutorial by remember { mutableStateOf(!context.getSharedPreferences("wildforce_body_progress", 0).getBoolean("capture_tutorial_v1", false)) }
    var reviewScale by remember { mutableStateOf(1f) }
    var reviewOffset by remember { mutableStateOf(Offset.Zero) }
    var reviewViewport by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
    fun assignCapturedImage(bitmap: Bitmap) {
        val cropped = cropToCaptureFrame(bitmap)
        if (cropped !== bitmap) bitmap.recycle()
        reviewScale = 1f
        reviewOffset = Offset.Zero
        when (step) { Step.Front -> front = cropped; Step.Profile -> profile = cropped; Step.Back -> back = cropped }
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> uri?.let { BodyProgressStore.bitmapFrom(context, it) }?.let(::assignCapturedImage) }
    var cameraPermissionGranted by remember { mutableStateOf(context.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    val requestCameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { cameraPermissionGranted = it }
    var microphonePermissionGranted by remember { mutableStateOf(context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) }
    var voiceState by remember { mutableStateOf(BodyProgressVoiceCaptureController.State.Idle) }
    val voiceCapture = remember(context) { BodyProgressVoiceCaptureController(context) { voiceState = it } }
    DisposableEffect(voiceCapture) { onDispose { voiceCapture.stop() } }
    var tiltState by remember { mutableStateOf(TiltState()) }
    val tiltService = remember(context) { DeviceTiltService(context) { tiltState = it } }
    DisposableEffect(tiltService) {
        tiltService.start()
        onDispose { tiltService.stop() }
    }
    var startVoiceAfterPermission by remember { mutableStateOf(false) }
    val requestMicrophonePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        microphonePermissionGranted = granted
        startVoiceAfterPermission = granted
    }
    var useFrontCamera by remember { mutableStateOf(context.getSharedPreferences("wildforce_body_progress", 0).getBoolean("preferred_front_camera", false)) }
    var cameraCaptureAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var captureFlashTarget by remember { mutableFloatStateOf(0f) }
    var announcesNextCapture by remember { mutableStateOf(false) }
    val captureFlashOpacity by animateFloatAsState(captureFlashTarget, tween(450), label = "capture-flash")
    val captureChime = remember { ToneGenerator(AudioManager.STREAM_MUSIC, 85) }
    DisposableEffect(captureChime) { onDispose { captureChime.release() } }
    // iOS springs the countdown bubble for each tick; key the Animatable by
    // the remaining second so 3 → 2 → 1 is perceived as three distinct beats.
    val countdownScale = remember(countdown) { Animatable(if (countdown == null) 0f else .68f) }
    LaunchedEffect(countdown) {
        if (countdown != null) {
            countdownScale.animateTo(1f, spring(dampingRatio = .58f, stiffness = 480f))
        }
    }
    LaunchedEffect(step) {
        reviewScale = 1f
        reviewOffset = Offset.Zero
    }
    val image = when (step) { Step.Front -> front; Step.Profile -> profile; Step.Back -> back }
    val stepLabel = when (step) {
        Step.Front -> androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.front)
        Step.Profile -> androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.ui_sheet_profile)
        Step.Back -> androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.back)
    }
    val stepHelp = when (step) {
        Step.Front -> androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.face_the_camera__keep_your_arms_relaxed__and_fit_your_full_body_inside_the_guide_)
        Step.Profile -> androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.stand_sideways__keep_your_full_body_in_frame__and_align_your_head_with_the_guide_)
        Step.Back -> androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.face_away_from_the_camera__keep_your_arms_relaxed__and_fit_your_full_body_inside_the_guide_)
    }
    val referencePath = sessions.sortedByDescending { it.date }.firstNotNullOfOrNull { when (step) { Step.Front -> it.frontPath; Step.Profile -> it.profilePath; Step.Back -> it.backPath } }
    var ghostScale by remember(step, referencePath) { mutableStateOf(1f) }
    var ghostOffset by remember(step, referencePath) { mutableStateOf(Offset.Zero) }
    fun playCaptureFeedback() {
        captureFlashTarget = 1f
        scope.launch {
            delay(80)
            captureFlashTarget = 0f
        }
        if (announcesNextCapture) {
            announcesNextCapture = false
            // Hands-free captures need an audible acknowledgement: the phone is
            // normally propped up, so vibration is neither useful nor desirable.
            captureChime.startTone(ToneGenerator.TONE_PROP_ACK, 450)
        }
    }
    fun startCamera(isHandsFree: Boolean = false) {
        if (!cameraPermissionGranted) { requestCameraPermission.launch(Manifest.permission.CAMERA); return }
        val capture = {
            announcesNextCapture = isHandsFree
            cameraCaptureAction?.invoke()
        }
        if (timerSeconds == 0) capture() else scope.launch {
            for (remaining in timerSeconds downTo 1) { countdown = remaining; delay(1_000) }
            countdown = null
            announcesNextCapture = true
            cameraCaptureAction?.invoke()
        }
    }
    fun toggleVoiceCapture() {
        if (!microphonePermissionGranted) {
            requestMicrophonePermission.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            voiceCapture.toggle { startCamera(isHandsFree = true) }
        }
    }
    LaunchedEffect(startVoiceAfterPermission) {
        if (startVoiceAfterPermission) {
            startVoiceAfterPermission = false
            voiceCapture.toggle { startCamera(isHandsFree = true) }
        }
    }
    Column(Modifier.fillMaxSize().background(Color.Black).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.ui_sheet_dismiss), Modifier.heightIn(min = 48.dp).clickable(onClick = close).padding(vertical = 10.dp), color = Color.White, style = MaterialTheme.typography.body2)
            Row(Modifier.weight(1f).padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) { repeat(3) { index -> Box(Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(6.dp)).background(if (index <= step.ordinal) Color.White else Color.White.copy(alpha = .22f))) } }
            Box(Modifier.size(48.dp).clickable {
                useFrontCamera = !useFrontCamera
                context.getSharedPreferences("wildforce_body_progress", 0).edit().putBoolean("preferred_front_camera", useFrontCamera).apply()
            }, contentAlignment = Alignment.Center) { Icon(Icons.Filled.Cameraswitch, androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.swap_camera), Modifier.size(24.dp), Color.White) }
            Spacer(Modifier.width(14.dp))
            Box(Modifier.size(48.dp).clickable { timerSeconds = when (timerSeconds) { 0 -> 3; 3 -> 5; else -> 0 } }, contentAlignment = Alignment.Center) { Icon(Icons.Filled.Timer, androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.timer), Modifier.size(23.dp), if (timerSeconds == 0) Color.White else WildforceThemeTokens.accent) }
        }
        Text("${step.ordinal + 1} / 3 · $stepLabel", color = Color.White, style = MaterialTheme.typography.h6)
        Text(stepHelp, color = Color.White.copy(alpha = .78f), style = MaterialTheme.typography.body2)
        if (voiceState == BodyProgressVoiceCaptureController.State.Listening) {
            Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_caa7246aae3a7b91, voiceCapture.triggerDisplayWord), color = WildforceThemeTokens.accent, style = MaterialTheme.typography.caption, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
        Box(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(20.dp)).background(Color.DarkGray), contentAlignment = Alignment.Center) {
            if (image == null) {
                if (cameraPermissionGranted) {
                    ProgressPhotoCamera(
                        useFrontCamera = useFrontCamera,
                        modifier = Modifier.fillMaxSize(),
                        onReady = { cameraCaptureAction = it },
                        onPhotoCaptured = { bitmap ->
                            assignCapturedImage(bitmap)
                            playCaptureFeedback()
                        },
                        onFailure = { error = it },
                    )
                    if (overlay == Overlay.Reference && referencePath != null) {
                        Box(
                            Modifier.fillMaxSize().pointerInput(referencePath) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    ghostScale = (ghostScale * zoom).coerceIn(.75f, 2.5f)
                                    ghostOffset += pan
                                }
                            },
                        ) {
                            Photo(
                                referencePath,
                                modifier = Modifier.fillMaxSize().graphicsLayer {
                                    alpha = .26f
                                    scaleX = ghostScale
                                    scaleY = ghostScale
                                    translationX = ghostOffset.x
                                    translationY = ghostOffset.y
                                },
                            )
                        }
                    }
                    FramingGuideOverlay()
                    Box(
                        Modifier.fillMaxSize().background(Color.White.copy(alpha = captureFlashOpacity)),
                    )
                    TiltIndicator(tiltState, Modifier.align(Alignment.CenterEnd))
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) { Text(if (overlay == Overlay.Reference && referencePath != null) androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_11102b29326ef97a) else androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_3657487951193daf), color = Color.White, textAlign = TextAlign.Center) }
                } else {
                    Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Icon(Icons.Filled.PhotoCamera, null, Modifier.size(52.dp), Color.White)
                        Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_16945fe99efdebf9), color = Color.White, textAlign = TextAlign.Center)
                        Button(onClick = { requestCameraPermission.launch(Manifest.permission.CAMERA) }, colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary, contentColor = WildforceThemeTokens.primaryButtonText)) { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_71dc78bdba9c367c), color = WildforceThemeTokens.primaryButtonText) }
                    }
                }
            } else {
                Box(
                    Modifier.fillMaxSize().onSizeChanged { reviewViewport = it }.pointerInput(image) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            reviewScale = (reviewScale * zoom).coerceIn(1f, 3f)
                            reviewOffset += pan
                        }
                    },
                ) {
                    Image(
                        image.asImageBitmap(),
                        stepLabel,
                        Modifier.fillMaxSize().graphicsLayer {
                            scaleX = reviewScale
                            scaleY = reviewScale
                            translationX = reviewOffset.x
                            translationY = reviewOffset.y
                        },
                        contentScale = ContentScale.Crop,
                    )
                    Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_101c82a9a65dd9cd), color = Color.White.copy(alpha = .82f), style = MaterialTheme.typography.caption, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp))
                }
            }
            countdown?.let {
                Text(
                    it.toString(),
                    style = MaterialTheme.typography.h1,
                    color = Color.White,
                    modifier = Modifier.graphicsLayer {
                        scaleX = countdownScale.value
                        scaleY = countdownScale.value
                    },
                )
            }
        }
        val overlayLabels = mapOf(Overlay.Guide to androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.silhouette), Overlay.Reference to androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.reference))
        Toggle(listOf(Overlay.Guide, Overlay.Reference), overlay, { overlayLabels.getValue(it) }) { overlay = it }
        if (image == null) {
            Text(if (timerSeconds == 0) androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.timer) + ": " + androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.off) else androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.timer) + ": " + timerSeconds + " s", color = Color.White.copy(alpha = .72f), style = MaterialTheme.typography.caption, modifier = Modifier.align(Alignment.CenterHorizontally))
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(48.dp).clip(RoundedCornerShape(24.dp)).background(Color.White.copy(alpha = .16f)).clickable { gallery.launch("image/*") }, contentAlignment = Alignment.Center) { Icon(Icons.Filled.PhotoCamera, androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.ui_sheet_gallery), Modifier.size(24.dp), Color.White) }
                    Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.ui_sheet_gallery), color = Color.White, style = MaterialTheme.typography.caption)
                }
                Box(Modifier.size(82.dp).clip(RoundedCornerShape(41.dp)).background(Color.White).clickable(enabled = countdown == null, onClick = ::startCamera), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(68.dp).clip(RoundedCornerShape(34.dp)).background(if (countdown == null) WildforceThemeTokens.accent else Color.Gray))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(48.dp).clip(RoundedCornerShape(24.dp)).background(Color.White.copy(alpha = .16f)).clickable { timerSeconds = when (timerSeconds) { 0 -> 3; 3 -> 5; else -> 0 } }, contentAlignment = Alignment.Center) { Icon(Icons.Filled.Timer, androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.set_shutter_timer), Modifier.size(24.dp), Color.White) }
                    Text(if (countdown == null) androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_fd035357b83dd14f) else androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_e160381847c42d4d), color = Color.White, style = MaterialTheme.typography.caption)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(48.dp).clip(RoundedCornerShape(24.dp)).background(Color.White.copy(alpha = .16f)).clickable(enabled = countdown == null, onClick = ::toggleVoiceCapture), contentAlignment = Alignment.Center) { Icon(if (voiceState == BodyProgressVoiceCaptureController.State.Listening) Icons.Filled.Mic else Icons.Filled.MicNone, androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.toggle_voice_capture), Modifier.size(24.dp), if (voiceState == BodyProgressVoiceCaptureController.State.Listening) WildforceThemeTokens.accent else Color.White) }
                    Text(if (voiceState == BodyProgressVoiceCaptureController.State.Listening) androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_21f51ef777460b14) else androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_c9566ca43bdcc78f), color = Color.White, style = MaterialTheme.typography.caption)
                }
            }
        } else {
            Button(onClick = {
                val adjusted = renderAdjustedBitmap(image, reviewScale, reviewOffset, reviewViewport)
                if (step == Step.Front) {
                    front = adjusted
                    step = Step.Profile
                } else if (step == Step.Profile) {
                    profile = adjusted
                    step = Step.Back
                } else {
                    back = adjusted
                    runCatching {
                        if (savePhotos != null) savePhotos(front!!, profile!!, adjusted)
                        else BodyProgressStore.save(context, front!!, profile!!, adjusted)
                    }.onSuccess { saved() }.onFailure { error = context.getString(io.codepassion.doubletriangle.core.designsystem.R.string.couldn_t_save_these_progress_photos__please_try_again_) }
                }
            }, colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary, contentColor = WildforceThemeTokens.primaryButtonText), modifier = Modifier.fillMaxWidth()) { Text(if (step != Step.Back) androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.next) else androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_97992b805fa065cf), color = WildforceThemeTokens.primaryButtonText) }
            Button(onClick = { when (step) { Step.Front -> front = null; Step.Profile -> profile = null; Step.Back -> back = null } }, modifier = Modifier.fillMaxWidth()) { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.ui_sheet_retake_photo)) }
        }
        error?.let { Text(it, color = Color.Red) }
    }
    if (tutorial) AlertDialog(onDismissRequest = { tutorial = false; context.getSharedPreferences("wildforce_body_progress", 0).edit().putBoolean("capture_tutorial_v1", true).apply() }, title = { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_ae46f63766af26c9)) }, text = { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_ca77c21fec39cef1)) }, confirmButton = { Button(onClick = { tutorial = false; context.getSharedPreferences("wildforce_body_progress", 0).edit().putBoolean("capture_tutorial_v1", true).apply() }) { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.got_it)) } })
}
private enum class Step(val label: String, val help: String) { Front("Frontal", "Misma hora, distancia y luz que en tus próximos check-ins."), Profile("Perfil", "Gira de lado y conserva la ropa, postura y encuadre."), Back("Espalda", "Colócate de espaldas, con los brazos relajados y el cuerpo dentro de la guía.") }
private enum class Overlay(val label: String) { Guide("Guía"), Reference("Referencia") }

/** Matches iOS's 2:3 capture crop before a progress photo is persisted. */
private fun cropToCaptureFrame(source: Bitmap): Bitmap {
    val targetAspect = 2f / 3f
    val sourceAspect = source.width.toFloat() / source.height.coerceAtLeast(1)
    if (kotlin.math.abs(sourceAspect - targetAspect) < .01f) return source
    return if (sourceAspect > targetAspect) {
        val width = (source.height * targetAspect).toInt().coerceAtLeast(1)
        Bitmap.createBitmap(source, (source.width - width) / 2, 0, width, source.height)
    } else {
        val height = (source.width / targetAspect).toInt().coerceAtLeast(1)
        Bitmap.createBitmap(source, 0, (source.height - height) / 2, source.width, height)
    }
}

/** Persists the same pan/zoom adjustment that the iOS review screen applies. */
private fun renderAdjustedBitmap(source: Bitmap, scale: Float, offset: Offset, viewport: androidx.compose.ui.unit.IntSize): Bitmap {
    if (viewport.width <= 0 || viewport.height <= 0 || (scale <= 1.001f && offset == Offset.Zero)) return source
    val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(output)
    canvas.drawColor(android.graphics.Color.BLACK)
    val matrix = android.graphics.Matrix().apply {
        setScale(scale, scale, source.width / 2f, source.height / 2f)
        postTranslate(
            offset.x * source.width / viewport.width.toFloat(),
            offset.y * source.height / viewport.height.toFloat(),
        )
    }
    canvas.drawBitmap(source, matrix, android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG or android.graphics.Paint.FILTER_BITMAP_FLAG))
    return output
}

@Composable
private fun TiltIndicator(state: TiltState, modifier: Modifier = Modifier) {
    val clamped = state.leanDegrees.coerceIn(-20f, 20f)
    val color = when {
        kotlin.math.abs(state.leanDegrees) <= 5f -> Color(0xFF69F0AE)
        kotlin.math.abs(state.leanDegrees) <= 12f -> Color(0xFFFFD54F)
        else -> Color(0xFFFF6E6E)
    }
    Column(
        modifier.padding(end = 10.dp).clip(RoundedCornerShape(18.dp))
            .background(Color.Black.copy(alpha = .38f)).padding(horizontal = 10.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.width(6.dp).height(132.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = .20f))) {
            Box(
                Modifier.size(18.dp).align(Alignment.Center).offset(y = (clamped / 20f * 57).dp)
                    .clip(androidx.compose.foundation.shape.CircleShape).background(color),
            )
        }
        if (!state.isUpright) {
            Text(if (state.leanDegrees < 0) androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_f79424eb7f17047c) else androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_d77ee55e1cbc8ec0), color = Color.White.copy(alpha = .82f), style = MaterialTheme.typography.overline, textAlign = TextAlign.Center, modifier = Modifier.width(68.dp).padding(top = 8.dp))
        }
    }
}

@Composable private fun FramingGuideOverlay() = Canvas(Modifier.fillMaxSize()) {
    val guideWidth = size.width * .64f
    val guideHeight = size.height * .86f
    val left = (size.width - guideWidth) / 2f
    val top = (size.height - guideHeight) / 2f
    val centerX = size.width / 2f
    val guideColor = Color.White.copy(alpha = .88f)
    val subtle = Color.White.copy(alpha = .42f)
    drawRoundRect(subtle, Offset(left, top), Size(guideWidth, guideHeight), CornerRadius(30.dp.toPx(), 30.dp.toPx()), style = Stroke(width = 1.5.dp.toPx()))
    drawLine(subtle, Offset(centerX, top), Offset(centerX, top + guideHeight), strokeWidth = 1.dp.toPx())
    val eyesY = top + guideHeight * .22f
    val hipsY = top + guideHeight * .63f
    drawLine(subtle, Offset(left, eyesY), Offset(left + guideWidth, eyesY), strokeWidth = 1.dp.toPx())
    drawLine(subtle, Offset(left, hipsY), Offset(left + guideWidth, hipsY), strokeWidth = 1.dp.toPx())
    val headSize = guideWidth * .25f
    drawOval(guideColor, Offset(centerX - headSize / 2f, top + guideHeight * .10f), Size(headSize, headSize * 1.23f), style = Stroke(width = 2.dp.toPx()))
    val torsoWidth = guideWidth * .48f
    val torsoTop = top + guideHeight * .32f
    drawRoundRect(guideColor, Offset(centerX - torsoWidth / 2f, torsoTop), Size(torsoWidth, guideHeight * .48f), CornerRadius(22.dp.toPx(), 22.dp.toPx()), style = Stroke(width = 2.dp.toPx()))
}

@Composable private fun Compare(
    sessions: List<BodyProgressSession>,
    initialId: String,
    profile: OnboardingProfile,
    close: () -> Unit,
    generateFuture: suspend (String, String) -> Bitmap,
    loadFuture: suspend (String, String) -> Bitmap?,
    analyzeComposition: suspend (String) -> BodyCompositionAnalysis,
    showsAiCoach: Boolean,
    canUseAiCoach: Boolean,
    onShowAiCoachOffer: () -> Unit,
) {
    val context = LocalContext.current; val scope = rememberCoroutineScope()
    var selectedId by remember { mutableStateOf(initialId) }; var referenceId by remember { mutableStateOf<String?>(null) }; var angle by remember { mutableStateOf(Angle.Front) }; var comparisonMode by remember { mutableStateOf(ComparisonMode.Reveal) }; var revealReference by remember { mutableStateOf(false) }
    var splitProgress by remember { mutableFloatStateOf(.5f) }; var splitWidthPx by remember { mutableIntStateOf(1) }; var loopShowsReference by remember { mutableStateOf(false) }; var historyLoopIndex by remember { mutableIntStateOf(0) }; var selectedBackgroundStyle by remember { mutableStateOf<BodyProgressBackgroundStyle?>(null) }
    var virtualFuture by remember { mutableStateOf<BodyProgressSession?>(null) }
    var showFutureSheet by remember { mutableStateOf(false) }
    val timelineSessions = remember(sessions, virtualFuture) { sessions.sortedBy { it.date } + listOfNotNull(virtualFuture) }
    val current = timelineSessions.firstOrNull { it.id == selectedId } ?: sessions.last()
    val isFuture = current.id == virtualFuture?.id
    val referenceSessions = remember(timelineSessions, current.id) { timelineSessions.filterNot { it.id == current.id }.sortedBy { it.date } }
    val activeReferenceIndex = referenceSessions.indexOfFirst { it.id == referenceId }.takeIf { it >= 0 } ?: 0
    val before = referenceSessions.getOrNull(activeReferenceIndex) ?: current
    val historySessions = remember(timelineSessions, current.id) { timelineSessions.take(timelineSessions.indexOfFirst { it.id == current.id } + 1) }
    val historySession = historySessions.getOrNull(historyLoopIndex) ?: current
    val prefs = remember { context.getSharedPreferences("wildforce_body_progress", 0) }; var consent by remember { mutableStateOf(prefs.getLong("future_preview_ai_consent_v1", 0) > 0) }; var dialog by remember { mutableStateOf(false) }; var loading by remember { mutableStateOf(false) }; var future by remember(current.id, angle) { mutableStateOf<Bitmap?>(null) }; var failure by remember { mutableStateOf<String?>(null) }; var analysis by remember(current.id) { mutableStateOf<BodyCompositionAnalysis?>(null) }; var analysisLoading by remember { mutableStateOf(false) }; var analysisError by remember { mutableStateOf<String?>(null) }; var showAnalysisIntro by remember { mutableStateOf(false) }
    fun path(session: BodyProgressSession) = when (angle) { Angle.Front -> session.frontPath; Angle.Profile -> session.profilePath; Angle.Back -> session.backPath.orEmpty() }
    LaunchedEffect(comparisonMode, current.id, before.id, historySessions.size) {
        revealReference = false
        loopShowsReference = false
        historyLoopIndex = 0
        when (comparisonMode) {
            ComparisonMode.Loop -> while (true) {
                delay(1_000)
                loopShowsReference = !loopShowsReference
            }
            ComparisonMode.HistoryLoop -> if (historySessions.size > 1) while (true) {
                delay(900)
                historyLoopIndex = (historyLoopIndex + 1) % historySessions.size
            }
            else -> Unit
        }
    }
    LaunchedEffect(current.id, angle, canUseAiCoach) {
        loading = true
        future = if (canUseAiCoach && !isFuture && angle == Angle.Front) runCatching { loadFuture(current.id, angle.apiValue) }.getOrNull() else null
        loading = false
    }
    LaunchedEffect(current.id, referenceSessions.map { it.id }) {
        if (referenceSessions.none { it.id == referenceId }) referenceId = referenceSessions.firstOrNull()?.id
    }
    Column(Modifier.fillMaxSize().background(WildforceThemeTokens.background).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CompareTopBar(
            close = close,
            showMenu = showsAiCoach && !isFuture,
            showFuture = angle == Angle.Front && ageInYears(profile.birthYear, profile.birthMonth, profile.birthDay, LocalDate.now()) >= 18,
            onFuture = { if (canUseAiCoach) showFutureSheet = true else onShowAiCoachOffer() },
            onAnalysis = { if (canUseAiCoach) showAnalysisIntro = true else onShowAiCoachOffer() },
        )
        val angleLabels = mapOf(
            Angle.Front to androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.front),
            Angle.Profile to androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.ui_sheet_profile),
            Angle.Back to androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.back),
        )
        Toggle(Angle.entries, angle, { angleLabels.getValue(it) }) { selectedAngle ->
            if (selectedAngle != Angle.Front && virtualFuture != null) {
                virtualFuture = null
                selectedId = initialId
                referenceId = null
            }
            angle = selectedAngle
        }
        val comparisonLabels = mapOf(
            ComparisonMode.Reveal to androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.tap_to_reveal),
            ComparisonMode.Split to androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.side_drag),
            ComparisonMode.SideBySide to androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.side_by_side),
            ComparisonMode.Loop to androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.loop),
            ComparisonMode.HistoryLoop to androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.full_history_loop),
        )
        if (referenceSessions.size > 2) {
            Slider(
                value = activeReferenceIndex.toFloat(),
                onValueChange = { value -> referenceId = referenceSessions[value.toInt().coerceIn(0, referenceSessions.lastIndex)].id },
                valueRange = 0f..referenceSessions.lastIndex.toFloat(),
                steps = (referenceSessions.size - 2).coerceAtLeast(0),
                colors = SliderDefaults.colors(thumbColor = WildforceThemeTokens.accent, activeTrackColor = WildforceThemeTokens.accent),
            )
            Row(Modifier.fillMaxWidth()) {
                Text(referenceSessions.first().date, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
                Spacer(Modifier.weight(1f))
                Text(referenceSessions.last().date, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
            }
        }
        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            Box(Modifier.fillMaxHeight().aspectRatio(2f / 3f).widthIn(max = 500.dp).clip(RoundedCornerShape(28.dp))) {
        if (comparisonMode == ComparisonMode.SideBySide) Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { ComparePhoto(before, path(before), "Antes", selectedBackgroundStyle, Modifier.weight(1f), profile.metricSystem, before.id == virtualFuture?.id); ComparePhoto(current, path(current), "Ahora", selectedBackgroundStyle, Modifier.weight(1f), profile.metricSystem, current.id == virtualFuture?.id) }
        else if (comparisonMode == ComparisonMode.Split) Box(Modifier.fillMaxSize().clip(RoundedCornerShape(18.dp)).onSizeChanged { splitWidthPx = it.width.coerceAtLeast(1) }.pointerInput(before.id, current.id) {
            detectDragGestures(
                onDragStart = { offset -> splitProgress = (offset.x / splitWidthPx.toFloat()).coerceIn(0f, 1f) },
                onDrag = { change, _ ->
                    change.consume()
                    splitProgress = (change.position.x / splitWidthPx.toFloat()).coerceIn(0f, 1f)
                },
            )
        }) {
            ComparePhoto(current, path(current), "Ahora", selectedBackgroundStyle, Modifier.fillMaxSize(), profile.metricSystem, current.id == virtualFuture?.id)
            Box(Modifier.fillMaxSize().drawWithContent {
                clipRect(right = size.width * splitProgress) { this@drawWithContent.drawContent() }
            }) {
                ComparePhoto(before, path(before), "Antes", selectedBackgroundStyle, Modifier.fillMaxSize(), profile.metricSystem, before.id == virtualFuture?.id)
            }
            Canvas(Modifier.fillMaxSize()) {
                val dividerX = size.width * splitProgress
                drawLine(Color.White.copy(alpha = .92f), Offset(dividerX, 0f), Offset(dividerX, size.height), strokeWidth = 2.dp.toPx())
            }
        }
        else Box(Modifier.fillMaxSize().pointerInput(before.id, current.id, comparisonMode, historySession.id) {
            detectTapGestures(onPress = {
                revealReference = true
                tryAwaitRelease()
                revealReference = false
            })
        }) {
            // iOS cross-fades while the comparison press is held. This avoids
            // a disorienting hard replacement between the two photos.
            val showingReference = when (comparisonMode) {
                ComparisonMode.Reveal -> revealReference
                ComparisonMode.Loop -> loopShowsReference
                ComparisonMode.HistoryLoop -> true
                else -> false
            }
            val comparisonTarget = if (comparisonMode == ComparisonMode.HistoryLoop) "history:${historySession.id}" else if (showingReference) "before" else "current"
            Crossfade(targetState = comparisonTarget, animationSpec = tween(180), label = "body-progress-reveal") { target ->
                val isHistoryTarget = target.startsWith("history:")
                val isReferenceTarget = target == "before"
                val item = if (isHistoryTarget) historySession else if (isReferenceTarget) before else current
                val label = when {
                    isHistoryTarget -> "Historial"
                    isReferenceTarget -> "Antes"
                    comparisonMode == ComparisonMode.Loop -> "Ahora"
                    else -> "Ahora · mantén pulsado para ver antes"
                }
                ComparePhoto(item, path(item), label, selectedBackgroundStyle, Modifier.fillMaxSize(), profile.metricSystem, item.id == virtualFuture?.id)
            }
        }
                Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                    if (before.id != current.id) ComparisonMetadata(before, profile.metricSystem, before.id == virtualFuture?.id)
                    else Spacer(Modifier.width(1.dp))
                    ComparisonMetadata(current, profile.metricSystem, isFuture)
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            ComparisonModeMenu(comparisonMode, comparisonLabels) { comparisonMode = it }
            BodyProgressBackgroundPicker(selectedBackgroundStyle) { selectedBackgroundStyle = it }
        }
        if (showAnalysisIntro && analysis == null) {
            BodyCompositionIntro(
                loading = analysisLoading,
                error = analysisError,
                onStart = {
                    analysisLoading = true
                    analysisError = null
                    scope.launch {
                        runCatching { analyzeComposition(current.id) }
                            .onSuccess { analysis = it }
                            .onFailure { analysisError = context.getString(io.codepassion.doubletriangle.core.designsystem.R.string.we_couldn_t_finish_the_estimate_right_now__please_try_again_) }
                        analysisLoading = false
                    }
                },
            )
        }
        analysis?.let { BodyCompositionResult(it) }
    }
    if (showFutureSheet && !isFuture) {
        FutureProgressDialog(
            image = future,
            loading = loading,
            error = failure,
            onDismiss = { showFutureSheet = false },
            onGenerate = {
                if (!consent) dialog = true else {
                    loading = true
                    failure = null
                    val source = current
                    scope.launch {
                        runCatching {
                            val image = generateFuture(source.id, Angle.Front.apiValue)
                            BodyProgressStore.saveFuture(context, source, image)
                            image
                        }.onSuccess { image -> future = image }
                            .onFailure { failure = context.getString(io.codepassion.doubletriangle.core.designsystem.R.string.couldn_t_generate_the_future_preview_right_now__please_try_again_) }
                        loading = false
                    }
                }
            },
            onCompare = {
                future?.let { image ->
                    val saved = BodyProgressStore.saveFuture(context, current, image)
                    val futureDate = runCatching { LocalDate.parse(current.date).plusDays(180).toString() }.getOrDefault(current.date)
                    virtualFuture = current.copy(id = "future:${current.id}", date = futureDate, frontPath = saved.futurePath.orEmpty(), profilePath = "", backPath = null)
                    referenceId = current.id
                    selectedId = virtualFuture!!.id
                    angle = Angle.Front
                    showFutureSheet = false
                }
            },
        )
    }
    if (dialog) AlertDialog(onDismissRequest = { dialog = false }, title = { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_b284a57124c1f887)) }, text = { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_9e3ea731ed74b4e9)) }, confirmButton = { Button(onClick = { prefs.edit().putLong("future_preview_ai_consent_v1", System.currentTimeMillis()).apply(); consent = true; dialog = false }) { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_47ceb6c25b64d1c6)) } }, dismissButton = { Button(onClick = { dialog = false }) { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.ui_sheet_cancel)) } })
}

@Composable
private fun BodyCompositionResult(analysis: BodyCompositionAnalysis) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), backgroundColor = WildforceThemeTokens.surfaceElevated) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_fef8f7d43e193626), style = MaterialTheme.typography.h6, color = WildforceThemeTokens.textPrimary)
            Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_fd4cf155018f52bb, analysis.estimatedBodyFatRange), color = WildforceThemeTokens.textPrimary)
            Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_76f0763a556d39c7, analysis.confidence.name), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
            Text(analysis.summary, style = MaterialTheme.typography.body2, color = WildforceThemeTokens.textSecondary)
            analysis.insights.forEach { Text("• $it", style = MaterialTheme.typography.body2, color = WildforceThemeTokens.textPrimary) }
            analysis.recommendations.forEach { Text("→ $it", style = MaterialTheme.typography.body2, color = WildforceThemeTokens.textPrimary) }
        }
    }
}

@Composable
private fun BodyCompositionIntro(loading: Boolean, error: String?, onStart: () -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), backgroundColor = WildforceThemeTokens.surfaceElevated) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(if (loading) androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.analysing_your_check_in) else if (error == null) androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.estimate_body_composition_from_this_check_in) else androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.estimate_unavailable), style = MaterialTheme.typography.h6, color = WildforceThemeTokens.textPrimary)
            Text(
                if (loading) androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.we_re_reviewing_your_current_front__profile__and_back_photos_when_available__then_building_a_narrow_body_fat_estimate_)
                else androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.we_ll_analyse_your_current_check_in_photos_with_ai_and_return_a_non_medical_body_fat_estimate_plus_a_few_coaching_notes__accuracy_can_vary_with_pose__clothing__lighting__and_image_quality_),
                style = MaterialTheme.typography.body2,
                color = WildforceThemeTokens.textSecondary,
            )
            if (loading) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(20.dp), color = WildforceThemeTokens.accent, strokeWidth = 2.dp)
                    Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_439857d9ffd3c54c), color = WildforceThemeTokens.textPrimary)
                }
            } else {
                error?.let { Text(it, color = Color.Red, style = MaterialTheme.typography.caption) }
                Button(onClick = onStart, colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary), modifier = Modifier.fillMaxWidth()) {
                    Text(if (error == null) androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.start_estimate) else androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.ui_sheet_retry), color = WildforceThemeTokens.primaryButtonText)
                }
            }
            Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_24d69a5c82264b40), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
        }
    }
}
private enum class Angle(val label: String, val apiValue: String) { Front("Frontal", "front"), Profile("Perfil", "profile"), Back("Espalda", "back") }
private enum class ComparisonMode(val label: String) {
    Reveal("Mantener pulsado"),
    Split("Deslizar"),
    SideBySide("Lado a lado"),
    Loop("Bucle"),
    HistoryLoop("Historial"),
}

@Composable private fun <T> Toggle(items: List<T>, selected: T, label: (T) -> String, select: (T) -> Unit) = Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) { items.forEach { item ->
    val isSelected = item == selected
    val selectionScale by animateFloatAsState(if (isSelected) 1.035f else 1f, spring(dampingRatio = .7f, stiffness = 620f), label = "body-progress-toggle-${label(item)}")
    Text(label(item), Modifier.widthIn(min = 104.dp).graphicsLayer { scaleX = selectionScale; scaleY = selectionScale }.clip(RoundedCornerShape(10.dp)).background(if (isSelected) WildforceThemeTokens.accent else WildforceThemeTokens.textSecondary.copy(alpha = .12f)).clickable { select(item) }.padding(horizontal = 12.dp, vertical = 9.dp), color = if (isSelected) WildforceThemeTokens.primaryButtonText else WildforceThemeTokens.textPrimary, style = MaterialTheme.typography.caption, textAlign = TextAlign.Center)
} }

@Composable
private fun CompareTopBar(close: () -> Unit, showMenu: Boolean, showFuture: Boolean, onFuture: () -> Unit, onAnalysis: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = close) { Icon(Icons.AutoMirrored.Filled.ArrowBack, androidx.compose.ui.res.stringResource(DesignR.string.ui_sheet_go_back), tint = WildforceThemeTokens.textPrimary) }
        Text(androidx.compose.ui.res.stringResource(DesignR.string.ui_sheet_compare), Modifier.weight(1f).padding(start = 8.dp), style = MaterialTheme.typography.h6, color = WildforceThemeTokens.textPrimary)
        if (showMenu) Box {
            IconButton(onClick = { expanded = true }) { Icon(Icons.Filled.MoreVert, androidx.compose.ui.res.stringResource(DesignR.string.menu), tint = WildforceThemeTokens.textPrimary) }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                if (showFuture) DropdownMenuItem(onClick = { expanded = false; onFuture() }) { Text(androidx.compose.ui.res.stringResource(DesignR.string.see_future)) }
                DropdownMenuItem(onClick = { expanded = false; onAnalysis() }) { Text(androidx.compose.ui.res.stringResource(DesignR.string.body_scan_analysis)) }
            }
        }
    }
}

@Composable
private fun ComparisonModeMenu(selected: ComparisonMode, labels: Map<ComparisonMode, String>, onSelect: (ComparisonMode) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        FooterMenuLabel(androidx.compose.ui.res.stringResource(DesignR.string.ui_sheet_compare), labels.getValue(selected)) { expanded = true }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ComparisonMode.entries.forEach { mode -> DropdownMenuItem(onClick = { expanded = false; onSelect(mode) }) { Text(labels.getValue(mode)) } }
        }
    }
}

@Composable
private fun BodyProgressBackgroundPicker(selected: BodyProgressBackgroundStyle?, onSelect: (BodyProgressBackgroundStyle?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val labels = mapOf(
        BodyProgressBackgroundStyle.White to androidx.compose.ui.res.stringResource(DesignR.string.white),
        BodyProgressBackgroundStyle.Gradient to androidx.compose.ui.res.stringResource(DesignR.string.gradient),
        BodyProgressBackgroundStyle.Blurred to androidx.compose.ui.res.stringResource(DesignR.string.blurred),
    )
    Box {
        FooterMenuLabel(androidx.compose.ui.res.stringResource(DesignR.string.background), selected?.let { labels.getValue(it) } ?: androidx.compose.ui.res.stringResource(DesignR.string.ui_sheet_original)) { expanded = true }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(onClick = { expanded = false; onSelect(null) }) { Text(androidx.compose.ui.res.stringResource(DesignR.string.ui_sheet_original)) }
            BodyProgressBackgroundStyle.entries.forEach { style -> DropdownMenuItem(onClick = { expanded = false; onSelect(style) }) { Text(labels.getValue(style)) } }
        }
    }
}

@Composable
private fun FooterMenuLabel(title: String, value: String, onClick: () -> Unit) {
    Row(Modifier.clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column { Text(title, style = MaterialTheme.typography.overline, color = WildforceThemeTokens.textSecondary); Text(value, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textPrimary) }
        Icon(Icons.Filled.ArrowDropDown, null, tint = WildforceThemeTokens.textSecondary)
    }
}

@Composable
private fun ComparisonMetadata(session: BodyProgressSession, metricSystem: MetricSystem, isFuture: Boolean) {
    val context = LocalContext.current
    val weight = remember(session.date, metricSystem, isFuture) {
        if (isFuture) null else nearestProgressWeight(session.date, BodyMetricsStore.load(context))?.let { value ->
            val displayed = if (metricSystem == MetricSystem.Imperial) value * 2.2046226218 else value
            String.format(Locale.getDefault(), "%.1f %s", displayed, if (metricSystem == MetricSystem.Imperial) "lb" else "kg")
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(if (isFuture) "+6mo" else progressPhotoDate(session.date, includeYear = true), Modifier.clip(RoundedCornerShape(24.dp)).background(Color.Black.copy(alpha = .55f)).padding(horizontal = 10.dp, vertical = 7.dp), style = MaterialTheme.typography.caption, color = Color.White)
        weight?.let { Text(it, Modifier.padding(horizontal = 8.dp), style = MaterialTheme.typography.caption, color = Color.White) }
    }
}

internal fun progressPhotoDate(date: String, includeYear: Boolean, locale: Locale = Locale.getDefault()): String =
    runCatching {
        val parsed = LocalDate.parse(date)
        if (includeYear) parsed.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))
        else parsed.format(DateTimeFormatter.ofPattern("MMM d", locale))
    }.getOrDefault(date)

internal fun nearestProgressWeight(date: String, entries: List<BodyMetricEntry>): Double? {
    val photoDate = runCatching { LocalDate.parse(date) }.getOrNull() ?: return null
    return entries.asSequence().mapNotNull { entry ->
        val entryDate = runCatching { LocalDate.parse(entry.date) }.getOrNull() ?: return@mapNotNull null
        val value = entry.weightKg ?: return@mapNotNull null
        val distance = kotlin.math.abs(ChronoUnit.DAYS.between(photoDate, entryDate))
        if (distance <= 7) Triple(distance, entryDate, value) else null
    }.sortedWith(compareBy<Triple<Long, LocalDate, Double>> { it.first }.thenByDescending { it.second })
        .firstOrNull()?.third
}

@Composable
private fun FutureProgressDialog(image: Bitmap?, loading: Boolean, error: String?, onDismiss: () -> Unit, onGenerate: () -> Unit, onCompare: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Card(shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp), backgroundColor = WildforceThemeTokens.surfaceElevated, modifier = Modifier.fillMaxWidth().heightIn(max = 650.dp)) {
            Column(Modifier.padding(20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(Modifier.align(Alignment.CenterHorizontally).width(36.dp).height(4.dp).clip(RoundedCornerShape(4.dp)).background(WildforceThemeTokens.textSecondary.copy(alpha = .4f)))
                Text(androidx.compose.ui.res.stringResource(if (loading) DesignR.string.building_your_forecast else if (error != null) DesignR.string.forecast_unavailable else DesignR.string.your_6_month_forecast), style = MaterialTheme.typography.h6, color = WildforceThemeTokens.textPrimary)
                Text(androidx.compose.ui.res.stringResource(if (image == null) DesignR.string.generate_a_realistic_preview_based_on_your_latest_check_in__training_plan__and_nutrition_profile_ else DesignR.string.this_is_a_realistic_projection_based_on_your_training_and_nutrition_plan__select__compare__to_see_it_alongside_your_current_check_in_), style = MaterialTheme.typography.body2, color = WildforceThemeTokens.textSecondary)
                if (image != null && !loading) Image(image.asImageBitmap(), null, Modifier.fillMaxWidth().heightIn(max = 340.dp).clip(RoundedCornerShape(18.dp)), contentScale = ContentScale.Fit)
                else listOf(DesignR.string.training_plan, DesignR.string.ui_sheet_nutrition_profile, DesignR.string.realistic_projection).forEach { Text(androidx.compose.ui.res.stringResource(it), color = WildforceThemeTokens.textPrimary) }
                error?.let { Text(it, color = Color.Red, style = MaterialTheme.typography.caption) }
                Button(onClick = if (image != null && !loading) onCompare else onGenerate, enabled = !loading, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary)) {
                    if (loading) CircularProgressIndicator(Modifier.size(20.dp), color = WildforceThemeTokens.primaryButtonText, strokeWidth = 2.dp)
                    else Text(androidx.compose.ui.res.stringResource(if (image != null) DesignR.string.ui_sheet_compare else DesignR.string.generate), color = WildforceThemeTokens.primaryButtonText)
                }
            }
        }
        }
    }
}

@Composable private fun ComparePhoto(
    session: BodyProgressSession,
    path: String,
    label: String,
    backgroundStyle: BodyProgressBackgroundStyle?,
    modifier: Modifier,
    metricSystem: MetricSystem,
    isFuture: Boolean = false,
) = Box(modifier.clip(RoundedCornerShape(18.dp)).background(WildforceThemeTokens.textSecondary.copy(alpha = .10f))) {
    Photo(path, backgroundStyle, Modifier.fillMaxSize())
    if (path.isEmpty()) Text(androidx.compose.ui.res.stringResource(R.string.body_photo_unavailable), Modifier.align(Alignment.Center), color = WildforceThemeTokens.textSecondary, textAlign = TextAlign.Center)
}
@Composable private fun TopBar(title: String, close: () -> Unit, actionLoading: Boolean = false, action: (() -> Unit)? = null) = Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(48.dp).clickable(onClick = close), contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Filled.ArrowBack, androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.ui_sheet_go_back), Modifier.size(22.dp), WildforceThemeTokens.textPrimary) }; Text(title, Modifier.weight(1f).padding(horizontal = 12.dp), style = MaterialTheme.typography.h6, color = WildforceThemeTokens.textPrimary); action?.let { Box(Modifier.size(48.dp).clickable(enabled = !actionLoading, onClick = it), contentAlignment = Alignment.Center) { if (actionLoading) CircularProgressIndicator(Modifier.size(20.dp), color = WildforceThemeTokens.accent, strokeWidth = 2.dp) else Icon(Icons.Filled.Analytics, androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.body_scan_analysis), Modifier.size(24.dp), WildforceThemeTokens.accent) } } }
@Composable
private fun Photo(
    path: String,
    backgroundStyle: BodyProgressBackgroundStyle? = null,
    modifier: Modifier = Modifier,
) {
    val bitmap = remember(path) { BitmapFactory.decodeFile(path) }
    val styledBitmap by produceState(initialValue = bitmap, key1 = path, key2 = backgroundStyle) {
        value = bitmap
        if (bitmap != null && backgroundStyle != null) {
            value = BodyProgressBackgroundRemoval.process(bitmap, backgroundStyle) ?: bitmap
        }
    }
    if (styledBitmap != null) Image(styledBitmap!!.asImageBitmap(), null, modifier.clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop) else Box(modifier.background(WildforceThemeTokens.textSecondary.copy(alpha = .12f)))
}
