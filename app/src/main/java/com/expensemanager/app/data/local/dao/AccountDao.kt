package com.expensemanager.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.expensemanager.app.data.local.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {

    data class AccountStats(
        val accountId: String,
        val count: Int,
        val totalExpense: Double,
        val totalIncome: Double
    )

    @Query("SELECT * FROM accounts WHERE maskNumber != 'PRIMARY' AND maskNumber != '' AND bankName != 'Bank Account' AND bankName != '' ORDER BY lastUpdated DESC")
    fun getAllAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE maskNumber != 'PRIMARY' AND maskNumber != '' AND bankName != 'Bank Account' AND bankName != ''")
    suspend fun getAllAccountsSnapshot(): List<AccountEntity>

    @Query("SELECT * FROM accounts WHERE id = :accountId LIMIT 1")
    suspend fun getAccountById(accountId: String): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(account: AccountEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(accounts: List<AccountEntity>)

    @Update
    suspend fun update(account: AccountEntity)

    @Query("DELETE FROM accounts WHERE id = :accountId")
    suspend fun deleteAccount(accountId: String)

    @Query("DELETE FROM accounts WHERE maskNumber = 'PRIMARY' AND bankName IN (SELECT bankName FROM accounts WHERE maskNumber != 'PRIMARY')")
    suspend fun cleanDuplicatePrimaryAccounts()

    @Query("DELETE FROM accounts")
    suspend fun clearAll()
}
