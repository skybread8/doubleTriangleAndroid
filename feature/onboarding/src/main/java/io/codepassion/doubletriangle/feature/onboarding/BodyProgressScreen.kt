package io.codepassion.doubletriangle.feature.onboarding

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.codepassion.doubletriangle.core.designsystem.WildforceThemeTokens
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Year

/** Full-screen check-in flow: paired capture, gallery, comparison and future preview. */
@Composable
internal fun BodyProgressScreen(profile: OnboardingProfile, generateFuturePreview: suspend (Bitmap, OnboardingProfile) -> Bitmap, onClose: () -> Unit) {
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
    when (val destination = screen) {
        Screen.Gallery -> Gallery(sessions, onClose, { screen = Screen.Capture }, { screen = Screen.Compare(it.id) }) { session ->
            BodyProgressStore.delete(context, session); sessions = BodyProgressStore.load(context)
        }
        Screen.Capture -> Capture(sessions, { screen = Screen.Gallery }) { sessions = BodyProgressStore.load(context); screen = Screen.Gallery }
        is Screen.Compare -> Compare(sessions, destination.id, profile, { screen = Screen.Gallery }, generateFuturePreview)
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
            Text("Omitir", Modifier.clickable(onClick = onFinish).padding(8.dp), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { repeat(3) { index -> Box(Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(6.dp)).background(if (index == step) WildforceThemeTokens.textPrimary else WildforceThemeTokens.textPrimary.copy(alpha = .14f))) } }
        Spacer(Modifier.height(22.dp))
        Box(Modifier.size(54.dp).clip(RoundedCornerShape(16.dp)).background(WildforceThemeTokens.textPrimary), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.PhotoCamera, null, Modifier.size(25.dp), WildforceThemeTokens.background)
        }
        Text(eyebrows[step], style = MaterialTheme.typography.caption, color = WildforceThemeTokens.accentGold)
        Text(titles[step], style = MaterialTheme.typography.h5, color = WildforceThemeTokens.textPrimary)
        Text(descriptions[step], style = MaterialTheme.typography.body1, color = WildforceThemeTokens.textSecondary)
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(WildforceThemeTokens.textPrimary.copy(alpha = .08f)).padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(if (step == 1) "Para una buena comparación" else if (step == 0) "Qué vas a capturar" else "Cómo funciona", style = MaterialTheme.typography.subtitle2, color = WildforceThemeTokens.textPrimary)
            Text(cards[step], style = MaterialTheme.typography.body2, color = WildforceThemeTokens.textSecondary)
        }
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (step > 0) Text("Atrás", Modifier.clickable { step-- }.padding(12.dp), color = WildforceThemeTokens.textSecondary)
            Button(onClick = { if (step == 2) onFinish() else step++ }, colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.accentGold), modifier = Modifier.weight(1f)) { Text(if (step == 2) "Entendido" else "Siguiente", color = Color.White) }
        }
        Text("Volver", Modifier.align(Alignment.CenterHorizontally).clickable(onClick = onClose).padding(8.dp), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
    }
}

