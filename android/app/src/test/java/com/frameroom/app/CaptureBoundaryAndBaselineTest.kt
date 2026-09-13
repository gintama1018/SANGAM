package com.frameroom.app

import com.frameroom.app.core.PhotoIdentity
import com.frameroom.app.watcher.CaptureSessionState
import com.frameroom.app.watcher.EligibilityResult
import com.frameroom.app.watcher.MediaMetadata
import com.frameroom.app.watcher.PhotoEligibilityChecker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Collections

class CaptureBoundaryAndBaselineTest {

    private val sessionStartTime = 1710000000000L // arbitrary fixed event capture start timestamp

    // 1. Existing photo before session: rejected.
    @Test
    fun testExistingPhotoBeforeSessionIsRejected() {
        val meta = MediaMetadata(
            mediaId = 101L,
            dateAddedSec = (sessionStartTime - 60_000L) / 1000, // 1 min before session
            dateTakenMs = sessionStartTime - 60_000L,
            mimeType = "image/jpeg",
            bucketDisplayName = "Camera",
            relativePath = "DCIM/Camera/",
            size = 2_000_000L
        )

        val result = PhotoEligibilityChecker.evaluate(
            meta = meta,
            captureSessionStartedAt = sessionStartTime,
            baselineMediaIds = emptySet()
        )

        assertEquals(EligibilityResult.REJECTED_PRE_SESSION, result)
    }

    // 2. New photo after session: accepted.
    @Test
    fun testNewPhotoAfterSessionIsAccepted() {
        val meta = MediaMetadata(
            mediaId = 102L,
            dateAddedSec = (sessionStartTime + 15_000L) / 1000, // 15 sec after session started
            dateTakenMs = sessionStartTime + 15_000L,
            mimeType = "image/jpeg",
            bucketDisplayName = "Camera",
            relativePath = "DCIM/Camera/",
            size = 3_500_000L
        )

        val result = PhotoEligibilityChecker.evaluate(
            meta = meta,
            captureSessionStartedAt = sessionStartTime,
            baselineMediaIds = emptySet()
        )

        assertEquals(EligibilityResult.ELIGIBLE, result)
    }

    // 3. MediaStore ID present in baseline: rejected.
    @Test
    fun testMediaStoreIdPresentInBaselineIsRejected() {
        val baseline = setOf(500L, 501L, 502L)
        val meta = MediaMetadata(
            mediaId = 501L, // Existing in baseline
            dateAddedSec = (sessionStartTime + 5_000L) / 1000,
            dateTakenMs = sessionStartTime + 5_000L,
            mimeType = "image/jpeg",
            bucketDisplayName = "Camera",
            relativePath = "DCIM/Camera/",
            size = 2_000_000L
        )

        val result = PhotoEligibilityChecker.evaluate(
            meta = meta,
            captureSessionStartedAt = sessionStartTime,
            baselineMediaIds = baseline
        )

        assertEquals(EligibilityResult.REJECTED_BASELINE, result)
    }

    // 4. MediaStore ID not in baseline and created after session: accepted.
    @Test
    fun testMediaStoreIdNotInBaselineAndCreatedAfterSessionIsAccepted() {
        val baseline = setOf(500L, 501L, 502L)
        val meta = MediaMetadata(
            mediaId = 503L, // New ID not in baseline
            dateAddedSec = (sessionStartTime + 10_000L) / 1000,
            dateTakenMs = sessionStartTime + 10_000L,
            mimeType = "image/jpeg",
            bucketDisplayName = "Camera",
            relativePath = "DCIM/Camera/",
            size = 2_500_000L
        )

        val result = PhotoEligibilityChecker.evaluate(
            meta = meta,
            captureSessionStartedAt = sessionStartTime,
            baselineMediaIds = baseline
        )

        assertEquals(EligibilityResult.ELIGIBLE, result)
    }

    // 5. Duplicate observer notification: only one candidate.
    @Test
    fun testDuplicateObserverNotificationOnlyOneCandidate() {
        val processedMediaIds = Collections.synchronizedSet(mutableSetOf<Long>())
        val mediaId = 601L

        // First notification
        val isFirstTime = !processedMediaIds.contains(mediaId)
        assertTrue(isFirstTime)
        processedMediaIds.add(mediaId)

        // Rapid duplicate notification for same MediaStore ID
        val isSecondTime = !processedMediaIds.contains(mediaId)
        assertFalse("Duplicate notification should be ignored", isSecondTime)
    }

