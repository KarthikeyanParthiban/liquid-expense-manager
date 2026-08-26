package com.expensemanager.app.ui.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensemanager.app.ExpenseApplication
import com.expensemanager.app.core.model.MerchantRule
import com.expensemanager.app.core.model.Transaction
import com.expensemanager.app.core.update.AppUpdateManager
import com.expensemanager.app.core.update.UpdateInfo
import com.expensemanager.app.data.repository.SmsRepository
import com.expensemanager.app.data.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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

    val syncState = smsRepository.syncState

    val isSyncing: StateFlow<Boolean> = smsRepository.syncState
        .map { it.isSyncing }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val syncProgress: StateFlow<Pair<Int, Int>> = smsRepository.syncState
        .map { Pair(it.current, it.total) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Pair(0, 0))

    private val _isNotificationListenerEnabled = MutableStateFlow(false)
    val isNotificationListenerEnabled: StateFlow<Boolean> = _isNotificationListenerEnabled.asStateFlow()

    fun checkNotificationListenerStatus(context: Context) {
        _isNotificationListenerEnabled.value = com.expensemanager.app.service.NotificationListenerHelper.isNotificationListenerEnabled(context)
    }

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    // OTA In-App Updates State
    private val _updateInfo = MutableStateFlow<UpdateInfo?>(null)
    val updateInfo: StateFlow<UpdateInfo?> = _updateInfo.asStateFlow()

    private val _isCheckingUpdate = MutableStateFlow(false)
    val isCheckingUpdate: StateFlow<Boolean> = _isCheckingUpdate.asStateFlow()

    private val _isDownloadingUpdate = MutableStateFlow(false)
    val isDownloadingUpdate: StateFlow<Boolean> = _isDownloadingUpdate.asStateFlow()

    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress.asStateFlow()

    fun checkForUpdates(currentVersion: String = "1.1.1") {
        viewModelScope.launch {
            _isCheckingUpdate.value = true
            _statusMessage.value = null
            try {
                val result = AppUpdateManager.checkForUpdates(currentVersion)
                if (result.isSuccess) {
                    val info = result.getOrNull()
                    if (info != null && info.isNewer) {
                        _updateInfo.value = info
                    } else {
                        _statusMessage.value = "You are on the latest version ($currentVersion)!"
                    }
                } else {
                    _statusMessage.value = "Failed to check for updates: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _statusMessage.value = "Update check error: ${e.localizedMessage}"
            } finally {
                _isCheckingUpdate.value = false
            }
        }
    }

    fun downloadAndInstallUpdate(context: Context) {
        val info = _updateInfo.value ?: return
        viewModelScope.launch {
            _isDownloadingUpdate.value = true
            _downloadProgress.value = 0f
            val result = AppUpdateManager.downloadAndInstall(
                context = context,
                downloadUrl = info.downloadUrl,
                onProgress = { p -> _downloadProgress.value = p }
            )
            _isDownloadingUpdate.value = false
            if (result.isFailure) {
                _statusMessage.value = "Download failed: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun dismissUpdateDialog() {
        _updateInfo.value = null
    }

    fun syncAllSms(context: Context? = null) {
        val appContext = context ?: ExpenseApplication.instance
        com.expensemanager.app.service.SmsSyncService.startSync(appContext, forceFull = true)
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

    fun clearAllData(context: Context? = null) {
        viewModelScope.launch {
            transactionRepository.clearAll()
            smsRepository.resetSyncTimestamp()
            _statusMessage.value = "All data cleared successfully."
            if (context != null) {
                com.expensemanager.app.widget.WidgetUpdateHelper.updateAllWidgets(context)
            }
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }
}
