package com.expensemanager.app.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensemanager.app.core.model.Account
import com.expensemanager.app.core.model.AccountType
import com.expensemanager.app.core.model.Transaction
import com.expensemanager.app.core.util.DateTimeUtils
import com.expensemanager.app.data.local.dao.TransactionDao
import com.expensemanager.app.data.repository.AccountRepository
import com.expensemanager.app.data.repository.SmsRepository
import com.expensemanager.app.data.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class DashboardViewModel(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val smsRepository: SmsRepository
) : ViewModel() {

    private val _selectedMonthTimestamp = MutableStateFlow(System.currentTimeMillis())
    val selectedMonthTimestamp: StateFlow<Long> = _selectedMonthTimestamp.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    val accounts: StateFlow<List<Account>> = accountRepository.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalBankBalance: StateFlow<Double> = accounts.map { list ->
        list.filter { it.accountType != AccountType.CREDIT_CARD }
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
            _isSyncing.value = true
            try {
                val count = smsRepository.syncAllInboxSms()
                _syncMessage.value = "Synced $count transaction(s) and live account balances!"
            } catch (e: Exception) {
                _syncMessage.value = "Failed to sync SMS: ${e.localizedMessage}"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun clearSyncMessage() {
        _syncMessage.value = null
    }
}
