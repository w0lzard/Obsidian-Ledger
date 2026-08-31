package com.ryuken.obsidianledger.core.data

import com.ryuken.obsidianledger.core.domain.dto.GroupMemberDto
import com.ryuken.obsidianledger.core.domain.dto.SplitExpenseDto
import com.ryuken.obsidianledger.core.domain.dto.SplitExpenseShareDto
import com.ryuken.obsidianledger.core.domain.dto.SplitGroupDto
import com.ryuken.obsidianledger.core.domain.dto.SplitSettlementDto
import com.ryuken.obsidianledger.core.domain.error.withRepositoryErrorHandling
import com.ryuken.obsidianledger.core.domain.helper.distributeCentsEvenly
import com.ryuken.obsidianledger.core.domain.helper.roundToCents
import com.ryuken.obsidianledger.core.domain.helper.toCents
import com.ryuken.obsidianledger.core.domain.model.GroupMember
import com.ryuken.obsidianledger.core.domain.model.MemberBalance
import com.ryuken.obsidianledger.core.domain.model.Settlement
import com.ryuken.obsidianledger.core.domain.model.SplitExpense
import com.ryuken.obsidianledger.core.domain.model.SplitGroup
import com.ryuken.obsidianledger.core.domain.model.SplitShare
import com.ryuken.obsidianledger.core.domain.repository.SplitRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.annotations.SupabaseExperimental
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.filter.FilterOperation
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.selectAsFlow
import io.github.jan.supabase.realtime.selectSingleValueAsFlow
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.time.Instant

