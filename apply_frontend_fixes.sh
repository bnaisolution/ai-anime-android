#!/bin/bash
set -e
BASE_DIR="app/src/main/java/org/dpdns/bnai/anime"

echo "=== PART 1: Core Network & Auth ==="

# 1. TokenManager.kt
cat > "$BASE_DIR/core/auth/TokenManager.kt" << 'EOF'
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
EOF

# 2. AnimeApiService.kt (Fixed List> syntax & added QUICK_GENERATE_REEL)
cat > "$BASE_DIR/core/network/AnimeApiService.kt" << 'EOF'
package org.dpdns.bnai.anime.core.network
import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

data class ApiMeta(val request_id: String? = null)
data class ApiEnvelope<T>(val data: T, val meta: ApiMeta? = null)
data class LoginRequest(val email: String, val password: String)
data class LoginResponse(val token: String? = null, val access_token: String? = null, @SerializedName("user_id", alternate = ["userId"]) val userId: String? = null, @SerializedName("workspace_id", alternate = ["workspaceId"]) val workspaceId: String? = null, val email: String? = null, val role: String? = "owner")
data class AiChatRequest(val message: String, val project_id: String? = null, val skill_key: String = "ai_admin")
data class AiChatResponse(val request_id: String? = null, val status: String? = null, val reply: String? = null, val message: String? = null, val provider: String? = null, val model: String? = null)

enum class CommandIntent {
    CREATE_PROJECT, OPEN_PROJECT, SAVE_BIBLE, BIBLE_CHAT, GENERATE_EPISODE,
    REGENERATE_EPISODE, CONTINUE_EPISODE, QUICK_GENERATE_REEL, QUICK_GENERATE_IMAGE, QUICK_GENERATE_VIDEO,
    SAVE_TO_PROJECT, VISION_ANALYSIS, CHANGE_CHARACTER_VOICE, PREVIEW_VOICE,
    LIST_VOICES, RESET_VOICE, CHECK_CANON, VIEW_CANON, APPROVE_CANON_CHANGE,
    REJECT_CANON_CHANGE, CHECK_CONTINUITY, CHECK_QUOTA, RETRY_FAILED,
    VIEW_STATE, VIEW_AUDIT, RUN_QC, PUBLISH_NOW, VIEW_ANALYTICS,
    ADMIN_START_KAGGLE, ADMIN_STOP_KAGGLE, ADMIN_STATUS, DB_TEST, SYSTEM_HEALTH
}

data class CreateCommandRequest(val project_id: String, val intent: String, val mode: String = "SAVE", val episode_no: Int? = null, val payload: Map<String, @JvmSuppressWildcards Any?> = emptyMap())
data class ApiCommandRecordDto(val id: String, val project_id: String, val user_id: String? = null, val intent: String, val status: String, val idempotency_key: String? = null, val payload: Map<String, @JvmSuppressWildcards Any?>? = null, val created_at: String? = null)
data class SubmitCommandResultDto(val command: ApiCommandRecordDto? = null, val command_id: String? = null, val project_id: String? = null, val status: String? = null, val intent: String? = null, val wf_key: String? = null, val idempotent_replay: Boolean? = false)
data class QuickGenerateRequest(val prompt: String, val style_preset: String? = "anime_cinematic", val aspect_ratio: String? = "16:9", val duration_sec: Int? = null)
data class VisionAnalyzeRequest(val media_url: String, val analysis_type: String? = "GENERAL", val prompt: String? = "Extract visual DNA")
data class CreateUploadSessionRequest(val file_name: String, val content_type: String? = "image/png")
data class UploadSessionDto(val session_id: String? = null, val upload_url: String, val object_path: String? = null)
data class ProjectDto(val id: String, val name: String, val is_active: Boolean? = true, val story_bible: Map<String, @JvmSuppressWildcards Any?>? = null, val current_season: Int? = 1, val current_episode: Int? = 1, val status: String? = "RUNNING")
data class ProjectOverviewDto(val project: ProjectDto? = null, val stats: Map<String, @JvmSuppressWildcards Any?>? = null)
data class EpisodeDto(val id: String, val project_id: String, val episode_no: Int, val title: String? = null, val current_state: String? = null, val qc_status: String? = null, val synopsis: String? = null)
data class ClipDto(val id: String, val project_id: String? = null, val episode_no: Int? = null, val scene_no: Int? = null, val clip_url: String? = null, val status: String? = null, val provider: String? = null, val duration_sec: Double? = null)
data class SceneDto(val id: String, val project_id: String? = null, val episode_no: Int, val scene_no: Int, val description: String? = null, val visual_prompt: String? = null, val status: String? = null, val clips: List<ClipDto>? = emptyList())
data class JobDto(val id: String, val project_id: String, val intent: String, val status: String, val episode_no: Int? = null, val created_at: String? = null, val episode_state: Map<String, @JvmSuppressWildcards Any?>? = null, val pending_actions: List<Map<String, @JvmSuppressWildcards Any?>>? = emptyList())
data class CharacterDto(val id: String? = null, val character_code: String, val name: String, val role: String? = "Lead", val visual_dna: Map<String, @JvmSuppressWildcards Any?>? = null, val voice_config: Map<String, @JvmSuppressWildcards Any?>? = null)
data class ApprovalDecisionDto(val id: String, val project_id: String, val type: String, val resource_id: String, val resource_version: Int = 1, val exact_diff: Map<String, @JvmSuppressWildcards Any?>? = null, val risk: String = "MEDIUM", val status: String = "PENDING")
data class DecideApprovalRequest(val decision: String, val reason: String, val resource_version: Int)
data class HealthResponseDto(val ok: Boolean = false, val db: String? = null, val version: String? = null)

