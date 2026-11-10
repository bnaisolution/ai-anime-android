package org.dpdns.bnai.anime.core.memory

import org.dpdns.bnai.anime.core.auth.TokenManager
import org.dpdns.bnai.anime.core.network.MemoryItemDto
import org.dpdns.bnai.anime.core.network.MemoryRepository
import org.dpdns.bnai.anime.core.network.Resource

class MemoryCoordinator(
    private val repository: MemoryRepository,
    private val tokenManager: TokenManager,
    private val reconciler: MemoryReconciler = MemoryReconciler()
) {
    suspend fun reconcileProjectMemory(projectId: String): Resource<MemoryReconciliationResult> {
        if (projectId.isBlank()) {
            return Resource.Error("projectId is required")
        }
        return when (val server = repository.readMemory(projectId = projectId)) {
            is Resource.Success -> {
                val local = tokenManager.getProjectChatHistory(projectId).map {
                    LocalMemoryMessage(
                        id = it.id,
                        role = it.role,
                        content = it.content,
                        timestampMs = it.timestampMs
                    )
                }
                val result = reconciler.reconcile(
                    localMessages = local,
                    serverItems = server.data.map { it.toServerMemoryItem() },
                    tombstones = tokenManager.getMemoryTombstones(projectId)
                )
                tokenManager.saveMemoryTombstones(projectId, result.tombstones)
                Resource.Success(result)
            }
            is Resource.Error -> server
            else -> Resource.Empty
        }
    }

    suspend fun deleteLocalMemory(projectId: String, stableId: String): Resource<Unit> {
        tokenManager.addMemoryTombstone(projectId, stableId)
        return when (val response = repository.deleteMemory(memoryId = stableId)) {
            is Resource.Success -> Resource.Success(Unit)
            is Resource.Error -> Resource.Error(response.message, response.code)
            else -> Resource.Success(Unit)
        }
    }

    suspend fun uploadPending(projectId: String, result: MemoryReconciliationResult): Resource<Int> {
        var uploaded = 0
        for (message in result.pendingUpload) {
            when (repository.writeMemory(
                projectId = projectId,
                content = message.content,
                scope = "project",
                category = "conversation",
                idempotencyKey = message.id
            )) {
                is Resource.Success -> uploaded += 1
                is Resource.Error -> return Resource.Error("Memory upload interrupted after $uploaded item(s)")
                else -> Unit
            }
        }
        return Resource.Success(uploaded)
    }
}

private fun MemoryItemDto.toServerMemoryItem(): ServerMemoryItem = ServerMemoryItem(
    id = id.orEmpty(),
    idempotencyKey = idempotency_key,
    content = content,
    knowledgeStatus = knowledge_status,
    projectId = project_id
)
