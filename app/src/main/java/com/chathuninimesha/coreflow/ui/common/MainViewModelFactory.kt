package com.chathuninimesha.coreflow.ui.common

import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner
import com.chathuninimesha.coreflow.model.Repositories
import com.chathuninimesha.coreflow.ui.habits.HabitViewModel
import com.chathuninimesha.coreflow.ui.home.HomeViewModel
import com.chathuninimesha.coreflow.ui.logs.AddHabitLogViewModel
import com.chathuninimesha.coreflow.ui.logs.HabitLogsViewModel

class MainViewModelFactory(
    private val repositories: Repositories,
    owner: SavedStateRegistryOwner
) : AbstractSavedStateViewModelFactory(owner, null) {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        key: String,
        modelClass: Class<T>,
        handle: SavedStateHandle
    ): T {
        return when (modelClass) {
            HabitViewModel::class.java -> HabitViewModel(repositories.habitRepository) as T
            HabitLogsViewModel::class.java -> HabitLogsViewModel(repositories.habitRepository, handle) as T
            AddHabitLogViewModel::class.java -> AddHabitLogViewModel(repositories.habitRepository, handle) as T
            HomeViewModel::class.java -> HomeViewModel(repositories.habitRepository) as T
            else -> throw IllegalStateException("Unknown ViewModel: ${modelClass.name}")
        }
    }
}
