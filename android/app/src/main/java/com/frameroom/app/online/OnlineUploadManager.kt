package com.frameroom.app.online

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * Pipeline B: Optional Online Upload Subsystem.
 *
 * Fully separated from Pipeline A (Local Event Sync).
 * Does not silently upload photos; requires explicit user selection.
 * Reacts to Internet availability without touching Local Outbox state.
 */
class OnlineUploadManager(
    baseDir: File? = null,
    val context: Context? = null,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
    var destination: OnlineDestination = MockOnlineDestination(),
    val maxConcurrentUploads: Int = 2,
    val baseBackoffMs: Long = 1000L,
    val maxBackoffMs: Long = 8000L
) {
    val repository: OnlineUploadRepository by lazy {
        val dir = baseDir ?: context?.filesDir ?: File(System.getProperty("java.io.tmpdir"), "frameroom_online")
        OnlineUploadRepository(dir)
    }

    private val _records = MutableStateFlow<Map<String, OnlineUploadRecord>>(emptyMap())
    val records = _records.asStateFlow()

    private val _queueStatus = MutableStateFlow("Ready")
    val queueStatus = _queueStatus.asStateFlow()

    var isInternetAvailable: Boolean = true
        private set

    private val activeUploadJobs = mutableMapOf<String, Job>()
    private val lock = Any()

    companion object {
        const val MAX_RETRIES = 5
    }

    init {
        // App restart recovery
        val recovered = repository.recoverRecords()
        synchronized(lock) {
            _records.value = recovered.associateBy { it.photoId }
            updateQueueStatus()
        }
    }

    fun setInternetAvailable(available: Boolean) {
        val wasAvailable = isInternetAvailable
        isInternetAvailable = available

        synchronized(lock) {
            if (!isInternetAvailable) {
                // Cancel active online upload jobs and mark them WAITING_FOR_INTERNET
                for (job in activeUploadJobs.values) {
                    job.cancel()
                }
                activeUploadJobs.clear()

                val map = _records.value.toMutableMap()
                var changed = false
                for ((id, record) in map) {
                    if (record.state == OnlineUploadState.QUEUED || record.state == OnlineUploadState.UPLOADING) {
                        map[id] = record.copy(
                            state = OnlineUploadState.WAITING_FOR_INTERNET,
                            updatedAt = System.currentTimeMillis()
                        )
                        changed = true
                    }
                }
                if (changed) {
                    _records.value = map
                    repository.saveAll(map.values)
                }
            } else if (!wasAvailable && isInternetAvailable) {
                // Internet returned: resume pending online items
                val map = _records.value.toMutableMap()
                var changed = false
                for ((id, record) in map) {
                    if (record.state == OnlineUploadState.WAITING_FOR_INTERNET ||
                        record.state == OnlineUploadState.FAILED_RETRYABLE
                    ) {
                        map[id] = record.copy(
                            state = OnlineUploadState.QUEUED,
                            updatedAt = System.currentTimeMillis()
                        )
                        changed = true
                    }
                }
                if (changed) {
                    _records.value = map
                    repository.saveAll(map.values)
                }
            }
            updateQueueStatus()
        }

        if (isInternetAvailable) {
            triggerQueueProcessing()
        }
    }

    /**
     * User explicitly initiates an online upload for one or more photos.
     * Photos NOT selected by the user NEVER enter this queue.
     */
    fun enqueueForUpload(
        photoId: String,
        roomId: String,
        mediaId: Long,
        mediaUri: String,
        originalPath: String? = null,
        thumbnailPath: String? = null,
        mimeType: String = "image/jpeg",
        fileSizeBytes: Long = 0L
    ) {
        synchronized(lock) {
            val currentMap = _records.value.toMutableMap()
            if (currentMap.containsKey(photoId)) {
                val existing = currentMap[photoId]!!
                if (existing.state == OnlineUploadState.UPLOADED) return // Already uploaded
            }

            val initialState = if (isInternetAvailable) {
                OnlineUploadState.QUEUED
            } else {
                OnlineUploadState.WAITING_FOR_INTERNET
            }

            val record = OnlineUploadRecord(
                photoId = photoId,
                roomId = roomId,
                mediaId = mediaId,
                mediaUri = mediaUri,
                originalPath = originalPath,
                thumbnailPath = thumbnailPath,
                selectedAt = System.currentTimeMillis(),
                state = initialState,
                mimeType = mimeType,
                fileSizeBytes = fileSizeBytes
            )
            currentMap[photoId] = record
            _records.value = currentMap
            repository.saveAll(currentMap.values)
            updateQueueStatus()
        }

        if (isInternetAvailable) {
            triggerQueueProcessing()
        }
    }

    fun enqueueBatch(records: List<OnlineUploadRecord>) {
        synchronized(lock) {
            val currentMap = _records.value.toMutableMap()
            for (rec in records) {
                val initialState = if (isInternetAvailable) {
                    OnlineUploadState.QUEUED
                } else {
                    OnlineUploadState.WAITING_FOR_INTERNET
                }
                currentMap[rec.photoId] = rec.copy(state = initialState)
            }
            _records.value = currentMap
            repository.saveAll(currentMap.values)
            updateQueueStatus()
        }

        if (isInternetAvailable) {
            triggerQueueProcessing()
        }
    }

    fun triggerQueueProcessing() {
        if (!isInternetAvailable) {
            updateQueueStatus()
            return
        }

        synchronized(lock) {
            val availableSlots = maxConcurrentUploads - activeUploadJobs.size
            if (availableSlots <= 0) return

            val eligible = _records.value.values.filter {
                (it.state == OnlineUploadState.QUEUED ||
                        it.state == OnlineUploadState.WAITING_FOR_INTERNET ||
                        it.state == OnlineUploadState.FAILED_RETRYABLE) &&
                        !activeUploadJobs.containsKey(it.photoId)
            }.take(availableSlots)

            for (record in eligible) {
                val photoId = record.photoId
                val job = coroutineScope.launch {
                    try {
                        processSingleUpload(photoId)
                    } finally {
                        synchronized(lock) {
                            activeUploadJobs.remove(photoId)
                        }
                        triggerQueueProcessing()
                    }
                }
                activeUploadJobs[photoId] = job
            }
            updateQueueStatus()
        }
    }

    private suspend fun processSingleUpload(photoId: String) {
        val record = synchronized(lock) { _records.value[photoId] } ?: return
        if (record.state == OnlineUploadState.UPLOADED || record.state == OnlineUploadState.FAILED_PERMANENT) return

        // Exponential backoff
        val backoff = calculateBackoffMs(record.retryCount)
        if (backoff > 0) {
            delay(backoff)
        }

        val currentRecord = synchronized(lock) { _records.value[photoId] } ?: return
        if (currentRecord.state == OnlineUploadState.UPLOADED || currentRecord.state == OnlineUploadState.FAILED_PERMANENT) return

        if (!isInternetAvailable) {
            updateRecordState(photoId, OnlineUploadState.WAITING_FOR_INTERNET)
            return
        }

        // Resolve high-resolution original bytes (strict: do not silently downgrade to low-res thumbnail)
        val originalBytes = resolveOriginalBytes(currentRecord)
        if (originalBytes == null || originalBytes.isEmpty()) {
            updateRecordState(
                photoId,
                OnlineUploadState.FAILED_PERMANENT,
                "Original full-resolution media unavailable"
            )
            return
        }

        updateRecordState(photoId, OnlineUploadState.UPLOADING)

        try {
            val result = destination.uploadPhoto(currentRecord, originalBytes, currentRecord.mimeType)
            when (result) {
                is OnlineUploadResult.Success -> {
                    synchronized(lock) {
                        val map = _records.value.toMutableMap()
                        val r = map[photoId] ?: currentRecord
                        map[photoId] = r.copy(
                            state = OnlineUploadState.UPLOADED,
                            remoteUrl = result.remoteUrl,
                            lastAttemptAt = System.currentTimeMillis(),
                            lastError = null,
                            updatedAt = System.currentTimeMillis()
                        )
                        _records.value = map
                        repository.saveAll(map.values)
                        updateQueueStatus()
                    }
                }
                is OnlineUploadResult.Error -> {
                    handleUploadError(photoId, currentRecord, result.message, result.isRetryable)
                }
            }
        } catch (e: Exception) {
            handleUploadError(photoId, currentRecord, e.message ?: "Network error", isRetryable = true)
        }
    }

    private fun handleUploadError(
        photoId: String,
        currentRecord: OnlineUploadRecord,
        errorMessage: String,
        isRetryable: Boolean
    ) {
        val nextRetry = currentRecord.retryCount + 1
        synchronized(lock) {
            val map = _records.value.toMutableMap()
            val r = map[photoId] ?: currentRecord
            if (!isRetryable || nextRetry >= MAX_RETRIES) {
                map[photoId] = r.copy(
                    state = OnlineUploadState.FAILED_PERMANENT,
                    retryCount = nextRetry,
                    lastAttemptAt = System.currentTimeMillis(),
                    lastError = if (!isRetryable) "Permanent failure: $errorMessage" else "Max retries ($MAX_RETRIES) exceeded: $errorMessage",
                    updatedAt = System.currentTimeMillis()
                )
            } else {
                map[photoId] = r.copy(
                    state = OnlineUploadState.FAILED_RETRYABLE,
                    retryCount = nextRetry,
                    lastAttemptAt = System.currentTimeMillis(),
                    lastError = errorMessage,
                    updatedAt = System.currentTimeMillis()
                )
            }
            _records.value = map
            repository.saveAll(map.values)
            updateQueueStatus()
        }
    }

    private fun resolveOriginalBytes(record: OnlineUploadRecord): ByteArray? {
        // 1. Try original file path on disk
        record.originalPath?.let { path ->
            val file = File(path)
            if (file.exists() && file.length() > 0) {
                return try { file.readBytes() } catch (e: Exception) { null }
            }
        }

        // 2. Try MediaStore ContentResolver if Context is available
        if (context != null && record.mediaUri.isNotBlank()) {
            try {
                val uri = Uri.parse(record.mediaUri)
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bytes = stream.readBytes()
                    if (bytes.isNotEmpty()) return bytes
                }
            } catch (e: Throwable) {
                // ignore
            }
        }

        // 3. If explicit thumbnail was specified as fallback path and exists
        record.thumbnailPath?.let { path ->
            val file = File(path)
            if (file.exists() && file.length() > 0) {
                return try { file.readBytes() } catch (e: Exception) { null }
            }
        }

        return null
    }

    fun updateRecordState(photoId: String, newState: OnlineUploadState, error: String? = null) {
        synchronized(lock) {
            val currentMap = _records.value.toMutableMap()
            val existing = currentMap[photoId] ?: return
            val updated = existing.copy(
                state = newState,
                lastError = error ?: existing.lastError,
                updatedAt = System.currentTimeMillis()
            )
            currentMap[photoId] = updated
            _records.value = currentMap
            repository.saveAll(currentMap.values)
            updateQueueStatus()
        }
    }

    private fun updateQueueStatus() {
        val list = _records.value.values.toList()
        _queueStatus.value = when {
            !isInternetAvailable -> "INTERNET_UNAVAILABLE"
            list.any { it.state == OnlineUploadState.UPLOADING } -> "UPLOADING"
            list.any { it.state == OnlineUploadState.WAITING_FOR_INTERNET } -> "WAITING_FOR_INTERNET"
            list.any { it.state == OnlineUploadState.QUEUED } -> "ONLINE"
            list.any { it.state == OnlineUploadState.FAILED_RETRYABLE } -> "FAILED"
            list.any { it.state == OnlineUploadState.FAILED_PERMANENT } -> "FAILED"
            list.isNotEmpty() && list.all { it.state == OnlineUploadState.UPLOADED } -> "COMPLETED"
            else -> "Ready"
        }
    }

    fun calculateBackoffMs(retryCount: Int): Long {
        if (retryCount <= 0) return 0L
        val shift = (retryCount - 1).coerceAtMost(3)
        val delayMs = baseBackoffMs * (1L shl shift)
        return delayMs.coerceAtMost(maxBackoffMs)
    }

    fun retryUpload(photoId: String) {
        synchronized(lock) {
            val currentMap = _records.value.toMutableMap()
            val existing = currentMap[photoId] ?: return
            currentMap[photoId] = existing.copy(
                state = if (isInternetAvailable) OnlineUploadState.QUEUED else OnlineUploadState.WAITING_FOR_INTERNET,
                updatedAt = System.currentTimeMillis()
            )
            _records.value = currentMap
            repository.saveAll(currentMap.values)
            updateQueueStatus()
        }
        if (isInternetAvailable) {
            triggerQueueProcessing()
        }
    }

    fun retryAllFailed() {
        synchronized(lock) {
            val currentMap = _records.value.toMutableMap()
            var changed = false
            for ((id, record) in currentMap) {
                if (record.state == OnlineUploadState.FAILED_RETRYABLE || record.state == OnlineUploadState.FAILED_PERMANENT) {
                    currentMap[id] = record.copy(
                        state = if (isInternetAvailable) OnlineUploadState.QUEUED else OnlineUploadState.WAITING_FOR_INTERNET,
                        retryCount = 0,
                        updatedAt = System.currentTimeMillis()
                    )
                    changed = true
                }
            }
            if (changed) {
                _records.value = currentMap
                repository.saveAll(currentMap.values)
                updateQueueStatus()
            }
        }
        if (isInternetAvailable) {
            triggerQueueProcessing()
        }
    }

    fun dismissUpload(photoId: String) {
        synchronized(lock) {
            activeUploadJobs[photoId]?.cancel()
            activeUploadJobs.remove(photoId)
            val currentMap = _records.value.toMutableMap()
            currentMap.remove(photoId)
            _records.value = currentMap
            repository.saveAll(currentMap.values)
            updateQueueStatus()
        }
    }

    fun activeJobCount(): Int = synchronized(lock) { activeUploadJobs.size }

    fun clear() {
        synchronized(lock) {
            for (job in activeUploadJobs.values) {
                job.cancel()
            }
            activeUploadJobs.clear()
            _records.value = emptyMap()
            repository.clear()
            updateQueueStatus()
        }
    }
}
