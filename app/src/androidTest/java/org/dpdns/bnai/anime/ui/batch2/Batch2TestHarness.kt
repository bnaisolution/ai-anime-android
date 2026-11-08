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
import org.dpdns.bnai.anime.core.network.ApprovalDecisionDto
import org.dpdns.bnai.anime.core.network.CharacterDto
import org.dpdns.bnai.anime.core.network.EpisodeDto
import org.dpdns.bnai.anime.core.network.ProjectDto
import org.dpdns.bnai.anime.core.network.Resource
import org.dpdns.bnai.anime.core.network.SubmitCommandResultDto
import org.dpdns.bnai.anime.core.network.SystemPromptDto
import org.dpdns.bnai.anime.ui.AppViewModelFactory
import org.dpdns.bnai.anime.ui.LocalAppContainer

internal const val BATCH2_PROJECT_ID = "batch2-project"

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

internal fun batch2RecordingRepository(recorder: Batch2Recorder): AnimeRepositoryApi =
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
                                status = "READY"
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
                    "fetchSystemPrompts" -> Resource.Success(
                        listOf(
                            SystemPromptDto(
                                id = "prompt-1",
                                project_id = BATCH2_PROJECT_ID,
                                title = "Batch Prompt",
                                prompt_text = "Original prompt",
                                prompt_category = "BIBLE"
                            )
                        )
                    )
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
                    "fetchProjectOverviewRaw" -> Resource.Success(
                        mapOf(
                            "project" to mapOf(
                                "id" to BATCH2_PROJECT_ID,
                                "name" to "Batch 2 Project",
                                "story_bible" to emptyMap<String, Any?>()
                            ),
                            "name" to "Batch 2 Project",
                            "genre" to "Action / Fantasy",
                            "visual_medium" to "ANIME",
                            "auto_approve" to false,
                            "cover_image_url" to ""
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
                    "fetchJobs", "fetchCommandStatus", "fetchCategoryItems" -> Resource.Empty
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
    val recorder: Batch2Recorder = Batch2Recorder()
) : AppContainer {
    override val tokenManager: TokenManager = TokenManager(null)
    override val apiService: AnimeApiService = batch2FakeApiService()
    override val repository: AnimeRepositoryApi = batch2RecordingRepository(recorder)
    override val viewModelFactory = AppViewModelFactory(this)
}
