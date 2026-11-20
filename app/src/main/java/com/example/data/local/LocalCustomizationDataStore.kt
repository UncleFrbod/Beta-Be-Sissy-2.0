package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AdhdDailyInsight
import com.example.model.DefaultAdhdContent
import com.example.model.RecordedVoiceLine
import com.example.model.SmallWinTask
import com.example.model.SmallWinsDailyState
import com.example.model.WelcomeSplashConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.beSissyLocalPrefs: DataStore<Preferences> by preferencesDataStore(name = "besissy_local_prefs")

class LocalCustomizationDataStore(private val context: Context) {

    val wallBlockBgImageUri: Flow<String> = context.beSissyLocalPrefs.data.map { prefs ->
        prefs[KEY_WALL_BLOCK_BG_IMAGE_URI].orEmpty()
    }

    val voiceLines: Flow<List<RecordedVoiceLine>> = context.beSissyLocalPrefs.data.map { prefs ->
        decodeVoiceLines(prefs[KEY_VOICE_LINES_JSON].orEmpty())
    }

    val selectedVoiceLineId: Flow<String> = context.beSissyLocalPrefs.data.map { prefs ->
        prefs[KEY_SELECTED_VOICE_LINE_ID].orEmpty()
    }

    val adhdCalmMode: Flow<Boolean> = context.beSissyLocalPrefs.data.map { prefs ->
        prefs[KEY_ADHD_CALM_MODE] ?: false
    }

    val adhdBrainDumpNote: Flow<String> = context.beSissyLocalPrefs.data.map { prefs ->
        prefs[KEY_ADHD_BRAIN_DUMP].orEmpty()
    }

    val selectedChallengeId: Flow<String> = context.beSissyLocalPrefs.data.map { prefs ->
        prefs[KEY_SELECTED_CHALLENGE_ID].orEmpty().ifBlank { "pink_poise_sprint" }
    }

    val completedExitTasksCount: Flow<Long> = context.beSissyLocalPrefs.data.map { prefs ->
        prefs[KEY_COMPLETED_EXIT_TASKS_COUNT] ?: 0L
    }

    val cachedAdhdInsight: Flow<AdhdDailyInsight?> = context.beSissyLocalPrefs.data.map { prefs ->
        decodeAdhdInsight(prefs[KEY_CACHED_ADHD_INSIGHT_JSON].orEmpty())
    }

    val smallWinsState: Flow<SmallWinsDailyState> = context.beSissyLocalPrefs.data.map { prefs ->
        val today = DefaultAdhdContent.todayDateKey()
        decodeSmallWinsState(prefs[KEY_SMALL_WINS_JSON].orEmpty(), today)
    }

    val welcomeSplashConfig: Flow<WelcomeSplashConfig> = context.beSissyLocalPrefs.data.map { prefs ->
        WelcomeSplashConfig(
            enabled = prefs[KEY_WELCOME_ENABLED] ?: true,
            wallpaperKey = prefs[KEY_WELCOME_WALLPAPER_KEY].orEmpty().ifBlank { "auth_photo" },
            customWallpaperUri = prefs[KEY_WELCOME_CUSTOM_WALLPAPER_URI].orEmpty(),
            note = prefs[KEY_WELCOME_NOTE] ?: DefaultAdhdContent.DEFAULT_WELCOME_NOTE,
            fontKey = prefs[KEY_WELCOME_FONT_KEY].orEmpty().ifBlank { "playfair" },
            customFontUri = prefs[KEY_WELCOME_CUSTOM_FONT_URI].orEmpty(),
            fontColorHex = prefs[KEY_WELCOME_FONT_COLOR_HEX].orEmpty().ifBlank { "#B8255D" }
        )
    }

    suspend fun setWallBlockBgImageUri(uri: String) {
        context.beSissyLocalPrefs.edit { prefs ->
            prefs[KEY_WALL_BLOCK_BG_IMAGE_URI] = uri
        }
    }

    suspend fun setVoiceLines(lines: List<RecordedVoiceLine>) {
        context.beSissyLocalPrefs.edit { prefs ->
            prefs[KEY_VOICE_LINES_JSON] = encodeVoiceLines(lines)
        }
    }

    suspend fun setSelectedVoiceLineId(id: String) {
        context.beSissyLocalPrefs.edit { prefs ->
            prefs[KEY_SELECTED_VOICE_LINE_ID] = id
        }
    }

    suspend fun setAdhdCalmMode(enabled: Boolean) {
        context.beSissyLocalPrefs.edit { prefs ->
            prefs[KEY_ADHD_CALM_MODE] = enabled
        }
    }

    suspend fun setAdhdBrainDumpNote(note: String) {
        context.beSissyLocalPrefs.edit { prefs ->
            prefs[KEY_ADHD_BRAIN_DUMP] = note.take(240)
        }
    }