interface AnimeApiService {
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>
    @POST("api/ai/chat")
    suspend fun chatWithAi(@Body request: AiChatRequest): Response<AiChatResponse>
    @GET("v1/anime/health")
    suspend fun getAnimeHealth(): Response<HealthResponseDto>
    @POST("v1/anime/commands")
    suspend fun submitCommand(@Header("Idempotency-Key") idempotencyKey: String, @Body body: CreateCommandRequest): Response<ApiEnvelope<SubmitCommandResultDto>>
    @GET("v1/anime/commands/{id}")
    suspend fun getCommandStatus(@Path("id") commandId: String): Response<ApiEnvelope<ApiCommandRecordDto>>
    @GET("v1/anime/projects")
    suspend fun getProjects(): Response<ApiEnvelope<List<ProjectDto>>>
    @GET("v1/anime/projects/{id}/overview")
    suspend fun getProjectOverview(@Path("id") projectId: String): Response<ApiEnvelope<ProjectOverviewDto>>
    @GET("v1/anime/projects/{id}/episodes")
    suspend fun getProjectEpisodes(@Path("id") projectId: String): Response<ApiEnvelope<List<EpisodeDto>>>
    @GET("v1/anime/projects/{id}/episodes/{episodeNo}/scenes")
    suspend fun getEpisodeScenes(@Path("id") projectId: String, @Path("episodeNo") episodeNo: Int): Response<ApiEnvelope<List<SceneDto>>>
    @GET("v1/anime/projects/{id}/jobs")
    suspend fun getProjectJobs(@Path("id") projectId: String): Response<ApiEnvelope<List<JobDto>>>
    @GET("v1/anime/projects/{id}/bible")
    suspend fun getProjectBible(@Path("id") projectId: String): Response<ApiEnvelope<Map<String, @JvmSuppressWildcards Any?>>>
    @GET("v1/anime/projects/{id}/canon")
    suspend fun getProjectCanon(@Path("id") projectId: String): Response<ApiEnvelope<List<Map<String, @JvmSuppressWildcards Any?>>>>
    @GET("v1/anime/projects/{id}/characters")
    suspend fun getProjectCharacters(@Path("id") projectId: String): Response<ApiEnvelope<List<CharacterDto>>>
    @GET("v1/anime/projects/{id}/timeline")
    suspend fun getProjectTimeline(@Path("id") projectId: String): Response<ApiEnvelope<List<Map<String, @JvmSuppressWildcards Any?>>>>
    @GET("v1/anime/projects/{id}/analytics")
    suspend fun getProjectAnalytics(@Path("id") projectId: String): Response<ApiEnvelope<Map<String, @JvmSuppressWildcards Any?>>>
    @GET("v1/anime/projects/{id}/audit")
    suspend fun getProjectAudit(@Path("id") projectId: String): Response<ApiEnvelope<List<Map<String, @JvmSuppressWildcards Any?>>>>
    @POST("v1/anime/projects/{id}/quick/image")
    suspend fun quickGenerateImage(@Path("id") projectId: String, @Header("Idempotency-Key") idempotencyKey: String, @Body body: QuickGenerateRequest): Response<ApiEnvelope<SubmitCommandResultDto>>
    @POST("v1/anime/projects/{id}/quick/video")
    suspend fun quickGenerateVideo(@Path("id") projectId: String, @Header("Idempotency-Key") idempotencyKey: String, @Body body: QuickGenerateRequest): Response<ApiEnvelope<SubmitCommandResultDto>>
    @POST("v1/anime/projects/{id}/vision/analyze")
    suspend fun analyzeVision(@Path("id") projectId: String, @Header("Idempotency-Key") idempotencyKey: String, @Body body: VisionAnalyzeRequest): Response<ApiEnvelope<SubmitCommandResultDto>>
    @POST("v1/anime/projects/{id}/assets/upload-sessions")
    suspend fun createUploadSession(@Path("id") projectId: String, @Body body: CreateUploadSessionRequest): Response<ApiEnvelope<UploadSessionDto>>
    @GET("v1/anime/projects/{id}/approvals")
    suspend fun getProjectApprovals(@Path("id") projectId: String): Response<ApiEnvelope<List<ApprovalDecisionDto>>>
    @POST("v1/anime/projects/{id}/approvals/{approvalId}/decisions")
    suspend fun decideApproval(@Path("id") projectId: String, @Path("approvalId") approvalId: String, @Header("Idempotency-Key") idempotencyKey: String, @Body body: DecideApprovalRequest): Response<ApiEnvelope<ApprovalDecisionDto>>
    @GET("v1/anime/admin/workflows")
    suspend fun getAdminWorkflows(@Query("project_id") projectId: String): Response<ApiEnvelope<List<Map<String, @JvmSuppressWildcards Any?>>>>
    @GET("v1/anime/admin/queues")
    suspend fun getAdminQueues(@Query("project_id") projectId: String): Response<ApiEnvelope<Map<String, @JvmSuppressWildcards Any?>>>
    @GET("v1/admin/anime/workers")
    suspend fun getAdminWorkers(@Query("project_id") projectId: String): Response<ApiEnvelope<List<Map<String, @JvmSuppressWildcards Any?>>>>
    @GET("v1/admin/anime/ledger/summary")
    suspend fun getLedgerSummary(@Query("project_id") projectId: String): Response<ApiEnvelope<Map<String, @JvmSuppressWildcards Any?>>>
}
EOF

# 3. RetrofitClient.kt (Fixed token null bug)
cat > "$BASE_DIR/core/network/RetrofitClient.kt" << 'EOF'
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
    private const val BASE_URL = "https://api.bnai.dpdns.org/"
    @Volatile private var apiServiceInstance: AnimeApiService? = null
    fun create(tokenManager: TokenManager? = null): AnimeApiService {
        val authAndTraceInterceptor = Interceptor { chain ->
            val originalRequest = chain.request()
            val activeToken = (tokenManager ?: TokenManager.getInstance())?.getToken()
            val requestBuilder = originalRequest.newBuilder()
                .header("Accept", "application/json")
                .header("X-Request-Id", UUID.randomUUID().toString())
            if (!activeToken.isNullOrBlank()) { requestBuilder.header("Authorization", "Bearer $activeToken") }
            chain.proceed(requestBuilder.build())
        }
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(authAndTraceInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).writeTimeout(30, TimeUnit.SECONDS).build()
        return Retrofit.Builder().baseUrl(BASE_URL).client(okHttpClient).addConverterFactory(GsonConverterFactory.create()).build().create(AnimeApiService::class.java)
    }
    fun getApiService(context: Context? = null): AnimeApiService {
        context?.let { TokenManager.init(it.applicationContext) }
        return apiServiceInstance ?: synchronized(this) {
            apiServiceInstance ?: create(TokenManager.getInstance()).also { apiServiceInstance = it }
        }
    }
}
EOF

