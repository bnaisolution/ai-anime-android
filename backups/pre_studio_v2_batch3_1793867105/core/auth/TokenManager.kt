package org.dpdns.bnai.anime.core.auth

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TokenManager(private val prefs: SharedPreferences) {

    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    )

    private val _sessionExpiredMessage = MutableStateFlow<String?>(null)
    val sessionExpiredMessage: StateFlow<String?> = _sessionExpiredMessage.asStateFlow()

    fun saveToken(token: String) {
        prefs.edit().putString(KEY_JWT_TOKEN, token).apply()
        _sessionExpiredMessage.value = null
    }

    fun getToken(): String? = prefs.getString(KEY_JWT_TOKEN, null)

    fun saveUserMeta(userId: String?, workspaceId: String?, role: String?, email: String? = null) {
        prefs.edit()
            .putString(KEY_USER_ID, userId ?: "")
            .putString(KEY_WORKSPACE_ID, workspaceId ?: "")
            .putString(KEY_USER_ROLE, role ?: "normal_user")
            .putString(KEY_USER_EMAIL, email ?: "")
            .apply()
    }

    fun getUserId(): String = prefs.getString(KEY_USER_ID, "") ?: ""
    fun getWorkspaceId(): String = prefs.getString(KEY_WORKSPACE_ID, "") ?: ""
    fun getUserEmail(): String = prefs.getString(KEY_USER_EMAIL, "") ?: ""
    fun getUserRole(): String = prefs.getString(KEY_USER_ROLE, "normal_user") ?: "normal_user"

    fun isOwner(): Boolean {
        val r = getUserRole().lowercase()
        return r == "owner" || r == "owner_admin" || r == "admin"
    }

    fun saveActiveProjectId(projectId: String) {
        if (projectId.isNotBlank()) {
            prefs.edit().putString(KEY_ACTIVE_PROJECT_ID, projectId).apply()
        }
    }

    fun getActiveProjectId(): String = prefs.getString(KEY_ACTIVE_PROJECT_ID, "") ?: ""

    fun clearToken(expiredMessage: String? = null) {
        prefs.edit().clear().apply()
        _sessionExpiredMessage.value = expiredMessage
    }

    fun consumeSessionExpiredMessage() {
        _sessionExpiredMessage.value = null
    }

    companion object {
        private const val PREFS_NAME = "anime_studio_prefs"
        private const val KEY_JWT_TOKEN = "jwt_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_WORKSPACE_ID = "workspace_id"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_ACTIVE_PROJECT_ID = "active_project_id"

        @Volatile
        private var instance: TokenManager? = null

        fun init(context: Context): TokenManager =
            instance ?: synchronized(this) {
                instance ?: TokenManager(context.applicationContext).also { instance = it }
            }

        fun getInstance(): TokenManager? = instance
    }
}
