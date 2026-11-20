package com.example.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DailyUsageGoalStatus(
    val allLimitsMet: Boolean,
    val exceededApps: List<String>,
    val trackedLimitAppsCount: Int
)

data class StreakRewardOutcome(
    val newStreak: Long,
    val dailyKeysAwarded: Long,
    val bonusKeysAwarded: Long,
    val plapsAwarded: Long,
    val isWeeklyMilestone: Boolean,
    val alreadyClaimedToday: Boolean
) {
    val totalKeysAwarded: Long get() = dailyKeysAwarded + bonusKeysAwarded
}

object StreakRewardCalculator {

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun formatDateKey(millis: Long = System.currentTimeMillis()): String =
        dateFormatter.format(Date(millis))

    fun yesterdayDateKey(nowMillis: Long = System.currentTimeMillis()): String {
        val cal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            add(Calendar.DAY_OF_MONTH, -1)
        }
        return dateFormatter.format(cal.time)
    }

    fun evaluateDailyGoals(rules: List<AppRule>): DailyUsageGoalStatus {
        val limitRules = rules.filter { it.dailyLimitEnabled }
        val exceeded = limitRules
            .filter { it.usedTodayMinutes > it.dailyLimitMinutes }
            .map { "${it.appName} (${it.usedTodayMinutes}m/${it.dailyLimitMinutes}m)" }
        return DailyUsageGoalStatus(
            allLimitsMet = exceeded.isEmpty(),
            exceededApps = exceeded,
            trackedLimitAppsCount = limitRules.size
        )
    }

    fun computeReward(
        currentStreak: Long,
        lastSessionDate: String,
        todayDateKey: String,
        yesterdayDateKey: String
    ): StreakRewardOutcome {
        if (lastSessionDate == todayDateKey) {
            return StreakRewardOutcome(
                newStreak = currentStreak,
                dailyKeysAwarded = 0L,
                bonusKeysAwarded = 0L,
                plapsAwarded = 0L,
                isWeeklyMilestone = false,
                alreadyClaimedToday = true
            )
        }

        val nextStreak = when {
            lastSessionDate == yesterdayDateKey -> currentStreak.coerceAtLeast(0L) + 1L
            lastSessionDate.isBlank() -> currentStreak.coerceAtLeast(0L) + 1L
            else -> 1L
        }

        val isWeeklyMilestone = nextStreak > 0L && nextStreak % 7L == 0L
        val dailyKeys = 1L
        val bonusKeys = if (isWeeklyMilestone) 10L else 0L
        val plaps = 5L + if (isWeeklyMilestone) 50L else 0L

        return StreakRewardOutcome(
            newStreak = nextStreak,
            dailyKeysAwarded = dailyKeys,
            bonusKeysAwarded = bonusKeys,
            plapsAwarded = plaps,
            isWeeklyMilestone = isWeeklyMilestone,
            alreadyClaimedToday = false
        )
    }

    /**
     * Returns 0..7 filled checkpoints in the current 7-day weekly cycle.
     * When dailyStreak is a non-zero multiple of 7 (7, 14, 21...), returns 7 so Day 7 shines!
     */
    fun currentCycleFilledDays(dailyStreak: Long): Int {
        if (dailyStreak <= 0L) return 0
        val mod = (dailyStreak % 7L).toInt()
        return if (mod == 0) 7 else mod
    }
}
