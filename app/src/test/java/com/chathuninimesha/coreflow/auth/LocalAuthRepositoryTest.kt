package com.chathuninimesha.coreflow.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthValidatorTest {

    @Test
    fun emptyName_returnsNameRequired() {
        val error = AuthValidator.validateSignUp(
            name = "  ",
            email = "user@example.com",
            password = "password1".toCharArray(),
            confirmPassword = "password1".toCharArray()
        )
        assertEquals(AuthError.NAME_REQUIRED, error)
    }

    @Test
    fun invalidEmail_returnsEmailInvalid() {
        val error = AuthValidator.validateSignUp(
            name = "Ada",
            email = "not-an-email",
            password = "password1".toCharArray(),
            confirmPassword = "password1".toCharArray()
        )
        assertEquals(AuthError.EMAIL_INVALID, error)
    }

    @Test
    fun weakPassword_returnsTooShort() {
        val error = AuthValidator.validateSignUp(
            name = "Ada",
            email = "user@example.com",
            password = "short".toCharArray(),
            confirmPassword = "short".toCharArray()
        )
        assertEquals(AuthError.PASSWORD_TOO_SHORT, error)
    }

    @Test
    fun passwordMismatch_returnsMismatch() {
        val error = AuthValidator.validateSignUp(
            name = "Ada",
            email = "user@example.com",
            password = "password1".toCharArray(),
            confirmPassword = "password2".toCharArray()
        )
        assertEquals(AuthError.PASSWORD_MISMATCH, error)
    }

    @Test
    fun validSignUp_returnsNull() {
        val error = AuthValidator.validateSignUp(
            name = "Ada",
            email = "user@example.com",
            password = "password1".toCharArray(),
            confirmPassword = "password1".toCharArray()
        )
        assertNull(error)
    }
}

class PasswordHasherTest {

    private val hasher = PasswordHasher(iterations = 1_000)

    @Test
    fun hash_isNotPlaintext() {
        val encoded = hasher.hash("secret-pass".toCharArray())
        assertFalse(encoded.contains("secret-pass"))
        assertTrue(hasher.isEncodedHash(encoded))
    }

    @Test
    fun verify_acceptsCorrectPassword() {
        val encoded = hasher.hash("correcthorse".toCharArray())
        assertTrue(hasher.verify("correcthorse".toCharArray(), encoded))
    }

    @Test
    fun verify_rejectsWrongPassword() {
        val encoded = hasher.hash("correcthorse".toCharArray())
        assertFalse(hasher.verify("wrong-password".toCharArray(), encoded))
    }
}

class LocalAuthRepositoryTest {

    @Test
    fun validSignUp_createsSession() {
        val store = InMemoryAuthStore()
        val auth = testAuthRepository(store)
        val result = auth.signUp(
            "Ada Lovelace",
            "ada@example.com",
            "password1".toCharArray(),
            "password1".toCharArray()
        )
        assertEquals(AuthResult.Success, result)
        assertTrue(auth.hasActiveSession())
        assertEquals("ada@example.com", store.storedProfile?.email)
        assertFalse(store.storedProfile!!.passwordHash.contains("password1"))
    }

    @Test
    fun invalidEmail_failsSignUp() {
        val result = testAuthRepository().signUp(
            "Ada",
            "bad-email",
            "password1".toCharArray(),
            "password1".toCharArray()
        )
        assertEquals(AuthResult.Failure(AuthError.EMAIL_INVALID), result)
    }

    @Test
    fun emptyName_failsSignUp() {
        val result = testAuthRepository().signUp(
            "",
            "ada@example.com",
            "password1".toCharArray(),
            "password1".toCharArray()
        )
        assertEquals(AuthResult.Failure(AuthError.NAME_REQUIRED), result)
    }

    @Test
    fun weakPassword_failsSignUp() {
        val result = testAuthRepository().signUp(
            "Ada",
            "ada@example.com",
            "123".toCharArray(),
            "123".toCharArray()
        )
        assertEquals(AuthResult.Failure(AuthError.PASSWORD_TOO_SHORT), result)
    }

    @Test
    fun passwordMismatch_failsSignUp() {
        val result = testAuthRepository().signUp(
            "Ada",
            "ada@example.com",
            "password1".toCharArray(),
            "password2".toCharArray()
        )
        assertEquals(AuthResult.Failure(AuthError.PASSWORD_MISMATCH), result)
    }

    @Test
    fun duplicateEmail_failsSecondSignUp() {
        val auth = testAuthRepository()
        auth.signUp("Ada", "ada@example.com", "password1".toCharArray(), "password1".toCharArray())
        val result = auth.signUp(
            "Ada",
            "ADA@example.com",
            "password1".toCharArray(),
            "password1".toCharArray()
        )
        assertEquals(AuthResult.Failure(AuthError.DUPLICATE_EMAIL), result)
    }

