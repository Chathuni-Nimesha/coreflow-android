package com.chathuninimesha.coreflow.data

import com.chathuninimesha.coreflow.data.local.JsonListParser
import com.chathuninimesha.coreflow.data.local.KeyValueStore
import com.chathuninimesha.coreflow.data.local.LoadedList
import com.chathuninimesha.coreflow.data.local.LocalStorageKeys
import com.chathuninimesha.coreflow.model.DataError
import com.chathuninimesha.coreflow.model.StoreResult
import com.chathuninimesha.coreflow.model.WorkResult
import com.chathuninimesha.coreflow.model.habits.Habit
import com.chathuninimesha.coreflow.model.logs.HabitLog
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class HabitRepository(
    private val store: KeyValueStore,
    gson: Gson = Gson()
) {
    private val mutex = Mutex()
    private val parser = JsonListParser(gson)
    private val habitsType = object : TypeToken<List<Habit>>() {}.type
    private val logsType = object : TypeToken<List<HabitLog>>() {}.type

    private val habitsFlow =
        MutableStateFlow<WorkResult<List<Habit>>>(readHabitsAsWorkResult())
    private val logsMap = mutableMapOf<Long, MutableStateFlow<WorkResult<List<HabitLog>>>>()

    fun getHabitsFlow(): Flow<WorkResult<List<Habit>>> = habitsFlow

    fun getLogsFlow(habitId: Long): Flow<WorkResult<List<HabitLog>>> {
        return logsMap.getOrPut(habitId) {
            MutableStateFlow(readLogsAsWorkResult(habitId))
        }
    }

    suspend fun reloadHabits() {
        mutex.withLock {
            habitsFlow.value = readHabitsAsWorkResult()
        }
    }

    suspend fun reloadLogs(habitId: Long) {
        mutex.withLock {
            publishLogs(habitId, readLogsAsWorkResult(habitId))
        }
    }

    suspend fun createHabit(habit: Habit): StoreResult<Habit> = mutex.withLock {
        if (habit.name.isBlank()) {
            return StoreResult.Failure(DataError.Validation)
        }
        val current = when (val loaded = readHabits()) {
            is LoadedList.Corrupt -> {
                habitsFlow.value = WorkResult.ErrorResult(DataError.CorruptedData)
                return StoreResult.Failure(DataError.CorruptedData)
            }
            is LoadedList.Data -> loaded.items
        }
        val created = habit.copy(id = nextHabitId(current), name = habit.name.trim())
        val updated = current + created
        if (!writeHabits(updated)) {
            habitsFlow.value = WorkResult.ErrorResult(DataError.StorageFailure)
            return StoreResult.Failure(DataError.StorageFailure)
        }
        habitsFlow.value = WorkResult.SuccessResult(updated)
        StoreResult.Success(created)
    }

    suspend fun renameHabit(habitId: Long, newName: String): StoreResult<Habit> = mutex.withLock {
        if (newName.isBlank()) {
            return StoreResult.Failure(DataError.Validation)
        }
        val current = when (val loaded = readHabits()) {
            is LoadedList.Corrupt -> {
                habitsFlow.value = WorkResult.ErrorResult(DataError.CorruptedData)
                return StoreResult.Failure(DataError.CorruptedData)
            }
            is LoadedList.Data -> loaded.items.toMutableList()
        }
        val index = current.indexOfFirst { it.id == habitId }
        if (index == -1) {
            return StoreResult.Failure(DataError.NotFound)
        }
        current[index] = current[index].copy(name = newName.trim())
        if (!writeHabits(current)) {
            habitsFlow.value = WorkResult.ErrorResult(DataError.StorageFailure)
            return StoreResult.Failure(DataError.StorageFailure)
        }
        habitsFlow.value = WorkResult.SuccessResult(current)
        StoreResult.Success(current[index])
    }

    suspend fun deleteHabit(habitId: Long): StoreResult<Unit> = mutex.withLock {
        val current = when (val loaded = readHabits()) {
            is LoadedList.Corrupt -> {
                habitsFlow.value = WorkResult.ErrorResult(DataError.CorruptedData)
                return StoreResult.Failure(DataError.CorruptedData)
            }
            is LoadedList.Data -> loaded.items
        }
        val updated = current.filterNot { it.id == habitId }
        if (!writeHabits(updated)) {
            habitsFlow.value = WorkResult.ErrorResult(DataError.StorageFailure)
            return StoreResult.Failure(DataError.StorageFailure)
        }
        if (!store.remove(LocalStorageKeys.casesKey(habitId))) {
            habitsFlow.value = WorkResult.SuccessResult(updated)
            logsMap.remove(habitId)
            return StoreResult.Failure(DataError.StorageFailure)
        }
        habitsFlow.value = WorkResult.SuccessResult(updated)
        logsMap.remove(habitId)
        StoreResult.Success(Unit)
    }

    suspend fun createLog(habitId: Long, newLog: HabitLog): StoreResult<HabitLog> = mutex.withLock {
        if (habitId <= 0L) {
            return StoreResult.Failure(DataError.Validation)
        }
        val habits = when (val loaded = readHabits()) {
            is LoadedList.Corrupt -> {
                habitsFlow.value = WorkResult.ErrorResult(DataError.CorruptedData)
                return StoreResult.Failure(DataError.CorruptedData)
            }
            is LoadedList.Data -> loaded.items
        }
        if (habits.none { it.id == habitId }) {
            return StoreResult.Failure(DataError.NotFound)
        }
        val current = when (val loaded = readLogs(habitId)) {
            is LoadedList.Corrupt -> {
                publishLogs(habitId, WorkResult.ErrorResult(DataError.CorruptedData))
                return StoreResult.Failure(DataError.CorruptedData)
            }
            is LoadedList.Data -> loaded.items
        }
        val created = newLog.copy(
            id = nextLogId(current),
            habitId = habitId,
            comment = newLog.comment,
            date = newLog.date
        )
        if (created.date < 0L) {
            return StoreResult.Failure(DataError.Validation)
        }
        val updated = current + created
        if (!writeLogs(habitId, updated)) {
            publishLogs(habitId, WorkResult.ErrorResult(DataError.StorageFailure))
            return StoreResult.Failure(DataError.StorageFailure)
        }
        publishLogs(habitId, WorkResult.SuccessResult(updated))
        StoreResult.Success(created)
    }

    suspend fun updateLogNote(
        habitId: Long,
        logId: Long,
        newComment: String
    ): StoreResult<HabitLog> = mutex.withLock {
        val current = when (val loaded = readLogs(habitId)) {
            is LoadedList.Corrupt -> {
                publishLogs(habitId, WorkResult.ErrorResult(DataError.CorruptedData))
                return StoreResult.Failure(DataError.CorruptedData)
            }
            is LoadedList.Data -> loaded.items.toMutableList()
        }
        val index = current.indexOfFirst { it.id == logId }
        if (index == -1) {
            return StoreResult.Failure(DataError.NotFound)
        }
        current[index] = current[index].copy(comment = newComment)
        if (!writeLogs(habitId, current)) {
            publishLogs(habitId, WorkResult.ErrorResult(DataError.StorageFailure))
            return StoreResult.Failure(DataError.StorageFailure)
        }
        publishLogs(habitId, WorkResult.SuccessResult(current))
        StoreResult.Success(current[index])
    }

    suspend fun deleteLog(habitId: Long, logId: Long): StoreResult<Unit> = mutex.withLock {
        val current = when (val loaded = readLogs(habitId)) {
            is LoadedList.Corrupt -> {
                publishLogs(habitId, WorkResult.ErrorResult(DataError.CorruptedData))
                return StoreResult.Failure(DataError.CorruptedData)
            }
            is LoadedList.Data -> loaded.items
        }
        val updated = current.filterNot { it.id == logId }
        if (!writeLogs(habitId, updated)) {
            publishLogs(habitId, WorkResult.ErrorResult(DataError.StorageFailure))
            return StoreResult.Failure(DataError.StorageFailure)
        }
        publishLogs(habitId, WorkResult.SuccessResult(updated))
        StoreResult.Success(Unit)
    }

    fun getLogsSnapshot(habitId: Long): List<HabitLog> {
        return when (val loaded = readLogs(habitId)) {
            is LoadedList.Data -> loaded.items
            is LoadedList.Corrupt -> emptyList()
        }
    }

    fun getAllLogsSafely(): List<HabitLog> {
        val knownHabitIds = when (val loaded = readHabits()) {
            is LoadedList.Data -> loaded.items.map { it.id }.toSet()
            is LoadedList.Corrupt -> emptySet()
        }
        val fromKeys = store.keys().mapNotNull(LocalStorageKeys::habitIdFromCasesKey).toSet()
        val habitIds = knownHabitIds + fromKeys
        return habitIds.flatMap { habitId ->
            when (val loaded = readLogs(habitId)) {
                is LoadedList.Data -> loaded.items.filter { log ->
                    log.habitId == habitId || log.habitId <= 0L
                }.map { it.copy(habitId = habitId) }
                is LoadedList.Corrupt -> emptyList()
            }
        }
    }

    suspend fun peekHabits(): StoreResult<List<Habit>> = mutex.withLock {
        when (val loaded = readHabits()) {
            is LoadedList.Corrupt -> StoreResult.Failure(DataError.CorruptedData)
            is LoadedList.Data -> StoreResult.Success(loaded.items)
        }
    }

    private fun readHabits(): LoadedList<Habit> {
        return parser.readList(store.getString(LocalStorageKeys.HABITS), habitsType) { habit ->
            try {
                val name = habit.name
                if (habit.id > 0L && name.isNotBlank()) {
                    habit.copy(name = name.trim())
                } else {
                    null
                }
            } catch (_: NullPointerException) {
                null
            }
        }
    }

    private fun readLogs(habitId: Long): LoadedList<HabitLog> {
        return parser.readList(
            store.getString(LocalStorageKeys.casesKey(habitId)),
            logsType
        ) { item ->
            try {
                if (item.id > 0L && item.date >= 0L) {
                    item.copy(
                        habitId = if (item.habitId > 0L) item.habitId else habitId,
                        comment = item.comment
                    )
                } else {
                    null
                }
            } catch (_: NullPointerException) {
                null
            }
        }
    }

    private fun readHabitsAsWorkResult(): WorkResult<List<Habit>> {
        return when (val loaded = readHabits()) {
            is LoadedList.Corrupt -> WorkResult.ErrorResult(DataError.CorruptedData)
            is LoadedList.Data -> WorkResult.SuccessResult(loaded.items)
        }
    }

    private fun readLogsAsWorkResult(habitId: Long): WorkResult<List<HabitLog>> {
        return when (val loaded = readLogs(habitId)) {
            is LoadedList.Corrupt -> WorkResult.ErrorResult(DataError.CorruptedData)
            is LoadedList.Data -> WorkResult.SuccessResult(loaded.items)
        }
    }

    private fun writeHabits(habits: List<Habit>): Boolean {
        return store.putString(LocalStorageKeys.HABITS, parser.writeList(habits))
    }

    private fun writeLogs(habitId: Long, logs: List<HabitLog>): Boolean {
        return store.putString(LocalStorageKeys.casesKey(habitId), parser.writeList(logs))
    }

    private fun publishLogs(habitId: Long, result: WorkResult<List<HabitLog>>) {
        val flow = logsMap.getOrPut(habitId) { MutableStateFlow(result) }
        flow.value = result
    }

    private fun nextHabitId(habits: List<Habit>): Long =
        (habits.maxOfOrNull { it.id } ?: 0L) + 1L

    private fun nextLogId(logs: List<HabitLog>): Long =
        (logs.maxOfOrNull { it.id } ?: 0L) + 1L
}
