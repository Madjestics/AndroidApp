package com.example.movieandroid.data

import android.content.Context
import androidx.core.content.edit

class SessionManager(context: Context) {
    private val prefs = context.getSharedPreferences("movie_session", Context.MODE_PRIVATE)

    var token: String?
        get() = prefs.getString(KEY_TOKEN, null)
        set(value) {
            prefs.edit { putString(KEY_TOKEN, value) }
        }

    var userId: Long?
        get() {
            val value = prefs.getLong(KEY_USER_ID, -1L)
            return if (value <= 0L) null else value
        }
        set(value) {
            prefs.edit {
                if (value == null) {
                    remove(KEY_USER_ID)
                } else {
                    putLong(KEY_USER_ID, value)
                }
            }
        }

    var username: String?
        get() = prefs.getString(KEY_USERNAME, null)
        set(value) {
            prefs.edit { putString(KEY_USERNAME, value) }
        }

    var role: String?
        get() = prefs.getString(KEY_ROLE, null)
        set(value) {
            prefs.edit { putString(KEY_ROLE, value) }
        }

    fun clear() {
        prefs.edit { clear() }
    }

    fun isLoggedIn(): Boolean = !token.isNullOrBlank()

    fun isAdmin(): Boolean = role.equals("ADMIN", ignoreCase = true)

    companion object {
        private const val KEY_TOKEN = "token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "username"
        private const val KEY_ROLE = "role"
    }
}
