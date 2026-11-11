package com.example.ui.navigation

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.data.local.entities.*
import com.example.domain.model.AppModule
import com.example.ui.screens.ai.AIAssistantScreen
import com.example.ui.screens.auth.AuthScreen
import com.example.ui.screens.calendar.CalendarScreen
import com.example.ui.screens.dashboard.DashboardScreen
import com.example.ui.screens.expenses.ExpenseScreen
import com.example.ui.screens.goals.GoalScreen
import com.example.ui.screens.guilds.GuildScreen
import com.example.ui.screens.habits.HabitScreen
import com.example.ui.screens.health.HealthScreen
import com.example.ui.screens.journal.JournalScreen
import com.example.ui.screens.notes.NotesScreen
import com.example.ui.screens.profile.ProfileScreen
import com.example.ui.screens.reports.ReportsScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.tasks.TaskScreen
import com.example.ui.viewmodel.AiChatMessage
import com.example.ui.viewmodel.LifeTrackerViewModel

@Composable
fun AppNavHost(
    appRouter: AppRouter,
    viewModel: LifeTrackerViewModel,
    userProfile: UserProfileEntity?,
    userStats: UserStatsEntity? = null,
    userEmail: String,
    tasks: List<TaskEntity>,
    habits: List<HabitEntity>,
    expenses: List<ExpenseEntity>,
    latestHealth: HealthLogEntity?,
    journals: List<JournalEntity>,
    notes: List<NoteEntity>,
    goals: List<GoalEntity>,
    calendarEvents: List<CalendarEventEntity>,
    aiMessages: List<AiChatMessage>,
    isAiLoading: Boolean,
    isDarkMode: Boolean,
    liveSensorSteps: Int = 0,
    isStepSensorAvailable: Boolean = false,
    context: Context,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = appRouter.navController,
        startDestination = AppRouter.routeForModule(AppModule.DASHBOARD),
        enterTransition = { fadeIn(animationSpec = tween(280)) + slideInHorizontally(animationSpec = tween(280)) { it / 6 } },
        exitTransition = { fadeOut(animationSpec = tween(200)) },
        popEnterTransition = { fadeIn(animationSpec = tween(280)) + slideInHorizontally(animationSpec = tween(280)) { -it / 6 } },
        popExitTransition = { fadeOut(animationSpec = tween(200)) },
        modifier = modifier
    ) {
        // Dashboard Screen
        composable(
            route = AppRouter.routeForModule(AppModule.DASHBOARD),
            deepLinks = AppRouter.createDeepLinks(AppRouter.routeForModule(AppModule.DASHBOARD))
        ) {
            val expensesToday = expenses.filter { it.type == "Expense" }.sumOf { it.amount }
            val habitStreakMax = habits.maxOfOrNull { it.streakCount } ?: 0
            val habitCompletionPercent = if (habits.isNotEmpty()) {
                (habits.count { it.isCompletedToday } * 100) / habits.size
            } else 0
            val goalProgressAvg = if (goals.isNotEmpty()) {
                goals.map { it.progressPercent }.average().toInt()
            } else 0

            DashboardScreen(
                userProfile = userProfile,
                userStats = userStats,
                tasks = tasks.filter { !it.isCompleted },
                expensesTotalToday = expensesToday,
                waterIntakeMl = latestHealth?.waterIntakeMl ?: 0,
                sleepHours = latestHealth?.sleepHours ?: 0f,
                habitStreakDays = habitStreakMax,
                habitCompletionPercent = habitCompletionPercent,
                goalProgressPercent = goalProgressAvg,
                onNavigate = { dest ->
                    viewModel.navigateTo(dest)
                    appRouter.navigateTo(dest)
                },
                onAddWater = { amount ->
                    viewModel.addWaterIntake(amount)
                    Toast.makeText(context, "+$amount ml Water Added 💧", Toast.LENGTH_SHORT).show()
                },
                onToggleTask = { task -> viewModel.toggleTaskCompletion(task) }
            )
        }

        // Tasks Screen
        composable(
            route = AppRouter.routeForModule(AppModule.TASKS),
            deepLinks = AppRouter.createDeepLinks(AppRouter.routeForModule(AppModule.TASKS))
        ) {
            TaskScreen(
                tasks = tasks,
                onAddTask = { title, cat, pri, due ->
                    viewModel.addTask(title, cat, pri, due)
                    Toast.makeText(context, "Task created!", Toast.LENGTH_SHORT).show()
                },
                onToggleTask = { task -> viewModel.toggleTaskCompletion(task) },
                onDeleteTask = { task -> viewModel.deleteTask(task) }
            )
        }

        // Habits Screen
        composable(
            route = AppRouter.routeForModule(AppModule.HABITS),
            deepLinks = AppRouter.createDeepLinks(AppRouter.routeForModule(AppModule.HABITS))
        ) {
            HabitScreen(
                habits = habits,
                onAddHabit = { name, cat, freq ->
                    viewModel.addHabit(name, cat, freq)
                    Toast.makeText(context, "Habit created 🔥", Toast.LENGTH_SHORT).show()
                },
                onToggleHabit = { habit -> viewModel.toggleHabitToday(habit) },
                onDeleteHabit = { habit -> viewModel.deleteHabit(habit) }
            )
        }

        // Expenses Screen
        composable(
            route = AppRouter.routeForModule(AppModule.EXPENSES),
            deepLinks = AppRouter.createDeepLinks(AppRouter.routeForModule(AppModule.EXPENSES))
        ) {
            ExpenseScreen(
                expenses = expenses,
                monthlyBudget = userProfile?.monthlyBudget ?: 3500.0,
                onAddExpense = { title, amount, type, cat, notesStr ->
                    viewModel.addExpense(title, amount, type, cat, notesStr)
                    Toast.makeText(context, "Transaction saved 💳", Toast.LENGTH_SHORT).show()
                },
                onDeleteExpense = { expense -> viewModel.deleteExpense(expense) }
            )
        }

        // Health & Sensor Screen
        composable(
            route = AppRouter.routeForModule(AppModule.HEALTH),
            deepLinks = AppRouter.createDeepLinks(AppRouter.routeForModule(AppModule.HEALTH))
        ) {
            HealthScreen(
                healthLog = latestHealth,
                liveSensorSteps = liveSensorSteps,
                isSensorAvailable = isStepSensorAvailable,
                onAddWater = { amount -> viewModel.addWaterIntake(amount) },
                onSyncSteps = { stepsDelta -> viewModel.syncPedometerSteps(stepsDelta) },
                onUpdateMetrics = { w, h, s, steps, mood, workout ->
                    viewModel.logHealthMetrics(w, h, s, steps, mood, workout)
                    Toast.makeText(context, "Health metrics saved ❤️", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Journal Screen
        composable(
            route = AppRouter.routeForModule(AppModule.JOURNAL),
            deepLinks = AppRouter.createDeepLinks(AppRouter.routeForModule(AppModule.JOURNAL))
        ) {
            JournalScreen(
                journals = journals,
                onAddJournal = { title, content, mood, tags ->
                    viewModel.addJournalEntry(title, content, mood, tags)
                    Toast.makeText(context, "Journal entry saved 📖", Toast.LENGTH_SHORT).show()
                },
                onDeleteJournal = { journal -> viewModel.deleteJournal(journal) },
                onAskAiToSummarize = {
                    viewModel.navigateTo(AppModule.AI_ASSISTANT)
                    appRouter.navigateTo(AppModule.AI_ASSISTANT)
                    viewModel.sendMessageToAi("Please summarize my recent journal entries and highlight key mood insights.")
                }
            )
        }

        // Notes Screen
        composable(
            route = AppRouter.routeForModule(AppModule.NOTES),
            deepLinks = AppRouter.createDeepLinks(AppRouter.routeForModule(AppModule.NOTES))
        ) {
            NotesScreen(
                notes = notes,
                onAddNote = { title, content, isChecklist, folder ->
                    viewModel.addNote(title, content, isChecklist, folder)
                    Toast.makeText(context, "Note saved 📝", Toast.LENGTH_SHORT).show()
                },
                onTogglePin = { note -> viewModel.toggleNotePin(note) },
                onDeleteNote = { note -> viewModel.deleteNote(note) }
            )
        }

        // Goals Screen
        composable(
            route = AppRouter.routeForModule(AppModule.GOALS),
            deepLinks = AppRouter.createDeepLinks(AppRouter.routeForModule(AppModule.GOALS))
        ) {
            GoalScreen(
                goals = goals,
                onAddGoal = { title, desc, cat, type, ms ->
                    viewModel.addGoal(title, desc, cat, type, ms)
                    Toast.makeText(context, "Goal created 🎯", Toast.LENGTH_SHORT).show()
                },
                onUpdateProgress = { goal, progress -> viewModel.updateGoalProgress(goal, progress) },
                onDeleteGoal = { goal -> viewModel.deleteGoal(goal) }
            )
        }

        // Calendar Screen
        composable(
            route = AppRouter.routeForModule(AppModule.CALENDAR),
            deepLinks = AppRouter.createDeepLinks(AppRouter.routeForModule(AppModule.CALENDAR))
        ) {
            CalendarScreen(
                events = calendarEvents,
                onAddEvent = { title, desc, time, type, loc ->
                    viewModel.addCalendarEvent(title, desc, time, type, loc)
                    Toast.makeText(context, "Event scheduled 📅", Toast.LENGTH_SHORT).show()
                },
                onDeleteEvent = { event -> viewModel.deleteCalendarEvent(event) }
            )
        }

        // AI Assistant Screen
        composable(
            route = AppRouter.routeForModule(AppModule.AI_ASSISTANT),
            deepLinks = AppRouter.createDeepLinks(AppRouter.routeForModule(AppModule.AI_ASSISTANT))
        ) {
            AIAssistantScreen(
                chatMessages = aiMessages,
                isLoading = isAiLoading,
                tasks = tasks,
                habits = habits,
                expenses = expenses,
                userProfile = userProfile,
                onSendMessage = { prompt -> viewModel.sendMessageToAi(prompt) }
            )
        }

        // Hunter Guilds & Collaborative Raids Screen
        composable(
            route = AppRouter.routeForModule(AppModule.GUILDS),
            deepLinks = AppRouter.createDeepLinks(AppRouter.routeForModule(AppModule.GUILDS))
        ) {
            val guildProgress by viewModel.guildProgress.collectAsState()
            GuildScreen(
                guildProgress = guildProgress,
                userName = userProfile?.name ?: "Hunter",
                userStats = userStats,
                onDealRaidStrike = { dmg -> viewModel.dealGuildRaidStrike(dmg) },
                onSwitchGuild = { name, motto -> viewModel.switchGuildName(name, motto) }
            )
        }

        // Reports & PDF/CSV Export Screen
        composable(
            route = AppRouter.routeForModule(AppModule.REPORTS),
            deepLinks = AppRouter.createDeepLinks(AppRouter.routeForModule(AppModule.REPORTS))
        ) {
            ReportsScreen(
                tasks = tasks,
                habits = habits,
                expenses = expenses,
                latestHealth = latestHealth,
                userName = userProfile?.name ?: "Hunter",
                level = userStats?.level ?: 1,
                totalXp = userStats?.totalXp ?: 0
            )
        }

        // Settings Screen
        composable(
            route = AppRouter.routeForModule(AppModule.SETTINGS),
            deepLinks = AppRouter.createDeepLinks(AppRouter.routeForModule(AppModule.SETTINGS))
        ) {
            val syncStatus by viewModel.syncStatus.collectAsState()
            val syncReport by viewModel.syncReport.collectAsState()
            val backupJson by viewModel.backupJsonString.collectAsState()
            val restoreMessage by viewModel.restoreMessage.collectAsState()

            SettingsScreen(
                isDarkMode = isDarkMode,
                userEmail = userEmail,
                syncStatus = syncStatus,
                syncReport = syncReport,
                backupJson = backupJson,
                restoreMessage = restoreMessage,
                onToggleDarkMode = { dark ->
                    viewModel.updateProfileNameAndGoal(
                        name = userProfile?.name ?: "User",
                        budget = userProfile?.monthlyBudget ?: 2000.0,
                        waterGoal = userProfile?.dailyWaterGoalMl ?: 2500,
                        darkMode = dark
                    )
                },
                onBackupData = { viewModel.exportBackupJson() },
                onRestoreData = { jsonInput -> viewModel.restoreFromJson(jsonInput) },
                onClearBackupJson = { viewModel.clearBackupJson() },
                onClearRestoreMessage = { viewModel.clearRestoreMessage() },
                onTriggerCloudSync = {
                    viewModel.triggerCloudSync()
                    Toast.makeText(context, "Running two-way cloud sync... ☁️", Toast.LENGTH_SHORT).show()
                },
                onClearAllData = {
                    viewModel.clearAllUserData()
                    Toast.makeText(context, "All data deleted. Application reset to fresh state.", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Profile Screen
        composable(
            route = AppRouter.routeForModule(AppModule.PROFILE),
            deepLinks = AppRouter.createDeepLinks(AppRouter.routeForModule(AppModule.PROFILE))
        ) {
            val hunterStats by viewModel.hunterStats.collectAsState()
            ProfileScreen(
                userProfile = userProfile,
                userEmail = userEmail,
                userStats = userStats,
                hunterStats = hunterStats,
                onAllocateStatPoint = { statName -> viewModel.allocateStatPoint(statName) },
                onSaveProfile = { name, budget, waterGoal ->
                    viewModel.updateProfileNameAndGoal(name, budget, waterGoal, isDarkMode)
                    Toast.makeText(context, "System configuration updated! ✨", Toast.LENGTH_SHORT).show()
                },
                onLogout = { viewModel.logout() }
            )
        }

        // Auth Screen
        composable(
            route = AppRouter.routeForModule(AppModule.AUTH),
            deepLinks = AppRouter.createDeepLinks(AppRouter.routeForModule(AppModule.AUTH))
        ) {
            AuthScreen(onLoginSuccess = { email -> viewModel.login(email) })
        }
    }
}