    @Test
    fun successfulSignIn_createsSession() {
        val auth = testAuthRepository()
        auth.signUp("Ada", "ada@example.com", "password1".toCharArray(), "password1".toCharArray())
        auth.signOut()
        val result = auth.signIn("ada@example.com", "password1".toCharArray())
        assertEquals(AuthResult.Success, result)
        assertTrue(auth.hasActiveSession())
    }

    @Test
    fun wrongPassword_failsSignIn() {
        val auth = testAuthRepository()
        auth.signUp("Ada", "ada@example.com", "password1".toCharArray(), "password1".toCharArray())
        auth.signOut()
        val result = auth.signIn("ada@example.com", "wrongpass".toCharArray())
        assertEquals(AuthResult.Failure(AuthError.WRONG_PASSWORD), result)
        assertFalse(auth.hasActiveSession())
    }

    @Test
    fun unknownEmail_failsSignIn() {
        val result = testAuthRepository().signIn("missing@example.com", "password1".toCharArray())
        assertEquals(AuthResult.Failure(AuthError.UNKNOWN_EMAIL), result)
    }

    @Test
    fun sessionPersistsAcrossRepositoryInstances() {
        val store = InMemoryAuthStore()
        val first = testAuthRepository(store)
        first.signUp("Ada", "ada@example.com", "password1".toCharArray(), "password1".toCharArray())
        val second = testAuthRepository(store)
        assertTrue(second.hasActiveSession())
        assertEquals("ada@example.com", store.sessionEmail)
    }

    @Test
    fun logout_clearsSessionButKeepsProfile() {
        val store = InMemoryAuthStore()
        val auth = testAuthRepository(store)
        auth.signUp("Ada", "ada@example.com", "password1".toCharArray(), "password1".toCharArray())
        auth.signOut()
        assertFalse(auth.hasActiveSession())
        assertNotNull(store.storedProfile)
        assertEquals("ada@example.com", store.storedProfile?.email)
    }

    @Test
    fun migrateLegacyPlaintext_hashesAndDoesNotKeepPlainPassword() {
        val store = InMemoryAuthStore()
        val auth = testAuthRepository(store)
        auth.migrateLegacyPlaintext("Ada", "ada@example.com", "legacy-secret".toCharArray())
        assertNotNull(store.storedProfile)
        assertFalse(store.storedProfile!!.passwordHash.contains("legacy-secret"))
        assertTrue(PasswordHasher(iterations = 1_000).verify("legacy-secret".toCharArray(), store.storedProfile!!.passwordHash))
        assertTrue(store.onboardingDone)
        assertFalse(auth.hasActiveSession())
    }

    @Test
    fun backStackClearFlags_includeNewTaskAndClearTask() {
        val flags = AuthNavigator.AUTH_STACK_FLAGS
        assertTrue(flags and android.content.Intent.FLAG_ACTIVITY_NEW_TASK != 0)
        assertTrue(flags and android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK != 0)
    }

    @Test
    fun launchRouter_firstInstall_goesToOnboarding() {
        assertEquals(
            LaunchDestination.ONBOARDING,
            AuthLaunchRouter.next(hasActiveSession = false, onboardingCompleted = false)
        )
    }

    @Test
    fun launchRouter_returningWithSession_goesToMain() {
        assertEquals(
            LaunchDestination.MAIN,
            AuthLaunchRouter.next(hasActiveSession = true, onboardingCompleted = true)
        )
    }

    @Test
    fun launchRouter_returningWithoutSession_goesToSignIn() {
        assertEquals(
            LaunchDestination.SIGN_IN,
            AuthLaunchRouter.next(hasActiveSession = false, onboardingCompleted = true)
        )
    }

    @Test
    fun differentEmail_failsWhenProfileAlreadyExists() {
        val auth = testAuthRepository()
        auth.signUp("Ada", "ada@example.com", "password1".toCharArray(), "password1".toCharArray())
        val result = auth.signUp(
            "Grace",
            "grace@example.com",
            "password1".toCharArray(),
            "password1".toCharArray()
        )
        assertEquals(AuthResult.Failure(AuthError.PROFILE_EXISTS), result)
    }

    @Test
    fun corruptProfile_doesNotKeepSession_andSignUpCanReplace() {
        val store = InMemoryAuthStore()
        store.storedProfile = LocalProfile("Ada", "ada@example.com", "not-a-valid-hash")
        store.sessionActive = true
        store.sessionEmail = "ada@example.com"
        val auth = testAuthRepository(store)
        assertFalse(auth.hasActiveSession())
        val signIn = auth.signIn("ada@example.com", "password1".toCharArray())
        assertEquals(AuthResult.Failure(AuthError.PROFILE_CORRUPT), signIn)
        val signUp = auth.signUp(
            "Ada",
            "ada@example.com",
            "password1".toCharArray(),
            "password1".toCharArray()
        )
        assertEquals(AuthResult.Success, signUp)
        assertTrue(auth.hasActiveSession())
        assertFalse(store.storedProfile!!.passwordHash.contains("password1"))
    }
}
