package org.dpdns.bnai.anime.core.runtime

import org.junit.Assert.assertEquals
import org.junit.Test

class RuntimeContractTest {
    @Test
    fun oneMinuteContractScalesDown() {
        val contract = RuntimeContractCalculator.build(1)
        assertEquals(60, contract.targetDurationSec)
        assertEquals(132, contract.dialogueWordBudget)
        assertEquals(1, contract.actCount)
        assertEquals("FULL_MOTION", contract.renderTier)
    }

    @Test
    fun thirtyMinuteContractScalesUp() {
        val contract = RuntimeContractCalculator.build(30)
        assertEquals(1800, contract.targetDurationSec)
        assertEquals(3960, contract.dialogueWordBudget)
        assertEquals(6, contract.actCount)
        assertEquals("HYBRID", contract.renderTier)
    }

    @Test
    fun durationIsClampedToSupportedRange() {
        assertEquals(1, RuntimeContractCalculator.build(0).targetDurationMin)
        assertEquals(30, RuntimeContractCalculator.build(90).targetDurationMin)
    }
}
