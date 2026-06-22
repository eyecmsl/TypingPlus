package com.writingapp.data.sync

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class FileSyncManager(private val context: Context) {

    data class SyncFile(
        val name: String,
        val uri: Uri? = null,
        val path: String? = null,
        val lastModified: Long = 0
    )

    private val syncDir: File
        get() {
            val dir = File(context.getExternalFilesDir(null), "synced_docs")
            dir.mkdirs()
            return dir
        }

    suspend fun exportToSyncDir(fileName: String, content: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val file = File(syncDir, fileName)
            file.writeText(content)
            Result.success(file.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importFromSyncDir(fileName: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val file = File(syncDir, fileName)
            if (!file.exists()) return@withContext Result.failure(Exception("File not found: $fileName"))
            Result.success(file.readText())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun listSyncedFiles(): List<SyncFile> = withContext(Dispatchers.IO) {
        syncDir.listFiles()
            ?.filter { it.name.endsWith(".md") }
            ?.map { SyncFile(name = it.name, path = it.absolutePath, lastModified = it.lastModified()) }
            ?.sortedByDescending { it.lastModified }
            ?: emptyList()
    }

    suspend fun deleteFromSyncDir(fileName: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val file = File(syncDir, fileName)
            if (file.exists()) file.delete()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun exportToUri(contentUri: Uri, content: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openOutputStream(contentUri)?.use { outputStream ->
                outputStream.write(content.toByteArray())
            } ?: return@withContext Result.failure(Exception("Cannot open URI"))
            Result.success(contentUri.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importFromUri(contentUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(contentUri)?.use { inputStream ->
                Result.success(inputStream.bufferedReader().readText())
            } ?: Result.failure(Exception("Cannot open URI"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun listExternalFiles(uri: Uri): List<SyncFile> = withContext(Dispatchers.IO) {
        val files = mutableListOf<SyncFile>()
        try {
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri))
            val cursor = context.contentResolver.query(childrenUri, null, null, null, null)
            cursor?.use {
                while (it.moveToNext()) {
                    val name = it.getString(it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME))
                    val docUri = DocumentsContract.buildDocumentUriUsingTree(uri, it.getString(it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)))
                    val mimeType = it.getString(it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE))
                    val lastModified = it.getLong(it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_LAST_MODIFIED))

                    if (mimeType == "text/markdown" || name.endsWith(".md")) {
                        files.add(SyncFile(name = name, uri = docUri, lastModified = lastModified))
                    }
                }
            }
        } catch (_: Exception) { }
        files.sortedByDescending { it.lastModified }
    }
}
