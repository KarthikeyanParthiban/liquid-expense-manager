package com.expensemanager.app.data.repository

import com.expensemanager.app.core.model.Category
import com.expensemanager.app.core.model.MerchantRule
import com.expensemanager.app.core.model.ParsedSmsResult
import com.expensemanager.app.core.model.Transaction
import com.expensemanager.app.data.local.dao.AccountDao
import com.expensemanager.app.data.local.dao.MerchantRuleDao
import com.expensemanager.app.data.local.dao.TransactionDao
import com.expensemanager.app.data.local.entity.AccountEntity
import com.expensemanager.app.data.local.entity.MerchantRuleEntity
import com.expensemanager.app.data.local.entity.TransactionEntity
import com.expensemanager.app.parser.DeduplicationEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao,
    private val merchantRuleDao: MerchantRuleDao
) {

    fun getAllTransactions(): Flow<List<Transaction>> =
        transactionDao.getAllTransactions().map { entities -> entities.map { it.toDomain() } }

    fun getTransactionsInRange(startTime: Long, endTime: Long): Flow<List<Transaction>> =
        transactionDao.getTransactionsInRange(startTime, endTime).map { entities -> entities.map { it.toDomain() } }

    fun getMonthlySummary(startTime: Long, endTime: Long): Flow<TransactionDao.MonthlySummary> =
        transactionDao.getMonthlySummary(startTime, endTime)

    fun getCategorySpending(startTime: Long, endTime: Long): Flow<List<TransactionDao.CategorySpending>> =
        transactionDao.getCategorySpendingInRange(startTime, endTime)

    suspend fun getRecentTransactions(sinceTime: Long): List<Transaction> =
        transactionDao.getTransactionsSince(sinceTime).map { it.toDomain() }

    suspend fun processAndSaveSms(result: ParsedSmsResult): DeduplicationEngine.DeduplicationResult {
        // Fetch existing transactions within +/- 48h window to check duplicates
        val windowStart = result.timestamp - (48 * 60 * 60 * 1000L)
        val windowEnd = result.timestamp + (48 * 60 * 60 * 1000L)
        val candidateExisting = transactionDao.getTransactionsBetween(windowStart, windowEnd).map { it.toDomain() }

        val deduplicationResult = DeduplicationEngine.checkDuplicate(result, candidateExisting)

        when (deduplicationResult) {
            is DeduplicationEngine.DeduplicationResult.Unique -> {
                val newTransaction = Transaction(
                    id = UUID.randomUUID().toString(),
                    rawSmsId = null,
                    sender = result.rawSender,
                    amount = result.amount,
                    currency = result.currency,
                    type = result.type,
                    category = result.category,
                    merchantName = result.merchant,
                    accountId = result.accountId,
                    bankName = result.bankName,
                    accountMask = result.accountMask,
                    referenceId = result.referenceId,
                    balanceAfter = result.balanceAfter,
                    timestamp = result.timestamp,
                    isExcludedFromBudget = result.isExcludedFromBudget,
                    rawBody = result.rawBody
                )
                transactionDao.insert(TransactionEntity.fromDomain(newTransaction))

                // Update or create account
                val existingAccount = accountDao.getAccountById(result.accountId)
                val updatedAccount = if (existingAccount != null) {
                    existingAccount.copy(
                        lastKnownBalance = result.balanceAfter ?: existingAccount.lastKnownBalance,
                        lastUpdated = maxOf(existingAccount.lastUpdated, result.timestamp)
                    )
                } else {
                    AccountEntity(
                        id = result.accountId,
                        bankName = result.bankName,
                        accountType = result.accountType.name,
                        maskNumber = result.accountMask ?: "PRIMARY",
                        lastKnownBalance = result.balanceAfter,
                        lastUpdated = result.timestamp
                    )
                }
                accountDao.insertOrUpdate(updatedAccount)
            }
            is DeduplicationEngine.DeduplicationResult.FuzzyDuplicate -> {
                // Enrich existing transaction with additional fields if available
                transactionDao.update(TransactionEntity.fromDomain(deduplicationResult.updatedTransaction))
            }
            is DeduplicationEngine.DeduplicationResult.ExactDuplicate -> {
                // Ignore identical duplicate
            }
        }

        return deduplicationResult
    }

    suspend fun processBalanceUpdate(update: com.expensemanager.app.core.model.AccountBalanceUpdate) {
        val existingAccount = accountDao.getAccountById(update.accountId)
        val updatedAccount = if (existingAccount != null) {
            val shouldUpdateBalance = update.timestamp >= existingAccount.lastUpdated || existingAccount.lastKnownBalance == null
            existingAccount.copy(
                lastKnownBalance = if (shouldUpdateBalance) update.balance else existingAccount.lastKnownBalance,
                lastUpdated = maxOf(existingAccount.lastUpdated, update.timestamp)
            )
        } else {
            AccountEntity(
                id = update.accountId,
                bankName = update.bankName,
                accountType = update.accountType.name,
                maskNumber = update.accountMask ?: "PRIMARY",
                lastKnownBalance = update.balance,
                lastUpdated = update.timestamp
            )
        }
        accountDao.insertOrUpdate(updatedAccount)
    }

    suspend fun updateTransaction(transaction: Transaction) {
        transactionDao.update(TransactionEntity.fromDomain(transaction))
    }

    suspend fun updateCategoryAndCreateRule(merchantKeyword: String, newCategory: Category) {
        merchantRuleDao.insert(
            MerchantRuleEntity(
                merchantPattern = merchantKeyword.trim(),
                category = newCategory.name,
                updatedAt = System.currentTimeMillis()
            )
        )
        transactionDao.updateCategoryForMerchant(merchantKeyword.trim(), newCategory.name)
    }

    suspend fun deleteTransaction(id: String) {
        transactionDao.deleteById(id)
    }

    fun getAllRules(): Flow<List<MerchantRule>> =
        merchantRuleDao.getAllRules().map { entities -> entities.map { it.toDomain() } }

    suspend fun getAllRulesSnapshot(): List<MerchantRule> =
        merchantRuleDao.getAllRulesSnapshot().map { it.toDomain() }

    suspend fun deleteRule(rule: MerchantRule) {
        merchantRuleDao.delete(MerchantRuleEntity.fromDomain(rule))
    }

    suspend fun clearAll() {
        transactionDao.clearAll()
        accountDao.clearAll()
        merchantRuleDao.clearAll()
    }
}
