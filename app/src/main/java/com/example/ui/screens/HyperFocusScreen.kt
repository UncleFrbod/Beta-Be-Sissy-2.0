package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BlushSurface
import com.example.ui.theme.DarkPlumBg
import com.example.ui.theme.DarkPlumSurface
import com.example.ui.theme.PinkAccent
import com.example.ui.theme.PinkPrimary
import kotlinx.coroutines.delay

@Composable
fun HyperFocusScreen(
    initialMinutes: Int = 15,
    onCompleteTaskReward: (String) -> Unit,
    onExit: () -> Unit
) {
    BackHandler { onExit() }

    var singleTaskText by remember {
        mutableStateOf("Finish my ONE priority task — nothing else exists right now ♡")
    }
    var isEditingTask by remember { mutableStateOf(false) }
    val totalSeconds = remember(initialMinutes) { (initialMinutes.coerceIn(5, 60)) * 60 }
    var remainingSeconds by remember(totalSeconds) { mutableIntStateOf(totalSeconds) }
    var isRunning by remember { mutableStateOf(true) }

    LaunchedEffect(isRunning, totalSeconds) {
        while (isRunning && remainingSeconds > 0) {
            delay(1000L)
            remainingSeconds -= 1
        }
        if (remainingSeconds <= 0) {
            isRunning = false
        }
    }

    val progressFraction = (remainingSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progressFraction,
        animationSpec = tween(400),
        label = "hyperFocusBar"
    )
    val mins = remainingSeconds / 60
    val secs = remainingSeconds % 60

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkPlumBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 20.dp)
            .testTag("hyper_focus_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Minimalist Top Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(DarkPlumSurface)
                        .border(1.5.dp, PinkAccent, RoundedCornerShape(50))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CenterFocusStrong,
                        contentDescription = null,
                        tint = PinkAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "HYPER-FOCUS",
                        style = MaterialTheme.typography.labelLarge,
                        color = BlushSurface,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                OutlinedButton(
                    onClick = onExit,
                    border = BorderStroke(1.5.dp, PinkPrimary),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("hyper_focus_exit_btn")
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Exit Hyper-Focus", tint = BlushSurface)
                    Spacer(Modifier.width(6.dp))
                    Text("Exit", color = BlushSurface, fontWeight = FontWeight.Bold)
                }
            }

            // Single High-Contrast Task Focus Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = DarkPlumSurface,
                border = BorderStroke(2.dp, PinkPrimary)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "ONLY THIS ONE TASK MATTERS",
                        style = MaterialTheme.typography.labelMedium,
                        color = PinkAccent,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(14.dp))

                    if (isEditingTask) {
                        OutlinedTextField(
                            value = singleTaskText,
                            onValueChange = { singleTaskText = it.take(160) },
                            label = { Text("Single focus task", color = PinkAccent) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = BlushSurface,
                                unfocusedTextColor = BlushSurface,
                                focusedBorderColor = PinkPrimary,
                                unfocusedBorderColor = PinkAccent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("hyper_focus_task_input")
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { isEditingTask = false },
                            border = BorderStroke(1.dp, PinkAccent),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text("Lock Task", color = BlushSurface)
                        }
                    } else {
                        Text(
                            text = singleTaskText,
                            style = MaterialTheme.typography.headlineSmall,
                            color = BlushSurface,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { isEditingTask = true },
                            border = BorderStroke(1.dp, PinkAccent.copy(alpha = 0.7f)),
                            modifier = Modifier.height(44.dp)
                        ) {
                            Text("Edit Task", color = PinkAccent)
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // Prominent High-Contrast Visual Time Bar
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(22.dp)
                            .clip(RoundedCornerShape(11.dp)),
                        color = PinkPrimary,
                        trackColor = DarkPlumBg
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = "%02d:%02d".format(mins, secs),
                        fontSize = 42.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = PinkAccent
                    )
                    Text(
                        text = if (isRunning) "High-contrast distraction shield active" else "Timer paused",
                        style = MaterialTheme.typography.bodyMedium,
                        color = BlushSurface.copy(alpha = 0.85f)
                    )
                }
            }

            // Minimalist Primary Controls (2 clean actions)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { isRunning = !isRunning },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PinkAccent,
                        contentColor = DarkPlumBg
                    ),
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("hyper_focus_play_pause_btn")
                ) {
                    Icon(
                        imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (isRunning) "Pause Sprint" else "Resume Sprint",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Button(
                    onClick = {
                        onCompleteTaskReward(singleTaskText)
                        onExit()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PinkPrimary,
                        contentColor = BlushSurface
                    ),
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("hyper_focus_complete_btn")
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Task Complete (+3 Plaps 🎀)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
