package org.dpdns.bnai.anime.ui.batch2

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import java.lang.reflect.Proxy
import org.dpdns.bnai.anime.core.auth.TokenManager
import org.dpdns.bnai.anime.core.di.AppContainer
import org.dpdns.bnai.anime.core.model.Character
import org.dpdns.bnai.anime.core.model.Episode
import org.dpdns.bnai.anime.core.model.Project
import org.dpdns.bnai.anime.core.network.AnimeApiService
import org.dpdns.bnai.anime.core.network.AnimeRepositoryApi
import org.dpdns.bnai.anime.core.network.ApiCommandRecordDto
import org.dpdns.bnai.anime.core.network.ApprovalDecisionDto
import org.dpdns.bnai.anime.core.network.CharacterDto
import org.dpdns.bnai.anime.core.network.ClipDto
import org.dpdns.bnai.anime.core.network.EpisodeDto
import org.dpdns.bnai.anime.core.network.HealthResponseDto
import org.dpdns.bnai.anime.core.network.JobDto
import org.dpdns.bnai.anime.core.network.JsonMap
import org.dpdns.bnai.anime.core.network.ProjectDto
import org.dpdns.bnai.anime.core.network.ProjectOverviewDto
import org.dpdns.bnai.anime.core.network.Resource
import org.dpdns.bnai.anime.core.network.SceneDto
import org.dpdns.bnai.anime.core.network.SubmitCommandResultDto
import org.dpdns.bnai.anime.core.network.SystemPromptDto
import org.dpdns.bnai.anime.ui.AppViewModelFactory
import org.dpdns.bnai.anime.ui.LocalAppContainer

internal const val BATCH2_PROJECT_ID = "batch2-project"
private const val BATCH2_MEDIA_URL = "https://example.com/batch2-clip.mp4"

internal fun batch2FakeRepository(): AnimeRepositoryApi =
    Proxy.newProxyInstance(
        AnimeRepositoryApi::class.java.classLoader,
        arrayOf(AnimeRepositoryApi::class.java)
    ) { proxy, method, args ->
        when (method.name) {
            "toString" -> "Batch2FakeRepository"
            "hashCode" -> System.identityHashCode(proxy)
            "equals" -> proxy === args?.firstOrNull()
            "currentProjectId" -> BATCH2_PROJECT_ID
            "checkBackendHealth" -> true
            "resolveConfiguredAiProviderBadge" -> "Batch2 Provider"
            else -> Resource.Empty
        }
    } as AnimeRepositoryApi

internal fun batch2FakeApiService(): AnimeApiService =
    Proxy.newProxyInstance(
        AnimeApiService::class.java.classLoader,
        arrayOf(AnimeApiService::class.java)
    ) { proxy, method, args ->
        when (method.name) {
            "toString" -> "Batch2FakeApiService"
            "hashCode" -> System.identityHashCode(proxy)
            "equals" -> proxy === args?.firstOrNull()
            else -> null
        }
    } as AnimeApiService

internal class Batch2FakeContainer : AppContainer {
    override val tokenManager: TokenManager = TokenManager(null)
    override val apiService: AnimeApiService = batch2FakeApiService()
    override val repository: AnimeRepositoryApi = batch2FakeRepository()
    override val viewModelFactory = AppViewModelFactory(this)
}

internal fun ComposeContentTestRule.setBatch2Content(
    container: AppContainer = Batch2FakeContainer(),
    content: @Composable () -> Unit
) {
    setContent {
        CompositionLocalProvider(LocalAppContainer provides container) {
            MaterialTheme {
                content()
            }
        }
    }
    waitForIdle()
}


internal data class Batch2RecordedCall(
    val method: String,
    val args: List<Any?>
)

internal class Batch2Recorder {
    val calls = mutableListOf<Batch2RecordedCall>()

    fun lastCall(method: String): Batch2RecordedCall? =
        calls.lastOrNull { it.method == method }
}

