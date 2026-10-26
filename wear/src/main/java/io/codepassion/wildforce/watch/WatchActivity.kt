package io.codepassion.wildforce.watch

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.wear.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Stop
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

private val serviceUuid = UUID.fromString("25e3a160-6c4d-4d73-9b4c-7b5bfeacf001")
private val stateUuid = UUID.fromString("25e3a161-6c4d-4d73-9b4c-7b5bfeacf001")
private val commandUuid = UUID.fromString("25e3a162-6c4d-4d73-9b4c-7b5bfeacf001")
private val metadataUuid = UUID.fromString("25e3a163-6c4d-4d73-9b4c-7b5bfeacf001")

private data class WatchState(
    val exerciseIndex: Int,
    val exerciseCount: Int,
    val setNumber: Int,
    val targetSets: Int,
    val reps: Int,
    val weightKg: Double,
    val restRemaining: Int?,
    val workoutTitle: String = "",
    val exerciseName: String = "",
    val isSummary: Boolean = false,
    val pendingFeedback: Boolean = false,
    val selectedFeedback: Int = 2,
    val durationMinutes: Int? = null,
    val distanceKm: Double? = null,
    val pathExercises: List<String> = emptyList(),
    val setStyleLabel: String = "",
    val setStyleDetails: List<String> = emptyList(),
) {
    val isResting get() = restRemaining != null
    val progress get() = if (exerciseCount == 0) 0f else (exerciseIndex + 1).toFloat() / exerciseCount
}

private enum class WatchPage { Activity, Path, Style, Insights }

