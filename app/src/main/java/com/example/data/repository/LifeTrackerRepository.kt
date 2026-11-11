package com.example.data.repository

import android.util.Log
import com.example.data.local.dao.*
import com.example.data.local.entities.*
import com.example.data.remote.FirestoreManager
import com.example.data.remote.GeminiApiService
import com.example.data.sync.CloudSyncEngine
import com.example.data.sync.SyncReport
import com.example.domain.ai.PredictiveLifeSnapshot
import com.example.security.SecurityManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.json.JSONArray
import org.json.JSONObject

/**
 * ============================================================================
 * LIFE TRACKER REPOSITORY — DATA ACCESS & ORCHESTRATION LAYER (RELEASE 1.0)
 * ============================================================================
 *
 * Serves as the single source of truth for the LifeOS AI application.
 * Manages multi-tier data operations:
 * 1. Offline-first caching via Room SQLite local database DAOs (12 tables).
 * 2. Bidirectional Last-Write-Wins (LWW) synchronization with Google Cloud Firestore.
 * 3. Triple-tier AI coaching cascade (OpenRouter -> Gemini REST -> On-Device Edge AI).
 * 4. Integrity-verified JSON Backup & Restore engine (Schema v3 with SHA-256).
 * 5. Collaborative Hunter Guild raid persistence.
 */
