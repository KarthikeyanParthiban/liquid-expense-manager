package com.expensemanager.app.core.model

data class ParsedSmsResult(
    val amount: Double,
    val currency: String = "INR",
    val type: TransactionType,
    val status: TransactionStatus = TransactionStatus.COMPLETED,
    val category: Category,
    val merchant: String?,
    val bankName: String,
    val accountMask: String?,
    val accountType: AccountType,
    val accountId: String,
    val referenceId: String?,
    val balanceAfter: Double?,
    /** Credit-card available limit, if this SMS reported one. */
    val availableLimit: Double? = null,
    /** Credit-card outstanding / total-due amount, if reported. */
    val outstandingAmount: Double? = null,
    val timestamp: Long,
    val confidence: Float = 1.0f,
    val isExcludedFromBudget: Boolean = false,
    val originalTransactionId: String? = null,
    val relatedTransactionId: String? = null,
    val classificationReason: String? = null,
    val diagnostics: ParsingDiagnostics = ParsingDiagnostics(),
    val rawSender: String,
    val rawBody: String
)
