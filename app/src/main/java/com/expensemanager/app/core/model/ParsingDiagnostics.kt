package com.expensemanager.app.core.model

data class ParsingDiagnostics(
    val parserId: String = "REGEX_BANK_PATTERNS",
    val extractedFields: Map<String, String> = emptyMap(),
    val warnings: List<String> = emptyList(),
    val rulesFired: List<String> = emptyList(),
    val classificationReason: String = "Standard Heuristic Parse"
)
