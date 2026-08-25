package com.expensemanager.app.core.model

data class SmsMessageItem(
    val id: Long,
    val address: String,
    val body: String,
    val date: Long,
    val type: Int = 1 // 1 = inbox
)
