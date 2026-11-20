package com.example.data

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Process
import android.provider.Settings
import com.example.model.InstalledAppItem
import com.example.model.UsageLogEntry
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class DeviceAppsDataSource(private val context: Context) {

    fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun isOverlayPermissionGranted(): Boolean = Settings.canDrawOverlays(context)

    fun loadLaunchableApps(): List<InstalledAppItem> {
        val pm = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolved = pm.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)
        val selfPkg = context.packageName

        val deviceApps = resolved
            .mapNotNull { info ->
                val pkg = info.activityInfo?.packageName ?: return@mapNotNull null
                if (pkg == selfPkg) return@mapNotNull null
                val label = info.loadLabel(pm)?.toString()?.trim().orEmpty()
                if (label.isEmpty()) return@mapNotNull null
                val isEssential = pkg.contains("dialer") ||
                    pkg.contains("phone") ||
                    pkg.contains("messaging") ||
                    pkg.contains("mms") ||
                    pkg.contains("clock") ||
                    pkg.contains("deskclock") ||
                    pkg.contains("contacts") ||
                    pkg.contains("camera")
                val category = when {
                    isEssential -> "Essential"
                    pkg.contains("chrome") || pkg.contains("browser") -> "Browser"
                    pkg.contains("youtube") || pkg.contains("music") || pkg.contains("video") -> "Media"
                    pkg.contains("game") -> "Games"
                    else -> "App"
                }
                InstalledAppItem(
                    packageName = pkg,
                    appName = label,
                    category = category,
                    isSystemEssential = isEssential
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.appName.lowercase() }

        // Ensure essential & common target apps are always present on minimal streaming emulators
        val baseline = listOf(
            InstalledAppItem("com.android.dialer", "Phone", "Essential", isSystemEssential = true),
            InstalledAppItem("com.google.android.apps.messaging", "Messages", "Essential", isSystemEssential = true),
            InstalledAppItem("com.google.android.deskclock", "Clock", "Essential", isSystemEssential = true),
            InstalledAppItem("com.android.camera", "Camera", "Essential", isSystemEssential = true),
            InstalledAppItem("com.android.chrome", "Chrome Browser", "Browser", isSystemEssential = false),
            InstalledAppItem("com.mobile.legends", "Mobile Legends", "Games", isSystemEssential = false),
            InstalledAppItem("com.google.android.youtube", "YouTube", "Media", isSystemEssential = false),
            InstalledAppItem("com.instagram.android", "Instagram", "Social", isSystemEssential = false),
            InstalledAppItem("com.zhiliaoapp.musically", "TikTok", "Social", isSystemEssential = false),
            InstalledAppItem("com.discord", "Discord", "Social", isSystemEssential = false)
        )

        val merged = (deviceApps + baseline).distinctBy { it.packageName }
        return merged.sortedWith(
            compareByDescending<InstalledAppItem> { it.isSystemEssential }
                .thenBy { it.appName.lowercase() }
        )
    }

    fun readTodaySystemUsage(userId: String): List<UsageLogEntry> {
        if (!hasUsageStatsPermission()) return emptyList()
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val cal = Calendar.getInstance()
        val end = cal.timeInMillis
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        val start = cal.timeInMillis

        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end) ?: return emptyList()
        val pm = context.packageManager
        val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val dayOfWeek = (((cal.get(Calendar.DAY_OF_WEEK) + 5) % 7) + 1).toLong()
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY).toLong()

        return stats
            .filter { it.totalTimeInForeground >= 60_000L && it.packageName != context.packageName }
            .mapNotNull { stat ->
                val mins = (stat.totalTimeInForeground / 60_000L).coerceIn(1L, 1440L)
                val appName = try {
                    pm.getApplicationLabel(pm.getApplicationInfo(stat.packageName, 0)).toString()
                } catch (_: Exception) {
                    stat.packageName.substringAfterLast('.')
                }
                UsageLogEntry(
                    userId = userId,
                    packageName = stat.packageName,
                    appName = appName,
                    dateKey = dateKey,
                    dayOfWeek = dayOfWeek,
                    hourOfDay = hour,
                    screenTimeMinutes = mins,
                    restrictedMinutes = 0L
                )
            }
            .sortedByDescending { it.screenTimeMinutes }
            .take(15)
    }

    fun readHistoricalSystemUsage(userId: String, daysBack: Int = 30): List<UsageLogEntry> {
        if (!hasUsageStatsPermission()) return emptyList()
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val pm = context.packageManager
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val end = System.currentTimeMillis()
        val start = end - daysBack.coerceIn(1, 90) * 86_400_000L
        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end) ?: return emptyList()

        return stats
            .filter { it.totalTimeInForeground >= 60_000L && it.packageName != context.packageName }
            .mapNotNull { stat ->
                val stamp = if (stat.lastTimeStamp > 0L) stat.lastTimeStamp else stat.firstTimeStamp
                val cal = Calendar.getInstance().apply { timeInMillis = stamp }
                val dateKey = dateFormat.format(Date(stamp))
                val dayOfWeek = (((cal.get(Calendar.DAY_OF_WEEK) + 5) % 7) + 1).toLong()
                val hour = cal.get(Calendar.HOUR_OF_DAY).toLong()
                val mins = (stat.totalTimeInForeground / 60_000L).coerceIn(1L, 1440L)
                val appName = try {
                    pm.getApplicationLabel(pm.getApplicationInfo(stat.packageName, 0)).toString()
                } catch (_: Exception) {
                    stat.packageName.substringAfterLast('.')
                }
                UsageLogEntry(
                    userId = userId,
                    packageName = stat.packageName,
                    appName = appName,
                    dateKey = dateKey,
                    dayOfWeek = dayOfWeek,
                    hourOfDay = hour,
                    screenTimeMinutes = mins,
                    restrictedMinutes = 0L
                )
            }
            .distinctBy { "${it.dateKey}_${it.packageName}_${it.hourOfDay}" }
            .sortedByDescending { it.dateKey }
            .take(180)
    }
}
