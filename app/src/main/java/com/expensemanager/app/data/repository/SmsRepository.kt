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

    suspend fun readHistoricalSms(limit: Int = 10000): List<SmsMessageItem> = withContext(Dispatchers.IO) {
        val messages = mutableListOf<SmsMessageItem>()
        val uri = Uri.parse("content://sms/inbox")
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE
        )

        try {
            val cursor = context.contentResolver.query(
                uri,
                projection,
                null,
                null,
                "${Telephony.Sms.DATE} DESC LIMIT $limit"
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
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return@withContext messages
    }

    suspend fun syncAllInboxSms(onProgress: (current: Int, total: Int) -> Unit = { _, _ -> }): Int = withContext(Dispatchers.IO) {
        val rawMessages = readHistoricalSms()
        val total = rawMessages.size
        val rules = transactionRepository.getAllRulesSnapshot()
        var insertedCount = 0

        rawMessages.forEachIndexed { index, sms ->
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

        return@withContext insertedCount
    }
}
