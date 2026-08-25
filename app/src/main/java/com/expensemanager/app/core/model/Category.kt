package com.expensemanager.app.core.model

enum class Category(
    val displayName: String,
    val defaultIconName: String,
    val colorHex: Long
) {
    FOOD("Food & Dining", "Restaurant", 0xFFFF7043),
    GROCERIES("Groceries", "ShoppingCart", 0xFF66BB6A),
    SHOPPING("Shopping", "ShoppingBag", 0xFFAB47BC),
    TRANSPORT("Transport & Travel", "DirectionsCar", 0xFF42A5F5),
    BILLS_UTILITIES("Bills & Utilities", "ReceiptLong", 0xFFFFA726),
    ENTERTAINMENT("Entertainment", "Movie", 0xFFEC407A),
    HEALTHCARE("Healthcare & Pharmacy", "LocalHospital", 0xFF26A69A),
    INVESTMENT("Investments", "TrendingUp", 0xFF29B6F6),
    SALARY_INCOME("Salary & Income", "AccountBalanceWallet", 0xFF4CAF50),
    TRANSFERS("Transfers", "SwapHoriz", 0xFF78909C),
    FEES_CHARGES("Fees & Charges", "MoneyOff", 0xFF8D6E63),
    EDUCATION("Education", "School", 0xFF5C6BC0),
    PERSONAL("Personal Care", "Spa", 0xFFFF8A65),
    OTHERS("Others", "Category", 0xFF9E9E9E);

    companion object {
        fun fromString(name: String?): Category {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) || it.displayName.equals(name, ignoreCase = true) }
                ?: OTHERS
        }
    }
}
