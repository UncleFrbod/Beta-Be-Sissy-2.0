package com.example.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.local.HistoricalAppUsageEntity
import com.example.data.local.RestrictionSessionLogEntity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExportHelper {

    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)

    fun buildUsageAndSessionCsv(
        usageLogs: List<HistoricalAppUsageEntity>,
        sessionLogs: List<RestrictionSessionLogEntity>
    ): String {
        val sb = StringBuilder()
        sb.appendLine(
            "record_type,date,day_of_week,time_info,name,package_or_mode," +
                "active_or_duration_minutes,restricted_minutes,status_or_source,early_exit_method"
        )

        usageLogs.sortedByDescending { it.timestampMillis }.forEach { entry ->
            val hourText = String.format(Locale.US, "%02d:00", entry.hourOfDay.toInt().coerceIn(0, 23))
            sb.appendLine(
                listOf(
                    "APP_USAGE",
                    entry.dateKey,
                    dayLabel(entry.dayOfWeek),
                    hourText,
                    entry.appName,
                    entry.packageName,
                    entry.screenTimeMinutes.toString(),
                    entry.restrictedMinutes.toString(),
                    entry.source,
                    ""
                ).joinToString(",") { escapeCsv(it) }
            )
        }

        sessionLogs.sortedByDescending { it.startedAtMillis }.forEach { log ->
            val startIso = isoFormat.format(Date(log.startedAtMillis))
            val endIso = isoFormat.format(Date(log.endedAtMillis))
            sb.appendLine(
                listOf(
                    "SESSION_HISTORY",
                    log.dateKey,
                    dayLabel(log.dayOfWeek),
                    "$startIso -> $endIso",
                    log.sessionTitle,
                    log.mode,
                    log.durationMinutes.toString(),
                    log.durationMinutes.toString(),
                    log.outcome,
                    log.earlyExitMethod
                ).joinToString(",") { escapeCsv(it) }
            )
        }

        return sb.toString()
    }

    fun writeCsvToUri(context: Context, uri: Uri, csvContent: String): Result<Unit> {
        return try {
            context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
                writer.write(csvContent)
            } ?: return Result.failure(IllegalStateException("Could not open destination file stream"))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun createShareIntent(context: Context, csvContent: String, suggestedFileName: String): Result<Intent> {
        return try {
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val outFile = File(exportDir, suggestedFileName)
            outFile.writeText(csvContent, Charsets.UTF_8)
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                outFile
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, "Be Sissy Screen Time & Session History Report")
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            Result.success(Intent.createChooser(shareIntent, "Export Screen Time CSV"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun defaultCsvFileName(nowMillis: Long = System.currentTimeMillis()): String {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date(nowMillis))
        return "besissy_screen_time_$stamp.csv"
    }

    internal fun escapeCsv(raw: String): String {
        val needsQuotes = raw.contains(',') || raw.contains('"') || raw.contains('\n') || raw.contains('\r')
        if (!needsQuotes) return raw
        return "\"${raw.replace("\"", "\"\"")}\""
    }

    private fun dayLabel(dayOfWeek: Long): String = when (dayOfWeek.toInt()) {
        1 -> "Mon"
        2 -> "Tue"
        3 -> "Wed"
        4 -> "Thu"
        5 -> "Fri"
        6 -> "Sat"
        7 -> "Sun"
        else -> "Day$dayOfWeek"
    }
}
