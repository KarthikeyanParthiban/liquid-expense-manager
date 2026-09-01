package com.expensemanager.app.parser

import com.expensemanager.app.core.model.Category
import com.expensemanager.app.core.model.MerchantRule
import com.expensemanager.app.core.model.TransactionType
import com.expensemanager.app.parser.pipeline.AmountPrior

object CategoryClassifier {

    data class CategorizationResult(
        val category: Category,
        val reason: String
    )

    // Word-boundary regex for the \bcred\b check used in tier 2
    private val CRED_WORD_REGEX = Regex("""\bcred\b""", RegexOption.IGNORE_CASE)

    // BNPL service names — matched by word boundary in tier 2 so their repayment SMS
    // (which contain the BNPL brand name and no other merchant context) go to TRANSFERS
    private val BNPL_REGEX = Regex(
        """\b(?:lazypay|zestmoney|simpl|axio|ola\s*money\s*postpaid|paytm\s*postpaid|flipkart\s*pay\s*later|amazon\s*pay\s*later)\b""",
        RegexOption.IGNORE_CASE
    )

    private val KEYWORD_CATEGORY_MAP = mapOf(
        Category.FOOD to listOf(
            // Food Delivery & QSR Brands
            "swiggy", "zomato", "dominos", "domino", "mcdonalds", "mcdonald", "mcd", "kfc",
            "starbucks", "burger king", "pizza hut", "subway", "taco bell", "wendy",
            "wow momo", "wow china", "behrouz", "faasos", "ovenstory", "eatclub", "box8",
            "freshmenu", "rebel foods", "mojo pizza", "firangi bake", "lunchbox", "sweet truth",
            "daily kitchen", "itminaan biryani", "good bowl", "mandarin oak", "biryani by kilo",
            "mad over donuts", "dunkin", "belgian waffle", "tibbs frankie", "rolls king",
            "rollsking", "kathi rolls",
            // Cafes & Beverages
            "chaayos", "chai point", "chai sutta", "costa coffee", "cafe coffee day", "ccd",
            "blue tokai", "third wave coffee", "roastery coffee", "barista", "sleepy owl",
            "theobroma", "naturals ice cream", "baskin robbins", "giani", "polar bear", "ibaco",
            "cream stone", "havmor", "kwality walls", "lassi shop", "keventers", "mba chaiwala",
            // Casual & Fine Dining / Pubs / Breweries
            "barbeque nation", "absolute barbecues", "bbq nation",
            "mainland china", "sigree", "farzi cafe", "social restaurant", "hard rock cafe",
            "lord of the drinks", "smoke house deli", "mamagoto", "chilis", "punjab grill",
            "copper chimney", "rajdhani", "sukhsagar",
            // Regional Chains & Sweets
            "haldiram", "bikanervala", "bikaner", "a2b", "adyar ananda", "saravana bhavan",
            "saravana", "sangeetha", "murugan idli", "anjappar", "thalappakatti", "meghana foods",
            "meghana biryani", "nagarjuna", "empire restaurant", "truffles", "paradise biryani",
            "bawarchi", "pista house", "karachi bakery", "pulla reddy", "anand sweets", "kc das",
            "mithai", "mishti", "halwa", "buhari", "kaapi",
            // General Food Terms
            // NOTE: "hotel" removed — causes OYO/hotel-stay bookings to misclassify as FOOD.
            //       Hotel brands (OYO, Treebo, etc.) are handled in the TRANSPORT keyword list.
            // NOTE: "sweet" and "sweets" deduplicated — keep only "sweets" (broader match)
            // NOTE: "olympic" duplicate removed
            "food", "dining", "restaurant", "eatery", "bakery", "cake", "cakes", "dhaba",
            "tiffin", "mess", "bistro", "pastry", "coffee", "eats", "meal", "breakfast",
            "lunch", "dinner", "snacks", "juice", "kitchen", "canteen", "darshini", "bakes",
            "tandoori", "tea", "dineout", "eazydiner", "magicpin", "veg", "non veg", "thali",
            "south indian", "north indian", "biryani", "pizza", "burger", "momos", "shawarma",
            "waffle", "shakes", "brewery", "pub", "lounge", "barbeque", "barbecue", "pizzeria",
            "grill", "roastery", "dessert", "desserts", "sweets"
        ),
        Category.GROCERIES to listOf(
            // Quick Commerce & Online Supermarkets
            "zepto", "blinkit", "instamart", "bigbasket", "bb daily", "bb instant", "bbinstant",
            "dmart", "dmart ready", "jiomart", "natures basket", "milkbasket", "country delight",
            "akshayakalpa", "grofers", "reliance smart", "lulu hypermarket",
            "spencers", "more retail", "more supermarket", "easyday", "spar", "star bazaar",
            "ratnadeep", "modern bazaar", "heritage fresh", "namdharis", "namdhari",
            "kpn farm fresh", "kpn", "otipy", "fraazo", "dealshare", "citymall", "pluckk",
            "kisankonnect", "falhari",
            // Fresh Meat & Seafood
            "licious", "freshtohome", "meatigo", "tendercuts", "meat", "fish", "chicken",
            // Supermarket & Produce Keywords
            // NOTE: "supermarket" duplicate removed — kept once below
            "supermarket", "hypermarket", "kirana", "ration", "departmental store", "organic",
            "organic world", "dairy", "milk", "eggs", "vegetables", "fruits", "vegetable",
            "farm fresh", "mandi", "bazaar", "grocery", "groceries", "daily needs", "dry fruits",
            "general store", "fresh mart", "city mart", "daily mart", "family mart",
            "provision store", "provisions"
        ),
        Category.SHOPPING to listOf(
            // E-Commerce & Retail Giants
            "amazon", "flipkart", "myntra", "ajio", "nykaa", "nykaa man", "tata cliq", "tatacliq",
            "tata neu", "tatanew", "meesho", "shopsy", "snapdeal", "shopclues", "limeroad",
            "purplle", "firstcry", "hopscotch", "tira beauty",
            // Apparel & Fashion Brands
            "zara", "h&m", "uniqlo", "marks & spencer", "marks and spencer", "shoppers stop",
            "westside", "lifestyle", "pantaloons", "max fashion", "reliance trends", "trends",
            "zudio", "snitch", "bewakoof", "souled store", "urbanic", "rare rabbit",
            "flying machine", "allen solly", "peter england", "van heusen", "louis philippe",
            "arrow", "raymond", "manyavar", "mohey", "fabindia", "biba", "w for woman",
            "aurelia", "global desi", "libas", "levis", "levi's", "us polo", "uspa",
            "pepe jeans", "tommy hilfiger", "calvin klein", "puma", "nike", "adidas",
            "skechers", "reebok", "asics", "under armour", "decathlon", "bata", "woodland",
            "metro shoes", "mochi", "red tape", "redtape", "crocs", "birkenstock", "clarks",
            // Electronics & Appliances
            "croma", "reliance digital", "vijay sales", "poorvika", "sangeetha mobiles",
            "pai international", "apple store", "imagine", "aptronix", "unicorn", "samsung",
            "oneplus", "xiaomi", "mi store", "realme", "boat", "noise", "fire boltt", "boult",
            // Eyewear & Watches
            "lenskart", "specsmakers", "titan eyeplus", "lawrence & mayo", "titan", "fastrack",
            "sonata", "casio", "fossil", "timex", "helios", "ethos",
            // Jewellery & Luxury
            "tanishq", "caratlane", "bluestone", "mia by tanishq", "malabar gold", "malabar",
            "kalyan jewellers", "grt jewellers", "grt", "joyalukkas", "jos alukkas", "senco gold",
            "lalitha jewellery", "bhima jewellers", "pc jeweller", "png jewellers", "orra",
            "melorra", "giva", "candere", "thangamaligai", "jewellers", "jewellery",
            "diamond", "silver", "gems", "ever shine",
            // NOTE: "gold" removed from SHOPPING — too ambiguous with gold bond investments.
            //       "gold bond" and "sovereign gold bond" remain in INVESTMENT.
            // Home, Furniture & Toys
            "ikea", "home centre", "pepperfry", "urban ladder", "wakefit", "sleepwell",
            "kurlon", "d decor", "bombay dyeing", "miniso", "mumuso", "hamleys", "toys r us",
            "crossword", "sapna book house", "archies",
            // General Shopping Terms
            // NOTE: "flowers" moved to PERSONAL (gifting/florist context is more accurate)
            "shopping", "retail", "apparel", "clothing", "garments", "footwear", "boutique",
            "tailor", "mall", "electronics", "furniture", "books", "stationery", "hardware",
            "hardwares", "olx"
        ),
        Category.TRANSPORT to listOf(
            // Rides & Cabs
            "uber", "ola", "rapido", "blusmart", "namma yatri", "indrive", "shoffr",
            "mega cabs", "meru", "yatri", "auto", "rickshaw", "taxi", "cab",
            // Railways & Public Metro
            "irctc", "indian railways", "uts", "railyatri", "confirmtkt", "trainman",
            "metro", "namma metro", "dmrc", "delhi metro", "mumbai metro", "chennai metro",
            "cmrl", "bmrc", "maha metro", "hyderabad metro", "kochi metro", "bmtc", "best",
            "dtc", "ksrtc", "msrtc", "tsrtc", "apsrtc", "tnstc", "railway",
            // Buses & Travel Booking
            "redbus", "abhibus", "zingbus", "intrcity", "nuego", "chalo",
            "makemytrip", "mmt", "goibibo", "cleartrip", "yatra", "easemytrip", "ixigo",
            "booking.com", "agoda", "airbnb", "oyo", "treebo", "fabhotels",
            // Airlines & Flights
            "indigo", "indigo airlines", "air india", "vistara", "spicejet", "akasa air",
            "air india express", "emirates", "qatar airways", "singapore airlines", "lufthansa",
            "flight", "airline", "airfare", "airport",
            // Tolls & Parking
            "fastag", "nhai", "netc", "ihmcl", "toll", "tollgate", "toll plaza", "parking",
            "park+", "valet", "airtel toll",
            // Fuel Stations & Petrol Pumps
            "indian oil", "iocl", "bharat petroleum", "bpcl", "hindustan petroleum", "hpcl",
            "shell", "nayara", "jio-bp", "essar", "petrol", "diesel", "cng", "fuel",
            "fuel station", "petrol pump",
            // EV Charging
            "tata power ev", "statiq", "kazam", "chargezone", "zeon", "ather", "ola electric",
            "ather grid", "charge zone"
        ),
        Category.BILLS_UTILITIES to listOf(
            // Telecom & DTH
            "airtel", "airtel postpaid", "airtel prepaid", "reliance jio", "vodafone",
            "vi mobile", "vodafone idea", "bsnl", "mtnl", "tata play", "tata sky", "dish tv",
            "sun direct", "d2h", "airtel dth", "videocon d2h",
            // NOTE: "jio" alone removed from here — it's handled by the Jio disambiguation block
            //       in tier 5, which correctly routes jiomart → GROCERIES, jiocinema → ENTERTAINMENT,
            //       jio-bp → TRANSPORT, and bare "jio" → BILLS_UTILITIES.
            // Broadband & Fiber
            "act fibernet", "act broadband", "hathway", "spectra", "excitel", "tikona",
            "you broadband", "jiofiber", "airtel xstream", "gtpl", "asianet", "netplus",
            "broadband", "internet", "fibernet",
            // Electricity Providers
            "bescom", "tata power", "adani electricity", "mahadiscom", "msedcl", "torrent power",
            "bses rajdhani", "bses yamuna", "cesc", "tneb", "tangedco", "tsspdcl", "tsnpdcl",
            "apcpdcl", "uppcl", "dhbvn", "uhbvn", "pspcl", "wbsedcl", "kseb", "jvvnl",
            "electricity",
            // Water & Municipality
            "delhi jal board", "djb", "bwssb", "hmwssb", "metrowater", "water board",
            "property tax", "municipal", "corporation tax", "water",
            // Gas & LPG
            "mahanagar gas", "mgl", "indraprastha gas", "igl", "gail", "adani gas",
            "torrent gas", "gujarat gas", "hp gas", "indane", "bharat gas", "lpg", "piped gas",
            // Housing & Rent
            "nobroker", "nobrokerhood", "mygate", "apnacomplex", "maintenance",
            "society maintenance", "apartment maintenance", "rent", "house rent", "landlord",
            // Utility Portals & Cloud Subscriptions
            "google india digital", "bbps", "bill desk", "billdesk", "recharge", "utility",
            "postpaid", "prepaid", "dth", "google cloud", "google clou", "deepseek", "anthropic",
            "openai", "chatgpt", "github", "cursor"
        ),
        Category.ENTERTAINMENT to listOf(
            // Video & OTT Streaming
            "netflix", "spotify", "amazon prime", "prime video", "disney", "hotstar",
            "jiohotstar", "jiocinema", "apple tv", "apple music", "apple tv+",
            "sonyliv", "zee5", "voot", "aha", "sun nxt", "hoichoi", "lionsgate",
            "discovery+", "youtube premium", "youtube music", "youtube", "google play",
            "mubi", "crunchyroll", "gaana", "wynk",
            // Podcasts & Audio
            "audible", "storytel", "kuku fm", "pocket fm",
            // Movies & Events
            "bookmyshow", "bms", "pvr", "inox", "pvr inox", "cinepolis", "moviemax",
            "carnival cinemas", "miraj cinemas", "mukta a2", "spi cinemas", "sathyam cinemas",
            "eventbrite", "paytm insider", "insider.in", "cinema", "movies", "theatre",
            // Gaming & Digital Goods
            "steam", "playstation", "psn", "xbox", "nintendo", "epic games", "riot games",
            "bgmi", "krafton", "roblox", "twitch", "discord", "gaming", "arcade", "bowling",
            // Amusement & Recreation
            "smaaash", "timezone", "imagicaa", "wonderla", "water park", "theme park",
            "concert", "comedy", "ticket 9", "unipex", "sportsbiz"
            // NOTE: "club" removed — too ambiguous (club membership = PERSONAL, nightclub = FOOD)
            //       Named clubs like "smaaash" are covered above.
            // NOTE: "premium" removed — insurance premium SMS would misclassify as ENTERTAINMENT.
            //       "spotify premium", "youtube premium" etc. are covered by the brand names above.
        ),
        Category.HEALTHCARE to listOf(
            // Pharmacies & Health E-Commerce
            "apollo", "apollo pharmacy", "apollo 24/7", "1mg", "tata 1mg", "pharmeasy",
            "medplus", "netmeds", "practo", "clinikally", "truemeds", "wellness forever",
            "frank ross", "noble plus", "guardian pharmacy", "medibuddy", "mfine", "pristyn care",
            // Diagnostics & Labs
            "dr lal", "lal pathlabs", "srl diagnostics", "agilus", "metropolis",
            "suburban diagnostics", "vijaya diagnostic", "pathkind", "thyrocare", "redcliffe",
            "healthians",
            // Hospitals & Clinics
            "apollo hospital", "max healthcare", "fortis", "manipal", "narayana health",
            "aster dm", "cloudnine", "motherhood", "rainbow hospital", "care hospital",
            "care hospitals", "medanta", "aiims", "kokilaben", "kims",
            // Dental & Optical
            "clove dental", "mydentist", "dental", "dentist", "opticals", "optician",
            // General Medical Terms
            "hospital", "clinic", "diagnostic", "diagnostics", "pathology",
            "pharmacy", "chemist", "druggist", "medical", "medicine", "doctor",
            "healthcare", "consultation", "nursing home", "pharma"
            // NOTE: "lab" removed — "computer lab fee", "lab test" etc. are too ambiguous at keyword level.
            //       Named labs (Lal Pathlabs, Metropolis, etc.) are covered by brand names above.
        ),
        Category.INVESTMENT to listOf(
            // Broking & Trading Platforms
            "groww", "zerodha", "upstox", "angel one", "5paisa", "icici direct", "hdfc sky",
            "kotak securities", "motilal oswal", "sharekhan", "dhan", "fyers", "indmoney",
            "kuvera", "smallcase", "coin by zerodha", "scripbox", "et money", "paytm money",
            "dezerv", "wint wealth", "jiraaf", "grip invest", "stable money", "goldenpi",
            "vested", "fi wealth", "jupiter money", "shoonya", "coindcx", "wazirx",
            // Mutual Funds & Registrars
            "camsonline", "cams", "kfintech", "mf central", "mutual fund", "sip",
            // NOTE: "amc" removed from INVESTMENT — it also means "Annual Maintenance Charge"
            //       (debit card/account AMC). That conflict always resolved to INVESTMENT (wrong).
            //       Specific AMF/fund-house names above are sufficient for investment detection.
            //       "amc" in banking fee context is handled in FEES_CHARGES tier 4 check.
            "uti mf", "nippon", "sbi mutual", "mirae", "parag parikh", "ppfas", "kotak mutual",
            "dsp mutual", "quant mutual", "bandhan mutual", "tata mutual", "motilal mutual",
            "icici prudential mutual", "hdfc mutual",
            // Insurance Providers
            "lic", "life insurance", "hdfc life", "icici prudential life", "icici pru life",
            "max life", "sbi life", "bajaj allianz", "tata aia", "star health", "care health",
            "niva bupa", "hdfc ergo", "icici lombard", "digit insurance", "go digit", "acko",
            "policybazaar", "insurance",
            // NOTE: "premium" removed from INVESTMENT — conflicts with ENTERTAINMENT.
            //       Insurance-premium detection is now handled by the named brand list above
            //       combined with body-level keyword check in tier 4 (added "insurance premium").
            // Securities & Gold Bonds
            "bse", "nse", "cdsl", "nsdl", "mcxtrd", "nsetra", "stock", "equity", "securities",
            "wealth", "gold bond", "sgb", "sovereign gold bond"
        ),
        Category.TRANSFERS to listOf(
            // Credit Card Payments & Settlements
            // NOTE: "cred" as a bare keyword is intentionally NOT here — tier 2 uses word-boundary
            //       regex \bcred\b to avoid substring hits like "incredible", "accredited".
            "cred club", "cheq", "credit card payment", "payu", "razorpay",
            "ccavenue", "cashfree", "payment received towards your credit card",
            "towards your credit card", "received towards your card", "payment towards card",
            "card payment", "card settlement",
            // BNPL repayment keywords
            "lazypay", "zestmoney", "simpl repay", "axio repay", "pay later repayment",
            // Self Transfers & ATM
            "self transfer", "transfer to own", "own account", "atm wdl", "cash withdrawal",
            "atm cash", "cash wdl", "withdrawn from atm", "cash dispense"
        ),
        Category.SALARY_INCOME to listOf(
            "salary", "payroll", "stipend", "dividend", "interest credited", "bonus",
            "pension", "remuneration", "incentive"
            // NOTE: "cashback" and "cashback received" removed from SALARY_INCOME — cashback
            //       credits are NOT salary/income. They are now categorised as OTHERS (or can be
            //       added as a separate sub-category in future). See tier 3 cashback handling below.
        ),
        // FEES_CHARGES comes BEFORE INVESTMENT in the map so "amc" resolves correctly here
        Category.FEES_CHARGES to listOf(
            "annual fee", "late fee", "penalty", "amc", "annual maintenance",
            "interest charge", "processing fee", "service charge", "fine",
            "debit card fee", "gst on charges", "forex markup",
            "overlimit fee", "card replacement fee", "bounce charges", "penal charges",
            "minimum balance charge", "non maintenance"
            // NOTE: "amc" added here AND removed from INVESTMENT. FEES_CHARGES is now
            //       listed BEFORE INVESTMENT in the map so keyword lookup hits this first.
            // NOTE: "insurance premium" removed — belongs in INVESTMENT, not fees.
        ),
        Category.EDUCATION to listOf(
            // Online Learning & EdTech
            "coursera", "udemy", "edureka", "simplilearn", "upgrad", "scaler", "newton school",
            "great learning", "emeritus", "skillshare", "linkedin learning", "unacademy",
            "byju", "vedantu", "physicswallah", "pw", "allen", "aakash", "fiitjee",
            "resonance", "brilliant", "chegg", "duolingo", "british council",
            // General Education Terms
            // NOTE: "coaching" and "classes" removed — too ambiguous (fitness classes, cooking
            //       classes misclassify as EDUCATION). Named platforms above are sufficient.
            "school", "college", "university", "institute", "academy", "tuition",
            "exam fee", "upsc", "gate", "cat", "gre", "gmat", "ielts", "toefl", "library",
            "coaching centre", "study material"
        ),
        Category.PERSONAL to listOf(
            // Fitness & Gyms
            "cultfit", "cult.fit", "cult fit", "golds gym", "gold's gym", "anytime fitness",
            "snap fitness", "talwalkars", "fitness first", "curefit", "gym", "fitness",
            "yoga", "pilates", "crossfit", "zumba",
            // Salons, Spas & Beauty
            "urban company", "urbanclap", "naturals salon", "enrich salon",
            "lakme salon", "toni & guy", "jawed habib", "geetanjali salon", "bblunt",
            "vlcc", "kaya skin", "bodycraft", "truefitt", "spa", "salon", "parlour",
            "barber", "haircut", "grooming", "massage", "dermatologist",
            // Gifting & Florists — moved here from SHOPPING for more accurate categorisation
            "flowers", "fnp", "fern n petals", "ferns n petals", "floweraura", "interflora",
            "igp.com", "florist", "gift",
            // Fitness classes (moved from EDUCATION)
            "fitness class", "yoga class", "dance class", "zumba class", "pilates class",
            "sports class", "swimming class"
        )
    )

