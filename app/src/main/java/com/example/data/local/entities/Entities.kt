package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * ============================================================================
 * LIFE TRACKER AI — ROOM DATABASE ENTITIES & SCHEMA DEFINITIONS
 * ============================================================================
 *
 * Defines the SQLite table structures for offline-first persistence.
 * Each entity maps to a distinct life dimension (productivity, habits, finance,
 * physical health, mental wellbeing, notes, goals, calendar, and gamification).
 */

/**
 * Represents a customizable user category for organizing tasks, expenses, and notes.
 *
 * @property id Auto-generated 64-bit integer primary key.
 * @property name Human-readable category label (e.g., "Work", "Fitness").
 * @property colorHex Color code in hexadecimal format for UI badge rendering.
 * @property iconName Material icon identifier string.
 */
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorHex: String = "#3B82F6",
    val iconName: String = "Folder"
)

/**
 * Represents a priority quest or task action item.
 *
 * @property id Auto-generated 64-bit integer primary key.
 * @property title Concise description of the quest or action item.
 * @property description Detailed notes or sub-steps for quest execution.
 * @property isCompleted Completion flag. Setting to true triggers XP rewards.
 * @property priority Urgency classification: "High" (100 XP), "Medium" (50 XP), "Low" (25 XP).
 * @property category Category grouping label (e.g., "Work", "Personal").
 * @property categoryId Foreign reference ID to [CategoryEntity].
 * @property dueDate Epoch timestamp (ms) when this task is expected to finish.
 * @property reminderTime Epoch timestamp (ms) for notification trigger.
 * @property isRecurring Flag denoting whether task repeats periodically.
 * @property repeatFrequency Recurrence interval: "Daily", "Weekly", "Monthly", or "None".
 */
@Entity(
    tableName = "tasks",
    indices = [Index(value = ["dueDate"]), Index(value = ["isCompleted"])]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val isCompleted: Boolean = false,
    val priority: String = "Medium",
    val category: String = "Personal",
    val categoryId: Long = 1L,
    val dueDate: Long = System.currentTimeMillis(),
    val reminderTime: Long = 0,
    val isRecurring: Boolean = false,
    val repeatFrequency: String = "None"
)

/**
 * Represents a recurring atomic habit / daily duty for behavioral conditioning.
 *
 * @property id Auto-generated 64-bit primary key.
 * @property name Habit label (e.g., "Mindfulness Meditation", "Hydrate 2.5L").
 * @property category Domain classification (e.g., "Health", "Mental", "Learning").
 * @property frequency Recurrence cycle: "Daily", "Weekly", "Monthly".
 * @property streakCount Total consecutive intervals completed without breaking.
 * @property isCompletedToday Daily checklist state reset each morning.
 * @property lastCompletedDate Epoch timestamp (ms) when habit was last toggled.
 * @property targetDaysPerWeek Target execution frequency per 7-day cycle.
 */
@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String = "Health",
    val frequency: String = "Daily",
    val streakCount: Int = 0,
    val isCompletedToday: Boolean = false,
    val lastCompletedDate: Long = 0,
    val targetDaysPerWeek: Int = 7
)

/**
 * Represents a financial transaction (income or expense item).
 *
 * @property id Auto-generated 64-bit primary key.
 * @property title Transaction description or merchant name.
 * @property amount Numeric value in dollars ($ USD).
 * @property type Transaction nature: "Expense" or "Income".
 * @property category Budget bucket (e.g., "Food", "Shopping", "Bills", "Salary").
 * @property date Epoch timestamp (ms) when the transaction took place.
 * @property notes Additional context or receipt metadata.
 */
@Entity(
    tableName = "expenses",
    indices = [Index(value = ["date"]), Index(value = ["category"])]
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amount: Double,
    val type: String = "Expense",
    val category: String = "General",
    val date: Long = System.currentTimeMillis(),
    val notes: String = ""
)

/**
 * Represents a daily biometric and wellness snapshot.
 *
 * @property id Auto-generated 64-bit primary key.
 * @property date Epoch timestamp (ms) of the health record.
 * @property waterIntakeMl Cumulative water consumption in milliliters (target: 2500ml).
 * @property sleepHours Duration of rest in hours (e.g., 7.5f).
 * @property weightKg Body mass in kilograms.
 * @property heightCm Stature in centimeters for BMI calculation.
 * @property stepsCount Pedometer step tally.
 * @property caloriesBurned Estimated active metabolic expenditure.
 * @property mood Emotional valence emoji tag (e.g., "😊 Happy", "⚡ Energetic").
 * @property workoutDurationMins Physical exercise duration in minutes.
 * @property workoutType Exercise classification (e.g., "HIIT Cardio", "Strength").
 */
@Entity(tableName = "health_logs")
data class HealthLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long = System.currentTimeMillis(),
    val waterIntakeMl: Int = 0,
    val sleepHours: Float = 0f,
    val weightKg: Float = 0f,
    val heightCm: Float = 175f,
    val stepsCount: Int = 0,
    val caloriesBurned: Int = 0,
    val mood: String = "😊 Happy",
    val workoutDurationMins: Int = 0,
    val workoutType: String = "General Workout"
)

