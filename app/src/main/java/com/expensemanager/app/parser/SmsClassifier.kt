package com.expensemanager.app.parser

import com.expensemanager.app.core.model.TransactionType

object SmsClassifier {

    private val OTP_PATTERNS = listOf(
        Regex("""(?i)\b(?:otp|one\s*time\s*password|verification\s*code|security\s*code|secret\s*code)\b"""),
        Regex("""(?i)\b(?:do\s*not\s*share|never\s*share\s*your\s*otp|valid\s*for\s*\d+\s*(?:mins|minutes|seconds))\b"""),
        Regex("""(?i)\b(?:use\s*code\s*\d{4,8}\s*to\s*login|login\s*otp)\b""")
    )

    // Bill Due & Statement generation reminders are NOT executed transactions
    private val REJECT_BILL_DUE_REMINDER_PATTERNS = listOf(
        Regex("""(?i)\b(?:is\s+due\s+on|payment\s+is\s+due|statement\s+generated|bill\s+generated|minimum\s+amount\s+due|total\s+amount\s+due|bill\s+due\s+date|bill\s+of\s+rs.*is\s+due|ignore\s+if\s+paid|emi\s+of\s+rs.*is\s+due|emi\s+for.*is\s+due)\b"""),
        Regex("""(?i)\b(?:bill\s+amount|bill\s+for\s+your|amount\s+payable|pay\s+before\s+due\s+date|dues\s+alert|avoid\s+unnecessary\s+charges|planning\s+ahead\s+supports)\b""")
    )

    // Loan Marketing, Pre-Approved Offers, Jumbo Loan Disbursement Consents
    private val REJECT_LOAN_DISBURSEMENT_OFFER_PATTERNS = listOf(
        Regex("""(?i)\b(?:funds\s+of\s+inr.*require\s+consent|require\s+consent\s+to\s+continue\s+disbursement|disbursement|pre-?approved|apply\s+now|instant\s+loan|loan\s+offer)\b"""),
        Regex("""(?i)\b(?:loan\s+against|unsecured\s+loan|sanctioned|loan\s+journey|complete\s+the\s+pending\s+steps|personal\s+loan.*is\s+disbursed|business\s+loan)\b"""),
        Regex("""(?i)\b(?:credit\s+limit\s+increase\s+offer|upgrade\s+your\s+card|overdraft\s+facility)\b""")
    )

    // Marketing Ads, Discounts, Workshops, Token payments, Clickbaits
    private val REJECT_MARKETING_ADS_PROMO_PATTERNS = listOf(
        Regex("""(?i)\b(?:grand\s+opening|%\s+off|biggest\s+deals|exciting\s+offers|gift\s+vouchers|token\s+amount\s+of\s+rs|click\s+here\s+to\s+pay)\b"""),
        Regex("""(?i)\b(?:free\s+craft|complimentary|subscription\s+is\s+now\s+active|stream\s+live|gift\s+card.*expires|cashback\s+offer|win\s+up\s+to)\b"""),
        Regex("""(?i)\b(?:use\s+coupon|use\s+promo|hurry|valid\s+only\s+on|limited\s+period|macro\s+class|excel\s+goodies|electoral\s+roll)\b"""),
        Regex("""(?i)\b(?:congratulations|you\s*have\s*won|claim\s*now|special\s*offer|exclusive\s*deal|discount\s*coupon)\b""")
    )

    // Trading, Demat, Stock Exchanges (NSE, BSE, MCX)
    private val REJECT_TRADING_DEMAT_PATTERNS = listOf(
        Regex("""(?i)\b(?:traded\s+value|trades\s+executed|trade\s+confirmation|bse\s+trade|nse\s+trade|fno\s+value|securities\s+bal|clcode|mem\.code|mcx|bse|nse\s+ipo)\b""")
    )

    // Travel Status, PNR, Rail Tickets
    private val REJECT_TRAVEL_PNR_PATTERNS = listOf(
        Regex("""(?i)\b(?:pnr\s+status|pnr\s+no|train\s+booking|chart\s+status|food\s+menu\s+options)\b""")
    )

    // Payment Collect & Mandate intent requests
    private val REJECT_REQUEST_PATTERNS = listOf(
        Regex("""(?i)\b(?:has\s+requested\s+money|request(?:ed)?\s+to\s+pay|on\s+approv(?:al|ing).*will\s+be\s+debited|will\s+be\s+debited\s+from\s+your|will\s+be\s+deducted\s+on|e-mandate!.*will\s+be|approve\s+the\s+request)\b"""),
        Regex("""(?i)\b(?:scheduled\s+for|mandate\s+created|mandate\s+registration|mandate\s+successful)\b""")
    )

    private val REJECT_FAILED_PATTERNS = listOf(
        Regex("""(?i)\b(?:unsuccessful|failed|declined|cancelled|could\s+not\s+be\s+processed|transaction\s+failure|txn\s+failed|payment\s+declined)\b"""),
        Regex("""(?i)\b(?:insufficient\s+funds|limit\s+exceeded|incorrect\s+pin)\b""")
    )

    private val REJECT_TELECOM_DATA_PATTERNS = listOf(
        Regex("""(?i)\b(?:recharge\s+plan\s+expir|plan\s+expired|data\s+quota|data\s+used|validity\s+expir|quota\s+as\s+per\s+plan|100%\s+of\s+daily\s+data|50%\s+data|90%\s+of\s+available\s+data)\b"""),
        Regex("""(?i)\b(?:pack\s+validity|recharge\s+now\s+to\s+continue|talktime\s+balance)\b""")
    )

