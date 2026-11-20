package com.example.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.BeSissyRepository
import com.example.data.DeviceAppsDataSource
import com.example.data.GeminiAiService
import com.example.data.UsageHistoryRepository
import com.example.data.VisualProofResult
import com.example.data.local.BeSissyDatabase
import com.example.data.local.HistoricalAppUsageEntity
import com.example.data.local.LocalCustomizationDataStore
import com.example.data.local.RestrictionSessionLogEntity
import com.example.model.AdhdDailyInsight
import com.example.model.AppRule
import com.example.model.ChallengeLeaderboardEntry
import com.example.model.CustomExitTask
import com.example.model.DefaultAdhdContent
import com.example.model.InstalledAppItem
import com.example.model.RecordedVoiceLine
import com.example.model.RestrictionSession
import com.example.model.SchedulePlan
import com.example.model.SmallWinTask
import com.example.model.SmallWinsDailyState
import com.example.model.UiState
import com.example.model.UsageLogEntry
import com.example.model.UserProfile
import com.example.model.WeeklyChallengeCatalog
import com.example.model.WelcomeSplashConfig
import com.example.service.RestrictionRuntimeState
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

data class WallBlockTrigger(
    val appName: String,
    val reasonTitle: String,
    val unlockHint: String,
    val unlockTargetMillis: Long
)

private data class LocalExperiencePrefs(
    val wallBlockBgImageUri: String = "",
    val voiceLines: List<RecordedVoiceLine> = emptyList(),
    val selectedVoiceLineId: String = "",
    val adhdCalmMode: Boolean = false,
    val adhdBrainDumpNote: String = "",
    val selectedChallengeId: String = "pink_poise_sprint",
    val completedExitTasksCount: Long = 0L,
    val cachedAdhdInsight: AdhdDailyInsight? = null,
    val smallWinsState: SmallWinsDailyState = SmallWinsDailyState(
        dateKey = DefaultAdhdContent.todayDateKey(),
        tasks = DefaultAdhdContent.defaultSmallWinPresets()
    ),
    val welcomeSplashConfig: WelcomeSplashConfig = WelcomeSplashConfig()
)

data class MainDashboardSnapshot(
    val profile: UserProfile,
    val appRules: List<AppRule>,
    val activeSession: RestrictionSession?,
    val usageLogs: List<UsageLogEntry>,
    val customTasks: List<CustomExitTask>,
    val installedApps: List<InstalledAppItem>,
    val schedules: List<SchedulePlan> = emptyList(),
    val historicalUsage: List<HistoricalAppUsageEntity> = emptyList(),
    val sessionHistoryLogs: List<RestrictionSessionLogEntity> = emptyList(),
    val wallBlockBgImageUri: String = "",
    val voiceLines: List<RecordedVoiceLine> = emptyList(),
    val selectedVoiceLineId: String = "",
    val adhdCalmMode: Boolean = false,
    val adhdBrainDumpNote: String = "",
    val selectedChallengeId: String = "pink_poise_sprint",
    val completedExitTasksCount: Long = 0L,
    val cachedAdhdInsight: AdhdDailyInsight? = null,
    val smallWinsState: SmallWinsDailyState = SmallWinsDailyState(
        dateKey = DefaultAdhdContent.todayDateKey(),
        tasks = DefaultAdhdContent.defaultSmallWinPresets()
    ),
    val welcomeSplashConfig: WelcomeSplashConfig = WelcomeSplashConfig()
) {
    val selectedVoiceLine: RecordedVoiceLine?
        get() = voiceLines.firstOrNull { it.id == selectedVoiceLineId }
}

