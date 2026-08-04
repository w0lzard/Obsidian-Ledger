package com.ryuken.obsidianledger.core.domain.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class SplitGroupDto(
    val id: String,
    val name: String,
    @SerialName("created_by") val createdBy: String,
    @SerialName("created_at") val createdAt: String
)

@Serializable
internal data class GroupMemberDto(
    val id: String,
    @SerialName("group_id") val groupId: String,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("display_name") val displayName: String,
    val email: String? = null
)

@Serializable
internal data class SplitExpenseDto(
    val id: String,
    @SerialName("group_id") val groupId: String,
    val description: String,
    val amount: Double,
    @SerialName("paid_by_member_id") val paidByMemberId: String,
    @SerialName("expense_date") val expenseDate: String,
    @SerialName("created_at") val createdAt: String
)

@Serializable
internal data class SplitExpenseShareDto(
    val id: String,
    @SerialName("expense_id") val expenseId: String,
    @SerialName("member_id") val memberId: String,
    val amount: Double
)

@Serializable
internal data class SplitSettlementDto(
    val id: String,
    @SerialName("group_id") val groupId: String,
    @SerialName("from_member_id") val fromMemberId: String,
    @SerialName("to_member_id") val toMemberId: String,
    val amount: Double,
    @SerialName("settled_date") val settledDate: String,
    @SerialName("created_at") val createdAt: String
)
