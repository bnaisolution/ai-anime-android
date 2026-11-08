package org.dpdns.bnai.anime.core.network

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test

class AnimeRepositoryApiContractTest {
    private val unimplemented = object : AnimeRepositoryApi {}

    @Test
    fun compositeContractExposesAllFeatureContracts() {
        assertTrue(unimplemented is ProjectRepository)
        assertTrue(unimplemented is CharacterRepository)
        assertTrue(unimplemented is EpisodeRepository)
        assertTrue(unimplemented is MediaRepository)
        assertTrue(unimplemented is ProductionRepository)
        assertTrue(unimplemented is PromptRepository)
        assertTrue(unimplemented is AdminRepository)
    }

    @Test
    fun unconfiguredContextFailsFast() {
        assertThrows(UnsupportedOperationException::class.java) {
            unimplemented.currentProjectId()
        }
    }

    @Test
    fun unconfiguredOperationsFailFast() {
        assertThrows(UnsupportedOperationException::class.java) {
            runBlocking { unimplemented.fetchProjects() }
        }
        assertThrows(UnsupportedOperationException::class.java) {
            runBlocking { unimplemented.checkBackendHealth() }
        }
    }
}
