package org.dpdns.bnai.anime.core.network

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.dpdns.bnai.anime.core.auth.TokenManager
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
    fun expiredAccessTokenIsRefreshedAndRetriedOnce() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setBody("""{"error":{"code":"AUTH_INVALID"}}""")
        )
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"token":"new-access","refresh_token":"new-refresh","expires_in":3600,"user_id":"user-1","role":"owner_admin"}""")
        )
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"data":[]}""")
        )
        val tokenManager = TokenManager(null).apply {
            saveSession("old-access", "old-refresh", 1, "user-1", role = "normal_user")
        }
        val api = RetrofitClient.create(
            baseUrl = server.url("/").toString(),
            tokenManager = tokenManager
        )

        val response = api.getProjects()

        assertTrue(response.isSuccessful)
        assertEquals("/v1/anime/projects", server.takeRequest().path)
        val refreshRequest = server.takeRequest()
        assertEquals("/api/v1/auth/refresh", refreshRequest.path)
        assertNotNull(refreshRequest.body.readUtf8().takeIf { it.contains("old-refresh") })
        val retried = server.takeRequest()
        assertEquals("Bearer new-access", retried.getHeader("Authorization"))
        assertEquals("new-access", tokenManager.getToken())
        assertEquals("new-refresh", tokenManager.getRefreshToken())
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
