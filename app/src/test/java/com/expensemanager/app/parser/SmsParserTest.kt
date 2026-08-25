package com.expensemanager.app.parser

import com.expensemanager.app.core.model.Category
import com.expensemanager.app.core.model.MerchantRule
import com.expensemanager.app.core.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsParserTest {

    @Test
    fun `test parsing HDFC Bank UPI debit SMS`() {
        val sender = "VM-HDFCBK"
        val body = "Rs. 249.00 debited from A/c **4582 on 24-AUG-26 to SWIGGY UPI/P2M/629104819201/Swiggy. Avl Bal: INR 12,450.75"
        val timestamp = 1724490000000L

        val result = SmsParser.parse(sender, body, timestamp)

        assertNotNull(result)
        assertEquals(249.00, result!!.amount, 0.01)
        assertEquals(TransactionType.DEBIT, result.type)
        assertEquals(Category.FOOD, result.category)
        assertEquals("HDFC Bank", result.bankName)
        assertEquals("XX4582", result.accountMask)
        assertEquals(12450.75, result.balanceAfter ?: 0.0, 0.01)
        assertEquals("629104819201", result.referenceId)
    }

    @Test
    fun `test parsing SBI debit SMS`() {
        val sender = "AD-SBIINB"
        val body = "Dear SBI User, A/C 9812 has a debit by transfer of Rs 1,500.00 on 24Aug26. Ref No 628190123981. AVL BAL: Rs 4,200.00"
        val timestamp = 1724490000000L

        val result = SmsParser.parse(sender, body, timestamp)

        assertNotNull(result)
        assertEquals(1500.00, result!!.amount, 0.01)
        assertEquals(TransactionType.DEBIT, result.type)
        assertEquals("SBI", result.bankName)
        assertEquals("XX9812", result.accountMask)
        assertEquals(4200.00, result.balanceAfter ?: 0.0, 0.01)
        assertEquals("628190123981", result.referenceId)
    }

    @Test
    fun `test parsing ICICI Credit Card spent SMS`() {
        val sender = "AX-ICICIB"
        val body = "Spent INR 3,999.00 on ICICI Bank Card ending 1044 at AMAZON RETAIL on 24-Aug-26. Avl Lmt: INR 85,000.00"
        val timestamp = 1724490000000L

        val result = SmsParser.parse(sender, body, timestamp)

        assertNotNull(result)
        assertEquals(3999.00, result!!.amount, 0.01)
        assertEquals(TransactionType.DEBIT, result.type)
        assertEquals(Category.SHOPPING, result.category)
        assertEquals("ICICI Bank", result.bankName)
        assertEquals("XX1044", result.accountMask)
    }

    @Test
    fun `test parsing Salary credit SMS`() {
        val sender = "VK-AXISBK"
        val body = "Your A/c no. XX8831 is credited for Rs 75,000.00 on 01-Aug-26 by Salary NEFT AXIS9921048. Total Bal: Rs 82,300.00"
        val timestamp = 1724490000000L

        val result = SmsParser.parse(sender, body, timestamp)

        assertNotNull(result)
        assertEquals(75000.00, result!!.amount, 0.01)
        assertEquals(TransactionType.CREDIT, result.type)
        assertEquals(Category.SALARY_INCOME, result.category)
        assertEquals("Axis Bank", result.bankName)
        assertEquals("XX8831", result.accountMask)
        assertEquals(82300.00, result.balanceAfter ?: 0.0, 0.01)
    }

    @Test
    fun `test user custom merchant rules take precedence`() {
        val sender = "VM-HDFCBK"
        val body = "Rs. 850.00 debited from A/c XX1234 on 24-AUG-26 to QuickFix Tech. Avl Bal Rs 5,000.00"
        val timestamp = 1724490000000L

        // Default categorization without rule
        val defaultResult = SmsParser.parse(sender, body, timestamp)
        assertNotNull(defaultResult)

        // Custom rule mapping "QuickFix" to EDUCATION
        val customRule = listOf(MerchantRule("QuickFix", Category.EDUCATION))
        val customResult = SmsParser.parse(sender, body, timestamp, customRule)

        assertNotNull(customResult)
        assertEquals(Category.EDUCATION, customResult!!.category)
    }

    @Test
    fun `test YES Bank Card UPI Blinkit SMS from real device`() {
        val sender = "AX-YESBNK-S"
        val body = "INR 896.00 spent on YES BANK Card X1006 @UPI_BLINKIT 25-08-2026 07:54:51 am. Avl Lmt INR 73,248.27. SMS BLKCC 1006 to 9840909000 if not you"
        val timestamp = 1787624693534L

        val result = SmsParser.parse(sender, body, timestamp)
        assertNotNull(result)
        assertEquals(896.00, result!!.amount, 0.01)
        assertEquals(TransactionType.DEBIT, result.type)
        assertEquals(Category.GROCERIES, result.category)
        assertEquals("BLINKIT", result.merchant)
        assertEquals("XX1006", result.accountMask)
        assertEquals(73248.27, result.balanceAfter ?: 0.0, 0.01)
    }

    @Test
    fun `test Axis Bank Card POS Zepto SMS from real device`() {
        val sender = "AD-AXISBK-S"
        val body = "Spent INR 74\nAxis Bank Card no. XX9117\n24-08-26 07:06:56 IST\nPHP*Zepto\nAvl Limit: INR 64827.01\nNot you? SMS BLOCK 9117 to 919951860002"
        val timestamp = 1787535418965L

        val result = SmsParser.parse(sender, body, timestamp)
        assertNotNull(result)
        assertEquals(74.00, result!!.amount, 0.01)
        assertEquals(TransactionType.DEBIT, result.type)
        assertEquals(Category.GROCERIES, result.category)
        assertEquals("Zepto", result.merchant)
        assertEquals("XX9117", result.accountMask)
        assertEquals(64827.01, result.balanceAfter ?: 0.0, 0.01)
    }

    @Test
    fun `test HDFC Bank multi-line UPI SMS from real device`() {
        val sender = "VM-HDFCBK-T"
        val body = "Sent Rs.350.90\nFrom HDFC Bank A/C *7011\nTo Google India Digital Serv\nOn 24/08/26\nRef 623639338712\nNot You?\nCall 18002586161/SMS BLOCK UPI to 7308080808"
        val timestamp = 1787561993550L

        val result = SmsParser.parse(sender, body, timestamp)
        assertNotNull(result)
        assertEquals(350.90, result!!.amount, 0.01)
        assertEquals(TransactionType.DEBIT, result.type)
        assertEquals("Google India Digital Serv", result.merchant)
        assertEquals("XX7011", result.accountMask)
        assertEquals("623639338712", result.referenceId)
    }

    @Test
    fun `test extracting real-time balance update from HDFC daily balance alert`() {
        val sender = "VD-HDFCBK-S"
        val body = "Available Bal in HDFC Bank A/c XX7011 as on yesterday:24-AUG-26 is INR 88,148.00. Cheques are subject to clearing.For updated A/C Bal dial 18002703333."
        val timestamp = 1787622722347L

        val balanceUpdate = SmsParser.extractBalanceUpdate(sender, body, timestamp)
        assertNotNull(balanceUpdate)
        assertEquals("HDFC_Bank_XX7011", balanceUpdate!!.accountId)
        assertEquals("HDFC Bank", balanceUpdate.bankName)
        assertEquals("XX7011", balanceUpdate.accountMask)
        assertEquals(88148.00, balanceUpdate.balance, 0.01)
    }

    @Test
    fun `test extracting credit limit balance from YES Bank Card SMS`() {
        val sender = "AX-YESBNK-S"
        val body = "INR 896.00 spent on YES BANK Card X1006 @UPI_BLINKIT 25-08-2026 07:54:51 am. Avl Lmt INR 73,248.27. SMS BLKCC 1006 to 9840909000 if not you"
        val timestamp = 1787624693534L

        val balanceUpdate = SmsParser.extractBalanceUpdate(sender, body, timestamp)
        assertNotNull(balanceUpdate)
        assertEquals("Yes_Bank_XX1006", balanceUpdate!!.accountId)
        assertEquals("Yes Bank", balanceUpdate.bankName)
        assertEquals("XX1006", balanceUpdate.accountMask)
        assertEquals(73248.27, balanceUpdate.balance, 0.01)
    }

    @Test
    fun `test parsing Federal Bank UPI debit SMS`() {
        val sender = "VK-FEDBNK-S"
        val body = "Rs 3000.00 sent via UPI on 17-06-2026 at 08:59:07 to KARISHMA P.Ref:616868340519.Not you? Call 18004251199/SMS BLOCKUPI"
        val timestamp = 1782100000000L

        val result = SmsParser.parse(sender, body, timestamp)
        assertNotNull(result)
        assertEquals(3000.00, result!!.amount, 0.01)
        assertEquals(TransactionType.DEBIT, result.type)
        assertEquals("Federal Bank", result.bankName)
        assertEquals("616868340519", result.referenceId)
    }

    @Test
    fun `test parsing Kiwi cashback credit SMS`() {
        val sender = "VM-HDFCBK-S"
        val body = "Credit Alert!\nRs.268.25 credited to HDFC Bank A/c XX7011 on 08-08-26 from VPA kiwicashback@axisbank (UPI 989594742206)"
        val timestamp = 1786500000000L

        val result = SmsParser.parse(sender, body, timestamp)
        assertNotNull(result)
        assertEquals(268.25, result!!.amount, 0.01)
        assertEquals(TransactionType.CREDIT, result.type)
        assertEquals("HDFC Bank", result.bankName)
        assertEquals("XX7011", result.accountMask)
    }

    @Test
    fun `test parsing HDFC IMPS Received SMS at 455 PM`() {
        val sender = "VM-HDFCBK-S"
        val body = "Received!\nINR 100.00 in HDFC Bank A/c xx7011\nOn 25-08-26\nFor IMPS -GROWW INVEST TECH PR- 623716016220\nAvl bal INR 88,248.00"
        val timestamp = 1787657136000L

        val isFin = SmsClassifier.isFinancialSms(body)
        assertTrue("Should be financial SMS", isFin)

        val result = SmsParser.parse(sender, body, timestamp)
        assertNotNull("Result should not be null", result)
        assertEquals(100.00, result!!.amount, 0.01)
        assertEquals(TransactionType.CREDIT, result.type)
        assertEquals("HDFC Bank", result.bankName)
        assertEquals("XX7011", result.accountMask)
        assertEquals(88248.00, result.balanceAfter ?: 0.0, 0.01)
    }
}