    private val DEBIT_KEYWORDS = listOf(
        "debited", "debit", "spent", "paid", "transferred", "withdrawn", "purchase",
        "deducted", "sent", "charge", "charged", "dr", "debited by", "debited with", "debited for"
    )

    private val CREDIT_KEYWORDS = listOf(
        "credited", "credit", "received", "refunded", "deposited", "cashback",
        "cr", "credited by", "credited with", "added to", "reversal of", "credit of", "payment received"
    )

    fun isFinancialSms(body: String): Boolean {
        val lower = body.lowercase()

        // 1. Filter out Bill Due / Statement generation reminders (Advisories)
        if (REJECT_BILL_DUE_REMINDER_PATTERNS.any { it.containsMatchIn(body) }) {
            return false
        }

        // 2. Filter out Loan Marketing, Pre-Approved Offers, Jumbo Loan Disbursement Consents
        if (REJECT_LOAN_DISBURSEMENT_OFFER_PATTERNS.any { it.containsMatchIn(body) }) {
            return false
        }

        // 3. Filter out Marketing Ads, Discounts, Workshops, Token payments, Clickbaits
        if (REJECT_MARKETING_ADS_PROMO_PATTERNS.any { it.containsMatchIn(body) }) {
            return false
        }

        // 4. Filter out Trading, Demat, Stock Exchanges (NSE, BSE, MCX)
        if (REJECT_TRADING_DEMAT_PATTERNS.any { it.containsMatchIn(body) }) {
            return false
        }

        // 5. Filter out Travel / PNR / Rail tickets
        if (REJECT_TRAVEL_PNR_PATTERNS.any { it.containsMatchIn(body) }) {
            return false
        }

        // 6. Filter out payment requests (Simpl requested money, Jio requested money on Kiwi, etc.)
        if (REJECT_REQUEST_PATTERNS.any { it.containsMatchIn(body) }) {
            return false
        }

        // 7. Filter out failed / declined transactions
        if (REJECT_FAILED_PATTERNS.any { it.containsMatchIn(body) }) {
            return false
        }

        // 8. Filter out telecom data / plan expiry notifications
        if (REJECT_TELECOM_DATA_PATTERNS.any { it.containsMatchIn(body) }) {
            return false
        }

        // 9. Filter out pure OTPs unless it explicitly contains a completed debit/credit confirmation
        val isOtp = OTP_PATTERNS.any { it.containsMatchIn(body) }
        val hasCompletedDebitOrCredit = lower.contains("has been debited") ||
                lower.contains("was debited") ||
                lower.contains("is debited") ||
                lower.contains("credited to your") ||
                lower.contains("has been credited")

        if (isOtp && !hasCompletedDebitOrCredit) {
            return false
        }

        // 10. Filter out Balance broadcasts without real debits/credits
        if ((lower.contains("available bal in") || lower.contains("avl bal in") || lower.contains("available balance in")) &&
            !lower.contains("debited") && !lower.contains("spent") && !lower.contains("sent") && !lower.contains("credited")
        ) {
            return false
        }

        // 11. Must contain monetary indicator (INR, Rs, ₹, USD, etc.) and debit/credit keyword
        val hasCurrencyOrAmount = lower.contains("rs") || lower.contains("inr") ||
                body.contains("₹") || body.contains("$") || lower.contains("usd")

        val hasTransactionKeyword = DEBIT_KEYWORDS.any { lower.contains(it) } ||
                CREDIT_KEYWORDS.any { lower.contains(it) }

        return hasCurrencyOrAmount && hasTransactionKeyword
    }

    fun classifyTransactionType(body: String): TransactionType? {
        val lower = body.lowercase()

        // Check for Refund / Reversal first
        if (lower.contains("refund") || lower.contains("reversal") || lower.contains("reversed")) {
            return TransactionType.REFUND
        }

        // Check for Credit Card Bill Payment Received (Settlement)
        if (lower.contains("received towards your credit card") ||
            lower.contains("payment of rs") && lower.contains("received towards") ||
            lower.contains("credited to your card ending")
        ) {
            return TransactionType.TRANSFER
        }

        // Check for Debits
        val isDebit = DEBIT_KEYWORDS.any { keyword ->
            if (keyword == "dr") {
                Regex("""\bdr\b|\bdr\.""", RegexOption.IGNORE_CASE).containsMatchIn(body)
            } else {
                lower.contains(keyword)
            }
        }

        // Check for Credits
        val isCredit = CREDIT_KEYWORDS.any { keyword ->
            if (keyword == "cr") {
                Regex("""\bcr\b|\bcr\.""", RegexOption.IGNORE_CASE).containsMatchIn(body)
            } else {
                lower.contains(keyword)
            }
        }

        return when {
            isDebit && !isCredit -> TransactionType.DEBIT
            isCredit && !isDebit -> TransactionType.CREDIT
            isDebit && isCredit -> {
                val debitIdx = DEBIT_KEYWORDS.map { lower.indexOf(it) }.filter { it >= 0 }.minOrNull() ?: Int.MAX_VALUE
                val creditIdx = CREDIT_KEYWORDS.map { lower.indexOf(it) }.filter { it >= 0 }.minOrNull() ?: Int.MAX_VALUE
                if (debitIdx < creditIdx) TransactionType.DEBIT else TransactionType.CREDIT
            }
            else -> null
        }
    }
}
