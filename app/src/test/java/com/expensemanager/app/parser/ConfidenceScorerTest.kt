package com.expensemanager.app.parser

import com.expensemanager.app.parser.pipeline.ConfidenceScorer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfidenceScorerTest {

    @Test
    fun `test full SMS with all fields achieves high confidence`() {
        val scoreResult = ConfidenceScorer.score(
            amount = 450.0,
            bankRecognized = true,
            accountMask = "XX7011",
            referenceId = "629104819201",
            merchant = "Swiggy",
            balanceAfter = 12450.0,
            rulesFired = listOf("Intent: DEBIT"),
            classificationReason = "Merchant: Swiggy"
        )

        assertEquals(1.0f, scoreResult.score, 0.01f)
        assertTrue(scoreResult.diagnostics.warnings.isEmpty())
        assertEquals("629104819201", scoreResult.diagnostics.extractedFields["referenceId"])
    }

    @Test
    fun `test SMS missing balance retains reliable confidence`() {
        val scoreResult = ConfidenceScorer.score(
            amount = 350.0,
            bankRecognized = true,
            accountMask = "XX7011",
            referenceId = "629104819201",
            merchant = "Google India",
            balanceAfter = null,
            rulesFired = listOf("Intent: DEBIT"),
            classificationReason = "Merchant: Google"
        )

        assertEquals(0.95f, scoreResult.score, 0.01f)
        assertTrue(scoreResult.score >= 0.80f)
    }

    @Test
    fun `test SMS missing merchant and UTR yields lower score with warnings`() {
        val scoreResult = ConfidenceScorer.score(
            amount = 100.0,
            bankRecognized = true,
            accountMask = "XX7011",
            referenceId = null,
            merchant = null,
            balanceAfter = null,
            rulesFired = listOf("Intent: DEBIT"),
            classificationReason = "Default Fallback"
        )

        assertTrue(scoreResult.score < 0.80f)
        assertTrue(scoreResult.diagnostics.warnings.size >= 2)
    }
}
