package com.expensemanager.app.parser

import com.expensemanager.app.core.model.AccountType
import java.util.regex.Pattern

object BankPatterns {

    data class BankInfo(
        val name: String,
        val defaultType: AccountType = AccountType.BANK_ACCOUNT
    )

    // Dedicated issuing bank & fintech registry
    private val BANK_SENDER_MAP = mapOf(
        // HDFC Bank
        "HDFCBK" to BankInfo("HDFC Bank"),
        "HDFCBN" to BankInfo("HDFC Bank"),
        "HDFCCC" to BankInfo("HDFC Bank", AccountType.CREDIT_CARD),
        "HDFC" to BankInfo("HDFC Bank"),
        // State Bank of India
        "SBIUPI" to BankInfo("SBI"),
        "SBMSMS" to BankInfo("SBI"),
        "SBIINB" to BankInfo("SBI"),
        "SBIN" to BankInfo("SBI"),
        "SBICRD" to BankInfo("SBI Card", AccountType.CREDIT_CARD),
        "SBI" to BankInfo("SBI"),
        // ICICI Bank
        "ICICIB" to BankInfo("ICICI Bank"),
        "ICICIN" to BankInfo("ICICI Bank"),
        "ICICIC" to BankInfo("ICICI Bank", AccountType.CREDIT_CARD),
        "ICICI" to BankInfo("ICICI Bank"),
        // Axis Bank
        "AXISBK" to BankInfo("Axis Bank"),
        "AXISMR" to BankInfo("Axis Bank"),
        "AXISMF" to BankInfo("Axis Bank"),
        "AXIS" to BankInfo("Axis Bank"),
        // Kotak Mahindra Bank
        "KOTAKB" to BankInfo("Kotak Mahindra Bank"),
        "KOTAKC" to BankInfo("Kotak Mahindra Bank", AccountType.CREDIT_CARD),
        "KOTAKN" to BankInfo("Kotak Mahindra Bank"),
        "KOTAK" to BankInfo("Kotak Mahindra Bank"),
        // Yes Bank
        "YESBNK" to BankInfo("Yes Bank"),
        "YESBAK" to BankInfo("Yes Bank"),
        "YESCRD" to BankInfo("Yes Bank", AccountType.CREDIT_CARD),
        "YES" to BankInfo("Yes Bank"),
        // Federal Bank & Neo banks
        "FEDBNK" to BankInfo("Federal Bank"),
        "FED" to BankInfo("Federal Bank"),
        "FEDERAL" to BankInfo("Federal Bank"),
        "FIMONEY" to BankInfo("Federal Bank"),
        "JUPITER" to BankInfo("Federal Bank"),
        // IDFC FIRST Bank
        "IDFCFB" to BankInfo("IDFC FIRST Bank"),
        "IDFCBN" to BankInfo("IDFC FIRST Bank"),
        "IDFC" to BankInfo("IDFC FIRST Bank"),
        // Punjab National Bank & BOB
        "PNBSMS" to BankInfo("Punjab National Bank"),
        "PNBBNK" to BankInfo("Punjab National Bank"),
        "PNB" to BankInfo("Punjab National Bank"),
        "BOBTXN" to BankInfo("Bank of Baroda"),
        "BOBSMS" to BankInfo("Bank of Baroda"),
        "BOB" to BankInfo("Bank of Baroda"),
        // Canara & Union Bank
        "CANBNK" to BankInfo("Canara Bank"),
        "CANARA" to BankInfo("Canara Bank"),
        "UBIN" to BankInfo("Union Bank of India"),
        "UNIONB" to BankInfo("Union Bank of India"),
        "UNION" to BankInfo("Union Bank of India"),
        // IndusInd & RBL
        "INDUS" to BankInfo("IndusInd Bank"),
        "RBLBNK" to BankInfo("RBL Bank"),
        "RBLCRD" to BankInfo("RBL Bank", AccountType.CREDIT_CARD),
        "RBL" to BankInfo("RBL Bank"),
        // Indian Bank & Bank of India
        "IDIBNK" to BankInfo("Indian Bank"),
        "INDIBK" to BankInfo("Indian Bank"),
        "INDBNK" to BankInfo("Indian Bank"),
        "INDBK" to BankInfo("Indian Bank"),
        "INDIAN" to BankInfo("Indian Bank"),
        "BOITXN" to BankInfo("Bank of India"),
        "BOISMS" to BankInfo("Bank of India"),
        "BOI" to BankInfo("Bank of India"),
        "IDBIBK" to BankInfo("IDBI Bank"),
        "IDBISM" to BankInfo("IDBI Bank"),
        "IDBI" to BankInfo("IDBI Bank"),
        "CBIN" to BankInfo("Central Bank of India"),
        "CENTBK" to BankInfo("Central Bank of India"),
        "CENTRAL" to BankInfo("Central Bank of India"),
        "IPPB" to BankInfo("IPPB"),
        "IPPBMS" to BankInfo("IPPB"),
        // Small Finance & Foreign Banks
        "AUBANK" to BankInfo("AU Small Finance Bank"),
        "AUFIN" to BankInfo("AU Small Finance Bank"),
        "EQUITAS" to BankInfo("Equitas Small Finance Bank"),
        "EQSFB" to BankInfo("Equitas Small Finance Bank"),
        "CITIBK" to BankInfo("CitiBank"),
        "CITI" to BankInfo("CitiBank"),
        "AMEX" to BankInfo("American Express", AccountType.CREDIT_CARD),
        "AMEXIN" to BankInfo("American Express", AccountType.CREDIT_CARD),
        "HSBCBK" to BankInfo("HSBC"),
        "HSBC" to BankInfo("HSBC"),
        "SCBL" to BankInfo("Standard Chartered Bank"),
        "SCB" to BankInfo("Standard Chartered Bank"),
        "BNDHN" to BankInfo("Bandhan Bank"),
        "BANDHAN" to BankInfo("Bandhan Bank"),
        "DBSBNK" to BankInfo("DBS Bank"),
        "DBS" to BankInfo("DBS Bank"),
        // Credit Cards & Neo Cards
        "CREDIN" to BankInfo("CRED", AccountType.CREDIT_CARD),
        "CRED" to BankInfo("CRED", AccountType.CREDIT_CARD),
        "SLIC" to BankInfo("Slice", AccountType.CREDIT_CARD),
        "SLICE" to BankInfo("Slice", AccountType.CREDIT_CARD),
        "ONECRD" to BankInfo("OneCard", AccountType.CREDIT_CARD),
        "ONECARD" to BankInfo("OneCard", AccountType.CREDIT_CARD),
        "1CARD" to BankInfo("OneCard", AccountType.CREDIT_CARD),
        "GKIWIP" to BankInfo("Kiwi", AccountType.CREDIT_CARD),
        "KIWI" to BankInfo("Kiwi", AccountType.CREDIT_CARD),
        // Wallets & UPI
        "PAYTM" to BankInfo("Paytm", AccountType.WALLET),
        "PYTM" to BankInfo("Paytm", AccountType.WALLET),
        "JIO" to BankInfo("Jio", AccountType.WALLET),
        "JIOPAY" to BankInfo("Jio", AccountType.WALLET),
        "AIRTEL" to BankInfo("Airtel Payments Bank", AccountType.WALLET),
        "AIRBNK" to BankInfo("Airtel Payments Bank", AccountType.WALLET),
        "GPAY" to BankInfo("Google Pay", AccountType.UPI),
        "PHONPE" to BankInfo("PhonePe", AccountType.UPI),
        "PHONEPE" to BankInfo("PhonePe", AccountType.UPI),
        "NOBRKR" to BankInfo("NoBroker", AccountType.BANK_ACCOUNT),
        "GROWWO" to BankInfo("Groww Wallet", AccountType.WALLET),
        "GROWW" to BankInfo("Groww Wallet", AccountType.WALLET),
        // EPFO & Loans
        "EPFO" to BankInfo("EPFO PF Account", AccountType.SAVINGS),
        "EPFOHO" to BankInfo("EPFO PF Account", AccountType.SAVINGS),
        "PFOHOS" to BankInfo("EPFO PF Account", AccountType.SAVINGS),
        "PFOHOG" to BankInfo("EPFO PF Account", AccountType.SAVINGS),
        "MUTFCL" to BankInfo("Muthoot Fincorp", AccountType.BANK_ACCOUNT),
        // Regional & Co-operative Banks
        "SARASW" to BankInfo("Saraswat Bank"),
        "SBCOBN" to BankInfo("Saraswat Bank"),
        "KARBNK" to BankInfo("Karnataka Bank"),
        "KTKBK" to BankInfo("Karnataka Bank"),
        "SIBANK" to BankInfo("South Indian Bank"),
        "SIBUS" to BankInfo("South Indian Bank"),
        "NAINIT" to BankInfo("Nainital Bank"),
        "TMBANK" to BankInfo("Tamilnad Mercantile Bank"),
        "TMBBNK" to BankInfo("Tamilnad Mercantile Bank"),
        "DCBANK" to BankInfo("DCB Bank"),
        "DCBBNK" to BankInfo("DCB Bank"),
        "JKBANK" to BankInfo("J&K Bank"),
        "JKBBNK" to BankInfo("J&K Bank"),
        "LAKSHM" to BankInfo("Lakshmi Vilas Bank"),
        "LVBNK" to BankInfo("Lakshmi Vilas Bank"),
        "UJJBNK" to BankInfo("Ujjivan Small Finance Bank"),
        "UJJSFB" to BankInfo("Ujjivan Small Finance Bank"),
        "ESAFBNK" to BankInfo("ESAF Small Finance Bank"),
        "SURYOD" to BankInfo("Suryoday Small Finance Bank"),
        "NORTBK" to BankInfo("North East Small Finance Bank")
    )

