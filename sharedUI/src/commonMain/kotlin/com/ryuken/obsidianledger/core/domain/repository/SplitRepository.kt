package com.ryuken.obsidianledger.core.domain.repository

import com.ryuken.obsidianledger.core.domain.model.MemberBalance
import com.ryuken.obsidianledger.core.domain.model.Settlement
import com.ryuken.obsidianledger.core.domain.model.SplitExpense
import com.ryuken.obsidianledger.core.domain.model.SplitGroup
import com.ryuken.obsidianledger.core.domain.model.SplitShare
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

interface SplitRepository {
    fun observeGroups(userId: String): Flow<List<SplitGroup>>
    fun observeGroup(groupId: String): Flow<SplitGroup>

    suspend fun createGroup(
        name: String,
        createdBy: String,
        creatorDisplayName: String,
        memberNames: List<String>
    ): SplitGroup

    suspend fun editMember(memberId: String, displayName: String)
    suspend fun removeMember(memberId: String)

    fun observeExpenses(groupId: String): Flow<List<SplitExpense>>

    suspend fun addExpense(
        groupId: String,
        description: String,
        amount: Double,
        paidByMemberId: String,
        date: LocalDate,
        shares: List<SplitShare>
    ): SplitExpense

    fun observeBalances(groupId: String): Flow<List<MemberBalance>>

    suspend fun recordSettlement(
        groupId: String,
        fromMemberId: String,
        toMemberId: String,
        amount: Double,
        date: LocalDate
    ): Settlement
}
