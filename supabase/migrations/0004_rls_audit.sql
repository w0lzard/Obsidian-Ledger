-- 0004_rls_audit.sql
-- Canonical row-level-security posture for every user-scoped table.
--
-- This migration first ENABLES RLS and drops ALL existing policies on each table
-- (dynamic drop, so unknown/legacy policy names cannot survive), then recreates
-- the strict owner/membership policies. The end state is fully known regardless
-- of what posture the table had before. It never widens access: every policy
-- created here is owner- or membership-scoped.
--
-- Apply BEFORE shipping the app update. Idempotent — safe to re-run.

-- ═══════════════ helper: reset policies ═══════════════

CREATE OR REPLACE FUNCTION public.__reset_table_policies(p_table text)
RETURNS void LANGUAGE plpgsql AS $$
DECLARE r record;
BEGIN
    EXECUTE format('ALTER TABLE public.%I ENABLE ROW LEVEL SECURITY', p_table);
    FOR r IN
        SELECT policyname FROM pg_policies
        WHERE schemaname = 'public' AND tablename = p_table
    LOOP
        EXECUTE format('DROP POLICY %I ON public.%I', r.policyname, p_table);
    END LOOP;
END;
$$;

-- ═══════════════ transactions / budgets / categories ═══════════════
-- Simple owner tables: user_id = auth.uid() for every operation.

SELECT public.__reset_table_policies('transactions');
CREATE POLICY tx_select ON public.transactions FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY tx_insert ON public.transactions FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY tx_update ON public.transactions FOR UPDATE USING (auth.uid() = user_id);
CREATE POLICY tx_delete ON public.transactions FOR DELETE USING (auth.uid() = user_id);

SELECT public.__reset_table_policies('budgets');
CREATE POLICY bd_select ON public.budgets FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY bd_insert ON public.budgets FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY bd_update ON public.budgets FOR UPDATE USING (auth.uid() = user_id);
CREATE POLICY bd_delete ON public.budgets FOR DELETE USING (auth.uid() = user_id);

SELECT public.__reset_table_policies('categories');
CREATE POLICY cat_select ON public.categories FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY cat_insert ON public.categories FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY cat_update ON public.categories FOR UPDATE USING (auth.uid() = user_id);
CREATE POLICY cat_delete ON public.categories FOR DELETE USING (auth.uid() = user_id);

-- ═══════════════ split_groups ═══════════════
-- Visible to the creator and to anyone with a membership row tied to their auth.uid().

SELECT public.__reset_table_policies('split_groups');
CREATE POLICY sg_select ON public.split_groups FOR SELECT USING (
    created_by = auth.uid()
    OR EXISTS (
        SELECT 1 FROM public.split_group_members m
        WHERE m.group_id = split_groups.id AND m.user_id = auth.uid()
    )
);
CREATE POLICY sg_insert ON public.split_groups FOR INSERT WITH CHECK (created_by = auth.uid());
CREATE POLICY sg_update ON public.split_groups FOR UPDATE USING (created_by = auth.uid());
CREATE POLICY sg_delete ON public.split_groups FOR DELETE USING (created_by = auth.uid());

-- ═══════════════ split_group_members ═══════════════
-- Visible to the group creator and to authenticated members of the same group.
-- Writes are creator-only (member removal/editing is a group-owner action; the
-- create_split_group RPC inserts rows as the creating user).

SELECT public.__reset_table_policies('split_group_members');
CREATE POLICY sm_select ON public.split_group_members FOR SELECT USING (
    EXISTS (
        SELECT 1 FROM public.split_groups g
        WHERE g.id = split_group_members.group_id
          AND (g.created_by = auth.uid()
               OR EXISTS (
                   SELECT 1 FROM public.split_group_members me
                   WHERE me.group_id = g.id AND me.user_id = auth.uid()
               ))
    )
);
CREATE POLICY sm_insert ON public.split_group_members FOR INSERT WITH CHECK (
    EXISTS (
        SELECT 1 FROM public.split_groups g
        WHERE g.id = split_group_members.group_id AND g.created_by = auth.uid()
    )
);
CREATE POLICY sm_update ON public.split_group_members FOR UPDATE USING (
    EXISTS (
        SELECT 1 FROM public.split_groups g
        WHERE g.id = split_group_members.group_id AND g.created_by = auth.uid()
    )
);
CREATE POLICY sm_delete ON public.split_group_members FOR DELETE USING (
    EXISTS (
        SELECT 1 FROM public.split_groups g
        WHERE g.id = split_group_members.group_id AND g.created_by = auth.uid()
    )
);

