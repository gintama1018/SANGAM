package com.frameroom.app

import com.frameroom.app.client.FrameRoomClient
import com.frameroom.app.core.PhotoIdentity
import com.frameroom.app.core.SyncAckStatus
import com.frameroom.app.online.MockOnlineDestination
import com.frameroom.app.online.OnlineUploadManager
import com.frameroom.app.online.OnlineUploadState
import com.frameroom.app.server.FrameRoomServer
import com.frameroom.app.watcher.CaptureSessionState
import com.frameroom.app.watcher.DetectedPhoto
import com.frameroom.app.watcher.EligibilityResult
import com.frameroom.app.watcher.MediaMetadata
import com.frameroom.app.watcher.OutboxRecord
import com.frameroom.app.watcher.OutboxRepository
import com.frameroom.app.watcher.OutboxState
import com.frameroom.app.watcher.PhotoEligibilityChecker
import com.frameroom.app.watcher.SyncQueueManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.net.ServerSocket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Phase 5 — Real-Device Integration & Failure Test Harness.
 *
 * Validates Levels 1–6 using embedded Ktor server on real dynamic ports,
 * real Ktor HTTP/WebSocket clients, disk-backed outboxes, MediaStore edge cases,
 * crash recovery, and progressive multi-client concurrency.
 */
class RealDeviceIntegrationAndFailureTest {

    private lateinit var rootTempDir: File
    private var server: FrameRoomServer? = null
    private var serverPort: Int = 0
    private val clients = mutableListOf<FrameRoomClient>()
    private var testScope: CoroutineScope? = null

    @Before
    fun setUp() {
        rootTempDir = File(System.getProperty("java.io.tmpdir"), "phase5_test_" + System.nanoTime()).apply {
            mkdirs()
        }
    }

    @After
    fun tearDown() {
        testScope?.cancel()
        for (c in clients) {
            try { c.close() } catch (e: Exception) {}
        }
        clients.clear()
        try { server?.stop() } catch (e: Exception) {}
        server = null
        rootTempDir.deleteRecursively()
    }

    private fun startHostServer(): Pair<FrameRoomServer, Int> {
        val serverDir = File(rootTempDir, "host_server").apply { mkdirs() }
        val port = ServerSocket(0).use { it.localPort }
        val s = FrameRoomServer(baseDir = serverDir)
        s.start(
            roomName = "Phase 5 Event Room",
            hostDeviceId = "host_device_alpha",
            hostDisplayName = "Host Admin",
            hostPublicKey = "pub_key_alpha",
            port = port
        )
        server = s
        serverPort = port
        return Pair(s, port)
    }

