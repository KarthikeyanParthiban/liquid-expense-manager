package com.expensemanager.app.parser

import com.expensemanager.app.core.model.AccountType
import java.util.regex.Pattern

object BankPatterns {

    data class BankInfo(
        val name: String,
        val defaultType: AccountType = AccountType.BANK_ACCOUNT
    )

    private val BANK_SENDER_MAP = mapOf(
        // HDFC
        "HDFCBK" to BankInfo("HDFC Bank"),
        "HDFCBN" to BankInfo("HDFC Bank"),
        "HDFC" to BankInfo("HDFC Bank"),
        // State Bank of India
        "SBIUPI" to BankInfo("SBI"),
        "SBMSMS" to BankInfo("SBI"),
        "SBIN" to BankInfo("SBI"),
        "SBICRD" to BankInfo("SBI Card", AccountType.CREDIT_CARD),
        "SBI" to BankInfo("SBI"),
        // ICICI
        "ICICIB" to BankInfo("ICICI Bank"),
        "ICICIN" to BankInfo("ICICI Bank"),
        "ICICI" to BankInfo("ICICI Bank"),
        // Axis Bank
        "AXISBK" to BankInfo("Axis Bank"),
        "AXISMR" to BankInfo("Axis Bank"),
        "AXISMF" to BankInfo("Axis Bank"),
        "AXIS" to BankInfo("Axis Bank"),
        // Kotak
        "KOTAKB" to BankInfo("Kotak Mahindra Bank"),
        "KOTAK" to BankInfo("Kotak Mahindra Bank"),
        // Yes Bank
        "YESBNK" to BankInfo("Yes Bank"),
        "YESBAK" to BankInfo("Yes Bank"),
        "YES" to BankInfo("Yes Bank"),
        // Federal Bank & Neo banks
        "FEDBNK" to BankInfo("Federal Bank"),
        "FED" to BankInfo("Federal Bank"),
        "FEDERAL" to BankInfo("Federal Bank"),
        "FIMONEY" to BankInfo("Federal Bank"),
        "JUPITER" to BankInfo("Federal Bank"),
        // IDFC FIRST Bank
        "IDFCFB" to BankInfo("IDFC FIRST Bank"),
        "IDFC" to BankInfo("IDFC FIRST Bank"),
        // Punjab National Bank & BOB
        "PNBSMS" to BankInfo("Punjab National Bank"),
        "PNB" to BankInfo("Punjab National Bank"),
        "BOBTXN" to BankInfo("Bank of Baroda"),
        "BOB" to BankInfo("Bank of Baroda"),
        // Canara & Union Bank
        "CANBNK" to BankInfo("Canara Bank"),
        "CANARA" to BankInfo("Canara Bank"),
        "UBIN" to BankInfo("Union Bank of India"),
        "UNION" to BankInfo("Union Bank of India"),
        // IndusInd & RBL
        "INDUS" to BankInfo("IndusInd Bank"),
        "RBL" to BankInfo("RBL Bank"),
        // Indian Bank & Bank of India
        "IDIBNK" to BankInfo("Indian Bank"),
        "INDIAN" to BankInfo("Indian Bank"),
        "BOITXN" to BankInfo("Bank of India"),
        "BOI" to BankInfo("Bank of India"),
        "IDBIBK" to BankInfo("IDBI Bank"),
        "IDBI" to BankInfo("IDBI Bank"),
        "CENTRAL" to BankInfo("Central Bank of India"),
        "IPPB" to BankInfo("IPPB"),
        // Small Finance & Foreign Banks
        "AUBANK" to BankInfo("AU Small Finance Bank"),
        "AUFIN" to BankInfo("AU Small Finance Bank"),
        "EQUITAS" to BankInfo("Equitas Small Finance Bank"),
        "CITI" to BankInfo("CitiBank"),
        "AMEX" to BankInfo("American Express", AccountType.CREDIT_CARD),
        "HSBC" to BankInfo("HSBC"),
        "SCBL" to BankInfo("Standard Chartered Bank"),
        // Credit Cards & Neo Cards
        "CREDIN" to BankInfo("CRED", AccountType.CREDIT_CARD),
        "CRED" to BankInfo("CRED", AccountType.CREDIT_CARD),
        "SLIC" to BankInfo("Slice", AccountType.CREDIT_CARD),
        "ONECARD" to BankInfo("OneCard", AccountType.CREDIT_CARD),
        "GKIWIP" to BankInfo("Kiwi", AccountType.CREDIT_CARD),
        "KIWI" to BankInfo("Kiwi", AccountType.CREDIT_CARD),
        // Wallets & UPI
        "PAYTM" to BankInfo("Paytm", AccountType.WALLET),
        "JIO" to BankInfo("Jio", AccountType.WALLET),
        "AIRTEL" to BankInfo("Airtel Payments Bank", AccountType.WALLET),
        "GPAY" to BankInfo("Google Pay", AccountType.UPI),
        "PHONPE" to BankInfo("PhonePe", AccountType.UPI),
        "PHONEPE" to BankInfo("PhonePe", AccountType.UPI),
        "NOBRKR" to BankInfo("NoBroker", AccountType.BANK_ACCOUNT),
        "GROWWO" to BankInfo("Groww Wallet", AccountType.WALLET),
        "GROWW" to BankInfo("Groww Wallet", AccountType.WALLET),
        // EPFO
        "EPFO" to BankInfo("EPFO PF Account", AccountType.SAVINGS),
        "EPFOHO" to BankInfo("EPFO PF Account", AccountType.SAVINGS),
        "PFOHOS" to BankInfo("EPFO PF Account", AccountType.SAVINGS),
        "PFOHOG" to BankInfo("EPFO PF Account", AccountType.SAVINGS)
    )

