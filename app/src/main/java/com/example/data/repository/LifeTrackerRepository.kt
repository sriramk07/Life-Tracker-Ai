package com.example.data.repository

import android.util.Log
import com.example.common.AppResult
import com.example.common.safeApiCall
import com.example.common.safeDbCall
import com.example.data.local.dao.*
import com.example.data.local.entities.*
import com.example.data.remote.FirestoreManager
import com.example.data.remote.GeminiApiService
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

/**
 * ============================================================================
 * LIFE TRACKER REPOSITORY — DATA ACCESS & ORCHESTRATION LAYER
 * ============================================================================
 *
 * Serves as the single source of truth for the LifeOS AI application.
 * Manages dual-tier data access:
 * 1. High-performance offline caching via Room SQLite local database DAOs.
 * 2. Remote synchronization with Google Cloud Firestore and Gemini AI API.
 * 3. Granular error boundaries protecting UI from database/network exceptions.
 * 4. Comprehensive JSON Backup & Restore engine for complete data migration.
 *
 * @property taskDao DAO providing CRUD operations on user tasks.
 * @property habitDao DAO providing CRUD operations on recurring habits.
 * @property expenseDao DAO providing CRUD operations on expenses & income.
 * @property healthDao DAO providing health metric logs & BMI history.
 * @property journalDao DAO providing daily reflection logs.
 * @property noteDao DAO providing notes and checklist items.
 * @property goalDao DAO providing short-term & long-term targets.
 * @property calendarDao DAO providing schedule events and agenda items.
 * @property userDao DAO providing user preference configuration.
 * @property userStatsDao DAO providing gamification levels & XP stats.
 * @property categoryDao Optional DAO providing customized categories.
 * @property geminiApiService Remote client for Gemini & OpenRouter AI inference.
 * @property firestoreManager Remote client for Firebase Cloud Firestore syncing.
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
    private val geminiApiService: GeminiApiService,
    private val firestoreManager: FirestoreManager = FirestoreManager()
) {
    // ========================================================================
    // 1. CATEGORY OPERATIONS
    // ========================================================================

    /**
     * Flow emitting all user and default task categories ordered alphabetically.
     */
    val allCategories: Flow<List<CategoryEntity>> = categoryDao?.getAllCategories()
        ?: kotlinx.coroutines.flow.flowOf(emptyList())

    /**
     * Inserts a new category into the local database.
     *
     * @param category The category entity to persist.
     * @return Generated database row ID.
     */
    suspend fun insertCategory(category: CategoryEntity): Long =
        categoryDao?.insertCategory(category) ?: 0L

    /**
     * Deletes a category from the local database.
     *
     * @param category The category entity to remove.
     */
    suspend fun deleteCategory(category: CategoryEntity) {
        categoryDao?.deleteCategory(category)
    }

    // ========================================================================
    // 2. TASK OPERATIONS
    // ========================================================================

    /**
     * Observable stream of all tasks ordered chronologically by due date.
     */
    val allTasks: Flow<List<TaskEntity>> = taskDao.getAllTasks()

    /**
     * Persists a new task entity locally and asynchronously mirrors it to Firestore.
     *
     * @param task The task data model to store.
     * @return Generated primary key ID.
     */
    suspend fun insertTask(task: TaskEntity): Long {
        val id = taskDao.insertTask(task)
        val entityWithId = if (task.id == 0L) task.copy(id = id) else task
        try {
            firestoreManager.saveDocument(
                "tasks", entityWithId.id.toString(), mapOf(
                    "title" to entityWithId.title,
                    "category" to entityWithId.category,
                    "categoryId" to entityWithId.categoryId,
                    "priority" to entityWithId.priority,
                    "isCompleted" to entityWithId.isCompleted,
                    "dueDate" to entityWithId.dueDate
                )
            )
        } catch (e: Exception) {
            Log.w("LifeTrackerRepo", "Offline mode: Failed mirroring task to Firestore: ${e.message}")
        }
        return id
    }

    /**
     * Updates an existing task entity in local storage and cloud database.
     *
     * @param task Updated task object containing modified fields.
     */
    suspend fun updateTask(task: TaskEntity) {
        taskDao.updateTask(task)
        try {
            firestoreManager.saveDocument(
                "tasks", task.id.toString(), mapOf(
                    "title" to task.title,
                    "category" to task.category,
                    "priority" to task.priority,
                    "isCompleted" to task.isCompleted,
                    "dueDate" to task.dueDate
                )
            )
        } catch (e: Exception) {
            Log.w("LifeTrackerRepo", "Offline mode: Failed updating task in Firestore: ${e.message}")
        }
    }

    /**
     * Permanently deletes a task from local database and Firestore.
     *
     * @param task The task instance to remove.
     */
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

    /**
     * Flow of all tracked habits ordered by newest first.
     */
    val allHabits: Flow<List<HabitEntity>> = habitDao.getAllHabits()

    /**
     * Inserts a new habit routine and mirrors metadata to Firestore.
     *
     * @param habit The habit entity to record.
     * @return Generated row ID.
     */
    suspend fun insertHabit(habit: HabitEntity): Long {
        val id = habitDao.insertHabit(habit)
        val entityWithId = if (habit.id == 0L) habit.copy(id = id) else habit
        try {
            firestoreManager.saveDocument(
                "habits", entityWithId.id.toString(), mapOf(
                    "name" to entityWithId.name,
                    "category" to entityWithId.category,
                    "frequency" to entityWithId.frequency,
                    "streakCount" to entityWithId.streakCount,
                    "isCompletedToday" to entityWithId.isCompletedToday
                )
            )
        } catch (e: Exception) {
            Log.w("LifeTrackerRepo", "Offline mode: Failed mirroring habit to Firestore: ${e.message}")
        }
        return id
    }

    /**
     * Updates habit streak counts and daily completion flags.
     *
     * @param habit Habit instance with updated streak numbers.
     */
    suspend fun updateHabit(habit: HabitEntity) {
        habitDao.updateHabit(habit)
        try {
            firestoreManager.saveDocument(
                "habits", habit.id.toString(), mapOf(
                    "name" to habit.name,
                    "category" to habit.category,
                    "frequency" to habit.frequency,
                    "streakCount" to habit.streakCount,
                    "isCompletedToday" to habit.isCompletedToday
                )
            )
        } catch (e: Exception) {
            Log.w("LifeTrackerRepo", "Offline mode: Failed updating habit in Firestore: ${e.message}")
        }
    }

    /**
     * Deletes a habit routine permanently.
     *
     * @param habit Habit instance to delete.
     */
    suspend fun deleteHabit(habit: HabitEntity) {
        habitDao.deleteHabit(habit)
        try {
            firestoreManager.deleteDocument("habits", habit.id.toString())
        } catch (e: Exception) {
            Log.w("LifeTrackerRepo", "Offline mode: Failed deleting habit from Firestore: ${e.message}")
        }
    }

    // ========================================================================
    // 4. EXPENSE & BUDGET OPERATIONS
    // ========================================================================

    /**
     * Flow of all logged financial transactions sorted descending by timestamp.
     */
    val allExpenses: Flow<List<ExpenseEntity>> = expenseDao.getAllExpenses()

    /**
     * Logs an expense or income transaction.
     *
     * @param expense The financial transaction record.
     * @return Generated primary key ID.
     */
    suspend fun insertExpense(expense: ExpenseEntity): Long {
        val id = expenseDao.insertExpense(expense)
        val entityWithId = if (expense.id == 0L) expense.copy(id = id) else expense
        try {
            firestoreManager.saveDocument(
                "expenses", entityWithId.id.toString(), mapOf(
                    "title" to entityWithId.title,
                    "amount" to entityWithId.amount,
                    "type" to entityWithId.type,
                    "category" to entityWithId.category,
                    "notes" to entityWithId.notes,
                    "date" to entityWithId.date
                )
            )
        } catch (e: Exception) {
            Log.w("LifeTrackerRepo", "Offline mode: Failed mirroring expense to Firestore: ${e.message}")
        }
        return id
    }

    /**
     * Deletes a financial transaction from history.
     */
    suspend fun deleteExpense(expense: ExpenseEntity) {
        expenseDao.deleteExpense(expense)
        try {
            firestoreManager.deleteDocument("expenses", expense.id.toString())
        } catch (e: Exception) {
            Log.w("LifeTrackerRepo", "Offline mode: Failed deleting expense from Firestore: ${e.message}")
        }
    }

    // ========================================================================
    // 5. HEALTH & WELLNESS LOG OPERATIONS
    // ========================================================================

    /**
     * Flow of historical health metric logs.
     */
    val allHealthLogs: Flow<List<HealthLogEntity>> = healthDao.getAllHealthLogs()

    /**
     * Flow emitting the most recently recorded health log.
     */
    val latestHealthLog: Flow<HealthLogEntity?> = healthDao.getLatestHealthLog()

    /**
     * Records or updates a health entry (hydration, sleep, steps, BMI).
     *
     * @param healthLog Health snapshot data entity.
     * @return Generated database ID.
     */
    suspend fun insertHealthLog(healthLog: HealthLogEntity): Long {
        val id = healthDao.insertHealthLog(healthLog)
        val entityWithId = if (healthLog.id == 0L) healthLog.copy(id = id) else healthLog
        try {
            firestoreManager.saveDocument(
                "health_logs", entityWithId.id.toString(), mapOf(
                    "weightKg" to entityWithId.weightKg,
                    "heightCm" to entityWithId.heightCm,
                    "waterIntakeMl" to entityWithId.waterIntakeMl,
                    "sleepHours" to entityWithId.sleepHours,
                    "stepsCount" to entityWithId.stepsCount,
                    "mood" to entityWithId.mood,
                    "workoutDurationMins" to entityWithId.workoutDurationMins,
                    "date" to entityWithId.date
                )
            )
        } catch (e: Exception) {
            Log.w("LifeTrackerRepo", "Offline mode: Failed mirroring health log to Firestore: ${e.message}")
        }
        return id
    }

    // ========================================================================
    // 6. JOURNAL OPERATIONS
    // ========================================================================

    /**
     * Flow of all diary and personal reflection entries.
     */
    val allJournals: Flow<List<JournalEntity>> = journalDao.getAllJournalEntries()

    /**
     * Inserts a new journal entry.
     */
    suspend fun insertJournal(journal: JournalEntity): Long {
        val id = journalDao.insertJournalEntry(journal)
        val entityWithId = if (journal.id == 0L) journal.copy(id = id) else journal
        try {
            firestoreManager.saveDocument(
                "journals", entityWithId.id.toString(), mapOf(
                    "title" to entityWithId.title,
                    "content" to entityWithId.content,
                    "moodTag" to entityWithId.moodTag,
                    "tags" to entityWithId.tags,
                    "date" to entityWithId.date
                )
            )
        } catch (e: Exception) {
            Log.w("LifeTrackerRepo", "Offline mode: Failed mirroring journal to Firestore: ${e.message}")
        }
        return id
    }

    /**
     * Deletes a journal entry.
     */
    suspend fun deleteJournal(journal: JournalEntity) {
        journalDao.deleteJournalEntry(journal)
        try {
            firestoreManager.deleteDocument("journals", journal.id.toString())
        } catch (e: Exception) {
            Log.w("LifeTrackerRepo", "Offline mode: Failed deleting journal from Firestore: ${e.message}")
        }
    }

    // ========================================================================
    // 7. NOTES & CHECKLIST OPERATIONS
    // ========================================================================

    /**
     * Flow of notes sorted with pinned notes first, then descending by update time.
     */
    val allNotes: Flow<List<NoteEntity>> = noteDao.getAllNotes()

    /**
     * Inserts a new note or checklist.
     */
    suspend fun insertNote(note: NoteEntity): Long {
        val id = noteDao.insertNote(note)
        val entityWithId = if (note.id == 0L) note.copy(id = id) else note
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
            Log.w("LifeTrackerRepo", "Offline mode: Failed mirroring note to Firestore: ${e.message}")
        }
        return id
    }

    /**
     * Updates an existing note.
     */
    suspend fun updateNote(note: NoteEntity) {
        noteDao.updateNote(note)
        try {
            firestoreManager.saveDocument(
                "notes", note.id.toString(), mapOf(
                    "title" to note.title,
                    "content" to note.content,
                    "isChecklist" to note.isChecklist,
                    "isPinned" to note.isPinned,
                    "folder" to note.folder,
                    "updatedAt" to note.updatedAt
                )
            )
        } catch (e: Exception) {
            Log.w("LifeTrackerRepo", "Offline mode: Failed updating note in Firestore: ${e.message}")
        }
    }

    /**
     * Deletes a note.
     */
    suspend fun deleteNote(note: NoteEntity) {
        noteDao.deleteNote(note)
        try {
            firestoreManager.deleteDocument("notes", note.id.toString())
        } catch (e: Exception) {
            Log.w("LifeTrackerRepo", "Offline mode: Failed deleting note from Firestore: ${e.message}")
        }
    }

    // ========================================================================
    // 8. GOALS & TARGETS OPERATIONS
    // ========================================================================

    /**
     * Flow of user goals ordered by deadline proximity.
     */
    val allGoals: Flow<List<GoalEntity>> = goalDao.getAllGoals()

    /**
     * Inserts a new life goal.
     */
    suspend fun insertGoal(goal: GoalEntity): Long {
        val id = goalDao.insertGoal(goal)
        val entityWithId = if (goal.id == 0L) goal.copy(id = id) else goal
        try {
            firestoreManager.saveDocument(
                "goals", entityWithId.id.toString(), mapOf(
                    "title" to entityWithId.title,
                    "description" to entityWithId.description,
                    "category" to entityWithId.category,
                    "type" to entityWithId.type,
                    "progressPercent" to entityWithId.progressPercent,
                    "milestones" to entityWithId.milestones,
                    "deadline" to entityWithId.deadline
                )
            )
        } catch (e: Exception) {
            Log.w("LifeTrackerRepo", "Offline mode: Failed mirroring goal to Firestore: ${e.message}")
        }
        return id
    }

    /**
     * Updates goal progress percentage.
     */
    suspend fun updateGoal(goal: GoalEntity) {
        goalDao.updateGoal(goal)
        try {
            firestoreManager.saveDocument(
                "goals", goal.id.toString(), mapOf(
                    "title" to goal.title,
                    "description" to goal.description,
                    "category" to goal.category,
                    "type" to goal.type,
                    "progressPercent" to goal.progressPercent,
                    "milestones" to goal.milestones,
                    "deadline" to goal.deadline
                )
            )
        } catch (e: Exception) {
            Log.w("LifeTrackerRepo", "Offline mode: Failed updating goal in Firestore: ${e.message}")
        }
    }

    /**
     * Deletes a goal.
     */
    suspend fun deleteGoal(goal: GoalEntity) {
        goalDao.deleteGoal(goal)
        try {
            firestoreManager.deleteDocument("goals", goal.id.toString())
        } catch (e: Exception) {
            Log.w("LifeTrackerRepo", "Offline mode: Failed deleting goal from Firestore: ${e.message}")
        }
    }

    // ========================================================================
    // 9. CALENDAR & SCHEDULE OPERATIONS
    // ========================================================================

    /**
     * Flow of calendar events ordered chronologically.
     */
    val allCalendarEvents: Flow<List<CalendarEventEntity>> = calendarDao.getAllEvents()

    /**
     * Inserts a calendar event.
     */
    suspend fun insertCalendarEvent(event: CalendarEventEntity): Long {
        val id = calendarDao.insertEvent(event)
        val entityWithId = if (event.id == 0L) event.copy(id = id) else event
        try {
            firestoreManager.saveDocument(
                "calendar_events", entityWithId.id.toString(), mapOf(
                    "title" to entityWithId.title,
                    "description" to entityWithId.description,
                    "startTime" to entityWithId.startTime,
                    "endTime" to entityWithId.endTime,
                    "eventType" to entityWithId.eventType,
                    "location" to entityWithId.location
                )
            )
        } catch (e: Exception) {
            Log.w("LifeTrackerRepo", "Offline mode: Failed mirroring event to Firestore: ${e.message}")
        }
        return id
    }

    /**
     * Deletes a calendar event.
     */
    suspend fun deleteCalendarEvent(event: CalendarEventEntity) {
        calendarDao.deleteEvent(event)
        try {
            firestoreManager.deleteDocument("calendar_events", event.id.toString())
        } catch (e: Exception) {
            Log.w("LifeTrackerRepo", "Offline mode: Failed deleting event from Firestore: ${e.message}")
        }
    }

    // ========================================================================
    // 10. USER PROFILE & GAMIFICATION STATS
    // ========================================================================

    /**
     * Flow emitting the single user profile configuration.
     */
    val userProfile: Flow<UserProfileEntity?> = userDao.getUserProfile()

    /**
     * Updates user profile attributes (goals, theme, budget).
     */
    suspend fun updateProfile(profile: UserProfileEntity) {
        userDao.insertOrUpdateUserProfile(profile)
        try {
            firestoreManager.saveDocument(
                "user_profile", profile.id.toString(), mapOf(
                    "name" to profile.name,
                    "email" to profile.email,
                    "monthlyBudget" to profile.monthlyBudget,
                    "dailyWaterGoalMl" to profile.dailyWaterGoalMl,
                    "isDarkMode" to profile.isDarkMode
                )
            )
        } catch (e: Exception) {
            Log.w("LifeTrackerRepo", "Offline mode: Failed mirroring profile to Firestore: ${e.message}")
        }
    }

    /**
     * Flow emitting the RPG Monarch / Solo Leveling stats.
     */
    val userStats: Flow<UserStatsEntity?> = userStatsDao.getUserStats()

    /**
     * Updates user gamification statistics (XP, level, coins, streak).
     */
    suspend fun updateUserStats(stats: UserStatsEntity) {
        userStatsDao.insertOrUpdateUserStats(stats)
        try {
            firestoreManager.saveDocument(
                "user_stats", stats.id.toString(), mapOf(
                    "totalXp" to stats.totalXp,
                    "level" to stats.level,
                    "coins" to stats.coins,
                    "streakDays" to stats.streakDays,
                    "updatedAt" to stats.updatedAt
                )
            )
        } catch (e: Exception) {
            Log.w("LifeTrackerRepo", "Offline mode: Failed mirroring user stats to Firestore: ${e.message}")
        }
    }

    // ========================================================================
    // 11. CLOUD SYNCHRONIZATION WITH ERROR BOUNDARY
    // ========================================================================

    /**
     * Performs an end-to-end cloud sync of all local Room records with Google Firestore.
     * Wrapped with strict error boundary to isolate network failures and report status cleanly.
     *
     * @param userEmail The email address used to namespace the user's Firestore data.
     * @return [Result.success] on completion or [Result.failure] wrapping the caught exception.
     */
    suspend fun syncUserCloudData(userEmail: String): Result<Unit> {
        return try {
            val tasksList = taskDao.getAllTasksList()
            tasksList.forEach { task ->
                firestoreManager.saveUserDocument(
                    userEmail, "tasks", task.id.toString(), mapOf(
                        "title" to task.title,
                        "category" to task.category,
                        "priority" to task.priority,
                        "isCompleted" to task.isCompleted,
                        "dueDate" to task.dueDate
                    )
                )
            }

            val habitsList = habitDao.getAllHabitsList()
            habitsList.forEach { habit ->
                firestoreManager.saveUserDocument(
                    userEmail, "habits", habit.id.toString(), mapOf(
                        "name" to habit.name,
                        "category" to habit.category,
                        "frequency" to habit.frequency,
                        "streakCount" to habit.streakCount,
                        "isCompletedToday" to habit.isCompletedToday
                    )
                )
            }

            val expensesList = expenseDao.getAllExpensesList()
            expensesList.forEach { exp ->
                firestoreManager.saveUserDocument(
                    userEmail, "expenses", exp.id.toString(), mapOf(
                        "title" to exp.title,
                        "amount" to exp.amount,
                        "type" to exp.type,
                        "category" to exp.category,
                        "notes" to exp.notes,
                        "date" to exp.date
                    )
                )
            }

            val healthLogs = healthDao.getAllHealthLogsList()
            healthLogs.forEach { hl ->
                firestoreManager.saveUserDocument(
                    userEmail, "health_logs", hl.id.toString(), mapOf(
                        "waterIntakeMl" to hl.waterIntakeMl,
                        "sleepHours" to hl.sleepHours,
                        "stepsCount" to hl.stepsCount,
                        "mood" to hl.mood,
                        "workoutDurationMins" to hl.workoutDurationMins,
                        "date" to hl.date
                    )
                )
            }

            val profile = userDao.getUserProfileSingle() ?: UserProfileEntity(email = userEmail)
            userDao.insertOrUpdateUserProfile(profile.copy(email = userEmail))
            firestoreManager.saveUserDocument(
                userEmail, "profile", "user_profile", mapOf(
                    "name" to profile.name,
                    "email" to userEmail,
                    "monthlyBudget" to profile.monthlyBudget,
                    "dailyWaterGoalMl" to profile.dailyWaterGoalMl,
                    "isDarkMode" to profile.isDarkMode,
                    "syncedAt" to System.currentTimeMillis()
                )
            )

            val stats = userStatsDao.getUserStatsSingle() ?: UserStatsEntity()
            firestoreManager.saveUserDocument(
                userEmail, "stats", "user_stats", mapOf(
                    "totalXp" to stats.totalXp,
                    "level" to stats.level,
                    "coins" to stats.coins,
                    "streakDays" to stats.streakDays,
                    "updatedAt" to stats.updatedAt
                )
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("LifeTrackerRepository", "Error boundary captured cloud sync failure: ${e.message}")
            Result.failure(e)
        }
    }

    // ========================================================================
    // 12. FULL SYSTEM BACKUP & RESTORE ENGINE (JSON DTO)
    // ========================================================================

    /**
     * Exports all user records across all Room tables into a structured JSON string.
     * Allows full offline backup, migration, and export for external storage.
     *
     * @return Formatted JSON string containing all application data.
     */
    suspend fun exportUserDataJson(): String {
        val root = JSONObject()
        root.put("version", 2)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("appName", "LifeOS AI")

        // 1. Tasks
        val tasksArray = JSONArray()
        taskDao.getAllTasksList().forEach { t ->
            val obj = JSONObject().apply {
                put("id", t.id)
                put("title", t.title)
                put("description", t.description)
                put("isCompleted", t.isCompleted)
                put("priority", t.priority)
                put("category", t.category)
                put("dueDate", t.dueDate)
            }
            tasksArray.put(obj)
        }
        root.put("tasks", tasksArray)

        // 2. Habits
        val habitsArray = JSONArray()
        habitDao.getAllHabitsList().forEach { h ->
            val obj = JSONObject().apply {
                put("id", h.id)
                put("name", h.name)
                put("category", h.category)
                put("frequency", h.frequency)
                put("streakCount", h.streakCount)
                put("isCompletedToday", h.isCompletedToday)
                put("lastCompletedDate", h.lastCompletedDate)
            }
            habitsArray.put(obj)
        }
        root.put("habits", habitsArray)

        // 3. Expenses
        val expensesArray = JSONArray()
        expenseDao.getAllExpensesList().forEach { e ->
            val obj = JSONObject().apply {
                put("id", e.id)
                put("title", e.title)
                put("amount", e.amount)
                put("type", e.type)
                put("category", e.category)
                put("date", e.date)
                put("notes", e.notes)
            }
            expensesArray.put(obj)
        }
        root.put("expenses", expensesArray)

        // 4. Health Logs
        val healthArray = JSONArray()
        healthDao.getAllHealthLogsList().forEach { hl ->
            val obj = JSONObject().apply {
                put("id", hl.id)
                put("date", hl.date)
                put("waterIntakeMl", hl.waterIntakeMl)
                put("sleepHours", hl.sleepHours.toDouble())
                put("weightKg", hl.weightKg.toDouble())
                put("heightCm", hl.heightCm.toDouble())
                put("stepsCount", hl.stepsCount)
                put("mood", hl.mood)
                put("workoutDurationMins", hl.workoutDurationMins)
            }
            healthArray.put(obj)
        }
        root.put("health_logs", healthArray)

        // 5. Journals
        val journalsArray = JSONArray()
        journalDao.getAllJournalEntriesList().forEach { j ->
            val obj = JSONObject().apply {
                put("id", j.id)
                put("title", j.title)
                put("content", j.content)
                put("date", j.date)
                put("moodTag", j.moodTag)
                put("tags", j.tags)
            }
            journalsArray.put(obj)
        }
        root.put("journals", journalsArray)

        // 6. Notes
        val notesArray = JSONArray()
        noteDao.getAllNotesList().forEach { n ->
            val obj = JSONObject().apply {
                put("id", n.id)
                put("title", n.title)
                put("content", n.content)
                put("isChecklist", n.isChecklist)
                put("isPinned", n.isPinned)
                put("folder", n.folder)
                put("updatedAt", n.updatedAt)
            }
            notesArray.put(obj)
        }
        root.put("notes", notesArray)

        // 7. Goals
        val goalsArray = JSONArray()
        goalDao.getAllGoalsList().forEach { g ->
            val obj = JSONObject().apply {
                put("id", g.id)
                put("title", g.title)
                put("description", g.description)
                put("category", g.category)
                put("type", g.type)
                put("progressPercent", g.progressPercent)
                put("deadline", g.deadline)
                put("milestones", g.milestones)
            }
            goalsArray.put(obj)
        }
        root.put("goals", goalsArray)

        // 8. Calendar
        val eventsArray = JSONArray()
        calendarDao.getAllEventsList().forEach { c ->
            val obj = JSONObject().apply {
                put("id", c.id)
                put("title", c.title)
                put("description", c.description)
                put("startTime", c.startTime)
                put("endTime", c.endTime)
                put("eventType", c.eventType)
                put("location", c.location)
            }
            eventsArray.put(obj)
        }
        root.put("calendar_events", eventsArray)

        // 9. User Profile
        userDao.getUserProfileSingle()?.let { p ->
            val profileObj = JSONObject().apply {
                put("name", p.name)
                put("email", p.email)
                put("isDarkMode", p.isDarkMode)
                put("monthlyBudget", p.monthlyBudget)
                put("dailyWaterGoalMl", p.dailyWaterGoalMl)
            }
            root.put("user_profile", profileObj)
        }

        // 10. User Stats
        userStatsDao.getUserStatsSingle()?.let { s ->
            val statsObj = JSONObject().apply {
                put("totalXp", s.totalXp)
                put("level", s.level)
                put("coins", s.coins)
                put("streakDays", s.streakDays)
            }
            root.put("user_stats", statsObj)
        }

        return root.toString(2)
    }

    /**
     * Restores application state from a verified backup JSON string.
     * Wrapped in an error boundary to prevent corrupt JSON from wiping existing data.
     *
     * @param jsonContent Serialized JSON payload created via [exportUserDataJson].
     * @return [Result.success] if restoration completes, or [Result.failure] on error.
     */
    suspend fun restoreUserDataFromJson(jsonContent: String): Result<Unit> {
        return try {
            if (jsonContent.isBlank()) {
                return Result.failure(IllegalArgumentException("Backup payload cannot be empty"))
            }

            val root = JSONObject(jsonContent)

            // 1. Restore Tasks
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
                            dueDate = obj.optLong("dueDate", System.currentTimeMillis())
                        )
                    )
                }
            }

            // 2. Restore Habits
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
                            lastCompletedDate = obj.optLong("lastCompletedDate", 0L)
                        )
                    )
                }
            }

            // 3. Restore Expenses
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
                            notes = obj.optString("notes", "")
                        )
                    )
                }
            }

            // 4. Restore Health Logs
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
                            mood = obj.optString("mood", "😊 Happy"),
                            workoutDurationMins = obj.optInt("workoutDurationMins", 0)
                        )
                    )
                }
            }

            // 5. Restore Journals
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

            // 6. Restore Notes
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
                            isPinned = obj.optBoolean("isPinned", false),
                            folder = obj.optString("folder", "General"),
                            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            // 7. Restore Goals
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
                            deadline = obj.optLong("deadline", System.currentTimeMillis())
                        )
                    )
                }
            }

            // 8. Restore Calendar
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

            // 9. Restore Profile
            if (root.has("user_profile")) {
                val pObj = root.getJSONObject("user_profile")
                userDao.insertOrUpdateUserProfile(
                    UserProfileEntity(
                        id = 1,
                        name = pObj.optString("name", "User"),
                        email = pObj.optString("email", ""),
                        isDarkMode = pObj.optBoolean("isDarkMode", true),
                        monthlyBudget = pObj.optDouble("monthlyBudget", 2000.0),
                        dailyWaterGoalMl = pObj.optInt("dailyWaterGoalMl", 2500)
                    )
                )
            }

            // 10. Restore Stats
            if (root.has("user_stats")) {
                val sObj = root.getJSONObject("user_stats")
                userStatsDao.insertOrUpdateUserStats(
                    UserStatsEntity(
                        id = 1,
                        totalXp = sObj.optInt("totalXp", 0),
                        level = sObj.optInt("level", 1),
                        coins = sObj.optInt("coins", 0),
                        streakDays = sObj.optInt("streakDays", 0),
                        updatedAt = System.currentTimeMillis()
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

    /**
     * Clears all local database tables for user data reset.
     */
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
            UserStatsEntity(id = 1, totalXp = 0, level = 1, coins = 0, streakDays = 0)
        )
    }

    /**
     * Removes mock seed entries to provide clean state for genuine user data.
     */
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
    // 14. GEMINI AI ASSISTANT INFERENCE
    // ========================================================================

    /**
     * Queries the dual AI inference engine with an enriched contextual life prompt.
     * Protected by network timeout boundaries and offline fallback engine.
     *
     * @param prompt Contextual life query formatted with recent metrics.
     * @return Markdown formatted life coaching response.
     */
    suspend fun askGemini(prompt: String): String {
        return geminiApiService.generateAiResponse(prompt)
    }
}
