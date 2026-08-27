package com.expensemanager.app.data.repository

import android.content.Context
import android.net.Uri
import android.provider.Telephony
import com.expensemanager.app.core.model.SmsMessageItem
import com.expensemanager.app.core.model.SyncProgressState
import com.expensemanager.app.core.model.SyncStage
import com.expensemanager.app.parser.SmsParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class SmsRepository(
    private val context: Context,
    private val transactionRepository: TransactionRepository
) {

    private val prefs = context.getSharedPreferences("sms_sync_prefs", Context.MODE_PRIVATE)

    fun getLastSyncedTimestamp(): Long {
        return prefs.getLong("last_synced_timestamp", 0L)
    }

    fun setLastSyncedTimestamp(timestamp: Long) {
        prefs.edit().putLong("last_synced_timestamp", timestamp).apply()
    }

    fun resetSyncTimestamp() {
        prefs.edit().clear().apply()
    }

    suspend fun readHistoricalSms(
        limit: Int = Int.MAX_VALUE,
        sinceTimestamp: Long = 0L
    ): List<SmsMessageItem> = withContext(Dispatchers.IO) {
        val messages = mutableListOf<SmsMessageItem>()
        val uri = Uri.parse("content://sms/inbox")
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE
        )

        val selection = if (sinceTimestamp > 0L) "${Telephony.Sms.DATE} > ?" else null
        val selectionArgs = if (sinceTimestamp > 0L) arrayOf(sinceTimestamp.toString()) else null

        try {
            val cursor = context.contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                "${Telephony.Sms.DATE} DESC"
            )

            cursor?.use {
                val idIdx = it.getColumnIndexOrThrow(Telephony.Sms._ID)
                val addressIdx = it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                val bodyIdx = it.getColumnIndexOrThrow(Telephony.Sms.BODY)
                val dateIdx = it.getColumnIndexOrThrow(Telephony.Sms.DATE)
                val typeIdx = it.getColumnIndexOrThrow(Telephony.Sms.TYPE)

                while (it.moveToNext()) {
                    val id = it.getLong(idIdx)
                    val address = it.getString(addressIdx) ?: ""
                    val body = it.getString(bodyIdx) ?: ""
                    val date = it.getLong(dateIdx)
                    val type = it.getInt(typeIdx)

                    messages.add(SmsMessageItem(id, address, body, date, type))
                    if (limit < Int.MAX_VALUE && messages.size >= limit) {
                        break
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("SmsRepository", "Error querying SMS content provider", e)
        }

        return@withContext messages
    }

    private val _syncState = MutableStateFlow(SyncProgressState())
    val syncState: StateFlow<SyncProgressState> = _syncState.asStateFlow()

    fun dismissSyncOverlay() {
        _syncState.value = _syncState.value.copy(isDismissedByUser = true)
    }

    /**
     * Fast incremental sync by default. Only scans new SMS since the last sync.
     * If forceFull is true, scans entire inbox.
     * Shows loading overlay only on initial sync, full sync, or when >= 5 messages need processing.
     */
    suspend fun syncAllInboxSms(
        forceFull: Boolean = false,
        onProgress: (current: Int, total: Int) -> Unit = { _, _ -> }
    ): Int = withContext(Dispatchers.IO) {
        val isInitialSync = getLastSyncedTimestamp() == 0L
        val isFullOrInitial = forceFull || isInitialSync
        val lastSynced = if (forceFull) 0L else getLastSyncedTimestamp()
        val rawMessages = readHistoricalSms(sinceTimestamp = lastSynced)
        val total = rawMessages.size

        // Only show overlay on initial install sync, explicit full sync, or batch >= 5 messages
        val showOverlay = isFullOrInitial || total >= 5

        _syncState.value = SyncProgressState(
            isSyncing = true,
            showOverlay = showOverlay,
            stage = SyncStage.SCANNING_INBOX,
            stageMessage = "Scanning inbox for bank & UPI alerts...",
            total = total
        )

        try {
            if (total == 0) {
                if (showOverlay) {
                    _syncState.value = SyncProgressState(
                        isSyncing = true,
                        showOverlay = true,
                        stage = SyncStage.COMPLETED,
                        stageMessage = "Inbox is up to date! No new messages.",
                        total = 0,
                        current = 0
                    )
                    delay(500)
                }
                _syncState.value = SyncProgressState(isSyncing = false, showOverlay = false)
                return@withContext 0
            }

            val rules = transactionRepository.getAllRulesSnapshot()
            var insertedCount = 0
            var balanceUpdateCount = 0
            var maxTimestamp = lastSynced

            _syncState.value = _syncState.value.copy(
                stage = SyncStage.CLASSIFYING,
                stageMessage = "Analyzing & classifying transactions...",
                total = total,
                current = 0
            )

            rawMessages.forEachIndexed { index, sms ->
                if (sms.date > maxTimestamp) {
                    maxTimestamp = sms.date
                }

                // 1. Process account balance updates (including daily bank balance broadcasts)
                val balanceUpdate = SmsParser.extractBalanceUpdate(
                    sender = sms.address,
                    body = sms.body,
                    timestamp = sms.date
                )
                if (balanceUpdate != null) {
                    transactionRepository.processBalanceUpdate(balanceUpdate)
                    balanceUpdateCount++
                }

                // 2. Process financial transaction records
                val parsedResult = SmsParser.parse(
                    sender = sms.address,
                    body = sms.body,
                    timestamp = sms.date,
                    userRules = rules
                )

                if (parsedResult != null) {
                    transactionRepository.processAndSaveSms(parsedResult)
                    insertedCount++
                }

                if (showOverlay) {
                    _syncState.value = _syncState.value.copy(
                        current = index + 1,
                        total = total,
                        latestSender = sms.address,
                        latestMerchant = parsedResult?.merchant ?: _syncState.value.latestMerchant,
                        latestCategory = parsedResult?.category ?: _syncState.value.latestCategory,
                        latestAmount = parsedResult?.amount ?: _syncState.value.latestAmount,
                        parsedTransactionsCount = insertedCount,
                        balanceUpdatesCount = balanceUpdateCount
                    )
                }

                onProgress(index + 1, total)
            }

            if (maxTimestamp > lastSynced) {
                setLastSyncedTimestamp(maxTimestamp)
            }

            // Stage 3: Finalize & Refresh Widgets
            if (showOverlay) {
                _syncState.value = _syncState.value.copy(
                    stage = SyncStage.FINALIZING,
                    stageMessage = "Finalizing account balances & widgets..."
                )
            }
            com.expensemanager.app.widget.WidgetUpdateHelper.updateAllWidgets(context)

            // Stage 4: Completed
            if (showOverlay) {
                _syncState.value = _syncState.value.copy(
                    stage = SyncStage.COMPLETED,
                    stageMessage = if (insertedCount > 0) "Imported $insertedCount transaction(s)" else "All transactions up to date"
                )
                delay(650)
            }
            _syncState.value = SyncProgressState(isSyncing = false, showOverlay = false)

            return@withContext insertedCount
        } catch (e: Exception) {
            _syncState.value = SyncProgressState(
                isSyncing = false,
                showOverlay = false,
                stage = SyncStage.FAILED,
                stageMessage = "Sync failed: ${e.localizedMessage}",
                errorMessage = e.localizedMessage
            )
            throw e
        }
    }
}
