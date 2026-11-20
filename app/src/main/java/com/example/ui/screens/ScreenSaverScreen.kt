package com.example.ui.screens

import android.net.Uri
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.model.RestrictionSession
import com.example.model.SchedulePlan
import com.example.model.UserProfile
import com.example.service.RestrictionRuntimeState
import com.example.ui.components.HeartDigitalClockBanner
import com.example.ui.theme.BerryInk
import com.example.ui.theme.BlushSurface
import com.example.ui.theme.DarkPlumBg
import com.example.ui.theme.PinkAccent
import com.example.ui.theme.PinkPrimary
import com.example.ui.theme.parseHexColorSafe
import com.example.ui.theme.resolveFontFamily
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ScreenSaverScreen(
    profile: UserProfile,
    activeSession: RestrictionSession?,
    activeSchedule: SchedulePlan? = null,
    nowMillis: Long,
    onOpenEarlyExit: () -> Unit,
    onExitScreenSaver: () -> Unit
) {
    BackHandler(onBack = onExitScreenSaver)

    val context = LocalContext.current
    val aodFont = resolveFontFamily(profile.aodFontKey, profile.customFontUri, context)
    val aodTextColor = parseHexColorSafe(profile.aodFontColorHex, BerryInk)
    val dimAlpha = (1f - (profile.aodDimPercent / 100f)).coerceIn(0.15f, 0.88f)

    val endMillis = when {
        activeSchedule != null -> RestrictionRuntimeState.computeScheduleEndMillis(activeSchedule, nowMillis)
        activeSession != null -> activeSession.endsAtMillis
        else -> nowMillis + 7200_000L
    }
    val remainingSeconds = ((endMillis - nowMillis) / 1000L).coerceAtLeast(0L)
    val unlockAt = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(endMillis))
    val hasActiveLock = activeSchedule != null || activeSession != null

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkPlumBg)
    ) {
        when {
            profile.aodMediaType == "VIDEO" && profile.aodMediaUri.isNotBlank() -> {
                AndroidView(
                    factory = { ctx ->
                        VideoView(ctx).apply {
                            setVideoURI(Uri.parse(profile.aodMediaUri))
                            setOnPreparedListener { mp ->
                                mp.isLooping = true
                                mp.setVolume(0f, 0f)
                                start()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            (profile.aodMediaType == "PHOTO" || profile.aodMediaType == "GIF") && profile.aodMediaUri.isNotBlank() -> {
                val modelData: Any = if (profile.aodMediaUri.startsWith("/")) {
                    File(profile.aodMediaUri)
                } else {
                    profile.aodMediaUri
                }
                AsyncImage(
                    model = modelData,
                    contentDescription = "Screen saver backdrop",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Dimming veil controlled by user's AOD dim slider
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkPlumBg.copy(alpha = dimAlpha))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HeartDigitalClockBanner(
                remainingSeconds = remainingSeconds,
                unlockAtFormatted = unlockAt,
                isActive = hasActiveLock,
                isPaused = activeSession?.status == "PAUSED",
                fontFamily = aodFont,
                textColor = aodTextColor
            )

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenEarlyExit,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("aod_early_exit_btn")
                ) {
                    Icon(Icons.Default.LockOpen, contentDescription = null, tint = PinkAccent)
                    Spacer(Modifier.width(6.dp))
                    Text("Early Exit", color = PinkAccent)
                }

                Button(
                    onClick = onExitScreenSaver,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("aod_close_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary, contentColor = BlushSurface)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Exit AOD")
                }
            }
        }
    }
}
