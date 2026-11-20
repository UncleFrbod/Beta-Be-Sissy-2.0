package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.auth.attemptAutoSignIn
import com.example.auth.authStateFlow
import androidx.compose.foundation.background
import com.example.firebase.configureAppCheck
import com.example.model.DefaultAdhdContent
import com.example.model.UiState
import com.example.ui.components.CoquetteCard
import com.example.ui.components.CoquetteWallpaperBackground
import com.example.ui.components.EarlyExitBottomSheet
import com.example.ui.components.WallBlockScreen
import com.example.ui.screens.AiStudioScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ChallengesScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.HyperFocusScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.RestrictionsScreen
import com.example.ui.screens.ScreenSaverScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.WelcomeSplashScreen
import com.example.ui.theme.BeSissyTheme
import com.example.ui.theme.BerryInk
import com.example.ui.theme.BlushSurface
import com.example.ui.theme.PinkContainer
import com.example.ui.theme.PinkPrimary
import com.example.viewmodel.AiViewModel
import com.example.viewmodel.BeSissyViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

import androidx.compose.runtime.mutableIntStateOf
import com.example.service.RestrictionRuntimeState

private sealed class MainTab(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    data object Home : MainTab("home", "Home", Icons.Default.Home)
    data object Restrictions : MainTab("restrictions", "Schedule", Icons.Default.Shield)
    data object Challenges : MainTab("challenges", "Quests", Icons.Default.EmojiEvents)
    data object Reports : MainTab("reports", "Reports", Icons.Default.Assessment)
    data object Ai : MainTab("ai", "AI", Icons.Default.AutoAwesome)
    data object Settings : MainTab("settings", "Settings", Icons.Default.Settings)
}

private data class WallBlockPreviewData(
    val appName: String,
    val reasonTitle: String,
    val unlockHint: String,
    val unlockTargetMillis: Long = RestrictionRuntimeState.computeNextMidnightMillis()
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Configure App Check with launch intent BEFORE Compose UI or ViewModels
        configureAppCheck(this, intent)
        enableEdgeToEdge()
        setContent {
            BeSissyTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val context = LocalContext.current
    val credentialManager = remember { CredentialManager.create(context) }
    val currentUser by Firebase.auth.authStateFlow()
        .collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

    LaunchedEffect(Unit) {
        if (Firebase.auth.currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {},
                onUnauthenticated = {},
                scope = this
            )
        }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            BeSissyMainApp(
                currentUserId = user.uid,
                displayName = user.displayName ?: "Princess"
            )
        }
    }
}

