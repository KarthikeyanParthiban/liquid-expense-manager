package com.expensemanager.app.parser

import com.expensemanager.app.core.model.Category
import com.expensemanager.app.core.model.MerchantRule
import com.expensemanager.app.core.model.TransactionType

object CategoryClassifier {

    data class CategorizationResult(
        val category: Category,
        val reason: String
    )

    private val KEYWORD_CATEGORY_MAP = mapOf(
        Category.FOOD to listOf(
            "zomato", "swiggy", "dominos", "mcdonald", "kfc", "starbucks", "burger king",
            "pizza hut", "subway", "chaayos", "chai point", "haldiram", "cafe", "restaurant",
            "barbeque", "biryani", "food", "dining", "eatery", "bakery", "cake", "dhaba",
            "tiffin", "mess", "bistro", "pastry", "coffee", "eats", "olympic", "buhari",
            "kaapi", "chat", "mithaai", "sangeetha", "hotel", "sweets", "juice", "kitchen",
            "canteen", "darshini", "bakes", "tandoori", "snacks", "tea", "dineout", "barbeque nation",
            "veg", "non veg", "thali", "south indian", "north indian", "meghana foods"
        ),
        Category.GROCERIES to listOf(
            "zepto", "blinkit", "instamart", "bigbasket", "dmart", "jiomart", "natures basket",
            "milkbasket", "grofers", "supermarket", "super marke", "provision", "spencers", "more retail",
            "fresh", "vegetables", "dairy", "fruits", "mart", "hypermarket", "kirana", "ration",
            "kpn", "farm fresh", "vegetable", "organic", "store", "general store", "grocery", "groceries",
            "minutes", "meat", "licious", "freshtohome"
        ),
        Category.SHOPPING to listOf(
            "amazon", "flipkart", "myntra", "ajio", "nykaa", "zara", "h&m", "meesho",
            "tata cliq", "decathlon", "croma", "reliance digital", "vijay sales", "lenskart",
            "uniqlo", "shoppers stop", "westside", "max fashion", "clothing", "apparel",
            "electronics", "footwear", "mall", "retail", "furniture", "ever shine", "tata neu",
            "jewellers", "jewellery", "gold", "thangamaligai", "malabar", "tanishq"
        ),
        Category.TRANSPORT to listOf(
            "uber", "ola", "rapido", "metro", "irctc", "makemytrip", "cleartrip", "yatra",
            "indigo", "air india", "fastag", "toll", "petrol", "indian oil",
            "bharat petroleum", "shell", "hpcl", "iocl", "bpcl", "fuel station", "cab",
            "flight", "airline", "railway", "diesel", "parking", "auto", "redbus", "redrail",
            "automobile", "namma metro", "bmrc", "airtel toll"
        ),
        Category.BILLS_UTILITIES to listOf(
            "airtel", "jio", "vodafone", "bescom", "tata power", "adani electricity",
            "water", "broadband", "act fibernet", "hathway", "dth", "tata play",
            "dish tv", "recharge", "electricity", "utility", "postpaid", "prepaid",
            "nobroker", "rent", "maintenance", "google india digital", "bbps", "bill desk",
            "mahadiscom", "cesc", "tneb", "torrent power", "piped gas", "igl", "mgl"
        ),
        Category.ENTERTAINMENT to listOf(
            "netflix", "spotify", "amazon prime", "prime video", "hotstar", "jiohotstar", "disney", "bookmyshow",
            "pvr", "inox", "youtube", "apple music", "sonyliv", "zee5", "gaana",
            "cinema", "movies", "theatre", "gaming", "steam", "playstation", "xbox", "cinepolis"
        ),
        Category.HEALTHCARE to listOf(
            "apollo", "1mg", "pharmeasy", "medplus", "practo", "netmeds", "hospital",
            "clinic", "diagnostic", "lab", "pharmacy", "medical", "doctor", "dental",
            "healthcare", "chemist", "pathology", "opticals", "dr lal", "metropolis", "care hospital"
        ),
        Category.INVESTMENT to listOf(
            "groww", "zerodha", "upstox", "kuvera", "coin", "indmoney", "mutual fund",
            "sip", "bse", "nse", "angel one", "coindcx", "wazirx", "sharekhan", "stock",
            "equity", "securities", "wealth", "mcxtrd", "nsetra", "smallcase",
            "camsonline", "kfintech", "mf central", "uti mf", "nippon", "sbi mutual", "mirae"
        ),
        Category.TRANSFERS to listOf(
            "cred", "cred club", "credit card payment", "billdesk", "payment received towards your credit card",
            "towards your credit card", "received towards your card", "online payment of", "payment towards card",
            "self transfer", "transfer to own", "atm wdl", "cash withdrawal", "atm cash", "cash wdl"
        ),
        Category.SALARY_INCOME to listOf(
            "salary", "payroll", "stipend", "dividend", "interest credited", "bonus",
            "pension", "cashback received"
        ),
        Category.FEES_CHARGES to listOf(
            "annual fee", "late fee", "penalty", "amc", "interest charge", "processing fee",
            "service charge", "fine", "debit card fee", "gst on charges"
        ),
        Category.EDUCATION to listOf(
            "school", "college", "university", "coursera", "udemy", "edureka", "tuition",
            "academy", "institute", "classes", "exam fee", "byju", "unacademy"
        ),
        Category.PERSONAL to listOf(
            "salon", "spa", "barber", "parlour", "grooming", "fitness", "gym", "cultfit",
            "cult.fit", "massage", "flowers", "fnp", "fern n petals"
        )
    )

