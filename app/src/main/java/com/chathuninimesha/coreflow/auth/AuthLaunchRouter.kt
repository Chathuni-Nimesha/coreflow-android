package com.chathuninimesha.coreflow.auth

enum class LaunchDestination {
    MAIN,
    SIGN_IN,
    ONBOARDING
}

object AuthLaunchRouter {
    fun next(hasActiveSession: Boolean, onboardingCompleted: Boolean): LaunchDestination {
        return when {
            hasActiveSession -> LaunchDestination.MAIN
            onboardingCompleted -> LaunchDestination.SIGN_IN
            else -> LaunchDestination.ONBOARDING
        }
    }
}