    private val NON_BANK_NOISE_SENDERS = setOf(
        "SESMSS", "TTHPLS", "EDBUSS", "CAMZNS", "LNKITS", "EPTONS", "WIGGYS", "SELTDS", "DSLTXS", "SWIGGY", "ZEPTO", "BLINKIT"
    )

    private val VERIFIED_INSTITUTIONS = setOf(
        "hdfc bank", "yes bank", "axis bank", "sbi", "sbi card", "icici bank", "kotak mahindra bank",
        "punjab national bank", "bank of baroda", "indusind bank", "canara bank",
        "union bank of india", "idfc first bank", "federal bank", "rbl bank", "citibank",
        "american express", "slice", "onecard", "kiwi", "indian bank",
        "bank of india", "idbi bank", "central bank of india", "ippb", "au small finance bank",
        "equitas small finance bank", "hsbc", "standard chartered bank", "paytm payments bank",
        "airtel payments bank"
    )

    fun isVerifiedFinancialInstitution(bankName: String): Boolean {
        val name = bankName.trim().lowercase()
        if (name == "bank account" || name.isEmpty() || name in NON_BANK_NOISE_SENDERS.map { it.lowercase() }) {
            return false
        }
        return VERIFIED_INSTITUTIONS.any { name.contains(it) }
    }

    fun identifyBank(sender: String, messageBody: String): BankInfo {
        val cleanSender = sender.uppercase().replace(Regex("[^A-Z]"), "")

        // Filter known non-bank noise senders
        if (NON_BANK_NOISE_SENDERS.any { cleanSender.contains(it) }) {
            return BankInfo("Bank Account", AccountType.BANK_ACCOUNT)
        }

        // 1. Direct Sender Lookup
        for ((key, info) in BANK_SENDER_MAP) {
            if (cleanSender.contains(key)) {
                val isCard = messageBody.contains("Card", ignoreCase = true) ||
                        messageBody.contains("Credit Card", ignoreCase = true) ||
                        messageBody.contains("Limit", ignoreCase = true) ||
                        messageBody.contains("Avl Lmt", ignoreCase = true) ||
                        messageBody.contains("Credit Limit", ignoreCase = true)

                return if (isCard && info.defaultType == AccountType.BANK_ACCOUNT) {
                    info.copy(defaultType = AccountType.CREDIT_CARD)
                } else info
            }
        }

        // 2. Message Body Lookup
        val bodyUpper = messageBody.uppercase()
        for ((key, info) in BANK_SENDER_MAP) {
            if (bodyUpper.contains(info.name.uppercase()) || bodyUpper.contains(key)) {
                return info
            }
        }

        if (bodyUpper.contains("EPFO") || bodyUpper.contains("PROVIDENT FUND") || bodyUpper.contains("PASSBOOK BALANCE")) {
            return BankInfo("EPFO PF Account", AccountType.SAVINGS)
        }

        if (bodyUpper.contains("GROWW")) {
            return BankInfo("Groww Wallet", AccountType.WALLET)
        }

        // 3. Fallback: Only return generic Bank Account if not recognized
        val isCard = messageBody.contains("Card", ignoreCase = true) ||
                messageBody.contains("Credit Card", ignoreCase = true) ||
                messageBody.contains("Limit", ignoreCase = true) ||
                messageBody.contains("Lmt", ignoreCase = true)

        val bankName = if (cleanSender.length >= 4 && cleanSender !in NON_BANK_NOISE_SENDERS && !cleanSender.matches(Regex("""^[A-Z]{6}$"""))) cleanSender.takeLast(6) else "Bank Account"
        val accountType = if (isCard) AccountType.CREDIT_CARD else AccountType.BANK_ACCOUNT

        return BankInfo(name = bankName, defaultType = accountType)
    }

