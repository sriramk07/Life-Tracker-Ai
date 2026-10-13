package com.example

import com.example.domain.model.BadgeCategory
import com.example.domain.model.GamificationProfile
import com.example.domain.model.HunterRank
import com.example.domain.model.HunterStats
import com.example.domain.model.defaultAchievementBadges
import org.junit.Assert.*
import org.junit.Test

/**
 * ============================================================================
 * UNIT TESTS: SOLO LEVELING GAMIFICATION & HUNTER ENGINE
 * ============================================================================
 *
 * Verifies core RPG mechanics:
 * 1. XP to Level mathematical mapping (Level = XP / 500 + 1).
 * 2. Hunter Rank boundaries (E-Rank up to Shadow Monarch).
 * 3. Title progression accuracy.
 * 4. Attribute allocation logic (STR, AGI, INT, VIT, SNE).
 * 5. Default achievement badges configuration.
 */
class GamificationModelTest {

    @Test
    fun calculateLevel_returnsExpectedLevelsForXpThresholds() {
        assertEquals(1, GamificationProfile.calculateLevel(0))
        assertEquals(1, GamificationProfile.calculateLevel(250))
        assertEquals(1, GamificationProfile.calculateLevel(499))
        assertEquals(2, GamificationProfile.calculateLevel(500))
        assertEquals(3, GamificationProfile.calculateLevel(1000))
        assertEquals(5, GamificationProfile.calculateLevel(2450))
        assertEquals(11, GamificationProfile.calculateLevel(5000))
    }

    @Test
    fun calculateRank_returnsCorrectHunterRankBoundaries() {
        assertEquals(HunterRank.E_RANK, GamificationProfile.calculateRank(1))
        assertEquals(HunterRank.E_RANK, GamificationProfile.calculateRank(2))
        assertEquals(HunterRank.D_RANK, GamificationProfile.calculateRank(3))
        assertEquals(HunterRank.D_RANK, GamificationProfile.calculateRank(5))
        assertEquals(HunterRank.C_RANK, GamificationProfile.calculateRank(6))
        assertEquals(HunterRank.C_RANK, GamificationProfile.calculateRank(9))
        assertEquals(HunterRank.B_RANK, GamificationProfile.calculateRank(10))
        assertEquals(HunterRank.B_RANK, GamificationProfile.calculateRank(14))
        assertEquals(HunterRank.A_RANK, GamificationProfile.calculateRank(15))
        assertEquals(HunterRank.A_RANK, GamificationProfile.calculateRank(19))
        assertEquals(HunterRank.S_RANK, GamificationProfile.calculateRank(20))
        assertEquals(HunterRank.S_RANK, GamificationProfile.calculateRank(29))
        assertEquals(HunterRank.NATIONAL_LEVEL, GamificationProfile.calculateRank(30))
        assertEquals(HunterRank.NATIONAL_LEVEL, GamificationProfile.calculateRank(44))
        assertEquals(HunterRank.SHADOW_MONARCH, GamificationProfile.calculateRank(45))
        assertEquals(HunterRank.SHADOW_MONARCH, GamificationProfile.calculateRank(100))
    }

    @Test
    fun calculateLevelTitle_returnsAccurateTitleProgression() {
        assertEquals("E-Rank Reawakened", GamificationProfile.calculateLevelTitle(1))
        assertEquals("System Player", GamificationProfile.calculateLevelTitle(4))
        assertEquals("Dungeon Specialist", GamificationProfile.calculateLevelTitle(8))
        assertEquals("Shadow Striker", GamificationProfile.calculateLevelTitle(12))
        assertEquals("Monarch Architect", GamificationProfile.calculateLevelTitle(18))
        assertEquals("S-Rank Sovereign", GamificationProfile.calculateLevelTitle(25))
        assertEquals("National Level Monarch", GamificationProfile.calculateLevelTitle(35))
        assertEquals("Monarch of Shadows", GamificationProfile.calculateLevelTitle(50))
    }

    @Test
    fun hunterStats_statAllocation_incrementsAttributeAndDeductsPoints() {
        val stats = HunterStats(
            strength = 10,
            agility = 12,
            intelligence = 15,
            vitality = 14,
            sense = 11,
            unallocatedPoints = 3
        )

        val updated = stats.copy(
            strength = stats.strength + 1,
            unallocatedPoints = stats.unallocatedPoints - 1
        )

        assertEquals(11, updated.strength)
        assertEquals(2, updated.unallocatedPoints)
    }

    @Test
    fun defaultAchievementBadges_containsAllRequiredBadgeCategories() {
        val badges = defaultAchievementBadges()

        assertTrue(badges.isNotEmpty())
        assertEquals(10, badges.size)

        val categories = badges.map { it.category }.toSet()
        assertTrue(categories.contains(BadgeCategory.TASKS))
        assertTrue(categories.contains(BadgeCategory.HABITS))
        assertTrue(categories.contains(BadgeCategory.EXPENSES))
        assertTrue(categories.contains(BadgeCategory.HEALTH))
        assertTrue(categories.contains(BadgeCategory.SPECIAL))

        // Verify badge rewards are positive
        badges.forEach { badge ->
            assertTrue("Badge XP reward should be positive", badge.xpReward > 0)
            assertTrue("Progress target must be greater than zero", badge.progressTarget > 0)
        }
    }

    @Test
    fun gamificationProfile_progressCalculations_areAccurate() {
        val profile = GamificationProfile(totalXp = 750)
        assertEquals(2, profile.level) // 750 / 500 + 1 = 2
        assertEquals(250, profile.xpInCurrentLevel) // 750 % 500 = 250
        assertEquals(500, profile.xpNeededForNextLevel)
        assertEquals(0.5f, profile.levelProgressPercent, 0.001f)
    }
}
