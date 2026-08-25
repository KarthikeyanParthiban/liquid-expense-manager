package com.expensemanager.app.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.expensemanager.app.ExpenseApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
        const val WORK_NAME = "SMS_HISTORICAL_SYNC_WORK"
    }
}
