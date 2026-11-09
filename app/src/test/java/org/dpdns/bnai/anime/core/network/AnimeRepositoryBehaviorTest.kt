package org.dpdns.bnai.anime.core.network

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.dpdns.bnai.anime.core.auth.TokenManager
import org.dpdns.bnai.anime.core.model.Character
import org.dpdns.bnai.anime.core.model.Project
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AnimeRepositoryBehaviorTest {
    private lateinit var server: MockWebServer
    private lateinit var tokenManager: TokenManager
    private lateinit var repository: AnimeRepository

    @Before
    fun setUp() {
        TokenManager.resetForTests()
        server = MockWebServer()
        server.start()
        tokenManager = TokenManager(null).apply { saveToken("behavior-jwt") }
        repository = AnimeRepository(
            RetrofitClient.create(server.url("/").toString(), tokenManager),
            tokenManager
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
        TokenManager.resetForTests()
    }

    @Test
    fun submitQuickVideoForwardsRequestedDuration() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"data":{"status":"QUEUED","command_id":"cmd-video"}}""")
        )

        val result = repository.submitQuickVideo(
            projectId = "project-1",
            prompt = "A cinematic rooftop chase",
            durationSec = 12
        )

        val recorded = server.takeRequest()
        val body = recorded.body.readUtf8()
        assertTrue(result is Resource.Success<*>)
        val success = result as Resource.Success<SubmitCommandResultDto>
        assertEquals("cmd-video", success.data.command_id)
        assertEquals("/v1/anime/projects/project-1/quick/video", recorded.path)
        assertTrue(body.contains("\"duration_sec\":12"))
        assertTrue(body.contains("\"prompt\":\"A cinematic rooftop chase\""))
    }

    @Test
    fun submitQuickVideoUsesFiveSecondDefaultWhenOmitted() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"data":{"status":"QUEUED","command_id":"cmd-default"}}""")
        )

        repository.submitQuickVideo(
            projectId = "project-1",
            prompt = "Default duration"
        )

        assertTrue(server.takeRequest().body.readUtf8().contains("\"duration_sec\":5"))
    }

    @Test
    fun emptySystemPromptListMapsToEmptyResource() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"data":[]}""")
        )

        val result = repository.fetchSystemPrompts("project-1")

        assertEquals(Resource.Empty, result)
    }

    @Test
    fun nullSystemPromptPayloadMapsToEmptyResource() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"data":null}""")
        )

        val result = repository.fetchSystemPrompts("project-1")

        assertEquals(Resource.Empty, result)
    }

    @Test
    fun populatedSystemPromptListMapsToSuccess() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """{"data":[{"id":"prompt-1","title":"Bible","prompt_text":"Write it","prompt_category":"BIBLE","visual_medium":"ANIME"}]}"""
                )
        )

        val result = repository.fetchSystemPrompts("project-1")

        assertTrue(result is Resource.Success<*>)
        val prompt = (result as Resource.Success<List<SystemPromptDto>>).data.single()
        assertEquals("prompt-1", prompt.id)
        assertEquals("Bible", prompt.title)
    }

    @Test
    fun nullVoiceIdDoesNotLeakLiteralNullString() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """{"data":[{"id":"char-1","character_code":"CHAR_001","name":"Aki","voice_config":{"voice_id":null}}]}"""
                )
        )

        val result = repository.fetchCharacters("project-1")

        assertTrue(result is Resource.Success<*>)
        assertEquals("UNSET", (result as Resource.Success<List<Character>>).data.single().voiceId)
    }

    @Test
    fun firstProjectBecomesActiveWhenNoneIsSelected() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """{"data":[{"id":"project-1","name":"First"},{"id":"project-2","name":"Second"}]}"""
                )
        )

        val result = repository.fetchProjects()

        assertTrue(result is Resource.Success<*>)
        assertEquals("project-1", tokenManager.getActiveProjectId())
        assertEquals(2, (result as Resource.Success<List<Project>>).data.size)
    }

    @Test
    fun projectHttpFailureMapsToErrorWithStatusCode() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(503))

        val result = repository.fetchProjects()

        assertTrue(result is Resource.Error)
        assertEquals(503, (result as Resource.Error).code)
    }

    @Test
    fun saveSystemPromptForwardsSelectedMediumAndCategory() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(201)
                .setBody(
                    """{"data":{"id":"prompt-2","title":"Image","prompt_text":"Keyframe","prompt_category":"IMAGE","visual_medium":"MANHWA"}}"""
                )
        )

        val result = repository.saveSystemPrompt(
            projectId = "project-1",
            title = "Image",
            promptText = "Keyframe",
            visualMedium = "MANHWA",
            promptCategory = "IMAGE"
        )

        val recorded = server.takeRequest()
        val body = recorded.body.readUtf8()
        assertTrue(result is Resource.Success<*>)
        assertEquals("POST", recorded.method)
        assertEquals("/v1/anime/prompts", recorded.path)
        assertTrue(body.contains("\"visual_medium\":\"MANHWA\""))
        assertTrue(body.contains("\"prompt_category\":\"IMAGE\""))
    }

    @Test
    fun updateSystemPromptUsesPatchAndMapsVersionedResponse() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """{"data":{"id":"prompt-1","title":"Renamed","prompt_text":"Updated","prompt_category":"BIBLE","visual_medium":"ANIME","is_favorite":true,"version":2}}"""
                )
        )

        val result = repository.updateSystemPrompt(
            promptId = "prompt-1",
            title = "Renamed",
            promptText = "Updated",
            isFavorite = true
        )

        val recorded = server.takeRequest()
        val body = recorded.body.readUtf8()
        assertEquals("PATCH", recorded.method)
        assertEquals("/v1/anime/prompts/prompt-1", recorded.path)
        assertTrue(body.contains("\"title\":\"Renamed\""))
        assertTrue(body.contains("\"is_favorite\":true"))
        assertEquals(2, (result as Resource.Success<SystemPromptDto>).data.version)
    }

    @Test
    fun togglePromptFavoritePostsToFavoritePath() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """{"data":{"id":"prompt-1","title":"Bible","prompt_text":"Write","prompt_category":"BIBLE","visual_medium":"ANIME","is_favorite":true,"version":1}}"""
                )
        )

        val result = repository.togglePromptFavorite("prompt-1")

        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/v1/anime/prompts/prompt-1/favorite", recorded.path)
        assertTrue((result as Resource.Success<SystemPromptDto>).data.is_favorite)
    }
}