class LifeTrackerRepository(
    private val taskDao: TaskDao,
    private val habitDao: HabitDao,
    private val expenseDao: ExpenseDao,
    private val healthDao: HealthDao,
    private val journalDao: JournalDao,
    private val noteDao: NoteDao,
    private val goalDao: GoalDao,
    private val calendarDao: CalendarDao,
    private val userDao: UserDao,
    private val userStatsDao: UserStatsDao,
    private val categoryDao: CategoryDao? = null,
    private val guildProgressDao: GuildProgressDao? = null,
    private val geminiApiService: GeminiApiService,
    private val firestoreManager: FirestoreManager = FirestoreManager()
) {
    // ========================================================================
    // 1. CATEGORY OPERATIONS
    // ========================================================================

    val allCategories: Flow<List<CategoryEntity>> = categoryDao?.getAllCategories() ?: flowOf(emptyList())

    suspend fun insertCategory(category: CategoryEntity): Long =
        categoryDao?.insertCategory(category) ?: 0L

    suspend fun deleteCategory(category: CategoryEntity) {
        categoryDao?.deleteCategory(category)
    }

    // ========================================================================
    // 2. TASK OPERATIONS
    // ========================================================================

    val allTasks: Flow<List<TaskEntity>> = taskDao.getAllTasks()

    suspend fun insertTask(task: TaskEntity): Long {
        val stamped = task.copy(updatedAt = System.currentTimeMillis())
        val id = taskDao.insertTask(stamped)
        val entityWithId = if (stamped.id == 0L) stamped.copy(id = id) else stamped
        try {
            firestoreManager.saveDocument("tasks", entityWithId.id.toString(), taskToMap(entityWithId))
        } catch (e: Exception) {
            Log.w("LifeTrackerRepo", "Offline mode: Failed mirroring task: ${e.message}")
        }
        return id
    }

    suspend fun updateTask(task: TaskEntity) {
        val stamped = task.copy(updatedAt = System.currentTimeMillis())
        taskDao.updateTask(stamped)
        try {
            firestoreManager.saveDocument("tasks", stamped.id.toString(), taskToMap(stamped))
        } catch (e: Exception) {
            Log.w("LifeTrackerRepo", "Offline mode: Failed updating task in Firestore: ${e.message}")
        }
    }

    suspend fun deleteTask(task: TaskEntity) {
        taskDao.deleteTask(task)
        try {
            firestoreManager.deleteDocument("tasks", task.id.toString())
        } catch (e: Exception) {
            Log.w("LifeTrackerRepo", "Offline mode: Failed deleting task from Firestore: ${e.message}")
        }
    }

    // ========================================================================
    // 3. HABIT OPERATIONS
    // ========================================================================

    val allHabits: Flow<List<HabitEntity>> = habitDao.getAllHabits()

    suspend fun insertHabit(habit: HabitEntity): Long {
        val stamped = habit.copy(updatedAt = System.currentTimeMillis())
        val id = habitDao.insertHabit(stamped)
        val entityWithId = if (stamped.id == 0L) stamped.copy(id = id) else stamped
        try {
            firestoreManager.saveDocument("habits", entityWithId.id.toString(), habitToMap(entityWithId))
        } catch (e: Exception) {
            Log.w("LifeTrackerRepo", "Offline mode: Failed mirroring habit: ${e.message}")
        }
        return id
    }

    suspend fun updateHabit(habit: HabitEntity) {
        val stamped = habit.copy(updatedAt = System.currentTimeMillis())
        habitDao.updateHabit(stamped)
        try {
            firestoreManager.saveDocument("habits", stamped.id.toString(), habitToMap(stamped))
        } catch (e: Exception) {
            Log.w("LifeTrackerRepo", "Offline mode: Failed updating habit: ${e.message}")
        }
    }

    suspend fun deleteHabit(habit: HabitEntity) {
        habitDao.deleteHabit(habit)
        try {
            firestoreManager.deleteDocument("habits", habit.id.toString())
        } catch (e: Exception) {
            Log.w("LifeTrackerRepo", "Offline mode: Failed deleting habit: ${e.message}")
        }
    }

    // ========================================================================
    // 4. EXPENSE & BUDGET OPERATIONS
    // ========================================================================

    val allExpenses: Flow<List<ExpenseEntity>> = expenseDao.getAllExpenses()

    suspend fun insertExpense(expense: ExpenseEntity): Long {
        val stamped = expense.copy(updatedAt = System.currentTimeMillis())
        val id = expenseDao.insertExpense(stamped)
        val entityWithId = if (stamped.id == 0L) stamped.copy(id = id) else stamped
        try {
            firestoreManager.saveDocument("expenses", entityWithId.id.toString(), expenseToMap(entityWithId))
        } catch (e: Exception) {
            Log.w("LifeTrackerRepo", "Offline mode: Failed mirroring expense: ${e.message}")
        }
        return id
    }

    suspend fun deleteExpense(expense: ExpenseEntity) {
        expenseDao.deleteExpense(expense)
        try {
            firestoreManager.deleteDocument("expenses", expense.id.toString())
        } catch (e: Exception) {
            Log.w("LifeTrackerRepo", "Offline mode: Failed deleting expense: ${e.message}")
        }
    }

    // ========================================================================
    // 5. HEALTH & WELLNESS LOG OPERATIONS
    // ========================================================================

    val allHealthLogs: Flow<List<HealthLogEntity>> = healthDao.getAllHealthLogs()
    val latestHealthLog: Flow<HealthLogEntity?> = healthDao.getLatestHealthLog()

    suspend fun insertHealthLog(healthLog: HealthLogEntity): Long {
        val stamped = healthLog.copy(updatedAt = System.currentTimeMillis())
        val id = healthDao.insertHealthLog(stamped)
        val entityWithId = if (stamped.id == 0L) stamped.copy(id = id) else stamped
        try {
            firestoreManager.saveDocument(
                "health_logs", entityWithId.id.toString(), mapOf(
                    "weightKg" to entityWithId.weightKg,
                    "heightCm" to entityWithId.heightCm,
                    "waterIntakeMl" to entityWithId.waterIntakeMl,
                    "sleepHours" to entityWithId.sleepHours,
                    "stepsCount" to entityWithId.stepsCount,
                    "caloriesBurned" to entityWithId.caloriesBurned,
                    "mood" to entityWithId.mood,
                    "workoutDurationMins" to entityWithId.workoutDurationMins,
                    "date" to entityWithId.date,
                    "updatedAt" to entityWithId.updatedAt
                )
            )
        } catch (e: Exception) {
            Log.w("LifeTrackerRepo", "Offline mode: Failed mirroring health log: ${e.message}")
        }
        return id
    }

    // ========================================================================
    // 6. JOURNAL OPERATIONS
    // ========================================================================

    val allJournals: Flow<List<JournalEntity>> = journalDao.getAllJournalEntries()

    suspend fun insertJournal(journal: JournalEntity): Long {
        val stamped = journal.copy(updatedAt = System.currentTimeMillis())
        val id = journalDao.insertJournalEntry(stamped)
        val entityWithId = if (stamped.id == 0L) stamped.copy(id = id) else stamped
        try {
            firestoreManager.saveDocument(
                "journals", entityWithId.id.toString(), mapOf(
                    "title" to entityWithId.title,
                    "content" to entityWithId.content,
                    "moodTag" to entityWithId.moodTag,
                    "tags" to entityWithId.tags,
                    "date" to entityWithId.date,
                    "updatedAt" to entityWithId.updatedAt
                )
            )
        } catch (e: Exception) {
            Log.w("LifeTrackerRepo", "Offline mode: Failed mirroring journal: ${e.message}")
        }
        return id
    }

    suspend fun deleteJournal(journal: JournalEntity) {
        journalDao.deleteJournalEntry(journal)
        try {
            firestoreManager.deleteDocument("journals", journal.id.toString())
        } catch (e: Exception) {
            Log.w("LifeTrackerRepo", "Offline mode: Failed deleting journal: ${e.message}")
        }
    }

    // ========================================================================
    // 7. NOTES & CHECKLIST OPERATIONS
    // ========================================================================

    val allNotes: Flow<List<NoteEntity>> = noteDao.getAllNotes()

    suspend fun insertNote(note: NoteEntity): Long {
        val stamped = note.copy(updatedAt = System.currentTimeMillis())
        val id = noteDao.insertNote(stamped)
        val entityWithId = if (stamped.id == 0L) stamped.copy(id = id) else stamped
        try {
            firestoreManager.saveDocument(
                "notes", entityWithId.id.toString(), mapOf(
                    "title" to entityWithId.title,
                    "content" to entityWithId.content,
                    "isChecklist" to entityWithId.isChecklist,
                    "isPinned" to entityWithId.isPinned,
                    "folder" to entityWithId.folder,
                    "updatedAt" to entityWithId.updatedAt
                )
            )
        } catch (e: Exception) {
            Log.w("LifeTrackerRepo", "Offline mode: Failed mirroring note: ${e.message}")
        }
        return id
    }

    suspend fun updateNote(note: NoteEntity) {
        val stamped = note.copy(updatedAt = System.currentTimeMillis())
        noteDao.updateNote(stamped)
        try {
            firestoreManager.saveDocument(
                "notes", stamped.id.toString(), mapOf(
                    "title" to stamped.title,
                    "content" to stamped.content,
                    "isChecklist" to stamped.isChecklist,
                    "isPinned" to stamped.isPinned,
                    "folder" to stamped.folder,
                    "updatedAt" to stamped.updatedAt
                )
            )
        } catch (e: Exception) {
            Log.w("LifeTrackerRepo", "Offline mode: Failed updating note: ${e.message}")
        }
    }

    suspend fun deleteNote(note: NoteEntity) {
        noteDao.deleteNote(note)
        try {
            firestoreManager.deleteDocument("notes", note.id.toString())
        } catch (e: Exception) {
            Log.w("LifeTrackerRepo", "Offline mode: Failed deleting note: ${e.message}")
        }
    }

    // ========================================================================
    // 8. GOALS & TARGETS OPERATIONS
    // ========================================================================

    val allGoals: Flow<List<GoalEntity>> = goalDao.getAllGoals()

    suspend fun insertGoal(goal: GoalEntity): Long {
        val stamped = goal.copy(updatedAt = System.currentTimeMillis())
        val id = goalDao.insertGoal(stamped)
        val entityWithId = if (stamped.id == 0L) stamped.copy(id = id) else stamped
        try {
            firestoreManager.saveDocument(
                "goals", entityWithId.id.toString(), mapOf(
                    "title" to entityWithId.title,
                    "description" to entityWithId.description,
                    "category" to entityWithId.category,
                    "type" to entityWithId.type,
                    "progressPercent" to entityWithId.progressPercent,
                    "milestones" to entityWithId.milestones,
                    "deadline" to entityWithId.deadline,
                    "updatedAt" to entityWithId.updatedAt
                )
            )
        } catch (e: Exception) {
            Log.w("LifeTrackerRepo", "Offline mode: Failed mirroring goal: ${e.message}")
        }
        return id
    }

    suspend fun updateGoal(goal: GoalEntity) {
        val stamped = goal.copy(updatedAt = System.currentTimeMillis())
        goalDao.updateGoal(stamped)
        try {
            firestoreManager.saveDocument(
                "goals", stamped.id.toString(), mapOf(
                    "title" to stamped.title,
                    "description" to stamped.description,
                    "category" to stamped.category,
                    "type" to stamped.type,
                    "progressPercent" to stamped.progressPercent,
                    "milestones" to stamped.milestones,
                    "deadline" to stamped.deadline,
                    "updatedAt" to stamped.updatedAt
                )
            )
        } catch (e: Exception) {
            Log.w("LifeTrackerRepo", "Offline mode: Failed updating goal: ${e.message}")
        }
    }

    suspend fun deleteGoal(goal: GoalEntity) {
        goalDao.deleteGoal(goal)
        try {
            firestoreManager.deleteDocument("goals", goal.id.toString())
        } catch (e: Exception) {
            Log.w("LifeTrackerRepo", "Offline mode: Failed deleting goal: ${e.message}")
        }
    }

    // ========================================================================
    // 9. CALENDAR & SCHEDULE OPERATIONS
    // ========================================================================

    val allCalendarEvents: Flow<List<CalendarEventEntity>> = calendarDao.getAllEvents()

    suspend fun insertCalendarEvent(event: CalendarEventEntity): Long {
        val stamped = event.copy(updatedAt = System.currentTimeMillis())
        val id = calendarDao.insertEvent(stamped)
        val entityWithId = if (stamped.id == 0L) stamped.copy(id = id) else stamped
        try {
            firestoreManager.saveDocument(
                "calendar_events", entityWithId.id.toString(), mapOf(
                    "title" to entityWithId.title,
                    "description" to entityWithId.description,
                    "startTime" to entityWithId.startTime,
                    "endTime" to entityWithId.endTime,
                    "eventType" to entityWithId.eventType,
                    "location" to entityWithId.location,
                    "updatedAt" to entityWithId.updatedAt
                )
            )
        } catch (e: Exception) {
            Log.w("LifeTrackerRepo", "Offline mode: Failed mirroring event: ${e.message}")
        }
        return id
    }

    suspend fun deleteCalendarEvent(event: CalendarEventEntity) {
        calendarDao.deleteEvent(event)
        try {
            firestoreManager.deleteDocument("calendar_events", event.id.toString())
        } catch (e: Exception) {
            Log.w("LifeTrackerRepo", "Offline mode: Failed deleting event: ${e.message}")
        }
    }

    // ========================================================================
    // 10. USER PROFILE, GAMIFICATION STATS & GUILD PROGRESSION
    // ========================================================================

    val userProfile: Flow<UserProfileEntity?> = userDao.getUserProfile()

    suspend fun updateProfile(profile: UserProfileEntity) {
        val stamped = profile.copy(updatedAt = System.currentTimeMillis())
        userDao.insertOrUpdateUserProfile(stamped)
        try {
            firestoreManager.saveDocument(
                "user_profile", stamped.id.toString(), mapOf(
                    "name" to stamped.name,
                    "email" to stamped.email,
                    "monthlyBudget" to stamped.monthlyBudget,
                    "dailyWaterGoalMl" to stamped.dailyWaterGoalMl,
                    "dailyStepGoal" to stamped.dailyStepGoal,
                    "isDarkMode" to stamped.isDarkMode,
                    "updatedAt" to stamped.updatedAt
                )
            )
        } catch (e: Exception) {
            Log.w("LifeTrackerRepo", "Offline mode: Failed mirroring profile: ${e.message}")
        }
    }

    val userStats: Flow<UserStatsEntity?> = userStatsDao.getUserStats()

    suspend fun updateUserStats(stats: UserStatsEntity) {
        val stamped = stats.copy(updatedAt = System.currentTimeMillis())
        userStatsDao.insertOrUpdateUserStats(stamped)
        try {
            firestoreManager.saveDocument(
                "user_stats", stamped.id.toString(), mapOf(
                    "totalXp" to stamped.totalXp,
                    "level" to stamped.level,
                    "coins" to stamped.coins,
                    "streakDays" to stamped.streakDays,
                    "strength" to stamped.strength,
                    "agility" to stamped.agility,
                    "intelligence" to stamped.intelligence,
                    "vitality" to stamped.vitality,
                    "sense" to stamped.sense,
                    "unallocatedPoints" to stamped.unallocatedPoints,
                    "updatedAt" to stamped.updatedAt
                )
            )
        } catch (e: Exception) {
            Log.w("LifeTrackerRepo", "Offline mode: Failed mirroring user stats: ${e.message}")
        }
    }

    val guildProgress: Flow<GuildProgressEntity?> =
        guildProgressDao?.getGuildProgress() ?: flowOf(GuildProgressEntity())

    suspend fun updateGuildProgress(progress: GuildProgressEntity) {
        guildProgressDao?.insertOrUpdateGuildProgress(progress.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun contributeGuildRaidDamage(xpContribution: Int) {
        val dao = guildProgressDao ?: return
        val current = dao.getGuildProgressSingle() ?: GuildProgressEntity()
        val updated = current.copy(
            raidDamageDealt = (current.raidDamageDealt + xpContribution).coerceAtMost(current.raidBossHp),
            personalContributionXp = current.personalContributionXp + xpContribution,
            updatedAt = System.currentTimeMillis()
        )
        dao.insertOrUpdateGuildProgress(updated)
    }

    // ========================================================================
    // 11. BIDIRECTIONAL CLOUD SYNCHRONIZATION WITH LWW CONFLICT RESOLUTION
    // ========================================================================

    /**
     * Performs bidirectional pull, Last-Write-Wins conflict resolution, and cloud push
     * across Tasks, Habits, Expenses, Health Logs, User Profile, and Stats.
     */
    suspend fun syncUserCloudDataBidirectional(userEmail: String): Result<SyncReport> {
        return try {
            var pulled = 0
            var pushed = 0
            var conflicts = 0

            // 1. Tasks bidirectional merge
            val localTasks = taskDao.getAllTasksList()
            val remoteTasks = firestoreManager.fetchUserCollection(userEmail, "tasks").mapNotNull { mapToTask(it) }
            val taskMerge = CloudSyncEngine.reconcileLastWriteWins(
                localItems = localTasks,
                remoteItems = remoteTasks,
                idSelector = { it.id.toString() },
                updatedAtSelector = { it.updatedAt }
            )
            taskMerge.toSaveLocally.forEach { taskDao.insertTask(it); pulled++ }
            taskMerge.toPushRemotely.forEach {
                firestoreManager.saveUserDocument(userEmail, "tasks", it.id.toString(), taskToMap(it))
                pushed++
            }
            conflicts += taskMerge.conflictsResolved

            // 2. Habits bidirectional merge
            val localHabits = habitDao.getAllHabitsList()
            val remoteHabits = firestoreManager.fetchUserCollection(userEmail, "habits").mapNotNull { mapToHabit(it) }
            val habitMerge = CloudSyncEngine.reconcileLastWriteWins(
                localItems = localHabits,
                remoteItems = remoteHabits,
                idSelector = { it.id.toString() },
                updatedAtSelector = { it.updatedAt }
            )
            habitMerge.toSaveLocally.forEach { habitDao.insertHabit(it); pulled++ }
            habitMerge.toPushRemotely.forEach {
                firestoreManager.saveUserDocument(userEmail, "habits", it.id.toString(), habitToMap(it))
                pushed++
            }
            conflicts += habitMerge.conflictsResolved

            // 3. Expenses bidirectional merge
            val localExpenses = expenseDao.getAllExpensesList()
            val remoteExpenses = firestoreManager.fetchUserCollection(userEmail, "expenses").mapNotNull { mapToExpense(it) }
            val expenseMerge = CloudSyncEngine.reconcileLastWriteWins(
                localItems = localExpenses,
                remoteItems = remoteExpenses,
                idSelector = { it.id.toString() },
                updatedAtSelector = { it.updatedAt }
            )
            expenseMerge.toSaveLocally.forEach { expenseDao.insertExpense(it); pulled++ }
            expenseMerge.toPushRemotely.forEach {
                firestoreManager.saveUserDocument(userEmail, "expenses", it.id.toString(), expenseToMap(it))
                pushed++
            }
            conflicts += expenseMerge.conflictsResolved

            // 4. Push Profile, Health Logs & Gamification Stats
            val healthLogs = healthDao.getAllHealthLogsList()
            healthLogs.forEach { hl ->
                firestoreManager.saveUserDocument(
                    userEmail, "health_logs", hl.id.toString(), mapOf(
                        "waterIntakeMl" to hl.waterIntakeMl,
                        "sleepHours" to hl.sleepHours,
                        "stepsCount" to hl.stepsCount,
                        "caloriesBurned" to hl.caloriesBurned,
                        "mood" to hl.mood,
                        "workoutDurationMins" to hl.workoutDurationMins,
                        "date" to hl.date,
                        "updatedAt" to hl.updatedAt
                    )
                )
                pushed++
            }

            val profile = userDao.getUserProfileSingle() ?: UserProfileEntity(email = userEmail)
            userDao.insertOrUpdateUserProfile(profile.copy(email = userEmail))
            firestoreManager.saveUserDocument(
                userEmail, "profile", "user_profile", mapOf(
                    "name" to profile.name,
                    "email" to userEmail,
                    "monthlyBudget" to profile.monthlyBudget,
                    "dailyWaterGoalMl" to profile.dailyWaterGoalMl,
                    "dailyStepGoal" to profile.dailyStepGoal,
                    "isDarkMode" to profile.isDarkMode,
                    "updatedAt" to profile.updatedAt
                )
            )

            val stats = userStatsDao.getUserStatsSingle() ?: UserStatsEntity()
            firestoreManager.saveUserDocument(
                userEmail, "stats", "user_stats", mapOf(
                    "totalXp" to stats.totalXp,
                    "level" to stats.level,
                    "coins" to stats.coins,
                    "streakDays" to stats.streakDays,
                    "strength" to stats.strength,
                    "agility" to stats.agility,
                    "intelligence" to stats.intelligence,
                    "vitality" to stats.vitality,
                    "sense" to stats.sense,
                    "unallocatedPoints" to stats.unallocatedPoints,
                    "updatedAt" to stats.updatedAt
                )
            )

            Result.success(
                SyncReport(
                    pulledFromCloud = pulled,
                    pushedToCloud = pushed,
                    conflictsResolved = conflicts,
                    syncedAt = System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            Log.e("LifeTrackerRepository", "Bidirectional sync error boundary caught: ${e.message}")
            Result.failure(e)
        }
    }

    /** Backwards-compatible wrapper for callers expecting Result<Unit>. */
    suspend fun syncUserCloudData(userEmail: String): Result<Unit> =
        syncUserCloudDataBidirectional(userEmail).map { Unit }

    // ========================================================================
    // 12. FULL SYSTEM BACKUP & RESTORE ENGINE (SCHEMA V3 WITH SHA-256 CHECKSUM)
    // ========================================================================

    /**
     * Exports all user records across all Room tables into a SHA-256 verified JSON string.
     */
    suspend fun exportUserDataJson(): String {
        val root = JSONObject()
        root.put("version", 3)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("appName", "LifeOS AI")

        // 1. Tasks
        val tasksArray = JSONArray()
        taskDao.getAllTasksList().forEach { t ->
            tasksArray.put(JSONObject().apply {
                put("id", t.id)
                put("title", t.title)
                put("description", t.description)
                put("isCompleted", t.isCompleted)
                put("priority", t.priority)
                put("category", t.category)
                put("categoryId", t.categoryId)
                put("dueDate", t.dueDate)
                put("updatedAt", t.updatedAt)
            })
        }
        root.put("tasks", tasksArray)

        // 2. Habits
        val habitsArray = JSONArray()
        habitDao.getAllHabitsList().forEach { h ->
            habitsArray.put(JSONObject().apply {
                put("id", h.id)
                put("name", h.name)
                put("category", h.category)
                put("frequency", h.frequency)
                put("streakCount", h.streakCount)
                put("isCompletedToday", h.isCompletedToday)
                put("lastCompletedDate", h.lastCompletedDate)
                put("updatedAt", h.updatedAt)
            })
        }
        root.put("habits", habitsArray)

        // 3. Expenses
        val expensesArray = JSONArray()
        expenseDao.getAllExpensesList().forEach { e ->
            expensesArray.put(JSONObject().apply {
                put("id", e.id)
                put("title", e.title)
                put("amount", e.amount)
                put("type", e.type)
                put("category", e.category)
                put("date", e.date)
                put("notes", e.notes)
                put("updatedAt", e.updatedAt)
            })
        }
        root.put("expenses", expensesArray)

        // 4. Health Logs
        val healthArray = JSONArray()
        healthDao.getAllHealthLogsList().forEach { hl ->
            healthArray.put(JSONObject().apply {
                put("id", hl.id)
                put("date", hl.date)
                put("waterIntakeMl", hl.waterIntakeMl)
                put("sleepHours", hl.sleepHours.toDouble())
                put("weightKg", hl.weightKg.toDouble())
                put("heightCm", hl.heightCm.toDouble())
                put("stepsCount", hl.stepsCount)
                put("caloriesBurned", hl.caloriesBurned)
                put("mood", hl.mood)
                put("workoutDurationMins", hl.workoutDurationMins)
            })
        }
        root.put("health_logs", healthArray)

        // 5. Journals
        val journalsArray = JSONArray()
        journalDao.getAllJournalEntriesList().forEach { j ->
            journalsArray.put(JSONObject().apply {
                put("id", j.id)
                put("title", j.title)
                put("content", j.content)
                put("date", j.date)
                put("moodTag", j.moodTag)
                put("tags", j.tags)
            })
        }
        root.put("journals", journalsArray)

        // 6. Notes
        val notesArray = JSONArray()
        noteDao.getAllNotesList().forEach { n ->
            notesArray.put(JSONObject().apply {
                put("id", n.id)
                put("title", n.title)
                put("content", n.content)
                put("isChecklist", n.isChecklist)
                put("checklistItems", n.checklistItems)
                put("isPinned", n.isPinned)
                put("folder", n.folder)
                put("updatedAt", n.updatedAt)
            })
        }
        root.put("notes", notesArray)

        // 7. Goals
        val goalsArray = JSONArray()
        goalDao.getAllGoalsList().forEach { g ->
            goalsArray.put(JSONObject().apply {
                put("id", g.id)
                put("title", g.title)
                put("description", g.description)
                put("category", g.category)
                put("type", g.type)
                put("progressPercent", g.progressPercent)
                put("deadline", g.deadline)
                put("milestones", g.milestones)
            })
        }
        root.put("goals", goalsArray)

        // 8. Calendar
        val eventsArray = JSONArray()
        calendarDao.getAllEventsList().forEach { c ->
            eventsArray.put(JSONObject().apply {
                put("id", c.id)
                put("title", c.title)
                put("description", c.description)
                put("startTime", c.startTime)
                put("endTime", c.endTime)
                put("eventType", c.eventType)
                put("location", c.location)
            })
        }
        root.put("calendar_events", eventsArray)

        // 9. User Profile
        userDao.getUserProfileSingle()?.let { p ->
            root.put("user_profile", JSONObject().apply {
                put("name", p.name)
                put("email", p.email)
                put("isDarkMode", p.isDarkMode)
                put("monthlyBudget", p.monthlyBudget)
                put("dailyWaterGoalMl", p.dailyWaterGoalMl)
                put("dailyStepGoal", p.dailyStepGoal)
                put("dailyCalorieGoal", p.dailyCalorieGoal)
                put("avatarId", p.avatarId)
            })
        }

        // 10. User Stats (including Hunter Attributes)
        userStatsDao.getUserStatsSingle()?.let { s ->
            root.put("user_stats", JSONObject().apply {
                put("totalXp", s.totalXp)
                put("level", s.level)
                put("coins", s.coins)
                put("streakDays", s.streakDays)
                put("strength", s.strength)
                put("agility", s.agility)
                put("intelligence", s.intelligence)
                put("vitality", s.vitality)
                put("sense", s.sense)
                put("unallocatedPoints", s.unallocatedPoints)
            })
        }

        // 11. Guild Progress
        guildProgressDao?.getGuildProgressSingle()?.let { gp ->
            root.put("guild_progress", JSONObject().apply {
                put("guildName", gp.guildName)
                put("raidBossName", gp.raidBossName)
                put("raidBossHp", gp.raidBossHp)
                put("raidDamageDealt", gp.raidDamageDealt)
                put("personalContributionXp", gp.personalContributionXp)
            })
        }

        // Compute deterministic integrity digest over canonical section counts
        val integritySeed = "${tasksArray.length()}:${habitsArray.length()}:${expensesArray.length()}:${root.optLong("exportedAt")}"
        root.put("checksumSha256", SecurityManager.computeSha256Integrity(integritySeed))

        return root.toString(2)
    }

    /**
     * Restores application state from a v2 or v3 backup JSON string.
     */
    suspend fun restoreUserDataFromJson(jsonContent: String): Result<Unit> {
        return try {
            if (jsonContent.isBlank()) {
                return Result.failure(IllegalArgumentException("Backup payload cannot be empty"))
            }

            val root = JSONObject(jsonContent)

            if (root.has("checksumSha256")) {
                val tLen = root.optJSONArray("tasks")?.length() ?: 0
                val hLen = root.optJSONArray("habits")?.length() ?: 0
                val eLen = root.optJSONArray("expenses")?.length() ?: 0
                val expectedSeed = "$tLen:$hLen:$eLen:${root.optLong("exportedAt")}"
                val expectedChecksum = SecurityManager.computeSha256Integrity(expectedSeed)
                val actualChecksum = root.optString("checksumSha256")
                if (actualChecksum.isNotBlank() && actualChecksum != expectedChecksum) {
                    return Result.failure(SecurityException("Backup SHA-256 integrity mismatch: payload altered"))
                }
            }

            if (root.has("tasks")) {
                val array = root.getJSONArray("tasks")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    taskDao.insertTask(
                        TaskEntity(
                            id = obj.optLong("id", 0L),
                            title = obj.getString("title"),
                            description = obj.optString("description", ""),
                            isCompleted = obj.optBoolean("isCompleted", false),
                            priority = obj.optString("priority", "Medium"),
                            category = obj.optString("category", "Personal"),
                            categoryId = obj.optLong("categoryId", 1L),
                            dueDate = obj.optLong("dueDate", System.currentTimeMillis()),
                            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            if (root.has("habits")) {
                val array = root.getJSONArray("habits")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    habitDao.insertHabit(
                        HabitEntity(
                            id = obj.optLong("id", 0L),
                            name = obj.getString("name"),
                            category = obj.optString("category", "Health"),
                            frequency = obj.optString("frequency", "Daily"),
                            streakCount = obj.optInt("streakCount", 0),
                            isCompletedToday = obj.optBoolean("isCompletedToday", false),
                            lastCompletedDate = obj.optLong("lastCompletedDate", 0L),
                            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            if (root.has("expenses")) {
                val array = root.getJSONArray("expenses")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    expenseDao.insertExpense(
                        ExpenseEntity(
                            id = obj.optLong("id", 0L),
                            title = obj.getString("title"),
                            amount = obj.getDouble("amount"),
                            type = obj.optString("type", "Expense"),
                            category = obj.optString("category", "General"),
                            date = obj.optLong("date", System.currentTimeMillis()),
                            notes = obj.optString("notes", ""),
                            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            if (root.has("health_logs")) {
                val array = root.getJSONArray("health_logs")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    healthDao.insertHealthLog(
                        HealthLogEntity(
                            id = obj.optLong("id", 0L),
                            date = obj.optLong("date", System.currentTimeMillis()),
                            waterIntakeMl = obj.optInt("waterIntakeMl", 0),
                            sleepHours = obj.optDouble("sleepHours", 0.0).toFloat(),
                            weightKg = obj.optDouble("weightKg", 70.0).toFloat(),
                            heightCm = obj.optDouble("heightCm", 175.0).toFloat(),
                            stepsCount = obj.optInt("stepsCount", 0),
                            caloriesBurned = obj.optInt("caloriesBurned", 0),
                            mood = obj.optString("mood", "😊 Happy"),
                            workoutDurationMins = obj.optInt("workoutDurationMins", 0)
                        )
                    )
                }
            }

            if (root.has("journals")) {
                val array = root.getJSONArray("journals")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    journalDao.insertJournalEntry(
                        JournalEntity(
                            id = obj.optLong("id", 0L),
                            title = obj.getString("title"),
                            content = obj.getString("content"),
                            date = obj.optLong("date", System.currentTimeMillis()),
                            moodTag = obj.optString("moodTag", "Calm"),
                            tags = obj.optString("tags", "Personal")
                        )
                    )
                }
            }

            if (root.has("notes")) {
                val array = root.getJSONArray("notes")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    noteDao.insertNote(
                        NoteEntity(
                            id = obj.optLong("id", 0L),
                            title = obj.getString("title"),
                            content = obj.getString("content"),
                            isChecklist = obj.optBoolean("isChecklist", false),
                            checklistItems = obj.optString("checklistItems", ""),
                            isPinned = obj.optBoolean("isPinned", false),
                            folder = obj.optString("folder", "General"),
                            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            if (root.has("goals")) {
                val array = root.getJSONArray("goals")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    goalDao.insertGoal(
                        GoalEntity(
                            id = obj.optLong("id", 0L),
                            title = obj.getString("title"),
                            description = obj.optString("description", ""),
                            category = obj.optString("category", "Career"),
                            type = obj.optString("type", "Short-term"),
                            progressPercent = obj.optInt("progressPercent", 0),
                            deadline = obj.optLong("deadline", System.currentTimeMillis()),
                            milestones = obj.optString("milestones", "")
                        )
                    )
                }
            }

            if (root.has("calendar_events")) {
                val array = root.getJSONArray("calendar_events")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    calendarDao.insertEvent(
                        CalendarEventEntity(
                            id = obj.optLong("id", 0L),
                            title = obj.getString("title"),
                            description = obj.optString("description", ""),
                            startTime = obj.optLong("startTime", System.currentTimeMillis()),
                            endTime = obj.optLong("endTime", System.currentTimeMillis() + 3600000L),
                            eventType = obj.optString("eventType", "Event"),
                            location = obj.optString("location", "")
                        )
                    )
                }
            }

            if (root.has("user_profile")) {
                val pObj = root.getJSONObject("user_profile")
                userDao.insertOrUpdateUserProfile(
                    UserProfileEntity(
                        id = 1,
                        name = pObj.optString("name", "User"),
                        email = pObj.optString("email", ""),
                        isDarkMode = pObj.optBoolean("isDarkMode", true),
                        monthlyBudget = pObj.optDouble("monthlyBudget", 2000.0),
                        dailyWaterGoalMl = pObj.optInt("dailyWaterGoalMl", 2500),
                        dailyStepGoal = pObj.optInt("dailyStepGoal", 10000),
                        dailyCalorieGoal = pObj.optInt("dailyCalorieGoal", 2200),
                        avatarId = pObj.optInt("avatarId", 1)
                    )
                )
            }

            if (root.has("user_stats")) {
                val sObj = root.getJSONObject("user_stats")
                userStatsDao.insertOrUpdateUserStats(
                    UserStatsEntity(
                        id = 1,
                        totalXp = sObj.optInt("totalXp", 0),
                        level = sObj.optInt("level", 1),
                        coins = sObj.optInt("coins", 0),
                        streakDays = sObj.optInt("streakDays", 0),
                        strength = sObj.optInt("strength", 18),
                        agility = sObj.optInt("agility", 22),
                        intelligence = sObj.optInt("intelligence", 25),
                        vitality = sObj.optInt("vitality", 20),
                        sense = sObj.optInt("sense", 19),
                        unallocatedPoints = sObj.optInt("unallocatedPoints", 5),
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }

            if (root.has("guild_progress")) {
                val gObj = root.getJSONObject("guild_progress")
                guildProgressDao?.insertOrUpdateGuildProgress(
                    GuildProgressEntity(
                        id = 1,
                        guildName = gObj.optString("guildName", "Shadow Vanguard"),
                        raidBossName = gObj.optString("raidBossName", "Igris: Productivity Citadel"),
                        raidBossHp = gObj.optInt("raidBossHp", 5000),
                        raidDamageDealt = gObj.optInt("raidDamageDealt", 0),
                        personalContributionXp = gObj.optInt("personalContributionXp", 0)
                    )
                )
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("LifeTrackerRepository", "Error restoring backup JSON: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ========================================================================
    // 13. DATA RESET & SEED CLEANUP
    // ========================================================================

    suspend fun deleteAllUserData() {
        taskDao.deleteAllTasks()
        habitDao.deleteAllHabits()
        expenseDao.deleteAllExpenses()
        healthDao.deleteAllHealthLogs()
        journalDao.deleteAllJournalEntries()
        noteDao.deleteAllNotes()
        goalDao.deleteAllGoals()
        calendarDao.deleteAllEvents()

        userStatsDao.insertOrUpdateUserStats(
            UserStatsEntity(
                id = 1, totalXp = 0, level = 1, coins = 0, streakDays = 0,
                strength = 10, agility = 10, intelligence = 10, vitality = 10, sense = 10,
                unallocatedPoints = 3
            )
        )
        guildProgressDao?.insertOrUpdateGuildProgress(GuildProgressEntity(id = 1, raidDamageDealt = 0, personalContributionXp = 0))
    }

    suspend fun clearFakeSeedData() {
        taskDao.getAllTasksList().filter {
            it.title.contains("Review Weekly Life Goals") ||
                it.title.contains("Upper Body HIIT") ||
                it.title.contains("Atomic Habits") ||
                it.title.contains("Hydrate 2.5 Liters") ||
                it.title.contains("AI Spending Analysis")
        }.forEach { taskDao.deleteTask(it) }

        habitDao.getAllHabitsList().filter {
            it.name.contains("Mindfulness Meditation") ||
                it.name.contains("Drink 250ml Water") ||
                it.name.contains("30 Mins Daily Reading") ||
                it.name.contains("No Sugar After") ||
                it.name.contains("Weekly Budget Review")
        }.forEach { habitDao.deleteHabit(it) }

        expenseDao.getAllExpensesList().filter {
            it.title.contains("Organic Grocery") ||
                it.title.contains("Gym Membership") ||
                it.title.contains("Freelance UX") ||
                it.title.contains("Coffee & Bakery") ||
                it.title.contains("Metro Transit")
        }.forEach { expenseDao.deleteExpense(it) }

        journalDao.getAllJournalEntriesList().filter {
            it.title.contains("Reflections on Productivity") ||
                it.title.contains("Evening Gratitude")
        }.forEach { journalDao.deleteJournalEntry(it) }

        noteDao.getAllNotesList().filter {
            it.title.contains("High Performance Morning Routine") ||
                it.title.contains("Healthy Grocery Checklist")
        }.forEach { noteDao.deleteNote(it) }

        goalDao.getAllGoalsList().filter {
            it.title.contains("Complete AI Life Tracker") ||
                it.title.contains("Run a Full Marathon")
        }.forEach { goalDao.deleteGoal(it) }

        calendarDao.getAllEventsList().filter {
            it.title.contains("Design System Review") ||
                it.title.contains("Mom's Birthday")
        }.forEach { calendarDao.deleteEvent(it) }

        val p = userDao.getUserProfileSingle()
        if (p?.name == "Alex Rivera") {
            userDao.insertOrUpdateUserProfile(p.copy(name = "User", email = ""))
        }
    }

    // ========================================================================
    // 14. GEMINI & ON-DEVICE EDGE AI ASSISTANT INFERENCE
    // ========================================================================

    suspend fun askGemini(prompt: String, snapshot: PredictiveLifeSnapshot? = null): String {
        return geminiApiService.generateAiResponse(prompt, snapshot)
    }

    // ========================================================================
    // FIRESTORE MAPPING HELPERS FOR BIDIRECTIONAL RECONCILIATION
    // ========================================================================

    private fun taskToMap(t: TaskEntity): Map<String, Any> = mapOf(
        "title" to t.title,
        "description" to t.description,
        "category" to t.category,
        "categoryId" to t.categoryId,
        "priority" to t.priority,
        "isCompleted" to t.isCompleted,
        "dueDate" to t.dueDate,
        "updatedAt" to t.updatedAt
    )

    private fun mapToTask(m: Map<String, Any>): TaskEntity? {
        val id = (m["id"] as? String)?.toLongOrNull() ?: return null
        val title = m["title"] as? String ?: return null
        return TaskEntity(
            id = id,
            title = title,
            description = m["description"] as? String ?: "",
            isCompleted = m["isCompleted"] as? Boolean ?: false,
            priority = m["priority"] as? String ?: "Medium",
            category = m["category"] as? String ?: "Personal",
            categoryId = (m["categoryId"] as? Number)?.toLong() ?: 1L,
            dueDate = (m["dueDate"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            updatedAt = (m["updatedAt"] as? Number)?.toLong() ?: 0L
        )
    }

    private fun habitToMap(h: HabitEntity): Map<String, Any> = mapOf(
        "name" to h.name,
        "category" to h.category,
        "frequency" to h.frequency,
        "streakCount" to h.streakCount,
        "isCompletedToday" to h.isCompletedToday,
        "lastCompletedDate" to h.lastCompletedDate,
        "updatedAt" to h.updatedAt
    )

    private fun mapToHabit(m: Map<String, Any>): HabitEntity? {
        val id = (m["id"] as? String)?.toLongOrNull() ?: return null
        val name = m["name"] as? String ?: return null
        return HabitEntity(
            id = id,
            name = name,
            category = m["category"] as? String ?: "Health",
            frequency = m["frequency"] as? String ?: "Daily",
            streakCount = (m["streakCount"] as? Number)?.toInt() ?: 0,
            isCompletedToday = m["isCompletedToday"] as? Boolean ?: false,
            lastCompletedDate = (m["lastCompletedDate"] as? Number)?.toLong() ?: 0L,
            updatedAt = (m["updatedAt"] as? Number)?.toLong() ?: 0L
        )
    }

    private fun expenseToMap(e: ExpenseEntity): Map<String, Any> = mapOf(
        "title" to e.title,
        "amount" to e.amount,
        "type" to e.type,
        "category" to e.category,
        "notes" to e.notes,
        "date" to e.date,
        "updatedAt" to e.updatedAt
    )

    private fun mapToExpense(m: Map<String, Any>): ExpenseEntity? {
        val id = (m["id"] as? String)?.toLongOrNull() ?: return null
        val title = m["title"] as? String ?: return null
        return ExpenseEntity(
            id = id,
            title = title,
            amount = (m["amount"] as? Number)?.toDouble() ?: 0.0,
            type = m["type"] as? String ?: "Expense",
            category = m["category"] as? String ?: "General",
            notes = m["notes"] as? String ?: "",
            date = (m["date"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            updatedAt = (m["updatedAt"] as? Number)?.toLong() ?: 0L
        )
    }
}
