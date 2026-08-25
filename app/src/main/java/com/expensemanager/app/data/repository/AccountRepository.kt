package com.expensemanager.app.data.repository

import com.expensemanager.app.core.model.Account
import com.expensemanager.app.data.local.dao.AccountDao
import com.expensemanager.app.data.local.entity.AccountEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AccountRepository(
    private val accountDao: AccountDao
) {
    fun getAllAccounts(): Flow<List<Account>> =
        accountDao.getAllAccounts().map { entities -> entities.map { it.toDomain() } }

    suspend fun updateAccount(account: Account) {
        accountDao.update(AccountEntity.fromDomain(account))
    }

    suspend fun deleteAccount(accountId: String) {
        accountDao.deleteAccount(accountId)
    }
}
