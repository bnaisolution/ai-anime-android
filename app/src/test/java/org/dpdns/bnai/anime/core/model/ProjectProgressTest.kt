package org.dpdns.bnai.anime.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ProjectProgressTest {

    @Test
    fun episodeZeroMeansNotStarted() {
        assertEquals("Not started", projectProgressLabel(1, 0))
        assertEquals("Not started", projectProgressLabel(null, null))
    }

    @Test
    fun seasonAndEpisodeComeFromDatabaseValues() {
        assertEquals("Season 2 • Episode 5", projectProgressLabel(2, 5))
        assertEquals("Episode 3", projectProgressLabel(0, 3))
        assertEquals("Episode 1", projectProgressLabel(null, 1))
    }
}
