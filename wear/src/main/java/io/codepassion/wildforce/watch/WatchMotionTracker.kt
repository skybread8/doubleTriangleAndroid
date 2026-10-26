package io.codepassion.wildforce.watch

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

internal enum class MotionStatus { Disabled, UnsupportedExercise, Ready, Tracking, LowConfidence, ManualFallback }

internal data class RepInsight(
    val number: Int,
    val durationSeconds: Double,
    val rangeOfMotion: Double,
    val velocity: String,
)

internal data class MotionSnapshot(
    val status: MotionStatus = MotionStatus.Disabled,
    val autoDetectedRepCount: Int = 0,
    val confidence: Double? = null,
    val reps: List<RepInsight> = emptyList(),
) {
    val statusTitle: String
        get() = when (status) {
            MotionStatus.Disabled -> "Manual reps"
            MotionStatus.UnsupportedExercise -> "Manual reps"
            MotionStatus.Ready -> "Auto reps ready"
            MotionStatus.Tracking -> "Auto reps"
            MotionStatus.LowConfidence -> "Low confidence"
            MotionStatus.ManualFallback -> "Manual fallback"
        }

    val statusSubtitle: String
        get() = when (status) {
            MotionStatus.Disabled -> "Watch auto-tracking is disabled"
            MotionStatus.UnsupportedExercise -> "This exercise is manual on watch"
            MotionStatus.Ready -> "Ready to detect reps on this set"
            MotionStatus.Tracking -> "$autoDetectedRepCount auto-detected"
            MotionStatus.LowConfidence -> "Validate reps manually"
            MotionStatus.ManualFallback -> "Motion unavailable. Using manual reps"
        }
}

private data class MotionProfile(
    val accelerationAxis: Int,
    val rotationAxis: Int,
    val minimumAmplitude: Double,
    val referenceAmplitude: Double,
    val minimumRepDuration: Double,
    val maximumRepDuration: Double,
    val explosiveDurationThreshold: Double,
    val slowDurationThreshold: Double,
    val accelerationWeight: Double = 0.65,
    val rotationWeight: Double = 0.35,
)

/**
 * Android counterpart to the watchOS motion tracker. It intentionally keeps
 * the detector self-contained so it can run when Health Services is absent.
 */
