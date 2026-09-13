package com.frameroom.app

import com.frameroom.app.client.FrameRoomClient
import com.frameroom.app.core.PhotoIdentity
import com.frameroom.app.core.SyncAckStatus
import com.frameroom.app.core.WebSocketEvent
import com.frameroom.app.server.FrameRoomServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.net.ServerSocket
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class PhotoIdentityAndDeduplicationTest {

    private var tempDir: File? = null
    private var server: FrameRoomServer? = null
    private var client: FrameRoomClient? = null
    private var testPort: Int = 0
    private val testHostDeviceId = "host_dev_1"

    private fun ensureServerStarted(): Pair<FrameRoomServer, FrameRoomClient> {
        if (server == null) {
            val dir = File(System.getProperty("java.io.tmpdir"), "frameroom_test_" + System.currentTimeMillis()).apply {
                mkdirs()
            }
            tempDir = dir
            testPort = ServerSocket(0).use { it.localPort }
            val s = FrameRoomServer(baseDir = dir)
            val createdRoom = s.start(
                roomName = "Test Event Room",
                hostDeviceId = testHostDeviceId,
                hostDisplayName = "Host User",
                hostPublicKey = "test_key",
                port = testPort
            )
            server = s
            client = FrameRoomClient().apply {
                sessionToken = createdRoom.sessionToken
            }
        }
        return Pair(server!!, client!!)
    }

    @After
    fun tearDown() {
        try { client?.close() } catch (e: Exception) {}
        try { server?.stop() } catch (e: Exception) {}
        tempDir?.deleteRecursively()
        server = null
        client = null
        tempDir = null
    }

    private fun createValidSampleJpeg(): ByteArray {
        return byteArrayOf(
            0xFF.toByte(), 0xD8.toByte(), // SOI
            0xFF.toByte(), 0xE0.toByte(), // APP0
            0x00.toByte(), 0x10.toByte(),
            'J'.code.toByte(), 'F'.code.toByte(), 'I'.code.toByte(), 'F'.code.toByte(), 0x00.toByte(),
            0x01.toByte(), 0x01.toByte(), 0x00.toByte(), 0x00.toByte(), 0x01.toByte(),
            0x00.toByte(), 0x01.toByte(), 0x00.toByte(), 0x00.toByte(),
            0xFF.toByte(), 0xD9.toByte()  // EOI
        )
    }

    // 1. Same identity inputs produce same photoId.
    @Test
    fun testSameIdentityInputsProduceSamePhotoId() {
        val id1 = PhotoIdentity.generatePhotoId("FR-100", "dev-A", 5001L, 1710000000000L)
        val id2 = PhotoIdentity.generatePhotoId("FR-100", "dev-A", 5001L, 1710000000000L)
        assertEquals(id1, id2)
        assertTrue(id1.startsWith("ph_"))
        assertEquals(35, id1.length) // "ph_" (3 chars) + 32 hex chars
    }

    // 2. Different MediaStore mediaId produces different photoId.
    @Test
    fun testDifferentMediaStoreMediaIdProducesDifferentPhotoId() {
        val id1 = PhotoIdentity.generatePhotoId("FR-100", "dev-A", 5001L, 1710000000000L)
        val id2 = PhotoIdentity.generatePhotoId("FR-100", "dev-A", 5002L, 1710000000000L)
        assertNotEquals(id1, id2)
    }

    // 3. Different capturedAt produces different photoId.
    @Test
    fun testDifferentCapturedAtProducesDifferentPhotoId() {
        val id1 = PhotoIdentity.generatePhotoId("FR-100", "dev-A", 5001L, 1710000000000L)
        val id2 = PhotoIdentity.generatePhotoId("FR-100", "dev-A", 5001L, 1710000000001L)
        assertNotEquals(id1, id2)
    }

    // 4. Different roomId produces different photoId.
    @Test
    fun testDifferentRoomIdProducesDifferentPhotoId() {
        val id1 = PhotoIdentity.generatePhotoId("FR-100", "dev-A", 5001L, 1710000000000L)
        val id2 = PhotoIdentity.generatePhotoId("FR-101", "dev-A", 5001L, 1710000000000L)
        assertNotEquals(id1, id2)
    }

    // 5. Different deviceId produces different photoId.
    @Test
    fun testDifferentDeviceIdProducesDifferentPhotoId() {
        val id1 = PhotoIdentity.generatePhotoId("FR-100", "dev-A", 5001L, 1710000000000L)
        val id2 = PhotoIdentity.generatePhotoId("FR-100", "dev-B", 5001L, 1710000000000L)
        assertNotEquals(id1, id2)
    }

    // 9. First valid upload returns STORED.
    @Test
    fun testFirstValidUploadReturnsStored() = runBlocking {
        val (srv, cl) = ensureServerStarted()
        val photoId = PhotoIdentity.generatePhotoId("FR-TEST", "guest-1", 101L, 1710000000000L)
        val jpeg = createValidSampleJpeg()

        val result = cl.uploadThumbnail(
            hostIp = "127.0.0.1",
            port = testPort,
            photoId = photoId,
            roomId = "FR-TEST",
            thumbnailBytes = jpeg,
            uploaderDeviceId = "guest-1",
            uploaderName = "Guest Alice",
            capturedAt = 1710000000000L
        )

        assertTrue(result.isSuccess)
        val ack = result.getOrThrow()
        assertEquals(photoId, ack.photoId)
        assertEquals(SyncAckStatus.STORED, ack.status)
        assertEquals(1, srv.photos.value.size)
        assertEquals(photoId, srv.photos.value.first().photoId)
    }

    // 6. Duplicate upload returns DUPLICATE_ACCEPTED.
    // 7. Duplicate upload does not increase gallery count.
    @Test
    fun testDuplicateUploadReturnsDuplicateAcceptedAndDoesNotIncreaseGalleryCount() = runBlocking {
        val (srv, cl) = ensureServerStarted()
        val photoId = PhotoIdentity.generatePhotoId("FR-TEST", "guest-1", 202L, 1710000000000L)
        val jpeg = createValidSampleJpeg()

        val firstResult = cl.uploadThumbnail(
            hostIp = "127.0.0.1",
            port = testPort,
            photoId = photoId,
            roomId = "FR-TEST",
            thumbnailBytes = jpeg,
            uploaderDeviceId = "guest-1",
            uploaderName = "Guest Alice",
            capturedAt = 1710000000000L
        )
        assertTrue(firstResult.isSuccess)
        assertEquals(SyncAckStatus.STORED, firstResult.getOrThrow().status)
        val countAfterFirst = srv.photos.value.size

        // Retry same photo (simulating network reconnect retry)
        val secondResult = cl.uploadThumbnail(
            hostIp = "127.0.0.1",
            port = testPort,
            photoId = photoId,
            roomId = "FR-TEST",
            thumbnailBytes = jpeg,
            uploaderDeviceId = "guest-1",
            uploaderName = "Guest Alice",
            capturedAt = 1710000000000L
        )
        assertTrue(secondResult.isSuccess)
        val ack2 = secondResult.getOrThrow()
        assertEquals(photoId, ack2.photoId)
        assertEquals(SyncAckStatus.DUPLICATE_ACCEPTED, ack2.status)

        // Verify gallery count did NOT increase
        assertEquals(countAfterFirst, srv.photos.value.size)
    }

    // 8. Duplicate upload does not broadcast a second new-photo event.
    @Test
    fun testDuplicateUploadDoesNotBroadcastSecondNewPhotoEvent() = runBlocking {
        val (_, cl) = ensureServerStarted()
        val photoId = PhotoIdentity.generatePhotoId("FR-TEST", "guest-1", 303L, 1710000000000L)
        val jpeg = createValidSampleJpeg()

        val receivedEvents = Collections.synchronizedList(mutableListOf<WebSocketEvent>())
        val firstEventLatch = CountDownLatch(1)
        val wsScope = CoroutineScope(Dispatchers.IO + Job())

        try {
            cl.startWebSocket(
                hostIp = "127.0.0.1",
                port = testPort,
                scope = wsScope,
                onEvent = { event ->
                    if (event.type == WebSocketEvent.TYPE_NEW_PHOTO) {
                        receivedEvents.add(event)
                        firstEventLatch.countDown()
                    }
                }
            )

            // Wait brief moment for WS handshake
            Thread.sleep(300)

            // First upload
            val firstResult = cl.uploadThumbnail(
                hostIp = "127.0.0.1",
                port = testPort,
                photoId = photoId,
                thumbnailBytes = jpeg,
                uploaderDeviceId = "guest-1",
                uploaderName = "Guest Alice",
                capturedAt = 1710000000000L
            )
            assertTrue(firstResult.isSuccess)
            assertTrue(firstEventLatch.await(3, TimeUnit.SECONDS))
            val eventCountAfterFirst = receivedEvents.size
            assertEquals(1, eventCountAfterFirst)

            // Second upload with same photoId
            val secondResult = cl.uploadThumbnail(
                hostIp = "127.0.0.1",
                port = testPort,
                photoId = photoId,
                thumbnailBytes = jpeg,
                uploaderDeviceId = "guest-1",
                uploaderName = "Guest Alice",
                capturedAt = 1710000000000L
            )
            assertTrue(secondResult.isSuccess)
            assertEquals(SyncAckStatus.DUPLICATE_ACCEPTED, secondResult.getOrThrow().status)

            // Wait to verify no additional event is sent
            Thread.sleep(300)
            assertEquals(eventCountAfterFirst, receivedEvents.size)
        } finally {
            cl.stopWebSocket()
            wsScope.cancel()
        }
    }

    // 10. Closed room returns REJECTED_CLOSED_ROOM.
    @Test
    fun testClosedRoomReturnsRejectedClosedRoom() = runBlocking {
        val (_, cl) = ensureServerStarted()
        val photoId = PhotoIdentity.generatePhotoId("FR-TEST", "guest-1", 404L, 1710000000000L)
        val jpeg = createValidSampleJpeg()

        // Close the room as host
        val closeResult = cl.closeRoom("127.0.0.1", testPort, testHostDeviceId)
        assertTrue(closeResult.isSuccess)

        val uploadResult = cl.uploadThumbnail(
            hostIp = "127.0.0.1",
            port = testPort,
            photoId = photoId,
            thumbnailBytes = jpeg,
            uploaderDeviceId = "guest-1",
            uploaderName = "Guest Alice",
            capturedAt = 1710000000000L
        )

        assertTrue(uploadResult.isSuccess)
        val ack = uploadResult.getOrThrow()
        assertEquals(SyncAckStatus.REJECTED_CLOSED_ROOM, ack.status)
    }

    // 11. Invalid payload returns REJECTED_INVALID_PAYLOAD.
    @Test
    fun testInvalidPayloadReturnsRejectedInvalidPayload() = runBlocking {
        // Create fresh server for this test since test 10 closed the previous one
        tearDown()
        val (_, cl) = ensureServerStarted()
        val photoId = PhotoIdentity.generatePhotoId("FR-TEST", "guest-1", 505L, 1710000000000L)
        val invalidBytes = byteArrayOf(0x00, 0x01, 0x02, 0x03) // not JPEG

        val uploadResult = cl.uploadThumbnail(
            hostIp = "127.0.0.1",
            port = testPort,
            photoId = photoId,
            thumbnailBytes = invalidBytes,
            uploaderDeviceId = "guest-1",
            uploaderName = "Guest Alice",
            capturedAt = 1710000000000L
        )

        assertTrue(uploadResult.isSuccess)
        val ack = uploadResult.getOrThrow()
        assertEquals(SyncAckStatus.REJECTED_INVALID_PAYLOAD, ack.status)
    }

    // 12. Request without or with invalid session token is rejected with Unauthorized.
    @Test
    fun testUnauthorizedRequestRejectedWithoutSessionToken() = runBlocking {
        val (srv, _) = ensureServerStarted()
        val rogueClient = FrameRoomClient() // no session token

        val joinResult = rogueClient.joinRoom(
            hostIp = "127.0.0.1",
            port = testPort,
            deviceId = "rogue-dev",
            displayName = "Rogue User",
            clientPublicKey = "rogue_key"
        )
        assertTrue("Join without session token must fail", joinResult.isFailure)

        val invalidTokenClient = FrameRoomClient().apply {
            sessionToken = "invalid_token_9999"
        }
        val invalidJoinResult = invalidTokenClient.joinRoom(
            hostIp = "127.0.0.1",
            port = testPort,
            deviceId = "rogue-dev-2",
            displayName = "Rogue User 2",
            clientPublicKey = "rogue_key_2"
        )
        assertTrue("Join with invalid session token must fail", invalidJoinResult.isFailure)

        rogueClient.close()
        invalidTokenClient.close()
    }

    // 13. Request with valid session token succeeds.
    @Test
    fun testAuthorizedRequestAcceptedWithSessionToken() = runBlocking {
        val (srv, _) = ensureServerStarted()
        val validClient = FrameRoomClient().apply {
            sessionToken = srv.room.value?.sessionToken
        }

        val joinResult = validClient.joinRoom(
            hostIp = "127.0.0.1",
            port = testPort,
            deviceId = "guest-authorized",
            displayName = "Auth Guest",
            clientPublicKey = "auth_key"
        )
        assertTrue("Join with valid session token must succeed", joinResult.isSuccess)
        assertEquals(srv.room.value?.roomId, joinResult.getOrThrow().room.roomId)

        validClient.close()
    }

    // 14. GET /api/photo/{id}/thumbnail is rejected without token and succeeds with token.
    @Test
    fun testThumbnailGetRejectedWithoutSessionTokenAndAcceptedWithToken() = runBlocking {
        val (srv, cl) = ensureServerStarted()
        val photoId = PhotoIdentity.generatePhotoId("FR-TEST", "guest-thumb-auth", 901L, 1710000000000L)
        val uploadResult = cl.uploadThumbnail(
            hostIp = "127.0.0.1",
            port = testPort,
            photoId = photoId,
            thumbnailBytes = createValidSampleJpeg(),
            uploaderDeviceId = "guest-thumb-auth",
            uploaderName = "Guest Thumb",
            capturedAt = 1710000000000L
        )
        assertTrue(uploadResult.isSuccess)

        // 1. Request without token must return 401 Unauthorized
        val connNoToken = (java.net.URL("http://127.0.0.1:$testPort/api/photo/$photoId/thumbnail").openConnection() as java.net.HttpURLConnection).apply {
            requestMethod = "GET"
        }
        assertEquals(401, connNoToken.responseCode)

        // 2. Request with query param ?token=... must return 200 OK
        val connWithQuery = (java.net.URL("http://127.0.0.1:$testPort/api/photo/$photoId/thumbnail?token=${srv.room.value?.sessionToken}").openConnection() as java.net.HttpURLConnection).apply {
            requestMethod = "GET"
        }
        assertEquals(200, connWithQuery.responseCode)

        // 3. Request with header X-Session-Token must return 200 OK
        val connWithHeader = (java.net.URL("http://127.0.0.1:$testPort/api/photo/$photoId/thumbnail").openConnection() as java.net.HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("X-Session-Token", srv.room.value?.sessionToken)
        }
        assertEquals(200, connWithHeader.responseCode)
    }

    // 15. WebSocket handshake is rejected without valid session token and accepted with valid token.
    @Test
    fun testWebSocketRejectedWithoutSessionTokenAndAcceptedWithToken() = runBlocking {
        val (srv, cl) = ensureServerStarted()
        val photoId = PhotoIdentity.generatePhotoId("FR-TEST", "guest-ws-auth", 902L, 1710000000000L)
        cl.uploadThumbnail(
            hostIp = "127.0.0.1",
            port = testPort,
            photoId = photoId,
            thumbnailBytes = createValidSampleJpeg(),
            uploaderDeviceId = "guest-ws-auth",
            uploaderName = "Guest WS",
            capturedAt = 1710000000000L
        )

        // 1. Rogue WebSocket with wrong token must be closed by server
        val rogueClient = FrameRoomClient()
        val disconnectedLatch = CountDownLatch(1)
        rogueClient.startWebSocket(
            hostIp = "127.0.0.1",
            port = testPort,
            scope = CoroutineScope(Dispatchers.IO),
            sessionToken = "invalid_ws_token",
            onEvent = {},
            onDisconnected = { disconnectedLatch.countDown() }
        )
        assertTrue("Rogue WebSocket must be disconnected by server", disconnectedLatch.await(3, TimeUnit.SECONDS))
        rogueClient.close()

        // 2. Authenticated WebSocket with valid token receives broadcasts
        val validClient = FrameRoomClient().apply {
            sessionToken = srv.room.value?.sessionToken
        }
        val eventReceivedLatch = CountDownLatch(1)
        validClient.startWebSocket(
            hostIp = "127.0.0.1",
            port = testPort,
            scope = CoroutineScope(Dispatchers.IO),
            sessionToken = srv.room.value?.sessionToken,
            onEvent = { eventReceivedLatch.countDown() },
            onDisconnected = {}
        )

        // Trigger reaction event to verify broadcast receipt
        cl.toggleReaction("127.0.0.1", testPort, photoId, "reactor-1")
        assertTrue("Authenticated WebSocket must receive live broadcast event", eventReceivedLatch.await(3, TimeUnit.SECONDS))
        validClient.close()
    }
}
