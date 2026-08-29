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

    @Test
    fun `test recipient VPA handle with okhdfcbank is not treated as HDFC Bank account`() {
        val sender = "AD-SBIINB"
        val body = "Dear SBI User, your A/c XX9812 has a debit of Rs. 450.00 on 24Aug26 to VPA merchant@okhdfcbank. Ref 123456. Avl Bal Rs 5000.00"
        val timestamp = 1787657136000L

        val result = SmsParser.parse(sender, body, timestamp)
        assertNotNull("Result should not be null", result)
        assertEquals(450.00, result!!.amount, 0.01)
        assertEquals(TransactionType.DEBIT, result.type)
        assertEquals("SBI", result.bankName) // NOT HDFC Bank!
        assertEquals("XX9812", result.accountMask)
    }

    @Test
    fun `test recipient VPA handle with icici in generic UPI SMS is not treated as ICICI Bank`() {
        val sender = "VM-UPIPAY"
        val body = "Paid Rs. 150.00 from Axis Bank A/c XX8831 to swiggy@icici. UTR 99887766."
        val timestamp = 1787657136000L

        val result = SmsParser.parse(sender, body, timestamp)
        assertNotNull("Result should not be null", result)
        assertEquals(150.00, result!!.amount, 0.01)
        assertEquals("Axis Bank", result.bankName) // NOT ICICI Bank!
        assertEquals("XX8831", result.accountMask)
    }

    @Test
    fun `test Paytm VPA handle in Google Pay notification is not treated as Paytm Wallet`() {
        val packageName = "com.google.android.apps.nbu.paisa.user"
        val title = "Google Pay"
        val text = "Paid ₹200 to fruitstall@paytm using HDFC Bank A/c ending 7011."
        val timestamp = 1787657136000L

        val result = NotificationParser.parse(packageName, title, text, timestamp)
        assertNotNull("Result should not be null", result)
        assertEquals(200.00, result!!.amount, 0.01)
        assertEquals("HDFC Bank", result.bankName) // NOT Paytm!
        assertEquals("7011", result.accountMask)
    }

    @Test
    fun `test recipient VPA with axisbank handle in SBI debit SMS is not treated as Axis Bank`() {
        val sender = "VK-SBIUPI"
        val body = "Rs. 500 debited from A/c XX4321 to VPA groceries@axisbank ref 998811."
        val timestamp = 1787657136000L

        val result = SmsParser.parse(sender, body, timestamp)
        assertNotNull("Result should not be null", result)
        assertEquals(500.00, result!!.amount, 0.01)
        assertEquals("SBI", result.bankName) // NOT Axis Bank!
        assertEquals("XX4321", result.accountMask)
    }

    @Test
    fun `test generic VPA @vpa in transaction body`() {
        val sender = "VK-CANBNK"
        val body = "INR 350.00 debited from Canara Bank A/c XX5678 to VPA test@vpa on 26-08-2026. Bal INR 12000.00"
        val timestamp = 1787657136000L

        val result = SmsParser.parse(sender, body, timestamp)
        assertNotNull("Result should not be null", result)
        assertEquals(350.00, result!!.amount, 0.01)
        assertEquals("Canara Bank", result.bankName) // NOT vpa!
        assertEquals("XX5678", result.accountMask)
    }

    @Test
    fun `test recipient VPA handle with yesbank or ybl`() {
        val sender = "AD-HDFCBK"
        val body = "Sent Rs. 120 to rent@ybl from HDFC Bank A/c XX7011."
        val timestamp = 1787657136000L

        val result = SmsParser.parse(sender, body, timestamp)
        assertNotNull("Result should not be null", result)
        assertEquals(120.00, result!!.amount, 0.01)
        assertEquals("HDFC Bank", result.bankName) // NOT Yes Bank!
        assertEquals("XX7011", result.accountMask)
    }

    @Test
    fun `test Kiwi credit card bill payment with hyphenated card mask`() {
        val sender = "JK-GKIWIP-S"
        val body = "Your YES Bank Credit Card XXXX-1006 bill of Rs. 28615.2 has been successfully paid via Kiwi."
        val timestamp = 1787657136000L

        val result = SmsParser.parse(sender, body, timestamp)
        assertNotNull("Result should not be null", result)
        assertEquals(28615.20, result!!.amount, 0.01)
        assertEquals("Yes Bank", result.bankName)
        assertEquals("XX1006", result.accountMask)
        assertTrue("Bill payment should be excluded from monthly expenses", result.isExcludedFromBudget)
    }

    @Test
    fun `test Indian Bank credit SMS with okaxis VPA handle`() {
        val sender = "BZ-INDBNK-S"
        val body = "Rs.2000.00 credited to a/c *2813 on 11/07/2026 by a/c linked to VPA adhithyavel11-1@okaxis (UPI Ref no 619262846741).Indian Bank"
        val timestamp = 1783765136000L

        val result = SmsParser.parse(sender, body, timestamp)
        assertNotNull("Result should not be null", result)
        assertEquals(2000.00, result!!.amount, 0.01)
        assertEquals(TransactionType.CREDIT, result.type)
        assertEquals("Indian Bank", result.bankName) // NOT Axis Bank!
        assertEquals("XX2813", result.accountMask)
        assertEquals("Indian_Bank_XX2813", result.accountId)
        assertEquals("619262846741", result.referenceId)
    }

    @Test
    fun `test Indian Bank credit SMS with okicici VPA handle`() {
        val sender = "BT-INDBNK-S"
        val body = "Rs.1000.00 credited to a/c *2813 on 18/06/2026 by a/c linked to VPA adhithyavel11-2@okicici (UPI Ref no 653525900686).Indian Bank"
        val timestamp = 1781765136000L

        val result = SmsParser.parse(sender, body, timestamp)
        assertNotNull("Result should not be null", result)
        assertEquals(1000.00, result!!.amount, 0.01)
        assertEquals(TransactionType.CREDIT, result.type)
        assertEquals("Indian Bank", result.bankName) // NOT ICICI Bank!
        assertEquals("XX2813", result.accountMask)
        assertEquals("Indian_Bank_XX2813", result.accountId)
        assertEquals("653525900686", result.referenceId)
    }
}

