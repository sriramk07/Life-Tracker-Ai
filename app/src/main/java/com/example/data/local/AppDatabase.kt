package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.*
import com.example.data.local.entities.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * ============================================================================
 * LIFE TRACKER AI — ROOM SQLite DATABASE CONTAINER (V4)
 * ============================================================================
 *
 * Central Room Database class configuring the local SQLite persistence layer.
 * Includes 12 entity models corresponding to all app modules:
 * - [CategoryEntity], [TaskEntity], [HabitEntity], [ExpenseEntity],
 *   [HealthLogEntity], [JournalEntity], [NoteEntity], [GoalEntity],
 *   [CalendarEventEntity], [UserProfileEntity], [UserStatsEntity],
 *   and [GuildProgressEntity].
 *
 * Features non-destructive SQLite schema migration ([MIGRATION_3_4]), thread-safe
 * Singleton instantiation, and asynchronous database seeding on creation.
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
        UserStatsEntity::class,
        GuildProgressEntity::class
    ],
    version = 4,
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
    abstract fun guildProgressDao(): GuildProgressDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Non-destructive Room migration from schema v3 (Review 2) to schema v4 (Release 1.0).
         * Adds `updatedAt` sync timestamps, persists RPG attributes on `user_stats`,
         * and creates the collaborative `guild_progress` table.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val now = System.currentTimeMillis()
                db.execSQL("ALTER TABLE categories ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT $now")
                db.execSQL("ALTER TABLE tasks ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT $now")
                db.execSQL("ALTER TABLE habits ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT $now")
                db.execSQL("ALTER TABLE expenses ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT $now")
                db.execSQL("ALTER TABLE health_logs ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT $now")
                db.execSQL("ALTER TABLE journal_entries ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT $now")
                db.execSQL("ALTER TABLE goals ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT $now")
                db.execSQL("ALTER TABLE calendar_events ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT $now")
                db.execSQL("ALTER TABLE user_profile ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT $now")

                db.execSQL("ALTER TABLE user_stats ADD COLUMN strength INTEGER NOT NULL DEFAULT 18")
                db.execSQL("ALTER TABLE user_stats ADD COLUMN agility INTEGER NOT NULL DEFAULT 22")
                db.execSQL("ALTER TABLE user_stats ADD COLUMN intelligence INTEGER NOT NULL DEFAULT 25")
                db.execSQL("ALTER TABLE user_stats ADD COLUMN vitality INTEGER NOT NULL DEFAULT 20")
                db.execSQL("ALTER TABLE user_stats ADD COLUMN sense INTEGER NOT NULL DEFAULT 19")
                db.execSQL("ALTER TABLE user_stats ADD COLUMN unallocatedPoints INTEGER NOT NULL DEFAULT 5")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS guild_progress (
                        id INTEGER NOT NULL PRIMARY KEY,
                        guildName TEXT NOT NULL,
                        guildMotto TEXT NOT NULL,
                        raidBossName TEXT NOT NULL,
                        raidBossHp INTEGER NOT NULL,
                        raidDamageDealt INTEGER NOT NULL,
                        personalContributionXp INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

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
                    .addMigrations(MIGRATION_3_4)
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
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
         * Asynchronously seeds initial categories, default user profile, hunter stats, and guild.
         */
        private suspend fun seedDatabase(db: AppDatabase) {
            val categories = listOf(
                CategoryEntity(id = 1, name = "Work", colorHex = "#3B82F6", iconName = "Work"),
                CategoryEntity(id = 2, name = "Personal", colorHex = "#10B981", iconName = "Person"),
                CategoryEntity(id = 3, name = "Health", colorHex = "#EF4444", iconName = "Favorite"),
                CategoryEntity(id = 4, name = "Finance", colorHex = "#F59E0B", iconName = "AttachMoney"),
                CategoryEntity(id = 5, name = "Study", colorHex = "#8B5CF6", iconName = "School")
            )
            categories.forEach { db.categoryDao().insertCategory(it) }

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

            db.userStatsDao().insertOrUpdateUserStats(
                UserStatsEntity(
                    id = 1,
                    totalXp = 0,
                    level = 1,
                    coins = 0,
                    streakDays = 0,
                    strength = 10,
                    agility = 10,
                    intelligence = 10,
                    vitality = 10,
                    sense = 10,
                    unallocatedPoints = 3
                )
            )

            db.guildProgressDao().insertOrUpdateGuildProgress(
                GuildProgressEntity(id = 1)
            )
        }
    }
}
