package com.ryuken.obsidianledger.core.domain.helper

// A field starting with =, +, -, @, tab, or CR is interpreted as a formula by
// Excel/Sheets/LibreOffice when the CSV is opened — a malicious note like
// =cmd|'/c calc'!A0 would execute. Prefixing with a single quote forces it to
// be read as text (CWE-1236 / OWASP CSV injection).
private val FORMULA_TRIGGER_CHARS = charArrayOf('=', '+', '-', '@', '\t', '\r')

internal fun sanitizeCsvFormulaInjection(value: String): String =
    if (value.isNotEmpty() && value[0] in FORMULA_TRIGGER_CHARS) "'$value" else value

// Reverses sanitizeCsvFormulaInjection when reading a value back, so a note/category
// re-imported from our own export doesn't accumulate a leading quote every round trip.
internal fun unsanitizeCsvFormulaInjection(value: String): String =
    if (value.length > 1 && value[0] == '\'' && value[1] in FORMULA_TRIGGER_CHARS) value.substring(1) else value

// RFC4180-style quoting — only fields containing a comma, quote, or newline get wrapped,
// so exported CSVs stay readable while still round-tripping notes with commas in them.
internal fun csvField(value: String): String =
    if (value.any { it == ',' || it == '"' || it == '\n' })
        "\"" + value.replace("\"", "\"\"") + "\""
    else value

/**
 * RFC4180 document parser: quoted fields may contain commas, escaped quotes ("") and
 * embedded newlines (LF or CRLF). Row boundary = an unquoted CR, LF, or CRLF; a blank
 * trailing line yields no row. Replaces the old line-splitting import flow, which
 * corrupted any exported note containing a newline (one skipped + one spurious row).
 */
internal fun parseCsv(text: String): List<List<String>> {
    val rows = mutableListOf<List<String>>()
    val row = mutableListOf<String>()
    val field = StringBuilder()
    var inQuotes = false

    fun endField() {
        row.add(field.toString())
        field.clear()
    }
    fun endRow() {
        endField()
        if (row.any { it.isNotEmpty() }) rows.add(row.toList())
        row.clear()
    }

    var i = 0
    while (i < text.length) {
        val c = text[i]
        when {
            inQuotes -> when {
                c == '"' && i + 1 < text.length && text[i + 1] == '"' -> { field.append('"'); i++ }
                c == '"' -> inQuotes = false
                else     -> field.append(c)
            }
            c == '"'  -> inQuotes = true
            c == ','  -> endField()
            c == '\r' -> { if (i + 1 < text.length && text[i + 1] == '\n') i++; endRow() }
            c == '\n' -> endRow()
            else      -> field.append(c)
        }
        i++
    }
    if (field.isNotEmpty() || row.isNotEmpty()) endRow()
    return rows
}
