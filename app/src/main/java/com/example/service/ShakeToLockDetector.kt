package com.example.service

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import kotlin.math.sqrt

/**
 * Sensitivity presets for PureLock's accelerometer-based Shake-to-Lock gesture.
 * Thresholds represent gravity-filtered linear acceleration in G units.
 */
enum class ShakeSensitivity(
    val key: String,
    val gThreshold: Float,
    val minShakeCount: Int,
    val badgeLabel: String
) {
    HIGH(key = "HIGH", gThreshold = 1.7f, minShakeCount = 2, badgeLabel = "1.7G"),
    MEDIUM(key = "MEDIUM", gThreshold = 2.2f, minShakeCount = 2, badgeLabel = "2.2G"),
    LOW(key = "LOW", gThreshold = 2.8f, minShakeCount = 2, badgeLabel = "2.8G");

    companion object {
        fun fromKey(key: String?): ShakeSensitivity {
            return entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: MEDIUM
        }
    }
}

/**
 * Pure, deterministic accelerometer signal processor for detecting deliberate shake gestures.
 *
 * Uses a low-pass gravity filter to isolate linear acceleration from device tilt/orientation,
 * requires multiple distinct acceleration peaks within a rolling window to eliminate accidental
 * single-bump triggers, and enforces a post-lockdown cooldown period.
 */
class ShakeGestureEvaluator(
    initialSensitivity: ShakeSensitivity = ShakeSensitivity.MEDIUM,
    private val gravityFilterAlpha: Float = DEFAULT_GRAVITY_ALPHA,
    private val minPeakGapMs: Long = DEFAULT_MIN_PEAK_GAP_MS,
    private val shakeWindowMs: Long = DEFAULT_SHAKE_WINDOW_MS,
    private val cooldownMs: Long = DEFAULT_COOLDOWN_MS
) {
    companion object {
        const val DEFAULT_GRAVITY_ALPHA = 0.8f
        const val DEFAULT_MIN_PEAK_GAP_MS = 110L
        const val DEFAULT_SHAKE_WINDOW_MS = 650L
        const val DEFAULT_COOLDOWN_MS = 1500L
    }

    @Volatile
    var sensitivity: ShakeSensitivity = initialSensitivity

    private var gravityInitialized = false
    private var gravityX = 0f
    private var gravityY = 0f
    private var gravityZ = 0f

    private var peakCount = 0
    private var firstPeakTimeMs = 0L
    private var lastPeakTimeMs = 0L
    private var lastTriggerTimeMs = 0L

    /**
     * Evaluates an accelerometer sample (in m/s^2) at [timestampMs].
     * Returns `true` if a valid shake gesture has completed and lockdown should trigger.
     */
    @Synchronized
    fun onAccelerationSample(x: Float, y: Float, z: Float, timestampMs: Long): Boolean {
        if (!gravityInitialized) {
            gravityX = x
            gravityY = y
            gravityZ = z
            gravityInitialized = true
            return false
        }

        // Low-pass filter isolates gravity vector; high-pass residual yields linear acceleration
        val alpha = gravityFilterAlpha
        gravityX = alpha * gravityX + (1f - alpha) * x
        gravityY = alpha * gravityY + (1f - alpha) * y
        gravityZ = alpha * gravityZ + (1f - alpha) * z

        val linearX = x - gravityX
        val linearY = y - gravityY
        val linearZ = z - gravityZ

        val linearAccelerationMs2 = sqrt(
            (linearX * linearX + linearY * linearY + linearZ * linearZ).toDouble()
        ).toFloat()
        val linearG = linearAccelerationMs2 / SensorManager.GRAVITY_EARTH

        // Expire stale partial shake window
        if (peakCount > 0 && (timestampMs - firstPeakTimeMs) > shakeWindowMs) {
            peakCount = 0
            firstPeakTimeMs = 0L
            lastPeakTimeMs = 0L
        }

        val currentSensitivity = sensitivity
        if (linearG < currentSensitivity.gThreshold) {
            return false
        }

        // Ignore high-frequency samples belonging to the same single motion impulse
        if (lastPeakTimeMs > 0L && (timestampMs - lastPeakTimeMs) < minPeakGapMs) {
            return false
        }

        if (peakCount == 0) {
            firstPeakTimeMs = timestampMs
        }
        lastPeakTimeMs = timestampMs
        peakCount++

        if (peakCount >= currentSensitivity.minShakeCount) {
            val cooldownElapsed = lastTriggerTimeMs == 0L || (timestampMs - lastTriggerTimeMs) >= cooldownMs
            peakCount = 0
            firstPeakTimeMs = 0L
            lastPeakTimeMs = 0L
            if (cooldownElapsed) {
                lastTriggerTimeMs = timestampMs
                return true
            }
        }
        return false
    }

    /**
     * Clears filter and peak state so resuming detection starts cleanly with fresh gravity orientation.
     */
    @Synchronized
    fun reset() {
        gravityInitialized = false
        gravityX = 0f
        gravityY = 0f
        gravityZ = 0f
        peakCount = 0
        firstPeakTimeMs = 0L
        lastPeakTimeMs = 0L
    }
}

