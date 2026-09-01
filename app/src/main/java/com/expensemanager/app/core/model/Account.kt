package com.expensemanager.app.core.model

data class Account(
    val id: String,
    val bankName: String,
    val accountType: AccountType,
    val maskNumber: String,
    /** For bank/wallet accounts: available balance (money owned). Null for cards. */
    val lastKnownBalance: Double? = null,
    val lastUpdated: Long = System.currentTimeMillis(),
    /** For credit cards: available spending limit (headroom). Null for bank accounts. */
    val availableLimit: Double? = null,
    /** For credit cards: outstanding amount owed / total due. Null for bank accounts. */
    val outstandingAmount: Double? = null,
    /** Timestamp of the SMS that last set lastKnownBalance/availableLimit — decoupled from lastUpdated. */
    val balanceTimestamp: Long = 0L,
    val transactionCount: Int = 0,
    val totalExpense: Double = 0.0,
    val totalIncome: Double = 0.0
) {
    /**
     * An account or card is marked inactive if its latest SMS / update is more than 1 year old.
     */
    val isInactive: Boolean
        get() = lastUpdated > 0L && (System.currentTimeMillis() - lastUpdated) > ONE_YEAR_MILLIS

    companion object {
        const val ONE_YEAR_MILLIS = 365L * 24L * 60L * 60L * 1000L
    }
}
