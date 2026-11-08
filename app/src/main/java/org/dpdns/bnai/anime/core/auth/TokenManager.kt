package org.dpdns.bnai.anime.core.auth

import android.content.Context
import android.content.SharedPreferences
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

    private val prefs: SharedPreferences? =
        (context ?: appContext)?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    val sessionExpiredMessage: StateFlow<String?>
        get() = _sessionExpiredFlow.asStateFlow()

    fun saveToken(token: String) {
        cachedToken = token
        prefs?.edit()?.putString(KEY_JWT, token)?.apply()
    }

    fun getToken(): String? {
        return cachedToken ?: prefs?.getString(KEY_JWT, null)?.also { cachedToken = it }
    }

    fun clearToken(reason: String? = null) {
        cachedToken = null
        prefs?.edit()?.remove(KEY_JWT)?.apply()
        if (!reason.isNullOrBlank()) {
            _sessionExpiredFlow.value = reason
        }
    }

    internal fun clearForTests() {
        cachedToken = null
        cachedProjectId = null
        cachedProviderLabel = null
        prefs?.edit()?.clear()?.apply()
    }

    fun saveUserMeta(
        userId: String?,
        workspaceId: String? = "default-workspace",
        role: String? = "owner",
        email: String? = ""
    ) {
        prefs?.edit()
            ?.putString(KEY_USER_ID, userId ?: "")
            ?.putString(KEY_WORKSPACE_ID, workspaceId ?: "default-workspace")
            ?.putString(KEY_USER_ROLE, role ?: "owner")
            ?.putString(KEY_USER_EMAIL, email ?: "")
            ?.apply()
    }

    fun getUserId(): String = prefs?.getString(KEY_USER_ID, "") ?: ""
    fun getWorkspaceId(): String = prefs?.getString(KEY_WORKSPACE_ID, "default-workspace") ?: "default-workspace"
    fun getUserEmail(): String = prefs?.getString(KEY_USER_EMAIL, "director@studio.ai") ?: "director@studio.ai"
    fun getUserRole(): String = prefs?.getString(KEY_USER_ROLE, "owner") ?: "owner"
    fun isOwner(): Boolean = getUserRole().lowercase() in listOf("owner", "admin")

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

    companion object {
        private const val PREFS_NAME = "anime_engine_auth_prefs"
        private const val KEY_JWT = "jwt_access_token"
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
                cachedProjectId = null
                cachedProviderLabel = null
                _sessionExpiredFlow.value = null
            }
        }
    }
}
