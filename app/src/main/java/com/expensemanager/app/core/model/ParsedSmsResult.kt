package com.expensemanager.app.core.model

data class ParsedSmsResult(
    val amount: Double,
    val currency: String = "INR",
    val type: TransactionType,
    val category: Category,
    val merchant: String?,
    val bankName: String,
    val accountMask: String?,
    val accountType: AccountType,
    val accountId: String,
    val referenceId: String?,
    val balanceAfter: Double?,
    val timestamp: Long,
    val confidence: Float = 1.0f,
    val isExcludedFromBudget: Boolean = false,
    val rawSender: String,
    val rawBody: String
)
