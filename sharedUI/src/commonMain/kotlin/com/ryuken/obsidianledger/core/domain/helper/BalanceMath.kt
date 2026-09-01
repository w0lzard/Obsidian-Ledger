package com.ryuken.obsidianledger.core.domain.helper

import com.ryuken.obsidianledger.core.domain.model.GroupMember
import com.ryuken.obsidianledger.core.domain.model.MemberBalance
import com.ryuken.obsidianledger.core.domain.model.Settlement
import com.ryuken.obsidianledger.core.domain.model.SplitExpense

/**
 * Pure group balance math, extracted from SplitRepositoryImpl so the ledger rules
 * are testable without a repository: payer +amount, each share −amount, settlements
 * adjust from/to, per-member net rounded to cents. Positive net = member is owed money.
 */
fun computeBalances(
    members     : List<GroupMember>,
    expenses    : List<SplitExpense>,
    settlements : List<Settlement>
): List<MemberBalance> {
    val net = mutableMapOf<String, Double>()
    members.forEach { net[it.id] = 0.0 }

    expenses.forEach { expense ->
        net[expense.paidByMemberId] = (net[expense.paidByMemberId] ?: 0.0) + expense.amount
        expense.shares.forEach { share ->
            net[share.memberId] = (net[share.memberId] ?: 0.0) - share.amount
        }
    }
    settlements.forEach { settlement ->
        net[settlement.fromMemberId] = (net[settlement.fromMemberId] ?: 0.0) + settlement.amount
        net[settlement.toMemberId] = (net[settlement.toMemberId] ?: 0.0) - settlement.amount
    }

    return members.map { member ->
        MemberBalance(
            memberId    = member.id,
            displayName = member.displayName,
            email       = member.email,
            netAmount   = (net[member.id] ?: 0.0).roundToCents()
        )
    }
}
