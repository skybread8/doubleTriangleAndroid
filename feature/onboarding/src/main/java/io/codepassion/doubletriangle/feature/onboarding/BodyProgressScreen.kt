package io.codepassion.doubletriangle.feature.onboarding

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.codepassion.doubletriangle.core.designsystem.WildforceThemeTokens
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Year

/** Full-screen check-in flow: paired capture, gallery, comparison and future preview. */
@Composable
internal fun BodyProgressScreen(
    profile: OnboardingProfile,
    generateFuturePreview: suspend (String, String) -> Bitmap,
    loadFuturePreview: suspend (String, String) -> Bitmap?,
    analyzeComposition: suspend (String) -> BodyCompositionAnalysis,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    var sessions by remember { mutableStateOf(BodyProgressStore.load(context)) }
    var screen by remember { mutableStateOf<Screen>(Screen.Gallery) }
    val preferences = remember(context) { context.getSharedPreferences("wildforce_body_progress", 0) }
    var showGalleryTutorial by remember { mutableStateOf(!preferences.getBoolean("gallery_tutorial_v1", false)) }
    if (showGalleryTutorial) {
        BodyProgressTutorial(
            onFinish = {
                preferences.edit().putBoolean("gallery_tutorial_v1", true).apply()
                showGalleryTutorial = false
            },
            onClose = onClose,
        )
        return
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
                BodyProgressStore.delete(context, session); sessions = BodyProgressStore.load(context)
            }
            Screen.Capture -> Capture(sessions, { screen = Screen.Gallery }) { sessions = BodyProgressStore.load(context); screen = Screen.Gallery }
            is Screen.Compare -> Compare(sessions, destination.id, profile, { screen = Screen.Gallery }, generateFuturePreview, loadFuturePreview, analyzeComposition)
        }
    }
}

private sealed class Screen { data object Gallery : Screen(); data object Capture : Screen(); data class Compare(val id: String) : Screen() }

@Composable private fun BodyProgressTutorial(onFinish: () -> Unit, onClose: () -> Unit) {
    var step by remember { mutableStateOf(0) }
    val titles = listOf("Sigue tu progreso visual", "Mantén la consistencia", "Compara con claridad")
    val eyebrows = listOf("REGISTRA TU CAMBIO", "MISMO ENCUADRE", "REVISA TU EVOLUCIÓN")
    val descriptions = listOf(
        "Cada check-in guarda una foto frontal y otra de perfil para que puedas ver los cambios reales.",
        "La misma luz, distancia, ropa y postura hacen que cada comparación sea más útil.",
        "Elige cualquier check-in de la galería y compáralo por ángulo con tu referencia inicial.",
    )
    val cards = listOf(
        "Frontal  +  Perfil",
        "• Luz similar\n• Dentro de la guía\n• Postura natural",
        "Galería  →  Comparar",
    )
    Column(Modifier.fillMaxSize().background(WildforceThemeTokens.backgroundSecondary).padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Paso ${step + 1} de 3", Modifier.weight(1f), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
            Text("Omitir", Modifier.heightIn(min = 48.dp).clickable(onClick = onFinish).padding(horizontal = 8.dp, vertical = 8.dp), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { repeat(3) { index -> Box(Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(6.dp)).background(if (index == step) WildforceThemeTokens.textPrimary else WildforceThemeTokens.textPrimary.copy(alpha = .14f))) } }
        Spacer(Modifier.height(22.dp))
        Box(Modifier.size(54.dp).clip(RoundedCornerShape(16.dp)).background(WildforceThemeTokens.textPrimary), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.PhotoCamera, null, Modifier.size(25.dp), WildforceThemeTokens.background)
        }
        Text(eyebrows[step], style = MaterialTheme.typography.caption, color = WildforceThemeTokens.accent)
        Text(titles[step], style = MaterialTheme.typography.h5, color = WildforceThemeTokens.textPrimary)
        Text(descriptions[step], style = MaterialTheme.typography.body1, color = WildforceThemeTokens.textSecondary)
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(WildforceThemeTokens.textPrimary.copy(alpha = .08f)).padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(if (step == 1) "Para una buena comparación" else if (step == 0) "Qué vas a capturar" else "Cómo funciona", style = MaterialTheme.typography.subtitle2, color = WildforceThemeTokens.textPrimary)
            Text(cards[step], style = MaterialTheme.typography.body2, color = WildforceThemeTokens.textSecondary)
        }
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (step > 0) Text("Atrás", Modifier.heightIn(min = 48.dp).clickable { step-- }.padding(horizontal = 12.dp, vertical = 12.dp), color = WildforceThemeTokens.textSecondary)
            Button(onClick = { if (step == 2) onFinish() else step++ }, colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary, contentColor = WildforceThemeTokens.primaryButtonText), modifier = Modifier.weight(1f)) { Text(if (step == 2) "Entendido" else "Siguiente", color = WildforceThemeTokens.primaryButtonText) }
        }
        Text("Volver", Modifier.align(Alignment.CenterHorizontally).heightIn(min = 48.dp).clickable(onClick = onClose).padding(horizontal = 8.dp, vertical = 8.dp), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
    }
}

