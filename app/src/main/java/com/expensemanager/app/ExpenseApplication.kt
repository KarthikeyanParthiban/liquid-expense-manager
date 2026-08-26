package com.expensemanager.app

import android.app.Application
import com.expensemanager.app.data.local.ExpenseDatabase
import com.expensemanager.app.data.repository.AccountRepository
import com.expensemanager.app.data.repository.SmsRepository
import com.expensemanager.app.data.repository.TransactionRepository
import com.expensemanager.app.worker.SmsSyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class ExpenseApplication : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database by lazy { ExpenseDatabase.getDatabase(this) }
    val transactionRepository by lazy {
        TransactionRepository(
            database.transactionDao(),
            database.accountDao(),
            database.merchantRuleDao()
        )
    }
    val accountRepository by lazy {
        AccountRepository(database.accountDao())
    }
    val smsRepository by lazy {
        SmsRepository(this, transactionRepository)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        com.expensemanager.app.ui.theme.ThemeManager.init(this)
        com.expensemanager.app.ml.OnDeviceMerchantClassifier.initialize(this)
        
        // Schedule 15-minute background periodic sync
        SmsSyncWorker.schedulePeriodicSync(this)
    }

    companion object {
        lateinit var instance: ExpenseApplication
            private set
    }
}
