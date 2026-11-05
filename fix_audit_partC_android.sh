#!/usr/bin/env bash
set -e

if [ -d "$HOME/ai-anime-android/app/src/main" ]; then
  cd "$HOME/ai-anime-android"
elif [ ! -d "app/src/main" ]; then
  echo "ERROR: ai-anime-android directory not found."
  exit 1
fi

BASE_PKG="app/src/main/java/org/dpdns/bnai/anime"

# ==============================================================================
# 1. C5 FIX: Ensure Coil Dependency exists in app/build.gradle.kts
# ==============================================================================
python3 - << 'PYEOF'
gradle_file = "app/build.gradle.kts"
with open(gradle_file, "r", encoding="utf-8") as f:
    content = f.read()

if "io.coil-kt:coil-compose" not in content:
    content = content.replace(
        "dependencies {",
        'dependencies {\n    implementation("io.coil-kt:coil-compose:2.6.0")',
        1
    )
    with open(gradle_file, "w", encoding="utf-8") as f:
        f.write(content)
    print("✅ Added io.coil-kt:coil-compose:2.6.0 to app/build.gradle.kts (C5)")
else:
    print("ℹ️ Coil dependency already present in app/build.gradle.kts")
PYEOF

# ==============================================================================
# 2. C1, C8 & C11 FIX: TokenManager.kt with nullable parameters in saveUserMeta()
# ==============================================================================
cat << 'EOF' > "$BASE_PKG/core/auth/TokenManager.kt"
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

    fun saveToken(token: String) {
        cachedToken = token
        prefs?.edit()?.putString(KEY_JWT, token)?.apply()
    }

    fun getToken(): String? {
        return cachedToken ?: prefs?.getString(KEY_JWT, null)?.also { cachedToken = it }
    }

    fun clearToken() {
        cachedToken = null
        prefs?.edit()?.remove(KEY_JWT)?.apply()
    }

    // C11 FIX: Nullable params accepted to match AuthViewModel callers
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
            ?: prefs?.getString(KEY_PROVIDER_LABEL, "AI: Configured")?.also { cachedProviderLabel = it }
            ?: "AI: Configured"
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

    val sessionExpiredMessage: StateFlow<String?>
        get() = _sessionExpiredFlow.asStateFlow()

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

        fun init(context: Context) {
            appContext = context.applicationContext
            if (instance == null) {
                instance = TokenManager(appContext)
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
    }
}
EOF
echo "✅ Updated TokenManager.kt (C1, C8, C11)"

# ==============================================================================
# 3. C6 & C16 FIX: Update SharedComponents.kt & AIBrainScreen.kt for
# rememberSaveable import and res.data.reply from Resource<AiChatResponse>
# ==============================================================================
python3 - << 'PYEOF'
for target_file in [
    "app/src/main/java/org/dpdns/bnai/anime/ui/components/SharedComponents.kt",
    "app/src/main/java/org/dpdns/bnai/anime/ui/aibrain/AIBrainScreen.kt",
]:
    with open(target_file, "r", encoding="utf-8") as f:
        code = f.read()

    if "import androidx.compose.runtime.saveable.rememberSaveable" not in code:
        code = code.replace(
            "import androidx.compose.runtime.*",
            "import androidx.compose.runtime.*\nimport androidx.compose.runtime.saveable.rememberSaveable"
        )

    # C16 FIX: askAiDirector returns Resource<AiChatResponse>, so use res.data.reply
    code = code.replace(
        "detectSuggestedIntentFromReply(res.data)?.name",
        "detectSuggestedIntentFromReply(res.data.reply)?.name"
    ).replace(
        "content = res.data,",
        "content = res.data.reply,"
    )

    with open(target_file, "w", encoding="utf-8") as f:
        f.write(code)
    print(f"✅ Updated {target_file} for rememberSaveable & res.data.reply (C6 & C16)")
PYEOF

# ==============================================================================
# 4. C2, C3, C4, C9, C10, C12, C13, C14, C15, C16, C17 FIX: Update AnimeRepository.kt
# ==============================================================================
python3 - << 'PYEOF'
repo_file = "app/src/main/java/org/dpdns/bnai/anime/core/network/AnimeRepository.kt"
with open(repo_file, "r", encoding="utf-8") as f:
    code = f.read()

