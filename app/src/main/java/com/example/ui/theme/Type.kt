package com.example.ui.theme

import android.content.Context
import android.graphics.Typeface
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R
import java.io.File

val PlayfairDisplayFamily = FontFamily(Font(R.font.playfair_display, FontWeight.Normal))
val PacificoFamily = FontFamily(Font(R.font.pacifico, FontWeight.Normal))
val DancingScriptFamily = FontFamily(Font(R.font.dancing_script, FontWeight.Normal))
val QuicksandFamily = FontFamily(Font(R.font.quicksand, FontWeight.Normal))
val PoppinsFamily = FontFamily(Font(R.font.poppins, FontWeight.Normal))
val JetBrainsMonoFamily = FontFamily(Font(R.font.jetbrains_mono, FontWeight.Normal))

data class FontPresetOption(val key: String, val label: String)

val BuiltInFontPresets = listOf(
    FontPresetOption("playfair", "Playfair Display"),
    FontPresetOption("pacifico", "Pacifico Script"),
    FontPresetOption("dancing", "Dancing Script"),
    FontPresetOption("quicksand", "Quicksand Soft"),
    FontPresetOption("poppins", "Poppins Clean"),
    FontPresetOption("jetbrains", "JetBrains Mono"),
    FontPresetOption("custom_file", "Custom Font File (.ttf/.otf)")
)

fun resolveFontFamily(key: String, customFontPath: String = "", context: Context? = null): FontFamily {
    if (key == "custom_file" && customFontPath.isNotBlank()) {
        try {
            val file = File(customFontPath)
            if (file.exists()) {
                return FontFamily(Typeface.createFromFile(file))
            }
            if (context != null) {
                val localFile = File(context.filesDir, "custom_user_font.ttf")
                if (localFile.exists()) {
                    return FontFamily(Typeface.createFromFile(localFile))
                }
            }
        } catch (_: Exception) {
            // Fallback gracefully to bundled display font
        }
    }
    return when (key) {
        "pacifico" -> PacificoFamily
        "dancing" -> DancingScriptFamily
        "quicksand" -> QuicksandFamily
        "poppins" -> PoppinsFamily
        "jetbrains" -> JetBrainsMonoFamily
        else -> PlayfairDisplayFamily
    }
}

fun buildBeSissyTypography(
    displayFamily: FontFamily = PlayfairDisplayFamily,
    bodyFamily: FontFamily = QuicksandFamily
): Typography {
    return Typography(
        displayLarge = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 36.sp,
            lineHeight = 44.sp
        ),
        headlineMedium = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 24.sp,
            lineHeight = 32.sp
        ),
        titleLarge = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
            lineHeight = 28.sp
        ),
        titleMedium = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            lineHeight = 22.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
            lineHeight = 22.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            lineHeight = 18.sp
        ),
        labelLarge = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            lineHeight = 20.sp
        ),
        labelSmall = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            lineHeight = 16.sp
        )
    )
}
