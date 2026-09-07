package com.chathuninimesha.coreflow

import android.app.Application
import com.chathuninimesha.coreflow.model.Repositories

class CoreflowApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Repositories.init(this)
    }
}
