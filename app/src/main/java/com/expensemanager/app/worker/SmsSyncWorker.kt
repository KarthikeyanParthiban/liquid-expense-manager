package com.expensemanager.app.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.expensemanager.app.ExpenseApplication
import com.expensemanager.app.service.SmsSyncService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Background WorkManager worker for periodic and opportunistic SMS synchronization.
 * Runs in background even when app has not been launched or screen is off.
 */
class SmsSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val app = applicationContext as? ExpenseApplication ?: return@withContext Result.failure()

        return@withContext try {
            val count = app.smsRepository.syncAllInboxSms { current, total ->
                setProgressAsync(workDataOf("current" to current, "total" to total))
            }
            Result.success(workDataOf("importedCount" to count))
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    companion object {
        const val PERIODIC_WORK_NAME = "SMS_PERIODIC_BACKGROUND_SYNC"
        const val ONE_TIME_WORK_NAME = "SMS_ONE_TIME_SYNC"

        /**
         * Schedule periodic background sync every 15-30 minutes.
         */
        fun schedulePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .build()

            val periodicRequest = PeriodicWorkRequestBuilder<SmsSyncWorker>(
                15, TimeUnit.MINUTES,
                5, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest
            )
        }

        /**
         * Enqueue immediate one-time sync task in WorkManager.
         */
        fun enqueueImmediateSync(context: Context) {
            val request = OneTimeWorkRequestBuilder<SmsSyncWorker>()
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                ONE_TIME_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                request
            )
        }
    }
}
