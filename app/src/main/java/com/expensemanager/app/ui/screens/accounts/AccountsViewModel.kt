package com.expensemanager.app.ui.screens.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensemanager.app.core.model.Account
import com.expensemanager.app.core.model.Transaction
import com.expensemanager.app.core.util.DateTimeUtils
import com.expensemanager.app.data.local.dao.TransactionDao
import com.expensemanager.app.data.repository.AccountRepository
import com.expensemanager.app.data.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AccountsViewModel(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    val accounts: StateFlow<List<Account>> = accountRepository.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTransactions: StateFlow<List<Transaction>> = transactionRepository.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _monthlyBudget = MutableStateFlow(50000.0)
    val monthlyBudget: StateFlow<Double> = _monthlyBudget.asStateFlow()

    private val startOfMonth = DateTimeUtils.getStartOfMonth()
    private val endOfMonth = DateTimeUtils.getEndOfMonth()

    val currentMonthSummary: StateFlow<TransactionDao.MonthlySummary> =
        transactionRepository.getMonthlySummary(startOfMonth, endOfMonth)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TransactionDao.MonthlySummary(0.0, 0.0, 0))

    fun setMonthlyBudget(budget: Double) {
        _monthlyBudget.value = budget
    }

    fun deleteAccount(accountId: String) {
        viewModelScope.launch {
            accountRepository.deleteAccount(accountId)
        }
    }
}
