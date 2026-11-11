package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.common.createViewModelExceptionHandler
import com.example.data.local.AppDatabase
import com.example.data.local.entities.*
import com.example.data.remote.GeminiApiService
import com.example.data.repository.LifeTrackerRepository
import com.example.data.sync.SyncReport
import com.example.domain.ai.EdgeAiCoachEngine
import com.example.domain.ai.PredictiveLifeSnapshot
import com.example.domain.model.AppModule
import com.example.domain.model.GamificationProfile
import com.example.domain.model.HunterStats
import com.example.sensors.VitalityAnalytics
import com.example.ui.components.HudEvent
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Data model for messages exchanged in the AI Assistant chat tab.
 */
data class AiChatMessage(
    val sender: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * ============================================================================
 * LIFE TRACKER VIEWMODEL — CENTRAL BUSINESS LOGIC & ORCHESTRATION (RELEASE 1.0)
 * ============================================================================
 *
 * Manages unidirectional data flow (UDF) across the application:
 * 1. Reactive [StateFlow] streams across all 12 Room entities.
 * 2. Coroutine error boundaries with [createViewModelExceptionHandler].
 * 3. Persisted Solo Leveling Hunter stats & guild raid damage contributions.
 * 4. Bidirectional cloud synchronization reporting conflicts & deltas.
 * 5. Real-time hardware pedometer & health metrics orchestration.
 * 6. On-device Edge AI snapshot enrichment & chat coaching.
 */
class LifeTrackerViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = LifeTrackerRepository(
        taskDao = db.taskDao(),
        habitDao = db.habitDao(),
        expenseDao = db.expenseDao(),
        healthDao = db.healthDao(),
        journalDao = db.journalDao(),
        noteDao = db.noteDao(),
        goalDao = db.goalDao(),
        calendarDao = db.calendarDao(),
        userDao = db.userDao(),
        userStatsDao = db.userStatsDao(),
        categoryDao = db.categoryDao(),
        guildProgressDao = db.guildProgressDao(),
        geminiApiService = GeminiApiService()
    )

    // Global Error Boundary State
    private val _systemError = MutableStateFlow<String?>(null)
    val systemError: StateFlow<String?> = _systemError.asStateFlow()

    private val exceptionHandler = createViewModelExceptionHandler("LifeTrackerViewModel") { errorMessage ->
        _systemError.value = errorMessage
    }

    // Current Active Navigation Destination
    private val _currentModule = MutableStateFlow(AppModule.DASHBOARD)
    val currentModule: StateFlow<AppModule> = _currentModule.asStateFlow()

    // Authentication State
    private val _isLoggedIn = MutableStateFlow(true)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _userEmail = MutableStateFlow("user@gmail.com")
    val userEmail: StateFlow<String> = _userEmail.asStateFlow()

    // Bidirectional Cloud Sync State
    private val _syncStatus = MutableStateFlow("Synced")
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    private val _syncReport = MutableStateFlow<SyncReport?>(null)
    val syncReport: StateFlow<SyncReport?> = _syncReport.asStateFlow()

    private val _lastSyncedTimestamp = MutableStateFlow(System.currentTimeMillis())
    val lastSyncedTimestamp: StateFlow<Long> = _lastSyncedTimestamp.asStateFlow()

    // Backup & Restore Export State
    private val _backupJsonString = MutableStateFlow<String?>(null)
    val backupJsonString: StateFlow<String?> = _backupJsonString.asStateFlow()

    private val _restoreMessage = MutableStateFlow<String?>(null)
    val restoreMessage: StateFlow<String?> = _restoreMessage.asStateFlow()

    init {
        viewModelScope.launch(exceptionHandler) {
            repository.clearFakeSeedData()
        }
    }

    fun clearSystemError() {
        _systemError.value = null
    }

    // ========================================================================
    // DATABASE STATEFLOWS (HOT CACHED)
    // ========================================================================

    val categories: StateFlow<List<CategoryEntity>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tasks: StateFlow<List<TaskEntity>> = repository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val habits: StateFlow<List<HabitEntity>> = repository.allHabits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expenses: StateFlow<List<ExpenseEntity>> = repository.allExpenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val healthLogs: StateFlow<List<HealthLogEntity>> = repository.allHealthLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val latestHealthLog: StateFlow<HealthLogEntity?> = repository.latestHealthLog
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val journals: StateFlow<List<JournalEntity>> = repository.allJournals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notes: StateFlow<List<NoteEntity>> = repository.allNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val goals: StateFlow<List<GoalEntity>> = repository.allGoals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val calendarEvents: StateFlow<List<CalendarEventEntity>> = repository.allCalendarEvents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userProfile: StateFlow<UserProfileEntity?> = repository.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val userStats: StateFlow<UserStatsEntity?> = repository.userStats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val guildProgress: StateFlow<GuildProgressEntity?> = repository.guildProgress
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GuildProgressEntity())

    // Persisted Hunter attributes derived directly from Room UserStatsEntity
    val hunterStats: StateFlow<HunterStats> = userStats.map { entity ->
        if (entity == null) {
            HunterStats()
        } else {
            HunterStats(
                strength = entity.strength,
                agility = entity.agility,
                intelligence = entity.intelligence,
                vitality = entity.vitality,
                sense = entity.sense,
                unallocatedPoints = entity.unallocatedPoints
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HunterStats())

    // ========================================================================
    // GAMIFICATION & SOLO LEVELING SYSTEM ENGINE
    // ========================================================================

    private val _hudEvent = MutableStateFlow<HudEvent?>(null)
    val hudEvent: StateFlow<HudEvent?> = _hudEvent.asStateFlow()

    private val _levelUpEvent = MutableStateFlow<Int?>(null)
    val levelUpEvent: StateFlow<Int?> = _levelUpEvent.asStateFlow()

    fun triggerHudAlert(title: String, subtitle: String, xpGained: Int = 0, isLevelUp: Boolean = false) {
        _hudEvent.value = HudEvent(
            title = title,
            subtitle = subtitle,
            xpGained = xpGained,
            isLevelUp = isLevelUp
        )
    }

    fun dismissHudAlert() {
        _hudEvent.value = null
    }

    fun clearLevelUpEvent() {
        _levelUpEvent.value = null
    }

    /**
     * Allocates an attribute point to a chosen RPG stat and persists it in Room.
     */
    fun allocateStatPoint(statName: String) {
        viewModelScope.launch(exceptionHandler) {
            val current = userStats.value ?: UserStatsEntity()
            if (current.unallocatedPoints <= 0) return@launch
            val updated = when (statName.uppercase()) {
                "STR" -> current.copy(strength = current.strength + 1, unallocatedPoints = current.unallocatedPoints - 1)
                "AGI" -> current.copy(agility = current.agility + 1, unallocatedPoints = current.unallocatedPoints - 1)
                "INT" -> current.copy(intelligence = current.intelligence + 1, unallocatedPoints = current.unallocatedPoints - 1)
                "VIT" -> current.copy(vitality = current.vitality + 1, unallocatedPoints = current.unallocatedPoints - 1)
                "SNE" -> current.copy(sense = current.sense + 1, unallocatedPoints = current.unallocatedPoints - 1)
                else -> current
            }
            repository.updateUserStats(updated)

            val newVal = when (statName.uppercase()) {
                "STR" -> updated.strength
                "AGI" -> updated.agility
                "INT" -> updated.intelligence
                "VIT" -> updated.vitality
                else -> updated.sense
            }
            triggerHudAlert("STAT POINT ALLOCATED", "$statName upgraded to $newVal")
        }
    }

    /**
     * Awards RPG Experience Points (XP), persists attributes, deals Guild Raid damage,
     * and broadcasts HUD notifications.
     */
    fun grantXp(xpAmount: Int, actionTitle: String = "Quest Cleared") {
        viewModelScope.launch(exceptionHandler) {
            val current = userStats.value ?: UserStatsEntity()
            val previousLevel = GamificationProfile.calculateLevel(current.totalXp)
            val newTotalXp = current.totalXp + xpAmount
            val newLevel = GamificationProfile.calculateLevel(newTotalXp)
            val levelUpOccurred = newLevel > previousLevel

            val updatedStats = current.copy(
                totalXp = newTotalXp,
                level = newLevel,
                coins = current.coins + (xpAmount / 5),
                unallocatedPoints = if (levelUpOccurred) current.unallocatedPoints + 3 else current.unallocatedPoints,
                updatedAt = System.currentTimeMillis()
            )
            repository.updateUserStats(updatedStats)
            repository.contributeGuildRaidDamage(xpAmount)

            if (levelUpOccurred) {
                _levelUpEvent.value = newLevel
                triggerHudAlert(
                    title = "LEVEL UP! REACHED LEVEL $newLevel",
                    subtitle = GamificationProfile.calculateLevelTitle(newLevel),
                    xpGained = xpAmount,
                    isLevelUp = true
                )
            } else {
                triggerHudAlert(
                    title = actionTitle,
                    subtitle = "+$xpAmount XP Gained (+Guild Raid Damage)",
                    xpGained = xpAmount
                )
            }
        }
    }

    // ========================================================================
    // COLLABORATIVE GUILD OPERATIONS
    // ========================================================================

    fun dealGuildRaidStrike(damageXp: Int = 50) {
        grantXp(damageXp, "Guild Raid Strike")
    }

    fun switchGuildName(guildName: String, motto: String) {
        viewModelScope.launch(exceptionHandler) {
            val current = guildProgress.value ?: GuildProgressEntity()
            repository.updateGuildProgress(
                current.copy(
                    guildName = guildName.ifBlank { current.guildName },
                    guildMotto = motto.ifBlank { current.guildMotto }
                )
            )
            triggerHudAlert("GUILD UPDATED", "Joined guild: $guildName")
        }
    }

    // ========================================================================
    // AI ASSISTANT CHAT STATE & ON-DEVICE EDGE COACHING
    // ========================================================================

    private val _aiChatMessages = MutableStateFlow<List<AiChatMessage>>(
        listOf(
            AiChatMessage(
                sender = "Gemini",
                text = "Hello! 👋 I'm your Life Tracker AI Assistant powered by Gemini 2.5 Flash & On-Device Edge Analytics. Ask me about spending forecasts, journal sentiment, habit streaks, or workout routines!"
            )
        )
    )
    val aiChatMessages: StateFlow<List<AiChatMessage>> = _aiChatMessages.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    fun buildLiveLifeSnapshot(): PredictiveLifeSnapshot {
        return PredictiveLifeSnapshot(
            tasks = tasks.value,
            habits = habits.value,
            expenses = expenses.value,
            healthLog = latestHealthLog.value,
            journals = journals.value,
            monthlyBudget = userProfile.value?.monthlyBudget ?: 3000.0
        )
    }

    fun sendMessageToAi(promptText: String) {
        if (promptText.isBlank()) return

        val userMessage = AiChatMessage(sender = "User", text = promptText)
        _aiChatMessages.value = _aiChatMessages.value + userMessage
        _isAiLoading.value = true

        viewModelScope.launch(exceptionHandler) {
            val liveSnapshot = buildLiveLifeSnapshot()
            try {
                val currentTasks = tasks.value.take(5).joinToString { "${it.title} (${if (it.isCompleted) "Done" else "Pending"})" }
                val currentHabits = habits.value.joinToString { "${it.name}: ${it.streakCount} day streak" }
                val totalExpense = expenses.value.filter { it.type == "Expense" }.sumOf { it.amount }
                val health = latestHealthLog.value
                val healthContext = "Water: ${health?.waterIntakeMl ?: 0}ml, Sleep: ${health?.sleepHours ?: 0f}h, Steps: ${health?.stepsCount ?: 0}, Mood: ${health?.mood ?: "Good"}"

                val contextualPrompt = """
                    User Profile context:
                    - Recent Tasks: $currentTasks
                    - Active Habits: $currentHabits
                    - Total Expenses: $$totalExpense
                    - Health Metrics: $healthContext

                    User Query: $promptText
                """.trimIndent()

                val response = repository.askGemini(contextualPrompt, liveSnapshot)
                _aiChatMessages.value = _aiChatMessages.value + AiChatMessage(sender = "Gemini", text = response)
            } catch (e: Exception) {
                val fallback = EdgeAiCoachEngine.generateEdgeCoaching(promptText, liveSnapshot)
                _aiChatMessages.value = _aiChatMessages.value + AiChatMessage(sender = "Gemini", text = fallback)
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    // ========================================================================
    // BACKUP & RESTORE ACTIONS
    // ========================================================================

    fun exportBackupJson() {
        viewModelScope.launch(exceptionHandler) {
            val json = repository.exportUserDataJson()
            _backupJsonString.value = json
        }
    }

    fun clearBackupJson() {
        _backupJsonString.value = null
    }

    fun restoreFromJson(jsonContent: String) {
        viewModelScope.launch(exceptionHandler) {
            val result = repository.restoreUserDataFromJson(jsonContent)
            if (result.isSuccess) {
                _restoreMessage.value = "Data verified & restored from backup!"
            } else {
                _restoreMessage.value = "Restore failed: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun clearRestoreMessage() {
        _restoreMessage.value = null
    }

    // ========================================================================
    // NAVIGATION, AUTH & BIDIRECTIONAL CLOUD SYNC
    // ========================================================================

    fun navigateTo(module: AppModule) {
        _currentModule.value = module
    }

    fun login(email: String) {
        _userEmail.value = email
        _isLoggedIn.value = true
        _currentModule.value = AppModule.DASHBOARD
        triggerCloudSync()
    }

    fun triggerCloudSync() {
        viewModelScope.launch(exceptionHandler) {
            _syncStatus.value = "Syncing"
            val result = repository.syncUserCloudDataBidirectional(_userEmail.value)
            result.fold(
                onSuccess = { report ->
                    _syncStatus.value = "Synced"
                    _syncReport.value = report
                    _lastSyncedTimestamp.value = report.syncedAt
                },
                onFailure = {
                    _syncStatus.value = "Error"
                }
            )
        }
    }

    fun logout() {
        _isLoggedIn.value = false
        _currentModule.value = AppModule.AUTH
    }

    fun clearAllUserData() {
        viewModelScope.launch(exceptionHandler) {
            repository.deleteAllUserData()
        }
    }

    // ========================================================================
    // DOMAIN ENTITY CRUD MUTATIONS
    // ========================================================================

    fun addCategory(name: String, colorHex: String = "#3B82F6", iconName: String = "Folder") {
        viewModelScope.launch(exceptionHandler) {
            repository.insertCategory(CategoryEntity(name = name, colorHex = colorHex, iconName = iconName))
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch(exceptionHandler) {
            repository.deleteCategory(category)
        }
    }

    fun addTask(title: String, category: String, priority: String, dueDate: Long, categoryId: Long = 1L) {
        viewModelScope.launch(exceptionHandler) {
            repository.insertTask(
                TaskEntity(title = title, category = category, priority = priority, dueDate = dueDate, categoryId = categoryId)
            )
        }
    }

    fun toggleTaskCompletion(task: TaskEntity) {
        viewModelScope.launch(exceptionHandler) {
            val newlyCompleted = !task.isCompleted
            repository.updateTask(task.copy(isCompleted = newlyCompleted))
            if (newlyCompleted) {
                val xpGained = when (task.priority.lowercase()) {
                    "high" -> 100
                    "medium" -> 50
                    "low" -> 25
                    else -> 50
                }
                grantXp(xpGained, "Quest Cleared: \"${task.title}\"")
            }
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch(exceptionHandler) {
            repository.deleteTask(task)
        }
    }

    fun addHabit(name: String, category: String, frequency: String) {
        viewModelScope.launch(exceptionHandler) {
            repository.insertHabit(HabitEntity(name = name, category = category, frequency = frequency))
        }
    }

    fun toggleHabitToday(habit: HabitEntity) {
        viewModelScope.launch(exceptionHandler) {
            val newCompleted = !habit.isCompletedToday
            val newStreak = if (newCompleted) habit.streakCount + 1 else maxOf(0, habit.streakCount - 1)
            repository.updateHabit(
                habit.copy(
                    isCompletedToday = newCompleted,
                    streakCount = newStreak,
                    lastCompletedDate = if (newCompleted) System.currentTimeMillis() else habit.lastCompletedDate
                )
            )
            if (newCompleted) {
                val xpGained = 50 + (newStreak * 5)
                grantXp(xpGained, "Daily Duty Cleared: \"${habit.name}\"")
            }
        }
    }

    fun deleteHabit(habit: HabitEntity) {
        viewModelScope.launch(exceptionHandler) {
            repository.deleteHabit(habit)
        }
    }

    fun addExpense(title: String, amount: Double, type: String, category: String, notes: String) {
        viewModelScope.launch(exceptionHandler) {
            repository.insertExpense(
                ExpenseEntity(title = title, amount = amount, type = type, category = category, notes = notes)
            )
        }
    }

    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch(exceptionHandler) {
            repository.deleteExpense(expense)
        }
    }

    fun addWaterIntake(amountMl: Int) {
        viewModelScope.launch(exceptionHandler) {
            val current = latestHealthLog.value ?: HealthLogEntity()
            val updated = current.copy(waterIntakeMl = current.waterIntakeMl + amountMl)
            repository.insertHealthLog(updated)
        }
    }

    fun syncPedometerSteps(additionalSteps: Int) {
        viewModelScope.launch(exceptionHandler) {
            val current = latestHealthLog.value ?: HealthLogEntity()
            val updatedSteps = current.stepsCount + additionalSteps.coerceAtLeast(0)
            val burned = VitalityAnalytics.estimateCaloriesBurned(
                stepsCount = updatedSteps,
                workoutDurationMins = current.workoutDurationMins,
                weightKg = current.weightKg
            )
            repository.insertHealthLog(current.copy(stepsCount = updatedSteps, caloriesBurned = burned))
        }
    }

    fun logHealthMetrics(weightKg: Float, heightCm: Float, sleepHours: Float, steps: Int, mood: String, workoutMins: Int) {
        viewModelScope.launch(exceptionHandler) {
            val current = latestHealthLog.value ?: HealthLogEntity()
            val burned = VitalityAnalytics.estimateCaloriesBurned(steps, workoutMins, weightKg)
            val updated = current.copy(
                weightKg = weightKg,
                heightCm = heightCm,
                sleepHours = sleepHours,
                stepsCount = steps,
                caloriesBurned = burned,
                mood = mood,
                workoutDurationMins = workoutMins
            )
            repository.insertHealthLog(updated)
        }
    }

    fun addJournalEntry(title: String, content: String, moodTag: String, tags: String) {
        viewModelScope.launch(exceptionHandler) {
            repository.insertJournal(
                JournalEntity(title = title, content = content, moodTag = moodTag, tags = tags)
            )
        }
    }

    fun deleteJournal(journal: JournalEntity) {
        viewModelScope.launch(exceptionHandler) {
            repository.deleteJournal(journal)
        }
    }

    fun addNote(title: String, content: String, isChecklist: Boolean, folder: String) {
        viewModelScope.launch(exceptionHandler) {
            repository.insertNote(
                NoteEntity(title = title, content = content, isChecklist = isChecklist, folder = folder)
            )
        }
    }

    fun toggleNotePin(note: NoteEntity) {
        viewModelScope.launch(exceptionHandler) {
            repository.updateNote(note.copy(isPinned = !note.isPinned, updatedAt = System.currentTimeMillis()))
        }
    }

    fun deleteNote(note: NoteEntity) {
        viewModelScope.launch(exceptionHandler) {
            repository.deleteNote(note)
        }
    }

    fun addGoal(title: String, description: String, category: String, type: String, milestones: String) {
        viewModelScope.launch(exceptionHandler) {
            repository.insertGoal(
                GoalEntity(title = title, description = description, category = category, type = type, milestones = milestones)
            )
        }
    }

    fun updateGoalProgress(goal: GoalEntity, progress: Int) {
        viewModelScope.launch(exceptionHandler) {
            repository.updateGoal(goal.copy(progressPercent = progress.coerceIn(0, 100)))
        }
    }

    fun deleteGoal(goal: GoalEntity) {
        viewModelScope.launch(exceptionHandler) {
            repository.deleteGoal(goal)
        }
    }

    fun addCalendarEvent(title: String, description: String, startTime: Long, eventType: String, location: String) {
        viewModelScope.launch(exceptionHandler) {
            repository.insertCalendarEvent(
                CalendarEventEntity(title = title, description = description, startTime = startTime, eventType = eventType, location = location)
            )
        }
    }

    fun deleteCalendarEvent(event: CalendarEventEntity) {
        viewModelScope.launch(exceptionHandler) {
            repository.deleteCalendarEvent(event)
        }
    }

    fun updateProfileNameAndGoal(name: String, budget: Double, waterGoal: Int, darkMode: Boolean) {
        viewModelScope.launch(exceptionHandler) {
            val current = userProfile.value ?: UserProfileEntity()
            repository.updateProfile(
                current.copy(name = name, monthlyBudget = budget, dailyWaterGoalMl = waterGoal, isDarkMode = darkMode)
            )
        }
    }
}
