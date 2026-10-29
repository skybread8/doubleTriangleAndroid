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
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
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

/** AI-backed nutrition methods are the same two providers gated by iOS. */
internal fun LogMethod.requiresAiNutritionAccess(): Boolean = this == LogMethod.Describe || this == LogMethod.Photo

@Composable
internal fun NutritionLogFlow(
    selectedDate: LocalDate,
    initialType: MealType,
    initialEntry: MealLog? = null,
    initialMethod: LogMethod? = null,
    initialTitle: String? = null,
    recent: List<MealLog>,
    aiNutritionAccess: Boolean = true,
    onPremiumOffer: () -> Unit = {},
    onBookmarkChanged: () -> Unit = {},
    onDismiss: () -> Unit,
    onSave: (MealLog) -> Unit,
) {
    val context = LocalContext.current
    var mealType by remember { mutableStateOf(initialType) }
    var method by remember { mutableStateOf<LogMethod?>(initialMethod ?: if (initialEntry != null) LogMethod.Manual else null) }
    var mealTypeMenuExpanded by remember { mutableStateOf(false) }
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
                        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
                            // iOS uses Picker(.menu) here; keep meal type compact and
                            // out of the primary provider grid instead of rendering
                            // Android-only horizontal chips.
                            Row(
                                Modifier
                                    .heightIn(min = 48.dp)
                                    .clickable { mealTypeMenuExpanded = !mealTypeMenuExpanded }
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Tipo de comida", style = MaterialTheme.typography.overline, color = WildforceThemeTokens.textSecondary)
                                    Text(mealType.title, style = MaterialTheme.typography.body2, color = WildforceThemeTokens.textPrimary, fontWeight = FontWeight.SemiBold)
                                }
                                Icon(if (mealTypeMenuExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = "Seleccionar tipo de comida", tint = WildforceThemeTokens.textSecondary)
                            }
                            if (mealTypeMenuExpanded) {
                                Surface(
                                    modifier = Modifier
                                        .widthIn(min = 220.dp)
                                        .padding(top = 4.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    color = WildforceThemeTokens.surfaceElevated,
                                    elevation = 8.dp,
                                ) {
                                    Column(Modifier.padding(vertical = 4.dp)) {
                                    MealType.entries.forEach { type ->
                                        DropdownMenuItem(onClick = { mealType = type; mealTypeMenuExpanded = false }) {
                                            Text(type.title, color = WildforceThemeTokens.textPrimary, fontWeight = if (mealType == type) FontWeight.SemiBold else FontWeight.Normal)
                                        }
                                    }
                                    }
                                }
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
                            PrimaryMethod(LogMethod.Describe, Icons.Filled.ChatBubbleOutline, Modifier.weight(1f), locked = !aiNutritionAccess) { if (aiNutritionAccess) method = LogMethod.Describe else onPremiumOffer() }
                            // iOS: camera.viewfinder
                            PrimaryMethod(LogMethod.Photo, Icons.Filled.CameraAlt, Modifier.weight(1f), locked = !aiNutritionAccess) { if (aiNutritionAccess) choosePhoto(LogMethod.Photo) else onPremiumOffer() }
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
                    onBookmarkChanged = onBookmarkChanged,
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
private fun PrimaryMethod(method: LogMethod, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, locked: Boolean = false, onClick: () -> Unit) {
    Column(modifier.height(108.dp).background(WildforceThemeTokens.textSecondary.copy(alpha = .08f), RoundedCornerShape(14.dp)).then(methodPressModifier(onClick)).padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        // iOS keeps the provider's SF Symbol visible and overlays a small lock
        // in the top-right corner when the method is premium. Replacing the
        // symbol with a large lock made the Android chooser semantically and
        // visually different from NutritionMethodButton.
        Box(Modifier.fillMaxWidth().height(36.dp)) {
            Icon(icon, null, tint = WildforceThemeTokens.textPrimary, modifier = Modifier.align(Alignment.Center).size(28.dp))
            if (locked) {
                Icon(
                    Icons.Filled.Lock,
                    contentDescription = "Premium",
                    tint = WildforceThemeTokens.textSecondary,
                    modifier = Modifier.align(Alignment.TopEnd).size(14.dp),
                )
            }
        }
        Box(Modifier.fillMaxWidth().padding(top = 8.dp).heightIn(min = 32.dp), contentAlignment = Alignment.Center) { Text(method.title, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textPrimary, textAlign = TextAlign.Center, lineHeight = 16.sp) }
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
    onBookmarkChanged: () -> Unit,
    onSave: (MealLog) -> Unit,
) {
    val context = LocalContext.current.applicationContext
    var recentEntries by remember(recent) { mutableStateOf(recent) }
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf<MealType?>(null) }
    var bookmarksOnly by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var review by remember { mutableStateOf<List<MealLog>?>(null) }
    var bookmarkEntry by remember { mutableStateOf<MealLog?>(null) }
    var bookmarkName by remember { mutableStateOf("") }
    var removeBookmarkEntry by remember { mutableStateOf<MealLog?>(null) }

    fun updateBookmark(entry: MealLog, name: String?) {
        val updated = applyNutritionBookmark(entry, name)
        NutritionStore.save(context, entry.loggedDate, NutritionStore.load(context, entry.loggedDate).map { if (it.id == entry.id) updated else it })
        RemoteSyncChangeTracker.mark(context, "nutrition-log-entries")
        recentEntries = recentEntries.map { if (it.id == entry.id) updated else it }
        onBookmarkChanged()
    }

    bookmarkEntry?.let { entry ->
        AlertDialog(
            onDismissRequest = { bookmarkEntry = null },
            title = { Text("Guardar comida") },
            text = { OutlinedTextField(bookmarkName, { bookmarkName = it }, label = { Text("Nombre") }, singleLine = true) },
            confirmButton = { TextButton(onClick = { updateBookmark(entry, bookmarkName); bookmarkEntry = null }) { Text("GUARDAR") } },
            dismissButton = { TextButton(onClick = { bookmarkEntry = null }) { Text("CANCELAR") } },
        )
    }
    removeBookmarkEntry?.let { entry ->
        AlertDialog(
            onDismissRequest = { removeBookmarkEntry = null },
            title = { Text("¿Quitar de guardadas?") },
            text = { Text("Esta comida dejará de aparecer en tus guardadas.") },
            confirmButton = { TextButton(onClick = { updateBookmark(entry, null); removeBookmarkEntry = null }) { Text("QUITAR", color = NutritionRed) } },
            dismissButton = { TextButton(onClick = { removeBookmarkEntry = null }) { Text("CANCELAR") } },
        )
    }

    review?.let { selected ->
        val copiedItems = selected.flatMap { entry ->
            entry.items.ifEmpty {
                listOf(MealItem(entry.name, entry.calories, entry.protein, entry.carbs, entry.fat))
            }
        }
        val totals = copiedItems.fold(NutritionTargets(0, 0, 0, 0)) { total, item ->
            NutritionTargets((total.calories + item.calories).roundToInt(), (total.protein + item.protein).roundToInt(), (total.carbs + item.carbs).roundToInt(), (total.fat + item.fat).roundToInt())
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
                photoPath = selected.singleOrNull()?.photoPath,
                mediaUrl = selected.singleOrNull()?.mediaUrl,
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
        val matchesText = matchesRecentMealSearch(entry, query)
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
                    .background(if (selected) WildforceThemeTokens.accent.copy(alpha = .12f) else WildforceThemeTokens.textSecondary.copy(alpha = .08f), RoundedCornerShape(16.dp))
                    .clickable { selectedIds = if (selected) selectedIds - entry.id else selectedIds + entry.id }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(if (selected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked, if (selected) "Seleccionada" else "Seleccionar", tint = if (selected) NutritionCoral else WildforceThemeTokens.textSecondary)
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(entry.name, fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textPrimary, modifier = Modifier.weight(1f))
                        IconButton(onClick = {
                            if (entry.isBookmarked) removeBookmarkEntry = entry
                            else { bookmarkName = entry.name; bookmarkEntry = entry }
                        }, modifier = Modifier.size(36.dp)) {
                            Icon(if (entry.isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder, if (entry.isBookmarked) "Quitar de guardadas" else "Guardar", tint = if (entry.isBookmarked) WildforceThemeTokens.accent else WildforceThemeTokens.textSecondary, modifier = Modifier.size(18.dp))
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
            onReview = { grams, quantity, unit, calories, protein, carbs, fat ->
                portionDraft = MealLog(
                    System.currentTimeMillis(), food.name, calories, protein, carbs, fat, type, date, food.brand, "search",
                    items = listOf(MealItem(food.name, calories, protein, carbs, fat, brand = food.brand, quantity = quantity, unit = unit, grams = grams)),
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
                val portion = food.portions.firstOrNull { it.amountGrams == 100.0 } ?: food.portions.firstOrNull()
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(food.name, fontFamily = FontFamily.Default, fontSize = 16.sp, color = WildforceThemeTokens.textPrimary)
                    Text(portion?.name ?: food.brand, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MealMacro(NutritionMacroIcon.Calories, "${portion?.calories ?: food.calories} kcal", NutritionCaloriesColor)
                        MealMacro(NutritionMacroIcon.Protein, "${portion?.protein ?: food.protein}g", ProteinColor)
                        MealMacro(NutritionMacroIcon.Carbs, "${portion?.carbs ?: food.carbs}g", CarbsColor)
                        MealMacro(NutritionMacroIcon.Fat, "${portion?.fat ?: food.fat}g", FatColor)
                    }
                }
                Icon(Icons.Filled.ChevronRight, null, tint = WildforceThemeTokens.textSecondary)
            }
        }
    }
}

/** The catalogue values are for a serving; iOS always asks for the consumed grams before review. */
@Composable
private fun FoodPortionReview(
    food: NutritionSearchFood,
    onBack: () -> Unit,
    onReview: (grams: Double?, quantity: Double, unit: String, calories: Int, protein: Int, carbs: Int, fat: Int) -> Unit,
) {
    var selectedPortion by remember(food) { mutableStateOf(food.portions.firstOrNull { it.amountGrams == 100.0 } ?: food.portions.firstOrNull()) }
    var byWeight by remember(food) { mutableStateOf(food.portions.isEmpty()) }
    var quantity by remember(food) { mutableStateOf(if (food.portions.isEmpty()) 100.0 else 1.0) }
    var expanded by remember { mutableStateOf(false) }
    var editAmount by remember { mutableStateOf(false) }
    var amountDraft by remember { mutableStateOf("") }
    val portionGrams = selectedPortion?.amountGrams ?: food.servingGrams
    val grams = if (byWeight) quantity else portionGrams?.let { quantity * it }
    val factor = if (byWeight) quantity / (portionGrams ?: 100.0) else quantity
    val calories = ((selectedPortion?.calories ?: food.calories) * factor).roundToInt()
    val protein = ((selectedPortion?.protein ?: food.protein) * factor).roundToInt()
    val carbs = ((selectedPortion?.carbs ?: food.carbs) * factor).roundToInt()
    val fat = ((selectedPortion?.fat ?: food.fat) * factor).roundToInt()
    val step = if (!byWeight) .5 else when { quantity < 50 -> 5.0; quantity < 200 -> 10.0; else -> 25.0 }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        item { SheetHeader("Cantidad", onBack) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (food.brand.isNotBlank()) Text(food.brand, style = MaterialTheme.typography.body2, color = WildforceThemeTokens.textSecondary)
                Text(food.name, fontFamily = FontFamily.Default, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Ración", Modifier.weight(1f), color = WildforceThemeTokens.textPrimary)
                    Box {
                        TextButton(onClick = { expanded = true }) { Text(if (byWeight) "g" else selectedPortion?.name ?: "Ración", color = WildforceThemeTokens.textPrimary); Icon(Icons.Filled.ExpandMore, null) }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            food.portions.forEach { portion -> DropdownMenuItem(onClick = { selectedPortion = portion; byWeight = false; quantity = 1.0; expanded = false }) { Text(portion.name) } }
                            if (food.portions.any { it.amountGrams != null } || food.portions.isEmpty()) DropdownMenuItem(onClick = { selectedPortion = food.portions.firstOrNull { it.amountGrams != null } ?: selectedPortion; byWeight = true; quantity = 100.0; expanded = false }) { Text("g") }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (byWeight) "Cantidad (gramos)" else "Número de raciones", Modifier.weight(1f), color = WildforceThemeTokens.textPrimary)
                    IconButton(onClick = { quantity = (quantity - step).coerceAtLeast(if (byWeight) 1.0 else .5) }, Modifier.size(32.dp).background(WildforceThemeTokens.backgroundSecondary, CircleShape)) { Icon(Icons.Filled.Remove, "Reducir cantidad", Modifier.size(16.dp)) }
                    Text(quantity.nutritionInputString(), Modifier.width(64.dp).clickable { amountDraft = quantity.nutritionInputString(); editAmount = true }, textAlign = TextAlign.Center, color = WildforceThemeTokens.textPrimary)
                    IconButton(onClick = { quantity = (quantity + step).coerceAtMost(5000.0) }, Modifier.size(32.dp).background(WildforceThemeTokens.backgroundSecondary, CircleShape)) { Icon(Icons.Filled.Add, "Aumentar cantidad", Modifier.size(16.dp)) }
                }
            }
        }
        item { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { Text("Total", fontFamily = FontFamily.Default, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary); PhotoMacroSummary(NutritionTargets(calories, protein, carbs, fat)) } }
        item { Button(onClick = { onReview(grams, quantity, if (byWeight) "g" else selectedPortion?.name ?: "ración", calories, protein, carbs, fat) }, enabled = quantity > 0, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary, contentColor = WildforceThemeTokens.primaryButtonText), shape = RoundedCornerShape(8.dp)) { NutritionButtonLabel("Revisar ingesta", 16) } }
    }
    if (editAmount) AlertDialog(onDismissRequest = { editAmount = false }, title = { Text(if (byWeight) "Cantidad en gramos" else "Número de raciones") }, text = { OutlinedTextField(amountDraft, { amountDraft = decimalValue(it) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)) }, confirmButton = { TextButton(onClick = { quantity = (amountDraft.decimalOrNull() ?: quantity).coerceIn(if (byWeight) 1.0 else .5, 5000.0); editAmount = false }, enabled = (amountDraft.decimalOrNull() ?: 0.0) > 0) { Text("Listo") } }, dismissButton = { TextButton(onClick = { editAmount = false }) { Text("Cancelar") } })
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
        NutritionTargets((total.calories + food.calories).roundToInt(), (total.protein + food.protein).roundToInt(), (total.carbs + food.carbs).roundToInt(), (total.fat + food.fat).roundToInt())
    }
    if (!analyzing && error == null && foods.isNotEmpty()) {
        var editingIndex by remember { mutableStateOf<Int?>(null) }
        var gramsIndex by remember { mutableStateOf<Int?>(null) }
        var gramsDraft by remember { mutableStateOf("") }
        var showInfo by remember { mutableStateOf(false) }
        var rename by remember { mutableStateOf(false) }
        var titleDraft by remember { mutableStateOf(title) }
        fun updateFood(index: Int, food: MealItem) {
            foods = foods.mapIndexed { i, current -> if (i == index) food else current }
        }
        fun updateGrams(index: Int, value: Double) {
            val food = foods[index]
            val grams = value.coerceIn(1.0, 5000.0)
            val factor = food.grams?.takeIf { it > 0 }?.let { grams / it } ?: 1.0
            updateFood(index, food.copy(grams = grams, calories = food.calories * factor, protein = food.protein * factor, carbs = food.carbs * factor, fat = food.fat * factor))
        }
        LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) { SheetHeader("Revisar foto", onBack) }
                    TextButton(onClick = { showingCorrection = true }) { Text("Corregir", color = WildforceThemeTokens.textPrimary) }
                    IconButton(onClick = { showInfo = true }) { Icon(Icons.Filled.Info, "Información de la estimación", tint = WildforceThemeTokens.textPrimary) }
                }
            }
            if (previewBitmap != null) item { Image(previewBitmap.asImageBitmap(), "Foto de la comida", Modifier.fillMaxWidth().height(300.dp).clip(RoundedCornerShape(20.dp)), contentScale = ContentScale.Crop) }
            item { Text(title, Modifier.clickable { titleDraft = title; rename = true }, fontFamily = FontFamily.Default, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary) }
            item {
                Column(Modifier.fillMaxWidth().background(WildforceThemeTokens.backgroundSecondary, RoundedCornerShape(24.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    PhotoMacroSummary(totals)
                    foods.forEachIndexed { index, food ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f).clickable { editingIndex = index }) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(food.name, fontFamily = FontFamily.Default, fontSize = 16.sp, color = WildforceThemeTokens.textPrimary)
                                    Icon(Icons.Filled.Edit, "Editar ${food.name}", Modifier.padding(start = 6.dp).size(16.dp), tint = WildforceThemeTokens.textSecondary)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (food.calories > 0) MealMacro(NutritionMacroIcon.Calories, "${food.calories.roundToInt()} kcal", NutritionCaloriesColor)
                                    MealMacro(NutritionMacroIcon.Protein, "${food.protein.roundToInt()}g", ProteinColor)
                                    MealMacro(NutritionMacroIcon.Carbs, "${food.carbs.roundToInt()}g", CarbsColor)
                                    MealMacro(NutritionMacroIcon.Fat, "${food.fat.roundToInt()}g", FatColor)
                                }
                            }
                            val amount = food.grams ?: 1.0
                            val step = when { amount < 50 -> 5.0; amount < 200 -> 10.0; else -> 25.0 }
                            IconButton(onClick = { updateGrams(index, amount - step) }, Modifier.size(32.dp).background(WildforceThemeTokens.background, CircleShape)) { Icon(Icons.Filled.Remove, "Reducir gramos", Modifier.size(16.dp)) }
                            Text(amount.nutritionInputString(), Modifier.width(56.dp).clickable { gramsDraft = amount.nutritionInputString(); gramsIndex = index }, textAlign = TextAlign.Center, fontFamily = FontFamily.Default, color = WildforceThemeTokens.textPrimary)
                            IconButton(onClick = { updateGrams(index, amount + step) }, Modifier.size(32.dp).background(WildforceThemeTokens.background, CircleShape)) { Icon(Icons.Filled.Add, "Aumentar gramos", Modifier.size(16.dp)) }
                        }
                    }
                    Button(onClick = { scope.launch { val path = withContext(Dispatchers.IO) { saveNutritionPhoto(context, photoUri, photoBitmap) }; onSave(MealLog(System.currentTimeMillis(), title.trim().ifBlank { "Comida" }, totals.calories, totals.protein, totals.carbs, totals.fat, mealType, date, notes.trim(), "photo", items = validFoods, photoPath = path)) } }, enabled = validFoods.isNotEmpty(), modifier = Modifier.fillMaxWidth().padding(top = 8.dp), colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary, contentColor = WildforceThemeTokens.primaryButtonText), shape = RoundedCornerShape(8.dp)) { NutritionButtonLabel("Guardar ingesta", 16) }
                }
            }
        }
        if (showInfo) AlertDialog(onDismissRequest = { showInfo = false }, title = { Text("Estimación de IA") }, text = { Text(listOf("Estos valores son estimados y pueden no ser exactos.", notes).filter { it.isNotBlank() }.joinToString("\n\n")) }, confirmButton = { TextButton(onClick = { showInfo = false }) { Text("Entendido") } })
        if (rename) AlertDialog(onDismissRequest = { rename = false }, title = { Text("Nombre de la comida") }, text = { OutlinedTextField(titleDraft, { titleDraft = it.take(80) }, singleLine = true) }, confirmButton = { TextButton(onClick = { title = titleDraft.trim().ifBlank { "Comida" }; rename = false }) { Text("Guardar") } }, dismissButton = { TextButton(onClick = { rename = false }) { Text("Cancelar") } })
        if (showingCorrection) AlertDialog(onDismissRequest = { showingCorrection = false }, title = { Text("¿Qué debemos corregir?") }, text = { OutlinedTextField(correction, { correction = it.take(300) }, minLines = 5, maxLines = 10, placeholder = { Text("Describe qué falta o está mal") }) }, confirmButton = { TextButton(onClick = { showingCorrection = false; analyze(correction) }, enabled = correction.isNotBlank()) { Text("Reanalizar") } }, dismissButton = { TextButton(onClick = { showingCorrection = false }) { Text("Cancelar") } })
        gramsIndex?.let { index -> AlertDialog(onDismissRequest = { gramsIndex = null }, title = { Text("Cantidad en gramos") }, text = { OutlinedTextField(gramsDraft, { gramsDraft = decimalValue(it) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)) }, confirmButton = { TextButton(onClick = { gramsDraft.decimalOrNull()?.let { updateGrams(index, it) }; gramsIndex = null }, enabled = (gramsDraft.decimalOrNull() ?: 0.0) > 0) { Text("Listo") } }, dismissButton = { TextButton(onClick = { gramsIndex = null }) { Text("Cancelar") } }) }
        editingIndex?.let { index -> ManualFoodEditorDialog(foods[index], onDismiss = { editingIndex = null }, onDelete = if (foods.size > 1) {{ foods = foods.filterIndexed { i, _ -> i != index }; editingIndex = null }} else null, onSave = { updateFood(index, it); editingIndex = null }) }
        return
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
            item { Button(onClick = { analyze() }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary, contentColor = WildforceThemeTokens.primaryButtonText), shape = RoundedCornerShape(8.dp)) { NutritionButtonLabel("Reintentar") } }
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
                            NumberField("kcal", food.calories.nutritionInputString(), { value -> foods = foods.mapIndexed { itemIndex, current -> if (itemIndex == index) current.copy(calories = value.decimalOrNull()?.coerceIn(0.0, 8000.0) ?: 0.0) else current } }, Modifier.weight(1f), allowDecimal = true)
                            NumberField("Proteína", food.protein.nutritionInputString(), { value -> foods = foods.mapIndexed { itemIndex, current -> if (itemIndex == index) current.copy(protein = value.decimalOrNull()?.coerceIn(0.0, 500.0) ?: 0.0) else current } }, Modifier.weight(1f), allowDecimal = true)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            NumberField("Carbos", food.carbs.nutritionInputString(), { value -> foods = foods.mapIndexed { itemIndex, current -> if (itemIndex == index) current.copy(carbs = value.decimalOrNull()?.coerceIn(0.0, 1000.0) ?: 0.0) else current } }, Modifier.weight(1f), allowDecimal = true)
                            NumberField("Grasa", food.fat.nutritionInputString(), { value -> foods = foods.mapIndexed { itemIndex, current -> if (itemIndex == index) current.copy(fat = value.decimalOrNull()?.coerceIn(0.0, 500.0) ?: 0.0) else current } }, Modifier.weight(1f), allowDecimal = true)
                        }
                        NumberField("Cantidad en gramos", food.grams?.toInt()?.toString().orEmpty(), { value ->
                            val newGrams = value.decimalOrNull()?.coerceIn(0.0, 5_000.0)
                            foods = foods.mapIndexed { itemIndex, current ->
                                if (itemIndex != index) current else {
                                    val previousGrams = current.grams
                                    val multiplier = if (previousGrams != null && previousGrams > 0.0 && newGrams != null) newGrams / previousGrams else 1.0
                                    current.copy(
                                        calories = current.calories * multiplier,
                                        protein = current.protein * multiplier,
                                        carbs = current.carbs * multiplier,
                                        fat = current.fat * multiplier,
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
                item { Button(onClick = { analyze(correction) }, enabled = correction.isNotBlank(), modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary, contentColor = WildforceThemeTokens.primaryButtonText), shape = RoundedCornerShape(8.dp)) { NutritionButtonLabel("Reanalizar foto") } }
            }
            item { OutlinedTextField(notes, { notes = it.take(240) }, Modifier.fillMaxWidth(), label = { Text("Notas (opcional)") }, minLines = 2) }
            item {
                Button(onClick = {
                    scope.launch {
                        val localPhotoPath = withContext(Dispatchers.IO) { saveNutritionPhoto(context, photoUri, photoBitmap) }
                        onSave(MealLog(System.currentTimeMillis(), title.trim().ifBlank { "Comida fotografiada" }, totals.calories, totals.protein, totals.carbs, totals.fat, mealType, date, notes.trim(), "photo", items = validFoods, photoPath = localPhotoPath))
                    }
                }, enabled = title.isNotBlank() && foods.any { it.name.isNotBlank() }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary, contentColor = WildforceThemeTokens.primaryButtonText), shape = RoundedCornerShape(8.dp)) { NutritionButtonLabel("Guardar ingesta", 16) }
            }
        }
    }
}

