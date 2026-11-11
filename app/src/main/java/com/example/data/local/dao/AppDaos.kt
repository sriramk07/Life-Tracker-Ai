package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entities.*
import kotlinx.coroutines.flow.Flow

/**
 * ============================================================================
 * LIFE TRACKER AI — ROOM DATA ACCESS OBJECTS (DAOs) — V4
 * ============================================================================
 *
 * Provides reactive [Flow] and suspending query methods for all 12 database entities.
 * Implements ConflictStrategy.REPLACE for resilient upsert mechanics.
 */

/**
 * Data Access Object for tasks and priority quest entities.
 */
@Dao
interface TaskDao {
    /** Reactive stream of all tasks ordered by nearest due date. */
    @Query("SELECT * FROM tasks ORDER BY dueDate ASC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    /** One-shot snapshot of all tasks for cloud sync or export. */
    @Query("SELECT * FROM tasks ORDER BY dueDate ASC")
    suspend fun getAllTasksList(): List<TaskEntity>

    /** Inserts or replaces a task entity. Returns row ID. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    /** Updates an existing task entity. */
    @Update
    suspend fun updateTask(task: TaskEntity)

    /** Deletes all tasks during a complete data reset. */
    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasks()

    /** Deletes a single task entity. */
    @Delete
    suspend fun deleteTask(task: TaskEntity)

    /** Deletes a task by primary key ID. */
    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Long)
}

/**
 * Data Access Object for recurring daily habits and duty streaks.
 */
@Dao
interface HabitDao {
    /** Reactive stream of all habits sorted descending by ID. */
    @Query("SELECT * FROM habits ORDER BY id DESC")
    fun getAllHabits(): Flow<List<HabitEntity>>

    /** One-shot list snapshot of all habits. */
    @Query("SELECT * FROM habits ORDER BY id DESC")
    suspend fun getAllHabitsList(): List<HabitEntity>

    /** Inserts or replaces a habit routine. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitEntity): Long

    /** Updates habit streak or daily completion state. */
    @Update
    suspend fun updateHabit(habit: HabitEntity)

    /** Deletes all habits. */
    @Query("DELETE FROM habits")
    suspend fun deleteAllHabits()

    /** Deletes a single habit entity. */
    @Delete
    suspend fun deleteHabit(habit: HabitEntity)
}

/**
 * Data Access Object for financial transactions and expenses.
 */
@Dao
interface ExpenseDao {
    /** Reactive stream of all transactions ordered newest to oldest. */
    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<ExpenseEntity>>

    /** One-shot list snapshot of all transactions. */
    @Query("SELECT * FROM expenses ORDER BY date DESC")
    suspend fun getAllExpensesList(): List<ExpenseEntity>

    /** Inserts or replaces an expense/income record. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity): Long

    /** Updates an existing transaction. */
    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    /** Deletes all financial logs. */
    @Query("DELETE FROM expenses")
    suspend fun deleteAllExpenses()

    /** Deletes a single transaction. */
    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)
}

/**
 * Data Access Object for health metrics, hydration, sleep, and sensor steps.
 */
@Dao
interface HealthDao {
    /** Reactive stream of all historical health logs. */
    @Query("SELECT * FROM health_logs ORDER BY date DESC")
    fun getAllHealthLogs(): Flow<List<HealthLogEntity>>

    /** One-shot list snapshot of all health logs. */
    @Query("SELECT * FROM health_logs ORDER BY date DESC")
    suspend fun getAllHealthLogsList(): List<HealthLogEntity>

    /** Emits the latest recorded health log for dashboard widgets. */
    @Query("SELECT * FROM health_logs ORDER BY date DESC LIMIT 1")
    fun getLatestHealthLog(): Flow<HealthLogEntity?>

    /** Inserts or updates a health log entry. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHealthLog(healthLog: HealthLogEntity): Long

    /** Updates an existing health log. */
    @Update
    suspend fun updateHealthLog(healthLog: HealthLogEntity)

    /** Deletes all health logs. */
    @Query("DELETE FROM health_logs")
    suspend fun deleteAllHealthLogs()
}

/**
 * Data Access Object for reflective journal entries.
 */
@Dao
interface JournalDao {
    /** Reactive stream of all journal entries. */
    @Query("SELECT * FROM journal_entries ORDER BY date DESC")
    fun getAllJournalEntries(): Flow<List<JournalEntity>>

    /** One-shot list snapshot of all journal entries. */
    @Query("SELECT * FROM journal_entries ORDER BY date DESC")
    suspend fun getAllJournalEntriesList(): List<JournalEntity>

    /** Inserts or replaces a journal entry. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJournalEntry(journal: JournalEntity): Long

    /** Deletes all journal entries. */
    @Query("DELETE FROM journal_entries")
    suspend fun deleteAllJournalEntries()

