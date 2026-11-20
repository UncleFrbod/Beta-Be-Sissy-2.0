package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.AppRule
import com.example.model.InstalledAppItem
import com.example.model.SchedulePlan
import com.example.service.RestrictionRuntimeState
import com.example.ui.components.CoquetteCard
import com.example.ui.components.SmallWinsChecklistCard
import com.example.ui.theme.BerryInk
import com.example.ui.theme.BlushElevated
import com.example.ui.theme.BlushSurface
import com.example.ui.theme.PinkAccent
import com.example.ui.theme.PinkContainer
import com.example.ui.theme.PinkPrimary
import com.example.ui.theme.RoseGoldBorder
import com.example.viewmodel.MainDashboardSnapshot

private enum class ScheduleAppListFilter(val label: String) {
    ALL("All"),
    RESTRICTED("Restricted"),
    ALLOWED("Allowed"),
    UNSET("Default")
}

@Composable
fun RestrictionsScreen(
    snapshot: MainDashboardSnapshot,
    initialTab: Int = 0,
    onSaveSchedulePlan: (SchedulePlan) -> Unit,
    onToggleScheduleEnabled: (SchedulePlan) -> Unit,
    onDeleteSchedulePlan: (String) -> Unit,
    onToggleSmallWin: (String) -> Unit = {},
    onAddSmallWin: (String) -> Unit = {},
    onDeleteSmallWin: (String) -> Unit = {},
    onUpdateDailyLimit: (InstalledAppItem, AppRule?, Boolean, Long, Long?) -> Unit,
    onTempUnblockPlaps: (AppRule) -> Unit,
    onTestWallBlock: (String, String, String) -> Unit
) {
    var selectedModeTab by remember { mutableIntStateOf(initialTab.coerceIn(0, 1)) }
    LaunchedEffect(initialTab) {
        selectedModeTab = initialTab.coerceIn(0, 1)
    }

    var searchQuery by remember { mutableStateOf("") }
    var editingPlan by remember { mutableStateOf<SchedulePlan?>(null) }

    val rulesByPackage = remember(snapshot.appRules) {
        snapshot.appRules.associateBy { it.packageName }
    }

    val filteredApps = remember(snapshot.installedApps, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) snapshot.installedApps
        else snapshot.installedApps.filter {
            it.appName.lowercase().contains(q) ||
                it.packageName.lowercase().contains(q) ||
                it.category.lowercase().contains(q)
        }
    }

    val filteredSchedules = remember(snapshot.schedules, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) snapshot.schedules
        else snapshot.schedules.filter {
            it.name.lowercase().contains(q) ||
                it.description.lowercase().contains(q)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text = "Schedule & App Limits",
            style = MaterialTheme.typography.headlineMedium,
            color = BerryInk,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))

        TabRow(
            selectedTabIndex = selectedModeTab,
            containerColor = PinkContainer,
            contentColor = BerryInk,
            modifier = Modifier.clip(RoundedCornerShape(16.dp))
        ) {
            Tab(
                selected = selectedModeTab == 0,
                onClick = { selectedModeTab = 0 },
                text = { Text("Schedule Plans", maxLines = 1) },
                modifier = Modifier.testTag("tab_mode_schedule")
            )
            Tab(
                selected = selectedModeTab == 1,
                onClick = { selectedModeTab = 1 },
                text = { Text("Daily Limit", maxLines = 1) },
                modifier = Modifier.testTag("tab_mode_limit")
            )
        }

        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = {
                Text(if (selectedModeTab == 0) "Search schedules..." else "Search installed apps...")
            },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = PinkPrimary) },
            singleLine = true,
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("app_search_input")
        )

        Spacer(Modifier.height(10.dp))

        if (selectedModeTab == 0) {
            Button(
                onClick = {
                    editingPlan = SchedulePlan(
                        name = "Custom Schedule ${snapshot.schedules.size + 1}",
                        description = "My customizable focus plan ♡",
                        enabled = true,
                        startMinutes = 22 * 60L,
                        endMinutes = 0L,
                        daysOfWeek = listOf(1L, 2L, 3L, 4L, 5L, 6L, 7L),
                        modeType = "BLOCK_SELECTED"
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("create_schedule_btn"),
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary, contentColor = BlushSurface)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Create Detailed Schedule Plan", fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item(key = "small_wins_checklist") {
                    SmallWinsChecklistCard(
                        smallWinsState = snapshot.smallWinsState,
                        plapsBalance = snapshot.profile.plapsBalance,
                        onToggleWin = onToggleSmallWin,
                        onAddCustomWin = onAddSmallWin,
                        onDeleteWin = onDeleteSmallWin
                    )
                }

                if (filteredSchedules.isEmpty()) {
                    item(key = "empty_schedules") {
                        CoquetteCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = PinkPrimary,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(Modifier.height(8.dp))
                                Text("No schedule plans yet", style = MaterialTheme.typography.titleMedium, color = BerryInk)
                                Text(
                                    "Tap 'Create Detailed Schedule Plan' to customize times, days, and per-schedule Allowed or Restricted apps.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = BerryInk
                                )
                            }
                        }
                    }
                } else {
                    items(filteredSchedules, key = { it.id.ifBlank { it.name + it.startMinutes } }) { plan ->
                        SchedulePlanCard(
                            plan = plan,
                            onToggleEnabled = { onToggleScheduleEnabled(plan) },
                            onEditPlan = { editingPlan = plan },
                            onDeletePlan = { onDeleteSchedulePlan(plan.id) },
                            onTestWallBlock = {
                                onTestWallBlock(
                                    plan.name,
                                    plan.customBlockReason.ifBlank {
                                        "${plan.name} (${RestrictionRuntimeState.formatMinutes(plan.startMinutes.toInt())} - ${RestrictionRuntimeState.formatMinutes(plan.endMinutes.toInt())})"
                                    },
                                    "Available again at ${RestrictionRuntimeState.formatMinutes(plan.endMinutes.toInt())}"
                                )
                            }
                        )
                    }
                }
            }
        } else {
            if (filteredApps.isEmpty()) {
                CoquetteCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No matching apps found", style = MaterialTheme.typography.titleMedium, color = BerryInk)
                        Text("Try clearing your search filter.", color = BerryInk)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(filteredApps, key = { it.packageName }) { app ->
                        val rule = rulesByPackage[app.packageName]
                        DailyLimitAppRuleCard(
                            app = app,
                            rule = rule,
                            onUpdateLimit = { enabled, limit, used ->
                                onUpdateDailyLimit(app, rule, enabled, limit, used)
                            },
                            onTempUnblock = {
                                if (rule != null) onTempUnblockPlaps(rule)
                            },
                            onTestWallBlock = { limit ->
                                onTestWallBlock(
                                    app.appName,
                                    "Daily Limit Reached (${limit}m / day)",
                                    "Resets at 00:00 or unlock 5m with 5 Plaps"
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    val planBeingEdited = editingPlan
    if (planBeingEdited != null) {
        SchedulePlanEditorSheet(
            initialPlan = planBeingEdited,
            installedApps = snapshot.installedApps,
            onSave = { updated ->
                onSaveSchedulePlan(updated)
                editingPlan = null
            },
            onDismiss = { editingPlan = null }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SchedulePlanCard(
    plan: SchedulePlan,
    onToggleEnabled: () -> Unit,
    onEditPlan: () -> Unit,
    onDeletePlan: () -> Unit,
    onTestWallBlock: () -> Unit
) {
    val now = System.currentTimeMillis()
    val activeNow = remember(plan, now) {
        RestrictionRuntimeState.getCurrentlyActiveSchedules(now, listOf(plan)).isNotEmpty()
    }
    val dayShortLabels = listOf(1L to "M", 2L to "T", 3L to "W", 4L to "T", 5L to "F", 6L to "S", 7L to "S")
    val timeText = "${RestrictionRuntimeState.formatMinutes(plan.startMinutes.toInt())} – ${RestrictionRuntimeState.formatMinutes(plan.endMinutes.toInt())}"

    CoquetteCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(plan.name, style = MaterialTheme.typography.titleMedium, color = BerryInk, fontWeight = FontWeight.Bold)
                        if (activeNow) {
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "ACTIVE",
                                style = MaterialTheme.typography.labelSmall,
                                color = BlushSurface,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(PinkPrimary)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = timeText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = PinkPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Switch(
                    checked = plan.enabled,
                    onCheckedChange = { onToggleEnabled() },
                    colors = SwitchDefaults.colors(checkedThumbColor = BlushSurface, checkedTrackColor = PinkPrimary)
                )
            }

            if (plan.description.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = plan.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = BerryInk
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                dayShortLabels.forEach { (dayNum, label) ->
                    val activeDay = dayNum in plan.daysOfWeek
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(if (activeDay) PinkPrimary else PinkContainer.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (activeDay) BlushSurface else BerryInk
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val modeLabel = if (plan.modeType == "ALLOW_ONLY_SELECTED") "Strict Allow-Only" else "Block Selected"
                SummaryPill(text = modeLabel)
                SummaryPill(text = "${plan.restrictedPackages.size} Restricted")
                SummaryPill(text = "${plan.allowedPackages.size} Allowed")
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onEditPlan,
                    modifier = Modifier.weight(1f).height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary, contentColor = BlushSurface)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Customize Plan & Apps", maxLines = 1)
                }

                OutlinedButton(
                    onClick = onTestWallBlock,
                    modifier = Modifier.height(44.dp)
                ) {
                    Icon(Icons.Default.Block, contentDescription = "Preview Wall Block", modifier = Modifier.size(16.dp))
                }

                IconButton(
                    onClick = onDeletePlan,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete schedule", tint = BerryInk)
                }
            }
        }
    }
}

@Composable
private fun SummaryPill(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = BerryInk,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(PinkContainer.copy(alpha = 0.7f))
            .border(1.dp, RoseGoldBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SchedulePlanEditorSheet(
    initialPlan: SchedulePlan,
    installedApps: List<InstalledAppItem>,
    onSave: (SchedulePlan) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember(initialPlan.id) { mutableStateOf(initialPlan.name) }
    var description by remember(initialPlan.id) { mutableStateOf(initialPlan.description) }
    var enabled by remember(initialPlan.id) { mutableStateOf(initialPlan.enabled) }
    var startMinutes by remember(initialPlan.id) { mutableIntStateOf(initialPlan.startMinutes.toInt()) }
    var endMinutes by remember(initialPlan.id) { mutableIntStateOf(initialPlan.endMinutes.toInt()) }
    var selectedDays by remember(initialPlan.id) { mutableStateOf(initialPlan.daysOfWeek.toSet()) }
    var modeType by remember(initialPlan.id) { mutableStateOf(initialPlan.modeType) }
    var customReason by remember(initialPlan.id) { mutableStateOf(initialPlan.customBlockReason) }
    var allowedSet by remember(initialPlan.id) { mutableStateOf(initialPlan.allowedPackages.toSet()) }
    var restrictedSet by remember(initialPlan.id) { mutableStateOf(initialPlan.restrictedPackages.toSet()) }

    var appSearch by remember { mutableStateOf("") }
    var appFilter by remember { mutableStateOf(ScheduleAppListFilter.ALL) }

    val dayOptions = listOf(1L to "Mon", 2L to "Tue", 3L to "Wed", 4L to "Thu", 5L to "Fri", 6L to "Sat", 7L to "Sun")

    val filteredScheduleApps = remember(installedApps, appSearch, appFilter, allowedSet, restrictedSet) {
        val q = appSearch.trim().lowercase()
        installedApps.filter { app ->
            val matchesQuery = q.isEmpty() ||
                app.appName.lowercase().contains(q) ||
                app.category.lowercase().contains(q)
            val matchesFilter = when (appFilter) {
                ScheduleAppListFilter.ALL -> true
                ScheduleAppListFilter.RESTRICTED -> app.packageName in restrictedSet
                ScheduleAppListFilter.ALLOWED -> app.packageName in allowedSet
                ScheduleAppListFilter.UNSET -> app.packageName !in restrictedSet && app.packageName !in allowedSet
            }
            matchesQuery && matchesFilter
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
                .fillMaxHeight(0.92f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Customize Schedule Plan",
                style = MaterialTheme.typography.titleLarge,
                color = BerryInk,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Schedule Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("schedule_name_input")
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Plan Details & Notes") },
                minLines = 2,
                maxLines = 3,
                modifier = Modifier.fillMaxWidth().testTag("schedule_desc_input")
            )

            // Time Window Steppers (exact hours + 15m steps)
            CoquetteCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Time Window", style = MaterialTheme.typography.titleSmall, color = BerryInk)
                    TimeMinuteStepper(
                        label = "Start Time",
                        totalMinutes = startMinutes,
                        onChange = { startMinutes = it }
                    )
                    TimeMinuteStepper(
                        label = "End Time",
                        totalMinutes = endMinutes,
                        onChange = { endMinutes = it }
                    )
                }
            }

            // Days of Week selector
            Column {
                Text("Active Days", style = MaterialTheme.typography.labelLarge, color = BerryInk)
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    dayOptions.forEach { (dayNum, label) ->
                        val isSelected = dayNum in selectedDays
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedDays = if (isSelected && selectedDays.size > 1) {
                                    selectedDays - dayNum
                                } else {
                                    selectedDays + dayNum
                                }
                            },
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
            }

            // Per-Schedule Mode & Custom Block Reason
            Column {
                Text("Schedule Rule Mode", style = MaterialTheme.typography.labelLarge, color = BerryInk)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = modeType == "BLOCK_SELECTED",
                        onClick = { modeType = "BLOCK_SELECTED" },
                        label = { Text("Block Selected Apps") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PinkPrimary,
                            selectedLabelColor = BlushSurface,
                            containerColor = PinkContainer,
                            labelColor = BerryInk
                        )
                    )
                    FilterChip(
                        selected = modeType == "ALLOW_ONLY_SELECTED",
                        onClick = { modeType = "ALLOW_ONLY_SELECTED" },
                        label = { Text("Allow Only Selected") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PinkPrimary,
                            selectedLabelColor = BlushSurface,
                            containerColor = PinkContainer,
                            labelColor = BerryInk
                        )
                    )
                }
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = customReason,
                    onValueChange = { customReason = it },
                    label = { Text("Custom Wall Block Message (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Individual Per-Schedule Allow / Restrict App List
            CoquetteCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Per-Schedule Apps (${restrictedSet.size} Restricted • ${allowedSet.size} Allowed)",
                        style = MaterialTheme.typography.titleSmall,
                        color = BerryInk,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = appSearch,
                        onValueChange = { appSearch = it },
                        placeholder = { Text("Search apps for this schedule...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PinkPrimary) },
                        singleLine = true,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ScheduleAppListFilter.entries.forEach { f ->
                            FilterChip(
                                selected = appFilter == f,
                                onClick = { appFilter = f },
                                label = { Text(f.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PinkPrimary,
                                    selectedLabelColor = BlushSurface,
                                    containerColor = PinkContainer,
                                    labelColor = BerryInk
                                )
                            )
                        }
                    }

                    filteredScheduleApps.forEach { app ->
                        val isAllowed = app.packageName in allowedSet
                        val isRestricted = app.packageName in restrictedSet

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(BlushElevated)
                                .border(1.dp, RoseGoldBorder, RoundedCornerShape(14.dp))
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(app.appName, style = MaterialTheme.typography.titleSmall, color = BerryInk)
                                Text(app.category, style = MaterialTheme.typography.labelSmall, color = PinkPrimary)
                            }
                            Spacer(Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                PerAppStateChip(
                                    label = "Allow",
                                    selected = isAllowed,
                                    selectedColor = PinkAccent,
                                    selectedTextColor = BerryInk,
                                    onClick = {
                                        allowedSet = allowedSet + app.packageName
                                        restrictedSet = restrictedSet - app.packageName
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                PerAppStateChip(
                                    label = "Default",
                                    selected = !isAllowed && !isRestricted,
                                    selectedColor = PinkContainer,
                                    selectedTextColor = BerryInk,
                                    onClick = {
                                        allowedSet = allowedSet - app.packageName
                                        restrictedSet = restrictedSet - app.packageName
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                PerAppStateChip(
                                    label = "Restrict",
                                    selected = isRestricted,
                                    selectedColor = PinkPrimary,
                                    selectedTextColor = BlushSurface,
                                    onClick = {
                                        restrictedSet = restrictedSet + app.packageName
                                        allowedSet = allowedSet - app.packageName
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(50.dp)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        onSave(
                            initialPlan.copy(
                                name = name.trim().ifBlank { "Custom Schedule" },
                                description = description.trim(),
                                enabled = enabled,
                                startMinutes = startMinutes.toLong(),
                                endMinutes = endMinutes.toLong(),
                                daysOfWeek = selectedDays.sorted(),
                                modeType = modeType,
                                allowedPackages = allowedSet.toList(),
                                restrictedPackages = restrictedSet.toList(),
                                customBlockReason = customReason.trim()
                            )
                        )
                    },
                    modifier = Modifier
                        .weight(1.4f)
                        .height(50.dp)
                        .testTag("save_schedule_plan_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary, contentColor = BlushSurface)
                ) {
                    Text("Save Schedule ♡", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun PerAppStateChip(
    label: String,
    selected: Boolean,
    selectedColor: androidx.compose.ui.graphics.Color,
    selectedTextColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) selectedColor else BlushSurface)
            .border(1.dp, if (selected) selectedTextColor.copy(alpha = 0.4f) else RoseGoldBorder, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) selectedTextColor else BerryInk
        )
    }
}

@Composable
private fun TimeMinuteStepper(
    label: String,
    totalMinutes: Int,
    onChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(PinkContainer.copy(alpha = 0.65f))
            .border(1.dp, RoseGoldBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = BerryInk)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { onChange((totalMinutes + 1440 - 60) % 1440) },
                modifier = Modifier.size(34.dp)
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Minus 1 hour", tint = BerryInk)
            }
            Text(
                text = RestrictionRuntimeState.formatMinutes(totalMinutes),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = BerryInk,
                modifier = Modifier.padding(horizontal = 6.dp)
            )
            IconButton(
                onClick = { onChange((totalMinutes + 15) % 1440) },
                modifier = Modifier.size(34.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Plus 15 minutes", tint = BerryInk)
            }
        }
    }
}

@Composable
private fun DailyLimitAppRuleCard(
    app: InstalledAppItem,
    rule: AppRule?,
    onUpdateLimit: (Boolean, Long, Long?) -> Unit,
    onTempUnblock: () -> Unit,
    onTestWallBlock: (Long) -> Unit
) {
    val enabled = rule?.dailyLimitEnabled ?: false
    val limitMins = rule?.dailyLimitMinutes ?: 60L
    val usedMins = rule?.usedTodayMinutes ?: 0L

    CoquetteCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(app.appName, style = MaterialTheme.typography.titleMedium, color = BerryInk)
                    Text(
                        text = if (enabled) "Daily Limit: ${usedMins}m / ${limitMins}m" else "Unlimited daily use",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PinkPrimary
                    )
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = { onUpdateLimit(it, limitMins, usedMins) },
                    colors = SwitchDefaults.colors(checkedThumbColor = BlushSurface, checkedTrackColor = PinkPrimary)
                )
            }

            if (enabled) {
                Spacer(Modifier.height(6.dp))
                Slider(
                    value = limitMins.toFloat(),
                    onValueChange = { newVal -> onUpdateLimit(true, newVal.toLong(), usedMins) },
                    valueRange = 15f..240f,
                    colors = SliderDefaults.colors(thumbColor = PinkPrimary, activeTrackColor = PinkPrimary)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { onUpdateLimit(true, limitMins, limitMins) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Simulate Limit Hit")
                    }
                    Button(
                        onClick = onTempUnblock,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = PinkAccent, contentColor = BerryInk)
                    ) {
                        Text("5 Plaps Unblock")
                    }
                }
                if (usedMins >= limitMins) {
                    Spacer(Modifier.height(6.dp))
                    Button(
                        onClick = { onTestWallBlock(limitMins) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary, contentColor = BlushSurface)
                    ) {
                        Text("Open Wall Block (Limit Reached)")
                    }
                }
            }
        }
    }
}
