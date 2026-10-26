package io.codepassion.wildforce.watch

import android.content.Context
import androidx.health.services.client.HealthServices
import androidx.health.services.client.ExerciseClient
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.ExerciseConfig
import androidx.health.services.client.data.ExerciseType
import androidx.health.services.client.ExerciseUpdateCallback
import androidx.health.services.client.data.ExerciseUpdate
import androidx.health.services.client.data.ExerciseLapSummary
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.DataTypeAvailability
import java.util.concurrent.Executor
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

internal object WatchHealthState {
    var heartRate by mutableStateOf(0.0)
}

/**
 * Android counterpart to the watchOS HKWorkoutSession.
 *
 * Health Services is optional at runtime: a Wear emulator or device without
 * the provider leaves the BLE workout usable, but a supported device gets a
 * native strength exercise session and heart-rate updates.
 */
internal class WatchHealthServicesManager(
    context: Context,
    private val onHeartRate: (Double) -> Unit = { WatchHealthState.heartRate = it },
    private val onStatus: (String) -> Unit = {},
) {
    private val exerciseClient: ExerciseClient = HealthServices.getClient(context).exerciseClient
    private val executor: Executor = Executor { it.run() }
    private var started = false

    private val callback = object : ExerciseUpdateCallback {
        override fun onRegistered() = Unit

        override fun onRegistrationFailed(throwable: Throwable) {
            onStatus("Health Services no disponible")
        }

        override fun onExerciseUpdateReceived(update: ExerciseUpdate) {
            update.latestMetrics.getData(DataType.HEART_RATE_BPM).lastOrNull()?.value?.let(onHeartRate)
            if (update.exerciseStateInfo.state.isEnded) {
                started = false
                onStatus("Health Services ha terminado la sesión")
            }
        }

        override fun onLapSummaryReceived(lapSummary: ExerciseLapSummary) = Unit

        override fun onAvailabilityChanged(dataType: DataType<*, *>, availability: Availability) {
            if (dataType == DataType.HEART_RATE_BPM && availability is DataTypeAvailability) {
                onStatus(if (availability == DataTypeAvailability.AVAILABLE) "Frecuencia cardiaca disponible" else "Frecuencia cardiaca no disponible")
            }
        }
    }

    fun start() {
        if (started) return
        exerciseClient.setUpdateCallback(callback)
        val config = ExerciseConfig(
            exerciseType = ExerciseType.STRENGTH_TRAINING,
            dataTypes = setOf(DataType.HEART_RATE_BPM),
            isAutoPauseAndResumeEnabled = false,
            isGpsEnabled = false,
        )
        exerciseClient.startExerciseAsync(config).addListener({
            started = true
            onStatus("Sesión Health Services activa")
        }, executor)
    }

    fun stop() {
        if (!started) return
        exerciseClient.endExerciseAsync().addListener({
            started = false
            WatchHealthState.heartRate = 0.0
            exerciseClient.clearUpdateCallbackAsync(callback)
        }, executor)
    }
}
