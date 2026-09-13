package com.frameroom.app.online

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Durable, disk-backed repository for Pipeline B (Online Upload Queue).
 * Stored in `online_outbox/` to ensure 100% isolation from Local Outbox.
 */
class OnlineUploadRepository(baseDir: File) {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    val outboxDir = File(baseDir, "online_outbox").apply { mkdirs() }
    val recordsFile = File(outboxDir, "records.json")

    @Synchronized
    fun loadAll(): List<OnlineUploadRecord> {
        if (!recordsFile.exists()) return emptyList()
        return try {
            val content = recordsFile.readText(Charsets.UTF_8)
            if (content.isBlank()) emptyList() else json.decodeFromString<List<OnlineUploadRecord>>(content)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    @Synchronized
    fun saveAll(records: Collection<OnlineUploadRecord>) {
        try {
            val tempFile = File(outboxDir, "records.json.tmp")
            val content = json.encodeToString(records.toList())
            tempFile.writeText(content, Charsets.UTF_8)
            try {
                Files.move(
                    tempFile.toPath(),
                    recordsFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
                )
            } catch (e: Exception) {
                Files.move(
                    tempFile.toPath(),
                    recordsFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Synchronized
    fun recoverRecords(): List<OnlineUploadRecord> {
        val loaded = loadAll()
        val recovered = loaded.map { record ->
            when (record.state) {
                OnlineUploadState.UPLOADING -> {
                    // Crashed during network upload -> mark retryable
                    record.copy(
                        state = OnlineUploadState.FAILED_RETRYABLE,
                        lastError = "Interrupted by process recreation",
                        updatedAt = System.currentTimeMillis()
                    )
                }
                OnlineUploadState.QUEUED,
                OnlineUploadState.WAITING_FOR_INTERNET,
                OnlineUploadState.FAILED_RETRYABLE -> {
                    // Check if referenced media is valid
                    val hasDiskFile = record.originalPath?.let { File(it).exists() } == true ||
                            record.thumbnailPath?.let { File(it).exists() } == true
                    val hasMediaUri = record.mediaUri.isNotBlank()
                    if (!hasDiskFile && !hasMediaUri) {
                        record.copy(
                            state = OnlineUploadState.FAILED_PERMANENT,
                            lastError = "Referenced media unavailable or removed",
                            updatedAt = System.currentTimeMillis()
                        )
                    } else {
                        record
                    }
                }
                OnlineUploadState.UPLOADED,
                OnlineUploadState.FAILED_PERMANENT,
                OnlineUploadState.NOT_SELECTED -> record
            }
        }
        saveAll(recovered)
        return recovered
    }

    @Synchronized
    fun clear() {
        recordsFile.delete()
    }
}
