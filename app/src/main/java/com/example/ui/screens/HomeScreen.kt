package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.DefaultAdhdContent
import com.example.model.RestrictionSession
import com.example.model.SchedulePlan
import com.example.model.SmallWinTask
import com.example.model.StreakRewardCalculator
import com.example.model.UserProfile
import com.example.service.RestrictionRuntimeState
import com.example.ui.components.CoquetteCard
import com.example.ui.components.HeartDigitalClockBanner
import com.example.ui.components.SissyPomodoroCard
import com.example.ui.theme.BerryInk
import com.example.ui.theme.BlushElevated
import com.example.ui.theme.BlushSurface
import com.example.ui.theme.PinkContainer
import com.example.ui.theme.PinkPrimary
import com.example.ui.theme.RoseGoldBorder
import com.example.ui.theme.parseHexColorSafe
import com.example.ui.theme.resolveFontFamily
import com.example.viewmodel.MainDashboardSnapshot
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    snapshot: MainDashboardSnapshot,
    nowMillis: Long,
    onToggleScheduleEnabled: (SchedulePlan) -> Unit,
    onToggleAdhdCalmMode: (Boolean) -> Unit = {},
    onUpdateBrainDumpNote: (String) -> Unit = {},
    onStartMicroSprint: (Long) -> Unit = {},
    onOpenHyperFocus: (Int) -> Unit = {},
    onPomodoroCompleted: (Boolean) -> Unit = {},
    onToggleSmallWin: (String) -> Unit = {},
    onSetActiveTask: (String) -> Unit = {},
    onGenerateDailyInsight: () -> Unit = {},
    onOpenChallenges: () -> Unit = {},
    onOpenScreenSaver: () -> Unit,
    onOpenEarlyExit: () -> Unit,
    onOpenScheduleManager: () -> Unit,
    onOpenDailyLimits: () -> Unit,
    onNavigateToTab: (String) -> Unit
) {
    val context = LocalContext.current
    val profile: UserProfile = snapshot.profile
    val activeSession: RestrictionSession? = snapshot.activeSession
    val customFont = resolveFontFamily(profile.selectedFontKey, profile.customFontUri, context)
    val inkColor = parseHexColorSafe(profile.selectedFontColorHex, BerryInk)

    val activeSchedules = remember(snapshot.schedules, nowMillis / 15_000L) {
        RestrictionRuntimeState.getCurrentlyActiveSchedules(nowMillis, snapshot.schedules)
    }
    val primaryActiveSchedule = activeSchedules.firstOrNull()

    if (snapshot.adhdCalmMode) {
        val currentActiveTask = remember(snapshot.smallWinsState.tasks) {
            snapshot.smallWinsState.tasks.firstOrNull { !it.completed }
        }
        AdhdFriendlyDashboard(
            activeTask = currentActiveTask,
            activeScheduleName = primaryActiveSchedule?.name,
            customFont = customFont,
            inkColor = inkColor,
            onToggleSmallWin = onToggleSmallWin,
            onSetActiveTask = onSetActiveTask,
            onOpenHyperFocus = onOpenHyperFocus,
            onPomodoroCompleted = onPomodoroCompleted,
            onOpenSettings = { onNavigateToTab("settings") }
        )
        return
    }

    val isSessionLocked = activeSession != null && activeSession.status in listOf("ACTIVE", "PAUSED")
    val isRestrictionActive = primaryActiveSchedule != null || isSessionLocked
    val isPaused = activeSession?.status == "PAUSED" && activeSession.pausedUntilMillis > nowMillis

    val endTargetMillis = when {
        primaryActiveSchedule != null -> RestrictionRuntimeState.computeScheduleEndMillis(primaryActiveSchedule, nowMillis)
        isSessionLocked && activeSession != null -> activeSession.endsAtMillis
        else -> {
            val firstEnabled = snapshot.schedules.firstOrNull { it.enabled } ?: snapshot.schedules.firstOrNull()
            if (firstEnabled != null) RestrictionRuntimeState.computeScheduleEndMillis(firstEnabled, nowMillis)
            else nowMillis + 7200_000L
        }
    }

    val remainingSeconds = if (isRestrictionActive) {
        ((endTargetMillis - nowMillis) / 1000L).coerceAtLeast(0L)
    } else {
        val previewPlan = snapshot.schedules.firstOrNull { it.enabled } ?: snapshot.schedules.firstOrNull()
        if (previewPlan != null) {
            val durMins = ((previewPlan.endMinutes - previewPlan.startMinutes + 1440L) % 1440L).let {
                if (it == 0L) 1440L else it
            }
            durMins * 60L
        } else {
            7200L
        }
    }

    val unlockTimeFormatted = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(endTargetMillis))

    val daysUntilBurn = remember(nowMillis) {
        val cal = Calendar.getInstance()
        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        (maxDay - cal.get(Calendar.DAY_OF_MONTH)).coerceAtLeast(0)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = DefaultAdhdContent.HEADER_TITLE,
                    fontFamily = customFont,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = inkColor
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = DefaultAdhdContent.SUBHEADER_LINES,
                    fontFamily = customFont,
                    style = MaterialTheme.typography.bodyMedium,
                    color = PinkPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${DefaultAdhdContent.SISSY_MOTTO} ♡",
                    fontFamily = customFont,
                    style = MaterialTheme.typography.labelLarge,
                    color = inkColor,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(8.dp))
            Image(
                painter = painterResource(id = R.drawable.img_mascot_plush),
                contentDescription = DefaultAdhdContent.SISSY_MOTTO,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .border(2.dp, PinkPrimary, CircleShape)
            )
        }

        // Sissy Visual Pomodoro Timer (Progress Bar for ADHD Time Blindness)
        SissyPomodoroCard(
            fontFamily = customFont,
            onOpenHyperFocus = onOpenHyperFocus,
            onCycleCompleted = onPomodoroCompleted
        )

        // Heart Digital Clock
        HeartDigitalClockBanner(
            remainingSeconds = remainingSeconds,
            unlockAtFormatted = unlockTimeFormatted,
            isActive = isRestrictionActive,
            isPaused = isPaused,
            fontFamily = customFont,
            textColor = inkColor
        )

        // ADHD Focus & Calm Mode Bar (1-Tap Micro-Sprints + Visual Simplifier)
        CoquetteCard(
            modifier = Modifier.fillMaxWidth(),
            containerColor = BlushElevated
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Psychology, contentDescription = null, tint = PinkPrimary)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "ADHD Friendly Mode",
                                style = MaterialTheme.typography.titleSmall,
                                color = inkColor,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Hide secondary nav & show only active task + Pomodoro",
                                style = MaterialTheme.typography.bodySmall,
                                color = PinkPrimary
                            )
                        }
                    }
                    Switch(
                        checked = snapshot.adhdCalmMode,
                        onCheckedChange = onToggleAdhdCalmMode,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = BlushSurface,
                            checkedTrackColor = PinkPrimary
                        ),
                        modifier = Modifier.testTag("adhd_calm_toggle")
                    )
                }

                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(10L to "10m Spark", 15L to "15m Glow", 25L to "25m Focus").forEach { (mins, label) ->
                        FilterChip(
                            selected = activeSession?.durationMinutes == mins && isSessionLocked,
                            onClick = { onStartMicroSprint(mins) },
                            leadingIcon = {
                                Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            label = { Text(label, maxLines = 1) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PinkPrimary,
                                selectedLabelColor = BlushSurface,
                                selectedLeadingIconColor = BlushSurface,
                                containerColor = PinkContainer,
                                labelColor = BerryInk
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("adhd_sprint_$mins")
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { onOpenHyperFocus(15) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BerryInk,
                        contentColor = BlushSurface
                    ),
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("home_hyperfocus_btn")
                ) {
                    Icon(Icons.Default.CenterFocusStrong, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Enter High-Contrast Hyper-Focus Mode", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Daily Gemini Sissy + ADHD Boost Card
        val cachedInsight = snapshot.cachedAdhdInsight
        CoquetteCard(
            modifier = Modifier.fillMaxWidth(),
            containerColor = PinkContainer.copy(alpha = 0.85f)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = PinkPrimary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Today's Sissy & ADHD Pep Talk",
                            style = MaterialTheme.typography.titleSmall,
                            color = inkColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                if (cachedInsight != null) {
                    Text(
                        text = cachedInsight.dailyAffirmation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = PinkPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Tip: ${cachedInsight.adhdFocusTip}",
                        style = MaterialTheme.typography.bodySmall,
                        color = BerryInk
                    )
                } else {
                    Text(
                        text = "Let Belle read your weekly pattern and give you a gentle dopamine-friendly plan for today.",
                        style = MaterialTheme.typography.bodySmall,
                        color = BerryInk
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onGenerateDailyInsight,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("home_daily_boost_btn")
                    ) {
                        Text("Get Today's Gemini Boost ♡", color = BerryInk, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Schedule Section (replaces Session Section)
        CoquetteCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (primaryActiveSchedule != null) {
                                "Active Schedule: ${primaryActiveSchedule.name}"
                            } else {
                                "Your Schedules"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = inkColor,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (primaryActiveSchedule != null) {
                                "${primaryActiveSchedule.restrictedPackages.size} restricted • ${primaryActiveSchedule.allowedPackages.size} allowed"
                            } else {
                                "Customize detailed plans & per-schedule app lists"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = PinkPrimary
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                if (snapshot.schedules.isEmpty()) {
                    Text(
                        text = "No schedules created yet. Build your first schedule plan with custom apps!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = BerryInk
                    )
                    Spacer(Modifier.height(10.dp))
                } else if (!snapshot.adhdCalmMode) {
                    snapshot.schedules.take(3).forEach { plan ->
                        val windowLabel = "${RestrictionRuntimeState.formatMinutes(plan.startMinutes.toInt())} - ${RestrictionRuntimeState.formatMinutes(plan.endMinutes.toInt())}"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(PinkContainer.copy(alpha = 0.55f))
                                .border(1.dp, RoseGoldBorder, RoundedCornerShape(14.dp))
                                .clickable { onOpenScheduleManager() }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(plan.name, style = MaterialTheme.typography.titleSmall, color = BerryInk)
                                Text(
                                    text = "$windowLabel • ${plan.restrictedPackages.size} blocked, ${plan.allowedPackages.size} allowed",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PinkPrimary
                                )
                            }
                            Switch(
                                checked = plan.enabled,
                                onCheckedChange = { onToggleScheduleEnabled(plan) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = BlushSurface,
                                    checkedTrackColor = PinkPrimary
                                )
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                if (isRestrictionActive) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onOpenScreenSaver,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("open_screensaver_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PinkPrimary,
                                contentColor = BlushSurface
                            ),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Icon(Icons.Default.Bedtime, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Screen Saver", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onOpenEarlyExit,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("open_early_exit_btn"),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Icon(Icons.Default.LockOpen, contentDescription = null, tint = BerryInk, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Unlock Early", color = BerryInk, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onOpenScheduleManager,
                            modifier = Modifier
                                .weight(1.3f)
                                .height(48.dp)
                                .testTag("home_manage_schedule_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PinkPrimary,
                                contentColor = BlushSurface
                            ),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Customize Schedule", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onOpenScreenSaver,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("open_screensaver_btn"),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Icon(Icons.Default.Bedtime, contentDescription = null, tint = BerryInk, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("AOD", color = BerryInk, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Gamified Weekly Challenges Entry Card
        CoquetteCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenChallenges)
                .testTag("home_challenges_card"),
            containerColor = PinkContainer
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(PinkPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = "Sissy Weekly Challenges", tint = BlushSurface)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Weekly Sissy Challenges & Leaderboard",
                        style = MaterialTheme.typography.titleSmall,
                        color = BerryInk,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Compete globally on completed tasks & focus minutes ♡",
                        style = MaterialTheme.typography.bodySmall,
                        color = PinkPrimary
                    )
                }
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Open Challenges",
                    tint = BerryInk
                )
            }
        }

        // ADHD Brain-Dump Parking Lot (keep random thoughts out of scrolling loops)
        CoquetteCard(
            modifier = Modifier.fillMaxWidth(),
            containerColor = BlushSurface
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "ADHD Brain-Dump Parking Lot",
                    style = MaterialTheme.typography.labelLarge,
                    color = BerryInk,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = snapshot.adhdBrainDumpNote,
                    onValueChange = onUpdateBrainDumpNote,
                    placeholder = { Text("Park a distracting thought here & stay on track ♡") },
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("adhd_brain_dump_input")
                )
            }
        }

        if (!snapshot.adhdCalmMode) {
            // Schedule & Daily Limits cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ModeQuickCard(
                    title = "Schedule Plans",
                    subtitle = "Create detailed plans & per-schedule apps",
                    icon = Icons.Default.CalendarMonth,
                    onClick = onOpenScheduleManager,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("home_schedule_mode_card")
                )
                ModeQuickCard(
                    title = "Daily Limits",
                    subtitle = "Cap app minutes & unlock with Plaps",
                    icon = Icons.Default.HourglassTop,
                    onClick = onOpenDailyLimits,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("home_daily_limit_card")
                )
            }

            // 4 Quick Tiles (Reports, AI, Keys, Plaps)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickSquareTile(
                    title = "Reports",
                    badge = "${snapshot.usageLogs.sumOf { it.restrictedMinutes }}m",
                    onClick = { onNavigateToTab("reports") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tile_reports")
                ) {
                    Icon(Icons.Default.Assessment, contentDescription = "Reports", tint = PinkPrimary, modifier = Modifier.size(30.dp))
                }

                QuickSquareTile(
                    title = "AI Coach",
                    badge = "Gemini",
                    onClick = { onNavigateToTab("ai") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tile_ai")
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = "AI Coach", tint = PinkPrimary, modifier = Modifier.size(30.dp))
                }

                QuickSquareTile(
                    title = "Keys",
                    badge = "${profile.keysBalance} (15m)",
                    onClick = {
                        if (isRestrictionActive) onOpenEarlyExit() else onNavigateToTab("settings")
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tile_keys")
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_heart_key),
                        contentDescription = "Keys",
                        modifier = Modifier.size(32.dp)
                    )
                }

                QuickSquareTile(
                    title = "Plaps",
                    badge = "${profile.plapsBalance} (1m)",
                    onClick = {
                        if (isRestrictionActive) onOpenEarlyExit() else onNavigateToTab("settings")
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tile_plaps")
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_glitter_bow),
                        contentDescription = "Plaps",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        // Encouragement & Streak Card
        CoquetteCard(
            modifier = Modifier.fillMaxWidth(),
            containerColor = PinkContainer.copy(alpha = 0.88f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_mascot_plush),
                    contentDescription = "Encouragement teddy",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(58.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, PinkPrimary, CircleShape)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "You're doing great, ${profile.displayName} ♡",
                        fontFamily = customFont,
                        style = MaterialTheme.typography.titleMedium,
                        color = inkColor
                    )
                    Text(
                        text = "Every completed day earns +1 Key & +5 Plaps (+10 Keys & +50 Plaps weekly!). Burns in $daysUntilBurn days.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = inkColor
                    )
                }
                Spacer(Modifier.width(8.dp))
                val cycleDays = remember(profile.dailyStreak) {
                    StreakRewardCalculator.currentCycleFilledDays(profile.dailyStreak)
                }
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(BlushSurface)
                        .border(1.dp, RoseGoldBorder, RoundedCornerShape(16.dp))
                        .clickable { onNavigateToTab("settings") }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.WorkspacePremium,
                        contentDescription = "Weekly Streak",
                        tint = PinkPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "${profile.dailyStreak} Days",
                        style = MaterialTheme.typography.labelLarge,
                        color = inkColor,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$cycleDays/7 (+10🔑)",
                        style = MaterialTheme.typography.labelSmall,
                        color = PinkPrimary
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun AdhdFriendlyDashboard(
    activeTask: SmallWinTask?,
    activeScheduleName: String?,
    customFont: FontFamily,
    inkColor: Color,
    onToggleSmallWin: (String) -> Unit,
    onSetActiveTask: (String) -> Unit,
    onOpenHyperFocus: (Int) -> Unit,
    onPomodoroCompleted: (Boolean) -> Unit,
    onOpenSettings: () -> Unit
) {
    var showTaskEditor by remember { mutableStateOf(false) }
    var draftTaskTitle by remember(activeTask?.id) { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .testTag("adhd_friendly_dashboard"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Minimal distraction-free top bar with single Settings access
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = PinkContainer,
                border = androidx.compose.foundation.BorderStroke(1.dp, RoseGoldBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = PinkPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (activeScheduleName != null) {
                            "ADHD Focus • $activeScheduleName"
                        } else {
                            "ADHD Friendly Mode"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = BerryInk,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(PinkContainer.copy(alpha = 0.85f))
                    .testTag("adhd_home_settings_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = BerryInk
                )
            }
        }

        // 1. Current Active Task Card (Single-task focus)
        CoquetteCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("adhd_current_active_task_card"),
            containerColor = BlushElevated
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CURRENT ACTIVE TASK",
                        style = MaterialTheme.typography.labelMedium,
                        color = PinkPrimary,
                        fontWeight = FontWeight.ExtraBold
                    )
                    if (activeTask != null) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = PinkContainer
                        ) {
                            Text(
                                text = "+${activeTask.plapReward} Plaps 🎀",
                                style = MaterialTheme.typography.labelSmall,
                                color = BerryInk,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Text(
                    text = activeTask?.title
                        ?: "All scheduled tasks complete! Set one next focus step below ♡",
                    fontFamily = customFont,
                    style = MaterialTheme.typography.headlineSmall,
                    color = inkColor,
                    fontWeight = FontWeight.Bold
                )

                if (activeTask != null) {
                    Button(
                        onClick = { onToggleSmallWin(activeTask.id) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PinkPrimary,
                            contentColor = BlushSurface
                        ),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("adhd_complete_active_task_btn")
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Complete Task (+${activeTask.plapReward} Plaps)", fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedButton(
                    onClick = { showTaskEditor = !showTaskEditor },
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("adhd_edit_active_task_btn")
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = BerryInk, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (showTaskEditor) "Close Task Editor" else "Set / Change Active Task",
                        color = BerryInk,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                AnimatedVisibility(visible = showTaskEditor) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = draftTaskTitle,
                            onValueChange = { draftTaskTitle = it.take(120) },
                            label = { Text("One priority task to focus on") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("adhd_new_active_task_input")
                        )
                        Button(
                            onClick = {
                                if (draftTaskTitle.isNotBlank()) {
                                    onSetActiveTask(draftTaskTitle)
                                    draftTaskTitle = ""
                                    showTaskEditor = false
                                }
                            },
                            enabled = draftTaskTitle.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BerryInk,
                                contentColor = BlushSurface
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("adhd_save_active_task_btn")
                        ) {
                            Text("Pin as Current Active Task ♡", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 2. Visual Pomodoro Timer (Progress Bar for ADHD Time Blindness)
        SissyPomodoroCard(
            fontFamily = customFont,
            onOpenHyperFocus = onOpenHyperFocus,
            onCycleCompleted = onPomodoroCompleted
        )
    }
}

@Composable
private fun ModeQuickCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    CoquetteCard(
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(PinkContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = title, tint = PinkPrimary)
                }
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(PinkPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Go",
                        tint = BlushSurface,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = BerryInk)
            Spacer(Modifier.height(4.dp))
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = BerryInk)
        }
    }
}

@Composable
private fun QuickSquareTile(
    title: String,
    badge: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconSlot: @Composable () -> Unit
) {
    CoquetteCard(
        modifier = modifier.clickable(onClick = onClick),
        containerColor = BlushElevated.copy(alpha = 0.92f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            iconSlot()
            Spacer(Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = BerryInk,
                maxLines = 1
            )
            Text(
                text = badge,
                style = MaterialTheme.typography.labelSmall,
                color = PinkPrimary,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}
