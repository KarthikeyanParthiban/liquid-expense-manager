package com.expensemanager.app.parser

import com.expensemanager.app.core.model.AccountType
import com.expensemanager.app.core.model.Category
import com.expensemanager.app.core.model.ParsedSmsResult
import com.expensemanager.app.core.model.Transaction
import com.expensemanager.app.core.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeduplicationEngineTest {

    @Test
    fun `test exact reference matching identifies duplicate`() {
        val existingTxn = Transaction(
            id = "txn_123",
            rawSmsId = 1L,
            sender = "VM-HDFCBK",
            amount = 500.0,
            currency = "INR",
            type = TransactionType.DEBIT,
            category = Category.FOOD,
            merchantName = "Swiggy",
            accountId = "HDFC_XX1234",
            bankName = "HDFC Bank",
            accountMask = "XX1234",
            referenceId = "REF_998877",
            balanceAfter = 5000.0,
            timestamp = 1724490000000L
        )

        val duplicateCandidate = ParsedSmsResult(
            amount = 500.0,
            currency = "INR",
            type = TransactionType.DEBIT,
            category = Category.FOOD,
            merchant = "Swiggy UPI",
            bankName = "HDFC Bank",
            accountMask = "XX1234",
            accountType = AccountType.BANK_ACCOUNT,
            accountId = "HDFC_XX1234",
            referenceId = "REF_998877",
            balanceAfter = 5000.0,
            timestamp = 1724490010000L,
            rawSender = "VM-HDFCBK",
            rawBody = "Rs 500 debited from A/c XX1234. Ref: REF_998877"
        )

        val result = DeduplicationEngine.checkDuplicate(duplicateCandidate, listOf(existingTxn))
        assertTrue(result is DeduplicationEngine.DeduplicationResult.ExactDuplicate)
        assertEquals("txn_123", (result as DeduplicationEngine.DeduplicationResult.ExactDuplicate).existingTransactionId)
    }

    @Test
    fun `test fuzzy matching within 10 minute window with same amount and account`() {
        val baseTime = 1724490000000L

        val existingTxn = Transaction(
            id = "txn_abc",
            rawSmsId = 1L,
            sender = "VM-HDFCBK",
            amount = 350.0,
            currency = "INR",
            type = TransactionType.DEBIT,
            category = Category.TRANSPORT,
            merchantName = "Uber",
            accountId = "HDFC_XX1234",
            bankName = "HDFC Bank",
            accountMask = "XX1234",
            referenceId = null,
            balanceAfter = null,
            timestamp = baseTime
        )

        // 2 minutes later, UPI notification arrived for the same ride with balance
        val fuzzyCandidate = ParsedSmsResult(
            amount = 350.0,
            currency = "INR",
            type = TransactionType.DEBIT,
            category = Category.TRANSPORT,
            merchant = "Uber India",
            bankName = "HDFC Bank",
            accountMask = "XX1234",
            accountType = AccountType.BANK_ACCOUNT,
            accountId = "HDFC_XX1234",
            referenceId = "UPI_918231",
            balanceAfter = 8200.0,
            timestamp = baseTime + (2 * 60 * 1000L), // 2 mins later
            rawSender = "VM-HDFCBK",
            rawBody = "Rs 350 debited from A/c XX1234. Avl Bal Rs 8200. Ref: UPI_918231"
        )

        val result = DeduplicationEngine.checkDuplicate(fuzzyCandidate, listOf(existingTxn))
        assertTrue(result is DeduplicationEngine.DeduplicationResult.FuzzyDuplicate)

        val fuzzyResult = result as DeduplicationEngine.DeduplicationResult.FuzzyDuplicate
        assertEquals("txn_abc", fuzzyResult.existingTransactionId)
        assertEquals("UPI_918231", fuzzyResult.updatedTransaction.referenceId)
        assertEquals(8200.0, fuzzyResult.updatedTransaction.balanceAfter ?: 0.0, 0.01)
    }

    @Test
    fun `test distinct transaction is identified as unique`() {
        val baseTime = 1724490000000L

        val existingTxn = Transaction(
            id = "txn_abc",
            rawSmsId = 1L,
            sender = "VM-HDFCBK",
            amount = 120.0,
            currency = "INR",
            type = TransactionType.DEBIT,
            category = Category.FOOD,
            merchantName = "Tea Post",
            accountId = "HDFC_XX1234",
            bankName = "HDFC Bank",
            accountMask = "XX1234",
            referenceId = "REF_111",
            balanceAfter = 5000.0,
            timestamp = baseTime
        )

        val uniqueCandidate = ParsedSmsResult(
            amount = 890.0, // Different amount
            currency = "INR",
            type = TransactionType.DEBIT,
            category = Category.GROCERIES,
            merchant = "Blinkit",
            bankName = "HDFC Bank",
            accountMask = "XX1234",
            accountType = AccountType.BANK_ACCOUNT,
            accountId = "HDFC_XX1234",
            referenceId = "REF_222",
            balanceAfter = 4110.0,
            timestamp = baseTime + (1000L),
            rawSender = "VM-HDFCBK",
            rawBody = "Rs 890 debited. Ref: REF_222"
        )

        val result = DeduplicationEngine.checkDuplicate(uniqueCandidate, listOf(existingTxn))
        assertTrue(result is DeduplicationEngine.DeduplicationResult.Unique)
    }

    @Test
    fun `test CRED bill payment debit and card payment confirmation are deduplicated`() {
        val baseTime = 1724490000000L

        // 1. Bank debit SMS for CRED bill payment
        val credDebitTxn = Transaction(
            id = "txn_cred_1",
            rawSmsId = 1L,
            sender = "AD-HDFCBK-S",
            amount = 1003.0,
            currency = "INR",
            type = TransactionType.DEBIT,
            category = Category.TRANSFERS,
            merchantName = "CRED Club",
            accountId = "HDFC_XX7011",
            bankName = "HDFC Bank",
            accountMask = "XX7011",
            referenceId = "657917279848",
            balanceAfter = 50000.0,
            isExcludedFromBudget = true,
            timestamp = baseTime
        )

        // 2. 5 minutes later, Card SMS arrives: PAYMENT OF Rs. 1003 RECEIVED TOWARDS YOUR CREDIT CARD
        val cardPaymentCandidate = ParsedSmsResult(
            amount = 1003.0,
            currency = "INR",
            type = TransactionType.TRANSFER,
            category = Category.TRANSFERS,
            merchant = "HDFC Bank Credit Card",
            bankName = "HDFC Bank",
            accountMask = "XX2942",
            accountType = AccountType.CREDIT_CARD,
            accountId = "HDFC_XX2942",
            referenceId = null,
            balanceAfter = null,
            timestamp = baseTime + (5 * 60 * 1000L),
            isExcludedFromBudget = true,
            rawSender = "AD-HDFCBK-S",
            rawBody = "PAYMENT OF Rs. 1003.00 RECEIVED TOWARDS YOUR CREDIT CARD ENDING WITH 2942 ON 1-8-2026."
        )

        val result = DeduplicationEngine.checkDuplicate(cardPaymentCandidate, listOf(credDebitTxn))
        assertTrue(result is DeduplicationEngine.DeduplicationResult.FuzzyDuplicate)
        assertEquals("txn_cred_1", (result as DeduplicationEngine.DeduplicationResult.FuzzyDuplicate).existingTransactionId)
    }
}
