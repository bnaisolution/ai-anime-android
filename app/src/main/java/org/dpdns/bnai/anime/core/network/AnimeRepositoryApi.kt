package org.dpdns.bnai.anime.core.network

import org.dpdns.bnai.anime.core.model.Character
import org.dpdns.bnai.anime.core.model.Episode
import org.dpdns.bnai.anime.core.model.Project

/**
 * Shared context for all repository features.
 *
 * Implementations must fail fast when an operation is not configured in a fake.
 * Production implementations override every operation.
 */
interface RepositoryContext {
    fun currentProjectId(): String =
        throw UnsupportedOperationException("currentProjectId is not configured")
}

interface AuthRepository {
    suspend fun logout(): Resource<Unit> =
        throw UnsupportedOperationException("logout is not configured")
}

interface ProjectRepository : RepositoryContext {
    suspend fun fetchProjects(): Resource<List<Project>> =
        throw UnsupportedOperationException("fetchProjects is not configured")

    suspend fun createProjectSync(name: String, promptIds: List<String> = emptyList()): Resource<ProjectDto> =
        throw UnsupportedOperationException("createProjectSync is not configured")

    suspend fun deleteProject(projectId: String): Resource<Unit> =
        throw UnsupportedOperationException("deleteProject is not configured")

    suspend fun fetchProjectOverview(projectId: String): Resource<ProjectOverviewDto> =
        throw UnsupportedOperationException("fetchProjectOverview is not configured")

    suspend fun fetchProjectOverviewRaw(
        projectId: String = currentProjectId()
    ): Resource<JsonMap> =
        throw UnsupportedOperationException("fetchProjectOverviewRaw is not configured")

    suspend fun updateProjectSettings(
        projectId: String,
        visualMedium: String? = null,
        genre: String? = null,
        autoApprove: Boolean? = null,
        coverImageUrl: String? = null,
        budgetCeiling: Int? = null,
        activeSystemPromptId: String? = null,
        targetEpisodeCount: Int? = null,
        episodeDurationMin: Int? = null,
    ): Resource<ProjectDto> =
        throw UnsupportedOperationException("updateProjectSettings is not configured")

    suspend fun setActiveSystemPrompt(projectId: String, promptId: String): Resource<JsonMap> =
        throw UnsupportedOperationException("setActiveSystemPrompt is not configured")

    suspend fun updateProjectSettings(
        projectId: String = currentProjectId(),
        updates: JsonMap
    ): Resource<JsonMap> =
        throw UnsupportedOperationException("updateProjectSettings is not configured")

    suspend fun updateBudgetLimit(projectId: String, limit: Int): Resource<JsonMap> =
        throw UnsupportedOperationException("updateBudgetLimit is not configured")

    suspend fun updateBudgetLimit(
        projectId: String = currentProjectId(),
        limit: Double
    ): Resource<JsonMap> =
        throw UnsupportedOperationException("updateBudgetLimit is not configured")

    suspend fun fetchBible(projectId: String): Resource<JsonMap> =
        throw UnsupportedOperationException("fetchBible is not configured")

    suspend fun updateProjectBible(
        projectId: String = currentProjectId(),
        bibleUpdates: JsonMap
    ): Resource<JsonMap> =
        throw UnsupportedOperationException("updateProjectBible is not configured")

    suspend fun fetchBiblePatches(projectId: String): Resource<List<BiblePatchDto>> =
        throw UnsupportedOperationException("fetchBiblePatches is not configured")

    suspend fun approveBiblePatch(projectId: String, patchId: String, baseHash: String?): Resource<JsonMap> =
        throw UnsupportedOperationException("approveBiblePatch is not configured")

    suspend fun rejectBiblePatch(projectId: String, patchId: String, reason: String?): Resource<JsonMap> =
        throw UnsupportedOperationException("rejectBiblePatch is not configured")

    suspend fun fetchQuota(projectId: String = currentProjectId()): Resource<JsonMap> =
        throw UnsupportedOperationException("fetchQuota is not configured")

    suspend fun fetchProjectQuota(projectId: String = currentProjectId()): Resource<JsonMap> =
        throw UnsupportedOperationException("fetchProjectQuota is not configured")

