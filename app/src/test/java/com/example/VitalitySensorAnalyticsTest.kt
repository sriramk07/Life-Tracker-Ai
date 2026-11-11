package com.example

import com.example.sensors.VitalityAnalytics
import org.junit.Assert.*
import org.junit.Test

/**
 * ============================================================================
 * UNIT TESTS: VITALITY ANALYTICS & PEDOMETER CALORIE ESTIMATION
 * ============================================================================
 */
class VitalitySensorAnalyticsTest {

    @Test
    fun estimateCaloriesBurned_calculatesStepsPlusWorkoutSafely() {
        val burned = VitalityAnalytics.estimateCaloriesBurned(
            stepsCount = 5000,
            workoutDurationMins = 20,
            weightKg = 70f
        )
        // 5000 * 0.04 = 200 + 20 * 7.5 = 150 -> 350 kcal
        assertEquals(350, burned)

        val safeZero = VitalityAnalytics.estimateCaloriesBurned(-100, -10, 0f)
        assertEquals(0, safeZero)
    }

    @Test
    fun calculateVitalityScore_blendsFourHealthPillarsAccurately() {
        val perfect = VitalityAnalytics.calculateVitalityScore(
            waterMl = 2500,
            waterGoalMl = 2500,
            sleepHours = 8.0f,
            stepsCount = 10000,
            stepGoal = 10000,
            workoutMins = 30
        )
        assertEquals(100, perfect)
        assertEquals("Peak Condition (S-Tier)", VitalityAnalytics.vitalityGrade(perfect))

        val halfway = VitalityAnalytics.calculateVitalityScore(
            waterMl = 1250,
            waterGoalMl = 2500,
            sleepHours = 4.0f,
            stepsCount = 5000,
            stepGoal = 10000,
            workoutMins = 15
        )
        assertEquals(50, halfway)
        assertEquals("Moderate Readiness", VitalityAnalytics.vitalityGrade(halfway))
    }
}
