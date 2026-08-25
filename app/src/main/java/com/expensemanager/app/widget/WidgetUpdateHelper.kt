package com.expensemanager.app.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent

object WidgetUpdateHelper {

    fun updateAllWidgets(context: Context) {
        val providers = listOf(
            ExpenseWidgetProvider::class.java,
            CategorySpectrumWidgetProvider::class.java,
            CashFlowWidgetProvider::class.java,
            UpcomingBillsWidgetProvider::class.java
        )

        val appWidgetManager = AppWidgetManager.getInstance(context)
        for (providerClass in providers) {
            val ids = appWidgetManager.getAppWidgetIds(ComponentName(context, providerClass))
            if (ids.isNotEmpty()) {
                val intent = Intent(context, providerClass).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                }
                context.sendBroadcast(intent)
            }
        }
    }
}