class BeSissyViewModel(
    private val repository: BeSissyRepository,
    private val usageHistoryRepository: UsageHistoryRepository,
    private val localCustomization: LocalCustomizationDataStore,
    private val appsDataSource: DeviceAppsDataSource,
    private val aiService: GeminiAiService,
    private val currentUserId: String,
    private val displayName: String
) : ViewModel() {

    private val _installedApps = MutableStateFlow<List<InstalledAppItem>>(emptyList())
    private val _nowMillis = MutableStateFlow(System.currentTimeMillis())
    val nowMillis: StateFlow<Long> = _nowMillis.asStateFlow()

    private val _actionBanner = MutableStateFlow<String?>(null)
    val actionBanner: StateFlow<String?> = _actionBanner.asStateFlow()

    private val _pendingWallBlock = MutableStateFlow<WallBlockTrigger?>(null)
    val pendingWallBlock: StateFlow<WallBlockTrigger?> = _pendingWallBlock.asStateFlow()

    private val _isVerifyingProof = MutableStateFlow(false)
    val isVerifyingProof: StateFlow<Boolean> = _isVerifyingProof.asStateFlow()

    private val _proofResult = MutableStateFlow<VisualProofResult?>(null)
    val proofResult: StateFlow<VisualProofResult?> = _proofResult.asStateFlow()

    private val _proofError = MutableStateFlow<String?>(null)
    val proofError: StateFlow<String?> = _proofError.asStateFlow()

    private val _isVerifyingTask = MutableStateFlow(false)
    val isVerifyingTask: StateFlow<Boolean> = _isVerifyingTask.asStateFlow()

    private val _taskProofResult = MutableStateFlow<VisualProofResult?>(null)
    val taskProofResult: StateFlow<VisualProofResult?> = _taskProofResult.asStateFlow()

    private val _taskProofError = MutableStateFlow<String?>(null)
    val taskProofError: StateFlow<String?> = _taskProofError.asStateFlow()

    private val _isAnalyzingDailyAdhd = MutableStateFlow(false)
    val isAnalyzingDailyAdhd: StateFlow<Boolean> = _isAnalyzingDailyAdhd.asStateFlow()

    private val _dailyAdhdError = MutableStateFlow<String?>(null)
    val dailyAdhdError: StateFlow<String?> = _dailyAdhdError.asStateFlow()

    private val _isSyncingLeaderboard = MutableStateFlow(false)
    val isSyncingLeaderboard: StateFlow<Boolean> = _isSyncingLeaderboard.asStateFlow()

    val welcomeSplashConfigState: StateFlow<WelcomeSplashConfig?> =
        localCustomization.welcomeSplashConfig
            .map<WelcomeSplashConfig, WelcomeSplashConfig?> { it }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = null
            )

    val currentWeekKey: String = WeeklyChallengeCatalog.currentWeekKey()

    val leaderboardState: StateFlow<UiState<List<ChallengeLeaderboardEntry>>> =
        repository.observeWeeklyLeaderboard(currentWeekKey)
            .map<List<ChallengeLeaderboardEntry>, UiState<List<ChallengeLeaderboardEntry>>> { entries ->
                if (entries.isEmpty()) UiState.Empty else UiState.Success(entries)
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing global challenge leaderboard", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load global leaderboard"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = UiState.Loading
            )

    private val coreFlow = combine(
        repository.observeProfile(currentUserId),
        repository.observeAppRules(currentUserId),
        repository.observeSessions(currentUserId)
    ) { profile, rules, sessions -> Triple(profile, rules, sessions) }

    private val contentFlow = combine(
        repository.observeUsageLogs(currentUserId),
        repository.observeCustomTasks(currentUserId),
        repository.observeSchedules(currentUserId)
    ) { logs, tasks, schedules -> Triple(logs, tasks, schedules) }

    private val localVoiceAndWallFlow = combine(
        localCustomization.wallBlockBgImageUri,
        localCustomization.voiceLines,
        localCustomization.selectedVoiceLineId,
        localCustomization.adhdCalmMode
    ) { wallBgUri, voices, selectedVoiceId, calmMode ->
        LocalExperiencePrefs(
            wallBlockBgImageUri = wallBgUri,
            voiceLines = voices,
            selectedVoiceLineId = selectedVoiceId,
            adhdCalmMode = calmMode
        )
    }

    private val localAdhdAndChallengeFlow = combine(
        localVoiceAndWallFlow,
        localCustomization.adhdBrainDumpNote,
        localCustomization.selectedChallengeId,
        localCustomization.completedExitTasksCount,
        localCustomization.cachedAdhdInsight
    ) { base, note, challengeId, tasksCount, cachedInsight ->
        base.copy(
            adhdBrainDumpNote = note,
            selectedChallengeId = challengeId,
            completedExitTasksCount = tasksCount,
            cachedAdhdInsight = cachedInsight
        )
    }

    private val localSmallWinsAndWelcomeFlow = combine(
        localAdhdAndChallengeFlow,
        localCustomization.smallWinsState,
        localCustomization.welcomeSplashConfig
    ) { base, smallWins, welcomeCfg ->
        base.copy(
            smallWinsState = smallWins,
            welcomeSplashConfig = welcomeCfg
        )
    }

    private val roomAndPrefsFlow = combine(
        usageHistoryRepository.observeAllUsage(currentUserId),
        usageHistoryRepository.observeAllSessionLogs(currentUserId),
        localSmallWinsAndWelcomeFlow
    ) { histUsage, histSessions, localPrefs -> Triple(histUsage, histSessions, localPrefs) }

    val uiState: StateFlow<UiState<MainDashboardSnapshot>> = combine(
        coreFlow,
        contentFlow,
        roomAndPrefsFlow
    ) { (profile, rules, sessions), (logs, tasks, schedules), (roomUsage, roomSessions, localPrefs) ->
        val safeProfile = profile ?: UserProfile(userId = currentUserId, displayName = displayName)
        val activeSession = sessions.firstOrNull {
            it.status == "ACTIVE" || it.status == "PAUSED"
        }
        RestrictionRuntimeState.updateState(activeSession, rules, safeProfile, schedules)
        RestrictionRuntimeState.updateWallBlockBgImageUri(localPrefs.wallBlockBgImageUri)

        val deviceUsage = appsDataSource.readTodaySystemUsage(currentUserId)
        if (deviceUsage.isNotEmpty()) {
            RestrictionRuntimeState.updateLiveSystemUsageMinutes(
                deviceUsage.associate { it.packageName to it.screenTimeMinutes }
            )
        }
        val combinedLogs = (logs + deviceUsage).distinctBy { "${it.dateKey}_${it.packageName}_${it.hourOfDay}" }

        val effectiveWelcomeConfig = if (localPrefs.welcomeSplashConfig.fontKey == "custom_file" &&
            localPrefs.welcomeSplashConfig.customFontUri.isBlank()
        ) {
            localPrefs.welcomeSplashConfig.copy(customFontUri = safeProfile.customFontUri)
        } else {
            localPrefs.welcomeSplashConfig
        }

        MainDashboardSnapshot(
            profile = safeProfile,
            appRules = rules,
            activeSession = activeSession,
            usageLogs = combinedLogs,
            customTasks = tasks,
            installedApps = _installedApps.value,
            schedules = schedules,
            historicalUsage = roomUsage,
            sessionHistoryLogs = roomSessions,
            wallBlockBgImageUri = localPrefs.wallBlockBgImageUri,
            voiceLines = localPrefs.voiceLines,
            selectedVoiceLineId = localPrefs.selectedVoiceLineId,
            adhdCalmMode = localPrefs.adhdCalmMode,
            adhdBrainDumpNote = localPrefs.adhdBrainDumpNote,
            selectedChallengeId = localPrefs.selectedChallengeId,
            completedExitTasksCount = localPrefs.completedExitTasksCount,
            cachedAdhdInsight = localPrefs.cachedAdhdInsight,
            smallWinsState = localPrefs.smallWinsState,
            welcomeSplashConfig = effectiveWelcomeConfig
        )
    }
        .map<MainDashboardSnapshot, UiState<MainDashboardSnapshot>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing dashboard state", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to sync Be Sissy data"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    init {
        _installedApps.value = appsDataSource.loadLaunchableApps()
        viewModelScope.launch {
            repository.ensureInitialized(currentUserId, displayName)
                .onFailure { _actionBanner.value = it.localizedMessage ?: "Profile sync warning" }
        }
        // Sync 30-day Android UsageStats (when permission granted) + Firestore logs into Room
        viewModelScope.launch {
            try {
                val systemHistory = appsDataSource.readHistoricalSystemUsage(currentUserId, daysBack = 30)
                if (systemHistory.isNotEmpty()) {
                    usageHistoryRepository.syncUsageLogs(currentUserId, systemHistory, source = "SYSTEM")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "System usage sync skipped", e)
            }
        }
        viewModelScope.launch {
            repository.observeUsageLogs(currentUserId)
                .catch { /* Handled by main uiState */ }
                .collect { remoteLogs ->
                    if (remoteLogs.isNotEmpty()) {
                        usageHistoryRepository.syncUsageLogs(currentUserId, remoteLogs, source = "FIRESTORE")
                    }
                }
        }
        viewModelScope.launch {
            repository.observeSessions(currentUserId)
                .catch { /* Handled by main uiState */ }
                .collect { remoteSessions ->
                    if (remoteSessions.isNotEmpty()) {
                        usageHistoryRepository.syncSessionsFromRemote(currentUserId, remoteSessions)
                    }
                }
        }
        viewModelScope.launch {
            while (true) {
                delay(1000L)
                val now = System.currentTimeMillis()
                _nowMillis.value = now
                checkAutoCompleteSession(now)
            }
        }
    }

    private fun checkAutoCompleteSession(now: Long) {
        val current = (uiState.value as? UiState.Success)?.data ?: return
        val session = current.activeSession ?: return
        if (session.status == "PAUSED" && session.pausedUntilMillis in 1..now) {
            viewModelScope.launch {
                repository.updateActiveSession(
                    currentUserId,
                    session.id,
                    mapOf("status" to "ACTIVE", "pausedUntilMillis" to 0L)
                )
            }
        } else if (session.status == "ACTIVE" && session.endsAtMillis <= now) {
            viewModelScope.launch {
                val res = repository.updateActiveSession(
                    currentUserId,
                    session.id,
                    mapOf("status" to "COMPLETED")
                )
                if (res.isSuccess) {
                    usageHistoryRepository.recordRestrictionSessionLog(
                        id = session.id,
                        userId = currentUserId,
                        sessionTitle = "Focus Session",
                        mode = session.mode,
                        startedAtMillis = session.startedAtMillis,
                        endedAtMillis = now,
                        durationMinutes = session.durationMinutes,
                        outcome = "COMPLETED",
                        earlyExitMethod = ""
                    )
                    usageHistoryRepository.recordUsageSnapshot(
                        userId = currentUserId,
                        packageName = "com.aistudio.besissy.qkvnxm",
                        appName = "Focus Session",
                        screenTimeMinutes = 0L,
                        restrictedMinutes = session.durationMinutes,
                        timestampMillis = now,
                        source = "SESSION"
                    )
                    val reward = repository.awardDailySessionCompletion(
                        currentUserId,
                        current.profile,
                        session.durationMinutes
                    ).getOrNull()
                    _actionBanner.value = if (reward != null) {
                        "Session complete! Earned +${reward.first} Key & +${reward.second} Plaps ♡"
                    } else {
                        "Session complete! ♡"
                    }
                }
            }
        }
    }

    fun clearBanner() {
        _actionBanner.value = null
    }

    fun showBanner(message: String) {
        _actionBanner.value = message
    }

    fun clearPendingWallBlock() {
        _pendingWallBlock.value = null
    }

    fun clearTaskProofState() {
        _taskProofResult.value = null
        _taskProofError.value = null
    }

    fun updateWallBlockBgImageUri(uri: String) {
        viewModelScope.launch {
            localCustomization.setWallBlockBgImageUri(uri)
            RestrictionRuntimeState.updateWallBlockBgImageUri(uri)
            _actionBanner.value = if (uri.isNotBlank()) {
                "Wall Block background image updated ♡"
            } else {
                "Wall Block background image cleared"
            }
        }
    }

    fun claimDailyGoalStreak() {
        val current = (uiState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            repository.claimDailyGoalStreak(
                userId = currentUserId,
                currentProfile = current.profile,
                rules = current.appRules
            ).onSuccess { outcome ->
                _actionBanner.value = when {
                    outcome.alreadyClaimedToday -> "Today's usage goal reward already claimed ♡"
                    outcome.isWeeklyMilestone ->
                        "7-Day Weekly Streak! Earned +${outcome.totalKeysAwarded} Keys (+10 Bonus!) & +${outcome.plapsAwarded} Plaps ♡"
                    else ->
                        "Day ${outcome.newStreak} Streak! Earned +${outcome.dailyKeysAwarded} Key & +${outcome.plapsAwarded} Plaps ♡"
                }
            }.onFailure { err ->
                _actionBanner.value = err.localizedMessage ?: "Stay within daily app limits to claim streak"
            }
        }
    }

    fun startSession(durationMinutes: Long) {
        viewModelScope.launch {
            repository.startSession(currentUserId, "SESSION", durationMinutes)
                .onSuccess { sessionId ->
                    val now = System.currentTimeMillis()
                    usageHistoryRepository.recordRestrictionSessionLog(
                        id = sessionId,
                        userId = currentUserId,
                        sessionTitle = "Focus Sprint (${durationMinutes}m)",
                        mode = "SESSION",
                        startedAtMillis = now,
                        endedAtMillis = now + durationMinutes * 60_000L,
                        durationMinutes = durationMinutes,
                        outcome = "ACTIVE",
                        earlyExitMethod = ""
                    )
                    _actionBanner.value = "Focus Sprint started for ${durationMinutes}m ♡"
                }
                .onFailure { _actionBanner.value = it.localizedMessage ?: "Could not start session" }
        }
    }

    fun setAdhdCalmMode(enabled: Boolean) {
        viewModelScope.launch {
            localCustomization.setAdhdCalmMode(enabled)
            _actionBanner.value = if (enabled) {
                "ADHD Friendly Mode ON — active task & Pomodoro only ♡"
            } else {
                "Full dashboard & navigation restored ♡"
            }
        }
    }

    fun setActiveFocusTask(title: String) {
        val current = (uiState.value as? UiState.Success)?.data ?: return
        val cleaned = title.trim().take(120)
        if (cleaned.isBlank()) return
        viewModelScope.launch {
            val today = DefaultAdhdContent.todayDateKey()
            val curState = current.smallWinsState
            val existingMatch = curState.tasks.firstOrNull { !it.completed && it.title.equals(cleaned, ignoreCase = true) }
            val reorderedTasks = if (existingMatch != null) {
                listOf(existingMatch) + curState.tasks.filterNot { it.id == existingMatch.id }
            } else {
                val newTask = SmallWinTask(
                    id = "sw_${System.currentTimeMillis()}",
                    title = cleaned,
                    completed = false,
                    plapReward = 2L,
                    isPreset = false
                )
                listOf(newTask) + curState.tasks
            }
            localCustomization.setSmallWinsState(
                curState.copy(
                    dateKey = today,
                    tasks = reorderedTasks
                )
            )
            _actionBanner.value = "Active focus task set: '$cleaned' ♡"
        }
    }

    fun setAdhdBrainDumpNote(note: String) {
        viewModelScope.launch {
            localCustomization.setAdhdBrainDumpNote(note)
        }
    }

    fun toggleSmallWinTask(taskId: String) {
        val current = (uiState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            val today = DefaultAdhdContent.todayDateKey()
            val curState = current.smallWinsState
            val targetTask = curState.tasks.firstOrNull { it.id == taskId } ?: return@launch
            val nowCompleted = !targetTask.completed
            val alreadyRewarded = taskId in curState.rewardedIdsToday

            val updatedTasks = curState.tasks.map { item ->
                if (item.id == taskId) item.copy(completed = nowCompleted) else item
            }
            val updatedRewarded = if (nowCompleted) {
                curState.rewardedIdsToday + taskId
            } else {
                curState.rewardedIdsToday
            }
            localCustomization.setSmallWinsState(
                SmallWinsDailyState(
                    dateKey = today,
                    tasks = updatedTasks,
                    rewardedIdsToday = updatedRewarded
                )
            )

            if (nowCompleted && !alreadyRewarded) {
                val newPlaps = (current.profile.plapsBalance + targetTask.plapReward).coerceAtMost(100_000L)
                repository.updateProfilePreferences(
                    currentUserId,
                    mapOf("plapsBalance" to newPlaps)
                )
                localCustomization.incrementCompletedExitTasksCount(1L)
                _actionBanner.value = "Small Win complete! Earned +${targetTask.plapReward} Plaps 🎀"
            }
        }
    }

    fun addSmallWinTask(title: String, plapReward: Long = 2L) {
        val current = (uiState.value as? UiState.Success)?.data ?: return
        val cleaned = title.trim().take(120)
        if (cleaned.isBlank()) return
        viewModelScope.launch {
            val curState = current.smallWinsState
            val newTask = SmallWinTask(
                id = "sw_${System.currentTimeMillis()}",
                title = cleaned,
                completed = false,
                plapReward = plapReward.coerceIn(1L, 10L),
                isPreset = false
            )
            localCustomization.setSmallWinsState(
                curState.copy(tasks = listOf(newTask) + curState.tasks)
            )
            _actionBanner.value = "Added Small Win task (+${newTask.plapReward} Plaps reward) ♡"
        }
    }

    fun deleteSmallWinTask(taskId: String) {
        val current = (uiState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            val curState = current.smallWinsState
            localCustomization.setSmallWinsState(
                curState.copy(tasks = curState.tasks.filterNot { it.id == taskId })
            )
        }
    }

    fun awardHyperFocusCompletion(taskLabel: String) {
        val current = (uiState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            val bonusPlaps = 3L
            val newPlaps = (current.profile.plapsBalance + bonusPlaps).coerceAtMost(100_000L)
            repository.updateProfilePreferences(
                currentUserId,
                mapOf("plapsBalance" to newPlaps)
            )
            localCustomization.incrementCompletedExitTasksCount(1L)
            _actionBanner.value = "Hyper-Focus complete: '${taskLabel.take(32)}' (+${bonusPlaps} Plaps 🎀)"
        }
    }

    fun setWelcomeSplashEnabled(enabled: Boolean) {
        viewModelScope.launch {
            localCustomization.setWelcomeSplashEnabled(enabled)
            _actionBanner.value = if (enabled) {
                "3-second Welcome Page enabled ♡"
            } else {
                "3-second Welcome Page turned off"
            }
        }
    }

    fun setWelcomeSplashWallpaper(wallpaperKey: String, customUri: String? = null) {
        viewModelScope.launch {
            localCustomization.setWelcomeSplashWallpaper(wallpaperKey, customUri)
            _actionBanner.value = "Welcome Page wallpaper updated ♡"
        }
    }

    fun setWelcomeSplashNote(note: String) {
        viewModelScope.launch {
            localCustomization.setWelcomeSplashNote(note)
            _actionBanner.value = "Welcome Page note saved ♡"
        }
    }

    fun setWelcomeSplashFontKey(fontKey: String, customFontUri: String? = null) {
        viewModelScope.launch {
            localCustomization.setWelcomeSplashFontKey(fontKey, customFontUri)
            _actionBanner.value = "Welcome Page font updated ♡"
        }
    }

    fun setWelcomeSplashFontColorHex(hex: String) {
        viewModelScope.launch {
            localCustomization.setWelcomeSplashFontColorHex(hex)
            _actionBanner.value = "Welcome Page text color updated ♡"
        }
    }

    fun saveRecordedVoiceLine(label: String, filePath: String, durationSeconds: Int) {
        val current = (uiState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            val newId = "v_${System.currentTimeMillis()}"
            val cleanLabel = label.trim().ifBlank { "Feminine Voice #${current.voiceLines.size + 1}" }
            val item = RecordedVoiceLine(
                id = newId,
                label = cleanLabel,
                filePath = filePath,
                durationSeconds = durationSeconds.coerceAtLeast(1),
                createdAtMillis = System.currentTimeMillis()
            )
            localCustomization.setVoiceLines(listOf(item) + current.voiceLines)
            localCustomization.setSelectedVoiceLineId(newId)
            _actionBanner.value = "Saved & selected '$cleanLabel' for End Session Tasks ♡"
        }
    }

    fun selectVoiceLine(id: String) {
        viewModelScope.launch {
            localCustomization.setSelectedVoiceLineId(id)
            _actionBanner.value = if (id.isBlank()) {
                "Using default system sound for End Session Tasks"
            } else {
                "Selected custom feminine voice line ♡"
            }
        }
    }

    fun deleteVoiceLine(line: RecordedVoiceLine) {
        val current = (uiState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            val remaining = current.voiceLines.filterNot { it.id == line.id }
            localCustomization.setVoiceLines(remaining)
            if (current.selectedVoiceLineId == line.id) {
                localCustomization.setSelectedVoiceLineId(remaining.firstOrNull()?.id.orEmpty())
            }
            try {
                java.io.File(line.filePath).delete()
            } catch (_: Exception) {
            }
            _actionBanner.value = "Removed '${line.label}'"
        }
    }

    fun generateWeeklyAdhdInsight() {
        val current = (uiState.value as? UiState.Success)?.data ?: return
        if (_isAnalyzingDailyAdhd.value) return
        viewModelScope.launch {
            _isAnalyzingDailyAdhd.value = true
            _dailyAdhdError.value = null
            try {
                val cutoff = System.currentTimeMillis() - 7L * 86_400_000L
                val weeklyUsage = current.historicalUsage.filter { it.timestampMillis >= cutoff }
                val weeklySessions = current.sessionHistoryLogs.filter { it.startedAtMillis >= cutoff }
                val insight = withTimeout(60_000L) {
                    aiService.generateDailyAdhdCoachInsights(
                        displayName = current.profile.displayName,
                        dailyStreak = current.profile.dailyStreak,
                        weeklyUsage = weeklyUsage,
                        weeklySessions = weeklySessions,
                        schedules = current.schedules
                    )
                }
                localCustomization.setCachedAdhdInsight(insight)
                _actionBanner.value = "Belle analyzed your week with ADHD focus tips ♡"
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Daily ADHD insight timed out", e)
                _dailyAdhdError.value = "Gemini analysis timed out. Please tap Retry."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Daily ADHD insight failed", e)
                _dailyAdhdError.value = e.localizedMessage ?: "Failed to analyze weekly data."
            } finally {
                _isAnalyzingDailyAdhd.value = false
            }
        }
    }

    fun selectWeeklyChallenge(challengeId: String) {
        viewModelScope.launch {
            localCustomization.setSelectedChallengeId(challengeId)
            syncChallengeLeaderboard()
        }
    }

    fun syncChallengeLeaderboard(extraTasksDelta: Long = 0L) {
        val current = (uiState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            _isSyncingLeaderboard.value = true
            try {
                val cutoff = System.currentTimeMillis() - 7L * 86_400_000L
                val weeklyRestricted = current.historicalUsage
                    .filter { it.timestampMillis >= cutoff }
                    .sumOf { it.restrictedMinutes }
                    .coerceAtLeast(
                        current.sessionHistoryLogs
                            .filter { it.startedAtMillis >= cutoff }
                            .sumOf { it.durationMinutes }
                    )
                val completedFromSessions = current.sessionHistoryLogs
                    .count { it.startedAtMillis >= cutoff && (it.outcome == "COMPLETED" || it.outcome == "ENDED_EARLY") }
                    .toLong()
                val totalTasks = (current.completedExitTasksCount + extraTasksDelta + completedFromSessions)
                    .coerceAtLeast(0L)
                val challenge = WeeklyChallengeCatalog.findById(current.selectedChallengeId)

                repository.syncChallengeLeaderboardEntry(
                    userId = currentUserId,
                    displayName = current.profile.displayName,
                    weekKey = currentWeekKey,
                    challengeId = challenge.id,
                    challengeTitle = challenge.title,
                    tasksCompleted = totalTasks,
                    restrictedMinutes = weeklyRestricted,
                    dailyStreak = current.profile.dailyStreak
                ).onSuccess { entry ->
                    _actionBanner.value = "Leaderboard synced! Score: ${entry.totalScore} (${entry.badgeTitle}) ♡"
                }.onFailure { err ->
                    _actionBanner.value = err.localizedMessage ?: "Could not sync leaderboard score"
                }
            } finally {
                _isSyncingLeaderboard.value = false
            }
        }
    }

    fun setAppGloballyAllowed(app: InstalledAppItem, currentRule: AppRule?, allowed: Boolean) {
        viewModelScope.launch {
            val base = currentRule ?: AppRule(
                userId = currentUserId,
                packageName = app.packageName,
                appName = app.appName,
                category = app.category
            )
            val updated = base.copy(
                necessaryApp = allowed,
                restrictedInSession = if (allowed) false else base.restrictedInSession
            )
            repository.upsertAppRule(currentUserId, updated)
                .onSuccess {
                    _actionBanner.value = if (allowed) "${app.appName} allowed ♡" else "${app.appName} removed from Allow List"
                }
                .onFailure { _actionBanner.value = it.localizedMessage ?: "Update failed" }
        }
    }

    fun allowAllNecessaryApps() {
        val current = (uiState.value as? UiState.Success)?.data ?: return
        val rulesMap = current.appRules.associateBy { it.packageName }
        val essentialApps = current.installedApps.filter { it.isSystemEssential }
        if (essentialApps.isEmpty()) return
        viewModelScope.launch {
            essentialApps.forEach { app ->
                val existing = rulesMap[app.packageName]
                if (existing?.necessaryApp != true) {
                    val base = existing ?: AppRule(
                        userId = currentUserId,
                        packageName = app.packageName,
                        appName = app.appName,
                        category = app.category
                    )
                    repository.upsertAppRule(currentUserId, base.copy(necessaryApp = true, restrictedInSession = false))
                }
            }
            _actionBanner.value = "All necessary apps allowed ♡"
        }
    }

    fun saveSchedulePlan(plan: SchedulePlan) {
        viewModelScope.launch {
            repository.upsertSchedulePlan(currentUserId, plan.copy(userId = currentUserId))
                .onSuccess {
                    _actionBanner.value = "Schedule '${plan.name.ifBlank { "Custom Schedule" }}' saved ♡"
                }
                .onFailure {
                    _actionBanner.value = it.localizedMessage ?: "Could not save schedule"
                }
        }
    }

    fun toggleScheduleEnabled(plan: SchedulePlan) {
        val nextEnabled = !plan.enabled
        viewModelScope.launch {
            repository.upsertSchedulePlan(
                currentUserId,
                plan.copy(
                    enabled = nextEnabled,
                    unlockedUntilMillis = if (nextEnabled) 0L else plan.unlockedUntilMillis
                )
            ).onSuccess {
                _actionBanner.value = if (nextEnabled) "'${plan.name}' enabled ♡" else "'${plan.name}' paused"
            }.onFailure {
                _actionBanner.value = it.localizedMessage ?: "Could not update schedule"
            }
        }
    }

    fun deleteSchedulePlan(scheduleId: String) {
        viewModelScope.launch {
            repository.deleteSchedulePlan(currentUserId, scheduleId)
                .onSuccess { _actionBanner.value = "Schedule removed" }
                .onFailure { _actionBanner.value = it.localizedMessage ?: "Could not delete schedule" }
        }
    }

    fun updateScheduleRule(
        app: InstalledAppItem,
        currentRule: AppRule?,
        enabled: Boolean,
        startMinutes: Long,
        endMinutes: Long
    ) {
        viewModelScope.launch {
            val base = currentRule ?: AppRule(
                userId = currentUserId,
                packageName = app.packageName,
                appName = app.appName,
                category = app.category
            )
            repository.upsertAppRule(
                currentUserId,
                base.copy(
                    scheduleEnabled = enabled,
                    scheduleStartMinutes = startMinutes,
                    scheduleEndMinutes = endMinutes
                )
            ).onSuccess {
                _actionBanner.value = "Updated schedule for ${app.appName}"
            }.onFailure {
                _actionBanner.value = it.localizedMessage ?: "Schedule update failed"
            }
        }
    }

    fun updateDailyLimitRule(
        app: InstalledAppItem,
        currentRule: AppRule?,
        enabled: Boolean,
        limitMinutes: Long,
        simulatedUsedMinutes: Long? = null
    ) {
        viewModelScope.launch {
            val base = currentRule ?: AppRule(
                userId = currentUserId,
                packageName = app.packageName,
                appName = app.appName,
                category = app.category
            )
            val clampedLimit = limitMinutes.coerceIn(1L, 1440L)
            val used = (simulatedUsedMinutes ?: base.usedTodayMinutes).coerceIn(0L, 1440L)
            repository.upsertAppRule(
                currentUserId,
                base.copy(
                    dailyLimitEnabled = enabled,
                    dailyLimitMinutes = clampedLimit,
                    usedTodayMinutes = used
                )
            ).onSuccess {
                val now = System.currentTimeMillis()
                val notTempUnblocked = base.tempUnblockUntilMillis <= now
                if (enabled && used >= clampedLimit && notTempUnblocked && simulatedUsedMinutes != null) {
                    _pendingWallBlock.value = WallBlockTrigger(
                        appName = app.appName,
                        reasonTitle = "Daily Limit Exceeded (${used}m / ${clampedLimit}m)",
                        unlockHint = "Resets at 00:00 or unlock 5m with 5 Plaps",
                        unlockTargetMillis = RestrictionRuntimeState.computeNextMidnightMillis(now)
                    )
                } else {
                    _actionBanner.value = "Updated daily limit for ${app.appName}"
                }
            }.onFailure {
                _actionBanner.value = it.localizedMessage ?: "Limit update failed"
            }
        }
    }

    fun tempUnblockAppWithPlaps(rule: AppRule, minutes: Int = 5) {
        val current = (uiState.value as? UiState.Success)?.data ?: return
        if (current.profile.plapsBalance < minutes) {
            _actionBanner.value = "Need $minutes Plaps for temporary unblock"
            return
        }
        viewModelScope.launch {
            val unblockUntil = System.currentTimeMillis() + minutes * 60_000L
            repository.upsertAppRule(
                currentUserId,
                rule.copy(tempUnblockUntilMillis = unblockUntil)
            )
            repository.updateProfilePreferences(
                currentUserId,
                mapOf("plapsBalance" to (current.profile.plapsBalance - minutes).coerceAtLeast(0L))
            )
            _actionBanner.value = "${rule.appName} temporarily unblocked for ${minutes}m (-$minutes Plaps)"
        }
    }

    fun spendKeysOnSession(keysCount: Int) {
        val current = (uiState.value as? UiState.Success)?.data ?: return
        if (keysCount <= 0 || current.profile.keysBalance < keysCount) {
            _actionBanner.value = "Not enough Keys"
            return
        }
        val now = System.currentTimeMillis()
        val session = current.activeSession
        val activeSchedules = RestrictionRuntimeState.getCurrentlyActiveSchedules(now, current.schedules)
        if (session == null && activeSchedules.isEmpty()) return

        viewModelScope.launch {
            val reductionMillis = keysCount * 15L * 60_000L
            val newKeys = (current.profile.keysBalance - keysCount).coerceAtLeast(0L)

            if (session != null) {
                val newEnd = session.endsAtMillis - reductionMillis
                if (newEnd <= now) {
                    repository.updateActiveSession(
                        currentUserId,
                        session.id,
                        mapOf("status" to "ENDED_EARLY", "earlyExitMethod" to "KEYS")
                    )
                    val elapsedMins = ((now - session.startedAtMillis) / 60_000L).coerceAtLeast(1L)
                    usageHistoryRepository.recordRestrictionSessionLog(
                        id = session.id,
                        userId = currentUserId,
                        sessionTitle = "Focus Session",
                        mode = session.mode,
                        startedAtMillis = session.startedAtMillis,
                        endedAtMillis = now,
                        durationMinutes = elapsedMins,
                        outcome = "ENDED_EARLY",
                        earlyExitMethod = "KEYS"
                    )
                    _actionBanner.value = "Restriction ended early using $keysCount Key(s)!"
                } else {
                    repository.updateActiveSession(
                        currentUserId,
                        session.id,
                        mapOf("endsAtMillis" to newEnd)
                    )
                    _actionBanner.value = "Reduced restriction by ${keysCount * 15}m using $keysCount Key(s)!"
                }
            }

            if (activeSchedules.isNotEmpty()) {
                val unlockUntil = now + reductionMillis
                activeSchedules.forEach { plan ->
                    val endMillis = RestrictionRuntimeState.computeScheduleEndMillis(plan, now)
                    repository.upsertSchedulePlan(
                        currentUserId,
                        plan.copy(unlockedUntilMillis = unlockUntil.coerceAtMost(endMillis))
                    )
                    usageHistoryRepository.recordRestrictionSessionLog(
                        id = "sched_keys_${plan.id}_$now",
                        userId = currentUserId,
                        sessionTitle = plan.name,
                        mode = "SCHEDULE",
                        startedAtMillis = now - 60_000L,
                        endedAtMillis = now,
                        durationMinutes = (keysCount * 15L).coerceAtLeast(1L),
                        outcome = "ENDED_EARLY",
                        earlyExitMethod = "KEYS"
                    )
                }
                _actionBanner.value = "Unlocked active schedule for ${keysCount * 15}m using $keysCount Key(s)!"
            }

            repository.updateProfilePreferences(currentUserId, mapOf("keysBalance" to newKeys))
        }
    }

    fun pauseSessionWithPlaps(minutes: Int) {
        val current = (uiState.value as? UiState.Success)?.data ?: return
        if (minutes <= 0 || current.profile.plapsBalance < minutes) {
            _actionBanner.value = "Not enough Plaps"
            return
        }
        val now = System.currentTimeMillis()
        val session = current.activeSession
        val activeSchedules = RestrictionRuntimeState.getCurrentlyActiveSchedules(now, current.schedules)
        if (session == null && activeSchedules.isEmpty()) return

        viewModelScope.launch {
            val pauseDuration = minutes * 60_000L
            val newPlaps = (current.profile.plapsBalance - minutes).coerceAtLeast(0L)
            if (session != null) {
                repository.updateActiveSession(
                    currentUserId,
                    session.id,
                    mapOf(
                        "status" to "PAUSED",
                        "pausedUntilMillis" to (now + pauseDuration),
                        "endsAtMillis" to (session.endsAtMillis + pauseDuration)
                    )
                )
            }
            if (activeSchedules.isNotEmpty()) {
                activeSchedules.forEach { plan ->
                    repository.upsertSchedulePlan(
                        currentUserId,
                        plan.copy(unlockedUntilMillis = now + pauseDuration)
                    )
                    usageHistoryRepository.recordRestrictionSessionLog(
                        id = "sched_plap_${plan.id}_$now",
                        userId = currentUserId,
                        sessionTitle = plan.name,
                        mode = "SCHEDULE",
                        startedAtMillis = now - 60_000L,
                        endedAtMillis = now,
                        durationMinutes = minutes.toLong(),
                        outcome = "PAUSED",
                        earlyExitMethod = "PLAPS"
                    )
                }
            }
            repository.updateProfilePreferences(currentUserId, mapOf("plapsBalance" to newPlaps))
            _actionBanner.value = "Schedule paused for ${minutes}m (-$minutes Plaps)"
        }
    }

    fun completeSequentialTasksExit() {
        val current = (uiState.value as? UiState.Success)?.data ?: return
        val now = System.currentTimeMillis()
        val session = current.activeSession
        val activeSchedules = RestrictionRuntimeState.getCurrentlyActiveSchedules(now, current.schedules)
        if (session == null && activeSchedules.isEmpty()) return

        viewModelScope.launch {
            if (session != null) {
                repository.updateActiveSession(
                    currentUserId,
                    session.id,
                    mapOf("status" to "ENDED_EARLY", "earlyExitMethod" to "SEQUENTIAL_TASKS")
                )
                val elapsedMins = ((now - session.startedAtMillis) / 60_000L).coerceAtLeast(1L)
                usageHistoryRepository.recordRestrictionSessionLog(
                    id = session.id,
                    userId = currentUserId,
                    sessionTitle = "Focus Session",
                    mode = session.mode,
                    startedAtMillis = session.startedAtMillis,
                    endedAtMillis = now,
                    durationMinutes = elapsedMins,
                    outcome = "ENDED_EARLY",
                    earlyExitMethod = "SEQUENTIAL_TASKS"
                )
            }
            activeSchedules.forEach { plan ->
                val endMillis = RestrictionRuntimeState.computeScheduleEndMillis(plan, now)
                repository.upsertSchedulePlan(currentUserId, plan.copy(unlockedUntilMillis = endMillis))
                usageHistoryRepository.recordRestrictionSessionLog(
                    id = "sched_tasks_${plan.id}_$now",
                    userId = currentUserId,
                    sessionTitle = plan.name,
                    mode = "SCHEDULE",
                    startedAtMillis = now - 60_000L,
                    endedAtMillis = now,
                    durationMinutes = 15L,
                    outcome = "ENDED_EARLY",
                    earlyExitMethod = "SEQUENTIAL_TASKS"
                )
            }
            val nextSquat = (current.profile.squatCountTarget + 5L).coerceAtMost(1000L)
            repository.updateProfilePreferences(
                currentUserId,
                mapOf("squatCountTarget" to nextSquat)
            )
            localCustomization.incrementCompletedExitTasksCount(1L)
            syncChallengeLeaderboard(extraTasksDelta = 1L)
            _actionBanner.value = "4 Tasks complete! Schedule unlocked. Next squat goal: $nextSquat"
        }
    }

    fun verifyPhysicalTask(
        captured: Bitmap,
        taskName: String,
        taskInstruction: String,
        targetCount: Int
    ) {
        viewModelScope.launch {
            _isVerifyingTask.value = true
            _taskProofError.value = null
            _taskProofResult.value = null
            try {
                val res = withTimeout(60_000L) {
                    aiService.verifyPhysicalTaskPhoto(
                        capturedPhoto = captured,
                        taskName = taskName,
                        taskInstruction = taskInstruction,
                        targetCount = targetCount
                    )
                }
                _taskProofResult.value = res
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Physical task verification failed", e)
                _taskProofError.value = e.localizedMessage ?: "AI camera check failed"
            } finally {
                _isVerifyingTask.value = false
            }
        }
    }

    fun verifyVisualProofPhoto(captured: Bitmap) {
        val current = (uiState.value as? UiState.Success)?.data ?: return
        val session = current.activeSession
        viewModelScope.launch {
            _isVerifyingProof.value = true
            _proofError.value = null
            _proofResult.value = null
            try {
                val res = withTimeout(60_000L) {
                    aiService.verifyVisualProof(
                        capturedPhoto = captured,
                        proofDescription = current.profile.visualProofDescription
                    )
                }
                _proofResult.value = res
                if (res.approved) {
                    val now = System.currentTimeMillis()
                    if (session != null) {
                        repository.updateActiveSession(
                            currentUserId,
                            session.id,
                            mapOf("status" to "ENDED_EARLY", "earlyExitMethod" to "AI_VISUAL_PROOF")
                        )
                        val elapsedMins = ((now - session.startedAtMillis) / 60_000L).coerceAtLeast(1L)
                        usageHistoryRepository.recordRestrictionSessionLog(
                            id = session.id,
                            userId = currentUserId,
                            sessionTitle = "Focus Session",
                            mode = session.mode,
                            startedAtMillis = session.startedAtMillis,
                            endedAtMillis = now,
                            durationMinutes = elapsedMins,
                            outcome = "ENDED_EARLY",
                            earlyExitMethod = "AI_VISUAL_PROOF"
                        )
                    }
                    RestrictionRuntimeState.getCurrentlyActiveSchedules(now, current.schedules).forEach { plan ->
                        val endMillis = RestrictionRuntimeState.computeScheduleEndMillis(plan, now)
                        repository.upsertSchedulePlan(currentUserId, plan.copy(unlockedUntilMillis = endMillis))
                        usageHistoryRepository.recordRestrictionSessionLog(
                            id = "sched_proof_${plan.id}_$now",
                            userId = currentUserId,
                            sessionTitle = plan.name,
                            mode = "SCHEDULE",
                            startedAtMillis = now - 60_000L,
                            endedAtMillis = now,
                            durationMinutes = 15L,
                            outcome = "ENDED_EARLY",
                            earlyExitMethod = "AI_VISUAL_PROOF"
                        )
                    }
                    localCustomization.incrementCompletedExitTasksCount(1L)
                    syncChallengeLeaderboard(extraTasksDelta = 1L)
                    _actionBanner.value = "AI Visual Proof verified! Schedule unlocked ♡"
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Visual proof verification failed", e)
                _proofError.value = e.localizedMessage ?: "Visual proof check failed"
            } finally {
                _isVerifyingProof.value = false
            }
        }
    }

    fun updatePreferences(updates: Map<String, Any>, toast: String = "Saved ♡") {
        viewModelScope.launch {
            repository.updateProfilePreferences(currentUserId, updates)
                .onSuccess { _actionBanner.value = toast }
                .onFailure { _actionBanner.value = it.localizedMessage ?: "Failed to save" }
        }
    }

    fun addCustomTask(taskType: String, content: String) {
        viewModelScope.launch {
            repository.addCustomTask(currentUserId, taskType, content)
                .onSuccess { _actionBanner.value = "Added custom task ♡" }
                .onFailure { _actionBanner.value = it.localizedMessage ?: "Failed to add task" }
        }
    }

    fun deleteCustomTask(taskId: String) {
        viewModelScope.launch {
            repository.deleteCustomTask(currentUserId, taskId)
        }
    }

    fun logSampleScreenTime(app: InstalledAppItem, screenMins: Long, restrictedMins: Long) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            usageHistoryRepository.recordUsageSnapshot(
                userId = currentUserId,
                packageName = app.packageName,
                appName = app.appName,
                screenTimeMinutes = screenMins,
                restrictedMinutes = restrictedMins,
                timestampMillis = now,
                source = "TELEMETRY"
            )
            usageHistoryRepository.recordRestrictionSessionLog(
                id = "sess_telemetry_$now",
                userId = currentUserId,
                sessionTitle = "Schedule Telemetry (${app.appName})",
                mode = "SCHEDULE",
                startedAtMillis = now - restrictedMins * 60_000L,
                endedAtMillis = now,
                durationMinutes = restrictedMins,
                outcome = "COMPLETED",
                earlyExitMethod = ""
            )
            repository.recordUsageLog(
                userId = currentUserId,
                packageName = app.packageName,
                appName = app.appName,
                screenTimeMinutes = screenMins,
                restrictedMinutes = restrictedMins
            )
            _actionBanner.value = "Logged ${app.appName} telemetry to Room & Reports ♡"
        }
    }

    companion object {
        private const val TAG = "BeSissyVM"

        fun provideFactory(
            context: Context,
            currentUserId: String,
            displayName: String
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val appContext = context.applicationContext
                val dbId = appContext.getString(com.example.R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(dbId)
                val roomDb = BeSissyDatabase.getInstance(appContext)
                BeSissyViewModel(
                    repository = BeSissyRepository(db),
                    usageHistoryRepository = UsageHistoryRepository(roomDb.usageHistoryDao()),
                    localCustomization = LocalCustomizationDataStore(appContext),
                    appsDataSource = DeviceAppsDataSource(appContext),
                    aiService = GeminiAiService(),
                    currentUserId = currentUserId,
                    displayName = displayName
                )
            }
        }
    }
}