internal class Batch2FakeState {
    var commandStatus: String = "SUCCEEDED"
    var commandResult: JsonMap = mapOf("media_url" to BATCH2_MEDIA_URL)
    var jobsStatus: String = "FAILED"
    var overviewActiveCommandId: String? = null
    var systemPrompts: List<SystemPromptDto> = listOf(
        SystemPromptDto(
            id = "prompt-1",
            project_id = BATCH2_PROJECT_ID,
            title = "Batch Prompt",
            prompt_text = "Original prompt",
            prompt_category = "BIBLE"
        )
    )
}

private fun batch2Job(fakeState: Batch2FakeState): JobDto =
    JobDto(
        id = "job-1",
        project_id = BATCH2_PROJECT_ID,
        intent = "SCENE_RENDER",
        status = fakeState.jobsStatus,
        episode_no = 1,
        episode_state = mapOf("current_state" to "Rendering"),
        pending_actions = listOf(mapOf("action" to "retry")),
        attempt_count = 1,
        max_attempts = 3,
        created_at = "2026-01-01T00:00:00Z"
    )

internal fun batch2RecordingRepository(
    recorder: Batch2Recorder,
    fakeState: Batch2FakeState = Batch2FakeState()
): AnimeRepositoryApi =
    Proxy.newProxyInstance(
        AnimeRepositoryApi::class.java.classLoader,
        arrayOf(AnimeRepositoryApi::class.java)
    ) { proxy, method, args ->
        val argList = args?.toList().orEmpty()
        when (method.name) {
            "toString" -> "Batch2RecordingRepository"
            "hashCode" -> System.identityHashCode(proxy)
            "equals" -> proxy === argList.firstOrNull()
            else -> {
                recorder.calls += Batch2RecordedCall(method.name, argList)
                when (method.name) {
                    "currentProjectId" -> BATCH2_PROJECT_ID
                    "checkBackendHealth" -> true
                    "resolveConfiguredAiProviderBadge" -> "Batch2 Provider"
                    "fetchProjects" -> Resource.Success(
                        listOf(
                            Project(
                                id = BATCH2_PROJECT_ID,
                                name = "Batch 2 Project",
                                activeEpisode = "Season 1 • Episode 1",
                                status = "ACTIVE"
                            )
                        )
                    )
                    "fetchEpisodes" -> Resource.Success(
                        listOf(
                            Episode(
                                id = "episode-1",
                                episodeNumber = 1,
                                title = "Pilot",
                                status = "READY",
                                synopsis = "First episode"
                            )
                        )
                    )
                    "fetchEpisodesRaw" -> Resource.Success(
                        listOf(
                            EpisodeDto(
                                id = "episode-1",
                                project_id = BATCH2_PROJECT_ID,
                                episode_no = 1,
                                title = "Pilot",
                                synopsis = "First episode",
                                current_state = "READY",
                                status = "READY",
                                qc_status = "APPROVED",
                                script_json = mapOf("raw_script" to "INT. STUDIO - DAY"),
                                master_video_url = BATCH2_MEDIA_URL
                            )
                        )
                    )
                    "fetchApprovals" -> Resource.Success(
                        listOf(
                            ApprovalDecisionDto(
                                id = "approval-1",
                                project_id = BATCH2_PROJECT_ID,
                                type = "QC",
                                resource_id = "scene-1",
                                resource_version = 3,
                                status = "PENDING",
                                reason = "Color continuity",
                                exact_diff = mapOf(
                                    "old_value" to "Blue eyes",
                                    "new_value" to "Green eyes"
                                )
                            )
                        )
                    )
                    "fetchSystemPrompts" -> Resource.Success(fakeState.systemPrompts)
                    "fetchGpuInfrastructure" -> Resource.Success(
                        mapOf(
                            "preferred_mode" to "BYOD_CLOUD",
                            "primary_llm_provider" to "groq",
                            "fallback_llm_provider" to "gemini",
                            "vision_provider" to "gemini",
                            "kaggle_username" to "",
                            "telegram_chat_id" to ""
                        )
                    )
                    "fetchProjectOverviewRaw" -> {
                        val overview = mutableMapOf<String, Any?>(
                            "project" to mapOf(
                                "id" to BATCH2_PROJECT_ID,
                                "name" to "Batch 2 Project",
                                "story_bible" to emptyMap<String, Any?>()
                            ),
                            "name" to "Batch 2 Project",
                            "genre" to "Action / Fantasy",
                            "visual_medium" to "ANIME",
                            "auto_approve" to false,
                            "cover_image_url" to "",
                            "budget_ceiling" to 1000,
                            "canon_status" to "LOCKED",
                            "continuity_status" to "CHECKED",
                            "episodes_count" to 1,
                            "characters_count" to 1,
                            "scenes_count" to 1,
                            "active_jobs_count" to 0,
                            "pending_approvals_count" to 1,
                            "pending_actions_count" to 0,
                            "counts" to mapOf(
                                "episodes_count" to 1,
                                "characters_count" to 1,
                                "scenes_count" to 1,
                                "active_jobs_count" to 0,
                                "pending_approvals_count" to 1,
                                "pending_actions_count" to 0
                            )
                        )
                        fakeState.overviewActiveCommandId?.let { cid ->
                            overview["status"] = mapOf("active_command_id" to cid)
                        }
                        Resource.Success(overview)
                    }
                    "fetchProjectOverview" -> Resource.Success(
                        ProjectOverviewDto(
                            project = ProjectDto(id = BATCH2_PROJECT_ID, name = "Batch 2 Project"),
                            stats = mapOf(
                                "total_episodes" to 1,
                                "total_characters" to 1,
                                "total_scenes" to 1,
                                "active_jobs_count" to 0
                            ),
                            episodes_count = 1,
                            characters_count = 1,
                            pending_approvals = 1,
                            active_jobs = 0,
                            canon_status = "LOCKED",
                            continuity_status = "CHECKED"
                        )
                    )
                    "fetchAdminLedgerSummary" -> Resource.Success(
                        mapOf(
                            "total_reserved" to 10,
                            "total_consumed" to 20,
                            "total_released" to 30
                        )
                    )
                    "fetchCharacters" -> Resource.Success(
                        listOf(
                            Character(
                                id = "CHAR_001",
                                name = "Aki",
                                role = "Protagonist",
                                visualDna = "Silver hair",
                                voiceId = "UNSET",
                                status = "alive"
                            )
                        )
                    )
                    "fetchCharactersRaw" -> Resource.Success(
                        listOf(
                            CharacterDto(
                                id = "char-1",
                                character_code = "CHAR_001",
                                name = "Aki",
                                role = "Protagonist",
                                canon_lock_status = "LOCKED",
                                visual_dna = mapOf("eye_color" to "Blue", "hair_style" to "Silver"),
                                voice_config = mapOf("provider" to "Svara TTS")
                            )
                        )
                    )
                    "fetchCharacterReferenceHistory" -> Resource.Success(
                        listOf(
                            mapOf(
                                "id" to "ref-1",
                                "image_url" to "https://example.com/ref.png",
                                "status" to "VERIFIED"
                            )
                        )
                    )
                    "fetchCategoryItems" -> Resource.Empty
                    "fetchCanon" -> Resource.Success(
                        listOf(
                            mapOf(
                                "id" to "canon-1",
                                "fact" to "The sky is red",
                                "locked" to true
                            )
                        )
                    )
                    "fetchCanonProposals" -> Resource.Success(
                        listOf(
                            mapOf(
                                "id" to "proposal-1",
                                "status" to "PENDING",
                                "exact_diff" to mapOf(
                                    "old_value" to "Blue eyes",
                                    "new_value" to "Green eyes"
                                )
                            )
                        )
                    )
                    "fetchContinuity" -> Resource.Success(
                        listOf(
                            mapOf(
                                "id" to "continuity-1",
                                "status" to "CRITICAL",
                                "issue" to "Dead character speaks",
                                "checked_at" to "2026-01-01T00:00:00Z"
                            )
                        )
                    )
                    "fetchJobs" -> Resource.Success(listOf(batch2Job(fakeState)))
                    "fetchJobDetail" -> Resource.Success(batch2Job(fakeState))
                    "fetchRetries" -> Resource.Success(
                        listOf(
                            mapOf(
                                "action_type" to "SCENE_RENDER",
                                "status" to "RETRY_SCHEDULED",
                                "attempt_count" to 1,
                                "max_attempts" to 3,
                                "ready_at" to "2026-01-01T00:00:00Z"
                            )
                        )
                    )
                    "fetchProjectMedia" -> Resource.Success(
                        mapOf(
                            "clips" to listOf(
                                mapOf(
                                    "id" to "clip-1",
                                    "scene_order" to 1,
                                    "clip_url" to BATCH2_MEDIA_URL,
                                    "status" to "READY"
                                )
                            ),
                            "character_refs" to listOf(
                                mapOf(
                                    "id" to "char-1",
                                    "name" to "Aki",
                                    "image_url" to "https://example.com/ref.png"
                                )
                            ),
                            "scenes" to listOf(
                                mapOf(
                                    "id" to "scene-1",
                                    "scene_order" to 1,
                                    "description" to "Opening scene",
                                    "status" to "READY"
                                )
                            )
                        )
                    )
                    "fetchNotifications" -> Resource.Success(
                        listOf(
                            mapOf(
                                "id" to "notification-1",
                                "action" to "RENDER_COMPLETE",
                                "new_value" to "Episode 1 rendered",
                                "created_at" to "2026-01-01T00:00:00Z"
                            )
                        )
                    )
                    "fetchAdminQueues" -> Resource.Success(
                        mapOf(
                            "outbox_pending" to 3,
                            "commands_running" to 2,
                            "dead_letter_count" to 1
                        )
                    )
                    "fetchAdminWorkers" -> Resource.Success(
                        listOf(
                            mapOf(
                                "worker_id" to "worker-1",
                                "provider" to "KAGGLE",
                                "status" to "ONLINE",
                                "current_model" to "anime-v1",
                                "last_heartbeat" to "2026-01-01T00:00:00Z"
                            )
                        )
                    )
                    "fetchAdminWorkflows" -> Resource.Success(
                        listOf(
                            mapOf(
                                "wf_key" to "WF-00",
                                "total_commands" to 4,
                                "failed_commands" to 0,
                                "n8n_live_active" to true
                            )
                        )
                    )
                    "fetchAuditLogs" -> Resource.Success(
                        listOf(
                            mapOf(
                                "id" to "audit-1",
                                "action" to "STATE_TRANSITION",
                                "old_value" to "QUEUED",
                                "new_value" to "RUNNING",
                                "created_at" to "2026-01-01T00:00:00Z"
                            )
                        )
                    )
                    "fetchAnalytics" -> Resource.Success(mapOf("total_episodes" to 1))
                    "fetchHealthDetail" -> Resource.Success(
                        HealthResponseDto(
                            ok = true,
                            database = "CONNECTED",
                            version = "1.0.0-studio"
                        )
                    )
                    "fetchScenesRaw" -> Resource.Success(
                        listOf(
                            SceneDto(
                                id = "scene-1",
                                episode_no = 1,
                                scene_order = 1,
                                description = "Opening scene",
                                visual_prompt = "Anime city at dusk",
                                status = "READY",
                                clips = listOf(
                                    ClipDto(
                                        id = "clip-1",
                                        scene_order = 1,
                                        clip_url = BATCH2_MEDIA_URL,
                                        status = "READY"
                                    )
                                )
                            )
                        )
                    )
                    "fetchBible" -> Resource.Success(
                        mapOf(
                            "premise" to "A hero rises",
                            "tone_genre" to "Action / Fantasy",
                            "world_setting" to "Neo Tokyo",
                            "world_rules" to "No time travel",
                            "power_system" to "Spirit energy",
                            "character_foundations" to "Aki",
                            "timeline" to "Season 1"
                        )
                    )
                    "fetchCommandStatus" -> Resource.Success(
                        ApiCommandRecordDto(
                            id = argList.getOrNull(0) as? String ?: "cmd-batch2",
                            status = fakeState.commandStatus,
                            result = fakeState.commandResult
                        )
                    )
                    "submitQuickImage", "submitQuickVideo" -> Resource.Success(
                        SubmitCommandResultDto(status = "QUEUED", command_id = "quick-cmd")
                    )
                    "cancelJobSync" -> Resource.Success(emptyMap<String, Any?>())
                    "updateBudgetLimit" -> Resource.Success(mapOf("budget_ceiling" to argList.lastOrNull()))
                    "updateSceneSync" -> Resource.Success(
                        SceneDto(id = argList.getOrNull(2) as? String ?: "scene-1")
                    )
                    "deleteSystemPrompt" -> Resource.Success(emptyMap<String, Any?>())
                    "createProjectSync" -> Resource.Success(
                        ProjectDto(
                            id = "created-project",
                            name = argList.getOrNull(0) as? String ?: "Created Project"
                        )
                    )
                    "saveGpuInfrastructure" -> Resource.Success(
                        argList.getOrNull(0) as? Map<String, Any?> ?: emptyMap()
                    )
                    "updateProjectSettings" -> {
                        val updates = argList.getOrNull(1) as? Map<String, Any?>
                        if (updates != null) {
                            Resource.Success(updates)
                        } else {
                            Resource.Success(
                                ProjectDto(
                                    id = argList.getOrNull(0) as? String ?: BATCH2_PROJECT_ID,
                                    name = "Batch 2 Project"
                                )
                            )
                        }
                    }
                    "submitApprovalDecision" -> Resource.Success(
                        ApprovalDecisionDto(
                            id = argList.getOrNull(1) as? String ?: "approval-1",
                            status = argList.getOrNull(2) as? String
                        )
                    )
                    "saveSystemPrompt" -> Resource.Success(
                        SystemPromptDto(
                            id = "saved-prompt",
                            project_id = argList.getOrNull(0) as? String,
                            title = argList.getOrNull(1) as? String ?: "Saved Prompt",
                            prompt_text = argList.getOrNull(2) as? String ?: "",
                            prompt_category = argList.getOrNull(4) as? String
                        )
                    )
                    "updateSystemPrompt" -> Resource.Success(
                        SystemPromptDto(
                            id = argList.getOrNull(0) as? String ?: "prompt-1",
                            title = argList.getOrNull(1) as? String ?: "Updated Prompt",
                            prompt_text = argList.getOrNull(2) as? String ?: "",
                            visual_medium = argList.getOrNull(3) as? String ?: "ANIME",
                            prompt_category = argList.getOrNull(4) as? String
                        )
                    )
                    "submitStudioCommand" -> Resource.Success(
                        SubmitCommandResultDto(status = "QUEUED", command_id = "cmd-batch2")
                    )
                    "createCharacterSync" -> Resource.Success(
                        CharacterDto(
                            id = "char-created",
                            name = argList.getOrNull(1) as? String
                        )
                    )
                    "updateProjectBible" -> Resource.Success(
                        argList.getOrNull(1) as? Map<String, Any?> ?: emptyMap()
                    )
                    "updateEpisodeSync" -> Resource.Success(
                        EpisodeDto(
                            id = "episode-1",
                            episode_no = argList.getOrNull(1) as? Int ?: 1
                        )
                    )
                    else -> when {
                        method.returnType == java.lang.Boolean.TYPE -> false
                        method.returnType == java.lang.Integer.TYPE -> 0
                        method.returnType == java.lang.Long.TYPE -> 0L
                        method.returnType == String::class.java -> ""
                        Resource::class.java.isAssignableFrom(method.returnType) -> Resource.Empty
                        else -> null
                    }
                }
            }
        }
    } as AnimeRepositoryApi

internal class Batch2RecordingContainer(
    val recorder: Batch2Recorder = Batch2Recorder(),
    val fakeState: Batch2FakeState = Batch2FakeState(),
    override val tokenManager: TokenManager = TokenManager(null)
) : AppContainer {
    override val apiService: AnimeApiService = batch2FakeApiService()
    override val repository: AnimeRepositoryApi = batch2RecordingRepository(recorder, fakeState)
    override val viewModelFactory = AppViewModelFactory(this)
}
