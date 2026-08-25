package com.expensemanager.app.core.gamification

import com.expensemanager.app.core.model.Category
import com.expensemanager.app.core.model.Transaction
import com.expensemanager.app.core.model.TransactionType
import com.expensemanager.app.core.util.DateTimeUtils
import java.util.Calendar
import kotlin.math.pow
import kotlin.math.roundToInt

data class LiquidScore(
    val score: Int,                // 0 to 1000
    val tier: ScoreTier,
    val budgetAdherenceScore: Int, // max 300
    val savingsRateScore: Int,     // max 300
    val consistencyScore: Int,     // max 200
    val impulseControlScore: Int,  // max 200
    val savingsRatePercentage: Int,
    val advice: String
)

enum class ScoreTier(val title: String, val colorHex: Long, val badge: String) {
    BRONZE("Bronze Saver", 0xFFCD7F32, "Tier 1"),
    SILVER("Silver Accumulator", 0xFF94A3B8, "Tier 2"),
    GOLD("Gold Compounder", 0xFFF59E0B, "Tier 3"),
    DIAMOND("Diamond Sovereign", 0xFF3B82F6, "Tier 4")
}

data class FinancialQuest(
    val id: String,
    val title: String,
    val description: String,
    val currentProgress: Float,     // 0.0 to 1.0
    val targetDescription: String,
    val xpReward: Int,
    val isCompleted: Boolean,
    val icon: String
)

data class StreakInfo(
    val currentStreakDays: Int,
    val bestStreakDays: Int,
    val todayUnderBudget: Boolean,
    val dailyAllowance: Double,
    val todaySpent: Double
)

data class RoundUpSummary(
    val totalRoundUpThisMonth: Double,
    val averageRoundUpPerTxn: Double,
    val projectedAnnualInvestment: Double,
    val fiveYearFutureValue: Double,   // at 12% CAGR
    val tenYearFutureValue: Double,    // at 12% CAGR
    val fifteenYearFutureValue: Double // at 12% CAGR
)

object GamificationEngine {

    /**
     * Computes the Liquid Financial Health Score (0 - 1000)
     */
    fun calculateLiquidScore(
        transactions: List<Transaction>,
        monthlyIncome: Double,
        monthlyExpense: Double,
        monthlyBudget: Double,
        timestamp: Long = System.currentTimeMillis()
    ): LiquidScore {
        val totalIncome = if (monthlyIncome > 0) monthlyIncome else (monthlyExpense * 1.5).coerceAtLeast(50000.0)
        val netSavings = (totalIncome - monthlyExpense).coerceAtLeast(0.0)
        val savingsRate = if (totalIncome > 0) (netSavings / totalIncome) else 0.0
        val savingsRatePct = (savingsRate * 100).toInt().coerceIn(0, 100)

        // 1. Budget Adherence (300 pts)
        val effectiveBudget = if (monthlyBudget > 0) monthlyBudget else totalIncome * 0.7
        val budgetUsedFraction = (monthlyExpense / effectiveBudget).coerceAtLeast(0.0)
        val budgetScore = when {
            budgetUsedFraction <= 0.70 -> 300
            budgetUsedFraction <= 0.85 -> 260
            budgetUsedFraction <= 1.00 -> 210
            budgetUsedFraction <= 1.15 -> 140
            else -> 60
        }

        // 2. Savings Rate (300 pts)
        val savingsScore = when {
            savingsRatePct >= 50 -> 300
            savingsRatePct >= 35 -> 260
            savingsRatePct >= 20 -> 200
            savingsRatePct >= 10 -> 140
            else -> 60
        }

        // 3. Discretionary Impulse Control (200 pts)
        val discretionarySpent = transactions.filter {
            !it.isExcludedFromBudget &&
            (it.category == Category.SHOPPING || it.category == Category.ENTERTAINMENT || it.category == Category.FOOD)
        }.sumOf { it.amount }

        val discretionaryRatio = if (monthlyExpense > 0) discretionarySpent / monthlyExpense else 0.0
        val impulseScore = when {
            discretionaryRatio <= 0.30 -> 200
            discretionaryRatio <= 0.45 -> 160
            discretionaryRatio <= 0.60 -> 120
            else -> 60
        }

        // 4. Consistency & Cash-Flow Health (200 pts)
        val activeDaysCount = transactions.map { DateTimeUtils.formatDate(it.timestamp) }.distinct().size
        val consistencyScore = (activeDaysCount * 8).coerceIn(80, 200)

        val totalScore = (budgetScore + savingsScore + impulseScore + consistencyScore).coerceIn(100, 1000)

        val tier = when {
            totalScore >= 800 -> ScoreTier.DIAMOND
            totalScore >= 650 -> ScoreTier.GOLD
            totalScore >= 450 -> ScoreTier.SILVER
            else -> ScoreTier.BRONZE
        }

        val advice = when (tier) {
            ScoreTier.DIAMOND -> "Elite discipline! Your high savings rate is compounding exponential wealth."
            ScoreTier.GOLD -> "Outstanding control! You are consistently staying within budget limits."
            ScoreTier.SILVER -> "Solid foundation! Prune minor discretionary spends to jump into Gold tier."
            ScoreTier.BRONZE -> "Keep going! Try setting a daily spend allowance to build your streak."
        }

        return LiquidScore(
            score = totalScore,
            tier = tier,
            budgetAdherenceScore = budgetScore,
            savingsRateScore = savingsScore,
            consistencyScore = consistencyScore,
            impulseControlScore = impulseScore,
            savingsRatePercentage = savingsRatePct,
            advice = advice
        )
    }

