package com.expensemanager.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.widget.RemoteViews
import com.expensemanager.app.ExpenseApplication
import com.expensemanager.app.MainActivity
import com.expensemanager.app.R
import com.expensemanager.app.core.model.Category
import com.expensemanager.app.core.model.TransactionType
import com.expensemanager.app.core.util.CurrencyFormatter
import com.expensemanager.app.core.util.DateTimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.Locale

class CategorySpectrumWidgetProvider : AppWidgetProvider() {

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
            ACTION_SYNC_CATEGORY -> {
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

        val now = System.currentTimeMillis()
        val startOfMonth = DateTimeUtils.getStartOfMonth(now)
        val endOfMonth = DateTimeUtils.getEndOfMonth(now)

        val allTransactions = app.transactionRepository.getAllTransactions().firstOrNull() ?: emptyList()
        val monthExpenses = allTransactions.filter {
            it.timestamp in startOfMonth..endOfMonth &&
            !it.isExcludedFromBudget &&
            (it.type == TransactionType.DEBIT || it.type == TransactionType.PAYMENT || it.type == TransactionType.CARD_PAYMENT || it.type == TransactionType.CASH_WITHDRAWAL)
        }

        val totalExpense = monthExpenses.sumOf { it.amount }

        // Category Breakdown
        val categoryTotals = monthExpenses.groupBy { it.category }
            .mapValues { (_, txns) -> txns.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }

        val top1 = categoryTotals.getOrNull(0)
        val top2 = categoryTotals.getOrNull(1)
        val top3 = categoryTotals.getOrNull(2)

        val cat1Name = top1?.first?.displayName?.split(" ")?.firstOrNull()?.uppercase(Locale.ROOT) ?: "FOOD"
        val cat1Amt = top1?.second ?: 0.0

        val cat2Name = top2?.first?.displayName?.split(" ")?.firstOrNull()?.uppercase(Locale.ROOT) ?: "SHOPPING"
        val cat2Amt = top2?.second ?: 0.0

        val cat3Name = top3?.first?.displayName?.split(" ")?.firstOrNull()?.uppercase(Locale.ROOT) ?: "BILLS"
        val cat3Amt = top3?.second ?: 0.0

        val top1Pct = if (totalExpense > 0 && top1 != null) {
            ((top1.second / totalExpense) * 100).toInt()
        } else {
            0
        }

        // Formatting
        val totalDisplay = if (isHidden) "•••• Spent" else "${formatCompact(totalExpense)} Spent"
        val cat1Display = if (isHidden) "••••" else formatCompact(cat1Amt)
        val cat2Display = if (isHidden) "••••" else formatCompact(cat2Amt)
        val cat3Display = if (isHidden) "••••" else formatCompact(cat3Amt)

        val topDrainDisplay = if (top1 != null && totalExpense > 0) {
            "Top Drain: ${top1.first.displayName.split(" ").firstOrNull() ?: "Food"} ($top1Pct%)"
        } else {
            "No Expenses Yet"
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            10,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val syncIntent = Intent(context, CategorySpectrumWidgetProvider::class.java).apply {
            action = ACTION_SYNC_CATEGORY
        }
        val syncPendingIntent = PendingIntent.getBroadcast(
            context,
            11,
            syncIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        for (widgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_category_spectrum).apply {
                setTextViewText(R.id.widget_tv_cat_total_spend, totalDisplay)
                setTextViewText(R.id.widget_tv_cat1_name, cat1Name)
                setTextViewText(R.id.widget_tv_cat1_amt, cat1Display)
                setTextViewText(R.id.widget_tv_cat2_name, cat2Name)
                setTextViewText(R.id.widget_tv_cat2_amt, cat2Display)
                setTextViewText(R.id.widget_tv_cat3_name, cat3Name)
                setTextViewText(R.id.widget_tv_cat3_amt, cat3Display)
                setTextViewText(R.id.widget_tv_top_drain, topDrainDisplay)
                setTextViewText(R.id.widget_tv_budget_left, if (totalExpense > 0) "Active" else "Clean")

                setOnClickPendingIntent(R.id.widget_cat_root, openAppPendingIntent)
                setOnClickPendingIntent(R.id.widget_btn_sync_cat, syncPendingIntent)
            }
            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }

    private fun formatCompact(amount: Double): String {
        return if (amount >= 100000) {
            String.format(Locale.US, "₹%.1fL", amount / 100000.0)
        } else if (amount >= 1000) {
            String.format(Locale.US, "₹%.1fk", amount / 1000.0)
        } else {
            String.format(Locale.US, "₹%.0f", amount)
        }
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences("expense_widget_prefs", Context.MODE_PRIVATE)
    }

    companion object {
        const val ACTION_SYNC_CATEGORY = "com.expensemanager.app.widget.ACTION_SYNC_CATEGORY"
        const val PREF_HIDE_BALANCE = "widget_hide_balance"
    }
}