    fun classifyWithReason(
        merchantOrVpa: String?,
        messageBody: String,
        transactionType: TransactionType,
        userRules: List<MerchantRule> = emptyList()
    ): CategorizationResult {
        val merchant = merchantOrVpa?.lowercase()?.trim() ?: ""
        val body = messageBody.lowercase()

        // 1. User custom rules take highest priority
        if (merchant.isNotEmpty()) {
            for (rule in userRules) {
                if (merchant.contains(rule.merchantPattern.lowercase())) {
                    return CategorizationResult(rule.category, "User Custom Rule: '${rule.merchantPattern}'")
                }
            }
        }

        // 2. Credit Card Bill Payments, Cash Withdrawals, and Inter-Account Transfers
        if (transactionType == TransactionType.CARD_SETTLEMENT ||
            transactionType == TransactionType.CASH_WITHDRAWAL ||
            transactionType == TransactionType.TRANSFER ||
            merchant.contains("cred") ||
            body.contains("cred club") ||
            body.contains("received towards your credit card") ||
            body.contains("payment towards your credit card") ||
            body.contains("towards your card ending") ||
            body.contains("billdesk")
        ) {
            return CategorizationResult(Category.TRANSFERS, "Transfer / Settlement rule")
        }

        // 3. Income / Salary detection
        if (transactionType == TransactionType.CREDIT) {
            if (body.contains("salary") || body.contains("payroll") || body.contains("stipend") || body.contains("bonus")) {
                return CategorizationResult(Category.SALARY_INCOME, "Income: Salary / Payroll keyword detected")
            }
            if (body.contains("cashback")) {
                return CategorizationResult(Category.SALARY_INCOME, "Income: Cashback keyword detected")
            }
        }

        // 4. Contextual Brand / Sub-service disambiguation
        // Amazon disambiguation
        if (merchant.contains("amazon") || body.contains("amazon")) {
            return when {
                body.contains("fresh") || body.contains("pantry") || merchant.contains("fresh") || merchant.contains("pantry") ->
                    CategorizationResult(Category.GROCERIES, "Context: Amazon Fresh / Pantry")
                body.contains("prime") || merchant.contains("prime") ->
                    CategorizationResult(Category.ENTERTAINMENT, "Context: Amazon Prime")
                body.contains("bill") || body.contains("recharge") ->
                    CategorizationResult(Category.BILLS_UTILITIES, "Context: Amazon Pay Utility Bill")
                else ->
                    CategorizationResult(Category.SHOPPING, "Merchant: Amazon Shopping")
            }
        }

        // Swiggy disambiguation
        if (merchant.contains("swiggy") || body.contains("swiggy")) {
            return when {
                body.contains("instamart") || merchant.contains("instamart") ->
                    CategorizationResult(Category.GROCERIES, "Context: Swiggy Instamart")
                body.contains("dineout") || merchant.contains("dineout") ->
                    CategorizationResult(Category.FOOD, "Context: Swiggy Dineout")
                else ->
                    CategorizationResult(Category.FOOD, "Merchant: Swiggy Food Delivery")
            }
        }

        // Flipkart disambiguation
        if (merchant.contains("flipkart") || body.contains("flipkart")) {
            return when {
                body.contains("minutes") || body.contains("grocery") || body.contains("supermart") ->
                    CategorizationResult(Category.GROCERIES, "Context: Flipkart Grocery / Minutes")
                else ->
                    CategorizationResult(Category.SHOPPING, "Merchant: Flipkart Shopping")
            }
        }

        // Tata disambiguation
        if (merchant.contains("tata") || body.contains("tata")) {
            return when {
                merchant.contains("1mg") || body.contains("1mg") ->
                    CategorizationResult(Category.HEALTHCARE, "Context: Tata 1mg Pharmacy")
                merchant.contains("play") || merchant.contains("power") || body.contains("tata play") || body.contains("tata power") ->
                    CategorizationResult(Category.BILLS_UTILITIES, "Context: Tata Utility")
                else ->
                    CategorizationResult(Category.SHOPPING, "Merchant: Tata Shopping")
            }
        }

        // 5. Match merchant name against keyword dictionary using Word Boundary matching
        if (merchant.isNotEmpty()) {
            for ((category, keywords) in KEYWORD_CATEGORY_MAP) {
                val matchedKeyword = keywords.firstOrNull { keyword ->
                    if (keyword.length <= 4) {
                        Regex("""\b${Regex.escape(keyword)}\b""", RegexOption.IGNORE_CASE).containsMatchIn(merchant)
                    } else {
                        merchant.contains(keyword)
                    }
                }
                if (matchedKeyword != null) {
                    return CategorizationResult(category, "Merchant keyword matched: '$matchedKeyword'")
                }
            }
        }

        // 6. Match message body keywords with word boundaries
        for ((category, keywords) in KEYWORD_CATEGORY_MAP) {
            val matchedKeyword = keywords.firstOrNull { keyword ->
                Regex("""\b${Regex.escape(keyword)}\b""", RegexOption.IGNORE_CASE).containsMatchIn(body)
            }
            if (matchedKeyword != null) {
                return CategorizationResult(category, "Body keyword matched: '$matchedKeyword'")
            }
        }

        // 7. Explicit self-transfer
        if (body.contains("self transfer") || body.contains("own account")) {
            return CategorizationResult(Category.TRANSFERS, "Self Transfer")
        }

        return CategorizationResult(Category.OTHERS, "Default Fallback")
    }

    fun classify(
        merchantOrVpa: String?,
        messageBody: String,
        transactionType: TransactionType,
        userRules: List<MerchantRule> = emptyList()
    ): Category {
        return classifyWithReason(merchantOrVpa, messageBody, transactionType, userRules).category
    }
}
