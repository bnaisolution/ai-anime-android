package org.dpdns.bnai.anime.core.auth
import android.content.Context
import android.content.SharedPreferences
class TokenManager(private val prefs: SharedPreferences) {
    constructor(context: Context) : this(context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE))
    fun saveToken(token: String) { prefs.edit().putString(KEY_JWT_TOKEN, token).apply() }
    fun getToken(): String? = prefs.getString(KEY_JWT_TOKEN, null)
    fun saveUserMeta(userId: String?, workspaceId: String?, role: String?) {
        prefs.edit().putString(KEY_USER_ID, userId).putString(KEY_WORKSPACE_ID, workspaceId).putString(KEY_USER_ROLE, role ?: "owner").apply()
    }
    fun getUserRole(): String = prefs.getString(KEY_USER_ROLE, "owner") ?: "owner"
    fun saveActiveProjectId(projectId: String) { if (projectId.isNotBlank()) { prefs.edit().putString(KEY_ACTIVE_PROJECT_ID, projectId).apply() } }
    fun getActiveProjectId(): String = prefs.getString(KEY_ACTIVE_PROJECT_ID, DEFAULT_PROJECT_ID) ?: DEFAULT_PROJECT_ID
    fun clearToken() { prefs.edit().clear().apply() }
    companion object {
        private const val PREFS_NAME = "anime_studio_prefs"
        private const val KEY_JWT_TOKEN = "jwt_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_WORKSPACE_ID = "workspace_id"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_ACTIVE_PROJECT_ID = "active_project_id"
        const val DEFAULT_PROJECT_ID = "11111111-1111-1111-1111-111111111111"
        @Volatile private var instance: TokenManager? = null
        fun init(context: Context): TokenManager = instance ?: synchronized(this) { instance ?: TokenManager(context.applicationContext).also { instance = it } }
        fun getInstance(): TokenManager? = instance
    }
}
