package com.chathuninimesha.coreflow.ui.logs

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chathuninimesha.coreflow.data.HabitRepository
import com.chathuninimesha.coreflow.model.DataError
import com.chathuninimesha.coreflow.model.WorkResult
import com.chathuninimesha.coreflow.model.logs.HabitLog
import com.chathuninimesha.coreflow.util.Consts.KEY_HABIT_ID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HabitLogsViewModel(
    private val repository: HabitRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private var currentHabitId: Long? = savedStateHandle[KEY_HABIT_ID]
    private lateinit var logs: Flow<WorkResult<List<HabitLog>>>
    private val numLoadingItems = MutableStateFlow(0)

    init {
        currentHabitId?.let { logs = repository.getLogsFlow(it) }
    }

    fun init(habitId: Long) {
        logs = repository.getLogsFlow(habitId)
        savedStateHandle[KEY_HABIT_ID] = habitId
        currentHabitId = habitId
    }

    val uiLogsState by lazy {
        combine(logs, numLoadingItems) { result, loadingItems ->
            when (result) {
                is WorkResult.SuccessResult -> HabitLogsUiState(
                    logs = result.data.sortedByDescending { it.date },
                    isMutating = loadingItems > 0
                )
                is WorkResult.LoadingResult -> HabitLogsUiState(isLoading = true)
                is WorkResult.ErrorResult -> HabitLogsUiState(
                    isError = true,
                    error = result.error
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = HabitLogsUiState(isLoading = true)
        )
    }

    fun updateNote(log: HabitLog, newComment: String) {
        currentHabitId?.let { habitId ->
            viewModelScope.launch {
                withLoading { repository.updateLogNote(habitId, log.id, newComment) }
            }
        }
    }

    fun deleteLog(log: HabitLog) {
        currentHabitId?.let { habitId ->
            viewModelScope.launch {
                withLoading { repository.deleteLog(habitId, log.id) }
            }
        }
    }

    fun reload() {
        currentHabitId?.let { habitId ->
            viewModelScope.launch {
                repository.reloadLogs(habitId)
            }
        }
    }

    private suspend fun withLoading(block: suspend () -> Unit) {
        try {
            numLoadingItems.value += 1
            block()
        } finally {
            numLoadingItems.value -= 1
        }
    }
}

data class HabitLogsUiState(
    val logs: List<HabitLog> = emptyList(),
    val isLoading: Boolean = false,
    val isMutating: Boolean = false,
    val isError: Boolean = false,
    val error: DataError? = null
)
