package com.ryuken.obsidianledger.core.domain.helper

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CsvTest {

    // ── parseCsv: RFC4180 ─────────────────────────────────────────

    @Test
    fun plainRows() {
        val rows = parseCsv("a,b,c\n1,2,3")
        assertEquals(listOf(listOf("a", "b", "c"), listOf("1", "2", "3")), rows)
    }

    @Test
    fun commaInsideQuotes() {
        val rows = parseCsv("""2026-01-01,EXPENSE,"Food, drinks",10.50,lunch""")
        assertEquals("Food, drinks", rows[0][2])
    }

    @Test
    fun escapedQuoteInsideQuotes() {
        val rows = parseCsv("\"said \"\"hi\"\"\",x")
        assertEquals("said \"hi\"", rows[0][0])
    }

    @Test
    fun newlineInsideQuotes_singleRow() {
        val rows = parseCsv("2026-01-01,EXPENSE,Food,10.0,\"line one\nline two\"")
        assertEquals(1, rows.size)
        assertEquals("line one\nline two", rows[0][4])
    }

    @Test
    fun newlineInsideQuotes_crlf() {
        val rows = parseCsv("2026-01-01,EXPENSE,Food,10.0,\"line one\r\nline two\"")
        assertEquals(1, rows.size)
        assertEquals("line one\r\nline two", rows[0][4])
    }

    @Test
    fun crlfRowBoundaries() {
        val rows = parseCsv("a,b\r\nc,d\r\n")
        assertEquals(listOf(listOf("a", "b"), listOf("c", "d")), rows)
    }

    @Test
    fun trailingBlankLine_producesNoRow() {
        val rows = parseCsv("a,b\n\n")
        assertEquals(listOf(listOf("a", "b")), rows)
    }

    @Test
    fun emptyTrailingFieldPreserved() {
        val rows = parseCsv("a,b,")
        assertEquals(listOf("a", "b", ""), rows[0])
    }

    @Test
    fun unclosedQuote_parsesDeterministicallyToEof() {
        val rows = parseCsv("a,\"unclosed")
        assertEquals(1, rows.size)
        assertEquals("unclosed", rows[0][1])
    }

    @Test
    fun emptyInput_noRows() {
        assertTrue(parseCsv("").isEmpty())
    }

    @Test
    fun tenThousandRows_parsesExactly() {
        val sb = StringBuilder()
        repeat(10_000) { i ->
            if (i > 0) sb.append('\n')
            sb.append("2026-01-01,EXPENSE,Cat $i,${i + 1}.0,note $i")
        }
        val rows = parseCsv(sb.toString())
        assertEquals(10_000, rows.size)
        assertEquals("note 9999", rows.last()[4])
    }

    // ── Formula injection sanitize/unsanitize ─────────────────────

    @Test
    fun sanitizePrefixesTriggers() {
        listOf("=cmd", "+1", "-2", "@x", "\ttab", "\rcr").forEach {
            assertEquals("'$it", sanitizeCsvFormulaInjection(it))
        }
    }

    @Test
    fun sanitizeLeavesSafeValues() {
        assertEquals("hello", sanitizeCsvFormulaInjection("hello"))
        assertEquals("", sanitizeCsvFormulaInjection(""))
    }

    @Test
    fun unsanitizeOnlyStripsWhenNextCharTriggers() {
        assertEquals("=cmd", unsanitizeCsvFormulaInjection("'=cmd"))
        assertEquals("'hello", unsanitizeCsvFormulaInjection("'hello"))   // quote kept
    }

    @Test
    fun roundTrip_noQuoteAccumulation() {
        val original = "=cmd|'/c calc'!A0"
        var value = original
        repeat(5) { value = unsanitizeCsvFormulaInjection(sanitizeCsvFormulaInjection(value)) }
        assertEquals(original, value)
    }

    // ── csvField quoting ──────────────────────────────────────────

    @Test
    fun csvFieldQuotesOnlyWhenNeeded() {
        assertEquals("plain", csvField("plain"))
        assertEquals("\"has,comma\"", csvField("has,comma"))
        assertEquals("\"has\"\"quote\"", csvField("has\"quote"))
        assertEquals("\"has\nnewline\"", csvField("has\nnewline"))
    }
}
