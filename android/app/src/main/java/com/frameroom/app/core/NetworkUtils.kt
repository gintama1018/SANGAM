package com.frameroom.app.core

import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections

object NetworkUtils {

    /**
     * Discovers the current device's local IPv4 address on Wi-Fi or Hotspot (SoftAP).
     */
    fun getLocalIpAddress(): String? {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            // First pass: look specifically for Wi-Fi or AP interfaces
            val preferredInterfaces = interfaces.filter {
                it.isUp && !it.isLoopback && (
                    it.name.contains("wlan", ignoreCase = true) ||
                    it.name.contains("ap", ignoreCase = true) ||
                    it.name.contains("softap", ignoreCase = true) ||
                    it.name.contains("rndis", ignoreCase = true)
                )
            }

            for (networkInterface in preferredInterfaces) {
                val addresses = Collections.list(networkInterface.inetAddresses)
                for (address in addresses) {
                    if (!address.isLoopbackAddress && address is Inet4Address) {
                        return address.hostAddress
                    }
                }
            }

            // Fallback pass: any non-loopback IPv4 address
            for (networkInterface in interfaces) {
                if (!networkInterface.isUp || networkInterface.isLoopback) continue
                val addresses = Collections.list(networkInterface.inetAddresses)
                for (address in addresses) {
                    if (!address.isLoopbackAddress && address is Inet4Address) {
                        val host = address.hostAddress
                        if (host != null && !host.startsWith("127.")) {
                            return host
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}
