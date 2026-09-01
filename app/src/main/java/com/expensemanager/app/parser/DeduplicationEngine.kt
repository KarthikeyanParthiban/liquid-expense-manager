package com.expensemanager.app.parser

import com.expensemanager.app.core.model.Category
import com.expensemanager.app.core.model.ParsedSmsResult
import com.expensemanager.app.core.model.Transaction
import com.expensemanager.app.core.model.TransactionType
import kotlin.math.abs

object DeduplicationEngine {

    private const val FUZZY_WINDOW_MILLIS = 20 * 60 * 1000L // 20 minutes window
    private const val SETTLEMENT_WINDOW_MILLIS = 6 * 60 * 60 * 1000L // 6 hours window for CC settlements (banks can delay confirmation by 2–6 hours)

    sealed class DeduplicationResult {
        data class Unique(val reason: String = "No matching transaction found within window") : DeduplicationResult()
        data class ExactDuplicate(val existingTransactionId: String, val reason: String) : DeduplicationResult()
        data class FuzzyDuplicate(val existingTransactionId: String, val updatedTransaction: Transaction, val reason: String) : DeduplicationResult()
        data class MergeWithExisting(val existingTransactionId: String, val updatedTransaction: Transaction, val reason: String) : DeduplicationResult()
        data class RelatedTransaction(val existingTransactionId: String, val relationshipType: String, val reason: String) : DeduplicationResult()
        data class Uncertain(val existingTransactionId: String, val reason: String) : DeduplicationResult()
    }

    fun checkDuplicate(
        candidate: ParsedSmsResult,
        existingTransactions: List<Transaction>
    ): DeduplicationResult {
        // 1. Exact Reference / UTR ID Matching
        if (!candidate.referenceId.isNullOrBlank()) {
            val exactRefMatch = existingTransactions.firstOrNull { existing ->
                !existing.referenceId.isNullOrBlank() &&
                        existing.referenceId.equals(candidate.referenceId, ignoreCase = true)
            }

            if (exactRefMatch != null) {
                // If candidate is a Refund or Reversal referencing original purchase UTR
                if (candidate.type == TransactionType.REFUND || candidate.type == TransactionType.REVERSAL) {
                    return DeduplicationResult.RelatedTransaction(
                        existingTransactionId = exactRefMatch.id,
                        relationshipType = candidate.type.name,
                        reason = "Matched original transaction via reference ID: ${candidate.referenceId}"
                    )
                }

                // If candidate is the same transaction direction or dual-channel notification
                return DeduplicationResult.ExactDuplicate(
                    existingTransactionId = exactRefMatch.id,
                    reason = "Exact reference / UTR ID matched: '${candidate.referenceId}'"
                )
            }
        }

        // 2. Exact Raw Body Matching
        if (!candidate.rawBody.isNullOrBlank()) {
            val bodyMatch = existingTransactions.firstOrNull { existing ->
                existing.rawBody != null && existing.rawBody == candidate.rawBody
            }
            if (bodyMatch != null) {
                return DeduplicationResult.ExactDuplicate(
                    existingTransactionId = bodyMatch.id,
                    reason = "Exact SMS raw body checksum matched"
                )
            }
        }

        // 3. Credit Card Bill Payment & Settlement Reconciliation (Bank Debit + Card Credit Settlement)
        val isCandidateCcSettlement = candidate.type == TransactionType.CARD_SETTLEMENT ||
                candidate.category == Category.TRANSFERS ||
                candidate.rawBody?.contains("received towards your credit card", ignoreCase = true) == true

        if (isCandidateCcSettlement) {
            val settlementMatch = existingTransactions.firstOrNull { existing ->
                val isSameAmount = abs(existing.amount - candidate.amount) < 0.01
                val isWithinWindow = abs(existing.timestamp - candidate.timestamp) <= SETTLEMENT_WINDOW_MILLIS
                val isDebitLeg = existing.type == TransactionType.DEBIT || existing.type == TransactionType.PAYMENT || existing.type == TransactionType.TRANSFER

                isSameAmount && isWithinWindow && isDebitLeg
            }

            if (settlementMatch != null) {
                val enriched = settlementMatch.copy(
                    balanceAfter = settlementMatch.balanceAfter ?: candidate.balanceAfter,
                    referenceId = settlementMatch.referenceId ?: candidate.referenceId,
                    relatedTransactionId = settlementMatch.id
                )
                return DeduplicationResult.FuzzyDuplicate(
                    existingTransactionId = settlementMatch.id,
                    updatedTransaction = enriched,
                    reason = "Credit card bill settlement matched with bank payment debit"
                )
            }
        }

        // 4. Multi-Channel Fuzzy Window (Bank SMS + UPI Notification within 20 mins)
        val fuzzyMatch = existingTransactions.firstOrNull { existing ->
            val isSameType = existing.type == candidate.type
            val isSameAmount = abs(existing.amount - candidate.amount) < 0.01
            val isWithinWindow = abs(existing.timestamp - candidate.timestamp) <= FUZZY_WINDOW_MILLIS

            val isUpiWrapper = existing.sender.equals("Google Pay", ignoreCase = true) ||
                    existing.sender.equals("PhonePe", ignoreCase = true) ||
                    existing.sender.equals("Paytm", ignoreCase = true) ||
                    existing.sender.equals("CRED", ignoreCase = true) ||
                    existing.sender.equals("BHIM", ignoreCase = true) ||
                    existing.sender.equals("Amazon Pay", ignoreCase = true) ||
                    candidate.rawSender?.equals("Google Pay", ignoreCase = true) == true ||
                    candidate.rawSender?.equals("PhonePe", ignoreCase = true) == true ||
                    candidate.rawSender?.equals("Paytm", ignoreCase = true) == true ||
                    candidate.rawSender?.equals("CRED", ignoreCase = true) == true ||
                    candidate.rawSender?.equals("BHIM", ignoreCase = true) == true ||
                    candidate.rawSender?.equals("Amazon Pay", ignoreCase = true) == true

            val isSameAccount = existing.accountId == candidate.accountId ||
                    (existing.bankName.equals(candidate.bankName, ignoreCase = true) &&
                            (existing.accountMask == candidate.accountMask || existing.accountMask == null || candidate.accountMask == null)) ||
                    isUpiWrapper

            val isCompatibleMerchant = when {
                existing.merchantName.isNullOrBlank() || candidate.merchant.isNullOrBlank() -> true
                existing.merchantName.equals(candidate.merchant, ignoreCase = true) -> true
                existing.merchantName.contains(candidate.merchant, ignoreCase = true) -> true
                candidate.merchant.contains(existing.merchantName, ignoreCase = true) -> true
                else -> false
            }

            isSameType && isSameAmount && isWithinWindow && isSameAccount && isCompatibleMerchant
        }

        if (fuzzyMatch != null) {
            val enriched = fuzzyMatch.copy(
                merchantName = fuzzyMatch.merchantName ?: candidate.merchant,
                balanceAfter = fuzzyMatch.balanceAfter ?: candidate.balanceAfter,
                referenceId = fuzzyMatch.referenceId ?: candidate.referenceId,
                accountMask = fuzzyMatch.accountMask ?: candidate.accountMask,
                category = if (fuzzyMatch.category == Category.OTHERS && candidate.category != Category.OTHERS) candidate.category else fuzzyMatch.category
            )

            return DeduplicationResult.FuzzyDuplicate(
                existingTransactionId = fuzzyMatch.id,
                updatedTransaction = enriched,
                reason = "Multi-channel notification reconciled within 20min window"
            )
        }

        return DeduplicationResult.Unique("No duplicate matched")
    }
}
