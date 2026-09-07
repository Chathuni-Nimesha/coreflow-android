package com.chathuninimesha.coreflow.auth

internal class InMemoryAuthStore : ProfileStore, SessionStore {
    var storedProfile: LocalProfile? = null
    var onboardingDone: Boolean = false
    var sessionActive: Boolean = false
    var sessionEmail: String? = null

    override fun getProfile(): LocalProfile? = storedProfile

    override fun saveProfile(profile: LocalProfile) {
        storedProfile = profile
    }

    override fun isOnboardingCompleted(): Boolean = onboardingDone

    override fun setOnboardingCompleted(completed: Boolean) {
        onboardingDone = completed
    }

    override fun isSignedIn(): Boolean = sessionActive

    override fun signedInEmail(): String? = sessionEmail

    override fun setSignedIn(email: String) {
        sessionActive = true
        sessionEmail = email
    }

    override fun clearSession() {
        sessionActive = false
        sessionEmail = null
    }
}

internal fun testAuthRepository(store: InMemoryAuthStore = InMemoryAuthStore()): LocalAuthRepository {
    return LocalAuthRepository(
        profileStore = store,
        sessionStore = store,
        hasher = PasswordHasher(iterations = 1_000)
    )
}
