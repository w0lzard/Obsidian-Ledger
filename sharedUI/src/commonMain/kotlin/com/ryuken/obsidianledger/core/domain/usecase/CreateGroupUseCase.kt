package com.ryuken.obsidianledger.core.domain.usecase

import com.ryuken.obsidianledger.core.domain.model.SplitGroup
import com.ryuken.obsidianledger.core.domain.repository.SplitRepository

class CreateGroupUseCase(
    private val repo: SplitRepository
) {
    suspend operator fun invoke(
        name: String,
        createdBy: String,
        creatorDisplayName: String,
        memberNames: List<String>
    ): SplitGroup = repo.createGroup(name, createdBy, creatorDisplayName, memberNames)
}
