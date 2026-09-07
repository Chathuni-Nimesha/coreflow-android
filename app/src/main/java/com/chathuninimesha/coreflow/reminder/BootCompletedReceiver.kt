package com.chathuninimesha.coreflow.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.chathuninimesha.coreflow.model.Repositories

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED) return

        val pending = goAsync()
        Thread {
            try {
                Repositories.init(context.applicationContext)
                val logs = Repositories.habitRepository.getAllLogsSafely()
                val requests = logs.map(ReminderModule::toRequest)
                ReminderModule.scheduler(context).restore(requests)
            } finally {
                pending.finish()
            }
        }.start()
    }
}