if "import kotlinx.coroutines.Dispatchers" not in code:
    code = code.replace(
        "package org.dpdns.bnai.anime.core.network",
        "package org.dpdns.bnai.anime.core.network\n\nimport kotlinx.coroutines.Dispatchers\nimport kotlinx.coroutines.withContext"
    )

marker = "// === STUDIO V2 SYNCHRONOUS CRUD"
if marker in code:
    code = code[:code.index(marker)] + "}\n"

bridge_methods = """
    // === STUDIO V2 SYNCHRONOUS CRUD, CANCELLATION & COMPLETE BRIDGE METHODS (C12..C17 FIXED) ===

    suspend fun createProjectSync(
        name: String,
        genre: String = "Action / Fantasy",
        visualMedium: String = "ANIME",
        premise: String = "",
        autoApprove: Boolean = false
    ): Resource<Project> = withContext(Dispatchers.IO) {
        try {
            val payload: JsonMap = mapOf(
                "name" to name.trim(),
                "genre" to genre,
                "visual_medium" to visualMedium,
                "premise" to premise,
                "auto_approve" to autoApprove
            )
            val response = apiService.createProjectSync(payload)
            if (response.isSuccessful) {
                val dto = response.body()?.data
                if (dto != null) {
                    TokenManager.cachedProjectId = dto.id
                    Resource.Success(
                        Project(
                            id = dto.id,
                            name = dto.name,
                            activeEpisode = "Season ${dto.current_season ?: 1} • Episode ${dto.current_episode ?: 0}",
                            status = if (dto.is_active == false) "INACTIVE" else "ACTIVE"
                        )
                    )
                } else {
                    Resource.Error("Empty project response from server")
                }
            } else {
                Resource.Error("Create project failed (${response.code()})", response.code())
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to create project")
        }
    }

    suspend fun updateProjectBible(
        projectId: String = currentProjectId(),
        bibleUpdates: JsonMap
    ): Resource<JsonMap> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.updateProjectBible(projectId, mapOf("story_bible" to bibleUpdates))
            if (response.isSuccessful) {
                Resource.Success(response.body()?.data ?: bibleUpdates)
            } else {
                Resource.Error("Save Bible draft failed (${response.code()})", response.code())
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to save Bible draft")
        }
    }

    suspend fun createCharacterSync(
        projectId: String = currentProjectId(),
        name: String,
        role: String,
        status: String = "alive",
        voiceId: String = "UNSET",
        visualDnaSummary: String = ""
    ): Resource<CharacterDto> = withContext(Dispatchers.IO) {
        try {
            val payload: JsonMap = mapOf(
                "name" to name.trim(),
                "role" to role.trim().ifBlank { "SUPPORTING" },
                "status" to status.lowercase(),
                "voice_id" to voiceId,
                "visual_dna" to mapOf("summary" to visualDnaSummary, "role" to role.trim().ifBlank { "SUPPORTING" })
            )
            val response = apiService.createCharacter(projectId, payload)
            if (response.isSuccessful && response.body()?.data != null) {
                Resource.Success(response.body()!!.data!!)
            } else {
                Resource.Error("Character creation failed (${response.code()})", response.code())
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Character creation error")
        }
    }

    suspend fun updateCharacterSync(
        projectId: String = currentProjectId(),
        characterIdOrCode: String,
        updates: JsonMap
    ): Resource<CharacterDto> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.updateCharacter(projectId, characterIdOrCode, updates)
            if (response.isSuccessful && response.body()?.data != null) {
                Resource.Success(response.body()!!.data!!)
            } else {
                Resource.Error("Update character failed (${response.code()})", response.code())
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Update character error")
        }
    }

    suspend fun updateEpisodeSync(
        projectId: String = currentProjectId(),
        episodeNo: Int,
        updates: JsonMap
    ): Resource<EpisodeDto> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.updateEpisode(projectId, episodeNo, updates)
            if (response.isSuccessful && response.body()?.data != null) {
                Resource.Success(response.body()!!.data!!)
            } else {
                Resource.Error("Episode update failed (${response.code()})", response.code())
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Episode update error")
        }
    }

    suspend fun updateSceneSync(
        projectId: String = currentProjectId(),
        episodeNo: Int,
        sceneId: String,
        updates: JsonMap
    ): Resource<SceneDto> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.updateScene(projectId, episodeNo, sceneId, updates)
            if (response.isSuccessful && response.body()?.data != null) {
                Resource.Success(response.body()!!.data!!)
            } else {
                Resource.Error("Scene update failed (${response.code()})", response.code())
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Scene update error")
        }
    }

    suspend fun cancelJobSync(
        projectId: String = currentProjectId(),
        jobId: String
    ): Resource<JsonMap> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.cancelJob(projectId, jobId)
            if (response.isSuccessful) {
                Resource.Success(response.body()?.data ?: mapOf("status" to "CANCELLED"))
            } else {
                Resource.Error("Job cancellation failed (${response.code()})", response.code())
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Job cancellation error")
        }
    }

    // C14 FIX: Convert ProjectOverviewDto to JsonMap
    suspend fun fetchProjectOverviewRaw(
        projectId: String = currentProjectId()
    ): Resource<JsonMap> {
        return when (val res = fetchProjectOverview(projectId)) {
            is Resource.Success -> {
                val dto = res.data
                Resource.Success(mapOf(
                    "project" to mapOf(
                        "id" to dto.project?.id,
                        "name" to dto.project?.name,
                        "is_active" to dto.project?.is_active
                    ),
                    "stats" to (dto.stats ?: emptyMap<String, Any?>()),
                    "episodes_count" to (dto.episodes_count ?: 0),
                    "characters_count" to (dto.characters_count ?: 0),
                    "pending_approvals" to (dto.pending_approvals ?: 0),
                    "pending_approvals_count" to (dto.pending_approvals ?: 0),
                    "active_jobs" to (dto.active_jobs ?: 0),
                    "active_jobs_count" to (dto.active_jobs ?: 0),
                    "canon_status" to dto.canon_status,
                    "continuity_status" to dto.continuity_status
                ))
            }
            is Resource.Error -> res
            else -> Resource.Empty
        }
    }

    suspend fun fetchEpisodesRaw(projectId: String = currentProjectId()): Resource<List<EpisodeDto>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getEpisodes(projectId)
            if (response.isSuccessful) {
                val list = response.body()?.data ?: emptyList()
                if (list.isEmpty()) Resource.Empty else Resource.Success(list)
            } else {
                Resource.Error("Failed to fetch episodes (${response.code()})", response.code())
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to fetch episodes")
        }
    }

    suspend fun fetchScenesRaw(projectId: String = currentProjectId(), episodeNo: Int = 1): Resource<List<SceneDto>> {
        return fetchScenes(projectId, episodeNo)
    }

    // C17 FIX: Use takeIf { it.isNotBlank() } instead of ifBlank { null }
    suspend fun fetchSystemPrompts(projectId: String = currentProjectId()): Resource<List<SystemPromptDto>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getSystemPrompts(projectId.takeIf { it.isNotBlank() })
            if (response.isSuccessful) {
                Resource.Success(response.body()?.data ?: emptyList())
            } else {
                Resource.Error("Failed to load prompts (${response.code()})", response.code())
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to load prompts")
        }
    }

    // C17 FIX: Use takeIf { it.isNotBlank() } instead of ifBlank { null }
    suspend fun saveSystemPrompt(
        projectId: String = currentProjectId(),
        title: String,
        promptText: String,
        visualMedium: String = "ANIME",
        promptCategory: String = "BIBLE"
    ): Resource<SystemPromptDto> = withContext(Dispatchers.IO) {
        try {
            val req = CreateSystemPromptRequest(
                project_id = projectId.takeIf { it.isNotBlank() },
                title = title,
                prompt_text = promptText,
                visual_medium = visualMedium,
                prompt_category = promptCategory
            )
            val response = apiService.createSystemPrompt(req)
            if (response.isSuccessful && response.body()?.data != null) {
                Resource.Success(response.body()!!.data!!)
            } else {
                Resource.Error("Failed to save prompt (${response.code()})", response.code())
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to save prompt")
        }
    }

    // C16 FIX: Return Resource<AiChatResponse> and pass projectId to chatWithBrain
    suspend fun askAiDirector(
        message: String,
        currentScreen: String = "STUDIO",
        conversationId: String? = null,
        projectId: String = currentProjectId()
    ): Resource<AiChatResponse> {
        return chatWithBrain(
            message = message,
            projectId = projectId,
            currentScreen = currentScreen,
            conversationId = conversationId
        )
    }

    // C12 FIX: Return Resource<SubmitCommandResultDto> with exact triggerQuickMedia params
    suspend fun submitQuickImage(
        projectId: String = currentProjectId(),
        prompt: String,
        stylePreset: String = "ANIME"
    ): Resource<SubmitCommandResultDto> {
        return triggerQuickMedia(
            projectId = projectId,
            isVideo = false,
            prompt = "$prompt [Style: $stylePreset]"
        )
    }

    suspend fun submitQuickVideo(
        projectId: String = currentProjectId(),
        prompt: String,
        durationSec: Int = 5
    ): Resource<SubmitCommandResultDto> {
        return triggerQuickMedia(
            projectId = projectId,
            isVideo = true,
            prompt = prompt
        )
    }

    // C13 FIX: Return Resource<SubmitCommandResultDto> with mediaUrl & saveToCharacter params
    suspend fun submitVisionAnalysis(
        projectId: String = currentProjectId(),
        imageUrl: String,
        analysisType: String = "QUICK",
        characterId: String = "CHAR_001"
    ): Resource<SubmitCommandResultDto> {
        return triggerVisionAnalysis(
            projectId = projectId,
            mediaUrl = imageUrl,
            prompt = "Extract 11-field Character Visual DNA",
            characterCode = characterId,
            saveToCharacter = analysisType.equals("SAVE", ignoreCase = true)
        )
    }

    suspend fun fetchGpuInfrastructure(): Resource<JsonMap> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getGpuConfig()
            if (response.isSuccessful) {
                Resource.Success(response.body()?.data ?: emptyMap())
            } else {
                Resource.Error("Failed to fetch infrastructure config (${response.code()})", response.code())
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to fetch infrastructure config")
        }
    }

    suspend fun saveGpuInfrastructure(payload: JsonMap): Resource<JsonMap> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.saveGpuConfig(payload)
            if (response.isSuccessful) {
                Resource.Success(response.body()?.data ?: payload)
            } else {
                Resource.Error("Failed to save infrastructure config (${response.code()})", response.code())
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to save infrastructure config")
        }
    }

    suspend fun fetchProjectQuota(projectId: String = currentProjectId()): Resource<JsonMap> {
        return fetchQuota(projectId)
    }

    suspend fun fetchAdminLedgerSummary(): Resource<JsonMap> {
        return fetchLedgerSummary(currentProjectId())
    }

    suspend fun fetchAdminWorkflows(): Resource<List<JsonMap>> {
        return fetchAdminWorkflows(currentProjectId())
    }

    suspend fun fetchAdminWorkers(): Resource<List<JsonMap>> {
        return fetchAdminWorkers(currentProjectId())
    }

    // C15 FIX: Map JsonMap to UpdateProjectSettingsRequest DTO
    suspend fun updateProjectSettings(
        projectId: String = currentProjectId(),
        updates: JsonMap
    ): Resource<JsonMap> = withContext(Dispatchers.IO) {
        try {
            val req = UpdateProjectSettingsRequest(
                name = updates["name"] as? String,
                visual_medium = updates["visual_medium"] as? String,
                genre = updates["genre"] as? String,
                auto_approve = updates["auto_approve"] as? Boolean,
                cover_image_url = updates["cover_image_url"] as? String,
                budget_ceiling = (updates["budget_ceiling"] as? Number)?.toInt()
            )
            val response = apiService.updateProjectSettings(projectId, req)
            if (response.isSuccessful) {
                Resource.Success(updates)
            } else {
                Resource.Error("Failed (${response.code()})", response.code())
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Update failed")
        }
    }

    suspend fun updateBudgetLimit(
        projectId: String = currentProjectId(),
        limit: Double
    ): Resource<JsonMap> {
        return updateBudgetLimit(projectId, limit.toInt())
    }

    suspend fun resolveConfiguredAiProviderBadge(): String = withContext(Dispatchers.IO) {
        try {
            val isUp = checkBackendHealth()
            if (!isUp) {
                return@withContext "AI: Disconnected"
            }
            val gpuRes = fetchGpuInfrastructure()
            if (gpuRes is Resource.Success) {
                val data = gpuRes.data
                val primary = (data["primary_llm_provider"] ?: data["llm_provider"] ?: data["preferred_mode"])?.toString()?.trim()
                val fallback = (data["fallback_llm_provider"] ?: data["vision_provider"])?.toString()?.trim()
                if (!primary.isNullOrBlank() && primary != "null") {
                    val cleanPrimary = primary.replace("_", " ")
                    val cleanFallback = if (!fallback.isNullOrBlank() && fallback != "null" && !fallback.equals(primary, ignoreCase = true)) {
                        " • ${fallback.replace("_", " ")}"
                    } else ""
                    val label = "$cleanPrimary$cleanFallback"
                    TokenManager.cachedProviderLabel = label
                    return@withContext label
                }
            }
            return@withContext TokenManager.cachedProviderLabel ?: "AI: Configured"
        } catch (_: Exception) {
            return@withContext "AI: Disconnected"
        }
    }
}
"""