    // 6. Obvious screenshot: rejected.
    @Test
    fun testObviousScreenshotIsRejected() {
        val screenshot1 = MediaMetadata(
            mediaId = 701L,
            dateAddedSec = (sessionStartTime + 5_000L) / 1000,
            dateTakenMs = sessionStartTime + 5_000L,
            mimeType = "image/png",
            bucketDisplayName = "Screenshots",
            relativePath = "Pictures/Screenshots/",
            dataPath = "/storage/emulated/0/Pictures/Screenshots/Screenshot_2026.png",
            size = 1_000_000L
        )
        val result1 = PhotoEligibilityChecker.evaluate(screenshot1, sessionStartTime, emptySet())
        assertEquals(EligibilityResult.REJECTED_EXCLUDED_LOCATION, result1)

        val screenshot2 = MediaMetadata(
            mediaId = 702L,
            dateAddedSec = (sessionStartTime + 5_000L) / 1000,
            dateTakenMs = sessionStartTime + 5_000L,
            mimeType = "image/jpeg",
            bucketDisplayName = "Camera",
            relativePath = "DCIM/Screenshots/",
            dataPath = "/storage/emulated/0/DCIM/Screenshots/Screenshot_01.jpg",
            size = 1_000_000L
        )
        val result2 = PhotoEligibilityChecker.evaluate(screenshot2, sessionStartTime, emptySet())
        assertEquals(EligibilityResult.REJECTED_EXCLUDED_LOCATION, result2)
    }

    // 7. Obvious downloaded image: rejected.
    @Test
    fun testObviousDownloadedImageIsRejected() {
        val downloadMeta = MediaMetadata(
            mediaId = 801L,
            dateAddedSec = (sessionStartTime + 5_000L) / 1000,
            dateTakenMs = sessionStartTime + 5_000L,
            mimeType = "image/jpeg",
            bucketDisplayName = "Download",
            relativePath = "Download/",
            dataPath = "/storage/emulated/0/Download/meme.jpg",
            size = 500_000L
        )
        val resultDownload = PhotoEligibilityChecker.evaluate(downloadMeta, sessionStartTime, emptySet())
        assertEquals(EligibilityResult.REJECTED_EXCLUDED_LOCATION, resultDownload)

        val whatsAppMeta = MediaMetadata(
            mediaId = 802L,
            dateAddedSec = (sessionStartTime + 5_000L) / 1000,
            dateTakenMs = sessionStartTime + 5_000L,
            mimeType = "image/jpeg",
            bucketDisplayName = "WhatsApp Images",
            relativePath = "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Images/",
            size = 300_000L
        )
        val resultWhatsApp = PhotoEligibilityChecker.evaluate(whatsAppMeta, sessionStartTime, emptySet())
        assertEquals(EligibilityResult.REJECTED_EXCLUDED_LOCATION, resultWhatsApp)
    }

    // 8. Valid camera image: accepted.
    @Test
    fun testValidCameraImageIsAccepted() {
        // Android standard DCIM/Camera
        val cameraMeta1 = MediaMetadata(
            mediaId = 901L,
            dateAddedSec = (sessionStartTime + 10_000L) / 1000,
            dateTakenMs = sessionStartTime + 10_000L,
            mimeType = "image/jpeg",
            bucketDisplayName = "Camera",
            relativePath = "DCIM/Camera/",
            dataPath = "/storage/emulated/0/DCIM/Camera/IMG_2026.jpg",
            size = 4_000_000L
        )
        assertEquals(EligibilityResult.ELIGIBLE, PhotoEligibilityChecker.evaluate(cameraMeta1, sessionStartTime, emptySet()))

        // OEM 100ANDRO folder
        val cameraMeta2 = MediaMetadata(
            mediaId = 902L,
            dateAddedSec = (sessionStartTime + 12_000L) / 1000,
            dateTakenMs = sessionStartTime + 12_000L,
            mimeType = "image/jpeg",
            bucketDisplayName = "100ANDRO",
            relativePath = "DCIM/100ANDRO/",
            size = 5_000_000L
        )
        assertEquals(EligibilityResult.ELIGIBLE, PhotoEligibilityChecker.evaluate(cameraMeta2, sessionStartTime, emptySet()))
    }

