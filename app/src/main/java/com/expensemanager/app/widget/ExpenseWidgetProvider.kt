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
import com.expensemanager.app.core.gamification.GamificationEngine
import com.expensemanager.app.core.model.AccountType
import com.expensemanager.app.core.model.Category
import com.expensemanager.app.core.model.TransactionType
import com.expensemanager.app.core.util.CurrencyFormatter
import com.expensemanager.app.core.util.DateTimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.Calendar

class ExpenseWidgetProvider : AppWidgetProvider() {

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
            ACTION_SYNC_WIDGET -> {
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
            ACTION_TOGGLE_BALANCE -> {
                val prefs = getPrefs(context)
                val currentHidden = prefs.getBoolean(PREF_HIDE_BALANCE, true)
                prefs.edit().putBoolean(PREF_HIDE_BALANCE, !currentHidden).apply()

                WidgetUpdateHelper.updateAllWidgets(context)
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

        // 1. Total Liquid Bank Balance
        val allAccounts = app.accountRepository.getAllAccounts().firstOrNull() ?: emptyList()
        val liquidBalance = allAccounts.filter {
            it.accountType != AccountType.CREDIT_CARD &&
            it.accountType != AccountType.WALLET &&
            !it.bankName.contains("Groww", ignoreCase = true) &&
            !it.bankName.contains("EPFO", ignoreCase = true) &&
            !it.bankName.contains("PF Account", ignoreCase = true) &&
            it.bankName != "Bank Account"
        }.mapNotNull { it.lastKnownBalance }.sum()

        // 2. Monthly Summary & Transactions
        val now = System.currentTimeMillis()
        val startOfMonth = DateTimeUtils.getStartOfMonth(now)
        val endOfMonth = DateTimeUtils.getEndOfMonth(now)
        val monthlySummary = app.transactionRepository.getMonthlySummary(startOfMonth, endOfMonth).firstOrNull()

        val totalExpense = monthlySummary?.totalExpense ?: 0.0
        val allTransactions = app.transactionRepository.getAllTransactions().firstOrNull() ?: emptyList()

        // 3. Today's Spends
        val startOfDay = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val todaySpends = allTransactions.filter {
            it.timestamp in startOfDay..now &&
            !it.isExcludedFromBudget &&
            (it.type == TransactionType.DEBIT || it.type == TransactionType.PAYMENT || it.type == TransactionType.CARD_PAYMENT || it.type == TransactionType.CASH_WITHDRAWAL)
        }.sumOf { it.amount }

        // 4. Streak & Safe Daily Allowance
        val streakInfo = GamificationEngine.calculateStreakInfo(
            transactions = allTransactions,
            monthlyBudget = 0.0,
            monthlyExpense = totalExpense,
            timestamp = now
        )

        val progressPct = if (streakInfo.dailyAllowance > 0) {
            ((todaySpends / streakInfo.dailyAllowance) * 100).toInt().coerceIn(0, 100)
        } else {
            0
        }

        val remainingPct = (100 - progressPct).coerceAtLeast(0)
        val statusText = if (todaySpends <= streakInfo.dailyAllowance) {
            "On Track (${remainingPct}% daily left)"
        } else {
            "Over Budget"
        }

        // Display string formatting respecting privacy
        val balanceDisplay = if (isHidden) "₹ ••••••••" else CurrencyFormatter.format(liquidBalance)
        val todaySpendDisplay = if (isHidden) "TODAY: ••••" else "TODAY: ${CurrencyFormatter.format(todaySpends)}"
        val safeDisplay = if (isHidden) "Safe: ••••" else "Safe: ${CurrencyFormatter.format(streakInfo.dailyAllowance)}/day"
        val streakDisplay = "${streakInfo.currentStreakDays}d streak"

        // Pending Intents
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val syncIntent = Intent(context, ExpenseWidgetProvider::class.java).apply {
            action = ACTION_SYNC_WIDGET
        }
        val syncPendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            syncIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val toggleIntent = Intent(context, ExpenseWidgetProvider::class.java).apply {
            action = ACTION_TOGGLE_BALANCE
        }
        val togglePendingIntent = PendingIntent.getBroadcast(
            context,
            2,
            toggleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        for (widgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_expense_summary).apply {
                setTextViewText(R.id.widget_tv_balance, balanceDisplay)
                setTextViewText(R.id.widget_tv_today_spend, todaySpendDisplay)
                setTextViewText(R.id.widget_tv_daily_avg, safeDisplay)
                setTextViewText(R.id.widget_tv_streak, streakDisplay)
                setTextViewText(R.id.widget_tv_recent, statusText)
                setProgressBar(R.id.widget_progress_pacing, 100, progressPct, false)

                setImageViewResource(
                    R.id.widget_btn_toggle_eye,
                    if (isHidden) R.drawable.ic_widget_visibility_off else R.drawable.ic_widget_visibility
                )

                setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent)
                setOnClickPendingIntent(R.id.widget_btn_sync, syncPendingIntent)
                setOnClickPendingIntent(R.id.widget_btn_toggle_eye, togglePendingIntent)
            }

            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences("expense_widget_prefs", Context.MODE_PRIVATE)
    }

    companion object {
        const val ACTION_SYNC_WIDGET = "com.expensemanager.app.widget.ACTION_SYNC_WIDGET"
        const val ACTION_TOGGLE_BALANCE = "com.expensemanager.app.widget.ACTION_TOGGLE_BALANCE"
        const val PREF_HIDE_BALANCE = "widget_hide_balance"

        fun updateAllWidgets(context: Context) {
            WidgetUpdateHelper.updateAllWidgets(context)
        }
    }
}
