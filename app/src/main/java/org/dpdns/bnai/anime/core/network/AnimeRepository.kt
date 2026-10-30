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