    // Known PSP / TPAP gateways where transactions often originate for other underlying bank accounts
    private val PSP_OR_AGGREGATOR_SENDERS = setOf(
        "YESBNK", "YESBAK", "YES", "YESCRD",
        "AXISBK", "AXISMR", "AXISMF", "AXIS",
        "ICICIB", "ICICIN", "ICICI",
        "SBIUPI", "SBMSMS", "SBIN", "SBI",
        "PAYTM", "PYTM", "GPAY", "PHONPE", "PHONEPE", "JIO", "AIRTEL",
        "NOBRKR", "GKIWIP", "KIWI", "CREDIN", "CRED", "SLICE", "ONECARD", "1CARD"
    )

    private val NON_BANK_NOISE_SENDERS = setOf(
        "SESMSS", "TTHPLS", "EDBUSS", "CAMZNS", "LNKITS", "EPTONS", "WIGGYS",
        "SELTDS", "DSLTXS", "SWIGGY", "ZEPTO", "BLINKIT", "ZOMATO", "MYNTRA",
        "FLIPKT", "AMAZON"
    )

    private val VERIFIED_INSTITUTIONS = setOf(
        "hdfc bank", "yes bank", "axis bank", "sbi", "sbi card", "icici bank", "kotak mahindra bank",
        "punjab national bank", "bank of baroda", "indusind bank", "canara bank",
        "union bank of india", "idfc first bank", "federal bank", "rbl bank", "citibank",
        "american express", "slice", "onecard", "kiwi", "indian bank",
        "bank of india", "idbi bank", "central bank of india", "ippb", "au small finance bank",
        "equitas small finance bank", "hsbc", "standard chartered bank", "paytm payments bank",
        "airtel payments bank", "bandhan bank", "dbs bank", "muthoot fincorp",
        // Regional banks
        "saraswat bank", "karnataka bank", "south indian bank", "nainital bank",
        "tamilnad mercantile bank", "dcb bank", "j&k bank", "lakshmi vilas bank",
        "ujjivan small finance bank", "esaf small finance bank", "suryoday small finance bank",
        "north east small finance bank"
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
            // Remove VPA phrases with handles, including "linked to VPA"
            .replace(Regex("""(?i)\b(?:linked\s+to\s+vpa|to\s+vpa|from\s+vpa|towards\s+vpa|via\s+vpa|vpa\s*:?|upi\s*[/:]\s*vpa|upi\s+id\s*:?)\s*[:\s]*[A-Za-z0-9._\-@]+"""), " ")
            // Remove recipient transfer clauses
            .replace(Regex("""(?i)\b(?:transfer\s+to\s+upi|paid\s+to\s+upi|sent\s+to\s+upi|transferred\s+to\s+vpa)\s*[:\s]*[A-Za-z0-9._\-@]+"""), " ")
            // Remove UPI reference numbers, RRNs and UTRs
            .replace(Regex("""(?i)\b(?:upi\s*ref(?:\s*no)?|rrn|utr|txn\s*id|reference\s*no|ref\s*no|ref)\s*[:\s]*[A-Za-z0-9]+"""), " ")
            // Remove URLs
            .replace(Regex("""https?://\S+"""), " ")
            // Remove merchant prefixes like @UPI_BLINKIT or PHP*Zepto
            .replace(Regex("""(?i)@UPI_[A-Za-z0-9._\-]+"""), " ")
            .replace(Regex("""(?i)(?:PHP\*|PG\*|POS\*|ECOM\*|BIL\*|IN\*)[A-Za-z0-9._\-]+"""), " ")

        return clean
    }

