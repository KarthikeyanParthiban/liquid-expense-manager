package com.expensemanager.app.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensemanager.app.core.gamification.FinancialQuest
import com.expensemanager.app.core.gamification.GamificationEngine
import com.expensemanager.app.core.gamification.LiquidScore
import com.expensemanager.app.core.gamification.RoundUpSummary
import com.expensemanager.app.core.gamification.StreakInfo
import com.expensemanager.app.core.model.Account
import com.expensemanager.app.core.model.AccountType
import com.expensemanager.app.core.model.Transaction
import com.expensemanager.app.core.util.DateTimeUtils
import com.expensemanager.app.data.local.dao.TransactionDao
import com.expensemanager.app.data.repository.AccountRepository
import com.expensemanager.app.data.repository.SmsRepository
import com.expensemanager.app.data.repository.TransactionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val smsRepository: SmsRepository
) : ViewModel() {

    private val _selectedMonthTimestamp = MutableStateFlow(System.currentTimeMillis())
    val selectedMonthTimestamp: StateFlow<Long> = _selectedMonthTimestamp.asStateFlow()

    val syncState: StateFlow<com.expensemanager.app.core.model.SyncProgressState> = smsRepository.syncState

    val isSyncing: StateFlow<Boolean> = smsRepository.syncState.map { it.isSyncing }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    val accounts: StateFlow<List<Account>> = accountRepository.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalBankBalance: StateFlow<Double> = accounts.map { list ->
        list.filter {
            it.accountType != AccountType.CREDIT_CARD &&
            it.accountType != AccountType.WALLET &&
            !it.bankName.contains("Groww", ignoreCase = true) &&
            !it.bankName.contains("EPFO", ignoreCase = true) &&
            !it.bankName.contains("PF Account", ignoreCase = true) &&
            it.bankName != "Bank Account"
        }
            .mapNotNull { it.lastKnownBalance }
            .sum()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val monthlySummary: StateFlow<TransactionDao.MonthlySummary> = _selectedMonthTimestamp
        .flatMapLatest { timestamp ->
            val start = DateTimeUtils.getStartOfMonth(timestamp)
            val end = DateTimeUtils.getEndOfMonth(timestamp)
            transactionRepository.getMonthlySummary(start, end)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TransactionDao.MonthlySummary(0.0, 0.0, 0))

    val recentTransactions: StateFlow<List<Transaction>> = transactionRepository.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categorySpending: StateFlow<List<TransactionDao.CategorySpending>> = _selectedMonthTimestamp
        .flatMapLatest { timestamp ->
            val start = DateTimeUtils.getStartOfMonth(timestamp)
            val end = DateTimeUtils.getEndOfMonth(timestamp)
            transactionRepository.getCategorySpending(start, end)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthTransactions: StateFlow<List<Transaction>> = _selectedMonthTimestamp
        .flatMapLatest { timestamp ->
            val start = DateTimeUtils.getStartOfMonth(timestamp)
            val end = DateTimeUtils.getEndOfMonth(timestamp)
            transactionRepository.getTransactionsInRange(start, end)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cashFlowPeriods: StateFlow<List<com.expensemanager.app.ui.components.CashFlowPeriod>> = recentTransactions.map { txns ->
        val dayFormat = java.text.SimpleDateFormat("EEE", java.util.Locale.getDefault())
        val dateFormat = java.text.SimpleDateFormat("d MMM", java.util.Locale.getDefault())

        (6 downTo 0).map { daysAgo ->
            val dayCal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -daysAgo)
            }
            val startOfDay = (dayCal.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val endOfDay = (dayCal.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis

            val dayLabel = when (daysAgo) {
                0 -> "Today"
                1 -> "Y'day"
                else -> dayFormat.format(dayCal.time)
            }
            val dateRange = dateFormat.format(dayCal.time)

            val periodTxns = txns.filter { it.timestamp in startOfDay..endOfDay }
            val income = periodTxns.filter { it.type == com.expensemanager.app.core.model.TransactionType.CREDIT }.sumOf { it.amount }
            val spend = periodTxns.filter { it.type == com.expensemanager.app.core.model.TransactionType.DEBIT && !it.isExcludedFromBudget }.sumOf { it.amount }

            com.expensemanager.app.ui.components.CashFlowPeriod(
                label = dayLabel,
                dateRange = dateRange,
                income = income,
                spend = spend
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Gamification & Wealth Building Flows
    val liquidScore: StateFlow<LiquidScore> = combine(recentTransactions, monthlySummary) { txns, summary ->
        GamificationEngine.calculateLiquidScore(
            transactions = txns,
            monthlyIncome = summary.totalIncome,
            monthlyExpense = summary.totalExpense,
            monthlyBudget = 40000.0
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        GamificationEngine.calculateLiquidScore(emptyList(), 0.0, 0.0, 40000.0)
    )

    val streakInfo: StateFlow<StreakInfo> = combine(recentTransactions, monthlySummary) { txns, summary ->
        GamificationEngine.calculateStreakInfo(
            transactions = txns,
            monthlyBudget = 40000.0,
            monthlyExpense = summary.totalExpense
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        GamificationEngine.calculateStreakInfo(emptyList(), 40000.0, 0.0)
    )

    val activeQuests: StateFlow<List<FinancialQuest>> = combine(recentTransactions, monthlySummary) { txns, summary ->
        GamificationEngine.generateActiveQuests(
            transactions = txns,
            monthlyBudget = 40000.0,
            monthlyExpense = summary.totalExpense
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val roundUpSummary: StateFlow<RoundUpSummary> = recentTransactions.map { txns ->
        GamificationEngine.calculateRoundUpInvestments(txns, roundToNearest = 50)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        GamificationEngine.calculateRoundUpInvestments(emptyList(), roundToNearest = 50)
    )

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

    fun syncSms() {
        viewModelScope.launch {
            try {
                val count = smsRepository.syncAllInboxSms()
                _syncMessage.value = "Synced $count transactions from SMS inbox"
            } catch (e: Exception) {
                _syncMessage.value = "SMS sync failed: ${e.localizedMessage}"
            }
        }
    }

    fun dismissSyncOverlay() {
        smsRepository.dismissSyncOverlay()
    }

    fun clearSyncMessage() {
        _syncMessage.value = null
    }
}
