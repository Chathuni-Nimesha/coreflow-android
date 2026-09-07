package com.chathuninimesha.coreflow

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.chathuninimesha.coreflow.auth.AuthLaunchRouter
import com.chathuninimesha.coreflow.auth.AuthModule
import com.chathuninimesha.coreflow.auth.AuthNavigator
import com.chathuninimesha.coreflow.auth.LaunchDestination

class Landing : AppCompatActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private val continueRunnable = Runnable { continueToNextScreen() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_landing)
        AuthModule.init(this)
        handler.postDelayed(continueRunnable, 2000L)
    }

    private fun continueToNextScreen() {
        val auth = AuthModule.repository()
        when (AuthLaunchRouter.next(auth.hasActiveSession(), auth.isOnboardingCompleted())) {
            LaunchDestination.MAIN -> AuthNavigator.openMain(this)
            LaunchDestination.SIGN_IN -> AuthNavigator.openSignIn(this)
            LaunchDestination.ONBOARDING -> AuthNavigator.openOnboarding(this)
        }
        finish()
    }

    override fun onDestroy() {
        handler.removeCallbacks(continueRunnable)
        super.onDestroy()
    }
}