    // Explicit User Account Regexes (Matches user's own bank account mention in body)
    private val USER_ACCOUNT_BANK_PATTERNS = listOf(
        // "in/from/into your HDFC Bank A/c XX7011"
        Regex("""(?i)\b(?:in|from|into)\s+(?:your\s+)?([A-Za-z\s]+?)\s+(?:Bank\s+)?(?:A/c|Account|Acct|Card|Credit Card|Savings)\s*(?:no\.?)?\s*(?:[X\*]+\d+|\b\d{3,4}\b)"""),
        // "debited from / credited to your HDFC Bank A/c XX7011"
        Regex("""(?i)\b(?:debited\s+from|credited\s+to|spent\s+on|charged\s+to)\s+(?:your\s+)?([A-Za-z\s]+?)\s+(?:Bank\s+)?(?:A/c|Account|Acct|Card|Credit Card|Savings)\s*(?:no\.?)?\s*(?:[X\*]+\d+|\b\d{3,4}\b)"""),
        // "HDFC Bank A/c XX7011 is debited/credited/spent"
        Regex("""(?i)\b([A-Za-z\s]+?)\s+(?:Bank\s+)?(?:A/c|Account|Acct|Card|Credit Card)\s*(?:no\.?)?\s*(?:[X\*]+\d+|\b\d{3,4}\b)\s+(?:is|has\s+been|was)?\s*(?:debited|credited|spent|charged)"""),
        // "Available Bal in HDFC Bank A/c XX7011"
        Regex("""(?i)\b(?:bal|balance|limit|avl\s+bal|avail\s+bal)\s+in\s+([A-Za-z\s]+?)\s+(?:Bank\s+)?(?:A/c|Account|Acct|Card)\s*(?:no\.?)?\s*(?:[X\*]+\d+|\b\d{3,4}\b)"""),
        // "Dear SBI User" / "Dear HDFC Bank Customer"
        Regex("""(?i)\bDear\s+([A-Za-z\s]+?)\s+(?:Bank\s+)?(?:Customer|User|Cardmember|Member)"""),
        // "your HDFC Bank A/c XX7011"
        Regex("""(?i)\byour\s+([A-Za-z\s]+?)\s+(?:Bank\s+)?(?:A/c|Account|Acct|Card|Credit Card)\s*(?:no\.?)?\s*(?:[X\*]+\d+|\b\d{3,4}\b)"""),
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
        Pair(Regex("""(?i)\bBANDHAN\s+BANK\b|\bBANDHAN\b"""), BankInfo("Bandhan Bank")),
        Pair(Regex("""(?i)\bDBS\s+BANK\b|\bDBS\b"""), BankInfo("DBS Bank")),
        Pair(Regex("""(?i)\bCRED\b"""), BankInfo("CRED", AccountType.CREDIT_CARD)),
        Pair(Regex("""(?i)\bSLICE\b"""), BankInfo("Slice", AccountType.CREDIT_CARD)),
        Pair(Regex("""(?i)\bONECARD\b"""), BankInfo("OneCard", AccountType.CREDIT_CARD)),
        Pair(Regex("""(?i)\bKIWI\b"""), BankInfo("Kiwi", AccountType.CREDIT_CARD)),
        Pair(Regex("""(?i)\bPAYTM\s+PAYMENTS\s+BANK\b|\bPAYTM\s+WALLET\b"""), BankInfo("Paytm", AccountType.WALLET)),
        Pair(Regex("""(?i)\bAIRTEL\s+PAYMENTS\s+BANK\b"""), BankInfo("Airtel Payments Bank", AccountType.WALLET)),
        Pair(Regex("""(?i)\bGROWW\s+WALLET\b|\bGROWW\b"""), BankInfo("Groww Wallet", AccountType.WALLET)),
        Pair(Regex("""(?i)\bEPFO\b|\bPROVIDENT\s+FUND\b|\bPASSBOOK\s+BALANCE\b"""), BankInfo("EPFO PF Account", AccountType.SAVINGS)),
        // Regional banks
        Pair(Regex("""(?i)\bSARASWAT\s+BANK\b|\bSARASWAT\s+CO-?OP\b"""), BankInfo("Saraswat Bank")),
        Pair(Regex("""(?i)\bKARNATAKA\s+BANK\b"""), BankInfo("Karnataka Bank")),
        Pair(Regex("""(?i)\bSOUTH\s+INDIAN\s+BANK\b"""), BankInfo("South Indian Bank")),
        Pair(Regex("""(?i)\bNAINITAL\s+BANK\b"""), BankInfo("Nainital Bank")),
        Pair(Regex("""(?i)\bTAMILNAD\s+MERCANTILE\s+BANK\b|\bTMB\b"""), BankInfo("Tamilnad Mercantile Bank")),
        Pair(Regex("""(?i)\bDCB\s+BANK\b"""), BankInfo("DCB Bank")),
        Pair(Regex("""(?i)\bJ&K\s+BANK\b|\bJAMMU\s+AND\s+KASHMIR\s+BANK\b"""), BankInfo("J&K Bank")),
        Pair(Regex("""(?i)\bLAKSHMI\s+VILAS\s+BANK\b|\bLVB\b"""), BankInfo("Lakshmi Vilas Bank")),
        Pair(Regex("""(?i)\bUJJIVAN\s+SMALL\s+FINANCE\b|\bUJJIVAN\b"""), BankInfo("Ujjivan Small Finance Bank")),
        Pair(Regex("""(?i)\bESAF\s+SMALL\s+FINANCE\b|\bESAF\s+BANK\b"""), BankInfo("ESAF Small Finance Bank")),
        Pair(Regex("""(?i)\bSURYODAY\s+SMALL\s+FINANCE\b|\bSURYODAY\b"""), BankInfo("Suryoday Small Finance Bank"))
    )

