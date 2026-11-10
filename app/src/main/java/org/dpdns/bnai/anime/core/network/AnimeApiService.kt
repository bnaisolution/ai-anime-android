package org.dpdns.bnai.anime.core.network

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.*

typealias JsonMap = Map<String, @JvmSuppressWildcards Any?>

data class ApiMeta(val request_id: String? = null)
data class ApiEnvelope<T>(val data: T?, val meta: ApiMeta? = null)

data class LoginRequest(val email: String, val password: String)
data class RefreshTokenRequest(
    @SerializedName("refresh_token", alternate = ["refreshToken"]) val refreshToken: String
)
data class LoginResponse(
    val token: String? = null,
    val access_token: String? = null,
    @SerializedName("refresh_token", alternate = ["refreshToken"]) val refreshToken: String? = null,
    @SerializedName("expires_in", alternate = ["expiresIn"]) val expiresIn: Long? = null,
    @SerializedName("token_type", alternate = ["tokenType"]) val tokenType: String? = null,
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
    val budget_ceiling: Double? = null,
    val active_system_prompt_id: String? = null
)

data class UpdateProjectSettingsRequest(
    val name: String? = null,
    val visual_medium: String? = null,
    val genre: String? = null,
    val auto_approve: Boolean? = null,
    val cover_image_url: String? = null,
    val budget_ceiling: Int? = null,
    val active_system_prompt_id: String? = null
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

data class GenreTemplateDto(
    val id: String,
    val genre: String,
    val title: String,
    val visual_medium: String = "ANIME",
    val format: String = "SERIES",
    val model_hint: String? = null,
    val tags: List<String> = emptyList(),
    val prompt_text: String
)

data class PromptLibraryMetaDto(
    val categories: List<String> = emptyList(),
    val media: List<String> = emptyList(),
    val formats: List<String> = emptyList(),
    val models: List<String> = emptyList(),
    val genres: List<String> = emptyList(),
    val genre_templates: List<GenreTemplateDto> = emptyList()
)

data class MemoryItemDto(
    val id: String? = null,
    val owner_id: String? = null,
    val workspace_id: String? = null,
    val project_id: String? = null,
    val scope: String? = null,
    val content: String = "",
    val category: String? = null,
    val knowledge_status: String? = null,
    val idempotency_key: String? = null,
    val created_at: String? = null
)

data class MemoryReadResultDto(
    val status: String? = null,
    val memory_items: List<MemoryItemDto> = emptyList()
)

data class MemoryWriteResultDto(
    val status: String? = null,
    val memory_item: MemoryItemDto? = null,
    val deduplicated: Boolean = false,
    val errors: List<JsonMap> = emptyList()
)

data class SystemPromptDto(
    val id: String,
    val project_id: String? = null,
    val title: String,
    val visual_medium: String = "ANIME",
    val prompt_category: String? = "BIBLE",
    val genre: String? = null,
    val format: String? = null,
    val model_hint: String? = null,
    val tags: List<String> = emptyList(),
    val is_favorite: Boolean = false,
    val is_archived: Boolean = false,
    val version: Int = 1,
    val prompt_text: String,
    val is_default: Boolean? = false
)

data class CreateSystemPromptRequest(
    val project_id: String? = null,
    val title: String,
    val visual_medium: String,
    val prompt_category: String = "BIBLE",
    val genre: String? = null,
    val format: String? = null,
    val model_hint: String? = null,
    val tags: List<String> = emptyList(),
    val is_favorite: Boolean = false,
    val prompt_text: String,
    val set_active: Boolean = true
)

data class UpdateSystemPromptRequest(
    val title: String? = null,
    val visual_medium: String? = null,
    val prompt_category: String? = null,
    val genre: String? = null,
    val format: String? = null,
    val model_hint: String? = null,
    val tags: List<String>? = null,
    val is_favorite: Boolean? = null,
    val is_archived: Boolean? = null,
    val prompt_text: String? = null
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

    @POST("api/v1/auth/refresh")
    suspend fun refresh(@Body request: RefreshTokenRequest): Response<LoginResponse>

    @POST("api/v1/auth/logout")
    suspend fun logout(): Response<JsonMap>

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

    @POST("api/v1/memory/read")
    suspend fun readMemory(@Body body: JsonMap): Response<MemoryReadResultDto>

    @POST("api/v1/memory/write")
    suspend fun writeMemory(@Body body: JsonMap): Response<MemoryWriteResultDto>

    @POST("api/v1/memory/correct")
    suspend fun correctMemory(@Body body: JsonMap): Response<MemoryWriteResultDto>

    @POST("api/v1/memory/delete")
    suspend fun deleteMemory(@Body body: JsonMap): Response<JsonMap>

    @POST("api/v1/memory/reconcile")
    suspend fun reconcileMemory(@Body body: JsonMap): Response<JsonMap>

    @GET("v1/anime/prompts/library/meta")
    suspend fun getPromptLibraryMeta(): Response<ApiEnvelope<PromptLibraryMetaDto>>

    @GET("v1/anime/prompts")
    suspend fun getSystemPrompts(
        @Query("project_id") projectId: String? = null,
        @Query("prompt_category") promptCategory: String? = null,
        @Query("q") query: String? = null,
        @Query("is_favorite") favoritesOnly: Boolean? = null,
        @Query("genre") genre: String? = null,
        @Query("format") format: String? = null
    ): Response<ApiEnvelope<List<SystemPromptDto>>>

    @POST("v1/anime/prompts/{promptId}/associate")
    suspend fun associatePromptWithProject(
        @Path("promptId") promptId: String,
        @Body body: JsonMap
    ): Response<ApiEnvelope<JsonMap>>

    @POST("v1/anime/prompts")
    suspend fun createSystemPrompt(@Body body: CreateSystemPromptRequest): Response<ApiEnvelope<SystemPromptDto>>

    @PATCH("v1/anime/prompts/{promptId}")
    suspend fun updateSystemPrompt(
        @Path("promptId") promptId: String,
        @Body body: UpdateSystemPromptRequest
    ): Response<ApiEnvelope<SystemPromptDto>>

    @POST("v1/anime/prompts/{promptId}/favorite")
    suspend fun togglePromptFavorite(@Path("promptId") promptId: String): Response<ApiEnvelope<SystemPromptDto>>

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