@Composable private fun Gallery(sessions: List<BodyProgressSession>, close: () -> Unit, capture: () -> Unit, open: (BodyProgressSession) -> Unit, delete: (BodyProgressSession) -> Unit) {
    var pendingDelete by remember { mutableStateOf<BodyProgressSession?>(null) }
    var tipsOpen by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(WildforceThemeTokens.background)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", Modifier.size(22.dp).clickable(onClick = close), WildforceThemeTokens.textPrimary)
            Spacer(Modifier.weight(1f))
            Icon(Icons.Filled.Info, "Ver consejos para las fotos", Modifier.size(25.dp).clickable { tipsOpen = true }, WildforceThemeTokens.textPrimary)
            Spacer(Modifier.width(18.dp))
            Icon(Icons.Filled.Add, "Añadir fotos de progreso", Modifier.size(29.dp).clickable(onClick = capture), WildforceThemeTokens.accentGold)
        }
        if (sessions.isEmpty()) EmptyGallery(capture) else {
            LazyVerticalGrid(GridCells.Fixed(3), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f).padding(horizontal = 16.dp, vertical = 4.dp)) {
                items(sessions.sortedByDescending { it.date }, key = { it.id }) { session ->
                    Box(Modifier.height(138.dp).clip(RoundedCornerShape(18.dp)).background(WildforceThemeTokens.textSecondary.copy(alpha = .10f)).clickable { open(session) }) {
                        Photo(session.frontPath, Modifier.fillMaxSize())
                        Box(Modifier.fillMaxWidth().height(52.dp).align(Alignment.BottomCenter).background(Color.Black.copy(alpha = .42f)))
                        Text(session.date, Modifier.align(Alignment.BottomStart).padding(10.dp), style = MaterialTheme.typography.caption, color = Color.White)
                        Icon(Icons.Filled.Delete, "Eliminar check-in", Modifier.align(Alignment.TopEnd).padding(7.dp).size(18.dp).clickable { pendingDelete = session }, Color.White.copy(alpha = .9f))
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
    Box(Modifier.size(110.dp).clip(RoundedCornerShape(30.dp)).background(WildforceThemeTokens.accentGold.copy(alpha = .16f)), contentAlignment = Alignment.Center) {
        Icon(Icons.Filled.PhotoCamera, null, Modifier.size(44.dp), WildforceThemeTokens.accentGold)
    }
    Text("Sigue tu físico con check-ins consistentes", style = MaterialTheme.typography.h6, color = WildforceThemeTokens.textPrimary, textAlign = TextAlign.Center)
    Text("Captura fotos frontal y de perfil con guías para que cada check-in sea fácil de comparar con el tiempo.", style = MaterialTheme.typography.body2, color = WildforceThemeTokens.textSecondary, textAlign = TextAlign.Center)
    Button(onClick = capture, colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.accentGold), modifier = Modifier.fillMaxWidth()) { Text("Hacer mis primeras fotos de progreso", color = Color.White) }
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
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> uri?.let { BodyProgressStore.bitmapFrom(context, it) }?.let { if (step == Step.Front) front = it else profile = it } }
    var cameraPermissionGranted by remember { mutableStateOf(context.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    val requestCameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { cameraPermissionGranted = it }
    var useFrontCamera by remember { mutableStateOf(context.getSharedPreferences("wildforce_body_progress", 0).getBoolean("preferred_front_camera", false)) }
    var cameraCaptureAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val image = if (step == Step.Front) front else profile
    val referencePath = sessions.maxByOrNull { it.date }?.let { if (step == Step.Front) it.frontPath else it.profilePath }
    fun startCamera() {
        if (!cameraPermissionGranted) { requestCameraPermission.launch(Manifest.permission.CAMERA); return }
        val capture = { cameraCaptureAction?.invoke() }
        if (timerSeconds == 0) capture() else scope.launch { for (remaining in timerSeconds downTo 1) { countdown = remaining; delay(1_000) }; countdown = null; capture() }
    }
    Column(Modifier.fillMaxSize().background(Color.Black).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Cerrar", Modifier.clickable(onClick = close).padding(vertical = 10.dp), color = Color.White, style = MaterialTheme.typography.body2)
            Row(Modifier.weight(1f).padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) { repeat(2) { index -> Box(Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(6.dp)).background(if (index < if (step == Step.Front) 1 else 2) Color.White else Color.White.copy(alpha = .22f))) } }
            Icon(Icons.Filled.Cameraswitch, "Cambiar cámara", Modifier.size(24.dp).clickable {
                useFrontCamera = !useFrontCamera
                context.getSharedPreferences("wildforce_body_progress", 0).edit().putBoolean("preferred_front_camera", useFrontCamera).apply()
            }, Color.White)
            Spacer(Modifier.width(14.dp))
            Icon(Icons.Filled.Timer, "Temporizador", Modifier.size(23.dp).clickable { timerSeconds = when (timerSeconds) { 0 -> 3; 3 -> 5; else -> 0 } }, if (timerSeconds == 0) Color.White else WildforceThemeTokens.accentGold)
        }
        Text("${if (step == Step.Front) "1" else "2"} de 2 · ${step.label}", color = Color.White, style = MaterialTheme.typography.h6)
        Text(step.help, color = Color.White.copy(alpha = .78f), style = MaterialTheme.typography.body2)
        Box(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(20.dp)).background(Color.DarkGray), contentAlignment = Alignment.Center) {
            if (image == null) {
                if (cameraPermissionGranted) {
                    ProgressPhotoCamera(
                        useFrontCamera = useFrontCamera,
                        modifier = Modifier.fillMaxSize(),
                        onReady = { cameraCaptureAction = it },
                        onPhotoCaptured = { bitmap -> if (step == Step.Front) front = bitmap else profile = bitmap },
                        onFailure = { error = it },
                    )
                    if (overlay == Overlay.Reference && referencePath != null) Photo(referencePath, Modifier.fillMaxSize().graphicsLayer(alpha = .26f))
                    FramingGuideOverlay()
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) { Text(if (overlay == Overlay.Reference && referencePath != null) "Alinea tu postura con el último check-in" else "Alinea tu cuerpo dentro de la guía", color = Color.White, textAlign = TextAlign.Center) }
                } else {
                    Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Icon(Icons.Filled.PhotoCamera, null, Modifier.size(52.dp), Color.White)
                        Text("Necesitamos acceso a la cámara para usar las guías de encuadre.", color = Color.White, textAlign = TextAlign.Center)
                        Button(onClick = { requestCameraPermission.launch(Manifest.permission.CAMERA) }, colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.accentGold)) { Text("Permitir cámara", color = Color.White) }
                    }
                }
            } else Image(image.asImageBitmap(), step.label, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            countdown?.let { Text(it.toString(), style = MaterialTheme.typography.h1, color = Color.White) }
        }
        Toggle(listOf(Overlay.Guide, Overlay.Reference), overlay, { it.label }) { overlay = it }
        if (image == null) {
            Text(if (timerSeconds == 0) "Temporizador desactivado" else "Temporizador de ${timerSeconds} segundos", color = Color.White.copy(alpha = .72f), style = MaterialTheme.typography.caption, modifier = Modifier.align(Alignment.CenterHorizontally))
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.PhotoCamera, "Elegir de galería", Modifier.size(44.dp).clip(RoundedCornerShape(24.dp)).background(Color.White.copy(alpha = .16f)).clickable { gallery.launch("image/*") }.padding(10.dp), Color.White)
                    Text("Galería", color = Color.White, style = MaterialTheme.typography.caption)
                }
                Box(Modifier.size(82.dp).clip(RoundedCornerShape(41.dp)).background(Color.White).clickable(enabled = countdown == null, onClick = ::startCamera), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(68.dp).clip(RoundedCornerShape(34.dp)).background(if (countdown == null) WildforceThemeTokens.accentGold else Color.Gray))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Timer, "Cambiar temporizador", Modifier.size(44.dp).clip(RoundedCornerShape(24.dp)).background(Color.White.copy(alpha = .16f)).clickable { timerSeconds = when (timerSeconds) { 0 -> 3; 3 -> 5; else -> 0 } }.padding(10.dp), Color.White)
                    Text(if (countdown == null) "Disparar" else "Preparando", color = Color.White, style = MaterialTheme.typography.caption)
                }
            }
        } else {
            Button(onClick = { if (step == Step.Front) step = Step.Profile else runCatching { BodyProgressStore.save(context, front!!, profile!!) }.onSuccess { saved() }.onFailure { error = "No se pudieron guardar las fotos. Inténtalo de nuevo." } }, colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.accentGold), modifier = Modifier.fillMaxWidth()) { Text(if (step == Step.Front) "Continuar al perfil" else "Guardar check-in", color = Color.White) }
            Button(onClick = { if (step == Step.Front) front = null else profile = null }, modifier = Modifier.fillMaxWidth()) { Text("Repetir foto") }
        }
        error?.let { Text(it, color = Color.Red) }
    }
    if (tutorial) AlertDialog(onDismissRequest = { tutorial = false; context.getSharedPreferences("wildforce_body_progress", 0).edit().putBoolean("capture_tutorial_v1", true).apply() }, title = { Text("Check-ins consistentes") }, text = { Text("Haz una foto frontal y otra de perfil. Mantén la misma ropa, distancia, postura y luz. Puedes activar Referencia para usar tu último check-in como guía.") }, confirmButton = { Button(onClick = { tutorial = false; context.getSharedPreferences("wildforce_body_progress", 0).edit().putBoolean("capture_tutorial_v1", true).apply() }) { Text("Entendido") } })
}
private enum class Step(val label: String, val help: String) { Front("Frontal", "Misma hora, distancia y luz que en tus próximos check-ins."), Profile("Perfil", "Gira de lado y conserva la ropa, postura y encuadre.") }
private enum class Overlay(val label: String) { Guide("Guía"), Reference("Referencia") }

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

