package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ChatModelTier
import com.example.data.ImageAspectOption
import com.example.data.ImageGenModelOption
import com.example.data.ImageSizeOption
import com.example.model.AiScheduleSuggestion
import com.example.model.UiState
import com.example.service.RestrictionRuntimeState
import com.example.ui.components.CoquetteCard
import com.example.ui.theme.BerryInk
import com.example.ui.theme.BlushElevated
import com.example.ui.theme.BlushSurface
import com.example.ui.theme.PinkAccent
import com.example.ui.theme.PinkContainer
import com.example.ui.theme.PinkPrimary
import com.example.ui.theme.RoseGoldBorder
import com.example.viewmodel.AiAnalysisUiState
import com.example.viewmodel.AiViewModel
import com.example.viewmodel.ImageGenUiState
import com.example.viewmodel.MainDashboardSnapshot

@Composable
fun AiStudioScreen(
    snapshot: MainDashboardSnapshot,
    aiViewModel: AiViewModel,
    onApplyWallpaperPath: (String, String) -> Unit
) {
    var selectedAiTab by remember { mutableIntStateOf(0) } // 0=Behavior, 1=Chat, 2=Art Studio

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text = "Be Sissy AI Studio",
            style = MaterialTheme.typography.headlineMedium,
            color = BerryInk,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))

        TabRow(
            selectedTabIndex = selectedAiTab,
            containerColor = PinkContainer,
            contentColor = BerryInk,
            modifier = Modifier.clip(RoundedCornerShape(16.dp))
        ) {
            Tab(
                selected = selectedAiTab == 0,
                onClick = { selectedAiTab = 0 },
                text = { Text("Behavior AI", maxLines = 1) },
                modifier = Modifier.testTag("ai_tab_behavior")
            )
            Tab(
                selected = selectedAiTab == 1,
                onClick = { selectedAiTab = 1 },
                text = { Text("Coach Chat", maxLines = 1) },
                modifier = Modifier.testTag("ai_tab_chat")
            )
            Tab(
                selected = selectedAiTab == 2,
                onClick = { selectedAiTab = 2 },
                text = { Text("Art Studio", maxLines = 1) },
                modifier = Modifier.testTag("ai_tab_art")
            )
        }

        Spacer(Modifier.height(10.dp))

        when (selectedAiTab) {
            0 -> BehaviorCoachTab(snapshot = snapshot, aiViewModel = aiViewModel)
            1 -> FocusChatTab(aiViewModel = aiViewModel)
            2 -> PinkArtStudioTab(aiViewModel = aiViewModel, onApplyWallpaperPath = onApplyWallpaperPath)
        }
    }
}

