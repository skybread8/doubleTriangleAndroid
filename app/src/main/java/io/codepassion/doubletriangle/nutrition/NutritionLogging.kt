package io.codepassion.doubletriangle.nutrition

import android.net.Uri
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.codepassion.doubletriangle.core.designsystem.AntonFontFamily
import io.codepassion.doubletriangle.core.designsystem.WildforceThemeTokens
import io.codepassion.doubletriangle.core.designsystem.liquidGlass
import io.codepassion.doubletriangle.core.designsystem.liquidGlassBackground
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
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
    var pendingPhotoMethod by remember { mutableStateOf<LogMethod?>(null) }
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
    val cameraPicker = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) { photoBitmap = bitmap; method = pendingPhotoMethod ?: LogMethod.Photo }
        pendingPhotoMethod = null
    }
    LaunchedEffect(initialMethod) {
        when (initialMethod) {
            LogMethod.Photo, LogMethod.Label -> { pendingPhotoMethod = initialMethod; cameraPicker.launch(null) }
            LogMethod.Barcode -> barcodeScanner.startScan()
                .addOnSuccessListener { barcode -> scannedBarcode = barcode.rawValue.orEmpty(); method = LogMethod.Barcode }
                .addOnFailureListener { method = LogMethod.Barcode }
            else -> Unit
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            Modifier.fillMaxWidth().fillMaxHeight(.94f).padding(horizontal = 8.dp),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = WildforceThemeTokens.background,
        ) {
            if (method == null) {
                LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) { Text("REGISTRAR INGESTA", fontFamily = AntonFontFamily, style = MaterialTheme.typography.h5, color = WildforceThemeTokens.textPrimary); Text(selectedDate.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale("es"))), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary) }
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
                            PrimaryMethod(LogMethod.Barcode, Icons.Filled.QrCodeScanner, Modifier.weight(1f)) {
                                barcodeScanner.startScan()
                                    .addOnSuccessListener { barcode -> scannedBarcode = barcode.rawValue.orEmpty(); method = LogMethod.Barcode }
                                    .addOnFailureListener { method = LogMethod.Barcode }
                            }
                            PrimaryMethod(LogMethod.Describe, Icons.Filled.EditNote, Modifier.weight(1f)) { method = LogMethod.Describe }
                            PrimaryMethod(LogMethod.Photo, Icons.Filled.CameraAlt, Modifier.weight(1f)) { pendingPhotoMethod = LogMethod.Photo; cameraPicker.launch(null) }
                        }
                    }
                    items(listOf(LogMethod.Recent, LogMethod.Label, LogMethod.Search, LogMethod.Manual)) { item ->
                        MethodRow(item) {
                            if (item == LogMethod.Label) { pendingPhotoMethod = item; cameraPicker.launch(null) } else method = item
                        }
                    }
                }
            } else if (method == LogMethod.Recent) {
                RecentPicker(recent, onBack = { method = null }, onSelected = { source ->
                    onSave(source.copy(id = System.currentTimeMillis(), type = mealType, loggedDate = selectedDate, source = "recent", isBookmarked = false))
                })
            } else if (method == LogMethod.Search) {
                FoodSearch(mealType, selectedDate, onBack = { method = null }, onSave = onSave)
            } else {
                MealEditor(
                    title = method!!.title,
                    initial = initialEntry ?: defaultFor(method!!, mealType, selectedDate, photoUri).copy(name = initialTitle ?: defaultFor(method!!, mealType, selectedDate, photoUri).name),
                    onBack = { method = null; photoUri = null },
                    onSave = onSave,
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

@Composable
private fun PrimaryMethod(method: LogMethod, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.height(108.dp).background(WildforceThemeTokens.textSecondary.copy(alpha = .08f), RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(icon, null, tint = WildforceThemeTokens.textPrimary, modifier = Modifier.size(28.dp))
        Text(method.title, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textPrimary, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun MethodRow(method: LogMethod, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(when (method) { LogMethod.Recent -> Icons.Filled.History; LogMethod.Label -> Icons.Filled.DocumentScanner; LogMethod.Search -> Icons.Filled.Search; else -> Icons.Filled.Edit }, null, modifier = Modifier.size(44.dp).padding(10.dp), tint = NutritionCoral)
        Column(Modifier.weight(1f)) { Text(method.title, color = WildforceThemeTokens.textPrimary); Text(method.description, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary) }
        Icon(Icons.Filled.ChevronRight, null, tint = WildforceThemeTokens.textSecondary)
    }
}

@Composable
private fun RecentPicker(recent: List<MealLog>, onBack: () -> Unit, onSelected: (MealLog) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { SheetHeader("COMIDAS RECIENTES", onBack) }
        if (recent.isEmpty()) item { EmptyState("Sin comidas recientes", "Los alimentos que registres aparecerán aquí para repetirlos rápidamente.") }
        items(recent, key = { it.id }) { entry ->
            Row(Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(16.dp)).clickable { onSelected(entry) }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Restaurant, null, tint = NutritionCoral)
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) { Text(entry.name, fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textPrimary); Text("${entry.type.title} · ${entry.calories} kcal", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary) }
                Icon(Icons.Filled.AddCircle, "Añadir", tint = NutritionCoral)
            }
        }
    }
}

