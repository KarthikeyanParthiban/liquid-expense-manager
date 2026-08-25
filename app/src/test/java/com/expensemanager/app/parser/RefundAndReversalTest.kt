package com.expensemanager.app.parser

import com.expensemanager.app.core.model.AccountType
import com.expensemanager.app.core.model.Category
import com.expensemanager.app.core.model.ParsedSmsResult
import com.expensemanager.app.core.model.Transaction
import com.expensemanager.app.core.model.TransactionStatus
import com.expensemanager.app.core.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RefundAndReversalTest {

    @Test
    fun `test parsing full refund SMS creates REFUND transaction intent`() {
        val sender = "VM-HDFCBK"
        val body = "Refund of Rs. 450.00 credited to HDFC Bank A/c XX7011 for Swiggy cancelled order. Ref 629104819201. Avl Bal Rs 12,900.00"
        val timestamp = 1724490000000L

        val result = SmsParser.parse(sender, body, timestamp)
        assertNotNull(result)
        assertEquals(450.00, result!!.amount, 0.01)
        assertEquals(TransactionType.REFUND, result.type)
        assertEquals("629104819201", result.referenceId)
    }

    @Test
    fun `test parsing partial refund SMS creates REFUND transaction intent`() {
        val sender = "AX-ICICIB"
        val body = "INR 1,500.00 has been refunded to your ICICI Bank Card ending 1044 from AMAZON. Avl Lmt INR 86,500.00"
        val timestamp = 1724490000000L

        val result = SmsParser.parse(sender, body, timestamp)
        assertNotNull(result)
        assertEquals(1500.00, result!!.amount, 0.01)
        assertEquals(TransactionType.REFUND, result.type)
    }

    @Test
    fun `test parsing transaction reversal creates REVERSAL transaction intent`() {
        val sender = "AD-SBIINB"
        val body = "Reversal of Rs 500.00 in SBI A/c XX9812 for failed merchant debit on 24Aug26. Ref No 628190123981. AVL BAL: Rs 4,700.00"
        val timestamp = 1724490000000L

        val result = SmsParser.parse(sender, body, timestamp)
        assertNotNull(result)
        assertEquals(500.00, result!!.amount, 0.01)
        assertEquals(TransactionType.REVERSAL, result.type)
        assertEquals("628190123981", result.referenceId)
    }

    @Test
    fun `test deduplication engine identifies refund referencing original UTR as RelatedTransaction`() {
        val originalDebit = Transaction(
            id = "txn_orig_1",
            rawSmsId = 1L,
            sender = "VM-HDFCBK",
            amount = 450.0,
            currency = "INR",
            type = TransactionType.DEBIT,
            status = TransactionStatus.COMPLETED,
            category = Category.FOOD,
            merchantName = "Swiggy",
            accountId = "HDFC_XX7011",
            bankName = "HDFC Bank",
            accountType = AccountType.BANK_ACCOUNT,
            accountMask = "XX7011",
            referenceId = "REF_SWIGGY_100",
            balanceAfter = 12000.0,
            timestamp = 1724490000000L
        )

        val refundCandidate = ParsedSmsResult(
            amount = 450.0,
            currency = "INR",
            type = TransactionType.REFUND,
            status = TransactionStatus.COMPLETED,
            category = Category.FOOD,
            merchant = "Swiggy",
            bankName = "HDFC Bank",
            accountMask = "XX7011",
            accountType = AccountType.BANK_ACCOUNT,
            accountId = "HDFC_XX7011",
            referenceId = "REF_SWIGGY_100",
            balanceAfter = 12450.0,
            timestamp = 1724490060000L,
            rawSender = "VM-HDFCBK",
            rawBody = "Refund of Rs 450.00 credited to A/c XX7011 for Swiggy. Ref: REF_SWIGGY_100"
        )

        val dedupResult = DeduplicationEngine.checkDuplicate(refundCandidate, listOf(originalDebit))
        assertTrue(dedupResult is DeduplicationEngine.DeduplicationResult.RelatedTransaction)
        val related = dedupResult as DeduplicationEngine.DeduplicationResult.RelatedTransaction
        assertEquals("txn_orig_1", related.existingTransactionId)
        assertEquals("REFUND", related.relationshipType)
    }
}
