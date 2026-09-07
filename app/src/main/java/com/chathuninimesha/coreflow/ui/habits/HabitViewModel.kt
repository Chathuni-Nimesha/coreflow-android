package com.chathuninimesha.coreflow.ui.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chathuninimesha.coreflow.data.HabitRepository
import com.chathuninimesha.coreflow.model.DataError
import com.chathuninimesha.coreflow.model.WorkResult
import com.chathuninimesha.coreflow.model.habits.Habit
import com.chathuninimesha.coreflow.model.logs.HabitLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HabitViewModel(
    private val repository: HabitRepository
) : ViewModel() {

    private val habitsFlow = repository.getHabitsFlow()
    private val numLoadingItems = MutableStateFlow(0)

    val uiHabitsState = combine(habitsFlow, numLoadingItems) { habits, loadingItems ->
        when (habits) {
            is WorkResult.SuccessResult -> HabitsListUiState(
                habits = habits.data,
                isMutating = loadingItems > 0
            )
            is WorkResult.LoadingResult -> HabitsListUiState(isLoading = true)
            is WorkResult.ErrorResult -> HabitsListUiState(
                isError = true,
                error = habits.error
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HabitsListUiState(isLoading = true)
    )

    fun updateHabits() {
        viewModelScope.launch { repository.reloadHabits() }
    }

    fun snapshotLogs(habitId: Long): List<HabitLog> = repository.getLogsSnapshot(habitId)

    fun deleteHabit(habit: Habit) {
        viewModelScope.launch {
            withLoading { repository.deleteHabit(habit.id) }
        }
    }

    fun renameHabit(id: Long, newName: String) {
        viewModelScope.launch {
            withLoading { repository.renameHabit(id, newName) }
        }
    }

    fun addHabit(name: String) {
        viewModelScope.launch {
            withLoading { repository.createHabit(Habit(0, name)) }
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

data class HabitsListUiState(
    val habits: List<Habit> = emptyList(),
    val isLoading: Boolean = false,
    val isMutating: Boolean = false,
    val isError: Boolean = false,
    val error: DataError? = null
)
