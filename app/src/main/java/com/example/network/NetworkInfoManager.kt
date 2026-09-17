package com.example.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.Collections

data class NetworkInterfaceInfo(
    val networkType: String,
    val isConnected: Boolean,
    val localIpv4: String,
    val localIpv6: String,
    val gatewayIp: String,
    val localDnsServers: List<String>,
    val wifiSsid: String?,
    val linkSpeedMbps: Int,
    val mtu: Int
)

class NetworkInfoManager(private val context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    fun getNetworkInfo(): NetworkInterfaceInfo {
        var isConnected = false
        var netType = "Disconnected"
        var wifiSsid: String? = null
        var linkSpeed = 0
        var mtu = 1500
        val dnsServers = mutableListOf<String>()
        var gatewayIp = "Unknown"

        val activeNetwork = connectivityManager?.activeNetwork
        val capabilities = connectivityManager?.getNetworkCapabilities(activeNetwork)
        val linkProperties = connectivityManager?.getLinkProperties(activeNetwork)

        if (capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            isConnected = true
            netType = when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular (Mobile)"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
                else -> "Connected (Other)"
            }
        }

        // Extract DNS Servers from LinkProperties
        linkProperties?.dnsServers?.forEach { inetAddr ->
            inetAddr.hostAddress?.let { ip ->
                val cleaned = ip.replace(Regex("%.*$"), "") // remove zone id
                if (!dnsServers.contains(cleaned)) {
                    dnsServers.add(cleaned)
                }
            }
        }

        // Fallback for DNS servers if empty
        if (dnsServers.isEmpty()) {
            dnsServers.addAll(getSystemDnsFallback())
        }

        // Extract Gateway
        linkProperties?.routes?.forEach { route ->
            if (route.isDefaultRoute && route.gateway != null) {
                route.gateway?.hostAddress?.let {
                    gatewayIp = it.replace(Regex("%.*$"), "")
                }
            }
        }

        // Link speed & SSID if WiFi
        if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val wifiInfo = wifiManager?.connectionInfo
            wifiInfo?.let {
                linkSpeed = it.linkSpeed
                val ssid = it.ssid
                if (!ssid.isNullOrBlank() && ssid != "<unknown ssid>") {
                    wifiSsid = ssid.replace("\"", "")
                }
            }
        }

        // Extract Local IPv4 & IPv6
        val (ipv4, ipv6) = getLocalIpAddresses()

        // MTU
        linkProperties?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && it.mtu > 0) {
                mtu = it.mtu
            }
        }

        return NetworkInterfaceInfo(
            networkType = netType,
            isConnected = isConnected,
            localIpv4 = ipv4 ?: "127.0.0.1",
            localIpv6 = ipv6 ?: "None",
            gatewayIp = gatewayIp,
            localDnsServers = dnsServers.ifEmpty { listOf("8.8.8.8", "1.1.1.1") },
            wifiSsid = wifiSsid,
            linkSpeedMbps = linkSpeed,
            mtu = mtu
        )
    }

    private fun getLocalIpAddresses(): Pair<String?, String?> {
        var ipv4: String? = null
        var ipv6: String? = null
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                if (intf.isLoopback || !intf.isUp) continue
                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress) {
                        val host = addr.hostAddress ?: continue
                        val clean = host.replace(Regex("%.*$"), "")
                        if (addr is Inet4Address && ipv4 == null) {
                            ipv4 = clean
                        } else if (addr is Inet6Address && ipv6 == null) {
                            ipv6 = clean
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return Pair(ipv4, ipv6)
    }

    private fun getSystemDnsFallback(): List<String> {
        val result = mutableListOf<String>()
        try {
            val process = Runtime.getRuntime().exec("getprop")
            val reader = process.inputStream.bufferedReader()
            val lines = reader.readLines()
            for (line in lines) {
                if (line.contains("net.dns") || line.contains("dns")) {
                    val match = Regex("\\[(.*?)\\]: \\[(.*?)\\]").find(line)
                    if (match != null) {
                        val value = match.groupValues[2].trim()
                        if (value.matches(Regex("\\d+\\.\\d+\\.\\d+\\.\\d+")) || value.contains(":")) {
                            if (!result.contains(value)) result.add(value)
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return result
    }
}