@Composable private fun Gallery(sessions: List<BodyProgressSession>, close: () -> Unit, capture: () -> Unit, open: (BodyProgressSession) -> Unit, delete: (BodyProgressSession) -> Unit) {
    var pendingDelete by remember { mutableStateOf<BodyProgressSession?>(null) }
    var tipsOpen by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(WildforceThemeTokens.background)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clickable(onClick = close), contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", Modifier.size(22.dp), WildforceThemeTokens.textPrimary) }
            Spacer(Modifier.weight(1f))
            Box(Modifier.size(48.dp).clickable { tipsOpen = true }, contentAlignment = Alignment.Center) { Icon(Icons.Filled.Info, "Ver consejos para las fotos", Modifier.size(25.dp), WildforceThemeTokens.textPrimary) }
            Spacer(Modifier.width(18.dp))
            Box(Modifier.size(48.dp).clickable(onClick = capture), contentAlignment = Alignment.Center) { Icon(Icons.Filled.Add, "Añadir fotos de progreso", Modifier.size(29.dp), WildforceThemeTokens.accent) }
        }
        if (sessions.isEmpty()) EmptyGallery(capture) else {
            LazyVerticalGrid(GridCells.Fixed(3), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f).padding(horizontal = 16.dp, vertical = 4.dp)) {
                items(sessions.sortedByDescending { it.date }, key = { it.id }) { session ->
                    Box(Modifier.height(138.dp).clip(RoundedCornerShape(18.dp)).background(WildforceThemeTokens.textSecondary.copy(alpha = .10f)).clickable { open(session) }) {
                        Photo(session.frontPath, modifier = Modifier.fillMaxSize())
                        Box(Modifier.fillMaxWidth().height(52.dp).align(Alignment.BottomCenter).background(Color.Black.copy(alpha = .42f)))
                        Text(session.date, Modifier.align(Alignment.BottomStart).padding(10.dp), style = MaterialTheme.typography.caption, color = Color.White)
                        Box(Modifier.align(Alignment.TopEnd).size(48.dp).clickable { pendingDelete = session }, contentAlignment = Alignment.Center) { Icon(Icons.Filled.Delete, "Eliminar check-in", Modifier.size(18.dp), Color.White.copy(alpha = .9f)) }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
    pendingDelete?.let { session -> AlertDialog(onDismissRequest = { pendingDelete = null }, title = { Text("¿Eliminar este check-in?") }, text = { Text("Se borrarán las fotos frontal y de perfil. Esta acción no se puede deshacer.") }, confirmButton = { Button(onClick = { delete(session); pendingDelete = null }) { Text("Eliminar") } }, dismissButton = { Button(onClick = { pendingDelete = null }) { Text("Cancelar") } }) }
    if (tipsOpen) BestPracticesDialog { tipsOpen = false }
}

@Composable private fun EmptyGallery(capture: () -> Unit) = Column(Modifier.fillMaxSize().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
    Spacer(Modifier.height(62.dp))
    Box(Modifier.size(110.dp).clip(RoundedCornerShape(30.dp)).background(WildforceThemeTokens.accent.copy(alpha = .16f)), contentAlignment = Alignment.Center) {
        Icon(Icons.Filled.PhotoCamera, null, Modifier.size(44.dp), WildforceThemeTokens.accent)
    }
    Text("Sigue tu físico con check-ins consistentes", style = MaterialTheme.typography.h6, color = WildforceThemeTokens.textPrimary, textAlign = TextAlign.Center)
    Text("Captura fotos frontal y de perfil con guías para que cada check-in sea fácil de comparar con el tiempo.", style = MaterialTheme.typography.body2, color = WildforceThemeTokens.textSecondary, textAlign = TextAlign.Center)
    Button(onClick = capture, colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary, contentColor = WildforceThemeTokens.primaryButtonText), modifier = Modifier.fillMaxWidth()) { Text("Hacer mis primeras fotos de progreso", color = WildforceThemeTokens.primaryButtonText) }
    Spacer(Modifier.weight(1f))
}

@Composable private fun BestPracticesDialog(close: () -> Unit) = AlertDialog(
    onDismissRequest = close,
    title = { Text("Consejos para las fotos de progreso") },
    text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Mantén tus fotos de progreso lo más consistentes posible para que los cambios sean fáciles de valorar.")
        Text("• Usa la misma ropa, ubicación y luz.\n• Coloca el móvil a la misma distancia y altura.\n• Mantén una postura relajada y natural.\n• Usa la referencia de tu último check-in para alinear el encuadre.", color = WildforceThemeTokens.textSecondary)
    } },
    confirmButton = { Button(onClick = close) { Text("Entendido") } }
)

