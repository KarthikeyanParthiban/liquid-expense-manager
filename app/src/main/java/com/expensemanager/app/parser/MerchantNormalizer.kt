package com.expensemanager.app.parser

/**
 * MerchantNormalizer — cleans raw merchant strings before classification.
 *
 * Problems it solves:
 *  - "SWIGGY_ORDER_4829173"  → "SWIGGY"
 *  - "ECOM*BLINKIT38291"     → "BLINKIT"
 *  - "PHP*HALDIRAMS_NOIDA"   → "HALDIRAMS"
 *  - "POS*ZOMATO-2847192"    → "ZOMATO"
 *  - "RAZORPAY*NETFLIX"      → "NETFLIX"
 *  - "BIL*AIRTEL_PREPAID"    → "AIRTEL"
 *  - "AMAZON SELLER SVCS"    → "AMAZON"
 *  - "PAYU*BOOKMYSHOW4829"   → "BOOKMYSHOW"
 *  - "HDFC BANK CREDIT CARD" → ""  (bank self-references → empty, skip classification)
 *  - "UPI-CR-1234567890"     → ""  (raw UPI ref → empty)
 *
 * The normalizer is applied BEFORE keyword matching and ML inference so that
 * long-tail and order-number-polluted merchant names get a fair chance at a
 * keyword hit.
 */
object MerchantNormalizer {

    // Gateway/payment processor prefixes that precede the real merchant name
    // e.g. "RAZORPAY*NETFLIX", "PAYU*BOOKMYSHOW", "PHP*ZOMATO", "POS*HALDIRAMS"
    private val GATEWAY_PREFIX_REGEX = Regex(
        """(?i)^(?:razorpay|payu|billdesk|ccavenue|cashfree|payumoney|instamojo|atom|zaakpay|citrus|itz|ebs|direcpay|hdfc\s*pg|sbi\s*pg|icici\s*pg|axis\s*pg|freecharge|mobikwik|airpay|nttdata|worldline|ingenico|[A-Z]{2,6})\s*[*_\-|/]\s*""",
        RegexOption.IGNORE_CASE
    )

    // Merchant class prefixes embedded in POS/ECOM strings
    // e.g. "PHP*HALDIRAMS", "PG*SWIGGY", "ECOM*BLINKIT", "BIL*AIRTEL", "IN*ZOMATO"
    private val POS_ECOM_PREFIX_REGEX = Regex(
        """(?i)^(?:PHP|PG|POS|ECOM|BIL|IN|SQ|NP)\s*[*_\-]\s*"""
    )

    // Trailing order/transaction IDs: alphanumeric noise after a separator
    // e.g. "SWIGGY_ORDER_4829173", "ZOMATO-2847192", "NETFLIX.COM/BILL"
    private val TRAILING_ID_REGEX = Regex(
        """(?i)[_\-\s/]*(?:ORDER|TXN|BILL|PAY|PAYMENT|SERVICES?|SVCS?|TECH|DIGITAL|ONLINE|INDIA|PVT|LTD|LLC)\s*[_\-\s/]*[A-Z0-9]{3,}$"""
    )

    // Pure numeric suffix: "SWIGGY12345", "BLINKIT38291"
    private val NUMERIC_SUFFIX_REGEX = Regex("""[0-9]{4,}$""")

    // Known noise merchant strings that are gateway/bank self-references, not real merchants
    private val NOISE_MERCHANTS = setOf(
        "upi", "neft", "imps", "rtgs", "nach", "mandate", "ecs",
        "credit card", "debit card", "bank account", "payment", "transfer",
        "auto debit", "standing instruction", "si", "emi",
        "upi cr", "upi dr", "upi ref", "upi-cr", "upi-dr"
    )

    // Terms that indicate a bank self-reference rather than a merchant
    private val BANK_SELF_REFERENCE_REGEX = Regex(
        """(?i)\b(?:hdfc\s*bank|icici\s*bank|axis\s*bank|sbi|kotak|yes\s*bank|pnb|bob|canara|idfc|indusind)\s+(?:credit\s*card|debit\s*card|bank|savings|account|a/c)\b"""
    )

    /**
     * Normalizes a raw merchant string to the cleanest possible form for classification.
     *
     * Returns null if the string is a gateway/bank self-reference with no useful merchant info.
     * Returns the normalized string otherwise (may be shorter/cleaner than the original).
     */
    fun normalize(raw: String?): String? {
        if (raw.isNullOrBlank()) return null

        var clean = raw.trim()

        // Reject bank self-references early
        if (BANK_SELF_REFERENCE_REGEX.containsMatchIn(clean)) return null

        // Reject known noise tokens
        if (clean.lowercase() in NOISE_MERCHANTS) return null
        if (clean.matches(Regex("""^[0-9A-Z\-_/]+$""")) && clean.length <= 8) return null  // pure codes

        // 1. Strip payment gateway prefix ("RAZORPAY*NETFLIX" → "NETFLIX")
        clean = GATEWAY_PREFIX_REGEX.replace(clean, "")

        // 2. Strip POS/ECOM class prefix ("PHP*HALDIRAMS" → "HALDIRAMS")
        clean = POS_ECOM_PREFIX_REGEX.replace(clean, "")

        // 3. Strip trailing noise words + IDs ("SWIGGY SERVICES 48291" → "SWIGGY")
        clean = TRAILING_ID_REGEX.replace(clean, "").trim()

        // 4. Strip trailing pure numeric suffixes ("BLINKIT38291" → "BLINKIT")
        clean = NUMERIC_SUFFIX_REGEX.replace(clean, "").trim()

        // 5. Normalize separators to spaces ("SWIGGY_FOOD" → "SWIGGY FOOD")
        clean = clean.replace(Regex("""[_\-|]+"""), " ").trim()

        // 6. Collapse whitespace
        clean = clean.replace(Regex("""\s{2,}"""), " ").trim()

        // 7. Final length / quality check
        if (clean.isBlank() || clean.length < 2) return null
        if (clean.matches(Regex("""^\d+$"""))) return null  // all digits

        return clean
    }

    /**
     * Returns a list of candidate merchant tokens to try for classification.
     * The first element is the most specific (full normalized name).
     * Subsequent elements are shorter prefix tokens for fallback matching.
     *
     * e.g. "SWIGGY INSTAMART ORDER" → ["SWIGGY INSTAMART", "SWIGGY"]
     */
    fun candidates(raw: String?): List<String> {
        val normalized = normalize(raw) ?: return emptyList()
        val candidates = mutableListOf(normalized)

        // Also add single-word tokens if the normalized form is multi-word
        val words = normalized.split(" ").filter { it.length >= 3 }
        if (words.size > 1) {
            // add bigram prefix
            candidates.add(words.take(2).joinToString(" "))
            // add first token alone as fallback
            candidates.add(words.first())
        }

        return candidates.distinct()
    }
}
