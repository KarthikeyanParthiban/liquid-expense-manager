package com.expensemanager.app.parser

import com.expensemanager.app.core.model.AccountBalanceUpdate
import com.expensemanager.app.core.model.AccountType
import com.expensemanager.app.core.model.Category
import com.expensemanager.app.core.model.MerchantRule
import com.expensemanager.app.core.model.ParsedSmsResult
import com.expensemanager.app.core.model.ParsingDiagnostics
import com.expensemanager.app.core.model.TransactionStatus
import com.expensemanager.app.core.model.TransactionType

object NotificationParser {

    val SUPPORTED_PACKAGES = setOf(
        // UPI & Payment Apps
        "com.google.android.apps.nbu.paisa.user", // Google Pay
        "com.phonepe.app",                        // PhonePe
        "net.one97.paytm",                        // Paytm
        "com.dreamplug.androidapp",               // CRED
        "in.org.npci.upiapp",                     // BHIM
        "com.amazon.mShop.android.shopping",      // Amazon Pay
        "com.whatsapp",                           // WhatsApp Pay
        "com.navi.mutualfunds",                   // Navi UPI
        "money.jupiter",                          // Jupiter
        "co.epifi.app",                           // Fi Money
        "indwin.c3.sharekar",                     // Slice
        "in.galaxypay",                           // Kiwi

        // Bank Apps
        "com.snapwork.hdfc",                      // HDFC MobileBanking
        "com.hdfcbank.payzapp",                   // HDFC PayZapp
        "com.csam.icici.bank.imobile",            // ICICI iMobile
        "com.sbi.lotusintouch",                   // SBI YONO
        "com.sbi.upi",                            // SBI BHIM Pay
        "com.axis.mobile",                        // Axis Mobile
        "com.msf.kbank.mobile",                   // Kotak 811
        "com.infrasofttech.indianbank",           // Indian Bank
        "com.canarabank.mobility",                // Canara Bank
        "com.bankofbaroda.mconnect",              // BOB World
        "com.idfcfirstbank.optimus",              // IDFC FIRST Bank
        "com.indusind.mplus",                     // IndusInd Bank
        "com.rblbank.mobank",                     // RBL MoBank
        "com.sc.mobile.in"                        // Standard Chartered
    )

    private val PACKAGE_APP_NAME_MAP = mapOf(
        "com.google.android.apps.nbu.paisa.user" to "Google Pay",
        "com.phonepe.app" to "PhonePe",
        "net.one97.paytm" to "Paytm",
        "com.dreamplug.androidapp" to "CRED",
        "in.org.npci.upiapp" to "BHIM",
        "com.amazon.mShop.android.shopping" to "Amazon Pay",
        "com.whatsapp" to "WhatsApp Pay",
        "money.jupiter" to "Jupiter",
        "co.epifi.app" to "Fi Money",
        "indwin.c3.sharekar" to "Slice",
        "in.galaxypay" to "Kiwi",
        "com.snapwork.hdfc" to "HDFC Bank",
        "com.hdfcbank.payzapp" to "HDFC PayZapp",
        "com.csam.icici.bank.imobile" to "ICICI Bank",
        "com.sbi.lotusintouch" to "SBI YONO",
        "com.sbi.upi" to "SBI UPI",
        "com.axis.mobile" to "Axis Bank",
        "com.msf.kbank.mobile" to "Kotak Bank",
        "com.idfcfirstbank.optimus" to "IDFC FIRST Bank",
        "com.bankofbaroda.mconnect" to "Bank of Baroda",
        "com.canarabank.mobility" to "Canara Bank",
        "com.indusind.mplus" to "IndusInd Bank"
    )

    fun isSupportedPackage(packageName: String): Boolean {
        return SUPPORTED_PACKAGES.contains(packageName)
    }

    fun getAppNameForPackage(packageName: String): String {
        return PACKAGE_APP_NAME_MAP[packageName] ?: "Payment Notification"
    }

