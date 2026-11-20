package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.util.Calendar

data class RecordedVoiceLine(
    val id: String,
    val label: String,
    val filePath: String,
    val durationSeconds: Int,
    val createdAtMillis: Long
)

data class AdhdDailyInsight(
    val dateKey: String,
    val dailyAffirmation: String,
    val adhdFocusTip: String,
    val scheduleOptimizationTip: String,
    val recommendedMicroSprintMinutes: Int = 15,
    val peakDistractionWindow: String = "Evening"
)

data class WeeklyChallengeTemplate(
    val id: String,
    val title: String,
    val themeSubtitle: String,
    val targetRestrictedMinutes: Long,
    val targetTasksCount: Long,
    val bonusKeys: Long,
    val bonusPlaps: Long,
    val adhdMicroTip: String
)

@Keep
@IgnoreExtraProperties
data class ChallengeLeaderboardEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val weekKey: String = "",
    val challengeId: String = "pink_poise_sprint",
    val challengeTitle: String = "Silk & Serenity Sprint",
    val displayName: String = "Princess",
    val tasksCompleted: Long = 0L,
    val restrictedMinutes: Long = 0L,
    val dailyStreak: Long = 0L,
    val totalScore: Long = 0L,
    val badgeTitle: String = "Silk Novice",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

object WeeklyChallengeCatalog {

    val weeklyChallenges: List<WeeklyChallengeTemplate> = listOf(
        WeeklyChallengeTemplate(
            id = "pink_poise_sprint",
            title = "Silk & Serenity Sprint",
            themeSubtitle = "Lock distractions & glide through calm pink focus blocks",
            targetRestrictedMinutes = 300L,
            targetTasksCount = 3L,
            bonusKeys = 5L,
            bonusPlaps = 25L,
            adhdMicroTip = "Start with one 10-minute timer—momentum beats perfection!"
        ),
        WeeklyChallengeTemplate(
            id = "heels_and_voice_queen",
            title = "Poised Heels & Voice Queen",
            themeSubtitle = "Complete End Session physical & voice poise tasks",
            targetRestrictedMinutes = 180L,
            targetTasksCount = 5L,
            bonusKeys = 8L,
            bonusPlaps = 40L,
            adhdMicroTip = "Pair movement tasks with upbeat music to wake up ADHD dopamine!"
        ),
        WeeklyChallengeTemplate(
            id = "midnight_detox_princess",
            title = "Midnight Detox Princess",
            themeSubtitle = "Protect sleep hours & build ultra-clean weekly focus time",
            targetRestrictedMinutes = 480L,
            targetTasksCount = 4L,
            bonusKeys = 10L,
            bonusPlaps = 50L,
            adhdMicroTip = "Enable a 22:00 auto-schedule so bedtime transitions happen automatically."
        )
    )

    fun findById(challengeId: String): WeeklyChallengeTemplate =
        weeklyChallenges.firstOrNull { it.id == challengeId } ?: weeklyChallenges.first()

    fun currentWeekKey(nowMillis: Long = System.currentTimeMillis()): String {
        val cal = Calendar.getInstance().apply {
            minimalDaysInFirstWeek = 4
            firstDayOfWeek = Calendar.MONDAY
            timeInMillis = nowMillis
        }
        val year = cal.get(Calendar.YEAR)
        val week = cal.get(Calendar.WEEK_OF_YEAR)
        return "%04d-W%02d".format(year, week)
    }

    fun computeScore(
        restrictedMinutes: Long,
        tasksCompleted: Long,
        dailyStreak: Long
    ): Long {
        val safeMinutes = restrictedMinutes.coerceAtLeast(0L)
        val safeTasks = tasksCompleted.coerceAtLeast(0L)
        val safeStreak = dailyStreak.coerceAtLeast(0L)
        return (safeMinutes + safeTasks * 50L + safeStreak * 25L).coerceIn(0L, 1_000_000L)
    }

    fun badgeForScore(totalScore: Long): String = when {
        totalScore >= 1200L -> "Crown Empress"
        totalScore >= 600L -> "Royal Princess"
        totalScore >= 250L -> "Glitter Star"
        else -> "Silk Novice"
    }
}
