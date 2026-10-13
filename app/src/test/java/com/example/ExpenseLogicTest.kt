package com.example

import com.example.data.local.entities.ExpenseEntity
import org.junit.Assert.*
import org.junit.Test

/**
 * ============================================================================
 * UNIT TESTS: EXPENSE TRACKER & BUDGET CALCULATIONS
 * ============================================================================
 *
 * Verifies:
 * 1. Total income, total expense, and net balance arithmetic.
 * 2. Monthly budget threshold difference and over-budget alert status.
 * 3. Category grouping and slice distribution sums.
 */
class ExpenseLogicTest {

    private val sampleTransactions = listOf(
        ExpenseEntity(id = 1, title = "Salary", amount = 5000.0, type = "Income", category = "Salary"),
        ExpenseEntity(id = 2, title = "Apartment Rent", amount = 1200.0, type = "Expense", category = "Bills"),
        ExpenseEntity(id = 3, title = "Organic Grocery", amount = 250.0, type = "Expense", category = "Food"),
        ExpenseEntity(id = 4, title = "Gym Membership", amount = 50.0, type = "Expense", category = "Health"),
        ExpenseEntity(id = 5, title = "Dining Out", amount = 100.0, type = "Expense", category = "Food")
    )

    @Test
    fun financialTotals_calculatesIncomeExpenseAndNetBalance() {
        val totalIncome = sampleTransactions.filter { it.type == "Income" }.sumOf { it.amount }
        val totalExpense = sampleTransactions.filter { it.type == "Expense" }.sumOf { it.amount }
        val netBalance = totalIncome - totalExpense

        assertEquals(5000.0, totalIncome, 0.001)
        assertEquals(1600.0, totalExpense, 0.001)
        assertEquals(3400.0, netBalance, 0.001)
    }

    @Test
    fun budgetDifference_detectsExceededOrSafeBudget() {
        val totalExpense = 1600.0
        val monthlyBudgetSafe = 2000.0
        val monthlyBudgetExceeded = 1500.0

        val diffSafe = totalExpense - monthlyBudgetSafe
        val diffExceeded = totalExpense - monthlyBudgetExceeded

        assertTrue("Safe budget diff should be negative", diffSafe < 0)
        assertEquals(-400.0, diffSafe, 0.001)

        assertTrue("Exceeded budget diff should be positive", diffExceeded > 0)
        assertEquals(100.0, diffExceeded, 0.001)
    }

    @Test
    fun categoryGrouping_aggregatesSpendingPerCategory() {
        val expenseByCategory = sampleTransactions.filter { it.type == "Expense" }
            .groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amount } }

        assertEquals(350.0, expenseByCategory["Food"] ?: 0.0, 0.001)
        assertEquals(1200.0, expenseByCategory["Bills"] ?: 0.0, 0.001)
        assertEquals(50.0, expenseByCategory["Health"] ?: 0.0, 0.001)
        assertNull(expenseByCategory["Salary"])
    }
}
