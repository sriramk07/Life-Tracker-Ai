package com.example

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.common.LifeTrackerErrorBoundary
import com.example.data.local.entities.*
import com.example.ui.screens.guilds.GuildScreen
import com.example.ui.screens.health.HealthScreen
import com.example.ui.screens.reports.ReportsScreen
import com.example.ui.screens.security.AppLockScreen
import com.example.ui.screens.tasks.TaskScreen
import com.example.ui.theme.LifeTrackerTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * ============================================================================
 * AUTOMATED COMPOSE UI INSTRUMENTED TESTS (RELEASE 1.0)
 * ============================================================================
 *
 * Validates critical user flows across primary screens and security boundaries:
 * 1. ErrorBoundary diagnostic UI and retry action callback.
 * 2. TaskScreen priority filter interactions.
 * 3. HealthScreen hydration and pedometer button triggers.
 * 4. ReportsScreen live analytics and Export Studio dialog actions.
 * 5. GuildScreen collaborative raid strike button interaction.
 * 6. AppLockScreen invalid PIN warning and valid PIN unlock flow.
 */
@RunWith(AndroidJUnit4::class)
class LifeTrackerUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun errorBoundary_displaysAlertAndInvokesRetryCallback() {
        var retried = false
        composeTestRule.setContent {
            LifeTrackerTheme(darkTheme = true) {
                LifeTrackerErrorBoundary(
                    errorMessage = "Simulated network drop",
                    onRetry = { retried = true }
                ) {}
            }
        }

        composeTestRule.onNodeWithTag("error_boundary_view").assertIsDisplayed()
        composeTestRule.onNodeWithText("Simulated network drop").assertIsDisplayed()
        composeTestRule.onNodeWithTag("error_boundary_retry_btn").performClick()
        assertTrue(retried)
    }

    @Test
    fun taskScreen_filtersTasksByPriorityChip() {
        val sampleTasks = listOf(
            TaskEntity(id = 1, title = "Finalize Release 1.0", priority = "High", category = "Work"),
            TaskEntity(id = 2, title = "Pick up groceries", priority = "Low", category = "Personal")
        )

        composeTestRule.setContent {
            LifeTrackerTheme(darkTheme = true) {
                TaskScreen(
                    tasks = sampleTasks,
                    onAddTask = { _, _, _, _ -> },
                    onToggleTask = {},
                    onDeleteTask = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Finalize Release 1.0").assertIsDisplayed()
        composeTestRule.onNodeWithText("Pick up groceries").assertIsDisplayed()

        composeTestRule.onNodeWithTag("filter_priority_High").performClick()
        composeTestRule.onNodeWithText("Finalize Release 1.0").assertIsDisplayed()
        composeTestRule.onNodeWithText("Pick up groceries").assertDoesNotExist()
    }

    @Test
    fun healthScreen_addWaterAndSyncStepsCallbacksFire() {
        var waterAdded = 0
        var stepsSynced = 0

        composeTestRule.setContent {
            LifeTrackerTheme(darkTheme = true) {
                HealthScreen(
                    healthLog = HealthLogEntity(waterIntakeMl = 1000, weightKg = 70f, heightCm = 175f, stepsCount = 3000),
                    onAddWater = { waterAdded += it },
                    onSyncSteps = { stepsSynced += it },
                    onUpdateMetrics = { _, _, _, _, _, _ -> }
                )
            }
        }

        composeTestRule.onNodeWithTag("water_add_250_btn").performScrollTo().performClick()
        assertEquals(250, waterAdded)

        composeTestRule.onNodeWithTag("sync_steps_btn").performScrollTo().performClick()
        assertEquals(500, stepsSynced)
    }

    @Test
    fun reportsScreen_opensExportStudioModalWithPdfAndCsvActions() {
        composeTestRule.setContent {
            LifeTrackerTheme(darkTheme = true) {
                ReportsScreen(
                    tasks = listOf(TaskEntity(id = 1, title = "Done Task", isCompleted = true)),
                    habits = listOf(HabitEntity(id = 1, name = "Read", streakCount = 5, isCompletedToday = true)),
                    expenses = listOf(ExpenseEntity(id = 1, title = "Lunch", amount = 15.0, category = "Food"))
                )
            }
        }

        composeTestRule.onNodeWithTag("export_report_btn").performClick()
        composeTestRule.onNodeWithTag("export_pdf_btn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("export_csv_btn").assertIsDisplayed()
    }

    @Test
    fun guildScreen_raidStrikeButtonTriggersContribution() {
        var damageDone = 0
        composeTestRule.setContent {
            LifeTrackerTheme(darkTheme = true) {
                GuildScreen(
                    guildProgress = GuildProgressEntity(id = 1, raidDamageDealt = 1000, raidBossHp = 5000),
                    userName = "Sriram",
                    userStats = UserStatsEntity(totalXp = 2500, level = 6),
                    onDealRaidStrike = { damageDone += it },
                    onSwitchGuild = { _, _ -> }
                )
            }
        }

        composeTestRule.onNodeWithTag("guild_raid_strike_btn").performClick()
        assertEquals(50, damageDone)
    }

    @Test
    fun appLockScreen_rejectsWrongPinAndUnlocksValidPin() {
        var unlocked = false
        composeTestRule.setContent {
            LifeTrackerTheme(darkTheme = true) {
                AppLockScreen(
                    canUseBiometrics = false,
                    onVerifyPin = { it == "1234" },
                    onTriggerBiometric = {},
                    onUnlocked = { unlocked = true }
                )
            }
        }

        composeTestRule.onNodeWithTag("vault_pin_input").performTextInput("9999")
        composeTestRule.onNodeWithTag("vault_unlock_btn").performClick()
        composeTestRule.onNodeWithTag("vault_pin_error").assertIsDisplayed()

        composeTestRule.onNodeWithTag("vault_pin_input").performTextReplacement("1234")
        composeTestRule.onNodeWithTag("vault_unlock_btn").performClick()
        assertTrue(unlocked)
    }
}