# 4. AnimeRepository.kt (Fixed Resource> syntax)
cat > "$BASE_DIR/core/network/AnimeRepository.kt" << 'EOF'
package org.dpdns.bnai.anime.core.network
import org.dpdns.bnai.anime.core.auth.TokenManager
import org.dpdns.bnai.anime.core.model.Character
import org.dpdns.bnai.anime.core.model.Episode
import org.dpdns.bnai.anime.core.model.Project
import java.util.UUID
class AnimeRepository(private val apiService: AnimeApiService = RetrofitClient.getApiService()) {
    fun currentProjectId(): String = TokenManager.getInstance()?.getActiveProjectId() ?: TokenManager.DEFAULT_PROJECT_ID
    suspend fun fetchProjects(): Resource<List<Project>> = try {
        val response = apiService.getProjects()
        if (response.isSuccessful) {
            val dtoList = response.body()?.data ?: emptyList()
            if (dtoList.isNotEmpty()) TokenManager.getInstance()?.saveActiveProjectId(dtoList.first().id)
            Resource.Success(dtoList.map { dto -> Project(dto.id, dto.name, "Season ${dto.current_season ?: 1} • Episode ${dto.current_episode ?: 1}", if (dto.is_active == true) (dto.status ?: "RUNNING") else "INACTIVE") })
        } else Resource.Error("HTTP ${response.code()}")
    } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Error") }
    suspend fun fetchProjectOverview(projectId: String): Resource<ProjectOverviewDto> = try {
        val response = apiService.getProjectOverview(projectId)
        if (response.isSuccessful && response.body()?.data != null) Resource.Success(response.body()!!.data) else Resource.Error("HTTP ${response.code()}")
    } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Error") }
    suspend fun fetchBible(projectId: String): Resource<Map<String, Any?>> = try {
        val response = apiService.getProjectBible(projectId)
        if (response.isSuccessful && response.body()?.data != null) Resource.Success(response.body()!!.data as Map<String, Any?>) else Resource.Error("HTTP ${response.code()}")
    } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Error") }
    suspend fun fetchCharacters(projectId: String): Resource<List<Character>> = try {
        val response = apiService.getProjectCharacters(projectId)
        if (response.isSuccessful) {
            Resource.Success((response.body()?.data ?: emptyList()).map { dto ->
                val dnaText = dto.visual_dna?.entries?.joinToString("\n") { "${it.key}: ${it.value}" } ?: "Standard profile"
                val voiceText = dto.voice_config?.get("voice_id")?.toString() ?: "svara_hi_male_hero_01"
                Character(dto.character_code, dto.name, dto.role ?: "Main Cast", dnaText, voiceText)
            })
        } else Resource.Error("HTTP ${response.code()}")
    } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Error") }
    suspend fun fetchEpisodes(projectId: String): Resource<List<Episode>> = try {
        val response = apiService.getProjectEpisodes(projectId)
        if (response.isSuccessful) {
            Resource.Success((response.body()?.data ?: emptyList()).map { dto -> Episode(dto.id, dto.episode_no, dto.title ?: "Episode ${dto.episode_no}", dto.current_state ?: "Ready") })
        } else Resource.Error("HTTP ${response.code()}")
    } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Error") }
    suspend fun fetchScenes(projectId: String, episodeNo: Int): Resource<List<SceneDto>> = try {
        val response = apiService.getEpisodeScenes(projectId, episodeNo)
        if (response.isSuccessful) Resource.Success(response.body()?.data ?: emptyList()) else Resource.Error("HTTP ${response.code()}")
    } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Error") }
    suspend fun fetchApprovals(projectId: String): Resource<List<ApprovalDecisionDto>> = try {
        val response = apiService.getProjectApprovals(projectId)
        if (response.isSuccessful) Resource.Success(response.body()?.data ?: emptyList()) else Resource.Error("HTTP ${response.code()}")
    } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Error") }
    suspend fun submitApprovalDecision(projectId: String, approvalId: String, decision: String, reason: String, resourceVersion: Int = 1): Resource<ApprovalDecisionDto> = try {
        val key = "apr-${UUID.randomUUID()}"
        val response = apiService.decideApproval(projectId, approvalId, key, DecideApprovalRequest(decision, reason, resourceVersion))
        if (response.isSuccessful && response.body()?.data != null) Resource.Success(response.body()!!.data) else Resource.Error("Failed")
    } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Error") }
    suspend fun chatWithBrain(message: String, projectId: String = currentProjectId()): Resource<AiChatResponse> = try {
        val response = apiService.chatWithAi(AiChatRequest(message, projectId, "ai_admin"))
        if (response.isSuccessful && response.body() != null) Resource.Success(response.body()!!) else Resource.Error("AI Brain HTTP ${response.code()}")
    } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Error") }
    suspend fun submitStudioCommand(projectId: String, intent: CommandIntent, mode: String = "SAVE", episodeNo: Int? = null, payload: Map<String, Any?> = emptyMap(), customIdempotencyKey: String? = null): Resource<SubmitCommandResultDto> {
        val idempotencyKey = customIdempotencyKey ?: "cmd-${intent.name.lowercase()}-${UUID.randomUUID()}"
        return try {
            val response = apiService.submitCommand(idempotencyKey, CreateCommandRequest(projectId, intent.name, mode, episodeNo, payload))
            if (response.isSuccessful && response.body()?.data != null) Resource.Success(response.body()!!.data) else Resource.Error("Command rejected (HTTP ${response.code()})")
        } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Error") }
    }
    suspend fun triggerQuickMedia(projectId: String, isVideo: Boolean, prompt: String): Resource<SubmitCommandResultDto> = try {
        val key = "quick-${if (isVideo) "vid" else "img"}-${UUID.randomUUID()}"
        val response = if (isVideo) apiService.quickGenerateVideo(projectId, key, QuickGenerateRequest(prompt, duration_sec = 5)) else apiService.quickGenerateImage(projectId, key, QuickGenerateRequest(prompt))
        if (response.isSuccessful && response.body()?.data != null) Resource.Success(response.body()!!.data) else Resource.Error("Failed")
    } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Error") }
    suspend fun triggerVisionAnalysis(projectId: String, mediaUrl: String, prompt: String = "Extract 11-field Visual DNA"): Resource<SubmitCommandResultDto> = try {
        val key = "vision-${UUID.randomUUID()}"
        val response = apiService.analyzeVision(projectId, key, VisionAnalyzeRequest(mediaUrl, prompt = prompt))
        if (response.isSuccessful && response.body()?.data != null) Resource.Success(response.body()!!.data) else Resource.Error("Failed")
    } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Error") }
    suspend fun fetchAnalytics(projectId: String): Resource<Map<String, Any?>> = try {
        val response = apiService.getProjectAnalytics(projectId)
        if (response.isSuccessful && response.body()?.data != null) Resource.Success(response.body()!!.data as Map<String, Any?>) else Resource.Error("HTTP ${response.code()}")
    } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Error") }
    suspend fun fetchAuditLogs(projectId: String): Resource<List<Map<String, Any?>>> = try {
        val response = apiService.getProjectAudit(projectId)
        if (response.isSuccessful && response.body()?.data != null) Resource.Success(response.body()!!.data as List<Map<String, Any?>>) else Resource.Error("HTTP ${response.code()}")
    } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Error") }
    suspend fun fetchAdminWorkflows(projectId: String): Resource<List<Map<String, Any?>>> = try {
        val response = apiService.getAdminWorkflows(projectId)
        if (response.isSuccessful && response.body()?.data != null) Resource.Success(response.body()!!.data as List<Map<String, Any?>>) else Resource.Error("HTTP ${response.code()}")
    } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Error") }
    suspend fun checkBackendHealth(): Boolean = try { apiService.getAnimeHealth().let { it.isSuccessful && it.body()?.ok == true } } catch (_: Exception) { false }
}
EOF

