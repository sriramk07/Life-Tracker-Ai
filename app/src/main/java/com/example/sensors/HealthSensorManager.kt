package com.example.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.roundToInt

/**
 * ============================================================================
 * HEALTH SENSOR MANAGER — PEDOMETER & VITALITY SCORING ENGINE
 * ============================================================================
 *
 * Bridges Android hardware step sensors ([Sensor.TYPE_STEP_COUNTER]) with
 * wellness analytics:
 * 1. Real-time session step delta calculation from boot-cumulative hardware counter.
 * 2. Pure Kotlin Composite Vitality Score (0-100) blending hydration, sleep,
 *    steps, and active workout minutes.
 * 3. Active metabolic calorie burn estimator based on weight, steps, and cardio.
 */

object VitalityAnalytics {

    /**
     * Estimates active calories burned from daily steps and structured exercise.
     */
    fun estimateCaloriesBurned(
        stepsCount: Int,
        workoutDurationMins: Int,
        weightKg: Float
    ): Int {
        val safeWeight = if (weightKg > 0f) weightKg else 70f
        val safeSteps = stepsCount.coerceAtLeast(0)
        val safeWorkout = workoutDurationMins.coerceAtLeast(0)

        // ~0.04 kcal per step scaled mildly by body weight ratio + 7.5 kcal/min workout
        val stepCalories = safeSteps * 0.04 * (safeWeight / 70.0)
        val workoutCalories = safeWorkout * 7.5 * (safeWeight / 70.0)
        return (stepCalories + workoutCalories).roundToInt()
    }

    /**
     * Calculates a balanced 0..100 Vitality Wellness Index from four weighted pillars (25 pts each):
     * Hydration, Sleep, Physical Steps, and Active Workout Time.
     */
    fun calculateVitalityScore(
        waterMl: Int,
        waterGoalMl: Int,
        sleepHours: Float,
        stepsCount: Int,
        stepGoal: Int,
        workoutMins: Int
    ): Int {
        val waterRatio = (waterMl.toFloat() / waterGoalMl.coerceAtLeast(1)).coerceIn(0f, 1f)
        val sleepRatio = (sleepHours / 8.0f).coerceIn(0f, 1f)
        val stepRatio = (stepsCount.toFloat() / stepGoal.coerceAtLeast(1)).coerceIn(0f, 1f)
        val workoutRatio = (workoutMins.toFloat() / 30f).coerceIn(0f, 1f)

        val total = (waterRatio * 25f) + (sleepRatio * 25f) + (stepRatio * 25f) + (workoutRatio * 25f)
        return total.roundToInt().coerceIn(0, 100)
    }

    /**
     * Classifies Vitality readiness tier for UI badges and coaching feedback.
     */
    fun vitalityGrade(score: Int): String = when {
        score >= 85 -> "Peak Condition (S-Tier)"
        score >= 70 -> "Optimal Balance"
        score >= 50 -> "Moderate Readiness"
        else -> "Recovery Needed"
    }
}

class HealthSensorManager(context: Context) : SensorEventListener {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val stepSensor: Sensor? =
        sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    private val _sessionSteps = MutableStateFlow(0)
    val sessionSteps: StateFlow<Int> = _sessionSteps.asStateFlow()

    private val _isHardwareSensorAvailable = MutableStateFlow(stepSensor != null)
    val isHardwareSensorAvailable: StateFlow<Boolean> = _isHardwareSensorAvailable.asStateFlow()

    private var initialBootSteps: Float? = null

    fun startListening() {
        if (stepSensor == null) {
            Log.i("HealthSensorManager", "Hardware step counter unavailable; manual/simulated tracking ready.")
            return
        }
        try {
            sensorManager?.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI)
        } catch (e: SecurityException) {
            Log.w("HealthSensorManager", "ACTIVITY_RECOGNITION permission not yet granted: ${e.message}")
        }
    }

    fun stopListening() {
        try {
            sensorManager?.unregisterListener(this)
        } catch (_: Exception) {
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
            val totalSinceBoot = event.values.firstOrNull() ?: return
            if (initialBootSteps == null) {
                initialBootSteps = totalSinceBoot
            }
            val delta = (totalSinceBoot - (initialBootSteps ?: totalSinceBoot)).toInt().coerceAtLeast(0)
            _sessionSteps.value = delta
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op
    }
}
