package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.example.WallBlockActivity
import com.example.data.DeviceAppsDataSource
import java.util.Calendar

class BeSissyBlockerAccessibilityService : AccessibilityService() {

    private var lastBlockedLaunchTime = 0L
    private var lastUsageRefreshTime = 0L
    private lateinit var overlayManager: WallBlockOverlayWindowManager
    private lateinit var appsDataSource: DeviceAppsDataSource

    override fun onServiceConnected() {
        super.onServiceConnected()
        overlayManager = WallBlockOverlayWindowManager(applicationContext)
        appsDataSource = DeviceAppsDataSource(applicationContext)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val eventType = event?.eventType ?: return
        if (eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        ) return

        val pkg = event.packageName?.toString() ?: return
        val now = System.currentTimeMillis()

        // Periodically refresh real foreground usage for daily limit enforcement even mid-session
        if (::appsDataSource.isInitialized && now - lastUsageRefreshTime > 4_000L) {
            lastUsageRefreshTime = now
            val todayUsage = appsDataSource.readTodaySystemUsage("local_service")
            if (todayUsage.isNotEmpty()) {
                RestrictionRuntimeState.updateLiveSystemUsageMinutes(
                    todayUsage.associate { it.packageName to it.screenTimeMinutes }
                )
            }
        }

        // For TYPE_WINDOW_CONTENT_CHANGED, only enforce if an app actually crosses a daily usage limit
        if (eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            val cal = Calendar.getInstance()
            val minutesOfDay = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
            val dayOfWeek = ((cal.get(Calendar.DAY_OF_WEEK) + 5) % 7) + 1
            val decision = RestrictionRuntimeState.evaluatePackageBlock(pkg, now, minutesOfDay, dayOfWeek)
            if (decision.isBlocked && decision.isDailyLimitExceeded && now - lastBlockedLaunchTime > 1500L) {
                triggerWallBlock(pkg, decision, now)
            }
            return
        }

        val cal = Calendar.getInstance()
        val minutesOfDay = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val dayOfWeek = ((cal.get(Calendar.DAY_OF_WEEK) + 5) % 7) + 1

        // Tamper shield during active schedule or lock: prevent Force Stop / Clear Data / Uninstall in Settings
        val tamperLocked = RestrictionRuntimeState.hasActiveTamperLock(now)
        if (tamperLocked && pkg == "com.android.settings") {
            val textContent = event.text?.joinToString(" ").orEmpty()
            if (textContent.contains("Be Sissy", ignoreCase = true) ||
                textContent.contains("Force stop", ignoreCase = true) ||
                textContent.contains("Clear data", ignoreCase = true) ||
                textContent.contains("Uninstall", ignoreCase = true)
            ) {
                lastBlockedLaunchTime = now
                showHighPriorityWallBlock(
                    appName = "System Settings",
                    packageName = pkg,
                    reason = "Tamper Guard: App settings are locked during an active schedule.",
                    unlockHint = "Finish or unlock your active schedule first.",
                    unlockTargetMillis = now + 3_600_000L
                )
                return
            }
        }

        val decision = RestrictionRuntimeState.evaluatePackageBlock(
            packageName = pkg,
            nowMillis = now,
            currentMinutesOfDay = minutesOfDay,
            currentDayOfWeek = dayOfWeek
        )
        if (decision.isBlocked && now - lastBlockedLaunchTime > 1200L) {
            triggerWallBlock(pkg, decision, now)
        }
    }

    private fun triggerWallBlock(pkg: String, decision: BlockDecision, now: Long) {
        lastBlockedLaunchTime = now
        val resolvedName = decision.rule?.appName ?: try {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
        } catch (_: Exception) {
            pkg
        }
        val targetMs = if (decision.unlockTargetMillis > 0L) {
            decision.unlockTargetMillis
        } else {
            RestrictionRuntimeState.computeNextMidnightMillis(now)
        }
        showHighPriorityWallBlock(
            appName = resolvedName,
            packageName = pkg,
            reason = decision.reasonTitle,
            unlockHint = decision.unlockTimeHint,
            unlockTargetMillis = targetMs
        )
    }

    private fun showHighPriorityWallBlock(
        appName: String,
        packageName: String,
        reason: String,
        unlockHint: String,
        unlockTargetMillis: Long
    ) {
        if (::overlayManager.isInitialized && overlayManager.canShowOverlay()) {
            val shown = overlayManager.showOverlay(
                appName = appName,
                packageName = packageName,
                reasonTitle = reason,
                unlockHint = unlockHint,
                unlockTargetMillis = unlockTargetMillis
            )
            if (shown) return
        }

        val intent = Intent(this, WallBlockActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
            )
            putExtra(WallBlockActivity.EXTRA_APP_NAME, appName)
            putExtra(WallBlockActivity.EXTRA_PACKAGE_NAME, packageName)
            putExtra(WallBlockActivity.EXTRA_REASON, reason)
            putExtra(WallBlockActivity.EXTRA_UNLOCK_HINT, unlockHint)
            putExtra(WallBlockActivity.EXTRA_UNLOCK_TARGET_MILLIS, unlockTargetMillis)
        }
        startActivity(intent)
    }

    override fun onDestroy() {
        if (::overlayManager.isInitialized) {
            overlayManager.dismissOverlay()
        }
        super.onDestroy()
    }

    override fun onInterrupt() = Unit
}
