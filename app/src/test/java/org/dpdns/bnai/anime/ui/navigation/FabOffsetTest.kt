package org.dpdns.bnai.anime.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FabOffsetTest {
    @Test
    fun offsetIsClampedInsideTheScreen() {
        val bounded = clampFabOffset(xDp = -900f, yDp = 500f, maxLeftDp = 320f, maxUpDp = 640f)
        assertEquals(-320f, bounded.xDp)
        assertEquals(0f, bounded.yDp)
    }

    @Test
    fun negativeDragMovesTheFabUpAndLeft() {
        val bounded = clampFabOffset(xDp = -80f, yDp = -120f, maxLeftDp = 320f, maxUpDp = 640f)
        assertEquals(-80f, bounded.xDp)
        assertEquals(-120f, bounded.yDp)
    }

    @Test
    fun invalidBoundsNeverProducePositiveOffsets() {
        val bounded = clampFabOffset(xDp = 10f, yDp = 10f, maxLeftDp = -5f, maxUpDp = -5f)
        assertEquals(0f, bounded.xDp)
        assertEquals(0f, bounded.yDp)
    }

    @Test
    fun globalFabIsHiddenOnlyOnFullScreenAiAndStudioRoutes() {
        assertFalse(isGlobalFabVisible("ai_brain"))
        assertFalse(isGlobalFabVisible("studio"))
        assertTrue(isGlobalFabVisible("project_detail"))
        assertTrue(isGlobalFabVisible("production"))
    }
}
