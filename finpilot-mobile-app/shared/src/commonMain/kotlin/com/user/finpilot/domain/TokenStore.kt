package com.user.finpilot.domain

/**
 * Deliberately simple in-memory holder for the capstone — the token is
 * lost on app restart, which is fine for a demo. For production you'd
 * persist this via expect/actual (EncryptedSharedPreferences on Android,
 * Keychain on iOS) rather than plain memory.
 */
object TokenStore {
    var token: String? = null
        private set
    var username: String? = null
        private set

    fun save(token: String, username: String) {
        this.token = token
        this.username = username
    }

    fun clear() {
        token = null
        username = null
    }

    fun isLoggedIn(): Boolean = token != null
}