package com.ryuken.obsidianledger.core.domain.usecase

import com.ryuken.obsidianledger.core.domain.model.MemberBalance
import com.ryuken.obsidianledger.core.domain.repository.SplitRepository
import kotlinx.coroutines.flow.Flow

class GetGroupBalancesUseCase(
    private val repo: SplitRepository
) {
    operator fun invoke(groupId: String): Flow<List<MemberBalance>> = repo.observeBalances(groupId)
}
