package org.dpdns.bnai.anime.core.memory

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MemoryReconcilerTest {
    private val reconciler = MemoryReconciler()

    @Test
    fun serverIdempotencyKeyMatchesLocalMessageAndDeduplicates() {
        val local = listOf(
            LocalMemoryMessage("msg-1", "user", "Remember the promise", 100L),
            LocalMemoryMessage("msg-1", "user", "Remember the promise", 100L)
        )
        val server = listOf(
            ServerMemoryItem("server-1", "msg-1", "Remember the promise", "observed", "p1")
        )

        val result = reconciler.reconcile(local, server)

        assertEquals(1, result.items.size)
        assertEquals("msg-1", result.items.single().stableId)
        assertEquals(MemorySyncState.SYNCED, result.items.single().state)
        assertTrue(result.pendingUpload.isEmpty())
    }

    @Test
    fun localOnlyMessagesRemainPendingForUpload() {
        val local = listOf(
            LocalMemoryMessage("msg-local", "assistant", "Local unsynced memory", 200L)
        )

        val result = reconciler.reconcile(local, emptyList())

        assertEquals(1, result.items.size)
        assertEquals(MemorySyncState.PENDING_UPLOAD, result.items.single().state)
        assertEquals(listOf("msg-local"), result.pendingUpload.map { it.id })
    }

    @Test
    fun correctedServerItemWinsOverLocalContent() {
        val local = listOf(
            LocalMemoryMessage("msg-2", "user", "Old incorrect memory", 100L)
        )
        val server = listOf(
            ServerMemoryItem("server-2", "msg-2", "Corrected memory", "corrected", "p1")
        )

        val result = reconciler.reconcile(local, server)

        assertEquals("Corrected memory", result.items.single().content)
        assertEquals(MemorySyncState.CORRECTED, result.items.single().state)
        assertTrue(result.pendingUpload.isEmpty())
    }

    @Test
    fun deletedAndTombstonedItemsAreNotResurrected() {
        val local = listOf(
            LocalMemoryMessage("msg-deleted", "user", "Deleted local memory", 100L),
            LocalMemoryMessage("msg-tombstone", "user", "Tombstoned memory", 100L)
        )
        val server = listOf(
            ServerMemoryItem("server-deleted", "msg-deleted", "Deleted server memory", "deleted", "p1")
        )

        val result = reconciler.reconcile(
            local,
            server,
            tombstones = setOf("msg-tombstone")
        )

        assertTrue(result.items.isEmpty())
        assertTrue(result.pendingUpload.isEmpty())
        assertTrue(result.tombstones.contains("msg-deleted"))
        assertTrue(result.tombstones.contains("msg-tombstone"))
    }

    @Test
    fun newestLocalDuplicateIsKeptWhenIdsCollide() {
        val local = listOf(
            LocalMemoryMessage("msg-3", "user", "Older content", 100L),
            LocalMemoryMessage("msg-3", "user", "Newer content", 300L)
        )

        val result = reconciler.reconcile(local, emptyList())

        assertEquals("Newer content", result.items.single().content)
    }
}
