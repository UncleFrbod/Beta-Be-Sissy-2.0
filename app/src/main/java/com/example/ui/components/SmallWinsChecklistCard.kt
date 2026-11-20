package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.model.SmallWinsDailyState
import com.example.ui.theme.BerryInk
import com.example.ui.theme.BlushElevated
import com.example.ui.theme.BlushSurface
import com.example.ui.theme.PinkAccent
import com.example.ui.theme.PinkContainer
import com.example.ui.theme.PinkPrimary
import com.example.ui.theme.RoseGoldBorder

@Composable
fun SmallWinsChecklistCard(
    smallWinsState: SmallWinsDailyState,
    plapsBalance: Long,
    onToggleWin: (String) -> Unit,
    onAddCustomWin: (String) -> Unit,
    onDeleteWin: (String) -> Unit
) {
    var newWinText by remember { mutableStateOf("") }
    val completedCount = smallWinsState.tasks.count { it.completed }
    val totalCount = smallWinsState.tasks.size.coerceAtLeast(1)
    val progress = (completedCount.toFloat() / totalCount.toFloat()).coerceIn(0f, 1f)

    CoquetteCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("small_wins_card"),
        containerColor = PinkContainer.copy(alpha = 0.85f)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = PinkPrimary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "Small Wins Daily Checklist 🎀",
                            style = MaterialTheme.typography.titleMedium,
                            color = BerryInk,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$completedCount / ${smallWinsState.tasks.size} done today • Balance: $plapsBalance Plaps",
                            style = MaterialTheme.typography.labelSmall,
                            color = PinkPrimary
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = PinkPrimary,
                trackColor = BlushSurface
            )

            Spacer(Modifier.height(10.dp))

            smallWinsState.tasks.forEach { task ->
                val rewardedToday = task.id in smallWinsState.rewardedIdsToday
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (task.completed) BlushElevated else BlushSurface)
                        .border(1.dp, RoseGoldBorder, RoundedCornerShape(14.dp))
                        .clickable { onToggleWin(task.id) }
                        .padding(horizontal = 10.dp, vertical = 10.dp)
                        .testTag("small_win_item_${task.id}"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (task.completed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = if (task.completed) "Completed" else "Mark complete",
                        tint = if (task.completed) PinkPrimary else BerryInk,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = BerryInk,
                        textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (rewardedToday) PinkPrimary else PinkAccent.copy(alpha = 0.45f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (rewardedToday) "Claimed ♡" else "+${task.plapReward} Plaps",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (rewardedToday) BlushSurface else BerryInk,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (!task.isPreset) {
                        IconButton(
                            onClick = { onDeleteWin(task.id) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove task", tint = BerryInk, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = newWinText,
                    onValueChange = { newWinText = it },
                    placeholder = { Text("Add easy ADHD win (+2 Plaps)...") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("add_small_win_input")
                )
                Button(
                    onClick = {
                        if (newWinText.isNotBlank()) {
                            onAddCustomWin(newWinText)
                            newWinText = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PinkPrimary,
                        contentColor = BlushSurface
                    ),
                    modifier = Modifier
                        .height(52.dp)
                        .testTag("add_small_win_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Small Win")
                }
            }
        }
    }
}
