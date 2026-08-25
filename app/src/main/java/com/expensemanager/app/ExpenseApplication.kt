package com.expensemanager.app

import android.app.Application
import com.expensemanager.app.data.local.ExpenseDatabase
import com.expensemanager.app.data.repository.AccountRepository
import com.expensemanager.app.data.repository.SmsRepository
import com.expensemanager.app.data.repository.TransactionRepository

class ExpenseApplication : Application() {

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
    }

    companion object {
        lateinit var instance: ExpenseApplication
            private set
    }
}