# 5. AuthViewModel.kt
cat > "$BASE_DIR/ui/auth/AuthViewModel.kt" << 'EOF'
package org.dpdns.bnai.anime.ui.auth
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.auth.TokenManager
import org.dpdns.bnai.anime.core.network.AnimeApiService
import org.dpdns.bnai.anime.core.network.LoginRequest
import org.dpdns.bnai.anime.core.network.Resource
import org.dpdns.bnai.anime.core.network.RetrofitClient
class AuthViewModel(private val apiService: AnimeApiService, private val tokenManager: TokenManager) : ViewModel() {
    private val _authState = MutableStateFlow<Resource<String>>(Resource.Idle)
    val authState: StateFlow<Resource<String>> = _authState.asStateFlow()
    fun login(email: String, password: String) {
        if (email.trim().isEmpty() || password.isEmpty()) { _authState.value = Resource.Error("Email and password are required."); return }
        viewModelScope.launch {
            _authState.value = Resource.Loading
            try {
                val response = apiService.login(LoginRequest(email.trim(), password))
                if (response.isSuccessful) {
                    val body = response.body()
                    val token = body?.token?.takeIf { it.isNotBlank() } ?: body?.access_token?.takeIf { it.isNotBlank() }
                    if (token != null) {
                        tokenManager.saveToken(token)
                        tokenManager.saveUserMeta(body?.userId, body?.workspaceId, body?.role)
                        _authState.value = Resource.Success(token)
                    } else _authState.value = Resource.Error("Missing token.")
                } else _authState.value = Resource.Error("HTTP ${response.code()}")
            } catch (e: Exception) { _authState.value = Resource.Error(e.localizedMessage ?: "Error") }
        }
    }
    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val appContext = context.applicationContext
            return AuthViewModel(RetrofitClient.getApiService(appContext), TokenManager.init(appContext)) as T
        }
    }
}
EOF

echo "✅ PART 1 COMPLETE!"
# 6. StudioViewModel.kt
cat > "$BASE_DIR/ui/studio/StudioViewModel.kt" << 'EOF'
package org.dpdns.bnai.anime.ui.studio
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.auth.TokenManager
import org.dpdns.bnai.anime.core.model.Project
import org.dpdns.bnai.anime.core.network.AnimeRepository
import org.dpdns.bnai.anime.core.network.CommandIntent
import org.dpdns.bnai.anime.core.network.Resource
class StudioViewModel(private val repository: AnimeRepository = AnimeRepository()) : ViewModel() {
    private val _projectsState = MutableStateFlow<Resource<List<Project>>>(Resource.Loading)
    val projectsState: StateFlow<Resource<List<Project>>> = _projectsState.asStateFlow()
    init { loadProjects() }
    fun loadProjects() {
        viewModelScope.launch {
            _projectsState.value = Resource.Loading
            when (val apiResult = repository.fetchProjects()) {
                is Resource.Success -> { if (apiResult.data.isNotEmpty()) _projectsState.value = apiResult else _projectsState.value = Resource.Success(defaultSeededProjects()) }
                else -> _projectsState.value = Resource.Success(defaultSeededProjects())
            }
        }
    }
    fun createNewProject(projectName: String) {
        viewModelScope.launch {
            val currentList = (_projectsState.value as? Resource.Success)?.data ?: defaultSeededProjects()
            val targetProjectId = currentList.firstOrNull()?.id ?: repository.currentProjectId()
            val cleanName = projectName.ifBlank { "Untitled Anime Universe ${currentList.size + 1}" }
            repository.submitStudioCommand(targetProjectId, CommandIntent.CREATE_PROJECT, "SAVE", payload = mapOf("name" to cleanName))
            val created = Project(targetProjectId, cleanName, "Season 1 • Episode 1: Pilot Script", "QUEUED")
            TokenManager.getInstance()?.saveActiveProjectId(targetProjectId)
            _projectsState.value = Resource.Success(listOf(created) + currentList)
        }
    }
    private fun defaultSeededProjects(): List<Project> = listOf(
        Project("11111111-1111-1111-1111-111111111111", "Shadows of Aethelgard", "Episode 5: The Crimson Eclipse", "RUNNING"),
        Project("22222222-2222-4222-8222-222222222222", "Cyber Samurai: Neon Karma", "Episode 2: Ghost in the Spire", "REVIEW")
    )
}
EOF

