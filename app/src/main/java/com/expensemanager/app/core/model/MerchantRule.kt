package com.expensemanager.app.core.model

data class MerchantRule(
    val merchantPattern: String,
    val category: Category,
    val updatedAt: Long = System.currentTimeMillis()
)
