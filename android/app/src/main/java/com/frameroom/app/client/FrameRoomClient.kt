package com.frameroom.app.client

import com.frameroom.app.core.JoinRequest
import com.frameroom.app.core.JoinResponse
import com.frameroom.app.core.PhotoMeta
import com.frameroom.app.core.ReactionRequest
import com.frameroom.app.core.ReactionResponse
import com.frameroom.app.core.Room
import com.frameroom.app.core.WebSocketEvent
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.Closeable

class FrameRoomClient : Closeable {

    private val json = Json { ignoreUnknownKeys = true }

    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(json)
        }
        install(WebSockets)
    }

    private var wsJob: Job? = null
    private var wsSession: DefaultClientWebSocketSession? = null

    suspend fun joinRoom(
        hostIp: String,
        port: Int,
        deviceId: String,
        displayName: String,
        clientPublicKey: String
    ): Result<JoinResponse> {
        return try {
            val response = httpClient.post("http://$hostIp:$port/api/join") {
                contentType(ContentType.Application.Json)
                setBody(JoinRequest(deviceId, displayName, clientPublicKey))
            }.body<JoinResponse>()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun startWebSocket(
        hostIp: String,
        port: Int,
        scope: CoroutineScope,
        onEvent: (WebSocketEvent) -> Unit,
        onDisconnected: () -> Unit = {}
    ) {
        stopWebSocket()
        wsJob = scope.launch(Dispatchers.IO) {
            try {
                httpClient.webSocket(host = hostIp, port = port, path = "/ws") {
                    wsSession = this
                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            val text = frame.readText()
                            try {
                                val event = json.decodeFromString<WebSocketEvent>(text)
                                onEvent(event)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    e.printStackTrace()
                }
            } finally {
                wsSession = null
                onDisconnected()
            }
        }
    }

    fun stopWebSocket() {
        wsJob?.cancel()
        wsJob = null
        wsSession = null
    }

    suspend fun uploadThumbnail(
        hostIp: String,
        port: Int,
        thumbnailBytes: ByteArray,
        uploaderDeviceId: String,
        uploaderName: String,
        capturedAt: Long,
        cameraModel: String? = null,
        exposure: String? = null,
        iso: String? = null,
        focalLength: String? = null
    ): Result<PhotoMeta> {
        return try {
            val response = httpClient.post("http://$hostIp:$port/api/photo/thumbnail") {
                contentType(ContentType.Image.JPEG)
                header("X-Device-Id", uploaderDeviceId)
                header("X-Uploader-Name", uploaderName)
                header("X-Captured-At", capturedAt.toString())
                cameraModel?.let { header("X-Camera-Model", it) }
                exposure?.let { header("X-Exposure", it) }
                iso?.let { header("X-ISO", it) }
                focalLength?.let { header("X-Focal-Length", it) }
                setBody(thumbnailBytes)
            }.body<PhotoMeta>()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleReaction(
        hostIp: String,
        port: Int,
        photoId: String,
        deviceId: String
    ): Result<ReactionResponse> {
        return try {
            val response = httpClient.post("http://$hostIp:$port/api/photo/$photoId/react") {
                contentType(ContentType.Application.Json)
                setBody(ReactionRequest(deviceId))
            }.body<ReactionResponse>()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun closeRoom(
        hostIp: String,
        port: Int,
        hostDeviceId: String
    ): Result<Room> {
        return try {
            val response = httpClient.post("http://$hostIp:$port/api/room/close") {
                header("X-Host-Device-Id", hostDeviceId)
            }.body<Room>()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun close() {
        stopWebSocket()
        httpClient.close()
    }
}
