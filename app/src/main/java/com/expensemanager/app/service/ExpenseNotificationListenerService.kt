package com.expensemanager.app.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.expensemanager.app.ExpenseApplication
import com.expensemanager.app.parser.NotificationParser
import com.expensemanager.app.widget.WidgetUpdateHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ExpenseNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName ?: return
        if (!NotificationParser.isSupportedPackage(packageName)) return

        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        val title = extras.getString(Notification.EXTRA_TITLE)
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
        val effectiveText = bigText ?: text

        val timestamp = if (sbn.postTime > 0L) sbn.postTime else System.currentTimeMillis()

        val app = applicationContext as? ExpenseApplication ?: ExpenseApplication.instance

        serviceScope.launch {
            try {
                val rules = app.transactionRepository.getAllRulesSnapshot()
                val parsedResult = NotificationParser.parse(
                    packageName = packageName,
                    title = title,
                    text = effectiveText,
                    timestamp = timestamp,
                    userRules = rules
                )

                if (parsedResult != null) {
                    app.transactionRepository.processAndSaveSms(parsedResult)

                    val balanceUpdate = NotificationParser.extractBalanceUpdate(
                        packageName = packageName,
                        title = title,
                        text = effectiveText,
                        timestamp = timestamp
                    )
                    if (balanceUpdate != null) {
                        app.transactionRepository.processBalanceUpdate(balanceUpdate)
                    }

                    WidgetUpdateHelper.updateAllWidgets(applicationContext)
                }
            } catch (e: Exception) {
                android.util.Log.e("ExpenseNotifListener", "Error processing notification from $packageName", e)
            }
        }
    }
}
