package com.ryuken.obsidianledger.core.domain.usecase

import com.ryuken.obsidianledger.core.domain.model.SplitGroup
import com.ryuken.obsidianledger.core.domain.repository.SplitRepository
import kotlinx.coroutines.flow.Flow

class GetGroupsUseCase(
    private val repo: SplitRepository
) {
    operator fun invoke(userId: String): Flow<List<SplitGroup>> = repo.observeGroups(userId)
}