    // Amount and Currency extraction regexes (Supports INR, USD, EUR, GBP, AED, SGD, CAD, AUD, JPY)
    val CURRENCY_AMOUNT_PATTERNS = listOf(
        Pattern.compile("""(?i)\b(USD|EUR|GBP|AED|SGD|CAD|AUD|JPY|INR|Rs\.?|₹|\$|€|£|S\$|C\$|A\$|¥)\s*([\d,]+(?:\.\d{1,2})?)"""),
        Pattern.compile("""(?i)(?:debited|credited|spent|paid|withdrawn|transferred|charge|refunded|Sent)\s+(?:by|for|of|with)?\s*(USD|EUR|GBP|AED|SGD|CAD|AUD|JPY|INR|Rs\.?|₹|\$|€|£)?\s*([\d,]+(?:\.\d{1,2})?)"""),
        Pattern.compile("""(?i)amount\s*(?:of)?\s*(USD|EUR|GBP|AED|SGD|CAD|AUD|JPY|INR|Rs\.?|₹|\$|€|£)?\s*([\d,]+(?:\.\d{1,2})?)""")
    )

    val AMOUNT_PATTERNS = listOf(
        Pattern.compile("""(?:(?:Rs\.?|INR|₹|USD|EUR|GBP|AED|SGD|CAD|AUD|JPY|\$|€|£)\s*([\d,]+(?:\.\d{1,2})?))""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""(?:(?:debited|credited|spent|paid|withdrawn|transferred|charge|refunded|Sent)\s+(?:by|for|of|with)?\s*(?:Rs\.?|INR|₹|USD|EUR|GBP|AED|SGD|CAD|AUD|JPY|\$|€|£)?\s*([\d,]+(?:\.\d{1,2})?))""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""(?:amount\s*(?:of)?\s*(?:Rs\.?|INR|₹|USD|EUR|GBP|AED|SGD|CAD|AUD|JPY|\$|€|£)?\s*([\d,]+(?:\.\d{1,2})?))""", Pattern.CASE_INSENSITIVE)
    )

    fun normalizeCurrency(rawCurrency: String?): String {
        if (rawCurrency.isNullOrEmpty()) return "INR"
        return when (rawCurrency.uppercase().trim()) {
            "USD", "$" -> "USD"
            "EUR", "€" -> "EUR"
            "GBP", "£" -> "GBP"
            "AED", "DHS" -> "AED"
            "SGD", "S$" -> "SGD"
            "CAD", "C$" -> "CAD"
            "AUD", "A$" -> "AUD"
            "JPY", "¥" -> "JPY"
            else -> "INR"
        }
    }

    fun extractCurrencyAndAmount(body: String): Pair<Double, String>? {
        for (pattern in CURRENCY_AMOUNT_PATTERNS) {
            val matcher = pattern.matcher(body)
            while (matcher.find()) {
                val group1 = matcher.group(1)?.trim()
                val group2 = matcher.group(2)?.trim()

                // Check if group 1 is currency and group 2 is amount
                val amountStr = group2?.replace(",", "")
                val amt = amountStr?.toDoubleOrNull()
                if (amt != null && amt > 0.0) {
                    val curr = normalizeCurrency(group1)
                    return Pair(amt, curr)
                }

                // If only single group matched
                val singleAmt = group1?.replace(",", "")?.toDoubleOrNull()
                if (singleAmt != null && singleAmt > 0.0) {
                    return Pair(singleAmt, "INR")
                }
            }
        }

        // Fallback to AMOUNT_PATTERNS
        for (pattern in AMOUNT_PATTERNS) {
            val matcher = pattern.matcher(body)
            while (matcher.find()) {
                val matchStr = matcher.group(1)?.replace(",", "")?.trim()
                val amt = matchStr?.toDoubleOrNull()
                if (amt != null && amt > 0.0) {
                    // Check if message mentions USD or EUR anywhere
                    val curr = when {
                        Regex("""(?i)\b(?:USD|\$)\b""").containsMatchIn(body) -> "USD"
                        Regex("""(?i)\b(?:EUR|€|Euros?)\b""").containsMatchIn(body) -> "EUR"
                        Regex("""(?i)\b(?:GBP|£|Pounds?)\b""").containsMatchIn(body) -> "GBP"
                        Regex("""(?i)\b(?:AED|Dirhams?)\b""").containsMatchIn(body) -> "AED"
                        else -> "INR"
                    }
                    return Pair(amt, curr)
                }
            }
        }
        return null
    }