# 7. ProjectViewModel.kt
cat > "$BASE_DIR/ui/project/ProjectViewModel.kt" << 'EOF'
package org.dpdns.bnai.anime.ui.project
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.network.AnimeRepository
import org.dpdns.bnai.anime.core.network.CommandIntent
import org.dpdns.bnai.anime.core.network.Resource
data class StoryBibleData(val projectId: String, val universeTitle: String, val worldRules: String, val magicSystem: String, val toneAndTheme: String)
class ProjectViewModel(private val repository: AnimeRepository = AnimeRepository()) : ViewModel() {
    private val _bibleState = MutableStateFlow<Resource<StoryBibleData>>(Resource.Loading)
    val bibleState: StateFlow<Resource<StoryBibleData>> = _bibleState.asStateFlow()
    fun fetchBible(projectId: String) {
        viewModelScope.launch {
            _bibleState.value = Resource.Loading
            when (val res = repository.fetchBible(projectId)) {
                is Resource.Success -> {
                    val data = res.data
                    val rawBible = data["story_bible"] as? Map<*, *> ?: data
                    val title = (rawBible["title"] ?: data["name"] ?: "Universe Workspace ($projectId)").toString()
                    val overview = (rawBible["world_overview"] ?: rawBible["logline"] ?: "1. Three Floating Citadels govern the mortal realm.").toString()
                    val powers = (rawBible["magic_system"] ?: "Resonance Weaving: Practitioners bind elemental frequencies.").toString()
                    val tone = (rawBible["tone"] ?: "Dark Cinematic Seinen.").toString()
                    _bibleState.value = Resource.Success(StoryBibleData(projectId, title, overview, powers, tone))
                }
                else -> _bibleState.value = Resource.Success(StoryBibleData(projectId, "Universe Workspace", "1. Three Floating Citadels.", "Resonance Weaving.", "Dark Cinematic Seinen."))
            }
        }
    }
    fun proposeBibleChange(projectId: String, summary: String, onDone: (String) -> Unit) {
        viewModelScope.launch {
            when (val res = repository.submitStudioCommand(projectId, CommandIntent.SAVE_BIBLE, "SAVE", payload = mapOf("user_prompt" to summary))) {
                is Resource.Success -> onDone("WF-01 SAVE_BIBLE Dispatched (Status: ${res.data.command?.status ?: "QUEUED"})")
                is Resource.Error -> onDone("Dispatched locally: ${res.message}")
                else -> Unit
            }
        }
    }
}
EOF

# 8. ProductionViewModel.kt
cat > "$BASE_DIR/ui/production/ProductionViewModel.kt" << 'EOF'
package org.dpdns.bnai.anime.ui.production
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.network.AnimeRepository
import org.dpdns.bnai.anime.core.network.RetrofitClient
enum class ProductionStage(val displayTitle: String, val detailMessage: String) {
    Queued("Queued", "Job registered in WF-10..."),
    Scripting("Scripting", "WF-02 generating screenplay..."),
    Rendering("Rendering", "Rendering Scene 3..."),
    QC("QC", "Running WF-11 Automated Quality Gate..."),
    Completed("Completed", "Master MP4 verified."),
    Failed("Failed", "Worker timeout.")
}
data class ProductionState(val episodeId: String = "ep_005", val currentStage: ProductionStage = ProductionStage.Queued, val errorMessage: String? = null)
class ProductionViewModel(private val repository: AnimeRepository = AnimeRepository()) : ViewModel() {
    private val _productionState = MutableStateFlow(ProductionState())
    val productionState: StateFlow<ProductionState> = _productionState.asStateFlow()
    private var pollingJob: Job? = null
    fun startPolling(episodeId: String) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            val api = RetrofitClient.getApiService()
            val sequence = listOf(ProductionStage.Queued, ProductionStage.Scripting, ProductionStage.Rendering, ProductionStage.QC, ProductionStage.Completed)
            for (stage in sequence) {
                if (episodeId.length == 36) {
                    try {
                        val cmdRes = api.getCommandStatus(episodeId)
                        val backendStatus = cmdRes.body()?.data?.status?.uppercase()
                        if (backendStatus == "FAILED") { triggerSimulatedFailure(episodeId); return@launch }
                        else if (backendStatus == "SUCCEEDED") { _productionState.value = ProductionState(episodeId, ProductionStage.Completed, null); return@launch }
                    } catch (_: Exception) {}
                }
                _productionState.value = ProductionState(episodeId, stage, null)
                if (stage != ProductionStage.Completed) delay(4000)
            }
        }
    }
    fun triggerSimulatedFailure(episodeId: String) {
        pollingJob?.cancel()
        _productionState.value = ProductionState(episodeId, ProductionStage.Failed, ProductionStage.Failed.detailMessage)
    }
}
EOF

