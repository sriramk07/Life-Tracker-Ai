package com.example.ui.screens.guilds

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.GuildProgressEntity
import com.example.data.local.entities.UserStatsEntity
import com.example.domain.model.GamificationProfile
import com.example.domain.model.SocialGuildEngine
import com.example.ui.components.GlassmorphicCard
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentPurple
import com.example.ui.theme.ElectricBlue

/**
 * ============================================================================
 * GUILD SCREEN — COLLABORATIVE RAIDS & ACCOUNTABILITY LEADERBOARD
 * ============================================================================
 *
 * Implements multiplayer productivity mechanics where completing quests/habits
 * deals damage to a shared weekly Boss Raid and advances Guild Rank Tiers.
 */
@Composable
fun GuildScreen(
    guildProgress: GuildProgressEntity?,
    userName: String,
    userStats: UserStatsEntity?,
    onDealRaidStrike: (Int) -> Unit,
    onSwitchGuild: (String, String) -> Unit
) {
    val progress = guildProgress ?: GuildProgressEntity()
    val stats = userStats ?: UserStatsEntity()
    val level = GamificationProfile.calculateLevel(stats.totalXp)
    val rankTitle = GamificationProfile.calculateRank(level).displayName

    val leaderboard = remember(userName, level, rankTitle, stats.streakDays, progress.personalContributionXp) {
        SocialGuildEngine.buildGuildLeaderboard(
            userName = userName,
            userLevel = level,
            userRankTitle = rankTitle,
            userStreakDays = stats.streakDays,
            personalContributionXp = progress.personalContributionXp
        )
    }

    val totalGuildXp = leaderboard.sumOf { it.weeklyContributionXp }
    val guildTier = SocialGuildEngine.calculateGuildRankTier(totalGuildXp)
    val raidPercent = SocialGuildEngine.calculateRaidProgressPercent(
        progress.raidDamageDealt,
        progress.raidBossHp
    )

    var showEditGuildDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("guild_screen"),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "HUNTER GUILD & RAIDS",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = ElectricBlue,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Co-op accountability challenges & squad XP leaderboards",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = { showEditGuildDialog = true }) {
                    Icon(Icons.Default.Edit, contentDescription = "Switch Guild", tint = AccentCyan)
                }
            }
        }

        // Active Guild Identity Banner
        item {
            GlassmorphicCard(
                backgroundColor = Color(0xFF0A1025).copy(alpha = 0.95f),
                borderColor = ElectricBlue.copy(alpha = 0.45f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(AccentPurple.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = AccentPurple)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(progress.guildName, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                            Text(progress.guildMotto, fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                        }
                    }

                    Surface(
                        color = AccentCyan.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, AccentCyan.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = guildTier.displayName,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentCyan,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Weekly Guild Total: $totalGuildXp XP • ${leaderboard.size} Active Hunters",
                    fontSize = 12.sp,
                    color = AccentEmerald,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Weekly Co-op Boss Raid Card
        item {
            GlassmorphicCard(
                modifier = Modifier.testTag("guild_raid_card"),
                borderColor = Color(0xFFEF4444).copy(alpha = 0.4f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("ACTIVE WEEKLY DUNGEON RAID", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFFEF4444), letterSpacing = 1.sp)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(progress.raidBossName, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    }
                    Text("$raidPercent%", fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color(0xFFEF4444))
                }

                Spacer(modifier = Modifier.height(10.dp))

                LinearProgressIndicator(
                    progress = { raidPercent / 100f },
                    color = Color(0xFFEF4444),
                    trackColor = Color(0xFFEF4444).copy(alpha = 0.15f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(CircleShape)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Damage Dealt: ${progress.raidDamageDealt} / ${progress.raidBossHp} HP",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (progress.raidDamageDealt >= progress.raidBossHp) "BOSS CLEARED!" else "Reward: +500 XP",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentEmerald
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = { onDealRaidStrike(50) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("guild_raid_strike_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Commit 25m Deep Focus (+50 Raid Damage)", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        // Guild Leaderboard
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Groups, contentDescription = null, tint = AccentCyan)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Weekly Guild Contribution Leaderboard", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
        }

        itemsIndexed(leaderboard, key = { _, m -> m.id }) { index, member ->
            GlassmorphicCard(
                borderColor = if (member.isCurrentUser) AccentCyan.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.08f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "#${index + 1}",
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            color = if (index == 0) Color(0xFFFFD700) else AccentCyan,
                            modifier = Modifier.width(34.dp)
                        )
                        Column {
                            Text(
                                text = if (member.isCurrentUser) "${member.name} (You)" else member.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Lv ${member.level} ${member.rankTitle} • ${member.activeStreakDays}d streak",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = "${member.weeklyContributionXp} XP",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = AccentEmerald
                    )
                }
            }
        }
    }

    if (showEditGuildDialog) {
        var gName by remember { mutableStateOf(progress.guildName) }
        var gMotto by remember { mutableStateOf(progress.guildMotto) }

        AlertDialog(
            onDismissRequest = { showEditGuildDialog = false },
            title = { Text("Configure Hunter Guild") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = gName,
                        onValueChange = { gName = it },
                        label = { Text("Guild Name") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = gMotto,
                        onValueChange = { gMotto = it },
                        label = { Text("Guild Motto") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    onSwitchGuild(gName, gMotto)
                    showEditGuildDialog = false
                }) {
                    Text("Save Guild")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditGuildDialog = false }) { Text("Cancel") }
            }
        )
    }
}