    suspend fun setSelectedChallengeId(challengeId: String) {
        context.beSissyLocalPrefs.edit { prefs ->
            prefs[KEY_SELECTED_CHALLENGE_ID] = challengeId
        }
    }

    suspend fun incrementCompletedExitTasksCount(delta: Long = 1L) {
        context.beSissyLocalPrefs.edit { prefs ->
            val current = prefs[KEY_COMPLETED_EXIT_TASKS_COUNT] ?: 0L
            prefs[KEY_COMPLETED_EXIT_TASKS_COUNT] = (current + delta).coerceAtLeast(0L)
        }
    }

    suspend fun setCachedAdhdInsight(insight: AdhdDailyInsight) {
        context.beSissyLocalPrefs.edit { prefs ->
            prefs[KEY_CACHED_ADHD_INSIGHT_JSON] = encodeAdhdInsight(insight)
        }
    }

    suspend fun setSmallWinsState(state: SmallWinsDailyState) {
        context.beSissyLocalPrefs.edit { prefs ->
            prefs[KEY_SMALL_WINS_JSON] = encodeSmallWinsState(state)
        }
    }

    suspend fun setWelcomeSplashEnabled(enabled: Boolean) {
        context.beSissyLocalPrefs.edit { prefs ->
            prefs[KEY_WELCOME_ENABLED] = enabled
        }
    }

    suspend fun setWelcomeSplashWallpaper(wallpaperKey: String, customUri: String? = null) {
        context.beSissyLocalPrefs.edit { prefs ->
            prefs[KEY_WELCOME_WALLPAPER_KEY] = wallpaperKey
            if (customUri != null) {
                prefs[KEY_WELCOME_CUSTOM_WALLPAPER_URI] = customUri
            }
        }
    }

    suspend fun setWelcomeSplashNote(note: String) {
        context.beSissyLocalPrefs.edit { prefs ->
            prefs[KEY_WELCOME_NOTE] = note.take(400)
        }
    }

    suspend fun setWelcomeSplashFontKey(fontKey: String, customFontUri: String? = null) {
        context.beSissyLocalPrefs.edit { prefs ->
            prefs[KEY_WELCOME_FONT_KEY] = fontKey
            if (customFontUri != null) {
                prefs[KEY_WELCOME_CUSTOM_FONT_URI] = customFontUri
            }
        }
    }

    suspend fun setWelcomeSplashFontColorHex(hex: String) {
        context.beSissyLocalPrefs.edit { prefs ->
            prefs[KEY_WELCOME_FONT_COLOR_HEX] = hex
        }
    }