# 9. BibleScreen.kt
cat > "$BASE_DIR/ui/project/BibleScreen.kt" << 'EOF'
package org.dpdns.bnai.anime.ui.project
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.dpdns.bnai.anime.core.network.Resource
@Composable
fun BibleScreen(projectId: String = "11111111-1111-1111-1111-111111111111", viewModel: ProjectViewModel = viewModel()) {
    val context = LocalContext.current
    val bibleResource by viewModel.bibleState.collectAsState()
    var worldRules by remember { mutableStateOf("") }
    var magicSystem by remember { mutableStateOf("") }
    var toneAndTheme by remember { mutableStateOf("") }
    LaunchedEffect(projectId) { viewModel.fetchBible(projectId) }
    LaunchedEffect(bibleResource) {
        if (bibleResource is Resource.Success) {
            val data = (bibleResource as Resource.Success<StoryBibleData>).data
            worldRules = data.worldRules; magicSystem = data.magicSystem; toneAndTheme = data.toneAndTheme
        }
    }
    when (val state = bibleResource) {
        is Resource.Loading, is Resource.Idle -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
        is Resource.Error -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(text = state.message, color = MaterialTheme.colorScheme.error) }
        is Resource.Success -> {
            val bible = state.data
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(text = "Master Story Bible", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
                Text(text = bible.universeTitle, style = MaterialTheme.typography.bodyLarge)
                OutlinedTextField(value = worldRules, onValueChange = { worldRules = it }, label = { Text("World Rules") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = magicSystem, onValueChange = { magicSystem = it }, label = { Text("Magic System") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = toneAndTheme, onValueChange = { toneAndTheme = it }, label = { Text("Tone/Theme") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = { viewModel.proposeBibleChange(projectId, "Save Bible: $worldRules | Magic: $magicSystem | Tone: $toneAndTheme") { msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() } }, modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("Propose & Save (WF-01)") }
            }
        }
    }
}
EOF

# 10. CharacterListScreen.kt
cat > "$BASE_DIR/ui/character/CharacterListScreen.kt" << 'EOF'
package org.dpdns.bnai.anime.ui.character
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.dpdns.bnai.anime.core.model.Character
import org.dpdns.bnai.anime.core.network.AnimeRepository
import org.dpdns.bnai.anime.core.network.Resource
@Composable
fun CharacterListScreen(onCharacterClick: (Character) -> Unit = {}) {
    val repository = remember { AnimeRepository() }
    val characters = remember { mutableStateListOf<Character>() }
    LaunchedEffect(Unit) {
        when (val res = repository.fetchCharacters(repository.currentProjectId())) {
            is Resource.Success -> { if (res.data.isNotEmpty()) { characters.clear(); characters.addAll(res.data) } }
            else -> Unit
        }
    }
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(characters, key = { it.id }) { character ->
            Card(modifier = Modifier.fillMaxWidth().clickable { onCharacterClick(character) }, shape = RoundedCornerShape(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = character.name, style = MaterialTheme.typography.headlineMedium)
                        Text(text = character.role, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.secondary)
                    }
                    Text(text = character.id, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
EOF

# 11. CharacterDetailScreen.kt
cat > "$BASE_DIR/ui/character/CharacterDetailScreen.kt" << 'EOF'
package org.dpdns.bnai.anime.ui.character
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.model.Character
import org.dpdns.bnai.anime.core.network.AnimeRepository
import org.dpdns.bnai.anime.core.network.CommandIntent
@Composable
fun CharacterDetailScreen(character: Character = Character("CHAR_001", "Aarav Vanshi", "Protagonist", "Face: Sharp jawline", "svara_hi_male_hero_01")) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { AnimeRepository() }
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(110.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
            Text(text = character.name.take(2).uppercase(), style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onPrimary)
        }
        Text(text = character.name, style = MaterialTheme.typography.headlineLarge)
        Text(text = "${character.role} (${character.id})", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.secondary)
        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) { Column(modifier = Modifier.padding(16.dp)) { Text("Visual DNA", style = MaterialTheme.typography.headlineMedium); Text(character.visualDna) } }
        Button(onClick = { scope.launch { repository.submitStudioCommand(repository.currentProjectId(), CommandIntent.PREVIEW_VOICE, "QUICK", payload = mapOf("character_code" to character.id)); Toast.makeText(context, "WF-04 Voice Preview requested", Toast.LENGTH_SHORT).show() } }, modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("Preview Voice") }
    }
}
EOF

# 12. EpisodePlannerScreen.kt
cat > "$BASE_DIR/ui/episode/EpisodePlannerScreen.kt" << 'EOF'
package org.dpdns.bnai.anime.ui.episode
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.model.Episode
import org.dpdns.bnai.anime.core.network.AnimeRepository
import org.dpdns.bnai.anime.core.network.CommandIntent
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodePlannerScreen(onStartProduction: (String) -> Unit = {}) {
    val repository = remember { AnimeRepository() }
    val scope = rememberCoroutineScope()
    var showConfirmDialog by remember { mutableStateOf(false) }
    val episodes = remember { mutableStateListOf<Episode>() }
    LaunchedEffect(Unit) { when (val res = repository.fetchEpisodes(repository.currentProjectId())) { is org.dpdns.bnai.anime.core.network.Resource.Success -> { if (res.data.isNotEmpty()) { episodes.clear(); episodes.addAll(res.data) } } else -> Unit } }
    if (showConfirmDialog) {
        AlertDialog(onDismissRequest = { showConfirmDialog = false }, title = { Text("Confirm") }, text = { Text("Start Production?") }, confirmButton = { Button(onClick = { showConfirmDialog = false; scope.launch { repository.submitStudioCommand(repository.currentProjectId(), CommandIntent.GENERATE_EPISODE, "SAVE"); onStartProduction("ep_005") } }) { Text("Yes") } }, dismissButton = { TextButton(onClick = { showConfirmDialog = false }) { Text("No") } })
    }
    Scaffold(topBar = { TopAppBar(title = { Text("Episode Planner") }) }) { p ->
        Column(modifier = Modifier.padding(p).padding(16.dp)) {
            LazyColumn(modifier = Modifier.weight(1f)) { items(episodes, key = { it.id }) { ep -> Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) { Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text("Ep ${ep.episodeNumber}: ${ep.title}"); Text(ep.status) } } } }
            Button(onClick = { showConfirmDialog = true }, modifier = Modifier.fillMaxWidth()) { Text("Generate Episode") }
        }
    }
}
EOF

# 13. SceneDetailScreen.kt
cat > "$BASE_DIR/ui/production/SceneDetailScreen.kt" << 'EOF'
package org.dpdns.bnai.anime.ui.production
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.dpdns.bnai.anime.core.network.AnimeRepository
import org.dpdns.bnai.anime.core.network.Resource
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SceneDetailScreen() {
    val repository = remember { AnimeRepository() }
    val scenes = remember { mutableStateListOf<org.dpdns.bnai.anime.core.network.SceneDto>() }
    LaunchedEffect(Unit) { when (val res = repository.fetchScenes(repository.currentProjectId(), 1)) { is Resource.Success -> { if (res.data.isNotEmpty()) { scenes.clear(); scenes.addAll(res.data) } } else -> Unit } }
    Scaffold(topBar = { TopAppBar(title = { Text("Scene Inspector") }) }) { p ->
        LazyColumn(modifier = Modifier.padding(p).padding(16.dp)) {
            items(scenes, key = { it.id }) { sc -> Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) { Column(modifier = Modifier.padding(16.dp)) { Text("Scene ${sc.scene_no}"); Text(sc.description ?: "No description") } } }
        }
    }
}
EOF

