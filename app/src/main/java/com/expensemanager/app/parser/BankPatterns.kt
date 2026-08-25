package com.expensemanager.app.parser

import com.expensemanager.app.core.model.AccountType
import java.util.regex.Pattern

object BankPatterns {

    data class BankInfo(
        val name: String,
        val defaultType: AccountType = AccountType.BANK_ACCOUNT
    )

    private val BANK_SENDER_MAP = mapOf(
        "HDFC" to BankInfo("HDFC Bank"),
        "YES" to BankInfo("Yes Bank"),
        "AXIS" to BankInfo("Axis Bank"),
        "SBI" to BankInfo("SBI"),
        "SBIN" to BankInfo("SBI"),
        "ICICI" to BankInfo("ICICI Bank"),
        "KOTAK" to BankInfo("Kotak Mahindra Bank"),
        "PAYTM" to BankInfo("Paytm", AccountType.WALLET),
        "PNB" to BankInfo("Punjab National Bank"),
        "BOB" to BankInfo("Bank of Baroda"),
        "INDUS" to BankInfo("IndusInd Bank"),
        "CANARA" to BankInfo("Canara Bank"),
        "UNION" to BankInfo("Union Bank of India"),
        "IDFC" to BankInfo("IDFC FIRST Bank"),
        "FEDERAL" to BankInfo("Federal Bank"),
        "RBL" to BankInfo("RBL Bank"),
        "CITI" to BankInfo("CitiBank"),
        "AMEX" to BankInfo("American Express", AccountType.CREDIT_CARD),
        "CRED" to BankInfo("CRED", AccountType.CREDIT_CARD),
        "JIO" to BankInfo("Jio", AccountType.WALLET),
        "AIRTEL" to BankInfo("Airtel Payments Bank", AccountType.WALLET),
        "GPAY" to BankInfo("Google Pay", AccountType.UPI),
        "PHONEPE" to BankInfo("PhonePe", AccountType.UPI),
        "NOBRKR" to BankInfo("NoBroker", AccountType.BANK_ACCOUNT),
        "GROWW" to BankInfo("Groww", AccountType.BANK_ACCOUNT),
        "SLIC" to BankInfo("Slice", AccountType.CREDIT_CARD),
        "ONECARD" to BankInfo("OneCard", AccountType.CREDIT_CARD)
    )

    fun identifyBank(sender: String, messageBody: String): BankInfo {
        val cleanSender = sender.uppercase().replace(Regex("[^A-Z]"), "")
        for ((key, info) in BANK_SENDER_MAP) {
            if (cleanSender.contains(key)) {
                val isCard = messageBody.contains("Card", ignoreCase = true) || messageBody.contains("Credit Card", ignoreCase = true)
                return if (isCard && info.defaultType == AccountType.BANK_ACCOUNT) {
                    info.copy(defaultType = AccountType.CREDIT_CARD)
                } else info
            }
        }
        val bodyUpper = messageBody.uppercase()
        for ((key, info) in BANK_SENDER_MAP) {
            if (bodyUpper.contains(info.name.uppercase()) || bodyUpper.contains(key)) {
                return info
            }
        }
        return BankInfo(
            name = if (cleanSender.length >= 4) cleanSender.takeLast(6) else "Bank Account",
            defaultType = AccountType.BANK_ACCOUNT
        )
    }

    // Amount extraction regexes
    val AMOUNT_PATTERNS = listOf(
        Pattern.compile("""(?:(?:Rs\.?|INR|₹|USD|\$)\s*([\d,]+(?:\.\d{1,2})?))""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""(?:(?:debited|credited|spent|paid|withdrawn|transferred|charge|refunded|Sent)\s+(?:by|for|of|with)?\s*(?:Rs\.?|INR|₹)?\s*([\d,]+(?:\.\d{1,2})?))""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""(?:amount\s*(?:of)?\s*(?:Rs\.?|INR|₹)?\s*([\d,]+(?:\.\d{1,2})?))""", Pattern.CASE_INSENSITIVE)
    )

    // Account / Card number patterns
    val ACCOUNT_PATTERNS = listOf(
        Pattern.compile("""(?:A/c|Acct|Account|Acc|A/C)\s*(?:no\.?|number)?\s*[*Xx]*([0-9]{3,4})""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""(?:Card|Credit Card|Debit Card)\s*(?:no\.?)?\s*[*Xx]*([0-9]{4})""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""(?:Card\s*X([0-9]{4}))""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""(?:ending\s*(?:with)?\s*[*Xx]*([0-9]{3,4}))""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""(?:[*Xx]{1,}([0-9]{3,4}))""", Pattern.CASE_INSENSITIVE)
    )

    // Available Balance patterns (Handles "Available Bal in HDFC Bank A/c XX7011 ... is INR 88,148.00")
    val BALANCE_PATTERNS = listOf(
        Pattern.compile("""(?:Avail(?:able)?\s*(?:Bal|Balance|Limit|Lmt)|Avl\s*(?:Bal|Lmt|Limit)|Bal|AVL\s*BAL|Net\s*Bal).*?(?:is\s*)?(?:Rs\.?|INR|₹)\s*([\d,]+(?:\.\d{1,2})?)""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""(?:Avail(?:able)?\s*(?:Bal|Balance|Limit|Lmt)|Avl\s*(?:Bal|Lmt|Limit)|Bal|AVL\s*BAL|Net\s*Bal)[:\s]*(?:is\s*)?(?:Rs\.?|INR|₹)?\s*([\d,]+(?:\.\d{1,2})?)""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""(?:Balance\s*(?:is|:)\s*(?:Rs\.?|INR|₹)?\s*([\d,]+(?:\.\d{1,2})?))""", Pattern.CASE_INSENSITIVE)
    )

    // Reference ID / UTR patterns
    val REFERENCE_PATTERNS = listOf(
        Pattern.compile("""(?:UPI(?:\s*Ref(?:\s*No)?)?|Ref(?:\s*No)?|Txn(?:\s*ID)?|Txn\s*Ref|UTR(?:\s*No)?|RRN|Ref)[:\s]*([a-zA-Z0-9]{6,24})""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""(?:UPI/(?:P2M|P2A)/([0-9]{6,24}))""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""(?:IMPS(?:\s*Ref(?:\s*No)?)?|NEFT|RTGS)[:\s]*([a-zA-Z0-9]{6,24})""", Pattern.CASE_INSENSITIVE)
    )

    // Merchant patterns
    val MERCHANT_PATTERNS = listOf(
        Pattern.compile("""@(?:UPI_)?([A-Za-z0-9\s&.\-_]+?)(?:\s+\d{2}-\d{2}-\d{4}|\s+Avl|\s+Lmt|\.|\s+SMS|$)""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""(?:PHP\*|PG\*|POS\*|ECOM\*|BIL\*|IN\*|VPA\s+)\s*([A-Za-z0-9\s&.\-_]+?)(?:\s+Avl|\s+Limit|\n|\r|$)""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""(?m)^To\s+([A-Za-z0-9\s&.\-_@]+?)(?:\s+On|\s+dated|\s+Ref|\n|\r|$)""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""(?:For|To)\s+([A-Za-z0-9\s&.\-_]+?)\s+mandate""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""(?:paid\s+to|sent\s+to|transferred\s+to|spent\s+at|at|to)\s+([A-Za-z0-9&_\-@]+(?:\s+[A-Za-z0-9&_\-@]+)*)""", Pattern.CASE_INSENSITIVE)
    )
}
