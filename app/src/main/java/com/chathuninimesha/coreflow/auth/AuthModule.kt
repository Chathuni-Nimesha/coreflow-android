package com.chathuninimesha.coreflow.auth

import android.content.Context

object AuthModule {

    @Volatile
    private var repositoryInstance: LocalAuthRepository? = null

    fun init(context: Context): LocalAuthRepository {
        repositoryInstance?.let { return it }
        synchronized(this) {
            repositoryInstance?.let { return it }
            val appContext = context.applicationContext
            val store = SharedPreferencesAuthStore(appContext)
            val repository = LocalAuthRepository(store, store)
            migrateLegacy(appContext, repository)
            repositoryInstance = repository
            return repository
        }
    }

    fun repository(): LocalAuthRepository {
        return repositoryInstance
            ?: error("AuthModule.init() must be called before use")
    }

    @Synchronized
    fun resetForTests() {
        repositoryInstance = null
    }

    private fun migrateLegacy(context: Context, repository: LocalAuthRepository) {
        val legacy = context.getSharedPreferences(
            SharedPreferencesAuthStore.LEGACY_PREFS,
            Context.MODE_PRIVATE
        )
        val plaintext = legacy.getString("Password", null)
        if (!plaintext.isNullOrEmpty()) {
            repository.migrateLegacyPlaintext(
                legacyName = legacy.getString("Name", null),
                legacyEmail = legacy.getString("Email", null),
                legacyPassword = plaintext.toCharArray()
            )
        }
        if (legacy.contains("Password")) {
            legacy.edit().remove("Password").commit()
        }
    }
}