    suspend fun fetchCanon(projectId: String): Resource<List<JsonMap>> =
        throw UnsupportedOperationException("fetchCanon is not configured")

    suspend fun fetchCanonProposals(projectId: String): Resource<List<JsonMap>> =
        throw UnsupportedOperationException("fetchCanonProposals is not configured")

    suspend fun fetchContinuity(projectId: String): Resource<List<JsonMap>> =
        throw UnsupportedOperationException("fetchContinuity is not configured")

    suspend fun fetchCategoryItems(projectId: String, categoryPath: String): Resource<List<JsonMap>> =
        throw UnsupportedOperationException("fetchCategoryItems is not configured")
}

interface CharacterRepository : RepositoryContext {
    suspend fun fetchCharacters(projectId: String): Resource<List<Character>> =
        throw UnsupportedOperationException("fetchCharacters is not configured")

    suspend fun fetchCharactersRaw(projectId: String): Resource<List<CharacterDto>> =
        throw UnsupportedOperationException("fetchCharactersRaw is not configured")

    suspend fun createCharacterSync(
        projectId: String = currentProjectId(),
        name: String,
        role: String,
        status: String = "alive",
        voiceId: String = "UNSET",
        visualDnaSummary: String = ""
    ): Resource<CharacterDto> =
        throw UnsupportedOperationException("createCharacterSync is not configured")

    suspend fun updateCharacterSync(
        projectId: String = currentProjectId(),
        characterIdOrCode: String,
        updates: JsonMap
    ): Resource<CharacterDto> =
        throw UnsupportedOperationException("updateCharacterSync is not configured")

    suspend fun fetchCharacterReferenceHistory(
        projectId: String,
        characterId: String
    ): Resource<List<JsonMap>> =
        throw UnsupportedOperationException("fetchCharacterReferenceHistory is not configured")
}

interface EpisodeRepository : RepositoryContext {
    suspend fun fetchEpisodes(projectId: String): Resource<List<Episode>> =
        throw UnsupportedOperationException("fetchEpisodes is not configured")

    suspend fun fetchEpisodesRaw(
        projectId: String = currentProjectId(),
        seasonNo: Int = 1
    ): Resource<List<EpisodeDto>> =
        throw UnsupportedOperationException("fetchEpisodesRaw is not configured")

    suspend fun updateEpisodeSync(
        projectId: String = currentProjectId(),
        episodeNo: Int,
        updates: JsonMap
    ): Resource<EpisodeDto> =
        throw UnsupportedOperationException("updateEpisodeSync is not configured")

    suspend fun submitEpisodeApproval(
        projectId: String = currentProjectId(),
        episodeNo: Int,
        seasonNo: Int = 1,
        decision: String = "APPROVED"
    ): Resource<EpisodeDto> =
        throw UnsupportedOperationException("submitEpisodeApproval is not configured")

    suspend fun fetchEpisodePlan(projectId: String, episodeNo: Int, seasonNo: Int = 1): Resource<JsonMap> =
        throw UnsupportedOperationException("fetchEpisodePlan is not configured")

    suspend fun fetchScenes(projectId: String, episodeNo: Int, seasonNo: Int = 1): Resource<List<SceneDto>> =
        throw UnsupportedOperationException("fetchScenes is not configured")

    suspend fun fetchScenesRaw(
        projectId: String = currentProjectId(),
        episodeNo: Int = 1
    ): Resource<List<SceneDto>> =
        throw UnsupportedOperationException("fetchScenesRaw is not configured")

    suspend fun updateSceneSync(
        projectId: String = currentProjectId(),
        episodeNo: Int,
        sceneId: String,
        updates: JsonMap
    ): Resource<SceneDto> =
        throw UnsupportedOperationException("updateSceneSync is not configured")
}

interface MediaRepository : RepositoryContext {
    suspend fun fetchProjectMedia(projectId: String): Resource<JsonMap> =
        throw UnsupportedOperationException("fetchProjectMedia is not configured")
}

