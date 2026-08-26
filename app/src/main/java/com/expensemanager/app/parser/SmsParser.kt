package com.expensemanager.app.parser

import com.expensemanager.app.core.model.AccountBalanceUpdate
import com.expensemanager.app.core.model.AccountType
import com.expensemanager.app.core.model.Category
import com.expensemanager.app.core.model.MerchantRule
import com.expensemanager.app.core.model.ParsedSmsResult
import com.expensemanager.app.core.model.ParsedTransactionResult
import com.expensemanager.app.core.model.TransactionStatus
import com.expensemanager.app.core.model.TransactionType
import com.expensemanager.app.parser.pipeline.ConfidenceScorer
import com.expensemanager.app.parser.pipeline.IntentDetector
import java.util.Locale

object SmsParser {

    fun parseWithDiagnostics(
        sender: String,
        body: String,
        timestamp: Long,
        userRules: List<MerchantRule> = emptyList()
    ): ParsedTransactionResult? {
        val result = parse(sender, body, timestamp, userRules) ?: return null
        return ParsedTransactionResult(
            transaction = result,
            confidence = result.confidence,
            diagnostics = result.diagnostics
        )
    }

    fun parse(
        sender: String,
        body: String,
        timestamp: Long,
        userRules: List<MerchantRule> = emptyList()
    ): ParsedSmsResult? {
        if (!SmsClassifier.isFinancialSms(body)) {
            return null
        }

        val bankInfo = BankPatterns.identifyBank(sender, body)
        val isRecognizedBank = bankInfo.name != "Bank Account" && !bankInfo.name.matches(Regex("""^[A-Z]{4,6}$"""))

        val type = IntentDetector.detectIntent(body, bankInfo.defaultType)
        val amount = extractAmount(body) ?: return null
        if (amount <= 0.0) return null

        val accountMask = extractAccountMask(body)
        val accountId = "${bankInfo.name.replace(" ", "_")}_${accountMask ?: "PRIMARY"}"
        val balance = extractBalance(body)
        val referenceId = extractReferenceId(body)
        val merchant = extractMerchant(body)

        val catResult = CategoryClassifier.classifyWithReason(merchant, body, type, userRules)
        val category = catResult.category

        // ONLY exclude genuine inter-account transfers, CC bill payments, settlements, and ATM withdrawals
        val isExcluded = category == Category.TRANSFERS ||
                type == TransactionType.CARD_SETTLEMENT ||
                type == TransactionType.CASH_WITHDRAWAL ||
                type == TransactionType.TRANSFER ||
                type == TransactionType.BILL_DUE ||
                body.contains("cred", ignoreCase = true) ||
                body.contains("credit card payment", ignoreCase = true) ||
                body.contains("received towards your credit card", ignoreCase = true) ||
                body.contains("towards your credit card", ignoreCase = true) ||
                body.contains("self transfer", ignoreCase = true) ||
                body.contains("transfer to own", ignoreCase = true)

        val rulesFired = mutableListOf<String>().apply {
            add("Intent: ${type.name}")
            add("Category: ${catResult.reason}")
            if (isExcluded) add("Budget: Excluded from monthly expense")
        }

        val scoreResult = ConfidenceScorer.score(
            amount = amount,
            bankRecognized = isRecognizedBank,
            accountMask = accountMask,
            referenceId = referenceId,
            merchant = merchant,
            balanceAfter = balance,
            rulesFired = rulesFired,
            classificationReason = catResult.reason
        )

        return ParsedSmsResult(
            amount = amount,
            currency = "INR",
            type = type,
            status = TransactionStatus.COMPLETED,
            category = category,
            merchant = merchant,
            bankName = bankInfo.name,
            accountMask = accountMask,
            accountType = bankInfo.defaultType,
            accountId = accountId,
            referenceId = referenceId,
            balanceAfter = balance,
            timestamp = timestamp,
            confidence = scoreResult.score,
            isExcludedFromBudget = isExcluded,
            classificationReason = catResult.reason,
            diagnostics = scoreResult.diagnostics,
            rawSender = sender,
            rawBody = body
        )
    }

