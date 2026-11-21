package com.termux.ilynxos.wireless

enum class ServiceStatus {
    STOPPED,
    STARTING,
    RUNNING,
    ERROR,
    NOT_INSTALLED
}

data class WirelessNetworkInfo(
    val ssid: String,
    val ipAddress: String?,
    val isConnected: Boolean,
    val interfaceName: String
)

data class ServiceState(
    val status: ServiceStatus,
    val port: Int,
    val address: String? = null,
    val pid: Int? = null,
    val error: String? = null
)

data class WirelessConnectionInfo(
    val wifiIp: String?,
    val vncAddress: String?,
    val vncPort: Int,
    val vncRunning: Boolean,
    val webUrl: String?,
    val webPort: Int,
    val webRunning: Boolean,
    val isWifiConnected: Boolean
)
