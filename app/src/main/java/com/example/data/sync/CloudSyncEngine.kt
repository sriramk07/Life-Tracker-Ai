package com.example.data.sync

/**
 * ============================================================================
 * CLOUD SYNC ENGINE — BIDIRECTIONAL LAST-WRITE-WINS RECONCILIATION
 * ============================================================================
 *
 * Implements deterministic bidirectional merge conflict resolution between
 * local SQLite records and Cloud Firestore documents using `updatedAt` epoch
 * timestamps.
 */

/**
 * Summary report produced after a bidirectional synchronization cycle.
 *
 * @property pulledFromCloud Number of newer or missing records pulled down to Room.
 * @property pushedToCloud Number of newer or missing local records pushed to Firestore.
 * @property conflictsResolved Number of records present in both stores that collided.
 * @property syncedAt Epoch millisecond timestamp when synchronization finished.
 */
data class SyncReport(
    val pulledFromCloud: Int = 0,
    val pushedToCloud: Int = 0,
    val conflictsResolved: Int = 0,
    val syncedAt: Long = System.currentTimeMillis()
)

/**
 * Encapsulates reconciled entity deltas for a single table.
 *
 * @param T Entity type implementing synchronization.
 * @property toSaveLocally Remote records that won Last-Write-Wins resolution.
 * @property toPushRemotely Local records that are absent remotely or newer than remote.
 * @property conflictsResolved Number of ID collisions resolved via timestamp comparison.
 */
data class ReconciledMerge<T>(
    val toSaveLocally: List<T>,
    val toPushRemotely: List<T>,
    val conflictsResolved: Int
)

/**
 * Pure conflict-resolution engine implementing Last-Write-Wins (LWW).
 */
object CloudSyncEngine {

    /**
     * Reconciles a collection of [localItems] and [remoteItems] keyed by [idSelector].
     *
     * Resolution Rules:
     * 1. Exists locally only -> queued in `toPushRemotely`.
     * 2. Exists remotely only -> queued in `toSaveLocally`.
     * 3. Exists in both -> triggers conflict check on [updatedAtSelector]:
     *    - If `remoteUpdatedAt > localUpdatedAt`: remote wins -> `toSaveLocally`.
     *    - If `localUpdatedAt > remoteUpdatedAt`: local wins -> `toPushRemotely`.
     *    - Equal timestamps: converged (no redundant write needed).
     *
     * @param localItems Snapshot of records from local Room DAO.
     * @param remoteItems Snapshot of records fetched from Cloud Firestore.
     * @param idSelector Extractor returning unique ID string.
     * @param updatedAtSelector Extractor returning last mutation timestamp in ms.
     * @return [ReconciledMerge] containing exact lists to upsert locally and remotely.
     */
    fun <T> reconcileLastWriteWins(
        localItems: List<T>,
        remoteItems: List<T>,
        idSelector: (T) -> String,
        updatedAtSelector: (T) -> Long
    ): ReconciledMerge<T> {
        val localMap = localItems.associateBy(idSelector)
        val remoteMap = remoteItems.associateBy(idSelector)
        val allIds = (localMap.keys + remoteMap.keys).distinct()

        val toSaveLocally = mutableListOf<T>()
        val toPushRemotely = mutableListOf<T>()
        var conflicts = 0

        for (id in allIds) {
            val local = localMap[id]
            val remote = remoteMap[id]

            when {
                local != null && remote == null -> {
                    toPushRemotely.add(local)
                }
                local == null && remote != null -> {
                    toSaveLocally.add(remote)
                }
                local != null && remote != null -> {
                    val localTs = updatedAtSelector(local)
                    val remoteTs = updatedAtSelector(remote)
                    when {
                        remoteTs > localTs -> {
                            conflicts++
                            toSaveLocally.add(remote)
                        }
                        localTs > remoteTs -> {
                            conflicts++
                            toPushRemotely.add(local)
                        }
                        else -> {
                            // Identical updatedAt -> already converged
                        }
                    }
                }
            }
        }

        return ReconciledMerge(
            toSaveLocally = toSaveLocally,
            toPushRemotely = toPushRemotely,
            conflictsResolved = conflicts
        )
    }
}
