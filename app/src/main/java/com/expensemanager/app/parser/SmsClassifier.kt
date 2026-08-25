package com.expensemanager.app.parser

import com.expensemanager.app.core.model.TransactionType

object SmsClassifier {

    private val OTP_PATTERNS = listOf(
        Regex("""(?i)\b(?:otp|one\s*time\s*password|verification\s*code|security\s*code|secret\s*code)\b"""),
        Regex("""(?i)\b(?:do\s*not\s*share|never\s*share\s*your\s*otp|valid\s*for\s*\d+\s*(?:mins|minutes|seconds))\b"""),
        Regex("""(?i)\b(?:use\s*code\s*\d{4,8}\s*to\s*login|login\s*otp)\b""")
    )

    // Future refund, payroll, and RBI e-mandate pre-debit notifications (actual debits are received separately)
    private val REJECT_FUTURE_INTIMATION_PATTERNS = listOf(
        Regex("""(?i)\b(?:will\s+be\s+auto-?debited|will\s+be\s+debited|will\s+be\s+deducted|will\s+be\s+credited|will\s+be\s+refunded|will\s+get\s+refunded)\b"""),
        Regex("""(?i)\b(?:please\s+ensure\s+sufficient\s+(?:limit|balance)|to\s+deactivate\s+the\s+autopay|sihub\.in|managesi)\b"""),
        Regex("""(?i)\b(?:should\s+reflect\s+in\s+\d+[-–]\d+\s+business\s+days|has\s+been\s+initiated.*reflect\s+in\s+your\s+account)\b"""),
        Regex("""(?i)\b(?:remuneration\s+of\s+rs.*will\s+be\s+credited|sent\s+to\s+bank\s+for\s+irctc\s+txn|has\s+been\s+sent\s+to\s+bank)\b""")
    )

    // Bill Due & Statement generation reminders are NOT executed transactions
    private val REJECT_BILL_DUE_REMINDER_PATTERNS = listOf(
        Regex("""(?i)\b(?:is\s+due\s+on|payment\s+is\s+due|statement\s+generated|bill\s+generated|minimum\s+amount\s+due|total\s+amount\s+due|bill\s+due\s+date|bill\s+of\s+rs.*is\s+due|ignore\s+if\s+paid|emi\s+of\s+rs.*is\s+due|emi\s+for.*is\s+due)\b"""),
        Regex("""(?i)\b(?:bill\s+amount|bill\s+for\s+your|amount\s+payable|pay\s+before\s+due\s+date|dues\s+alert|avoid\s+unnecessary\s+charges|planning\s+ahead\s+supports)\b"""),
        Regex("""(?i)\b(?:statement:\s*total\s*due|total\s*due\s*inr|min\s*due\s*inr|min\.?\s*due:|due\s*by\s*\d{2}|pay\s*by\s*\d{2}|bill\s*has\s*been\s*generated|overdue|amt\s*due:|amount\s*due)\b"""),
        Regex("""(?i)\b(?:credit\s*card\s*statement|e-statement|account\s*statement\s*for)\b""")
    )

    // Loan Marketing, Pre-Approved Offers, Jumbo Loan Disbursement Consents
    private val REJECT_LOAN_DISBURSEMENT_OFFER_PATTERNS = listOf(
        Regex("""(?i)\b(?:funds\s+of\s+inr.*require\s+consent|require\s+consent\s+to\s+continue\s+disbursement|disbursement|pre-?approved|apply\s+now|instant\s+loan|loan\s+offer)\b"""),
        Regex("""(?i)\b(?:loan\s+against|unsecured\s+loan|sanctioned|loan\s+journey|complete\s+the\s+pending\s+steps|personal\s+loan.*is\s+disbursed|business\s+loan)\b"""),
        Regex("""(?i)\b(?:credit\s+limit\s+increase\s+offer|upgrade\s+your\s+card|overdraft\s+facility|revised\s+credit\s+limit|access\s+to\s+funds|convert.*into\s+emi|converted\s+into\s+emi|has\s+dues\s+of)\b""")
    )

    // Marketing Ads, Discounts, Workshops, Token payments, Clickbaits
    private val REJECT_MARKETING_ADS_PROMO_PATTERNS = listOf(
        Regex("""(?i)\b(?:grand\s+opening|%\s+off|biggest\s+deals|exciting\s+offers|gift\s+vouchers|token\s+amount\s+of\s+rs|click\s+here\s+to\s+pay)\b"""),
        Regex("""(?i)\b(?:free\s+craft|complimentary|subscription\s+is\s+now\s+active|stream\s+live|gift\s+card.*expires|cashback\s+offer|win\s+up\s+to|cashback\s+bonanza)\b"""),
        Regex("""(?i)\b(?:use\s+coupon|use\s+promo|hurry|valid\s+only\s+on|limited\s+period|macro\s+class|excel\s+goodies|electoral\s+roll)\b"""),
        Regex("""(?i)\b(?:congratulations|you\s*have\s*won|claim\s*now|special\s*offer|exclusive\s*deal|discount\s*coupon)\b""")
    )

