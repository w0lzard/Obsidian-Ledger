# Obsidian-Ledger Full Remediation Pass — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix every confirmed audit finding — bidirectional sync, category sync, split atomicity, iOS completeness, dead intent wiring, effect collection, CSV parser, and the long tail of small bugs — without breaking the Clean MVI architecture.

**Architecture:** Local SQLDelight stays the single source of truth for UI. Sync becomes a manifest-diff bidirectional protocol with an explicit, documented conflict policy (local-wins push-first, delete-wins, server-authoritative `updated_at`). Splits move multi-table writes into Postgres RPCs. Categories become a synced, user-scoped table.

**Tech Stack:** Kotlin 2.3.20, Compose Multiplatform, Decompose, Koin, SQLDelight 2.3.2, supabase-kt 3.4.1 (postgrest/auth/realtime), kotlinx-datetime, Napier.

**Spec:** User remediation prompt of 2026-09-01 (16 phases). This plan implements it verbatim.

## Global Constraints

- Preserve MVI (State/Intent/Effect), feature-first packaging, dependency direction UI→VM→UseCase→Repo-interface→Repo-impl→data source.
- Domain models stay free of SQLDelight/Supabase/Compose/platform types.
- Rethrow `CancellationException` in every coroutine catch block.
- No TODO/FIXME/fake impls/empty catches as final solutions.
- Verify every fix with build + tests. `./gradlew :sharedUI:testDebugUnitTest`, `:sharedUI:compileKotlinIosSimulatorArm64`, `:androidApp:assembleDebug`, `:sharedUI:verifySqlDelightMigration`.
- Machine limits: Linux host — no Xcode, no live Supabase DDL (anon key only). iOS Swift changes are compile-verified on Kotlin side only; Supabase migrations are delivered as SQL files under `supabase/migrations/` for the owner to apply. Both limits called out again in Phase 4 / Phase 14.
- Money representation decision (Phase 13): keep `Double` + `roundToCents()` at every boundary — full cents migration touches remote schema + all stored rows and both DTO layers; risk outweighs benefit for a personal ledger with cent-rounding already enforced at every aggregation. Documented + hardened instead. See Task 13.

---

## Verified findings baseline (re-confirmed against current code this session)

| # | Finding | Status |
|---|---|---|
| 1 | Push-only sync; no pull | CONFIRMED (`TransactionRepositoryImpl.syncPendingToRemote:145-160`) |
| 2 | `selectDirty` not user-scoped | CONFIRMED (`TransactionEntity.sq:9`) |
| 3 | Categories local-only, customs never sync | CONFIRMED (`CategoryRepositoryImpl` has no Supabase client) |
| 4 | Splits multi-write + client compensation | CONFIRMED (`SplitRepositoryImpl` retry/compensation) |
| 5 | Dashboard/Analytics intents never dispatched | CONFIRMED (`DashboardScreen.kt:40`) |
| 6 | `SplitsEffect` never collected | CONFIRMED (`SplitsScreen.kt`) |
| 7 | CSV import splits on `lines()` — multiline fields corrupt | CONFIRMED (`ImportCsvUseCase.kt:24`) |
| 8 | `monthlyBudget` sums preview slice (3) | CONFIRMED (`DashboardViewModel.kt:73`) |
| 9 | Splash has no timeout path | CONFIRMED (`App.kt:61-82`) |
| 10 | `CancellationException` swallowed in ProfileViewModel (5 sites) | CONFIRMED |
| 11 | `last_sync` = literal "Just now" | CONFIRMED (`ProfileViewModel.kt:257`) |
| 12 | `avgTransaction` divides by category count | CONFIRMED (`AnalyticsState.kt:22`) |
| 13 | Settle dialog no cap vs balance; business layer no invariant | CONFIRMED (`GroupDetailScreen.kt:315`, `RecordSettlementUseCase`) |
| 14 | CSV import accepts ≤0 amounts, no validation | CONFIRMED (`ImportCsvUseCase.kt:46`) |
| 15 | Duplicate `AuthSuccess` on password sign-in | CONFIRMED, benign (`replaceAll` idempotent) — harmonize to single emission path |
| 16 | Dead code: 3 empty use cases, unused `error` fields, `getSessionStatusString()`, `greeting()` stub | CONFIRMED |
| 17 | iOS: `SupabaseConfig.configure` never called; Swift refs to missing `SyncScheduler`/`AuthHandler`; Google sign-in stub; NSUserDefaults plaintext; no-op SyncCoordinator | CONFIRMED |
| 18 | Test suite = 1 file; drift test asserts fresh expression | CONFIRMED (`HelpersTest.kt:10-11`) |
| 19 | `Helpers.kt` `distributeCentsEvenly` cents-exact (Long internally) | CONFIRMED — foundation for Phase 13 decision |

