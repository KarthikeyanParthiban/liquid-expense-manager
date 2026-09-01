package com.expensemanager.app.core.model

data class AccountBalanceUpdate(
    val accountId: String,
    val bankName: String,
    val accountType: AccountType,
    val accountMask: String?,
    /** Bank/wallet available balance. Null for credit cards. */
    val balance: Double?,
    /** Credit-card available limit. Null for bank accounts. */
    val availableLimit: Double? = null,
    /** Credit-card outstanding / total due. Null for bank accounts. */
    val outstandingAmount: Double? = null,
    val timestamp: Long,
    val rawSender: String,
    val rawBody: String
)