    // Trading, Demat, Stock Exchanges (NSE, BSE, MCX)
    private val REJECT_TRADING_DEMAT_PATTERNS = listOf(
        Regex("""(?i)\b(?:traded\s+value|trades\s+executed|trade\s+confirmation|bse\s+trade|nse\s+trade|fno\s+value|securities\s+bal|clcode|mem\.code|mcx|bse|nse\s+ipo|ipo\s+tempsens|ipo\s+milky|ipo.*collect\s+request|cdsl:|nsdl:|pledge\s+accepted)\b""")
    )

    // Travel Status, PNR, Rail Tickets
    private val REJECT_TRAVEL_PNR_PATTERNS = listOf(
        Regex("""(?i)\b(?:pnr\s+status|pnr\s+no|train\s+booking|chart\s+status|food\s+menu\s+options|automatic\s+refund\s+processed|cancelled\s+being\s+waitlist)\b""")
    )

    // Payment Collect & Mandate intent requests
    private val REJECT_REQUEST_PATTERNS = listOf(
        Regex("""(?i)\b(?:has\s+requested\s+money|request(?:ed)?\s+to\s+pay|on\s+approv(?:al|ing).*will\s+be\s+debited|will\s+be\s+debited\s+from\s+your|will\s+be\s+deducted\s+on|e-mandate!.*will\s+be|approve\s+the\s+request)\b"""),
        Regex("""(?i)\b(?:scheduled\s+for|mandate\s+created|mandate\s+registration|mandate\s+successful|autopay\s+activation|autopay\s+request)\b""")
    )

    private val REJECT_FAILED_PATTERNS = listOf(
        Regex("""(?i)\b(?:unsuccessful|failed|declined|could\s+not\s+be\s+processed|transaction\s+failure|txn\s+failed|payment\s+declined|was\s+not\s+completed)\b"""),
        Regex("""(?i)\b(?:insufficient\s+funds|limit\s+exceeded|incorrect\s+pin)\b""")
    )

    private val REJECT_TELECOM_DATA_PATTERNS = listOf(
        Regex("""(?i)\b(?:recharge\s+plan\s+expir|plan\s+expired|data\s+quota|data\s+used|validity\s+expir|quota\s+as\s+per\s+plan|100%\s+of\s+daily\s+data|50%\s+data|90%\s+of\s+available\s+data)\b"""),
        Regex("""(?i)\b(?:pack\s+validity|recharge\s+now\s+to\s+continue|talktime\s+balance)\b""")
    )

    private val REJECT_EPFO_PATTERNS = listOf(
        Regex("""(?i)\b(?:passbook\s+balance|member\s+passbook|uan\b|epfo|contribution\s+of\s+rs.*for\s+due\s+month|claim\s+form-31|officialepfo)\b""")
    )

    private val DEBIT_PATTERNS = listOf(
        Regex("""(?i)\b(?:debited|debit|spent|paid|transferred|withdrawn|withdrew|withdrawal|wdl|cash\s*wdl|atm\s*wdl|purchase|deducted|sent|charged?)\b"""),
        Regex("""(?i)\b(?:dr|dr\.)\b""")
    )

    private val CREDIT_PATTERNS = listOf(
        Regex("""(?i)\b(?:credited|received|refunded|refund|deposited|cashback)\b"""),
        Regex("""(?i)\b(?:cr|cr\.)\b"""),
        Regex("""(?i)\b(?:added\s+to|reversal\s+of|credit\s+of|payment\s+received)\b""")
    )