    fun extractBalanceUpdate(
        sender: String,
        body: String,
        timestamp: Long
    ): AccountBalanceUpdate? {
        val balance = extractBalance(body) ?: return null
        val bankInfo = BankPatterns.identifyBank(sender, body)
        if (!BankPatterns.isVerifiedFinancialInstitution(bankInfo.name)) {
            return null
        }
        val lower = body.lowercase()
        // Ignore promotional loan marketing, telecom data reminders, and pure OTPs
        if (lower.contains("data quota") || lower.contains("daily data") || lower.contains("100% of daily") ||
            lower.contains("pre-approved loan") || lower.contains("apply now") ||
            (lower.contains("otp") && !lower.contains("debited") && !lower.contains("credited") && !lower.contains("available bal"))
        ) {
            return null
        }
        val accountMask = extractAccountMask(body)
        val accountId = "${bankInfo.name.replace(" ", "_")}_${accountMask ?: "PRIMARY"}"

        return AccountBalanceUpdate(
            accountId = accountId,
            bankName = bankInfo.name,
            accountType = bankInfo.defaultType,
            accountMask = accountMask,
            balance = balance,
            timestamp = timestamp,
            rawSender = sender,
            rawBody = body
        )
    }

    private fun extractAmount(body: String): Double? {
        for (pattern in BankPatterns.AMOUNT_PATTERNS) {
            val matcher = pattern.matcher(body)
            while (matcher.find()) {
                val matchStr = matcher.group(1)?.replace(",", "")?.trim()
                if (!matchStr.isNullOrEmpty()) {
                    val amt = matchStr.toDoubleOrNull()
                    if (amt != null && amt > 0.0) {
                        return amt
                    }
                }
            }
        }
        return null
    }

    private fun extractAccountMask(body: String): String? {
        for (pattern in BankPatterns.ACCOUNT_PATTERNS) {
            val matcher = pattern.matcher(body)
            if (matcher.find()) {
                val digits = matcher.group(1)?.trim()
                if (!digits.isNullOrEmpty()) {
                    return "XX$digits"
                }
            }
        }
        return null
    }

    private fun extractBalance(body: String): Double? {
        for (pattern in BankPatterns.BALANCE_PATTERNS) {
            val matcher = pattern.matcher(body)
            if (matcher.find()) {
                val matchStr = matcher.group(1)?.replace(",", "")?.trim()
                if (!matchStr.isNullOrEmpty()) {
                    val bal = matchStr.toDoubleOrNull()
                    if (bal != null && bal >= 0.0) {
                        return bal
                    }
                }
            }
        }
        return null
    }

    private fun extractReferenceId(body: String): String? {
        for (pattern in BankPatterns.REFERENCE_PATTERNS) {
            val matcher = pattern.matcher(body)
            if (matcher.find()) {
                val ref = matcher.group(1)?.trim()
                if (!ref.isNullOrEmpty() && ref.length in 6..30 &&
                    !ref.equals("Mandate", ignoreCase = true) &&
                    !ref.equals("BLOCK", ignoreCase = true) &&
                    !ref.equals("CALL", ignoreCase = true) &&
                    !ref.equals("SMS", ignoreCase = true)
                ) {
                    return ref
                }
            }
        }
        return null
    }

    private fun extractMerchant(body: String): String? {
        val cleanBody = body
            .replace(Regex("""(?i)(?:SMS\s+BLKCC|SMS\s+BLOCK|Not\s+You\?|Call\s+1800|Cheques\s+are\s+subject|dial\s+1800|Maintain\s+Balance).*$""", RegexOption.DOT_MATCHES_ALL), "")
            .trim()

        for (pattern in BankPatterns.MERCHANT_PATTERNS) {
            val matcher = pattern.matcher(cleanBody)
            if (matcher.find()) {
                val rawMerchant = matcher.group(1)?.trim()
                if (!rawMerchant.isNullOrEmpty()) {
                    val clean = cleanMerchantName(rawMerchant)
                    if (clean.isNotBlank() && clean.length in 2..40 && !clean.matches(Regex("""^\d+$"""))) {
                        return clean
                    }
                }
            }
        }
        return null
    }

    private fun cleanMerchantName(raw: String): String {
        var clean = raw
            .replace(Regex("""(?i)^(?:vpa|info:|towards|to|at|PHP\*|PG\*|POS\*|ECOM\*|BIL\*|IN\*|@UPI_)\s*"""), "")
            .replace(Regex("""(?i)\s+(?:on|dated|ref|bal|avl|lmt|txn|using|via|through|from|is).*$"""), "")
            .replace(Regex("""[^\w\s&.\-@]"""), " ")
            .trim()

        val noiseWords = setOf(
            "account", "acct", "bank", "bal", "balance", "card", "rs", "inr", "otp",
            "ref", "txn", "avl", "available", "lmt", "limit", "yesg", "ybl", "maintain",
            "process", "auto-debit", "facility"
        )
        val words = clean.split(Regex("""\s+""")).filter { it.lowercase(Locale.ROOT) !in noiseWords }
        clean = words.joinToString(" ")

        return clean.take(40)
    }
}