interface ProductionRepository : RepositoryContext {
    suspend fun fetchJobs(projectId: String): Resource<List<JobDto>> =
        throw UnsupportedOperationException("fetchJobs is not configured")

    suspend fun fetchJobDetail(projectId: String, jobId: String): Resource<JobDto> =
        throw UnsupportedOperationException("fetchJobDetail is not configured")

    suspend fun cancelJobSync(
        projectId: String = currentProjectId(),
        jobId: String
    ): Resource<JsonMap> =
        throw UnsupportedOperationException("cancelJobSync is not configured")

    suspend fun fetchRetries(projectId: String): Resource<List<JsonMap>> =
        throw UnsupportedOperationException("fetchRetries is not configured")

    suspend fun fetchApprovals(projectId: String): Resource<List<ApprovalDecisionDto>> =
        throw UnsupportedOperationException("fetchApprovals is not configured")

    suspend fun submitApprovalDecision(
        projectId: String,
        approvalId: String,
        decision: String,
        reason: String,
        resourceVersion: Int = 1
    ): Resource<ApprovalDecisionDto> =
        throw UnsupportedOperationException("submitApprovalDecision is not configured")

    suspend fun chatWithBrain(
        message: String,
        projectId: String = currentProjectId(),
        currentScreen: String? = null,
        conversationId: String? = null,
        seasonNo: Int? = null,
        episodeNo: Int? = null,
        targetDurationSec: Int? = null,
        requestedAction: RequestedDirectorActionDto? = null
    ): Resource<AiChatResponse> =
        throw UnsupportedOperationException("chatWithBrain is not configured")

    suspend fun askAiDirector(
        message: String,
        currentScreen: String = "STUDIO",
        conversationId: String? = null,
        projectId: String = currentProjectId(),
        seasonNo: Int? = null,
        episodeNo: Int? = null,
        targetDurationSec: Int? = null,
        requestedAction: RequestedDirectorActionDto? = null
    ): Resource<AiChatResponse> =
        throw UnsupportedOperationException("askAiDirector is not configured")

    suspend fun executeDirectorAction(
        projectId: String,
        actionId: String,
        idempotencyKey: String? = null
    ): Resource<DirectorActionExecutionDto> =
        throw UnsupportedOperationException("executeDirectorAction is not configured")

    suspend fun clearDirectorConversation(projectId: String): Resource<Unit> =
        throw UnsupportedOperationException("clearDirectorConversation is not configured")

    suspend fun loadDirectorReadiness(projectId: String? = null): Resource<DirectorReadinessDto> =
        throw UnsupportedOperationException("loadDirectorReadiness is not configured")

    suspend fun createDirectorProject(
        name: String,
        premise: String? = null,
        genre: String? = null,
        projectFormat: String? = null,
        targetEpisodeCount: Int? = null,
        episodeDurationMin: Int? = null,
        totalSeasons: Int? = null,
        episodesPerSeason: Int? = null
    ): Resource<DirectorProjectCreatePayload> =
        throw UnsupportedOperationException("createDirectorProject is not configured")

    suspend fun loadDirectorCapabilities(): Resource<DirectorCapabilitiesPayload> =
        throw UnsupportedOperationException("loadDirectorCapabilities is not configured")

    suspend fun loadDirectorTimeline(
        projectId: String,
        commandId: String? = null,
        seasonNo: Int? = null,
        episodeNo: Int? = null
    ): Resource<DirectorTimelineDto> =
        throw UnsupportedOperationException("loadDirectorTimeline is not configured")

    suspend fun retryDirectorTimeline(
        projectId: String,
        commandId: String
    ): Resource<DirectorActionExecutionDto> =
        throw UnsupportedOperationException("retryDirectorTimeline is not configured")

    suspend fun buildDirectorPlan(projectId: String, goal: String? = null): Resource<DirectorPlanDto> =
        throw UnsupportedOperationException("buildDirectorPlan is not configured")

    suspend fun loadPublishChannels(): Resource<PublishChannelsPayload> =
        throw UnsupportedOperationException("loadPublishChannels is not configured")

    suspend fun startPublishOAuth(platform: String, projectId: String? = null): Resource<PublishOAuthStartPayload> =
        throw UnsupportedOperationException("startPublishOAuth is not configured")

