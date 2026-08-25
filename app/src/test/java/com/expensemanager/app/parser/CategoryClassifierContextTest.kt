package com.expensemanager.app.parser

import com.expensemanager.app.core.model.Category
import com.expensemanager.app.core.model.MerchantRule
import com.expensemanager.app.core.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryClassifierContextTest {

    @Test
    fun `test Amazon contextual disambiguation`() {
        val freshSms = "Spent Rs. 850.00 at Amazon Fresh on Card XX1006"
        val primeSms = "Spent Rs. 1,499.00 at Amazon Prime Video on Card XX1006"
        val billSms = "Paid Rs. 500.00 for electricity bill on Amazon Pay"
        val retailSms = "Spent Rs. 3,500.00 at Amazon Retail India on Card XX1006"

        assertEquals(Category.GROCERIES, CategoryClassifier.classify("Amazon Fresh", freshSms, TransactionType.DEBIT))
        assertEquals(Category.ENTERTAINMENT, CategoryClassifier.classify("Amazon Prime", primeSms, TransactionType.DEBIT))
        assertEquals(Category.BILLS_UTILITIES, CategoryClassifier.classify("Amazon Pay", billSms, TransactionType.DEBIT))
        assertEquals(Category.SHOPPING, CategoryClassifier.classify("Amazon", retailSms, TransactionType.DEBIT))
    }

    @Test
    fun `test Swiggy contextual disambiguation`() {
        val instamartSms = "Rs 349.00 debited for Swiggy Instamart order"
        val foodSms = "Rs 540.00 debited for Swiggy food delivery"
        val dineoutSms = "Rs 1,200.00 spent at restaurant via Swiggy Dineout"

        assertEquals(Category.GROCERIES, CategoryClassifier.classify("Swiggy Instamart", instamartSms, TransactionType.DEBIT))
        assertEquals(Category.FOOD, CategoryClassifier.classify("Swiggy", foodSms, TransactionType.DEBIT))
        assertEquals(Category.FOOD, CategoryClassifier.classify("Swiggy Dineout", dineoutSms, TransactionType.DEBIT))
    }

    @Test
    fun `test user rules take precedence over contextual logic`() {
        val userRules = listOf(
            MerchantRule("Swiggy", Category.PERSONAL)
        )

        val foodSms = "Rs 540.00 debited for Swiggy food delivery"
        val result = CategoryClassifier.classify("Swiggy", foodSms, TransactionType.DEBIT, userRules)
        assertEquals(Category.PERSONAL, result)
    }

    @Test
    fun `test investment platforms categorization`() {
        val growwSms = "Rs. 5,000.00 debited from A/c XX7011 for Groww mutual fund SIP"
        val zerodhaSms = "Rs. 10,000.00 debited from A/c XX7011 towards Zerodha Broking"

        assertEquals(Category.INVESTMENT, CategoryClassifier.classify("Groww", growwSms, TransactionType.DEBIT))
        assertEquals(Category.INVESTMENT, CategoryClassifier.classify("Zerodha", zerodhaSms, TransactionType.DEBIT))
    }
}
