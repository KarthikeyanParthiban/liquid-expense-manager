package com.expensemanager.app.data.repository

import android.content.Context
import android.net.Uri
import android.provider.Telephony
import com.expensemanager.app.core.model.SmsMessageItem
import com.expensemanager.app.parser.SmsParser
import kotlinx.coroutines.Dispatchers
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

    suspend fun readHistoricalSms(
        limit: Int = 10000,
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
                    if (messages.size >= limit) {
                        break
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("SmsRepository", "Error querying SMS content provider", e)
        }

        return@withContext messages
    }

    /**
     * Fast incremental sync by default. Only scans new SMS since the last sync.
     * If forceFull is true, scans entire inbox.
     */
    suspend fun syncAllInboxSms(
        forceFull: Boolean = false,
        onProgress: (current: Int, total: Int) -> Unit = { _, _ -> }
    ): Int = withContext(Dispatchers.IO) {
        val lastSynced = if (forceFull) 0L else getLastSyncedTimestamp()
        val rawMessages = readHistoricalSms(sinceTimestamp = lastSynced)
        val total = rawMessages.size
        val rules = transactionRepository.getAllRulesSnapshot()
        var insertedCount = 0
        var maxTimestamp = lastSynced

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

            onProgress(index + 1, total)
        }

        if (maxTimestamp > lastSynced) {
            setLastSyncedTimestamp(maxTimestamp)
        }

        return@withContext insertedCount
    }
}