class SplitRepositoryImpl(
    private val db: SupabaseClient
) : SplitRepository {

    private val groupsTable    get() = db.postgrest["split_groups"]
    private val membersTable   get() = db.postgrest["split_group_members"]
    private val expensesTable  get() = db.postgrest["split_expenses"]
    private val sharesTable    get() = db.postgrest["split_expense_shares"]
    private val settlementsTable get() = db.postgrest["split_settlements"]

    // ── Groups ────────────────────────────────────────────────────────
    // ponytail: unfiltered — relies on RLS to scope rows per user. Fine at personal-finance
    // table sizes; add a per-user server-side filter if this ever needs to scale further.
    @OptIn(SupabaseExperimental::class)
    override fun observeGroups(userId: String): Flow<List<SplitGroup>> =
        combine(
            groupsTable.selectAsFlow(primaryKey = SplitGroupDto::id),
            membersTable.selectAsFlow(primaryKey = GroupMemberDto::id)
        ) { groups, members ->
            val membersByGroup = members.groupBy { it.groupId }
            val visibleGroupIds = (
                groups.filter { it.createdBy == userId }.map { it.id } +
                    members.filter { it.userId == userId }.map { it.groupId }
                ).distinct()

            groups
                .filter { it.id in visibleGroupIds }
                .map { g -> g.toDomain(membersByGroup[g.id].orEmpty().map { it.toDomain() }) }
                .sortedByDescending { it.createdAt }
        }.catch { e ->
            Napier.e("observeGroups failed, showing empty list", e)
            emit(emptyList())
        }

    @OptIn(SupabaseExperimental::class)
    override fun observeGroup(groupId: String): Flow<SplitGroup> =
        combine(
            groupsTable.selectSingleValueAsFlow(primaryKey = SplitGroupDto::id) { eq("id", groupId) },
            membersTable.selectAsFlow(
                primaryKey = GroupMemberDto::id,
                filter = FilterOperation("group_id", FilterOperator.EQ, groupId)
            )
        ) { group, members -> group.toDomain(members.map { it.toDomain() }) }

    override suspend fun createGroup(
        name: String,
        createdBy: String,
        creatorDisplayName: String,
        memberNames: List<String>
    ): SplitGroup = withRepositoryErrorHandling("SplitRepository.createGroup") {
        withContext(Dispatchers.IO) {
            // Atomic server-side RPC (group + all members in one transaction) — replaces
            // the old insert-then-compensate dance that could leave half a group behind.
            val result = db.postgrest.rpc(
                function = "create_split_group",
                parameters = buildJsonObject {
                    put("p_name", name)
                    put("p_creator_display_name", creatorDisplayName)
                    put("p_member_names", JsonArray(memberNames.map { JsonPrimitive(it) }))
                }
            ).decodeAs<CreateGroupRpcResult>()

            SplitGroup(
                id        = result.id,
                name      = result.name,
                members   = result.members.map { it.toDomain() },
                createdBy = result.createdBy,
                createdAt = Instant.parse(result.createdAt)
            )
        }
    }

    override suspend fun editMember(memberId: String, displayName: String): Unit = withRepositoryErrorHandling("SplitRepository.editMember") {
        withContext(Dispatchers.IO) {
            membersTable.update(MemberDisplayNameUpdate(displayName)) { filter { eq("id", memberId) } }
        }
    }

    override suspend fun removeMember(memberId: String): Unit = withRepositoryErrorHandling("SplitRepository.removeMember") {
        withContext(Dispatchers.IO) {
            val hasShares = sharesTable.select { filter { eq("member_id", memberId) } }
                .decodeList<SplitExpenseShareDto>().isNotEmpty()
            val hasSettlements = settlementsTable.select {
                filter { or { eq("from_member_id", memberId); eq("to_member_id", memberId) } }
            }.decodeList<SplitSettlementDto>().isNotEmpty()

            check(!hasShares && !hasSettlements) {
                "Cannot remove member: they have existing expense shares or settlements. " +
                    "Settle their balance first to avoid orphaning balance data."
            }

            membersTable.delete { filter { eq("id", memberId) } }
        }
    }

    // ── Expenses ──────────────────────────────────────────────────────
    // ponytail: shares stream is unfiltered (no group_id column to filter by server-side);
    // relies on RLS. Fine at personal-finance table sizes.
    @OptIn(SupabaseExperimental::class)
    override fun observeExpenses(groupId: String): Flow<List<SplitExpense>> =
        combine(
            expensesTable.selectAsFlow(
                primaryKey = SplitExpenseDto::id,
                filter = FilterOperation("group_id", FilterOperator.EQ, groupId)
            ),
            sharesTable.selectAsFlow(primaryKey = SplitExpenseShareDto::id)
        ) { expenses, shares ->
            val sharesByExpense = shares.groupBy { it.expenseId }
            expenses
                .map { e -> e.toDomain(sharesByExpense[e.id].orEmpty().map { it.toDomain() }) }
                .sortedByDescending { it.date }
        }.catch { e ->
            Napier.e("observeExpenses failed, showing empty list", e)
            emit(emptyList())
        }

    override suspend fun addExpense(
        groupId: String,
        description: String,
        amount: Double,
        paidByMemberId: String,
        date: LocalDate,
        shares: List<SplitShare>
    ): SplitExpense = withRepositoryErrorHandling("SplitRepository.addExpense") {
        withContext(Dispatchers.IO) {
            val amountCents = amount.toCents()

            // ponytail: assumes an equal split (only mode the UI offers today); a custom-amount
            // split would need to distribute by weight instead of by even count.
            val shareCents = distributeCentsEvenly(amountCents, shares.size)
            check(shareCents.sum() == amountCents) {
                "Split shares (${shareCents.sum() / 100.0}) must sum to expense amount (${amountCents / 100.0})"
            }

            // Atomic server-side RPC (expense + shares in one transaction, membership and
            // sum-exactness validated on the server) — no more shareless expenses on failure.
            val result = db.postgrest.rpc(
                function = "create_split_expense",
                parameters = buildJsonObject {
                    put("p_group_id", groupId)
                    put("p_description", description)
                    put("p_amount", amountCents / 100.0)
                    put("p_paid_by", paidByMemberId)
                    put("p_member_ids", JsonArray(shares.map { JsonPrimitive(it.memberId) }))
                    put("p_share_amounts", JsonArray(shareCents.map { JsonPrimitive(it / 100.0) }))
                    put("p_date", date.toString())
                }
            ).decodeAs<AddExpenseRpcResult>()

            SplitExpense(
                id             = result.id,
                groupId        = result.groupId,
                description    = result.description,
                amount         = result.amount,
                paidByMemberId = result.paidByMemberId,
                date           = LocalDate.parse(result.expenseDate),
                shares         = result.shares.map { it.toDomain() },
                createdAt      = Instant.parse(result.createdAt)
            )
        }
    }

    // ── Settlements ───────────────────────────────────────────────────
    override suspend fun recordSettlement(
        groupId: String,
        fromMemberId: String,
        toMemberId: String,
        amount: Double,
        date: LocalDate
    ): Settlement = withRepositoryErrorHandling("SplitRepository.recordSettlement") {
        withContext(Dispatchers.IO) {
            // Atomic RPC with server-side validation: both parties are group members and
            // the amount does not exceed the pairwise outstanding balance.
            val result = db.postgrest.rpc(
                function = "record_split_settlement",
                parameters = buildJsonObject {
                    put("p_group_id", groupId)
                    put("p_from", fromMemberId)
                    put("p_to", toMemberId)
                    put("p_amount", amount.roundToCents())
                    put("p_date", date.toString())
                }
            ).decodeAs<SplitSettlementDto>()

            Settlement(
                id           = result.id,
                groupId      = result.groupId,
                fromMemberId = result.fromMemberId,
                toMemberId   = result.toMemberId,
                amount       = result.amount,
                date         = LocalDate.parse(result.settledDate),
                createdAt    = Instant.parse(result.createdAt)
            )
        }
    }

    // ── Balances ──────────────────────────────────────────────────────
    @OptIn(SupabaseExperimental::class)
    override fun observeBalances(groupId: String): Flow<List<MemberBalance>> =
        combine(
            observeGroup(groupId),
            observeExpenses(groupId),
            settlementsTable.selectAsFlow(
                primaryKey = SplitSettlementDto::id,
                filter = FilterOperation("group_id", FilterOperator.EQ, groupId)
            )
        ) { group, expenses, settlements -> computeBalances(group, expenses, settlements) }
            .catch { e ->
                Napier.e("observeBalances failed, showing empty list", e)
                emit(emptyList())
            }

    private fun computeBalances(
        group: SplitGroup,
        expenses: List<SplitExpense>,
        settlements: List<SplitSettlementDto>
    ): List<MemberBalance> {
        val net = mutableMapOf<String, Double>()
        group.members.forEach { net[it.id] = 0.0 }

        expenses.forEach { expense ->
            net[expense.paidByMemberId] = (net[expense.paidByMemberId] ?: 0.0) + expense.amount
            expense.shares.forEach { share ->
                net[share.memberId] = (net[share.memberId] ?: 0.0) - share.amount
            }
        }
        settlements.forEach { settlement ->
            net[settlement.fromMemberId] = (net[settlement.fromMemberId] ?: 0.0) + settlement.amount
            net[settlement.toMemberId] = (net[settlement.toMemberId] ?: 0.0) - settlement.amount
        }

        return group.members.map { member ->
            MemberBalance(
                memberId    = member.id,
                displayName = member.displayName,
                email       = member.email,
                netAmount   = (net[member.id] ?: 0.0).roundToCents()
            )
        }
    }
}

