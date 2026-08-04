package com.ryuken.obsidianledger.core.domain.usecase

import com.ryuken.obsidianledger.core.network.ResendEmailService

class SendPaymentRequestUseCase(
    private val emailService: ResendEmailService
) {
    suspend operator fun invoke(
        toEmail: String,
        toName: String,
        fromUserName: String,
        amount: Double,
        groupName: String,
        breakdown: List<Pair<String, Double>>
    ): Result<Unit> = emailService.sendPaymentRequest(toEmail, toName, fromUserName, amount, groupName, breakdown)
}
