package com.frameroom.app.core

import java.security.MessageDigest

object PhotoIdentity {

    /**
     * Generates a deterministic client-side photo identity.
     * photoId = "ph_" + SHA256("$roomId|$deviceId|$mediaId|$capturedAt")[0..31]
     */
    fun generatePhotoId(
        roomId: String,
        deviceId: String,
        mediaId: Long,
        capturedAt: Long
    ): String {
        val raw = "$roomId|$deviceId|$mediaId|$capturedAt"
        val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8))
        val hex = digest.joinToString("") { "%02x".format(it) }
        return "ph_" + hex.take(32)
    }

    /**
     * Computes an optional content fingerprint separate from the primary photo identity.
     */
    fun computeContentHash(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }
}
