package io.codepassion.doubletriangle.feature.onboarding

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.abs
import kotlin.math.atan2

internal data class TiltState(val leanDegrees: Float = 0f, val isUpright: Boolean = true)

/** Gravity-based counterpart of iOS DeviceTiltService. */
internal class DeviceTiltService(
    context: Context,
    private val onChanged: (TiltState) -> Unit,
) : SensorEventListener {
    private val manager = context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensor = manager.getDefaultSensor(Sensor.TYPE_GRAVITY)
        ?: manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    fun start() {
        sensor?.let { manager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
    }

    fun stop() {
        manager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.values.size < 3) return
        // Same convention as iOS: zero is upright, positive leans away.
        val lean = Math.toDegrees(atan2(event.values[2].toDouble(), -event.values[1].toDouble())).toFloat()
        onChanged(TiltState(lean, abs(lean) <= 5f))
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
