package com.chathuninimesha.coreflow.auth

import android.content.Context
import android.content.SharedPreferences

class SharedPreferencesAuthStore(context: Context) : ProfileStore, SessionStore {

    private val profilePrefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PROFILE_PREFS, Context.MODE_PRIVATE)
    private val sessionPrefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(SESSION_PREFS, Context.MODE_PRIVATE)

    override fun getProfile(): LocalProfile? {
        val email = profilePrefs.getString(KEY_EMAIL, null) ?: return null
        val hash = profilePrefs.getString(KEY_PASSWORD_HASH, null) ?: return null
        if (email.isBlank() || hash.isBlank()) return null
        return LocalProfile(
            name = profilePrefs.getString(KEY_NAME, "").orEmpty(),
            email = email,
            passwordHash = hash
        )
    }

    override fun saveProfile(profile: LocalProfile) {
        profilePrefs.edit()
            .putString(KEY_NAME, profile.name)
            .putString(KEY_EMAIL, profile.email)
            .putString(KEY_PASSWORD_HASH, profile.passwordHash)
            .apply()
    }

    override fun isOnboardingCompleted(): Boolean =
        profilePrefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)

    override fun setOnboardingCompleted(completed: Boolean) {
        profilePrefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()
    }

    override fun isSignedIn(): Boolean = sessionPrefs.getBoolean(KEY_SIGNED_IN, false)

    override fun signedInEmail(): String? = sessionPrefs.getString(KEY_SESSION_EMAIL, null)

    override fun setSignedIn(email: String) {
        sessionPrefs.edit()
            .putBoolean(KEY_SIGNED_IN, true)
            .putString(KEY_SESSION_EMAIL, email)
            .apply()
    }

    override fun clearSession() {
        sessionPrefs.edit().clear().apply()
    }

    companion object {
        const val PROFILE_PREFS = "coreflow_profile"
        const val SESSION_PREFS = "coreflow_session"
        const val LEGACY_PREFS = "MyPrefs"

        private const val KEY_NAME = "name"
        private const val KEY_EMAIL = "email"
        private const val KEY_PASSWORD_HASH = "password_hash"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_SIGNED_IN = "signed_in"
        private const val KEY_SESSION_EMAIL = "session_email"
    }
}
