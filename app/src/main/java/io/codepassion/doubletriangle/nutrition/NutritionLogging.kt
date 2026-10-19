package io.codepassion.doubletriangle.nutrition

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.codepassion.doubletriangle.core.designsystem.AntonFontFamily
import io.codepassion.doubletriangle.core.designsystem.Exo2FontFamily
import io.codepassion.doubletriangle.core.designsystem.WildforceThemeTokens
import io.codepassion.doubletriangle.core.designsystem.liquidGlass
import io.codepassion.doubletriangle.core.designsystem.liquidGlassBackground
import io.codepassion.doubletriangle.core.model.RemoteSyncChangeTracker
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.UUID
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

internal enum class LogMethod(val title: String, val description: String) {
    Barcode("Código de barras", "Escanea o introduce el código de un producto"),
    Describe("Describir comida", "Describe con tus palabras lo que has comido"),
    Photo("Foto", "Selecciona una foto de tu comida"),
    Recent("Recientes", "Vuelve a registrar una comida anterior"),
    Label("Etiqueta nutricional", "Fotografía los valores de una etiqueta"),
    Search("Buscar", "Busca un alimento en el catálogo"),
    Manual("Entrada manual", "Introduce alimento, cantidad y macros"),
}

@Composable
internal fun NutritionLogFlow(
    selectedDate: LocalDate,
    initialType: MealType,
    initialEntry: MealLog? = null,
    initialMethod: LogMethod? = null,
    initialTitle: String? = null,
    recent: List<MealLog>,
    onDismiss: () -> Unit,
    onSave: (MealLog) -> Unit,
) {
    val context = LocalContext.current
    var mealType by remember { mutableStateOf(initialType) }
    var method by remember { mutableStateOf<LogMethod?>(initialMethod ?: if (initialEntry != null) LogMethod.Manual else null) }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var photoBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var pendingCameraPhoto by remember { mutableStateOf<File?>(null) }
    var pendingPhotoMethod by remember { mutableStateOf<LogMethod?>(null) }
    var showPhotoSourcePicker by remember { mutableStateOf(false) }
    var photoSourceMethod by remember { mutableStateOf(LogMethod.Photo) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    // A fast double tap on the final button must not create two meal logs.
    // iOS dismisses the sheet immediately after the first commit.
    var saveCommitted by remember(initialEntry?.id, selectedDate) { mutableStateOf(false) }
    val saveOnce: (MealLog) -> Unit = { entry ->
        if (!saveCommitted) {
            saveCommitted = true
            onSave(entry)
        }
    }
    var scannedBarcode by remember { mutableStateOf("") }
    val barcodeScanner = remember(context) {
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8, Barcode.FORMAT_UPC_A, Barcode.FORMAT_UPC_E)
            .enableAutoZoom()
            .build()
        GmsBarcodeScanning.getClient(context, options)
    }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) { photoUri = uri; method = pendingPhotoMethod ?: LogMethod.Photo }
        pendingPhotoMethod = null
    }
    val cameraPicker = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        if (saved) {
            photoUri = pendingCameraPhoto?.let { FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", it) }
            method = pendingPhotoMethod ?: LogMethod.Photo
        } else pendingCameraPhoto?.delete()
        pendingCameraPhoto = null
        pendingPhotoMethod = null
    }
    val requestCameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            pendingCameraPhoto?.let { cameraPicker.launch(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", it)) }
        } else {
            pendingCameraPhoto?.delete()
            pendingCameraPhoto = null
            pendingPhotoMethod = null
            cameraError = "Necesitamos acceso a la cámara para fotografiar la comida. Puedes permitirlo en Ajustes o elegir una foto de la galería."
        }
    }
    fun capturePhoto(method: LogMethod) {
        pendingPhotoMethod = method
        cameraError = null
        val cameraDirectory = File(context.cacheDir, "nutrition_camera").apply { mkdirs() }
        val cameraFile = File(cameraDirectory, "nutrition-${UUID.randomUUID()}.jpg")
        pendingCameraPhoto = cameraFile
        val outputUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", cameraFile)
        if (context.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            cameraPicker.launch(outputUri)
        } else {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }
    }
    fun choosePhoto(method: LogMethod) {
        photoSourceMethod = method
        showPhotoSourcePicker = true
    }
    LaunchedEffect(initialMethod) {
        when (initialMethod) {
            LogMethod.Photo, LogMethod.Label -> capturePhoto(initialMethod)
            LogMethod.Barcode -> barcodeScanner.startScan()
                .addOnSuccessListener { barcode -> scannedBarcode = barcode.rawValue.orEmpty(); method = LogMethod.Barcode }
                .addOnFailureListener { method = LogMethod.Barcode }
            else -> Unit
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Surface(
                // Keep the chooser tall enough to expose the final manual-entry
                // row while still matching the compact iOS sheet presentation.
                Modifier.fillMaxWidth().fillMaxHeight(if (method == null) .70f else .94f).padding(horizontal = 8.dp),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = WildforceThemeTokens.background,
            ) {
            Column(Modifier.fillMaxSize()) {
                // Mirrors the visible grabber in iOS's medium/large nutrition sheet.
                Box(
                    Modifier.align(Alignment.CenterHorizontally)
                        .padding(top = 10.dp, bottom = 2.dp)
                        .size(width = 34.dp, height = 4.dp)
                        .background(WildforceThemeTokens.textSecondary.copy(alpha = .28f), CircleShape),
                )
                Box(Modifier.weight(1f)) {
            if (method == null) {
                LazyColumn(
                    contentPadding = PaddingValues(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 12.dp),
                    // The iOS secondary provider stack uses a tight 6pt rhythm;
                    // keep Android similarly compact so all four rows read as
                    // one grouped list instead of four separated cards.
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) { Text("Registrar ingesta", fontFamily = Exo2FontFamily, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textPrimary); Text(selectedDate.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale("es"))), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary) }
                            IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, "Cerrar") }
                        }
                    }
                    item {
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MealType.entries.forEach { type ->
                                Text(type.title, Modifier.background(if (mealType == type) WildforceThemeTokens.accent else WildforceThemeTokens.textSecondary.copy(alpha = .09f), CircleShape).clickable { mealType = type }.padding(horizontal = 13.dp, vertical = 8.dp), color = if (mealType == type) WildforceThemeTokens.primaryButtonText else WildforceThemeTokens.textPrimary, style = MaterialTheme.typography.caption, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            // iOS: barcode.viewfinder
                            PrimaryMethod(LogMethod.Barcode, Icons.Filled.QrCodeScanner, Modifier.weight(1f)) {
                                barcodeScanner.startScan()
                                    .addOnSuccessListener { barcode -> scannedBarcode = barcode.rawValue.orEmpty(); method = LogMethod.Barcode }
                                    .addOnFailureListener { method = LogMethod.Barcode }
                            }
                            // iOS: text.bubble
                            PrimaryMethod(LogMethod.Describe, Icons.Filled.ChatBubbleOutline, Modifier.weight(1f)) { method = LogMethod.Describe }
                            // iOS: camera.viewfinder
                            PrimaryMethod(LogMethod.Photo, Icons.Filled.CameraAlt, Modifier.weight(1f)) { choosePhoto(LogMethod.Photo) }
                        }
                    }
                    cameraError?.let { message ->
                        item {
                            Text(
                                message,
                                color = MaterialTheme.colors.error,
                                style = MaterialTheme.typography.caption,
                                modifier = Modifier
                                    .background(MaterialTheme.colors.error.copy(alpha = .08f), RoundedCornerShape(10.dp))
                                    .padding(10.dp),
                            )
                        }
                    }
                    items(listOf(LogMethod.Recent, LogMethod.Label, LogMethod.Search, LogMethod.Manual)) { item ->
                        MethodRow(item) {
                            if (item == LogMethod.Label) choosePhoto(item) else method = item
                        }
                    }
                }
            } else if (method == LogMethod.Recent) {
                RecentPicker(
                    recent = recent,
                    mealType = mealType,
                    date = selectedDate,
                    onBack = { method = null },
                onSave = saveOnce,
                )
            } else if (method == LogMethod.Search) {
                FoodSearch(mealType, selectedDate, onBack = { method = null }, onSave = saveOnce)
            } else if (method == LogMethod.Photo && (photoUri != null || photoBitmap != null)) {
                PhotoAnalysisReview(
                    date = selectedDate,
                    initialType = mealType,
                    photoUri = photoUri,
                    photoBitmap = photoBitmap,
                    onBack = { method = null; photoUri = null; photoBitmap = null },
                    onSave = saveOnce,
                )
            } else {
                MealEditor(
                    title = method!!.title,
                initial = (initialEntry ?: defaultFor(method!!, mealType, selectedDate, photoUri)).let { draft ->
                    draft.copy(name = initialTitle ?: draft.name)
                },
                    onBack = { method = null; photoUri = null },
                    onSave = saveOnce,
                    photoLabel = photoUri?.lastPathSegment ?: if (photoBitmap != null) "Captura de cámara" else null,
                    photoUri = photoUri,
                    photoBitmap = photoBitmap,
                    describeMode = method == LogMethod.Describe,
                    barcodeMode = method == LogMethod.Barcode,
                    labelMode = method == LogMethod.Label,
                    initialBarcode = scannedBarcode,
                )
            }
                }
            }
            }
        }
    }
    if (showPhotoSourcePicker) {
        AlertDialog(
            onDismissRequest = { showPhotoSourcePicker = false },
            title = { Text(if (photoSourceMethod == LogMethod.Label) "Añadir etiqueta" else "Añadir foto", fontFamily = Exo2FontFamily, fontWeight = FontWeight.SemiBold) },
            text = { Text(if (photoSourceMethod == LogMethod.Label) "Fotografía o selecciona la etiqueta nutricional para leer sus valores." else "Haz una foto ahora o selecciona una de tu galería para analizar la comida.") },
            confirmButton = {
                TextButton(onClick = {
                    showPhotoSourcePicker = false
                    capturePhoto(photoSourceMethod)
                }) { Text("Cámara", color = NutritionCoral) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPhotoSourcePicker = false
                    pendingPhotoMethod = photoSourceMethod
                    photoPicker.launch("image/*")
                }) { Text("Galería") }
            },
        )
    }
}

