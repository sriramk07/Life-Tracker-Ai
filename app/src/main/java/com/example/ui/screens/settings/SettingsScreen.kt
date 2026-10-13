package com.example.ui.screens.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GlassmorphicCard
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentEmerald

/**
 * ============================================================================
 * SETTINGS & PREFERENCES SCREEN (REVIEW 2 - 75% MILESTONE)
 * ============================================================================
 *
 * Provides control over:
 * 1. Cloud synchronization status and manual trigger to Firebase Firestore.
 * 2. Theme switching (Dark / Light mode).
 * 3. Notification reminder preferences (Tasks, Habits, Hydration, Expenses).
 * 4. Full JSON Backup generation & clipboard export.
 * 5. Full JSON Restoration with schema validation and error boundaries.
 * 6. Clean database reset routines.
 */
@Composable
fun SettingsScreen(
    isDarkMode: Boolean,
    userEmail: String = "user@gmail.com",
    syncStatus: String = "Synced",
    backupJson: String? = null,
    restoreMessage: String? = null,
    onToggleDarkMode: (Boolean) -> Unit,
    onBackupData: () -> Unit,
    onRestoreData: (String) -> Unit = {},
    onClearBackupJson: () -> Unit = {},
    onClearRestoreMessage: () -> Unit = {},
    onTriggerCloudSync: () -> Unit = {},
    onClearAllData: () -> Unit = {}
) {
    val context = LocalContext.current
    var taskReminder by remember { mutableStateOf(true) }
    var habitReminder by remember { mutableStateOf(true) }
    var waterReminder by remember { mutableStateOf(true) }
    var expenseReminder by remember { mutableStateOf(false) }

    var showClearDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var restoreInputText by remember { mutableStateOf("") }

    // Backup JSON Dialog
    if (backupJson != null) {
        AlertDialog(
            onDismissRequest = onClearBackupJson,
            title = { Text("Database Backup JSON") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Your complete local database has been serialized into formatted JSON. You can copy it to your clipboard for external backup or migration.", fontSize = 13.sp)
                    OutlinedTextField(
                        value = backupJson.take(1500) + if (backupJson.length > 1500) "\n... [truncated]" else "",
                        onValueChange = {},
                        readOnly = true,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth().testTag("backup_json_preview")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("LifeTrackerBackup", backupJson)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Backup JSON copied to clipboard! 📋", Toast.LENGTH_SHORT).show()
                        onClearBackupJson()
                    },
                    modifier = Modifier.testTag("copy_backup_btn")
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy to Clipboard")
                }
            },
            dismissButton = {
                TextButton(onClick = onClearBackupJson) {
                    Text("Close")
                }
            }
        )
    }

    // Restore Data Dialog
    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("Restore From Backup JSON") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Paste a valid LifeOS AI backup JSON below to restore your tasks, habits, expenses, health logs, and stats.", fontSize = 13.sp)
                    OutlinedTextField(
                        value = restoreInputText,
                        onValueChange = { restoreInputText = it },
                        placeholder = { Text("Paste JSON payload here...") },
                        minLines = 4,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth().testTag("restore_json_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (restoreInputText.isNotBlank()) {
                            onRestoreData(restoreInputText)
                            showRestoreDialog = false
                            restoreInputText = ""
                        }
                    },
                    modifier = Modifier.testTag("confirm_restore_btn")
                ) {
                    Text("Restore Database")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Restore Status Feedback Dialog
    if (restoreMessage != null) {
        AlertDialog(
            onDismissRequest = onClearRestoreMessage,
            title = { Text("Restore Status") },
            text = { Text(restoreMessage) },
            confirmButton = {
                Button(onClick = onClearRestoreMessage) {
                    Text("OK")
                }
            }
        )
    }

    // Clear All Confirmation Dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Delete All User Data") },
            text = { Text("Are you sure you want to permanently delete all tasks, habits, expenses, health logs, journal entries, notes, goals, and calendar events? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAllData()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_all_btn")
                ) {
                    Text("Delete Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("settings_screen"),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Settings & Preferences",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Firebase Cloud Sync Card
        item {
            GlassmorphicCard(modifier = Modifier.testTag("firebase_sync_card")) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = if (syncStatus == "Synced") Icons.Default.CloudDone else Icons.Default.CloudSync,
                        contentDescription = "Cloud Sync",
                        tint = AccentCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Firebase Cloud Sync",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Connected: $userEmail",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        color = if (syncStatus == "Syncing") AccentCyan.copy(alpha = 0.2f) else AccentEmerald.copy(alpha = 0.2f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = if (syncStatus == "Syncing") "Syncing..." else "Synced",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (syncStatus == "Syncing") AccentCyan else AccentEmerald,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onTriggerCloudSync,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("sync_now_btn")
                ) {
                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sync Data Now with Google Account", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }
        }

        // Appearance
        item {
            GlassmorphicCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DarkMode, contentDescription = "Dark Mode", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Dark Theme", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = onToggleDarkMode,
                        modifier = Modifier.testTag("dark_mode_switch")
                    )
                }
            }
        }

        // Notification Reminders
        item {
            GlassmorphicCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Reminders & Notifications", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Task Due Reminders")
                    Switch(checked = taskReminder, onCheckedChange = { taskReminder = it })
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Habit Streak Reminders")
                    Switch(checked = habitReminder, onCheckedChange = { habitReminder = it })
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Water Intake Reminders")
                    Switch(checked = waterReminder, onCheckedChange = { waterReminder = it })
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Daily Expense Log Reminder")
                    Switch(checked = expenseReminder, onCheckedChange = { expenseReminder = it })
                }
            }
        }

        // Backup & Restore
        item {
            GlassmorphicCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Backup, contentDescription = "Backup", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Backup & Restore Data", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Generate a full JSON backup of all local tables or restore an existing backup file.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onBackupData,
                        modifier = Modifier.weight(1f).testTag("backup_data_btn")
                    ) {
                        Text("Backup JSON")
                    }
                    OutlinedButton(
                        onClick = { showRestoreDialog = true },
                        modifier = Modifier.weight(1f).testTag("restore_data_btn")
                    ) {
                        Text("Restore Data")
                    }
                }
            }
        }

        // Data Reset Section
        item {
            GlassmorphicCard {
                Text(
                    text = "Data Management",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Reset all database tables to start fresh with clean user state.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = { showClearDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("clear_all_data_btn")
                ) {
                    Text("Clear All Data & Start Fresh", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
