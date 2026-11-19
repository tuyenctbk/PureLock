package com.example

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.test.core.app.ApplicationProvider
import com.example.service.ShakeGestureEvaluator
import com.example.service.ShakeSensitivity
import com.example.service.ShakeToLockSensorController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSensor

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ShakeToLockDetectorTest {

    @Test
    fun stationaryGravity_neverTriggersRegardlessOfOrientation() {
        val evaluator = ShakeGestureEvaluator(ShakeSensitivity.HIGH)

        // Initialize with upright portrait gravity, then smoothly hold or test different static axes
        val orientations = listOf(
            Triple(0f, SensorManager.GRAVITY_EARTH, 0f),
            Triple(0f, SensorManager.GRAVITY_EARTH, 0f),
            Triple(0f, SensorManager.GRAVITY_EARTH, 0f),
            Triple(SensorManager.GRAVITY_EARTH, 0f, 0f),
            Triple(0f, 0f, SensorManager.GRAVITY_EARTH)
        )

        var timestamp = 1000L
        for ((x, y, z) in orientations) {
            repeat(5) {
                assertFalse(evaluator.onAccelerationSample(x, y, z, timestamp))
                timestamp += 200L
            }
        }
    }

    @Test
    fun singleIsolatedBump_doesNotTriggerFalseLockdown() {
        val evaluator = ShakeGestureEvaluator(ShakeSensitivity.MEDIUM)
        // Calibrate baseline gravity
        assertFalse(evaluator.onAccelerationSample(0f, SensorManager.GRAVITY_EARTH, 0f, 1000L))
        // Single strong tap / bump (~3.5G delta)
        assertFalse(evaluator.onAccelerationSample(35f, SensorManager.GRAVITY_EARTH, 0f, 1150L))
        // Return to rest
        assertFalse(evaluator.onAccelerationSample(0f, SensorManager.GRAVITY_EARTH, 0f, 1300L))
        assertFalse(evaluator.onAccelerationSample(0f, SensorManager.GRAVITY_EARTH, 0f, 1600L))
    }

    @Test
    fun deliberateMultiPeakShake_triggersLockdown() {
        val evaluator = ShakeGestureEvaluator(ShakeSensitivity.MEDIUM)
        // Baseline sample
        assertFalse(evaluator.onAccelerationSample(0f, SensorManager.GRAVITY_EARTH, 0f, 1000L))
        // Peak 1 (x = +32 m/s^2 -> ~2.6G filtered linear delta)
        assertFalse(evaluator.onAccelerationSample(32f, SensorManager.GRAVITY_EARTH, 0f, 1150L))
        // Peak 2 in opposite direction within window (>110ms gap, <650ms window)
        assertTrue(evaluator.onAccelerationSample(-34f, SensorManager.GRAVITY_EARTH, 0f, 1320L))
    }

    @Test
    fun cooldown_blocksDuplicateTriggersUntilExpired() {
        val evaluator = ShakeGestureEvaluator(
            initialSensitivity = ShakeSensitivity.MEDIUM,
            cooldownMs = 1500L
        )
        // Baseline
        assertFalse(evaluator.onAccelerationSample(0f, SensorManager.GRAVITY_EARTH, 0f, 1000L))
        // Trigger #1 at 1320ms
        assertFalse(evaluator.onAccelerationSample(32f, SensorManager.GRAVITY_EARTH, 0f, 1150L))
        assertTrue(evaluator.onAccelerationSample(-34f, SensorManager.GRAVITY_EARTH, 0f, 1320L))

        // Immediate shake within cooldown window (< 1500ms after 1320ms) must NOT trigger
        assertFalse(evaluator.onAccelerationSample(35f, SensorManager.GRAVITY_EARTH, 0f, 1600L))
        assertFalse(evaluator.onAccelerationSample(-35f, SensorManager.GRAVITY_EARTH, 0f, 1800L))

        // After cooldown expires (>= 1320 + 1500 = 2820ms), shaking again SHOULD trigger
        assertFalse(evaluator.onAccelerationSample(35f, SensorManager.GRAVITY_EARTH, 0f, 2900L))
        assertTrue(evaluator.onAccelerationSample(-35f, SensorManager.GRAVITY_EARTH, 0f, 3080L))
    }

    @Test
    fun expiredShakeWindow_resetsPartialPeakCount() {
        val evaluator = ShakeGestureEvaluator(
            initialSensitivity = ShakeSensitivity.MEDIUM,
            shakeWindowMs = 650L
        )
        assertFalse(evaluator.onAccelerationSample(0f, SensorManager.GRAVITY_EARTH, 0f, 1000L))
        // First peak at 1150ms
        assertFalse(evaluator.onAccelerationSample(32f, SensorManager.GRAVITY_EARTH, 0f, 1150L))
        // Second peak arrives after 650ms window expired (1900ms - 1150ms = 750ms) -> treated as peak #1 again
        assertFalse(evaluator.onAccelerationSample(-34f, SensorManager.GRAVITY_EARTH, 0f, 1900L))
        // Third peak within new window completes the gesture
        assertTrue(evaluator.onAccelerationSample(34f, SensorManager.GRAVITY_EARTH, 0f, 2060L))
    }

    @Test
    fun sensitivityPresets_adjustThresholdAsExpected() {
        assertEquals(ShakeSensitivity.MEDIUM, ShakeSensitivity.fromKey(null))
        assertEquals(ShakeSensitivity.HIGH, ShakeSensitivity.fromKey("high"))
        assertEquals(ShakeSensitivity.LOW, ShakeSensitivity.fromKey("LOW"))

        // Moderate shake (~2.0G filtered): triggers HIGH (1.7G) but NOT LOW (2.8G)
        val highEvaluator = ShakeGestureEvaluator(ShakeSensitivity.HIGH)
        val lowEvaluator = ShakeGestureEvaluator(ShakeSensitivity.LOW)

        highEvaluator.onAccelerationSample(0f, SensorManager.GRAVITY_EARTH, 0f, 1000L)
        lowEvaluator.onAccelerationSample(0f, SensorManager.GRAVITY_EARTH, 0f, 1000L)

        // With alpha=0.8, residual = 0.8 * x; for 2.0G linear (~19.6 m/s^2), x ~ 24.5 m/s^2
        assertFalse(highEvaluator.onAccelerationSample(24.5f, SensorManager.GRAVITY_EARTH, 0f, 1150L))
        assertTrue(highEvaluator.onAccelerationSample(-24.5f, SensorManager.GRAVITY_EARTH, 0f, 1310L))

        assertFalse(lowEvaluator.onAccelerationSample(24.5f, SensorManager.GRAVITY_EARTH, 0f, 1150L))
        assertFalse(lowEvaluator.onAccelerationSample(-24.5f, SensorManager.GRAVITY_EARTH, 0f, 1310L))
    }

    @Test
    fun sensorController_registersAndUnregistersCleanlyWithLifecycle() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = ShadowSensor.newInstance(Sensor.TYPE_ACCELEROMETER)
        shadowOf(sensorManager).addSensor(accelerometer)

        var triggerCount = 0
        var nowMs = 1000L
        val controller = ShakeToLockSensorController(
            context = context,
            onShakeDetected = { triggerCount++ },
            clockMs = { nowMs }
        )

        assertTrue(ShakeToLockSensorController.isAccelerometerAvailable(context))
        assertTrue(controller.isAccelerometerAvailable)
        assertFalse(controller.isListening)

        assertTrue(controller.start(ShakeSensitivity.MEDIUM))
        assertTrue(controller.isListening)
        assertTrue(shadowOf(sensorManager).hasListener(controller.sensorListener))

        // Idempotent second start
        assertTrue(controller.start(ShakeSensitivity.HIGH))
        assertTrue(controller.isListening)

        controller.stop()
        assertFalse(controller.isListening)
        assertFalse(shadowOf(sensorManager).hasListener(controller.sensorListener))
        assertEquals(0, triggerCount)
    }
}
