package com.ryuken.obsidianledger.fake

import com.ryuken.obsidianledger.core.domain.model.Budget
import com.ryuken.obsidianledger.core.domain.model.Category
import com.ryuken.obsidianledger.core.domain.model.MemberBalance
import com.ryuken.obsidianledger.core.domain.model.MonthlySummary
import com.ryuken.obsidianledger.core.domain.model.Settlement
import com.ryuken.obsidianledger.core.domain.model.SplitExpense
import com.ryuken.obsidianledger.core.domain.model.SplitGroup
import com.ryuken.obsidianledger.core.domain.model.SplitShare
import com.ryuken.obsidianledger.core.domain.model.Transaction
import com.ryuken.obsidianledger.core.domain.model.UserProfile
import com.ryuken.obsidianledger.core.domain.repository.AuthRepository
import com.ryuken.obsidianledger.core.domain.repository.AuthSessionState
import com.ryuken.obsidianledger.core.domain.repository.BudgetRepository
import com.ryuken.obsidianledger.core.domain.repository.CategoryRepository
import com.ryuken.obsidianledger.core.domain.repository.ProfileRepository
import com.ryuken.obsidianledger.core.domain.repository.SplitRepository
import com.ryuken.obsidianledger.core.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

/** Deterministic in-memory fakes for ViewModel/use case tests — no DB, no network. */

class FakeAuthRepository(
    var userId: String? = "user-1"
) : AuthRepository {
    val authState = MutableStateFlow<AuthSessionState>(
        userId?.let { AuthSessionState.Authenticated(it) } ?: AuthSessionState.NotAuthenticated
    )

    override suspend fun signIn(email: String, password: String) { authState.value = AuthSessionState.Authenticated("user-1") }
    override suspend fun signUp(email: String, password: String, displayName: String) { authState.value = AuthSessionState.Authenticated("user-1") }
    override suspend fun signInWithGoogleIdToken(idToken: String, nonce: String) { authState.value = AuthSessionState.Authenticated("user-1") }
    override suspend fun signOut() { authState.value = AuthSessionState.NotAuthenticated }
    override suspend fun updateUser(displayName: String) {}
    override suspend fun updatePassword(newPassword: String) {}
    override fun currentUserId(): String? = (authState.value as? AuthSessionState.Authenticated)?.userId
    override fun isSignedIn(): Boolean = authState.value is AuthSessionState.Authenticated
    override fun observeUserId(): Flow<String?> = authState.map { (it as? AuthSessionState.Authenticated)?.userId }
    override fun observeAuthState(): Flow<AuthSessionState> = authState
    override fun getSessionStatusString(): String = authState.value.toString()
}

class FakeTransactionRepository : TransactionRepository {
    val monthlySummaries = MutableStateFlow<Map<Pair<Int, Int>, MonthlySummary>>(emptyMap())
    val byMonth = MutableStateFlow<Map<Pair<Int, Int>, List<Transaction>>>(emptyMap())
    val added = mutableListOf<Transaction>()
    var failOnAdd: Exception? = null

    override fun observeByMonth(userId: String, year: Int, month: Int): Flow<List<Transaction>> =
        byMonth.map { it[year to month].orEmpty() }

    override fun observeMonthlySummary(userId: String, year: Int, month: Int): Flow<MonthlySummary> =
        monthlySummaries.map { it[year to month] ?: MonthlySummary(0.0, 0.0, emptyMap()) }

    override suspend fun getAll(userId: String): List<Transaction> = added.toList()
    override suspend fun add(transaction: Transaction) {
        failOnAdd?.let { throw it }
        added += transaction
    }
    override suspend fun update(transaction: Transaction) { added.removeAll { it.id == transaction.id }; added += transaction }
    override suspend fun delete(id: String) { added.removeAll { it.id == id } }
    override suspend fun syncPendingToRemote(userId: String) {}
    override suspend fun pullRemote(userId: String) {}
}

class FakeBudgetRepository : BudgetRepository {
    val budgetsByMonth = MutableStateFlow<Map<Pair<Int, Int>, List<Budget>>>(emptyMap())