    /**
     * Parses a notification posted by a banking or UPI app into a structured transaction result.
     */
    fun parse(
        packageName: String,
        title: String?,
        text: String?,
        timestamp: Long,
        userRules: List<MerchantRule> = emptyList()
    ): ParsedSmsResult? {
        val rawTitle = title?.trim() ?: ""
        val rawText = text?.trim() ?: ""
        val fullContent = if (rawTitle.isNotEmpty()) "$rawTitle\n$rawText" else rawText

        if (fullContent.isBlank()) return null
        val lower = fullContent.lowercase()

        // 1. Filter out pure non-transactional notifications (OTPs, promotional marketing, loan offers, EMI conversions)
        if (lower.contains("otp") || lower.contains("verification code") ||
            lower.contains("pre-approved") || lower.contains("pre-qualified") || lower.contains("pre qualified") || lower.contains("pre approved") ||
            lower.contains("personal loan") || lower.contains("loan offer") || lower.contains("instant loan") || lower.contains("business loan") ||
            lower.contains("flexi emi") || lower.contains("into emi") || lower.contains("convert now") || lower.contains("split your") ||
            lower.contains("khushkhabri") || lower.contains("win up to") || lower.contains("cashback offer") ||
            lower.contains("congratulations") || lower.contains("scratch card") || lower.contains("t&c apply") ||
            lower.contains("link par click") || lower.contains("click here") || lower.contains("loan")) {
            return null
        }

        // 2. Extract Amount
        val amount = extractAmount(fullContent) ?: return null

        // 3. Determine Transaction Type
        val type = extractTransactionType(fullContent, lower)

        // 4. Identify Bank or Payment Provider
        val appName = getAppNameForPackage(packageName)
        val identifiedBank = BankPatterns.identifyBank(appName, fullContent)
        val finalBankName = if (identifiedBank.name != "Bank Account") {
            identifiedBank.name
        } else {
            appName
        }

        // 5. Extract Account Mask
        val accountMask = extractAccountMask(fullContent)

        // 6. Extract Merchant / Payee
        val merchant = extractMerchant(fullContent, appName)

        // 7. Extract Reference ID / UTR
        val referenceId = extractReferenceId(fullContent)

        // 8. Extract Balance After
        val balanceAfter = extractBalanceAfter(fullContent)

        // 9. Classify Category
        val categorization = CategoryClassifier.classifyWithReason(
            merchantOrVpa = merchant,
            messageBody = fullContent,
            transactionType = type,
            userRules = userRules
        )

        // 10. Account ID Resolution
        val accountType = if (lower.contains("credit card") || lower.contains("card ending")) {
            AccountType.CREDIT_CARD
        } else {
            identifiedBank.defaultType
        }
        val accountId = "${finalBankName.replace(" ", "_")}_${accountMask ?: "PRIMARY"}"

        return ParsedSmsResult(
            amount = amount,
            currency = "INR",
            type = type,
            status = TransactionStatus.COMPLETED,
            category = categorization.category,
            merchant = merchant,
            bankName = finalBankName,
            accountMask = accountMask,
            accountType = accountType,
            accountId = accountId,
            referenceId = referenceId,
            balanceAfter = balanceAfter,
            timestamp = timestamp,
            confidence = 0.95f,
            isExcludedFromBudget = (type == TransactionType.CARD_SETTLEMENT || categorization.category == Category.TRANSFERS),
            classificationReason = categorization.reason,
            diagnostics = ParsingDiagnostics(
                parserId = "NOTIFICATION_PARSER",
                classificationReason = categorization.reason
            ),
            rawSender = appName,
            rawBody = fullContent
        )
    }

    fun extractBalanceUpdate(
        packageName: String,
        title: String?,
        text: String?,
        timestamp: Long
    ): AccountBalanceUpdate? {
        val rawTitle = title?.trim() ?: ""
        val rawText = text?.trim() ?: ""
        val fullContent = if (rawTitle.isNotEmpty()) "$rawTitle\n$rawText" else rawText

        val balance = extractBalanceAfter(fullContent) ?: return null
        val appName = getAppNameForPackage(packageName)
        val identifiedBank = BankPatterns.identifyBank(appName, fullContent)

        if (!BankPatterns.isVerifiedFinancialInstitution(identifiedBank.name)) {
            return null
        }

        val accountMask = extractAccountMask(fullContent)
        val accountId = "${identifiedBank.name.replace(" ", "_")}_${accountMask ?: "PRIMARY"}"

        return AccountBalanceUpdate(
            accountId = accountId,
            bankName = identifiedBank.name,
            accountType = identifiedBank.defaultType,
            accountMask = accountMask,
            balance = balance,
            timestamp = timestamp,
            rawSender = appName,
            rawBody = fullContent
        )
    }

