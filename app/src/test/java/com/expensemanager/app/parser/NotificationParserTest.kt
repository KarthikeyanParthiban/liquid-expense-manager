package com.expensemanager.app.parser

import com.expensemanager.app.core.model.Category
import com.expensemanager.app.core.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class NotificationParserTest {

    @Test
    fun `test Google Pay payment notification parsing`() {
        val packageName = "com.google.android.apps.nbu.paisa.user"
        val title = "Google Pay"
        val text = "Paid ₹450.00 to Swiggy using HDFC Bank •••• 7011. UPI Ref: 423456789012"
        val timestamp = 1787721200000L

        val result = NotificationParser.parse(packageName, title, text, timestamp)

        assertNotNull(result)
        assertEquals(450.00, result!!.amount, 0.01)
        assertEquals("Swiggy", result.merchant)
        assertEquals(Category.FOOD, result.category)
        assertEquals("7011", result.accountMask)
        assertEquals("423456789012", result.referenceId)
        assertEquals(TransactionType.DEBIT, result.type)
    }

    @Test
    fun `test PhonePe grocery notification parsing`() {
        val packageName = "com.phonepe.app"
        val title = "PhonePe"
        val text = "Payment of ₹896.00 to Blinkit successful. Ref No 987654321098"
        val timestamp = 1787721200000L

        val result = NotificationParser.parse(packageName, title, text, timestamp)

        assertNotNull(result)
        assertEquals(896.00, result!!.amount, 0.01)
        assertEquals("Blinkit", result.merchant)
        assertEquals(Category.GROCERIES, result.category)
        assertEquals("987654321098", result.referenceId)
        assertEquals(TransactionType.DEBIT, result.type)
    }

    @Test
    fun `test Paytm dining notification parsing`() {
        val packageName = "net.one97.paytm"
        val title = "Paytm"
        val text = "Paid ₹350 at McDonald's from Paytm Payments Bank. Txn ID: 556677889900"
        val timestamp = 1787721200000L

        val result = NotificationParser.parse(packageName, title, text, timestamp)

        assertNotNull(result)
        assertEquals(350.00, result!!.amount, 0.01)
        assertEquals("McDonald's", result.merchant)
        assertEquals(Category.FOOD, result.category)
        assertEquals("556677889900", result.referenceId)
    }

    @Test
    fun `test CRED credit card settlement notification parsing`() {
        val packageName = "com.dreamplug.androidapp"
        val title = "CRED"
        val text = "₹15,000 paid towards HDFC Bank Credit Card ending 1006. Ref: CRED998877"
        val timestamp = 1787721200000L

        val result = NotificationParser.parse(packageName, title, text, timestamp)

        assertNotNull(result)
        assertEquals(15000.00, result!!.amount, 0.01)
        assertEquals("1006", result.accountMask)
        assertEquals(TransactionType.CARD_SETTLEMENT, result.type)
        assertEquals(Category.TRANSFERS, result.category)
    }

    @Test
    fun `test HDFC Bank mobile app notification with balance parsing`() {
        val packageName = "com.snapwork.hdfc"
        val title = "HDFC Bank Alert"
        val text = "Alert: Rs 1,450.00 debited from A/c XX7011 for Swiggy. Avl Bal: Rs 86,698.00. Ref: 616868340519"
        val timestamp = 1787721200000L

        val result = NotificationParser.parse(packageName, title, text, timestamp)

        assertNotNull(result)
        assertEquals(1450.00, result!!.amount, 0.01)
        assertEquals("HDFC Bank", result.bankName)
        assertEquals("7011", result.accountMask)
        assertEquals(86698.00, result.balanceAfter ?: 0.0, 0.01)
        assertEquals(Category.FOOD, result.category)
        assertEquals("616868340519", result.referenceId)
    }

    @Test
    fun `test ICICI iMobile notification parsing`() {
        val packageName = "com.csam.icici.bank.imobile"
        val title = "iMobile Pay"
        val text = "Transaction Alert: INR 2,000.00 spent on ICICI Bank Card XX1044 at Amazon Retail"
        val timestamp = 1787721200000L

        val result = NotificationParser.parse(packageName, title, text, timestamp)

        assertNotNull(result)
        assertEquals(2000.00, result!!.amount, 0.01)
        assertEquals("ICICI Bank", result.bankName)
        assertEquals("1044", result.accountMask)
        assertEquals("Amazon Retail", result.merchant)
        assertEquals(Category.SHOPPING, result.category)
    }

    @Test
    fun `test Jupiter and Fi Money notification parsing`() {
        val jupiterResult = NotificationParser.parse(
            "money.jupiter",
            "Jupiter",
            "Spent ₹240.00 at Starbucks with Jupiter card ending 3344",
            1787721200000L
        )
        assertNotNull(jupiterResult)
        assertEquals(240.00, jupiterResult!!.amount, 0.01)
        assertEquals("Starbucks", jupiterResult.merchant)
        assertEquals(Category.FOOD, jupiterResult.category)
        assertEquals("3344", jupiterResult.accountMask)

        val fiResult = NotificationParser.parse(
            "co.epifi.app",
            "Fi Money",
            "Paid ₹450 to Uber via Fi",
            1787721200000L
        )
        assertNotNull(fiResult)
        assertEquals(450.00, fiResult!!.amount, 0.01)
        assertEquals("Uber", fiResult.merchant)
        assertEquals(Category.TRANSPORT, fiResult.category)
    }

    @Test
    fun `test Non-transactional OTP and marketing notifications are ignored`() {
        val otpResult = NotificationParser.parse(
            "com.google.android.apps.nbu.paisa.user",
            "Google Pay",
            "Your Google Pay verification code is 492810. Do not share this OTP with anyone.",
            1787721200000L
        )
        assertNull("OTP notifications must not be parsed as transactions", otpResult)

        val promoResult = NotificationParser.parse(
            "com.phonepe.app",
            "PhonePe",
            "Win up to ₹1,000 cashback! Scratch card unlocked on PhonePe.",
            1787721200000L
        )
        assertNull("Promotional marketing notifications must be ignored", promoResult)
    }
}