@Composable
private fun BehaviorCoachTab(
    snapshot: MainDashboardSnapshot,
    aiViewModel: AiViewModel
) {
    val analysisState by aiViewModel.analysisState.collectAsStateWithLifecycle()
    val suggestionsState by aiViewModel.suggestionsState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CoquetteCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Psychology, contentDescription = null, tint = PinkPrimary)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Deep Screen Behavior Analyzer",
                        style = MaterialTheme.typography.titleMedium,
                        color = BerryInk
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Uses Gemini 4 Carbon (High Thinking) to inspect unusual screen time spikes and craft editable restriction schedules.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = BerryInk
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        aiViewModel.analyzeScreenBehavior(snapshot.usageLogs, snapshot.appRules)
                    },
                    enabled = analysisState !is AiAnalysisUiState.Loading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("analyze_behavior_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary, contentColor = BlushSurface)
                ) {
                    if (analysisState is AiAnalysisUiState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = BlushSurface, strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Deep Thinking Analysis...")
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Analyze Behavior & Suggest Plans")
                    }
                }

                when (val s = analysisState) {
                    is AiAnalysisUiState.Success -> {
                        Spacer(Modifier.height(10.dp))
                        Text(s.summary, style = MaterialTheme.typography.bodyLarge, color = PinkPrimary)
                    }
                    is AiAnalysisUiState.Error -> {
                        Spacer(Modifier.height(10.dp))
                        Text(s.message, color = MaterialTheme.colorScheme.error)
                    }
                    else -> Unit
                }
            }
        }

        when (val state = suggestionsState) {
            is UiState.Loading -> {
                CircularProgressIndicator(color = PinkPrimary)
            }
            is UiState.Empty -> {
                CoquetteCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Tap Analyze above to generate editable schedule recommendations.",
                        modifier = Modifier.padding(16.dp),
                        color = BerryInk
                    )
                }
            }
            is UiState.Error -> {
                CoquetteCard(modifier = Modifier.fillMaxWidth()) {
                    Text(state.message, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.error)
                }
            }
            is UiState.Success -> {
                state.data.forEach { sug ->
                    EditableSuggestionCard(
                        suggestion = sug,
                        onApply = { start, end, limit ->
                            aiViewModel.applyEditableSuggestion(sug, start, end, limit)
                        },
                        onDismiss = { aiViewModel.dismissSuggestion(sug) }
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun EditableSuggestionCard(
    suggestion: AiScheduleSuggestion,
    onApply: (Long, Long, Long) -> Unit,
    onDismiss: () -> Unit
) {
    var startHour by remember(suggestion.id) { mutableIntStateOf((suggestion.suggestedStartMinutes / 60L).toInt()) }
    var endHour by remember(suggestion.id) { mutableIntStateOf((suggestion.suggestedEndMinutes / 60L).toInt()) }
    var dailyLimit by remember(suggestion.id) { mutableLongStateOf(suggestion.suggestedDailyLimitMinutes) }

    CoquetteCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(suggestion.appName, style = MaterialTheme.typography.titleMedium, color = BerryInk)
                Text(
                    suggestion.status,
                    style = MaterialTheme.typography.labelSmall,
                    color = PinkPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(suggestion.reason, style = MaterialTheme.typography.bodyMedium, color = BerryInk)
            Spacer(Modifier.height(10.dp))

            // Editable start/end hours and daily limit
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { startHour = (startHour + 23) % 24 }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Remove, contentDescription = "Start -1h")
                    }
                    Text("Start ${RestrictionRuntimeState.formatMinutes(startHour * 60)}", color = BerryInk)
                    IconButton(onClick = { startHour = (startHour + 1) % 24 }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Add, contentDescription = "Start +1h")
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { endHour = (endHour + 23) % 24 }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Remove, contentDescription = "End -1h")
                    }
                    Text("End ${RestrictionRuntimeState.formatMinutes(endHour * 60)}", color = BerryInk)
                    IconButton(onClick = { endHour = (endHour + 1) % 24 }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Add, contentDescription = "End +1h")
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Daily Cap: ${dailyLimit}m", style = MaterialTheme.typography.labelLarge, color = BerryInk)
                Row {
                    IconButton(onClick = { dailyLimit = (dailyLimit - 15L).coerceAtLeast(15L) }) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease limit")
                    }
                    IconButton(onClick = { dailyLimit = (dailyLimit + 15L).coerceAtMost(360L) }) {
                        Icon(Icons.Default.Add, contentDescription = "Increase limit")
                    }
                }
            }

            if (suggestion.status == "PENDING") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Dismiss")
                    }
                    Button(
                        onClick = { onApply(startHour * 60L, endHour * 60L, dailyLimit) },
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary, contentColor = BlushSurface)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Apply Plan")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FocusChatTab(aiViewModel: AiViewModel) {
    val messages by aiViewModel.chatMessages.collectAsStateWithLifecycle()
    val selectedTier by aiViewModel.selectedChatTier.collectAsStateWithLifecycle()
    val isStreaming by aiViewModel.isChatStreaming.collectAsStateWithLifecycle()
    val chatError by aiViewModel.chatError.collectAsStateWithLifecycle()
    var inputText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ChatModelTier.entries.forEach { tier ->
                FilterChip(
                    selected = selectedTier == tier,
                    onClick = { aiViewModel.setChatTier(tier) },
                    label = { Text(tier.name.lowercase().replace('_', ' '), maxLines = 1) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PinkPrimary,
                        selectedLabelColor = BlushSurface,
                        containerColor = PinkContainer,
                        labelColor = BerryInk
                    )
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 6.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                val isUser = msg.role == "user"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(0.86f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (isUser) PinkPrimary else BlushElevated)
                            .border(1.dp, RoseGoldBorder, RoundedCornerShape(18.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = msg.text.ifBlank { "..." },
                            color = if (isUser) BlushSurface else BerryInk,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        if (msg.groundingSources.isNotEmpty()) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "Sources: ${msg.groundingSources.take(3).joinToString(" • ")}",
                                style = MaterialTheme.typography.labelSmall,
                                color = PinkPrimary
                            )
                        }
                    }
                }
            }
        }

        if (chatError != null) {
            Text(chatError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(top = 4.dp)
        ) {
            listOf(
                "Stuck in ADHD paralysis" to "I have severe ADHD and feel stuck in task paralysis right now. Give me a gentle 3-step micro-plan in a cute Sissy encouraging tone!",
                "10m dopamine sprint" to "Design a fun 10-minute dopamine sprint for my severe ADHD so I can start working right now without feeling overwhelmed.",
                "Stop doomscrolling" to "Help me break out of an ADHD evening doomscrolling loop and suggest how to tighten my Be Sissy schedule!"
            ).forEach { (chipLabel, fullPrompt) ->
                FilterChip(
                    selected = false,
                    enabled = !isStreaming,
                    onClick = { aiViewModel.sendChatMessage(fullPrompt) },
                    label = { Text(chipLabel, maxLines = 1) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = PinkContainer,
                        labelColor = BerryInk
                    )
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("Ask Belle (with Search Grounding)...") },
                modifier = Modifier.weight(1f).testTag("ai_chat_input"),
                shape = RoundedCornerShape(22.dp)
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = {
                    aiViewModel.sendChatMessage(inputText)
                    inputText = ""
                },
                enabled = !isStreaming && inputText.isNotBlank(),
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(25.dp))
                    .background(PinkPrimary)
                    .testTag("ai_chat_send_btn")
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = BlushSurface)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PinkArtStudioTab(
    aiViewModel: AiViewModel,
    onApplyWallpaperPath: (String, String) -> Unit
) {
    val genState by aiViewModel.imageGenState.collectAsStateWithLifecycle()
    var prompt by remember {
        mutableStateOf("Dreamy pastel pink silk coquette wallpaper with satin bows, crystal hearts, and soft pearls")
    }
    var selectedModel by remember { mutableStateOf(ImageGenModelOption.NANO_BANANA) }
    var selectedAspect by remember { mutableStateOf(ImageAspectOption.RATIO_9_16) }
    var selectedSize by remember { mutableStateOf(ImageSizeOption.SIZE_1K) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CoquetteCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("AI Wallpaper & Wall Block Art Studio", style = MaterialTheme.typography.titleMedium, color = BerryInk)
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    label = { Text("Artwork prompt") },
                    modifier = Modifier.fillMaxWidth().testTag("art_prompt_input")
                )

                Spacer(Modifier.height(10.dp))
                Text("Model", style = MaterialTheme.typography.labelLarge, color = BerryInk)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ImageGenModelOption.entries.forEach { m ->
                        FilterChip(
                            selected = selectedModel == m,
                            onClick = { selectedModel = m },
                            label = { Text(m.label) },
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
                Text("Aspect Ratio", style = MaterialTheme.typography.labelLarge, color = BerryInk)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ImageAspectOption.entries.forEach { ratio ->
                        FilterChip(
                            selected = selectedAspect == ratio,
                            onClick = { selectedAspect = ratio },
                            label = { Text(ratio.label) },
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
                Text("Image Size", style = MaterialTheme.typography.labelLarge, color = BerryInk)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ImageSizeOption.entries.forEach { sz ->
                        FilterChip(
                            selected = selectedSize == sz,
                            onClick = { selectedSize = sz },
                            label = { Text(sz.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PinkPrimary,
                                selectedLabelColor = BlushSurface,
                                containerColor = PinkContainer,
                                labelColor = BerryInk
                            )
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        aiViewModel.generatePinkArt(selectedModel, prompt, selectedAspect, selectedSize)
                    },
                    enabled = genState !is ImageGenUiState.Loading && prompt.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("generate_art_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary, contentColor = BlushSurface)
                ) {
                    Text(if (genState is ImageGenUiState.Loading) "Generating (30-60s)..." else "Generate Pink Artwork")
                }
            }
        }

        when (val s = genState) {
            is ImageGenUiState.Loading -> {
                CoquetteCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(color = PinkPrimary)
                        Spacer(Modifier.width(12.dp))
                        Text("Synthesizing ${selectedSize.label} ${selectedAspect.label} artwork...", color = BerryInk)
                    }
                }
            }
            is ImageGenUiState.Error -> {
                CoquetteCard(modifier = Modifier.fillMaxWidth()) {
                    Text(s.message, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.error)
                }
            }
            is ImageGenUiState.Success -> {
                CoquetteCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Image(
                            bitmap = s.bitmap.asImageBitmap(),
                            contentDescription = "Generated Pink Artwork",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .clip(RoundedCornerShape(18.dp))
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onApplyWallpaperPath("UI", s.savedPath) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary, contentColor = BlushSurface)
                            ) {
                                Text("Set UI Wallpaper")
                            }
                            OutlinedButton(
                                onClick = { onApplyWallpaperPath("AOD", s.savedPath) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Set Screen Saver")
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        OutlinedButton(
                            onClick = { onApplyWallpaperPath("WALL_BLOCK", s.savedPath) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Set Wall Block Photo")
                        }
                    }
                }
            }
            ImageGenUiState.Idle -> Unit
        }
        Spacer(Modifier.height(16.dp))
    }
}