// ── Mappers ───────────────────────────────────────────────────────────

private fun SplitGroupDto.toDomain(members: List<GroupMember>) =
    SplitGroup(
        id        = id,
        name      = name,
        members   = members,
        createdBy = createdBy,
        createdAt = kotlin.time.Instant.parse(createdAt)
    )

private fun GroupMemberDto.toDomain() =
    GroupMember(
        id          = id,
        groupId     = groupId,
        userId      = userId,
        displayName = displayName,
        email       = email
    )

private fun SplitExpenseDto.toDomain(shares: List<SplitShare>) =
    SplitExpense(
        id             = id,
        groupId        = groupId,
        description    = description,
        amount         = amount,
        paidByMemberId = paidByMemberId,
        date           = LocalDate.parse(expenseDate),
        shares         = shares,
        createdAt      = kotlin.time.Instant.parse(createdAt)
    )

private fun SplitExpenseShareDto.toDomain() =
    SplitShare(memberId = memberId, amount = amount)

@Serializable
private data class MemberDisplayNameUpdate(
    @SerialName("display_name") val displayName: String
)

// RPC result shapes — the server functions return the exact JSON the client
// previously assembled itself (supabase/migrations/0003_split_rpc.sql).

@Serializable
internal data class CreateGroupRpcResult(
    val id         : String,
    val name       : String,
    @SerialName("created_by")
    val createdBy  : String,
    @SerialName("created_at")
    val createdAt  : String,
    val members    : List<GroupMemberDto>
)

@Serializable
internal data class AddExpenseRpcResult(
    val id             : String,
    @SerialName("group_id")
    val groupId        : String,
    val description    : String,
    val amount         : Double,
    @SerialName("paid_by_member_id")
    val paidByMemberId : String,
    @SerialName("expense_date")
    val expenseDate    : String,
    @SerialName("created_at")
    val createdAt      : String,
    val shares         : List<SplitExpenseShareDto>
)
