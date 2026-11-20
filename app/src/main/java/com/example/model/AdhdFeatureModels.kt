package com.example.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SmallWinTask(
    val id: String,
    val title: String,
    val completed: Boolean = false,
    val plapReward: Long = 2L,
    val isPreset: Boolean = false
)

data class SmallWinsDailyState(
    val dateKey: String,
    val tasks: List<SmallWinTask>,
    val rewardedIdsToday: Set<String> = emptySet()
)

data class WelcomeSplashConfig(
    val enabled: Boolean = true,
    val wallpaperKey: String = "auth_photo",
    val customWallpaperUri: String = "",
    val note: String = DefaultAdhdContent.DEFAULT_WELCOME_NOTE,
    val fontKey: String = "playfair",
    val customFontUri: String = "",
    val fontColorHex: String = "#B8255D"
)

object DefaultAdhdContent {
    const val HEADER_TITLE = "Stop Gooning Beta"
    const val SUBHEADER_LINES = "You Not Man Anymore,\nYou Are Just Cock Slave For Real Men's"
    const val SISSY_MOTTO = "Get Up And Be Sissy Slut You Always Wanted"
    const val DEFAULT_WELCOME_NOTE =
        "Stop Gooning Beta\nYou Not Man Anymore, You Are Just Cock Slave For Real Men's\nGet Up And Be Sissy Slut You Always Wanted"

    fun todayDateKey(nowMillis: Long = System.currentTimeMillis()): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(nowMillis))

    fun defaultSmallWinPresets(): List<SmallWinTask> = listOf(
        SmallWinTask(
            id = "sw_check_schedule",
            title = "Review today's active Schedule plan",
            completed = false,
            plapReward = 2L,
            isPreset = true
        ),
        SmallWinTask(
            id = "sw_pomodoro_sprint",
            title = "Finish 1 Sissy visual Pomodoro block",
            completed = false,
            plapReward = 3L,
            isPreset = true
        ),
        SmallWinTask(
            id = "sw_hydrate_posture",
            title = "Drink water & hold poised posture 60s",
            completed = false,
            plapReward = 2L,
            isPreset = true
        ),
        SmallWinTask(
            id = "sw_hyperfocus_win",
            title = "Clear 1 single task in Hyper-Focus Mode",
            completed = false,
            plapReward = 3L,
            isPreset = true
        )
    )
}
