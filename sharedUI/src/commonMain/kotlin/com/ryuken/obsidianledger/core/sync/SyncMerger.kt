package com.ryuken.obsidianledger.core.sync

/**
 * Pure pull-merge decision logic, kept free of SQLDelight and Supabase types so the
 * conflict policy is unit-testable and deterministic.
 *
 * Conflict resolution policy (see docs/SyncContract.md for the full contract):
 *  - Push always runs before pull, so a row changed locally (dirty) and remotely
 *    resolves as LOCAL WINS — the push already overwrote the remote state.
 *  - A dirty local row is never overwritten or deleted by a pull. If it is missing
 *    remotely it stays local and resurrects on the next push.
 *  - A clean local row missing remotely was deleted on another device — delete it
 *    locally (delete wins over edit, enforced by push ordering).
 *  - A clean local row whose cached server stamp differs from the remote manifest
 *    changed on another device — fetch the remote version (remote wins over clean).
 *  - `serverUpdatedAt` is the server-maintained `updated_at` (trigger-stamped), used
 *    only as a change token; device clocks are never compared.
 */
data class LocalManifestRow(
    val id              : String,
    val serverUpdatedAt : String?,
    val isDirty         : Boolean
)

data class RemoteManifestRow(
    val id              : String,
    val serverUpdatedAt : String?
)

data class SyncPlan(
    /** Remote rows that are new or changed relative to the local cache — fetch full rows. */
    val toFetch         : List<String>,
    /** Clean local rows absent remotely (deleted on another device) — hard-delete locally. */
    val toDeleteLocal   : List<String>
)

object SyncMerger {

    fun plan(local: List<LocalManifestRow>, remote: List<RemoteManifestRow>): SyncPlan {
        val localById = local.associateBy { it.id }

        val toFetch = remote.mapNotNull { r ->
            val l = localById[r.id]
            when {
                // Unknown locally — remote create (or this device reinstalled).
                l == null                          -> r.id
                // Local has unsynced work — local wins; pull must not touch it.
                l.isDirty                          -> null
                // Stamp differs — changed remotely since our last confirmed sync.
                r.serverUpdatedAt != l.serverUpdatedAt -> r.id
                else                               -> null
            }
        }

        val remoteIds = remote.asSequence().map { it.id }.toHashSet()
        val toDeleteLocal = local.filter { l ->
            !l.isDirty && l.id !in remoteIds
        }.map { it.id }

        return SyncPlan(toFetch = toFetch, toDeleteLocal = toDeleteLocal)
    }
}
