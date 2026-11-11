package com.example.domain.model

/**
 * ============================================================================
 * SOCIAL GUILD MODEL — MULTIPLAYER ACCOUNTABILITY & COLLABORATIVE RAIDS
 * ============================================================================
 *
 * Provides pure domain models and deterministic progression functions for
 * Hunter Guilds, collaborative weekly Raid Challenges, and Ranked Leaderboards.
 */

enum class GuildRankTier(val displayName: String, val minGuildXp: Int) {
    BRONZE_VANGUARD("Bronze Vanguard", 0),
    SILVER_CITATION("Silver Citation", 2500),
    GOLD_CITADEL("Gold Citadel", 6000),
    PLATINUM_SOVEREIGN("Platinum Sovereign", 12000),
    MONARCH_LEGION("Monarch Legion", 25000)
}

data class GuildMember(
    val id: String,
    val name: String,
    val level: Int,
    val rankTitle: String,
    val weeklyContributionXp: Int,
    val activeStreakDays: Int,
    val isCurrentUser: Boolean = false
)

data class GuildRaidChallenge(
    val id: String,
    val bossName: String,
    val rewardTitle: String,
    val rewardBonusXp: Int,
    val targetHp: Int,
    val damageDealt: Int
) {
    val isDefeated: Boolean get() = damageDealt >= targetHp
    val progressPercent: Int get() = SocialGuildEngine.calculateRaidProgressPercent(damageDealt, targetHp)
}

object SocialGuildEngine {

    /**
     * Resolves Guild tier based on aggregate team weekly XP.
     */
    fun calculateGuildRankTier(totalGuildXp: Int): GuildRankTier {
        return when {
            totalGuildXp >= 25000 -> GuildRankTier.MONARCH_LEGION
            totalGuildXp >= 12000 -> GuildRankTier.PLATINUM_SOVEREIGN
            totalGuildXp >= 6000 -> GuildRankTier.GOLD_CITADEL
            totalGuildXp >= 2500 -> GuildRankTier.SILVER_CITATION
            else -> GuildRankTier.BRONZE_VANGUARD
        }
    }

    /**
     * Calculates raid boss defeat progress percentage clamped cleanly between 0 and 100.
     */
    fun calculateRaidProgressPercent(damageDealt: Int, targetHp: Int): Int {
        if (targetHp <= 0) return 100
        return ((damageDealt.toLong() * 100L) / targetHp.toLong()).toInt().coerceIn(0, 100)
    }

    /**
     * Sorts Guild members descending by weekly XP contribution, breaking ties by active streak.
     */
    fun sortLeaderboard(members: List<GuildMember>): List<GuildMember> {
        return members.sortedWith(
            compareByDescending<GuildMember> { it.weeklyContributionXp }
                .thenByDescending { it.activeStreakDays }
        )
    }

    /**
     * Generates default active guild roster augmented with the current user's live XP contribution.
     */
    fun buildGuildLeaderboard(
        userName: String,
        userLevel: Int,
        userRankTitle: String,
        userStreakDays: Int,
        personalContributionXp: Int
    ): List<GuildMember> {
        val squad = listOf(
            GuildMember(
                id = "you",
                name = userName.ifBlank { "Hunter" },
                level = userLevel,
                rankTitle = userRankTitle,
                weeklyContributionXp = personalContributionXp,
                activeStreakDays = userStreakDays,
                isCurrentUser = true
            ),
            GuildMember("m_aria", "Aria Chen", 9, "C-Rank Hunter", 1640, 19),
            GuildMember("m_kai", "Kai Nakamura", 7, "C-Rank Hunter", 1390, 15),
            GuildMember("m_elena", "Elena Rostova", 6, "C-Rank Hunter", 1120, 11),
            GuildMember("m_marcus", "Marcus Thorne", 4, "D-Rank Hunter", 860, 8)
        )
        return sortLeaderboard(squad)
    }
}
