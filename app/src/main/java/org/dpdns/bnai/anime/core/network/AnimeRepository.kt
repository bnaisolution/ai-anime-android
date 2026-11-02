package org.dpdns.bnai.anime.core.network

import org.dpdns.bnai.anime.core.auth.TokenManager
import org.dpdns.bnai.anime.core.model.Character
import org.dpdns.bnai.anime.core.model.Episode
import org.dpdns.bnai.anime.core.model.Project
import retrofit2.Response
import java.util.UUID

class AnimeRepository(
    private val apiService: AnimeApiService = RetrofitClient.getApiService()
) {

    fun currentProjectId(): String =
        TokenManager.getInstance()?.getActiveProjectId().orEmpty()

    private fun <T, R> mapEnvelope(
        response: Response<ApiEnvelope<T>>,
        transform: (T) -> R
    ): Resource<R> {
        if (!response.isSuccessful) {
            return Resource.Error(
                "HTTP ${response.code()}: ${response.message()}",
                response.code()
            )
        }
        val data = response.body()?.data ?: return Resource.Empty
        if (data is Collection<*> && data.isEmpty()) return Resource.Empty
        if (data is Map<*, *> && data.isEmpty()) return Resource.Empty
        return Resource.Success(transform(data))
    }

    // ========================================================================
    // PROJECTS
    // ========================================================================
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
                        // GAP #5.2: Derive status from is_active — backend has no "status" column
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

    // ========================================================================
    // STORY BIBLE
    // ========================================================================
    suspend fun fetchBible(projectId: String): Resource<JsonMap> = try {
        mapEnvelope(apiService.getProjectBible(projectId)) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to load Story Bible")
    }

    // ========================================================================
    // CHARACTERS
    // ========================================================================
    suspend fun fetchCharacters(projectId: String): Resource<List<Character>> = try {
        mapEnvelope(apiService.getProjectCharacters(projectId)) { list ->
            list.map { dto ->
                val dnaText = dto.visual_dna?.entries?.joinToString("\n") {
                    "${it.key}: ${it.value}"
                } ?: "No visual DNA stored"
                val voiceText = dto.voice_config?.get("voice_id")?.toString() ?: "Unassigned"
                Character(
                    id = dto.character_code.ifBlank { dto.id ?: "UNKNOWN" },
                    name = dto.name,
                    role = dto.role ?: "Cast",
                    visualDna = dnaText,
                    voiceId = voiceText
                )
            }
        }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to load characters")
    }

    // ========================================================================
    // EPISODES
    // ========================================================================
    suspend fun fetchEpisodes(projectId: String): Resource<List<Episode>> = try {
        mapEnvelope(apiService.getProjectEpisodes(projectId)) { list ->
            list.map { dto ->
                Episode(
                    id = dto.id,
                    episodeNumber = dto.episode_no,
                    title = dto.title ?: "Episode ${dto.episode_no}",
                    status = dto.current_state ?: "DRAFT",
                    synopsis = dto.synopsis,
                    updatedAt = dto.updated_at
                )
            }
        }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to load episodes")
    }

    // ========================================================================
    // SCENES
    // ========================================================================
    suspend fun fetchScenes(projectId: String, episodeNo: Int): Resource<List<SceneDto>> = try {
        mapEnvelope(apiService.getEpisodeScenes(projectId, episodeNo)) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to load scenes")
    }

    // ========================================================================
    // JOBS
    // ========================================================================
    suspend fun fetchJobs(projectId: String): Resource<List<JobDto>> = try {
        mapEnvelope(apiService.getProjectJobs(projectId)) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to load jobs")
    }

    // ========================================================================
    // RETRIES
    // ========================================================================
    suspend fun fetchRetries(projectId: String): Resource<List<JsonMap>> = try {
        mapEnvelope(apiService.getProjectRetries(projectId)) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to load retry history")
    }

    // ========================================================================
    // APPROVALS
    // ========================================================================
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
            apiService.decideApproval(
                projectId,
                approvalId,
                key,
                DecideApprovalRequest(decision, reason, resourceVersion)
            )
        ) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to submit decision")
    }

    // ========================================================================
    // AI BRAIN
    // ========================================================================
    suspend fun chatWithBrain(
        message: String,
        projectId: String = currentProjectId()
    ): Resource<AiChatResponse> = try {
        val req = AiChatRequest(
            message = message,
            project_id = projectId.ifBlank { null },
            skill_key = "ai_admin",
            request_id = UUID.randomUUID().toString()
        )
        val response = apiService.chatWithAi(req)
        if (response.isSuccessful && response.body() != null) {
            Resource.Success(response.body()!!)
        } else {
            Resource.Error("AI Brain HTTP ${response.code()}", response.code())
        }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "AI Brain unreachable")
    }

    // ========================================================================
    // COMMANDS (WF-00 — idempotency enforced)
    // ========================================================================
    suspend fun submitStudioCommand(
        projectId: String,
        intent: CommandIntent,
        mode: String = "SAVE",
        episodeNo: Int? = null,
        payload: JsonMap = emptyMap(),
        customIdempotencyKey: String? = null
    ): Resource<SubmitCommandResultDto> {
        if (projectId.isBlank()) {
            return Resource.Error("Please select or create a project first.")
        }
        val idempotencyKey = customIdempotencyKey
            ?: "cmd-${intent.name.lowercase()}-${UUID.randomUUID()}"
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

    suspend fun fetchCommandStatus(commandId: String): Resource<ApiCommandRecordDto> = try {
        mapEnvelope(apiService.getCommandStatus(commandId)) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Status poll failed")
    }

    // ========================================================================
    // QUICK MEDIA & VISION (WF-05)
    // ========================================================================
    suspend fun triggerQuickMedia(
        projectId: String,
        isVideo: Boolean,
        prompt: String
    ): Resource<SubmitCommandResultDto> = try {
        val key = "quick-${if (isVideo) "vid" else "img"}-${UUID.randomUUID()}"
        val response = if (isVideo) {
            apiService.quickGenerateVideo(
                projectId,
                key,
                QuickGenerateRequest(prompt = prompt, duration_sec = 5)
            )
        } else {
            apiService.quickGenerateImage(
                projectId,
                key,
                QuickGenerateRequest(prompt = prompt)
            )
        }
        mapEnvelope(response) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Quick media request failed")
    }

    suspend fun triggerVisionAnalysis(
        projectId: String,
        mediaUrl: String,
        prompt: String = "Extract 11-field Visual DNA"
    ): Resource<SubmitCommandResultDto> = try {
        val key = "vision-${UUID.randomUUID()}"
        mapEnvelope(
            apiService.analyzeVision(
                projectId,
                key,
                VisionAnalyzeRequest(media_url = mediaUrl, prompt = prompt)
            )
        ) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Vision request failed")
    }

    // ========================================================================
    // ANALYTICS & AUDIT (WF-08)
    // ========================================================================
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

    // ========================================================================
    // ADMIN — WORKFLOWS, WORKERS, QUEUES, LEDGER
    // ========================================================================
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

    // ========================================================================
    // GAP #2 NEW READS — QUOTA, CONTINUITY, CANON, CATEGORIES, REFERENCE HISTORY
    // ========================================================================
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

    suspend fun fetchContinuity(projectId: String): Resource<List<JsonMap>> = try {
        mapEnvelope(apiService.getProjectContinuity(projectId)) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to load continuity checks")
    }

    suspend fun fetchCategoryItems(
        projectId: String,
        categoryPath: String
    ): Resource<List<JsonMap>> = try {
        mapEnvelope(apiService.getProjectCategoryList(projectId, categoryPath)) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to load $categoryPath")
    }

    suspend fun fetchCharacterReferenceHistory(
        projectId: String,
        characterId: String
    ): Resource<List<JsonMap>> = try {
        mapEnvelope(apiService.getCharacterReferenceHistory(projectId, characterId)) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to load character reference history")
    }

    // ========================================================================
    // HEALTH
    // ========================================================================
    suspend fun fetchHealthDetail(): Resource<HealthResponseDto> = try {
        val res = apiService.getAnimeHealth()
        if (res.isSuccessful && res.body() != null) {
            Resource.Success(res.body()!!)
        } else {
            Resource.Error("Health HTTP ${res.code()}", res.code())
        }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Backend unreachable")
    }

    suspend fun checkBackendHealth(): Boolean =
        (fetchHealthDetail() as? Resource.Success)?.data?.ok == true
}
