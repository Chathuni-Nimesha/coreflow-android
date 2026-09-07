package com.chathuninimesha.coreflow.timer

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class ElapsedTimeCalculatorTest {

    private val hour = ElapsedTimeCalculator.HOUR_MS
    private val minute = ElapsedTimeCalculator.MINUTE_MS

    @Test
    fun thirtyMinutesElapsed_isZeroDays() {
        val duration = ElapsedTimeCalculator.between(0L, 30 * minute)
        assertEquals(0, duration.days)
        assertEquals(0, duration.hours)
        assertEquals(30, duration.minutes)
    }

    @Test
    fun twentyThreeHoursElapsed_isZeroDays() {
        val duration = ElapsedTimeCalculator.between(0L, 23 * hour)
        assertEquals(0, duration.days)
        assertEquals(23, duration.hours)
    }

    @Test
    fun twentyFourHoursElapsed_isOneDay() {
        val duration = ElapsedTimeCalculator.between(0L, 24 * hour)
        assertEquals(1, duration.days)
        assertEquals(0, duration.hours)
    }

    @Test
    fun twentyFiveHoursElapsed_isOneDay() {
        val duration = ElapsedTimeCalculator.between(0L, 25 * hour)
        assertEquals(1, duration.days)
        assertEquals(1, duration.hours)
    }

    @Test
    fun fortyEightHoursElapsed_isTwoDays() {
        val duration = ElapsedTimeCalculator.between(0L, 48 * hour)
        assertEquals(2, duration.days)
        assertEquals(0, duration.hours)
    }

    @Test
    fun fortySevenHoursElapsed_isOneDay() {
        val duration = ElapsedTimeCalculator.between(0L, 47 * hour)
        assertEquals(1, duration.days)
        assertEquals(23, duration.hours)
    }

    @Test
    fun midnightCrossing_usesElapsedDurationNotCalendarDay() {
        val utc = TimeZone.getTimeZone("UTC")
        val start = calendar(utc, 2026, Calendar.JANUARY, 1, 23, 59).timeInMillis
        val afterMidnight = calendar(utc, 2026, Calendar.JANUARY, 2, 0, 30).timeInMillis
        val duration = ElapsedTimeCalculator.between(start, afterMidnight)
        assertEquals(0, duration.days)
        assertEquals(0, duration.hours)
        assertEquals(31, duration.minutes)
    }

    @Test
    fun monthAndYearBoundary_usesElapsedDuration() {
        val utc = TimeZone.getTimeZone("UTC")
        val start = calendar(utc, 2025, Calendar.DECEMBER, 31, 12, 0).timeInMillis
        val nextYear = calendar(utc, 2026, Calendar.JANUARY, 1, 13, 0).timeInMillis
        val duration = ElapsedTimeCalculator.between(start, nextYear)
        assertEquals(1, duration.days)
        assertEquals(1, duration.hours)
    }

    @Test
    fun appRestart_recalculatesFromTheSameTimestamp() {
        val start = 1_700_000_000_000L
        val now = start + 25 * hour
        val firstOpen = ElapsedTimeCalculator.between(start, now)
        val afterRestart = ElapsedTimeCalculator.between(start, now)
        assertEquals(firstOpen, afterRestart)
        assertEquals(1, afterRestart.days)
    }

    @Test
    fun timerLifecycle_ignoresTickCountAndUsesTimestamps() {
        val start = 10_000L
        val pausedAt = start + 23 * hour
        val whileVisible = ElapsedTimeCalculator.between(start, pausedAt)
        val whileStopped = ElapsedTimeCalculator.between(start, pausedAt)
        assertEquals(whileVisible, whileStopped)
        val afterResume = ElapsedTimeCalculator.between(start, start + 25 * hour)
        assertEquals(1, afterResume.days)
        assertEquals(0, whileStopped.days)
    }

    @Test
    fun futureStart_isClampedToZero() {
        val duration = ElapsedTimeCalculator.between(1000L, 500L)
        assertEquals(ElapsedDuration(0, 0, 0, 0), duration)
    }

    private fun calendar(
        timeZone: TimeZone,
        year: Int,
        month: Int,
        day: Int,
        hourOfDay: Int,
        minuteOfHour: Int
    ): Calendar {
        val calendar = Calendar.getInstance(timeZone)
        calendar.set(year, month, day, hourOfDay, minuteOfHour, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar
    }
}
