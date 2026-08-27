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

    /**
     * Strips all VPA handles, recipient payee addresses, and UPI references so that
     * beneficiary VPA handles (e.g. merchant@okhdfcbank, user@icici, abc@paytm) are NEVER
     * mistaken as the user's owning bank account.
     */
    fun sanitizeBodyForBankIdentification(messageBody: String): String {
        var clean = messageBody
            // Remove full UPI URI / Info strings (e.g. Info: UPI/P2M/629104819201/user@okhdfcbank)
            .replace(Regex("""(?i)\b(?:info:\s*)?UPI/[A-Za-z0-9._\-]+/[A-Za-z0-9._\-]+/[A-Za-z0-9._\-@]+""", RegexOption.DOT_MATCHES_ALL), " ")
            // Remove full email-like VPA handles (e.g. swiggy@okhdfcbank, user@icici, name.123@axisbank, xyz@paytm)
            .replace(Regex("""\b[A-Za-z0-9._\-]+@[A-Za-z0-9._\-]+\b"""), " ")
            // Remove VPA phrases with handles
            .replace(Regex("""(?i)\b(?:to\s+vpa|from\s+vpa|towards\s+vpa|via\s+vpa|vpa\s*:?|upi\s*[/:]\s*vpa|upi\s+id\s*:?)\s*[:\s]*[A-Za-z0-9._\-@]+"""), " ")
            // Remove recipient transfer clauses
            .replace(Regex("""(?i)\b(?:transfer\s+to\s+upi|paid\s+to\s+upi|sent\s+to\s+upi|transferred\s+to\s+vpa)\s*[:\s]*[A-Za-z0-9._\-@]+"""), " ")
            // Remove UPI reference numbers and UTRs
            .replace(Regex("""(?i)\b(?:upi\s*ref|rrn|utr|txn\s*id|reference\s*no|ref\s*no|ref)\s*[:\s]*[A-Za-z0-9]+"""), " ")
            // Remove URLs
            .replace(Regex("""https?://\S+"""), " ")
            // Remove merchant prefixes like @UPI_BLINKIT or PHP*Zepto
            .replace(Regex("""(?i)@UPI_[A-Za-z0-9._\-]+"""), " ")
            .replace(Regex("""(?i)(?:PHP\*|PG\*|POS\*|ECOM\*|BIL\*|IN\*)[A-Za-z0-9._\-]+"""), " ")

        return clean
    }

    // Explicit User Account Regexes (Matches user's own bank account mention in body)
    private val USER_ACCOUNT_BANK_PATTERNS = listOf(
        // "in/from/to/into your HDFC Bank A/c XX7011"
        Regex("""(?i)\b(?:in|from|to|into)\s+(?:your\s+)?([A-Za-z\s]+?)\s+(?:Bank\s+)?(?:A/c|Account|Acct|Card|Credit Card|Savings)\s*(?:no\.?)?\s*(?:[X\*]+\d+|\b\d{3,4}\b)"""),
        // "HDFC Bank A/c XX7011 is debited/credited"
        Regex("""(?i)\b([A-Za-z\s]+?)\s+(?:Bank\s+)?(?:A/c|Account|Acct|Card|Credit Card)\s*(?:no\.?)?\s*(?:[X\*]+\d+|\b\d{3,4}\b)\s+(?:is\s+)?(?:debited|credited|spent|charged)"""),
        // "Available Bal in HDFC Bank A/c XX7011"
        Regex("""(?i)\b(?:bal|balance|limit)\s+in\s+([A-Za-z\s]+?)\s+(?:Bank\s+)?(?:A/c|Account|Acct|Card)\s*(?:no\.?)?\s*(?:[X\*]+\d+|\b\d{3,4}\b)"""),
        // "Dear SBI User" / "Dear HDFC Bank Customer"
        Regex("""(?i)\bDear\s+([A-Za-z\s]+?)\s+(?:Bank\s+)?(?:Customer|User|Cardmember)"""),
        // "debited from your HDFC Bank A/c"
        Regex("""(?i)\b(?:debited\s+from|credited\s+to)\s+(?:your\s+)?([A-Za-z\s]+?)\s+(?:Bank\s+)?(?:A/c|Account|Acct|Card)""")
    )

    // Ranked list of verified bank body patterns with strict word boundaries
    private val VERIFIED_BODY_BANK_PATTERNS = listOf(
        Pair(Regex("""(?i)\bHDFC\s+BANK\b|\bHDFC\b"""), BankInfo("HDFC Bank")),
        Pair(Regex("""(?i)\bSTATE\s+BANK\s+OF\s+INDIA\b|\bSBI\s+CARD\b"""), BankInfo("SBI Card", AccountType.CREDIT_CARD)),
        Pair(Regex("""(?i)\bSTATE\s+BANK\s+OF\s+INDIA\b|\bSBI\b|\bSBIN\b"""), BankInfo("SBI")),
        Pair(Regex("""(?i)\bICICI\s+BANK\b|\bICICI\b"""), BankInfo("ICICI Bank")),
        Pair(Regex("""(?i)\bAXIS\s+BANK\b|\bAXIS\b"""), BankInfo("Axis Bank")),
        Pair(Regex("""(?i)\bKOTAK\s+MAHINDRA\s+BANK\b|\bKOTAK\s+BANK\b|\bKOTAK\b"""), BankInfo("Kotak Mahindra Bank")),
        Pair(Regex("""(?i)\bYES\s+BANK\b|\bYES\s+BANK\s+CARD\b"""), BankInfo("Yes Bank")),
        Pair(Regex("""(?i)\bFEDERAL\s+BANK\b|\bFEDBNK\b"""), BankInfo("Federal Bank")),
        Pair(Regex("""(?i)\bFIMONEY\b|\bFI\s+MONEY\b"""), BankInfo("Federal Bank")),
        Pair(Regex("""(?i)\bJUPITER\b"""), BankInfo("Federal Bank")),
        Pair(Regex("""(?i)\bIDFC\s+FIRST\s+BANK\b|\bIDFC\s+BANK\b|\bIDFCFB\b"""), BankInfo("IDFC FIRST Bank")),
        Pair(Regex("""(?i)\bPUNJAB\s+NATIONAL\s+BANK\b|\bPNB\b"""), BankInfo("Punjab National Bank")),
        Pair(Regex("""(?i)\bBANK\s+OF\s+BARODA\b|\bBOB\b"""), BankInfo("Bank of Baroda")),
        Pair(Regex("""(?i)\bCANARA\s+BANK\b"""), BankInfo("Canara Bank")),
        Pair(Regex("""(?i)\bUNION\s+BANK\s+OF\s+INDIA\b|\bUNION\s+BANK\b"""), BankInfo("Union Bank of India")),
        Pair(Regex("""(?i)\bINDUSIND\s+BANK\b"""), BankInfo("IndusInd Bank")),
        Pair(Regex("""(?i)\bRBL\s+BANK\b"""), BankInfo("RBL Bank")),
        Pair(Regex("""(?i)\bINDIAN\s+BANK\b"""), BankInfo("Indian Bank")),
        Pair(Regex("""(?i)\bBANK\s+OF\s+INDIA\b"""), BankInfo("Bank of India")),
        Pair(Regex("""(?i)\bIDBI\s+BANK\b"""), BankInfo("IDBI Bank")),
        Pair(Regex("""(?i)\bCENTRAL\s+BANK\s+OF\s+INDIA\b"""), BankInfo("Central Bank of India")),
        Pair(Regex("""(?i)\bIPPB\b|\bINDIA\s+POST\s+PAYMENTS\s+BANK\b"""), BankInfo("IPPB")),
        Pair(Regex("""(?i)\bAU\s+SMALL\s+FINANCE\s+BANK\b|\bAU\s+BANK\b"""), BankInfo("AU Small Finance Bank")),
        Pair(Regex("""(?i)\bEQUITAS\s+SMALL\s+FINANCE\s+BANK\b|\bEQUITAS\b"""), BankInfo("Equitas Small Finance Bank")),
        Pair(Regex("""(?i)\bCITIBANK\b|\bCITI\b"""), BankInfo("CitiBank")),
        Pair(Regex("""(?i)\bAMERICAN\s+EXPRESS\b|\bAMEX\b"""), BankInfo("American Express", AccountType.CREDIT_CARD)),
        Pair(Regex("""(?i)\bHSBC\s+BANK\b|\bHSBC\b"""), BankInfo("HSBC")),
        Pair(Regex("""(?i)\bSTANDARD\s+CHARTERED\b"""), BankInfo("Standard Chartered Bank")),
        Pair(Regex("""(?i)\bCRED\b"""), BankInfo("CRED", AccountType.CREDIT_CARD)),
        Pair(Regex("""(?i)\bSLICE\b"""), BankInfo("Slice", AccountType.CREDIT_CARD)),
        Pair(Regex("""(?i)\bONECARD\b"""), BankInfo("OneCard", AccountType.CREDIT_CARD)),
        Pair(Regex("""(?i)\bKIWI\b"""), BankInfo("Kiwi", AccountType.CREDIT_CARD)),
        Pair(Regex("""(?i)\bPAYTM\s+PAYMENTS\s+BANK\b|\bPAYTM\s+WALLET\b"""), BankInfo("Paytm", AccountType.WALLET)),
        Pair(Regex("""(?i)\bAIRTEL\s+PAYMENTS\s+BANK\b"""), BankInfo("Airtel Payments Bank", AccountType.WALLET)),
        Pair(Regex("""(?i)\bGROWW\s+WALLET\b|\bGROWW\b"""), BankInfo("Groww Wallet", AccountType.WALLET)),
        Pair(Regex("""(?i)\bEPFO\b|\bPROVIDENT\s+FUND\b|\bPASSBOOK\s+BALANCE\b"""), BankInfo("EPFO PF Account", AccountType.SAVINGS))
    )

    fun identifyBank(sender: String, messageBody: String): BankInfo {
        val cleanSender = sender.uppercase().replace(Regex("[^A-Z]"), "")

        // Filter known non-bank noise senders
        if (NON_BANK_NOISE_SENDERS.any { cleanSender.contains(it) }) {
            return BankInfo("Bank Account", AccountType.BANK_ACCOUNT)
        }

        val isCard = messageBody.contains("Card", ignoreCase = true) ||
                messageBody.contains("Credit Card", ignoreCase = true) ||
                messageBody.contains("Limit", ignoreCase = true) ||
                messageBody.contains("Avl Lmt", ignoreCase = true) ||
                messageBody.contains("Credit Limit", ignoreCase = true)

        // 1. Direct Sender Lookup (for official bank header codes like HDFCBK, SBIN, AXISBK, etc.)
        for ((key, info) in BANK_SENDER_MAP) {
            // Ignore generic UPI / aggregator / credit card app senders in step 1 if the body contains a specific bank account statement
            if (key in setOf("PAYTM", "GPAY", "PHONPE", "PHONEPE", "JIO", "AIRTEL", "NOBRKR", "GKIWIP", "KIWI", "CREDIN", "CRED") && cleanSender.contains(key)) {
                // We'll check body first to see if an underlying bank account (e.g. HDFC, SBI, Axis, Yes Bank) is mentioned!
                continue
            }

            if (cleanSender.contains(key)) {
                return if (isCard && info.defaultType == AccountType.BANK_ACCOUNT) {
                    info.copy(defaultType = AccountType.CREDIT_CARD)
                } else info
            }
        }

        // 2. Sanitize Body: Strip all VPA handles, recipient emails, and UPI reference IDs
        val sanitizedBody = sanitizeBodyForBankIdentification(messageBody)

        // 3. User Account Context Pattern Matching in Sanitized Body
        for (pattern in USER_ACCOUNT_BANK_PATTERNS) {
            val match = pattern.find(sanitizedBody)
            if (match != null) {
                val candidate = match.groupValues[1].trim()
                for ((bankPattern, info) in VERIFIED_BODY_BANK_PATTERNS) {
                    if (bankPattern.containsMatchIn(candidate)) {
                        return if (isCard && info.defaultType == AccountType.BANK_ACCOUNT) {
                            info.copy(defaultType = AccountType.CREDIT_CARD)
                        } else info
                    }
                }
            }
        }

        // 4. General Sanitized Body Lookup with Word-Boundary Patterns
        for ((bankPattern, info) in VERIFIED_BODY_BANK_PATTERNS) {
            if (bankPattern.containsMatchIn(sanitizedBody)) {
                return if (isCard && info.defaultType == AccountType.BANK_ACCOUNT) {
                    info.copy(defaultType = AccountType.CREDIT_CARD)
                } else info
            }
        }

        // 5. Fallback for aggregator senders (Paytm, GPay, PhonePe, etc.) if no underlying bank found
        for ((key, info) in BANK_SENDER_MAP) {
            if (cleanSender.contains(key)) {
                return if (isCard && info.defaultType == AccountType.BANK_ACCOUNT) {
                    info.copy(defaultType = AccountType.CREDIT_CARD)
                } else info
            }
        }

        // 6. Final Fallback: Only return generic Bank Account if not recognized
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
        Pattern.compile("""(?:A/c|Acct|Account|Acc|A/C)\s*(?:no\.?|number)?\s*[*Xx-]*([0-9]{3,4})""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""(?:Card|Credit Card|Debit Card|CC)\s*(?:no\.?)?\s*[*Xx-]*([0-9]{4})""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""(?:Card\s*X([0-9]{4}))""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""(?:ending\s*(?:with|in)?\s*[*Xx-]*([0-9]{3,4}))""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""(?:UAN|Member\s*ID|PF\s*A/c)\s*[*Xx-]*([0-9]{4})""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""(?:[*Xx]{1,}[-]?[*Xx]*([0-9]{3,4}))""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""(?:SMS\s+BLKCC|SMS\s+BLOCK\s+CC|SMS\s+BLOCK)\s*([0-9]{4})""", Pattern.CASE_INSENSITIVE)
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
