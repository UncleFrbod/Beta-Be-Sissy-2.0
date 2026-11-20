package com.example.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.BeSissyRepository
import com.example.data.ChatModelTier
import com.example.data.GeminiAiService
import com.example.data.ImageAspectOption
import com.example.data.ImageGenModelOption
import com.example.data.ImageSizeOption
import com.example.model.AiScheduleSuggestion
import com.example.model.AppRule
import com.example.model.ChatMessage
import com.example.model.SchedulePlan
import com.example.model.UiState
import com.example.model.UsageLogEntry
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.io.File
import java.io.FileOutputStream

sealed interface AiAnalysisUiState {
    data object Idle : AiAnalysisUiState
    data object Loading : AiAnalysisUiState
    data class Success(val summary: String) : AiAnalysisUiState
    data class Error(val message: String) : AiAnalysisUiState
}

sealed interface ImageGenUiState {
    data object Idle : ImageGenUiState
    data object Loading : ImageGenUiState
    data class Success(val bitmap: Bitmap, val savedPath: String) : ImageGenUiState
    data class Error(val message: String) : ImageGenUiState
}

class AiViewModel(
    private val repository: BeSissyRepository,
    private val aiService: GeminiAiService,
    private val currentUserId: String,
    private val filesDir: File
) : ViewModel() {

    val suggestionsState: StateFlow<UiState<List<AiScheduleSuggestion>>> =
        repository.observeAiSuggestions(currentUserId)
            .map<List<AiScheduleSuggestion>, UiState<List<AiScheduleSuggestion>>> { list ->
                if (list.isEmpty()) UiState.Empty else UiState.Success(list)
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing AI suggestions", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load AI suggestions"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = UiState.Loading
            )

    private val _analysisState = MutableStateFlow<AiAnalysisUiState>(AiAnalysisUiState.Idle)
    val analysisState: StateFlow<AiAnalysisUiState> = _analysisState.asStateFlow()

    private val _selectedChatTier = MutableStateFlow(ChatModelTier.HYDROGEN)
    val selectedChatTier: StateFlow<ChatModelTier> = _selectedChatTier.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                id = "welcome",
                role = "model",
                text = "Hi gorgeous! I'm Belle, your Be Sissy AI Focus Coach. Ask me about screen habits, healthy routines, posture, or let me analyze your schedule!",
                modelUsed = ChatModelTier.HYDROGEN.modelId
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isChatStreaming = MutableStateFlow(false)
    val isChatStreaming: StateFlow<Boolean> = _isChatStreaming.asStateFlow()

    private val _chatError = MutableStateFlow<String?>(null)
    val chatError: StateFlow<String?> = _chatError.asStateFlow()

    private val _imageGenState = MutableStateFlow<ImageGenUiState>(ImageGenUiState.Idle)
    val imageGenState: StateFlow<ImageGenUiState> = _imageGenState.asStateFlow()

    fun setChatTier(tier: ChatModelTier) {
        _selectedChatTier.value = tier
    }

    fun analyzeScreenBehavior(usageLogs: List<UsageLogEntry>, appRules: List<AppRule>) {
        viewModelScope.launch {
            _analysisState.value = AiAnalysisUiState.Loading
            try {
                val (summary, plans) = withTimeout(60_000L) {
                    aiService.analyzeBehaviorAndSuggestSchedules(currentUserId, usageLogs, appRules)
                }
                repository.saveAiSuggestions(currentUserId, plans)
                _analysisState.value = AiAnalysisUiState.Success(summary)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Behavior analysis timed out", e)
                _analysisState.value = AiAnalysisUiState.Error("Behavior analysis timed out after 60 seconds. Please retry.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Behavior analysis failed", e)
                _analysisState.value = AiAnalysisUiState.Error(e.localizedMessage ?: "Behavior analysis failed.")
            }
        }
    }

    fun applyEditableSuggestion(
        suggestion: AiScheduleSuggestion,
        editedStart: Long,
        editedEnd: Long,
        editedDailyLimit: Long
    ) {
        viewModelScope.launch {
            repository.upsertAppRule(
                currentUserId,
                AppRule(
                    userId = currentUserId,
                    packageName = suggestion.packageName,
                    appName = suggestion.appName,
                    category = "AI Planned",
                    necessaryApp = false,
                    restrictedInSession = true,
                    scheduleEnabled = true,
                    scheduleStartMinutes = editedStart,
                    scheduleEndMinutes = editedEnd,
                    dailyLimitEnabled = true,
                    dailyLimitMinutes = editedDailyLimit
                )
            )
            repository.upsertSchedulePlan(
                currentUserId,
                SchedulePlan(
                    id = "sched_ai_${suggestion.packageName.replace(Regex("[^a-zA-Z0-9_]"), "_").take(50)}",
                    userId = currentUserId,
                    name = "AI Plan: ${suggestion.appName}",
                    description = suggestion.reason,
                    enabled = true,
                    startMinutes = editedStart,
                    endMinutes = editedEnd,
                    daysOfWeek = listOf(1L, 2L, 3L, 4L, 5L, 6L, 7L),
                    modeType = "BLOCK_SELECTED",
                    allowedPackages = emptyList(),
                    restrictedPackages = listOf(suggestion.packageName),
                    customBlockReason = suggestion.reason.take(250)
                )
            )
            repository.updateAiSuggestion(
                userId = currentUserId,
                suggestionId = suggestion.id,
                startMinutes = editedStart,
                endMinutes = editedEnd,
                dailyLimitMinutes = editedDailyLimit,
                status = "APPLIED"
            )
        }
    }

    fun dismissSuggestion(suggestion: AiScheduleSuggestion) {
        viewModelScope.launch {
            repository.updateAiSuggestion(
                userId = currentUserId,
                suggestionId = suggestion.id,
                startMinutes = suggestion.suggestedStartMinutes,
                endMinutes = suggestion.suggestedEndMinutes,
                dailyLimitMinutes = suggestion.suggestedDailyLimitMinutes,
                status = "DISMISSED"
            )
        }
    }

    fun sendChatMessage(prompt: String) {
        val clean = prompt.trim()
        if (clean.isBlank() || _isChatStreaming.value) return
        val tier = _selectedChatTier.value
        val historyBefore = _chatMessages.value.drop(1) // skip synthetic welcome
        val userMsg = ChatMessage(
            id = "u_${System.currentTimeMillis()}",
            role = "user",
            text = clean
        )
        val modelMsgId = "m_${System.currentTimeMillis() + 1}"
        _chatMessages.value = _chatMessages.value + userMsg + ChatMessage(
            id = modelMsgId,
            role = "model",
            text = "",
            modelUsed = tier.modelId
        )
        _chatError.value = null
        _isChatStreaming.value = true

        viewModelScope.launch {
            val acc = StringBuilder()
            val collectedSources = linkedSetOf<String>()
            try {
                withTimeout(60_000L) {
                    aiService.streamChatReply(tier, historyBefore, clean).collect { (chunk, sources) ->
                        acc.append(chunk)
                        collectedSources.addAll(sources)
                        _chatMessages.value = _chatMessages.value.map { msg ->
                            if (msg.id == modelMsgId) {
                                msg.copy(
                                    text = acc.toString(),
                                    groundingSources = collectedSources.toList()
                                )
                            } else msg
                        }
                    }
                }
                if (acc.isBlank()) {
                    _chatError.value = "Model returned an empty response."
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Chat streaming timed out", e)
                _chatError.value = "Chat response timed out after 60 seconds."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Chat streaming failed", e)
                _chatError.value = e.localizedMessage ?: "AI chat failed."
            } finally {
                _isChatStreaming.value = false
            }
        }
    }

    fun generatePinkArt(
        modelOption: ImageGenModelOption,
        prompt: String,
        aspectOption: ImageAspectOption,
        sizeOption: ImageSizeOption
    ) {
        viewModelScope.launch {
            _imageGenState.value = ImageGenUiState.Loading
            try {
                val bitmap = withTimeout(60_000L) {
                    aiService.generateCustomWallpaper(modelOption, prompt, aspectOption, sizeOption)
                }
                if (bitmap != null) {
                    val outFile = File(filesDir, "ai_wallpaper_${System.currentTimeMillis()}.png")
                    FileOutputStream(outFile).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
                    }
                    _imageGenState.value = ImageGenUiState.Success(bitmap, outFile.absolutePath)
                } else {
                    _imageGenState.value = ImageGenUiState.Error("No image was returned by Gemini.")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Image generation timed out", e)
                _imageGenState.value = ImageGenUiState.Error("Image generation timed out after 60 seconds.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Image generation failed", e)
                _imageGenState.value = ImageGenUiState.Error(e.localizedMessage ?: "Image generation failed.")
            }
        }
    }

    companion object {
        private const val TAG = "AiViewModel"

        fun provideFactory(context: Context, currentUserId: String): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    val appContext = context.applicationContext
                    val db = FirebaseFirestore.getInstance(appContext.getString(R.string.firestore_database_id))
                    AiViewModel(
                        repository = BeSissyRepository(db),
                        aiService = GeminiAiService(),
                        currentUserId = currentUserId,
                        filesDir = appContext.filesDir
                    )
                }
            }
    }
}
