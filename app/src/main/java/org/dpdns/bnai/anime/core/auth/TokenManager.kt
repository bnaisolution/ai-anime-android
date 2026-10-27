package org.dpdns.bnai.anime.core.auth
import android.content.Context
import android.content.SharedPreferences
class TokenManager(private val prefs: SharedPreferences) {
    fun saveToken(token: String) { prefs.edit().putString("jwt_token", token).apply() }
    fun getToken(): String? = prefs.getString("jwt_token", null)
    fun clearToken() { prefs.edit().remove("jwt_token").apply() }
    companion object {
        @Volatile private var instance: TokenManager? = null
        fun init(context: Context): TokenManager {
            return instance ?: synchronized(this) {
                instance ?: TokenManager(context.applicationContext.getSharedPreferences("anime_prefs", Context.MODE_PRIVATE)).also { instance = it }
            }
        }
    }
}