@Composable
private fun PrimaryMethod(method: LogMethod, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.height(108.dp).background(WildforceThemeTokens.textSecondary.copy(alpha = .08f), RoundedCornerShape(14.dp)).then(methodPressModifier(onClick)).padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(icon, null, tint = WildforceThemeTokens.textPrimary, modifier = Modifier.size(28.dp))
        Text(method.title, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textPrimary, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun MethodRow(method: LogMethod, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().then(methodPressModifier(onClick)).padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        // iOS NutritionMethodButtonRow keeps method icons neutral; the accent is
        // reserved for selected/progress states, not for every entry method.
        // Match the iOS SF Symbols: calendar.badge.clock, list.bullet.rectangle,
        // magnifyingglass and square.and.pencil.
        Icon(when (method) {
            LogMethod.Recent -> Icons.Filled.Event
            LogMethod.Label -> Icons.Filled.ListAlt
            LogMethod.Search -> Icons.Filled.Search
            else -> Icons.Filled.EditNote
        }, null, modifier = Modifier.size(44.dp).padding(10.dp), tint = WildforceThemeTokens.textPrimary)
        Column(Modifier.weight(1f)) { Text(method.title, color = WildforceThemeTokens.textPrimary); Text(method.description, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary) }
        Icon(Icons.Filled.ChevronRight, null, tint = WildforceThemeTokens.textSecondary)
    }
}

@Composable
private fun methodPressModifier(onClick: () -> Unit): Modifier {
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) .96f else 1f,
        animationSpec = spring(dampingRatio = .58f, stiffness = 620f),
        label = "nutrition-method-press",
    )
    return Modifier
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(interactionSource = interactions, indication = null, onClick = onClick)
}

@Composable
private fun RecentPicker(
    recent: List<MealLog>,
    mealType: MealType,
    date: LocalDate,
    onBack: () -> Unit,
    onSave: (MealLog) -> Unit,
) {
    val context = LocalContext.current.applicationContext
    var recentEntries by remember(recent) { mutableStateOf(recent) }
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf<MealType?>(null) }
    var bookmarksOnly by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var review by remember { mutableStateOf<List<MealLog>?>(null) }

    review?.let { selected ->
        val copiedItems = selected.flatMap { entry ->
            entry.items.ifEmpty {
                listOf(MealItem(entry.name, entry.calories, entry.protein, entry.carbs, entry.fat))
            }
        }
        val totals = copiedItems.fold(NutritionTargets(0, 0, 0, 0)) { total, item ->
            NutritionTargets(total.calories + item.calories, total.protein + item.protein, total.carbs + item.carbs, total.fat + item.fat)
        }
        MealEditor(
            title = if (selected.size == 1) "Revisar comida" else "Revisar comidas",
            initial = MealLog(
                id = System.currentTimeMillis(),
                name = selected.singleOrNull()?.name ?: "",
                calories = totals.calories,
                protein = totals.protein,
                carbs = totals.carbs,
                fat = totals.fat,
                type = mealType,
                loggedDate = date,
                source = "recent",
                items = copiedItems,
            ),
            onBack = { review = null },
            onSave = onSave,
            photoLabel = null,
            photoUri = null,
            photoBitmap = null,
            describeMode = false,
            barcodeMode = false,
            labelMode = false,
            initialBarcode = "",
        )
        return
    }

    val visible = recentEntries.filter { entry ->
        val matchesText = query.isBlank() || listOf(entry.name, entry.notes, entry.items.joinToString(" ") { it.name })
            .any { it.contains(query.trim(), ignoreCase = true) }
        val matchesType = filter == null || entry.type == filter
        matchesText && matchesType && (!bookmarksOnly || entry.isBookmarked)
    }
    // iOS keeps the review action attached to the bottom edge while the list
    // scrolls underneath it.  Keeping it inside LazyColumn made the primary
    // action disappear after long recent-meal lists.
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, top = 20.dp, end = 20.dp, bottom = if (selectedIds.isNotEmpty()) 96.dp else 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
        item { SheetHeader("Comidas recientes", onBack) }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it.take(80) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Buscar comidas o alimentos") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                singleLine = true,
            )
        }
        item {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                RecentFilterChip("Todas", filter == null && !bookmarksOnly) { filter = null; bookmarksOnly = false }
                RecentFilterChip("Guardadas", bookmarksOnly) { filter = null; bookmarksOnly = true }
                MealType.entries.forEach { type -> RecentFilterChip(type.title, filter == type) { filter = type; bookmarksOnly = false } }
            }
        }
        if (visible.isEmpty()) item {
            EmptyState(
                if (recent.isEmpty()) "Sin comidas recientes" else "No hay coincidencias",
                if (recent.isEmpty()) "Los alimentos que registres aparecerán aquí para repetirlos rápidamente." else "Prueba con otra búsqueda o filtro.",
                icon = Icons.Filled.Event,
            )
        }
        items(visible, key = { it.id }) { entry ->
            val selected = entry.id in selectedIds
            Row(
                Modifier.fillMaxWidth()
                    .background(if (selected) NutritionCoral.copy(alpha = .14f) else WildforceThemeTokens.textSecondary.copy(alpha = .08f), RoundedCornerShape(16.dp))
                    .clickable { selectedIds = if (selected) selectedIds - entry.id else selectedIds + entry.id }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(if (selected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked, if (selected) "Seleccionada" else "Seleccionar", tint = if (selected) NutritionCoral else WildforceThemeTokens.textSecondary)
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(entry.name, fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textPrimary, modifier = Modifier.weight(1f))
                        IconButton(onClick = {
                            val updated = entry.copy(isBookmarked = !entry.isBookmarked)
                            NutritionStore.save(context, entry.loggedDate, NutritionStore.load(context, entry.loggedDate).map { if (it.id == entry.id) updated else it })
                            RemoteSyncChangeTracker.mark(context, "nutrition-log-entries")
                            recentEntries = recentEntries.map { if (it.id == entry.id) updated else it }
                        }, modifier = Modifier.size(36.dp)) {
                            Icon(if (entry.isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder, if (entry.isBookmarked) "Quitar de guardadas" else "Guardar", tint = if (entry.isBookmarked) NutritionCoral else WildforceThemeTokens.textSecondary, modifier = Modifier.size(18.dp))
                        }
                    }
                    Text("${entry.type.title} · ${entry.calories} kcal", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
                    entry.items.takeIf { it.isNotEmpty() }?.let { foods -> Text(foods.joinToString(" · ") { it.name }, maxLines = 1, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary) }
                }
            }
        }
        }
        if (selectedIds.isNotEmpty()) {
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                color = WildforceThemeTokens.backgroundSecondary.copy(alpha = .97f),
                elevation = 8.dp,
            ) {
                Button(
                    onClick = { review = recentEntries.filter { it.id in selectedIds } },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary, contentColor = WildforceThemeTokens.primaryButtonText),
                    shape = RoundedCornerShape(8.dp),
                ) { Text(if (selectedIds.size == 1) "REVISAR 1 COMIDA" else "REVISAR ${selectedIds.size} COMIDAS", fontFamily = Exo2FontFamily, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp) }
            }
        }
    }
}

@Composable
private fun FoodSearch(type: MealType, date: LocalDate, onBack: () -> Unit, onSave: (MealLog) -> Unit) {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<NutritionSearchFood>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedFood by remember { mutableStateOf<NutritionSearchFood?>(null) }
    var portionDraft by remember { mutableStateOf<MealLog?>(null) }
    if (portionDraft != null) {
        val draft = requireNotNull(portionDraft)
        MealEditor(
            title = "Revisar alimento",
            initial = draft,
            onBack = { portionDraft = null }, onSave = onSave, photoLabel = null, photoUri = null, photoBitmap = null,
            describeMode = false, barcodeMode = false, labelMode = false, initialBarcode = "",
        )
    } else if (selectedFood != null) {
        val food = requireNotNull(selectedFood)
        FoodPortionReview(
            food = food,
            onBack = { selectedFood = null },
            onReview = { grams, calories, protein, carbs, fat ->
                portionDraft = MealLog(
                    System.currentTimeMillis(), food.name, calories, protein, carbs, fat, type, date, food.brand, "search",
                    items = listOf(MealItem(food.name, calories, protein, carbs, fat, brand = food.brand, quantity = grams, unit = "g", grams = grams)),
                )
            },
        )
    } else LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { SheetHeader("Buscar alimento", onBack) }
        item { Row(verticalAlignment = Alignment.CenterVertically) { OutlinedTextField(query, { query = it.take(100) }, Modifier.weight(1f), label = { Text("Buscar alimento") }, leadingIcon = { Icon(Icons.Filled.Search, null) }, singleLine = true); IconButton(onClick = { scope.launch { loading = true; error = null; runCatching { withContext(Dispatchers.IO) { NutritionAnalyzer.searchFoods(context, query) } }.onSuccess { results = it }.onFailure { error = "No se pudo buscar el alimento. Comprueba tu conexión e inténtalo de nuevo." }; loading = false } }, enabled = query.trim().length >= 2 && !loading) { if (loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = NutritionCoral) else Icon(Icons.Filled.Search, "Buscar", tint = NutritionCoral) } } }
        error?.let { message -> item { Text(message, color = MaterialTheme.colors.error, style = MaterialTheme.typography.caption) } }
        if (!loading && results.isEmpty() && query.isNotBlank() && error == null) item { Text("Busca para ver alimentos del catálogo.", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary) }
        items(results) { food ->
            Row(Modifier.fillMaxWidth().clickable { selectedFood = food }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text(food.name, color = WildforceThemeTokens.textPrimary, fontWeight = FontWeight.SemiBold); Text(food.brand.ifBlank { "Alimento genérico" }, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary); Text("P ${food.protein}g  ·  C ${food.carbs}g  ·  G ${food.fat}g", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary) }
                Text("${food.calories} kcal", fontWeight = FontWeight.Bold, color = NutritionCoral)
            }
        }
    }
}

