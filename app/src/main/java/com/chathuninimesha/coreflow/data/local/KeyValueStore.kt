package com.chathuninimesha.coreflow.data.local

interface KeyValueStore {
    fun getString(key: String): String?
    fun putString(key: String, value: String): Boolean
    fun remove(key: String): Boolean
    fun keys(): Set<String>
}
