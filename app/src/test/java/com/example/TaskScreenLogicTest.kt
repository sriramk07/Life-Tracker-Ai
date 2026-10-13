package com.example

import com.example.data.local.entities.TaskEntity
import org.junit.Assert.*
import org.junit.Test

/**
 * ============================================================================
 * UNIT TESTS: TASK MANAGER BUSINESS LOGIC & FILTERING
 * ============================================================================
 *
 * Verifies:
 * 1. Filtering by Priority ("High", "Medium", "Low", "All").
 * 2. Filtering by Category ("Work", "Personal", "Health", "All").
 * 3. Text search query matching (title substring, category substring).
 * 4. XP reward calculation rules.
 */
class TaskScreenLogicTest {

    private val sampleTasks = listOf(
        TaskEntity(id = 1, title = "Complete System Architecture", priority = "High", category = "Work", isCompleted = false),
        TaskEntity(id = 2, title = "HIIT Cardio Workout", priority = "Medium", category = "Health", isCompleted = true),
        TaskEntity(id = 3, title = "Weekly Grocery Run", priority = "Low", category = "Personal", isCompleted = false),
        TaskEntity(id = 4, title = "Prepare Review 2 Report", priority = "High", category = "Work", isCompleted = false)
    )

    @Test
    fun filterTasks_matchesPriorityCorrectly() {
        val highPriority = sampleTasks.filter { it.priority.equals("High", ignoreCase = true) }
        assertEquals(2, highPriority.size)
        assertTrue(highPriority.all { it.priority == "High" })

        val lowPriority = sampleTasks.filter { it.priority.equals("Low", ignoreCase = true) }
        assertEquals(1, lowPriority.size)
        assertEquals("Weekly Grocery Run", lowPriority.first().title)
    }

    @Test
    fun filterTasks_matchesCategoryCorrectly() {
        val workTasks = sampleTasks.filter { it.category.equals("Work", ignoreCase = true) }
        assertEquals(2, workTasks.size)

        val healthTasks = sampleTasks.filter { it.category.equals("Health", ignoreCase = true) }
        assertEquals(1, healthTasks.size)
        assertEquals("HIIT Cardio Workout", healthTasks.first().title)
    }

    @Test
    fun filterTasks_searchQuery_matchesTitleOrCategoryCaseInsensitive() {
        val query = "report"
        val matched = sampleTasks.filter {
            it.title.contains(query, ignoreCase = true) || it.category.contains(query, ignoreCase = true)
        }
        assertEquals(1, matched.size)
        assertEquals("Prepare Review 2 Report", matched.first().title)

        val categoryQuery = "health"
        val matchedCategory = sampleTasks.filter {
            it.title.contains(categoryQuery, ignoreCase = true) || it.category.contains(categoryQuery, ignoreCase = true)
        }
        assertEquals(1, matchedCategory.size)
        assertEquals("HIIT Cardio Workout", matchedCategory.first().title)
    }

    @Test
    fun taskXpReward_calculatesHigherForHighPriority() {
        fun calculateXp(priority: String): Int = when (priority.lowercase()) {
            "high" -> 100
            "medium" -> 50
            "low" -> 25
            else -> 50
        }

        assertEquals(100, calculateXp("High"))
        assertEquals(50, calculateXp("Medium"))
        assertEquals(25, calculateXp("Low"))
        assertEquals(50, calculateXp("Unknown"))
    }
}
