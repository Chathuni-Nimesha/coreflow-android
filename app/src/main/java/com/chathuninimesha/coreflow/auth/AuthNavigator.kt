package com.chathuninimesha.coreflow.auth

import android.app.Activity
import android.content.Intent
import com.chathuninimesha.coreflow.ui.auth.SignInActivity
import com.chathuninimesha.coreflow.ui.auth.SignUpActivity
import com.chathuninimesha.coreflow.ui.main.MainActivity
import com.chathuninimesha.coreflow.ui.onboarding.OnboardingActivity

object AuthNavigator {

    const val AUTH_STACK_FLAGS =
        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

    fun openMain(activity: Activity) {
        activity.startActivity(
            Intent(activity, MainActivity::class.java).apply {
                flags = AUTH_STACK_FLAGS
            }
        )
    }

    fun openSignIn(activity: Activity) {
        activity.startActivity(
            Intent(activity, SignInActivity::class.java).apply {
                flags = AUTH_STACK_FLAGS
            }
        )
    }

    fun openSignUp(activity: Activity) {
        activity.startActivity(
            Intent(activity, SignUpActivity::class.java).apply {
                flags = AUTH_STACK_FLAGS
            }
        )
    }

    fun openOnboarding(activity: Activity) {
        activity.startActivity(
            Intent(activity, OnboardingActivity::class.java).apply {
                flags = AUTH_STACK_FLAGS
            }
        )
    }

    fun openAuthAfterOnboarding(activity: Activity, repository: LocalAuthRepository) {
        repository.completeOnboarding()
        if (repository.hasLocalProfile()) {
            openSignIn(activity)
        } else {
            openSignUp(activity)
        }
    }
}
