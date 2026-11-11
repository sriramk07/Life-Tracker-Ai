package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * ============================================================================
 * REMINDER RECEIVER — ALARM & DEVICE BOOT HANDLER
 * ============================================================================
 *
 * Receives scheduled [AlarmManager] reminder broadcasts and dispatches
 * high-signal notifications. Also restores recurring schedules upon
 * [Intent.ACTION_BOOT_COMPLETED].
 */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            NotificationScheduler.rescheduleAllEnabled(context)
            return
        }

        val categoryName = intent.getStringExtra(NotificationScheduler.EXTRA_CATEGORY_NAME) ?: return
        val category = runCatching { ReminderCategory.valueOf(categoryName) }.getOrNull() ?: return
        if (NotificationScheduler.isReminderEnabled(context, category, true)) {
            NotificationScheduler.dispatchNotification(context, category)
        }
    }
}
