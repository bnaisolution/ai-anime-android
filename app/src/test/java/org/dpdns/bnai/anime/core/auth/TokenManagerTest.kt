package org.dpdns.bnai.anime.core.auth

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class TokenManagerTest {
    private lateinit var manager: TokenManager

    @Before
    fun setUp() {
        manager = TokenManager(null)
        manager.clearToken()
        TokenManager.cachedProjectId = null
    }

    @After
    fun tearDown() {
        manager.clearToken()
        TokenManager.cachedProjectId = null
    }

    @Test
    fun tokenIsCachedWithoutAndroidContext() {
        manager.saveToken("test.jwt.token")
        assertEquals("test.jwt.token", manager.getToken())
        manager.clearToken()
        assertNull(manager.getToken())
    }

    @Test
    fun blankProjectIdIsNeverPersistedOrCached() {
        manager.saveActiveProjectId("   ")
        assertEquals("", manager.getActiveProjectId())
    }

    @Test
    fun validProjectIdIsCachedAcrossManagerInstances() {
        val projectId = "11111111-1111-4111-8111-111111111111"
        manager.saveActiveProjectId(projectId)
        val other = TokenManager(null)
        assertEquals(projectId, other.getActiveProjectId())
    }

    @Test
    fun sessionExpiryMessageCanBeConsumedOnce() {
        manager.notifySessionExpired("expired")
        assertEquals("expired", manager.sessionExpiredMessage.value)
        manager.consumeSessionExpiredMessage()
        assertNull(manager.sessionExpiredMessage.value)
    }
}
