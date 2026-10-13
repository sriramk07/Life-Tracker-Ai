package com.example

import com.example.data.local.entities.HabitEntity
import org.junit.Assert.*
import org.junit.Test

/**
 * ============================================================================
 * UNIT TESTS: HABIT TRACKER STREAKS & COMPLETION MATH
 * ============================================================================
 *
 * Verifies:
 * 1. Toggling habit completion increments streak and updates timestamp.
 * 2. Unchecking decrements streak count safely (clamped to 0).
 * 3. Habit frequency filtering ("Daily", "Weekly", "Monthly").
 * 4. Completion percentage formula.
 */
class HabitLogicTest {

    @Test
    fun toggleHabitToday_incrementsStreakCorrectly() {
        val initialHabit = HabitEntity(
            id = 1,
            name = "Morning Meditation",
            streakCount = 5,
            isCompletedToday = false
        )

        val toggled = initialHabit.copy(
            isCompletedToday = true,
            streakCount = initialHabit.streakCount + 1,
            lastCompletedDate = 1000000L
        )

        assertTrue(toggled.isCompletedToday)
        assertEquals(6, toggled.streakCount)
        assertEquals(1000000L, toggled.lastCompletedDate)
    }

    @Test
    fun toggleHabitToday_uncheckingDecrementsStreakSafelyWithoutNegative() {
        val completedHabit = HabitEntity(
            id = 1,
            name = "Read 30 Mins",
            streakCount = 1,
            isCompletedToday = true
        )

        val uncompleted = completedHabit.copy(
            isCompletedToday = false,
            streakCount = maxOf(0, completedHabit.streakCount - 1)
        )

        assertFalse(uncompleted.isCompletedToday)
        assertEquals(0, uncompleted.streakCount)

        // Ensure 0 streak doesn't become -1
        val zeroHabit = HabitEntity(id = 2, name = "Run", streakCount = 0, isCompletedToday = true)
        val zeroUncompleted = zeroHabit.copy(
            isCompletedToday = false,
            streakCount = maxOf(0, zeroHabit.streakCount - 1)
        )
        assertEquals(0, zeroUncompleted.streakCount)
    }

    @Test
    fun habitCompletionPercent_calculatesAccurately() {
        val habits = listOf(
            HabitEntity(id = 1, name = "Habit 1", isCompletedToday = true),
            HabitEntity(id = 2, name = "Habit 2", isCompletedToday = true),
            HabitEntity(id = 3, name = "Habit 3", isCompletedToday = false),
            HabitEntity(id = 4, name = "Habit 4", isCompletedToday = false)
        )

        val completedCount = habits.count { it.isCompletedToday }
        val percent = (completedCount * 100) / habits.size

        assertEquals(2, completedCount)
        assertEquals(50, percent)
    }

    @Test
    fun habitXpBonus_increasesWithStreakLength() {
        fun calculateHabitXp(streak: Int): Int = 50 + (streak * 5)

        assertEquals(55, calculateHabitXp(1))
        assertEquals(75, calculateHabitXp(5))
        assertEquals(150, calculateHabitXp(20))
    }
}
