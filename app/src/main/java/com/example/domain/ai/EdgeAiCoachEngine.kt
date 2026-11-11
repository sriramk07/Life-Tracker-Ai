package com.example.domain.ai

import com.example.data.local.entities.*
import kotlin.math.roundToInt

/**
 * ============================================================================
 * EDGE AI COACH ENGINE — ON-DEVICE PREDICTIVE ANALYTICS & OFFLINE INFERENCE
 * ============================================================================
 *
 * Executes zero-latency, privacy-preserving on-device heuristics without network
 * dependency:
 * 1. Linear Cashflow & Budget Burn-Rate Forecasting.
 * 2. Priority Task Triage (High/Medium/Low urgency matrix).
 * 3. Habit Streak Protection & Adherence Scoring.
 * 4. Lexicon Sentiment Analysis on recent journal entries.
 * 5. Intent Classification synthesizing personalized coaching insights.
 */

enum class AiCoachingIntent {
    FINANCE, FITNESS, HABITS, TASKS, JOURNAL, GENERAL
}

data class PredictiveLifeSnapshot(
    val tasks: List<TaskEntity> = emptyList(),
    val habits: List<HabitEntity> = emptyList(),
    val expenses: List<ExpenseEntity> = emptyList(),
    val healthLog: HealthLogEntity? = null,
    val journals: List<JournalEntity> = emptyList(),
    val monthlyBudget: Double = 3000.0
)

data class BudgetForecastResult(
    val totalSpent: Double,
    val monthlyBudget: Double,
    val utilizationPercent: Int,
    val projectedEndOfMonthSpend: Double,
    val isAtRiskOfOverspend: Boolean
)

object EdgeAiCoachEngine {

    private val positiveLexicon = setOf(
        "happy", "grateful", "calm", "energetic", "focused", "proud", "progress",
        "accomplished", "relaxed", "strong", "confident", "clear", "productive"
    )

    private val stressLexicon = setOf(
        "tired", "anxious", "stressed", "overwhelmed", "exhausted", "worried",
        "burnout", "stuck", "frustrated", "sad", "delayed", "skipped"
    )

    /**
     * Classifies user query intent using deterministic weighted keyword routing.
     */
    fun classifyIntent(query: String): AiCoachingIntent {
        val q = query.lowercase()
        return when {
            q.containsAny("spend", "budget", "expense", "money", "finance", "cash", "income", "saving") -> AiCoachingIntent.FINANCE
            q.containsAny("workout", "bmi", "health", "water", "sleep", "steps", "fitness", "cardio") -> AiCoachingIntent.FITNESS
            q.containsAny("habit", "streak", "routine", "duty", "consistency", "daily") -> AiCoachingIntent.HABITS
            q.containsAny("task", "quest", "todo", "priority", "focus", "pomodoro", "work") -> AiCoachingIntent.TASKS
            q.containsAny("journal", "mood", "reflect", "mental", "feeling", "stress", "diary") -> AiCoachingIntent.JOURNAL
            else -> AiCoachingIntent.GENERAL
        }
    }

    /**
     * Projects end-of-month expenditure assuming linear burn rate across active day of month.
     */
    fun forecastBudget(
        expenses: List<ExpenseEntity>,
        monthlyBudget: Double,
        dayOfMonth: Int = 15,
        daysInMonth: Int = 30
    ): BudgetForecastResult {
        val safeDay = dayOfMonth.coerceIn(1, daysInMonth)
        val totalSpent = expenses.filter { it.type.equals("Expense", ignoreCase = true) }.sumOf { it.amount }
        val dailyRate = totalSpent / safeDay.toDouble()
        val projected = dailyRate * daysInMonth.toDouble()
        val utilization = if (monthlyBudget > 0.0) ((totalSpent / monthlyBudget) * 100.0).roundToInt() else 0

        return BudgetForecastResult(
            totalSpent = totalSpent,
            monthlyBudget = monthlyBudget,
            utilizationPercent = utilization,
            projectedEndOfMonthSpend = projected,
            isAtRiskOfOverspend = projected > monthlyBudget || totalSpent > monthlyBudget
        )
    }

    /**
     * Computes a 0..100 Mental Sentiment Index across journal entries using keyword polarity.
     */
    fun analyzeJournalSentimentScore(journals: List<JournalEntity>): Int {
        if (journals.isEmpty()) return 75 // neutral-positive baseline
        var posHits = 0
        var negHits = 0

        journals.forEach { entry ->
            val tokens = "${entry.title} ${entry.content} ${entry.moodTag}"
                .lowercase()
                .split(Regex("[^a-z]+"))
            tokens.forEach { token ->
                if (token in positiveLexicon) posHits++
                if (token in stressLexicon) negHits++
            }
        }

        val totalSignals = posHits + negHits
        if (totalSignals == 0) return 75
        val ratio = posHits.toDouble() / totalSignals.toDouble()
        return (30.0 + ratio * 70.0).roundToInt().coerceIn(10, 100)
    }