    private fun extractSenderEntity(sender: String): String {
        val upper = sender.uppercase().trim()
        if (upper.contains("-")) {
            val parts = upper.split("-")
            // In TRAI format XX-ENTITY-T or XX-ENTITY, entity is typically at index 1
            if (parts.size >= 2 && parts[1].length >= 3) {
                return parts[1].replace(Regex("[^A-Z0-9]"), "")
            }
        }
        return upper.replace(Regex("[^A-Z0-9]"), "")
    }

    /**
     * Hierarchical Bank Identification Engine:
     * 1. Sanitize body to strip beneficiary VPAs and UTRs.
     * 2. Highest Priority: Explicit User Account Bank Mention in Body (always overrides PSP routing senders).
     * 3. Second Priority: Dedicated Issuing Bank TRAI Sender Header (when sender is not a PSP gateway).
     * 4. Third Priority: PSP / Gateway Deep-Scan in Sanitized Body.
     * 5. Fourth Priority: Direct PSP / Aggregator Sender Lookup.
     * 6. Final Fallback: Generic Bank Account.
     */
    fun identifyBank(sender: String, messageBody: String): BankInfo {
        val cleanSender = extractSenderEntity(sender)

        // Filter known non-bank noise senders
        if (NON_BANK_NOISE_SENDERS.any { cleanSender.contains(it) }) {
            return BankInfo("Bank Account", AccountType.BANK_ACCOUNT)
        }

        val isCard = messageBody.contains("Card", ignoreCase = true) ||
                messageBody.contains("Credit Card", ignoreCase = true) ||
                messageBody.contains("Limit", ignoreCase = true) ||
                messageBody.contains("Avl Lmt", ignoreCase = true) ||
                messageBody.contains("Credit Limit", ignoreCase = true)

        // 1. Sanitize Body: Strip all VPA handles, recipient emails, and UPI reference IDs
        val sanitizedBody = sanitizeBodyForBankIdentification(messageBody)

        // 2. HIGHEST PRIORITY: Explicit User Account Bank Mention in Sanitized Body
        // E.g. "your HDFC Bank A/c XX7011", "Dear SBI User, your A/c XX1234", "spent on ICICI Bank Card XX1044"
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

        // 3. SECOND PRIORITY: Dedicated Issuing Bank TRAI Sender Header (Non-PSP)
        val isPspSender = PSP_OR_AGGREGATOR_SENDERS.any { cleanSender.contains(it) }
        if (!isPspSender) {
            for ((key, info) in BANK_SENDER_MAP) {
                if (cleanSender.contains(key)) {
                    return if (isCard && info.defaultType == AccountType.BANK_ACCOUNT) {
                        info.copy(defaultType = AccountType.CREDIT_CARD)
                    } else info
                }
            }
        }

        // 4. THIRD PRIORITY: PSP / Aggregator Deep-Scan in Sanitized Body
        for ((bankPattern, info) in VERIFIED_BODY_BANK_PATTERNS) {
            if (bankPattern.containsMatchIn(sanitizedBody)) {
                return if (isCard && info.defaultType == AccountType.BANK_ACCOUNT) {
                    info.copy(defaultType = AccountType.CREDIT_CARD)
                } else info
            }
        }

        // 5. FOURTH PRIORITY: Direct PSP / Aggregator Sender Lookup
        for ((key, info) in BANK_SENDER_MAP) {
            if (cleanSender.contains(key)) {
                return if (isCard && info.defaultType == AccountType.BANK_ACCOUNT) {
                    info.copy(defaultType = AccountType.CREDIT_CARD)
                } else info
            }
        }

        // 6. FINAL FALLBACK: Return generic Bank Account
        val bankName = if (cleanSender.length >= 4 && cleanSender !in NON_BANK_NOISE_SENDERS && !cleanSender.matches(Regex("""^[A-Z0-9]{6}$"""))) cleanSender.takeLast(6) else "Bank Account"
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

    // ──────────────────────────────────────────────────────────────────────────────────
    // BANK BALANCE patterns — deposit-account "available balance" ONLY.
    //
    // IMPORTANT: These deliberately DO NOT match credit-card "Available Limit / Avl Lmt",
    // because a card's available limit is spending headroom, NOT money the user owns.
    // Storing a limit as a positive balance is the #1 cause of wrong balances/totals.
    //
    // Matches: "Available Bal", "Avl Bal", "Avbl Bal", "A/c Balance", "Net Bal",
    //          "passbook balance", "Bal:" — but NOT "Limit" / "Lmt".
    // ──────────────────────────────────────────────────────────────────────────────────
    val BALANCE_PATTERNS = listOf(
        // "Available Bal in HDFC Bank A/c XX7011 as on yesterday:24-AUG-26 is INR 88,148.00"
        // The window between the balance keyword and the currency+amount can contain the
        // account label and a date (with its own colon), so we allow up to 60 non-digit-ish
        // chars but crucially DO NOT allow the word "Limit"/"Lmt" in between (negative lookahead)
        // so a card's available-limit line never satisfies a bank-balance pattern.
        Pattern.compile("""(?:Avail(?:able)?\s*Bal(?:ance)?|Avbl\s*Bal|Avl\s*Bal|A/c\s*Bal(?:ance)?|Acct\s*Bal(?:ance)?|Net\s*Bal(?:ance)?|passbook\s*balance)(?:(?!\bL(?:i)?m(?:i)?t\b).){0,60}?(?:Rs\.?|INR|₹)\s*([\d,]+(?:\.\d{1,2})?)""", Pattern.CASE_INSENSITIVE),
        // "Bal: Rs 88,148.00" / "Balance is Rs 88148"
        Pattern.compile("""(?:^|\s)(?:Bal|Balance)[:\s]+(?:is\s*)?(?:Rs\.?|INR|₹)\s*([\d,]+(?:\.\d{1,2})?)""", Pattern.CASE_INSENSITIVE),
        // "Balance is 88148.00" (currency optional, but keyword must be a standalone balance word)
        Pattern.compile("""(?:Balance\s*(?:is|:)\s*(?:Rs\.?|INR|₹)?\s*([\d,]+(?:\.\d{1,2})?))""", Pattern.CASE_INSENSITIVE)
    )

    // ──────────────────────────────────────────────────────────────────────────────────
    // CREDIT-CARD AVAILABLE LIMIT patterns — spending headroom on a credit card.
    // This is a DIFFERENT quantity from a bank balance and must be stored separately.
    // Matches: "Avl Lmt", "Available Limit", "Avl Limit", "Credit Limit avlbl".
    // ──────────────────────────────────────────────────────────────────────────────────
    val CREDIT_LIMIT_PATTERNS = listOf(
        Pattern.compile("""(?:Avail(?:able)?\s*(?:Credit\s*)?Limit|Avl\s*Lmt|Avbl\s*Lmt|Avl\s*Limit|Available\s*Lmt)[:\s]*(?:is\s*)?(?:Rs\.?|INR|₹)?\s*([\d,]+(?:\.\d{1,2})?)""", Pattern.CASE_INSENSITIVE)
    )

    // ──────────────────────────────────────────────────────────────────────────────────
    // CREDIT-CARD OUTSTANDING / AMOUNT DUE patterns — money the user OWES on the card.
    // Stored as a positive "outstanding" value (represents debt, shown separately).
    // Matches: "Outstanding", "Total Amt Due", "Total Amount Due", "Total Due".
    // ──────────────────────────────────────────────────────────────────────────────────
    val OUTSTANDING_PATTERNS = listOf(
        Pattern.compile("""(?:Total\s*(?:Amt|Amount)\s*Due|Total\s*Due|Outstanding(?:\s*(?:Amt|Amount|Bal(?:ance)?))?|Amt\s*Due|Statement\s*Bal(?:ance)?)[:\s]*(?:is\s*)?(?:Rs\.?|INR|₹)\s*([\d,]+(?:\.\d{1,2})?)""", Pattern.CASE_INSENSITIVE)
    )

    // Reference ID / UTR patterns
    val REFERENCE_PATTERNS = listOf(
        Pattern.compile("""(?:UPI(?:\s*Ref(?:\s*No)?)?|Ref(?:\s*No)?|Txn(?:\s*ID)?|Txn\s*Ref|UTR(?:\s*No)?|RRN|Ref)[:\s#]*([a-zA-Z0-9]{6,24})""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""(?:UPI/(?:P2M|P2A)/([0-9]{6,24}))""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""(?:IMPS|NEFT|RTGS)\s*[-:]\s*[A-Za-z0-9\s&.\-_]+?\s*-\s*([0-9]{6,24})""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""(?:IMPS(?:\s*Ref(?:\s*No)?)?|NEFT|RTGS)[:\s]*([a-zA-Z0-9]{6,24})""", Pattern.CASE_INSENSITIVE)
    )

    // ──────────────────────────────────────────────────────────────────────────────────
    // MCC (Merchant Category Code) — 4-digit code embedded in some credit-card SMS bodies.
    // Card networks (Visa, Mastercard, RuPay) assign MCCs to classify merchant business type.
    // When present, this gives ground-truth category signal that needs no ML.
    //
    // Common patterns in Indian CC SMS:
    //   "MCC: 5812"  /  "MCC 5411"  /  "Merchant Code: 5812"  /  "mcc5812"
    // ──────────────────────────────────────────────────────────────────────────────────
    val MCC_PATTERN: java.util.regex.Pattern = java.util.regex.Pattern.compile(
        """(?:MCC|Merchant\s+Category\s+Code|Merchant\s+Code)[:\s#]*([0-9]{4})""",
        java.util.regex.Pattern.CASE_INSENSITIVE
    )

    /**
     * Extracts a 4-digit MCC from an SMS body, or returns null if none is present.
     */
    fun extractMcc(body: String): String? {
        val m = MCC_PATTERN.matcher(body)
        return if (m.find()) m.group(1)?.trim() else null
    }

    /**
     * Maps a 4-digit MCC code string to an expense Category.
     *
     * Source: ISO 18245 / Visa/Mastercard MCC guide, filtered to categories
     * supported by this app.  Only well-defined, unambiguous MCCs are mapped;
     * everything else returns null (falls through to keyword/ML classification).
     *
     * Reference: https://business.phonepe.com/articles/what-is-an-mcc-code-complete-guide-to-merchant-category-codes-in-india
     */
    fun mccToCategory(mcc: String?): com.expensemanager.app.core.model.Category? {
        if (mcc == null) return null
        return when (mcc) {
            // ── Food & Dining ────────────────────────────────────────────────────
            "5811" -> com.expensemanager.app.core.model.Category.FOOD  // Caterers
            "5812" -> com.expensemanager.app.core.model.Category.FOOD  // Eating Places, Restaurants
            "5813" -> com.expensemanager.app.core.model.Category.FOOD  // Drinking Places, Bars
            "5814" -> com.expensemanager.app.core.model.Category.FOOD  // Fast Food Restaurants
            "5441" -> com.expensemanager.app.core.model.Category.FOOD  // Candy, Nut, Confectionery Stores
            "5451" -> com.expensemanager.app.core.model.Category.FOOD  // Dairy Products Stores
            "5462" -> com.expensemanager.app.core.model.Category.FOOD  // Bakeries
            "5499" -> com.expensemanager.app.core.model.Category.FOOD  // Miscellaneous Food Stores
            // ── Groceries ───────────────────────────────────────────────────────
            "5411" -> com.expensemanager.app.core.model.Category.GROCERIES  // Grocery Stores, Supermarkets
            "5422" -> com.expensemanager.app.core.model.Category.GROCERIES  // Freezer/Meat Lockers
            // NOTE: 5441 Candy/Confectionery kept in FOOD above — removed duplicate here
            // NOTE: 5912 Drug Stores kept in HEALTHCARE below — removed from GROCERIES
            // ── Shopping / Retail ────────────────────────────────────────────────
            "5045" -> com.expensemanager.app.core.model.Category.SHOPPING  // Computers & Peripherals
            "5065" -> com.expensemanager.app.core.model.Category.SHOPPING  // Electrical Parts/Equipment
            "5200" -> com.expensemanager.app.core.model.Category.SHOPPING  // Home Supply/Hardware
            "5251" -> com.expensemanager.app.core.model.Category.SHOPPING  // Hardware Stores
            "5311" -> com.expensemanager.app.core.model.Category.SHOPPING  // Department Stores
            "5331" -> com.expensemanager.app.core.model.Category.SHOPPING  // Variety Stores
            "5399" -> com.expensemanager.app.core.model.Category.SHOPPING  // Misc General Merchandise
            "5600" -> com.expensemanager.app.core.model.Category.SHOPPING  // Apparel/Accessory Stores
            "5611" -> com.expensemanager.app.core.model.Category.SHOPPING  // Men's Clothing
            "5621" -> com.expensemanager.app.core.model.Category.SHOPPING  // Women's Clothing
            "5631" -> com.expensemanager.app.core.model.Category.SHOPPING  // Women's Accessories
            "5641" -> com.expensemanager.app.core.model.Category.SHOPPING  // Children's Clothing
            "5651" -> com.expensemanager.app.core.model.Category.SHOPPING  // Family Clothing
            "5661" -> com.expensemanager.app.core.model.Category.SHOPPING  // Shoe Stores
            "5691" -> com.expensemanager.app.core.model.Category.SHOPPING  // Men's/Women's Clothing
            "5699" -> com.expensemanager.app.core.model.Category.SHOPPING  // Misc Apparel
            "5719" -> com.expensemanager.app.core.model.Category.SHOPPING  // Misc Home Furnishings
            "5722" -> com.expensemanager.app.core.model.Category.SHOPPING  // Household Appliance Stores
            "5732" -> com.expensemanager.app.core.model.Category.SHOPPING  // Electronics Stores
            "5734" -> com.expensemanager.app.core.model.Category.SHOPPING  // Computer Software Stores
            "5735" -> com.expensemanager.app.core.model.Category.SHOPPING  // Music Stores
            "5945" -> com.expensemanager.app.core.model.Category.SHOPPING  // Hobby/Toy/Game Shops
            "5947" -> com.expensemanager.app.core.model.Category.SHOPPING  // Gift/Card/Novelty/Souvenir
            "5999" -> com.expensemanager.app.core.model.Category.SHOPPING  // Misc Retail Stores
            // ── Transport & Travel ───────────────────────────────────────────────
            "4111" -> com.expensemanager.app.core.model.Category.TRANSPORT  // Local/Suburban Commuter Transport
            "4112" -> com.expensemanager.app.core.model.Category.TRANSPORT  // Passenger Railways
            "4121" -> com.expensemanager.app.core.model.Category.TRANSPORT  // Taxicabs/Limousines
            "4131" -> com.expensemanager.app.core.model.Category.TRANSPORT  // Bus Lines
            "4215" -> com.expensemanager.app.core.model.Category.TRANSPORT  // Courier Services
            "4411" -> com.expensemanager.app.core.model.Category.TRANSPORT  // Cruise Lines
            "4511" -> com.expensemanager.app.core.model.Category.TRANSPORT  // Airlines, Air Carriers
            "4582" -> com.expensemanager.app.core.model.Category.TRANSPORT  // Airports, Flying Fields
            "4722" -> com.expensemanager.app.core.model.Category.TRANSPORT  // Travel Agencies
            "4784" -> com.expensemanager.app.core.model.Category.TRANSPORT  // Tolls, Bridge Fees
            "5171" -> com.expensemanager.app.core.model.Category.TRANSPORT  // Petroleum Products
            "5172" -> com.expensemanager.app.core.model.Category.TRANSPORT  // Petroleum & Petroleum Products
            "5541" -> com.expensemanager.app.core.model.Category.TRANSPORT  // Service Stations
            "5542" -> com.expensemanager.app.core.model.Category.TRANSPORT  // Automated Fuel Dispensers
            "7011" -> com.expensemanager.app.core.model.Category.TRANSPORT  // Hotels & Motels
            "7512" -> com.expensemanager.app.core.model.Category.TRANSPORT  // Car Rental Agencies
            "7523" -> com.expensemanager.app.core.model.Category.TRANSPORT  // Parking Lots/Garages
            "7531" -> com.expensemanager.app.core.model.Category.TRANSPORT  // Auto Body Repair
            "7549" -> com.expensemanager.app.core.model.Category.TRANSPORT  // Towing Services
            // ── Bills & Utilities ────────────────────────────────────────────────
            "4812" -> com.expensemanager.app.core.model.Category.BILLS_UTILITIES  // Telephone Services
            "4813" -> com.expensemanager.app.core.model.Category.BILLS_UTILITIES  // Telephone Services (non-fax)
            "4814" -> com.expensemanager.app.core.model.Category.BILLS_UTILITIES  // Fax / Telecom
            "4816" -> com.expensemanager.app.core.model.Category.BILLS_UTILITIES  // Computer Network Services
            "4899" -> com.expensemanager.app.core.model.Category.BILLS_UTILITIES  // Cable/Satellite/Other Pay TV
            "4900" -> com.expensemanager.app.core.model.Category.BILLS_UTILITIES  // Utilities
            "4911" -> com.expensemanager.app.core.model.Category.BILLS_UTILITIES  // Electric, Gas, Sanitary Water
            "4941" -> com.expensemanager.app.core.model.Category.BILLS_UTILITIES  // Water Supply
            "6300" -> com.expensemanager.app.core.model.Category.BILLS_UTILITIES  // Insurance (general)
            // ── Entertainment ────────────────────────────────────────────────────
            "5815" -> com.expensemanager.app.core.model.Category.ENTERTAINMENT  // Digital Goods – Books, Movies, Music
            "5816" -> com.expensemanager.app.core.model.Category.ENTERTAINMENT  // Digital Goods – Games
            "5817" -> com.expensemanager.app.core.model.Category.ENTERTAINMENT  // Digital Goods – Applications
            "5818" -> com.expensemanager.app.core.model.Category.ENTERTAINMENT  // Digital Goods – Multicat
            // NOTE: 7011 Hotels — kept in TRANSPORT above (hotel stays = travel expense)
            "7832" -> com.expensemanager.app.core.model.Category.ENTERTAINMENT  // Motion Picture Theaters
            "7841" -> com.expensemanager.app.core.model.Category.ENTERTAINMENT  // Video Tape Rental
            "7922" -> com.expensemanager.app.core.model.Category.ENTERTAINMENT  // Ticket Agencies, Theatrical Producers
            "7929" -> com.expensemanager.app.core.model.Category.ENTERTAINMENT  // Bands, Orchestras, Entertainers
            "7993" -> com.expensemanager.app.core.model.Category.ENTERTAINMENT  // Video Game Arcades, Establishments
            "7994" -> com.expensemanager.app.core.model.Category.ENTERTAINMENT  // Video Game Supply Stores
            "7999" -> com.expensemanager.app.core.model.Category.ENTERTAINMENT  // Recreation Services
            // ── Healthcare ───────────────────────────────────────────────────────
            "5047" -> com.expensemanager.app.core.model.Category.HEALTHCARE  // Medical & Dental Equipment
            "5122" -> com.expensemanager.app.core.model.Category.HEALTHCARE  // Drugs, Drug Proprietaries
            "5912" -> com.expensemanager.app.core.model.Category.HEALTHCARE  // Drug Stores, Pharmacies
            "8011" -> com.expensemanager.app.core.model.Category.HEALTHCARE  // Doctors & Physicians
            "8021" -> com.expensemanager.app.core.model.Category.HEALTHCARE  // Dentists & Orthodontists
            "8031" -> com.expensemanager.app.core.model.Category.HEALTHCARE  // Osteopaths
            "8041" -> com.expensemanager.app.core.model.Category.HEALTHCARE  // Chiropractors
            "8049" -> com.expensemanager.app.core.model.Category.HEALTHCARE  // Podiatrists
            "8050" -> com.expensemanager.app.core.model.Category.HEALTHCARE  // Nursing & Personal Care
            "8062" -> com.expensemanager.app.core.model.Category.HEALTHCARE  // Hospitals
            "8071" -> com.expensemanager.app.core.model.Category.HEALTHCARE  // Medical & Dental Labs
            "8099" -> com.expensemanager.app.core.model.Category.HEALTHCARE  // Health Practitioners (not elsewhere)
            // ── Investment & Insurance ────────────────────────────────────────────
            "6211" -> com.expensemanager.app.core.model.Category.INVESTMENT  // Security Brokers/Dealers
            "6282" -> com.expensemanager.app.core.model.Category.INVESTMENT  // Investment Advisory Services
            "6311" -> com.expensemanager.app.core.model.Category.INVESTMENT  // Life Insurance
            "6321" -> com.expensemanager.app.core.model.Category.INVESTMENT  // Accident/Health Insurance
            "6331" -> com.expensemanager.app.core.model.Category.INVESTMENT  // Fire/Marine/Casualty Insurance
            "6381" -> com.expensemanager.app.core.model.Category.INVESTMENT  // Insurance Premiums
            // ── Education ────────────────────────────────────────────────────────
            "8211" -> com.expensemanager.app.core.model.Category.EDUCATION  // Elementary/Secondary Schools
            "8220" -> com.expensemanager.app.core.model.Category.EDUCATION  // Colleges, Universities
            "8241" -> com.expensemanager.app.core.model.Category.EDUCATION  // Correspondence Schools
            "8244" -> com.expensemanager.app.core.model.Category.EDUCATION  // Business & Secretarial Schools
            "8249" -> com.expensemanager.app.core.model.Category.EDUCATION  // Vocational & Trade Schools
            "8299" -> com.expensemanager.app.core.model.Category.EDUCATION  // Educational Services (not elsewhere)
            // ── Personal Care ────────────────────────────────────────────────────
            // NOTE: 7011 Hotels — kept in TRANSPORT; removed duplicate here
            "7230" -> com.expensemanager.app.core.model.Category.PERSONAL  // Beauty/Barber Shops
            "7297" -> com.expensemanager.app.core.model.Category.PERSONAL  // Massage Parlors
            "7298" -> com.expensemanager.app.core.model.Category.PERSONAL  // Health & Beauty Spas
            "7911" -> com.expensemanager.app.core.model.Category.PERSONAL  // Dance Studios
            "7941" -> com.expensemanager.app.core.model.Category.PERSONAL  // Sports Clubs/Athletic Fields
            "7991" -> com.expensemanager.app.core.model.Category.PERSONAL  // Tourist Attractions & Exhibits
            "7997" -> com.expensemanager.app.core.model.Category.PERSONAL  // Clubs — Country, Golf, Athletic
            // ── Fees & Charges ────────────────────────────────────────────────────
            "6012" -> com.expensemanager.app.core.model.Category.FEES_CHARGES  // Financial Institutions — Merchandise
            "6051" -> com.expensemanager.app.core.model.Category.FEES_CHARGES  // Non-Financial Institutions — Forex
            "6099" -> com.expensemanager.app.core.model.Category.FEES_CHARGES  // Financial Institutions — not elsewhere
            "6532" -> com.expensemanager.app.core.model.Category.FEES_CHARGES  // Payment Transaction / Money Transfer
            // ── Transfers / Wallets ───────────────────────────────────────────────
            "4829" -> com.expensemanager.app.core.model.Category.TRANSFERS  // Money Orders
            "6010" -> com.expensemanager.app.core.model.Category.TRANSFERS  // Financial Institutions — Manual Cash
            "6011" -> com.expensemanager.app.core.model.Category.TRANSFERS  // Financial Institutions — ATM
            "6540" -> com.expensemanager.app.core.model.Category.TRANSFERS  // POI (Funding Transactions)
            else -> null
        }
    }

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