    suspend fun testPublishChannel(channelId: String): Resource<PublishChannelTestPayload> =
        throw UnsupportedOperationException("testPublishChannel is not configured")

    suspend fun disconnectPublishChannel(channelId: String): Resource<JsonMap> =
        throw UnsupportedOperationException("disconnectPublishChannel is not configured")

    suspend fun loadCpuTarget(): Resource<CpuTargetDto> =
        throw UnsupportedOperationException("loadCpuTarget is not configured")

    suspend fun testCpuTarget(): Resource<CpuTestDto> =
        throw UnsupportedOperationException("testCpuTarget is not configured")

    suspend fun loadSaasPlans(): Resource<SaasPlansPayload> =
        throw UnsupportedOperationException("loadSaasPlans is not configured")

    suspend fun loadSaasSubscription(): Resource<SaasSubscriptionDto> =
        throw UnsupportedOperationException("loadSaasSubscription is not configured")

    suspend fun loadSaasUsage(days: Int = 30): Resource<SaasUsageDto> =
        throw UnsupportedOperationException("loadSaasUsage is not configured")

    suspend fun loadModelCatalog(): Resource<ModelCatalogPayload> =
        throw UnsupportedOperationException("loadModelCatalog is not configured")

    suspend fun loadModelPreferences(): Resource<ModelPreferencesPayload> =
        throw UnsupportedOperationException("loadModelPreferences is not configured")

    suspend fun saveModelSelection(
        purpose: String,
        provider: String,
        modelId: String,
        renderProfile: String? = null
    ): Resource<ModelSelectionPayload> =
        throw UnsupportedOperationException("saveModelSelection is not configured")

    suspend fun submitStudioCommand(
        projectId: String,
        intent: CommandIntent,
        mode: String = "SAVE",
        episodeNo: Int? = null,
        payload: JsonMap = emptyMap(),
        customIdempotencyKey: String? = null,
        seasonNo: Int? = null
    ): Resource<SubmitCommandResultDto> =
        throw UnsupportedOperationException("submitStudioCommand is not configured")

    suspend fun fetchCommandStatus(commandId: String): Resource<ApiCommandRecordDto> =
        throw UnsupportedOperationException("fetchCommandStatus is not configured")

    suspend fun triggerQuickMedia(
        projectId: String,
        isVideo: Boolean,
        prompt: String,
        durationSec: Int = 5
    ): Resource<SubmitCommandResultDto> =
        throw UnsupportedOperationException("triggerQuickMedia is not configured")

    suspend fun submitQuickImage(
        projectId: String = currentProjectId(),
        prompt: String,
        stylePreset: String = "ANIME"
    ): Resource<SubmitCommandResultDto> =
        throw UnsupportedOperationException("submitQuickImage is not configured")

    suspend fun submitQuickVideo(
        projectId: String = currentProjectId(),
        prompt: String,
        durationSec: Int = 5
    ): Resource<SubmitCommandResultDto> =
        throw UnsupportedOperationException("submitQuickVideo is not configured")

    suspend fun triggerVisionAnalysis(
        projectId: String,
        mediaUrl: String,
        prompt: String = "Extract 11-field Visual DNA",
        characterCode: String? = null,
        saveToCharacter: Boolean = false
    ): Resource<SubmitCommandResultDto> =
        throw UnsupportedOperationException("triggerVisionAnalysis is not configured")

    suspend fun submitVisionAnalysis(
        projectId: String = currentProjectId(),
        imageUrl: String,
        analysisType: String = "QUICK",
        characterId: String = "CHAR_001"
    ): Resource<SubmitCommandResultDto> =
        throw UnsupportedOperationException("submitVisionAnalysis is not configured")
}

interface PromptRepository : RepositoryContext {
    suspend fun fetchPromptLibraryMeta(projectId: String? = null): Resource<PromptLibraryMetaDto> =
        throw UnsupportedOperationException("fetchPromptLibraryMeta is not configured")

