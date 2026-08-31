package com.ryuken.obsidianledger.features.profile

import com.ryuken.obsidianledger.core.domain.model.Category
import com.ryuken.obsidianledger.core.domain.model.Transaction
import com.ryuken.obsidianledger.core.domain.model.TransactionType
import com.ryuken.obsidianledger.fake.FakeCategoryRepository
import com.ryuken.obsidianledger.fake.FakeTransactionRepository
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class ImportExportRoundTripTest {

    private fun tx(note: String?, categoryName: String = "Food & Dining") = Transaction(
        id = "t-${Math.random()}",
        amount = 12.34,
        type = TransactionType.EXPENSE,
        category = Category(id = "cat_food", name = categoryName, emoji = "🍔"),
        note = note,
        date = LocalDate.parse("2026-01-15"),
        createdAt = Instant.parse("2026-01-15T10:00:00Z"),
        updatedAt = Instant.parse("2026-01-15T10:00:00Z"),
        isDirty = false,
        userId = "user-1"
    )

    @Test
    fun multilineNote_roundTrips() = runBlocking {
        val txRepo = FakeTransactionRepository().apply {
            added += tx(note = "line one\nline two, with comma")
        }
        val csv = ExportCsvUseCase(txRepo)("user-1")

        val importRepo = FakeTransactionRepository()
        val result = ImportCsvUseCase(importRepo, FakeCategoryRepository())("user-1", csv)

        assertEquals(1, result.imported)
        assertEquals(0, result.skipped)
        assertEquals("line one\nline two, with comma", importRepo.added.single().note)
    }

    @Test
    fun formulaNote_roundTripsSanitized() = runBlocking {
        val malicious = "=cmd|'/c calc'!A0"
        val txRepo = FakeTransactionRepository().apply { added += tx(note = malicious) }
        val csv = ExportCsvUseCase(txRepo)("user-1")

        // Exported field must carry the quote prefix, so Excel reads it as text.
        assertEquals(true, csv.contains(",'=cmd|'/c calc'!A0"))

        val importRepo = FakeTransactionRepository()
        val result = ImportCsvUseCase(importRepo, FakeCategoryRepository())("user-1", csv)
        assertEquals(1, result.imported)
        assertEquals(malicious, importRepo.added.single().note)
    }

    @Test
    fun zeroAndNegativeAmounts_skippedAsMalformed() = runBlocking {
        val csv = """
            Date,Type,Category,Amount,Note
            2026-01-15,EXPENSE,Food,0,lunch
            2026-01-15,EXPENSE,Food,-5,dinner
            2026-01-15,EXPENSE,Food,12.5,valid
        """.trimIndent()

        val result = ImportCsvUseCase(FakeTransactionRepository(), FakeCategoryRepository())("user-1", csv)
        assertEquals(1, result.imported)
        assertEquals(2, result.skipped)
    }

    @Test
    fun malformedRows_countedSkipped_notSpurious() = runBlocking {
        val csv = """
            Date,Type,Category,Amount,Note
            not-a-date,EXPENSE,Food,10,x
            2026-01-15,NOTATYPE,Food,10,x
            2026-01-15,EXPENSE,,10,x
            2026-01-15,EXPENSE,Food,not-a-number,x
            2026-01-15,EXPENSE,Food,10,good
        """.trimIndent()

        val result = ImportCsvUseCase(FakeTransactionRepository(), FakeCategoryRepository())("user-1", csv)
        assertEquals(1, result.imported)
        assertEquals(4, result.skipped)
    }

    @Test
    fun unknownCategory_autoCreated() = runBlocking {
        val csv = "2026-01-15,EXPENSE,Mystery,9.99,x"
        val catRepo = FakeCategoryRepository()
        val result = ImportCsvUseCase(FakeTransactionRepository(), catRepo)("user-1", csv)
        assertEquals(1, result.imported)
        assertEquals("Mystery", catRepo.inserted.single().name)
    }

    @Test
    fun crlfInput_imports() = runBlocking {
        val csv = "2026-01-15,EXPENSE,Food,10.0,x\r\n2026-01-16,INCOME,Food,20.0,y\r\n"
        val txRepo = FakeTransactionRepository()
        val result = ImportCsvUseCase(txRepo, FakeCategoryRepository())("user-1", csv)
        assertEquals(2, result.imported)
        assertEquals(0, result.skipped)
    }
}
