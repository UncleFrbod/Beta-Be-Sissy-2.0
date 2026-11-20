package com.example.data

import android.graphics.Bitmap
import com.example.data.local.HistoricalAppUsageEntity
import com.example.data.local.RestrictionSessionLogEntity
import com.example.model.AdhdDailyInsight
import com.example.model.AiScheduleSuggestion
import com.example.model.AppRule
import com.example.model.ChatMessage
import com.example.model.SchedulePlan
import com.example.model.StreakRewardCalculator
import com.example.model.UsageLogEntry
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.AspectRatio
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.ImageConfig
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.ImageSize
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.ThinkingLevel
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.ai.type.thinkingConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject

data class VisualProofResult(
    val approved: Boolean,
    val confidencePercent: Int,
    val feedback: String
)

enum class ChatModelTier(val modelId: String, val label: String) {
    HYDROGEN("gemini-4-hydrogen", "Gemini 4 Hydrogen (Search Grounded)"),
    NEON("gemini-4-neon", "Gemini 4 Neon (Interactive)"),
    CARBON("gemini-4-carbon", "Gemini 4 Carbon (Deep Reasoning)"),
    FLASH_LITE("gemini-3.1-flash-lite", "Gemini 3.1 Flash-Lite (Fast)")
}

enum class ImageAspectOption(val label: String, val sdkRatio: AspectRatio) {
    RATIO_1_1("1:1", AspectRatio.SQUARE_1x1),
    RATIO_2_3("2:3", AspectRatio.PORTRAIT_2x3),
    RATIO_3_2("3:2", AspectRatio.LANDSCAPE_3x2),
    RATIO_3_4("3:4", AspectRatio.PORTRAIT_3x4),
    RATIO_4_3("4:3", AspectRatio.LANDSCAPE_4x3),
    RATIO_9_16("9:16", AspectRatio.PORTRAIT_9x16),
    RATIO_16_9("16:9", AspectRatio.LANDSCAPE_16x9),
    RATIO_21_9("21:9", AspectRatio.LANDSCAPE_21x9)
}

enum class ImageSizeOption(val label: String, val sdkSize: ImageSize) {
    SIZE_1K("1K", ImageSize.SIZE_1K),
    SIZE_2K("2K", ImageSize.SIZE_2K),
    SIZE_4K("4K", ImageSize.SIZE_4K)
}

enum class ImageGenModelOption(val modelId: String, val label: String) {
    NANO_BANANA("gemini-nano-banana-2.1", "Nano Banana 2.1 (Fast)"),
    PRO_IMAGE("gemini-3-pro-image-preview", "Gemini 3 Pro Image (Studio)")
}

class GeminiAiService {

    private val visualProofSchema = Schema.obj(
        properties = mapOf(
            "approved" to Schema.boolean(description = "Whether the captured photo satisfies the user's proof criteria"),
            "confidencePercent" to Schema.integer(description = "Confidence score from 0 to 100"),
            "feedback" to Schema.string(description = "Warm, concise explanation of what was seen in the image")
        )
    )