    suspend fun fetchSystemPrompts(
        projectId: String = currentProjectId(),
        promptCategory: String? = null,
        query: String? = null,
        favoritesOnly: Boolean? = null,
        genre: String? = null,
        format: String? = null
    ): Resource<List<SystemPromptDto>> =
        throw UnsupportedOperationException("fetchSystemPrompts is not configured")

    suspend fun fetchPromptVersions(promptId: String): Resource<List<PromptVersionDto>> =
        throw UnsupportedOperationException("fetchPromptVersions is not configured")

    suspend fun activateSystemPrompt(projectId: String, promptId: String, surface: String = "BIBLE"): Resource<JsonMap> =
        throw UnsupportedOperationException("activateSystemPrompt is not configured")

    suspend fun associatePromptWithProject(
        projectId: String,
        promptId: String,
        surface: String
    ): Resource<JsonMap> =
        throw UnsupportedOperationException("associatePromptWithProject is not configured")

    suspend fun saveSystemPrompt(
        projectId: String = currentProjectId(),
        title: String,
        promptText: String,
        visualMedium: String = "ANIME",
        promptCategory: String = "BIBLE",
        genre: String? = null,
        format: String? = null,
        modelHint: String? = null
    ): Resource<SystemPromptDto> =
        throw UnsupportedOperationException("saveSystemPrompt is not configured")

    suspend fun updateSystemPrompt(
        promptId: String,
        title: String? = null,
        promptText: String? = null,
        visualMedium: String? = null,
        promptCategory: String? = null,
        isFavorite: Boolean? = null,
        genre: String? = null,
        format: String? = null,
        modelHint: String? = null
    ): Resource<SystemPromptDto> =
        throw UnsupportedOperationException("updateSystemPrompt is not configured")

    suspend fun togglePromptFavorite(promptId: String): Resource<SystemPromptDto> =
        throw UnsupportedOperationException("togglePromptFavorite is not configured")

    suspend fun deletePrompt(promptId: String): Resource<JsonMap> =
        throw UnsupportedOperationException("deletePrompt is not configured")

    suspend fun deleteSystemPrompt(promptId: String): Resource<JsonMap> =
        throw UnsupportedOperationException("deleteSystemPrompt is not configured")
}

interface MemoryRepository : RepositoryContext {
    suspend fun readMemory(
        projectId: String = currentProjectId(),
        scope: String? = null,
        query: String? = null
    ): Resource<List<MemoryItemDto>> =
        throw UnsupportedOperationException("readMemory is not configured")

    suspend fun writeMemory(
        projectId: String = currentProjectId(),
        content: String,
        scope: String = "project",
        category: String = "general",
        idempotencyKey: String? = null
    ): Resource<MemoryItemDto> =
        throw UnsupportedOperationException("writeMemory is not configured")

    suspend fun correctMemory(memoryId: String, content: String): Resource<MemoryItemDto> =
        throw UnsupportedOperationException("correctMemory is not configured")

    suspend fun deleteMemory(
        memoryId: String? = null,
        projectId: String? = null,
        scope: String? = null
    ): Resource<JsonMap> =
        throw UnsupportedOperationException("deleteMemory is not configured")

    suspend fun reconcileMemory(projectId: String = currentProjectId()): Resource<JsonMap> =
        throw UnsupportedOperationException("reconcileMemory is not configured")
}

interface AdminRepository : RepositoryContext {
    suspend fun fetchAnalytics(projectId: String): Resource<JsonMap> =
        throw UnsupportedOperationException("fetchAnalytics is not configured")

    suspend fun fetchAuditLogs(projectId: String): Resource<List<JsonMap>> =
        throw UnsupportedOperationException("fetchAuditLogs is not configured")

    suspend fun fetchNotifications(projectId: String): Resource<List<JsonMap>> =
        throw UnsupportedOperationException("fetchNotifications is not configured")

    suspend fun fetchAdminWorkflows(
        projectId: String = currentProjectId()
    ): Resource<List<JsonMap>> =
        throw UnsupportedOperationException("fetchAdminWorkflows is not configured")

    suspend fun fetchAdminWorkers(
        projectId: String = currentProjectId()
    ): Resource<List<JsonMap>> =
        throw UnsupportedOperationException("fetchAdminWorkers is not configured")

