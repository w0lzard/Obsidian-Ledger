package com.ryuken.obsidianledger.core.domain.usecase

import com.ryuken.obsidianledger.core.domain.model.MemberBalance
import com.ryuken.obsidianledger.fake.FakeCategoryRepository
import com.ryuken.obsidianledger.fake.FakeSplitRepository
import com.ryuken.obsidianledger.fake.FakeTransactionRepository
import com.ryuken.obsidianledger.features.expenses.AddTransactionUseCase
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class RecordSettlementUseCaseTest {

    private val date = LocalDate.parse("2026-08-20")

    private fun useCaseWith(payerNet: Double): Pair<RecordSettlementUseCase, FakeSplitRepository> {
        val splitRepo = FakeSplitRepository().apply {
            balancesByGroup.value = mapOf(
                "g1" to listOf(MemberBalance(memberId = "me", displayName = "Me", email = null, netAmount = payerNet))
            )
        }
        val useCase = RecordSettlementUseCase(
            repo          = splitRepo,
            addTransaction = AddTransactionUseCase(FakeTransactionRepository()),
            categoryRepo  = FakeCategoryRepository()
        )
        return useCase to splitRepo
    }

    @Test
    fun settlementWithinOutstanding_passes() = runBlocking {
        // I owe 50 (net -50): settling 50 is allowed.
        val (useCase, splitRepo) = useCaseWith(payerNet = -50.0)
        useCase(
            groupId = "g1", fromMemberId = "me", toMemberId = "them",
            amount = 50.0, date = date,
            currentUserId = "user-1", currentUserIsPayer = true, currentUserIsReceiver = false
        )
        assertEquals(1, splitRepo.settlements.size)
    }

    @Test
    fun settlementAboveOutstanding_rejected() = runBlocking {
        val (useCase, splitRepo) = useCaseWith(payerNet = -50.0)
        assertFailsWith<IllegalArgumentException> {
            useCase(
                groupId = "g1", fromMemberId = "me", toMemberId = "them",
                amount = 75.0, date = date,
                currentUserId = "user-1", currentUserIsPayer = true, currentUserIsReceiver = false
            )
        }
        assertTrue(splitRepo.settlements.isEmpty())
    }

    @Test
    fun zeroOrNegative_rejected() = runBlocking {
        val (useCase, _) = useCaseWith(payerNet = -50.0)
        val ex = assertFailsWith<IllegalArgumentException> {
            useCase(
                groupId = "g1", fromMemberId = "me", toMemberId = "them",
                amount = 0.0, date = date,
                currentUserId = "user-1", currentUserIsPayer = true, currentUserIsReceiver = false
            )
        }
        assertTrue(ex.message!!.contains("positive"))
    }

    @Test
    fun settlingWhenPayerIsOwed_rejected() = runBlocking {
        // Net +50 means the payer is OWED money — nothing to settle from their side.
        val (useCase, splitRepo) = useCaseWith(payerNet = 50.0)
        assertFailsWith<IllegalArgumentException> {
            useCase(
                groupId = "g1", fromMemberId = "me", toMemberId = "them",
                amount = 20.0, date = date,
                currentUserId = "user-1", currentUserIsPayer = true, currentUserIsReceiver = false
            )
        }
        assertTrue(splitRepo.settlements.isEmpty())
    }
}
