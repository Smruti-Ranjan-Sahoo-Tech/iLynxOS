package com.termux.ilynxos.vnc

enum class VncStatus {
    STOPPED,
    STARTING,
    RUNNING,
    ERROR,
    NOT_INSTALLED
}

enum class VncSessionMode {
    VIRTUAL_DISPLAY, // Display :1 - independent background desktop session
    MIRROR_DISPLAY   // Display :0 - mirrors on-device screen
}

data class VncNetworkInfo(
    val ssid: String,
    val ipAddress: String?,
    val isConnected: Boolean,
    val interfaceName: String
)

data class VncServerState(
    val status: VncStatus = VncStatus.STOPPED,
    val port: Int = 5901,
    val display: Int = 1,
    val mode: VncSessionMode = VncSessionMode.VIRTUAL_DISPLAY,
    val address: String? = null,
    val resolution: String = "1280x720",
    val error: String? = null
)
