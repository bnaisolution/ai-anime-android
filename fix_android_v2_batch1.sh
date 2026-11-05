#!/usr/bin/env bash
set -e

if [ -d "$HOME/ai-anime-android/app/src/main" ]; then
  cd "$HOME/ai-anime-android"
elif [ -d "app/src/main" ]; then
  echo "Running inside ai-anime-android directory..."
else
  echo "ERROR: Could not find ai-anime-android directory."
  exit 1
fi

BASE_DIR="app/src/main/java/org/dpdns/bnai/anime"

echo "=== 1. Cleaning up any duplicate/misplaced screen folders ==="
rm -rf "$BASE_DIR/ui/scene" \
       "$BASE_DIR/ui/aibrain" \
       "$BASE_DIR/ui/prompt" \
       "$BASE_DIR/ui/quicktools" \
       "$BASE_DIR/ui/media" \
       "$BASE_DIR/ui/vision" \
       "$BASE_DIR/ui/quota" \
       "$BASE_DIR/ui/budget"

mkdir -p "$BASE_DIR/core/auth" \
         "$BASE_DIR/core/model" \
         "$BASE_DIR/core/network" \
         "$BASE_DIR/ui/designsystem" \
         "$BASE_DIR/ui/components" \
         "$BASE_DIR/ui/navigation"

# ==============================================================================
# 2. app/build.gradle.kts (With Coil & Lifecycle Compose)
# ==============================================================================
cat << 'EOF' > app/build.gradle.kts
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "org.dpdns.bnai.anime"
    compileSdk = 34

    defaultConfig {
        applicationId = "org.dpdns.bnai.anime"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.3")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("io.coil-kt:coil-compose:2.6.0")
}
EOF
echo "✅ Updated app/build.gradle.kts"

# ==============================================================================
# 3. CORE MODELS (Character.kt & Episode.kt)
# ==============================================================================
cat << 'EOF' > "$BASE_DIR/core/model/Character.kt"
package org.dpdns.bnai.anime.core.model

data class Character(
    val id: String,
    val name: String,
    val role: String,
    val visualDna: String,
    val voiceId: String,
    val status: String = "alive",
    val imageUrl: String? = null
)
EOF

cat << 'EOF' > "$BASE_DIR/core/model/Episode.kt"
package org.dpdns.bnai.anime.core.model

data class Episode(
    val id: String,
    val episodeNumber: Int,
    val title: String,
    val status: String,
    val synopsis: String? = null,
    val updatedAt: String? = null,
    val qcStatus: String? = null,
    val scriptText: String? = null,
    val masterVideoUrl: String? = null,
    val thumbnailUrl: String? = null,
    val projectName: String? = null
)
EOF
echo "✅ Updated core/model/Character.kt & Episode.kt"

# ==============================================================================
# 4. TokenManager.kt
# Fixes AuthViewModel.kt:55 (init returns TokenManager)
# Fixes RetrofitClient.kt:42 (clearToken accepts optional reason: String? = null)
# ==============================================================================
cat << 'EOF' > "$BASE_DIR/core/auth/TokenManager.kt"
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
    }
}
EOF
echo "✅ Updated core/auth/TokenManager.kt"

# ==============================================================================
# 5. RetrofitClient.kt
# ==============================================================================
cat << 'EOF' > "$BASE_DIR/core/network/RetrofitClient.kt"
package org.dpdns.bnai.anime.core.network

import android.content.Context
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import org.dpdns.bnai.anime.core.auth.TokenManager
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.UUID
import java.util.concurrent.TimeUnit

object RetrofitClient {
    const val BASE_URL = "https://api.bnai.dpdns.org/"

    @Volatile
    private var apiServiceInstance: AnimeApiService? = null

