package com.frameroom.app.server

import android.content.Context
import com.frameroom.app.core.JoinRequest
import com.frameroom.app.core.JoinResponse
import com.frameroom.app.core.Participant
import com.frameroom.app.core.ParticipantRole
import com.frameroom.app.core.PhotoMeta
import com.frameroom.app.core.ReactionRequest
import com.frameroom.app.core.ReactionResponse
import com.frameroom.app.core.Room
import com.frameroom.app.core.WebSocketEvent
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.request.receiveChannel
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.util.cio.toByteArray
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.frameroom.app.core.PhotoIdentity
import com.frameroom.app.core.SyncAck
import com.frameroom.app.core.SyncAckStatus
import java.io.File
import java.util.Collections
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class FrameRoomServer(
    private val context: Context? = null,
    baseDir: File? = null
) {
    companion object {
        val VALID_PHOTO_ID_REGEX = Regex("^[a-zA-Z0-9_]{1,128}$")
        const val MAX_FULL_RES_BYTES = 20 * 1024 * 1024L // 20 MB cap
        const val MAX_THUMBNAIL_BYTES = 500 * 1024L      // 500 KB cap
    }

    private var serverEngine: ApplicationEngine? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    private val _room = MutableStateFlow<Room?>(null)
    val room = _room.asStateFlow()

    private val _participants = MutableStateFlow<List<Participant>>(emptyList())
    val participants = _participants.asStateFlow()

    private val _photos = MutableStateFlow<List<PhotoMeta>>(emptyList())
    val photos = _photos.asStateFlow()

    // photoId -> Set of deviceIds who reacted
    private val reactionsMap = ConcurrentHashMap<String, MutableSet<String>>()

    private val activeSessions = Collections.synchronizedSet(mutableSetOf<DefaultWebSocketServerSession>())

    private val storageDir by lazy {
        baseDir ?: context?.filesDir ?: File(System.getProperty("java.io.tmpdir"), "frameroom_test")
    }

    private val thumbnailsDir by lazy {
        File(storageDir, "photos/thumbnails").apply { mkdirs() }
    }

    private val originalsDir by lazy {
        File(storageDir, "photos/originals").apply { mkdirs() }
    }

    fun start(
        roomName: String,
        hostDeviceId: String,
        hostDisplayName: String,
        hostPublicKey: String,
        port: Int = 8080,
        sessionToken: String? = null,
        hostSecret: String? = null
    ): Room {
        stop()

        val token = sessionToken ?: java.util.UUID.randomUUID().toString()
        val secret = hostSecret ?: ("sec_" + java.util.UUID.randomUUID().toString())
        val roomId = "FR-" + (1000..9999).random()
        val createdRoom = Room(
            roomId = roomId,
            name = roomName,
            activeWindowStart = System.currentTimeMillis(),
            hostDeviceId = hostDeviceId,
            hostPublicKey = hostPublicKey,
            sessionToken = token,
            hostSecret = secret
        )
        _room.value = createdRoom

        val hostParticipant = Participant(
            deviceId = hostDeviceId,
            displayName = hostDisplayName,
            joinedAt = System.currentTimeMillis(),
            role = ParticipantRole.HOST
        )
        _participants.value = listOf(hostParticipant)
        _photos.value = emptyList()
        reactionsMap.clear()

        serverEngine = embeddedServer(CIO, port = port) {
            install(ContentNegotiation) {
                json(json)
            }
            install(WebSockets)

            routing {
                // Handshake & Join
                post("/api/join") {
                    val incomingToken = call.request.headers["X-Session-Token"]
                    if (incomingToken != _room.value?.sessionToken) {
                        call.respond(HttpStatusCode.Unauthorized, "Invalid or missing session token")
                        return@post
                    }

                    val currentRoom = _room.value
                    if (currentRoom == null || currentRoom.closedAt != null) {
                        call.respond(HttpStatusCode.Forbidden, "Room is not active")
                        return@post
                    }

                    val req = call.receive<JoinRequest>()
                    synchronized(_participants) {
                        val existing = _participants.value.find { it.deviceId == req.deviceId }
                        if (existing == null) {
                            val newGuest = Participant(
                                deviceId = req.deviceId,
                                displayName = req.displayName,
                                joinedAt = System.currentTimeMillis(),
                                role = ParticipantRole.GUEST
                            )
                            _participants.value = _participants.value + newGuest
                            broadcastEvent(WebSocketEvent.TYPE_PARTICIPANT_JOINED, json.encodeToString(newGuest))
                        }
                    }

                    call.respond(
                        JoinResponse(
                            room = currentRoom.toPublicRoom(),
                            participants = _participants.value,
                            photos = _photos.value
                        )
                    )
                }

                // Thumbnail Upload
                post("/api/photo/thumbnail") {
                    val incomingToken = call.request.headers["X-Session-Token"]
                    if (incomingToken != _room.value?.sessionToken) {
                        call.respond(HttpStatusCode.Unauthorized, "Invalid or missing session token")
                        return@post
                    }

                    val photoIdHeader = call.request.headers["X-Photo-Id"]
                    val roomIdHeader = call.request.headers["X-Room-Id"]
                    val uploaderDeviceId = call.request.headers["X-Device-Id"] ?: "unknown"
                    val uploaderName = call.request.headers["X-Uploader-Name"] ?: "Guest"
                    val capturedAt = call.request.headers["X-Captured-At"]?.toLongOrNull() ?: System.currentTimeMillis()
                    val cameraModel = call.request.headers["X-Camera-Model"]
                    val exposure = call.request.headers["X-Exposure"]
                    val iso = call.request.headers["X-ISO"]
                    val focalLength = call.request.headers["X-Focal-Length"]

                    val currentRoom = _room.value
                    val resolvedRoomId = roomIdHeader ?: currentRoom?.roomId ?: "unknown"

                    val photoId = if (!photoIdHeader.isNullOrBlank()) {
                        photoIdHeader
                    } else {
                        PhotoIdentity.generatePhotoId(resolvedRoomId, uploaderDeviceId, System.currentTimeMillis(), capturedAt)
                    }

                    if (!VALID_PHOTO_ID_REGEX.matches(photoId)) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            SyncAck(
                                photoId = photoId,
                                status = SyncAckStatus.REJECTED_INVALID_PAYLOAD,
                                serverTimestamp = System.currentTimeMillis()
                            )
                        )
                        return@post
                    }

                    if (currentRoom == null || currentRoom.closedAt != null) {
                        call.respond(
                            HttpStatusCode.Forbidden,
                            SyncAck(
                                photoId = photoId,
                                status = SyncAckStatus.REJECTED_CLOSED_ROOM,
                                serverTimestamp = System.currentTimeMillis()
                            )
                        )
                        return@post
                    }

                    val declaredLength = call.request.headers["Content-Length"]?.toLongOrNull()
                    if (declaredLength != null && declaredLength > MAX_THUMBNAIL_BYTES) {
                        call.respond(
                            HttpStatusCode.PayloadTooLarge,
                            SyncAck(
                                photoId = photoId,
                                status = SyncAckStatus.REJECTED_INVALID_PAYLOAD,
                                serverTimestamp = System.currentTimeMillis()
                            )
                        )
                        return@post
                    }

                    val channel = call.receiveChannel()
                    val bytes = channel.toByteArray()

                    // Validate MIME JPEG signature (0xFF, 0xD8) and size cap (max 500 KB)
                    if (bytes.size < 4 || bytes[0] != 0xFF.toByte() || bytes[1] != 0xD8.toByte() || bytes.size > MAX_THUMBNAIL_BYTES) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            SyncAck(
                                photoId = photoId,
                                status = SyncAckStatus.REJECTED_INVALID_PAYLOAD,
                                serverTimestamp = System.currentTimeMillis()
                            )
                        )
                        return@post
                    }

                    // Deduplication check: return DUPLICATE_ACCEPTED if already present
                    val isDuplicate = synchronized(_photos) {
                        _photos.value.any { it.photoId == photoId }
                    }

                    if (isDuplicate) {
                        call.respond(
                            HttpStatusCode.OK,
                            SyncAck(
                                photoId = photoId,
                                status = SyncAckStatus.DUPLICATE_ACCEPTED,
                                serverTimestamp = System.currentTimeMillis()
                            )
                        )
                        return@post
                    }

                    val thumbFile = File(thumbnailsDir, "$photoId.jpg")
                    if (!thumbFile.canonicalPath.startsWith(thumbnailsDir.canonicalPath)) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            SyncAck(
                                photoId = photoId,
                                status = SyncAckStatus.REJECTED_INVALID_PAYLOAD,
                                serverTimestamp = System.currentTimeMillis()
                            )
                        )
                        return@post
                    }
                    thumbFile.writeBytes(bytes)

                    val meta = PhotoMeta(
                        photoId = photoId,
                        uploaderDeviceId = uploaderDeviceId,
                        uploaderName = uploaderName,
                        capturedAt = capturedAt,
                        reactionCount = 0,
                        cameraModel = cameraModel,
                        exposureTime = exposure,
                        iso = iso,
                        focalLength = focalLength
                    )

                    synchronized(_photos) {
                        _photos.value = listOf(meta) + _photos.value
                    }

                    // Increment participant photo count
                    synchronized(_participants) {
                        _participants.value = _participants.value.map {
                            if (it.deviceId == uploaderDeviceId) it.copy(photoCount = it.photoCount + 1) else it
                        }
                    }

                    broadcastEvent(WebSocketEvent.TYPE_NEW_PHOTO, json.encodeToString(meta))
                    call.respond(
                        HttpStatusCode.Created,
                        SyncAck(
                            photoId = photoId,
                            status = SyncAckStatus.STORED,
                            serverTimestamp = System.currentTimeMillis()
                        )
                    )
                }

                // Get Thumbnail
                get("/api/photo/{id}/thumbnail") {
                    val incomingToken = call.request.headers["X-Session-Token"] ?: call.request.queryParameters["token"]
                    if (incomingToken != _room.value?.sessionToken) {
                        call.respond(HttpStatusCode.Unauthorized, "Invalid or missing session token")
                        return@get
                    }
                    val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                    if (!VALID_PHOTO_ID_REGEX.matches(id)) {
                        call.respond(HttpStatusCode.BadRequest, "Invalid photo ID format")
                        return@get
                    }
                    val file = File(thumbnailsDir, "$id.jpg")
                    if (!file.exists()) {
                        call.respond(HttpStatusCode.NotFound)
                    } else {
                        call.respondBytes(file.readBytes(), ContentType.Image.JPEG)
                    }
                }

                // Upload Full-Res
                post("/api/photo/{id}/full") {
                    val incomingToken = call.request.headers["X-Session-Token"]
                    if (incomingToken != _room.value?.sessionToken) {
                        call.respond(HttpStatusCode.Unauthorized, "Invalid or missing session token")
                        return@post
                    }
                    val id = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                    if (!VALID_PHOTO_ID_REGEX.matches(id)) {
                        call.respond(HttpStatusCode.BadRequest, "Invalid photo ID format")
                        return@post
                    }
                    val currentRoom = _room.value
                    if (currentRoom == null || currentRoom.closedAt != null) {
                        call.respond(HttpStatusCode.Forbidden, "Room is closed")
                        return@post
                    }

                    // 1. Quick-reject via Content-Length header if declared larger than 20MB
                    val declaredLength = call.request.headers["Content-Length"]?.toLongOrNull()
                    if (declaredLength != null && declaredLength > MAX_FULL_RES_BYTES) {
                        call.respond(HttpStatusCode.PayloadTooLarge, "Declared payload size exceeds 20MB limit")
                        return@post
                    }

                    val channel = call.receiveChannel()
                    val bytes = channel.toByteArray()

                    // 2. Definitive check on actual received byte count
                    if (bytes.size > MAX_FULL_RES_BYTES) {
                        call.respond(HttpStatusCode.PayloadTooLarge, "Payload size exceeds 20MB limit")
                        return@post
                    }

                    // 3. Validate JPEG magic bytes signature (0xFF, 0xD8)
                    if (bytes.size < 4 || bytes[0] != 0xFF.toByte() || bytes[1] != 0xD8.toByte()) {
                        call.respond(HttpStatusCode.BadRequest, "Invalid image format (expected JPEG)")
                        return@post
                    }

                    val file = File(originalsDir, "$id.jpg")
                    if (!file.canonicalPath.startsWith(originalsDir.canonicalPath)) {
                        call.respond(HttpStatusCode.BadRequest, "Invalid photo ID format")
                        return@post
                    }
                    file.writeBytes(bytes)
                    call.respond(HttpStatusCode.OK, "Stored full-res")
                }

                // Get Full-Res
                get("/api/photo/{id}/full") {
                    val incomingToken = call.request.headers["X-Session-Token"] ?: call.request.queryParameters["token"]
                    if (incomingToken != _room.value?.sessionToken) {
                        call.respond(HttpStatusCode.Unauthorized, "Invalid or missing session token")
                        return@get
                    }
                    val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                    if (!VALID_PHOTO_ID_REGEX.matches(id)) {
                        call.respond(HttpStatusCode.BadRequest, "Invalid photo ID format")
                        return@get
                    }
                    val original = File(originalsDir, "$id.jpg")
                    if (original.exists()) {
                        call.respondBytes(original.readBytes(), ContentType.Image.JPEG)
                    } else {
                        // Fallback to thumbnail if original not yet uploaded to host
                        val thumb = File(thumbnailsDir, "$id.jpg")
                        if (thumb.exists()) {
                            call.respondBytes(thumb.readBytes(), ContentType.Image.JPEG)
                        } else {
                            call.respond(HttpStatusCode.NotFound)
                        }
                    }
                }

                // Toggle Reaction
                post("/api/photo/{id}/react") {
                    val incomingToken = call.request.headers["X-Session-Token"]
                    if (incomingToken != _room.value?.sessionToken) {
                        call.respond(HttpStatusCode.Unauthorized, "Invalid or missing session token")
                        return@post
                    }
                    val id = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                    if (!VALID_PHOTO_ID_REGEX.matches(id)) {
                        call.respond(HttpStatusCode.BadRequest, "Invalid photo ID format")
                        return@post
                    }
                    val req = call.receive<ReactionRequest>()
                    val userSet = reactionsMap.computeIfAbsent(id) { Collections.synchronizedSet(mutableSetOf()) }

                    val isReacted = synchronized(userSet) {
                        if (userSet.contains(req.deviceId)) {
                            userSet.remove(req.deviceId)
                            false
                        } else {
                            userSet.add(req.deviceId)
                            true
                        }
                    }
                    val newCount = userSet.size

                    synchronized(_photos) {
                        _photos.value = _photos.value.map {
                            if (it.photoId == id) it.copy(reactionCount = newCount) else it
                        }
                    }

                    val resp = ReactionResponse(id, newCount, isReacted)
                    broadcastEvent(WebSocketEvent.TYPE_REACTION_UPDATED, json.encodeToString(resp))
                    call.respond(resp)
                }

                // Host Close Room
                post("/api/room/close") {
                    val incomingToken = call.request.headers["X-Session-Token"]
                    if (incomingToken != _room.value?.sessionToken) {
                        call.respond(HttpStatusCode.Unauthorized, "Invalid or missing session token")
                        return@post
                    }
                    val incomingSecret = call.request.headers["X-Host-Secret"]
                    val currentRoom = _room.value
                    if (currentRoom == null || incomingSecret.isNullOrBlank() || incomingSecret != currentRoom.hostSecret) {
                        call.respond(HttpStatusCode.Unauthorized, "Only the host with valid secret can close this room")
                        return@post
                    }

                    val closed = currentRoom.copy(closedAt = System.currentTimeMillis())
                    _room.value = closed
                    broadcastEvent(WebSocketEvent.TYPE_ROOM_CLOSED, json.encodeToString(closed.toPublicRoom()))
                    call.respond(closed.toPublicRoom())
                }

                // Embedded Spectator Web Page (Stretch Goal - Live Projector Wall)
                get("/") {
                    val token = _room.value?.sessionToken ?: ""
                    call.respondText(generateSpectatorHtml(token), ContentType.Text.Html)
                }

                // Live WebSocket
                webSocket("/ws") {
                    val incomingToken = call.request.headers["X-Session-Token"] ?: call.request.queryParameters["token"]
                    if (incomingToken != _room.value?.sessionToken) {
                        close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Unauthorized session token"))
                        return@webSocket
                    }
                    activeSessions.add(this)
                    try {
                        for (frame in incoming) {
                            if (frame is Frame.Text) {
                                val text = frame.readText()
                                // Client ping or heartbeat
                                if (text == "ping") {
                                    send(Frame.Text("pong"))
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // Client disconnected
                    } finally {
                        activeSessions.remove(this)
                    }
                }
            }
        }.start(wait = false)

        return createdRoom
    }

    private fun broadcastEvent(type: String, payload: String) {
        val message = WebSocketEvent(type, payload)
        val text = json.encodeToString(message)
        scope.launch {
            val sessions = synchronized(activeSessions) { activeSessions.toList() }
            val deadSessions = mutableListOf<DefaultWebSocketServerSession>()
            for (session in sessions) {
                try {
                    session.send(Frame.Text(text))
                } catch (e: Exception) {
                    deadSessions.add(session)
                }
            }
            if (deadSessions.isNotEmpty()) {
                synchronized(activeSessions) {
                    activeSessions.removeAll(deadSessions)
                }
            }
        }
    }

    fun stop() {
        serverEngine?.stop(gracePeriodMillis = 500, timeoutMillis = 1500)
        serverEngine = null
        _room.value = null
        _participants.value = emptyList()
        _photos.value = emptyList()
        reactionsMap.clear()
        activeSessions.clear()
    }

    fun getThumbnailFile(photoId: String): File {
        return File(thumbnailsDir, "$photoId.jpg")
    }

    fun getAllPhotosForArchive(): List<File> {
        return originalsDir.listFiles()?.toList()
            ?: thumbnailsDir.listFiles()?.toList()
            ?: emptyList()
    }

    private fun generateSpectatorHtml(token: String = ""): String {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>FrameRoom — Live Spectator Wall</title>
                <style>
                    body {
                        margin: 0;
                        padding: 0;
                        background: #111318;
                        color: #E2E2E8;
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                    }
                    header {
                        display: flex;
                        align-items: center;
                        justify-content: space-between;
                        padding: 16px 24px;
                        background: rgba(17, 19, 24, 0.85);
                        backdrop-filter: blur(12px);
                        position: sticky;
                        top: 0;
                        z-index: 10;
                        border-bottom: 1px solid rgba(255, 255, 255, 0.08);
                    }
                    .brand {
                        display: flex;
                        align-items: center;
                        gap: 12px;
                    }
                    .logo-dot {
                        width: 12px;
                        height: 12px;
                        border-radius: 50%;
                        background: #F59E0B;
                        box-shadow: 0 0 12px #F59E0B;
                        animation: pulse 2s infinite;
                    }
                    @keyframes pulse {
                        0%, 100% { transform: scale(1); opacity: 1; }
                        50% { transform: scale(1.3); opacity: 0.7; }
                    }
                    .grid {
                        display: grid;
                        grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
                        gap: 16px;
                        padding: 24px;
                    }
                    .card {
                        background: #1A1C20;
                        border-radius: 16px;
                        overflow: hidden;
                        border: 1px solid rgba(255, 255, 255, 0.08);
                        position: relative;
                        transition: transform 0.2s;
                    }
                    .card:hover {
                        transform: translateY(-4px);
                    }
                    .card img {
                        width: 100%;
                        aspect-ratio: 1;
                        object-fit: cover;
                        display: block;
                    }
                    .overlay {
                        padding: 12px;
                        display: flex;
                        justify-content: space-between;
                        align-items: center;
                        font-size: 13px;
                    }
                    .uploader {
                        color: #FFC174;
                        font-weight: 600;
                    }
                    .reactions {
                        color: #F59E0B;
                        font-weight: bold;
                    }
                </style>
            </head>
            <body>
                <header>
                    <div class="brand">
                        <div class="logo-dot"></div>
                        <h2 style="margin: 0; font-size: 20px;">FrameRoom Live Wall</h2>
                    </div>
                    <div id="stats" style="color: #94A3B8; font-size: 14px;">Connecting to room...</div>
                </header>
                <div class="grid" id="photoGrid"></div>

                <script>
                    const sessionToken = '$token';
                    const wsProtocol = location.protocol === 'https:' ? 'wss:' : 'ws:';
                    const wsUrl = wsProtocol + '//' + location.host + '/ws?token=' + sessionToken;
                    let socket = new WebSocket(wsUrl);

                    function loadInitialPhotos() {
                        fetch('/api/join', {
                            method: 'POST',
                            headers: {
                                'Content-Type': 'application/json',
                                'X-Session-Token': sessionToken
                            },
                            body: JSON.stringify({
                                deviceId: 'spectator_' + Math.random().toString(36).substring(7),
                                displayName: 'Web Spectator',
                                clientPublicKey: ''
                            })
                        }).then(res => res.json()).then(data => {
                            document.getElementById('stats').innerText = (data.photos ? data.photos.length : 0) + ' Live Photos';
                            if (data.photos) {
                                data.photos.forEach(addPhoto);
                            }
                        });
                    }

                    function addPhoto(photo) {
                        const grid = document.getElementById('photoGrid');
                        if (document.getElementById('photo-' + photo.photoId)) return;

                        const card = document.createElement('div');
                        card.className = 'card';
                        card.id = 'photo-' + photo.photoId;
                        card.innerHTML = `
                            <img src="/api/photo/${'$'}{photo.photoId}/thumbnail?token=${'$'}{sessionToken}" alt="Event photo" />
                            <div class="overlay">
                                <span class="uploader">${'$'}{photo.uploaderName}</span>
                                <span class="reactions" id="react-${'$'}{photo.photoId}">★ ${'$'}{photo.reactionCount}</span>
                            </div>
                        `;
                        grid.prepend(card);
                    }

                    socket.onopen = () => {
                        loadInitialPhotos();
                    };

                    socket.onmessage = (event) => {
                        const msg = JSON.parse(event.data);
                        if (msg.type === 'NEW_PHOTO') {
                            const photo = JSON.parse(msg.payload);
                            addPhoto(photo);
                        } else if (msg.type === 'REACTION_UPDATED') {
                            const react = JSON.parse(msg.payload);
                            const el = document.getElementById('react-' + react.photoId);
                            if (el) el.innerText = '★ ' + react.newCount;
                        }
                    };

                    socket.onclose = () => {
                        setTimeout(() => location.reload(), 3000);
                    };
                </script>
            </body>
            </html>
        """.trimIndent()
    }
}