    private fun extractAmount(content: String): Double? {
        val amountPatterns = listOf(
            // ₹1,450.00 or Rs. 1,450 or INR 1,450
            Regex("""(?:(?:rs|inr|₹)\.?\s*)([0-9]{1,3}(?:,[0-9]{2,3})*(?:\.[0-9]{1,2})?|[0-9]+(?:\.[0-9]{1,2})?)""", RegexOption.IGNORE_CASE),
            // Paid 1450 to / Spent 500 on
            Regex("""(?:paid|spent|sent|received|transferred)\s+(?:(?:rs|inr|₹)\.?\s*)?([0-9]{1,3}(?:,[0-9]{2,3})*(?:\.[0-9]{1,2})?|[0-9]+(?:\.[0-9]{1,2})?)""", RegexOption.IGNORE_CASE),
            // 450.00 spent / paid
            Regex("""([0-9]{1,3}(?:,[0-9]{2,3})*(?:\.[0-9]{1,2})?|[0-9]+(?:\.[0-9]{1,2})?)\s*(?:(?:rs|inr|₹)\.?\s*)?(?:paid|spent|debited|credited)""", RegexOption.IGNORE_CASE)
        )

        for (pattern in amountPatterns) {
            val match = pattern.find(content)
            if (match != null) {
                val rawVal = match.groupValues[1].replace(",", "").trim()
                val parsed = rawVal.toDoubleOrNull()
                if (parsed != null && parsed > 0.0) {
                    return parsed
                }
            }
        }
        return null
    }

    private fun extractTransactionType(content: String, lower: String): TransactionType {
        if (lower.contains("towards your credit card") || lower.contains("towards credit card") ||
            (lower.contains("paid towards") && lower.contains("credit card")) ||
            (lower.contains("bill paid") && lower.contains("credit card")) ||
            lower.contains("credit card payment") ||
            (lower.contains("credit card") && lower.contains("settlement"))
        ) {
            return TransactionType.CARD_SETTLEMENT
        }

        if (lower.contains("refund") || lower.contains("refunded") || lower.contains("reversal") || lower.contains("reversed")) {
            return TransactionType.REFUND
        }

        if (lower.contains("atm cash") || lower.contains("cash withdrawal") || lower.contains("withdrawn from atm")) {
            return TransactionType.CASH_WITHDRAWAL
        }

        if (lower.contains("received") || lower.contains("credited") || lower.contains("cashback received") || lower.contains("money received")) {
            return TransactionType.CREDIT
        }

        if (lower.contains("transferred to") || lower.contains("sent via upi to") || lower.contains("sent to")) {
            return TransactionType.TRANSFER
        }

        return TransactionType.DEBIT
    }