    /** Deletes a single journal entry. */
    @Delete
    suspend fun deleteJournalEntry(journal: JournalEntity)
}

/**
 * Data Access Object for notes and interactive checklists.
 */
@Dao
interface NoteDao {
    /** Reactive stream of notes with pinned notes prioritized. */
    @Query("SELECT * FROM notes ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    /** One-shot list snapshot of all notes. */
    @Query("SELECT * FROM notes ORDER BY isPinned DESC, updatedAt DESC")
    suspend fun getAllNotesList(): List<NoteEntity>

    /** Inserts or replaces a note. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    /** Updates a note's content or pinned status. */
    @Update
    suspend fun updateNote(note: NoteEntity)

    /** Deletes all notes. */
    @Query("DELETE FROM notes")
    suspend fun deleteAllNotes()

    /** Deletes a single note. */
    @Delete
    suspend fun deleteNote(note: NoteEntity)
}

/**
 * Data Access Object for short-term and long-term goals.
 */
@Dao
interface GoalDao {
    /** Reactive stream of goals sorted by deadline proximity. */
    @Query("SELECT * FROM goals ORDER BY deadline ASC")
    fun getAllGoals(): Flow<List<GoalEntity>>

    /** One-shot list snapshot of all goals. */
    @Query("SELECT * FROM goals ORDER BY deadline ASC")
    suspend fun getAllGoalsList(): List<GoalEntity>

    /** Inserts or replaces a goal. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: GoalEntity): Long

    /** Updates goal progress or description. */
    @Update
    suspend fun updateGoal(goal: GoalEntity)

    /** Deletes all goals. */
    @Query("DELETE FROM goals")
    suspend fun deleteAllGoals()

    /** Deletes a single goal. */
    @Delete
    suspend fun deleteGoal(goal: GoalEntity)
}

/**
 * Data Access Object for calendar events and planner items.
 */
@Dao
interface CalendarDao {
    /** Reactive stream of calendar events ordered chronologically. */
    @Query("SELECT * FROM calendar_events ORDER BY startTime ASC")
    fun getAllEvents(): Flow<List<CalendarEventEntity>>

    /** One-shot list snapshot of all calendar events. */
    @Query("SELECT * FROM calendar_events ORDER BY startTime ASC")
    suspend fun getAllEventsList(): List<CalendarEventEntity>

    /** Inserts or replaces a calendar event. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: CalendarEventEntity): Long

    /** Deletes all calendar events. */
    @Query("DELETE FROM calendar_events")
    suspend fun deleteAllEvents()

    /** Deletes a single calendar event. */
    @Delete
    suspend fun deleteEvent(event: CalendarEventEntity)
}

/**
 * Data Access Object for user profile preferences and settings.
 */
@Dao
interface UserDao {
    /** Reactive stream observing the singleton user profile row (ID = 1). */
    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun getUserProfile(): Flow<UserProfileEntity?>

    /** One-shot retrieval of the singleton user profile. */
    @Query("SELECT * FROM user_profile WHERE id = 1")
    suspend fun getUserProfileSingle(): UserProfileEntity?

    /** Upserts the singleton user profile row. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateUserProfile(profile: UserProfileEntity)
}

/**
 * Data Access Object for custom task & expense categories.
 */
@Dao
interface CategoryDao {
    /** Reactive stream of all categories. */
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    /** One-shot snapshot of categories. */
    @Query("SELECT * FROM categories ORDER BY name ASC")
    suspend fun getAllCategoriesList(): List<CategoryEntity>

    /** Inserts or replaces a category. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    /** Deletes a category. */
    @Delete
    suspend fun deleteCategory(category: CategoryEntity)
}

/**
 * Data Access Object for Solo Leveling Hunter gamification stats.
 */
@Dao
interface UserStatsDao {
    /** Reactive stream observing singleton gamification stats (ID = 1). */
    @Query("SELECT * FROM user_stats WHERE id = 1")
    fun getUserStats(): Flow<UserStatsEntity?>

    /** One-shot retrieval of singleton gamification stats. */
    @Query("SELECT * FROM user_stats WHERE id = 1")
    suspend fun getUserStatsSingle(): UserStatsEntity?

    /** Upserts the singleton gamification stats record. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateUserStats(stats: UserStatsEntity)
}

/**
 * Data Access Object for collaborative Social Guild raid progression.
 */
@Dao
interface GuildProgressDao {
    /** Reactive stream observing singleton guild progression (ID = 1). */
    @Query("SELECT * FROM guild_progress WHERE id = 1")
    fun getGuildProgress(): Flow<GuildProgressEntity?>

    /** One-shot retrieval of guild progression. */
    @Query("SELECT * FROM guild_progress WHERE id = 1")
    suspend fun getGuildProgressSingle(): GuildProgressEntity?

    /** Upserts singleton guild progression record. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateGuildProgress(progress: GuildProgressEntity)
}
