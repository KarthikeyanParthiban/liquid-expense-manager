package com.expensemanager.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.expensemanager.app.data.local.dao.AccountDao
import com.expensemanager.app.data.local.dao.MerchantRuleDao
import com.expensemanager.app.data.local.dao.TransactionDao
import com.expensemanager.app.data.local.entity.AccountEntity
import com.expensemanager.app.data.local.entity.MerchantRuleEntity
import com.expensemanager.app.data.local.entity.TransactionEntity

@Database(
    entities = [
        TransactionEntity::class,
        AccountEntity::class,
        MerchantRuleEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class ExpenseDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun accountDao(): AccountDao
    abstract fun merchantRuleDao(): MerchantRuleDao

    companion object {
        @Volatile
        private var INSTANCE: ExpenseDatabase? = null

        fun getDatabase(context: Context): ExpenseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ExpenseDatabase::class.java,
                    "expense_manager.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