internal class WatchMotionTracker(
    context: Context,
    private val onSnapshot: (MotionSnapshot) -> Unit,
) : SensorEventListener {
    private val sensors = context.getSystemService(SensorManager::class.java)
    private val acceleration = sensors?.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        ?: sensors?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val rotation = sensors?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private var profile: MotionProfile? = null
    private var enabled = false
    private var smoothed: Double? = null
    private var lastValue: Double? = null
    private var lastDerivative = 0.0
    private var extremes = mutableListOf<Extreme>()
    private var reps = mutableListOf<RepInsight>()
    private var confidences = mutableListOf<Double>()
    private var latestAcceleration = doubleArrayOf(0.0, 0.0, 0.0)
    private var latestRotation = doubleArrayOf(0.0, 0.0, 0.0)
    private var lastTimestampSeconds = 0.0
    private var snapshot = MotionSnapshot()

    private data class Extreme(val value: Double, val time: Double, val peak: Boolean)

    fun updateExercise(name: String, active: Boolean) {
        if (!active) {
            stop(reset = true)
            return
        }
        val nextProfile = profileFor(name)
        if (nextProfile == profile && enabled) return
        stop(reset = false)
        profile = nextProfile
        if (nextProfile == null) {
            snapshot = MotionSnapshot(MotionStatus.UnsupportedExercise)
            onSnapshot(snapshot)
            return
        }
        if (acceleration == null) {
            snapshot = MotionSnapshot(MotionStatus.ManualFallback)
            onSnapshot(snapshot)
            return
        }
        enabled = true
        snapshot = MotionSnapshot(MotionStatus.Ready)
        onSnapshot(snapshot)
        sensors?.registerListener(this, acceleration, SensorManager.SENSOR_DELAY_GAME)
        rotation?.let { sensors?.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
    }

    fun finishSet(): Int {
        val detectedCount = reps.size
        snapshot = snapshot.copy(
            status = if (profile == null) MotionStatus.UnsupportedExercise else MotionStatus.Ready,
            autoDetectedRepCount = 0,
            confidence = confidences.averageOrNull(),
            reps = reps.toList(),
        )
        onSnapshot(snapshot)
        resetDetector()
        return detectedCount
    }

    fun stop(reset: Boolean) {
        if (enabled) sensors?.unregisterListener(this)
        enabled = false
        resetDetector()
        if (reset) {
            profile = null
            snapshot = MotionSnapshot()
            onSnapshot(snapshot)
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_LINEAR_ACCELERATION, Sensor.TYPE_ACCELEROMETER -> latestAcceleration = doubleArrayOf(event.values[0].toDouble(), event.values[1].toDouble(), event.values[2].toDouble())
            Sensor.TYPE_GYROSCOPE -> latestRotation = doubleArrayOf(event.values[0].toDouble(), event.values[1].toDouble(), event.values[2].toDouble())
        }
        if (event.sensor.type != Sensor.TYPE_LINEAR_ACCELERATION &&
            event.sensor.type != Sensor.TYPE_ACCELEROMETER
        ) return
        val activeProfile = profile ?: return
        val time = event.timestamp / 1_000_000_000.0
        val accelerationValue = abs(latestAcceleration[activeProfile.accelerationAxis])
        val rotationValue = abs(latestRotation[activeProfile.rotationAxis])
        val composite = accelerationValue * activeProfile.accelerationWeight + rotationValue * activeProfile.rotationWeight
        ingest(composite, time, activeProfile)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun ingest(raw: Double, time: Double, activeProfile: MotionProfile) {
        val filtered = (smoothed?.let { 0.25 * raw + 0.75 * it } ?: raw).also { smoothed = it }
        val previous = lastValue ?: run { lastValue = filtered; lastTimestampSeconds = time; return }
        val derivative = filtered - previous
        lastValue = filtered
        lastTimestampSeconds = time
        if (abs(derivative) < 0.025) return
        if (lastDerivative != 0.0 && (derivative > 0) != (lastDerivative > 0)) {
            val peak = lastDerivative > 0
            extremes += Extreme(previous, time, peak)
            acceptCandidate(activeProfile)
        }
        lastDerivative = derivative
    }

    private fun acceptCandidate(activeProfile: MotionProfile) {
        if (extremes.size < 3) return
        val recent = extremes.takeLast(3)
        if (recent[0].peak == recent[1].peak || recent[1].peak == recent[2].peak) return
        val amplitudeOne = abs(recent[0].value - recent[1].value)
        val amplitudeTwo = abs(recent[1].value - recent[2].value)
        val duration = recent[2].time - recent[0].time
        if (min(amplitudeOne, amplitudeTwo) < activeProfile.minimumAmplitude ||
            duration < activeProfile.minimumRepDuration ||
            duration > activeProfile.maximumRepDuration
        ) {
            if (extremes.size > 4) extremes = extremes.takeLast(4).toMutableList()
            return
        }
        val amplitudeScore = min(min(amplitudeOne, amplitudeTwo) / activeProfile.referenceAmplitude, 1.2)
        val balanceScore = 1 - min(abs(amplitudeOne - amplitudeTwo) / max(amplitudeOne, amplitudeTwo), 1.0)
        val midpoint = (activeProfile.minimumRepDuration + activeProfile.maximumRepDuration) / 2
        val durationScore = 1 - min(abs(duration - midpoint) / midpoint, 1.0)
        val confidence = (amplitudeScore * .45 + balanceScore * .35 + durationScore * .20).coerceIn(.15, 1.0)
        confidences += confidence
        val velocity = when {
            duration <= activeProfile.explosiveDurationThreshold -> "Explosive"
            duration >= activeProfile.slowDurationThreshold -> "Slow"
            else -> "Controlled"
        }
        reps += RepInsight(reps.size + 1, duration, min(min(amplitudeOne, amplitudeTwo) / activeProfile.referenceAmplitude, 1.25), velocity)
        snapshot = snapshot.copy(
            status = if (confidence < .55) MotionStatus.LowConfidence else MotionStatus.Tracking,
            autoDetectedRepCount = reps.size,
            confidence = confidences.averageOrNull(),
            reps = reps.toList(),
        )
        onSnapshot(snapshot)
        extremes = mutableListOf(recent[2])
    }

    private fun resetDetector() {
        smoothed = null
        lastValue = null
        lastDerivative = 0.0
        extremes.clear()
        reps.clear()
        confidences.clear()
    }

    private fun profileFor(name: String): MotionProfile? {
        val value = name.lowercase()
        fun p(a: Int, r: Int, minAmp: Double, ref: Double, minTime: Double, maxTime: Double, explosive: Double, slow: Double, aw: Double = .65, rw: Double = .35) =
            MotionProfile(a, r, minAmp, ref, minTime, maxTime, explosive, slow, aw, rw)
        return when {
            "curl" in value -> p(1, 0, 1.05, 1.5, .7, 5.0, 1.0, 2.2)
            "lateral raise" in value || "front raise" in value || "elevación lateral" in value || "elevación frontal" in value -> p(0, 2, .9, 1.25, .8, 4.5, 1.0, 2.1)
            "overhead press" in value || "arnold" in value || "press militar" in value -> p(1, 0, .95, 1.35, .8, 4.5, .95, 2.0)
            "pushdown" in value || "triceps" in value || "tríceps" in value -> p(1, 2, .95, 1.4, .75, 4.8, 1.0, 2.15)
            "bench press" in value || "incline bench" in value || "press banca" in value -> p(0, 1, .08, .16, .48, 5.0, 1.1, 2.2, .75, .25)
            "chest fly" in value || "cable fly" in value || "apertura" in value -> p(0, 1, .8, 1.2, 1.0, 5.0, 1.2, 2.5)
            "squat" in value || "sentadilla" in value -> p(1, 0, 1.0, 1.4, 1.2, 6.0, 1.5, 3.0)
            else -> null
        }
    }

    private fun List<Double>.averageOrNull(): Double? = takeIf { isNotEmpty() }?.average()
}
