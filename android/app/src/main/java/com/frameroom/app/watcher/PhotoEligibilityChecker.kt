package com.frameroom.app.watcher

data class MediaMetadata(
    val mediaId: Long,
    val dateAddedSec: Long,
    val dateTakenMs: Long? = null,
    val mimeType: String? = null,
    val bucketDisplayName: String? = null,
    val relativePath: String? = null,
    val dataPath: String? = null,
    val size: Long = 0L,
    val isPending: Boolean = false
)

enum class EligibilityResult {
    ELIGIBLE,
    REJECTED_BASELINE,
    REJECTED_PRE_SESSION,
    REJECTED_EXCLUDED_LOCATION,
    REJECTED_NOT_AN_IMAGE,
    REJECTED_NON_CAMERA_SOURCE,
    DEFERRED_INCOMPLETE_ROW
}

enum class CaptureSessionState {
    IDLE,
    ACTIVE,
    STOPPED
}

object PhotoEligibilityChecker {

    // Known non-camera directories to reject explicitly
    private val EXCLUDED_DIRECTORIES = listOf(
        "Screenshots",
        "Screenshot",
        "WhatsApp",
        "Telegram",
        "Download",
        "Downloads",
        "Instagram",
        "Snapchat",
        "Twitter",
        "Facebook",
        "Messenger",
        "Reddit"
    )

    fun evaluate(
        meta: MediaMetadata,
        captureSessionStartedAt: Long,
        baselineMediaIds: Set<Long>
    ): EligibilityResult {
        // 1. Incomplete MediaStore row check (file is currently being written by camera app)
        if (meta.isPending || meta.size == 0L) {
            return EligibilityResult.DEFERRED_INCOMPLETE_ROW
        }

        // 2. MIME type check: must be an image
        val mime = meta.mimeType
        if (mime != null && !mime.startsWith("image/", ignoreCase = true)) {
            return EligibilityResult.REJECTED_NOT_AN_IMAGE
        }

        // 3. Baseline snapshot check: if ID existed before session started, reject
        if (baselineMediaIds.contains(meta.mediaId)) {
            return EligibilityResult.REJECTED_BASELINE
        }

        // 4. Capture session timestamp boundary check
        val capturedAt = resolveCaptureTimestamp(meta)
        if (capturedAt < captureSessionStartedAt) {
            return EligibilityResult.REJECTED_PRE_SESSION
        }

        // 5. Excluded non-camera folder check
        if (isExcludedFolder(meta)) {
            return EligibilityResult.REJECTED_EXCLUDED_LOCATION
        }

        // 6. Camera source filtering
        if (!isCameraOrIndeterminate(meta)) {
            return EligibilityResult.REJECTED_NON_CAMERA_SOURCE
        }

        return EligibilityResult.ELIGIBLE
    }

    fun resolveCaptureTimestamp(meta: MediaMetadata): Long {
        return meta.dateTakenMs?.takeIf { it > 0 } ?: (meta.dateAddedSec * 1000)
    }

    fun isExcludedFolder(meta: MediaMetadata): Boolean {
        val path = meta.dataPath ?: ""
        val bucket = meta.bucketDisplayName ?: ""
        val relPath = meta.relativePath ?: ""

        return EXCLUDED_DIRECTORIES.any { excluded ->
            path.contains(excluded, ignoreCase = true) ||
            bucket.contains(excluded, ignoreCase = true) ||
            relPath.contains(excluded, ignoreCase = true)
        }
    }

    fun isCameraOrIndeterminate(meta: MediaMetadata): Boolean {
        val path = meta.dataPath ?: ""
        val bucket = meta.bucketDisplayName ?: ""
        val relPath = meta.relativePath ?: ""

        // Obvious camera indicators across Android versions and OEMs
        val isExplicitCamera = bucket.equals("Camera", ignoreCase = true) ||
                               bucket.equals("100ANDRO", ignoreCase = true) ||
                               bucket.equals("100MEDIA", ignoreCase = true) ||
                               relPath.startsWith("DCIM/Camera", ignoreCase = true) ||
                               relPath.startsWith("DCIM", ignoreCase = true) ||
                               path.contains("/DCIM/Camera", ignoreCase = true) ||
                               path.contains("/DCIM/", ignoreCase = true) ||
                               path.contains("Camera", ignoreCase = true)

        if (isExplicitCamera) return true

        // If neither DCIM nor explicit Camera, check if it's indeterminate
        val hasSpecificNonCameraPath = (relPath.isNotBlank() && !relPath.startsWith("DCIM")) ||
                                       (path.isNotBlank() && !path.contains("DCIM"))
        if (hasSpecificNonCameraPath && bucket.isNotBlank() && !bucket.equals("Camera", ignoreCase = true)) {
            return false
        }

        // Per product rule: "If source cannot be determined reliably, prefer: 'eligible candidate for confirmation'"
        return true
    }
}