/**
 * Lifecycle-safe wrapper around Android's hardware accelerometer (`Sensor.TYPE_ACCELEROMETER`)
 * that triggers [onShakeDetected] whenever [ShakeGestureEvaluator] confirms a deliberate shake.
 */
class ShakeToLockSensorController(
    context: Context,
    private val onShakeDetected: () -> Unit,
    private val evaluator: ShakeGestureEvaluator = ShakeGestureEvaluator(),
    private val clockMs: () -> Long = { System.currentTimeMillis() }
) {
    private val appContext = context.applicationContext
    private val sensorManager: SensorManager? =
        appContext.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer: Sensor? =
        sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    @Volatile
    var isListening: Boolean = false
        private set

    val isAccelerometerAvailable: Boolean
        get() = accelerometer != null

    internal val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
            if (event.values.size < 3) return
            val triggered = evaluator.onAccelerationSample(
                x = event.values[0],
                y = event.values[1],
                z = event.values[2],
                timestampMs = clockMs()
            )
            if (triggered) {
                onShakeDetected()
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            // No action required
        }
    }

    @Synchronized
    fun start(sensitivity: ShakeSensitivity = evaluator.sensitivity): Boolean {
        evaluator.sensitivity = sensitivity
        if (isListening) return true
        val manager = sensorManager ?: return false
        val sensor = accelerometer ?: return false

        evaluator.reset()
        val registered = try {
            manager.registerListener(sensorListener, sensor, SensorManager.SENSOR_DELAY_GAME)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register accelerometer listener", e)
            false
        }
        isListening = registered
        return registered
    }

    fun updateSensitivity(sensitivity: ShakeSensitivity) {
        evaluator.sensitivity = sensitivity
    }

    @Synchronized
    fun stop() {
        if (!isListening) return
        try {
            sensorManager?.unregisterListener(sensorListener)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unregister accelerometer listener", e)
        } finally {
            isListening = false
            evaluator.reset()
        }
    }

    companion object {
        private const val TAG = "ShakeToLockController"

        fun isAccelerometerAvailable(context: Context): Boolean {
            val sm = context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            return sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null
        }

        /**
         * Emits a crisp double-pulse haptic confirmation when Shake-to-Lock engages.
         */
        @Suppress("DEPRECATION")
        fun triggerLockHapticFeedback(context: Context) {
            try {
                val appCtx = context.applicationContext
                val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vm = appCtx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    vm?.defaultVibrator
                } else {
                    appCtx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                }

                if (vibrator == null || !vibrator.hasVibrator()) return

                val timings = longArrayOf(0L, 45L, 55L, 70L)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val amplitudes = intArrayOf(0, 180, 0, 255)
                    vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                } else {
                    vibrator.vibrate(timings, -1)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Haptic feedback unavailable for Shake-to-Lock", e)
            }
        }
    }
}
