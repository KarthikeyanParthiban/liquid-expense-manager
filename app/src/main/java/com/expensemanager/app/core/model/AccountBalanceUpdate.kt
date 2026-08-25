package com.expensemanager.app.core.model

data class AccountBalanceUpdate(
    val accountId: String,
    val bankName: String,
    val accountType: AccountType,
    val accountMask: String?,
    val balance: Double,
    val timestamp: Long,
    val rawSender: String,
    val rawBody: String
)
