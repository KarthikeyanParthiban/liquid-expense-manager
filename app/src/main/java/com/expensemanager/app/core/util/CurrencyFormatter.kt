package com.expensemanager.app.core.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object CurrencyFormatter {
    private val standardSymbols = DecimalFormatSymbols(Locale.US)

    fun getCurrencySymbol(currency: String): String {
        return when (currency.uppercase().trim()) {
            "USD", "$" -> "$"
            "EUR", "€" -> "€"
            "GBP", "£" -> "£"
            "AED" -> "AED "
            "SGD", "S$" -> "S$"
            "CAD", "C$" -> "C$"
            "AUD", "A$" -> "A$"
            "JPY", "¥" -> "¥"
            else -> "₹"
        }
    }

    fun format(amount: Double, currency: String = "INR", showSymbol: Boolean = true): String {
        val curr = currency.uppercase().trim()
        val symbol = if (showSymbol) getCurrencySymbol(curr) else ""

        return try {
            when (curr) {
                "INR", "RS" -> {
                    val df = DecimalFormat("#,##,##0.00", standardSymbols)
                    val formatted = df.format(amount)
                    if (showSymbol) "$symbol$formatted" else formatted
                }
                "JPY" -> {
                    val df = DecimalFormat("#,##0", standardSymbols)
                    val formatted = df.format(amount)
                    if (showSymbol) "$symbol$formatted" else formatted
                }
                else -> {
                    val df = DecimalFormat("#,##0.00", standardSymbols)
                    val formatted = df.format(amount)
                    if (showSymbol) "$symbol$formatted" else formatted
                }
            }
        } catch (e: Exception) {
            val df = DecimalFormat("#,##0.00", standardSymbols)
            if (showSymbol) "$symbol${df.format(amount)}" else df.format(amount)
        }
    }

    fun formatSigned(amount: Double, isCredit: Boolean, currency: String = "INR"): String {
        val sign = if (isCredit) "+ " else "- "
        return sign + format(amount, currency)
    }
}
