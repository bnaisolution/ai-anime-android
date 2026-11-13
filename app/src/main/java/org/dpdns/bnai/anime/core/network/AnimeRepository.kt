package org.dpdns.bnai.anime.core.network

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.dpdns.bnai.anime.core.auth.TokenManager
import org.dpdns.bnai.anime.core.model.*
import retrofit2.Response
import java.util.UUID

class AnimeRepository(
    private val apiService: AnimeApiService,
    private val tokenManager: TokenManager
) : AnimeRepositoryApi {

    override fun currentProjectId(): String =
        tokenManager.getActiveProjectId()

    override suspend fun logout(): Resource<Unit> = try {
        val response = apiService.logout()
        tokenManager.clearToken()
        if (response.isSuccessful || response.code() == 401) {
            Resource.Success(Unit)
        } else {
            Resource.Error("Logout failed (HTTP ${response.code()})", response.code())
        }
    } catch (e: Exception) {
        tokenManager.clearToken()
        Resource.Error(e.localizedMessage ?: "Logout failed")
    }

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

    override suspend fun fetchProjects(): Resource<List<Project>> = try {
        val response = apiService.getProjects()
        if (response.isSuccessful) {
            val dtoList = response.body()?.data.orEmpty()
            if (dtoList.isEmpty()) {
                Resource.Empty
            } else {
                val currentActiveId = currentProjectId()
                val effectiveActiveId = if (currentActiveId.isBlank() || dtoList.none { it.id == currentActiveId }) {
                    dtoList.first().id.also { tokenManager.saveActiveProjectId(it) }
                } else {
                    currentActiveId
                }
                Resource.Success(dtoList.map { dto ->
                    Project(
                        id = dto.id,
                        name = dto.name,
                        activeEpisode = projectProgressLabel(dto.current_season, dto.current_episode),
                        status = if (dto.id == effectiveActiveId) "SELECTED" else "IDLE"
                    )
                })
            }
        } else {
            Resource.Error("HTTP ${response.code()}", response.code())
        }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Network failure")
    }

    override suspend fun createProjectSync(name: String, promptIds: List<String>): Resource<ProjectDto> = try {
        val payload = mutableMapOf<String, Any?>("name" to name)
        if (promptIds.isNotEmpty()) payload["prompt_ids"] = promptIds
        mapEnvelope(apiService.createProjectSync(payload)) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Project creation failed")
    }

    override suspend fun deleteProject(projectId: String): Resource<Unit> = try {
        val response = apiService.deleteProject(projectId)
        if (response.isSuccessful) {
            if (currentProjectId() == projectId) {
                tokenManager.clearActiveProjectId()
            }
            Resource.Success(Unit)
        } else {
            Resource.Error("HTTP ${response.code()}", response.code())
        }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Project deletion failed")
    }

    override suspend fun fetchProjectOverview(projectId: String): Resource<ProjectOverviewDto> = try {
        mapEnvelope(apiService.getProjectOverview(projectId)) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to load overview")
    }

    override suspend fun fetchProjectOverviewRaw(
        projectId: String
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
                        "story_bible" to bible,
                        "active_system_prompt_id" to dto.project?.active_system_prompt_id
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

    override suspend fun updateProjectSettings(
        projectId: String,
        visualMedium: String?,
        genre: String?,
        autoApprove: Boolean?,
        coverImageUrl: String?,
        budgetCeiling: Int?,
        activeSystemPromptId: String?
    ): Resource<ProjectDto> = try {
        mapEnvelope(
            apiService.updateProjectSettings(
                projectId,
                UpdateProjectSettingsRequest(
                    visual_medium = visualMedium,
                    genre = genre,
                    auto_approve = autoApprove,
                    cover_image_url = coverImageUrl,
                    budget_ceiling = budgetCeiling,
                    active_system_prompt_id = activeSystemPromptId
                )
            )
        ) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to update project settings")
    }

    override suspend fun setActiveSystemPrompt(
        projectId: String,
        promptId: String
    ): Resource<JsonMap> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.updateProjectSettings(
                projectId,
                UpdateProjectSettingsRequest(active_system_prompt_id = promptId)
            )
            if (response.isSuccessful) {
                Resource.Success(mapOf("active_system_prompt_id" to promptId))
            } else {
                Resource.Error("Failed to activate prompt (${response.code()})", response.code())
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to activate prompt")
        }
    }

    override suspend fun updateProjectSettings(
        projectId: String,
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

    override suspend fun updateBudgetLimit(projectId: String, limit: Int): Resource<JsonMap> = try {
        mapEnvelope(apiService.updateProjectBudget(projectId, UpdateBudgetRequest(limit))) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to update budget limit")
    }

    override suspend fun updateBudgetLimit(
        projectId: String,
        limit: Double
    ): Resource<JsonMap> {
        return updateBudgetLimit(projectId, limit.toInt())
    }

    override suspend fun fetchBible(projectId: String): Resource<JsonMap> = try {
        mapEnvelope(apiService.getProjectBible(projectId)) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to load Story Bible")
    }

    override suspend fun updateProjectBible(
        projectId: String,
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

    override suspend fun fetchCharacters(projectId: String): Resource<List<Character>> = try {
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
                val voiceText = dto.voice_config?.get("voice_id")
                    ?.toString()
                    ?.takeIf { it.isNotBlank() && it != "null" }
                    ?: "UNSET"
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

    override suspend fun fetchCharactersRaw(projectId: String): Resource<List<CharacterDto>> = try {
        mapEnvelope(apiService.getProjectCharacters(projectId)) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to load raw characters")
    }

    override suspend fun createCharacterSync(
        projectId: String,
        name: String,
        role: String,
        status: String,
        voiceId: String,
        visualDnaSummary: String
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

    override suspend fun updateCharacterSync(
        projectId: String,
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

    override suspend fun fetchEpisodes(projectId: String): Resource<List<Episode>> = try {
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

    override suspend fun fetchEpisodesRaw(projectId: String): Resource<List<EpisodeDto>> = withContext(Dispatchers.IO) {
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

    override suspend fun updateEpisodeSync(
        projectId: String,
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

    override suspend fun fetchEpisodePlan(projectId: String, episodeNo: Int): Resource<JsonMap> = try {
        mapEnvelope(apiService.getEpisodePlan(projectId, episodeNo)) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to load episode plan")
    }

    override suspend fun fetchScenes(projectId: String, episodeNo: Int): Resource<List<SceneDto>> = try {
        mapEnvelope(apiService.getEpisodeScenes(projectId, episodeNo)) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to load scenes")
    }

    override suspend fun fetchScenesRaw(projectId: String, episodeNo: Int): Resource<List<SceneDto>> {
        return fetchScenes(projectId, episodeNo)
    }

    override suspend fun updateSceneSync(
        projectId: String,
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

    override suspend fun fetchProjectMedia(projectId: String): Resource<JsonMap> = try {
        mapEnvelope(apiService.getProjectMedia(projectId)) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to load project media")
    }

    override suspend fun fetchJobs(projectId: String): Resource<List<JobDto>> = try {
        mapEnvelope(apiService.getProjectJobs(projectId)) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to load jobs")
    }

    override suspend fun fetchJobDetail(projectId: String, jobId: String): Resource<JobDto> = try {
        mapEnvelope(apiService.getJobDetail(projectId, jobId)) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to load job details")
    }

    override suspend fun cancelJobSync(
        projectId: String,
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

    override suspend fun fetchRetries(projectId: String): Resource<List<JsonMap>> = try {
        mapEnvelope(apiService.getProjectRetries(projectId)) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to load retry history")
    }

    override suspend fun fetchApprovals(projectId: String): Resource<List<ApprovalDecisionDto>> = try {
        mapEnvelope(apiService.getProjectApprovals(projectId)) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to load approvals")
    }

    override suspend fun submitApprovalDecision(
        projectId: String,
        approvalId: String,
        decision: String,
        reason: String,
        resourceVersion: Int
    ): Resource<ApprovalDecisionDto> = try {
        val key = "apr-${UUID.randomUUID()}"
        mapEnvelope(
            apiService.decideApproval(projectId, approvalId, key, DecideApprovalRequest(decision, reason, resourceVersion))
        ) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to submit decision")
    }

    override suspend fun chatWithBrain(
        message: String,
        projectId: String,
        currentScreen: String?,
        conversationId: String?
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

    override suspend fun askAiDirector(
        message: String,
        currentScreen: String,
        conversationId: String?,
        projectId: String
    ): Resource<AiChatResponse> {
        return chatWithBrain(
            message = message,
            projectId = projectId,
            currentScreen = currentScreen,
            conversationId = conversationId
        )
    }

    override suspend fun submitStudioCommand(
        projectId: String,
        intent: CommandIntent,
        mode: String,
        episodeNo: Int?,
        payload: JsonMap,
        customIdempotencyKey: String?
    ): Resource<SubmitCommandResultDto> {
        if (projectId.isBlank()) {
            return Resource.Error("Please select or create a project first.")
        }
        val idempotencyKey = customIdempotencyKey ?: "cmd-${intent.name.lowercase()}-${UUID.randomUUID()}"
        return try {
            val response = apiService.submitCommand(
                idempotencyKey,
                CreateCommandRequest(projectId, intent.name, mode, episodeNo, payload)
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

    override suspend fun fetchCommandStatus(commandId: String): Resource<ApiCommandRecordDto> = try {
        mapEnvelope(apiService.getCommandStatus(commandId)) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Status poll failed")
    }

    override suspend fun triggerQuickMedia(
        projectId: String,
        isVideo: Boolean,
        prompt: String,
        durationSec: Int
    ): Resource<SubmitCommandResultDto> = try {
        val key = "quick-${if (isVideo) "vid" else "img"}-${UUID.randomUUID()}"
        val response = if (isVideo) {
            apiService.quickGenerateVideo(
                projectId,
                key,
                QuickGenerateRequest(prompt = prompt, duration_sec = durationSec)
            )
        } else {
            apiService.quickGenerateImage(projectId, key, QuickGenerateRequest(prompt = prompt))
        }
        mapEnvelope(response) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Quick media request failed")
    }

    override suspend fun submitQuickImage(
        projectId: String,
        prompt: String,
        stylePreset: String
    ): Resource<SubmitCommandResultDto> {
        return triggerQuickMedia(
            projectId = projectId,
            isVideo = false,
            prompt = "$prompt [Style: $stylePreset]"
        )
    }

    override suspend fun submitQuickVideo(
        projectId: String,
        prompt: String,
        durationSec: Int
    ): Resource<SubmitCommandResultDto> {
        return triggerQuickMedia(
            projectId = projectId,
            isVideo = true,
            prompt = prompt,
            durationSec = durationSec
        )
    }

    override suspend fun triggerVisionAnalysis(
        projectId: String,
        mediaUrl: String,
        prompt: String,
        characterCode: String?,
        saveToCharacter: Boolean
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

    override suspend fun submitVisionAnalysis(
        projectId: String,
        imageUrl: String,
        analysisType: String,
        characterId: String
    ): Resource<SubmitCommandResultDto> {
        return triggerVisionAnalysis(
            projectId = projectId,
            mediaUrl = imageUrl,
            prompt = "Extract 11-field Character Visual DNA",
            characterCode = characterId,
            saveToCharacter = analysisType.equals("SAVE", ignoreCase = true)
        )
    }

    override suspend fun fetchAnalytics(projectId: String): Resource<JsonMap> = try {
        mapEnvelope(apiService.getProjectAnalytics(projectId)) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to load analytics")
    }

    override suspend fun fetchAuditLogs(projectId: String): Resource<List<JsonMap>> = try {
        mapEnvelope(apiService.getProjectAudit(projectId)) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to load audit logs")
    }

    override suspend fun fetchNotifications(projectId: String): Resource<List<JsonMap>> = try {
        mapEnvelope(apiService.getProjectNotifications(projectId)) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to load notifications")
    }

    override suspend fun fetchAdminWorkflows(projectId: String): Resource<List<JsonMap>> = try {
        mapEnvelope(apiService.getAdminWorkflows(projectId)) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to load workflows")
    }

    override suspend fun fetchAdminWorkers(projectId: String): Resource<List<JsonMap>> = try {
        mapEnvelope(apiService.getAdminWorkers(projectId)) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to load workers")
    }

    override suspend fun fetchAdminQueues(projectId: String): Resource<JsonMap> = try {
        mapEnvelope(apiService.getAdminQueues(projectId)) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to load queues")
    }

    override suspend fun fetchLedgerSummary(projectId: String): Resource<JsonMap> = try {
        mapEnvelope(apiService.getLedgerSummary(projectId)) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to load ledger summary")
    }

    override suspend fun fetchAdminLedgerSummary(): Resource<JsonMap> =
        fetchLedgerSummary(currentProjectId())

    override suspend fun fetchQuota(projectId: String): Resource<JsonMap> = try {
        mapEnvelope(apiService.getProjectQuota(projectId)) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to load quota")
    }

    override suspend fun fetchProjectQuota(projectId: String): Resource<JsonMap> =
        fetchQuota(projectId)

    override suspend fun fetchCanon(projectId: String): Resource<List<JsonMap>> = try {
        mapEnvelope(apiService.getProjectCanon(projectId)) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to load canon")
    }

    override suspend fun fetchCanonProposals(projectId: String): Resource<List<JsonMap>> = try {
        mapEnvelope(apiService.getCanonProposals(projectId)) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to load canon proposals")
    }

    override suspend fun fetchContinuity(projectId: String): Resource<List<JsonMap>> = try {
        mapEnvelope(apiService.getProjectContinuity(projectId)) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to load continuity checks")
    }

    override suspend fun fetchCategoryItems(projectId: String, categoryPath: String): Resource<List<JsonMap>> = try {
        mapEnvelope(apiService.getProjectCategoryList(projectId, categoryPath)) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to load $categoryPath")
    }

    override suspend fun fetchCharacterReferenceHistory(projectId: String, characterId: String): Resource<List<JsonMap>> = try {
        mapEnvelope(apiService.getCharacterReferenceHistory(projectId, characterId)) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to load character reference history")
    }

    override suspend fun fetchPromptLibraryMeta(): Resource<PromptLibraryMetaDto> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getPromptLibraryMeta()
            if (response.isSuccessful && response.body()?.data != null) {
                Resource.Success(response.body()!!.data!!)
            } else {
                Resource.Error("Failed to load prompt library metadata (${response.code()})", response.code())
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to load prompt library metadata")
        }
    }

    override suspend fun fetchSystemPrompts(
        projectId: String,
        promptCategory: String?,
        query: String?,
        favoritesOnly: Boolean?,
        genre: String?,
        format: String?
    ): Resource<List<SystemPromptDto>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getSystemPrompts(
                projectId = projectId.takeIf { it.isNotBlank() },
                promptCategory = promptCategory,
                query = query,
                favoritesOnly = favoritesOnly,
                genre = genre,
                format = format
            )
            if (response.isSuccessful) {
                val prompts = response.body()?.data.orEmpty()
                if (prompts.isEmpty()) Resource.Empty else Resource.Success(prompts)
            } else {
                Resource.Error("Failed to load prompts (${response.code()})", response.code())
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to load prompts")
        }
    }

    override suspend fun saveSystemPrompt(
        projectId: String,
        title: String,
        promptText: String,
        visualMedium: String,
        promptCategory: String,
        genre: String?,
        format: String?,
        modelHint: String?
    ): Resource<SystemPromptDto> = withContext(Dispatchers.IO) {
        try {
            val req = CreateSystemPromptRequest(
                project_id = projectId.takeIf { it.isNotBlank() },
                title = title,
                prompt_text = promptText,
                visual_medium = visualMedium,
                prompt_category = promptCategory,
                genre = genre,
                format = format,
                model_hint = modelHint
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

    override suspend fun updateSystemPrompt(
        promptId: String,
        title: String?,
        promptText: String?,
        visualMedium: String?,
        promptCategory: String?,
        isFavorite: Boolean?,
        genre: String?,
        format: String?,
        modelHint: String?
    ): Resource<SystemPromptDto> = withContext(Dispatchers.IO) {
        try {
            val req = UpdateSystemPromptRequest(
                title = title,
                prompt_text = promptText,
                visual_medium = visualMedium,
                prompt_category = promptCategory,
                is_favorite = isFavorite,
                genre = genre,
                format = format,
                model_hint = modelHint
            )
            val response = apiService.updateSystemPrompt(promptId, req)
            if (response.isSuccessful && response.body()?.data != null) {
                Resource.Success(response.body()!!.data!!)
            } else {
                Resource.Error("Failed to update prompt (${response.code()})", response.code())
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to update prompt")
        }
    }

    override suspend fun associatePromptWithProject(
        projectId: String,
        promptId: String,
        surface: String
    ): Resource<JsonMap> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.associatePromptWithProject(
                promptId,
                mapOf("project_id" to projectId, "surface" to surface)
            )
            if (response.isSuccessful) {
                Resource.Success(response.body()?.data ?: emptyMap())
            } else {
                Resource.Error("Failed to associate prompt (${response.code()})", response.code())
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to associate prompt")
        }
    }

    override suspend fun togglePromptFavorite(promptId: String): Resource<SystemPromptDto> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.togglePromptFavorite(promptId)
            if (response.isSuccessful && response.body()?.data != null) {
                Resource.Success(response.body()!!.data!!)
            } else {
                Resource.Error("Failed to favorite prompt (${response.code()})", response.code())
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to favorite prompt")
        }
    }

    override suspend fun deletePrompt(promptId: String): Resource<JsonMap> = try {
        mapEnvelope(apiService.deleteSystemPrompt(promptId)) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to delete prompt")
    }

    override suspend fun deleteSystemPrompt(promptId: String): Resource<JsonMap> =
        deletePrompt(promptId)

    override suspend fun fetchPlatformManagedStatus(): Resource<JsonMap> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getPlatformManagedStatus()
            if (response.isSuccessful) Resource.Success(response.body()?.data ?: emptyMap())
            else Resource.Error("Failed to fetch platform status (${response.code()})", response.code())
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to fetch platform status")
        }
    }

    override suspend fun fetchReplicationStatus(): Resource<JsonMap> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getReplicationStatus()
            if (response.isSuccessful) Resource.Success(response.body()?.data ?: emptyMap())
            else Resource.Error("Failed to fetch replication status (${response.code()})", response.code())
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to fetch replication status")
        }
    }

    override suspend fun fetchOwnerQuota(): Resource<JsonMap> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getOwnerQuota()
            if (response.isSuccessful) Resource.Success(response.body()?.data ?: emptyMap())
            else Resource.Error("Failed to fetch owner quota (${response.code()})", response.code())
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to fetch owner quota")
        }
    }

    override suspend fun fetchOwnerTenants(): Resource<List<JsonMap>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getOwnerTenants()
            if (response.isSuccessful) Resource.Success(response.body()?.data.orEmpty())
            else Resource.Error("Failed to fetch owner tenants (${response.code()})", response.code())
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to fetch owner tenants")
        }
    }

    override suspend fun fetchReadiness(): Resource<JsonMap> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getReadiness()
            if (response.isSuccessful) {
                Resource.Success(response.body() ?: emptyMap())
            } else {
                Resource.Error("Readiness check failed (${response.code()})", response.code())
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Readiness check failed")
        }
    }

    override suspend fun fetchStorageStatus(): Resource<JsonMap> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getStorageStatus()
            if (response.isSuccessful) {
                Resource.Success(response.body()?.data ?: emptyMap())
            } else {
                Resource.Error("Storage status failed (${response.code()})", response.code())
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Storage status failed")
        }
    }

    override suspend fun uploadStorageObject(
        key: String,
        filename: String,
        contentType: String,
        bytes: ByteArray
    ): Resource<JsonMap> = withContext(Dispatchers.IO) {
        try {
            val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
            val response = apiService.uploadStorageObject(
                StorageUploadRequest(
                    key = key,
                    filename = filename,
                    content_type = contentType,
                    data_base64 = encoded
                )
            )
            if (response.isSuccessful) {
                Resource.Success(response.body()?.data ?: emptyMap())
            } else {
                Resource.Error("Cover upload failed (${response.code()})", response.code())
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Cover upload failed")
        }
    }

    override suspend fun fetchGpuInfrastructure(): Resource<JsonMap> = withContext(Dispatchers.IO) {
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

    override suspend fun saveGpuInfrastructure(payload: JsonMap): Resource<JsonMap> = withContext(Dispatchers.IO) {
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

    override suspend fun fetchHealthDetail(): Resource<HealthResponseDto> = try {
        val res = apiService.getAnimeHealth()
        if (res.isSuccessful && res.body() != null) Resource.Success(res.body()!!)
        else Resource.Error("Health HTTP ${res.code()}", res.code())
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Backend unreachable")
    }

    override suspend fun checkBackendHealth(): Boolean =
        (fetchHealthDetail() as? Resource.Success)?.data?.ok == true

    override suspend fun resolveConfiguredAiProviderBadge(): String = withContext(Dispatchers.IO) {
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
                    val label = "AI: $cleanPrimary$cleanFallback"
                    TokenManager.cachedProviderLabel = label
                    return@withContext label
                }
            }
            return@withContext TokenManager.cachedProviderLabel ?: "AI: Groq • Gemini"
        } catch (_: Exception) {
            return@withContext "AI: Disconnected"
        }
    }
    override suspend fun readMemory(
        projectId: String,
        scope: String?,
        query: String?
    ): Resource<List<MemoryItemDto>> = withContext(Dispatchers.IO) {
        try {
            val body = buildMap<String, Any?> {
                if (projectId.isNotBlank()) put("project_id", projectId)
                if (!scope.isNullOrBlank()) put("scope", scope)
                if (!query.isNullOrBlank()) put("query", query)
            }
            val response = apiService.readMemory(body)
            if (response.isSuccessful) {
                Resource.Success(response.body()?.memory_items.orEmpty())
            } else {
                Resource.Error("Failed to read memory (${response.code()})", response.code())
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to read memory")
        }
    }

    override suspend fun writeMemory(
        projectId: String,
        content: String,
        scope: String,
        category: String,
        idempotencyKey: String?
    ): Resource<MemoryItemDto> = withContext(Dispatchers.IO) {
        try {
            val body = buildMap<String, Any?> {
                if (projectId.isNotBlank()) put("project_id", projectId)
                put("content", content)
                put("scope", scope)
                put("category", category)
                if (!idempotencyKey.isNullOrBlank()) put("idempotency_key", idempotencyKey)
            }
            val response = apiService.writeMemory(body)
            val item = response.body()?.memory_item
            if (response.isSuccessful && item != null) {
                Resource.Success(item)
            } else {
                Resource.Error("Failed to write memory (${response.code()})", response.code())
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to write memory")
        }
    }

    override suspend fun correctMemory(memoryId: String, content: String): Resource<MemoryItemDto> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.correctMemory(
                    mapOf("memory_id" to memoryId, "content" to content)
                )
                val item = response.body()?.memory_item
                if (response.isSuccessful && item != null) {
                    Resource.Success(item)
                } else {
                    Resource.Error("Failed to correct memory (${response.code()})", response.code())
                }
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to correct memory")
            }
        }

    override suspend fun deleteMemory(
        memoryId: String?,
        projectId: String?,
        scope: String?
    ): Resource<JsonMap> = withContext(Dispatchers.IO) {
        try {
            val body = buildMap<String, Any?> {
                if (!memoryId.isNullOrBlank()) put("memory_id", memoryId)
                else if (!scope.isNullOrBlank()) put("scope", scope)
                else if (!projectId.isNullOrBlank()) put("project_id", projectId)
            }
            if (body.isEmpty()) {
                return@withContext Resource.Error("memory_id, scope, or project_id required")
            }
            val response = apiService.deleteMemory(body)
            if (response.isSuccessful) {
                Resource.Success(response.body() ?: emptyMap())
            } else {
                Resource.Error("Failed to delete memory (${response.code()})", response.code())
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to delete memory")
        }
    }

    override suspend fun reconcileMemory(projectId: String): Resource<JsonMap> =
        withContext(Dispatchers.IO) {
            try {
                val body = if (projectId.isBlank()) emptyMap<String, Any?>() else mapOf("project_id" to projectId)
                val response = apiService.reconcileMemory(body)
                if (response.isSuccessful) {
                    Resource.Success(response.body() ?: emptyMap())
                } else {
                    Resource.Error("Failed to reconcile memory (${response.code()})", response.code())
                }
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to reconcile memory")
            }
        }

}
