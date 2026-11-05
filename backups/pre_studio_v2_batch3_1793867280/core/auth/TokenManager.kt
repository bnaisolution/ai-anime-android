package org.dpdns.bnai.anime.core.auth

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class PersistedChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val role: String, // "user" | "assistant" | "system"
    val content: String,
    val suggestedIntent: String? = null,
    val commandId: String? = null,
    val timestampMs: Long = System.currentTimeMillis()
)

class TokenManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveToken(token: String) {
        cachedToken = token
        prefs.edit().putString(KEY_JWT, token).apply()
    }

    fun getToken(): String? {
        return cachedToken ?: prefs.getString(KEY_JWT, null)?.also { cachedToken = it }
    }

    fun clearToken() {
        cachedToken = null
        prefs.edit().remove(KEY_JWT).apply()
    }

    fun saveActiveProjectId(projectId: String) {
        if (projectId.isBlank()) return
        cachedProjectId = projectId
        prefs.edit().putString(KEY_ACTIVE_PROJECT_ID, projectId).apply()
    }

    fun getActiveProjectId(): String {
        return cachedProjectId ?: prefs.getString(KEY_ACTIVE_PROJECT_ID, "")?.also {
            cachedProjectId = it
        } ?: ""
    }

    // BUG-001 & BUG-031: Persist active async command IDs per project + screen key
    fun saveActiveCommandId(projectId: String, scopeKey: String, commandId: String?) {
        if (projectId.isBlank()) return
        val key = "cmd_${projectId}_${scopeKey}"
        if (commandId.isNullOrBlank()) {
            prefs.edit().remove(key).apply()
        } else {
            prefs.edit().putString(key, commandId).apply()
        }
    }

    fun getActiveCommandId(projectId: String, scopeKey: String): String? {
        if (projectId.isBlank()) return null
        return prefs.getString("cmd_${projectId}_${scopeKey}", null)
    }

    // BUG-003: Real configured AI provider badge state (never fake hardcoded label)
    fun saveConfiguredProviderLabel(label: String) {
        cachedProviderLabel = label
        prefs.edit().putString(KEY_PROVIDER_LABEL, label).apply()
    }

    fun getConfiguredProviderLabel(): String {
        return cachedProviderLabel ?: prefs.getString(KEY_PROVIDER_LABEL, "Provider: Checking...")?.also {
            cachedProviderLabel = it
        } ?: "Provider: Checking..."
    }

    // BUG-004: Persist user-selected draggable FAB offset
    fun saveFabOffset(offsetX: Float, offsetY: Float) {
        prefs.edit()
            .putFloat(KEY_FAB_OFFSET_X, offsetX)
            .putFloat(KEY_FAB_OFFSET_Y, offsetY)
            .apply()
    }

    fun getFabOffsetX(): Float = prefs.getFloat(KEY_FAB_OFFSET_X, 0f)
    fun getFabOffsetY(): Float = prefs.getFloat(KEY_FAB_OFFSET_Y, 0f)

    // BUG-005 & BUG-028: Persistent project-scoped AI Director conversation history
    fun getProjectChatHistory(projectId: String): List<PersistedChatMessage> {
        val pid = projectId.ifBlank { "global_studio" }
        val raw = prefs.getString("chat_history_$pid", null) ?: return emptyList()
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
        prefs.edit().putString("chat_history_$pid", gson.toJson(trimmed)).apply()
    }

    fun appendProjectChatMessage(projectId: String, message: PersistedChatMessage): List<PersistedChatMessage> {
        val current = getProjectChatHistory(projectId).toMutableList()
        current.add(message)
        saveProjectChatHistory(projectId, current)
        return current
    }

    fun clearProjectChatHistory(projectId: String) {
        val pid = projectId.ifBlank { "global_studio" }
        prefs.edit().remove("chat_history_$pid").apply()
    }

    fun getProjectConversationId(projectId: String): String {
        val pid = projectId.ifBlank { "global_studio" }
        val existing = prefs.getString("conv_id_$pid", null)
        if (!existing.isNullOrBlank()) return existing
        val created = "anime_${pid.take(8)}_${System.currentTimeMillis() / 1000}"
        prefs.edit().putString("conv_id_$pid", created).apply()
        return created
    }

    companion object {
        private const val PREFS_NAME = "anime_engine_auth_prefs"
        private const val KEY_JWT = "jwt_access_token"
        private const val KEY_ACTIVE_PROJECT_ID = "active_project_id"
        private const val KEY_PROVIDER_LABEL = "configured_ai_provider_label"
        private const val KEY_FAB_OFFSET_X = "global_ai_fab_offset_x"
        private const val KEY_FAB_OFFSET_Y = "global_ai_fab_offset_y"

        @Volatile
        var cachedToken: String? = null

        @Volatile
        var cachedProjectId: String? = null

        @Volatile
        var cachedProviderLabel: String? = null
    }
}
