package com.writingapp.data.local.db

import androidx.room.*
import com.writingapp.data.local.db.entities.RulebookEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RulebookDao {
    @Query("SELECT * FROM rulebooks ORDER BY updatedAt DESC")
    fun getAllRules(): Flow<List<RulebookEntity>>

    @Query("SELECT * FROM rulebooks WHERE category = :category ORDER BY updatedAt DESC")
    fun getRulesByCategory(category: String): Flow<List<RulebookEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rulebook: RulebookEntity): Long

    @Delete
    suspend fun delete(rulebook: RulebookEntity)
}
