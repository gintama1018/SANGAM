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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.math.max

data class DetectedPhoto(
    val uri: Uri,
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
        return mediaId == other.mediaId
    }

    override fun hashCode(): Int {
        return mediaId.hashCode()
    }
}

class GalleryContentObserver(
    private val context: Context,
    private val coroutineScope: CoroutineScope,
    private val onPhotosDetected: (List<DetectedPhoto>) -> Unit
) : ContentObserver(Handler(Looper.getMainLooper())) {

    private var activeWindowStartMs: Long = 0
    private var isWatching = false
    private val processedMediaIds = mutableSetOf<Long>()

    fun startWatching(eventStartMs: Long) {
        activeWindowStartMs = eventStartMs
        isWatching = true
        context.contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            this
        )
        // Scan any photos taken right at room creation
        scanRecentPhotos()
    }

    fun stopWatching() {
        if (isWatching) {
            isWatching = false
            context.contentResolver.unregisterContentObserver(this)
        }
    }

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        super.onChange(selfChange, uri)
        if (!isWatching) return
        scanRecentPhotos()
    }

    private fun scanRecentPhotos() {
        coroutineScope.launch(Dispatchers.IO) {
            val resolver = context.contentResolver
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media.DATE_TAKEN,
                MediaStore.Images.Media.DATA
            )

            // Convert event start time from ms to seconds for DATE_ADDED
            val minDateAddedSec = (activeWindowStartMs / 1000) - 60 // 1 min margin of safety
            val selection = "${MediaStore.Images.Media.DATE_ADDED} >= ?"
            val selectionArgs = arrayOf(minDateAddedSec.toString())
            val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

            val detected = mutableListOf<DetectedPhoto>()

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

                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idCol)
                        if (processedMediaIds.contains(id)) continue

                        val path = if (dataCol >= 0) cursor.getString(dataCol) ?: "" else ""
                        // Filter strictly for camera rolls (DCIM/Camera)
                        val isCameraPhoto = path.contains("DCIM", ignoreCase = true) ||
                                            path.contains("Camera", ignoreCase = true)

                        if (!isCameraPhoto && path.isNotEmpty()) {
                            continue
                        }

                        val dateAddedSec = cursor.getLong(dateAddedCol)
                        val dateTakenMs = if (dateTakenCol >= 0) cursor.getLong(dateTakenCol) else dateAddedSec * 1000
                        val capturedAt = if (dateTakenMs > 0) dateTakenMs else dateAddedSec * 1000

                        val contentUri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toString())

                        // Extract EXIF & compress thumbnail
                        val photo = processImageUri(resolver, contentUri, id, capturedAt)
                        if (photo != null) {
                            processedMediaIds.add(id)
                            detected.add(photo)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            if (detected.isNotEmpty()) {
                onPhotosDetected(detected)
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

            DetectedPhoto(
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
