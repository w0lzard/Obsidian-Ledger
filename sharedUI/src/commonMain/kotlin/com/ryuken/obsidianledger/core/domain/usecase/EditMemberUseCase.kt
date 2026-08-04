package com.ryuken.obsidianledger.core.domain.usecase

import com.ryuken.obsidianledger.core.domain.repository.SplitRepository

class EditMemberUseCase(
    private val repo: SplitRepository
) {
    suspend operator fun invoke(memberId: String, displayName: String) =
        repo.editMember(memberId, displayName)
}
