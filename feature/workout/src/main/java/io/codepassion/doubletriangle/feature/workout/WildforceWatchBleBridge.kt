package io.codepassion.doubletriangle.feature.workout

import android.Manifest
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.util.UUID

/**
 * Offline protocol between Wildforce Android and the Zepp OS extension.
 * The phone is a BLE peripheral; the watch is the central. No Zepp private
 * protocol, cloud account, Internet connection or user health data is used.
 */
internal object WildforceWatchBridge {
    const val ACTION_START = "io.codepassion.doubletriangle.watch.START"
    const val ACTION_STOP = "io.codepassion.doubletriangle.watch.STOP"
    const val ACTION_PUBLISH = "io.codepassion.doubletriangle.watch.PUBLISH"

    data class State(
        val workoutId: String,
        val workoutTitle: String,
        val exerciseName: String,
        val exerciseIndex: Int,
        val exerciseCount: Int,
        val setNumber: Int,
        val targetSets: Int,
        val reps: Int,
        val weightKg: Double,
        val restRemaining: Int?,
        val restInitialSeconds: Int,
        val elapsedSeconds: Int,
    ) {
        /**
         * One ATT-sized frame. Zepp's central can deliver a partial value for
         * long reads, so JSON state packets made the watch silently discard
         * otherwise successful reads. Weight is expressed in 0.5 kg units.
         */
        fun toWire(): String = listOf(
            exerciseIndex, exerciseCount, setNumber, targetSets, reps,
            (weightKg * 2).toInt(), restRemaining ?: -1,
        ).joinToString("|")
    }

    data class Command(
        val operation: String,
        val reps: Int? = null,
        val weightKg: Double? = null,
        val seconds: Int? = null,
    )

    @Volatile var state: State? = null
        private set
    @Volatile var commandListener: ((Command) -> Unit)? = null

    fun start(context: Context) {
        if (!hasBluetoothPermissions(context)) return
        context.startService(Intent(context, WildforceWatchBleService::class.java).setAction(ACTION_START))
    }

    fun stop(context: Context) {
        context.stopService(Intent(context, WildforceWatchBleService::class.java).setAction(ACTION_STOP))
        state = null
    }

    fun publish(context: Context, value: State) {
        state = value
        if (hasBluetoothPermissions(context)) {
            context.startService(Intent(context, WildforceWatchBleService::class.java).setAction(ACTION_PUBLISH))
        }
    }

    fun dispatch(command: Command) {
        Handler(Looper.getMainLooper()).post { commandListener?.invoke(command) }
    }

    fun hasBluetoothPermissions(context: Context): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        (context.checkSelfPermission(Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED &&
            context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED)
}

internal class WildforceWatchBleService : Service() {
    private val bluetoothManager by lazy { getSystemService(BluetoothManager::class.java) }
    private var gattServer: BluetoothGattServer? = null
    private var advertiser: android.bluetooth.le.BluetoothLeAdvertiser? = null

    private val stateCharacteristic = BluetoothGattCharacteristic(
        STATE_UUID,
        BluetoothGattCharacteristic.PROPERTY_READ,
        BluetoothGattCharacteristic.PERMISSION_READ,
    )
    private val commandCharacteristic = BluetoothGattCharacteristic(
        COMMAND_UUID,
        BluetoothGattCharacteristic.PROPERTY_WRITE,
        BluetoothGattCharacteristic.PERMISSION_WRITE,
    )

    private val callback = object : BluetoothGattServerCallback() {
        override fun onServiceAdded(status: Int, service: BluetoothGattService) {
            Log.d(LOG_TAG, "service added status=$status uuid=${service.uuid}")
            if (status == BluetoothGatt.GATT_SUCCESS && service.uuid == SERVICE_UUID) startAdvertising()
        }

        override fun onConnectionStateChange(device: android.bluetooth.BluetoothDevice, status: Int, newState: Int) {
            Log.d(LOG_TAG, "connection address=${device.address} status=$status state=$newState")
        }

        override fun onCharacteristicReadRequest(device: android.bluetooth.BluetoothDevice, requestId: Int, offset: Int, characteristic: BluetoothGattCharacteristic) {
            Log.d(LOG_TAG, "read uuid=${characteristic.uuid} offset=$offset")
            if (characteristic.uuid != STATE_UUID) return
            val payload = (WildforceWatchBridge.state?.toWire() ?: "-").toByteArray(StandardCharsets.UTF_8)
            val response = if (offset in 0..payload.size) payload.copyOfRange(offset, payload.size) else byteArrayOf()
            gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, response)
        }