@Composable private fun Compare(sessions: List<BodyProgressSession>, initialId: String, profile: OnboardingProfile, close: () -> Unit, generateFuture: suspend (Bitmap, OnboardingProfile) -> Bitmap) {
    val context = LocalContext.current; val scope = rememberCoroutineScope()
    var selectedId by remember { mutableStateOf(initialId) }; var angle by remember { mutableStateOf(Angle.Front) }; var comparisonMode by remember { mutableStateOf(ComparisonMode.Reveal) }; var revealReference by remember { mutableStateOf(false) }
    val current = sessions.firstOrNull { it.id == selectedId } ?: sessions.last(); val before = sessions.minByOrNull { it.date } ?: current
    val prefs = remember { context.getSharedPreferences("wildforce_body_progress", 0) }; var consent by remember { mutableStateOf(prefs.getLong("future_preview_ai_consent_v1", 0) > 0) }; var dialog by remember { mutableStateOf(false) }; var loading by remember { mutableStateOf(false) }; var future by remember(current.id) { mutableStateOf(current.futurePath?.let(BitmapFactory::decodeFile)) }; var failure by remember { mutableStateOf<String?>(null) }
    fun path(session: BodyProgressSession) = if (angle == Angle.Front) session.frontPath else session.profilePath
    Column(Modifier.fillMaxSize().background(WildforceThemeTokens.background).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TopBar("Comparar progreso", close)
        Toggle(listOf(Angle.Front, Angle.Profile), angle, { it.label }) { angle = it }
        Toggle(ComparisonMode.entries.toList(), comparisonMode, { it.label }) { comparisonMode = it }
        if (comparisonMode == ComparisonMode.SideBySide) Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) { ComparePhoto(before, path(before), "Antes", Modifier.weight(1f)); ComparePhoto(current, path(current), "Ahora", Modifier.weight(1f)) }
        else Box(Modifier.fillMaxWidth().weight(1f).pointerInput(before.id, current.id) {
            detectTapGestures(onPress = {
                revealReference = true
                tryAwaitRelease()
                revealReference = false
            })
        }) { val item = if (revealReference) before else current; ComparePhoto(item, path(item), if (revealReference) "Antes" else "Ahora · mantén pulsado para ver antes", Modifier.fillMaxSize()) }
        if (sessions.size > 1) {
            val timeline = sessions.sortedBy { it.date }
            val selectedIndex = timeline.indexOfFirst { it.id == current.id }.coerceAtLeast(0)
            Slider(
                value = selectedIndex.toFloat(),
                onValueChange = { selectedId = timeline[it.toInt().coerceIn(0, timeline.lastIndex)].id },
                valueRange = 0f..timeline.lastIndex.toFloat(),
                steps = (timeline.size - 2).coerceAtLeast(0),
                colors = SliderDefaults.colors(thumbColor = WildforceThemeTokens.accentGold, activeTrackColor = WildforceThemeTokens.accentGold)
            )
            Row(Modifier.fillMaxWidth()) {
                Text(timeline.first().date, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
                Spacer(Modifier.weight(1f))
                Text(timeline.last().date, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
            }
        }
        if (profile.birthYear <= Year.now().value - 18) {
            Button(onClick = { if (consent) { loading = true; failure = null; scope.launch { runCatching { generateFuture(BitmapFactory.decodeFile(current.frontPath), profile) }.onSuccess { future = it; BodyProgressStore.saveFuture(context, current, it) }.onFailure { failure = it.message ?: "No se pudo crear la previsión" }; loading = false } } else dialog = true }, enabled = !loading, colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.accentGold), modifier = Modifier.fillMaxWidth()) { if (loading) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp) else Text(if (future == null) "Ver previsión a 6 meses" else "Regenerar previsión", color = Color.White) }
            future?.let { Image(it.asImageBitmap(), "Previsión a 6 meses", Modifier.fillMaxWidth().height(250.dp).clip(RoundedCornerShape(14.dp)), contentScale = ContentScale.Crop) }; failure?.let { Text(it, color = Color.Red, style = MaterialTheme.typography.caption) }
            if (consent) Text("Retirar consentimiento", Modifier.clickable { prefs.edit().remove("future_preview_ai_consent_v1").apply(); consent = false }, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
        }
    }
    if (dialog) AlertDialog(onDismissRequest = { dialog = false }, title = { Text("Vista a futuro con IA") }, text = { Text("Enviaremos una copia de tu foto frontal y datos básicos de entrenamiento a la API de OpenAI para crear una previsión de 6 meses. El contenido no se usa para entrenar modelos; puede conservarse para monitorización de abuso durante hasta 30 días. Confirmas que tienes 18 años o más y aceptas este tratamiento.") }, confirmButton = { Button(onClick = { prefs.edit().putLong("future_preview_ai_consent_v1", System.currentTimeMillis()).apply(); consent = true; dialog = false }) { Text("Acepto") } }, dismissButton = { Button(onClick = { dialog = false }) { Text("Cancelar") } })
}
private enum class Angle(val label: String) { Front("Frontal"), Profile("Perfil") }
private enum class ComparisonMode(val label: String) { Reveal("Mantener pulsado"), SideBySide("Lado a lado") }

