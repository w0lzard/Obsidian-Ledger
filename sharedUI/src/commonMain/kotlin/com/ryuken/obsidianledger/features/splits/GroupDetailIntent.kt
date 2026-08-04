package com.ryuken.obsidianledger.features.splits

sealed interface GroupDetailIntent {
    data object Refresh : GroupDetailIntent
    data class EditMemberClick(val memberId: String) : GroupDetailIntent
    data object DismissEditMember : GroupDetailIntent
    data class ConfirmEditMember(val displayName: String) : GroupDetailIntent
    data class RemoveMember(val memberId: String) : GroupDetailIntent
    data class SettleUpClick(val memberId: String) : GroupDetailIntent
    data object DismissSettleUp : GroupDetailIntent
    data class ConfirmSettleUp(val counterpartyMemberId: String, val amount: Double, val iPaid: Boolean) : GroupDetailIntent
    data class SendPaymentRequest(val memberId: String) : GroupDetailIntent
}

sealed interface GroupDetailEffect {
    data object MemberUpdated : GroupDetailEffect
    data object SettlementRecorded : GroupDetailEffect
    data object PaymentRequestSent : GroupDetailEffect
    data class Error(val message: String) : GroupDetailEffect
}
