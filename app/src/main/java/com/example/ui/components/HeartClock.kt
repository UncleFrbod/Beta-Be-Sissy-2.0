package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.theme.BerryInk
import com.example.ui.theme.BlushDeep
import com.example.ui.theme.BlushElevated
import com.example.ui.theme.BlushSurface
import com.example.ui.theme.PinkAccent
import com.example.ui.theme.PinkContainer
import com.example.ui.theme.PinkPrimary
import com.example.ui.theme.RoseGoldBorder
import java.io.File

@Composable
fun CoquetteWallpaperBackground(
    wallpaperKey: String,
    customWallpaperUri: String,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BlushSurface)
    ) {
        when {
            wallpaperKey == "custom" && customWallpaperUri.isNotBlank() -> {
                val modelData: Any = if (customWallpaperUri.startsWith("/")) {
                    File(customWallpaperUri)
                } else {
                    customWallpaperUri
                }
                AsyncImage(
                    model = modelData,
                    contentDescription = "Custom pink wallpaper",
                    contentScale = ContentScale.Crop,
                    alpha = 0.42f,
                    modifier = Modifier.fillMaxSize()
                )
            }
            wallpaperKey == "coquette_silk" -> {
                Image(
                    painter = painterResource(id = R.drawable.img_coquette_wallpaper),
                    contentDescription = "Coquette silk wallpaper",
                    contentScale = ContentScale.Crop,
                    alpha = 0.50f,
                    modifier = Modifier.fillMaxSize()
                )
            }
            wallpaperKey == "neon_candy" -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    PinkAccent.copy(alpha = 0.35f),
                                    PinkContainer.copy(alpha = 0.85f),
                                    BlushSurface
                                )
                            )
                        )
                )
            }
            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(BlushSurface, PinkContainer, BlushElevated)
                            )
                        )
                )
            }
        }
        // Soft pearl-blush veil to guarantee crisp #B8255D text readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BlushSurface.copy(alpha = 0.42f))
        )
        content()
    }
}

@Composable
fun CoquetteCard(
    modifier: Modifier = Modifier,
    containerColor: Color = BlushSurface.copy(alpha = 0.90f),
    borderColor: Color = RoseGoldBorder,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.5.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        content()
    }
}

@Composable
fun HeartDigitPair(
    digits: String,
    heartSize: Dp = 84.dp,
    fontFamily: FontFamily? = null,
    digitColor: Color = BlushSurface,
    pulseScale: Float = 1f
) {
    Box(
        modifier = Modifier.size(heartSize),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width * pulseScale
            val h = size.height * pulseScale
            val left = (size.width - w) / 2f
            val top = (size.height - h) / 2f

            val heartPath = Path().apply {
                val cx = left + w / 2f
                moveTo(cx, top + h * 0.88f)
                cubicTo(
                    left + w * 0.08f, top + h * 0.62f,
                    left - w * 0.02f, top + h * 0.25f,
                    left + w * 0.27f, top + h * 0.14f
                )
                cubicTo(
                    left + w * 0.42f, top + h * 0.08f,
                    cx, top + h * 0.22f,
                    cx, top + h * 0.31f
                )
                cubicTo(
                    cx, top + h * 0.22f,
                    left + w * 0.58f, top + h * 0.08f,
                    left + w * 0.73f, top + h * 0.14f
                )
                cubicTo(
                    left + w * 1.02f, top + h * 0.25f,
                    left + w * 0.92f, top + h * 0.62f,
                    cx, top + h * 0.88f
                )
                close()
            }

            // Outer glow
            drawPath(
                path = heartPath,
                color = PinkAccent.copy(alpha = 0.38f),
                style = Stroke(width = 8f, cap = StrokeCap.Round)
            )
            // Glossy 3D Pink gradient fill
            drawPath(
                path = heartPath,
                brush = Brush.linearGradient(
                    colors = listOf(PinkAccent, PinkPrimary, BerryInk),
                    start = Offset(left, top),
                    end = Offset(left + w, top + h)
                )
            )
            // Inner pearl border
            drawPath(
                path = heartPath,
                color = BlushSurface.copy(alpha = 0.78f),
                style = Stroke(width = 3f)
            )
            // Specular highlight
            drawCircle(
                color = BlushSurface.copy(alpha = 0.45f),
                radius = w * 0.07f,
                center = Offset(left + w * 0.28f, top + h * 0.30f)
            )
        }

        Text(
            text = digits,
            fontFamily = fontFamily ?: MaterialTheme.typography.displayLarge.fontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = (heartSize.value * 0.34f).sp,
            color = digitColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 4.dp)
        )
    }
}

