package com.chathuninimesha.coreflow.data.demo

import android.content.Context
import android.content.pm.ApplicationInfo
import com.chathuninimesha.coreflow.data.HabitRepository
import com.chathuninimesha.coreflow.data.local.LocalStorageKeys
import com.chathuninimesha.coreflow.model.StoreResult
import com.chathuninimesha.coreflow.model.habits.Habit
import com.chathuninimesha.coreflow.model.logs.HabitLog
import kotlinx.coroutines.runBlocking
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * DEBUG-only sample data for portfolio demos.
 * Never runs in non-debuggable (release) builds and never overwrites existing habits.
 */
object DemoDataSeeder {

    private const val PREFS_FLAG = "demo_data_seeded_v1"

    fun seedIfNeeded(context: Context, repository: HabitRepository) {
        if (!isDebuggable(context)) return

        val prefs = context.applicationContext
            .getSharedPreferences(LocalStorageKeys.TRACKER_PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(PREFS_FLAG, false)) return

        runBlocking {
            when (val habits = repository.peekHabits()) {
                is StoreResult.Failure -> return@runBlocking
                is StoreResult.Success -> if (habits.data.isNotEmpty()) {
                    // Real user data already present — mark so we never inject later.
                    prefs.edit().putBoolean(PREFS_FLAG, true).apply()
                    return@runBlocking
                }
            }

            val now = System.currentTimeMillis()
            val specs = listOf(
                HabitSpec(
                    "Morning Workout",
                    listOf(
                        LogSpec("Full-body warm-up and mobility", daysAgo = 0, hour = 7, minute = 15),
                        LogSpec("Bodyweight circuit, 25 min", daysAgo = 2, hour = 7, minute = 5),
                        LogSpec("Light jog + core finisher", daysAgo = 4, hour = 6, minute = 50),
                        LogSpec("Push-up and squat sets", daysAgo = 6, hour = 7, minute = 20),
                        LogSpec("Morning flow, felt strong", daysAgo = 9, hour = 7, minute = 10)
                    )
                ),
                HabitSpec(
                    "Strength Training",
                    listOf(
                        LogSpec("Upper body: bench and rows", daysAgo = 1, hour = 18, minute = 30),
                        LogSpec("Lower body: squats and RDLs", daysAgo = 3, hour = 18, minute = 15),
                        LogSpec("Pull day — lat pulldowns", daysAgo = 5, hour = 19, minute = 0),
                        LogSpec("Push day — overhead press", daysAgo = 8, hour = 18, minute = 45),
                        LogSpec("Full strength session, 45 min", daysAgo = 11, hour = 17, minute = 50)
                    )
                ),
                HabitSpec(
                    "Cardio",
                    listOf(
                        LogSpec("Steady bike ride, 30 min", daysAgo = 0, hour = 12, minute = 20),
                        LogSpec("Interval run, 20 min", daysAgo = 2, hour = 17, minute = 40),
                        LogSpec("Brisk walk outdoors", daysAgo = 5, hour = 12, minute = 5),
                        LogSpec("Elliptical recovery session", daysAgo = 7, hour = 16, minute = 30),
                        LogSpec("Tempo run around the park", daysAgo = 10, hour = 17, minute = 15)
                    )
                ),
                HabitSpec(
                    "Stretching",
                    listOf(
                        LogSpec("Evening hip and hamstring stretch", daysAgo = 0, hour = 21, minute = 10),
                        LogSpec("Post-workout cool-down", daysAgo = 1, hour = 19, minute = 45),
                        LogSpec("Yoga-inspired flow, 15 min", daysAgo = 3, hour = 21, minute = 0),
                        LogSpec("Shoulder and back mobility", daysAgo = 6, hour = 20, minute = 30),
                        LogSpec("Full-body stretch before bed", daysAgo = 8, hour = 21, minute = 20)
                    )
                )
            )

            for (spec in specs) {
                val created = when (val result = repository.createHabit(Habit(0, spec.name))) {
                    is StoreResult.Success -> result.data
                    is StoreResult.Failure -> return@runBlocking
                }
                for (log in spec.logs) {
                    val timestamp = atLocalTime(now, log.daysAgo, log.hour, log.minute)
                    if (timestamp > now) continue
                    repository.createLog(
                        created.id,
                        HabitLog(0, log.note, timestamp, created.id)
                    )
                }
            }

            prefs.edit().putBoolean(PREFS_FLAG, true).commit()
        }
    }

    private fun isDebuggable(context: Context): Boolean {
        return (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }

    private fun atLocalTime(nowMillis: Long, daysAgo: Int, hour: Int, minute: Int): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = nowMillis - TimeUnit.DAYS.toMillis(daysAgo.toLong())
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    private data class HabitSpec(val name: String, val logs: List<LogSpec>)
    private data class LogSpec(
        val note: String,
        val daysAgo: Int,
        val hour: Int,
        val minute: Int
    )
}
