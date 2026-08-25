package com.expensemanager.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.expensemanager.app.ExpenseApplication
import com.expensemanager.app.parser.SmsParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            if (messages.isNullOrEmpty()) return

            val pendingResult = goAsync()
            val app = context.applicationContext as? ExpenseApplication ?: return

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val combinedBody = StringBuilder()
                    var sender = ""
                    var timestamp = System.currentTimeMillis()

                    for (sms in messages) {
                        sender = sms.displayOriginatingAddress ?: sms.originatingAddress ?: ""
                        combinedBody.append(sms.displayMessageBody ?: sms.messageBody ?: "")
                        timestamp = sms.timestampMillis
                    }

                    val fullMessage = combinedBody.toString()
                    val rules = app.transactionRepository.getAllRulesSnapshot()

                    val balanceUpdate = SmsParser.extractBalanceUpdate(sender, fullMessage, timestamp)
                    if (balanceUpdate != null) {
                        app.transactionRepository.processBalanceUpdate(balanceUpdate)
                    }

                    val parsedResult = SmsParser.parse(sender, fullMessage, timestamp, rules)
                    if (parsedResult != null) {
                        app.transactionRepository.processAndSaveSms(parsedResult)
                    }

                    // Update all live homescreen widgets
                    com.expensemanager.app.widget.WidgetUpdateHelper.updateAllWidgets(context)
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
