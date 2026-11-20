package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.ChallengeLeaderboardEntry
import com.example.model.UiState
import com.example.model.WeeklyChallengeCatalog
import com.example.ui.components.CoquetteCard
import com.example.ui.theme.BerryInk
import com.example.ui.theme.BlushElevated
import com.example.ui.theme.BlushSurface
import com.example.ui.theme.PinkAccent
import com.example.ui.theme.PinkContainer
import com.example.ui.theme.PinkPrimary
import com.example.ui.theme.RoseGoldBorder
import com.example.viewmodel.MainDashboardSnapshot

@Composable
fun ChallengesScreen(
    snapshot: MainDashboardSnapshot,
    weekKey: String,
    leaderboardState: UiState<List<ChallengeLeaderboardEntry>>,
    isSyncing: Boolean,
    onSelectChallenge: (String) -> Unit,
    onSyncScore: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val cutoff = remember { System.currentTimeMillis() - 7L * 86_400_000L }
    val weeklyRestrictedMins = remember(snapshot.historicalUsage, snapshot.sessionHistoryLogs) {
        snapshot.historicalUsage
            .filter { it.timestampMillis >= cutoff }
            .sumOf { it.restrictedMinutes }
            .coerceAtLeast(
                snapshot.sessionHistoryLogs
                    .filter { it.startedAtMillis >= cutoff }
                    .sumOf { it.durationMinutes }
            )
    }
    val weeklyTasksCompleted = remember(snapshot.completedExitTasksCount, snapshot.sessionHistoryLogs) {
        val sessCount = snapshot.sessionHistoryLogs
            .count { it.startedAtMillis >= cutoff && (it.outcome == "COMPLETED" || it.outcome == "ENDED_EARLY") }
            .toLong()
        (snapshot.completedExitTasksCount + sessCount).coerceAtLeast(0L)
    }
    val myProjectedScore = WeeklyChallengeCatalog.computeScore(
        restrictedMinutes = weeklyRestrictedMins,
        tasksCompleted = weeklyTasksCompleted,
        dailyStreak = snapshot.profile.dailyStreak
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "challenges_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("challenges_back_btn")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = BerryInk)
                }
                Spacer(Modifier.width(4.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Sissy Weekly Challenges",
                        style = MaterialTheme.typography.headlineSmall,
                        color = BerryInk,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Week $weekKey • Compete globally on tasks & restricted time",
                        style = MaterialTheme.typography.labelMedium,
                        color = PinkPrimary
                    )
                }
            }
        }

        // My Weekly Score & Sync Action
        item(key = "my_score_card") {
            CoquetteCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = PinkContainer.copy(alpha = 0.9f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Your Weekly Score: $myProjectedScore pts",
                                style = MaterialTheme.typography.titleMedium,
                                color = BerryInk,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "$weeklyRestrictedMins restricted mins • $weeklyTasksCompleted tasks • ${snapshot.profile.dailyStreak}d streak",
                                style = MaterialTheme.typography.bodySmall,
                                color = BerryInk
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(PinkPrimary)
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = WeeklyChallengeCatalog.badgeForScore(myProjectedScore),
                                style = MaterialTheme.typography.labelSmall,
                                color = BlushSurface,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    Button(
                        onClick = onSyncScore,
                        enabled = !isSyncing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("sync_challenge_score_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PinkPrimary,
                            contentColor = BlushSurface
                        )
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = BlushSurface, strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Syncing Global Rank...")
                        } else {
                            Icon(Icons.Default.EmojiEvents, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Sync Score to Global Leaderboard ♡", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Weekly Themed Challenges Selector
        item(key = "challenge_section_title") {
            Text(
                text = "Pick Your Weekly Sissy Quest",
                style = MaterialTheme.typography.titleMedium,
                color = BerryInk,
                fontWeight = FontWeight.Bold
            )
        }

        itemsIndexed(
            items = WeeklyChallengeCatalog.weeklyChallenges,
            key = { _, item -> "quest_${item.id}" }
        ) { _, challenge ->
            val isSelected = snapshot.selectedChallengeId == challenge.id
            val minsProgress = (weeklyRestrictedMins.toFloat() / challenge.targetRestrictedMinutes.toFloat()).coerceIn(0f, 1f)
            val tasksProgress = (weeklyTasksCompleted.toFloat() / challenge.targetTasksCount.toFloat()).coerceIn(0f, 1f)

            CoquetteCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectChallenge(challenge.id) },
                containerColor = if (isSelected) PinkContainer else BlushElevated
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = challenge.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = BerryInk,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) PinkPrimary else PinkAccent.copy(alpha = 0.45f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isSelected) "Active Quest ♡" else "+${challenge.bonusKeys}🔑 +${challenge.bonusPlaps}🎀",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) BlushSurface else BerryInk,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(challenge.themeSubtitle, style = MaterialTheme.typography.bodySmall, color = BerryInk)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "ADHD Boost: ${challenge.adhdMicroTip}",
                        style = MaterialTheme.typography.labelSmall,
                        color = PinkPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Focus Time: $weeklyRestrictedMins / ${challenge.targetRestrictedMinutes}m",
                        style = MaterialTheme.typography.labelSmall,
                        color = BerryInk
                    )
                    LinearProgressIndicator(
                        progress = { minsProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = PinkPrimary,
                        trackColor = BlushSurface
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Tasks Completed: $weeklyTasksCompleted / ${challenge.targetTasksCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = BerryInk
                    )
                    LinearProgressIndicator(
                        progress = { tasksProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = PinkAccent,
                        trackColor = BlushSurface
                    )
                }
            }
        }

        // Global Leaderboard Header
        item(key = "leaderboard_title") {
            Text(
                text = "Global Weekly Leaderboard",
                style = MaterialTheme.typography.titleMedium,
                color = BerryInk,
                fontWeight = FontWeight.Bold
            )
        }

        when (leaderboardState) {
            is UiState.Loading -> {
                item(key = "lb_loading") {
                    CoquetteCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(color = PinkPrimary, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(12.dp))
                            Text("Loading global Sissy rankings...", color = BerryInk)
                        }
                    }
                }
            }

            is UiState.Error -> {
                item(key = "lb_error") {
                    CoquetteCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(leaderboardState.message, color = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = onSyncScore,
                                colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary, contentColor = BlushSurface)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Retry")
                            }
                        }
                    }
                }
            }

            is UiState.Empty -> {
                item(key = "lb_empty") {
                    CoquetteCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Be the first queen on this week's leaderboard! Tap Sync Score above to claim #1 ♡",
                                color = BerryInk
                            )
                        }
                    }
                }
            }

            is UiState.Success -> {
                itemsIndexed(
                    items = leaderboardState.data,
                    key = { _, item -> "lb_${item.id}" }
                ) { idx, entry ->
                    val isMe = entry.userId == snapshot.profile.userId
                    val rank = idx + 1
                    CoquetteCard(
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = if (isMe) PinkContainer else BlushElevated
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (rank <= 3) PinkPrimary else PinkAccent.copy(alpha = 0.5f))
                                    .border(1.dp, RoseGoldBorder, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "#$rank",
                                    color = if (rank <= 3) BlushSurface else BerryInk,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (isMe) "${entry.displayName} (You)" else entry.displayName,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = BerryInk,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (rank == 1) {
                                        Spacer(Modifier.width(6.dp))
                                        Icon(
                                            Icons.Default.WorkspacePremium,
                                            contentDescription = "Rank 1 Crown",
                                            tint = PinkPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "${entry.challengeTitle} • ${entry.restrictedMinutes}m • ${entry.tasksCompleted} tasks",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PinkPrimary
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${entry.totalScore} pts",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = BerryInk,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = entry.badgeTitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PinkPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
