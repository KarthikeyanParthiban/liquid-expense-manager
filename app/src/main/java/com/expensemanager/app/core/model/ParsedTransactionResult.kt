package com.expensemanager.app.core.model

data class ParsedTransactionResult(
    val transaction: ParsedSmsResult,
    val confidence: Float,
    val diagnostics: ParsingDiagnostics
)