    fun create(tokenManager: TokenManager? = null): AnimeApiService {
        val authAndTraceInterceptor = Interceptor { chain ->
            val tm = tokenManager ?: TokenManager.getInstance()
            val activeToken = tm.getToken()
            val activeProject = tm.getActiveProjectId()
            val requestPath = chain.request().url.encodedPath

            val requestBuilder = chain.request().newBuilder()
                .header("Accept", "application/json")
                .header("X-Request-Id", UUID.randomUUID().toString())

            if (activeProject.isNotBlank()) {
                requestBuilder.header("X-Project-Id", activeProject)
            }
            if (!activeToken.isNullOrBlank()) {
                requestBuilder.header("Authorization", "Bearer $activeToken")
            }

            val response = chain.proceed(requestBuilder.build())

            if (response.code == 401 && !activeToken.isNullOrBlank() && !requestPath.contains("/auth/login")) {
                val errBody = response.peekBody(512).string().lowercase()
                if (errBody.contains("expired") || errBody.contains("invalid token") || errBody.contains("jwt") || errBody.contains("unauthorized")) {
                    tm.clearToken("Session expired. Please sign in again.")
                }
            }
            response
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(authAndTraceInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AnimeApiService::class.java)
    }

    fun getApiService(context: Context? = null): AnimeApiService {
        context?.let { TokenManager.init(it.applicationContext) }
        return apiServiceInstance ?: synchronized(this) {
            apiServiceInstance ?: create(TokenManager.getInstance()).also { apiServiceInstance = it }
        }
    }
}
EOF
echo "✅ Updated core/network/RetrofitClient.kt"

# ==============================================================================
# 6. AnimeApiService.kt (Complete DTOs + All Studio Read/Write/Cancel Endpoints)
# ==============================================================================
cat << 'EOF' > "$BASE_DIR/core/network/AnimeApiService.kt"
package org.dpdns.bnai.anime.core.network

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.*

typealias JsonMap = Map<String, @JvmSuppressWildcards Any?>

data class ApiMeta(val request_id: String? = null)
data class ApiEnvelope<T>(val data: T?, val meta: ApiMeta? = null)

data class LoginRequest(val email: String, val password: String)
data class LoginResponse(
    val token: String? = null,
    val access_token: String? = null,
    @SerializedName("user_id", alternate = ["userId"]) val userId: String? = null,
    @SerializedName("workspace_id", alternate = ["workspaceId"]) val workspaceId: String? = null,
    val email: String? = null,
    val role: String? = "normal_user"
)
data class AuthMeResponse(val status: String? = null, val user: JsonMap? = null)

data class AiChatRequest(
    val message: String,
    val project_id: String? = null,
    val skill_key: String = "ai_admin",
    val current_screen: String? = null,
    val conversation_id: String? = null,
    val request_id: String? = null
)
data class AiChatResponse(
    val request_id: String? = null,
    val status: String? = null,
    val reply: String? = null,
    val message: String? = null,
    val provider: String? = null,
    val model: String? = null,
    val action: String? = null
)

data class HealthResponseDto(
    val ok: Boolean = false,
    val status: String? = null,
    val db: String? = null,
    val database: String? = null,
    val version: String? = null,
    val timestamp: String? = null
)

enum class CommandIntent {
    CREATE_PROJECT,
    OPEN_PROJECT,
    SAVE_BIBLE,
    BIBLE_CHAT,
    QUICK_GENERATE_REEL,
    GENERATE_EPISODE,
    REGENERATE_EPISODE,
    CONTINUE_EPISODE,
    QUICK_GENERATE_IMAGE,
    QUICK_GENERATE_VIDEO,
    SAVE_TO_PROJECT,
    VISION_ANALYSIS,
    CHANGE_CHARACTER_VOICE,
    PREVIEW_VOICE,
    LIST_VOICES,
    RESET_VOICE,
    CHECK_CANON,
    VIEW_CANON,
    APPROVE_CANON_CHANGE,
    REJECT_CANON_CHANGE,
    CHECK_CONTINUITY,
    CHECK_QUOTA,
    RETRY_FAILED,
    VIEW_STATE,
    VIEW_AUDIT,
    RUN_QC,
    PUBLISH_NOW,
    VIEW_ANALYTICS,
    ADMIN_START_KAGGLE,
    ADMIN_STOP_KAGGLE,
    ADMIN_STATUS,
    DB_TEST,
    SYSTEM_HEALTH
}

data class CreateCommandRequest(
    val project_id: String,
    val intent: String,
    val mode: String = "SAVE",
    val episode_no: Int? = null,
    val payload: JsonMap = emptyMap()
)

data class ApiCommandRecordDto(
    val id: String,
    val project_id: String? = null,
    val intent: String? = null,
    val status: String? = null,
    val error_message: String? = null,
    val payload: JsonMap? = null,
    val result: JsonMap? = null,
    val created_at: String? = null,
    val updated_at: String? = null
) {
    val error: String?
        get() = error_message
}

data class SubmitCommandResultDto(
    val status: String? = null,
    val command_id: String? = null,
    val wf_key: String? = null,
    val command: ApiCommandRecordDto? = null
)

data class ProjectDto(
    val id: String,
    val name: String,
    val is_active: Boolean? = true,
    val story_bible: JsonMap? = null,
    val current_season: Int? = 1,
    val current_episode: Int? = 1,
    val budget_ceiling: Double? = null
)

data class UpdateProjectSettingsRequest(
    val name: String? = null,
    val visual_medium: String? = null,
    val genre: String? = null,
    val auto_approve: Boolean? = null,
    val cover_image_url: String? = null,
    val budget_ceiling: Int? = null
)

data class UpdateBudgetRequest(
    val budget_ceiling: Int
)

data class ProjectOverviewDto(
    val project: ProjectDto? = null,
    val stats: JsonMap? = null,
    val episodes_count: Int? = null,
    val characters_count: Int? = null,
    val pending_approvals: Int? = null,
    val active_jobs: Int? = null,
    val canon_status: String? = null,
    val continuity_status: String? = null
)

data class EpisodeDto(
    val id: String,
    val project_id: String? = null,
    val episode_no: Int,
    val title: String? = null,
    val synopsis: String? = null,
    val current_state: String? = null,
    val status: String? = null,
    val qc_status: String? = null,
    val script_json: Any? = null,
    val master_video_url: String? = null,
    val thumbnail_url: String? = null,
    val viral_metadata: JsonMap? = null,
    val updated_at: String? = null
)

data class ClipDto(
    val id: String? = null,
    val scene_order: Int? = null,
    val scene_no: Int? = null,
    val clip_url: String? = null,
    val provider: String? = null,
    val duration_sec: Double? = null,
    val status: String? = null
)

data class SceneDto(
    val id: String,
    val episode_no: Int? = null,
    val scene_order: Int? = null,
    val scene_no: Int? = null,
    val description: String? = null,
    val visual_prompt: String? = null,
    val dialogue_speaker_id: String? = null,
    val voice_text: String? = null,
    val bgm_hint: String? = null,
    val sfx_hint: String? = null,
    val status: String? = null,
    val clips: List<ClipDto>? = null
)

data class CharacterDto(
    val id: String? = null,
    val character_code: String? = null,
    val name: String? = null,
    val role: String? = null,
    val description: String? = null,
    val initial_status: String? = null,
    val image_url: String? = null,
    val canon_lock_status: String? = null,
    val visual_dna: JsonMap? = null,
    val voice_config: JsonMap? = null,
    val powers_and_lore: JsonMap? = null
)

data class JobDto(
    val id: String,
    val project_id: String? = null,
    val intent: String? = null,
    val status: String? = null,
    val episode_no: Int? = null,
    val episode_state: JsonMap? = null,
    val pending_actions: List<JsonMap>? = null,
    val attempt_count: Int? = 0,
    val max_attempts: Int? = 3,
    val error_message: String? = null,
    val created_at: String? = null
)

data class ApprovalDecisionDto(
    val id: String,
    val project_id: String? = null,
    val type: String = "APPROVAL",
    val resource_id: String? = null,
    val resource_version: Int = 1,
    val status: String? = null,
    val risk: String? = null,
    @SerializedName("decision_reason", alternate = ["reason"]) val reason: String? = null,
    val decision: String? = null,
    val exact_diff: JsonMap? = null,
    val expires_at: String? = null,
    val created_at: String? = null
) {
    val resource_type: String
        get() = type
}

data class DecideApprovalRequest(
    val decision: String,
    val reason: String,
    val resource_version: Int = 1
)

data class QuickGenerateRequest(
    val prompt: String,
    val style_preset: String? = "anime_cinematic",
    val aspect_ratio: String? = "16:9",
    val duration_sec: Int? = null
)

data class VisionAnalyzeRequest(
    val media_url: String,
    val analysis_type: String = "GENERAL",
    val prompt: String = "Extract 11-field Visual DNA",
    val character_code: String? = null,
    val save_to_character: Boolean = false
)

data class SystemPromptDto(
    val id: String,
    val project_id: String? = null,
    val title: String,
    val visual_medium: String = "ANIME",
    val prompt_category: String? = "BIBLE",
    val prompt_text: String,
    val is_default: Boolean? = false
)

data class CreateSystemPromptRequest(
    val project_id: String? = null,
    val title: String,
    val visual_medium: String,
    val prompt_category: String = "BIBLE",
    val prompt_text: String,
    val set_active: Boolean = true
)

data class CustomGpuConfigDto(
    val enabled: Boolean = false,
    val worker_url: String = "",
    val api_key: String? = null
)

data class KaggleAccountConfigDto(
    val username: String = "",
    val api_key: String = "",
    val notebook_slug: String? = "anime-gpu-worker",
    val worker_url: String? = null
)

data class TelegramStorageConfigDto(
    val bot_token: String = "",
    val storage_chat_id: String = "",
    val stream_bridge_base_url: String? = "https://api.bnai.dpdns.org/v1/media/telegram"
)

data class GpuSetupRequest(
    val preferred_mode: String = "KAGGLE_AUTO",
    val models_dataset_slug: String? = "bnaisolution/anime-engine-models-v1",
    val auto_deploy_kaggle: Boolean = true,
    val custom_gpu: CustomGpuConfigDto? = null,
    val kaggle_primary: KaggleAccountConfigDto? = null,
    val kaggle_secondary: KaggleAccountConfigDto? = null,
    val telegram_storage: TelegramStorageConfigDto? = null,
    val github_worker_url: String? = null
)

data class GpuSetupResponseDto(
    val ok: Boolean = false,
    val preferred_mode: String? = null,
    val kaggle_deploy: JsonMap? = null
)

interface AnimeApiService {
    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("api/v1/auth/me")
    suspend fun getMe(): Response<AuthMeResponse>

    @POST("api/v1/ai/chat")
    suspend fun chatWithAi(@Body request: AiChatRequest): Response<AiChatResponse>

    @GET("v1/anime/health")
    suspend fun getAnimeHealth(): Response<HealthResponseDto>

    @POST("v1/anime/commands")
    suspend fun submitCommand(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: CreateCommandRequest
    ): Response<ApiEnvelope<SubmitCommandResultDto>>

    @GET("v1/anime/commands/{id}")
    suspend fun getCommandStatus(@Path("id") commandId: String): Response<ApiEnvelope<ApiCommandRecordDto>>

    @GET("v1/anime/projects")
    suspend fun getProjects(): Response<ApiEnvelope<List<ProjectDto>>>

    @POST("v1/anime/projects")
    suspend fun createProjectSync(@Body body: JsonMap): Response<ApiEnvelope<ProjectDto>>

    @GET("v1/anime/projects/{id}/overview")
    suspend fun getProjectOverview(@Path("id") projectId: String): Response<ApiEnvelope<ProjectOverviewDto>>

    @PATCH("v1/anime/projects/{id}")
    suspend fun updateProjectSettings(
        @Path("id") projectId: String,
        @Body body: UpdateProjectSettingsRequest
    ): Response<ApiEnvelope<ProjectDto>>

    @PATCH("v1/anime/projects/{id}/bible")
    suspend fun updateProjectBible(
        @Path("id") projectId: String,
        @Body body: JsonMap
    ): Response<ApiEnvelope<JsonMap>>

    @PATCH("v1/anime/projects/{id}/budget")
    suspend fun updateProjectBudget(
        @Path("id") projectId: String,
        @Body body: UpdateBudgetRequest
    ): Response<ApiEnvelope<JsonMap>>

    @GET("v1/anime/projects/{id}/episodes")
    suspend fun getProjectEpisodes(@Path("id") projectId: String): Response<ApiEnvelope<List<EpisodeDto>>>

    @GET("v1/anime/projects/{id}/episodes/{episodeNo}")
    suspend fun getEpisodeDetail(@Path("id") projectId: String, @Path("episodeNo") episodeNo: Int): Response<ApiEnvelope<EpisodeDto>>

    @PATCH("v1/anime/projects/{id}/episodes/{episodeNo}")
    suspend fun updateEpisode(
        @Path("id") projectId: String,
        @Path("episodeNo") episodeNo: Int,
        @Body body: JsonMap
    ): Response<ApiEnvelope<EpisodeDto>>

    @GET("v1/anime/projects/{id}/episodes/{episodeNo}/plan")
    suspend fun getEpisodePlan(@Path("id") projectId: String, @Path("episodeNo") episodeNo: Int): Response<ApiEnvelope<JsonMap>>

    @GET("v1/anime/projects/{id}/episodes/{episodeNo}/scenes")
    suspend fun getEpisodeScenes(@Path("id") projectId: String, @Path("episodeNo") episodeNo: Int): Response<ApiEnvelope<List<SceneDto>>>

