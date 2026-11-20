package com.example.ui.components

import android.net.Uri
import android.widget.VideoView
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.R
import com.example.model.UserProfile
import com.example.service.RestrictionRuntimeState
import com.example.ui.theme.BerryInk
import com.example.ui.theme.BlushSurface
import com.example.ui.theme.PinkAccent
import com.example.ui.theme.PinkContainer
import com.example.ui.theme.PinkPrimary
import com.example.ui.theme.RoseGoldBorder
import com.example.ui.theme.parseHexColorSafe
import com.example.ui.theme.resolveFontFamily
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun WallBlockScreen(
    blockedAppName: String,
    blockReasonTitle: String,
    unlockTimeHint: String,
    profile: UserProfile,
    unlockTargetMillis: Long = RestrictionRuntimeState.computeNextMidnightMillis(),
    customBgImageUri: String = "",
    onTempUnblockWithPlaps: (() -> Unit)? = null,
    onOpenEarlyExit: (() -> Unit)? = null,
    onDismissBack: () -> Unit
) {
    val context = LocalContext.current
    val customFont = resolveFontFamily(profile.wallBlockFontKey, profile.customFontUri, context)
    val fontColor = parseHexColorSafe(profile.wallBlockFontColorHex, BerryInk)
    val bgColor = parseHexColorSafe(profile.wallBlockBgHex, BlushSurface)

    val effectiveBgUri = customBgImageUri.ifBlank {
        if (profile.wallBlockMediaType == "PHOTO" || profile.wallBlockMediaType == "GIF") {
            profile.wallBlockMediaUri
        } else {
            ""
        }
    }

    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(unlockTargetMillis) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(1000L)
        }
    }

    val remainingSeconds = ((unlockTargetMillis - nowMillis) / 1000L).coerceAtLeast(0L)
    val hours = remainingSeconds / 3600L
    val mins = (remainingSeconds % 3600L) / 60L
    val secs = remainingSeconds % 60L
    val countdownFormatted = String.format(Locale.US, "%02d : %02d : %02d", hours, mins, secs)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(bgColor, PinkContainer.copy(alpha = 0.85f), bgColor)
                )
            )
            .testTag("fullscreen_wall_block_overlay")
    ) {
        // Customizable Fullscreen Background Image
        if (effectiveBgUri.isNotBlank()) {
            AsyncImage(
                model = effectiveBgUri,
                contentDescription = "Wall Block fullscreen background",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Soft tinted scrim for guaranteed legibility
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                bgColor.copy(alpha = 0.48f),
                                PinkContainer.copy(alpha = 0.58f),
                                bgColor.copy(alpha = 0.68f)
                            )
                        )
                    )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Prominent Live Countdown Timer Banner
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(BlushSurface.copy(alpha = 0.92f))
                    .border(2.dp, PinkPrimary, RoundedCornerShape(26.dp))
                    .padding(horizontal = 20.dp, vertical = 14.dp)
                    .testTag("wall_block_countdown_timer"),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "TIME UNTIL UNLOCK",
                    style = MaterialTheme.typography.labelLarge,
                    color = PinkPrimary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = countdownFormatted,
                    fontFamily = customFont,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = fontColor,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontFeatureSettings = "tnum"
                    )
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Unlock time",
                        tint = PinkPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = unlockTimeHint,
                        fontFamily = customFont,
                        style = MaterialTheme.typography.labelLarge,
                        color = fontColor,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Custom Center Media Slot: ICON, PHOTO, GIF, or VIDEO
            when (profile.wallBlockMediaType) {
                "VIDEO" if profile.wallBlockMediaUri.isNotBlank() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp)
                            .clip(RoundedCornerShape(26.dp))
                            .border(2.dp, PinkPrimary, RoundedCornerShape(26.dp))
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                VideoView(ctx).apply {
                                    setVideoURI(Uri.parse(profile.wallBlockMediaUri))
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
                }
                "PHOTO" if profile.wallBlockMediaUri.isNotBlank() && effectiveBgUri != profile.wallBlockMediaUri -> {
                    AsyncImage(
                        model = profile.wallBlockMediaUri,
                        contentDescription = "Wall block custom photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp)
                            .clip(RoundedCornerShape(26.dp))
                            .border(2.dp, PinkPrimary, RoundedCornerShape(26.dp))
                    )
                }
                "GIF" if profile.wallBlockMediaUri.isNotBlank() && effectiveBgUri != profile.wallBlockMediaUri -> {
                    AsyncImage(
                        model = profile.wallBlockMediaUri,
                        contentDescription = "Wall block custom GIF",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp)
                            .clip(RoundedCornerShape(26.dp))
                            .border(2.dp, PinkPrimary, RoundedCornerShape(26.dp))
                    )
                }
                else -> {
                    Image(
                        painter = painterResource(id = R.drawable.img_wallblock_hero),
                        contentDescription = "Heart padlock",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(144.dp)
                            .clip(CircleShape)
                            .border(3.dp, PinkPrimary, CircleShape)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            CoquetteCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = BlushSurface.copy(alpha = 0.94f),
                borderColor = PinkPrimary
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Blocked",
                            tint = PinkPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "$blockedAppName is Blocked",
                            fontFamily = customFont,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = fontColor,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = blockReasonTitle,
                        fontFamily = customFont,
                        style = MaterialTheme.typography.titleMedium,
                        color = PinkPrimary,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = profile.wallBlockReason,
                        fontFamily = customFont,
                        style = MaterialTheme.typography.bodyLarge,
                        color = fontColor,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            if (onTempUnblockWithPlaps != null) {
                Button(
                    onClick = onTempUnblockWithPlaps,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("wallblock_plap_unblock_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PinkAccent,
                        contentColor = BerryInk
                    ),
                    shape = RoundedCornerShape(26.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_glitter_bow),
                        contentDescription = "Plap bow",
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Temp Unblock 5m (5 Plaps)", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(10.dp))
            }

            if (onOpenEarlyExit != null) {
                OutlinedButton(
                    onClick = onOpenEarlyExit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("wallblock_early_exit_btn"),
                    shape = RoundedCornerShape(26.dp)
                ) {
                    Icon(Icons.Default.LockOpen, contentDescription = null, tint = BerryInk)
                    Spacer(Modifier.width(8.dp))
                    Text("3 Ways to Unlock Early", color = BerryInk, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(10.dp))
            }

            Button(
                onClick = onDismissBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("wallblock_return_btn"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PinkPrimary,
                    contentColor = BlushSurface
                ),
                shape = RoundedCornerShape(26.dp)
            ) {
                Text("Return to Be Sissy", fontWeight = FontWeight.Bold)
            }
        }
    }
}