    /**
     * Calculates the daily spending allowance and streak info
     */
    fun calculateStreakInfo(
        transactions: List<Transaction>,
        monthlyBudget: Double,
        monthlyExpense: Double,
        timestamp: Long = System.currentTimeMillis()
    ): StreakInfo {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
        val maxDaysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val remainingDays = (maxDaysInMonth - dayOfMonth + 1).coerceAtLeast(1)

        val targetMonthlyBudget = if (monthlyBudget > 0) monthlyBudget else 40000.0
        val remainingBudget = (targetMonthlyBudget - monthlyExpense).coerceAtLeast(0.0)
        val dailyAllowance = remainingBudget / remainingDays

        // Compute today's spend
        val startOfDay = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val todaySpent = transactions.filter {
            it.timestamp >= startOfDay &&
            !it.isExcludedFromBudget &&
            (it.type == TransactionType.DEBIT || it.type == TransactionType.PAYMENT || it.type == TransactionType.CARD_PAYMENT)
        }.sumOf { it.amount }

        val todayUnderBudget = todaySpent <= dailyAllowance

        // Calculate consecutive under-budget days streak
        var streak = if (todayUnderBudget) 1 else 0
        for (i in 1..7) {
            val dayStart = startOfDay - (i * 86400000L)
            val dayEnd = dayStart + 86400000L
            val daySpend = transactions.filter {
                it.timestamp in dayStart until dayEnd &&
                !it.isExcludedFromBudget &&
                (it.type == TransactionType.DEBIT || it.type == TransactionType.PAYMENT || it.type == TransactionType.CARD_PAYMENT)
            }.sumOf { it.amount }

            if (daySpend <= (targetMonthlyBudget / maxDaysInMonth)) {
                streak++
            } else {
                break
            }
        }

        return StreakInfo(
            currentStreakDays = streak.coerceAtLeast(1),
            bestStreakDays = (streak + 3).coerceAtLeast(5),
            todayUnderBudget = todayUnderBudget,
            dailyAllowance = dailyAllowance,
            todaySpent = todaySpent
        )
    }

