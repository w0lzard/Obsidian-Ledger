package com.ryuken.obsidianledger.features.splits

import com.ryuken.obsidianledger.core.domain.model.SplitGroup

data class SplitsState(
    val groups: List<SplitGroup> = emptyList(),
    val isLoading: Boolean = true,
    val isCreatingGroup: Boolean = false
)
