package com.ryuken.obsidianledger.core.domain.helper

import com.ryuken.obsidianledger.core.domain.model.GroupMember
import com.ryuken.obsidianledger.core.domain.model.MemberBalance
import com.ryuken.obsidianledger.core.domain.model.Settlement
import com.ryuken.obsidianledger.core.domain.model.SplitExpense
import com.ryuken.obsidianledger.core.domain.model.SplitShare
import kotlinx.datetime.LocalDate
import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class BalanceMathTest {

    private val date = LocalDate.parse("2026-08-01")
    private val created = Instant.parse("2026-08-01T10:00:00Z")

    private fun member(id: String) = GroupMember(id = id, groupId = "g", userId = null, displayName = id, email = null)

    private fun expense(payer: String, amount: Double, shares: List<Pair<String, Double>>) = SplitExpense(
        id = "e-$payer-$amount", groupId = "g", description = "x", amount = amount,
        paidByMemberId = payer, date = date,
        shares = shares.map { SplitShare(memberId = it.first, amount = it.second) },
        createdAt = created
    )

    private fun settlement(from: String, to: String, amount: Double) = Settlement(
        id = "s-$from-$to", groupId = "g", fromMemberId = from, toMemberId = to,
        amount = amount, date = date, createdAt = created
    )

    private fun nets(balances: List<MemberBalance>) = balances.associate { it.memberId to it.netAmount }

    @Test
    fun equalSplit_payerIsOwed_othersOwe() {
        val balances = computeBalances(
            members  = listOf(member("a"), member("b"), member("c")),
            expenses = listOf(expense("a", 30.0, listOf("a" to 10.0, "b" to 10.0, "c" to 10.0))),
            settlements = emptyList()
        )
        val net = nets(balances)
        assertEquals(20.0, net["a"])   // paid 30, owes 10
        assertEquals(-10.0, net["b"])
        assertEquals(-10.0, net["c"])
    }

    @Test
    fun unevenCentsSplit_sumPreserved() {
        // 100.01 / 3 = 33.34 + 33.34 + 33.33
        val balances = computeBalances(
            members  = listOf(member("a"), member("b"), member("c")),
            expenses = listOf(expense("a", 100.01, listOf("a" to 33.34, "b" to 33.34, "c" to 33.33))),
            settlements = emptyList()
        )
        val net = nets(balances)
        assertEquals(66.67, net["a"])
        assertEquals(-33.34, net["b"])
        assertEquals(-33.33, net["c"])
    }

    @Test
    fun settlement_reducesOutstanding() {
        val balances = computeBalances(
            members  = listOf(member("a"), member("b")),
            expenses = listOf(expense("a", 50.0, listOf("a" to 25.0, "b" to 25.0))),
            settlements = listOf(settlement(from = "b", to = "a", amount = 25.0))
        )
        val net = nets(balances)
        assertEquals(0.0, net["a"])
        assertEquals(0.0, net["b"])
    }

    @Test
    fun settlementDirection_reversedSigns() {
        // from = payer: paying a debt raises the payer's net, lowers the receiver's.
        val balances = computeBalances(
            members  = listOf(member("a"), member("b")),
            expenses = emptyList(),
            settlements = listOf(settlement(from = "b", to = "a", amount = 10.0))
        )
        val net = nets(balances)
        assertEquals(-10.0, net["a"])
        assertEquals(10.0, net["b"])
    }

    @Test
    fun zeroBalance_noActivity() {
        val balances = computeBalances(
            members = listOf(member("a"), member("b")),
            expenses = emptyList(), settlements = emptyList()
        )
        nets(balances).values.forEach { assertEquals(0.0, it) }
    }

    @Test
    fun multipleMembersAndExpenses_roundedToCents() {
        val balances = computeBalances(
            members = listOf(member("a"), member("b"), member("c"), member("d")),
            expenses = listOf(
                expense("a", 40.0, listOf("a" to 10.0, "b" to 10.0, "c" to 10.0, "d" to 10.0)),
                expense("b", 20.0, listOf("a" to 5.0, "b" to 5.0, "c" to 5.0, "d" to 5.0))
            ),
            settlements = listOf(settlement(from = "c", to = "a", amount = 15.0))
        )
        val net = nets(balances)
        // expense1 (a paid 40, even): a +30, b/c/d -10
        // expense2 (b paid 20, even): b +15-5=+5 net, a -5, c/d -5
        // settlement c -> a of 15 (c pays a): c +15, a -15
        assertEquals(10.0, net["a"])   // 30 - 5 - 15
        assertEquals(5.0, net["b"])
        assertEquals(0.0, net["c"])    // -15 + 15
        assertEquals(-15.0, net["d"])
    }
}
