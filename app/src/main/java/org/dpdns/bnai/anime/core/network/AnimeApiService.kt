package org.dpdns.bnai.anime.core.network

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

typealias JsonMap = Map<String, @JvmSuppressWildcards Any?>

// ============================================================================
// ENVELOPE & AUTH
// ============================================================================
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

data class AuthMeResponse(
    val status: String? = null,
    val user: JsonMap? = null
)

// ============================================================================
// AI BRAIN
// ============================================================================
data class AiChatRequest(
    val message: String,
    val project_id: String? = null,
    val skill_key: String = "ai_admin",
    val request_id: String? = null
)

data class AiChatResponse(
    val request_id: String? = null,
    val status: String? = null,
    val reply: String? = null,
    val message: String? = null,
    val provider: String? = null
)

// ============================================================================
// HEALTH
// ============================================================================
data class HealthResponseDto(
    val ok: Boolean = false,
    val status: String? = null,
    val db: String? = null,
    val database: String? = null,
    val version: String? = null,
    val timestamp: String? = null
)

// ============================================================================
// GAP #1: CommandIntent — EXACT match with backend commands.types.ts (33 values)
// ============================================================================
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

// ============================================================================
// COMMAND DTOs
// ============================================================================
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
    val result: JsonMap? = null,
    val created_at: String? = null,
    val updated_at: String? = null
)

data class SubmitCommandResultDto(
    val status: String? = null,
    val command_id: String? = null,
    val wf_key: String? = null,
    val command: ApiCommandRecordDto? = null
)

