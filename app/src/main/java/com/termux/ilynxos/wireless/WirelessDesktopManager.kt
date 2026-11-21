package com.termux.ilynxos.wireless

import android.content.Context
import android.util.Log
import com.termux.ilynxos.runtime.LinuxEnvironmentManager
import com.termux.ilynxos.runtime.PRootRuntimeEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WirelessDesktopManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "WirelessDesktopManager"

        @Volatile
        private var instance: WirelessDesktopManager? = null

        fun getInstance(context: Context): WirelessDesktopManager {
            return instance ?: synchronized(this) {
                instance ?: WirelessDesktopManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val runtimeEngine = PRootRuntimeEngine.getInstance(context)
    private val envManager = LinuxEnvironmentManager.getInstance(context)

    val networkDiscovery = NetworkDiscovery.getInstance(context)
    val vncManager = VncServerManager(context)
    val webManager = WebDesktopManager(context)

    private val _logs = MutableStateFlow<String>("[iLynxOS Wireless] Service ready.\n")
    val logs: StateFlow<String> = _logs.asStateFlow()

    private val _isInstallingDependencies = MutableStateFlow(false)
    val isInstallingDependencies: StateFlow<Boolean> = _isInstallingDependencies.asStateFlow()

    init {
        scope.launch {
            envManager.unpackWirelessDesktopScripts()
            checkAllStatuses()
            webManager.checkXpraInstalled()
        }
    }

    fun appendLog(line: String) {
        val current = _logs.value
        val updated = (current + line + "\n").takeLast(8000)
        _logs.value = updated
    }

    fun clearLogs() {
        _logs.value = ""
    }

    fun getWifiIp(): String? {
        return networkDiscovery.getFreshNetworkInfo().ipAddress
    }

    fun getConnectionInfo(): WirelessConnectionInfo {
        val net = networkDiscovery.getFreshNetworkInfo()
        val vncState = vncManager.state.value
        val webState = webManager.state.value

        val ip = net.ipAddress
        val vncAddress = if (ip != null) "$ip:${vncState.port}" else null
        val proto = if (webManager.isTlsEnabled()) "https" else "http"
        val webPass = webManager.getPassword()
        val encPass = if (webPass.isNotEmpty()) "&password=" + java.net.URLEncoder.encode(webPass, "UTF-8") else ""
        val webUrl = if (ip != null) "$proto://$ip:${webState.port}/vnc.html?autoconnect=true&resize=scale$encPass" else null

        return WirelessConnectionInfo(
            wifiIp = ip,
            vncAddress = vncAddress,
            vncPort = vncState.port,
            vncRunning = vncState.status == ServiceStatus.RUNNING,
            webUrl = webUrl,
            webPort = webState.port,
            webRunning = webState.status == ServiceStatus.RUNNING,
            isWifiConnected = net.isConnected
        )
    }

    suspend fun checkAllStatuses() {
        vncManager.checkStatus()
        webManager.checkStatus()
    }

    suspend fun startVnc(port: Int = vncManager.getPort(), password: String = vncManager.getPassword()): Boolean {
        appendLog("[VNC] Starting VNC server on port $port...")
        val success = vncManager.start(port, password)
        if (success) {
            appendLog("[VNC] VNC server started successfully on port $port.")
        } else {
            appendLog("[VNC] Failed to start VNC server. Check desktop is active.")
        }
        return success
    }

    suspend fun stopVnc(): Boolean {
        appendLog("[VNC] Stopping VNC server...")
        val res = vncManager.stop()
        appendLog("[VNC] VNC server stopped.")
        return res
    }

    suspend fun restartVnc(port: Int = vncManager.getPort(), password: String = vncManager.getPassword()): Boolean {
        appendLog("[VNC] Restarting VNC server...")
        stopVnc()
        kotlinx.coroutines.delay(500)
        return startVnc(port, password)
    }

    suspend fun startWeb(
        port: Int = webManager.getPort(),
        password: String = webManager.getPassword(),
        useTls: Boolean = webManager.isTlsEnabled()
    ): Boolean {
        appendLog("[Web] Starting Web Mode (HTML5 Browser) on port $port (TLS: $useTls)...")
        val success = webManager.start(port, password, useTls)
        if (success) {
            val proto = if (useTls) "https" else "http"
            val ip = getWifiIp() ?: "localhost"
            appendLog("[Web] Web Mode running at $proto://$ip:$port/vnc.html")
        } else {
            appendLog("[Web] Failed to start Web Mode on port $port.")
        }
        return success
    }

    suspend fun stopWeb(): Boolean {
        appendLog("[Web] Stopping Web Mode...")
        val res = webManager.stop()
        appendLog("[Web] Web Mode stopped.")
        return res
    }

    suspend fun restartWeb(
        port: Int = webManager.getPort(),
        password: String = webManager.getPassword(),
        useTls: Boolean = webManager.isTlsEnabled()
    ): Boolean {
        appendLog("[Web] Restarting Web Mode...")
        stopWeb()
        kotlinx.coroutines.delay(500)
        return startWeb(port, password, useTls)
    }

    suspend fun startAll(): Boolean {
        appendLog("[Wireless] Starting enabled wireless desktop services...")
        var allOk = true
        if (vncManager.isEnabled()) {
            val vncOk = startVnc()
            if (!vncOk) allOk = false
        }
        if (webManager.isEnabled()) {
            val webOk = startWeb()
            if (!webOk) allOk = false
        }
        return allOk
    }

    suspend fun stopAll(): Boolean {
        appendLog("[Wireless] Stopping all wireless desktop services...")
        stopVnc()
        stopWeb()
        appendLog("[Wireless] All wireless desktop services stopped.")
        return true
    }

    suspend fun installDependencies(onProgress: (String) -> Unit) = withContext(Dispatchers.IO) {
        if (_isInstallingDependencies.value) return@withContext
        _isInstallingDependencies.value = true

        appendLog("[Installer] Beginning wireless desktop dependencies installation...")
        val scriptPath = "/usr/local/share/ilynxos/wireless-desktop/install.sh"
        val cmd = "if [ -x $scriptPath ]; then $scriptPath; else echo 'Install script not found'; fi"

        try {
            runtimeEngine.executeCliCommand(cmd) { line ->
                appendLog("[Installer] $line")
                onProgress(line)
            }
            webManager.checkXpraInstalled()
            appendLog("[Installer] Dependency verification completed.")
        } catch (e: Exception) {
            appendLog("[Installer Error] ${e.message}")
        } finally {
            _isInstallingDependencies.value = false
        }
    }
}
