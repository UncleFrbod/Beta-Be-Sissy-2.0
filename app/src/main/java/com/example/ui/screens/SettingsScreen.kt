package com.example.ui.screens

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.auth.signOut
import com.example.model.AppRule
import com.example.model.InstalledAppItem
import com.example.model.RecordedVoiceLine
import com.example.service.BeSissyDeviceAdminReceiver
import com.example.ui.components.CoquetteCard
import com.example.ui.components.VoiceLineRecorderCard
import com.example.ui.components.WeeklyStreakProfileCard
import com.example.ui.components.WelcomeSplashCustomizationCard
import com.example.ui.theme.BerryInk
import com.example.ui.theme.BlushElevated
import com.example.ui.theme.BlushSurface
import com.example.ui.theme.BuiltInFontPresets
import com.example.ui.theme.FontColorPresets
import com.example.ui.theme.PinkAccent
import com.example.ui.theme.PinkContainer
import com.example.ui.theme.PinkPrimary
import com.example.ui.theme.RoseGoldBorder
import com.example.viewmodel.MainDashboardSnapshot
import java.io.File
import java.io.FileOutputStream

private enum class AllowFilterMode(val label: String) {
    FAST_PICK("Fast Pick"),
    MOST_USED("Most Used"),
    NECESSARY("Necessary"),
    ALL("All Apps")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    snapshot: MainDashboardSnapshot,
    onUpdatePrefs: (Map<String, Any>, String) -> Unit,
    onSetWallBlockBgImageUri: (String) -> Unit,
    onToggleAdhdCalmMode: (Boolean) -> Unit = {},
    onToggleWelcomeSplash: (Boolean) -> Unit = {},
    onSetWelcomeWallpaper: (String, String?) -> Unit = { _, _ -> },
    onSaveWelcomeNote: (String) -> Unit = {},
    onSetWelcomeFontKey: (String, String?) -> Unit = { _, _ -> },
    onSetWelcomeFontColor: (String) -> Unit = {},
    onPreviewWelcomeSplash: () -> Unit = {},
    onSaveVoiceLine: (String, String, Int) -> Unit = { _, _, _ -> },
    onSelectVoiceLine: (String) -> Unit = {},
    onDeleteVoiceLine: (RecordedVoiceLine) -> Unit = {},
    onShowMessage: (String) -> Unit = {},
    onClaimDailyGoal: () -> Unit,
    onSetAppGloballyAllowed: (InstalledAppItem, AppRule?, Boolean) -> Unit,
    onAllowAllNecessaryApps: () -> Unit,
    onAddCustomTask: (String, String) -> Unit,
    onDeleteCustomTask: (String) -> Unit,
    onOpenScreenSaver: () -> Unit,
    onTestWallBlock: () -> Unit,
    onBackToHome: () -> Unit = {}
) {
    if (snapshot.adhdCalmMode) {
        BackHandler { onBackToHome() }
    }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    val profile = snapshot.profile

    var showAllowAppsSheet by remember { mutableStateOf(false) }
    var wallReason by remember(profile.wallBlockReason) { mutableStateOf(profile.wallBlockReason) }
    var proofDesc by remember(profile.visualProofDescription) { mutableStateOf(profile.visualProofDescription) }
    var newTaskContent by remember { mutableStateOf("") }
    var newTaskType by remember { mutableStateOf("READ_LINE") }

    val rulesByPackage = remember(snapshot.appRules) {
        snapshot.appRules.associateBy { it.packageName }
    }

    val usageByPackage = remember(snapshot.usageLogs, snapshot.appRules) {
        val map = mutableMapOf<String, Long>()
        snapshot.usageLogs.forEach { entry ->
            map[entry.packageName] = (map[entry.packageName] ?: 0L) + entry.screenTimeMinutes
        }
        snapshot.appRules.forEach { rule ->
            if (rule.usedTodayMinutes > (map[rule.packageName] ?: 0L)) {
                map[rule.packageName] = rule.usedTodayMinutes
            }
        }
        map
    }

    val allowedCount = remember(snapshot.installedApps, rulesByPackage) {
        snapshot.installedApps.count { app ->
            rulesByPackage[app.packageName]?.necessaryApp == true
        }
    }

    val fastSuggestedApps = remember(snapshot.installedApps, usageByPackage) {
        snapshot.installedApps
            .sortedWith(
                compareByDescending<InstalledAppItem> { usageByPackage[it.packageName] ?: 0L }
                    .thenByDescending { it.isSystemEssential }
                    .thenBy { it.appName.lowercase() }
            )
            .take(6)
    }

    // Custom Font Picker from Device Storage (.ttf / .otf)
    val fontFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val dest = File(context.filesDir, "custom_user_font.ttf")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(dest).use { out -> input.copyTo(out) }
                }
                onUpdatePrefs(
                    mapOf(
                        "selectedFontKey" to "custom_file",
                        "customFontUri" to dest.absolutePath
                    ),
                    "Custom font imported from storage ♡"
                )
            } catch (_: Exception) {
            }
        }
    }

    // Photo Picker for UI Wallpaper
    val uiWallpaperPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val savedPath = copyMediaToFiles(context, uri, "ui_wallpaper_custom")
            if (savedPath != null) {
                onUpdatePrefs(
                    mapOf("wallpaperKey" to "custom", "customWallpaperUri" to savedPath),
                    "Custom UI wallpaper saved ♡"
                )
            }
        }
    }

    // Photo/Video Picker for Screen Saver / AOD
    val aodMediaPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val mime = context.contentResolver.getType(uri).orEmpty()
            val mediaType = when {
                mime.contains("video") -> "VIDEO"
                mime.contains("gif") -> "GIF"
                else -> "PHOTO"
            }
            val savedPath = copyMediaToFiles(context, uri, "aod_media_custom") ?: uri.toString()
            onUpdatePrefs(
                mapOf("aodMediaUri" to savedPath, "aodMediaType" to mediaType),
                "Screen Saver media updated ($mediaType) ♡"
            )
        }
    }

    // Photo/Video Picker for Wall Block
    val wallMediaPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val mime = context.contentResolver.getType(uri).orEmpty()
            val mediaType = when {
                mime.contains("video") -> "VIDEO"
                mime.contains("gif") -> "GIF"
                else -> "PHOTO"
            }
            val savedPath = copyMediaToFiles(context, uri, "wallblock_media_custom") ?: uri.toString()
            onUpdatePrefs(
                mapOf("wallBlockMediaUri" to savedPath, "wallBlockMediaType" to mediaType),
                "Wall Block media updated ($mediaType) ♡"
            )
        }
    }

    // Fullscreen Background Image Picker for Wall Block
    val wallBgImagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val savedPath = copyMediaToFiles(context, uri, "wallblock_bg_custom") ?: uri.toString()
            onSetWallBlockBgImageUri(savedPath)
        }
    }

    // Gallery Photo Picker for 3-second Welcome Splash Page
    val welcomeWallpaperPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val savedPath = copyMediaToFiles(context, uri, "welcome_bg_custom") ?: uri.toString()
            onSetWelcomeWallpaper("custom", savedPath)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (snapshot.adhdCalmMode) {
            Button(
                onClick = onBackToHome,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PinkPrimary,
                    contentColor = BlushSurface
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("settings_back_to_focus_btn")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Back to ADHD Focus Dashboard", fontWeight = FontWeight.Bold)
            }
        }

        Text(
            text = "Profile & Settings",
            style = MaterialTheme.typography.headlineMedium,
            color = BerryInk,
            fontWeight = FontWeight.Bold
        )

        // Profile & Visual Weekly Streak Counter
        WeeklyStreakProfileCard(
            profile = profile,
            appRules = snapshot.appRules,
            onClaimDailyGoal = onClaimDailyGoal
        )

        // Global ADHD Friendly Mode Toggle
        CoquetteCard(modifier = Modifier.fillMaxWidth(), containerColor = BlushElevated) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Psychology, contentDescription = null, tint = PinkPrimary)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "ADHD Friendly Mode",
                            style = MaterialTheme.typography.titleMedium,
                            color = BerryInk,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Simplifies main dashboard to ONLY your current active task and visual Pomodoro timer, hiding secondary navigation.",
                            style = MaterialTheme.typography.bodySmall,
                            color = PinkPrimary
                        )
                    }
                }
                Switch(
                    checked = snapshot.adhdCalmMode,
                    onCheckedChange = onToggleAdhdCalmMode,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = BlushSurface,
                        checkedTrackColor = PinkPrimary
                    ),
                    modifier = Modifier.testTag("settings_adhd_friendly_toggle")
                )
            }
        }

        // Custom Microphone Feminine Voice Lines Studio for End Session Tasks
        VoiceLineRecorderCard(
            voiceLines = snapshot.voiceLines,
            selectedVoiceLineId = snapshot.selectedVoiceLineId,
            onSaveRecording = onSaveVoiceLine,
            onSelectVoiceLine = onSelectVoiceLine,
            onDeleteVoiceLine = onDeleteVoiceLine,
            onShowMessage = onShowMessage
        )

        // Post-Sign-In 3-Second Welcome Page Customization
        WelcomeSplashCustomizationCard(
            config = snapshot.welcomeSplashConfig,
            customFontAvailable = profile.customFontUri.isNotBlank() || snapshot.welcomeSplashConfig.customFontUri.isNotBlank(),
            onToggleEnabled = onToggleWelcomeSplash,
            onSelectWallpaper = { key -> onSetWelcomeWallpaper(key, null) },
            onPickGalleryWallpaper = {
                welcomeWallpaperPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onSaveNote = onSaveWelcomeNote,
            onSelectFontKey = { key ->
                val customUri = if (key == "custom_file") profile.customFontUri else null
                onSetWelcomeFontKey(key, customUri)
            },
            onImportCustomFont = {
                fontFileLauncher.launch(arrayOf("font/ttf", "font/otf", "application/x-font-ttf", "*/*"))
            },
            onSelectFontColorHex = onSetWelcomeFontColor,
            onPreviewSplash = onPreviewWelcomeSplash
        )

        // 0. Allow Apps Quick Whitelist & Menu (Most Used + Necessary Apps)
        CoquetteCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = PinkPrimary)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                "Allow Apps (Fast Menu)",
                                style = MaterialTheme.typography.titleMedium,
                                color = BerryInk
                            )
                            Text(
                                "$allowedCount allowed globally • Most Used & Necessary apps",
                                style = MaterialTheme.typography.bodySmall,
                                color = PinkPrimary
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                Text(
                    "Quickly allow your most used and necessary apps:",
                    style = MaterialTheme.typography.labelMedium,
                    color = BerryInk
                )
                Spacer(Modifier.height(6.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    fastSuggestedApps.forEach { app ->
                        val rule = rulesByPackage[app.packageName]
                        val isAllowed = rule?.necessaryApp == true
                        val usedMins = usageByPackage[app.packageName] ?: 0L
                        val badge = when {
                            usedMins > 0L -> " (${usedMins}m)"
                            app.isSystemEssential -> " ★"
                            else -> ""
                        }
                        FilterChip(
                            selected = isAllowed,
                            onClick = { onSetAppGloballyAllowed(app, rule, !isAllowed) },
                            label = { Text("${app.appName}$badge", maxLines = 1) },
                            leadingIcon = if (isAllowed) {
                                { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PinkPrimary,
                                selectedLabelColor = BlushSurface,
                                selectedLeadingIconColor = BlushSurface,
                                containerColor = PinkContainer,
                                labelColor = BerryInk
                            )
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onAllowAllNecessaryApps,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("allow_all_necessary_btn")
                    ) {
                        Text("Allow Necessary", maxLines = 1)
                    }
                    Button(
                        onClick = { showAllowAppsSheet = true },
                        modifier = Modifier
                            .weight(1.2f)
                            .height(48.dp)
                            .testTag("open_allow_apps_menu_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PinkPrimary,
                            contentColor = BlushSurface
                        )
                    ) {
                        Text("Allow App Menu", fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            }
        }

        // 1. Theme, Font & Colors
        CoquetteCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Palette, contentDescription = null, tint = PinkPrimary)
                    Spacer(Modifier.width(8.dp))
                    Text("Theme, Fonts & Ink Colors", style = MaterialTheme.typography.titleMedium, color = BerryInk)
                }
                Spacer(Modifier.height(10.dp))

                Text("App Wallpaper", style = MaterialTheme.typography.labelLarge, color = BerryInk)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("coquette_silk" to "Coquette Silk", "neon_candy" to "Neon Candy", "blush" to "Blush Pearl").forEach { (k, label) ->
                        FilterChip(
                            selected = profile.wallpaperKey == k,
                            onClick = { onUpdatePrefs(mapOf("wallpaperKey" to k), "Wallpaper updated") },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PinkPrimary,
                                selectedLabelColor = BlushSurface,
                                containerColor = PinkContainer,
                                labelColor = BerryInk
                            )
                        )
                    }
                }
                OutlinedButton(
                    onClick = {
                        uiWallpaperPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.fillMaxWidth().testTag("pick_ui_wallpaper_btn")
                ) {
                    Text("Choose Gallery Photo / GIF Wallpaper")
                }

                Spacer(Modifier.height(12.dp))
                Text("Typography Font", style = MaterialTheme.typography.labelLarge, color = BerryInk)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    BuiltInFontPresets.forEach { preset ->
                        FilterChip(
                            selected = profile.selectedFontKey == preset.key,
                            onClick = {
                                if (preset.key == "custom_file") {
                                    fontFileLauncher.launch(arrayOf("font/ttf", "font/otf", "application/x-font-ttf", "*/*"))
                                } else {
                                    onUpdatePrefs(mapOf("selectedFontKey" to preset.key), "Font set to ${preset.label}")
                                }
                            },
                            label = { Text(preset.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PinkPrimary,
                                selectedLabelColor = BlushSurface,
                                containerColor = PinkContainer,
                                labelColor = BerryInk
                            )
                        )
                    }
                }
                OutlinedButton(
                    onClick = { fontFileLauncher.launch(arrayOf("*/*")) },
                    modifier = Modifier.fillMaxWidth().testTag("import_font_btn")
                ) {
                    Icon(Icons.Default.FontDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Import .ttf / .otf Font from Storage")
                }

                Spacer(Modifier.height(12.dp))
                Text("Font Color", style = MaterialTheme.typography.labelLarge, color = BerryInk)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FontColorPresets.forEach { swatch ->
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(swatch.color)
                                .border(
                                    width = if (profile.selectedFontColorHex == swatch.hex) 3.dp else 1.dp,
                                    color = if (profile.selectedFontColorHex == swatch.hex) PinkPrimary else RoseGoldBorder,
                                    shape = CircleShape
                                )
                                .clickable {
                                    onUpdatePrefs(mapOf("selectedFontColorHex" to swatch.hex), "Font color: ${swatch.name}")
                                }
                        )
                    }
                }
            }
        }

        // 2. Always-On Display / Screen Saver Studio
        CoquetteCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Bedtime, contentDescription = null, tint = PinkPrimary)
                    Spacer(Modifier.width(8.dp))
                    Text("Always-On Display & Screen Saver", style = MaterialTheme.typography.titleMedium, color = BerryInk)
                }
                Spacer(Modifier.height(8.dp))
                Text("Dim Opacity: ${profile.aodDimPercent}%", color = BerryInk)
                Slider(
                    value = profile.aodDimPercent.toFloat(),
                    onValueChange = { onUpdatePrefs(mapOf("aodDimPercent" to it.toLong()), "Dim updated") },
                    valueRange = 15f..90f,
                    colors = SliderDefaults.colors(thumbColor = PinkPrimary, activeTrackColor = PinkPrimary)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            aodMediaPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Pick Photo/GIF/Video")
                    }
                    Button(
                        onClick = onOpenScreenSaver,
                        modifier = Modifier.weight(1f).testTag("settings_launch_aod_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary, contentColor = BlushSurface)
                    ) {
                        Text("Preview AOD")
                    }
                }
            }
        }

        // 3. Wall Block Customization Studio
        CoquetteCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Block, contentDescription = null, tint = PinkPrimary)
                    Spacer(Modifier.width(8.dp))
                    Text("Wall Block Studio", style = MaterialTheme.typography.titleMedium, color = BerryInk)
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = wallReason,
                    onValueChange = { wallReason = it },
                    label = { Text("Reason shown on Wall Block") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("ICON", "PHOTO", "GIF", "VIDEO").forEach { type ->
                        FilterChip(
                            selected = profile.wallBlockMediaType == type,
                            onClick = { onUpdatePrefs(mapOf("wallBlockMediaType" to type), "Wall Block media: $type") },
                            label = { Text(type) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PinkPrimary,
                                selectedLabelColor = BlushSurface,
                                containerColor = PinkContainer,
                                labelColor = BerryInk
                            )
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            wallMediaPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Pick Media")
                    }
                    Button(
                        onClick = {
                            onUpdatePrefs(mapOf("wallBlockReason" to wallReason.take(300)), "Wall Block saved")
                            onTestWallBlock()
                        },
                        modifier = Modifier.weight(1f).testTag("test_wall_block_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary, contentColor = BlushSurface)
                    ) {
                        Text("Save & Test")
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            wallBgImagePicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("pick_wallblock_bg_btn")
                    ) {
                        Text("Fullscreen Bg Image")
                    }
                    if (snapshot.wallBlockBgImageUri.isNotBlank()) {
                        OutlinedButton(
                            onClick = { onSetWallBlockBgImageUri("") },
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text("Clear Bg")
                        }
                    }
                }
            }
        }

        // 4. Visual Proof Description + Custom Tasks Editor
        CoquetteCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Visibility, contentDescription = null, tint = PinkPrimary)
                    Spacer(Modifier.width(8.dp))
                    Text("AI Visual Proof & Early Exit Tasks", style = MaterialTheme.typography.titleMedium, color = BerryInk)
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = proofDesc,
                    onValueChange = { proofDesc = it },
                    label = { Text("Visual proof criteria for AI camera scan") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                Button(
                    onClick = { onUpdatePrefs(mapOf("visualProofDescription" to proofDesc.take(400)), "Visual proof updated") },
                    colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary, contentColor = BlushSurface)
                ) {
                    Text("Save Proof Rule")
                }

                Spacer(Modifier.height(12.dp))
                Text("Add Custom Reading Line or Typing Paragraph", style = MaterialTheme.typography.labelLarge, color = BerryInk)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = newTaskType == "READ_LINE",
                        onClick = { newTaskType = "READ_LINE" },
                        label = { Text("Read Aloud Line") }
                    )
                    FilterChip(
                        selected = newTaskType == "TYPING_PROMPT",
                        onClick = { newTaskType = "TYPING_PROMPT" },
                        label = { Text("Typing Prompt") }
                    )
                }
                OutlinedTextField(
                    value = newTaskContent,
                    onValueChange = { newTaskContent = it },
                    label = { Text("Enter custom line or paragraph...") },
                    modifier = Modifier.fillMaxWidth().testTag("custom_task_input")
                )
                Spacer(Modifier.height(6.dp))
                OutlinedButton(
                    onClick = {
                        if (newTaskContent.isNotBlank()) {
                            onAddCustomTask(newTaskType, newTaskContent)
                            newTaskContent = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("add_custom_task_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Add Custom Task")
                }

                Spacer(Modifier.height(8.dp))
                snapshot.customTasks.forEach { task ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(PinkContainer.copy(alpha = 0.55f))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(task.taskType, style = MaterialTheme.typography.labelSmall, color = PinkPrimary)
                            Text(task.content, style = MaterialTheme.typography.bodyMedium, color = BerryInk, maxLines = 2)
                        }
                        IconButton(onClick = { onDeleteCustomTask(task.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete task", tint = BerryInk)
                        }
                    }
                }
            }
        }

        // 5. Device Admin & System Permissions Center
        CoquetteCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = PinkPrimary)
                    Spacer(Modifier.width(8.dp))
                    Text("Admin & Tamper Protection", style = MaterialTheme.typography.titleMedium, color = BerryInk)
                }
                Text(
                    "Grant Device Admin, Accessibility, Overlay, and Usage Access to block Force Stop, Clear Data, and Uninstall during active schedules.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = BerryInk
                )

                Button(
                    onClick = {
                        val comp = ComponentName(context, BeSissyDeviceAdminReceiver::class.java)
                        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, comp)
                            putExtra(
                                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                                "Prevents uninstalling Be Sissy while a schedule is active."
                            )
                        }
                        try { context.startActivity(intent) } catch (_: Exception) {}
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary, contentColor = BlushSurface)
                ) {
                    Icon(Icons.Default.AdminPanelSettings, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Activate Device Admin")
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            try { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) } catch (_: Exception) {}
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Accessibility")
                    }
                    OutlinedButton(
                        onClick = {
                            try { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) } catch (_: Exception) {}
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Usage Stats")
                    }
                }

                OutlinedButton(
                    onClick = {
                        try {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                            )
                        } catch (_: Exception) {}
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Display Over Other Apps")
                }

                Spacer(Modifier.height(6.dp))
                Button(
                    onClick = { signOut(credentialManager, scope) },
                    modifier = Modifier.fillMaxWidth().testTag("sign_out_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = PinkAccent, contentColor = BerryInk)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Sign Out (${profile.displayName})", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(20.dp))
    }

    if (showAllowAppsSheet) {
        AllowAppsMenuSheet(
            apps = snapshot.installedApps,
            rulesByPackage = rulesByPackage,
            usageByPackage = usageByPackage,
            onToggleAllow = onSetAppGloballyAllowed,
            onAllowAllNecessary = onAllowAllNecessaryApps,
            onDismiss = { showAllowAppsSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AllowAppsMenuSheet(
    apps: List<InstalledAppItem>,
    rulesByPackage: Map<String, AppRule>,
    usageByPackage: Map<String, Long>,
    onToggleAllow: (InstalledAppItem, AppRule?, Boolean) -> Unit,
    onAllowAllNecessary: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }
    var filterMode by remember { mutableStateOf(AllowFilterMode.FAST_PICK) }

    val displayedApps = remember(apps, searchQuery, filterMode, usageByPackage) {
        val q = searchQuery.trim().lowercase()
        val searched = if (q.isEmpty()) apps else apps.filter {
            it.appName.lowercase().contains(q) ||
                it.packageName.lowercase().contains(q) ||
                it.category.lowercase().contains(q)
        }
        when (filterMode) {
            AllowFilterMode.FAST_PICK -> searched.sortedWith(
                compareByDescending<InstalledAppItem> { (usageByPackage[it.packageName] ?: 0L) > 0L || it.isSystemEssential }
                    .thenByDescending { usageByPackage[it.packageName] ?: 0L }
                    .thenByDescending { it.isSystemEssential }
                    .thenBy { it.appName.lowercase() }
            )
            AllowFilterMode.MOST_USED -> searched.sortedWith(
                compareByDescending<InstalledAppItem> { usageByPackage[it.packageName] ?: 0L }
                    .thenBy { it.appName.lowercase() }
            )
            AllowFilterMode.NECESSARY -> searched.filter { it.isSystemEssential }
            AllowFilterMode.ALL -> searched.sortedBy { it.appName.lowercase() }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = BlushSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Allow Apps Menu",
                        style = MaterialTheme.typography.titleLarge,
                        color = BerryInk,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Fast-allow your most used & necessary apps",
                        style = MaterialTheme.typography.bodySmall,
                        color = PinkPrimary
                    )
                }
                OutlinedButton(
                    onClick = onAllowAllNecessary,
                    modifier = Modifier.height(42.dp)
                ) {
                    Text("Allow Necessary", maxLines = 1)
                }
            }

            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search apps to allow...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = PinkPrimary) },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("allow_menu_search_input")
            )

            Spacer(Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AllowFilterMode.entries.forEach { mode ->
                    FilterChip(
                        selected = filterMode == mode,
                        onClick = { filterMode = mode },
                        label = { Text(mode.label, maxLines = 1) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PinkPrimary,
                            selectedLabelColor = BlushSurface,
                            containerColor = PinkContainer,
                            labelColor = BerryInk
                        )
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(displayedApps, key = { it.packageName }) { app ->
                    val rule = rulesByPackage[app.packageName]
                    val isAllowed = rule?.necessaryApp == true
                    val usedMins = usageByPackage[app.packageName] ?: 0L

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (isAllowed) PinkContainer.copy(alpha = 0.72f) else BlushElevated)
                            .border(1.dp, RoseGoldBorder, RoundedCornerShape(18.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = app.appName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = BerryInk,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (app.isSystemEssential) {
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "Necessary",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = BlushSurface,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(PinkPrimary)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            val detailText = buildString {
                                append(app.category)
                                if (usedMins > 0L) append(" • ${usedMins}m used today")
                            }
                            Text(
                                text = detailText,
                                style = MaterialTheme.typography.bodySmall,
                                color = PinkPrimary
                            )
                        }

                        Switch(
                            checked = isAllowed,
                            onCheckedChange = { checked -> onToggleAllow(app, rule, checked) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = BlushSurface,
                                checkedTrackColor = PinkPrimary
                            )
                        )
                    }
                }
            }
        }
    }
}

private fun copyMediaToFiles(context: Context, uri: Uri, baseName: String): String? {
    return try {
        val ext = context.contentResolver.getType(uri)?.substringAfter("/")?.take(5) ?: "bin"
        val dest = File(context.filesDir, "$baseName.$ext")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(dest).use { out -> input.copyTo(out) }
        }
        dest.absolutePath
    } catch (_: Exception) {
        null
    }
}
