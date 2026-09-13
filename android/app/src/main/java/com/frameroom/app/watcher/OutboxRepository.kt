package com.frameroom.app.watcher

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
enum class OutboxState {
    DETECTED,
    ELIGIBLE,
    QUEUED,
    WAITING_FOR_HOST,
    SENDING,
    ACKED,
    FAILED_RETRYABLE,
    FAILED_PERMANENT
}

@Serializable
data class OutboxRecord(
    val photoId: String,
    val roomId: String,
    val mediaId: Long,
    val mediaUri: String,
    val deviceId: String,
    val capturedAt: Long,
    val thumbnailPath: String? = null,
    val cameraModel: String? = null,
    val exposureTime: String? = null,
    val iso: String? = null,
    val focalLength: String? = null,
    val isPrivate: Boolean = false,
    val state: OutboxState = OutboxState.QUEUED,
    val retryCount: Int = 0,
    val lastAttemptAt: Long? = null,
    val lastError: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

class OutboxRepository(baseDir: File) {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    val outboxDir = File(baseDir, "outbox").apply { mkdirs() }
    val thumbsDir = File(outboxDir, "thumbs").apply { mkdirs() }
    val recordsFile = File(outboxDir, "records.json")

    @Synchronized
    fun loadAll(): List<OutboxRecord> {
        if (!recordsFile.exists()) return emptyList()
        return try {
            val content = recordsFile.readText(Charsets.UTF_8)
            if (content.isBlank()) emptyList() else json.decodeFromString<List<OutboxRecord>>(content)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    @Synchronized
    fun saveAll(records: Collection<OutboxRecord>) {
        try {
            val tempFile = File(outboxDir, "records.json.tmp")
            val content = json.encodeToString(records.toList())
            tempFile.writeText(content, Charsets.UTF_8)
            try {
                java.nio.file.Files.move(
                    tempFile.toPath(),
                    recordsFile.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                    java.nio.file.StandardCopyOption.ATOMIC_MOVE
                )
            } catch (e: Exception) {
                // Fallback without ATOMIC_MOVE (e.g. cross-filesystem or older OS)
                java.nio.file.Files.move(
                    tempFile.toPath(),
                    recordsFile.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun saveThumbnail(photoId: String, bytes: ByteArray): String {
        val file = File(thumbsDir, "${photoId}_thumb.jpg")
        file.writeBytes(bytes)
        return file.absolutePath
    }

    fun readThumbnail(thumbnailPath: String): ByteArray? {
        val file = File(thumbnailPath)
        return if (file.exists() && file.length() > 0) file.readBytes() else null
    }

    @Synchronized
    fun recoverRecords(): List<OutboxRecord> {
        val loaded = loadAll()
        val recovered = loaded.map { record ->
            when (record.state) {
                OutboxState.SENDING -> {
                    // Crashed/killed while actively sending. Mark FAILED_RETRYABLE so it retries safely.
                    record.copy(
                        state = OutboxState.FAILED_RETRYABLE,
                        lastError = "Interrupted by process recreation",
                        updatedAt = System.currentTimeMillis()
                    )
                }
                OutboxState.QUEUED,
                OutboxState.WAITING_FOR_HOST,
                OutboxState.FAILED_RETRYABLE -> {
                    // Check if local thumbnail file still exists
                    val exists = record.thumbnailPath?.let { File(it).exists() } == true
                    if (!exists) {
                        record.copy(
                            state = OutboxState.FAILED_PERMANENT,
                            lastError = "Local thumbnail file missing or unrecoverable",
                            updatedAt = System.currentTimeMillis()
                        )
                    } else {
                        record
                    }
                }
                OutboxState.ACKED,
                OutboxState.FAILED_PERMANENT,
                OutboxState.DETECTED,
                OutboxState.ELIGIBLE -> record
            }
        }
        saveAll(recovered)
        return recovered
    }

    @Synchronized
    fun clear() {
        recordsFile.delete()
        thumbsDir.deleteRecursively()
        thumbsDir.mkdirs()
    }
}
