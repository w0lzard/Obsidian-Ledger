package com.ryuken.obsidianledger.core.data

import com.ryuken.obsidianledger.core.database.CategoryEntity
import com.ryuken.obsidianledger.core.domain.error.withRepositoryErrorHandling
import com.ryuken.obsidianledger.core.domain.model.Category
import com.ryuken.obsidianledger.core.domain.repository.CategoryRepository
import com.ryuken.obsidianledger.core.database.LedgerDatabase
import com.ryuken.obsidianledger.core.sync.LocalManifestRow
import com.ryuken.obsidianledger.core.sync.RemoteManifestRow
import com.ryuken.obsidianledger.core.sync.SyncMerger
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import io.github.aakira.napier.Napier
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Clock

/**
 * Categories are a hybrid: defaults (userId NULL, hardcoded ids identical on every
 * device) are local-only and never sync; custom categories sync bidirectionally with
 * the same tombstone/manifest contract as transactions and budgets (docs/SyncContract.md).
 * Sync order in SyncUseCase puts categories first so pulled transactions always find
 * their category rows locally.
 */
class CategoryRepositoryImpl(
    private val db            : LedgerDatabase,
    private val supabaseClient: SupabaseClient
) : CategoryRepository {

    private val queries = db.categoryEntityQueries

    override fun observeAll(userId: String): Flow<List<Category>> =
        queries
            .selectAll(userId = userId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toDomain() } }

    override suspend fun insertCustom(
        category : Category,
        userId   : String
    ) {
        withContext(Dispatchers.IO) {
            queries.insertCustom(
                id        = category.id,
                name      = category.name,
                emoji     = category.emoji,
                colorHex  = category.colorHex,
                userId    = userId,
                sortOrder = 99L,
                updatedAt = Clock.System.now().toString(),
                isDirty   = 1L
            )
        }
    }

    /**
     * Soft-deletes a custom category. Refuses when live transactions or budgets still
     * reference it — tombstoning a referenced category would orphan those rows on
     * every other device once the delete propagates.
     */
    override suspend fun delete(id: String): Unit = withRepositoryErrorHandling("CategoryRepository.delete") {
        withContext(Dispatchers.IO) {
            db.transaction {
                val referenced = queries.isReferencedByTransaction(id).executeAsOne() ||
                    queries.isReferencedByBudget(id).executeAsOne()
                if (referenced) {
                    throw IllegalStateException("Category is still referenced by transactions or budgets")
                }
                queries.markDeleted(id = id, deletedAt = Clock.System.now().toString())
            }
        }
    }

    override suspend fun getDefaultCategory(id: String): Category =
        withContext(Dispatchers.IO) {
            queries.selectById(id).executeAsOneOrNull()?.toDomain()
                ?: Category(
                    id       = "cat_food",
                    name     = "General",
                    emoji    = "💰",
                    colorHex = "#00C896",
                    isCustom = false
                )
        }

    // ── Sync ──────────────────────────────────────────────────────────
    override suspend fun syncPendingToRemote(userId: String): Unit = withRepositoryErrorHandling("CategoryRepository.syncPendingToRemote") {
        withContext(Dispatchers.IO) {
            // Defaults have userId NULL, so the user-scoped dirty select excludes them.
            val dirty = queries.selectDirty(userId = userId).executeAsList()
            if (dirty.isEmpty()) return@withContext

            val (tombstoned, live) = dirty.partition { it.deletedAt != null }
            if (live.isNotEmpty()) {
                supabaseClient.postgrest["categories"].upsert(live.map { it.toDto() })
            }
            tombstoned.forEach {
                supabaseClient.postgrest["categories"].delete { filter { eq("id", it.id) } }
            }
            dirty.forEach { queries.markClean(it.id) }
            queries.purgeTombstones()
        }
    }

    override suspend fun pullRemote(userId: String): Unit = withRepositoryErrorHandling("CategoryRepository.pullRemote") {
        withContext(Dispatchers.IO) {
            val remoteStamps = fetchRemoteStamps(userId)

            val localStamps = queries.selectManifest(userId = userId).executeAsList()
            val plan = SyncMerger.plan(
                local  = localStamps.map { LocalManifestRow(it.id, it.serverUpdatedAt, it.isDirty == 1L) },
                remote = remoteStamps.map { RemoteManifestRow(it.id, it.updatedAt) }
            )

            plan.toFetch.forEach { id ->
                val dto = supabaseClient.postgrest["categories"].select {
                    filter { eq("user_id", userId); eq("id", id) }
                }.decodeList<CategoryDto>().firstOrNull() ?: return@forEach

                db.transaction {
                    queries.insertRemote(
                        id              = dto.id,
                        name            = dto.name,
                        emoji           = dto.emoji,
                        colorHex        = dto.color_hex,
                        isCustom        = 1L,
                        userId          = dto.user_id,
                        sortOrder       = dto.sort_order,
                        serverUpdatedAt = dto.updated_at
                    )
                    queries.applyRemote(
                        id              = dto.id,
                        name            = dto.name,
                        emoji           = dto.emoji,
                        colorHex        = dto.color_hex,
                        sortOrder       = dto.sort_order,
                        serverUpdatedAt = dto.updated_at
                    )
                }
            }

            plan.toDeleteLocal.forEach { queries.deleteCleanById(it) }
        }
    }

    private suspend fun fetchRemoteStamps(userId: String): List<CategoryStampDto> {
        val stamps = mutableListOf<CategoryStampDto>()
        var offset = 0L
        while (true) {
            val page = supabaseClient.postgrest["categories"].select(columns = Columns.list("id", "updated_at")) {
                filter { eq("user_id", userId) }
                range(offset, offset + MANIFEST_PAGE - 1)
            }.decodeList<CategoryStampDto>()
            stamps += page
            if (page.size < MANIFEST_PAGE) break
            offset += MANIFEST_PAGE
        }
        return stamps
    }

    // ── Seed defaults on first launch ─────────────────────────────────
    suspend fun seedDefaultsIfEmpty(userId: String) =
        withContext(Dispatchers.IO) {
            val existing = queries.selectDefaults().executeAsList()
            if (existing.isNotEmpty()) return@withContext
            DefaultCategories.all.forEach { cat ->
                queries.insertCustom(
                    id        = cat.id,
                    name      = cat.name,
                    emoji     = cat.emoji,
                    colorHex  = cat.colorHex,
                    userId    = null,
                    sortOrder = cat.sortOrder.toLong(),
                    updatedAt = "",
                    isDirty   = 0L   // defaults never sync — stay clean forever
                )
            }
        }

    private companion object {
        const val MANIFEST_PAGE = 1_000L
    }
}

