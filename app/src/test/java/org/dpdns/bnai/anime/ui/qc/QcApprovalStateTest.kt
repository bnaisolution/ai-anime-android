package org.dpdns.bnai.anime.ui.qc

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QcApprovalStateTest {
    @Test
    fun publishStaysLockedWhenAutomatedQcPassedButHumanApprovalIsPending() {
        assertFalse(isQcPublishUnlocked("PASSED", "PENDING"))
        assertFalse(isQcPublishUnlocked("PENDING_HUMAN_APPROVAL", "PENDING"))
    }

    @Test
    fun publishUnlocksOnlyWhenQcPassedAndApprovalIsRecorded() {
        assertTrue(isQcPublishUnlocked("PASSED", "APPROVED"))
        assertTrue(isQcPublishUnlocked("PENDING_HUMAN_APPROVAL", "APPROVED"))
        assertTrue(isQcPublishUnlocked("approved", "approved"))
    }

    @Test
    fun publishStaysLockedForFailedQcEvenIfApprovalFlagIsWrong() {
        assertFalse(isQcPublishUnlocked("FAILED", "APPROVED"))
        assertFalse(isQcPublishUnlocked("REJECTED", "APPROVED"))
        assertFalse(isQcPublishUnlocked("APPROVED", null))
    }

    @Test
    fun approveActionIsAvailableOnlyBeforeHumanApproval() {
        assertTrue(canApproveQc("PENDING_HUMAN_APPROVAL", "PENDING"))
        assertTrue(canApproveQc("PASSED", null))
        assertFalse(canApproveQc("PASSED", "APPROVED"))
        assertFalse(canApproveQc("FAILED", "PENDING"))
    }
}
