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
                    val hasMask = !result.accountMask.isNullOrBlank() && result.accountMask != "PRIMARY"
                    val resolvedAccountId = resolveTargetAccountId(result.bankName, result.accountType.name, result.accountMask, result.accountId)
                    val existingAccount = accountDao.getAccountById(resolvedAccountId)
                    if (existingAccount != null) {
                        val isNewer = result.timestamp >= existingAccount.lastUpdated
                        val updatedAccount = existingAccount.copy(
                            lastKnownBalance = if (isNewer && result.balanceAfter != null) result.balanceAfter else (existingAccount.lastKnownBalance ?: result.balanceAfter),
                            lastUpdated = maxOf(existingAccount.lastUpdated, result.timestamp)
                        )
                        accountDao.insertOrUpdate(updatedAccount)
                    } else if (hasMask) {
                        // ONLY create a new bank account card if ending account digits are present
                        val newAccount = AccountEntity(
                            id = resolvedAccountId,
                            bankName = result.bankName,
                            accountType = result.accountType.name,
                            maskNumber = result.accountMask!!,
                            lastKnownBalance = result.balanceAfter,
                            lastUpdated = result.timestamp
                        )
                        accountDao.insertOrUpdate(newAccount)
                    }
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

        val hasMask = !update.accountMask.isNullOrBlank() && update.accountMask != "PRIMARY"
        val resolvedAccountId = resolveTargetAccountId(update.bankName, update.accountType.name, update.accountMask, update.accountId)
        val existingAccount = accountDao.getAccountById(resolvedAccountId)
        if (existingAccount != null) {
            val shouldUpdateBalance = update.timestamp >= existingAccount.lastUpdated || existingAccount.lastKnownBalance == null
            val updatedAccount = existingAccount.copy(
                lastKnownBalance = if (shouldUpdateBalance) update.balance else existingAccount.lastKnownBalance,
                lastUpdated = maxOf(existingAccount.lastUpdated, update.timestamp)
            )
            accountDao.insertOrUpdate(updatedAccount)
        } else if (hasMask) {
            // ONLY create a new bank account card if ending account digits are present
            val newAccount = AccountEntity(
                id = resolvedAccountId,
                bankName = update.bankName,
                accountType = update.accountType.name,
                maskNumber = update.accountMask!!,
                lastKnownBalance = update.balance,
                lastUpdated = update.timestamp
            )
            accountDao.insertOrUpdate(newAccount)
        }
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

    suspend fun reclassifyAndHealDatabase(): Int = processingMutex.withLock {
        android.util.Log.d("HealDB", "Starting fast reclassifyAndHealDatabase...")
        val entities = transactionDao.getAllTransactionsList()
        android.util.Log.d("HealDB", "Found ${entities.size} entities to evaluate")
        var fixedCount = 0

        for (entity in entities) {
            val body = entity.rawBody ?: continue

            // 1. Purge non-financial noise, promotional cashback ads, telecom validity confirmations, and rogue chat app notification leaks
            if (entity.sender == "WhatsApp Pay" || entity.sender.contains("WhatsApp", ignoreCase = true) || !com.expensemanager.app.parser.SmsClassifier.isFinancialSms(body)) {
                android.util.Log.d("HealDB", "Purging non-financial noise / chat leak: id=${entity.id} sender='${entity.sender}' body='${body.take(50)}'")
                transactionDao.deleteById(entity.id)
                fixedCount++
                continue
            }

            val bankInfo = BankPatterns.identifyBank(entity.sender, body)
            val mask = com.expensemanager.app.parser.SmsParser.extractAccountMask(body) ?: entity.accountMask
            val targetAccountId = "${bankInfo.name.replace(" ", "_")}_${mask ?: "PRIMARY"}"
            android.util.Log.d("HealDB", "Row: id=${entity.id} sender='${entity.sender}' currBank='${entity.bankName}' identBank='${bankInfo.name}'")

            // If bankName, accountId, or accountMask changed
            if (bankInfo.name != entity.bankName ||
                targetAccountId != entity.accountId ||
                (mask != null && mask != entity.accountMask)
            ) {
                android.util.Log.d("HealDB", "Fixing ${entity.id}: Old bank='${entity.bankName}' -> New bank='${bankInfo.name}', Old account='${entity.accountId}' -> New account='$targetAccountId'")
                val updated = entity.copy(
                    bankName = bankInfo.name,
                    accountId = targetAccountId,
                    accountMask = mask ?: entity.accountMask,
                    accountType = bankInfo.defaultType.name
                )
                transactionDao.update(updated)
                fixedCount++
            }
        }

        android.util.Log.d("HealDB", "Finished fast reclassifyAndHealDatabase, fixedCount=$fixedCount")
        syncAccountsTable()

        return@withLock fixedCount
    }

    private suspend fun syncAccountsTable() {
        // 1. Purge all legacy PRIMARY accounts
        accountDao.deleteAllPrimaryAccounts()

        val allTxns = transactionDao.getAllTransactionsList()
        val existingAccounts = accountDao.getAllRawAccounts()

        // Find active account IDs that have transactions
        val activeAccountIds = allTxns.map { it.accountId }.toSet()

        // Delete accounts that no longer have any transactions (e.g. misclassified accounts)
        for (account in existingAccounts) {
            if (account.id !in activeAccountIds) {
                accountDao.deleteAccount(account.id)
            }
        }

        // Create/update accounts for all valid transactions
        val groupedByAccount = allTxns.groupBy { it.accountId }
        for ((accId, txns) in groupedByAccount) {
            val sample = txns.first()
            if (!BankPatterns.isVerifiedFinancialInstitution(sample.bankName)) continue
            val hasMask = !sample.accountMask.isNullOrBlank() && sample.accountMask != "PRIMARY"
            if (!hasMask) continue

            val latestTxnWithBalance = txns.filter { it.balanceAfter != null }.maxByOrNull { it.timestamp }
            val latestTxn = txns.maxByOrNull { it.timestamp } ?: sample

            val existing = accountDao.getAccountById(accId)
            val newAccount = AccountEntity(
                id = accId,
                bankName = sample.bankName,
                accountType = sample.accountType,
                maskNumber = sample.accountMask!!,
                lastKnownBalance = latestTxnWithBalance?.balanceAfter ?: existing?.lastKnownBalance,
                lastUpdated = latestTxn.timestamp
            )
            accountDao.insertOrUpdate(newAccount)
        }
    }

    suspend fun clearAll() = processingMutex.withLock {
        transactionDao.clearAll()
        accountDao.clearAll()
        merchantRuleDao.clearAll()
    }
}

