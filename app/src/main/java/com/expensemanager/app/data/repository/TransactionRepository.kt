package com.expensemanager.app.data.repository

import com.expensemanager.app.core.model.Category
import com.expensemanager.app.core.model.MerchantRule
import com.expensemanager.app.core.model.ParsedSmsResult
import com.expensemanager.app.core.model.Transaction
import com.expensemanager.app.core.model.TransactionStatus
import com.expensemanager.app.core.model.TransactionType
import com.expensemanager.app.data.local.dao.AccountDao
import com.expensemanager.app.data.local.dao.MerchantRuleDao
import com.expensemanager.app.data.local.dao.TransactionDao
import com.expensemanager.app.data.local.entity.AccountEntity
import com.expensemanager.app.data.local.entity.MerchantRuleEntity
import com.expensemanager.app.data.local.entity.TransactionEntity
import com.expensemanager.app.parser.BankPatterns
import com.expensemanager.app.parser.DeduplicationEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao,
    private val merchantRuleDao: MerchantRuleDao
) {
    private val processingMutex = Mutex()

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

    suspend fun processAndSaveSms(result: ParsedSmsResult): DeduplicationEngine.DeduplicationResult = processingMutex.withLock {
        // Fetch existing transactions within +/- 48h window to check duplicates
        val windowStart = result.timestamp - (48 * 60 * 60 * 1000L)
        val windowEnd = result.timestamp + (48 * 60 * 60 * 1000L)
        val candidateExisting = transactionDao.getTransactionsBetween(windowStart, windowEnd).map { it.toDomain() }

        val deduplicationResult = DeduplicationEngine.checkDuplicate(result, candidateExisting)

        when (deduplicationResult) {
            is DeduplicationEngine.DeduplicationResult.Unique,
            is DeduplicationEngine.DeduplicationResult.RelatedTransaction -> {
                var linkedOriginalId: String? = null
                var finalCategory = result.category

                // Automatic Refund / Reversal linking
                if (result.type == TransactionType.REFUND || result.type == TransactionType.REVERSAL) {
                    if (deduplicationResult is DeduplicationEngine.DeduplicationResult.RelatedTransaction) {
                        linkedOriginalId = deduplicationResult.existingTransactionId
                    } else if (!result.referenceId.isNullOrBlank()) {
                        val refMatch = transactionDao.getTransactionByReference(result.referenceId)
                        if (refMatch != null) {
                            linkedOriginalId = refMatch.id
                        }
                    }

                    // If not found by exact reference, search by merchant within past 30 days
                    if (linkedOriginalId == null && !result.merchant.isNullOrBlank()) {
                        val pastMonthStart = result.timestamp - (30 * 24 * 60 * 60 * 1000L)
                        val match = transactionDao.findMatchingDebitForRefund(
                            merchantKeyword = result.merchant,
                            startTime = pastMonthStart,
                            endTime = result.timestamp
                        )
                        if (match != null) {
                            linkedOriginalId = match.id
                        }
                    }

                    // If linked to an original debit, mirror its category so the refund nets out correctly
                    if (linkedOriginalId != null) {
                        val originalEntity = transactionDao.getTransactionById(linkedOriginalId)
                        if (originalEntity != null) {
                            finalCategory = Category.fromString(originalEntity.category)
                            // If it's a full refund/reversal, mark original status
                            if (Math.abs(originalEntity.amount - result.amount) < 0.01) {
                                val updatedStatus = if (result.type == TransactionType.REVERSAL) TransactionStatus.REVERSED else TransactionStatus.REFUNDED
                                transactionDao.updateStatus(originalEntity.id, updatedStatus.name)
                            }
                        }
                    }
                }

                val newTransaction = Transaction(
                    id = UUID.randomUUID().toString(),
                    rawSmsId = null,
                    sender = result.rawSender,
                    amount = result.amount,
                    currency = result.currency,
                    type = result.type,
                    status = result.status,
                    category = finalCategory,
                    merchantName = result.merchant,
                    accountId = result.accountId,
                    bankName = result.bankName,
                    accountType = result.accountType,
                    accountMask = result.accountMask,
                    referenceId = result.referenceId,
                    balanceAfter = result.balanceAfter,
                    timestamp = result.timestamp,
                    isExcludedFromBudget = result.isExcludedFromBudget,
                    originalTransactionId = linkedOriginalId ?: result.originalTransactionId,
                    relatedTransactionId = result.relatedTransactionId,
                    confidence = result.confidence,
                    classificationReason = result.classificationReason,
                    rawBody = result.rawBody
                )
                transactionDao.insert(TransactionEntity.fromDomain(newTransaction))

                // Only create/update account cards for verified financial institutions
                if (BankPatterns.isVerifiedFinancialInstitution(result.bankName)) {
                    val resolvedAccountId = resolveTargetAccountId(result.bankName, result.accountType.name, result.accountMask, result.accountId)
                    val existingAccount = accountDao.getAccountById(resolvedAccountId)
                    val updatedAccount = if (existingAccount != null) {
                        val isNewer = result.timestamp >= existingAccount.lastUpdated
                        existingAccount.copy(
                            lastKnownBalance = if (isNewer && result.balanceAfter != null) result.balanceAfter else (existingAccount.lastKnownBalance ?: result.balanceAfter),
                            lastUpdated = maxOf(existingAccount.lastUpdated, result.timestamp)
                        )
                    } else {
                        AccountEntity(
                            id = resolvedAccountId,
                            bankName = result.bankName,
                            accountType = result.accountType.name,
                            maskNumber = result.accountMask ?: "PRIMARY",
                            lastKnownBalance = result.balanceAfter,
                            lastUpdated = result.timestamp
                        )
                    }
                    accountDao.insertOrUpdate(updatedAccount)
                }
            }
            is DeduplicationEngine.DeduplicationResult.FuzzyDuplicate -> {
                transactionDao.update(TransactionEntity.fromDomain(deduplicationResult.updatedTransaction))
            }
            is DeduplicationEngine.DeduplicationResult.MergeWithExisting -> {
                transactionDao.update(TransactionEntity.fromDomain(deduplicationResult.updatedTransaction))
            }
            is DeduplicationEngine.DeduplicationResult.ExactDuplicate,
            is DeduplicationEngine.DeduplicationResult.Uncertain -> {
                // Avoid unsafe duplicate insertion
            }
        }

        return deduplicationResult
    }

    suspend fun processBalanceUpdate(update: com.expensemanager.app.core.model.AccountBalanceUpdate) = processingMutex.withLock {
        if (!BankPatterns.isVerifiedFinancialInstitution(update.bankName)) {
            return@withLock
        }

        val resolvedAccountId = resolveTargetAccountId(update.bankName, update.accountType.name, update.accountMask, update.accountId)
        val existingAccount = accountDao.getAccountById(resolvedAccountId)
        val updatedAccount = if (existingAccount != null) {
            val shouldUpdateBalance = update.timestamp >= existingAccount.lastUpdated || existingAccount.lastKnownBalance == null
            existingAccount.copy(
                lastKnownBalance = if (shouldUpdateBalance) update.balance else existingAccount.lastKnownBalance,
                lastUpdated = maxOf(existingAccount.lastUpdated, update.timestamp)
            )
        } else {
            AccountEntity(
                id = resolvedAccountId,
                bankName = update.bankName,
                accountType = update.accountType.name,
                maskNumber = update.accountMask ?: "PRIMARY",
                lastKnownBalance = update.balance,
                lastUpdated = update.timestamp
            )
        }
        accountDao.insertOrUpdate(updatedAccount)
    }

    private suspend fun resolveTargetAccountId(
        bankName: String,
        accountTypeName: String,
        accountMask: String?,
        rawAccountId: String
    ): String {
        if (accountMask != null && accountMask != "PRIMARY") {
            return rawAccountId
        }
        val existingAccounts = accountDao.getAllAccountsSnapshot()
        val match = existingAccounts.firstOrNull {
            it.bankName.equals(bankName, ignoreCase = true) &&
                    it.accountType.equals(accountTypeName, ignoreCase = true) &&
                    it.maskNumber != "PRIMARY"
        }
        return match?.id ?: rawAccountId
    }

    suspend fun updateTransaction(transaction: Transaction) = processingMutex.withLock {
        transactionDao.update(TransactionEntity.fromDomain(transaction))
    }

    suspend fun updateCategoryAndCreateRule(merchantKeyword: String, newCategory: Category) = processingMutex.withLock {
        merchantRuleDao.insert(
            MerchantRuleEntity(
                merchantPattern = merchantKeyword.trim(),
                category = newCategory.name,
                updatedAt = System.currentTimeMillis()
            )
        )
        transactionDao.updateCategoryForMerchant(merchantKeyword.trim(), newCategory.name)
    }

    suspend fun deleteTransaction(id: String) = processingMutex.withLock {
        transactionDao.deleteById(id)
    }

    fun getAllRules(): Flow<List<MerchantRule>> =
        merchantRuleDao.getAllRules().map { entities -> entities.map { it.toDomain() } }

    suspend fun getAllRulesSnapshot(): List<MerchantRule> =
        merchantRuleDao.getAllRulesSnapshot().map { it.toDomain() }

    suspend fun deleteRule(rule: MerchantRule) = processingMutex.withLock {
        merchantRuleDao.delete(MerchantRuleEntity.fromDomain(rule))
    }

    suspend fun clearAll() = processingMutex.withLock {
        transactionDao.clearAll()
        accountDao.clearAll()
        merchantRuleDao.clearAll()
    }
}
