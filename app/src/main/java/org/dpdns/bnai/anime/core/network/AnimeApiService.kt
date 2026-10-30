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
