-- 0001_bidirectional_sync.sql
-- Enables the bidirectional sync contract (see docs/SyncContract.md).
--
-- Run order: apply this BEFORE shipping the app update that calls pullRemote().
-- Idempotent — safe to re-run.
--
-- What it does:
--   1. budgets gains server-maintained created_at/updated_at (transactions already has
--      updated_at from the original schema).
--   2. BEFORE UPDATE triggers make updated_at SERVER-AUTHORITATIVE: clients can send
--      their own updated_at but the server overwrites it with now() on every write, so
--      device clocks are never compared.
--   3. Per-user indexes for the pull manifest queries.

ALTER TABLE public.budgets
    ADD COLUMN IF NOT EXISTS updated_at timestamptz NOT NULL DEFAULT now();
ALTER TABLE public.budgets
    ADD COLUMN IF NOT EXISTS created_at timestamptz NOT NULL DEFAULT now();

CREATE OR REPLACE FUNCTION public.touch_updated_at() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_budgets_touch ON public.budgets;
CREATE TRIGGER trg_budgets_touch BEFORE UPDATE ON public.budgets
    FOR EACH ROW EXECUTE FUNCTION public.touch_updated_at();

DROP TRIGGER IF EXISTS trg_transactions_touch ON public.transactions;
CREATE TRIGGER trg_transactions_touch BEFORE UPDATE ON public.transactions
    FOR EACH ROW EXECUTE FUNCTION public.touch_updated_at();

CREATE INDEX IF NOT EXISTS idx_transactions_user ON public.transactions (user_id);
CREATE INDEX IF NOT EXISTS idx_budgets_user ON public.budgets (user_id);