private data class CatalogFood(val name: String, val brand: String, val calories: Int, val protein: Int, val carbs: Int, val fat: Int)
private val foodCatalog = listOf(
    CatalogFood("Plátano", "Alimento genérico · 100 g", 89, 1, 23, 0), CatalogFood("Manzana", "Alimento genérico · 100 g", 52, 0, 14, 0),
    CatalogFood("Pechuga de pollo", "Cocinada · 100 g", 165, 31, 0, 4), CatalogFood("Arroz blanco", "Cocido · 100 g", 130, 3, 28, 0),
    CatalogFood("Yogur griego", "Natural · 100 g", 97, 9, 4, 5), CatalogFood("Avena", "Copos · 100 g", 389, 17, 66, 7),
    CatalogFood("Huevo", "Unidad grande", 78, 6, 1, 5), CatalogFood("Salmón", "Cocinado · 100 g", 208, 20, 0, 13),
)

@Composable
private fun FoodSearch(type: MealType, date: LocalDate, onBack: () -> Unit, onSave: (MealLog) -> Unit) {
    var query by remember { mutableStateOf("") }
    val results = foodCatalog.filter { query.isBlank() || it.name.contains(query, true) }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { SheetHeader("BUSCAR ALIMENTO", onBack) }
        item { OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), label = { Text("Buscar alimento") }, leadingIcon = { Icon(Icons.Filled.Search, null) }, singleLine = true) }
        items(results) { food ->
            Row(Modifier.fillMaxWidth().clickable { onSave(MealLog(System.currentTimeMillis(), food.name, food.calories, food.protein, food.carbs, food.fat, type, date, food.brand, "search")) }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text(food.name, color = WildforceThemeTokens.textPrimary, fontWeight = FontWeight.SemiBold); Text(food.brand, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary); Text("P ${food.protein}g  ·  C ${food.carbs}g  ·  G ${food.fat}g", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary) }
                Text("${food.calories} kcal", fontWeight = FontWeight.Bold, color = NutritionCoral)
            }
        }
    }
}

