package com.example.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.ScheduledAppBlockRepository
import com.example.data.local.BeSissyDatabase
import com.example.data.local.ScheduledAppBlockTaskEntity
import com.example.model.UiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ScheduledAppBlockViewModel(
    private val repository: ScheduledAppBlockRepository
) : ViewModel() {

    val tasks: StateFlow<List<ScheduledAppBlockTaskEntity>> = repository.allTasks
        .catch { error ->
            if (error is CancellationException) throw error
            _statusMessage.value = error.localizedMessage ?: "Failed to load scheduled block tasks."
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = emptyList()
        )

    val uiState: StateFlow<UiState<List<ScheduledAppBlockTaskEntity>>> = repository.allTasks
        .map<List<ScheduledAppBlockTaskEntity>, UiState<List<ScheduledAppBlockTaskEntity>>> { list ->
            if (list.isEmpty()) UiState.Empty else UiState.Success(list)
        }
        .catch { error ->
            if (error is CancellationException) throw error
            emit(UiState.Error(error.localizedMessage ?: "Failed to load scheduled block tasks."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = UiState.Loading
        )

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    fun saveTask(
        id: Long = 0L,
        packageName: String,
        appName: String,
        taskTitle: String = "",
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int,
        daysOfWeek: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7),
        enabled: Boolean = true
    ) {
        saveTaskMinutes(
            id = id,
            packageName = packageName,
            appName = appName,
            taskTitle = taskTitle,
            startMinutes = ScheduledAppBlockTaskEntity.toMinutesOfDay(startHour, startMinute),
            endMinutes = ScheduledAppBlockTaskEntity.toMinutesOfDay(endHour, endMinute),
            daysOfWeekMask = ScheduledAppBlockTaskEntity.daysMaskFromList(daysOfWeek),
            enabled = enabled
        )
    }

    fun saveTaskMinutes(
        id: Long = 0L,
        packageName: String,
        appName: String,
        taskTitle: String = "",
        startMinutes: Int,
        endMinutes: Int,
        daysOfWeekMask: Int = ScheduledAppBlockTaskEntity.ALL_DAYS_MASK,
        enabled: Boolean = true
    ) {
        viewModelScope.launch {
            repository.saveTask(
                id = id,
                packageName = packageName,
                appName = appName,
                taskTitle = taskTitle,
                startMinutes = startMinutes,
                endMinutes = endMinutes,
                daysOfWeekMask = daysOfWeekMask,
                enabled = enabled
            ).onSuccess {
                _statusMessage.value = "Scheduled block saved for $appName."
            }.onFailure { error ->
                _statusMessage.value = error.localizedMessage ?: "Could not save scheduled block."
            }
        }
    }

    fun toggleTaskEnabled(taskId: Long, enabled: Boolean) {
        viewModelScope.launch {
            repository.setTaskEnabled(taskId = taskId, enabled = enabled)
                .onFailure { error ->
                    _statusMessage.value = error.localizedMessage ?: "Could not update task state."
                }
        }
    }

    fun deleteTask(taskId: Long) {
        viewModelScope.launch {
            repository.deleteTask(taskId)
                .onSuccess {
                    _statusMessage.value = "Scheduled block removed."
                }
                .onFailure { error ->
                    _statusMessage.value = error.localizedMessage ?: "Could not delete task."
                }
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun isAppBlockedAt(
        packageName: String,
        minutesOfDay: Int,
        dayOfWeek: Int
    ): Boolean {
        return tasks.value.any { task ->
            task.packageName == packageName && task.isActiveAt(minutesOfDay, dayOfWeek)
        }
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val db = BeSissyDatabase.getInstance(context.applicationContext)
                ScheduledAppBlockViewModel(
                    repository = ScheduledAppBlockRepository(db.scheduledAppBlockDao())
                )
            }
        }
    }
}
