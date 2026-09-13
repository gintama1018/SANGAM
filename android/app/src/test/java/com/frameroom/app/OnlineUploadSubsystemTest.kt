package com.frameroom.app

import com.frameroom.app.core.PhotoIdentity
import com.frameroom.app.core.SyncAck
import com.frameroom.app.core.SyncAckStatus
import com.frameroom.app.online.MockOnlineDestination
import com.frameroom.app.online.OnlineUploadManager
import com.frameroom.app.online.OnlineUploadRecord
import com.frameroom.app.online.OnlineUploadRepository
import com.frameroom.app.online.OnlineUploadResult
import com.frameroom.app.online.OnlineUploadState
import com.frameroom.app.watcher.DetectedPhoto
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class OnlineUploadSubsystemTest {

    private lateinit var tempDir: File
    private var testScope: CoroutineScope? = null

    @Before
    fun setUp() {
        tempDir = File(System.getProperty("java.io.tmpdir"), "online_test_" + System.nanoTime()).apply {
            mkdirs()
        }
    }

    @After
    fun tearDown() {
        testScope?.cancel()
        tempDir.deleteRecursively()
    }

    private fun createDummyImageFile(name: String): File {
        val file = File(tempDir, "$name.jpg")
        file.writeBytes("dummy_lossless_fullres_image_bytes_for_$name".toByteArray())
        return file
    }

    // 1. Explicitly selected photo enters OnlineUploadQueue.
    @Test
    fun test1_explicitlySelectedPhotoEntersOnlineUploadQueue() {
        val scope = CoroutineScope(Dispatchers.IO).also { testScope = it }
        val manager = OnlineUploadManager(baseDir = tempDir, coroutineScope = scope)
        val image = createDummyImageFile("photo_1")

        manager.enqueueForUpload(
            photoId = "ph_selected_1",
            roomId = "room_1",
            mediaId = 1001L,
            mediaUri = "content://media/1001",
            originalPath = image.absolutePath
        )

        val record = manager.records.value["ph_selected_1"]
        assertNotNull("Explicitly selected photo must enter queue", record)
        assertEquals("ph_selected_1", record!!.photoId)
        assertEquals("room_1", record.roomId)

        // Verify disk persistence in online_outbox
        val repo = OnlineUploadRepository(tempDir)
        val onDisk = repo.loadAll().find { it.photoId == "ph_selected_1" }
        assertNotNull("Online record must be durably persisted to disk", onDisk)
        assertEquals("ph_selected_1", onDisk!!.photoId)
    }

    // 2. Unselected photo never enters OnlineUploadQueue.
    @Test
    fun test2_unselectedPhotoNeverEntersOnlineUploadQueue() {
        val scope = CoroutineScope(Dispatchers.IO).also { testScope = it }
        val manager = OnlineUploadManager(baseDir = tempDir, coroutineScope = scope)
        val image = createDummyImageFile("photo_selected")

        // User explicitly selects only photo A
        manager.enqueueForUpload(
            photoId = "ph_A",
            roomId = "room_1",
            mediaId = 2001L,
            mediaUri = "content://media/2001",
            originalPath = image.absolutePath
        )

        // Photo B was taken during event but NOT selected for online upload
        val unselectedRecord = manager.records.value["ph_B"]
        assertNull("Unselected photo must NEVER enter online upload queue", unselectedRecord)
        assertEquals(1, manager.records.value.size)
    }

    // 3. Internet unavailable -> WAITING_FOR_INTERNET.
    @Test
    fun test3_internetUnavailableMovesToWaitingForInternet() {
        val scope = CoroutineScope(Dispatchers.IO).also { testScope = it }
        val manager = OnlineUploadManager(baseDir = tempDir, coroutineScope = scope)
        val image = createDummyImageFile("photo_offline")

        // Internet is disabled
        manager.setInternetAvailable(false)
        assertFalse(manager.isInternetAvailable)

        manager.enqueueForUpload(
            photoId = "ph_offline_1",
            roomId = "room_1",
            mediaId = 3001L,
            mediaUri = "content://media/3001",
            originalPath = image.absolutePath
        )

        val record = manager.records.value["ph_offline_1"]
        assertNotNull(record)
        assertEquals(OnlineUploadState.WAITING_FOR_INTERNET, record!!.state)

        // Also test: if Internet is lost while queued
        manager.setInternetAvailable(true)
        manager.updateRecordState("ph_offline_1", OnlineUploadState.QUEUED)
        manager.setInternetAvailable(false)
        assertEquals(OnlineUploadState.WAITING_FOR_INTERNET, manager.records.value["ph_offline_1"]!!.state)
    }

    // 4. Internet returns -> queue resumes.
    @Test
    fun test4_internetReturnsQueueResumes() = runBlocking {
        val scope = CoroutineScope(Dispatchers.IO).also { testScope = it }
        val destination = MockOnlineDestination()
        val manager = OnlineUploadManager(
            baseDir = tempDir,
            coroutineScope = scope,
            destination = destination,
            baseBackoffMs = 1L
        )
        val image = createDummyImageFile("photo_resume")

        // Start offline
        manager.setInternetAvailable(false)
        manager.enqueueForUpload(
            photoId = "ph_resume_1",
            roomId = "room_1",
            mediaId = 4001L,
            mediaUri = "content://media/4001",
            originalPath = image.absolutePath
        )
        assertEquals(OnlineUploadState.WAITING_FOR_INTERNET, manager.records.value["ph_resume_1"]!!.state)

        // Restore Internet
        manager.setInternetAvailable(true)

        delay(200)

        val record = manager.records.value["ph_resume_1"]
        assertNotNull(record)
        assertEquals(OnlineUploadState.UPLOADED, record!!.state)
        assertEquals(1, destination.uploadedRecords.size)
    }

    // 5. Successful upload -> ONLINE_UPLOADED.
    @Test
    fun test5_successfulUploadProducesUploaded() = runBlocking {
        val scope = CoroutineScope(Dispatchers.IO).also { testScope = it }
        val destination = MockOnlineDestination()
        val manager = OnlineUploadManager(
            baseDir = tempDir,
            coroutineScope = scope,
            destination = destination,
            baseBackoffMs = 1L
        )
        val image = createDummyImageFile("photo_success")

        manager.enqueueForUpload(
            photoId = "ph_success_1",
            roomId = "room_1",
            mediaId = 5001L,
            mediaUri = "content://media/5001",
            originalPath = image.absolutePath
        )

        delay(150)

        val record = manager.records.value["ph_success_1"]
        assertNotNull(record)
        assertEquals(OnlineUploadState.UPLOADED, record!!.state)
        assertTrue(record.remoteUrl?.contains("mock.frameroom.app") == true)
    }

    // 6. Retryable failure -> FAILED_RETRYABLE.
    @Test
    fun test6_retryableFailureProducesFailedRetryable() = runBlocking {
        val scope = CoroutineScope(Dispatchers.IO).also { testScope = it }
        val destination = MockOnlineDestination(shouldSucceed = false, shouldFailPermanently = false, failureMessage = "HTTP 503")
        val manager = OnlineUploadManager(
            baseDir = tempDir,
            coroutineScope = scope,
            destination = destination,
            baseBackoffMs = 1000L // 1000ms delay so assertion catches FAILED_RETRYABLE
        )
        val image = createDummyImageFile("photo_retry")

        manager.enqueueForUpload(
            photoId = "ph_retry_1",
            roomId = "room_1",
            mediaId = 6001L,
            mediaUri = "content://media/6001",
            originalPath = image.absolutePath
        )

        delay(150)

        val record = manager.records.value["ph_retry_1"]
        assertNotNull(record)
        assertEquals(OnlineUploadState.FAILED_RETRYABLE, record!!.state)
        assertEquals(1, record.retryCount)
        assertTrue(record.lastError?.contains("HTTP 503") == true)
    }

    // 7. Permanent failure -> FAILED_PERMANENT.
    @Test
    fun test7_permanentFailureProducesFailedPermanent() = runBlocking {
        val scope = CoroutineScope(Dispatchers.IO).also { testScope = it }
        val destination = MockOnlineDestination(shouldSucceed = false, shouldFailPermanently = true, failureMessage = "HTTP 400 Invalid Auth")
        val manager = OnlineUploadManager(
            baseDir = tempDir,
            coroutineScope = scope,
            destination = destination,
            baseBackoffMs = 1L
        )
        val image = createDummyImageFile("photo_perm_fail")

        manager.enqueueForUpload(
            photoId = "ph_perm_1",
            roomId = "room_1",
            mediaId = 7001L,
            mediaUri = "content://media/7001",
            originalPath = image.absolutePath
        )

        delay(150)

        val record = manager.records.value["ph_perm_1"]
        assertNotNull(record)
        assertEquals(OnlineUploadState.FAILED_PERMANENT, record!!.state)
        assertTrue(record.lastError?.contains("Permanent failure") == true)
    }

    // 8. App restart preserves queued online uploads.
    @Test
    fun test8_appRestartPreservesQueuedOnlineUploads() {
        val scope1 = CoroutineScope(Dispatchers.IO)
        val manager1 = OnlineUploadManager(baseDir = tempDir, coroutineScope = scope1)
        val image = createDummyImageFile("photo_survive")

        // Put in offline state so it remains pending
        manager1.setInternetAvailable(false)
        manager1.enqueueForUpload(
            photoId = "ph_survive_1",
            roomId = "room_survive",
            mediaId = 8001L,
            mediaUri = "content://media/8001",
            originalPath = image.absolutePath
        )

        // Kill manager1
        scope1.cancel()

        // Restart with new manager instance
        val scope2 = CoroutineScope(Dispatchers.IO).also { testScope = it }
        val manager2 = OnlineUploadManager(baseDir = tempDir, coroutineScope = scope2)

        val recovered = manager2.records.value["ph_survive_1"]
        assertNotNull("Record must survive process restart", recovered)
        assertEquals("ph_survive_1", recovered!!.photoId)
        assertEquals("room_survive", recovered.roomId)
    }

    // 9. Local ACKED + Online pending can coexist.
    @Test
    fun test9_localAckedAndOnlinePendingCoexist() {
        val localDir = File(tempDir, "local_pipe").apply { mkdirs() }
        val onlineDir = File(tempDir, "online_pipe").apply { mkdirs() }

        val localScope = CoroutineScope(Dispatchers.IO)
        val onlineScope = CoroutineScope(Dispatchers.IO).also { testScope = it }

        val localManager = SyncQueueManager(baseDir = localDir, coroutineScope = localScope)
        val onlineManager = OnlineUploadManager(baseDir = onlineDir, coroutineScope = onlineScope)

        val photoId = "ph_coexist_100"
        val image = createDummyImageFile("photo_coexist")

        // Pipeline A: Photo is locally ACKED by Host
        localManager.enqueuePhotos(
            listOf(
                DetectedPhoto(
                    photoId = photoId,
                    uri = null,
                    mediaId = 9001L,
                    capturedAt = 1710000000000L,
                    thumbnailBytes = "thumb".toByteArray()
                )
            )
        )
        localManager.updateRecordState(photoId, OutboxState.ACKED)

        // Pipeline B: Photo is WAITING_FOR_INTERNET
        onlineManager.setInternetAvailable(false)
        onlineManager.enqueueForUpload(
            photoId = photoId,
            roomId = "room_coexist",
            mediaId = 9001L,
            mediaUri = "content://media/9001",
            originalPath = image.absolutePath
        )

        // Assert coexistence
        assertEquals("Pipeline A must be ACKED", OutboxState.ACKED, localManager.records.value[photoId]!!.state)
        assertEquals("Pipeline B must be WAITING_FOR_INTERNET", OnlineUploadState.WAITING_FOR_INTERNET, onlineManager.records.value[photoId]!!.state)

        localScope.cancel()
    }

    // 10. Online upload failure does not alter LocalOutbox state.
    @Test
    fun test10_onlineUploadFailureDoesNotAlterLocalOutboxState() = runBlocking {
        val localDir = File(tempDir, "local_pipe_2").apply { mkdirs() }
        val onlineDir = File(tempDir, "online_pipe_2").apply { mkdirs() }

        val localScope = CoroutineScope(Dispatchers.IO)
        val onlineScope = CoroutineScope(Dispatchers.IO).also { testScope = it }

        val localManager = SyncQueueManager(baseDir = localDir, coroutineScope = localScope)
        val destination = MockOnlineDestination(shouldSucceed = false, shouldFailPermanently = true, failureMessage = "Cloud Quota Exceeded")
        val onlineManager = OnlineUploadManager(baseDir = onlineDir, coroutineScope = onlineScope, destination = destination, baseBackoffMs = 1L)

        val photoId = "ph_independent_200"
        val image = createDummyImageFile("photo_independent")

        // 1. Photo is ACKED in LocalOutbox
        localManager.enqueuePhotos(
            listOf(
                DetectedPhoto(
                    photoId = photoId,
                    uri = null,
                    mediaId = 9002L,
                    capturedAt = 1710000000000L,
                    thumbnailBytes = "thumb".toByteArray()
                )
            )
        )
        localManager.updateRecordState(photoId, OutboxState.ACKED)
        assertEquals(OutboxState.ACKED, localManager.records.value[photoId]!!.state)

        // 2. Online upload fails permanently
        onlineManager.enqueueForUpload(
            photoId = photoId,
            roomId = "room_indep",
            mediaId = 9002L,
            mediaUri = "content://media/9002",
            originalPath = image.absolutePath
        )

        delay(150)

        // 3. Online queue marked FAILED_PERMANENT
        assertEquals(OnlineUploadState.FAILED_PERMANENT, onlineManager.records.value[photoId]!!.state)

        // 4. LocalOutbox MUST REMAIN ACKED! Zero contamination!
        assertEquals("LocalOutbox must remain ACKED regardless of online failure", OutboxState.ACKED, localManager.records.value[photoId]!!.state)

        localScope.cancel()
    }

    // 11. Multiple queued uploads respect bounded concurrency.
    @Test
    fun test11_multipleQueuedUploadsRespectBoundedConcurrency() = runBlocking {
        val scope = CoroutineScope(Dispatchers.IO).also { testScope = it }
        val maxConcurrency = 2
        val currentRunning = AtomicInteger(0)
        val maxObservedConcurrency = AtomicInteger(0)
        val totalPhotos = 6
        val allDoneLatch = CountDownLatch(totalPhotos)

        val customDest = object : com.frameroom.app.online.OnlineDestination {
            override suspend fun uploadPhoto(
                record: OnlineUploadRecord,
                photoBytes: ByteArray,
                mimeType: String
            ): OnlineUploadResult {
                val count = currentRunning.incrementAndGet()
                var max = maxObservedConcurrency.get()
                while (count > max) {
                    if (maxObservedConcurrency.compareAndSet(max, count)) break
                    max = maxObservedConcurrency.get()
                }

                delay(60) // Transfer latency
                currentRunning.decrementAndGet()
                allDoneLatch.countDown()
                return OnlineUploadResult.Success("https://mock.com/${record.photoId}")
            }
        }

        val manager = OnlineUploadManager(
            baseDir = tempDir,
            coroutineScope = scope,
            destination = customDest,
            maxConcurrentUploads = maxConcurrency,
            baseBackoffMs = 1L
        )

        for (i in 1..totalPhotos) {
            val img = createDummyImageFile("photo_concurr_$i")
            manager.enqueueForUpload(
                photoId = "ph_conc_$i",
                roomId = "room_conc",
                mediaId = 10000L + i,
                mediaUri = "content://media/${10000L + i}",
                originalPath = img.absolutePath
            )
        }

        assertTrue("All online uploads must complete", allDoneLatch.await(5, TimeUnit.SECONDS))
        assertTrue(
            "Max concurrent online uploads (${maxObservedConcurrency.get()}) must not exceed limit ($maxConcurrency)",
            maxObservedConcurrency.get() <= maxConcurrency
        )
    }
}
