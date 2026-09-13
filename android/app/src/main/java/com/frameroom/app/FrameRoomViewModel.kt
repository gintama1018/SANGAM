package com.frameroom.app

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.frameroom.app.client.FrameRoomClient
import com.frameroom.app.core.CryptoManager
import com.frameroom.app.core.NetworkUtils
import com.frameroom.app.core.Participant
import com.frameroom.app.core.PhotoIdentity
import com.frameroom.app.core.PhotoMeta
import com.frameroom.app.core.QRPayload
import com.frameroom.app.core.QRGenerator
import com.frameroom.app.core.ReactionResponse
import com.frameroom.app.core.Room
import com.frameroom.app.core.SyncAckStatus
import com.frameroom.app.core.WebSocketEvent
import com.frameroom.app.server.FrameRoomServer
import com.frameroom.app.server.HostServerService
import com.frameroom.app.online.OnlineUploadManager
import com.frameroom.app.online.OnlineUploadState
import com.frameroom.app.watcher.DetectedPhoto
import com.frameroom.app.watcher.GalleryContentObserver
import com.frameroom.app.watcher.OutboxState
import com.frameroom.app.watcher.SyncQueueManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

enum class AppScreen {
    WELCOME,
    HOST_ROOM,
    JOIN_SCANNER,
    GALLERY,
    MY_UPLOADS,
    PARTICIPANTS,
    PHOTO_DETAIL,
    SETTINGS
}

class FrameRoomViewModel(application: Application) : AndroidViewModel(application) {

    private val json = Json { ignoreUnknownKeys = true }
    val deviceId: String = Settings.Secure.getString(application.contentResolver, Settings.Secure.ANDROID_ID)
        ?: UUID.randomUUID().toString()

    private val server = FrameRoomServer(application)
    private val client = FrameRoomClient()
    private val syncQueueManager = SyncQueueManager(
        context = application,
        coroutineScope = viewModelScope
    ).apply {
        uploadExecutor = { record, thumbBytes ->
            val targetIp = _hostIp.value ?: throw java.net.ConnectException("Host IP is not set")
            val token = _sessionToken.value ?: _room.value?.sessionToken
            val result = client.uploadThumbnail(
                hostIp = targetIp,
                port = hostPort,
                photoId = record.photoId,
                roomId = record.roomId,
                thumbnailBytes = thumbBytes,
                uploaderDeviceId = record.deviceId,
                uploaderName = userDisplayName,
                capturedAt = record.capturedAt,
                cameraModel = record.cameraModel,
                exposure = record.exposureTime,
                iso = record.iso,
                focalLength = record.focalLength,
                sessionToken = token
            )
            result.getOrThrow()
        }
    }
    private var galleryObserver: GalleryContentObserver? = null

    private var myKeyPair = CryptoManager.generateKeyPair()

    // Navigation & UI state
    private val _currentScreen = MutableStateFlow(AppScreen.WELCOME)
    val currentScreen = _currentScreen.asStateFlow()

    private val _room = MutableStateFlow<Room?>(null)
    val room = _room.asStateFlow()

    private val _sessionToken = MutableStateFlow<String?>(null)
    val sessionToken = _sessionToken.asStateFlow()

    private val _isHost = MutableStateFlow(false)
    val isHost = _isHost.asStateFlow()

    private val _hostIp = MutableStateFlow<String?>(null)
    val hostIp = _hostIp.asStateFlow()

    val hostPort = 8080

    private val _qrBitmap = MutableStateFlow<Bitmap?>(null)
    val qrBitmap = _qrBitmap.asStateFlow()

    private val _participants = MutableStateFlow<List<Participant>>(emptyList())
    val participants = _participants.asStateFlow()

    private val _photos = MutableStateFlow<List<PhotoMeta>>(emptyList())
    val photos = _photos.asStateFlow()

    private val _likedPhotoIds = MutableStateFlow<Set<String>>(emptySet())
    val likedPhotoIds = _likedPhotoIds.asStateFlow()

    private val _selectedPhoto = MutableStateFlow<PhotoMeta?>(null)
    val selectedPhoto = _selectedPhoto.asStateFlow()

