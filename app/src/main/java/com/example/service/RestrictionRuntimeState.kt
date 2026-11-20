package com.example.service

import com.example.model.AppRule
import com.example.model.RestrictionSession
import com.example.model.SchedulePlan
import com.example.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar

data class BlockDecision(
    val isBlocked: Boolean,
    val reasonTitle: String,
    val unlockTimeHint: String,
    val rule: AppRule? = null,
    val matchedSchedule: SchedulePlan? = null,
    val unlockTargetMillis: Long = 0L,
    val isDailyLimitExceeded: Boolean = false
)

object RestrictionRuntimeState {
    private val _activeSession = MutableStateFlow<RestrictionSession?>(null)
    val activeSession: StateFlow<RestrictionSession?> = _activeSession.asStateFlow()

    private val _appRules = MutableStateFlow<List<AppRule>>(emptyList())
    val appRules: StateFlow<List<AppRule>> = _appRules.asStateFlow()

    private val _schedules = MutableStateFlow<List<SchedulePlan>>(emptyList())
    val schedules: StateFlow<List<SchedulePlan>> = _schedules.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private val _wallBlockBgImageUri = MutableStateFlow("")
    val wallBlockBgImageUri: StateFlow<String> = _wallBlockBgImageUri.asStateFlow()

    private val _liveSystemMinutesByPackage = MutableStateFlow<Map<String, Long>>(emptyMap())

    fun updateState(
        session: RestrictionSession?,
        rules: List<AppRule>,
        profile: UserProfile?,
        schedules: List<SchedulePlan> = _schedules.value
    ) {
        _activeSession.value = session
        _appRules.value = rules
        _schedules.value = schedules
        _userProfile.value = profile
    }

    fun updateWallBlockBgImageUri(uri: String) {
        _wallBlockBgImageUri.value = uri
    }

    fun updateLiveSystemUsageMinutes(usageByPackage: Map<String, Long>) {
        _liveSystemMinutesByPackage.value = usageByPackage
    }