    @PATCH("v1/anime/projects/{id}/episodes/{episodeNo}/scenes/{sceneId}")
    suspend fun updateScene(
        @Path("id") projectId: String,
        @Path("episodeNo") episodeNo: Int,
        @Path("sceneId") sceneId: String,
        @Body body: JsonMap
    ): Response<ApiEnvelope<SceneDto>>

    @GET("v1/anime/projects/{id}/media")
    suspend fun getProjectMedia(@Path("id") projectId: String): Response<ApiEnvelope<JsonMap>>

    @GET("v1/anime/projects/{id}/jobs")
    suspend fun getProjectJobs(@Path("id") projectId: String): Response<ApiEnvelope<List<JobDto>>>

    @GET("v1/anime/projects/{id}/jobs/{jobId}")
    suspend fun getJobDetail(@Path("id") projectId: String, @Path("jobId") jobId: String): Response<ApiEnvelope<JobDto>>

    @POST("v1/anime/projects/{id}/jobs/{jobId}/cancel")
    suspend fun cancelJob(
        @Path("id") projectId: String,
        @Path("jobId") jobId: String
    ): Response<ApiEnvelope<JsonMap>>

    @GET("v1/anime/projects/{id}/bible")
    suspend fun getProjectBible(@Path("id") projectId: String): Response<ApiEnvelope<JsonMap>>

    @GET("v1/anime/projects/{id}/canon")
    suspend fun getProjectCanon(@Path("id") projectId: String): Response<ApiEnvelope<List<JsonMap>>>

    @GET("v1/anime/projects/{id}/canon/proposals")
    suspend fun getCanonProposals(@Path("id") projectId: String): Response<ApiEnvelope<List<JsonMap>>>

    @GET("v1/anime/projects/{id}/characters")
    suspend fun getProjectCharacters(@Path("id") projectId: String): Response<ApiEnvelope<List<CharacterDto>>>

    @POST("v1/anime/projects/{id}/characters")
    suspend fun createCharacter(
        @Path("id") projectId: String,
        @Body body: JsonMap
    ): Response<ApiEnvelope<CharacterDto>>

    @PATCH("v1/anime/projects/{id}/characters/{cid}")
    suspend fun updateCharacter(
        @Path("id") projectId: String,
        @Path("cid") characterIdOrCode: String,
        @Body body: JsonMap
    ): Response<ApiEnvelope<CharacterDto>>

    @GET("v1/anime/projects/{id}/timeline")
    suspend fun getProjectTimeline(@Path("id") projectId: String): Response<ApiEnvelope<List<JsonMap>>>

    @GET("v1/anime/projects/{id}/analytics")
    suspend fun getProjectAnalytics(@Path("id") projectId: String): Response<ApiEnvelope<JsonMap>>

    @GET("v1/anime/projects/{id}/audit")
    suspend fun getProjectAudit(@Path("id") projectId: String): Response<ApiEnvelope<List<JsonMap>>>

    @GET("v1/anime/projects/{id}/notifications")
    suspend fun getProjectNotifications(@Path("id") projectId: String): Response<ApiEnvelope<List<JsonMap>>>

    @POST("v1/anime/projects/{id}/quick/image")
    suspend fun quickGenerateImage(
        @Path("id") projectId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: QuickGenerateRequest
    ): Response<ApiEnvelope<SubmitCommandResultDto>>

    @POST("v1/anime/projects/{id}/quick/video")
    suspend fun quickGenerateVideo(
        @Path("id") projectId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: QuickGenerateRequest
    ): Response<ApiEnvelope<SubmitCommandResultDto>>

    @POST("v1/anime/projects/{id}/vision/analyze")
    suspend fun analyzeVision(
        @Path("id") projectId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: VisionAnalyzeRequest
    ): Response<ApiEnvelope<SubmitCommandResultDto>>

    @GET("v1/anime/projects/{id}/approvals")
    suspend fun getProjectApprovals(@Path("id") projectId: String): Response<ApiEnvelope<List<ApprovalDecisionDto>>>

    @POST("v1/anime/projects/{id}/approvals/{approvalId}/decisions")
    suspend fun decideApproval(
        @Path("id") projectId: String,
        @Path("approvalId") approvalId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: DecideApprovalRequest
    ): Response<ApiEnvelope<ApprovalDecisionDto>>

    @GET("v1/anime/admin/workflows")
    suspend fun getAdminWorkflows(@Query("project_id") projectId: String): Response<ApiEnvelope<List<JsonMap>>>

    @GET("v1/anime/admin/queues")
    suspend fun getAdminQueues(@Query("project_id") projectId: String): Response<ApiEnvelope<JsonMap>>

    @GET("v1/admin/anime/workers")
    suspend fun getAdminWorkers(@Query("project_id") projectId: String): Response<ApiEnvelope<List<JsonMap>>>

    @GET("v1/admin/anime/ledger/summary")
    suspend fun getLedgerSummary(@Query("project_id") projectId: String): Response<ApiEnvelope<JsonMap>>

    @GET("v1/anime/prompts")
    suspend fun getSystemPrompts(@Query("project_id") projectId: String? = null): Response<ApiEnvelope<List<SystemPromptDto>>>

    @POST("v1/anime/prompts")
    suspend fun createSystemPrompt(@Body body: CreateSystemPromptRequest): Response<ApiEnvelope<SystemPromptDto>>

    @DELETE("v1/anime/prompts/{promptId}")
    suspend fun deleteSystemPrompt(@Path("promptId") promptId: String): Response<ApiEnvelope<JsonMap>>

    @GET("v1/anime/infrastructure/gpu")
    suspend fun getGpuConfig(): Response<ApiEnvelope<JsonMap>>

    @POST("v1/anime/infrastructure/gpu")
    suspend fun saveGpuConfig(@Body request: GpuSetupRequest): Response<ApiEnvelope<GpuSetupResponseDto>>

    @POST("v1/anime/infrastructure/gpu")
    suspend fun saveGpuConfigRaw(@Body request: JsonMap): Response<ApiEnvelope<JsonMap>>

    @GET("v1/anime/projects/{id}/quota")
    suspend fun getProjectQuota(@Path("id") projectId: String): Response<ApiEnvelope<JsonMap>>

    @GET("v1/anime/projects/{id}/continuity")
    suspend fun getProjectContinuity(@Path("id") projectId: String): Response<ApiEnvelope<List<JsonMap>>>

    @GET("v1/anime/projects/{id}/retries")
    suspend fun getProjectRetries(@Path("id") projectId: String): Response<ApiEnvelope<List<JsonMap>>>

    @GET("v1/anime/projects/{id}/characters/{characterId}/reference-history")
    suspend fun getCharacterReferenceHistory(
        @Path("id") projectId: String,
        @Path("characterId") characterId: String
    ): Response<ApiEnvelope<List<JsonMap>>>

    @GET("v1/anime/projects/{id}/{category}")
    suspend fun getProjectCategoryList(
        @Path("id") projectId: String,
        @Path("category") category: String
    ): Response<ApiEnvelope<List<JsonMap>>>
}
EOF
echo "✅ Updated core/network/AnimeApiService.kt"

# ==============================================================================
# 7. AnimeRepository.kt (Complete Methods + All Bridge Methods + deleteSystemPrompt)
# ==============================================================================
cat << 'EOF' > "$BASE_DIR/core/network/AnimeRepository.kt"
package org.dpdns.bnai.anime.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.dpdns.bnai.anime.core.auth.TokenManager
import org.dpdns.bnai.anime.core.model.*
import retrofit2.Response
import java.util.UUID

class AnimeRepository(private val apiService: AnimeApiService = RetrofitClient.getApiService()) {

    fun currentProjectId(): String =
        TokenManager.getInstance().getActiveProjectId()

    private fun <T, R> mapEnvelope(
        response: Response<ApiEnvelope<T>>,
        transform: (T) -> R
    ): Resource<R> {
        if (!response.isSuccessful) {
            return Resource.Error("HTTP ${response.code()}: ${response.message()}", response.code())
        }
        val data = response.body()?.data ?: return Resource.Empty
        if (data is Collection<*> && data.isEmpty()) return Resource.Empty
        if (data is Map<*, *> && data.isEmpty()) return Resource.Empty
        return Resource.Success(transform(data))
    }

    suspend fun fetchProjects(): Resource<List<Project>> = try {
        val response = apiService.getProjects()
        if (response.isSuccessful) {
            val dtoList = response.body()?.data.orEmpty()
            if (dtoList.isEmpty()) {
                Resource.Empty
            } else {
                val activeId = currentProjectId()
                if (activeId.isBlank() || dtoList.none { it.id == activeId }) {
                    TokenManager.getInstance().saveActiveProjectId(dtoList.first().id)
                }
                Resource.Success(dtoList.map { dto ->
                    Project(
                        id = dto.id,
                        name = dto.name,
                        activeEpisode = "Season ${dto.current_season ?: 1} • Episode ${dto.current_episode ?: 1}",
                        status = if (dto.is_active == true) "ACTIVE" else "INACTIVE"
                    )
                })
            }
        } else {
            Resource.Error("HTTP ${response.code()}", response.code())
        }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Network failure")
    }

