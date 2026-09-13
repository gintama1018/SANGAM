package com.frameroom.app

import com.frameroom.app.core.CryptoManager
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class CryptoManagerTest {

    @Test
    fun testKeyPairGeneration() {
        val keyPair = CryptoManager.generateKeyPair()
        assertNotNull(keyPair.publicKeyBase64)
        assertNotNull(keyPair.privateKeyBase64)
    }

    @Test
    fun testECDHKeyExchangeAndAESGCM() {
        // Alice (Host)
        val hostKeys = CryptoManager.generateKeyPair()

        // Bob (Guest)
        val guestKeys = CryptoManager.generateKeyPair()

        // Both derive shared secret
        val hostSecret = CryptoManager.deriveSharedSecret(
            hostKeys.privateKeyBase64,
            guestKeys.publicKeyBase64
        )

        val guestSecret = CryptoManager.deriveSharedSecret(
            guestKeys.privateKeyBase64,
            hostKeys.publicKeyBase64
        )

        // Verify keys match
        assertArrayEquals(hostSecret.encoded, guestSecret.encoded)

        // Test encryption / decryption roundtrip
        val testData = "FrameRoom Test Photo JPEG Payload 1234567890".toByteArray(Charsets.UTF_8)
        val encrypted = CryptoManager.encrypt(testData, guestSecret)
        val decrypted = CryptoManager.decrypt(encrypted, hostSecret)

        assertArrayEquals(testData, decrypted)
        assertEquals(String(testData), String(decrypted))
    }
}
