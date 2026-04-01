package com.ryuken.obsidianledger.core.domain.model

data class Category(
    val id : String,
    val name : String,
    val emoji : String,
    val colorHex : String = "#00C896",
    val isCustom : Boolean = false
)