@Composable private fun Capture(sessions: List<BodyProgressSession>, close: () -> Unit, saved: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var step by remember { mutableStateOf(Step.Front) }; var front by remember { mutableStateOf<Bitmap?>(null) }; var profile by remember { mutableStateOf<Bitmap?>(null) }; var error by remember { mutableStateOf<String?>(null) }
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
        if (step == Step.Front) front = cropped else profile = cropped
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
    val image = if (step == Step.Front) front else profile
    val referencePath = sessions.maxByOrNull { it.date }?.let { if (step == Step.Front) it.frontPath else it.profilePath }
    var ghostScale by remember(step, referencePath) { mutableStateOf(1f) }
    var ghostOffset by remember(step, referencePath) { mutableStateOf(Offset.Zero) }
    fun startCamera() {
        if (!cameraPermissionGranted) { requestCameraPermission.launch(Manifest.permission.CAMERA); return }
        val capture = { cameraCaptureAction?.invoke() }
        if (timerSeconds == 0) capture() else scope.launch { for (remaining in timerSeconds downTo 1) { countdown = remaining; delay(1_000) }; countdown = null; capture() }
    }
    fun toggleVoiceCapture() {
        if (!microphonePermissionGranted) {
            requestMicrophonePermission.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            voiceCapture.toggle { startCamera() }
        }
    }
    LaunchedEffect(startVoiceAfterPermission) {
        if (startVoiceAfterPermission) {
            startVoiceAfterPermission = false
            voiceCapture.toggle { startCamera() }
        }
    }
    Column(Modifier.fillMaxSize().background(Color.Black).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Cerrar", Modifier.heightIn(min = 48.dp).clickable(onClick = close).padding(vertical = 10.dp), color = Color.White, style = MaterialTheme.typography.body2)
            Row(Modifier.weight(1f).padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) { repeat(2) { index -> Box(Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(6.dp)).background(if (index < if (step == Step.Front) 1 else 2) Color.White else Color.White.copy(alpha = .22f))) } }
            Box(Modifier.size(48.dp).clickable {
                useFrontCamera = !useFrontCamera
                context.getSharedPreferences("wildforce_body_progress", 0).edit().putBoolean("preferred_front_camera", useFrontCamera).apply()
            }, contentAlignment = Alignment.Center) { Icon(Icons.Filled.Cameraswitch, "Cambiar cámara", Modifier.size(24.dp), Color.White) }
            Spacer(Modifier.width(14.dp))
            Box(Modifier.size(48.dp).clickable { timerSeconds = when (timerSeconds) { 0 -> 3; 3 -> 5; else -> 0 } }, contentAlignment = Alignment.Center) { Icon(Icons.Filled.Timer, "Temporizador", Modifier.size(23.dp), if (timerSeconds == 0) Color.White else WildforceThemeTokens.accent) }
        }
        Text("${if (step == Step.Front) "1" else "2"} de 2 · ${step.label}", color = Color.White, style = MaterialTheme.typography.h6)
        Text(step.help, color = Color.White.copy(alpha = .78f), style = MaterialTheme.typography.body2)
        if (voiceState == BodyProgressVoiceCaptureController.State.Listening) {
            Text("Di ${voiceCapture.triggerDisplayWord} para hacer la foto", color = WildforceThemeTokens.accent, style = MaterialTheme.typography.caption, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
        Box(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(20.dp)).background(Color.DarkGray), contentAlignment = Alignment.Center) {
            if (image == null) {
                if (cameraPermissionGranted) {
                    ProgressPhotoCamera(
                        useFrontCamera = useFrontCamera,
                        modifier = Modifier.fillMaxSize(),
                        onReady = { cameraCaptureAction = it },
                        onPhotoCaptured = ::assignCapturedImage,
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
                    TiltIndicator(tiltState, Modifier.align(Alignment.CenterEnd))
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) { Text(if (overlay == Overlay.Reference && referencePath != null) "Arrastra y pellizca para alinear tu último check-in" else "Alinea tu cuerpo dentro de la guía", color = Color.White, textAlign = TextAlign.Center) }
                } else {
                    Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Icon(Icons.Filled.PhotoCamera, null, Modifier.size(52.dp), Color.White)
                        Text("Necesitamos acceso a la cámara para usar las guías de encuadre.", color = Color.White, textAlign = TextAlign.Center)
                        Button(onClick = { requestCameraPermission.launch(Manifest.permission.CAMERA) }, colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary, contentColor = WildforceThemeTokens.primaryButtonText)) { Text("Permitir cámara", color = WildforceThemeTokens.primaryButtonText) }
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
                        step.label,
                        Modifier.fillMaxSize().graphicsLayer {
                            scaleX = reviewScale
                            scaleY = reviewScale
                            translationX = reviewOffset.x
                            translationY = reviewOffset.y
                        },
                        contentScale = ContentScale.Crop,
                    )
                    Text("Arrastra y pellizca para ajustar", color = Color.White.copy(alpha = .82f), style = MaterialTheme.typography.caption, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp))
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
        Toggle(listOf(Overlay.Guide, Overlay.Reference), overlay, { it.label }) { overlay = it }
        if (image == null) {
            Text(if (timerSeconds == 0) "Temporizador desactivado" else "Temporizador de ${timerSeconds} segundos", color = Color.White.copy(alpha = .72f), style = MaterialTheme.typography.caption, modifier = Modifier.align(Alignment.CenterHorizontally))
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(48.dp).clip(RoundedCornerShape(24.dp)).background(Color.White.copy(alpha = .16f)).clickable { gallery.launch("image/*") }, contentAlignment = Alignment.Center) { Icon(Icons.Filled.PhotoCamera, "Elegir de galería", Modifier.size(24.dp), Color.White) }
                    Text("Galería", color = Color.White, style = MaterialTheme.typography.caption)
                }
                Box(Modifier.size(82.dp).clip(RoundedCornerShape(41.dp)).background(Color.White).clickable(enabled = countdown == null, onClick = ::startCamera), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(68.dp).clip(RoundedCornerShape(34.dp)).background(if (countdown == null) WildforceThemeTokens.accent else Color.Gray))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(48.dp).clip(RoundedCornerShape(24.dp)).background(Color.White.copy(alpha = .16f)).clickable { timerSeconds = when (timerSeconds) { 0 -> 3; 3 -> 5; else -> 0 } }, contentAlignment = Alignment.Center) { Icon(Icons.Filled.Timer, "Cambiar temporizador", Modifier.size(24.dp), Color.White) }
                    Text(if (countdown == null) "Disparar" else "Preparando", color = Color.White, style = MaterialTheme.typography.caption)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(48.dp).clip(RoundedCornerShape(24.dp)).background(Color.White.copy(alpha = .16f)).clickable(enabled = countdown == null, onClick = ::toggleVoiceCapture), contentAlignment = Alignment.Center) { Icon(if (voiceState == BodyProgressVoiceCaptureController.State.Listening) Icons.Filled.Mic else Icons.Filled.MicNone, "Activar captura por voz", Modifier.size(24.dp), if (voiceState == BodyProgressVoiceCaptureController.State.Listening) WildforceThemeTokens.accent else Color.White) }
                    Text(if (voiceState == BodyProgressVoiceCaptureController.State.Listening) "Escuchando" else "Voz", color = Color.White, style = MaterialTheme.typography.caption)
                }
            }
        } else {
            Button(onClick = {
                val adjusted = renderAdjustedBitmap(image, reviewScale, reviewOffset, reviewViewport)
                if (step == Step.Front) {
                    front = adjusted
                    step = Step.Profile
                } else {
                    profile = adjusted
                    runCatching { BodyProgressStore.save(context, front!!, adjusted) }.onSuccess { saved() }.onFailure { error = "No se pudieron guardar las fotos. Inténtalo de nuevo." }
                }
            }, colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary, contentColor = WildforceThemeTokens.primaryButtonText), modifier = Modifier.fillMaxWidth()) { Text(if (step == Step.Front) "Continuar al perfil" else "Guardar check-in", color = WildforceThemeTokens.primaryButtonText) }
            Button(onClick = { if (step == Step.Front) front = null else profile = null }, modifier = Modifier.fillMaxWidth()) { Text("Repetir foto") }
        }
        error?.let { Text(it, color = Color.Red) }
    }
    if (tutorial) AlertDialog(onDismissRequest = { tutorial = false; context.getSharedPreferences("wildforce_body_progress", 0).edit().putBoolean("capture_tutorial_v1", true).apply() }, title = { Text("Check-ins consistentes") }, text = { Text("Haz una foto frontal y otra de perfil. Mantén la misma ropa, distancia, postura y luz. Puedes activar Referencia para usar tu último check-in como guía.") }, confirmButton = { Button(onClick = { tutorial = false; context.getSharedPreferences("wildforce_body_progress", 0).edit().putBoolean("capture_tutorial_v1", true).apply() }) { Text("Entendido") } })
}
private enum class Step(val label: String, val help: String) { Front("Frontal", "Misma hora, distancia y luz que en tus próximos check-ins."), Profile("Perfil", "Gira de lado y conserva la ropa, postura y encuadre.") }
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
            Text(if (state.leanDegrees < 0) "Inclina hacia delante" else "Inclina hacia atrás", color = Color.White.copy(alpha = .82f), style = MaterialTheme.typography.overline, textAlign = TextAlign.Center, modifier = Modifier.width(68.dp).padding(top = 8.dp))
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
) {
    val context = LocalContext.current; val scope = rememberCoroutineScope()
    var selectedId by remember { mutableStateOf(initialId) }; var referenceId by remember { mutableStateOf<String?>(null) }; var angle by remember { mutableStateOf(Angle.Front) }; var comparisonMode by remember { mutableStateOf(ComparisonMode.Reveal) }; var revealReference by remember { mutableStateOf(false) }
    var splitProgress by remember { mutableFloatStateOf(.5f) }; var splitWidthPx by remember { mutableIntStateOf(1) }; var loopShowsReference by remember { mutableStateOf(false) }; var historyLoopIndex by remember { mutableIntStateOf(0) }; var selectedBackgroundStyle by remember { mutableStateOf<BodyProgressBackgroundStyle?>(null) }
    val current = sessions.firstOrNull { it.id == selectedId } ?: sessions.last()
    val referenceSessions = remember(sessions, current.id) { sessions.filterNot { it.id == current.id }.sortedBy { it.date } }
    val activeReferenceIndex = referenceSessions.indexOfFirst { it.id == referenceId }.takeIf { it >= 0 } ?: 0
    val before = referenceSessions.getOrNull(activeReferenceIndex) ?: current
    val historySessions = remember(sessions, current.id) { sessions.sortedBy { it.date }.takeWhile { it.date <= current.date } }
    val historySession = historySessions.getOrNull(historyLoopIndex) ?: current
    val prefs = remember { context.getSharedPreferences("wildforce_body_progress", 0) }; var consent by remember { mutableStateOf(prefs.getLong("future_preview_ai_consent_v1", 0) > 0) }; var dialog by remember { mutableStateOf(false) }; var loading by remember { mutableStateOf(false) }; var future by remember(current.id, angle) { mutableStateOf<Bitmap?>(null) }; var failure by remember { mutableStateOf<String?>(null) }; var analysis by remember(current.id) { mutableStateOf<BodyCompositionAnalysis?>(null) }; var analysisLoading by remember { mutableStateOf(false) }; var analysisError by remember { mutableStateOf<String?>(null) }; var showAnalysisIntro by remember { mutableStateOf(false) }
    fun path(session: BodyProgressSession) = if (angle == Angle.Front) session.frontPath else session.profilePath
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
    LaunchedEffect(current.id, angle) {
        loading = true
        future = runCatching { loadFuture(current.id, angle.apiValue) }.getOrNull()
        loading = false
    }
    LaunchedEffect(current.id, referenceSessions.map { it.id }) {
        if (referenceSessions.none { it.id == referenceId }) referenceId = referenceSessions.firstOrNull()?.id
    }
    Column(Modifier.fillMaxSize().background(WildforceThemeTokens.background).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TopBar("Comparar progreso", close, actionLoading = analysisLoading) { showAnalysisIntro = true }
        Toggle(listOf(Angle.Front, Angle.Profile), angle, { it.label }) { angle = it }
        Toggle(ComparisonMode.entries.toList(), comparisonMode, { it.label }) { comparisonMode = it }
        BodyProgressBackgroundPicker(selectedBackgroundStyle) { selectedBackgroundStyle = it }
        if (referenceSessions.size > 1) {
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
        if (comparisonMode == ComparisonMode.SideBySide) Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) { ComparePhoto(before, path(before), "Antes", selectedBackgroundStyle, Modifier.weight(1f)); ComparePhoto(current, path(current), "Ahora", selectedBackgroundStyle, Modifier.weight(1f)) }
        else if (comparisonMode == ComparisonMode.Split) Box(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(18.dp)).onSizeChanged { splitWidthPx = it.width.coerceAtLeast(1) }.pointerInput(before.id, current.id) {
            detectDragGestures { change, dragAmount ->
                change.consume()
                splitProgress = (splitProgress + dragAmount.x / splitWidthPx.toFloat()).coerceIn(.02f, .98f)
            }
        }) {
            ComparePhoto(current, path(current), "Ahora", selectedBackgroundStyle, Modifier.fillMaxSize())
            Box(Modifier.fillMaxWidth(splitProgress).fillMaxHeight().clip(RoundedCornerShape(18.dp))) {
                ComparePhoto(before, path(before), "Antes", selectedBackgroundStyle, Modifier.fillMaxSize())
            }
            Canvas(Modifier.fillMaxSize()) {
                val dividerX = size.width * splitProgress
                drawLine(Color.White.copy(alpha = .92f), Offset(dividerX, 0f), Offset(dividerX, size.height), strokeWidth = 2.dp.toPx())
            }
        }
        else Box(Modifier.fillMaxWidth().weight(1f).pointerInput(before.id, current.id, comparisonMode, historySession.id) {
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
                ComparePhoto(item, path(item), label, selectedBackgroundStyle, Modifier.fillMaxSize())
            }
        }
        if (sessions.size > 1) {
            val timeline = sessions.sortedBy { it.date }
            val selectedIndex = timeline.indexOfFirst { it.id == current.id }.coerceAtLeast(0)
            Slider(
                value = selectedIndex.toFloat(),
                onValueChange = { selectedId = timeline[it.toInt().coerceIn(0, timeline.lastIndex)].id },
                valueRange = 0f..timeline.lastIndex.toFloat(),
                steps = (timeline.size - 2).coerceAtLeast(0),
                colors = SliderDefaults.colors(thumbColor = WildforceThemeTokens.accent, activeTrackColor = WildforceThemeTokens.accent)
            )
            Row(Modifier.fillMaxWidth()) {
                Text(timeline.first().date, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
                Spacer(Modifier.weight(1f))
                Text(timeline.last().date, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
            }
        }
        if (profile.birthYear <= Year.now().value - 18) {
            Button(onClick = { if (consent) { loading = true; failure = null; scope.launch { runCatching { generateFuture(current.id, angle.apiValue) }.onSuccess { future = it; BodyProgressStore.saveFuture(context, current, it) }.onFailure { failure = it.message ?: "No se pudo crear la previsión" }; loading = false } } else dialog = true }, enabled = !loading, colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary, contentColor = WildforceThemeTokens.primaryButtonText), modifier = Modifier.fillMaxWidth()) { if (loading) CircularProgressIndicator(Modifier.size(18.dp), color = WildforceThemeTokens.primaryButtonText, strokeWidth = 2.dp) else Text(if (future == null) "Ver previsión a 6 meses" else "Regenerar previsión", color = WildforceThemeTokens.primaryButtonText) }
            future?.let { Image(it.asImageBitmap(), "Previsión a 6 meses", Modifier.fillMaxWidth().height(250.dp).clip(RoundedCornerShape(14.dp)), contentScale = ContentScale.Crop) }; failure?.let { Text(it, color = Color.Red, style = MaterialTheme.typography.caption) }
            if (consent) Text("Retirar consentimiento", Modifier.clickable { prefs.edit().remove("future_preview_ai_consent_v1").apply(); consent = false }, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
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
                            .onFailure { analysisError = it.message ?: "No se pudo analizar el check-in" }
                        analysisLoading = false
                    }
                },
            )
        }
        analysis?.let { BodyCompositionResult(it) }
    }
    if (dialog) AlertDialog(onDismissRequest = { dialog = false }, title = { Text("Vista a futuro con IA") }, text = { Text("Enviaremos una copia de tu foto frontal y datos básicos de entrenamiento a la API de OpenAI para crear una previsión de 6 meses. El contenido no se usa para entrenar modelos; puede conservarse para monitorización de abuso durante hasta 30 días. Confirmas que tienes 18 años o más y aceptas este tratamiento.") }, confirmButton = { Button(onClick = { prefs.edit().putLong("future_preview_ai_consent_v1", System.currentTimeMillis()).apply(); consent = true; dialog = false }) { Text("Acepto") } }, dismissButton = { Button(onClick = { dialog = false }) { Text("Cancelar") } })
}

