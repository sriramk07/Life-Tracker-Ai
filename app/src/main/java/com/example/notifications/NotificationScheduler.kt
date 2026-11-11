package com.example.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import java.util.Calendar

/**
 * ============================================================================
 * NOTIFICATION SCHEDULER — SMART REMINDERS & DAILY BRIEFINGS ENGINE
 * ============================================================================
 *
 * Configures NotificationChannels (API 26+) and schedules persistent recurring
 * daily reminders via [AlarmManager] and [ReminderReceiver]:
 * - Morning Quest Briefing (Tasks)
 * - Habit Streak Shield Alerts (Habits)
 * - Hydration Interval Alerts (Health)
 * - Evening Financial Audit Prompt (Expenses)
 */

enum class ReminderCategory(
    val code: Int,
    val prefKey: String,
    val channelId: String,
    val title: String,
    val message: String,
    val defaultHour: Int,
    val defaultMinute: Int
) {
    TASKS(
        code = 1001,
        prefKey = "rem_tasks",
        channelId = "lifeos_tasks_channel",
        title = "Daily Quests Awaiting",
        message = "Open LifeOS AI to clear today's High-Priority quests and earn Hunter XP.",
        defaultHour = 9,
        defaultMinute = 0
    ),
    HABITS(
        code = 1002,
        prefKey = "rem_habits",
        channelId = "lifeos_habits_channel",
        title = "Protect Your Streak Shield",
        message = "Check off your Daily Duties before midnight to keep your streak intact!",
        defaultHour = 19,
        defaultMinute = 30
    ),
    WATER(
        code = 1003,
        prefKey = "rem_water",
        channelId = "lifeos_wellness_channel",
        title = "Hydration Check-In",
        message = "Time to log +250ml of water toward your daily 2,500ml Vitality target.",
        defaultHour = 14,
        defaultMinute = 0
    ),
    EXPENSES(
        code = 1004,
        prefKey = "rem_expenses",
        channelId = "lifeos_finance_channel",
        title = "Evening Treasury Audit",
        message = "Log today's expenses to keep your Edge AI budget forecast accurate.",
        defaultHour = 21,
        defaultMinute = 0
    )
}

object NotificationScheduler {

    private const val PREFS_NAME = "lifeos_reminder_prefs"
    const val EXTRA_CATEGORY_NAME = "extra_reminder_category"

    /** Creates notification channels on Android 8.0+ (Oreo) safely. */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val channels = listOf(
            NotificationChannel("lifeos_tasks_channel", "Quest & Task Alerts", NotificationManager.IMPORTANCE_DEFAULT),
            NotificationChannel("lifeos_habits_channel", "Habit Streak Protection", NotificationManager.IMPORTANCE_HIGH),
            NotificationChannel("lifeos_wellness_channel", "Hydration & Biometric Reminders", NotificationManager.IMPORTANCE_DEFAULT),
            NotificationChannel("lifeos_finance_channel", "Financial Budget Audits", NotificationManager.IMPORTANCE_LOW)
        )
        channels.forEach { manager.createNotificationChannel(it) }
    }

    /** Persists toggle preference and schedules or cancels the alarm accordingly. */
    fun setReminderEnabled(context: Context, category: ReminderCategory, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(category.prefKey, enabled)
            .apply()

        if (enabled) {
            scheduleDailyReminder(context, category)
        } else {
            cancelReminder(context, category)
        }
    }

    fun isReminderEnabled(context: Context, category: ReminderCategory, defaultEnabled: Boolean = true): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(category.prefKey, defaultEnabled)
    }

    /** Computes next epoch trigger time for a given hour/minute pair. */
    fun computeNextTriggerMillis(hour: Int, minute: Int, nowMillis: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (cal.timeInMillis <= nowMillis) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }

    fun scheduleDailyReminder(context: Context, category: ReminderCategory) {
        createNotificationChannels(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val triggerAt = computeNextTriggerMillis(category.defaultHour, category.defaultMinute)

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(EXTRA_CATEGORY_NAME, category.name)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            category.code,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
            Log.d("NotificationScheduler", "Scheduled daily ${category.name} reminder.")
        } catch (e: SecurityException) {
            Log.w("NotificationScheduler", "Alarm schedule skipped cleanly: ${e.message}")
        }
    }

    fun cancelReminder(context: Context, category: ReminderCategory) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            category.code,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
        }
    }

    /** Re-registers all enabled alarms after device reboot. */
    fun rescheduleAllEnabled(context: Context) {
        ReminderCategory.entries.forEach { cat ->
            val defaultVal = cat != ReminderCategory.EXPENSES
            if (isReminderEnabled(context, cat, defaultVal)) {
                scheduleDailyReminder(context, cat)
            }
        }
    }

    /** Dispatches an immediate notification card (safe if permission revoked). */
    fun dispatchNotification(context: Context, category: ReminderCategory) {
        createNotificationChannels(context)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val notification = NotificationCompat.Builder(context, category.channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(category.title)
            .setContentText(category.message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        try {
            manager.notify(category.code, notification)
        } catch (e: SecurityException) {
            Log.w("NotificationScheduler", "POST_NOTIFICATIONS permission required: ${e.message}")
        }
    }
}
