package com.chathuninimesha.coreflow.reminder

/**
 * Stable alarm and notification identifiers derived from persisted log IDs.
 * habitId occupies the high 15 bits, caseId the low 16 bits.
 */
object ReminderIds {

    fun alarmRequestCode(habitId: Long, caseId: Long): Int {
        if (habitId <= 0L || caseId <= 0L) return INVALID
        val packed = ((habitId and HABIT_MASK) shl CASE_BITS) or (caseId and CASE_MASK)
        val id = packed.toInt()
        return if (id == 0) 1 else id
    }

    fun notificationId(habitId: Long, caseId: Long): Int = alarmRequestCode(habitId, caseId)

    fun isValid(habitId: Long, caseId: Long): Boolean = habitId > 0L && caseId > 0L

    const val INVALID = -1

    private const val CASE_BITS = 16
    private const val HABIT_MASK = 0x7FFFL
    private const val CASE_MASK = 0xFFFFL
}