/**
 * Represents an evening or morning reflective journal entry.
 *
 * @property id Auto-generated 64-bit primary key.
 * @property title Entry header or reflection theme.
 * @property content Full Markdown/text reflections.
 * @property date Epoch timestamp (ms) of composition.
 * @property moodTag Qualitative emotional sentiment (e.g., "Calm", "Grateful").
 * @property tags Comma-separated categorization tags.
 * @property photoUri Optional local URI to an attached media snapshot.
 * @property voiceNoteSnippet Optional transcription snippet of a voice recording.
 */
@Entity(tableName = "journal_entries")
data class JournalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val date: Long = System.currentTimeMillis(),
    val moodTag: String = "Calm",
    val tags: String = "Personal",
    val photoUri: String? = null,
    val voiceNoteSnippet: String? = null
)

/**
 * Represents a fast-capture note, ideation snippet, or checklist.
 *
 * @property id Auto-generated 64-bit primary key.
 * @property title Note title or subject line.
 * @property content Detailed note body text.
 * @property isChecklist Flag enabling interactive checkbox rendering.
 * @property checklistItems Delimited or JSON-encoded checklist rows.
 * @property isPinned Flag prioritizing note to the top of list.
 * @property folder Folder directory name (e.g., "Routine", "Shopping", "Ideas").
 * @property updatedAt Epoch timestamp (ms) of the last mutation.
 */
@Entity(
    tableName = "notes",
    indices = [Index(value = ["isPinned", "updatedAt"])]
)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val isChecklist: Boolean = false,
    val checklistItems: String = "",
    val isPinned: Boolean = false,
    val folder: String = "General",
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Represents a strategic life aspiration or milestone target.
 *
 * @property id Auto-generated 64-bit primary key.
 * @property title Objective statement.
 * @property description Detailed roadmap or motivation context.
 * @property category Area of life (e.g., "Career", "Finance", "Fitness").
 * @property type Time horizon: "Short-term" (< 3 months) or "Long-term".
 * @property progressPercent Current completion level from 0 to 100%.
 * @property deadline Target completion timestamp (ms).
 * @property milestones Pipe-separated sub-deliverables.
 */
@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val category: String = "Career",
    val type: String = "Short-term",
    val progressPercent: Int = 0,
    val deadline: Long = System.currentTimeMillis() + 86400000L * 30,
    val milestones: String = ""
)

/**
 * Represents a scheduled calendar event or time-blocked appointment.
 *
 * @property id Auto-generated 64-bit primary key.
 * @property title Event title.
 * @property description Detailed event agenda.
 * @property startTime Start time in epoch ms.
 * @property endTime End time in epoch ms.
 * @property eventType Category: "Event", "Birthday", "Reminder", "Task".
 * @property location Physical address or virtual meeting room URI.
 */
@Entity(
    tableName = "calendar_events",
    indices = [Index(value = ["startTime"])]
)
data class CalendarEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long = System.currentTimeMillis() + 3600000L,
    val eventType: String = "Event",
    val location: String = ""
)

/**
 * Represents global user preferences, targets, and system settings.
 * Single-row table with ID = 1.
 *
 * @property id Singleton primary key (fixed to 1).
 * @property name Display name of the user.
 * @property email Authenticated Google Account email address.
 * @property isDarkMode System color theme preference.
 * @property dailyWaterGoalMl Target water consumption (default 2500 ml).
 * @property dailyCalorieGoal Daily calorie burn or intake target.
 * @property dailyStepGoal Target daily physical steps (default 10000).
 * @property monthlyBudget Financial spending threshold ($ USD).
 * @property avatarId Selected avatar visual identifier index.
 */
@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1,
    val name: String = "Alex Rivera",
    val email: String = "alex.rivera@lifetracker.ai",
    val isDarkMode: Boolean = true,
    val dailyWaterGoalMl: Int = 2500,
    val dailyCalorieGoal: Int = 2200,
    val dailyStepGoal: Int = 10000,
    val monthlyBudget: Double = 3000.0,
    val avatarId: Int = 1
)

/**
 * Represents Solo Leveling / Monarch System RPG Gamification stats.
 * Single-row table with ID = 1.
 *
 * @property id Singleton primary key (fixed to 1).
 * @property totalXp Cumulative Experience Points earned from quests and habits.
 * @property level Current calculated Hunter Rank level (Level = XP / 500 + 1).
 * @property coins In-app currency awarded for quest completion.
 * @property streakDays Highest continuous overall activity streak.
 * @property updatedAt Epoch timestamp of last stat modification.
 */
@Entity(tableName = "user_stats")
data class UserStatsEntity(
    @PrimaryKey val id: Int = 1,
    val totalXp: Int = 2450,
    val level: Int = 5,
    val coins: Int = 450,
    val streakDays: Int = 21,
    val updatedAt: Long = System.currentTimeMillis()
)
