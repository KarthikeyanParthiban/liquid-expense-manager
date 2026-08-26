package com.expensemanager.app.parser

import com.expensemanager.app.core.util.CurrencyFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class MultiCurrencyParsingTest {

    @Test
    fun `test USD international transaction parsing`() {
        val sms = "Spent USD 21.20 Axis Bank Card no. XX9117 18-08-26 13:56:24 IST DeepSeek Avl Limit: INR 71798.57"
        val result = SmsParser.parse("AXISBK", sms, 1787721200000L)

        assertNotNull(result)
        assertEquals(21.20, result!!.amount, 0.01)
        assertEquals("USD", result.currency)
        assertEquals("DeepSeek", result.merchant)
        assertEquals("$21.20", CurrencyFormatter.format(result.amount, result.currency))
    }

    @Test
    fun `test EUR European transaction parsing`() {
        val sms = "Spent EUR 45.50 on ICICI Bank Credit Card XX1044 at Lufthansa Airlines. Avl Lmt: INR 95,000.00"
        val result = SmsParser.parse("ICICIB", sms, 1787721200000L)

        assertNotNull(result)
        assertEquals(45.50, result!!.amount, 0.01)
        assertEquals("EUR", result.currency)
        assertEquals("€45.50", CurrencyFormatter.format(result.amount, result.currency))
    }

    @Test
    fun `test GBP and AED currency extraction`() {
        val gbpSms = "Paid GBP 15.00 on HDFC Bank Card XX7011 at London Underground"
        val gbpResult = BankPatterns.extractCurrencyAndAmount(gbpSms)
        assertNotNull(gbpResult)
        assertEquals(15.00, gbpResult!!.first, 0.01)
        assertEquals("GBP", gbpResult.second)

        val aedSms = "Spent AED 120.00 at Dubai Duty Free using YES Bank Card X1006"
        val aedResult = BankPatterns.extractCurrencyAndAmount(aedSms)
        assertNotNull(aedResult)
        assertEquals(120.00, aedResult!!.first, 0.01)
        assertEquals("AED", aedResult.second)
    }

    @Test
    fun `test INR domestic transaction parsing`() {
        val sms = "INR 192.00 spent on YES BANK Card X1006 @UPI_KASIRAJA N 26-08-2026. Avl Lmt INR 72,701.27"
        val result = SmsParser.parse("YESBNK", sms, 1787721200000L)

        assertNotNull(result)
        assertEquals(192.00, result!!.amount, 0.01)
        assertEquals("INR", result.currency)
        assertEquals("₹192.00", CurrencyFormatter.format(result.amount, result.currency))
    }
}
