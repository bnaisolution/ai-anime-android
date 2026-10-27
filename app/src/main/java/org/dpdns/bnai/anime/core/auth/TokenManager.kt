package org.dpdns.bnai.anime.core.auth

import android.content.Context
import android.content.SharedPreferences

class TokenManager(private val prefs: SharedPreferences) {

    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    )

    fun saveToken(token: String) {
        prefs.edit().putString(KEY_JWT_TOKEN, token).apply()
    }

    fun getToken(): String? {
        return prefs.getString(KEY_JWT_TOKEN, null)
    }

    fun clearToken() {
        prefs.edit().remove(KEY_JWT_TOKEN).apply()
    }

    companion object {
        private const val PREFS_NAME = "anime_studio_prefs"
        private const val KEY_JWT_TOKEN = "jwt_token"

        @Volatile
        private var instance: TokenManager? = null

        fun init(context: Context): TokenManager {
            return instance ?: synchronized(this) {
                instance ?: TokenManager(context.applicationContext).also { instance = it }
            }
        }

        fun getInstance(): TokenManager? = instance
    }
}
