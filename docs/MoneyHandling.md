# Money Handling

## Decision

Monetary values are represented as **`Double` decimal units with mandatory
`roundToCents()` at every conversion boundary** (Helper: `roundToCents`,
`toCents`). This is a deliberate, examined tradeoff — not an oversight.

A full migration to integer minor units (cents as `Long`) was considered and
rejected for now:

- SQLDelight columns are `REAL`; SQLite `ALTER TABLE` cannot change a column
  type, so a migration means per-table rebuild-with-copy on every device.
- The remote Postgres columns are `numeric`/`double precision`; they would need a
  data migration for every existing row plus coordinated DTO changes.
- Every aggregation, mapper, CSV format string, split distribution, and UI format
  call site would change — a wide, risky diff for a single-user personal ledger.

## Why `Double` is safe here

- `Double` represents every integer up to 2^53 exactly. Cent values (any realistic
  personal-finance magnitude) round-trip through `roundToCents()` without drift:
  `round(x * 100) / 100.0` is stable under re-rounding.
- All aggregation points round at their boundary: monthly summary sums, category
  breakdowns, dashboard totals, budget `spent`, split net balances.
- `distributeCentsEvenly` operates entirely in **integer cents** (`Long`):
  `total.toCents()` split by largest-remainder, summed back exactly — the
  split-share invariant (Σ shares == amount) is exact, not approximate.
- The split RPCs re-verify `Σ shares = amount` in Postgres `numeric` (exact
  decimal) server-side.

## Hardened boundaries (the invariant list)

| Boundary | Enforcement |
|---|---|
| Keyboard ingest (AddTransaction) | `amountDouble.roundToCents()` before storage |
| CSV import | positive check + `roundToCents()` on parse |
| Budget spent (SQL `SUM` over REAL) | `roundToCents()` at read |
| Monthly summary / category totals | `roundToCents()` at aggregation |
| Split balances (`computeBalances`) | `roundToCents()` per member net |
| Settlements | `roundToCents()` on write; server-side pairwise cap |
| Split shares | integer-cents distribution + server `numeric` check |

## Known ceiling

Values at or above 2^53 cents (~90 trillion currency units) cannot be represented
exactly. The numpad caps input at 9 characters, so the UI cannot produce such a
value; a hand-crafted CSV row could, and would round — acceptable for this domain.
// ponytail: if multi-currency or shared-household ledgers ever demand exact
// integer accounting, migrate storage to minor units end-to-end; this document
// is the map of every boundary that must change.
