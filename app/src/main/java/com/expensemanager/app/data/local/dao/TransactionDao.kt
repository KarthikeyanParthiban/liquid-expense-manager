package com.expensemanager.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.expensemanager.app.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    data class CategorySpending(
        val category: String,
        val totalAmount: Double,
        val count: Int
    )

    data class MonthlySummary(
        val totalExpense: Double,
        val totalIncome: Double,
        val transactionCount: Int
    )

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getTransactionsInRange(startTime: Long, endTime: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY timestamp DESC")
    fun getTransactionsByAccount(accountId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE category = :category ORDER BY timestamp DESC")
    fun getTransactionsByCategory(category: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE referenceId = :referenceId LIMIT 1")
    suspend fun getTransactionByReference(referenceId: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE timestamp >= :sinceTime")
    suspend fun getTransactionsSince(sinceTime: Long): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE timestamp BETWEEN :startTime AND :endTime")
    suspend fun getTransactionsBetween(startTime: Long, endTime: Long): List<TransactionEntity>

    @Query("""
        SELECT * FROM transactions 
        WHERE (type IN ('DEBIT', 'CARD_PAYMENT'))
          AND (merchantName LIKE '%' || :merchantKeyword || '%' OR :merchantKeyword LIKE '%' || merchantName || '%')
          AND timestamp BETWEEN :startTime AND :endTime
        ORDER BY timestamp DESC
        LIMIT 1
    """)
    suspend fun findMatchingDebitForRefund(
        merchantKeyword: String,
        startTime: Long,
        endTime: Long
    ): TransactionEntity?

    @Query("""
        SELECT 
            MAX(0.0, 
                COALESCE(SUM(CASE WHEN type IN ('DEBIT', 'CARD_PAYMENT') AND isExcludedFromBudget = 0 AND status != 'FAILED' THEN amount ELSE 0 END), 0.0) -
                COALESCE(SUM(CASE WHEN type IN ('REFUND', 'REVERSAL') AND isExcludedFromBudget = 0 AND status != 'FAILED' THEN amount ELSE 0 END), 0.0)
            ) AS totalExpense,
            COALESCE(SUM(CASE WHEN type = 'CREDIT' AND isExcludedFromBudget = 0 AND status != 'FAILED' THEN amount ELSE 0 END), 0.0) AS totalIncome,
            COUNT(*) AS transactionCount
        FROM transactions
        WHERE timestamp BETWEEN :startTime AND :endTime
    """)
    fun getMonthlySummary(startTime: Long, endTime: Long): Flow<MonthlySummary>

    @Query("""
        SELECT 
            category,
            MAX(0.0, SUM(CASE WHEN type IN ('DEBIT', 'CARD_PAYMENT') THEN amount WHEN type IN ('REFUND', 'REVERSAL') THEN -amount ELSE 0.0 END)) AS totalAmount,
            COUNT(*) AS count
        FROM transactions
        WHERE isExcludedFromBudget = 0 AND status != 'FAILED' AND timestamp BETWEEN :startTime AND :endTime
        GROUP BY category
        HAVING totalAmount > 0
        ORDER BY totalAmount DESC
    """)
    fun getCategorySpendingInRange(startTime: Long, endTime: Long): Flow<List<CategorySpending>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionEntity>)

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Query("UPDATE transactions SET status = :newStatus WHERE id = :id")
    suspend fun updateStatus(id: String, newStatus: String)

    @Query("UPDATE transactions SET category = :newCategory, isUserEdited = 1 WHERE merchantName LIKE '%' || :merchantKeyword || '%'")
    suspend fun updateCategoryForMerchant(merchantKeyword: String, newCategory: String)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM transactions")
    suspend fun clearAll()
}
