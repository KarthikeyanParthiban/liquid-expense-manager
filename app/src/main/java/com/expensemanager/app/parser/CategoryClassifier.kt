package com.expensemanager.app.parser

import com.expensemanager.app.core.model.Category
import com.expensemanager.app.core.model.MerchantRule
import com.expensemanager.app.core.model.TransactionType

object CategoryClassifier {

    private val KEYWORD_CATEGORY_MAP = mapOf(
        Category.FOOD to listOf(
            "zomato", "swiggy", "dominos", "mcdonald", "kfc", "starbucks", "burger king",
            "pizza hut", "subway", "chaayos", "chai point", "haldiram", "cafe", "restaurant",
            "barbeque", "biryani", "food", "dining", "eatery", "bakery", "cake", "dhaba",
            "tiffin", "mess", "bistro", "pastry", "coffee", "eats", "olympic", "buhari",
            "kaapi", "chat", "mithaai", "sangeetha", "hotel", "sweets", "juice", "kitchen",
            "canteen", "darshini", "bakes", "tandoori", "snacks", "tea"
        ),
        Category.GROCERIES to listOf(
            "zepto", "blinkit", "instamart", "bigbasket", "dmart", "jiomart", "natures basket",
            "milkbasket", "grofers", "supermarket", "super marke", "provision", "spencers", "more retail",
            "fresh", "vegetables", "dairy", "fruits", "mart", "hypermarket", "kirana", "ration",
            "kpn", "farm fresh", "vegetable", "organic", "store", "general store"
        ),
        Category.SHOPPING to listOf(
            "amazon", "flipkart", "myntra", "ajio", "nykaa", "zara", "h&m", "meesho",
            "tata cliq", "decathlon", "croma", "reliance digital", "vijay sales", "lenskart",
            "uniqlo", "shoppers stop", "westside", "max fashion", "clothing", "apparel",
            "electronics", "footwear", "mall", "retail", "furniture", "ever shine"
        ),
        Category.TRANSPORT to listOf(
            "uber", "ola", "rapido", "metro", "irctc", "makemytrip", "cleartrip", "yatra",
            "indigo", "air india", "fastag", "toll", "fuel", "petrol", "indian oil",
            "bharat petroleum", "shell", "hpcl", "iocl", "bpcl", "fuel station", "cab",
            "flight", "airline", "railway", "diesel", "parking", "auto", "redbus", "redrail",
            "automobile", "balakrishna"
        ),
        Category.BILLS_UTILITIES to listOf(
            "airtel", "jio", "vi", "vodafone", "bescom", "tata power", "adani electricity",
            "gas", "water", "broadband", "act fibernet", "hathway", "dth", "tata play",
            "dish tv", "recharge", "electricity", "utility", "postpaid", "prepaid",
            "nobroker", "rent", "maintenance", "google india digital"
        ),
        Category.ENTERTAINMENT to listOf(
            "netflix", "spotify", "amazon prime", "hotstar", "jiohotstar", "disney", "bookmyshow",
            "pvr", "inox", "youtube", "apple music", "sonyliv", "zee5", "gaana",
            "cinema", "movies", "theatre", "gaming", "steam", "playstation", "xbox"
        ),
        Category.HEALTHCARE to listOf(
            "apollo", "1mg", "pharmeasy", "medplus", "practo", "netmeds", "hospital",
            "clinic", "diagnostic", "lab", "pharmacy", "medical", "doctor", "dental",
            "healthcare", "chemist", "pathology", "opticals"
        ),
        Category.INVESTMENT to listOf(
            "groww", "zerodha", "upstox", "kuvera", "coin", "indmoney", "mutual fund",
            "sip", "bse", "nse", "angel one", "coindcx", "wazirx", "sharekhan", "stock",
            "equity", "securities", "wealth", "mandate", "mcxtrd", "nsetra"
        ),
        Category.TRANSFERS to listOf(
            "cred", "cred club", "credit card payment", "billdesk", "payment received towards your credit card",
            "towards your credit card", "received towards your card", "online payment of", "payment towards card",
            "self transfer", "transfer to", "transferred to", "sent to vpa", "imps", "neft", "rtgs"
        ),
        Category.SALARY_INCOME to listOf(
            "salary", "payroll", "stipend", "dividend", "interest credited", "bonus",
            "pension", "cashback received"
        ),
        Category.FEES_CHARGES to listOf(
            "annual fee", "late fee", "penalty", "amc", "interest charge", "processing fee",
            "service charge", "fine"
        ),
        Category.EDUCATION to listOf(
            "school", "college", "university", "coursera", "udemy", "edureka", "tuition",
            "academy", "institute", "classes", "exam fee"
        ),
        Category.PERSONAL to listOf(
            "salon", "spa", "barber", "parlour", "grooming", "fitness", "gym", "cultfit",
            "cult.fit", "massage", "flowers"
        )
    )

    fun classify(
        merchantOrVpa: String?,
        messageBody: String,
        transactionType: TransactionType,
        userRules: List<MerchantRule> = emptyList()
    ): Category {
        val merchant = merchantOrVpa?.lowercase() ?: ""
        val body = messageBody.lowercase()

        // 1. User custom rules take highest priority
        if (merchant.isNotEmpty()) {
            for (rule in userRules) {
                if (merchant.contains(rule.merchantPattern.lowercase())) {
                    return rule.category
                }
            }
        }

        // 2. Credit Card Bill Payments & Inter-Account Transfers take immediate precedence
        if (merchant.contains("cred") ||
            body.contains("cred club") ||
            body.contains("received towards your credit card") ||
            body.contains("payment towards your credit card") ||
            body.contains("towards your card ending") ||
            body.contains("billdesk")
        ) {
            return Category.TRANSFERS
        }

        // 3. Income / Salary detection
        if (transactionType == TransactionType.CREDIT) {
            if (body.contains("salary") || body.contains("payroll") || body.contains("stipend") || body.contains("bonus")) {
                return Category.SALARY_INCOME
            }
            if (body.contains("cashback")) {
                return Category.SALARY_INCOME
            }
        }

        // 4. Match merchant name against keyword dictionary
        if (merchant.isNotEmpty()) {
            for ((category, keywords) in KEYWORD_CATEGORY_MAP) {
                if (keywords.any { keyword -> merchant.contains(keyword) }) {
                    return category
                }
            }
        }

        // 5. Match message body keywords
        for ((category, keywords) in KEYWORD_CATEGORY_MAP) {
            if (keywords.any { keyword ->
                    Regex("""\b${Regex.escape(keyword)}\b""", RegexOption.IGNORE_CASE).containsMatchIn(body)
                }) {
                return category
            }
        }

        // 6. Generic UPI / P2P Transfer fallbacks
        if (body.contains("transferred to") || body.contains("sent to vpa") || body.contains("imps") || body.contains("neft") || body.contains("upi")) {
            return Category.TRANSFERS
        }

        return Category.OTHERS
    }
}