    suspend fun fetchAdminQueues(
        projectId: String = currentProjectId()
    ): Resource<JsonMap> =
        throw UnsupportedOperationException("fetchAdminQueues is not configured")

    suspend fun fetchLedgerSummary(
        projectId: String = currentProjectId()
    ): Resource<JsonMap> =
        throw UnsupportedOperationException("fetchLedgerSummary is not configured")

    suspend fun fetchAdminLedgerSummary(): Resource<JsonMap> =
        throw UnsupportedOperationException("fetchAdminLedgerSummary is not configured")

    suspend fun fetchPlatformManagedStatus(): Resource<JsonMap> = Resource.Empty

    suspend fun fetchReplicationStatus(): Resource<JsonMap> = Resource.Empty

    suspend fun fetchOwnerQuota(): Resource<JsonMap> = Resource.Empty

    suspend fun fetchOwnerTenants(): Resource<List<JsonMap>> = Resource.Empty

    suspend fun fetchOwnerTenantDatabases(
        status: String? = null,
        mode: String? = null,
        provider: String? = null,
        query: String? = null
    ): Resource<List<JsonMap>> = Resource.Empty

    suspend fun fetchOwnerTenantDatabaseAudit(tenantId: String, limit: Int = 20): Resource<List<JsonMap>> = Resource.Empty

    suspend fun recheckOwnerTenantDatabase(tenantId: String): Resource<JsonMap> = Resource.Empty

    suspend fun provisionOwnerTenantDatabase(tenantId: String): Resource<JsonMap> = Resource.Empty

    suspend fun fetchOwnerFeatureFlags(): Resource<JsonMap> = Resource.Empty

    suspend fun fetchTenantDatabase(): Resource<JsonMap> = Resource.Empty

    suspend fun testTenantDatabase(dsn: String): Resource<JsonMap> = Resource.Empty

    suspend fun connectTenantDatabase(dsn: String): Resource<JsonMap> = Resource.Empty

    suspend fun disconnectTenantDatabase(): Resource<JsonMap> = Resource.Empty

    suspend fun fetchTenantDatabaseHealth(): Resource<JsonMap> = Resource.Empty

    suspend fun migrateTenantProjects(projectIds: List<String>): Resource<JsonMap> = Resource.Empty

    suspend fun fetchTenantWallet(): Resource<JsonMap> = Resource.Empty

    suspend fun fetchOwnerTenantWallet(tenantId: String): Resource<JsonMap> = Resource.Empty

    suspend fun fetchReadiness(): Resource<JsonMap> =
        throw UnsupportedOperationException("fetchReadiness is not configured")

    suspend fun fetchStorageStatus(): Resource<JsonMap> =
        throw UnsupportedOperationException("fetchStorageStatus is not configured")

    suspend fun uploadStorageObject(
        key: String,
        filename: String,
        contentType: String,
        bytes: ByteArray
    ): Resource<JsonMap> =
        throw UnsupportedOperationException("uploadStorageObject is not configured")

    suspend fun fetchGpuInfrastructure(): Resource<JsonMap> =
        throw UnsupportedOperationException("fetchGpuInfrastructure is not configured")

    suspend fun saveGpuInfrastructure(payload: JsonMap): Resource<JsonMap> =
        throw UnsupportedOperationException("saveGpuInfrastructure is not configured")

    suspend fun fetchHealthDetail(): Resource<HealthResponseDto> =
        throw UnsupportedOperationException("fetchHealthDetail is not configured")

    suspend fun checkBackendHealth(): Boolean =
        throw UnsupportedOperationException("checkBackendHealth is not configured")

    suspend fun resolveConfiguredAiProviderBadge(): String =
        throw UnsupportedOperationException("resolveConfiguredAiProviderBadge is not configured")
}

interface AnimeRepositoryApi :
    AuthRepository,
    ProjectRepository,
    CharacterRepository,
    EpisodeRepository,
    MediaRepository,
    ProductionRepository,
    PromptRepository,
    MemoryRepository,
    AdminRepository
