package com.chathuninimesha.coreflow.ui.logs

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.chathuninimesha.coreflow.data.HabitRepository
import com.chathuninimesha.coreflow.model.StoreResult
import com.chathuninimesha.coreflow.model.logs.HabitLog
import com.chathuninimesha.coreflow.util.Consts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AddHabitLogViewModel(
    private val repository: HabitRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private var currentHabitId: Long? = null

    fun init(habitId: Long) {
        currentHabitId = habitId
        savedStateHandle[Consts.KEY_HABIT_ID] = habitId
    }

    suspend fun createLog(log: HabitLog): Result<HabitLog> {
        val habitId = currentHabitId ?: return Result.failure(IllegalStateException("missing habit"))
        return withContext(Dispatchers.IO) {
            when (val result = repository.createLog(habitId, log)) {
                is StoreResult.Success -> Result.success(result.data)
                is StoreResult.Failure -> Result.failure(IllegalStateException(result.error.toString()))
            }
        }
    }
}