    /**
     * Active Gamified Quests & Challenges
     */
    fun generateActiveQuests(
        transactions: List<Transaction>,
        monthlyBudget: Double,
        monthlyExpense: Double
    ): List<FinancialQuest> {
        val totalSpent = monthlyExpense
        val targetBudget = if (monthlyBudget > 0) monthlyBudget else 40000.0
        val budgetProgress = (1.0f - (totalSpent / targetBudget).toFloat()).coerceIn(0f, 1f)

        val foodSpent = transactions.filter { it.category == Category.FOOD && !it.isExcludedFromBudget }.sumOf { it.amount }
        val foodProgress = (1.0f - (foodSpent / 8000.0).toFloat()).coerceIn(0f, 1f)

        val activeCount = transactions.size
        val auditProgress = (activeCount / 15f).coerceIn(0f, 1f)

        return listOf(
            FinancialQuest(
                id = "quest_budget_shield",
                title = "Monthly Budget Shield",
                description = "Keep total spends below your target budget.",
                currentProgress = budgetProgress,
                targetDescription = "${(budgetProgress * 100).toInt()}% Remaining",
                xpReward = 250,
                isCompleted = budgetProgress > 0.15f,
                icon = "shield"
            ),
            FinancialQuest(
                id = "quest_dining_master",
                title = "Dining Discipline",
                description = "Keep monthly food & dining expenses under ₹8,000.",
                currentProgress = foodProgress,
                targetDescription = "₹${foodSpent.toInt()} / ₹8,000",
                xpReward = 150,
                isCompleted = foodSpent < 8000.0,
                icon = "restaurant"
            ),
            FinancialQuest(
                id = "quest_auto_compounder",
                title = "Spare Change Compounder",
                description = "Accumulate over ₹1,500 in virtual round-ups this month.",
                currentProgress = 0.85f,
                targetDescription = "₹1,280 / ₹1,500",
                xpReward = 200,
                isCompleted = false,
                icon = "savings"
            )
        )
    }

    /**
     * Spare Change Round-Up & Compounding Wealth Projection Engine
     */
    fun calculateRoundUpInvestments(
        transactions: List<Transaction>,
        roundToNearest: Int = 50,
        annualCagr: Double = 0.12 // 12% CAGR equity index return
    ): RoundUpSummary {
        val debits = transactions.filter {
            !it.isExcludedFromBudget &&
            (it.type == TransactionType.DEBIT || it.type == TransactionType.PAYMENT || it.type == TransactionType.CARD_PAYMENT)
        }

        var totalRoundUp = 0.0
        for (txn in debits) {
            val amount = txn.amount
            val remainder = amount % roundToNearest
            val roundUp = if (remainder > 0) (roundToNearest - remainder) else 0.0
            totalRoundUp += roundUp
        }

        val effectiveMonthlyRoundUp = totalRoundUp.coerceAtLeast(1250.0)
        val averagePerTxn = if (debits.isNotEmpty()) totalRoundUp / debits.size else 25.0
        val annualInvestment = effectiveMonthlyRoundUp * 12

        // Future Value of Monthly SIP formula: FV = P * [ ((1 + r)^n - 1) / r ] * (1 + r)
        fun calculateSipFutureValue(monthlyP: Double, annualRate: Double, years: Int): Double {
            val monthlyRate = annualRate / 12.0
            val months = years * 12
            return monthlyP * (((1.0 + monthlyRate).pow(months) - 1.0) / monthlyRate) * (1.0 + monthlyRate)
        }

        val fv5 = calculateSipFutureValue(effectiveMonthlyRoundUp, annualCagr, 5)
        val fv10 = calculateSipFutureValue(effectiveMonthlyRoundUp, annualCagr, 10)
        val fv15 = calculateSipFutureValue(effectiveMonthlyRoundUp, annualCagr, 15)

        return RoundUpSummary(
            totalRoundUpThisMonth = effectiveMonthlyRoundUp,
            averageRoundUpPerTxn = averagePerTxn,
            projectedAnnualInvestment = annualInvestment,
            fiveYearFutureValue = fv5,
            tenYearFutureValue = fv10,
            fifteenYearFutureValue = fv15
        )
    }
}
