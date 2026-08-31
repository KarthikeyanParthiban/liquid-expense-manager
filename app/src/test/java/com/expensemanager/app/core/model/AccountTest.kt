package com.expensemanager.app.core.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountTest {

    @Test
    fun `test account active when updated within 1 year`() {
        val now = System.currentTimeMillis()
        val recentAccount = Account(
            id = "HDFC_Bank_XX7011",
            bankName = "HDFC Bank",
            accountType = AccountType.SAVINGS,
            maskNumber = "XX7011",
            lastKnownBalance = 45000.0,
            lastUpdated = now - (30L * 24L * 60L * 60L * 1000L) // 30 days ago
        )

        assertFalse("Account updated 30 days ago should be active", recentAccount.isInactive)
    }

    @Test
    fun `test account inactive when last SMS is more than 1 year old`() {
        val now = System.currentTimeMillis()
        val dormantAccount = Account(
            id = "Axis_Bank_XX1234",
            bankName = "Axis Bank",
            accountType = AccountType.SAVINGS,
            maskNumber = "XX1234",
            lastKnownBalance = 12500.0,
            lastUpdated = now - (400L * 24L * 60L * 60L * 1000L) // 400 days ago
        )

        assertTrue("Account updated 400 days ago should be marked inactive", dormantAccount.isInactive)
    }

    @Test
    fun `test credit card dormancy calculation`() {
        val now = System.currentTimeMillis()
        val oldCard = Account(
            id = "ICICI_Bank_XX9999",
            bankName = "ICICI Bank",
            accountType = AccountType.CREDIT_CARD,
            maskNumber = "XX9999",
            lastKnownBalance = 150000.0,
            lastUpdated = now - (370L * 24L * 60L * 60L * 1000L) // 370 days ago
        )

        assertTrue("Card not used for > 1 year should be dormant/inactive", oldCard.isInactive)
    }
}
