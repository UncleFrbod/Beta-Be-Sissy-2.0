package com.example.data

import com.example.data.local.HistoricalAppUsageEntity
import com.example.data.local.RestrictionSessionLogEntity
import com.example.data.local.UsageHistoryDao
import com.example.model.RestrictionSession
import com.example.model.UsageLogEntry
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class UsageHistoryRepository(private val dao: UsageHistoryDao) {

    fun observeAllUsage(userId: String): Flow<List<HistoricalAppUsageEntity>> =
        dao.observeAllUsage(userId)

    fun observeUsageSince(userId: String, daysBack: Int): Flow<List<HistoricalAppUsageEntity>> {
        val minTs = System.currentTimeMillis() - daysBack.coerceAtLeast(1) * DAY_MILLIS
        return dao.observeUsageSince(userId, minTs)
    }

    fun observeAllSessionLogs(userId: String): Flow<List<RestrictionSessionLogEntity>> =
        dao.observeAllSessionLogs(userId)

    fun observeSessionLogsSince(userId: String, daysBack: Int): Flow<List<RestrictionSessionLogEntity>> {
        val minTs = System.currentTimeMillis() - daysBack.coerceAtLeast(1) * DAY_MILLIS
        return dao.observeSessionLogsSince(userId, minTs)
    }

    suspend fun syncUsageLogs(
        userId: String,
        entries: List<UsageLogEntry>,
        source: String = "SYNC"
    ) {
        if (entries.isEmpty()) return
        val entities = entries.map { entry ->
            val ts = parseDateKeyAndHourMillis(entry.dateKey, entry.hourOfDay)
            HistoricalAppUsageEntity(
                id = "${userId}_${entry.dateKey}_${entry.packageName}_${entry.hourOfDay}",
                userId = userId,
                packageName = entry.packageName,
                appName = entry.appName,
                dateKey = entry.dateKey,
                timestampMillis = ts,
                dayOfWeek = entry.dayOfWeek.coerceIn(1L, 7L),
                hourOfDay = entry.hourOfDay.coerceIn(0L, 23L),
                screenTimeMinutes = entry.screenTimeMinutes.coerceAtLeast(0L),
                restrictedMinutes = entry.restrictedMinutes.coerceAtLeast(0L),
                source = source
            )
        }
        dao.upsertUsageEntries(entities)
    }

    suspend fun syncSessionsFromRemote(
        userId: String,
        sessions: List<RestrictionSession>
    ) {
        if (sessions.isEmpty()) return
        val logs = sessions.mapNotNull { session ->
            if (session.id.isBlank() && session.startedAtMillis <= 0L) return@mapNotNull null
            val started = if (session.startedAtMillis > 0L) session.startedAtMillis else System.currentTimeMillis()
            val ended = when {
                session.status == "COMPLETED" -> session.endsAtMillis.coerceAtLeast(started)
                session.status == "ENDED_EARLY" -> session.updatedAt?.toDate()?.time ?: System.currentTimeMillis()
                else -> session.endsAtMillis.coerceAtLeast(started)
            }
            val cal = Calendar.getInstance().apply { timeInMillis = started }
            val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(started))
            val dayOfWeek = (((cal.get(Calendar.DAY_OF_WEEK) + 5) % 7) + 1).toLong()
            val title = when (session.mode) {
                "SCHEDULE" -> "Schedule Plan"
                "DAILY_LIMIT" -> "Daily Cap"
                else -> "Focus Restriction"
            }
            RestrictionSessionLogEntity(
                id = session.id.ifBlank { "sess_$started" },
                userId = userId,
                sessionTitle = title,
                mode = session.mode.ifBlank { "SESSION" },
                startedAtMillis = started,
                endedAtMillis = ended,
                durationMinutes = session.durationMinutes.coerceAtLeast(1L),
                outcome = session.status.ifBlank { "ACTIVE" },
                earlyExitMethod = session.earlyExitMethod,
                dateKey = dateKey,
                dayOfWeek = dayOfWeek
            )
        }
        if (logs.isNotEmpty()) {
            dao.upsertSessionLogs(logs)
        }
    }

    suspend fun recordUsageSnapshot(
        userId: String,
        packageName: String,
        appName: String,
        screenTimeMinutes: Long,
        restrictedMinutes: Long,
        timestampMillis: Long = System.currentTimeMillis(),
        source: String = "MANUAL"
    ) {
        val cal = Calendar.getInstance().apply { timeInMillis = timestampMillis }
        val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(timestampMillis))
        val dayOfWeek = (((cal.get(Calendar.DAY_OF_WEEK) + 5) % 7) + 1).toLong()
        val hour = cal.get(Calendar.HOUR_OF_DAY).toLong()
        dao.upsertUsageEntry(
            HistoricalAppUsageEntity(
                id = "${userId}_${dateKey}_${packageName}_$hour",
                userId = userId,
                packageName = packageName,
                appName = appName,
                dateKey = dateKey,
                timestampMillis = timestampMillis,
                dayOfWeek = dayOfWeek,
                hourOfDay = hour,
                screenTimeMinutes = screenTimeMinutes.coerceAtLeast(0L),
                restrictedMinutes = restrictedMinutes.coerceAtLeast(0L),
                source = source
            )
        )
    }

    suspend fun recordRestrictionSessionLog(
        id: String,
        userId: String,
        sessionTitle: String,
        mode: String,
        startedAtMillis: Long,
        endedAtMillis: Long,
        durationMinutes: Long,
        outcome: String,
        earlyExitMethod: String = ""
    ) {
        val safeStart = if (startedAtMillis > 0L) startedAtMillis else System.currentTimeMillis()
        val safeEnd = endedAtMillis.coerceAtLeast(safeStart)
        val cal = Calendar.getInstance().apply { timeInMillis = safeStart }
        val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(safeStart))
        val dayOfWeek = (((cal.get(Calendar.DAY_OF_WEEK) + 5) % 7) + 1).toLong()
        dao.upsertSessionLog(
            RestrictionSessionLogEntity(
                id = id,
                userId = userId,
                sessionTitle = sessionTitle,
                mode = mode,
                startedAtMillis = safeStart,
                endedAtMillis = safeEnd,
                durationMinutes = durationMinutes.coerceAtLeast(1L),
                outcome = outcome,
                earlyExitMethod = earlyExitMethod,
                dateKey = dateKey,
                dayOfWeek = dayOfWeek
            )
        )
    }

    companion object {
        private const val DAY_MILLIS = 86_400_000L

        fun parseDateKeyAndHourMillis(dateKey: String, hourOfDay: Long): Long {
            return try {
                val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateKey)
                val cal = Calendar.getInstance()
                if (parsed != null) {
                    cal.time = parsed
                }
                cal.set(Calendar.HOUR_OF_DAY, hourOfDay.toInt().coerceIn(0, 23))
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            } catch (_: Exception) {
                System.currentTimeMillis() - abs(hourOfDay) * 3_600_000L
            }
        }
    }
}
