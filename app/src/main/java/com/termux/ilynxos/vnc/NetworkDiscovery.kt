package com.termux.ilynxos.vnc

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.Inet4Address
import java.net.NetworkInterface

class NetworkDiscovery(private val context: Context) {

    companion object {
        private const val TAG = "NetworkDiscovery"

        @Volatile
        private var instance: NetworkDiscovery? = null

        fun getInstance(context: Context): NetworkDiscovery {
            return instance ?: synchronized(this) {
                instance ?: NetworkDiscovery(context.applicationContext).also { instance = it }
            }
        }
    }

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

    private val _networkInfo = MutableStateFlow(getFreshNetworkInfo())
    val networkInfo: StateFlow<VncNetworkInfo> = _networkInfo.asStateFlow()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) { refresh() }
        override fun onLost(network: Network) { refresh() }
        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) { refresh() }
        override fun onLinkPropertiesChanged(network: Network, linkProperties: android.net.LinkProperties) { refresh() }
    }

    init {
        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager.registerNetworkCallback(request, networkCallback)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to register network callback: ${e.message}")
        }
    }

    fun refresh(): VncNetworkInfo {
        val info = getFreshNetworkInfo()
        _networkInfo.value = info
        return info
    }

    fun getFreshNetworkInfo(): VncNetworkInfo {
        var detectedIp: String? = null
        var ifaceName = "wlan0"
        var isConnected = false

        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()?.toList() ?: emptyList()

            // Prioritize Wi-Fi interfaces (wlan, ap, wifi), followed by ethernet or other interfaces
            val sortedInterfaces = interfaces.sortedWith(
                compareByDescending<NetworkInterface> {
                    it.name.startsWith("wlan") || it.name.startsWith("wifi") || it.name.startsWith("ap")
                }.thenByDescending { it.name.startsWith("eth") }
            )

            for (iface in sortedInterfaces) {
                if (!iface.isUp || iface.isLoopback) continue
                val addrs = iface.inetAddresses.toList()
                for (addr in addrs) {
                    if (addr is Inet4Address && !addr.isLoopbackAddress && !addr.isLinkLocalAddress) {
                        detectedIp = addr.hostAddress
                        ifaceName = iface.name
                        isConnected = true
                        break
                    }
                }
                if (isConnected) break
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error enumerating network interfaces: ${e.message}")
        }

        var ssid: String? = null
        try {
            val wifiInfo = wifiManager?.connectionInfo
            if (wifiInfo != null && wifiInfo.networkId != -1) {
                val rawSsid = wifiInfo.ssid
                if (rawSsid != null && !rawSsid.contains("<unknown ssid>")) {
                    ssid = rawSsid.removePrefix("\"").removeSuffix("\"")
                }
            }
        } catch (_: Exception) {}

        return VncNetworkInfo(
            ssid = ssid ?: if (isConnected) "Wi-Fi LAN" else "Disconnected",
            ipAddress = detectedIp,
            isConnected = isConnected && !detectedIp.isNullOrEmpty(),
            interfaceName = ifaceName
        )
    }
}
