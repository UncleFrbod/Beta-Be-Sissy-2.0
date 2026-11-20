package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data object Empty : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class RestrictionMode { SESSION, SCHEDULE, DAILY_LIMIT }
enum class SessionStatus { ACTIVE, PAUSED, COMPLETED, ENDED_EARLY }
enum class MediaType { NONE, ICON, PHOTO, GIF, VIDEO }
enum class CustomTaskType { READ_LINE, TYPING_PROMPT }
enum class SuggestionStatus { PENDING, APPLIED, DISMISSED }
enum class ReportPeriod { DAILY, WEEKLY, MONTHLY }

inline fun <reified T : Enum<T>> safeEnumValueOf(raw: String?, default: T): T {
    return enumValues<T>().firstOrNull { it.name == raw } ?: default
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Princess",
    val keysBalance: Long = 2L,
    val plapsBalance: Long = 10L,
    val dailyStreak: Long = 0L,
    val lastSessionDate: String = "",
    val lastBurnMonth: String = "",
    val squatCountTarget: Long = 20L,
    val selectedFontKey: String = "playfair",
    val customFontUri: String = "",
    val selectedFontColorHex: String = "#B8255D",
    val wallpaperKey: String = "coquette_silk",
    val customWallpaperUri: String = "",
    val aodMediaUri: String = "",
    val aodMediaType: String = "NONE",
    val aodFontKey: String = "pacifico",
    val aodFontColorHex: String = "#B8255D",
    val aodDimPercent: Long = 35L,
    val wallBlockReason: String = "Stay focused, gorgeous! This app is taking a cute break right now.",
    val wallBlockMediaUri: String = "",
    val wallBlockMediaType: String = "ICON",
    val wallBlockFontKey: String = "playfair",
    val wallBlockFontColorHex: String = "#B8255D",
    val wallBlockBgHex: String = "#FFE3EE",
    val visualProofDescription: String = "Show a tidy desk, water glass, or pink ribbon to verify focus completion",
    val visualProofSampleUri: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AppRule(
    @DocumentId val id: String = "",
    val userId: String = "",
    val packageName: String = "",
    val appName: String = "",
    val category: String = "App",
    val necessaryApp: Boolean = false,
    val restrictedInSession: Boolean = false,
    val scheduleEnabled: Boolean = false,
    val scheduleStartMinutes: Long = 1320L, // 22:00
    val scheduleEndMinutes: Long = 0L,      // 00:00
    val dailyLimitEnabled: Boolean = false,
    val dailyLimitMinutes: Long = 60L,
    val usedTodayMinutes: Long = 0L,
    val tempUnblockUntilMillis: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class RestrictionSession(
    @DocumentId val id: String = "",
    val userId: String = "",
    val mode: String = "SESSION",
    val durationMinutes: Long = 60L,
    val startedAtMillis: Long = 0L,
    val endsAtMillis: Long = 0L,
    val pausedUntilMillis: Long = 0L,
    val status: String = "ACTIVE",
    val earlyExitMethod: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class UsageLogEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val packageName: String = "",
    val appName: String = "",
    val dateKey: String = "",
    val dayOfWeek: Long = 1L,
    val hourOfDay: Long = 12L,
    val screenTimeMinutes: Long = 0L,
    val restrictedMinutes: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CustomExitTask(
    @DocumentId val id: String = "",
    val userId: String = "",
    val taskType: String = "READ_LINE",
    val content: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AiScheduleSuggestion(
    @DocumentId val id: String = "",
    val userId: String = "",
    val appName: String = "",
    val packageName: String = "",
    val suggestedStartMinutes: Long = 1320L,
    val suggestedEndMinutes: Long = 60L,
    val suggestedDailyLimitMinutes: Long = 45L,
    val reason: String = "",
    val status: String = "PENDING",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SchedulePlan(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "Evening Focus",
    val description: String = "",
    val enabled: Boolean = true,
    val startMinutes: Long = 1320L, // 22:00
    val endMinutes: Long = 0L,      // 00:00
    val daysOfWeek: List<Long> = listOf(1L, 2L, 3L, 4L, 5L, 6L, 7L),
    val modeType: String = "BLOCK_SELECTED", // "BLOCK_SELECTED" or "ALLOW_ONLY_SELECTED"
    val allowedPackages: List<String> = emptyList(),
    val restrictedPackages: List<String> = emptyList(),
    val customBlockReason: String = "",
    val unlockedUntilMillis: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class InstalledAppItem(
    val packageName: String,
    val appName: String,
    val category: String,
    val isSystemEssential: Boolean = false
)

data class ChatMessage(
    val id: String,
    val role: String, // "user" or "model"
    val text: String,
    val modelUsed: String = "",
    val groundingSources: List<String> = emptyList()
)
