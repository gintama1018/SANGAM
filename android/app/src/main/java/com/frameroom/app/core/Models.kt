package com.frameroom.app.core

import kotlinx.serialization.Serializable
import java.net.URI

@Serializable
enum class ParticipantRole {
    HOST,
    GUEST
}

@Serializable
data class Room(
    val roomId: String,
    val name: String,
    val activeWindowStart: Long,
    val closedAt: Long? = null,
    val hostDeviceId: String,
    val hostPublicKey: String,
    val sessionToken: String = "",
    @kotlinx.serialization.Transient
    val hostSecret: String? = null
) {
    fun toPublicRoom(): Room = copy(hostSecret = null)
}

@Serializable
data class Participant(
    val deviceId: String,
    val displayName: String,
    val joinedAt: Long,
    val role: ParticipantRole = ParticipantRole.GUEST,
    val photoCount: Int = 0
)

@Serializable
data class PhotoMeta(
    val photoId: String,
    val uploaderDeviceId: String,
    val uploaderName: String,
    val capturedAt: Long,
    val reactionCount: Int = 0,
    val isPrivate: Boolean = false,
    val width: Int = 0,
    val height: Int = 0,
    val cameraModel: String? = null,
    val exposureTime: String? = null,
    val iso: String? = null,
    val focalLength: String? = null
)

@Serializable
data class JoinRequest(
    val deviceId: String,
    val displayName: String,
    val clientPublicKey: String
)

@Serializable
data class JoinResponse(
    val room: Room,
    val participants: List<Participant>,
    val photos: List<PhotoMeta>
)

@Serializable
data class ReactionRequest(
    val deviceId: String
)

@Serializable
data class ReactionResponse(
    val photoId: String,
    val newCount: Int,
    val isReacted: Boolean
)

@Serializable
enum class SyncAckStatus {
    STORED,
    DUPLICATE_ACCEPTED,
    REJECTED_CLOSED_ROOM,
    REJECTED_INVALID_PAYLOAD
}

@Serializable
data class SyncAck(
    val photoId: String,
    val status: SyncAckStatus,
    val serverTimestamp: Long = System.currentTimeMillis()
)


@Serializable
data class WebSocketEvent(
    val type: String,
    val payload: String
) {
    companion object {
        const val TYPE_NEW_PHOTO = "NEW_PHOTO"
        const val TYPE_REACTION_UPDATED = "REACTION_UPDATED"
        const val TYPE_PARTICIPANT_JOINED = "PARTICIPANT_JOINED"
        const val TYPE_ROOM_CLOSED = "ROOM_CLOSED"
    }
}

data class QRPayload(
    val hostIp: String,
    val port: Int,
    val roomId: String,
    val hostPublicKey: String,
    val sessionToken: String
) {
    fun toUrl(): String {
        return "fr://$hostIp:$port/$roomId/$hostPublicKey/$sessionToken"
    }

    companion object {
        fun parse(raw: String): QRPayload? {
            return try {
                if (!raw.startsWith("fr://")) return null
                val uri = URI(raw)
                val host = uri.host ?: return null
                val port = if (uri.port > 0) uri.port else 8080
                val pathSegments = uri.path.trim('/').split('/')
                if (pathSegments.size != 3) return null
                val roomId = pathSegments[0]
                val publicKey = pathSegments[1]
                val sessionToken = pathSegments[2]
                if (roomId.isBlank() || publicKey.isBlank() || sessionToken.isBlank()) {
                    return null
                }
                QRPayload(host, port, roomId, publicKey, sessionToken)
            } catch (e: Exception) {
                null
            }
        }
    }
}
