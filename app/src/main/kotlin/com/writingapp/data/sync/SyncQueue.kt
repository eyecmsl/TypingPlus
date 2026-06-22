package com.writingapp.data.sync

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class PendingChange(
    val id: String = java.util.UUID.randomUUID().toString(),
    val documentId: Long,
    val title: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val retryCount: Int = 0
)

class SyncQueue(private val context: Context, private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())) {

    private val queueFile: File
        get() = File(context.filesDir, "sync_queue.json")

    private val _pendingChanges = MutableStateFlow<List<PendingChange>>(emptyList())
    val pendingChanges: StateFlow<List<PendingChange>> = _pendingChanges.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    init {
        loadQueue()
    }

    fun enqueue(documentId: Long, title: String, content: String) {
        val change = PendingChange(
            documentId = documentId,
            title = title,
            content = content
        )
        val current = _pendingChanges.value.toMutableList()
        val existingIndex = current.indexOfLast { it.documentId == documentId }
        if (existingIndex >= 0) {
            current[existingIndex] = change
        } else {
            current.add(change)
        }
        _pendingChanges.value = current
        saveQueue()
    }

    fun removeFromQueue(changeId: String) {
        _pendingChanges.value = _pendingChanges.value.filter { it.id != changeId }
        saveQueue()
    }

    fun clearQueue() {
        _pendingChanges.value = emptyList()
        saveQueue()
    }

    suspend fun processQueue(onProcess: suspend (PendingChange) -> Boolean) {
        if (_isSyncing.value) return
        _isSyncing.value = true

        val changes = _pendingChanges.value.toList()
        for (change in changes) {
            try {
                val success = onProcess(change)
                if (success) {
                    removeFromQueue(change.id)
                } else {
                    val updated = change.copy(retryCount = change.retryCount + 1)
                    val list = _pendingChanges.value.toMutableList()
                    val idx = list.indexOfFirst { it.id == change.id }
                    if (idx >= 0) {
                        if (updated.retryCount >= 5) {
                            list.removeAt(idx)
                        } else {
                            list[idx] = updated
                        }
                        _pendingChanges.value = list
                        saveQueue()
                    }
                }
            } catch (e: Exception) {
                Log.e("SyncQueue", "Failed to process change ${change.id}: ${e.message}")
            }
        }

        _isSyncing.value = false
    }

    private fun saveQueue() {
        try {
            val jsonArray = JSONArray()
            _pendingChanges.value.forEach { change ->
                jsonArray.put(JSONObject().apply {
                    put("id", change.id)
                    put("documentId", change.documentId)
                    put("title", change.title)
                    put("content", change.content)
                    put("timestamp", change.timestamp)
                    put("retryCount", change.retryCount)
                })
            }
            queueFile.writeText(jsonArray.toString())
        } catch (_: Exception) { }
    }

    private fun loadQueue() {
        try {
            if (!queueFile.exists()) return
            val text = queueFile.readText()
            val jsonArray = JSONArray(text)
            val list = mutableListOf<PendingChange>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(PendingChange(
                    id = obj.getString("id"),
                    documentId = obj.getLong("documentId"),
                    title = obj.getString("title"),
                    content = obj.getString("content"),
                    timestamp = obj.getLong("timestamp"),
                    retryCount = obj.getInt("retryCount")
                ))
            }
            _pendingChanges.value = list
        } catch (_: Exception) { }
    }
}
