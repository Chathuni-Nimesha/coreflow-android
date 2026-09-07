package com.chathuninimesha.coreflow.data

import com.chathuninimesha.coreflow.data.local.InMemoryKeyValueStore
import com.chathuninimesha.coreflow.data.local.LocalStorageKeys
import com.chathuninimesha.coreflow.model.DataError
import com.chathuninimesha.coreflow.model.Repositories
import com.chathuninimesha.coreflow.model.StoreResult
import com.chathuninimesha.coreflow.model.WorkResult
import com.chathuninimesha.coreflow.model.habits.Habit
import com.chathuninimesha.coreflow.model.logs.HabitLog
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HabitRepositoryTest {

    private val store = InMemoryKeyValueStore()
    private val repository = HabitRepository(store)

    @After
    fun tearDown() {
        Repositories.resetForTests()
    }

    @Test
    fun createHabit_persistsReadableHabit() = runBlocking {
        val result = repository.createHabit(Habit(0, "  Run  "))
        val created = (result as StoreResult.Success).data
        assertEquals(1L, created.id)
        assertEquals("Run", created.name)
        val habits = (repository.peekHabits() as StoreResult.Success).data
        assertEquals(listOf(created), habits)
        assertTrue(store.getString(LocalStorageKeys.HABITS)!!.contains("Run"))
    }

    @Test
    fun readHabits_returnsCreatedHabits() = runBlocking {
        repository.createHabit(Habit(0, "Sleep"))
        val flowValue = repository.getHabitsFlow().first() as WorkResult.SuccessResult
        assertEquals("Sleep", flowValue.data.single().name)
    }

    @Test
    fun renameHabit_updatesName() = runBlocking {
        val created = (repository.createHabit(Habit(0, "Old")) as StoreResult.Success).data
        val renamed = (repository.renameHabit(created.id, "New") as StoreResult.Success).data
        assertEquals("New", renamed.name)
        assertEquals("New", (repository.peekHabits() as StoreResult.Success).data.single().name)
    }

    @Test
    fun deleteHabit_removesHabitAndRelatedLogs() = runBlocking {
        val habit = (repository.createHabit(Habit(0, "Training")) as StoreResult.Success).data
        repository.createLog(habit.id, HabitLog(0, "log", 1_000L, habit.id))
        assertTrue(store.getString(LocalStorageKeys.casesKey(habit.id))!!.isNotBlank())
        repository.deleteHabit(habit.id)
        assertTrue((repository.peekHabits() as StoreResult.Success).data.isEmpty())
        assertEquals(null, store.getString(LocalStorageKeys.casesKey(habit.id)))
        assertTrue(repository.getLogsSnapshot(habit.id).isEmpty())
    }

    @Test
    fun createLog_persistsUnderCasesKey() = runBlocking {
        val habit = (repository.createHabit(Habit(0, "Read")) as StoreResult.Success).data
        val created = (repository.createLog(
            habit.id,
            HabitLog(0, "chapter 1", 2_000L, 0)
        ) as StoreResult.Success).data
        assertEquals(1L, created.id)
        assertEquals(habit.id, created.habitId)
        val logs = repository.getLogsSnapshot(habit.id)
        assertEquals(1, logs.size)
        assertTrue(store.getString(LocalStorageKeys.casesKey(habit.id))!!.contains("chapter 1"))
    }

    @Test
    fun readLogs_returnsCreatedLogs() = runBlocking {
        val habit = (repository.createHabit(Habit(0, "Read")) as StoreResult.Success).data
        repository.createLog(habit.id, HabitLog(0, "a", 1L, habit.id))
        repository.createLog(habit.id, HabitLog(0, "b", 2L, habit.id))
        val logs = (repository.getLogsFlow(habit.id).first() as WorkResult.SuccessResult).data
        assertEquals(2, logs.size)
    }

    @Test
    fun editLog_updatesComment() = runBlocking {
        val habit = (repository.createHabit(Habit(0, "Read")) as StoreResult.Success).data
        val log = (repository.createLog(habit.id, HabitLog(0, "old", 1L, habit.id)) as StoreResult.Success).data
        val updated = (repository.updateLogNote(habit.id, log.id, "new") as StoreResult.Success).data
        assertEquals("new", updated.comment)
        assertEquals("new", repository.getLogsSnapshot(habit.id).single().comment)
    }

    @Test
    fun deleteLog_removesOnlyThatLog() = runBlocking {
        val habit = (repository.createHabit(Habit(0, "Read")) as StoreResult.Success).data
        val first = (repository.createLog(habit.id, HabitLog(0, "keep", 1L, habit.id)) as StoreResult.Success).data
        val second = (repository.createLog(habit.id, HabitLog(0, "drop", 2L, habit.id)) as StoreResult.Success).data
        repository.deleteLog(habit.id, second.id)
        val remaining = repository.getLogsSnapshot(habit.id)
        assertEquals(listOf(first.id), remaining.map { it.id })
    }

    @Test
    fun missingData_isEmptySuccess() = runBlocking {
        val habits = repository.peekHabits() as StoreResult.Success
        assertTrue(habits.data.isEmpty())
        assertTrue(repository.getLogsSnapshot(9).isEmpty())
    }

    @Test
    fun emptyData_isEmptySuccess() = runBlocking {
        store.putString(LocalStorageKeys.HABITS, "[]")
        store.putString(LocalStorageKeys.casesKey(1), "[]")
        assertTrue((repository.peekHabits() as StoreResult.Success).data.isEmpty())
        assertTrue(repository.getLogsSnapshot(1).isEmpty())
    }

    @Test
    fun corruptedJson_doesNotOverwriteAndReturnsError() = runBlocking {
        store.putString(LocalStorageKeys.HABITS, "{not-json")
        val peek = repository.peekHabits() as StoreResult.Failure
        assertEquals(DataError.CorruptedData, peek.error)
        val create = repository.createHabit(Habit(0, "New")) as StoreResult.Failure
        assertEquals(DataError.CorruptedData, create.error)
        assertEquals("{not-json", store.getString(LocalStorageKeys.HABITS))
        val flow = repository.getHabitsFlow().first()
        assertTrue(flow is WorkResult.ErrorResult)
    }

    @Test
    fun invalidStoredData_keepsValidItems() = runBlocking {
        store.putString(
            LocalStorageKeys.HABITS,
            """[{"id":-1,"name":""},{"id":2,"name":"Walk"}]"""
        )
        val habits = (repository.peekHabits() as StoreResult.Success).data
        assertEquals(1, habits.size)
        assertEquals("Walk", habits.single().name)
        assertEquals(2L, habits.single().id)
    }

    @Test
    fun storageFailure_returnsStorageError() = runBlocking {
        store.failWrites = true
        val result = repository.createHabit(Habit(0, "Fail")) as StoreResult.Failure
        assertEquals(DataError.StorageFailure, result.error)
        assertEquals(null, store.getString(LocalStorageKeys.HABITS))
    }

    @Test
    fun repositoryInitialization_isIdempotent() {
        Repositories.resetForTests()
        assertFalse(Repositories.isInitialized())
        Repositories.initForTests(repository)
        assertTrue(Repositories.isInitialized())
        Repositories.initForTests(HabitRepository(InMemoryKeyValueStore()))
        assertTrue(Repositories.habitRepository === repository)
    }

    @Test
    fun existingHabitsKey_remainsReadable() = runBlocking {
        store.putString(LocalStorageKeys.HABITS, """[{"id":1,"name":"Legacy Habit"}]""")
        val habits = (HabitRepository(store).peekHabits() as StoreResult.Success).data
        assertEquals("Legacy Habit", habits.single().name)
        assertEquals(1L, habits.single().id)
    }

    @Test
    fun existingCasesKey_remainsReadableAsHabitLog() = runBlocking {
        store.putString(LocalStorageKeys.HABITS, """[{"id":1,"name":"Legacy Habit"}]""")
        store.putString(
            LocalStorageKeys.casesKey(1),
            """[{"id":10,"comment":"legacy log","date":123456,"habitId":1}]"""
        )
        val logs = HabitRepository(store).getLogsSnapshot(1)
        assertEquals(10L, logs.single().id)
        assertEquals("legacy log", logs.single().comment)
        assertEquals(123456L, logs.single().date)
    }

    @Test
    fun logoutDoesNotDeleteHabitsOrLogs() = runBlocking {
        val habit = (repository.createHabit(Habit(0, "Stay")) as StoreResult.Success).data
        repository.createLog(habit.id, HabitLog(0, "keep me", 5L, habit.id))
        val habitsJson = store.getString(LocalStorageKeys.HABITS)
        val casesJson = store.getString(LocalStorageKeys.casesKey(habit.id))
        assertEquals(habitsJson, store.getString(LocalStorageKeys.HABITS))
        assertEquals(casesJson, store.getString(LocalStorageKeys.casesKey(habit.id)))
        assertEquals("Stay", (repository.peekHabits() as StoreResult.Success).data.single().name)
        assertEquals("keep me", repository.getLogsSnapshot(habit.id).single().comment)
    }

    @Test
    fun appRestart_preservesData() = runBlocking {
        val habit = (repository.createHabit(Habit(0, "Restart")) as StoreResult.Success).data
        repository.createLog(habit.id, HabitLog(0, "after reboot", 9L, habit.id))
        val restarted = HabitRepository(store)
        assertEquals("Restart", (restarted.peekHabits() as StoreResult.Success).data.single().name)
        assertEquals("after reboot", restarted.getLogsSnapshot(habit.id).single().comment)
    }

    @Test
    fun createLog_forUnknownHabit_returnsNotFound() = runBlocking {
        val result = repository.createLog(99L, HabitLog(0, "orphan", 1L, 99L)) as StoreResult.Failure
        assertEquals(DataError.NotFound, result.error)
        assertEquals(null, store.getString(LocalStorageKeys.casesKey(99)))
    }

    @Test
    fun blankHabitName_returnsValidationError() = runBlocking {
        val result = repository.createHabit(Habit(0, "   ")) as StoreResult.Failure
        assertEquals(DataError.Validation, result.error)
    }
}