    // Account / Card number patterns
    val ACCOUNT_PATTERNS = listOf(
        Pattern.compile("""(?:A/c|Acct|Account|Acc|A/C)\s*(?:no\.?|number)?\s*[*Xx]*([0-9]{3,4})""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""(?:Card|Credit Card|Debit Card)\s*(?:no\.?)?\s*[*Xx]*([0-9]{4})""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""(?:Card\s*X([0-9]{4}))""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""(?:ending\s*(?:with)?\s*[*Xx]*([0-9]{3,4}))""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""(?:UAN|Member\s*ID|PF\s*A/c)\s*[*Xx]*([0-9]{4})""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""(?:[*Xx]{1,}([0-9]{3,4}))""", Pattern.CASE_INSENSITIVE)
    )

    // Available Balance patterns (Handles "Available Bal in HDFC Bank A/c XX7011 ... is INR 88,148.00" and EPFO)
    val BALANCE_PATTERNS = listOf(
        Pattern.compile("""(?:Avail(?:able)?\s*(?:Bal|Balance|Limit|Lmt)|Avl\s*(?:Bal|Lmt|Limit)|Bal|AVL\s*BAL|Net\s*Bal|passbook\s*balance|total\s*balance).*?(?:is\s*)?(?:Rs\.?|INR|₹)\s*([\d,]+(?:\.\d{1,2})?)""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""(?:Avail(?:able)?\s*(?:Bal|Balance|Limit|Lmt)|Avl\s*(?:Bal|Lmt|Limit)|Bal|AVL\s*BAL|Net\s*Bal|passbook\s*balance)[:\s]*(?:is\s*)?(?:Rs\.?|INR|₹)?\s*([\d,]+(?:\.\d{1,2})?)""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""(?:Balance\s*(?:is|:)\s*(?:Rs\.?|INR|₹)?\s*([\d,]+(?:\.\d{1,2})?))""", Pattern.CASE_INSENSITIVE)
    )

    // Reference ID / UTR patterns
    val REFERENCE_PATTERNS = listOf(
        Pattern.compile("""(?:UPI(?:\s*Ref(?:\s*No)?)?|Ref(?:\s*No)?|Txn(?:\s*ID)?|Txn\s*Ref|UTR(?:\s*No)?|RRN|Ref)[:\s#]*([a-zA-Z0-9]{6,24})""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""(?:UPI/(?:P2M|P2A)/([0-9]{6,24}))""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""(?:IMPS|NEFT|RTGS)\s*[-:]\s*[A-Za-z0-9\s&.\-_]+?\s*-\s*([0-9]{6,24})""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""(?:IMPS(?:\s*Ref(?:\s*No)?)?|NEFT|RTGS)[:\s]*([a-zA-Z0-9]{6,24})""", Pattern.CASE_INSENSITIVE)
    )

    // Merchant & Payee extraction patterns
    val MERCHANT_PATTERNS = listOf(
        Pattern.compile("""(?:from\s+VPA|to\s+VPA|VPA)\s+([A-Za-z0-9.\-_]+@[A-Za-z0-9.\-_]+)""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""(?:IST|UTC)\s+([A-Za-z0-9\s*.\-_]+?)(?:\s+Avl|\s+Limit|\s+Not\s+you|\n|\r|$)""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""@(?:UPI_)?([A-Za-z0-9\s&.\-_]+?)(?:\s+\d{2}-\d{2}-\d{4}|\s+Avl|\s+Lmt|\.|\s+SMS|$)""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""(?:PHP\*|PG\*|POS\*|ECOM\*|BIL\*|IN\*)\s*([A-Za-z0-9\s&.\-_]+?)(?:\s+Avl|\s+Limit|\n|\r|$)""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""(?:For|Through)\s+(?:IMPS|NEFT|RTGS)\s*[-:]\s*([A-Za-z0-9\s&.\-_]+?)(?:\s*-\s*[0-9]{6,24}|\n|\r|$)""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""(?m)^To\s+([A-Za-z0-9\s&.\-_@]+?)(?:\s+On|\s+dated|\s+Ref|\n|\r|$)""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""(?:For|To)\s+([A-Za-z0-9\s&.\-_]+?)\s+mandate""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""(?:paid\s+to|sent\s+to|transferred\s+to|spent\s+at|at|to)\s+([A-Za-z0-9&_\-@.]+(?:\s+[A-Za-z0-9&_\-@.]+)*)""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""(?:towards|info:)\s+([A-Za-z0-9&_\-@.]+(?:\s+[A-Za-z0-9&_\-@.]+)*)""", Pattern.CASE_INSENSITIVE)
    )
}
