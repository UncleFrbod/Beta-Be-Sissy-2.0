package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

// Central Be Sissy Pink Palette (never use pure #FFFFFF white; use #FFE3EE BlushSurface)
val PinkPrimary = Color(0xFFFF5C9A)
val PinkContainer = Color(0xFFFFCCE0)
val PinkAccent = Color(0xFFFF96FC)
val BerryInk = Color(0xFFB8255D)
val BlushSurface = Color(0xFFFFE3EE)
val BlushElevated = Color(0xFFFFD6E7)
val BlushDeep = Color(0xFFF9B8D4)
val DeepPlum = Color(0xFF6D1B4B)
val DarkPlumBg = Color(0xFF2B0E1D)
val DarkPlumSurface = Color(0xFF3E162B)
val RoseGoldBorder = Color(0xFFFF85B3)
val SoftErrorPink = Color(0xFFC2185B)

val LocalAccentInkColor = staticCompositionLocalOf { BerryInk }

val BeSissyShapes = Shapes(
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(34.dp)
)

fun buildBeSissyColorScheme(textInkColor: Color = BerryInk): ColorScheme = lightColorScheme(
    primary = PinkPrimary,
    onPrimary = BlushSurface,
    primaryContainer = PinkContainer,
    onPrimaryContainer = textInkColor,
    secondary = PinkAccent,
    onSecondary = DeepPlum,
    secondaryContainer = BlushElevated,
    onSecondaryContainer = textInkColor,
    tertiary = BerryInk,
    onTertiary = BlushSurface,
    background = BlushSurface,
    onBackground = textInkColor,
    surface = BlushSurface,
    onSurface = textInkColor,
    surfaceVariant = PinkContainer,
    onSurfaceVariant = textInkColor,
    outline = RoseGoldBorder,
    outlineVariant = BlushDeep,
    error = SoftErrorPink,
    onError = BlushSurface
)

val BeSissyDefaultColorScheme: ColorScheme = buildBeSissyColorScheme(BerryInk)

@Composable
fun BeSissyTheme(
    fontKey: String = "playfair",
    customFontPath: String = "",
    fontColorHex: String = "#B8255D",
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val customInk = parseHexColorSafe(fontColorHex, BerryInk)
    val displayFont = resolveFontFamily(fontKey, customFontPath, context)
    val bodyFont = if (fontKey == "custom_file" && customFontPath.isNotBlank()) {
        displayFont
    } else {
        QuicksandFamily
    }

    val colorScheme = buildBeSissyColorScheme(customInk)

    CompositionLocalProvider(LocalAccentInkColor provides customInk) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = buildBeSissyTypography(displayFont, bodyFont),
            shapes = BeSissyShapes,
            content = content
        )
    }
}
