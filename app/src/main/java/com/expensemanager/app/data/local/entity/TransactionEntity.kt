package com.expensemanager.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.expensemanager.app.core.model.AccountType
import com.expensemanager.app.core.model.Category
import com.expensemanager.app.core.model.Transaction
import com.expensemanager.app.core.model.TransactionStatus
import com.expensemanager.app.core.model.TransactionType

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["referenceId"]),
        Index(value = ["timestamp"]),
        Index(value = ["accountId"]),
        Index(value = ["category"]),
        Index(value = ["type"]),
        Index(value = ["status"]),
        Index(value = ["originalTransactionId"]),
        Index(value = ["relatedTransactionId"])
    ]
)
data class TransactionEntity(
    @PrimaryKey val id: String,
    val rawSmsId: Long?,
    val sender: String,
    val amount: Double,
    val currency: String = "INR",
    val type: String, // TransactionType name
    val status: String = TransactionStatus.COMPLETED.name, // TransactionStatus name
    val category: String, // Category name
    val merchantName: String?,
    val accountId: String,
    val bankName: String,
    val accountType: String = AccountType.BANK_ACCOUNT.name,
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
) {
    fun toDomain(): Transaction {
        return Transaction(
            id = id,
            rawSmsId = rawSmsId,
            sender = sender,
            amount = amount,
            currency = currency,
            type = try { TransactionType.valueOf(type) } catch (e: Exception) { TransactionType.DEBIT },
            status = try { TransactionStatus.valueOf(status) } catch (e: Exception) { TransactionStatus.COMPLETED },
            category = Category.fromString(category),
            merchantName = merchantName,
            accountId = accountId,
            bankName = bankName,
            accountType = try { AccountType.valueOf(accountType) } catch (e: Exception) { AccountType.BANK_ACCOUNT },
            accountMask = accountMask,
            referenceId = referenceId,
            balanceAfter = balanceAfter,
            timestamp = timestamp,
            note = note,
            isUserEdited = isUserEdited,
            isExcludedFromBudget = isExcludedFromBudget,
            originalTransactionId = originalTransactionId,
            relatedTransactionId = relatedTransactionId,
            confidence = confidence,
            classificationReason = classificationReason,
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
                status = txn.status.name,
                category = txn.category.name,
                merchantName = txn.merchantName,
                accountId = txn.accountId,
                bankName = txn.bankName,
                accountType = txn.accountType.name,
                accountMask = txn.accountMask,
                referenceId = txn.referenceId,
                balanceAfter = txn.balanceAfter,
                timestamp = txn.timestamp,
                note = txn.note,
                isUserEdited = txn.isUserEdited,
                isExcludedFromBudget = txn.isExcludedFromBudget,
                originalTransactionId = txn.originalTransactionId,
                relatedTransactionId = txn.relatedTransactionId,
                confidence = txn.confidence,
                classificationReason = txn.classificationReason,
                rawBody = txn.rawBody
            )
        }
    }
}