@Composable private fun <T> Toggle(items: List<T>, selected: T, label: (T) -> String, select: (T) -> Unit) = Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) { items.forEach { item -> Text(label(item), Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(if (item == selected) WildforceThemeTokens.accentGold else WildforceThemeTokens.textSecondary.copy(alpha = .12f)).clickable { select(item) }.padding(vertical = 9.dp), color = if (item == selected) Color.White else WildforceThemeTokens.textPrimary, style = MaterialTheme.typography.caption, textAlign = TextAlign.Center) } }
@Composable private fun ComparePhoto(session: BodyProgressSession, path: String, label: String, modifier: Modifier) = Box(modifier.clip(RoundedCornerShape(18.dp)).background(WildforceThemeTokens.textSecondary.copy(alpha = .10f))) {
    Photo(path, Modifier.fillMaxSize())
    Box(Modifier.fillMaxWidth().height(48.dp).align(Alignment.BottomCenter).background(Color.Black.copy(alpha = .46f)))
    Text("$label · ${session.date}", Modifier.align(Alignment.BottomStart).padding(10.dp), style = MaterialTheme.typography.caption, color = Color.White)
}
@Composable private fun TopBar(title: String, close: () -> Unit, action: (() -> Unit)? = null) = Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", Modifier.size(22.dp).clickable(onClick = close), WildforceThemeTokens.textPrimary); Text(title, Modifier.weight(1f).padding(horizontal = 12.dp), style = MaterialTheme.typography.h6, color = WildforceThemeTokens.textPrimary); action?.let { Icon(Icons.Filled.PhotoCamera, "Nuevo check-in", Modifier.size(27.dp).clickable(onClick = it), WildforceThemeTokens.accentGold) } }
@Composable private fun Photo(path: String, modifier: Modifier = Modifier) { val bitmap = remember(path) { BitmapFactory.decodeFile(path) }; if (bitmap != null) Image(bitmap.asImageBitmap(), null, modifier.clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop) else Box(modifier.background(WildforceThemeTokens.textSecondary.copy(alpha = .12f))) }