/** The catalogue values are for a serving; iOS always asks for the consumed grams before review. */
@Composable
private fun FoodPortionReview(
    food: NutritionSearchFood,
    onBack: () -> Unit,
    onReview: (grams: Double, calories: Int, protein: Int, carbs: Int, fat: Int) -> Unit,
) {
    var selectedPortion by remember(food) {
        mutableStateOf(food.portions.firstOrNull { it.amountGrams == 100.0 } ?: food.portions.firstOrNull())
    }
    var portionsExpanded by remember(food) { mutableStateOf(false) }
    var gramsText by remember(food, selectedPortion) {
        mutableStateOf((selectedPortion?.amountGrams ?: food.servingGrams ?: 100.0).toInt().toString())
    }
    val grams = gramsText.decimalOrNull()?.coerceIn(1.0, 5_000.0) ?: 0.0
    val serving = selectedPortion?.amountGrams ?: food.servingGrams ?: 100.0
    val factor = if (serving > 0) grams / serving else 0.0
    val calories = ((selectedPortion?.calories ?: food.calories) * factor).toInt()
    val protein = ((selectedPortion?.protein ?: food.protein) * factor).toInt()
    val carbs = ((selectedPortion?.carbs ?: food.carbs) * factor).toInt()
    val fat = ((selectedPortion?.fat ?: food.fat) * factor).toInt()
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { SheetHeader("Cantidad", onBack) }
        item {
            Column(Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(20.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (food.brand.isNotBlank()) Text(food.brand, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
                Text(food.name, style = MaterialTheme.typography.h6, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
                Text("Indica la cantidad que has consumido.", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
            }
        }
        if (food.portions.size > 1) {
            item {
                Box {
                    OutlinedButton(onClick = { portionsExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(selectedPortion?.name ?: "Seleccionar ración")
                    }
                    DropdownMenu(expanded = portionsExpanded, onDismissRequest = { portionsExpanded = false }) {
                        food.portions.forEach { portion ->
                            DropdownMenuItem(onClick = {
                                selectedPortion = portion
                                portionsExpanded = false
                            }) {
                                Text(portion.name)
                            }
                        }
                    }
                }
            }
        }
        item { NumberField("Cantidad (g)", gramsText, { gramsText = decimalValue(it) }, Modifier.fillMaxWidth(), allowDecimal = true) }
        item { PhotoMacroSummary(NutritionTargets(calories, protein, carbs, fat)) }
        item { Text("Valores para ${grams.toInt()} g · ración del catálogo: ${selectedPortion?.name ?: "${serving.toInt()} g"}", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary) }
        item {
            Button(
                onClick = { onReview(grams, calories, protein, carbs, fat) },
                enabled = grams > 0,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary, contentColor = WildforceThemeTokens.primaryButtonText),
                shape = RoundedCornerShape(8.dp),
            ) { Text("Revisar ingesta", fontFamily = Exo2FontFamily, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp) }
        }
    }
}

/**
 * Photo logging deliberately has its own review step.  iOS does not drop a
 * photo estimate straight into the generic editor: it lets the person inspect
 * every detected food, correct the estimate, and only then persist it.
 */
@Composable
private fun PhotoAnalysisReview(
    date: LocalDate,
    initialType: MealType,
    photoUri: Uri?,
    photoBitmap: Bitmap?,
    onBack: () -> Unit,
    onSave: (MealLog) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("Comida fotografiada") }
    var mealType by remember { mutableStateOf(initialType) }
    var notes by remember { mutableStateOf("") }
    var foods by remember { mutableStateOf<List<MealItem>>(emptyList()) }
    var analyzing by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var correction by remember { mutableStateOf("") }
    var showingCorrection by remember { mutableStateOf(false) }
    // Keep the row in composition briefly so a deletion has the same springy
    // collapse as the iOS review screen instead of disappearing abruptly.
    var removingFoodIndex by remember { mutableStateOf<Int?>(null) }
    val previewBitmap = remember(photoUri, photoBitmap) {
        photoBitmap ?: photoUri?.let { uri ->
            context.contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)
        }
    }

    fun apply(analysis: NutritionAnalysis) {
        title = analysis.name.ifBlank { "Comida fotografiada" }
        notes = analysis.notes
        foods = analysis.foods.map { food ->
            MealItem(food.name, food.calories, food.protein, food.carbs, food.fat, grams = food.grams.toDouble().takeIf { it > 0 })
        }.ifEmpty {
            listOf(MealItem(analysis.name.ifBlank { "Alimento" }, analysis.calories, analysis.protein, analysis.carbs, analysis.fat))
        }
    }

    fun analyze(userCorrection: String? = null) {
        scope.launch {
            analyzing = true
            error = null
            runCatching {
                withContext(Dispatchers.IO) {
                    when {
                        photoUri != null -> NutritionAnalyzer.image(context, photoUri, false, userCorrection)
                        photoBitmap != null -> NutritionAnalyzer.image(context, photoBitmap, false, userCorrection)
                        else -> error("No se ha podido cargar la foto")
                    }
                }
            }.onSuccess(::apply)
                .onFailure { error = "No se pudo analizar la foto. Comprueba tu conexión e inténtalo de nuevo." }
            analyzing = false
        }
    }

    LaunchedEffect(photoUri, photoBitmap) { analyze() }

    val validFoods = foods.filter { it.name.isNotBlank() }
    val totals = validFoods.fold(NutritionTargets(0, 0, 0, 0)) { total, food ->
        NutritionTargets(total.calories + food.calories, total.protein + food.protein, total.carbs + food.carbs, total.fat + food.fat)
    }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
        item { SheetHeader("Revisar foto", onBack) }
        item {
            if (previewBitmap != null) {
                Image(
                    previewBitmap.asImageBitmap(),
                    "Foto de la comida",
                    Modifier.fillMaxWidth().height(230.dp).clip(RoundedCornerShape(20.dp)),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Row(Modifier.fillMaxWidth().background(NutritionCoral.copy(alpha = .08f), RoundedCornerShape(14.dp)).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Image, null, tint = NutritionCoral)
                    Text("Foto lista para revisar", Modifier.padding(start = 10.dp).weight(1f), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textPrimary)
                    TextButton(onClick = onBack, enabled = !analyzing) { Text("Repetir", color = NutritionCoral) }
                }
            }
        }
        if (analyzing) {
            item {
                Column(Modifier.fillMaxWidth().padding(vertical = 36.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = NutritionCoral)
                    Text("Analizando alimentos, cantidades y macros…", Modifier.padding(top = 14.dp), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
                }
            }
        } else if (error != null) {
            item { EmptyState("No se pudo analizar la foto", error!!) }
            item { Button(onClick = { analyze() }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary, contentColor = WildforceThemeTokens.primaryButtonText), shape = RoundedCornerShape(8.dp)) { Text("Reintentar", fontFamily = Exo2FontFamily, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp) } }
        } else {
            item { OutlinedTextField(title, { title = it.take(80) }, Modifier.fillMaxWidth(), label = { Text("Nombre de la comida") }, singleLine = true) }
            item { PhotoMacroSummary(totals) }
            item { Text("Alimentos detectados", fontFamily = Exo2FontFamily, fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textPrimary) }
            items(foods.indices.toList()) { index ->
                val food = foods[index]
                AnimatedVisibility(
                    visible = removingFoodIndex != index,
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    Column(Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(16.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(food.name, { value -> foods = foods.mapIndexed { itemIndex, current -> if (itemIndex == index) current.copy(name = value.take(80)) else current } }, Modifier.weight(1f), label = { Text("Alimento") }, singleLine = true)
                            IconButton(onClick = {
                                removingFoodIndex = index
                                scope.launch {
                                    delay(220)
                                    foods = foods.filterIndexed { itemIndex, _ -> itemIndex != index }
                                    removingFoodIndex = null
                                }
                            }, enabled = foods.size > 1 && removingFoodIndex == null) { Icon(Icons.Filled.DeleteOutline, "Eliminar alimento", tint = NutritionRed) }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            NumberField("kcal", food.calories.toString(), { value -> foods = foods.mapIndexed { itemIndex, current -> if (itemIndex == index) current.copy(calories = value.toIntOrNull()?.coerceIn(0, 8000) ?: 0) else current } }, Modifier.weight(1f))
                            NumberField("Proteína", food.protein.toString(), { value -> foods = foods.mapIndexed { itemIndex, current -> if (itemIndex == index) current.copy(protein = value.toIntOrNull()?.coerceIn(0, 500) ?: 0) else current } }, Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            NumberField("Carbos", food.carbs.toString(), { value -> foods = foods.mapIndexed { itemIndex, current -> if (itemIndex == index) current.copy(carbs = value.toIntOrNull()?.coerceIn(0, 1000) ?: 0) else current } }, Modifier.weight(1f))
                            NumberField("Grasa", food.fat.toString(), { value -> foods = foods.mapIndexed { itemIndex, current -> if (itemIndex == index) current.copy(fat = value.toIntOrNull()?.coerceIn(0, 500) ?: 0) else current } }, Modifier.weight(1f))
                        }
                        NumberField("Cantidad en gramos", food.grams?.toInt()?.toString().orEmpty(), { value ->
                            val newGrams = value.decimalOrNull()?.coerceIn(0.0, 5_000.0)
                            foods = foods.mapIndexed { itemIndex, current ->
                                if (itemIndex != index) current else {
                                    val previousGrams = current.grams
                                    val multiplier = if (previousGrams != null && previousGrams > 0.0 && newGrams != null) newGrams / previousGrams else 1.0
                                    current.copy(
                                        calories = (current.calories * multiplier).roundToInt(),
                                        protein = (current.protein * multiplier).roundToInt(),
                                        carbs = (current.carbs * multiplier).roundToInt(),
                                        fat = (current.fat * multiplier).roundToInt(),
                                        grams = newGrams,
                                    )
                                }
                            }
                        }, Modifier.fillMaxWidth(), allowDecimal = true)
                    }
                }
            }
            item { TextButton(onClick = { foods = foods + MealItem("", 0, 0, 0, 0) }) { Icon(Icons.Filled.Add, null, tint = NutritionCoral); Text("Añadir alimento", Modifier.padding(start = 6.dp), color = NutritionCoral) } }
            item { Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) { MealType.entries.forEach { type -> FilterChip(type.title, type == mealType) { mealType = type } } } }
            item {
                TextButton(onClick = { showingCorrection = !showingCorrection }) {
                    Icon(Icons.Filled.Edit, null, tint = NutritionCoral)
                    Text(if (showingCorrection) "Ocultar corrección" else "Corregir estimación", Modifier.padding(start = 6.dp), color = NutritionCoral)
                }
            }
            if (showingCorrection) {
                item {
                    Text("Indica cualquier ingrediente, cantidad o preparación que falte o esté estimada incorrectamente.", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
                    OutlinedTextField(correction, { correction = it.take(300) }, Modifier.fillMaxWidth(), label = { Text("Qué falta o está mal") }, placeholder = { Text("Ej.: había aguacate y 200 g de pollo") }, minLines = 5, maxLines = 10)
                    Text("${(300 - correction.length).coerceAtLeast(0)}", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End)
                }
                item { Button(onClick = { analyze(correction) }, enabled = correction.isNotBlank(), modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary, contentColor = WildforceThemeTokens.primaryButtonText), shape = RoundedCornerShape(8.dp)) { Text("Reanalizar foto", fontFamily = Exo2FontFamily, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp) } }
            }
            item { OutlinedTextField(notes, { notes = it.take(240) }, Modifier.fillMaxWidth(), label = { Text("Notas (opcional)") }, minLines = 2) }
            item {
                Button(onClick = {
                    scope.launch {
                        val localPhotoPath = withContext(Dispatchers.IO) { saveNutritionPhoto(context, photoUri, photoBitmap) }
                        onSave(MealLog(System.currentTimeMillis(), title.trim().ifBlank { "Comida fotografiada" }, totals.calories, totals.protein, totals.carbs, totals.fat, mealType, date, notes.trim(), "photo", items = validFoods, photoPath = localPhotoPath))
                    }
                }, enabled = title.isNotBlank() && foods.any { it.name.isNotBlank() }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary, contentColor = WildforceThemeTokens.primaryButtonText), shape = RoundedCornerShape(8.dp)) { Text("Guardar ingesta", fontFamily = Exo2FontFamily, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp) }
            }
        }
    }
}

