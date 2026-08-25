package com.expensemanager.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.expensemanager.app.data.local.entity.MerchantRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MerchantRuleDao {

    @Query("SELECT * FROM merchant_rules ORDER BY updatedAt DESC")
    fun getAllRules(): Flow<List<MerchantRuleEntity>>

    @Query("SELECT * FROM merchant_rules")
    suspend fun getAllRulesSnapshot(): List<MerchantRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: MerchantRuleEntity)

    @Delete
    suspend fun delete(rule: MerchantRuleEntity)

    @Query("DELETE FROM merchant_rules")
    suspend fun clearAll()
}