    fun classifyWithReason(
        merchantOrVpa: String?,
        messageBody: String,
        transactionType: TransactionType,
        userRules: List<MerchantRule> = emptyList(),
        amount: Double = 0.0,
        currency: String = "INR",
        timestampMs: Long = System.currentTimeMillis()
    ): CategorizationResult {
        val rawMerchant = merchantOrVpa?.trim() ?: ""
        val merchant = rawMerchant.lowercase()
        val body = messageBody.lowercase()

        // 1. User custom rules take highest priority
        if (merchant.isNotEmpty()) {
            for (rule in userRules) {
                if (merchant.contains(rule.merchantPattern.lowercase())) {
                    return CategorizationResult(rule.category, "User Custom Rule: '${rule.merchantPattern}'")
                }
            }
        }

        // 1b. MCC code lookup — ground-truth category signal from the card network.
        // Present in some credit-card SMS bodies as "MCC: 5812" or "Merchant Code: 5411".
        // When present this is more reliable than any keyword or ML inference.
        val mcc = BankPatterns.extractMcc(messageBody)
        if (mcc != null) {
            val mccCategory = BankPatterns.mccToCategory(mcc)
            if (mccCategory != null && mccCategory != Category.OTHERS) {
                return CategorizationResult(mccCategory, "MCC $mcc → ${mccCategory.displayName}")
            }
        }

        // 2. Credit Card Bill Payments, BNPL repayments, Cash Withdrawals, and Inter-Account Transfers
        // FIX: Use \bcred\b word-boundary regex instead of raw substring to prevent false hits
        //      on "incredible", "credentials", "sacred", etc.
        if (transactionType == TransactionType.CARD_SETTLEMENT ||
            transactionType == TransactionType.CASH_WITHDRAWAL ||
            transactionType == TransactionType.TRANSFER ||
            CRED_WORD_REGEX.containsMatchIn(merchant) ||
            CRED_WORD_REGEX.containsMatchIn(body) ||
            BNPL_REGEX.containsMatchIn(merchant) ||
            BNPL_REGEX.containsMatchIn(body) ||
            body.contains("cred club") ||
            body.contains("towards your credit card") ||
            body.contains("payment towards your credit card") ||
            body.contains("towards your card ending") ||
            (body.contains("received towards your") && body.contains("card")) ||
            (body.contains("payment of") && body.contains("received towards"))
        ) {
            return CategorizationResult(Category.TRANSFERS, "Credit Card Bill Settlement / Transfer")
        }

        // 3. Income / Salary detection
        if (transactionType == TransactionType.CREDIT) {
            if (body.contains("salary") || body.contains("payroll") || body.contains("stipend") ||
                body.contains("bonus") || body.contains("remuneration")
            ) {
                return CategorizationResult(Category.SALARY_INCOME, "Income: Salary / Payroll keyword detected")
            }
            if (body.contains("dividend") || body.contains("interest credited")) {
                return CategorizationResult(Category.SALARY_INCOME, "Income: Dividend / Interest credited")
            }
            // FIX: cashback is NOT salary/income — return OTHERS so it doesn't pollute the income bucket.
            // A dedicated CASHBACK sub-category can be added later if needed.
            if (body.contains("cashback")) {
                return CategorizationResult(Category.OTHERS, "Income: Cashback credit (not categorised as salary)")
            }
        }

        // 4. Fees and Charges — checked before keyword map so "amc" resolves here, not INVESTMENT.
        // NOTE: "insurance premium" is intentionally NOT here — an insurance premium is an
        //       INVESTMENT/protection outflow, not a bank fee. It's handled by the INVESTMENT
        //       keyword map (LIC, HDFC Life, Star Health, "insurance", etc.).
        if (body.contains("annual fee") || body.contains("late fee") || body.contains("penalty") ||
            body.contains("forex markup") || body.contains("gst on charges") ||
            body.contains("interest charge") || body.contains("mandate return") ||
            body.contains("ecs return") || body.contains("annual maintenance charge") ||
            body.contains("amc charge")
        ) {
            return CategorizationResult(Category.FEES_CHARGES, "Banking Fees / Surcharge detected")
        }

        // 5. Contextual Brand / Sub-service disambiguation

        // Amazon disambiguation
        if (Regex("""\bamazon\b""", RegexOption.IGNORE_CASE).containsMatchIn(merchant) ||
            Regex("""\bamazon\b""", RegexOption.IGNORE_CASE).containsMatchIn(body)
        ) {
            return when {
                body.contains("fresh") || body.contains("pantry") ||
                merchant.contains("fresh") || merchant.contains("pantry") ->
                    CategorizationResult(Category.GROCERIES, "Context: Amazon Fresh / Pantry")
                body.contains("prime") || merchant.contains("prime") ||
                body.contains("kindle") || body.contains("audible") ->
                    CategorizationResult(Category.ENTERTAINMENT, "Context: Amazon Prime / Digital")
                // FIX: "Amazon Pay Later" repayments are TRANSFERS, not shopping
                body.contains("pay later") || merchant.contains("pay later") ->
                    CategorizationResult(Category.TRANSFERS, "Context: Amazon Pay Later Repayment")
                body.contains("bill") || body.contains("recharge") ||
                body.contains("electricity") || body.contains("dth") || body.contains("gas") ->
                    CategorizationResult(Category.BILLS_UTILITIES, "Context: Amazon Pay Utility Bill")
                else ->
                    CategorizationResult(Category.SHOPPING, "Merchant: Amazon Shopping")
            }
        }

        // Swiggy disambiguation
        if (Regex("""\bswiggy\b""", RegexOption.IGNORE_CASE).containsMatchIn(merchant) ||
            Regex("""\bswiggy\b""", RegexOption.IGNORE_CASE).containsMatchIn(body)
        ) {
            return when {
                body.contains("instamart") || merchant.contains("instamart") ->
                    CategorizationResult(Category.GROCERIES, "Context: Swiggy Instamart")
                body.contains("dineout") || merchant.contains("dineout") ->
                    CategorizationResult(Category.FOOD, "Context: Swiggy Dineout")
                body.contains("genie") || merchant.contains("genie") ->
                    CategorizationResult(Category.TRANSPORT, "Context: Swiggy Genie Courier")
                else ->
                    CategorizationResult(Category.FOOD, "Merchant: Swiggy Food Delivery")
            }
        }

        // Zomato disambiguation
        if (Regex("""\bzomato\b""", RegexOption.IGNORE_CASE).containsMatchIn(merchant) ||
            Regex("""\bzomato\b""", RegexOption.IGNORE_CASE).containsMatchIn(body)
        ) {
            return when {
                body.contains("blinkit") || merchant.contains("blinkit") ||
                body.contains("hyperpure") || merchant.contains("hyperpure") ->
                    CategorizationResult(Category.GROCERIES, "Context: Zomato Blinkit / Hyperpure")
                body.contains("dining") || body.contains("gold") ->
                    CategorizationResult(Category.FOOD, "Context: Zomato Dining")
                else ->
                    CategorizationResult(Category.FOOD, "Merchant: Zomato Food Delivery")
            }
        }

        // Flipkart disambiguation
        if (Regex("""\bflipkart\b""", RegexOption.IGNORE_CASE).containsMatchIn(merchant) ||
            Regex("""\bflipkart\b""", RegexOption.IGNORE_CASE).containsMatchIn(body)
        ) {
            return when {
                body.contains("minutes") || body.contains("grocery") ||
                body.contains("supermart") || merchant.contains("minutes") ->
                    CategorizationResult(Category.GROCERIES, "Context: Flipkart Grocery / Minutes")
                body.contains("travel") || body.contains("flight") || body.contains("hotel") ->
                    CategorizationResult(Category.TRANSPORT, "Context: Flipkart Travel")
                // FIX: Flipkart Pay Later repayments
                body.contains("pay later") || merchant.contains("pay later") ->
                    CategorizationResult(Category.TRANSFERS, "Context: Flipkart Pay Later Repayment")
                else ->
                    CategorizationResult(Category.SHOPPING, "Merchant: Flipkart Shopping")
            }
        }

        // Tata disambiguation
        if (Regex("""\btata\b""", RegexOption.IGNORE_CASE).containsMatchIn(merchant) ||
            Regex("""\btata\b""", RegexOption.IGNORE_CASE).containsMatchIn(body)
        ) {
            return when {
                merchant.contains("1mg") || body.contains("1mg") ->
                    CategorizationResult(Category.HEALTHCARE, "Context: Tata 1mg Pharmacy")
                merchant.contains("play") || merchant.contains("power") ||
                body.contains("tata play") || body.contains("tata power") || body.contains("tata sky") ->
                    CategorizationResult(Category.BILLS_UTILITIES, "Context: Tata Utility")
                merchant.contains("motors") || body.contains("tata motors") || body.contains("ev charging") ->
                    CategorizationResult(Category.TRANSPORT, "Context: Tata Motors / EV")
                merchant.contains("capital") || merchant.contains("mutual") ||
                body.contains("tata mutual") || body.contains("tata aia") ->
                    CategorizationResult(Category.INVESTMENT, "Context: Tata Capital / Insurance / Mutual Fund")
                else ->
                    CategorizationResult(Category.SHOPPING, "Merchant: Tata Shopping / Neu")
            }
        }

        // Jio disambiguation (guard: 'ajio' must NOT match)
        val hasJio = !merchant.contains("ajio") && !body.contains("ajio") &&
                (Regex("""\bjio\b""", RegexOption.IGNORE_CASE).containsMatchIn(merchant) ||
                 Regex("""\bjio\b""", RegexOption.IGNORE_CASE).containsMatchIn(body) ||
                 merchant.startsWith("jio") || body.contains("jiofiber") ||
                 body.contains("jiomart") || body.contains("jiocinema"))

        if (hasJio) {
            return when {
                merchant.contains("mart") || body.contains("jiomart") || body.contains("jio mart") ->
                    CategorizationResult(Category.GROCERIES, "Context: JioMart")
                merchant.contains("cinema") || merchant.contains("hotstar") ||
                body.contains("jiocinema") || body.contains("jiohotstar") ->
                    CategorizationResult(Category.ENTERTAINMENT, "Context: JioCinema / Media")
                merchant.contains("bp") || body.contains("jio-bp") || body.contains("jio bp") ->
                    CategorizationResult(Category.TRANSPORT, "Context: Jio-bp Fuel")
                else ->
                    CategorizationResult(Category.BILLS_UTILITIES, "Context: Jio Telecom / Fiber Recharge")
            }
        }

        // Google disambiguation
        if (Regex("""\bgoogle\b""", RegexOption.IGNORE_CASE).containsMatchIn(merchant) ||
            Regex("""\bgoogle\b""", RegexOption.IGNORE_CASE).containsMatchIn(body)
        ) {
            return when {
                merchant.contains("play") || body.contains("google play") || body.contains("youtube") ->
                    CategorizationResult(Category.ENTERTAINMENT, "Context: Google Play / YouTube")
                merchant.contains("cloud") || merchant.contains("workspace") ||
                body.contains("google cloud") ->
                    CategorizationResult(Category.BILLS_UTILITIES, "Context: Google Cloud / Workspace")
                body.contains("google india digital") ->
                    CategorizationResult(Category.BILLS_UTILITIES, "Context: Google India Digital Services")
                else ->
                    CategorizationResult(Category.BILLS_UTILITIES, "Context: Google Digital Services")
            }
        }

        // Apple disambiguation
        // FIX: Added "tv", "tv+", "apple.com" to ENTERTAINMENT branch so Apple TV+ subscriptions
        //      no longer fall through to SHOPPING.
        if (Regex("""\bapple\b""", RegexOption.IGNORE_CASE).containsMatchIn(merchant) ||
            Regex("""\bapple\b""", RegexOption.IGNORE_CASE).containsMatchIn(body)
        ) {
            return when {
                body.contains("services") || body.contains("music") || body.contains("icloud") ||
                body.contains("app store") || body.contains("itunes") || body.contains("tv+") ||
                body.contains("apple tv") || body.contains("apple.com") ||
                merchant.contains("tv") || merchant.contains("music") ->
                    CategorizationResult(Category.ENTERTAINMENT, "Context: Apple Digital Subscription")
                else ->
                    CategorizationResult(Category.SHOPPING, "Merchant: Apple Store / Hardware")
            }
        }

        // 6. UPI VPA Decomposition & Token Matching
        // E.g. "swiggy.rzp@icici", "user.blinkit@okaxis", "tatapower@billdesk", "hpcl.retail@icici"
        if (merchant.contains("@")) {
            val vpaHandle = merchant.substringBefore("@")
                .replace(Regex("""(?i)\.(?:rzp|payu|billdesk|ccavenue|pos|online|store|ecom|merchant|upi|app|retail|corp|ind|pvtltd)"""), "")
            val vpaTokens = vpaHandle.split(Regex("""[.\-_]""")).filter { it.length >= 3 }

            for (token in vpaTokens) {
                val tokenMatch = matchKeywords(token)
                if (tokenMatch != null) {
                    return CategorizationResult(tokenMatch.first, "UPI VPA handle matched: '${tokenMatch.second}' in '$rawMerchant'")
                }
            }
        }

        // 7. Match merchant name directly against keyword dictionary.
        // First try the raw merchant, then try MerchantNormalizer candidates so that
        // gateway-polluted names like "ECOM*SWIGGY_ORDER_4829173" → "SWIGGY" still hit.
        if (merchant.isNotEmpty()) {
            val merchantMatch = matchKeywords(merchant)
            if (merchantMatch != null) {
                return CategorizationResult(merchantMatch.first, "Merchant keyword matched: '${merchantMatch.second}'")
            }

            // 7b. Normalized merchant candidates (strips order IDs, gateway prefixes, numeric suffixes)
            val normalizedCandidates = MerchantNormalizer.candidates(rawMerchant)
            for (candidate in normalizedCandidates) {
                if (candidate.lowercase() == merchant) continue  // already tried
                val normMatch = matchKeywords(candidate.lowercase())
                if (normMatch != null) {
                    return CategorizationResult(normMatch.first, "Normalized merchant '${candidate}' keyword matched: '${normMatch.second}'")
                }
            }
        }

        // 8. Match message body keywords with word boundaries
        val bodyMatch = matchKeywords(body)
        if (bodyMatch != null) {
            return CategorizationResult(bodyMatch.first, "Body keyword matched: '${bodyMatch.second}'")
        }

        // 9. P2P Transfer pattern detection
        val isP2pPattern = body.contains("sent via upi to") ||
                body.contains("transferred to") ||
                body.contains("upi/p2a/") ||
                body.contains("sent to vpa") ||
                (body.contains("sent via upi") && body.contains("to ")) ||
                (body.contains("debited") && body.contains("by transfer of") && !body.contains("at ")) ||
                Regex("""sent\s+(?:rs\.?|inr)\s*[0-9,.]+\s+(?:from\s+.*?a/c.*?)?to\s+""", RegexOption.IGNORE_CASE).containsMatchIn(body)

        if (isP2pPattern) {
            return CategorizationResult(Category.TRANSFERS, "Peer-to-Peer Transfer: '${rawMerchant.ifBlank { "Personal Transfer" }}'")
        }

        // 9b. YES Bank Credit Card @UPI_Name human format detection
        if (body.contains("@upi_", ignoreCase = true) || merchant.startsWith("upi_", ignoreCase = true)) {
            val cleanM = rawMerchant.replace(Regex("""(?i)^upi_"""), "").trim()
            if (cleanM.matches(Regex("""^[A-Za-z\s.]+$""")) && cleanM.split(Regex("""\s+""")).size in 2..3) {
                return CategorizationResult(Category.TRANSFERS, "Peer-to-Peer Transfer: '$cleanM'")
            }
        }

        // 10. Tier-3 Fallback: Lightweight On-Device Machine Learning (ML) Inference
        // FIX: Raised confidence threshold from 0.40 to 0.55 for general categories.
        //      OTHERS is never returned by ML (it's already the fallback), so this only fires
        //      when the model is reasonably confident.
        val mlText = if (merchant.isNotBlank()) rawMerchant else messageBody
        val mlPrediction = com.expensemanager.app.ml.OnDeviceMerchantClassifier.predict(mlText)
        if (mlPrediction != null && mlPrediction.category != Category.OTHERS) {
            // Use category-aware thresholds: lower for well-defined categories, higher for ambiguous ones.
            // These are calibrated against the on-device model's typical peak softmax probability
            // (~0.40–0.65 for a confident 14-class prediction on short merchant strings).
            val threshold = when (mlPrediction.category) {
                Category.FOOD, Category.GROCERIES, Category.TRANSPORT,
                Category.HEALTHCARE, Category.ENTERTAINMENT -> 0.42f
                Category.SHOPPING, Category.BILLS_UTILITIES -> 0.48f
                Category.INVESTMENT, Category.EDUCATION, Category.PERSONAL -> 0.50f
                else -> 0.55f
            }
            if (mlPrediction.confidence >= threshold) {
                return CategorizationResult(
                    mlPrediction.category,
                    "On-Device ML Model (${(mlPrediction.confidence * 100).toInt()}% confidence, threshold ${(threshold * 100).toInt()}%)"
                )
            }
        }

        // 11. Also try ML on the normalized merchant name if it differs from raw
        if (merchant.isNotBlank()) {
            val normalized = MerchantNormalizer.normalize(rawMerchant)
            if (normalized != null && normalized.lowercase() != merchant) {
                val mlNormPrediction = com.expensemanager.app.ml.OnDeviceMerchantClassifier.predict(normalized)
                if (mlNormPrediction != null && mlNormPrediction.category != Category.OTHERS) {
                    val threshold = when (mlNormPrediction.category) {
                        Category.FOOD, Category.GROCERIES, Category.TRANSPORT,
                        Category.HEALTHCARE, Category.ENTERTAINMENT -> 0.52f
                        else -> 0.62f
                    }
                    if (mlNormPrediction.confidence >= threshold) {
                        return CategorizationResult(
                            mlNormPrediction.category,
                            "On-Device ML (normalized '${normalized}', ${(mlNormPrediction.confidence * 100).toInt()}% confidence)"
                        )
                    }
                }
            }
        }

        // 12. Amount + time-of-day prior — last-resort tiebreaker before OTHERS.
        // Only applied when amount > 0 and currency is INR (priors are calibrated for INR).
        if (amount > 0.0) {
            val prior = AmountPrior.suggest(amount, currency, transactionType, timestampMs)
            if (prior != null && prior.category != Category.OTHERS) {
                return CategorizationResult(prior.category, prior.reason)
            }
        }

        return CategorizationResult(Category.OTHERS, "Default Fallback")
    }

    private fun matchKeywords(text: String): Pair<Category, String>? {
        for ((category, keywords) in KEYWORD_CATEGORY_MAP) {
            val matchedKeyword = keywords.firstOrNull { keyword ->
                val escaped = Regex.escape(keyword)
                if (keyword.first().isLetterOrDigit() && keyword.last().isLetterOrDigit()) {
                    Regex("""\b$escaped\b""", RegexOption.IGNORE_CASE).containsMatchIn(text)
                } else {
                    Regex("""(?i)(?:^|\s|[^\w])$escaped(?:$|\s|[^\w])""").containsMatchIn(text)
                }
            }
            if (matchedKeyword != null) {
                return Pair(category, matchedKeyword)
            }
        }
        return null
    }

    fun classify(
        merchantOrVpa: String?,
        messageBody: String,
        transactionType: TransactionType,
        userRules: List<MerchantRule> = emptyList(),
        amount: Double = 0.0,
        currency: String = "INR",
        timestampMs: Long = System.currentTimeMillis()
    ): Category {
        return classifyWithReason(merchantOrVpa, messageBody, transactionType, userRules, amount, currency, timestampMs).category
    }
}
