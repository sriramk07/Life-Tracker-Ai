package com.example

import com.example.domain.model.calculateBmi
import org.junit.Assert.*
import org.junit.Test

/**
 * ============================================================================
 * UNIT TESTS: BMI CALCULATOR & HEALTH METRICS ENGINE
 * ============================================================================
 *
 * Verifies mathematical accuracy and classification boundaries:
 * 1. Underweight (< 18.5)
 * 2. Normal Weight (18.5 .. 24.9)
 * 3. Overweight (25.0 .. 29.9)
 * 4. Obese (>= 30.0)
 * 5. Edge cases: Zero or negative height/weight.
 */
class BmiCalculatorTest {

    @Test
    fun calculateBmi_underweightClassification() {
        // Height 180cm, Weight 55kg -> BMI ~ 16.98
        val result = calculateBmi(weightKg = 55f, heightCm = 180f)
        assertEquals("Underweight", result.category)
        assertTrue(result.bmiValue < 18.5f)
        assertEquals(0xFF2196F3, result.colorHex)
    }

    @Test
    fun calculateBmi_normalWeightClassification() {
        // Height 175cm, Weight 68kg -> BMI = 68 / (1.75^2) = 22.20
        val result = calculateBmi(weightKg = 68f, heightCm = 175f)
        assertEquals("Normal Weight", result.category)
        assertTrue(result.bmiValue in 18.5f..24.9f)
        assertEquals(0xFF4CAF50, result.colorHex)
    }

    @Test
    fun calculateBmi_overweightClassification() {
        // Height 170cm, Weight 78kg -> BMI = 78 / (1.70^2) = 26.99
        val result = calculateBmi(weightKg = 78f, heightCm = 170f)
        assertEquals("Overweight", result.category)
        assertTrue(result.bmiValue in 25.0f..29.9f)
        assertEquals(0xFFFF9800, result.colorHex)
    }

    @Test
    fun calculateBmi_obeseClassification() {
        // Height 170cm, Weight 95kg -> BMI = 95 / (1.70^2) = 32.87
        val result = calculateBmi(weightKg = 95f, heightCm = 170f)
        assertEquals("Obese", result.category)
        assertTrue(result.bmiValue >= 30.0f)
        assertEquals(0xFFF44336, result.colorHex)
    }

    @Test
    fun calculateBmi_zeroOrNegativeInputs_returnsUnknownSafeResult() {
        val resultZeroHeight = calculateBmi(weightKg = 70f, heightCm = 0f)
        assertEquals("Unknown", resultZeroHeight.category)
        assertEquals(0f, resultZeroHeight.bmiValue, 0.001f)

        val resultZeroWeight = calculateBmi(weightKg = 0f, heightCm = 175f)
        assertEquals("Unknown", resultZeroWeight.category)

        val resultNegative = calculateBmi(weightKg = -50f, heightCm = -170f)
        assertEquals("Unknown", resultNegative.category)
    }
}
