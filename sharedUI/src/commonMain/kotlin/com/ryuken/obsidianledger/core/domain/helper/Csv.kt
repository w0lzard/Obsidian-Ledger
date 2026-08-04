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

internal fun parseCsvLine(line: String): List<String> {
    val fields = mutableListOf<String>()
    val current = StringBuilder()
    var inQuotes = false
    var i = 0
    while (i < line.length) {
        val c = line[i]
        when {
            inQuotes && c == '"' && i + 1 < line.length && line[i + 1] == '"' -> {
                current.append('"')
                i++
            }
            c == '"' -> inQuotes = !inQuotes
            c == ',' && !inQuotes -> {
                fields.add(current.toString())
                current.clear()
            }
            else -> current.append(c)
        }
        i++
    }
    fields.add(current.toString())
    return fields
}
