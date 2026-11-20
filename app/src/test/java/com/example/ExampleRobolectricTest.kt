package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.AppRule
import com.example.model.RestrictionSession
import com.example.model.SchedulePlan
import com.example.model.UserProfile
import com.example.service.RestrictionRuntimeState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.flow.first

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Be Sissy", appName)
    }

    @Test
    fun `restriction runtime evaluates schedule plans with per-schedule allow and restrict apps`() {
        val now = 1_700_000_000_000L
        val rules = listOf(
            AppRule(
                packageName = "com.android.dialer",
                appName = "Phone",
                necessaryApp = true
            ),
            AppRule(
                packageName = "com.android.chrome",
                appName = "Chrome Browser",
                dailyLimitEnabled = true,
                dailyLimitMinutes = 60L,
                usedTodayMinutes = 60L
            )
        )
        val schedules = listOf(
            SchedulePlan(
                id = "sched_night",
                userId = "u1",
                name = "Night Focus",
                enabled = true,
                startMinutes = 22 * 60L,
                endMinutes = 0L,
                daysOfWeek = listOf(1L, 2L, 3L, 4L, 5L, 6L, 7L),
                modeType = "BLOCK_SELECTED",
                allowedPackages = listOf("com.google.android.youtube"),
                restrictedPackages = listOf("com.mobile.legends"),
                customBlockReason = "Sleep focus active"
            ),
            SchedulePlan(
                id = "sched_study",
                userId = "u1",
                name = "Strict Study",
                enabled = true,
                startMinutes = 9 * 60L,
                endMinutes = 17 * 60L,
                daysOfWeek = listOf(1L, 2L, 3L, 4L, 5L, 6L, 7L),
                modeType = "ALLOW_ONLY_SELECTED",
                allowedPackages = listOf("com.android.chrome"),
                restrictedPackages = listOf("com.instagram.android")
            )
        )
        RestrictionRuntimeState.updateState(null, rules, UserProfile(userId = "u1"), schedules)

        // Necessary app allowed during night schedule
        assertFalse(
            RestrictionRuntimeState.evaluatePackageBlock(
                "com.android.dialer",
                nowMillis = now,
                currentMinutesOfDay = 23 * 60,
                currentDayOfWeek = 1
            ).isBlocked
        )

        // Per-schedule restricted app blocked during night schedule
        assertTrue(
            RestrictionRuntimeState.evaluatePackageBlock(
                "com.mobile.legends",
                nowMillis = now,
                currentMinutesOfDay = 23 * 60,
                currentDayOfWeek = 1
            ).isBlocked
        )

        // Per-schedule allowed app permitted during study schedule even in ALLOW_ONLY_SELECTED
        // (unless daily limit overrides after schedule passes; test non-limited app in schedule)
        assertFalse(
            RestrictionRuntimeState.evaluatePackageBlock(
                "com.google.android.youtube",
                nowMillis = now,
                currentMinutesOfDay = 23 * 60,
                currentDayOfWeek = 1
            ).isBlocked
        )

        // Unlisted social app blocked in ALLOW_ONLY_SELECTED study schedule
        assertTrue(
            RestrictionRuntimeState.evaluatePackageBlock(
                "com.instagram.android",
                nowMillis = now,
                currentMinutesOfDay = 14 * 60,
                currentDayOfWeek = 1
            ).isBlocked
        )

        // Daily limit app blocked when limit reached outside schedule
        assertTrue(
            RestrictionRuntimeState.evaluatePackageBlock(
                "com.android.chrome",
                nowMillis = now,
                currentMinutesOfDay = 20 * 60,
                currentDayOfWeek = 1
            ).isBlocked
        )
    }

    @Test
    fun `restriction runtime evaluates legacy session, schedule, and daily limit modes`() {
        val now = 1_700_000_000_000L
        val session = RestrictionSession(
            id = "s1",
            userId = "u1",
            mode = "SESSION",
            durationMinutes = 60L,
            startedAtMillis = now - 60_000L,
            endsAtMillis = now + 3_540_000L,
            status = "ACTIVE"
        )
        val rules = listOf(
            AppRule(
                packageName = "com.android.dialer",
                appName = "Phone",
                necessaryApp = true,
                restrictedInSession = false
            ),
            AppRule(
                packageName = "com.mobile.legends",
                appName = "Mobile Legends",
                necessaryApp = false,
                restrictedInSession = true,
                scheduleEnabled = true,
                scheduleStartMinutes = 22 * 60L,
                scheduleEndMinutes = 0L
            )
        )
        RestrictionRuntimeState.updateState(session, rules, UserProfile(userId = "u1"), emptyList())

        assertFalse(
            RestrictionRuntimeState.evaluatePackageBlock(
                "com.android.dialer",
                nowMillis = now,
                currentMinutesOfDay = 23 * 60,
                currentDayOfWeek = 1
            ).isBlocked
        )

        assertTrue(
            RestrictionRuntimeState.evaluatePackageBlock(
                "com.mobile.legends",
                nowMillis = now,
                currentMinutesOfDay = 14 * 60,
                currentDayOfWeek = 1
            ).isBlocked
        )
    }

    @Test
    fun `theme color palette matches required pink theme constants`() {
        assertEquals(0xFFFF5C9A.toInt(), com.example.ui.theme.PinkPrimary.toArgb())
        assertEquals(0xFFFFCCE0.toInt(), com.example.ui.theme.PinkContainer.toArgb())
        assertEquals(0xFFFF96FC.toInt(), com.example.ui.theme.PinkAccent.toArgb())
        assertEquals(0xFFB8255D.toInt(), com.example.ui.theme.BerryInk.toArgb())
        assertEquals(0xFFFFE3EE.toInt(), com.example.ui.theme.BlushSurface.toArgb())
        assertEquals(com.example.ui.theme.PinkPrimary, com.example.ui.theme.BeSissyDefaultColorScheme.primary)
        assertEquals(com.example.ui.theme.BlushSurface, com.example.ui.theme.BeSissyDefaultColorScheme.background)
        assertEquals(com.example.ui.theme.BerryInk, com.example.ui.theme.BeSissyDefaultColorScheme.onBackground)
    }

    @Test
    fun `room usage history repository stores and queries weekly and monthly usage and session logs`() {
        kotlinx.coroutines.test.runTest {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val db = androidx.room.Room.inMemoryDatabaseBuilder(
                context,
                com.example.data.local.BeSissyDatabase::class.java
            ).allowMainThreadQueries().build()
            try {
                val repo = com.example.data.UsageHistoryRepository(db.usageHistoryDao())
                val now = System.currentTimeMillis()
                val dayMs = 86_400_000L

                // Record usage 2 days ago (within 7d weekly) and 15 days ago (within 30d monthly)
                repo.recordUsageSnapshot(
                    userId = "u1",
                    packageName = "com.android.chrome",
                    appName = "Chrome Browser",
                    screenTimeMinutes = 40L,
                    restrictedMinutes = 60L,
                    timestampMillis = now - 2L * dayMs
                )
                repo.recordUsageSnapshot(
                    userId = "u1",
                    packageName = "com.mobile.legends",
                    appName = "Mobile Legends",
                    screenTimeMinutes = 25L,
                    restrictedMinutes = 90L,
                    timestampMillis = now - 15L * dayMs
                )

                // Record restriction session log
                repo.recordRestrictionSessionLog(
                    id = "log_sess_1",
                    userId = "u1",
                    sessionTitle = "Night Wind-Down",
                    mode = "SCHEDULE",
                    startedAtMillis = now - 2L * dayMs,
                    endedAtMillis = now - 2L * dayMs + 3_600_000L,
                    durationMinutes = 60L,
                    outcome = "COMPLETED",
                    earlyExitMethod = ""
                )

                val weeklyUsage = repo.observeUsageSince("u1", daysBack = 7).first()
                val monthlyUsage = repo.observeUsageSince("u1", daysBack = 30).first()
                val sessionLogs = repo.observeAllSessionLogs("u1").first()

                assertEquals(1, weeklyUsage.size)
                assertEquals("Chrome Browser", weeklyUsage.first().appName)
                assertEquals(2, monthlyUsage.size)
                assertEquals(1, sessionLogs.size)
                assertEquals("Night Wind-Down", sessionLogs.first().sessionTitle)
                assertEquals("COMPLETED", sessionLogs.first().outcome)
            } finally {
                db.close()
            }
        }
    }

    @Test
    fun `csv export helper builds valid header, rows, and escapes commas and quotes`() {
        val now = 1_700_000_000_000L
        val usageLogs = listOf(
            com.example.data.local.HistoricalAppUsageEntity(
                id = "u1_com.browser_2026-11-19_14",
                userId = "u1",
                packageName = "com.browser",
                appName = "Pink, \"Fast\" Browser",
                dateKey = "2026-11-19",
                dayOfWeek = 4L,
                hourOfDay = 14L,
                screenTimeMinutes = 45L,
                restrictedMinutes = 15L,
                timestampMillis = now,
                source = "SYSTEM"
            )
        )
        val sessionLogs = listOf(
            com.example.data.local.RestrictionSessionLogEntity(
                id = "sess_1",
                userId = "u1",
                sessionTitle = "Night, Calm Focus",
                mode = "SCHEDULE",
                dateKey = "2026-11-19",
                dayOfWeek = 4L,
                startedAtMillis = now - 3_600_000L,
                endedAtMillis = now,
                durationMinutes = 60L,
                outcome = "COMPLETED",
                earlyExitMethod = ""
            )
        )

        val csv = com.example.data.CsvExportHelper.buildUsageAndSessionCsv(usageLogs, sessionLogs)
        val lines = csv.trim().lines()

        assertEquals(3, lines.size)
        assertTrue(lines[0].startsWith("record_type,date,day_of_week,time_info,name"))
        assertTrue(lines[1].contains("APP_USAGE"))
        assertTrue(lines[1].contains("\"Pink, \"\"Fast\"\" Browser\""))
        assertTrue(lines[1].contains(",14:00,"))
        assertTrue(lines[2].contains("SESSION_HISTORY"))
        assertTrue(lines[2].contains("\"Night, Calm Focus\""))
        assertTrue(lines[2].contains("COMPLETED"))
    }

    @Test
    fun `streak reward calculator evaluates daily limits and awards day 7 bonus keys`() {
        val rulesMet = listOf(
            AppRule(
                packageName = "com.browser",
                appName = "Browser",
                dailyLimitEnabled = true,
                dailyLimitMinutes = 60L,
                usedTodayMinutes = 45L
            )
        )
        val rulesExceeded = listOf(
            AppRule(
                packageName = "com.browser",
                appName = "Browser",
                dailyLimitEnabled = true,
                dailyLimitMinutes = 60L,
                usedTodayMinutes = 75L
            )
        )

        assertTrue(com.example.model.StreakRewardCalculator.evaluateDailyGoals(rulesMet).allLimitsMet)
        val exceededGoal = com.example.model.StreakRewardCalculator.evaluateDailyGoals(rulesExceeded)
        assertFalse(exceededGoal.allLimitsMet)
        assertEquals(1, exceededGoal.exceededApps.size)

        // Consecutive day 7 milestone awards 1 daily key + 10 bonus keys = 11 keys and 55 plaps
        val day7Outcome = com.example.model.StreakRewardCalculator.computeReward(
            currentStreak = 6L,
            lastSessionDate = "2026-11-18",
            todayDateKey = "2026-11-19",
            yesterdayDateKey = "2026-11-18"
        )
        assertEquals(7L, day7Outcome.newStreak)
        assertEquals(1L, day7Outcome.dailyKeysAwarded)
        assertEquals(10L, day7Outcome.bonusKeysAwarded)
        assertEquals(11L, day7Outcome.totalKeysAwarded)
        assertEquals(55L, day7Outcome.plapsAwarded)
        assertTrue(day7Outcome.isWeeklyMilestone)
        assertEquals(7, com.example.model.StreakRewardCalculator.currentCycleFilledDays(7L))

        // Missed day resets streak to 1
        val resetOutcome = com.example.model.StreakRewardCalculator.computeReward(
            currentStreak = 5L,
            lastSessionDate = "2026-11-15",
            todayDateKey = "2026-11-19",
            yesterdayDateKey = "2026-11-18"
        )
        assertEquals(1L, resetOutcome.newStreak)
        assertEquals(0L, resetOutcome.bonusKeysAwarded)
        assertFalse(resetOutcome.isWeeklyMilestone)
    }

    @Test
    fun `wall block decision triggers with countdown target when daily limit is exceeded`() {
        val now = 1_700_000_000_000L
        val rules = listOf(
            AppRule(
                packageName = "com.social.feed",
                appName = "Social Feed",
                dailyLimitEnabled = true,
                dailyLimitMinutes = 30L,
                usedTodayMinutes = 30L
            )
        )
        RestrictionRuntimeState.updateState(null, rules, UserProfile(userId = "u1"), emptyList())

        val decision = RestrictionRuntimeState.evaluatePackageBlock(
            packageName = "com.social.feed",
            nowMillis = now,
            currentMinutesOfDay = 15 * 60,
            currentDayOfWeek = 3
        )

        assertTrue(decision.isBlocked)
        assertTrue(decision.isDailyLimitExceeded)
        assertTrue(decision.unlockTargetMillis > now)
        assertTrue(decision.reasonTitle.contains("Daily Limit Exceeded"))
    }

    @Test
    fun `weekly challenge catalog computes scores, week keys, and badge tiers accurately`() {
        val wk = com.example.model.WeeklyChallengeCatalog.currentWeekKey(1_700_000_000_000L)
        assertTrue(wk.matches(Regex("""\d{4}-W\d{2}""")))
        assertEquals(3, com.example.model.WeeklyChallengeCatalog.weeklyChallenges.size)

        val score = com.example.model.WeeklyChallengeCatalog.computeScore(
            restrictedMinutes = 300L,
            tasksCompleted = 4L,
            dailyStreak = 8L
        )
        // 300 + 4*50 + 8*25 = 700
        assertEquals(700L, score)
        assertEquals("Royal Princess", com.example.model.WeeklyChallengeCatalog.badgeForScore(score))
        assertEquals("Crown Empress", com.example.model.WeeklyChallengeCatalog.badgeForScore(1500L))
        assertEquals("Silk Novice", com.example.model.WeeklyChallengeCatalog.badgeForScore(50L))
    }

    @Test
    fun `local customization datastore encodes and decodes voice lines and adhd daily insights`() {
        val sampleLines = listOf(
            com.example.model.RecordedVoiceLine(
                id = "v1",
                label = "Pretty Focus Cue",
                filePath = "/data/local/voice_v1.m4a",
                durationSeconds = 4,
                createdAtMillis = 123456L
            )
        )
        val encodedLines = com.example.data.local.LocalCustomizationDataStore.encodeVoiceLines(sampleLines)
        val decodedLines = com.example.data.local.LocalCustomizationDataStore.decodeVoiceLines(encodedLines)
        assertEquals(1, decodedLines.size)
        assertEquals("Pretty Focus Cue", decodedLines.first().label)
        assertEquals(4, decodedLines.first().durationSeconds)

        val insight = com.example.model.AdhdDailyInsight(
            dateKey = "2026-11-19",
            dailyAffirmation = "You shine brightest one calm sprint at a time!",
            adhdFocusTip = "Pair your 15m sprint with music and park extra thoughts below.",
            scheduleOptimizationTip = "Lock social apps from 20:00 to 23:00.",
            recommendedMicroSprintMinutes = 15,
            peakDistractionWindow = "Evening 18-23"
        )
        val encodedInsight = com.example.data.local.LocalCustomizationDataStore.encodeAdhdInsight(insight)
        val decodedInsight = com.example.data.local.LocalCustomizationDataStore.decodeAdhdInsight(encodedInsight)
        assertEquals(insight, decodedInsight)
    }

    @Test
    fun `voice line audio manager creates recording file path and handles missing files gracefully`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = com.example.media.VoiceLineAudioManager(context)
        val file = manager.createRecordingFile("test_cue")
        assertTrue(file.absolutePath.endsWith("voice_test_cue.m4a"))
        manager.playVoiceLineOrDefault(filePath = null)
        manager.release()
    }

    @Test
    fun `small wins datastore preserves same day progress and resets completion on a new day`() {
        val initialTasks = listOf(
            com.example.model.SmallWinTask(
                id = "sw_1",
                title = "Finish Pomodoro",
                completed = true,
                plapReward = 3L,
                isPreset = true
            ),
            com.example.model.SmallWinTask(
                id = "sw_custom",
                title = "Custom Hydration Goal",
                completed = true,
                plapReward = 2L,
                isPreset = false
            )
        )
        val state = com.example.model.SmallWinsDailyState(
            dateKey = "2026-11-19",
            tasks = initialTasks,
            rewardedIdsToday = setOf("sw_1", "sw_custom")
        )
        val encoded = com.example.data.local.LocalCustomizationDataStore.encodeSmallWinsState(state)

        // Same day restores completed flags and rewarded IDs
        val sameDayDecoded = com.example.data.local.LocalCustomizationDataStore.decodeSmallWinsState(
            raw = encoded,
            todayDateKey = "2026-11-19"
        )
        assertEquals(2, sameDayDecoded.tasks.size)
        assertTrue(sameDayDecoded.tasks.all { it.completed })
        assertEquals(setOf("sw_1", "sw_custom"), sameDayDecoded.rewardedIdsToday)

        // Next day keeps custom task titles but resets completed and rewardedIdsToday
        val nextDayDecoded = com.example.data.local.LocalCustomizationDataStore.decodeSmallWinsState(
            raw = encoded,
            todayDateKey = "2026-11-20"
        )
        assertEquals("2026-11-20", nextDayDecoded.dateKey)
        assertEquals(2, nextDayDecoded.tasks.size)
        assertFalse(nextDayDecoded.tasks.any { it.completed })
        assertTrue(nextDayDecoded.rewardedIdsToday.isEmpty())
    }

    @Test
    fun `welcome splash config and adhd copy defaults match requested strings`() {
        assertEquals("Stop Gooning Beta", com.example.model.DefaultAdhdContent.HEADER_TITLE)
        assertEquals(
            "You Not Man Anymore,\nYou Are Just Cock Slave For Real Men's",
            com.example.model.DefaultAdhdContent.SUBHEADER_LINES
        )
        assertEquals(
            "Get Up And Be Sissy Slut You Always Wanted",
            com.example.model.DefaultAdhdContent.SISSY_MOTTO
        )

        val defaultSplash = com.example.model.WelcomeSplashConfig()
        assertTrue(defaultSplash.enabled)
        assertEquals("auth_photo", defaultSplash.wallpaperKey)
        assertEquals("#B8255D", defaultSplash.fontColorHex)
        assertEquals("playfair", defaultSplash.fontKey)
        assertTrue(defaultSplash.note.contains("Stop Gooning Beta"))
    }

    @Test
    fun `adhd friendly mode persists global toggle and selects first uncompleted active task`() {
        kotlinx.coroutines.test.runTest {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val store = com.example.data.local.LocalCustomizationDataStore(context)
            assertFalse(store.adhdCalmMode.first())

            store.setAdhdCalmMode(true)
            assertTrue(store.adhdCalmMode.first())

            store.setAdhdCalmMode(false)
            assertFalse(store.adhdCalmMode.first())

            val tasks = com.example.model.DefaultAdhdContent.defaultSmallWinPresets()
            val firstActive = tasks.firstOrNull { !it.completed }
            assertEquals("sw_check_schedule", firstActive?.id)

            val afterCompletingFirst = tasks.map {
                if (it.id == firstActive?.id) it.copy(completed = true) else it
            }
            val nextActive = afterCompletingFirst.firstOrNull { !it.completed }
            assertEquals("sw_pomodoro_sprint", nextActive?.id)
        }
    }
}
