package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.media.VoiceLineAudioManager
import com.example.model.RecordedVoiceLine
import com.example.ui.theme.BerryInk
import com.example.ui.theme.BlushElevated
import com.example.ui.theme.BlushSurface
import com.example.ui.theme.PinkContainer
import com.example.ui.theme.PinkPrimary
import com.example.ui.theme.RoseGoldBorder
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun VoiceLineRecorderCard(
    voiceLines: List<RecordedVoiceLine>,
    selectedVoiceLineId: String,
    onSaveRecording: (label: String, filePath: String, durationSeconds: Int) -> Unit,
    onSelectVoiceLine: (String) -> Unit,
    onDeleteVoiceLine: (RecordedVoiceLine) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val audioManager = remember(context) { VoiceLineAudioManager(context) }

    DisposableEffect(audioManager) {
        onDispose { audioManager.release() }
    }

    var lineLabel by remember { mutableStateOf("I am poised, calm & glowing ♡") }
    var isRecording by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var pendingFile by remember { mutableStateOf<File?>(null) }
    var playingId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            elapsedSeconds = 0
            while (isRecording) {
                delay(1000L)
                elapsedSeconds += 1
                if (elapsedSeconds >= 30) {
                    val savedFile = audioManager.stopRecording()
                    isRecording = false
                    if (savedFile != null) {
                        onSaveRecording(lineLabel, savedFile.absolutePath, elapsedSeconds)
                    }
                    pendingFile = null
                }
            }
        }
    }

    val beginMicRecording: () -> Unit = {
        val fileId = System.currentTimeMillis().toString()
        val outFile = audioManager.createRecordingFile(fileId)
        audioManager.startRecording(outFile)
            .onSuccess {
                pendingFile = outFile
                isRecording = true
            }
            .onFailure { err ->
                pendingFile = null
                isRecording = false
                onShowMessage(err.localizedMessage ?: "Microphone unavailable on this device.")
            }
    }

    val micPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            beginMicRecording()
        } else {
            onShowMessage("Microphone permission is needed to record feminine voice lines.")
        }
    }

    CoquetteCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.GraphicEq, contentDescription = null, tint = PinkPrimary)
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Feminine Voice Lines Studio",
                        style = MaterialTheme.typography.titleMedium,
                        color = BerryInk,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Record your own feminine cues to play during End Session Tasks instead of system sounds.",
                        style = MaterialTheme.typography.bodySmall,
                        color = BerryInk.copy(alpha = 0.85f)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = lineLabel,
                onValueChange = { lineLabel = it },
                label = { Text("Voice line title / affirmation") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("voice_line_label_input")
            )

            Spacer(Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        if (isRecording) {
                            val saved = audioManager.stopRecording()
                            isRecording = false
                            if (saved != null) {
                                onSaveRecording(lineLabel, saved.absolutePath, elapsedSeconds.coerceAtLeast(1))
                            } else {
                                onShowMessage("Recording was too short or empty.")
                            }
                            pendingFile = null
                        } else {
                            val granted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            if (granted) {
                                beginMicRecording()
                            } else {
                                micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("record_voice_line_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRecording) BerryInk else PinkPrimary,
                        contentColor = BlushSurface
                    )
                ) {
                    Icon(
                        if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = if (isRecording) "Stop recording" else "Record voice line",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (isRecording) "Stop & Save (${elapsedSeconds}s)" else "Record Feminine Voice",
                        fontWeight = FontWeight.Bold
                    )
                }

                if (isRecording) {
                    OutlinedButton(
                        onClick = {
                            audioManager.cancelRecording()
                            isRecording = false
                            pendingFile = null
                        },
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("Cancel")
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Option to revert to Default System Sound
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selectedVoiceLineId.isBlank()) PinkContainer else BlushElevated)
                    .border(1.dp, RoseGoldBorder, RoundedCornerShape(12.dp))
                    .clickable { onSelectVoiceLine("") }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (selectedVoiceLineId.isBlank()) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = "Select default system sound",
                    tint = if (selectedVoiceLineId.isBlank()) PinkPrimary else BerryInk
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "System Default Chime (Fallback)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = BerryInk,
                    fontWeight = if (selectedVoiceLineId.isBlank()) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        playingId = "DEFAULT"
                        audioManager.playVoiceLineOrDefault(
                            filePath = null,
                            onFinished = { playingId = null },
                            onError = { playingId = null; onShowMessage(it) }
                        )
                    }
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Preview system sound", tint = PinkPrimary)
                }
            }

            if (voiceLines.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                voiceLines.forEach { line ->
                    val isSelected = selectedVoiceLineId == line.id
                    val isPlaying = playingId == line.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) PinkContainer else BlushElevated)
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) PinkPrimary else RoseGoldBorder,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onSelectVoiceLine(line.id) }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = "Select voice line",
                            tint = if (isSelected) PinkPrimary else BerryInk
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = line.label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = BerryInk,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1
                            )
                            Text(
                                text = "${line.durationSeconds}s custom recording",
                                style = MaterialTheme.typography.labelSmall,
                                color = PinkPrimary
                            )
                        }
                        IconButton(
                            onClick = {
                                if (isPlaying) {
                                    audioManager.stopPlayback()
                                    playingId = null
                                } else {
                                    playingId = line.id
                                    audioManager.playVoiceLineOrDefault(
                                        filePath = line.filePath,
                                        onFinished = { playingId = null },
                                        onError = { playingId = null; onShowMessage(it) }
                                    )
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Stop playback" else "Play voice line",
                                tint = PinkPrimary
                            )
                        }
                        IconButton(onClick = { onDeleteVoiceLine(line) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete voice line", tint = BerryInk)
                        }
                    }
                }
            }
        }
    }
}
