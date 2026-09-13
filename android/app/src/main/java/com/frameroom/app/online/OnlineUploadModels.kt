package com.frameroom.app.online

import kotlinx.serialization.Serializable

/**
 * 7-State explicit progression for Optional Online Uploads (Pipeline B).
 * Fully independent from Pipeline A (Local Event Outbox).
 */
@Serializable
enum class OnlineUploadState {
    NOT_SELECTED,
    QUEUED,
    WAITING_FOR_INTERNET,
    UPLOADING,
    UPLOADED,
    FAILED_RETRYABLE,
    FAILED_PERMANENT
}

/**
 * Persisted record for an explicitly user-selected online upload candidate.
 * Preserves local references and avoids storing large image byte arrays in JSON.
 */
@Serializable
data class OnlineUploadRecord(
    val photoId: String,
    val roomId: String,
    val mediaId: Long,
    val mediaUri: String,
    val originalPath: String? = null,
    val thumbnailPath: String? = null,
    val selectedAt: Long = System.currentTimeMillis(),
    val state: OnlineUploadState = OnlineUploadState.QUEUED,
    val retryCount: Int = 0,
    val lastAttemptAt: Long? = null,
    val lastError: String? = null,
    val remoteUrl: String? = null,
    val mimeType: String = "image/jpeg",
    val fileSizeBytes: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Result returned by an OnlineDestination upload adapter.
 */
sealed class OnlineUploadResult {
    data class Success(
        val remoteUrl: String,
        val timestamp: Long = System.currentTimeMillis()
    ) : OnlineUploadResult()

    data class Error(
        val message: String,
        val isRetryable: Boolean
    ) : OnlineUploadResult()
}

/**
 * Pluggable abstraction for an internet-based storage destination.
 */
interface OnlineDestination {
    suspend fun uploadPhoto(
        record: OnlineUploadRecord,
        photoBytes: ByteArray,
        mimeType: String
    ): OnlineUploadResult
}

/**
 * Deterministic in-memory / local mock destination for tests and offline validation.
 */
class MockOnlineDestination(
    var shouldSucceed: Boolean = true,
    var shouldFailPermanently: Boolean = false,
    var failureMessage: String = "Network failure"
) : OnlineDestination {
    val uploadedRecords = mutableListOf<OnlineUploadRecord>()

    override suspend fun uploadPhoto(
        record: OnlineUploadRecord,
        photoBytes: ByteArray,
        mimeType: String
    ): OnlineUploadResult {
        return if (shouldSucceed) {
            uploadedRecords.add(record)
            OnlineUploadResult.Success(
                remoteUrl = "https://mock.frameroom.app/uploads/${record.roomId}/${record.photoId}.jpg"
            )
        } else if (shouldFailPermanently) {
            OnlineUploadResult.Error(
                message = failureMessage,
                isRetryable = false
            )
        } else {
            OnlineUploadResult.Error(
                message = failureMessage,
                isRetryable = true
            )
        }
    }
}
