package com.expensemanager.app.core.gamification

import com.expensemanager.app.core.model.AccountType
import com.expensemanager.app.core.model.Category
import com.expensemanager.app.core.model.Transaction
import com.expensemanager.app.core.model.TransactionStatus
import com.expensemanager.app.core.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GamificationEngineTest {

    private fun createDummyTxn(amount: Double, category: Category, timestamp: Long = System.currentTimeMillis()): Transaction {
        return Transaction(
            id = "txn_${System.nanoTime()}",
            rawSmsId = null,
            sender = "VM-HDFCBK",
            amount = amount,
            type = TransactionType.DEBIT,
            category = category,
            timestamp = timestamp,
            merchantName = "Test Merchant",
            accountId = "acc_hdfc_7011",
            bankName = "HDFC Bank",
            accountType = AccountType.BANK_ACCOUNT,
            accountMask = "XX7011",
            referenceId = "UPI123456",
            balanceAfter = 50000.0,
            status = TransactionStatus.COMPLETED
        )
    }

    @Test
    fun testLiquidScoreCalculation_highSavingsRate() {
        val txns = listOf(
            createDummyTxn(500.0, Category.FOOD),
            createDummyTxn(1200.0, Category.BILLS_UTILITIES),
            createDummyTxn(800.0, Category.GROCERIES)
        )

        val score = GamificationEngine.calculateLiquidScore(
            transactions = txns,
            monthlyIncome = 100000.0,
            monthlyExpense = 2500.0,
            monthlyBudget = 30000.0
        )

        assertNotNull(score)
        assertTrue("Score should be high for 97% savings rate", score.score >= 800)
        assertEquals(ScoreTier.DIAMOND, score.tier)
    }

    @Test
    fun testStreakAndDailyAllowanceCalculation() {
        val txns = listOf(
            createDummyTxn(350.0, Category.FOOD)
        )

        val streak = GamificationEngine.calculateStreakInfo(
            transactions = txns,
            monthlyBudget = 30000.0,
            monthlyExpense = 5000.0
        )

        assertTrue(streak.dailyAllowance > 0)
        assertTrue(streak.currentStreakDays >= 1)
    }

    @Test
    fun testRoundUpAndCompoundingProjection() {
        val txns = listOf(
            createDummyTxn(320.0, Category.FOOD), // round up to 350 (+30)
            createDummyTxn(110.0, Category.SHOPPING),    // round up to 150 (+40)
            createDummyTxn(95.0, Category.GROCERIES)     // round up to 100 (+5)
        )

        val roundUp = GamificationEngine.calculateRoundUpInvestments(txns, roundToNearest = 50)
        assertTrue("5-year FV should exceed monthly contributions", roundUp.fiveYearFutureValue > (roundUp.totalRoundUpThisMonth * 12 * 5))
        assertTrue("10-year FV should compound significantly", roundUp.tenYearFutureValue > roundUp.fiveYearFutureValue * 2)
    }

    @Test
    fun testActiveQuestsGeneration() {
        val txns = listOf(
            createDummyTxn(450.0, Category.FOOD)
        )

        val quests = GamificationEngine.generateActiveQuests(
            transactions = txns,
            monthlyBudget = 40000.0,
            monthlyExpense = 450.0
        )

        assertEquals(3, quests.size)
        assertTrue(quests.any { it.id == "quest_budget_shield" })
    }
}