// ============================================================================
// GAP #5.2: ProjectDto — no phantom "status" field, use is_active
// ============================================================================
data class ProjectDto(
    val id: String,
    val name: String,
    val is_active: Boolean? = true,
    val story_bible: JsonMap? = null,
    val current_season: Int? = 1,
    val current_episode: Int? = 1
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

// ============================================================================
// EPISODE
// ============================================================================
data class EpisodeDto(
    val id: String,
    val project_id: String? = null,
    val episode_no: Int,
    val title: String? = null,
    val synopsis: String? = null,
    val current_state: String? = null,
    val qc_status: String? = null,
    val updated_at: String? = null
)

// ============================================================================
// SCENE & CLIP
// ============================================================================
data class ClipDto(
    val id: String? = null,
    val scene_order: Int? = null,
    val scene_no: Int? = null,
    val clip_url: String? = null,
    val provider: String? = null,
    val duration_sec: Double? = null,
    val status: String? = null
)

// GAP #5.3: SceneDto — no phantom "title" column (scenes table doesn't have it)
data class SceneDto(
    val id: String,
    val episode_no: Int? = null,
    val scene_order: Int? = null,
    val scene_no: Int? = null,
    val description: String? = null,
    val visual_prompt: String? = null,
    val status: String? = null,
    val clips: List<ClipDto>? = null
)

// ============================================================================
// CHARACTER
// ============================================================================
data class CharacterDto(
    val id: String? = null,
    val character_code: String,
    val name: String,
    val role: String? = null,
    val visual_dna: JsonMap? = null,
    val voice_config: JsonMap? = null
)

// ============================================================================
// GAP #5.4: JobDto — aligned with backend listJobs fields
// ============================================================================
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

// ============================================================================
// GAP #5.1: ApprovalDecisionDto — decision_reason mapping, resource_type computed
// ============================================================================
data class ApprovalDecisionDto(
    val id: String,
    val project_id: String? = null,
    val type: String = "APPROVAL",
    val resource_id: String? = null,
    val resource_version: Int = 1,
    val status: String? = null,
    val risk: String? = null,
    @SerializedName("decision_reason", alternate = ["reason"])
    val reason: String? = null,
    val decision: String? = null,
    val exact_diff: JsonMap? = null,
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

// ============================================================================
// QUICK MEDIA & VISION
// ============================================================================
data class QuickGenerateRequest(
    val prompt: String,
    val style_preset: String? = "anime_cinematic",
    val aspect_ratio: String? = "16:9",
    val duration_sec: Int? = null
)

data class VisionAnalyzeRequest(
    val media_url: String,
    val analysis_type: String = "GENERAL",
    val prompt: String = "Extract 11-field Visual DNA"
)

// ============================================================================
// PROMPT STUDIO
// ============================================================================
data class SystemPromptDto(
    val id: String,
    val project_id: String? = null,
    val title: String,
    val visual_medium: String = "ANIME",
    val prompt_text: String,
    val is_default: Boolean? = false
)

data class CreateSystemPromptRequest(
    val project_id: String? = null,
    val title: String,
    val visual_medium: String,
    val prompt_text: String,
    val set_active: Boolean = true
)

// ============================================================================
// GPU / KAGGLE / TELEGRAM STORAGE CONFIG
// ============================================================================
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

// ============================================================================
// RETROFIT INTERFACE — All endpoints including 13 GAP #2 additions
// ============================================================================
interface AnimeApiService {

    // --- Auth ---
    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("api/v1/auth/me")
    suspend fun getMe(): Response<AuthMeResponse>

    // --- AI Brain ---
    @POST("api/v1/ai/chat")
    suspend fun chatWithAi(@Body request: AiChatRequest): Response<AiChatResponse>

    // --- Health ---
    @GET("v1/anime/health")
    suspend fun getAnimeHealth(): Response<HealthResponseDto>

    // --- Commands (WF-00) ---
    @POST("v1/anime/commands")
    suspend fun submitCommand(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: CreateCommandRequest
    ): Response<ApiEnvelope<SubmitCommandResultDto>>

    @GET("v1/anime/commands/{id}")
    suspend fun getCommandStatus(
        @Path("id") commandId: String
    ): Response<ApiEnvelope<ApiCommandRecordDto>>

    // --- Projects & Story ---
    @GET("v1/anime/projects")
    suspend fun getProjects(): Response<ApiEnvelope<List<ProjectDto>>>

    @GET("v1/anime/projects/{id}/overview")
    suspend fun getProjectOverview(
        @Path("id") projectId: String
    ): Response<ApiEnvelope<ProjectOverviewDto>>

    @GET("v1/anime/projects/{id}/episodes")
    suspend fun getProjectEpisodes(
        @Path("id") projectId: String
    ): Response<ApiEnvelope<List<EpisodeDto>>>

    @GET("v1/anime/projects/{id}/episodes/{episodeNo}")
    suspend fun getEpisodeDetail(
        @Path("id") projectId: String,
        @Path("episodeNo") episodeNo: Int
    ): Response<ApiEnvelope<EpisodeDto>>

    @GET("v1/anime/projects/{id}/episodes/{episodeNo}/plan")
    suspend fun getEpisodePlan(
        @Path("id") projectId: String,
        @Path("episodeNo") episodeNo: Int
    ): Response<ApiEnvelope<JsonMap>>

    @GET("v1/anime/projects/{id}/episodes/{episodeNo}/scenes")
    suspend fun getEpisodeScenes(
        @Path("id") projectId: String,
        @Path("episodeNo") episodeNo: Int
    ): Response<ApiEnvelope<List<SceneDto>>>

    @GET("v1/anime/projects/{id}/jobs")
    suspend fun getProjectJobs(
        @Path("id") projectId: String
    ): Response<ApiEnvelope<List<JobDto>>>

    @GET("v1/anime/projects/{id}/jobs/{jobId}")
    suspend fun getJobDetail(
        @Path("id") projectId: String,
        @Path("jobId") jobId: String
    ): Response<ApiEnvelope<JobDto>>

    @GET("v1/anime/projects/{id}/bible")
    suspend fun getProjectBible(
        @Path("id") projectId: String
    ): Response<ApiEnvelope<JsonMap>>

    @GET("v1/anime/projects/{id}/canon")
    suspend fun getProjectCanon(
        @Path("id") projectId: String
    ): Response<ApiEnvelope<List<JsonMap>>>

    @GET("v1/anime/projects/{id}/characters")
    suspend fun getProjectCharacters(
        @Path("id") projectId: String
    ): Response<ApiEnvelope<List<CharacterDto>>>

    @GET("v1/anime/projects/{id}/timeline")
    suspend fun getProjectTimeline(
        @Path("id") projectId: String
    ): Response<ApiEnvelope<List<JsonMap>>>

    @GET("v1/anime/projects/{id}/analytics")
    suspend fun getProjectAnalytics(
        @Path("id") projectId: String
    ): Response<ApiEnvelope<JsonMap>>

    @GET("v1/anime/projects/{id}/audit")
    suspend fun getProjectAudit(
        @Path("id") projectId: String
    ): Response<ApiEnvelope<List<JsonMap>>>

    // --- Quick Tools & Vision (WF-05) ---
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

    // --- Approvals (WF-11) ---
    @GET("v1/anime/projects/{id}/approvals")
    suspend fun getProjectApprovals(
        @Path("id") projectId: String
    ): Response<ApiEnvelope<List<ApprovalDecisionDto>>>

    @POST("v1/anime/projects/{id}/approvals/{approvalId}/decisions")
    suspend fun decideApproval(
        @Path("id") projectId: String,
        @Path("approvalId") approvalId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: DecideApprovalRequest
    ): Response<ApiEnvelope<ApprovalDecisionDto>>

    // --- Admin & Owner ---
    @GET("v1/anime/admin/workflows")
    suspend fun getAdminWorkflows(
        @Query("project_id") projectId: String
    ): Response<ApiEnvelope<List<JsonMap>>>

    @GET("v1/anime/admin/queues")
    suspend fun getAdminQueues(
        @Query("project_id") projectId: String
    ): Response<ApiEnvelope<JsonMap>>

    @GET("v1/admin/anime/workers")
    suspend fun getAdminWorkers(
        @Query("project_id") projectId: String
    ): Response<ApiEnvelope<List<JsonMap>>>

    @GET("v1/admin/anime/ledger/summary")
    suspend fun getLedgerSummary(
        @Query("project_id") projectId: String
    ): Response<ApiEnvelope<JsonMap>>

    // --- Prompts & GPU Config ---
    @GET("v1/anime/prompts")
    suspend fun getSystemPrompts(
        @Query("project_id") projectId: String? = null
    ): Response<ApiEnvelope<List<SystemPromptDto>>>

    @POST("v1/anime/prompts")
    suspend fun createSystemPrompt(
        @Body body: CreateSystemPromptRequest
    ): Response<ApiEnvelope<SystemPromptDto>>

    @GET("v1/anime/infrastructure/gpu")
    suspend fun getGpuConfig(): Response<ApiEnvelope<JsonMap>>

    @POST("v1/anime/infrastructure/gpu")
    suspend fun saveGpuConfig(
        @Body request: GpuSetupRequest
    ): Response<ApiEnvelope<GpuSetupResponseDto>>

    // ========================================================================
    // GAP #2: 13 NEW READ ENDPOINTS (must be deployed on backend first)
    // ========================================================================
    @GET("v1/anime/projects/{id}/quota")
    suspend fun getProjectQuota(
        @Path("id") projectId: String
    ): Response<ApiEnvelope<JsonMap>>

    @GET("v1/anime/projects/{id}/continuity")
    suspend fun getProjectContinuity(
        @Path("id") projectId: String
    ): Response<ApiEnvelope<List<JsonMap>>>

    @GET("v1/anime/projects/{id}/retries")
    suspend fun getProjectRetries(
        @Path("id") projectId: String
    ): Response<ApiEnvelope<List<JsonMap>>>

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
