# Sync Contract

The local SQLDelight database is the **source of truth for the UI**. The remote
Postgres tables (Supabase) are the durability and cross-device medium. Every sync
run executes **push, then pull**, per repository, in this order: categories →
budgets → transactions (categories first so pulled transactions always find their
category rows locally).

## Data flow

### Push (`syncPendingToRemote(userId)`)

1. Select local rows `WHERE isDirty = 1 AND userId = :userId` (user-scoped — one
   user's sync never touches another's pending rows on a shared device).
2. Upsert all live dirty rows (client-generated UUID ids make this an insert-or-update).
3. For every tombstoned row (`deletedAt` set), DELETE the remote row by id.
4. Only after every remote operation succeeded: `markClean` all pushed rows, then
   purge local tombstones. A failure at any earlier step throws and leaves all rows
   dirty — the next run re-pushes idempotently (upsert + delete are idempotent).

### Pull (`pullRemote(userId)`)

1. **Manifest**: fetch `(id, updated_at)` for every remote row of the user,
   paginated (PostgREST caps rows per request; Supabase defaults to 1000).
2. **Plan**: `SyncMerger.plan(localManifest, remoteManifest)` decides what to fetch,
   what to delete locally, and what to leave alone (pure function, unit-tested).
3. **Fetch**: full rows for planned ids, in chunks of 500. Applied locally with
   `INSERT OR IGNORE` + `UPDATE ... WHERE isDirty = 0` — a row that became dirty
   after push started (user editing mid-sync) is never overwritten.
4. **Deletes**: clean local rows absent from the remote manifest are hard-deleted.

## Conflict resolution policy

| Situation | Resolution | Why |
|---|---|---|
| Row dirty locally, also changed remotely | **Local wins** | Push runs first; the local version already overwrote remote |
| Row clean locally, remote stamp differs | **Remote wins** | Changed on another device; local had no pending work |
| Deleted remotely, clean locally | **Delete wins** | Local copy hard-deleted on pull |
| Deleted remotely, dirty locally | **Local wins (resurrect)** | Kept; next push re-creates it remotely |
| Deleted locally (tombstone), edited remotely | **Delete wins** | Tombstone pushed as remote DELETE |
| Deleted locally, deleted remotely | Converges | Tombstone purged after push; absent remotely |

`serverUpdatedAt` (local column) caches the server `updated_at` last seen for the
row. It is NULL right after a push (markClean does not read the response back), so
the immediately following pull fetches the row once and stamps it — subsequent
manifests then match and skip it. **Server timestamps are change tokens only**;
they are never compared across devices, and device clocks are never trusted.

## Sync triggers

- **Sign-in / re-auth**: `App.kt` runs `SyncUseCase` on every transition to
  `Authenticated` (per userId, fire-and-forget, log-only failures). This is what
  recovers a reinstall, second device, or cleared app data.
- **Periodic**: Android WorkManager, every 15 min, network-constrained
  (`SyncScheduler`), cancelled on sign-out.
- **Manual**: Profile → Sync Now.

## Failure semantics

- Each repo's push/pull is attempted independently; one repo failing does not block
  the others. The first failure is rethrown after all steps run.
- Push failure ⇒ rows stay dirty ⇒ retried next run (idempotent).
- Pull failure mid-chunk ⇒ already-applied chunks stay (idempotent re-run: stamps
  now match, skipped).
- Tombstones are never purged before their remote DELETE succeeded.

## Sign-out

Sign-out does not wipe the local database. Rows are user-scoped by `userId`
everywhere (queries, dirty selection, manifests), so a subsequent sign-in as a
different user never sees the previous user's rows; their dirty rows simply stop
being selected. WorkManager sync is cancelled via `SyncCoordinator.onSignedOut()`.