@Composable
private fun BodyCompositionResult(analysis: BodyCompositionAnalysis) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), backgroundColor = WildforceThemeTokens.surfaceElevated) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Análisis de composición", style = MaterialTheme.typography.h6, color = WildforceThemeTokens.textPrimary)
            Text("Grasa estimada: ${analysis.estimatedBodyFatRange}", color = WildforceThemeTokens.textPrimary)
            Text("Confianza: ${analysis.confidence.name}", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
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
            Text(if (loading) "Analizando tu check-in" else if (error == null) "Estimar composición corporal" else "Estimación no disponible", style = MaterialTheme.typography.h6, color = WildforceThemeTokens.textPrimary)
            Text(
                if (loading) "Revisamos tus fotos frontal y de perfil cuando están disponibles y calculamos un rango orientativo."
                else "Analizaremos tus fotos con IA para ofrecer un rango no médico de grasa corporal y algunas observaciones. La precisión puede variar según postura, ropa, luz y calidad de imagen.",
                style = MaterialTheme.typography.body2,
                color = WildforceThemeTokens.textSecondary,
            )
            if (loading) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(20.dp), color = WildforceThemeTokens.accent, strokeWidth = 2.dp)
                    Text("Analizando fotos…", color = WildforceThemeTokens.textPrimary)
                }
            } else {
                error?.let { Text(it, color = Color.Red, style = MaterialTheme.typography.caption) }
                Button(onClick = onStart, colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary), modifier = Modifier.fillMaxWidth()) {
                    Text(if (error == null) "Iniciar estimación" else "Reintentar", color = WildforceThemeTokens.primaryButtonText)
                }
            }
            Text("El resultado es orientativo y no sustituye asesoramiento médico.", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
        }
    }
}
private enum class Angle(val label: String, val apiValue: String) { Front("Frontal", "front"), Profile("Perfil", "profile") }
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
private fun BodyProgressBackgroundPicker(
    selected: BodyProgressBackgroundStyle?,
    onSelect: (BodyProgressBackgroundStyle?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Text(
            selected?.label ?: "Fondo original",
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(WildforceThemeTokens.textSecondary.copy(alpha = .10f))
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 9.dp),
            style = MaterialTheme.typography.caption,
            color = WildforceThemeTokens.textPrimary,
            textAlign = TextAlign.Center,
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(onClick = { onSelect(null); expanded = false }) { Text("Original") }
            BodyProgressBackgroundStyle.entries.forEach { style ->
                DropdownMenuItem(onClick = { onSelect(style); expanded = false }) { Text(style.label) }
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
) = Box(modifier.clip(RoundedCornerShape(18.dp)).background(WildforceThemeTokens.textSecondary.copy(alpha = .10f))) {
    Photo(path, backgroundStyle, Modifier.fillMaxSize())
    Box(Modifier.fillMaxWidth().height(48.dp).align(Alignment.BottomCenter).background(Color.Black.copy(alpha = .46f)))
    Text("$label · ${session.date}", Modifier.align(Alignment.BottomStart).padding(10.dp), style = MaterialTheme.typography.caption, color = Color.White)
}
@Composable private fun TopBar(title: String, close: () -> Unit, actionLoading: Boolean = false, action: (() -> Unit)? = null) = Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(48.dp).clickable(onClick = close), contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", Modifier.size(22.dp), WildforceThemeTokens.textPrimary) }; Text(title, Modifier.weight(1f).padding(horizontal = 12.dp), style = MaterialTheme.typography.h6, color = WildforceThemeTokens.textPrimary); action?.let { Box(Modifier.size(48.dp).clickable(enabled = !actionLoading, onClick = it), contentAlignment = Alignment.Center) { if (actionLoading) CircularProgressIndicator(Modifier.size(20.dp), color = WildforceThemeTokens.accent, strokeWidth = 2.dp) else Icon(Icons.Filled.Analytics, "Analizar composición", Modifier.size(24.dp), WildforceThemeTokens.accent) } } }
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
