package org.dpdns.bnai.anime.ui.qc

private val QC_PASS_STATUSES = setOf(
    "APPROVED",
    "PASS",
    "PASSED",
    "QC_PASSED",
    "PENDING_HUMAN_APPROVAL",
)

internal fun isQcPublishUnlocked(qcStatus: String?, approvalStatus: String?): Boolean {
    val qc = qcStatus?.trim()?.uppercase().orEmpty()
    val approval = approvalStatus?.trim()?.uppercase().orEmpty()
    return approval == "APPROVED" && qc in QC_PASS_STATUSES
}

internal fun canApproveQc(qcStatus: String?, approvalStatus: String?): Boolean {
    val qc = qcStatus?.trim()?.uppercase().orEmpty()
    val approval = approvalStatus?.trim()?.uppercase().orEmpty()
    return approval != "APPROVED" && qc in QC_PASS_STATUSES
}
