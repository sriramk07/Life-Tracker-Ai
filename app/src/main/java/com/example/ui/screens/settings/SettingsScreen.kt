package com.example.ui.screens.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.sync.SyncReport
import com.example.notifications.NotificationScheduler
import com.example.notifications.ReminderCategory
import com.example.security.SecurityManager
import com.example.ui.components.GlassmorphicCard
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentEmerald

/**
 * ============================================================================
 * SETTINGS & PREFERENCES SCREEN (RELEASE 1.0 — 100% PRODUCTION)
 * ============================================================================
 *
 * Provides unified configuration for:
 * 1. Bidirectional LWW Cloud Sync status, conflict count, and manual trigger.
 * 2. Vault PIN & Biometric Security Lock toggle + Encrypted Backup passphrase.
 * 3. Persistent Smart Notification reminders via [NotificationScheduler].
 * 4. Theme switching (Dark / Light mode).
 * 5. SHA-256 Verified JSON Backup & Restoration engine.
 * 6. Clean database reset routines.
 */
@Composable
fun SettingsScreen(
    isDarkMode: Boolean,
    userEmail: String = "user@gmail.com",
    syncStatus: String = "Synced",
    syncReport: SyncReport? = null,
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

    var taskReminder by remember {
        mutableStateOf(NotificationScheduler.isReminderEnabled(context, ReminderCategory.TASKS, true))
    }
    var habitReminder by remember {
        mutableStateOf(NotificationScheduler.isReminderEnabled(context, ReminderCategory.HABITS, true))
    }
    var waterReminder by remember {
        mutableStateOf(NotificationScheduler.isReminderEnabled(context, ReminderCategory.WATER, true))
    }
    var expenseReminder by remember {
        mutableStateOf(NotificationScheduler.isReminderEnabled(context, ReminderCategory.EXPENSES, false))
    }

    var isPinLockEnabled by remember { mutableStateOf(SecurityManager.isAppLockEnabled(context)) }
    var showPinSetupDialog by remember { mutableStateOf(false) }
    var newPinText by remember { mutableStateOf("") }

    var showClearDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var restoreInputText by remember { mutableStateOf("") }

    // Configure PIN Dialog
    if (showPinSetupDialog) {
        AlertDialog(
            onDismissRequest = { showPinSetupDialog = false },
            title = { Text("Configure Security Vault PIN") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter a 4-to-8 digit PIN. It is stored salted using SHA-256 and works alongside Biometric unlock.", fontSize = 13.sp)
                    OutlinedTextField(
                        value = newPinText,
                        onValueChange = { if (it.length <= 8) newPinText = it },
                        label = { Text("New Vault PIN") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth().testTag("setup_pin_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPinText.length >= 4) {
                            SecurityManager.saveAppPin(context, newPinText)
                            isPinLockEnabled = true
                            newPinText = ""
                            showPinSetupDialog = false
                            Toast.makeText(context, "Vault Security Lock enabled!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "PIN must be at least 4 digits.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.testTag("confirm_pin_btn")
                ) {
                    Text("Save & Enable Lock")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinSetupDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Backup JSON Dialog
    if (backupJson != null) {
        AlertDialog(
            onDismissRequest = onClearBackupJson,
            title = { Text("SHA-256 Verified Database Backup JSON") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Your complete database (v3 schema + SHA-256 integrity hash) is ready to copy or archive.", fontSize = 13.sp)
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
                TextButton(onClick = onClearBackupJson) { Text("Close") }
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
                    Text("Paste a valid LifeOS AI v2/v3 backup JSON below. Integrity checksums are verified automatically before writing.", fontSize = 13.sp)
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
                TextButton(onClick = { showRestoreDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (restoreMessage != null) {
        AlertDialog(
            onDismissRequest = onClearRestoreMessage,
            title = { Text("Restore Status") },
            text = { Text(restoreMessage) },
            confirmButton = {
                Button(onClick = onClearRestoreMessage) { Text("OK") }
            }
        )
    }

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
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
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

        // Bidirectional Cloud Sync Card
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
                            text = "Bidirectional LWW Cloud Sync",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Connected: $userEmail",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (syncReport != null) {
                            Text(
                                text = "Last merge: ${syncReport.pushedToCloud} pushed • ${syncReport.pulledFromCloud} pulled • ${syncReport.conflictsResolved} conflicts resolved",
                                fontSize = 11.sp,
                                color = AccentCyan
                            )
                        }
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
                    Text("Run Two-Way Conflict-Free Sync", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }
        }

        // Security Vault & Biometric Lock Card
        item {
            val biometricsAvailable = remember { SecurityManager.canAuthenticateBiometrics(context) }
            GlassmorphicCard(modifier = Modifier.testTag("security_vault_card")) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Lock, contentDescription = "Security Lock", tint = AccentCyan)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Biometric & PIN Vault Lock", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(
                                text = if (biometricsAvailable) "Biometric hardware ready • AES-256 KeyStore active"
                                else "Salted SHA-256 PIN lock protection",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = isPinLockEnabled,
                        onCheckedChange = { shouldEnable ->
                            if (shouldEnable) {
                                showPinSetupDialog = true
                            } else {
                                SecurityManager.disableAppLock(context)
                                isPinLockEnabled = false
                                Toast.makeText(context, "Vault PIN lock disabled.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.testTag("security_lock_switch")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { showPinSetupDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isPinLockEnabled) "Change Vault PIN" else "Set Vault PIN")
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

        // Persistent Smart Reminders
        item {
            GlassmorphicCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Smart Background Reminders", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Task Due Reminders (9:00 AM)")
                    Switch(checked = taskReminder, onCheckedChange = {
                        taskReminder = it
                        NotificationScheduler.setReminderEnabled(context, ReminderCategory.TASKS, it)
                    })
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Habit Streak Shield (7:30 PM)")
                    Switch(checked = habitReminder, onCheckedChange = {
                        habitReminder = it
                        NotificationScheduler.setReminderEnabled(context, ReminderCategory.HABITS, it)
                    })
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Water Intake Reminders (2:00 PM)")
                    Switch(checked = waterReminder, onCheckedChange = {
                        waterReminder = it
                        NotificationScheduler.setReminderEnabled(context, ReminderCategory.WATER, it)
                    })
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Daily Expense Audit (9:00 PM)")
                    Switch(checked = expenseReminder, onCheckedChange = {
                        expenseReminder = it
                        NotificationScheduler.setReminderEnabled(context, ReminderCategory.EXPENSES, it)
                    })
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        NotificationScheduler.dispatchNotification(context, ReminderCategory.HABITS)
                        Toast.makeText(context, "Test Streak Shield reminder dispatched!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Send Test Streak Notification")
                }
            }
        }

        // Backup & Restore
        item {
            GlassmorphicCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Backup, contentDescription = "Backup", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Backup & Restore Data (SHA-256)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Generate an integrity-verified v3 JSON backup across all 12 database tables or restore an existing archive.",
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
