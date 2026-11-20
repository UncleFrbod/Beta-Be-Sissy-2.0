package com.example.data

import android.content.Context
import com.example.R
import com.example.firebase.OperationType
import com.example.firebase.handleFirestoreError
import com.example.model.AiScheduleSuggestion
import com.example.model.AppRule
import com.example.model.ChallengeLeaderboardEntry
import com.example.model.CustomExitTask
import com.example.model.RestrictionSession
import com.example.model.SchedulePlan
import com.example.model.StreakRewardCalculator
import com.example.model.StreakRewardOutcome
import com.example.model.UsageLogEntry
import com.example.model.UserProfile
import com.example.model.WeeklyChallengeCatalog
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class BeSissyRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun sanitizeId(raw: String): String {
        val cleaned = raw.replace(Regex("[^a-zA-Z0-9_.\\-]"), "_").take(120)
        return cleaned.ifBlank { "id_${UUID.randomUUID().toString().take(8)}" }
    }

    private fun currentMonthKey(): String =
        SimpleDateFormat("yyyy-MM", Locale.US).format(Date())

    private fun currentDateKey(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    fun observeProfile(userId: String): Flow<UserProfile?> =
        db.collection("users").document(userId)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(
                        UserProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                } else null
            }

    fun observeAppRules(userId: String): Flow<List<AppRule>> =
        db.collection("users").document(userId).collection("app_rules")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    AppRule::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }

    fun observeSessions(userId: String): Flow<List<RestrictionSession>> =
        db.collection("users").document(userId).collection("sessions")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    RestrictionSession::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.startedAtMillis }
            }

    fun observeUsageLogs(userId: String): Flow<List<UsageLogEntry>> =
        db.collection("users").document(userId).collection("usage_logs")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    UsageLogEntry::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }

    fun observeCustomTasks(userId: String): Flow<List<CustomExitTask>> =
        db.collection("users").document(userId).collection("custom_tasks")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    CustomExitTask::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }

    fun observeAiSuggestions(userId: String): Flow<List<AiScheduleSuggestion>> =
        db.collection("users").document(userId).collection("ai_suggestions")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    AiScheduleSuggestion::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }

    fun observeSchedules(userId: String): Flow<List<SchedulePlan>> =
        db.collection("users").document(userId).collection("schedules")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    SchedulePlan::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedBy { it.startMinutes }
            }

    fun observeWeeklyLeaderboard(weekKey: String): Flow<List<ChallengeLeaderboardEntry>> =
        db.collection("challenge_leaderboard")
            .whereEqualTo("weekKey", weekKey)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    ChallengeLeaderboardEntry::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedWith(
                    compareByDescending<ChallengeLeaderboardEntry> { it.totalScore }
                        .thenByDescending { it.restrictedMinutes }
                        .thenByDescending { it.tasksCompleted }
                )
            }

    suspend fun ensureInitialized(userId: String, displayName: String): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            val userDoc = db.collection("users").document(uid)
            val snap = userDoc.get().await()
            val thisMonth = currentMonthKey()
            val safeName = displayName.trim().ifBlank { "Princess" }.take(100)

            if (!snap.exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "keysBalance" to 2L,
                    "plapsBalance" to 10L,
                    "dailyStreak" to 0L,
                    "lastSessionDate" to "",
                    "lastBurnMonth" to thisMonth,
                    "squatCountTarget" to 20L,
                    "selectedFontKey" to "playfair",
                    "customFontUri" to "",
                    "selectedFontColorHex" to "#B8255D",
                    "wallpaperKey" to "coquette_silk",
                    "customWallpaperUri" to "",
                    "aodMediaUri" to "",
                    "aodMediaType" to "NONE",
                    "aodFontKey" to "pacifico",
                    "aodFontColorHex" to "#B8255D",
                    "aodDimPercent" to 35L,
                    "wallBlockReason" to "Stay focused, gorgeous! This app is taking a cute break right now.",
                    "wallBlockMediaUri" to "",
                    "wallBlockMediaType" to "ICON",
                    "wallBlockFontKey" to "playfair",
                    "wallBlockFontColorHex" to "#B8255D",
                    "wallBlockBgHex" to "#FFE3EE",
                    "visualProofDescription" to "Show a tidy desk, water glass, or pink ribbon to verify focus completion",
                    "visualProofSampleUri" to "",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                userDoc.set(payload).await()
                seedDefaultCustomTasks(uid)
                seedDefaultSchedulePlans(uid)
            } else {
                // Monthly burn check: all Keys and Plaps burn at end of month
                val lastBurn = snap.getString("lastBurnMonth").orEmpty()
                if (lastBurn.isNotBlank() && lastBurn != thisMonth) {
                    userDoc.update(
                        mapOf(
                            "keysBalance" to 0L,
                            "plapsBalance" to 0L,
                            "lastBurnMonth" to thisMonth,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    private suspend fun seedDefaultCustomTasks(uid: String) {
        val defaults = listOf(
            "READ_LINE" to "I am calm, radiant, poised, and completely in control of my time.",
            "READ_LINE" to "Every mindful minute away from distractions makes me glow brighter.",
            "TYPING_PROMPT" to "Grace, poise, and discipline! I choose calm focus over mindless scrolling today.",
            "TYPING_PROMPT" to "Wait, breathe, smile: 100% present, 0% rushed, and sparkling with poise."
        )
        defaults.forEachIndexed { index, (type, text) ->
            val taskRef = db.collection("users").document(uid)
                .collection("custom_tasks").document("default_task_$index")
            taskRef.set(
                mapOf(
                    "userId" to uid,
                    "taskType" to type,
                    "content" to text,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
    }

    private suspend fun seedDefaultSchedulePlans(uid: String) {
        val defaultRef = db.collection("users").document(uid)
            .collection("schedules").document("sched_night_winddown")
        defaultRef.set(
            mapOf(
                "userId" to uid,
                "name" to "Night Wind-Down",
                "description" to "Quiet bedtime schedule to restrict games & late-night scrolling ♡",
                "enabled" to false,
                "startMinutes" to 1320L, // 22:00
                "endMinutes" to 0L,      // 00:00
                "daysOfWeek" to listOf(1L, 2L, 3L, 4L, 5L, 6L, 7L),
                "modeType" to "BLOCK_SELECTED",
                "allowedPackages" to listOf("com.android.dialer", "com.google.android.apps.messaging"),
                "restrictedPackages" to listOf("com.mobile.legends", "com.zhiliaoapp.musically"),
                "customBlockReason" to "Night Wind-Down is active. Rest and glow for tomorrow ♡",
                "unlockedUntilMillis" to 0L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun updateProfilePreferences(userId: String, updates: Map<String, Any>): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val safeUpdates = updates.toMutableMap().apply {
                put("updatedAt", FieldValue.serverTimestamp())
            }
            db.collection("users").document(uid).update(safeUpdates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun upsertAppRule(userId: String, rule: AppRule): Result<Unit> {
        val ruleId = sanitizeId(rule.packageName)
        val docPath = "users/$userId/app_rules/$ruleId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val docRef = db.collection("users").document(uid).collection("app_rules").document(ruleId)
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "appName" to rule.appName.take(100),
                        "category" to rule.category.take(60),
                        "necessaryApp" to rule.necessaryApp,
                        "restrictedInSession" to rule.restrictedInSession,
                        "scheduleEnabled" to rule.scheduleEnabled,
                        "scheduleStartMinutes" to rule.scheduleStartMinutes.coerceIn(0L, 1439L),
                        "scheduleEndMinutes" to rule.scheduleEndMinutes.coerceIn(0L, 1439L),
                        "dailyLimitEnabled" to rule.dailyLimitEnabled,
                        "dailyLimitMinutes" to rule.dailyLimitMinutes.coerceIn(1L, 1440L),
                        "usedTodayMinutes" to rule.usedTodayMinutes.coerceIn(0L, 1440L),
                        "tempUnblockUntilMillis" to rule.tempUnblockUntilMillis.coerceAtLeast(0L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "packageName" to rule.packageName.take(200),
                        "appName" to rule.appName.take(100),
                        "category" to rule.category.take(60),
                        "necessaryApp" to rule.necessaryApp,
                        "restrictedInSession" to rule.restrictedInSession,
                        "scheduleEnabled" to rule.scheduleEnabled,
                        "scheduleStartMinutes" to rule.scheduleStartMinutes.coerceIn(0L, 1439L),
                        "scheduleEndMinutes" to rule.scheduleEndMinutes.coerceIn(0L, 1439L),
                        "dailyLimitEnabled" to rule.dailyLimitEnabled,
                        "dailyLimitMinutes" to rule.dailyLimitMinutes.coerceIn(1L, 1440L),
                        "usedTodayMinutes" to rule.usedTodayMinutes.coerceIn(0L, 1440L),
                        "tempUnblockUntilMillis" to rule.tempUnblockUntilMillis.coerceAtLeast(0L),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun startSession(userId: String, mode: String, durationMinutes: Long): Result<String> {
        val sessionId = "sess_${System.currentTimeMillis()}"
        val docPath = "users/$userId/sessions/$sessionId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val clampedDuration = durationMinutes.coerceIn(1L, 1440L)
            val now = System.currentTimeMillis()
            val endsAt = now + clampedDuration * 60_000L
            db.collection("users").document(uid).collection("sessions").document(sessionId)
                .set(
                    mapOf(
                        "userId" to uid,
                        "mode" to mode,
                        "durationMinutes" to clampedDuration,
                        "startedAtMillis" to now,
                        "endsAtMillis" to endsAt,
                        "pausedUntilMillis" to 0L,
                        "status" to "ACTIVE",
                        "earlyExitMethod" to "",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(sessionId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateActiveSession(
        userId: String,
        sessionId: String,
        updates: Map<String, Any>
    ): Result<Unit> {
        val docPath = "users/$userId/sessions/$sessionId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val safeUpdates = updates.toMutableMap().apply {
                put("updatedAt", FieldValue.serverTimestamp())
            }
            db.collection("users").document(uid).collection("sessions").document(sessionId)
                .update(safeUpdates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun awardDailySessionCompletion(
        userId: String,
        currentProfile: UserProfile,
        restrictedMinutesEarned: Long,
        packageName: String = "com.aistudio.besissy.qkvnxm",
        appName: String = "Focus Session"
    ): Result<Pair<Long, Long>> {
        val docPath = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val today = currentDateKey()
            val alreadyCountedToday = currentProfile.lastSessionDate == today
            val newStreak = if (alreadyCountedToday) currentProfile.dailyStreak else currentProfile.dailyStreak + 1L

            // Daily reward: +1 Key, +5 Plaps; Weekly streak (every 7 days): +10 Keys, +50 Plaps
            var addedKeys = 1L
            var addedPlaps = 5L
            if (!alreadyCountedToday && newStreak > 0L && newStreak % 7L == 0L) {
                addedKeys += 10L
                addedPlaps += 50L
            }

            db.collection("users").document(uid).update(
                mapOf(
                    "keysBalance" to (currentProfile.keysBalance + addedKeys).coerceAtMost(100000L),
                    "plapsBalance" to (currentProfile.plapsBalance + addedPlaps).coerceAtMost(100000L),
                    "dailyStreak" to newStreak,
                    "lastSessionDate" to today,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            recordUsageLog(
                userId = uid,
                packageName = packageName,
                appName = appName,
                screenTimeMinutes = 0L,
                restrictedMinutes = restrictedMinutesEarned.coerceIn(1L, 1440L)
            )
            Result.success(addedKeys to addedPlaps)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun claimDailyGoalStreak(
        userId: String,
        currentProfile: UserProfile,
        rules: List<AppRule>
    ): Result<StreakRewardOutcome> {
        val docPath = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val goalEval = StreakRewardCalculator.evaluateDailyGoals(rules)
            if (!goalEval.allLimitsMet) {
                return Result.failure(
                    IllegalStateException("Exceeding limit on: ${goalEval.exceededApps.joinToString(", ")}")
                )
            }
            val now = System.currentTimeMillis()
            val today = StreakRewardCalculator.formatDateKey(now)
            val yesterday = StreakRewardCalculator.yesterdayDateKey(now)
            val outcome = StreakRewardCalculator.computeReward(
                currentStreak = currentProfile.dailyStreak,
                lastSessionDate = currentProfile.lastSessionDate,
                todayDateKey = today,
                yesterdayDateKey = yesterday
            )
            if (outcome.alreadyClaimedToday) {
                return Result.success(outcome)
            }

            db.collection("users").document(uid).update(
                mapOf(
                    "keysBalance" to (currentProfile.keysBalance + outcome.totalKeysAwarded).coerceAtMost(100000L),
                    "plapsBalance" to (currentProfile.plapsBalance + outcome.plapsAwarded).coerceAtMost(100000L),
                    "dailyStreak" to outcome.newStreak.coerceIn(0L, 10000L),
                    "lastSessionDate" to today,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            Result.success(outcome)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun recordUsageLog(
        userId: String,
        packageName: String,
        appName: String,
        screenTimeMinutes: Long,
        restrictedMinutes: Long
    ): Result<Unit> {
        val logId = "log_${System.currentTimeMillis()}_${sanitizeId(appName).take(20)}"
        val docPath = "users/$userId/usage_logs/$logId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val cal = java.util.Calendar.getInstance()
            val dayOfWeek = (((cal.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7) + 1).toLong() // 1=Mon..7=Sun
            val hour = cal.get(java.util.Calendar.HOUR_OF_DAY).toLong()
            db.collection("users").document(uid).collection("usage_logs").document(logId)
                .set(
                    mapOf(
                        "userId" to uid,
                        "packageName" to packageName.take(200),
                        "appName" to appName.take(100),
                        "dateKey" to currentDateKey(),
                        "dayOfWeek" to dayOfWeek,
                        "hourOfDay" to hour,
                        "screenTimeMinutes" to screenTimeMinutes.coerceIn(0L, 1440L),
                        "restrictedMinutes" to restrictedMinutes.coerceIn(0L, 1440L),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun addCustomTask(userId: String, taskType: String, content: String): Result<Unit> {
        val taskId = "task_${System.currentTimeMillis()}"
        val docPath = "users/$userId/custom_tasks/$taskId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val trimmed = content.trim().take(2000)
            require(trimmed.isNotEmpty()) { "Task content cannot be empty" }
            db.collection("users").document(uid).collection("custom_tasks").document(taskId)
                .set(
                    mapOf(
                        "userId" to uid,
                        "taskType" to taskType,
                        "content" to trimmed,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteCustomTask(userId: String, taskId: String): Result<Unit> {
        val docPath = "users/$userId/custom_tasks/$taskId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            db.collection("users").document(uid).collection("custom_tasks").document(taskId)
                .delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun saveAiSuggestions(userId: String, suggestions: List<AiScheduleSuggestion>): Result<Unit> {
        val basePath = "users/$userId/ai_suggestions"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            suggestions.forEachIndexed { index, sug ->
                val sugId = "sug_${System.currentTimeMillis()}_$index"
                db.collection("users").document(uid).collection("ai_suggestions").document(sugId)
                    .set(
                        mapOf(
                            "userId" to uid,
                            "appName" to sug.appName.take(100),
                            "packageName" to sug.packageName.take(200),
                            "suggestedStartMinutes" to sug.suggestedStartMinutes.coerceIn(0L, 1439L),
                            "suggestedEndMinutes" to sug.suggestedEndMinutes.coerceIn(0L, 1439L),
                            "suggestedDailyLimitMinutes" to sug.suggestedDailyLimitMinutes.coerceIn(5L, 1440L),
                            "reason" to sug.reason.take(500),
                            "status" to "PENDING",
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, basePath)
            Result.failure(e)
        }
    }

    suspend fun updateAiSuggestion(
        userId: String,
        suggestionId: String,
        startMinutes: Long,
        endMinutes: Long,
        dailyLimitMinutes: Long,
        status: String
    ): Result<Unit> {
        val docPath = "users/$userId/ai_suggestions/$suggestionId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            db.collection("users").document(uid).collection("ai_suggestions").document(suggestionId)
                .update(
                    mapOf(
                        "suggestedStartMinutes" to startMinutes.coerceIn(0L, 1439L),
                        "suggestedEndMinutes" to endMinutes.coerceIn(0L, 1439L),
                        "suggestedDailyLimitMinutes" to dailyLimitMinutes.coerceIn(5L, 1440L),
                        "status" to status,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun upsertSchedulePlan(userId: String, plan: SchedulePlan): Result<String> {
        val schedId = if (plan.id.isNotBlank()) sanitizeId(plan.id) else "sched_${System.currentTimeMillis()}"
        val docPath = "users/$userId/schedules/$schedId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val docRef = db.collection("users").document(uid).collection("schedules").document(schedId)
            val cleanName = plan.name.trim().ifBlank { "Custom Schedule" }.take(100)
            val cleanDesc = plan.description.trim().take(500)
            val cleanDays = plan.daysOfWeek.distinct().filter { it in 1L..7L }.take(7)
            val cleanMode = if (plan.modeType == "ALLOW_ONLY_SELECTED") "ALLOW_ONLY_SELECTED" else "BLOCK_SELECTED"
            val cleanAllowed = plan.allowedPackages.distinct().take(150)
            val cleanRestricted = plan.restrictedPackages.distinct().filterNot { it in cleanAllowed }.take(150)
            val cleanReason = plan.customBlockReason.trim().take(300)

            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "name" to cleanName,
                        "description" to cleanDesc,
                        "enabled" to plan.enabled,
                        "startMinutes" to plan.startMinutes.coerceIn(0L, 1439L),
                        "endMinutes" to plan.endMinutes.coerceIn(0L, 1439L),
                        "daysOfWeek" to cleanDays,
                        "modeType" to cleanMode,
                        "allowedPackages" to cleanAllowed,
                        "restrictedPackages" to cleanRestricted,
                        "customBlockReason" to cleanReason,
                        "unlockedUntilMillis" to plan.unlockedUntilMillis.coerceAtLeast(0L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "name" to cleanName,
                        "description" to cleanDesc,
                        "enabled" to plan.enabled,
                        "startMinutes" to plan.startMinutes.coerceIn(0L, 1439L),
                        "endMinutes" to plan.endMinutes.coerceIn(0L, 1439L),
                        "daysOfWeek" to cleanDays,
                        "modeType" to cleanMode,
                        "allowedPackages" to cleanAllowed,
                        "restrictedPackages" to cleanRestricted,
                        "customBlockReason" to cleanReason,
                        "unlockedUntilMillis" to plan.unlockedUntilMillis.coerceAtLeast(0L),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(schedId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteSchedulePlan(userId: String, scheduleId: String): Result<Unit> {
        val docPath = "users/$userId/schedules/$scheduleId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            db.collection("users").document(uid).collection("schedules").document(scheduleId)
                .delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun syncChallengeLeaderboardEntry(
        userId: String,
        displayName: String,
        weekKey: String,
        challengeId: String,
        challengeTitle: String,
        tasksCompleted: Long,
        restrictedMinutes: Long,
        dailyStreak: Long
    ): Result<ChallengeLeaderboardEntry> {
        val cleanWeek = weekKey.trim().take(16)
        val entryId = sanitizeId("${cleanWeek}_$userId")
        val docPath = "challenge_leaderboard/$entryId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val safeName = displayName.trim().ifBlank { "Princess" }.take(100)
            val safeChallengeId = challengeId.trim().ifBlank { "pink_poise_sprint" }.take(64)
            val safeTitle = challengeTitle.trim().ifBlank { "Silk & Serenity Sprint" }.take(100)
            val safeTasks = tasksCompleted.coerceIn(0L, 10000L)
            val safeMinutes = restrictedMinutes.coerceIn(0L, 50000L)
            val safeStreak = dailyStreak.coerceIn(0L, 10000L)
            val totalScore = WeeklyChallengeCatalog.computeScore(safeMinutes, safeTasks, safeStreak)
            val badge = WeeklyChallengeCatalog.badgeForScore(totalScore).take(60)

            val docRef = db.collection("challenge_leaderboard").document(entryId)
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "challengeId" to safeChallengeId,
                        "challengeTitle" to safeTitle,
                        "displayName" to safeName,
                        "tasksCompleted" to safeTasks,
                        "restrictedMinutes" to safeMinutes,
                        "dailyStreak" to safeStreak,
                        "totalScore" to totalScore,
                        "badgeTitle" to badge,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "weekKey" to cleanWeek,
                        "challengeId" to safeChallengeId,
                        "challengeTitle" to safeTitle,
                        "displayName" to safeName,
                        "tasksCompleted" to safeTasks,
                        "restrictedMinutes" to safeMinutes,
                        "dailyStreak" to safeStreak,
                        "totalScore" to totalScore,
                        "badgeTitle" to badge,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            Result.success(
                ChallengeLeaderboardEntry(
                    id = entryId,
                    userId = uid,
                    weekKey = cleanWeek,
                    challengeId = safeChallengeId,
                    challengeTitle = safeTitle,
                    displayName = safeName,
                    tasksCompleted = safeTasks,
                    restrictedMinutes = safeMinutes,
                    dailyStreak = safeStreak,
                    totalScore = totalScore,
                    badgeTitle = badge
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }
}
