package com.example

import com.example.domain.model.GuildMember
import com.example.domain.model.GuildRankTier
import com.example.domain.model.SocialGuildEngine
import org.junit.Assert.*
import org.junit.Test

/**
 * ============================================================================
 * UNIT TESTS: SOCIAL HUNTER GUILDS & COLLABORATIVE RAID MECHANICS
 * ============================================================================
 */
class SocialGuildTest {

    @Test
    fun calculateGuildRankTier_mapsXpThresholdsCorrectly() {
        assertEquals(GuildRankTier.BRONZE_VANGUARD, SocialGuildEngine.calculateGuildRankTier(1200))
        assertEquals(GuildRankTier.SILVER_CITATION, SocialGuildEngine.calculateGuildRankTier(3000))
        assertEquals(GuildRankTier.GOLD_CITADEL, SocialGuildEngine.calculateGuildRankTier(7500))
        assertEquals(GuildRankTier.PLATINUM_SOVEREIGN, SocialGuildEngine.calculateGuildRankTier(14000))
        assertEquals(GuildRankTier.MONARCH_LEGION, SocialGuildEngine.calculateGuildRankTier(26000))
    }

    @Test
    fun calculateRaidProgressPercent_clampsBoundsProperly() {
        assertEquals(0, SocialGuildEngine.calculateRaidProgressPercent(0, 5000))
        assertEquals(50, SocialGuildEngine.calculateRaidProgressPercent(2500, 5000))
        assertEquals(100, SocialGuildEngine.calculateRaidProgressPercent(7000, 5000))
        assertEquals(100, SocialGuildEngine.calculateRaidProgressPercent(100, 0))
    }

    @Test
    fun sortLeaderboard_sortsByXpThenBreaksTiesByStreak() {
        val members = listOf(
            GuildMember("a", "Alice", 5, "C-Rank", weeklyContributionXp = 1000, activeStreakDays = 5),
            GuildMember("b", "Bob", 6, "C-Rank", weeklyContributionXp = 1000, activeStreakDays = 14),
            GuildMember("c", "Cara", 8, "B-Rank", weeklyContributionXp = 2200, activeStreakDays = 10)
        )

        val sorted = SocialGuildEngine.sortLeaderboard(members)
        assertEquals("Cara", sorted[0].name)
        assertEquals("Bob", sorted[1].name)
        assertEquals("Alice", sorted[2].name)
    }
}
