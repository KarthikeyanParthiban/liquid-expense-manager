package com.expensemanager.app.core.util

import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale

object CurrencyFormatter {
    private val indianLocale = Locale("en", "IN")
    private val formatter = NumberFormat.getCurrencyInstance(indianLocale).apply {
        maximumFractionDigits = 2
        minimumFractionDigits = 0
    }

    fun format(amount: Double, currency: String = "INR", showSymbol: Boolean = true): String {
        return try {
            val formatted = formatter.format(amount)
            if (!showSymbol) {
                formatted.replace("₹", "").trim()
            } else {
                formatted
            }
        } catch (e: Exception) {
            val df = DecimalFormat("#,##,##0.##")
            if (showSymbol) "₹${df.format(amount)}" else df.format(amount)
        }
    }

    fun formatSigned(amount: Double, isCredit: Boolean): String {
        val sign = if (isCredit) "+ " else "- "
        return sign + format(amount)
    }
}
