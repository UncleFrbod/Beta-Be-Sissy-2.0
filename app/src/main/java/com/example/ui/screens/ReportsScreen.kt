package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.AddChart
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TimerOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.CsvExportHelper
import com.example.data.UsageHistoryRepository
import com.example.data.local.HistoricalAppUsageEntity
import com.example.model.InstalledAppItem
import com.example.model.ReportPeriod
import com.example.ui.components.CoquetteCard
import com.example.ui.theme.BerryInk
import com.example.ui.theme.BlushElevated
import com.example.ui.theme.BlushSurface
import com.example.ui.theme.PinkAccent
import com.example.ui.theme.PinkContainer
import com.example.ui.theme.PinkPrimary
import com.example.ui.theme.RoseGoldBorder
import com.example.viewmodel.MainDashboardSnapshot
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val DAY_MS = 86_400_000L

@Composable
fun ReportsScreen(
    snapshot: MainDashboardSnapshot,
    isAnalyzingAdhd: Boolean = false,
    adhdInsightError: String? = null,
    onGenerateAdhdInsight: () -> Unit = {},
    onStartMicroSprint: (Long) -> Unit = {},
    onRecordSampleUsage: (InstalledAppItem, Long, Long) -> Unit,
    onShowMessage: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var period by remember { mutableStateOf(ReportPeriod.WEEKLY) }
    val now = remember { System.currentTimeMillis() }
    val todayKey = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(now)) }

    // Prefer Room historical logs and merge any freshly-observed unsynced logs
    val unifiedUsage = remember(snapshot.historicalUsage, snapshot.usageLogs) {
        val fromRoom = snapshot.historicalUsage
        val roomIds = fromRoom.map { it.id }.toHashSet()
        val extra = snapshot.usageLogs.mapNotNull { entry ->
            val id = "${entry.userId}_${entry.dateKey}_${entry.packageName}_${entry.hourOfDay}"
            if (id in roomIds) null else HistoricalAppUsageEntity(
                id = id,
                userId = entry.userId,
                packageName = entry.packageName,
                appName = entry.appName,
                dateKey = entry.dateKey,
                timestampMillis = UsageHistoryRepository.parseDateKeyAndHourMillis(entry.dateKey, entry.hourOfDay),
                dayOfWeek = entry.dayOfWeek,
                hourOfDay = entry.hourOfDay,
                screenTimeMinutes = entry.screenTimeMinutes,
                restrictedMinutes = entry.restrictedMinutes,
                source = "LIVE"
            )
        }
        fromRoom + extra
    }

    val filteredUsage = remember(unifiedUsage, period, todayKey, now) {
        when (period) {
            ReportPeriod.DAILY -> unifiedUsage.filter { it.dateKey == todayKey }
            ReportPeriod.WEEKLY -> {
                val cutoff = now - 7L * DAY_MS
                unifiedUsage.filter { it.timestampMillis >= cutoff }
            }
            ReportPeriod.MONTHLY -> {
                val cutoff = now - 30L * DAY_MS
                unifiedUsage.filter { it.timestampMillis >= cutoff }
            }
        }
    }

    val filteredSessionLogs = remember(snapshot.sessionHistoryLogs, period, todayKey, now) {
        when (period) {
            ReportPeriod.DAILY -> snapshot.sessionHistoryLogs.filter { it.dateKey == todayKey }
            ReportPeriod.WEEKLY -> {
                val cutoff = now - 7L * DAY_MS
                snapshot.sessionHistoryLogs.filter { it.startedAtMillis >= cutoff }
            }
            ReportPeriod.MONTHLY -> {
                val cutoff = now - 30L * DAY_MS
                snapshot.sessionHistoryLogs.filter { it.startedAtMillis >= cutoff }
            }
        }
    }

    val totalScreenTime = filteredUsage.sumOf { it.screenTimeMinutes }
    val totalRestrictedTime = filteredUsage.sumOf { it.restrictedMinutes }
    val completedSessions = filteredSessionLogs.count { it.outcome == "COMPLETED" }
    val earlyExitSessions = filteredSessionLogs.count { it.outcome == "ENDED_EARLY" }

    val appTotals = remember(filteredUsage) {
        filteredUsage.groupBy { it.appName }
            .map { (name, entries) ->
                Triple(
                    name,
                    entries.sumOf { it.screenTimeMinutes },
                    entries.sumOf { it.restrictedMinutes }
                )
            }
            .sortedByDescending { it.second + it.third }
    }

    // Dual metric per weekday: (restrictedMinutes, activeMinutes)
    val weekdayStats = remember(filteredUsage) {
        (1..7).map { day ->
            val dayEntries = filteredUsage.filter { it.dayOfWeek.toInt() == day }
            dayEntries.sumOf { it.restrictedMinutes } to dayEntries.sumOf { it.screenTimeMinutes }
        }
    }

    // 4-week rolling buckets for Monthly pattern view (Wk 1 oldest -> Wk 4 most recent 7 days)
    val monthlyWeekBuckets = remember(unifiedUsage, now) {
        (3 downTo 0).map { weeksAgo ->
            val bucketEnd = now - weeksAgo * 7L * DAY_MS
            val bucketStart = bucketEnd - 7L * DAY_MS
            val entries = unifiedUsage.filter { it.timestampMillis in bucketStart..bucketEnd }
            entries.sumOf { it.restrictedMinutes } to entries.sumOf { it.screenTimeMinutes }
        }
    }

    val hourlyBuckets = remember(filteredUsage) {
        listOf(0..5, 6..11, 12..17, 18..23).map { range ->
            filteredUsage.filter { it.hourOfDay.toInt() in range }.sumOf { it.screenTimeMinutes }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Screen Time & Focus Reports",
            style = MaterialTheme.typography.headlineMedium,
            color = BerryInk,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Room History Vault: ${unifiedUsage.size} usage records • ${snapshot.sessionHistoryLogs.size} restriction logs",
            style = MaterialTheme.typography.labelMedium,
            color = BerryInk.copy(alpha = 0.85f)
        )

        val csvPayload = remember(unifiedUsage, snapshot.sessionHistoryLogs) {
            CsvExportHelper.buildUsageAndSessionCsv(
                usageLogs = unifiedUsage,
                sessionLogs = snapshot.sessionHistoryLogs
            )
        }
        val createCsvLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("text/csv")
        ) { uri ->
            if (uri != null) {
                CsvExportHelper.writeCsvToUri(context, uri, csvPayload)
                    .onSuccess {
                        onShowMessage("Exported CSV (${unifiedUsage.size} usage + ${snapshot.sessionHistoryLogs.size} sessions) ♡")
                    }
                    .onFailure {
                        onShowMessage(it.localizedMessage ?: "CSV export failed")
                    }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ReportPeriod.entries.forEach { option ->
                FilterChip(
                    selected = period == option,
                    onClick = { period = option },
                    label = { Text(option.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PinkPrimary,
                        selectedLabelColor = BlushSurface,
                        containerColor = PinkContainer,
                        labelColor = BerryInk
                    ),
                    modifier = Modifier.testTag("report_period_${option.name.lowercase()}")
                )
            }
        }

        // Belle Gemini Weekly Report Analysis + Severe ADHD Daily Tips
        CoquetteCard(modifier = Modifier.fillMaxWidth(), containerColor = PinkContainer.copy(alpha = 0.86f)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = PinkPrimary)
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Belle's Gemini Weekly ADHD Coach",
                            style = MaterialTheme.typography.titleMedium,
                            color = BerryInk,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Analyzes your 7-day report for Sissy motivation & severe-ADHD focus pacing",
                            style = MaterialTheme.typography.bodySmall,
                            color = BerryInk.copy(alpha = 0.85f)
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))

                Button(
                    onClick = onGenerateAdhdInsight,
                    enabled = !isAnalyzingAdhd,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("generate_weekly_ai_insight_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PinkPrimary,
                        contentColor = BlushSurface
                    )
                ) {
                    if (isAnalyzingAdhd) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = BlushSurface, strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Analyzing Weekly Data with Gemini...")
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (snapshot.cachedAdhdInsight == null) {
                                "Analyze My Week with Gemini ♡"
                            } else {
                                "Refresh Today's Gemini Tips ♡"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (adhdInsightError != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = adhdInsightError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                val insight = snapshot.cachedAdhdInsight
                if (insight != null) {
                    Spacer(Modifier.height(10.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(BlushSurface)
                            .border(1.dp, RoseGoldBorder, RoundedCornerShape(14.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "♡ ${insight.dailyAffirmation}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = PinkPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "ADHD Focus Tip: ${insight.adhdFocusTip}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = BerryInk
                        )
                        Text(
                            text = "Schedule Boost (${insight.peakDistractionWindow} peak): ${insight.scheduleOptimizationTip}",
                            style = MaterialTheme.typography.bodySmall,
                            color = BerryInk.copy(alpha = 0.9f)
                        )
                        Spacer(Modifier.height(2.dp))
                        OutlinedButton(
                            onClick = {
                                onStartMicroSprint(insight.recommendedMicroSprintMinutes.toLong())
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("start_ai_micro_sprint_btn")
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = PinkPrimary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Start Recommended ${insight.recommendedMicroSprintMinutes}m Sprint ♡",
                                color = BerryInk,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Long-Term CSV Export Card
        CoquetteCard(modifier = Modifier.fillMaxWidth(), containerColor = BlushElevated) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Long-Term CSV Export",
                    style = MaterialTheme.typography.titleSmall,
                    color = BerryInk,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Export ${unifiedUsage.size} usage logs & ${snapshot.sessionHistoryLogs.size} restriction sessions for personal tracking.",
                    style = MaterialTheme.typography.bodySmall,
                    color = BerryInk.copy(alpha = 0.85f)
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            try {
                                createCsvLauncher.launch(CsvExportHelper.defaultCsvFileName())
                            } catch (_: Exception) {
                                CsvExportHelper.createShareIntent(
                                    context,
                                    csvPayload,
                                    CsvExportHelper.defaultCsvFileName()
                                ).onSuccess { intent ->
                                    context.startActivity(intent)
                                }.onFailure {
                                    Toast.makeText(context, "Could not export CSV", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PinkPrimary,
                            contentColor = BlushSurface
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("export_csv_save_btn")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Save CSV", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            CsvExportHelper.createShareIntent(
                                context,
                                csvPayload,
                                CsvExportHelper.defaultCsvFileName()
                            ).onSuccess { intent ->
                                try {
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                    onShowMessage("CSV prepared in app cache exports folder")
                                }
                            }.onFailure {
                                onShowMessage(it.localizedMessage ?: "Share CSV failed")
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("export_csv_share_btn")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Share CSV")
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CoquetteCard(modifier = Modifier.weight(1f), containerColor = PinkContainer) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Restricted", style = MaterialTheme.typography.labelLarge, color = BerryInk)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${totalRestrictedTime / 60}h ${totalRestrictedTime % 60}m",
                        style = MaterialTheme.typography.headlineSmall,
                        color = PinkPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            CoquetteCard(modifier = Modifier.weight(1f), containerColor = BlushSurface) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Active Screen", style = MaterialTheme.typography.labelLarge, color = BerryInk)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${totalScreenTime / 60}h ${totalScreenTime % 60}m",
                        style = MaterialTheme.typography.headlineSmall,
                        color = BerryInk,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            CoquetteCard(modifier = Modifier.weight(0.9f), containerColor = BlushElevated) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Sessions", style = MaterialTheme.typography.labelLarge, color = BerryInk)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${filteredSessionLogs.size}",
                        style = MaterialTheme.typography.headlineSmall,
                        color = PinkPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${completedSessions}✓ • ${earlyExitSessions}↺",
                        style = MaterialTheme.typography.labelSmall,
                        color = BerryInk
                    )
                }
            }
        }

        // Pattern Visualization Chart (Monthly 4-Week Trend when MONTHLY, or Weekly Mon-Sun Pattern)
        if (period == ReportPeriod.MONTHLY) {
            CoquetteCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "30-Day Monthly Pattern (Weeks 1–4)",
                        style = MaterialTheme.typography.titleMedium,
                        color = BerryInk,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                    ChartLegendRow()
                    Spacer(Modifier.height(10.dp))
                    val maxWeekVal = monthlyWeekBuckets
                        .maxOfOrNull { it.first + it.second }
                        ?.coerceAtLeast(1L)
                        ?.toFloat() ?: 1f

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .testTag("monthly_pattern_chart")
                    ) {
                        val count = 4
                        val slotW = size.width / count
                        val barW = slotW * 0.28f
                        monthlyWeekBuckets.forEachIndexed { idx, (restrictedMins, activeMins) ->
                            val center = idx * slotW + slotW / 2f
                            val resH = (size.height * (restrictedMins / maxWeekVal)).coerceAtLeast(
                                if (restrictedMins > 0) 10f else 4f
                            )
                            val actH = (size.height * (activeMins / maxWeekVal)).coerceAtLeast(
                                if (activeMins > 0) 10f else 4f
                            )
                            drawRoundRect(
                                color = if (restrictedMins > 0) PinkPrimary else PinkContainer,
                                topLeft = Offset(center - barW - 3f, size.height - resH),
                                size = Size(barW, resH),
                                cornerRadius = CornerRadius(10f, 10f)
                            )
                            drawRoundRect(
                                color = if (activeMins > 0) PinkAccent else PinkContainer.copy(alpha = 0.7f),
                                topLeft = Offset(center + 3f, size.height - actH),
                                size = Size(barW, actH),
                                cornerRadius = CornerRadius(10f, 10f)
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        listOf("Wk 1", "Wk 2", "Wk 3", "Wk 4 (Now)").forEach { label ->
                            Text(label, style = MaterialTheme.typography.labelSmall, color = BerryInk)
                        }
                    }
                }
            }
        }

        // Day-of-week Bar Chart (Restricted vs Active)
        CoquetteCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    if (period == ReportPeriod.MONTHLY) "Day-of-Week Pattern (30d)" else "Weekly Rhythm (Mon - Sun)",
                    style = MaterialTheme.typography.titleMedium,
                    color = BerryInk,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                ChartLegendRow()
                Spacer(Modifier.height(10.dp))
                val maxVal = weekdayStats.maxOfOrNull { maxOf(it.first, it.second) }?.coerceAtLeast(1L)?.toFloat() ?: 1f
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .testTag("weekly_rhythm_chart")
                ) {
                    val barCount = 7
                    val slotW = size.width / barCount
                    val barW = slotW * 0.28f
                    weekdayStats.forEachIndexed { idx, (restrictedMins, activeMins) ->
                        val center = idx * slotW + slotW / 2f
                        val resH = (size.height * (restrictedMins / maxVal)).coerceAtLeast(
                            if (restrictedMins > 0) 10f else 4f
                        )
                        val actH = (size.height * (activeMins / maxVal)).coerceAtLeast(
                            if (activeMins > 0) 10f else 4f
                        )
                        drawRoundRect(
                            color = if (restrictedMins > 0) PinkPrimary else PinkContainer,
                            topLeft = Offset(center - barW - 2f, size.height - resH),
                            size = Size(barW, resH),
                            cornerRadius = CornerRadius(10f, 10f)
                        )
                        drawRoundRect(
                            color = if (activeMins > 0) PinkAccent else PinkContainer.copy(alpha = 0.65f),
                            topLeft = Offset(center + 2f, size.height - actH),
                            size = Size(barW, actH),
                            cornerRadius = CornerRadius(10f, 10f)
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { day ->
                        Text(day, style = MaterialTheme.typography.labelSmall, color = BerryInk)
                    }
                }
            }
        }

        // Time of Day distribution
        CoquetteCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Time of Day Screen Activity", style = MaterialTheme.typography.titleMedium, color = BerryInk)
                Spacer(Modifier.height(8.dp))
                val labels = listOf("Night 00-05", "Morning 06-11", "Afternoon 12-17", "Evening 18-23")
                val maxH = (hourlyBuckets.maxOrNull() ?: 1L).coerceAtLeast(1L).toFloat()
                labels.forEachIndexed { idx, label ->
                    val mins = hourlyBuckets[idx]
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(label, style = MaterialTheme.typography.bodyMedium, color = BerryInk, modifier = Modifier.width(120.dp))
                        LinearProgressIndicator(
                            progress = { (mins / maxH).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .weight(1f)
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp)),
                            color = PinkAccent,
                            trackColor = PinkContainer
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("${mins}m", style = MaterialTheme.typography.labelSmall, color = BerryInk)
                    }
                }
            }
        }

        // Persisted Restriction Session History from Room
        CoquetteCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.History, contentDescription = null, tint = PinkPrimary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Restriction Session Logs",
                        style = MaterialTheme.typography.titleMedium,
                        color = BerryInk,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(8.dp))
                if (filteredSessionLogs.isEmpty()) {
                    Text(
                        text = "No restriction session logs saved for this window yet. Complete or unlock a schedule/session to log it locally.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = BerryInk
                    )
                } else {
                    filteredSessionLogs.take(10).forEach { log ->
                        val timeFormatted = remember(log.startedAtMillis) {
                            SimpleDateFormat("MMM d • HH:mm", Locale.US).format(Date(log.startedAtMillis))
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(PinkContainer.copy(alpha = 0.5f))
                                .border(1.dp, RoseGoldBorder, RoundedCornerShape(14.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (log.outcome == "COMPLETED") Icons.Default.CheckCircle else Icons.Default.TimerOff,
                                contentDescription = log.outcome,
                                tint = if (log.outcome == "COMPLETED") PinkPrimary else BerryInk,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${log.sessionTitle} (${log.durationMinutes}m)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = BerryInk
                                )
                                val exitSuffix = if (log.earlyExitMethod.isNotBlank()) " via ${log.earlyExitMethod}" else ""
                                Text(
                                    text = "$timeFormatted • ${log.outcome}$exitSuffix",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PinkPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Per-App Breakdown
        CoquetteCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Per-App Breakdown", style = MaterialTheme.typography.titleMedium, color = BerryInk)
                    val sampleApp = snapshot.installedApps.firstOrNull { !it.isSystemEssential }
                    if (sampleApp != null && appTotals.isNotEmpty()) {
                        OutlinedButton(
                            onClick = { onRecordSampleUsage(sampleApp, 25L, 35L) },
                            modifier = Modifier.height(48.dp).testTag("log_usage_entry_btn")
                        ) {
                            Icon(Icons.Default.AddChart, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Log Telemetry")
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                if (appTotals.isEmpty()) {
                    Text(
                        text = "No screen logs recorded for this period yet. Complete a focus session or grant Usage Access in Settings to populate Room history.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = BerryInk
                    )
                    Spacer(Modifier.height(10.dp))
                    val sampleApp = snapshot.installedApps.firstOrNull { !it.isSystemEssential }
                    if (sampleApp != null) {
                        Button(
                            onClick = { onRecordSampleUsage(sampleApp, 35L, 45L) },
                            colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary, contentColor = BlushSurface),
                            modifier = Modifier.height(48.dp).testTag("log_usage_entry_btn")
                        ) {
                            Text("Log Current Session Telemetry")
                        }
                    }
                } else {
                    val maxApp = appTotals.maxOf { it.second + it.third }.coerceAtLeast(1L).toFloat()
                    appTotals.forEach { (appName, screenMins, restrictedMins) ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(PinkContainer.copy(alpha = 0.45f))
                                .border(1.dp, RoseGoldBorder, RoundedCornerShape(14.dp))
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(appName, fontWeight = FontWeight.Bold, color = BerryInk)
                                Text("${screenMins}m active • ${restrictedMins}m restricted", style = MaterialTheme.typography.labelSmall, color = PinkPrimary)
                            }
                            Spacer(Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { ((screenMins + restrictedMins) / maxApp).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = PinkPrimary,
                                trackColor = BlushSurface
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun ChartLegendRow() {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(PinkPrimary))
            Spacer(Modifier.width(5.dp))
            Text("Restricted", style = MaterialTheme.typography.labelSmall, color = BerryInk)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(PinkAccent))
            Spacer(Modifier.width(5.dp))
            Text("Active Screen", style = MaterialTheme.typography.labelSmall, color = BerryInk)
        }
    }
}
