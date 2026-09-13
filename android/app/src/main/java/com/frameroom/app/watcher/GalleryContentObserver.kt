package com.frameroom.app.watcher

import android.content.ContentResolver
import android.content.Context
import android.database.ContentObserver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import com.frameroom.app.core.PhotoIdentity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.Collections
import kotlin.math.max

data class DetectedPhoto(
    val photoId: String = "",
    val uri: Uri? = null,
    val mediaId: Long,
    val capturedAt: Long,
    val thumbnailBytes: ByteArray,
    val cameraModel: String? = null,
    val exposureTime: String? = null,
    val iso: String? = null,
    val focalLength: String? = null,
    var isPrivate: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as DetectedPhoto
        return if (photoId.isNotBlank() && other.photoId.isNotBlank()) {
            photoId == other.photoId
        } else {
            mediaId == other.mediaId
        }
    }

    override fun hashCode(): Int {
        return if (photoId.isNotBlank()) photoId.hashCode() else mediaId.hashCode()
    }
}

class GalleryContentObserver(
    private val context: Context,
    private val coroutineScope: CoroutineScope,
    private val onPhotosDetected: (List<DetectedPhoto>) -> Unit
) : ContentObserver(Handler(Looper.getMainLooper())) {

    var sessionState: CaptureSessionState = CaptureSessionState.IDLE
        private set
    var captureSessionStartedAt: Long = 0
        private set
    var activeRoomId: String = "local"
        private set
    var activeDeviceId: String = "local_device"
        private set

    val baselineMediaIds = Collections.synchronizedSet(mutableSetOf<Long>())
    val processedMediaIds = Collections.synchronizedSet(mutableSetOf<Long>())
    val pendingRetryMediaIds = Collections.synchronizedSet(mutableSetOf<Long>())

    fun startWatching(eventStartMs: Long, roomId: String? = null, deviceId: String? = null) {
        sessionState = CaptureSessionState.ACTIVE
        captureSessionStartedAt = eventStartMs
        roomId?.let { activeRoomId = it }
        deviceId?.let { activeDeviceId = it }
        processedMediaIds.clear()
        pendingRetryMediaIds.clear()
        baselineMediaIds.clear()

        // Snapshot baseline of existing MediaStore image IDs
        snapshotBaseline()

        try {
            context.contentResolver.registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                true,
                this
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopWatching() {
        if (sessionState == CaptureSessionState.ACTIVE) {
            sessionState = CaptureSessionState.STOPPED
            try {
                context.contentResolver.unregisterContentObserver(this)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            baselineMediaIds.clear()
            processedMediaIds.clear()
            pendingRetryMediaIds.clear()
        }
    }

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        super.onChange(selfChange, uri)
        if (sessionState != CaptureSessionState.ACTIVE) return
        scanRecentPhotos()
    }

    private fun snapshotBaseline() {
        try {
            val projection = arrayOf(MediaStore.Images.Media._ID)
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                null
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                while (cursor.moveToNext()) {
                    baselineMediaIds.add(cursor.getLong(idCol))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun scanRecentPhotos() {
        coroutineScope.launch(Dispatchers.IO) {
            if (sessionState != CaptureSessionState.ACTIVE) return@launch
            val resolver = context.contentResolver
            val projectionList = mutableListOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media.DATE_TAKEN,
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
                MediaStore.Images.Media.MIME_TYPE,
                MediaStore.Images.Media.SIZE
            )
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                projectionList.add(MediaStore.Images.Media.RELATIVE_PATH)
                projectionList.add(MediaStore.Images.Media.IS_PENDING)
            }
            val projection = projectionList.toTypedArray()

            val minDateAddedSec = (captureSessionStartedAt / 1000) - 10
            val selection = "${MediaStore.Images.Media.DATE_ADDED} >= ?"
            val selectionArgs = arrayOf(minDateAddedSec.toString())
            val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

            val detected = mutableListOf<DetectedPhoto>()
            var needsRetry = false

            try {
                resolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    sortOrder
                )?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                    val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                    val dateTakenCol = cursor.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN)
                    val dataCol = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
                    val bucketCol = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                    val mimeCol = cursor.getColumnIndex(MediaStore.Images.Media.MIME_TYPE)
                    val sizeCol = cursor.getColumnIndex(MediaStore.Images.Media.SIZE)
                    val relPathCol = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        cursor.getColumnIndex(MediaStore.Images.Media.RELATIVE_PATH)
                    } else -1
                    val isPendingCol = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        cursor.getColumnIndex(MediaStore.Images.Media.IS_PENDING)
                    } else -1

                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idCol)
                        if (processedMediaIds.contains(id)) continue

                        val dateAddedSec = cursor.getLong(dateAddedCol)
                        val dateTakenMs = if (dateTakenCol >= 0 && !cursor.isNull(dateTakenCol)) cursor.getLong(dateTakenCol) else null
                        val path = if (dataCol >= 0) cursor.getString(dataCol) else null
                        val bucket = if (bucketCol >= 0) cursor.getString(bucketCol) else null
                        val mime = if (mimeCol >= 0) cursor.getString(mimeCol) else null
                        val size = if (sizeCol >= 0) cursor.getLong(sizeCol) else 0L
                        val relPath = if (relPathCol >= 0) cursor.getString(relPathCol) else null
                        val isPending = if (isPendingCol >= 0) cursor.getInt(isPendingCol) == 1 else false

                        val meta = MediaMetadata(
                            mediaId = id,
                            dateAddedSec = dateAddedSec,
                            dateTakenMs = dateTakenMs,
                            mimeType = mime,
                            bucketDisplayName = bucket,
                            relativePath = relPath,
                            dataPath = path,
                            size = size,
                            isPending = isPending
                        )

                        val eligibility = PhotoEligibilityChecker.evaluate(meta, captureSessionStartedAt, baselineMediaIds)

                        when (eligibility) {
                            EligibilityResult.ELIGIBLE -> {
                                val capturedAt = PhotoEligibilityChecker.resolveCaptureTimestamp(meta)
                                val contentUri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toString())
                                val photo = processImageUri(resolver, contentUri, id, capturedAt)
                                if (photo != null) {
                                    processedMediaIds.add(id)
                                    pendingRetryMediaIds.remove(id)
                                    detected.add(photo)
                                } else {
                                    pendingRetryMediaIds.add(id)
                                    needsRetry = true
                                }
                            }
                            EligibilityResult.DEFERRED_INCOMPLETE_ROW -> {
                                pendingRetryMediaIds.add(id)
                                needsRetry = true
                            }
                            else -> {
                                // Rejected
                                processedMediaIds.add(id)
                                pendingRetryMediaIds.remove(id)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            if (detected.isNotEmpty() && sessionState == CaptureSessionState.ACTIVE) {
                onPhotosDetected(detected)
            }

            if (needsRetry && sessionState == CaptureSessionState.ACTIVE) {
                delay(800)
                if (sessionState == CaptureSessionState.ACTIVE) {
                    scanRecentPhotos()
                }
            }
        }
    }

    private fun processImageUri(
        resolver: ContentResolver,
        uri: Uri,
        mediaId: Long,
        capturedAt: Long
    ): DetectedPhoto? {
        return try {
            // Read EXIF
            var cameraModel: String? = null
            var exposureTime: String? = null
            var iso: String? = null
            var focalLength: String? = null

            try {
                resolver.openInputStream(uri)?.use { stream ->
                    val exif = ExifInterface(stream)
                    val model = exif.getAttribute(ExifInterface.TAG_MODEL)
                    val make = exif.getAttribute(ExifInterface.TAG_MAKE)
                    cameraModel = when {
                        model != null && make != null && !model.startsWith(make) -> "$make $model"
                        model != null -> model
                        else -> null
                    }
                    exposureTime = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)?.let { "${it}s" }
                    iso = exif.getAttribute(ExifInterface.TAG_ISO_SPEED_RATINGS)?.let { "ISO $it" }
                    focalLength = exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH)?.let { "${it}mm" }
                }
            } catch (e: Exception) {
                // EXIF reading optional
            }

            // Downsample & compress thumbnail
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }

            val maxDim = max(options.outWidth, options.outHeight)
            var sampleSize = 1
            while ((maxDim / (sampleSize * 2)) >= 800) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }

            val bitmap = resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return null

            val bos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, bos)
            bitmap.recycle()
            val thumbBytes = bos.toByteArray()

            val photoId = PhotoIdentity.generatePhotoId(
                roomId = activeRoomId,
                deviceId = activeDeviceId,
                mediaId = mediaId,
                capturedAt = capturedAt
            )

            DetectedPhoto(
                photoId = photoId,
                uri = uri,
                mediaId = mediaId,
                capturedAt = capturedAt,
                thumbnailBytes = thumbBytes,
                cameraModel = cameraModel,
                exposureTime = exposureTime,
                iso = iso,
                focalLength = focalLength
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
