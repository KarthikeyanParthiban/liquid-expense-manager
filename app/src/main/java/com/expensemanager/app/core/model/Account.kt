package com.expensemanager.app.core.model

data class Account(
    val id: String,
    val bankName: String,
    val accountType: AccountType,
    val maskNumber: String,
    val lastKnownBalance: Double? = null,
    val lastUpdated: Long = System.currentTimeMillis(),
    val transactionCount: Int = 0,
    val totalExpense: Double = 0.0,
    val totalIncome: Double = 0.0
)
