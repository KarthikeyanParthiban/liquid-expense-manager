package com.expensemanager.app.parser

import com.expensemanager.app.core.model.ParsedSmsResult
import com.expensemanager.app.core.model.Transaction
import kotlin.math.abs

object DeduplicationEngine {

    private const val FUZZY_WINDOW_MILLIS = 20 * 60 * 1000L // 20 minutes window

    sealed class DeduplicationResult {
        object Unique : DeduplicationResult()
        data class ExactDuplicate(val existingTransactionId: String) : DeduplicationResult()
        data class FuzzyDuplicate(val existingTransactionId: String, val updatedTransaction: Transaction) : DeduplicationResult()
    }

    fun checkDuplicate(
        candidate: ParsedSmsResult,
        existingTransactions: List<Transaction>
    ): DeduplicationResult {
        // 1. Exact Reference / UTR ID Matching
        if (!candidate.referenceId.isNullOrBlank()) {
            val exactMatch = existingTransactions.firstOrNull { existing ->
                !existing.referenceId.isNullOrBlank() &&
                        existing.referenceId.equals(candidate.referenceId, ignoreCase = true)
            }
            if (exactMatch != null) {
                return DeduplicationResult.ExactDuplicate(exactMatch.id)
            }
        }

        // 2. Exact Raw Body Matching
        if (!candidate.rawBody.isNullOrBlank()) {
            val bodyMatch = existingTransactions.firstOrNull { existing ->
                existing.rawBody != null && existing.rawBody == candidate.rawBody
            }
            if (bodyMatch != null) {
                return DeduplicationResult.ExactDuplicate(bodyMatch.id)
            }
        }

        // 3. Multi-channel Fuzzy Matching (Bank debit + UPI debit + Mandate alert within 20 mins)
        val fuzzyMatch = existingTransactions.firstOrNull { existing ->
            val isSameType = existing.type == candidate.type
            val isSameAmount = abs(existing.amount - candidate.amount) < 0.01
            val isWithinWindow = abs(existing.timestamp - candidate.timestamp) <= FUZZY_WINDOW_MILLIS

            val isSameAccount = existing.accountId == candidate.accountId ||
                    (existing.bankName.equals(candidate.bankName, ignoreCase = true) &&
                            (existing.accountMask == candidate.accountMask || existing.accountMask == null || candidate.accountMask == null))

            val isSameMerchant = !existing.merchantName.isNullOrBlank() &&
                    !candidate.merchant.isNullOrBlank() &&
                    (existing.merchantName.equals(candidate.merchant, ignoreCase = true) ||
                     existing.merchantName.contains(candidate.merchant, ignoreCase = true) ||
                     candidate.merchant.contains(existing.merchantName, ignoreCase = true))

            val isSameSender = existing.sender.equals(candidate.rawSender, ignoreCase = true)

            val isCcSettlement = (existing.category == com.expensemanager.app.core.model.Category.TRANSFERS ||
                    candidate.category == com.expensemanager.app.core.model.Category.TRANSFERS) &&
                    abs(existing.timestamp - candidate.timestamp) <= 60 * 60 * 1000L

            (isSameType || isCcSettlement) && isSameAmount && (isWithinWindow || isCcSettlement) &&
                    (isSameAccount || isSameMerchant || isSameSender || isCcSettlement)
        }

        if (fuzzyMatch != null) {
            val enriched = fuzzyMatch.copy(
                merchantName = fuzzyMatch.merchantName ?: candidate.merchant,
                balanceAfter = fuzzyMatch.balanceAfter ?: candidate.balanceAfter,
                referenceId = fuzzyMatch.referenceId ?: candidate.referenceId,
                accountMask = fuzzyMatch.accountMask ?: candidate.accountMask
            )
            return DeduplicationResult.FuzzyDuplicate(fuzzyMatch.id, enriched)
        }

        return DeduplicationResult.Unique
    }
}