    suspend fun fetchProjectOverview(projectId: String): Resource<ProjectOverviewDto> = try {
        mapEnvelope(apiService.getProjectOverview(projectId)) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to load overview")
    }

    suspend fun fetchProjectOverviewRaw(
        projectId: String = currentProjectId()
    ): Resource<JsonMap> {
        return when (val res = fetchProjectOverview(projectId)) {
            is Resource.Success -> {
                val dto = res.data
                val bible = dto.project?.story_bible ?: emptyMap()
                Resource.Success(mapOf(
                    "project" to mapOf(
                        "id" to dto.project?.id,
                        "name" to dto.project?.name,
                        "is_active" to dto.project?.is_active,
                        "story_bible" to bible
                    ),
                    "name" to (dto.project?.name ?: "Studio Project"),
                    "genre" to (bible["genre"] ?: "Action / Fantasy"),
                    "visual_medium" to (bible["visual_medium"] ?: "ANIME"),
                    "auto_approve" to (bible["auto_approve"] == true),
                    "cover_image_url" to bible["cover_image_url"],
                    "budget_ceiling" to (bible["budget_ceiling"] ?: dto.project?.budget_ceiling ?: 1000),
                    "stats" to (dto.stats ?: emptyMap<String, Any?>()),
                    "episodes_count" to (dto.episodes_count ?: 0),
                    "characters_count" to (dto.characters_count ?: 0),
                    "pending_approvals" to (dto.pending_approvals ?: 0),
                    "pending_approvals_count" to (dto.pending_approvals ?: 0),
                    "active_jobs" to (dto.active_jobs ?: 0),
                    "active_jobs_count" to (dto.active_jobs ?: 0),
                    "canon_status" to (dto.canon_status ?: "UNSET"),
                    "continuity_status" to (dto.continuity_status ?: "UNCHECKED")
                ))
            }
            is Resource.Error -> res
            else -> Resource.Empty
        }
    }

    suspend fun updateProjectSettings(
        projectId: String,
        visualMedium: String? = null,
        genre: String? = null,
        autoApprove: Boolean? = null,
        coverImageUrl: String? = null,
        budgetCeiling: Int? = null
    ): Resource<ProjectDto> = try {
        mapEnvelope(
            apiService.updateProjectSettings(
                projectId,
                UpdateProjectSettingsRequest(
                    visual_medium = visualMedium,
                    genre = genre,
                    auto_approve = autoApprove,
                    cover_image_url = coverImageUrl,
                    budget_ceiling = budgetCeiling
                )
            )
        ) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to update project settings")
    }

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

    suspend fun updateBudgetLimit(projectId: String, limit: Int): Resource<JsonMap> = try {
        mapEnvelope(apiService.updateProjectBudget(projectId, UpdateBudgetRequest(limit))) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to update budget limit")
    }

    suspend fun updateBudgetLimit(
        projectId: String = currentProjectId(),
        limit: Double
    ): Resource<JsonMap> {
        return updateBudgetLimit(projectId, limit.toInt())
    }

