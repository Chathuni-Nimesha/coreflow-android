package com.chathuninimesha.coreflow.auth

/**
 * Offline local profile and session. This is not cloud authentication.
 */
class LocalAuthRepository(
    private val profileStore: ProfileStore,
    private val sessionStore: SessionStore,
    private val hasher: PasswordHasher = PasswordHasher()
) {

    fun signUp(
        name: String,
        email: String,
        password: CharArray,
        confirmPassword: CharArray
    ): AuthResult {
        try {
            AuthValidator.validateSignUp(name, email, password, confirmPassword)?.let {
                return AuthResult.Failure(it)
            }
            val normalizedEmail = AuthValidator.normalizeEmail(email)
            val existing = profileStore.getProfile()
            if (existing != null && hasher.isEncodedHash(existing.passwordHash)) {
                return if (existing.email == normalizedEmail) {
                    AuthResult.Failure(AuthError.DUPLICATE_EMAIL)
                } else {
                    AuthResult.Failure(AuthError.PROFILE_EXISTS)
                }
            }
            val hash = hasher.hash(password)
            profileStore.saveProfile(
                LocalProfile(
                    name = name.trim(),
                    email = normalizedEmail,
                    passwordHash = hash
                )
            )
            profileStore.setOnboardingCompleted(true)
            sessionStore.setSignedIn(normalizedEmail)
            return AuthResult.Success
        } catch (_: Exception) {
            return AuthResult.Failure(AuthError.STORAGE_ERROR)
        } finally {
            password.fill('\u0000')
            confirmPassword.fill('\u0000')
        }
    }

    fun signIn(email: String, password: CharArray): AuthResult {
        try {
            AuthValidator.validateSignIn(email, password)?.let {
                return AuthResult.Failure(it)
            }
            val normalizedEmail = AuthValidator.normalizeEmail(email)
            val profile = profileStore.getProfile()
                ?: return AuthResult.Failure(AuthError.UNKNOWN_EMAIL)
            if (!hasher.isEncodedHash(profile.passwordHash)) {
                sessionStore.clearSession()
                return AuthResult.Failure(AuthError.PROFILE_CORRUPT)
            }
            if (profile.email != normalizedEmail) {
                return AuthResult.Failure(AuthError.UNKNOWN_EMAIL)
            }
            val matches = hasher.verify(password, profile.passwordHash)
            if (!matches) {
                return AuthResult.Failure(AuthError.WRONG_PASSWORD)
            }
            profileStore.setOnboardingCompleted(true)
            sessionStore.setSignedIn(normalizedEmail)
            return AuthResult.Success
        } catch (_: Exception) {
            return AuthResult.Failure(AuthError.STORAGE_ERROR)
        } finally {
            password.fill('\u0000')
        }
    }

    fun signOut() {
        sessionStore.clearSession()
    }

    fun hasActiveSession(): Boolean {
        val profile = profileStore.getProfile() ?: return false
        return sessionStore.isSignedIn() && hasher.isEncodedHash(profile.passwordHash)
    }

    fun isOnboardingCompleted(): Boolean = profileStore.isOnboardingCompleted()

    fun completeOnboarding() {
        profileStore.setOnboardingCompleted(true)
    }

    fun hasLocalProfile(): Boolean = profileStore.getProfile() != null

    fun currentProfileName(): String? = profileStore.getProfile()?.name

    fun currentProfileEmail(): String? = profileStore.getProfile()?.email

    fun currentProfile(): LocalProfile? = profileStore.getProfile()

    /**
     * One-time upgrade: hash a legacy plaintext password in memory, then delete it.
     * The plaintext value is never logged.
     */
    fun migrateLegacyPlaintext(legacyName: String?, legacyEmail: String?, legacyPassword: CharArray?) {
        try {
            val existing = profileStore.getProfile()
            if (existing == null &&
                !legacyEmail.isNullOrBlank() &&
                legacyPassword != null &&
                legacyPassword.isNotEmpty()
            ) {
                val hash = hasher.hash(legacyPassword)
                profileStore.saveProfile(
                    LocalProfile(
                        name = legacyName?.trim().orEmpty(),
                        email = AuthValidator.normalizeEmail(legacyEmail),
                        passwordHash = hash
                    )
                )
                profileStore.setOnboardingCompleted(true)
            }
        } catch (_: Exception) {
            // If migration fails, the caller still wipes the plaintext key.
        } finally {
            legacyPassword?.fill('\u0000')
        }
    }
}
