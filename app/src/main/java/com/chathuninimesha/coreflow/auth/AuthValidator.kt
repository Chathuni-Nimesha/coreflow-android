package com.chathuninimesha.coreflow.auth

object AuthValidator {
    const val MIN_PASSWORD_LENGTH = 8

    private val EMAIL_REGEX = Regex(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    )

    fun normalizeEmail(email: String): String = email.trim().lowercase()

    fun validateSignUp(
        name: String,
        email: String,
        password: CharArray,
        confirmPassword: CharArray
    ): AuthError? {
        if (name.isBlank()) return AuthError.NAME_REQUIRED
        if (email.isBlank()) return AuthError.EMAIL_REQUIRED
        if (!EMAIL_REGEX.matches(normalizeEmail(email))) return AuthError.EMAIL_INVALID
        if (password.isEmpty()) return AuthError.PASSWORD_REQUIRED
        if (password.size < MIN_PASSWORD_LENGTH) return AuthError.PASSWORD_TOO_SHORT
        if (confirmPassword.isEmpty()) return AuthError.CONFIRM_PASSWORD_REQUIRED
        if (!password.contentEquals(confirmPassword)) return AuthError.PASSWORD_MISMATCH
        return null
    }

    fun validateSignIn(email: String, password: CharArray): AuthError? {
        if (email.isBlank()) return AuthError.EMAIL_REQUIRED
        if (!EMAIL_REGEX.matches(normalizeEmail(email))) return AuthError.EMAIL_INVALID
        if (password.isEmpty()) return AuthError.PASSWORD_REQUIRED
        return null
    }
}