    suspend fun fetchBible(projectId: String): Resource<JsonMap> = try {
        mapEnvelope(apiService.getProjectBible(projectId)) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to load Story Bible")
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

    suspend fun fetchCharacters(projectId: String): Resource<List<Character>> = try {
        mapEnvelope(apiService.getProjectCharacters(projectId)) { list ->
            list.mapIndexed { idx, dto ->
                val resolvedCode = dto.character_code?.takeIf { it.isNotBlank() }
                    ?: dto.id?.take(8)
                    ?: "CHAR_${String.format("%03d", idx + 1)}"
                val resolvedName = dto.name?.takeIf { it.isNotBlank() } ?: resolvedCode
                val dnaText = dto.visual_dna?.entries
                    ?.filter { it.value != null && it.value.toString().isNotBlank() }
                    ?.joinToString("\n") { "${it.key}: ${it.value}" }
                    ?: dto.description
                    ?: "Visual DNA not extracted yet"
                val voiceText = dto.voice_config?.get("voice_id")?.toString() ?: "UNSET"
                val roleText = dto.role ?: dto.visual_dna?.get("role")?.toString() ?: "UNASSIGNED"
                val statusText = dto.initial_status ?: "alive"
                Character(
                    id = resolvedCode,
                    name = resolvedName,
                    role = roleText,
                    visualDna = dnaText,
                    voiceId = voiceText,
                    status = statusText,
                    imageUrl = dto.image_url
                )
            }
        }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to load characters")
    }

    suspend fun fetchCharactersRaw(projectId: String): Resource<List<CharacterDto>> = try {
        mapEnvelope(apiService.getProjectCharacters(projectId)) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to load raw characters")
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

    suspend fun fetchEpisodes(projectId: String): Resource<List<Episode>> = try {
        mapEnvelope(apiService.getProjectEpisodes(projectId)) { list ->
            list.map { dto ->
                val rawTitle = dto.title?.trim().orEmpty()
                val cleanTitle = if (rawTitle.isBlank() || rawTitle.equals("Episode ${dto.episode_no}", ignoreCase = true)) {
                    "Untitled Episode"
                } else {
                    rawTitle
                }
                Episode(
                    id = dto.id,
                    episodeNumber = dto.episode_no,
                    title = cleanTitle,
                    status = dto.current_state ?: dto.status ?: "DRAFT",
                    synopsis = dto.synopsis,
                    updatedAt = dto.updated_at,
                    qcStatus = dto.qc_status,
                    scriptText = dto.script_json?.toString(),
                    masterVideoUrl = dto.master_video_url,
                    thumbnailUrl = dto.thumbnail_url
                )
            }
        }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to load episodes")
    }

    suspend fun fetchEpisodesRaw(projectId: String = currentProjectId()): Resource<List<EpisodeDto>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getProjectEpisodes(projectId)
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

    suspend fun fetchEpisodePlan(projectId: String, episodeNo: Int): Resource<JsonMap> = try {
        mapEnvelope(apiService.getEpisodePlan(projectId, episodeNo)) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to load episode plan")
    }

    suspend fun fetchScenes(projectId: String, episodeNo: Int): Resource<List<SceneDto>> = try {
        mapEnvelope(apiService.getEpisodeScenes(projectId, episodeNo)) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to load scenes")
    }

    suspend fun fetchScenesRaw(projectId: String = currentProjectId(), episodeNo: Int = 1): Resource<List<SceneDto>> {
        return fetchScenes(projectId, episodeNo)
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

    suspend fun fetchProjectMedia(projectId: String): Resource<JsonMap> = try {
        mapEnvelope(apiService.getProjectMedia(projectId)) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to load project media")
    }

    suspend fun fetchJobs(projectId: String): Resource<List<JobDto>> = try {
        mapEnvelope(apiService.getProjectJobs(projectId)) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to load jobs")
    }

    suspend fun fetchJobDetail(projectId: String, jobId: String): Resource<JobDto> = try {
        mapEnvelope(apiService.getJobDetail(projectId, jobId)) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to load job details")
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

    suspend fun fetchRetries(projectId: String): Resource<List<JsonMap>> = try {
        mapEnvelope(apiService.getProjectRetries(projectId)) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to load retry history")
    }

    suspend fun fetchApprovals(projectId: String): Resource<List<ApprovalDecisionDto>> = try {
        mapEnvelope(apiService.getProjectApprovals(projectId)) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to load approvals")
    }

    suspend fun submitApprovalDecision(
        projectId: String,
        approvalId: String,
        decision: String,
        reason: String,
        resourceVersion: Int = 1
    ): Resource<ApprovalDecisionDto> = try {
        val key = "apr-${UUID.randomUUID()}"
        mapEnvelope(
            apiService.decideApproval(projectId, approvalId, key, DecideApprovalRequest(decision, reason, resourceVersion))
        ) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to submit decision")
    }

    suspend fun chatWithBrain(
        message: String,
        projectId: String = currentProjectId(),
        currentScreen: String? = null,
        conversationId: String? = null
    ): Resource<AiChatResponse> = try {
        val req = AiChatRequest(
            message = message,
            project_id = projectId.takeIf { it.isNotBlank() },
            skill_key = "cinematic_director",
            current_screen = currentScreen,
            conversation_id = conversationId,
            request_id = UUID.randomUUID().toString()
        )
        val response = apiService.chatWithAi(req)
        if (response.isSuccessful && response.body() != null) {
            Resource.Success(response.body()!!)
        } else if (response.code() == 429) {
            Resource.Error("Rate limit or Quota reached (HTTP 429). Please wait or check BYOD keys.", 429)
        } else {
            Resource.Error("AI Director HTTP ${response.code()}", response.code())
        }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "AI Director unreachable")
    }

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

    suspend fun submitStudioCommand(
        projectId: String,
        intent: CommandIntent,
        mode: String = "SAVE",
        episodeNo: Int? = null,
        payload: JsonMap = emptyMap(),
        customIdempotencyKey: String? = null
    ): Resource<SubmitCommandResultDto> {
        if (projectId.isBlank() && intent != CommandIntent.CREATE_PROJECT) {
            return Resource.Error("Please select or create a project first.")
        }
        val safeProjId = projectId.ifBlank { "00000000-0000-0000-0000-000000000000" }
        val idempotencyKey = customIdempotencyKey ?: "cmd-${intent.name.lowercase()}-${UUID.randomUUID()}"
        return try {
            val response = apiService.submitCommand(
                idempotencyKey,
                CreateCommandRequest(safeProjId, intent.name, mode, episodeNo, payload)
            )
            if (response.isSuccessful && response.body()?.data != null) {
                Resource.Success(response.body()!!.data!!)
            } else {
                Resource.Error("Command rejected (HTTP ${response.code()})", response.code())
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Command dispatch failed")
        }
    }

    suspend fun fetchCommandStatus(commandId: String): Resource<ApiCommandRecordDto> = try {
        mapEnvelope(apiService.getCommandStatus(commandId)) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Status poll failed")
    }

    suspend fun triggerQuickMedia(projectId: String, isVideo: Boolean, prompt: String): Resource<SubmitCommandResultDto> = try {
        val key = "quick-${if (isVideo) "vid" else "img"}-${UUID.randomUUID()}"
        val response = if (isVideo) {
            apiService.quickGenerateVideo(projectId, key, QuickGenerateRequest(prompt = prompt, duration_sec = 5))
        } else {
            apiService.quickGenerateImage(projectId, key, QuickGenerateRequest(prompt = prompt))
        }
        mapEnvelope(response) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Quick media request failed")
    }

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

    suspend fun triggerVisionAnalysis(
        projectId: String,
        mediaUrl: String,
        prompt: String = "Extract 11-field Visual DNA",
        characterCode: String? = null,
        saveToCharacter: Boolean = false
    ): Resource<SubmitCommandResultDto> = try {
        val key = "vision-${UUID.randomUUID()}"
        mapEnvelope(
            apiService.analyzeVision(
                projectId,
                key,
                VisionAnalyzeRequest(
                    media_url = mediaUrl,
                    prompt = prompt,
                    character_code = characterCode,
                    save_to_character = saveToCharacter
                )
            )
        ) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Vision request failed")
    }

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

    suspend fun fetchAnalytics(projectId: String): Resource<JsonMap> = try {
        mapEnvelope(apiService.getProjectAnalytics(projectId)) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to load analytics")
    }

    suspend fun fetchAuditLogs(projectId: String): Resource<List<JsonMap>> = try {
        mapEnvelope(apiService.getProjectAudit(projectId)) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to load audit logs")
    }

    suspend fun fetchNotifications(projectId: String): Resource<List<JsonMap>> = try {
        mapEnvelope(apiService.getProjectNotifications(projectId)) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to load notifications")
    }

    suspend fun fetchAdminWorkflows(projectId: String = currentProjectId()): Resource<List<JsonMap>> = try {
        mapEnvelope(apiService.getAdminWorkflows(projectId)) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to load workflows")
    }

    suspend fun fetchAdminWorkers(projectId: String = currentProjectId()): Resource<List<JsonMap>> = try {
        mapEnvelope(apiService.getAdminWorkers(projectId)) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to load workers")
    }

    suspend fun fetchAdminQueues(projectId: String = currentProjectId()): Resource<JsonMap> = try {
        mapEnvelope(apiService.getAdminQueues(projectId)) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to load queues")
    }

    suspend fun fetchLedgerSummary(projectId: String = currentProjectId()): Resource<JsonMap> = try {
        mapEnvelope(apiService.getLedgerSummary(projectId)) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to load ledger summary")
    }

    suspend fun fetchAdminLedgerSummary(): Resource<JsonMap> =
        fetchLedgerSummary(currentProjectId())

    suspend fun fetchQuota(projectId: String = currentProjectId()): Resource<JsonMap> = try {
        mapEnvelope(apiService.getProjectQuota(projectId)) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to load quota")
    }

    suspend fun fetchProjectQuota(projectId: String = currentProjectId()): Resource<JsonMap> =
        fetchQuota(projectId)

    suspend fun fetchCanon(projectId: String): Resource<List<JsonMap>> = try {
        mapEnvelope(apiService.getProjectCanon(projectId)) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to load canon")
    }

    suspend fun fetchCanonProposals(projectId: String): Resource<List<JsonMap>> = try {
        mapEnvelope(apiService.getCanonProposals(projectId)) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to load canon proposals")
    }

    suspend fun fetchContinuity(projectId: String): Resource<List<JsonMap>> = try {
        mapEnvelope(apiService.getProjectContinuity(projectId)) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to load continuity checks")
    }

    suspend fun fetchCategoryItems(projectId: String, categoryPath: String): Resource<List<JsonMap>> = try {
        mapEnvelope(apiService.getProjectCategoryList(projectId, categoryPath)) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to load $categoryPath")
    }

    suspend fun fetchCharacterReferenceHistory(projectId: String, characterId: String): Resource<List<JsonMap>> = try {
        mapEnvelope(apiService.getCharacterReferenceHistory(projectId, characterId)) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to load character reference history")
    }

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

    suspend fun deletePrompt(promptId: String): Resource<JsonMap> = try {
        mapEnvelope(apiService.deleteSystemPrompt(promptId)) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to delete prompt")
    }

    suspend fun deleteSystemPrompt(promptId: String): Resource<JsonMap> =
        deletePrompt(promptId)

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
            val response = apiService.saveGpuConfigRaw(payload)
            if (response.isSuccessful) {
                Resource.Success(response.body()?.data ?: payload)
            } else {
                Resource.Error("Failed to save infrastructure config (${response.code()})", response.code())
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to save infrastructure config")
        }
    }

    suspend fun fetchHealthDetail(): Resource<HealthResponseDto> = try {
        val res = apiService.getAnimeHealth()
        if (res.isSuccessful && res.body() != null) Resource.Success(res.body()!!)
        else Resource.Error("Health HTTP ${res.code()}", res.code())
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Backend unreachable")
    }

    suspend fun checkBackendHealth(): Boolean =
        (fetchHealthDetail() as? Resource.Success)?.data?.ok == true

    suspend fun resolveConfiguredAiProviderBadge(): String = withContext(Dispatchers.IO) {
        try {
            if (!checkBackendHealth()) return@withContext "AI: Disconnected"
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
            return@withContext TokenManager.cachedProviderLabel ?: "Groq • Gemini"
        } catch (_: Exception) {
            return@withContext "AI: Disconnected"
        }
    }
}
EOF
echo "✅ Updated core/network/AnimeRepository.kt"

# ==============================================================================
# 8. SharedComponents.kt (Includes KeyValueMapList + Null-Safe GlobalAiDirectorBottomSheet)
# ==============================================================================
cat << 'EOF' > "$BASE_DIR/ui/components/SharedComponents.kt"
package org.dpdns.bnai.anime.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.auth.PersistedChatMessage
import org.dpdns.bnai.anime.core.auth.TokenManager
import org.dpdns.bnai.anime.core.model.Project
import org.dpdns.bnai.anime.core.network.AnimeRepository
import org.dpdns.bnai.anime.core.network.CommandIntent
import org.dpdns.bnai.anime.core.network.JsonMap
import org.dpdns.bnai.anime.core.network.Resource
import org.dpdns.bnai.anime.ui.designsystem.MarkdownText
import org.dpdns.bnai.anime.ui.designsystem.StatusChip
import org.dpdns.bnai.anime.ui.designsystem.StudioTokens
import java.time.Instant
import java.time.temporal.ChronoUnit

fun formatInt(value: Any?): String {
    return when (value) {
        null -> "0"
        is Int -> value.toString()
        is Long -> value.toString()
        is Double -> value.toLong().toString()
        is Float -> value.toLong().toString()
        is Number -> value.toLong().toString()
        is String -> value.toDoubleOrNull()?.toLong()?.toString() ?: value
        else -> value.toString()
    }
}

fun formatRelativeTime(isoString: String?): String {
    if (isoString.isNullOrBlank()) return "Just now"
    return try {
        val instant = Instant.parse(isoString)
        val now = Instant.now()
        val mins = ChronoUnit.MINUTES.between(instant, now)
        when {
            mins < 1 -> "Just now"
            mins < 60 -> "${mins}m ago"
            mins < 1440 -> "${mins / 60}h ago"
            else -> "${mins / 1440}d ago"
        }
    } catch (_: Exception) {
        isoString.take(16).replace("T", " ")
    }
}

fun cleanDisplayTitle(raw: String?, fallback: String = "Studio Item"): String {
    if (raw.isNullOrBlank()) return fallback
    val uuidRegex = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
    if (uuidRegex.matches(raw.trim())) {
        return "$fallback (#${raw.take(6)})"
    }
    return raw
}

fun friendlyPipelineStatus(rawStatus: String?): String {
    return when (rawStatus?.uppercase()?.trim()) {
        "RECONCILING" -> "Rendering..."
        "QUEUED" -> "Waiting in Queue..."
        "DISPATCHED", "ACKNOWLEDGED", "RUNNING" -> "Generating..."
        "SUCCEEDED", "COMPLETED" -> "Completed"
        "FAILED", "ERROR", "DEAD_LETTER" -> "Failed"
        "CANCELLED" -> "Cancelled"
        "IDLE", null, "" -> "Idle"
        else -> rawStatus
    }
}

@Composable
fun <T> ResourceStateBox(
    resource: Resource<T>,
    emptyMessage: String = "No records found for this project.",
    onRetry: (() -> Unit)? = null,
    content: @Composable (T) -> Unit
) {
    when (resource) {
        is Resource.Idle, is Resource.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = StudioTokens.Primary)
            }
        }
        is Resource.Empty -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(emptyMessage, style = MaterialTheme.typography.bodyMedium)
                    if (onRetry != null) {
                        OutlinedButton(onClick = onRetry) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Refresh")
                        }
                    }
                }
            }
        }
        is Resource.Error -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = StudioTokens.Error.copy(alpha = 0.12f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Failed to Load Data",
                        style = MaterialTheme.typography.titleMedium,
                        color = StudioTokens.Error,
                        fontWeight = FontWeight.Bold
                    )
                    Text(text = resource.message, style = MaterialTheme.typography.bodySmall)
                    if (onRetry != null) {
                        OutlinedButton(onClick = onRetry) {
                            Icon(Icons.Default.Refresh, contentDescription = "Retry", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Retry")
                        }
                    }
                }
            }
        }
        is Resource.Success -> {
            content(resource.data)
        }
    }
}