---

# PHASE 1 — Bidirectional sync (transactions + budgets)

## Design: the sync contract

**Principle.** Local SQLDelight is the source of truth for reads. Remote Postgres is the durability + cross-device medium. Every sync run is: **push → pull**, both manifest-driven, both idempotent.

**Conflict resolution policy (documented in `SyncContract.md` and code comments):**

1. **Push before pull.** Local dirty rows are pushed first. Therefore a row changed both locally (dirty) and remotely resolves as *local wins* — the push overwrites the remote state, and the server stamps a fresh `updated_at`.
2. **Delete wins over edit.** Local tombstones are pushed as remote deletes *before* upserting live dirty rows is not required; order within push is (a) live upserts, (b) tombstone deletes — wait, spec: if local deleted + remote edited, we want deletion to win. Pushing live upserts first can't resurrect a row the same device tombstoned (a row is either live or tombstoned locally, never both). Cross-device: device A edits, device B deletes → B's delete lands after A's edit ⇒ remote row gone ⇒ A's pull sees id-missing-remotely + local-clean ⇒ A hard-deletes locally. Delete wins. ✔
3. **Remote wins over clean local.** If the row is clean locally and remote `updated_at` differs from the locally cached server timestamp, the remote version replaces the local row.
4. **Local dirty is never overwritten by pull.** A dirty local row missing remotely is *kept* — it resurrects on the next push (recovering from "remote deleted while I edited offline": the editor's intent is treated as stronger than the stale deletion).
5. **Server-authoritative `updated_at`.** Remote `updated_at timestamptz NOT NULL DEFAULT now()` maintained by a `BEFORE UPDATE` trigger — clients never compare device clocks. The client stores the *server* `updated_at` read back after every push/pull; it is used only as a change-detection token in the pull manifest diff, never as a cross-device ordering key.
6. **No pull cursor.** The pull fetches a manifest `(id, updated_at)` of *all* the user's rows and diffs it against local `(id, serverUpdatedAt, isDirty)`. This detects creates, updates, **and deletes** (rows absent remotely) in one pass, survives partial failure (re-run is idempotent), and has no clock-skew window. 10k rows ≈ one JSON array of uuid+timestamp — acceptable; measured in Phase 16.

**Sync order per run:** categories (Phase 2) → budgets → transactions.

## Task 1.1: Local schema — server-sync columns

**Files:**
- Modify: `sharedUI/src/commonMain/database/.../TransactionEntity.sq`
- Modify: `sharedUI/src/commonMain/database/.../BudgetEntity.sq`
- Create: `sharedUI/src/commonMain/database/.../2.sqm`

**Design:** add `serverUpdatedAt TEXT` to both tables (the remote `updated_at` token last seen for this row; NULL/'' means never confirmed). Migration `2.sqm`:

```sql
ALTER TABLE TransactionEntity ADD COLUMN serverUpdatedAt TEXT;
ALTER TABLE BudgetEntity ADD COLUMN serverUpdatedAt TEXT;
-- user-scoped dirty selection (fixes finding #2)
```

New/changed queries (both .sq files, matching column order in `insert`):

```sql
selectDirty: SELECT * FROM TransactionEntity WHERE isDirty = 1 AND userId = :userId;
selectManifest: SELECT id, serverUpdatedAt FROM TransactionEntity WHERE userId = :userId;
upsertFromRemote: INSERT OR REPLACE INTO TransactionEntity VALUES (?,?,...,?); -- full row, isDirty=0, serverUpdatedAt set
updateServerStamp: UPDATE TransactionEntity SET serverUpdatedAt = :t, isDirty = 0 WHERE id = :id;
deleteById: DELETE FROM TransactionEntity WHERE id = :id AND isDirty = 0;
selectAllIds: SELECT id FROM TransactionEntity WHERE userId = :userId;
```

Note: `.sq` CREATE TABLE must match migration result; `verifyMigrations = true` enforces. Since `.sq` v1 already contains `deletedAt` while `1.sqm` adds it (pre-existing inconsistency), Task 1.1 also normalizes: bump `.sq` files to final shape, keep `1.sqm` as-is (its ALTER is what real v1→v2 devices ran), add `2.sqm` for the new columns. If `verifySqlDelightMigration` fails on the pre-existing mismatch, fix by making `1.sqm` exactly reproduce v1 (drop its `deletedAt` ALTER only if .sq history proves columns existed at v1 — inspect before deciding).

## Task 1.2: Remote schema — `updated_at` authority (budgets) + read-back

**Files:**
- Create: `supabase/migrations/0001_bidirectional_sync.sql`

```sql
-- budgets lacks server-maintained updated_at; transactions has the column but
-- clients could write it. Make both server-authoritative.
ALTER TABLE public.budgets ADD COLUMN IF NOT EXISTS updated_at timestamptz NOT NULL DEFAULT now();
ALTER TABLE public.budgets ADD COLUMN IF NOT EXISTS created_at timestamptz NOT NULL DEFAULT now();

CREATE OR REPLACE FUNCTION public.touch_updated_at() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN NEW.updated_at = now(); RETURN NEW; END $$;

DROP TRIGGER IF EXISTS trg_budgets_touch ON public.budgets;
CREATE TRIGGER trg_budgets_touch BEFORE UPDATE ON public.budgets
FOR EACH ROW EXECUTE FUNCTION public.touch_updated_at();

DROP TRIGGER IF EXISTS trg_transactions_touch ON public.transactions;
CREATE TRIGGER trg_transactions_touch BEFORE UPDATE ON public.transactions
FOR EACH ROW EXECUTE FUNCTION public.touch_updated_at();

CREATE INDEX IF NOT EXISTS idx_transactions_user ON public.transactions(user_id);
CREATE INDEX IF NOT EXISTS idx_budgets_user ON public.budgets(user_id);
```

Owner applies via Supabase SQL editor (no service key on this machine). README section added.

## Task 1.3: Pure merge planner (testable core)

**Files:**
- Create: `sharedUI/.../core/sync/SyncMerger.kt`
- Test: `sharedUI/src/commonTest/.../core/sync/SyncMergerTest.kt`

Pushed-down decision logic so the merge policy is unit-testable without a DB or network:

```kotlin
data class RemoteStamp(val id: String, val serverUpdatedAt: String?)

sealed interface RowAction {
    data class Fetch(val ids: List<String>) : RowAction           // new or remotely-changed
    data class DeleteLocal(val ids: List<String>) : RowAction     // remotely deleted, local clean
    // ids to leave alone: local dirty (local-wins / resurrect-later)
}

object SyncMerger {
    fun plan(local: List<LocalStamp>, remote: List<RemoteStamp>): RowAction
    // local: (id, serverUpdatedAt, isDirty)
}
```

Rules in code + exhaustive tests: remote-new, remote-changed vs clean local, unchanged (same stamp → no fetch), local-dirty skipped, remote-missing+clean → delete, remote-missing+dirty → keep.

## Task 1.4: Repository pull implementation

**Files:**
- Modify: `sharedUI/.../core/domain/repository/TransactionRepository.kt`, `BudgetRepository.kt` (interface)
- Modify: `TransactionRepositoryImpl.kt`, `BudgetRepositoryImpl.kt`

New interface method (both repos): `suspend fun pullRemote(userId: String)`.

Implementation shape (transactions; budgets analogous):

```kotlin
override suspend fun pullRemote(userId: String) = withRepositoryErrorHandling("TransactionRepository.pullRemote") {
    withContext(Dispatchers.IO) {
        // 1. manifest
        val remoteStamps = supabaseClient.postgrest["transactions"].select {
            filter { eq("user_id", userId) }; limit(50_000)
        }.decodeList<TransactionStampDto>()           // id + updated_at only
        // 2. plan
        val localStamps = queries.selectManifest(userId).executeAsList()
        val action = SyncMerger.plan(localStamps.map { it.toLocalStamp() }, remoteStamps.map { it.toRemoteStamp() })
        // 3. fetch full rows for changed/new (chunks of 500)
        action.fetch.chunked(500).forEach { chunk ->
            val rows = supabaseClient.postgrest["transactions"].select {
                filter { eq("user_id", userId); In("id", chunk) }
            }.decodeList<TransactionDto>()
            rows.forEach { queries.upsertFromRemote(it.toEntity()) }
        }
        // 4. propagate remote deletes (clean local rows only — dirty ones resurrect on next push)
        action.deleteLocal.forEach { queries.deleteById(it) }
    }
}
```

Push changes in `syncPendingToRemote`:
- upsert with `returning`/`count` read-back: `upsert(live.map{it.toDto()})` returns decoded rows carrying the server `updated_at` → for each returned row `updateServerStamp(id, serverUpdatedAt)` instead of bare `markClean`.
- tombstone deletes stay; `purgeTombstones` only after the whole push succeeded (already sequential — keep).
- `selectDirty` now user-scoped.

`SyncUseCase` becomes push+pull for both repos (and categories after Phase 2), with per-repo failure isolation: one repo failing must not block the other (try/catch per step, `CancellationException` rethrown, failures logged + aggregated into a `SyncResult` so the UI can surface status).

## Task 1.5: Sync on sign-in + docs

- `App.kt` / `ProfileViewModel.syncData()` unchanged consumers; add a sync trigger when `AuthSessionState.Authenticated` first fires in `App.kt` (`LaunchedEffect` → `SyncUseCase(uid)` in `runCatching`, log-only failure) so a second device/reinstall hydrates promptly after login, not only on the 15-min WorkManager tick or manual sync.
- Create `docs/SyncContract.md`: the six-point policy above, exact field semantics, failure modes.

**Tests (Task 1.3 file + repository-level where driver allows):** fresh-install full pull, second-device merge, local-edit-offline preserved, local-delete-offline tombstone pushed, network failure mid-push (dirty stays dirty), remote-missing-local-clean deleted, remote-missing-local-dirty kept, same-row-changed-both-sides local wins, delete-vs-edit delete wins, repeated runs no-ops (idempotence), 10k-row manifest plan performance.

---

# PHASE 2 — Category synchronization + referential integrity

## Task 2.1: Remote categories table

**Files:** Create `supabase/migrations/0002_categories_sync.sql`

```sql
CREATE TABLE IF NOT EXISTS public.categories (
    id          uuid PRIMARY KEY,
    user_id     uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    name        text NOT NULL,
    emoji       text NOT NULL DEFAULT '💰',
    color_hex   text NOT NULL DEFAULT '#00C896',
    is_custom   boolean NOT NULL DEFAULT true,
    sort_order  integer NOT NULL DEFAULT 99,
    created_at  timestamptz NOT NULL DEFAULT now(),
    updated_at  timestamptz NOT NULL DEFAULT now()
);
ALTER TABLE public.categories ENABLE ROW LEVEL SECURITY;
CREATE POLICY cat_own_select ON public.categories FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY cat_own_insert ON public.categories FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY cat_own_update ON public.categories FOR UPDATE USING (auth.uid() = user_id);
CREATE POLICY cat_own_delete ON public.categories FOR DELETE USING (auth.uid() = user_id);
CREATE TRIGGER trg_categories_touch BEFORE UPDATE ON public.categories
FOR EACH ROW EXECUTE FUNCTION public.touch_updated_at();
CREATE INDEX idx_categories_user ON public.categories(user_id);
```

Default categories (`cat_food` etc.) stay **local-only** seeds with `userId = NULL` and stable hardcoded ids — identical on every device by construction, so they never need rows in the remote table. Only custom categories sync. This is the "user-scoped, stable IDs" model: ids are client-generated UUIDs (already are — `ImportCsvUseCase`/`AddTransaction` generate them), never mutated.

## Task 2.2: Local category sync columns

**Files:** Modify `CategoryEntity.sq`; Create `3.sqm` (or fold into `2.sqm` if Phase 1 hasn't shipped — single combined migration file `2.sqm` covering transactions+budgets+categories, since none of this is released yet; final decision at implementation: **one migration `2.sqm`, one remote migration set** — fewer moving parts).

```sql
ALTER TABLE CategoryEntity ADD COLUMN updatedAt TEXT NOT NULL DEFAULT '';
ALTER TABLE CategoryEntity ADD COLUMN serverUpdatedAt TEXT;
ALTER TABLE CategoryEntity ADD COLUMN isDirty INTEGER NOT NULL DEFAULT 0;
ALTER TABLE CategoryEntity ADD COLUMN deletedAt TEXT;
```

New queries: `selectDirty(userId)`, `selectManifest(userId)`, `upsertFromRemote`, `updateServerStamp`, `deleteCleanById`, `markDeleted`, purge; `insertCustom` sets `isDirty = 1`. Deletion becomes tombstone (soft) to propagate deletes cross-device, same semantics as transactions.

## Task 2.3: Repo + sync wiring

- `CategoryRepositoryImpl` gains `supabaseClient`, `syncPendingToRemote(userId)`, `pullRemote(userId)` — identical shape to Task 1.4 (delegating to `SyncMerger`).
- `SyncUseCase` order: categories → budgets → transactions.
- **Referential integrity guard:** `delete(id)` refuses when `TransactionEntity` or `BudgetEntity` rows reference the id (SQL `EXISTS` check inside one transaction) — no silent orphaning. Categories pulled/merged before transactions each run, so remote transactions referencing a custom category find it locally.

**Tests:** custom category round-trip two-device simulation (pure merger), delete-blocked-when-referenced, CSV-imported category marked dirty, defaults never pushed.

---

# PHASE 3 — Splits atomicity (server RPCs)

## Task 3.1: RPC migration

**Files:** Create `supabase/migrations/0003_split_rpc.sql`

```sql
CREATE OR REPLACE FUNCTION public.create_split_group(p_name text, p_members jsonb)
RETURNS uuid LANGUAGE plpgsql SECURITY INVOKER AS $$
DECLARE v_group uuid; v_member uuid;
BEGIN
  INSERT INTO split_groups (id, name, created_by) VALUES (gen_random_uuid(), p_name, auth.uid()) RETURNING id INTO v_group;
  FOR m IN SELECT * FROM jsonb_array_elements(p_members) LOOP
    INSERT INTO split_group_members (id, group_id, user_id, display_name, email)
    VALUES (gen_random_uuid(), v_group, NULLIF(m->>'userId',''), m->>'displayName', NULLIF(m->>'email',''));
    -- first member with auth.uid() claims ownership row
  END LOOP;
  -- caller's own membership row: ensure exists
  INSERT INTO split_group_members (id, group_id, user_id, display_name)
  VALUES (gen_random_uuid(), v_group, auth.uid(), '') ON CONFLICT DO NOTHING;
  RETURN v_group;
END $$;

CREATE OR REPLACE FUNCTION public.create_split_expense(
  p_group uuid, p_description text, p_amount numeric, p_paid_by uuid,
  p_member_ids jsonb, p_date date, p_shares jsonb)
RETURNS uuid ... -- atomic expense + shares; validates: group exists, payer∈group, every member∈group, amount>0, Σshares=amount

CREATE OR REPLACE FUNCTION public.record_split_settlement(
  p_group uuid, p_from uuid, p_to uuid, p_amount numeric, p_date date)
RETURNS uuid ... -- validates membership, amount>0, amount ≤ |outstanding balance between pair| computed server-side
```

Plus constraints (idempotent `DO $$ ... IF NOT EXISTS` guards):
- `CHECK (amount > 0)` on `split_expenses`, `split_expense_shares`, `split_settlements`.
- FKs `expense_id→split_expenses`, `member_id→split_group_members`, `group_id→split_groups` with `ON DELETE CASCADE`.
- Indexes on `group_id` columns.

RLS stays invoker-checked (`SECURITY INVOKER` so RLS applies inside the function). Cent-exact shares: server re-verifies `Σshares = amount` (numeric exact); client keeps `distributeCentsEvenly`.

## Task 3.2: Client RPC calls

- Modify `SplitRepositoryImpl`: `createGroup` → `postgrest.rpc("create_split_group", …)`; `addExpense` → `rpc("create_split_expense")` (shares computed client-side, verified server-side); `recordSettlement` → `rpc("record_split_settlement")`. Delete `retryTransient` compensation blocks. Failures propagate as `RepositoryException` → existing `SplitsEffect.Error` / `GroupDetailEffect` path (Phase 6 makes those visible).
- Settlement cap business rule also enforced client-side in `RecordSettlementUseCase` (`amount <= owed`, throw domain error) — UI validates in Phase 11.

**Tests:** `distributeCentsEvenly` share-sum invariants, `computeBalances` incl. settlements, settlement-cap rule in use case (fake repo), RPC payload shape serialization test.

---

# PHASE 4 — iOS completeness (bounded by Linux host)

## Task 4.1: Kotlin iOS-side fixes (compile-verifiable)

- `SupabaseConfig.ios.kt`: read URL/key from `NSBundle.mainBundle` `Info.plist` keys (`SUPABASE_URL`, `SUPABASE_KEY`) via `platform.Foundation.NSBundle` — removes dependence on a never-called `configure()`. Keep `configure()` as override for tests/debug.
- `IosModule.kt`: real `SyncCoordinator` — no longer needed as no-op: replace with real implementation that runs `SyncUseCase` on sign-in via the common `App.kt` trigger from Task 1.5 (sync triggering moves to common code; iOS-specific coordinator keeps only the cancel hook).
- Add iOS `Info.plist` entries documented (keys only, values injected at build).
- Napier: iOS `KoinInit`/`doInitKoin` installs a debug Antilog via `Napier.base(...)` guarded by a `DEBUG` define equivalent (`isDebug` parameter from Swift; default false).

## Task 4.2: Swift-side fixes (best-effort, cannot compile here — flagged)

- `iosApp.swift`: remove phantom `AuthHandler.shared.handleDeepLink` (deep links were removed with OAuth redirect migration — memory confirms) and phantom `SyncScheduler().schedule()` (sync now triggered from common code). Keep BGTask registration only if a real task id exists — remove otherwise.
- Session storage: `IosModule` currently `Settings()` (NSUserDefaults). Replace with a Keychain-backed `Settings` implementation in Kotlin using `platform.Security` (`SecItemAdd`/`SecItemCopyMatching`) — pure Kotlin/iOS, compiles under `compileKotlinIosArm64`, no Swift needed. Ship as `KeychainSettings.kt` in `iosMain`.
- Google sign-in iOS: remains out of scope on this machine — full `GoogleSignInLauncher.ios.kt` requires the Swift SDK, real device, and Xcode build validation. Explicitly reported as not-done with reason. Credential Manager flow on Android unaffected.

**Validation:** `./gradlew :sharedUI:compileKotlinIosArm64 compileKotlinIosSimulatorArm64`. Swift file edits reviewed by eye; final Xcode build is an owner action.

---

# PHASE 5 — Dashboard/Analytics intent wiring

**Files:** `DashboardScreen.kt`, `DashboardState.kt`, `AnalyticsScreen.kt`, `AnalyticsState.kt` (+ tests)

- Add month selector UI (‹ month › chevrons + `MMM yyyy` label) to both screens, dispatching `MonthChanged(year, month)`; add refresh affordance (pull-to-refresh is not in the design system — use the existing `SplitsSummaryCard`-style header refresh icon or a "sync" action; smallest correct: toolbar refresh icon button dispatching `Refresh`).
- Month rollover logic centralizes in a pure helper so tests are deterministic:

```kotlin
// core/domain/helper/MonthArithmetic.kt
fun nextMonth(year: Int, month: Int): Pair<Int, Int> = if (month == 12) year + 1 to 1 else year to month + 1
fun previousMonth(year: Int, month: Int): Pair<Int, Int> = if (month == 1) year - 1 to 12 else year to month - 1
```

VM `onIntent` clamps navigation (no future months beyond current) and updates `_year`/`_month`. `selectedMonthLabel` derived in State.

**Tests (fake use cases + `kotlinx-coroutines-test`, `Dispatchers.setMain`):** initial load current month; forward/backward; year rollover both directions; refresh increments tick (re-collection); rapid consecutive month changes (only latest state survives — `flatMapLatest`); refresh during load.

---

# PHASE 6 — SplitsScreen effect collection

**Files:** `SplitsScreen.kt` (+ `SplitsViewModel` if effect surface needs adjusting)

- Standard collector: `LaunchedEffect(Unit) { viewModel.effect.collect { … } }` matching `AuthScreen.kt:40-47` pattern (Channel = one-shot, config-change safe).
- `GroupCreated` → invoke existing navigation callback (`onGroupCreated` param added, `App.kt` routes to GroupDetail or pops per existing UX — inspect `CreateGroupScreen` wiring at implementation time and mirror it).
- `Error` → snackbar via the same `SnackbarHostState` pattern other screens use (mirror `AddTransactionScreen`).

**Tests:** ViewModel-level — emit error → effect observed; GroupCreated emitted once.

---

# PHASE 7 — RFC4180 CSV parser

**Files:** `core/domain/helper/Csv.kt`, `features/profile/ImportCsvUseCase.kt` (+ tests)

Replace line-splitting with a document parser (RFC4180: quoted fields, escaped quotes `""`, embedded commas/newlines, CRLF + LF):

```kotlin
internal fun parseCsv(text: String): List<List<String>> {
    val rows = mutableListOf<List<String>>()
    val row = mutableListOf<String>(); val field = StringBuilder()
    var i = 0; var inQuotes = false
    fun endField() { row.add(field.toString()); field.clear() }
    fun endRow() { endField(); if (row.any { it.isNotEmpty() }) rows.add(row.toList()); row.clear() }
    while (i < text.length) {
        val c = text[i]
        when {
            inQuotes -> when {
                c == '"' && i + 1 < text.length && text[i+1] == '"' -> { field.append('"'); i++ }
                c == '"' -> inQuotes = false
                else -> field.append(c)
            }
            c == '"' -> inQuotes = true
            c == ',' -> endField()
            c == '\r' -> { if (i + 1 < text.length && text[i+1] == '\n') i++; endRow() }
            c == '\n' -> endRow()
            else -> field.append(c)
        }
        i++
    }
    if (field.isNotEmpty() || row.isNotEmpty()) endRow()
    return rows
}
```

`parseCsvLine` stays (used for header detection is replaced too — header detection becomes "first row's first field parses as LocalDate" over parsed rows). Import loop: rows → same validation, **plus** `amount > 0` required (domain rule: sign is conveyed by `TransactionType`; a non-positive amount is malformed → skipped and counted in `skipped`), unknown category auto-create unchanged (now dirty → syncs per Phase 2).

**Tests:** comma-in-quotes, quote-in-quotes, newline-in-quotes (LF+CRLF), empty trailing field, malformed unclosed quote (parses to EOF deterministically), export→import round trip incl. sanitized formula fields, negative/zero amount rejected, header/no-header.

---

# PHASE 8 — monthlyBudget total

**Files:** `DashboardViewModel.kt`, `GetBudgetPreviewUseCase.kt` (+ tests)

- `GetBudgetPreviewUseCase` gains an unbounded variant or a second flow: VM combines full budgets list for the total and slices 3 for the preview strip: `monthlyBudget = allBudgets.sumOf { it.limitAmount }.roundToCents()`, `budgets = allBudgets.take(3)`.

**Tests:** 0/1/3/5 budgets — total always full sum, strip ≤3.

---

# PHASE 9 — Splash reliability

**Files:** `App.kt` (+ test on the decision helper)

```kotlin
private val authDecision = async {
    runCatching { authRepo.observeAuthState().first { it !is AuthSessionState.Initializing } }
        .getOrElse { Napier.e("auth resolution failed", it); AuthSessionState.NotAuthenticated }
}
// with timeout:
val resolved = withTimeoutOrNull(AUTH_RESOLVE_TIMEOUT_MS) { authDecision.await() }
val target = when (resolved) {
    is AuthSessionState.Authenticated -> Config.Main
    else -> Config.Auth   // NotAuthenticated, null-timeout, or failure — Auth is the recoverable fallback
}
```

10s timeout (transient network will not misroute — session restore is local-first via `SettingsSessionManager`, so a signed-in user resolves from disk in ms; only a genuinely stuck session hits the timeout). Log every fallback branch. Splash minimum 2s unchanged. `root.replace*` only if current config differs (avoids the redundant recreate).

**Tests:** pure decision function: authenticated / not / delayed / failure / timeout (with `runTest` + virtual time).

---

# PHASE 10 — CancellationException sweep

Grep `catch` across all `.kt`; every handler that catches `Exception` (or broader) rethrows `CancellationException` first. Known: `ProfileViewModel` ×5. Also audit repo `withRepositoryErrorHandling` + `.catch{}` flow operators (flow `catch` does not intercept cancellation — verify, no change expected).

**Tests:** cancellation propagation test on one representative VM method (`runTest`/`launch`, cancel, assert no error-state emission).

---

# PHASE 11 — Analytics/Profile small bugs

1. `last_sync`: store ISO instant (`Clock.System.now().toString()`) in `AppPreferences`; display text derived (`"2 min ago"` helper — `relativeTime(instant)` pure fn).
2. `avgTransaction`: `totalOutflow / transactionCount` — AnalyticsState gains `transactionCount` from summary repo flow (extend `MonthlySummary` or count from existing flows; smallest: count in `observeMonthlySummary`'s map).
3. Settlement cap: `RecordSettlementUseCase` enforces `amount <= owed + EPSILON_CENTS` (throws domain `ValidationException` → surfaces via effect); dialog validates input (Phase 3 RPC double-checks server-side).
4. CSV amount validation: Phase 7.
5. Duplicate `AuthSuccess`: remove the direct send in `submit()`, rely on the `sessionStatus` collector for both paths (Google already does) — single emission path, no behavior change (`replaceAll` idempotent either way).

**Tests:** relativeTime boundaries; avgTransaction math; settlement cap accept/reject; single AuthSuccess emission.

---

# PHASE 12 — Dead code removal

Verified-then-remove (grep + compiler as truth): `GetSplitwiseBalanceUseCase.kt`, `GetSplitwiseFriendUseCase.kt`, `CreateSplitExpenseUseCase.kt` (empty files); `getSessionStatusString()` (interface + impl); `greeting()` → real time-of-day greeting (`Clock`-based, pure-tested fn — replaces stub rather than deleting UI call site); unused `error` fields in Dashboard/Budgets/Analytics states (Phase 5 keeps them only if a producer now exists — otherwise remove); `BudgetRepositoryImpl` private `BudgetDto` → use the public DTO (dedupe). Post-removal full compile + Koin start check (`koin.verify()`).

---

# PHASE 13 — Money handling decision

**Decision: keep `Double` + `roundToCents()` boundaries; harden + document.** Rationale: all monetary math already routes through `roundToCents()` or the Long-cents `distributeCentsEvenly`; a cents migration changes SQLDelight column types (REAL→INTEGER breaks migration path for existing rows — SQLite ALTER can't change type, needs table rebuild), remote column types (numeric→bigint with data migration), DTOs, both mappers, CSV format, and every UI format call site. For a single-user personal ledger with cent-rounding at every aggregation, `Double` is exact within magnitude < 2^53 cents; drift is structurally prevented. Hardening added:
- `ImportCsvUseCase`: `amount = raw.roundToCents()` on ingest.
- `AddTransactionViewModel`: numpad already limits format; add `roundToCents` before save.
- Document `docs/MoneyHandling.md`: representation, invariants, exactness argument, known ceiling (values ≥ 2^53 cents impossible via UI; ponytail-marked upgrade path to integer minor units).

**Tests:** roundToCents boundaries (.005, negative, large), sum-of-shares exactness after round-trip through Double.

---

# PHASE 14 — Security/RLS audit

- Verify every user-scoped table has RLS + owner policies: transactions, budgets, categories (new), split_groups/members/expenses/shares/settlements. Write `supabase/migrations/0004_rls_audit.sql` containing policy DDL only for gaps found; verify-by-inspection documented in `docs/SecurityAudit.md` (annot key in APK = by design, RLS-bounded; no service key in client; CSV injection protection regression-tested in Phase 7).
- Confirm new RPCs are `SECURITY INVOKER`, validate membership, and never bypass RLS.
- Live-database verification requires owner credentials — checklist delivered in the audit doc.

---

# PHASE 15 — Test suite

Files under `sharedUI/src/commonTest/kotlin/com/ryuken/obsidianledger/`:
- `core/domain/helper/HelpersTest.kt` — fix drift assertion (`assertNotEquals`-style check that `drifted` input differs / rounding removes drift).
- `core/domain/helper/CsvTest.kt` — Phase 7 matrix.
- `core/domain/helper/MonthArithmeticTest.kt`.
- `core/sync/SyncMergerTest.kt` — Phase 1 matrix.
- `core/domain/usecase/RecordSettlementUseCaseTest.kt`, `AddSplitExpenseUseCaseTest.kt` (fake repos).
- `features/dashboard/DashboardViewModelTest.kt`, `features/analytics/AnalyticsViewModelTest.kt` — Phase 5 cases (needs `kotlinx-coroutines-test` in `commonTest`; `Dispatchers.setMain`).
- `features/auth/AuthViewModelTest.kt` — validation + single-success-emission.
- `MoneyTest.kt`, `RelativeTimeTest.kt`, `GreetingTest.kt`.
Fakes: `FakeTransactionRepository`, `FakeBudgetRepository`, `FakeCategoryRepository`, `FakeAuthRepository` in `commonTest/.../fake/`.
No wall-clock (inject `Clock` where a VM needs it — Dashboard greeting takes `now` param), no network.

---

# PHASE 16 — Stress & failure simulation

Where driver/network allow (pure + JVM-unit level):
- `SyncMergerTest` with 10k random stamps — plan completes, no dupes, O(n) assertions.
- `CsvTest` large doc (10k rows incl. multiline) — parses, row count exact.
- Rapid month-change/refresh VM tests (Phase 5) already cover the recomposition races; effect-emission-during-recomposition covered by Channel semantics tests.
- Report as executed-simulated vs. not-executable-on-host (live Supabase, real device, WorkManager scheduling, Xcode) in final report.

---

## Execution order & validation gates

Each phase: implement → `./gradlew :sharedUI:testDebugUnitTest :sharedUI:verifySqlDelightMigration :sharedUI:compileKotlinIosSimulatorArm64 :androidApp:assembleDebug` → fix → commit (`fix:`/`feat:` per repo convention). Phase 4 adds `:sharedUI:compileKotlinIosArm64`. Final: full clean build + all tests + report.

## Risks

- SQLDelight migration verification on the pre-existing `1.sqm`/`.sq` mismatch may force normalization of `1.sqm` (inspected before edit).
- Remote migrations (0001–0004) are owner-applied; app code depending on RPC columns must not ship before they are applied — README ordering note + final-report warning.
- iOS Swift edits are not compilable on this host — flagged, minimal-diff, owner builds in Xcode.
- WorkManager 15-min cadence unchanged; new sign-in sync trigger is additive.