    override fun observeBudgetsWithSpending(userId: String, year: Int, month: Int): Flow<List<Budget>> =
        budgetsByMonth.map { it[year to month].orEmpty() }
    override suspend fun add(budget: Budget) {}
    override suspend fun delete(id: String) {}
    override suspend fun syncPendingToRemote(userId: String) {}
    override suspend fun pullRemote(userId: String) {}
}

class FakeCategoryRepository : CategoryRepository {
    val categories = MutableStateFlow<List<Category>>(emptyList())
    val inserted = mutableListOf<Category>()

    override fun observeAll(userId: String): Flow<List<Category>> = categories
    override suspend fun insertCustom(category: Category, userId: String) { inserted += category }
    override suspend fun delete(id: String) { categories.value = categories.value.filter { it.id != id } }
    override suspend fun getDefaultCategory(id: String): Category =
        categories.value.firstOrNull { it.id == id }
            ?: Category(id = "cat_food", name = "General", emoji = "💰", isCustom = false)
    override suspend fun syncPendingToRemote(userId: String) {}
    override suspend fun pullRemote(userId: String) {}
}

class FakeProfileRepository : ProfileRepository {
    var profile = UserProfile(id = "user-1", displayName = "Test User", email = "t@t.com")

    override suspend fun getProfile(userId: String): UserProfile = profile
    override suspend fun updateProfile(userId: String, displayName: String) {
        profile = profile.copy(displayName = displayName)
    }
    override fun observeProfile(userId: String): Flow<UserProfile> = MutableStateFlow(profile)
}

class FakeSplitRepository : SplitRepository {
    val groups = MutableStateFlow<List<SplitGroup>>(emptyList())
    val expensesByGroup = MutableStateFlow<Map<String, List<SplitExpense>>>(emptyMap())
    val balancesByGroup = MutableStateFlow<Map<String, List<MemberBalance>>>(emptyMap())
    val settlements = mutableListOf<Settlement>()
    var failOnCreate: Exception? = null
    var failOnSettlement: Exception? = null

    override fun observeGroups(userId: String): Flow<List<SplitGroup>> = groups
    override fun observeGroup(groupId: String): Flow<SplitGroup> =
        groups.map { list -> list.firstOrNull { it.id == groupId } ?: list.first() }
    override suspend fun createGroup(name: String, createdBy: String, creatorDisplayName: String, memberNames: List<String>): SplitGroup {
        failOnCreate?.let { throw it }
        val group = SplitGroup(id = "g-${groups.value.size}", name = name, members = emptyList(), createdBy = createdBy, createdAt = Instant.parse("2026-01-01T00:00:00Z"))
        groups.value = groups.value + group
        return group
    }
    override suspend fun editMember(memberId: String, displayName: String) {}
    override suspend fun removeMember(memberId: String) {}
    override fun observeExpenses(groupId: String): Flow<List<SplitExpense>> =
        expensesByGroup.map { it[groupId].orEmpty() }
    override suspend fun addExpense(groupId: String, description: String, amount: Double, paidByMemberId: String, date: LocalDate, shares: List<SplitShare>): SplitExpense {
        val expense = SplitExpense(
            id = "e-${expensesByGroup.value.values.sumOf { it.size }}",
            groupId = groupId, description = description, amount = amount,
            paidByMemberId = paidByMemberId, date = date, shares = shares,
            createdAt = Instant.parse("2026-01-01T00:00:00Z")
        )
        expensesByGroup.value = expensesByGroup.value + (groupId to ((expensesByGroup.value[groupId] ?: emptyList()) + expense))
        return expense
    }
    override fun observeBalances(groupId: String): Flow<List<MemberBalance>> =
        balancesByGroup.map { it[groupId].orEmpty() }
    override suspend fun recordSettlement(groupId: String, fromMemberId: String, toMemberId: String, amount: Double, date: LocalDate): Settlement {
        failOnSettlement?.let { throw it }
        val settlement = Settlement(
            id = "s-${settlements.size}", groupId = groupId,
            fromMemberId = fromMemberId, toMemberId = toMemberId,
            amount = amount, date = date, createdAt = Instant.parse("2026-01-01T00:00:00Z")
        )
        settlements += settlement
        return settlement
    }
}
