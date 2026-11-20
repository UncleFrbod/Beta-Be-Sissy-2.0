package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.VisualProofResult
import com.example.model.CustomExitTask
import com.example.model.UserProfile
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import com.example.media.VoiceLineAudioManager
import com.example.model.RecordedVoiceLine
import com.example.ui.theme.BerryInk
import com.example.ui.theme.BlushElevated
import com.example.ui.theme.BlushSurface
import com.example.ui.theme.PinkAccent
import com.example.ui.theme.PinkContainer
import com.example.ui.theme.PinkPrimary
import com.example.ui.theme.RoseGoldBorder
import kotlin.math.sqrt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EarlyExitBottomSheet(
    remainingMinutes: Long,
    profile: UserProfile,
    customTasks: List<CustomExitTask>,
    voiceLines: List<RecordedVoiceLine> = emptyList(),
    selectedVoiceLine: RecordedVoiceLine? = null,
    onSelectVoiceLine: (String) -> Unit = {},
    proofResult: VisualProofResult?,
    isVerifyingProof: Boolean,
    proofError: String?,
    taskProofResult: VisualProofResult?,
    isVerifyingTask: Boolean,
    taskProofError: String?,
    onDismiss: () -> Unit,
    onVerifyVisualProof: (Bitmap) -> Unit,
    onVerifyPhysicalTask: (Bitmap, String, String, Int) -> Unit,
    onClearTaskProof: () -> Unit,
    onUseKeys: (Int) -> Unit,
    onPauseWithPlaps: (Int) -> Unit,
    onCompleteSequentialTasks: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = BlushSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "3 Ways to End or Pause Early",
                style = MaterialTheme.typography.titleLarge,
                color = BerryInk,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = PinkContainer,
                contentColor = BerryInk,
                modifier = Modifier.clip(RoundedCornerShape(16.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("AI Proof", maxLines = 1) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Keys / Plaps", maxLines = 1) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("4 Tasks", maxLines = 1) }
                )
            }

            Spacer(Modifier.height(16.dp))

            when (selectedTab) {
                0 -> VisualProofTab(
                    profile = profile,
                    proofResult = proofResult,
                    isVerifying = isVerifyingProof,
                    errorMsg = proofError,
                    onVerifyBitmap = onVerifyVisualProof,
                    onFinishEarly = onDismiss
                )
                1 -> KeysAndPlapsTab(
                    remainingMinutes = remainingMinutes,
                    keysBalance = profile.keysBalance.toInt(),
                    plapsBalance = profile.plapsBalance.toInt(),
                    onUseKeys = { keys ->
                        onUseKeys(keys)
                        onDismiss()
                    },
                    onPausePlaps = { mins ->
                        onPauseWithPlaps(mins)
                        onDismiss()
                    }
                )
                2 -> SequentialTasksTab(
                    squatTarget = profile.squatCountTarget.toInt().coerceAtLeast(20),
                    customTasks = customTasks,
                    voiceLines = voiceLines,
                    selectedVoiceLine = selectedVoiceLine,
                    onSelectVoiceLine = onSelectVoiceLine,
                    taskProofResult = taskProofResult,
                    isVerifyingTask = isVerifyingTask,
                    taskProofError = taskProofError,
                    onVerifyPhysicalTask = onVerifyPhysicalTask,
                    onClearTaskProof = onClearTaskProof,
                    onAllCompleted = {
                        onCompleteSequentialTasks()
                        onDismiss()
                    }
                )
            }
        }
    }
}

@Composable
private fun VisualProofTab(
    profile: UserProfile,
    proofResult: VisualProofResult?,
    isVerifying: Boolean,
    errorMsg: String?,
    onVerifyBitmap: (Bitmap) -> Unit,
    onFinishEarly: () -> Unit
) {
    CoquetteCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Required Visual Proof",
                style = MaterialTheme.typography.titleMedium,
                color = BerryInk
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = profile.visualProofDescription,
                style = MaterialTheme.typography.bodyLarge,
                color = BerryInk
            )
            Spacer(Modifier.height(12.dp))

            CameraXScannerCard(
                title = "CameraX AI Proof Scanner",
                subtitle = "Scan your proof live or upload a photo for Gemini Vision.",
                isVerifying = isVerifying,
                verificationResult = proofResult,
                errorMessage = errorMsg,
                onBitmapCaptured = onVerifyBitmap,
                defaultFrontCamera = false,
                testTagPrefix = "proof"
            )

            if (proofResult?.approved == true) {
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onFinishEarly,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary, contentColor = BlushSurface)
                ) {
                    Text("Session Ended!")
                }
            }
        }
    }
}