private class WatchBleClient(
    private val onState: (WatchState?) -> Unit,
    private val onStatus: (String) -> Unit,
    private val onRemoteStateEmpty: () -> Unit,
) {
    private data class WatchMetadata(
        val workoutTitle: String,
        val exerciseName: String,
        val pathExercises: List<String>,
        val setStyleLabel: String,
        val setStyleDetails: List<String>,
    )
    private val handler = Handler(Looper.getMainLooper())
    private var gatt: BluetoothGatt? = null
    private var stateCharacteristic: BluetoothGattCharacteristic? = null
    private var commandCharacteristic: BluetoothGattCharacteristic? = null
    private var metadataCharacteristic: BluetoothGattCharacteristic? = null
    private var latestNumericState: WatchState? = null
    private val pendingCommands = ArrayDeque<String>()
    private var scanner: android.bluetooth.le.BluetoothLeScanner? = null
    private var scanning = false

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            stopScan()
            connect(result.device)
        }

        override fun onScanFailed(errorCode: Int) {
            onStatus("No se pudo buscar el reloj ($errorCode)")
        }
    }

    @SuppressLint("MissingPermission")
    fun start(adapter: android.bluetooth.BluetoothAdapter?) {
        scanner = adapter?.bluetoothLeScanner
        val activeScanner = scanner ?: run { onStatus("Bluetooth no disponible"); return }
        if (scanning) return
        scanning = true
        onStatus("Buscando el teléfono…")
        activeScanner.startScan(
            listOf(ScanFilter.Builder().setServiceUuid(android.os.ParcelUuid(serviceUuid)).build()),
            ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(),
            scanCallback,
        )
        handler.postDelayed({ if (scanning) { stopScan(); onStatus("No se encontró el teléfono") } }, 12_000)
    }

    @SuppressLint("MissingPermission")
    private fun connect(device: BluetoothDevice) {
        onStatus("Conectando…")
        gatt = device.connectGatt(null, false, object : BluetoothGattCallback() {
            override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    onStatus("Conectado")
                    g.discoverServices()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    stateCharacteristic = null
                    commandCharacteristic = null
                    onState(null)
                    onStatus("Desconectado")
                }
            }

            override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
                val service = g.getService(serviceUuid)
                stateCharacteristic = service?.getCharacteristic(stateUuid)
                commandCharacteristic = service?.getCharacteristic(commandUuid)
                metadataCharacteristic = service?.getCharacteristic(metadataUuid)
                if (stateCharacteristic == null || commandCharacteristic == null || metadataCharacteristic == null) {
                    onStatus("Bridge incompatible")
                    return
                }
                while (pendingCommands.isNotEmpty()) send(pendingCommands.removeFirst())
                readState()
            }

            override fun onCharacteristicRead(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray, status: Int) {
                if (characteristic.uuid == stateUuid && status == BluetoothGatt.GATT_SUCCESS) {
                    latestNumericState = parseState(value)
                    if (latestNumericState == null) onRemoteStateEmpty() else onState(latestNumericState)
                    readMetadata()
                } else if (characteristic.uuid == metadataUuid && status == BluetoothGatt.GATT_SUCCESS) {
                    parseMetadata(value)?.let { metadata ->
                        latestNumericState = latestNumericState?.copy(
                            workoutTitle = metadata.workoutTitle,
                            exerciseName = metadata.exerciseName,
                            pathExercises = metadata.pathExercises,
                            setStyleLabel = metadata.setStyleLabel,
                            setStyleDetails = metadata.setStyleDetails,
                        )
                        onState(latestNumericState)
                    }
                    handler.postDelayed(::readState, 1_000)
                }
            }
        })
    }

    @SuppressLint("MissingPermission")
    private fun readState() {
        stateCharacteristic?.let { gatt?.readCharacteristic(it) }
    }

    @SuppressLint("MissingPermission")
    private fun readMetadata() {
        metadataCharacteristic?.let { gatt?.readCharacteristic(it) }
    }

    @SuppressLint("MissingPermission")
    fun send(wire: String) {
        if (commandCharacteristic == null || gatt == null) {
            pendingCommands.addLast(wire)
            return
        }
        val characteristic = commandCharacteristic ?: return
        characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        characteristic.value = wire.toByteArray(StandardCharsets.UTF_8)
        gatt?.writeCharacteristic(characteristic)
    }

    @SuppressLint("MissingPermission")
    fun stop() {
        stopScan()
        gatt?.close()
        gatt = null
        latestNumericState = null
        onState(null)
    }

    @SuppressLint("MissingPermission")
    private fun stopScan() {
        if (scanning) scanner?.stopScan(scanCallback)
        scanning = false
    }

    private fun parseState(value: ByteArray): WatchState? = runCatching {
        val parts = String(value, StandardCharsets.UTF_8).split('|').map(String::toInt)
        if (parts.size < 7 || parts[0] < 0) return null
        WatchState(
            parts[0], parts[1], parts[2], parts[3], parts[4], parts[5] / 2.0,
            parts[6].takeIf { it >= 0 }, isSummary = parts.getOrNull(7) == 1,
            pendingFeedback = parts.getOrNull(8) == 1,
            selectedFeedback = parts.getOrNull(9) ?: 2,
            durationMinutes = parts.getOrNull(10)?.toInt()?.takeIf { it >= 0 },
            distanceKm = parts.getOrNull(11)?.toInt()?.takeIf { it >= 0 }?.div(10.0),
        )
    }.getOrNull()

    private fun parseMetadata(value: ByteArray): WatchMetadata? = runCatching {
        val parts = String(value, StandardCharsets.UTF_8).split('|')
        if (parts.size < 2) return null
        val title = Base64.getDecoder().decode(parts[0]).toString(StandardCharsets.UTF_8)
        val exercise = Base64.getDecoder().decode(parts[1]).toString(StandardCharsets.UTF_8)
        val path = parts.getOrNull(2)?.let { encoded ->
            runCatching {
                val array = JSONArray(Base64.getDecoder().decode(encoded).toString(StandardCharsets.UTF_8))
                buildList { for (index in 0 until array.length()) add(array.optString(index)) }
            }.getOrDefault(emptyList())
        }.orEmpty()
        val style = parts.getOrNull(3)?.let { encoded ->
            runCatching {
                val json = JSONObject(Base64.getDecoder().decode(encoded).toString(StandardCharsets.UTF_8))
                json.optString("label") to buildList {
                    val details = json.optJSONArray("details") ?: JSONArray()
                    for (index in 0 until details.length()) add(details.optString(index))
                }
            }.getOrDefault("" to emptyList())
        } ?: ("" to emptyList())
        WatchMetadata(title, exercise, path, style.first, style.second)
    }.getOrNull()
}