@Composable
private fun PhotoMacroSummary(target: NutritionTargets) {
    Row(
        Modifier.fillMaxWidth().background(NutritionCoral.copy(alpha = .08f), RoundedCornerShape(16.dp)).padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("${target.calories} kcal", fontWeight = FontWeight.Bold, color = NutritionCoral)
        Text("P ${target.protein}g", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textPrimary)
        Text("C ${target.carbs}g", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textPrimary)
        Text("G ${target.fat}g", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textPrimary)
    }
}

@Composable
private fun MealEditor(title: String, initial: MealLog, onBack: () -> Unit, onSave: (MealLog) -> Unit, photoLabel: String?, photoUri: Uri?, photoBitmap: Bitmap?, describeMode: Boolean, barcodeMode: Boolean, labelMode: Boolean, initialBarcode: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val previewBitmap = remember(photoUri, photoBitmap) {
        photoBitmap ?: photoUri?.let { uri ->
            context.contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)
        }
    }
    val firstItem = initial.items.firstOrNull()
    var mealTitle by remember(initial) { mutableStateOf(initial.name) }
    var name by remember(initial) { mutableStateOf(firstItem?.name ?: initial.name) }
    var calories by remember(initial) { mutableStateOf((firstItem?.calories ?: initial.calories).takeIf { it > 0 }?.toString().orEmpty()) }
    var protein by remember(initial) { mutableStateOf((firstItem?.protein ?: initial.protein).takeIf { it > 0 }?.toString().orEmpty()) }
    var carbs by remember(initial) { mutableStateOf((firstItem?.carbs ?: initial.carbs).takeIf { it > 0 }?.toString().orEmpty()) }
    var fat by remember(initial) { mutableStateOf((firstItem?.fat ?: initial.fat).takeIf { it > 0 }?.toString().orEmpty()) }
    // A label's figures are normalized per 100 g, just as in the iOS label-review flow.
    // The consumed amount remains independently editable and scales the saved item.
    var consumedLabelGrams by remember(initial, labelMode) { mutableStateOf((firstItem?.grams ?: 100.0).toInt().toString()) }
    var barcodeServings by remember(initial, barcodeMode) { mutableStateOf((firstItem?.quantity ?: 1.0).toInt().coerceAtLeast(1).toString()) }
    var barcodeServingGrams by remember(initial, barcodeMode) { mutableStateOf((firstItem?.grams ?: 100.0).toInt().toString()) }
    var itemQuantity by remember(initial) { mutableStateOf((firstItem?.quantity ?: 1.0).toInt().coerceAtLeast(1).toString()) }
    var itemUnit by remember(initial) { mutableStateOf(firstItem?.unit.orEmpty()) }
    var itemGrams by remember(initial) { mutableStateOf(firstItem?.grams?.toInt()?.toString().orEmpty()) }
    var itemBrand by remember(initial) { mutableStateOf(firstItem?.brand.orEmpty()) }
    var itemNotes by remember(initial) { mutableStateOf(firstItem?.notes.orEmpty()) }
    // iOS's item editor supports both total/per-100g macros and total/per-unit
    // weight. Preserve that distinction instead of saving the visible numbers
    // unchanged when the person changes a portion.
    var macrosPer100g by remember(initial) { mutableStateOf(false) }
    var gramsPerUnit by remember(initial) { mutableStateOf(false) }
    var notes by remember(initial) { mutableStateOf(initial.notes) }
    var type by remember(initial) { mutableStateOf(initial.type) }
    var barcode by remember(initialBarcode) { mutableStateOf(initialBarcode) }
    var brands by remember { mutableStateOf("") }
    var barcodeImageUrl by remember { mutableStateOf("") }
    var contributionFront by remember { mutableStateOf<Uri?>(null) }
    var contributionNutrition by remember { mutableStateOf<Uri?>(null) }
    var contributionCameraTarget by remember { mutableStateOf<String?>(null) }
    var contributionCameraFile by remember { mutableStateOf<File?>(null) }
    var contributionSourceTarget by remember { mutableStateOf<String?>(null) }
    var contributionMessage by remember { mutableStateOf<String?>(null) }
    val contributionFrontPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { contributionFront = it }
    val contributionNutritionPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { contributionNutrition = it }
    val contributionCameraPicker = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val file = contributionCameraFile
        contributionCameraFile = null
        if (saved && file != null) {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            if (contributionCameraTarget == "front") contributionFront = uri else if (contributionCameraTarget == "nutrition") contributionNutrition = uri
        } else file?.delete()
        contributionCameraTarget = null
    }
    val contributionCameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val file = contributionCameraFile
        if (granted && file != null) contributionCameraPicker.launch(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file))
        else { file?.delete(); contributionCameraFile = null; contributionCameraTarget = null }
    }
    fun captureContribution(target: String) {
        val file = File(context.cacheDir, "nutrition-contribution-${UUID.randomUUID()}.jpg").apply { parentFile?.mkdirs() }
        contributionCameraTarget = target; contributionCameraFile = file
        if (context.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) contributionCameraPicker.launch(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)) else contributionCameraPermission.launch(Manifest.permission.CAMERA)
    }
    // One repeatable list is used for every additional food. Keeping a separate
    // "second food" branch rendered the same add affordance twice on Android.
    var extraItems by remember(initial) { mutableStateOf(initial.items.drop(1)) }
    var manualEditingItem by remember { mutableStateOf<Pair<Int, MealItem>?>(null) }
    var removingExtraItemIndex by remember(initial) { mutableStateOf<Int?>(null) }
    var analyzing by remember { mutableStateOf(false) }
    var analysisError by remember { mutableStateOf<String?>(null) }
    var hasBarcodeProduct by remember(initialBarcode) { mutableStateOf(false) }
    // The review editor must occupy its layout immediately. Keeping this true
    // avoids a large empty gap while the staggered animation is waiting and
    // matches iOS, where the food editor is visible directly below the summary.
    var reviewAppeared by remember(initial) { mutableStateOf(true) }
    var descriptionAnalyzed by remember(initial) { mutableStateOf(!describeMode) }

    val isPortionEditor = !labelMode && !barcodeMode
    val isManualReview = !describeMode && !barcodeMode && !labelMode && photoLabel == null && photoUri == null && photoBitmap == null
    val quantityNumber = itemQuantity.decimalOrNull()?.coerceIn(1.0, 1_000.0) ?: 1.0
    val enteredGrams = itemGrams.decimalOrNull()?.coerceIn(1.0, 5_000.0)
    val totalPortionGrams = enteredGrams?.let { if (gramsPerUnit) it * quantityNumber else it }
    val canUsePer100g = isPortionEditor && (totalPortionGrams ?: 0.0) > 0.0

    fun decimalText(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else "%.1f".format(Locale.US, value)

    fun firstManualItem(): MealItem = MealItem(name.trim(), (calories.decimalOrNull() ?: 0.0).roundToInt(), (protein.decimalOrNull() ?: 0.0).roundToInt(), (carbs.decimalOrNull() ?: 0.0).roundToInt(), (fat.decimalOrNull() ?: 0.0).roundToInt(), itemBrand.trim(), itemQuantity.decimalOrNull() ?: 1.0, itemUnit.trim(), totalPortionGrams, itemNotes.trim())

    fun setMacrosPer100g(enabled: Boolean) {
        if (enabled == macrosPer100g || (enabled && !canUsePer100g)) return
        val grams = totalPortionGrams ?: return
        val factor = if (enabled) 100.0 / grams else grams / 100.0
        calories = decimalText((calories.decimalOrNull() ?: 0.0) * factor)
        protein = decimalText((protein.decimalOrNull() ?: 0.0) * factor)
        carbs = decimalText((carbs.decimalOrNull() ?: 0.0) * factor)
        fat = decimalText((fat.decimalOrNull() ?: 0.0) * factor)
        macrosPer100g = enabled
    }

    fun setGramsPerUnit(enabled: Boolean) {
        if (enabled == gramsPerUnit) return
        val grams = enteredGrams ?: run { gramsPerUnit = enabled; return }
        itemGrams = decimalText(if (enabled) grams / quantityNumber else grams * quantityNumber)
        gramsPerUnit = enabled
    }

    suspend fun runAnalysis(block: () -> NutritionAnalysis) {
        analyzing = true
        analysisError = null
        runCatching { withContext(Dispatchers.IO) { block() } }
            .onSuccess { result ->
                name = result.name
                calories = result.calories.toString()
                protein = result.protein.toString()
                carbs = result.carbs.toString()
                fat = result.fat.toString()
                notes = result.notes
                if (barcodeMode) {
                    brands = result.brand
                    barcodeImageUrl = result.imageUrl
                    hasBarcodeProduct = result.servingGrams != null
                }
                result.servingGrams?.let { barcodeServingGrams = it.toInt().coerceAtLeast(1).toString() }
                if (describeMode) descriptionAnalyzed = true
            }
            .onFailure { analysisError = "No se pudo completar el análisis. Comprueba tu conexión e inténtalo de nuevo." }
        analyzing = false
    }

    LaunchedEffect(photoUri, photoBitmap, labelMode) {
        if (photoUri != null) runAnalysis { NutritionAnalyzer.image(context, photoUri, labelMode) }
        else if (photoBitmap != null) runAnalysis { NutritionAnalyzer.image(context, photoBitmap, labelMode) }
    }
    LaunchedEffect(initialBarcode) {
        if (initialBarcode.isNotBlank()) runAnalysis { NutritionAnalyzer.barcode(initialBarcode) }
    }
    LaunchedEffect(initial) { reviewAppeared = true }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
        item { ReviewAppearance(reviewAppeared, 0) { SheetHeader(title, onBack, if (isManualReview) ({ val blank = MealItem("", 0, 0, 0, 0); val manualIndex = extraItems.size + 1; extraItems = extraItems + blank; manualEditingItem = manualIndex to blank }) else null) } }
        if (isManualReview) item {
            // iOS opens manual logging in the review hierarchy: meal summary
            // first, then the editable food rows. Keep that hierarchy visible
            // on Android instead of leading with a wall of numeric fields.
            Column(
                Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(24.dp)).padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    mealTitle,
                    { mealTitle = it.take(80) },
                    Modifier.fillMaxWidth(),
                    label = { Text("Nombre de la comida") },
                    placeholder = { Text("Ej.: Pollo y arroz") },
                    singleLine = true,
                )
                val summaryFoods = listOf(firstManualItem()) + extraItems.filter { it.name.isNotBlank() }
                PhotoMacroSummary(NutritionTargets(
                    summaryFoods.sumOf { it.calories },
                    summaryFoods.sumOf { it.protein },
                    summaryFoods.sumOf { it.carbs },
                    summaryFoods.sumOf { it.fat },
                ))
                OutlinedTextField(
                    notes,
                    { notes = it.take(240) },
                    Modifier.fillMaxWidth(),
                    label = { Text("Notas (opcional)") },
                    minLines = 2,
                )
            }
        }
        if (isManualReview) item {
            val foods = listOf(firstManualItem()) + extraItems
            Column(Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(24.dp)).padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Alimentos", style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
                foods.forEachIndexed { index, food ->
                    Row(Modifier.fillMaxWidth().clickable { manualEditingItem = index to food }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(food.name.ifBlank { "Alimento sin nombre" }, fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textPrimary)
                            Text(listOfNotNull(food.brand.takeIf { it.isNotBlank() }, "${decimalText(food.quantity)} ${food.unit}".trim(), food.grams?.let { "${decimalText(it)} g" }).joinToString(" · "), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
                            Text("P ${food.protein} g · C ${food.carbs} g · G ${food.fat} g", style = MaterialTheme.typography.overline, color = WildforceThemeTokens.textSecondary)
                        }
                        Text("${food.calories} kcal", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textPrimary)
                        Icon(Icons.Filled.ChevronRight, "Editar alimento", tint = WildforceThemeTokens.textSecondary)
                    }
                    if (index < foods.lastIndex) Divider(color = WildforceThemeTokens.textSecondary.copy(alpha = .16f))
                }
                TextButton(onClick = { val blank = MealItem("", 0, 0, 0, 0); val manualIndex = extraItems.size + 1; extraItems = extraItems + blank; manualEditingItem = manualIndex to blank }) {
                    Icon(Icons.Filled.Add, null, tint = WildforceThemeTokens.textPrimary)
                    Text("Añadir alimento", Modifier.padding(start = 6.dp), color = WildforceThemeTokens.textPrimary)
                }
            }
        }
        if (photoLabel != null) item {
            if (labelMode && previewBitmap != null) {
                Image(
                    previewBitmap.asImageBitmap(),
                    "Etiqueta nutricional",
                    Modifier.fillMaxWidth().height(230.dp).clip(RoundedCornerShape(20.dp)),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Row(Modifier.fillMaxWidth().background(NutritionCoral.copy(alpha = .08f), RoundedCornerShape(14.dp)).padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.Image, null, tint = NutritionCoral); Text("Imagen seleccionada · $photoLabel", Modifier.padding(start = 10.dp), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textPrimary) }
            }
        }
        if (barcodeMode) item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(barcode, { barcode = it.filter(Char::isDigit).take(14) }, Modifier.weight(1f), label = { Text("Código de barras") }, leadingIcon = { Icon(Icons.Filled.QrCodeScanner, null) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                IconButton(onClick = { scope.launch { hasBarcodeProduct = false; runAnalysis { NutritionAnalyzer.barcode(barcode) } } }, enabled = barcode.length >= 8 && !analyzing) { Icon(Icons.Filled.Search, "Buscar producto", tint = NutritionCoral) }
            }
        }
        if (barcodeMode && hasBarcodeProduct) item {
            val servings = barcodeServings.decimalOrNull()?.coerceIn(1.0, 100.0) ?: 1.0
            val gramsPerServing = barcodeServingGrams.decimalOrNull()?.coerceIn(1.0, 5_000.0) ?: 100.0
            val productFactor = servings * gramsPerServing / 100.0
            Column(
                Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(22.dp)).padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (barcodeImageUrl.isNotBlank()) {
                    BarcodeProductImage(barcodeImageUrl)
                }
                Text(name, style = MaterialTheme.typography.h6, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
                if (brands.isNotBlank()) Text(brands, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField("Número de raciones", barcodeServings, { barcodeServings = decimalValue(it) }, Modifier.weight(1f), allowDecimal = true)
                    NumberField("Cantidad por ración (g)", barcodeServingGrams, { barcodeServingGrams = decimalValue(it) }, Modifier.weight(1f), allowDecimal = true)
                }
                PhotoMacroSummary(NutritionTargets(
                    ((calories.decimalOrNull() ?: 0.0) * productFactor).roundToInt(),
                    ((protein.decimalOrNull() ?: 0.0) * productFactor).roundToInt(),
                    ((carbs.decimalOrNull() ?: 0.0) * productFactor).roundToInt(),
                    ((fat.decimalOrNull() ?: 0.0) * productFactor).roundToInt(),
                ))
                Text("Por 100 g · ${calories.ifBlank { "0" }} kcal · P ${protein.ifBlank { "0" }} g · C ${carbs.ifBlank { "0" }} g · G ${fat.ifBlank { "0" }} g", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
            }
        }
        // The iOS item editor identifies the food before asking for its
        // portion.  Keeping this first also makes a manual entry easier to
        // scan than starting with a row of anonymous numbers.
        if (!isManualReview && (!barcodeMode || !hasBarcodeProduct || listOf(calories, protein, carbs, fat).all { (it.decimalOrNull() ?: 0.0) == 0.0 })) item {
            ReviewAppearance(reviewAppeared, 70) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (describeMode && !descriptionAnalyzed) {
                        Text("Describe tu comida con tus propias palabras. La IA estimará los ingredientes y las porciones.", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
                    }
                    OutlinedTextField(
                        name,
                        { name = it.take(if (describeMode) 300 else 80) },
                        Modifier.fillMaxWidth(),
                        label = { Text(if (describeMode) "Describe lo que has comido" else "Alimento o comida") },
                        placeholder = if (describeMode) ({ Text("Ej.: 2 huevos, una tostada con aguacate y café") }) else null,
                        minLines = if (describeMode) 5 else 1,
                        maxLines = if (describeMode) 10 else 1,
                    )
                    if (describeMode && !descriptionAnalyzed) {
                        Text("${(300 - name.length).coerceAtLeast(0)}", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End)
                    }
                    if (!labelMode && !barcodeMode && (!describeMode || descriptionAnalyzed)) OutlinedTextField(itemBrand, { itemBrand = it.take(80) }, Modifier.fillMaxWidth(), label = { Text("Marca (opcional)") }, singleLine = true)
                }
            }
        }
        if (!isManualReview && (!labelMode && !barcodeMode) && (!describeMode || descriptionAnalyzed)) item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField("Cantidad", itemQuantity, { itemQuantity = decimalValue(it) }, Modifier.weight(1f), allowDecimal = true)
                OutlinedTextField(itemUnit, { itemUnit = it.take(24) }, Modifier.weight(1f), label = { Text("Unidad") }, singleLine = true)
            }
        }
        if (!isManualReview && isPortionEditor && (!describeMode || descriptionAnalyzed)) item { NumberField(if (gramsPerUnit) "Gramos por unidad" else "Peso total (g, opcional)", itemGrams, { value ->
            itemGrams = decimalValue(value)
            val parsedGrams = value.decimalOrNull()
            if (parsedGrams == null || parsedGrams <= 0.0) macrosPer100g = false
        }, Modifier.fillMaxWidth(), allowDecimal = true) }
        if (!isManualReview && isPortionEditor && (!describeMode || descriptionAnalyzed)) item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip("Gramos totales", !gramsPerUnit) { setGramsPerUnit(false) }
                FilterChip("Por unidad", gramsPerUnit) { setGramsPerUnit(true) }
            }
        }
        if (!isManualReview && isPortionEditor && gramsPerUnit && totalPortionGrams != null) item {
            Text("${decimalText(quantityNumber)} ${itemUnit.ifBlank { "unidad" }} × ${decimalText(enteredGrams)} g = ${decimalText(totalPortionGrams)} g en total", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
        }
        if (barcodeMode && (!hasBarcodeProduct || listOf(calories, protein, carbs, fat).all { (it.decimalOrNull() ?: 0.0) == 0.0 })) {
            item { OutlinedTextField(brands, { brands = it.take(80) }, Modifier.fillMaxWidth(), label = { Text("Marca para contribuir") }, singleLine = true) }
        }
        if (describeMode) item {
            // iOS uses PrimaryButton here: solid textPrimary background,
            // primaryButtonText label, no outlined/accent treatment.
            Button(
                onClick = { scope.launch { runAnalysis { NutritionAnalyzer.describe(context, name) } } },
                enabled = name.isNotBlank() && !analyzing,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = WildforceThemeTokens.textPrimary,
                    contentColor = WildforceThemeTokens.primaryButtonText,
                ),
                shape = RoundedCornerShape(8.dp),
            ) { Text("Analizar descripción", fontFamily = Exo2FontFamily, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp) }
        }
        if (analyzing) item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(Modifier.size(22.dp), color = NutritionCoral, strokeWidth = 2.dp); Text(if (labelMode) "Leyendo etiqueta…" else "Analizando nutrición…", Modifier.padding(start = 10.dp), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary) } }
        analysisError?.let { message -> item { Text(message, color = MaterialTheme.colors.error, style = MaterialTheme.typography.caption, modifier = Modifier.background(MaterialTheme.colors.error.copy(alpha = .08f), RoundedCornerShape(10.dp)).padding(10.dp)) } }
        if (!describeMode || descriptionAnalyzed) item { Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) { MealType.entries.forEach { item -> FilterChip(item.title, item == type) { type = item } } } }
        if (labelMode) item {
            val consumed = consumedLabelGrams.decimalOrNull()?.coerceIn(1.0, 5_000.0) ?: 100.0
            val factor = consumed / 100.0
            Column(Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(20.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                NumberField("Cantidad consumida (g)", consumedLabelGrams, { consumedLabelGrams = decimalValue(it) }, Modifier.fillMaxWidth(), allowDecimal = true)
                PhotoMacroSummary(NutritionTargets(
                    ((calories.decimalOrNull() ?: 0.0) * factor).roundToInt(),
                    ((protein.decimalOrNull() ?: 0.0) * factor).roundToInt(),
                    ((carbs.decimalOrNull() ?: 0.0) * factor).roundToInt(),
                    ((fat.decimalOrNull() ?: 0.0) * factor).roundToInt(),
                ))
                Text("Total para ${decimalText(consumed)} g", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
            }
        }
        if (!isManualReview && isPortionEditor && (!describeMode || descriptionAnalyzed)) item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip("Macros totales", !macrosPer100g) { setMacrosPer100g(false) }
                FilterChip("Por 100 g", macrosPer100g && canUsePer100g) { setMacrosPer100g(true) }
            }
        }
        if (!isManualReview && isPortionEditor && (!describeMode || descriptionAnalyzed) && macrosPer100g && totalPortionGrams != null) item {
            Text("Los campos son por 100 g. Se guardará el total para ${decimalText(totalPortionGrams)} g.", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
        }
        if (!isManualReview && (!describeMode || descriptionAnalyzed) && (!barcodeMode || !hasBarcodeProduct || listOf(calories, protein, carbs, fat).all { (it.decimalOrNull() ?: 0.0) == 0.0 })) item { ReviewAppearance(reviewAppeared, 130) { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { NumberField(if (labelMode || barcodeMode || macrosPer100g) "kcal /100g" else "Calorías", calories, { calories = decimalValue(it) }, Modifier.weight(1f), allowDecimal = true); NumberField(if (labelMode || barcodeMode || macrosPer100g) "Proteína /100g" else "Proteína g", protein, { protein = decimalValue(it) }, Modifier.weight(1f), allowDecimal = true) } } }
        if (!isManualReview && (!describeMode || descriptionAnalyzed) && (!barcodeMode || !hasBarcodeProduct || listOf(calories, protein, carbs, fat).all { (it.decimalOrNull() ?: 0.0) == 0.0 })) item { ReviewAppearance(reviewAppeared, 170) { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { NumberField(if (labelMode || barcodeMode || macrosPer100g) "Carbos /100g" else "Carbos g", carbs, { carbs = decimalValue(it) }, Modifier.weight(1f), allowDecimal = true); NumberField(if (labelMode || barcodeMode || macrosPer100g) "Grasa /100g" else "Grasa g", fat, { fat = decimalValue(it) }, Modifier.weight(1f), allowDecimal = true) } } }
        if (!isManualReview && isPortionEditor && (!describeMode || descriptionAnalyzed) && macrosPer100g && totalPortionGrams != null) item {
            val totalFactor = totalPortionGrams / 100.0
            PhotoMacroSummary(NutritionTargets(
                ((calories.decimalOrNull() ?: 0.0) * totalFactor).toInt(),
                ((protein.decimalOrNull() ?: 0.0) * totalFactor).toInt(),
                ((carbs.decimalOrNull() ?: 0.0) * totalFactor).toInt(),
                ((fat.decimalOrNull() ?: 0.0) * totalFactor).toInt(),
            ))
        }
        if (barcodeMode && !hasBarcodeProduct) item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField("Raciones", barcodeServings, { barcodeServings = decimalValue(it) }, Modifier.weight(1f), allowDecimal = true)
                NumberField("g / ración", barcodeServingGrams, { barcodeServingGrams = decimalValue(it) }, Modifier.weight(1f), allowDecimal = true)
            }
        }
        if (barcodeMode && (!hasBarcodeProduct || listOf(calories, protein, carbs, fat).all { (it.decimalOrNull() ?: 0.0) == 0.0 })) {
            item { Text("Si el producto no existe en Open Food Facts, puedes añadirlo con fotos del frontal y de la tabla nutricional.", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary) }
            item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { contributionSourceTarget = "front" }, modifier = Modifier.weight(1f)) { Text(if (contributionFront == null) "Foto frontal" else "Frontal ✓") }
                OutlinedButton(onClick = { contributionSourceTarget = "nutrition" }, modifier = Modifier.weight(1f)) { Text(if (contributionNutrition == null) "Foto nutrición" else "Nutrición ✓") }
            } }
            item {
                val canContribute = barcode.length in 8..14 && name.isNotBlank() && brands.isNotBlank() && (calories.decimalOrNull() ?: 0.0) > 0 && contributionFront != null && contributionNutrition != null && !analyzing
                OutlinedButton(onClick = {
                    scope.launch {
                        analyzing = true; contributionMessage = null
                        runCatching { withContext(Dispatchers.IO) { NutritionAnalyzer.contributeOpenFoodFacts(context, barcode, name.trim(), brands.trim(), calories.decimalOrNull() ?: 0.0, protein.decimalOrNull() ?: 0.0, carbs.decimalOrNull() ?: 0.0, fat.decimalOrNull() ?: 0.0, contributionFront!!, contributionNutrition!!) } }
                            .onSuccess { contributionMessage = "Producto enviado a Open Food Facts." }
                            .onFailure { contributionMessage = "No se pudo enviar el producto. Comprueba tu conexión e inténtalo de nuevo." }
                        analyzing = false
                    }
                }, enabled = canContribute, modifier = Modifier.fillMaxWidth()) { Text("Contribuir producto") }
            }
            contributionMessage?.let { message -> item { Text(message, style = MaterialTheme.typography.caption, color = if (message.startsWith("Producto enviado")) NutritionCoral else MaterialTheme.colors.error) } }
        }
        if (!isManualReview) itemsIndexed(extraItems, key = { index, _ -> index }) { index, food ->
            AnimatedVisibility(visible = removingExtraItemIndex != index, exit = shrinkVertically() + fadeOut()) {
                Column(Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(16.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(food.name, { value -> extraItems = extraItems.mapIndexed { itemIndex, current -> if (itemIndex == index) current.copy(name = value.take(80)) else current } }, Modifier.weight(1f), label = { Text("Otro alimento") }, singleLine = true)
                        IconButton(onClick = {
                            removingExtraItemIndex = index
                            scope.launch {
                                delay(220)
                                extraItems = extraItems.filterIndexed { itemIndex, _ -> itemIndex != index }
                                removingExtraItemIndex = null
                            }
                        }, enabled = removingExtraItemIndex == null) { Icon(Icons.Filled.DeleteOutline, "Eliminar alimento", tint = NutritionRed) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberField("Calorías", food.calories.toString(), { value -> extraItems = extraItems.mapIndexed { itemIndex, current -> if (itemIndex == index) current.copy(calories = value.toIntOrNull()?.coerceIn(0, 8000) ?: 0) else current } }, Modifier.weight(1f))
                        NumberField("Proteína g", food.protein.toString(), { value -> extraItems = extraItems.mapIndexed { itemIndex, current -> if (itemIndex == index) current.copy(protein = value.toIntOrNull()?.coerceIn(0, 500) ?: 0) else current } }, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberField("Carbos g", food.carbs.toString(), { value -> extraItems = extraItems.mapIndexed { itemIndex, current -> if (itemIndex == index) current.copy(carbs = value.toIntOrNull()?.coerceIn(0, 1000) ?: 0) else current } }, Modifier.weight(1f))
                        NumberField("Grasa g", food.fat.toString(), { value -> extraItems = extraItems.mapIndexed { itemIndex, current -> if (itemIndex == index) current.copy(fat = value.toIntOrNull()?.coerceIn(0, 500) ?: 0) else current } }, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberField("Cantidad", food.quantity.toInt().coerceAtLeast(1).toString(), { value -> extraItems = extraItems.mapIndexed { itemIndex, current -> if (itemIndex == index) current.copy(quantity = value.decimalOrNull()?.coerceIn(1.0, 1_000.0) ?: 1.0) else current } }, Modifier.weight(1f), allowDecimal = true)
                        OutlinedTextField(food.unit, { value -> extraItems = extraItems.mapIndexed { itemIndex, current -> if (itemIndex == index) current.copy(unit = value.take(24)) else current } }, Modifier.weight(1f), label = { Text("Unidad") }, singleLine = true)
                    }
                    NumberField("Peso total (g, opcional)", food.grams?.toInt()?.toString().orEmpty(), { value ->
                        val newGrams = value.decimalOrNull()?.coerceIn(1.0, 5_000.0)
                        extraItems = extraItems.mapIndexed { itemIndex, current ->
                            if (itemIndex != index) current else {
                                val previousGrams = current.grams
                                val multiplier = if (previousGrams != null && previousGrams > 0.0 && newGrams != null) newGrams / previousGrams else 1.0
                                current.copy(
                                    calories = (current.calories * multiplier).roundToInt(),
                                    protein = (current.protein * multiplier).roundToInt(),
                                    carbs = (current.carbs * multiplier).roundToInt(),
                                    fat = (current.fat * multiplier).roundToInt(),
                                    grams = newGrams,
                                )
                            }
                        }
                    }, Modifier.fillMaxWidth(), allowDecimal = true)
                }
            }
        }
        if (!isManualReview && (!describeMode || descriptionAnalyzed)) item {
            TextButton(onClick = { extraItems = extraItems + MealItem("", 0, 0, 0, 0) }) {
                Icon(Icons.Filled.Add, null, tint = NutritionCoral)
                Text("Añadir otro alimento", Modifier.padding(start = 6.dp), color = NutritionCoral)
            }
        }
        if (!isManualReview && (!describeMode || descriptionAnalyzed)) item { ReviewAppearance(reviewAppeared, 210) { OutlinedTextField(notes, { notes = it.take(240) }, Modifier.fillMaxWidth(), label = { Text("Notas (opcional)") }, minLines = 2) } }
        if (!describeMode || descriptionAnalyzed) item {
            ReviewAppearance(reviewAppeared, 260) { Button(
                onClick = {
                    val labelFactor = if (labelMode) (consumedLabelGrams.decimalOrNull()?.coerceIn(1.0, 5_000.0) ?: 100.0) / 100.0 else 1.0
                    val barcodeFactor = if (barcodeMode) {
                        val servings = barcodeServings.decimalOrNull()?.coerceIn(1.0, 100.0) ?: 1.0
                        val gramsPerServing = barcodeServingGrams.decimalOrNull()?.coerceIn(1.0, 5_000.0) ?: 100.0
                        servings * gramsPerServing / 100.0
                    } else 1.0
                    val manualFactor = if (isPortionEditor && macrosPer100g) (totalPortionGrams ?: 0.0) / 100.0 else 1.0
            val factor = labelFactor * barcodeFactor * manualFactor
                    val first = MealItem(
                        name.trim(),
                        ((calories.decimalOrNull() ?: 0.0) * factor).roundToInt(),
                        ((protein.decimalOrNull() ?: 0.0) * factor).roundToInt(),
                        ((carbs.decimalOrNull() ?: 0.0) * factor).roundToInt(),
                        ((fat.decimalOrNull() ?: 0.0) * factor).roundToInt(),
                        brand = if (barcodeMode) brands.trim() else itemBrand.trim(),
                        quantity = when { labelMode -> consumedLabelGrams.decimalOrNull()?.coerceIn(1.0, 5_000.0) ?: 100.0; barcodeMode -> barcodeServings.decimalOrNull()?.coerceIn(1.0, 100.0) ?: 1.0; else -> itemQuantity.decimalOrNull()?.coerceIn(1.0, 1_000.0) ?: 1.0 },
                        unit = when { labelMode -> "g"; barcodeMode -> "ración"; else -> itemUnit.trim() },
                        grams = when { labelMode -> consumedLabelGrams.decimalOrNull()?.coerceIn(1.0, 5_000.0) ?: 100.0; barcodeMode -> (barcodeServings.decimalOrNull()?.coerceIn(1.0, 100.0) ?: 1.0) * (barcodeServingGrams.decimalOrNull()?.coerceIn(1.0, 5_000.0) ?: 100.0); else -> totalPortionGrams },
                        notes = itemNotes.trim(),
                    )
                    val itemList = buildList {
                        add(first)
                        addAll(extraItems.filter { it.name.isNotBlank() })
                    }
                    onSave(initial.copy(name = mealTitle.trim().ifBlank { name.trim() }, calories = itemList.sumOf { it.calories }, protein = itemList.sumOf { it.protein }, carbs = itemList.sumOf { it.carbs }, fat = itemList.sumOf { it.fat }, type = type, notes = notes.trim(), items = itemList))
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary, contentColor = WildforceThemeTokens.primaryButtonText),
                shape = RoundedCornerShape(8.dp),
            ) { Text("Guardar ingesta", fontFamily = Exo2FontFamily, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp) } }
        }
    }
    contributionSourceTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { contributionSourceTarget = null },
            title = { Text(if (target == "front") "Foto frontal" else "Foto nutricional", fontFamily = Exo2FontFamily, fontWeight = FontWeight.SemiBold) },
            text = { Text("Haz una foto o selecciona una imagen para contribuir este producto.") },
            confirmButton = { TextButton(onClick = { contributionSourceTarget = null; captureContribution(target) }) { Text("Cámara", color = NutritionCoral) } },
            dismissButton = { TextButton(onClick = { contributionSourceTarget = null; if (target == "front") contributionFrontPicker.launch("image/*") else contributionNutritionPicker.launch("image/*") }) { Text("Galería") } },
        )
    }

    manualEditingItem?.let { (index, food) ->
        ManualFoodEditorDialog(food, onDismiss = { manualEditingItem = null }, onDelete = {
            if (index == 0) { name = ""; calories = ""; protein = ""; carbs = ""; fat = ""; itemBrand = ""; itemNotes = ""; itemQuantity = "1"; itemUnit = ""; itemGrams = "" }
            else extraItems = extraItems.filterIndexed { i, _ -> i != index - 1 }
            manualEditingItem = null
        }, onSave = { updated ->
            if (index == 0) { name = updated.name; calories = updated.calories.toString(); protein = updated.protein.toString(); carbs = updated.carbs.toString(); fat = updated.fat.toString(); itemBrand = updated.brand; itemNotes = updated.notes; itemQuantity = updated.quantity.toString(); itemUnit = updated.unit; itemGrams = updated.grams?.toString().orEmpty() }
            else extraItems = extraItems.mapIndexed { i, current -> if (i == index - 1) updated else current }
            manualEditingItem = null
        })
    }
}

