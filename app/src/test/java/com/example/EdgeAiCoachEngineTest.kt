package com.example

import com.example.data.local.entities.*
import com.example.domain.ai.AiCoachingIntent
import com.example.domain.ai.EdgeAiCoachEngine
import com.example.domain.ai.PredictiveLifeSnapshot
import org.junit.Assert.*
import org.junit.Test

/**
 * ============================================================================
 * UNIT TESTS: ON-DEVICE EDGE AI COACH & PREDICTIVE ANALYTICS ENGINE
 * ============================================================================
 */
class EdgeAiCoachEngineTest {

    @Test
    fun classifyIntent_routesDomainPromptsAccurately() {
        assertEquals(AiCoachingIntent.FINANCE, EdgeAiCoachEngine.classifyIntent("Analyze my monthly budget spending"))
        assertEquals(AiCoachingIntent.FITNESS, EdgeAiCoachEngine.classifyIntent("How much water and steps do I need?"))
        assertEquals(AiCoachingIntent.HABITS, EdgeAiCoachEngine.classifyIntent("Optimize my morning habit streak"))
        assertEquals(AiCoachingIntent.TASKS, EdgeAiCoachEngine.classifyIntent("Which high priority quest should I focus on?"))
        assertEquals(AiCoachingIntent.JOURNAL, EdgeAiCoachEngine.classifyIntent("Check mood in my journal reflection"))
        assertEquals(AiCoachingIntent.GENERAL, EdgeAiCoachEngine.classifyIntent("Give me a holistic overview"))
    }

    @Test
    fun forecastBudget_projectsLinearOverspendCorrectly() {
        val expenses = listOf(
            ExpenseEntity(id = 1, title = "Rent", amount = 1200.0, type = "Expense"),
            ExpenseEntity(id = 2, title = "Dining", amount = 300.0, type = "Expense"),
            ExpenseEntity(id = 3, title = "Salary", amount = 5000.0, type = "Income")
        )

        // Day 15 of 30: $1500 spent -> $3000 projected -> Exceeds $2500 budget
        val atRisk = EdgeAiCoachEngine.forecastBudget(expenses, monthlyBudget = 2500.0, dayOfMonth = 15, daysInMonth = 30)
        assertEquals(1500.0, atRisk.totalSpent, 0.01)
        assertEquals(3000.0, atRisk.projectedEndOfMonthSpend, 0.01)
        assertTrue(atRisk.isAtRiskOfOverspend)

        // With $4000 budget -> Safe trajectory
        val safe = EdgeAiCoachEngine.forecastBudget(expenses, monthlyBudget = 4000.0, dayOfMonth = 15, daysInMonth = 30)
        assertFalse(safe.isAtRiskOfOverspend)
    }

    @Test
    fun analyzeJournalSentimentScore_distinguishesPositiveAndStressedEntries() {
        val calmEntries = listOf(
            JournalEntity(id = 1, title = "Strong Day", content = "Felt calm, focused, grateful and productive", moodTag = "Happy")
        )
        val stressedEntries = listOf(
            JournalEntity(id = 2, title = "Tough Week", content = "Felt anxious, exhausted, overwhelmed and stressed", moodTag = "Burnout")
        )

        val highScore = EdgeAiCoachEngine.analyzeJournalSentimentScore(calmEntries)
        val lowScore = EdgeAiCoachEngine.analyzeJournalSentimentScore(stressedEntries)

        assertTrue("Calm journals should score >= 85", highScore >= 85)
        assertTrue("Stressed journals should score <= 45", lowScore <= 45)
    }

    @Test
    fun generateEdgeCoaching_incorporatesLiveSnapshotMetrics() {
        val snapshot = PredictiveLifeSnapshot(
            tasks = listOf(TaskEntity(id = 1, title = "Deploy Production Build", priority = "High", isCompleted = false)),
            habits = listOf(HabitEntity(id = 1, name = "Read Papers", streakCount = 12, isCompletedToday = false)),
            expenses = listOf(ExpenseEntity(id = 1, title = "Cloud GPU", amount = 450.0, type = "Expense")),
            healthLog = HealthLogEntity(waterIntakeMl = 1500, stepsCount = 6500),
            monthlyBudget = 2000.0
        )

        val taskCoach = EdgeAiCoachEngine.generateEdgeCoaching("What task priority next?", snapshot)
        assertTrue(taskCoach.contains("Deploy Production Build"))

        val habitCoach = EdgeAiCoachEngine.generateEdgeCoaching("Check my streak", snapshot)
        assertTrue(habitCoach.contains("Read Papers"))
        assertTrue(habitCoach.contains("12-day streak"))
    }
}
