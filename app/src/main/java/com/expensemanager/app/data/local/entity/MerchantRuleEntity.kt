package com.expensemanager.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.expensemanager.app.core.model.Category
import com.expensemanager.app.core.model.MerchantRule

@Entity(tableName = "merchant_rules")
data class MerchantRuleEntity(
    @PrimaryKey val merchantPattern: String,
    val category: String,
    val updatedAt: Long
) {
    fun toDomain(): MerchantRule {
        return MerchantRule(
            merchantPattern = merchantPattern,
            category = Category.fromString(category),
            updatedAt = updatedAt
        )
    }

    companion object {
        fun fromDomain(rule: MerchantRule): MerchantRuleEntity {
            return MerchantRuleEntity(
                merchantPattern = rule.merchantPattern,
                category = rule.category.name,
                updatedAt = rule.updatedAt
            )
        }
    }
}
