package com.chathuninimesha.coreflow.reminder

import com.chathuninimesha.coreflow.timer.Clock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeClock(private var nowMillis: Long) : Clock {
    override fun now(): Long = nowMillis
    fun advance(delta: Long) {
        nowMillis += delta
    }
}

class FakeAlarmGateway(
    var exactAllowed: Boolean = true,
    var failSchedule: Boolean = false
) : AlarmGateway {
    data class ScheduledAlarm(
        val requestCode: Int,
        val triggerAtMillis: Long,
        val exact: Boolean,
        val message: String,
        val habitId: Long,
        val caseId: Long,
        val notificationId: Int
    )

    val scheduled = linkedMapOf<Int, ScheduledAlarm>()
    val cancelled = mutableListOf<Int>()

    override fun canScheduleExactAlarms(): Boolean = exactAllowed

    override fun setExact(
        requestCode: Int,
        triggerAtMillis: Long,
        message: String,
        habitId: Long,
        caseId: Long,
        notificationId: Int
    ) {
        if (failSchedule) throw IllegalStateException("schedule failed")
        scheduled[requestCode] = ScheduledAlarm(
            requestCode, triggerAtMillis, true, message, habitId, caseId, notificationId
        )
    }

    override fun setInexact(
        requestCode: Int,
        triggerAtMillis: Long,
        message: String,
        habitId: Long,
        caseId: Long,
        notificationId: Int
    ) {
        if (failSchedule) throw IllegalStateException("schedule failed")
        scheduled[requestCode] = ScheduledAlarm(
            requestCode, triggerAtMillis, false, message, habitId, caseId, notificationId
        )
    }

    override fun cancel(requestCode: Int) {
        scheduled.remove(requestCode)
        cancelled.add(requestCode)
    }
}

class ReminderSchedulerTest {

    private val clock = FakeClock(1_000_000L)

    private fun scheduler(gateway: FakeAlarmGateway = FakeAlarmGateway()) =
        ReminderScheduler(gateway, clock) to gateway

    @Test
    fun createOneReminder_usesUniqueIds() {
        val (scheduler, gateway) = scheduler()
        val result = scheduler.schedule(request(habitId = 1, caseId = 4, trigger = clock.now() + 60_000))
        assertEquals(ScheduleResult.ScheduledExact, result)
        assertEquals(1, gateway.scheduled.size)
        val alarm = gateway.scheduled.values.single()
        assertEquals(ReminderIds.alarmRequestCode(1, 4), alarm.requestCode)
        assertEquals(ReminderIds.notificationId(1, 4), alarm.notificationId)
        assertNotEquals(0, alarm.requestCode)
        assertNotEquals(1, alarm.notificationId)
    }

    @Test
    fun createMultipleReminders_doNotOverwriteEachOther() {
        val (scheduler, gateway) = scheduler()
        scheduler.schedule(request(1, 1, clock.now() + 10_000, "A"))
        scheduler.schedule(request(1, 2, clock.now() + 20_000, "B"))
        scheduler.schedule(request(2, 1, clock.now() + 30_000, "C"))
        assertEquals(3, gateway.scheduled.size)
        val messages = gateway.scheduled.values.map { it.message }.toSet()
        assertEquals(setOf("A", "B", "C"), messages)
        val codes = gateway.scheduled.keys
        assertEquals(3, codes.size)
    }

    @Test
    fun editReminder_updatesSameId() {
        val (scheduler, gateway) = scheduler()
        val original = request(3, 8, clock.now() + 10_000, "old")
        scheduler.schedule(original)
        val updated = original.copy(message = "new", triggerAtMillis = clock.now() + 50_000)
        scheduler.schedule(updated)
        assertEquals(1, gateway.scheduled.size)
        val alarm = gateway.scheduled.values.single()
        assertEquals("new", alarm.message)
        assertEquals(clock.now() + 50_000, alarm.triggerAtMillis)
        assertEquals(ReminderIds.alarmRequestCode(3, 8), alarm.requestCode)
    }

