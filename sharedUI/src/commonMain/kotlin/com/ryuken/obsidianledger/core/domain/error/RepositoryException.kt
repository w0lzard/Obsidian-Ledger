package com.ryuken.obsidianledger.core.domain.error

import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException

// Wraps whatever the data source throws (Postgrest, SQLDelight, ...) so ViewModels
// see one consistent exception type/message instead of raw SDK exceptions leaking through.
class RepositoryException(message: String, cause: Throwable) : Exception(message, cause)

internal suspend fun <T> withRepositoryErrorHandling(
    tag   : String,
    block : suspend () -> T
): T =
    try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: RepositoryException) {
        throw e
    } catch (e: Exception) {
        Napier.e("$tag failed: ${e.message}", e)
        throw RepositoryException(e.message ?: "$tag failed", e)
    }
