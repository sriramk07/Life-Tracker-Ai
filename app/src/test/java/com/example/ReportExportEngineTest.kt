package com.example

import com.example.data.local.entities.ExpenseEntity
import com.example.data.local.entities.HabitEntity
import com.example.data.local.entities.HealthLogEntity
import com.example.data.local.entities.TaskEntity
import com.example.export.LifeReportSnapshot
import com.example.export.ReportExportEngine
import org.junit.Assert.*
import org.junit.Test

/**
 * ============================================================================
 * UNIT TESTS: REPORT EXPORT ENGINE (CSV, EFFICIENCY & EXECUTIVE SUMMARY)
 * ============================================================================
 */
class ReportExportEngineTest {

    @Test
    fun escapeCsvField_quotesCommasQuotesAndNewlinesPerRfc4180() {
        assertEquals("SimpleText", ReportExportEngine.escapeCsvField("SimpleText"))
        assertEquals("\"Food, Dining\"", ReportExportEngine.escapeCsvField("Food, Dining"))
        assertEquals("\"Quote \"\"Pro\"\"\"", ReportExportEngine.escapeCsvField("Quote \"Pro\""))
    }

    @Test
    fun calculateEfficiencyScore_computesWeightedProgress() {
        val tasks = listOf(
            TaskEntity(id = 1, title = "T1", isCompleted = true),
            TaskEntity(id = 2, title = "T2", isCompleted = true)
        )
        val habits = listOf(
            HabitEntity(id = 1, name = "H1", isCompletedToday = true)
        )
        val scoreFull = ReportExportEngine.calculateEfficiencyScore(tasks, habits, waterIntakeMl = 2500)
        assertEquals(100, scoreFull)

        val emptyZero = ReportExportEngine.calculateEfficiencyScore(
            tasks = listOf(TaskEntity(id = 1, title = "T1", isCompleted = false)),
            habits = listOf(HabitEntity(id = 1, name = "H1", isCompletedToday = false)),
            waterIntakeMl = 0
        )
        assertEquals(0, emptyZero)
    }

    @Test
    fun computeExpenseSlices_aggregatesExpensesAndIgnoresIncome() {
        val tx = listOf(
            ExpenseEntity(id = 1, title = "Paycheck", amount = 4000.0, type = "Income", category = "Salary"),
            ExpenseEntity(id = 2, title = "Rent", amount = 1000.0, type = "Expense", category = "Bills"),
            ExpenseEntity(id = 3, title = "Lunch", amount = 25.0, type = "Expense", category = "Food"),
            ExpenseEntity(id = 4, title = "Dinner", amount = 50.0, type = "Expense", category = "Food")
        )

        val slices = ReportExportEngine.computeExpenseSlices(tx)
        assertEquals(2, slices.size)
        assertEquals("Bills", slices[0].category)
        assertEquals(1000.0, slices[0].value, 0.01)
        assertEquals("Food", slices[1].category)
        assertEquals(75.0, slices[1].value, 0.01)
    }

    @Test
    fun generateLifeLedgerCsvAndExecutiveSummary_includeLiveRecords() {
        val snapshot = LifeReportSnapshot(
            userName = "Sriram",
            timeframe = "Monthly",
            tasks = listOf(TaskEntity(id = 1, title = "Finish Final Release, v2", category = "Work", priority = "High", isCompleted = true)),
            habits = listOf(HabitEntity(id = 1, name = "Meditate", streakCount = 21, isCompletedToday = true)),
            expenses = listOf(ExpenseEntity(id = 1, title = "Books", amount = 42.5, type = "Expense", category = "Study")),
            latestHealth = HealthLogEntity(waterIntakeMl = 2500, stepsCount = 9200, sleepHours = 8.0f),
            level = 7,
            totalXp = 3200
        )

        val csv = ReportExportEngine.generateLifeLedgerCsv(snapshot)
        assertTrue(csv.startsWith("Section,ID,Title,Category"))
        assertTrue(csv.contains("\"Finish Final Release, v2\""))
        assertTrue(csv.contains("Finance,1,Books,Study,Expense,42.50"))

        val summary = ReportExportEngine.generateExecutiveSummaryText(snapshot)
        assertTrue(summary.contains("Hunter: Sriram"))
        assertTrue(summary.contains("Total Expenses: $42.50"))
        assertTrue(summary.contains("Highest Active Streak: 21 days"))
    }
}
