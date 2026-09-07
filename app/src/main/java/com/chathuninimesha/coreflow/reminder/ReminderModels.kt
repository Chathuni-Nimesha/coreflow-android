package com.chathuninimesha.coreflow.reminder

data class ReminderRequest(
    val habitId: Long,
    val caseId: Long,
    val message: String,
    val triggerAtMillis: Long
)

sealed class ScheduleResult {
    object ScheduledExact : ScheduleResult()
    object ScheduledInexact : ScheduleResult()
    object SkippedExpired : ScheduleResult()
    object InvalidTime : ScheduleResult()
    object MissingId : ScheduleResult()
    object ExactAlarmUnavailable : ScheduleResult()
    object Failed : ScheduleResult()
}

sealed class CancelResult {
    object Cancelled : CancelResult()
    object MissingId : CancelResult()
    object Failed : CancelResult()
}

data class RestoreReport(
    val scheduled: Int = 0,
    val skippedExpired: Int = 0,
    val skippedInvalid: Int = 0,
    val failed: Int = 0
)