@Composable
fun HeartDigitalClockBanner(
    remainingSeconds: Long,
    unlockAtFormatted: String,
    isActive: Boolean,
    isPaused: Boolean,
    fontFamily: FontFamily? = null,
    textColor: Color = BerryInk,
    modifier: Modifier = Modifier
) {
    val hours = (remainingSeconds / 3600L).coerceAtLeast(0L)
    val mins = ((remainingSeconds % 3600L) / 60L).coerceAtLeast(0L)
    val secs = (remainingSeconds % 60L).coerceAtLeast(0L)

    val transition = rememberInfiniteTransition(label = "heartPulse")
    val pulse by transition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    CoquetteCard(
        modifier = modifier.fillMaxWidth(),
        containerColor = BlushSurface.copy(alpha = 0.92f),
        borderColor = PinkPrimary.copy(alpha = 0.7f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Ribbon badge
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(
                        Brush.horizontalGradient(listOf(PinkContainer, PinkAccent.copy(alpha = 0.55f), PinkContainer))
                    )
                    .border(1.dp, RoseGoldBorder, RoundedCornerShape(50))
                    .padding(horizontal = 18.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Heart ribbon",
                    tint = PinkPrimary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = when {
                        isPaused -> "Session Paused"
                        isActive -> "Time Left"
                        else -> "Ready for Focus"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = fontFamily ?: MaterialTheme.typography.titleMedium.fontFamily,
                    color = textColor
                )
                Spacer(Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Heart ribbon",
                    tint = PinkPrimary,
                    modifier = Modifier.size(14.dp)
                )
            }

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                HeartDigitPair(
                    digits = "%02d".format(hours),
                    fontFamily = fontFamily,
                    pulseScale = if (isActive && !isPaused) pulse else 1f
                )
                ClockColon(textColor = PinkPrimary)
                HeartDigitPair(
                    digits = "%02d".format(mins),
                    fontFamily = fontFamily,
                    pulseScale = if (isActive && !isPaused) pulse else 1f
                )
                ClockColon(textColor = PinkPrimary)
                HeartDigitPair(
                    digits = "%02d".format(secs),
                    fontFamily = fontFamily,
                    pulseScale = if (isActive && !isPaused) pulse else 1f
                )
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = if (isActive) {
                    "You can use your apps again at $unlockAtFormatted ♡"
                } else {
                    "Start a session to lock distracting apps ♡"
                },
                style = MaterialTheme.typography.bodyLarge,
                fontFamily = fontFamily ?: MaterialTheme.typography.bodyLarge.fontFamily,
                color = textColor,
                textAlign = TextAlign.Center
            )

            // Pearl row divider
            Spacer(Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(9) { idx ->
                    Box(
                        modifier = Modifier
                            .size(if (idx == 4) 10.dp else 6.dp)
                            .clip(CircleShape)
                            .background(if (idx == 4) PinkPrimary else BlushDeep)
                            .border(1.dp, BlushSurface, CircleShape)
                    )
                }
            }
        }
    }
}

@Composable
private fun ClockColon(textColor: Color) {
    Column(
        modifier = Modifier.padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(textColor)
        )
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(textColor)
        )
    }
}
