package org.dpdns.bnai.anime.core.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PersistedChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val role: String,
    val content: String,
    val suggestedIntent: String? = null,
    val commandId: String? = null,
    val timestampMs: Long = System.currentTimeMillis()
)

class TokenManager(context: Context? = null) {

    private val prefs: SharedPreferences? = (context ?: appContext)?.let { createSecurePreferences(it) }
    private val gson = Gson()
    private var cachedUserId: String? = null
    private var cachedWorkspaceId: String? = null
    private var cachedUserRole: String? = null
    private var cachedUserEmail: String? = null

    val sessionExpiredMessage: StateFlow<String?>
        get() = _sessionExpiredFlow.asStateFlow()

    fun saveToken(token: String) {
        cachedToken = token
        prefs?.edit()?.putString(KEY_JWT, token)?.apply()
    }

    fun getToken(): String? {
        return cachedToken ?: prefs?.getString(KEY_JWT, null)?.also { cachedToken = it }
    }

    fun saveSession(
        accessToken: String,
        refreshToken: String?,
        expiresInSeconds: Long?,
        userId: String?,
        workspaceId: String? = "default-workspace",
        role: String? = "normal_user",
        email: String? = ""
    ) {
        saveToken(accessToken)
        val expiresIn = expiresInSeconds?.takeIf { it > 0 } ?: 3600L
        cachedRefreshToken = refreshToken?.takeIf { it.isNotBlank() }
        cachedExpiresAt = System.currentTimeMillis() + expiresIn * 1000L
        val editor = prefs?.edit()
        if (cachedRefreshToken.isNullOrBlank()) {
            editor?.remove(KEY_REFRESH_TOKEN)
        } else {
            editor?.putString(KEY_REFRESH_TOKEN, cachedRefreshToken)
        }
        editor?.putLong(KEY_EXPIRES_AT, cachedExpiresAt)
        editor?.apply()
        saveUserMeta(userId, workspaceId, role, email)
    }

    fun getRefreshToken(): String? =
        cachedRefreshToken ?: prefs?.getString(KEY_REFRESH_TOKEN, null)?.also { cachedRefreshToken = it }

    fun getAccessTokenExpiresAt(): Long =
        cachedExpiresAt.takeIf { it > 0L } ?: (prefs?.getLong(KEY_EXPIRES_AT, 0L) ?: 0L).also { cachedExpiresAt = it }

    fun isAccessTokenExpired(skewMs: Long = 60_000L): Boolean {
        val expiresAt = getAccessTokenExpiresAt()
        return expiresAt <= 0L || expiresAt <= System.currentTimeMillis() + skewMs
    }

    fun clearToken(reason: String? = null) {
        cachedToken = null
        cachedRefreshToken = null
        cachedExpiresAt = 0L
        cachedUserId = null
        cachedWorkspaceId = null
        cachedUserRole = null
        cachedUserEmail = null
        prefs?.edit()
            ?.remove(KEY_JWT)
            ?.remove(KEY_REFRESH_TOKEN)
            ?.remove(KEY_EXPIRES_AT)
            ?.remove(KEY_USER_ID)
            ?.remove(KEY_WORKSPACE_ID)
            ?.remove(KEY_USER_ROLE)
            ?.remove(KEY_USER_EMAIL)
            ?.apply()
        if (!reason.isNullOrBlank()) {
            _sessionExpiredFlow.value = reason
        }
    }

    internal fun clearForTests() {
        cachedToken = null
        cachedRefreshToken = null
        cachedExpiresAt = 0L
        cachedUserId = null
        cachedWorkspaceId = null
        cachedUserRole = null
        cachedUserEmail = null
        cachedProjectId = null
        cachedProviderLabel = null
        prefs?.edit()?.clear()?.apply()
    }

    fun saveUserMeta(
        userId: String?,
        workspaceId: String? = "default-workspace",
        role: String? = "normal_user",
        email: String? = ""
    ) {
        cachedUserId = userId
        cachedWorkspaceId = workspaceId ?: "default-workspace"
        cachedUserRole = role ?: "normal_user"
        cachedUserEmail = email ?: ""
        prefs?.edit()
            ?.putString(KEY_USER_ID, userId ?: "")
            ?.putString(KEY_WORKSPACE_ID, workspaceId ?: "default-workspace")
            ?.putString(KEY_USER_ROLE, role ?: "normal_user")
            ?.putString(KEY_USER_EMAIL, email ?: "")
            ?.apply()
    }

    fun getUserId(): String = cachedUserId ?: prefs?.getString(KEY_USER_ID, "") ?: ""
    fun getWorkspaceId(): String = cachedWorkspaceId ?: prefs?.getString(KEY_WORKSPACE_ID, "default-workspace") ?: "default-workspace"
    fun getUserEmail(): String = cachedUserEmail ?: prefs?.getString(KEY_USER_EMAIL, "") ?: ""
    fun getUserRole(): String = cachedUserRole ?: prefs?.getString(KEY_USER_ROLE, "normal_user") ?: "normal_user"
    fun isOwner(): Boolean = getUserRole().lowercase() in listOf("owner", "owner_admin", "admin")

    fun notifySessionExpired(message: String = "Your session has expired. Please sign in again.") {
        _sessionExpiredFlow.value = message
    }

    fun consumeSessionExpiredMessage() {
        _sessionExpiredFlow.value = null
    }

    fun saveActiveProjectId(projectId: String) {
        if (projectId.isBlank()) return
        cachedProjectId = projectId
        prefs?.edit()?.putString(KEY_ACTIVE_PROJECT_ID, projectId)?.apply()
    }

