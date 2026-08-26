package com.expensemanager.app.parser

import com.expensemanager.app.core.model.AccountType
import com.expensemanager.app.core.model.Category
import com.expensemanager.app.core.model.ParsedSmsResult
import com.expensemanager.app.core.model.Transaction
import com.expensemanager.app.core.model.TransactionStatus
import com.expensemanager.app.core.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CrossChannelDeduplicationTest {

    @Test
    fun `test Notification arrives first then Bank SMS arrives with same UTR`() {
        val baseTimestamp = 1787721200000L

        // 1. Google Pay Notification captured first
        val gpayNotif = NotificationParser.parse(
            packageName = "com.google.android.apps.nbu.paisa.user",
            title = "Google Pay",
            text = "Paid ₹450.00 to Swiggy using HDFC Bank •••• 7011. UPI Ref: 423456789012",
            timestamp = baseTimestamp
        )!!

        val existingNotifTxn = Transaction(
            id = "txn-notif-1",
            rawSmsId = null,
            sender = gpayNotif.rawSender,
            amount = gpayNotif.amount,
            currency = gpayNotif.currency,
            type = gpayNotif.type,
            status = TransactionStatus.COMPLETED,
            category = gpayNotif.category,
            merchantName = gpayNotif.merchant,
            accountId = gpayNotif.accountId,
            bankName = gpayNotif.bankName,
            accountType = gpayNotif.accountType,
            accountMask = gpayNotif.accountMask,
            referenceId = gpayNotif.referenceId,
            balanceAfter = gpayNotif.balanceAfter,
            timestamp = gpayNotif.timestamp,
            confidence = gpayNotif.confidence,
            classificationReason = gpayNotif.classificationReason,
            rawBody = gpayNotif.rawBody
        )

        // 2. Bank Debit SMS arrives 15 seconds later with full balance & account mask
        val smsCandidate = ParsedSmsResult(
            amount = 450.00,
            currency = "INR",
            type = TransactionType.DEBIT,
            status = TransactionStatus.COMPLETED,
            category = Category.FOOD,
            merchant = "Swiggy",
            bankName = "HDFC Bank",
            accountMask = "7011",
            accountType = AccountType.BANK_ACCOUNT,
            accountId = "HDFC_Bank_7011",
            referenceId = "423456789012",
            balanceAfter = 86698.00,
            timestamp = baseTimestamp + 15000L,
            confidence = 1.0f,
            rawSender = "VD-HDFCBK",
            rawBody = "Rs 450.00 debited from HDFC Bank A/c XX7011 to Swiggy UPI Ref 423456789012. Avl Bal: Rs 86,698.00"
        )

        val result = DeduplicationEngine.checkDuplicate(smsCandidate, listOf(existingNotifTxn))

        assertTrue("SMS should be identified as exact duplicate with matching reference ID", result is DeduplicationEngine.DeduplicationResult.ExactDuplicate)
        assertEquals("txn-notif-1", (result as DeduplicationEngine.DeduplicationResult.ExactDuplicate).existingTransactionId)
    }

    @Test
    fun `test Bank SMS arrives first then PhonePe notification arrives without reference ID within 20min window`() {
        val baseTimestamp = 1787721200000L

        // 1. Bank SMS captured first
        val bankSms = Transaction(
            id = "txn-sms-1",
            rawSmsId = 101L,
            sender = "VM-YESBNK",
            amount = 896.00,
            currency = "INR",
            type = TransactionType.DEBIT,
            status = TransactionStatus.COMPLETED,
            category = Category.GROCERIES,
            merchantName = "Blinkit",
            accountId = "YES_Bank_1006",
            bankName = "YES Bank",
            accountType = AccountType.BANK_ACCOUNT,
            accountMask = "1006",
            referenceId = null,
            balanceAfter = 14500.00,
            timestamp = baseTimestamp,
            rawBody = "INR 896.00 spent on YES BANK Card X1006 @UPI_BLINKIT. Avl Bal INR 14,500.00"
        )

        // 2. PhonePe Notification arrives 5 seconds later
        val phonePeCandidate = NotificationParser.parse(
            packageName = "com.phonepe.app",
            title = "PhonePe",
            text = "Payment of ₹896.00 to Blinkit successful",
            timestamp = baseTimestamp + 5000L
        )!!

        val result = DeduplicationEngine.checkDuplicate(phonePeCandidate, listOf(bankSms))

        assertTrue("PhonePe notification should be reconciled with existing SMS debit", result is DeduplicationEngine.DeduplicationResult.FuzzyDuplicate)
        val reconciled = (result as DeduplicationEngine.DeduplicationResult.FuzzyDuplicate).updatedTransaction
        assertEquals("txn-sms-1", reconciled.id)
        assertEquals(896.00, reconciled.amount, 0.01)
        assertEquals("Blinkit", reconciled.merchantName)
        assertEquals(14500.00, reconciled.balanceAfter ?: 0.0, 0.01)
    }

    @Test
    fun `test Two distinct payments of same amount within 5 mins are NOT merged`() {
        val baseTimestamp = 1787721200000L

        val existingUberTxn = Transaction(
            id = "txn-uber",
            rawSmsId = 102L,
            sender = "Google Pay",
            amount = 350.00,
            currency = "INR",
            type = TransactionType.DEBIT,
            status = TransactionStatus.COMPLETED,
            category = Category.TRANSPORT,
            merchantName = "Uber",
            accountId = "HDFC_Bank_7011",
            bankName = "HDFC Bank",
            accountType = AccountType.BANK_ACCOUNT,
            accountMask = "7011",
            referenceId = "REF-UBER-123",
            balanceAfter = null,
            timestamp = baseTimestamp
        )

        // Candidate payment to Zomato of same amount (350.00) 2 mins later
        val zomatoCandidate = NotificationParser.parse(
            packageName = "com.google.android.apps.nbu.paisa.user",
            title = "Google Pay",
            text = "Paid ₹350.00 to Zomato using HDFC Bank •••• 7011. UPI Ref: REF-ZOMATO-456",
            timestamp = baseTimestamp + (2 * 60 * 1000L)
        )!!

        val result = DeduplicationEngine.checkDuplicate(zomatoCandidate, listOf(existingUberTxn))

        assertTrue("Different merchants must not be merged as duplicates", result is DeduplicationEngine.DeduplicationResult.Unique)
    }

    @Test
    fun `test CRED notification and Bank CC payment debit reconciliation`() {
        val baseTimestamp = 1787721200000L

        val bankDebitTxn = Transaction(
            id = "txn-bank-debit",
            rawSmsId = 103L,
            sender = "VD-HDFCBK",
            amount = 15000.00,
            currency = "INR",
            type = TransactionType.DEBIT,
            status = TransactionStatus.COMPLETED,
            category = Category.TRANSFERS,
            merchantName = "CRED",
            accountId = "HDFC_Bank_7011",
            bankName = "HDFC Bank",
            accountType = AccountType.BANK_ACCOUNT,
            accountMask = "7011",
            referenceId = "CRED998877",
            balanceAfter = 50000.00,
            timestamp = baseTimestamp,
            rawBody = "Rs 15000.00 debited from A/c XX7011 towards CRED CC bill payment. Ref: CRED998877"
        )

        val credNotif = NotificationParser.parse(
            packageName = "com.dreamplug.androidapp",
            title = "CRED",
            text = "₹15,000 paid towards HDFC Bank Credit Card ending 1006. Ref: CRED998877",
            timestamp = baseTimestamp + 10000L
        )!!

        val result = DeduplicationEngine.checkDuplicate(credNotif, listOf(bankDebitTxn))

        assertTrue("CRED settlement should reconcile with bank debit", result is DeduplicationEngine.DeduplicationResult.ExactDuplicate)
    }
}