@Composable
private fun KeysAndPlapsTab(
    remainingMinutes: Long,
    keysBalance: Int,
    plapsBalance: Int,
    onUseKeys: (Int) -> Unit,
    onPausePlaps: (Int) -> Unit
) {
    val maxKeysNeeded = ((remainingMinutes + 14L) / 15L).toInt().coerceAtLeast(1)
    val usableKeys = keysBalance.coerceAtMost(maxKeysNeeded)
    var keysToSpend by remember(usableKeys) { mutableFloatStateOf(if (usableKeys > 0) 1f else 0f) }
    var plapMinutes by remember(plapsBalance) { mutableFloatStateOf(if (plapsBalance >= 5) 5f else plapsBalance.toFloat()) }

    CoquetteCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.img_heart_key),
                    contentDescription = "Heart Key",
                    modifier = Modifier.size(30.dp)
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text("Keys: $keysBalance available", style = MaterialTheme.typography.titleMedium, color = BerryInk)
                    Text("1 Key = 15 min reduction ($maxKeysNeeded Keys to end ${remainingMinutes}m)", style = MaterialTheme.typography.bodyMedium, color = BerryInk)
                }
            }
            Spacer(Modifier.height(10.dp))
            if (usableKeys > 0) {
                val chosenKeys = keysToSpend.toInt().coerceIn(1, usableKeys)
                Slider(
                    value = keysToSpend,
                    onValueChange = { keysToSpend = it },
                    valueRange = 1f..usableKeys.toFloat().coerceAtLeast(1f),
                    steps = (usableKeys - 2).coerceAtLeast(0),
                    colors = SliderDefaults.colors(thumbColor = PinkPrimary, activeTrackColor = PinkPrimary)
                )
                Button(
                    onClick = { onUseKeys(chosenKeys) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("spend_keys_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary, contentColor = BlushSurface)
                ) {
                    Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (chosenKeys * 15 >= remainingMinutes) {
                            "End Session Early ($chosenKeys Keys)"
                        } else {
                            "Reduce by ${chosenKeys * 15}m ($chosenKeys Keys)"
                        }
                    )
                }
            } else {
                Text("Earn Keys by completing daily sessions (+1/day, +10 weekly streak).", color = BerryInk)
            }
        }
    }

    Spacer(Modifier.height(12.dp))

    CoquetteCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.img_glitter_bow),
                    contentDescription = "Plaps Bow",
                    modifier = Modifier.size(30.dp)
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text("Plaps: $plapsBalance available", style = MaterialTheme.typography.titleMedium, color = BerryInk)
                    Text("1 Plap = 1 minute session pause", style = MaterialTheme.typography.bodyMedium, color = BerryInk)
                }
            }
            Spacer(Modifier.height(10.dp))
            if (plapsBalance > 0) {
                val maxPlaps = plapsBalance.coerceAtMost(60)
                val chosenMins = plapMinutes.toInt().coerceIn(1, maxPlaps)
                Slider(
                    value = plapMinutes,
                    onValueChange = { plapMinutes = it },
                    valueRange = 1f..maxPlaps.toFloat().coerceAtLeast(1f),
                    colors = SliderDefaults.colors(thumbColor = PinkAccent, activeTrackColor = PinkAccent)
                )
                Button(
                    onClick = { onPausePlaps(chosenMins) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("spend_plaps_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = PinkAccent, contentColor = BerryInk)
                ) {
                    Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Pause Session for ${chosenMins}m ($chosenMins Plaps)", fontWeight = FontWeight.Bold)
                }
            } else {
                Text("Earn Plaps daily (+5/day, +50 weekly streak). All Keys & Plaps burn at month-end!", color = BerryInk)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SequentialTasksTab(
    squatTarget: Int,
    customTasks: List<CustomExitTask>,
    voiceLines: List<RecordedVoiceLine>,
    selectedVoiceLine: RecordedVoiceLine?,
    onSelectVoiceLine: (String) -> Unit,
    taskProofResult: VisualProofResult?,
    isVerifyingTask: Boolean,
    taskProofError: String?,
    onVerifyPhysicalTask: (Bitmap, String, String, Int) -> Unit,
    onClearTaskProof: () -> Unit,
    onAllCompleted: () -> Unit
) {
    val context = LocalContext.current
    val audioManager = remember(context) { VoiceLineAudioManager(context) }
    DisposableEffect(audioManager) {
        onDispose { audioManager.release() }
    }
    var voicePlaybackStatus by remember { mutableStateOf("") }

    var stepIndex by remember { mutableIntStateOf(0) } // 0=Squats, 1=Read Line, 2=Walk 20 steps, 3=Exact Type
    var squatsDone by remember { mutableIntStateOf(0) }
    var lineReadVerified by remember { mutableStateOf(false) }
    var stepsDone by remember { mutableIntStateOf(0) }

    // Unlock physical tasks automatically when Gemini AI approves the CameraX frame
    LaunchedEffect(taskProofResult, stepIndex) {
        if (taskProofResult?.approved == true) {
            if (stepIndex == 0) {
                squatsDone = squatTarget
            } else if (stepIndex == 2) {
                stepsDone = 20
            }
        }
    }

    val readLines = remember(customTasks) {
        customTasks.filter { it.taskType == "READ_LINE" }.map { it.content }
            .ifEmpty { listOf("I choose poised calm, radiant focus, and total discipline today.") }
    }
    val typingPrompts = remember(customTasks) {
        customTasks.filter { it.taskType == "TYPING_PROMPT" }.map { it.content }
            .ifEmpty { listOf("Grace, poise, and discipline! I choose calm focus over mindless scrolling today.") }
    }
    val activeReadLine = remember(readLines) { readLines.random() }
    val activeTypingPrompt = remember(typingPrompts) { typingPrompts.random() }
    var typedText by remember { mutableStateOf("") }

    // Accelerometer / Step tracking during steps 0 and 2
    DisposableEffect(stepIndex) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        var lastTrigger = 0L
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                val values = event?.values ?: return
                val mag = sqrt(values[0] * values[0] + values[1] * values[1] + values[2] * values[2])
                val now = System.currentTimeMillis()
                if (mag > 13.5f && now - lastTrigger > 650L) {
                    lastTrigger = now
                    if (stepIndex == 0 && squatsDone < squatTarget) {
                        squatsDone++
                    } else if (stepIndex == 2 && stepsDone < 20) {
                        stepsDone++
                    }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        if (accel != null && (stepIndex == 0 || stepIndex == 2)) {
            sensorManager.registerListener(listener, accel, SensorManager.SENSOR_DELAY_UI)
        }
        onDispose { sensorManager.unregisterListener(listener) }
    }

    // Speech recognizer for step 1
    var speechStatus by remember { mutableStateOf("Tap Speak & read the line aloud") }
    var isListening by remember { mutableStateOf(false) }
    val micPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) speechStatus = "Mic permission denied — use confirm button below if testing on emulator."
    }

    CoquetteCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Sequential Task ${stepIndex + 1} of 4 (Must complete in order)",
                style = MaterialTheme.typography.titleMedium,
                color = PinkPrimary
            )
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (stepIndex + 1) / 4f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = PinkPrimary,
                trackColor = PinkContainer
            )
            Spacer(Modifier.height(10.dp))

            // Active Feminine Voice Line Cue Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(PinkContainer.copy(alpha = 0.6f))
                    .border(1.dp, RoseGoldBorder, RoundedCornerShape(14.dp))
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.GraphicEq, contentDescription = null, tint = PinkPrimary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "Task Voice Cue: ${selectedVoiceLine?.label ?: "System Default Chime"}",
                                style = MaterialTheme.typography.labelLarge,
                                color = BerryInk,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                            if (voicePlaybackStatus.isNotBlank()) {
                                Text(
                                    text = voicePlaybackStatus,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PinkPrimary
                                )
                            }
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            voicePlaybackStatus = "Playing ${selectedVoiceLine?.label ?: "system chime"}..."
                            audioManager.playVoiceLineOrDefault(
                                filePath = selectedVoiceLine?.filePath,
                                onFinished = { voicePlaybackStatus = "Played ♡" },
                                onError = { voicePlaybackStatus = it }
                            )
                        },
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("play_custom_voice_btn")
                    ) {
                        Icon(Icons.Default.VolumeUp, contentDescription = "Play selected feminine voice cue", modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Play Cue", maxLines = 1)
                    }
                }
                if (voiceLines.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FilterChip(
                            selected = selectedVoiceLine == null,
                            onClick = { onSelectVoiceLine("") },
                            label = { Text("System Chime", maxLines = 1) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PinkPrimary,
                                selectedLabelColor = BlushSurface,
                                containerColor = BlushSurface,
                                labelColor = BerryInk
                            )
                        )
                        voiceLines.take(4).forEach { line ->
                            FilterChip(
                                selected = selectedVoiceLine?.id == line.id,
                                onClick = {
                                    onSelectVoiceLine(line.id)
                                    audioManager.playVoiceLineOrDefault(line.filePath)
                                },
                                label = { Text(line.label.take(18), maxLines = 1) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PinkPrimary,
                                    selectedLabelColor = BlushSurface,
                                    containerColor = BlushSurface,
                                    labelColor = BerryInk
                                )
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            when (stepIndex) {
                0 -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = BerryInk)
                        Spacer(Modifier.width(8.dp))
                        Text("Step 1: $squatTarget Buttocks Squats (+5 each exit!)", fontWeight = FontWeight.Bold, color = BerryInk)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Progress: $squatsDone / $squatTarget squats — scan your squat stance with AI Camera or track reps:",
                        color = BerryInk
                    )
                    Spacer(Modifier.height(10.dp))

                    CameraXScannerCard(
                        title = "AI Camera Scan: Buttocks Squats",
                        subtitle = "Frame your squat stance & tap Scan & Verify for Gemini AI approval.",
                        isVerifying = isVerifyingTask,
                        verificationResult = taskProofResult,
                        errorMessage = taskProofError,
                        onBitmapCaptured = { bmp ->
                            onVerifyPhysicalTask(
                                bmp,
                                "Buttocks Squats",
                                "Perform $squatTarget deep buttocks squats with poised posture",
                                squatTarget
                            )
                        },
                        defaultFrontCamera = true,
                        testTagPrefix = "squat_camera"
                    )

                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = { if (squatsDone < squatTarget) squatsDone++ },
                            modifier = Modifier.weight(1f).height(48.dp).testTag("squat_rep_btn")
                        ) {
                            Text("Count Rep (+1)")
                        }
                        Button(
                            onClick = {
                                audioManager.playVoiceLineOrDefault(selectedVoiceLine?.filePath)
                                onClearTaskProof()
                                stepIndex = 1
                            },
                            enabled = squatsDone >= squatTarget,
                            modifier = Modifier.weight(1f).height(48.dp).testTag("squat_next_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary, contentColor = BlushSurface)
                        ) {
                            Text("Next Step")
                        }
                    }
                }

                1 -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Mic, contentDescription = null, tint = BerryInk)
                        Spacer(Modifier.width(8.dp))
                        Text("Step 2: Read Line Aloud (Poised Voice)", fontWeight = FontWeight.Bold, color = BerryInk)
                    }
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(BlushElevated)
                            .border(1.dp, RoseGoldBorder, RoundedCornerShape(14.dp))
                            .padding(12.dp)
                    ) {
                        Text(text = "\"$activeReadLine\"", style = MaterialTheme.typography.bodyLarge, color = BerryInk)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(speechStatus, style = MaterialTheme.typography.bodyMedium, color = BerryInk)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                val granted = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED
                                if (!granted) {
                                    micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    return@OutlinedButton
                                }
                                if (SpeechRecognizer.isRecognitionAvailable(context)) {
                                    isListening = true
                                    speechStatus = "Listening..."
                                    val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
                                    recognizer.setRecognitionListener(object : RecognitionListener {
                                        override fun onResults(results: Bundle?) {
                                            isListening = false
                                            val spoken = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                                            if (spoken.isNotBlank()) {
                                                lineReadVerified = true
                                                speechStatus = "Heard: \"$spoken\""
                                            } else {
                                                speechStatus = "Didn't catch audio — try again."
                                            }
                                            recognizer.destroy()
                                        }
                                        override fun onError(error: Int) {
                                            isListening = false
                                            speechStatus = "Speech unavailable on emulator — use Manual Voice Confirm."
                                            recognizer.destroy()
                                        }
                                        override fun onReadyForSpeech(params: Bundle?) = Unit
                                        override fun onBeginningOfSpeech() = Unit
                                        override fun onRmsChanged(rmsdB: Float) = Unit
                                        override fun onBufferReceived(buffer: ByteArray?) = Unit
                                        override fun onEndOfSpeech() = Unit
                                        override fun onPartialResults(partialResults: Bundle?) = Unit
                                        override fun onEvent(eventType: Int, params: Bundle?) = Unit
                                    })
                                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    }
                                    recognizer.startListening(intent)
                                } else {
                                    lineReadVerified = true
                                    speechStatus = "Emulator voice fallback confirmed."
                                }
                            },
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Text(if (isListening) "Listening..." else "Speak Line")
                        }
                        OutlinedButton(
                            onClick = {
                                lineReadVerified = true
                                speechStatus = "Confirmed aloud!"
                            },
                            modifier = Modifier.weight(1f).height(48.dp).testTag("voice_confirm_btn")
                        ) {
                            Text("Confirm Read")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            audioManager.playVoiceLineOrDefault(selectedVoiceLine?.filePath)
                            onClearTaskProof()
                            stepIndex = 2
                        },
                        enabled = lineReadVerified,
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("voice_next_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary, contentColor = BlushSurface)
                    ) {
                        Text("Next: Posture Walk")
                    }
                }

                2 -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DirectionsWalk, contentDescription = null, tint = BerryInk)
                        Spacer(Modifier.width(8.dp))
                        Text("Step 3: 20-Step Posture Walk", fontWeight = FontWeight.Bold, color = BerryInk)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("Walk 20 graceful steps with tall shoulders, swaying hips, and crossed stride.", color = BerryInk)
                    Spacer(Modifier.height(8.dp))
                    Text("Steps: $stepsDone / 20", fontWeight = FontWeight.Bold, color = PinkPrimary)
                    Spacer(Modifier.height(10.dp))

                    CameraXScannerCard(
                        title = "AI Camera Scan: Posture Walk",
                        subtitle = "Show your tall standing/walking posture in frame to verify with AI.",
                        isVerifying = isVerifyingTask,
                        verificationResult = taskProofResult,
                        errorMessage = taskProofError,
                        onBitmapCaptured = { bmp ->
                            onVerifyPhysicalTask(
                                bmp,
                                "20-Step Posture Walk",
                                "Walk 20 steps with high heels or feminine posture, crossed stride, and tall shoulders",
                                20
                            )
                        },
                        defaultFrontCamera = true,
                        testTagPrefix = "walk_camera"
                    )

                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = { if (stepsDone < 20) stepsDone++ },
                            modifier = Modifier.weight(1f).height(48.dp).testTag("walk_step_btn")
                        ) {
                            Text("Log Step (+1)")
                        }
                        Button(
                            onClick = {
                                audioManager.playVoiceLineOrDefault(selectedVoiceLine?.filePath)
                                onClearTaskProof()
                                stepIndex = 3
                            },
                            enabled = stepsDone >= 20,
                            modifier = Modifier.weight(1f).height(48.dp).testTag("walk_next_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary, contentColor = BlushSurface)
                        ) {
                            Text("Next: Typing")
                        }
                    }
                }

                3 -> {
                    val exactMatch = typedText == activeTypingPrompt
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Keyboard, contentDescription = null, tint = BerryInk)
                        Spacer(Modifier.width(8.dp))
                        Text("Step 4: Exact Typing Challenge", fontWeight = FontWeight.Bold, color = BerryInk)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("Type every dot, comma, space, capital letter, and symbol exactly:", style = MaterialTheme.typography.bodyMedium, color = BerryInk)
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(BlushElevated)
                            .border(1.dp, RoseGoldBorder, RoundedCornerShape(14.dp))
                            .padding(12.dp)
                    ) {
                        Text(text = activeTypingPrompt, style = MaterialTheme.typography.bodyLarge, color = BerryInk)
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = typedText,
                        onValueChange = { candidate ->
                            if (candidate.length - typedText.length <= 3) {
                                typedText = candidate
                            }
                        },
                        label = { Text("Type exact text (no paste)") },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.fillMaxWidth().testTag("exact_typing_input")
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (exactMatch) "Perfect 100% match!" else "Characters matched: ${typedText.commonPrefixWith(activeTypingPrompt).length} / ${activeTypingPrompt.length}",
                        color = if (exactMatch) PinkPrimary else BerryInk,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = {
                            audioManager.playVoiceLineOrDefault(selectedVoiceLine?.filePath)
                            onAllCompleted()
                        },
                        enabled = exactMatch,
                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("complete_tasks_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary, contentColor = BlushSurface)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Complete & End Session")
                    }
                }
            }
        }
    }
}
