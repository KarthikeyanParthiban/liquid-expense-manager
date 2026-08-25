package com.expensemanager.app.parser

import com.expensemanager.app.core.model.AccountType
import com.expensemanager.app.core.model.Category
import com.expensemanager.app.core.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TransferAndCreditCardAccountingTest {

    @Test
    fun `test credit card purchase is categorized as expense and NOT excluded from budget`() {
        val sender = "AX-YESBNK-S"
        val body = "INR 2,450.00 spent on YES BANK Card X1006 @ZARA on 24-08-2026. Avl Lmt INR 70,000.00"
        val timestamp = 1724490000000L

        val result = SmsParser.parse(sender, body, timestamp)
        assertNotNull(result)
        assertEquals(2450.00, result!!.amount, 0.01)
        assertEquals(TransactionType.DEBIT, result.type)
        assertEquals(Category.SHOPPING, result.category)
        assertEquals(false, result.isExcludedFromBudget)
    }

    @Test
    fun `test credit card payment via CRED is categorized as TRANSFER and excluded from budget`() {
        val sender = "AD-HDFCBK-S"
        val body = "Rs. 15,000.00 debited from A/c XX7011 on 24-AUG-26 to CRED Club. Ref 623639338712."
        val timestamp = 1724490000000L

        val result = SmsParser.parse(sender, body, timestamp)
        assertNotNull(result)
        assertEquals(15000.00, result!!.amount, 0.01)
        assertEquals(Category.TRANSFERS, result.category)
        assertTrue(result.isExcludedFromBudget)
    }

    @Test
    fun `test credit card settlement SMS confirmation is categorized as CARD_SETTLEMENT and excluded from budget`() {
        val sender = "AD-HDFCBK-S"
        val body = "Payment of Rs. 15000.00 received towards your credit card ending with 2942 on 24-8-2026."
        val timestamp = 1724490000000L

        val result = SmsParser.parse(sender, body, timestamp)
        assertNotNull(result)
        assertEquals(15000.00, result!!.amount, 0.01)
        assertEquals(TransactionType.CARD_SETTLEMENT, result.type)
        assertEquals(Category.TRANSFERS, result.category)
        assertTrue(result.isExcludedFromBudget)
    }

    @Test
    fun `test bank to bank self transfer is categorized as TRANSFER and excluded from budget`() {
        val sender = "VK-AXISBK"
        val body = "Rs 10,000.00 transferred from A/c XX8831 to self transfer SBI A/c XX9812 via IMPS Ref 991201."
        val timestamp = 1724490000000L

        val result = SmsParser.parse(sender, body, timestamp)
        assertNotNull(result)
        assertEquals(10000.00, result!!.amount, 0.01)
        assertEquals(Category.TRANSFERS, result.category)
        assertTrue(result.isExcludedFromBudget)
    }

    @Test
    fun `test ATM cash withdrawal is categorized as CASH_WITHDRAWAL and excluded from double-counted budget`() {
        val sender = "AD-SBIINB"
        val body = "Cash wdl of Rs. 2,000.00 made at SBI ATM from A/c XX9812 on 24Aug26. Avl Bal Rs 5,000.00"
        val timestamp = 1724490000000L

        val result = SmsParser.parse(sender, body, timestamp)
        assertNotNull(result)
        assertEquals(2000.00, result!!.amount, 0.01)
        assertEquals(TransactionType.CASH_WITHDRAWAL, result.type)
        assertEquals(Category.TRANSFERS, result.category)
        assertTrue(result.isExcludedFromBudget)
    }
}