        override fun onCharacteristicWriteRequest(
            device: android.bluetooth.BluetoothDevice,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray,
        ) {
            Log.d(LOG_TAG, "write uuid=${characteristic.uuid} offset=$offset bytes=${value.size}")
            val result = if (characteristic.uuid == COMMAND_UUID && offset == 0 && !preparedWrite) {
                parseCommand(value)?.also(WildforceWatchBridge::dispatch)
                BluetoothGatt.GATT_SUCCESS
            } else BluetoothGatt.GATT_FAILURE
            if (responseNeeded) gattServer?.sendResponse(device, requestId, result, offset, null)
        }
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            Log.d(LOG_TAG, "advertising started")
        }

        override fun onStartFailure(errorCode: Int) {
            Log.e(LOG_TAG, "advertising failed error=$errorCode")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!WildforceWatchBridge.hasBluetoothPermissions(this)) {
            Log.e(LOG_TAG, "Bluetooth permissions missing")
            stopSelf()
            return START_NOT_STICKY
        }
        if (intent?.action == WildforceWatchBridge.ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        startGattServer()
        return START_STICKY
    }

    @Suppress("MissingPermission")
    private fun startGattServer() {
        if (gattServer != null) return
        val adapter: BluetoothAdapter = bluetoothManager.adapter ?: return
        val server = bluetoothManager.openGattServer(this, callback) ?: return
        val added = server.addService(BluetoothGattService(SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY).apply {
            addCharacteristic(stateCharacteristic)
            addCharacteristic(commandCharacteristic)
        })
        gattServer = server
        if (!added) {
            server.close()
            gattServer = null
        }
    }

    @Suppress("MissingPermission")
    private fun startAdvertising() {
        if (advertiser != null) return
        val adapter: BluetoothAdapter = bluetoothManager.adapter ?: return
        advertiser = adapter.bluetoothLeAdvertiser
        advertiser?.startAdvertising(
            AdvertiseSettings.Builder().setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY).setConnectable(true).build(),
            AdvertiseData.Builder().setIncludeDeviceName(false).addServiceUuid(ParcelUuid(SERVICE_UUID)).build(),
            advertiseCallback,
        )
    }

    override fun onDestroy() {
        runCatching { advertiser?.stopAdvertising(advertiseCallback) }
        runCatching { gattServer?.close() }
        gattServer = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun parseCommand(bytes: ByteArray): WildforceWatchBridge.Command? = runCatching {
        val wire = String(bytes, StandardCharsets.UTF_8)
        when {
            wire == "c" -> WildforceWatchBridge.Command(operation = "complete_set")
            wire.startsWith("r|") -> WildforceWatchBridge.Command(operation = "set_reps", reps = wire.substringAfter('|').toInt())
            wire.startsWith("w|") -> WildforceWatchBridge.Command(operation = "set_weight", weightKg = wire.substringAfter('|').toInt() / 2.0)
            else -> null
        }
    }.getOrNull()

    private companion object {
        const val LOG_TAG = "WildforceBle"
        val SERVICE_UUID: UUID = UUID.fromString("25e3a160-6c4d-4d73-9b4c-7b5bfeacf001")
        val STATE_UUID: UUID = UUID.fromString("25e3a161-6c4d-4d73-9b4c-7b5bfeacf001")
        val COMMAND_UUID: UUID = UUID.fromString("25e3a162-6c4d-4d73-9b4c-7b5bfeacf001")
    }
}
