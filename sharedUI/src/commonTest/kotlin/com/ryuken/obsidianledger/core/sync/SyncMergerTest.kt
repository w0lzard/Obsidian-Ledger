package com.ryuken.obsidianledger.core.sync

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SyncMergerTest {

    private fun local(id: String, stamp: String? = null, dirty: Boolean = false) =
        LocalManifestRow(id = id, serverUpdatedAt = stamp, isDirty = dirty)

    private fun remote(id: String, stamp: String? = null) =
        RemoteManifestRow(id = id, serverUpdatedAt = stamp)

    @Test
    fun freshInstall_allRemoteRowsFetched() {
        val plan = SyncMerger.plan(
            local  = emptyList(),
            remote = listOf(remote("a", "t1"), remote("b", "t2"))
        )
        assertEquals(listOf("a", "b"), plan.toFetch)
        assertTrue(plan.toDeleteLocal.isEmpty())
    }

    @Test
    fun secondDevice_existingCleanRowUnchanged_notFetched() {
        val plan = SyncMerger.plan(
            local  = listOf(local("a", "t1")),
            remote = listOf(remote("a", "t1"))
        )
        assertTrue(plan.toFetch.isEmpty())
        assertTrue(plan.toDeleteLocal.isEmpty())
    }

    @Test
    fun remoteChanged_cleanLocal_fetched() {
        val plan = SyncMerger.plan(
            local  = listOf(local("a", "t1")),
            remote = listOf(remote("a", "t2"))
        )
        assertEquals(listOf("a"), plan.toFetch)
    }

    @Test
    fun localDirty_remoteChanged_skipped_localWins() {
        val plan = SyncMerger.plan(
            local  = listOf(local("a", "t1", dirty = true)),
            remote = listOf(remote("a", "t2"))
        )
        assertTrue(plan.toFetch.isEmpty())
        assertTrue(plan.toDeleteLocal.isEmpty())
    }

    @Test
    fun remoteDeleted_cleanLocal_deletedLocally() {
        val plan = SyncMerger.plan(
            local  = listOf(local("a", "t1")),
            remote = emptyList()
        )
        assertEquals(listOf("a"), plan.toDeleteLocal)
    }

    @Test
    fun remoteDeleted_dirtyLocal_keptForResurrection() {
        val plan = SyncMerger.plan(
            local  = listOf(local("a", "t1", dirty = true)),
            remote = emptyList()
        )
        assertTrue(plan.toDeleteLocal.isEmpty())
        assertTrue(plan.toFetch.isEmpty())
    }

    @Test
    fun nullLocalStamp_alwaysFetched_backfillsServerStampAfterPush() {
        // A row just pushed: markClean ran but the server stamp was never read back,
        // so it stays null until the first pull confirms it.
        val plan = SyncMerger.plan(
            local  = listOf(local("a", stamp = null)),
            remote = listOf(remote("a", "t1"))
        )
        assertEquals(listOf("a"), plan.toFetch)
    }

    @Test
    fun nullRemoteStamp_treatedAsChanged() {
        // Defensive: a remote row lacking updated_at is always worth fetching.
        val plan = SyncMerger.plan(
            local  = listOf(local("a", "t1")),
            remote = listOf(remote("a", null))
        )
        assertEquals(listOf("a"), plan.toFetch)
    }

    @Test
    fun mixedPlan_allDecisionsIndependent() {
        val plan = SyncMerger.plan(
            local = listOf(
                local("unchanged", "t1"),            // clean, same stamp     -> untouched
                local("remote-new-for-me"),          // clean, not remote     -> delete local
                local("mine-dirty", "t1", true),     // dirty, missing remote -> keep
                local("stale", "t1"),                // clean, remote changed -> fetch
                local("only-local", "t1")            // clean, not remote     -> delete local
            ),
            remote = listOf(
                remote("unchanged", "t1"),
                remote("stale", "t9"),
                remote("brand-new", "t1")
            )
        )
        assertEquals(listOf("stale", "brand-new"), plan.toFetch)
        assertEquals(listOf("remote-new-for-me", "only-local"), plan.toDeleteLocal)
    }

    @Test
    fun repeatedRuns_idempotent_noFetchNoDelete() {
        val local = listOf(local("a", "t1"))
        val remote = listOf(remote("a", "t1"))
        val first = SyncMerger.plan(local, remote)
        val second = SyncMerger.plan(local, remote)
        assertEquals(first, second)
        assertTrue(first.toFetch.isEmpty() && first.toDeleteLocal.isEmpty())
    }

    @Test
    fun tenThousandRows_plansInLinearTime() {
        val local  = (0 until 10_000).map { local("row-$it", "t$it") }
        val remote = (0 until 10_000).map { remote("row-$it", "t$it") } +
            listOf(remote("extra", "tx"))
        val start = System.currentTimeMillis()
        val plan = SyncMerger.plan(local, remote)
        val elapsed = System.currentTimeMillis() - start
        assertEquals(listOf("extra"), plan.toFetch)
        assertTrue(plan.toDeleteLocal.isEmpty())
        assertTrue(elapsed < 5_000, "manifest diff took ${elapsed}ms for 10k rows")
    }
}
