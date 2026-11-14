package org.dpdns.bnai.anime.ui

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.dpdns.bnai.anime.core.auth.TokenManager
import org.dpdns.bnai.anime.core.model.Character
import org.dpdns.bnai.anime.core.model.Episode
import org.dpdns.bnai.anime.core.model.Project
import org.dpdns.bnai.anime.core.network.AnimeApiService
import org.dpdns.bnai.anime.core.network.AnimeRepositoryApi
import org.dpdns.bnai.anime.core.network.ApiCommandRecordDto
import org.dpdns.bnai.anime.core.network.ApiEnvelope
import org.dpdns.bnai.anime.core.network.ApprovalDecisionDto
import org.dpdns.bnai.anime.core.network.JobDto
import org.dpdns.bnai.anime.core.network.LoginResponse
import org.dpdns.bnai.anime.core.network.ProjectDto
import org.dpdns.bnai.anime.core.network.Resource
import org.dpdns.bnai.anime.core.network.SystemPromptDto
import org.dpdns.bnai.anime.ui.auth.AuthViewModel
import org.dpdns.bnai.anime.ui.character.CharacterViewModel
import org.dpdns.bnai.anime.ui.production.ProductionViewModel
import org.dpdns.bnai.anime.ui.project.ProjectViewModel
import org.dpdns.bnai.anime.ui.project.StoryBibleData
import org.dpdns.bnai.anime.ui.studio.PromptStudioViewModel
import org.dpdns.bnai.anime.ui.studio.StudioViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelBehaviorTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        TokenManager.resetForTests()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        TokenManager.resetForTests()
    }

    @Test
    fun authRejectsBlankCredentialsWithoutCallingApi() = runTest {
        val api = mockk<AnimeApiService>(relaxed = true)
        val viewModel = AuthViewModel(api, TokenManager(null))

        viewModel.login("   ", "")

        assertTrue(viewModel.authState.value is Resource.Error)
        coVerify(exactly = 0) { api.login(any()) }
    }

    @Test
    fun authSuccessPersistsSessionToken() = runTest {
        val api = mockk<AnimeApiService>(relaxed = true)
        val tokenManager = TokenManager(null)
        coEvery { api.login(any()) } returns Response.success(
            LoginResponse(
                token = "jwt-1",
                refreshToken = "refresh-1",
                expiresIn = 60,
                userId = "user-1",
                workspaceId = "workspace-1",
                email = "owner@test.invalid",
                role = "owner"
            )
        )
        val viewModel = AuthViewModel(api, tokenManager)

        viewModel.login(" owner@test.invalid ", "secret")
        advanceUntilIdle()

        assertEquals("jwt-1", tokenManager.getToken())
        assertEquals("refresh-1", tokenManager.getRefreshToken())
        assertTrue(viewModel.authState.value is Resource.Success)
    }

    @Test
    fun authHttpFailureMapsToErrorWithStatusCode() = runTest {
        val api = mockk<AnimeApiService>(relaxed = true)
        coEvery { api.login(any()) } returns Response.error(
            401,
            """{"status":"failed"}""".toResponseBody("application/json".toMediaType())
        )
        val viewModel = AuthViewModel(api, TokenManager(null))

        viewModel.login("owner@test.invalid", "wrong")
        advanceUntilIdle()

        val error = viewModel.authState.value as Resource.Error
        assertEquals(401, error.code)
    }

    @Test
    fun studioLoadsProjectsAndHomeMetrics() = runTest {
        val repository = mockk<AnimeRepositoryApi>(relaxed = true)
        val tokenManager = TokenManager(null)
        coEvery { repository.fetchProjects() } returns Resource.Success(
            listOf(Project("project-1", "Alpha", "", "ACTIVE"))
        )
        coEvery { repository.currentProjectId() } returns ""
        coEvery { repository.fetchEpisodes("project-1") } returns Resource.Success(
            listOf(Episode(id = "episode-1", episodeNumber = 1, title = "Pilot", status = "DONE"))
        )
        coEvery { repository.fetchApprovals("project-1") } returns Resource.Success(
            listOf(ApprovalDecisionDto(id = "approval-1", status = "PENDING"))
        )
        coEvery { repository.fetchJobs("project-1") } returns Resource.Success(
            listOf(JobDto(id = "job-1", status = "RUNNING"))
        )

        val viewModel = StudioViewModel(repository, tokenManager)
        advanceUntilIdle()

        assertEquals("project-1", tokenManager.getActiveProjectId())
        assertEquals("Alpha", viewModel.homeSummary.value.activeProjectName)
        assertEquals(1, viewModel.homeSummary.value.recentEpisodes.size)
        assertEquals(1, viewModel.homeSummary.value.pendingApprovalsCount)
        assertEquals("RUNNING", viewModel.homeSummary.value.activeProductionStatus)
    }

    @Test
    fun studioCreateProjectSavesActiveProjectAndMessage() = runTest {
        val repository = mockk<AnimeRepositoryApi>(relaxed = true)
        val tokenManager = TokenManager(null)
        coEvery { repository.fetchProjects() } returns Resource.Success(emptyList())
        coEvery { repository.createProjectSync("New Project", emptyList()) } returns Resource.Success(
            ProjectDto(id = "project-2", name = "New Project")
        )

        val viewModel = StudioViewModel(repository, tokenManager)
        advanceUntilIdle()
        viewModel.createNewProject(" New Project ")
        advanceUntilIdle()

        assertEquals("project-2", tokenManager.getActiveProjectId())
        assertTrue(viewModel.homeSummary.value.createStatusMessage!!.contains("created"))
    }

    @Test
    fun projectRejectsMissingActiveProject() = runTest {
        val repository = mockk<AnimeRepositoryApi>(relaxed = true)
        coEvery { repository.currentProjectId() } returns ""
        val viewModel = ProjectViewModel(repository)

        viewModel.fetchOverview("")

        assertTrue(viewModel.overviewState.value is Resource.Error)
        coVerify(exactly = 0) { repository.fetchProjectOverview(any()) }
    }

    @Test
    fun projectBibleMapsNestedStoryBibleFields() = runTest {
        val repository = mockk<AnimeRepositoryApi>(relaxed = true)
        coEvery { repository.fetchBible("project-1") } returns Resource.Success(
            mapOf(
                "story_bible" to mapOf(
                    "title" to "Ashes of Heaven",
                    "world_overview" to "A broken celestial order.",
                    "magic_system" to "Breath-bound runes.",
                    "tone" to "Dark mythic"
                )
            )
        )
        val viewModel = ProjectViewModel(repository)

        viewModel.fetchBible("project-1")
        advanceUntilIdle()

        val bible = (viewModel.bibleState.value as Resource.Success<StoryBibleData>).data
        assertEquals("Ashes of Heaven", bible.universeTitle)
        assertEquals("A broken celestial order.", bible.worldRules)
        assertEquals("Breath-bound runes.", bible.magicSystem)
        assertEquals("Dark mythic", bible.toneAndTheme)
    }

    @Test
    fun characterRejectsMissingProjectAndLoadsVoiceCatalog() = runTest {
        val repository = mockk<AnimeRepositoryApi>(relaxed = true)
        val viewModel = CharacterViewModel(repository)

        viewModel.loadCharacters("")
        assertTrue(viewModel.charactersState.value is Resource.Error)

        coEvery { repository.fetchCharacters("project-1") } returns Resource.Success(
            listOf(Character("character-1", "Aki", "lead", "", "UNSET"))
        )
        coEvery { repository.fetchCategoryItems("project-1", "voices") } returns Resource.Success(
            listOf(mapOf("voice_id" to "voice-1"), mapOf("name" to "voice-2"))
        )

        viewModel.loadCharacters("project-1")
        advanceUntilIdle()

        assertTrue(viewModel.charactersState.value is Resource.Success<*>)
        assertEquals(listOf("voice-1", "voice-2"), viewModel.voiceCatalog.value)
    }

    @Test
    fun productionNonCommandIdUsesLatestJobStatus() = runTest {
        val repository = mockk<AnimeRepositoryApi>(relaxed = true)
        coEvery { repository.currentProjectId() } returns "project-1"
        coEvery { repository.fetchEpisodes("project-1") } returns Resource.Success(
            listOf(Episode(id = "episode-2", episodeNumber = 2, title = "The Gate", status = "DONE"))
        )
        coEvery { repository.fetchJobs("project-1") } returns Resource.Success(
            listOf(JobDto(id = "job-1", status = "RUNNING", episode_no = 2))
        )
        val viewModel = ProductionViewModel(repository)

        viewModel.startMonitoring("not-a-command-uuid")
        advanceUntilIdle()

        assertEquals("RUNNING", viewModel.state.value.status)
        assertTrue(viewModel.state.value.episodeTitle.contains("Episode 2"))
    }

    @Test
    fun productionCommandIdPollsUntilTerminalStatus() = runTest {
        val repository = mockk<AnimeRepositoryApi>(relaxed = true)
        val commandId = "12345678-1234-1234-1234-123456789012"
        coEvery { repository.currentProjectId() } returns "project-1"
        coEvery { repository.fetchEpisodes("project-1") } returns Resource.Success(emptyList())
        coEvery { repository.fetchCommandStatus(commandId) } returns Resource.Success(
            ApiCommandRecordDto(
                id = commandId,
                status = "SUCCEEDED",
                payload = mapOf("title" to "Finale", "episode_no" to 3)
            )
        )
        val viewModel = ProductionViewModel(repository)

        viewModel.startMonitoring(commandId)
        advanceUntilIdle()

        assertEquals("SUCCEEDED", viewModel.state.value.status)
        assertEquals(100, viewModel.state.value.progressPercent)
        assertTrue(viewModel.state.value.episodeTitle.contains("Episode 3"))
    }

    @Test
    fun promptStudioLoadsDefaultPromptForActiveProject() = runTest {
        val api = mockk<AnimeApiService>(relaxed = true)
        coEvery { api.getSystemPrompts("project-1") } returns Response.success(
            ApiEnvelope(
                listOf(
                    SystemPromptDto(id = "prompt-1", title = "Default", prompt_text = "Write", is_default = true, visual_medium = "MANHWA"),
                    SystemPromptDto(id = "prompt-2", title = "Other", prompt_text = "Alt")
                )
            )
        )
        val viewModel = PromptStudioViewModel(api) { "project-1" }

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("prompt-1", viewModel.uiState.value.selectedPrompt?.id)
        assertEquals("MANHWA", viewModel.uiState.value.selectedVisualMedium)
        assertEquals(2, viewModel.uiState.value.prompts.size)
    }

    @Test
    fun promptStudioMapsHttpFailureToStatusMessage() = runTest {
        val api = mockk<AnimeApiService>(relaxed = true)
        coEvery { api.getSystemPrompts("project-1") } returns Response.error(
            500,
            """{"status":"failed"}""".toResponseBody("application/json".toMediaType())
        )
        val viewModel = PromptStudioViewModel(api) { "project-1" }

        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.statusMessage!!.contains("500"))
    }

    @Test
    fun promptStudioIgnoresBlankCreateAndAddsSuccessfulPrompt() = runTest {
        val api = mockk<AnimeApiService>(relaxed = true)
        coEvery { api.getSystemPrompts("project-1") } returns Response.success(ApiEnvelope(emptyList()))
        coEvery { api.createSystemPrompt(any()) } returns Response.success(
            ApiEnvelope(SystemPromptDto(id = "prompt-3", title = "New", prompt_text = "Text"))
        )
        val viewModel = PromptStudioViewModel(api) { "project-1" }
        advanceUntilIdle()

        viewModel.createAndActivatePrompt(" ", "Text")
        coVerify(exactly = 0) { api.createSystemPrompt(any()) }

        viewModel.createAndActivatePrompt("New", "Text")
        advanceUntilIdle()

        assertEquals("prompt-3", viewModel.uiState.value.selectedPrompt?.id)
        assertEquals("prompt-3", viewModel.uiState.value.prompts.first().id)
    }
}
