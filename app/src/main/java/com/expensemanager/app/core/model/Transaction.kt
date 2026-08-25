package com.expensemanager.app.core.model

data class Transaction(
    val id: String,
    val rawSmsId: Long?,
    val sender: String,
    val amount: Double,
    val currency: String = "INR",
    val type: TransactionType,
    val status: TransactionStatus = TransactionStatus.COMPLETED,
    val category: Category,
    val merchantName: String?,
    val accountId: String,
    val bankName: String,
    val accountType: AccountType = AccountType.BANK_ACCOUNT,
    val accountMask: String?,
    val referenceId: String?,
    val balanceAfter: Double?,
    val timestamp: Long,
    val note: String? = null,
    val isUserEdited: Boolean = false,
    val isExcludedFromBudget: Boolean = false,
    val originalTransactionId: String? = null,
    val relatedTransactionId: String? = null,
    val confidence: Float = 1.0f,
    val classificationReason: String? = null,
    val rawBody: String? = null
)