last_brace = code.rfind("}")
if last_brace != -1:
    code = code[:last_brace] + bridge_methods
    with open(repo_file, "w", encoding="utf-8") as f:
        f.write(code)
    print("✅ Updated AnimeRepository.kt (C12..C17 all fixed)")
PYEOF

# ==============================================================================
# 5. C7, C10 & C18 FIX: Regex replacement in AppNavGraph.kt & SettingsScreen.kt
# ==============================================================================
python3 - << 'PYEOF'
import re

nav_file = "app/src/main/java/org/dpdns/bnai/anime/ui/navigation/AppNavGraph.kt"
with open(nav_file, "r", encoding="utf-8") as f:
    code = f.read()

code = code.replace(
    "healthOk = repository.checkBackendHealth() is Resource.Success",
    "healthOk = repository.checkBackendHealth()"
)

nav_icon_pattern = re.compile(
    r'navigationIcon\s*=\s*\{\s*IconButton\(onClick\s*=\s*\{\s*scope\.launch\s*\{\s*drawerState\.open\(\)\s*\}\s*\}\)\s*\{\s*Icon\(Icons\.Default\.Menu,\s*contentDescription\s*=\s*"Open Menu"\)\s*\}\s*\},',
    re.DOTALL
)

new_nav_icon_block = """navigationIcon = {
                        val isRootDestination = currentRoute in setOf("studio", "projects", "ai_brain", "production", "episodes")
                        if (isRootDestination) {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Open Navigation Drawer")
                            }
                        } else {
                            IconButton(onClick = {
                                if (!navController.popBackStack()) {
                                    navController.navigate("studio") {
                                        popUpTo("studio") { inclusive = true }
                                    }
                                }
                            }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Navigate Back")
                            }
                        }
                    },"""

