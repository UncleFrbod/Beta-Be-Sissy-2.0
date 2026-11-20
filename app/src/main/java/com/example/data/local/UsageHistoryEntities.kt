package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "historical_app_usage",
    indices = [
        Index(value = ["userId", "timestampMillis"]),
        Index(value = ["userId", "dateKey"])
    ]
)
data class HistoricalAppUsageEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val packageName: String,
    val appName: String,
    val dateKey: String,        // yyyy-MM-dd
    val timestampMillis: Long,  // Start of hour / record epoch millis
    val dayOfWeek: Long,        // 1=Mon..7=Sun
    val hourOfDay: Long,        // 0..23
    val screenTimeMinutes: Long,
    val restrictedMinutes: Long,
    val source: String = "SYSTEM" // SYSTEM, SESSION, MANUAL
)

@Entity(
    tableName = "restriction_session_logs",
    indices = [
        Index(value = ["userId", "startedAtMillis"])
    ]
)
data class RestrictionSessionLogEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val sessionTitle: String,
    val mode: String,           // SESSION, SCHEDULE, DAILY_LIMIT
    val startedAtMillis: Long,
    val endedAtMillis: Long,
    val durationMinutes: Long,
    val outcome: String,        // COMPLETED, ENDED_EARLY, PAUSED, ACTIVE
    val earlyExitMethod: String,// "", AI_VISUAL_PROOF, KEYS, PLAPS, SEQUENTIAL_TASKS
    val dateKey: String,        // yyyy-MM-dd
    val dayOfWeek: Long         // 1=Mon..7=Sun
)
