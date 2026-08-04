package com.ryuken.obsidianledger.core.domain.helper

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
