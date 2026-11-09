package org.dpdns.bnai.anime.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST

class AnimeApiContractTest {
    private fun method(name: String) = AnimeApiService::class.java.methods.first { it.name == name }

    @Test
    fun createProjectUsesTheCanonicalPostPath() {
        val annotation = method("createProjectSync").getAnnotation(POST::class.java)
        assertNotNull(annotation)
        assertEquals("v1/anime/projects", annotation.value)
    }

    @Test
    fun authRefreshUsesTheCanonicalPostPath() {
        val annotation = method("refresh").getAnnotation(POST::class.java)
        assertNotNull(annotation)
        assertEquals("api/v1/auth/refresh", annotation.value)
    }

    @Test
    fun authLogoutUsesTheCanonicalPostPath() {
        val annotation = method("logout").getAnnotation(POST::class.java)
        assertNotNull(annotation)
        assertEquals("api/v1/auth/logout", annotation.value)
    }

    @Test
    fun projectsAreReadFromTheCanonicalGetPath() {
        val annotation = method("getProjects").getAnnotation(GET::class.java)
        assertNotNull(annotation)
        assertEquals("v1/anime/projects", annotation.value)
    }

    @Test
    fun commandSubmissionUsesTheCanonicalPostPath() {
        val annotation = method("submitCommand").getAnnotation(POST::class.java)
        assertNotNull(annotation)
        assertEquals("v1/anime/commands", annotation.value)
    }

    @Test
    fun projectPatchUsesTheCanonicalPath() {
        val annotation = method("updateProjectSettings").getAnnotation(PATCH::class.java)
        assertNotNull(annotation)
        assertEquals("v1/anime/projects/{id}", annotation.value)
    }
}
