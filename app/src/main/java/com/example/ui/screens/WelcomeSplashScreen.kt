package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.model.DefaultAdhdContent
import com.example.model.WelcomeSplashConfig
import com.example.ui.theme.BerryInk
import com.example.ui.theme.BlushElevated
import com.example.ui.theme.BlushSurface
import com.example.ui.theme.PinkAccent
import com.example.ui.theme.PinkContainer
import com.example.ui.theme.PinkPrimary
import com.example.ui.theme.parseHexColorSafe
import com.example.ui.theme.resolveFontFamily
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun WelcomeSplashScreen(
    config: WelcomeSplashConfig,
    onFinished: () -> Unit
) {
    val context = LocalContext.current
    val customFont = resolveFontFamily(config.fontKey, config.customFontUri, context)
    val inkColor = parseHexColorSafe(config.fontColorHex, BerryInk)
    var progressTarget by remember { mutableFloatStateOf(0f) }

    val animatedProgress by animateFloatAsState(
        targetValue = progressTarget,
        animationSpec = tween(durationMillis = 2900),
        label = "welcomeSplashTimer"
    )

    LaunchedEffect(Unit) {
        progressTarget = 1f
        delay(3000L)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BlushSurface)
            .testTag("welcome_splash_screen")
    ) {
        when {
            config.wallpaperKey == "custom" && config.customWallpaperUri.isNotBlank() -> {
                val modelData: Any = if (config.customWallpaperUri.startsWith("/")) {
                    File(config.customWallpaperUri)
                } else {
                    config.customWallpaperUri
                }
                AsyncImage(
                    model = modelData,
                    contentDescription = "Custom welcome background",
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopCenter,
                    modifier = Modifier.fillMaxSize()
                )
            }
            config.wallpaperKey == "coquette_silk" -> {
                Image(
                    painter = painterResource(id = R.drawable.img_coquette_wallpaper),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            config.wallpaperKey == "neon_candy" -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(PinkPrimary.copy(alpha = 0.7f), PinkAccent.copy(alpha = 0.65f), BlushSurface)
                            )
                        )
                )
            }
            config.wallpaperKey == "blush" -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(listOf(BlushSurface, PinkContainer, BlushElevated))
                        )
                )
            }
            else -> {
                Image(
                    painter = painterResource(id = R.drawable.img_auth_background),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopCenter,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        0.55f to Color.Transparent,
                        1.0f to BlushSurface.copy(alpha = 0.65f)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(max = 460.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = BlushSurface.copy(alpha = 0.90f),
                border = BorderStroke(1.5.dp, PinkPrimary.copy(alpha = 0.6f)),
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = DefaultAdhdContent.HEADER_TITLE,
                        fontFamily = customFont,
                        style = MaterialTheme.typography.headlineSmall,
                        color = inkColor,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = config.note.ifBlank { DefaultAdhdContent.DEFAULT_WELCOME_NOTE },
                        fontFamily = customFont,
                        style = MaterialTheme.typography.bodyLarge,
                        color = inkColor,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(14.dp))
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = PinkPrimary,
                        trackColor = PinkContainer
                    )
                    Spacer(Modifier.height(4.dp))
                    TextButton(
                        onClick = onFinished,
                        modifier = Modifier.testTag("welcome_splash_skip_btn")
                    ) {
                        Text("Entering app... (Tap to skip)", color = PinkPrimary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
