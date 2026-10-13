package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.*
import com.example.data.local.entities.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * ============================================================================
 * LIFE TRACKER AI — ROOM SQLite DATABASE CONTAINER
 * ============================================================================
 *
 * Central Room Database class configuring the local SQLite persistence layer.
 * Includes 11 entity models corresponding to all app modules:
 * - [CategoryEntity], [TaskEntity], [HabitEntity], [ExpenseEntity],
 *   [HealthLogEntity], [JournalEntity], [NoteEntity], [GoalEntity],
 *   [CalendarEventEntity], [UserProfileEntity], [UserStatsEntity].
 *
 * Thread-safe Singleton instantiation with destructive migration fallback and
 * asynchronous database seeding on creation.
 */
@Database(
    entities = [
        CategoryEntity::class,
        TaskEntity::class,
        HabitEntity::class,
        ExpenseEntity::class,
        HealthLogEntity::class,
        JournalEntity::class,
        NoteEntity::class,
        GoalEntity::class,
        CalendarEventEntity::class,
        UserProfileEntity::class,
        UserStatsEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao
    abstract fun taskDao(): TaskDao
    abstract fun habitDao(): HabitDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun healthDao(): HealthDao
    abstract fun journalDao(): JournalDao
    abstract fun noteDao(): NoteDao
    abstract fun goalDao(): GoalDao
    abstract fun calendarDao(): CalendarDao
    abstract fun userDao(): UserDao
    abstract fun userStatsDao(): UserStatsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Returns thread-safe singleton instance of [AppDatabase].
         *
         * @param context Application context used for Room builder initialization.
         * @return Initialized or cached [AppDatabase] instance.
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "life_tracker_ai_database"
                )
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Seed foundational categories and clean user state
                        CoroutineScope(Dispatchers.IO).launch {
                            INSTANCE?.let { seedDatabase(it) }
                        }
                    }
                    override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
                        super.onDestructiveMigration(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            INSTANCE?.let { seedDatabase(it) }
                        }
                    }
                })
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * Asynchronously seeds initial categories, default user profile, and level 1 hunter stats.
         */
        private suspend fun seedDatabase(db: AppDatabase) {
            // Seed Categories
            val categories = listOf(
                CategoryEntity(id = 1, name = "Work", colorHex = "#3B82F6", iconName = "Work"),
                CategoryEntity(id = 2, name = "Personal", colorHex = "#10B981", iconName = "Person"),
                CategoryEntity(id = 3, name = "Health", colorHex = "#EF4444", iconName = "Favorite"),
                CategoryEntity(id = 4, name = "Finance", colorHex = "#F59E0B", iconName = "AttachMoney"),
                CategoryEntity(id = 5, name = "Study", colorHex = "#8B5CF6", iconName = "School")
            )
            categories.forEach { db.categoryDao().insertCategory(it) }

            // Initial Clean User Profile
            db.userDao().insertOrUpdateUserProfile(
                UserProfileEntity(
                    id = 1,
                    name = "User",
                    email = "",
                    isDarkMode = true,
                    dailyWaterGoalMl = 2500,
                    dailyCalorieGoal = 2200,
                    dailyStepGoal = 10000,
                    monthlyBudget = 2000.0,
                    avatarId = 1
                )
            )

            // Initial Clean User Stats
            db.userStatsDao().insertOrUpdateUserStats(
                UserStatsEntity(
                    id = 1,
                    totalXp = 0,
                    level = 1,
                    coins = 0,
                    streakDays = 0
                )
            )
        }
    }
}
