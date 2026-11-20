package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.AppRule
import com.example.model.StreakRewardCalculator
import com.example.model.UserProfile
import com.example.ui.theme.BerryInk
import com.example.ui.theme.BlushElevated
import com.example.ui.theme.BlushSurface
import com.example.ui.theme.PinkAccent
import com.example.ui.theme.PinkContainer
import com.example.ui.theme.PinkPrimary
import com.example.ui.theme.RoseGoldBorder

@Composable
fun WeeklyStreakProfileCard(
    profile: UserProfile,
    appRules: List<AppRule>,
    onClaimDailyGoal: () -> Unit,
    modifier: Modifier = Modifier
) {
    val todayKey = remember { StreakRewardCalculator.formatDateKey() }
    val alreadyClaimedToday = profile.lastSessionDate == todayKey
    val goalStatus = remember(appRules) {
        StreakRewardCalculator.evaluateDailyGoals(appRules)
    }
    val filledCycleDays = remember(profile.dailyStreak) {
        StreakRewardCalculator.currentCycleFilledDays(profile.dailyStreak)
    }
    val completedWeeks = (profile.dailyStreak / 7L).coerceAtLeast(0L)
    val bonusKeysEarnedTotal = completedWeeks * 10L
    val daysToNextBonus = if (filledCycleDays == 7) 0 else (7 - filledCycleDays)

    CoquetteCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("weekly_streak_profile_card"),
        containerColor = PinkContainer.copy(alpha = 0.82f),
        borderColor = PinkPrimary
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Profile Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_mascot_plush),
                    contentDescription = "Profile mascot avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .border(2.dp, PinkPrimary, CircleShape)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = profile.displayName.ifBlank { "Princess" },
                        style = MaterialTheme.typography.titleLarge,
                        color = BerryInk,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Weekly Streak & Goal Rewards",
                        style = MaterialTheme.typography.bodySmall,
                        color = PinkPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Wallet summary badges
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(BlushSurface)
                            .border(1.dp, RoseGoldBorder, RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_heart_key),
                            contentDescription = "Keys",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "${profile.keysBalance} Keys",
                            style = MaterialTheme.typography.labelMedium,
                            color = BerryInk,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(BlushSurface)
                            .border(1.dp, RoseGoldBorder, RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_glitter_bow),
                            contentDescription = "Plaps",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "${profile.plapsBalance} Plaps",
                            style = MaterialTheme.typography.labelMedium,
                            color = BerryInk,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Streak Highlight Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(BlushSurface)
                    .border(1.dp, RoseGoldBorder, RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = "Active streak",
                        tint = PinkPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "${profile.dailyStreak}-Day Consecutive Streak",
                            style = MaterialTheme.typography.titleMedium,
                            color = BerryInk,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("weekly_streak_count_text")
                        )
                        Text(
                            text = if (daysToNextBonus == 0 && profile.dailyStreak > 0L) {
                                "Weekly milestone unlocked! +10 Bonus Keys earned ♡"
                            } else {
                                "$daysToNextBonus more day(s) for +10 Bonus Keys!"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = PinkPrimary
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = "Bonus Keys earned",
                        tint = PinkPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "+$bonusKeysEarnedTotal Bonus",
                        style = MaterialTheme.typography.labelSmall,
                        color = BerryInk,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // 7-Day Visual Checkpoint Stepper
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                (1..7).forEach { dayIndex ->
                    val completed = dayIndex <= filledCycleDays
                    val isBonusDay = dayIndex == 7
                    val isNextTarget = dayIndex == (filledCycleDays + 1).coerceAtMost(7) && !alreadyClaimedToday

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (isBonusDay) 36.dp else 32.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        completed -> PinkPrimary
                                        isNextTarget -> PinkAccent.copy(alpha = 0.55f)
                                        else -> BlushElevated
                                    }
                                )
                                .border(
                                    width = if (isBonusDay || isNextTarget) 2.dp else 1.dp,
                                    color = if (completed || isNextTarget) PinkPrimary else RoseGoldBorder,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                completed -> Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Day $dayIndex complete",
                                    tint = BlushSurface,
                                    modifier = Modifier.size(18.dp)
                                )
                                isBonusDay -> Icon(
                                    imageVector = Icons.Default.WorkspacePremium,
                                    contentDescription = "Day 7 Bonus Keys",
                                    tint = PinkPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                else -> Text(
                                    text = "$dayIndex",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = BerryInk,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (isBonusDay) "+10🔑" else "D$dayIndex",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (completed || isBonusDay) PinkPrimary else BerryInk,
                            fontWeight = if (isBonusDay) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (filledCycleDays / 7f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = PinkPrimary,
                trackColor = BlushSurface
            )

            Spacer(Modifier.height(10.dp))

            // Daily App Usage Goal Status & Claim CTA
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(BlushElevated)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (goalStatus.allLimitsMet) Icons.Default.Check else Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = if (goalStatus.allLimitsMet) PinkPrimary else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (goalStatus.allLimitsMet) {
                        if (goalStatus.trackedLimitAppsCount > 0) {
                            "All ${goalStatus.trackedLimitAppsCount} app usage goals met today!"
                        } else {
                            "On track today! Complete daily check-in to build your 7-day key streak."
                        }
                    } else {
                        "Usage goal exceeded: ${goalStatus.exceededApps.joinToString(", ")}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = BerryInk
                )
            }

            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onClaimDailyGoal,
                enabled = goalStatus.allLimitsMet && !alreadyClaimedToday,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("claim_weekly_streak_btn"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PinkPrimary,
                    contentColor = BlushSurface
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text(
                    text = when {
                        alreadyClaimedToday -> "Today's Goal Claimed ✓ (See you tomorrow!)"
                        !goalStatus.allLimitsMet -> "Stay Within Usage Goals to Claim"
                        filledCycleDays == 6 -> "Claim Day 7 Milestone (+11 Keys & +55 Plaps!)"
                        else -> "Claim Daily Goal (+1 Key & +5 Plaps)"
                    },
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