@Composable
private fun ManualFoodEditorDialog(food: MealItem, onDismiss: () -> Unit, onDelete: () -> Unit, onSave: (MealItem) -> Unit) {
    val consumedGrams = food.grams?.takeIf { it > 0.0 } ?: 100.0
    val per100Factor = 100.0 / consumedGrams
    var name by remember(food) { mutableStateOf(food.name) }
    var brand by remember(food) { mutableStateOf(food.brand) }
    var kcal by remember(food) { mutableStateOf((food.calories * per100Factor).roundToInt().toString()) }
    var protein by remember(food) { mutableStateOf((food.protein * per100Factor).roundToInt().toString()) }
    var carbs by remember(food) { mutableStateOf((food.carbs * per100Factor).roundToInt().toString()) }
    var fat by remember(food) { mutableStateOf((food.fat * per100Factor).roundToInt().toString()) }
    var quantity by remember(food) { mutableStateOf(food.quantity.toString()) }
    var unit by remember(food) { mutableStateOf(food.unit) }
    var grams by remember(food) { mutableStateOf(food.grams?.toString().orEmpty()) }
    var notes by remember(food) { mutableStateOf(food.notes) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize().padding(top = 32.dp), color = MaterialTheme.colors.background) {
            LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item { SheetHeader("Editar alimento", onDismiss, onDelete) }
                item { OutlinedTextField(name, { name = it.take(80) }, Modifier.fillMaxWidth(), label = { Text("Nombre") }, singleLine = true) }
                item { OutlinedTextField(brand, { brand = it.take(80) }, Modifier.fillMaxWidth(), label = { Text("Marca") }, singleLine = true) }
                item { Text("Nutrición por 100 g", style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary) }
                item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { NumberField("Kcal", kcal, { kcal = decimalValue(it) }, Modifier.weight(1f), allowDecimal = true); NumberField("Proteína g", protein, { protein = decimalValue(it) }, Modifier.weight(1f), allowDecimal = true) } }
                item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { NumberField("Carbos g", carbs, { carbs = decimalValue(it) }, Modifier.weight(1f), allowDecimal = true); NumberField("Grasa g", fat, { fat = decimalValue(it) }, Modifier.weight(1f), allowDecimal = true) } }
                item { Text("¿Cuánto has comido?", style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(onClick = { quantity = ((quantity.decimalOrNull() ?: 1.0) - 1.0).coerceAtLeast(1.0).toString() }) { Icon(Icons.Filled.RemoveCircleOutline, "Reducir cantidad") }
                            NumberField("Cantidad", quantity, { quantity = decimalValue(it) }, Modifier.weight(1f), allowDecimal = true)
                            IconButton(onClick = { quantity = ((quantity.decimalOrNull() ?: 1.0) + 1.0).coerceAtMost(1000.0).toString() }) { Icon(Icons.Filled.AddCircleOutline, "Aumentar cantidad") }
                        }
                        OutlinedTextField(unit, { unit = it.take(24) }, Modifier.weight(1f), label = { Text("Unidad") }, singleLine = true)
                    }
                }
                item { NumberField("Gramos", grams, { grams = decimalValue(it) }, Modifier.fillMaxWidth(), allowDecimal = true) }
                item { OutlinedTextField(notes, { notes = it.take(240) }, Modifier.fillMaxWidth(), label = { Text("Notas del alimento") }, minLines = 2) }
                item {
                    val consumed = grams.decimalOrNull() ?: 100.0
                    val factor = consumed / 100.0
                    PhotoMacroSummary(NutritionTargets(
                        ((kcal.decimalOrNull() ?: 0.0) * factor).roundToInt(),
                        ((protein.decimalOrNull() ?: 0.0) * factor).roundToInt(),
                        ((carbs.decimalOrNull() ?: 0.0) * factor).roundToInt(),
                        ((fat.decimalOrNull() ?: 0.0) * factor).roundToInt(),
                    ))
                    Text("Para ${grams.ifBlank { "100" }} g consumidos", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
                }
                item { Button(onClick = { val factor = (grams.decimalOrNull() ?: 100.0) / 100.0; onSave(food.copy(name = name.trim(), brand = brand.trim(), calories = ((kcal.decimalOrNull() ?: 0.0) * factor).roundToInt(), protein = ((protein.decimalOrNull() ?: 0.0) * factor).roundToInt(), carbs = ((carbs.decimalOrNull() ?: 0.0) * factor).roundToInt(), fat = ((fat.decimalOrNull() ?: 0.0) * factor).roundToInt(), quantity = quantity.decimalOrNull() ?: 1.0, unit = unit.trim(), grams = grams.decimalOrNull(), notes = notes.trim())) }, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary, contentColor = WildforceThemeTokens.primaryButtonText)) { Text("Listo") } }
            }
        }
    }
}