@Composable
fun StatusBadge(label: String) {
    StatusChip(label = label)
}

@Composable
fun InfoTooltipButton(title: String, description: String) {
    var showDialog by remember { mutableStateOf(false) }
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(title, fontWeight = FontWeight.Bold) },
            text = { Text(description) },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) { Text("Got it") }
            }
        )
    }
    IconButton(
        onClick = { showDialog = true },
        modifier = Modifier.size(36.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = "Info about $title",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun KeyValueMapList(items: List<JsonMap>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        items(items) { map ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val rawTitle = (map["title"] ?: map["name"] ?: map["arc_name"] ?: map["power_name"]
                        ?: map["rule_code"] ?: map["location_code"] ?: map["artifact_code"]
                        ?: map["character_code"] ?: map["wf_key"] ?: map["action"] ?: "Studio Item").toString()
                    val status = (map["status"] ?: map["state"] ?: map["verification_status"] ?: map["priority"])?.toString()
                    val timestamp = (map["updated_at"] ?: map["created_at"] ?: map["checked_at"])?.toString()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(cleanDisplayTitle(rawTitle), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        if (!status.isNullOrBlank()) StatusBadge(status)
                    }

                    map.entries.forEach { (k, v) ->
                        if (k !in setOf("id", "project_id", "user_id", "workspace_id", "title", "name", "status", "state", "created_at", "updated_at", "checked_at") && v != null) {
                            val label = k.replace("_", " ").replaceFirstChar { it.uppercase() }
                            val formattedVal = if (v is Number) formatInt(v) else v.toString()
                            Text("$label: $formattedVal", style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    if (!timestamp.isNullOrBlank()) {
                        Text(
                            formatRelativeTime(timestamp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectSelectorBar(
    onProjectChanged: (String) -> Unit = {},
    onRefreshClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager.init(context) }
    val repository = remember { AnimeRepository() }
    var projects by remember { mutableStateOf<List<Project>>(emptyList()) }
    var expanded by remember { mutableStateOf(false) }
    var selectedProjectId by remember { mutableStateOf(repository.currentProjectId()) }

    LaunchedEffect(Unit) {
        when (val res = repository.fetchProjects()) {
            is Resource.Success -> {
                projects = res.data
                if (selectedProjectId.isBlank() && res.data.isNotEmpty()) {
                    selectedProjectId = res.data.first().id
                    tokenManager.saveActiveProjectId(selectedProjectId)
                    onProjectChanged(selectedProjectId)
                }
            }
            else -> {}
        }
    }

    val currentProjectName = projects.find { it.id == selectedProjectId }?.name
        ?: if (selectedProjectId.isNotBlank()) "Project #${selectedProjectId.take(6)}" else "Select Project"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.weight(1f)
        ) {
            OutlinedTextField(
                value = currentProjectName,
                onValueChange = {},
                readOnly = true,
                label = { Text("Active Studio Project") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                singleLine = true,
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                projects.forEach { proj ->
                    DropdownMenuItem(
                        text = { Text(proj.name, fontWeight = if (proj.id == selectedProjectId) FontWeight.Bold else FontWeight.Normal) },
                        onClick = {
                            selectedProjectId = proj.id
                            tokenManager.saveActiveProjectId(proj.id)
                            expanded = false
                            onProjectChanged(proj.id)
                        }
                    )
                }
            }
        }
        if (onRefreshClick != null) {
            IconButton(
                onClick = onRefreshClick,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh project data")
            }
        }
    }
}

fun detectSuggestedIntentFromReply(replyText: String?): CommandIntent? {
    val upper = replyText?.uppercase().orEmpty()
    return when {
        upper.contains("GENERATE_EPISODE") -> CommandIntent.GENERATE_EPISODE
        upper.contains("CONTINUE_EPISODE") -> CommandIntent.CONTINUE_EPISODE
        upper.contains("SAVE_BIBLE") -> CommandIntent.SAVE_BIBLE
        upper.contains("CHECK_CONTINUITY") -> CommandIntent.CHECK_CONTINUITY
        upper.contains("RUN_QC") -> CommandIntent.RUN_QC
        else -> null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalAiDirectorBottomSheet(
    currentScreen: String = "STUDIO",
    currentScreenName: String = currentScreen,
    onDismiss: () -> Unit,
    onNavigateToRoute: (String) -> Unit = {},
    onOpenFullAiBrain: () -> Unit = { onNavigateToRoute("ai_brain") }
) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager.init(context) }
    val repository = remember { AnimeRepository() }
    val scope = rememberCoroutineScope()
    val projectId = repository.currentProjectId()
    val effectiveScreen = currentScreenName.ifBlank { currentScreen }

    var messages by remember(projectId) { mutableStateOf(tokenManager.getProjectChatHistory(projectId)) }
    var prompt by rememberSaveable(projectId) { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var trackedCmdId by remember(projectId) { mutableStateOf(tokenManager.getActiveCommandId(projectId, "ai_director_sheet")) }
    var trackedCmdStatus by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(trackedCmdId, projectId) {
        val cid = trackedCmdId ?: return@LaunchedEffect
        while (true) {
            when (val st = repository.fetchCommandStatus(cid)) {
                is Resource.Success -> {
                    val statusUpper = st.data.status?.uppercase() ?: "QUEUED"
                    trackedCmdStatus = statusUpper
                    if (statusUpper in listOf("SUCCEEDED", "COMPLETED", "FAILED", "ERROR", "CANCELLED")) {
                        tokenManager.saveActiveCommandId(projectId, "ai_director_sheet", null)
                        break
                    }
                }
                else -> break
            }
            delay(3000)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = StudioTokens.Primary)
                    Column {
                        Text("AI Studio Director", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = "Context: $effectiveScreen • Project #${projectId.take(6).ifBlank { "Global" }}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (messages.isNotEmpty()) {
                        IconButton(onClick = {
                            tokenManager.clearProjectChatHistory(projectId)
                            messages = emptyList()
                        }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear project conversation")
                        }
                    }
                    TextButton(onClick = {
                        onDismiss()
                        onOpenFullAiBrain()
                    }) {
                        Text("Full Screen")
                    }
                }
            }

            if (!trackedCmdId.isNullOrBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = StudioTokens.Primary.copy(alpha = 0.12f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Command #${trackedCmdId?.take(8)}: ${friendlyPipelineStatus(trackedCmdStatus ?: "QUEUED")}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        StatusBadge(trackedCmdStatus ?: "QUEUED")
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 180.dp, max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (messages.isEmpty()) {
                    item {
                        Text(
                            text = "Ask the AI Director about your Story Bible, Episode Script, Character DNA, or Continuity. Conversation persists for this project.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                }
                items(messages, key = { it.id }) { msg ->
                    val isUser = msg.role == "user"
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isUser) StudioTokens.Primary.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (isUser) "You" else "AI Director",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isUser) StudioTokens.Primary else StudioTokens.Secondary
                            )
                            MarkdownText(markdown = msg.content)

                            if (!isUser && !msg.suggestedIntent.isNullOrBlank()) {
                                val intentEnum = runCatching { CommandIntent.valueOf(msg.suggestedIntent) }.getOrNull()
                                if (intentEnum != null) {
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Suggested Action: ${intentEnum.name}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Button(
                                            onClick = {
                                                scope.launch {
                                                    val res = repository.submitStudioCommand(
                                                        projectId = projectId,
                                                        intent = intentEnum
                                                    )
                                                    if (res is Resource.Success) {
                                                        val cid = res.data.command_id.orEmpty()
                                                        if (cid.isNotBlank()) {
                                                            trackedCmdId = cid
                                                            trackedCmdStatus = res.data.status ?: "QUEUED"
                                                            tokenManager.saveActiveCommandId(projectId, "ai_director_sheet", cid)
                                                        }
                                                    }
                                                }
                                            },
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Confirm & Run", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    placeholder = { Text("Ask AI Director...") },
                    modifier = Modifier.weight(1f),
                    maxLines = 3
                )
                IconButton(
                    onClick = {
                        val cleanPrompt = prompt.trim()
                        if (cleanPrompt.isEmpty() || isSending) return@IconButton
                        prompt = ""
                        isSending = true
                        messages = tokenManager.appendProjectChatMessage(
                            projectId,
                            PersistedChatMessage(role = "user", content = cleanPrompt)
                        )
                        scope.launch {
                            val convId = tokenManager.getProjectConversationId(projectId)
                            when (val res = repository.askAiDirector(cleanPrompt, effectiveScreen, convId)) {
                                is Resource.Success -> {
                                    val replyText = res.data.reply ?: res.data.message ?: "Done."
                                    val suggested = detectSuggestedIntentFromReply(replyText)?.name
                                    messages = tokenManager.appendProjectChatMessage(
                                        projectId,
                                        PersistedChatMessage(
                                            role = "assistant",
                                            content = replyText,
                                            suggestedIntent = suggested
                                        )
                                    )
                                }
                                is Resource.Error -> {
                                    messages = tokenManager.appendProjectChatMessage(
                                        projectId,
                                        PersistedChatMessage(
                                            role = "assistant",
                                            content = "**Error:** ${res.message}"
                                        )
                                    )
                                }
                                else -> {}
                            }
                            isSending = false
                        }
                    },
                    enabled = prompt.isNotBlank() && !isSending,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (prompt.isNotBlank() && !isSending) StudioTokens.Primary else Color.Gray.copy(alpha = 0.3f))
                ) {
                    if (isSending) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                    } else {
                        Icon(Icons.Default.Send, contentDescription = "Send message", tint = Color.White)
                    }
                }
            }
        }
    }
}
EOF
echo "✅ Updated ui/components/SharedComponents.kt"

# ==============================================================================
# 9. AppNavGraph.kt (Clean Full File — Fixes lines 171, 173, 354, 422)
# ==============================================================================
cat << 'EOF' > "$BASE_DIR/ui/navigation/AppNavGraph.kt"
package org.dpdns.bnai.anime.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.auth.TokenManager
import org.dpdns.bnai.anime.core.model.Character
import org.dpdns.bnai.anime.core.network.AnimeRepository
import org.dpdns.bnai.anime.ui.advanced.*
import org.dpdns.bnai.anime.ui.analytics.AnalyticsScreen
import org.dpdns.bnai.anime.ui.auth.AuthScreen
import org.dpdns.bnai.anime.ui.brain.AIBrainScreen
import org.dpdns.bnai.anime.ui.character.CharacterDetailScreen
import org.dpdns.bnai.anime.ui.character.CharacterListScreen
import org.dpdns.bnai.anime.ui.components.GlobalAiDirectorBottomSheet
import org.dpdns.bnai.anime.ui.episode.EpisodePlannerScreen
import org.dpdns.bnai.anime.ui.library.MediaLibraryScreen
import org.dpdns.bnai.anime.ui.notifications.NotificationScreen
import org.dpdns.bnai.anime.ui.owner.*
import org.dpdns.bnai.anime.ui.production.ProductionScreen
import org.dpdns.bnai.anime.ui.production.SceneDetailScreen
import org.dpdns.bnai.anime.ui.project.BibleScreen
import org.dpdns.bnai.anime.ui.project.ProjectDetailScreen
import org.dpdns.bnai.anime.ui.publish.PublishScreen
import org.dpdns.bnai.anime.ui.qc.QCScreen
import org.dpdns.bnai.anime.ui.review.ReviewInboxScreen
import org.dpdns.bnai.anime.ui.settings.SettingsScreen
import org.dpdns.bnai.anime.ui.studio.PromptStudioScreen
import org.dpdns.bnai.anime.ui.studio.StudioDashboardScreen
import org.dpdns.bnai.anime.ui.tools.QuickToolsScreen

data class NavItem(val route: String, val label: String, val icon: ImageVector)

@Composable
fun AppNavGraph(navController: NavHostController) {
    val tokenManager = remember { TokenManager.getInstance() }
    val repository = remember { AnimeRepository() }
    val startDest = if (!tokenManager.getToken().isNullOrBlank()) "studio" else "auth"
    val expiredMessage by tokenManager.sessionExpiredMessage.collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: startDest
    var selectedCharacter by remember { mutableStateOf<Character?>(null) }

    var isBackendHealthy by remember { mutableStateOf(true) }
    var activeProviderBadge by remember { mutableStateOf(tokenManager.getConfiguredProviderLabel()) }
    var showGlobalAiSheet by remember { mutableStateOf(false) }

    val expandedSections = remember {
        mutableStateMapOf(
            "WORKSPACE" to true,
            "STORY" to true,
            "PRODUCTION" to false,
            "INSIGHTS" to false,
            "OWNER CONTROL" to false
        )
    }

    LaunchedEffect(currentRoute) {
        if (currentRoute != "auth") {
            isBackendHealthy = repository.checkBackendHealth()
            activeProviderBadge = repository.resolveConfiguredAiProviderBadge()
            tokenManager.saveConfiguredProviderLabel(activeProviderBadge)
        }
    }

    LaunchedEffect(expiredMessage) {
        if (!expiredMessage.isNullOrBlank() && currentRoute != "auth") {
            navController.navigate("auth") {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    val bottomDestinations = listOf(
        NavItem("studio", "Home", Icons.Default.Home),
        NavItem("project_detail", "Projects", Icons.Default.Folder),
        NavItem("ai_brain", "AI Director", Icons.Default.AutoAwesome),
        NavItem("production", "Pipeline", Icons.Default.Movie),
        NavItem("episodes", "Episodes", Icons.Default.VideoLibrary)
    )

    val drawerSections = remember(tokenManager.isOwner()) {
        buildList {
            add("WORKSPACE" to listOf(
                "studio" to "Home Dashboard",
                "project_detail" to "Active Project",
                "ai_brain" to "AI Director",
                "quick_tools" to "Quick Generate",
                "prompt_studio" to "Prompt Studio"
            ))
            add("STORY" to listOf(
                "bible" to "Story Bible",
                "characters" to "Characters",
                "character_deep_dive" to "Character Deep Dive",
                "lore_explorer" to "World Wiki",
                "episodes" to "Episodes",
                "continuity" to "Continuity Checker",
                "canon" to "Canon & Facts",
                "vision_analysis" to "Vision DNA"
            ))
            add("PRODUCTION" to listOf(
                "production" to "Pipeline Monitor",
                "scenes" to "Scene Inspector",
                "jobs_monitor" to "Pipeline Jobs",
                "media_library" to "Media Library",
                "qc" to "Quality Check",
                "review_inbox" to "Review & Approvals",
                "publish" to "Publish Studio"
            ))
            add("INSIGHTS" to listOf(
                "analytics" to "Project Analytics",
                "quota_dashboard" to "Quota & Usage",
                "budget_ledger" to "Resource Usage (Credits)",
                "notifications" to "Notifications",
                "audit_log" to "Audit Activity"
            ))
            if (tokenManager.isOwner()) {
                add("OWNER CONTROL" to listOf(
                    "owner_overview" to "Owner Overview",
                    "workflows" to "Workflow Engine Status",
                    "workers" to "GPU & Worker Fleet",
                    "admin_queues" to "Pipeline Queues",
                    "database" to "Database Connectivity"
                ))
            }
        }
    }

    if (currentRoute == "auth") {
        NavHost(navController = navController, startDestination = startDest) {
            composable("auth") { AuthScreen(navController = navController) }
            composable("studio") { StudioDashboardScreen() }
        }
        return
    }

    if (showGlobalAiSheet) {
        GlobalAiDirectorBottomSheet(
            currentScreen = currentRoute,
            currentScreenName = currentRoute,
            onDismiss = { showGlobalAiSheet = false },
            onNavigateToRoute = { target -> navController.navigate(target) },
            onOpenFullAiBrain = { navController.navigate("ai_brain") { launchSingleTop = true } }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(modifier = Modifier.fillMaxHeight().padding(12.dp)) {
                    val userEmail = tokenManager.getUserEmail().ifBlank { "studio@bnai.dpdns.org" }
                    val userRole = tokenManager.getUserRole().uppercase()
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = userEmail.take(1).uppercase(),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(userEmail.substringBefore("@"), style = MaterialTheme.typography.titleMedium, maxLines = 1)
                                    Text(userEmail, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                AssistChip(onClick = {}, label = { Text(userRole, fontSize = 10.sp) })
                            }
                            OutlinedButton(
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    navController.navigate("ai_brain") { launchSingleTop = true }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("AI Director & Chat History")
                            }
                        }
                    }

                    HorizontalDivider()

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 6.dp)
                    ) {
                        drawerSections.forEach { (sectionTitle, routes) ->
                            val isExpanded = expandedSections[sectionTitle] == true
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { expandedSections[sectionTitle] = !isExpanded }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = sectionTitle,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = sectionTitle,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (isExpanded) {
                                routes.forEach { (route, title) ->
                                    NavigationDrawerItem(
                                        label = { Text(title) },
                                        selected = currentRoute == route,
                                        onClick = {
                                            scope.launch { drawerState.close() }
                                            navController.navigate(route) { launchSingleTop = true }
                                        },
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                scope.launch { drawerState.close() }
                                navController.navigate("settings") { launchSingleTop = true }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Settings")
                        }
                        Button(
                            onClick = {
                                scope.launch { drawerState.close() }
                                tokenManager.clearToken()
                                navController.navigate("auth") {
                                    popUpTo(0) { inclusive = true }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Logout")
                        }
                    }
                }
            }
        }
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isTablet = maxWidth >= 600.dp
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.clickable {
                                    navController.navigate("studio") { launchSingleTop = true }
                                }
                            ) {
                                Text("AI Studio", fontWeight = FontWeight.Bold)
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (isBackendHealthy) Color(0xFF10B981) else Color(0xFFEF4444))
                                )
                            }
                        },
                        navigationIcon = {
                            val isRootDestination = currentRoute in setOf("studio", "project_detail", "ai_brain", "production", "episodes")
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
                        },
                        actions = {
                            AssistChip(
                                onClick = { navController.navigate("settings") { launchSingleTop = true } },
                                label = { Text(activeProviderBadge, fontSize = 11.sp) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                    )
                },
                floatingActionButton = {
                    if (currentRoute !in listOf("ai_brain", "studio")) {
                        FloatingActionButton(
                            onClick = { showGlobalAiSheet = true },
                            containerColor = MaterialTheme.colorScheme.primary
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "Ask AI Director", tint = Color.White)
                        }
                    }
                },
                bottomBar = {
                    if (!isTablet) {
                        NavigationBar {
                            bottomDestinations.forEach { item ->
                                NavigationBarItem(
                                    selected = currentRoute == item.route,
                                    onClick = { navController.navigate(item.route) { launchSingleTop = true } },
                                    icon = { Icon(item.icon, contentDescription = item.label) },
                                    label = {
                                        Text(
                                            text = item.label,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            fontSize = 11.sp
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                    if (isTablet) {
                        NavigationRail {
                            bottomDestinations.forEach { item ->
                                NavigationRailItem(
                                    selected = currentRoute == item.route,
                                    onClick = { navController.navigate(item.route) { launchSingleTop = true } },
                                    icon = { Icon(item.icon, contentDescription = item.label) },
                                    label = { Text(item.label, maxLines = 1) }
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                        NavHost(navController = navController, startDestination = startDest) {
                            composable("auth") { AuthScreen(navController = navController) }
                            composable("studio") {
                                StudioDashboardScreen(
                                    onProjectClick = { navController.navigate("project_detail") },
                                    onOpenReviews = { navController.navigate("review_inbox") },
                                    onOpenEpisodes = { navController.navigate("episodes") }
                                )
                            }
                            composable("project_detail") {
                                ProjectDetailScreen(
                                    onOpenBible = { navController.navigate("bible") },
                                    onOpenCharacters = { navController.navigate("characters") },
                                    onOpenEpisodes = { navController.navigate("episodes") },
                                    onOpenReviews = { navController.navigate("review_inbox") },
                                    onOpenReviewInbox = { navController.navigate("review_inbox") },
                                    onOpenAnalytics = { navController.navigate("analytics") }
                                )
                            }
                            composable("bible") { BibleScreen() }
                            composable("characters") {
                                CharacterListScreen(
                                    onCharacterClick = { ch ->
                                        selectedCharacter = ch
                                        navController.navigate("character_detail")
                                    },
                                    onOpenDeepDive = { id ->
                                        selectedCharacter = Character(id, id, "", "", "")
                                        navController.navigate("character_deep_dive")
                                    }
                                )
                            }
                            composable("character_detail") {
                                CharacterDetailScreen(
                                    characterId = selectedCharacter?.id.orEmpty(),
                                    initialCharacter = selectedCharacter,
                                    onOpenDeepDive = { navController.navigate("character_deep_dive") }
                                )
                            }
                            composable("episodes") {
                                EpisodePlannerScreen(
                                    onStartProduction = { cmdId -> navController.navigate("production/$cmdId") },
                                    onOpenScenes = { epNo -> navController.navigate("scenes/$epNo") }
                                )
                            }
                            composable("production") {
                                ProductionScreen(
                                    onViewVideoClick = { navController.navigate("qc") },
                                    onOpenSceneInspector = { navController.navigate("scenes") },
                                    onOpenMediaLibrary = { navController.navigate("media_library") }
                                )
                            }
                            composable("production/{cmdId}") { entry ->
                                ProductionScreen(
                                    episodeId = entry.arguments?.getString("cmdId").orEmpty(),
                                    onViewVideoClick = { navController.navigate("qc") },
                                    onOpenSceneInspector = { navController.navigate("scenes") },
                                    onOpenMediaLibrary = { navController.navigate("media_library") }
                                )
                            }
                            composable("scenes") { SceneDetailScreen(1) }
                            composable("scenes/{epNo}") { entry ->
                                val ep = entry.arguments?.getString("epNo")?.toIntOrNull() ?: 1
                                SceneDetailScreen(ep)
                            }
                            composable("qc") { QCScreen(onProceedToPublish = { navController.navigate("publish") }) }
                            composable("publish") { PublishScreen() }
                            composable("review_inbox") { ReviewInboxScreen() }
                            composable("ai_brain") { AIBrainScreen() }
                            composable("quick_tools") {
                                QuickToolsScreen(onOpenProductionPoll = { cmdId -> navController.navigate("production/$cmdId") })
                            }
                            composable("settings") { SettingsScreen() }
                            composable("prompt_studio") { PromptStudioScreen() }
                            composable("owner_overview") { OwnerOverviewScreen() }
                            composable("workflows") { WorkflowManagerScreen() }
                            composable("workers") { WorkerManagerScreen() }
                            composable("database") { DatabaseScreen() }
                            composable("audit_log") { AuditLogScreen() }
                            composable("analytics") { AnalyticsScreen() }
                            composable("media_library") { MediaLibraryScreen() }
                            composable("notifications") { NotificationScreen() }
                            composable("vision_analysis") { VisionAnalysisScreen() }
                            composable("jobs_monitor") { JobsMonitorScreen() }
                            composable("quota_dashboard") { QuotaDashboardScreen() }
                            composable("canon") { CanonViewerScreen() }
                            composable("continuity") { ContinuityCheckerScreen() }
                            composable("lore_explorer") { LoreExplorerScreen() }
                            composable("character_deep_dive") {
                                CharacterDeepDiveScreen(characterId = selectedCharacter?.id.orEmpty())
                            }
                            composable("admin_queues") { AdminQueuesScreen() }
                            composable("budget_ledger") { BudgetLedgerScreen() }
                        }
                    }
                }
            }
        }
    }
}
EOF
echo "✅ Updated ui/navigation/AppNavGraph.kt"

echo "✅ ANDROID FIX BATCH 1 APPLIED SUCCESSFULLY! Ready for Batch 2."