@Composable
private fun PhotoMacroSummary(target: NutritionTargets) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DescriptionMacroBadge("Calorías", target.calories, NutritionMacroIcon.Calories, NutritionCaloriesColor, Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DescriptionMacroBadge("Proteína", target.protein, NutritionMacroIcon.Protein, ProteinColor, Modifier.weight(1f))
            DescriptionMacroBadge("Carbos", target.carbs, NutritionMacroIcon.Carbs, CarbsColor, Modifier.weight(1f))
            DescriptionMacroBadge("Grasa", target.fat, NutritionMacroIcon.Fat, FatColor, Modifier.weight(1f))
        }
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
    var calories by remember(initial) { mutableStateOf((firstItem?.calories ?: initial.calories.toDouble()).takeIf { it > 0.0 }?.nutritionInputString().orEmpty()) }
    var protein by remember(initial) { mutableStateOf((firstItem?.protein ?: initial.protein.toDouble()).takeIf { it > 0.0 }?.nutritionInputString().orEmpty()) }
    var carbs by remember(initial) { mutableStateOf((firstItem?.carbs ?: initial.carbs.toDouble()).takeIf { it > 0.0 }?.nutritionInputString().orEmpty()) }
    var fat by remember(initial) { mutableStateOf((firstItem?.fat ?: initial.fat.toDouble()).takeIf { it > 0.0 }?.nutritionInputString().orEmpty()) }
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
    // iOS opens the manual provider with an empty review draft. The first food
    // is created only after the user taps Add food, while restored/provider
    // results still keep their first item here.
    var hasManualFirstItem by remember(initial) { mutableStateOf(initial.items.isNotEmpty()) }
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
    var originalDescription by remember(initial) { mutableStateOf(initial.name) }

    val isPortionEditor = !labelMode && !barcodeMode
    val isManualReview = !describeMode && !barcodeMode && !labelMode && photoLabel == null && photoUri == null && photoBitmap == null
    val quantityNumber = itemQuantity.decimalOrNull()?.coerceIn(1.0, 1_000.0) ?: 1.0
    val enteredGrams = itemGrams.decimalOrNull()?.coerceIn(1.0, 5_000.0)
    val totalPortionGrams = enteredGrams?.let { if (gramsPerUnit) it * quantityNumber else it }
    val canUsePer100g = isPortionEditor && (totalPortionGrams ?: 0.0) > 0.0

    fun decimalText(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else "%.1f".format(Locale.US, value)

    fun firstManualItem(): MealItem = MealItem(name.trim(), calories.decimalOrNull() ?: 0.0, protein.decimalOrNull() ?: 0.0, carbs.decimalOrNull() ?: 0.0, fat.decimalOrNull() ?: 0.0, itemBrand.trim(), itemQuantity.decimalOrNull() ?: 1.0, itemUnit.trim(), totalPortionGrams, itemNotes.trim())

    fun applyFirstManualItem(food: MealItem) {
        name = food.name
        calories = food.calories.nutritionInputString()
        protein = food.protein.nutritionInputString()
        carbs = food.carbs.nutritionInputString()
        fat = food.fat.nutritionInputString()
        itemBrand = food.brand
        itemNotes = food.notes
        itemQuantity = food.quantity.toString()
        itemUnit = food.unit
        itemGrams = food.grams?.toString().orEmpty()
    }

    fun addManualItem() {
        val blank = MealItem("", 0, 0, 0, 0)
        if (!hasManualFirstItem) {
            hasManualFirstItem = true
            manualEditingItem = 0 to blank
        } else {
            val manualIndex = extraItems.size + 1
            extraItems = extraItems + blank
            manualEditingItem = manualIndex to blank
        }
    }

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
                if (describeMode) mealTitle = result.name
                if (describeMode && result.foods.isNotEmpty()) {
                    val analyzedFoods = result.foods.filter { it.name.isNotBlank() }
                    val firstFood = analyzedFoods.firstOrNull()
                    if (firstFood != null) {
                        name = firstFood.name
                        calories = firstFood.calories.toDouble().nutritionInputString()
                        protein = firstFood.protein.toDouble().nutritionInputString()
                        carbs = firstFood.carbs.toDouble().nutritionInputString()
                        fat = firstFood.fat.toDouble().nutritionInputString()
                        itemQuantity = "1"
                        itemUnit = "g"
                        itemGrams = firstFood.grams.takeIf { it > 0.0 }?.let(::decimalText).orEmpty()
                        extraItems = analyzedFoods.drop(1).map { food ->
                            MealItem(
                                name = food.name,
                                calories = food.calories,
                                protein = food.protein,
                                carbs = food.carbs,
                                fat = food.fat,
                                quantity = 1.0,
                                unit = "g",
                                grams = food.grams.takeIf { it > 0.0 },
                            )
                        }
                    }
                }
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
    val hasScannedMacros = listOf(calories, protein, carbs, fat).any { (it.decimalOrNull() ?: 0.0) > 0.0 }
    if (!analyzing && analysisError == null && hasScannedMacros && ((labelMode && (photoUri != null || photoBitmap != null)) || (barcodeMode && hasBarcodeProduct))) {
        var rename by remember { mutableStateOf(false) }
        var titleDraft by remember { mutableStateOf(name) }
        var editingMacros by remember { mutableStateOf(false) }
        val servings = if (barcodeMode) barcodeServings.decimalOrNull()?.coerceIn(1.0, 100.0) ?: 1.0 else 1.0
        val grams = if (barcodeMode) barcodeServingGrams.decimalOrNull()?.coerceIn(1.0, 5000.0) ?: 100.0 else consumedLabelGrams.decimalOrNull()?.coerceIn(1.0, 5000.0) ?: 100.0
        val factor = servings * grams / 100.0
        val per100 = MealItem(name, calories.decimalOrNull() ?: 0.0, protein.decimalOrNull() ?: 0.0, carbs.decimalOrNull() ?: 0.0, fat.decimalOrNull() ?: 0.0, grams = 100.0)
        val totals = NutritionTargets((per100.calories * factor).roundToInt(), (per100.protein * factor).roundToInt(), (per100.carbs * factor).roundToInt(), (per100.fat * factor).roundToInt())
        LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            item { SheetHeader(title, onBack) }
            if (labelMode && previewBitmap != null) item { Image(previewBitmap.asImageBitmap(), "Etiqueta nutricional", Modifier.fillMaxWidth().height(300.dp).clip(RoundedCornerShape(20.dp)), contentScale = ContentScale.Crop) }
            if (barcodeMode && barcodeImageUrl.isNotBlank()) item { BarcodeProductImage(barcodeImageUrl) }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).clickable(enabled = labelMode) { titleDraft = name; rename = true }) {
                        Text(name.ifBlank { "Alimento escaneado" }, fontFamily = FontFamily.Default, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
                        if (barcodeMode && brands.isNotBlank()) Text(brands, style = MaterialTheme.typography.body2, color = WildforceThemeTokens.textSecondary)
                    }
                    if (barcodeMode) ScannedAmountControl(servings, "Número de raciones", 1.0, 100.0, 1.0) { barcodeServings = it.nutritionInputString() }
                }
            }
            if (barcodeMode) item { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("Cantidad por ración", Modifier.weight(1f)); ScannedAmountControl(grams, "Cantidad por ración", 1.0, 5000.0, if (grams < 50) 5.0 else if (grams < 200) 10.0 else 25.0) { barcodeServingGrams = it.nutritionInputString() } } }
            item {
                Column(Modifier.fillMaxWidth().background(WildforceThemeTokens.backgroundSecondary, RoundedCornerShape(24.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    PhotoMacroSummary(totals)
                    if (labelMode) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("Cantidad consumida", Modifier.weight(1f), color = WildforceThemeTokens.textPrimary); ScannedAmountControl(grams, "Cantidad consumida", 1.0, 5000.0, if (grams < 50) 5.0 else if (grams < 200) 10.0 else 25.0) { consumedLabelGrams = it.nutritionInputString() } }
                    Row(Modifier.fillMaxWidth().clickable(enabled = labelMode) { editingMacros = true }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Por 100 g", Modifier.weight(1f), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
                        MealMacro(NutritionMacroIcon.Calories, "${per100.calories.roundToInt()}", NutritionCaloriesColor)
                        MealMacro(NutritionMacroIcon.Protein, "${per100.protein.roundToInt()}g", ProteinColor)
                        MealMacro(NutritionMacroIcon.Carbs, "${per100.carbs.roundToInt()}g", CarbsColor)
                        MealMacro(NutritionMacroIcon.Fat, "${per100.fat.roundToInt()}g", FatColor)
                        if (labelMode) Icon(Icons.Filled.Edit, "Editar macros", Modifier.size(16.dp), tint = WildforceThemeTokens.textSecondary)
                    }
                    Button(onClick = { scope.launch {
                        val totalGrams = servings * grams
                        val food = per100.copy(name = name.trim().ifBlank { "Alimento escaneado" }, brand = brands, quantity = totalGrams, unit = "g", grams = totalGrams, calories = per100.calories * factor, protein = per100.protein * factor, carbs = per100.carbs * factor, fat = per100.fat * factor)
                        val path = if (labelMode) withContext(Dispatchers.IO) { saveNutritionPhoto(context, photoUri, photoBitmap) } else initial.photoPath
                        onSave(initial.copy(name = food.name, calories = totals.calories, protein = totals.protein, carbs = totals.carbs, fat = totals.fat, items = listOf(food), photoPath = path, mediaUrl = barcodeImageUrl.takeIf { it.isNotBlank() } ?: initial.mediaUrl))
                    } }, Modifier.fillMaxWidth().padding(top = 8.dp), colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary, contentColor = WildforceThemeTokens.primaryButtonText), shape = RoundedCornerShape(8.dp)) { NutritionButtonLabel("Guardar ingesta", 16) }
                }
            }
        }
        if (rename) AlertDialog(onDismissRequest = { rename = false }, title = { Text("Nombre de la comida") }, text = { OutlinedTextField(titleDraft, { titleDraft = it.take(80) }, singleLine = true) }, confirmButton = { TextButton(onClick = { name = titleDraft.trim().ifBlank { "Alimento escaneado" }; rename = false }) { Text("Guardar") } }, dismissButton = { TextButton(onClick = { rename = false }) { Text("Cancelar") } })
        if (editingMacros) ManualFoodEditorDialog(per100, onDismiss = { editingMacros = false }, onDelete = null, onSave = { updated -> val scale = 100.0 / (updated.grams?.takeIf { it > 0.0 } ?: 100.0); calories = (updated.calories * scale).nutritionInputString(); protein = (updated.protein * scale).nutritionInputString(); carbs = (updated.carbs * scale).nutritionInputString(); fat = (updated.fat * scale).nutritionInputString(); editingMacros = false })
        return
    }
    if (describeMode && !descriptionAnalyzed) {
        val focusRequester = remember { FocusRequester() }
        val focusManager = LocalFocusManager.current
        LaunchedEffect(analyzing) {
            if (!analyzing) focusRequester.requestFocus()
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            SheetHeader(title, onBack)
            if (analyzing) {
                Column(Modifier.fillMaxWidth().padding(top = 70.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = WildforceThemeTokens.textPrimary)
                    Text("Analizando tu descripción...", Modifier.padding(top = 16.dp), style = MaterialTheme.typography.h6, color = WildforceThemeTokens.textPrimary)
                    Text("Estimando alimentos, macros y calorías a partir de tu texto.", Modifier.padding(top = 8.dp), style = MaterialTheme.typography.body2, color = WildforceThemeTokens.textSecondary, textAlign = TextAlign.Center)
                }
            } else {
                Column(Modifier.padding(top = 70.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("¿Qué has comido?", style = MaterialTheme.typography.h6, fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textPrimary)
                    Text("Describe tu comida con tus propias palabras. La IA estimará los ingredientes y las porciones.", style = MaterialTheme.typography.body2, color = WildforceThemeTokens.textSecondary)
                    OutlinedTextField(name, { name = it.take(300) }, Modifier.fillMaxWidth().focusRequester(focusRequester), placeholder = { Text("Ej.: 2 huevos, una tostada integral con aguacate y un café solo") }, minLines = 5, maxLines = 10, shape = RoundedCornerShape(8.dp), colors = TextFieldDefaults.outlinedTextFieldColors(focusedBorderColor = WildforceThemeTokens.textSecondary.copy(alpha = .4f), unfocusedBorderColor = WildforceThemeTokens.textSecondary.copy(alpha = .3f), backgroundColor = WildforceThemeTokens.backgroundSecondary))
                    Text("${(300 - name.length).coerceAtLeast(0)}", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
                }
                analysisError?.let { Text(it, color = MaterialTheme.colors.error, style = MaterialTheme.typography.caption) }
                Button(onClick = { originalDescription = name; focusManager.clearFocus(); scope.launch { runAnalysis { NutritionAnalyzer.describe(context, originalDescription, mealTitle) } } }, enabled = name.isNotBlank(), modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary, contentColor = WildforceThemeTokens.primaryButtonText), shape = RoundedCornerShape(8.dp)) { NutritionButtonLabel("Analizar descripción") }
            }
        }
        return
    }
    if (describeMode && descriptionAnalyzed) {
        var editingIndex by remember { mutableStateOf<Int?>(null) }
        var showInfo by remember { mutableStateOf(false) }
        var rename by remember { mutableStateOf(false) }
        var titleDraft by remember { mutableStateOf(mealTitle) }
        var gramsIndex by remember { mutableStateOf<Int?>(null) }
        var gramsDraft by remember { mutableStateOf("") }
        val foods = listOf(firstManualItem()) + extraItems
        fun updateFood(index: Int, food: MealItem) {
            if (index == 0) applyFirstManualItem(food)
            else extraItems = extraItems.mapIndexed { i, current -> if (i == index - 1) food else current }
        }
        fun updateGrams(index: Int, value: Double) {
            val food = foods[index]
            val grams = value.coerceIn(1.0, 5000.0)
            val factor = food.grams?.takeIf { it > 0.0 }?.let { grams / it } ?: 1.0
            updateFood(index, food.copy(grams = grams, calories = food.calories * factor, protein = food.protein * factor, carbs = food.carbs * factor, fat = food.fat * factor))
        }
        val totals = NutritionTargets(foods.sumOf { it.calories }.roundToInt(), foods.sumOf { it.protein }.roundToInt(), foods.sumOf { it.carbs }.roundToInt(), foods.sumOf { it.fat }.roundToInt())
        LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) { SheetHeader(title, onBack) }
                    TextButton(onClick = { name = originalDescription; descriptionAnalyzed = false; analysisError = null; extraItems = emptyList() }) { Text("Editar", color = WildforceThemeTokens.textPrimary) }
                    IconButton(onClick = { showInfo = true }) { Icon(Icons.Filled.Info, "Información de la estimación", tint = WildforceThemeTokens.textPrimary) }
                }
            }
            item { Spacer(Modifier.height(62.dp)) }
            item {
                Text(mealTitle.ifBlank { "Comida" }, Modifier.clickable { titleDraft = mealTitle; rename = true }.padding(horizontal = 4.dp), fontFamily = FontFamily.Default, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
            }
            item {
                Column(Modifier.fillMaxWidth().background(WildforceThemeTokens.backgroundSecondary, RoundedCornerShape(24.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DescriptionMacroBadge("Calorías", totals.calories, NutritionMacroIcon.Calories, NutritionCaloriesColor, Modifier.fillMaxWidth())
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DescriptionMacroBadge("Proteína", totals.protein, NutritionMacroIcon.Protein, ProteinColor, Modifier.weight(1f))
                            DescriptionMacroBadge("Carbos", totals.carbs, NutritionMacroIcon.Carbs, CarbsColor, Modifier.weight(1f))
                            DescriptionMacroBadge("Grasa", totals.fat, NutritionMacroIcon.Fat, FatColor, Modifier.weight(1f))
                        }
                    }
                    foods.forEachIndexed { index, food ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f).clickable { editingIndex = index }) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(food.name, fontFamily = FontFamily.Default, fontSize = 16.sp, color = WildforceThemeTokens.textPrimary)
                                    Icon(Icons.Filled.Edit, "Editar ${food.name}", Modifier.padding(start = 6.dp).size(16.dp), tint = WildforceThemeTokens.textSecondary)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    if (food.calories > 0) MealMacro(NutritionMacroIcon.Calories, "${food.calories.roundToInt()} kcal", NutritionCaloriesColor)
                                    MealMacro(NutritionMacroIcon.Protein, "${food.protein.roundToInt()}g", ProteinColor)
                                    MealMacro(NutritionMacroIcon.Carbs, "${food.carbs.roundToInt()}g", CarbsColor)
                                    MealMacro(NutritionMacroIcon.Fat, "${food.fat.roundToInt()}g", FatColor)
                                }
                            }
                            val amount = food.grams ?: 1.0
                            val step = when { amount < 50 -> 5.0; amount < 200 -> 10.0; else -> 25.0 }
                            IconButton(onClick = { updateGrams(index, amount - step) }, modifier = Modifier.size(32.dp).background(WildforceThemeTokens.background, CircleShape)) { Icon(Icons.Filled.Remove, "Reducir gramos", Modifier.size(16.dp), tint = WildforceThemeTokens.textPrimary) }
                            Text(decimalText(amount), modifier = Modifier.width(56.dp).clickable { gramsDraft = decimalText(amount); gramsIndex = index }, textAlign = TextAlign.Center, fontFamily = FontFamily.Default, color = WildforceThemeTokens.textPrimary)
                            IconButton(onClick = { updateGrams(index, amount + step) }, modifier = Modifier.size(32.dp).background(WildforceThemeTokens.background, CircleShape)) { Icon(Icons.Filled.Add, "Aumentar gramos", Modifier.size(16.dp), tint = WildforceThemeTokens.textPrimary) }
                        }
                    }
                    Button(onClick = {
                        val validFoods = foods.filter { it.name.isNotBlank() }
                        onSave(initial.copy(name = mealTitle.trim().ifBlank { "Comida" }, calories = totals.calories, protein = totals.protein, carbs = totals.carbs, fat = totals.fat, notes = notes.trim(), items = validFoods))
                    }, enabled = foods.any { it.name.isNotBlank() }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary, contentColor = WildforceThemeTokens.primaryButtonText), shape = RoundedCornerShape(8.dp)) { NutritionButtonLabel("Guardar ingesta", 16) }
                }
            }
        }
        if (showInfo) AlertDialog(onDismissRequest = { showInfo = false }, title = { Text("Estimación de IA") }, text = { Text(listOf("Estos valores son estimados y pueden no ser exactos.", notes).filter { it.isNotBlank() }.joinToString("\n\n")) }, confirmButton = { TextButton(onClick = { showInfo = false }) { Text("Entendido") } })
        if (rename) AlertDialog(onDismissRequest = { rename = false }, title = { Text("Nombre de la comida") }, text = { OutlinedTextField(titleDraft, { titleDraft = it.take(80) }, singleLine = true) }, confirmButton = { TextButton(onClick = { mealTitle = titleDraft.trim().ifBlank { "Comida" }; rename = false }) { Text("Guardar") } }, dismissButton = { TextButton(onClick = { rename = false }) { Text("Cancelar") } })
        gramsIndex?.let { index ->
            AlertDialog(onDismissRequest = { gramsIndex = null }, title = { Text("Cantidad en gramos") }, text = { OutlinedTextField(gramsDraft, { gramsDraft = decimalValue(it) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)) }, confirmButton = { TextButton(onClick = { gramsDraft.decimalOrNull()?.let { updateGrams(index, it) }; gramsIndex = null }, enabled = (gramsDraft.decimalOrNull() ?: 0.0) > 0.0) { Text("Listo") } }, dismissButton = { TextButton(onClick = { gramsIndex = null }) { Text("Cancelar") } })
        }
        editingIndex?.let { index ->
            ManualFoodEditorDialog(foods[index], onDismiss = { editingIndex = null }, onDelete = if (foods.size > 1) {{
                if (foods.size > 1) {
                    if (index == 0) { applyFirstManualItem(extraItems.first()); extraItems = extraItems.drop(1) }
                    else extraItems = extraItems.filterIndexed { i, _ -> i != index - 1 }
                }
                editingIndex = null
            }} else null, onSave = { updated -> updateFood(index, updated); editingIndex = null })
        }
        return
    }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
        // iOS keeps the only add affordance inside the review section; the
        // navigation header has no extra plus action.
        item { ReviewAppearance(reviewAppeared, 0) { if (describeMode && descriptionAnalyzed) { Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.weight(1f)) { SheetHeader("Revisar comida", onBack) }; TextButton(onClick = { name = originalDescription; descriptionAnalyzed = false; analysisError = null; extraItems = emptyList() }) { Text("Editar", color = NutritionCoral) } } } else SheetHeader(title, onBack) } }
        if (describeMode && descriptionAnalyzed) item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(mealTitle, { mealTitle = it.take(80) }, Modifier.fillMaxWidth(), label = { Text("Nombre de la comida") }, singleLine = true)
                PhotoMacroSummary(NutritionTargets(
                    ((calories.decimalOrNull() ?: 0.0) + extraItems.sumOf { it.calories }).roundToInt(),
                    ((protein.decimalOrNull() ?: 0.0) + extraItems.sumOf { it.protein }).roundToInt(),
                    ((carbs.decimalOrNull() ?: 0.0) + extraItems.sumOf { it.carbs }).roundToInt(),
                    ((fat.decimalOrNull() ?: 0.0) + extraItems.sumOf { it.fat }).roundToInt(),
                ))
                Text("Alimentos detectados", style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textPrimary)
            }
        }
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
                val summaryFoods = buildList {
                    if (hasManualFirstItem) add(firstManualItem())
                    addAll(extraItems.filter { it.name.isNotBlank() })
                }
                PhotoMacroSummary(NutritionTargets(
                    summaryFoods.sumOf { it.calories }.roundToInt(),
                    summaryFoods.sumOf { it.protein }.roundToInt(),
                    summaryFoods.sumOf { it.carbs }.roundToInt(),
                    summaryFoods.sumOf { it.fat }.roundToInt(),
                ))
            }
        }
        if (isManualReview) item {
            val foods = buildList {
                if (hasManualFirstItem) add(0 to firstManualItem())
                extraItems.forEachIndexed { index, food ->
                    add((if (hasManualFirstItem) index + 1 else index) to food)
                }
            }
            Column(Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(24.dp)).padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Alimentos", style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
                foods.forEachIndexed { rowIndex, item ->
                    val (index, food) = item
                    Row(Modifier.fillMaxWidth().clickable { manualEditingItem = index to food }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(food.name.ifBlank { "Alimento sin nombre" }, fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textPrimary)
                            Text(listOfNotNull(food.brand.takeIf { it.isNotBlank() }, "${decimalText(food.quantity)} ${food.unit}".trim(), food.grams?.let { "${decimalText(it)} g" }).joinToString(" · "), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary)
                            Text("P ${food.protein} g · C ${food.carbs} g · G ${food.fat} g", style = MaterialTheme.typography.overline, color = WildforceThemeTokens.textSecondary)
                        }
                        Text("${food.calories} kcal", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textPrimary)
                        Icon(Icons.Filled.ChevronRight, "Editar alimento", tint = WildforceThemeTokens.textSecondary)
                    }
                    if (rowIndex < foods.lastIndex) Divider(color = WildforceThemeTokens.textSecondary.copy(alpha = .16f))
                }
                TextButton(onClick = { addManualItem() }) {
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
                        Text("¿Qué has comido?", style = MaterialTheme.typography.h6, fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textPrimary)
                        Text("Describe tu comida con tus propias palabras. La IA estimará los ingredientes y las porciones.", style = MaterialTheme.typography.body2, color = WildforceThemeTokens.textSecondary)
                    }
                    OutlinedTextField(
                        name,
                        { name = it.take(if (describeMode) 300 else 80) },
                        Modifier.fillMaxWidth(),
                        label = { Text(if (describeMode && !descriptionAnalyzed) "Describe lo que has comido" else "Alimento o comida") },
                        placeholder = if (describeMode && !descriptionAnalyzed) ({ Text("Ej.: 2 huevos, una tostada con aguacate y café") }) else null,
                        minLines = if (describeMode && !descriptionAnalyzed) 5 else 1,
                        maxLines = if (describeMode && !descriptionAnalyzed) 10 else 1,
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
        if (describeMode && !descriptionAnalyzed) item {
            // iOS uses PrimaryButton here: solid textPrimary background,
            // primaryButtonText label, no outlined/accent treatment.
            Button(
                onClick = { originalDescription = name; scope.launch { runAnalysis { NutritionAnalyzer.describe(context, originalDescription, mealTitle) } } },
                enabled = name.isNotBlank() && !analyzing,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = WildforceThemeTokens.textPrimary,
                    contentColor = WildforceThemeTokens.primaryButtonText,
                ),
                shape = RoundedCornerShape(8.dp),
            ) { NutritionButtonLabel("Analizar descripción") }
        }
        if (analyzing) item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(Modifier.size(22.dp), color = NutritionCoral, strokeWidth = 2.dp); Text(if (labelMode) "Leyendo etiqueta…" else "Analizando nutrición…", Modifier.padding(start = 10.dp), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary) } }
        analysisError?.let { message -> item { Text(message, color = MaterialTheme.colors.error, style = MaterialTheme.typography.caption, modifier = Modifier.background(MaterialTheme.colors.error.copy(alpha = .08f), RoundedCornerShape(10.dp)).padding(10.dp)) } }
        // iOS chooses the meal type in the parent logging sheet and does not
        // repeat that picker inside NutritionLogReviewView.
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
                OutlinedButton(onClick = { contributionSourceTarget = "front" }, modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(if (contributionFront == null) "Foto frontal" else "Frontal")
                        if (contributionFront != null) Icon(Icons.Filled.CheckCircle, contentDescription = "Foto frontal añadida", modifier = Modifier.size(16.dp), tint = NutritionCoral)
                    }
                }
                OutlinedButton(onClick = { contributionSourceTarget = "nutrition" }, modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(if (contributionNutrition == null) "Foto nutrición" else "Nutrición")
                        if (contributionNutrition != null) Icon(Icons.Filled.CheckCircle, contentDescription = "Foto de nutrición añadida", modifier = Modifier.size(16.dp), tint = NutritionCoral)
                    }
                }
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
                        NumberField("Calorías", food.calories.nutritionInputString(), { value -> extraItems = extraItems.mapIndexed { itemIndex, current -> if (itemIndex == index) current.copy(calories = value.decimalOrNull()?.coerceIn(0.0, 8000.0) ?: 0.0) else current } }, Modifier.weight(1f), allowDecimal = true)
                        NumberField("Proteína g", food.protein.nutritionInputString(), { value -> extraItems = extraItems.mapIndexed { itemIndex, current -> if (itemIndex == index) current.copy(protein = value.decimalOrNull()?.coerceIn(0.0, 500.0) ?: 0.0) else current } }, Modifier.weight(1f), allowDecimal = true)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberField("Carbos g", food.carbs.nutritionInputString(), { value -> extraItems = extraItems.mapIndexed { itemIndex, current -> if (itemIndex == index) current.copy(carbs = value.decimalOrNull()?.coerceIn(0.0, 1000.0) ?: 0.0) else current } }, Modifier.weight(1f), allowDecimal = true)
                        NumberField("Grasa g", food.fat.nutritionInputString(), { value -> extraItems = extraItems.mapIndexed { itemIndex, current -> if (itemIndex == index) current.copy(fat = value.decimalOrNull()?.coerceIn(0.0, 500.0) ?: 0.0) else current } }, Modifier.weight(1f), allowDecimal = true)
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
                                    calories = current.calories * multiplier,
                                    protein = current.protein * multiplier,
                                    carbs = current.carbs * multiplier,
                                    fat = current.fat * multiplier,
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
        // Meal-level notes are persisted when supplied by an analysis or an
        // existing entry, but iOS does not expose an editable notes field in
        // NutritionLogReviewView.
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
                        (calories.decimalOrNull() ?: 0.0) * factor,
                        (protein.decimalOrNull() ?: 0.0) * factor,
                        (carbs.decimalOrNull() ?: 0.0) * factor,
                        (fat.decimalOrNull() ?: 0.0) * factor,
                        brand = if (barcodeMode) brands.trim() else itemBrand.trim(),
                        quantity = when { labelMode -> consumedLabelGrams.decimalOrNull()?.coerceIn(1.0, 5_000.0) ?: 100.0; barcodeMode -> barcodeServings.decimalOrNull()?.coerceIn(1.0, 100.0) ?: 1.0; else -> itemQuantity.decimalOrNull()?.coerceIn(1.0, 1_000.0) ?: 1.0 },
                        unit = when { labelMode -> "g"; barcodeMode -> "ración"; else -> itemUnit.trim() },
                        grams = when { labelMode -> consumedLabelGrams.decimalOrNull()?.coerceIn(1.0, 5_000.0) ?: 100.0; barcodeMode -> (barcodeServings.decimalOrNull()?.coerceIn(1.0, 100.0) ?: 1.0) * (barcodeServingGrams.decimalOrNull()?.coerceIn(1.0, 5_000.0) ?: 100.0); else -> totalPortionGrams },
                        notes = itemNotes.trim(),
                    )
                    val itemList = buildList {
                        if (!isManualReview || hasManualFirstItem) add(first)
                        addAll(extraItems.filter { it.name.isNotBlank() })
                    }
                    onSave(initial.copy(name = mealTitle.trim().ifBlank { itemList.firstOrNull()?.name.orEmpty() }, calories = itemList.sumOf { it.calories }.roundToInt(), protein = itemList.sumOf { it.protein }.roundToInt(), carbs = itemList.sumOf { it.carbs }.roundToInt(), fat = itemList.sumOf { it.fat }.roundToInt(), type = type, notes = notes.trim(), items = itemList, mediaUrl = barcodeImageUrl.trim().takeIf { it.isNotBlank() } ?: initial.mediaUrl))
                },
                enabled = if (isManualReview) {
                    (hasManualFirstItem && name.isNotBlank()) || extraItems.any { it.name.isNotBlank() }
                } else name.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary, contentColor = WildforceThemeTokens.primaryButtonText),
                shape = RoundedCornerShape(8.dp),
            ) { NutritionButtonLabel("Guardar ingesta", 16) } }
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
            if (index == 0) {
                val promoted = extraItems.firstOrNull()
                if (promoted != null) {
                    applyFirstManualItem(promoted)
                    extraItems = extraItems.drop(1)
                    hasManualFirstItem = true
                } else {
                    name = ""; calories = ""; protein = ""; carbs = ""; fat = ""; itemBrand = ""; itemNotes = ""; itemQuantity = "1"; itemUnit = ""; itemGrams = ""
                    hasManualFirstItem = false
                }
            }
            else extraItems = extraItems.filterIndexed { i, _ -> i != index - 1 }
            manualEditingItem = null
        }, onSave = { updated ->
            if (index == 0) { applyFirstManualItem(updated); hasManualFirstItem = true }
            else extraItems = extraItems.mapIndexed { i, current -> if (i == index - 1) updated else current }
            manualEditingItem = null
        })
    }
}

