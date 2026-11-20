package com.example.ui.theme

import androidx.compose.ui.graphics.Color

data class FontColorPreset(val name: String, val hex: String, val color: Color)

val FontColorPresets = listOf(
    FontColorPreset("Berry Ink", "#B8255D", BerryInk),
    FontColorPreset("Bubblegum", "#FF5C9A", PinkPrimary),
    FontColorPreset("Neon Pink", "#FF96FC", PinkAccent),
    FontColorPreset("Deep Rose", "#8E1444", Color(0xFF8E1444)),
    FontColorPreset("Velvet Plum", "#6D1B4B", DeepPlum),
    FontColorPreset("Blush Pearl", "#FFE3EE", BlushSurface)
)

fun parseHexColorSafe(hex: String, fallback: Color = BerryInk): Color {
    return try {
        val cleaned = hex.trim().removePrefix("#")
        val parsed = cleaned.toLong(16)
        when (cleaned.length) {
            6 -> Color(0xFF000000L or parsed)
            8 -> Color(parsed)
            else -> fallback
        }
    } catch (_: Exception) {
        fallback
    }
}
