-- 0002_categories_sync.sql
-- User-scoped categories table for bidirectional sync of CUSTOM categories.
-- Default categories (cat_food, cat_transport, …) are never stored remotely: their
-- ids are hardcoded in the client and identical on every device, so remote rows
-- would be pure duplication. Only user-created categories sync.
--
-- Apply BEFORE shipping the app update that calls the categories push/pull.
-- Idempotent — safe to re-run.

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

DROP POLICY IF EXISTS cat_own_select ON public.categories;
CREATE POLICY cat_own_select ON public.categories
    FOR SELECT USING (auth.uid() = user_id);
DROP POLICY IF EXISTS cat_own_insert ON public.categories;
CREATE POLICY cat_own_insert ON public.categories
    FOR INSERT WITH CHECK (auth.uid() = user_id);
DROP POLICY IF EXISTS cat_own_update ON public.categories;
CREATE POLICY cat_own_update ON public.categories
    FOR UPDATE USING (auth.uid() = user_id);
DROP POLICY IF EXISTS cat_own_delete ON public.categories;
CREATE POLICY cat_own_delete ON public.categories
    FOR DELETE USING (auth.uid() = user_id);

-- Server-authoritative updated_at, same contract as transactions/budgets (0001).
DROP TRIGGER IF EXISTS trg_categories_touch ON public.categories;
CREATE TRIGGER trg_categories_touch BEFORE UPDATE ON public.categories
    FOR EACH ROW EXECUTE FUNCTION public.touch_updated_at();

CREATE INDEX IF NOT EXISTS idx_categories_user ON public.categories (user_id);
