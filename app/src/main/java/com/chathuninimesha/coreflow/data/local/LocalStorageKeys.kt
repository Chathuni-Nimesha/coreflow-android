package com.chathuninimesha.coreflow.data.local

/**
 * Tracker habit/log persistence only.
 * Auth profile/session stay in coreflow_profile and coreflow_session.
 */
object LocalStorageKeys {
    const val TRACKER_PREFS = "your_shared_prefs"
    const val HABITS = "habits"
    const val CASES_PREFIX = "cases_"

    fun casesKey(habitId: Long): String = "$CASES_PREFIX$habitId"

    fun habitIdFromCasesKey(key: String): Long? {
        if (!key.startsWith(CASES_PREFIX)) return null
        return key.removePrefix(CASES_PREFIX).toLongOrNull()
    }
}