@Composable
private fun ScannedAmountControl(value: Double, label: String, min: Double, max: Double, step: Double, onChange: (Double) -> Unit) {
    var editing by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf("") }
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onChange((value - step).coerceAtLeast(min)) }, Modifier.size(32.dp).background(WildforceThemeTokens.background, CircleShape)) { Icon(Icons.Filled.Remove, "Reducir $label", Modifier.size(16.dp)) }
        Text(value.nutritionInputString(), Modifier.width(56.dp).clickable { draft = value.nutritionInputString(); editing = true }, textAlign = TextAlign.Center, color = WildforceThemeTokens.textPrimary)
        IconButton(onClick = { onChange((value + step).coerceAtMost(max)) }, Modifier.size(32.dp).background(WildforceThemeTokens.background, CircleShape)) { Icon(Icons.Filled.Add, "Aumentar $label", Modifier.size(16.dp)) }
    }
    if (editing) AlertDialog(onDismissRequest = { editing = false }, title = { Text(label) }, text = { OutlinedTextField(draft, { draft = decimalValue(it) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)) }, confirmButton = { TextButton(onClick = { onChange((draft.decimalOrNull() ?: value).coerceIn(min, max)); editing = false }, enabled = (draft.decimalOrNull() ?: 0.0) > 0) { Text("Listo") } }, dismissButton = { TextButton(onClick = { editing = false }) { Text("Cancelar") } })
}
@Composable
private fun DescriptionMacroBadge(label: String, value: Int, icon: NutritionMacroIcon, color: Color, modifier: Modifier = Modifier) {
    Row(modifier.border(1.dp, WildforceThemeTokens.textSecondary.copy(alpha = .10f), RoundedCornerShape(16.dp)).background(WildforceThemeTokens.backgroundSecondary, RoundedCornerShape(16.dp)).padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(40.dp).background(WildforceThemeTokens.textSecondary.copy(alpha = .05f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            NutritionMacroSymbol(icon, color, Modifier.size(20.dp))
        }
        Column {
            Text(label, fontFamily = FontFamily.Default, fontSize = 10.sp, color = WildforceThemeTokens.textSecondary, maxLines = 1)
            Text(value.toString(), fontFamily = FontFamily.Default, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
        }
    }
}
@Composable
private fun ManualFoodEditorDialog(food: MealItem, onDismiss: () -> Unit, onDelete: (() -> Unit)?, onSave: (MealItem) -> Unit) {
    val consumedGrams = food.grams?.takeIf { it > 0.0 } ?: 100.0
    val per100Factor = 100.0 / consumedGrams
    var name by remember(food) { mutableStateOf(food.name) }
    var brand by remember(food) { mutableStateOf(food.brand) }
    var kcal by remember(food) { mutableStateOf((food.calories * per100Factor).nutritionInputString()) }
    var protein by remember(food) { mutableStateOf((food.protein * per100Factor).nutritionInputString()) }
    var carbs by remember(food) { mutableStateOf((food.carbs * per100Factor).nutritionInputString()) }
    var fat by remember(food) { mutableStateOf((food.fat * per100Factor).nutritionInputString()) }
    var quantity by remember(food) { mutableStateOf(food.quantity.nutritionInputString()) }
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
                item { Button(onClick = { val factor = (grams.decimalOrNull() ?: 100.0) / 100.0; onSave(food.copy(name = name.trim(), brand = brand.trim(), calories = (kcal.decimalOrNull() ?: 0.0) * factor, protein = (protein.decimalOrNull() ?: 0.0) * factor, carbs = (carbs.decimalOrNull() ?: 0.0) * factor, fat = (fat.decimalOrNull() ?: 0.0) * factor, quantity = quantity.decimalOrNull() ?: 1.0, unit = unit.trim(), grams = grams.decimalOrNull(), notes = notes.trim())) }, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary, contentColor = WildforceThemeTokens.primaryButtonText)) { Text("Listo") } }
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

@Composable
private fun NutritionButtonLabel(label: String, size: Int = 14) {
    Box(Modifier.fillMaxWidth().heightIn(min = 24.dp), contentAlignment = Alignment.Center) {
        Text(label, fontFamily = Exo2FontFamily, fontSize = size.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp, textAlign = TextAlign.Center)
    }
}

@Composable private fun SheetHeader(title: String, onBack: () -> Unit, onAction: (() -> Unit)? = null) {
    val displayTitle = if (title == title.uppercase(Locale.US)) {
        title.lowercase(Locale.US).replaceFirstChar { it.uppercase() }
    } else title.replaceFirstChar { it.uppercase() }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Atrás", modifier = Modifier.size(22.dp)) }
        Text(displayTitle, Modifier.weight(1f), fontFamily = Exo2FontFamily, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textPrimary)
        if (onAction != null) IconButton(onClick = onAction) { Icon(Icons.Filled.Add, "Añadir alimento", tint = WildforceThemeTokens.textPrimary) }
    }
}
@Composable
private fun FilterChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text,
        Modifier
            .heightIn(min = 48.dp)
            .background(if (selected) WildforceThemeTokens.accent else WildforceThemeTokens.textSecondary.copy(alpha = .1f), CircleShape)
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = text
                this.selected = selected
            }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        color = if (selected) WildforceThemeTokens.primaryButtonText else WildforceThemeTokens.textPrimary,
        style = MaterialTheme.typography.caption,
    )
}