    fun computeNextMidnightMillis(nowMillis: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            add(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun isScheduleWindowActive(
        plan: SchedulePlan,
        nowMillis: Long,
        currentMinutesOfDay: Int,
        currentDayOfWeek: Int
    ): Boolean {
        if (!plan.enabled) return false
        if (plan.unlockedUntilMillis > nowMillis) return false
        if (plan.daysOfWeek.isNotEmpty() && currentDayOfWeek.toLong() !in plan.daysOfWeek) {
            return false
        }
        val start = plan.startMinutes.toInt()
        val end = plan.endMinutes.toInt()
        return if (start == end) {
            true // 24-hour full-day schedule
        } else if (start < end) {
            currentMinutesOfDay in start until end
        } else {
            currentMinutesOfDay >= start || currentMinutesOfDay < end
        }
    }

    fun getCurrentlyActiveSchedules(
        nowMillis: Long = System.currentTimeMillis(),
        schedules: List<SchedulePlan> = _schedules.value
    ): List<SchedulePlan> {
        val (minsOfDay, dayOfWeek) = extractMinutesAndDay(nowMillis)
        return schedules.filter { isScheduleWindowActive(it, nowMillis, minsOfDay, dayOfWeek) }
    }

    fun computeScheduleEndMillis(plan: SchedulePlan, nowMillis: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val start = plan.startMinutes.toInt()
        val end = plan.endMinutes.toInt()
        val currentMins = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

        val endCal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, end / 60)
            set(Calendar.MINUTE, end % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (start == end) {
            endCal.add(Calendar.DAY_OF_MONTH, 1)
        } else if (end <= currentMins) {
            endCal.add(Calendar.DAY_OF_MONTH, 1)
        }
        return endCal.timeInMillis
    }

    fun hasActiveTamperLock(nowMillis: Long = System.currentTimeMillis()): Boolean {
        val session = _activeSession.value
        val sessionLocked = session != null && session.status == "ACTIVE" &&
            session.endsAtMillis > nowMillis && session.pausedUntilMillis <= nowMillis
        if (sessionLocked) return true
        return getCurrentlyActiveSchedules(nowMillis).isNotEmpty()
    }

    fun evaluatePackageBlock(
        packageName: String,
        nowMillis: Long = System.currentTimeMillis(),
        currentMinutesOfDay: Int = extractMinutesAndDay(nowMillis).first,
        currentDayOfWeek: Int = extractMinutesAndDay(nowMillis).second
    ): BlockDecision {
        if (packageName == "com.aistudio.besissy.qkvnxm" || packageName == "com.example") {
            return BlockDecision(false, "", "")
        }

        val rule = _appRules.value.firstOrNull { it.packageName == packageName }
        if (rule != null && rule.tempUnblockUntilMillis > nowMillis) {
            return BlockDecision(false, "", "", rule)
        }

        // 1. Evaluate customizable SchedulePlans (per-schedule Allow / Restrict takes top priority)
        for (plan in _schedules.value) {
            if (!isScheduleWindowActive(plan, nowMillis, currentMinutesOfDay, currentDayOfWeek)) continue

            if (packageName in plan.allowedPackages) {
                continue // Explicitly allowed in this schedule
            }
            val explicitlyRestricted = packageName in plan.restrictedPackages
            val blockedByStrictAllow = plan.modeType == "ALLOW_ONLY_SELECTED" && (rule?.necessaryApp != true)

            if (explicitlyRestricted || blockedByStrictAllow) {
                val reason = plan.customBlockReason.ifBlank { "${plan.name} Schedule Active" }
                val targetMs = computeScheduleEndMillis(plan, nowMillis)
                return BlockDecision(
                    isBlocked = true,
                    reasonTitle = reason,
                    unlockTimeHint = "Available again at ${formatMinutes(plan.endMinutes.toInt())}",
                    rule = rule,
                    matchedSchedule = plan,
                    unlockTargetMillis = targetMs
                )
            }
        }

        // 2. Global Allowed App whitelist from Settings
        if (rule?.necessaryApp == true) {
            return BlockDecision(false, "", "", rule)
        }

        // 3. Legacy active session fallback if present
        val session = _activeSession.value
        val sessionRunning = session != null &&
            session.status == "ACTIVE" &&
            session.endsAtMillis > nowMillis &&
            session.pausedUntilMillis <= nowMillis

        if (sessionRunning && (rule?.restrictedInSession == true)) {
            val minsLeft = ((session!!.endsAtMillis - nowMillis) / 60_000L).coerceAtLeast(1L)
            return BlockDecision(
                isBlocked = true,
                reasonTitle = "Restricted Window Active",
                unlockTimeHint = "Unlocks in ${minsLeft / 60}h ${minsLeft % 60}m",
                rule = rule,
                unlockTargetMillis = session.endsAtMillis
            )
        }

        // 4. Legacy per-app schedule fallback
        if (rule != null && rule.scheduleEnabled) {
            val start = rule.scheduleStartMinutes.toInt()
            val end = rule.scheduleEndMinutes.toInt()
            val inWindow = if (start <= end) {
                currentMinutesOfDay in start until end
            } else {
                currentMinutesOfDay >= start || currentMinutesOfDay < end
            }
            if (inWindow) {
                val pseudoPlan = SchedulePlan(startMinutes = rule.scheduleStartMinutes, endMinutes = rule.scheduleEndMinutes)
                return BlockDecision(
                    isBlocked = true,
                    reasonTitle = "Scheduled Quiet Window",
                    unlockTimeHint = "Available again at ${formatMinutes(end)}",
                    rule = rule,
                    unlockTargetMillis = computeScheduleEndMillis(pseudoPlan, nowMillis)
                )
            }
        }

        // 5. Daily Limit Mode (combines rule.usedTodayMinutes and live UsageStatsManager foreground usage)
        if (rule != null && rule.dailyLimitEnabled) {
            val liveSystemUsed = _liveSystemMinutesByPackage.value[packageName] ?: 0L
            val effectiveUsed = maxOf(rule.usedTodayMinutes, liveSystemUsed)
            if (effectiveUsed >= rule.dailyLimitMinutes) {
                return BlockDecision(
                    isBlocked = true,
                    reasonTitle = "Daily Limit Exceeded (${effectiveUsed}m / ${rule.dailyLimitMinutes}m)",
                    unlockTimeHint = "Resets at 00:00 (or temp unblock with Plaps)",
                    rule = rule,
                    unlockTargetMillis = computeNextMidnightMillis(nowMillis),
                    isDailyLimitExceeded = true
                )
            }
        }

        return BlockDecision(false, "", "", rule)
    }

    private fun extractMinutesAndDay(nowMillis: Long): Pair<Int, Int> {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val mins = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val dow = ((cal.get(Calendar.DAY_OF_WEEK) + 5) % 7) + 1 // 1=Mon..7=Sun
        return mins to dow
    }

    fun formatMinutes(totalMinutes: Int): String {
        val clamped = ((totalMinutes % 1440) + 1440) % 1440
        val h = clamped / 60
        val m = clamped % 60
        return "%02d:%02d".format(h, m)
    }
}