    val pendingPhotos = syncQueueManager.pendingPhotos
    val outboxRecords = syncQueueManager.records
    val outboxStatus = syncQueueManager.outboxStatus

    // Pipeline B: Optional Online Upload Subsystem
    val onlineUploadManager = OnlineUploadManager(
        context = application,
        coroutineScope = viewModelScope
    )
    val onlineUploadRecords = onlineUploadManager.records
    val onlineUploadStatus = onlineUploadManager.queueStatus

    private val _statusMessage = MutableStateFlow<String?>("Ready")
    val statusMessage = _statusMessage.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    var userDisplayName: String = "Attendee"

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    /**
     * Creates a new room as Host.
     */
    fun createRoom(roomName: String, displayName: String) {
        userDisplayName = displayName.ifBlank { "Host" }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val ip = NetworkUtils.getLocalIpAddress() ?: "127.0.0.1"
                _hostIp.value = ip
                _isHost.value = true
                syncQueueManager.setHostEndpoint(ip, hostPort)

                myKeyPair = CryptoManager.generateKeyPair()
                val createdRoom = server.start(
                    roomName = roomName,
                    hostDeviceId = deviceId,
                    hostDisplayName = userDisplayName,
                    hostPublicKey = myKeyPair.publicKeyBase64,
                    port = hostPort
                )
                _room.value = createdRoom
                _sessionToken.value = createdRoom.sessionToken
                client.sessionToken = createdRoom.sessionToken
                _participants.value = server.participants.value
                _photos.value = emptyList()

                // Generate scannable QR containing fr://{ip}:{port}/{roomId}/{publicKey}/{sessionToken}
                val qrPayload = QRPayload(ip, hostPort, createdRoom.roomId, myKeyPair.publicKeyBase64, createdRoom.sessionToken)
                val qrBmp = QRGenerator.generateQRCodeBitmap(qrPayload.toUrl())
                _qrBitmap.value = qrBmp

                // Start Foreground Service so host stays alive
                HostServerService.start(getApplication(), roomName)

                // Start observing gallery for host's own photos too
                startGalleryWatcher(createdRoom.activeWindowStart)

                withContext(Dispatchers.Main) {
                    _currentScreen.value = AppScreen.HOST_ROOM
                    _statusMessage.value = "Room ${createdRoom.roomId} is live at $ip:$hostPort"
                }

                // Collect server updates
                launch {
                    server.photos.collect { _photos.value = it }
                }
                launch {
                    server.participants.collect { _participants.value = it }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to create room: ${e.message}"
            }
        }
    }

    /**
     * Joins an existing room by scanning QR.
     */
    fun joinWithQrPayload(rawPayload: String, displayName: String) {
        userDisplayName = displayName.ifBlank { "Guest" }
        val payload = QRPayload.parse(rawPayload)
        if (payload == null) {
            _errorMessage.value = "Invalid FrameRoom QR code"
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                _hostIp.value = payload.hostIp
                _isHost.value = false
                _sessionToken.value = payload.sessionToken
                client.sessionToken = payload.sessionToken
                syncQueueManager.setHostEndpoint(payload.hostIp, payload.port)

                myKeyPair = CryptoManager.generateKeyPair()
                val joinResult = client.joinRoom(
                    hostIp = payload.hostIp,
                    port = payload.port,
                    deviceId = deviceId,
                    displayName = userDisplayName,
                    clientPublicKey = myKeyPair.publicKeyBase64,
                    sessionToken = payload.sessionToken
                )

                if (joinResult.isSuccess) {
                    val resp = joinResult.getOrThrow()
                    _room.value = resp.room
                    _participants.value = resp.participants
                    _photos.value = resp.photos

                    // Start live WebSocket stream
                    client.startWebSocket(
                        hostIp = payload.hostIp,
                        port = payload.port,
                        scope = viewModelScope,
                        sessionToken = payload.sessionToken,
                        onEvent = { event -> handleWebSocketEvent(event) }
                    )

                    // Start gallery watcher for attendee
                    startGalleryWatcher(resp.room.activeWindowStart)

                    withContext(Dispatchers.Main) {
                        _currentScreen.value = AppScreen.GALLERY
                        _statusMessage.value = "Connected to ${resp.room.name}"
                    }
                } else {
                    _errorMessage.value = "Join failed: ${joinResult.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Connection error: ${e.message}"
            }
        }
    }

    private fun handleWebSocketEvent(event: WebSocketEvent) {
        when (event.type) {
            WebSocketEvent.TYPE_NEW_PHOTO -> {
                try {
                    val photo = json.decodeFromString<PhotoMeta>(event.payload)
                    _photos.value = listOf(photo) + _photos.value.filter { it.photoId != photo.photoId }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            WebSocketEvent.TYPE_REACTION_UPDATED -> {
                try {
                    val resp = json.decodeFromString<ReactionResponse>(event.payload)
                    _photos.value = _photos.value.map {
                        if (it.photoId == resp.photoId) it.copy(reactionCount = resp.newCount) else it
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            WebSocketEvent.TYPE_PARTICIPANT_JOINED -> {
                try {
                    val p = json.decodeFromString<Participant>(event.payload)
                    _participants.value = _participants.value.filter { it.deviceId != p.deviceId } + p
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            WebSocketEvent.TYPE_ROOM_CLOSED -> {
                galleryObserver?.stopWatching()
                _statusMessage.value = "The event room has been closed by host"
            }
        }
    }

    private fun startGalleryWatcher(activeWindowStartMs: Long) {
        galleryObserver?.stopWatching()
        galleryObserver = GalleryContentObserver(
            context = getApplication(),
            coroutineScope = viewModelScope,
            onPhotosDetected = { detected ->
                syncQueueManager.enqueuePhotos(
                    photos = detected,
                    roomId = _room.value?.roomId ?: "local",
                    deviceId = deviceId
                )
            }
        ).apply {
            startWatching(
                eventStartMs = activeWindowStartMs,
                roomId = _room.value?.roomId ?: "local",
                deviceId = deviceId
            )
        }
    }

    /**
     * User taps "Sync Now" on BulkConfirmPill.
     */
    fun confirmPendingPhotos() {
        syncQueueManager.triggerQueueProcessing()
    }

    fun dismissPendingPhotos() {
        syncQueueManager.dismissAll()
    }

    /**
     * Fallback manual upload if user picks photos from system gallery.
     * Always routes through SyncQueueManager to guarantee durability.
     */
    fun uploadManualPhotos(uris: List<Uri>) {
        val currentRoomId = _room.value?.roomId ?: "manual"
        val resolver = getApplication<Application>().contentResolver

        viewModelScope.launch(Dispatchers.IO) {
            val detectedList = mutableListOf<DetectedPhoto>()
            for (uri in uris) {
                try {
                    val stream = resolver.openInputStream(uri) ?: continue
                    val bitmap = BitmapFactory.decodeStream(stream) ?: continue
                    val bos = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 75, bos)
                    val bytes = bos.toByteArray()
                    bitmap.recycle()

                    val capturedAt = System.currentTimeMillis()
                    val mediaId = try {
                        android.content.ContentUris.parseId(uri)
                    } catch (e: Exception) {
                        capturedAt
                    }
                    val photoId = PhotoIdentity.generatePhotoId(currentRoomId, deviceId, mediaId, capturedAt)

                    detectedList.add(
                        DetectedPhoto(
                            photoId = photoId,
                            uri = uri,
                            mediaId = mediaId,
                            capturedAt = capturedAt,
                            thumbnailBytes = bytes
                        )
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            if (detectedList.isNotEmpty()) {
                syncQueueManager.enqueuePhotos(
                    photos = detectedList,
                    roomId = currentRoomId,
                    deviceId = deviceId
                )
            }
        }
    }

    fun toggleReaction(photoId: String) {
        val targetIp = _hostIp.value ?: return
        val token = _sessionToken.value ?: _room.value?.sessionToken
        viewModelScope.launch(Dispatchers.IO) {
            val result = client.toggleReaction(targetIp, hostPort, photoId, deviceId, sessionToken = token)
            if (result.isSuccess) {
                val resp = result.getOrThrow()
                _likedPhotoIds.value = if (resp.isReacted) {
                    _likedPhotoIds.value + photoId
                } else {
                    _likedPhotoIds.value - photoId
                }
            }
        }
    }

    fun selectPhotoForDetail(photo: PhotoMeta?) {
        _selectedPhoto.value = photo
        if (photo != null) {
            _currentScreen.value = AppScreen.PHOTO_DETAIL
        }
    }

    fun closeRoom() {
        val targetIp = _hostIp.value ?: return
        val token = _sessionToken.value ?: _room.value?.sessionToken
        viewModelScope.launch(Dispatchers.IO) {
            client.closeRoom(targetIp, hostPort, deviceId, sessionToken = token)
            HostServerService.stop(getApplication())
            galleryObserver?.stopWatching()
            syncQueueManager.setHostEndpoint(null, hostPort)
            withContext(Dispatchers.Main) {
                _statusMessage.value = "Room closed and frozen to archive"
            }
        }
    }

    fun purgeCache() {
        viewModelScope.launch(Dispatchers.IO) {
            val cacheDir = File(getApplication<Application>().filesDir, "photos")
            cacheDir.deleteRecursively()
            _photos.value = emptyList()
            _statusMessage.value = "Local photo cache purged"
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Pipeline B: Explicit user-initiated online upload.
     * Never triggered automatically. Only selected photos enter this queue.
     */
    fun uploadSelectedOnline(photoIds: List<String>) {
        val currentRoomId = _room.value?.roomId ?: "event"
        val outbox = syncQueueManager.records.value

        for (photoId in photoIds) {
            val localRecord = outbox[photoId]
            val mediaId = localRecord?.mediaId ?: System.currentTimeMillis()
            val mediaUri = localRecord?.mediaUri ?: ""
            val originalPath = localRecord?.thumbnailPath
            val thumbPath = localRecord?.thumbnailPath

            onlineUploadManager.enqueueForUpload(
                photoId = photoId,
                roomId = currentRoomId,
                mediaId = mediaId,
                mediaUri = mediaUri,
                originalPath = originalPath,
                thumbnailPath = thumbPath
            )
        }
        _statusMessage.value = "Selected ${photoIds.size} photo(s) queued for online upload"
    }

    fun retryOnlineUpload(photoId: String) {
        onlineUploadManager.retryUpload(photoId)
    }

    fun dismissOnlineUpload(photoId: String) {
        onlineUploadManager.dismissUpload(photoId)
    }

    /**
     * Returns paired statuses: (Local Status, Optional Online Status).
     * Online failure never impacts Local status.
     */
    fun getPhotoSyncStatus(photoId: String): Pair<String, String?> {
        val localState = syncQueueManager.records.value[photoId]?.state
        val onlineState = onlineUploadManager.records.value[photoId]?.state

        val localLabel = when (localState) {
            OutboxState.ACKED -> "LOCAL SYNCED"
            OutboxState.SENDING -> "LOCAL SYNCING"
            OutboxState.QUEUED -> "LOCAL QUEUED"
            OutboxState.WAITING_FOR_HOST -> "LOCAL WAITING"
            OutboxState.FAILED_RETRYABLE -> "LOCAL RETRYING"
            OutboxState.FAILED_PERMANENT -> "LOCAL FAILED"
            else -> "LOCAL READY"
        }

        val onlineLabel = when (onlineState) {
            OnlineUploadState.UPLOADED -> "ONLINE UPLOADED"
            OnlineUploadState.UPLOADING -> "ONLINE UPLOADING"
            OnlineUploadState.QUEUED,
            OnlineUploadState.WAITING_FOR_INTERNET -> "ONLINE PENDING"
            OnlineUploadState.FAILED_RETRYABLE,
            OnlineUploadState.FAILED_PERMANENT -> "ONLINE FAILED"
            else -> null // Not selected for online upload
        }

        return Pair(localLabel, onlineLabel)
    }

    override fun onCleared() {
        super.onCleared()
        galleryObserver?.stopWatching()
        client.close()
        server.stop()
    }
}
