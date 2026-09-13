package com.frameroom.app

import com.frameroom.app.core.QRPayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class QRPayloadTest {

    @Test
    fun testQRPayloadSerializationAndParsing() {
        val original = QRPayload(
            hostIp = "192.168.1.105",
            port = 8080,
            roomId = "FR-8829",
            hostPublicKey = "MCowBQYDK2VuAyEAYV5x8L7mP..."
        )

        val url = original.toUrl()
        assertEquals("fr://192.168.1.105:8080/FR-8829/MCowBQYDK2VuAyEAYV5x8L7mP...", url)

        val parsed = QRPayload.parse(url)
        assertNotNull(parsed)
        assertEquals(original.hostIp, parsed?.hostIp)
        assertEquals(original.port, parsed?.port)
        assertEquals(original.roomId, parsed?.roomId)
        assertEquals(original.hostPublicKey, parsed?.hostPublicKey)
    }

    @Test
    fun testInvalidQRPayload() {
        assertNull(QRPayload.parse("https://google.com"))
        assertNull(QRPayload.parse("fr://invalid"))
        assertNull(QRPayload.parse("just random text"))
    }
}
