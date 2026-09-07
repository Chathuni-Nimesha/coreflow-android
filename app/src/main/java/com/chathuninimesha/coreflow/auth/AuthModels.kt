package com.chathuninimesha.coreflow.auth

data class LocalProfile(
    val name: String,
    val email: String,
    val passwordHash: String
)

enum class AuthError {
    NAME_REQUIRED,
    EMAIL_REQUIRED,
    EMAIL_INVALID,
    PASSWORD_REQUIRED,
    PASSWORD_TOO_SHORT,
    CONFIRM_PASSWORD_REQUIRED,
    PASSWORD_MISMATCH,
    DUPLICATE_EMAIL,
    PROFILE_EXISTS,
    UNKNOWN_EMAIL,
    WRONG_PASSWORD,
    INVALID_CREDENTIALS,
    PROFILE_CORRUPT,
    STORAGE_ERROR
}

sealed class AuthResult {
    object Success : AuthResult()
    data class Failure(val error: AuthError) : AuthResult()
}

interface ProfileStore {
    fun getProfile(): LocalProfile?
    fun saveProfile(profile: LocalProfile)
    fun isOnboardingCompleted(): Boolean
    fun setOnboardingCompleted(completed: Boolean)
}

interface SessionStore {
    fun isSignedIn(): Boolean
    fun signedInEmail(): String?
    fun setSignedIn(email: String)
    fun clearSession()
}
