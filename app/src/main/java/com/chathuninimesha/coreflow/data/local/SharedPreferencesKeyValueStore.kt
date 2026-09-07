package com.chathuninimesha.coreflow.data.local

import android.content.Context

class SharedPreferencesKeyValueStore(
    context: Context,
    prefsName: String = LocalStorageKeys.TRACKER_PREFS
) : KeyValueStore {

    private val prefs = context.applicationContext.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    override fun getString(key: String): String? = prefs.getString(key, null)

    override fun putString(key: String, value: String): Boolean {
        return try {
            prefs.edit().putString(key, value).commit()
        } catch (_: Exception) {
            false
        }
    }

    override fun remove(key: String): Boolean {
        return try {
            prefs.edit().remove(key).commit()
        } catch (_: Exception) {
            false
        }
    }

    override fun keys(): Set<String> = prefs.all.keys.filterNotNull().toSet()
}
