package com.frameroom.app.archive

import android.content.Context
import com.frameroom.app.core.Participant
import com.frameroom.app.core.PhotoMeta
import com.frameroom.app.core.Room
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@Serializable
data class ArchiveManifest(
    val room: Room,
    val totalPhotos: Int,
    val totalContributors: Int,
    val participants: List<Participant>,
    val photos: List<PhotoMeta>,
    val exportedAt: Long = System.currentTimeMillis()
)

object RoomArchiveManager {

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    /**
     * Packages room manifest and all cached photos into an exportable ZIP archive.
     */
    fun createArchiveZip(
        context: Context,
        room: Room,
        participants: List<Participant>,
        photos: List<PhotoMeta>,
        photoFiles: List<File>
    ): File? {
        return try {
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val sanitizedName = room.name.replace(Regex("[^a-zA-Z0-9_]"), "_")
            val zipFile = File(exportDir, "FrameRoom_${sanitizedName}_${room.roomId}.zip")

            val manifest = ArchiveManifest(
                room = room,
                totalPhotos = photos.size,
                totalContributors = participants.size,
                participants = participants,
                photos = photos
            )
            val manifestJson = json.encodeToString(manifest)

            ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                // Write manifest.json
                val manifestEntry = ZipEntry("manifest.json")
                zos.putNextEntry(manifestEntry)
                zos.write(manifestJson.toByteArray(Charsets.UTF_8))
                zos.closeEntry()

                // Write photos
                val buffer = ByteArray(8192)
                for (file in photoFiles) {
                    if (!file.exists()) continue
                    val photoEntry = ZipEntry("photos/${file.name}")
                    zos.putNextEntry(photoEntry)
                    FileInputStream(file).use { fis ->
                        var len: Int
                        while (fis.read(buffer).also { len = it } > 0) {
                            zos.write(buffer, 0, len)
                        }
                    }
                    zos.closeEntry()
                }
            }

            zipFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Purges photo files older than the retention duration (default: 7 days) if room is closed.
     */
    fun checkAndPurgeExpiredRooms(context: Context, maxAgeMs: Long = 7L * 24 * 60 * 60 * 1000) {
        try {
            val photosDir = File(context.filesDir, "photos")
            if (!photosDir.exists()) return

            val now = System.currentTimeMillis()
            photosDir.walkBottomUp().forEach { file ->
                if (file.isFile && (now - file.lastModified()) > maxAgeMs) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
