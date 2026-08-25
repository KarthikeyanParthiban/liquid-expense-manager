package com.expensemanager.app.ui.screens.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensemanager.app.core.model.Category
import com.expensemanager.app.core.model.Transaction
import com.expensemanager.app.core.model.TransactionType
import com.expensemanager.app.data.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class TransactionSortOption(val displayName: String) {
    NEWEST_FIRST("Newest"),
    OLDEST_FIRST("Oldest"),
    AMOUNT_HIGH_TO_LOW("Highest Amount"),
    AMOUNT_LOW_TO_HIGH("Lowest Amount")
}

class TransactionsViewModel(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<Category?>(null)
    val selectedCategory: StateFlow<Category?> = _selectedCategory.asStateFlow()

    private val _selectedType = MutableStateFlow<TransactionType?>(null)
    val selectedType: StateFlow<TransactionType?> = _selectedType.asStateFlow()

    private val _selectedSortOption = MutableStateFlow(TransactionSortOption.NEWEST_FIRST)
    val selectedSortOption: StateFlow<TransactionSortOption> = _selectedSortOption.asStateFlow()

    private val _selectedTransactionForEdit = MutableStateFlow<Transaction?>(null)
    val selectedTransactionForEdit: StateFlow<Transaction?> = _selectedTransactionForEdit.asStateFlow()

    val filteredTransactions: StateFlow<List<Transaction>> = combine(
        transactionRepository.getAllTransactions(),
        _searchQuery,
        _selectedCategory,
        _selectedType,
        _selectedSortOption
    ) { transactions, query, category, type, sort ->
        val filtered = transactions.filter { txn ->
            val matchesQuery = if (query.isBlank()) true else {
                val q = query.lowercase().trim()
                (txn.merchantName?.lowercase()?.contains(q) == true) ||
                        (txn.bankName.lowercase().contains(q)) ||
                        (txn.referenceId?.lowercase()?.contains(q) == true) ||
                        (txn.note?.lowercase()?.contains(q) == true) ||
                        (txn.category.displayName.lowercase().contains(q)) ||
                        (txn.amount.toString().contains(q))
            }

            val matchesCategory = category == null || txn.category == category
            val matchesType = type == null || txn.type == type

            matchesQuery && matchesCategory && matchesType
        }

        when (sort) {
            TransactionSortOption.NEWEST_FIRST -> filtered.sortedByDescending { it.timestamp }
            TransactionSortOption.OLDEST_FIRST -> filtered.sortedBy { it.timestamp }
            TransactionSortOption.AMOUNT_HIGH_TO_LOW -> filtered.sortedByDescending { it.amount }
            TransactionSortOption.AMOUNT_LOW_TO_HIGH -> filtered.sortedBy { it.amount }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: Category?) {
        _selectedCategory.value = if (_selectedCategory.value == category) null else category
    }

    fun selectType(type: TransactionType?) {
        _selectedType.value = if (_selectedType.value == type) null else type
    }

    fun setSortOption(sortOption: TransactionSortOption) {
        _selectedSortOption.value = sortOption
    }

    fun clearAllFilters() {
        _searchQuery.value = ""
        _selectedCategory.value = null
        _selectedType.value = null
        _selectedSortOption.value = TransactionSortOption.NEWEST_FIRST
    }

    fun openTransactionDetail(transaction: Transaction) {
        _selectedTransactionForEdit.value = transaction
    }

    fun closeTransactionDetail() {
        _selectedTransactionForEdit.value = null
    }

    fun updateTransaction(transaction: Transaction, applyRuleToMerchant: Boolean = false) {
        viewModelScope.launch {
            transactionRepository.updateTransaction(transaction)
            if (applyRuleToMerchant && !transaction.merchantName.isNullOrBlank()) {
                transactionRepository.updateCategoryAndCreateRule(
                    merchantKeyword = transaction.merchantName,
                    newCategory = transaction.category
                )
            }
            closeTransactionDetail()
        }
    }

    fun deleteTransaction(id: String) {
        viewModelScope.launch {
            transactionRepository.deleteTransaction(id)
            closeTransactionDetail()
        }
    }
}