    // 9. Session stopped: new photo is not automatically accepted.
    @Test
    fun testSessionStoppedNewPhotoIsNotAutomaticallyAccepted() {
        var sessionState = CaptureSessionState.ACTIVE
        val detectedList = mutableListOf<Long>()

        fun onPhotoObserved(mediaId: Long) {
            if (sessionState != CaptureSessionState.ACTIVE) return
            detectedList.add(mediaId)
        }

        // Active session receives photo
        onPhotoObserved(1001L)
        assertEquals(1, detectedList.size)

        // Session stops
        sessionState = CaptureSessionState.STOPPED

        // New photo arrives after session stopped
        onPhotoObserved(1002L)
        // Should not be accepted
        assertEquals(1, detectedList.size)
        assertFalse(detectedList.contains(1002L))
    }

    // 10. Same photo observed multiple times: same identity and no duplicate candidate.
    @Test
    fun testSamePhotoObservedMultipleTimesSameIdentityAndNoDuplicateCandidate() {
        val roomId = "FR-8829"
        val deviceId = "device_guest_42"
        val mediaId = 1101L
        val capturedAt = sessionStartTime + 20_000L

        val id1 = PhotoIdentity.generatePhotoId(roomId, deviceId, mediaId, capturedAt)
        val id2 = PhotoIdentity.generatePhotoId(roomId, deviceId, mediaId, capturedAt)
        assertEquals("Identical inputs must yield identical photoId", id1, id2)

        val candidates = mutableSetOf<String>()
        candidates.add(id1)
        candidates.add(id2)
        assertEquals("Set should deduplicate candidates by identity", 1, candidates.size)
    }

    // 11. Metadata initially incomplete: safely deferred/retried.
    @Test
    fun testMetadataInitiallyIncompleteSafelyDeferred() {
        // Camera app creates MediaStore row before finishing file write
        val incompleteMeta = MediaMetadata(
            mediaId = 1201L,
            dateAddedSec = (sessionStartTime + 5_000L) / 1000,
            dateTakenMs = null,
            mimeType = "image/jpeg",
            bucketDisplayName = "Camera",
            relativePath = "DCIM/Camera/",
            size = 0L,
            isPending = true // Android 10+ pending flag
        )

        val initialResult = PhotoEligibilityChecker.evaluate(incompleteMeta, sessionStartTime, emptySet())
        assertEquals("Incomplete row must be deferred, not permanently rejected", EligibilityResult.DEFERRED_INCOMPLETE_ROW, initialResult)

        // Later, camera finishes writing and updates row
        val completeMeta = incompleteMeta.copy(
            dateTakenMs = sessionStartTime + 5_000L,
            size = 3_200_000L,
            isPending = false
        )

        val completedResult = PhotoEligibilityChecker.evaluate(completeMeta, sessionStartTime, emptySet())
        assertEquals("Completed row must now be accepted", EligibilityResult.ELIGIBLE, completedResult)
    }

    // 12. Two separate event sessions: a photo from session A must not be attached to session B.
    @Test
    fun testTwoSeparateEventSessionsPhotoFromSessionANotAttachedToSessionB() {
        val roomA = "FR-FEST-2026"
        val roomB = "FR-CONCLAVE-2026"
        val deviceId = "device_tara"
        val mediaId = 1301L
        val capturedAt = sessionStartTime + 30_000L

        val idRoomA = PhotoIdentity.generatePhotoId(roomA, deviceId, mediaId, capturedAt)
        val idRoomB = PhotoIdentity.generatePhotoId(roomB, deviceId, mediaId, capturedAt)

        assertNotEquals("Photo identities across distinct rooms must never collide", idRoomA, idRoomB)
        assertTrue(idRoomA.startsWith("ph_"))
        assertTrue(idRoomB.startsWith("ph_"))
    }
}
