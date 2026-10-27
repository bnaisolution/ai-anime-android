package org.dpdns.bnai.anime.core.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

// 1. FASTIFY BACKEND ENVELOPE & AUTH MODELS
data class ApiMeta(val request_id: String? = null)
data class ApiEnvelope<T>(val data: T, val meta: ApiMeta? = null)
data class LoginRequest(val email: String, val password: String)
data class LoginResponse(val token: String, val userId: String, val role: String? = "owner")

// 2. PHASE B1: COMMANDS GATEWAY MODELS (/v1/anime/commands)
enum class CommandIntent {
    CREATE_PROJECT, OPEN_PROJECT, SAVE_BIBLE, BIBLE_CHAT, GENERATE_EPISODE,
    REGENERATE_EPISODE, CONTINUE_EPISODE, QUICK_GENERATE_IMAGE, QUICK_GENERATE_VIDEO,
    SAVE_TO_PROJECT, VISION_ANALYSIS, CHANGE_CHARACTER_VOICE, PREVIEW_VOICE,
    LIST_VOICES, RESET_VOICE, CHECK_CANON, VIEW_CANON, APPROVE_CANON_CHANGE,
    REJECT_CANON_CHANGE, CHECK_CONTINUITY, CHECK_QUOTA, RETRY_FAILED,
    VIEW_STATE, VIEW_AUDIT, RUN_QC, PUBLISH_NOW, VIEW_ANALYTICS,
    ADMIN_START_KAGGLE, ADMIN_STOP_KAGGLE, ADMIN_STATUS, DB_TEST, SYSTEM_HEALTH
}

data class CreateCommandRequest(
    val project_id: String,
    val intent: String,
    val mode: String = "SAVE",
    val episode_no: Int? = null,
    val payload: Map<String, @JvmSuppressWildcards Any?> = emptyMap()
)

data class ApiCommandRecordDto(
    val id: String,
    val project_id: String,
    val user_id: String,
    val intent: String,
    val status: String,
    val idempotency_key: String,
    val created_at: String? = null
)

data class SubmitCommandResultDto(
    val command: ApiCommandRecordDto? = null,
    val command_id: String? = null,
    val project_id: String? = null,
    val status: String? = null,
    val intent: String? = null,
    val wf_key: String? = null,
    val idempotent_replay: Boolean? = false
)

// 3. PHASE B8: CREATIVE READS & MEDIA MODELS
data class QuickGenerateRequest(val prompt: String)
data class VisionAnalyzeRequest(val media_url: String)
data class UploadSessionDto(val upload_url: String, val asset_id: String, val expires_in: Int)
data class ProjectDto(val id: String, val name: String, val current_season: Int? = 1, val current_episode: Int? = 1, val status: String? = "RUNNING")
data class CharacterDto(val id: String? = null, val character_code: String, val name: String, val role: String? = "Lead", val visual_dna: Map<String, @JvmSuppressWildcards Any?>? = null, val voice_config: Map<String, @JvmSuppressWildcards Any?>? = null)

// 4. PHASE B4: APPROVAL DECISIONS MODELS
data class ApprovalDecisionDto(
    val id: String, val project_id: String, val type: String, val resource_id: String,
    val resource_version: Int = 1, val exact_diff: Map<String, @JvmSuppressWildcards Any?>? = null,
    val risk: String = "MEDIUM", val status: String = "PENDING"
)

data class DecideApprovalRequest(val decision: String, val decision_reason: String, val resource_version: Int)

// 5. RETROFIT INTERFACE (STRICTLY FASTIFY BACKEND ROUTES)
interface AnimeApiService {
    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("v1/anime/commands")
    suspend fun submitCommand(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: CreateCommandRequest
    ): Response<ApiEnvelope<SubmitCommandResultDto>>

    @GET("v1/anime/commands/{id}")
    suspend fun getCommandStatus(@Path("id") commandId: String): Response<ApiEnvelope<ApiCommandRecordDto>>

    @GET("v1/anime/projects")
    suspend fun getProjects(): Response<ApiEnvelope<List<ProjectDto>>>

    @GET("v1/anime/projects/{id}/bible")
    suspend fun getProjectBible(@Path("id") projectId: String): Response<ApiEnvelope<Map<String, @JvmSuppressWildcards Any?>>>

    @GET("v1/anime/projects/{id}/canon")
    suspend fun getProjectCanon(@Path("id") projectId: String): Response<ApiEnvelope<List<Map<String, @JvmSuppressWildcards Any?>>>>

    @GET("v1/anime/projects/{id}/characters")
    suspend fun getProjectCharacters(@Path("id") projectId: String): Response<ApiEnvelope<List<CharacterDto>>>

    @GET("v1/anime/projects/{id}/timeline")
    suspend fun getProjectTimeline(@Path("id") projectId: String): Response<ApiEnvelope<List<Map<String, @JvmSuppressWildcards Any?>>>>

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

    @POST("v1/anime/projects/{id}/assets/upload-sessions")
    suspend fun createUploadSession(@Path("id") projectId: String): Response<ApiEnvelope<UploadSessionDto>>

    @GET("v1/anime/projects/{id}/approvals")
    suspend fun getProjectApprovals(@Path("id") projectId: String): Response<ApiEnvelope<List<ApprovalDecisionDto>>>

    @POST("v1/anime/projects/{id}/approvals/{approvalId}/decide")
    suspend fun decideApproval(
        @Path("id") projectId: String,
        @Path("approvalId") approvalId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: DecideApprovalRequest
    ): Response<ApiEnvelope<ApprovalDecisionDto>>
}
