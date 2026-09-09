package com.chathuninimesha.coreflow

import android.app.Application
import com.chathuninimesha.coreflow.data.demo.DemoDataSeeder
import com.chathuninimesha.coreflow.model.Repositories

class CoreflowApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Repositories.init(this)
        DemoDataSeeder.seedIfNeeded(this, Repositories.habitRepository)
    }
}
