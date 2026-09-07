package com.chathuninimesha.coreflow.reminder

import android.content.Context
import com.chathuninimesha.coreflow.model.logs.HabitLog
import com.chathuninimesha.coreflow.timer.SystemClock

object ReminderModule {

    fun scheduler(context: Context): ReminderScheduler {
        return ReminderScheduler(AndroidAlarmGateway(context.applicationContext), SystemClock)
    }

    fun toRequest(log: HabitLog): ReminderRequest {
        return ReminderRequest(
            habitId = log.habitId,
            caseId = log.id,
            message = log.comment,
            triggerAtMillis = log.date
        )
    }
}
