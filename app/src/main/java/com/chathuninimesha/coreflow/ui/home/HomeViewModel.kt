package com.chathuninimesha.coreflow.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chathuninimesha.coreflow.auth.AuthModule
import com.chathuninimesha.coreflow.data.HabitRepository
import com.chathuninimesha.coreflow.model.WorkResult
import com.chathuninimesha.coreflow.model.habits.Habit
import com.chathuninimesha.coreflow.model.logs.HabitLog
import com.chathuninimesha.coreflow.timer.ElapsedTimeCalculator
import com.chathuninimesha.coreflow.timer.SystemClock
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val greetingName: String? = null,
    val habitCount: Int = 0,
    val recentLogs: List<RecentLogItem> = emptyList(),
    val timeSinceLastLog: String? = null,
    val isEmpty: Boolean = true,
    val isError: Boolean = false
)

data class RecentLogItem(
    val habitName: String,
    val note: String,
    val date: Long
)

class HomeViewModel(
    private val repository: HabitRepository
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = repository.getHabitsFlow()
        .map { habitsResult ->
            when (habitsResult) {
                is WorkResult.ErrorResult -> HomeUiState(isError = true)
                is WorkResult.LoadingResult -> HomeUiState()
                is WorkResult.SuccessResult -> buildState(habitsResult.data)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    private fun buildState(habits: List<Habit>): HomeUiState {
        val logs = repository.getAllLogsSafely().sortedByDescending { it.date }
        val names = habits.associateBy({ it.id }, { it.name })
        val recent = logs.take(5).map { log ->
            RecentLogItem(
                habitName = names[log.habitId] ?: "Habit",
                note = log.comment,
                date = log.date
            )
        }
        val latest = logs.firstOrNull()
        val timeSince = latest?.let {
            val elapsed = ElapsedTimeCalculator.between(it.date, SystemClock.now())
            "${elapsed.daysLabel()}${elapsed.clockLabel()}"
        }
        return HomeUiState(
            greetingName = AuthModule.repository().currentProfileName(),
            habitCount = habits.size,
            recentLogs = recent,
            timeSinceLastLog = timeSince,
            isEmpty = habits.isEmpty()
        )
    }
}