@Composable
private fun BeSissyMainApp(
    currentUserId: String,
    displayName: String
) {
    val context = LocalContext.current
    val mainViewModel: BeSissyViewModel = viewModel(
        key = "main_$currentUserId",
        factory = BeSissyViewModel.provideFactory(context, currentUserId, displayName)
    )
    val aiViewModel: AiViewModel = viewModel(
        key = "ai_$currentUserId",
        factory = AiViewModel.provideFactory(context, currentUserId)
    )

    val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()
    val nowMillis by mainViewModel.nowMillis.collectAsStateWithLifecycle()
    val actionBanner by mainViewModel.actionBanner.collectAsStateWithLifecycle()
    val isVerifyingProof by mainViewModel.isVerifyingProof.collectAsStateWithLifecycle()
    val proofResult by mainViewModel.proofResult.collectAsStateWithLifecycle()
    val proofError by mainViewModel.proofError.collectAsStateWithLifecycle()
    val isVerifyingTask by mainViewModel.isVerifyingTask.collectAsStateWithLifecycle()
    val taskProofResult by mainViewModel.taskProofResult.collectAsStateWithLifecycle()
    val taskProofError by mainViewModel.taskProofError.collectAsStateWithLifecycle()
    val pendingWallBlock by mainViewModel.pendingWallBlock.collectAsStateWithLifecycle()
    val leaderboardState by mainViewModel.leaderboardState.collectAsStateWithLifecycle()
    val isSyncingLeaderboard by mainViewModel.isSyncingLeaderboard.collectAsStateWithLifecycle()
    val isAnalyzingDailyAdhd by mainViewModel.isAnalyzingDailyAdhd.collectAsStateWithLifecycle()
    val dailyAdhdError by mainViewModel.dailyAdhdError.collectAsStateWithLifecycle()
    val welcomeConfigOrNull by mainViewModel.welcomeSplashConfigState.collectAsStateWithLifecycle()

    var coldSplashHandled by remember(currentUserId) { mutableStateOf(false) }
    var previewWelcomeSplash by remember { mutableStateOf(false) }
    var hyperFocusMinutes by remember { mutableStateOf<Int?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(actionBanner) {
        val msg = actionBanner
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            mainViewModel.clearBanner()
        }
    }

    if (!coldSplashHandled) {
        val loadedConfig = welcomeConfigOrNull
        if (loadedConfig == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BlushSurface)
            )
            return
        } else if (loadedConfig.enabled) {
            WelcomeSplashScreen(
                config = loadedConfig,
                onFinished = { coldSplashHandled = true }
            )
            return
        } else {
            LaunchedEffect(Unit) {
                coldSplashHandled = true
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BlushSurface)
            )
            return
        }
    }

    when (val state = uiState) {
        is UiState.Loading -> {
            CoquetteWallpaperBackground(wallpaperKey = "coquette_silk", customWallpaperUri = "") {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = PinkPrimary)
                        Spacer(Modifier.height(12.dp))
                        Text("${DefaultAdhdContent.SISSY_MOTTO} ♡", color = BerryInk)
                    }
                }
            }
        }

        is UiState.Error -> {
            CoquetteWallpaperBackground(wallpaperKey = "coquette_silk", customWallpaperUri = "") {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    CoquetteCard {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Sync Error", style = MaterialTheme.typography.titleLarge, color = BerryInk)
                            Spacer(Modifier.height(8.dp))
                            Text(state.message, color = BerryInk)
                            Spacer(Modifier.height(14.dp))
                            Button(
                                onClick = { mainViewModel.updatePreferences(emptyMap(), "Retrying sync...") },
                                colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary, contentColor = BlushSurface)
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                }
            }
        }

        is UiState.Empty -> Unit

        is UiState.Success -> {
            val snapshot = state.data
            val profile = snapshot.profile
            var showScreenSaver by remember { mutableStateOf(false) }
            var showEarlyExitSheet by remember { mutableStateOf(false) }
            var wallBlockPreview by remember { mutableStateOf<WallBlockPreviewData?>(null) }
            var restrictionsInitialTab by remember { mutableIntStateOf(0) }

            LaunchedEffect(pendingWallBlock) {
                val trigger = pendingWallBlock
                if (trigger != null) {
                    wallBlockPreview = WallBlockPreviewData(
                        appName = trigger.appName,
                        reasonTitle = trigger.reasonTitle,
                        unlockHint = trigger.unlockHint,
                        unlockTargetMillis = trigger.unlockTargetMillis
                    )
                    mainViewModel.clearPendingWallBlock()
                }
            }

            val activeSchedule = remember(snapshot.schedules, nowMillis / 15_000L) {
                RestrictionRuntimeState.getCurrentlyActiveSchedules(nowMillis, snapshot.schedules).firstOrNull()
            }

            val navController = rememberNavController()
            val navBackStack by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStack?.destination?.route ?: MainTab.Home.route

            val bottomTabs = remember {
                listOf(
                    MainTab.Home,
                    MainTab.Restrictions,
                    MainTab.Reports,
                    MainTab.Ai,
                    MainTab.Settings
                )
            }
            val railTabs = remember {
                listOf(
                    MainTab.Home,
                    MainTab.Restrictions,
                    MainTab.Challenges,
                    MainTab.Reports,
                    MainTab.Ai,
                    MainTab.Settings
                )
            }

            BeSissyTheme(
                fontKey = profile.selectedFontKey,
                customFontPath = profile.customFontUri,
                fontColorHex = profile.selectedFontColorHex
            ) {
                if (previewWelcomeSplash) {
                    BackHandler { previewWelcomeSplash = false }
                    WelcomeSplashScreen(
                        config = snapshot.welcomeSplashConfig,
                        onFinished = { previewWelcomeSplash = false }
                    )
                } else if (hyperFocusMinutes != null) {
                    HyperFocusScreen(
                        initialMinutes = hyperFocusMinutes ?: 15,
                        onCompleteTaskReward = mainViewModel::awardHyperFocusCompletion,
                        onExit = { hyperFocusMinutes = null }
                    )
                } else if (wallBlockPreview != null) {
                    BackHandler { wallBlockPreview = null }
                    val preview = wallBlockPreview!!
                    val matchedRule = snapshot.appRules.firstOrNull { it.appName == preview.appName }
                    WallBlockScreen(
                        blockedAppName = preview.appName,
                        blockReasonTitle = preview.reasonTitle,
                        unlockTimeHint = preview.unlockHint,
                        profile = profile,
                        unlockTargetMillis = preview.unlockTargetMillis,
                        customBgImageUri = snapshot.wallBlockBgImageUri,
                        onTempUnblockWithPlaps = if (matchedRule != null) {
                            {
                                mainViewModel.tempUnblockAppWithPlaps(matchedRule, 5)
                                wallBlockPreview = null
                            }
                        } else null,
                        onOpenEarlyExit = if (snapshot.activeSession != null || activeSchedule != null) {
                            {
                                wallBlockPreview = null
                                showEarlyExitSheet = true
                            }
                        } else null,
                        onDismissBack = { wallBlockPreview = null }
                    )
                } else if (showScreenSaver) {
                    ScreenSaverScreen(
                        profile = profile,
                        activeSession = snapshot.activeSession,
                        activeSchedule = activeSchedule,
                        nowMillis = nowMillis,
                        onOpenEarlyExit = {
                            showScreenSaver = false
                            showEarlyExitSheet = true
                        },
                        onExitScreenSaver = { showScreenSaver = false }
                    )
                } else {
                    LaunchedEffect(snapshot.adhdCalmMode, currentRoute) {
                        if (snapshot.adhdCalmMode &&
                            currentRoute != MainTab.Home.route &&
                            currentRoute != MainTab.Settings.route
                        ) {
                            navController.navigate(MainTab.Home.route) {
                                popUpTo(MainTab.Home.route) { inclusive = false }
                                launchSingleTop = true
                            }
                        }
                    }

                    androidx.compose.foundation.layout.BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val isWide = maxWidth >= 600.dp

                        CoquetteWallpaperBackground(
                            wallpaperKey = profile.wallpaperKey,
                            customWallpaperUri = profile.customWallpaperUri
                        ) {
                            Scaffold(
                                containerColor = androidx.compose.ui.graphics.Color.Transparent,
                                contentWindowInsets = WindowInsets.safeDrawing,
                                snackbarHost = { SnackbarHost(snackbarHostState) },
                                bottomBar = {
                                    if (!isWide && !snapshot.adhdCalmMode) {
                                        NavigationBar(
                                            containerColor = PinkContainer.copy(alpha = 0.96f),
                                            windowInsets = WindowInsets.navigationBars
                                        ) {
                                            bottomTabs.forEach { tab ->
                                                NavigationBarItem(
                                                    selected = currentRoute == tab.route,
                                                    onClick = {
                                                        navController.navigate(tab.route) {
                                                            launchSingleTop = true
                                                            restoreState = true
                                                        }
                                                    },
                                                    icon = { Icon(tab.icon, contentDescription = tab.label) },
                                                    label = { Text(tab.label, maxLines = 1) },
                                                    colors = NavigationBarItemDefaults.colors(
                                                        selectedIconColor = BlushSurface,
                                                        selectedTextColor = BerryInk,
                                                        indicatorColor = PinkPrimary,
                                                        unselectedIconColor = BerryInk,
                                                        unselectedTextColor = BerryInk
                                                    ),
                                                    modifier = Modifier.testTag("nav_${tab.route}")
                                                )
                                            }
                                        }
                                    }
                                }
                            ) { innerPadding ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(innerPadding)
                                ) {
                                    if (isWide && !snapshot.adhdCalmMode) {
                                        NavigationRail(
                                            containerColor = PinkContainer.copy(alpha = 0.92f)
                                        ) {
                                            railTabs.forEach { tab ->
                                                NavigationRailItem(
                                                    selected = currentRoute == tab.route,
                                                    onClick = {
                                                        navController.navigate(tab.route) {
                                                            launchSingleTop = true
                                                            restoreState = true
                                                        }
                                                    },
                                                    icon = { Icon(tab.icon, contentDescription = tab.label) },
                                                    label = { Text(tab.label) },
                                                    colors = NavigationRailItemDefaults.colors(
                                                        selectedIconColor = BlushSurface,
                                                        selectedTextColor = BerryInk,
                                                        indicatorColor = PinkPrimary,
                                                        unselectedIconColor = BerryInk,
                                                        unselectedTextColor = BerryInk
                                                    )
                                                )
                                            }
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxSize(),
                                        contentAlignment = Alignment.TopCenter
                                    ) {
                                        NavHost(
                                            navController = navController,
                                            startDestination = MainTab.Home.route,
                                            modifier = Modifier.widthIn(max = 720.dp)
                                        ) {
                                            composable(MainTab.Home.route) {
                                                HomeScreen(
                                                    snapshot = snapshot,
                                                    nowMillis = nowMillis,
                                                    onToggleScheduleEnabled = mainViewModel::toggleScheduleEnabled,
                                                    onToggleAdhdCalmMode = mainViewModel::setAdhdCalmMode,
                                                    onUpdateBrainDumpNote = mainViewModel::setAdhdBrainDumpNote,
                                                    onStartMicroSprint = mainViewModel::startSession,
                                                    onOpenHyperFocus = { mins -> hyperFocusMinutes = mins },
                                                    onPomodoroCompleted = { wasFocus ->
                                                        mainViewModel.showBanner(
                                                            if (wasFocus) "Pomodoro focus complete! Time for a pink break ♡"
                                                            else "Break over! Ready for next Pomodoro ♡"
                                                        )
                                                    },
                                                    onToggleSmallWin = mainViewModel::toggleSmallWinTask,
                                                    onSetActiveTask = mainViewModel::setActiveFocusTask,
                                                    onGenerateDailyInsight = mainViewModel::generateWeeklyAdhdInsight,
                                                    onOpenChallenges = {
                                                        navController.navigate(MainTab.Challenges.route) {
                                                            launchSingleTop = true
                                                        }
                                                    },
                                                    onOpenScreenSaver = { showScreenSaver = true },
                                                    onOpenEarlyExit = { showEarlyExitSheet = true },
                                                    onOpenScheduleManager = {
                                                        restrictionsInitialTab = 0
                                                        navController.navigate(MainTab.Restrictions.route) {
                                                            launchSingleTop = true
                                                        }
                                                    },
                                                    onOpenDailyLimits = {
                                                        restrictionsInitialTab = 1
                                                        navController.navigate(MainTab.Restrictions.route) {
                                                            launchSingleTop = true
                                                        }
                                                    },
                                                    onNavigateToTab = { route ->
                                                        navController.navigate(route) {
                                                            launchSingleTop = true
                                                        }
                                                    }
                                                )
                                            }
                                            composable(MainTab.Restrictions.route) {
                                                RestrictionsScreen(
                                                    snapshot = snapshot,
                                                    initialTab = restrictionsInitialTab,
                                                    onSaveSchedulePlan = mainViewModel::saveSchedulePlan,
                                                    onToggleScheduleEnabled = mainViewModel::toggleScheduleEnabled,
                                                    onDeleteSchedulePlan = mainViewModel::deleteSchedulePlan,
                                                    onToggleSmallWin = mainViewModel::toggleSmallWinTask,
                                                    onAddSmallWin = { title -> mainViewModel.addSmallWinTask(title) },
                                                    onDeleteSmallWin = mainViewModel::deleteSmallWinTask,
                                                    onUpdateDailyLimit = mainViewModel::updateDailyLimitRule,
                                                    onTempUnblockPlaps = { rule -> mainViewModel.tempUnblockAppWithPlaps(rule, 5) },
                                                    onTestWallBlock = { appName, reason, hint ->
                                                        wallBlockPreview = WallBlockPreviewData(appName, reason, hint)
                                                    }
                                                )
                                            }
                                            composable(MainTab.Challenges.route) {
                                                ChallengesScreen(
                                                    snapshot = snapshot,
                                                    weekKey = mainViewModel.currentWeekKey,
                                                    leaderboardState = leaderboardState,
                                                    isSyncing = isSyncingLeaderboard,
                                                    onSelectChallenge = mainViewModel::selectWeeklyChallenge,
                                                    onSyncScore = { mainViewModel.syncChallengeLeaderboard() },
                                                    onBack = { navController.popBackStack() }
                                                )
                                            }
                                            composable(MainTab.Reports.route) {
                                                ReportsScreen(
                                                    snapshot = snapshot,
                                                    isAnalyzingAdhd = isAnalyzingDailyAdhd,
                                                    adhdInsightError = dailyAdhdError,
                                                    onGenerateAdhdInsight = mainViewModel::generateWeeklyAdhdInsight,
                                                    onStartMicroSprint = mainViewModel::startSession,
                                                    onRecordSampleUsage = mainViewModel::logSampleScreenTime,
                                                    onShowMessage = mainViewModel::showBanner
                                                )
                                            }
                                            composable(MainTab.Ai.route) {
                                                AiStudioScreen(
                                                    snapshot = snapshot,
                                                    aiViewModel = aiViewModel,
                                                    onApplyWallpaperPath = { target, path ->
                                                        when (target) {
                                                            "UI" -> mainViewModel.updatePreferences(
                                                                mapOf("wallpaperKey" to "custom", "customWallpaperUri" to path),
                                                                "Applied AI artwork as UI Wallpaper ♡"
                                                            )
                                                            "AOD" -> mainViewModel.updatePreferences(
                                                                mapOf("aodMediaType" to "PHOTO", "aodMediaUri" to path),
                                                                "Applied AI artwork as Screen Saver ♡"
                                                            )
                                                            else -> mainViewModel.updatePreferences(
                                                                mapOf("wallBlockMediaType" to "PHOTO", "wallBlockMediaUri" to path),
                                                                "Applied AI artwork to Wall Block ♡"
                                                            )
                                                        }
                                                    }
                                                )
                                            }
                                            composable(MainTab.Settings.route) {
                                                SettingsScreen(
                                                    snapshot = snapshot,
                                                    onUpdatePrefs = mainViewModel::updatePreferences,
                                                    onSetWallBlockBgImageUri = mainViewModel::updateWallBlockBgImageUri,
                                                    onToggleAdhdCalmMode = mainViewModel::setAdhdCalmMode,
                                                    onToggleWelcomeSplash = mainViewModel::setWelcomeSplashEnabled,
                                                    onSetWelcomeWallpaper = mainViewModel::setWelcomeSplashWallpaper,
                                                    onSaveWelcomeNote = mainViewModel::setWelcomeSplashNote,
                                                    onSetWelcomeFontKey = mainViewModel::setWelcomeSplashFontKey,
                                                    onSetWelcomeFontColor = mainViewModel::setWelcomeSplashFontColorHex,
                                                    onPreviewWelcomeSplash = { previewWelcomeSplash = true },
                                                    onSaveVoiceLine = mainViewModel::saveRecordedVoiceLine,
                                                    onSelectVoiceLine = mainViewModel::selectVoiceLine,
                                                    onDeleteVoiceLine = mainViewModel::deleteVoiceLine,
                                                    onShowMessage = mainViewModel::showBanner,
                                                    onClaimDailyGoal = mainViewModel::claimDailyGoalStreak,
                                                    onSetAppGloballyAllowed = mainViewModel::setAppGloballyAllowed,
                                                    onAllowAllNecessaryApps = mainViewModel::allowAllNecessaryApps,
                                                    onAddCustomTask = mainViewModel::addCustomTask,
                                                    onDeleteCustomTask = mainViewModel::deleteCustomTask,
                                                    onOpenScreenSaver = { showScreenSaver = true },
                                                    onTestWallBlock = {
                                                        val targetEnd = activeSchedule?.let {
                                                            RestrictionRuntimeState.computeScheduleEndMillis(it, nowMillis)
                                                        } ?: RestrictionRuntimeState.computeNextMidnightMillis(nowMillis)
                                                        wallBlockPreview = WallBlockPreviewData(
                                                            appName = "Sample Restricted App",
                                                            reasonTitle = "Wall Block Preview",
                                                            unlockHint = "Available when restriction ends",
                                                            unlockTargetMillis = targetEnd
                                                        )
                                                    },
                                                    onBackToHome = {
                                                        navController.navigate(MainTab.Home.route) {
                                                            launchSingleTop = true
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (showEarlyExitSheet) {
                        val activeSess = snapshot.activeSession
                        val remMins = when {
                            activeSchedule != null -> {
                                val endMs = RestrictionRuntimeState.computeScheduleEndMillis(activeSchedule, nowMillis)
                                ((endMs - nowMillis) / 60_000L).coerceAtLeast(1L)
                            }
                            activeSess != null -> {
                                ((activeSess.endsAtMillis - nowMillis) / 60_000L).coerceAtLeast(1L)
                            }
                            else -> 60L
                        }

                        EarlyExitBottomSheet(
                            remainingMinutes = remMins,
                            profile = profile,
                            customTasks = snapshot.customTasks,
                            voiceLines = snapshot.voiceLines,
                            selectedVoiceLine = snapshot.selectedVoiceLine,
                            onSelectVoiceLine = mainViewModel::selectVoiceLine,
                            proofResult = proofResult,
                            isVerifyingProof = isVerifyingProof,
                            proofError = proofError,
                            taskProofResult = taskProofResult,
                            isVerifyingTask = isVerifyingTask,
                            taskProofError = taskProofError,
                            onDismiss = { showEarlyExitSheet = false },
                            onVerifyVisualProof = mainViewModel::verifyVisualProofPhoto,
                            onVerifyPhysicalTask = mainViewModel::verifyPhysicalTask,
                            onClearTaskProof = mainViewModel::clearTaskProofState,
                            onUseKeys = mainViewModel::spendKeysOnSession,
                            onPauseWithPlaps = mainViewModel::pauseSessionWithPlaps,
                            onCompleteSequentialTasks = mainViewModel::completeSequentialTasksExit
                        )
                    }
                }
            }
        }
    }
}