    private fun extractMerchant(content: String, appName: String): String? {
        val merchantPatterns = listOf(
            // "Paid ₹450 to Swiggy using...", "Spent ₹240 at Starbucks with..."
            Regex("""(?:paid|spent|sent|transferred|payment of)\s+(?:(?:rs|inr|₹)\.?\s*[0-9,.]+\s+)?(?:to|at|for|on)\s+([A-Za-z0-9&'.@_-]+(?:\s+[A-Za-z0-9&'.@_-]+){0,4})(?:\s+(?:using|with|from|via|ref|upi|avl|successful|a/c|\.|\n|$)|$)""", RegexOption.IGNORE_CASE),
            // "Paid to Swiggy", "Payment to Blinkit", "Transferred to John"
            Regex("""(?:paid to|paid at|spent at|spent on|transferred to|sent to|payment to)\s+([A-Za-z0-9&'.@_-]+(?:\s+[A-Za-z0-9&'.@_-]+){0,4})(?:\s+(?:using|with|from|via|on|ref|upi|avl|successful|a/c|\.|\n|$)|$)""", RegexOption.IGNORE_CASE),
            // "Received ₹500 from Rahul"
            Regex("""(?:received|received .*)\s+(?:(?:rs|inr|₹)\.?\s*[0-9,.]+\s+)?from\s+([A-Za-z0-9&'.@_-]+(?:\s+[A-Za-z0-9&'.@_-]+){0,4})(?:\s+(?:via|in|on|ref|upi|a/c|\.|\n|$)|$)""", RegexOption.IGNORE_CASE),
            // "... at Amazon Retail", "... for Swiggy"
            Regex("""(?:\s+at|\s+for)\s+([A-Za-z0-9&'.@_-]+(?:\s+[A-Za-z0-9&'.@_-]+){0,4})(?:\s+(?:on|via|ref|using|with|from|a/c|\.|\n|$)|$)""", RegexOption.IGNORE_CASE)
        )

        for (pattern in merchantPatterns) {
            val match = pattern.find(content)
            if (match != null) {
                val candidate = match.groupValues[1].trim()
                val clean = candidate
                    .replace(Regex("""(?i)^(?:the|a|an)\s+"""), "")
                    .replace(Regex("""(?i)\s+(?:via|using|with|from|on|ref|upi|a/c|in|successful).*$"""), "")
                    .replace(Regex("""(?i)\s+(?:bank|card|account|a/c)$"""), "")
                    .trim()
                if (clean.length >= 2 && !clean.equals("upi", ignoreCase = true) && !clean.equals("vpa", ignoreCase = true)) {
                    return clean
                }
            }
        }

        return null
    }

    private fun extractAccountMask(content: String): String? {
        val maskPatterns = listOf(
            Regex("""(?i)(?:a/c|acct|account|card)\s*(?:no\.?)?\s*(?:ending\s*(?:in|with)?\s*)?(?:x{1,}|[*]{1,}|[•]{1,})?\s*([0-9]{3,4})"""),
            Regex("""(?i)(?:x{2,}|[*]{2,}|[•]{2,})\s*([0-9]{3,4})"""),
            Regex("""(?i)(?:ending in|ending with|ending)\s*([0-9]{3,4})""")
        )

        for (pattern in maskPatterns) {
            val match = pattern.find(content)
            if (match != null) {
                return match.groupValues[1]
            }
        }
        return null
    }

    private fun extractReferenceId(content: String): String? {
        val refPatterns = listOf(
            Regex("""(?i)(?:upi\s*ref(?:erence)?(?:\s*no\.?)?|ref(?:\s*no\.?)?|utr(?:\s*no\.?)?|txn(?:\s*id)?)\s*[:#-]?\s*([0-9A-Za-z]{6,16})"""),
            Regex("""(?i)rrn\s*[:#-]?\s*([0-9]{10,14})""")
        )

        for (pattern in refPatterns) {
            val match = pattern.find(content)
            if (match != null) {
                return match.groupValues[1].trim()
            }
        }
        return null
    }

    private fun extractBalanceAfter(content: String): Double? {
        val balPatterns = listOf(
            Regex("""(?i)(?:avl\s*bal(?:ance)?|available\s*bal(?:ance)?|bal(?:ance)?(?:\s*is)?)\s*[:=-]?\s*(?:rs\.?|inr|₹)?\s*([0-9]{1,3}(?:,[0-9]{2,3})*(?:\.[0-9]{1,2})?|[0-9]+(?:\.[0-9]{1,2})?)"""),
            Regex("""(?i)(?:total\s*bal(?:ance)?|new\s*bal(?:ance)?)\s*[:=-]?\s*(?:rs\.?|inr|₹)?\s*([0-9]{1,3}(?:,[0-9]{2,3})*(?:\.[0-9]{1,2})?|[0-9]+(?:\.[0-9]{1,2})?)""")
        )

        for (pattern in balPatterns) {
            val match = pattern.find(content)
            if (match != null) {
                val rawVal = match.groupValues[1].replace(",", "").trim()
                val parsed = rawVal.toDoubleOrNull()
                if (parsed != null && parsed >= 0.0) {
                    return parsed
                }
            }
        }
        return null
    }
}