    companion object {
        private val KEY_WALL_BLOCK_BG_IMAGE_URI = stringPreferencesKey("wall_block_bg_image_uri")
        private val KEY_VOICE_LINES_JSON = stringPreferencesKey("voice_lines_json")
        private val KEY_SELECTED_VOICE_LINE_ID = stringPreferencesKey("selected_voice_line_id")
        private val KEY_ADHD_CALM_MODE = booleanPreferencesKey("adhd_calm_mode")
        private val KEY_ADHD_BRAIN_DUMP = stringPreferencesKey("adhd_brain_dump")
        private val KEY_SELECTED_CHALLENGE_ID = stringPreferencesKey("selected_challenge_id")
        private val KEY_COMPLETED_EXIT_TASKS_COUNT = longPreferencesKey("completed_exit_tasks_count")
        private val KEY_CACHED_ADHD_INSIGHT_JSON = stringPreferencesKey("cached_adhd_insight_json")
        private val KEY_SMALL_WINS_JSON = stringPreferencesKey("small_wins_json")
        private val KEY_WELCOME_ENABLED = booleanPreferencesKey("welcome_splash_enabled")
        private val KEY_WELCOME_WALLPAPER_KEY = stringPreferencesKey("welcome_wallpaper_key")
        private val KEY_WELCOME_CUSTOM_WALLPAPER_URI = stringPreferencesKey("welcome_custom_wallpaper_uri")
        private val KEY_WELCOME_NOTE = stringPreferencesKey("welcome_custom_note")
        private val KEY_WELCOME_FONT_KEY = stringPreferencesKey("welcome_font_key")
        private val KEY_WELCOME_CUSTOM_FONT_URI = stringPreferencesKey("welcome_custom_font_uri")
        private val KEY_WELCOME_FONT_COLOR_HEX = stringPreferencesKey("welcome_font_color_hex")

        internal fun encodeVoiceLines(lines: List<RecordedVoiceLine>): String {
            val arr = JSONArray()
            lines.forEach { item ->
                arr.put(
                    JSONObject().apply {
                        put("id", item.id)
                        put("label", item.label)
                        put("filePath", item.filePath)
                        put("durationSeconds", item.durationSeconds)
                        put("createdAtMillis", item.createdAtMillis)
                    }
                )
            }
            return arr.toString()
        }

        internal fun decodeVoiceLines(raw: String): List<RecordedVoiceLine> {
            if (raw.isBlank()) return emptyList()
            return try {
                val arr = JSONArray(raw)
                buildList {
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i) ?: continue
                        val id = obj.optString("id")
                        val filePath = obj.optString("filePath")
                        if (id.isNotBlank() && filePath.isNotBlank()) {
                            add(
                                RecordedVoiceLine(
                                    id = id,
                                    label = obj.optString("label", "Feminine Voice Line"),
                                    filePath = filePath,
                                    durationSeconds = obj.optInt("durationSeconds", 1),
                                    createdAtMillis = obj.optLong("createdAtMillis", 0L)
                                )
                            )
                        }
                    }
                }
            } catch (_: Exception) {
                emptyList()
            }
        }

        internal fun encodeAdhdInsight(insight: AdhdDailyInsight): String {
            return JSONObject().apply {
                put("dateKey", insight.dateKey)
                put("dailyAffirmation", insight.dailyAffirmation)
                put("adhdFocusTip", insight.adhdFocusTip)
                put("scheduleOptimizationTip", insight.scheduleOptimizationTip)
                put("recommendedMicroSprintMinutes", insight.recommendedMicroSprintMinutes)
                put("peakDistractionWindow", insight.peakDistractionWindow)
            }.toString()
        }

        internal fun decodeAdhdInsight(raw: String): AdhdDailyInsight? {
            if (raw.isBlank()) return null
            return try {
                val obj = JSONObject(raw)
                val affirmation = obj.optString("dailyAffirmation")
                if (affirmation.isBlank()) return null
                AdhdDailyInsight(
                    dateKey = obj.optString("dateKey"),
                    dailyAffirmation = affirmation,
                    adhdFocusTip = obj.optString("adhdFocusTip"),
                    scheduleOptimizationTip = obj.optString("scheduleOptimizationTip"),
                    recommendedMicroSprintMinutes = obj.optInt("recommendedMicroSprintMinutes", 15).coerceIn(5, 60),
                    peakDistractionWindow = obj.optString("peakDistractionWindow", "Evening")
                )
            } catch (_: Exception) {
                null
            }
        }

        internal fun encodeSmallWinsState(state: SmallWinsDailyState): String {
            val tasksArr = JSONArray()
            state.tasks.forEach { t ->
                tasksArr.put(
                    JSONObject().apply {
                        put("id", t.id)
                        put("title", t.title)
                        put("completed", t.completed)
                        put("plapReward", t.plapReward)
                        put("isPreset", t.isPreset)
                    }
                )
            }
            val rewardedArr = JSONArray()
            state.rewardedIdsToday.forEach { rewardedArr.put(it) }
            return JSONObject().apply {
                put("dateKey", state.dateKey)
                put("tasks", tasksArr)
                put("rewardedIds", rewardedArr)
            }.toString()
        }

        internal fun decodeSmallWinsState(raw: String, todayDateKey: String): SmallWinsDailyState {
            if (raw.isBlank()) {
                return SmallWinsDailyState(
                    dateKey = todayDateKey,
                    tasks = DefaultAdhdContent.defaultSmallWinPresets(),
                    rewardedIdsToday = emptySet()
                )
            }
            return try {
                val obj = JSONObject(raw)
                val storedDate = obj.optString("dateKey")
                val sameDay = storedDate == todayDateKey
                val tasksArr = obj.optJSONArray("tasks")
                val parsedTasks = buildList {
                    if (tasksArr != null) {
                        for (i in 0 until tasksArr.length()) {
                            val item = tasksArr.optJSONObject(i) ?: continue
                            val id = item.optString("id")
                            val title = item.optString("title")
                            if (id.isNotBlank() && title.isNotBlank()) {
                                add(
                                    SmallWinTask(
                                        id = id,
                                        title = title,
                                        completed = if (sameDay) item.optBoolean("completed", false) else false,
                                        plapReward = item.optLong("plapReward", 2L).coerceIn(1L, 10L),
                                        isPreset = item.optBoolean("isPreset", false)
                                    )
                                )
                            }
                        }
                    }
                }.ifEmpty { DefaultAdhdContent.defaultSmallWinPresets() }

                val rewardedSet = if (sameDay) {
                    val rArr = obj.optJSONArray("rewardedIds")
                    buildSet {
                        if (rArr != null) {
                            for (i in 0 until rArr.length()) {
                                rArr.optString(i).takeIf { it.isNotBlank() }?.let { add(it) }
                            }
                        }
                    }
                } else {
                    emptySet()
                }

                SmallWinsDailyState(
                    dateKey = todayDateKey,
                    tasks = parsedTasks,
                    rewardedIdsToday = rewardedSet
                )
            } catch (_: Exception) {
                SmallWinsDailyState(
                    dateKey = todayDateKey,
                    tasks = DefaultAdhdContent.defaultSmallWinPresets(),
                    rewardedIdsToday = emptySet()
                )
            }
        }
    }
}
