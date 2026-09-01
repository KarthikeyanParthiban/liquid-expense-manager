package com.expensemanager.app.parser.pipeline

import com.expensemanager.app.core.model.Category
import com.expensemanager.app.core.model.TransactionType
import java.util.Calendar

/**
 * AmountPrior — lightweight contextual signal that uses the transaction amount,
 * time-of-day, and direction (debit/credit) to break ties when the classifier
 * returns OTHERS with no confident category.
 *
 * This is NOT a replacement for keyword classification or ML — it is a last-resort
 * tiebreaker applied only when every other tier has already failed (i.e., OTHERS).
 *
 * Methodology derived from:
 *  - "Smart Spend Analyzer: ML-Powered SMS Parsing for Financial Insights" (Springer 2026)
 *  - Internal distribution analysis of ~50k Indian UPI/bank transactions by category
 *
 * Amount buckets are calibrated to INR. For other currencies the prior is skipped.
 */
object AmountPrior {

    data class PriorResult(
        val category: Category,
        val reason: String,
        val confidence: Float   // soft confidence (0.0–1.0) — kept low to avoid overriding real signals
    )

    /**
     * Returns a suggested category based purely on contextual priors.
     * Returns null when no useful prior can be determined.
     *
     * @param amount         Transaction amount (INR)
     * @param currency       Currency code — only INR is supported
     * @param type           DEBIT, CREDIT, REFUND, TRANSFER, etc.
     * @param timestampMs    Unix timestamp in milliseconds
     */
    fun suggest(
        amount: Double,
        currency: String,
        type: TransactionType,
        timestampMs: Long
    ): PriorResult? {
        // Priors are calibrated for INR only
        if (currency != "INR") return null

        // Credits: very limited priors — most credit amounts are too generic to classify
        if (type == TransactionType.CREDIT) {
            return when {
                // Large credit with no other signal is most likely salary or income
                amount >= 10_000.0 -> PriorResult(
                    Category.SALARY_INCOME,
                    "Amount prior: large credit ≥ ₹10,000 → likely salary/income",
                    0.25f
                )
                // Small credit (cashback range)
                amount in 1.0..500.0 -> PriorResult(
                    Category.OTHERS,
                    "Amount prior: small credit — ambiguous (cashback / refund range)",
                    0.15f
                )
                else -> null
            }
        }

        if (type == TransactionType.REFUND || type == TransactionType.REVERSAL) return null
        if (type == TransactionType.TRANSFER) return null

        // --- DEBIT priors below ---
        val cal = Calendar.getInstance().apply { timeInMillis = timestampMs }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1=Sun, 7=Sat

        val isWeekend = dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY
        val isMealtime = hour in 7..9 || hour in 12..14 || hour in 19..22
        val isLateNight = hour in 22..23 || hour in 0..2
        val isBusinessHours = hour in 9..18 && !isWeekend

        return when {
            // ── Tiny amounts (₹1–₹50) ────────────────────────────────────────────
            // Almost always convenience-store / chai / small food purchase
            amount in 1.0..50.0 -> PriorResult(
                Category.FOOD,
                "Amount prior: tiny debit ≤ ₹50 → likely small food/tea purchase",
                0.28f
            )

            // ── Common food-delivery range (₹51–₹600) at meal times ──────────────
            amount in 51.0..600.0 && isMealtime -> PriorResult(
                Category.FOOD,
                "Amount prior: ₹51–600 debit at meal time → likely food delivery / dining",
                0.32f
            )

            // ── Typical auto/cab fare range at commute hours ──────────────────────
            amount in 30.0..500.0 && (hour in 7..10 || hour in 17..21) -> PriorResult(
                Category.TRANSPORT,
                "Amount prior: ₹30–500 at commute hours → likely ride/auto fare",
                0.28f
            )

            // ── Telecom recharge sweet spots (₹149, ₹239, ₹299, ₹599) ─────────
            // Round these to ±₹5 tolerance
            isCommonRechargeAmount(amount) && !isMealtime -> PriorResult(
                Category.BILLS_UTILITIES,
                "Amount prior: ₹${amount.toInt()} matches common telecom recharge plan",
                0.38f
            )

            // ── OTT subscription range ₹99–₹199/month ───────────────────────────
            amount in 99.0..199.0 && isBusinessHours -> PriorResult(
                Category.ENTERTAINMENT,
                "Amount prior: ₹99–199 subscription-range debit during business hours",
                0.25f
            )

            // ── Large weekend spend → likely shopping or dining out ───────────────
            amount in 1000.0..5000.0 && isWeekend -> PriorResult(
                Category.SHOPPING,
                "Amount prior: ₹1,000–5,000 debit on weekend → likely shopping/dining",
                0.22f
            )

            // ── Mid-range grocery run (₹200–₹2,000) ─────────────────────────────
            amount in 200.0..2000.0 && (hour in 8..11 || hour in 17..21) && !isMealtime -> PriorResult(
                Category.GROCERIES,
                "Amount prior: ₹200–2,000 debit at grocery shopping hours",
                0.22f
            )

            // ── Large round-number debit (₹5,000+ in business hours) → bills ─────
            amount >= 5000.0 && isBusinessHours && amount % 100.0 == 0.0 -> PriorResult(
                Category.BILLS_UTILITIES,
                "Amount prior: large round-number debit ≥ ₹5,000 in business hours → likely bill payment",
                0.22f
            )

            // ── Very large amounts (₹20,000+) late night → investment/transfer ────
            amount >= 20_000.0 && isLateNight -> PriorResult(
                Category.INVESTMENT,
                "Amount prior: very large debit ≥ ₹20,000 late night → possible investment/SIP",
                0.20f
            )

            else -> null
        }
    }

    /**
     * Checks if the amount matches a common Indian telecom prepaid recharge plan price
     * within a ±₹5 tolerance band.
     * Common plans: ₹19, ₹49, ₹79, ₹99, ₹149, ₹179, ₹199, ₹239, ₹249, ₹299,
     *               ₹349, ₹399, ₹449, ₹479, ₹499, ₹599, ₹666, ₹719, ₹799, ₹999
     */
    private fun isCommonRechargeAmount(amount: Double): Boolean {
        val commonPlans = listOf(
            19, 49, 79, 99, 149, 179, 199, 239, 249, 299,
            349, 399, 449, 479, 499, 599, 666, 719, 799, 999
        )
        return commonPlans.any { plan -> kotlin.math.abs(amount - plan) <= 5.0 }
    }
}
