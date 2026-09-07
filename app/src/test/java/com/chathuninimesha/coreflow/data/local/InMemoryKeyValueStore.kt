package com.chathuninimesha.coreflow.data.local

class InMemoryKeyValueStore : KeyValueStore {
    private val values = mutableMapOf<String, String>()
    var failWrites: Boolean = false

    override fun getString(key: String): String? = values[key]

    override fun putString(key: String, value: String): Boolean {
        if (failWrites) return false
        values[key] = value
        return true
    }

    override fun remove(key: String): Boolean {
        if (failWrites) return false
        values.remove(key)
        return true
    }

    override fun keys(): Set<String> = values.keys.toSet()
}
