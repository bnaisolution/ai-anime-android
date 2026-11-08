package org.dpdns.bnai.anime.core.network

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.dpdns.bnai.anime.core.auth.TokenManager
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RetrofitClientIsolationTest {
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        TokenManager.resetForTests()
        server = MockWebServer()
    }

    @After
    fun tearDown() {
        server.shutdown()
        TokenManager.resetForTests()
    }

    @Test
    fun createUsesInjectedBaseUrlAndTokenManager() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"data":[]}""")
        )
        val tokenManager = TokenManager(null).apply { saveToken("test-jwt") }
        val api = RetrofitClient.create(
            baseUrl = server.url("/").toString(),
            tokenManager = tokenManager
        )

        val response = api.getProjects()
        val recorded = server.takeRequest()

        assertTrue(response.isSuccessful)
        assertEquals("/v1/anime/projects", recorded.path)
        assertEquals("Bearer test-jwt", recorded.getHeader("Authorization"))
    }
}
