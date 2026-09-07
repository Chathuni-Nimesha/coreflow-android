package com.chathuninimesha.coreflow.timer

/**
 * Elapsed time since a stored timestamp.
 * Days are full 24-hour periods, not calendar dates, so midnight
 * does not increment the day count by itself.
 */
data class ElapsedDuration(
    val days: Long,
    val hours: Long,
    val minutes: Long,
    val seconds: Long
) {
    fun daysLabel(): String = "$days:"

    fun clockLabel(): String = String.format("%d:%02d:%02d", hours, minutes, seconds)
}

fun interface Clock {
    fun now(): Long
}

object SystemClock : Clock {
    override fun now(): Long = java.lang.System.currentTimeMillis()
}

object ElapsedTimeCalculator {
    const val DAY_MS = 86_400_000L
    const val HOUR_MS = 3_600_000L
    const val MINUTE_MS = 60_000L
    const val SECOND_MS = 1_000L

    fun between(startMillis: Long, nowMillis: Long): ElapsedDuration {
        val elapsed = (nowMillis - startMillis).coerceAtLeast(0L)
        val days = elapsed / DAY_MS
        var remainder = elapsed % DAY_MS
        val hours = remainder / HOUR_MS
        remainder %= HOUR_MS
        val minutes = remainder / MINUTE_MS
        remainder %= MINUTE_MS
        val seconds = remainder / SECOND_MS
        return ElapsedDuration(days, hours, minutes, seconds)
    }
}
