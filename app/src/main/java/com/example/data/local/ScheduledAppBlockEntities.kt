package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Locale

@Entity(
    tableName = "scheduled_app_block_tasks",
    indices = [
        Index(value = ["packageName"]),
        Index(value = ["enabled", "startMinutes"])
    ]
)
data class ScheduledAppBlockTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val packageName: String,
    val appName: String,
    val taskTitle: String = "",
    /** Start time in minutes since midnight (0..1439). */
    val startMinutes: Int,
    /** End time in minutes since midnight (0..1439). Supports overnight ranges when startMinutes > endMinutes. */
    val endMinutes: Int,
    /** 7-bit day-of-week mask where bit 0 = Monday (1) .. bit 6 = Sunday (7). */
    val daysOfWeekMask: Int = ALL_DAYS_MASK,
    val enabled: Boolean = true,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
) {
    val startHour: Int get() = startMinutes.coerceIn(0, MAX_MINUTE_OF_DAY) / 60
    val startMinute: Int get() = startMinutes.coerceIn(0, MAX_MINUTE_OF_DAY) % 60
    val endHour: Int get() = endMinutes.coerceIn(0, MAX_MINUTE_OF_DAY) / 60
    val endMinute: Int get() = endMinutes.coerceIn(0, MAX_MINUTE_OF_DAY) % 60

    fun formattedWindow(): String = "${formatMinutes(startMinutes)} - ${formatMinutes(endMinutes)}"

    fun isDayActive(dayOfWeek: Int): Boolean {
        if (dayOfWeek !in 1..7) return false
        val bit = 1 shl (dayOfWeek - 1)
        return (daysOfWeekMask and bit) != 0
    }

    fun isActiveAt(minutesOfDay: Int, dayOfWeek: Int): Boolean {
        if (!enabled) return false
        if (!isDayActive(dayOfWeek)) return false
        val current = minutesOfDay.coerceIn(0, MAX_MINUTE_OF_DAY)
        val start = startMinutes.coerceIn(0, MAX_MINUTE_OF_DAY)
        val end = endMinutes.coerceIn(0, MAX_MINUTE_OF_DAY)
        return when {
            start == end -> true // 24-hour block window
            start < end -> current in start until end
            else -> current >= start || current < end // Overnight window (e.g. 22:00 - 07:00)
        }
    }

    companion object {
        const val MAX_MINUTE_OF_DAY = 1439
        const val ALL_DAYS_MASK = 0b1111111

        fun toMinutesOfDay(hour: Int, minute: Int): Int {
            val safeHour = hour.coerceIn(0, 23)
            val safeMinute = minute.coerceIn(0, 59)
            return safeHour * 60 + safeMinute
        }

        fun formatMinutes(totalMinutes: Int): String {
            val clamped = totalMinutes.coerceIn(0, MAX_MINUTE_OF_DAY)
            return String.format(Locale.US, "%02d:%02d", clamped / 60, clamped % 60)
        }

        fun daysMaskFromList(daysOfWeek: List<Int>): Int {
            if (daysOfWeek.isEmpty()) return ALL_DAYS_MASK
            var mask = 0
            for (day in daysOfWeek) {
                if (day in 1..7) {
                    mask = mask or (1 shl (day - 1))
                }
            }
            return if (mask == 0) ALL_DAYS_MASK else mask
        }
    }
}
