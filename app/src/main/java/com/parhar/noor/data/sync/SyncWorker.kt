package com.parhar.noor.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.parhar.noor.NoorApplication

class SyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as NoorApplication).appContainer
        return runCatching {
            container.syncCoordinator.processOutbox()
            Result.success()
        }.getOrElse {
            Result.retry()
        }
    }
}
