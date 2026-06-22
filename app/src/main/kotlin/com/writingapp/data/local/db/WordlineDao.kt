package com.writingapp.data.local.db

import androidx.room.*
import com.writingapp.data.local.db.entities.WordlineEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WordlineDao {
    @Query("SELECT * FROM wordline_events ORDER BY chapter ASC, orderIndex ASC")
    fun getAllEvents(): Flow<List<WordlineEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: WordlineEventEntity): Long

    @Delete
    suspend fun delete(event: WordlineEventEntity)

    @Query("UPDATE wordline_events SET orderIndex = :orderIndex WHERE id = :id")
    suspend fun updateOrder(id: Long, orderIndex: Int)
}
