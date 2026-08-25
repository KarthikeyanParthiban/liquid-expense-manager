package com.expensemanager.app.core.model

enum class SyncStage {
    IDLE,
    SCANNING_INBOX,
    CLASSIFYING,
    FINALIZING,
    COMPLETED,
    FAILED
}

data class SyncProgressState(
    val isSyncing: Boolean = false,
    val showOverlay: Boolean = false,
    val current: Int = 0,
    val total: Int = 0,
    val stage: SyncStage = SyncStage.IDLE,
    val stageMessage: String = "",
    val latestSender: String = "",
    val latestMerchant: String = "",
    val latestCategory: Category? = null,
    val latestAmount: Double? = null,
    val parsedTransactionsCount: Int = 0,
    val balanceUpdatesCount: Int = 0,
    val isDismissedByUser: Boolean = false,
    val errorMessage: String? = null
) {
    val progressFraction: Float
        get() = if (total > 0) (current.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
}
