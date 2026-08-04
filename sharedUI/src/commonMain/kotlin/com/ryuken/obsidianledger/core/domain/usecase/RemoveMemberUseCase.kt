package com.ryuken.obsidianledger.core.domain.usecase

import com.ryuken.obsidianledger.core.domain.repository.SplitRepository

class RemoveMemberUseCase(
    private val repo: SplitRepository
) {
    suspend operator fun invoke(memberId: String) = repo.removeMember(memberId)
}
