package com.chathuninimesha.coreflow.model

import android.content.Context
import com.chathuninimesha.coreflow.data.HabitRepository
import com.chathuninimesha.coreflow.data.local.SharedPreferencesKeyValueStore
import com.google.gson.Gson

object Repositories {

    @Volatile
    private var initialized: Boolean = false

    lateinit var habitRepository: HabitRepository
        private set

    val gsonRepository: HabitRepository
        get() = habitRepository

    fun init(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            habitRepository = HabitRepository(
                SharedPreferencesKeyValueStore(context.applicationContext),
                Gson()
            )
            initialized = true
        }
    }

    fun isInitialized(): Boolean = initialized

    fun initForTests(repository: HabitRepository) {
        if (initialized) return
        habitRepository = repository
        initialized = true
    }

    @Synchronized
    fun resetForTests() {
        initialized = false
    }
}
