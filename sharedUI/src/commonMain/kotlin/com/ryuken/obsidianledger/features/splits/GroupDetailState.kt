package com.ryuken.obsidianledger.features.splits

import com.ryuken.obsidianledger.core.domain.model.GroupMember
import com.ryuken.obsidianledger.core.domain.model.MemberBalance
import com.ryuken.obsidianledger.core.domain.model.SplitExpense
import com.ryuken.obsidianledger.core.domain.model.SplitGroup

data class GroupDetailState(
    val group: SplitGroup? = null,
    val balances: List<MemberBalance> = emptyList(),
    val expenses: List<SplitExpense> = emptyList(),
    val currentUserId: String = "",
    val isLoading: Boolean = true,
    val editingMember: GroupMember? = null,
    val settlingWithMemberId: String? = null,
    val sendingRequestForMemberId: String? = null
) {
    val currentUserMemberId: String?
        get() = group?.members?.firstOrNull { it.userId == currentUserId }?.id
}