    @Test
    fun deleteReminder_cancelsCorrectReminder() {
        val (scheduler, gateway) = scheduler()
        scheduler.schedule(request(1, 1, clock.now() + 10_000, "keep"))
        scheduler.schedule(request(1, 2, clock.now() + 20_000, "remove"))
        val cancel = scheduler.cancel(1, 2)
        assertEquals(CancelResult.Cancelled, cancel)
        assertEquals(1, gateway.scheduled.size)
        assertEquals("keep", gateway.scheduled.values.single().message)
        assertEquals(listOf(ReminderIds.alarmRequestCode(1, 2)), gateway.cancelled)
    }

    @Test
    fun cancelMissingId_returnsMissingId() {
        val (scheduler, _) = scheduler()
        assertEquals(CancelResult.MissingId, scheduler.cancel(0, 1))
    }

    @Test
    fun notificationPermissionDenied_doesNotPreventScheduling() {
        val access = NotificationAccessState(
            granted = false,
            shouldExplain = false,
            askedBefore = true
        )
        assertTrue(access.permanentlyDenied)
        val (scheduler, gateway) = scheduler()
        val result = scheduler.schedule(request(1, 1, clock.now() + 5_000, "later"))
        assertEquals(ScheduleResult.ScheduledExact, result)
        assertEquals(1, gateway.scheduled.size)
    }

    @Test
    fun exactAlarmUnavailable_schedulesInexact() {
        val gateway = FakeAlarmGateway(exactAllowed = false)
        val scheduler = ReminderScheduler(gateway, clock)
        val result = scheduler.schedule(request(4, 9, clock.now() + 5_000, "inexact"))
        assertEquals(ScheduleResult.ScheduledInexact, result)
        assertFalse(gateway.scheduled.values.single().exact)
    }

    @Test
    fun rebootRestore_schedulesFutureAndSkipsExpired() {
        val (scheduler, gateway) = scheduler()
        val report = scheduler.restore(
            listOf(
                request(1, 1, clock.now() - 1_000, "expired"),
                request(1, 2, clock.now() + 10_000, "future-a"),
                request(1, 3, clock.now() + 20_000, "future-b"),
                request(0, 1, clock.now() + 30_000, "invalid")
            )
        )
        assertEquals(2, report.scheduled)
        assertEquals(1, report.skippedExpired)
        assertEquals(1, report.skippedInvalid)
        assertEquals(0, report.failed)
        assertEquals(2, gateway.scheduled.size)
    }

    @Test
    fun restoreTwice_doesNotCreateDuplicates() {
        val (scheduler, gateway) = scheduler()
        val reminders = listOf(
            request(1, 1, clock.now() + 10_000, "one"),
            request(1, 2, clock.now() + 20_000, "two")
        )
        scheduler.restore(reminders)
        scheduler.restore(reminders)
        assertEquals(2, gateway.scheduled.size)
    }

    @Test
    fun scheduleFailure_returnsFailed() {
        val gateway = FakeAlarmGateway(failSchedule = true)
        val scheduler = ReminderScheduler(gateway, clock)
        assertEquals(
            ScheduleResult.Failed,
            scheduler.schedule(request(1, 1, clock.now() + 5_000, "x"))
        )
    }

    @Test
    fun reminderIds_areStableAndUnique() {
        val first = ReminderIds.alarmRequestCode(2, 5)
        val second = ReminderIds.alarmRequestCode(2, 5)
        val other = ReminderIds.alarmRequestCode(2, 6)
        assertEquals(first, second)
        assertNotEquals(first, other)
        assertTrue(first > 0)
        assertNotEquals(0, first)
        assertNotEquals(1, ReminderIds.notificationId(1, 1))
    }

    private fun request(
        habitId: Long,
        caseId: Long,
        trigger: Long,
        message: String = "note"
    ) = ReminderRequest(habitId, caseId, message, trigger)
}
