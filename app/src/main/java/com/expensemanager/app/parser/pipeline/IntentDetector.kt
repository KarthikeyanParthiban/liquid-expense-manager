package com.expensemanager.app.parser.pipeline

import com.expensemanager.app.core.model.AccountType
import com.expensemanager.app.core.model.TransactionType

object IntentDetector {

    private val REVERSAL_PATTERNS = listOf(
        Regex("""(?i)\b(?:reversal\s+of|reversed|txn\s+reversed|chargeback|reversal\s+txn)\b"""),
        Regex("""(?i)\b(?:has\s+been\s+reversed|amount\s+reversed)\b""")
    )

    private val REFUND_PATTERNS = listOf(
        Regex("""(?i)\b(?:refund|refunded|refund\s+of|credit\s+for\s+cancelled|money\s+refunded)\b""")
    )

    private val CASH_WITHDRAWAL_PATTERNS = listOf(
        Regex("""(?i)\b(?:cash\s*wdl|atm\s*wdl|withdrawn\s+at\s+atm|atm\s+cash|cash\s+withdrawal|withdrawn\s+from\s+atm)\b"""),
        Regex("""(?i)\b(?:nfs\s*atm|matm|atw\b)\b""")
    )

    private val CARD_SETTLEMENT_PATTERNS = listOf(
        Regex("""(?i)\b(?:received\s+towards\s+your\s+credit\s+card|payment\s+received\s+towards\s+your\s+card|payment\s+of\s+rs.*received\s+towards)\b"""),
        Regex("""(?i)\b(?:credited\s+to\s+your\s+card\s+ending|payment\s+towards\s+credit\s+card|towards\s+card\s+ending)\b""")
    )

    private val BILL_PAYMENT_PATTERNS = listOf(
        Regex("""(?i)\b(?:billdesk|cred\s+club|cred\.club|paid\s+to\s+cred|bbps\s+bill\s+pay|auto-debit\s+towards\s+card)\b""")
    )

    private val TRANSFER_PATTERNS = listOf(
        Regex("""(?i)\b(?:sent\s+to\s+vpa|transferred\s+to|transferred\s+from|transfer\s+to|transfer\s+from|self\s+transfer|imps\s+outward|neft\s+outward|rtgs\s+outward|p2p\s+debit)\b""")
    )

    private val DEBIT_PATTERNS = listOf(
        Regex("""(?i)\b(?:debited|debit|spent|paid|purchase|deducted|sent|charged?|withdrawn|withdrew|withdrawal|wdl)\b"""),
        Regex("""(?i)\b(?:dr|dr\.)\b""")
    )

    private val CREDIT_PATTERNS = listOf(
        Regex("""(?i)\b(?:credited|received|deposited|cashback)\b"""),
        Regex("""(?i)\b(?:cr|cr\.)\b"""),
        Regex("""(?i)\b(?:added\s+to|credit\s+of|payment\s+received)\b""")
    )

    fun detectIntent(body: String, accountType: AccountType = AccountType.BANK_ACCOUNT): TransactionType {
        val lower = body.lowercase()

        // 1. Reversals
        if (REVERSAL_PATTERNS.any { it.containsMatchIn(body) }) {
            return TransactionType.REVERSAL
        }

        // 2. Refunds
        if (REFUND_PATTERNS.any { it.containsMatchIn(body) }) {
            return TransactionType.REFUND
        }

        // 3. ATM Cash Withdrawal
        if (CASH_WITHDRAWAL_PATTERNS.any { it.containsMatchIn(body) }) {
            return TransactionType.CASH_WITHDRAWAL
        }

        // 4. Credit Card Bill Settlement (Credit side of CC bill payment)
        if (CARD_SETTLEMENT_PATTERNS.any { it.containsMatchIn(body) }) {
            return TransactionType.CARD_SETTLEMENT
        }

        // 5. Bill Payment / Transfers
        if (BILL_PAYMENT_PATTERNS.any { it.containsMatchIn(body) }) {
            return TransactionType.TRANSFER
        }

        // 6. Inter-account / P2P Outbound Transfers (Must not be a credit/deposit into the account)
        val cleanBodyForCredit = body.replace(Regex("""(?i)\bcredit\s+(?:card|limit|score|line|facility|bureau|rating)\b"""), "card_token")
        val isCredit = CREDIT_PATTERNS.any { it.containsMatchIn(cleanBodyForCredit) }

        if (!isCredit && TRANSFER_PATTERNS.any { it.containsMatchIn(body) }) {
            return TransactionType.TRANSFER
        }

        // 7. General Debit vs Credit classification
        val isDebit = DEBIT_PATTERNS.any { it.containsMatchIn(body) }

        return when {
            isDebit && !isCredit -> TransactionType.DEBIT
            isCredit && !isDebit -> TransactionType.CREDIT
            isDebit && isCredit -> {
                val debitPos = DEBIT_PATTERNS.mapNotNull { it.find(body)?.range?.first }.minOrNull() ?: Int.MAX_VALUE
                val creditPos = CREDIT_PATTERNS.mapNotNull { it.find(cleanBodyForCredit)?.range?.first }.minOrNull() ?: Int.MAX_VALUE
                if (debitPos < creditPos) TransactionType.DEBIT else TransactionType.CREDIT
            }
            else -> TransactionType.DEBIT
        }
    }
}