    private val schedulePlanSchema = Schema.obj(
        properties = mapOf(
            "summary" to Schema.string(description = "Short behavioral observation summary"),
            "suggestions" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "appName" to Schema.string(description = "App display name"),
                        "packageName" to Schema.string(description = "Android package name"),
                        "suggestedStartMinutes" to Schema.integer(description = "Start minute of day 0..1439"),
                        "suggestedEndMinutes" to Schema.integer(description = "End minute of day 0..1439"),
                        "suggestedDailyLimitMinutes" to Schema.integer(description = "Recommended daily cap in minutes 5..1440"),
                        "reason" to Schema.string(description = "Why this restriction schedule helps")
                    )
                ),
                description = "Editable restriction plans based on screen behavior"
            )
        )
    )

    // 1. Visual Proof verification using gemini-4-neon
    private val visionProofModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-4-neon",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = visualProofSchema
            }
        )
    }

    // 2. Deep Behavior Analysis using gemini-4-carbon with ThinkingLevel.HIGH (no maxOutputTokens)
    private val behaviorAnalysisModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-4-carbon",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = schedulePlanSchema
                thinkingConfig = thinkingConfig {
                    thinkingLevel = ThinkingLevel.HIGH
                }
            }
        )
    }

    private val adhdDailyInsightSchema = Schema.obj(
        properties = mapOf(
            "dailyAffirmation" to Schema.string(description = "Warm, uplifting, Sissy-themed daily motivational message referencing their real progress"),
            "adhdFocusTip" to Schema.string(description = "Practical, dopamine-friendly micro-action tailored for severe ADHD (under 25 words)"),
            "scheduleOptimizationTip" to Schema.string(description = "Concrete schedule adjustment based on weekly usage peaks and distraction times"),
            "recommendedMicroSprintMinutes" to Schema.integer(description = "Recommended next focus sprint length in minutes: 10, 15, 20, or 25"),
            "peakDistractionWindow" to Schema.string(description = "Short label for peak distraction window e.g. '21:00 - 23:00'")
        )
    )

    // 3. Daily ADHD + Sissy Coach weekly report analyzer using gemini-3.8-flash
    private val adhdInsightModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = adhdDailyInsightSchema
            }
        )
    }

    suspend fun verifyVisualProof(
        capturedPhoto: Bitmap,
        proofDescription: String,
        sampleReference: Bitmap? = null
    ): VisualProofResult {
        val response = visionProofModel.generateContent(
            content {
                text(
                    "You are verifying a focus session exit proof photo. " +
                        "User's required proof description: \"$proofDescription\". " +
                        "Inspect the captured photo carefully and decide if it genuinely matches the requirement."
                )
                if (sampleReference != null) {
                    text("Reference example photo provided by the user:")
                    image(sampleReference)
                }
                text("Captured verification photo:")
                image(capturedPhoto)
            }
        )
        val rawJson = response.text ?: throw IllegalStateException("AI returned an empty response.")
        val obj = JSONObject(rawJson)
        return VisualProofResult(
            approved = obj.optBoolean("approved", false),
            confidencePercent = obj.optInt("confidencePercent", 0),
            feedback = obj.optString("feedback", "Verification completed.")
        )
    }

    suspend fun verifyPhysicalTaskPhoto(
        capturedPhoto: Bitmap,
        taskName: String,
        taskInstruction: String,
        targetCount: Int
    ): VisualProofResult {
        val response = visionProofModel.generateContent(
            content {
                text(
                    "You are verifying a physical fitness/posture task in the 'End Session Tasks' challenge. " +
                        "Task: \"$taskName\" (target: $targetCount). " +
                        "Instructions: \"$taskInstruction\". " +
                        "Inspect the photo from the user's camera and verify whether the user is visibly performing or positioned in the proper physical stance/exercise (for squats: squat stance/lowered hip position or workout posture; for posture walk: standing/heeled walking posture or full-body stride). " +
                        "Respond warmly, encouragingly, and honestly in JSON."
                )
                text("Camera capture for $taskName:")
                image(capturedPhoto)
            }
        )
        val rawJson = response.text ?: throw IllegalStateException("AI returned an empty response.")
        val obj = JSONObject(rawJson)
        return VisualProofResult(
            approved = obj.optBoolean("approved", false),
            confidencePercent = obj.optInt("confidencePercent", 0),
            feedback = obj.optString("feedback", "Physical task check completed.")
        )
    }

    suspend fun analyzeBehaviorAndSuggestSchedules(
        userId: String,
        usageLogs: List<UsageLogEntry>,
        appRules: List<AppRule>
    ): Pair<String, List<AiScheduleSuggestion>> {
        val logSummary = if (usageLogs.isEmpty()) {
            "No recent logs recorded yet. Known configured apps: " +
                appRules.joinToString { "${it.appName} (${it.packageName})" }
        } else {
            usageLogs.take(25).joinToString("\n") {
                "- ${it.appName} (${it.packageName}): ${it.screenTimeMinutes}m active, ${it.restrictedMinutes}m restricted at hour ${it.hourOfDay}:00 on day ${it.dayOfWeek}"
            }
        }

        val prompt = """
            Analyze the user's screen time behavior and unusual usage spikes (especially late-night hours or heavy social/gaming usage).
            Recommend 2 to 4 practical, editable restriction schedules and daily time limits.
            Available apps / logs:
            $logSummary
        """.trimIndent()

        val response = behaviorAnalysisModel.generateContent(prompt)
        val rawJson = response.text ?: throw IllegalStateException("AI returned an empty analysis.")
        val json = JSONObject(rawJson)
        val summary = json.optString("summary", "Behavior analysis completed.")
        val arr = json.optJSONArray("suggestions")
        val list = buildList {
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    val item = arr.getJSONObject(i)
                    add(
                        AiScheduleSuggestion(
                            userId = userId,
                            appName = item.optString("appName", "App"),
                            packageName = item.optString("packageName", "com.android.chrome"),
                            suggestedStartMinutes = item.optLong("suggestedStartMinutes", 1320L).coerceIn(0L, 1439L),
                            suggestedEndMinutes = item.optLong("suggestedEndMinutes", 60L).coerceIn(0L, 1439L),
                            suggestedDailyLimitMinutes = item.optLong("suggestedDailyLimitMinutes", 45L).coerceIn(5L, 1440L),
                            reason = item.optString("reason", "Helps reduce distracting screen spikes."),
                            status = "PENDING"
                        )
                    )
                }
            }
        }
        return summary to list
    }

    suspend fun generateDailyAdhdCoachInsights(
        displayName: String,
        dailyStreak: Long,
        weeklyUsage: List<HistoricalAppUsageEntity>,
        weeklySessions: List<RestrictionSessionLogEntity>,
        schedules: List<SchedulePlan>
    ): AdhdDailyInsight {
        val totalScreen = weeklyUsage.sumOf { it.screenTimeMinutes }
        val totalRestricted = weeklyUsage.sumOf { it.restrictedMinutes }
        val topAppsText = weeklyUsage.groupBy { it.appName }
            .entries
            .sortedByDescending { e -> e.value.sumOf { it.screenTimeMinutes } }
            .take(5)
            .joinToString(", ") { (app, entries) ->
                "$app (${entries.sumOf { it.screenTimeMinutes }}m active, ${entries.sumOf { it.restrictedMinutes }}m blocked)"
            }
            .ifBlank { "No heavy usage apps logged yet" }

        val peakHour = weeklyUsage.groupBy { it.hourOfDay }
            .maxByOrNull { (_, items) -> items.sumOf { it.screenTimeMinutes } }
            ?.key

        val completedCount = weeklySessions.count { it.outcome == "COMPLETED" }
        val earlyCount = weeklySessions.count { it.outcome == "ENDED_EARLY" }
        val activeSchedulesText = schedules.joinToString(", ") {
            "${it.name} (${if (it.enabled) "ON" else "OFF"})"
        }.ifBlank { "None yet" }

        val prompt = """
            You are Belle, a warm, glamorous, ADHD-specialized Sissy Focus Coach.
            The user ($displayName) has severe ADHD (struggles with time blindness, task initiation paralysis, and dopamine scrolling).
            Analyze their past 7 days of weekly report data and return personalized daily guidance:
            - Weekly Active Screen Time: ${totalScreen}m
            - Weekly Restricted/Protected Time: ${totalRestricted}m
            - Current Daily Streak: $dailyStreak days
            - Sessions Completed: $completedCount | Ended Early: $earlyCount
            - Top Apps: $topAppsText
            - Peak Usage Hour: ${peakHour?.let { "${it}:00" } ?: "Not recorded yet"}
            - Schedules Configured: $activeSchedulesText

            Tone & Rules:
            - Zero shame, warm Sissy-themed encouragement ("princess", poised, radiant, self-control glow).
            - Keep each tip punchy, concrete, and ADHD-friendly (small steps, clear timers, external visual cues).
            - Pick recommendedMicroSprintMinutes from 10, 15, 20, or 25 based on their early-exit pattern.
        """.trimIndent()

        val response = adhdInsightModel.generateContent(prompt)
        val rawJson = response.text ?: throw IllegalStateException("Gemini returned an empty coaching insight.")
        val json = JSONObject(rawJson)
        return AdhdDailyInsight(
            dateKey = StreakRewardCalculator.formatDateKey(),
            dailyAffirmation = json.optString(
                "dailyAffirmation",
                "You're glowing today, $displayName! Every tiny focus step counts ♡"
            ),
            adhdFocusTip = json.optString(
                "adhdFocusTip",
                "Start a 10-minute micro-sprint right now—starting small unlocks ADHD momentum!"
            ),
            scheduleOptimizationTip = json.optString(
                "scheduleOptimizationTip",
                "Protect your peak distraction window with an automatic evening schedule."
            ),
            recommendedMicroSprintMinutes = json.optInt("recommendedMicroSprintMinutes", 15).coerceIn(10, 25),
            peakDistractionWindow = json.optString(
                "peakDistractionWindow",
                peakHour?.let { "%02d:00 - %02d:00".format(it, (it + 2) % 24) } ?: "21:00 - 23:00"
            )
        )
    }

    fun streamChatReply(
        tier: ChatModelTier,
        history: List<ChatMessage>,
        userPrompt: String
    ): Flow<Pair<String, List<String>>> {
        val systemInstruction = content {
            text(
                "You are Belle, the supportive, glamorous, pink-aesthetic Sissy & Severe-ADHD Focus Coach inside the Be Sissy Screen Time Out app. " +
                    "The user has severe ADHD (time blindness, dopamine-seeking scrolling, task initiation paralysis). " +
                    "Always break advice into tiny 1-to-15 minute micro-steps, use bullet points, avoid walls of text, give gentle dopamine rewards, and keep an uplifting, zero-shame tone."
            )
        }
        val tools = if (tier == ChatModelTier.HYDROGEN) {
            listOf(Tool.googleSearch())
        } else {
            emptyList()
        }
        val chatModel = Firebase.ai.generativeModel(
            modelName = tier.modelId,
            systemInstruction = systemInstruction,
            tools = tools
        )
        val sdkHistory: List<Content> = history.map { msg ->
            content(role = msg.role) { text(msg.text) }
        }
        val chat = chatModel.startChat(history = sdkHistory)
        return chat.sendMessageStream(userPrompt).map { chunk ->
            val sources = chunk.candidates.firstOrNull()
                ?.groundingMetadata
                ?.groundingChunks
                ?.mapNotNull { it.web?.title ?: it.web?.uri }
                .orEmpty()
            chunk.text.orEmpty() to sources
        }
    }

    suspend fun generateCustomWallpaper(
        modelOption: ImageGenModelOption,
        prompt: String,
        aspectOption: ImageAspectOption,
        sizeOption: ImageSizeOption
    ): Bitmap? {
        val config = generationConfig {
            responseModalities = listOf(ResponseModality.IMAGE)
            imageConfig = ImageConfig.builder()
                .setAspectRatio(aspectOption.sdkRatio)
                .setImageSize(sizeOption.sdkSize)
                .build()
        }
        val imageModel = Firebase.ai.generativeModel(
            modelName = modelOption.modelId,
            generationConfig = config
        )
        val response = imageModel.generateContent(prompt)
        val imagePart = response.candidates.firstOrNull()
            ?.content?.parts?.filterIsInstance<ImagePart>()
            ?.firstOrNull()
        return imagePart?.image
    }
}
