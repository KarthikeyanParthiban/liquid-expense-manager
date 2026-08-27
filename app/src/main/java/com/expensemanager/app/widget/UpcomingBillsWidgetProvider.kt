package com.expensemanager.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.view.View
import android.widget.RemoteViews
import com.expensemanager.app.ExpenseApplication
import com.expensemanager.app.MainActivity
import com.expensemanager.app.R
import com.expensemanager.app.core.model.AccountType
import com.expensemanager.app.core.model.Category
import com.expensemanager.app.core.util.CurrencyFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class UpcomingBillsWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val app = context.applicationContext as? ExpenseApplication ?: return
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                updateWidgets(context, appWidgetManager, appWidgetIds, app)
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val app = context.applicationContext as? ExpenseApplication ?: return

        when (intent.action) {
            ACTION_SYNC_BILLS -> {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        app.smsRepository.syncAllInboxSms()
                        WidgetUpdateHelper.updateAllWidgets(context)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }

    private suspend fun updateWidgets(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
        app: ExpenseApplication
    ) {
        val prefs = getPrefs(context)
        val isHidden = prefs.getBoolean(PREF_HIDE_BALANCE, true)

        val allAccounts = app.accountRepository.getAllAccounts().firstOrNull() ?: emptyList()
        val allTransactions = app.transactionRepository.getAllTransactions().firstOrNull() ?: emptyList()

        // 1. Credit Card Liabilities & Bills
        val creditCardAccounts = allAccounts.filter { it.accountType == AccountType.CREDIT_CARD }
        val liquidBalance = allAccounts.filter {
            it.accountType != AccountType.CREDIT_CARD &&
            it.accountType != AccountType.WALLET &&
            !it.bankName.contains("Groww", ignoreCase = true) &&
            !it.bankName.contains("EPFO", ignoreCase = true) &&
            !it.bankName.contains("PF Account", ignoreCase = true) &&
            it.bankName != "Bank Account"
        }.mapNotNull { it.lastKnownBalance }.sum()

        // Extract Top 2 Dues / Commitments
        val duesList = mutableListOf<BillItemData>()

        for (cc in creditCardAccounts) {
            val bal = cc.lastKnownBalance ?: 0.0
            if (bal > 0) {
                duesList.add(
                    BillItemData(
                        title = "${cc.bankName} CC",
                        dueDate = "Statement Due Soon",
                        amount = bal,
                        isUrgent = true
                    )
                )
            }
        }

        // Check for SIPs / Utilities from recent history if needed
        val recentSips = allTransactions.filter {
            it.category == Category.INVESTMENT || it.category == Category.BILLS_UTILITIES
        }.take(2)

        for (sip in recentSips) {
            if (duesList.size < 2) {
                duesList.add(
                    BillItemData(
                        title = sip.merchantName ?: sip.category.displayName,
                        dueDate = "Monthly Recurring",
                        amount = sip.amount,
                        isUrgent = false
                    )
                )
            }
        }

        // Fallbacks if no accounts yet
        if (duesList.isEmpty()) {
            duesList.add(BillItemData("Credit Card Bills", "No active card dues", 0.0, false))
            duesList.add(BillItemData("Monthly SIPs & Utilities", "All clear this month", 0.0, false))
        } else if (duesList.size == 1) {
            duesList.add(BillItemData("SIP & Utilities", "Scheduled", 0.0, false))
        }

        val totalScheduled = duesList.sumOf { it.amount }
        val billsCountText = "${duesList.count { it.amount > 0 }} Dues"
        val coverageStatus = if (liquidBalance >= totalScheduled) "Covered by LQD" else "Attention Needed"

        val bill1 = duesList[0]
        val bill2 = duesList.getOrNull(1)

        val bill1AmtStr = if (isHidden) "••••••" else if (bill1.amount > 0) CurrencyFormatter.format(bill1.amount) else "₹0"
        val bill2AmtStr = if (isHidden) "••••••" else if (bill2 != null && bill2.amount > 0) CurrencyFormatter.format(bill2.amount) else "₹0"
        val totalScheduledStr = if (isHidden) "Total Scheduled: ••••••" else "Total Scheduled: ${CurrencyFormatter.format(totalScheduled)}"

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            30,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val syncIntent = Intent(context, UpcomingBillsWidgetProvider::class.java).apply {
            action = ACTION_SYNC_BILLS
        }
        val syncPendingIntent = PendingIntent.getBroadcast(
            context,
            31,
            syncIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        for (widgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_upcoming_bills).apply {
                setTextViewText(R.id.widget_tv_bills_count, billsCountText)
                setTextViewText(R.id.widget_tv_bill1_title, bill1.title)
                setTextViewText(R.id.widget_tv_bill1_due, bill1.dueDate)
                setTextViewText(R.id.widget_tv_bill1_amt, bill1AmtStr)
                setViewVisibility(R.id.widget_chip_bill1_urgent, if (bill1.isUrgent) View.VISIBLE else View.GONE)

                if (bill2 != null) {
                    setTextViewText(R.id.widget_tv_bill2_title, bill2.title)
                    setTextViewText(R.id.widget_tv_bill2_due, bill2.dueDate)
                    setTextViewText(R.id.widget_tv_bill2_amt, bill2AmtStr)
                    setViewVisibility(R.id.widget_bill2_row, View.VISIBLE)
                } else {
                    setViewVisibility(R.id.widget_bill2_row, View.GONE)
                }

                setTextViewText(R.id.widget_tv_bills_total, totalScheduledStr)
                setTextViewText(R.id.widget_tv_bills_status, coverageStatus)

                setOnClickPendingIntent(R.id.widget_bills_root, openAppPendingIntent)
                setOnClickPendingIntent(R.id.widget_btn_sync_bills, syncPendingIntent)
            }
            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences("expense_widget_prefs", Context.MODE_PRIVATE)
    }

    data class BillItemData(
        val title: String,
        val dueDate: String,
        val amount: Double,
        val isUrgent: Boolean
    )

    companion object {
        const val ACTION_SYNC_BILLS = "com.expensemanager.app.widget.ACTION_SYNC_BILLS"
        const val PREF_HIDE_BALANCE = "widget_hide_balance"
    }
}