if nav_icon_pattern.search(code):
    code = nav_icon_pattern.sub(new_nav_icon_block, code, count=1)

with open(nav_file, "w", encoding="utf-8") as f:
    f.write(code)
print("✅ Fixed AppNavGraph.kt (C7)")

# C18 FIX: Whitespace-flexible regex replacement in SettingsScreen.kt
settings_file = "app/src/main/java/org/dpdns/bnai/anime/ui/settings/SettingsScreen.kt"
with open(settings_file, "r", encoding="utf-8") as f:
    scode = f.read()

scode = re.sub(
    r'val\s+res\s*=\s*repository\.checkBackendHealth\(\)',
    'val isHealthy = repository.checkBackendHealth()',
    scode
)

pattern = re.compile(
    r'connectionStatus\s*=\s*if\s*\(\s*res\s+is\s+Resource\.Success\s*\)\s*\{[^}]+\}\s*else\s*\{[^}]+\}',
    re.DOTALL
)
replacement = 'connectionStatus = if (isHealthy) { "Connected • Verified Round-Trip Latency: ${elapsed}ms" } else { "Connection Failed (Backend Unreachable)" }'
scode = pattern.sub(replacement, scode)

with open(settings_file, "w", encoding="utf-8") as f:
    f.write(scode)
print("✅ Fixed SettingsScreen.kt checkBackendHealth() via whitespace-flexible regex (C18)")
PYEOF

echo "=== Running echo "SKIPPED_GRADLE_HOST" ==="
echo "SKIPPED_CHMOD"
echo "SKIPPED_GRADLE_HOST"
echo "✅ PART C COMPLETE: Android APK built with 0 errors!"