// ── Mapper ────────────────────────────────────────────────────────────

private fun CategoryEntity.toDomain() =
    Category(
        id       = id,
        name     = name,
        emoji    = emoji,
        colorHex = colorHex,
        isCustom = isCustom == 1L
    )

@Serializable
internal data class CategoryDto(
    val id          : String,
    val name        : String,
    val emoji       : String,
    @SerialName("color_hex")
    val color_hex   : String,
    @SerialName("is_custom")
    val is_custom   : Boolean,
    @SerialName("sort_order")
    val sort_order  : Long,
    @SerialName("user_id")
    val user_id     : String,
    // Server-maintained; defaults keep them out of push payloads.
    @SerialName("created_at")
    val created_at  : String? = null,
    @SerialName("updated_at")
    val updated_at  : String? = null
)

private fun CategoryEntity.toDto() =
    CategoryDto(
        id         = id,
        name       = name,
        emoji      = emoji,
        color_hex  = colorHex,
        is_custom  = isCustom == 1L,
        sort_order = sortOrder,
        user_id    = userId ?: "",
        updated_at = null
    )

@Serializable
internal data class CategoryStampDto(
    val id         : String,
    @SerialName("updated_at")
    val updatedAt  : String?
)

// ── Default categories ────────────────────────────────────────────────

private data class DefaultCategory(
    val id        : String,
    val name      : String,
    val emoji     : String,
    val colorHex  : String,
    val sortOrder : Int
)

private object DefaultCategories {
    val all = listOf(
        DefaultCategory("cat_food",      "Food & Dining",  "🍔", "#FF6B6B", 0),
        DefaultCategory("cat_transport", "Transport",      "🚕", "#4ECDC4", 1),
        DefaultCategory("cat_shopping",  "Shopping",       "🛍", "#45B7D1", 2),
        DefaultCategory("cat_health",    "Health",         "💊", "#96CEB4", 3),
        DefaultCategory("cat_bills",     "Bills",          "⚡", "#FFEAA7", 4),
        DefaultCategory("cat_housing",   "Housing",        "🏠", "#DDA0DD", 5),
        DefaultCategory("cat_dining",    "Fine Dining",    "🍽", "#F0E68C", 6),
        DefaultCategory("cat_entertain", "Entertainment",  "🎬", "#98FB98", 7),
        DefaultCategory("cat_savings",   "Savings",        "💰", "#87CEEB", 8),
        DefaultCategory("cat_income",    "Income",         "💼", "#00C896", 9)
    )
}
