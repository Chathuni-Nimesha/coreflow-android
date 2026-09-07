package com.chathuninimesha.coreflow.reminder

import com.chathuninimesha.coreflow.timer.Clock
import com.chathuninimesha.coreflow.timer.SystemClock

class ReminderScheduler(
    private val gateway: AlarmGateway,
    private val clock: Clock = SystemClock
) {

    fun now(): Long = clock.now()

    fun schedule(request: ReminderRequest): ScheduleResult {
        if (!ReminderIds.isValid(request.habitId, request.caseId)) {
            return ScheduleResult.MissingId
        }
        if (request.triggerAtMillis <= 0L) {
            return ScheduleResult.InvalidTime
        }
        if (request.triggerAtMillis <= clock.now()) {
            cancel(request.habitId, request.caseId)
            return ScheduleResult.SkippedExpired
        }
        val requestCode = ReminderIds.alarmRequestCode(request.habitId, request.caseId)
        val notificationId = ReminderIds.notificationId(request.habitId, request.caseId)
        return try {
            if (gateway.canScheduleExactAlarms()) {
                gateway.setExact(
                    requestCode,
                    request.triggerAtMillis,
                    request.message,
                    request.habitId,
                    request.caseId,
                    notificationId
                )
                ScheduleResult.ScheduledExact
            } else {
                gateway.setInexact(
                    requestCode,
                    request.triggerAtMillis,
                    request.message,
                    request.habitId,
                    request.caseId,
                    notificationId
                )
                ScheduleResult.ScheduledInexact
            }
        } catch (_: Exception) {
            ScheduleResult.Failed
        }
    }

    fun cancel(habitId: Long, caseId: Long): CancelResult {
        if (!ReminderIds.isValid(habitId, caseId)) {
            return CancelResult.MissingId
        }
        return try {
            gateway.cancel(ReminderIds.alarmRequestCode(habitId, caseId))
            CancelResult.Cancelled
        } catch (_: Exception) {
            CancelResult.Failed
        }
    }

    fun cancelAll(habitId: Long, caseIds: Collection<Long>): List<CancelResult> {
        return caseIds.map { cancel(habitId, it) }
    }

    fun restore(requests: List<ReminderRequest>): RestoreReport {
        var scheduled = 0
        var skippedExpired = 0
        var skippedInvalid = 0
        var failed = 0
        val seen = HashSet<Int>()
        for (request in requests) {
            val code = ReminderIds.alarmRequestCode(request.habitId, request.caseId)
            if (code == ReminderIds.INVALID || seen.contains(code)) {
                if (code == ReminderIds.INVALID) skippedInvalid++
                continue
            }
            when (schedule(request)) {
                ScheduleResult.ScheduledExact, ScheduleResult.ScheduledInexact -> {
                    seen.add(code)
                    scheduled++
                }
                ScheduleResult.SkippedExpired -> skippedExpired++
                ScheduleResult.InvalidTime, ScheduleResult.MissingId -> skippedInvalid++
                ScheduleResult.Failed, ScheduleResult.ExactAlarmUnavailable -> failed++
            }
        }
        return RestoreReport(
            scheduled = scheduled,
            skippedExpired = skippedExpired,
            skippedInvalid = skippedInvalid,
            failed = failed
        )
    }
}
