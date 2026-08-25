package com.expensemanager.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.expensemanager.app.core.model.Account
import com.expensemanager.app.core.model.AccountType

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val id: String,
    val bankName: String,
    val accountType: String,
    val maskNumber: String,
    val lastKnownBalance: Double?,
    val lastUpdated: Long
) {
    fun toDomain(): Account {
        return Account(
            id = id,
            bankName = bankName,
            accountType = try { AccountType.valueOf(accountType) } catch (e: Exception) { AccountType.BANK_ACCOUNT },
            maskNumber = maskNumber,
            lastKnownBalance = lastKnownBalance,
            lastUpdated = lastUpdated
        )
    }

    companion object {
        fun fromDomain(account: Account): AccountEntity {
            return AccountEntity(
                id = account.id,
                bankName = account.bankName,
                accountType = account.accountType.name,
                maskNumber = account.maskNumber,
                lastKnownBalance = account.lastKnownBalance,
                lastUpdated = account.lastUpdated
            )
        }
    }
}
