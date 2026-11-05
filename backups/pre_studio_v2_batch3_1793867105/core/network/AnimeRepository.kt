package org.dpdns.bnai.anime.core.network

import org.dpdns.bnai.anime.core.auth.TokenManager
import org.dpdns.bnai.anime.core.model.*
import retrofit2.Response
import java.util.UUID

class AnimeRepository(private val apiService: AnimeApiService = RetrofitClient.getApiService()) {

    fun currentProjectId(): String =
        TokenManager.getInstance()?.getActiveProjectId().orEmpty()

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
                    TokenManager.getInstance()?.saveActiveProjectId(dtoList.first().id)
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

    suspend fun updateBudgetLimit(projectId: String, limit: Int): Resource<JsonMap> = try {
        mapEnvelope(apiService.updateProjectBudget(projectId, UpdateBudgetRequest(limit))) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to update budget limit")
    }

    suspend fun fetchBible(projectId: String): Resource<JsonMap> = try {
        mapEnvelope(apiService.getProjectBible(projectId)) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to load Story Bible")
    }

    // Fix #12: Null-safe CharacterDto mapping
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
                    ?: "Visual DNA ready for extraction"
                val voiceText = dto.voice_config?.get("voice_id")?.toString() ?: "svara_hi_male_hero_01"
                val roleText = dto.role ?: dto.visual_dna?.get("role")?.toString() ?: "Main Cast"
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

    // Fix #39 & #44: Avoid "Episode 1: Episode 1" duplication and expose script/video/thumbnail
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
                    status = dto.current_state ?: "DRAFT",
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
            project_id = projectId.ifBlank { null },
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

    suspend fun fetchAdminWorkflows(projectId: String): Resource<List<JsonMap>> = try {
        mapEnvelope(apiService.getAdminWorkflows(projectId)) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to load workflows")
    }

    suspend fun fetchAdminWorkers(projectId: String): Resource<List<JsonMap>> = try {
        mapEnvelope(apiService.getAdminWorkers(projectId)) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to load workers")
    }

    suspend fun fetchAdminQueues(projectId: String): Resource<JsonMap> = try {
        mapEnvelope(apiService.getAdminQueues(projectId)) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to load queues")
    }

    suspend fun fetchLedgerSummary(projectId: String): Resource<JsonMap> = try {
        mapEnvelope(apiService.getLedgerSummary(projectId)) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to load ledger summary")
    }

    suspend fun fetchQuota(projectId: String): Resource<JsonMap> = try {
        mapEnvelope(apiService.getProjectQuota(projectId)) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to load quota")
    }

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

    suspend fun deletePrompt(promptId: String): Resource<JsonMap> = try {
        mapEnvelope(apiService.deleteSystemPrompt(promptId)) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to delete prompt")
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
}
