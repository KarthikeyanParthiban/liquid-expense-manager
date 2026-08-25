package com.expensemanager.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 2,
    exportSchema = false
)
abstract class ExpenseDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun accountDao(): AccountDao
    abstract fun merchantRuleDao(): MerchantRuleDao

    companion object {
        @Volatile
        private var INSTANCE: ExpenseDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN status TEXT NOT NULL DEFAULT 'COMPLETED'")
                db.execSQL("ALTER TABLE transactions ADD COLUMN originalTransactionId TEXT")
                db.execSQL("ALTER TABLE transactions ADD COLUMN relatedTransactionId TEXT")
                db.execSQL("ALTER TABLE transactions ADD COLUMN confidence REAL NOT NULL DEFAULT 1.0")
                db.execSQL("ALTER TABLE transactions ADD COLUMN classificationReason TEXT")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_status ON transactions(status)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_originalTransactionId ON transactions(originalTransactionId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_relatedTransactionId ON transactions(relatedTransactionId)")
            }
        }

        fun getDatabase(context: Context): ExpenseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ExpenseDatabase::class.java,
                    "expense_manager.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
