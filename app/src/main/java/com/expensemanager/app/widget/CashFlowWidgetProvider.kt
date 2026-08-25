package com.expensemanager.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.widget.RemoteViews
import com.expensemanager.app.ExpenseApplication
import com.expensemanager.app.MainActivity
import com.expensemanager.app.R
import com.expensemanager.app.core.util.CurrencyFormatter
import com.expensemanager.app.core.util.DateTimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.TimeUnit

class CashFlowWidgetProvider : AppWidgetProvider() {

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
            ACTION_SYNC_CASH_FLOW -> {
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
        val monthlySummary = app.transactionRepository.getMonthlySummary(startOfMonth, endOfMonth).firstOrNull()

        val totalIncome = monthlySummary?.totalIncome ?: 0.0
        val totalExpense = monthlySummary?.totalExpense ?: 0.0
        val netSurplus = totalIncome - totalExpense

        val savingsRate = if (totalIncome > 0) {
            ((netSurplus / totalIncome) * 100.0).coerceAtLeast(0.0)
        } else {
            0.0
        }

        val daysLeft = TimeUnit.MILLISECONDS.toDays(endOfMonth - now).coerceAtLeast(1)

        val trajectory = when {
            savingsRate >= 40.0 -> "Trajectory: Strong"
            savingsRate >= 15.0 -> "Trajectory: Steady"
            savingsRate > 0.0 -> "Trajectory: Tight"
            else -> "Trajectory: Deficit"
        }

        // Formattings
        val inflowDisplay = if (isHidden) "+••••••" else "+${CurrencyFormatter.format(totalIncome)}"
        val outflowDisplay = if (isHidden) "−••••••" else "−${CurrencyFormatter.format(totalExpense)}"
        val surplusDisplay = if (isHidden) "+••••••" else "${if (netSurplus >= 0) "+" else "−"}${CurrencyFormatter.format(Math.abs(netSurplus))}"
        val rateDisplay = String.format(Locale.US, "+%.1f%% SAVED", savingsRate)
        val daysDisplay = "$daysLeft Days Left in Month"

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            20,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val syncIntent = Intent(context, CashFlowWidgetProvider::class.java).apply {
            action = ACTION_SYNC_CASH_FLOW
        }
        val syncPendingIntent = PendingIntent.getBroadcast(
            context,
            21,
            syncIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        for (widgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_cash_flow).apply {
                setTextViewText(R.id.widget_tv_savings_rate, rateDisplay)
                setTextViewText(R.id.widget_tv_inflow, inflowDisplay)
                setTextViewText(R.id.widget_tv_outflow, outflowDisplay)
                setTextViewText(R.id.widget_tv_surplus, surplusDisplay)
                setTextViewText(R.id.widget_tv_days_left, daysDisplay)
                setTextViewText(R.id.widget_tv_flow_status, trajectory)
                setProgressBar(R.id.widget_progress_flow, 100, savingsRate.toInt().coerceIn(0, 100), false)

                setOnClickPendingIntent(R.id.widget_flow_root, openAppPendingIntent)
                setOnClickPendingIntent(R.id.widget_btn_sync_flow, syncPendingIntent)
            }
            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences("expense_widget_prefs", Context.MODE_PRIVATE)
    }

    companion object {
        const val ACTION_SYNC_CASH_FLOW = "com.expensemanager.app.widget.ACTION_SYNC_CASH_FLOW"
        const val PREF_HIDE_BALANCE = "widget_hide_balance"
    }
}