@Composable
private fun RecentFilterChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text,
        Modifier
            .heightIn(min = 48.dp)
            .background(if (selected) WildforceThemeTokens.textPrimary else Color.Transparent, CircleShape)
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = text
                this.selected = selected
            }
            .border(1.dp, if (selected) WildforceThemeTokens.textPrimary else WildforceThemeTokens.textSecondary.copy(alpha = .35f), CircleShape)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        color = if (selected) WildforceThemeTokens.primaryButtonText else WildforceThemeTokens.textPrimary,
        style = MaterialTheme.typography.caption,
    )
}
@Composable internal fun NumberField(label: String, value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier, allowDecimal: Boolean = false) { OutlinedTextField(value, onValueChange, modifier, label = { Text(label) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = if (allowDecimal) KeyboardType.Decimal else KeyboardType.Number)) }
@Composable internal fun EmptyState(title: String, body: String, icon: ImageVector = Icons.Filled.Eco) { Column(Modifier.fillMaxWidth().padding(vertical = 30.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(icon, null, tint = NutritionCoral, modifier = Modifier.size(42.dp)); Text(title, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary, modifier = Modifier.padding(top = 10.dp)); Text(body, style = MaterialTheme.typography.caption, textAlign = TextAlign.Center, color = WildforceThemeTokens.textSecondary, modifier = Modifier.padding(top = 5.dp)) } }
private fun digits(value: String) = value.filter(Char::isDigit).take(4)
internal fun String.decimalOrNull(): Double? = replace(',', '.').toDoubleOrNull()

private fun Double.nutritionInputString(): String =
    if (isFinite()) java.math.BigDecimal.valueOf(this).stripTrailingZeros().toPlainString() else ""
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
    val maxDimension = 1_024
    val largestDimension = maxOf(source.width, source.height)
    val bounded = if (largestDimension > maxDimension) {
        val scale = maxDimension.toFloat() / largestDimension
        Bitmap.createScaledBitmap(source, (source.width * scale).toInt(), (source.height * scale).toInt(), true)
    } else source
    val directory = File(context.filesDir, "NutritionLogMedia").apply { mkdirs() }
    val file = File(directory, "${UUID.randomUUID()}.jpg")
    listOf(70, 55, 40, 30).forEach { quality ->
        FileOutputStream(file).use { output -> check(bounded.compress(Bitmap.CompressFormat.JPEG, quality, output)) }
        if (file.length() <= 500L * 1024L) return@runCatching file.absolutePath
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