    fun isFinancialSms(body: String): Boolean {
        val lower = body.lowercase()

        // 1. Filter out future refund / payroll / auto-debit pre-notifications
        if (REJECT_FUTURE_INTIMATION_PATTERNS.any { it.containsMatchIn(body) }) {
            return false
        }

        // 2. Filter out Bill Due / Statement generation reminders (Advisories)
        if (REJECT_BILL_DUE_REMINDER_PATTERNS.any { it.containsMatchIn(body) }) {
            return false
        }

        // 3. Filter out Loan Marketing, Pre-Approved Offers, Jumbo Loan Disbursement Consents
        if (REJECT_LOAN_DISBURSEMENT_OFFER_PATTERNS.any { it.containsMatchIn(body) }) {
            return false
        }

        // 4. Filter out Marketing Ads, Discounts, Workshops, Token payments, Clickbaits
        if (REJECT_MARKETING_ADS_PROMO_PATTERNS.any { it.containsMatchIn(body) }) {
            return false
        }

        // 5. Filter out Trading, Demat, Stock Exchanges (NSE, BSE, MCX, CDSL, NSDL)
        if (REJECT_TRADING_DEMAT_PATTERNS.any { it.containsMatchIn(body) }) {
            return false
        }

        // 6. Filter out Travel / PNR / Rail tickets
        if (REJECT_TRAVEL_PNR_PATTERNS.any { it.containsMatchIn(body) }) {
            return false
        }

        // 7. Filter out payment requests (AutoPay requests, Collect requests)
        if (REJECT_REQUEST_PATTERNS.any { it.containsMatchIn(body) }) {
            return false
        }

        // 8. Filter out failed / declined transactions (unless it is a confirmed refund or reversal credit)
        val isRefundOrReversal = lower.contains("refund") || lower.contains("reversal") || lower.contains("reversed")
        if (!isRefundOrReversal && REJECT_FAILED_PATTERNS.any { it.containsMatchIn(body) }) {
            return false
        }

        // 9. Filter out telecom data / plan expiry notifications
        if (REJECT_TELECOM_DATA_PATTERNS.any { it.containsMatchIn(body) }) {
            return false
        }

        // 10. Filter out EPFO / PF passbook notifications from transaction creation
        if (REJECT_EPFO_PATTERNS.any { it.containsMatchIn(body) }) {
            return false
        }

        // 11. Filter out pure OTPs unless it explicitly contains a completed debit/credit confirmation
        val isOtp = OTP_PATTERNS.any { it.containsMatchIn(body) }
        val hasCompletedDebitOrCredit = lower.contains("has been debited") ||
                lower.contains("was debited") ||
                lower.contains("is debited") ||
                lower.contains("credited to your") ||
                lower.contains("has been credited") ||
                lower.contains("refund of rs") ||
                lower.contains("reversal of rs")

        if (isOtp && !hasCompletedDebitOrCredit) {
            return false
        }

        // 12. Filter out pure daily Balance broadcasts without real debits/credits
        val isDailyBalanceBroadcast = Regex("""(?i)\b(?:available\s+bal(?:ance)?\s+in\s+[a-z\s]+a/c|avl\s+bal\s+in\s+[a-z\s]+a/c|balance\s+in\s+your\s+account\s+as\s+on)\b""").containsMatchIn(body)
        val hasTransactionKeyword = lower.contains("debited") || lower.contains("spent") || lower.contains("sent") ||
                lower.contains("credited") || lower.contains("received") || lower.contains("deposited") ||
                lower.contains("refund") || lower.contains("reversal")

        if (isDailyBalanceBroadcast && !hasTransactionKeyword) {
            return false
        }

        // 13. Must contain monetary indicator (INR, Rs, ₹, USD, etc.) and debit/credit keyword
        val hasCurrencyOrAmount = lower.contains("rs") || lower.contains("inr") ||
                body.contains("₹") || body.contains("$") || lower.contains("usd")

        // Neutralize "credit card", "credit limit", etc. before testing credit keywords
        val cleanBodyForCredit = body.replace(Regex("""(?i)\bcredit\s+(?:card|limit|score|line|facility|bureau|rating)\b"""), "card_token")

        val isDebit = DEBIT_PATTERNS.any { it.containsMatchIn(body) }
        val isCredit = CREDIT_PATTERNS.any { it.containsMatchIn(cleanBodyForCredit) } || isRefundOrReversal

        return hasCurrencyOrAmount && (isDebit || isCredit)
    }

    fun classifyTransactionType(body: String): TransactionType? {
        val lower = body.lowercase()

        // Check for Reversal
        if (lower.contains("reversal") || lower.contains("reversed")) {
            return TransactionType.REVERSAL
        }

        // Check for Refund
        if (lower.contains("refund") || lower.contains("refunded")) {
            return TransactionType.REFUND
        }

        // Check for Credit Card Bill Payment Received (Settlement)
        if (lower.contains("received towards your credit card") ||
            lower.contains("payment of rs") && lower.contains("received towards") ||
            lower.contains("credited to your card ending")
        ) {
            return TransactionType.CARD_SETTLEMENT
        }

        // Check for ATM Cash Withdrawal
        if (lower.contains("cash wdl") || lower.contains("atm wdl") || lower.contains("cash withdrawal") || lower.contains("withdrawn from atm")) {
            return TransactionType.CASH_WITHDRAWAL
        }

        // Neutralize "credit card", "credit limit", etc. before testing credit keywords
        val cleanBodyForCredit = body.replace(Regex("""(?i)\bcredit\s+(?:card|limit|score|line|facility|bureau|rating)\b"""), "card_token")

        val isDebit = DEBIT_PATTERNS.any { it.containsMatchIn(body) }
        val isCredit = CREDIT_PATTERNS.any { it.containsMatchIn(cleanBodyForCredit) }

        return when {
            isDebit && !isCredit -> TransactionType.DEBIT
            isCredit && !isDebit -> TransactionType.CREDIT
            isDebit && isCredit -> {
                val debitPos = DEBIT_PATTERNS.mapNotNull { it.find(body)?.range?.first }.minOrNull() ?: Int.MAX_VALUE
                val creditPos = CREDIT_PATTERNS.mapNotNull { it.find(cleanBodyForCredit)?.range?.first }.minOrNull() ?: Int.MAX_VALUE
                if (debitPos < creditPos) TransactionType.DEBIT else TransactionType.CREDIT
            }
            else -> null
        }
    }
}
