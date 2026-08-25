package com.expensemanager.app.ui.screens.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensemanager.app.core.model.AccountType
import com.expensemanager.app.core.model.Category
import com.expensemanager.app.core.model.Transaction
import com.expensemanager.app.core.model.TransactionType
import com.expensemanager.app.core.util.DateTimeUtils
import com.expensemanager.app.data.local.dao.TransactionDao
import com.expensemanager.app.data.repository.TransactionRepository
import com.expensemanager.app.ui.components.DailySpend
import com.expensemanager.app.ui.components.PaymentModeSplit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AnalyticsViewModel(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    data class MerchantSpending(
        val merchantName: String,
        val totalAmount: Double,
        val count: Int,
        val category: Category
    )

    private val _selectedMonthTimestamp = MutableStateFlow(System.currentTimeMillis())
    val selectedMonthTimestamp: StateFlow<Long> = _selectedMonthTimestamp.asStateFlow()

    private val _selectedCategoryForDrilldown = MutableStateFlow<Category?>(null)
    val selectedCategoryForDrilldown: StateFlow<Category?> = _selectedCategoryForDrilldown.asStateFlow()

    private val _selectedMerchantForDrilldown = MutableStateFlow<String?>(null)
    val selectedMerchantForDrilldown: StateFlow<String?> = _selectedMerchantForDrilldown.asStateFlow()

    val monthlySummary: StateFlow<TransactionDao.MonthlySummary> = _selectedMonthTimestamp
        .flatMapLatest { timestamp ->
            val start = DateTimeUtils.getStartOfMonth(timestamp)
            val end = DateTimeUtils.getEndOfMonth(timestamp)
            transactionRepository.getMonthlySummary(start, end)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TransactionDao.MonthlySummary(0.0, 0.0, 0))

    val monthTransactions: StateFlow<List<Transaction>> = _selectedMonthTimestamp
        .flatMapLatest { timestamp ->
            val start = DateTimeUtils.getStartOfMonth(timestamp)
            val end = DateTimeUtils.getEndOfMonth(timestamp)
            transactionRepository.getTransactionsInRange(start, end)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categorySpending: StateFlow<List<TransactionDao.CategorySpending>> = _selectedMonthTimestamp
        .flatMapLatest { timestamp ->
            val start = DateTimeUtils.getStartOfMonth(timestamp)
            val end = DateTimeUtils.getEndOfMonth(timestamp)
            transactionRepository.getCategorySpending(start, end)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val topMerchants: StateFlow<List<MerchantSpending>> = monthTransactions.map { list ->
        list.filter { it.type == TransactionType.DEBIT && !it.isExcludedFromBudget }
            .groupBy { it.merchantName ?: it.category.displayName }
            .map { (merchant, txns) ->
                MerchantSpending(
                    merchantName = merchant,
                    totalAmount = txns.sumOf { it.amount },
                    count = txns.size,
                    category = txns.first().category
                )
            }
            .sortedByDescending { it.totalAmount }
            .take(10)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dailySpendingTrends: StateFlow<List<DailySpend>> = monthTransactions.map { list ->
        val cal = Calendar.getInstance().apply { timeInMillis = _selectedMonthTimestamp.value }
        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val monthStr = java.text.SimpleDateFormat("MMM", java.util.Locale.getDefault()).format(cal.time)

        val daySpends = (1..maxDays).map { day ->
            DailySpend(
                dayOfMonth = day,
                dateLabel = "$day $monthStr",
                amount = 0.0
            )
        }.toMutableList()

        val debitTxns = list.filter { it.type == TransactionType.DEBIT && !it.isExcludedFromBudget }
        val txnsByDay = debitTxns.groupBy {
            val tCal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            tCal.get(Calendar.DAY_OF_MONTH)
        }

        txnsByDay.forEach { (day, txns) ->
            if (day in 1..maxDays) {
                daySpends[day - 1] = daySpends[day - 1].copy(
                    amount = txns.sumOf { it.amount }
                )
            }
        }

        val maxAmount = daySpends.maxOfOrNull { it.amount } ?: 0.0
        daySpends.map {
            it.copy(isPeakDay = it.amount == maxAmount && it.amount > 0)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val paymentModeSplit: StateFlow<PaymentModeSplit> = monthTransactions.map { list ->
        val debits = list.filter { it.type == TransactionType.DEBIT && !it.isExcludedFromBudget }
        val ccDebits = debits.filter {
            it.accountType == AccountType.CREDIT_CARD ||
                    it.bankName.contains("Card", ignoreCase = true) ||
                    (it.accountMask?.startsWith("XX") == true && (it.bankName.contains("YES", ignoreCase = true) || it.bankName.contains("Axis", ignoreCase = true) || it.bankName.contains("CRED", ignoreCase = true) || it.bankName.contains("Kiwi", ignoreCase = true)))
        }
        val ccAmount = ccDebits.sumOf { it.amount }
        val totalAmount = debits.sumOf { it.amount }
        val bankUpiAmount = (totalAmount - ccAmount).coerceAtLeast(0.0)

        PaymentModeSplit(
            creditCardAmount = ccAmount,
            bankUpiAmount = bankUpiAmount,
            totalAmount = totalAmount
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PaymentModeSplit(0.0, 0.0, 0.0))

    fun selectCategoryForDrilldown(category: Category?) {
        _selectedCategoryForDrilldown.value = category
    }

    fun selectMerchantForDrilldown(merchantName: String?) {
        _selectedMerchantForDrilldown.value = merchantName
    }

    fun previousMonth() {
        val cal = Calendar.getInstance().apply {
            timeInMillis = _selectedMonthTimestamp.value
            add(Calendar.MONTH, -1)
        }
        _selectedMonthTimestamp.value = cal.timeInMillis
    }

    fun nextMonth() {
        val cal = Calendar.getInstance().apply {
            timeInMillis = _selectedMonthTimestamp.value
            add(Calendar.MONTH, 1)
        }
        _selectedMonthTimestamp.value = cal.timeInMillis
    }
}
