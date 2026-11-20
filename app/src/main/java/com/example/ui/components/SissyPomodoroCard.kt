package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.BerryInk
import com.example.ui.theme.BlushDeep
import com.example.ui.theme.BlushElevated
import com.example.ui.theme.BlushSurface
import com.example.ui.theme.PinkAccent
import com.example.ui.theme.PinkContainer
import com.example.ui.theme.PinkPrimary
import com.example.ui.theme.RoseGoldBorder
import kotlinx.coroutines.delay

enum class PomodoroPreset(val label: String, val focusMinutes: Int, val breakMinutes: Int) {
    SPRINT("15m / 3m", 15, 3),
    CLASSIC("25m / 5m", 25, 5),
    DEEP("45m / 10m", 45, 10)
}

@Composable
fun SissyPomodoroCard(
    fontFamily: FontFamily? = null,
    onOpenHyperFocus: (Int) -> Unit = {},
    onCycleCompleted: (Boolean) -> Unit = {}
) {
    var preset by remember { mutableStateOf(PomodoroPreset.CLASSIC) }
    var isBreakPhase by remember { mutableStateOf(false) }
    var isRunning by remember { mutableStateOf(false) }
    val totalSeconds = remember(preset, isBreakPhase) {
        (if (isBreakPhase) preset.breakMinutes else preset.focusMinutes) * 60
    }
    var remainingSeconds by remember(preset, isBreakPhase) {
        mutableIntStateOf(totalSeconds)
    }

    LaunchedEffect(isRunning, preset, isBreakPhase) {
        while (isRunning && remainingSeconds > 0) {
            delay(1000L)
            remainingSeconds -= 1
            if (remainingSeconds <= 0) {
                val finishedBreak = isBreakPhase
                isRunning = false
                isBreakPhase = !isBreakPhase
                val nextTotal = (if (isBreakPhase) preset.breakMinutes else preset.focusMinutes) * 60
                remainingSeconds = nextTotal
                onCycleCompleted(!finishedBreak)
            }
        }
    }

    val targetFraction = (remainingSeconds.toFloat() / totalSeconds.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = tween(durationMillis = 450),
        label = "sissyPomodoroFill"
    )
    val percentRemaining = (targetFraction * 100f).toInt()
    val mins = remainingSeconds / 60
    val secs = remainingSeconds % 60

    CoquetteCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("sissy_pomodoro_card"),
        containerColor = BlushSurface.copy(alpha = 0.94f),
        borderColor = PinkPrimary.copy(alpha = 0.75f)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isBreakPhase) PinkAccent else PinkPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isBreakPhase) Icons.Default.SelfImprovement else Icons.Default.Favorite,
                            contentDescription = "Sissy Pomodoro",
                            tint = BlushSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isBreakPhase) "Sissy Pink Rest Break 🎀" else "Sissy Visual Pomodoro ♡",
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = fontFamily ?: MaterialTheme.typography.titleMedium.fontFamily,
                            color = BerryInk,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isRunning) {
                                "$percentRemaining% time bar remaining (${mins}m ${secs}s)"
                            } else {
                                "Visual time bar beats ADHD time blindness"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = PinkPrimary
                        )
                    }
                }

                // Switch Focus <-> Break manually
                FilterChip(
                    selected = isBreakPhase,
                    onClick = {
                        isRunning = false
                        isBreakPhase = !isBreakPhase
                    },
                    label = { Text(if (isBreakPhase) "Break" else "Focus", maxLines = 1) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PinkAccent,
                        selectedLabelColor = BerryInk,
                        containerColor = PinkContainer,
                        labelColor = BerryInk
                    ),
                    modifier = Modifier.testTag("pomodoro_phase_toggle")
                )
            }

            Spacer(Modifier.height(12.dp))

            // Visual Depleting Progress Bar + Heart Indicator + Quarter Milestone Ticks
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .clip(RoundedCornerShape(17.dp))
                    .background(PinkContainer.copy(alpha = 0.75f))
                    .border(1.5.dp, RoseGoldBorder, RoundedCornerShape(17.dp))
                    .padding(horizontal = 4.dp, vertical = 4.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(26.dp)
                        .testTag("pomodoro_visual_progress_bar")
                ) {
                    val barW = size.width
                    val barH = size.height
                    val corner = CornerRadius(barH / 2f, barH / 2f)

                    // Subtle background track
                    drawRoundRect(
                        color = BlushElevated,
                        topLeft = Offset.Zero,
                        size = Size(barW, barH),
                        cornerRadius = corner
                    )

                    // Filled glowing pink progress representing remaining time
                    val fillW = (barW * animatedFraction).coerceAtLeast(if (animatedFraction > 0f) barH else 0f)
                    if (fillW > 0f) {
                        drawRoundRect(
                            brush = Brush.horizontalGradient(
                                colors = if (isBreakPhase) {
                                    listOf(PinkAccent, PinkPrimary)
                                } else {
                                    listOf(PinkPrimary, PinkAccent, BlushDeep)
                                }
                            ),
                            topLeft = Offset.Zero,
                            size = Size(fillW, barH),
                            cornerRadius = corner
                        )
                    }

                    // Quarter notch guides for intuitive time-left perception
                    listOf(0.25f, 0.50f, 0.75f).forEach { mark ->
                        val x = barW * mark
                        drawLine(
                            color = BerryInk.copy(alpha = 0.28f),
                            start = Offset(x, barH * 0.22f),
                            end = Offset(x, barH * 0.78f),
                            strokeWidth = 2.5f
                        )
                    }

                    // Moving cute heart marker at the leading edge
                    val cx = (fillW - barH * 0.45f).coerceIn(barH * 0.45f, (barW - barH * 0.45f).coerceAtLeast(barH * 0.45f))
                    val hs = barH * 0.65f
                    val hTop = (barH - hs) / 2f
                    val hLeft = cx - hs / 2f
                    val heartPath = Path().apply {
                        val hcx = hLeft + hs / 2f
                        moveTo(hcx, hTop + hs * 0.88f)
                        cubicTo(
                            hLeft + hs * 0.08f, hTop + hs * 0.62f,
                            hLeft, hTop + hs * 0.24f,
                            hLeft + hs * 0.27f, hTop + hs * 0.14f
                        )
                        cubicTo(
                            hLeft + hs * 0.42f, hTop + hs * 0.08f,
                            hcx, hTop + hs * 0.22f,
                            hcx, hTop + hs * 0.30f
                        )
                        cubicTo(
                            hcx, hTop + hs * 0.22f,
                            hLeft + hs * 0.58f, hTop + hs * 0.08f,
                            hLeft + hs * 0.73f, hTop + hs * 0.14f
                        )
                        cubicTo(
                            hLeft + hs, hTop + hs * 0.24f,
                            hLeft + hs * 0.92f, hTop + hs * 0.62f,
                            hcx, hTop + hs * 0.88f
                        )
                        close()
                    }
                    drawPath(path = heartPath, color = BlushSurface)
                }
            }

            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Done",
                    style = MaterialTheme.typography.labelSmall,
                    color = BerryInk.copy(alpha = 0.75f)
                )
                Text(
                    text = "%02d:%02d left".format(mins, secs),
                    style = MaterialTheme.typography.titleSmall,
                    color = BerryInk,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "100%",
                    style = MaterialTheme.typography.labelSmall,
                    color = BerryInk.copy(alpha = 0.75f)
                )
            }

            Spacer(Modifier.height(10.dp))

            // Preset Duration Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PomodoroPreset.entries.forEach { option ->
                    FilterChip(
                        selected = preset == option,
                        onClick = {
                            preset = option
                            isRunning = false
                        },
                        label = { Text(option.label, maxLines = 1) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PinkPrimary,
                            selectedLabelColor = BlushSurface,
                            containerColor = PinkContainer,
                            labelColor = BerryInk
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pomodoro_preset_${option.name.lowercase()}")
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { isRunning = !isRunning },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PinkPrimary,
                        contentColor = BlushSurface
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(48.dp)
                        .testTag("pomodoro_start_pause_btn")
                ) {
                    Icon(
                        imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isRunning) "Pause Pomodoro" else "Start Pomodoro",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (isRunning) "Pause" else "Start Timer ♡",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }

                IconButton(
                    onClick = {
                        isRunning = false
                        remainingSeconds = totalSeconds
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(PinkContainer)
                        .testTag("pomodoro_reset_btn")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reset Pomodoro", tint = BerryInk)
                }

                OutlinedButton(
                    onClick = { onOpenHyperFocus((remainingSeconds / 60).coerceAtLeast(10)) },
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .weight(1.1f)
                        .height(48.dp)
                        .testTag("pomodoro_hyperfocus_btn")
                ) {
                    Icon(Icons.Default.CenterFocusStrong, contentDescription = null, tint = BerryInk, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Hyper-Focus", color = BerryInk, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
        }
    }
}