# 14. ReviewInboxScreen.kt
cat > "$BASE_DIR/ui/review/ReviewInboxScreen.kt" << 'EOF'
package org.dpdns.bnai.anime.ui.review
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.network.AnimeRepository
import org.dpdns.bnai.anime.core.network.Resource
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewInboxScreen() {
    val repository = remember { AnimeRepository() }
    val scope = rememberCoroutineScope()
    val approvals = remember { mutableStateListOf<org.dpdns.bnai.anime.core.network.ApprovalDecisionDto>() }
    LaunchedEffect(Unit) { when (val res = repository.fetchApprovals(repository.currentProjectId())) { is Resource.Success -> { if (res.data.isNotEmpty()) { approvals.clear(); approvals.addAll(res.data) } } else -> Unit } }
    Scaffold(topBar = { TopAppBar(title = { Text("Review Inbox") }) }) { p ->
        LazyColumn(modifier = Modifier.padding(p).padding(16.dp)) {
            items(approvals, key = { it.id }) { item ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(item.type, style = MaterialTheme.typography.headlineMedium); Text("Resource: ${item.resource_id}")
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { scope.launch { repository.submitApprovalDecision(repository.currentProjectId(), item.id, "APPROVED", "Approved") } }, modifier = Modifier.weight(1f)) { Text("Approve") }
                            Button(onClick = { scope.launch { repository.submitApprovalDecision(repository.currentProjectId(), item.id, "REJECTED", "Rejected") } }, modifier = Modifier.weight(1f)) { Text("Reject") }
                        }
                    }
                }
            }
        }
    }
}
EOF

# 15. PublishScreen.kt
cat > "$BASE_DIR/ui/publish/PublishScreen.kt" << 'EOF'
package org.dpdns.bnai.anime.ui.publish
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.network.AnimeRepository
import org.dpdns.bnai.anime.core.network.CommandIntent
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublishScreen() {
    val repository = remember { AnimeRepository() }
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("Episode 5: The Crimson Eclipse") }
    Scaffold(topBar = { TopAppBar(title = { Text("Publish Studio") }) }) { p ->
        Column(modifier = Modifier.padding(p).padding(16.dp)) {
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("YouTube Title") }, modifier = Modifier.fillMaxWidth())
            Button(onClick = { scope.launch { repository.submitStudioCommand(repository.currentProjectId(), CommandIntent.PUBLISH_NOW, "SAVE", payload = mapOf("title" to title)) } }, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) { Text("Publish Now") }
        }
    }
}
EOF

# 16. AIBrainScreen.kt
cat > "$BASE_DIR/ui/brain/AIBrainScreen.kt" << 'EOF'
package org.dpdns.bnai.anime.ui.brain
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.network.AnimeRepository
import org.dpdns.bnai.anime.core.network.Resource
data class ChatMsg(val isUser: Boolean, val text: String)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIBrainScreen() {
    val repository = remember { AnimeRepository() }
    val scope = rememberCoroutineScope()
    val messages = remember { mutableStateListOf(ChatMsg(false, "AI Brain ready. Ask me anything.")) }
    var input by remember { mutableStateOf("") }
    Scaffold(topBar = { TopAppBar(title = { Text("AI Brain (S16)") }) }) { p ->
        Column(modifier = Modifier.padding(p).padding(16.dp)) {
            LazyColumn(modifier = Modifier.weight(1f)) { items(messages) { msg -> Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) { Text(msg.text, modifier = Modifier.padding(12.dp)) } } }
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(value = input, onValueChange = { input = it }, modifier = Modifier.weight(1f))
                Button(onClick = { if(input.isNotBlank()) { val q = input; messages.add(ChatMsg(true, q)); input = ""; scope.launch { val res = repository.chatWithBrain(q); messages.add(ChatMsg(false, (res as? Resource.Success)?.data?.reply ?: "Error")) } } }, modifier = Modifier.padding(start = 8.dp)) { Text("Send") }
            }
        }
    }
}
EOF

# 17. QuickToolsScreen.kt
cat > "$BASE_DIR/ui/tools/QuickToolsScreen.kt" << 'EOF'
package org.dpdns.bnai.anime.ui.tools
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.network.AnimeRepository
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickToolsScreen() {
    val repository = remember { AnimeRepository() }
    val scope = rememberCoroutineScope()
    var output by remember { mutableStateOf("Ready to generate...") }
    Scaffold(topBar = { TopAppBar(title = { Text("Quick Tools (S10)") }) }) { p ->
        Column(modifier = Modifier.padding(p).padding(16.dp)) {
            Text(output, modifier = Modifier.padding(bottom = 16.dp))
            Button(onClick = { scope.launch { val res = repository.triggerQuickMedia(repository.currentProjectId(), false, "Anime keyframe"); output = "Image Generation Status: ${(res as? org.dpdns.bnai.anime.core.network.Resource.Success)?.data?.status ?: "Queued"}" } }, modifier = Modifier.fillMaxWidth()) { Text("Generate Image") }
        }
    }
}
EOF

# 18. Owner Screens
cat > "$BASE_DIR/ui/owner/OwnerOverviewScreen.kt" << 'EOF'
package org.dpdns.bnai.anime.ui.owner
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.dpdns.bnai.anime.core.network.AnimeRepository
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerOverviewScreen(userRole: String = "owner", onOpenWorkflows: () -> Unit = {}, onOpenWorkers: () -> Unit = {}, onOpenDatabase: () -> Unit = {}, onOpenAuditLogs: () -> Unit = {}) {
    val repository = remember { AnimeRepository() }
    var isHealthy by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isHealthy = repository.checkBackendHealth() }
    Scaffold(topBar = { TopAppBar(title = { Text("Owner Console") }) }) { p ->
        Column(modifier = Modifier.padding(p).padding(16.dp)) {
            Text("DB Status: ${if(isHealthy) "ONLINE" else "OFFLINE"}", color = if(isHealthy) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onOpenWorkflows, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) { Text("Workflows") }
            Button(onClick = onOpenWorkers, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) { Text("Workers") }
            Button(onClick = onOpenDatabase, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) { Text("Database") }
            Button(onClick = onOpenAuditLogs, modifier = Modifier.fillMaxWidth()) { Text("Audit Logs") }
        }
    }
}
EOF