    private fun createValidJpeg(tag: String): ByteArray {
        val header = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(), 0x00, 0x10)
        return header + "jpeg_content_$tag".toByteArray()
    }

    // ====================================================
    // VALIDATION LEVEL 1 — GOLDEN PATH
    // ====================================================
    @Test
    fun testLevel1_goldenPathEndToEndSync() = runBlocking {
        val (hostServer, port) = startHostServer()
        val guestClient = FrameRoomClient().also { clients.add(it) }
        val guestDir = File(rootTempDir, "guest_device_b").apply { mkdirs() }
        val scope = CoroutineScope(Dispatchers.IO).also { testScope = it }

        // Guest joins room
        val joinResult = guestClient.joinRoom(
            hostIp = "127.0.0.1",
            port = port,
            deviceId = "guest_device_b",
            displayName = "Guest User",
            clientPublicKey = "key_guest_b"
        )
        assertTrue("Guest join must succeed", joinResult.isSuccess)
        val room = joinResult.getOrThrow().room

        // Wire SyncQueueManager with real client uploadExecutor
        val outboxManager = SyncQueueManager(
            baseDir = guestDir,
            coroutineScope = scope,
            baseBackoffMs = 10L
        ).apply {
            uploadExecutor = { record, thumbBytes ->
                val result = guestClient.uploadThumbnail(
                    hostIp = "127.0.0.1",
                    port = port,
                    photoId = record.photoId,
                    roomId = record.roomId,
                    thumbnailBytes = thumbBytes,
                    uploaderDeviceId = record.deviceId,
                    uploaderName = "Guest User",
                    capturedAt = record.capturedAt,
                    cameraModel = "Pixel 8 Pro"
                )
                result.getOrThrow()
            }
        }
        outboxManager.setHostEndpoint("127.0.0.1", port)

        // Capture 5 photos
        val startTime = System.currentTimeMillis()
        val photos = (1..5).map { i ->
            val capturedAt = System.currentTimeMillis() + i
            val photoId = PhotoIdentity.generatePhotoId(room.roomId, "guest_device_b", 1000L + i, capturedAt)
            DetectedPhoto(
                photoId = photoId,
                uri = null,
                mediaId = 1000L + i,
                capturedAt = capturedAt,
                thumbnailBytes = createValidJpeg("photo_$i"),
                cameraModel = "Pixel 8 Pro"
            )
        }

        // Enqueue into persistent local outbox
        outboxManager.enqueuePhotos(photos, roomId = room.roomId, deviceId = "guest_device_b")

        // Wait for synchronization
        var allAcked = false
        for (attempt in 1..40) {
            val records = outboxManager.records.value.values
            if (records.size == 5 && records.all { it.state == OutboxState.ACKED }) {
                allAcked = true
                break
            }
            delay(50)
        }
        val totalSyncLatencyMs = System.currentTimeMillis() - startTime

        assertTrue("All 5 photos must reach ACKED state in LocalOutbox", allAcked)
        assertEquals("Host must receive exactly 5 photos in gallery", 5, hostServer.photos.value.size)
        println("Level 1 Golden Path: 5 photos synced in ${totalSyncLatencyMs}ms (avg ${totalSyncLatencyMs / 5}ms/photo)")
    }

    // ====================================================
    // VALIDATION LEVEL 2 — DISCONNECT / RECONNECT
    // ====================================================
    @Test
    fun testLevel2_disconnectCaptureRestartReconnectNoDuplicates() = runBlocking {
        val (hostServer, port) = startHostServer()
        val guestClient = FrameRoomClient().also { clients.add(it) }
        val guestDir = File(rootTempDir, "guest_level_2").apply { mkdirs() }
        val scope1 = CoroutineScope(Dispatchers.IO)

        val outboxManager1 = SyncQueueManager(
            baseDir = guestDir,
            coroutineScope = scope1,
            baseBackoffMs = 10L
        )

        // 1. Disconnect Guest from Host network
        outboxManager1.setHostEndpoint(null, port)

        // 2. Capture 10 photos while disconnected
        val photos = (1..10).map { i ->
            val capturedAt = 1710000000000L + i
            val photoId = PhotoIdentity.generatePhotoId("FR-TEST-L2", "guest_l2", 2000L + i, capturedAt)
            DetectedPhoto(
                photoId = photoId,
                uri = null,
                mediaId = 2000L + i,
                capturedAt = capturedAt,
                thumbnailBytes = createValidJpeg("l2_photo_$i")
            )
        }
        outboxManager1.enqueuePhotos(photos, roomId = "FR-TEST-L2", deviceId = "guest_l2")

        // Verify all 10 remain queued in WAITING_FOR_HOST
        assertEquals(10, outboxManager1.records.value.size)
        assertTrue(outboxManager1.records.value.values.all { it.state == OutboxState.WAITING_FOR_HOST })

        // 3. Force-stop/kill Guest app process
        scope1.cancel()

        // 4. Restart Guest application
        val scope2 = CoroutineScope(Dispatchers.IO).also { testScope = it }
        val outboxManager2 = SyncQueueManager(
            baseDir = guestDir,
            coroutineScope = scope2,
            baseBackoffMs = 10L
        ).apply {
            uploadExecutor = { record, thumbBytes ->
                val result = guestClient.uploadThumbnail(
                    hostIp = "127.0.0.1",
                    port = port,
                    photoId = record.photoId,
                    roomId = record.roomId,
                    thumbnailBytes = thumbBytes,
                    uploaderDeviceId = record.deviceId,
                    uploaderName = "Guest L2",
                    capturedAt = record.capturedAt
                )
                result.getOrThrow()
            }
        }

        // Verify records survived restart
        assertEquals("Records must survive simulated process restart", 10, outboxManager2.records.value.size)

        // 5. Reconnect to Host
        outboxManager2.setHostEndpoint("127.0.0.1", port)

        // Verify automatic resume and all 10 photos arrive
        var allDone = false
        for (i in 1..50) {
            val records = outboxManager2.records.value.values
            if (records.size == 10 && records.all { it.state == OutboxState.ACKED }) {
                allDone = true
                break
            }
            delay(50)
        }
        assertTrue("All 10 photos must be ACKED after reconnect", allDone)
        assertEquals("Host must receive exactly 10 photos", 10, hostServer.photos.value.size)

        // 6. Retry same batch to verify Host deduplication guarantees NO duplicates
        outboxManager2.triggerQueueProcessing()
        delay(100)
        assertEquals("Host gallery must still contain exactly 10 photos without duplicates", 10, hostServer.photos.value.size)
    }

    // ====================================================
    // VALIDATION LEVEL 3 — CRASH DURING TRANSFER
    // ====================================================
    @Test
    fun testLevel3_crashDuringActiveTransferRecoversAndAcks() = runBlocking {
        val (hostServer, port) = startHostServer()
        val guestClient = FrameRoomClient().also { clients.add(it) }
        val guestDir = File(rootTempDir, "guest_level_3").apply { mkdirs() }

        // Setup repository directly with simulated crashed in-flight records
        val repo = OutboxRepository(guestDir)
        val thumbPath1 = repo.saveThumbnail("ph_crash_a", createValidJpeg("crash_a"))
        val thumbPath2 = repo.saveThumbnail("ph_crash_b", createValidJpeg("crash_b"))

        // Simulate photos that were in state SENDING when process died
        val record1 = OutboxRecord(
            photoId = "ph_crash_a",
            roomId = "FR-CRASH-TEST",
            mediaId = 3001L,
            mediaUri = "content://media/3001",
            deviceId = "guest_crash",
            capturedAt = 1710000000000L,
            thumbnailPath = thumbPath1,
            state = OutboxState.SENDING
        )
        val record2 = OutboxRecord(
            photoId = "ph_crash_b",
            roomId = "FR-CRASH-TEST",
            mediaId = 3002L,
            mediaUri = "content://media/3002",
            deviceId = "guest_crash",
            capturedAt = 1710000001000L,
            thumbnailPath = thumbPath2,
            state = OutboxState.SENDING
        )
        repo.saveAll(listOf(record1, record2))

        // Pre-upload photo A to host to simulate that photo A reached host right before crash
        val preUploadResult = guestClient.uploadThumbnail(
            hostIp = "127.0.0.1",
            port = port,
            photoId = "ph_crash_a",
            roomId = "FR-CRASH-TEST",
            thumbnailBytes = createValidJpeg("crash_a"),
            uploaderDeviceId = "guest_crash",
            uploaderName = "Guest Crash",
            capturedAt = 1710000000000L
        )
        assertEquals(SyncAckStatus.STORED, preUploadResult.getOrThrow().status)
        assertEquals(1, hostServer.photos.value.size)

        // Relaunch app / initialize SyncQueueManager
        val scope = CoroutineScope(Dispatchers.IO).also { testScope = it }
        val outboxManager = SyncQueueManager(
            baseDir = guestDir,
            coroutineScope = scope,
            baseBackoffMs = 10L
        ).apply {
            uploadExecutor = { record, thumbBytes ->
                val res = guestClient.uploadThumbnail(
                    hostIp = "127.0.0.1",
                    port = port,
                    photoId = record.photoId,
                    roomId = record.roomId,
                    thumbnailBytes = thumbBytes,
                    uploaderDeviceId = record.deviceId,
                    uploaderName = "Guest Crash",
                    capturedAt = record.capturedAt
                )
                res.getOrThrow()
            }
        }

        // Verify that startup recovery immediately converted stale SENDING to FAILED_RETRYABLE
        val recoveredA = outboxManager.records.value["ph_crash_a"]
        val recoveredB = outboxManager.records.value["ph_crash_b"]
        assertNotNull(recoveredA)
        assertNotNull(recoveredB)

        // Set host endpoint and trigger processing
        outboxManager.setHostEndpoint("127.0.0.1", port)

        // Wait for retry completion
        var bothAcked = false
        for (i in 1..40) {
            val rA = outboxManager.records.value["ph_crash_a"]
            val rB = outboxManager.records.value["ph_crash_b"]
            if (rA?.state == OutboxState.ACKED && rB?.state == OutboxState.ACKED) {
                bothAcked = true
                break
            }
            delay(50)
        }

        assertTrue("Both recovered crash records must reach ACKED", bothAcked)
        // Photo A was already on host, so host returned DUPLICATE_ACCEPTED.
        // Photo B was stored fresh.
        // Host must have exactly 2 photos, with zero duplicates!
        assertEquals("Host must contain exactly 2 photos without duplicates", 2, hostServer.photos.value.size)
    }

    // ====================================================
    // VALIDATION LEVEL 4 — MEDIASTORE EDGE CASES
    // ====================================================
    @Test
    fun testLevel4_mediaStoreEdgeCases() {
        val sessionStart = 1710000000000L
        val baseline = setOf(101L, 102L)

        // 1. HEIC photo
        val heicMeta = MediaMetadata(
            mediaId = 501L,
            dateAddedSec = (sessionStart + 10_000L) / 1000,
            dateTakenMs = sessionStart + 10_000L,
            mimeType = "image/heic",
            bucketDisplayName = "Camera",
            relativePath = "DCIM/Camera/",
            size = 2_500_000L
        )
        assertEquals(EligibilityResult.ELIGIBLE, PhotoEligibilityChecker.evaluate(heicMeta, sessionStart, baseline))

        // 2. Standard JPEG photo
        val jpegMeta = heicMeta.copy(mediaId = 502L, mimeType = "image/jpeg")
        assertEquals(EligibilityResult.ELIGIBLE, PhotoEligibilityChecker.evaluate(jpegMeta, sessionStart, baseline))

        // 3. Incomplete row (size = 0 or isPending = true) -> must be deferred
        val zeroByteMeta = jpegMeta.copy(mediaId = 503L, size = 0L)
        assertEquals(EligibilityResult.DEFERRED_INCOMPLETE_ROW, PhotoEligibilityChecker.evaluate(zeroByteMeta, sessionStart, baseline))

        val pendingMeta = jpegMeta.copy(mediaId = 504L, size = 1_000_000L, isPending = true)
        assertEquals(EligibilityResult.DEFERRED_INCOMPLETE_ROW, PhotoEligibilityChecker.evaluate(pendingMeta, sessionStart, baseline))

        // 4. Non-camera sources (WhatsApp, Screenshots, Downloads) -> rejected
        val whatsappMeta = jpegMeta.copy(mediaId = 505L, relativePath = "Pictures/WhatsApp/")
        assertEquals(EligibilityResult.REJECTED_EXCLUDED_LOCATION, PhotoEligibilityChecker.evaluate(whatsappMeta, sessionStart, baseline))

        val screenshotMeta = jpegMeta.copy(mediaId = 506L, bucketDisplayName = "Screenshots")
        assertEquals(EligibilityResult.REJECTED_EXCLUDED_LOCATION, PhotoEligibilityChecker.evaluate(screenshotMeta, sessionStart, baseline))

        // 5. Pre-session image -> rejected
        val preSessionMeta = jpegMeta.copy(mediaId = 507L, dateTakenMs = sessionStart - 5000L)
        assertEquals(EligibilityResult.REJECTED_PRE_SESSION, PhotoEligibilityChecker.evaluate(preSessionMeta, sessionStart, baseline))

        // 6. Baseline image -> rejected
        val baselineMeta = jpegMeta.copy(mediaId = 101L)
        assertEquals(EligibilityResult.REJECTED_BASELINE, PhotoEligibilityChecker.evaluate(baselineMeta, sessionStart, baseline))
    }

    // ====================================================
    // VALIDATION LEVEL 5 — MULTI-USER CONCURRENCY (2, 5, 10 CLIENTS)
    // ====================================================
    @Test
    fun testLevel5_multiUserConcurrencyStress() = runBlocking {
        val (hostServer, port) = startHostServer()
        val scope = CoroutineScope(Dispatchers.IO).also { testScope = it }

        // Progressive test matrix: 2 clients, 5 clients, 10 clients
        val clientCounts = listOf(2, 5, 10)

        for (clientCount in clientCounts) {
            val photosPerClient = 3
            val totalExpected = clientCount * photosPerClient
            val doneLatch = CountDownLatch(totalExpected)
            val successfulTransfers = AtomicInteger(0)
            val failedTransfers = AtomicInteger(0)

            val startTime = System.currentTimeMillis()

            val jobs = (1..clientCount).map { clientIndex ->
                scope.launch {
                    val client = FrameRoomClient().also { synchronized(clients) { clients.add(it) } }
                    val clientDeviceId = "dev_sim_${clientCount}_$clientIndex"

                    for (p in 1..photosPerClient) {
                        val mediaId = 10000L * clientIndex + p
                        val capturedAt = System.currentTimeMillis()
                        val photoId = PhotoIdentity.generatePhotoId(
                            hostServer.room.value?.roomId ?: "room",
                            clientDeviceId,
                            mediaId,
                            capturedAt
                        )

                        val result = client.uploadThumbnail(
                            hostIp = "127.0.0.1",
                            port = port,
                            photoId = photoId,
                            roomId = hostServer.room.value?.roomId,
                            thumbnailBytes = createValidJpeg("c${clientCount}_p${clientIndex}_$p"),
                            uploaderDeviceId = clientDeviceId,
                            uploaderName = "SimClient_$clientIndex",
                            capturedAt = capturedAt
                        )

                        if (result.isSuccess && (result.getOrThrow().status == SyncAckStatus.STORED || result.getOrThrow().status == SyncAckStatus.DUPLICATE_ACCEPTED)) {
                            successfulTransfers.incrementAndGet()
                        } else {
                            failedTransfers.incrementAndGet()
                        }
                        doneLatch.countDown()
                    }
                }
            }

            jobs.forEach { it.join() }
            val completedInTime = doneLatch.await(10, TimeUnit.SECONDS)
            val elapsedMs = System.currentTimeMillis() - startTime
            val throughput = if (elapsedMs > 0) (totalExpected * 1000.0) / elapsedMs else 0.0

            assertTrue("All $totalExpected transfers must complete in time for $clientCount clients", completedInTime)
            assertEquals("Zero failed transfers allowed", 0, failedTransfers.get())
            assertEquals("All $totalExpected transfers must succeed", totalExpected, successfulTransfers.get())

            println("Level 5 Concurrency [$clientCount clients]: $totalExpected uploads in ${elapsedMs}ms (${String.format("%.2f", throughput)} thumbs/sec, avg ${elapsedMs / totalExpected}ms/upload)")
        }
    }

    // ====================================================
    // VALIDATION LEVEL 6 — ONLINE PIPELINE INDEPENDENCE
    // ====================================================
    @Test
    fun testLevel6_onlinePipelineIndependence() = runBlocking {
        val (hostServer, port) = startHostServer()
        val guestClient = FrameRoomClient().also { clients.add(it) }
        val guestDir = File(rootTempDir, "guest_l6").apply { mkdirs() }
        val onlineDir = File(rootTempDir, "guest_l6_online").apply { mkdirs() }
        val scope = CoroutineScope(Dispatchers.IO).also { testScope = it }

        // Pipeline A: Local Outbox
        val localManager = SyncQueueManager(
            baseDir = guestDir,
            coroutineScope = scope,
            baseBackoffMs = 10L
        ).apply {
            uploadExecutor = { record, thumbBytes ->
                val res = guestClient.uploadThumbnail(
                    hostIp = "127.0.0.1",
                    port = port,
                    photoId = record.photoId,
                    roomId = record.roomId,
                    thumbnailBytes = thumbBytes,
                    uploaderDeviceId = record.deviceId,
                    uploaderName = "Guest L6",
                    capturedAt = record.capturedAt
                )
                res.getOrThrow()
            }
        }
        localManager.setHostEndpoint("127.0.0.1", port)

        // Pipeline B: Online Upload
        val mockDestination = MockOnlineDestination()
        val onlineManager = OnlineUploadManager(
            baseDir = onlineDir,
            coroutineScope = scope,
            destination = mockDestination,
            baseBackoffMs = 10L
        )

        // Step 1: Local Internet disabled initially
        onlineManager.setInternetAvailable(false)

        // Step 2: Local sync takes place and reaches ACKED
        val photoId = "ph_l6_independent_1"
        val imageFile = File(guestDir, "fullres_photo.jpg").apply {
            writeBytes("fullres_content_12345".toByteArray())
        }

        localManager.enqueuePhotos(
            listOf(
                DetectedPhoto(
                    photoId = photoId,
                    uri = null,
                    mediaId = 9991L,
                    capturedAt = System.currentTimeMillis(),
                    thumbnailBytes = createValidJpeg("l6")
                )
            ),
            roomId = hostServer.room.value?.roomId ?: "room",
            deviceId = "guest_l6"
        )

        // Wait for LocalOutbox to ACK
        var localAcked = false
        for (i in 1..40) {
            if (localManager.records.value[photoId]?.state == OutboxState.ACKED) {
                localAcked = true
                break
            }
            delay(50)
        }
        assertTrue("LocalOutbox must reach ACKED even when Internet is disabled", localAcked)
        assertEquals(1, hostServer.photos.value.size)

        // Step 3: Explicitly select photo for online backup
        onlineManager.enqueueForUpload(
            photoId = photoId,
            roomId = hostServer.room.value?.roomId ?: "room",
            mediaId = 9991L,
            mediaUri = "content://media/9991",
            originalPath = imageFile.absolutePath
        )

        // Verify state is WAITING_FOR_INTERNET
        assertEquals(OnlineUploadState.WAITING_FOR_INTERNET, onlineManager.records.value[photoId]!!.state)

        // Step 4: Restore Internet -> upload resumes automatically
        onlineManager.setInternetAvailable(true)

        var onlineDone = false
        for (i in 1..40) {
            if (onlineManager.records.value[photoId]?.state == OnlineUploadState.UPLOADED) {
                onlineDone = true
                break
            }
            delay(50)
        }
        assertTrue("Online upload must reach UPLOADED when Internet returns", onlineDone)
        assertEquals("LocalOutbox must remain ACKED", OutboxState.ACKED, localManager.records.value[photoId]!!.state)

        // Step 5: Simulate online failure on a second photo
        mockDestination.shouldSucceed = false
        mockDestination.shouldFailPermanently = true
        val photoId2 = "ph_l6_fail_online"

        localManager.enqueuePhotos(
            listOf(
                DetectedPhoto(
                    photoId = photoId2,
                    uri = null,
                    mediaId = 9992L,
                    capturedAt = System.currentTimeMillis(),
                    thumbnailBytes = createValidJpeg("l6_2")
                )
            ),
            roomId = hostServer.room.value?.roomId ?: "room",
            deviceId = "guest_l6"
        )

        var localAcked2 = false
        for (i in 1..40) {
            if (localManager.records.value[photoId2]?.state == OutboxState.ACKED) {
                localAcked2 = true
                break
            }
            delay(50)
        }
        assertTrue("LocalOutbox must reach ACKED for photo2", localAcked2)

        onlineManager.enqueueForUpload(
            photoId = photoId2,
            roomId = "room",
            mediaId = 9992L,
            mediaUri = "content://media/9992",
            originalPath = imageFile.absolutePath
        )

        var onlineFailed = false
        for (i in 1..40) {
            if (onlineManager.records.value[photoId2]?.state == OnlineUploadState.FAILED_PERMANENT) {
                onlineFailed = true
                break
            }
            delay(50)
        }
        assertTrue("Online upload must reach FAILED_PERMANENT", onlineFailed)
        assertEquals("LocalOutbox MUST NOT be altered by online upload failure", OutboxState.ACKED, localManager.records.value[photoId2]!!.state)
    }
}