    fun getActiveProjectId(): String {
        return cachedProjectId ?: prefs?.getString(KEY_ACTIVE_PROJECT_ID, "")?.also { cachedProjectId = it } ?: ""
    }

    fun saveActiveCommandId(projectId: String, scopeKey: String, commandId: String?) {
        if (projectId.isBlank()) return
        val key = "cmd_${projectId}_${scopeKey}"
        if (commandId.isNullOrBlank()) {
            prefs?.edit()?.remove(key)?.apply()
        } else {
            prefs?.edit()?.putString(key, commandId)?.apply()
        }
    }

    fun getActiveCommandId(projectId: String, scopeKey: String): String? {
        if (projectId.isBlank()) return null
        return prefs?.getString("cmd_${projectId}_${scopeKey}", null)
    }

    fun saveConfiguredProviderLabel(label: String) {
        cachedProviderLabel = label
        prefs?.edit()?.putString(KEY_PROVIDER_LABEL, label)?.apply()
    }

    fun getConfiguredProviderLabel(): String {
        return cachedProviderLabel
            ?: prefs?.getString(KEY_PROVIDER_LABEL, "Groq • Gemini")?.also { cachedProviderLabel = it }
            ?: "Groq • Gemini"
    }

    fun saveFabOffset(offsetX: Float, offsetY: Float) {
        prefs?.edit()
            ?.putFloat(KEY_FAB_OFFSET_X, offsetX)
            ?.putFloat(KEY_FAB_OFFSET_Y, offsetY)
            ?.apply()
    }

    fun getFabOffsetX(): Float = prefs?.getFloat(KEY_FAB_OFFSET_X, 0f) ?: 0f
    fun getFabOffsetY(): Float = prefs?.getFloat(KEY_FAB_OFFSET_Y, 0f) ?: 0f

    fun getProjectChatHistory(projectId: String): List<PersistedChatMessage> {
        val pid = projectId.ifBlank { "global_studio" }
        val raw = prefs?.getString("chat_history_$pid", null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<PersistedChatMessage>>() {}.type
            gson.fromJson<List<PersistedChatMessage>>(raw, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveProjectChatHistory(projectId: String, messages: List<PersistedChatMessage>) {
        val pid = projectId.ifBlank { "global_studio" }
        val trimmed = if (messages.size > 60) messages.takeLast(60) else messages
        prefs?.edit()?.putString("chat_history_$pid", gson.toJson(trimmed))?.apply()
    }

    fun appendProjectChatMessage(projectId: String, message: PersistedChatMessage): List<PersistedChatMessage> {
        val current = getProjectChatHistory(projectId).toMutableList()
        current.add(message)
        saveProjectChatHistory(projectId, current)
        return current
    }

    fun clearProjectChatHistory(projectId: String) {
        val pid = projectId.ifBlank { "global_studio" }
        prefs?.edit()?.remove("chat_history_$pid")?.apply()
    }

    fun getProjectConversationId(projectId: String): String {
        val pid = projectId.ifBlank { "global_studio" }
        val existing = prefs?.getString("conv_id_$pid", null)
        if (!existing.isNullOrBlank()) return existing
        val created = "anime_${pid.take(8)}_${System.currentTimeMillis() / 1000}"
        prefs?.edit()?.putString("conv_id_$pid", created)?.apply()
        return created
    }

    private fun createSecurePreferences(context: Context): SharedPreferences? {
        return try {
            openEncryptedPreferences(context)
        } catch (_: Exception) {
            try {
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().commit()
                openEncryptedPreferences(context)
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun openEncryptedPreferences(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    companion object {
        private const val PREFS_NAME = "anime_engine_auth_prefs"
        private const val KEY_JWT = "jwt_access_token"
        private const val KEY_REFRESH_TOKEN = "jwt_refresh_token"
        private const val KEY_EXPIRES_AT = "jwt_expires_at"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_WORKSPACE_ID = "workspace_id"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_ACTIVE_PROJECT_ID = "active_project_id"
        private const val KEY_PROVIDER_LABEL = "configured_ai_provider_label"
        private const val KEY_FAB_OFFSET_X = "global_ai_fab_offset_x"
        private const val KEY_FAB_OFFSET_Y = "global_ai_fab_offset_y"

        @Volatile
        private var appContext: Context? = null

        @Volatile
        private var instance: TokenManager? = null

        private val _sessionExpiredFlow = MutableStateFlow<String?>(null)
        val sessionExpiredMessage: StateFlow<String?> = _sessionExpiredFlow.asStateFlow()

        @Volatile
        var cachedToken: String? = null

        @Volatile
        private var cachedRefreshToken: String? = null

        @Volatile
        private var cachedExpiresAt: Long = 0L

        @Volatile
        var cachedProjectId: String? = null

        @Volatile
        var cachedProviderLabel: String? = null

        fun init(context: Context): TokenManager {
            appContext = context.applicationContext
            return instance ?: synchronized(this) {
                instance ?: TokenManager(appContext).also { instance = it }
            }
        }

        fun getInstance(): TokenManager {
            return instance ?: synchronized(this) {
                instance ?: TokenManager(appContext).also { instance = it }
            }
        }

        fun notifySessionExpired(message: String = "Your session has expired. Please sign in again.") {
            _sessionExpiredFlow.value = message
        }

        fun consumeSessionExpiredMessage() {
            _sessionExpiredFlow.value = null
        }

        fun resetForTests() {
            synchronized(this) {
                instance = null
                appContext = null
                cachedToken = null
                cachedRefreshToken = null
                cachedExpiresAt = 0L
                cachedProjectId = null
                cachedProviderLabel = null
                _sessionExpiredFlow.value = null
            }
        }
    }
}
