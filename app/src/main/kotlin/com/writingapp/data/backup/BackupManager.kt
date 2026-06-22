package com.writingapp.data.backup

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.writingapp.data.local.db.AppDatabase
import com.writingapp.data.local.db.entities.DocumentEntity
import com.writingapp.data.local.db.entities.RulebookEntity
import com.writingapp.data.local.db.entities.WordlineEventEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

data class BackupData(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val documents: List<DocumentEntity> = emptyList(),
    val rulebooks: List<RulebookEntity> = emptyList(),
    val wordlineEvents: List<WordlineEventEntity> = emptyList()
)

class BackupManager(private val context: Context, private val database: AppDatabase) {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    suspend fun createBackup(): Result<File> = withContext(Dispatchers.IO) {
        try {
            val documents: List<DocumentEntity> = database.documentDao().getAllDocuments().let { flow ->
                flow.first()
            }
            val rulebooks: List<RulebookEntity> = database.rulebookDao().getAllRules().let { flow ->
                flow.first()
            }
            val wordlineEvents: List<WordlineEventEntity> = database.wordlineDao().getAllEvents().let { flow ->
                flow.first()
            }

            val backup = BackupData(
                documents = documents,
                rulebooks = rulebooks,
                wordlineEvents = wordlineEvents
            )

            val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val backupFile = File(context.getExternalFilesDir(null), "backup_$dateStr.json")
            backupFile.writeText(gson.toJson(backup))

            Result.success(backupFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreFromUri(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
                ?: return@withContext Result.failure(Exception("Cannot read backup file"))

            val backup = gson.fromJson(json, BackupData::class.java)

            backup.documents.forEach { database.documentDao().insert(it) }
            backup.rulebooks.forEach { database.rulebookDao().insert(it) }
            backup.wordlineEvents.forEach { database.wordlineDao().insert(it) }

            Result.success("Restored ${backup.documents.size} documents, ${backup.rulebooks.size} rules, ${backup.wordlineEvents.size} events")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun exportToUri(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val backupFile = createBackup().getOrThrow()
            context.contentResolver.openOutputStream(uri)?.use { output ->
                backupFile.inputStream().copyTo(output)
            }
            Result.success("Backup exported")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