class WatchActivity : ComponentActivity() {
    private var connectedState by mutableStateOf<WatchState?>(null)
    private var lastKnownState by mutableStateOf<WatchState?>(null)
    private var watchPage by mutableStateOf(WatchPage.Activity)
    private var motionSnapshot by mutableStateOf(MotionSnapshot())
    private var status by mutableStateOf("Pulsa conectar para empezar")
    private var autoTrackingEnabled = false
    private var healthSessionStarted = false
    private var healthStartPending = false
    private val motionTracker by lazy { WatchMotionTracker(this) { publishMotionSnapshot(it) } }
    private val client by lazy {
        WatchBleClient(
            { state ->
                val previous = connectedState
                connectedState = state
                if (state != null) {
                    if (previous?.exerciseIndex != state.exerciseIndex || state.isResting || state.pendingFeedback || state.isSummary) {
                        watchPage = WatchPage.Activity
                    }
                    lastKnownState = state
                    persistState(state)
                    motionTracker.updateExercise(state.exerciseName, active = autoTrackingEnabled && !state.isResting && !state.isSummary)
                    if (!healthSessionStarted) {
                        startHealthSession()
                    }
                } else {
                    motionTracker.stop(reset = true)
                }
            },
            { status = it },
            {
                connectedState = null
                lastKnownState = null
                getPreferences(MODE_PRIVATE).edit().remove("last_state").apply()
            },
        )
    }
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (hasPermissions()) connectWatch()
    }
    private val healthPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (healthStartPending) {
            healthStartPending = false
            if (it) {
                healthSessionStarted = true
                WatchExerciseService.start(this)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        autoTrackingEnabled = getPreferences(MODE_PRIVATE).getBoolean("motion_auto_tracking_enabled", false)
        lastKnownState = restoreState()
        setContent { WatchScreen() }
        if (savedInstanceState == null && isRemoteWorkoutIntent(intent)) {
            applyRemoteMotionPreference(intent)
            connectWatch()
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (isRemoteWorkoutIntent(intent)) {
            applyRemoteMotionPreference(intent)
            connectWatch()
        }
    }

    private fun applyRemoteMotionPreference(intent: android.content.Intent) {
        intent.data?.getQueryParameter("motion")?.let { value ->
            autoTrackingEnabled = value == "1"
            getPreferences(MODE_PRIVATE).edit().putBoolean("motion_auto_tracking_enabled", autoTrackingEnabled).apply()
        }
    }

    private fun isRemoteWorkoutIntent(intent: android.content.Intent?): Boolean =
        intent?.action == android.content.Intent.ACTION_VIEW &&
            intent.data?.scheme == "wildforce" &&
            intent.data?.host == "workout" &&
            intent.data?.path == "/start"

    private fun hasPermissions() = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
        checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

    private fun connectWatch() {
        if (!hasPermissions()) {
            permissionLauncher.launch(arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT))
            return
        }
        val manager = getSystemService(android.bluetooth.BluetoothManager::class.java)
        client.start(manager.adapter)
    }

    private fun publishMotionSnapshot(snapshot: MotionSnapshot) {
        motionSnapshot = snapshot
        if (connectedState != null && snapshot.status != MotionStatus.Disabled) {
            client.send(encodeMotionSnapshot(snapshot))
        }
    }

    private fun encodeMotionSnapshot(snapshot: MotionSnapshot): String {
        val status = when (snapshot.status) {
            MotionStatus.Disabled -> 0
            MotionStatus.UnsupportedExercise -> 2
            MotionStatus.Ready -> 3
            MotionStatus.Tracking -> 4
            MotionStatus.LowConfidence -> 5
            MotionStatus.ManualFallback -> 6
        }
        val confidence = snapshot.confidence?.times(1_000.0)?.roundToInt() ?: -1
        val reps = snapshot.reps.takeLast(8).joinToString(";") { rep ->
            val velocity = when (rep.velocity) {
                "Explosive" -> 0
                "Slow" -> 2
                else -> 1
            }
            "${(rep.durationSeconds * 1_000.0).roundToInt()},${(rep.rangeOfMotion * 1_000.0).roundToInt()},$velocity"
        }
        return "m|$status|${snapshot.autoDetectedRepCount}|$confidence|$reps"
    }

    private fun startHealthSession() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            checkSelfPermission(Manifest.permission.BODY_SENSORS) != PackageManager.PERMISSION_GRANTED
        ) {
            if (healthStartPending) return
            healthStartPending = true
            healthPermissionLauncher.launch(Manifest.permission.BODY_SENSORS)
        } else {
            healthSessionStarted = true
            WatchExerciseService.start(this)
        }
    }

    override fun onDestroy() {
        motionTracker.stop(reset = true)
        client.stop()
        WatchExerciseService.stop(this)
        super.onDestroy()
    }

    @androidx.compose.runtime.Composable
    private fun WatchScreen() {
        MaterialTheme(colors = MaterialTheme.colors.copy(background = Color(0xFF101010), surface = Color(0xFF202020), primary = Color(0xFFFFB300))) {
            Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF101010)) {
                Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("WILDFORCE", color = Color(0xFFFFB300), fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                    Text(status, color = Color.LightGray, fontSize = 12.sp, textAlign = TextAlign.Center)
                    val state = connectedState ?: lastKnownState
                    if (state == null) {
                        CircularProgressIndicator(modifier = Modifier.size(42.dp), color = Color(0xFFFFB300), strokeWidth = 3.dp)
                        Button(onClick = ::connectWatch) { Text("CONECTAR") }
                    } else {
                        if (!state.pendingFeedback && !state.isSummary) WatchPageSelector(state)
                        if (state.isResting && watchPage == WatchPage.Path) {
                            WatchPathPage(state)
                        } else if (state.isResting) {
                        Text(formatSeconds(state.restRemaining ?: 0), color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.Bold)
                        Text(if (connectedState == null) "DESCANSO · SIN CONEXIÓN" else "DESCANSO", color = Color(0xFFFFB300), fontWeight = FontWeight.Bold)
                        if (WatchHealthState.heartRate > 0) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Filled.Favorite, contentDescription = null, tint = Color(0xFFFF80AB), modifier = Modifier.size(18.dp))
                                Text("${WatchHealthState.heartRate.roundToInt()} BPM", color = Color(0xFFFF80AB), fontSize = 16.sp)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(onClick = { client.send("a") }) { Icon(Icons.Filled.Add, "+30", tint = Color.White) }
                            IconButton(onClick = { client.send("s") }) { Icon(Icons.Filled.SkipNext, "Saltar", tint = Color.White) }
                        }
                        } else if (state.pendingFeedback) {
                        Text("¿CÓMO HA IDO?", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Selecciona el esfuerzo del ejercicio", color = Color.LightGray, textAlign = TextAlign.Center)
                        val feedback = listOf("MUY FÁCIL", "FÁCIL", "JUSTO", "DIFÍCIL", "MUY DIFÍCIL")
                        feedback.forEachIndexed { index, title ->
                            Button(onClick = { client.send("f|$title") }) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(title)
                                    Spacer(Modifier.weight(1f))
                                    if (index == state.selectedFeedback) {
                                        Icon(Icons.Filled.Check, contentDescription = "Seleccionado", modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                        Button(onClick = { client.send("fc") }, enabled = state.selectedFeedback in feedback.indices) {
                            Text("CONTINUAR")
                        }
                        } else if (state.isSummary) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF69F0AE), modifier = Modifier.size(56.dp))
                            Text("ENTRENAMIENTO COMPLETADO", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                            if (state.workoutTitle.isNotBlank()) Text(state.workoutTitle, color = Color.LightGray, maxLines = 2, textAlign = TextAlign.Center)
                        } else if (watchPage == WatchPage.Path) {
                            WatchPathPage(state)
                        } else if (watchPage == WatchPage.Style) {
                            WatchStylePage(state)
                        } else if (watchPage == WatchPage.Insights) {
                            MotionInsights(snapshot = motionSnapshot)
                        } else {
                            Text(state.exerciseName.ifBlank { "Ejercicio ${state.exerciseIndex + 1}/${state.exerciseCount}" }, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            if (state.workoutTitle.isNotBlank()) Text(state.workoutTitle, color = Color.LightGray, maxLines = 1)
                            Text("Serie ${state.setNumber}/${state.targetSets}", color = Color.LightGray)
                            WatchMetricStepper(
                                label = "Reps",
                                value = state.reps.toString(),
                                onDecrement = { client.send("r|${(state.reps - 1).coerceAtLeast(0)}") },
                                onIncrement = { client.send("r|${(state.reps + 1).coerceAtMost(999)}") },
                            )
                            WatchMetricStepper(
                                label = "kg",
                                value = "%.1f".format(state.weightKg),
                                onDecrement = { client.send("w|${((state.weightKg - 0.5).coerceAtLeast(0.0) * 2).toInt()}") },
                                onIncrement = { client.send("w|${((state.weightKg + 0.5).coerceAtMost(999.0) * 2).toInt()}") },
                            )
                            state.durationMinutes?.let { minutes ->
                                WatchMetricStepper(
                                    label = "min",
                                    value = minutes.toString(),
                                    onDecrement = { client.send("d|${(minutes - 1).coerceAtLeast(0)}") },
                                    onIncrement = { client.send("d|${(minutes + 1).coerceAtMost(999)}") },
                                )
                            }
                            state.distanceKm?.let { distance ->
                                WatchMetricStepper(
                                    label = "km",
                                    value = "%.1f".format(distance),
                                    onDecrement = { client.send("x|${((distance - 0.1).coerceAtLeast(0.0) * 10).toInt()}") },
                                    onIncrement = { client.send("x|${((distance + 0.1).coerceAtMost(999.0) * 10).toInt()}") },
                                )
                            }
                            MotionInsights(snapshot = motionSnapshot)
                            Button(onClick = {
                                motionTracker.finishSet().takeIf { it > 0 }?.let { client.send("r|$it") }
                                client.send("c")
                            }) {
                                Icon(Icons.Filled.Stop, null, modifier = Modifier.size(18.dp))
                                Text("  COMPLETAR SERIE")
                            }
                        }
                    }
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun WatchPageSelector(state: WatchState) {
        val pages = buildList {
            add(WatchPage.Activity to "EJ")
            if (state.pathExercises.isNotEmpty()) add(WatchPage.Path to "RUTA")
            if (state.setStyleLabel.isNotBlank()) add(WatchPage.Style to "ESTILO")
            if (autoTrackingEnabled) add(WatchPage.Insights to "INSIGHTS")
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            pages.forEach { (page, label) ->
                Button(onClick = { watchPage = page }, modifier = Modifier.weight(1f)) {
                    Text(label, fontSize = 9.sp, maxLines = 1)
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun WatchPathPage(state: WatchState) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("RUTA DEL ENTRENAMIENTO", color = Color(0xFFFFB300), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            state.pathExercises.ifEmpty { listOf(state.exerciseName) }.forEachIndexed { index, name ->
                Text(
                    "${if (index == state.exerciseIndex) "•" else "○"} $name",
                    color = if (index == state.exerciseIndex) Color.White else Color.LightGray,
                    fontSize = 12.sp,
                    maxLines = 1,
                )
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun WatchStylePage(state: WatchState) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("ESTILO DE SERIE", color = Color(0xFFFFB300), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(state.setStyleLabel.ifBlank { "Series normales" }, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            state.setStyleDetails.ifEmpty { listOf("Sin instrucciones adicionales") }.forEach { detail ->
                Text(detail, color = Color.LightGray, fontSize = 12.sp)
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun WatchMetricStepper(
        label: String,
        value: String,
        onDecrement: () -> Unit,
        onIncrement: () -> Unit,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(label, color = Color.LightGray, fontSize = 12.sp)
            IconButton(onClick = onDecrement) {
                Icon(Icons.Filled.Remove, "Reducir $label", tint = Color.LightGray)
            }
            Text(value, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = onIncrement) {
                Icon(Icons.Filled.Add, "Aumentar $label", tint = Color.LightGray)
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun MotionInsights(snapshot: MotionSnapshot) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("REP INSIGHTS", color = Color(0xFFFFB300), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(snapshot.statusTitle, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(snapshot.statusSubtitle, color = Color.LightGray, fontSize = 11.sp, textAlign = TextAlign.Center)
            if (snapshot.reps.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    snapshot.reps.takeLast(8).forEach { rep ->
                        Text(
                            "${rep.number}\n${(rep.rangeOfMotion * 100).roundToInt()}%",
                            color = when (rep.velocity) {
                                "Explosive" -> Color.Red
                                "Slow" -> Color.Yellow
                                else -> Color(0xFFFFB300)
                            },
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }

    private fun formatSeconds(seconds: Int): String = "%02d:%02d".format(seconds / 60, seconds % 60)

    private fun persistState(state: WatchState) {
        getPreferences(MODE_PRIVATE).edit().putString(
            "last_state",
            JSONObject()
                .put("exerciseIndex", state.exerciseIndex)
                .put("exerciseCount", state.exerciseCount)
                .put("setNumber", state.setNumber)
                .put("targetSets", state.targetSets)
                .put("reps", state.reps)
                .put("weightKg", state.weightKg)
                .put("restRemaining", state.restRemaining ?: -1)
                .put("workoutTitle", state.workoutTitle)
                .put("exerciseName", state.exerciseName)
                .put("isSummary", state.isSummary)
                .put("pathExercises", JSONArray().apply { state.pathExercises.forEach(::put) })
                .put("setStyleLabel", state.setStyleLabel)
                .put("setStyleDetails", JSONArray().apply { state.setStyleDetails.forEach(::put) })
                .toString(),
        ).apply()
    }

    private fun restoreState(): WatchState? = runCatching {
        val json = JSONObject(getPreferences(MODE_PRIVATE).getString("last_state", null) ?: return null)
        WatchState(
            exerciseIndex = json.getInt("exerciseIndex"),
            exerciseCount = json.getInt("exerciseCount"),
            setNumber = json.getInt("setNumber"),
            targetSets = json.getInt("targetSets"),
            reps = json.getInt("reps"),
            weightKg = json.getDouble("weightKg"),
            restRemaining = json.getInt("restRemaining").takeIf { it >= 0 },
            workoutTitle = json.optString("workoutTitle"),
            exerciseName = json.optString("exerciseName"),
            isSummary = json.optBoolean("isSummary"),
            pathExercises = json.optJSONArray("pathExercises")?.let { array ->
                buildList { for (index in 0 until array.length()) add(array.optString(index)) }
            }.orEmpty(),
            setStyleLabel = json.optString("setStyleLabel"),
            setStyleDetails = json.optJSONArray("setStyleDetails")?.let { array ->
                buildList { for (index in 0 until array.length()) add(array.optString(index)) }
            }.orEmpty(),
        )
    }.getOrNull()
}
