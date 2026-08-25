package com.expensemanager.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.expensemanager.app.core.model.Category
import com.expensemanager.app.core.model.Transaction
import com.expensemanager.app.core.model.TransactionType

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["referenceId"]),
        Index(value = ["timestamp"]),
        Index(value = ["accountId"]),
        Index(value = ["category"]),
        Index(value = ["type"])
    ]
)
data class TransactionEntity(
    @PrimaryKey val id: String,
    val rawSmsId: Long?,
    val sender: String,
    val amount: Double,
    val currency: String = "INR",
    val type: String, // TransactionType name
    val category: String, // Category name
    val merchantName: String?,
    val accountId: String,
    val bankName: String,
    val accountMask: String?,
    val referenceId: String?,
    val balanceAfter: Double?,
    val timestamp: Long,
    val note: String? = null,
    val isUserEdited: Boolean = false,
    val isExcludedFromBudget: Boolean = false,
    val rawBody: String? = null
) {
    fun toDomain(): Transaction {
        return Transaction(
            id = id,
            rawSmsId = rawSmsId,
            sender = sender,
            amount = amount,
            currency = currency,
            type = try { TransactionType.valueOf(type) } catch (e: Exception) { TransactionType.DEBIT },
            category = Category.fromString(category),
            merchantName = merchantName,
            accountId = accountId,
            bankName = bankName,
            accountMask = accountMask,
            referenceId = referenceId,
            balanceAfter = balanceAfter,
            timestamp = timestamp,
            note = note,
            isUserEdited = isUserEdited,
            isExcludedFromBudget = isExcludedFromBudget,
            rawBody = rawBody
        )
    }

    companion object {
        fun fromDomain(txn: Transaction): TransactionEntity {
            return TransactionEntity(
                id = txn.id,
                rawSmsId = txn.rawSmsId,
                sender = txn.sender,
                amount = txn.amount,
                currency = txn.currency,
                type = txn.type.name,
                category = txn.category.name,
                merchantName = txn.merchantName,
                accountId = txn.accountId,
                bankName = txn.bankName,
                accountMask = txn.accountMask,
                referenceId = txn.referenceId,
                balanceAfter = txn.balanceAfter,
                timestamp = txn.timestamp,
                note = txn.note,
                isUserEdited = txn.isUserEdited,
                isExcludedFromBudget = txn.isExcludedFromBudget,
                rawBody = txn.rawBody
            )
        }
    }
}