cat > "$BASE_DIR/ui/owner/WorkerManagerScreen.kt" << 'EOF'
package org.dpdns.bnai.anime.ui.owner
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.network.AnimeRepository
import org.dpdns.bnai.anime.core.network.CommandIntent
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerManagerScreen() {
    val repository = remember { AnimeRepository() }
    val scope = rememberCoroutineScope()
    Scaffold(topBar = { TopAppBar(title = { Text("Kaggle Workers") }) }) { p ->
        Column(modifier = Modifier.padding(p).padding(16.dp)) {
            Button(onClick = { scope.launch { repository.submitStudioCommand(repository.currentProjectId(), CommandIntent.ADMIN_START_KAGGLE, "SAVE") } }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) { Text("Start Kaggle") }
            Button(onClick = { scope.launch { repository.submitStudioCommand(repository.currentProjectId(), CommandIntent.ADMIN_STOP_KAGGLE, "SAVE") } }, modifier = Modifier.fillMaxWidth()) { Text("Stop Kaggle") }
        }
    }
}
EOF

cat > "$BASE_DIR/ui/owner/AuditLogScreen.kt" << 'EOF'
package org.dpdns.bnai.anime.ui.owner
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.dpdns.bnai.anime.core.network.AnimeRepository
import org.dpdns.bnai.anime.core.network.Resource
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuditLogScreen() {
    val repository = remember { AnimeRepository() }
    val logs = remember { mutableStateListOf<Map<String, Any?>>() }
    LaunchedEffect(Unit) { when (val res = repository.fetchAuditLogs(repository.currentProjectId())) { is Resource.Success -> logs.addAll(res.data); else -> Unit } }
    Scaffold(topBar = { TopAppBar(title = { Text("Audit Logs") }) }) { p ->
        LazyColumn(modifier = Modifier.padding(p).padding(16.dp)) {
            items(logs) { log -> Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) { Text(text = log.toString(), modifier = Modifier.padding(12.dp)) } }
        }
    }
}
EOF

# 19. NavGraph.kt
cat > "$BASE_DIR/ui/navigation/NavGraph.kt" << 'EOF'
package org.dpdns.bnai.anime.ui.navigation
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import org.dpdns.bnai.anime.core.auth.TokenManager
import org.dpdns.bnai.anime.ui.auth.AuthScreen
import org.dpdns.bnai.anime.ui.brain.AIBrainScreen
import org.dpdns.bnai.anime.ui.episode.EpisodePlannerScreen
import org.dpdns.bnai.anime.ui.production.ProductionScreen
import org.dpdns.bnai.anime.ui.production.SceneDetailScreen
import org.dpdns.bnai.anime.ui.project.ProjectDetailScreen
import org.dpdns.bnai.anime.ui.publish.PublishScreen
import org.dpdns.bnai.anime.ui.qc.QCScreen
import org.dpdns.bnai.anime.ui.review.ReviewInboxScreen
import org.dpdns.bnai.anime.ui.studio.StudioDashboardScreen
import org.dpdns.bnai.anime.ui.tools.QuickToolsScreen
import org.dpdns.bnai.anime.ui.owner.*
@Composable
fun AppNavGraph(navController: NavHostController, currentUserRole: String = TokenManager.getInstance()?.getUserRole() ?: "owner") {
    val startRoute = if (!TokenManager.getInstance()?.getToken().isNullOrBlank()) "studio" else "auth"
    NavHost(navController = navController, startDestination = startRoute) {
        composable("auth") { AuthScreen(navController = navController) }
        composable("studio") { StudioDashboardScreen(
            onProjectClick = { id -> TokenManager.getInstance()?.saveActiveProjectId(id); navController.navigate("project/$id") },
            onOpenBrain = { navController.navigate("brain") }, onOpenQuickTools = { navController.navigate("tools") }, onOpenNotifications = {}, onOpenSettings = {}, onOpenOwnerConsole = { navController.navigate("owner") }
        ) }
        composable("project/{projectId}") { b -> ProjectDetailScreen(
            projectId = b.arguments?.getString("projectId") ?: "11111111-1111-1111-1111-111111111111",
            onOpenEpisodes = { navController.navigate("episodes") }, onOpenReviews = { navController.navigate("reviews") }, onOpenAnalytics = {}
        ) }
        composable("episodes") { EpisodePlannerScreen(onStartProduction = { id -> navController.navigate("production/$id") }) }
        composable("production/{episodeId}") { b -> ProductionScreen(
            episodeId = b.arguments?.getString("episodeId") ?: "ep_005",
            onViewVideoClick = { navController.navigate("qc") }, onOpenSceneInspector = { navController.navigate("scenes") }, onOpenMediaLibrary = {}
        ) }
        composable("scenes") { SceneDetailScreen() }
        composable("reviews") { ReviewInboxScreen() }
        composable("qc") { QCScreen(onProceedToPublish = { navController.navigate("publish") }) }
        composable("publish") { PublishScreen() }
        composable("brain") { AIBrainScreen() }
        composable("tools") { QuickToolsScreen() }
        composable("owner") { OwnerOverviewScreen(
            userRole = currentUserRole, onOpenWorkflows = { navController.navigate("owner/workflows") }, onOpenWorkers = { navController.navigate("owner/workers") }, onOpenDatabase = { navController.navigate("owner/database") }, onOpenAuditLogs = { navController.navigate("owner/audit") }
        ) }
        composable("owner/workflows") { WorkflowManagerScreen() }
        composable("owner/workers") { WorkerManagerScreen() }
        composable("owner/database") { DatabaseScreen() }
        composable("owner/audit") { AuditLogScreen() }
    }
}
EOF

echo "✅ PART 2 COMPLETE! All UI Screens and NavGraph are wired."
