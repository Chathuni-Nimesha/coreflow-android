package com.chathuninimesha.coreflow.reminder

interface AlarmGateway {
    fun canScheduleExactAlarms(): Boolean
    fun setExact(
        requestCode: Int,
        triggerAtMillis: Long,
        message: String,
        habitId: Long,
        caseId: Long,
        notificationId: Int
    )
    fun setInexact(
        requestCode: Int,
        triggerAtMillis: Long,
        message: String,
        habitId: Long,
        caseId: Long,
        notificationId: Int
    )
    fun cancel(requestCode: Int)
}

data class NotificationAccessState(
    val granted: Boolean,
    val shouldExplain: Boolean,
    val askedBefore: Boolean
) {
    val permanentlyDenied: Boolean
        get() = !granted && askedBefore && !shouldExplain
}
