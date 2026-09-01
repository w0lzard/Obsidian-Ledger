# Security Audit — 2026-09-01 remediation pass

## Posture after migrations 0001–0004

| Table | RLS | Policy model |
|---|---|---|
| `transactions` | enabled (0004) | owner-only (`user_id = auth.uid()`), all four ops |
| `budgets` | enabled (0004) | owner-only |
| `categories` | enabled (0002, reaffirmed 0004) | owner-only; custom categories per user |
| `split_groups` | enabled (0004) | creator + joined members (SELECT); creator-only writes |
| `split_group_members` | enabled (0004) | same-group visibility; group-creator-only writes |
| `split_expenses` | enabled (0004) | group members SELECT/INSERT; creator-only UPDATE/DELETE |
| `split_expense_shares` | enabled (0004) | membership via parent expense; creator-only writes |
| `split_settlements` | enabled (0004) | group members SELECT/INSERT; creator-only writes |

0004 is written as **reset-then-create**: it enables RLS, dynamically drops every
existing policy on each table (unknown legacy names cannot survive), and recreates
the canonical policies above — so the end state is fully known even if a table was
accidentally left permissive. No policy widens access; RLS is only ever tightened.

## RPCs (0003)

`create_split_group`, `create_split_expense`, `record_split_settlement` are
`SECURITY INVOKER` — they run under the caller's JWT, so every statement inside
executes under the RLS policies above. They validate membership, positive amounts,
exact share sums, and the pairwise settlement cap. No `SECURITY DEFINER`, no
bypass. The older `handle_new_user()` trigger is `SECURITY DEFINER` by design
(runs on auth insert) with the `search_path` fix from 2026-08-04 intact.

## Client-side findings (code-verified)

- **CSV formula injection**: protected. `sanitizeCsvFormulaInjection` prefixes
  `'` on `= + - @ \t \r` for category/note on export; import reverses exactly
  (round-trip tested, no quote accumulation). Regression tests in `CsvTest` +
  `ImportExportRoundTripTest`.
- **Anon key**: only the Supabase URL + anon publishable key + Google web client
  id are compiled into `BuildConfig` — standard for Supabase clients; exposure is
  bounded by RLS. No service-role key anywhere in the repo (grep-verified).
- **Secrets**: `local.properties` untracked; `RESEND_API_KEY` plist entry is a
  `$(RESEND_API_KEY)` build-variable placeholder with no committed value. The
  Resend key is never in the Kotlin client — mail sends go through the
  `quick-responder` Edge Function holding the key server-side.
- **Session at rest**: Android = EncryptedSharedPreferences + `allowBackup=false`;
  iOS = Keychain (`KeychainSettings`, Phase 4).
- **PII logging**: Napier is debug-gated on Android (`BuildConfig.DEBUG`) and
  debug-binary-gated on iOS (`Platform.isDebugBinary`). Release builds log nothing.
- **Input validation**: auth (blank checks + Supabase enforcement), transaction
  amounts (numpad format + positivity + cent rounding), settlement amounts
  (client + RPC cap), CSV import (RFC4180 parse, date/type/category/amount
  validation, positive-only amounts).

## Owner verification checklist (requires Supabase dashboard/SQL access)

This repo has no service-role credentials, so the following must be confirmed in
the dashboard after applying 0001–0004:

1. `select * from pg_policies` shows only the canonical policies (the 0004 reset
   guarantees this if applied cleanly).
2. `select relrowsecurity from pg_class where relname in (...)` — all true.
3. Edge Function `quick-responder` authenticates the caller (JWT check) before
   sending email and holds the Resend key in its own secrets — not in code.
4. `auth.users` → `profiles` trigger `handle_new_user` still carries
   `SET search_path TO public` and schema-qualified inserts (2026-08-04 fix).
5. PostgREST `db-max-rows` note: the pull manifest paginates at 1000 rows/page,
   so any server cap ≥ 1000 is safe.
