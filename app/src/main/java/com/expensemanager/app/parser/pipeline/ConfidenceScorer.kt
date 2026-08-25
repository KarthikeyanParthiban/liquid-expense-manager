package com.expensemanager.app.parser.pipeline

import com.expensemanager.app.core.model.ParsingDiagnostics

object ConfidenceScorer {

    data class ScoreResult(
        val score: Float,
        val diagnostics: ParsingDiagnostics
    )

    fun score(
        amount: Double?,
        bankRecognized: Boolean,
        accountMask: String?,
        referenceId: String?,
        merchant: String?,
        balanceAfter: Double?,
        rulesFired: List<String>,
        classificationReason: String
    ): ScoreResult {
        var totalScore = 0.0f
        val warnings = mutableListOf<String>()
        val fields = mutableMapOf<String, String>()

        // 1. Amount Extraction (0.25)
        if (amount != null && amount > 0.0) {
            totalScore += 0.25f
            fields["amount"] = amount.toString()
        } else {
            warnings.add("Amount is missing or invalid")
        }

        // 2. Intent & Direction (0.20)
        totalScore += 0.20f

        // 3. Account Mask (0.15)
        if (!accountMask.isNullOrBlank()) {
            totalScore += 0.15f
            fields["accountMask"] = accountMask
        } else {
            warnings.add("Account mask missing, defaulted to PRIMARY")
        }

        // 4. Reference / UTR ID (0.15)
        if (!referenceId.isNullOrBlank()) {
            totalScore += 0.15f
            fields["referenceId"] = referenceId
        } else {
            warnings.add("Reference / UTR ID not found in message")
        }

        // 5. Recognized Bank Issuer (0.10)
        if (bankRecognized) {
            totalScore += 0.10f
            fields["bankRecognized"] = "true"
        } else {
            warnings.add("Bank sender format was generic")
        }

        // 6. Merchant Name (0.10)
        if (!merchant.isNullOrBlank()) {
            totalScore += 0.10f
            fields["merchant"] = merchant
        } else {
            warnings.add("Merchant name could not be identified")
        }

        // 7. Balance Extraction (0.05)
        if (balanceAfter != null && balanceAfter >= 0.0) {
            totalScore += 0.05f
            fields["balanceAfter"] = balanceAfter.toString()
        }

        val clampedScore = totalScore.coerceIn(0.0f, 1.0f)
        val roundedScore = (Math.round(clampedScore * 100f) / 100f)

        val diagnostics = ParsingDiagnostics(
            parserId = "LAYERED_REGEX_ENGINE_V2",
            extractedFields = fields,
            warnings = warnings,
            rulesFired = rulesFired,
            classificationReason = classificationReason
        )

        return ScoreResult(roundedScore, diagnostics)
    }
}