@Composable
private fun MealEditor(title: String, initial: MealLog, onBack: () -> Unit, onSave: (MealLog) -> Unit, photoLabel: String?, photoUri: Uri?, photoBitmap: Bitmap?, describeMode: Boolean, barcodeMode: Boolean, labelMode: Boolean, initialBarcode: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val firstItem = initial.items.firstOrNull()
    val secondItem = initial.items.getOrNull(1)
    var name by remember(initial) { mutableStateOf(firstItem?.name ?: initial.name) }
    var calories by remember(initial) { mutableStateOf((firstItem?.calories ?: initial.calories).takeIf { it > 0 }?.toString().orEmpty()) }
    var protein by remember(initial) { mutableStateOf((firstItem?.protein ?: initial.protein).takeIf { it > 0 }?.toString().orEmpty()) }
    var carbs by remember(initial) { mutableStateOf((firstItem?.carbs ?: initial.carbs).takeIf { it > 0 }?.toString().orEmpty()) }
    var fat by remember(initial) { mutableStateOf((firstItem?.fat ?: initial.fat).takeIf { it > 0 }?.toString().orEmpty()) }
    var notes by remember(initial) { mutableStateOf(initial.notes) }
    var type by remember(initial) { mutableStateOf(initial.type) }
    var barcode by remember(initialBarcode) { mutableStateOf(initialBarcode) }
    var brands by remember { mutableStateOf("") }
    var contributionFront by remember { mutableStateOf<Uri?>(null) }
    var contributionNutrition by remember { mutableStateOf<Uri?>(null) }
    var contributionMessage by remember { mutableStateOf<String?>(null) }
    val contributionFrontPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { contributionFront = it }
    val contributionNutritionPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { contributionNutrition = it }
    var secondName by remember(initial) { mutableStateOf(secondItem?.name.orEmpty()) }
    var secondCalories by remember(initial) { mutableStateOf(secondItem?.calories?.toString().orEmpty()) }
    var secondProtein by remember(initial) { mutableStateOf(secondItem?.protein?.toString().orEmpty()) }
    var secondCarbs by remember(initial) { mutableStateOf(secondItem?.carbs?.toString().orEmpty()) }
    var secondFat by remember(initial) { mutableStateOf(secondItem?.fat?.toString().orEmpty()) }
    var showSecondItem by remember(initial) { mutableStateOf(initial.items.size > 1) }
    var analyzing by remember { mutableStateOf(false) }
    var analysisError by remember { mutableStateOf<String?>(null) }

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
            }
            .onFailure { analysisError = it.message ?: "No se pudo completar el análisis" }
        analyzing = false
    }

    LaunchedEffect(photoUri, photoBitmap, labelMode) {
        if (photoUri != null) runAnalysis { NutritionAnalyzer.image(context, photoUri, labelMode) }
        else if (photoBitmap != null) runAnalysis { NutritionAnalyzer.image(context, photoBitmap, labelMode) }
    }
    LaunchedEffect(initialBarcode) {
        if (initialBarcode.isNotBlank()) runAnalysis { NutritionAnalyzer.barcode(initialBarcode) }
    }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
        item { SheetHeader(title.uppercase(), onBack) }
        if (photoLabel != null) item { Row(Modifier.fillMaxWidth().background(NutritionCoral.copy(alpha = .08f), RoundedCornerShape(14.dp)).padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.Image, null, tint = NutritionCoral); Text("Imagen seleccionada · $photoLabel", Modifier.padding(start = 10.dp), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textPrimary) } }
        if (barcodeMode) item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(barcode, { barcode = it.filter(Char::isDigit).take(14) }, Modifier.weight(1f), label = { Text("Código de barras") }, leadingIcon = { Icon(Icons.Filled.QrCodeScanner, null) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                IconButton(onClick = { scope.launch { runAnalysis { NutritionAnalyzer.barcode(barcode) } } }, enabled = barcode.length >= 8 && !analyzing) { Icon(Icons.Filled.Search, "Buscar producto", tint = NutritionCoral) }
            }
        }
        item { OutlinedTextField(name, { name = it.take(80) }, Modifier.fillMaxWidth(), label = { Text(if (describeMode) "Describe lo que has comido" else "Alimento o comida") }, minLines = if (describeMode) 3 else 1) }
        if (barcodeMode) {
            item { OutlinedTextField(brands, { brands = it.take(80) }, Modifier.fillMaxWidth(), label = { Text("Marca para contribuir") }, singleLine = true) }
        }
        if (describeMode) item { OutlinedButton(onClick = { scope.launch { runAnalysis { NutritionAnalyzer.describe(context, name) } } }, enabled = name.isNotBlank() && !analyzing, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Filled.AutoAwesome, null, tint = NutritionCoral); Text("ANALIZAR DESCRIPCIÓN", Modifier.padding(start = 8.dp), color = NutritionCoral) } }
        if (analyzing) item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(Modifier.size(22.dp), color = NutritionCoral, strokeWidth = 2.dp); Text(if (labelMode) "Leyendo etiqueta…" else "Analizando nutrición…", Modifier.padding(start = 10.dp), style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary) } }
        analysisError?.let { message -> item { Text(message, color = MaterialTheme.colors.error, style = MaterialTheme.typography.caption, modifier = Modifier.background(MaterialTheme.colors.error.copy(alpha = .08f), RoundedCornerShape(10.dp)).padding(10.dp)) } }
        item { Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) { MealType.entries.forEach { item -> FilterChip(item.title, item == type) { type = item } } } }
        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { NumberField("Calorías", calories, { calories = digits(it) }, Modifier.weight(1f)); NumberField("Proteína g", protein, { protein = digits(it) }, Modifier.weight(1f)) } }
        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { NumberField("Carbos g", carbs, { carbs = digits(it) }, Modifier.weight(1f)); NumberField("Grasa g", fat, { fat = digits(it) }, Modifier.weight(1f)) } }
        if (barcodeMode) {
            item { Text("Si el producto no existe en Open Food Facts, puedes añadirlo con fotos del frontal y de la tabla nutricional.", style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary) }
            item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { contributionFrontPicker.launch("image/*") }, modifier = Modifier.weight(1f)) { Text(if (contributionFront == null) "FOTO FRONTAL" else "FRONTAL ✓") }
                OutlinedButton(onClick = { contributionNutritionPicker.launch("image/*") }, modifier = Modifier.weight(1f)) { Text(if (contributionNutrition == null) "FOTO NUTRICIÓN" else "NUTRICIÓN ✓") }
            } }
            item {
                val canContribute = barcode.length in 8..14 && name.isNotBlank() && brands.isNotBlank() && (calories.toDoubleOrNull() ?: 0.0) > 0 && contributionFront != null && contributionNutrition != null && !analyzing
                OutlinedButton(onClick = {
                    scope.launch {
                        analyzing = true; contributionMessage = null
                        runCatching { withContext(Dispatchers.IO) { NutritionAnalyzer.contributeOpenFoodFacts(context, barcode, name.trim(), brands.trim(), calories.toDoubleOrNull() ?: 0.0, protein.toDoubleOrNull() ?: 0.0, carbs.toDoubleOrNull() ?: 0.0, fat.toDoubleOrNull() ?: 0.0, contributionFront!!, contributionNutrition!!) } }
                            .onSuccess { contributionMessage = "Producto enviado a Open Food Facts." }
                            .onFailure { contributionMessage = it.message ?: "No se pudo enviar el producto." }
                        analyzing = false
                    }
                }, enabled = canContribute, modifier = Modifier.fillMaxWidth()) { Text("CONTRIBUIR PRODUCTO") }
            }
            contributionMessage?.let { message -> item { Text(message, style = MaterialTheme.typography.caption, color = if (message.startsWith("Producto enviado")) NutritionCoral else MaterialTheme.colors.error) } }
        }
        item {
            TextButton(onClick = { showSecondItem = !showSecondItem }) { Icon(if (showSecondItem) Icons.Filled.Remove else Icons.Filled.Add, null, tint = NutritionCoral); Text(if (showSecondItem) "QUITAR ALIMENTO" else "AÑADIR OTRO ALIMENTO", Modifier.padding(start = 6.dp), color = NutritionCoral) }
        }
        if (showSecondItem) {
            item { OutlinedTextField(secondName, { secondName = it.take(80) }, Modifier.fillMaxWidth(), label = { Text("Segundo alimento") }, singleLine = true) }
            item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { NumberField("Calorías", secondCalories, { secondCalories = digits(it) }, Modifier.weight(1f)); NumberField("Proteína g", secondProtein, { secondProtein = digits(it) }, Modifier.weight(1f)) } }
            item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { NumberField("Carbos g", secondCarbs, { secondCarbs = digits(it) }, Modifier.weight(1f)); NumberField("Grasa g", secondFat, { secondFat = digits(it) }, Modifier.weight(1f)) } }
        }
        item { OutlinedTextField(notes, { notes = it.take(240) }, Modifier.fillMaxWidth(), label = { Text("Notas (opcional)") }, minLines = 2) }
        item {
            Button(
                onClick = {
                    val first = MealItem(name.trim(), calories.toIntOrNull() ?: 0, protein.toIntOrNull() ?: 0, carbs.toIntOrNull() ?: 0, fat.toIntOrNull() ?: 0)
                    val itemList = buildList { add(first); if (showSecondItem && secondName.isNotBlank()) add(MealItem(secondName.trim(), secondCalories.toIntOrNull() ?: 0, secondProtein.toIntOrNull() ?: 0, secondCarbs.toIntOrNull() ?: 0, secondFat.toIntOrNull() ?: 0)) }
                    onSave(initial.copy(name = name.trim(), calories = itemList.sumOf { it.calories }, protein = itemList.sumOf { it.protein }, carbs = itemList.sumOf { it.carbs }, fat = itemList.sumOf { it.fat }, type = type, notes = notes.trim(), items = itemList))
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary, contentColor = WildforceThemeTokens.backgroundSecondary),
                shape = RoundedCornerShape(14.dp),
            ) { Text("GUARDAR INGESTA", fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable private fun SheetHeader(title: String, onBack: () -> Unit) { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Atrás", modifier = Modifier.size(22.dp)) }; Text(title, fontFamily = AntonFontFamily, style = MaterialTheme.typography.h5, color = WildforceThemeTokens.textPrimary) } }
@Composable private fun FilterChip(text: String, selected: Boolean, onClick: () -> Unit) { Text(text, Modifier.background(if (selected) WildforceThemeTokens.accent else WildforceThemeTokens.textSecondary.copy(alpha = .1f), CircleShape).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 8.dp), color = if (selected) WildforceThemeTokens.primaryButtonText else WildforceThemeTokens.textPrimary, style = MaterialTheme.typography.caption) }
@Composable internal fun NumberField(label: String, value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) { OutlinedTextField(value, onValueChange, modifier, label = { Text(label) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)) }
@Composable internal fun EmptyState(title: String, body: String) { Column(Modifier.fillMaxWidth().padding(vertical = 30.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Filled.Eco, null, tint = NutritionCoral, modifier = Modifier.size(42.dp)); Text(title, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary, modifier = Modifier.padding(top = 10.dp)); Text(body, style = MaterialTheme.typography.caption, textAlign = TextAlign.Center, color = WildforceThemeTokens.textSecondary, modifier = Modifier.padding(top = 5.dp)) } }
private fun digits(value: String) = value.filter(Char::isDigit).take(4)

private fun defaultFor(method: LogMethod, type: MealType, date: LocalDate, uri: Uri?) = MealLog(
    id = System.currentTimeMillis(),
    name = when (method) { LogMethod.Photo -> "Comida fotografiada"; LogMethod.Label -> "Alimento de etiqueta"; else -> "" },
    calories = 0, protein = 0, carbs = 0, fat = 0, type = type, loggedDate = date,
    notes = uri?.let { "Imagen: ${it.lastPathSegment}" }.orEmpty(), source = method.name.lowercase(),
)
