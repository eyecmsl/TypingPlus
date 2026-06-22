package com.writingapp.data.local.db

import androidx.room.*
import com.writingapp.data.local.db.entities.DocumentVersionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentVersionDao {
    @Query("SELECT * FROM document_versions WHERE documentId = :documentId ORDER BY versionNumber DESC")
    fun getVersions(documentId: Long): Flow<List<DocumentVersionEntity>>

    @Query("SELECT * FROM document_versions WHERE documentId = :documentId ORDER BY versionNumber DESC LIMIT 1")
    suspend fun getLatestVersion(documentId: Long): DocumentVersionEntity?

    @Query("SELECT COUNT(*) FROM document_versions WHERE documentId = :documentId")
    suspend fun getVersionCount(documentId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(version: DocumentVersionEntity): Long

    @Delete
    suspend fun delete(version: DocumentVersionEntity)

    @Query("DELETE FROM document_versions WHERE documentId = :documentId AND versionNumber <= :maxVersions")
    suspend fun deleteOlderVersions(documentId: Long, maxVersions: Int = 50)
}
