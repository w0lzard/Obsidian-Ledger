package com.ryuken.obsidianledger.core.sync

import com.ryuken.obsidianledger.core.domain.repository.AuthRepository
import com.ryuken.obsidianledger.core.domain.usecase.SyncUseCase
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import kotlin.native.Platform

/**
 * iOS sync strategy (no WorkManager on iOS): a foreground sync on every
 * scenePhase == .active transition — Swift calls [onForegroundSync] from the app's
 * onChange(of: scenePhase) handler. Cold-start and sign-in pulls are already covered
 * by the common App.kt trigger. The in-flight job is cancelled on sign-out via
 * [SyncCoordinator.onSignedOut], mirroring Android's WorkManager cancellation.
 */
class IosSyncCoordinator : SyncCoordinator, KoinComponent {

    // Process-lifetime scope; only the in-flight job is cancelled on sign-out so a
    // later re-sign-in foreground sync still runs.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var inFlight: Job? = null

    override fun onSignedOut() {
        inFlight?.cancel()
    }

    fun onForegroundSync() {
        val authRepo: AuthRepository = get()
        val sync: SyncUseCase = get()
        val userId = authRepo.currentUserId() ?: return
        inFlight = scope.launch {
            runCatching { sync(userId) }
                .onFailure { Napier.e("iOS foreground sync failed", it) }
        }
    }
}

/** Installs Napier logging on iOS — debug binaries only, matching Android's gating. */
@OptIn(kotlin.experimental.ExperimentalNativeApi::class)
fun installIosLogging() {
    if (Platform.isDebugBinary) {
        Napier.base(DebugAntilog())
    }
}
