package org.dpdns.bnai.anime.core.memory

enum class MemorySyncState {
    SYNCED,
    PENDING_UPLOAD,
    CORRECTED,
    DELETED
}

data class LocalMemoryMessage(
    val id: String,
    val role: String,
    val content: String,
    val timestampMs: Long
)

data class ServerMemoryItem(
    val id: String,
    val idempotencyKey: String? = null,
    val content: String,
    val knowledgeStatus: String? = null,
    val projectId: String? = null,
    val updatedAtMs: Long? = null
)

data class ReconciledMemoryItem(
    val stableId: String,
    val content: String,
    val role: String?,
    val state: MemorySyncState,
    val serverId: String? = null
)

data class MemoryReconciliationResult(
    val items: List<ReconciledMemoryItem>,
    val pendingUpload: List<LocalMemoryMessage>,
    val tombstones: Set<String>
)

class MemoryReconciler {
    fun reconcile(
        localMessages: List<LocalMemoryMessage>,
        serverItems: List<ServerMemoryItem>,
        tombstones: Set<String> = emptySet()
    ): MemoryReconciliationResult {
        val localById = LinkedHashMap<String, LocalMemoryMessage>()
        for (message in localMessages) {
            if (message.id in tombstones) continue
            val existing = localById[message.id]
            if (existing == null || message.timestampMs >= existing.timestampMs) {
                localById[message.id] = message
            }
        }

        val merged = LinkedHashMap<String, ReconciledMemoryItem>()
        val removed = tombstones.toMutableSet()
        val matchedLocalIds = mutableSetOf<String>()

        for (server in serverItems) {
            val stableId = server.idempotencyKey?.takeIf { it.isNotBlank() } ?: server.id
            if (stableId in removed) continue
            val status = server.knowledgeStatus?.lowercase()
            if (status == "deleted" || status == "superseded") {
                removed += stableId
                continue
            }
            matchedLocalIds += stableId
            val corrected = status == "corrected"
            merged[stableId] = ReconciledMemoryItem(
                stableId = stableId,
                content = server.content,
                role = localById[stableId]?.role,
                state = if (corrected) MemorySyncState.CORRECTED else MemorySyncState.SYNCED,
                serverId = server.id
            )
        }

        val pending = mutableListOf<LocalMemoryMessage>()
        for ((id, local) in localById) {
            if (id in removed || id in matchedLocalIds) continue
            pending += local
            merged[id] = ReconciledMemoryItem(
                stableId = id,
                content = local.content,
                role = local.role,
                state = MemorySyncState.PENDING_UPLOAD
            )
        }

        return MemoryReconciliationResult(
            items = merged.values.sortedBy { it.stableId },
            pendingUpload = pending.sortedBy { it.timestampMs },
            tombstones = removed
        )
    }
}
