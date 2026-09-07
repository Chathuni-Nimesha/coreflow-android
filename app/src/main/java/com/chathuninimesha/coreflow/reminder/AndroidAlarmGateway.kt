package com.chathuninimesha.coreflow.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.chathuninimesha.coreflow.AlarmReceiver

class AndroidAlarmGateway(private val context: Context) : AlarmGateway {

    private val alarmManager =
        context.applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    override fun setExact(
        requestCode: Int,
        triggerAtMillis: Long,
        message: String,
        habitId: Long,
        caseId: Long,
        notificationId: Int
    ) {
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            createPendingIntent(requestCode, message, habitId, caseId, notificationId)
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
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            createPendingIntent(requestCode, message, habitId, caseId, notificationId)
        )
    }

    override fun cancel(requestCode: Int) {
        val pending = PendingIntent.getBroadcast(
            context.applicationContext,
            requestCode,
            reminderIntent("", 0L, 0L, requestCode),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pending != null) {
            alarmManager.cancel(pending)
            pending.cancel()
        }
    }

    private fun createPendingIntent(
        requestCode: Int,
        message: String,
        habitId: Long,
        caseId: Long,
        notificationId: Int
    ): PendingIntent {
        return PendingIntent.getBroadcast(
            context.applicationContext,
            requestCode,
            reminderIntent(message, habitId, caseId, notificationId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun reminderIntent(
        message: String,
        habitId: Long,
        caseId: Long,
        notificationId: Int
    ): Intent {
        return Intent(context.applicationContext, AlarmReceiver::class.java).apply {
            action = ReminderContract.ACTION
            putExtra(ReminderContract.EXTRA_MESSAGE, message)
            putExtra(ReminderContract.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(ReminderContract.EXTRA_HABIT_ID, habitId)
            putExtra(ReminderContract.EXTRA_CASE_ID, caseId)
        }
    }
}
