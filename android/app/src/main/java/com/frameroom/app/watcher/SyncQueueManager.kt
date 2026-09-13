package com.frameroom.app.watcher

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Collections

class SyncQueueManager {

    private val _pendingPhotos = MutableStateFlow<List<DetectedPhoto>>(emptyList())
    val pendingPhotos = _pendingPhotos.asStateFlow()

    fun addPendingPhotos(photos: List<DetectedPhoto>) {
        synchronized(_pendingPhotos) {
            val current = _pendingPhotos.value.toMutableList()
            for (photo in photos) {
                if (!current.contains(photo)) {
                    current.add(photo)
                }
            }
            _pendingPhotos.value = current
        }
    }

    fun markPrivate(mediaId: Long) {
        synchronized(_pendingPhotos) {
            _pendingPhotos.value = _pendingPhotos.value.map {
                if (it.mediaId == mediaId) it.copy(isPrivate = true) else it
            }
        }
    }

    suspend fun confirmAndUploadAll(uploadAction: suspend (DetectedPhoto) -> Unit) {
        val toUpload = synchronized(_pendingPhotos) {
            val list = _pendingPhotos.value.filter { !it.isPrivate }
            _pendingPhotos.value = emptyList()
            list
        }

        for (photo in toUpload) {
            uploadAction(photo)
        }
    }

    fun dismissAll() {
        synchronized(_pendingPhotos) {
            _pendingPhotos.value = emptyList()
        }
    }

    fun clear() {
        dismissAll()
    }
}
