package com.example.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.TaskEntity
import com.example.data.local.entities.UserProfileEntity
import com.example.data.local.entities.UserStatsEntity
import com.example.domain.model.AppModule
import com.example.ui.components.CircularProgressGauge
import com.example.ui.components.GamificationCard
import com.example.ui.components.GlassmorphicCard
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    userProfile: UserProfileEntity?,
    userStats: UserStatsEntity? = null,
    tasks: List<TaskEntity>,
    expensesTotalToday: Double,
    waterIntakeMl: Int,
    sleepHours: Float,
    habitStreakDays: Int,
    habitCompletionPercent: Int,
    goalProgressPercent: Int,
    onNavigate: (AppModule) -> Unit,
    onAddWater: (Int) -> Unit,
    onToggleTask: (TaskEntity) -> Unit
) {
    val dateString = SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault()).format(Date())
    val userName = userProfile?.name?.takeIf { it.isNotBlank() } ?: "User"

    val totalTasksToday = tasks.size
    val completedTasksToday = tasks.count { it.isCompleted }
    val calculatedProgressPercent = if (totalTasksToday > 0) {
        (completedTasksToday * 100 / totalTasksToday)
    } else {
        habitCompletionPercent
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_screen")
    ) {
        val isWideScreen = maxWidth >= 600.dp

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (isWideScreen) 24.dp else 18.dp),
            contentPadding = PaddingValues(top = 20.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            // Large Elegant Header Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = ElectricBlue.copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.6f))
                            ) {
                                Text(
                                    text = "SHADOW MONARCH SYSTEM",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = ElectricBlue,
                                    letterSpacing = 1.5.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "S-RANK REAWAKENED",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentCyan
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "LifeOS AI",
                            style = MaterialTheme.typography.displayLarge,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "Welcome Sovereign $userName • $dateString",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                    }

                    IconButton(
                        onClick = { onNavigate(AppModule.PROFILE) },
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .size(52.dp)
                            .testTag("dashboard_profile_btn")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(
                                    androidx.compose.ui.graphics.Brush.radialGradient(
                                        colors = listOf(
                                            ElectricBlue.copy(alpha = 0.4f),
                                            Color(0xFF0A1025)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = "Profile",
                                tint = ElectricBlue
                            )
                        }
                    }
                }
            }

            // Today's Progress Floating Glass Card
            item {
                GlassmorphicCard(
                    cornerRadius = 28.dp,
                    modifier = Modifier.testTag("todays_progress_card")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Today's Progress",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Personal Productivity Matrix",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        CircularProgressGauge(
                            progressPercent = calculatedProgressPercent,
                            activeColor = AccentIndigo,
                            modifier = Modifier.size(68.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("🔥 Habit Streak", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$habitStreakDays Days", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = AccentEmerald)
                        }
                        Column {
                            Text("📋 Tasks Left", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${tasks.count { !it.isCompleted }}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = AccentIndigo)
                        }
                        Column {
                            Text("💰 Expense", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$${String.format(Locale.getDefault(), "%.2f", expensesTotalToday)}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = AccentCoral)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val waterGlasses = waterIntakeMl / 250
                        val waterTargetGlasses = (userProfile?.dailyWaterGoalMl ?: 2500) / 250
                        Column {
                            Text("💧 Water", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$waterGlasses / $waterTargetGlasses", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = AccentCyan)
                        }
                        Column {
                            Text("😴 Sleep", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${sleepHours} hrs", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = AccentPurple)
                        }
                        Column {
                            Text("⚡ Energy", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$calculatedProgressPercent%", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = AccentEmerald)
                        }
                    }
                }
            }

            // AI Assistant Insight Hero Banner Floating Glass Card
            item {
                GlassmorphicCard(
                    onClick = { onNavigate(AppModule.AI_ASSISTANT) },
                    cornerRadius = 28.dp,
                    backgroundColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                    modifier = Modifier.testTag("dashboard_ai_banner")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = "AI Assistant",
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "AI Life Assistant",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "All systems synced. Tap to request today's personalized habits & productivity digest.",
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                )
                            }
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Open AI",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }

            // Gamification & Rewards Card
            item {
                val stats = userStats ?: UserStatsEntity()
                GamificationCard(
                    profile = com.example.domain.model.GamificationProfile(
                        totalXp = stats.totalXp,
                        coins = stats.coins,
                        streakDays = habitStreakDays,
                        stats = com.example.domain.model.HunterStats(
                            strength = stats.strength,
                            agility = stats.agility,
                            intelligence = stats.intelligence,
                            vitality = stats.vitality,
                            sense = stats.sense,
                            unallocatedPoints = stats.unallocatedPoints
                        )
                    ),
                    onViewAllBadges = { onNavigate(AppModule.PROFILE) }
                )
            }

            // Overview Section Header
            item {
                Text(
                    text = "Life Summary & Trackers",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Summary Data Cards - Responsive Multi-Column Layout
            item {
                if (isWideScreen) {
                    // 2-Column Grid on Wide Screens
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TasksSummaryCard(
                                tasks = tasks,
                                onNavigate = onNavigate,
                                modifier = Modifier.weight(1f)
                            )
                            HabitsSummaryCard(
                                streakDays = habitStreakDays,
                                completionPercent = habitCompletionPercent,
                                onNavigate = onNavigate,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ExpensesSummaryCard(
                                expensesTotalToday = expensesTotalToday,
                                onNavigate = onNavigate,
                                modifier = Modifier.weight(1f)
                            )
                            HealthSummaryCard(
                                waterIntakeMl = waterIntakeMl,
                                sleepHours = sleepHours,
                                onAddWater = onAddWater,
                                onNavigate = onNavigate,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                } else {
                    // Single Column / 2x2 Compact Grid on Compact Screens
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TasksSummaryCard(
                                tasks = tasks,
                                onNavigate = onNavigate,
                                modifier = Modifier.weight(1f)
                            )
                            HabitsSummaryCard(
                                streakDays = habitStreakDays,
                                completionPercent = habitCompletionPercent,
                                onNavigate = onNavigate,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ExpensesSummaryCard(
                                expensesTotalToday = expensesTotalToday,
                                onNavigate = onNavigate,
                                modifier = Modifier.weight(1f)
                            )
                            HealthSummaryCard(
                                waterIntakeMl = waterIntakeMl,
                                sleepHours = sleepHours,
                                onAddWater = onAddWater,
                                onNavigate = onNavigate,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Goals Progress Banner
            item {
                OutlinedCard(
                    onClick = { onNavigate(AppModule.GOALS) },
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.TrackChanges,
                                        contentDescription = "Goals",
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Goals Progress",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "$goalProgressPercent% Overall Completion",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        CircularProgressGauge(
                            progressPercent = goalProgressPercent,
                            modifier = Modifier.size(48.dp),
                            strokeWidth = 6.dp,
                            activeColor = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            // Today's Priority Tasks List
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Today's Priorities",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    TextButton(onClick = { onNavigate(AppModule.TASKS) }) {
                        Text("View All")
                    }
                }
            }

            if (tasks.isEmpty()) {
                item {
                    GlassmorphicCard {
                        Text(
                            text = "🎉 All tasks for today are completed!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(tasks.take(3)) { task ->
                    GlassmorphicCard {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = task.isCompleted,
                                onCheckedChange = { onToggleTask(task) },
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = task.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${task.category} • Priority: ${task.priority}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Quick Access Modules
            item {
                Text(
                    text = "Quick Access",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ShortcutChip(
                        label = "Journal",
                        icon = Icons.Default.Book,
                        modifier = Modifier.weight(1f)
                    ) { onNavigate(AppModule.JOURNAL) }
                    ShortcutChip(
                        label = "Notes",
                        icon = Icons.Default.StickyNote2,
                        modifier = Modifier.weight(1f)
                    ) { onNavigate(AppModule.NOTES) }
                    ShortcutChip(
                        label = "Calendar",
                        icon = Icons.Default.CalendarMonth,
                        modifier = Modifier.weight(1f)
                    ) { onNavigate(AppModule.CALENDAR) }
                    ShortcutChip(
                        label = "Reports",
                        icon = Icons.Default.BarChart,
                        modifier = Modifier.weight(1f)
                    ) { onNavigate(AppModule.REPORTS) }
                }
            }
        }
    }
}

@Composable
private fun TasksSummaryCard(
    tasks: List<TaskEntity>,
    onNavigate: (AppModule) -> Unit,
    modifier: Modifier = Modifier
) {
    val pendingCount = tasks.count { !it.isCompleted }
    GlassmorphicCard(
        onClick = { onNavigate(AppModule.TASKS) },
        modifier = modifier
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.TaskAlt,
                contentDescription = "Tasks",
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Tasks", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text("$pendingCount Pending", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(4.dp))
        Text("Active Action Items", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun HabitsSummaryCard(
    streakDays: Int,
    completionPercent: Int,
    onNavigate: (AppModule) -> Unit,
    modifier: Modifier = Modifier
) {
    GlassmorphicCard(
        onClick = { onNavigate(AppModule.HABITS) },
        modifier = modifier
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.LocalFireDepartment,
                contentDescription = "Habit Streak",
                tint = Color(0xFFFF9800)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Habits", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text("$streakDays Days", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text("Streak", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            CircularProgressGauge(
                progressPercent = completionPercent,
                modifier = Modifier.size(40.dp),
                strokeWidth = 4.dp
            )
        }
    }
}

@Composable
private fun ExpensesSummaryCard(
    expensesTotalToday: Double,
    onNavigate: (AppModule) -> Unit,
    modifier: Modifier = Modifier
) {
    GlassmorphicCard(
        onClick = { onNavigate(AppModule.EXPENSES) },
        modifier = modifier
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.AttachMoney,
                contentDescription = "Expenses",
                tint = Color(0xFF4CAF50)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Expenses", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = String.format(Locale.getDefault(), "$%.2f", expensesTotalToday),
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text("Today's Spend", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun HealthSummaryCard(
    waterIntakeMl: Int,
    sleepHours: Float,
    onAddWater: (Int) -> Unit,
    onNavigate: (AppModule) -> Unit,
    modifier: Modifier = Modifier
) {
    GlassmorphicCard(
        onClick = { onNavigate(AppModule.HEALTH) },
        modifier = modifier
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.WaterDrop,
                contentDescription = "Water",
                tint = Color(0xFF2196F3)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Health", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text("${waterIntakeMl}ml Water", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(6.dp))

        FilledTonalButton(
            onClick = { onAddWater(250) },
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .testTag("add_water_btn")
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("+250ml", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun ShortcutChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.defaultMinSize(minHeight = 64.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth()
        ) {
            Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

