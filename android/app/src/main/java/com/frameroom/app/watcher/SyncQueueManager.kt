package com.frameroom.app.watcher

import android.content.Context
import android.net.Uri
import com.frameroom.app.core.SyncAck
import com.frameroom.app.core.SyncAckStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class SyncQueueManager(
    baseDir: File? = null,
    context: Context? = null,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
    val maxConcurrentUploads: Int = 2,
    val baseBackoffMs: Long = 1000L,
    val maxBackoffMs: Long = 8000L
) {
    val repository: OutboxRepository by lazy {
        val dir = baseDir ?: context?.filesDir ?: File(System.getProperty("java.io.tmpdir"), "frameroom_outbox")
        OutboxRepository(dir)
    }

    private val _records = MutableStateFlow<Map<String, OutboxRecord>>(emptyMap())
    val records = _records.asStateFlow()

    // Backward-compatible flow for existing Compose UI (e.g. BulkConfirmPill in LiveGalleryScreen)
    private val _pendingPhotos = MutableStateFlow<List<DetectedPhoto>>(emptyList())
    val pendingPhotos = _pendingPhotos.asStateFlow()

    private val _outboxStatus = MutableStateFlow("Ready")
    val outboxStatus = _outboxStatus.asStateFlow()

    var isHostAvailable: Boolean = false
        private set
    var hostIp: String? = null
        private set
    var hostPort: Int = 8080
        private set

    // Executor callback invoked to perform network upload
    var uploadExecutor: (suspend (OutboxRecord, ByteArray) -> SyncAck)? = null

    private val activeUploadJobs = mutableMapOf<String, Job>()
    private val lock = Any()

    companion object {
        const val MAX_RETRIES = 5
    }

    init {
        // Restart recovery: recover un-synced items after app restart / process death
        val recovered = repository.recoverRecords()
        synchronized(lock) {
            val map = recovered.associateBy { it.photoId }.toMutableMap()
            _records.value = map
            syncPendingPhotosState()
        }
    }

    fun setHostEndpoint(ip: String?, port: Int) {
        val wasAvailable = isHostAvailable
        hostIp = ip
        hostPort = port
        isHostAvailable = !ip.isNullOrBlank()

        if (!wasAvailable && isHostAvailable) {
            // Host became reachable: transition any WAITING_FOR_HOST or FAILED_RETRYABLE items to QUEUED and resume
            resumeWaitingPhotos()
        } else if (!isHostAvailable) {
            markNonTerminalAsWaitingForHost()
        }
    }

    fun enqueuePhotos(
        photos: List<DetectedPhoto>,
        roomId: String = "local",
        deviceId: String = "local_device"
    ) {
        synchronized(lock) {
            val currentMap = _records.value.toMutableMap()
            for (photo in photos) {
                if (currentMap.containsKey(photo.photoId)) continue

                val thumbPath = repository.saveThumbnail(photo.photoId, photo.thumbnailBytes)
                val initialState = if (isHostAvailable) OutboxState.QUEUED else OutboxState.WAITING_FOR_HOST

                val record = OutboxRecord(
                    photoId = photo.photoId,
                    roomId = roomId,
                    mediaId = photo.mediaId,
                    mediaUri = photo.uri?.toString() ?: "content://media/external/images/media/${photo.mediaId}",
                    deviceId = deviceId,
                    capturedAt = photo.capturedAt,
                    thumbnailPath = thumbPath,
                    cameraModel = photo.cameraModel,
                    exposureTime = photo.exposureTime,
                    iso = photo.iso,
                    focalLength = photo.focalLength,
                    isPrivate = photo.isPrivate,
                    state = initialState
                )
                currentMap[photo.photoId] = record
            }
            _records.value = currentMap
            repository.saveAll(currentMap.values)
            syncPendingPhotosState()
        }

        if (isHostAvailable) {
            triggerQueueProcessing()
        }
    }

    fun addPendingPhotos(photos: List<DetectedPhoto>) {
        enqueuePhotos(photos)
    }

    fun markPrivate(mediaId: Long) {
        synchronized(lock) {
            val currentMap = _records.value.toMutableMap()
            val target = currentMap.values.find { it.mediaId == mediaId }
            if (target != null) {
                currentMap[target.photoId] = target.copy(isPrivate = true, updatedAt = System.currentTimeMillis())
                _records.value = currentMap
                repository.saveAll(currentMap.values)
                syncPendingPhotosState()
            }
        }
    }

    fun updateRecordState(photoId: String, newState: OutboxState, error: String? = null) {
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
            syncPendingPhotosState()
        }
    }

    fun triggerQueueProcessing() {
        if (!isHostAvailable) return

        synchronized(lock) {
            val availableSlots = maxConcurrentUploads - activeUploadJobs.size
            if (availableSlots <= 0) return

            val eligible = _records.value.values.filter {
                !it.isPrivate && (it.state == OutboxState.QUEUED ||
                        it.state == OutboxState.WAITING_FOR_HOST ||
                        it.state == OutboxState.FAILED_RETRYABLE) &&
                        !activeUploadJobs.containsKey(it.photoId)
            }.take(availableSlots)

            for (record in eligible) {
                val photoId = record.photoId
                val job = coroutineScope.launch {
                    try {
                        processSingleRecordWithBackoff(photoId)
                    } finally {
                        synchronized(lock) {
                            activeUploadJobs.remove(photoId)
                        }
                        triggerQueueProcessing()
                    }
                }
                activeUploadJobs[photoId] = job
            }
        }
    }

    private suspend fun processSingleRecordWithBackoff(photoId: String) {
        val record = synchronized(lock) { _records.value[photoId] } ?: return
        if (record.state == OutboxState.ACKED || record.state == OutboxState.FAILED_PERMANENT) return

        // Exponential backoff delay
        val backoff = calculateBackoffMs(record.retryCount)
        if (backoff > 0) {
            delay(backoff)
        }

        val currentRecord = synchronized(lock) { _records.value[photoId] } ?: return
        if (currentRecord.state == OutboxState.ACKED || currentRecord.state == OutboxState.FAILED_PERMANENT) return

        if (!isHostAvailable) {
            updateRecordState(photoId, OutboxState.WAITING_FOR_HOST)
            return
        }

        val thumbBytes = currentRecord.thumbnailPath?.let { repository.readThumbnail(it) }
        if (thumbBytes == null || thumbBytes.isEmpty()) {
            updateRecordState(photoId, OutboxState.FAILED_PERMANENT, "Missing local thumbnail file")
            return
        }

        updateRecordState(photoId, OutboxState.SENDING)

        try {
            val executor = uploadExecutor
            if (executor == null) {
                updateRecordState(photoId, OutboxState.WAITING_FOR_HOST, "No upload executor configured")
                return
            }

            val ack = executor(currentRecord, thumbBytes)
            when (ack.status) {
                SyncAckStatus.STORED,
                SyncAckStatus.DUPLICATE_ACCEPTED -> {
                    synchronized(lock) {
                        val map = _records.value.toMutableMap()
                        val r = map[photoId] ?: currentRecord
                        map[photoId] = r.copy(
                            state = OutboxState.ACKED,
                            lastAttemptAt = System.currentTimeMillis(),
                            lastError = null,
                            updatedAt = System.currentTimeMillis()
                        )
                        _records.value = map
                        repository.saveAll(map.values)
                        syncPendingPhotosState()
                    }
                }
                SyncAckStatus.REJECTED_CLOSED_ROOM,
                SyncAckStatus.REJECTED_INVALID_PAYLOAD -> {
                    synchronized(lock) {
                        val map = _records.value.toMutableMap()
                        val r = map[photoId] ?: currentRecord
                        map[photoId] = r.copy(
                            state = OutboxState.FAILED_PERMANENT,
                            lastAttemptAt = System.currentTimeMillis(),
                            lastError = "Host rejected: ${ack.status}",
                            updatedAt = System.currentTimeMillis()
                        )
                        _records.value = map
                        repository.saveAll(map.values)
                        syncPendingPhotosState()
                    }
                }
            }
        } catch (e: Exception) {
            val nextRetry = currentRecord.retryCount + 1
            val isHostDown = e is java.net.ConnectException ||
                    e is java.net.SocketTimeoutException ||
                    e is java.net.UnknownHostException

            synchronized(lock) {
                val map = _records.value.toMutableMap()
                val r = map[photoId] ?: currentRecord
                if (isHostDown) {
                    map[photoId] = r.copy(
                        state = OutboxState.WAITING_FOR_HOST,
                        retryCount = nextRetry,
                        lastAttemptAt = System.currentTimeMillis(),
                        lastError = "Host unreachable: ${e.message}",
                        updatedAt = System.currentTimeMillis()
                    )
                } else if (nextRetry >= MAX_RETRIES) {
                    map[photoId] = r.copy(
                        state = OutboxState.FAILED_PERMANENT,
                        retryCount = nextRetry,
                        lastAttemptAt = System.currentTimeMillis(),
                        lastError = "Max retries ($MAX_RETRIES) exceeded: ${e.message}",
                        updatedAt = System.currentTimeMillis()
                    )
                } else {
                    map[photoId] = r.copy(
                        state = OutboxState.FAILED_RETRYABLE,
                        retryCount = nextRetry,
                        lastAttemptAt = System.currentTimeMillis(),
                        lastError = e.message,
                        updatedAt = System.currentTimeMillis()
                    )
                }
                _records.value = map
                repository.saveAll(map.values)
                syncPendingPhotosState()
            }
        }
    }

    fun resumeWaitingPhotos() {
        synchronized(lock) {
            val map = _records.value.toMutableMap()
            var changed = false
            for ((id, record) in map) {
                if (record.state == OutboxState.WAITING_FOR_HOST || record.state == OutboxState.FAILED_RETRYABLE) {
                    map[id] = record.copy(state = OutboxState.QUEUED, updatedAt = System.currentTimeMillis())
                    changed = true
                }
            }
            if (changed) {
                _records.value = map
                repository.saveAll(map.values)
                syncPendingPhotosState()
            }
        }
        triggerQueueProcessing()
    }

    fun markNonTerminalAsWaitingForHost() {
        synchronized(lock) {
            for (job in activeUploadJobs.values) {
                job.cancel()
            }
            activeUploadJobs.clear()

            val map = _records.value.toMutableMap()
            var changed = false
            for ((id, record) in map) {
                if (record.state == OutboxState.QUEUED || record.state == OutboxState.SENDING) {
                    map[id] = record.copy(state = OutboxState.WAITING_FOR_HOST, updatedAt = System.currentTimeMillis())
                    changed = true
                }
            }
            if (changed) {
                _records.value = map
                repository.saveAll(map.values)
                syncPendingPhotosState()
            }
        }
    }

    private fun syncPendingPhotosState() {
        val recordsList = _records.value.values.toList()
        val unacked = recordsList.filter {
            !it.isPrivate && it.state != OutboxState.ACKED && it.state != OutboxState.FAILED_PERMANENT
        }
        _pendingPhotos.value = unacked.map { record ->
            DetectedPhoto(
                photoId = record.photoId,
                uri = try { Uri.parse(record.mediaUri) } catch (t: Throwable) { null },
                mediaId = record.mediaId,
                capturedAt = record.capturedAt,
                thumbnailBytes = record.thumbnailPath?.let { repository.readThumbnail(it) } ?: ByteArray(0),
                cameraModel = record.cameraModel,
                exposureTime = record.exposureTime,
                iso = record.iso,
                focalLength = record.focalLength,
                isPrivate = record.isPrivate
            )
        }

        _outboxStatus.value = when {
            recordsList.any { it.state == OutboxState.SENDING } -> "SYNCING"
            recordsList.any { it.state == OutboxState.FAILED_RETRYABLE } -> "RETRYING"
            recordsList.any { it.state == OutboxState.WAITING_FOR_HOST } -> "WAITING FOR ROOM"
            recordsList.any { it.state == OutboxState.QUEUED } -> "QUEUED"
            recordsList.any { it.state == OutboxState.FAILED_PERMANENT } -> "FAILED"
            recordsList.isNotEmpty() && recordsList.all { it.state == OutboxState.ACKED } -> "SYNCED"
            else -> "Ready"
        }
    }

    suspend fun confirmAndUploadAll(uploadAction: (suspend (DetectedPhoto) -> Unit)? = null) {
        triggerQueueProcessing()
    }

    fun calculateBackoffMs(retryCount: Int): Long {
        if (retryCount <= 0) return 0L
        val shift = (retryCount - 1).coerceAtMost(3)
        val delayMs = baseBackoffMs * (1L shl shift)
        return delayMs.coerceAtMost(maxBackoffMs)
    }

    fun activeJobCount(): Int = synchronized(lock) { activeUploadJobs.size }

    fun dismissAll() {
        synchronized(lock) {
            for (job in activeUploadJobs.values) {
                job.cancel()
            }
            activeUploadJobs.clear()
            _records.value = emptyMap()
            repository.clear()
            _pendingPhotos.value = emptyList()
            _outboxStatus.value = "Ready"
        }
    }

    fun clear() {
        dismissAll()
    }
}