-- ═══════════════ split_expenses ═══════════════
-- Visible to any member of the parent group; creatable by members; edit/delete
-- restricted to the group creator.

SELECT public.__reset_table_policies('split_expenses');
CREATE POLICY se_select ON public.split_expenses FOR SELECT USING (
    EXISTS (
        SELECT 1 FROM public.split_group_members m
        WHERE m.group_id = split_expenses.group_id AND m.user_id = auth.uid()
    )
);
CREATE POLICY se_insert ON public.split_expenses FOR INSERT WITH CHECK (
    EXISTS (
        SELECT 1 FROM public.split_group_members m
        WHERE m.group_id = split_expenses.group_id AND m.user_id = auth.uid()
    )
);
CREATE POLICY se_update ON public.split_expenses FOR UPDATE USING (
    EXISTS (
        SELECT 1 FROM public.split_groups g
        WHERE g.id = split_expenses.group_id AND g.created_by = auth.uid()
    )
);
CREATE POLICY se_delete ON public.split_expenses FOR DELETE USING (
    EXISTS (
        SELECT 1 FROM public.split_groups g
        WHERE g.id = split_expenses.group_id AND g.created_by = auth.uid()
    )
);

-- ═══════════════ split_expense_shares ═══════════════
-- No group_id column — visibility resolves through the parent expense's group.
-- The create_split_expense RPC inserts shares for the calling member.

SELECT public.__reset_table_policies('split_expense_shares');
CREATE POLICY ss_select ON public.split_expense_shares FOR SELECT USING (
    EXISTS (
        SELECT 1 FROM public.split_expenses e
        JOIN public.split_group_members m ON m.group_id = e.group_id
        WHERE e.id = split_expense_shares.expense_id AND m.user_id = auth.uid()
    )
);
CREATE POLICY ss_insert ON public.split_expense_shares FOR INSERT WITH CHECK (
    EXISTS (
        SELECT 1 FROM public.split_expenses e
        JOIN public.split_group_members m ON m.group_id = e.group_id
        WHERE e.id = split_expense_shares.expense_id AND m.user_id = auth.uid()
    )
);
CREATE POLICY ss_update ON public.split_expense_shares FOR UPDATE USING (
    EXISTS (
        SELECT 1 FROM public.split_expenses e
        JOIN public.split_groups g ON g.id = e.group_id
        WHERE e.id = split_expense_shares.expense_id AND g.created_by = auth.uid()
    )
);
CREATE POLICY ss_delete ON public.split_expense_shares FOR DELETE USING (
    EXISTS (
        SELECT 1 FROM public.split_expenses e
        JOIN public.split_groups g ON g.id = e.group_id
        WHERE e.id = split_expense_shares.expense_id AND g.created_by = auth.uid()
    )
);

-- ═══════════════ split_settlements ═══════════════

SELECT public.__reset_table_policies('split_settlements');
CREATE POLICY st_select ON public.split_settlements FOR SELECT USING (
    EXISTS (
        SELECT 1 FROM public.split_group_members m
        WHERE m.group_id = split_settlements.group_id AND m.user_id = auth.uid()
    )
);
CREATE POLICY st_insert ON public.split_settlements FOR INSERT WITH CHECK (
    EXISTS (
        SELECT 1 FROM public.split_group_members m
        WHERE m.group_id = split_settlements.group_id AND m.user_id = auth.uid()
    )
);
CREATE POLICY st_update ON public.split_settlements FOR UPDATE USING (
    EXISTS (
        SELECT 1 FROM public.split_groups g
        WHERE g.id = split_settlements.group_id AND g.created_by = auth.uid()
    )
);
CREATE POLICY st_delete ON public.split_settlements FOR DELETE USING (
    EXISTS (
        SELECT 1 FROM public.split_groups g
        WHERE g.id = split_settlements.group_id AND g.created_by = auth.uid()
    )
);

DROP FUNCTION public.__reset_table_policies(text);
