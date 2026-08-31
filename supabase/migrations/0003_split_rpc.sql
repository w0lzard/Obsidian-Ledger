-- 0003_split_rpc.sql
-- Server-side atomicity for multi-table split operations + database-level invariants.
--
-- Group creation (group + members), expense creation (expense + shares) and
-- settlement recording were previously multiple client-side INSERTs with
-- best-effort compensation deletes. A network drop between the inserts left
-- partial groups / shareless expenses. These RPCs make each operation a single
-- transaction with server-side validation; RLS still applies (SECURITY INVOKER).
--
-- Apply BEFORE shipping the app update that calls these RPCs.
-- Idempotent for triggers/indexes/constraints; functions are replaced.

-- ═══════════════════════════════════════════════════════════════════════
-- Structural constraints
-- ═══════════════════════════════════════════════════════════════════════

DO $$ BEGIN
  ALTER TABLE public.split_expense_shares
    ADD CONSTRAINT split_expense_shares_expense_id_fkey
    FOREIGN KEY (expense_id) REFERENCES public.split_expenses(id) ON DELETE CASCADE;
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
  ALTER TABLE public.split_expense_shares
    ADD CONSTRAINT split_expense_shares_member_id_fkey
    FOREIGN KEY (member_id) REFERENCES public.split_group_members(id) ON DELETE CASCADE;
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
  ALTER TABLE public.split_expense_shares
    ADD CONSTRAINT split_expense_shares_amount_positive CHECK (amount > 0);
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
  ALTER TABLE public.split_expenses
    ADD CONSTRAINT split_expenses_group_id_fkey
    FOREIGN KEY (group_id) REFERENCES public.split_groups(id) ON DELETE CASCADE;
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
  ALTER TABLE public.split_expenses
    ADD CONSTRAINT split_expenses_paid_by_member_fkey
    FOREIGN KEY (paid_by_member_id) REFERENCES public.split_group_members(id) ON DELETE RESTRICT;
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
  ALTER TABLE public.split_expenses
    ADD CONSTRAINT split_expenses_amount_positive CHECK (amount > 0);
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
  ALTER TABLE public.split_group_members
    ADD CONSTRAINT split_group_members_group_id_fkey
    FOREIGN KEY (group_id) REFERENCES public.split_groups(id) ON DELETE CASCADE;
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
  ALTER TABLE public.split_settlements
    ADD CONSTRAINT split_settlements_group_id_fkey
    FOREIGN KEY (group_id) REFERENCES public.split_groups(id) ON DELETE CASCADE;
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
  ALTER TABLE public.split_settlements
    ADD CONSTRAINT split_settlements_amount_positive CHECK (amount > 0);
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

CREATE INDEX IF NOT EXISTS idx_split_expenses_group ON public.split_expenses (group_id);
CREATE INDEX IF NOT EXISTS idx_split_expense_shares_expense ON public.split_expense_shares (expense_id);
CREATE INDEX IF NOT EXISTS idx_split_expense_shares_member ON public.split_expense_shares (member_id);
CREATE INDEX IF NOT EXISTS idx_split_settlements_group ON public.split_settlements (group_id);
CREATE INDEX IF NOT EXISTS idx_split_members_group ON public.split_group_members (group_id);

-- ═══════════════════════════════════════════════════════════════════════
-- create_split_group(name, creator display name, placeholder member names)
-- Atomically inserts the group, the creator's membership row, and one
-- placeholder member per name. Returns the full group JSON (matching the
-- split_groups + split_group_members shape the client already decodes).
-- ═══════════════════════════════════════════════════════════════════════

CREATE OR REPLACE FUNCTION public.create_split_group(
    p_name                  text,
    p_creator_display_name  text,
    p_member_names          text[]
) RETURNS jsonb
LANGUAGE plpgsql
SECURITY INVOKER
AS $$
DECLARE
    v_group  uuid := gen_random_uuid();
    v_now   timestamptz := now();
    v_members jsonb;
BEGIN
    IF coalesce(btrim(p_name), '') = '' THEN
        RAISE EXCEPTION 'Group name must not be blank';
    END IF;

    INSERT INTO public.split_groups (id, name, created_by, created_at)
    VALUES (v_group, btrim(p_name), auth.uid(), v_now);

    WITH ins AS (
        INSERT INTO public.split_group_members (id, group_id, user_id, display_name)
        SELECT gen_random_uuid(), v_group, auth.uid(), p_creator_display_name
        UNION ALL
        SELECT gen_random_uuid(), v_group, NULL, m
        FROM unnest(p_member_names) AS m
        WHERE coalesce(btrim(m), '') <> ''
        RETURNING id, group_id, user_id, display_name, email
    )
    SELECT coalesce(jsonb_agg(to_jsonb(ins)), '[]'::jsonb) INTO v_members FROM ins;

    RETURN jsonb_build_object(
        'id',         v_group,
        'name',       btrim(p_name),
        'created_by', auth.uid(),
        'created_at', v_now,
        'members',    v_members
    );
END;
$$;

-- ═══════════════════════════════════════════════════════════════════════
-- create_split_expense(group, description, amount, payer, participants,
--                      share amounts, date)
-- Atomically inserts the expense and its shares. Validates: group exists and
-- caller can see it (RLS), payer and every participant are members of the
-- group, amount > 0, and shares sum exactly to the amount.
-- ═══════════════════════════════════════════════════════════════════════

CREATE OR REPLACE FUNCTION public.create_split_expense(
    p_group_id       uuid,
    p_description    text,
    p_amount         numeric,
    p_paid_by        uuid,
    p_member_ids     uuid[],
    p_share_amounts  numeric[],
    p_date           date
) RETURNS jsonb
LANGUAGE plpgsql
SECURITY INVOKER
AS $$
DECLARE
    v_expense uuid := gen_random_uuid();
    v_now    timestamptz := now();
    v_group  record;
    v_shares jsonb;
