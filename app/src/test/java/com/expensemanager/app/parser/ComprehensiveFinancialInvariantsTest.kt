package com.expensemanager.app.parser

import com.expensemanager.app.core.model.AccountType
import com.expensemanager.app.core.model.Category
import com.expensemanager.app.core.model.MerchantRule
import com.expensemanager.app.core.model.ParsedSmsResult
import com.expensemanager.app.core.model.Transaction
import com.expensemanager.app.core.model.TransactionStatus
import com.expensemanager.app.core.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ComprehensiveFinancialInvariantsTest {

    // INVARIANT 1: Transfers do not count as expenses
    @Test
    fun `invariant 1 - Transfers are excluded from budget and do not count as expenses`() {
        val transferBody = "Rs. 25,000.00 transferred from HDFC A/c XX7011 to SBI A/c XX9812 via NEFT Ref 991201."
        val result = SmsParser.parse("VM-HDFCBK", transferBody, 1724490000000L)

        assertNotNull(result)
        assertEquals(TransactionType.TRANSFER, result!!.type)
        assertEquals(Category.TRANSFERS, result.category)
        assertTrue("Transfer must be excluded from budget", result.isExcludedFromBudget)
    }

    // INVARIANT 2: Credit-card settlements do not count as new spending
    @Test
    fun `invariant 2 - Credit card bill settlements do not count as new spending`() {
        val settlementBody = "Payment of Rs 12,450.00 received towards your YES BANK Credit Card ending 1006 on 24-08-2026."
        val result = SmsParser.parse("AX-YESBNK", settlementBody, 1724490000000L)

        assertNotNull(result)
        assertEquals(TransactionType.CARD_SETTLEMENT, result!!.type)
        assertTrue("Settlement must be excluded from budget", result.isExcludedFromBudget)
    }

    // INVARIANT 3: Refunds are recognized with REFUND intent
    @Test
    fun `invariant 3 - Refunds have REFUND intent and capture original reference`() {
        val refundBody = "Refund of Rs. 640.00 credited to HDFC Bank A/c XX7011 for Swiggy cancelled order. Ref 99887766."
        val result = SmsParser.parse("VM-HDFCBK", refundBody, 1724490000000L)

        assertNotNull(result)
        assertEquals(TransactionType.REFUND, result!!.type)
        assertEquals("99887766", result.referenceId)
    }

    // INVARIANT 4: Failed transactions do not become completed transactions
    @Test
    fun `invariant 4 - Failed and declined transactions are rejected by classifier`() {
        val failedBody = "Your payment of Rs. 1,500.00 to Swiggy was declined due to incorrect UPI PIN."
        val isFinancial = SmsClassifier.isFinancialSms(failedBody)

        assertFalse("Failed transaction must be rejected", isFinancial)
        assertNull("Failed transaction must not be parsed", SmsParser.parse("VM-HDFCBK", failedBody, 1724490000000L))
    }

    // INVARIANT 5: Duplicate SMS messages do not create duplicate transactions
    @Test
    fun `invariant 5 - Duplicate SMS with identical body or reference is rejected by deduplication engine`() {
        val existingTxn = Transaction(
            id = "txn_orig",
            rawSmsId = 1L,
            sender = "VM-HDFCBK",
            amount = 199.0,
            currency = "INR",
            type = TransactionType.DEBIT,
            status = TransactionStatus.COMPLETED,
            category = Category.ENTERTAINMENT,
            merchantName = "Spotify",
            accountId = "HDFC_XX7011",
            bankName = "HDFC Bank",
            accountType = AccountType.BANK_ACCOUNT,
            accountMask = "XX7011",
            referenceId = "REF_SPOTIFY_1",
            balanceAfter = 15000.0,
            timestamp = 1724490000000L,
            rawBody = "Rs 199 debited for Spotify subscription. Ref REF_SPOTIFY_1"
        )

        val duplicateCandidate = ParsedSmsResult(
            amount = 199.0,
            currency = "INR",
            type = TransactionType.DEBIT,
            status = TransactionStatus.COMPLETED,
            category = Category.ENTERTAINMENT,
            merchant = "Spotify",
            bankName = "HDFC Bank",
            accountMask = "XX7011",
            accountType = AccountType.BANK_ACCOUNT,
            accountId = "HDFC_XX7011",
            referenceId = "REF_SPOTIFY_1",
            balanceAfter = 15000.0,
            timestamp = 1724490010000L,
            rawSender = "VM-HDFCBK",
            rawBody = "Rs 199 debited for Spotify subscription. Ref REF_SPOTIFY_1"
        )

        val dedupResult = DeduplicationEngine.checkDuplicate(duplicateCandidate, listOf(existingTxn))
        assertTrue("Duplicate must be detected", dedupResult is DeduplicationEngine.DeduplicationResult.ExactDuplicate)
    }

    // INVARIANT 6: Balance-only SMS does not create transactions
    @Test
    fun `invariant 6 - Balance-only broadcast does not create financial transactions`() {
        val balanceOnlyBody = "Available Bal in HDFC Bank A/c XX7011 as on yesterday:24-AUG-26 is INR 88,148.00. Cheques subject to clearing."
        
        assertFalse("Balance-only broadcast must not be classified as financial transaction", SmsClassifier.isFinancialSms(balanceOnlyBody))
        assertNull("Balance-only broadcast must not produce parsed transaction", SmsParser.parse("VD-HDFCBK", balanceOnlyBody, 1724490000000L))
        
        // But must successfully produce balance update
        val balUpdate = SmsParser.extractBalanceUpdate("VD-HDFCBK", balanceOnlyBody, 1724490000000L)
        assertNotNull("Balance update must be extracted", balUpdate)
        assertEquals(88148.00, balUpdate!!.balance, 0.01)
    }

    // INVARIANT 7: User categorization rules override generic classification
    @Test
    fun `invariant 7 - User custom categorization rules override default and contextual classification`() {
        val customRule = listOf(
            MerchantRule("Uber", Category.PERSONAL)
        )

        val uberSms = "Spent Rs. 450.00 on Uber ride"
        val category = CategoryClassifier.classify("Uber", uberSms, TransactionType.DEBIT, customRule)

        assertEquals("User rule must override default Transport category", Category.PERSONAL, category)
    }

    // INVARIANT 8: Distinct same-amount transactions with different merchants are not merged
    @Test
    fun `invariant 8 - Distinct transactions with different merchants are not accidentally merged`() {
        val baseTime = 1724490000000L

        val existingZeptoTxn = Transaction(
            id = "txn_zepto",
            rawSmsId = 1L,
            sender = "AX-YESBNK",
            amount = 99.0,
            currency = "INR",
            type = TransactionType.DEBIT,
            status = TransactionStatus.COMPLETED,
            category = Category.GROCERIES,
            merchantName = "Zepto",
            accountId = "YES_XX1006",
            bankName = "Yes Bank",
            accountType = AccountType.CREDIT_CARD,
            accountMask = "XX1006",
            referenceId = null,
            balanceAfter = 70000.0,
            timestamp = baseTime
        )

        // 3 minutes later, a different purchase of same amount (₹99) on Swiggy
        val candidateSwiggy = ParsedSmsResult(
            amount = 99.0,
            currency = "INR",
            type = TransactionType.DEBIT,
            status = TransactionStatus.COMPLETED,
            category = Category.FOOD,
            merchant = "Swiggy",
            bankName = "Yes Bank",
            accountMask = "XX1006",
            accountType = AccountType.CREDIT_CARD,
            accountId = "YES_XX1006",
            referenceId = null,
            balanceAfter = 69901.0,
            timestamp = baseTime + (3 * 60 * 1000L),
            rawSender = "AX-YESBNK",
            rawBody = "INR 99.00 spent on YES BANK Card X1006 @SWIGGY. Avl Lmt INR 69,901.00"
        )

        val dedupResult = DeduplicationEngine.checkDuplicate(candidateSwiggy, listOf(existingZeptoTxn))
        // Must NOT be merged as FuzzyDuplicate because merchants differ!
        assertFalse("Distinct transactions with different merchants must not be merged", dedupResult is DeduplicationEngine.DeduplicationResult.FuzzyDuplicate)
    }
}
