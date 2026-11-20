package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.unit.dp
import com.example.model.WelcomeSplashConfig
import com.example.ui.theme.BerryInk
import com.example.ui.theme.BlushElevated
import com.example.ui.theme.BlushSurface
import com.example.ui.theme.BuiltInFontPresets
import com.example.ui.theme.FontColorPresets
import com.example.ui.theme.PinkContainer
import com.example.ui.theme.PinkPrimary
import com.example.ui.theme.RoseGoldBorder

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WelcomeSplashCustomizationCard(
    config: WelcomeSplashConfig,
    customFontAvailable: Boolean,
    onToggleEnabled: (Boolean) -> Unit,
    onSelectWallpaper: (String) -> Unit,
    onPickGalleryWallpaper: () -> Unit,
    onSaveNote: (String) -> Unit,
    onSelectFontKey: (String) -> Unit,
    onImportCustomFont: () -> Unit,
    onSelectFontColorHex: (String) -> Unit,
    onPreviewSplash: () -> Unit
) {
    var noteDraft by remember(config.note) { mutableStateOf(config.note) }

    CoquetteCard(modifier = Modifier.fillMaxWidth(), containerColor = BlushElevated) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = PinkPrimary)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Launch Welcome Page (3s)",
                            style = MaterialTheme.typography.titleMedium,
                            color = BerryInk,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Auto-shows for 3s when opening app after sign-in",
                            style = MaterialTheme.typography.bodySmall,
                            color = PinkPrimary
                        )
                    }
                }
                Switch(
                    checked = config.enabled,
                    onCheckedChange = onToggleEnabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = BlushSurface,
                        checkedTrackColor = PinkPrimary
                    ),
                    modifier = Modifier.testTag("settings_welcome_splash_toggle")
                )
            }

            Spacer(Modifier.height(10.dp))

            Text("Welcome Wallpaper", style = MaterialTheme.typography.labelLarge, color = BerryInk)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    "auth_photo" to "Sign-In Photo",
                    "coquette_silk" to "Coquette Silk",
                    "neon_candy" to "Neon Candy",
                    "blush" to "Blush Pearl"
                ).forEach { (key, label) ->
                    FilterChip(
                        selected = config.wallpaperKey == key,
                        onClick = { onSelectWallpaper(key) },
                        label = { Text(label, maxLines = 1) },
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
                onClick = onPickGalleryWallpaper,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("pick_welcome_wallpaper_btn")
            ) {
                Text("Choose Custom Gallery Photo Wallpaper")
            }

            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = noteDraft,
                onValueChange = { noteDraft = it },
                label = { Text("Custom Welcome Note") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("welcome_splash_note_input")
            )
            Spacer(Modifier.height(6.dp))
            OutlinedButton(
                onClick = { onSaveNote(noteDraft) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("save_welcome_note_btn")
            ) {
                Text("Save Welcome Note ♡")
            }

            Spacer(Modifier.height(10.dp))

            Text("Welcome Font", style = MaterialTheme.typography.labelLarge, color = BerryInk)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                BuiltInFontPresets.forEach { preset ->
                    FilterChip(
                        selected = config.fontKey == preset.key,
                        onClick = {
                            if (preset.key == "custom_file" && !customFontAvailable) {
                                onImportCustomFont()
                            } else {
                                onSelectFontKey(preset.key)
                            }
                        },
                        label = { Text(preset.label, maxLines = 1) },
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

            Text("Welcome Text Color", style = MaterialTheme.typography.labelLarge, color = BerryInk)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FontColorPresets.forEach { swatch ->
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(swatch.color)
                            .border(
                                width = if (config.fontColorHex == swatch.hex) 3.dp else 1.dp,
                                color = if (config.fontColorHex == swatch.hex) PinkPrimary else RoseGoldBorder,
                                shape = CircleShape
                            )
                            .clickable { onSelectFontColorHex(swatch.hex) }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = onPreviewSplash,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("preview_welcome_splash_btn"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PinkPrimary,
                    contentColor = BlushSurface
                )
            ) {
                Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Preview 3s Welcome Page ♡", fontWeight = FontWeight.Bold)
            }
        }
    }
}