BEGIN
    IF p_amount <= 0 THEN
        RAISE EXCEPTION 'Expense amount must be positive';
    END IF;
    IF p_member_ids IS NULL OR cardinality(p_member_ids) = 0 THEN
        RAISE EXCEPTION 'Split expense requires at least one participant';
    END IF;
    IF cardinality(p_member_ids) <> cardinality(p_share_amounts) THEN
        RAISE EXCEPTION 'participant/share array length mismatch';
    END IF;
    IF (SELECT round(SUM(s), 2) FROM unnest(p_share_amounts) s) <> round(p_amount, 2) THEN
        RAISE EXCEPTION 'Share amounts must sum exactly to the expense amount';
    END IF;

    SELECT * INTO v_group FROM public.split_groups WHERE id = p_group_id;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Split group not found';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM public.split_group_members WHERE id = p_paid_by AND group_id = p_group_id) THEN
        RAISE EXCEPTION 'Payer is not a member of this group';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM unnest(p_member_ids) AS m
        WHERE NOT EXISTS (
            SELECT 1 FROM public.split_group_members
            WHERE id = m AND group_id = p_group_id
        )
    ) THEN
        RAISE EXCEPTION 'All participants must be members of this group';
    END IF;

    INSERT INTO public.split_expenses (id, group_id, description, amount, paid_by_member_id, expense_date, created_at)
    VALUES (v_expense, p_group_id, p_description, round(p_amount, 2), p_paid_by, p_date, v_now);

    WITH ins AS (
        INSERT INTO public.split_expense_shares (id, expense_id, member_id, amount)
        SELECT gen_random_uuid(), v_expense, m, round(s, 2)
        FROM unnest(p_member_ids, p_share_amounts) AS t(m, s)
        RETURNING id, expense_id, member_id, amount
    )
    SELECT coalesce(jsonb_agg(to_jsonb(ins)), '[]'::jsonb) INTO v_shares FROM ins;

    RETURN jsonb_build_object(
        'id',               v_expense,
        'group_id',         p_group_id,
        'description',      p_description,
        'amount',           round(p_amount, 2),
        'paid_by_member_id', p_paid_by,
        'expense_date',     p_date,
        'created_at',       v_now,
        'shares',           v_shares
    );
END;
$$;

-- ═══════════════════════════════════════════════════════════════════════
-- record_split_settlement(group, from, to, amount, date)
-- Inserts a settlement atomically with server-side validation: both members
-- belong to the group, amount > 0, and the amount does not exceed the
-- pairwise outstanding balance (shares owed between the pair minus net
-- settlements already recorded between them).
-- ═══════════════════════════════════════════════════════════════════════

CREATE OR REPLACE FUNCTION public.record_split_settlement(
    p_group_id  uuid,
    p_from      uuid,
    p_to        uuid,
    p_amount    numeric,
    p_date      date
) RETURNS jsonb
LANGUAGE plpgsql
SECURITY INVOKER
AS $$
DECLARE
    v_settlement uuid := gen_random_uuid();
    v_now       timestamptz := now();
    v_outstanding numeric;
BEGIN
    IF p_from = p_to THEN
        RAISE EXCEPTION 'Settlement parties must differ';
    END IF;
    IF p_amount <= 0 THEN
        RAISE EXCEPTION 'Settlement amount must be positive';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM public.split_group_members WHERE id = p_from AND group_id = p_group_id)
       OR NOT EXISTS (SELECT 1 FROM public.split_group_members WHERE id = p_to AND group_id = p_group_id) THEN
        RAISE EXCEPTION 'Both settlement parties must be members of the group';
    END IF;

    -- Pairwise outstanding: what p_from still owes p_to.
    SELECT
        COALESCE((SELECT SUM(s.amount) FROM public.split_expense_shares s
                  JOIN public.split_expenses e ON e.id = s.expense_id
                  WHERE e.group_id = p_group_id AND e.paid_by_member_id = p_to AND s.member_id = p_from), 0)
      - COALESCE((SELECT SUM(s.amount) FROM public.split_expense_shares s
                  JOIN public.split_expenses e ON e.id = s.expense_id
                  WHERE e.group_id = p_group_id AND e.paid_by_member_id = p_from AND s.member_id = p_to), 0)
      - COALESCE((SELECT SUM(st.amount) FROM public.split_settlements st
                  WHERE st.group_id = p_group_id AND st.from_member_id = p_from AND st.to_member_id = p_to), 0)
      + COALESCE((SELECT SUM(st.amount) FROM public.split_settlements st
                  WHERE st.group_id = p_group_id AND st.from_member_id = p_to AND st.to_member_id = p_from), 0)
      INTO v_outstanding;

    IF round(p_amount, 2) > round(v_outstanding, 2) THEN
        RAISE EXCEPTION 'Settlement amount % exceeds outstanding balance %', round(p_amount, 2), round(v_outstanding, 2);
    END IF;

    INSERT INTO public.split_settlements (id, group_id, from_member_id, to_member_id, amount, settled_date, created_at)
    VALUES (v_settlement, p_group_id, p_from, p_to, round(p_amount, 2), p_date, v_now);

    RETURN jsonb_build_object(
        'id',              v_settlement,
        'group_id',        p_group_id,
        'from_member_id',  p_from,
        'to_member_id',    p_to,
        'amount',          round(p_amount, 2),
        'settled_date',    p_date,
        'created_at',      v_now
    );
END;
$$;
