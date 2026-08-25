package com.expensemanager.app.ui.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensemanager.app.core.model.MerchantRule
import com.expensemanager.app.core.model.Transaction
import com.expensemanager.app.data.repository.SmsRepository
import com.expensemanager.app.data.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter

class SettingsViewModel(
    private val transactionRepository: TransactionRepository,
    private val smsRepository: SmsRepository
) : ViewModel() {

    val rules: StateFlow<List<MerchantRule>> = transactionRepository.getAllRules()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncProgress = MutableStateFlow(Pair(0, 0))
    val syncProgress: StateFlow<Pair<Int, Int>> = _syncProgress.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    fun syncAllSms() {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val count = smsRepository.syncAllInboxSms { current, total ->
                    _syncProgress.value = Pair(current, total)
                }
                _statusMessage.value = "Successfully imported $count transaction(s)!"
            } catch (e: Exception) {
                _statusMessage.value = "Sync error: ${e.localizedMessage}"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun deleteRule(rule: MerchantRule) {
        viewModelScope.launch {
            transactionRepository.deleteRule(rule)
        }
    }

    fun exportTransactionsCsv(context: Context, onExportReady: (File) -> Unit) {
        viewModelScope.launch {
            try {
                val transactions = transactionRepository.getAllTransactions().first()
                val exportFile = File(context.cacheDir, "ExpenseManager_Export_${System.currentTimeMillis()}.csv")
                FileWriter(exportFile).use { writer ->
                    writer.append("ID,Date,Time,Type,Category,Merchant,Amount,Currency,Bank,Account,ReferenceID,BalanceAfter,Note\n")
                    for (txn in transactions) {
                        writer.append("\"${txn.id}\",")
                        writer.append("\"${com.expensemanager.app.core.util.DateTimeUtils.formatFullDate(txn.timestamp)}\",")
                        writer.append("\"${com.expensemanager.app.core.util.DateTimeUtils.formatTime(txn.timestamp)}\",")
                        writer.append("\"${txn.type.name}\",")
                        writer.append("\"${txn.category.displayName}\",")
                        writer.append("\"${txn.merchantName ?: ""}\",")
                        writer.append("${txn.amount},")
                        writer.append("\"${txn.currency}\",")
                        writer.append("\"${txn.bankName}\",")
                        writer.append("\"${txn.accountMask ?: ""}\",")
                        writer.append("\"${txn.referenceId ?: ""}\",")
                        writer.append("${txn.balanceAfter ?: ""},")
                        writer.append("\"${txn.note ?: ""}\"\n")
                    }
                }
                onExportReady(exportFile)
                _statusMessage.value = "CSV exported with ${transactions.size} records!"
            } catch (e: Exception) {
                _statusMessage.value = "Export failed: ${e.localizedMessage}"
            }
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            transactionRepository.clearAll()
            _statusMessage.value = "All data cleared successfully"
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }
}
