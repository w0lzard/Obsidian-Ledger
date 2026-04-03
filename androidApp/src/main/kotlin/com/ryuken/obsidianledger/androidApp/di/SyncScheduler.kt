package com.ryuken.obsidianledger.androidApp.di

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.CoroutineWorker
import com.ryuken.obsidianledger.core.domain.usecase.SyncUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.TimeUnit

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams), KoinComponent {

    private val syncUseCase: SyncUseCase by inject()
    private val authRepo: com.ryuken.obsidianledger.core.domain.repository.AuthRepository by inject()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val userId = authRepo.currentUserId() ?: return@withContext Result.success()
        try {
            syncUseCase(userId)
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}

class SyncScheduler(private val context: Context) {
    fun schedule() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "ObsidianLedgerSync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }

    fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork("ObsidianLedgerSync")
    }
}
