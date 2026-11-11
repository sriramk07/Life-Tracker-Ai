package com.example

import com.example.data.local.entities.TaskEntity
import com.example.data.sync.CloudSyncEngine
import org.junit.Assert.*
import org.junit.Test

/**
 * ============================================================================
 * UNIT TESTS: BIDIRECTIONAL CLOUD SYNC & LAST-WRITE-WINS RECONCILIATION
 * ============================================================================
 */
class ConflictResolutionSyncTest {

    @Test
    fun reconcileLastWriteWins_pushesLocalOnlyRecordsAndPullsRemoteOnlyRecords() {
        val local = listOf(
            TaskEntity(id = 1, title = "Local Only Task", updatedAt = 1000L)
        )
        val remote = listOf(
            TaskEntity(id = 2, title = "Cloud Only Task", updatedAt = 1500L)
        )

        val result = CloudSyncEngine.reconcileLastWriteWins(
            localItems = local,
            remoteItems = remote,
            idSelector = { it.id.toString() },
            updatedAtSelector = { it.updatedAt }
        )

        assertEquals(0, result.conflictsResolved)
        assertEquals(1, result.toPushRemotely.size)
        assertEquals("Local Only Task", result.toPushRemotely.first().title)
        assertEquals(1, result.toSaveLocally.size)
        assertEquals("Cloud Only Task", result.toSaveLocally.first().title)
    }

    @Test
    fun reconcileLastWriteWins_remoteNewerTimestampWinsConflict() {
        val local = listOf(
            TaskEntity(id = 10, title = "Old Title Local", isCompleted = false, updatedAt = 1000L)
        )
        val remote = listOf(
            TaskEntity(id = 10, title = "Updated On Tablet", isCompleted = true, updatedAt = 2500L)
        )

        val result = CloudSyncEngine.reconcileLastWriteWins(
            localItems = local,
            remoteItems = remote,
            idSelector = { it.id.toString() },
            updatedAtSelector = { it.updatedAt }
        )

        assertEquals(1, result.conflictsResolved)
        assertEquals(1, result.toSaveLocally.size)
        assertTrue(result.toPushRemotely.isEmpty())
        assertEquals("Updated On Tablet", result.toSaveLocally.first().title)
        assertTrue(result.toSaveLocally.first().isCompleted)
    }

    @Test
    fun reconcileLastWriteWins_localNewerTimestampWinsConflict() {
        val local = listOf(
            TaskEntity(id = 20, title = "Edited Offline Phone", updatedAt = 5000L)
        )
        val remote = listOf(
            TaskEntity(id = 20, title = "Stale Cloud Draft", updatedAt = 2000L)
        )

        val result = CloudSyncEngine.reconcileLastWriteWins(
            localItems = local,
            remoteItems = remote,
            idSelector = { it.id.toString() },
            updatedAtSelector = { it.updatedAt }
        )

        assertEquals(1, result.conflictsResolved)
        assertEquals(1, result.toPushRemotely.size)
        assertTrue(result.toSaveLocally.isEmpty())
        assertEquals("Edited Offline Phone", result.toPushRemotely.first().title)
    }

    @Test
    fun reconcileLastWriteWins_equalTimestampsAlreadyConverged() {
        val local = listOf(TaskEntity(id = 30, title = "Synced Record", updatedAt = 3000L))
        val remote = listOf(TaskEntity(id = 30, title = "Synced Record", updatedAt = 3000L))

        val result = CloudSyncEngine.reconcileLastWriteWins(
            localItems = local,
            remoteItems = remote,
            idSelector = { it.id.toString() },
            updatedAtSelector = { it.updatedAt }
        )

        assertEquals(0, result.conflictsResolved)
        assertTrue(result.toPushRemotely.isEmpty())
        assertTrue(result.toSaveLocally.isEmpty())
    }
}
