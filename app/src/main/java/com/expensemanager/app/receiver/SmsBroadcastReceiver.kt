package com.expensemanager.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Telephony
import com.expensemanager.app.ExpenseApplication
import com.expensemanager.app.parser.SmsParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val app = context.applicationContext as? ExpenseApplication ?: return

        // Acquire WakeLock immediately so CPU doesn't sleep while processing
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "liquidexpense:sms_broadcast_wakelock"
        )?.apply {
            acquire(15000L) // 15 seconds max safety timeout
        }

        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            if (messages.isNullOrEmpty()) {
                releaseWakeLock(wakeLock)
                return
            }

            val pendingResult = goAsync()
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

                    processIncomingMessage(app, context, sender, combinedBody.toString(), timestamp)
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    releaseWakeLock(wakeLock)
                    pendingResult.finish()
                }
            }
        } else if (intent.action == "com.expensemanager.app.ACTION_SIMULATE_SMS") {
            val sender = intent.getStringExtra("sender") ?: "HDFCBK"
            val body = intent.getStringExtra("body") ?: ""
            val timestamp = intent.getLongExtra("timestamp", System.currentTimeMillis())

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    processIncomingMessage(app, context, sender, body, timestamp)
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    releaseWakeLock(wakeLock)
                    pendingResult.finish()
                }
            }
        } else {
            releaseWakeLock(wakeLock)
        }
    }

    private fun releaseWakeLock(wakeLock: PowerManager.WakeLock?) {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun processIncomingMessage(
        app: ExpenseApplication,
        context: Context,
        sender: String,
        fullMessage: String,
        timestamp: Long
    ) {
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
    }
}
