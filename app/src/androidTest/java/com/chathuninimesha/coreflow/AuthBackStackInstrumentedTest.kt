package com.chathuninimesha.coreflow

import android.content.Context
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.chathuninimesha.coreflow.auth.AuthModule
import com.chathuninimesha.coreflow.auth.AuthNavigator
import com.chathuninimesha.coreflow.auth.AuthResult
import com.chathuninimesha.coreflow.auth.SharedPreferencesAuthStore
import com.chathuninimesha.coreflow.data.local.LocalStorageKeys
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthBackStackInstrumentedTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setUp() {
        AuthModule.resetForTests()
        context.getSharedPreferences(SharedPreferencesAuthStore.PROFILE_PREFS, Context.MODE_PRIVATE)
            .edit().clear().commit()
        context.getSharedPreferences(SharedPreferencesAuthStore.SESSION_PREFS, Context.MODE_PRIVATE)
            .edit().clear().commit()
        context.getSharedPreferences(SharedPreferencesAuthStore.LEGACY_PREFS, Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    @After
    fun tearDown() {
        AuthModule.resetForTests()
    }

    @Test
    fun authNavigationFlags_clearTheTask() {
        val flags = AuthNavigator.AUTH_STACK_FLAGS
        assertTrue(flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
        assertTrue(flags and Intent.FLAG_ACTIVITY_CLEAR_TASK != 0)
    }

    @Test
    fun signUp_storesHashNotPlaintext_andSessionSurvivesReload() {
        val auth = AuthModule.init(context)
        val result = auth.signUp(
            "Ada",
            "ada@example.com",
            "password1".toCharArray(),
            "password1".toCharArray()
        )
        assertEquals(AuthResult.Success, result)

        val profilePrefs = context.getSharedPreferences(
            SharedPreferencesAuthStore.PROFILE_PREFS,
            Context.MODE_PRIVATE
        )
        val stored = profilePrefs.all.toString()
        assertFalse(stored.contains("password1"))
        assertNotNull(profilePrefs.getString("password_hash", null))

        AuthModule.resetForTests()
        val reloaded = AuthModule.init(context)
        assertTrue(reloaded.hasActiveSession())
    }

    @Test
    fun logout_clearsSession_andLeavesHabitsIntact() {
        val habitPrefs = context.getSharedPreferences(LocalStorageKeys.TRACKER_PREFS, Context.MODE_PRIVATE)
        habitPrefs.edit()
            .putString(LocalStorageKeys.HABITS, """[{"id":1}]""")
            .putString(LocalStorageKeys.casesKey(1), """[{"id":10}]""")
            .commit()

        val auth = AuthModule.init(context)
        auth.signUp("Ada", "ada@example.com", "password1".toCharArray(), "password1".toCharArray())
        auth.signOut()

        assertFalse(auth.hasActiveSession())
        assertEquals("""[{"id":1}]""", habitPrefs.getString(LocalStorageKeys.HABITS, null))
        assertEquals("""[{"id":10}]""", habitPrefs.getString(LocalStorageKeys.casesKey(1), null))
        assertNull(
            context.getSharedPreferences(
                SharedPreferencesAuthStore.SESSION_PREFS,
                Context.MODE_PRIVATE
            ).getString("session_email", null)
        )
    }

    @Test
    fun legacyPlaintextPassword_isHashedThenRemoved() {
        context.getSharedPreferences(SharedPreferencesAuthStore.LEGACY_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString("Name", "Ada")
            .putString("Email", "ada@example.com")
            .putString("Password", "legacy-secret")
            .commit()

        val auth = AuthModule.init(context)
        assertTrue(auth.hasLocalProfile())
        val leftover = context.getSharedPreferences(
            SharedPreferencesAuthStore.LEGACY_PREFS,
            Context.MODE_PRIVATE
        )
        assertFalse(leftover.contains("Password"))
        val hash = context.getSharedPreferences(
            SharedPreferencesAuthStore.PROFILE_PREFS,
            Context.MODE_PRIVATE
        ).getString("password_hash", null)
        assertNotNull(hash)
        assertFalse(hash!!.contains("legacy-secret"))
    }
}
