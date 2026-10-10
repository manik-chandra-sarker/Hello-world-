package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.CustomRule
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomRuleDao {
    @Query("SELECT * FROM custom_rules ORDER BY id ASC")
    fun getAllRules(): Flow<List<CustomRule>>

    @Query("SELECT * FROM custom_rules WHERE isEnabled = 1")
    suspend fun getActiveRulesList(): List<CustomRule>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: CustomRule): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRules(rules: List<CustomRule>)

    @Update
    suspend fun updateRule(rule: CustomRule)

    @Delete
    suspend fun deleteRule(rule: CustomRule)

    @Query("DELETE FROM custom_rules WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM custom_rules")
    suspend fun getCount(): Int
}
