package com.frameroom.app

import com.frameroom.app.core.PhotoIdentity
import com.frameroom.app.core.SyncAck
import com.frameroom.app.core.SyncAckStatus
import com.frameroom.app.watcher.DetectedPhoto
import com.frameroom.app.watcher.OutboxRecord
import com.frameroom.app.watcher.OutboxRepository
import com.frameroom.app.watcher.OutboxState
import com.frameroom.app.watcher.SyncQueueManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class PersistentLocalOutboxTest {

    private lateinit var tempDir: File
    private var queueScope: CoroutineScope? = null

    @Before
    fun setUp() {
        tempDir = File(System.getProperty("java.io.tmpdir"), "outbox_test_" + System.nanoTime()).apply {
            mkdirs()
        }
    }

    @After
    fun tearDown() {
        queueScope?.cancel()
        tempDir.deleteRecursively()
    }

    private fun createPhoto(
        mediaId: Long,
        capturedAt: Long = System.currentTimeMillis(),
        roomId: String = "room_1",
        deviceId: String = "dev_test"
    ): DetectedPhoto {
        val photoId = PhotoIdentity.generatePhotoId(roomId, deviceId, mediaId, capturedAt)
        val dummyBytes = "photo_${mediaId}_thumbnail_content".toByteArray()
        return DetectedPhoto(
            photoId = photoId,
            uri = null,
            mediaId = mediaId,
            capturedAt = capturedAt,
            thumbnailBytes = dummyBytes,
            cameraModel = "TestCamera",
            exposureTime = "1/100s",
            iso = "ISO 200",
            focalLength = "24mm"
        )
    }

    // 1. Newly queued photo is persisted.
    @Test
    fun test1_newlyQueuedPhotoIsPersisted() {
        val scope = CoroutineScope(Dispatchers.IO).also { queueScope = it }
        val manager = SyncQueueManager(baseDir = tempDir, coroutineScope = scope)
        val photo = createPhoto(101L)

        manager.enqueuePhotos(listOf(photo), roomId = "room_1", deviceId = "dev_1")

        // Verify in-memory state
        val record = manager.records.value[photo.photoId]
        assertNotNull("Record should exist in outbox", record)
        assertEquals(photo.photoId, record!!.photoId)
        assertEquals("room_1", record.roomId)
        assertEquals(101L, record.mediaId)

        // Verify disk persistence
        val repo = OutboxRepository(tempDir)
        val onDisk = repo.loadAll().find { it.photoId == photo.photoId }
        assertNotNull("Record must be persisted to disk", onDisk)
        assertEquals(photo.photoId, onDisk!!.photoId)
        assertNotNull("Thumbnail file path must exist", onDisk.thumbnailPath)
        assertTrue("Thumbnail file must exist on disk", File(onDisk.thumbnailPath!!).exists())
    }

    // 2. Persisted photo is recovered after repository recreation.
    @Test
    fun test2_persistedPhotoIsRecoveredAfterRepositoryRecreation() {
        val repo1 = OutboxRepository(tempDir)
        val thumbPath = repo1.saveThumbnail("ph_test_1", "bytes1".toByteArray())
        val record1 = OutboxRecord(
            photoId = "ph_test_1",
            roomId = "room_abc",
            mediaId = 555L,
            mediaUri = "content://media/555",
            deviceId = "dev_a",
            capturedAt = 1710000000000L,
            thumbnailPath = thumbPath,
            state = OutboxState.QUEUED
        )
        repo1.saveAll(listOf(record1))

        // Recreate repository
        val repo2 = OutboxRepository(tempDir)
        val loaded = repo2.loadAll()
        assertEquals(1, loaded.size)
        assertEquals("ph_test_1", loaded[0].photoId)
        assertEquals("room_abc", loaded[0].roomId)
        assertEquals(OutboxState.QUEUED, loaded[0].state)
    }

    // 3. QUEUED photo survives simulated process restart.
    @Test
    fun test3_queuedPhotoSurvivesSimulatedProcessRestart() {
        val scope1 = CoroutineScope(Dispatchers.IO)
        val manager1 = SyncQueueManager(baseDir = tempDir, coroutineScope = scope1)
        val photo = createPhoto(201L)
        manager1.enqueuePhotos(listOf(photo))

        // Simulate process shutdown by discarding manager1
        scope1.cancel()

        // Simulate new process startup
        val scope2 = CoroutineScope(Dispatchers.IO).also { queueScope = it }
        val manager2 = SyncQueueManager(baseDir = tempDir, coroutineScope = scope2)

        val recoveredRecord = manager2.records.value[photo.photoId]
        assertNotNull("Queued photo must survive simulated restart", recoveredRecord)
        assertEquals(photo.photoId, recoveredRecord!!.photoId)
        assertEquals(1, manager2.pendingPhotos.value.size)
    }

    // 4. SENDING photo recovered after simulated crash becomes retryable.
    @Test
    fun test4_sendingPhotoRecoveredAfterCrashBecomesRetryable() {
        val repo = OutboxRepository(tempDir)
        val thumbPath = repo.saveThumbnail("ph_crash_1", "valid_bytes".toByteArray())
        val inFlightRecord = OutboxRecord(
            photoId = "ph_crash_1",
            roomId = "room_crash",
            mediaId = 301L,
            mediaUri = "content://media/301",
            deviceId = "dev_crash",
            capturedAt = 1710000000000L,
            thumbnailPath = thumbPath,
            state = OutboxState.SENDING // App killed while actively sending
        )
        repo.saveAll(listOf(inFlightRecord))

        // Simulate app restart
        val scope = CoroutineScope(Dispatchers.IO).also { queueScope = it }
        val manager = SyncQueueManager(baseDir = tempDir, coroutineScope = scope)

        val recovered = manager.records.value["ph_crash_1"]
        assertNotNull(recovered)
        assertEquals("SENDING record must transition to FAILED_RETRYABLE", OutboxState.FAILED_RETRYABLE, recovered!!.state)
        assertTrue("Error message must note process recreation", recovered.lastError?.contains("process recreation") == true)
    }

    // 5. Host unavailable moves photo to WAITING_FOR_HOST.
    @Test
    fun test5_hostUnavailableMovesPhotoToWaitingForHost() {
        val scope = CoroutineScope(Dispatchers.IO).also { queueScope = it }
        val manager = SyncQueueManager(baseDir = tempDir, coroutineScope = scope)

        // Ensure host is unavailable
        manager.setHostEndpoint(null, 8080)
        assertFalse(manager.isHostAvailable)

        val photo = createPhoto(401L)
        manager.enqueuePhotos(listOf(photo))

        val record = manager.records.value[photo.photoId]
        assertNotNull(record)
        assertEquals(OutboxState.WAITING_FOR_HOST, record!!.state)

        // Also test: if host disconnects while photo is QUEUED
        manager.setHostEndpoint("192.168.1.5", 8080)
        manager.updateRecordState(photo.photoId, OutboxState.QUEUED)
        assertEquals(OutboxState.QUEUED, manager.records.value[photo.photoId]!!.state)

        // Host disconnects
        manager.setHostEndpoint(null, 8080)
        assertEquals(OutboxState.WAITING_FOR_HOST, manager.records.value[photo.photoId]!!.state)
    }

    // 6. Host becomes available and queued photo resumes.
    @Test
    fun test6_hostBecomesAvailableAndQueuedPhotoResumes() = runBlocking {
        val scope = CoroutineScope(Dispatchers.IO).also { queueScope = it }
        val manager = SyncQueueManager(baseDir = tempDir, coroutineScope = scope, baseBackoffMs = 1L)

        // Host not available
        manager.setHostEndpoint(null, 8080)
        val photo = createPhoto(501L)
        manager.enqueuePhotos(listOf(photo))
        assertEquals(OutboxState.WAITING_FOR_HOST, manager.records.value[photo.photoId]!!.state)

        val latch = CountDownLatch(1)
        manager.uploadExecutor = { record, _ ->
            latch.countDown()
            SyncAck(photoId = record.photoId, status = SyncAckStatus.STORED)
        }

        // Host becomes available
        manager.setHostEndpoint("192.168.1.50", 8080)

        val completed = latch.await(3, TimeUnit.SECONDS)
        assertTrue("Queue processing should resume when host is available", completed)

        // Wait brief moment for state write
        delay(100)
        assertEquals(OutboxState.ACKED, manager.records.value[photo.photoId]!!.state)
    }

    // 7. Successful STORED ACK produces ACKED.
    @Test
    fun test7_successfulStoredAckProducesAcked() = runBlocking {
        val scope = CoroutineScope(Dispatchers.IO).also { queueScope = it }
        val manager = SyncQueueManager(baseDir = tempDir, coroutineScope = scope, baseBackoffMs = 1L)
        manager.setHostEndpoint("192.168.1.100", 8080)

        val latch = CountDownLatch(1)
        manager.uploadExecutor = { record, _ ->
            latch.countDown()
            SyncAck(photoId = record.photoId, status = SyncAckStatus.STORED)
        }

        val photo = createPhoto(601L)
        manager.enqueuePhotos(listOf(photo))

        assertTrue(latch.await(3, TimeUnit.SECONDS))
        delay(100)

        val record = manager.records.value[photo.photoId]
        assertNotNull(record)
        assertEquals(OutboxState.ACKED, record!!.state)
    }

    // 8. DUPLICATE_ACCEPTED produces ACKED.
    @Test
    fun test8_duplicateAcceptedProducesAcked() = runBlocking {
        val scope = CoroutineScope(Dispatchers.IO).also { queueScope = it }
        val manager = SyncQueueManager(baseDir = tempDir, coroutineScope = scope, baseBackoffMs = 1L)
        manager.setHostEndpoint("192.168.1.100", 8080)

        val latch = CountDownLatch(1)
        manager.uploadExecutor = { record, _ ->
            latch.countDown()
            SyncAck(photoId = record.photoId, status = SyncAckStatus.DUPLICATE_ACCEPTED)
        }

        val photo = createPhoto(701L)
        manager.enqueuePhotos(listOf(photo))

        assertTrue(latch.await(3, TimeUnit.SECONDS))
        delay(100)

        val record = manager.records.value[photo.photoId]
        assertNotNull(record)
        assertEquals("DUPLICATE_ACCEPTED must transition to ACKED", OutboxState.ACKED, record!!.state)
    }

    // 9. Retryable failure increments retry state.
    @Test
    fun test9_retryableFailureIncrementsRetryState() = runBlocking {
        val scope = CoroutineScope(Dispatchers.IO).also { queueScope = it }
        val manager = SyncQueueManager(baseDir = tempDir, coroutineScope = scope, baseBackoffMs = 1000L)
        manager.setHostEndpoint("192.168.1.100", 8080)

        val latch = CountDownLatch(1)
        manager.uploadExecutor = { _, _ ->
            latch.countDown()
            throw IOException("Socket broken / network glitch")
        }

        val photo = createPhoto(801L)
        manager.enqueuePhotos(listOf(photo))

        assertTrue(latch.await(3, TimeUnit.SECONDS))
        delay(150)

        val record = manager.records.value[photo.photoId]
        assertNotNull(record)
        assertEquals(OutboxState.FAILED_RETRYABLE, record!!.state)
        assertEquals(1, record.retryCount)
        assertTrue(record.lastError?.contains("Socket broken") == true)
    }

    // 10. Retry uses backoff and does not busy-loop.
    @Test
    fun test10_retryUsesBackoffAndDoesNotBusyLoop() {
        val manager = SyncQueueManager(baseDir = tempDir, baseBackoffMs = 1000L, maxBackoffMs = 8000L)

        assertEquals(0L, manager.calculateBackoffMs(0))
        assertEquals(1000L, manager.calculateBackoffMs(1))
        assertEquals(2000L, manager.calculateBackoffMs(2))
        assertEquals(4000L, manager.calculateBackoffMs(3))
        assertEquals(8000L, manager.calculateBackoffMs(4))
        assertEquals(8000L, manager.calculateBackoffMs(5)) // Capped at maxBackoffMs
        assertEquals(8000L, manager.calculateBackoffMs(10)) // Capped
    }

    // 11. Permanent failure does not retry forever.
    @Test
    fun test11_permanentFailureDoesNotRetryForever() = runBlocking {
        val scope = CoroutineScope(Dispatchers.IO).also { queueScope = it }
        val manager = SyncQueueManager(baseDir = tempDir, coroutineScope = scope, baseBackoffMs = 1L)
        manager.setHostEndpoint("192.168.1.100", 8080)

        val attemptCount = AtomicInteger(0)
        val latch = CountDownLatch(1)
        manager.uploadExecutor = { record, _ ->
            attemptCount.incrementAndGet()
            latch.countDown()
            SyncAck(photoId = record.photoId, status = SyncAckStatus.REJECTED_INVALID_PAYLOAD)
        }

        val photo = createPhoto(901L)
        manager.enqueuePhotos(listOf(photo))

        assertTrue(latch.await(3, TimeUnit.SECONDS))
        delay(200)

        val record = manager.records.value[photo.photoId]
        assertNotNull(record)
        assertEquals(OutboxState.FAILED_PERMANENT, record!!.state)
        assertEquals("Should not retry permanent failure", 1, attemptCount.get())
    }

    // 12. Multiple queued photos respect bounded concurrency.
    @Test
    fun test12_multipleQueuedPhotosRespectBoundedConcurrency() = runBlocking {
        val scope = CoroutineScope(Dispatchers.IO).also { queueScope = it }
        val maxConcurrency = 2
        val manager = SyncQueueManager(
            baseDir = tempDir,
            coroutineScope = scope,
            maxConcurrentUploads = maxConcurrency,
            baseBackoffMs = 1L
        )
        manager.setHostEndpoint("192.168.1.100", 8080)

        val currentRunning = AtomicInteger(0)
        val maxObservedConcurrency = AtomicInteger(0)
        val totalPhotos = 6
        val allDoneLatch = CountDownLatch(totalPhotos)

        manager.uploadExecutor = { record, _ ->
            val count = currentRunning.incrementAndGet()
            var max = maxObservedConcurrency.get()
            while (count > max) {
                if (maxObservedConcurrency.compareAndSet(max, count)) break
                max = maxObservedConcurrency.get()
            }

            delay(60) // Simulate network transfer duration
            currentRunning.decrementAndGet()
            allDoneLatch.countDown()
            SyncAck(photoId = record.photoId, status = SyncAckStatus.STORED)
        }

        val photos = (1..totalPhotos).map { createPhoto(1000L + it) }
        manager.enqueuePhotos(photos)

        assertTrue("All uploads must finish within timeout", allDoneLatch.await(5, TimeUnit.SECONDS))
        assertTrue(
            "Max concurrent uploads (${maxObservedConcurrency.get()}) must not exceed limit ($maxConcurrency)",
            maxObservedConcurrency.get() <= maxConcurrency
        )
    }

    // 13. Same photoId remains unchanged across retries.
    @Test
    fun test13_samePhotoIdRemainsUnchangedAcrossRetries() = runBlocking {
        val scope = CoroutineScope(Dispatchers.IO).also { queueScope = it }
        val manager = SyncQueueManager(baseDir = tempDir, coroutineScope = scope, baseBackoffMs = 10L)
        manager.setHostEndpoint("192.168.1.100", 8080)

        val photo = createPhoto(1101L)
        val originalPhotoId = photo.photoId
        val receivedPhotoIds = mutableListOf<String>()

        val attempts = AtomicInteger(0)
        val latch = CountDownLatch(2)

        manager.uploadExecutor = { record, _ ->
            synchronized(receivedPhotoIds) {
                receivedPhotoIds.add(record.photoId)
            }
            val att = attempts.incrementAndGet()
            latch.countDown()
            if (att == 1) {
                throw IOException("Temporary glitch")
            } else {
                SyncAck(photoId = record.photoId, status = SyncAckStatus.STORED)
            }
        }

        manager.enqueuePhotos(listOf(photo))

        // Trigger retry when ready
        assertTrue(latch.await(4, TimeUnit.SECONDS))
        delay(100)

        assertEquals(2, receivedPhotoIds.size)
        assertEquals(originalPhotoId, receivedPhotoIds[0])
        assertEquals(originalPhotoId, receivedPhotoIds[1])
        assertEquals(OutboxState.ACKED, manager.records.value[originalPhotoId]!!.state)
    }

    // 14. Missing local media is handled safely.
    @Test
    fun test14_missingLocalMediaIsHandledSafely() = runBlocking {
        val scope = CoroutineScope(Dispatchers.IO).also { queueScope = it }
        val manager = SyncQueueManager(baseDir = tempDir, coroutineScope = scope, baseBackoffMs = 1L)

        val photo = createPhoto(1201L)
        manager.enqueuePhotos(listOf(photo))

        val record = manager.records.value[photo.photoId]
        assertNotNull(record)
        assertNotNull(record!!.thumbnailPath)

        // Delete the thumbnail file to simulate missing local media
        File(record.thumbnailPath!!).delete()

        // Enable host and trigger processing
        manager.uploadExecutor = { r, _ ->
            SyncAck(photoId = r.photoId, status = SyncAckStatus.STORED)
        }
        manager.setHostEndpoint("192.168.1.100", 8080)

        delay(150)

        val updatedRecord = manager.records.value[photo.photoId]
        assertNotNull(updatedRecord)
        assertEquals("Missing thumbnail file must transition to FAILED_PERMANENT", OutboxState.FAILED_PERMANENT, updatedRecord!!.state)
        assertTrue(updatedRecord.lastError?.contains("Missing local thumbnail file") == true)
    }

    // 15. Queue does not lose records when multiple updates occur quickly.
    @Test
    fun test15_queueDoesNotLoseRecordsWhenMultipleUpdatesOccurQuickly() = runBlocking {
        val scope = CoroutineScope(Dispatchers.IO).also { queueScope = it }
        val manager = SyncQueueManager(baseDir = tempDir, coroutineScope = scope)

        val count = 20
        val photos = (1..count).map { createPhoto(2000L + it) }

        // Enqueue in parallel batches
        val jobs = photos.chunked(5).map { chunk ->
            scope.launch {
                manager.enqueuePhotos(chunk)
            }
        }
        jobs.forEach { it.join() }

        assertEquals(count, manager.records.value.size)

        // Re-read from disk
        val repo = OutboxRepository(tempDir)
        val loaded = repo.loadAll()
        assertEquals("Disk repository must contain all $count records without corruption", count, loaded.size)
    }
}