/** Mirrors iOS's optional AsyncImage packshot without adding an image-loading dependency. */
@Composable
private fun BarcodeProductImage(url: String) {
    var image by remember(url) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(url) {
        image = withContext(Dispatchers.IO) {
            runCatching { URL(url).openStream().use(BitmapFactory::decodeStream) }.getOrNull()
        }
    }
    image?.let { bitmap ->
        Image(
            bitmap.asImageBitmap(),
            contentDescription = "Foto del producto",
            modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(16.dp)),
            contentScale = ContentScale.Fit,
        )
    }
}

@Composable
private fun ReviewAppearance(visible: Boolean, delayMillis: Int, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(260, delayMillis = delayMillis)) + slideInVertically(tween(300, delayMillis = delayMillis)) { it / 14 },
    ) { content() }
}

@Composable private fun SheetHeader(title: String, onBack: () -> Unit, onAction: (() -> Unit)? = null) {
    val displayTitle = if (title == title.uppercase(Locale.getDefault())) {
        title.lowercase(Locale.getDefault()).replaceFirstChar { it.uppercase() }
    } else title.replaceFirstChar { it.uppercase() }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Atrás", modifier = Modifier.size(22.dp)) }
        Text(displayTitle, Modifier.weight(1f), fontFamily = Exo2FontFamily, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textPrimary)
        if (onAction != null) IconButton(onClick = onAction) { Icon(Icons.Filled.Add, "Añadir alimento", tint = WildforceThemeTokens.textPrimary) }
    }
}
@Composable private fun FilterChip(text: String, selected: Boolean, onClick: () -> Unit) { Text(text, Modifier.background(if (selected) WildforceThemeTokens.accent else WildforceThemeTokens.textSecondary.copy(alpha = .1f), CircleShape).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 8.dp), color = if (selected) WildforceThemeTokens.primaryButtonText else WildforceThemeTokens.textPrimary, style = MaterialTheme.typography.caption) }
@Composable private fun RecentFilterChip(text: String, selected: Boolean, onClick: () -> Unit) { Text(text, Modifier.background(if (selected) WildforceThemeTokens.textPrimary else Color.Transparent, CircleShape).clickable(onClick = onClick).border(1.dp, if (selected) WildforceThemeTokens.textPrimary else WildforceThemeTokens.textSecondary.copy(alpha = .35f), CircleShape).padding(horizontal = 12.dp, vertical = 8.dp), color = if (selected) WildforceThemeTokens.primaryButtonText else WildforceThemeTokens.textPrimary, style = MaterialTheme.typography.caption) }
@Composable internal fun NumberField(label: String, value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier, allowDecimal: Boolean = false) { OutlinedTextField(value, onValueChange, modifier, label = { Text(label) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = if (allowDecimal) KeyboardType.Decimal else KeyboardType.Number)) }
@Composable internal fun EmptyState(title: String, body: String, icon: ImageVector = Icons.Filled.Eco) { Column(Modifier.fillMaxWidth().padding(vertical = 30.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(icon, null, tint = NutritionCoral, modifier = Modifier.size(42.dp)); Text(title, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary, modifier = Modifier.padding(top = 10.dp)); Text(body, style = MaterialTheme.typography.caption, textAlign = TextAlign.Center, color = WildforceThemeTokens.textSecondary, modifier = Modifier.padding(top = 5.dp)) } }
private fun digits(value: String) = value.filter(Char::isDigit).take(4)
internal fun String.decimalOrNull(): Double? = replace(',', '.').toDoubleOrNull()
private fun decimalValue(value: String): String {
    val normalized = value.replace(',', '.')
    val hasSeparator = normalized.contains('.')
    val parts = normalized.split('.', limit = 2)
    val integer = parts.firstOrNull().orEmpty().filter(Char::isDigit).take(4)
    val decimal = parts.getOrNull(1).orEmpty().filter(Char::isDigit).take(2)
    return if (hasSeparator) "$integer.$decimal" else integer
}

/** Stores a bounded JPEG copy, matching iOS's local NutritionLogMedia behavior. */
private fun saveNutritionPhoto(context: android.content.Context, uri: Uri?, bitmap: Bitmap?): String? = runCatching {
    val source = bitmap ?: uri?.let { selected -> context.contentResolver.openInputStream(selected)?.use(BitmapFactory::decodeStream) } ?: return null
    val maxDimension = 2_048
    val largestDimension = maxOf(source.width, source.height)
    val bounded = if (largestDimension > maxDimension) {
        val scale = maxDimension.toFloat() / largestDimension
        Bitmap.createScaledBitmap(source, (source.width * scale).toInt(), (source.height * scale).toInt(), true)
    } else source
    val directory = File(context.filesDir, "NutritionLogMedia").apply { mkdirs() }
    val file = File(directory, "${UUID.randomUUID()}.jpg")
    listOf(82, 65, 50, 35).forEach { quality ->
        FileOutputStream(file).use { output -> check(bounded.compress(Bitmap.CompressFormat.JPEG, quality, output)) }
        if (file.length() <= 5L * 1024L * 1024L) return@runCatching file.absolutePath
    }
    file.delete()
    null
}.getOrNull()

private fun defaultFor(method: LogMethod, type: MealType, date: LocalDate, uri: Uri?) = MealLog(
    id = System.currentTimeMillis(),
    name = when (method) { LogMethod.Photo -> "Comida fotografiada"; LogMethod.Label -> "Alimento de etiqueta"; else -> "" },
    calories = 0, protein = 0, carbs = 0, fat = 0, type = type, loggedDate = date,
    notes = uri?.let { "Imagen: ${it.lastPathSegment}" }.orEmpty(), source = method.name.lowercase(),
)