    /**
     * Synthesizes a metric-backed on-device response using real user data in [snapshot].
     */
    fun generateEdgeCoaching(prompt: String, snapshot: PredictiveLifeSnapshot): String {
        val intent = classifyIntent(prompt)
        val pendingTasks = snapshot.tasks.filter { !it.isCompleted }
        val highPriTasks = pendingTasks.filter { it.priority.equals("High", ignoreCase = true) }
        val uncompletedHabits = snapshot.habits.filter { !it.isCompletedToday }
        val budgetForecast = forecastBudget(snapshot.expenses, snapshot.monthlyBudget)
        val mentalScore = analyzeJournalSentimentScore(snapshot.journals)
        val waterMl = snapshot.healthLog?.waterIntakeMl ?: 0
        val steps = snapshot.healthLog?.stepsCount ?: 0

        return when (intent) {
            AiCoachingIntent.FINANCE -> buildString {
                appendLine("Edge AI Financial Forecast")
                appendLine("• Spent to date: $${String.format("%.2f", budgetForecast.totalSpent)} (${budgetForecast.utilizationPercent}% of $${String.format("%.0f", budgetForecast.monthlyBudget)} budget)")
                appendLine("• Projected month-end spend: $${String.format("%.2f", budgetForecast.projectedEndOfMonthSpend)}")
                if (budgetForecast.isAtRiskOfOverspend) {
                    appendLine("• Alert: Trajectory exceeds your monthly ceiling. Trim discretionary dining/shopping transactions this week.")
                } else {
                    appendLine("• Status: Healthy surplus trajectory. Allocate extra savings toward goals.")
                }
            }

            AiCoachingIntent.FITNESS -> buildString {
                appendLine("Edge AI Biometric & Vitality Digest")
                appendLine("• Hydration: $waterMl / 2500 ml logged today")
                appendLine("• Steps: $steps steps recorded")
                appendLine("• Recommendation: Drink ${(2500 - waterMl).coerceAtLeast(0)} ml water in even intervals and complete a 20-min brisk walk or mobility circuit.")
            }

            AiCoachingIntent.HABITS -> buildString {
                appendLine("Edge AI Habit Consistency Analysis")
                appendLine("• Pending Daily Duties: ${uncompletedHabits.size} remaining today")
                val topAtRisk = uncompletedHabits.maxByOrNull { it.streakCount }
                if (topAtRisk != null) {
                    appendLine("• Streak Shield Alert: Protect your ${topAtRisk.streakCount}-day streak on \"${topAtRisk.name}\" before midnight.")
                } else {
                    appendLine("• All tracked duties cleared for today! Exceptional Monarch discipline.")
                }
            }

            AiCoachingIntent.TASKS -> buildString {
                appendLine("Edge AI Priority Execution Plan")
                appendLine("• Pending Quests: ${pendingTasks.size} total (${highPriTasks.size} High priority)")
                if (highPriTasks.isNotEmpty()) {
                    appendLine("• Next Action: Attack \"${highPriTasks.first().title}\" (+100 XP) in a 25-minute distraction-free Pomodoro sprint.")
                } else if (pendingTasks.isNotEmpty()) {
                    appendLine("• Next Action: Knock out \"${pendingTasks.first().title}\" next to maintain momentum.")
                } else {
                    appendLine("• Quest log clear! Use this window for strategic long-term goal planning.")
                }
            }

            AiCoachingIntent.JOURNAL -> buildString {
                appendLine("Edge AI Reflection & Mood Synthesis")
                appendLine("• Computed Mental Clarity Score: $mentalScore / 100 across ${snapshot.journals.size} journal entries")
                val tip = if (mentalScore >= 70) {
                    "Your emotional baseline is strong and resilient. Capture 3 gratitude wins tonight."
                } else {
                    "Recent entries indicate elevated cognitive load. Schedule a 10-minute box-breathing reset and lighten non-essential commitments."
                }
                appendLine("• Guidance: $tip")
            }

            AiCoachingIntent.GENERAL -> buildString {
                appendLine("Edge AI System Digest (On-Device Engine)")
                appendLine("• Quests: ${pendingTasks.size} pending (${highPriTasks.size} High priority)")
                appendLine("• Habits: ${snapshot.habits.count { it.isCompletedToday }}/${snapshot.habits.size} cleared today")
                appendLine("• Budget Utilization: ${budgetForecast.utilizationPercent}% ($${String.format("%.2f", budgetForecast.totalSpent)})")
                appendLine("• Mental Clarity Index: $mentalScore/100 | Hydration: ${waterMl}ml")
                append("• Actionable Step: Complete one High-priority quest + hydrate 250ml next.")
            }
        }.trim()
    }

    private fun String.containsAny(vararg keywords: String): Boolean =
        keywords.any { this.contains(it) }
}
