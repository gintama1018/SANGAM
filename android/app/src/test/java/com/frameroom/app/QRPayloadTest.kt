package com.frameroom.app

import com.frameroom.app.core.QRPayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class QRPayloadTest {

    @Test
    fun testQRPayloadSerializationAndParsingWithSessionToken() {
        val original = QRPayload(
            hostIp = "192.168.1.105",
            port = 8080,
            roomId = "FR-8829",
            hostPublicKey = "MCowBQYDK2VuAyEAYV5x8L7mP...",
            sessionToken = "tok_secret_12345"
        )

        val url = original.toUrl()
        assertEquals("fr://192.168.1.105:8080/FR-8829/MCowBQYDK2VuAyEAYV5x8L7mP.../tok_secret_12345", url)

        val parsed = QRPayload.parse(url)
        assertNotNull(parsed)
        assertEquals(original.hostIp, parsed?.hostIp)
        assertEquals(original.port, parsed?.port)
        assertEquals(original.roomId, parsed?.roomId)
        assertEquals(original.hostPublicKey, parsed?.hostPublicKey)
        assertEquals("tok_secret_12345", parsed?.sessionToken)
    }

    @Test
    fun testQRWithoutSessionTokenIsRejected() {
        // Missing 3rd segment (sessionToken) must be rejected
        val twoSegmentUrl = "fr://192.168.1.105:8080/FR-8829/MCowBQYDK2VuAyEAYV5x8L7mP..."
        assertNull("2-segment QR without sessionToken must be rejected", QRPayload.parse(twoSegmentUrl))

        // Blank sessionToken must be rejected
        val blankTokenUrl = "fr://192.168.1.105:8080/FR-8829/MCowBQYDK2VuAyEAYV5x8L7mP.../%20"
        assertNull("Blank sessionToken in QR must be rejected", QRPayload.parse(blankTokenUrl))
    }

    @Test
    fun testInvalidQRPayload() {
        assertNull(QRPayload.parse("https://google.com"))
        assertNull(QRPayload.parse("fr://invalid"))
        assertNull(QRPayload.parse("just random text"))
    }
}
